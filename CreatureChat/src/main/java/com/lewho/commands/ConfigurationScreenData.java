// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.commands;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Sanitized configuration data exchanged with the client-side setup screen.
 */
public final class ConfigurationScreenData {
    private ConfigurationScreenData() {
    }

    public static OpenData fromConfig(ConfigurationHandler.Config config) {
        return new OpenData(
                "",
                config.getUrl(),
                "",
                ConfigurationPresets.describeApiKeys(config.getApiKey()),
                toMultiline(config.getModel()),
                config.getTimeout(),
                config.getMaxOutputTokens(),
                config.getThinkingLevel()
        );
    }

    public static void applyToConfig(ConfigurationHandler.Config config, SaveData data) {
        String url = data.url() == null ? "" : data.url().trim();
        String models = normalizeListInput(data.models());
        String apiKeys = normalizeListInput(data.apiKeys());

        if (url.isEmpty()) {
            throw new IllegalArgumentException("URL is required.");
        }
        if (models.isEmpty()) {
            throw new IllegalArgumentException("At least one model is required.");
        }
        if (data.timeout() < 1) {
            throw new IllegalArgumentException("Timeout must be at least 1 second.");
        }
        if (data.maxOutputTokens() < ConfigurationHandler.Config.MIN_MAX_OUTPUT_TOKENS) {
            throw new IllegalArgumentException("Output tokens must be at least " + ConfigurationHandler.Config.MIN_MAX_OUTPUT_TOKENS + ".");
        }

        config.setUrl(url);
        config.setModel(models);
        config.setTimeout(data.timeout());
        config.setMaxOutputTokens(data.maxOutputTokens());
        config.setThinkingLevel(data.thinkingLevel());
        if (!apiKeys.isEmpty()) {
            config.setApiKey(apiKeys);
        }
    }

    public static String normalizeListInput(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        return List.of(value.split("[,\\r\\n]+"))
                .stream()
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.joining(","));
    }

    public static String toMultiline(String csv) {
        List<String> values = ConfigurationPresets.splitCsv(csv);
        if (values.isEmpty()) {
            return "";
        }
        return String.join("\n", values);
    }

    public record OpenData(String provider, String url, String apiKeys, String maskedApiKeys, String models, int timeout, int maxOutputTokens, String thinkingLevel) {
        public OpenData(String provider, String url, String apiKeys, String maskedApiKeys, String models, int timeout, String thinkingLevel) {
            this(provider, url, apiKeys, maskedApiKeys, models, timeout, ConfigurationHandler.Config.DEFAULT_MAX_OUTPUT_TOKENS, thinkingLevel);
        }

        public OpenData(String provider, String url, String apiKeys, String maskedApiKeys, String models, int timeout) {
            this(provider, url, apiKeys, maskedApiKeys, models, timeout, ConfigurationHandler.Config.DEFAULT_MAX_OUTPUT_TOKENS, "auto");
        }
    }

    public record SaveData(String provider, String url, String apiKeys, String models, int timeout, int maxOutputTokens, String thinkingLevel) {
        public SaveData(String provider, String url, String apiKeys, String models, int timeout, String thinkingLevel) {
            this(provider, url, apiKeys, models, timeout, ConfigurationHandler.Config.DEFAULT_MAX_OUTPUT_TOKENS, thinkingLevel);
        }

        public SaveData(String provider, String url, String apiKeys, String models, int timeout) {
            this(provider, url, apiKeys, models, timeout, ConfigurationHandler.Config.DEFAULT_MAX_OUTPUT_TOKENS, "auto");
        }
    }
}
