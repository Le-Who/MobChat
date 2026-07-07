// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.owlmaddie.commands.ConfigurationHandler;
import com.owlmaddie.json.ChatGPTResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.concurrent.CompletableFuture;

/**
 * The {@code ChatGPTRequest} class is used to send HTTP requests to our LLM to generate
 * messages.
 */
public class ChatGPTRequest {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static final Gson GSON = new Gson();
    public static String lastErrorMessage;
    public static int lastErrorCode = 0;

    public enum StructuredOutputMode {
        NONE,
        CHAT,
        CHARACTER
    }

    static class ChatGPTRequestMessage {
        String role;
        String content;

        public ChatGPTRequestMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    // Keeps the legacy Boolean overload intact while letting new callers choose a specific schema.
    private static class OutputModeMessageHistory extends ArrayList<ChatMessage> {
        final StructuredOutputMode outputMode;

        OutputModeMessageHistory(List<ChatMessage> messages, StructuredOutputMode outputMode) {
            super(messages == null ? Collections.emptyList() : messages);
            this.outputMode = outputMode == null ? StructuredOutputMode.NONE : outputMode;
        }
    }

    static class ChatGPTRequestPayload {
        String model;
        List<ChatGPTRequestMessage> messages;
        ResponseFormat response_format;
        String reasoning_effort;
        float temperature;
        int max_tokens;
        boolean stream;

        public ChatGPTRequestPayload(String apiUrl, String model, List<ChatGPTRequestMessage> messages, Boolean jsonMode, float temperature, int maxTokens, String thinkingLevel) {
            this(apiUrl, model, messages, Boolean.TRUE.equals(jsonMode) ? StructuredOutputMode.CHAT : StructuredOutputMode.NONE, temperature, maxTokens, thinkingLevel);
        }

        public ChatGPTRequestPayload(String apiUrl, String model, List<ChatGPTRequestMessage> messages, StructuredOutputMode outputMode, float temperature, int maxTokens, String thinkingLevel) {
            this.model = model;
            this.messages = messages;
            this.temperature = temperature;
            this.max_tokens = maxTokens;
            this.stream = false;
            if (shouldSendReasoningEffort(apiUrl, model, thinkingLevel)) {
                this.reasoning_effort = thinkingLevel;
            }
            StructuredOutputMode normalizedMode = outputMode == null ? StructuredOutputMode.NONE : outputMode;
            switch (normalizedMode) {
                case CHAT -> this.response_format = ResponseFormat.creatureChatSchema();
                case CHARACTER -> this.response_format = ResponseFormat.creatureChatCharacterSchema();
                case NONE -> this.response_format = ResponseFormat.text();
            }
        }
    }

    static class ResponseFormat {
        String type;
        JsonSchema json_schema;

        private ResponseFormat(String type) {
            this.type = type;
        }

        static ResponseFormat text() {
            return new ResponseFormat("text");
        }

        static ResponseFormat creatureChatSchema() {
            ResponseFormat format = new ResponseFormat("json_schema");
            format.json_schema = JsonSchema.creatureChatResponse();
            return format;
        }

        static ResponseFormat creatureChatCharacterSchema() {
            ResponseFormat format = new ResponseFormat("json_schema");
            format.json_schema = JsonSchema.creatureChatCharacter();
            return format;
        }
    }

    static class JsonSchema {
        String name;
        boolean strict;
        Map<String, Object> schema;

        private JsonSchema(String name, boolean strict, Map<String, Object> schema) {
            this.name = name;
            this.strict = strict;
            this.schema = schema;
        }

        static JsonSchema creatureChatResponse() {
            Map<String, Object> actionSchema = new LinkedHashMap<>();
            actionSchema.put("type", "object");
            actionSchema.put("additionalProperties", false);
            actionSchema.put("properties", Map.of(
                    "type", Map.of(
                            "type", "string",
                            "enum", List.of(
                                    "FOLLOW",
                                    "UNFOLLOW",
                                    "LEAD",
                                    "UNLEAD",
                                    "FLEE",
                                    "UNFLEE",
                                    "ATTACK",
                                    "PROTECT",
                                    "UNPROTECT",
                                    "FRIENDSHIP",
                                    "WAIT",
                                    "RETURN_HOME",
                                    "GUARD_HOME"
                            )
                    ),
                    "value", Map.of(
                            "type", List.of("integer", "null"),
                            "minimum", -3,
                            "maximum", 3
                    )
            ));
            actionSchema.put("required", List.of("type", "value"));

            Map<String, Object> rootSchema = new LinkedHashMap<>();
            rootSchema.put("type", "object");
            rootSchema.put("additionalProperties", false);
            rootSchema.put("properties", Map.of(
                    "message", Map.of("type", "string"),
                    "mood", Map.of("type", "string"),
                    "memory_updates", Map.of(
                            "type", "array",
                            "items", Map.of("type", "string")
                    ),
                    "actions", Map.of(
                            "type", "array",
                            "items", actionSchema
                    )
            ));
            rootSchema.put("required", List.of("message", "actions", "mood", "memory_updates"));

            return new JsonSchema("creaturechat_response", true, rootSchema);
        }

