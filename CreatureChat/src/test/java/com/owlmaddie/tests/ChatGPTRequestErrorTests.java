// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.tests;

import com.owlmaddie.chat.ChatGPTRequest;
import com.owlmaddie.chat.EntityChatData;
import com.owlmaddie.commands.ConfigurationHandler;
import com.owlmaddie.utils.Randomizer;
import com.owlmaddie.i18n.TR;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests error handling in {@link ChatGPTRequest} for various HTTP and connection failures.
 */
public class ChatGPTRequestErrorTests {

    private static final String PATH = "/v1/chat/completions";

    @AfterEach
    public void reset() {
        ChatGPTRequest.lastErrorCode = 0;
        ChatGPTRequest.lastErrorMessage = null;
    }

    private ConfigurationHandler.Config buildConfig(String url) {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setApiKey("test-key");
        config.setModel("test-model");
        config.setTimeout(1);
        config.setUrl(url);
        return config;
    }

    private Randomizer.ErrorType errorTypeFor(int code) {
        return switch (code) {
            case -1 -> Randomizer.ErrorType.CONNECTION;
            case 401 -> Randomizer.ErrorType.CODE401;
            case 403 -> Randomizer.ErrorType.CODE403;
            case 429 -> Randomizer.ErrorType.CODE429;
            case 500 -> Randomizer.ErrorType.CODE500;
            case 503 -> Randomizer.ErrorType.CODE503;
            default -> Randomizer.ErrorType.GENERAL;
        };
    }

