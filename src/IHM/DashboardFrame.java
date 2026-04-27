package IHM;

import Database.DatabaseManager;
import Model.RoomInfo;
import Model.RoomType;
import Network.NetworkUtils;
import Network.UDPRoomDiscovery;
import RMI.RemoteSession;
import RMI.SessionImpl;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.Naming;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DashboardFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(DashboardFrame.class.getName());
    private final String username;
    private final JLabel statusLabel;
    private final DefaultListModel<RoomInfo> roomListModel;
    private UDPRoomDiscovery udpDiscovery;
    // ...existing code...
    private Network.SocketServer hostSocketServer;
    private Network.SocketClient clientSocketClient;
    // ...existing code...
    private volatile boolean discoveryInProgress = false;
    private volatile boolean joinDialogOpen = false;
    public DashboardFrame(String username) {
        this.username = username;

        setTitle("Whiteboard Dashboard - " + username);
        setSize(650, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(248, 250, 252));
        setLayout(new BorderLayout(0, 0));

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(99, 102, 241));
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        JLabel titleLabel = new JLabel("Welcome, " + username);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton btnLogout = new JButton("Logout");
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnLogout.setBackground(new Color(239, 68, 68));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFocusPainted(false);
        btnLogout.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btnLogout.setBackground(new Color(220, 38, 38));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btnLogout.setBackground(new Color(239, 68, 68));
            }
        });
        btnLogout.addActionListener(e -> {
            if (udpDiscovery != null) udpDiscovery.stop();
            dispose();
            new LoginDialog(null).setVisible(true);
        });
        headerPanel.add(btnLogout, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Main Content Panel
        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(new Color(248, 250, 252));
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBorder(new EmptyBorder(40, 30, 30, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 0, 15, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Title/Subtitle
        gbc.gridy = 0;
        JLabel subtitleLabel = new JLabel("What would you like to do?");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(107, 114, 128));
        contentPanel.add(subtitleLabel, gbc);

        // Host Room Button
        gbc.gridy = 1;
        gbc.insets = new Insets(15, 0, 10, 0);
        JButton btnHost = createActionButton("Host a Session", "Create a new whiteboard\nOthers can join you", new Color(16, 185, 129));
        contentPanel.add(btnHost, gbc);
        btnHost.addActionListener(e -> showHostRoomDialog());

        // Join Room Button
        gbc.gridy = 2;
        gbc.insets = new Insets(10, 0, 30, 0);
        JButton btnJoin = createActionButton("Join a Session", "Discover and connect\nCollaborate in real-time", new Color(59, 130, 246));
        contentPanel.add(btnJoin, gbc);
        btnJoin.addActionListener(e -> showJoinRoomDialog());

        add(contentPanel, BorderLayout.CENTER);

        // Status Panel
        JPanel statusPanel = new JPanel();
        statusPanel.setBackground(new Color(241, 245, 249));
        statusPanel.setBorder(new EmptyBorder(12, 30, 12, 30));
        statusLabel = new JLabel("Status: Ready | IP: " + NetworkUtils.getLocalIP());
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(75, 85, 99));
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.SOUTH);

        udpDiscovery = new UDPRoomDiscovery();
        roomListModel = new DefaultListModel<>();
        udpDiscovery.setOnRoomDiscovered(roomListModel::addElement);
    }

    private JButton createActionButton(String title, String description, Color bgColor) {
        JButton button = new JButton("<html><b style='font-size:14px;'>" + title + "</b><br><span style='font-size:11px;'>" + description.replace("\n", "<br>") + "</span></html>");
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(550, 100));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(Math.max(0, bgColor.getRed() - 15), Math.max(0, bgColor.getGreen() - 15), Math.max(0, bgColor.getBlue() - 15)));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        return button;
    }
    // ...existing code...

    private void showJoinRoomDialog() {
        if (joinDialogOpen) {
            System.out.println("Join dialog already open, ignoring request");
            return;
        }
        joinDialogOpen = true;

        JDialog dialog = new JDialog(this, "Join a Room", true);
        dialog.getContentPane().setBackground(new Color(248, 250, 252));
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        roomListModel.clear();
        JList<RoomInfo> roomList = new JList<>(roomListModel);
        roomList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel("<html><b>" + value.roomName + "</b><br>" +
                    "Host: " + value.hostUsername + " | " + value.roomType + "</html>");
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
                label.setOpaque(true);
            } else {
                label.setBackground(Color.WHITE);
                label.setForeground(Color.BLACK);
                label.setOpaque(true);
            }
            return label;
        });

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnRefresh = createDialogButton("Refresh", new Color(59, 130, 246));
        JButton btnJoin = createDialogButton("Join Selected", new Color(16, 185, 129));
        JButton btnDirect = createDialogButton("Direct Join", new Color(245, 158, 11));

        final Timer[] discoveryTimer = new Timer[1];

        final Runnable cleanup = () -> {
            System.out.println("Cleaning up join dialog resources...");
            if (discoveryTimer[0] != null && discoveryTimer[0].isRunning()) {
                discoveryTimer[0].stop();
                System.out.println("Stopped periodic room discovery timer");
            }
            joinDialogOpen = false;
            System.out.println("Join dialog closed, flag reset");
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

            // Ask for password first (assuming direct join is for private rooms)
            String password = JOptionPane.showInputDialog(dialog,
                    "Enter password for direct join to " + finalIP,
                    "Direct Join", JOptionPane.QUESTION_MESSAGE);
            if (password == null) {
                statusLabel.setText("Status: Ready");
                return;
            }

            dialog.setEnabled(false);
            statusLabel.setText("Status: Verifying password...");

            new Thread(() -> {
                RoomInfo room = udpDiscovery.requestDirectJoin(finalIP, password);
                SwingUtilities.invokeLater(() -> {
                    dialog.setEnabled(true);
                    if (room == null) {
                        JOptionPane.showMessageDialog(dialog,
                                "Direct join failed.\nInvalid password or no room found at: " + finalIP,
                                "Join Failed", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText("Status: Ready");
                        return;
                    }
                    statusLabel.setText("Status: Access granted. Joining...");
                    proceedToJoin(room, dialog);
                });
            }).start();
        });

        btnPanel.add(btnRefresh);
        btnPanel.add(btnJoin);
        btnPanel.add(btnDirect);

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.add(new JLabel("Discovered Rooms:"), BorderLayout.NORTH);
        infoPanel.add(new JScrollPane(roomList), BorderLayout.CENTER);

        dialog.add(infoPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(this);

        // ...existing code...
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                System.out.println("Join dialog windowClosed event triggered");
                cleanup.run();
            }

            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.out.println("Join dialog windowClosing event triggered");
                cleanup.run();
            }
        });

        // ...existing code...
        discoveryTimer[0] = new Timer(10000, e -> {
            System.out.println("Periodic discovery triggered");
            discoverRoomsAsync();
        });
        discoveryTimer[0].setRepeats(true);

        // ...existing code...
        statusLabel.setText("Status: Discovering rooms...");
        discoveryTimer[0].start();
        System.out.println("Started periodic room discovery timer (every 10 seconds)");
        discoverRoomsAsync();  // Initial discovery

        // ...existing code...
        System.out.println("Opening join dialog...");
        dialog.setVisible(true);
    }


    private JButton createDialogButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(Math.max(0, bgColor.getRed() - 15), Math.max(0, bgColor.getGreen() - 15), Math.max(0, bgColor.getBlue() - 15)));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        return button;
    }

    private void discoverRoomsAsync() {
        // ...existing code...
        if (discoveryInProgress) return;
        discoveryInProgress = true;

        statusLabel.setText("Status: Broadcasting...");
        new Thread(() -> {
            List<RoomInfo> rooms = udpDiscovery.discoverRooms();
            SwingUtilities.invokeLater(() -> {
                roomListModel.clear();
                rooms.forEach(roomListModel::addElement);
                statusLabel.setText("Status: Found " + rooms.size() + " room(s)");
                discoveryInProgress = false; // ...existing code...
            });
        }, "UDP-Discovery-Thread").start();
    }
    // ...existing code...

    private void showHostRoomDialog() {
        JDialog dialog = new JDialog(this, "Host a New Room", true);
        dialog.getContentPane().setBackground(new Color(248, 250, 252));
        dialog.setLayout(new BorderLayout(0, 0));

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(16, 185, 129));
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        JLabel titleLabel = new JLabel("Create New Session");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        dialog.add(headerPanel, BorderLayout.NORTH);

        // Content Panel
        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(new Color(248, 250, 252));
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JTextField txtRoomName = new JTextField(username + "'s Whiteboard");
        JComboBox<RoomType> cmbType = new JComboBox<>(RoomType.values());
        JPasswordField txtPassword = new JPasswordField();
        txtPassword.setEnabled(false);
        JTextField txtPort = new JTextField("8083");

        cmbType.addActionListener(e -> {
            RoomType selected = (RoomType) cmbType.getSelectedItem();
            txtPassword.setEnabled(selected != RoomType.PUBLIC);
        });

        // Room Name
        gbc.gridy = 0;
        JLabel lbl1 = new JLabel("Room Name");
        lbl1.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl1.setForeground(new Color(75, 85, 99));
        contentPanel.add(lbl1, gbc);

        gbc.gridy = 1;
        txtRoomName.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        txtRoomName.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(219, 222, 225)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        txtRoomName.setBackground(Color.WHITE);
        contentPanel.add(txtRoomName, gbc);

        // Visibility
        gbc.gridy = 2;
        JLabel lbl2 = new JLabel("Visibility");
        lbl2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl2.setForeground(new Color(75, 85, 99));
        contentPanel.add(lbl2, gbc);

        gbc.gridy = 3;
        cmbType.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        contentPanel.add(cmbType, gbc);

        // Password
        gbc.gridy = 4;
        JLabel lbl3 = new JLabel("Password");
        lbl3.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl3.setForeground(new Color(75, 85, 99));
        contentPanel.add(lbl3, gbc);

        gbc.gridy = 5;
        txtPassword.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(219, 222, 225)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        txtPassword.setBackground(Color.WHITE);
        contentPanel.add(txtPassword, gbc);

        // Port
        gbc.gridy = 6;
        JLabel lbl4 = new JLabel("Socket Port");
        lbl4.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl4.setForeground(new Color(75, 85, 99));
        contentPanel.add(lbl4, gbc);

        gbc.gridy = 7;
        txtPort.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        txtPort.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(219, 222, 225)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        txtPort.setBackground(Color.WHITE);
        contentPanel.add(txtPort, gbc);

        dialog.add(contentPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setBackground(new Color(248, 250, 252));
        btnPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        JButton btnCreate = createDialogButton("Start Hosting", new Color(16, 185, 129));
        JButton btnCancel = createDialogButton("Cancel", new Color(107, 114, 128));
        btnPanel.add(btnCreate);
        btnPanel.add(btnCancel);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dialog.dispose());

        btnCreate.addActionListener(e -> {
            String roomName = txtRoomName.getText().trim();
            RoomType type = (RoomType) cmbType.getSelectedItem();
            if (type == null) type = RoomType.PUBLIC;
            String password = (type == RoomType.PUBLIC) ? null : new String(txtPassword.getPassword());
            int port;
            try {
                port = Integer.parseInt(txtPort.getText());
            } catch (NumberFormatException ex) {
                port = 8083;
            }

            if (roomName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Room name is required", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (type != RoomType.PUBLIC && password != null && password.length() < 4) {
                JOptionPane.showMessageDialog(dialog, "Password must be at least 4 characters", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            btnCreate.setEnabled(false);
            statusLabel.setText("Status: Starting host services...");

            final String finalRoomName = roomName;
            final RoomType finalType = type;
            final String finalPassword = password;
            final int finalPort = port;

            new Thread(() -> {
                try {
                    final String localIP = NetworkUtils.getLocalIP();
                    System.out.println("Host setup: IP=" + localIP + ", Port=" + finalPort);

                    DatabaseManager.createRoom(username, finalRoomName, finalType, finalPassword, localIP, finalPort);

                    SessionImpl session = new SessionImpl();
                    try {
                        java.rmi.registry.LocateRegistry.createRegistry(1099);
                    } catch (java.rmi.RemoteException ex) {
                        System.out.println("RMI registry already exists");
                    }
                    Naming.rebind("rmi://" + localIP + ":1099/WhiteboardSession", session);
                    session.registerUser(username);
                    int sessionId = session.notifySessionStart();
                    System.out.println("RMI session created, ID=" + sessionId);

                    udpDiscovery.startListening(localIP, finalPort, finalRoomName, username, finalType, finalPassword);
                    System.out.println("UDP discovery listening on :8888");

                    // ...existing code...
                    final SessionImpl currentSession = session;

                    SwingUtilities.invokeLater(() -> {
                        dialog.dispose();
                        WhiteboardFrame frame = new WhiteboardFrame(true, username, sessionId);
                        frame.setVisible(true);

                        Network.SocketServer server = new Network.SocketServer();

                        // ...existing code...

                        server.setOnClientConnected(() -> {
                            frame.setAlive(true);

                            // ...existing code...
                            server.broadcast("SYNC_START");

                            System.out.println("Syncing existing drawings to client...");
                            List<String> history = DatabaseManager.getDrawCommandsForSession(sessionId);

                            for (String cmd : history) {
                                // ...existing code...
                                try {
                                    Thread.sleep(10);
                                } catch (InterruptedException ignored) {
                                }
                                server.broadcast(cmd);
                            }

                            server.broadcast("SYNC_END");
                            System.out.println("Synced " + history.size() + " commands with animation markers");
                        });

                        server.setOnClientDisconnected(() -> frame.setAlive(false));
                        server.setOnCommandReceived(frame::handleNetworkCommand);

                        frame.bindNetworkSender(server::broadcast);
                        frame.bindLocalDraw(server::broadcast); // ...existing code...

                        server.start(finalPort);
                        statusLabel.setText("Status: Hosting '" + finalRoomName + "' on " + localIP + ":" + finalPort);

                        // ...existing code...
                        frame.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowClosing(java.awt.event.WindowEvent e) {
                                System.out.println("Closing host window - notifying clients & freeing ports...");
                                new Thread(() -> {
                                    server.broadcast("HOST_CLOSED"); // Notify all clients first
                                    try {
                                        Thread.sleep(300);
                                    } catch (InterruptedException ignored) {
                                    } // ...existing code...
                                    server.shutdown();             // Closes TCP
                                    udpDiscovery.stop();           // Closes UDP
                                    try {
                                        currentSession.notifySessionEnd();
                                    } catch (Exception ignored) {
                                    }
                                    SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Ready"));
                                }).start();
                            }
                        });
                    });

                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(dialog, "Hosting failed:\n" + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        btnCreate.setEnabled(true);
                        statusLabel.setText("Status: Ready");
                    });
                    LOGGER.log(Level.SEVERE, "Failed to start hosting", ex);
                    ex.printStackTrace();
                }
            }, "Host-Setup-Thread").start();
        });

        dialog.setSize(450, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ...existing code...

    private void attemptJoin(RoomInfo room, JDialog parentDialog) {
        if (room.requiresPassword) {
            String password = JOptionPane.showInputDialog(parentDialog, "Password for '" + room.roomName + "':");
            if (password == null) return; // User cancelled

            // ...existing code...
            parentDialog.setEnabled(false);
            statusLabel.setText("Status: Verifying password...");

            new Thread(() -> {
                boolean isValid = udpDiscovery.requestJoin(room.hostIP, room.roomName, password);
                SwingUtilities.invokeLater(() -> {
                    parentDialog.setEnabled(true);
                    if (isValid) {
                        statusLabel.setText("Status: Access granted. Joining...");
                        proceedToJoin(room, parentDialog);
                    } else {
                        JOptionPane.showMessageDialog(parentDialog,
                                "Incorrect password.\nPlease try again or contact the host.",
                                "Access Denied", JOptionPane.ERROR_MESSAGE);
                        statusLabel.setText("Status: Ready");
                    }
                });
            }).start();
        } else {
            proceedToJoin(room, parentDialog);
        }
    }

    private void proceedToJoin(RoomInfo room, JDialog parentDialog) {
        statusLabel.setText("Status: Connecting to " + room.hostIP + "...");
        System.out.println("Client join: " + room.hostIP + ":" + room.socketPort);

        new Thread(() -> {
            try {
                System.out.println("RMI lookup: " + room.hostIP + ":1099");
                RemoteSession remote = (RemoteSession) Naming.lookup("rmi://" + room.hostIP + ":1099/WhiteboardSession");
                remote.registerUser(username);
                int sessionId = remote.notifySessionStart();
                System.out.println("RMI joined, session=" + sessionId);

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

                    // ...existing code...
                    final RemoteSession currentSession = remote;
                    frame.addWindowListener(new java.awt.event.WindowAdapter() {
                        @Override
                        public void windowClosing(java.awt.event.WindowEvent e) {
                            System.out.println("Closing client window - freeing ports...");
                            new Thread(() -> {
                                client.shutdown();            // Frees TCP socket
                                try {
                                    currentSession.notifySessionEnd();
                                } catch (Exception ignored) {
                                }
                                SwingUtilities.invokeLater(() -> statusLabel.setText("Status: Ready"));
                            }).start();
                        }
                    });

                    statusLabel.setText("Status: Joined '" + room.roomName + "'");
                });

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(parentDialog, "Failed to join:\n" + ex.getMessage(),
                            "Connection Error", JOptionPane.ERROR_MESSAGE);
                    parentDialog.setEnabled(true);
                    statusLabel.setText("Status: Ready");
                });
                LOGGER.log(Level.SEVERE, "Failed to join room", ex);
                ex.printStackTrace();
            }
        }, "Join-Setup-Thread").start();
    }

    // ...existing code...


    // ...existing code...
    @Override
    public void dispose() {
        if (hostSocketServer != null) hostSocketServer.shutdown();
        if (clientSocketClient != null) clientSocketClient.shutdown();
        if (udpDiscovery != null) udpDiscovery.stop();
        super.dispose();
    }

}

