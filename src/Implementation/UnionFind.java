package Implementation;

import Interface.UnionFindInterface;

/**
 * UnionFind —— 带路径压缩与按秩合并的并查集
 * 用于 Kruskal 迷宫生成的环检测
 */
public class UnionFind implements UnionFindInterface {

    private final int[] parent;
    private final int[] rank;

    public UnionFind(int n) {
        parent = new int[n];
        rank   = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
    }

    /** 找根节点（路径压缩） */
    @Override
    public int find(int x) {
        if (parent[x] != x) parent[x] = find(parent[x]);
        return parent[x];
    }

    /** 合并两个集合；返回是否合并成功（false 表示已在同一集合）*/
    public boolean union(int x, int y) {
        int px = find(x), py = find(y);
        if (px == py) return false;
        if (rank[px] < rank[py]) { int t = px; px = py; py = t; }
        parent[py] = px;
        if (rank[px] == rank[py]) rank[px]++;
        return true;
    }

    /** 判断两元素是否在同一集合 */
    public boolean connected(int x, int y) {
        return find(x) == find(y);
    }
}