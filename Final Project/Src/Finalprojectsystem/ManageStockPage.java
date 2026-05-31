package finalsprojectsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.*;

public class ManageStockPage extends JFrame {

    private JTable table;
    private DefaultTableModel model;
    private JTextField txtSearch;
    private JLabel lblStatusSummary;
    private final Color PRIMARY_COLOR = new Color(44, 62, 80);
    private final Color ACCENT_COLOR  = new Color(39, 174, 96);
    private final Color WARNING_COLOR = new Color(241, 196, 15);
    private final Color DANGER_COLOR  = new Color(231, 76, 60);

    public ManageStockPage() {
        setTitle("Inventory Management");
        setSize(1250, 750);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(236, 240, 241));
        setLayout(new BorderLayout(15, 15));

        initComponents();
        loadProducts();
        setVisible(true);
    }

    private void initComponents() {
        // --- HEADER ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(18, 25, 18, 25));

        JLabel lblTitle = new JLabel("STOCK & INVENTORY CONTROL");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle, BorderLayout.WEST);

        // Search in header
        txtSearch = new JTextField(20);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.setPreferredSize(new Dimension(220, 32));

        JButton btnSearch = new JButton("Search");
        btnSearch.setBackground(ACCENT_COLOR);
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.addActionListener(e -> filterTable(txtSearch.getText()));
        txtSearch.addActionListener(e -> filterTable(txtSearch.getText()));

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);
        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setForeground(Color.WHITE);
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchPanel.add(lblSearch);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        header.add(searchPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // --- STATUS SUMMARY PANEL ---
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        statusPanel.setBackground(new Color(248, 249, 250));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(189, 195, 199)),
            BorderFactory.createEmptyBorder(5, 20, 5, 20)
        ));

        lblStatusSummary = new JLabel("Loading inventory status...");
        lblStatusSummary.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStatusSummary.setForeground(PRIMARY_COLOR);
        statusPanel.add(lblStatusSummary);

        // Legend
        JLabel lblLegend = new JLabel(
            "<html>" +
            "<span style='background:#e74c3c;color:white;padding:2px 8px;border-radius:3px;'> Out of Stock </span> &nbsp;" +
            "<span style='background:#f1c40f;color:black;padding:2px 8px;border-radius:3px;'> Low Stock </span> &nbsp;" +
            "<span style='background:#27ae60;color:white;padding:2px 8px;border-radius:3px;'> In Stock </span>" +
            "</html>");
        lblLegend.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusPanel.add(Box.createHorizontalStrut(30));
        statusPanel.add(lblLegend);

        add(statusPanel, BorderLayout.SOUTH);

        // --- TABLE ---
        // Columns: id, sku, name, category, stock, threshold, status, price, barcode
        String[] columns = {
            "ID", "SKU", "Product Name", "Category",
            "Stock", "Threshold", "Status", "Price", "Barcode"
        };
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable(table);

        // Custom renderers
        // Status column renderer with colors
        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 12));

                if (!isSelected && value != null) {
                    String status = value.toString();
                    if (status.contains("OUT")) {
                        c.setBackground(DANGER_COLOR);
                        c.setForeground(Color.WHITE);
                    } else if (status.contains("LOW")) {
                        c.setBackground(WARNING_COLOR);
                        c.setForeground(Color.BLACK);
                    } else {
                        c.setBackground(ACCENT_COLOR);
                        c.setForeground(Color.WHITE);
                    }
                }
                return c;
            }
        };
        table.getColumnModel().getColumn(6).setCellRenderer(statusRenderer);

        // Stock column renderer - red when low
        DefaultTableCellRenderer stockRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 13));

                if (!isSelected && value != null) {
                    int stock = (int) value;
                    if (stock <= 0) {
                        c.setForeground(DANGER_COLOR);
                    } else if (stock <= 10) {
                        c.setForeground(WARNING_COLOR.darker());
                    } else {
                        c.setForeground(ACCENT_COLOR);
                    }
                }
                return c;
            }
        };
        table.getColumnModel().getColumn(4).setCellRenderer(stockRenderer);

        // Barcode column - monospace, centered
        DefaultTableCellRenderer barcodeRenderer = new DefaultTableCellRenderer();
        barcodeRenderer.setFont(new Font("Courier New", Font.BOLD, 12));
        barcodeRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        barcodeRenderer.setForeground(new Color(100, 100, 100));
        table.getColumnModel().getColumn(8).setCellRenderer(barcodeRenderer);

        // SKU column - bold
        DefaultTableCellRenderer skuRenderer = new DefaultTableCellRenderer();
        skuRenderer.setFont(new Font("Segoe UI", Font.BOLD, 12));
        skuRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        skuRenderer.setForeground(PRIMARY_COLOR);
        table.getColumnModel().getColumn(1).setCellRenderer(skuRenderer);

        // Column widths
        int[] widths = {45, 65, 220, 110, 65, 75, 100, 80, 150};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199)));

        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 10));
        tableContainer.add(scrollPane, BorderLayout.CENTER);
        add(tableContainer, BorderLayout.CENTER);

        // --- SIDEBAR ---
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createMatteBorder(
            0, 0, 0, 1, new Color(189, 195, 199)));

        // Title
        JLabel lblSideTitle = new JLabel("ACTIONS");
        lblSideTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSideTitle.setForeground(new Color(127, 140, 141));
        lblSideTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblSideTitle.setBorder(BorderFactory.createEmptyBorder(15, 0, 10, 0));
        sidebar.add(lblSideTitle);
        sidebar.add(Box.createVerticalStrut(5));

        JButton btnAddStock   = createSideBtn("Add Stock",   ACCENT_COLOR);
        JButton btnSetBarcode = createSideBtn("Set Barcode", new Color(155, 89, 182));
        JButton btnGenerateBarcode = createSideBtn("Generate Barcode", new Color(52, 152, 219));
        JButton btnRefresh    = createSideBtn("Refresh",     new Color(230, 126, 34));
        JButton btnClose      = createSideBtn("Close",       DANGER_COLOR);

        btnAddStock.addActionListener(e -> {
    int row = table.getSelectedRow();
    if (row == -1) {
        JOptionPane.showMessageDialog(this,
            "Please select a product first.",
            "No Selection", JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    // Getting data from table
    int productId = (int) table.getValueAt(row, 0);
    String prodName = (String) table.getValueAt(row, 2);
    int currentStock = (int) table.getValueAt(row, 4);

    SpinnerNumberModel spin = new SpinnerNumberModel(1, 1, 9999, 1);
    JSpinner spinner = new JSpinner(spin);
    spinner.setFont(new Font("Segoe UI", Font.BOLD, 16));
    ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setEditable(false);

    int result = JOptionPane.showConfirmDialog(this,
        new Object[]{
            "Product: " + prodName,
            "Current Stock: " + currentStock,
            "Quantity to Add:", spinner
        },
        "Add Stock", JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);

    if (result == JOptionPane.OK_OPTION) {
        int qty = (int) spinner.getValue();
        // Assumes ProductManager class exists in your project
        if (ProductManager.addStock(productId, qty)) {
            JOptionPane.showMessageDialog(this,
                qty + " units added to \"" + prodName + "\".", // Fixed quotes here
                "Stock Updated", JOptionPane.INFORMATION_MESSAGE);
            loadProducts();
        } else {
            JOptionPane.showMessageDialog(this,
                "Failed to update stock.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
});

        // Set Barcode
        btnSetBarcode.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this,
                    "Please select a product first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int    productId      = (int)    table.getValueAt(row, 0);
            String sku            = (String) table.getValueAt(row, 1);
            String prodName       = (String) table.getValueAt(row, 2);
            String currentBarcode = (String) table.getValueAt(row, 8);

            JTextField txtBarcode = new JTextField(
                (currentBarcode == null || currentBarcode.equals("—"))
                    ? "" : currentBarcode);
            txtBarcode.setFont(new Font("Courier New", Font.PLAIN, 15));
            txtBarcode.setPreferredSize(new Dimension(250, 40));

            JLabel hint = new JLabel(
                "<html><i style='color:gray;'>" +
                "Enter the UPC/EAN barcode printed on the packaging.<br>" +
                "Leave blank to use SKU (" + sku + ") as the barcode." +
                "</i></html>");

            int result = JOptionPane.showConfirmDialog(this,
                new Object[]{
                    "Product: " + prodName,
                    "SKU: " + sku,
                    "Barcode Number:", txtBarcode, hint
                },
                "Set Barcode — " + prodName,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String barcodeValue = txtBarcode.getText().trim();
                if (barcodeValue.isEmpty()) barcodeValue = sku;

                String sql = "UPDATE products SET barcode = ? WHERE id = ?";
                try (Connection conn = DBconnection.getConnection();
                     PreparedStatement pst = conn.prepareStatement(sql)) {
                    pst.setString(1, barcodeValue);
                    pst.setInt(2, productId);
                    pst.executeUpdate();
                    JOptionPane.showMessageDialog(this,
                        "Barcode saved: " + barcodeValue,
                        "Saved", JOptionPane.INFORMATION_MESSAGE);
                    loadProducts();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this,
                        "Failed to save barcode:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Generate Barcode
        btnGenerateBarcode.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this,
                    "Please select a product first.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int    productId = (int)    table.getValueAt(row, 0);
            String sku       = (String) table.getValueAt(row, 1);
            String prodName  = (String) table.getValueAt(row, 2);
            String barcode   = (String) table.getValueAt(row, 8);

            String barcodeValue = (barcode == null || barcode.equals("—")) ? sku : barcode;
            BarcodeGenerator.showBarcodeDialog(this, productId, sku, barcodeValue, prodName);
        });

        // Refresh
        btnRefresh.addActionListener(e -> {
            loadProducts();
            JOptionPane.showMessageDialog(this,
                "Inventory refreshed successfully!",
                "Refresh", JOptionPane.INFORMATION_MESSAGE);
        });

        // Close
        btnClose.addActionListener(e -> dispose());

        sidebar.add(btnAddStock);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(btnSetBarcode);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(btnGenerateBarcode);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(btnRefresh);
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(btnClose);

        sidebar.add(Box.createVerticalGlue());

        // Tips panel
        JPanel tipsPanel = new JPanel();
        tipsPanel.setLayout(new BoxLayout(tipsPanel, BoxLayout.Y_AXIS));
        tipsPanel.setBackground(new Color(250, 250, 250));
        tipsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel lblTipTitle = new JLabel("QUICK GUIDE");
        lblTipTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTipTitle.setForeground(new Color(127, 140, 141));
        lblTipTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        tipsPanel.add(lblTipTitle);
        tipsPanel.add(Box.createVerticalStrut(8));

        String[] tips = {
            "① Select a product from the table",
            "② Click 'Set Barcode' to enter UPC/EAN",
            "③ Click 'Generate Barcode' to preview/print",
            "④ Use 'Add Stock' to update inventory",
            "⑤ Red = Out of Stock | Yellow = Low | Green = OK"
        };
        for (String tip : tips) {
            JLabel lblTip = new JLabel("<html><div style='width:180px; font-size:10px; color:#666;'>" + tip + "</div></html>");
            lblTip.setAlignmentX(Component.CENTER_ALIGNMENT);
            tipsPanel.add(lblTip);
            tipsPanel.add(Box.createVerticalStrut(4));
        }

        sidebar.add(tipsPanel);
        add(sidebar, BorderLayout.WEST);
    }

    private void styleTable(JTable tbl) {
        tbl.setRowHeight(36);
        tbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tbl.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tbl.getTableHeader().setBackground(PRIMARY_COLOR);
        tbl.getTableHeader().setForeground(Color.WHITE);
        tbl.getTableHeader().setPreferredSize(new Dimension(0, 38));
        tbl.setSelectionBackground(new Color(46, 204, 113, 80));
        tbl.setSelectionForeground(Color.BLACK);
        tbl.setShowGrid(false);
        tbl.setIntercellSpacing(new Dimension(0, 0));
        tbl.setFillsViewportHeight(true);

        // Alternating row colors
        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 250));
                }
                return c;
            }
        });
    }

    private JButton createSideBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(190, 46));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(bg.darker());
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    private void loadProducts() {
        model.setRowCount(0);
        int outOfStock = 0, lowStock = 0, inStock = 0;

        String sql =
            "SELECT id, sku, name, category, stock, price, low_stock_threshold, " +
            "COALESCE(barcode, '') AS barcode " +
            "FROM products ORDER BY name";
        try (Connection conn = DBconnection.getConnection();
             Statement  st   = conn.createStatement();
             ResultSet  rs   = st.executeQuery(sql)) {
            while (rs.next()) {
                int stock = rs.getInt("stock");
                int threshold = rs.getInt("low_stock_threshold");
                String status;

                if (stock <= 0) {
                    status = "OUT OF STOCK";
                    outOfStock++;
                } else if (stock <= threshold) {
                    status = "LOW STOCK";
                    lowStock++;
                } else {
                    status = "In Stock";
                    inStock++;
                }

                String bc = rs.getString("barcode");
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("sku"),
                    rs.getString("name"),
                    rs.getString("category"),
                    stock,
                    threshold,
                    status,
                    String.format("\u20b1%.2f", rs.getDouble("price")),
                    (bc == null || bc.isBlank()) ? "—" : bc
                });
            }

            // Update status summary
            lblStatusSummary.setText(
                String.format("<html><b>Inventory Status:</b> " +
                    "<span style='color:#e74c3c;'>%d Out of Stock</span> | " +
                    "<span style='color:#f1c40f;'>%d Low Stock</span> | " +
                    "<span style='color:#27ae60;'>%d In Stock</span> | " +
                    "<b>Total: %d products</b></html>",
                    outOfStock, lowStock, inStock, outOfStock + lowStock + inStock));

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Error loading products: " + e.getMessage(),
                "DB Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterTable(String keyword) {
        loadProducts();
        if (keyword == null || keyword.isBlank()) return;
        String kw = keyword.trim().toLowerCase();
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            boolean found = false;
            for (int j = 0; j < model.getColumnCount(); j++) {
                Object val = model.getValueAt(i, j);
                if (val != null && val.toString().toLowerCase().contains(kw)) {
                    found = true;
                    break;
                }
            }
            if (!found) model.removeRow(i);
        }
    }
}