package com.maze.adt;

import java.util.List;

/**
 * Binary Search Tree ADT - Abstract Data Type for ordered tree operations.
 * Used for the player's skill tree system (skills ordered by level requirement).
 *
 * @param <K> the key type (must be Comparable)
 * @param <V> the value type
 */
public interface BinarySearchTreeADT<K extends Comparable<K>, V> {

    /** Insert a key-value pair. */
    void insert(K key, V value);

    /** Search for a value by key. */
    V search(K key);

    /** Delete a key-value pair. */
    boolean delete(K key);

    /** Return all values via in-order traversal (sorted). */
    List<V> inOrderTraversal();

    /** Return all values where key <= given key (unlocked skills). */
    List<V> getUpTo(K key);

    /** Check if the tree contains a key. */
    boolean contains(K key);

    /** Return the number of entries. */
    int size();

    /** Return the minimum key. */
    K getMin();

    /** Return the maximum key. */
    K getMax();
}
