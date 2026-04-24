package Model;
public class User {
    public User() {
    }

    public String username;
    public String passwordHash; // Store hashed in production
    public String role; // "HOST" or "CLIENT"

    public User(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }
}