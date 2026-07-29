// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.chat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lewho.commands.ConfigurationHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;

/**
 * Native Google Gemini API client executing generateContent requests.
 */
public final class GeminiNativeRequest {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();

    private GeminiNativeRequest() {
    }

    public static CompletableFuture<String> fetchMessageFromGemini(
            ConfigurationHandler.Config config,
            String systemPrompt,
            Map<String, String> contextData,
            List<ChatMessage> messageHistory,
            ChatGPTRequest.StructuredOutputMode outputMode) {

        String baseUrl = config.getUrl().replaceAll("/+$", "");
        int timeout = config.getTimeout() * 1000;
        String thinkingLevel = config.getThinkingLevel();
        int maxOutputTokens = ChatGPTRequest.effectiveMaxOutputTokens(config.getMaxOutputTokens(), outputMode, thinkingLevel);
        int candidateCount = Math.max(1, config.getApiKeyCount());


        return CompletableFuture.supplyAsync(() -> {
            ChatGPTRequest.lastErrorCode = 0;
            ChatGPTRequest.lastFinishReason = null;
            ChatGPTRequest.lastErrorMessage = null;

            int attemptsPerCandidate = 2; // 1 attempt + 1 retry
            int maxAttempts = candidateCount * attemptsPerCandidate;

            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                String currentKey = config.getActiveApiKey();
                String currentModel = config.getActiveModel();

                // Build request URL: {baseUrl}/models/{model}:generateContent
                String endpointUrl = baseUrl;
                if (!endpointUrl.contains("/models/")) {
                    endpointUrl += "/models/" + currentModel + ":generateContent";
                } else if (!endpointUrl.endsWith(":generateContent")) {
                    endpointUrl += ":generateContent";
                }

                HttpURLConnection connection = null;
                try {
                    URL url = new URL(endpointUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("x-goog-api-key", currentKey);
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("Accept-Encoding", "gzip");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(timeout);
                    connection.setReadTimeout(timeout);

                    GeminiPayload payload = buildPayload(systemPrompt, contextData, messageHistory, outputMode, currentModel, maxOutputTokens);
                    String jsonInput = GSON.toJson(payload);
                    byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);

                    connection.setFixedLengthStreamingMode(input.length);
                    try (OutputStream os = connection.getOutputStream()) {
                        os.write(input);
                        os.flush();
                    }

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        String encoding = connection.getHeaderField("Content-Encoding");
                        InputStream responseStream = connection.getInputStream();
                        if ("gzip".equalsIgnoreCase(encoding)) {
                            responseStream = new GZIPInputStream(responseStream);
                        }

                        String responseBody = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);
                        String text = parseSuccessResponse(responseBody);
                        if (text != null) {
                            return text;
                        }
                    } else {
                        ChatGPTRequest.lastErrorCode = responseCode;
                        InputStream errorStream = connection.getErrorStream();
                        if (errorStream != null) {
                            String encoding = connection.getHeaderField("Content-Encoding");
                            if ("gzip".equalsIgnoreCase(encoding)) {
                                errorStream = new GZIPInputStream(errorStream);
                            }
                            String errorBody = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                            ChatGPTRequest.lastErrorMessage = parseErrorResponse(errorBody);
                        } else {
                            ChatGPTRequest.lastErrorMessage = "HTTP " + responseCode;
                        }

                        LOGGER.warn("Native Gemini API returned code: " + responseCode + " Error: " + ChatGPTRequest.lastErrorMessage);
                    }

                } catch (Exception e) {
                    LOGGER.warn("Native Gemini API connection failed (attempt " + (attempt + 1) + "/" + maxAttempts + "): " + e.getMessage());
                    ChatGPTRequest.lastErrorMessage = e.getMessage();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }

                // If candidate exhausted, rotate
                if ((attempt + 1) % attemptsPerCandidate == 0 && candidateCount > 1) {
                    config.rotateApiKey();
                }
            }

