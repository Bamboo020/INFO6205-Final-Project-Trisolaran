package Interface;

import java.util.Comparator;
import java.util.List;

public interface SortInterface<T> {
    public void sort(T[] arr, Comparator<T> cmp);
    public List<T> sortList(List<T> list, Comparator<T> cmp);
}