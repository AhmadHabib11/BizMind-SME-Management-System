package com.bizmind.view;

import com.bizmind.controller.AddProductController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Full-page Add Product form — comprehensive SME product entry.
 */
public class AddProductView {

    private final VBox root;
    private final AddProductController controller;
    private final Runnable onBack;

    // Form fields (public so AddProductController can access them)
    public TextField nameField, skuField, costPriceField, sellingPriceField, qtyField, minStockField;
    public ComboBox<String> categoryCombo;
    public TextArea descriptionArea;
    public Label imagePathLabel;
    public ImageView imagePreview;
    public Label feedbackLabel;

    public AddProductView(Runnable onBackCallback) {
        this.onBack = onBackCallback;
        this.controller = new AddProductController(this);

        root = new VBox(0);
        root.getStyleClass().add("content-area");

        // Scrollable content
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("add-product-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(24);
        content.setPadding(new Insets(32, 40, 40, 40));

        content.getChildren().addAll(
                buildHeader(),
                buildBasicInfoCard(),
                buildPricingStockCard(),
                buildImageCard(),
                buildActionRow()
        );

        scroll.setContent(content);
        root.getChildren().add(scroll);
    }

    // ── Page header with back button ──
    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back to Inventory");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e -> onBack.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox titleBox = new VBox(3);
        Label title = new Label("Add New Product");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Fill in the product details below. Fields marked * are required.");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        header.getChildren().addAll(titleBox, spacer, backBtn);
        return header;
    }

