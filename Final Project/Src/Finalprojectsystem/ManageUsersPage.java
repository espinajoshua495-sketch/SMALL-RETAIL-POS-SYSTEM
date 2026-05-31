package finalsprojectsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class ManageUsersPage extends JFrame {
    private JTable table;
    private DefaultTableModel model;

    public ManageUsersPage() {
        setTitle("Manage Users");
        setSize(750, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        loadUsers();

        setVisible(true);
    }

    private void initComponents() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(44, 62, 80));
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel lblTitle = new JLabel("Manage Users");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle);
        add(header, BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Username", "Role", "Created At"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(44, 62, 80));
        table.getTableHeader().setForeground(Color.WHITE);

        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnPanel.setBackground(new Color(236, 240, 241));

        JButton btnAdd = createButton("Add User", new Color(46, 204, 113));
        JButton btnDelete = createButton("Delete User", new Color(231, 76, 60));
        JButton btnChangePass = createButton(
            "Change Password", new Color(52, 152, 219));
        JButton btnClose = createButton("Close", new Color(149, 165, 166));

        btnAdd.addActionListener(e -> addUser());
        btnDelete.addActionListener(e -> deleteUser());
        btnChangePass.addActionListener(e -> changePassword());
        btnClose.addActionListener(e -> dispose());

        btnPanel.add(btnAdd);
        btnPanel.add(btnDelete);
        btnPanel.add(btnChangePass);
        btnPanel.add(btnClose);

        add(btnPanel, BorderLayout.SOUTH);
    }

    private void loadUsers() {
        model.setRowCount(0);
        String query = "SELECT id, username, role, " +
            "created_at FROM users ORDER BY id";

        try (Connection conn = DBconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getTimestamp("created_at") != null ?
                        rs.getTimestamp("created_at").toString() : "N/A"
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading users: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addUser() {
        JTextField txtUsername = new JTextField();
        JPasswordField txtPassword = new JPasswordField();
        JComboBox<String> cmbRole = new JComboBox<>(
            new String[]{"Admin", "Cashier"});

        Object[] fields = {
            "Username:", txtUsername,
            "Password:", txtPassword,
            "Role:", cmbRole
        };

        int result = JOptionPane.showConfirmDialog(this,
            fields, "Add New User", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String username = txtUsername.getText().trim();
            String password = new String(txtPassword.getPassword());
            String role = (String) cmbRole.getSelectedItem();

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Please fill in all fields!",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String sql = "INSERT INTO users " +
                "(username, password, role) VALUES (?, ?, ?)";

            try (Connection conn = DBconnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(sql)) {

                pst.setString(1, username);
                pst.setString(2, password);
                pst.setString(3, role);
                pst.executeUpdate();

                JOptionPane.showMessageDialog(this,
                    "User added successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                loadUsers();

            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate")) {
                    JOptionPane.showMessageDialog(this,
                        "Username already exists!",
                        "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Error: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void deleteUser() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a user to delete!",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) model.getValueAt(selectedRow, 0);
        String username = (String) model.getValueAt(selectedRow, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete user: " + username + "?\n" +
            "This action cannot be undone!",
            "Confirm Delete", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DBconnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(
                     "DELETE FROM users WHERE id = ?")) {

                pst.setInt(1, id);
                pst.executeUpdate();

                JOptionPane.showMessageDialog(this,
                    "User deleted successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                loadUsers();

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                    "Error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void changePassword() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a user!",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) model.getValueAt(selectedRow, 0);
        String username = (String) model.getValueAt(selectedRow, 1);

        JPasswordField txtNewPass = new JPasswordField();
        JPasswordField txtConfirmPass = new JPasswordField();

        Object[] fields = {
            "New password for: " + username,
            "New Password:", txtNewPass,
            "Confirm Password:", txtConfirmPass
        };

        int result = JOptionPane.showConfirmDialog(this,
            fields, "Change Password", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String newPass = new String(txtNewPass.getPassword());
            String confirmPass = new String(txtConfirmPass.getPassword());

            if (newPass.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Password cannot be empty!",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!newPass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this,
                    "Passwords do not match!",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try (Connection conn = DBconnection.getConnection();
                 PreparedStatement pst = conn.prepareStatement(
                     "UPDATE users SET password = ? WHERE id = ?")) {

                pst.setString(1, newPass);
                pst.setInt(2, id);
                pst.executeUpdate();

                JOptionPane.showMessageDialog(this,
                    "Password changed successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                    "Error: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JButton createButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(150, 38));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}