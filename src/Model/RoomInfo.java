package Model;
import java.io.Serializable;

public class RoomInfo implements Serializable {
    public String roomName;
    public String hostUsername;
    public String hostIP;
    public int socketPort;
    public RoomType roomType; // PUBLIC, PUBLIC_PASSWORD, PRIVATE
    public boolean requiresPassword;

    public RoomInfo() {} // For serialization

    public RoomInfo(String roomName, String hostUsername, String hostIP,
                    int socketPort, RoomType roomType, boolean requiresPassword) {
        this.roomName = roomName;
        this.hostUsername = hostUsername;
        this.hostIP = hostIP;
        this.socketPort = socketPort;
        this.roomType = roomType;
        this.requiresPassword = requiresPassword;
    }

    @Override
    public String toString() {
        return roomName + " (Host: " + hostUsername + ") - " + roomType;
    }
}