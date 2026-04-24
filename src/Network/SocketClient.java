package Network;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class SocketClient {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final LinkedBlockingQueue<String> outgoingQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = false;

    private Consumer<String> onCommandReceived;
    private Runnable onConnected;
    private Runnable onDisconnected;

    public void setOnCommandReceived(Consumer<String> listener) { this.onCommandReceived = listener; }
    public void setOnConnected(Runnable listener) { this.onConnected = listener; }
    public void setOnDisconnected(Runnable listener) { this.onDisconnected = listener; }

    public boolean connect(String hostIP, int port) {
        try {
            socket = new Socket(hostIP, port);
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            running = true;
            if (onConnected != null) SwingUtilities.invokeLater(onConnected);

            new Thread(this::readLoop, "Client-Reader").start();
            new Thread(this::writeLoop, "Client-Writer").start();
            return true;
        } catch (IOException e) {
            System.err.println("❌ Client connection failed: " + e.getMessage());
            return false;
        }
    }

    private void readLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                String cmd = line.trim();
                if (cmd.isEmpty()) continue;
                System.out.println("📥 Client read: [" + cmd + "]");
                if (onCommandReceived != null) {
                    SwingUtilities.invokeLater(() -> onCommandReceived.accept(cmd));
                }
            }
        } catch (IOException e) {
            System.out.println("🔌 Client read loop ended: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void writeLoop() {
        try {
            while (running) {
                String cmd = outgoingQueue.take();
                if (out != null) {
                    out.println(cmd);
                    out.flush(); // ✅ CRITICAL: Force network transmission
                    System.out.println("📤 Client wrote: [" + cmd + "]");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void send(String command) {
        if (running && !socket.isClosed()) outgoingQueue.offer(command);
    }

    private void cleanup() {
        running = false;
        if (onDisconnected != null) SwingUtilities.invokeLater(onDisconnected);
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException ignored) {}
    }

    public void shutdown() { cleanup(); }
}