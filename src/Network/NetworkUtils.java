package Network;



import java.net.*;
import java.util.*;

public class NetworkUtils {
    /**
     * Get the best local IP address for LAN communication.
     * Prefers IPv4, non-loopback, non-link-local, active interfaces.
     */
    public static String getLocalIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            List<InetAddress> candidates = new ArrayList<>();

            for (NetworkInterface iface : Collections.list(interfaces)) {
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;

                for (InetAddress addr : Collections.list(iface.getInetAddresses())) {
                    if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress()) continue;

                    // Prefer IPv4 for simplicity
                    if (addr instanceof Inet4Address) {
                        return addr.getHostAddress();
                    }
                    candidates.add(addr);
                }
            }

            // Fallback to first candidate or localhost
            if (!candidates.isEmpty()) return candidates.get(0).getHostAddress();
            return InetAddress.getLocalHost().getHostAddress();

        } catch (SocketException | UnknownHostException e) {
            System.err.println("⚠️ IP detection failed: " + e.getMessage());
            return "127.0.0.1";
        }
    }
}