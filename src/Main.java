import GUI.InventoryPanel;
import GUI.LeaderboardPanel;
import GUI.LevelPanel;
import GUI.PlayerHUD;
import Maze.CellState;
import Maze.Difficulty;
import Maze.MazeGrid;
import Model.Action;
import Model.Enemy;
import Model.Level;
import World.GameStateController;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Entry point for Maze Explorer RPG.
 *
 * Layout:
 *   TOP    – PlayerHUD        (lives, score, node info, buffs)
 *   LEFT   – LevelPanel       (generated map, node selection)
 *   CENTER – MazeCanvas       (real MazeGrid rendered here)
 *   RIGHT  – LeaderboardPanel + InventoryPanel
 *   BOTTOM – Status bar
 *
 * Integration:
 *   - WZH's GameStateController / GameMap manage level-selection state
 *   - Trisolaran-main's MazeGrid renders the actual maze in the center canvas
 *   - WASD / Arrow keys move the player through the physical maze grid
 *   - Reaching EXIT → onNodeCompleted(); losing lives → onNodeFailed()
 */
public class Main extends Application {

    // ──── Constants ────
    private static final int    W     = 1200;
    private static final int    H     = 760;
    private static final int    MAZE  = 570;
    private static final String TITLE = "Maze Explorer RPG – INFO 6205";

    // ──── Core state ────
    private GameStateController gameState;

    // Current maze (from Trisolaran maze engine)
    private MazeGrid currentMaze;
    private Level    currentLevel;
    private boolean  mazeInProgress;

    // Enemies
    private java.util.List<Enemy> enemies = new java.util.ArrayList<>();
    private int gameTick = 0;

    // ──── Panels (WZH GUI) ────
    private PlayerHUD        playerHUD;
    private LevelPanel       levelPanel;
    private LeaderboardPanel leaderboardPanel;
    private InventoryPanel   inventoryPanel;

    // ──── UI refs ────
    private Canvas mazeCanvas;
    private Label  statusLabel;

    // ──────────────────────────────────────────────
    //  JavaFX lifecycle
    // ──────────────────────────────────────────────

    @Override
    public void start(Stage stage) {
        gameState        = new GameStateController();
        playerHUD        = new PlayerHUD(gameState);
        levelPanel       = new LevelPanel(gameState);
        leaderboardPanel = new LeaderboardPanel(gameState);
        inventoryPanel   = new InventoryPanel(gameState);

        levelPanel.setOnNodeSelected(this::onNodeSelected);

        BorderPane root = buildLayout();
        Scene scene = new Scene(root, W, H);
        scene.setFill(Color.web("#1a1a2e"));
        scene.setOnKeyPressed(e -> handleKey(e.getCode().toString()));

        stage.setTitle(TITLE);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        startNewGame();
    }

    // ──────────────────────────────────────────────
    //  Game flow
    // ──────────────────────────────────────────────

    private void startNewGame() {
        gameState.generateNewMap();
        currentMaze    = null;
        currentLevel   = null;
        mazeInProgress = false;
        enemies        = new java.util.ArrayList<>();
        gameTick       = 0;
        refreshAll();
        drawWelcome();
        updateStatus("New map generated! Click a highlighted node on the left to begin.");
    }

    /**
     * Called when the player clicks a level node on the map.
     * Creates a MazeGrid with the matching difficulty and renders it.
     */
    private void onNodeSelected(Level node) {
        boolean moved = gameState.moveToNode(node);
        if (!moved) return;

        currentLevel   = node;
        mazeInProgress = false;

        refreshAll();
        startMazeForNode(node);

        String diff = node.getDifficulty() != null ? node.getDifficulty().toString() : "?";
        updateStatus("Entered: " + node + "  [" + diff + "]"
                + "  +" + node.getScoreValue() + " pts on completion"
                + "  |  Lives: " + gameState.getLives()
                + "  |  Use WASD / Arrow keys to navigate the maze");
    }

    /**
     * Generates a MazeGrid matching the node's difficulty and renders it.
     * Difficulty → logical grid size:
     *   EASY   = 5×5  → 19×19 physical
     *   MEDIUM = 7×7  → 27×27 physical
     *   HARD   = 9×9  → 35×35 physical
     */
    private void startMazeForNode(Level node) {
        int[] size = mazeLogSize(node.getDifficulty());
        currentMaze = new MazeGrid(size[0], size[1]);
        currentMaze.generateMaze(toMazeDifficulty(node.getDifficulty()));
        mazeInProgress = true;
        gameTick = 0;
        spawnEnemies();
        renderMaze();
    }

