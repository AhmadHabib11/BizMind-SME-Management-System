package com.bizmind.model;

import javafx.beans.property.*;

/**
 * Product model with JavaFX properties for TableView binding.
 * Extended for Sprint 1 with full SME-relevant fields.
 */
public class Product {

    private final StringProperty name;
    private final StringProperty sku;
    private final StringProperty category;
    private final StringProperty description;
    private final DoubleProperty costPrice;
    private final DoubleProperty sellingPrice;
    private final IntegerProperty quantity;
    private final IntegerProperty minimumStock;
    private final StringProperty imagePath;

    public Product(String name, String sku, String category, String description,
                   double costPrice, double sellingPrice, int quantity, int minimumStock,
                   String imagePath) {
        this.name = new SimpleStringProperty(name);
        this.sku = new SimpleStringProperty(sku);
        this.category = new SimpleStringProperty(category);
        this.description = new SimpleStringProperty(description);
        this.costPrice = new SimpleDoubleProperty(costPrice);
        this.sellingPrice = new SimpleDoubleProperty(sellingPrice);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.minimumStock = new SimpleIntegerProperty(minimumStock);
        this.imagePath = new SimpleStringProperty(imagePath);
    }

    // --- Name ---
    public StringProperty nameProperty() { return name; }
    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }

    // --- SKU ---
    public StringProperty skuProperty() { return sku; }
    public String getSku() { return sku.get(); }
    public void setSku(String v) { sku.set(v); }

    // --- Category ---
    public StringProperty categoryProperty() { return category; }
    public String getCategory() { return category.get(); }
    public void setCategory(String v) { category.set(v); }

    // --- Description ---
    public StringProperty descriptionProperty() { return description; }
    public String getDescription() { return description.get(); }
    public void setDescription(String v) { description.set(v); }

    // --- Cost Price ---
    public DoubleProperty costPriceProperty() { return costPrice; }
    public double getCostPrice() { return costPrice.get(); }
    public void setCostPrice(double v) { costPrice.set(v); }

    // --- Selling Price ---
    public DoubleProperty sellingPriceProperty() { return sellingPrice; }
    public double getSellingPrice() { return sellingPrice.get(); }
    public void setSellingPrice(double v) { sellingPrice.set(v); }

    // --- Quantity ---
    public IntegerProperty quantityProperty() { return quantity; }
    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int v) { quantity.set(v); }

    // --- Minimum Stock ---
    public IntegerProperty minimumStockProperty() { return minimumStock; }
    public int getMinimumStock() { return minimumStock.get(); }
    public void setMinimumStock(int v) { minimumStock.set(v); }

    // --- Image Path ---
    public StringProperty imagePathProperty() { return imagePath; }
    public String getImagePath() { return imagePath.get(); }
    public void setImagePath(String v) { imagePath.set(v); }
}
