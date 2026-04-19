package com.bizmind.manager;

import com.bizmind.db.DatabaseManager;
import com.bizmind.model.Product;
import com.bizmind.model.Sale;
import com.bizmind.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.sql.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;
import java.util.stream.Collectors;

public class SalesManager {

    private static SalesManager instance;
    private final ObservableList<Sale> sales = FXCollections.observableArrayList();

    private SalesManager() {}

    public static SalesManager getInstance() {
        if (instance == null) instance = new SalesManager();
        return instance;
    }

    public ObservableList<Sale> getSales() { return sales; }

    public int getSaleCount() { return sales.size(); }

    /** Reload all sales for the current business from Supabase. */
    public void refresh() {
        sales.clear();
        Sale.resetIdCounter();
        UUID businessId = currentBusinessId();
        if (businessId == null) return;
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT s.id, p.name AS product_name, p.sku AS product_sku, " +
                "si.quantity, si.unit_price, s.notes, s.sale_date " +
                "FROM sales s " +
                "JOIN sale_items si ON si.sale_id = s.id " +
                "JOIN products p ON p.id = si.product_id " +
                "WHERE s.business_id = ? ORDER BY s.sale_date DESC");
            ps.setObject(1, businessId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("sale_date");
                LocalDate date = ts != null ? ts.toLocalDateTime().toLocalDate() : LocalDate.now();
                Sale sale = new Sale(
                    rs.getString("product_name"),
                    rs.getString("product_sku"),
                    rs.getInt("quantity"),
                    rs.getDouble("unit_price"),
                    date,
                    "",
                    rs.getString("notes") != null ? rs.getString("notes") : ""
                );
                sale.setDbId((UUID) rs.getObject("id"));
                sales.add(sale);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load sales: " + e.getMessage(), e);
        }
    }

    /**
     * Record a sale: validates stock, inserts to DB (sales + sale_items),
     * updates product quantity in DB and in-memory.
     */
    public void addSale(Sale sale) {
        UUID businessId = currentBusinessId();
        UUID userId     = SessionManager.getInstance().getCurrentUser() != null
                          ? SessionManager.getInstance().getCurrentUser().getId() : null;
        if (businessId == null) throw new RuntimeException("No active business session.");

        InventoryManager invMgr = InventoryManager.getInstance();
        Product product = invMgr.getProducts().stream()
            .filter(p -> p.getName().equalsIgnoreCase(sale.getProductName())
                      || p.getSku().equalsIgnoreCase(sale.getProductSku()))
            .findFirst().orElse(null);

        if (product == null)
            throw new IllegalArgumentException("Product \"" + sale.getProductName() + "\" not found in inventory.");

        if (sale.getQuantity() > product.getQuantity())
            throw new IllegalArgumentException(
                "Insufficient stock! Available: " + product.getQuantity() + ", Requested: " + sale.getQuantity());

        try {
            Connection conn = DatabaseManager.getInstance().getConnection();

            // Insert sale record
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO sales (business_id, recorded_by, total_amount, notes, sale_date) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING id");
            ps.setObject(1, businessId);
            ps.setObject(2, userId);
            ps.setDouble(3, sale.getTotalPrice());
            ps.setString(4, sale.getNotes());
            ps.setTimestamp(5, Timestamp.valueOf(sale.getDate().atStartOfDay()));
            ResultSet rs = ps.executeQuery();
            UUID saleId = null;
            if (rs.next()) saleId = (UUID) rs.getObject(1);
            sale.setDbId(saleId);

            // Insert sale_item
            PreparedStatement ps2 = conn.prepareStatement(
                "INSERT INTO sale_items (sale_id, product_id, quantity, unit_price, total) VALUES (?, ?, ?, ?, ?)");
            ps2.setObject(1, saleId);
            ps2.setObject(2, product.getDbId());
            ps2.setInt(3, sale.getQuantity());
            ps2.setDouble(4, sale.getUnitPrice());
            ps2.setDouble(5, sale.getTotalPrice());
            ps2.executeUpdate();

            // Deduct stock
            int newQty = product.getQuantity() - sale.getQuantity();
            invMgr.updateQuantityInDB(product, newQty);

            sales.add(0, sale);

        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception e) {
            throw new RuntimeException("Failed to record sale: " + e.getMessage(), e);
        }
    }

    public double getTotalSalesRevenue() {
        return sales.stream().mapToDouble(Sale::getTotalPrice).sum();
    }

    public double getNetProfit() {
        return getTotalSalesRevenue() - ExpenseManager.getInstance().getTotalExpenses();
    }

    public double getRevenueByProduct(String productName) {
        return sales.stream()
            .filter(s -> s.getProductName().equalsIgnoreCase(productName))
            .mapToDouble(Sale::getTotalPrice).sum();
    }

    public FilteredList<Sale> getSalesByDateRange(LocalDate from, LocalDate to) {
        FilteredList<Sale> f = new FilteredList<>(sales);
        f.setPredicate(s -> !s.getDate().isBefore(from) && !s.getDate().isAfter(to));
        return f;
    }

    public double getRevenueByDateRange(LocalDate from, LocalDate to) {
        return getSalesByDateRange(from, to).stream().mapToDouble(Sale::getTotalPrice).sum();
    }

    public int getSalesCountByDateRange(LocalDate from, LocalDate to) {
        return (int) sales.stream()
            .filter(s -> !s.getDate().isBefore(from) && !s.getDate().isAfter(to)).count();
    }

    public FilteredList<Sale> getSalesByProduct(String productName) {
        FilteredList<Sale> f = new FilteredList<>(sales);
        f.setPredicate(s -> s.getProductName().equalsIgnoreCase(productName));
        return f;
    }

    public String getBestSellingProduct() {
        return sales.stream()
            .collect(Collectors.groupingBy(Sale::getProductName, Collectors.summingInt(Sale::getQuantity)))
            .entrySet().stream()
            .max((a, b) -> Integer.compare(a.getValue(), b.getValue()))
            .map(e -> e.getKey()).orElse("");
    }

    public int getQuantitySoldByProduct(String productName) {
        return sales.stream().filter(s -> s.getProductName().equalsIgnoreCase(productName))
            .mapToInt(Sale::getQuantity).sum();
    }

    public double getMonthlyRevenue(YearMonth ym) {
        return getRevenueByDateRange(ym.atDay(1), ym.atEndOfMonth());
    }

    public double getMonthlyExpenses(YearMonth ym) {
        return ExpenseManager.getInstance().getTotalExpensesByDateRange(ym.atDay(1), ym.atEndOfMonth());
    }

    public double getMonthlyNetProfit(YearMonth ym) {
        return getMonthlyRevenue(ym) - getMonthlyExpenses(ym);
    }

    public void clearAllSales() { sales.clear(); }

    public void clear() { sales.clear(); }

    private UUID currentBusinessId() {
        var b = SessionManager.getInstance().getCurrentBusiness();
        return b == null ? null : b.getId();
    }
}
