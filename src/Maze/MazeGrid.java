package Maze;

import Implementation.Graph;
import Implementation.HashMap;
import Interface.MazeModel;

import java.util.ArrayList;
import java.util.List;

/**
 * MazeGrid —— MazeModel 的具体实现
 *
 * 数据结构：
 *   - boolean[][] rightWall  ：(r,c) 与 (r,c+1) 之间是否有墙
 *   - boolean[][] bottomWall ：(r,c) 与 (r+1,c) 之间是否有墙
 *   - Graph<String>          ：打通的相邻格子形成边（键格式 "r,c"）
 *   - HashMap<CellState>     ：每个格子的状态（键格式 "r,c"）
 *
 * 初始状态：所有内部墙均存在，所有格子状态为 EMPTY。
 */
public class MazeGrid implements MazeModel {

    private final int rows;
    private final int cols;

    // 墙壁存储：true = 墙存在
    private final boolean[][] rightWall;   // [rows][cols-1]
    private final boolean[][] bottomWall;  // [rows-1][cols]

    // 邻接图：顶点为 "r,c"，边表示两格之间已打通
    private final Graph<String> graph;

    // 格子状态：key = "r,c", value = CellState
    private final HashMap<CellState> cellStates;

    public MazeGrid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        // 初始化所有墙为存在
        rightWall  = new boolean[rows][cols];
        bottomWall = new boolean[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                rightWall[r][c]  = (c < cols - 1); // 最右列无右墙
                bottomWall[r][c] = (r < rows - 1); // 最底行无下墙
            }

        graph      = new Graph<>();
        cellStates = new HashMap<>();

        // 向图中添加所有顶点，初始化格子状态
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                graph.addVertex(key(r, c));
                cellStates.put(key(r, c), CellState.EMPTY);
            }
    }

    // -------- MazeModel 接口 --------

    @Override
    public CellState getCell(int row, int col) {
        CellState s = cellStates.get(key(row, col));
        return s == null ? CellState.EMPTY : s;
    }

    @Override
    public void setCell(int row, int col, CellState state) {
        cellStates.put(key(row, col), state);
    }

    @Override
    public List<int[]> getNeighbors(int row, int col) {
        List<int[]> result = new ArrayList<>();
        for (String nKey : graph.adj(key(row, col))) {
            String[] parts = nKey.split(",");
            result.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
        }
        return result;
    }

    @Override
    public int getWidth()  { return cols; }

    @Override
    public int getHeight() { return rows; }

    @Override
    public void generateMaze(Difficulty d) {
        // 重置所有墙
        resetWalls();
        switch (d) {
            case EASY:   new DFSMazeGenerator().generate(this);   break;
            case MEDIUM: new PrimMazeGenerator().generate(this);  break;
            case HARD:   new KruskalMazeGenerator().generate(this); break;
        }
        // 设置出口在右下角
        setCell(rows - 1, cols - 1, CellState.EXIT);
        // 设置玩家在左上角
        setCell(0, 0, CellState.PLAYER);
    }

    @Override
    public boolean hasWall(int row, int col, int nRow, int nCol) {
        int dr = nRow - row;
        int dc = nCol - col;
        if (dr == 0 && dc == 1)  return rightWall[row][col];
        if (dr == 0 && dc == -1) return rightWall[row][nCol];
        if (dr == 1 && dc == 0)  return bottomWall[row][col];
        if (dr == -1 && dc == 0) return bottomWall[nRow][col];
        return true; // 非相邻格子视为有墙
    }

    // -------- 包级别辅助（供生成器使用）--------

    /** 拆除 (r,c) 与 (nr,nc) 之间的墙，并在图中添加边 */
    public void removeWall(int r, int c, int nr, int nc) {
        int dr = nr - r;
        int dc = nc - c;
        if (dr == 0 && dc == 1)  rightWall[r][c]    = false;
        if (dr == 0 && dc == -1) rightWall[r][nc]   = false;
        if (dr == 1 && dc == 0)  bottomWall[r][c]   = false;
        if (dr == -1 && dc == 0) bottomWall[nr][c]  = false;
        graph.addEdge(key(r, c), key(nr, nc));
    }

    /** 返回格子在图中的四个方向邻居（不论是否有墙），用于生成器遍历 */
    List<int[]> allNeighbors(int r, int c) {
        List<int[]> list = new ArrayList<>();
        int[][] dirs = {{-1,0},{1,0},{0,-1},{0,1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols)
                list.add(new int[]{nr, nc});
        }
        return list;
    }

    int getRows() { return rows; }
    int getCols() { return cols; }

    // -------- 私有辅助 --------

    private String key(int r, int c) { return r + "," + c; }

    /** 重置为所有墙存在、图中仅顶点无边 */
    private void resetWalls() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                rightWall[r][c]  = (c < cols - 1);
                bottomWall[r][c] = (r < rows - 1);
            }
        // 移除图中所有边（重建图）
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                for (int[] nb : allNeighbors(r, c))
                    graph.removeEdge(key(r, c), key(nb[0], nb[1]));
    }
}