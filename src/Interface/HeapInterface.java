package Interface;

public interface HeapInterface<T>{
    public void insert(T item);
    public T popMin();
    public boolean isEmpty();
    public int size();
}