package Model;

import Implementation.LinkedStack;
import Implementation.SkillTreeBST;
import Model.Skill.SkillType;

import java.util.List;

/**
 * Player - The player character controlled by keyboard input.
 * Tracks position, movement history (via Stack), level, experience,
 * and skills (via BST).
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

    // Skill tree (BST ADT)
    private final SkillTreeBST<Integer, Skill> skillTree;

    // Active buffs
    private boolean speedBoostActive;
    private boolean pathHintActive;
    private boolean visionActive;
    private int visionRadius;

    // Constants
    private static final int BASE_VISION_RADIUS = 2;
    private static final int EXP_PER_LEVEL = 100;

    public Player(int startRow, int startCol) {
        this.row = startRow;
        this.col = startCol;
        this.level = 1;
        this.experience = 0;
        this.coins = 0;
        this.moveCount = 0;
        this.moveHistory = new LinkedStack<>();
        this.skillTree = new SkillTreeBST<>();
        this.visionRadius = BASE_VISION_RADIUS;
        this.speedBoostActive = false;
        this.pathHintActive = false;
        this.visionActive = false;

        // Initialize skill tree with all skills (sorted by level requirement)
        for (SkillType st : SkillType.values()) {
            skillTree.insert(st.levelRequired * 10 + st.ordinal(), new Skill(st));
        }
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
            unlockSkillsForLevel();
            return true;
        }
        return false;
    }

    /** Unlock all skills whose requirement <= current level. */
    private void unlockSkillsForLevel() {
        // key = levelRequired * 10 + ordinal, so threshold = level * 10 + 9
        List<Skill> available = skillTree.getUpTo(level * 10 + 9);
        for (Skill s : available) {
            if (!s.isUnlocked()) {
                s.unlock();
            }
        }
    }

    /** Collect a coin. */
    public void collectCoin() {
        coins++;
        addExperience(25);
    }

    // ==================== Skills ====================

    /** Get all skills (sorted by level requirement via in-order traversal). */
    public List<Skill> getAllSkills() {
        return skillTree.inOrderTraversal();
    }

    /** Get unlocked skills. */
    public List<Skill> getUnlockedSkills() {
        return skillTree.getUpTo(level * 10 + 9).stream()
                .filter(Skill::isUnlocked)
                .toList();
    }

    /** Activate speed boost. */
    public void activateSpeedBoost(boolean active) {
        this.speedBoostActive = active;
    }

    /** Activate path hint. */
    public void activatePathHint(boolean active) {
        this.pathHintActive = active;
    }

    /** Activate eagle vision. */
    public void activateVision(boolean active) {
        this.visionActive = active;
        this.visionRadius = active ? 5 : BASE_VISION_RADIUS;
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
    public boolean isPathHintActive() { return pathHintActive; }
    public boolean isVisionActive() { return visionActive; }
    public LinkedStack<int[]> getMoveHistory() { return moveHistory; }
}
