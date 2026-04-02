package Model;

import Implementation.HeapPriorityQueue;
import Maze.Difficulty;
import Maze.MazeGrid;

import java.util.*;

/**
 * GameController - Manages game state, processes events via PriorityQueue,
 * and handles player input.
 *
 * Maze generation delegates to MazeGrid.generateMaze(Difficulty), which
 * internally uses DFSMazeGenerator (EASY), PrimMazeGenerator (MEDIUM), or
 * KruskalMazeGenerator (HARD) — all from the main project's Maze package.
 */
public class GameController {

    public enum Direction { UP, DOWN, LEFT, RIGHT }
    public enum GameState { PLAYING, WON, PAUSED }

    // Core components
    private MazeGrid maze;
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

    /**
     * Start a new game with the given maze size and difficulty.
     * Maze generation uses MazeGrid.generateMaze(difficulty):
     *   EASY   -> DFSMazeGenerator
     *   MEDIUM -> PrimMazeGenerator
     *   HARD   -> KruskalMazeGenerator
     */
    public void newGame(int rows, int cols, Difficulty difficulty) {
        this.mazeRows = rows;
        this.mazeCols = cols;

        // Generate maze using the three generators in the main project
        maze = new MazeGrid(rows, cols);
        maze.generateMaze(difficulty);

        // Player starts at top-left (0, 0)
        player = new Player(0, 0);

        // Exit at bottom-right corner
        exitCell = cellKey(rows - 1, cols - 1);

        // Initialize event queue
        eventQueue = new HeapPriorityQueue<>();
        currentTick = 0;

        // Place coins/traps at dead-end cells
        coinCells.clear();
        trapCells.clear();
        hintPath.clear();

        List<String> deadEnds = findDeadEnds();
        Random rng = new Random();
        for (String cell : deadEnds) {
            if (!cell.equals(player.getCellKey()) && !cell.equals(exitCell)) {
                if (rng.nextDouble() < 0.6) {
                    coinCells.add(cell);
                } else if (rng.nextDouble() < 0.3) {
                    trapCells.add(cell);
                }
            }
        }

        state = GameState.PLAYING;
        playerLives = MAX_LIVES;

        // Spawn enemies at strategic positions
        enemies.clear();
        spawnEnemies(rng);

        statusMessage = "Find the exit! Beware of enemies! Lives: " + playerLives;
        notifyUpdate();
        notifyStatus(statusMessage);
    }

    /** Utility: create a cell key from row and column. */
    private static String cellKey(int r, int c) {
        return r + "," + c;
    }

    /**
     * Find dead-end cells (cells with exactly 1 passage-neighbor).
     * Uses MazeGrid.getNeighbors() which returns only passable neighbors.
     */
    private List<String> findDeadEnds() {
        List<String> deadEnds = new ArrayList<>();
        for (int r = 0; r < mazeRows; r++) {
            for (int c = 0; c < mazeCols; c++) {
                if (maze.getNeighbors(r, c).size() == 1) {
                    deadEnds.add(cellKey(r, c));
                }
            }
        }
        return deadEnds;
    }

    // ==================== Enemy AI ====================

    /** Spawn enemies at positions spread across the maze. */
    private void spawnEnemies(Random rng) {
        // All cells are valid positions (no wall-cells in main 2's maze model)
        List<String> allCells = new ArrayList<>();
        for (int r = 0; r < mazeRows; r++) {
            for (int c = 0; c < mazeCols; c++) {
                allCells.add(cellKey(r, c));
            }
        }
        allCells.remove(player.getCellKey());
        allCells.remove(exitCell);

        // Remove cells too close to player (minimum 8 Manhattan distance)
        allCells.removeIf(cell -> {
            int r = Enemy.getRow(cell);
            int c = Enemy.getCol(cell);
            return (Math.abs(r - player.getRow()) + Math.abs(c - player.getCol())) < 8;
        });

        Collections.shuffle(allCells, rng);

        int count = Math.min(NUM_ENEMIES, allCells.size());
        for (int i = 0; i < count; i++) {
            String cell = allCells.get(i);
            int r = Enemy.getRow(cell);
            int c = Enemy.getCol(cell);
            // Detection radius 6, move every 2 ticks (slower than player)
            Enemy enemy = new Enemy(i, r, c, 6, 2);
            enemies.add(enemy);

            // Schedule first enemy move via PriorityQueue
            eventQueue.enqueue(new GameEvent(
                    GameEvent.EventType.ENEMY_MOVE, currentTick + 2, String.valueOf(i)));
        }
    }

