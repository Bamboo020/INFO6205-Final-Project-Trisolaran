package Interface;

public interface BinarySearchTreeInterface<K extends Comparable<K>, V> {
    void insert(K key, V value);
    V search(K key);
    boolean delete(K key);
    ListInterface<V> inOrderTraversal();
    ListInterface<V> getUpTo(K key);
    boolean contains(K key);
    int size();
    K getMin();
    K getMax();
}
