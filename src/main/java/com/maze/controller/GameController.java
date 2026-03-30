package com.maze.controller;

import com.maze.model.*;
import com.maze.util.MazeGenerator;
import com.maze.util.Pathfinder;

import java.util.*;

/**
 * GameController - Manages game state, processes events via PriorityQueue,
 * and handles player input.
 */
public class GameController {

    public enum Direction { UP, DOWN, LEFT, RIGHT }
    public enum GameState { PLAYING, WON, PAUSED }

    // Core components
    private MazeGenerator mazeGen;
    private MazeGraph graph;
    private Player player;

    // Event system (Priority Queue ADT)
    private HeapPriorityQueue<GameEvent> eventQueue;
    private int currentTick;

    // Maze data
    private int mazeRows;
    private int mazeCols;
    private String exitCell;
    private final Set<String> coinCells;
    private final Set<String> trapCells;
    private List<String> hintPath;

    // Enemy AI
    private final List<Enemy> enemies;
    private static final int NUM_ENEMIES = 3;
    private int playerLives;
    private static final int MAX_LIVES = 3;

    // State
    private GameState state;
    private String statusMessage;
    private final List<GameEventListener> listeners;

    /** Callback interface for UI updates. */
    public interface GameEventListener {
        void onGameUpdate();
        void onStatusMessage(String message);
        void onGameWon(int moves, int coins, int level);
        void onPlayerCaught(int livesRemaining);
    }

    public GameController() {
        this.coinCells = new HashSet<>();
        this.trapCells = new HashSet<>();
        this.hintPath = new ArrayList<>();
        this.enemies = new ArrayList<>();
        this.listeners = new ArrayList<>();
        this.statusMessage = "";
        this.playerLives = MAX_LIVES;
    }

    public void addListener(GameEventListener listener) {
        listeners.add(listener);
    }

    // ==================== Game Initialization ====================

    /** Start a new game with the given maze size. */
    public void newGame(int rows, int cols) {
        this.mazeRows = rows;
        this.mazeCols = cols;

        // Generate maze
        mazeGen = new MazeGenerator(rows, cols);
        mazeGen.generate();
        graph = mazeGen.getGraph();

        // Player starts at (1, 1)
        player = new Player(1, 1);

        // Exit at bottom-right open cell
        exitCell = findExitCell();

        // Initialize event queue
        eventQueue = new HeapPriorityQueue<>();
        currentTick = 0;

        // Place coins at dead-ends
        coinCells.clear();
        trapCells.clear();
        hintPath.clear();

        List<String> deadEnds = mazeGen.findDeadEnds();
        Random rng = new Random();
        for (String cell : deadEnds) {
            if (!cell.equals(player.getCellKey()) && !cell.equals(exitCell)) {
                if (rng.nextDouble() < 0.6) {
                    coinCells.add(cell);
                    // Schedule coin collection check event
                } else if (rng.nextDouble() < 0.3) {
                    trapCells.add(cell);
                }
            }
        }

        state = GameState.PLAYING;
        playerLives = MAX_LIVES;

        // Spawn enemies at strategic positions (away from player and exit)
        enemies.clear();
        spawnEnemies(rng);

        statusMessage = "Find the exit! Beware of enemies! Lives: " + playerLives;
        notifyUpdate();
        notifyStatus(statusMessage);
    }

    /** Find the exit cell (bottom-right area of maze). */
    private String findExitCell() {
        int rows = mazeGen.getRows();
        int cols = mazeGen.getCols();
        // Search from bottom-right for an open cell
        for (int r = rows - 2; r > 0; r--) {
            for (int c = cols - 2; c > 0; c--) {
                if (!mazeGen.isWall(r, c)) {
                    return MazeGraph.cellKey(r, c);
                }
            }
        }
        return MazeGraph.cellKey(rows - 2, cols - 2);
    }

    // ==================== Enemy AI ====================

    /** Spawn enemies at positions spread across the maze. */
    private void spawnEnemies(Random rng) {
        List<String> allCells = new ArrayList<>(graph.getAllVertices());
        // Remove player start and exit from candidates
        allCells.remove(player.getCellKey());
        allCells.remove(exitCell);

        // Remove cells too close to the player (minimum 8 Manhattan distance)
        allCells.removeIf(cell -> {
            int r = MazeGraph.getRow(cell);
            int c = MazeGraph.getCol(cell);
            return (Math.abs(r - player.getRow()) + Math.abs(c - player.getCol())) < 8;
        });

        Collections.shuffle(allCells, rng);

        int count = Math.min(NUM_ENEMIES, allCells.size());
        for (int i = 0; i < count; i++) {
            String cell = allCells.get(i);
            int r = MazeGraph.getRow(cell);
            int c = MazeGraph.getCol(cell);
            // Enemies: detection radius 6, move every 2 ticks (slower than player)
            Enemy enemy = new Enemy(i, r, c, 6, 2);
            enemies.add(enemy);

            // Schedule first enemy move event via PriorityQueue
            eventQueue.enqueue(new GameEvent(
                    GameEvent.EventType.ENEMY_MOVE, currentTick + 2, String.valueOf(i)));
        }
    }

