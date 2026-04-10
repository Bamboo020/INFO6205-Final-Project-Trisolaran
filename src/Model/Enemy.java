package Model;

import Implementation.ArrayList;
import Implementation.LinkedStack;
import Interface.ListInterface;
import Interface.MazeModel;

import java.util.Random;

public class Enemy {

    public enum AIState {
        PATROL,
        CHASE,
        RETURN
    }

    private int row;
    private int col;

    private AIState state;
    private int detectionRadius;
    private final int chaseTimeout;
    private int ticksSinceLastSeen;

    private final String spawnCell;
    private final LinkedStack<String> patrolHistory;
    private final Random rng;

    private ListInterface<String> chasePath;
    private int chasePathIndex;

    private int moveInterval;
    private int tickCounter;

    private final int id;
    private boolean stunned;
    private int stunnedTicks;

    public Enemy(int id, int row, int col, int detectionRadius, int moveInterval) {
        this.id = id;
        this.row = row;
        this.col = col;
        this.spawnCell = cellKey(row, col);
        this.state = AIState.PATROL;
        this.detectionRadius = detectionRadius;
        this.moveInterval = moveInterval;
        this.chaseTimeout = 15;
        this.ticksSinceLastSeen = 0;
        this.tickCounter = 0;
        this.patrolHistory = new LinkedStack<>();
        this.rng = new Random();
        this.chasePath = new ArrayList<>();
        this.chasePathIndex = 0;
        this.stunned = false;
        this.stunnedTicks = 0;
    }

    public static String cellKey(int r, int c) {
        return r + "," + c;
    }

    public static int getRow(String key) {
        return Integer.parseInt(key.split(",")[0]);
    }

    public static int getCol(String key) {
        return Integer.parseInt(key.split(",")[1]);
    }

    public boolean update(MazeModel maze, int playerRow, int playerCol, int currentTick) {
        if (stunned) {
            stunnedTicks--;
            if (stunnedTicks <= 0) {
                stunned = false;
            }
            return false;
        }

        tickCounter++;
        if (tickCounter < moveInterval) {
            return false;
        }
        tickCounter = 0;

        int distToPlayer = Math.abs(row - playerRow) + Math.abs(col - playerCol);

        switch (state) {
            case PATROL -> {
                if (distToPlayer <= detectionRadius) {
                    state = AIState.CHASE;
                    ticksSinceLastSeen = 0;
                    computeChasePath(maze, playerRow, playerCol);
                } else {
                    doPatrol(maze);
                }
            }
            case CHASE -> {
                if (distToPlayer <= detectionRadius) {
                    ticksSinceLastSeen = 0;
                    computeChasePath(maze, playerRow, playerCol);
                    doChase(maze);
                } else {
                    ticksSinceLastSeen++;
                    if (ticksSinceLastSeen > chaseTimeout) {
                        state = AIState.RETURN;
                        computeReturnPath(maze);
                    } else {
                        doChase(maze);
                    }
                }
            }
            case RETURN -> {
                if (distToPlayer <= detectionRadius) {
                    state = AIState.CHASE;
                    ticksSinceLastSeen = 0;
                    computeChasePath(maze, playerRow, playerCol);
                    doChase(maze);
                } else {
                    doReturn(maze);
                }
            }
        }

        return true;
    }

    private void doPatrol(MazeModel maze) {
        ListInterface<int[]> rawNeighbors = maze.getNeighbors(row, col);
        ArrayList<String> neighbors = new ArrayList<>();
        for (int[] nb : rawNeighbors) {
            neighbors.add(cellKey(nb[0], nb[1]));
        }

        if (neighbors.isEmpty()) return;

        String lastCell = patrolHistory.isEmpty() ? null : patrolHistory.peek();
        ArrayList<String> preferred = new ArrayList<>();
        for (int i = 0; i < neighbors.size(); i++) {
            String n = neighbors.get(i);
            if (!n.equals(lastCell)) {
                preferred.add(n);
            }
        }

        if (preferred.isEmpty()) {
            preferred = neighbors;
        }

        String currentKey = getCellKey();
        String next = preferred.get(rng.nextInt(preferred.size()));
        patrolHistory.push(currentKey);
        moveTo(next);
    }

    private void doChase(MazeModel maze) {
        if (chasePath.isEmpty() || chasePathIndex >= chasePath.size()) {
            state = AIState.PATROL;
            doPatrol(maze);
            return;
        }

        String next = chasePath.get(chasePathIndex);
        chasePathIndex++;
        moveTo(next);
    }

    private void doReturn(MazeModel maze) {
        if (chasePath.isEmpty() || chasePathIndex >= chasePath.size()) {
            state = AIState.PATROL;
            patrolHistory.clear();
            return;
        }

        String next = chasePath.get(chasePathIndex);
        chasePathIndex++;
        moveTo(next);

        if (getCellKey().equals(spawnCell)) {
            state = AIState.PATROL;
            patrolHistory.clear();
        }
    }

    private void computeChasePath(MazeModel maze, int playerRow, int playerCol) {
        String source = getCellKey();
        String target = cellKey(playerRow, playerCol);
        chasePath = PathFinder.findShortestPath(maze, source, target);
        chasePathIndex = (chasePath.size() > 1) ? 1 : 0;
    }

    private void computeReturnPath(MazeModel maze) {
        String source = getCellKey();
        chasePath = PathFinder.findShortestPath(maze, source, spawnCell);
        chasePathIndex = (chasePath.size() > 1) ? 1 : 0;
    }

    private void moveTo(String cellKey) {
        this.row = getRow(cellKey);
        this.col = getCol(cellKey);
    }

    public String getCellKey() {
        return cellKey(row, col);
    }

    public boolean isAtPlayer(int playerRow, int playerCol) {
        return this.row == playerRow && this.col == playerCol;
    }

    public void stun(int ticks) {
        this.stunned = true;
        this.stunnedTicks = ticks;
    }

    public int getId() { return id; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public AIState getState() { return state; }
    public int getDetectionRadius() { return detectionRadius; }
    public boolean isStunned() { return stunned; }
    public String getSpawnCell() { return spawnCell; }

    public void setDetectionRadius(int radius) { this.detectionRadius = radius; }
    public void setMoveInterval(int interval) { this.moveInterval = interval; }

    @Override
    public String toString() {
        return "Enemy#" + id + "{" + state + " @(" + row + "," + col + ")" +
                (stunned ? " STUNNED" : "") + "}";
    }
}