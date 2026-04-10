package Model;

import Implementation.ArrayList;
import Implementation.LinkedStack;

public class Player {

    private final ArrayList<Item> inventory;

    private int     speedBoostTicks;
    private boolean wallPassActive;

    public static final int ATTACK_RADIUS       = 5;
    public static final int SPEED_BOOST_DURATION = 10;

    public Player() {
        this.inventory       = new ArrayList<>();
        this.speedBoostTicks = 0;
        this.wallPassActive  = false;
    }

    public void addItem(Item.ItemType type) {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).getType() == type) {
                inventory.get(i).addQuantity(1);
                return;
            }
        }
        inventory.add(new Item(type));
    }

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

    public int getItemCount(Item.ItemType type) {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.get(i).getType() == type)
                return inventory.get(i).getQuantity();
        }
        return 0;
    }

    public ArrayList<Item> getInventory() { return inventory; }

    public void activateSpeedBoost(int ticks) {
        this.speedBoostTicks = ticks;
    }

    public int tickSpeedBoost() {
        if (speedBoostTicks > 0) speedBoostTicks--;
        return speedBoostTicks;
    }

    public int     getSpeedBoostTicks()  { return speedBoostTicks;  }
    public boolean isSpeedBoostActive()  { return speedBoostTicks > 0; }

    public void    activateWallPass()    { this.wallPassActive = true;  }
    public void    consumeWallPass()     { this.wallPassActive = false; }
    public boolean isWallPassActive()    { return wallPassActive; }

    public void reset() {
        inventory.clear();
        speedBoostTicks = 0;
        wallPassActive  = false;
    }
}