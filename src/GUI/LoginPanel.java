package GUI;

import World.AuthController;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Full-screen login / register panel.
 *
 * Uses MySQL via AuthController.
 * Shows a warning banner when running in offline (in-memory) mode.
 *
 * On success fires AuthCallback(username) so Main.java can switch scene.
 */
public class LoginPanel extends StackPane {

    // ------------------------------------------------------------------ //
    //  Callback
    // ------------------------------------------------------------------ //

    @FunctionalInterface
    public interface AuthCallback {
        void onSuccess(String username);
    }

    // ------------------------------------------------------------------ //
    //  Colours
    // ------------------------------------------------------------------ //

    private static final String BG      = "#0d0d1a";
    private static final String CARD_BG = "#16213e";
    private static final String BORDER  = "#0f3460";
    private static final String RED     = "#e94560";
    private static final String GREEN   = "#57cc99";
    private static final String GOLD    = "#f4d35e";
    private static final String ORANGE  = "#f4a261";
    private static final String TEXT    = "#a8dadc";
    private static final String DIM     = "#b0b0cc";

    // ------------------------------------------------------------------ //
    //  Fields
    // ------------------------------------------------------------------ //

    private final AuthController auth;
    private AuthCallback         onSuccess;
    private boolean              loginMode = true;

    private final TextField     usernameField;
    private final PasswordField passwordField;
    private final PasswordField confirmField;
    private final Label         confirmLabel;
    private final Label         errorLabel;
    private final Button        submitBtn;
    private final Button        loginTab;
    private final Button        registerTab;
    private final Label         switchHint;
    private final Label         dbStatusLabel;

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //

    public LoginPanel(AuthController auth) {
        this.auth = auth;

        setStyle("-fx-background-color: " + BG + ";");

        // Background canvas
        Canvas bg = new Canvas(1200, 760);
        drawBackground(bg);

        // ---- Card ----
        VBox card = new VBox(12);
        card.setPrefWidth(420);
        card.setMaxWidth(420);
        card.setPadding(new Insets(36, 40, 30, 40));
        card.setAlignment(Pos.CENTER);
        card.setStyle(
                "-fx-background-color: " + CARD_BG + ";"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-color: " + BORDER + ";"
                        + "-fx-border-width: 1; -fx-border-radius: 12;"
                        + "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.7),28,0,0,6);");

        // Title
        Label titleLbl = new Label("⚔  MAZE EXPLORER RPG");
        titleLbl.setFont(Font.font("Monospaced", FontWeight.BOLD, 17));
        titleLbl.setTextFill(Color.web(RED));

        Label subtitleLbl = new Label("INFO 6205  ·  Spring 2026");
        subtitleLbl.setFont(Font.font("Monospaced", 11));
        subtitleLbl.setTextFill(Color.web(DIM));

        // DB status banner
        dbStatusLabel = buildDbBanner();

        // Tabs
        loginTab    = tabBtn("  Login  ");
        registerTab = tabBtn(" Register ");
        loginTab.setOnAction(e    -> switchMode(true));
        registerTab.setOnAction(e -> switchMode(false));
        HBox tabs = new HBox(0, loginTab, registerTab);
        tabs.setAlignment(Pos.CENTER);
        tabs.setStyle("-fx-background-color: " + BORDER + "; -fx-background-radius: 6;");

        // Fields
        usernameField = inputField("Username");
        passwordField = pwField("Password");
        confirmLabel  = fieldLabel("Confirm Password");
        confirmField  = pwField("Re-enter password");

        // Error / success label
        errorLabel = new Label("");
        errorLabel.setFont(Font.font("Monospaced", 11));
        errorLabel.setTextFill(Color.web(RED));
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(340);
        errorLabel.setTextAlignment(TextAlignment.CENTER);

        // Submit button
        submitBtn = new Button("LOGIN");
        submitBtn.setFont(Font.font("Monospaced", FontWeight.BOLD, 14));
        submitBtn.setPrefWidth(340);
        submitBtn.setPrefHeight(42);
        submitBtn.setTextFill(Color.web("#0d0d1a"));
        applySubmitStyle(true);
        submitBtn.setOnAction(e -> handleSubmit());

        // Enter key shortcuts
        passwordField.setOnAction(e -> handleSubmit());
        confirmField.setOnAction(e  -> handleSubmit());
        usernameField.setOnAction(e -> passwordField.requestFocus());

        // Switch hint
        switchHint = new Label("");
        switchHint.setFont(Font.font("Monospaced", 10));
        switchHint.setTextFill(Color.web(DIM));
        switchHint.setTextAlignment(TextAlignment.CENTER);

        // Guest hint
        Label guestHint = new Label("Quick test:  guest / guest");
        guestHint.setFont(Font.font("Monospaced", 10));
        guestHint.setTextFill(Color.web(DIM));

        card.getChildren().addAll(
                titleLbl, subtitleLbl,
                dbStatusLabel,
                tabs,
                fieldLabel("Username"), usernameField,
                fieldLabel("Password"), passwordField,
                confirmLabel, confirmField,
                errorLabel,
                submitBtn,
                switchHint,
                guestHint
        );

        getChildren().addAll(bg, card);
        StackPane.setAlignment(card, Pos.CENTER);
        switchMode(true);
    }

