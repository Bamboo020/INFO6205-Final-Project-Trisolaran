package Controller;

import Implementation.ArrayList;
import Implementation.HashSet;
import Implementation.HeapPriorityQueue;
import Maze.Difficulty;
import Maze.MazeGrid;
import Interface.ListInterface;
import Interface.SetInterface;
import Model.*;

import java.util.Random;

/**
 * GameController - Manages game state, processes events via PriorityQueue,
 * and handles player input.
 *
 * 已修改：
 *   - 移除 Skill 技能系统，替换为 Item 道具系统
 *   - 新增 itemCells（道具拾取点）+ itemCellTypes（道具类型映射）
 *   - 新增 useItem() 方法处理三种道具的使用逻辑
 *   - movePlayer() 中增加穿墙判断
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
    private final HashSet<String> coinCells;
    private final HashSet<String> trapCells;
    private ListInterface<String> hintPath;

    // ======== 道具散布点 (HashSet + HashMap) ========
    private final HashSet<String> itemCells;                          // 含有道具的格子
    private final Implementation.HashMap<Item.ItemType> itemCellTypes; // 格子 → 道具类型

    // Enemy AI
    private final ArrayList<Enemy> enemies;
    private static final int NUM_ENEMIES = 3;
    private int playerLives;
    private static final int MAX_LIVES = 3;

    // State
    private GameState state;
    private String statusMessage;
    private final ArrayList<GameEventListener> listeners;

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
        this.itemCells = new HashSet<>();
        this.itemCellTypes = new Implementation.HashMap<>();
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

    public void newGame(int rows, int cols, Difficulty difficulty) {
        this.mazeRows = rows;
        this.mazeCols = cols;

        maze = new MazeGrid(rows, cols);
        maze.generateMaze(difficulty);

        player = new Player(0, 0);

        exitCell = cellKey(rows - 1, cols - 1);

        eventQueue = new HeapPriorityQueue<>();
        currentTick = 0;

        coinCells.clear();
        trapCells.clear();
        itemCells.clear();
        // HashMap 没有 clear，重新初始化或逐个 remove
        hintPath = new ArrayList<>();

        ListInterface<String> deadEnds = findDeadEnds();
        Random rng = new Random();

        for (String cell : deadEnds) {
            if (!cell.equals(player.getCellKey()) && !cell.equals(exitCell)) {
                double roll = rng.nextDouble();
                if (roll < 0.35) {
                    coinCells.add(cell);
                } else if (roll < 0.45) {
                    trapCells.add(cell);
                } else if (roll < 0.60) {
                    // ---- 放置道具 ----
                    placeRandomItem(cell, rng);
                }
            }
        }

        // 额外在非死胡同处放置一些道具（保证玩家能捡到）
        ArrayList<String> allOpen = new ArrayList<>();
        for (int r = 0; r < mazeRows; r++) {
            for (int c = 0; c < mazeCols; c++) {
                String key = cellKey(r, c);
                if (!key.equals(player.getCellKey()) && !key.equals(exitCell)
                        && !coinCells.contains(key) && !trapCells.contains(key)
                        && !itemCells.contains(key)) {
                    allOpen.add(key);
                }
            }
        }
        shuffle(allOpen, rng);
        int extraItems = Math.min(3, allOpen.size());
        for (int i = 0; i < extraItems; i++) {
            placeRandomItem(allOpen.get(i), rng);
        }

        state = GameState.PLAYING;
        playerLives = MAX_LIVES;

        enemies.clear();
        spawnEnemies(rng);

        statusMessage = "Find the exit! Pick up items [1=Speed 2=WallPass 3=Attack]. Lives: " + playerLives;
        notifyUpdate();
        notifyStatus(statusMessage);
    }

    /** 在指定格子放置随机道具 */
    private void placeRandomItem(String cell, Random rng) {
        Item.ItemType[] types = Item.ItemType.values();
        Item.ItemType type = types[rng.nextInt(types.length)];
        itemCells.add(cell);
        itemCellTypes.put(cell, type);
    }

    private static String cellKey(int r, int c) {
        return r + "," + c;
    }

    private ListInterface<String> findDeadEnds() {
        ArrayList<String> deadEnds = new ArrayList<>();
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

    private void spawnEnemies(Random rng) {
        ArrayList<String> allCells = new ArrayList<>();
        for (int r = 0; r < mazeRows; r++) {
            for (int c = 0; c < mazeCols; c++) {
                allCells.add(cellKey(r, c));
            }
        }
        allCells.removeElement(player.getCellKey());
        allCells.removeElement(exitCell);

        ArrayList<String> filtered = new ArrayList<>();
        for (String cell : allCells) {
            int r = Enemy.getRow(cell);
            int c = Enemy.getCol(cell);
            if ((Math.abs(r - player.getRow()) + Math.abs(c - player.getCol())) >= 8) {
                filtered.add(cell);
            }
        }

        shuffle(filtered, rng);

        int count = Math.min(NUM_ENEMIES, filtered.size());
        for (int i = 0; i < count; i++) {
            String cell = filtered.get(i);
            int r = Enemy.getRow(cell);
            int c = Enemy.getCol(cell);
            Enemy enemy = new Enemy(i, r, c, 6, 2);
            enemies.add(enemy);

            eventQueue.enqueue(new GameEvent(
                    GameEvent.EventType.ENEMY_MOVE, currentTick + 2, String.valueOf(i)));
        }
    }

    private static <E> void shuffle(ArrayList<E> list, Random rng) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            E temp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, temp);
        }
    }

    private void updateEnemies() {
        for (Enemy enemy : enemies) {
            enemy.update(maze, player.getRow(), player.getCol(), currentTick);
        }
        eventQueue.enqueue(new GameEvent(
                GameEvent.EventType.ENEMY_MOVE, currentTick + 1, "all"));
    }

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

                player.getMoveHistory().clear();
                player.moveTo(0, 0);
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

        // ---- 加速道具：连续移动 2 格 ----
        if (player.isSpeedBoostActive()) {
            int farRow = player.getRow() + dr * 2;
            int farCol = player.getCol() + dc * 2;
            if (isPassable(player.getRow(), player.getCol(), newRow, newCol) &&
                    isPassable(newRow, newCol, farRow, farCol)) {
                newRow = farRow;
                newCol = farCol;
            }
        }

        // ---- 穿墙道具：如果目标不可通行且穿墙已激活 ----
        if (!isPassable(player.getRow(), player.getCol(), newRow, newCol)
                && player.isWallPassActive()) {
            // 检查目标格是否在范围内
            if (newRow >= 0 && newRow < mazeRows && newCol >= 0 && newCol < mazeCols) {
                // 穿墙成功！消耗穿墙状态
                player.activateWallPass(false);
                notifyStatus("Wall Pass used! You phased through a wall!");
                // 直接移动到目标格（不检查是否 passable）
                player.moveTo(newRow, newCol);
                currentTick++;
                processCellEvents();
                updateEnemies();
                checkEnemyCollision();
                processEvents();
                notifyUpdate();
                return;
            }
        }

        // 普通移动：目标必须可通行
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

        // ---- 道具拾取 ----
        if (itemCells.contains(cell)) {
            Item.ItemType type = itemCellTypes.get(cell);
            if (type != null) {
                player.addItem(type);
                itemCells.remove(cell);
                notifyStatus("Picked up: " + type.icon + " " + type.displayName
                        + "! Press " + itemKeyHint(type) + " to use.");
            }
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

    /** 返回道具对应的快捷键提示 */
    private String itemKeyHint(Item.ItemType type) {
        switch (type) {
            case SPEED_BOOST: return "[1]";
            case WALL_PASS:   return "[2]";
            case ATTACK:      return "[3]";
            default:          return "[?]";
        }
    }

    /** Process all events up to current tick. */
    private void processEvents() {
        while (!eventQueue.isEmpty() && eventQueue.peek().getTick() <= currentTick) {
            GameEvent event = eventQueue.dequeue();

            switch (event.getType()) {
                case LEVEL_UP -> {
                    notifyStatus("LEVEL UP! You are now level " + player.getLevel() + "!");
                }
                case GAME_WIN -> {
                    for (GameEventListener l : listeners) {
                        l.onGameWon(player.getMoveCount(), player.getCoins(), player.getLevel());
                    }
                }
                case HINT_EXPIRE -> {
                    hintPath = new ArrayList<>();
                    notifyStatus("Path hint expired.");
                    notifyUpdate();
                }
                case ITEM_EXPIRE -> {
                    // 处理道具效果到期
                    String itemName = event.getData();
                    if ("SPEED_BOOST".equals(itemName)) {
                        player.activateSpeedBoost(false);
                        notifyStatus("Speed boost expired.");
                    }
                    notifyUpdate();
                }
                case ENEMY_MOVE -> {
                    // Enemy movement is processed in updateEnemies()
                }
                default -> { }
            }
        }
    }

    // ==================== 道具使用 ====================

    /**
     * 使用道具。由键盘输入 1/2/3 触发。
     * @param type 道具类型
     */
    public void useItem(Item.ItemType type) {
        if (state != GameState.PLAYING) return;

        // 检查玩家背包中是否有该道具
        if (player.getItemCount(type) <= 0) {
            notifyStatus("You don't have " + type.displayName + "!");
            return;
        }

        switch (type) {
            case SPEED_BOOST -> {
                if (player.isSpeedBoostActive()) {
                    notifyStatus("Speed boost is already active!");
                    return;
                }
                player.useItem(type);
                player.activateSpeedBoost(true);
                // 10 步后到期
                eventQueue.enqueue(new GameEvent(
                        GameEvent.EventType.ITEM_EXPIRE, currentTick + 10, "SPEED_BOOST"));
                notifyStatus("⚡ Speed Boost activated! Move 2 cells for 10 moves!");
            }
            case WALL_PASS -> {
                if (player.isWallPassActive()) {
                    notifyStatus("Wall pass is already ready! Move towards a wall to use it.");
                    return;
                }
                player.useItem(type);
                player.activateWallPass(true);
                notifyStatus("🧱 Wall Pass ready! Move towards a wall to phase through it!");
            }
            case ATTACK -> {
                player.useItem(type);
                // 眩晕范围内的所有敌人
                int stunCount = 0;
                int pr = player.getRow();
                int pc = player.getCol();
                for (Enemy enemy : enemies) {
                    int dist = Math.abs(enemy.getRow() - pr) + Math.abs(enemy.getCol() - pc);
                    if (dist <= Player.ATTACK_RADIUS && !enemy.isStunned()) {
                        enemy.stun(8);
                        stunCount++;
                    }
                }
                if (stunCount > 0) {
                    notifyStatus("⚔ Attack! Stunned " + stunCount + " enem"
                            + (stunCount == 1 ? "y" : "ies") + " for 8 ticks!");
                } else {
                    notifyStatus("⚔ Attack! No enemies in range (radius "
                            + Player.ATTACK_RADIUS + ").");
                }
            }
        }

        notifyUpdate();
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
    public SetInterface<String> getCoinCells() { return coinCells; }
    public SetInterface<String> getTrapCells() { return trapCells; }
    public SetInterface<String> getItemCells() { return itemCells; }
    public Implementation.HashMap<Item.ItemType> getItemCellTypes() { return itemCellTypes; }
    public ListInterface<String> getHintPath() { return hintPath; }
    public String getStatusMessage() { return statusMessage; }
    public int getCurrentTick() { return currentTick; }
    public ListInterface<Enemy> getEnemies() { return enemies; }
    public int getPlayerLives() { return playerLives; }
}