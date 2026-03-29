package ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import maze.Difficulty;
import maze.MazeGrid;

/**
 * MazeApp —— JavaFX 应用入口
 * 提供难度选择、迷宫生成按钮，并展示 MazeCanvas 渲染结果
 */
public class MazeApp extends Application {

    private static final int  MAZE_ROWS  = 20;
    private static final int  MAZE_COLS  = 20;
    private static final double CELL_SIZE = 24;

    private MazeGrid   mazeGrid;
    private MazeCanvas mazeCanvas;

    @Override
    public void start(Stage primaryStage) {
        // 初始化迷宫模型与画布
        mazeGrid   = new MazeGrid(MAZE_ROWS, MAZE_COLS);
        mazeCanvas = new MazeCanvas(mazeGrid, CELL_SIZE);

        // 顶部工具栏
        Label diffLabel = new Label("难度：");
        ComboBox<String> diffBox = new ComboBox<>();
        diffBox.getItems().addAll("简单（DFS）", "中等（Prim）", "困难（Kruskal）");
        diffBox.setValue("简单（DFS）");

        Button genBtn = new Button("生成迷宫");
        genBtn.setOnAction(e -> {
            Difficulty d = switch (diffBox.getValue()) {
                case "中等（Prim）"     -> Difficulty.MEDIUM;
                case "困难（Kruskal）" -> Difficulty.HARD;
                default                -> Difficulty.EASY;
            };
            mazeGrid.generateMaze(d);
            mazeCanvas.render();
        });

        HBox toolbar = new HBox(10, diffLabel, diffBox, genBtn);
        toolbar.setPadding(new Insets(8));

        // 画布放入可滚动面板（支持大迷宫）
        ScrollPane scroll = new ScrollPane(mazeCanvas);
        scroll.setPadding(new Insets(8));

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(scroll);

        // 初始生成一个简单迷宫
        mazeGrid.generateMaze(Difficulty.EASY);
        mazeCanvas.render();

        Scene scene = new Scene(root, MAZE_COLS * CELL_SIZE + 40, MAZE_ROWS * CELL_SIZE + 80);
        primaryStage.setTitle("Maze Explorer RPG — 迷宫预览");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
