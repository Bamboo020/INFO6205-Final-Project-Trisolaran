package Model;

import Implementation.ArrayList;
import Implementation.LinkedStack;
import Interface.ListInterface;

/**
 * Player - The player character controlled by keyboard input.
 * Tracks position, movement history (via Stack), level, experience,
 * and items (via ArrayList).
 *
 * 已修改：
 *   - 移除 SkillTreeBST 技能树，改用 ArrayList<Item> 道具背包
 *   - 新增穿墙 (wallPassActive) 状态
 *   - 新增攻击范围常量
 */
public class Player {

    // Position
    private int row;
    private int col;

    // Stats
    private int level;
    private int experience;
    private int coins;
    private int moveCount;

    // Movement history for undo (Stack ADT)
    private final LinkedStack<int[]> moveHistory;

    // ======== 道具背包 (ArrayList ADT) ========
    private final ArrayList<Item> inventory;

    // Active buffs
    private boolean speedBoostActive;
    private boolean wallPassActive;     // 穿墙激活状态（仅一次）

    // Vision
    private int visionRadius;

    // Constants
    private static final int BASE_VISION_RADIUS = 2;
    private static final int EXP_PER_LEVEL = 100;
    public  static final int ATTACK_RADIUS = 5;   // 攻击道具影响范围（曼哈顿距离）

    public Player(int startRow, int startCol) {
        this.row = startRow;
        this.col = startCol;
        this.level = 1;
        this.experience = 0;
        this.coins = 0;
        this.moveCount = 0;
        this.moveHistory = new LinkedStack<>();
        this.inventory = new ArrayList<>();
        this.visionRadius = BASE_VISION_RADIUS;
        this.speedBoostActive = false;
        this.wallPassActive = false;
    }

    // ==================== Movement ====================

    /** Move the player to a new position, saving history for undo. */
    public void moveTo(int newRow, int newCol) {
        moveHistory.push(new int[]{row, col});
        this.row = newRow;
        this.col = newCol;
        this.moveCount++;
    }

    /** Undo the last move (backtrack). Returns true if successful. */
    public boolean undoMove() {
        if (moveHistory.isEmpty()) return false;
        int[] prev = moveHistory.pop();
        this.row = prev[0];
        this.col = prev[1];
        return true;
    }

    /** Quick backtrack: undo multiple moves at once. */
    public void quickBacktrack(int steps) {
        for (int i = 0; i < steps && !moveHistory.isEmpty(); i++) {
            int[] prev = moveHistory.pop();
            this.row = prev[0];
            this.col = prev[1];
        }
    }

    /** Get the current position as a cell key. */
    public String getCellKey() {
        return row + "," + col;
    }

    // ==================== Experience & Leveling ====================

    /** Add experience and check for level up. Returns true if leveled up. */
    public boolean addExperience(int amount) {
        this.experience += amount;
        if (experience >= level * EXP_PER_LEVEL) {
            experience -= level * EXP_PER_LEVEL;
            level++;
            return true;
        }
        return false;
    }

    /** Collect a coin. */
    public void collectCoin() {
        coins++;
        addExperience(25);
    }

    // ==================== 道具系统 ====================

    /**
     * 添加道具到背包。如果已有同类道具则叠加数量。
     * 使用 ArrayList 遍历查找。
     */
    public void addItem(Item.ItemType type) {
        for (int i = 0; i < inventory.size(); i++) {
            Item existing = inventory.get(i);
            if (existing.getType() == type) {
                existing.addQuantity(1);
                return;
            }
        }
        // 没有该类型道具，新增
        inventory.add(new Item(type));
    }

    /**
     * 使用指定类型的道具。
     * @return 如果成功使用返回 true
     */
    public boolean useItem(Item.ItemType type) {
        for (int i = 0; i < inventory.size(); i++) {
            Item item = inventory.get(i);
            if (item.getType() == type && item.canUse()) {
                item.use();
                // 如果数量归零，从背包移除
                if (item.getQuantity() <= 0) {
                    inventory.remove(i);
                }
                return true;
            }
        }
        return false;
    }

    /** 获取指定道具的剩余数量 */
    public int getItemCount(Item.ItemType type) {
        for (int i = 0; i < inventory.size(); i++) {
            Item item = inventory.get(i);
            if (item.getType() == type) {
                return item.getQuantity();
            }
        }
        return 0;
    }

    /** 获取所有道具列表（用于 UI 显示） */
    public ArrayList<Item> getInventory() {
        return inventory;
    }

    // ==================== Buff 状态 ====================

    /** Activate speed boost. */
    public void activateSpeedBoost(boolean active) {
        this.speedBoostActive = active;
    }

    /** Activate wall pass (穿墙). */
    public void activateWallPass(boolean active) {
        this.wallPassActive = active;
    }

    // ==================== Getters ====================

    public int getRow() { return row; }
    public int getCol() { return col; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public int getExpToNextLevel() { return level * EXP_PER_LEVEL; }
    public int getCoins() { return coins; }
    public int getMoveCount() { return moveCount; }
    public int getVisionRadius() { return visionRadius; }
    public boolean isSpeedBoostActive() { return speedBoostActive; }
    public boolean isWallPassActive() { return wallPassActive; }
    public LinkedStack<int[]> getMoveHistory() { return moveHistory; }
}