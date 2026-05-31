package finalsprojectsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.Timer;

public class Dashboard extends JFrame {
    private String username;
    private String role;
    private JLabel lblTime;
    private JLabel lblTodaySales;
    private JLabel lblTodayTransactions;
    private JLabel lblLowStockCount;
    private JLabel lblTotalProducts;
    private Timer clockTimer;
    private Timer statsTimer;

    // Recent Transactions references so we can reload on filter change
    private DefaultTableModel recentModel;
    private JComboBox<String> cmbTxFilter;

    public Dashboard(String username, String role) {
        this.username = username;
        this.role = role;
        setTitle("Admin Dashboard - " + username);
        setSize(1200, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        initComponents();
        startTimers();

        setVisible(true);
    }

    private void initComponents() {
        add(createTopBar(), BorderLayout.NORTH);
        add(createSidebar(), BorderLayout.WEST);
        add(createMainContent(), BorderLayout.CENTER);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopTimers();
            }
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  TOP BAR
    // ──────────────────────────────────────────────────────────────────────────
    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(new Color(44, 62, 80));
        topBar.setPreferredSize(new Dimension(0, 65));
        topBar.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftPanel.setOpaque(false);

        JLabel lblBrand = new JLabel(" ADMIN PANEL");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblBrand.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("POS & Inventory Management System");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(189, 195, 199));

        JPanel brandPanel = new JPanel(new GridLayout(2, 1));
        brandPanel.setOpaque(false);
        brandPanel.add(lblBrand);
        brandPanel.add(lblSub);
        leftPanel.add(brandPanel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        rightPanel.setOpaque(false);

        lblTime = new JLabel();
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTime.setForeground(new Color(189, 195, 199));
        updateClock();

        JLabel lblUser = new JLabel("" + username + " (" + role + ")");
        lblUser.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblUser.setForeground(new Color(46, 204, 113));

        JButton btnLogout = new JButton("Logout");
        btnLogout.setBackground(new Color(231, 76, 60));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLogout.setFocusPainted(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> logout());

        rightPanel.add(lblTime);
        rightPanel.add(lblUser);
        rightPanel.add(btnLogout);

        topBar.add(leftPanel, BorderLayout.WEST);
        topBar.add(rightPanel, BorderLayout.EAST);
        return topBar;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  SIDEBAR
    // ──────────────────────────────────────────────────────────────────────────
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(new Color(52, 73, 94));
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JLabel lblMenu = new JLabel("NAVIGATION");
        lblMenu.setForeground(new Color(127, 140, 141));
        lblMenu.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblMenu.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblMenu.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        sidebar.add(lblMenu);

        JButton btnDashboard    = createSideBtn("Dashboard",        true);
        JButton btnNewSale      = createSideBtn("New Sale",         false);
        JButton btnSalesHistory = createSideBtn("Sales History",    false);
        JButton btnProducts     = createSideBtn("Manage Products",  false);
        JButton btnManageStock  = createSideBtn("Manage Stock",     false);
        JButton btnReports      = createSideBtn("Reports",          false);
        JButton btnUsers        = createSideBtn("Manage Users",     false);

        for (JButton b : new JButton[]{btnDashboard, btnNewSale,
                btnSalesHistory, btnProducts, btnManageStock,
                btnReports, btnUsers}) {
            sidebar.add(b);
            sidebar.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        sidebar.add(Box.createVerticalGlue());

        JLabel lblVersion = new JLabel("v1.0.0", SwingConstants.CENTER);
        lblVersion.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblVersion.setForeground(new Color(127, 140, 141));
        lblVersion.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblVersion);

        btnDashboard.addActionListener(e -> resetSidebarButtons(sidebar, btnDashboard));
        btnNewSale.addActionListener(e -> {
            resetSidebarButtons(sidebar, btnNewSale);
            new NewSalePage(username);
        });
        btnSalesHistory.addActionListener(e -> {
            resetSidebarButtons(sidebar, btnSalesHistory);
            new SalesHistoryPage();
        });
        btnProducts.addActionListener(e -> {
            resetSidebarButtons(sidebar, btnProducts);
            new ProductPage();
        });
        btnManageStock.addActionListener(e -> {
            resetSidebarButtons(sidebar, btnManageStock);
            new ManageStockPage();
        });
        btnReports.addActionListener(e -> {
            resetSidebarButtons(sidebar, btnReports);
            new AdminReportsPage(username);
        });
        btnUsers.addActionListener(e -> {
            resetSidebarButtons(sidebar, btnUsers);
            new ManageUsersPage();
        });

        return sidebar;
    }

