package maze;

import method.Stack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * DFS 迷宫生成器（简单难度）
 * 算法：随机化迭代深度优先搜索（回溯法）
 * 底层使用手写 Stack<int[]> 替代系统调用栈
 *
 * 生成结果保证：
 *   1. 完美迷宫（任意两格之间有且仅有一条路径）
 *   2. 所有格子均可达
 */
public class DFSMazeGenerator {

    private final Random random = new Random();

    /**
     * 对给定 MazeGrid 执行迷宫生成
     * 入口固定为左上角 (0, 0)
     */
    public void generate(MazeGrid maze) {
        int rows = maze.getRows();
        int cols = maze.getCols();
        boolean[][] visited = new boolean[rows][cols];

        Stack<int[]> stack = new Stack<>();
        int startR = 0, startC = 0;
        visited[startR][startC] = true;
        stack.push(new int[]{startR, startC});

        while (!stack.isEmpty()) {
            int[] current = stack.peek();
            int r = current[0];
            int c = current[1];

            // 收集未访问的邻居
            List<int[]> unvisited = new ArrayList<>();
            for (int[] nb : maze.allNeighbors(r, c)) {
                if (!visited[nb[0]][nb[1]]) unvisited.add(nb);
            }

            if (!unvisited.isEmpty()) {
                // 随机选一个未访问邻居
                int[] chosen = unvisited.get(random.nextInt(unvisited.size()));
                // 拆墙
                maze.removeWall(r, c, chosen[0], chosen[1]);
                visited[chosen[0]][chosen[1]] = true;
                stack.push(chosen);
            } else {
                // 无可扩展邻居，回溯
                stack.pop();
            }
        }
    }
}
