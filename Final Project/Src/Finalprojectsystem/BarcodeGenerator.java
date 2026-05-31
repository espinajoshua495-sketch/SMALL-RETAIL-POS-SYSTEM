package finalsprojectsystem;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;

/**
 * BarcodeGenerator — generates a Code 128 barcode image for a REAL barcode number
 * and stores the barcode string in the products.barcode column.
 * 
 * Supports actual UPC/EAN barcode numbers (e.g., 480016121012)
 * instead of just using SKU as the barcode value.
 *
 * Required SQL:
 *   ALTER TABLE products ADD COLUMN IF NOT EXISTS barcode VARCHAR(50) DEFAULT NULL;
 */
public class BarcodeGenerator {

    // Code 128 B encoding table — binary patterns (1=bar, 0=space)
    // Values 0-95 correspond to ASCII 32-127 (Code Set B)
    private static final String[] CODE128_PATTERNS = {
        "11011001100", "11001101100", "11001100110", "10010011000",  // 0-3
        "10010001100", "10001001100", "10011001000", "10011000100",  // 4-7
        "10001100100", "11001001000", "11001000100", "11000100100",  // 8-11
        "10110011100", "10011011100", "10011001110", "10111001100",  // 12-15
        "10011101100", "10011100110", "11001110010", "11001011100",  // 16-19
        "11001001110", "11011100100", "11001110100", "11101101110",  // 20-23
        "11101001100", "11100101100", "11100100110", "11101100100",  // 24-27
        "11100110100", "11100110010", "11011011000", "11011000110",  // 28-31
        "11000110110", "10100011000", "10001011000", "10001000110",  // 32-35
        "10110001000", "10001101000", "10001100010", "11010001000",  // 36-39
        "11000101000", "11000100010", "10110111000", "10110001110",  // 40-43
        "10001101110", "10111011000", "10111000110", "10001110110",  // 44-47
        "11101110110", "11010001110", "11000101110", "11011101000",  // 48-51
        "11011100010", "11011101110", "11101011000", "11101000110",  // 52-55
        "11100010110", "11101101000", "11101100010", "11100011010",  // 56-59
        "11101111010", "11001000010", "11110001010", "10100110000",  // 60-63
        "10100001100", "10010110000", "10010000110", "10000101100",  // 64-67
        "10000100110", "10110010000", "10110000100", "10011010000",  // 68-71
        "10011000010", "10000110100", "10000110010", "11000010010",  // 72-75
        "11001010000", "11110111010", "11000010100", "10001111010",  // 76-79
        "10100111100", "10010111100", "10010011110", "10111100100",  // 80-83
        "10011110100", "10011110010", "11110100100", "11110010100",  // 84-87
        "11110010010", "11011011110", "11011110110", "11110110110",  // 88-91
        "10101111000", "10100011110", "10001011110", "10111101000",  // 92-95
        "10111100010", "11110101000", "11110100010", "10111011110",  // 96-99 (FNC etc)
        "10111101110", "11101011110", "11110101110", "11010000100",  // 100-103
        "11010010000", "11010011100", "11000111010"                    // 104-106 (START/STOP)
    };

    private static final int START_B = 104;
    private static final int STOP = 106;

    /**
     * Show a barcode dialog for the given product.
     * Accepts the REAL barcode number (UPC/EAN) to store and encode.
     */
    public static void showBarcodeDialog(Component parent,
            int productId, String sku, String barcodeNumber, String productName) {

        // Use real barcode if provided, otherwise fall back to SKU
        String barcodeValue = (barcodeNumber != null && !barcodeNumber.trim().isEmpty()) 
            ? barcodeNumber.trim() 
            : sku;

        // Save to DB
        saveBarcodeToDB(productId, barcodeValue);

        // Generate image with BLACK BAR LINES
        BufferedImage img = generateCode128Image(barcodeValue, 360, 130);

        JDialog dialog = new JDialog(
            (Frame) SwingUtilities.getWindowAncestor(parent),
            "Barcode — " + productName, true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setSize(460, 320);
        dialog.setLocationRelativeTo(parent);
        dialog.setResizable(false);

        // Header
        JLabel lblHeader = new JLabel("Barcode for: " + productName,
            SwingConstants.CENTER);
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblHeader.setForeground(new Color(44, 62, 80));
        lblHeader.setBorder(BorderFactory.createEmptyBorder(12, 10, 0, 10));
        dialog.add(lblHeader, BorderLayout.NORTH);

        // Barcode image panel with BLACK BAR LINES
        JPanel imgPanel = new JPanel(new BorderLayout(0, 8));
        imgPanel.setBackground(Color.WHITE);
        imgPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(15, 25, 15, 25)
        ));

        JLabel lblBarcode = new JLabel(new ImageIcon(img));
        lblBarcode.setHorizontalAlignment(SwingConstants.CENTER);
        lblBarcode.setOpaque(true);
        lblBarcode.setBackground(Color.WHITE);
        imgPanel.add(lblBarcode, BorderLayout.CENTER);

