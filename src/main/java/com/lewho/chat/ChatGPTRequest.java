// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.chat;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.lewho.commands.ConfigurationHandler;
import com.lewho.json.ChatGPTResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
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
    public static String lastFinishReason;
    public static Integer lastCompletionTokens;
    public static String lastResponsePreview;
    public static String lastStructuredResponseWarning;
    public static int lastRequestedMaxOutputTokens;
    private static ApiUsageLimiter usageLimiter = new ApiUsageLimiter();

    public enum StructuredOutputMode {
        NONE,
        CHAT,
        CHARACTER
    }

    /** Immutable diagnostics belonging to one request, including any successful retry. */
    public record RequestResult(
            String content,
            int errorCode,
            String errorMessage,
            String finishReason,
            Integer completionTokens,
            String responsePreview,
            String structuredResponseWarning,
            int requestedMaxOutputTokens) {
    }

    record ResponseContent(String content, String finishReason, Integer completionTokens) {
    }

    static class ChatGPTRequestMessage {
        String role;
        String content;

        public ChatGPTRequestMessage(String role, String content) {
            this.role = role;
            this.content = content;
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
                            "type", "integer",
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
        return parseAndLogErrorResponse(errorResponse, List.of());
    }

    private static String parseAndLogErrorResponse(String errorResponse, List<String> secrets) {
        errorResponse = sanitize(errorResponse, secrets);
        try {
            JsonElement root = JsonParser.parseString(errorResponse);
            if (root.isJsonArray() && !root.getAsJsonArray().isEmpty()) {
                root = root.getAsJsonArray().get(0);
            }
            ErrorResponse response = GSON.fromJson(root, ErrorResponse.class);

            if (response != null && response.error != null) {
                String message = sanitize(response.error.message, secrets);
                LOGGER.error("Error Message: " + message);
                LOGGER.error("Error Type: " + sanitize(response.error.type, secrets));
                LOGGER.error("Error Code: " + sanitize(response.error.code, secrets));
                return message != null ? message : "Unknown error";
            } else {
                // Some gateways return {"message":"Internal server error"} or similar
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = GSON.fromJson(root, Map.class);
                    Object msg = (m != null) ? m.get("message") : null;
                    if (msg instanceof String && !((String) msg).isEmpty()) {
                        String message = sanitize((String) msg, secrets);
                        LOGGER.error("Gateway error message: " + message);
                        return message;
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
            LOGGER.error("Failed to parse error response: {}", sanitize(e.getMessage(), secrets));
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
        return fetchResultFromChatGPT(config, systemPrompt, contextData, messageHistory, outputMode)
                .thenApply(RequestResult::content);
    }

    public static CompletableFuture<String> fetchMessageFromChatGPT(
            ConfigurationHandler.Config config,
            String systemPrompt,
            Map<String, String> contextData,
            List<ChatMessage> messageHistory,
            Boolean jsonMode) {
        return fetchMessageFromChatGPT(config, systemPrompt, contextData, messageHistory,
                Boolean.TRUE.equals(jsonMode) ? StructuredOutputMode.CHAT : StructuredOutputMode.NONE);
    }

    public static CompletableFuture<RequestResult> fetchResultFromChatGPT(
            ConfigurationHandler.Config config,
            String systemPrompt,
            Map<String, String> contextData,
            List<ChatMessage> messageHistory,
            StructuredOutputMode outputMode) {
        return fetchResult(config, systemPrompt, contextData, messageHistory, outputMode, false);
    }

    // The explicit native entrypoint also uses this runner, including localhost fixtures.
    static CompletableFuture<RequestResult> fetchResult(
            ConfigurationHandler.Config config,
            String systemPrompt,
            Map<String, String> contextData,
            List<ChatMessage> messageHistory,
            StructuredOutputMode outputMode,
            boolean forceNative) {
        RequestSnapshot snapshot = snapshotRequest(config, systemPrompt, contextData, messageHistory, outputMode);
        boolean nativeGemini = forceNative || isNativeGeminiUrl(snapshot.config().getUrl());
        return CompletableFuture.supplyAsync(() -> runRequest(snapshot, nativeGemini))
                .thenApply(ChatGPTRequest::publishLegacyDiagnostics);
    }

    private record RequestSnapshot(
            ConfigurationHandler.Config config,
            ConfigurationHandler.Config sourceConfig,
            List<ChatGPTRequestMessage> history,
            String systemMessage,
            StructuredOutputMode outputMode,
            int maxOutputTokens,
            List<String> secrets) {
    }

    private static RequestSnapshot snapshotRequest(
            ConfigurationHandler.Config source,
            String systemPrompt,
            Map<String, String> contextData,
            List<ChatMessage> messageHistory,
            StructuredOutputMode outputMode) {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        // Copy every request/quota setting before starting asynchronous work. The copy owns its cursors.
        synchronized (source) {
            config.setApiKey(source.getApiKey());
            config.setUrl(source.getUrl());
            config.setModel(source.getModel());
            config.setTimeout(source.getTimeout());
            config.setMaxContextTokens(source.getMaxContextTokens());
            config.setMaxOutputTokens(source.getMaxOutputTokens());
            config.setPercentOfContext(source.getPercentOfContext());
            config.setThinkingLevel(source.getThinkingLevel());
            config.setGeminiUsageLimitsEnabled(source.getGeminiUsageLimitsEnabled());
            config.setGeminiRequestsPerMinute(source.getGeminiRequestsPerMinute());
            config.setGeminiRequestsPerDay(source.getGeminiRequestsPerDay());
            config.setGeminiUsageLimitScope(source.getGeminiUsageLimitScope());
            config.setUsageDataPath(source.getUsageDataPath());
            selectCandidate(config, source.getActiveApiKey(), source.getActiveModel());
        }
        StructuredOutputMode mode = outputMode == null ? StructuredOutputMode.NONE : outputMode;
        int maxOutputTokens = effectiveMaxOutputTokens(config.getMaxOutputTokens(), mode, config.getThinkingLevel());
        Map<String, String> context = contextData == null ? Map.of() : new HashMap<>(contextData);
        String systemMessage = replacePlaceholders(systemPrompt == null ? "" : systemPrompt, context);
        List<ChatGPTRequestMessage> history = new ArrayList<>();
        int remainingContextTokens = (int) (Math.max(0, config.getMaxContextTokens() - maxOutputTokens)
                * config.getPercentOfContext());
        int usedTokens = estimateTokenSize("system: " + systemMessage);
        if (messageHistory != null) {
            // Resolve the mutable ChatMessage fields now, before the caller can change them during a retry.
            List<ChatGPTRequestMessage> copiedHistory = new ArrayList<>();
            for (ChatMessage message : messageHistory) {
                copiedHistory.add(new ChatGPTRequestMessage(
                        message.sender.toString().toLowerCase(Locale.ENGLISH),
                        replacePlaceholders(message.message, context)));
            }
            for (int i = copiedHistory.size() - 1; i >= 0; i--) {
                ChatGPTRequestMessage message = copiedHistory.get(i);
                int messageTokens = estimateTokenSize(message.role + ": " + message.content);
                if (usedTokens + messageTokens > remainingContextTokens) {
                    break;
                }
                history.add(message);
                usedTokens += messageTokens;
            }
            Collections.reverse(history);
        }
        List<String> secrets = new ArrayList<>();
        if (config.getApiKey() != null) {
            for (String key : config.getApiKey().split(",")) {
                if (!key.isBlank()) secrets.add(key.trim());
            }
        }
        // Redact a longer overlapping key before a shorter key can hide part of it.
        secrets.sort(Comparator.comparingInt(String::length).reversed());
        return new RequestSnapshot(config, source, List.copyOf(history), systemMessage, mode,
                maxOutputTokens, List.copyOf(secrets));
    }

    private static RequestResult runRequest(RequestSnapshot snapshot, boolean nativeGemini) {
        ConfigurationHandler.Config config = snapshot.config();
        ApiUsageLimiter limiter = usageLimiter;
        String apiUrl = config.getUrl();
        int timeout = config.getTimeout() * 1000;
        int keyCount = Math.max(1, config.getApiKeyCount());
        int modelCount = Math.max(1, config.getModelCount());
        int maxAttempts = keyCount * modelCount;
        RequestResult failure = null;
        long shortestRetryAfterMillis = Long.MAX_VALUE;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String activeKey = config.getActiveApiKey();
            String modelName = config.getActiveModel();
            publishCandidatePreference(snapshot, activeKey, modelName);
            // Match the established compatible policy: prefer another candidate on connection failure,
            // with one transient retry after the final candidate. Every real attempt reserves quota.
            for (int connectionAttempt = 0; connectionAttempt < 2; connectionAttempt++) {
                HttpURLConnection connection = null;
                try {
                    ApiUsageLimiter.Reservation reservation = limiter.tryReserve(config, apiUrl, activeKey, modelName);
                    if (!reservation.allowed()) {
                        if (reservation.retryAfterMillis() > 0) {
                            shortestRetryAfterMillis = Math.min(shortestRetryAfterMillis, reservation.retryAfterMillis());
                        }
                        LOGGER.warn("Skipping AI request candidate due to local usage limit: model={}, reason={}, retry_after_ms={}",
                                sanitize(modelName, snapshot.secrets()), reservation.reason(), reservation.retryAfterMillis());
                        break;
                    }

                    String endpoint = nativeGemini ? GeminiNativeRequest.endpoint(apiUrl, modelName) : apiUrl;
                    URL url = URI.create(endpoint).toURL();
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty(nativeGemini ? "x-goog-api-key" : "Authorization",
                            nativeGemini ? activeKey : "Bearer " + activeKey);
                    connection.setRequestProperty("Connection", "keep-alive");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("Accept-Encoding", "gzip");
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(timeout);
                    connection.setReadTimeout(timeout);
                    Object payload = nativeGemini
                            ? GeminiNativeRequest.buildPayload(snapshot.systemMessage(), snapshot.history(),
                                    snapshot.outputMode(), modelName, snapshot.maxOutputTokens(), config.getThinkingLevel())
                            : compatiblePayload(snapshot, modelName);
                    byte[] input = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
                    connection.setFixedLengthStreamingMode(input.length);
                    try (OutputStream stream = connection.getOutputStream()) {
                        stream.write(input);
                    }
                    int statusCode = connection.getResponseCode();
                    if (statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                        if (statusCode == 429) {
                            limiter.markProviderRateLimited(config, apiUrl, activeKey, modelName);
                        }
                        // Provider-controlled bodies and headers are sanitized before parsing or logging.
                        String reason = sanitize(connection.getResponseMessage(), snapshot.secrets());
                        String body = sanitize(readResponse(connection.getErrorStream(), connection.getContentEncoding()), snapshot.secrets());
                        String cleanError = body.isEmpty() ? "Unknown error" : parseAndLogErrorResponse(body, snapshot.secrets());
                        boolean tryNextCandidate = shouldTryNextCandidate(statusCode, cleanError, attempt, maxAttempts);
                        String message = "HTTP " + statusCode + (reason == null || reason.isEmpty() ? "" : " " + reason);
                        cleanError = userFacingProviderError(cleanError);
                        if (cleanError != null && !cleanError.isEmpty() && !"Unknown error".equals(cleanError)) {
                            message += ": " + cleanError;
                        } else if (!body.isEmpty()) {
                            message += ": " + (body.length() > 300 ? body.substring(0, 300) + "..." : body);
                        }
                        failure = failedResult(snapshot, statusCode, message);
                        LOGGER.error(failure.errorMessage());
                        if (!tryNextCandidate) {
                            return failure;
                        }
                        LOGGER.warn("API request returned HTTP {}. Trying next API key/model candidate (attempt {} of {}).",
                                statusCode, attempt, maxAttempts);
                        break;
                    }
                    String body = readResponse(connection.getInputStream(), connection.getContentEncoding());
                    ResponseContent response = nativeGemini
                            ? GeminiNativeRequest.parseSuccessResponse(body) : parseCompatibleResponse(body);
                    if (response == null) {
                        return failedResult(snapshot, 0, "Failed to parse response");
                    }
                    String safeContent = sanitize(response.content(), snapshot.secrets());
                    String safeFinishReason = sanitize(response.finishReason(), snapshot.secrets());
                    String warning = structuredResponseWarning(snapshot.outputMode(), safeContent,
                            safeFinishReason, sanitize(modelName, snapshot.secrets()), snapshot.maxOutputTokens(), response.completionTokens());
                    if (warning != null) LOGGER.warn(warning);
                    // A successful retry constructs fresh diagnostics and drops all earlier failure fields.
                    return new RequestResult(safeContent, 0, null,
                            safeFinishReason, response.completionTokens(), preview(safeContent), warning, snapshot.maxOutputTokens());
                } catch (IOException connectionFailure) {
                    String message = sanitize(connectionFailure.getMessage(), snapshot.secrets());
                    failure = failedResult(snapshot, -1, "No Internet or Blocked Request: " + message);
                    if (attempt < maxAttempts) {
                        LOGGER.warn("Connection failed on attempt {} of {}, trying next candidate: {}", attempt, maxAttempts, message);
                        break;
                    }
                    if (connectionAttempt == 0) {
                        LOGGER.warn("Connection failed, retrying same candidate (1/1): {}", message);
                    } else {
                        LOGGER.warn(failure.errorMessage());
                        return failure;
                    }
                } catch (Exception requestFailure) {
                    failure = failedResult(snapshot, 0, "Failed to request message: " + requestFailure.getMessage());
                    LOGGER.error(failure.errorMessage());
                    return failure;
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
            rotateCandidate(config, attempt, keyCount, modelCount);
        }
        if (failure != null) return failure;
        long retrySeconds = shortestRetryAfterMillis == Long.MAX_VALUE ? 0L
                : Math.max(1L, (shortestRetryAfterMillis + 999L) / 1000L);
        return failedResult(snapshot, 429, retrySeconds > 0
                ? "Local AI usage limit reached for all configured candidates. Try again in about " + retrySeconds + " seconds."
                : "Local AI usage limit reached for all configured candidates.");
    }

    private static ChatGPTRequestPayload compatiblePayload(RequestSnapshot snapshot, String modelName) {
        List<ChatGPTRequestMessage> messages = new ArrayList<>();
        // Gemini compatible endpoints require user content even for an empty history/config test.
        messages.add(new ChatGPTRequestMessage(snapshot.history().isEmpty() ? "user" : "system", snapshot.systemMessage()));
        messages.addAll(snapshot.history());
        return new ChatGPTRequestPayload(snapshot.config().getUrl(), modelName, messages, snapshot.outputMode(),
                1.0f, snapshot.maxOutputTokens(), snapshot.config().getThinkingLevel());
    }

    private static ResponseContent parseCompatibleResponse(String body) {
        ChatGPTResponse response = GSON.fromJson(body, ChatGPTResponse.class);
        if (response == null || response.choices == null || response.choices.isEmpty()) return null;
        ChatGPTResponse.ChatGPTChoice choice = response.choices.get(0);
        String content = "";
        if (choice.message != null) {
            content = choice.message.content != null ? choice.message.content : choice.message.refusal;
        }
        return new ResponseContent(content, choice.finish_reason,
                response.usage == null ? null : response.usage.completion_tokens);
    }

    private static String readResponse(InputStream stream, String encoding) throws IOException {
        if (stream == null) return "";
        try (InputStream raw = stream;
             InputStream decoded = "gzip".equalsIgnoreCase(encoding) ? new GZIPInputStream(raw) : raw) {
            return new String(decoded.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static RequestResult failedResult(RequestSnapshot snapshot, int code, String message) {
        return new RequestResult(null, code, sanitize(message, snapshot.secrets()), null, null, null, null,
                snapshot.maxOutputTokens());
    }

    private static synchronized RequestResult publishLegacyDiagnostics(RequestResult result) {
        lastErrorCode = result.errorCode();
        lastErrorMessage = result.errorMessage();
        lastFinishReason = result.finishReason();
        lastCompletionTokens = result.completionTokens();
        lastResponsePreview = result.responsePreview();
        lastStructuredResponseWarning = result.structuredResponseWarning();
        lastRequestedMaxOutputTokens = result.requestedMaxOutputTokens();
        return result;
    }

    private static void publishCandidatePreference(RequestSnapshot snapshot, String key, String model) {
        ConfigurationHandler.Config source = snapshot.sourceConfig();
        synchronized (source) {
            // A retry must not overwrite an administrator's newer configuration.
            if (Objects.equals(source.getApiKey(), snapshot.config().getApiKey())
                    && Objects.equals(source.getModel(), snapshot.config().getModel())
                    && Objects.equals(source.getUrl(), snapshot.config().getUrl())) {
                selectCandidate(source, key, model);
            }
        }
    }

    private static void selectCandidate(ConfigurationHandler.Config config, String key, String model) {
        for (int i = 0; i < config.getApiKeyCount() && !Objects.equals(config.getActiveApiKey(), key); i++) {
            config.rotateApiKey();
        }
        for (int i = 0; i < config.getModelCount() && !Objects.equals(config.getActiveModel(), model); i++) {
            config.rotateModel();
        }
    }

    private static int estimateTokenSize(String text) {
        return (int) Math.round(text.length() / 3.5);
    }

    public static void resetUsageLimiterForTests() {
        usageLimiter = new ApiUsageLimiter();
    }

    private static String sanitize(String message, List<String> secrets) {
        if (message == null || secrets.isEmpty()) return message;
        StringBuilder safe = new StringBuilder();
        int cursor = 0;
        while (cursor < message.length()) {
            int start = message.indexOf('"', cursor);
            if (start < 0) break;
            int end = -1;
            boolean escaped = false;
            for (int index = start + 1; index < message.length(); index++) {
                char current = message.charAt(index);
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    end = index;
                    break;
                }
            }
            if (end < 0) break;
            safe.append(redactLiteral(message.substring(cursor, start), secrets));
            String token = message.substring(start, end + 1);
            try {
                // Decode complete string tokens before redaction: a JSON escape can hide part of a key.
                // Only changed strings are reserialized; action numbers, structure, fences and incomplete
                // tails keep their original representation, including the parser's salvage paths.
                String decoded = JsonParser.parseString(token).getAsString();
                String redacted = redactLiteral(decoded, secrets);
                safe.append(decoded.equals(redacted) ? token : GSON.toJson(redacted));
            } catch (RuntimeException malformedToken) {
                safe.append(redactLiteral(token, secrets));
            }
            cursor = end + 1;
        }
        safe.append(redactLiteral(message.substring(cursor), secrets));
        return safe.toString();
    }

    private static String redactLiteral(String message, List<String> secrets) {
        for (String secret : secrets) {
            message = message.replace(secret, "**********");
        }
        return message;
    }

    public static boolean isNativeGeminiUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase(Locale.ENGLISH);
        return lower.contains("generativelanguage.googleapis.com") && !lower.contains("/openai");
    }

    private static boolean shouldTryNextCandidate(int statusCode, String providerError, int attempt, int maxAttempts) {
        if (attempt >= maxAttempts) {
            return false;
        }
        if (isLocationRestrictionError(providerError)) {
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

    private static boolean isLocationRestrictionError(String providerError) {
        if (providerError == null) {
            return false;
        }
        String normalized = providerError.toLowerCase(Locale.ENGLISH);
        return normalized.contains("user location is not supported for the api use");
    }

    private static String userFacingProviderError(String providerError) {
        if (isLocationRestrictionError(providerError)) {
            return "Google AI Studio is unavailable from this network location. Use a supported network or choose another provider.";
        }
        return providerError;
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
        if (!List.of("minimal", "low", "medium", "high").contains(normalizedThinking)) {
            return false;
        }
        String normalizedUrl = apiUrl == null ? "" : apiUrl.toLowerCase(Locale.ENGLISH);
        String normalizedModel = modelName == null ? "" : modelName.toLowerCase(Locale.ENGLISH);
        return normalizedUrl.contains("generativelanguage.googleapis.com")
                || normalizedModel.startsWith("gemini-");
    }

    static int effectiveMaxOutputTokens(int configuredTokens, StructuredOutputMode outputMode, String thinkingLevel) {
        int configured = Math.max(ConfigurationHandler.Config.MIN_MAX_OUTPUT_TOKENS, configuredTokens);
        StructuredOutputMode mode = outputMode == null ? StructuredOutputMode.NONE : outputMode;
        if (mode == StructuredOutputMode.NONE) {
            return configured;
        }

        String normalizedThinking = thinkingLevel == null ? "auto" : thinkingLevel.trim().toLowerCase(Locale.ENGLISH);
        int floor = mode == StructuredOutputMode.CHARACTER ? 1536 : 1024;
        if ("medium".equals(normalizedThinking)) {
            floor = mode == StructuredOutputMode.CHARACTER ? 2048 : 1536;
        } else if ("high".equals(normalizedThinking)) {
            floor = mode == StructuredOutputMode.CHARACTER ? 3072 : 2048;
        }
        return Math.max(configured, floor);
    }

    private static String structuredResponseWarning(
            StructuredOutputMode outputMode,
            String content,
            String finishReason,
            String modelName,
            int maxOutputTokens,
            Integer completionTokens) {
        if (outputMode == null || outputMode == StructuredOutputMode.NONE) {
            return null;
        }

        String preview = preview(content);
        if ("length".equalsIgnoreCase(finishReason) || "MAX_TOKENS".equalsIgnoreCase(finishReason)) {
            return "Structured AI response was truncated: mode=" + outputMode
                    + ", model=" + modelName
                    + ", finish_reason=" + finishReason
                    + ", max_tokens=" + maxOutputTokens
                    + ", completion_tokens=" + completionTokens
                    + ", preview=" + preview;
        }
        if (content == null || content.trim().isEmpty()) {
            return "Structured AI response was empty: mode=" + outputMode
                    + ", model=" + modelName
                    + ", finish_reason=" + finishReason
                    + ", max_tokens=" + maxOutputTokens;
        }
        if (!containsCompleteJsonObject(content)) {
            return "Structured AI response did not include a complete JSON object: mode=" + outputMode
                    + ", model=" + modelName
                    + ", finish_reason=" + finishReason
                    + ", max_tokens=" + maxOutputTokens
                    + ", completion_tokens=" + completionTokens
                    + ", preview=" + preview;
        }
        return null;
    }

    private static String preview(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replace("\r", " ").replace("\n", " ").trim();
        while (normalized.contains("  ")) {
            normalized = normalized.replace("  ", " ");
        }
        return normalized.length() > 160 ? normalized.substring(0, 160) + "..." : normalized;
    }

    private static boolean containsCompleteJsonObject(String input) {
        if (input == null) {
            return false;
        }
        int depth = 0;
        boolean started = false;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < input.length(); i++) {
            char current = input.charAt(i);
            if (!started) {
                if (current == '{') {
                    started = true;
                    depth = 1;
                }
                continue;
            }
            if (escaped) {
                escaped = false;
                continue;
            }
            if (inString && current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
