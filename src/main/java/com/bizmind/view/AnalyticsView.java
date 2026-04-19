package com.bizmind.view;

import com.bizmind.analytics.BusinessDataContext;
import com.bizmind.analytics.GrokClient;
import com.bizmind.session.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.*;

public class AnalyticsView {

    private final StackPane hostPane;
    private VBox messagesBox;
    private ScrollPane chatScroll;
    private TextField inputField;
    private Button sendBtn;

    // Conversation history kept in memory for multi-turn context
    private final List<Map<String, String>> history = new ArrayList<>();
    private String systemPrompt;
    private final GrokClient grok = new GrokClient();

    // Quick-ask prompts shown as chips
    private static final String[][] QUICK = {
        {"📊", "Business Summary",       "Give me a complete summary of my business performance this month."},
        {"🏆", "Best Sellers",           "Which products are my best sellers? Give details and recommendations."},
        {"⚠️",  "Alerts",                "Are there any alerts I should know about? Unusual expenses, sales drops, or low stock?"},
        {"🔄", "Restock Advice",         "Which products should I restock urgently? Give me a prioritized list."},
        {"💰", "Expense Analysis",       "Analyse my expenses. Are they too high compared to revenue? Where can I cut costs?"},
        {"📈", "Sales Trend",            "Analyse my sales trend. Are sales going up or down? What's driving it?"},
        {"💹", "Profit Analysis",        "Which products give me the best and worst profit margins? What should I focus on?"},
        {"🎯", "Recommendations",        "What are your top 3 actionable recommendations to improve my business right now?"},
    };

    public AnalyticsView(StackPane hostPane) {
        this.hostPane = hostPane;
    }

    public void show() {
        hostPane.getChildren().setAll(buildRoot());
        // Auto-generate opening insight in background
        autoGreet();
    }

    // ── Root layout ───────────────────────────────────────────────
    private VBox buildRoot() {
        VBox root = new VBox(0);
        root.getStyleClass().add("content-area");

        VBox chatArea = buildChatArea();
        VBox.setVgrow(chatArea, Priority.ALWAYS);
        root.getChildren().addAll(buildHeader(), chatArea, buildInputBar());
        return root;
    }

    private HBox buildHeader() {
        HBox header = new HBox(14);
        header.getStyleClass().add("analytics-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 32, 20, 32));

        Text icon = new Text("🧠");
        icon.setStyle("-fx-font-size: 28px;");

        VBox titles = new VBox(2);
        Label title = new Label("Analytics Assistant");
        title.getStyleClass().add("page-title");
        String biz = SessionManager.getInstance().getCurrentBusiness() != null
            ? SessionManager.getInstance().getCurrentBusiness().getName() : "Your Business";
        Label sub = new Label("Powered by Grok AI  •  Analyzing: " + biz);
        sub.getStyleClass().add("page-subtitle");
        titles.getChildren().addAll(title, sub);

        header.getChildren().addAll(icon, titles);
        return header;
    }

    private VBox buildChatArea() {
        messagesBox = new VBox(14);
        messagesBox.setPadding(new Insets(20, 32, 20, 32));
        messagesBox.setFillWidth(true);

        chatScroll = new ScrollPane(messagesBox);
        chatScroll.setFitToWidth(true);
        chatScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chatScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chatScroll.getStyleClass().add("analytics-scroll");
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        // Auto-scroll when content height changes
        messagesBox.heightProperty().addListener((obs, o, n) ->
            chatScroll.setVvalue(1.0));

        VBox wrapper = new VBox(chatScroll);
        VBox.setVgrow(chatScroll, Priority.ALWAYS);
        VBox.setVgrow(wrapper,    Priority.ALWAYS);
        return wrapper;
    }

    private VBox buildInputBar() {
        // Quick-ask chips
        ScrollPane chipsScroll = new ScrollPane();
        chipsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        chipsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        chipsScroll.setFitToHeight(true);
        chipsScroll.getStyleClass().add("chips-scroll");
        chipsScroll.setPrefHeight(52);

        HBox chips = new HBox(8);
        chips.setPadding(new Insets(8, 16, 8, 16));
        chips.setAlignment(Pos.CENTER_LEFT);
        for (String[] q : QUICK) {
            Button chip = new Button(q[0] + "  " + q[1]);
            chip.getStyleClass().add("chip-btn");
            String prompt = q[2];
            chip.setOnAction(e -> sendMessage(prompt));
            chips.getChildren().add(chip);
        }
        chipsScroll.setContent(chips);

        // Input row
        inputField = new TextField();
        inputField.setPromptText("Ask about your business…");
        inputField.getStyleClass().add("analytics-input");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        inputField.setOnAction(e -> sendMessage(inputField.getText().trim()));

        sendBtn = new Button("Send ➤");
        sendBtn.getStyleClass().add("primary-btn");
        sendBtn.setMinHeight(44);
        sendBtn.setOnAction(e -> sendMessage(inputField.getText().trim()));

        HBox inputRow = new HBox(10, inputField, sendBtn);
        inputRow.setPadding(new Insets(10, 16, 14, 16));
        inputRow.setAlignment(Pos.CENTER);

        Separator sep = new Separator();

        VBox bar = new VBox(sep, chipsScroll, inputRow);
        bar.getStyleClass().add("analytics-input-bar");
        return bar;
    }

    // ── Message rendering ─────────────────────────────────────────
    private void addBotMessage(String text) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);

