package Model;

import Implementation.ArrayList;
import Implementation.HashMap;
import Implementation.HashSet;
import Implementation.HeapPriorityQueue;
import Implementation.Queue;
import Interface.ListInterface;
import Interface.MazeModel;
import Interface.SetInterface;

/**
 * PathFinder —— 路径搜索工具
 *
 * 已修改：
 *   - ArrayList   替代 java.util.ArrayList / java.util.List
 *   - HashSet     替代 java.util.HashSet / java.util.Set
 *   - Queue       替代 java.util.LinkedList (Queue 用法)
 *   - HashMap     替代 java.util.HashMap / java.util.Map
 *   - 手动填充数组替代 java.util.Arrays.fill
 */
public class PathFinder {

    private static class DijkstraEntry implements Comparable<DijkstraEntry> {
        final int row, col;
        final double distance;

        DijkstraEntry(int row, int col, double distance) {
            this.row = row;
            this.col = col;
            this.distance = distance;
        }

        @Override
        public int compareTo(DijkstraEntry other) {
            return Double.compare(this.distance, other.distance);
        }
    }

    public static ListInterface<int[]> findShortestPath(MazeModel maze, int srcRow, int srcCol,
                                                        int tgtRow, int tgtCol) {
        int rows = maze.getHeight();
        int cols = maze.getWidth();

        double[][] dist = new double[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                dist[r][c] = Double.MAX_VALUE;
        dist[srcRow][srcCol] = 0.0;

        int[][] prevRow = new int[rows][cols];
        int[][] prevCol = new int[rows][cols];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                prevRow[r][c] = -1;
                prevCol[r][c] = -1;
            }

        boolean[][] visited = new boolean[rows][cols];

        HeapPriorityQueue<DijkstraEntry> pq = new HeapPriorityQueue<>();
        pq.enqueue(new DijkstraEntry(srcRow, srcCol, 0.0));

        while (!pq.isEmpty()) {
            DijkstraEntry current = pq.dequeue();
            int r = current.row;
            int c = current.col;

            if (visited[r][c]) continue;
            visited[r][c] = true;

            // 到达终点，提前结束
            if (r == tgtRow && c == tgtCol) break;

            // 松弛相邻边（通过 MazeModel.getNeighbors 获取可通行邻居）
            for (int[] nb : maze.getNeighbors(r, c)) {
                int nr = nb[0], nc = nb[1];
                if (visited[nr][nc]) continue;

                double newDist = dist[r][c] + 1.0; // 无权图，边权为 1
                if (newDist < dist[nr][nc]) {
                    dist[nr][nc] = newDist;
                    prevRow[nr][nc] = r;
                    prevCol[nr][nc] = c;
                    pq.enqueue(new DijkstraEntry(nr, nc, newDist));
                }
            }
        }

        // 回溯路径
        return reconstructPath(prevRow, prevCol, srcRow, srcCol, tgtRow, tgtCol);
    }

    public static ListInterface<String> findShortestPath(MazeModel maze, String source, String target) {
        int[] src = parseKey(source);
        int[] tgt = parseKey(target);
        ListInterface<int[]> path = findShortestPath(maze, src[0], src[1], tgt[0], tgt[1]);
        ArrayList<String> keys = new ArrayList<>();
        for (int[] cell : path) {
            keys.add(cell[0] + "," + cell[1]);
        }
        return keys;
    }

    public static SetInterface<String> getReachableWithin(MazeModel maze, int srcRow, int srcCol, int maxSteps) {
        HashSet<String> reachable = new HashSet<>();
        Queue<int[]> queue = new Queue<>();
        HashMap<Integer> distMap = new HashMap<>();

        String srcKey = srcRow + "," + srcCol;
        queue.add(new int[]{srcRow, srcCol});
        distMap.put(srcKey, 0);
        reachable.add(srcKey);

        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int r = cell[0], c = cell[1];
            String key = r + "," + c;
            Integer d = distMap.get(key);
            if (d >= maxSteps) continue;

            for (int[] nb : maze.getNeighbors(r, c)) {
                String nbKey = nb[0] + "," + nb[1];
                if (!distMap.containsKeyObj(nbKey)) {
                    distMap.put(nbKey, d + 1);
                    reachable.add(nbKey);
                    queue.add(nb);
                }
            }
        }
        return reachable;
    }

    private static ListInterface<int[]> reconstructPath(int[][] prevRow, int[][] prevCol,
                                                        int srcRow, int srcCol,
                                                        int tgtRow, int tgtCol) {
        ArrayList<int[]> path = new ArrayList<>();

        if (prevRow[tgtRow][tgtCol] == -1 && !(srcRow == tgtRow && srcCol == tgtCol)) {
            return path;
        }

        int r = tgtRow, c = tgtCol;
        while (r != -1 && c != -1) {
            path.add(0, new int[]{r, c});
            if (r == srcRow && c == srcCol) break;
            int pr = prevRow[r][c];
            int pc = prevCol[r][c];
            r = pr;
            c = pc;
        }
        return path;
    }

    private static int[] parseKey(String key) {
        String[] parts = key.split(",");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }
}