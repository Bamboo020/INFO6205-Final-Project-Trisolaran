package World;

import Implementation.HashMap;

/**
 * Handles user authentication by delegating to DBManager.
 * Falls back to in-memory mode if the database is unavailable.
 */
public class AuthController {

    // ------------------------------------------------------------------ //
    //  Fields
    // ------------------------------------------------------------------ //

    private final DBManager db;
    private       String    currentUser = null;
    private       boolean   dbAvailable = false;

    // Fallback in-memory store (used when DB is offline)
    private final HashMap<String> memUsers = new HashMap<>();

    // ------------------------------------------------------------------ //
    //  Constructor
    // ------------------------------------------------------------------ //

    public AuthController(DBManager db) {
        this.db = db;
        dbAvailable = db.isConnected();

        // Always keep a guest account for quick testing
        memUsers.put("guest", "guest");
    }

    // ------------------------------------------------------------------ //
    //  Public API
    // ------------------------------------------------------------------ //

    /**
     * Registers a new user.
     * Uses MySQL if available, in-memory map as fallback.
     *
     * @return null on success, error message on failure
     */
    public String register(String username, String password, String confirmPassword) {
        if (dbAvailable) {
            return db.register(username, password, confirmPassword);
        }
        // Fallback
        if (username == null || username.isBlank())   return "Username cannot be empty.";
        if (username.length() < 3)                    return "Username must be ≥ 3 chars.";
        if (password == null || password.length() < 4) return "Password must be ≥ 4 chars.";
        if (!password.equals(confirmPassword))         return "Passwords do not match.";
        if (memUsers.containsKeyObj(username))          return "Username already taken.";
        memUsers.put(username, password);
        return null;
    }

    /**
     * Logs in an existing user.
     * Uses MySQL if available, in-memory map as fallback.
     *
     * @return null on success, error message on failure
     */
    public String login(String username, String password) {
        String err;
        if (dbAvailable) {
            err = db.login(username, password);
        } else {
            // Fallback
            if (!memUsers.containsKeyObj(username)) err = "User not found.";
            else if (!memUsers.get(username).equals(password)) err = "Incorrect password.";
            else err = null;
        }
        if (err == null) currentUser = username;
        return err;
    }

    /** Logs out the current user. */
    public void logout() { currentUser = null; }

    /** Returns the logged-in username, or null if not logged in. */
    public String getCurrentUser() { return currentUser; }

    /** Returns true if a user is currently logged in. */
    public boolean isLoggedIn() { return currentUser != null; }

    /** Returns true if the MySQL database is being used. */
    public boolean isDbAvailable() { return dbAvailable; }
}