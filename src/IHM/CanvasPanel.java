package IHM;



import Network.CommandProtocol;
import Model.DrawCommand;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CanvasPanel extends JPanel {
    private final List<DrawCommand> history = new ArrayList<>();
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
                    history.add(currentStroke);
                    repaint();
                    if (onLocalDraw != null) onLocalDraw.accept(currentStroke.type.name() + "|" +
                            currentStroke.x1 + "|" + currentStroke.y1 + "|" + currentStroke.x2 + "|" + currentStroke.y2 + "|" +
                            currentStroke.colorHex + "|" + currentStroke.strokeWidth);
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

    // Called by network thread via SwingUtilities.invokeLater
    public void addRemoteCommand(String rawCmd) {
        if ("CLEAR".equals(rawCmd)) { history.clear(); repaint(); return; }
        if ("PING".equals(rawCmd) || "PONG".equals(rawCmd)) return; // Handled elsewhere

        try {
            DrawCommand cmd = CommandProtocol.deserialize(rawCmd);
            history.add(cmd);
            repaint();
        } catch (Exception e) { System.err.println("Invalid remote command: " + rawCmd); }
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

    public void clearLocal() { history.clear(); repaint(); }
}