// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.chat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lewho.commands.ConfigurationHandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Native Google Gemini API client executing generateContent requests.
 */
public final class GeminiNativeRequest {
    private GeminiNativeRequest() {
    }

    public static CompletableFuture<String> fetchMessageFromGemini(
            ConfigurationHandler.Config config,
            String systemPrompt,
            Map<String, String> contextData,
            List<ChatMessage> messageHistory,
            ChatGPTRequest.StructuredOutputMode outputMode) {
        return ChatGPTRequest.fetchResult(config, systemPrompt, contextData, messageHistory, outputMode, true)
                .thenApply(ChatGPTRequest.RequestResult::content);
    }

    static String endpoint(String baseUrl, String modelName) throws java.net.URISyntaxException {
        URI base = URI.create(baseUrl);
        String path = base.getPath() == null ? "" : base.getPath().replaceAll("/+$", "");
        int modelPath = path.indexOf("/models/");
        // Explicit generateContent URLs must still follow the selected fallback model.
        if (modelPath >= 0) path = path.substring(0, modelPath);
        path += "/models/" + modelName + ":generateContent";
        return new URI(base.getScheme(), base.getAuthority(), path, base.getQuery(), base.getFragment()).toString();
    }

    static GeminiPayload buildPayload(
            String systemMessage,
            List<ChatGPTRequest.ChatGPTRequestMessage> messageHistory,
            ChatGPTRequest.StructuredOutputMode outputMode,
            String modelName,
            int maxOutputTokens,
            String thinkingLevel) {
        GeminiPayload payload = new GeminiPayload();
        if (!systemMessage.isBlank()) {
            payload.systemInstruction = new GeminiPayload.ContentParts(List.of(new GeminiPayload.Part(systemMessage)));
        }

        List<GeminiPayload.Content> contents = new ArrayList<>();
        for (ChatGPTRequest.ChatGPTRequestMessage message : messageHistory) {
            String role = "user".equals(message.role) ? "user" : "model";
            contents.add(new GeminiPayload.Content(role, List.of(new GeminiPayload.Part(message.content))));
        }
        if (contents.isEmpty()) {
            contents.add(new GeminiPayload.Content("user", List.of(new GeminiPayload.Part("Proceed."))));
        }
        payload.contents = contents;

        GeminiPayload.GenerationConfig generation = new GeminiPayload.GenerationConfig();
        generation.maxOutputTokens = maxOutputTokens;
        String normalizedModel = modelName.toLowerCase(Locale.ENGLISH);
        // Keep the existing Flash-Lite temperature exception.
        if (!normalizedModel.contains("3.5-flash-lite")) {
            generation.temperature = 1.0f;
        }
        if (normalizedModel.startsWith("gemini-3") && !"auto".equals(thinkingLevel)) {
            generation.thinkingConfig = new GeminiPayload.ThinkingConfig(thinkingLevel);
        }
        if (outputMode == ChatGPTRequest.StructuredOutputMode.CHARACTER) {
            generation.responseMimeType = "application/json";
            generation.responseSchema = stripAdditionalProperties(ChatGPTRequest.JsonSchema.creatureChatCharacter().schema);
        } else if (outputMode == ChatGPTRequest.StructuredOutputMode.CHAT) {
            generation.responseMimeType = "application/json";
            generation.responseSchema = stripAdditionalProperties(ChatGPTRequest.JsonSchema.creatureChatResponse().schema);
        }
        payload.generationConfig = generation;
        return payload;
    }

    /**
     * Returns a deep copy of the given JSON schema map with all {@code additionalProperties}
     * keys removed at every nesting level.
     *
     * <p>The Gemini native {@code generationConfig.responseSchema} field accepts a restricted
     * OpenAPI 3.0 subset that does <em>not</em> support {@code additionalProperties}. Our schemas
     * are built for OpenAI structured output, which requires the field. Stripping it here keeps
     * the two serialisation paths independent.</p>
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> stripAdditionalProperties(Map<String, Object> schema) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : schema.entrySet()) {
            if ("additionalProperties".equals(entry.getKey())) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof Map) {
                value = stripAdditionalProperties((Map<String, Object>) value);
            } else if (value instanceof List) {
                value = stripAdditionalPropertiesFromList((List<?>) value);
            }
            result.put(entry.getKey(), value);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> stripAdditionalPropertiesFromList(List<?> list) {
        List<Object> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (item instanceof Map) {
                result.add(stripAdditionalProperties((Map<String, Object>) item));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    static ChatGPTRequest.ResponseContent parseSuccessResponse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.has("candidates") || root.getAsJsonArray("candidates").isEmpty()) return null;
        JsonObject candidate = root.getAsJsonArray("candidates").get(0).getAsJsonObject();
        String finishReason = candidate.has("finishReason") ? candidate.get("finishReason").getAsString() : null;
        Integer completionTokens = null;
        if (root.has("usageMetadata")) {
            JsonObject usage = root.getAsJsonObject("usageMetadata");
            if (usage.has("candidatesTokenCount")) completionTokens = usage.get("candidatesTokenCount").getAsInt();
        }
        if (candidate.has("content")) {
            JsonObject content = candidate.getAsJsonObject("content");
            if (content.has("parts") && !content.getAsJsonArray("parts").isEmpty()) {
                JsonObject part = content.getAsJsonArray("parts").get(0).getAsJsonObject();
                if (part.has("text")) {
                    return new ChatGPTRequest.ResponseContent(part.get("text").getAsString(), finishReason, completionTokens);
                }
            }
        }
        // A valid candidate can stop at MAX_TOKENS before producing text. Keep its diagnostics.
        return new ChatGPTRequest.ResponseContent(null, finishReason, completionTokens);
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
            ThinkingConfig thinkingConfig;
        }

        static class ThinkingConfig {
            String thinkingLevel;

            ThinkingConfig(String thinkingLevel) {
                this.thinkingLevel = thinkingLevel;
            }
        }
    }
}
