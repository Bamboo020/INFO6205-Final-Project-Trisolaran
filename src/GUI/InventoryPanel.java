package GUI;

import World.GameStateController;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;

/**
 * C9 – Inventory panel (bottom section of right sidebar).
 *
 * Displays the player's collected items with three sort options:
 *   [Type]  [Rarity]  [Qty]
 *
 * Integration hook: call setItems(List<Item> items) from InventoryController.
 */
public class InventoryPanel extends VBox {

    private static final String BG      = "#16213e";
    private static final String BG_CARD = "#0f1e3a";
    private static final String BORDER  = "#0f3460";
    private static final String RED     = "#e94560";
    private static final String GOLD    = "#f4d35e";
    private static final String GREEN   = "#57cc99";
    private static final String BLUE    = "#4a90d9";
    private static final String PURPLE  = "#9b59b6";
    private static final String TEXT    = "#a8dadc";
    private static final String DIM     = "#555555";

    public static class Item {
        public final String name;
        public final String type;
        public final String rarity;
        public final int    quantity;

        public Item(String name, String type, String rarity, int quantity) {
            this.name = name; this.type = type;
            this.rarity = rarity; this.quantity = quantity;
        }
    }

    private enum SortKey { TYPE, RARITY, QUANTITY }

    private final GameStateController gameState;
    private final VBox                itemListBox;
    private final Label               countLabel;

    private List<Item> items   = new ArrayList<>();
    private SortKey    sortKey = SortKey.TYPE;

    public InventoryPanel(GameStateController gameState) {
        this.gameState = gameState;

        setSpacing(0);
        setStyle("-fx-background-color: " + BG + ";");

        VBox header = buildHeader();
        HBox sortRow = buildSortRow();

        countLabel = new Label("0 items");
        countLabel.setFont(Font.font("Monospaced", 10));
        countLabel.setTextFill(Color.web(DIM));
        countLabel.setPadding(new Insets(2, 10, 2, 10));

        itemListBox = new VBox(4);
        itemListBox.setPadding(new Insets(6, 8, 6, 8));

        ScrollPane scroll = new ScrollPane(itemListBox);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(180);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setStyle("-fx-background: " + BG + "; -fx-background-color: " + BG + ";");

        getChildren().addAll(header, sortRow, countLabel, scroll);
        refresh();
    }

    public void setItems(List<Item> items) {
        this.items = items != null ? items : new ArrayList<>();
        refresh();
    }

    public void refresh() {
        itemListBox.getChildren().clear();
        if (items.isEmpty()) {
            Label empty = new Label("No items yet.\nCollect items while exploring!");
            empty.setFont(Font.font("Monospaced", 11));
            empty.setTextFill(Color.web(DIM));
            empty.setWrapText(true); empty.setPadding(new Insets(6));
            itemListBox.getChildren().add(empty);
            countLabel.setText("0 items");
            return;
        }
        List<Item> sorted = sortedItems();
        countLabel.setText(sorted.size() + " item" + (sorted.size() > 1 ? "s" : ""));
        for (Item item : sorted) itemListBox.getChildren().add(buildItemRow(item));
    }

    private List<Item> sortedItems() {
        List<Item> copy = new ArrayList<>(items);
        copy.sort((a, b) -> {
            switch (sortKey) {
                case TYPE:     return a.type.compareTo(b.type);
                case RARITY:   return rarityRank(a.rarity) - rarityRank(b.rarity);
                case QUANTITY: return b.quantity - a.quantity;
                default:       return 0;
            }
        });
        return copy;
    }

    private int rarityRank(String rarity) {
        switch (rarity) {
            case "Epic": return 0;
            case "Rare": return 1;
            default:     return 2;
        }
    }

