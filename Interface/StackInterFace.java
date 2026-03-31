package Interface;

public interface StackInterFace<T> {
    public boolean isEmpty();
    public boolean isFull();
    public void push(T o);
    public T pop();
    public T peek();
    public int size();
    public void clear();
}
