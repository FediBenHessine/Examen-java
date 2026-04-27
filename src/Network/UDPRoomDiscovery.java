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
                        // Parse join request: JOIN_REQUEST|roomName|password
                        String[] parts = request.split("\\|");
                        String requestedRoomName = parts.length > 1 ? parts[1] : "";
                        String providedPassword = parts.length > 2 ? parts[2] : "";

                        // Validate room name and password against host's database
                        boolean isValidJoin = false;
                        if (roomName.equals(requestedRoomName)) {
                            if (roomType == Model.RoomType.PUBLIC) {
                                // Public rooms don't require password
                                isValidJoin = true;
                            } else {
                                // Password-protected rooms: validate against stored password
                                isValidJoin = (password != null && password.equals(providedPassword));
                            }
                        }

                        if (isValidJoin) {
                            if (onDiscoveryRequest != null) {
                                onDiscoveryRequest.accept(packet.getAddress().getHostAddress());
                            }
                            // Send JOIN_ACCEPTED response
                            byte[] responseData = "JOIN_ACCEPTED".getBytes();
                            DatagramPacket responsePacket = new DatagramPacket(
                                    responseData, responseData.length, packet.getAddress(), packet.getPort());
                            socket.send(responsePacket);
                            System.out.println("✅ Join request accepted for room '" + roomName + "' from " + packet.getAddress().getHostAddress());
                        } else {
                            // Send JOIN_REJECTED response
                            byte[] responseData = "JOIN_REJECTED".getBytes();
                            DatagramPacket responsePacket = new DatagramPacket(
                                    responseData, responseData.length, packet.getAddress(), packet.getPort());
                            socket.send(responsePacket);
                            System.out.println("❌ Join request rejected for room '" + roomName + "' from " + packet.getAddress().getHostAddress());
                        }
                    } else if ("DIRECT_JOIN".equals(request.split("\\|")[0])) {
                        // Parse direct join request: DIRECT_JOIN|password
                        String[] parts = request.split("\\|");
                        String providedPassword = parts.length > 1 ? parts[1] : "";

                        // Validate password against host's room
                        boolean isValidJoin = false;
                        if (roomType == Model.RoomType.PUBLIC) {
                            // Public rooms don't require password
                            isValidJoin = true;
                        } else {
                            // Password-protected rooms: validate against stored password
                            isValidJoin = (password != null && password.equals(providedPassword));
                        }

                        if (isValidJoin) {
                            if (onDiscoveryRequest != null) {
                                onDiscoveryRequest.accept(packet.getAddress().getHostAddress());
                            }
                            // Send room info in response
                            RoomInfo info = new RoomInfo(roomName, username, hostIP, socketPort,
                                    roomType, roomType != Model.RoomType.PUBLIC && password != null);
                            String response = "DIRECT_JOIN_ACCEPTED|" + gson.toJson(info);
                            byte[] responseData = response.getBytes();
                            DatagramPacket responsePacket = new DatagramPacket(
                                    responseData, responseData.length, packet.getAddress(), packet.getPort());
                            socket.send(responsePacket);
                            System.out.println("✅ Direct join accepted for room '" + roomName + "' from " + packet.getAddress().getHostAddress());
                        } else {
                            // Send DIRECT_JOIN_REJECTED response
                            byte[] responseData = "DIRECT_JOIN_REJECTED".getBytes();
                            DatagramPacket responsePacket = new DatagramPacket(
                                    responseData, responseData.length, packet.getAddress(), packet.getPort());
                            socket.send(responsePacket);
                            System.out.println("❌ Direct join rejected for room '" + roomName + "' from " + packet.getAddress().getHostAddress());
                        }
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
            if ("JOIN_ACCEPTED".equals(result)) {
                return true;
            } else if ("JOIN_REJECTED".equals(result)) {
                System.out.println("❌ Join request rejected by host");
                return false;
            } else {
                System.err.println("❌ Unexpected response from host: " + result);
                return false;
            }
        } catch (IOException e) {
            System.err.println("❌ Join request failed: " + e.getMessage());
            return false;
        }
    }

    // === CLIENT MODE: Direct join without knowing room name ===
    public RoomInfo requestDirectJoin(String hostIP, String password) {
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(2000);

            String request = "DIRECT_JOIN|" + password;
            byte[] requestData = request.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    requestData, requestData.length,
                    InetAddress.getByName(hostIP), DISCOVERY_PORT);
            socket.send(packet);

            // Wait for response
            byte[] buffer = new byte[4096];
            DatagramPacket response = new DatagramPacket(buffer, buffer.length);
            socket.receive(response);

            String result = new String(response.getData(), 0, response.getLength()).trim();
            socket.close();
            
            if (result.startsWith("DIRECT_JOIN_ACCEPTED|")) {
                // Parse room info from response
                String json = result.substring("DIRECT_JOIN_ACCEPTED|".length());
                return gson.fromJson(json, RoomInfo.class);
            } else if ("DIRECT_JOIN_REJECTED".equals(result)) {
                System.out.println("❌ Direct join request rejected by host");
                return null;
            } else {
                System.err.println("❌ Unexpected response from host: " + result);
                return null;
            }
        } catch (IOException e) {
            System.err.println("❌ Direct join request failed: " + e.getMessage());
            return null;
        }
    }

    public void stop() { running = false; if (socket != null && !socket.isClosed()) socket.close(); }
}