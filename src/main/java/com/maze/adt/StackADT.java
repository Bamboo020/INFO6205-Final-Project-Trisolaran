package com.maze.adt;

/**
 * Stack ADT - Abstract Data Type for LIFO operations.
 * Used to track player movement history and enable undo/backtracking.
 *
 * @param <T> the type of elements in the stack
 */
public interface StackADT<T> {

    /** Push an element onto the top of the stack. */
    void push(T element);

    /** Remove and return the top element. */
    T pop();

    /** Return the top element without removing it. */
    T peek();

    /** Check if the stack is empty. */
    boolean isEmpty();

    /** Return the number of elements in the stack. */
    int size();

    /** Clear all elements from the stack. */
    void clear();
}
