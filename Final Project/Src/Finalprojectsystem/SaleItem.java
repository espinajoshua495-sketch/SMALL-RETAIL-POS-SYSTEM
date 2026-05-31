package finalsprojectsystem;

public class SaleItem {
    private final int productId;
    private final String productName;
    private final int quantity;
    private final double price;
    private final double subtotal;

    public SaleItem(int productId, String productName, int quantity, double price) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.subtotal = quantity * price;
    }

    public int getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public double getSubtotal() { return subtotal; }
}