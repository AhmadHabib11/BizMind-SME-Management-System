package com.bizmind.view;

import com.bizmind.controller.RecordSaleController;
import com.bizmind.manager.InventoryManager;
import com.bizmind.manager.SalesManager;
import com.bizmind.model.Product;
import com.bizmind.model.Sale;
import com.bizmind.report.SalesReceiptGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Sales Module UI — US-11, US-12, US-14, US-15.
 * Two-column layout: Form (left) + Sales History Table (right).
 * Mirrors ExpenseView pattern for consistency.
 */
public class RecordSaleView {

    // ── Form fields (package-visible for controller) ──
    public ComboBox<Product> productCombo;
    public TextField quantityField;
    public TextField unitPriceField;
    public Label totalPriceLabel;
    public DatePicker datePicker;
    public TextField customerNameField;
    public TextArea notesArea;
    public Label feedbackLabel;
    public Label availableStockLabel;

    // ── Filter controls ──
    public DatePicker fromDatePicker;
    public DatePicker toDatePicker;
    public TextField searchField;

    // ── Table ──
    public TableView<Sale> salesTable;

    // ── Summary labels ──
    private Label totalSalesCountLabel;
    private Label totalRevenueLabel;

    private final StackPane contentArea;
    private RecordSaleController controller;
    private FilteredList<Sale> filteredSales;

    public RecordSaleView(StackPane contentArea) {
        this.contentArea = contentArea;
    }

    public void show() {
        controller = new RecordSaleController(this);
        contentArea.getChildren().clear();

        ScrollPane scroll = new ScrollPane(buildRoot());
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.getStyleClass().add("add-product-scroll");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        contentArea.getChildren().add(scroll);
    }

