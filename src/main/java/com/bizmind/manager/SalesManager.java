package com.bizmind.manager;

import com.bizmind.model.Product;
import com.bizmind.model.Sale;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * Singleton manager for in-memory sales storage with inventory integration.
 * Covers US-11 (Record Product Sale), US-12 (Automatic Stock Deduction),
 * US-14 (View Sales History), US-15 (Filter Sales by Date Range).
 *
 * Key feature: When a sale is added, automatically deducts stock from InventoryManager.
 * Validates that available stock is sufficient before allowing the sale.
 */
public class SalesManager {

    private static SalesManager instance;

    private final ObservableList<Sale> sales;

    private SalesManager() {
        sales = FXCollections.observableArrayList();
    }

    public static SalesManager getInstance() {
        if (instance == null) {
            instance = new SalesManager();
        }
        return instance;
    }

    /**
     * Get all sales as an observable list for TableView binding.
     * @return ObservableList<Sale> of all sales
     */
    public ObservableList<Sale> getSales() {
        return sales;
    }

    /**
     * Add a new sale and automatically deduct stock from inventory.
     * US-11: Record the sale
     * US-12: Automatically deduct from product quantity
     *
     * @param sale Sale object to add
     * @throws IllegalArgumentException if product not found or insufficient stock
     */
    public void addSale(Sale sale) throws IllegalArgumentException {
        // Find the product in inventory by matching name or SKU
        InventoryManager invMgr = InventoryManager.getInstance();
        Product product = findProductByNameOrSku(sale.getProductName(), sale.getProductSku());

        if (product == null) {
            throw new IllegalArgumentException("Product \"" + sale.getProductName() + "\" not found in inventory.");
        }

        // US-12: Check available stock before deducting
        int availableStock = product.getQuantity();
        int saleQuantity = sale.getQuantity();

        if (saleQuantity > availableStock) {
            throw new IllegalArgumentException(
                    "Insufficient stock! Available: " + availableStock + ", Requested: " + saleQuantity
            );
        }

        // Deduct from inventory (US-12: Automatic Stock Deduction After Sale)
        int newQuantity = availableStock - saleQuantity;
        product.setQuantity(newQuantity);

        // Add sale to the list
        sales.add(sale);
    }

    /**
     * Find product by name or SKU from InventoryManager.
     * Helper method for addSale validation.
     *
     * @param productName Name of product
     * @param productSku SKU of product
     * @return Product if found, null otherwise
     */
    private Product findProductByNameOrSku(String productName, String productSku) {
        InventoryManager invMgr = InventoryManager.getInstance();
        return invMgr.getProducts().stream()
                .filter(p -> p.getName().equalsIgnoreCase(productName) || p.getSku().equalsIgnoreCase(productSku))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get total number of sales recorded.
     * @return Total sales count
     */
    public int getSaleCount() {
        return sales.size();
    }

    /**
     * Get total revenue from all sales (sum of all sale totals).
     * Used for dashboard stats and reports.
     *
     * @return Total revenue in PKR
     */
    public double getTotalSalesRevenue() {
        return sales.stream()
                .mapToDouble(Sale::getTotalPrice)
                .sum();
    }

    /**
     * Get total revenue for a specific product.
     * Useful for product-level analytics.
     *
     * @param productName Name of product
     * @return Total revenue for that product
     */
    public double getRevenueByProduct(String productName) {
        return sales.stream()
                .filter(s -> s.getProductName().equalsIgnoreCase(productName))
                .mapToDouble(Sale::getTotalPrice)
                .sum();
    }

    /**
     * Get all sales within a specific date range.
     * US-15: Filter Sales by Date Range
     *
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return FilteredList of sales within the date range
     */
    public FilteredList<Sale> getSalesByDateRange(LocalDate fromDate, LocalDate toDate) {
        FilteredList<Sale> filteredSales = new FilteredList<>(sales);
        filteredSales.setPredicate(sale -> {
            LocalDate saleDate = sale.getDate();
            return !saleDate.isBefore(fromDate) && !saleDate.isAfter(toDate);
        });
        return filteredSales;
    }

    /**
     * Get total revenue for a specific date range.
     * Used for period-based financial analysis.
     *
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return Total revenue for the date range
     */
    public double getRevenueByDateRange(LocalDate fromDate, LocalDate toDate) {
        return getSalesByDateRange(fromDate, toDate).stream()
                .mapToDouble(Sale::getTotalPrice)
                .sum();
    }

    /**
     * Get sales count for a specific date range.
     *
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return Number of sales in date range
     */
    public int getSalesCountByDateRange(LocalDate fromDate, LocalDate toDate) {
        return (int) sales.stream()
                .filter(s -> {
                    LocalDate saleDate = s.getDate();
                    return !saleDate.isBefore(fromDate) && !saleDate.isAfter(toDate);
                })
                .count();
    }

    /**
     * Get sales filtered by product name.
     *
     * @param productName Name of product
     * @return FilteredList of sales for that product
     */
    public FilteredList<Sale> getSalesByProduct(String productName) {
        FilteredList<Sale> filteredSales = new FilteredList<>(sales);
        filteredSales.setPredicate(sale -> sale.getProductName().equalsIgnoreCase(productName));
        return filteredSales;
    }

    /**
     * Get the best-selling product by total quantity sold.
     * Future use for US-24 (Best-Selling Product Identification).
     *
     * @return Product name of best seller, or empty string if no sales
     */
    public String getBestSellingProduct() {
        return sales.stream()
                .collect(Collectors.groupingBy(Sale::getProductName, Collectors.summingInt(Sale::getQuantity)))
                .entrySet().stream()
                .max((e1, e2) -> Integer.compare(e1.getValue(), e2.getValue()))
                .map(e -> e.getKey())
                .orElse("");
    }

    /**
     * Get sales count (quantity) for a specific product.
     *
     * @param productName Name of product
     * @return Total units sold for that product
     */
    public int getQuantitySoldByProduct(String productName) {
        return sales.stream()
                .filter(s -> s.getProductName().equalsIgnoreCase(productName))
                .mapToInt(Sale::getQuantity)
                .sum();
    }

    /**
     * Clear all sales (useful for testing or reset).
     * Note: This does NOT restore inventory, so use with caution.
     */
    public void clearAllSales() {
        sales.clear();
    }
}