    /** Spawns 3 enemies at non-wall cells at least 8 physical cells from the player. */
    private void spawnEnemies() {
        enemies = new java.util.ArrayList<>();
        if (currentMaze == null) return;
        int physRows = currentMaze.getRows();
        int physCols = currentMaze.getCols();
        int pR = currentMaze.getPlayerRow();
        int pC = currentMaze.getPlayerCol();
        java.util.List<int[]> candidates = new java.util.ArrayList<>();
        for (int r = 0; r < physRows; r++) {
            for (int c = 0; c < physCols; c++) {
                if (!currentMaze.isWall(r, c)
                        && Math.abs(r - pR) + Math.abs(c - pC) >= 8) {
                    candidates.add(new int[]{r, c});
                }
            }
        }
        java.util.Collections.shuffle(candidates, new java.util.Random());
        int count = Math.min(3, candidates.size());
        for (int i = 0; i < count; i++) {
            int[] cell = candidates.get(i);
            enemies.add(new Enemy(i, cell[0], cell[1], 8, 2));
        }
    }

    /**
     * Renders the current MazeGrid onto the center canvas.
     * Wall cells are drawn as filled rectangles; open cells show their state
     * (PLAYER = green, EXIT = yellow, EMPTY = dark blue).
     */
    private void renderMaze() {
        if (currentMaze == null) return;
        GraphicsContext gc = mazeCanvas.getGraphicsContext2D();

        int    physRows = currentMaze.getRows();
        int    physCols = currentMaze.getCols();
        double cellW    = (double) MAZE / physCols;
        double cellH    = (double) MAZE / physRows;

        // Background
        gc.setFill(Color.web("#0d0d1a"));
        gc.fillRect(0, 0, MAZE, MAZE);

        for (int r = 0; r < physRows; r++) {
            for (int c = 0; c < physCols; c++) {
                double x = c * cellW;
                double y = r * cellH;

                if (currentMaze.isWall(r, c)) {
                    gc.setFill(wallColor());
                    gc.fillRect(x, y, cellW, cellH);
                } else {
                    CellState state = currentMaze.getCell(r, c);
                    switch (state) {
                        case PLAYER:
                            gc.setFill(Color.web("#57cc99")); // green player
                            gc.fillOval(x + cellW * 0.1, y + cellH * 0.1,
                                    cellW * 0.8, cellH * 0.8);
                            break;
                        case EXIT:
                            gc.setFill(Color.web("#1e2a4a"));
                            gc.fillRect(x, y, cellW, cellH);
                            gc.setFill(Color.web("#f4d35e")); // yellow exit star
                            gc.fillOval(x + cellW * 0.2, y + cellH * 0.2,
                                    cellW * 0.6, cellH * 0.6);
                            break;
                        default:
                            gc.setFill(Color.web("#1e2a4a"));
                            gc.fillRect(x, y, cellW, cellH);
                            break;
                    }
                }
            }
        }

        // Draw enemies on top
        for (Enemy e : enemies) {
            int er = e.getRow(), ec = e.getCol();
            if (!currentMaze.isWall(er, ec)) {
                double ex = ec * cellW;
                double ey = er * cellH;
                // Stunned enemies shown in gray, active in red
                if (e.isStunned()) {
                    gc.setFill(Color.web("#888888"));
                } else if (e.getState() == Enemy.AIState.CHASE) {
                    gc.setFill(Color.web("#ff2244")); // bright red while chasing
                } else {
                    gc.setFill(Color.web("#e94560")); // normal red
                }
                gc.fillOval(ex + cellW * 0.15, ey + cellH * 0.15,
                        cellW * 0.7, cellH * 0.7);
            }
        }
    }

    /** Returns the wall fill color based on current level difficulty. */
    private Color wallColor() {
        if (currentLevel == null || currentLevel.getDifficulty() == null)
            return Color.web("#334466");
        switch (currentLevel.getDifficulty()) {
            case EASY:   return Color.web("#1a3a2a");
            case MEDIUM: return Color.web("#2a2a1a");
            case HARD:   return Color.web("#3a1a1a");
            default:     return Color.web("#334466");
        }
    }