    // ------------------------------------------------------------------ //
    //  Public API
    // ------------------------------------------------------------------ //

    public void setOnSuccess(AuthCallback cb) { this.onSuccess = cb; }

    /** Clears all fields and resets to login mode. */
    public void reset() {
        usernameField.clear();
        passwordField.clear();
        confirmField.clear();
        errorLabel.setText("");
        dbStatusLabel.setText(auth.isDbAvailable()
                ? "🟢  Connected to MySQL database"
                : "🟡  Offline mode – using in-memory storage");
        dbStatusLabel.setTextFill(auth.isDbAvailable()
                ? Color.web(GREEN) : Color.web(ORANGE));
        switchMode(true);
    }

    // ------------------------------------------------------------------ //
    //  Mode switching
    // ------------------------------------------------------------------ //

    private void switchMode(boolean toLogin) {
        loginMode = toLogin;
        errorLabel.setText("");

        String activeStyle   = "-fx-background-color:" + RED
                + ";-fx-text-fill:white;-fx-background-radius:5;-fx-border-width:0;-fx-cursor:hand;";
        String inactiveStyle = "-fx-background-color:transparent;-fx-text-fill:"
                + DIM + ";-fx-background-radius:5;-fx-border-width:0;-fx-cursor:hand;";

        loginTab.setStyle(toLogin    ? activeStyle : inactiveStyle);
        loginTab.setTextFill(toLogin ? Color.WHITE : Color.web(DIM));
        registerTab.setStyle(!toLogin    ? activeStyle : inactiveStyle);
        registerTab.setTextFill(!toLogin ? Color.WHITE : Color.web(DIM));

        boolean showConfirm = !toLogin;
        confirmLabel.setVisible(showConfirm);
        confirmLabel.setManaged(showConfirm);
        confirmField.setVisible(showConfirm);
        confirmField.setManaged(showConfirm);
        confirmField.clear();

        submitBtn.setText(toLogin ? "LOGIN" : "CREATE ACCOUNT");
        applySubmitStyle(toLogin);

        switchHint.setText(toLogin
                ? "Don't have an account?  Click Register above."
                : "Already have an account?  Click Login above.");
    }

    // ------------------------------------------------------------------ //
    //  Submit handler
    // ------------------------------------------------------------------ //

