package finalsprojectsystem;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.KeyboardFocusManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class NewSalePage extends JFrame {

    private JTable productTable, cartTable;
    private DefaultTableModel productModel, cartModel;
    private JLabel lblSubtotal, lblVat, lblDiscount, lblTotal,
                   lblItemCount, lblSearchStatus;
    private JLabel lblScannerStatus;          // ← new scanner indicator
    private JTextField txtSearch;
    private JComboBox<String> cmbCategory;
    private JComboBox<String> cmbPaymentMethod;
    private double totalAmount = 0;
    private double discountAmount = 0;
    private static final double VAT_RATE = 0.12;
    private List<SaleItem> cartItems  = new ArrayList<>();
    private List<Product>  allProducts = new ArrayList<>();
    private String cashierName;

    // ── Barcode scanner state ──────────────────────────────────────────────
    private final StringBuilder barcodeBuffer = new StringBuilder();
    private long lastKeyTime = 0;
    /**
     * Maximum gap (ms) between successive characters still considered
     * part of the same scanner burst.  Most scanners finish a 13-char
     * barcode in < 100 ms total; 50 ms per char is generous.
     */
    private static final int SCANNER_CHAR_GAP_MS = 50;
    /**
     * Minimum number of characters to treat a burst as a scanned barcode
     * (avoids triggering on single accidental key presses).
     */
    private static final int MIN_BARCODE_LENGTH = 3;

    public NewSalePage(String cashier) {
        this.cashierName = cashier;
        setTitle("Point of Sale - Cashier: " + cashier);
        setSize(1200, 780);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(5, 5));

        initComponents();
        loadProducts();
        setVisible(true);

        // Give focus to the search field so any manual typing goes there
        SwingUtilities.invokeLater(() -> txtSearch.requestFocusInWindow());

        // ── Global key dispatcher: catches scanner bursts anywhere in window ─
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher(evt -> {
                if (evt.getID() != java.awt.event.KeyEvent.KEY_TYPED) return false;
                if (!isActive()) return false;

                long now = System.currentTimeMillis();
                char c   = evt.getKeyChar();

                if (c == '\n') {
                    // Enter = end of barcode scan
                    String scanned = barcodeBuffer.toString().trim();
                    barcodeBuffer.setLength(0);

                    long elapsed = now - lastKeyTime;
                    boolean looksLikeScanner =
                        scanned.length() >= MIN_BARCODE_LENGTH &&
                        elapsed < SCANNER_CHAR_GAP_MS * 10;  // full barcode time

                    if (looksLikeScanner) {
                        final String code = scanned;
                        SwingUtilities.invokeLater(() -> scanBarcode(code));
                        return true;    // consume the event
                    }
                    // Otherwise it was just the user pressing Enter in search
                } else {
                    long gap = now - lastKeyTime;
                    if (gap <= SCANNER_CHAR_GAP_MS) {
                        // Fast — part of scanner burst
                        barcodeBuffer.append(c);
                    } else {
                        // Slow — manual typing; reset buffer and start fresh
                        barcodeBuffer.setLength(0);
                        barcodeBuffer.append(c);
                    }
                    lastKeyTime = now;
                }
                return false;
            });
    }

    
    private void initComponents() {
        add(createTopBar(),    BorderLayout.NORTH);
        add(createStatusBar(), BorderLayout.SOUTH);   // ← scanner status bar

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(620);
        splitPane.setDividerSize(5);
        splitPane.setLeftComponent(createProductPanel());
        splitPane.setRightComponent(createCartPanel());
        add(splitPane, BorderLayout.CENTER);
    }

    // ── Status bar ────────────────────────────────────────────────────────────
    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(new Color(44, 62, 80));
        bar.setBorder(BorderFactory.createEmptyBorder(4, 14, 4, 14));

        lblScannerStatus = new JLabel("SCANNER READY — aim scanner at any barcode");
        lblScannerStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblScannerStatus.setForeground(new Color(46, 204, 113));
        bar.add(lblScannerStatus, BorderLayout.WEST);
        
        

        JLabel lblShortcuts = new JLabel(
            "F2 = Add to Cart  |  F12 = Checkout  |  ESC = Cancel",
            SwingConstants.RIGHT);
        lblShortcuts.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblShortcuts.setForeground(new Color(149, 165, 166));
        bar.add(lblShortcuts, BorderLayout.EAST);

        return bar;
    }

    // ── Top bar ───────────────────────────────────────────────────────────────
    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        topBar.setBackground(new Color(44, 62, 80));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        topBar.setPreferredSize(new Dimension(0, 70));

        JLabel lblTitle = new JLabel("POINT OF SALE");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        searchPanel.setOpaque(false);

        JLabel lblSearch = new JLabel("Search / Barcode:");
        lblSearch.setForeground(Color.WHITE);
        lblSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        txtSearch = new JTextField(20);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.setPreferredSize(new Dimension(200, 35));
        txtSearch.setToolTipText(
            "Type to search OR scan barcode (scanner auto-triggers)");

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { filterProducts(); }
            @Override public void removeUpdate(DocumentEvent e)  { filterProducts(); }
            @Override public void changedUpdate(DocumentEvent e) { filterProducts(); }
        });

        JLabel lblCat = new JLabel("Category:");
        lblCat.setForeground(Color.WHITE);
        lblCat.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        cmbCategory = new JComboBox<>(new String[]{
            "All", "Beverages", "Snacks", "Noodles",
            "Canned Goods", "Dairy", "Household",
            "Personal Care", "Bakery", "Condiments",
            "Rice & Grains", "Cigarettes"
        });
        cmbCategory.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbCategory.setPreferredSize(new Dimension(130, 35));
        cmbCategory.addActionListener(e -> filterProducts());

        lblSearchStatus = new JLabel("Showing all products");
        lblSearchStatus.setVisible(true);
        lblSearchStatus.setForeground(new Color(189, 195, 199));
        lblSearchStatus.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        

        JButton btnClear = new JButton("✕");
        btnClear.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnClear.setBackground(new Color(149, 165, 166));
        btnClear.setForeground(Color.WHITE);
        btnClear.setFocusPainted(false);
        btnClear.setPreferredSize(new Dimension(40, 35));
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            cmbCategory.setSelectedIndex(0);
            loadProducts();
        });

        searchPanel.add(lblSearch);
        searchPanel.add(txtSearch);
        searchPanel.add(lblCat);
        searchPanel.add(cmbCategory);
        searchPanel.add(btnClear);

        JPanel rightSection = new JPanel(new BorderLayout(0, 3));
        rightSection.setOpaque(false);
        rightSection.add(searchPanel,    BorderLayout.NORTH);
        rightSection.add(lblSearchStatus, BorderLayout.SOUTH);

        topBar.add(lblTitle,    BorderLayout.WEST);
        topBar.add(rightSection, BorderLayout.EAST);
        return topBar;
    }

    // ── Product panel (left) ──────────────────────────────────────────────────
    private JPanel createProductPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(189, 195, 199)),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        panel.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Available Products");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(new Color(44, 62, 80));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(lblTitle, BorderLayout.NORTH);

        String[] columns = {"SKU", "Barcode", "Product Name", "Price", "Stock"};
        productModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        productTable = new JTable(productModel);
        productTable.setRowHeight(30);
        productTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        productTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        productTable.getTableHeader().setBackground(new Color(44, 62, 80));
        productTable.getTableHeader().setForeground(Color.WHITE);
        productTable.setSelectionBackground(new Color(174, 214, 241));

        productTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        productTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        productTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        productTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        productTable.getColumnModel().getColumn(4).setPreferredWidth(60);

        productTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) addToCart();
            }
        });

        panel.add(new JScrollPane(productTable), BorderLayout.CENTER);

        JButton btnAdd = new JButton("Add to Cart (F2)");
        btnAdd.setPreferredSize(new Dimension(0, 42));
        btnAdd.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAdd.setBackground(new Color(46, 204, 113));
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setFocusPainted(false);
        btnAdd.setBorderPainted(false);
        btnAdd.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAdd.addActionListener(e -> addToCart());

        KeyStroke f2 = KeyStroke.getKeyStroke("F2");
        btnAdd.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f2, "add");
        btnAdd.getActionMap().put("add", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { addToCart(); }
        });

        panel.add(btnAdd, BorderLayout.SOUTH);
        return panel;
    }

    // ── Cart panel (right) ────────────────────────────────────────────────────
    private JPanel createCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel lblTitle = new JLabel("Shopping Cart");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(new Color(44, 62, 80));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        panel.add(lblTitle, BorderLayout.NORTH);

        String[] cartCols = {"Product", "Qty", "Unit Price", "Subtotal"};
        cartModel = new DefaultTableModel(cartCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        cartTable = new JTable(cartModel);
        cartTable.setRowHeight(30);
        cartTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cartTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        cartTable.getTableHeader().setBackground(new Color(41, 128, 185));
        cartTable.getTableHeader().setForeground(Color.WHITE);
        cartTable.setSelectionBackground(new Color(174, 214, 241));
        panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new GridLayout(1, 4, 5, 0));
        controlPanel.setOpaque(false);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        JButton btnRemove   = createCartBtn("Remove",     new Color(231, 76, 60));
        JButton btnQty      = createCartBtn("Change Qty", new Color(241, 196, 15));
        JButton btnDiscount = createCartBtn("Discount",   new Color(155, 89, 182));
        JButton btnClear    = createCartBtn("Clear All",  new Color(149, 165, 166));

        btnRemove.addActionListener(e   -> removeFromCart());
        btnQty.addActionListener(e      -> changeQuantity());
        btnDiscount.addActionListener(e -> applyDiscount());
        btnClear.addActionListener(e    -> clearCart());

        controlPanel.add(btnRemove);
        controlPanel.add(btnQty);
        controlPanel.add(btnDiscount);
        controlPanel.add(btnClear);

        JPanel southSection = new JPanel(new BorderLayout(0, 5));
        southSection.setOpaque(false);
        southSection.add(controlPanel,         BorderLayout.NORTH);
        southSection.add(createSummaryPanel(), BorderLayout.CENTER);
        panel.add(southSection, BorderLayout.SOUTH);

        return panel;
    }

    private JButton createCartBtn(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(0, 35));
        return btn;
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(new Color(248, 249, 250));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JPanel labelsPanel = new JPanel(new GridLayout(5, 2, 5, 4));
        labelsPanel.setOpaque(false);

        JLabel lblItemsTitle    = createSummaryLabel("Items in Cart:", false);
        lblItemCount            = createSummaryLabel("0 items", false);
        lblItemCount.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel lblSubtotalTitle = createSummaryLabel("Subtotal:", false);
        lblSubtotal             = createSummaryLabel("₱0.00", false);
        lblSubtotal.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel lblDiscountTitle = createSummaryLabel("Discount:", false);
        lblDiscount             = createSummaryLabel("-₱0.00", false);
        lblDiscount.setForeground(new Color(231, 76, 60));
        lblDiscount.setHorizontalAlignment(SwingConstants.RIGHT);

        JLabel lblVatTitle      = createSummaryLabel("VAT (12%):", false);
        lblVat                  = createSummaryLabel("₱0.00", false);
        lblVat.setForeground(new Color(41, 128, 185));
        lblVat.setHorizontalAlignment(SwingConstants.RIGHT);

        labelsPanel.add(lblItemsTitle);  labelsPanel.add(lblItemCount);
        labelsPanel.add(lblSubtotalTitle); labelsPanel.add(lblSubtotal);
        labelsPanel.add(lblDiscountTitle); labelsPanel.add(lblDiscount);
        labelsPanel.add(lblVatTitle);    labelsPanel.add(lblVat);
        labelsPanel.add(new JLabel());   labelsPanel.add(new JLabel());

        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setOpaque(false);
        lblTotal = new JLabel("TOTAL: ₱0.00");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTotal.setForeground(new Color(44, 62, 80));
        lblTotal.setHorizontalAlignment(SwingConstants.RIGHT);
        totalPanel.add(new JSeparator(), BorderLayout.NORTH);
        totalPanel.add(lblTotal,         BorderLayout.CENTER);

        JPanel payPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        payPanel.setOpaque(false);
        JLabel lblPay = new JLabel("Payment:");
        lblPay.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cmbPaymentMethod = new JComboBox<>(new String[]{"Cash", "GCash", "Maya", "Card"});
        cmbPaymentMethod.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbPaymentMethod.setPreferredSize(new Dimension(120, 32));
        payPanel.add(lblPay);
        payPanel.add(cmbPaymentMethod);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        btnPanel.setOpaque(false);

        JButton btnCheckout = new JButton("CHECKOUT (F12)");
        btnCheckout.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnCheckout.setBackground(new Color(46, 204, 113));
        btnCheckout.setForeground(Color.WHITE);
        btnCheckout.setFocusPainted(false);
        btnCheckout.setBorderPainted(false);
        btnCheckout.setPreferredSize(new Dimension(0, 48));
        btnCheckout.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton btnCancel = new JButton("CANCEL (ESC)");
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnCancel.setBackground(new Color(231, 76, 60));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        btnCancel.setBorderPainted(false);
        btnCancel.setPreferredSize(new Dimension(0, 48));
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnCheckout.addActionListener(e -> checkout());
        btnCancel.addActionListener(e   -> cancelSale());

        KeyStroke f12 = KeyStroke.getKeyStroke("F12");
        KeyStroke esc = KeyStroke.getKeyStroke("ESCAPE");

        btnCheckout.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f12, "checkout");
        btnCheckout.getActionMap().put("checkout", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { checkout(); }
        });
        btnCancel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(esc, "cancel");
        btnCancel.getActionMap().put("cancel", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { cancelSale(); }
        });

        btnPanel.add(btnCheckout);
        btnPanel.add(btnCancel);

        panel.add(labelsPanel, BorderLayout.NORTH);
        panel.add(totalPanel,  BorderLayout.CENTER);

        JPanel bottomBtns = new JPanel(new BorderLayout());
        bottomBtns.setOpaque(false);
        bottomBtns.add(payPanel,  BorderLayout.NORTH);
        bottomBtns.add(btnPanel,  BorderLayout.CENTER);
        panel.add(bottomBtns, BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createSummaryLabel(String text, boolean bold) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, 13));
        lbl.setForeground(new Color(44, 62, 80));
        return lbl;
    }

    
    private void scanBarcode(String code) {
        // Brief visual feedback on the search box
        txtSearch.setText("[SCAN] " + code);
        txtSearch.setBackground(new Color(212, 230, 241));
        Timer resetColor = new Timer(400,
            e -> { txtSearch.setBackground(Color.WHITE); txtSearch.setText(""); });
        resetColor.setRepeats(false);
        resetColor.start();

        // Update status bar
        setScannerStatus("Scanning: " + code + " …", new Color(241, 196, 15));

        // ── DB lookup: barcode OR sku ─────────────────────────────────────────
        Product found = lookupByBarcodeOrSku(code);

        // ── Fallback: search in-memory allProducts list by SKU ───────────────
        if (found == null) {
            for (Product p : allProducts) {
                if (p.getSku().equalsIgnoreCase(code)) {
                    found = p;
                    break;
                }
            }
        }

        if (found == null) {
            Toolkit.getDefaultToolkit().beep();
            setScannerStatus("Not found: " + code, new Color(231, 76, 60));
            JOptionPane.showMessageDialog(this,
                "No product found for barcode: " + code + "\n\n" +
                "Make sure the product's barcode is stored in the database.\n" +
                "(Products table → barcode column)",
                "Barcode Not Found", JOptionPane.WARNING_MESSAGE);
            txtSearch.setText("");
            txtSearch.requestFocusInWindow();
            return;
        }

        addToCartDirect(found);
        setScannerStatus("Added: " + found.getName(), new Color(46, 204, 113));
        txtSearch.setText("");
        txtSearch.requestFocusInWindow();
    }

    /**
     * Query the database for a product whose `barcode` OR `sku`
     * equals the scanned code (case-insensitive).
     */
    private Product lookupByBarcodeOrSku(String code) {
        return ProductManager.getProductByBarcodeOrSku(code);
    }

    /** Update the scanner status label with colour feedback. */
    private void setScannerStatus(String message, Color color) {
        lblScannerStatus.setText("●" + message);
        lblScannerStatus.setForeground(color);
        // Reset to green "ready" after 2 s
        Timer t = new Timer(2000, e ->  {
            lblScannerStatus.setText("●SCANNER READY — aim scanner at any barcode");
            lblScannerStatus.setForeground(new Color(46, 204, 113));
        });
        t.setRepeats(false);
        t.start();
    }

    /**
     * Adds 1 unit of product p directly to the cart (no quantity dialog).
     * If already in cart, increments quantity by 1.
     */
    private void addToCartDirect(Product p) {
        if (p.isOutOfStock()) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(this,
                p.getName() + " is out of stock!",
                "Out of Stock", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int availableStock = ProductManager.getCurrentStock(p.getId());

        for (int i = 0; i < cartItems.size(); i++) {
            SaleItem item = cartItems.get(i);
            if (item.getProductId() == p.getId()) {
                int newQty = item.getQuantity() + 1;
                if (newQty > availableStock) {
                    Toolkit.getDefaultToolkit().beep();
                    JOptionPane.showMessageDialog(this,
                        "Cannot add more — stock limit reached!\nMax: " + availableStock,
                        "Stock Limit", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                SaleItem updated = new SaleItem(
                    p.getId(), p.getName(), newQty, p.getPrice());
                cartItems.set(i, updated);
                cartModel.setValueAt(newQty, i, 1);
                cartModel.setValueAt(
                    String.format("₱%.2f", updated.getSubtotal()), i, 3);
                updateSummary();
                cartTable.setRowSelectionInterval(i, i);
                return;
            }
        }

        // New line in cart
        SaleItem item = new SaleItem(p.getId(), p.getName(), 1, p.getPrice());
        cartItems.add(item);
        cartModel.addRow(new Object[]{
            "[" + p.getSku() + "] " + p.getName(),
            1,
            String.format("₱%.2f", p.getPrice()),
            String.format("₱%.2f", item.getSubtotal())
        });
        int lastRow = cartItems.size() - 1;
        cartTable.setRowSelectionInterval(lastRow, lastRow);
        updateSummary();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  PRODUCT LOADING / FILTERING
    // ──────────────────────────────────────────────────────────────────────────
    private void loadProducts() {
        productModel.setRowCount(0);
        allProducts = loadAllProductsWithBarcode();   // load from DB including barcode

        for (Product p : allProducts) {
            if (!p.isOutOfStock()) {
                productModel.addRow(productRow(p));
            }
        }
        lblSearchStatus.setText("Showing all " + allProducts.size() + " products");
    }

    /**
     * Load all products including the barcode column.
     * Falls back gracefully if the barcode column doesn't exist yet.
     */
    private List<Product> loadAllProductsWithBarcode() {
        return ProductManager.getAllProducts();
    }

    private Object[] productRow(Product p) {
        String barcode = p.getBarcode();
        return new Object[]{
            p.getSku(),
            barcode.isEmpty() ? "—" : barcode,
            p.getName(),
            String.format("₱%.2f", p.getPrice()),
            p.getStock()
        };
    }

    private void filterProducts() {
        String keyword = txtSearch.getText().trim().toLowerCase();
        String category = (String) cmbCategory.getSelectedItem();

        productModel.setRowCount(0);
        int count = 0;

        for (Product p : allProducts) {
            if (p.isOutOfStock()) continue;

            String barcode = p.getBarcode().toLowerCase();

            boolean matchKeyword = keyword.isEmpty()
                || p.getSku().toLowerCase().contains(keyword)
                || p.getName().toLowerCase().contains(keyword)
                || p.getCategory().toLowerCase().contains(keyword)
                || barcode.contains(keyword);

            boolean matchCategory = category.equals("All")
                || p.getCategory().equalsIgnoreCase(category);

            if (matchKeyword && matchCategory) {
                productModel.addRow(productRow(p));
                count++;
            }
        }

        if (keyword.isEmpty() && category.equals("All")) {
            lblSearchStatus.setText("Showing all " + count + " products");
        } else {
            String info = "Found " + count + " product(s)";
            if (!keyword.isEmpty()) info += " for \"" + keyword + "\"";
            if (!category.equals("All")) info += " in " + category;
            lblSearchStatus.setText(info);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  CART OPERATIONS
    // ──────────────────────────────────────────────────────────────────────────
    private void addToCart() {
        int selectedRow = productTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                "Please select a product!", "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sku         = (String) productModel.getValueAt(selectedRow, 0);
        String productName = (String) productModel.getValueAt(selectedRow, 2);
        String priceStr    = ((String) productModel.getValueAt(selectedRow, 3))
            .replace("₱", "").replace(",", "");
        double price       = Double.parseDouble(priceStr);
        int availableStock = (int) productModel.getValueAt(selectedRow, 4);

        Product selectedProduct = null;
        for (Product p : allProducts) {
            if (p.getSku().equals(sku)) { selectedProduct = p; break; }
        }
        if (selectedProduct == null) return;

        JPanel inputPanel = new JPanel(new GridLayout(6, 1, 5, 8));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel lblSKU   = new JLabel("SKU: " + sku);
        lblSKU.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel lblProd  = new JLabel("Product: " + productName);
        lblProd.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JLabel lblPrice = new JLabel("Unit Price: ₱" + String.format("%.2f", price));
        lblPrice.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblPrice.setForeground(new Color(46, 204, 113));

        // ── Quantity spinner — no alphanumeric input, max = available stock ──
        SpinnerNumberModel spinModel = new SpinnerNumberModel(
            1, 1, Math.max(1, availableStock), 1);
        JSpinner spnQty = new JSpinner(spinModel);
        spnQty.setFont(new Font("Segoe UI", Font.BOLD, 16));
        spnQty.setPreferredSize(new Dimension(0, 40));
        // Disable manual text editing — numbers only via arrows
        ((JSpinner.DefaultEditor) spnQty.getEditor())
            .getTextField().setEditable(false);

        JLabel lblStock = new JLabel("Available Stock: " + availableStock);
        lblStock.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStock.setForeground(availableStock <= 5
            ? new Color(231, 76, 60) : new Color(39, 174, 96));

        inputPanel.add(lblSKU);
        inputPanel.add(lblProd);
        inputPanel.add(lblPrice);
        inputPanel.add(lblStock);
        inputPanel.add(new JLabel("Quantity:"));
        inputPanel.add(spnQty);

        int result = JOptionPane.showConfirmDialog(this,
            inputPanel, "Select Quantity",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        try {
            int qty = (int) spnQty.getValue();
            if (qty > availableStock) {
                JOptionPane.showMessageDialog(this,
                    "Not enough stock!\nAvailable: " + availableStock,
                    "Stock Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean found = false;
            for (int i = 0; i < cartItems.size(); i++) {
                SaleItem item = cartItems.get(i);
                if (item.getProductId() == selectedProduct.getId()) {
                    int newQty = item.getQuantity() + qty;
                    if (newQty > availableStock) {
                        JOptionPane.showMessageDialog(this,
                            "Total quantity exceeds stock!",
                            "Stock Limit", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    SaleItem updated = new SaleItem(
                        selectedProduct.getId(), productName, newQty, price);
                    cartItems.set(i, updated);
                    cartModel.setValueAt(newQty, i, 1);
                    cartModel.setValueAt(
                        String.format("₱%.2f", updated.getSubtotal()), i, 3);
                    found = true;
                    break;
                }
            }

            if (!found) {
                SaleItem item = new SaleItem(
                    selectedProduct.getId(), productName, qty, price);
                cartItems.add(item);
                cartModel.addRow(new Object[]{
                    "[" + sku + "] " + productName,
                    qty,
                    String.format("₱%.2f", price),
                    String.format("₱%.2f", item.getSubtotal())
                });
            }
            updateSummary();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Unexpected error: " + e.getMessage(), "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeFromCart() {
        int row = cartTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an item to remove!",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Remove this item from cart?",
                "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            cartItems.remove(row);
            cartModel.removeRow(row);
            updateSummary();
        }
    }

    private void changeQuantity() {
        int row = cartTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an item to change!",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        SaleItem item = cartItems.get(row);
        int stock = ProductManager.getCurrentStock(item.getProductId());

        SpinnerNumberModel chgModel = new SpinnerNumberModel(
            item.getQuantity(), 1, Math.max(1, stock), 1);
        JSpinner spnChgQty = new JSpinner(chgModel);
        spnChgQty.setFont(new Font("Segoe UI", Font.BOLD, 16));
        spnChgQty.setPreferredSize(new Dimension(0, 40));
        ((JSpinner.DefaultEditor) spnChgQty.getEditor())
            .getTextField().setEditable(false);

        int result = JOptionPane.showConfirmDialog(this,
            new Object[]{
                "Product: "         + item.getProductName(),
                "Unit Price: ₱"    + String.format("%.2f", item.getPrice()),
                "Available Stock: " + stock,
                "New Quantity:", spnChgQty
            }, "Change Quantity", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                int newQty = (int) spnChgQty.getValue();
                if (newQty <= 0 || newQty > stock) {
                    JOptionPane.showMessageDialog(this,
                        "Invalid quantity!\nMax: " + stock,
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                SaleItem updated = new SaleItem(
                    item.getProductId(), item.getProductName(), newQty, item.getPrice());
                cartItems.set(row, updated);
                cartModel.setValueAt(newQty, row, 1);
                cartModel.setValueAt(
                    String.format("₱%.2f", updated.getSubtotal()), row, 3);
                updateSummary();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error updating quantity!",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void applyDiscount() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Cart is empty!", "Empty Cart",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] options = {"Percentage (%)", "Senior/PWD (20%)", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
            "Select discount type:", "Apply Discount",
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
            null, options, options[0]);

        if (choice == 2 || choice < 0) return;  // Cancel
        double subtotal = totalAmount + discountAmount;

        // Senior / PWD flat 20 %
        if (choice == 1) {
            discountAmount = subtotal * 0.20;
            updateSummary();
            JOptionPane.showMessageDialog(this,
                String.format("Senior/PWD 20%% discount applied!\nDiscount: -₱%.2f",
                    discountAmount),
                "Discount Applied", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Percentage — use spinner so only numbers are accepted
        SpinnerNumberModel pctModel = new SpinnerNumberModel(0, 0, 100, 1);
        JSpinner spnPct = new JSpinner(pctModel);
        spnPct.setFont(new Font("Segoe UI", Font.BOLD, 16));
        spnPct.setPreferredSize(new Dimension(0, 40));
        ((JSpinner.DefaultEditor) spnPct.getEditor())
            .getTextField().setEditable(false);

        int result = JOptionPane.showConfirmDialog(this,
            new Object[]{"Enter discount percentage (0 – 100):", spnPct},
            "Apply Percentage Discount", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) return;

        int pct = (int) spnPct.getValue();
        if (pct <= 0) {
            JOptionPane.showMessageDialog(this,
                "No discount applied (0%).",
                "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        discountAmount = subtotal * (pct / 100.0);
        updateSummary();
        JOptionPane.showMessageDialog(this,
            String.format("%d%% discount applied!\nDiscount: -₱%.2f",
                pct, discountAmount),
            "Discount Applied", JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearCart() {
        if (cartItems.isEmpty()) return;
        if (JOptionPane.showConfirmDialog(this, "Clear all items from cart?",
                "Clear Cart", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            cartItems.clear();
            cartModel.setRowCount(0);
            discountAmount = 0;
            updateSummary();
        }
    }

    private void updateSummary() {
        double subtotal = 0;
        for (SaleItem item : cartItems) subtotal += item.getSubtotal();
        totalAmount = subtotal;
        double afterDiscount = subtotal - discountAmount;
        double vatAmount     = afterDiscount * VAT_RATE;
        double finalTotal    = afterDiscount + vatAmount;

        lblItemCount.setText(cartItems.size() + " item(s)");
        lblSubtotal.setText(String.format("₱%.2f", subtotal));
        lblDiscount.setText(String.format("-₱%.2f", discountAmount));
        lblVat.setText(String.format("₱%.2f (12%%)", vatAmount));
        lblTotal.setText(String.format("TOTAL: ₱%.2f", finalTotal));
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  CHECKOUT
    // ──────────────────────────────────────────────────────────────────────────
    private void checkout() {
        if (cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Cart is empty! Please add items first.", "Empty Cart",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        double subtotal    = totalAmount;
        double afterDisc   = subtotal - discountAmount;
        double vatAmount   = afterDisc * VAT_RATE;
        double finalTotal  = afterDisc + vatAmount;
        String payMethod   = (String) cmbPaymentMethod.getSelectedItem();

        JTextField txtPayment = new JTextField();
        txtPayment.setFont(new Font("Segoe UI", Font.BOLD, 20));
        txtPayment.setPreferredSize(new Dimension(0, 50));

        int result = JOptionPane.showConfirmDialog(this,
            new Object[]{
                "=== PAYMENT DETAILS ===",
                "Payment Method: " + payMethod,
                String.format("Subtotal:   ₱%.2f", subtotal),
                String.format("Discount:  -₱%.2f", discountAmount),
                String.format("VAT (12%%):  ₱%.2f", vatAmount),
                String.format("TOTAL:      ₱%.2f", finalTotal),
                " ", "Enter Payment Amount:", txtPayment
            }, "Payment", JOptionPane.OK_CANCEL_OPTION);

        if (result != JOptionPane.OK_OPTION) return;

        try {
            double payment = Double.parseDouble(txtPayment.getText().trim());
            if (payment < finalTotal) {
                JOptionPane.showMessageDialog(this,
                    String.format("Insufficient payment!\nRequired: ₱%.2f\nEntered:  ₱%.2f",
                        finalTotal, payment),
                    "Payment Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            double change = payment - finalTotal;
            int confirm = JOptionPane.showConfirmDialog(this,
                String.format(
                    "=== CONFIRM SALE ===\n\nItems: %d\n" +
                    "Subtotal:   ₱%.2f\nDiscount:  -₱%.2f\n" +
                    "VAT (12%%):  ₱%.2f\n━━━━━━━━━━━━━━━━━\n" +
                    "TOTAL:      ₱%.2f\nPayment:    ₱%.2f\nChange:     ₱%.2f\n\n" +
                    "Payment: %s\n\nConfirm sale?",
                    cartItems.size(), subtotal, discountAmount,
                    vatAmount, finalTotal, payment, change, payMethod),
                "Confirm Sale", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                boolean success = true;
                for (SaleItem item : cartItems) {
                    if (!ProductManager.reduceStock(item.getProductId(), item.getQuantity())) {
                        success = false;
                        break;
                    }
                }
                if (success && SaleManager.recordSale(
                        cashierName, cartItems, finalTotal, discountAmount, payMethod)) {
                    showReceipt(payMethod, subtotal, discountAmount, vatAmount,
                        finalTotal, payment, change);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Sale failed! Please try again.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid amount!", "Invalid Payment",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showReceipt(String paymentMethod, double subtotal,
            double discount, double vat, double total,
            double payment, double change) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
        String receiptNo = String.valueOf(System.currentTimeMillis()).substring(7);

        StringBuilder r = new StringBuilder();
        r.append("════════════════════════════════════\n");
        r.append("        SALES RECEIPT\n");
        r.append("════════════════════════════════════\n");
        r.append("Receipt No: #").append(receiptNo).append("\n");
        r.append("Date: ").append(LocalDateTime.now().format(fmt)).append("\n");
        r.append("Cashier: ").append(cashierName).append("\n");
        r.append("Payment: ").append(paymentMethod).append("\n");
        r.append("════════════════════════════════════\n\n");
        r.append(String.format("%-20s %4s %8s %10s\n", "ITEM", "QTY", "PRICE", "SUBTOTAL"));
        r.append("────────────────────────────────────\n");
        for (SaleItem item : cartItems) {
            String name = item.getProductName();
            if (name.length() > 20) name = name.substring(0, 17) + "...";
            r.append(String.format("%-20s %4d %8.2f %10.2f\n",
                name, item.getQuantity(), item.getPrice(), item.getSubtotal()));
        }
        r.append("\n════════════════════════════════════\n");
        r.append(String.format("%-26s %10.2f\n", "Subtotal:", subtotal));
        r.append(String.format("%-26s %10.2f\n", "Discount:", -discount));
        r.append(String.format("%-26s %10.2f\n", "VATable Amount:", subtotal - discount));
        r.append(String.format("%-26s %10.2f\n", "VAT (12%):", vat));
        r.append("════════════════════════════════════\n");
        r.append(String.format("%-26s %10.2f\n", "TOTAL (VAT Inclusive):", total));
        r.append(String.format("%-26s %10.2f\n", "Cash Payment:", payment));
        r.append(String.format("%-26s %10.2f\n", "Change:", change));
        r.append("════════════════════════════════════\n\n");
        r.append("  VAT Reg TIN: 123-456-789-000\n");
        r.append("  This serves as your\n  OFFICIAL RECEIPT\n\n");
        r.append("   Thank you for shopping!\n");
        r.append("    Please come again!\n");
        r.append("════════════════════════════════════\n");

        JTextArea txtReceipt = new JTextArea(r.toString());
        txtReceipt.setFont(new Font("Monospaced", Font.PLAIN, 13));
        txtReceipt.setEditable(false);
        txtReceipt.setBackground(Color.WHITE);

        JScrollPane sp = new JScrollPane(txtReceipt);
        sp.setPreferredSize(new Dimension(450, 520));

        JPanel receiptPanel = new JPanel(new BorderLayout(0, 10));
        receiptPanel.add(sp, BorderLayout.CENTER);

        JButton btnPrint = new JButton("Print Receipt");
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPrint.setBackground(new Color(46, 204, 113));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFocusPainted(false);
        btnPrint.addActionListener(e -> printReceiptText(txtReceipt));

        JButton btnClose = new JButton("Close");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnClose.setBackground(new Color(231, 76, 60));
        btnClose.setForeground(Color.WHITE);
        btnClose.setFocusPainted(false);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.add(btnPrint);
        btnPanel.add(btnClose);
        receiptPanel.add(btnPanel, BorderLayout.SOUTH);

        JDialog receiptDialog = new JDialog(this, "Sales Receipt", true);
        receiptDialog.setSize(490, 620);
        receiptDialog.setLocationRelativeTo(this);
        receiptDialog.setLayout(new BorderLayout(10, 10));
        receiptDialog.getRootPane().setBorder(
            BorderFactory.createEmptyBorder(10, 10, 10, 10));
        receiptDialog.add(receiptPanel);
        btnClose.addActionListener(e -> receiptDialog.dispose());
        receiptDialog.setVisible(true);
    }


    /**
     * Prints the receipt.
     * JTextComponent.print() throws the checked PrinterException —
     * catching it explicitly prevents the "unreported exception" compile error.
     */
    private void printReceiptText(javax.swing.JTextArea area) {
        try {
            area.print(null, null);   // null = no header/footer
        } catch (java.awt.print.PrinterException ex) {
            JOptionPane.showMessageDialog(this,
                "Print failed: " + ex.getMessage(),
                "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelSale() {
        if (cartItems.isEmpty()) { dispose(); return; }
        int confirm = JOptionPane.showConfirmDialog(this,
            "Cancel this sale?\nAll items will be removed.",
            "Cancel Sale", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) dispose();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  INNER CLASS: Product with barcode field
    // ──────────────────────────────────────────────────────────────────────────
    /**
     * Extends Product to carry the barcode column value.
     * Keeps backward-compatibility with all existing code that uses Product.
     */

}