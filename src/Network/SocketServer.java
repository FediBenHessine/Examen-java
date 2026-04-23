package Network;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class SocketServer {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private final LinkedBlockingQueue<String> outgoingQueue = new LinkedBlockingQueue<>();
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
                System.out.println("🟢 Socket Server listening on port " + port);
                clientSocket = serverSocket.accept();
                running = true;
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                if (onClientConnected != null) SwingUtilities.invokeLater(onClientConnected);
                new Thread(this::readLoop, "Server-Reader").start();
                new Thread(this::writeLoop, "Server-Writer").start();
            } catch (IOException e) {
                System.err.println("❌ Socket Server failed: " + e.getMessage());
            }
        }, "Server-Acceptor").start();
    }

    private void readLoop() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                String cmd = line.trim();
                if (onCommandReceived != null) {
                    SwingUtilities.invokeLater(() -> onCommandReceived.accept(cmd));
                }
            }
        } catch (IOException e) {
            System.out.println("🔌 Client disconnected from Server");
        } finally {
            running = false;
            if (onClientDisconnected != null) SwingUtilities.invokeLater(onClientDisconnected);
        }
    }

    private void writeLoop() {
        try {
            while (running) {
                String cmd = outgoingQueue.take();
                if (out != null && !out.checkError()) out.println(cmd);
            }
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    public void send(String command) { outgoingQueue.offer(command); }
    public void shutdown() { running = false; try { if(clientSocket != null) clientSocket.close(); } catch(IOException ignored){} }
}