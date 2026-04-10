package Implementation;

import Interface.UnionFindInterface;

public class UnionFind implements UnionFindInterface {

    private final int[] parent;
    private final int[] rank;

    public UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
    }

    @Override
    public int find(int x) {
        if (parent[x] != x) parent[x] = find(parent[x]);
        return parent[x];
    }

    public boolean union(int x, int y) {
        int px = find(x), py = find(y);
        if (px == py) return false;
        if (rank[px] < rank[py]) {
            int t = px;
            px = py;
            py = t;
        }
        parent[py] = px;
        if (rank[px] == rank[py]) rank[px]++;
        return true;
    }

}