    // ══════════════════════════════════════════════
    //  Root
    // ══════════════════════════════════════════════
    private VBox buildRoot() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(32, 36, 32, 36));
        root.getStyleClass().add("content-area");

        // Page header
        VBox header = new VBox(4);
        Label pageTitle = new Label("Sales Management");
        pageTitle.getStyleClass().add("page-title");
        Label pageSub = new Label("Record product sales and track sales history with automatic inventory deduction.");
        pageSub.getStyleClass().add("page-subtitle");
        header.getChildren().addAll(pageTitle, pageSub);

        // Two-column row
        HBox mainRow = new HBox(20);
        mainRow.setAlignment(Pos.TOP_LEFT);

        VBox formPanel = buildFormPanel();
        VBox historyPanel = buildHistoryPanel();

        formPanel.setPrefWidth(320);
        formPanel.setMinWidth(300);
        formPanel.setMaxWidth(340);
        HBox.setHgrow(historyPanel, Priority.ALWAYS);

        mainRow.getChildren().addAll(formPanel, historyPanel);

        root.getChildren().addAll(header, mainRow);
        return root;
    }

    // ══════════════════════════════════════════════
    //  Form Panel (Left)
    // ══════════════════════════════════════════════
    private VBox buildFormPanel() {
        VBox panel = new VBox(16);
        panel.getStyleClass().add("card");
        panel.setPadding(new Insets(24, 20, 24, 20));

        // ── Section: Product Selection ──
        VBox productSection = new VBox(8);
        Label productLabel = new Label("Product *");
        productLabel.getStyleClass().add("form-label");

        productCombo = new ComboBox<>();
        productCombo.setPromptText("Select a product...");
        productCombo.setPrefWidth(300);
        productCombo.getStyleClass().add("form-input");

        // Populate combo with all products from inventory
        ObservableList<Product> products = InventoryManager.getInstance().getProducts();
        productCombo.setItems(products);

        // Custom cell factory to show product name + SKU + available qty
        productCombo.setCellFactory(param -> new ListCell<Product>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName() + " (SKU: " + item.getSku() + ") - " + item.getQuantity() + " in stock");
                }
            }
        });

        // Update button text when selection changes
        productCombo.setButtonCell(new ListCell<Product>() {
            @Override
            protected void updateItem(Product item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getName());
                }
            }
        });

        // Listen for product selection changes
        productCombo.setOnAction(e -> {
            Product selected = productCombo.getValue();
            if (selected != null) {
                controller.updateAvailableStock(selected);
                controller.autofillUnitPrice(selected);
                controller.updateTotalPrice();
            }
        });

        availableStockLabel = new Label("Select a product");
        availableStockLabel.getStyleClass().add("form-helper-text");

        productSection.getChildren().addAll(productLabel, productCombo, availableStockLabel);

        // ── Section: Quantity ──
        VBox qtySection = new VBox(8);
        Label qtyLabel = new Label("Quantity *");
        qtyLabel.getStyleClass().add("form-label");

        quantityField = new TextField();
        quantityField.setPromptText("Enter quantity...");
        quantityField.getStyleClass().add("form-input");
        quantityField.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getControlNewText();
            return text.matches("\\d*") ? change : null;
        }));

        // Listen for quantity changes to update total
        quantityField.setOnKeyReleased(e -> controller.updateTotalPrice());

        qtySection.getChildren().addAll(qtyLabel, quantityField);

        // ── Section: Unit Price ──
        VBox priceSection = new VBox(8);
        Label priceLabel = new Label("Unit Price (PKR) *");
        priceLabel.getStyleClass().add("form-label");

        unitPriceField = new TextField();
        unitPriceField.setPromptText("Enter unit price...");
        unitPriceField.getStyleClass().add("form-input");

        // Listen for price changes to update total
        unitPriceField.setOnKeyReleased(e -> controller.updateTotalPrice());

        priceSection.getChildren().addAll(priceLabel, unitPriceField);

        // ── Total Price Display (Read-only) ──
        VBox totalSection = new VBox(8);
        Label totalLabel = new Label("Total Price");
        totalLabel.getStyleClass().add("form-label");

        totalPriceLabel = new Label("—");
        totalPriceLabel.getStyleClass().add("total-price-display");

        totalSection.getChildren().addAll(totalLabel, totalPriceLabel);

        // ── Section: Date ──
        VBox dateSection = new VBox(8);
        Label dateLabel = new Label("Sale Date *");
        dateLabel.getStyleClass().add("form-label");

        datePicker = new DatePicker();
        datePicker.setValue(LocalDate.now());
        datePicker.getStyleClass().add("form-input");

        dateSection.getChildren().addAll(dateLabel, datePicker);

        // ── Section: Customer Name (Optional) ──
        VBox customerSection = new VBox(8);
        Label customerLabel = new Label("Customer Name");
        customerLabel.getStyleClass().add("form-label");

        customerNameField = new TextField();
        customerNameField.setPromptText("Optional: Enter customer name...");
        customerNameField.getStyleClass().add("form-input");

        customerSection.getChildren().addAll(customerLabel, customerNameField);

        // ── Section: Notes (Optional) ──
        VBox notesSection = new VBox(8);
        Label notesLabel = new Label("Notes");
        notesLabel.getStyleClass().add("form-label");

        notesArea = new TextArea();
        notesArea.setPromptText("Optional: Any additional notes...");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);
        notesArea.getStyleClass().add("form-input");

        notesSection.getChildren().addAll(notesLabel, notesArea);

        // ── Feedback Label ──
        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setVisible(false);

        // ── Action Buttons ──
        HBox buttonRow = new HBox(12);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        Button saveBtn = new Button("Record Sale");
        saveBtn.getStyleClass().add("primary-btn");
        saveBtn.setPrefWidth(140);
        saveBtn.setOnAction(e -> controller.handleSave());

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("secondary-btn");
        clearBtn.setPrefWidth(100);
        clearBtn.setOnAction(e -> clearForm());

        buttonRow.getChildren().addAll(saveBtn, clearBtn);

        // Assemble form panel
        panel.getChildren().addAll(
                productSection,
                qtySection,
                priceSection,
                totalSection,
                dateSection,
                customerSection,
                notesSection,
                feedbackLabel,
                new Separator(),
                buttonRow
        );

        return panel;
    }

    // ══════════════════════════════════════════════
    //  History Panel (Right)
    // ══════════════════════════════════════════════
    private VBox buildHistoryPanel() {
        VBox panel = new VBox(16);
        HBox.setHgrow(panel, Priority.ALWAYS);

        // ── Header ──
        VBox headerBox = new VBox(8);
        Label historyTitle = new Label("Sales History");
        historyTitle.getStyleClass().add("section-title");
        Label historySub = new Label("All recorded sales transactions");
        historySub.getStyleClass().add("section-subtitle");
        headerBox.getChildren().addAll(historyTitle, historySub);

        // ── Filter Section ──
        VBox filterSection = buildFilterSection();

        // ── Initialize FilteredList FIRST (needed by summary cards) ──
        filteredSales = new FilteredList<>(SalesManager.getInstance().getSales());

        // ── Summary Cards ──
        HBox summaryRow = buildSummaryCards();

        // ── Table ──
        VBox tableSection = buildTableSection();

        panel.getChildren().addAll(headerBox, filterSection, summaryRow, tableSection);
        return panel;
    }

    // ══════════════════════════════════════════════
    //  Filter Section
    // ══════════════════════════════════════════════
    private VBox buildFilterSection() {
        VBox section = new VBox(12);
        section.getStyleClass().add("card");
        section.setPadding(new Insets(12, 16, 12, 16));

        // ── Search by product/customer ──
        Label searchLabel = new Label("Search");
        searchLabel.getStyleClass().add("form-label");

        searchField = new TextField();
        searchField.setPromptText("Search by product or customer...");
        searchField.getStyleClass().add("form-input");

        // Listen for search input to filter table
        searchField.setOnKeyReleased(e -> applyFilters());

        // ── Date Range Filter (US-15) ──
        Label dateRangeLabel = new Label("Date Range");
        dateRangeLabel.getStyleClass().add("form-label");

        HBox dateRangeRow = new HBox(8);
        dateRangeRow.setAlignment(Pos.CENTER_LEFT);

        VBox fromDateBox = new VBox(4);
        Label fromLabel = new Label("From");
        fromLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");
        fromDatePicker = new DatePicker();
        fromDatePicker.setValue(LocalDate.now().minusMonths(1));
        fromDatePicker.setPrefWidth(130);
        fromDatePicker.setOnAction(e -> applyFilters());
        fromDateBox.getChildren().addAll(fromLabel, fromDatePicker);

        VBox toDateBox = new VBox(4);
        Label toLabel = new Label("To");
        toLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");
        toDatePicker = new DatePicker();
        toDatePicker.setValue(LocalDate.now());
        toDatePicker.setPrefWidth(130);
        toDatePicker.setOnAction(e -> applyFilters());
        toDateBox.getChildren().addAll(toLabel, toDatePicker);

        Button resetFilterBtn = new Button("Reset");
        resetFilterBtn.getStyleClass().add("secondary-btn");
        resetFilterBtn.setPrefWidth(60);
        resetFilterBtn.setOnAction(e -> resetFilters());

        dateRangeRow.getChildren().addAll(fromDateBox, toDateBox, resetFilterBtn);

        section.getChildren().addAll(
                searchLabel,
                searchField,
                dateRangeLabel,
                dateRangeRow
        );

        return section;
    }

    // ══════════════════════════════════════════════
    //  Summary Cards
    // ══════════════════════════════════════════════
    private HBox buildSummaryCards() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        // Card 1: Total Sales Count
        StatCardData card1Data = buildStatCardWithLabel("💰", "Total Sales", "0");
        totalSalesCountLabel = card1Data.valueLabel;

        // Card 2: Total Revenue
        StatCardData card2Data = buildStatCardWithLabel("📊", "Total Revenue", "PKR 0.00");
        totalRevenueLabel = card2Data.valueLabel;

        row.getChildren().addAll(card1Data.card, card2Data.card);

        // Update summary on sales list change
        SalesManager.getInstance().getSales().addListener((javafx.collections.ListChangeListener<Sale>) c -> updateSummary());
        updateSummary();

        return row;
    }

    /**
     * Helper class to return both the card UI and the label reference.
     */
    private static class StatCardData {
        VBox card;
        Label valueLabel;

        StatCardData(VBox card, Label valueLabel) {
            this.card = card;
            this.valueLabel = valueLabel;
        }
    }

    /**
     * Build a stat card with accessible value label.
     * Returns both the card UI and reference to the value label.
     */
    private StatCardData buildStatCardWithLabel(String emoji, String label, String value) {
        VBox card = new VBox(8);
        card.getStyleClass().add("stat-card");
        card.setPadding(new Insets(12, 16, 12, 16));

        HBox headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Text emojiText = new Text(emoji);
        emojiText.setStyle("-fx-font-size: 20;");

        VBox contentBox = new VBox(2);
        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #999;");

        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        contentBox.getChildren().addAll(labelLbl, valueLbl);
        headerBox.getChildren().addAll(emojiText, contentBox);

        card.getChildren().add(headerBox);

        return new StatCardData(card, valueLbl);
    }

    // ══════════════════════════════════════════════
    //  Table Section
    // ══════════════════════════════════════════════
    private VBox buildTableSection() {
        VBox section = new VBox(12);
        HBox.setHgrow(section, Priority.ALWAYS);
        VBox.setVgrow(section, Priority.ALWAYS);

        // Create table (filteredSales already initialized in buildHistoryPanel)
        salesTable = new TableView<>(filteredSales);
        salesTable.getStyleClass().add("data-table");
        VBox.setVgrow(salesTable, Priority.ALWAYS);

        // ── Table Columns ──
        TableColumn<Sale, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(40);

        TableColumn<Sale, String> productCol = new TableColumn<>("Product");
        productCol.setCellValueFactory(new PropertyValueFactory<>("productName"));
        productCol.setPrefWidth(100);

        TableColumn<Sale, Integer> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        qtyCol.setPrefWidth(50);

        TableColumn<Sale, Double> unitPriceCol = new TableColumn<>("Unit Price");
        unitPriceCol.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        unitPriceCol.setPrefWidth(80);
        unitPriceCol.setCellFactory(col -> new TableCell<Sale, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("PKR %.2f", item));
            }
        });

        TableColumn<Sale, Double> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalPrice"));
        totalCol.setPrefWidth(100);
        totalCol.setCellFactory(col -> new TableCell<Sale, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : String.format("PKR %.2f", item));
            }
        });

        TableColumn<Sale, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(100);
        dateCol.setCellFactory(col -> new TableCell<Sale, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")));
            }
        });

        TableColumn<Sale, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        customerCol.setPrefWidth(100);

        // ── Action Column ──
        TableColumn<Sale, Void> actionCol = new TableColumn<>("Action");
        actionCol.setPrefWidth(120);
        actionCol.setStyle("-fx-alignment: CENTER;");
        actionCol.setCellFactory(col -> new TableCell<Sale, Void>() {
            private final Button exportBtn = new Button("📄 Export");

            {
                exportBtn.getStyleClass().add("primary-btn");
                exportBtn.setStyle("-fx-font-size: 11; -fx-padding: 6 12;");
                exportBtn.setOnAction(event -> {
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        Sale sale = getTableView().getItems().get(index);
                        if (sale != null) {
                            exportSaleReceipt(sale);
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getIndex() < 0) {
                    setGraphic(null);
                } else {
                    setGraphic(exportBtn);
                }
            }
        });

        salesTable.getColumns().addAll(idCol, productCol, qtyCol, unitPriceCol, totalCol, dateCol, customerCol, actionCol);

        section.getChildren().add(salesTable);
        return section;
    }

    // ══════════════════════════════════════════════
    //  Helper Methods
    // ══════════════════════════════════════════════

    /**
     * Clear the sales entry form.
     * Called after successful sale recording.
     */
    public void clearForm() {
        productCombo.setValue(null);
        quantityField.clear();
        unitPriceField.clear();
        totalPriceLabel.setText("—");
        datePicker.setValue(LocalDate.now());
        customerNameField.clear();
        notesArea.clear();
        feedbackLabel.setVisible(false);
        availableStockLabel.setText("Select a product");
        availableStockLabel.getStyleClass().removeAll("stock-available", "stock-low", "stock-none");
    }

    /**
     * Apply all filters (search + date range) to the sales table.
     * US-15: Filter Sales by Date Range
     */
    private void applyFilters() {
        String searchText = searchField.getText().trim().toLowerCase();
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        filteredSales.setPredicate(sale -> {
            // Check date range
            LocalDate saleDate = sale.getDate();
            boolean dateMatch = !saleDate.isBefore(fromDate) && !saleDate.isAfter(toDate);

            if (!dateMatch) return false;

            // Check search text (product name or customer)
            if (searchText.isEmpty()) return true;
            return sale.getProductName().toLowerCase().contains(searchText) ||
                   sale.getCustomerName().toLowerCase().contains(searchText);
        });

        updateSummary();
    }

    /**
     * Reset all filters.
     */
    private void resetFilters() {
        searchField.clear();
        fromDatePicker.setValue(LocalDate.now().minusMonths(1));
        toDatePicker.setValue(LocalDate.now());
        filteredSales.setPredicate(s -> true);
        updateSummary();
    }

    /**
     * Update summary cards based on filtered data.
     */
    private void updateSummary() {
        int count = filteredSales.size();
        double revenue = filteredSales.stream()
                .mapToDouble(Sale::getTotalPrice)
                .sum();

        totalSalesCountLabel.setText(String.valueOf(count));
        totalRevenueLabel.setText(String.format("PKR %.2f", revenue));
    }

    /**
     * Export a sales receipt as PDF.
     * US-13: Generate Sales Receipt
     *
     * @param sale Sale to export as PDF receipt
     */
    private void exportSaleReceipt(Sale sale) {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Sales Receipt");
            fileChooser.setInitialFileName("Receipt-RCP-" + String.format("%05d", sale.getId()) + ".pdf");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            File selectedFile = fileChooser.showSaveDialog(null);
            if (selectedFile != null) {
                SalesReceiptGenerator.generateSingleReceipt(sale, selectedFile.getAbsolutePath());

                // Show success alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Receipt Exported");
                alert.setHeaderText("Success!");
                alert.setContentText("Sales receipt saved to:\n" + selectedFile.getAbsolutePath());
                alert.showAndWait();
            }
        } catch (Exception ex) {
            // Show error alert
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Export Failed");
            alert.setHeaderText("Error exporting receipt");
            alert.setContentText("Error: " + ex.getMessage());
            alert.showAndWait();
        }
    }
}

