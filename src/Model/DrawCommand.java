package Model;

import java.io.Serializable;

public class DrawCommand implements Serializable {
    public DrawCommand() {

    }

    public enum Type { LINE, RECT, CIRCLE, ERASER, TOOL, CLEAR, PING, PONG, SYNC }

    public Type type;
    public String tool;      // e.g., "PEN", "ERASER"
    public double x1, y1, x2, y2;
    public String colorHex;  // "#FF0000"
    public double strokeWidth;
    public String payload;   // For future extensions (e.g., usernames)

    public DrawCommand(Type type) { this.type = type; }
}
