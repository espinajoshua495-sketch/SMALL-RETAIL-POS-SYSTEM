package finalsprojectsystem;

import javax.swing.*;
import java.awt.*;

public class CashierDashboard extends JFrame {
    private String cashierName;

    public CashierDashboard(String cashierName) {
        this.cashierName = cashierName;
        setTitle("Point of Sale | Cashier: " + cashierName);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        // Modern Top Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(44, 62, 80));
        topBar.setPreferredSize(new Dimension(0, 70));
        topBar.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));

        JLabel lblWelcome = new JLabel("Welcome, " + cashierName);
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblWelcome.setForeground(Color.WHITE);
        topBar.add(lblWelcome, BorderLayout.WEST);

        add(topBar, BorderLayout.NORTH);

        // Main Grid Layout for specific actions
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(236, 240, 241));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);

        // ONLY NEW SALE AND HISTORY (Removed Daily Summary and Stock Check)
        JButton btnNewSale = createMenuButton("NEW TRANSACTION", "Start a new sale", new Color(39, 174, 96));
        JButton btnHistory = createMenuButton("SALES HISTORY", "View previous logs", new Color(52, 152, 219));
        JButton btnLogout = createMenuButton("LOGOUT", "Exit the system", new Color(231, 76, 60));

        btnNewSale.addActionListener(e -> new NewSalePage(cashierName));
        btnLogout.addActionListener(e -> logout());

        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(btnNewSale, gbc);
        gbc.gridx = 1;
        mainPanel.add(btnHistory, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        mainPanel.add(btnLogout, gbc);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JButton createMenuButton(String title, String subtitle, Color color) {
        JButton btn = new JButton("<html><center><b>" + title + "</b><br><font size='3'>" + subtitle + "</font></center></html>");
        btn.setPreferredSize(new Dimension(250, 120));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createLineBorder(color.darker(), 2));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void logout() {
        dispose();
        new LoginForm().setVisible(true);
    }
}