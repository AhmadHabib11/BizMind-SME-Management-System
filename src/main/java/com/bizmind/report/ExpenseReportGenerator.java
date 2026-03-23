package com.bizmind.report;

import com.bizmind.model.Expense;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates a professional PDF expense report using Apache PDFBox.
 *
 * ─── DEPENDENCY (add to pom.xml) ───────────────────────────────────────────
 * <dependency>
 *     <groupId>org.apache.pdfbox</groupId>
 *     <artifactId>pdfbox</artifactId>
 *     <version>3.0.2</version>
 * </dependency>
 * ───────────────────────────────────────────────────────────────────────────
 */
public class ExpenseReportGenerator {

    // ── Layout constants ──
    private static final float PAGE_W      = PDRectangle.A4.getWidth();   // 595
    private static final float PAGE_H      = PDRectangle.A4.getHeight();  // 842
    private static final float MARGIN      = 50f;
    private static final float CONTENT_W   = PAGE_W - 2 * MARGIN;

    // ── Colours (R,G,B 0-1) ──
    private static final float[] BLUE      = {0.23f, 0.51f, 0.96f};
    private static final float[] DARK      = {0.12f, 0.16f, 0.24f};
    private static final float[] GRAY      = {0.39f, 0.45f, 0.55f};
    private static final float[] LIGHT_BG  = {0.94f, 0.96f, 0.98f};
    private static final float[] ORANGE    = {0.97f, 0.62f, 0.07f};
    private static final float[] WHITE     = {1f,    1f,    1f};
    private static final float[] ROW_ALT   = {0.98f, 0.99f, 1.00f};

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ══════════════════════════════════════════════
    //  Public entry point
    // ══════════════════════════════════════════════
    public static void generate(List<Expense> expenses, File outputFile) throws IOException {

        try (PDDocument doc = new PDDocument()) {

            // ── Page 1: Cover + Summary ──
            PDPage page1 = new PDPage(PDRectangle.A4);
            doc.addPage(page1);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page1)) {
                drawPage1(cs, expenses);
            }

            // ── Page 2+: Expense table (paginated) ──
            drawExpenseTable(doc, expenses);

