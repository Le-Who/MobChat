// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Provider presets and display helpers for CreatureChat configuration commands.
 */
public final class ConfigurationPresets {
    public static final String OPENAI_CHAT_COMPLETIONS_URL = "https://api.openai.com/v1/chat/completions";
    public static final String AI_STUDIO_CHAT_COMPLETIONS_URL = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
    public static final String OPENROUTER_CHAT_COMPLETIONS_URL = "https://openrouter.ai/api/v1/chat/completions";
    public static final String GROQ_CHAT_COMPLETIONS_URL = "https://api.groq.com/openai/v1/chat/completions";
    public static final String OLLAMA_CHAT_COMPLETIONS_URL = "http://localhost:11434/v1/chat/completions";

    private static final List<ProviderPreset> PRESETS = List.of(
            new ProviderPreset("openai", "OpenAI", OPENAI_CHAT_COMPLETIONS_URL, "gpt-4o-mini",
                    List.of("chatgpt")),
            new ProviderPreset("ai-studio", "Google AI Studio", AI_STUDIO_CHAT_COMPLETIONS_URL, "gemini-3.1-flash-lite", "minimal",
                    List.of("aistudio", "google", "gemini")),
            new ProviderPreset("openrouter", "OpenRouter", OPENROUTER_CHAT_COMPLETIONS_URL, "openai/gpt-4o-mini",
                    List.of("router")),
            new ProviderPreset("groq", "Groq", GROQ_CHAT_COMPLETIONS_URL, "llama-3.1-8b-instant",
                    List.of()),
            new ProviderPreset("ollama", "Ollama", OLLAMA_CHAT_COMPLETIONS_URL, "llama3.1",
                    List.of("local")),
            new ProviderPreset("litellm", "LiteLLM", "http://localhost:4000/v1/chat/completions", "gpt-4o-mini",
                    List.of("lite-llm"))
    );

    private ConfigurationPresets() {
    }

    public static Optional<ProviderPreset> find(String idOrAlias) {
        if (idOrAlias == null) {
            return Optional.empty();
        }
        String normalized = idOrAlias.trim().toLowerCase(Locale.ENGLISH);
        return PRESETS.stream()
                .filter(preset -> preset.id().equals(normalized) || preset.aliases().contains(normalized))
                .findFirst();
    }

    public static List<String> providerIds() {
        return PRESETS.stream().map(ProviderPreset::id).toList();
    }

    public static List<ProviderPreset> presets() {
        return PRESETS;
    }

    public static void applyPreset(ConfigurationHandler.Config config, ProviderPreset preset) {
        config.setUrl(preset.url());
        config.setModel(preset.defaultModel());
        config.setThinkingLevel(preset.defaultThinkingLevel());
        if (config.getMaxOutputTokens() < ConfigurationHandler.Config.DEFAULT_MAX_OUTPUT_TOKENS) {
            config.setMaxOutputTokens(ConfigurationHandler.Config.DEFAULT_MAX_OUTPUT_TOKENS);
        }
    }

    public static String describeApiKeys(String apiKeys) {
        List<String> keys = splitCsv(apiKeys);
        if (keys.isEmpty()) {
            return "0 key(s)";
        }

        List<String> masked = new ArrayList<>();
        for (String key : keys) {
            masked.add(maskSecret(key));
        }
        return keys.size() + " key(s): " + String.join(", ", masked);
    }

    public static String describeModels(String models) {
        List<String> parsedModels = splitCsv(models);
        if (parsedModels.isEmpty()) {
            return "0 model(s)";
        }
        return parsedModels.size() + " model(s): " + String.join(", ", parsedModels);
    }

    public static List<String> splitCsv(String value) {
        if (value == null || value.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList();
    }

    private static String maskSecret(String value) {
        if (value == null || value.isEmpty()) {
            return "<empty>";
        }
        int visible = Math.min(4, value.length());
        return value.substring(0, visible) + "...";
    }

    public record ProviderPreset(String id, String displayName, String url, String defaultModel, String defaultThinkingLevel, List<String> aliases) {
        public ProviderPreset(String id, String displayName, String url, String defaultModel, List<String> aliases) {
            this(id, displayName, url, defaultModel, "auto", aliases);
        }
    }
}
