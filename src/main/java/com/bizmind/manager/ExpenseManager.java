package com.bizmind.manager;

import com.bizmind.model.Expense;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.time.LocalDate;

/**
 * Singleton manager for in-memory expense storage.
 * Mirrors InventoryManager pattern.
 * Covers US-16, US-17, US-18, US-19, US-22 (monthly summary).
 */
public class ExpenseManager {

    private static ExpenseManager instance;
    private final ObservableList<Expense> expenses;

    private ExpenseManager() {
        expenses = FXCollections.observableArrayList();
    }

    public static ExpenseManager getInstance() {
        if (instance == null) {
            instance = new ExpenseManager();
        }
        return instance;
    }

    public ObservableList<Expense> getExpenses() {
        return expenses;
    }

    public void addExpense(Expense expense) {
        expenses.add(expense);
    }

    public int getExpenseCount() {
        return expenses.size();
    }

    /** Returns the sum of all expense amounts — used for US-19 summary. */
    public double getTotalExpenses() {
        return expenses.stream().mapToDouble(Expense::getAmount).sum();
    }

    /** Returns sum of expenses for a specific category — used for US-19 summary. */
    public double getTotalByCategory(String category) {
        return expenses.stream()
                .filter(e -> e.getCategory().equalsIgnoreCase(category))
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    /**
     * Get expenses filtered by date range (US-22: Monthly Summary).
     *
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return FilteredList of expenses within the date range
     */
    public FilteredList<Expense> getExpensesByDateRange(LocalDate fromDate, LocalDate toDate) {
        FilteredList<Expense> filteredExpenses = new FilteredList<>(expenses);
        filteredExpenses.setPredicate(expense -> {
            LocalDate expenseDate = expense.getDate();
            return !expenseDate.isBefore(fromDate) && !expenseDate.isAfter(toDate);
        });
        return filteredExpenses;
    }

    /**
     * Get total expenses for a specific date range (US-22: Monthly Summary).
     *
     * @param fromDate Start date (inclusive)
     * @param toDate End date (inclusive)
     * @return Total expenses for the date range
     */
    public double getTotalExpensesByDateRange(LocalDate fromDate, LocalDate toDate) {
        return getExpensesByDateRange(fromDate, toDate).stream()
                .mapToDouble(Expense::getAmount)
                .sum();
    }
}
