// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.tests;

import com.owlmaddie.chat.ApiUsageLimiter;
import com.owlmaddie.commands.ConfigurationHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GeminiUsageLimiterTests {

    @TempDir
    Path tempDir;

    @Test
    public void perKeyMinuteLimitBlocksOnlyTheExhaustedKey() {
        ConfigurationHandler.Config config = geminiConfig();
        config.setGeminiRequestsPerMinute(1);
        config.setGeminiRequestsPerDay(450);
        config.setGeminiUsageLimitScope("per_key");

        ApiUsageLimiter limiter = new ApiUsageLimiter(fixedClock("2026-07-07T12:00:00Z"));

        assertTrue(limiter.tryReserve(config, config.getUrl(), "AIza-key-one", config.getActiveModel()).allowed());
        assertFalse(limiter.tryReserve(config, config.getUrl(), "AIza-key-one", config.getActiveModel()).allowed());
        assertTrue(limiter.tryReserve(config, config.getUrl(), "AIza-key-two", config.getActiveModel()).allowed());
    }

    @Test
    public void dailyUsagePersistsAcrossLimiterInstances() {
        Path usagePath = tempDir.resolve("creaturechat_usage.json");
        ConfigurationHandler.Config config = geminiConfig();
        config.setGeminiRequestsPerMinute(20);
        config.setGeminiRequestsPerDay(1);
        config.setGeminiUsageLimitScope("per_key");
        config.setUsageDataPath(usagePath);

        Clock clock = fixedClock("2026-07-07T12:00:00Z");
        ApiUsageLimiter firstLimiter = new ApiUsageLimiter(clock);
        assertTrue(firstLimiter.tryReserve(config, config.getUrl(), "AIza-key-one", config.getActiveModel()).allowed());

        ApiUsageLimiter secondLimiter = new ApiUsageLimiter(clock);
        assertFalse(secondLimiter.tryReserve(config, config.getUrl(), "AIza-key-one", config.getActiveModel()).allowed());
        assertTrue(secondLimiter.tryReserve(config, config.getUrl(), "AIza-key-two", config.getActiveModel()).allowed());
    }

    @Test
    public void sharedScopeTreatsAllKeysAsOneQuotaBucket() {
        ConfigurationHandler.Config config = geminiConfig();
        config.setGeminiRequestsPerMinute(1);
        config.setGeminiRequestsPerDay(450);
        config.setGeminiUsageLimitScope("shared");

        ApiUsageLimiter limiter = new ApiUsageLimiter(fixedClock("2026-07-07T12:00:00Z"));

        assertTrue(limiter.tryReserve(config, config.getUrl(), "AIza-key-one", config.getActiveModel()).allowed());
        assertFalse(limiter.tryReserve(config, config.getUrl(), "AIza-key-two", config.getActiveModel()).allowed());
    }

    private ConfigurationHandler.Config geminiConfig() {
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setUrl("https://generativelanguage.googleapis.com/v1beta/openai/chat/completions");
        config.setModel("gemini-3.1-flash-lite");
        config.setApiKey("AIza-key-one,AIza-key-two");
        config.setUsageDataPath(tempDir.resolve("usage-" + System.nanoTime() + ".json"));
        return config;
    }

    private Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneId.of("UTC"));
    }
}
