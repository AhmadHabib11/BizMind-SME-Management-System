package com.bizmind.controller;

import com.bizmind.manager.InventoryManager;
import com.bizmind.manager.SalesManager;
import com.bizmind.model.Product;
import com.bizmind.model.Sale;
import com.bizmind.view.RecordSaleView;
import javafx.animation.PauseTransition;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.time.LocalDate;

/**
 * Handles validation and submission of the Record Sale form.
 * Covers US-11 (Record Product Sale), US-12 (Automatic Stock Deduction After Sale).
 * Mirrors ExpenseController validation pattern.
 */
public class RecordSaleController {

    private final RecordSaleView view;

    public RecordSaleController(RecordSaleView view) {
        this.view = view;
    }

    /**
     * Handle save button click — validate form and record sale.
     * US-11: Record the sale
     * US-12: Stock deduction happens automatically in SalesManager.addSale()
     */
    public void handleSave() {
        // ── Clear previous error states ──
        clearErrorStates();

        StringBuilder errors = new StringBuilder();

        // ── Product Selection (required) ──
        Product selectedProduct = view.productCombo.getValue();
        if (selectedProduct == null) {
            errors.append("• Please select a product\n");
            view.productCombo.getStyleClass().add("input-error");
        }

        // ── Quantity (required, must be > 0 and <= available stock) ──
        int quantity = 0;
        String qtyText = view.quantityField.getText().trim();
        if (qtyText.isEmpty()) {
            errors.append("• Quantity is required\n");
            view.quantityField.getStyleClass().add("input-error");
        } else {
            try {
                quantity = Integer.parseInt(qtyText);
                if (quantity <= 0) {
                    errors.append("• Quantity must be greater than 0\n");
                    view.quantityField.getStyleClass().add("input-error");
                } else if (selectedProduct != null && quantity > selectedProduct.getQuantity()) {
                    // US-12 validation: Check available stock
                    errors.append("• Insufficient stock! Available: " + selectedProduct.getQuantity() + "\n");
                    view.quantityField.getStyleClass().add("input-error");
                }
            } catch (NumberFormatException e) {
                errors.append("• Quantity must be a valid number\n");
                view.quantityField.getStyleClass().add("input-error");
            }
        }

        // ── Unit Price (required, must be > 0) ──
        double unitPrice = 0;
        String priceText = view.unitPriceField.getText().trim();
        if (priceText.isEmpty()) {
            errors.append("• Unit price is required\n");
            view.unitPriceField.getStyleClass().add("input-error");
        } else {
            try {
                unitPrice = Double.parseDouble(priceText);
                if (unitPrice <= 0) {
                    errors.append("• Unit price must be greater than 0\n");
                    view.unitPriceField.getStyleClass().add("input-error");
                }
            } catch (NumberFormatException e) {
                errors.append("• Unit price must be a valid number\n");
                view.unitPriceField.getStyleClass().add("input-error");
            }
        }

        // ── Date (required) ──
        LocalDate date = view.datePicker.getValue();
        if (date == null) {
            errors.append("• Please select a date\n");
            view.datePicker.getStyleClass().add("input-error");
        }

        // ── Show errors or save ──
        if (errors.length() > 0) {
            showFeedback(view.feedbackLabel, errors.toString().trim(), true);
            return;
        }

        // ── All validation passed, record the sale ──
        try {
            String customerName = view.customerNameField.getText().trim();
            String notes = view.notesArea.getText().trim();

            // Create and add sale (US-11 & US-12 in SalesManager.addSale())
            Sale sale = new Sale(
                    selectedProduct.getName(),
                    selectedProduct.getSku(),
                    quantity,
                    unitPrice,
                    date,
                    customerName,
                    notes
            );

            SalesManager.getInstance().addSale(sale);

            // Success feedback
            double totalAmount = quantity * unitPrice;
            showFeedback(
                    view.feedbackLabel,
                    String.format("✓ Sale recorded successfully! (PKR %.2f)", totalAmount),
                    false
            );

            // Clear form after successful save
            PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
            pause.setOnFinished(e -> view.clearForm());
            pause.play();

        } catch (IllegalArgumentException ex) {
            // Stock validation or product not found error from SalesManager
            showFeedback(view.feedbackLabel, "✗ Error: " + ex.getMessage(), true);
        } catch (Exception ex) {
            // Unexpected error
            showFeedback(view.feedbackLabel, "✗ Unexpected error: " + ex.getMessage(), true);
        }
    }

    /**
     * Update available stock display when product is selected.
     * Provides real-time feedback on inventory availability.
     *
     * @param product Selected product
     */
    public void updateAvailableStock(Product product) {
        if (product != null) {
            int available = product.getQuantity();
            String stock = "Available: " + available + " units";
            view.availableStockLabel.setText(stock);

            // Color code: green if good stock, orange if low, red if out
            view.availableStockLabel.getStyleClass().removeAll("stock-available", "stock-low", "stock-none");
            if (available > 0) {
                if (available <= product.getMinimumStock()) {
                    view.availableStockLabel.getStyleClass().add("stock-low");
                } else {
                    view.availableStockLabel.getStyleClass().add("stock-available");
                }
            } else {
                view.availableStockLabel.getStyleClass().add("stock-none");
            }
        } else {
            view.availableStockLabel.setText("Select a product");
            view.availableStockLabel.getStyleClass().removeAll("stock-available", "stock-low", "stock-none");
        }
    }

    /**
     * Auto-populate unit price from selected product's selling price.
     * User can override if needed.
     *
     * @param product Selected product
     */
    public void autofillUnitPrice(Product product) {
        if (product != null) {
            view.unitPriceField.setText(String.valueOf(product.getSellingPrice()));
        } else {
            view.unitPriceField.clear();
        }
    }

    /**
     * Auto-calculate and display total price (quantity × unit price).
     * Provides real-time feedback to user.
     */
    public void updateTotalPrice() {
        try {
            int quantity = Integer.parseInt(view.quantityField.getText().trim());
            double unitPrice = Double.parseDouble(view.unitPriceField.getText().trim());
            double total = quantity * unitPrice;
            view.totalPriceLabel.setText(String.format("PKR %.2f", total));
        } catch (NumberFormatException e) {
            view.totalPriceLabel.setText("—");
        }
    }

    /**
     * Clear all form error states.
     */
    private void clearErrorStates() {
        view.productCombo.getStyleClass().remove("input-error");
        view.quantityField.getStyleClass().remove("input-error");
        view.unitPriceField.getStyleClass().remove("input-error");
        view.datePicker.getStyleClass().remove("input-error");
    }

    /**
     * Show feedback message with color coding.
     *
     * @param label Label to display message in
     * @param message Message text
     * @param isError True for error (red), false for success (green)
     */
    private void showFeedback(Label label, String message, boolean isError) {
        label.setText(message);
        label.getStyleClass().removeAll("feedback-success", "feedback-error");
        label.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
        label.setVisible(true);
    }
}

