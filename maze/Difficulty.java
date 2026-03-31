package maze;

/** 迷宫难度枚举，对应三种生成算法 */
public enum Difficulty {
    EASY,   // 随机化 DFS —— 使用手写 Stack
    MEDIUM, // Prim 算法  —— 使用手写 MinHeap
    HARD    // Kruskal    —— 使用手写 MergeSort + UnionFind
}
