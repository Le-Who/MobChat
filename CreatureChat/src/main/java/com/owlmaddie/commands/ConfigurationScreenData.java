// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.commands;

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

        config.setUrl(url);
        config.setModel(models);
        config.setTimeout(data.timeout());
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

    public record OpenData(String provider, String url, String apiKeys, String maskedApiKeys, String models, int timeout, String thinkingLevel) {
        public OpenData(String provider, String url, String apiKeys, String maskedApiKeys, String models, int timeout) {
            this(provider, url, apiKeys, maskedApiKeys, models, timeout, "auto");
        }
    }

    public record SaveData(String provider, String url, String apiKeys, String models, int timeout, String thinkingLevel) {
        public SaveData(String provider, String url, String apiKeys, String models, int timeout) {
            this(provider, url, apiKeys, models, timeout, "auto");
        }
    }
}
