package World;

import Implementation.ArrayList;

import java.sql.*;

/**
 * Manages all MySQL database operations.
 *
 * Covers:
 *   - Connection management
 *   - User registration / login / logout
 *   - Score persistence (save + load into MaxHeap / AVLTree)
 *
 * Change DB_URL / DB_USER / DB_PASS to match your MySQL setup.
 */
public class DBManager {

    // ------------------------------------------------------------------ //
    //  Connection config  ← change these to match your MySQL setup
    // ------------------------------------------------------------------ //

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/maze_explorer"
            + "?useSSL=false&serverTimezone=UTC"
            + "&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Bxh12230102.";   // ← change this

    // ------------------------------------------------------------------ //
    //  Singleton connection
    // ------------------------------------------------------------------ //

    private Connection connection = null;

    // ------------------------------------------------------------------ //
    //  Connection
    // ------------------------------------------------------------------ //

    /**
     * Opens the database connection.
     * Call once at application startup.
     *
     * @return null on success, or error message on failure
     */
    public String connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            initTables();
            return null;   // success
        } catch (ClassNotFoundException e) {
            return "MySQL driver not found. Add mysql-connector-j JAR to Libraries.";
        } catch (SQLException e) {
            return "Database connection failed: " + e.getMessage();
        }
    }

    /**
     * Closes the database connection.
     * Call on application shutdown.
     */
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }

    /** Returns true if the database is connected and ready. */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ //
    //  Table initialisation
    // ------------------------------------------------------------------ //

    /**
     * Creates the users and scores tables if they don't already exist.
     * Safe to call every startup.
     */
    private void initTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS users ("
                            + "  id         INT AUTO_INCREMENT PRIMARY KEY,"
                            + "  username   VARCHAR(50)  NOT NULL UNIQUE,"
                            + "  password   VARCHAR(255) NOT NULL,"
                            + "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP"
                            + ")"
            );

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS scores ("
                            + "  id         INT AUTO_INCREMENT PRIMARY KEY,"
                            + "  username   VARCHAR(50) NOT NULL,"
                            + "  score      INT         NOT NULL,"
                            + "  level_id   INT         DEFAULT 0,"
                            + "  played_at  DATETIME    DEFAULT CURRENT_TIMESTAMP"
                            + ")"
            );
        }
    }

    // ------------------------------------------------------------------ //
    //  User registration
    // ------------------------------------------------------------------ //

    /**
     * Registers a new user with the given username and password.
     *
     * @return null on success, or an error message on failure
     */
    public String register(String username, String password, String confirmPassword) {
        // Input validation
        if (username == null || username.isBlank())
            return "Username cannot be empty.";
        if (username.length() < 3)
            return "Username must be at least 3 characters.";
        if (password == null || password.isBlank())
            return "Password cannot be empty.";
        if (password.length() < 4)
            return "Password must be at least 4 characters.";
        if (!password.equals(confirmPassword))
            return "Passwords do not match.";

        if (!isConnected()) return "Database not connected.";

        // Check if username already exists
        try (PreparedStatement check = connection.prepareStatement(
                "SELECT id FROM users WHERE username = ?")) {
            check.setString(1, username);
            ResultSet rs = check.executeQuery();
            if (rs.next()) return "Username '" + username + "' is already taken.";
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }

        // Insert new user
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?, ?)")) {
            insert.setString(1, username);
            insert.setString(2, password);   // plain text (sufficient for course project)
            insert.executeUpdate();
            return null;   // success
        } catch (SQLException e) {
            return "Registration failed: " + e.getMessage();
        }
    }

    // ------------------------------------------------------------------ //
    //  User login
    // ------------------------------------------------------------------ //

    /**
     * Validates login credentials against the database.
     *
     * @return null on success, or an error message on failure
     */
    public String login(String username, String password) {
        if (username == null || username.isBlank())
            return "Please enter your username.";
        if (password == null || password.isBlank())
            return "Please enter your password.";

        if (!isConnected()) return "Database not connected.";

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT password FROM users WHERE username = ?")) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                return "User '" + username + "' not found.";
            if (!rs.getString("password").equals(password))
                return "Incorrect password.";

            return null;   // success
        } catch (SQLException e) {
            return "Login failed: " + e.getMessage();
        }
    }

    // ------------------------------------------------------------------ //
    //  Score persistence
    // ------------------------------------------------------------------ //

    /**
     * Saves a completed path score to the scores table.
     *
     * @param username  the logged-in player
     * @param score     final accumulated path score
     * @param levelId   last node's level id
     */
    public void saveScore(String username, int score, int levelId) {
        if (!isConnected()) return;
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO scores (username, score, level_id) VALUES (?, ?, ?)")) {
            stmt.setString(1, username);
            stmt.setInt(2, score);
            stmt.setInt(3, levelId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to save score: " + e.getMessage());
        }
    }

    /**
     * Loads all historical scores from the database in descending order.
     * Called at startup to restore MaxHeap and AVLTree state.
     */
    public ArrayList<ScoreRecord> loadAllScores() {
        ArrayList<ScoreRecord> records = new ArrayList<>();
        if (!isConnected()) return records;

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT username, score, level_id, played_at "
                        + "FROM scores ORDER BY score DESC")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                records.add(new ScoreRecord(
                        rs.getString("username"),
                        rs.getInt("score"),
                        rs.getInt("level_id"),
                        rs.getString("played_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Failed to load scores: " + e.getMessage());
        }
        return records;
    }

    /**
     * Loads the top-N scores from the database for the leaderboard display.
     */
    public ArrayList<ScoreRecord> loadTopScores(int limit) {
        ArrayList<ScoreRecord> records = new ArrayList<>();
        if (!isConnected()) return records;

        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT username, score, level_id, played_at "
                        + "FROM scores ORDER BY score DESC LIMIT ?")) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                records.add(new ScoreRecord(
                        rs.getString("username"),
                        rs.getInt("score"),
                        rs.getInt("level_id"),
                        rs.getString("played_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Failed to load top scores: " + e.getMessage());
        }
        return records;
    }

    // ------------------------------------------------------------------ //
    //  Inner data class
    // ------------------------------------------------------------------ //

    /** Lightweight container for a row from the scores table. */
    public static class ScoreRecord {
        public final String username;
        public final int    score;
        public final int    levelId;
        public final String playedAt;

        public ScoreRecord(String username, int score, int levelId, String playedAt) {
            this.username = username;
            this.score    = score;
            this.levelId  = levelId;
            this.playedAt = playedAt;
        }

        @Override
        public String toString() {
            return username + "  " + score + " pts  Lv" + levelId + "  " + playedAt;
        }
    }
}