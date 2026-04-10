package Interface;

import Maze.CellState;
import Maze.Difficulty;

public interface MazeModel {

    CellState getCell(int row, int col);

    void setCell(int row, int col, CellState state);

    ListInterface<int[]> getNeighbors(int row, int col);

    int getWidth();

    int getHeight();

    void generateMaze(Difficulty d);
}