package com.bizmind.report;

import com.bizmind.model.Sale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Sales Receipt Generator using Apache PDFBox.
 * Covers US-13 (Generate Sales Receipt).
 * Mirrors ExpenseReportGenerator pattern.
 *
 * Creates professional PDF receipts for individual sales transactions.
 */
public class SalesReceiptGenerator {

    private static final float MARGIN = 40;
    private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.LETTER.getHeight();
    private static final float LINE_HEIGHT = 14;

    private static final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    /**
     * Generate a PDF receipt for a single sale transaction.
     * US-13: Generate Sales Receipt
     *
     * @param sale Sale object to generate receipt for
     * @param outputPath File path where to save the PDF
     * @throws IOException if PDF generation fails
     */
    public static void generateSingleReceipt(Sale sale, String outputPath) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            float yPosition = PAGE_HEIGHT - MARGIN;

            // ── Header: Receipt Title ──
            yPosition = drawHeader(contentStream, yPosition);

            // ── Receipt Details ──
            yPosition = drawReceiptDetails(contentStream, sale, yPosition);

            // ── Item Details ──
            yPosition = drawItemDetails(contentStream, sale, yPosition);

            // ── Total Section ──
            yPosition = drawTotals(contentStream, sale, yPosition);

            // ── Footer ──
            drawFooter(contentStream, yPosition);
        }

        document.save(outputPath);
        document.close();
    }

    /**
     * Generate PDF receipts for multiple sales (batch receipt).
     * Future enhancement for bulk receipt generation.
     *
     * @param sales List of Sale objects
     * @param outputPath File path where to save the PDF
     * @throws IOException if PDF generation fails
     */
    public static void generateBatchReceipts(List<Sale> sales, String outputPath) throws IOException {
        PDDocument document = new PDDocument();

        for (Sale sale : sales) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float yPosition = PAGE_HEIGHT - MARGIN;

                // ── Header ──
                yPosition = drawHeader(contentStream, yPosition);

                // ── Details ──
                yPosition = drawReceiptDetails(contentStream, sale, yPosition);

                // ── Items ──
                yPosition = drawItemDetails(contentStream, sale, yPosition);

                // ── Totals ──
                yPosition = drawTotals(contentStream, sale, yPosition);

                // ── Footer ──
                drawFooter(contentStream, yPosition);
            }
        }

        document.save(outputPath);
        document.close();
    }

    /**
     * Draw the receipt header with title and business info.
     */
    private static float drawHeader(PDPageContentStream contentStream, float yPosition) throws IOException {
        // ── Company/Receipt Title ──
        contentStream.setFont(FONT_BOLD, 24);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("SALES RECEIPT");
        contentStream.endText();

        yPosition -= 30;

        // ── Divider ──
        drawLine(contentStream, MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition);
        yPosition -= 15;

        return yPosition;
    }

    /**
     * Draw receipt details (receipt ID, date, customer).
     */
    private static float drawReceiptDetails(PDPageContentStream contentStream, Sale sale, float yPosition) throws IOException {
        contentStream.setFont(FONT_REGULAR, 11);

        // Receipt ID
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Receipt ID: " + formatReceiptId(sale.getId()));
        contentStream.endText();
        yPosition -= LINE_HEIGHT;

        // Date
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN, yPosition);
        contentStream.showText("Date: " + sale.getDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
        contentStream.endText();
        yPosition -= LINE_HEIGHT;

        // Customer (if available)
        if (sale.getCustomerName() != null && !sale.getCustomerName().isBlank()) {
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN, yPosition);
            contentStream.showText("Customer: " + sale.getCustomerName());
            contentStream.endText();
            yPosition -= LINE_HEIGHT;
        }

        yPosition -= 10;
        drawLine(contentStream, MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition);
        yPosition -= 15;

        return yPosition;
    }

    /**
     * Draw itemized details of the sale.
     */
    private static float drawItemDetails(PDPageContentStream contentStream, Sale sale, float yPosition) throws IOException {
        // ── Item Header ──
        contentStream.setFont(FONT_BOLD, 10);
        float col1 = MARGIN;
        float col2 = MARGIN + 180;
        float col3 = MARGIN + 260;
        float col4 = PAGE_WIDTH - MARGIN - 80;

        contentStream.beginText();
        contentStream.newLineAtOffset(col1, yPosition);
        contentStream.showText("Product");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(col2, yPosition);
        contentStream.showText("Qty");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(col3, yPosition);
        contentStream.showText("Unit Price");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(col4, yPosition);
        contentStream.showText("Amount");
        contentStream.endText();

        yPosition -= LINE_HEIGHT + 5;
        drawLine(contentStream, MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition);
        yPosition -= 15;

        // ── Item Details ──
        contentStream.setFont(FONT_REGULAR, 10);

        // Product Name (truncated if too long)
        String productName = sale.getProductName();
        if (productName.length() > 25) {
            productName = productName.substring(0, 22) + "...";
        }

        contentStream.beginText();
        contentStream.newLineAtOffset(col1, yPosition);
        contentStream.showText(productName);
        contentStream.endText();

        // Quantity
        contentStream.beginText();
        contentStream.newLineAtOffset(col2, yPosition);
        contentStream.showText(String.valueOf(sale.getQuantity()));
        contentStream.endText();

        // Unit Price
        contentStream.beginText();
        contentStream.newLineAtOffset(col3, yPosition);
        contentStream.showText(String.format("PKR %.2f", sale.getUnitPrice()));
        contentStream.endText();

        // Amount (Total Price)
        contentStream.beginText();
        contentStream.newLineAtOffset(col4, yPosition);
        contentStream.showText(String.format("PKR %.2f", sale.getTotalPrice()));
        contentStream.endText();

        yPosition -= LINE_HEIGHT + 10;
        drawLine(contentStream, MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition);
        yPosition -= 15;

        return yPosition;
    }

    /**
     * Draw totals section.
     */
    private static float drawTotals(PDPageContentStream contentStream, Sale sale, float yPosition) throws IOException {
        contentStream.setFont(FONT_BOLD, 12);

        float labelCol = PAGE_WIDTH - MARGIN - 150;
        float valueCol = PAGE_WIDTH - MARGIN - 80;

        // Subtotal
        contentStream.beginText();
        contentStream.newLineAtOffset(labelCol, yPosition);
        contentStream.showText("Subtotal:");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(valueCol, yPosition);
        contentStream.showText(String.format("PKR %.2f", sale.getTotalPrice()));
        contentStream.endText();

        yPosition -= LINE_HEIGHT;

        // Tax (placeholder for future - currently 0)
        contentStream.setFont(FONT_REGULAR, 10);
        contentStream.beginText();
        contentStream.newLineAtOffset(labelCol, yPosition);
        contentStream.showText("Tax (0%):");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(valueCol, yPosition);
        contentStream.showText("PKR 0.00");
        contentStream.endText();

        yPosition -= LINE_HEIGHT + 8;
        drawLine(contentStream, labelCol - 10, yPosition, PAGE_WIDTH - MARGIN + 10, yPosition);
        yPosition -= 12;

        // Total (Grand Total)
        contentStream.setFont(FONT_BOLD, 12);
        contentStream.beginText();
        contentStream.newLineAtOffset(labelCol, yPosition);
        contentStream.showText("TOTAL:");
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(valueCol, yPosition);
        contentStream.showText(String.format("PKR %.2f", sale.getTotalPrice()));
        contentStream.endText();

        yPosition -= LINE_HEIGHT + 15;

        return yPosition;
    }

    /**
     * Draw footer with thank you message.
     */
    private static void drawFooter(PDPageContentStream contentStream, float yPosition) throws IOException {
        contentStream.setFont(FONT_REGULAR, 10);

        drawLine(contentStream, MARGIN, yPosition, PAGE_WIDTH - MARGIN, yPosition);
        yPosition -= 15;

        String thankYou = "Thank you for your business!";
        float stringWidth = FONT_REGULAR.getStringWidth(thankYou) / 1000 * 10;
        float xCenter = (PAGE_WIDTH - stringWidth) / 2;

        contentStream.beginText();
        contentStream.newLineAtOffset(xCenter, yPosition);
        contentStream.showText(thankYou);
        contentStream.endText();

        yPosition -= LINE_HEIGHT + 5;

        String bizmind = "BizMind — SME Management System";
        float bizmindWidth = FONT_REGULAR.getStringWidth(bizmind) / 1000 * 9;
        float xCenterBiz = (PAGE_WIDTH - bizmindWidth) / 2;

        contentStream.setFont(FONT_REGULAR, 9);
        contentStream.beginText();
        contentStream.newLineAtOffset(xCenterBiz, yPosition);
        contentStream.showText(bizmind);
        contentStream.endText();
    }

    /**
     * Draw a horizontal line on the PDF.
     */
    private static void drawLine(PDPageContentStream contentStream, float x1, float y1, float x2, float y2) throws IOException {
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(x1, y1);
        contentStream.lineTo(x2, y2);
        contentStream.stroke();
    }

    /**
     * Format receipt ID with leading zeros.
     * Example: 5 becomes "RCP-00005"
     */
    private static String formatReceiptId(int id) {
        return String.format("RCP-%05d", id);
    }
}

