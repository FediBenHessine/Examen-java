
import IHM.LoginDialog;
import IHM.WhiteboardFrame;
import Network.SocketClient;
import Network.SocketServer;
import RMI.RemoteSession;
import RMI.SessionImpl;

import javax.swing.*;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class Main {
    private static final String ROLE = System.getProperty("app.role", "HOST");
    private static final String HOST_IP = System.getProperty("app.host_ip", "192.168.1.10");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame dummy = new JFrame(); dummy.setUndecorated(true);
            LoginDialog login = new LoginDialog(dummy);
            login.setVisible(true);
            String username = login.getAuthenticatedUser();
            if (username == null) { dummy.dispose(); return; }

            try {
                int sessionId = (ROLE.equals("HOST")) ? setupHost(username) : setupClient(username);
                boolean isHost = ROLE.equals("HOST");
                WhiteboardFrame frame = new WhiteboardFrame(isHost, username, sessionId);
                frame.setVisible(true);

                // Wire Network Layer
                if (isHost) {
                    SocketServer server = new SocketServer();
                    server.setOnClientConnected(() -> frame.updateStatus("Status: ✅ Client Connected"));
                    server.setOnClientDisconnected(() -> frame.setAlive(false));
                    server.setOnCommandReceived(cmd -> frame.handleNetworkCommand(cmd));
                    server.start(8083);
                    frame.updateStatus("Status: ⏳ Waiting for client on :8080...");
                    frame.bindLocalDraw(server::send);
                } else {
                    SocketClient client = new SocketClient();
                    client.setOnConnected(() -> frame.setAlive(true));
                    client.setOnDisconnected(() -> frame.setAlive(false));
                    client.setOnCommandReceived(cmd -> frame.handleNetworkCommand(cmd));
                    client.connect(HOST_IP, 8083);
                    frame.bindLocalDraw(client::send);

                    // Client-side heartbeat
                    new Thread(() -> {
                        while(true) {
                            try {
                                client.send("PING");
                                Thread.sleep(2000);
                            } catch (InterruptedException e) { break; }
                        }
                    }, "Client-Heartbeat").start();
                }

                dummy.dispose();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Setup Failed:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static int setupHost(String username) throws Exception {
        LocateRegistry.createRegistry(1099);
        SessionImpl session = new SessionImpl();
        Naming.rebind("rmi://localhost:1099/WhiteboardSession", session);
        session.registerUser(username);
        session.notifySessionStart();
        return -1; // Host session ID tracked internally
    }

    private static int setupClient(String username) throws Exception {
        RemoteSession remote = (RemoteSession) Naming.lookup("rmi://" + HOST_IP + ":1099/WhiteboardSession");
        remote.registerUser(username);
        remote.notifySessionStart();
        return -1;
    }
}