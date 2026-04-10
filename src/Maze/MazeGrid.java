package Maze;

import Implementation.ArrayList;
import Implementation.Graph;
import Implementation.HashMap;
import Interface.ListInterface;
import Interface.MazeModel;

public class MazeGrid implements MazeModel {

    private final int rows;
    private final int cols;
    private final int logRows;
    private final int logCols;

    private final boolean[][] isWall;

    private final Graph<String> graph;

    private final HashMap<CellState> cellStates;

    private int playerRow = 0;
    private int playerCol = 0;

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

        initAllWalls();
    }

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

    public boolean isWall(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return true;
        return isWall[row][col];
    }

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
        playerRow = 1;
        playerCol = 1;
        setCell(playerRow, playerCol, CellState.PLAYER);
        setCell((logRows - 1) * 4 + 1, (logCols - 1) * 4 + 1, CellState.EXIT);
    }

    public int getLogRows() { return logRows; }
    public int getLogCols() { return logCols; }
    public int getRows()    { return rows;    }
    public int getCols()    { return cols;    }

    public void carveCell(int r, int c) {
        if (r >= 0 && r < rows && c >= 0 && c < cols && isWall[r][c]) {
            carve(r, c);
        }
    }

    public void openCorridor(int lr, int lc, int nr, int nc) {
        int dr = nr - lr, dc = nc - lc;
        if (dr == 0 && dc == -1) { openCorridor(nr, nc, lr, lc); return; }
        if (dr == -1 && dc == 0) { openCorridor(nr, nc, lr, lc); return; }

        if (dr == 0 && dc == 1) {
            int wallC = lc * 4 + 3;
            int baseR = lr * 4;
            for (int i = 0; i < 3; i++) carve(baseR + i, wallC);
        } else if (dr == 1 && dc == 0) {
            int wallR = lr * 4 + 3;
            int baseC = lc * 4;
            for (int i = 0; i < 3; i++) carve(wallR, baseC + i);
        }
    }

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

    private void initAllWalls() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                isWall[r][c] = true;
        for (int lr = 0; lr < logRows; lr++)
            for (int lc = 0; lc < logCols; lc++)
                openRoom(lr, lc);
    }

    private void openRoom(int lr, int lc) {
        int pr = lr * 4, pc = lc * 4;
        for (int dr = 0; dr < 3; dr++)
            for (int dc = 0; dc < 3; dc++)
                carve(pr + dr, pc + dc);
    }

    private void resetWalls() {
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
        initAllWalls();
    }
}
