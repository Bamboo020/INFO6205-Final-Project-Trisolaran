package Interface;

public interface HeapInterface<T>{
    public void insert(T item);
    public T peekMin();
    public T popMin();
    public void decreaseKey(T item, T newKey);
    public boolean isEmpty();
    public int size();
}