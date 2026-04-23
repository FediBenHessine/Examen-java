package IHM;


import auth.AuthService;

import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {
    private final JTextField txtUser = new JTextField(15);
    private final JPasswordField txtPass = new JPasswordField(15);
    private final JLabel lblStatus = new JLabel(" ");
    private String authenticatedUser;

    public LoginDialog(JFrame parent) {
        super(parent, "Whiteboard Login", true);
        setLayout(new GridLayout(4, 1, 5, 5));
        add(new JLabel("Username:")); add(txtUser);
        add(new JLabel("Password:")); add(txtPass);

        JPanel btnP = new JPanel();
        JButton btn = new JButton("Login");
        btnP.add(btn); add(btnP);
        add(lblStatus);

        btn.addActionListener(e -> {
            lblStatus.setText("Authenticating...");
            lblStatus.setForeground(Color.GRAY);
            new Thread(() -> {
                var res = AuthService.login(txtUser.getText(), new String(txtPass.getPassword()));
                SwingUtilities.invokeLater(() -> {
                    if (res.success()) {
                        authenticatedUser = txtUser.getText().trim();
                        dispose();
                    } else {
                        lblStatus.setText(res.message());
                        lblStatus.setForeground(Color.RED);
                    }
                });
            }).start();
        });
        setSize(260, 170);
        setLocationRelativeTo(parent);
    }
    public String getAuthenticatedUser() { return authenticatedUser; }
}