    private void resetSidebarButtons(JPanel sidebar, JButton active) {
        for (Component c : sidebar.getComponents()) {
            if (c instanceof JButton) {
                c.setBackground(new Color(52, 73, 94));
            }
        }
        active.setBackground(new Color(41, 128, 185));
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  MAIN CONTENT
    // ──────────────────────────────────────────────────────────────────────────
    private JPanel createMainContent() {
        JPanel mainContent = new JPanel(new BorderLayout(15, 15));
        mainContent.setBackground(new Color(236, 240, 241));
        mainContent.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel lblWelcome = new JLabel(
            "Welcome back, " + username + "! Here's your overview.");
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblWelcome.setForeground(new Color(44, 62, 80));
        lblWelcome.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        mainContent.add(lblWelcome, BorderLayout.NORTH);

        // ── Stat cards ────────────────────────────────────────────────────────
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statsPanel.setOpaque(false);
        statsPanel.setPreferredSize(new Dimension(0, 110));

        lblTodaySales = createStatCard(statsPanel,
            "Today's Sales", "₱0.00", new Color(46, 204, 113), "");
        lblTodayTransactions = createStatCard(statsPanel,
            "Transactions", "0", new Color(52, 152, 219), "");
        lblLowStockCount = createStatCard(statsPanel,
            "Low Stock Items", "0", new Color(231, 76, 60), "");
        lblTotalProducts = createStatCard(statsPanel,
            "Total Products", "0", new Color(155, 89, 182), "");

        mainContent.add(statsPanel, BorderLayout.NORTH);

        // ── Center: Quick Actions | Recent Transactions | Login Activity ───────
        JPanel centerPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        centerPanel.setOpaque(false);

        centerPanel.add(createQuickActionsPanel());
        centerPanel.add(createRecentSalesPanel());     // ← has filter now
        centerPanel.add(createLoginActivityPanel());   // ← NEW

        mainContent.add(centerPanel, BorderLayout.CENTER);
        return mainContent;
    }

    // ── Stat card helper ────────────────────────────────────────────────────
    private JLabel createStatCard(JPanel parent, String title,
            String value, Color color, String icon) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 3),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        titlePanel.setOpaque(false);
        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(new Color(127, 140, 141));
        titlePanel.add(lblIcon);
        titlePanel.add(lblTitle);

        JLabel lblValue = new JLabel(value, SwingConstants.RIGHT);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblValue.setForeground(color);
        lblValue.setName("statValue");

        card.add(titlePanel, BorderLayout.NORTH);
        card.add(lblValue,   BorderLayout.CENTER);
        parent.add(card);
        return lblValue;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  QUICK ACTIONS PANEL
    // ──────────────────────────────────────────────────────────────────────────
    private JPanel createQuickActionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblTitle = new JLabel("Quick Actions");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(new Color(44, 62, 80));
        lblTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        panel.add(lblTitle, BorderLayout.NORTH);

        JPanel btnGrid = new JPanel(new GridLayout(3, 2, 10, 10));
        btnGrid.setOpaque(false);

        JButton btnNewSale      = createQuickBtn("New Sale",      new Color(46, 204, 113));
        JButton btnAddProduct   = createQuickBtn("Add Product",   new Color(52, 152, 219));
        JButton btnAddStock     = createQuickBtn("Add Stock",     new Color(230, 126, 34));
        JButton btnReports      = createQuickBtn("Reports",       new Color(155, 89, 182));
        JButton btnSalesHistory = createQuickBtn("Sales History", new Color(22, 160, 133));
        JButton btnManageUsers  = createQuickBtn("Manage Users",  new Color(192, 57, 43));

