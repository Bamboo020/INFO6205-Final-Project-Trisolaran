package Interface;

import Maze.CellState;
import Maze.Difficulty;

/**
 * MazeModel 接口 —— 成员 A 提供，所有模块消费
 * 定义迷宫的只读查询与生成触发方法
 *
 * 已修改：使用自定义 ListInterface 替代 java.util.List
 */
public interface MazeModel {

    /** 返回指定格子的状态 */
    CellState getCell(int row, int col);

    /** 设置指定格子的状态 */
    void setCell(int row, int col, CellState state);

    /**
     * 返回与 (row, col) 相邻且可通行（无墙阻挡）的格子坐标
     * 每个 int[] 格式为 {row, col}
     */
    ListInterface<int[]> getNeighbors(int row, int col);

    /** 网格列数（宽） */
    int getWidth();

    /** 网格行数（高） */
    int getHeight();

    /** 按指定难度触发迷宫生成 */
    void generateMaze(Difficulty d);

    /** 判断 (row, col) 和 (nRow, nCol) 之间是否有墙 */
    boolean hasWall(int row, int col, int nRow, int nCol);
}