package views;

import controllers.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;
import models.User;

public class AuthViews {

    // --- LOGIN PANEL ---
    public static class LoginPanel extends JPanel {
        private MainApp mainApp;
        private JTextField userField;
        private JPasswordField passField;
        private JButton loginBtn, goToRegisterBtn;
        private JLabel statusLabel;

       public LoginPanel(MainApp app) {
            this.mainApp = app;
            setLayout(new GridBagLayout());
            setBackground(Color.WHITE); // Mobile apps usually have white/clean backgrounds
            
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 20, 10, 20); // Add side padding like a phone screen
            gbc.fill = GridBagConstraints.HORIZONTAL; // Make inputs stretch
            gbc.gridx = 0;
            
            // 1. APP NAME (EazyBus)
            JLabel appName = new JLabel("EazyBus", SwingConstants.CENTER);
            appName.setFont(new Font("SansSerif", Font.BOLD, 40));
            appName.setForeground(new Color(50, 150, 250)); // Nice App Blue
            gbc.gridy = 0;
            gbc.insets = new Insets(50, 20, 10, 20); // Extra top padding
            add(appName, gbc);

            // 2. "Log in" Sub-header
            JLabel subTitle = new JLabel("Log in", SwingConstants.CENTER);
            subTitle.setFont(new Font("SansSerif", Font.PLAIN, 24));
            subTitle.setForeground(Color.GRAY);
            gbc.gridy = 1;
            gbc.insets = new Insets(0, 20, 40, 20); // Gap below title
            add(subTitle, gbc);

            // 3. Username Field
            gbc.insets = new Insets(5, 20, 5, 20); // Reset padding
            gbc.gridy = 2;
            add(new JLabel("Username"), gbc);
            
            userField = new JTextField(20);
            userField.setFont(new Font("SansSerif", Font.PLAIN, 16)); // Larger text for mobile
            userField.setPreferredSize(new Dimension(0, 40)); // Taller input box (touch friendly)
            gbc.gridy = 3;
            add(userField, gbc);

            // 4. Password Field
            gbc.gridy = 4;
            add(new JLabel("Password"), gbc);
            
            passField = new JPasswordField(20);
            passField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            passField.setPreferredSize(new Dimension(0, 40));
            gbc.gridy = 5;
            add(passField, gbc);

            // 5. Login Button (Big & Blue)
            loginBtn = new JButton("Log in");
            loginBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
            loginBtn.setBackground(new Color(50, 150, 250));
            loginBtn.setForeground(Color.WHITE);
            loginBtn.setFocusPainted(false);
            loginBtn.setPreferredSize(new Dimension(0, 50)); // Big button
            
            gbc.gridy = 6;
            gbc.insets = new Insets(30, 20, 10, 20); // Gap before button
            add(loginBtn, gbc);

            // 6. Register Link
            goToRegisterBtn = new JButton("No account? Register");
            goToRegisterBtn.setBorderPainted(false);
            goToRegisterBtn.setContentAreaFilled(false);
            goToRegisterBtn.setForeground(Color.BLUE);
            goToRegisterBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
            
            gbc.gridy = 7;
            gbc.insets = new Insets(0, 20, 20, 20);
            add(goToRegisterBtn, gbc);

            // 7. Status Label
            statusLabel = new JLabel(" ", SwingConstants.CENTER);
            statusLabel.setForeground(Color.RED);
            gbc.gridy = 8;
            add(statusLabel, gbc);

            // Actions
            loginBtn.addActionListener(this::handleLogin);
            goToRegisterBtn.addActionListener(e -> mainApp.showScreen(MainApp.REGISTER_VIEW));
        }
        //ACTION FOR LOG IN 
        private void handleLogin(ActionEvent e) {
            String user = userField.getText();
            String pass = new String(passField.getPassword());

            loginBtn.setEnabled(false);
            statusLabel.setText("Logging in...");

            // Run DB call in background
            new SwingWorker<User, Void>() {
                @Override
                protected User doInBackground() {
                    return UserController.getInstance().login(user, pass);
                }
 
                @Override
                protected void done() {
                    try {
                        User result = get();
                        if (result != null) {
                            statusLabel.setText("Success!");
                            mainApp.loginSuccess(result);
                        } else {
                            statusLabel.setText("Invalid credentials.");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        statusLabel.setText("Error connecting to DB.");
                    } finally {
                        loginBtn.setEnabled(true);
                    }
                }
            }.execute();
        }
    }
    // --- REGISTER PANEL (Mobile Style) ---
    public static class RegisterPanel extends JPanel {
        private MainApp mainApp;
        private JTextField userField;
        private JPasswordField passField;
        private JButton registerBtn, backBtn;
        private JLabel statusLabel;

