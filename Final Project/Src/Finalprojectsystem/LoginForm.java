package finalsprojectsystem;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

/**
 * LoginForm — records every successful login to the `user_login_log` table
 * so that the Admin Dashboard can display "User Login Activity" filtered by
 * today / this week / this month / all.
 *
 * Required one-time SQL (run once in phpMyAdmin or MySQL Workbench):
 * ─────────────────────────────────────────────────────────────────
 *   CREATE TABLE IF NOT EXISTS user_login_log (
 *       id         INT AUTO_INCREMENT PRIMARY KEY,
 *       username   VARCHAR(100)  NOT NULL,
 *       login_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
 *       INDEX idx_login_time (login_time),
 *       INDEX idx_username   (username)
 *   );
 */
public class LoginForm extends JFrame {

    JTextField     txtUser;
    JPasswordField txtPass;
    JButton        btnLogin, btnToRegister;

    public LoginForm() {
        setTitle("POS System Login");
        setSize(400, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel();
        header.setBackground(new Color(41, 128, 185));
        header.setPreferredSize(new Dimension(0, 100));
        JLabel lblWelcome = new JLabel("POS SYSTEM");
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblWelcome.setForeground(Color.WHITE);
        header.add(lblWelcome);

        // ── Body ──────────────────────────────────────────────────────────────
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(30, 50, 30, 50));
        body.setBackground(Color.WHITE);

        txtUser = new JTextField();
        txtUser.setBorder(BorderFactory.createTitledBorder("Username"));
        txtUser.setMaximumSize(new Dimension(300, 50));

        txtPass = new JPasswordField();
        txtPass.setBorder(BorderFactory.createTitledBorder("Password"));
        txtPass.setMaximumSize(new Dimension(300, 50));

        btnLogin = new JButton("LOGIN");
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setBackground(new Color(41, 128, 185));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setMaximumSize(new Dimension(300, 40));

        btnToRegister = new JButton("Don't have an account? Register");
        btnToRegister.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnToRegister.setBorderPainted(false);
        btnToRegister.setContentAreaFilled(false);
        btnToRegister.setForeground(Color.GRAY);

        body.add(Box.createRigidArea(new Dimension(0, 10)));
        body.add(txtUser);
        body.add(Box.createRigidArea(new Dimension(0, 15)));
        body.add(txtPass);
        body.add(Box.createRigidArea(new Dimension(0, 25)));
        body.add(btnLogin);
        body.add(btnToRegister);

        add(header, BorderLayout.NORTH);
        add(body,   BorderLayout.CENTER);

        btnLogin.addActionListener(e -> login());
        btnToRegister.addActionListener(e -> { new RegisterForm(); dispose(); });
        getRootPane().setDefaultButton(btnLogin);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  LOGIN LOGIC
    // ──────────────────────────────────────────────────────────────────────────
    private void login() {
        String user = txtUser.getText().trim();
        String pass = new String(txtPass.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields");
            return;
        }

        try (Connection conn = DBconnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(
                 "SELECT * FROM users WHERE username=? AND password=?")) {

            pst.setString(1, user);
            pst.setString(2, pass);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");

                // ── Record login event ─────────────────────────────────────────
                recordLoginEvent(user, conn);

                dispose();

                // Route to correct dashboard
                if (role.equalsIgnoreCase("Cashier")) {
                    new CashierDashboard(user);
                } else {
                    new Dashboard(user, role);
                }

            } else {
                JOptionPane.showMessageDialog(this,
                    "Invalid Username or Password",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Database Error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Insert a row into user_login_log.
     * If the table doesn't exist yet this fails silently so login still works.
     */
    private void recordLoginEvent(String username, Connection conn) {
        try (PreparedStatement pst = conn.prepareStatement(
                "INSERT INTO user_login_log (username, login_time) " +
                "VALUES (?, NOW())")) {
            pst.setString(1, username);
            pst.executeUpdate();
        } catch (SQLException e) {
            // Table may not exist yet — don't block the login
            System.err.println("Login log warning: " + e.getMessage());
        }
    }
}