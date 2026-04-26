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

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//        btnPanel.setPreferredSize(new Dimension(320, 40));
        JButton btnLogin = new JButton("Login");
        JButton btnSignup = new JButton("Sign Up");
        btnPanel.add(btnLogin); btnPanel.add(btnSignup);
        add(btnPanel);
        add(lblStatus);

        btnLogin.addActionListener(e -> authenticate());
        btnSignup.addActionListener(e -> showSignupDialog());
        setSize(340, 250);
        setLocationRelativeTo(parent);
    }

    private void authenticate() {
        String username = txtUser.getText().trim();
        String password = new String(txtPass.getPassword());

        // Basic validation
        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("❌ Please enter both username and password");
            lblStatus.setForeground(Color.RED);
            return;
        }

        lblStatus.setText("🔐 Authenticating...");
        lblStatus.setForeground(Color.GRAY);

        new Thread(() -> {
            try {
                var res = auth.AuthService.login(username, password);
                SwingUtilities.invokeLater(() -> {
                    if (res.success()) {
                        authenticatedUser = username;
                        lblStatus.setText("✅ Login successful!");
                        lblStatus.setForeground(Color.GREEN);
                        // Small delay to show success message
                        new Thread(() -> {
                            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                            SwingUtilities.invokeLater(this::dispose);
                        }).start();
                    } else {
                        lblStatus.setText("❌ " + res.message());
                        lblStatus.setForeground(Color.RED);
                        txtPass.setText(""); // Clear password on failure
                        txtPass.requestFocus();
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("❌ Connection error. Please try again.");
                    lblStatus.setForeground(Color.RED);
                });
            }
        }).start();
    }

    private void showSignupDialog() {
        JDialog dlg = new JDialog(this, "Create Account", true);
        dlg.setLayout(new GridLayout(5, 1, 5, 5));

        JTextField u = new JTextField();
        JPasswordField p = new JPasswordField();
        JPasswordField pConfirm = new JPasswordField();

        dlg.add(new JLabel("Username:")); dlg.add(u);
        dlg.add(new JLabel("Password:")); dlg.add(p);
        dlg.add(new JLabel("Confirm Password:")); dlg.add(pConfirm);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnRegister = new JButton("Register");
        JButton btnCancel = new JButton("Cancel");
        btnPanel.add(btnRegister); btnPanel.add(btnCancel);
        dlg.add(btnPanel);

        btnCancel.addActionListener(e -> dlg.dispose());

        btnRegister.addActionListener(e -> {
            String username = u.getText().trim();
            String password = new String(p.getPassword());
            String confirmPassword = new String(pConfirm.getPassword());

            // ✅ Input validation
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "❌ Username and password cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (username.length() < 3) {
                JOptionPane.showMessageDialog(dlg, "❌ Username must be at least 3 characters", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (password.length() < 6) {
                JOptionPane.showMessageDialog(dlg, "❌ Password must be at least 6 characters", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(dlg, "❌ Passwords do not match", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // ✅ Attempt registration
            btnRegister.setEnabled(false);
            btnRegister.setText("Registering...");

            new Thread(() -> {
                boolean success = DatabaseManager.signUp(username, password, "CLIENT");
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(dlg, "✅ Account created successfully!\nPlease login with your new credentials.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dlg.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dlg, "❌ Username already exists or registration failed.\nPlease try a different username.", "Registration Failed", JOptionPane.ERROR_MESSAGE);
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Register");
                    }
                });
            }).start();
        });

        dlg.setSize(300, 200);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    public String getAuthenticatedUser() { return authenticatedUser; }
}