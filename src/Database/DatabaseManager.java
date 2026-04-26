package Database;


import Model.RoomInfo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
                "(session_id, cmd_type, x1, y1, x2, y2, color_hex, stroke_width, tool_name, username) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
            // ✅ NEW: Insert username
            ps.setString(10, cmd.username != null ? cmd.username : "UNKNOWN");

            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("DB insertDrawCommand Error: " + e.getMessage());
        }
    }
    // ✅ SIGNUP: Register new user
    public static boolean signUp(String username, String password, String role) {
        // Input validation
        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            System.err.println("❌ Sign up failed: Username and password cannot be empty");
            return false;
        }

        if (username.length() < 3 || password.length() < 6) {
            System.err.println("❌ Sign up failed: Username must be >= 3 chars, password >= 6 chars");
            return false;
        }

        // Check for existing user
        if (userExists(username)) {
            System.err.println("❌ Sign up failed: Username '" + username + "' already exists");
            return false;
        }

        // ⚠️ For demo: store plaintext. Use BCrypt in production!
        String sql = "INSERT INTO users (username, password_hash, role, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, password.trim()); // TODO: hash with BCrypt
            ps.setString(3, role != null ? role : "CLIENT");
            boolean success = ps.executeUpdate() > 0;
            if (success) {
                System.out.println("✅ User '" + username + "' registered successfully");
            }
            return success;
        } catch (SQLException e) {
            System.err.println("❌ Signup failed for user '" + username + "': " + e.getMessage());
            return false;
        }
    }

    // ✅ ROOMS: Create new room entry
    public static int createRoom(String hostUsername, String roomName, Model.RoomType roomType,
                                 String password, String hostIP, int socketPort) {
        String sql = "INSERT INTO rooms (host_username, room_name, room_type_id, password_hash, host_ip, socket_port) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, hostUsername);
            ps.setString(2, roomName);
            ps.setInt(3, roomType.ordinal() + 1); // 1=PUBLIC, 2=PUBLIC_PASSWORD, 3=PRIVATE
            ps.setString(4, roomType == Model.RoomType.PUBLIC ? null : password);
            ps.setString(5, hostIP);
            ps.setInt(6, socketPort);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        } catch (SQLException e) {
            System.err.println("Room creation failed: " + e.getMessage());
            return -1;
        }
    }

    // ✅ ROOMS: Validate join password
    public static boolean validateRoomPassword(int roomId, String password) {
        String sql = "SELECT password_hash FROM rooms WHERE id = ? AND is_active = TRUE";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String storedHash = rs.getString("password_hash");
                return storedHash == null || storedHash.equals(password); // Plaintext for demo
            }
        } catch (SQLException e) {
            System.err.println("Password validation failed: " + e.getMessage());
            return false;
        }
    }

    // ✅ ROOMS: Get active public rooms (for discovery list)
    public static List<RoomInfo> getPublicRooms() {
        List<Model.RoomInfo> rooms = new ArrayList<>();
        String sql = "SELECT r.*, u.username as host_username, rt.name as room_type " +
                "FROM rooms r JOIN users u ON r.host_username = u.username " +
                "JOIN room_types rt ON r.room_type_id = rt.id " +
                "WHERE r.is_active = TRUE AND rt.name != 'PRIVATE'";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Model.RoomType type = Model.RoomType.valueOf(rs.getString("room_type"));
                rooms.add(new Model.RoomInfo(
                        rs.getString("room_name"),
                        rs.getString("host_username"),
                        rs.getString("host_ip"),
                        rs.getInt("socket_port"),
                        type,
                        type != Model.RoomType.PUBLIC && rs.getString("password_hash") != null
                ));
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch public rooms: " + e.getMessage());
        }
        return rooms;
    }
    public static String getUserRole(String username) {
        String sql = "SELECT role FROM users WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("role") : "CLIENT";
            }
        } catch (SQLException e) {
            System.err.println("Failed to fetch user role: " + e.getMessage());
            return "CLIENT";
        }
    }
    public static List<String> getDrawCommandsForSession(int sessionId) {
        List<String> commands = new ArrayList<>();
        // Fetch only drawing commands, ordered by time, excluding heartbeat/control packets
        String sql = "SELECT cmd_type, x1, y1, x2, y2, color_hex, stroke_width, username " +
                "FROM draw_commands WHERE session_id = ? AND cmd_type NOT IN ('PING','PONG','SYNC') " +
                "ORDER BY executed_at ASC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // ✅ NEW: Include username in synchronized commands
                    String username = rs.getString("username");
                    if (username == null) username = "UNKNOWN";
                    String cmd = rs.getString("cmd_type") + "|" +
                            rs.getDouble("x1") + "|" + rs.getDouble("y1") + "|" +
                            rs.getDouble("x2") + "|" + rs.getDouble("y2") + "|" +
                            rs.getString("color_hex") + "|" + rs.getFloat("stroke_width") + "|" + username;
                    commands.add(cmd);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load drawing history: " + e.getMessage());
        }
        return commands;
    }
    /**
     * Validates room password against active sessions in MySQL.
     * Returns true if password matches, or if room is public.
     */
    public static boolean validateRoomPassword(String hostIP, int socketPort, String password) {
        String sql = "SELECT password_hash FROM rooms WHERE host_ip = ? AND socket_port = ? AND is_active = TRUE ORDER BY id DESC  LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hostIP);
            ps.setInt(2, socketPort);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString("password_hash");
                    // If stored is null, it's a public room (bypass validation)
                    if (stored == null) return true;
                    // Plaintext comparison for demo (use BCrypt in production)
                    return stored.equals(password);
                }
                return false; // Room not found or inactive
            }
        } catch (SQLException e) {
            System.err.println("❌ DB Password Validation Error: " + e.getMessage());
            return false;
        }
    }
    /**
     * Get room info by host IP (for direct join)
     * Returns null if no active room found at that IP
     */
    public static RoomInfo getRoomByIP(String hostIP) {
        String sql = "SELECT r.*, u.username as host_username, rt.name as room_type " +
                "FROM rooms r JOIN users u ON r.host_username = u.username " +
                "JOIN room_types rt ON r.room_type_id = rt.id " +
                "WHERE r.host_ip = ? AND r.is_active = TRUE ORDER BY r.id DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hostIP);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Model.RoomType type = Model.RoomType.valueOf(rs.getString("room_type"));
                    return new Model.RoomInfo(
                            rs.getString("room_name"),
                            rs.getString("host_username"),
                            rs.getString("host_ip"),
                            rs.getInt("socket_port"),
                            type,
                            type != Model.RoomType.PUBLIC // Requires password if not public
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get room by IP: " + e.getMessage());
        }
        return null;
    }
    public static void main(String[] args) {
        // Test authentication
        System.out.println("Testing authentication...");
        System.out.println("client_user login: " + authenticate("client_user", "client123"));

        // Test sign up
        System.out.println("Testing sign up...");
        boolean signup1 = signUp("testuser1", "password123", "USER");
        System.out.println("Sign up testuser1: " + signup1);

        boolean signup2 = signUp("testuser1", "password123", "USER"); // Should fail
        System.out.println("Sign up duplicate testuser1: " + signup2);

        // Test validation
        boolean signup3 = signUp("", "password123", "USER"); // Should fail
        System.out.println("Sign up empty username: " + signup3);

        boolean signup4 = signUp("ab", "password123", "USER"); // Should fail
        System.out.println("Sign up short username: " + signup4);

        boolean signup5 = signUp("testuser2", "12345", "USER"); // Should fail
        System.out.println("Sign up short password: " + signup5);
    }
}