        static JsonSchema creatureChatCharacter() {
            Map<String, Object> stringSchema = Map.of("type", "string");
            Map<String, Object> stringArraySchema = Map.of(
                    "type", "array",
                    "items", stringSchema
            );

            Map<String, Object> rootSchema = new LinkedHashMap<>();
            rootSchema.put("type", "object");
            rootSchema.put("additionalProperties", false);
            rootSchema.put("properties", Map.of(
                    "name", stringSchema,
                    "personality", stringSchema,
                    "speaking_style", stringSchema,
                    "class_name", stringSchema,
                    "skills", stringArraySchema,
                    "likes", stringArraySchema,
                    "dislikes", stringArraySchema,
                    "alignment", stringSchema,
                    "background", stringSchema,
                    "short_greeting", stringSchema
            ));
            rootSchema.put("required", List.of(
                    "name",
                    "personality",
                    "speaking_style",
                    "class_name",
                    "skills",
                    "likes",
                    "dislikes",
                    "alignment",
                    "background",
                    "short_greeting"
            ));

            return new JsonSchema("creaturechat_character", true, rootSchema);
        }
    }

    public static String removeQuotes(String str) {
        if (str != null && str.length() > 1 && str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    // Class to represent the error response structure
    public static class ErrorResponse {
        Error error;

        static class Error {
            String message;
            String type;
            String code;
        }
    }

    public static String parseAndLogErrorResponse(String errorResponse) {
        try {
            ErrorResponse response = GSON.fromJson(errorResponse, ErrorResponse.class);

            if (response != null && response.error != null) {
                LOGGER.error("Error Message: " + response.error.message);
                LOGGER.error("Error Type: " + response.error.type);
                LOGGER.error("Error Code: " + response.error.code);
                return response.error.message != null ? response.error.message : "Unknown error";
            } else {
                // Some gateways return {"message":"Internal server error"} or similar
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = GSON.fromJson(errorResponse, Map.class);
                    Object msg = (m != null) ? m.get("message") : null;
                    if (msg instanceof String && !((String) msg).isEmpty()) {
                        LOGGER.error("Gateway error message: " + msg);
                        return (String) msg;
                    }
                } catch (Exception ignore) {
                    // fall through to generic handling below
                }
                LOGGER.error("Unknown error response: " + errorResponse);
                return "Unknown error";
            }
        } catch (JsonSyntaxException e) {
            LOGGER.warn("Failed to parse error response as JSON, falling back to plain text");
            LOGGER.error("Error response: " + errorResponse);
        } catch (Exception e) {
            LOGGER.error("Failed to parse error response", e);
        }
        return removeQuotes(errorResponse);
    }

    // Function to replace placeholders in the template
    public static String replacePlaceholders(String template, Map<String, String> replacements) {
        String result = template;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    public static CompletableFuture<String> fetchMessageFromChatGPT(
            ConfigurationHandler.Config config,
            String systemPrompt,
            Map<String, String> contextData,
            List<ChatMessage> messageHistory,
            StructuredOutputMode outputMode) {
        StructuredOutputMode normalizedMode = outputMode == null ? StructuredOutputMode.NONE : outputMode;
        return fetchMessageFromChatGPT(
                config,
                systemPrompt,
                contextData,
                new OutputModeMessageHistory(messageHistory, normalizedMode),
                normalizedMode != StructuredOutputMode.NONE);
    }

    private static StructuredOutputMode outputModeFrom(List<ChatMessage> messageHistory, Boolean jsonMode) {
        if (messageHistory instanceof OutputModeMessageHistory outputModeHistory) {
            return outputModeHistory.outputMode;
        }
        return Boolean.TRUE.equals(jsonMode) ? StructuredOutputMode.CHAT : StructuredOutputMode.NONE;
    }

    // Function to roughly estimate # of OpenAI tokens in String
    private static int estimateTokenSize(String text) {
        return (int) Math.round(text.length() / 3.5);
    }

    private static String sanitizeApiKey(String message, String apiKey) {
        if (message == null || apiKey == null || apiKey.isEmpty()) {
            return message;
        }
        return message.replace(apiKey, "**********");
    }    public static CompletableFuture<String> fetchMessageFromChatGPT(ConfigurationHandler.Config config, String systemPrompt, Map<String, String> contextData, List<ChatMessage> messageHistory, Boolean jsonMode) {
        // Init API & LLM details
        String apiUrl = config.getUrl();
        Integer timeout = config.getTimeout() * 1000;
        int maxContextTokens = config.getMaxContextTokens();
        int maxOutputTokens = config.getMaxOutputTokens();
        double percentOfContext = config.getPercentOfContext();
        StructuredOutputMode normalizedOutputMode = outputModeFrom(messageHistory, jsonMode);

        return CompletableFuture.supplyAsync(() -> {
            lastErrorCode = 0;
            int keyCount = Math.max(1, config.getApiKeyCount());
            int modelCount = Math.max(1, config.getModelCount());
            int maxAttempts = keyCount * modelCount;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                String activeKey = config.getActiveApiKey();
                String modelName = config.getActiveModel();
                HttpURLConnection connection = null;
                try {
                    // Replace placeholders
                    String systemMessage = replacePlaceholders(systemPrompt, contextData);

                    URL url = new URL(apiUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("Authorization", "Bearer " + activeKey);
                    connection.setRequestProperty("Connection", "keep-alive");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("Accept-Encoding", "gzip");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(timeout);
                    connection.setReadTimeout(timeout);

                    // Create messages list (for chat history)
                    List<ChatGPTRequestMessage> messages = new ArrayList<>();

                    // Don't exceed a specific % of total context window (to limit message history in request)
                    int remainingContextTokens = (int) ((maxContextTokens - maxOutputTokens) * percentOfContext);
                    int usedTokens = estimateTokenSize("system: " + systemMessage);

                    // Iterate backwards through the message history
                    for (int i = messageHistory.size() - 1; i >= 0; i--) {
                        ChatMessage chatMessage = messageHistory.get(i);
                        String senderName = chatMessage.sender.toString().toLowerCase(Locale.ENGLISH);
                        String messageText = replacePlaceholders(chatMessage.message, contextData);
                        int messageTokens = estimateTokenSize(senderName + ": " + messageText);

                        if (usedTokens + messageTokens > remainingContextTokens) {
                            break;  // If adding this message would exceed the token limit, stop adding more messages
                        }

                        // Add the message to the temporary list
                        messages.add(new ChatGPTRequestMessage(senderName, messageText));
                        usedTokens += messageTokens;
                    }

                    // Add system message
                    messages.add(new ChatGPTRequestMessage("system", systemMessage));

                    // Reverse the list to restore chronological order
                    // This is needed since we build the list in reverse order for token restricting above
                    Collections.reverse(messages);

                    // Convert JSON to String
                    ChatGPTRequestPayload payload = new ChatGPTRequestPayload(
                            apiUrl, modelName, messages, normalizedOutputMode, 1.0f, maxOutputTokens, config.getThinkingLevel());

                    Gson gsonInput = new Gson();
                    String jsonInputString = gsonInput.toJson(payload);

                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    connection.setFixedLengthStreamingMode(input.length);
                    try (OutputStream os = connection.getOutputStream()) {
                        os.write(input);
                    }

                    // Check for error message in response
                    int statusCode = connection.getResponseCode();
                    if (statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                        if (shouldTryNextCandidate(statusCode, attempt, maxAttempts)) {
                            LOGGER.warn("API request returned HTTP " + statusCode + ". Trying next API key/model candidate (attempt " + attempt + " of " + maxAttempts + ").");
                            rotateCandidate(config, attempt, keyCount, modelCount);
                            if (connection != null) {
                                try {
                                    connection.disconnect();
                                } catch (Exception ignored) {}
                            }
                            continue;
                        }

                        lastErrorCode = statusCode;
                        final String reason = connection.getResponseMessage() != null ? connection.getResponseMessage() : "";

                        // Try to capture helpful IDs for tracing through AWS and OpenAI
                        final String awsRequestId    = connection.getHeaderField("x-amzn-RequestId");
                        final String awsErrorType    = connection.getHeaderField("x-amzn-ErrorType");
                        final String openaiRequestId = connection.getHeaderField("x-request-id");

                        // Log AWS headers only for debugging so they don't bloat user-facing messages
                        if (awsRequestId != null) LOGGER.debug("AWS Request ID: {}", awsRequestId);
                        if (awsErrorType != null) LOGGER.debug("AWS Error Type: {}", awsErrorType);
                        if (openaiRequestId != null) LOGGER.debug("OpenAI Request ID: {}", openaiRequestId);

                        InputStream errStream = connection.getErrorStream();
                        if (errStream == null) {
                            try {
                                errStream = connection.getInputStream();
                            } catch (Exception ex) {
                                LOGGER.error("Failed to obtain error stream", ex);
                                String msg = reason != null ? reason : ("HTTP error " + statusCode);
                                StringBuilder base = new StringBuilder();
                                base.append("HTTP ").append(statusCode);
                                if (msg != null && !msg.isEmpty()) base.append(" ").append(msg);

                                lastErrorMessage = sanitizeApiKey(base + ": " + ex.getMessage(), activeKey);
                                return null;
                            }
                        }
                        if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
                            errStream = new GZIPInputStream(errStream);
                        }
                        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(errStream, StandardCharsets.UTF_8))) {
                            String line;
                            StringBuilder errorResponse = new StringBuilder();
                            while ((line = errorReader.readLine()) != null) {
                                errorResponse.append(line.trim());
                            }

                            // Try known shapes first
                            String cleanError = parseAndLogErrorResponse(errorResponse.toString());

                            // Build a richer message (status + reason + IDs + short body preview)
                            StringBuilder sb = new StringBuilder();
                            sb.append("HTTP ").append(statusCode);
                            if (!reason.isEmpty()) sb.append(" ").append(reason);

                            if (cleanError != null && !cleanError.isEmpty() && !"Unknown error".equals(cleanError)) {
                                sb.append(": ").append(cleanError);
                            } else if (errorResponse.length() > 0) {
                                String bodyPreview = errorResponse.length() > 300
                                        ? errorResponse.substring(0, 300) + "..."
                                        : errorResponse.toString();
                                sb.append(": ").append(bodyPreview);
                            }

                            String finalMsg = sb.toString();
                            LOGGER.error(finalMsg);
                            lastErrorMessage = sanitizeApiKey(finalMsg, activeKey);
                        } catch (Exception e) {
                            LOGGER.error("Failed to read error response", e);
                            lastErrorMessage = sanitizeApiKey("Failed to read error response: " + e.getMessage(), activeKey);
                        }
                        return null;
                    } else {
                        lastErrorMessage = null;
                        lastErrorCode = 0;
                    }

                    InputStream inStream = connection.getInputStream();
                    if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
                        inStream = new GZIPInputStream(inStream);
                    }
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }

                        ChatGPTResponse chatGPTResponse = GSON.fromJson(response.toString(), ChatGPTResponse.class);
                        if (chatGPTResponse != null && chatGPTResponse.choices != null && !chatGPTResponse.choices.isEmpty()) {
                            return chatGPTResponse.choices.get(0).message.content;
                        }
                        lastErrorMessage = "Failed to parse response";
                        return null;
                    }
                } catch (SocketException | SocketTimeoutException ce) {
                    LOGGER.warn("Connection failed", ce);
                    lastErrorMessage = "No Internet or Blocked Request: " + ce.getMessage();
                    lastErrorCode = -1;
                    return null;
                } catch (Exception e) {
                    LOGGER.error("Failed to request message", e);
                    lastErrorMessage = sanitizeApiKey("Failed to request message: " + e.getMessage(), activeKey);
                    lastErrorCode = 0;
                    return null;
                }
            }
            return null;
        });
    }

    private static boolean shouldTryNextCandidate(int statusCode, int attempt, int maxAttempts) {
        if (attempt >= maxAttempts) {
            return false;
        }
        return statusCode == 400
                || statusCode == 401
                || statusCode == 403
                || statusCode == 404
                || statusCode == 408
                || statusCode == 409
                || statusCode == 429
                || statusCode >= 500;
    }

    private static void rotateCandidate(ConfigurationHandler.Config config, int attempt, int keyCount, int modelCount) {
        if (modelCount > 1) {
            config.rotateModel();
        }
        if (keyCount > 1 && (modelCount <= 1 || attempt % modelCount == 0)) {
            config.rotateApiKey();
        }
    }

    private static boolean shouldSendReasoningEffort(String apiUrl, String modelName, String thinkingLevel) {
        if (thinkingLevel == null || thinkingLevel.equals("auto")) {
            return false;
        }
        String normalizedThinking = thinkingLevel.trim().toLowerCase(Locale.ENGLISH);
        if (!List.of("low", "medium", "high").contains(normalizedThinking)) {
            return false;
        }
        String normalizedUrl = apiUrl == null ? "" : apiUrl.toLowerCase(Locale.ENGLISH);
        String normalizedModel = modelName == null ? "" : modelName.toLowerCase(Locale.ENGLISH);
        return normalizedUrl.contains("generativelanguage.googleapis.com")
                || normalizedModel.startsWith("gemini-");
    }
}
