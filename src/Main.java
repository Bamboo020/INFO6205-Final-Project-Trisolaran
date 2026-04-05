import GUI.InventoryPanel;
import GUI.LeaderboardPanel;
import GUI.LevelPanel;
import GUI.LoginPanel;
import GUI.PlayerHUD;
import Maze.CellState;
import Maze.Difficulty;
import Maze.MazeGrid;
import Model.Enemy;
import Model.Item;
import Model.Level;
import World.AuthController;
import World.DBManager;
import World.GameStateController;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.List;

/**
 * Entry point for Maze Explorer RPG.
 *
 * 已修改：
 *   - 新增 MySQL 登录 / 注册 / 登出功能（LoginPanel + AuthController + DBManager）
 *   - 通关时分数写入 MySQL scores 表
 *   - 顶部新增用户名显示 + Logout 按钮
 *   - 移除 Action logAction（回放功能不需要）
 */
public class Main extends Application {

    // ──── Constants ────
    private static final int    W     = 1200;
    private static final int    H     = 760;
    private static final int    MAZE  = 570;
    private static final String TITLE = "Maze Explorer RPG – INFO 6205";

    // ──── DB / Auth（新增） ────
    private DBManager      db;
    private AuthController auth;

    // ──── Scenes（新增） ────
    private Stage      primaryStage;
    private Scene      loginScene;
    private Scene      gameScene;
    private LoginPanel loginPanel;

    // ──── Core state ────
    private GameStateController gameState;

    // Current maze (from Trisolaran maze engine)
    private MazeGrid currentMaze;
    private Level    currentLevel;
    private boolean  mazeInProgress;

    // Enemies
    private List<Enemy> enemies = new java.util.ArrayList<>();
    private int gameTick = 0;

    // ──── 道具散布点 ────
    private java.util.Map<String, Item.ItemType> itemsOnMap = new java.util.HashMap<>();

    // ──── Panels (WZH GUI) ────
    private PlayerHUD        playerHUD;
    private LevelPanel       levelPanel;
    private LeaderboardPanel leaderboardPanel;
    private InventoryPanel   inventoryPanel;

    // ──── UI refs ────
    private Canvas mazeCanvas;
    private Label  statusLabel;
    private Label  userLabel;   // 新增：显示当前登录用户

    // ──────────────────────────────────────────────
    //  JavaFX lifecycle
    // ──────────────────────────────────────────────

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        // ── 新增：初始化 DB & Auth ──
        db = new DBManager();
        String dbErr = db.connect();
        if (dbErr != null) System.out.println("[DB] " + dbErr + " – offline mode.");

        auth      = new AuthController(db);
        gameState = new GameStateController();

        // ── 新增：从 MySQL 恢复历史分数到 MaxHeap + AVLTree ──
        loadHistoricalScores();

        // ── 新增：建两个 Scene ──
        buildLoginScene();
        buildGameScene();

        stage.setTitle(TITLE);
        stage.setResizable(false);
        stage.setScene(loginScene);          // 先显示登录页
        stage.show();

