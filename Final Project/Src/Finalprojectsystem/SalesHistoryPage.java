package finalsprojectsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SalesHistoryPage extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private JLabel lblTotalSales;
    private JLabel lblTotalTransactions;
    private JComboBox<String> cmbFilter;
    private JTextField txtDateFrom;
    private JTextField txtDateTo;

    public SalesHistoryPage() {
        setTitle("Sales History");
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        loadSales("TODAY");

        setVisible(true);
    }

    private void initComponents() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(44, 62, 80));
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel lblTitle = new JLabel("📋 Sales History");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitle.setForeground(Color.WHITE);

        header.add(lblTitle, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Filter Panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
            new Color(189, 195, 199)));

        JLabel lblFilter = new JLabel("Filter:");
        lblFilter.setFont(new Font("Segoe UI", Font.BOLD, 13));

        cmbFilter = new JComboBox<>(new String[]{
            "TODAY", "THIS WEEK", "THIS MONTH", "CUSTOM DATE"
        });
        cmbFilter.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbFilter.setPreferredSize(new Dimension(150, 35));

        JLabel lblFrom = new JLabel("From:");
        lblFrom.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        txtDateFrom = new JTextField(10);
        txtDateFrom.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDateFrom.setText(LocalDate.now().toString());
        txtDateFrom.setEnabled(false);

        JLabel lblTo = new JLabel("To:");
        lblTo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        txtDateTo = new JTextField(10);
        txtDateTo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtDateTo.setText(LocalDate.now().toString());
        txtDateTo.setEnabled(false);

        JButton btnApply = createButton("Apply Filter", new Color(52, 152, 219));

        filterPanel.add(lblFilter);
        filterPanel.add(cmbFilter);
        filterPanel.add(lblFrom);
        filterPanel.add(txtDateFrom);
        filterPanel.add(lblTo);
        filterPanel.add(txtDateTo);
        filterPanel.add(btnApply);

        // Stats panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        statsPanel.setBackground(Color.WHITE);

        lblTotalTransactions = new JLabel("Transactions: 0");
        lblTotalTransactions.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTotalTransactions.setForeground(new Color(52, 152, 219));

        lblTotalSales = new JLabel("Total: ₱0.00");
        lblTotalSales.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTotalSales.setForeground(new Color(46, 204, 113));

        statsPanel.add(lblTotalTransactions);
        statsPanel.add(lblTotalSales);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(filterPanel, BorderLayout.WEST);
        topPanel.add(statsPanel, BorderLayout.EAST);
        topPanel.setBackground(Color.WHITE);

        add(topPanel, BorderLayout.NORTH);

        // Table
        String[] columns = {
            "Sale ID", "Cashier", "Items", "Subtotal",
            "Discount", "Total", "Payment Method", "Date & Time"
        };

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
        table.setSelectionBackground(new Color(174, 214, 241));

        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
        table.getColumnModel().getColumn(6).setPreferredWidth(120);
        table.getColumnModel().getColumn(7).setPreferredWidth(150);

        // Center align columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < columns.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bottomPanel.setBackground(new Color(236, 240, 241));

        JButton btnViewItems = createButton("View Sale Items", new Color(52, 152, 219));
        JButton btnClose = createButton("Close", new Color(231, 76, 60));

        btnViewItems.addActionListener(e -> viewSaleItems());
        btnClose.addActionListener(e -> dispose());

        bottomPanel.add(btnViewItems);
        bottomPanel.add(btnClose);

        add(bottomPanel, BorderLayout.SOUTH);

        // Action Listeners
        cmbFilter.addActionListener(e -> {
            boolean isCustom = cmbFilter.getSelectedItem()
                .equals("CUSTOM DATE");
            txtDateFrom.setEnabled(isCustom);
            txtDateTo.setEnabled(isCustom);
        });

        btnApply.addActionListener(e ->
            loadSales((String) cmbFilter.getSelectedItem()));

        // Double click to view items
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewSaleItems();
                }
            }
        });
    }

    private void loadSales(String filter) {
        model.setRowCount(0);

        String dateCondition;
        switch (filter) {
            case "TODAY":
                dateCondition = "DATE(s.sale_date) = CURDATE()";
                break;
            case "THIS WEEK":
                dateCondition = "WEEK(s.sale_date) = WEEK(NOW()) " +
                    "AND YEAR(s.sale_date) = YEAR(NOW())";
                break;
            case "THIS MONTH":
                dateCondition = "MONTH(s.sale_date) = MONTH(NOW()) " +
                    "AND YEAR(s.sale_date) = YEAR(NOW())";
                break;
            case "CUSTOM DATE":
                dateCondition = "DATE(s.sale_date) BETWEEN '" +
                    txtDateFrom.getText() + "' AND '" +
                    txtDateTo.getText() + "'";
                break;
            default:
                dateCondition = "DATE(s.sale_date) = CURDATE()";
        }

        String query =
            "SELECT s.id, s.cashier, s.total, s.sale_date, " +
            "COALESCE(s.payment_method, 'Cash') AS payment_method, " +
            "COALESCE(s.discount, 0) AS discount, " +
            "COUNT(si.id) AS item_count, " +
            "SUM(si.subtotal) AS subtotal " +
            "FROM sales s " +
            "LEFT JOIN sale_items si ON s.id = si.sale_id " +
            "WHERE " + dateCondition + " " +
            "GROUP BY s.id, s.cashier, s.total, s.sale_date, " +
            "s.payment_method, s.discount " +
            "ORDER BY s.sale_date DESC";

        double grandTotal = 0;
        int transCount = 0;

        DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("MMM dd, yyyy hh:mm a");

        try (Connection conn = DBconnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                double total = rs.getDouble("total");
                double discount = rs.getDouble("discount");
                double subtotal = rs.getDouble("subtotal");

                grandTotal += total;
                transCount++;

                model.addRow(new Object[]{
                    "#" + rs.getInt("id"),
                    rs.getString("cashier"),
                    rs.getInt("item_count") + " items",
                    String.format("₱%.2f", subtotal),
                    discount > 0 ? String.format("-₱%.2f", discount) : "-",
                    String.format("₱%.2f", total),
                    rs.getString("payment_method"),
                    rs.getTimestamp("sale_date")
                        .toLocalDateTime().format(formatter)
                });
            }

            lblTotalSales.setText(String.format("Total: ₱%.2f", grandTotal));
            lblTotalTransactions.setText("Transactions: " + transCount);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading sales: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewSaleItems() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a sale to view items!",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String saleIdStr = model.getValueAt(selectedRow, 0)
            .toString().replace("#", "");
        int saleId = Integer.parseInt(saleIdStr);

        // Open sale items dialog
        JDialog dialog = new JDialog(this, "Sale #" + saleId + " - Items", true);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(44, 62, 80));
        header.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel lblTitle = new JLabel("Items in Sale #" + saleId);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle);
        dialog.add(header, BorderLayout.NORTH);

        // Items Table
        String[] columns = {"Product", "Quantity", "Price", "Subtotal"};
        DefaultTableModel itemsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable itemsTable = new JTable(itemsModel);
        itemsTable.setRowHeight(28);
        itemsTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        itemsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        itemsTable.getTableHeader().setBackground(new Color(44, 62, 80));
        itemsTable.getTableHeader().setForeground(Color.WHITE);

        String query =
            "SELECT product_name, quantity, price, subtotal " +
            "FROM sale_items WHERE sale_id = ?";

        double total = 0;
        try (Connection conn = DBconnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setInt(1, saleId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                total += rs.getDouble("subtotal");
                itemsModel.addRow(new Object[]{
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    String.format("₱%.2f", rs.getDouble("price")),
                    String.format("₱%.2f", rs.getDouble("subtotal"))
                });
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(dialog,
                "Error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }

        dialog.add(new JScrollPane(itemsTable), BorderLayout.CENTER);

        // Total and close
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel lblTotal = new JLabel(
            String.format("Sale Total: ₱%.2f",
                Double.parseDouble(model.getValueAt(selectedRow, 5)
                    .toString().replace("₱", ""))),
            SwingConstants.RIGHT);
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTotal.setForeground(new Color(46, 204, 113));

        JButton btnClose = new JButton("Close");
        btnClose.setBackground(new Color(231, 76, 60));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);
        btnClose.addActionListener(e -> dialog.dispose());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.add(btnClose);

        bottomPanel.add(lblTotal, BorderLayout.NORTH);
        bottomPanel.add(btnPanel, BorderLayout.SOUTH);

        dialog.add(bottomPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JButton createButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(160, 38));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}