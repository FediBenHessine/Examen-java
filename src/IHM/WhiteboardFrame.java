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
    private final String username; // ✅ NEW: Track username
    private boolean syncStarted = false;

    public WhiteboardFrame(boolean isHost, String username, int sessionId) {
        this.isHost = isHost;
        this.username = username;
        this.sessionId = sessionId;
        setTitle("Whiteboard | " + (isHost ? "🏠 HOST" : "👤 CLIENT") + " | " + username);
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        canvas = new CanvasPanel();
        canvas.setCurrentUsername(username); // ✅ NEW: Set username in canvas
        // ✅ FIX: Connect canvas to button updater
        canvas.setOnHistoryChanged(this::updateUndoRedoButtons);
        statusLabel = new JLabel(" Status: Initializing...");
        statusLabel.setForeground(Color.GRAY);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        // ✅ UNDO/REDO buttons
        btnUndo = new JButton("↶ Undo");
        btnRedo = new JButton("↷ Redo");
        btnUndo.setEnabled(false);
        btnRedo.setEnabled(false);
        
        // ✅ NEW: Tool buttons
        JButton btnLine = new JButton("📏 Line");
        JButton btnPen = new JButton("✏️ Pen");
        JButton btnErase = new JButton("🧹 Erase");
        JButton btnDelete = new JButton("🗑️ Delete");
        
        // ✅ Advanced color palette
        JButton btnColorPicker = new JButton("🎨 Color");
        JButton btnClear = new JButton("🧼 Clear Canvas");
        
        // ✅ Size selector
        JComboBox<String> sizeSelector = new JComboBox<>(new String[]{"1px", "2px", "3px", "5px", "8px", "12px"});
        sizeSelector.setSelectedIndex(1); // Default 2px
        

        // ✅ Event handlers - Undo/Redo
        btnUndo.addActionListener(e -> {
            canvas.undo();
//            updateUndoRedoButtons();
        });
        btnRedo.addActionListener(e -> {
            canvas.redo();
//            updateUndoRedoButtons();
        });
        
        // ✅ NEW: Tool selection buttons
        btnLine.addActionListener(e -> {
            canvas.setCurrentTool(DrawCommand.Type.LINE);
            updateToolButtons(btnLine, new JButton[]{btnLine, btnPen, btnErase, btnDelete});
            statusLabel.setText(" Tool: 📏 Line");
        });
        btnPen.addActionListener(e -> {
            canvas.setCurrentTool(DrawCommand.Type.PEN);
            updateToolButtons(btnPen, new JButton[]{btnLine, btnPen, btnErase, btnDelete});
            statusLabel.setText(" Tool: ✏️ Pen (Freehand drawing)");
        });
        btnErase.addActionListener(e -> {
            canvas.setCurrentTool(DrawCommand.Type.ERASER);
            updateToolButtons(btnErase, new JButton[]{btnLine, btnPen, btnErase, btnDelete});
            statusLabel.setText(" Tool: 🧹 Erase");
        });
        btnDelete.addActionListener(e -> {
            canvas.setCurrentTool(DrawCommand.Type.DELETE);
            updateToolButtons(btnDelete, new JButton[]{btnLine, btnPen, btnErase, btnDelete});
            statusLabel.setText(" Tool: 🗑️ Delete (Click to remove strokes)");
        });
        
        // ✅ Color and size controls
        btnColorPicker.addActionListener(e -> {
            Color selectedColor = JColorChooser.showDialog(this, "Choose Color", Color.decode(canvas.getCurrentColor()));
            if (selectedColor != null) {
                String hex = String.format("#%02x%02x%02x", selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue());
                canvas.setToolSettings(hex, canvas.getCurrentStrokeWidth());
                btnColorPicker.setBackground(selectedColor);
            }
        });
        
        btnClear.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Clear the entire canvas?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                canvas.clearLocal();
                canvas.emitLocalCommand("CLEAR");
            }
        });
        
        sizeSelector.addActionListener(e -> {
            String selected = (String) sizeSelector.getSelectedItem();
            float size = Float.parseFloat(selected.replace("px", ""));
            canvas.setToolSettings(canvas.getCurrentColor(), size);
        });
        

        // ✅ Add components to toolbar
//        toolbar.add(btnUndo); toolbar.add(btnRedo);
        toolbar.add(Box.createHorizontalStrut(10));
        
        // ✅ NEW: Tool buttons section
        toolbar.add(btnLine); toolbar.add(btnPen); toolbar.add(btnErase); toolbar.add(btnDelete);
        toolbar.add(Box.createHorizontalStrut(10));
        
        toolbar.add(btnColorPicker); toolbar.add(sizeSelector);
        toolbar.add(Box.createHorizontalStrut(10));

        toolbar.add(Box.createHorizontalStrut(20)); toolbar.add(btnClear);

        add(toolbar, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        SwingUtilities.invokeLater(this::updateUndoRedoButtons);

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

    // ✅ NEW: Update tool button styles to show active tool
    private void updateToolButtons(JButton activeBtn, JButton[] allBtns) {
        for (JButton btn : allBtns) {
            if (btn == activeBtn) {
                btn.setBackground(new Color(100, 150, 200));
                btn.setOpaque(true);
                btn.setFocusPainted(false);
            } else {
                btn.setBackground(UIManager.getColor("Button.background"));
                btn.setOpaque(false);
            }
        }
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

    // ✅ FIX 3: Offload DB write to background thread + include username
    public void bindLocalDraw(java.util.function.Consumer<String> sender) {
        // First set the network sender
        canvas.setOnLocalDraw(rawCmd -> {
            // Then handle DB + forwarding
            if (isHost) {
                new Thread(() -> {
                    try {
                        DrawCommand cmd = CommandProtocol.deserialize(rawCmd);
                        if (cmd != null) {
                            cmd.username = username;
                            DatabaseManager.insertDrawCommand(sessionId, cmd);
                        }
                    } catch (Exception ignored) {}
                }, "DB-Writer").start();
            }
            sender.accept(rawCmd); // Forward to network
        });
    }

    public void handleNetworkCommand(String rawCmd) {
        if ("PING".equals(rawCmd)) {
            if (networkSender != null) networkSender.accept("PONG");
            return;
        }
        if ("PONG".equals(rawCmd)) { setAlive(true); return; }
        if ("HOST_CLOSED".equals(rawCmd)) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "🚪 Host closed session", "Session Ended", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            });
            return;
        }

        // ✅ Detect SYNC_START command from host
        if ("SYNC_START".equals(rawCmd)) {
            System.out.println("🔄 Received SYNC_START - beginning animated sync");
            syncStarted = true;
            canvas.startInitialSync();
            return;
        }

        // ✅ Detect SYNC_END command from host
        if ("SYNC_END".equals(rawCmd)) {
            System.out.println("✅ Received SYNC_END - finishing animated sync");
            canvas.endInitialSync();
            syncStarted = false;
            return;
        }

        if ("CLEAR".equals(rawCmd)) { canvas.clearLocal(); return; }

        // All other commands go through canvas (which handles sync queuing)
        canvas.addRemoteCommand(rawCmd);
    }
    // ✅ Update undo/redo button states
    private void updateUndoRedoButtons() {
        if (SwingUtilities.isEventDispatchThread()) {
            btnUndo.setEnabled(canvas.canUndo());
            btnRedo.setEnabled(canvas.canRedo());
        } else {
            SwingUtilities.invokeLater(() -> {
                btnUndo.setEnabled(canvas.canUndo());
                btnRedo.setEnabled(canvas.canRedo());
            });
        }
    }
}