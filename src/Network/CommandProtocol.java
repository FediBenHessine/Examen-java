package Network;


import Model.DrawCommand;
// Network/CommandProtocol.java

public class CommandProtocol {

    public static String serialize(DrawCommand cmd) {
        StringBuilder sb = new StringBuilder();
        sb.append(cmd.type.name()).append("|");
        sb.append(cmd.username != null ? cmd.username : "UNKNOWN").append("|");

        // ✅ Handle commands with payload (PEN, ERASE_PATH)
        if (cmd.type == DrawCommand.Type.PEN || cmd.type == DrawCommand.Type.ERASE_PATH) {
            sb.append(cmd.payload != null ? cmd.payload : "").append("|");
            sb.append(cmd.strokeWidth).append("|");
            sb.append(cmd.colorHex != null ? cmd.colorHex : "#000000");
            return sb.toString();
        }

        // ✅ Handle CLEAR, PING, PONG, UNDO, REDO (no coordinates)
        if (cmd.type == DrawCommand.Type.CLEAR || cmd.type == DrawCommand.Type.PING ||
                cmd.type == DrawCommand.Type.PONG || cmd.type == DrawCommand.Type.UNDO ||
                cmd.type == DrawCommand.Type.REDO) {
            return sb.toString(); // Just "TYPE|username"
        }

        // ✅ Handle DELETE (has coordinates)
        if (cmd.type == DrawCommand.Type.DELETE) {
            sb.append(cmd.x1).append("|").append(cmd.y1);
            return sb.toString();
        }

        // ✅ Default: LINE, ERASER, RECT, etc.
        sb.append(cmd.x1).append("|").append(cmd.y1).append("|")
                .append(cmd.x2).append("|").append(cmd.y2).append("|")
                .append(cmd.colorHex != null ? cmd.colorHex : "#000000").append("|")
                .append(cmd.strokeWidth);
        return sb.toString();
    }

    public static DrawCommand deserialize(String raw) {
        DrawCommand cmd = new DrawCommand();
        String[] parts = raw.split("\\|");
        if (parts.length < 2) return null;

        try {
            cmd.type = DrawCommand.Type.valueOf(parts[0]);
            cmd.username = parts[1];

            // ✅ Handle commands with payload
            if (cmd.type == DrawCommand.Type.PEN || cmd.type == DrawCommand.Type.ERASE_PATH) {
                if (parts.length >= 4) {
                    cmd.payload = parts[2];
                    cmd.strokeWidth = Float.parseFloat(parts[3]);
                    if (parts.length >= 5) cmd.colorHex = parts[4];
                }
                return cmd;
            }

            // ✅ Handle simple commands
            if (cmd.type == DrawCommand.Type.CLEAR || cmd.type == DrawCommand.Type.PING ||
                    cmd.type == DrawCommand.Type.PONG || cmd.type == DrawCommand.Type.UNDO ||
                    cmd.type == DrawCommand.Type.REDO) {
                return cmd;
            }

            // ✅ Handle DELETE
            if (cmd.type == DrawCommand.Type.DELETE) {
                if (parts.length >= 4) {
                    cmd.x1 = Double.parseDouble(parts[2]);
                    cmd.y1 = Double.parseDouble(parts[3]);
                }
                return cmd;
            }

            // ✅ Default: LINE, ERASER, etc.
            if (parts.length >= 8) {
                cmd.x1 = Double.parseDouble(parts[2]);
                cmd.y1 = Double.parseDouble(parts[3]);
                cmd.x2 = Double.parseDouble(parts[4]);
                cmd.y2 = Double.parseDouble(parts[5]);
                cmd.colorHex = parts[6];
                cmd.strokeWidth = Float.parseFloat(parts[7]);
            }
            return cmd;
        } catch (Exception e) {
            System.err.println("❌ Deserialize error: " + e.getMessage());
            return null;
        }
    }
}