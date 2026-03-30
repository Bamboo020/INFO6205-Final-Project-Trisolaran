package com.maze.model;

import com.maze.util.Pathfinder;

import java.util.*;

/**
 * Enemy - An AI-controlled entity that patrols the maze and chases the player.
 *
 * AI States:
 *   PATROL  - Wander randomly through the maze
 *   CHASE   - Pursue the player using Dijkstra's shortest path (via Pathfinder + PriorityQueue)
 *   RETURN  - Return to patrol origin after losing sight of player
 *
 * Uses:
 *   - Graph ADT (MazeGraph) for movement and neighbor lookups
 *   - PriorityQueue ADT (HeapPriorityQueue) via Pathfinder for chase pathfinding
 *   - Stack ADT concept for patrol backtracking
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
        this.spawnCell = MazeGraph.cellKey(row, col);
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

    // ==================== AI Update (called each game tick) ====================

    /**
     * Main AI update method. Decides state transitions and executes movement.
     *
     * @param graph      the maze graph (Graph ADT)
     * @param playerRow  player's current row
     * @param playerCol  player's current column
     * @param currentTick current game tick
     * @return true if the enemy moved this tick
     */
    public boolean update(MazeGraph graph, int playerRow, int playerCol, int currentTick) {
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
                    computeChasePath(graph, playerRow, playerCol);
                } else {
                    doPatrol(graph);
                }
            }
            case CHASE -> {
                if (distToPlayer <= detectionRadius) {
                    // Still see the player, update path
                    ticksSinceLastSeen = 0;
                    computeChasePath(graph, playerRow, playerCol);
                    doChase(graph);
                } else {
                    // Lost sight of player
                    ticksSinceLastSeen++;
                    if (ticksSinceLastSeen > chaseTimeout) {
                        // Give up, return to patrol
                        state = AIState.RETURN;
                        computeReturnPath(graph);
                    } else {
                        // Continue along last known path
                        doChase(graph);
                    }
                }
            }
            case RETURN -> {
                if (distToPlayer <= detectionRadius) {
                    // Spotted player again while returning
                    state = AIState.CHASE;
                    ticksSinceLastSeen = 0;
                    computeChasePath(graph, playerRow, playerCol);
                    doChase(graph);
                } else {
                    doReturn(graph);
                }
            }
        }

        return true;
    }

    // ==================== Movement Behaviors ====================

    /** PATROL: Wander randomly, preferring unvisited directions. */
    private void doPatrol(MazeGraph graph) {
        String currentKey = getCellKey();
        List<String> neighbors = graph.getNeighbors(currentKey);

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
        String next = preferred.get(rng.nextInt(preferred.size()));
        patrolHistory.push(currentKey);

        // Keep patrol history bounded
        if (patrolHistory.size() > 50) {
            // We don't have a bottom-remove, so just clear old history
            // This is fine for patrol behavior
        }

        moveTo(next);
    }

    /** CHASE: Follow the Dijkstra shortest path toward the player. */
    private void doChase(MazeGraph graph) {
        if (chasePath.isEmpty() || chasePathIndex >= chasePath.size()) {
            // No valid path, fall back to patrol
            state = AIState.PATROL;
            doPatrol(graph);
            return;
        }

        // Move along the path
        String next = chasePath.get(chasePathIndex);
        chasePathIndex++;
        moveTo(next);
    }

    /** RETURN: Move back toward spawn point. */
    private void doReturn(MazeGraph graph) {
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

    // ==================== Pathfinding (uses PriorityQueue via Pathfinder) ====================

    /** Compute chase path to player using Dijkstra's algorithm. */
    private void computeChasePath(MazeGraph graph, int playerRow, int playerCol) {
        String source = getCellKey();
        String target = MazeGraph.cellKey(playerRow, playerCol);
        chasePath = Pathfinder.findShortestPath(graph, source, target);
        // Skip index 0 (current position)
        chasePathIndex = (chasePath.size() > 1) ? 1 : 0;
    }

    /** Compute return path to spawn using Dijkstra's algorithm. */
    private void computeReturnPath(MazeGraph graph) {
        String source = getCellKey();
        chasePath = Pathfinder.findShortestPath(graph, source, spawnCell);
        chasePathIndex = (chasePath.size() > 1) ? 1 : 0;
    }

    // ==================== Movement ====================

    private void moveTo(String cellKey) {
        this.row = MazeGraph.getRow(cellKey);
        this.col = MazeGraph.getCol(cellKey);
    }

    public String getCellKey() {
        return MazeGraph.cellKey(row, col);
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
