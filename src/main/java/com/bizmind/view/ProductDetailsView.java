package com.bizmind.view;

import com.bizmind.model.Product;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.io.File;

/**
 * Read-only product detail page shown when a table row is clicked.
 */
public class ProductDetailsView {

    private final VBox root;
    private final Runnable onBack;

    public ProductDetailsView(Product product, Runnable onBackCallback) {
        this.onBack = onBackCallback;

        root = new VBox(0);
        root.getStyleClass().add("content-area");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("add-product-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(24);
        content.setPadding(new Insets(32, 40, 40, 40));

        content.getChildren().addAll(
                buildHeader(product),
                buildTopRow(product),
                buildDetailsGrid(product)
        );

        scroll.setContent(content);
        root.getChildren().add(scroll);
    }

    // ── Page header ──
    private HBox buildHeader(Product p) {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back to Inventory");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e -> onBack.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox titleBox = new VBox(3);
        Label title = new Label("Product Details");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Viewing: " + p.getName());
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        // View-only badge
        Label badge = new Label("👁  View Only");
        badge.getStyleClass().add("view-only-badge");

        header.getChildren().addAll(titleBox, spacer, badge, backBtn);
        return header;
    }

    // ── Top row: image + identity card ──
    private HBox buildTopRow(Product p) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.TOP_LEFT);

        // Image card
        VBox imageCard = new VBox(12);
        imageCard.getStyleClass().add("card");
        imageCard.setPadding(new Insets(20));
        imageCard.setAlignment(Pos.CENTER);
        imageCard.setMinWidth(180);
        imageCard.setMaxWidth(200);

        StackPane previewBox = new StackPane();
        previewBox.getStyleClass().add("image-preview-box");
        previewBox.setMinSize(140, 140);
        previewBox.setMaxSize(140, 140);

        if (p.getImagePath() != null && !p.getImagePath().isBlank()) {
            try {
                File imgFile = new File(p.getImagePath());
                if (imgFile.exists()) {
                    ImageView iv = new ImageView(new Image(imgFile.toURI().toString(), 130, 130, true, true));
                    iv.setFitWidth(130);
                    iv.setFitHeight(130);
                    iv.setPreserveRatio(true);
                    previewBox.getChildren().add(iv);
                } else {
                    previewBox.getChildren().add(buildNoImage());
                }
            } catch (Exception e) {
                previewBox.getChildren().add(buildNoImage());
            }
        } else {
            previewBox.getChildren().add(buildNoImage());
        }

        Label imgLabel = new Label("Product Image");
        imgLabel.getStyleClass().add("detail-section-label");
        imageCard.getChildren().addAll(previewBox, imgLabel);

        // Identity / overview card
        VBox identityCard = new VBox(16);
        identityCard.getStyleClass().add("card");
        identityCard.setPadding(new Insets(24, 28, 24, 28));
        HBox.setHgrow(identityCard, Priority.ALWAYS);

        HBox idHeader = new HBox(10);
        idHeader.setAlignment(Pos.CENTER_LEFT);
        Text icon = new Text("📦");
        icon.setStyle("-fx-font-size: 22px;");
        Label productNameLabel = new Label(p.getName());
        productNameLabel.getStyleClass().add("detail-product-name");
        idHeader.getChildren().addAll(icon, productNameLabel);

        // Category badge + SKU
        HBox metaRow = new HBox(12);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label catBadge = new Label(p.getCategory() != null && !p.getCategory().isBlank() ? p.getCategory() : "Uncategorized");
        catBadge.getStyleClass().add("category-badge");

        Label skuLabel = new Label("SKU: " + p.getSku());
        skuLabel.getStyleClass().add("sku-label");
        metaRow.getChildren().addAll(catBadge, skuLabel);

        // Stock status
        HBox stockRow = buildStockStatusRow(p);

        // Description
        VBox descBox = new VBox(4);
        Label descTitle = new Label("Description");
        descTitle.getStyleClass().add("detail-section-label");
        String desc = (p.getDescription() != null && !p.getDescription().isBlank())
                ? p.getDescription() : "No description provided.";
        Label descValue = new Label(desc);
        descValue.getStyleClass().add("detail-description");
        descValue.setWrapText(true);
        descBox.getChildren().addAll(descTitle, descValue);

