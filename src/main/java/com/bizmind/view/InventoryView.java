package com.bizmind.view;

import com.bizmind.manager.InventoryManager;
import com.bizmind.model.Product;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Inventory list view — shows the product table and navigates to Add/Detail pages.
 */
public class InventoryView {

    private final StackPane hostPane;
    private final VBox root;

    public InventoryView(StackPane hostPane) {
        this.hostPane = hostPane;
        root = new VBox(24);
        root.getStyleClass().add("content-area");
        root.setPadding(new Insets(32, 40, 32, 40));

        root.getChildren().addAll(
                buildHeader(),
                buildTableCard()
        );
    }

    // ── Page header ──
    private HBox buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        Text icon = new Text("📦");
        icon.setStyle("-fx-font-size: 26px;");

        VBox titleBox = new VBox(2);
        Label title = new Label("Inventory Management");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("View your products or add a new one to the inventory");
        subtitle.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("＋  Add New Product");
        addBtn.getStyleClass().add("primary-btn");
        addBtn.setMinHeight(42);
        addBtn.setOnAction(e -> openAddProduct());

        header.getChildren().addAll(icon, titleBox, spacer, addBtn);
        return header;
    }

    // ── Table card ──
    @SuppressWarnings("unchecked")
    private VBox buildTableCard() {
        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(24, 28, 24, 28));
        VBox.setVgrow(card, Priority.ALWAYS);

        Label actionFeedback = new Label();
        actionFeedback.getStyleClass().add("feedback-label");
        actionFeedback.setVisible(false);
        actionFeedback.setManaged(false);
        actionFeedback.setWrapText(true);

        // Card header
        HBox cardHeader = new HBox(12);
        cardHeader.setAlignment(Pos.CENTER_LEFT);
        Label cardTitle = new Label("Product List");
        cardTitle.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label countLabel = new Label(InventoryManager.getInstance().getProductCount() + " products");
        countLabel.getStyleClass().add("product-count-badge");

        Label clickHint = new Label("Select a row, then click Edit or Delete");
        clickHint.getStyleClass().add("table-hint-label");

        Button editBtn = new Button("Edit Product");
        editBtn.getStyleClass().add("secondary-btn");

        Button deleteBtn = new Button("Delete Product");
        deleteBtn.getStyleClass().add("danger-btn");

        cardHeader.getChildren().addAll(cardTitle, spacer, clickHint, editBtn, deleteBtn, countLabel);

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        Label searchLabel = new Label("Search:");
        searchLabel.getStyleClass().add("field-label");

        TextField searchField = new TextField();
        searchField.setPromptText("Enter product name or SKU");
        searchField.getStyleClass().add("text-input");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        Button searchBtn = new Button("Search");
        searchBtn.getStyleClass().add("secondary-btn");

        Button clearSearchBtn = new Button("Clear");
        clearSearchBtn.getStyleClass().add("ghost-btn");

        Label categoryFilterLabel = new Label("Filter by Category:");
        categoryFilterLabel.getStyleClass().add("field-label");

        ComboBox<String> categoryFilterCombo = new ComboBox<>();
        categoryFilterCombo.getStyleClass().add("combo-input");
        categoryFilterCombo.setPromptText("All");
        categoryFilterCombo.setMinWidth(190);
        categoryFilterCombo.setPrefWidth(210);
        populateCategoryFilterOptions(categoryFilterCombo);

        // ── TableView ──
        TableView<Product> table = new TableView<>();
        table.getStyleClass().add("product-table");
        table.setPlaceholder(new Label("No products yet. Click \"＋ Add New Product\" to get started."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        VBox.setVgrow(table, Priority.ALWAYS);

        // # column
        TableColumn<Product, Number> indexCol = new TableColumn<>("#");
        indexCol.setMinWidth(48); indexCol.setMaxWidth(56); indexCol.setSortable(false);
        indexCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || getTableRow() == null || getTableRow().getIndex() < 0
                        ? null : String.valueOf(getTableRow().getIndex() + 1));
            }
        });

        // Name
        TableColumn<Product, String> nameCol = new TableColumn<>("Product Name");
        nameCol.setCellValueFactory(d -> d.getValue().nameProperty());
        nameCol.setMinWidth(160);

        // SKU
        TableColumn<Product, String> skuCol = new TableColumn<>("SKU");
        skuCol.setCellValueFactory(d -> d.getValue().skuProperty());
        skuCol.setMinWidth(100);

        // Category
        TableColumn<Product, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(d -> d.getValue().categoryProperty());
        catCol.setMinWidth(120);
        catCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                Label badge = new Label(item);
                badge.getStyleClass().add("category-badge");
                setText(null); setGraphic(badge);
            }
        });

        // Selling Price
        TableColumn<Product, Number> priceCol = new TableColumn<>("Selling Price");
        priceCol.setCellValueFactory(d -> d.getValue().sellingPriceProperty());
        priceCol.setMinWidth(120);
        priceCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("Rs. %,.2f", item.doubleValue()));
            }
        });

        // Quantity
        TableColumn<Product, Number> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(d -> d.getValue().quantityProperty());
        qtyCol.setMinWidth(70);
        qtyCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setGraphic(null); return; }
                int qty = item.intValue();
                Label badge = new Label(String.valueOf(qty));
                badge.getStyleClass().add("qty-badge");
                if (qty == 0) badge.getStyleClass().add("qty-zero");
                else if (qty <= 5) badge.getStyleClass().add("qty-low");
                setText(null); setGraphic(badge);
            }
        });

        // Min Stock
        TableColumn<Product, Number> minCol = new TableColumn<>("Min. Stock");
        minCol.setCellValueFactory(d -> d.getValue().minimumStockProperty());
        minCol.setMinWidth(80);

        // Status
        TableColumn<Product, String> statusCol = new TableColumn<>("Status");
        statusCol.setMinWidth(100); statusCol.setSortable(false);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null); setGraphic(null); return;
                }
                Product p = (Product) getTableRow().getItem();
                int qty = p.getQuantity(); int min = p.getMinimumStock();
                Label badge; String text; String style;
                if (qty == 0) { text = "Out of Stock"; style = "status-out-of-stock"; }
                else if (qty <= min) { text = "Low Stock"; style = "status-low-stock"; }
                else { text = "In Stock"; style = "status-in-stock"; }
                badge = new Label("● " + text);
                badge.getStyleClass().addAll("status-badge", style);
                setText(null); setGraphic(badge);
            }
        });

        table.getColumns().addAll(indexCol, nameCol, skuCol, catCol, priceCol, qtyCol, minCol, statusCol);

        FilteredList<Product> filteredProducts = new FilteredList<>(
            InventoryManager.getInstance().getProducts(),
            product -> true
        );
        table.setItems(filteredProducts);

        searchBtn.setOnAction(e -> applySearch(table, searchField, categoryFilterCombo, filteredProducts));
        clearSearchBtn.setOnAction(e -> clearSearch(table, searchField, categoryFilterCombo, filteredProducts));
        searchField.setOnAction(e -> applySearch(table, searchField, categoryFilterCombo, filteredProducts));
        categoryFilterCombo.setOnAction(e -> applySearch(table, searchField, categoryFilterCombo, filteredProducts));

        searchRow.getChildren().addAll(
            searchLabel,
            searchField,
            searchBtn,
            clearSearchBtn,
            categoryFilterLabel,
            categoryFilterCombo
        );
        editBtn.setOnAction(e -> handleEditSelectedProduct(table));
        deleteBtn.setOnAction(e -> handleDeleteSelectedProduct(table, actionFeedback));

        InventoryManager.getInstance().getProducts().addListener(
            (javafx.collections.ListChangeListener<Product>) c -> {
                countLabel.setText(InventoryManager.getInstance().getProductCount() + " products");
                populateCategoryFilterOptions(categoryFilterCombo);
                applySearch(table, searchField, categoryFilterCombo, filteredProducts);
            }
        );

        // Row double click → AddProductView in edit mode
        table.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getClickCount() == 2) {
                    openEditProduct(row.getItem());
                }
            });
            return row;
        });

        card.getChildren().addAll(cardHeader, searchRow, actionFeedback, table);
        return card;
    }

    private void openAddProduct() {
        AddProductView addView = new AddProductView(this::show);
        hostPane.getChildren().setAll(addView.getRoot());
    }

    private void openEditProduct(Product product) {
        AddProductView editView = new AddProductView(this::show, product);
        hostPane.getChildren().setAll(editView.getRoot());
    }

    private void handleEditSelectedProduct(TableView<Product> table) {
        Product selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Edit Product");
            alert.setHeaderText(null);
            alert.setContentText("Please select a product to edit");
            alert.showAndWait();
            return;
        }

        openEditProduct(selected);
    }

    private void handleDeleteSelectedProduct(TableView<Product> table, Label actionFeedback) {
        Product selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Delete Product");
            alert.setHeaderText(null);
            alert.setContentText("Please select a product to delete");
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Product");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete this product?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            InventoryManager.getInstance().getProducts().remove(selected);
            table.refresh();
            showTableFeedback(actionFeedback, "Product deleted successfully", false);
        }
    }

    private void showTableFeedback(Label label, String message, boolean isError) {
        label.setText(message);
        label.getStyleClass().removeAll("feedback-success", "feedback-error");
        label.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
        label.setVisible(true);
        label.setManaged(true);

        if (!isError) {
            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> {
                label.setVisible(false);
                label.setManaged(false);
            });
            pause.play();
        }
    }

    private void applySearch(TableView<Product> table, TextField searchField,
                             ComboBox<String> categoryFilterCombo, FilteredList<Product> filteredProducts) {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String selectedCategory = categoryFilterCombo.getValue() == null ? "All" : categoryFilterCombo.getValue();
        boolean hasCategoryFilter = !"All".equalsIgnoreCase(selectedCategory);

        filteredProducts.setPredicate(product -> {
            String name = product.getName() == null ? "" : product.getName().toLowerCase();
            String sku = product.getSku() == null ? "" : product.getSku().toLowerCase();
            String category = product.getCategory() == null ? "" : product.getCategory();

            boolean matchesSearch = query.isEmpty() || name.contains(query) || sku.contains(query);
            boolean matchesCategory = !hasCategoryFilter || category.equalsIgnoreCase(selectedCategory);

            return matchesSearch && matchesCategory;
        });

        if (filteredProducts.isEmpty() && (!query.isEmpty() || hasCategoryFilter)) {
            table.setPlaceholder(new Label("No products found"));
        } else {
            table.setPlaceholder(new Label("No products yet. Click \"＋ Add New Product\" to get started."));
        }
    }

    private void clearSearch(TableView<Product> table, TextField searchField,
                             ComboBox<String> categoryFilterCombo, FilteredList<Product> filteredProducts) {
        searchField.clear();
        categoryFilterCombo.setValue("All");
        applySearch(table, searchField, categoryFilterCombo, filteredProducts);
    }

    private void populateCategoryFilterOptions(ComboBox<String> categoryFilterCombo) {
        String previousSelection = categoryFilterCombo.getValue();

        List<String> categories = InventoryManager.getInstance().getProducts().stream()
                .map(Product::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        ObservableList<String> items = FXCollections.observableArrayList();
        items.add("All");
        items.addAll(categories);

        categoryFilterCombo.setItems(items);
        if (previousSelection != null && items.contains(previousSelection)) {
            categoryFilterCombo.setValue(previousSelection);
        } else {
            categoryFilterCombo.setValue("All");
        }
    }

    private void openProductDetails(Product product) {
        ProductDetailsView detailsView = new ProductDetailsView(product, this::show);
        hostPane.getChildren().setAll(detailsView.getRoot());
    }

    public void show() {
        hostPane.getChildren().setAll(root);
    }

    public VBox getRoot() { return root; }
}
