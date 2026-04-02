package Implementation;

import Interface.SortInterface;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 归并排序 —— 泛型实现，支持 Comparator
 * 时间复杂度：O(n log n)，空间复杂度：O(n)
 * 提供数组版本（sort）和 List 版本（sortList），均返回排序结果（原地修改）
 */
public class MergeSort<T> implements SortInterface<T> {

    // -------- 公开 API --------

    /**
     * 对数组原地排序并返回（方便链式调用）
     * @param arr 待排序数组
     * @param cmp 比较器
     */
    @Override
    public void sort(T[] arr, Comparator<T> cmp) {
        if (arr == null || arr.length <= 1) return;
        Object[] aux = new Object[arr.length];
        mergeSort(arr, aux, 0, arr.length - 1, cmp);
    }

    /**
     * 对 List 排序并返回排序后的新 List（不修改原 List）
     * @param list 待排序列表
     * @param cmp  比较器
     * @return 排序后的新 ArrayList
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<T> sortList(List<T> list, Comparator<T> cmp) {
        if (list == null || list.size() <= 1) return new ArrayList<>(list == null ? List.of() : list);
        T[] arr = (T[]) list.toArray();
        sort(arr, cmp);
        List<T> result = new ArrayList<>(arr.length);
        for (T item : arr) result.add(item);
        return result;
    }

    // -------- 私有辅助 --------

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
        // 复制到辅助数组
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