package Maze;

import Implementation.ArrayList;
import Implementation.MergeSort;
import Implementation.UnionFind;
import Interface.MazeGenerator;

import java.util.Comparator;
import java.util.Random;

public class KruskalMazeGenerator implements MazeGenerator {

    private final Random random = new Random();

    private static class Edge {
        final int lr1, lc1, lr2, lc2;
        final double weight;

        Edge(int lr1, int lc1, int lr2, int lc2, double weight) {
            this.lr1 = lr1; this.lc1 = lc1;
            this.lr2 = lr2; this.lc2 = lc2;
            this.weight = weight;
        }
    }

    @Override
    public void generate(MazeGrid maze) {
        int logRows = maze.getLogRows();
        int logCols = maze.getLogCols();

        ArrayList<Edge> edges = new ArrayList<>();
        for (int lr = 0; lr < logRows; lr++)
            for (int lc = 0; lc < logCols; lc++) {
                if (lc + 1 < logCols) edges.add(new Edge(lr, lc, lr, lc + 1, random.nextDouble()));
                if (lr + 1 < logRows) edges.add(new Edge(lr, lc, lr + 1, lc, random.nextDouble()));
            }

        ArrayList<Edge> sorted = new MergeSort<Edge>().sortList(edges, Comparator.comparingDouble(e -> e.weight));

        UnionFind uf = new UnionFind(logRows * logCols);
        for (Edge e : sorted) {
            int id1 = e.lr1 * logCols + e.lc1;
            int id2 = e.lr2 * logCols + e.lc2;
            if (uf.union(id1, id2))
                maze.openCorridor(e.lr1, e.lc1, e.lr2, e.lc2);
        }
    }
}
