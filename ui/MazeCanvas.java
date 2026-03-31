package ui;

import Interface.MazeModel;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import maze.CellState;
import maze.MazeGrid;

/**
 * MazeCanvas —— JavaFX 迷宫渲染画布（任务 A8）
 *
 * 使用方式：
 *   MazeCanvas canvas = new MazeCanvas(maze, 20);
 *   pane.getChildren().add(canvas);
 *   canvas.render();           // 首次渲染
 *   canvas.render();           // 每帧/每次状态变化后重新调用
 *
 * 渲染规则：
 *   - 每个格子为 cellSize × cellSize 像素的正方形
 *   - 墙壁用深色线段绘制在格子边缘
 *   - 不同 CellState 对应不同背景色
 *   - 外围边框单独绘制
 */
public class MazeCanvas extends Canvas {

    // -------- 颜色常量 --------
    private static final Color COLOR_FLOOR   = Color.web("#F5F5F5");  // 空地：浅灰
    private static final Color COLOR_WALL    = Color.web("#2C2C2C");  // 墙壁：深灰
    private static final Color COLOR_PLAYER  = Color.web("#3A86FF");  // 玩家：蓝色
    private static final Color COLOR_EXIT    = Color.web("#06D6A0");  // 出口：青绿
    private static final Color COLOR_ITEM    = Color.web("#FFD166");  // 道具：金黄
    private static final Color COLOR_ENEMY   = Color.web("#EF476F");  // 敌人：红色
    private static final Color COLOR_BORDER  = Color.web("#1A1A1A");  // 外边框

    private static final double WALL_WIDTH   = 2.0;  // 墙线宽
    private static final double ICON_PADDING = 0.2;  // 图标内边距（占格子比例）

    private final MazeModel maze;
    private final double cellSize;

    /**
     * @param maze     迷宫模型
     * @param cellSize 每个格子的像素大小（建议 16–30）
     */
    public MazeCanvas(MazeGrid maze, double cellSize) {
        super(maze.getWidth() * cellSize, maze.getHeight() * cellSize);
        this.maze     = maze;
        this.cellSize = cellSize;
    }

    /** 完整重绘迷宫，应在每次状态变化后调用 */
    public void render() {
        GraphicsContext gc = getGraphicsContext2D();
        int rows = maze.getHeight();
        int cols = maze.getWidth();

        // 1. 背景清空
        gc.setFill(COLOR_FLOOR);
        gc.fillRect(0, 0, getWidth(), getHeight());

        // 2. 逐格绘制单元格背景与图标
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                drawCell(gc, r, c);
            }
        }

        // 3. 绘制内部墙壁
        gc.setStroke(COLOR_WALL);
        gc.setLineWidth(WALL_WIDTH);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                drawWalls(gc, r, c);
            }
        }

        // 4. 绘制外边框
        gc.setStroke(COLOR_BORDER);
        gc.setLineWidth(WALL_WIDTH + 1);
        gc.strokeRect(0, 0, getWidth(), getHeight());
    }

    // -------- 私有绘制辅助 --------

    /** 根据格子状态绘制背景色及图标 */
    private void drawCell(GraphicsContext gc, int r, int c) {
        double x = c * cellSize;
        double y = r * cellSize;
        CellState state = maze.getCell(r, c);

        // 背景色
        Color bg = switch (state) {
            case PLAYER -> COLOR_PLAYER;
            case EXIT   -> COLOR_EXIT;
            case ITEM   -> COLOR_ITEM;
            case ENEMY  -> COLOR_ENEMY;
            default     -> COLOR_FLOOR;
        };
        gc.setFill(bg);
        gc.fillRect(x, y, cellSize, cellSize);

        // 图标（圆形符号）
        double pad  = cellSize * ICON_PADDING;
        double size = cellSize - 2 * pad;
        switch (state) {
            case PLAYER -> {
                gc.setFill(Color.WHITE);
                gc.fillOval(x + pad, y + pad, size, size);
            }
            case EXIT -> {
                // 绘制出口星形简化为一个小白圆
                gc.setFill(Color.WHITE);
                gc.fillOval(x + pad, y + pad, size, size);
                gc.setFill(COLOR_EXIT);
                gc.fillOval(x + cellSize / 2 - size * 0.15,
                            y + cellSize / 2 - size * 0.15,
                            size * 0.3, size * 0.3);
            }
            case ITEM -> {
                // 道具：白色菱形（用旋转矩形近似）
                gc.setFill(Color.WHITE);
                double cx = x + cellSize / 2;
                double cy = y + cellSize / 2;
                double half = size * 0.4;
                double[] xs = {cx, cx + half, cx, cx - half};
                double[] ys = {cy - half, cy, cy + half, cy};
                gc.fillPolygon(xs, ys, 4);
            }
            case ENEMY -> {
                // 敌人：白色叉号简化为两条线
                gc.setStroke(Color.WHITE);
                gc.setLineWidth(2.0);
                gc.strokeLine(x + pad, y + pad, x + cellSize - pad, y + cellSize - pad);
                gc.strokeLine(x + cellSize - pad, y + pad, x + pad, y + cellSize - pad);
            }
            default -> { /* 空地不额外绘制 */ }
        }
    }

    /**
     * 绘制 (r,c) 格子的右侧墙和下侧墙
     * 左侧/上侧墙由相邻格子负责绘制，避免重复；外边框单独处理
     */
    private void drawWalls(GraphicsContext gc, int r, int c) {
        double x = c * cellSize;
        double y = r * cellSize;
        int rows = maze.getHeight();
        int cols = maze.getWidth();

        // 右侧墙
        if (c + 1 < cols && maze.hasWall(r, c, r, c + 1)) {
            gc.strokeLine(x + cellSize, y, x + cellSize, y + cellSize);
        }
        // 下侧墙
        if (r + 1 < rows && maze.hasWall(r, c, r + 1, c)) {
            gc.strokeLine(x, y + cellSize, x + cellSize, y + cellSize);
        }
    }
}
