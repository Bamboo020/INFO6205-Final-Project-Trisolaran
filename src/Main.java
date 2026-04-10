import GUI.InventoryPanel;
import GUI.LeaderboardPanel;
import GUI.LevelPanel;
import GUI.LoginPanel;
import GUI.PlayerHUD;
import Maze.CellState;
import Maze.Difficulty;
import Maze.MazeGrid;
import Model.Enemy;
import Model.GameEvent;
import Model.GameEvent.EventType;
import Model.Item;
import Model.Level;
import Model.Player;
import World.AuthController;
import World.DBManager;
import World.GameStateController;

import Implementation.ArrayList;
import Implementation.HashMap;

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

public class Main extends Application {

    private static final int    W     = 1200;
    private static final int    H     = 760;
    private static final int    MAZE  = 570;
    private static final String TITLE = "Maze Explorer RPG – INFO 6205";

    private final GameEvent.Bus bus = GameEvent.Bus.get();

    private DBManager      db;
    private AuthController auth;

    private Stage      primaryStage;
    private Scene      loginScene;
    private Scene      gameScene;
    private LoginPanel loginPanel;

    private GameStateController gameState;

    private MazeGrid              currentMaze;
    private Level                 currentLevel;
    private boolean               mazeInProgress;
    private ArrayList<Enemy>      enemies    = new ArrayList<>();
    private HashMap<Item.ItemType> itemsOnMap = new HashMap<>();
    private int                   gameTick;

    private Player player = new Player();

    private PlayerHUD        playerHUD;
    private LevelPanel       levelPanel;
    private LeaderboardPanel leaderboardPanel;
    private InventoryPanel   inventoryPanel;

    private Canvas mazeCanvas;
    private Label  statusLabel;
    private Label  userLabel;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        db = new DBManager();
        String dbErr = db.connect();
        if (dbErr != null) System.out.println("[DB] " + dbErr + " – offline mode.");
        auth      = new AuthController(db);
        gameState = new GameStateController();

        loadHistoricalScores();
        buildLoginScene();
        buildGameScene();
        wireMainSubscriptions();

