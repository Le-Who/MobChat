// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.chat.ChatGPTRequest;
import com.lewho.chat.GeminiNativeRequest;
import com.lewho.commands.ConfigurationHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class GeminiNativeRequestTests {

    private HttpServer server;

    @AfterEach
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        ChatGPTRequest.lastErrorCode = 0;
        ChatGPTRequest.lastErrorMessage = null;
    }

    @Test
    public void testIsNativeGeminiUrl() {
        assertTrue(ChatGPTRequest.isNativeGeminiUrl("https://generativelanguage.googleapis.com/v1beta"));
        assertFalse(ChatGPTRequest.isNativeGeminiUrl("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"));
        assertFalse(ChatGPTRequest.isNativeGeminiUrl("https://api.openai.com/v1/chat/completions"));
    }

    @Test
    public void testNativeGeminiRequestSuccess() throws Exception {
        int port = 9098;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/models/gemini-3.5-flash-lite:generateContent", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String apiKeyHeader = exchange.getRequestHeaders().getFirst("x-goog-api-key");
                assertEquals("test-gemini-key", apiKeyHeader);

                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(body.contains("systemInstruction"));
                assertTrue(exchange.getRequestURI().getPath().contains("gemini-3.5-flash-lite"));

                String response = "{\n" +
                        "  \"candidates\": [\n" +
                        "    {\n" +
                        "      \"content\": {\n" +
                        "        \"parts\": [\n" +
                        "          {\n" +
                        "            \"text\": \"Hello from Native Gemini!\"\n" +
                        "          }\n" +
                        "        ]\n" +
                        "      },\n" +
                        "      \"finishReason\": \"STOP\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            }
        });
        server.start();

        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setApiKey("test-gemini-key");
        config.setUrl("http://localhost:" + port);
        config.setModel("gemini-3.5-flash-lite");

        CompletableFuture<String> future = GeminiNativeRequest.fetchMessageFromGemini(
                config,
                "System prompt",
                new HashMap<>(),
                new ArrayList<>(),
                ChatGPTRequest.StructuredOutputMode.NONE
        );

        String result = future.get();
        assertEquals("Hello from Native Gemini!", result);
        assertEquals("STOP", ChatGPTRequest.lastFinishReason);
    }

    @Test
    public void testStripAdditionalPropertiesRemovesAtAllLevels() {
        // Build a schema that mirrors what creatureChatResponse() produces:
        // root level and a nested object inside 'properties' both carry additionalProperties.
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("type", "object");
        inner.put("additionalProperties", false);
        inner.put("required", List.of("type", "value"));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "object");
        root.put("additionalProperties", false);
        root.put("properties", Map.of("action", inner));

        Map<String, Object> stripped = GeminiNativeRequest.stripAdditionalProperties(root);

        assertFalse(stripped.containsKey("additionalProperties"), "root must not contain additionalProperties");
        assertEquals("object", stripped.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> strippedProperties = (Map<String, Object>) stripped.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> strippedInner = (Map<String, Object>) strippedProperties.get("action");
        assertFalse(strippedInner.containsKey("additionalProperties"), "nested object must not contain additionalProperties");
        assertEquals("object", strippedInner.get("type"));
    }

    @Test
    public void testChatAndCharacterPayloadsOmitAdditionalProperties() throws Exception {
        int port = 9099;
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Capture the request body for both CHAT and CHARACTER modes in one shared handler.
        String[] capturedBody = {null};
        server.createContext("/models/gemini-3.5-flash-lite:generateContent", exchange -> {
            InputStream is = exchange.getRequestBody();
            capturedBody[0] = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String response = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{}\"}]},\"finishReason\":\"STOP\"}]}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setApiKey("test-gemini-key");
        config.setUrl("http://localhost:" + port);
        config.setModel("gemini-3.5-flash-lite");

        for (ChatGPTRequest.StructuredOutputMode mode : List.of(
                ChatGPTRequest.StructuredOutputMode.CHAT,
                ChatGPTRequest.StructuredOutputMode.CHARACTER)) {
            capturedBody[0] = null;
            GeminiNativeRequest.fetchMessageFromGemini(
                    config, "sys", new HashMap<>(), new ArrayList<>(), mode).get();
            assertNotNull(capturedBody[0], "Expected a request body for mode " + mode);
            assertFalse(capturedBody[0].contains("additionalProperties"),
                    "Payload for mode " + mode + " must not contain additionalProperties");
        }
    }
}
