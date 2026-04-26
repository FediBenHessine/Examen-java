package Model;
import java.io.Serializable;
// Model/DrawCommand.java

public class DrawCommand  implements Serializable {
    public enum Type {
        LINE, PEN, ERASER, DELETE, CLEAR, PING, PONG,
        UNDO, REDO, ERASE_PATH  // ✅ ADD ERASE_PATH
    }

    public Type type;
    public String username;      // ✅ Track who drew this
    public double x1, y1, x2, y2;
    public String colorHex;
    public float strokeWidth;
    public String payload;       // ✅ For PEN path data & ERASE_PATH
    public String tool;          // Optional: tool name

    // Constructors
    public DrawCommand() {}

    public DrawCommand(Type type, String username) {
        this.type = type;
        this.username = username;
    }

    // ✅ Helper: Create erase command
    public static DrawCommand createErasePath(String username, String pathData, float eraserSize) {
        DrawCommand cmd = new DrawCommand();
        cmd.type = Type.ERASE_PATH;
        cmd.username = username;
        cmd.payload = pathData;
        cmd.strokeWidth = eraserSize;
        return cmd;
    }
}