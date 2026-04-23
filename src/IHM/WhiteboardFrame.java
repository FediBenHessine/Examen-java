package IHM;

import javax.swing.*;
import java.awt.*;

public class WhiteboardFrame extends JFrame {
    private CanvasPanel canvas;
    private JLabel statusLabel;
    private boolean isAlive = false;
    private boolean isHost;


    public WhiteboardFrame(String host) {
    }

    public WhiteboardFrame(boolean isHost, String username, int sessionId) {
        this.isHost = isHost;
        setTitle("Whiteboard | " + (isHost ? "HOST" : "CLIENT") + " | " + username);
        setSize(1000, 700); setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); setLocationRelativeTo(null);

        canvas = new CanvasPanel();
        statusLabel = new JLabel(" Status: Initializing...");
        statusLabel.setForeground(Color.GRAY);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnClear = new JButton("Clear Canvas");
        JButton btnRed = new JButton("🔴 Red");
        JButton btnBlue = new JButton("🔵 Blue");
        JButton btnBlack = new JButton("⚫ Black");

        btnClear.addActionListener(e -> {
            canvas.clearLocal();
            canvas.addRemoteCommand("CLEAR");
        });
        btnRed.addActionListener(e -> canvas.setToolSettings("#FF0000", 3.0f));
        btnBlue.addActionListener(e -> canvas.setToolSettings("#0000FF", 3.0f));
        btnBlack.addActionListener(e -> canvas.setToolSettings("#000000", 2.0f));

        toolbar.add(btnBlack); toolbar.add(btnRed); toolbar.add(btnBlue);
        toolbar.add(Box.createHorizontalStrut(20)); toolbar.add(btnClear);

        // Layout
        add(toolbar, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Heartbeat Setup
        Timer heartbeatTimer = new Timer(2000, e -> {
            if (!isAlive) updateStatus("Status: ⚠️ Connection Lost (Check Ethernet)");
        });
        heartbeatTimer.setRepeats(true);
    }



    public CanvasPanel getCanvas() { return canvas; }
    public void updateStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(" " + text));
    }
    public void setAlive(boolean alive) {
        this.isAlive = alive;
        updateStatus("Status: " + (alive ? "✅ Connected & Syncing" : "⏳ Waiting..."));
    }

    // Hook for local drawing
    public void bindLocalDraw(java.util.function.Consumer<String> sender) {
        canvas.setOnLocalDraw(sender);
    }

    // Handle incoming network commands
    public void handleNetworkCommand(String rawCmd) {
        if ("PING".equals(rawCmd)) {
            // Auto-reply handled by socket layer if we add callback, or just ignore on host
            return;
        }
        if ("PONG".equals(rawCmd)) {
            setAlive(true);
            return;
        }
        if ("CLEAR".equals(rawCmd)) {
            canvas.clearLocal();
            return;
        }
        canvas.addRemoteCommand(rawCmd);
    }
}