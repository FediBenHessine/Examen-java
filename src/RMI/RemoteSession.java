package RMI;


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteSession extends Remote {
    void registerUser(String username) throws RemoteException;
    int notifySessionStart() throws RemoteException;
    void notifySessionEnd() throws RemoteException;
    boolean ping() throws RemoteException;
}
