package World;

/**
 * Handles user authentication by delegating to DBManager.
 * Falls back to in-memory mode if the database is unavailable.
 */
public class AuthController {

    private final DBManager db;
    private       String    currentUser = null;
    private       boolean   dbAvailable = false;

    private final Implementation.HashMap<String> memUsers = new Implementation.HashMap<>();

    public AuthController(DBManager db) {
        this.db = db;
        dbAvailable = db.isConnected();

        memUsers.put("guest", "guest");
    }

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

    public String login(String username, String password) {
        String err;
        if (dbAvailable) {
            err = db.login(username, password);
        } else {
            if (!memUsers.containsKeyObj(username)) err = "User not found.";
            else if (!memUsers.get(username).equals(password)) err = "Incorrect password.";
            else err = null;
        }
        if (err == null) currentUser = username;
        return err;
    }

    public void logout() { currentUser = null; }

    public String getCurrentUser() { return currentUser; }

    public boolean isLoggedIn() { return currentUser != null; }

    public boolean isDbAvailable() { return dbAvailable; }
}