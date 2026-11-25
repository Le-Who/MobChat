// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.chat.ApiUsageLimiter;
import com.lewho.commands.ConfigurationHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    public void pacificMidnightResetsDailyCountButKeepsTheRollingMinuteWindow() {
        ConfigurationHandler.Config config = geminiConfig();
        config.setGeminiRequestsPerMinute(1);
        config.setGeminiRequestsPerDay(1);
        MutableClock clock = new MutableClock("2026-07-07T06:59:50Z");
        ApiUsageLimiter limiter = new ApiUsageLimiter(clock);

        assertTrue(limiter.tryReserve(config, config.getUrl(), config.getActiveApiKey(), config.getActiveModel()).allowed());
        clock.set("2026-07-07T07:00:00Z");
        ApiUsageLimiter.Reservation blocked = limiter.tryReserve(config, config.getUrl(),
                config.getActiveApiKey(), config.getActiveModel());
        assertFalse(blocked.allowed());
        assertEquals("minute-limit", blocked.reason());

        clock.set("2026-07-07T07:00:50Z");
        assertTrue(limiter.tryReserve(config, config.getUrl(), config.getActiveApiKey(), config.getActiveModel()).allowed());
    }

    @Test
    public void pacificMidnightKeepsTheProviderCooldown() {
        ConfigurationHandler.Config config = geminiConfig();
        MutableClock clock = new MutableClock("2026-07-07T06:59:50Z");
        ApiUsageLimiter limiter = new ApiUsageLimiter(clock);

        limiter.markProviderRateLimited(config, config.getUrl(), config.getActiveApiKey(), config.getActiveModel());
        clock.set("2026-07-07T07:00:00Z");
        ApiUsageLimiter.Reservation blocked = limiter.tryReserve(config, config.getUrl(),
                config.getActiveApiKey(), config.getActiveModel());
        assertFalse(blocked.allowed());
        assertEquals("provider-rate-limit", blocked.reason());

        clock.set("2026-07-07T07:00:50Z");
        assertTrue(limiter.tryReserve(config, config.getUrl(), config.getActiveApiKey(), config.getActiveModel()).allowed());
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

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(String instant) {
            set(instant);
        }

        private void set(String instant) {
            this.instant = Instant.parse(instant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
