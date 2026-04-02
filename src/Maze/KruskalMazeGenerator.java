package Maze;


import Implementation.MergeSort;
import Implementation.UnionFind;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Kruskal 迷宫生成器（困难难度）
 * 算法：随机化 Kruskal —— 对所有内部边随机赋权，用手写 MergeSort 排序，
 *       再用 UnionFind 选取不成环的边加入生成树（即打通墙壁）
 */
public class KruskalMazeGenerator {

    private final Random random = new Random();

    /** 迷宫内部一条可拆的墙（连接两相邻格） */
    private static class Edge {
        final int r1, c1, r2, c2;
        final double weight;
        Edge(int r1, int c1, int r2, int c2, double weight) {
            this.r1 = r1; this.c1 = c1;
            this.r2 = r2; this.c2 = c2;
            this.weight = weight;
        }
    }

    public void generate(MazeGrid maze) {
        int rows = maze.getRows();
        int cols = maze.getCols();

        // 1. 枚举所有内部边，赋随机权重
        List<Edge> edges = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (c + 1 < cols) edges.add(new Edge(r, c, r, c + 1, random.nextDouble()));
                if (r + 1 < rows) edges.add(new Edge(r, c, r + 1, c, random.nextDouble()));
            }
        }

        // 2. 用手写 MergeSort 按权重升序排序
        MergeSort<Edge> sorter = new MergeSort<>();
        List<Edge> sorted = sorter.sortList(edges, Comparator.comparingDouble(e -> e.weight));

        // 3. 用 UnionFind 按 Kruskal 选边
        UnionFind uf = new UnionFind(rows * cols);
        for (Edge e : sorted) {
            int id1 = e.r1 * cols + e.c1;
            int id2 = e.r2 * cols + e.c2;
            if (uf.union(id1, id2)) {
                maze.removeWall(e.r1, e.c1, e.r2, e.c2);
            }
        }
    }
}