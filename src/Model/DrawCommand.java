package Model;
import java.io.Serializable;

public class DrawCommand implements Serializable {
    public enum Type { LINE, RECT, CIRCLE, ERASER, TOOL, CLEAR, PING, PONG, SYNC, UNDO, REDO }
    public Type type;
    public String tool;
    public double x1, y1, x2, y2;
    public String colorHex;
    public float strokeWidth; // ✅ Changed to float
    public String payload;

    public DrawCommand() {}
    public DrawCommand(Type type) { this.type = type; }
}