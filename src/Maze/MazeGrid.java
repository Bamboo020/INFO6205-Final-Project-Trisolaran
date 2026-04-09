package Maze;

import Implementation.ArrayList;
import Implementation.Graph;
import Implementation.HashMap;
import Interface.ListInterface;
import Interface.MazeModel;

/**
 * MazeGrid —— 3格宽走廊迷宫模型
 *
 * ═══════════════════════════════════════════════════
 * 物理网格布局（每个逻辑格展开为 3×3 路径格 + 1格隔墙）：
 *
 *   物理尺寸 = 4*L - 1（L = 逻辑格数）
 *
 *   逻辑格 (lr,lc) 对应物理左上角 (lr*4, lc*4)，
 *   占据 3×3 的路径格区域。
 *
 *   两逻辑格之间有 1 行/列隔墙带，打通时清空该带的 3 格，
 *   形成 3 格宽的可走通道。
 *
 * 格子类型（isWall 判断）：
 *   false = 可走（路径格）
 *   true  = 墙（不可走）
 * ═══════════════════════════════════════════════════
 *
 * 数据结构：
 *   - boolean[][] isWall      ：物理格是否为墙
 *   - Graph<String>           ：打通的相邻格子形成边（键格式 "r,c"）
 *   - HashMap<CellState>      ：每个格子的状态（键格式 "r,c"）
 *
 * 已修改：使用自定义 Graph / HashMap / ArrayList 替代 java.util 对应类
 */
public class MazeGrid implements MazeModel {

    private final int rows;     // 物理行数 = 4*logRows - 1
    private final int cols;     // 物理列数 = 4*logCols - 1
    private final int logRows;
    private final int logCols;

    private final boolean[][] isWall;

    // 邻接图：顶点为 "r,c"，边表示两物理格之间已打通
    private final Graph<String> graph;

    // 格子状态：key = "r,c", value = CellState
    private final HashMap<CellState> cellStates;

    // 玩家物理坐标（独立追踪，方便移动逻辑）
    private int playerRow = 0;
    private int playerCol = 0;

    /**
     * @param logRows 逻辑行数，物理行 = 4*logRows - 1
     * @param logCols 逻辑列数，物理列 = 4*logCols - 1
     */
    public MazeGrid(int logRows, int logCols) {
        this.logRows = logRows;
        this.logCols = logCols;
        this.rows    = 4 * logRows - 1;
        this.cols    = 4 * logCols - 1;

        isWall     = new boolean[rows][cols];
        graph      = new Graph<>();
        cellStates = new HashMap<>();

        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                graph.addVertex(key(r, c));
                cellStates.put(key(r, c), CellState.EMPTY);
            }

