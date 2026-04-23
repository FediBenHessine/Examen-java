package auth;


import Database.DatabaseManager;

public class AuthService {
    public static AuthResult login(String username, String password) {
        if (username == null || password == null || username.trim().isEmpty()) {
            return new AuthResult(false, "Credentials cannot be empty");
        }
        if (DatabaseManager.authenticate(username.trim(), password.trim())) {
            return new AuthResult(true, "Login successful");
        }
        return new AuthResult(false, "Invalid username or password");
    }

    public record AuthResult(boolean success, String message) {}
}