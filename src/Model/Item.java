package Model;

public class Item {

    public enum ItemType {
        SPEED_BOOST("Speed Boost",  "Move 2 cells at once for 10 moves",  "⚡"),
        WALL_PASS  ("Wall Pass",    "Pass through one wall (one-time)",   "🧱"),
        ATTACK     ("Attack",       "Stun nearby enemies for 8 ticks",    "⚔");

        public final String displayName;
        public final String description;
        public final String icon;

        ItemType(String displayName, String description, String icon) {
            this.displayName = displayName;
            this.description = description;
            this.icon = icon;
        }
    }

    private final ItemType type;
    private int quantity;

    public Item(ItemType type, int quantity) {
        this.type = type;
        this.quantity = quantity;
    }

    public Item(ItemType type) {
        this(type, 1);
    }

    public ItemType getType() { return type; }
    public String   getName() { return type.displayName; }
    public String   getDescription() { return type.description; }
    public String   getIcon() { return type.icon; }
    public int      getQuantity() { return quantity; }

    public void addQuantity(int amount) { this.quantity += amount; }

    public boolean use() {
        if (quantity <= 0) return false;
        quantity--;
        return true;
    }

    public boolean canUse() {
        return quantity > 0;
    }

    @Override
    public String toString() {
        return type.icon + " " + type.displayName + " x" + quantity;
    }
}