        btnNewSale.addActionListener(e      -> new NewSalePage(username));
        btnAddProduct.addActionListener(e   -> new ProductPage());
        btnAddStock.addActionListener(e     -> new ManageStockPage());
        btnReports.addActionListener(e      -> new AdminReportsPage(username));
        btnSalesHistory.addActionListener(e -> new SalesHistoryPage());
        btnManageUsers.addActionListener(e  -> new ManageUsersPage());

        btnGrid.add(btnNewSale);
        btnGrid.add(btnAddProduct);
        btnGrid.add(btnAddStock);
        btnGrid.add(btnReports);
        btnGrid.add(btnSalesHistory);
        btnGrid.add(btnManageUsers);

        panel.add(btnGrid, BorderLayout.CENTER);
        return panel;
    }

    private JButton createQuickBtn(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(color.darker()); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { btn.setBackground(color); }
        });
        return btn;
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  RECENT TRANSACTIONS PANEL  (with Day / Date / Week filter)
    // ──────────────────────────────────────────────────────────────────────────
    private JPanel createRecentSalesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        // ── Header row with title + filter ─────────────────────────────────
        JPanel headerRow = new JPanel(new BorderLayout(8, 0));
        headerRow.setOpaque(false);

        JLabel lblTitle = new JLabel("Recent Transactions");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(44, 62, 80));
        headerRow.add(lblTitle, BorderLayout.WEST);

        // Filter combo: Today | This Week | This Month | All
        cmbTxFilter = new JComboBox<>(new String[]{
            "TODAY", "THIS WEEK", "THIS MONTH", "ALL"
        });
        cmbTxFilter.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cmbTxFilter.setPreferredSize(new Dimension(110, 26));
        cmbTxFilter.setToolTipText("Filter by date range");
        cmbTxFilter.addActionListener(e ->
            loadRecentSales(recentModel,
                (String) cmbTxFilter.getSelectedItem()));
        headerRow.add(cmbTxFilter, BorderLayout.EAST);

        panel.add(headerRow, BorderLayout.NORTH);

        // ── Table ──────────────────────────────────────────────────────────
        // Columns: Sale ID | Cashier | Total | Day | Date & Time
        String[] columns = {"Sale ID", "Cashier", "Total", "Day", "Date & Time"};
        recentModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable recentTable = new JTable(recentModel);
        recentTable.setRowHeight(26);
        recentTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        recentTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        recentTable.getTableHeader().setBackground(new Color(44, 62, 80));
        recentTable.getTableHeader().setForeground(Color.WHITE);
        recentTable.setShowGrid(false);
        recentTable.setIntercellSpacing(new Dimension(0, 0));
        recentTable.setSelectionBackground(new Color(174, 214, 241));

        // Column widths
        recentTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        recentTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        recentTable.getColumnModel().getColumn(2).setPreferredWidth(75);
        recentTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        recentTable.getColumnModel().getColumn(4).setPreferredWidth(115);

        // Center-align all cells
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < columns.length; i++) {
            recentTable.getColumnModel().getColumn(i).setCellRenderer(center);
        }

        loadRecentSales(recentModel, "TODAY");

        panel.add(new JScrollPane(recentTable), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Load recent sales into the model for the given filter period.
     * Columns: Sale ID | Cashier | Total | Day (Mon/Tue…) | Date & Time
     */
    private void loadRecentSales(DefaultTableModel model, String period) {
        model.setRowCount(0);

        String dateCondition;
        switch (period) {
            case "TODAY":
                dateCondition = "DATE(sale_date) = CURDATE()";
                break;
            case "THIS WEEK":
                dateCondition = "WEEK(sale_date) = WEEK(NOW()) " +
                    "AND YEAR(sale_date) = YEAR(NOW())";
                break;
            case "THIS MONTH":
                dateCondition = "MONTH(sale_date) = MONTH(NOW()) " +
                    "AND YEAR(sale_date) = YEAR(NOW())";
                break;
            default:  // ALL — last 50
                dateCondition = "1=1";
                break;
        }

        String query =
            "SELECT id, cashier, total, sale_date " +
            "FROM sales WHERE " + dateCondition + " " +
            "ORDER BY sale_date DESC LIMIT 50";

        // Formatters
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
        DateTimeFormatter dayFmt  = DateTimeFormatter.ofPattern("EEE");   // Mon, Tue …

        try (Connection conn = DBconnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(query)) {

            while (rs.next()) {
                LocalDateTime saleDateTime =
                    rs.getTimestamp("sale_date").toLocalDateTime();

                model.addRow(new Object[]{
                    "#" + rs.getInt("id"),
                    rs.getString("cashier"),
                    String.format("₱%.2f", rs.getDouble("total")),
                    saleDateTime.format(dayFmt),        // Day column
                    saleDateTime.format(timeFmt)         // Date & Time column
                });
            }
        } catch (SQLException e) {
            System.err.println("Error loading recent sales: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  LOGIN ACTIVITY PANEL  (NEW)
    //  Shows which user logged in, when, and the day/week.
    //  Requires the `user_login_log` table — see README SQL below.
    // ──────────────────────────────────────────────────────────────────────────
    private JPanel createLoginActivityPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        // Header with filter
        JPanel headerRow = new JPanel(new BorderLayout(8, 0));
        headerRow.setOpaque(false);

        JLabel lblTitle = new JLabel("User Login Activity");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(new Color(44, 62, 80));
        headerRow.add(lblTitle, BorderLayout.WEST);

        JComboBox<String> cmbLoginFilter = new JComboBox<>(new String[]{
            "TODAY", "THIS WEEK", "THIS MONTH", "ALL"
        });
        cmbLoginFilter.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        cmbLoginFilter.setPreferredSize(new Dimension(110, 26));

        headerRow.add(cmbLoginFilter, BorderLayout.EAST);
        panel.add(headerRow, BorderLayout.NORTH);

        // Table: Username | Role | Day | Login Date & Time
        String[] cols = {"Username", "Role", "Day", "Login Date & Time"};
        DefaultTableModel loginModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable loginTable = new JTable(loginModel);
        loginTable.setRowHeight(26);
        loginTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        loginTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        loginTable.getTableHeader().setBackground(new Color(52, 73, 94));
        loginTable.getTableHeader().setForeground(Color.WHITE);
        loginTable.setShowGrid(false);
        loginTable.setIntercellSpacing(new Dimension(0, 0));
        loginTable.setSelectionBackground(new Color(174, 214, 241));

        loginTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        loginTable.getColumnModel().getColumn(1).setPreferredWidth(65);
        loginTable.getColumnModel().getColumn(2).setPreferredWidth(45);
        loginTable.getColumnModel().getColumn(3).setPreferredWidth(140);

        // Role colour renderer
        DefaultTableCellRenderer roleRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected && col == 1) {
                    String v = value == null ? "" : value.toString();
                    setForeground(v.equalsIgnoreCase("Admin")
                        ? new Color(155, 89, 182) : new Color(52, 152, 219));
                    setFont(new Font("Segoe UI", Font.BOLD, 11));
                } else {
                    setForeground(isSelected ? Color.WHITE : Color.BLACK);
                    setFont(new Font("Segoe UI", Font.PLAIN, 12));
                }
                return this;
            }
        };
        for (int i = 0; i < cols.length; i++) {
            loginTable.getColumnModel().getColumn(i).setCellRenderer(roleRenderer);
        }

        loadLoginActivity(loginModel, "TODAY");

        cmbLoginFilter.addActionListener(e ->
            loadLoginActivity(loginModel, (String) cmbLoginFilter.getSelectedItem()));

        panel.add(new JScrollPane(loginTable), BorderLayout.CENTER);

        // Note if table doesn't exist yet
        JLabel lblNote = new JLabel(
            "Requires user_login_log table", SwingConstants.CENTER);
        lblNote.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblNote.setForeground(new Color(149, 165, 166));
        panel.add(lblNote, BorderLayout.SOUTH);

        return panel;
    }

    private void loadLoginActivity(DefaultTableModel model, String period) {
        model.setRowCount(0);

        String dateCondition;
        switch (period) {
            case "TODAY":
                dateCondition = "DATE(login_time) = CURDATE()";
                break;
            case "THIS WEEK":
                dateCondition = "WEEK(login_time) = WEEK(NOW()) " +
                    "AND YEAR(login_time) = YEAR(NOW())";
                break;
            case "THIS MONTH":
                dateCondition = "MONTH(login_time) = MONTH(NOW()) " +
                    "AND YEAR(login_time) = YEAR(NOW())";
                break;
            default:
                dateCondition = "1=1";
                break;
        }

        String query =
            "SELECT l.username, u.role, l.login_time " +
            "FROM user_login_log l " +
            "LEFT JOIN users u ON l.username = u.username " +
            "WHERE " + dateCondition + " " +
            "ORDER BY l.login_time DESC LIMIT 50";

        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
        DateTimeFormatter dayFmt  = DateTimeFormatter.ofPattern("EEE");

        try (Connection conn = DBconnection.getConnection();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(query)) {

            while (rs.next()) {
                LocalDateTime loginTime =
                    rs.getTimestamp("login_time").toLocalDateTime();
                model.addRow(new Object[]{
                    rs.getString("username"),
                    rs.getString("role") != null ? rs.getString("role") : "—",
                    loginTime.format(dayFmt),
                    loginTime.format(timeFmt)
                });
            }
        } catch (SQLException e) {
            // If the table doesn't exist yet, show a helpful placeholder row
            if (e.getMessage() != null &&
                    e.getMessage().toLowerCase().contains("doesn't exist")) {
                model.addRow(new Object[]{
                    "Run setup SQL", "—", "—", "user_login_log table missing"
                });
            } else {
                System.err.println("Login activity error: " + e.getMessage());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  STATS UPDATE
    // ──────────────────────────────────────────────────────────────────────────
    private void updateStats() {
        String salesQuery =
            "SELECT COALESCE(SUM(total), 0) AS total, " +
            "COUNT(*) AS count FROM sales " +
            "WHERE DATE(sale_date) = CURDATE()";
        String stockQuery =
            "SELECT COUNT(*) AS low FROM products " +
            "WHERE stock <= low_stock_threshold";
        String productQuery =
            "SELECT COUNT(*) AS total FROM products";

        try (Connection conn = DBconnection.getConnection()) {
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(salesQuery)) {
                if (rs.next()) {
                    lblTodaySales.setText(
                        String.format("₱%.2f", rs.getDouble("total")));
                    lblTodayTransactions.setText(
                        String.valueOf(rs.getInt("count")));
                }
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(stockQuery)) {
                if (rs.next()) {
                    int low = rs.getInt("low");
                    lblLowStockCount.setText(String.valueOf(low));
                    lblLowStockCount.setForeground(
                        low > 0 ? new Color(231, 76, 60)
                                : new Color(46, 204, 113));
                }
            }
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(productQuery)) {
                if (rs.next()) {
                    lblTotalProducts.setText(
                        String.valueOf(rs.getInt("total")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Stats error: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ──────────────────────────────────────────────────────────────────────────
    private JButton createSideBtn(String text, boolean isActive) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(200, 42));
        btn.setPreferredSize(new Dimension(200, 42));
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setBackground(isActive
            ? new Color(41, 128, 185) : new Color(52, 73, 94));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!btn.getBackground().equals(new Color(41, 128, 185)))
                    btn.setBackground(new Color(44, 62, 80));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (!btn.getBackground().equals(new Color(41, 128, 185)))
                    btn.setBackground(new Color(52, 73, 94));
            }
        });
        return btn;
    }

    private void updateClock() {
        DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("MMM dd, yyyy  hh:mm:ss a");
        lblTime.setText(LocalDateTime.now().format(formatter));
    }

    private void startTimers() {
        clockTimer = new Timer(1000, e -> updateClock());
        clockTimer.start();

        statsTimer = new Timer(5000, e -> {
            updateStats();
            // Also refresh recent transactions automatically
            if (cmbTxFilter != null && recentModel != null) {
                loadRecentSales(recentModel,
                    (String) cmbTxFilter.getSelectedItem());
            }
        });
        statsTimer.start();

        updateStats();
    }

    private void stopTimers() {
        if (clockTimer != null) clockTimer.stop();
        if (statsTimer != null) statsTimer.stop();
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to logout?",
            "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            stopTimers();
            dispose();
            new LoginForm().setVisible(true);
        }
    }
}