package Interface;

import java.util.Comparator;
import Implementation.ArrayList;

public interface SortInterface<T> {
    public void sort(T[] arr, Comparator<T> cmp);
    public ArrayList<T> sortList(ListInterface<T> list, Comparator<T> cmp);
}