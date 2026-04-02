package Model;

import Implementation.LinkedStack;
import Interface.MazeModel;

import java.util.*;

/**
 * Enemy - An AI-controlled entity that patrols the maze and chases the player.
 *
 * AI States:
 *   PATROL  - Wander randomly through the maze
 *   CHASE   - Pursue the player using Dijkstra's shortest path (via PathFinder + PriorityQueue)
 *   RETURN  - Return to patrol origin after losing sight of player
 *
 * Uses:
 *   - MazeModel (MazeGrid) for movement and neighbor lookups
 *   - PriorityQueue ADT (HeapPriorityQueue) via PathFinder for chase pathfinding
 *   - Stack ADT (LinkedStack) for patrol backtracking
 */
public class Enemy {

    public enum AIState {
        PATROL,     // Wandering randomly
        CHASE,      // Pursuing the player (Dijkstra pathfinding)
        RETURN      // Returning to patrol origin
    }

    // Position
    private int row;
    private int col;

    // AI
    private AIState state;
    private int detectionRadius;       // How far (Manhattan distance) the enemy can "see"
    private int chaseTimeout;          // Ticks before giving up chase
    private int ticksSinceLastSeen;    // Ticks since last seeing the player

    // Patrol
    private final String spawnCell;    // Where this enemy was spawned
    private final LinkedStack<String> patrolHistory;  // Movement history for backtracking
    private final Random rng;

    // Chase
    private List<String> chasePath;    // Current path to player (from Dijkstra)
    private int chasePathIndex;        // Current index in chase path

    // Movement
    private int moveInterval;          // Move every N game ticks (speed control)
    private int tickCounter;           // Internal counter

    // Visual
    private final int id;              // Unique identifier
    private boolean stunned;           // Temporarily unable to move
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

    // ==================== 坐标-键 转换工具 ====================

    /** 将 (row, col) 转为 "row,col" 字符串键 */
    public static String cellKey(int r, int c) {
        return r + "," + c;
    }

    /** 从 "row,col" 键中解析行号 */
    public static int getRow(String key) {
        return Integer.parseInt(key.split(",")[0]);
    }

    /** 从 "row,col" 键中解析列号 */
    public static int getCol(String key) {
        return Integer.parseInt(key.split(",")[1]);
    }

    // ==================== AI Update (called each game tick) ====================

