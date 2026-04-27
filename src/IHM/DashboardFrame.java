package IHM;

import Database.DatabaseManager;
import Model.RoomInfo;
import Model.RoomType;
import Network.UDPRoomDiscovery;
import Network.NetworkUtils;
import RMI.RemoteSession;
import RMI.SessionImpl;

import javax.swing.*;
import java.awt.*;
import java.rmi.Naming;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DashboardFrame extends JFrame {
    // === JOIN FLOW ===
    private void showJoinRoomDialog() {
        if (joinDialogOpen) {
            System.out.println("⚠️ Join dialog already open, ignoring request");
            return;
        }
        joinDialogOpen = true;

        JDialog dialog = new JDialog(this, "🔍 Join a Room", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        roomListModel.clear();
        JList<RoomInfo> roomList = new JList<>(roomListModel);
        roomList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel("<html><b>" + value.roomName + "</b><br>" +
                    "Host: " + value.hostUsername + " | " + value.roomType + "</html>");
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            if (isSelected) label.setBackground(list.getSelectionBackground());
            return label;
        });

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnRefresh = new JButton("🔄 Refresh");
        JButton btnJoin = new JButton("✅ Join Selected");
        JButton btnDirect = new JButton("🔗 Direct Join (Private)");

        final Timer[] discoveryTimer = new Timer[1];

        final Runnable cleanup = () -> {
            System.out.println("🧹 Cleaning up join dialog resources...");
            if (discoveryTimer[0] != null && discoveryTimer[0].isRunning()) {
                discoveryTimer[0].stop();
                System.out.println("⏹️ Stopped periodic room discovery timer");
            }
            joinDialogOpen = false;
            System.out.println("✅ Join dialog closed, flag reset");
        };

        btnRefresh.addActionListener(e -> discoverRoomsAsync());

        btnJoin.addActionListener(e -> {
            RoomInfo selected = roomList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(dialog, "Please select a room", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            attemptJoin(selected, dialog);
        });

        btnDirect.addActionListener(e -> {
            String ip = JOptionPane.showInputDialog(dialog, "Host IP Address:", NetworkUtils.getLocalIP());
            if (ip == null || ip.trim().isEmpty()) return;
            final String finalIP = ip.trim();

            dialog.setEnabled(false);
            statusLabel.setText(" Status: 🔍 Looking for room at " + finalIP + "...");

            new Thread(() -> {
                RoomInfo room = DatabaseManager.getRoomByIP(finalIP);
                SwingUtilities.invokeLater(() -> {
                    dialog.setEnabled(true);
                    if (room == null) {
                        JOptionPane.showMessageDialog(dialog,
                                "❌ No active room found at IP: " + finalIP,
                                "Room Not Found", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText(" Status: Ready");
                        return;
                    }
                    String password = JOptionPane.showInputDialog(dialog,
                            "🔐 Enter password for '" + room.roomName + "'",
                            "Join Private Room", JOptionPane.QUESTION_MESSAGE);
                    if (password == null) {
                        statusLabel.setText(" Status: Ready");
                        return;
                    }
                    dialog.setEnabled(false);
                    statusLabel.setText(" Status: 🔐 Verifying password...");
                    new Thread(() -> {
                        boolean isValid = udpDiscovery.requestJoin(room.hostIP, room.roomName, password);
                        SwingUtilities.invokeLater(() -> {
                            dialog.setEnabled(true);
                            if (isValid) {
                                statusLabel.setText(" Status: ✅ Access granted. Joining...");
                                proceedToJoin(room, dialog);
                            } else {
                                JOptionPane.showMessageDialog(dialog, "❌ Incorrect password.",
                                        "Access Denied", JOptionPane.ERROR_MESSAGE);
                                statusLabel.setText(" Status: Ready");
                            }
                        });
                    }).start();
                });
            }).start();
        });

        btnPanel.add(btnRefresh);
        btnPanel.add(btnJoin);
        btnPanel.add(btnDirect);

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(new JLabel("📡 Discovered Rooms:"), BorderLayout.NORTH);
        infoPanel.add(new JScrollPane(roomList), BorderLayout.CENTER);

        dialog.add(infoPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(this);

        // ✅ Window listener for cleanup on close
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                System.out.println("ℹ️ Join dialog windowClosed event triggered");
                cleanup.run();
            }
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.out.println("ℹ️ Join dialog windowClosing event triggered");
                cleanup.run();
            }
        });

        // ✅ Create periodic discovery timer (every 10 seconds)
        discoveryTimer[0] = new Timer(10000, e -> {
            System.out.println("🔄 Periodic discovery triggered");
            discoverRoomsAsync();
        });
        discoveryTimer[0].setRepeats(true);

        // ✅ KEY FIX: Start timer AND initial discovery BEFORE showing dialog
        statusLabel.setText(" Status: 🔍 Discovering rooms...");
        discoveryTimer[0].start();
        System.out.println("🔄 Started periodic room discovery timer (every 10 seconds)");
        discoverRoomsAsync();  // Initial discovery

        // ✅ NOW show the dialog (modal, so this blocks until closed)
        System.out.println("📂 Opening join dialog...");
        dialog.setVisible(true);
        // ⚠️ Code here runs AFTER dialog closes - timer already stopped by cleanup
    }    private void discoverRoomsAsync() {
        // ✅ Prevent overlapping discoveries
        if (discoveryInProgress) return;
        discoveryInProgress = true;

        statusLabel.setText(" Status: 🔍 Broadcasting...");
        new Thread(() -> {
            List<RoomInfo> rooms = udpDiscovery.discoverRooms();
            SwingUtilities.invokeLater(() -> {
                roomListModel.clear();
                rooms.forEach(roomListModel::addElement);
                statusLabel.setText(" Status: Found " + rooms.size() + " room(s)");
                discoveryInProgress = false; // ✅ Allow new discoveries
            });
        }, "UDP-Discovery-Thread").start();
    }
    private static final Logger LOGGER = Logger.getLogger(DashboardFrame.class.getName());
    private final String username;
    private final JLabel statusLabel;
    private UDPRoomDiscovery udpDiscovery;

    private final DefaultListModel<RoomInfo> roomListModel;
    // ✅ CRITICAL: Keep socket references alive to prevent GC
    private Network.SocketServer hostSocketServer;

    private Network.SocketClient clientSocketClient;
    // ✅ Prevent multiple simultaneous discoveries
    private volatile boolean discoveryInProgress = false;
    // ✅ Prevent multiple join dialogs

    private volatile boolean joinDialogOpen = false;

    public DashboardFrame(String username) {
        this.username = username;

        setTitle("🎨 Whiteboard Dashboard - " + username);
        setSize(550, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT));
        header.add(new JLabel("👋 Welcome, " + username + "!"));
        header.add(Box.createHorizontalGlue());
        JButton btnLogout = new JButton("🚪 Logout");
        btnLogout.addActionListener(e -> {
            if (udpDiscovery != null) udpDiscovery.stop();
            dispose();
            new LoginDialog(null).setVisible(true);
        });
        header.add(btnLogout);
        add(header, BorderLayout.NORTH);

        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 10));
        JButton btnHost = new JButton("<html><h2>🏠 Host a Room</h2><br>Create a new whiteboard session<br>others can join you</html>");
        btnHost.setPreferredSize(new Dimension(250, 100));
        btnHost.addActionListener(e -> showHostRoomDialog());
        JButton btnJoin = new JButton("<html><h2>🔍 Join a Room</h2><br>Discover or connect to an existing session<br>collaborate in real-time</html>");
        btnJoin.setPreferredSize(new Dimension(250, 100));
        btnJoin.addActionListener(e -> showJoinRoomDialog());
        actions.add(btnHost);
        actions.add(btnJoin);
        add(actions, BorderLayout.CENTER);

        statusLabel = new JLabel(" Status: Ready | IP: " + NetworkUtils.getLocalIP());
        add(statusLabel, BorderLayout.SOUTH);

        udpDiscovery = new UDPRoomDiscovery();
        roomListModel = new DefaultListModel<>();
        udpDiscovery.setOnRoomDiscovered(roomListModel::addElement);
    }
    // === HOST FLOW ===

    private void showHostRoomDialog() {
        JDialog dialog = new JDialog(this, "🏠 Host a New Room", true);
        dialog.setLayout(new GridLayout(6, 2, 5, 5));

        JTextField txtRoomName = new JTextField(username + "'s Whiteboard");
        JComboBox<RoomType> cmbType = new JComboBox<>(RoomType.values());
        JPasswordField txtPassword = new JPasswordField();
        txtPassword.setEnabled(false);
        JTextField txtPort = new JTextField("8083");

        cmbType.addActionListener(e -> {
            RoomType selected = (RoomType) cmbType.getSelectedItem();
            txtPassword.setEnabled(selected != RoomType.PUBLIC);
        });

        dialog.add(new JLabel("Room Name:")); dialog.add(txtRoomName);
        dialog.add(new JLabel("Visibility:")); dialog.add(cmbType);
        dialog.add(new JLabel("Password:")); dialog.add(txtPassword);
        dialog.add(new JLabel("Socket Port:")); dialog.add(txtPort);

        JButton btnCreate = new JButton("🚀 Start Hosting");
        JPanel btnPanel = new JPanel();
        btnPanel.add(btnCreate);
        dialog.add(new JLabel()); dialog.add(btnPanel);

        btnCreate.addActionListener(e -> {
            String roomName = txtRoomName.getText().trim();
            RoomType type = (RoomType) cmbType.getSelectedItem();
            if (type == null) type = RoomType.PUBLIC;
            String password = (type == RoomType.PUBLIC) ? null : new String(txtPassword.getPassword());
            int port;
            try { port = Integer.parseInt(txtPort.getText()); }
            catch (NumberFormatException ex) { port = 8083; }

            if (roomName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Room name is required", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (type != RoomType.PUBLIC && password != null && password.length() < 4) {
                JOptionPane.showMessageDialog(dialog, "Password must be at least 4 characters", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            btnCreate.setEnabled(false);
            statusLabel.setText(" Status: Starting host services...");

            final String finalRoomName = roomName;
            final RoomType finalType = type;
            final String finalPassword = password;
            final int finalPort = port;

            new Thread(() -> {
                try {
                    final String localIP = NetworkUtils.getLocalIP();
                    System.out.println("🔧 Host setup: IP=" + localIP + ", Port=" + finalPort);

                    DatabaseManager.createRoom(username, finalRoomName, finalType, finalPassword, localIP, finalPort);

                    SessionImpl session = new SessionImpl();
                    try { java.rmi.registry.LocateRegistry.createRegistry(1099); }
                    catch (java.rmi.RemoteException ex) { System.out.println("ℹ️ RMI registry already exists"); }
                    Naming.rebind("rmi://" + localIP + ":1099/WhiteboardSession", session);
                    session.registerUser(username);
                    int sessionId = session.notifySessionStart();
                    System.out.println("✅ RMI session created, ID=" + sessionId);

                    udpDiscovery.startListening(localIP, finalPort, finalRoomName, username, finalType, finalPassword);
                    System.out.println("🟢 UDP discovery listening on :8888");

                    // ✅ Capture session for lambda scope
                    final SessionImpl currentSession = session;

                    SwingUtilities.invokeLater(() -> {
                        dialog.dispose();
                        WhiteboardFrame frame = new WhiteboardFrame(true, username, sessionId);
                        frame.setVisible(true);

                        Network.SocketServer server = new Network.SocketServer();

                        // DashboardFrame.java - Inside host's setOnClientConnected callback:

                        server.setOnClientConnected(() -> {
                            frame.setAlive(true);

                            // ✅ Send sync markers to client
                            server.broadcast("SYNC_START");

                            System.out.println("📤 Syncing existing drawings to client...");
                            List<String> history = DatabaseManager.getDrawCommandsForSession(sessionId);

                            for (String cmd : history) {
                                // Small delay between commands for smooth animation (optional)
                                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                                server.broadcast(cmd);
                            }

                            server.broadcast("SYNC_END");
                            System.out.println("✅ Synced " + history.size() + " commands with animation markers");
                        });

                        server.setOnClientDisconnected(() -> frame.setAlive(false));
                        server.setOnCommandReceived(frame::handleNetworkCommand);

                        frame.bindNetworkSender(server::broadcast);
                        frame.bindLocalDraw(server::broadcast); // ✅ Use broadcast instead of send

                        server.start(finalPort);
                        statusLabel.setText(" Status: 🟢 Hosting '" + finalRoomName + "' on " + localIP + ":" + finalPort);

                        // 🔥 Clean shutdown: notify clients → free ports → update UI
                        frame.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosing(java.awt.event.WindowEvent e) {
                                System.out.println("🧹 Closing host window - notifying clients & freeing ports...");
                                new Thread(() -> {
                                    server.broadcast("HOST_CLOSED"); // Notify all clients first
                                    try { Thread.sleep(300); } catch (InterruptedException ignored) {} // Give network time to deliver
                                    server.shutdown();             // Closes TCP
                                    udpDiscovery.stop();           // Closes UDP
                                    try { currentSession.notifySessionEnd(); } catch (Exception ignored) {}
                                    SwingUtilities.invokeLater(() -> statusLabel.setText(" Status: Ready"));
                                }).start();
                            }
                        });
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(dialog, "Hosting failed:\n" + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        btnCreate.setEnabled(true);
                        statusLabel.setText(" Status: Ready");
                    });
                    LOGGER.log(Level.SEVERE, "Failed to start hosting", ex);
                    ex.printStackTrace();
                }
            }, "Host-Setup-Thread").start();
        });

        dialog.setSize(450, 280);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    // === JOIN FLOW ===

    private void attemptJoin(RoomInfo room, JDialog parentDialog) {
        if (room.requiresPassword) {
            String password = JOptionPane.showInputDialog(parentDialog, "🔐 Password for '" + room.roomName + "':");
            if (password == null) return; // User cancelled

            // 🔥 Validate BEFORE proceeding
            parentDialog.setEnabled(false);
            statusLabel.setText(" Status: 🔐 Verifying password...");

            new Thread(() -> {
                boolean isValid = udpDiscovery.requestJoin(room.hostIP, room.roomName, password);
                SwingUtilities.invokeLater(() -> {
                    parentDialog.setEnabled(true);
                    if (isValid) {
                        statusLabel.setText(" Status: ✅ Access granted. Joining...");
                        proceedToJoin(room, parentDialog);
                    } else {
                        JOptionPane.showMessageDialog(parentDialog,
                                "❌ Incorrect password.\nPlease try again or contact the host.",
                                "Access Denied", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText(" Status: Ready");
                    }
                });
            }).start();
        } else {
            proceedToJoin(room, parentDialog);
        }
    }

    private void proceedToJoin(RoomInfo room, JDialog parentDialog) {
        statusLabel.setText(" Status: Connecting to " + room.hostIP + "...");
        System.out.println("🔧 Client join: " + room.hostIP + ":" + room.socketPort);

        new Thread(() -> {
            try {
                System.out.println("🔍 RMI lookup: " + room.hostIP + ":1099");
                RemoteSession remote = (RemoteSession) Naming.lookup("rmi://" + room.hostIP + ":1099/WhiteboardSession");
                remote.registerUser(username);
                int sessionId = remote.notifySessionStart();
                System.out.println("✅ RMI joined, session=" + sessionId);

                SwingUtilities.invokeLater(() -> {
                    parentDialog.dispose();
                    WhiteboardFrame frame = new WhiteboardFrame(false, username, sessionId);
                    frame.setVisible(true);

                    Network.SocketClient client = new Network.SocketClient();
                    client.setOnConnected(() -> frame.setAlive(true));
                    client.setOnDisconnected(() -> frame.setAlive(false));
                    client.setOnCommandReceived(frame::handleNetworkCommand);
                    frame.bindLocalDraw(client::send);
                    frame.bindNetworkSender(client::send);
                    client.connect(room.hostIP, room.socketPort);

                    // ✅ Capture session for lambda scope
                    final RemoteSession currentSession = remote;
                    frame.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override
                        public void windowClosing(java.awt.event.WindowEvent e) {
                            System.out.println("🧹 Closing client window - freeing ports...");
                            new Thread(() -> {
                                client.shutdown();            // Frees TCP socket
                                try { currentSession.notifySessionEnd(); } catch (Exception ignored) {}
                                SwingUtilities.invokeLater(() -> statusLabel.setText(" Status: Ready"));
                            }).start();
                        }
                    });

                    statusLabel.setText(" Status: ✅ Joined '" + room.roomName + "'");
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(parentDialog, "Failed to join:\n" + ex.getMessage(),
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                    parentDialog.setEnabled(true);
                    statusLabel.setText(" Status: Ready");
                });
                LOGGER.log(Level.SEVERE, "Failed to join room", ex);
                ex.printStackTrace();
            }
        }, "Join-Setup-Thread").start();
    }

    // ✅ Cleanup method to properly close sockets
    @Override
    public void dispose() {
        if (hostSocketServer != null) hostSocketServer.shutdown();
        if (clientSocketClient != null) clientSocketClient.shutdown();
        if (udpDiscovery != null) udpDiscovery.stop();
        super.dispose();
    }

    public void updateStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(" " + text));
    }
}

