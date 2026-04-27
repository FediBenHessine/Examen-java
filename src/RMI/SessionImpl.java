package RMI;


import Database.DatabaseManager;

import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class SessionImpl extends UnicastRemoteObject implements RemoteSession {
    private volatile String hostUser;  // The user who created/hosts this session
    private volatile int sessionId = -1;
    private volatile boolean isHostActive = false;
    private final Set<String> activeUsers = new HashSet<>();  // All users in session
    private volatile String lastRegisteredUser;  // Track most recently registered user

    public SessionImpl() throws RemoteException { super(); }

    @Override
    public synchronized void registerUser(String username) throws RemoteException {
        if (username == null || username.trim().isEmpty())
            throw new RemoteException("Username cannot be empty");

        // Verify user exists in DB
//        if (!DatabaseManager.userExists(username.trim()))
//            throw new RemoteException("User not found in database");

        // Track this user as the one about to call notifySessionStart
        this.lastRegisteredUser = username.trim();
        activeUsers.add(username.trim());
    }

    @Override
    public synchronized int notifySessionStart() throws RemoteException {
        if (lastRegisteredUser == null)
            throw new RemoteException("No user registered. Call registerUser first.");

        // First user becomes the host and creates the session
        if (hostUser == null) {
            hostUser = lastRegisteredUser;

            String hostIP = null;
            try {
                hostIP = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            sessionId = DatabaseManager.createSession(hostIP, hostUser);
            if (sessionId <= 0) throw new RemoteException("Failed to create session in database");
            isHostActive = true;
            System.out.println("✅ Host '" + hostUser + "' created session " + sessionId);
        } else {
            // Subsequent calls just log the join
            System.out.println("✅ User '" + lastRegisteredUser + "' joined session " + sessionId);
        }

        return sessionId;
    }

    @Override
    public synchronized void notifySessionEnd() throws RemoteException {
        if (isHostActive && sessionId > 0) {
            DatabaseManager.closeSession(sessionId);
            System.out.println("🔴 Session " + sessionId + " closed by host");
        }
        isHostActive = false;
        sessionId = -1;
        hostUser = null;
        activeUsers.clear();
    }

    @Override
    public boolean ping() throws RemoteException {
        return isHostActive && !activeUsers.isEmpty();
    }

    @Override
    public void insertDrawCommand(int sessionId, Model.DrawCommand cmd) throws RemoteException {
        DatabaseManager.insertDrawCommand(sessionId, cmd);
    }

    @Override
    public java.util.List<String> getDrawCommandsForSession(int sessionId) throws RemoteException {
        return DatabaseManager.getDrawCommandsForSession(sessionId);
    }
}