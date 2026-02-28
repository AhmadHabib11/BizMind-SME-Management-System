package com.bizmind.controller;

import com.bizmind.manager.InventoryManager;
import com.bizmind.model.Product;
import com.bizmind.view.AddProductView;
import javafx.animation.PauseTransition;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/**
 * Handles validation and submission of the Add Product form.
 */
public class AddProductController {

    private final AddProductView view;

    public AddProductController(AddProductView view) {
        this.view = view;
    }

    public void handleSave() {
        TextField nameField       = view.nameField;
        TextField skuField        = view.skuField;
        ComboBox<String> catCombo = view.categoryCombo;
        TextField costField       = view.costPriceField;
        TextField sellField       = view.sellingPriceField;
        TextField qtyField        = view.qtyField;
        TextField minStockField   = view.minStockField;
        Label feedback            = view.feedbackLabel;

        // Reset error states
        for (TextField f : new TextField[]{nameField, skuField, costField, sellField, qtyField}) {
            f.getStyleClass().remove("input-error");
        }
        catCombo.getStyleClass().remove("input-error");

        StringBuilder errors = new StringBuilder();

        // ── Name ──
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            errors.append("• Product name is required\n");
            nameField.getStyleClass().add("input-error");
        }

        // ── SKU (required + unique) ──
        String sku = skuField.getText().trim();
        if (sku.isEmpty()) {
            errors.append("• SKU / Product Code is required\n");
            skuField.getStyleClass().add("input-error");
        } else if (isSkuDuplicate(sku)) {
            errors.append("• SKU \"" + sku + "\" already exists — must be unique\n");
            skuField.getStyleClass().add("input-error");
        }

        // ── Category ──
        String category = catCombo.getValue();
        if (category == null || category.isBlank()) {
            errors.append("• Please select a category\n");
            catCombo.getStyleClass().add("input-error");
        }

        // ── Cost Price ──
        double costPrice = 0;
        String costText = costField.getText().trim();
        if (costText.isEmpty()) {
            errors.append("• Cost price is required\n");
            costField.getStyleClass().add("input-error");
        } else {
            try {
                costPrice = Double.parseDouble(costText);
                if (costPrice <= 0) {
                    errors.append("• Cost price must be greater than 0\n");
                    costField.getStyleClass().add("input-error");
                }
            } catch (NumberFormatException e) {
                errors.append("• Cost price must be a valid number\n");
                costField.getStyleClass().add("input-error");
            }
        }

        // ── Selling Price ──
        double sellingPrice = 0;
        String sellText = sellField.getText().trim();
        if (sellText.isEmpty()) {
            errors.append("• Selling price is required\n");
            sellField.getStyleClass().add("input-error");
        } else {
            try {
                sellingPrice = Double.parseDouble(sellText);
                if (sellingPrice <= 0) {
                    errors.append("• Selling price must be greater than 0\n");
                    sellField.getStyleClass().add("input-error");
                } else if (sellingPrice <= costPrice && costPrice > 0) {
                    errors.append("• Selling price must be greater than cost price\n");
                    sellField.getStyleClass().add("input-error");
                }
            } catch (NumberFormatException e) {
                errors.append("• Selling price must be a valid number\n");
                sellField.getStyleClass().add("input-error");
            }
        }

        // ── Quantity ──
        int quantity = 0;
        String qtyText = qtyField.getText().trim();
        if (qtyText.isEmpty()) {
            errors.append("• Quantity in stock is required\n");
            qtyField.getStyleClass().add("input-error");
        } else {
            try {
                quantity = Integer.parseInt(qtyText);
                if (quantity < 0) {
                    errors.append("• Quantity cannot be negative\n");
                    qtyField.getStyleClass().add("input-error");
                }
            } catch (NumberFormatException e) {
                errors.append("• Quantity must be a whole number\n");
                qtyField.getStyleClass().add("input-error");
            }
        }

        // ── Minimum Stock (optional, defaults to 0) ──
        int minStock = 0;
        String minText = minStockField.getText().trim();
        if (!minText.isEmpty()) {
            try {
                minStock = Integer.parseInt(minText);
                if (minStock < 0) {
                    errors.append("• Minimum stock level cannot be negative\n");
                    minStockField.getStyleClass().add("input-error");
                }
            } catch (NumberFormatException e) {
                errors.append("• Minimum stock level must be a whole number\n");
                minStockField.getStyleClass().add("input-error");
            }
        }

        // ── Show error or save ──
        if (errors.length() > 0) {
            showFeedback(feedback, errors.toString().trim(), true);
            return;
        }

        String description = view.descriptionArea.getText().trim();
        String imagePath   = view.imagePathLabel.getText().equals("No image selected") ? "" : view.imagePathLabel.getText();

        Product product = new Product(name, sku, category, description,
                costPrice, sellingPrice, quantity, minStock, imagePath);
        InventoryManager.getInstance().addProduct(product);

        showFeedback(feedback, "✓ Product \"" + name + "\" saved successfully!", false);

        // Auto-clear and navigate back after brief delay
        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(e -> view.clearForm());
        pause.play();
    }

    private boolean isSkuDuplicate(String sku) {
        return InventoryManager.getInstance().getProducts().stream()
                .anyMatch(p -> p.getSku().equalsIgnoreCase(sku));
    }

    private void showFeedback(Label label, String message, boolean isError) {
        label.setText(message);
        label.getStyleClass().removeAll("feedback-success", "feedback-error");
        label.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
        label.setVisible(true);
        label.setManaged(true);

        if (!isError) {
            PauseTransition pause = new PauseTransition(Duration.seconds(5));
            pause.setOnFinished(e -> {
                label.setVisible(false);
                label.setManaged(false);
            });
            pause.play();
        }
    }
}

