package com.bizmind.model;

import javafx.beans.property.*;
import java.time.LocalDate;

/**
 * Expense model with JavaFX properties for TableView binding.
 * Covers US-16 (Add Expense), US-17 (Categorize Expenses).
 */
public class Expense {

    private final StringProperty title;
    private final StringProperty category;
    private final DoubleProperty amount;
    private final StringProperty description;
    private final ObjectProperty<LocalDate> date;

    public Expense(String title, String category, double amount, String description, LocalDate date) {
        this.title       = new SimpleStringProperty(title);
        this.category    = new SimpleStringProperty(category);
        this.amount      = new SimpleDoubleProperty(amount);
        this.description = new SimpleStringProperty(description);
        this.date        = new SimpleObjectProperty<>(date);
    }

    // --- Title ---
    public StringProperty titleProperty()   { return title; }
    public String getTitle()                { return title.get(); }
    public void setTitle(String v)          { title.set(v); }

    // --- Category ---
    public StringProperty categoryProperty() { return category; }
    public String getCategory()              { return category.get(); }
    public void setCategory(String v)        { category.set(v); }

    // --- Amount ---
    public DoubleProperty amountProperty()  { return amount; }
    public double getAmount()               { return amount.get(); }
    public void setAmount(double v)         { amount.set(v); }

    // --- Description ---
    public StringProperty descriptionProperty() { return description; }
    public String getDescription()              { return description.get(); }
    public void setDescription(String v)        { description.set(v); }

    // --- Date ---
    public ObjectProperty<LocalDate> dateProperty() { return date; }
    public LocalDate getDate()                      { return date.get(); }
    public void setDate(LocalDate v)                { date.set(v); }
}
