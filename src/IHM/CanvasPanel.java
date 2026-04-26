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
    private List<Point> penPath = null;
    private List<Point> eraserPath = null;  // ✅ Track eraser path

    private Consumer<String> onLocalDraw;
    private Runnable onHistoryChanged;  // ✅ Notify when history changes

    private String currentColor = "#000000";
    private float currentStrokeWidth = 2.0f;
    private String currentUsername = "UNKNOWN";
    private DrawCommand.Type currentTool = DrawCommand.Type.LINE;

    public CanvasPanel() {
        setBackground(Color.WHITE);
        setDoubleBuffered(true);

        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (currentTool == DrawCommand.Type.PEN) {
                    penPath = new ArrayList<>();
                    penPath.add(e.getPoint());
                } else if (currentTool == DrawCommand.Type.ERASER) {
                    eraserPath = new ArrayList<>();
                    eraserPath.add(e.getPoint());
                } else if (currentTool == DrawCommand.Type.DELETE) {
                    deleteStrokeAtPoint(e.getPoint());
                    notifyHistoryChanged();
                    return;
                } else {
                    currentStroke = new DrawCommand();
                    currentStroke.type = currentTool;
                    currentStroke.x1 = e.getX(); currentStroke.y1 = e.getY();
                    currentStroke.colorHex = currentColor;
                    currentStroke.strokeWidth = currentStrokeWidth;
                    currentStroke.username = currentUsername;
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (currentTool == DrawCommand.Type.PEN && penPath != null && !penPath.isEmpty()) {
                    DrawCommand cmd = new DrawCommand();
                    cmd.type = DrawCommand.Type.PEN;
                    cmd.colorHex = currentColor;
                    cmd.strokeWidth = currentStrokeWidth;
                    cmd.username = currentUsername;
                    cmd.payload = serializePath(penPath);

                    saveUndoState();
                    history.add(cmd);
                    repaint();
                    broadcast(cmd);

                    penPath = null;
                    notifyHistoryChanged();

                } else if (currentTool == DrawCommand.Type.ERASER && eraserPath != null && !eraserPath.isEmpty()) {
                    saveUndoState();

                    // Remove intersecting strokes locally
                    List<DrawCommand> toRemove = new ArrayList<>();
                    double eraserRadius = Math.max(currentStrokeWidth * 2, 10.0) / 2;
                    for (DrawCommand hc : history) {
                        if (isStrokeIntersectingPath(hc, eraserPath, eraserRadius)) {
                            toRemove.add(hc);
                        }
                    }
                    history.removeAll(toRemove);
                    repaint();

                    // Broadcast erase action
                    DrawCommand eraseCmd = DrawCommand.createErasePath(
                            currentUsername, serializePath(eraserPath), currentStrokeWidth * 2);
                    broadcast(eraseCmd);

                    eraserPath = null;
                    notifyHistoryChanged();

                } else if (currentStroke != null) {
                    currentStroke.x2 = e.getX(); currentStroke.y2 = e.getY();
                    saveUndoState();
                    history.add(currentStroke);
                    repaint();
                    broadcast(currentStroke);
                    currentStroke = null;
                    notifyHistoryChanged();
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (currentTool == DrawCommand.Type.PEN && penPath != null) {
                    penPath.add(e.getPoint());
                    repaint();
                } else if (currentTool == DrawCommand.Type.ERASER && eraserPath != null) {
                    eraserPath.add(e.getPoint());
                    repaint(); // Optional: draw eraser preview
                } else if (currentStroke != null) {
                    currentStroke.x2 = e.getX(); currentStroke.y2 = e.getY();
                    repaint();
                }
            }
        });
    }

    // ✅ Broadcast command to network
    private void broadcast(DrawCommand cmd) {
        if (onLocalDraw != null) {
            onLocalDraw.accept(CommandProtocol.serialize(cmd));
        }
    }

    // ✅ Notify listeners of history changes
    private void notifyHistoryChanged() {
        if (onHistoryChanged != null) {
            onHistoryChanged.run();
        }
    }

    private String serializePath(List<Point> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            Point p = path.get(i);
            sb.append(p.x).append(",").append(p.y);
            if (i < path.size() - 1) sb.append(";");
        }
        return sb.toString();
    }

    private List<Point> deserializePath(String payload) {
        List<Point> path = new ArrayList<>();
        if (payload == null || payload.isEmpty()) return path;
        String[] points = payload.split(";");
        for (String point : points) {
            String[] coords = point.split(",");
            if (coords.length == 2) {
                path.add(new Point(Integer.parseInt(coords[0]), Integer.parseInt(coords[1])));
            }
        }
        return path;
    }

    private void deleteStrokeAtPoint(Point p) {
        saveUndoState();
        for (int i = history.size() - 1; i >= 0; i--) {
            if (isPointNearStroke(p, history.get(i))) {
                history.remove(i);
                break;
            }
        }
        repaint();

        // Broadcast delete
        DrawCommand delCmd = new DrawCommand(DrawCommand.Type.DELETE, currentUsername);
        delCmd.x1 = p.x; delCmd.y1 = p.y;
        broadcast(delCmd);
    }

    private boolean isPointNearStroke(Point p, DrawCommand cmd) {
        double tolerance = Math.max(cmd.strokeWidth, 5.0) + 5;
        if (cmd.type == DrawCommand.Type.PEN) {
            for (Point pp : deserializePath(cmd.payload)) {
                if (p.distance(pp) <= tolerance) return true;
            }
            return false;
        }
        return distanceFromPointToLine(p, cmd.x1, cmd.y1, cmd.x2, cmd.y2) <= tolerance;
    }

    private boolean isStrokeIntersectingPath(DrawCommand cmd, List<Point> path, double radius) {
        if (cmd.type == DrawCommand.Type.PEN) {
            for (Point sp : deserializePath(cmd.payload)) {
                for (Point ep : path) {
                    if (sp.distance(ep) <= radius + Math.max(cmd.strokeWidth, 2)/2) return true;
                }
            }
            return false;
        }
        for (Point ep : path) {
            if (distanceFromPointToLine(ep, cmd.x1, cmd.y1, cmd.x2, cmd.y2) <= radius + Math.max(cmd.strokeWidth, 2)/2) {
                return true;
            }
        }
        return false;
    }

    private double distanceFromPointToLine(Point p, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        double len2 = dx*dx + dy*dy;
        if (len2 == 0) return p.distance(x1, y1);
        double t = Math.max(0, Math.min(1, ((p.x-x1)*dx + (p.y-y1)*dy) / len2));
        return p.distance(x1 + t*dx, y1 + t*dy);
    }

    // ✅ Public setters
    public void setOnLocalDraw(Consumer<String> listener) { this.onLocalDraw = listener; }
    public void setOnHistoryChanged(Runnable listener) { this.onHistoryChanged = listener; }
    public void setToolSettings(String color, float width) {
        this.currentColor = color; this.currentStrokeWidth = width;
    }
    public void setCurrentTool(DrawCommand.Type tool) { this.currentTool = tool; }
    public void setCurrentUsername(String username) {
        this.currentUsername = username != null ? username : "UNKNOWN";
    }

    // ✅ UNDO/REDO with proper stack management
    private void saveUndoState() {
        if (!history.isEmpty()) {
            undoStack.push(new ArrayList<>(history));
            redoStack.clear();  // Clear redo on new action (standard behavior)
            notifyHistoryChanged();
        }
    }

    public void undo() {
        if (undoStack.isEmpty()) return;

        redoStack.push(new ArrayList<>(history));  // Save current for redo
        history.clear();
        history.addAll(undoStack.pop());  // Restore previous state

        repaint();
        notifyHistoryChanged();

        // Broadcast undo
        broadcast(new DrawCommand(DrawCommand.Type.UNDO, currentUsername));
    }

    public void redo() {
        if (redoStack.isEmpty()) return;

        undoStack.push(new ArrayList<>(history));  // Save current for undo
        history.clear();
        history.addAll(redoStack.pop());  // Restore redone state

        repaint();
        notifyHistoryChanged();

        // Broadcast redo
        broadcast(new DrawCommand(DrawCommand.Type.REDO, currentUsername));
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    // ✅ Process remote commands - SYNC ALL ACTIONS
    public void addRemoteCommand(String rawCmd) {
        if ("CLEAR".equals(rawCmd)) {
            saveUndoState();
            history.clear();
            repaint();
            notifyHistoryChanged();
            return;
        }
        if ("PING".equals(rawCmd) || "PONG".equals(rawCmd)) return;

        try {
            DrawCommand cmd = CommandProtocol.deserialize(rawCmd);
            if (cmd == null) return;

            // ✅ SYNC: Process remote UNDO
            if (cmd.type == DrawCommand.Type.UNDO) {
                if (!undoStack.isEmpty()) {
                    redoStack.push(new ArrayList<>(history));
                    history.clear();
                    history.addAll(undoStack.pop());
                    repaint();
                    notifyHistoryChanged();
                }
                return;
            }

            // ✅ SYNC: Process remote REDO
            if (cmd.type == DrawCommand.Type.REDO) {
                if (!redoStack.isEmpty()) {
                    undoStack.push(new ArrayList<>(history));
                    history.clear();
                    history.addAll(redoStack.pop());
                    repaint();
                    notifyHistoryChanged();
                }
                return;
            }

            // ✅ SYNC: Process remote DELETE
            if (cmd.type == DrawCommand.Type.DELETE) {
                saveUndoState();
                for (int i = history.size() - 1; i >= 0; i--) {
                    if (isPointNearStroke(new Point((int)cmd.x1, (int)cmd.y1), history.get(i))) {
                        history.remove(i);
                        break;
                    }
                }
                repaint();
                notifyHistoryChanged();
                return;
            }

            // ✅ SYNC: Process remote ERASE_PATH
            if (cmd.type == DrawCommand.Type.ERASE_PATH) {
                saveUndoState();
                List<Point> erasePath = deserializePath(cmd.payload);
                double radius = Math.max(cmd.strokeWidth, 10.0) / 2;
                List<DrawCommand> toRemove = new ArrayList<>();
                for (DrawCommand hc : history) {
                    if (isStrokeIntersectingPath(hc, erasePath, radius)) {
                        toRemove.add(hc);
                    }
                }
                history.removeAll(toRemove);
                repaint();
                notifyHistoryChanged();
                return;
            }

            // ✅ Normal drawing commands
            history.add(cmd);
            repaint();
            notifyHistoryChanged();

        } catch (Exception e) {
            System.err.println("❌ Invalid remote cmd: " + rawCmd);
        }
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (DrawCommand c : history) draw(g2, c);
        if (currentStroke != null) draw(g2, currentStroke);

        // Draw pen preview
        if (penPath != null && !penPath.isEmpty() && currentTool == DrawCommand.Type.PEN) {
            g2.setColor(Color.decode(currentColor));
            g2.setStroke(new BasicStroke(currentStrokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 1; i < penPath.size(); i++) {
                Point p1 = penPath.get(i-1), p2 = penPath.get(i);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
    }

    private void draw(Graphics2D g2, DrawCommand c) {
        if (c.type == DrawCommand.Type.LINE || c.type == DrawCommand.Type.ERASER) {
            g2.setColor(c.type == DrawCommand.Type.ERASER ? Color.WHITE : Color.decode(c.colorHex));
            g2.setStroke(new BasicStroke(c.strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int)c.x1, (int)c.y1, (int)c.x2, (int)c.y2);
        } else if (c.type == DrawCommand.Type.PEN) {
            g2.setColor(Color.decode(c.colorHex));
            g2.setStroke(new BasicStroke(c.strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            List<Point> path = deserializePath(c.payload);
            for (int i = 1; i < path.size(); i++) {
                Point p1 = path.get(i-1), p2 = path.get(i);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
        // ERASE_PATH doesn't draw - it removes strokes
    }

    public void clearLocal() {
        saveUndoState();
        history.clear();
        undoStack.clear();
        redoStack.clear();
        repaint();
        notifyHistoryChanged();
    }
    public String getCurrentColor() {
        return currentColor;
    }

    // ✅ Getter for current stroke width (used by size selector)
    public float getCurrentStrokeWidth() {
        return currentStrokeWidth;
    }

    // ✅ Emit a raw command directly to network (used for CLEAR)
    public void emitLocalCommand(String rawCmd) {
        if (onLocalDraw != null) {
            onLocalDraw.accept(rawCmd);
        }
    }

    // ✅ Getters for tool state (optional but useful)
    public DrawCommand.Type getCurrentTool() {
        return currentTool;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }
}