    private void handleSubmit() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (loginMode) {
            String err = auth.login(username, password);
            if (err != null) {
                showError(err);
            } else {
                showSuccess("Welcome back, " + username + "!");
                if (onSuccess != null) onSuccess.onSuccess(username);
            }
        } else {
            String confirm = confirmField.getText();
            String err = auth.register(username, password, confirm);
            if (err != null) {
                showError(err);
            } else {
                // Auto-login after registration
                auth.login(username, password);
                showSuccess("Account created! Welcome, " + username + "!");
                if (onSuccess != null) onSuccess.onSuccess(username);
            }
        }
    }

    // ------------------------------------------------------------------ //
    //  DB status banner
    // ------------------------------------------------------------------ //

    private Label buildDbBanner() {
        Label lbl = new Label();
        lbl.setFont(Font.font("Monospaced", 11));
        lbl.setPadding(new Insets(6, 12, 6, 12));
        lbl.setMaxWidth(340);
        lbl.setWrapText(true);
        lbl.setTextAlignment(TextAlignment.CENTER);

        if (auth.isDbAvailable()) {
            lbl.setText("🟢  Connected to MySQL database");
            lbl.setTextFill(Color.web(GREEN));
            lbl.setStyle("-fx-background-color: #0a2a1a;"
                    + "-fx-background-radius: 6;");
        } else {
            lbl.setText("🟡  Database unavailable – running in offline mode");
            lbl.setTextFill(Color.web(ORANGE));
            lbl.setStyle("-fx-background-color: #2a1a08;"
                    + "-fx-background-radius: 6;");
        }
        return lbl;
    }

    // ------------------------------------------------------------------ //
    //  Background
    // ------------------------------------------------------------------ //

    private void drawBackground(Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web(BG));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setStroke(Color.web("#0f3460"));
        gc.setLineWidth(0.8);
        for (int x = 0; x <= canvas.getWidth(); x += 30)
            gc.strokeLine(x, 0, x, canvas.getHeight());
        for (int y = 0; y <= canvas.getHeight(); y += 30)
            gc.strokeLine(0, y, canvas.getWidth(), y);
        gc.setFill(Color.web("#0f3460"));
        gc.setFont(Font.font("Monospaced", 11));
        gc.fillText("INFO 6205 · Spring 2026 · Maze Explorer RPG", 20, 28);
    }

    // ------------------------------------------------------------------ //
    //  UI helpers
    // ------------------------------------------------------------------ //

    private TextField inputField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefWidth(340); tf.setPrefHeight(40);
        tf.setFont(Font.font("Monospaced", 13));
        tf.setStyle(fieldCss());
        return tf;
    }

    private PasswordField pwField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setPrefWidth(340); pf.setPrefHeight(40);
        pf.setFont(Font.font("Monospaced", 13));
        pf.setStyle(fieldCss());
        return pf;
    }

    private String fieldCss() {
        return "-fx-background-color:#0f1e3a;"
                + "-fx-text-fill:" + TEXT + ";"
                + "-fx-prompt-text-fill:" + DIM + ";"
                + "-fx-border-color:" + BORDER + ";"
                + "-fx-border-width:1;-fx-border-radius:6;-fx-background-radius:6;";
    }

    private Label fieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Monospaced", FontWeight.BOLD, 11));
        lbl.setTextFill(Color.web(TEXT));
        return lbl;
    }

    private Button tabBtn(String text) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        btn.setPrefWidth(168); btn.setPrefHeight(34);
        return btn;
    }

    private void applySubmitStyle(boolean isLogin) {
        String col  = isLogin ? RED : GREEN;
        String dark = isLogin ? "#c73652" : "#45a87d";
        submitBtn.setStyle("-fx-background-color:" + col
                + ";-fx-background-radius:6;-fx-cursor:hand;");
        submitBtn.setOnMouseEntered(e -> submitBtn.setStyle(
                "-fx-background-color:" + dark + ";-fx-background-radius:6;-fx-cursor:hand;"));
        submitBtn.setOnMouseExited(e -> submitBtn.setStyle(
                "-fx-background-color:" + col + ";-fx-background-radius:6;-fx-cursor:hand;"));
    }

    private void showError(String msg) {
        errorLabel.setTextFill(Color.web(RED));
        errorLabel.setText("⚠  " + msg);
    }

    private void showSuccess(String msg) {
        errorLabel.setTextFill(Color.web(GREEN));
        errorLabel.setText("✓  " + msg);
    }
}