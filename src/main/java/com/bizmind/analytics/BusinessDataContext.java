package com.bizmind.analytics;

import com.bizmind.db.DatabaseManager;
import com.bizmind.session.SessionManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Fetches live business data from Supabase and builds a structured
 * context string to feed into the Grok system prompt.
 */
public class BusinessDataContext {

    public static String build() throws Exception {
        UUID businessId = SessionManager.getInstance().getCurrentBusiness().getId();
        String businessName = SessionManager.getInstance().getCurrentBusiness().getName();
        Connection conn = DatabaseManager.getInstance().getConnection();
        StringBuilder ctx = new StringBuilder();

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        ctx.append("=== LIVE BUSINESS DATA FOR: ").append(businessName)
           .append(" (as of ").append(today).append(") ===\n\n");

        // ── INVENTORY ──────────────────────────────────────────────
        ctx.append("INVENTORY:\n");
        PreparedStatement ps = conn.prepareStatement(
            "SELECT name, sku, category, cost_price, selling_price, quantity, min_stock " +
            "FROM products WHERE business_id = ? ORDER BY name");
        ps.setObject(1, businessId);
        ResultSet rs = ps.executeQuery();

        int totalProducts = 0, lowStockCount = 0, outOfStockCount = 0;
        StringBuilder invRows = new StringBuilder();
        while (rs.next()) {
            totalProducts++;
            int qty  = rs.getInt("quantity");
            int min  = rs.getInt("min_stock");
            double cost = rs.getDouble("cost_price");
            double sell = rs.getDouble("selling_price");
            double margin = sell > 0 ? ((sell - cost) / sell) * 100 : 0;
            String status = qty == 0 ? "OUT OF STOCK" : (qty <= min ? "LOW STOCK" : "OK");
            if (qty == 0) outOfStockCount++;
            else if (qty <= min) lowStockCount++;
            invRows.append(String.format("  • %s [%s] | Cat: %s | Qty: %d (min:%d) | Status: %s | " +
                "Cost: PKR %.0f | Price: PKR %.0f | Margin: %.1f%%\n",
                rs.getString("name"), rs.getString("sku"), rs.getString("category"),
                qty, min, status, cost, sell, margin));
        }
        ctx.append("  Total products: ").append(totalProducts)
           .append(" | Low stock: ").append(lowStockCount)
           .append(" | Out of stock: ").append(outOfStockCount).append("\n");
        ctx.append(invRows).append("\n");

        // ── SALES LAST 30 DAYS ─────────────────────────────────────
        ctx.append("SALES — LAST 30 DAYS:\n");
        PreparedStatement salesPs = conn.prepareStatement(
            "SELECT COUNT(*) as txn_count, COALESCE(SUM(total_amount),0) as total_rev " +
            "FROM sales WHERE business_id = ? AND sale_date >= NOW() - INTERVAL '30 days'");
        salesPs.setObject(1, businessId);
        ResultSet salesRs = salesPs.executeQuery();
        double totalRev = 0; int txnCount = 0;
        if (salesRs.next()) {
            totalRev = salesRs.getDouble("total_rev");
            txnCount = salesRs.getInt("txn_count");
        }
        ctx.append(String.format("  Total Revenue: PKR %.2f | Transactions: %d | Avg/Sale: PKR %.2f\n",
            totalRev, txnCount, txnCount > 0 ? totalRev / txnCount : 0));

        PreparedStatement prodSalesPs = conn.prepareStatement(
            "SELECT p.name, p.sku, SUM(si.quantity) as units_sold, " +
            "COALESCE(SUM(si.total),0) as revenue " +
            "FROM sales s JOIN sale_items si ON si.sale_id = s.id " +
            "JOIN products p ON p.id = si.product_id " +
            "WHERE s.business_id = ? AND s.sale_date >= NOW() - INTERVAL '30 days' " +
            "GROUP BY p.name, p.sku ORDER BY revenue DESC");
        prodSalesPs.setObject(1, businessId);
        ResultSet psRs = prodSalesPs.executeQuery();
        ctx.append("  Product breakdown:\n");
        boolean anySales = false;
        while (psRs.next()) {
            anySales = true;
            double rev = psRs.getDouble("revenue");
            double pct = totalRev > 0 ? (rev / totalRev) * 100 : 0;
            ctx.append(String.format("    • %s [%s]: %d units sold, PKR %.2f revenue (%.1f%% of total)\n",
                psRs.getString("name"), psRs.getString("sku"),
                psRs.getInt("units_sold"), rev, pct));
        }
        if (!anySales) ctx.append("    (no sales recorded in the last 30 days)\n");

        // ── SALES TREND: this week vs last week ────────────────────
        ctx.append("\nSALES TREND (week-over-week):\n");
        PreparedStatement trendPs = conn.prepareStatement(
            "SELECT " +
            "  COALESCE(SUM(CASE WHEN sale_date >= NOW() - INTERVAL '7 days' THEN total_amount END),0) AS this_week, " +
            "  COALESCE(SUM(CASE WHEN sale_date >= NOW() - INTERVAL '14 days' AND sale_date < NOW() - INTERVAL '7 days' THEN total_amount END),0) AS last_week " +
            "FROM sales WHERE business_id = ?");
        trendPs.setObject(1, businessId);
        ResultSet trendRs = trendPs.executeQuery();
        if (trendRs.next()) {
            double thisWeek = trendRs.getDouble("this_week");
            double lastWeek = trendRs.getDouble("last_week");
            double change = lastWeek > 0 ? ((thisWeek - lastWeek) / lastWeek) * 100 : 0;
            String arrow = thisWeek > lastWeek ? "↑ INCREASING" : (thisWeek < lastWeek ? "↓ DECREASING" : "→ STABLE");
            ctx.append(String.format("  This week: PKR %.2f | Last week: PKR %.2f | Change: %.1f%% %s\n",
                thisWeek, lastWeek, Math.abs(change), arrow));
        }

        // ── EXPENSES LAST 30 DAYS ──────────────────────────────────
        ctx.append("\nEXPENSES — LAST 30 DAYS:\n");
        PreparedStatement expPs = conn.prepareStatement(
            "SELECT COALESCE(SUM(amount),0) as total FROM expenses " +
            "WHERE business_id = ? AND expense_date >= NOW() - INTERVAL '30 days'");
        expPs.setObject(1, businessId);
        ResultSet expRs = expPs.executeQuery();
        double totalExp = expRs.next() ? expRs.getDouble("total") : 0;
        ctx.append(String.format("  Total Expenses: PKR %.2f\n", totalExp));

        PreparedStatement catPs = conn.prepareStatement(
            "SELECT category, COALESCE(SUM(amount),0) as total FROM expenses " +
            "WHERE business_id = ? AND expense_date >= NOW() - INTERVAL '30 days' " +
            "GROUP BY category ORDER BY total DESC");
        catPs.setObject(1, businessId);
        ResultSet catRs = catPs.executeQuery();
        while (catRs.next()) {
            double catTotal = catRs.getDouble("total");
            double pct = totalExp > 0 ? (catTotal / totalExp) * 100 : 0;
            ctx.append(String.format("    • %s: PKR %.2f (%.1f%%)\n",
                catRs.getString("category"), catTotal, pct));
        }

        // ── KEY METRICS ────────────────────────────────────────────
        ctx.append("\nKEY METRICS (last 30 days):\n");
        double netProfit = totalRev - totalExp;
        double profitMargin = totalRev > 0 ? (netProfit / totalRev) * 100 : 0;
        double expToRevRatio = totalRev > 0 ? (totalExp / totalRev) * 100 : 0;
        ctx.append(String.format("  Net Profit: PKR %.2f\n", netProfit));
        ctx.append(String.format("  Profit Margin: %.1f%%\n", profitMargin));
        ctx.append(String.format("  Expense-to-Revenue Ratio: %.1f%%\n", expToRevRatio));
        ctx.append(String.format("  Low Stock Products: %d | Out of Stock: %d\n",
            lowStockCount, outOfStockCount));

        return ctx.toString();
    }
}
