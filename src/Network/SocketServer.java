package Network;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class SocketServer {
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private volatile boolean running = false;

    private Consumer<String> onCommandReceived;
    private Runnable onClientConnected;
    private Runnable onClientDisconnected;

    public void setOnCommandReceived(Consumer<String> listener) { this.onCommandReceived = listener; }
    public void setOnClientConnected(Runnable listener) { this.onClientConnected = listener; }
    public void setOnClientDisconnected(Runnable listener) { this.onClientDisconnected = listener; }

    public void start(int port) {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                running = true;
                System.out.println("🟢 Socket Server listening on port " + port);
                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    clientSocket.setTcpNoDelay(true);
                    clientSocket.setKeepAlive(true);
                    ClientHandler handler = new ClientHandler(clientSocket);
                    clients.add(handler);
                    threadPool.execute(handler);
                    System.out.println("👥 New client connected. Total: " + clients.size());
                    if (onClientConnected != null) SwingUtilities.invokeLater(onClientConnected);
                }
            } catch (IOException e) {
                if (running) System.err.println("❌ Server accept failed: " + e.getMessage());
            }
        }, "Server-Acceptor").start();
    }

    // ✅ Broadcast to ALL connected clients
    public void broadcast(String command) {
        for (ClientHandler client : clients) {
            client.send(command);
        }
    }

    // ✅ Send to specific client (used internally)
    private void sendToClient(ClientHandler handler, String command) {
        handler.send(command);
    }

    public void shutdown() {
        running = false;
        try { if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close(); } catch (IOException ignored) {}
        for (ClientHandler c : clients) c.close();
        threadPool.shutdownNow();
        if (onClientDisconnected != null) SwingUtilities.invokeLater(onClientDisconnected);
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private volatile boolean active = true;

        public ClientHandler(Socket socket) { this.socket = socket; }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                String line;
                while (active && (line = in.readLine()) != null) {
                    String cmd = line.trim();
                    if (cmd.isEmpty()) continue;
                    System.out.println("📥 Host received from client: " + cmd);

                    if (onCommandReceived != null) {
                        SwingUtilities.invokeLater(() -> onCommandReceived.accept(cmd));
                    }

                    // ✅ Auto-broadcast drawing commands to OTHER clients
                    if (!cmd.equals("PING") && !cmd.equals("PONG")) {
                        broadcast(cmd);
                    }
                }
            } catch (IOException e) {
                System.out.println("🔌 Client disconnected from host");
            } finally {
                clients.remove(this);
                if (onClientDisconnected != null) SwingUtilities.invokeLater(onClientDisconnected);
                close();
            }
        }

        public void send(String command) {
            if (active && out != null) {
                out.println(command);
                out.flush();
            }
        }

        public void close() {
            active = false;
            try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
        }
    }
}