    /** Update all enemies (called each tick). */
    private void updateEnemies() {
        for (Enemy enemy : enemies) {
            enemy.update(graph, player.getRow(), player.getCol(), currentTick);
        }
        // Schedule next enemy move event
        eventQueue.enqueue(new GameEvent(
                GameEvent.EventType.ENEMY_MOVE, currentTick + 1, "all"));
    }

    /** Check if any enemy is on the player's cell. */
    private boolean checkEnemyCollision() {
        for (Enemy enemy : enemies) {
            if (enemy.isAtPlayer(player.getRow(), player.getCol()) && !enemy.isStunned()) {
                // Player caught!
                playerLives--;
                notifyStatus("Caught by Enemy #" + enemy.getId() + "! Lives: " + playerLives);

                if (playerLives <= 0) {
                    state = GameState.WON; // Reuse WON state to end game
                    notifyStatus("GAME OVER! All lives lost.");
                    for (GameEventListener l : listeners) {
                        l.onPlayerCaught(0);
                    }
                    return true;
                }

                // Respawn player at start
                player.getMoveHistory().clear();
                player.moveTo(1, 1);

                // Stun the enemy that caught the player
                enemy.stun(5);

                for (GameEventListener l : listeners) {
                    l.onPlayerCaught(playerLives);
                }
                return true;
            }
        }
        return false;
    }

    // ==================== Player Movement ====================

    /** Move the player in the given direction. */
    public void movePlayer(Direction dir) {
        if (state != GameState.PLAYING) return;

        int dr = 0, dc = 0;
        switch (dir) {
            case UP -> dr = -1;
            case DOWN -> dr = 1;
            case LEFT -> dc = -1;
            case RIGHT -> dc = 1;
        }

        int newRow = player.getRow() + dr;
        int newCol = player.getCol() + dc;

        // Speed boost: move 2 cells if possible
        if (player.isSpeedBoostActive()) {
            int farRow = player.getRow() + dr * 2;
            int farCol = player.getCol() + dc * 2;
            if (!mazeGen.isWall(farRow, farCol) && !mazeGen.isWall(newRow, newCol)) {
                newRow = farRow;
                newCol = farCol;
            }
        }

        // Validate move
        if (mazeGen.isWall(newRow, newCol)) {
            notifyStatus("Wall! Try another direction. Green dots show open paths. Press U to undo.");
            return;
        }

        // Execute move
        player.moveTo(newRow, newCol);
        currentTick++;

        // Process cell events
        processCellEvents();

        // Update enemy AI (uses Graph ADT + Pathfinder with PriorityQueue)
        updateEnemies();

        // Check if enemy caught the player
        checkEnemyCollision();

        // Process queued events
        processEvents();

        notifyUpdate();
    }

    /** Undo the last move. */
    public void undoMove() {
        if (state != GameState.PLAYING) return;
        if (player.undoMove()) {
            currentTick++;
            updateEnemies();
            checkEnemyCollision();
            processEvents();
            notifyStatus("Move undone! (Backtracked using Stack)");
            notifyUpdate();
        } else {
            notifyStatus("No moves to undo!");
        }
    }

    /** Process events when player enters a cell. */
    private void processCellEvents() {
        String cell = player.getCellKey();

        // Coin collection
        if (coinCells.contains(cell)) {
            coinCells.remove(cell);
            player.collectCoin();
            boolean leveled = player.getLevel() > 1 &&
                    player.getExperience() < 25; // rough check
            notifyStatus("Coin collected! +25 XP. Total coins: " + player.getCoins());

            if (leveled) {
                eventQueue.enqueue(new GameEvent(
                        GameEvent.EventType.LEVEL_UP, currentTick, ""));
            }
        }

        // Trap trigger
        if (trapCells.contains(cell)) {
            trapCells.remove(cell);
            // Push player back
            player.undoMove();
            notifyStatus("Trap triggered! You were pushed back.");
            eventQueue.enqueue(new GameEvent(
                    GameEvent.EventType.TRAP_TRIGGER, currentTick, cell));
        }

        // Check for exit
        if (cell.equals(exitCell)) {
            state = GameState.WON;
            eventQueue.enqueue(new GameEvent(
                    GameEvent.EventType.GAME_WIN, currentTick, ""));
        }
    }

