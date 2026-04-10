package GUI;

import Model.GameEvent;
import Model.GameEvent.EventType;
import Model.Level;
import World.GameMap;
import World.GameStateController;

import Implementation.ArrayList;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * LevelPanel — 左侧关卡地图面板。
 *
 * 重构后通过 GameEvent.Bus 自我刷新，外部无需调用任何方法。
 * 订阅事件：MAP_GENERATED / NODE_ENTERED / NODE_COMPLETED / NODE_FAILED /
 *           LIVES_CHANGED / SCORE_CHANGED
 */
public class LevelPanel extends VBox {

    @FunctionalInterface
    public interface NodeSelectCallback {
        void onSelected(Level node);
    }

    private static final Color COL_BG          = Color.web("#0f0f1a");
    private static final Color COL_EDGE         = Color.web("#404060");
    private static final Color COL_EDGE_AVAIL   = Color.web("#43cfff");
    private static final Color COL_START        = Color.web("#4d96ff");
    private static final Color COL_EASY         = Color.web("#6bcb77");
    private static final Color COL_MEDIUM       = Color.web("#ffd93d");
    private static final Color COL_HARD         = Color.web("#ff6b81");
    private static final Color COL_COMPLETED_OV = Color.web("#ffffff99");
    private static final Color COL_CURRENT_RING = Color.web("#ffffff");
    private static final Color COL_AVAIL_RING   = Color.web("#43cfff");
    private static final Color COL_LOCKED       = Color.web("#252535");
    private static final Color COL_TEXT         = Color.web("#e2e2f0");
    private static final Color COL_TEXT_DARK    = Color.web("#13131f");

    private final GameStateController gameState;
    private NodeSelectCallback        onNodeSelected;
    private final Canvas              mapCanvas;
    private final Label               livesLabel;
    private final Label               scoreLabel;