        // 初始：所有格子都是墙，只打开各逻辑格对应的 3×3 房间
        initAllWalls();
    }

    // ──────────────────────────────────────────────
    //  MazeModel 接口
    // ──────────────────────────────────────────────

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
    public ListInterface<int[]> getNeighbors(int row, int col) {
        ArrayList<int[]> result = new ArrayList<>();
        for (String nKey : graph.adj(key(row, col))) {
            String[] parts = nKey.split(",");
            result.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
        }
        return result;
    }

    @Override public int getWidth()  { return cols; }
    @Override public int getHeight() { return rows; }

    /** 渲染层：判断某物理格是否为墙 */
    public boolean isWall(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return true;
        return isWall[row][col];
    }

    /**
     * 尝试将玩家向 (dr,dc) 方向移动1个物理格。
     * 目标格必须是非墙格才能移动。
     * @return true 表示移动成功
     */
    public boolean movePlayer(int dr, int dc) {
        int nr = playerRow + dr;
        int nc = playerCol + dc;
        if (isWall(nr, nc)) return false;
        if (getCell(playerRow, playerCol) == CellState.PLAYER)
            setCell(playerRow, playerCol, CellState.EMPTY);
        playerRow = nr;
        playerCol = nc;
        setCell(playerRow, playerCol, CellState.PLAYER);
        return true;
    }

    public int getPlayerRow() { return playerRow; }
    public int getPlayerCol() { return playerCol; }

    /** 判断玩家是否到达出口 */
    public boolean isPlayerAtExit() {
        return getCell(playerRow, playerCol) == CellState.EXIT
                || (playerRow == (logRows - 1) * 4 + 1 && playerCol == (logCols - 1) * 4 + 1);
    }

    @Override
    public void generateMaze(Difficulty d) {
        resetWalls();
        switch (d) {
            case EASY:   new DFSMazeGenerator().generate(this);     break;
            case MEDIUM: new PrimMazeGenerator().generate(this);    break;
            case HARD:   new KruskalMazeGenerator().generate(this); break;
        }
        // 玩家出生在左上角房间中心
        playerRow = 1;
        playerCol = 1;
        setCell(playerRow, playerCol, CellState.PLAYER);
        // 出口在右下角房间中心
        setCell((logRows - 1) * 4 + 1, (logCols - 1) * 4 + 1, CellState.EXIT);
    }

    // ──────────────────────────────────────────────
    //  包级别辅助（供生成器使用）
    // ──────────────────────────────────────────────

    public int getLogRows() { return logRows; }
    public int getLogCols() { return logCols; }
    public int getRows()    { return rows;    }
    public int getCols()    { return cols;    }

    /**
     * 公开接口：凿开单个物理墙格（供破墙技能调用）
     * 将指定格设为可走，并与相邻非墙格建立图边
     */
    public void carveCell(int r, int c) {
        if (r >= 0 && r < rows && c >= 0 && c < cols && isWall[r][c]) {
            carve(r, c);
        }
    }

    /**
     * 打通两相邻逻辑格之间的 3 格宽走廊（逻辑坐标）
     *
     *   水平打通 (lr,lc)→(lr,lc+1)：清空列 lc*4+3，行 [lr*4, lr*4+2]
     *   垂直打通 (lr,lc)→(lr+1,lc)：清空行 lr*4+3，列 [lc*4, lc*4+2]
     */
    public void openCorridor(int lr, int lc, int nr, int nc) {
        int dr = nr - lr, dc = nc - lc;
        if (dr == 0 && dc == -1) { openCorridor(nr, nc, lr, lc); return; }
        if (dr == -1 && dc == 0) { openCorridor(nr, nc, lr, lc); return; }

        if (dr == 0 && dc == 1) {
            // 水平：打通右侧隔墙列
            int wallC = lc * 4 + 3;
            int baseR = lr * 4;
            for (int i = 0; i < 3; i++) carve(baseR + i, wallC);
        } else if (dr == 1 && dc == 0) {
            // 垂直：打通下方隔墙行
            int wallR = lr * 4 + 3;
            int baseC = lc * 4;
            for (int i = 0; i < 3; i++) carve(wallR, baseC + i);
        }
    }

    /** 返回逻辑格的四个逻辑邻居（供生成器遍历） */
    ArrayList<int[]> logicalNeighbors(int lr, int lc) {
        ArrayList<int[]> list = new ArrayList<>();
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : dirs) {
            int nr = lr + d[0], nc = lc + d[1];
            if (nr >= 0 && nr < logRows && nc >= 0 && nc < logCols)
                list.add(new int[]{nr, nc});
        }
        return list;
    }

    // ──────────────────────────────────────────────
    //  私有辅助
    // ──────────────────────────────────────────────

    /** 将某个物理格标记为可走，并与四周非墙格建图边 */
    private void carve(int r, int c) {
        isWall[r][c] = false;
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && !isWall[nr][nc]) {
                graph.addEdge(key(r, c), key(nr, nc));
            }
        }
    }

    private String key(int r, int c) { return r + "," + c; }

    /** 初始化：全部设为墙，再打开各逻辑格的 3×3 房间 */
    private void initAllWalls() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                isWall[r][c] = true;
        for (int lr = 0; lr < logRows; lr++)
            for (int lc = 0; lc < logCols; lc++)
                openRoom(lr, lc);
    }

    /** 打开逻辑格 (lr,lc) 对应的 3×3 房间 */
    private void openRoom(int lr, int lc) {
        int pr = lr * 4, pc = lc * 4;
        for (int dr = 0; dr < 3; dr++)
            for (int dc = 0; dc < 3; dc++)
                carve(pr + dr, pc + dc);
    }

    /** 重置所有墙与图边，准备重新生成 */
    private void resetWalls() {
        // 清空图边与格子状态
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                for (int[] d : dirs) {
                    int nr = r + d[0], nc = c + d[1];
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols)
                        graph.removeEdge(key(r, c), key(nr, nc));
                }
                cellStates.put(key(r, c), CellState.EMPTY);
            }
        // 重新初始化墙和房间
        initAllWalls();
    }
}
