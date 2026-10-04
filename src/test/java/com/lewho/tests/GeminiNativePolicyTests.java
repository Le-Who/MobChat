// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lewho.chat.ChatDataManager;
import com.lewho.chat.ChatGPTRequest;
import com.lewho.chat.ChatMessage;
import com.lewho.chat.GeminiNativeRequest;
import com.lewho.commands.ConfigurationHandler;
import com.lewho.message.MessageParser;
import com.lewho.message.ParsedMessage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class GeminiNativePolicyTests {
    @TempDir Path tempDir;
    private HttpServer server;
    private ExecutorService executor;
    private final List<Request> requests = Collections.synchronizedList(new ArrayList<>());
    private final List<CountDownLatch> releases = new ArrayList<>();

    @AfterEach
    void closeServer() {
        releases.forEach(CountDownLatch::countDown);
        if (server != null) server.stop(0);
        if (executor != null) executor.shutdownNow();
        ChatGPTRequest.resetUsageLimiterForTests();
        ChatGPTRequest.lastErrorCode = 0;
        ChatGPTRequest.lastErrorMessage = null;
    }

    @Test
    void nativeMinuteLimitRotatesBeforeSendingAnotherRequest() throws Exception {
        ConfigurationHandler.Config config = start(request -> success());
        config.setApiKey("AIza-first,AIza-second");
        config.setGeminiRequestsPerMinute(1);

        assertEquals("ok", fetch(config));
        assertEquals("ok", fetch(config));

        assertEquals(List.of("AIza-first", "AIza-second"), requests.stream().map(Request::key).toList());
    }

    @Test
    void nativeDailyLimitSurvivesLimiterReloadWithoutAnotherHttpRequest() throws Exception {
        ConfigurationHandler.Config config = start(request -> success());
        config.setGeminiRequestsPerDay(1);
        assertEquals("ok", fetch(config));
        ChatGPTRequest.resetUsageLimiterForTests();

        assertNull(fetch(config));
        assertEquals(1, requests.size());
        assertEquals(429, ChatGPTRequest.lastErrorCode);
    }

    @Test
    void nativeSharedScopeCannotBypassMinuteLimitByRotatingKeys() throws Exception {
        ConfigurationHandler.Config config = start(request -> success());
        config.setApiKey("AIza-first,AIza-second");
        config.setGeminiUsageLimitScope("shared");
        config.setGeminiRequestsPerMinute(1);

        assertEquals("ok", fetch(config));
        assertNull(fetch(config));
        assertEquals(1, requests.size());
    }

    @Test
    void nativeFallbackVisitsEachKeyModelPairOnce() throws Exception {
        ConfigurationHandler.Config config = start(request -> error(404, "model unavailable"));
        config.setApiKey("AIza-first,AIza-second");
        config.setModel("gemini-first,gemini-second");

        assertNull(fetch(config));
        assertEquals(List.of(
                "AIza-first /models/gemini-first:generateContent",
                "AIza-first /models/gemini-second:generateContent",
                "AIza-second /models/gemini-first:generateContent",
                "AIza-second /models/gemini-second:generateContent"),
                requests.stream().map(request -> request.key() + " " + request.path()).toList());
    }

    @Test
    void nativeModelFallbackCanRecoverFromAnUnavailableModel() throws Exception {
        ConfigurationHandler.Config config = start(request -> request.path().contains("gemini-second")
                ? success() : error(404, "model unavailable"));
        config.setModel("gemini-first,gemini-second");

        assertEquals("ok", fetch(config));
        assertEquals(2, requests.size());
    }

    @Test
    void nativeFallbackReplacesModelInAnExplicitGenerateContentUrl() throws Exception {
        ConfigurationHandler.Config config = start(request -> request.path().contains("gemini-second")
                ? success() : error(404, "model unavailable"));
        config.setUrl(config.getUrl() + "/models/gemini-first:generateContent");
        config.setModel("gemini-first,gemini-second");

        assertEquals("ok", fetch(config));
        assertEquals(List.of("/models/gemini-first:generateContent", "/models/gemini-second:generateContent"),
                requests.stream().map(Request::path).toList());
    }

    @Test
    void nativeRegionRestrictionStopsAfterOneRequest() throws Exception {
        ConfigurationHandler.Config config = start(request -> error(400, "User location is not supported for the API use."));
        config.setApiKey("AIza-first,AIza-second");

        assertNull(fetch(config));
        assertEquals(1, requests.size());
        assertTrue(ChatGPTRequest.lastErrorMessage.contains("network location"));
    }

    @Test
    void nativePermanentErrorDoesNotRetrySameCandidate() throws Exception {
        ConfigurationHandler.Config config = start(request -> error(422, "invalid request"));
        config.setApiKey("AIza-first,AIza-second");

        assertNull(fetch(config));
        assertEquals(1, requests.size());
        assertEquals(422, ChatGPTRequest.lastErrorCode);
    }

    @Test
    void nativeErrorRedactsEveryConfiguredKey() throws Exception {
        ConfigurationHandler.Config config = start(request -> error(422, "Rejected AIza-first and AIza-second"));
        config.setApiKey("AIza-first,AIza-second");

        assertNull(fetch(config));
        assertNotNull(ChatGPTRequest.lastErrorMessage);
        assertTrue(ChatGPTRequest.lastErrorMessage.contains("Rejected"), ChatGPTRequest.lastErrorMessage);
        assertFalse(ChatGPTRequest.lastErrorMessage.contains("AIza-first"));
        assertFalse(ChatGPTRequest.lastErrorMessage.contains("AIza-second"));
    }

    @Test
    void nativeSuccessClearsEarlierHttpError() throws Exception {
        ConfigurationHandler.Config config = start(request -> requests.size() == 1 ? error(429, "busy") : success());
        config.setApiKey("AIza-first,AIza-second");

        assertEquals("ok", fetch(config));
        assertEquals(0, ChatGPTRequest.lastErrorCode);
        assertNull(ChatGPTRequest.lastErrorMessage);
    }

    @Test
    void nativeTruncationPublishesFreshDiagnostics() throws Exception {
        ConfigurationHandler.Config config = start(request -> new Response(200,
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"message\\\":\\\"hello\\\"\"}]},\"finishReason\":\"MAX_TOKENS\"}],\"usageMetadata\":{\"candidatesTokenCount\":17}}"));
        ChatGPTRequest.lastCompletionTokens = 999;
        ChatGPTRequest.lastRequestedMaxOutputTokens = 999;

        String content = GeminiNativeRequest.fetchMessageFromGemini(config, "sys", Map.of(), List.of(),
                ChatGPTRequest.StructuredOutputMode.CHAT).get(10, TimeUnit.SECONDS);

        assertEquals("{\"message\":\"hello\"", content);
        assertEquals(17, ChatGPTRequest.lastCompletionTokens);
        assertEquals(1024, ChatGPTRequest.lastRequestedMaxOutputTokens);
        assertNotNull(ChatGPTRequest.lastStructuredResponseWarning);
        assertTrue(ChatGPTRequest.lastStructuredResponseWarning.contains("truncated"));
    }

    @Test
    void nativeEmptyTruncationRetainsEnvelopeDiagnostics() throws Exception {
        ConfigurationHandler.Config config = start(request -> new Response(200,
                "{\"candidates\":[{\"content\":{\"parts\":[]},\"finishReason\":\"MAX_TOKENS\"}],\"usageMetadata\":{\"candidatesTokenCount\":17}}"));

        assertNull(GeminiNativeRequest.fetchMessageFromGemini(config, "sys", Map.of(), List.of(),
                ChatGPTRequest.StructuredOutputMode.CHAT).get(10, TimeUnit.SECONDS));
        assertEquals("MAX_TOKENS", ChatGPTRequest.lastFinishReason);
        assertEquals(17, ChatGPTRequest.lastCompletionTokens);
        assertEquals(1024, ChatGPTRequest.lastRequestedMaxOutputTokens);
        assertNotNull(ChatGPTRequest.lastStructuredResponseWarning);
        assertTrue(ChatGPTRequest.lastStructuredResponseWarning.contains("truncated"));
    }

    @Test
    void nativeRetryUsesRequestSnapshotAfterCallerChangesInputs() throws Exception {
        CountDownLatch received = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        releases.add(release);
        ConfigurationHandler.Config config = start(request -> {
            if (requests.size() == 1) {
                received.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("test release timed out");
                return error(429, "busy");
            }
            return success();
        });
        config.setApiKey("AIza-first,AIza-second");
        ChatMessage message = new ChatMessage("Hello {{name}}", ChatDataManager.ChatSender.USER, "player");
        List<ChatMessage> history = new ArrayList<>(List.of(message));
        Map<String, String> context = new HashMap<>(Map.of("name", "original"));
        CompletableFuture<String> future = GeminiNativeRequest.fetchMessageFromGemini(config, "sys {{name}}", context,
                history, ChatGPTRequest.StructuredOutputMode.NONE);
        assertTrue(received.await(5, TimeUnit.SECONDS));
        context.put("name", "changed");
        message.message = "changed message";
        history.clear();
        config.setApiKey("AIza-changed");
        config.setModel("gemini-changed");
        release.countDown();

        assertEquals("ok", future.get(10, TimeUnit.SECONDS));
        Request retried = requests.get(1);
        assertEquals("AIza-second", retried.key());
        assertEquals("/models/gemini-first:generateContent", retried.path());
        assertEquals("sys original", retried.body().getAsJsonObject("systemInstruction")
                .getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString());
        assertEquals("Hello original", retried.body().getAsJsonArray("contents").get(0)
                .getAsJsonObject().getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString());
    }

    @Test
    void nativeHistoryHonorsConfiguredContextBudget() throws Exception {
        ConfigurationHandler.Config config = start(request -> success());
        config.setMaxContextTokens(200);
        config.setMaxOutputTokens(64);
        config.setPercentOfContext(1.0);
        List<ChatMessage> history = List.of(
                new ChatMessage("old".repeat(1000), ChatDataManager.ChatSender.USER, "player"),
                new ChatMessage("latest", ChatDataManager.ChatSender.USER, "player"));

        GeminiNativeRequest.fetchMessageFromGemini(config, "sys", Map.of(), history,
                ChatGPTRequest.StructuredOutputMode.NONE).get(10, TimeUnit.SECONDS);

        assertEquals(1, requests.get(0).body().getAsJsonArray("contents").size());
        assertEquals("latest", requests.get(0).body().getAsJsonArray("contents").get(0)
                .getAsJsonObject().getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString());
    }

    @Test
    void nativeGeminiThreeSendsConfiguredThinkingLevel() throws Exception {
        ConfigurationHandler.Config config = start(request -> success());
        config.setModel("gemini-3.5-flash-lite");
        config.setThinkingLevel("high");

        assertEquals("ok", fetch(config));

        JsonObject generation = requests.get(0).body().getAsJsonObject("generationConfig");
        assertTrue(generation.has("thinkingConfig"));
        assertEquals("high", generation.getAsJsonObject("thinkingConfig").get("thinkingLevel").getAsString());
        assertFalse(generation.has("temperature"));
    }

    @Test
    void nativeThinkingLevelIsOmittedForAutoAndGeminiTwoPointFive() throws Exception {
        ConfigurationHandler.Config config = start(request -> success());
        config.setModel("gemini-3.5-flash-lite");
        config.setThinkingLevel("auto");
        assertEquals("ok", fetch(config));
        config.setModel("gemini-2.5-flash");
        config.setThinkingLevel("high");
        assertEquals("ok", fetch(config));

        for (Request request : requests) {
            assertFalse(request.body().getAsJsonObject("generationConfig").has("thinkingConfig"));
        }
    }

    @Test
    void compatibleRetryUsesInputSnapshot() throws Exception {
        CountDownLatch received = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        releases.add(release);
        ConfigurationHandler.Config config = start(request -> {
            if (requests.size() == 1) {
                received.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("test release timed out");
                return error(429, "busy");
            }
            return new Response(200, "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");
        });
        config.setApiKey("key-first,key-second");
        config.setModel("model-first");
        ChatMessage message = new ChatMessage("Hello {{name}}", ChatDataManager.ChatSender.USER, "player");
        List<ChatMessage> history = new ArrayList<>(List.of(message));
        Map<String, String> context = new HashMap<>(Map.of("name", "original"));
        CompletableFuture<String> future = ChatGPTRequest.fetchMessageFromChatGPT(config, "sys {{name}}", context,
                history, false);
        assertTrue(received.await(5, TimeUnit.SECONDS));
        context.put("name", "changed");
        message.message = "changed message";
        history.clear();
        config.setApiKey("key-changed");
        config.setModel("model-changed");
        release.countDown();

        assertEquals("ok", future.get(10, TimeUnit.SECONDS));
        Request retried = requests.get(1);
        assertEquals("key-second", retried.key());
        assertEquals("model-first", retried.body().get("model").getAsString());
        assertEquals("sys original", retried.body().getAsJsonArray("messages").get(0)
                .getAsJsonObject().get("content").getAsString());
        assertEquals("Hello original", retried.body().getAsJsonArray("messages").get(1)
                .getAsJsonObject().get("content").getAsString());
        assertEquals("key-changed", config.getActiveApiKey());
    }

    @Test
    void completedRequestResultsKeepTheirOwnDiagnosticsDuringConcurrentRequests() throws Exception {
        CountDownLatch received = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        releases.add(release);
        ConfigurationHandler.Config failing = start(request -> {
            if ("first-key".equals(request.key())) {
                received.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("test release timed out");
                return error(422, "Rejected first-key and second-key");
            }
            return new Response(200, "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"ok\"}}],\"usage\":{\"completion_tokens\":9}}");
        });
        failing.setApiKey("first-key,second-key");
        failing.setModel("model");
        failing.setTimeout(5);
        CompletableFuture<ChatGPTRequest.RequestResult> first = scopedFetch(failing);
        assertTrue(received.await(5, TimeUnit.SECONDS));
        ConfigurationHandler.Config succeeding = new ConfigurationHandler.Config();
        succeeding.setUrl(failing.getUrl());
        succeeding.setApiKey("second-key");
        succeeding.setModel("model");
        ChatGPTRequest.RequestResult success = scopedFetch(succeeding).get(10, TimeUnit.SECONDS);
        release.countDown();
        ChatGPTRequest.RequestResult failure = first.get(10, TimeUnit.SECONDS);

        assertEquals("ok", success.content());
        assertEquals(0, success.errorCode());
        assertNull(success.errorMessage());
        assertEquals("stop", success.finishReason());
        assertEquals(9, success.completionTokens());
        assertNull(failure.content());
        assertEquals(422, failure.errorCode());
        String message = failure.errorMessage();
        assertFalse(message.contains("first-key"));
        assertFalse(message.contains("second-key"));
        assertNull(failure.finishReason());
        assertNull(failure.completionTokens());
    }

    @Test
    void compatibleErrorRedactsEveryConfiguredKeyBeforeLogging() throws Exception {
        ConfigurationHandler.Config config = start(request -> error(422, "Rejected AIza-first and AIza-second"));
        config.setApiKey("AIza-first,AIza-second");
        Logger logger = (Logger) LogManager.getLogger("creaturechat");
        List<String> logs = Collections.synchronizedList(new ArrayList<>());
        AbstractAppender appender = new AbstractAppender("request-redaction-test", null, null, false, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                logs.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        logger.addAppender(appender);
        try {
            assertNull(ChatGPTRequest.fetchMessageFromChatGPT(config, "sys", Map.of(), List.of(), false)
                    .get(10, TimeUnit.SECONDS));
            assertNotNull(ChatGPTRequest.lastErrorMessage);
            assertTrue(ChatGPTRequest.lastErrorMessage.contains("Rejected"), ChatGPTRequest.lastErrorMessage);
            assertFalse(ChatGPTRequest.lastErrorMessage.contains("AIza-first"));
            assertFalse(ChatGPTRequest.lastErrorMessage.contains("AIza-second"));
            assertFalse(logs.isEmpty());
            for (String event : logs) {
                assertFalse(event.contains("AIza-first"));
                assertFalse(event.contains("AIza-second"));
            }
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }
    }

    @Test
    void escapedProviderErrorRedactsDecodedKeysBeforeLogging() throws Exception {
        ConfigurationHandler.Config config = start(request -> new Response(422,
                "{\"error\":{\"message\":\"Rejected \\u0041Iza-first and \\u0041Iza-second\"}}"));
        config.setApiKey("AIza-first,AIza-second");
        Logger logger = (Logger) LogManager.getLogger("creaturechat");
        List<String> logs = Collections.synchronizedList(new ArrayList<>());
        AbstractAppender appender = new AbstractAppender("escaped-request-redaction-test", null, null, false, Property.EMPTY_ARRAY) {
            @Override
            public void append(LogEvent event) {
                logs.add(event.getMessage().getFormattedMessage());
            }
        };
        appender.start();
        logger.addAppender(appender);
        try {
            assertNull(ChatGPTRequest.fetchMessageFromChatGPT(config, "sys", Map.of(), List.of(), false)
                    .get(10, TimeUnit.SECONDS));
            assertEquals(1, requests.size());
            assertEquals(422, ChatGPTRequest.lastErrorCode);
            assertTrue(ChatGPTRequest.lastErrorMessage.contains("Rejected"), ChatGPTRequest.lastErrorMessage);
            assertFalse(logs.isEmpty());
            assertTrue(logs.stream().anyMatch(event -> event.startsWith("Error Message: Rejected ")),
                    "Parser error was not captured: " + String.join("\n", logs));
            for (String event : logs) {
                assertFalse(event.contains("AIza-first"), event);
                assertFalse(event.contains("AIza-second"), event);
            }
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }
    }

    @Test
    void compatibleSuccessRedactsKeysInScopedContentAndLegacyWrapper() throws Exception {
        ConfigurationHandler.Config config = start(request -> new Response(200,
                "{\"choices\":[{\"message\":{\"content\":\"Accepted AIza-first and AIza-second\"}}]}"));
        config.setApiKey("AIza-first,AIza-second");

        ChatGPTRequest.RequestResult result = scopedFetch(config).get(10, TimeUnit.SECONDS);
        assertEquals("Accepted ********** and **********", result.content());
        assertEquals("Accepted ********** and **********", ChatGPTRequest.fetchMessageFromChatGPT(
                config, "sys", Map.of(), List.of(), false).get(10, TimeUnit.SECONDS));
        assertEquals(2, requests.size());
    }

    @Test
    void nativeSuccessRedactsKeysInScopedContentAndLegacyWrapper() throws Exception {
        ConfigurationHandler.Config config = start(request -> new Response(200,
                "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Accepted AIza-first and AIza-second\"}]},\"finishReason\":\"STOP\"}]}"));
        config.setApiKey("AIza-first,AIza-second");
        // The public URL router recognizes the provider marker; the endpoint remains entirely on loopback.
        config.setUrl(config.getUrl() + "/generativelanguage.googleapis.com");

        ChatGPTRequest.RequestResult result = scopedFetch(config).get(10, TimeUnit.SECONDS);
        assertEquals("Accepted ********** and **********", result.content());
        assertEquals("Accepted ********** and **********", GeminiNativeRequest.fetchMessageFromGemini(
                config, "sys", Map.of(), List.of(), ChatGPTRequest.StructuredOutputMode.NONE).get(10, TimeUnit.SECONDS));
        assertEquals(2, requests.size());
        for (Request request : requests) {
            assertTrue(request.body().has("generationConfig"));
            assertFalse(request.body().has("messages"));
        }
    }

    @Test
    void compatibleStructuredContentRedactsDecodedChatFieldsAndPreservesActions() throws Exception {
        verifyStructuredContentRedaction(false);
    }

    @Test
    void nativeStructuredContentRedactsDecodedChatFieldsAndPreservesActions() throws Exception {
        verifyStructuredContentRedaction(true);
    }

    private void verifyStructuredContentRedaction(boolean nativeGemini) throws Exception {
        String complete = "{\"message\":\"Accepted AIza\\u002dfirst and AIza\\u002dsecond\","
                + "\"mood\":\"friendly\",\"memory_updates\":[\"Remember AIza\\u002dfirst\"],"
                + "\"actions\":[{\"type\":\"FOLLOW\",\"value\":0}]}";
        String truncated = "{\"message\":\"Accepted AIza\\u002dfirst and AIza\\u002dsecond\","
                + "\"mood\":\"friendly\",\"memory_updates\":[],\"actions\":[";
        AtomicReference<String> content = new AtomicReference<>(complete);
        ConfigurationHandler.Config config = start(request -> structuredSuccess(content.get(), nativeGemini));
        config.setApiKey("AIza-first,AIza-second");
        if (nativeGemini) config.setUrl(config.getUrl() + "/generativelanguage.googleapis.com");

        for (String response : List.of(complete, "Here is the JSON requested:\n```json\n" + complete + "\n```", truncated)) {
            content.set(response);
            ChatGPTRequest.RequestResult scoped = ChatGPTRequest.fetchResultFromChatGPT(
                    config, "sys", Map.of(), List.of(), ChatGPTRequest.StructuredOutputMode.CHAT).get(10, TimeUnit.SECONDS);
            String legacy = nativeGemini ? GeminiNativeRequest.fetchMessageFromGemini(config, "sys", Map.of(), List.of(),
                    ChatGPTRequest.StructuredOutputMode.CHAT).get(10, TimeUnit.SECONDS)
                    : ChatGPTRequest.fetchMessageFromChatGPT(config, "sys", Map.of(), List.of(),
                    ChatGPTRequest.StructuredOutputMode.CHAT).get(10, TimeUnit.SECONDS);
            for (String output : List.of(scoped.content(), legacy)) {
                ParsedMessage parsed = MessageParser.parseMessage(output);
                assertEquals("Accepted ********** and **********", parsed.getCleanedMessage());
                assertEquals("friendly", parsed.getMood());
                if (response.equals(truncated)) {
                    assertTrue(parsed.getBehaviors().isEmpty());
                    assertTrue(parsed.getMemoryUpdates().isEmpty());
                } else {
                    assertEquals(List.of("Remember **********"), parsed.getMemoryUpdates());
                    assertEquals(1, parsed.getBehaviors().size());
                    assertEquals("FOLLOW", parsed.getBehaviors().get(0).getName());
                    assertEquals(0, parsed.getBehaviors().get(0).getArgument());
                }
            }
        }
        assertEquals(6, requests.size());
    }

    private static Response structuredSuccess(String content, boolean nativeGemini) {
        JsonObject envelope = JsonParser.parseString(nativeGemini
                ? "{\"candidates\":[{\"content\":{\"parts\":[{}]},\"finishReason\":\"STOP\"}]}"
                : "{\"choices\":[{\"message\":{},\"finish_reason\":\"stop\"}]}").getAsJsonObject();
        if (nativeGemini) {
            envelope.getAsJsonArray("candidates").get(0).getAsJsonObject().getAsJsonObject("content")
                    .getAsJsonArray("parts").get(0).getAsJsonObject().addProperty("text", content);
        } else {
            envelope.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message")
                    .addProperty("content", content);
        }
        return new Response(200, envelope.toString());
    }

    @Test
    void nativeConnectionTimeoutRetriesOnceAndClearsErrorAfterRecovery() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        releases.add(release);
        ConfigurationHandler.Config config = start(request -> {
            if (requests.size() == 1) release.await(5, TimeUnit.SECONDS);
            return success();
        });

        assertEquals("ok", fetch(config));
        assertEquals(2, requests.size());
        assertEquals(0, ChatGPTRequest.lastErrorCode);
        assertNull(ChatGPTRequest.lastErrorMessage);
    }

    @Test
    void nativeTransientRetryReservesQuotaBeforeOpeningAnotherConnection() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        releases.add(release);
        ConfigurationHandler.Config config = start(request -> {
            release.await(5, TimeUnit.SECONDS);
            return success();
        });
        config.setGeminiRequestsPerMinute(1);

        assertNull(fetch(config));
        assertEquals(1, requests.size());
    }

    private static CompletableFuture<ChatGPTRequest.RequestResult> scopedFetch(ConfigurationHandler.Config config) {
        return ChatGPTRequest.fetchResultFromChatGPT(config, "sys", Map.of(), List.of(),
                ChatGPTRequest.StructuredOutputMode.NONE);
    }

    private ConfigurationHandler.Config start(Responder responder) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            try {
                JsonObject body = JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                String key = exchange.getRequestHeaders().getFirst("x-goog-api-key");
                if (key == null) {
                    String authorization = exchange.getRequestHeaders().getFirst("Authorization");
                    key = authorization == null ? null : authorization.replaceFirst("^Bearer ", "");
                }
                Request request = new Request(key,
                        exchange.getRequestURI().getPath(), body);
                requests.add(request);
                respond(exchange, responder.respond(request));
            } catch (Exception failure) {
                respond(exchange, error(500, failure.toString()));
            } finally {
                exchange.close();
            }
        });
        server.start();
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setUrl("http://127.0.0.1:" + server.getAddress().getPort());
        config.setApiKey("AIza-first");
        config.setModel("gemini-first");
        config.setUsageDataPath(tempDir.resolve("usage.json"));
        config.setTimeout(1);
        return config;
    }

    private String fetch(ConfigurationHandler.Config config) throws Exception {
        return GeminiNativeRequest.fetchMessageFromGemini(config, "sys", Map.of(), List.of(),
                ChatGPTRequest.StructuredOutputMode.NONE).get(10, TimeUnit.SECONDS);
    }

    private static Response success() {
        return new Response(200, "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"ok\"}]},\"finishReason\":\"STOP\"}]}");
    }

    private static Response error(int status, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("message", message);
        JsonObject body = new JsonObject();
        body.add("error", error);
        return new Response(status, body.toString());
    }

    private static void respond(HttpExchange exchange, Response response) throws IOException {
        byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(response.status(), bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    private record Request(String key, String path, JsonObject body) {}
    private record Response(int status, String body) {}
    private interface Responder { Response respond(Request request) throws Exception; }
}
