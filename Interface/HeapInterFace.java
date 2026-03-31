package Interface;

public interface HeapInterFace<T>{
    public void insert(T item);
    public T peekMin();
    public T popMin();
    public void decreaseKey(T item, T newKey);
    public boolean isEmpty();
    public int size();
}
