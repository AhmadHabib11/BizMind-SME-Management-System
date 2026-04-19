package com.bizmind.manager;

import com.bizmind.db.DatabaseManager;
import com.bizmind.model.Product;
import com.bizmind.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.util.UUID;

public class InventoryManager {

    private static InventoryManager instance;
    private final ObservableList<Product> products = FXCollections.observableArrayList();

    private InventoryManager() {}

    public static InventoryManager getInstance() {
        if (instance == null) instance = new InventoryManager();
        return instance;
    }

    public ObservableList<Product> getProducts() { return products; }

    public int getProductCount() { return products.size(); }

    /** Reload all products for the current business from Supabase. */
    public void refresh() {
        products.clear();
        UUID businessId = currentBusinessId();
        if (businessId == null) return;
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, name, sku, category, description, cost_price, selling_price, " +
                "quantity, min_stock, image_path FROM products WHERE business_id = ? ORDER BY name");
            ps.setObject(1, businessId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Product p = new Product(
                    rs.getString("name"),
                    rs.getString("sku"),
                    rs.getString("category"),
                    rs.getString("description") != null ? rs.getString("description") : "",
                    rs.getDouble("cost_price"),
                    rs.getDouble("selling_price"),
                    rs.getInt("quantity"),
                    rs.getInt("min_stock"),
                    rs.getString("image_path") != null ? rs.getString("image_path") : ""
                );
                p.setDbId((UUID) rs.getObject("id"));
                products.add(p);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load products: " + e.getMessage(), e);
        }
    }

    /** Insert new product into DB then add to list. */
    public void addProduct(Product product) {
        UUID businessId = currentBusinessId();
        if (businessId == null) throw new RuntimeException("No active business session.");
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO products (business_id, name, sku, category, description, cost_price, " +
                "selling_price, quantity, min_stock, image_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id");
            ps.setObject(1, businessId);
            ps.setString(2, product.getName());
            ps.setString(3, product.getSku());
            ps.setString(4, product.getCategory());
            ps.setString(5, product.getDescription());
            ps.setDouble(6, product.getCostPrice());
            ps.setDouble(7, product.getSellingPrice());
            ps.setInt(8, product.getQuantity());
            ps.setInt(9, product.getMinimumStock());
            ps.setString(10, product.getImagePath());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) product.setDbId((UUID) rs.getObject(1));
            products.add(product);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add product: " + e.getMessage(), e);
        }
    }

    /** Update existing product in DB (in-memory properties already updated by controller). */
    public void updateProduct(Product product) {
        if (product.getDbId() == null) return;
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE products SET name=?, sku=?, category=?, description=?, cost_price=?, " +
                "selling_price=?, quantity=?, min_stock=?, image_path=? WHERE id=?");
            ps.setString(1, product.getName());
            ps.setString(2, product.getSku());
            ps.setString(3, product.getCategory());
            ps.setString(4, product.getDescription());
            ps.setDouble(5, product.getCostPrice());
            ps.setDouble(6, product.getSellingPrice());
            ps.setInt(7, product.getQuantity());
            ps.setInt(8, product.getMinimumStock());
            ps.setString(9, product.getImagePath());
            ps.setObject(10, product.getDbId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update product: " + e.getMessage(), e);
        }
    }

    /** Delete product from DB and remove from list. */
    public void deleteProduct(Product product) {
        if (product.getDbId() != null) {
            try {
                Connection conn = DatabaseManager.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement("DELETE FROM products WHERE id=?");
                ps.setObject(1, product.getDbId());
                ps.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete product: " + e.getMessage(), e);
            }
        }
        products.remove(product);
    }

    /** Update only quantity in DB (called by SalesManager after a sale). */
    public void updateQuantityInDB(Product product, int newQty) {
        product.setQuantity(newQty);
        if (product.getDbId() == null) return;
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "UPDATE products SET quantity=? WHERE id=?");
            ps.setInt(1, newQty);
            ps.setObject(2, product.getDbId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update product quantity: " + e.getMessage(), e);
        }
    }

    public boolean isSkuTaken(String sku, Product excluding) {
        return products.stream().anyMatch(
            p -> p.getSku().equalsIgnoreCase(sku) && p != excluding);
    }

    public void clear() { products.clear(); }

    private UUID currentBusinessId() {
        var b = SessionManager.getInstance().getCurrentBusiness();
        return b == null ? null : b.getId();
    }
}
