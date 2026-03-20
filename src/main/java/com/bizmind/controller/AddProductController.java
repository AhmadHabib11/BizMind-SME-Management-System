package com.bizmind.controller;

import com.bizmind.manager.InventoryManager;
import com.bizmind.model.Product;
import com.bizmind.view.AddProductView;
import javafx.animation.PauseTransition;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

/**
 * Handles validation and submission of the Add Product form.
 */
public class AddProductController {

    private final AddProductView view;
    private Product editingProduct;

    public AddProductController(AddProductView view) {
        this.view = view;
    }

    public void loadProductIntoForm(Product product) {
        if (product == null) {
            return;
        }

        editingProduct = product;
        view.nameField.setText(product.getName());
        view.skuField.setText(product.getSku());
        view.categoryCombo.setValue(product.getCategory());
        view.descriptionArea.setText(product.getDescription());
        view.costPriceField.setText(String.valueOf(product.getCostPrice()));
        view.sellingPriceField.setText(String.valueOf(product.getSellingPrice()));
        view.qtyField.setText(String.valueOf(product.getQuantity()));
        view.minStockField.setText(String.valueOf(product.getMinimumStock()));

        String imagePath = product.getImagePath();
        if (imagePath == null || imagePath.isBlank()) {
            view.imagePathLabel.setText("No image selected");
            view.imagePathLabel.getStyleClass().remove("image-path-selected");
        } else {
            view.imagePathLabel.setText(imagePath);
            if (!view.imagePathLabel.getStyleClass().contains("image-path-selected")) {
                view.imagePathLabel.getStyleClass().add("image-path-selected");
            }
        }
    }

    public void handleSave() {
        ValidationResult result = validateForm(false);
        if (!result.valid()) {
            showFeedback(view.feedbackLabel, result.errorText(), true);
            showValidationAlert(result.errorText());
            return;
        }

        Product product = new Product(
                result.name(),
                result.sku(),
                result.category(),
                result.description(),
                result.costPrice(),
                result.sellingPrice(),
                result.quantity(),
                result.minStock(),
                result.imagePath()
        );
        InventoryManager.getInstance().addProduct(product);

        showFeedback(view.feedbackLabel, "✓ Product \"" + result.name() + "\" saved successfully!", false);

        // Return to inventory after successful save.
        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(e -> view.goBack());
        pause.play();
    }

    public void handleUpdate() {
        if (editingProduct == null) {
            showValidationAlert("No product selected for update.");
            return;
        }

        ValidationResult result = validateForm(true);
        if (!result.valid()) {
            showFeedback(view.feedbackLabel, result.errorText(), true);
            showValidationAlert(result.errorText());
            return;
        }

        // Update existing Product object so TableView reflects property changes.
        editingProduct.setName(result.name());
        editingProduct.setSku(result.sku());
        editingProduct.setCategory(result.category());
        editingProduct.setDescription(result.description());
        editingProduct.setCostPrice(result.costPrice());
        editingProduct.setSellingPrice(result.sellingPrice());
        editingProduct.setQuantity(result.quantity());
        editingProduct.setMinimumStock(result.minStock());
        editingProduct.setImagePath(result.imagePath());

        showFeedback(view.feedbackLabel, "✓ Product updated successfully!", false);

        PauseTransition pause = new PauseTransition(Duration.seconds(0.8));
        pause.setOnFinished(e -> view.goBack());
        pause.play();
    }

    private ValidationResult validateForm(boolean isUpdate) {
        TextField nameField       = view.nameField;
        TextField skuField        = view.skuField;
        ComboBox<String> catCombo = view.categoryCombo;
        TextField costField       = view.costPriceField;
        TextField sellField       = view.sellingPriceField;
        TextField qtyField        = view.qtyField;
        TextField minStockField   = view.minStockField;
        Label feedback            = view.feedbackLabel;

        // Reset error states
        for (TextField f : new TextField[]{nameField, skuField, costField, sellField, qtyField, minStockField}) {
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
        } else if (isSkuDuplicate(sku, isUpdate)) {
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

        // ── Minimum Stock (required, must be positive) ──
        int minStock = 0;
        String minText = minStockField.getText().trim();
        if (minText.isEmpty()) {
            errors.append("• Minimum stock level is required\n");
            minStockField.getStyleClass().add("input-error");
        } else {
            try {
                minStock = Integer.parseInt(minText);
                if (minStock <= 0) {
                    errors.append("• Minimum stock level must be a positive whole number\n");
                    minStockField.getStyleClass().add("input-error");
                }
            } catch (NumberFormatException e) {
                errors.append("• Minimum stock level must be a whole number\n");
                minStockField.getStyleClass().add("input-error");
            }
        }

        // ── Show error or save ──
        if (errors.length() > 0) {
            return ValidationResult.invalid(errors.toString().trim());
        }

        String description = view.descriptionArea.getText().trim();
        String imagePath   = view.imagePathLabel.getText().equals("No image selected") ? "" : view.imagePathLabel.getText();

        return ValidationResult.valid(name, sku, category, description, costPrice, sellingPrice, quantity, minStock, imagePath);
    }

    private boolean isSkuDuplicate(String sku, boolean isUpdate) {
        return InventoryManager.getInstance().getProducts().stream()
                .anyMatch(p -> p.getSku().equalsIgnoreCase(sku) && (!isUpdate || p != editingProduct));
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

    private void showValidationAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText("Please fix the highlighted input issues");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private record ValidationResult(
            boolean valid,
            String errorText,
            String name,
            String sku,
            String category,
            String description,
            double costPrice,
            double sellingPrice,
            int quantity,
            int minStock,
            String imagePath
    ) {
        private static ValidationResult invalid(String errorText) {
            return new ValidationResult(false, errorText, "", "", "", "", 0, 0, 0, 0, "");
        }

        private static ValidationResult valid(String name, String sku, String category, String description,
                                              double costPrice, double sellingPrice, int quantity,
                                              int minStock, String imagePath) {
            return new ValidationResult(true, "", name, sku, category, description,
                    costPrice, sellingPrice, quantity, minStock, imagePath);
        }
    }
}

