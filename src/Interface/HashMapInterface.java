package Interface;

public interface HashMapInterface<T> {
    public void put(Object key, Object value);
    public  T remove(Object key);
    public T get(Object key);
    public Boolean containsKey(T key);
    public int size();
    public void resize();
}