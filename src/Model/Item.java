package Model;

/**
 * Item - 可在迷宫中拾取的道具。
 * 替代原有的 Skill 系统，道具通过在地图上随机生成来获取。
 *
 * 三种道具类型：
 *   SPEED_BOOST  - 加速，持续 10 步移动 2 格
 *   WALL_PASS    - 穿墙，一次性使用，可穿过一面墙
 *   ATTACK       - 攻击，眩晕附近范围内的敌人
 */
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

    /** 增加数量（拾取同类道具时叠加） */
    public void addQuantity(int amount) { this.quantity += amount; }

    /** 使用一个道具，返回是否还有剩余 */
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