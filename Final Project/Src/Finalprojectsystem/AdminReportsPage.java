package finalsprojectsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AdminReportsPage extends JFrame {
    private JLabel lblTotalSales;
    private JLabel lblTotalTransactions;
    private JLabel lblTotalItems;
    private JLabel lblBestSeller;
    private DefaultTableModel salesByDayModel;
    private DefaultTableModel topProductsModel;
    private JComboBox<String> cmbPeriod;

    public AdminReportsPage(String adminName) {
        setTitle("Admin Reports & Analytics");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        loadReports("THIS WEEK");

        setVisible(true);
    }

    private void initComponents() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(44, 62, 80));
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel lblTitle = new JLabel("Reports & Analytics");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(Color.WHITE);

        // Period selector
        JPanel periodPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        periodPanel.setOpaque(false);

        JLabel lblPeriod = new JLabel("Period:");
        lblPeriod.setForeground(Color.WHITE);
        lblPeriod.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        cmbPeriod = new JComboBox<>(new String[]{
            "TODAY", "THIS WEEK", "THIS MONTH", "THIS YEAR"
        });
        cmbPeriod.setSelectedItem("THIS WEEK");
        cmbPeriod.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JButton btnApply = new JButton("Apply");
        btnApply.setBackground(new Color(46, 204, 113));
        btnApply.setForeground(Color.WHITE);
        btnApply.setFocusPainted(false);
        btnApply.addActionListener(e ->
            loadReports((String) cmbPeriod.getSelectedItem()));

        periodPanel.add(lblPeriod);
        periodPanel.add(cmbPeriod);
        periodPanel.add(btnApply);

        header.add(lblTitle, BorderLayout.WEST);
        header.add(periodPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Main Content
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBackground(new Color(236, 240, 241));
        mainContent.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Stats Cards
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        statsPanel.setOpaque(false);
        statsPanel.setPreferredSize(new Dimension(0, 100));

        lblTotalSales = createStatCard(statsPanel,
            "Total Sales", "₱0.00", new Color(46, 204, 113));
        lblTotalTransactions = createStatCard(statsPanel,
            "Transactions", "0", new Color(52, 152, 219));
        lblTotalItems = createStatCard(statsPanel,
            "Items Sold", "0", new Color(155, 89, 182));
        lblBestSeller = createStatCard(statsPanel,
            "Best Seller", "-", new Color(230, 126, 34));

        mainContent.add(statsPanel, BorderLayout.NORTH);

        // Tables Panel
        JPanel tablesPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        tablesPanel.setOpaque(false);

        // Sales by Day Table
        JPanel salesByDayPanel = createTablePanel(
            "Sales by Day",
            new String[]{"Date", "Transactions", "Total Sales"},
            tableModel -> salesByDayModel = tableModel
        );

        // Top Products Table
        JPanel topProductsPanel = createTablePanel(
            "Top Selling Products",
            new String[]{"Product", "Qty Sold", "Revenue"},
            tableModel -> topProductsModel = tableModel
        );

        tablesPanel.add(salesByDayPanel);
        tablesPanel.add(topProductsPanel);
        mainContent.add(tablesPanel, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);

        // Bottom
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bottomPanel.setBackground(new Color(236, 240, 241));

        JButton btnClose = createButton(
            "Close", new Color(231, 76, 60));

        btnClose.addActionListener(e -> dispose());

        bottomPanel.add(btnClose);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createTablePanel(String title, String[] columns,
            java.util.function.Consumer<DefaultTableModel> modelSetter) {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(44, 62, 80));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(lblTitle, BorderLayout.NORTH);

        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        modelSetter.accept(tableModel);

        JTable table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(44, 62, 80));
        table.getTableHeader().setForeground(Color.WHITE);

        DefaultTableCellRenderer centerRenderer =
            new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < columns.length; i++) {
            table.getColumnModel().getColumn(i)
                .setCellRenderer(centerRenderer);
        }

        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JLabel createStatCard(JPanel parent, String title,
            String value, Color color) {
        JPanel card = new JPanel(new GridLayout(2, 1));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 3),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(new Color(127, 140, 141));

        JLabel lblValue = new JLabel(value, SwingConstants.CENTER);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblValue.setForeground(color);

        card.add(lblTitle);
        card.add(lblValue);
        parent.add(card);

        return lblValue;
    }

    private void loadReports(String period) {
        String dateCondition;
        dateCondition = switch (period) {
            case "TODAY" -> "DATE(sale_date) = CURDATE()";
            case "THIS WEEK" -> "WEEK(sale_date) = WEEK(NOW()) " +
                "AND YEAR(sale_date) = YEAR(NOW())";
            case "THIS MONTH" -> "MONTH(sale_date) = MONTH(NOW()) " +
                "AND YEAR(sale_date) = YEAR(NOW())";
            case "THIS YEAR" -> "YEAR(sale_date) = YEAR(NOW())";
            default -> "DATE(sale_date) = CURDATE()";
        };

        try (Connection conn = DBconnection.getConnection()) {
            // Summary Stats
            String statsQuery =
                "SELECT " +
                "COALESCE(SUM(total), 0) AS total_sales, " +
                "COUNT(*) AS transactions " +
                "FROM sales WHERE " + dateCondition;

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(statsQuery)) {
                if (rs.next()) {
                    lblTotalSales.setText(String.format(
                        "₱%.2f", rs.getDouble("total_sales")));
                    lblTotalTransactions.setText(
                        String.valueOf(rs.getInt("transactions")));
                }
            }

            // Items sold
            String itemsQuery =
                "SELECT COALESCE(SUM(si.quantity), 0) AS items " +
                "FROM sale_items si JOIN sales s " +
                "ON si.sale_id = s.id WHERE " + dateCondition;

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(itemsQuery)) {
                if (rs.next()) {
                    lblTotalItems.setText(
                        String.valueOf(rs.getInt("items")));
                }
            }

            // Sales by Day
            salesByDayModel.setRowCount(0);
            String byDayQuery =
                "SELECT DATE(sale_date) AS sale_day, " +
                "COUNT(*) AS transactions, " +
                "SUM(total) AS daily_total " +
                "FROM sales WHERE " + dateCondition + " " +
                "GROUP BY DATE(sale_date) " +
                "ORDER BY sale_day DESC";

            DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("MMM dd, yyyy");

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(byDayQuery)) {
                while (rs.next()) {
                    salesByDayModel.addRow(new Object[]{
                        LocalDate.parse(rs.getString("sale_day"))
                            .format(formatter),
                        rs.getInt("transactions"),
                        String.format("₱%.2f",
                            rs.getDouble("daily_total"))
                    });
                }
            }

            // Top Products
            topProductsModel.setRowCount(0);
            String topQuery =
                "SELECT si.product_name, " +
                "SUM(si.quantity) AS qty_sold, " +
                "SUM(si.subtotal) AS revenue " +
                "FROM sale_items si " +
                "JOIN sales s ON si.sale_id = s.id " +
                "WHERE " + dateCondition + " " +
                "GROUP BY si.product_name " +
                "ORDER BY qty_sold DESC LIMIT 10";

            String bestSeller = "-";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(topQuery)) {
                boolean first = true;
                while (rs.next()) {
                    if (first) {
                        bestSeller = rs.getString("product_name");
                        first = false;
                    }
                    topProductsModel.addRow(new Object[]{
                        rs.getString("product_name"),
                        rs.getInt("qty_sold"),
                        String.format("₱%.2f",
                            rs.getDouble("revenue"))
                    });
                }
            }

            // Truncate best seller name if too long
            lblBestSeller.setText(bestSeller.length() > 12 ?
                bestSeller.substring(0, 12) + "..." : bestSeller);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading reports: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton createButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(120, 38));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}