    public LevelPanel(GameStateController gameState) {
        this.gameState = gameState;

        setPrefWidth(220);
        setSpacing(0);
        setStyle("-fx-background-color:#13131f;"
                + "-fx-border-color:#383860;-fx-border-width:0 2 0 0;");

        livesLabel = new Label("♥ ♥ ♥");
        livesLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        livesLabel.setTextFill(Color.web("#ff6b81"));

        scoreLabel = new Label("Path score: 0");
        scoreLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 13));
        scoreLabel.setTextFill(Color.web("#ffd93d"));

        VBox statusBox = new VBox(3, livesLabel, scoreLabel);
        statusBox.setPadding(new Insets(7, 12, 7, 12));
        statusBox.setStyle("-fx-background-color:#1a1a2e;"
                + "-fx-border-color:#383860;-fx-border-width:0 0 1 0;");

        mapCanvas = new Canvas(GameMap.CANVAS_W, GameMap.CANVAS_H);
        mapCanvas.setOnMouseClicked(e -> handleClick(e.getX(), e.getY()));

        VBox canvasBox = new VBox(mapCanvas);
        canvasBox.setStyle("-fx-background-color:#0d0d1a;");
        VBox.setVgrow(canvasBox, Priority.ALWAYS);

        getChildren().addAll(buildHeader(), statusBox, canvasBox, buildLegend());

        // ── 订阅事件 ────────────────────────────────────────────────────
        GameEvent.Bus bus = GameEvent.Bus.get();

        // 地图结构变化 → 重绘整张地图
        bus.subscribe(EventType.MAP_GENERATED,   e -> Platform.runLater(this::drawMap));
        bus.subscribe(EventType.NODE_ENTERED,    e -> Platform.runLater(this::drawMap));
        bus.subscribe(EventType.NODE_COMPLETED,  e -> Platform.runLater(this::drawMap));
        bus.subscribe(EventType.NODE_FAILED,     e -> Platform.runLater(this::drawMap));
        bus.subscribe(EventType.GAME_OVER,       e -> Platform.runLater(this::drawMap));

        // 状态标签变化 → 更新生命/分数显示
        bus.subscribe(EventType.LIVES_CHANGED,   e -> Platform.runLater(this::updateStatusLabels));
        bus.subscribe(EventType.SCORE_CHANGED,   e -> Platform.runLater(this::updateStatusLabels));
        bus.subscribe(EventType.NODE_COMPLETED,  e -> Platform.runLater(this::updateStatusLabels));
    }

    public void setOnNodeSelected(NodeSelectCallback cb) { this.onNodeSelected = cb; }

    // ── Drawing ──────────────────────────────────────────────────────────

    private void drawMap() {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        gc.setFill(COL_BG);
        gc.fillRect(0, 0, GameMap.CANVAS_W, GameMap.CANVAS_H);

        GameMap map = gameState.getCurrentMap();
        if (map == null) {
            gc.setFill(COL_TEXT);
            gc.setFont(Font.font("Arial", 14));
            gc.fillText("No map generated.\nPress  [ New Map ].", 20, 200);
            return;
        }

        Level            currentNode = gameState.getCurrentNode();
        ArrayList<Level> avail       = map.getChildren(currentNode);
        ArrayList<Level> allNodes    = map.getAllNodes();

        for (Level node : allNodes) {
            double[] pPos = map.getPosition(node);
            if (pPos == null) continue;
            for (Level child : map.getChildren(node)) {
                double[] cPos = map.getPosition(child);
                if (cPos == null) continue;
                boolean isAvailEdge = avail.contains(child) && node.equals(currentNode);
                drawEdge(gc, pPos[0], pPos[1], cPos[0], cPos[1], isAvailEdge, child.isCompleted());
            }
        }

        for (Level node : allNodes) {
            double[] pos = map.getPosition(node);
            if (pos == null) continue;
            drawNode(gc, pos[0], pos[1], node, node.equals(currentNode), avail.contains(node));
        }
    }

    private void drawEdge(GraphicsContext gc,
                          double x1, double y1, double x2, double y2,
                          boolean available, boolean completed) {
        gc.setLineWidth(available ? 2.0 : 1.2);
        gc.setStroke(available ? COL_EDGE_AVAIL : (completed ? COL_EDGE.brighter() : COL_EDGE));
        gc.setLineDashes(available || completed ? null : new double[]{5, 5});
        gc.strokeLine(x1, y1, x2, y2);
        gc.setLineDashes((double[]) null);
    }

    private void drawNode(GraphicsContext gc, double cx, double cy,
                          Level node, boolean isCurrent, boolean isAvailable) {
        double r = GameMap.NODE_RADIUS;

        if (isCurrent) {
            gc.setStroke(COL_CURRENT_RING); gc.setLineWidth(3);
            gc.strokeOval(cx - r - 4, cy - r - 4, (r + 4) * 2, (r + 4) * 2);
        } else if (isAvailable) {
            gc.setStroke(COL_AVAIL_RING); gc.setLineWidth(2.5);
            gc.strokeOval(cx - r - 3, cy - r - 3, (r + 3) * 2, (r + 3) * 2);
        }

        Color fill = nodeColour(node);
        if (!isCurrent && !isAvailable && !node.isCompleted() && !node.isStart())
            fill = COL_LOCKED;
        gc.setFill(fill);
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);
        gc.setStroke(fill.brighter()); gc.setLineWidth(1.5);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);

        if (node.isCompleted()) {
            gc.setFill(COL_COMPLETED_OV);
            gc.fillOval(cx - r, cy - r, r * 2, r * 2);
            gc.setFill(COL_TEXT_DARK);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 13));
            gc.fillText("✓", cx - 5, cy + 5);
            return;
        }

        if (node.isStart()) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 9));
            gc.fillText("START", cx - 13, cy + 4);
        } else {
            gc.setFill(COL_TEXT_DARK);
            gc.setFont(Font.font("Arial", FontWeight.BOLD, 10));
            String pts = "+" + node.getScoreValue();
            gc.fillText(pts, cx - (pts.length() * 3), cy + 4);
        }
    }

    private Color nodeColour(Level node) {
        if (node.isStart() || node.getDifficulty() == null) return COL_START;
        switch (node.getDifficulty()) {
            case EASY:   return COL_EASY;
            case MEDIUM: return COL_MEDIUM;
            case HARD:   return COL_HARD;
            default:     return COL_TEXT;
        }
    }

    // ── Click handling ───────────────────────────────────────────────────

    private void handleClick(double mx, double my) {
        GameMap map = gameState.getCurrentMap();
        if (map == null || onNodeSelected == null) return;

        Level current = gameState.getCurrentNode();
        ArrayList<Level> available = map.getChildren(current);

        for (Level node : available) {
            double[] pos = map.getPosition(node);
            if (pos == null) continue;
            double dx = mx - pos[0], dy = my - pos[1];
            if (dx * dx + dy * dy <= (GameMap.NODE_RADIUS + 6) * (GameMap.NODE_RADIUS + 6)) {
                onNodeSelected.onSelected(node);
                return;
            }
        }
    }

    // ── Status labels ────────────────────────────────────────────────────

    private void updateStatusLabels() {
        int lives = gameState.getLives();
        StringBuilder hearts = new StringBuilder();
        for (int i = 0; i < GameStateController.INITIAL_LIVES; i++)
            hearts.append(i < lives ? "♥ " : "♡ ");
        livesLabel.setText(hearts.toString().trim());
        scoreLabel.setText("Path score: " + gameState.getAccumulatedScore());
    }

    // ── UI builders ──────────────────────────────────────────────────────

    private VBox buildHeader() {
        VBox header = new VBox(4);
        header.setPadding(new Insets(10, 12, 8, 12));
        header.setStyle("-fx-background-color:#1a1a2e;"
                + "-fx-border-color:#383860;-fx-border-width:0 0 1 0;");

        Label icon = new Label("MAP  —  Level Selection");
        icon.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        icon.setTextFill(Color.web("#43cfff"));

        Label sub = new Label("Click a highlighted node to enter");
        sub.setFont(Font.font("Arial", 11));
        sub.setTextFill(Color.web("#b0b0cc"));

        header.getChildren().addAll(icon, sub);
        return header;
    }

    private VBox buildLegend() {
        VBox legend = new VBox(4);
        legend.setPadding(new Insets(6, 12, 8, 12));
        legend.setStyle("-fx-background-color:#1a1a2e;"
                + "-fx-border-color:#383860;-fx-border-width:1 0 0 0;");
        legend.getChildren().addAll(
                legendRow("● Easy   +20 pts",              "#6bcb77"),
                legendRow("● Medium +30 pts",              "#ffd93d"),
                legendRow("● Hard   +50 pts",              "#ff6b81"),
                legendRow("○ Highlighted = available",     "#43cfff")
        );
        return legend;
    }

    private Label legendRow(String text, String colour) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 11));
        lbl.setTextFill(Color.web(colour));
        return lbl;
    }
}