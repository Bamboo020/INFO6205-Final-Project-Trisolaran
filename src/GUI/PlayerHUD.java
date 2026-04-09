package GUI;

import Model.Level;
import World.GameStateController;

import Interface.ListInterface;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * C9 – Player HUD (top bar).
 * Displays lives, accumulated score, current node info, and active buffs.
 */
public class PlayerHUD extends HBox {

    private static final String BG     = "#13131f";
    private static final String BORDER = "#383860";
    private static final String RED    = "#ff6b81";
    private static final String GOLD   = "#ffd93d";
    private static final String GREEN  = "#6bcb77";
    private static final String BLUE   = "#4d96ff";
    private static final String TEXT   = "#e2e2f0";
    private static final String DIM    = "#b0b0cc";

    private final GameStateController gameState;

    private final Label       titleLabel;
    private final Label       livesLabel;
    private final ProgressBar hpBar;
    private final Label       scoreLabel;
    private final Label       nodeInfoLabel;
    private final HBox        buffBox;

    public PlayerHUD(GameStateController gameState) {
        this.gameState = gameState;

        setSpacing(0);
        setPadding(new Insets(0));
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-background-color: " + BG + ";"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-width: 0 0 2 0;");

        titleLabel = new Label("⚔  MAZE EXPLORER RPG");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 17));
        titleLabel.setTextFill(Color.web(RED));
        titleLabel.setPadding(new Insets(10, 20, 10, 16));

        livesLabel = new Label("♥ ♥ ♥");
        livesLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        livesLabel.setTextFill(Color.web(RED));

        hpBar = new ProgressBar(1.0);
        hpBar.setPrefWidth(80); hpBar.setPrefHeight(8);
        hpBar.setStyle("-fx-accent: " + RED + "; -fx-control-inner-background: #2a1a2a;");

        VBox livesBox = hudCell(dimLabel("LIVES"), livesLabel, hpBar);

        scoreLabel = new Label("0 pts");
        scoreLabel.setFont(Font.font("Courier New", FontWeight.BOLD, 16));
        scoreLabel.setTextFill(Color.web(GOLD));
        VBox scoreBox = hudCell(dimLabel("PATH SCORE"), scoreLabel);

        nodeInfoLabel = new Label("—");
        nodeInfoLabel.setFont(Font.font("Arial", FontWeight.BOLD, 13));
        nodeInfoLabel.setTextFill(Color.web(TEXT));
        VBox nodeBox = hudCell(dimLabel("CURRENT NODE"), nodeInfoLabel);

        buffBox = new HBox(6);
        buffBox.setAlignment(Pos.CENTER_LEFT);
        Label buffNone = new Label("none");
        buffNone.setFont(Font.font("Arial", 13));
        buffNone.setTextFill(Color.web(DIM));
        buffBox.getChildren().add(buffNone);
        VBox buffSection = hudCell(dimLabel("ACTIVE BUFFS"), buffBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(
                titleLabel, divider(), livesBox, divider(),
                scoreBox, divider(), nodeBox, divider(), buffSection, spacer
        );
    }

    public void refresh() {
        refreshLives();
        refreshScore();
        refreshNodeInfo();
    }

    public void setActiveBuffs(ListInterface<String> buffNames) {
        buffBox.getChildren().clear();
        if (buffNames == null || buffNames.isEmpty()) {
            Label none = new Label("none");
            none.setFont(Font.font("Arial", 13));
            none.setTextFill(Color.web(DIM));
            buffBox.getChildren().add(none);
            return;
        }
        for (String buff : buffNames) buffBox.getChildren().add(buffChip(buff));
    }

    private void refreshLives() {
        int lives    = gameState.getLives();
        int maxLives = GameStateController.INITIAL_LIVES;
        StringBuilder hearts = new StringBuilder();
        for (int i = 0; i < maxLives; i++) hearts.append(i < lives ? "♥ " : "♡ ");
        livesLabel.setText(hearts.toString().trim());

        double ratio = (double) lives / maxLives;
        hpBar.setProgress(ratio);
        String barColour = ratio > 0.6 ? GREEN : (ratio > 0.3 ? GOLD : RED);
        hpBar.setStyle("-fx-accent: " + barColour + "; -fx-control-inner-background: #2a1a1a;");
    }

    private void refreshScore() {
        int score = gameState.getAccumulatedScore();
        scoreLabel.setText(String.format("%,d pts", score));
        scoreLabel.setTextFill(score > 0 ? Color.web(GOLD) : Color.web(DIM));
    }

    private void refreshNodeInfo() {
        Level node = gameState.getCurrentNode();
        if (node == null || node.isStart()) {
            nodeInfoLabel.setText("—  select a node");
            nodeInfoLabel.setTextFill(Color.web(DIM));
            return;
        }
        String diffCol;
        switch (node.getDifficulty()) {
            case EASY:   diffCol = GREEN; break;
            case MEDIUM: diffCol = GOLD;  break;
            default:     diffCol = RED;   break;
        }
        nodeInfoLabel.setText(node.getDifficulty() + "  +" + node.getScoreValue() + " pts");
        nodeInfoLabel.setTextFill(Color.web(diffCol));
    }

    private VBox hudCell(Label header, javafx.scene.Node... content) {
        VBox cell = new VBox(2);
        cell.setPadding(new Insets(8, 16, 8, 16));
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.getChildren().add(header);
        cell.getChildren().addAll(content);
        return cell;
    }

    private Region divider() {
        Region div = new Region();
        div.setPrefWidth(1); div.setPrefHeight(50);
        div.setStyle("-fx-background-color: " + BORDER + ";");
        return div;
    }

    private Label dimLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Arial", FontWeight.BOLD, 10));
        lbl.setTextFill(Color.web(DIM));
        return lbl;
    }

    private Label buffChip(String text) {
        Label chip = new Label(text);
        chip.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        chip.setTextFill(Color.web(BLUE));
        chip.setPadding(new Insets(2, 8, 2, 8));
        chip.setStyle("-fx-background-color: #1a2a40;"
                + "-fx-border-color: " + BLUE + ";"
                + "-fx-border-width: 1;"
                + "-fx-background-radius: 10; -fx-border-radius: 10;");
        return chip;
    }
}
