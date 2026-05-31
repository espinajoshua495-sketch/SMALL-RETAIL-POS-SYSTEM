package finalsprojectsystem;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class RegisterForm extends JFrame {

    JTextField txtUser;
    JPasswordField txtPass;
    JComboBox<String> roleBox;
    JButton btnRegister;

    public RegisterForm() {
        setTitle("Register");
        setSize(350, 300);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(5,1,10,10));
        panel.setBorder(BorderFactory.createEmptyBorder(20,40,20,40));

        txtUser = new JTextField();
        txtUser.setBorder(BorderFactory.createTitledBorder("Username"));

        txtPass = new JPasswordField();
        txtPass.setBorder(BorderFactory.createTitledBorder("Password"));

        roleBox = new JComboBox<>(new String[]{"Admin", "Cashier"});

        btnRegister = new JButton("Register");

        panel.add(txtUser);
        panel.add(txtPass);
        panel.add(roleBox);
        panel.add(btnRegister);

        add(panel);

        btnRegister.addActionListener(e -> register());

        setVisible(true);
    }

    private void register() {
        String user = txtUser.getText();
        String pass = new String(txtPass.getPassword());
        String role = roleBox.getSelectedItem().toString();

        if(user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Fill all fields");
            return;
        }

        try {
            try (Connection conn = DBconnection.getConnection()) {
                String sql = "INSERT INTO users(username, password, role) VALUES (?, ?, ?)";
                PreparedStatement pst = conn.prepareStatement(sql);
                
                pst.setString(1, user);
                pst.setString(2, pass);
                pst.setString(3, role);
                
                pst.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "User registered!");

            dispose();

        } catch(HeadlessException | SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }
}