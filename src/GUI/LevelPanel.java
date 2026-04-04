package GUI;

import Model.Level;
import World.GameMap;
import World.GameStateController;

import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

/**
 * C7 – Level-selection map panel.
 *
 * Renders the randomly generated GameMap on a Canvas:
 *   Green  = EASY (+20 pts)
 *   Yellow = MEDIUM (+30 pts)
 *   Red    = HARD (+50 pts)
 *   Blue   = START
 *
 * Clicking an available node fires the onNodeSelected callback.
 */
public class LevelPanel extends VBox {

    @FunctionalInterface
    public interface NodeSelectCallback {
        void onSelected(Level node);
    }

    private static final Color COL_BG          = Color.web("#0d0d1a");
    private static final Color COL_EDGE         = Color.web("#334466");
    private static final Color COL_EDGE_AVAIL   = Color.web("#a8dadc");
    private static final Color COL_START        = Color.web("#4a90d9");
    private static final Color COL_EASY         = Color.web("#57cc99");
    private static final Color COL_MEDIUM       = Color.web("#f4d35e");
    private static final Color COL_HARD         = Color.web("#e94560");
    private static final Color COL_COMPLETED_OV = Color.web("#ffffff88");
    private static final Color COL_CURRENT_RING = Color.web("#ffffff");
    private static final Color COL_AVAIL_RING   = Color.web("#ffe066");
    private static final Color COL_LOCKED       = Color.web("#2a2a3a");
    private static final Color COL_TEXT         = Color.web("#a8dadc");
    private static final Color COL_TEXT_DARK    = Color.web("#1a1a2e");

    private final GameStateController gameState;
    private NodeSelectCallback        onNodeSelected;
    private final Canvas              mapCanvas;
    private final Label               livesLabel;
    private final Label               scoreLabel;

    public LevelPanel(GameStateController gameState) {
        this.gameState = gameState;

        setPrefWidth(300);
        setSpacing(0);
        setStyle("-fx-background-color: #16213e;"
                + "-fx-border-color: #0f3460; -fx-border-width: 0 2 0 0;");

        VBox header = buildHeader();

        livesLabel = new Label("♥ ♥ ♥");
        livesLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        livesLabel.setTextFill(Color.web("#e94560"));

        scoreLabel = new Label("Path score: 0");
        scoreLabel.setFont(Font.font("Monospaced", 12));
        scoreLabel.setTextFill(Color.web("#f4d35e"));

        VBox statusBox = new VBox(2, livesLabel, scoreLabel);
        statusBox.setPadding(new Insets(6, 10, 6, 10));
        statusBox.setStyle("-fx-background-color: #0f1e3a;");

        mapCanvas = new Canvas(GameMap.CANVAS_W, GameMap.CANVAS_H);
        mapCanvas.setOnMouseClicked(e -> handleClick(e.getX(), e.getY()));

        ScrollPane scroll = new ScrollPane(mapCanvas);
        scroll.setFitToWidth(false);
        scroll.setPrefViewportWidth(286);
        scroll.setPrefViewportHeight(500);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: #0d0d1a; -fx-background-color: #0d0d1a;");

        getChildren().addAll(header, statusBox, scroll, buildLegend());
    }

    public void setOnNodeSelected(NodeSelectCallback cb) { this.onNodeSelected = cb; }

    public void refresh() {
        updateStatusLabels();
        drawMap();
    }

    // ──── Drawing ────

    private void drawMap() {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        gc.setFill(COL_BG);
        gc.fillRect(0, 0, GameMap.CANVAS_W, GameMap.CANVAS_H);

        GameMap map = gameState.getCurrentMap();
        if (map == null) {
            gc.setFill(COL_TEXT);
            gc.setFont(Font.font("Monospaced", 13));
            gc.fillText("No map generated.\nPress  [ New Game ].", 160, 260);
            return;
        }

        Level       currentNode = gameState.getCurrentNode();
        List<Level> avail       = map.getChildren(currentNode);
        List<Level> allNodes    = map.getAllNodes();

        // Draw edges first
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

        // Draw nodes on top
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
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
            gc.fillText("✓", cx - 5, cy + 5);
            return;
        }

        if (node.isStart()) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
            gc.fillText("START", cx - 13, cy + 4);
        } else {
            gc.setFill(COL_TEXT_DARK);
            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 10));
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

    // ──── Click handling ────

    private void handleClick(double mx, double my) {
        GameMap map = gameState.getCurrentMap();
        if (map == null || onNodeSelected == null) return;

        Level current = gameState.getCurrentNode();
        List<Level> available = map.getChildren(current);

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

    // ──── Status labels ────

    private void updateStatusLabels() {
        int lives = gameState.getLives();
        StringBuilder hearts = new StringBuilder();
        for (int i = 0; i < GameStateController.INITIAL_LIVES; i++)
            hearts.append(i < lives ? "♥ " : "♡ ");
        livesLabel.setText(hearts.toString().trim());
        scoreLabel.setText("Path score: " + gameState.getAccumulatedScore());
    }

    // ──── UI builders ────

    private VBox buildHeader() {
        VBox header = new VBox(4);
        header.setPadding(new Insets(10, 10, 8, 10));
        header.setStyle("-fx-background-color: #0f3460;");

        Label icon = new Label("🗺  LEVEL MAP");
        icon.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        icon.setTextFill(Color.web("#e94560"));

        Label sub = new Label("Click a highlighted node to enter");
        sub.setFont(Font.font("Monospaced", 10));
        sub.setTextFill(Color.web("#555"));

        header.getChildren().addAll(icon, sub);
        return header;
    }

    private VBox buildLegend() {
        VBox legend = new VBox(3);
        legend.setPadding(new Insets(6, 10, 8, 10));
        legend.setStyle("-fx-background-color: #0f1e3a;");
        legend.getChildren().addAll(
                legendRow("● Easy   (+20)",  "#57cc99"),
                legendRow("● Medium (+30)",  "#f4d35e"),
                legendRow("● Hard   (+50)",  "#e94560"),
                legendRow("○ Highlighted = available", "#ffe066")
        );
        return legend;
    }

    private Label legendRow(String text, String colour) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Monospaced", 11));
        lbl.setTextFill(Color.web(colour));
        return lbl;
    }
}
