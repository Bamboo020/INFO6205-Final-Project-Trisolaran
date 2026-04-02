package Interface;

import java.util.List;

public interface BinarySearchTreeInterface<K extends Comparable<K>, V> {
    void insert(K key, V value);
    V search(K key);
    boolean delete(K key);
    List<V> inOrderTraversal();
    List<V> getUpTo(K key);
    boolean contains(K key);
    int size();
    K getMin();
    K getMax();
}
