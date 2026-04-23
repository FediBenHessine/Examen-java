package RMI;


import Database.DatabaseManager;

import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.net.InetAddress;

public class SessionImpl extends UnicastRemoteObject implements RemoteSession {
    private volatile String currentUser;
    private volatile int currentSessionId = -1;
    private volatile boolean isActive = false;

    public SessionImpl() throws RemoteException { super(); }

    @Override
    public synchronized void registerUser(String username) throws RemoteException {
        if (username == null || username.trim().isEmpty())
            throw new RemoteException("Username cannot be empty");

        // Verify user exists in DB
        if (!DatabaseManager.userExists(username.trim()))
            throw new RemoteException("User not found in database");

        this.currentUser = username.trim();
    }

    @Override
    public synchronized void notifySessionStart() throws RemoteException {
        if (currentUser == null) throw new RemoteException("No user registered. Call registerUser first.");
        if (isActive) throw new RemoteException("Session already active");

        String hostIP = null;
        try {
            hostIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        currentSessionId = DatabaseManager.createSession(hostIP, currentUser);
        if (currentSessionId <= 0) throw new RemoteException("Failed to create session in database");

        isActive = true;
    }

    @Override
    public synchronized void notifySessionEnd() throws RemoteException {
        if (isActive && currentSessionId > 0) {
            DatabaseManager.closeSession(currentSessionId);
        }
        isActive = false;
        currentSessionId = -1;
        currentUser = null;
    }

    @Override
    public boolean ping() throws RemoteException {
        return isActive;
    }
}