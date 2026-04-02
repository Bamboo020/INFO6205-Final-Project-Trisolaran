package Maze;

import Implementation.MinHeap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Prim 迷宫生成器（中等难度）
 * 随机化 Prim 算法：每次从候选边集合中随机取出一条连接已访问格与未访问格的边
 * 底层使用手写 MinHeap<WeightedEdge>（边权随机赋值实现随机选取）
 */
public class PrimMazeGenerator {

    private final Random random = new Random();

    /** 带随机权重的边，用于 MinHeap 排序 */
    private static class WeightedEdge implements Comparable<WeightedEdge> {
        final int r1, c1, r2, c2;
        final double weight;

        WeightedEdge(int r1, int c1, int r2, int c2, double weight) {
            this.r1 = r1; this.c1 = c1;
            this.r2 = r2; this.c2 = c2;
            this.weight = weight;
        }

        @Override
        public int compareTo(WeightedEdge other) {
            return Double.compare(this.weight, other.weight);
        }
    }

    public void generate(MazeGrid maze) {
        int rows = maze.getRows();
        int cols = maze.getCols();
        boolean[][] visited = new boolean[rows][cols];

        MinHeap<WeightedEdge> heap = new MinHeap<>();

        // 从 (0,0) 开始
        visited[0][0] = true;
        addEdges(maze, heap, 0, 0, visited);

        while (!heap.isEmpty()) {
            WeightedEdge edge = heap.popMin();
            // 若目标格已访问则跳过
            if (visited[edge.r2][edge.c2]) continue;

            // 打通墙，标记访问
            maze.removeWall(edge.r1, edge.c1, edge.r2, edge.c2);
            visited[edge.r2][edge.c2] = true;
            addEdges(maze, heap, edge.r2, edge.c2, visited);
        }
    }

    /** 将 (r,c) 所有通向未访问格的边加入堆 */
    private void addEdges(MazeGrid maze, MinHeap<WeightedEdge> heap,
                          int r, int c, boolean[][] visited) {
        for (int[] nb : maze.allNeighbors(r, c)) {
            if (!visited[nb[0]][nb[1]]) {
                heap.insert(new WeightedEdge(r, c, nb[0], nb[1], random.nextDouble()));
            }
        }
    }
}