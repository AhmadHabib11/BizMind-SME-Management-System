package com.bizmind.analytics;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class GrokClient {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    private final String apiKey;
    private final String model;

    public GrokClient() {
        this.apiKey = EnvLoader.get("GROQ_API_KEY");
        this.model  = EnvLoader.get("GROQ_MODEL", "llama-3.3-70b-versatile");
    }

    /**
     * Sends a conversation to the Grok API and returns the assistant's reply.
     *
     * @param messages List of {role, content} maps representing conversation history
     *                 (including the system prompt as the first entry)
     * @return The assistant's reply text
     */
    public String chat(List<Map<String, String>> messages) throws Exception {
        if (apiKey.isBlank() || apiKey.equals("paste_your_groq_api_key_here")) {
            throw new IllegalStateException(
                "Groq API key not set. Open the .env file in the project root and paste your Groq API key.\n" +
                "Get a free key at: https://console.groq.com");
        }

        JSONArray msgArray = new JSONArray();
        for (Map<String, String> m : messages) {
            JSONObject msg = new JSONObject();
            msg.put("role",    m.get("role"));
            msg.put("content", m.get("content"));
            msgArray.put(msg);
        }

        JSONObject body = new JSONObject();
        body.put("model",    model);
        body.put("messages", msgArray);
        body.put("stream",   false);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type",  "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Grok API error " + response.statusCode() + ": " + response.body());
        }

        JSONObject json = new JSONObject(response.body());
        return json.getJSONArray("choices")
                   .getJSONObject(0)
                   .getJSONObject("message")
                   .getString("content");
    }
}