    /**
     * Main AI update method. Decides state transitions and executes movement.
     *
     * @param maze       the maze model (MazeModel 接口)
     * @param playerRow  player's current row
     * @param playerCol  player's current column
     * @param currentTick current game tick
     * @return true if the enemy moved this tick
     */
    public boolean update(MazeModel maze, int playerRow, int playerCol, int currentTick) {
        // Handle stun
        if (stunned) {
            stunnedTicks--;
            if (stunnedTicks <= 0) {
                stunned = false;
            }
            return false;
        }

        // Only move on interval
        tickCounter++;
        if (tickCounter < moveInterval) {
            return false;
        }
        tickCounter = 0;

        // Calculate distance to player (Manhattan)
        int distToPlayer = Math.abs(row - playerRow) + Math.abs(col - playerCol);

        // ---- State Transitions ----
        switch (state) {
            case PATROL -> {
                if (distToPlayer <= detectionRadius) {
                    // Player detected! Switch to CHASE
                    state = AIState.CHASE;
                    ticksSinceLastSeen = 0;
                    computeChasePath(maze, playerRow, playerCol);
                } else {
                    doPatrol(maze);
                }
            }
            case CHASE -> {
                if (distToPlayer <= detectionRadius) {
                    // Still see the player, update path
                    ticksSinceLastSeen = 0;
                    computeChasePath(maze, playerRow, playerCol);
                    doChase(maze);
                } else {
                    // Lost sight of player
                    ticksSinceLastSeen++;
                    if (ticksSinceLastSeen > chaseTimeout) {
                        // Give up, return to patrol
                        state = AIState.RETURN;
                        computeReturnPath(maze);
                    } else {
                        // Continue along last known path
                        doChase(maze);
                    }
                }
            }
            case RETURN -> {
                if (distToPlayer <= detectionRadius) {
                    // Spotted player again while returning
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

    // ==================== Movement Behaviors ====================

    /**
     * PATROL: Wander randomly, preferring unvisited directions.
     * 通过 MazeModel.getNeighbors(row, col) 获取可通行邻居（返回 List<int[]>）
     */
    private void doPatrol(MazeModel maze) {
        List<int[]> rawNeighbors = maze.getNeighbors(row, col);
        List<String> neighbors = new ArrayList<>();
        for (int[] nb : rawNeighbors) {
            neighbors.add(cellKey(nb[0], nb[1]));
        }

        if (neighbors.isEmpty()) return;

        // Filter out the cell we just came from (avoid ping-pong)
        String lastCell = patrolHistory.isEmpty() ? null : patrolHistory.peek();
        List<String> preferred = new ArrayList<>();
        for (String n : neighbors) {
            if (!n.equals(lastCell)) {
                preferred.add(n);
            }
        }

        // If stuck at dead end, allow backtracking
        if (preferred.isEmpty()) {
            preferred = neighbors;
        }

        // Pick a random neighbor
        String currentKey = getCellKey();
        String next = preferred.get(rng.nextInt(preferred.size()));
        patrolHistory.push(currentKey);

        // Keep patrol history bounded
        if (patrolHistory.size() > 50) {
            // LinkedStack 没有底部移除操作，这里不额外处理
            // 对巡逻行为影响不大
        }

        moveTo(next);
    }

    /** CHASE: Follow the Dijkstra shortest path toward the player. */
    private void doChase(MazeModel maze) {
        if (chasePath.isEmpty() || chasePathIndex >= chasePath.size()) {
            // No valid path, fall back to patrol
            state = AIState.PATROL;
            doPatrol(maze);
            return;
        }

        // Move along the path
        String next = chasePath.get(chasePathIndex);
        chasePathIndex++;
        moveTo(next);
    }

    /** RETURN: Move back toward spawn point. */
    private void doReturn(MazeModel maze) {
        if (chasePath.isEmpty() || chasePathIndex >= chasePath.size()) {
            // Reached spawn or no path, resume patrol
            state = AIState.PATROL;
            patrolHistory.clear();
            return;
        }

        String next = chasePath.get(chasePathIndex);
        chasePathIndex++;
        moveTo(next);

        // Check if we've arrived at spawn
        if (getCellKey().equals(spawnCell)) {
            state = AIState.PATROL;
            patrolHistory.clear();
        }
    }

    // ==================== Pathfinding (uses PriorityQueue via PathFinder) ====================

    /** Compute chase path to player using Dijkstra's algorithm. */
    private void computeChasePath(MazeModel maze, int playerRow, int playerCol) {
        String source = getCellKey();
        String target = cellKey(playerRow, playerCol);
        chasePath = PathFinder.findShortestPath(maze, source, target);
        // Skip index 0 (current position)
        chasePathIndex = (chasePath.size() > 1) ? 1 : 0;
    }

    /** Compute return path to spawn using Dijkstra's algorithm. */
    private void computeReturnPath(MazeModel maze) {
        String source = getCellKey();
        chasePath = PathFinder.findShortestPath(maze, source, spawnCell);
        chasePathIndex = (chasePath.size() > 1) ? 1 : 0;
    }

    // ==================== Movement ====================

    private void moveTo(String cellKey) {
        this.row = getRow(cellKey);
        this.col = getCol(cellKey);
    }

    public String getCellKey() {
        return cellKey(row, col);
    }

    /** Check if enemy is on the same cell as the player. */
    public boolean isAtPlayer(int playerRow, int playerCol) {
        return this.row == playerRow && this.col == playerCol;
    }

    // ==================== Stun ====================

    /** Stun the enemy for N ticks (e.g., from a trap or player skill). */
    public void stun(int ticks) {
        this.stunned = true;
        this.stunnedTicks = ticks;
    }

    // ==================== Getters / Setters ====================

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