package finalsprojectsystem;

public class Product {
    private int id;
    private String sku;
    private String name;
    private String category;
    private double price;
    private int stock;
    private int lowStockThreshold;
    private String barcode;

    // Full constructor
    public Product(int id, String sku, String name, String category, 
                   double price, int stock, int lowStockThreshold) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.lowStockThreshold = lowStockThreshold;
        this.barcode = "";
    }

    // Constructor with barcode
    public Product(int id, String sku, String barcode, String name, 
                   String category, double price, int stock, int lowStockThreshold) {
        this.id = id;
        this.sku = sku;
        this.barcode = barcode != null ? barcode : "";
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.lowStockThreshold = lowStockThreshold;
    }

    // Simplified constructor for new products
    public Product(int id, String name, String category, 
                   double price, int stock, int threshold) {
        this.id = id;
        this.sku = null;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.lowStockThreshold = threshold;
        this.barcode = "";
    }

    // Getters
    public int getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public int getLowStockThreshold() { return lowStockThreshold; }
    public String getBarcode() { return barcode != null ? barcode : ""; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setSku(String sku) { this.sku = sku; }
    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(double price) { this.price = price; }
    public void setStock(int stock) { this.stock = stock; }
    public void setLowStockThreshold(int threshold) { this.lowStockThreshold = threshold; }
    public void setBarcode(String barcode) { this.barcode = barcode != null ? barcode : ""; }

    // Status checks
    public boolean isLowStock() {
        return stock > 0 && stock <= lowStockThreshold;
    }

    public boolean isOutOfStock() {
        return stock <= 0;
    }

    public String getStockStatus() {
        if (isOutOfStock()) return "OUT OF STOCK";
        if (isLowStock()) return "LOW STOCK";
        return "In Stock";
    }

    @Override
    public String toString() {
        return "[" + sku + "] " + name + " - \u20b1" + String.format("%.2f", price);
    }
}