        Label avatar = new Label("🧠");
        avatar.getStyleClass().add("bot-avatar");

        TextFlow bubble = new TextFlow();
        bubble.getStyleClass().add("bot-bubble");
        bubble.setMaxWidth(680);
        // Render line-by-line preserving bullets and newlines
        Text content = new Text(text);
        content.getStyleClass().add("bubble-text");
        bubble.getChildren().add(content);

        row.getChildren().addAll(avatar, bubble);
        Platform.runLater(() -> messagesBox.getChildren().add(row));
    }

    private void addUserMessage(String text) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_RIGHT);

        TextFlow bubble = new TextFlow();
        bubble.getStyleClass().add("user-bubble");
        bubble.setMaxWidth(560);
        Text content = new Text(text);
        content.getStyleClass().add("bubble-text");
        bubble.getChildren().add(content);

        row.getChildren().add(bubble);
        messagesBox.getChildren().add(row);
    }

    private HBox addTypingIndicator() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        Label avatar = new Label("🧠");
        avatar.getStyleClass().add("bot-avatar");
        Label dots = new Label("Analysing your business data…");
        dots.getStyleClass().add("typing-indicator");
        row.getChildren().addAll(avatar, dots);
        Platform.runLater(() -> messagesBox.getChildren().add(row));
        return row;
    }

    // ── Chat logic ────────────────────────────────────────────────
    private void autoGreet() {
        setInputEnabled(false);
        HBox typing = addTypingIndicator();

        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                // Build system prompt once with live data
                String data = BusinessDataContext.build();
                systemPrompt =
                    "You are BizMind Analytics Assistant, an intelligent business analyst. " +
                    "You analyze real-time SME business data and provide practical, actionable insights " +
                    "in a conversational, professional tone. " +
                    "Always use PKR for currency. Keep responses concise (3-5 paragraphs max). " +
                    "Use bullet points for lists. Focus on decisions the owner can act on immediately. " +
                    "Never say you don't have data — it is all provided below.\n\n" + data;

                history.clear();
                history.add(Map.of("role", "system", "content", systemPrompt));
                history.add(Map.of("role", "user",
                    "content", "Give me an automatic business health summary including: " +
                               "overall performance, top and worst products, sales trend, expense situation, " +
                               "any urgent alerts, and your top 3 recommendations."));

                return grok.chat(history);
            }
        };

        task.setOnSucceeded(e -> {
            String reply = task.getValue();
            history.add(Map.of("role", "assistant", "content", reply));
            Platform.runLater(() -> {
                messagesBox.getChildren().remove(typing);
                addBotMessage(reply);
                setInputEnabled(true);
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                messagesBox.getChildren().remove(typing);
                addBotMessage("⚠️  Could not connect to Grok API: " +
                    task.getException().getMessage() +
                    "\n\nCheck that your API key is correct in the .env file.");
                setInputEnabled(true);
            });
        });

        new Thread(task, "grok-auto-greet").start();
    }

    private void sendMessage(String userText) {
        if (userText.isBlank()) return;
        inputField.clear();
        addUserMessage(userText);
        setInputEnabled(false);
        HBox typing = addTypingIndicator();

        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                if (systemPrompt == null) {
                    // In case auto-greet hasn't finished, rebuild system prompt
                    String data = BusinessDataContext.build();
                    systemPrompt = "You are BizMind Analytics Assistant. Use PKR for currency. " +
                        "Be concise and actionable.\n\n" + data;
                    history.add(Map.of("role", "system", "content", systemPrompt));
                }
                history.add(Map.of("role", "user", "content", userText));
                // Keep last 20 messages + system prompt to avoid token bloat
                List<Map<String, String>> trimmed = new ArrayList<>();
                trimmed.add(history.get(0)); // system prompt always first
                int start = Math.max(1, history.size() - 20);
                trimmed.addAll(history.subList(start, history.size()));
                return grok.chat(trimmed);
            }
        };

        task.setOnSucceeded(e -> {
            String reply = task.getValue();
            history.add(Map.of("role", "assistant", "content", reply));
            Platform.runLater(() -> {
                messagesBox.getChildren().remove(typing);
                addBotMessage(reply);
                setInputEnabled(true);
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                messagesBox.getChildren().remove(typing);
                addBotMessage("⚠️  Error: " + task.getException().getMessage());
                history.remove(history.size() - 1); // remove failed user message
                setInputEnabled(true);
            });
        });

        new Thread(task, "grok-chat").start();
    }

    private void setInputEnabled(boolean enabled) {
        inputField.setDisable(!enabled);
        sendBtn.setDisable(!enabled);
    }
}
