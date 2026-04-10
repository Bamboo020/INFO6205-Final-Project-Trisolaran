package Maze;

import Implementation.MinHeap;
import Interface.MazeGenerator;

import java.util.Random;

public class PrimMazeGenerator implements MazeGenerator {

    private final Random random = new Random();

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

        visited[0][0] = true;
        addEdges(maze, heap, 0, 0, visited);

        while (!heap.isEmpty()) {
            WeightedEdge e = heap.popMin();
            if (visited[e.lr2][e.lc2]) continue;

            maze.openCorridor(e.lr1, e.lc1, e.lr2, e.lc2);
            visited[e.lr2][e.lc2] = true;
            addEdges(maze, heap, e.lr2, e.lc2, visited);
        }
    }

    private void addEdges(MazeGrid maze, MinHeap<WeightedEdge> heap,
                          int lr, int lc, boolean[][] visited) {
        for (int[] nb : maze.logicalNeighbors(lr, lc))
            if (!visited[nb[0]][nb[1]])
                heap.insert(new WeightedEdge(lr, lc, nb[0], nb[1], random.nextDouble()));
    }
}
