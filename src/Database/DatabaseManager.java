package Database;


import java.sql.*;

public class DatabaseManager {


    Singleton conn = Singleton.getConnection();

    public boolean authenticate(String username, String password) {
        String sql = "SELECT 1 FROM users WHERE username = ? AND password_hash = ? LIMIT 1";

        try (
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("DB Auth Error: " + e.getMessage());
            return false;
        }
    }

    public static int createSession(String hostIP, String clientIP) {
        String sql = "INSERT INTO sessions (host_ip, client_ip) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, hostIP);
            ps.setString(2, clientIP != null ? clientIP : "UNKNOWN");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (SQLException e) {
            System.err.println("Session Creation Failed: " + e.getMessage());
            return -1;
        }
    }

    public static void closeSession(int sessionId) {
        String sql = "UPDATE sessions SET status = 'CLOSED', end_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Session Close Failed: " + e.getMessage());
        }
    }
}