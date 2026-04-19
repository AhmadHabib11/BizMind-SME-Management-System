package com.bizmind.manager;

import com.bizmind.db.DatabaseManager;
import com.bizmind.model.Expense;
import com.bizmind.session.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;

public class ExpenseManager {

    private static ExpenseManager instance;
    private final ObservableList<Expense> expenses = FXCollections.observableArrayList();

    private ExpenseManager() {}

    public static ExpenseManager getInstance() {
        if (instance == null) instance = new ExpenseManager();
        return instance;
    }

    public ObservableList<Expense> getExpenses() { return expenses; }

    public int getExpenseCount() { return expenses.size(); }

    /** Reload all expenses for the current business from Supabase. */
    public void refresh() {
        expenses.clear();
        UUID businessId = currentBusinessId();
        if (businessId == null) return;
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "SELECT id, title, category, amount, description, expense_date " +
                "FROM expenses WHERE business_id = ? ORDER BY expense_date DESC");
            ps.setObject(1, businessId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Expense e = new Expense(
                    rs.getString("title"),
                    rs.getString("category"),
                    rs.getDouble("amount"),
                    rs.getString("description") != null ? rs.getString("description") : "",
                    rs.getDate("expense_date").toLocalDate()
                );
                e.setDbId((UUID) rs.getObject("id"));
                expenses.add(e);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load expenses: " + e.getMessage(), e);
        }
    }

    /** Insert new expense into DB then add to list. */
    public void addExpense(Expense expense) {
        UUID businessId = currentBusinessId();
        UUID userId     = SessionManager.getInstance().getCurrentUser() != null
                          ? SessionManager.getInstance().getCurrentUser().getId() : null;
        if (businessId == null) throw new RuntimeException("No active business session.");
        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO expenses (business_id, recorded_by, title, category, amount, description, expense_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id");
            ps.setObject(1, businessId);
            ps.setObject(2, userId);
            ps.setString(3, expense.getTitle());
            ps.setString(4, expense.getCategory());
            ps.setDouble(5, expense.getAmount());
            ps.setString(6, expense.getDescription());
            ps.setDate(7, Date.valueOf(expense.getDate()));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) expense.setDbId((UUID) rs.getObject(1));
            expenses.add(0, expense);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add expense: " + e.getMessage(), e);
        }
    }

    public double getTotalExpenses() {
        return expenses.stream().mapToDouble(Expense::getAmount).sum();
    }

    public double getTotalByCategory(String category) {
        return expenses.stream()
            .filter(e -> e.getCategory().equalsIgnoreCase(category))
            .mapToDouble(Expense::getAmount).sum();
    }

    public FilteredList<Expense> getExpensesByDateRange(LocalDate from, LocalDate to) {
        FilteredList<Expense> f = new FilteredList<>(expenses);
        f.setPredicate(e -> !e.getDate().isBefore(from) && !e.getDate().isAfter(to));
        return f;
    }

    public double getTotalExpensesByDateRange(LocalDate from, LocalDate to) {
        return getExpensesByDateRange(from, to).stream().mapToDouble(Expense::getAmount).sum();
    }

    public void clear() { expenses.clear(); }

    private UUID currentBusinessId() {
        var b = SessionManager.getInstance().getCurrentBusiness();
        return b == null ? null : b.getId();
    }
}
