package Database;


import java.sql.*;

import static Database.Singleton.getConnection;

public class DatabaseManager {



    public static boolean authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password_hash = ? LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                // true only if a matching row exists
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("DB Auth Error: " + e.getMessage());
            return false;
        }
    }

    public  static int createSession(String hostIP, String clientIP) {
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

    public  static void closeSession(int sessionId) {
        String sql = "UPDATE sessions SET status = 'CLOSED', end_time = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Session Close Failed: " + e.getMessage());
        }
    }
    public static boolean userExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("DB userExists Error: " + e.getMessage());
            return false;
        }
    }

    public static void insertDrawCommand(int sessionId, Model.DrawCommand cmd) {
        if (sessionId <= 0 || cmd == null || cmd.type == null) return;

        String sql = "INSERT INTO draw_commands " +
                "(session_id, cmd_type, x1, y1, x2, y2, color_hex, stroke_width, tool_name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            ps.setString(2, cmd.type.name());

            if (cmd.type == Model.DrawCommand.Type.CLEAR || cmd.type == Model.DrawCommand.Type.PING || cmd.type == Model.DrawCommand.Type.PONG) {
                ps.setNull(3, Types.DOUBLE);
                ps.setNull(4, Types.DOUBLE);
                ps.setNull(5, Types.DOUBLE);
                ps.setNull(6, Types.DOUBLE);
                ps.setNull(7, Types.VARCHAR);
                ps.setNull(8, Types.FLOAT);
                ps.setNull(9, Types.VARCHAR);
            } else {
                ps.setDouble(3, cmd.x1);
                ps.setDouble(4, cmd.y1);
                ps.setDouble(5, cmd.x2);
                ps.setDouble(6, cmd.y2);
                ps.setString(7, cmd.colorHex);
                ps.setFloat(8, (float) cmd.strokeWidth);
                ps.setString(9, cmd.tool);
            }

            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("DB insertDrawCommand Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        DatabaseManager  dm = new DatabaseManager();
        System.out.println(dm.authenticate("client_user","client123"));
    }
}