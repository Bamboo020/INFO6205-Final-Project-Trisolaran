package Model;

/**
 * Skill - Represents a player ability in the skill tree.
 * Skills are unlocked when the player reaches the required level.
 */
public class Skill {

    public enum SkillType {
        SPEED_BOOST("Speed Boost", "Move 2 cells at once", 1),
        WALL_BREAK("Wall Break", "Destroy one wall (3 uses)", 2),
        PATH_HINT("Path Hint", "Show shortest path for 5s", 3),
        TELEPORT("Teleport", "Jump to a random open cell", 5),
        VISION("Eagle Vision", "Reveal fog in 5-cell radius", 4),
        BACKTRACK("Quick Backtrack", "Undo last 5 moves instantly", 2);

        public final String displayName;
        public final String description;
        public final int levelRequired;

        SkillType(String displayName, String description, int levelRequired) {
            this.displayName = displayName;
            this.description = description;
            this.levelRequired = levelRequired;
        }
    }

    private final SkillType type;
    private boolean unlocked;
    private int usesRemaining;
    private final int maxUses;

    public Skill(SkillType type) {
        this.type = type;
        this.unlocked = false;
        this.maxUses = (type == SkillType.WALL_BREAK) ? 3 : -1; // -1 = unlimited
        this.usesRemaining = maxUses;
    }

    public SkillType getType() { return type; }
    public String getName() { return type.displayName; }
    public String getDescription() { return type.description; }
    public int getLevelRequired() { return type.levelRequired; }
    public boolean isUnlocked() { return unlocked; }

    public void unlock() { this.unlocked = true; }

    public boolean canUse() {
        return unlocked && (maxUses == -1 || usesRemaining > 0);
    }

    public void use() {
        if (maxUses > 0) usesRemaining--;
    }

    public void resetUses() {
        usesRemaining = maxUses;
    }

    @Override
    public String toString() {
        String status = unlocked ? "UNLOCKED" : "Locked (Lv." + type.levelRequired + ")";
        return type.displayName + " [" + status + "] - " + type.description;
    }
}
