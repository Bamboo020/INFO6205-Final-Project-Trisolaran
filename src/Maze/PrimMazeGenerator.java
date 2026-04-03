package Maze;

import Implementation.MinHeap;
import Interface.MazeGenerator;

import java.util.Random;

/**
 * Prim 迷宫生成器（中等难度）
 * 随机化 Prim 算法：每次从候选边集合中随机取出一条连接已访问格与未访问格的边
 * 底层使用手写 MinHeap<WeightedEdge>（边权随机赋值实现随机选取）
 */
public class PrimMazeGenerator implements MazeGenerator {

    private final Random random = new Random();

    /** 带随机权重的逻辑边，用于 MinHeap 排序 */
    private static class WeightedEdge implements Comparable<WeightedEdge> {
        final int lr1, lc1, lr2, lc2;
        final double weight;

        WeightedEdge(int lr1, int lc1, int lr2, int lc2, double weight) {
            this.lr1 = lr1; this.lc1 = lc1;
            this.lr2 = lr2; this.lc2 = lc2;
            this.weight = weight;
        }

        @Override
        public int compareTo(WeightedEdge other) {
            return Double.compare(this.weight, other.weight);
        }
    }

    @Override
    public void generate(MazeGrid maze) {
        int logRows = maze.getLogRows();
        int logCols = maze.getLogCols();
        boolean[][] visited = new boolean[logRows][logCols];

        MinHeap<WeightedEdge> heap = new MinHeap<>();

        // 从逻辑格 (0,0) 开始
        visited[0][0] = true;
        addEdges(maze, heap, 0, 0, visited);

        while (!heap.isEmpty()) {
            WeightedEdge e = heap.popMin();
            // 若目标逻辑格已访问则跳过
            if (visited[e.lr2][e.lc2]) continue;

            // 打通 3 格宽走廊，标记访问
            maze.openCorridor(e.lr1, e.lc1, e.lr2, e.lc2);
            visited[e.lr2][e.lc2] = true;
            addEdges(maze, heap, e.lr2, e.lc2, visited);
        }
    }

    /** 将逻辑格 (lr,lc) 所有通向未访问逻辑格的边加入堆 */
    private void addEdges(MazeGrid maze, MinHeap<WeightedEdge> heap,
                          int lr, int lc, boolean[][] visited) {
        for (int[] nb : maze.logicalNeighbors(lr, lc))
            if (!visited[nb[0]][nb[1]])
                heap.insert(new WeightedEdge(lr, lc, nb[0], nb[1], random.nextDouble()));
    }
}
