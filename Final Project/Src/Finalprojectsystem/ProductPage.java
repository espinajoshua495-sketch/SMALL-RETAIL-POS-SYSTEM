package finalsprojectsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Timer;

public class ProductPage extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private JLabel lblLowStockWarning;
    private JTextField txtSearch;
    private JComboBox<String> cmbCategory;
    private Timer autoRefreshTimer;
    private List<Product> allProducts = new ArrayList<>();

    public ProductPage() {
        setTitle("Manage Products");
        setSize(1050, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        initComponents();
        loadProducts();

        autoRefreshTimer = new Timer(3000, e -> {
            int selectedRow = table.getSelectedRow();
            loadProducts();
            if (selectedRow >= 0 && selectedRow < table.getRowCount()) {
                table.setRowSelectionInterval(selectedRow, selectedRow);
            }
        });
        autoRefreshTimer.start();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                autoRefreshTimer.stop();
            }
        });

        setVisible(true);
    }

    private void initComponents() {
        // Warning Label
        lblLowStockWarning = new JLabel();
        lblLowStockWarning.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblLowStockWarning.setHorizontalAlignment(SwingConstants.CENTER);
        lblLowStockWarning.setOpaque(true);
        lblLowStockWarning.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setBackground(new Color(52, 73, 94));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel lblTitle = new JLabel("Manage Products");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        // Search Panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchPanel.setOpaque(false);

        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setForeground(Color.WHITE);
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        txtSearch = new JTextField(18);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.setPreferredSize(new Dimension(200, 32));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        JLabel lblCategory = new JLabel("Category:");
        lblCategory.setForeground(Color.WHITE);
        lblCategory.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        cmbCategory = new JComboBox<>(new String[]{
            "All", "Beverages", "Snacks", "Noodles", "Dairy",
            "Canned Goods", "Household", "Personal Care",
            "Bakery", "Condiments", "Rice & Grains", "Others"
        });
        cmbCategory.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbCategory.setPreferredSize(new Dimension(130, 32));

        JButton btnSearch = new JButton("Search");
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.setBackground(new Color(41, 128, 185));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setBorderPainted(false);
        btnSearch.setPreferredSize(new Dimension(100, 32));
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton btnClear = new JButton("Clear");
        btnClear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClear.setBackground(new Color(149, 165, 166));
        btnClear.setForeground(Color.WHITE);
        btnClear.setFocusPainted(false);
        btnClear.setBorderPainted(false);
        btnClear.setPreferredSize(new Dimension(80, 32));
        btnClear.setCursor(new Cursor(Cursor.HAND_CURSOR));

        searchPanel.add(lblSearch);
        searchPanel.add(txtSearch);
        searchPanel.add(lblCategory);
        searchPanel.add(cmbCategory);
        searchPanel.add(btnSearch);
        searchPanel.add(btnClear);

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(searchPanel, BorderLayout.EAST);

        // Table
        String[] columns = {
            "SKU", "Name", "Category", "Price",
            "Stock", "Threshold", "Status", "Barcode"
        };
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(52, 73, 94));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.setSelectionBackground(new Color(174, 214, 241));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(3).setPreferredWidth(90);
        table.getColumnModel().getColumn(4).setPreferredWidth(70);
        table.getColumnModel().getColumn(5).setPreferredWidth(90);
        table.getColumnModel().getColumn(6).setPreferredWidth(110);
        table.getColumnModel().getColumn(7).setPreferredWidth(140);

        // Custom cell renderer
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);

                // Alternating rows
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 250));
                }

                switch (column) {
                    case 6 -> {
                        // Status column
                        String status = value != null ? value.toString() : "";
                        setHorizontalAlignment(SwingConstants.CENTER);
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                        if (!isSelected) {
                            if (status.contains("OUT")) {
                                c.setBackground(new Color(231, 76, 60));
                                c.setForeground(Color.WHITE);
                            } else if (status.contains("LOW")) {
                                c.setBackground(new Color(241, 196, 15));
                                c.setForeground(Color.BLACK);
                            } else {
                                c.setBackground(new Color(46, 204, 113));
                                c.setForeground(Color.WHITE);
                            }
                        }
                    }
                    case 0 -> {
                        setHorizontalAlignment(SwingConstants.CENTER);
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                        if (!isSelected) {
                            c.setBackground(new Color(245, 245, 245));
                            c.setForeground(new Color(52, 73, 94));
                        }
                    }
                    case 7 -> {
                        // Barcode column
                        setHorizontalAlignment(SwingConstants.CENTER);
                        setFont(new Font("Courier New", Font.PLAIN, 11));
                        if (!isSelected) {
                            c.setBackground(new Color(250, 250, 250));
                            c.setForeground(new Color(100, 100, 100));
                        }
                    }
                    default -> {
                        setHorizontalAlignment(SwingConstants.LEFT);
                        if (!isSelected) {
                            c.setForeground(Color.BLACK);
                        }
                    }
                }
                return c;
            }
        };

        for (int i = 0; i < columns.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(new Color(236, 240, 241));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JLabel lblResultCount = new JLabel("Showing all products");
        lblResultCount.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblResultCount.setForeground(new Color(127, 140, 141));
        lblResultCount.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        btnPanel.setOpaque(false);

        JButton btnAdd    = createStyledButton("Add Product",    new Color(46, 204, 113));
        JButton btnUpdate = createStyledButton("Update Product", new Color(52, 152, 219));
        JButton btnBarcode = createStyledButton("View Barcode",  new Color(155, 89, 182));
        JButton btnClose  = createStyledButton("Close",          new Color(231, 76, 60));

        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnBarcode);
        btnPanel.add(btnClose);

        bottomPanel.add(lblResultCount, BorderLayout.WEST);
        bottomPanel.add(btnPanel, BorderLayout.CENTER);

        // North Panel
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(headerPanel, BorderLayout.NORTH);
        northPanel.add(lblLowStockWarning, BorderLayout.SOUTH);

        add(northPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Action Listeners
        btnSearch.addActionListener(e -> searchProducts(lblResultCount));
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            cmbCategory.setSelectedIndex(0);
            loadProducts();
            lblResultCount.setText("Showing all " + allProducts.size() + " products");
        });

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { searchProducts(lblResultCount); }
            @Override public void removeUpdate(DocumentEvent e) { searchProducts(lblResultCount); }
            @Override public void changedUpdate(DocumentEvent e) { searchProducts(lblResultCount); }
        });

        txtSearch.addActionListener(e -> searchProducts(lblResultCount));
        cmbCategory.addActionListener(e -> searchProducts(lblResultCount));

        btnAdd.addActionListener(e    -> addProduct());
        btnUpdate.addActionListener(e -> updateProduct());
        btnBarcode.addActionListener(e -> viewBarcode());
        btnClose.addActionListener(e -> {
            autoRefreshTimer.stop();
            dispose();
        });
    }

    private void viewBarcode() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a product to view its barcode!",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sku = (String) model.getValueAt(selectedRow, 0);
        String name = (String) model.getValueAt(selectedRow, 1);
        String barcode = (String) model.getValueAt(selectedRow, 7);
        int id = 0;

        for (Product p : allProducts) {
            if (p.getSku().equals(sku)) {
                id = p.getId();
                break;
            }
        }

        String barcodeValue = (barcode == null || barcode.equals("N/A")) ? sku : barcode;
        BarcodeGenerator.showBarcodeDialog(this, id, sku, barcodeValue, name);
    }

   private void searchProducts(JLabel lblResultCount) {
    if (allProducts == null || allProducts.isEmpty()) return;

    String keyword = txtSearch.getText().trim().toLowerCase();
    String category = (String) cmbCategory.getSelectedItem();

    model.setRowCount(0);
    int count = 0;

    for (Product p : allProducts) {
        String productName     = p.getName()     != null ? p.getName().toLowerCase()     : "";
        String productCategory = p.getCategory() != null ? p.getCategory().toLowerCase() : "";
        String productSku      = p.getSku()      != null ? p.getSku().toLowerCase()      : "";
        String productBarcode  = p.getBarcode()  != null ? p.getBarcode().toLowerCase()  : "";

        boolean matchesKeyword = keyword.isEmpty() ||
            productName.contains(keyword) ||
            productCategory.contains(keyword) ||
            productSku.contains(keyword) ||
            productBarcode.contains(keyword);

        boolean matchesCategory =
            category == null ||
            category.equals("All") ||
            productCategory.equalsIgnoreCase(category);

        if (matchesKeyword && matchesCategory) {
            addProductToTable(p);
            count++;
        }
    }

    if (count == 0) {
        // Fixed quote escaping here
        lblResultCount.setText("No results found for: \"" + keyword + "\"");
        lblResultCount.setForeground(new Color(231, 76, 60));
    } else if (keyword.isEmpty() && category.equals("All")) {
        lblResultCount.setText("Showing all " + count + " products");
        lblResultCount.setForeground(new Color(127, 140, 141));
    } else {
        // Fixed quote escaping here
        lblResultCount.setText((!category.equals("All") ? " in " + category : "") +
                "Found " + count + " product(s)" +
                (!keyword.isEmpty() ? " for \"" + keyword + "\"" : ""));
        lblResultCount.setForeground(new Color(39, 174, 96));
    }
}
    private void addProductToTable(Product p) {
        String status = p.getStockStatus();

        model.addRow(new Object[]{
            p.getSku(),
            p.getName(),
            p.getCategory() != null ? p.getCategory() : "N/A",
            String.format("₱%.2f", p.getPrice()),
            p.getStock(),
            p.getLowStockThreshold(),
            status,
            p.getBarcode() != null && !p.getBarcode().isEmpty() ? p.getBarcode() : "N/A"
        });
    }

    private void loadProducts() {
        allProducts = ProductManager.getAllProducts();
        if (allProducts == null) allProducts = new ArrayList<>();

        model.setRowCount(0);
        int lowStockCount = 0;
        int outOfStockCount = 0;

        for (Product p : allProducts) {
            if (p.isOutOfStock()) {
                outOfStockCount++;
            } else if (p.isLowStock()) {
                lowStockCount++;
            }
            addProductToTable(p);
        }

        // Update warning label
        if (outOfStockCount > 0 || lowStockCount > 0) {
            lblLowStockWarning.setText(String.format(
                "⚠ ALERT: %d Out of Stock | %d Low Stock — Please restock soon!",
                outOfStockCount, lowStockCount));
            lblLowStockWarning.setForeground(new Color(192, 57, 43));
            lblLowStockWarning.setBackground(new Color(255, 235, 235));
        } else {
            lblLowStockWarning.setText(
                "✓ All products are in stock — Inventory healthy");
            lblLowStockWarning.setForeground(new Color(39, 174, 96));
            lblLowStockWarning.setBackground(new Color(234, 250, 241));
        }
    }

    private void addProduct() {
        autoRefreshTimer.stop();

        JTextField txtName      = new JTextField();
        JTextField txtPrice     = new JTextField();
        JTextField txtStock     = new JTextField();
        JTextField txtThreshold = new JTextField("10");
        JTextField txtBarcode   = new JTextField();

        JComboBox<String> cmbCat = new JComboBox<>(new String[]{
            "Beverages", "Snacks", "Noodles", "Dairy",
            "Canned Goods", "Household", "Personal Care",
            "Bakery", "Condiments", "Rice & Grains", "Others"
        });

        Dimension fieldSize = new Dimension(250, 35);
        Font fieldFont = new Font("Segoe UI", Font.PLAIN, 13);

        txtName.setPreferredSize(fieldSize);
        txtPrice.setPreferredSize(fieldSize);
        txtStock.setPreferredSize(fieldSize);
        txtThreshold.setPreferredSize(fieldSize);
        txtBarcode.setPreferredSize(fieldSize);
        cmbCat.setPreferredSize(fieldSize);

        txtName.setFont(fieldFont);
        txtPrice.setFont(fieldFont);
        txtStock.setFont(fieldFont);
        txtThreshold.setFont(fieldFont);
        txtBarcode.setFont(new Font("Courier New", Font.PLAIN, 13));
        cmbCat.setFont(fieldFont);

        Object[] message = {
            "Product Name: *",      txtName,
            "Category:",            cmbCat,
            "Price (₱): *",         txtPrice,
            "Stock Quantity: *",    txtStock,
            "Low Stock Threshold:", txtThreshold,
            "Barcode (UPC):",       txtBarcode
        };

        int option = JOptionPane.showConfirmDialog(
            this, message, "Add New Product",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            try {
                String name          = txtName.getText().trim();
                String category      = (String) cmbCat.getSelectedItem();
                String priceText     = txtPrice.getText().trim();
                String stockText     = txtStock.getText().trim();
                String thresholdText = txtThreshold.getText().trim();
                String barcodeText   = txtBarcode.getText().trim();

                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                        "Product name is required!",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                    autoRefreshTimer.start();
                    return;
                }

                if (priceText.isEmpty() || stockText.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                        "Price and Stock are required!",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                    autoRefreshTimer.start();
                    return;
                }

                double price  = Double.parseDouble(priceText);
                int stock     = Integer.parseInt(stockText);
                int threshold = Integer.parseInt(
                    thresholdText.isEmpty() ? "10" : thresholdText);

                if (price <= 0) {
                    JOptionPane.showMessageDialog(this,
                        "Price must be greater than 0!",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                    autoRefreshTimer.start();
                    return;
                }

                if (stock < 0) {
                    JOptionPane.showMessageDialog(this,
                        "Stock cannot be negative!",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                    autoRefreshTimer.start();
                    return;
                }

                Product product = new Product(
                    0, null, name, category, price, stock, threshold);

                if (!barcodeText.isEmpty()) {
                    product.setBarcode(barcodeText);
                }

                if (ProductManager.addProduct(product)) {
                    JOptionPane.showMessageDialog(this,
                        "Product added successfully!\n\n" +
                        "Name: "     + name     + "\n" +
                        "Category: " + category + "\n" +
                        "Price: ₱"  + String.format("%.2f", price) + "\n" +
                        "Stock: "    + stock    + "\n" +
                        "Barcode: "  + (barcodeText.isEmpty() ? "N/A (auto-generated)" : barcodeText),
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadProducts();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Failed to add product!",
                        "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this,
                    "Invalid input!\n" +
                    "Price = decimal (e.g. 25.50)\n" +
                    "Stock & Threshold = whole numbers",
                    "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        autoRefreshTimer.start();
    }

    private void updateProduct() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a product to update!",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        autoRefreshTimer.stop();

        try {
            String sku = (String) model.getValueAt(selectedRow, 0);

            Product selectedProduct = allProducts.stream()
                .filter(p -> p.getSku() != null && p.getSku().equals(sku))
                .findFirst()
                .orElse(null);

            if (selectedProduct == null) {
                JOptionPane.showMessageDialog(this,
                    "Product not found!",
                    "Error", JOptionPane.ERROR_MESSAGE);
                autoRefreshTimer.start();
                return;
            }

            int    id           = selectedProduct.getId();
            String name         = (String) model.getValueAt(selectedRow, 1);
            String category     = (String) model.getValueAt(selectedRow, 2);
            String priceStr     = ((String) model.getValueAt(selectedRow, 3))
                .replace("₱", "").replace(",", "").trim();
            int    currentStock = (int) model.getValueAt(selectedRow, 4);
            int    threshold    = (int) model.getValueAt(selectedRow, 5);
            String barcode      = (String) model.getValueAt(selectedRow, 7);

            JTextField txtName      = new JTextField(name);
            JTextField txtPrice     = new JTextField(priceStr);
            JTextField txtThreshold = new JTextField(String.valueOf(threshold));
            JTextField txtBarcode   = new JTextField(
                barcode.equals("N/A") ? "" : barcode);

            JComboBox<String> cmbCat = new JComboBox<>(new String[]{
                "Beverages", "Snacks", "Noodles", "Dairy",
                "Canned Goods", "Household", "Personal Care",
                "Bakery", "Condiments", "Rice & Grains", "Others"
            });
            cmbCat.setSelectedItem(category);

            Font fieldFont = new Font("Segoe UI", Font.PLAIN, 13);
            Dimension fieldSize = new Dimension(250, 35);

            txtName.setFont(fieldFont);
            txtName.setPreferredSize(fieldSize);
            cmbCat.setFont(fieldFont);
            cmbCat.setPreferredSize(fieldSize);
            txtPrice.setFont(fieldFont);
            txtPrice.setPreferredSize(fieldSize);
            txtThreshold.setFont(fieldFont);
            txtThreshold.setPreferredSize(fieldSize);
            txtBarcode.setFont(new Font("Courier New", Font.PLAIN, 13));
            txtBarcode.setPreferredSize(fieldSize);

            Object[] message = {
                "SKU: " + sku + " (cannot be changed)",
                " ",
                "Product Name:",        txtName,
                "Category:",            cmbCat,
                "Price (₱):",           txtPrice,
                "Low Stock Threshold:", txtThreshold,
                "Barcode (UPC):",       txtBarcode,
                " ",
                "ℹ Current Stock: " + currentStock +
                " (Use Manage Stock to update)"
            };

            int option = JOptionPane.showConfirmDialog(
                this, message, "Update Product — " + sku,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

            if (option == JOptionPane.OK_OPTION) {
                String newName         = txtName.getText().trim();
                String newCategory     = (String) cmbCat.getSelectedItem();
                String newPriceText    = txtPrice.getText().trim();
                String newThresholdText = txtThreshold.getText().trim();
                String newBarcode      = txtBarcode.getText().trim();

                if (newName.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                        "Product name is required!",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                    autoRefreshTimer.start();
                    return;
                }

                double newPrice     = Double.parseDouble(newPriceText);
                int    newThreshold = Integer.parseInt(newThresholdText);

                if (newPrice <= 0) {
                    JOptionPane.showMessageDialog(this,
                        "Price must be greater than 0!",
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                    autoRefreshTimer.start();
                    return;
                }

                Product product = new Product(
                    id, sku, newName, newCategory,
                    newPrice, currentStock, newThreshold);

                if (!newBarcode.isEmpty()) {
                    product.setBarcode(newBarcode);
                }

                if (ProductManager.updateProductBasic(product)) {
                    JOptionPane.showMessageDialog(this,
                        "Product updated successfully!\n\n" +
                        "SKU: "       + sku          + "\n" +
                        "Name: "      + newName      + "\n" +
                        "Category: "  + newCategory  + "\n" +
                        "Price: ₱"   + String.format("%.2f", newPrice) + "\n" +
                        "Threshold: " + newThreshold + "\n" +
                        "Barcode: "   + (newBarcode.isEmpty() ? "N/A" : newBarcode),
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadProducts();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Failed to update product!",
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Invalid input!\n" +
                "Price = decimal (e.g. 25.50)\n" +
                "Threshold = whole number",
                "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }

        autoRefreshTimer.start();
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(160, 38));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(bgColor.darker());
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(bgColor);
            }
        });

        return btn;
    }
}