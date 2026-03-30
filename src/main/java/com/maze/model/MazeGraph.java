package com.maze.model;

import com.maze.adt.GraphADT;
import java.util.*;

/**
 * MazeGraph - Adjacency-list implementation of GraphADT.
 * Represents the maze as a weighted undirected graph where:
 *   - Each cell (row, col) is a vertex
 *   - Passages between cells are edges
 *   - Edge weight represents terrain cost (1=normal, 2=slow, etc.)
 */
public class MazeGraph implements GraphADT<String> {

    // Adjacency list: vertex -> list of (neighbor, weight)
    private final Map<String, List<Edge>> adjacencyList;
    private int vertexCount;

    /** Internal edge representation. */
    private static class Edge {
        final String target;
        final double weight;

        Edge(String target, double weight) {
            this.target = target;
            this.weight = weight;
        }
    }

    public MazeGraph() {
        this.adjacencyList = new LinkedHashMap<>();
        this.vertexCount = 0;
    }

    @Override
    public void addVertex(String vertex) {
        if (!adjacencyList.containsKey(vertex)) {
            adjacencyList.put(vertex, new ArrayList<>());
            vertexCount++;
        }
    }

    @Override
    public void addEdge(String from, String to, double weight) {
        addVertex(from);
        addVertex(to);
        // Undirected: add both directions
        if (!hasEdge(from, to)) {
            adjacencyList.get(from).add(new Edge(to, weight));
            adjacencyList.get(to).add(new Edge(from, weight));
        }
    }

    @Override
    public List<String> getNeighbors(String vertex) {
        List<String> neighbors = new ArrayList<>();
        List<Edge> edges = adjacencyList.get(vertex);
        if (edges != null) {
            for (Edge e : edges) {
                neighbors.add(e.target);
            }
        }
        return neighbors;
    }

    @Override
    public boolean hasEdge(String from, String to) {
        List<Edge> edges = adjacencyList.get(from);
        if (edges == null) return false;
        for (Edge e : edges) {
            if (e.target.equals(to)) return true;
        }
        return false;
    }

    @Override
    public double getEdgeWeight(String from, String to) {
        List<Edge> edges = adjacencyList.get(from);
        if (edges != null) {
            for (Edge e : edges) {
                if (e.target.equals(to)) return e.weight;
            }
        }
        return Double.MAX_VALUE;
    }

    @Override
    public int getVertexCount() {
        return vertexCount;
    }

    @Override
    public boolean containsVertex(String vertex) {
        return adjacencyList.containsKey(vertex);
    }

    /** Get all vertex keys (for iteration). */
    public Set<String> getAllVertices() {
        return adjacencyList.keySet();
    }

    /** Utility: create a vertex key from row and column. */
    public static String cellKey(int row, int col) {
        return row + "," + col;
    }

    /** Utility: parse row from a cell key. */
    public static int getRow(String key) {
        return Integer.parseInt(key.split(",")[0]);
    }

    /** Utility: parse col from a cell key. */
    public static int getCol(String key) {
        return Integer.parseInt(key.split(",")[1]);
    }
}