    private HttpServer startServer(int status, String message) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(PATH, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String body = "{\"error\":{\"message\":\"" + message + "\",\"type\":\"test\",\"code\":\"" + status + "\"}}";
                byte[] resp = body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
            }
        });
        server.start();
        return server;
    }

    private void executeRequest(ConfigurationHandler.Config config) {
        CompletableFuture<String> future = ChatGPTRequest.fetchMessageFromChatGPT(
                config, "", new HashMap<>(), new ArrayList<>(), false);
        future.join();
    }

    private void logOutput(String serverResponse, int code, String randomMsg, String solution) {
        System.out.println("Server Output: " + serverResponse);
        System.out.println("Error Code: " + code);
        System.out.println("Random Message: " + randomMsg);
        System.out.println("Help: " + solution);
    }

    @Test
    public void invalidApiKey() throws Exception {
        int code = 401;
        String message = "Invalid API key";
        HttpServer server = startServer(code, message);
        String url = "http://localhost:" + server.getAddress().getPort() + PATH;
        ConfigurationHandler.Config config = buildConfig(url);

        executeRequest(config);
        server.stop(0);

        TR tr = Randomizer.getRandomError(errorTypeFor(code));
        String randomMsg = String.format(tr.en(), Randomizer.DISCORD_LINK);
        TR solution = EntityChatData.getSolutionMessage(code);
        logOutput(ChatGPTRequest.lastErrorMessage, ChatGPTRequest.lastErrorCode, randomMsg, solution.en());

        assertEquals(code, ChatGPTRequest.lastErrorCode);
        assertNotNull(ChatGPTRequest.lastErrorMessage);
        assertFalse(ChatGPTRequest.lastErrorMessage.isEmpty());
        assertNotNull(randomMsg);
        assertEquals("Solution: Add a valid API key", solution.en());
    }

    @Test
    public void regionForbidden() throws Exception {
        int code = 403;
        String message = "Region is blocked";
        HttpServer server = startServer(code, message);
        String url = "http://localhost:" + server.getAddress().getPort() + PATH;
        ConfigurationHandler.Config config = buildConfig(url);

        executeRequest(config);
        server.stop(0);

        TR tr = Randomizer.getRandomError(errorTypeFor(code));
        String randomMsg = String.format(tr.en(), Randomizer.DISCORD_LINK);
        TR solution = EntityChatData.getSolutionMessage(code);
        logOutput(ChatGPTRequest.lastErrorMessage, ChatGPTRequest.lastErrorCode, randomMsg, solution.en());

        assertEquals(code, ChatGPTRequest.lastErrorCode);
        assertNotNull(ChatGPTRequest.lastErrorMessage);
        assertFalse(ChatGPTRequest.lastErrorMessage.isEmpty());
        assertNotNull(randomMsg);
        assertEquals("Solution: Check region or VPN", solution.en());
    }

    @Test
    public void outOfTokens() throws Exception {
        int code = 429;
        String message = "Rate limit exceeded";
        HttpServer server = startServer(code, message);
        String url = "http://localhost:" + server.getAddress().getPort() + PATH;
        ConfigurationHandler.Config config = buildConfig(url);

        executeRequest(config);
        server.stop(0);

        TR tr = Randomizer.getRandomError(errorTypeFor(code));
        String randomMsg = String.format(tr.en(), Randomizer.DISCORD_LINK);
        TR solution = EntityChatData.getSolutionMessage(code);
        logOutput(ChatGPTRequest.lastErrorMessage, ChatGPTRequest.lastErrorCode, randomMsg, solution.en());

        assertEquals(code, ChatGPTRequest.lastErrorCode);
        assertNotNull(ChatGPTRequest.lastErrorMessage);
        assertFalse(ChatGPTRequest.lastErrorMessage.isEmpty());
        assertNotNull(randomMsg);
        assertEquals("Solution: Add funds to your account", solution.en());
    }

    @Test
    public void internalServerError() throws Exception {
        int code = 500;
        String message = "Internal server error";
        HttpServer server = startServer(code, message);
        String url = "http://localhost:" + server.getAddress().getPort() + PATH;
        ConfigurationHandler.Config config = buildConfig(url);

        executeRequest(config);
        server.stop(0);

        TR tr = Randomizer.getRandomError(errorTypeFor(code));
        String randomMsg = String.format(tr.en(), Randomizer.DISCORD_LINK);
        TR solution = EntityChatData.getSolutionMessage(code);
        logOutput(ChatGPTRequest.lastErrorMessage, ChatGPTRequest.lastErrorCode, randomMsg, solution.en());

        assertEquals(code, ChatGPTRequest.lastErrorCode);
        assertNotNull(ChatGPTRequest.lastErrorMessage);
        assertFalse(ChatGPTRequest.lastErrorMessage.isEmpty());
        assertNotNull(randomMsg);
        assertEquals("Solution: Server error, try again later", solution.en());
    }

    @Test
    public void serviceUnavailable() throws Exception {
        int code = 503;
        String message = "Service unavailable";
        HttpServer server = startServer(code, message);
        String url = "http://localhost:" + server.getAddress().getPort() + PATH;
        ConfigurationHandler.Config config = buildConfig(url);

        executeRequest(config);
        server.stop(0);

        TR tr = Randomizer.getRandomError(errorTypeFor(code));
        String randomMsg = String.format(tr.en(), Randomizer.DISCORD_LINK);
        TR solution = EntityChatData.getSolutionMessage(code);
        logOutput(ChatGPTRequest.lastErrorMessage, ChatGPTRequest.lastErrorCode, randomMsg, solution.en());

        assertEquals(code, ChatGPTRequest.lastErrorCode);
        assertNotNull(ChatGPTRequest.lastErrorMessage);
        assertFalse(ChatGPTRequest.lastErrorMessage.isEmpty());
        assertNotNull(randomMsg);
        assertEquals("Solution: Try again later", solution.en());
    }

    @Test
    public void badUrl() {
        ConfigurationHandler.Config config = buildConfig("http://");
        executeRequest(config);

        TR tr = Randomizer.getRandomError(errorTypeFor(0));
        String randomMsg = String.format(tr.en(), Randomizer.DISCORD_LINK);
        TR solution = EntityChatData.getSolutionMessage(0);
        logOutput(ChatGPTRequest.lastErrorMessage, ChatGPTRequest.lastErrorCode, randomMsg, solution.en());

        assertEquals(0, ChatGPTRequest.lastErrorCode);
        assertNotNull(ChatGPTRequest.lastErrorMessage);
        assertFalse(ChatGPTRequest.lastErrorMessage.isEmpty());
        assertNotNull(randomMsg);
        assertEquals("Solution: Verify the API URL", solution.en());
    }

    @Test
    public void noInternetConnection() {
        ConfigurationHandler.Config config = buildConfig("http://10.255.255.1" + PATH);
        executeRequest(config);

        TR tr = Randomizer.getRandomError(errorTypeFor(-1));
        String randomMsg = String.format(tr.en(), Randomizer.DISCORD_LINK);
        TR solution = EntityChatData.getSolutionMessage(-1);
        logOutput(ChatGPTRequest.lastErrorMessage, ChatGPTRequest.lastErrorCode, randomMsg, solution.en());

        assertEquals(-1, ChatGPTRequest.lastErrorCode);
        assertTrue(ChatGPTRequest.lastErrorMessage.startsWith("No Internet or Blocked Request"));
        assertNotNull(randomMsg);
        assertEquals("Solution: Check internet connection or firewall", solution.en());
    }

    @Test
    public void firewallBlocked() {
        ConfigurationHandler.Config config = buildConfig("http://localhost:1" + PATH);
        executeRequest(config);

        TR tr = Randomizer.getRandomError(errorTypeFor(-1));
        String randomMsg = String.format(tr.en(), Randomizer.DISCORD_LINK);
        TR solution = EntityChatData.getSolutionMessage(-1);
        logOutput(ChatGPTRequest.lastErrorMessage, ChatGPTRequest.lastErrorCode, randomMsg, solution.en());

        assertEquals(-1, ChatGPTRequest.lastErrorCode);
        assertTrue(ChatGPTRequest.lastErrorMessage.startsWith("No Internet or Blocked Request"));
        assertNotNull(randomMsg);
        assertEquals("Solution: Check internet connection or firewall", solution.en());
    }

    @Test
    public void apiRotationOn429() throws Exception {
        List<String> authHeadersReceived = Collections.synchronizedList(new ArrayList<>());

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(PATH, new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                if (auth != null) {
                    authHeadersReceived.add(auth);
                }

                String body = "{\"error\":{\"message\":\"Rate limit exceeded\",\"type\":\"test\",\"code\":\"429\"}}";
                byte[] resp = body.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(429, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
            }
        });
        server.start();

        String url = "http://localhost:" + server.getAddress().getPort() + PATH;
        ConfigurationHandler.Config config = buildConfig(url);
        config.setApiKey("key1,key2,key3");

        executeRequest(config);
        server.stop(0);

        assertEquals(3, authHeadersReceived.size());
        assertEquals("Bearer key1", authHeadersReceived.get(0));
        assertEquals("Bearer key2", authHeadersReceived.get(1));
        assertEquals("Bearer key3", authHeadersReceived.get(2));
        assertEquals(429, ChatGPTRequest.lastErrorCode);
    }

    @Test
    public void apiRotationSuccessOnSecondKey() throws Exception {
        List<String> authHeadersReceived = Collections.synchronizedList(new ArrayList<>());

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(PATH, new HttpHandler() {
            int requestCount = 0;
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                requestCount++;
                String auth = exchange.getRequestHeaders().getFirst("Authorization");
                if (auth != null) {
                    authHeadersReceived.add(auth);
                }

                if (requestCount == 1) {
                    String body = "{\"error\":{\"message\":\"Rate limit\",\"type\":\"test\",\"code\":\"429\"}}";
                    byte[] resp = body.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(429, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                } else {
                    String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Success Response\"}}]}";
                    byte[] resp = body.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, resp.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(resp);
                    }
                }
            }
        });
        server.start();

        String url = "http://localhost:" + server.getAddress().getPort() + PATH;
        ConfigurationHandler.Config config = buildConfig(url);
        config.setApiKey("key1,key2,key3");

        CompletableFuture<String> future = ChatGPTRequest.fetchMessageFromChatGPT(
                config, "", new HashMap<>(), new ArrayList<>(), false);
        String response = future.join();

        server.stop(0);

        assertEquals(2, authHeadersReceived.size());
        assertEquals("Bearer key1", authHeadersReceived.get(0));
        assertEquals("Bearer key2", authHeadersReceived.get(1));
        assertEquals("Success Response", response);
        assertEquals(0, ChatGPTRequest.lastErrorCode);
    }
}
