package IHM;
import Database.DatabaseManager;
import Model.DrawCommand;
import Network.CommandProtocol;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class WhiteboardFrame extends JFrame {
    private final CanvasPanel canvas;
    private final JLabel statusLabel;
    private boolean isAlive;
    private final boolean isHost;
    private final int sessionId;
    private Timer heartbeatTimer;
    private int missedPongCount = 0;
    private static final int MAX_MISSED_PONGS = 3;
    private java.util.function.Consumer<String> networkSender;

    public WhiteboardFrame(boolean isHost, String username, int sessionId) {
        this.isHost = isHost;
        this.sessionId = sessionId;
        setTitle("Whiteboard | " + (isHost ? "🏠 HOST" : "👤 CLIENT") + " | " + username);
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        canvas = new CanvasPanel();
        statusLabel = new JLabel(" Status: Initializing...");
        statusLabel.setForeground(Color.GRAY);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnClear = new JButton("🗑️ Clear");
        JButton btnRed = new JButton("🔴");
        JButton btnBlue = new JButton("🔵");
        JButton btnBlack = new JButton("⚫");

        btnClear.addActionListener(e -> {
            canvas.clearLocal();
            canvas.emitLocalCommand("CLEAR");
        });
        btnRed.addActionListener(e -> canvas.setToolSettings("#FF0000", 3.0f));
        btnBlue.addActionListener(e -> canvas.setToolSettings("#0000FF", 3.0f));
        btnBlack.addActionListener(e -> canvas.setToolSettings("#000000", 2.0f));

        toolbar.add(btnBlack); toolbar.add(btnRed); toolbar.add(btnBlue);
        toolbar.add(Box.createHorizontalStrut(20)); toolbar.add(btnClear);

        add(toolbar, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // ✅ FIX 1: Start heartbeat timer ONLY for clients
        if (!isHost) {
            heartbeatTimer = new Timer(2000, e -> {
                if (networkSender != null) {
                    networkSender.accept("PING");
                }
            });
            heartbeatTimer.setRepeats(true);
            heartbeatTimer.start();
        }

        // ✅ FIX 2: Clean shutdown
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (heartbeatTimer != null && heartbeatTimer.isRunning()) {
                    heartbeatTimer.stop();
                }
                // Cleanup handled by disposing
            }
        });
    }

    public CanvasPanel getCanvas() { return canvas; }
    public void updateStatus(String text) { SwingUtilities.invokeLater(() -> statusLabel.setText(" " + text)); }
    public void setAlive(boolean alive) {
        this.isAlive = alive;
        resetHeartbeat(); // Reset pong counter on status change
        updateStatus("Status: " + (alive ? "✅ Syncing" : "⏳ Connecting..."));
    }

    public void bindNetworkSender(java.util.function.Consumer<String> sender) {
        this.networkSender = sender;
    }

    private void resetHeartbeat() {
        missedPongCount = 0;
    }

    // ✅ FIX 3: Offload DB write to background thread
    public void bindLocalDraw(java.util.function.Consumer<String> sender) {
        canvas.setOnLocalDraw(rawCmd -> {
            if (isHost) {
                new Thread(() -> {
                    try {
                        DrawCommand cmd = CommandProtocol.deserialize(rawCmd);
                        DatabaseManager.insertDrawCommand(sessionId, cmd);
                    } catch (Exception ignored) {}
                }, "DB-Writer").start();
            }
            sender.accept(rawCmd);
        });
    }

    public void handleNetworkCommand(String rawCmd) {
        if ("PING".equals(rawCmd)) {
            if (networkSender != null) networkSender.accept("PONG");
            return;
        }
        if ("PONG".equals(rawCmd)) {
            setAlive(true);
            return;
        }
        // ✅ NEW: Host disconnects → close client whiteboard
        if ("HOST_CLOSED".equals(rawCmd)) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this,
                        "🚪 Host has closed the session.\nYour whiteboard will now close.",
                        "Session Ended", JOptionPane.INFORMATION_MESSAGE);
                dispose(); // Closes the window cleanly
            });
            return;
        }
        if ("CLEAR".equals(rawCmd)) { canvas.clearLocal(); return; }
        canvas.addRemoteCommand(rawCmd);
    }
}