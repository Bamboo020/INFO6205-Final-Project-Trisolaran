package com.maze.adt;

/**
 * PriorityQueue ADT - Abstract Data Type for priority-based ordering.
 * Used for Dijkstra's pathfinding and game event scheduling.
 *
 * @param <T> the type of elements (must be Comparable)
 */
public interface PriorityQueueADT<T extends Comparable<T>> {

    /** Insert an element into the priority queue. */
    void enqueue(T element);

    /** Remove and return the element with highest priority (smallest value). */
    T dequeue();

    /** Return the element with highest priority without removing it. */
    T peek();

    /** Check if the priority queue is empty. */
    boolean isEmpty();

    /** Return the number of elements. */
    int size();

    /** Clear all elements. */
    void clear();
}