        stage.setTitle(TITLE);
        stage.setResizable(false);
        stage.setScene(loginScene);
        stage.show();
        stage.setOnCloseRequest(e -> db.disconnect());
    }

    private void wireMainSubscriptions() {
        bus.subscribe(EventType.STATUS_UPDATE,
                e -> Platform.runLater(() -> statusLabel.setText(e.getData())));

        bus.subscribe(EventType.GAME_OVER, e -> Platform.runLater(() -> {
            statusLabel.setText("☠ All lives lost! Generating new map...");
            startNewGame();
        }));

        bus.subscribe(EventType.NODE_FAILED, e -> Platform.runLater(() ->
                statusLabel.setText("✗ Failed! Lives: " + gameState.getLives()
                        + "  |  Click the same node to retry.")));

        bus.subscribe(EventType.NODE_COMPLETED, e -> Platform.runLater(() -> {
            int pathScore = e.getData().isEmpty() ? 0 : Integer.parseInt(e.getData());
            Level curr = gameState.getCurrentNode();
            if (curr != null && curr.isStart()) {
                gameState.generateNewMap();
                bus.publish(EventType.MAP_GENERATED);
                statusLabel.setText("Path complete!  +" + pathScore
                        + " pts saved.  New map – choose a path!");
                drawWelcome();
            } else {
                statusLabel.setText("Cleared! +" + currentLevel.getScoreValue()
                        + " pts  |  Accumulated: " + gameState.getAccumulatedScore()
                        + "  |  Click next node.");
            }
        }));

        bus.subscribe(EventType.MAP_GENERATED,
                e -> Platform.runLater(this::drawWelcome));

        bus.subscribe(EventType.NODE_ENTERED, e -> Platform.runLater(() -> {
            if (currentLevel == null) return;
            String diff = currentLevel.getDifficulty() != null
                    ? currentLevel.getDifficulty().toString() : "?";
            statusLabel.setText("Entered: " + currentLevel + "  [" + diff + "]"
                    + "  +" + currentLevel.getScoreValue() + " pts on completion"
                    + "  |  Lives: " + gameState.getLives()
                    + "  |  WASD=Move  1=Speed  2=WallPass  3=Attack  F=Fail");
        }));

        bus.subscribe(EventType.PLAYER_LOGGED_IN, e -> Platform.runLater(() -> {
            String username = e.getData();
            startNewGame();
            userLabel.setText("👤 " + username + (auth.isDbAvailable() ? "  🟢" : "  🟡"));
            primaryStage.setScene(gameScene);
        }));
    }

    private void startNewGame() {
        gameState.generateNewMap();
        currentMaze    = null;
        currentLevel   = null;
        mazeInProgress = false;
        enemies        = new ArrayList<>();
        itemsOnMap     = new HashMap<>();
        gameTick       = 0;
        player         = new Player();          // ← 替换三行手动清零
        bus.publish(EventType.MAP_GENERATED);
        bus.publish(EventType.STATUS_UPDATE,
                "New map generated! Click a highlighted node on the left to begin.");
    }

    private void onNodeSelected(Level node) {
        boolean moved = gameState.moveToNode(node);
        if (!moved) return;
        currentLevel   = node;
        mazeInProgress = false;
        startMazeForNode(node);
        bus.publish(EventType.NODE_ENTERED, node.toString());
    }

    private void startMazeForNode(Level node) {
        int[] size = mazeLogSize(node.getDifficulty());
        currentMaze = new MazeGrid(size[0], size[1]);
        currentMaze.generateMaze(toMazeDifficulty(node.getDifficulty()));
        mazeInProgress = true;
        gameTick       = 0;
        player.reset();                         // ← 替换三行手动清零
        spawnEnemies();
        placeItemsOnMaze();
        bus.publish(EventType.INVENTORY_CHANGED);
        publishBuffChanged();
        renderMaze();
    }

    private void handleLogout() {
        auth.logout();
        loginPanel.reset();
        primaryStage.setScene(loginScene);
        bus.publish(EventType.STATUS_UPDATE, "Logged out.");
        bus.publish(EventType.PLAYER_LOGGED_OUT);
    }

    private void buildLoginScene() {
        loginPanel = new LoginPanel(auth);
        loginPanel.setOnSuccess(username ->
                bus.publish(EventType.PLAYER_LOGGED_IN, username));
        loginScene = new Scene(loginPanel, W, H);
    }

    private void handleKey(String key) {
        if (mazeInProgress) {
            switch (key) {
                case "DIGIT1": case "NUMPAD1": useItemInMaze(Item.ItemType.SPEED_BOOST); return;
                case "DIGIT2": case "NUMPAD2": useItemInMaze(Item.ItemType.WALL_PASS);   return;
                case "DIGIT3": case "NUMPAD3": useItemInMaze(Item.ItemType.ATTACK);      return;
            }
        }
        int dr = 0, dc = 0;
        switch (key) {
            case "W": case "UP":    dr = -1; break;
            case "S": case "DOWN":  dr =  1; break;
            case "A": case "LEFT":  dc = -1; break;
            case "D": case "RIGHT": dc =  1; break;
            case "F": if (mazeInProgress) forceNodeFail(); return;
            default: return;
        }
        if (currentMaze != null && mazeInProgress) handleMove(dr, dc);
    }

    private void handleMove(int dr, int dc) {
        int oldR  = currentMaze.getPlayerRow();
        int oldC  = currentMaze.getPlayerCol();
        boolean moved = currentMaze.movePlayer(dr, dc);

        if (moved && player.isSpeedBoostActive())       // ← speedBoostTicks > 0
            currentMaze.movePlayer(dr, dc);

        if (!moved && player.isWallPassActive())         // ← wallPassActive
            moved = tryWallPass(oldR, oldC, dr, dc);

        if (!moved) return;

        gameTick++;
        int pR = currentMaze.getPlayerRow();
        int pC = currentMaze.getPlayerCol();
        checkItemPickup(pR, pC);
        tickEnemies(pR, pC);
        renderMaze();

        if (currentMaze != null && currentMaze.isPlayerAtExit()) onMazeCompleted();
    }

    private boolean tryWallPass(int oldR, int oldC, int dr, int dc) {
        int targetR = oldR + dr, targetC = oldC + dc;
        if (targetR < 0 || targetR >= currentMaze.getRows()
                || targetC < 0 || targetC >= currentMaze.getCols()
                || !currentMaze.isWall(targetR, targetC)) return false;
        int landR = targetR + dr, landC = targetC + dc;
        if (landR < 0 || landR >= currentMaze.getRows()
                || landC < 0 || landC >= currentMaze.getCols()
                || currentMaze.isWall(landR, landC)) return false;
        currentMaze.carveCell(targetR, targetC);
        currentMaze.movePlayer(dr, dc);
        currentMaze.movePlayer(dr, dc);
        player.consumeWallPass();
        bus.publish(EventType.STATUS_UPDATE, "🧱 Wall Pass used! You phased through a wall!");
        publishBuffChanged();
        return true;
    }

    private void tickEnemies(int pR, int pC) {
        for (Enemy e : enemies) e.update(currentMaze, pR, pC, gameTick);
        for (Enemy e : enemies) {
            if (!e.isStunned() && e.isAtPlayer(pR, pC)) {
                e.stun(10);
                boolean dead = gameState.onNodeFailed();
                bus.publish(EventType.LIVES_CHANGED, String.valueOf(gameState.getLives()));
                if (dead) {
                    mazeInProgress = false;
                    currentMaze    = null;
                    bus.publish(EventType.GAME_OVER);
                } else {
                    bus.publish(EventType.NODE_FAILED);
                    startMazeForNode(currentLevel);
                }
                return;
            }
        }
    }

    private void checkItemPickup(int r, int c) {
        String key = r + "," + c;
        if (itemsOnMap.containsKeyObj(key)) {
            Item.ItemType type = itemsOnMap.remove(key);
            player.addItem(type);
            bus.publish(EventType.ITEM_PICKUP, type.name());
            bus.publish(EventType.STATUS_UPDATE,
                    "Picked up: " + type.icon + " " + type.displayName
                            + "! Press " + itemKeyHint(type) + " to use.");
            bus.publish(EventType.INVENTORY_CHANGED);
        }
        if (player.isSpeedBoostActive()) {
            int remaining = player.tickSpeedBoost();
            if (remaining <= 0) {
                bus.publish(EventType.STATUS_UPDATE, "Speed Boost expired.");
                bus.publish(EventType.ITEM_EXPIRE, Item.ItemType.SPEED_BOOST.name());
                publishBuffChanged();
            }
        }
    }

    private void onMazeCompleted() {
        mazeInProgress = false;
        enemies        = new ArrayList<>();
        itemsOnMap     = new HashMap<>();
        gameState.onNodeCompleted();
        int pathScore = gameState.getLastPathScore();
        if (pathScore > 0 && auth.getCurrentUser() != null)
            db.saveScore(auth.getCurrentUser(), pathScore, currentLevel.getLevelId());
        bus.publish(EventType.NODE_COMPLETED, String.valueOf(pathScore));
        bus.publish(EventType.SCORE_CHANGED,  String.valueOf(gameState.getAccumulatedScore()));
        currentMaze = null;
    }

    private void forceNodeFail() {
        mazeInProgress = false;
        currentMaze    = null;
        boolean dead   = gameState.onNodeFailed();
        bus.publish(EventType.LIVES_CHANGED, String.valueOf(gameState.getLives()));
        bus.publish(dead ? EventType.GAME_OVER : EventType.NODE_FAILED);
    }

    private void useItemInMaze(Item.ItemType type) {
        if (player.getItemCount(type) <= 0) {
            bus.publish(EventType.STATUS_UPDATE,
                    "You don't have " + type.displayName + "! Pick one up in the maze.");
            return;
        }
        switch (type) {
            case SPEED_BOOST:
                player.useItem(type);
                player.activateSpeedBoost(Player.SPEED_BOOST_DURATION);
                bus.publish(EventType.STATUS_UPDATE,
                        "⚡ Speed Boost activated! Double-step for 10 moves!");
                break;
            case WALL_PASS:
                if (player.isWallPassActive()) {
                    bus.publish(EventType.STATUS_UPDATE, "🧱 Wall Pass already ready!"); return;
                }
                player.useItem(type);
                player.activateWallPass();
                bus.publish(EventType.STATUS_UPDATE,
                        "🧱 Wall Pass ready! Move towards a wall to phase through it!");
                break;
            case ATTACK:
                player.useItem(type);
                int stunCount = 0;
                int pR = currentMaze.getPlayerRow(), pC = currentMaze.getPlayerCol();
                for (Enemy e : enemies) {
                    if (Math.abs(e.getRow()-pR)+Math.abs(e.getCol()-pC) <= Player.ATTACK_RADIUS
                            && !e.isStunned()) {
                        e.stun(8); stunCount++;
                    }
                }
                bus.publish(EventType.STATUS_UPDATE,
                        stunCount > 0
                                ? "⚔ Attack! Stunned " + stunCount
                                + " enem" + (stunCount == 1 ? "y" : "ies") + " for 8 ticks!"
                                : "⚔ Attack! No enemies within range " + Player.ATTACK_RADIUS + ".");
                renderMaze();
                break;
        }
        bus.publish(EventType.INVENTORY_CHANGED);
        publishBuffChanged();
    }

    private ArrayList<InventoryPanel.Item> buildInventoryItems() {
        ArrayList<InventoryPanel.Item> list = new ArrayList<>();
        for (Model.Item item : player.getInventory()) {
            list.add(new InventoryPanel.Item(
                    item.getName(), "Consumable", rarityForType(item.getType()), item.getQuantity()));
        }
        return list;
    }

    private void publishBuffChanged() {
        StringBuilder sb = new StringBuilder();
        int ticks = player.getSpeedBoostTicks();
        if (ticks > 0) sb.append("⚡Speed(").append(ticks).append(')');
        if (player.isWallPassActive()) {
            if (sb.length() > 0) sb.append(',');
            sb.append("🧱WallPass");
        }
        bus.publish(EventType.BUFF_CHANGED, sb.toString());
    }

    private void spawnEnemies() {
        enemies = new ArrayList<>();
        if (currentMaze == null) return;
        int physRows = currentMaze.getRows(), physCols = currentMaze.getCols();
        int pR = currentMaze.getPlayerRow(), pC = currentMaze.getPlayerCol();
        ArrayList<int[]> candidates = new ArrayList<>();
        for (int r = 0; r < physRows; r++)
            for (int c = 0; c < physCols; c++)
                if (!currentMaze.isWall(r, c) && Math.abs(r-pR)+Math.abs(c-pC) >= 8)
                    candidates.add(new int[]{r, c});
        java.util.Random rng = new java.util.Random();
        for (int i = candidates.size()-1; i > 0; i--) {
            int j = rng.nextInt(i+1);
            int[] tmp = candidates.get(i); candidates.set(i, candidates.get(j)); candidates.set(j, tmp);
        }
        for (int i = 0; i < Math.min(3, candidates.size()); i++) {
            int[] cell = candidates.get(i);
            enemies.add(new Enemy(i, cell[0], cell[1], 8, 2));
        }
    }

    private void placeItemsOnMaze() {
        itemsOnMap = new HashMap<>();
        if (currentMaze == null) return;
        int physRows = currentMaze.getRows(), physCols = currentMaze.getCols();
        int exitR = (currentMaze.getLogRows()-1)*4+1, exitC = (currentMaze.getLogCols()-1)*4+1;
        ArrayList<int[]> openCells = new ArrayList<>();
        for (int r = 0; r < physRows; r++)
            for (int c = 0; c < physCols; c++)
                if (!currentMaze.isWall(r,c) && !(r==1&&c==1) && !(r==exitR&&c==exitC))
                    openCells.add(new int[]{r, c});
        java.util.Random rng = new java.util.Random();
        for (int i = openCells.size()-1; i > 0; i--) {
            int j = rng.nextInt(i+1);
            int[] tmp = openCells.get(i); openCells.set(i, openCells.get(j)); openCells.set(j, tmp);
        }
        int idx = 0;
        for (Item.ItemType t : Item.ItemType.values())
            for (int i = 0; i < 2 && idx < openCells.size(); i++, idx++) {
                int[] cell = openCells.get(idx);
                itemsOnMap.put(cell[0]+","+cell[1], t);
            }
    }

    private void renderMaze() {
        if (currentMaze == null) return;
        GraphicsContext gc = mazeCanvas.getGraphicsContext2D();
        int physRows = currentMaze.getRows(), physCols = currentMaze.getCols();
        double cellW = (double)MAZE/physCols, cellH = (double)MAZE/physRows;
        gc.setFill(Color.web("#0d0d1a")); gc.fillRect(0, 0, MAZE, MAZE);
        for (int r = 0; r < physRows; r++) {
            for (int c = 0; c < physCols; c++) {
                double x = c*cellW, y = r*cellH;
                if (currentMaze.isWall(r,c)) {
                    gc.setFill(wallColor()); gc.fillRect(x, y, cellW, cellH);
                } else {
                    CellState state = currentMaze.getCell(r, c);
                    switch (state) {
                        case PLAYER:
                            gc.setFill(Color.web("#1e2a4a")); gc.fillRect(x, y, cellW, cellH);
                            gc.setFill(Color.web("#57cc99"));
                            gc.fillOval(x+cellW*.1, y+cellH*.1, cellW*.8, cellH*.8); break;
                        case EXIT:
                            gc.setFill(Color.web("#1e2a4a")); gc.fillRect(x, y, cellW, cellH);
                            gc.setFill(Color.web("#f4d35e"));
                            gc.fillOval(x+cellW*.2, y+cellH*.2, cellW*.6, cellH*.6); break;
                        default:
                            gc.setFill(Color.web("#1e2a4a")); gc.fillRect(x, y, cellW, cellH); break;
                    }
                    String key = r+","+c;
                    if (itemsOnMap.containsKeyObj(key))
                        drawItemIcon(gc, x, y, cellW, cellH, itemsOnMap.get(key));
                }
            }
        }
        for (Enemy e : enemies) {
            int er = e.getRow(), ec = e.getCol();
            if (currentMaze.isWall(er, ec)) continue;
            double ex = ec*cellW, ey = er*cellH;
            gc.setFill(e.isStunned() ? Color.web("#888888")
                    : e.getState() == Enemy.AIState.CHASE ? Color.web("#ff2244") : Color.web("#e94560"));
            gc.fillOval(ex+cellW*.15, ey+cellH*.15, cellW*.7, cellH*.7);
        }
    }

    private void drawItemIcon(GraphicsContext gc, double x, double y,
                              double w, double h, Item.ItemType type) {
        double cx = x+w/2, cy = y+h/2, size = Math.min(w,h)*.35;
        Color color;
        switch (type) {
            case SPEED_BOOST: color = Color.web("#00e5ff"); break;
            case WALL_PASS:   color = Color.web("#ff9800"); break;
            case ATTACK:      color = Color.web("#ff4081"); break;
            default:          color = Color.WHITE; break;
        }
        gc.setFill(color);
        gc.fillPolygon(new double[]{cx,cx+size,cx,cx-size}, new double[]{cy-size,cy,cy+size,cy}, 4);
        gc.setStroke(Color.WHITE); gc.setLineWidth(1);
        gc.strokePolygon(new double[]{cx,cx+size,cx,cx-size}, new double[]{cy-size,cy,cy+size,cy}, 4);
    }

    private void drawWelcome() {
        if (mazeCanvas == null) return;
        GraphicsContext gc = mazeCanvas.getGraphicsContext2D();
        gc.setFill(Color.web("#0f0f1a")); gc.fillRect(0, 0, MAZE, MAZE);
        gc.setStroke(Color.web("#252540")); gc.setLineWidth(1);
        for (int x = 0; x <= MAZE; x += 30) gc.strokeLine(x, 0, x, MAZE);
        for (int y = 0; y <= MAZE; y += 30) gc.strokeLine(0, y, MAZE, y);
        gc.setFill(Color.web("#ff6b81"));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 26));
        gc.fillText("MAZE EXPLORER RPG", 105, MAZE/2.0-24);
        gc.setFill(Color.web("#e2e2f0"));
        gc.setFont(Font.font("Arial", 15));
        gc.fillText("Select a node on the map to begin", 115, MAZE/2.0+16);
    }

    private Color wallColor() {
        if (currentLevel == null || currentLevel.getDifficulty() == null) return Color.web("#334466");
        switch (currentLevel.getDifficulty()) {
            case EASY:   return Color.web("#1a3a2a");
            case MEDIUM: return Color.web("#2a2a1a");
            case HARD:   return Color.web("#3a1a1a");
            default:     return Color.web("#334466");
        }
    }

    private void buildGameScene() {
        playerHUD        = new PlayerHUD(gameState);
        levelPanel       = new LevelPanel(gameState);
        leaderboardPanel = new LeaderboardPanel(gameState, db);
        inventoryPanel   = new InventoryPanel(gameState, this::buildInventoryItems);
        levelPanel.setOnNodeSelected(this::onNodeSelected);

        mazeCanvas = new Canvas(MAZE, MAZE);
        BorderPane root = buildLayout();
        gameScene = new Scene(root, W, H);
        gameScene.setFill(Color.web("#13131f"));
        gameScene.setOnKeyPressed(e -> handleKey(e.getCode().toString()));
    }

    private BorderPane buildLayout() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#13131f;");
        root.setTop(buildTopBar());
        root.setLeft(levelPanel);
        root.setCenter(buildMazePane());
        root.setRight(buildRightColumn());
        root.setBottom(buildStatusBar());
        return root;
    }

    private HBox buildTopBar() {
        userLabel = new Label("👤 —");
        userLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        userLabel.setTextFill(Color.web("#e2e2f0"));
        userLabel.setPadding(new Insets(10, 10, 10, 14));

        Button newMapBtn = styledBtn("↺  New Map", "#6bcb77");
        newMapBtn.setOnAction(e -> startNewGame());
        newMapBtn.setPadding(new Insets(10, 14, 10, 14));

        Button logoutBtn = styledBtn("⏻  Logout", "#ffd93d");
        logoutBtn.setOnAction(e -> handleLogout());
        logoutBtn.setPadding(new Insets(10, 14, 10, 14));

        Button quitBtn = styledBtn("✕  Quit", "#ff6b81");
        quitBtn.setOnAction(e -> { db.disconnect(); Platform.exit(); });
        quitBtn.setPadding(new Insets(10, 14, 10, 14));

        HBox btnBox = new HBox(0, userLabel, newMapBtn, logoutBtn, quitBtn);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        btnBox.setStyle("-fx-background-color:#1a1a2e;"
                + "-fx-border-color:#383860;-fx-border-width:0 0 2 0;");

        HBox bar = new HBox(0);
        HBox.setHgrow(playerHUD, Priority.ALWAYS);
        bar.getChildren().addAll(playerHUD, btnBox);
        return bar;
    }

    private StackPane buildMazePane() {
        StackPane wrap = new StackPane(mazeCanvas);
        wrap.setStyle("-fx-background-color:#252540;");
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
        bar.setStyle("-fx-background-color:#1a1a2e;-fx-border-color:#383860;-fx-border-width:1 0 0 0;");
        statusLabel = new Label("Initialising…");
        statusLabel.setFont(Font.font("Arial", 13));
        statusLabel.setTextFill(Color.web("#e2e2f0"));
        bar.getChildren().add(statusLabel);
        return bar;
    }

    private void loadHistoricalScores() {
        ArrayList<DBManager.ScoreRecord> rows = db.loadAllScores();
        for (DBManager.ScoreRecord row : rows)
            gameState.recordScore(row.score,
                    new Model.GameRecord(row.username, row.score, row.levelId, 0L));
        System.out.println("[DB] Loaded " + rows.size() + " historical scores.");
    }

    private static Difficulty toMazeDifficulty(Level.Difficulty d) {
        if (d == null) return Difficulty.EASY;
        switch (d) { case EASY: return Difficulty.EASY; case MEDIUM: return Difficulty.MEDIUM; default: return Difficulty.HARD; }
    }

    private static int[] mazeLogSize(Level.Difficulty d) {
        if (d == null) return new int[]{5,5};
        switch (d) { case EASY: return new int[]{5,5}; case MEDIUM: return new int[]{7,7}; default: return new int[]{9,9}; }
    }

    private static String itemKeyHint(Item.ItemType type) {
        switch (type) { case SPEED_BOOST: return "[1]"; case WALL_PASS: return "[2]"; case ATTACK: return "[3]"; default: return "[?]"; }
    }

    private static String rarityForType(Item.ItemType type) {
        switch (type) { case SPEED_BOOST: return "Common"; case WALL_PASS: return "Rare"; case ATTACK: return "Epic"; default: return "Common"; }
    }

    private Button styledBtn(String text, String col) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        btn.setTextFill(Color.web(col));
        String n = "-fx-background-color:#1e1e30;-fx-border-color:" + col
                + ";-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-cursor:hand;";
        String h = "-fx-background-color:" + col + ";-fx-border-color:" + col
                + ";-fx-border-width:1;-fx-border-radius:4;-fx-background-radius:4;-fx-cursor:hand;";
        btn.setStyle(n);
        btn.setOnMouseEntered(e -> { btn.setStyle(h); btn.setTextFill(Color.web("#13131f")); });
        btn.setOnMouseExited (e -> { btn.setStyle(n); btn.setTextFill(Color.web(col));       });
        return btn;
    }

    public static void main(String[] args) { launch(args); }
}