        stage.setOnCloseRequest(e -> db.disconnect());
    }

    // ──────────────────────────────────────────────
    //  新增：MySQL 历史分数恢复
    // ──────────────────────────────────────────────

    private void loadHistoricalScores() {
        List<DBManager.ScoreRecord> rows = db.loadAllScores();
        for (DBManager.ScoreRecord row : rows) {
            gameState.recordScore(row.score,
                    new Model.GameRecord(row.username, row.score, row.levelId, 0L));
        }
        System.out.println("[DB] Loaded " + rows.size() + " historical scores.");
    }

    // ──────────────────────────────────────────────
    //  新增：Login Scene
    // ──────────────────────────────────────────────

    private void buildLoginScene() {
        loginPanel = new LoginPanel(auth);
        loginPanel.setOnSuccess(this::onLoginSuccess);
        loginScene = new Scene(loginPanel, W, H);
    }

    private void onLoginSuccess(String username) {
        startNewGame();
        userLabel.setText("👤 " + username + (auth.isDbAvailable() ? "  🟢" : "  🟡"));
        primaryStage.setScene(gameScene);
    }

    // ──────────────────────────────────────────────
    //  新增：Game Scene（原来 start() 的逻辑移到这里）
    // ──────────────────────────────────────────────

    private void buildGameScene() {
        playerHUD        = new PlayerHUD(gameState);
        levelPanel       = new LevelPanel(gameState);
        leaderboardPanel = new LeaderboardPanel(gameState);
        inventoryPanel   = new InventoryPanel(gameState);

        levelPanel.setOnNodeSelected(this::onNodeSelected);

        BorderPane root = buildLayout();
        gameScene = new Scene(root, W, H);
        gameScene.setFill(Color.web("#1a1a2e"));
        gameScene.setOnKeyPressed(e -> handleKey(e.getCode().toString()));
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
        itemsOnMap     = new java.util.HashMap<>();
        gameTick       = 0;
        refreshAll();
        drawWelcome();
        updateStatus("New map generated! Click a highlighted node on the left to begin.");
    }

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
                + "  |  WASD/Arrow=Move  1=Speed  2=WallPass  3=Attack");
    }

    private void startMazeForNode(Level node) {
        int[] size = mazeLogSize(node.getDifficulty());
        currentMaze = new MazeGrid(size[0], size[1]);
        currentMaze.generateMaze(toMazeDifficulty(node.getDifficulty()));
        mazeInProgress = true;
        gameTick = 0;
        wallPassActive = false;
        speedBoostTicks = 0;
        playerItems = new java.util.HashMap<>();
        spawnEnemies();
        placeItemsOnMaze();
        updateInventoryDisplay();
        renderMaze();
    }

    private void spawnEnemies() {
        enemies = new java.util.ArrayList<>();
        if (currentMaze == null) return;
        int physRows = currentMaze.getRows();
        int physCols = currentMaze.getCols();
        int pR = currentMaze.getPlayerRow();
        int pC = currentMaze.getPlayerCol();
        List<int[]> candidates = new java.util.ArrayList<>();
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

    private void placeItemsOnMaze() {
        itemsOnMap = new java.util.HashMap<>();
        if (currentMaze == null) return;
        int physRows = currentMaze.getRows();
        int physCols = currentMaze.getCols();
        int exitR = (currentMaze.getLogRows() - 1) * 4 + 1;
        int exitC = (currentMaze.getLogCols() - 1) * 4 + 1;
        List<int[]> openCells = new java.util.ArrayList<>();
        for (int r = 0; r < physRows; r++) {
            for (int c = 0; c < physCols; c++) {
                if (!currentMaze.isWall(r, c)
                        && !(r == 1 && c == 1)
                        && !(r == exitR && c == exitC)) {
                    openCells.add(new int[]{r, c});
                }
            }
        }
        java.util.Collections.shuffle(openCells, new java.util.Random());
        Item.ItemType[] types = Item.ItemType.values();
        int perType = 2;
        int idx = 0;
        for (Item.ItemType type : types) {
            for (int i = 0; i < perType && idx < openCells.size(); i++, idx++) {
                int[] cell = openCells.get(idx);
                itemsOnMap.put(cell[0] + "," + cell[1], type);
            }
        }
    }

    private void renderMaze() {
        if (currentMaze == null) return;
        GraphicsContext gc = mazeCanvas.getGraphicsContext2D();
        int    physRows = currentMaze.getRows();
        int    physCols = currentMaze.getCols();
        double cellW    = (double) MAZE / physCols;
        double cellH    = (double) MAZE / physRows;

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
                            gc.setFill(Color.web("#57cc99"));
                            gc.fillOval(x + cellW * 0.1, y + cellH * 0.1,
                                    cellW * 0.8, cellH * 0.8);
                            break;
                        case EXIT:
                            gc.setFill(Color.web("#1e2a4a"));
                            gc.fillRect(x, y, cellW, cellH);
                            gc.setFill(Color.web("#f4d35e"));
                            gc.fillOval(x + cellW * 0.2, y + cellH * 0.2,
                                    cellW * 0.6, cellH * 0.6);
                            break;
                        default:
                            gc.setFill(Color.web("#1e2a4a"));
                            gc.fillRect(x, y, cellW, cellH);
                            break;
                    }
                    String key = r + "," + c;
                    if (itemsOnMap.containsKey(key)) {
                        drawItemIcon(gc, x, y, cellW, cellH, itemsOnMap.get(key));
                    }
                }
            }
        }

        for (Enemy e : enemies) {
            int er = e.getRow(), ec = e.getCol();
            if (!currentMaze.isWall(er, ec)) {
                double ex = ec * cellW;
                double ey = er * cellH;
                if (e.isStunned()) {
                    gc.setFill(Color.web("#888888"));
                } else if (e.getState() == Enemy.AIState.CHASE) {
                    gc.setFill(Color.web("#ff2244"));
                } else {
                    gc.setFill(Color.web("#e94560"));
                }
                gc.fillOval(ex + cellW * 0.15, ey + cellH * 0.15,
                        cellW * 0.7, cellH * 0.7);
            }
        }
    }

    private void drawItemIcon(GraphicsContext gc, double x, double y,
                              double w, double h, Item.ItemType type) {
        double cx = x + w / 2;
        double cy = y + h / 2;
        double size = Math.min(w, h) * 0.35;
        Color color;
        switch (type) {
            case SPEED_BOOST: color = Color.web("#00e5ff"); break;
            case WALL_PASS:   color = Color.web("#ff9800"); break;
            case ATTACK:      color = Color.web("#ff4081"); break;
            default:          color = Color.WHITE; break;
        }
        gc.setFill(color);
        gc.fillPolygon(
                new double[]{cx, cx + size, cx, cx - size},
                new double[]{cy - size, cy, cy + size, cy}, 4);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.0);
        gc.strokePolygon(
                new double[]{cx, cx + size, cx, cx - size},
                new double[]{cy - size, cy, cy + size, cy}, 4);
    }

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

        // ── 新增：用户名标签 ──
        userLabel = new Label("👤 —");
        userLabel.setFont(Font.font("Monospaced", 12));
        userLabel.setTextFill(Color.web("#a8dadc"));
        userLabel.setPadding(new Insets(10, 10, 10, 14));

        Button newMapBtn = styledBtn("↺ New Map", "#57cc99");
        newMapBtn.setOnAction(e -> startNewGame());
        newMapBtn.setPadding(new Insets(10, 12, 10, 12));

        // ── 新增：Logout 按钮 ──
        Button logoutBtn = styledBtn("⏻ Logout", "#f4a261");
        logoutBtn.setOnAction(e -> handleLogout());
        logoutBtn.setPadding(new Insets(10, 12, 10, 12));

        Button quitBtn = styledBtn("✕ Quit", "#e94560");
        quitBtn.setOnAction(e -> { db.disconnect(); Platform.exit(); });
        quitBtn.setPadding(new Insets(10, 12, 10, 12));

        HBox btnBox = new HBox(0, userLabel, newMapBtn, logoutBtn, quitBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);
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

    private void handleKey(String key) {
        if (mazeInProgress) {
            switch (key) {
                case "DIGIT1": case "NUMPAD1":
                    useItemInMaze(Item.ItemType.SPEED_BOOST); return;
                case "DIGIT2": case "NUMPAD2":
                    useItemInMaze(Item.ItemType.WALL_PASS);   return;
                case "DIGIT3": case "NUMPAD3":
                    useItemInMaze(Item.ItemType.ATTACK);      return;
                case "U":
                    updateStatus("Undo is not yet integrated with physical maze movement.");
                    return;
            }
        }

        int dr = 0, dc = 0;
        switch (key) {
            case "W": case "UP":    dr = -1; break;
            case "S": case "DOWN":  dr =  1; break;
            case "A": case "LEFT":  dc = -1; break;
            case "D": case "RIGHT": dc =  1; break;
            case "F":
                if (mazeInProgress) forceNodeFail();
                return;
            default: return;
        }

        if (currentMaze != null && mazeInProgress && (dr != 0 || dc != 0)) {
            int oldR = currentMaze.getPlayerRow();
            int oldC = currentMaze.getPlayerCol();

            boolean moved = currentMaze.movePlayer(dr, dc);

            if (moved && speedBoostTicks > 0) {
                currentMaze.movePlayer(dr, dc);
            }

            if (!moved && wallPassActive) {
                int targetR = oldR + dr;
                int targetC = oldC + dc;
                if (targetR >= 0 && targetR < currentMaze.getRows()
                        && targetC >= 0 && targetC < currentMaze.getCols()
                        && currentMaze.isWall(targetR, targetC)) {
                    int landR = targetR + dr;
                    int landC = targetC + dc;
                    if (landR >= 0 && landR < currentMaze.getRows()
                            && landC >= 0 && landC < currentMaze.getCols()
                            && !currentMaze.isWall(landR, landC)) {
                        currentMaze.carveCell(targetR, targetC);
                        currentMaze.movePlayer(dr, dc);
                        currentMaze.movePlayer(dr, dc);
                        moved = true;
                        wallPassActive = false;
                        updateStatus("🧱 Wall Pass used! You phased through a wall!");
                    }
                }
            }

            if (moved) {
                gameTick++;
                int pR = currentMaze.getPlayerRow();
                int pC = currentMaze.getPlayerCol();
                checkItemPickup(pR, pC);

                for (Enemy e : enemies) {
                    e.update(currentMaze, pR, pC, gameTick);
                }
                for (Enemy e : enemies) {
                    if (!e.isStunned() && e.isAtPlayer(pR, pC)) {
                        e.stun(10);
                        boolean dead = gameState.onNodeFailed();
                        refreshAll();
                        if (dead) {
                            mazeInProgress = false;
                            currentMaze = null;
                            enemies = new java.util.ArrayList<>();
                            itemsOnMap = new java.util.HashMap<>();
                            updateStatus("☠ Caught by enemy! All lives lost. Score RESET.");
                            drawWelcome();
                            return;
                        } else {
                            updateStatus("Caught by enemy! Lives: " + gameState.getLives()
                                    + "  |  Click the same node to retry.");
                            startMazeForNode(currentLevel);
                            return;
                        }
                    }
                }
                renderMaze();
                if (currentMaze.isPlayerAtExit()) {
                    mazeInProgress = false;
                    enemies = new java.util.ArrayList<>();
                    itemsOnMap = new java.util.HashMap<>();
                    gameState.onNodeCompleted();
                    refreshAll();
                    Level curr = gameState.getCurrentNode();
                    if (curr != null && curr.isStart()) {
                        // ── 新增：通关写入 MySQL ──
                        List<Integer> top = gameState.getTopK(1);
                        if (!top.isEmpty() && auth.getCurrentUser() != null) {
                            db.saveScore(auth.getCurrentUser(), top.get(0),
                                    currentLevel.getLevelId());
                        }
                        // Bug fix: generate a fresh map so completed nodes don't persist
                        gameState.generateNewMap();
                        refreshAll();
                        updateStatus("Path complete! Score saved. New map generated – choose a path!");
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

    // ──────────────────────────────────────────────
    //  新增：Logout 处理
    // ──────────────────────────────────────────────

    private void handleLogout() {
        auth.logout();
        loginPanel.reset();
        primaryStage.setScene(loginScene);
        updateStatus("Logged out.");
    }

    // ──── 穿墙 / 加速状态 ────
    private boolean wallPassActive  = false;
    private java.util.Map<Item.ItemType, Integer> playerItems = new java.util.HashMap<>();

    private void useItemInMaze(Item.ItemType type) {
        int count = playerItems.getOrDefault(type, 0);
        if (count <= 0) {
            updateStatus("You don't have " + type.displayName + "! Pick one up in the maze.");
            return;
        }
        switch (type) {
            case SPEED_BOOST:
                playerItems.put(type, count - 1);
                speedBoostTicks = 10;
                updateStatus("⚡ Speed Boost activated! Double-step for 10 moves!");
                break;
            case WALL_PASS:
                if (wallPassActive) {
                    updateStatus("🧱 Wall Pass already ready!");
                    return;
                }
                playerItems.put(type, count - 1);
                wallPassActive = true;
                updateStatus("🧱 Wall Pass ready! Move towards a wall to phase through it!");
                break;
            case ATTACK:
                playerItems.put(type, count - 1);
                int stunCount = 0;
                int pR = currentMaze.getPlayerRow();
                int pC = currentMaze.getPlayerCol();
                for (Enemy e : enemies) {
                    int dist = Math.abs(e.getRow() - pR) + Math.abs(e.getCol() - pC);
                    if (dist <= 5 && !e.isStunned()) {
                        e.stun(8);
                        stunCount++;
                    }
                }
                if (stunCount > 0) {
                    updateStatus("⚔ Attack! Stunned " + stunCount + " enem"
                            + (stunCount == 1 ? "y" : "ies") + " for 8 ticks!");
                } else {
                    updateStatus("⚔ Attack! No enemies within range 5.");
                }
                renderMaze();
                break;
        }
        updateInventoryDisplay();
    }

    private int speedBoostTicks = 0;

    private void checkItemPickup(int r, int c) {
        String key = r + "," + c;
        if (itemsOnMap.containsKey(key)) {
            Item.ItemType type = itemsOnMap.remove(key);
            int current = playerItems.getOrDefault(type, 0);
            playerItems.put(type, current + 1);
            updateStatus("Picked up: " + type.icon + " " + type.displayName
                    + "! Press " + itemKeyHint(type) + " to use.  "
                    + "Inventory: " + inventorySummary());
        }
        if (speedBoostTicks > 0) {
            speedBoostTicks--;
            if (speedBoostTicks <= 0) updateStatus("Speed Boost expired.");
        }
        updateInventoryDisplay();
    }

    private String itemKeyHint(Item.ItemType type) {
        switch (type) {
            case SPEED_BOOST: return "[1]";
            case WALL_PASS:   return "[2]";
            case ATTACK:      return "[3]";
            default:          return "[?]";
        }
    }

    private String inventorySummary() {
        StringBuilder sb = new StringBuilder();
        for (Item.ItemType t : Item.ItemType.values()) {
            int count = playerItems.getOrDefault(t, 0);
            if (count > 0) {
                if (sb.length() > 0) sb.append("  ");
                sb.append(t.icon).append("x").append(count);
            }
        }
        return sb.length() > 0 ? sb.toString() : "(empty)";
    }

    private void updateInventoryDisplay() {
        List<InventoryPanel.Item> displayItems = new java.util.ArrayList<>();
        for (Item.ItemType type : Item.ItemType.values()) {
            int count = playerItems.getOrDefault(type, 0);
            if (count > 0) {
                displayItems.add(new InventoryPanel.Item(
                        type.displayName, "Consumable", rarityForType(type), count));
            }
        }
        inventoryPanel.setItems(displayItems);

        List<String> activeBuffs = new java.util.ArrayList<>();
        if (speedBoostTicks > 0)  activeBuffs.add("⚡Speed(" + speedBoostTicks + ")");
        if (wallPassActive)       activeBuffs.add("🧱WallPass");
        playerHUD.setActiveBuffs(activeBuffs);
    }

    private String rarityForType(Item.ItemType type) {
        switch (type) {
            case SPEED_BOOST: return "Common";
            case WALL_PASS:   return "Rare";
            case ATTACK:      return "Epic";
            default:          return "Common";
        }
    }

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

    private static Difficulty toMazeDifficulty(Level.Difficulty d) {
        if (d == null) return Difficulty.EASY;
        switch (d) {
            case EASY:   return Difficulty.EASY;
            case MEDIUM: return Difficulty.MEDIUM;
            default:     return Difficulty.HARD;
        }
    }

    private static int[] mazeLogSize(Level.Difficulty d) {
        if (d == null) return new int[]{5, 5};
        switch (d) {
            case EASY:   return new int[]{5, 5};
            case MEDIUM: return new int[]{7, 7};
            default:     return new int[]{9, 9};
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