    // ── Basic Information card ──
    private VBox buildBasicInfoCard() {
        VBox card = new VBox(20);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24, 28, 28, 28));

        HBox cardHeader = buildCardHeader("📋", "Basic Information");

        // Row 1: Name + SKU
        HBox row1 = new HBox(20);
        VBox nameBox = buildFieldGroup("Product Name *", "e.g. Wireless Mechanical Keyboard");
        nameField = (TextField) nameBox.getChildren().get(1);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        VBox skuBox = buildFieldGroup("SKU / Product Code *", "e.g. KBD-WL-001");
        skuField = (TextField) skuBox.getChildren().get(1);
        skuBox.setMinWidth(220);
        skuBox.setMaxWidth(260);

        row1.getChildren().addAll(nameBox, skuBox);

        // Row 2: Category + Description
        HBox row2 = new HBox(20);
        VBox categoryBox = new VBox(6);
        Label catLabel = new Label("Category *");
        catLabel.getStyleClass().add("field-label");

        categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll(
                "Electronics", "Office Supplies", "Furniture", "Clothing & Apparel",
                "Food & Beverages", "Raw Materials", "Machinery & Tools",
                "Packaging", "Stationery", "Health & Safety", "Other"
        );
        categoryCombo.setPromptText("Select a category");
        categoryCombo.getStyleClass().add("combo-input");
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        categoryCombo.setMinHeight(40);
        categoryBox.getChildren().addAll(catLabel, categoryCombo);
        categoryBox.setMinWidth(220);
        categoryBox.setMaxWidth(280);

        VBox descBox = new VBox(6);
        Label descLabel = new Label("Description");
        descLabel.getStyleClass().add("field-label");

        descriptionArea = new TextArea();
        descriptionArea.setPromptText("Brief product description — features, specs, notes...");
        descriptionArea.getStyleClass().add("text-area-input");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        descBox.getChildren().addAll(descLabel, descriptionArea);
        HBox.setHgrow(descBox, Priority.ALWAYS);

        row2.getChildren().addAll(categoryBox, descBox);

        card.getChildren().addAll(cardHeader, row1, row2);
        return card;
    }

    // ── Pricing & Stock card ──
    private VBox buildPricingStockCard() {
        VBox card = new VBox(20);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24, 28, 28, 28));

        HBox cardHeader = buildCardHeader("💲", "Pricing & Stock");

        HBox row = new HBox(20);
        row.setAlignment(Pos.BOTTOM_LEFT);

        VBox costBox = buildFieldGroup("Cost Price (PKR) *", "e.g. 800.00");
        costPriceField = (TextField) costBox.getChildren().get(1);
        HBox.setHgrow(costBox, Priority.ALWAYS);

        VBox sellBox = buildFieldGroup("Selling Price (PKR) *", "e.g. 1200.00");
        sellingPriceField = (TextField) sellBox.getChildren().get(1);
        HBox.setHgrow(sellBox, Priority.ALWAYS);

        VBox qtyBox = buildFieldGroup("Quantity in Stock *", "e.g. 50");
        qtyField = (TextField) qtyBox.getChildren().get(1);
        HBox.setHgrow(qtyBox, Priority.ALWAYS);

        VBox minStockBox = buildFieldGroup("Min. Stock Level", "e.g. 5");
        minStockField = (TextField) minStockBox.getChildren().get(1);
        HBox.setHgrow(minStockBox, Priority.ALWAYS);

        row.getChildren().addAll(costBox, sellBox, qtyBox, minStockBox);

        // Margin preview
        Label marginHint = new Label("Profit margin will be calculated automatically");
        marginHint.getStyleClass().add("field-hint");

        // Live margin calculation
        costPriceField.textProperty().addListener((obs, o, n) -> updateMarginHint(marginHint));
        sellingPriceField.textProperty().addListener((obs, o, n) -> updateMarginHint(marginHint));

        card.getChildren().addAll(cardHeader, row, marginHint);
        return card;
    }

    // ── Product Image card ──
    private VBox buildImageCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24, 28, 28, 28));

        HBox cardHeader = buildCardHeader("🖼", "Product Image");

        HBox imageRow = new HBox(24);
        imageRow.setAlignment(Pos.CENTER_LEFT);

        // Image preview box
        StackPane previewBox = new StackPane();
        previewBox.getStyleClass().add("image-preview-box");
        previewBox.setMinSize(120, 120);
        previewBox.setMaxSize(120, 120);

        imagePreview = new ImageView();
        imagePreview.setFitWidth(110);
        imagePreview.setFitHeight(110);
        imagePreview.setPreserveRatio(true);

        Label placeholderIcon = new Label("📷");
        placeholderIcon.setStyle("-fx-font-size: 36px; -fx-text-fill: #cbd5e1;");
        placeholderIcon.getStyleClass().add("image-placeholder-icon");

        previewBox.getChildren().addAll(placeholderIcon, imagePreview);

        // Image controls
        VBox imageControls = new VBox(10);
        imageControls.setAlignment(Pos.CENTER_LEFT);

        Label imageHint = new Label("Upload a product image (JPG, PNG, GIF)");
        imageHint.getStyleClass().add("field-hint");

        imagePathLabel = new Label("No image selected");
        imagePathLabel.getStyleClass().add("image-path-label");

        Button chooseBtn = new Button("📁  Browse Image");
        chooseBtn.getStyleClass().add("secondary-btn");
        chooseBtn.setOnAction(e -> handleImageSelect(chooseBtn, placeholderIcon));

        imageControls.getChildren().addAll(imageHint, chooseBtn, imagePathLabel);
        imageRow.getChildren().addAll(previewBox, imageControls);

        card.getChildren().addAll(cardHeader, imageRow);
        return card;
    }

    // ── Action row: feedback + buttons ──
    private VBox buildActionRow() {
        VBox wrapper = new VBox(12);

        feedbackLabel = new Label();
        feedbackLabel.getStyleClass().add("feedback-label");
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);
        feedbackLabel.setWrapText(true);

        HBox btnRow = new HBox(12);
        btnRow.setAlignment(Pos.CENTER_RIGHT);

        Button clearBtn = new Button("Clear Form");
        clearBtn.getStyleClass().add("ghost-btn");
        clearBtn.setOnAction(e -> clearForm());

        Button saveBtn = new Button("💾  Save Product");
        saveBtn.getStyleClass().add("primary-btn");
        saveBtn.setMinWidth(160);
        saveBtn.setOnAction(e -> controller.handleSave());

        btnRow.getChildren().addAll(clearBtn, saveBtn);
        wrapper.getChildren().addAll(feedbackLabel, btnRow);
        return wrapper;
    }

    // ── Helpers ──
    private HBox buildCardHeader(String icon, String title) {
        HBox hb = new HBox(10);
        hb.setAlignment(Pos.CENTER_LEFT);
        Text ic = new Text(icon);
        ic.setStyle("-fx-font-size: 18px;");
        Label lbl = new Label(title);
        lbl.getStyleClass().add("card-title");
        Separator sep = new Separator();
        sep.getStyleClass().add("card-separator");
        HBox.setHgrow(sep, Priority.ALWAYS);
        hb.getChildren().addAll(ic, lbl, sep);
        return hb;
    }

    public VBox buildFieldGroup(String labelText, String prompt) {
        VBox group = new VBox(6);
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("text-input");
        field.setMinHeight(40);
        group.getChildren().addAll(label, field);
        return group;
    }

    private void handleImageSelect(Button chooseBtn, Label placeholderIcon) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Select Product Image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        File file = fc.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            imagePathLabel.setText(file.getName());
            imagePathLabel.getStyleClass().add("image-path-selected");
            try {
                Image img = new Image(file.toURI().toString(), 110, 110, true, true);
                imagePreview.setImage(img);
                placeholderIcon.setVisible(false);
            } catch (Exception ignored) {}
        }
    }

    private void updateMarginHint(Label marginHint) {
        try {
            double cost = Double.parseDouble(costPriceField.getText().trim());
            double sell = Double.parseDouble(sellingPriceField.getText().trim());
            if (cost > 0 && sell > cost) {
                double margin = ((sell - cost) / sell) * 100;
                double profit = sell - cost;
                marginHint.setText(String.format("✓ Profit: Rs. %.2f  |  Margin: %.1f%%", profit, margin));
                marginHint.setStyle("-fx-text-fill: #16a34a;");
            } else if (cost > 0 && sell > 0 && sell <= cost) {
                marginHint.setText("⚠ Selling price must be greater than cost price");
                marginHint.setStyle("-fx-text-fill: #d97706;");
            } else {
                marginHint.setText("Profit margin will be calculated automatically");
                marginHint.setStyle("-fx-text-fill: #94a3b8;");
            }
        } catch (NumberFormatException ignored) {
            marginHint.setText("Profit margin will be calculated automatically");
            marginHint.setStyle("-fx-text-fill: #94a3b8;");
        }
    }

    public void clearForm() {
        nameField.clear(); skuField.clear(); costPriceField.clear();
        sellingPriceField.clear(); qtyField.clear(); minStockField.clear();
        descriptionArea.clear(); categoryCombo.setValue(null);
        imagePathLabel.setText("No image selected");
        imagePathLabel.getStyleClass().remove("image-path-selected");
        imagePreview.setImage(null);
        // Reset error styles
        for (TextField f : new TextField[]{nameField, skuField, costPriceField, sellingPriceField, qtyField}) {
            f.getStyleClass().remove("input-error");
        }
        categoryCombo.getStyleClass().remove("input-error");
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);
    }

    public VBox getRoot() { return root; }
}

