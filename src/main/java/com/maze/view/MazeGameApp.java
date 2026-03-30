package com.maze.view;

import com.maze.controller.GameController;
import com.maze.controller.GameController.Direction;
import com.maze.model.*;
import com.maze.model.Skill.SkillType;
import com.maze.util.MazeGenerator;
import com.maze.util.Pathfinder;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.*;

/**
 * MazeGameApp - JavaFX GUI for the Maze Adventure Game.
 *
 * Controls:
 *   WASD / Arrow Keys  - Move player
 *   U                  - Undo last move (Stack)
 *   1-6                - Use skills (BST)
 *   R                  - New game
 *   H                  - Show path hint (Dijkstra + Priority Queue)
 */
public class MazeGameApp extends Application implements GameController.GameEventListener {

    // Game
    private GameController controller;

    // UI Components
    private Canvas mazeCanvas;
    private Label statusLabel;
    private Label statsLabel;
    private VBox skillPanel;
    private Label eventLogLabel;

    // Rendering
    private static final int CELL_SIZE = 24;
    private static final int DEFAULT_MAZE_SIZE = 25; // 25x25 maze

    // Fog of war - cells player has seen
    private final Set<String> revealedCells = new HashSet<>();

    @Override
    public void start(Stage primaryStage) {
        controller = new GameController();
        controller.addListener(this);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a2e;");

        // === Top: Status bar ===
        HBox topBar = createTopBar();
        root.setTop(topBar);

        // === Center: Maze canvas ===
        mazeCanvas = new Canvas(DEFAULT_MAZE_SIZE * CELL_SIZE, DEFAULT_MAZE_SIZE * CELL_SIZE);
        StackPane canvasHolder = new StackPane(mazeCanvas);
        canvasHolder.setStyle("-fx-background-color: #16213e; -fx-padding: 10;");
        root.setCenter(canvasHolder);

        // === Right: Skill panel + Stats ===
        VBox rightPanel = createRightPanel();
        root.setRight(rightPanel);

        // === Bottom: Controls info ===
        HBox bottomBar = createBottomBar();
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 900, 720);
        // Use addEventFilter (capturing phase) so game keys always work
        // regardless of which UI control has focus
        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
            KeyCode code = e.getCode();
            switch (code) {
                case W, A, S, D, UP, DOWN, LEFT, RIGHT,
                     U, H, R, DIGIT1, DIGIT2, DIGIT3, DIGIT4, DIGIT5, DIGIT6 -> {
                    handleKeyPress(code);
                    e.consume();
                }
                default -> { }
            }
        });

        primaryStage.setTitle("Maze Adventure - INFO 6205 Final Project");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();

        // Start first game
        controller.newGame(DEFAULT_MAZE_SIZE, DEFAULT_MAZE_SIZE);
        mazeCanvas.requestFocus();
    }

    // ==================== UI Creation ====================

    private HBox createTopBar() {
        statusLabel = new Label("Welcome to Maze Adventure!");
        statusLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        statusLabel.setTextFill(Color.web("#e94560"));

        statsLabel = new Label("Moves: 0 | Coins: 0 | Level: 1");
        statsLabel.setFont(Font.font("Consolas", 13));
        statsLabel.setTextFill(Color.web("#a8d8ea"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newGameBtn = new Button("New Game (R)");
        newGameBtn.setStyle("-fx-background-color: #e94560; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-cursor: hand;");
        newGameBtn.setOnAction(e -> {
            controller.newGame(DEFAULT_MAZE_SIZE, DEFAULT_MAZE_SIZE);
            revealedCells.clear();
            mazeCanvas.requestFocus();
        });

        HBox bar = new HBox(15, statusLabel, spacer, statsLabel, newGameBtn);
        bar.setPadding(new Insets(10, 15, 10, 15));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #0f3460;");
        return bar;
    }

    private VBox createRightPanel() {
        Label skillTitle = new Label("Skills (BST)");
        skillTitle.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        skillTitle.setTextFill(Color.web("#e94560"));

        skillPanel = new VBox(5);
        skillPanel.setPadding(new Insets(5));

        Label eventTitle = new Label("Event Log (PQ)");
        eventTitle.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        eventTitle.setTextFill(Color.web("#e94560"));
        eventTitle.setPadding(new Insets(10, 0, 0, 0));

        eventLogLabel = new Label("Game started...");
        eventLogLabel.setFont(Font.font("Consolas", 11));
        eventLogLabel.setTextFill(Color.web("#a8d8ea"));
        eventLogLabel.setWrapText(true);
        eventLogLabel.setMaxWidth(200);

        Label dsInfo = new Label(
                "Data Structures Used:\n" +
                        "• Graph (Maze + Enemy AI)\n" +
                        "• Stack (Move Undo)\n" +
                        "• PriorityQueue (Events)\n" +
                        "• BST (Skill Tree)\n" +
                        "• Dijkstra (Enemy Chase)"
        );
        dsInfo.setFont(Font.font("Consolas", 10));
        dsInfo.setTextFill(Color.web("#533483"));
        dsInfo.setPadding(new Insets(15, 0, 0, 0));

        VBox panel = new VBox(8, skillTitle, skillPanel,
                new Separator(), eventTitle, eventLogLabel,
                new Separator(), dsInfo);
        panel.setPadding(new Insets(10, 10, 10, 10));
        panel.setPrefWidth(220);
        panel.setStyle("-fx-background-color: #16213e;");
        return panel;
    }

    private HBox createBottomBar() {
        Label controls = new Label(
                "WASD/Arrows: Move | U: Undo | 1-6: Skills | H: Hint | R: New Game");
        controls.setFont(Font.font("Consolas", 11));
        controls.setTextFill(Color.web("#a8d8ea"));

        HBox bar = new HBox(controls);
        bar.setPadding(new Insets(8, 15, 8, 15));
        bar.setAlignment(Pos.CENTER);
        bar.setStyle("-fx-background-color: #0f3460;");
        return bar;
    }

    // ==================== Input Handling ====================

    private void handleKeyPress(KeyCode code) {
        switch (code) {
            case W, UP -> controller.movePlayer(Direction.UP);
            case S, DOWN -> controller.movePlayer(Direction.DOWN);
            case A, LEFT -> controller.movePlayer(Direction.LEFT);
            case D, RIGHT -> controller.movePlayer(Direction.RIGHT);
            case U -> controller.undoMove();
            case H -> controller.useSkill(SkillType.PATH_HINT);
            case DIGIT1 -> controller.useSkill(SkillType.SPEED_BOOST);
            case DIGIT2 -> controller.useSkill(SkillType.WALL_BREAK);
            case DIGIT3 -> controller.useSkill(SkillType.PATH_HINT);
            case DIGIT4 -> controller.useSkill(SkillType.TELEPORT);
            case DIGIT5 -> controller.useSkill(SkillType.VISION);
            case DIGIT6 -> controller.useSkill(SkillType.BACKTRACK);
            case R -> {
                controller.newGame(DEFAULT_MAZE_SIZE, DEFAULT_MAZE_SIZE);
                revealedCells.clear();
            }
            default -> { }
        }
        mazeCanvas.requestFocus();
    }

    // ==================== Rendering ====================

    private void render() {
        GraphicsContext gc = mazeCanvas.getGraphicsContext2D();
        MazeGenerator gen = controller.getMazeGenerator();
        Player player = controller.getPlayer();
        int rows = gen.getRows();
        int cols = gen.getCols();

        // Resize canvas if needed
        mazeCanvas.setWidth(cols * CELL_SIZE);
        mazeCanvas.setHeight(rows * CELL_SIZE);

        // Update fog of war - reveal cells near player
        updateFogOfWar(player);

        // Clear - same as fog color
        gc.setFill(Color.web("#020208"));
        gc.fillRect(0, 0, mazeCanvas.getWidth(), mazeCanvas.getHeight());

        // Draw maze cells
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                double x = c * CELL_SIZE;
                double y = r * CELL_SIZE;
                String cellKey = MazeGraph.cellKey(r, c);

                if (!revealedCells.contains(cellKey)) {
                    // Fog - unrevealed: very dark, clearly different from passages
                    gc.setFill(Color.web("#020208"));
                    gc.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    continue;
                }

                if (gen.isWall(r, c)) {
                    // Wall - blue block
                    gc.setFill(Color.web("#0f3460"));
                    gc.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    gc.setStroke(Color.web("#16213e"));
                    gc.setLineWidth(0.5);
                    gc.strokeRect(x, y, CELL_SIZE, CELL_SIZE);
                } else {
                    // Passage - noticeably lighter than fog
                    gc.setFill(Color.web("#252545"));
                    gc.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    // Subtle inner border to make passages clearly "open"
                    gc.setFill(Color.web("#2d2d55"));
                    gc.fillRect(x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2);
                }
            }
        }

        // Draw hint path
        if (player.isPathHintActive()) {
            gc.setFill(Color.web("#533483", 0.4));
            for (String cell : controller.getHintPath()) {
                int r = MazeGraph.getRow(cell);
                int c = MazeGraph.getCol(cell);
                gc.fillRect(c * CELL_SIZE + 2, r * CELL_SIZE + 2,
                        CELL_SIZE - 4, CELL_SIZE - 4);
            }
        }

        // Draw coins
        gc.setFill(Color.web("#ffd700"));
        for (String cell : controller.getCoinCells()) {
            if (revealedCells.contains(cell)) {
                int r = MazeGraph.getRow(cell);
                int c = MazeGraph.getCol(cell);
                double cx = c * CELL_SIZE + CELL_SIZE / 2.0;
                double cy = r * CELL_SIZE + CELL_SIZE / 2.0;
                gc.fillOval(cx - 5, cy - 5, 10, 10);
            }
        }

        // Draw traps
        gc.setFill(Color.web("#ff6b6b", 0.7));
        for (String cell : controller.getTrapCells()) {
            if (revealedCells.contains(cell)) {
                int r = MazeGraph.getRow(cell);
                int c = MazeGraph.getCol(cell);
                double cx = c * CELL_SIZE + CELL_SIZE / 2.0;
                double cy = r * CELL_SIZE + CELL_SIZE / 2.0;
                // Draw X for trap
                gc.setStroke(Color.web("#ff6b6b"));
                gc.setLineWidth(2);
                gc.strokeLine(cx - 4, cy - 4, cx + 4, cy + 4);
                gc.strokeLine(cx + 4, cy - 4, cx - 4, cy + 4);
            }
        }

        // Draw enemies
        for (Enemy enemy : controller.getEnemies()) {
            String enemyCell = enemy.getCellKey();
            if (revealedCells.contains(enemyCell)) {
                int er = enemy.getRow();
                int ec = enemy.getCol();
                double ex = ec * CELL_SIZE + CELL_SIZE / 2.0;
                double ey = er * CELL_SIZE + CELL_SIZE / 2.0;

                // Color based on AI state
                Color enemyColor;
                if (enemy.isStunned()) {
                    enemyColor = Color.web("#888888"); // Gray when stunned
                } else if (enemy.getState() == Enemy.AIState.CHASE) {
                    enemyColor = Color.web("#ff0000"); // Red when chasing
                } else if (enemy.getState() == Enemy.AIState.RETURN) {
                    enemyColor = Color.web("#ff8800"); // Orange when returning
                } else {
                    enemyColor = Color.web("#ff4444"); // Light red when patrolling
                }

                // Glow effect (larger when chasing)
                double glowSize = enemy.getState() == Enemy.AIState.CHASE ? 14 : 10;
                gc.setFill(enemyColor.deriveColor(0, 1, 1, 0.3));
                gc.fillOval(ex - glowSize, ey - glowSize, glowSize * 2, glowSize * 2);

                // Enemy body (triangle shape)
                gc.setFill(enemyColor);
                double size = 6;
                double[] xPoints = {ex, ex - size, ex + size};
                double[] yPoints = {ey - size, ey + size, ey + size};
                gc.fillPolygon(xPoints, yPoints, 3);

                // Eyes
                gc.setFill(Color.WHITE);
                gc.fillOval(ex - 3, ey - 2, 3, 3);
                gc.fillOval(ex + 1, ey - 2, 3, 3);

                // State indicator text
                if (enemy.isStunned()) {
                    gc.setFill(Color.YELLOW);
                    gc.setFont(Font.font("Consolas", FontWeight.BOLD, 8));
                    gc.fillText("ZZ", ex - 6, ey - 8);
                } else if (enemy.getState() == Enemy.AIState.CHASE) {
                    gc.setFill(Color.RED);
                    gc.setFont(Font.font("Consolas", FontWeight.BOLD, 8));
                    gc.fillText("!", ex - 2, ey - 8);
                }
            }
        }

        // Draw exit
        String exitCell = controller.getExitCell();
        if (revealedCells.contains(exitCell)) {
            int er = MazeGraph.getRow(exitCell);
            int ec = MazeGraph.getCol(exitCell);
            gc.setFill(Color.web("#00e676"));
            gc.fillRect(ec * CELL_SIZE + 3, er * CELL_SIZE + 3,
                    CELL_SIZE - 6, CELL_SIZE - 6);
            gc.setFill(Color.web("#252545"));
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 12));
            gc.fillText("E", ec * CELL_SIZE + 7, er * CELL_SIZE + 17);
        }

        // Draw player
        double px = player.getCol() * CELL_SIZE + CELL_SIZE / 2.0;
        double py = player.getRow() * CELL_SIZE + CELL_SIZE / 2.0;
        // Glow effect
        gc.setFill(Color.web("#e94560", 0.3));
        gc.fillOval(px - 10, py - 10, 20, 20);
        // Player circle
        gc.setFill(Color.web("#e94560"));
        gc.fillOval(px - 6, py - 6, 12, 12);
        // Direction indicator
        gc.setFill(Color.WHITE);
        gc.fillOval(px - 2, py - 2, 4, 4);

        // Draw small arrows showing which directions the player CAN move
        gc.setFill(Color.web("#00e676", 0.6));
        int pr = player.getRow(), pc = player.getCol();
        int[][] moveDirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : moveDirs) {
            if (!gen.isWall(pr + d[0], pc + d[1])) {
                double ax = px + d[1] * (CELL_SIZE / 2.0 + 2);
                double ay = py + d[0] * (CELL_SIZE / 2.0 + 2);
                gc.fillOval(ax - 2.5, ay - 2.5, 5, 5);
            }
        }
    }

    /** Update fog of war based on player's vision radius. */
    private void updateFogOfWar(Player player) {
        MazeGenerator gen = controller.getMazeGenerator();
        int radius = player.getVisionRadius();
        int pr = player.getRow();
        int pc = player.getCol();

        for (int r = pr - radius; r <= pr + radius; r++) {
            for (int c = pc - radius; c <= pc + radius; c++) {
                if (r >= 0 && r < gen.getRows() && c >= 0 && c < gen.getCols()) {
                    // Manhattan distance check
                    if (Math.abs(r - pr) + Math.abs(c - pc) <= radius) {
                        revealedCells.add(MazeGraph.cellKey(r, c));
                    }
                }
            }
        }
    }

    /** Update the skill panel UI from BST data. */
    private void updateSkillPanel() {
        if (controller.getPlayer() == null) return;
        skillPanel.getChildren().clear();

        List<Skill> skills = controller.getPlayer().getAllSkills();
        int index = 1;
        for (Skill s : skills) {
            String prefix = "[" + index + "] ";
            String status = s.isUnlocked() ?
                    (s.canUse() ? "Ready" : "Used") : "Lv." + s.getLevelRequired();
            Label lbl = new Label(prefix + s.getName() + " (" + status + ")");
            lbl.setFont(Font.font("Consolas", 11));
            lbl.setTextFill(s.isUnlocked() ?
                    (s.canUse() ? Color.web("#00e676") : Color.web("#666")) :
                    Color.web("#533483"));
            lbl.setTooltip(new Tooltip(s.getDescription()));
            skillPanel.getChildren().add(lbl);
            index++;
        }
    }

    /** Update stats display. */
    private void updateStats() {
        Player p = controller.getPlayer();
        if (p == null) return;
        statsLabel.setText(String.format(
                "Moves: %d | Coins: %d | Level: %d | XP: %d/%d | Lives: %d",
                p.getMoveCount(), p.getCoins(), p.getLevel(),
                p.getExperience(), p.getExpToNextLevel(), controller.getPlayerLives()));
    }

    // ==================== GameEventListener ====================

    @Override
    public void onGameUpdate() {
        Platform.runLater(() -> {
            render();
            updateStats();
            updateSkillPanel();
        });
    }

    @Override
    public void onStatusMessage(String message) {
        Platform.runLater(() -> {
            statusLabel.setText(message);
            // Append to event log
            String current = eventLogLabel.getText();
            String[] lines = current.split("\n");
            if (lines.length > 8) {
                current = String.join("\n", Arrays.copyOfRange(lines, lines.length - 8, lines.length));
            }
            eventLogLabel.setText(current + "\n" + message);
        });
    }

    @Override
    public void onGameWon(int moves, int coins, int level) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Congratulations!");
            alert.setHeaderText("You escaped the maze!");
            alert.setContentText(String.format(
                    "Moves: %d\nCoins: %d\nLevel: %d\n\nPress OK to play again.", moves, coins, level));
            alert.showAndWait();
            controller.newGame(DEFAULT_MAZE_SIZE, DEFAULT_MAZE_SIZE);
            revealedCells.clear();
        });
    }

    @Override
    public void onPlayerCaught(int livesRemaining) {
        Platform.runLater(() -> {
            if (livesRemaining <= 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Game Over!");
                alert.setHeaderText("You were caught too many times!");
                alert.setContentText("All lives lost. Press OK to try again.");
                alert.showAndWait();
                controller.newGame(DEFAULT_MAZE_SIZE, DEFAULT_MAZE_SIZE);
                revealedCells.clear();
            } else {
                statusLabel.setText("Caught! Respawned at start. Lives: " + livesRemaining);
                revealedCells.clear(); // Reset fog since player respawned
                render();
            }
        });
    }

    // ==================== Main ====================

    public static void main(String[] args) {
        launch(args);
    }
}