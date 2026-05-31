package finalsprojectsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DailySummaryPage extends JFrame {
    private String cashierName;
    private JLabel lblTotalSales;
    private JLabel lblTotalTransactions;
    private JLabel lblTotalItems;
    private JLabel lblAverageSale;
    private DefaultTableModel topSellingModel;

    public DailySummaryPage(String cashierName) {
        this.cashierName = cashierName;
        setTitle("Daily Summary - " + LocalDate.now()
            .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        setSize(800, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        loadSummary();

        setVisible(true);
    }

    private void initComponents() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(44, 62, 80));
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel lblTitle = new JLabel("Daily Sales Summary");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblDate = new JLabel(
            LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")),
            SwingConstants.RIGHT
        );
        lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lblDate.setForeground(new Color(189, 195, 199));

        header.add(lblTitle, BorderLayout.WEST);
        header.add(lblDate, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Main content
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBackground(new Color(236, 240, 241));
        mainContent.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Stats Cards
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        statsPanel.setOpaque(false);
        statsPanel.setPreferredSize(new Dimension(0, 100));

        lblTotalSales = createSummaryCard(statsPanel,
            "Total Sales", "₱0.00", new Color(46, 204, 113));
        lblTotalTransactions = createSummaryCard(statsPanel,
            "Transactions", "0", new Color(52, 152, 219));
        lblTotalItems = createSummaryCard(statsPanel,
            "Items Sold", "0", new Color(155, 89, 182));
        lblAverageSale = createSummaryCard(statsPanel,
            "Avg. Sale", "₱0.00", new Color(230, 126, 34));

        mainContent.add(statsPanel, BorderLayout.NORTH);

        // Top Selling Products Table
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);
        tablePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(44, 62, 80), 2),
            "Top Selling Products Today",
            0, 0,
            new Font("Segoe UI", Font.BOLD, 14)
        ));

        String[] columns = {"Rank", "Product Name", "Qty Sold", "Total Revenue"};
        topSellingModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable topSellingTable = new JTable(topSellingModel);
        topSellingTable.setRowHeight(30);
        topSellingTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        topSellingTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        topSellingTable.getTableHeader().setBackground(new Color(44, 62, 80));
        topSellingTable.getTableHeader().setForeground(Color.WHITE);

        // Center align all columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < columns.length; i++) {
            topSellingTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        tablePanel.add(new JScrollPane(topSellingTable), BorderLayout.CENTER);
        mainContent.add(tablePanel, BorderLayout.CENTER);

        add(mainContent, BorderLayout.CENTER);

        // Bottom Buttons
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bottomPanel.setBackground(new Color(236, 240, 241));

        JButton btnRefresh = createButton("Refresh", new Color(52, 152, 219));
        JButton btnClose = createButton("Close", new Color(231, 76, 60));

        btnRefresh.addActionListener(e -> loadSummary());
        btnClose.addActionListener(e -> dispose());

        bottomPanel.add(btnRefresh);
        bottomPanel.add(btnClose);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JLabel createSummaryCard(JPanel parent, String title, String value, Color color) {
        JPanel card = new JPanel(new GridLayout(2, 1));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 3),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(new Color(127, 140, 141));

        JLabel lblValue = new JLabel(value, SwingConstants.CENTER);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblValue.setForeground(color);

        card.add(lblTitle);
        card.add(lblValue);
        parent.add(card);

        return lblValue;
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

    private void loadSummary() {
        // Load stats
        String statsQuery =
            "SELECT " +
            "COALESCE(SUM(total), 0) AS total_sales, " +
            "COUNT(*) AS transactions, " +
            "COALESCE(AVG(total), 0) AS avg_sale " +
            "FROM sales " +
            "WHERE DATE(sale_date) = CURDATE() " +
            "AND cashier = ?";

        String itemsQuery =
            "SELECT COALESCE(SUM(si.quantity), 0) AS items_sold " +
            "FROM sale_items si " +
            "JOIN sales s ON si.sale_id = s.id " +
            "WHERE DATE(s.sale_date) = CURDATE() " +
            "AND s.cashier = ?";

        String topSellingQuery =
            "SELECT si.product_name, " +
            "SUM(si.quantity) AS total_qty, " +
            "SUM(si.subtotal) AS total_revenue " +
            "FROM sale_items si " +
            "JOIN sales s ON si.sale_id = s.id " +
            "WHERE DATE(s.sale_date) = CURDATE() " +
            "AND s.cashier = ? " +
            "GROUP BY si.product_name " +
            "ORDER BY total_qty DESC " +
            "LIMIT 10";

        try (Connection conn = DBconnection.getConnection()) {
            // Stats
            try (PreparedStatement pst = conn.prepareStatement(statsQuery)) {
                pst.setString(1, cashierName);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    lblTotalSales.setText(
                        String.format("₱%.2f", rs.getDouble("total_sales")));
                    lblTotalTransactions.setText(
                        String.valueOf(rs.getInt("transactions")));
                    lblAverageSale.setText(
                        String.format("₱%.2f", rs.getDouble("avg_sale")));
                }
            }

            // Items sold
            try (PreparedStatement pst = conn.prepareStatement(itemsQuery)) {
                pst.setString(1, cashierName);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    lblTotalItems.setText(
                        String.valueOf(rs.getInt("items_sold")));
                }
            }

            // Top selling
            topSellingModel.setRowCount(0);
            try (PreparedStatement pst = conn.prepareStatement(topSellingQuery)) {
                pst.setString(1, cashierName);
                ResultSet rs = pst.executeQuery();
                int rank = 1;
                while (rs.next()) {
                    topSellingModel.addRow(new Object[]{
                        "#" + rank++,
                        rs.getString("product_name"),
                        rs.getInt("total_qty"),
                        String.format("₱%.2f", rs.getDouble("total_revenue"))
                    });
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading summary: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}