        // Show BOTH the real barcode number AND SKU for reference
        JLabel lblCode = new JLabel(
            "<html><center>Barcode: <b>" + barcodeValue + "</b><br>" +
            "<span style='color:gray; font-size:11px;'>SKU: " + sku + "</span></center></html>", 
            SwingConstants.CENTER);
        lblCode.setFont(new Font("Courier New", Font.BOLD, 14));
        lblCode.setForeground(new Color(44, 62, 80));
        lblCode.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        imgPanel.add(lblCode, BorderLayout.SOUTH);
        dialog.add(imgPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JButton btnSave = createBtn("Save as PNG", new Color(46, 204, 113));
        JButton btnPrint = createBtn("Print", new Color(52, 152, 219));
        JButton btnClose = createBtn("Close", new Color(231, 76, 60));

        btnSave.addActionListener(e -> saveBarcodeImage(img, barcodeValue, dialog));
        btnPrint.addActionListener(e -> printBarcode(img, barcodeValue, productName));
        btnClose.addActionListener(e -> dialog.dispose());

        btnPanel.add(btnSave);
        btnPanel.add(btnPrint);
        btnPanel.add(btnClose);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    /**
     * Generate Code 128 B barcode image with proper black bars.
     */
    public static BufferedImage generateCode128Image(String data, int width, int height) {
        if (data == null || data.isEmpty()) {
            data = "0";
        }

        String encoded = encodeCode128(data);
        if (encoded == null || encoded.isEmpty()) {
            return createErrorImage(width, height);
        }

        int quietZone = 20;
        int barHeight = height - 40; // Reserve space for text

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // White background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Calculate bar width
        int totalModules = encoded.length();
        int availableWidth = width - (quietZone * 2);
        double moduleWidth = (double) availableWidth / totalModules;

        // Draw bars
        g.setColor(Color.BLACK);
        int x = quietZone;
        for (int i = 0; i < encoded.length(); i++) {
            if (encoded.charAt(i) == '1') {
                int barW = (int) Math.ceil(moduleWidth);
                g.fillRect(x, 0, barW, barHeight);
            }
            x += (int) Math.round(moduleWidth);
        }

        // Human-readable text
        g.setColor(Color.BLACK);
        g.setFont(new Font("Courier New", Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(data);
        int textX = (width - textWidth) / 2;
        g.drawString(data, textX, height - 12);

        g.dispose();
        return img;
    }

    private static BufferedImage createErrorImage(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.RED);
        g.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g.drawString("Invalid barcode data", 20, height / 2);
        g.dispose();
        return img;
    }

    private static String encodeCode128(String data) {
        StringBuilder encoded = new StringBuilder();

        // Start Code B
        encoded.append(CODE128_PATTERNS[START_B]);

        int checksum = START_B;
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            int value;
            if (c >= 32 && c <= 127) {
                value = c - 32;
            } else {
                // Replace unsupported chars with space
                value = 0;
            }
            encoded.append(CODE128_PATTERNS[value]);
            checksum += (i + 1) * value;
        }

        // Check character
        int check = checksum % 103;
        encoded.append(CODE128_PATTERNS[check]);

        // Stop pattern (13 modules: 2 bars, 1 space, 1 bar, 2 spaces, 1 bar, 1 space, 2 bars)
        encoded.append("1100011101011");

        return encoded.toString();
    }

    private static void saveBarcodeToDB(int productId, String barcodeValue) {
        String sql = "UPDATE products SET barcode = ? WHERE id = ?";
        try (Connection conn = DBconnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, barcodeValue);
            pst.setInt(2, productId);
            int updated = pst.executeUpdate();
            System.out.println("Barcode saved: " + barcodeValue + " for product ID " + productId + " (rows: " + updated + ")");
        } catch (SQLException e) {
            System.err.println("Barcode save warning: " + e.getMessage());
            JOptionPane.showMessageDialog(null, 
                "Failed to save barcode to database:\n" + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void saveBarcodeImage(BufferedImage img, String barcodeValue,
            Component parent) {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("barcode_" + barcodeValue + ".png"));
        if (fc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                ImageIO.write(img, "PNG", fc.getSelectedFile());
                JOptionPane.showMessageDialog(parent,
                    "Saved: " + fc.getSelectedFile().getName(),
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent,
                    "Save failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void printBarcode(BufferedImage img, String barcodeValue,
            String productName) {
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return java.awt.print.Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            double x = pageFormat.getImageableX();
            double y = pageFormat.getImageableY();
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.drawString(productName, (int) x, (int) y + 20);
            g2.drawImage(img, (int) x, (int) y + 30, null);
            g2.setFont(new Font("Courier New", Font.PLAIN, 12));
            g2.drawString(barcodeValue, (int) x + 10, (int) y + 30 + img.getHeight() + 15);
            return java.awt.print.Printable.PAGE_EXISTS;
        });
        if (job.printDialog()) {
            try { job.print(); }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                    "Print error: " + ex.getMessage());
            }
        }
    }

    private static JButton createBtn(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(110, 36));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}