    private VBox buildItemRow(Item item) {
        VBox row = new VBox(2);
        row.setPadding(new Insets(5, 8, 5, 8));
        row.setStyle("-fx-background-color: " + BG_CARD + ";"
                + "-fx-background-radius: 4;"
                + "-fx-border-color: " + rarityColour(item.rarity) + ";"
                + "-fx-border-width: 0 0 0 2; -fx-border-radius: 0;");

        HBox topLine = new HBox(6); topLine.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(itemIcon(item.type)); iconLbl.setFont(Font.font(13));
        Label nameLbl = new Label(item.name);
        nameLbl.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        nameLbl.setTextFill(Color.web(TEXT));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label qtyLbl = new Label("x" + item.quantity);
        qtyLbl.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        qtyLbl.setTextFill(Color.web(GOLD));
        topLine.getChildren().addAll(iconLbl, nameLbl, spacer, qtyLbl);

        HBox botLine = new HBox(6); botLine.setAlignment(Pos.CENTER_LEFT);
        Label typeLbl = new Label(item.type);
        typeLbl.setFont(Font.font("Monospaced", 9)); typeLbl.setTextFill(Color.web(DIM));
        Label rarityLbl = new Label(item.rarity);
        rarityLbl.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
        rarityLbl.setTextFill(Color.web(rarityColour(item.rarity)));
        rarityLbl.setPadding(new Insets(1, 4, 1, 4));
        rarityLbl.setStyle("-fx-background-color: transparent;"
                + "-fx-border-color: " + rarityColour(item.rarity) + ";"
                + "-fx-border-width: 1; -fx-border-radius: 3;");
        botLine.getChildren().addAll(typeLbl, rarityLbl);

        row.getChildren().addAll(topLine, botLine);
        return row;
    }

    private VBox buildHeader() {
        VBox header = new VBox(2);
        header.setPadding(new Insets(8, 10, 6, 10));
        header.setStyle("-fx-background-color: " + BORDER + ";");
        Label t = new Label("🎒  INVENTORY");
        t.setFont(Font.font("Monospaced", FontWeight.BOLD, 12)); t.setTextFill(Color.web(RED));
        Label s = new Label("Items collected on current path");
        s.setFont(Font.font("Monospaced", 9)); s.setTextFill(Color.web(DIM));
        header.getChildren().addAll(t, s);
        return header;
    }

    private HBox buildSortRow() {
        HBox row = new HBox(6);
        row.setPadding(new Insets(6, 10, 4, 10));
        row.setAlignment(Pos.CENTER_LEFT);
        Label sortLbl = new Label("Sort:");
        sortLbl.setFont(Font.font("Monospaced", 10)); sortLbl.setTextFill(Color.web(DIM));
        row.getChildren().addAll(sortLbl, sortBtn("Type", SortKey.TYPE),
                sortBtn("Rarity", SortKey.RARITY), sortBtn("Qty", SortKey.QUANTITY));
        return row;
    }

    private Button sortBtn(String label, SortKey key) {
        Button btn = new Button(label);
        btn.setFont(Font.font("Monospaced", 10)); btn.setPrefHeight(20);
        String col  = TEXT;
        String base = "-fx-background-color:#0f3460;-fx-border-color:" + col + ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;-fx-cursor:hand;";
        String act  = "-fx-background-color:" + col + ";-fx-border-color:" + col + ";-fx-border-width:1;-fx-background-radius:3;-fx-border-radius:3;-fx-cursor:hand;";
        btn.setTextFill(Color.web(col)); btn.setStyle(base);
        btn.setOnAction(e -> { sortKey = key; refresh(); });
        btn.setOnMouseEntered(e -> { btn.setStyle(act);  btn.setTextFill(Color.web("#0d0d1a")); });
        btn.setOnMouseExited(e  -> { btn.setStyle(base); btn.setTextFill(Color.web(col));       });
        return btn;
    }

    private String rarityColour(String rarity) {
        switch (rarity) {
            case "Epic": return PURPLE;
            case "Rare": return BLUE;
            default:     return DIM;
        }
    }

    private String itemIcon(String type) {
        switch (type) {
            case "Potion": return "⚗";
            case "Weapon": return "⚔";
            case "Armor":  return "🛡";
            case "Key":    return "🗝";
            default:       return "✦";
        }
    }
}
