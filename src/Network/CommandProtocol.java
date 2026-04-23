package Network;


import Model.DrawCommand;

public class CommandProtocol {
    public static String serialize(DrawCommand cmd) {
        if (cmd.type == DrawCommand.Type.PING) return "PING";
        if (cmd.type == DrawCommand.Type.PONG) return "PONG";
        if (cmd.type == DrawCommand.Type.CLEAR) return "CLEAR";
        return String.join("|", cmd.type.name(),
                String.valueOf(cmd.x1), String.valueOf(cmd.y1),
                String.valueOf(cmd.x2), String.valueOf(cmd.y2),
                cmd.colorHex, String.valueOf(cmd.strokeWidth));
    }

    public static DrawCommand deserialize(String line) {
        DrawCommand cmd = new DrawCommand();
        if (line.equals("PING")) { cmd.type = DrawCommand.Type.PING; return cmd; }
        if (line.equals("PONG")) { cmd.type = DrawCommand.Type.PONG; return cmd; }
        if (line.equals("CLEAR")) { cmd.type = DrawCommand.Type.CLEAR; return cmd; }

        String[] p = line.split("\\|");
        cmd.type = DrawCommand.Type.valueOf(p[0]);
        cmd.x1 = Double.parseDouble(p[1]); cmd.y1 = Double.parseDouble(p[2]);
        cmd.x2 = Double.parseDouble(p[3]); cmd.y2 = Double.parseDouble(p[4]);
        cmd.colorHex = p[5]; cmd.strokeWidth = Float.parseFloat(p[6]);
        return cmd;
    }
}