        identityCard.getChildren().addAll(idHeader, metaRow, stockRow, descBox);
        row.getChildren().addAll(imageCard, identityCard);
        return row;
    }

    // ── Detail grid: pricing + stock numbers ──
    private HBox buildDetailsGrid(Product p) {
        HBox grid = new HBox(20);

        // Pricing card
        VBox priceCard = new VBox(16);
        priceCard.getStyleClass().addAll("card", "detail-stat-card");
        priceCard.setPadding(new Insets(24, 28, 24, 28));
        HBox.setHgrow(priceCard, Priority.ALWAYS);

        Label priceTitle = new Label("💲  Pricing");
        priceTitle.getStyleClass().add("card-title");

        VBox priceGrid = new VBox(12);
        priceGrid.getChildren().addAll(
                buildDetailRow("Cost Price", String.format("Rs. %,.2f", p.getCostPrice()), "detail-value-neutral"),
                buildDetailRow("Selling Price", String.format("Rs. %,.2f", p.getSellingPrice()), "detail-value-primary"),
                buildDetailSeparator(),
                buildDetailRow("Gross Profit", String.format("Rs. %,.2f", p.getSellingPrice() - p.getCostPrice()), "detail-value-green"),
                buildDetailRow("Profit Margin", String.format("%.1f%%", ((p.getSellingPrice() - p.getCostPrice()) / p.getSellingPrice()) * 100), "detail-value-green")
        );
        priceCard.getChildren().addAll(priceTitle, priceGrid);

        // Stock card
        VBox stockCard = new VBox(16);
        stockCard.getStyleClass().addAll("card", "detail-stat-card");
        stockCard.setPadding(new Insets(24, 28, 24, 28));
        HBox.setHgrow(stockCard, Priority.ALWAYS);

        Label stockTitle = new Label("📦  Inventory");
        stockTitle.getStyleClass().add("card-title");

        VBox stockGrid = new VBox(12);
        String qtyStyle = p.getQuantity() == 0 ? "detail-value-red"
                : p.getQuantity() <= p.getMinimumStock() ? "detail-value-orange"
                : "detail-value-green";

        stockGrid.getChildren().addAll(
                buildDetailRow("Quantity in Stock", String.valueOf(p.getQuantity()), qtyStyle),
                buildDetailRow("Minimum Stock Level", String.valueOf(p.getMinimumStock()), "detail-value-neutral"),
                buildDetailSeparator(),
                buildDetailRow("Stock Value (Cost)", String.format("Rs. %,.2f", p.getCostPrice() * p.getQuantity()), "detail-value-neutral"),
                buildDetailRow("Stock Value (Retail)", String.format("Rs. %,.2f", p.getSellingPrice() * p.getQuantity()), "detail-value-primary")
        );
        stockCard.getChildren().addAll(stockTitle, stockGrid);

        grid.getChildren().addAll(priceCard, stockCard);
        return grid;
    }

    // ── Sub-helpers ──
    private HBox buildStockStatusRow(Product p) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        String statusText;
        String statusStyle;
        if (p.getQuantity() == 0) {
            statusText = "● Out of Stock";
            statusStyle = "status-out-of-stock";
        } else if (p.getQuantity() <= p.getMinimumStock()) {
            statusText = "● Low Stock";
            statusStyle = "status-low-stock";
        } else {
            statusText = "● In Stock";
            statusStyle = "status-in-stock";
        }
        Label statusBadge = new Label(statusText);
        statusBadge.getStyleClass().addAll("status-badge", statusStyle);
        row.getChildren().add(statusBadge);
        return row;
    }

    private HBox buildDetailRow(String label, String value, String valueStyle) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.getStyleClass().add("detail-row-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label val = new Label(value);
        val.getStyleClass().addAll("detail-row-value", valueStyle);

        row.getChildren().addAll(lbl, spacer, val);
        return row;
    }

    private Separator buildDetailSeparator() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #f1f5f9;");
        return sep;
    }

    private VBox buildNoImage() {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        Label icon = new Label("📷");
        icon.setStyle("-fx-font-size: 36px; -fx-text-fill: #cbd5e1;");
        Label lbl = new Label("No Image");
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        box.getChildren().addAll(icon, lbl);
        return box;
    }

    public VBox getRoot() { return root; }
}

