package IHM;
import Database.DatabaseManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LoginDialog extends JDialog {
    private final JTextField txtUser = new JTextField(15);
    private final JPasswordField txtPass = new JPasswordField(15);
    private final JLabel lblStatus = new JLabel(" ");
    private String authenticatedUser;

    public LoginDialog(JFrame parent) {
        super(parent, "Whiteboard Login", true);
        getContentPane().setBackground(new Color(248, 250, 252));
        setLayout(new BorderLayout(0, 0));

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(99, 102, 241));
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Whiteboard Login");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        add(headerPanel, BorderLayout.NORTH);

        // Main Content Panel
        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(new Color(248, 250, 252));
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBorder(new EmptyBorder(30, 40, 20, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Username Label and Field
        gbc.gridy = 0;
        JLabel lblUsername = new JLabel("Username");
        lblUsername.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblUsername.setForeground(new Color(75, 85, 99));
        contentPanel.add(lblUsername, gbc);

        gbc.gridy = 1;
        txtUser.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtUser.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(219, 222, 225)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        txtUser.setBackground(Color.WHITE);
        contentPanel.add(txtUser, gbc);

        // Password Label and Field
        gbc.gridy = 2;
        JLabel lblPassword = new JLabel("Password");
        lblPassword.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPassword.setForeground(new Color(75, 85, 99));
        contentPanel.add(lblPassword, gbc);

        gbc.gridy = 3;
        txtPass.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtPass.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(219, 222, 225)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        txtPass.setBackground(Color.WHITE);
        contentPanel.add(txtPass, gbc);

        // Status Label
        gbc.gridy = 4;
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblStatus.setHorizontalAlignment(JLabel.CENTER);
        contentPanel.add(lblStatus, gbc);

        // Button Panel
        gbc.gridy = 5;
        gbc.insets = new Insets(20, 0, 0, 0);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setBackground(new Color(248, 250, 252));

        JButton btnLogin = createActionButton("Login", new Color(34, 197, 94));
        JButton btnSignup = createActionButton("Sign Up", new Color(59, 130, 246));

        btnPanel.add(btnLogin);
        btnPanel.add(btnSignup);
        contentPanel.add(btnPanel, gbc);

        add(contentPanel, BorderLayout.CENTER);

        btnLogin.addActionListener(e -> authenticate());
        btnSignup.addActionListener(e -> showSignupDialog());
        setSize(420, 450);
        setLocationRelativeTo(parent);
    }

    private JButton createActionButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(
                    Math.max(0, bgColor.getRed() - 20),
                    Math.max(0, bgColor.getGreen() - 20),
                    Math.max(0, bgColor.getBlue() - 20)
                ));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        return button;
    }

    private void authenticate() {
        String username = txtUser.getText().trim();
        String password = new String(txtPass.getPassword());

        // Basic validation
        if (username.isEmpty() || password.isEmpty()) {
            lblStatus.setText("Error: Please enter both username and password");
            lblStatus.setForeground(new Color(239, 68, 68));
            return;
        }

        lblStatus.setText("Authenticating...");
        lblStatus.setForeground(new Color(107, 114, 128));

        new Thread(() -> {
            try {
                var res = auth.AuthService.login(username, password);
                SwingUtilities.invokeLater(() -> {
                    if (res.success()) {
                        authenticatedUser = username;
                        lblStatus.setText("Login successful!");
                        lblStatus.setForeground(new Color(34, 197, 94));
                        // Small delay to show success message
                        new Thread(() -> {
                            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                            SwingUtilities.invokeLater(this::dispose);
                        }).start();
                    } else {
                        lblStatus.setText("Error: " + res.message());
                        lblStatus.setForeground(new Color(239, 68, 68));
                        txtPass.setText(""); // Clear password on failure
                        txtPass.requestFocus();
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("Error: Connection failed. Please try again.");
                    lblStatus.setForeground(new Color(239, 68, 68));
                });
            }
        }).start();
    }

    private void showSignupDialog() {
        JDialog dlg = new JDialog(this, "Create Account", true);
        dlg.getContentPane().setBackground(new Color(248, 250, 252));
        dlg.setLayout(new BorderLayout(0, 0));

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(59, 130, 246));
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Create New Account");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        dlg.add(headerPanel, BorderLayout.NORTH);

        // Content Panel
        JPanel contentPanel = new JPanel();
        contentPanel.setBackground(new Color(248, 250, 252));
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JTextField u = new JTextField();
        JPasswordField p = new JPasswordField();
        JPasswordField pConfirm = new JPasswordField();

        // Username
        gbc.gridy = 0;
        JLabel lbl1 = new JLabel("Username");
        lbl1.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl1.setForeground(new Color(75, 85, 99));
        contentPanel.add(lbl1, gbc);

        gbc.gridy = 1;
        u.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        u.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(219, 222, 225)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        u.setBackground(Color.WHITE);
        contentPanel.add(u, gbc);

        // Password
        gbc.gridy = 2;
        JLabel lbl2 = new JLabel("Password");
        lbl2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl2.setForeground(new Color(75, 85, 99));
        contentPanel.add(lbl2, gbc);

        gbc.gridy = 3;
        p.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(219, 222, 225)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        p.setBackground(Color.WHITE);
        contentPanel.add(p, gbc);

        // Confirm Password
        gbc.gridy = 4;
        JLabel lbl3 = new JLabel("Confirm Password");
        lbl3.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl3.setForeground(new Color(75, 85, 99));
        contentPanel.add(lbl3, gbc);

        gbc.gridy = 5;
        pConfirm.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        pConfirm.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(219, 222, 225)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        pConfirm.setBackground(Color.WHITE);
        contentPanel.add(pConfirm, gbc);

        // Button Panel
        gbc.gridy = 6;
        gbc.insets = new Insets(16, 0, 0, 0);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setBackground(new Color(248, 250, 252));

        JButton btnRegister = createActionButton("Register", new Color(34, 197, 94));
        JButton btnCancel = createActionButton("Cancel", new Color(107, 114, 128));

        btnPanel.add(btnRegister);
        btnPanel.add(btnCancel);
        contentPanel.add(btnPanel, gbc);

        dlg.add(contentPanel, BorderLayout.CENTER);

        btnCancel.addActionListener(e -> dlg.dispose());

        btnRegister.addActionListener(e -> {
            String username = u.getText().trim();
            String password = new String(p.getPassword());
            String confirmPassword = new String(pConfirm.getPassword());

            // Validation
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Username and password cannot be empty", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (username.length() < 3) {
                JOptionPane.showMessageDialog(dlg, "Username must be at least 3 characters", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (password.length() < 6) {
                JOptionPane.showMessageDialog(dlg, "Password must be at least 6 characters", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(dlg, "Passwords do not match", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Attempt registration
            btnRegister.setEnabled(false);
            btnRegister.setText("Registering...");

            new Thread(() -> {
                boolean success = DatabaseManager.signUp(username, password, "CLIENT");
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        JOptionPane.showMessageDialog(dlg, "Account created successfully!\nPlease login with your new credentials.", "Success", JOptionPane.INFORMATION_MESSAGE);
                        dlg.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dlg, "Username already exists or registration failed.\nPlease try a different username.", "Registration Failed", JOptionPane.ERROR_MESSAGE);
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Register");
                    }
                });
            }).start();
        });

        dlg.setSize(380, 420);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }


    public String getAuthenticatedUser() { return authenticatedUser; }
}