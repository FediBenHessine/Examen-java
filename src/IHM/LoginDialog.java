package IHM;
import Database.DatabaseManager;
import javax.swing.*;
import java.awt.*;

public class LoginDialog extends JDialog {
    private JTextField txtUser = new JTextField(15);
    private JPasswordField txtPass = new JPasswordField(15);
    private JLabel lblStatus = new JLabel(" ");
    private String authenticatedUser;

    public LoginDialog(JFrame parent) {
        super(parent, "Whiteboard Login", true);
        setLayout(new GridLayout(5, 1, 5, 5));
        add(new JLabel("Username:")); add(txtUser);
        add(new JLabel("Password:")); add(txtPass);

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton btnLogin = new JButton("Login");
        JButton btnSignup = new JButton("Sign Up");
        btnPanel.add(btnLogin); btnPanel.add(btnSignup);
        add(btnPanel);
        add(lblStatus);

        btnLogin.addActionListener(e -> authenticate());
        btnSignup.addActionListener(e -> showSignupDialog());
        setSize(280, 190);
        setLocationRelativeTo(parent);
    }

    private void authenticate() {
        lblStatus.setText("Authenticating...");
        lblStatus.setForeground(Color.GRAY);
        new Thread(() -> {
            var res = auth.AuthService.login(txtUser.getText(), new String(txtPass.getPassword()));
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
    }

    private void showSignupDialog() {
        JDialog dlg = new JDialog(this, "Create Account", true);
        dlg.setLayout(new GridLayout(4, 1, 5, 5));
        JTextField u = new JTextField(); JPasswordField p = new JPasswordField();
        dlg.add(new JLabel("Username:")); dlg.add(u);
        dlg.add(new JLabel("Password:")); dlg.add(p);
        JButton btn = new JButton("Register");
        dlg.add(btn);
        btn.addActionListener(e -> {
            if (DatabaseManager.signUp(u.getText(), new String(p.getPassword()), "USER")) {
                JOptionPane.showMessageDialog(dlg, "✅ Account created! Please login.");
                dlg.dispose();
            } else {
                JOptionPane.showMessageDialog(dlg, "❌ Username exists or DB error");
            }
        });
        dlg.setSize(250, 150); dlg.setLocationRelativeTo(this); dlg.setVisible(true);
    }

    public String getAuthenticatedUser() { return authenticatedUser; }
}