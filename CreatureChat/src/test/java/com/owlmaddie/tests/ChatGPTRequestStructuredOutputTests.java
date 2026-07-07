// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.tests;

import com.owlmaddie.chat.ChatGPTRequest;
import com.owlmaddie.commands.ConfigurationHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChatGPTRequestStructuredOutputTests {
    private static final String PATH = "/v1/chat/completions";

    @Test
    public void jsonModeUsesStructuredResponseSchema() throws Exception {
        List<String> requestBodies = new ArrayList<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(PATH, exchange -> handleSuccess(exchange, requestBodies));
        server.start();

        String url = "http://localhost:" + server.getAddress().getPort() + PATH;
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setUrl(url);
        config.setApiKey("test-key");
        config.setModel("gemini-test");
        config.setTimeout(1);

        CompletableFuture<String> future = ChatGPTRequest.fetchMessageFromChatGPT(
                config, "Reply as JSON.", new HashMap<>(), new ArrayList<>(), true);
        String response = future.join();

        server.stop(0);

        assertEquals("{\"message\":\"ok\",\"actions\":[]}", response);
        assertEquals(1, requestBodies.size());
        String body = requestBodies.get(0);
        assertTrue(body.contains("\"response_format\""));
        assertTrue(body.contains("\"type\":\"json_schema\""));
        assertTrue(body.contains("\"name\":\"creaturechat_response\""));
        assertTrue(body.contains("\"FOLLOW\""));
        assertTrue(body.contains("\"FRIENDSHIP\""));
        assertTrue(body.contains("\"required\":[\"type\",\"value\"]"));
        assertTrue(body.contains("\"type\":[\"integer\",\"null\"]"));
    }

    @Test
    public void characterModeUsesStructuredCharacterSchema() throws Exception {
        List<String> requestBodies = new ArrayList<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(PATH, exchange -> handleSuccess(exchange, requestBodies));
        server.start();

        String url = "http://localhost:" + server.getAddress().getPort() + PATH;
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setUrl(url);
        config.setApiKey("test-key");
        config.setModel("gemini-test");
        config.setTimeout(1);

        CompletableFuture<String> future = ChatGPTRequest.fetchMessageFromChatGPT(
                config, "Create a character.", new HashMap<>(), new ArrayList<>(),
                ChatGPTRequest.StructuredOutputMode.CHARACTER);
        String response = future.join();

        server.stop(0);

        assertEquals("{\"message\":\"ok\",\"actions\":[]}", response);
        assertEquals(1, requestBodies.size());
        String body = requestBodies.get(0);
        assertTrue(body.contains("\"response_format\""));
        assertTrue(body.contains("\"type\":\"json_schema\""));
        assertTrue(body.contains("\"name\":\"creaturechat_character\""));
        assertTrue(body.contains("\"short_greeting\""));
        assertTrue(body.contains("\"speaking_style\""));
        assertTrue(body.contains("\"class_name\""));
        assertTrue(!body.contains("\"actions\""));
    }

    @Test
    public void systemPromptPreservesJsonExampleQuotes() throws Exception {
        List<String> requestBodies = new ArrayList<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(PATH, exchange -> handleSuccess(exchange, requestBodies));
        server.start();

        String url = "http://localhost:" + server.getAddress().getPort() + PATH;
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setUrl(url);
        config.setApiKey("test-key");
        config.setModel("gemini-test");
        config.setTimeout(1);

        HashMap<String, String> context = new HashMap<>();
        context.put("player_name", "Steve");
        String systemPrompt = "Output ONLY JSON like {\"message\":\"Hi {{player_name}}\",\"actions\":[]}";

        ChatGPTRequest.fetchMessageFromChatGPT(
                config, systemPrompt, context, new ArrayList<>(), true).join();

        server.stop(0);

        assertEquals(1, requestBodies.size());
        String body = requestBodies.get(0);
        assertTrue(body.contains("\\\"message\\\""));
        assertTrue(body.contains("\\\"Hi Steve\\\""));
        assertTrue(body.contains("\\\"actions\\\""));
    }

    @Test
    public void geminiThinkingLevelAddsReasoningEffort() throws Exception {
        List<String> requestBodies = new ArrayList<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(PATH, exchange -> handleSuccess(exchange, requestBodies));
        server.start();

        String url = "http://localhost:" + server.getAddress().getPort() + PATH;
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setUrl("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions");
        config.setApiKey("test-key");
        config.setModel("gemini-3.5-flash");
        config.setThinkingLevel("high");
        config.setTimeout(1);
        config.setUrl(url);

        ChatGPTRequest.fetchMessageFromChatGPT(
                config, "Reply as JSON.", new HashMap<>(), new ArrayList<>(), true).join();

        server.stop(0);

        assertEquals(1, requestBodies.size());
        assertTrue(requestBodies.get(0).contains("\"reasoning_effort\":\"high\""));
    }

    @Test
    public void nonGeminiRequestDoesNotSendReasoningEffort() throws Exception {
        List<String> requestBodies = new ArrayList<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(PATH, exchange -> handleSuccess(exchange, requestBodies));
        server.start();

        String url = "http://localhost:" + server.getAddress().getPort() + PATH;
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setUrl(url);
        config.setApiKey("test-key");
        config.setModel("gpt-4o-mini");
        config.setThinkingLevel("high");
        config.setTimeout(1);

        ChatGPTRequest.fetchMessageFromChatGPT(
                config, "Reply as JSON.", new HashMap<>(), new ArrayList<>(), true).join();

        server.stop(0);

        assertEquals(1, requestBodies.size());
        assertTrue(!requestBodies.get(0).contains("reasoning_effort"));
    }

    @Test
    public void openRouterGeminiModelDoesNotSendGoogleReasoningEffort() throws Exception {
        List<String> requestBodies = new ArrayList<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(PATH, exchange -> handleSuccess(exchange, requestBodies));
        server.start();

        String url = "http://localhost:" + server.getAddress().getPort() + PATH;
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setUrl(url);
        config.setApiKey("test-key");
        config.setModel("google/gemini-2.5-flash");
        config.setThinkingLevel("high");
        config.setTimeout(1);

        ChatGPTRequest.fetchMessageFromChatGPT(
                config, "Reply as JSON.", new HashMap<>(), new ArrayList<>(), true).join();

        server.stop(0);

        assertEquals(1, requestBodies.size());
        assertTrue(!requestBodies.get(0).contains("reasoning_effort"));
    }

    private static void handleSuccess(HttpExchange exchange, List<String> requestBodies) throws IOException {
        requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"message\\\":\\\"ok\\\",\\\"actions\\\":[]}\"}}]}";
        byte[] resp = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }
}
