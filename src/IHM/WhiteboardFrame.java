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
    private JButton btnUndo;
    private JButton btnRedo;

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
        
        // ✅ UNDO/REDO buttons
        btnUndo = new JButton("↶ Undo");
        btnRedo = new JButton("↷ Redo");
        btnUndo.setEnabled(false);
        btnRedo.setEnabled(false);
        
        // ✅ Advanced color palette
        JButton btnColorPicker = new JButton("🎨 Color");
        JButton btnClear = new JButton("🗑️ Clear");
        
        // ✅ Size selector
        JComboBox<String> sizeSelector = new JComboBox<>(new String[]{"1px", "2px", "3px", "5px", "8px", "12px"});
        sizeSelector.setSelectedIndex(1); // Default 2px
        
        // ✅ Quick color buttons
        JButton btnBlack = new JButton("⚫");
        JButton btnRed = new JButton("🔴");
        JButton btnBlue = new JButton("🔵");
        JButton btnGreen = new JButton("🟢");
        JButton btnYellow = new JButton("🟡");
        JButton btnPurple = new JButton("🟣");

        // ✅ Event handlers
        btnUndo.addActionListener(e -> {
            canvas.undo();
            updateUndoRedoButtons();
        });
        btnRedo.addActionListener(e -> {
            canvas.redo();
            updateUndoRedoButtons();
        });
        
        btnColorPicker.addActionListener(e -> {
            Color selectedColor = JColorChooser.showDialog(this, "Choose Color", Color.decode(canvas.getCurrentColor()));
            if (selectedColor != null) {
                String hex = String.format("#%02x%02x%02x", selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue());
                canvas.setToolSettings(hex, canvas.getCurrentStrokeWidth());
                btnColorPicker.setBackground(selectedColor);
            }
        });
        
        btnClear.addActionListener(e -> {
            canvas.clearLocal();
            canvas.emitLocalCommand("CLEAR");
        });
        
        sizeSelector.addActionListener(e -> {
            String selected = (String) sizeSelector.getSelectedItem();
            float size = Float.parseFloat(selected.replace("px", ""));
            canvas.setToolSettings(canvas.getCurrentColor(), size);
        });
        
        // Quick color buttons
        btnBlack.addActionListener(e -> canvas.setToolSettings("#000000", canvas.getCurrentStrokeWidth()));
        btnRed.addActionListener(e -> canvas.setToolSettings("#FF0000", canvas.getCurrentStrokeWidth()));
        btnBlue.addActionListener(e -> canvas.setToolSettings("#0000FF", canvas.getCurrentStrokeWidth()));
        btnGreen.addActionListener(e -> canvas.setToolSettings("#00FF00", canvas.getCurrentStrokeWidth()));
        btnYellow.addActionListener(e -> canvas.setToolSettings("#FFFF00", canvas.getCurrentStrokeWidth()));
        btnPurple.addActionListener(e -> canvas.setToolSettings("#800080", canvas.getCurrentStrokeWidth()));

        // ✅ Add components to toolbar
        toolbar.add(btnUndo); toolbar.add(btnRedo);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(btnColorPicker); toolbar.add(sizeSelector);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(btnBlack); toolbar.add(btnRed); toolbar.add(btnBlue); 
        toolbar.add(btnGreen); toolbar.add(btnYellow); toolbar.add(btnPurple);
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

    // ✅ Update undo/redo button states
    private void updateUndoRedoButtons() {
        SwingUtilities.invokeLater(() -> {
            btnUndo.setEnabled(canvas.canUndo());
            btnRedo.setEnabled(canvas.canRedo());
        });
    }
}