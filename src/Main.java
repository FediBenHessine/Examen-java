
import IHM.LoginDialog;
import IHM.DashboardFrame;
import Database.DatabaseManager;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 1. Login/Signup flow
            JFrame dummy = new JFrame();
            dummy.setUndecorated(true);
            dummy.setAlwaysOnTop(true);

            LoginDialog login = new LoginDialog(dummy);
            login.setVisible(true);

            String username = login.getAuthenticatedUser();
            if (username == null) {
                dummy.dispose();
                return; // User cancelled
            }

            // 2. Launch Dashboard (no role passed - user chooses at runtime)
            DashboardFrame dashboard = new DashboardFrame(username);
            dashboard.setVisible(true);

            dummy.dispose();
        });
    }
}