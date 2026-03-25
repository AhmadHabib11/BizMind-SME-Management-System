package com.bizmind.model;

import javafx.beans.property.*;
import java.time.LocalDate;

/**
 * Sale model with JavaFX properties for TableView binding.
 * Covers US-11 (Record Product Sale), US-14 (View Sales History), US-15 (Filter Sales).
 * Mirrors Expense.java structure for consistency.
 */
public class Sale {

    private final IntegerProperty id;
    private final StringProperty productName;
    private final StringProperty productSku;
    private final IntegerProperty quantity;
    private final DoubleProperty unitPrice;
    private final DoubleProperty totalPrice;
    private final ObjectProperty<LocalDate> date;
    private final StringProperty customerName;
    private final StringProperty notes;

    // Static counter for auto-incrementing ID
    private static int nextId = 1;

    /**
     * Constructor with all fields.
     * Total price is calculated from quantity and unit price.
     */
    public Sale(String productName, String productSku, int quantity, double unitPrice,
                LocalDate date, String customerName, String notes) {
        this.id = new SimpleIntegerProperty(nextId++);
        this.productName = new SimpleStringProperty(productName);
        this.productSku = new SimpleStringProperty(productSku);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.unitPrice = new SimpleDoubleProperty(unitPrice);
        this.totalPrice = new SimpleDoubleProperty(quantity * unitPrice);
        this.date = new SimpleObjectProperty<>(date);
        this.customerName = new SimpleStringProperty(customerName);
        this.notes = new SimpleStringProperty(notes);
    }

    /**
     * Constructor without customer/notes (optional fields).
     */
    public Sale(String productName, String productSku, int quantity, double unitPrice, LocalDate date) {
        this(productName, productSku, quantity, unitPrice, date, "", "");
    }

    // --- ID ---
    public IntegerProperty idProperty() { return id; }
    public int getId() { return id.get(); }
    public void setId(int v) { id.set(v); }

    // --- Product Name ---
    public StringProperty productNameProperty() { return productName; }
    public String getProductName() { return productName.get(); }
    public void setProductName(String v) { productName.set(v); }

    // --- Product SKU ---
    public StringProperty productSkuProperty() { return productSku; }
    public String getProductSku() { return productSku.get(); }
    public void setProductSku(String v) { productSku.set(v); }

    // --- Quantity ---
    public IntegerProperty quantityProperty() { return quantity; }
    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int v) {
        quantity.set(v);
        updateTotalPrice();
    }

    // --- Unit Price ---
    public DoubleProperty unitPriceProperty() { return unitPrice; }
    public double getUnitPrice() { return unitPrice.get(); }
    public void setUnitPrice(double v) {
        unitPrice.set(v);
        updateTotalPrice();
    }

    // --- Total Price (Calculated) ---
    public DoubleProperty totalPriceProperty() { return totalPrice; }
    public double getTotalPrice() { return totalPrice.get(); }

    /**
     * Internal method to recalculate total price when quantity or unit price changes.
     */
    private void updateTotalPrice() {
        double total = quantity.get() * unitPrice.get();
        totalPrice.set(total);
    }

    // --- Date ---
    public ObjectProperty<LocalDate> dateProperty() { return date; }
    public LocalDate getDate() { return date.get(); }
    public void setDate(LocalDate v) { date.set(v); }

    // --- Customer Name (Optional) ---
    public StringProperty customerNameProperty() { return customerName; }
    public String getCustomerName() { return customerName.get(); }
    public void setCustomerName(String v) { customerName.set(v); }

    // --- Notes (Optional) ---
    public StringProperty notesProperty() { return notes; }
    public String getNotes() { return notes.get(); }
    public void setNotes(String v) { notes.set(v); }

    /**
     * Reset the ID counter (useful for testing).
     * In production, this would be managed by a database.
     */
    public static void resetIdCounter() {
        nextId = 1;
    }
}