            doc.save(outputFile);
        }
    }

    // ══════════════════════════════════════════════
    //  Page 1 — Cover header + stat cards + category breakdown
    // ══════════════════════════════════════════════
    private static void drawPage1(PDPageContentStream cs,
                                   List<Expense> expenses) throws IOException {

        PDType1Font bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font oblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

        float y = PAGE_H;

        // ── Header banner ──
        fillRect(cs, 0, PAGE_H - 90, PAGE_W, 90, BLUE);
        drawText(cs, bold,    26, "BizMind — Expense Report",      MARGIN, PAGE_H - 38, WHITE);
        drawText(cs, regular, 10, "Generated: " + LocalDate.now().format(DATE_FMT),
                MARGIN, PAGE_H - 58, WHITE);
        drawText(cs, regular, 10, "Total Records: " + expenses.size(),
                MARGIN, PAGE_H - 72, WHITE);
        y = PAGE_H - 100;

        // ── Section title ──
        y -= 10;
        drawText(cs, bold, 13, "FINANCIAL SUMMARY", MARGIN, y, DARK);
        y -= 6;
        drawLine(cs, MARGIN, y, PAGE_W - MARGIN, y, BLUE, 1.5f);
        y -= 18;

        // ── Stat cards (2 × 2 grid) ──
        double total   = expenses.stream().mapToDouble(Expense::getAmount).sum();
        double average = expenses.isEmpty() ? 0 : total / expenses.size();
        double highest = expenses.stream().mapToDouble(Expense::getAmount).max().orElse(0);
        double lowest  = expenses.stream().mapToDouble(Expense::getAmount).min().orElse(0);

        float cardW = (CONTENT_W - 16) / 2;
        float cardH = 64;

        drawStatCard(cs, bold, regular, MARGIN,          y, cardW, cardH,
                "Total Expenses",  String.format("PKR %.2f", total),   ORANGE);
        drawStatCard(cs, bold, regular, MARGIN + cardW + 16, y, cardW, cardH,
                "No. of Records",  String.valueOf(expenses.size()),      BLUE);
        y -= cardH + 12;

        drawStatCard(cs, bold, regular, MARGIN,          y, cardW, cardH,
                "Average Expense", String.format("PKR %.2f", average),  BLUE);
        drawStatCard(cs, bold, regular, MARGIN + cardW + 16, y, cardW, cardH,
                "Highest Expense", String.format("PKR %.2f", highest),  ORANGE);
        y -= cardH + 24;

        // ── Category breakdown table ──
        drawText(cs, bold, 13, "EXPENSES BY CATEGORY", MARGIN, y, DARK);
        y -= 6;
        drawLine(cs, MARGIN, y, PAGE_W - MARGIN, y, BLUE, 1.5f);
        y -= 18;

        // Table header
        float col1 = MARGIN;
        float col2 = MARGIN + 200;
        float col3 = MARGIN + 340;
        float col4 = MARGIN + 440;
        float rowH = 22;

        fillRect(cs, MARGIN, y - rowH + 6, CONTENT_W, rowH, DARK);
        drawText(cs, bold, 10, "Category",       col1 + 6, y - 2, WHITE);
        drawText(cs, bold, 10, "No. of Entries", col2 + 6, y - 2, WHITE);
        drawText(cs, bold, 10, "Total (PKR)",    col3 + 6, y - 2, WHITE);
        drawText(cs, bold, 10, "% of Total",     col4 + 6, y - 2, WHITE);
        y -= rowH;

        Map<String, List<Expense>> grouped = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory));
        List<String> cats = new ArrayList<>(grouped.keySet());
        Collections.sort(cats);

        boolean alt = false;
        for (String cat : cats) {
            List<Expense> catExpenses = grouped.get(cat);
            double catTotal = catExpenses.stream().mapToDouble(Expense::getAmount).sum();
            double pct      = total > 0 ? (catTotal / total) * 100 : 0;

            if (alt) fillRect(cs, MARGIN, y - rowH + 6, CONTENT_W, rowH, ROW_ALT);
            drawText(cs, regular, 10, cat,                                col1 + 6, y - 2, DARK);
            drawText(cs, regular, 10, String.valueOf(catExpenses.size()), col2 + 6, y - 2, DARK);
            drawText(cs, regular, 10, String.format("%.2f", catTotal),   col3 + 6, y - 2, DARK);
            drawText(cs, regular, 10, String.format("%.1f%%", pct),      col4 + 6, y - 2, DARK);

            // Simple bar for percentage
            float barMax = 80f;
            float barLen = (float)(pct / 100.0 * barMax);
            fillRect(cs, col4 + 55, y - rowH + 10, barLen, 8, ORANGE);

            drawRowBorder(cs, MARGIN, y - rowH + 6, CONTENT_W, rowH);
            y -= rowH;
            alt = !alt;
        }

        // ── Total row ──
        fillRect(cs, MARGIN, y - rowH + 6, CONTENT_W, rowH, DARK);
        drawText(cs, bold, 10, "TOTAL",                              col1 + 6, y - 2, WHITE);
        drawText(cs, bold, 10, String.valueOf(expenses.size()),      col2 + 6, y - 2, WHITE);
        drawText(cs, bold, 10, String.format("%.2f", total),         col3 + 6, y - 2, WHITE);
        drawText(cs, bold, 10, "100.0%",                             col4 + 6, y - 2, WHITE);
        y -= rowH + 24;

        // ── Monthly totals ──
        drawText(cs, bold, 13, "MONTHLY TOTALS", MARGIN, y, DARK);
        y -= 6;
        drawLine(cs, MARGIN, y, PAGE_W - MARGIN, y, BLUE, 1.5f);
        y -= 18;

        Map<String, Double> monthly = new LinkedHashMap<>();
        expenses.stream()
                .sorted(Comparator.comparing(Expense::getDate))
                .forEach(e -> {
                    String key = e.getDate().getMonth()
                            .getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
                            + " " + e.getDate().getYear();
                    monthly.merge(key, e.getAmount(), Double::sum);
                });

        fillRect(cs, MARGIN, y - rowH + 6, CONTENT_W, rowH, DARK);
        drawText(cs, bold, 10, "Month",        MARGIN  + 6, y - 2, WHITE);
        drawText(cs, bold, 10, "Total (PKR)",  MARGIN + 200, y - 2, WHITE);
        y -= rowH;

        alt = false;
        for (Map.Entry<String, Double> entry : monthly.entrySet()) {
            if (alt) fillRect(cs, MARGIN, y - rowH + 6, CONTENT_W, rowH, ROW_ALT);
            drawText(cs, regular, 10, entry.getKey(),                     MARGIN + 6,   y - 2, DARK);
            drawText(cs, regular, 10, String.format("PKR %.2f", entry.getValue()), MARGIN + 200, y - 2, DARK);
            drawRowBorder(cs, MARGIN, y - rowH + 6, CONTENT_W, rowH);
            y -= rowH;
            alt = !alt;
            if (y < MARGIN + 40) break; // safety guard
        }

        // ── Footer ──
        drawFooter(cs, regular, 1, 1);
    }

    // ══════════════════════════════════════════════
    //  Page 2+ — Full expense records table
    // ══════════════════════════════════════════════
    private static void drawExpenseTable(PDDocument doc,
                                          List<Expense> expenses) throws IOException {

        PDType1Font bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        int totalPages    = (int) Math.ceil(expenses.size() / 22.0) + 1;
        int rowsPerPage   = 22;
        int currentPage   = 2;
        int expenseIndex  = 0;
        float rowH        = 22f;

        while (expenseIndex < expenses.size()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // Mini header
                fillRect(cs, 0, PAGE_H - 50, PAGE_W, 50, BLUE);
                drawText(cs, bold,    14, "BizMind — Expense Records",  MARGIN, PAGE_H - 25, WHITE);
                drawText(cs, regular,  9, "Continued from summary page", MARGIN, PAGE_H - 40, WHITE);

                float y = PAGE_H - 70;
                drawText(cs, bold, 12, "ALL EXPENSE RECORDS", MARGIN, y, DARK);
                y -= 6;
                drawLine(cs, MARGIN, y, PAGE_W - MARGIN, y, BLUE, 1.5f);
                y -= 16;

                // Column positions
                float c1 = MARGIN;           // Title
                float c2 = MARGIN + 155;     // Category
                float c3 = MARGIN + 285;     // Amount
                float c4 = MARGIN + 375;     // Date
                float c5 = MARGIN + 450;     // Notes

                // Table header
                fillRect(cs, MARGIN, y - rowH + 6, CONTENT_W, rowH, DARK);
                drawText(cs, bold, 9, "Title",      c1 + 4, y - 2, WHITE);
                drawText(cs, bold, 9, "Category",   c2 + 4, y - 2, WHITE);
                drawText(cs, bold, 9, "Amount",     c3 + 4, y - 2, WHITE);
                drawText(cs, bold, 9, "Date",       c4 + 4, y - 2, WHITE);
                drawText(cs, bold, 9, "Notes",      c5 + 4, y - 2, WHITE);
                y -= rowH;

                boolean alt = false;
                int rowsOnPage = 0;

                while (expenseIndex < expenses.size() && rowsOnPage < rowsPerPage) {
                    Expense e = expenses.get(expenseIndex);

                    if (alt) fillRect(cs, MARGIN, y - rowH + 6, CONTENT_W, rowH, ROW_ALT);

                    drawText(cs, regular, 9, truncate(e.getTitle(), 22),        c1 + 4, y - 2, DARK);
                    drawText(cs, regular, 9, e.getCategory(),                   c2 + 4, y - 2, DARK);
                    drawText(cs, regular, 9, String.format("PKR %.2f", e.getAmount()), c3 + 4, y - 2, DARK);
                    drawText(cs, regular, 9, e.getDate().format(DATE_FMT),      c4 + 4, y - 2, DARK);
                    drawText(cs, regular, 9, truncate(e.getDescription(), 14),  c5 + 4, y - 2, GRAY);

                    drawRowBorder(cs, MARGIN, y - rowH + 6, CONTENT_W, rowH);
                    y -= rowH;
                    alt = !alt;
                    expenseIndex++;
                    rowsOnPage++;
                }

                drawFooter(cs, regular, currentPage, totalPages);
            }
            currentPage++;
        }
    }

    // ══════════════════════════════════════════════
    //  Drawing helpers
    // ══════════════════════════════════════════════
    private static void fillRect(PDPageContentStream cs,
                                  float x, float y, float w, float h,
                                  float[] rgb) throws IOException {
        cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private static void drawText(PDPageContentStream cs, PDType1Font font,
                                  float size, String text,
                                  float x, float y, float[] rgb) throws IOException {
        cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        // Strip non-latin1 characters that PDType1Font can't encode
        String safe = text.chars()
                .filter(c -> c < 256)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        cs.showText(safe.isEmpty() ? " " : safe);
        cs.endText();
    }

    private static void drawLine(PDPageContentStream cs,
                                  float x1, float y1, float x2, float y2,
                                  float[] rgb, float width) throws IOException {
        cs.setStrokingColor(rgb[0], rgb[1], rgb[2]);
        cs.setLineWidth(width);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private static void drawRowBorder(PDPageContentStream cs,
                                       float x, float y, float w, float h) throws IOException {
        cs.setStrokingColor(0.88f, 0.90f, 0.93f);
        cs.setLineWidth(0.5f);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private static void drawStatCard(PDPageContentStream cs,
                                      PDType1Font bold, PDType1Font regular,
                                      float x, float y, float w, float h,
                                      String title, String value,
                                      float[] accentColor) throws IOException {
        // Card background
        fillRect(cs, x, y - h, w, h, LIGHT_BG);
        // Accent left border
        fillRect(cs, x, y - h, 4, h, accentColor);
        // Value
        drawText(cs, bold,    14, value, x + 12, y - 24, DARK);
        // Title
        drawText(cs, regular,  9, title, x + 12, y - 40, GRAY);
    }

    private static void drawFooter(PDPageContentStream cs,
                                    PDType1Font regular,
                                    int page, int total) throws IOException {
        float y = 30f;
        drawLine(cs, MARGIN, y + 12, PAGE_W - MARGIN, y + 12, GRAY, 0.5f);
        drawText(cs, regular, 8, "BizMind SME Management System  •  Confidential",
                MARGIN, y, GRAY);
        String pageStr = "Page " + page + " of " + total;
        float textW = pageStr.length() * 4.5f;
        drawText(cs, regular, 8, pageStr,
                PAGE_W - MARGIN - textW, y, GRAY);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
