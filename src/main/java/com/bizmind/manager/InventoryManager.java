package com.bizmind.manager;

import com.bizmind.model.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Singleton manager for in-memory product inventory.
 * Holds an ObservableList that the TableView binds to directly.
 */
public class InventoryManager {

    private static InventoryManager instance;

    private final ObservableList<Product> products;

    private InventoryManager() {
        products = FXCollections.observableArrayList();
    }

    public static InventoryManager getInstance() {
        if (instance == null) {
            instance = new InventoryManager();
        }
        return instance;
    }

    public ObservableList<Product> getProducts() {
        return products;
    }

    public void addProduct(Product product) {
        products.add(product);
    }

    public int getProductCount() {
        return products.size();
    }
}

