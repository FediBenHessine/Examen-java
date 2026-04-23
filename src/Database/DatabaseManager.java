package Database;


import java.sql.*;

import static Database.Singleton.getConnection;

public class DatabaseManager {


    Connection conn = getConnection();

    public  boolean authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ? LIMIT 1";

        try (
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData rsm = rs.getMetaData();

                while (rs.next()  ){
                    for(int i = 1; i<=(rsm.getColumnCount());i++){
                    System.out.println(rs.getObject(i));
                    i++;}
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("DB Auth Error: " + e.getMessage());
            return false;
        }
    }

    public  int createSession(String hostIP, String clientIP) {
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

    public  void closeSession(int sessionId) {
        String sql = "UPDATE sessions SET status = 'CLOSED', end_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Session Close Failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        DatabaseManager  dm = new DatabaseManager();
        System.out.println(dm.authenticate("client_user","client123"));
    }
}