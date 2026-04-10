package Model;

import Implementation.ArrayList;
import Implementation.LinkedStack;

/**
 * Player — 玩家角色，管理背包道具与激活 buff。
 *
 * 位置不在此类维护（由 MazeGrid 负责），Player 只聚合：
 *   - 移动历史（LinkedStack，用于 undo）
 *   - 道具背包（ArrayList<Item>，含数量）
 *   - 激活 buff：speedBoostTicks（倒计时）、wallPassActive（一次性）
 *
 * 重构变更：
 *   - speedBoostActive (boolean) → speedBoostTicks (int 倒计时)
 *   - 新增 activateSpeedBoost(int ticks) / tickSpeedBoost() / getSpeedBoostTicks()
 *   - reset() 方法：迷宫重新开始时清空背包与 buff
 */
public class Player {

    // ── 移动历史（Undo 用）──────────────────────────────────────────────
    private final LinkedStack<int[]> moveHistory;
    private int moveCount;

    // ── 道具背包 ────────────────────────────────────────────────────────
    private final ArrayList<Item> inventory;

    // ── 激活 buff ───────────────────────────────────────────────────────
    /** 剩余加速步数；0 表示未激活。 */
    private int     speedBoostTicks;
    /** 穿墙激活状态（仅一次）。 */
    private boolean wallPassActive;

    // ── 常量 ─────────────────────────────────────────────────────────────
    public static final int ATTACK_RADIUS       = 5;
    public static final int SPEED_BOOST_DURATION = 10;

    // ── 构造 ─────────────────────────────────────────────────────────────

    public Player() {
        this.moveHistory     = new LinkedStack<>();
        this.inventory       = new ArrayList<>();
        this.moveCount       = 0;
        this.speedBoostTicks = 0;
        this.wallPassActive  = false;
    }

    // ── 移动历史 ─────────────────────────────────────────────────────────

    public void recordMove(int fromRow, int fromCol) {
        moveHistory.push(new int[]{fromRow, fromCol});
        moveCount++;
    }

    public boolean undoMove() {
        return !moveHistory.isEmpty() && moveHistory.pop() != null;
    }

    public int[] peekLastPosition() {
        return moveHistory.isEmpty() ? null : moveHistory.peek();
    }

    // ── 背包：拾取 ──────────────────────────────────────────────────────

    /**
     * 拾取道具：同类叠加数量，新类型新建条目。
     */
    public void addItem(Item.ItemType type) {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).getType() == type) {
                inventory.get(i).addQuantity(1);
                return;
            }
        }
        inventory.add(new Item(type));
    }

    // ── 背包：使用 ──────────────────────────────────────────────────────

    /**
     * 使用指定类型道具，数量 -1；归零则从背包移除。
     * @return 成功使用返回 true，没有该道具返回 false
     */
    public boolean useItem(Item.ItemType type) {
        for (int i = 0; i < inventory.size(); i++) {
            Item item = inventory.get(i);
            if (item.getType() == type && item.canUse()) {
                item.use();
                if (item.getQuantity() <= 0) inventory.remove(i);
                return true;
            }
        }
        return false;
    }

    /** 获取指定道具的剩余数量（无则返回 0）。 */
    public int getItemCount(Item.ItemType type) {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).getType() == type)
                return inventory.get(i).getQuantity();
        }
        return 0;
    }

    /** 获取完整背包列表（用于 UI 显示）。 */
    public ArrayList<Item> getInventory() { return inventory; }

    // ── Buff：加速 ───────────────────────────────────────────────────────

    /**
     * 激活加速 buff，持续 ticks 步。
     * 通常传 {@link #SPEED_BOOST_DURATION}。
     */
    public void activateSpeedBoost(int ticks) {
        this.speedBoostTicks = ticks;
    }

    /**
     * 每次玩家成功移动后调用，倒计时 -1。
     * @return 剩余步数（0 表示刚刚到期）
     */
    public int tickSpeedBoost() {
        if (speedBoostTicks > 0) speedBoostTicks--;
        return speedBoostTicks;
    }

    public int     getSpeedBoostTicks()  { return speedBoostTicks;  }
    public boolean isSpeedBoostActive()  { return speedBoostTicks > 0; }

    // ── Buff：穿墙 ───────────────────────────────────────────────────────

    public void    activateWallPass()    { this.wallPassActive = true;  }
    public void    consumeWallPass()     { this.wallPassActive = false; }
    public boolean isWallPassActive()    { return wallPassActive; }

    // ── 重置（开始新迷宫时调用）─────────────────────────────────────────

    /**
     * 清空背包与所有 buff，移动历史归零。
     * 供 Main 在 startMazeForNode() 开头调用。
     */
    public void reset() {
        inventory.clear();
        speedBoostTicks = 0;
        wallPassActive  = false;
        moveCount       = 0;
        while (!moveHistory.isEmpty()) moveHistory.pop();
    }

    // ── Getters ──────────────────────────────────────────────────────────

    public int getMoveCount()                    { return moveCount;    }
    public LinkedStack<int[]> getMoveHistory()   { return moveHistory;  }
}