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
    private final boolean isHost;
    private final int sessionId;
    private Timer heartbeatTimer;
    private static final int MAX_MISSED_PONGS = 3;
    private java.util.function.Consumer<String> networkSender;

    private final String username; // ✅ NEW: Track username

    public WhiteboardFrame(boolean isHost, String username, int sessionId) {
        this.isHost = isHost;
        this.username = username;
        this.sessionId = sessionId;
        setTitle("Whiteboard - " + (isHost ? "Host" : "Client") + " - " + username);
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);

        canvas = new CanvasPanel();
        canvas.setCurrentUsername(username); // ✅ NEW: Set username in canvas
        // ✅ FIX: Connect canvas to button updater
        statusLabel = new JLabel("Status: Initializing...");
        statusLabel.setForeground(Color.DARK_GRAY);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusLabel.setBackground(Color.WHITE);
        statusLabel.setOpaque(true);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        toolbar.setBackground(new Color(245, 245, 245));

        // Tool buttons panel
        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        toolPanel.setBackground(new Color(245, 245, 245));

        // ✅ NEW: Tool buttons
        JButton btnLine = createToolButton("Line");
        JButton btnPen = createToolButton("Pen");
        JButton btnErase = createToolButton("Erase");
        JButton btnDelete = createToolButton("Delete");

        // Settings panel
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        settingsPanel.setBackground(new Color(245, 245, 245));

        // ✅ Advanced color palette
        JButton btnColorPicker = createToolButton("Color");
        btnColorPicker.setBackground(Color.BLACK); // Default color
        btnColorPicker.setForeground(Color.WHITE); // Ensure text is visible on black

        // ✅ Size selector
        JComboBox<String> sizeSelector = new JComboBox<>(new String[]{"1px", "2px", "3px", "5px", "8px", "12px"});
        sizeSelector.setSelectedIndex(1); // Default 2px
        sizeSelector.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sizeSelector.setMaximumSize(new Dimension(60, 30));

        // Actions panel
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        actionsPanel.setBackground(new Color(245, 245, 245));

        JButton btnClear = createToolButton("Clear Canvas");

        // Add to panels
        toolPanel.add(btnLine);
        toolPanel.add(btnPen);
        toolPanel.add(btnErase);
        toolPanel.add(btnDelete);

        settingsPanel.add(btnColorPicker);
        settingsPanel.add(sizeSelector);

        actionsPanel.add(btnClear);

        toolbar.add(toolPanel);
        toolbar.add(Box.createHorizontalStrut(20));
        toolbar.add(settingsPanel);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(actionsPanel);

        // ✅ NEW: Tool selection buttons
        btnLine.addActionListener(e -> {
            canvas.setCurrentTool(DrawCommand.Type.LINE);
            updateToolButtons(btnLine, new JButton[]{btnLine, btnPen, btnErase, btnDelete});
            statusLabel.setText("Tool: Line");
        });
        btnPen.addActionListener(e -> {
            canvas.setCurrentTool(DrawCommand.Type.PEN);
            updateToolButtons(btnPen, new JButton[]{btnLine, btnPen, btnErase, btnDelete});
            statusLabel.setText("Tool: Pen (Freehand drawing)");
        });
        btnErase.addActionListener(e -> {
            canvas.setCurrentTool(DrawCommand.Type.ERASER);
            updateToolButtons(btnErase, new JButton[]{btnLine, btnPen, btnErase, btnDelete});
            statusLabel.setText("Tool: Erase");
        });
        btnDelete.addActionListener(e -> {
            canvas.setCurrentTool(DrawCommand.Type.DELETE);
            updateToolButtons(btnDelete, new JButton[]{btnLine, btnPen, btnErase, btnDelete});
            statusLabel.setText("Tool: Delete (Click to remove strokes)");
        });

        // ✅ Color and size controls
        btnColorPicker.addActionListener(e -> {
            Color selectedColor = JColorChooser.showDialog(this, "Choose Color", Color.decode(canvas.getCurrentColor()));
            if (selectedColor != null) {
                String hex = String.format("#%02x%02x%02x", selectedColor.getRed(), selectedColor.getGreen(), selectedColor.getBlue());
                canvas.setToolSettings(hex, canvas.getCurrentStrokeWidth());
                btnColorPicker.setBackground(selectedColor);
                // Adjust text color for contrast
                int r = selectedColor.getRed();
                int g = selectedColor.getGreen();
                int b = selectedColor.getBlue();
                double luminance = 0.299 * r + 0.587 * g + 0.114 * b;
                btnColorPicker.setForeground(luminance > 128 ? Color.BLACK : Color.WHITE);
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

    private JButton createToolButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setPreferredSize(new Dimension(80, 30));
        return button;
    }

    // ✅ NEW: Update tool button styles to show active tool
    private void updateToolButtons(JButton activeBtn, JButton[] allBtns) {
        for (JButton btn : allBtns) {
            if (btn == activeBtn) {
                btn.setBackground(new Color(0, 123, 255));
                btn.setForeground(Color.WHITE);
                btn.setOpaque(true);
                btn.setBorder(BorderFactory.createLoweredBevelBorder());
            } else {
                btn.setBackground(UIManager.getColor("Button.background"));
                btn.setForeground(Color.BLACK);
                btn.setOpaque(false);
                btn.setBorder(BorderFactory.createRaisedBevelBorder());
            }
        }
    }

    public CanvasPanel getCanvas() { return canvas; }
    public void updateStatus(String text) { SwingUtilities.invokeLater(() -> statusLabel.setText(" " + text)); }
    public void setAlive(boolean alive) {
        updateStatus("Status: " + (alive ? "Syncing" : "Connecting..."));
    }

    public void bindNetworkSender(java.util.function.Consumer<String> sender) {
        this.networkSender = sender;
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
                JOptionPane.showMessageDialog(this, "Host closed session", "Session Ended", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            });
            return;
        }

        // ✅ Detect SYNC_START command from host
        if ("SYNC_START".equals(rawCmd)) {
            System.out.println("Received SYNC_START - beginning animated sync");
            canvas.startInitialSync();
            return;
        }

        // ✅ Detect SYNC_END command from host
        if ("SYNC_END".equals(rawCmd)) {
            System.out.println("Received SYNC_END - finishing animated sync");
            canvas.endInitialSync();
            return;
        }

        if ("CLEAR".equals(rawCmd)) { canvas.clearLocal(); return; }

        // All other commands go through canvas (which handles sync queuing)
        // ✅ HOST: Store client commands in database (but not our own echoing back)
        if (isHost) {
            try {
                DrawCommand cmd = CommandProtocol.deserialize(rawCmd);
                if (cmd != null && cmd.username != null && !cmd.username.equals(username)) {
                    // This is a command from a client - store it in the database
                    DatabaseManager.insertDrawCommand(sessionId, cmd);
                    System.out.println("Stored client command from " + cmd.username + ": " + cmd.type);
                }
            } catch (Exception e) {
                System.err.println("Failed to store client command: " + e.getMessage());
            }
        }
        canvas.addRemoteCommand(rawCmd);
    }
    // ✅ Update undo/redo button states

}