        public RegisterPanel(MainApp app) {
            this.mainApp = app;
            setLayout(new GridBagLayout());
            setBackground(Color.WHITE); // Clean mobile background

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 20, 10, 20); // Side padding
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;

            // 1. APP NAME (Consistent Header)
            JLabel appName = new JLabel("EazyBus", SwingConstants.CENTER);
            appName.setFont(new Font("SansSerif", Font.BOLD, 40));
            appName.setForeground(new Color(50, 150, 250)); // App Blue
            gbc.gridy = 0;
            gbc.insets = new Insets(50, 20, 10, 20); // Top spacing
            add(appName, gbc);

            // 2. Sub-header
            JLabel subTitle = new JLabel("Create Account", SwingConstants.CENTER);
            subTitle.setFont(new Font("SansSerif", Font.PLAIN, 24));
            subTitle.setForeground(Color.GRAY);
            gbc.gridy = 1;
            gbc.insets = new Insets(0, 20, 40, 20); // Spacing below title
            add(subTitle, gbc);

            // 3. Username Field
            gbc.insets = new Insets(5, 20, 5, 20); // Reset padding
            gbc.gridy = 2;
            add(new JLabel("Choose Username"), gbc);

            userField = new JTextField(20);
            userField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            userField.setPreferredSize(new Dimension(0, 40)); // Taller input
            gbc.gridy = 3;
            add(userField, gbc);

            // 4. Password Field
            gbc.gridy = 4;
            add(new JLabel("Choose Password"), gbc);

            passField = new JPasswordField(20);
            passField.setFont(new Font("SansSerif", Font.PLAIN, 16));
            passField.setPreferredSize(new Dimension(0, 40)); // Taller input
            gbc.gridy = 5;
            add(passField, gbc);

            // 5. Register Button (Big & Blue)
            registerBtn = new JButton("Sign Up");
            registerBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
            registerBtn.setBackground(new Color(50, 150, 250));
            registerBtn.setForeground(Color.WHITE);
            registerBtn.setFocusPainted(false);
            registerBtn.setPreferredSize(new Dimension(0, 50)); // Mobile button height

            gbc.gridy = 6;
            gbc.insets = new Insets(30, 20, 10, 20); // Gap before button
            add(registerBtn, gbc);

            // 6. "Back to Login" Link
            backBtn = new JButton("Already have an account? Login");
            backBtn.setBorderPainted(false);
            backBtn.setContentAreaFilled(false);
            backBtn.setForeground(Color.BLUE); // Blue link looks cleaner for "Back"
            backBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));

            gbc.gridy = 7;
            gbc.insets = new Insets(0, 20, 20, 20);
            add(backBtn, gbc);

            // 7. Status Label
            statusLabel = new JLabel(" ", SwingConstants.CENTER);
            statusLabel.setForeground(Color.RED); // Error messages in red
            gbc.gridy = 8;
            add(statusLabel, gbc);

            // Actions
            backBtn.addActionListener(e -> mainApp.showScreen(MainApp.LOGIN_VIEW));
            registerBtn.addActionListener(this::handleRegister);
        }

        private void handleRegister(ActionEvent e) {
            String u = userField.getText();
            String p = new String(passField.getPassword());
            // Hardcoded Role
            String r = "CUSTOMER";

            if (u.isEmpty() || p.isEmpty()) {
                statusLabel.setText("Fields cannot be empty.");
                return;
            }

            registerBtn.setEnabled(false);
            statusLabel.setText("Creating account...");
            statusLabel.setForeground(Color.GRAY); // Neutral color while loading

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() {
                    // Use AuthController if you split it, or UserController if not
                    return UserController.getInstance().register(u, p, r);
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(RegisterPanel.this, "Success! Please Login.");
                            mainApp.showScreen(MainApp.LOGIN_VIEW);
                            // Clear fields
                            userField.setText("");
                            passField.setText("");
                            statusLabel.setText(" ");
                        } else {
                            statusLabel.setText("Username already exists.");
                            statusLabel.setForeground(Color.RED);
                        }
                    } catch (Exception ex) {
                        statusLabel.setText("Error connecting to database.");
                        ex.printStackTrace();
                    } finally {
                        registerBtn.setEnabled(true);
                    }
                }
            }.execute();
        }
    }
}