    /** Process all events up to current tick. */
    private void processEvents() {
        while (!eventQueue.isEmpty() && eventQueue.peek().getTick() <= currentTick) {
            GameEvent event = eventQueue.dequeue();

            switch (event.getType()) {
                case LEVEL_UP -> {
                    notifyStatus("LEVEL UP! You are now level " + player.getLevel() +
                            ". Check your skills!");
                }
                case GAME_WIN -> {
                    for (GameEventListener l : listeners) {
                        l.onGameWon(player.getMoveCount(), player.getCoins(), player.getLevel());
                    }
                }
                case HINT_EXPIRE -> {
                    hintPath.clear();
                    player.activatePathHint(false);
                    notifyStatus("Path hint expired.");
                    notifyUpdate();
                }
                case SKILL_EXPIRE -> {
                    String skillName = event.getData();
                    if ("SPEED_BOOST".equals(skillName)) {
                        player.activateSpeedBoost(false);
                        notifyStatus("Speed boost expired.");
                    } else if ("VISION".equals(skillName)) {
                        player.activateVision(false);
                        notifyStatus("Eagle vision expired.");
                    }
                    notifyUpdate();
                }
                case ENEMY_MOVE -> {
                    // Enemy movement is processed in updateEnemies()
                    // This event is used for scheduling via PriorityQueue ADT
                }
                default -> { }
            }
        }
    }

    // ==================== Skills ====================

    /** Use a skill by type. */
    public void useSkill(Skill.SkillType type) {
        if (state != GameState.PLAYING) return;

        List<Skill> skills = player.getAllSkills();
        Skill target = null;
        for (Skill s : skills) {
            if (s.getType() == type) { target = s; break; }
        }

        if (target == null || !target.canUse()) {
            notifyStatus("Skill not available!");
            return;
        }

        target.use();

        switch (type) {
            case SPEED_BOOST -> {
                player.activateSpeedBoost(true);
                eventQueue.enqueue(new GameEvent(
                        GameEvent.EventType.SKILL_EXPIRE, currentTick + 10, "SPEED_BOOST"));
                notifyStatus("Speed Boost activated for 10 moves!");
            }
            case PATH_HINT -> {
                hintPath = Pathfinder.findShortestPath(graph, player.getCellKey(), exitCell);
                player.activatePathHint(true);
                eventQueue.enqueue(new GameEvent(
                        GameEvent.EventType.HINT_EXPIRE, currentTick + 15, ""));
                notifyStatus("Path hint activated! Shortest path shown for 15 moves.");
            }
            case VISION -> {
                player.activateVision(true);
                eventQueue.enqueue(new GameEvent(
                        GameEvent.EventType.SKILL_EXPIRE, currentTick + 8, "VISION"));
                notifyStatus("Eagle Vision activated! Vision radius expanded for 8 moves.");
            }
            case BACKTRACK -> {
                player.quickBacktrack(5);
                notifyStatus("Quick Backtrack! Rewound 5 moves.");
            }
            case WALL_BREAK -> {
                notifyStatus("Wall Break ready! Click a wall adjacent to you to destroy it.");
                // Wall breaking is handled in the view layer on click
            }
            case TELEPORT -> {
                teleportToRandom();
                notifyStatus("Teleported to a random location!");
            }
        }

        notifyUpdate();
    }

    /** Teleport player to a random open cell. */
    private void teleportToRandom() {
        List<String> allCells = new ArrayList<>(graph.getAllVertices());
        Random rng = new Random();
        String target = allCells.get(rng.nextInt(allCells.size()));
        int r = MazeGraph.getRow(target);
        int c = MazeGraph.getCol(target);
        player.moveTo(r, c);
    }

    /** Attempt to break a wall at (r, c). */
    public boolean breakWall(int r, int c) {
        if (!mazeGen.isWall(r, c)) return false;
        // Must be adjacent to player
        int dr = Math.abs(r - player.getRow());
        int dc = Math.abs(c - player.getCol());
        if (dr + dc != 1) return false;

        // Break the wall in the grid
        mazeGen.getWalls()[r][c] = false;
        // Add edges in graph
        String wallKey = MazeGraph.cellKey(r, c);
        graph.addVertex(wallKey);
        // Connect to adjacent open cells
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (!mazeGen.isWall(nr, nc)) {
                graph.addEdge(wallKey, MazeGraph.cellKey(nr, nc), 1.0);
            }
        }
        notifyStatus("Wall destroyed!");
        notifyUpdate();
        return true;
    }

    // ==================== Notifications ====================

    private void notifyUpdate() {
        for (GameEventListener l : listeners) l.onGameUpdate();
    }

    private void notifyStatus(String msg) {
        this.statusMessage = msg;
        for (GameEventListener l : listeners) l.onStatusMessage(msg);
    }

    // ==================== Getters ====================

    public Player getPlayer() { return player; }
    public MazeGenerator getMazeGenerator() { return mazeGen; }
    public MazeGraph getGraph() { return graph; }
    public GameState getState() { return state; }
    public String getExitCell() { return exitCell; }
    public Set<String> getCoinCells() { return coinCells; }
    public Set<String> getTrapCells() { return trapCells; }
    public List<String> getHintPath() { return hintPath; }
    public String getStatusMessage() { return statusMessage; }
    public int getCurrentTick() { return currentTick; }
    public List<Enemy> getEnemies() { return enemies; }
    public int getPlayerLives() { return playerLives; }
}