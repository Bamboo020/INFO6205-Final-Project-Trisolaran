package com.maze.util;

import com.maze.model.LinkedStack;
import com.maze.model.MazeGraph;

import java.util.*;

/**
 * MazeGenerator - Uses randomized DFS (with our LinkedStack ADT) to generate
 * a perfect maze (exactly one path between any two cells).
 *
 * The generated maze is stored as:
 *   1. A boolean[][] grid (true = wall, false = passage)
 *   2. A MazeGraph (Graph ADT) for pathfinding
 */
public class MazeGenerator {

    private final int rows;
    private final int cols;
    private final boolean[][] walls;      // true = wall cell
    private final MazeGraph graph;
    private final Random random;

    // Directions: up, right, down, left
    private static final int[][] DIRS = {{-1, 0}, {0, 1}, {1, 0}, {0, -1}};

    public MazeGenerator(int rows, int cols) {
        this(rows, cols, new Random());
    }

    public MazeGenerator(int rows, int cols, Random random) {
        // Ensure odd dimensions for proper maze grid
        this.rows = (rows % 2 == 0) ? rows + 1 : rows;
        this.cols = (cols % 2 == 0) ? cols + 1 : cols;
        this.walls = new boolean[this.rows][this.cols];
        this.graph = new MazeGraph();
        this.random = random;
    }

    /**
     * Generate the maze using randomized DFS (recursive backtracker).
     * Uses our custom LinkedStack to avoid Java stack overflow on large mazes.
     */
    public void generate() {
        // Initialize: all cells are walls
        for (boolean[] row : walls) Arrays.fill(row, true);

        // Maze cells are at odd coordinates (1,1), (1,3), (3,1), ...
        int startR = 1, startC = 1;
        walls[startR][startC] = false;

        // Use our LinkedStack ADT for the DFS
        LinkedStack<int[]> stack = new LinkedStack<>();
        stack.push(new int[]{startR, startC});

        boolean[][] visited = new boolean[rows][cols];
        visited[startR][startC] = true;

        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            int r = current[0], c = current[1];

            // Find unvisited neighbors (2 cells away in each direction)
            List<int[]> unvisited = new ArrayList<>();
            for (int[] d : DIRS) {
                int nr = r + d[0] * 2;
                int nc = c + d[1] * 2;
                if (nr > 0 && nr < rows - 1 && nc > 0 && nc < cols - 1 && !visited[nr][nc]) {
                    unvisited.add(new int[]{nr, nc, d[0], d[1]});
                }
            }

            if (unvisited.isEmpty()) {
                stack.pop(); // Backtrack
            } else {
                // Pick a random unvisited neighbor
                int[] next = unvisited.get(random.nextInt(unvisited.size()));
                int nr = next[0], nc = next[1];
                int dr = next[2], dc = next[3];

                // Carve passage: remove the wall between current and next
                walls[r + dr][c + dc] = false;
                walls[nr][nc] = false;

                visited[nr][nc] = true;
                stack.push(new int[]{nr, nc});
            }
        }

        // Build the Graph ADT from the carved maze
        buildGraph();

        // Place coins on some dead-end cells
    }

    /** Build the MazeGraph from the wall grid. */
    private void buildGraph() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (!walls[r][c]) {
                    String key = MazeGraph.cellKey(r, c);
                    graph.addVertex(key);

                    // Check right and down neighbors to add edges
                    if (c + 1 < cols && !walls[r][c + 1]) {
                        graph.addEdge(key, MazeGraph.cellKey(r, c + 1), 1.0);
                    }
                    if (r + 1 < rows && !walls[r + 1][c]) {
                        graph.addEdge(key, MazeGraph.cellKey(r + 1, c), 1.0);
                    }
                }
            }
        }
    }

    /**
     * Find dead-end cells (cells with exactly 1 neighbor in the graph).
     * Good positions for placing coins and items.
     */
    public List<String> findDeadEnds() {
        List<String> deadEnds = new ArrayList<>();
        for (String v : graph.getAllVertices()) {
            if (graph.getNeighbors(v).size() == 1) {
                deadEnds.add(v);
            }
        }
        return deadEnds;
    }

    // ==================== Getters ====================

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public boolean[][] getWalls() { return walls; }
    public MazeGraph getGraph() { return graph; }

    public boolean isWall(int r, int c) {
        return r < 0 || r >= rows || c < 0 || c >= cols || walls[r][c];
    }
}
