package Maze;

import Implementation.ArrayList;
import Implementation.Stack;
import Interface.MazeGenerator;

import java.util.Random;

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

            ArrayList<int[]> unvisited = new ArrayList<>();
            for (int[] nb : maze.logicalNeighbors(lr, lc))
                if (!visited[nb[0]][nb[1]]) unvisited.add(nb);

            if (!unvisited.isEmpty()) {
                int[] chosen = unvisited.get(random.nextInt(unvisited.size()));
                maze.openCorridor(lr, lc, chosen[0], chosen[1]);
                visited[chosen[0]][chosen[1]] = true;
                stack.push(chosen);
            } else {
                stack.pop();
            }
        }
    }
}
