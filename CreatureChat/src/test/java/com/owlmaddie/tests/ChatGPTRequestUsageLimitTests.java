// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.tests;

import com.owlmaddie.chat.ChatGPTRequest;
import com.owlmaddie.commands.ConfigurationHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ChatGPTRequestUsageLimitTests {
    private static final String PATH = "/v1/chat/completions";

    @TempDir
    Path tempDir;

    @AfterEach
    public void resetLimiter() {
        ChatGPTRequest.resetUsageLimiterForTests();
        ChatGPTRequest.lastErrorCode = 0;
        ChatGPTRequest.lastErrorMessage = null;
    }

    @Test
    public void localMinuteLimitRotatesToNextAvailableApiKeyBeforeSendingHttpRequest() throws Exception {
        List<String> authHeadersReceived = Collections.synchronizedList(new ArrayList<>());
        HttpServer server = startSuccessServer(authHeadersReceived);
        String url = "http://localhost:" + server.getAddress().getPort() + PATH;
        ConfigurationHandler.Config config = geminiConfig(url);
        config.setGeminiRequestsPerMinute(1);
        config.setGeminiRequestsPerDay(450);

        String first = executeRequest(config);
        String second = executeRequest(config);
        server.stop(0);

        assertEquals("Success Response", first);
        assertEquals("Success Response", second);
        assertEquals(2, authHeadersReceived.size());
        assertEquals("Bearer AIza-key-one", authHeadersReceived.get(0));
        assertEquals("Bearer AIza-key-two", authHeadersReceived.get(1));
    }

    @Test
    public void localDailyLimitReturns429WithoutSendingAnotherRequestWhenAllKeysAreExhausted() throws Exception {
        List<String> authHeadersReceived = Collections.synchronizedList(new ArrayList<>());
        HttpServer server = startSuccessServer(authHeadersReceived);
        String url = "http://localhost:" + server.getAddress().getPort() + PATH;
        ConfigurationHandler.Config config = geminiConfig(url);
        config.setApiKey("AIza-key-one");
        config.setGeminiRequestsPerMinute(10);
        config.setGeminiRequestsPerDay(1);

        String first = executeRequest(config);
        String second = executeRequest(config);
        server.stop(0);

        assertEquals("Success Response", first);
        assertNull(second);
        assertEquals(1, authHeadersReceived.size());
        assertEquals(429, ChatGPTRequest.lastErrorCode);
    }

    private HttpServer startSuccessServer(List<String> authHeadersReceived) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(PATH, exchange -> {
            String auth = exchange.getRequestHeaders().getFirst("Authorization");
            if (auth != null) {
                authHeadersReceived.add(auth);
            }
            sendSuccess(exchange);
        });
        server.start();
        return server;
    }

    private void sendSuccess(HttpExchange exchange) throws IOException {
        String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Success Response\"}}]}";
        byte[] resp = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }

    private String executeRequest(ConfigurationHandler.Config config) {
        return ChatGPTRequest.fetchMessageFromChatGPT(
                config, "", new HashMap<>(), new ArrayList<>(), false).join();
    }

    private ConfigurationHandler.Config geminiConfig(String url) {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setUrl(url);
        config.setApiKey("AIza-key-one,AIza-key-two");
        config.setModel("gemini-3.1-flash-lite");
        config.setTimeout(1);
        config.setUsageDataPath(tempDir.resolve("creaturechat_usage.json"));
        return config;
    }
}
