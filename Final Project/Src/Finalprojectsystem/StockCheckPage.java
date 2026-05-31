package finalsprojectsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

public class StockCheckPage extends JFrame {
    private JTable table;
    private DefaultTableModel model;
    private JTextField txtSearch;
    private JLabel lblSummary;

    public StockCheckPage() {
        setTitle("Stock Check - Read Only");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        loadStock();

        setVisible(true);
    }

    private void initComponents() {
        // Header
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(new Color(230, 126, 34));
        header.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel lblTitle = new JLabel("Stock Check");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        // Search
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        searchPanel.setOpaque(false);

        txtSearch = new JTextField(20);
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.setPreferredSize(new Dimension(200, 32));

        JButton btnSearch = new JButton("Search");
        btnSearch.setBackground(new Color(44, 62, 80));
        btnSearch.setForeground(Color.WHITE);
        btnSearch.setFocusPainted(false);
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSearch.setCursor(new Cursor(Cursor.HAND_CURSOR));

        searchPanel.add(new JLabel("Search: ") {{ setForeground(Color.WHITE); setFont(new Font("Segoe UI", Font.PLAIN, 13)); }});
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);

        header.add(lblTitle, BorderLayout.WEST);
        header.add(searchPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Summary panel
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        summaryPanel.setBackground(new Color(248, 249, 250));
        summaryPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(189, 195, 199)),
            BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));

        lblSummary = new JLabel("Loading inventory data...");
        lblSummary.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSummary.setForeground(new Color(44, 62, 80));
        summaryPanel.add(lblSummary);

        add(summaryPanel, BorderLayout.SOUTH);

        // Table
        String[] columns = {"SKU", "Product Name", "Category", "Price", "Stock", "Threshold", "Status"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(44, 62, 80));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setPreferredSize(new Dimension(0, 38));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);

        table.getColumnModel().getColumn(0).setPreferredWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        table.getColumnModel().getColumn(6).setPreferredWidth(120);

        // Status color renderer
        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

                // Alternating rows
                if (!isSelected && column != 6) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 250));
                }

                if (column == 6) {
                    String status = (String) value;
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                    if (!isSelected) {
                        switch (status) {
                            case "OUT OF STOCK" -> {
                                c.setBackground(new Color(231, 76, 60));
                                c.setForeground(Color.WHITE);
                            }
                            case "LOW STOCK" -> {
                                c.setBackground(new Color(241, 196, 15));
                                c.setForeground(Color.BLACK);
                            }
                            default -> {
                                c.setBackground(new Color(46, 204, 113));
                                c.setForeground(Color.WHITE);
                            }
                        }
                    }
                } else if (column == 4) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setFont(new Font("Segoe UI", Font.BOLD, 13));
                } else if (column == 0) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                    if (!isSelected) c.setForeground(new Color(52, 73, 94));
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                return c;
            }
        };

        for (int i = 0; i < 7; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(statusRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        add(scrollPane, BorderLayout.CENTER);

        // Action Listeners
        btnSearch.addActionListener(e -> searchProducts());
        txtSearch.addActionListener(e -> searchProducts());
    }

    private void loadStock() {
        model.setRowCount(0);
        List<Product> products = ProductManager.getAllProducts();
        int outOfStock = 0, lowStock = 0, inStock = 0;

        for (Product p : products) {
            String status = p.getStockStatus();
            if (status.equals("OUT OF STOCK")) outOfStock++;
            else if (status.equals("LOW STOCK")) lowStock++;
            else inStock++;

            model.addRow(new Object[]{
                p.getSku(),
                p.getName(),
                p.getCategory(),
                String.format("₱%.2f", p.getPrice()),
                p.getStock(),
                p.getLowStockThreshold(),
                status
            });
        }

        lblSummary.setText(String.format(
            "<html><b>Inventory Overview:</b> " +
            "<span style='color:#e74c3c;'>%d Out of Stock</span> | " +
            "<span style='color:#f1c40f;'>%d Low Stock</span> | " +
            "<span style='color:#27ae60;'>%d In Stock</span> | " +
            "<b>Total: %d products</b></html>",
            outOfStock, lowStock, inStock, products.size()));
    }

    private void searchProducts() {
        String keyword = txtSearch.getText().trim().toLowerCase();
        model.setRowCount(0);
        int outOfStock = 0, lowStock = 0, inStock = 0;

        List<Product> products = ProductManager.getAllProducts();
        for (Product p : products) {
            if (keyword.isEmpty() ||
                p.getName().toLowerCase().contains(keyword) ||
                p.getCategory().toLowerCase().contains(keyword) ||
                p.getSku().toLowerCase().contains(keyword)) {

                String status = p.getStockStatus();
                if (status.equals("OUT OF STOCK")) outOfStock++;
                else if (status.equals("LOW STOCK")) lowStock++;
                else inStock++;

                model.addRow(new Object[]{
                    p.getSku(),
                    p.getName(),
                    p.getCategory(),
                    String.format("₱%.2f", p.getPrice()),
                    p.getStock(),
                    p.getLowStockThreshold(),
                    status
                });
            }
        }

        int total = outOfStock + lowStock + inStock;
        lblSummary.setText(String.format(
            "<html><b>Search Results:</b> " +
            "<span style='color:#e74c3c;'>%d Out of Stock</span> | " +
            "<span style='color:#f1c40f;'>%d Low Stock</span> | " +
            "<span style='color:#27ae60;'>%d In Stock</span> | " +
            "<b>Showing: %d of %d products</b></html>",
            outOfStock, lowStock, inStock, total, products.size()));
    }
}