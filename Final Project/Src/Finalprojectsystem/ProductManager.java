package finalsprojectsystem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/pos_inventory";
    private static final String USER = "root";
    private static final String PASS = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public static boolean addProduct(Product product) {
        String sql = "INSERT INTO products (sku, barcode, name, category, price, stock, low_stock_threshold) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql,
                 Statement.RETURN_GENERATED_KEYS)) {

            // Generate SKU
            String sku = product.getSku();
            if (sku == null || sku.isEmpty()) {
                sku = generateSKU();
            }

            pstmt.setString(1, sku);
            pstmt.setString(2, product.getBarcode());
            pstmt.setString(3, product.getName());
            pstmt.setString(4, product.getCategory());
            pstmt.setDouble(5, product.getPrice());
            pstmt.setInt(6, product.getStock());
            pstmt.setInt(7, product.getLowStockThreshold());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    int newId = rs.getInt(1);
                    initializeProductTracking(newId, product.getPrice(),
                        product.getStock());
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String generateSKU() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT MAX(CAST(SUBSTRING(sku, 2) AS UNSIGNED)) as max_num FROM products")) {

            int nextId = 1004;
            if (rs.next()) {
                int maxId = rs.getInt("max_num");
                if (maxId >= 1004) {
                    nextId = maxId + 1;
                }
            }
            return "P" + nextId;

        } catch (SQLException e) {
            e.printStackTrace();
            return "P1004";
        }
    }

    private static void initializeProductTracking(int productId,
            double price, int stock) throws SQLException {
        try (Connection conn = getConnection()) {
            String priceSql =
                "INSERT INTO product_price (ProductID, Price, " +
                "DateAdded, PriceStatus) VALUES (?, ?, CURDATE(), 'Active')";
            try (PreparedStatement pstmt = conn.prepareStatement(priceSql)) {
                pstmt.setInt(1, productId);
                pstmt.setDouble(2, price);
                pstmt.executeUpdate();
            }

            String qtySql =
                "INSERT INTO product_quantity (ProductID, StockQty) " +
                "VALUES (?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(qtySql)) {
                pstmt.setInt(1, productId);
                pstmt.setInt(2, stock);
                pstmt.executeUpdate();
            }
        }
    }

    public static boolean updateProductBasic(Product product) {
        String sql =
            "UPDATE products SET name=?, category=?, price=?, " +
            "low_stock_threshold=?, barcode=? WHERE id=?";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, product.getName());
                    pstmt.setString(2, product.getCategory());
                    pstmt.setDouble(3, product.getPrice());
                    pstmt.setInt(4, product.getLowStockThreshold());
                    pstmt.setString(5, product.getBarcode());
                    pstmt.setInt(6, product.getId());
                    pstmt.executeUpdate();
                }

                String updateStatus =
                    "UPDATE product_price SET PriceStatus = 'Updated', " +
                    "DateAdded = CURDATE() WHERE ProductID = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateStatus)) {
                    pstmt.setInt(1, product.getId());
                    pstmt.executeUpdate();
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean addStock(int productId, int amountToAdd) {
        if (amountToAdd <= 0) return true;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                String updateProduct =
                    "UPDATE products SET stock = stock + ? WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateProduct)) {
                    pstmt.setInt(1, amountToAdd);
                    pstmt.setInt(2, productId);
                    pstmt.executeUpdate();
                }

                String checkSql =
                    "SELECT QuantityID FROM product_quantity " +
                    "WHERE ProductID = ?";
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setInt(1, productId);
                    ResultSet rs = checkStmt.executeQuery();

                    if (rs.next()) {
                        String updateQty =
                            "UPDATE product_quantity " +
                            "SET StockQty = StockQty + ? " +
                            "WHERE ProductID = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(updateQty)) {
                            pstmt.setInt(1, amountToAdd);
                            pstmt.setInt(2, productId);
                            pstmt.executeUpdate();
                        }
                    } else {
                        String insertQty =
                            "INSERT INTO product_quantity " +
                            "(ProductID, StockQty) VALUES (?, ?)";
                        try (PreparedStatement pstmt = conn.prepareStatement(insertQty)) {
                            pstmt.setInt(1, productId);
                            pstmt.setInt(2, amountToAdd);
                            pstmt.executeUpdate();
                        }
                    }
                }

                String updateStatus =
                    "UPDATE product_price SET PriceStatus = 'Updated', " +
                    "DateAdded = CURDATE() WHERE ProductID = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateStatus)) {
                    pstmt.setInt(1, productId);
                    pstmt.executeUpdate();
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean removeStock(int productId, int amountToRemove) {
        if (amountToRemove <= 0) return true;

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                String checkSql = "SELECT stock FROM products WHERE id = ?";
                int currentStock = 0;
                try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
                    pstmt.setInt(1, productId);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        currentStock = rs.getInt("stock");
                    }
                }

                if (currentStock < amountToRemove) {
                    return false;
                }

                String updateProduct =
                    "UPDATE products SET stock = stock - ? WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateProduct)) {
                    pstmt.setInt(1, amountToRemove);
                    pstmt.setInt(2, productId);
                    pstmt.executeUpdate();
                }

                conn.commit();
                return true;

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean updateProduct(Product product) {
        return updateProductBasic(product);
    }

    public static boolean reduceStock(int productId, int quantity) {
        return removeStock(productId, quantity);
    }

    public static List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT id, sku, COALESCE(barcode, '') as barcode, name, category, price, stock, " +
            "low_stock_threshold FROM products ORDER BY name";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(new Product(
                    rs.getInt("id"),
                    rs.getString("sku"),
                    rs.getString("barcode"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("price"),
                    rs.getInt("stock"),
                    rs.getInt("low_stock_threshold")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public static List<Product> getLowStockProducts() {
        List<Product> products = new ArrayList<>();
        String sql =
            "SELECT id, sku, COALESCE(barcode, '') as barcode, name, category, price, stock, " +
            "low_stock_threshold FROM products " +
            "WHERE stock <= low_stock_threshold ORDER BY stock ASC";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                products.add(new Product(
                    rs.getInt("id"),
                    rs.getString("sku"),
                    rs.getString("barcode"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("price"),
                    rs.getInt("stock"),
                    rs.getInt("low_stock_threshold")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public static int getCurrentStock(int productId) {
        String sql = "SELECT stock FROM products WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, productId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("stock");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Product getProductByBarcodeOrSku(String code) {
        String sql =
            "SELECT id, sku, COALESCE(barcode, '') as barcode, name, category, price, stock, " +
            "low_stock_threshold FROM products " +
            "WHERE LOWER(barcode) = LOWER(?) OR LOWER(sku) = LOWER(?) " +
            "LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, code);
            pstmt.setString(2, code);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Product(
                    rs.getInt("id"),
                    rs.getString("sku"),
                    rs.getString("barcode"),
                    rs.getString("name"),
                    rs.getString("category"),
                    rs.getDouble("price"),
                    rs.getInt("stock"),
                    rs.getInt("low_stock_threshold")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}