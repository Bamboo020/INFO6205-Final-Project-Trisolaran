package Implementation;

import Interface.ListInterface;
import Interface.SortInterface;

import java.util.Comparator;

public class MergeSort<T> implements SortInterface<T> {

    @Override
    public void sort(T[] arr, Comparator<T> cmp) {
        if (arr == null || arr.length <= 1) return;
        Object[] aux = new Object[arr.length];
        mergeSort(arr, aux, 0, arr.length - 1, cmp);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<T> sortList(ListInterface<T> list, Comparator<T> cmp) {
        if (list == null || list.size() <= 1) {
            ArrayList<T> result = new ArrayList<>();
            if (list != null) for (T item : list) result.add(item);
            return result;
        }
        T[] arr = (T[]) new Object[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i);
        sort(arr, cmp);
        ArrayList<T> result = new ArrayList<>(arr.length);
        for (T item : arr) result.add(item);
        return result;
    }

    @SuppressWarnings("unchecked")
    private void mergeSort(T[] arr, Object[] aux, int lo, int hi, Comparator<T> cmp) {
        if (lo >= hi) return;
        int mid = lo + (hi - lo) / 2;
        mergeSort(arr, aux, lo, mid, cmp);
        mergeSort(arr, aux, mid + 1, hi, cmp);
        merge(arr, aux, lo, mid, hi, cmp);
    }

    @SuppressWarnings("unchecked")
    private void merge(T[] arr, Object[] aux, int lo, int mid, int hi, Comparator<T> cmp) {
        System.arraycopy(arr, lo, aux, lo, hi - lo + 1);

        int i = lo;
        int j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if (i > mid) {
                arr[k] = (T) aux[j++];
            } else if (j > hi) {
                arr[k] = (T) aux[i++];
            } else if (cmp.compare((T) aux[i], (T) aux[j]) <= 0) {
                arr[k] = (T) aux[i++]; // 稳定：相等时取左侧
            } else {
                arr[k] = (T) aux[j++];
            }
        }
    }
}