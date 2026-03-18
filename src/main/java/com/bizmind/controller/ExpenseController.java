package com.bizmind.controller;

import com.bizmind.manager.ExpenseManager;
import com.bizmind.model.Expense;
import com.bizmind.view.ExpenseView;
import javafx.animation.PauseTransition;
import javafx.scene.control.*;
import javafx.util.Duration;

import java.time.LocalDate;

/**
 * Handles validation and submission of the Add Expense form.
 * Covers US-16 (Add Expense), US-17 (Categorize Expenses).
 */
public class ExpenseController {

    private final ExpenseView view;

    public ExpenseController(ExpenseView view) {
        this.view = view;
    }

    public void handleSave() {
        TextField        titleField    = view.titleField;
        ComboBox<String> catCombo      = view.categoryCombo;
        TextField        amountField   = view.amountField;
        DatePicker       datePicker    = view.datePicker;
        Label            feedback      = view.feedbackLabel;

        // ── Reset error states ──
        titleField.getStyleClass().remove("input-error");
        catCombo.getStyleClass().remove("input-error");
        amountField.getStyleClass().remove("input-error");
        datePicker.getStyleClass().remove("input-error");

        StringBuilder errors = new StringBuilder();

        // ── Expense Title ──
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            errors.append("• Expense title is required\n");
            titleField.getStyleClass().add("input-error");
        }

        // ── Category (US-17) ──
        String category = catCombo.getValue();
        if (category == null || category.isBlank()) {
            errors.append("• Please select a category\n");
            catCombo.getStyleClass().add("input-error");
        }

        // ── Amount ──
        double amount = 0;
        String amtText = amountField.getText().trim();
        if (amtText.isEmpty()) {
            errors.append("• Amount is required\n");
            amountField.getStyleClass().add("input-error");
        } else {
            try {
                amount = Double.parseDouble(amtText);
                if (amount <= 0) {
                    errors.append("• Amount must be greater than 0\n");
                    amountField.getStyleClass().add("input-error");
                }
            } catch (NumberFormatException e) {
                errors.append("• Amount must be a valid number\n");
                amountField.getStyleClass().add("input-error");
            }
        }

        // ── Date ──
        LocalDate date = datePicker.getValue();
        if (date == null) {
            errors.append("• Please select a date\n");
            datePicker.getStyleClass().add("input-error");
        }

        // ── Show errors or save ──
        if (errors.length() > 0) {
            showFeedback(feedback, errors.toString().trim(), true);
            return;
        }

        String description = view.descriptionArea.getText().trim();

        Expense expense = new Expense(title, category, amount, description, date);
        ExpenseManager.getInstance().addExpense(expense);

        showFeedback(feedback, "✓ Expense \"" + title + "\" saved successfully!", false);

        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(e -> view.clearForm());
        pause.play();
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
