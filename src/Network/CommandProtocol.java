package Network;


import Model.DrawCommand;
// Network/CommandProtocol.java
// Network/CommandProtocol.java

public class CommandProtocol {

    public static String serialize(DrawCommand cmd) {
        StringBuilder sb = new StringBuilder();
        sb.append(cmd.type.name()).append("|");
        sb.append(cmd.username != null ? cmd.username : "UNKNOWN").append("|");

        if (cmd.type == DrawCommand.Type.PEN || cmd.type == DrawCommand.Type.ERASE_PATH) {
            String safePayload = cmd.payload != null ? cmd.payload.replace("|", "%7C").replace(";", "%3B") : "";
            sb.append(safePayload).append("|");
            sb.append(cmd.strokeWidth).append("|");
            sb.append(cmd.colorHex != null ? cmd.colorHex : "#000000");
            return sb.toString();
        }

        if (cmd.type == DrawCommand.Type.CLEAR || cmd.type == DrawCommand.Type.PING ||
                cmd.type == DrawCommand.Type.PONG || cmd.type == DrawCommand.Type.UNDO ||
                cmd.type == DrawCommand.Type.REDO) {
            return sb.toString();
        }

        if (cmd.type == DrawCommand.Type.DELETE) {
            sb.append(cmd.x1).append("|").append(cmd.y1);
            return sb.toString();
        }

        // Default: LINE, ERASER, RECT, etc.
        sb.append(cmd.x1).append("|").append(cmd.y1).append("|")
                .append(cmd.x2).append("|").append(cmd.y2).append("|")
                .append(cmd.colorHex != null ? cmd.colorHex : "#000000").append("|")
                .append(cmd.strokeWidth);
        return sb.toString();
    }

    public static DrawCommand deserialize(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        raw = raw.trim();
        String[] parts = raw.split("\\|");

        if (parts.length < 2) {
            System.err.println("❌ Protocol error: Too few parts in: " + raw);
            return null;
        }

        try {
            DrawCommand cmd = new DrawCommand();

            // ✅ Try NEW format first: TYPE|username|...
            DrawCommand.Type type;
            try {
                type = DrawCommand.Type.valueOf(parts[0]);
            } catch (IllegalArgumentException e) {
                System.err.println("⚠️ Invalid command type: " + parts[0]);
                return null;
            }
            cmd.type = type;

            // Check if parts[1] looks like a username (not a number)
            boolean isNewFormat = !isNumeric(parts[1]);

            if (isNewFormat) {
                // ✅ NEW format: TYPE|username|data...
                cmd.username = parts[1];
                deserializeWithData(cmd, parts, 2);
            } else {
                // ✅ LEGACY/DB format: TYPE|x1|y1|x2|y2|color|width|username
                deserializeLegacyFormat(cmd, parts);
            }

            // ✅ Sanitize colorHex to prevent Color.decode crashes
            if (cmd.colorHex == null || !cmd.colorHex.matches("^#[0-9A-Fa-f]{6}$")) {
                cmd.colorHex = "#000000"; // Default fallback
            }

            return cmd;

        } catch (Exception e) {
            System.err.println("❌ Deserialize failed for: " + raw + " | " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ✅ Helper: Check if string is numeric (for format detection)
    private static boolean isNumeric(String str) {
        if (str == null) return false;
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // ✅ Deserialize NEW format (username at position 1)
    private static void deserializeWithData(DrawCommand cmd, String[] parts, int startIndex) {
        if (cmd.type == DrawCommand.Type.PEN || cmd.type == DrawCommand.Type.ERASE_PATH) {
            if (parts.length >= startIndex + 3) {
                cmd.payload = parts[startIndex].replace("%7C", "|").replace("%3B", ";");
                cmd.strokeWidth = Float.parseFloat(parts[startIndex + 1]);
                cmd.colorHex = parts[startIndex + 2];
            }
            return;
        }

        if (cmd.type == DrawCommand.Type.CLEAR || cmd.type == DrawCommand.Type.PING ||
                cmd.type == DrawCommand.Type.PONG || cmd.type == DrawCommand.Type.UNDO ||
                cmd.type == DrawCommand.Type.REDO) {
            return;
        }

        if (cmd.type == DrawCommand.Type.DELETE) {
            if (parts.length >= startIndex + 2) {
                cmd.x1 = Double.parseDouble(parts[startIndex]);
                cmd.y1 = Double.parseDouble(parts[startIndex + 1]);
            }
            return;
        }

        // Default: LINE, ERASER, etc.
        if (parts.length >= startIndex + 6) {
            cmd.x1 = Double.parseDouble(parts[startIndex]);
            cmd.y1 = Double.parseDouble(parts[startIndex + 1]);
            cmd.x2 = Double.parseDouble(parts[startIndex + 2]);
            cmd.y2 = Double.parseDouble(parts[startIndex + 3]);
            cmd.colorHex = parts[startIndex + 4];
            cmd.strokeWidth = Float.parseFloat(parts[startIndex + 5]);
        }
    }

    // ✅ Deserialize LEGACY/DB format (username at end)
    private static void deserializeLegacyFormat(DrawCommand cmd, String[] parts) {
        // DB format: TYPE|x1|y1|x2|y2|color|width|username
        if (parts.length < 7) {
            System.err.println("⚠️ Legacy format too short: " + String.join("|", parts));
            return;
        }

        try {
            cmd.x1 = Double.parseDouble(parts[1]);
            cmd.y1 = Double.parseDouble(parts[2]);
            cmd.x2 = Double.parseDouble(parts[3]);
            cmd.y2 = Double.parseDouble(parts[4]);
            cmd.colorHex = parts[5];
            cmd.strokeWidth = Float.parseFloat(parts[6]);
            // Username is at parts[7] if present
            if (parts.length >= 8) {
                cmd.username = parts[7];
            }
        } catch (NumberFormatException e) {
            System.err.println("⚠️ Legacy parse error: " + e.getMessage());
        }
    }
}