    private void refreshAll() {
        playerHUD.refresh();
        levelPanel.refresh();
        leaderboardPanel.refresh();
        inventoryPanel.refresh();
    }

    // ──────────────────────────────────────────────
    //  Layout
    // ──────────────────────────────────────────────

    private BorderPane buildLayout() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a2e;");
        root.setTop(buildTopBar());
        root.setLeft(levelPanel);
        root.setCenter(buildMazeCanvas());
        root.setRight(buildRightColumn());
        root.setBottom(buildStatusBar());
        return root;
    }

    private HBox buildTopBar() {
        HBox bar = new HBox(0);
        HBox.setHgrow(playerHUD, Priority.ALWAYS);

        Button newMapBtn = styledBtn("↺ New Map", "#57cc99");
        newMapBtn.setOnAction(e -> startNewGame());
        newMapBtn.setPadding(new Insets(10, 12, 10, 12));

        Button quitBtn = styledBtn("✕ Quit", "#e94560");
        quitBtn.setOnAction(e -> System.exit(0));
        quitBtn.setPadding(new Insets(10, 12, 10, 12));

        HBox btnBox = new HBox(0, newMapBtn, quitBtn);
        btnBox.setStyle("-fx-background-color: #16213e;"
                + "-fx-border-color: #0f3460; -fx-border-width: 0 0 2 0;");

        bar.getChildren().addAll(playerHUD, btnBox);
        return bar;
    }

    private StackPane buildMazeCanvas() {
        mazeCanvas = new Canvas(MAZE, MAZE);
        StackPane wrap = new StackPane(mazeCanvas);
        wrap.setStyle("-fx-background-color: #0f3460;");
        wrap.setPadding(new Insets(10));
        return wrap;
    }

    private VBox buildRightColumn() {
        VBox col = new VBox(0);
        col.setPrefWidth(215);
        col.getChildren().addAll(leaderboardPanel, new Separator(), inventoryPanel);
        return col;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(6, 16, 6, 16));
        bar.setStyle("-fx-background-color: #0f3460;");
        statusLabel = new Label("Initialising…");
        statusLabel.setFont(Font.font("Monospaced", 12));
        statusLabel.setTextFill(Color.web("#a8dadc"));
        bar.getChildren().add(statusLabel);
        return bar;
    }

    // ──────────────────────────────────────────────
    //  Canvas drawing
    // ──────────────────────────────────────────────

    private void drawWelcome() {
        GraphicsContext gc = mazeCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#0d0d1a"));
        gc.fillRect(0, 0, MAZE, MAZE);
        gc.setStroke(Color.web("#0f3460")); gc.setLineWidth(1);
        for (int x = 0; x <= MAZE; x += 20) gc.strokeLine(x, 0, x, MAZE);
        for (int y = 0; y <= MAZE; y += 20) gc.strokeLine(0, y, MAZE, y);
        gc.setFill(Color.web("#e94560"));
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 22));
        gc.fillText("MAZE EXPLORER RPG", 120, MAZE / 2 - 20);
        gc.setFill(Color.web("#a8dadc"));
        gc.setFont(Font.font("Monospaced", 14));
        gc.fillText("Click a node on the map to start", 135, MAZE / 2 + 20);
    }

    // ──────────────────────────────────────────────
    //  Keyboard input
    // ──────────────────────────────────────────────

    /**
     * Handles keyboard input:
     *   - WASD / Arrow keys move the player one physical cell in the MazeGrid
     *   - Reaching the EXIT triggers onNodeCompleted()
     *   - F key forces a "fail" (for testing live / path-failure logic)
     */
    private void handleKey(String key) {
        int dr = 0, dc = 0;
        Action.ActionType actionType = null;

        switch (key) {
            case "W": case "UP":    dr = -1; actionType = Action.ActionType.MOVE_UP;    break;
            case "S": case "DOWN":  dr =  1; actionType = Action.ActionType.MOVE_DOWN;  break;
            case "A": case "LEFT":  dc = -1; actionType = Action.ActionType.MOVE_LEFT;  break;
            case "D": case "RIGHT": dc =  1; actionType = Action.ActionType.MOVE_RIGHT; break;
            case "F":  // force-fail for testing
                if (mazeInProgress) forceNodeFail();
                return;
            default: return;
        }

        if (actionType != null) gameState.logAction(new Action(actionType));

        if (currentMaze != null && mazeInProgress && (dr != 0 || dc != 0)) {
            boolean moved = currentMaze.movePlayer(dr, dc);
            if (moved) {
                gameTick++;
                // Update enemy AI
                int pR = currentMaze.getPlayerRow();
                int pC = currentMaze.getPlayerCol();
                for (Enemy e : enemies) {
                    e.update(currentMaze, pR, pC, gameTick);
                }
                // Check enemy collision
                for (Enemy e : enemies) {
                    if (!e.isStunned() && e.isAtPlayer(pR, pC)) {
                        e.stun(10);
                        boolean dead = gameState.onNodeFailed();
                        refreshAll();
                        if (dead) {
                            mazeInProgress = false;
                            currentMaze = null;
                            enemies = new java.util.ArrayList<>();
                            updateStatus("☠ Caught by enemy! All lives lost. Score RESET.");
                            drawWelcome();
                            return;
                        } else {
                            updateStatus("Caught by enemy! Lives: " + gameState.getLives()
                                    + "  |  Click the same node to retry.");
                            // Respawn: restart current maze
                            startMazeForNode(currentLevel);
                            return;
                        }
                    }
                }
                renderMaze();
                if (currentMaze.isPlayerAtExit()) {
                    mazeInProgress = false;
                    enemies = new java.util.ArrayList<>();
                    gameState.onNodeCompleted();
                    refreshAll();
                    Level curr = gameState.getCurrentNode();
                    if (curr != null && curr.isStart()) {
                        updateStatus("Path complete! Score submitted. Choose a new path.");
                        drawWelcome();
                    } else {
                        updateStatus("Cleared! +" + currentLevel.getScoreValue()
                                + " pts  |  Accumulated: " + gameState.getAccumulatedScore()
                                + "  |  Click next node or continue.");
                    }
                    currentMaze = null;
                }
            }
        }
    }

    /** Simulate failing the current node (lose a life). Press F to trigger. */
    private void forceNodeFail() {
        mazeInProgress = false;
        currentMaze    = null;
        boolean dead   = gameState.onNodeFailed();
        refreshAll();
        if (dead) {
            updateStatus("☠ All lives lost! Score RESET to 0. Try again.");
            drawWelcome();
        } else {
            updateStatus("✗ Failed! Lives: " + gameState.getLives()
                    + "  |  Click the same node to retry.");
        }
    }

    // ──────────────────────────────────────────────
    //  Difficulty mapping helpers
    // ──────────────────────────────────────────────

    /** Maps Level.Difficulty → Maze.Difficulty for MazeGrid.generateMaze(). */
    private static Difficulty toMazeDifficulty(Level.Difficulty d) {
        if (d == null) return Difficulty.EASY;
        switch (d) {
            case EASY:   return Difficulty.EASY;
            case MEDIUM: return Difficulty.MEDIUM;
            default:     return Difficulty.HARD;
        }
    }

    /** Returns the [logRows, logCols] size for a maze of the given difficulty. */
    private static int[] mazeLogSize(Level.Difficulty d) {
        if (d == null) return new int[]{5, 5};
        switch (d) {
            case EASY:   return new int[]{5, 5};   // physical 19×19
            case MEDIUM: return new int[]{7, 7};   // physical 27×27
            default:     return new int[]{9, 9};   // physical 35×35
        }
    }

    // ──────────────────────────────────────────────
    //  Utility
    // ──────────────────────────────────────────────

    private void updateStatus(String msg) {
        if (statusLabel != null) statusLabel.setText(msg);
    }

    private Button styledBtn(String text, String col) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Monospaced", 12));
        btn.setTextFill(Color.web(col));
        String n = "-fx-background-color:#0f3460;-fx-border-color:" + col + ";-fx-border-width:1;-fx-cursor:hand;";
        String h = "-fx-background-color:#1a1a4e;-fx-border-color:" + col + ";-fx-border-width:1;-fx-cursor:hand;";
        btn.setStyle(n);
        btn.setOnMouseEntered(e -> btn.setStyle(h));
        btn.setOnMouseExited(e  -> btn.setStyle(n));
        return btn;
    }

    public static void main(String[] args) { launch(args); }
}
