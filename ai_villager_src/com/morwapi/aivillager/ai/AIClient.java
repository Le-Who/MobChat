/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  me.shedaniel.autoconfig.AutoConfig
 */
package com.morwapi.aivillager.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.morwapi.aivillager.AIVillagerMod;
import com.morwapi.aivillager.config.AIVillagerConfig;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import me.shedaniel.autoconfig.AutoConfig;

public class AIClient {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    public static CompletableFuture<String> sendRequest(String prompt) {
        return AIClient.sendRequest(prompt, 0);
    }

    private static CompletableFuture<String> sendRequest(String prompt, int retryCount) {
        AIVillagerConfig config = (AIVillagerConfig)AutoConfig.getConfigHolder(AIVillagerConfig.class).getConfig();
        if (!config.enableAI || config.apiKey.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalStateException("AI is disabled or API key is missing"));
        }
        String apiUrl = config.apiUrl;
        String requestBody = "";
        String modelToUse = config.modelName;
        if (retryCount > 0 && config.autoSwitchModel && !config.fallbackModels.isEmpty()) {
            int index = retryCount - 1;
            modelToUse = index < config.fallbackModels.size() ? config.fallbackModels.get(index) : config.fallbackModels.get(config.fallbackModels.size() - 1);
        }
        JsonObject json = new JsonObject();
        json.addProperty("model", modelToUse);
        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        messages.add((JsonElement)message);
        json.add("messages", (JsonElement)messages);
        requestBody = gson.toJson((JsonElement)json);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(apiUrl)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(requestBody));
        requestBuilder.header("Authorization", "Bearer " + config.apiKey);
        return client.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString()).thenCompose(response -> {
            if (response.statusCode() == 429 && config.autoSwitchModel && retryCount < config.fallbackModels.size()) {
                String nextModel = config.fallbackModels.get(retryCount);
                AIVillagerMod.LOGGER.warn("Rate limit detected (429). Switching to fallback model: " + nextModel);
                return AIClient.sendRequest(prompt, retryCount + 1);
            }
            if (response.statusCode() != 200) {
                throw new RuntimeException("API Request failed: " + response.statusCode() + " " + (String)response.body());
            }
            return CompletableFuture.completedFuture(AIClient.parseResponse((String)response.body()));
        });
    }

    private static String parseResponse(String responseBody) {
        JsonObject json = JsonParser.parseString((String)responseBody).getAsJsonObject();
        return json.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
    }
}

