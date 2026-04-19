package com.bizmind.analytics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class EnvLoader {

    private static final Map<String, String> values = new HashMap<>();
    private static boolean loaded = false;

    private static void load() {
        if (loaded) return;
        loaded = true;
        // Try working directory (project root when running via mvn javafx:run)
        File envFile = new File(".env");
        if (!envFile.exists()) {
            // Fallback: one level up from wherever we are
            envFile = new File("../.env");
        }
        if (!envFile.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String key = line.substring(0, eq).trim();
                String val = line.substring(eq + 1).trim();
                values.put(key, val);
            }
        } catch (Exception ignored) {}
    }

    public static String get(String key) {
        load();
        return values.getOrDefault(key, "");
    }

    public static String get(String key, String defaultVal) {
        load();
        return values.getOrDefault(key, defaultVal);
    }
}
