package finalsprojectsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.List;

public class SaleManager extends JFrame {
    private String cashier;
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JLabel lblTotal;
    private double grandTotal = 0.0;

    public SaleManager(String cashier) {
        this.cashier = cashier;
        setTitle("Point of Sale - Cashier: " + cashier);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        // Top Scanner Area
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        top.setBackground(new Color(39, 174, 96));

        JLabel lblScan = new JLabel("BARCODE / SKU:");
        lblScan.setForeground(Color.WHITE);
        lblScan.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JTextField txtScan = new JTextField(20);
        txtScan.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtScan.setPreferredSize(new Dimension(250, 35));

        JButton btnAdd = new JButton("ADD TO CART");
        btnAdd.setBackground(new Color(44, 62, 80));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnAdd.setFocusPainted(false);
        btnAdd.addActionListener(e -> addToCart(txtScan.getText().trim()));

        txtScan.addActionListener(e -> addToCart(txtScan.getText().trim()));

        top.add(lblScan);
        top.add(txtScan);
        top.add(btnAdd);
        add(top, BorderLayout.NORTH);

        // Cart Table
        cartModel = new DefaultTableModel(
            new String[]{"SKU", "Product", "Qty", "Price", "Subtotal"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(32);
        cartTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cartTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        cartTable.getTableHeader().setBackground(new Color(44, 62, 80));
        cartTable.getTableHeader().setForeground(Color.WHITE);
        add(new JScrollPane(cartTable), BorderLayout.CENTER);

        // Checkout Panel
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setPreferredSize(new Dimension(0, 100));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        lblTotal = new JLabel("TOTAL: ₱ 0.00   ");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblTotal.setForeground(new Color(44, 62, 80));

        JButton btnCheckout = new JButton("PROCEED TO CHECKOUT");
        btnCheckout.setBackground(new Color(46, 204, 113));
        btnCheckout.setForeground(Color.WHITE);
        btnCheckout.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnCheckout.setFocusPainted(false);
        btnCheckout.addActionListener(e -> processCheckout());

        bottom.add(lblTotal, BorderLayout.EAST);
        bottom.add(btnCheckout, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void addToCart(String input) {
        if (input.isEmpty()) return;

        // Use ProductManager to lookup by barcode or SKU
        Product p = ProductManager.getProductByBarcodeOrSku(input);

        if (p != null) {
            cartModel.addRow(new Object[]{
                p.getSku(), p.getName(), 1, p.getPrice(), p.getPrice()});
            updateTotal();
        } else {
            JOptionPane.showMessageDialog(this,
                "Product not found for: " + input,
                "Not Found", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateTotal() {
        grandTotal = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            grandTotal += (double) cartModel.getValueAt(i, 4);
        }
        lblTotal.setText(String.format("TOTAL: ₱ %.2f   ", grandTotal));
    }

    private void processCheckout() {
        if (cartModel.getRowCount() == 0) return;
        JOptionPane.showMessageDialog(this,
            "Transaction Successful!\nTotal: ₱" +
                String.format("%.2f", grandTotal));
        cartModel.setRowCount(0);
        updateTotal();
    }

    // Static recordSale() - called by NewSalePage after checkout
    public static boolean recordSale(String cashierName,
            List<SaleItem> items, double total,
            double discount, String paymentMethod) {

        if (items == null || items.isEmpty()) return false;

        String insertSale =
            "INSERT INTO sales " +
            "(cashier, total, discount, payment_method, sale_date) " +
            "VALUES (?, ?, ?, ?, NOW())";

        String insertItem =
            "INSERT INTO sale_items " +
            "(sale_id, product_id, product_name, quantity, price, subtotal) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBconnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Insert sale header
                int saleId;
                try (PreparedStatement ps =
                        conn.prepareStatement(insertSale,
                            Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, cashierName);
                    ps.setDouble(2, total);
                    ps.setDouble(3, discount);
                    ps.setString(4, paymentMethod);
                    ps.executeUpdate();

                    ResultSet keys = ps.getGeneratedKeys();
                    if (!keys.next()) {
                        conn.rollback();
                        return false;
                    }
                    saleId = keys.getInt(1);
                }

                // Insert each line item
                try (PreparedStatement ps = conn.prepareStatement(insertItem)) {
                    for (SaleItem item : items) {
                        ps.setInt(1, saleId);
                        ps.setInt(2, item.getProductId());
                        ps.setString(3, item.getProductName());
                        ps.setInt(4, item.getQuantity());
                        ps.setDouble(5, item.getPrice());
                        ps.setDouble(6, item.getSubtotal());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                conn.commit();
                return true;

            } catch (SQLException ex) {
                conn.rollback();
                System.err.println("recordSale error: " + ex.getMessage());
                return false;
            }
        } catch (SQLException ex) {
            System.err.println("recordSale connection error: " + ex.getMessage());
            return false;
        }
    }
}