    /** Update all enemies (called each tick). */
    private void updateEnemies() {
        for (Enemy enemy : enemies) {
            enemy.update(maze, player.getRow(), player.getCol(), currentTick);
        }
        eventQueue.enqueue(new GameEvent(
                GameEvent.EventType.ENEMY_MOVE, currentTick + 1, "all"));
    }

    /** Check if any enemy is on the player's cell. */
    private boolean checkEnemyCollision() {
        for (Enemy enemy : enemies) {
            if (enemy.isAtPlayer(player.getRow(), player.getCol()) && !enemy.isStunned()) {
                playerLives--;
                notifyStatus("Caught by Enemy #" + enemy.getId() + "! Lives: " + playerLives);

                if (playerLives <= 0) {
                    state = GameState.WON;
                    notifyStatus("GAME OVER! All lives lost.");
                    for (GameEventListener l : listeners) {
                        l.onPlayerCaught(0);
                    }
                    return true;
                }

                // Respawn player at start
                player.getMoveHistory().clear();
                player.moveTo(0, 0);

                // Stun the catching enemy
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
            case UP    -> dr = -1;
            case DOWN  -> dr =  1;
            case LEFT  -> dc = -1;
            case RIGHT -> dc =  1;
        }

        int newRow = player.getRow() + dr;
        int newCol = player.getCol() + dc;

        // Speed boost: move 2 cells if both steps are passable
        if (player.isSpeedBoostActive()) {
            int farRow = player.getRow() + dr * 2;
            int farCol = player.getCol() + dc * 2;
            if (isPassable(player.getRow(), player.getCol(), newRow, newCol) &&
                    isPassable(newRow, newCol, farRow, farCol)) {
                newRow = farRow;
                newCol = farCol;
            }
        }

        // Validate move: target must be a passable neighbor
        if (!isPassable(player.getRow(), player.getCol(), newRow, newCol)) {
            notifyStatus("Wall! Try another direction. Press U to undo.");
            return;
        }

        player.moveTo(newRow, newCol);
        currentTick++;

        processCellEvents();
        updateEnemies();
        checkEnemyCollision();
        processEvents();

        notifyUpdate();
    }

    /**
     * Check if (toRow, toCol) is a passable neighbor of (fromRow, fromCol).
     * Uses MazeGrid.getNeighbors() which returns only wall-free adjacent cells.
     */
    private boolean isPassable(int fromRow, int fromCol, int toRow, int toCol) {
        if (toRow < 0 || toRow >= mazeRows || toCol < 0 || toCol >= mazeCols) return false;
        for (int[] nb : maze.getNeighbors(fromRow, fromCol)) {
            if (nb[0] == toRow && nb[1] == toCol) return true;
        }
        return false;
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
            notifyStatus("Coin collected! +25 XP. Total coins: " + player.getCoins());
        }

        // Trap trigger: push player back
        if (trapCells.contains(cell)) {
            trapCells.remove(cell);
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
                hintPath = PathFinder.findShortestPath(maze, player.getCellKey(), exitCell);
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

    /** Teleport player to a random cell. */
    private void teleportToRandom() {
        Random rng = new Random();
        int r = rng.nextInt(mazeRows);
        int c = rng.nextInt(mazeCols);
        player.moveTo(r, c);
    }

    /**
     * Attempt to break the wall between the player and an adjacent cell (r, c).
     * In main 2's maze model, walls are between cells (not cell-obstacles),
     * so (r, c) is the target cell the player wants to reach, not a wall cell.
     */
    public boolean breakWall(int r, int c) {
        int playerRow = player.getRow();
        int playerCol = player.getCol();
        int dr = Math.abs(r - playerRow);
        int dc = Math.abs(c - playerCol);
        // Must be exactly adjacent (Manhattan distance = 1)
        if (dr + dc != 1) return false;
        // Must be in bounds
        if (r < 0 || r >= mazeRows || c < 0 || c >= mazeCols) return false;
        // Must currently be blocked (wall exists between player and target)
        if (isPassable(playerRow, playerCol, r, c)) return false;

        // Remove the wall between player's cell and (r, c)
        maze.removeWall(playerRow, playerCol, r, c);
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
    public MazeGrid getMaze() { return maze; }
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
