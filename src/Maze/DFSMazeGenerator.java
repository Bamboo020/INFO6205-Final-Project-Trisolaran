package Maze;

import Implementation.ArrayList;
import Implementation.Stack;
import Interface.MazeGenerator;

import java.util.Random;

/**
 * DFS 迷宫生成器（简单难度）
 * 算法：随机化迭代深度优先搜索（回溯法）
 * 底层使用手写 Stack<int[]> 替代系统调用栈
 *
 * 生成结果保证：
 *   1. 完美迷宫（任意两逻辑格之间有且仅有一条路径）
 *   2. 所有逻辑格均可达
 */
public class DFSMazeGenerator implements MazeGenerator {

    private final Random random = new Random();

    @Override
    public void generate(MazeGrid maze) {
        int logRows = maze.getLogRows();
        int logCols = maze.getLogCols();
        boolean[][] visited = new boolean[logRows][logCols];

        Stack<int[]> stack = new Stack<>();
        visited[0][0] = true;
        stack.push(new int[]{0, 0});

        while (!stack.isEmpty()) {
            int[] cur = stack.peek();
            int lr = cur[0], lc = cur[1];

            // 收集未访问的逻辑邻居
            ArrayList<int[]> unvisited = new ArrayList<>();
            for (int[] nb : maze.logicalNeighbors(lr, lc))
                if (!visited[nb[0]][nb[1]]) unvisited.add(nb);

            if (!unvisited.isEmpty()) {
                // 随机选一个未访问的逻辑邻居，打通 3 格宽走廊
                int[] chosen = unvisited.get(random.nextInt(unvisited.size()));
                maze.openCorridor(lr, lc, chosen[0], chosen[1]);
                visited[chosen[0]][chosen[1]] = true;
                stack.push(chosen);
            } else {
                // 无可扩展邻居，回溯
                stack.pop();
            }
        }
    }
}
