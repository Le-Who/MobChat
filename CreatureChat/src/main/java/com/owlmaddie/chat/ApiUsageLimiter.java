// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.owlmaddie.commands.ConfigurationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tracks provider request usage before HTTP calls so local key/model rotation can avoid known quotas.
 */
public class ApiUsageLimiter {
    private static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long MINUTE_MILLIS = 60_000L;
    private static final ZoneId PACIFIC_ZONE = ZoneId.of("America/Los_Angeles");

    private final Clock clock;
    private final Map<Path, UsageState> states = new HashMap<>();

    public ApiUsageLimiter() {
        this(Clock.systemUTC());
    }

    public ApiUsageLimiter(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    public synchronized Reservation tryReserve(ConfigurationHandler.Config config, String apiUrl, String apiKey, String modelName) {
        if (!shouldTrack(config, apiUrl, apiKey, modelName)) {
            return Reservation.allowed(false);
        }

        UsageState state = stateFor(config.getUsageDataPath());
        String bucketId = bucketId(config, apiKey, modelName);
        UsageBucket bucket = state.buckets.computeIfAbsent(bucketId, ignored -> new UsageBucket(currentPacificDay(), 0));
        syncBucketDay(bucket);
        pruneMinuteWindow(bucket);

        long now = clock.millis();
        if (bucket.blockedUntilMillis > now) {
            return Reservation.denied("provider-rate-limit", bucket.blockedUntilMillis - now);
        }

        int rpmLimit = config.getGeminiRequestsPerMinute();
        if (rpmLimit > 0 && bucket.minuteRequests.size() >= rpmLimit) {
            Long oldest = bucket.minuteRequests.peekFirst();
            long retryAfterMillis = oldest == null ? MINUTE_MILLIS : Math.max(1L, oldest + MINUTE_MILLIS - now);
            return Reservation.denied("minute-limit", retryAfterMillis);
        }

        int dailyLimit = config.getGeminiRequestsPerDay();
        if (dailyLimit > 0 && bucket.dailyCount >= dailyLimit) {
            return Reservation.denied("daily-limit", millisUntilNextPacificDay());
        }

        bucket.minuteRequests.addLast(now);
        bucket.dailyCount++;
        saveState(state);
        return Reservation.allowed(true);
    }

    public synchronized void markProviderRateLimited(ConfigurationHandler.Config config, String apiUrl, String apiKey, String modelName) {
        if (!shouldTrack(config, apiUrl, apiKey, modelName)) {
            return;
        }
        UsageState state = stateFor(config.getUsageDataPath());
        String bucketId = bucketId(config, apiKey, modelName);
        UsageBucket bucket = state.buckets.computeIfAbsent(bucketId, ignored -> new UsageBucket(currentPacificDay(), 0));
        bucket.blockedUntilMillis = Math.max(bucket.blockedUntilMillis, clock.millis() + MINUTE_MILLIS);
    }

    private boolean shouldTrack(ConfigurationHandler.Config config, String apiUrl, String apiKey, String modelName) {
        if (config == null || !config.getGeminiUsageLimitsEnabled()) {
            return false;
        }
        String normalizedModel = normalize(modelName);
        if (!normalizedModel.startsWith("gemini-")) {
            return false;
        }

        String normalizedUrl = normalize(apiUrl);
        String key = apiKey == null ? "" : apiKey.trim();
        return normalizedUrl.contains("generativelanguage.googleapis.com")
                || key.startsWith("AIza");
    }

    private UsageState stateFor(Path usageDataPath) {
        Path path = usageDataPath == null
                ? ConfigurationHandler.Config.DEFAULT_USAGE_DATA_PATH
                : usageDataPath;
        Path normalizedPath = path.toAbsolutePath().normalize();
        return states.computeIfAbsent(normalizedPath, this::loadState);
    }

    private UsageState loadState(Path path) {
        UsageState state = new UsageState(path);
        if (!Files.exists(path)) {
            return state;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            StoredUsageFile stored = GSON.fromJson(reader, StoredUsageFile.class);
            if (stored != null && stored.buckets != null) {
                for (Map.Entry<String, StoredBucket> entry : stored.buckets.entrySet()) {
                    StoredBucket storedBucket = entry.getValue();
                    if (storedBucket != null) {
                        state.buckets.put(entry.getKey(), new UsageBucket(storedBucket.day, Math.max(0, storedBucket.dailyCount)));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load CreatureChat AI usage data from {}", path, e);
        }
        return state;
    }

    private void saveState(UsageState state) {
        StoredUsageFile stored = new StoredUsageFile();
        for (Map.Entry<String, UsageBucket> entry : state.buckets.entrySet()) {
            UsageBucket bucket = entry.getValue();
            StoredBucket storedBucket = new StoredBucket();
            storedBucket.day = bucket.day;
            storedBucket.dailyCount = bucket.dailyCount;
            stored.buckets.put(entry.getKey(), storedBucket);
        }

        try {
            Path parent = state.path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(state.path, StandardCharsets.UTF_8)) {
                GSON.toJson(stored, writer);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to save CreatureChat AI usage data to {}", state.path, e);
        }
    }

    private String bucketId(ConfigurationHandler.Config config, String apiKey, String modelName) {
        String scope = config.getGeminiUsageLimitScope();
        String owner = "shared".equals(scope) ? "shared" : "key:" + hashSecret(apiKey);
        return "gemini:" + owner + ":model:" + normalize(modelName);
    }

    private String hashSecret(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < Math.min(8, hashed.length); i++) {
                result.append(String.format("%02x", hashed[i]));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString((value == null ? "" : value).hashCode());
        }
    }

    private void syncBucketDay(UsageBucket bucket) {
        String today = currentPacificDay();
        if (!today.equals(bucket.day)) {
            bucket.day = today;
            bucket.dailyCount = 0;
            bucket.minuteRequests.clear();
            bucket.blockedUntilMillis = 0L;
        }
    }

    private void pruneMinuteWindow(UsageBucket bucket) {
        long cutoff = clock.millis() - MINUTE_MILLIS;
        while (!bucket.minuteRequests.isEmpty() && bucket.minuteRequests.peekFirst() <= cutoff) {
            bucket.minuteRequests.removeFirst();
        }
    }

    private String currentPacificDay() {
        return LocalDate.now(clock.withZone(PACIFIC_ZONE)).toString();
    }

    private long millisUntilNextPacificDay() {
        ZonedDateTime now = ZonedDateTime.ofInstant(Instant.ofEpochMilli(clock.millis()), PACIFIC_ZONE);
        ZonedDateTime nextDay = now.toLocalDate().plusDays(1).atStartOfDay(PACIFIC_ZONE);
        return Math.max(1L, nextDay.toInstant().toEpochMilli() - now.toInstant().toEpochMilli());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }

    public static class Reservation {
        private final boolean allowed;
        private final boolean tracked;
        private final String reason;
        private final long retryAfterMillis;

        private Reservation(boolean allowed, boolean tracked, String reason, long retryAfterMillis) {
            this.allowed = allowed;
            this.tracked = tracked;
            this.reason = reason;
            this.retryAfterMillis = retryAfterMillis;
        }

        public static Reservation allowed(boolean tracked) {
            return new Reservation(true, tracked, "", 0L);
        }

        public static Reservation denied(String reason, long retryAfterMillis) {
            return new Reservation(false, true, reason, retryAfterMillis);
        }

        public boolean allowed() {
            return allowed;
        }

        public boolean tracked() {
            return tracked;
        }

        public String reason() {
            return reason;
        }

        public long retryAfterMillis() {
            return retryAfterMillis;
        }
    }

    private static class UsageState {
        final Path path;
        final Map<String, UsageBucket> buckets = new HashMap<>();

        UsageState(Path path) {
            this.path = path;
        }
    }

    private static class UsageBucket {
        String day;
        int dailyCount;
        long blockedUntilMillis;
        final ArrayDeque<Long> minuteRequests = new ArrayDeque<>();

        UsageBucket(String day, int dailyCount) {
            this.day = day;
            this.dailyCount = dailyCount;
        }
    }

    private static class StoredUsageFile {
        Map<String, StoredBucket> buckets = new HashMap<>();
    }

    private static class StoredBucket {
        String day;
        int dailyCount;
    }
}