            return null;
        });
    }

    private static GeminiPayload buildPayload(
            String systemPrompt,
            Map<String, String> contextData,
            List<ChatMessage> messageHistory,
            ChatGPTRequest.StructuredOutputMode outputMode,
            String modelName,
            int maxOutputTokens) {

        GeminiPayload payload = new GeminiPayload();

        // System Instruction — Gemini native API: no 'role' field, only 'parts'
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            String resolvedPrompt = ChatGPTRequest.replacePlaceholders(systemPrompt, contextData);
            payload.systemInstruction = new GeminiPayload.ContentParts(List.of(new GeminiPayload.Part(resolvedPrompt)));
        }

        // Contents
        List<GeminiPayload.Content> contents = new ArrayList<>();
        if (messageHistory != null) {
            for (ChatMessage msg : messageHistory) {
                String role = msg.sender == ChatDataManager.ChatSender.USER ? "user" : "model";
                String text = ChatGPTRequest.replacePlaceholders(msg.message, contextData);
                contents.add(new GeminiPayload.Content(role, List.of(new GeminiPayload.Part(text))));
            }
        }

        if (contents.isEmpty()) {
            contents.add(new GeminiPayload.Content("user", List.of(new GeminiPayload.Part("Proceed."))));
        }

        payload.contents = contents;

        // Generation Config
        GeminiPayload.GenerationConfig config = new GeminiPayload.GenerationConfig();
        config.maxOutputTokens = maxOutputTokens;

        // gemini-3.5-flash-lite deprecates temperature customization
        if (!modelName.toLowerCase(Locale.ENGLISH).contains("3.5-flash-lite")) {
            config.temperature = 1.0f;
        }

        if (outputMode == ChatGPTRequest.StructuredOutputMode.CHARACTER) {
            config.responseMimeType = "application/json";
            config.responseSchema = ChatGPTRequest.JsonSchema.creatureChatCharacter().schema;
        } else if (outputMode == ChatGPTRequest.StructuredOutputMode.CHAT) {
            config.responseMimeType = "application/json";
            config.responseSchema = ChatGPTRequest.JsonSchema.creatureChatResponse().schema;
        }

        payload.generationConfig = config;
        return payload;
    }


    private static String parseSuccessResponse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("candidates") && root.getAsJsonArray("candidates").size() > 0) {
                JsonObject candidate = root.getAsJsonArray("candidates").get(0).getAsJsonObject();
                if (candidate.has("finishReason")) {
                    ChatGPTRequest.lastFinishReason = candidate.get("finishReason").getAsString();
                }
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts") && content.getAsJsonArray("parts").size() > 0) {
                        JsonObject part = content.getAsJsonArray("parts").get(0).getAsJsonObject();
                        if (part.has("text")) {
                            return part.get("text").getAsString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse native Gemini response", e);
        }
        return null;
    }

    private static String parseErrorResponse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            if (root.has("error")) {
                JsonObject error = root.getAsJsonObject("error");
                if (error.has("message")) {
                    return error.get("message").getAsString();
                }
            }
        } catch (Exception e) {
            // Ignore JSON parse errors for non-JSON error pages
        }
        return json;
    }

    static class GeminiPayload {
        ContentParts systemInstruction;
        List<Content> contents;
        GenerationConfig generationConfig;

        static class ContentParts {
            List<Part> parts;

            ContentParts(List<Part> parts) {
                this.parts = parts;
            }
        }
        static class Content {
            String role;
            List<Part> parts;

            Content(String role, List<Part> parts) {
                this.role = role;
                this.parts = parts;
            }
        }

        static class Part {
            String text;

            Part(String text) {
                this.text = text;
            }
        }

        static class GenerationConfig {
            Integer maxOutputTokens;
            Float temperature;
            String responseMimeType;
            Map<String, Object> responseSchema;
        }
    }
}
