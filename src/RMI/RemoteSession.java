package RMI;


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteSession extends Remote {
    void registerUser(String username) throws RemoteException;
    int notifySessionStart() throws RemoteException;
    void notifySessionEnd() throws RemoteException;
    boolean ping() throws RemoteException;

    // Database operations that should happen on host
    void insertDrawCommand(int sessionId, Model.DrawCommand cmd) throws RemoteException;
    java.util.List<String> getDrawCommandsForSession(int sessionId) throws RemoteException;
}
