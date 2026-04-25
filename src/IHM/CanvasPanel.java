package IHM;
import Model.DrawCommand;
import Network.CommandProtocol;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

public class CanvasPanel extends JPanel {
    private final List<DrawCommand> history = new ArrayList<>();
    private final Stack<List<DrawCommand>> undoStack = new Stack<>();
    private final Stack<List<DrawCommand>> redoStack = new Stack<>();
    private DrawCommand currentStroke = null;
    private Consumer<String> onLocalDraw;
    private String currentColor = "#000000";
    private float currentStrokeWidth = 2.0f;

    public CanvasPanel() {
        setBackground(Color.WHITE);
        setDoubleBuffered(true);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                currentStroke = new DrawCommand();
                currentStroke.type = DrawCommand.Type.LINE;
                currentStroke.x1 = e.getX(); currentStroke.y1 = e.getY();
                currentStroke.colorHex = currentColor; currentStroke.strokeWidth = currentStrokeWidth;
            }
            public void mouseReleased(MouseEvent e) {
                if (currentStroke != null) {
                    currentStroke.x2 = e.getX(); currentStroke.y2 = e.getY();
                    saveUndoState(); // Save state before adding new stroke
                    history.add(currentStroke);
                    repaint();
                    if (onLocalDraw != null) {
                        String cmd = CommandProtocol.serialize(currentStroke);
                        onLocalDraw.accept(cmd);
                    }
                    currentStroke = null;
                }
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (currentStroke != null) {
                    currentStroke.x2 = e.getX(); currentStroke.y2 = e.getY();
                    repaint();
                }
            }
        });
    }

    public void setOnLocalDraw(Consumer<String> listener) { this.onLocalDraw = listener; }
    public void setToolSettings(String color, float width) { this.currentColor = color; this.currentStrokeWidth = width; }

    // ✅ FIX: Complete method
    public void emitLocalCommand(String rawCmd) {
        if (onLocalDraw != null) onLocalDraw.accept(rawCmd);
    }

    // ✅ UNDO/REDO: Save state before drawing
    private void saveUndoState() {
        undoStack.push(new ArrayList<>(history));
        redoStack.clear(); // Clear redo stack after new action
    }

    // ✅ UNDO: Pop from undo stack and push to redo
    public void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(new ArrayList<>(history));
            history.clear();
            history.addAll(undoStack.pop());
            repaint();
            // Notify network about undo
            if (onLocalDraw != null) {
                DrawCommand undoCmd = new DrawCommand(DrawCommand.Type.UNDO);
                onLocalDraw.accept(CommandProtocol.serialize(undoCmd));
            }
        }
    }

    // ✅ REDO: Pop from redo stack and push to undo  
    public void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(new ArrayList<>(history));
            history.clear();
            history.addAll(redoStack.pop());
            repaint();
            // Notify network about redo
            if (onLocalDraw != null) {
                DrawCommand redoCmd = new DrawCommand(DrawCommand.Type.REDO);
                onLocalDraw.accept(CommandProtocol.serialize(redoCmd));
            }
        }
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    // ✅ Getters for toolbar
    public String getCurrentColor() { return currentColor; }
    public float getCurrentStrokeWidth() { return currentStrokeWidth; }

    public void addRemoteCommand(String rawCmd) {
        if ("CLEAR".equals(rawCmd)) { history.clear(); undoStack.clear(); redoStack.clear(); repaint(); return; }
        if ("PING".equals(rawCmd) || "PONG".equals(rawCmd)) return;
        try {
            DrawCommand cmd = CommandProtocol.deserialize(rawCmd);
            if (cmd.type == DrawCommand.Type.UNDO) {
                undo();
                return;
            }
            if (cmd.type == DrawCommand.Type.REDO) {
                redo();
                return;
            }
            history.add(cmd); 
            repaint();
        } catch (Exception e) { System.err.println("Invalid cmd: " + rawCmd); }
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (DrawCommand c : history) draw(g2, c);
        if (currentStroke != null) draw(g2, currentStroke);
    }

    private void draw(Graphics2D g2, DrawCommand c) {
        g2.setColor(Color.decode(c.colorHex));
        g2.setStroke(new BasicStroke((float) c.strokeWidth));
        if (c.type == DrawCommand.Type.LINE)
            g2.drawLine((int)c.x1, (int)c.y1, (int)c.x2, (int)c.y2);
    }

    public void clearLocal() { history.clear(); undoStack.clear(); redoStack.clear(); repaint(); }
}
