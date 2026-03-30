package com.maze.adt;

import java.util.List;

/**
 * Graph ADT - Abstract Data Type for representing the maze as a graph.
 * Each cell in the maze is a vertex; passages between cells are edges.
 *
 * @param <T> the type of vertex label
 */
public interface GraphADT<T> {

    /** Add a vertex to the graph. */
    void addVertex(T vertex);

    /** Add an undirected edge between two vertices with a given weight. */
    void addEdge(T from, T to, double weight);

    /** Get all neighbors of a vertex. */
    List<T> getNeighbors(T vertex);

    /** Check if an edge exists between two vertices. */
    boolean hasEdge(T from, T to);

    /** Get the weight of an edge. */
    double getEdgeWeight(T from, T to);

    /** Get the total number of vertices. */
    int getVertexCount();

    /** Check if the graph contains a vertex. */
    boolean containsVertex(T vertex);
}
