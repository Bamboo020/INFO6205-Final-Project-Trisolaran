package Interface;

import java.util.Iterator;

public interface SetInterface<T> extends Iterable<T> {

    boolean add(T element);

    boolean remove(Object element);

    boolean contains(Object element);

    int size();

    boolean isEmpty();

    void clear();

    Iterator<T> iterator();
}