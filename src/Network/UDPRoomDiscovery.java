package Network;

import Model.RoomInfo;
import com.google.gson.Gson; // Add Gson dependency for JSON serialization
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UDPRoomDiscovery {
    private static final int DISCOVERY_PORT = 8888;
    private static final int BROADCAST_TIMEOUT = 3000; // 3 seconds
    private static final String BROADCAST_ADDRESS = "255.255.255.255";

    private DatagramSocket socket;
    private volatile boolean running = false;
    private final Gson gson = new Gson();

    // Callbacks
    private Consumer<RoomInfo> onRoomDiscovered;
    private Consumer<String> onDiscoveryRequest;
    private Runnable onError;

    public void setOnRoomDiscovered(Consumer<RoomInfo> listener) { this.onRoomDiscovered = listener; }
    public void setOnDiscoveryRequest(Consumer<String> listener) { this.onDiscoveryRequest = listener; }
    public void setOnError(Runnable listener) { this.onError = listener; }

    // === HOST MODE: Listen for discovery requests ===
    public void startListening(String hostIP, int socketPort, String roomName,
                               String username, Model.RoomType roomType, String password) {
        new Thread(() -> {
            try {
                socket = new DatagramSocket(DISCOVERY_PORT);
                socket.setBroadcast(true);
                socket.setReuseAddress(true); // Allow socket reuse
                running = true;
                byte[] buffer = new byte[4096];

                System.out.println("🟢 Host listening for room discovery on UDP :" + DISCOVERY_PORT);

                while (running) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet); // Blocks until request received

                    String request = new String(packet.getData(), 0, packet.getLength()).trim();
                    System.out.println("📨 Received discovery request: " + request + " from " + packet.getAddress().getHostAddress());
                    
                    if ("DISCOVER_ROOMS".equals(request)) {
                        // ✅ Only respond to discovery if room is not PRIVATE
                        if (roomType != Model.RoomType.PRIVATE) {
                            // Build room info response
                            RoomInfo info = new RoomInfo(roomName, username, hostIP, socketPort,
                                    roomType, roomType != Model.RoomType.PUBLIC && password != null);
                            String response = "ROOM_FOUND|" + gson.toJson(info);

                            // Send unicast response back to requester
                            byte[] responseData = response.getBytes();
                            DatagramPacket responsePacket = new DatagramPacket(
                                    responseData, responseData.length, packet.getAddress(), packet.getPort());
                            socket.send(responsePacket);
                            System.out.println("✅ Sent room info to " + packet.getAddress().getHostAddress());
                        } else {
                            System.out.println("🔒 Private room - not responding to discovery request from " + packet.getAddress().getHostAddress());
                        }
                    } else if ("JOIN_REQUEST".equals(request.split("\\|")[0])) {
                        if (onDiscoveryRequest != null) {
                            onDiscoveryRequest.accept(packet.getAddress().getHostAddress());
                        }
                        // Send JOIN_ACCEPTED response
                        byte[] responseData = "JOIN_ACCEPTED".getBytes();
                        DatagramPacket responsePacket = new DatagramPacket(
                                responseData, responseData.length, packet.getAddress(), packet.getPort());
                        socket.send(responsePacket);
                    }
                }
            } catch (IOException e) {
                if (running && onError != null) {
                    System.err.println("❌ UDP Discovery Error: " + e.getMessage());
                    e.printStackTrace();
                    onError.run();
                }
            } finally {
                if (socket != null && !socket.isClosed()) socket.close();
            }
        }, "UDP-Discovery-Listener").start();
    }

    // === CLIENT MODE: Broadcast discovery request ===
    public List<RoomInfo> discoverRooms() {
        List<RoomInfo> discovered = new ArrayList<>();
        DatagramSocket tempSocket = null;
        try {
            tempSocket = new DatagramSocket();
            tempSocket.setBroadcast(true);
            tempSocket.setSoTimeout(BROADCAST_TIMEOUT);
            tempSocket.setReuseAddress(true);

            // Broadcast discovery request
            String request = "DISCOVER_ROOMS";
            byte[] requestData = request.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    requestData, requestData.length,
                    InetAddress.getByName(BROADCAST_ADDRESS), DISCOVERY_PORT);
            System.out.println("📡 Broadcasting discovery request to " + BROADCAST_ADDRESS + ":" + DISCOVERY_PORT);
            tempSocket.send(packet);

            // Listen for responses (with timeout)
            byte[] buffer = new byte[4096];
            long startTime = System.currentTimeMillis();

            while (System.currentTimeMillis() - startTime < BROADCAST_TIMEOUT) {
                try {
                    DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                    tempSocket.receive(response);

                    String data = new String(response.getData(), 0, response.getLength()).trim();
                    System.out.println("📨 Received response: " + data.substring(0, Math.min(50, data.length())) + "...");
                    
                    if (data.startsWith("ROOM_FOUND|")) {
                        String json = data.substring("ROOM_FOUND|".length());
                        RoomInfo info = gson.fromJson(json, RoomInfo.class);
                        discovered.add(info);
                        System.out.println("✅ Found room: " + info.roomName);
                        if (onRoomDiscovered != null) {
                            javax.swing.SwingUtilities.invokeLater(() -> onRoomDiscovered.accept(info));
                        }
                    }
                } catch (SocketTimeoutException e) {
                    break; // No more responses
                }
            }
            System.out.println("✅ Discovery completed. Found " + discovered.size() + " room(s)");
        } catch (IOException e) {
            if (!e.getMessage().contains("Socket closed")) { // Ignore intentional closure
                System.err.println("❌ Discovery failed: " + e.getMessage());
            }
            if (onError != null) javax.swing.SwingUtilities.invokeLater(onError);
        } finally {
            if (tempSocket != null && !tempSocket.isClosed()) {
                tempSocket.close();
            }
        }
        return discovered;
    }

    // === CLIENT MODE: Direct join (for private rooms) ===
    public boolean requestJoin(String hostIP, String roomName, String password) {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(2000);

            String request = "JOIN_REQUEST|" + roomName + "|" + password;
            byte[] requestData = request.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    requestData, requestData.length,
                    InetAddress.getByName(hostIP), DISCOVERY_PORT);
            socket.send(packet);

            // Wait for response
            byte[] buffer = new byte[1024];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);

            String result = new String(response.getData(), 0, response.getLength()).trim();
            socket.close();
            return "JOIN_ACCEPTED".equals(result);
        } catch (IOException e) {
            System.err.println("❌ Join request failed: " + e.getMessage());
            return false;
        }
    }

    public void stop() { running = false; if (socket != null && !socket.isClosed()) socket.close(); }
}