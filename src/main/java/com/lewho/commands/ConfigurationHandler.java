// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lewho.network.ServerPackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code ConfigurationHandler} class loads and saves configuration settings for this mod. It first
 * checks for a config file in the world save folder, and if not found, falls back to the root folder.
 * This allows for global/default settings, or optional server-specific settings.
 */

public class ConfigurationHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path serverConfigPath;
    private final Path defaultConfigPath;

    public ConfigurationHandler(MinecraftServer server) {
        this.serverConfigPath = server.getWorldPath(LevelResource.ROOT).resolve("creaturechat.json");
        this.defaultConfigPath = Paths.get(".", "creaturechat.json"); // Assumes the default location is the server root or a similar logical default
    }

    public Config loadConfig() {
        Config config = loadConfigFromFile(serverConfigPath);
        if (config == null) {
            config = loadConfigFromFile(defaultConfigPath);
        }
        return config != null ? config : new Config(); // Return new config if both are null
    }

    public boolean saveConfig(Config config, boolean useServerConfig) {
        Path path = useServerConfig ? serverConfigPath : defaultConfigPath;
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(config, writer);
            return true;
        } catch (IOException e) {
            String errorMessage = "Error saving `creaturechat.json`. CreatureChat config was not saved. " + e.getMessage();
            LOGGER.error(errorMessage, e);
            ServerPackets.sendErrorToAllOps(ServerPackets.serverInstance, errorMessage);
            return false;
        }
    }

    private Config loadConfigFromFile(Path filePath) {
        try (Reader reader = Files.newBufferedReader(filePath)) {
            Config config = gson.fromJson(reader, Config.class);
            if (config != null) {
                Path parent = filePath.getParent();
                config.setUsageDataPath((parent == null ? Paths.get(".") : parent).resolve("creaturechat_usage.json"));
            }
            return config;
        } catch (IOException e) {
            return null; // File does not exist or other IO errors
        }
    }

    public static class Config {
        public static final int DEFAULT_MAX_OUTPUT_TOKENS = 1024;
        public static final int MIN_MAX_OUTPUT_TOKENS = 64;
        public static final Path DEFAULT_USAGE_DATA_PATH = Paths.get(".", "creaturechat_usage.json");

        private String apiKey = "";
        private String url = "https://api.openai.com/v1/chat/completions";
        private String model = "gpt-3.5-turbo";
        private String thinkingLevel = "auto";
        private int maxContextTokens = 16385;
        private int maxOutputTokens = DEFAULT_MAX_OUTPUT_TOKENS;
        private double percentOfContext = 0.75;
        private int timeout = 30;
        private boolean chatBubbles = true;
        private boolean sendToChat = true;
        private List<String> whitelist = new ArrayList<>();
        private List<String> blacklist = new ArrayList<>();
        private String story = "";
        private int maxPlayerAutoResponses = 10;
        private int playerAutoCooldownSeconds = 3;
        private int maxEntityAutoResponses = 3;
        private int entityAutoCooldownSeconds = 3;
        private int damageReactionCooldownSeconds = 25;
        private boolean geminiUsageLimitsEnabled = true;
        private int geminiRequestsPerMinute = 14;
        private int geminiRequestsPerDay = 450;
        private String geminiUsageLimitScope = "per_key";
        private boolean proximityChatEnabled = true;
        private int proximityChatRadius = 12;
        private int maxProximityResponsesPerMessage = 1;
        private int maxPlayerAmbientResponses = 1;
        private int playerAmbientCooldownSeconds = 45;
        private int maxEntityAmbientResponses = 1;
        private int entityAmbientCooldownSeconds = 90;
        private boolean mobToMobChatEnabled = true;
        private int mobToMobChatRadius = 10;
        private int maxMobToMobResponsesPerMessage = 1;

        /** Locale code override for LLM generation, e.g. "ru_ru". Empty string = auto (client locale). */
        private String generationLanguage = "";

        private transient int currentKeyIndex = 0;
        private transient int currentModelIndex = 0;
        private transient Path usageDataPath = DEFAULT_USAGE_DATA_PATH;

        private String[] parseCsv(String value) {
            if (value == null || value.trim().isEmpty()) {
                return new String[0];
            }
            String[] parts = value.split(",");
            List<String> validValues = new ArrayList<>();
            for (String p : parts) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    validValues.add(trimmed);
                }
            }
            return validValues.toArray(new String[0]);
        }

        private String[] parseApiKeys() {
            return parseCsv(apiKey);
        }

        private String[] parseModels() {
            return parseCsv(model);
        }

        public String getActiveApiKey() {
            String[] keys = parseApiKeys();
            if (keys.length == 0) {
                return "";
            }
            if (currentKeyIndex < 0 || currentKeyIndex >= keys.length) {
                currentKeyIndex = 0;
            }
            return keys[currentKeyIndex];
        }

        public void rotateApiKey() {
            String[] keys = parseApiKeys();
            if (keys.length > 0) {
                currentKeyIndex = (currentKeyIndex + 1) % keys.length;
            }
        }

        public int getApiKeyCount() {
            return parseApiKeys().length;
        }

        public String getActiveModel() {
            String[] models = parseModels();
            if (models.length == 0) {
                return "";
            }
            if (currentModelIndex < 0 || currentModelIndex >= models.length) {
                currentModelIndex = 0;
            }
            return models[currentModelIndex];
        }

        public void rotateModel() {
            String[] models = parseModels();
            if (models.length > 0) {
                currentModelIndex = (currentModelIndex + 1) % models.length;
            }
        }

        public int getModelCount() {
            return parseModels().length;
        }

        // Getters and setters for existing fields
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
            this.currentKeyIndex = 0;
            String activeKey = getActiveApiKey();
            if (activeKey.startsWith("sk-") && !activeKey.startsWith("sk-or-") && isOpenAiUrl(url)) {
                // Update URL if an OpenAI API key is detected
                setUrl("https://api.openai.com/v1/chat/completions");
            }
        }

        private boolean isOpenAiUrl(String url) {
            return url == null || url.isEmpty() || url.contains("api.openai.com");
        }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getModel() { return model; }
        public void setModel(String model) {
            this.model = model;
            this.currentModelIndex = 0;
        }

        public String getThinkingLevel() {
            return normalizeThinkingLevel(thinkingLevel);
        }

        public void setThinkingLevel(String thinkingLevel) {
            this.thinkingLevel = normalizeThinkingLevel(thinkingLevel);
        }

        private String normalizeThinkingLevel(String value) {
            if (value == null || value.trim().isEmpty()) {
                return "auto";
            }
            String normalized = value.trim().toLowerCase();
            return switch (normalized) {
                case "minimal", "low", "medium", "high" -> normalized;
                default -> "auto";
            };
        }

        private String normalizeGeminiUsageLimitScope(String value) {
            if (value == null || value.trim().isEmpty()) {
                return "per_key";
            }
            String normalized = value.trim().toLowerCase();
            return switch (normalized) {
                case "shared", "global", "project" -> "shared";
                default -> "per_key";
            };
        }

        public int getTimeout() { return timeout; }
        public void setTimeout(int timeout) { this.timeout = timeout; }

        public int getMaxContextTokens() { return maxContextTokens; }
        public void setMaxContextTokens(int maxContextTokens) { this.maxContextTokens = maxContextTokens; }

        public int getMaxOutputTokens() { return maxOutputTokens; }
        public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = Math.max(MIN_MAX_OUTPUT_TOKENS, maxOutputTokens); }

        public double getPercentOfContext() { return percentOfContext; }
        public void setPercentOfContext(double percentOfContext) { this.percentOfContext = percentOfContext; }

        public List<String> getWhitelist() { return whitelist; }
        public void setWhitelist(List<String> whitelist) { this.whitelist = whitelist; }

        public List<String> getBlacklist() { return blacklist; }
        public void setBlacklist(List<String> blacklist) { this.blacklist = blacklist; }

        public String getStory() { return story; }
        public void setStory(String story) { this.story = story; }

        // Add getter and setter
        public boolean getChatBubbles() { return chatBubbles; }
        public void setChatBubbles(boolean chatBubblesEnabled) { this.chatBubbles = chatBubblesEnabled; }

        public boolean getSendToChat() { return sendToChat; }
        public void setSendToChat(boolean sendToChatEnabled) { this.sendToChat = sendToChatEnabled; }

        public int getMaxPlayerAutoResponses() { return maxPlayerAutoResponses; }
        public void setMaxPlayerAutoResponses(int maxPlayerAutoResponses) { this.maxPlayerAutoResponses = maxPlayerAutoResponses; }

        public int getPlayerAutoCooldownSeconds() { return playerAutoCooldownSeconds; }
        public void setPlayerAutoCooldownSeconds(int playerAutoCooldownSeconds) { this.playerAutoCooldownSeconds = playerAutoCooldownSeconds; }

        public int getMaxEntityAutoResponses() { return maxEntityAutoResponses; }
        public void setMaxEntityAutoResponses(int maxEntityAutoResponses) { this.maxEntityAutoResponses = maxEntityAutoResponses; }

        public int getEntityAutoCooldownSeconds() { return entityAutoCooldownSeconds; }
        public void setEntityAutoCooldownSeconds(int entityAutoCooldownSeconds) { this.entityAutoCooldownSeconds = entityAutoCooldownSeconds; }

        public int getDamageReactionCooldownSeconds() { return damageReactionCooldownSeconds; }
        public void setDamageReactionCooldownSeconds(int damageReactionCooldownSeconds) { this.damageReactionCooldownSeconds = Math.max(0, damageReactionCooldownSeconds); }

        public boolean getGeminiUsageLimitsEnabled() { return geminiUsageLimitsEnabled; }
        public void setGeminiUsageLimitsEnabled(boolean geminiUsageLimitsEnabled) { this.geminiUsageLimitsEnabled = geminiUsageLimitsEnabled; }

        public int getGeminiRequestsPerMinute() { return geminiRequestsPerMinute; }
        public void setGeminiRequestsPerMinute(int geminiRequestsPerMinute) { this.geminiRequestsPerMinute = Math.max(0, geminiRequestsPerMinute); }

        public int getGeminiRequestsPerDay() { return geminiRequestsPerDay; }
        public void setGeminiRequestsPerDay(int geminiRequestsPerDay) { this.geminiRequestsPerDay = Math.max(0, geminiRequestsPerDay); }

        public String getGeminiUsageLimitScope() { return normalizeGeminiUsageLimitScope(geminiUsageLimitScope); }
        public void setGeminiUsageLimitScope(String geminiUsageLimitScope) { this.geminiUsageLimitScope = normalizeGeminiUsageLimitScope(geminiUsageLimitScope); }

        public Path getUsageDataPath() { return usageDataPath == null ? DEFAULT_USAGE_DATA_PATH : usageDataPath; }
        public void setUsageDataPath(Path usageDataPath) { this.usageDataPath = usageDataPath == null ? DEFAULT_USAGE_DATA_PATH : usageDataPath; }

        public boolean getProximityChatEnabled() { return proximityChatEnabled; }
        public void setProximityChatEnabled(boolean proximityChatEnabled) { this.proximityChatEnabled = proximityChatEnabled; }

        public int getProximityChatRadius() { return proximityChatRadius; }
        public void setProximityChatRadius(int proximityChatRadius) { this.proximityChatRadius = proximityChatRadius; }

        public int getMaxProximityResponsesPerMessage() { return maxProximityResponsesPerMessage; }
        public void setMaxProximityResponsesPerMessage(int maxProximityResponsesPerMessage) { this.maxProximityResponsesPerMessage = maxProximityResponsesPerMessage; }

        public int getMaxPlayerAmbientResponses() { return maxPlayerAmbientResponses; }
        public void setMaxPlayerAmbientResponses(int maxPlayerAmbientResponses) { this.maxPlayerAmbientResponses = maxPlayerAmbientResponses; }

        public int getPlayerAmbientCooldownSeconds() { return playerAmbientCooldownSeconds; }
        public void setPlayerAmbientCooldownSeconds(int playerAmbientCooldownSeconds) { this.playerAmbientCooldownSeconds = playerAmbientCooldownSeconds; }

        public int getMaxEntityAmbientResponses() { return maxEntityAmbientResponses; }
        public void setMaxEntityAmbientResponses(int maxEntityAmbientResponses) { this.maxEntityAmbientResponses = maxEntityAmbientResponses; }

        public int getEntityAmbientCooldownSeconds() { return entityAmbientCooldownSeconds; }
        public void setEntityAmbientCooldownSeconds(int entityAmbientCooldownSeconds) { this.entityAmbientCooldownSeconds = entityAmbientCooldownSeconds; }

        public boolean getMobToMobChatEnabled() { return mobToMobChatEnabled; }
        public void setMobToMobChatEnabled(boolean mobToMobChatEnabled) { this.mobToMobChatEnabled = mobToMobChatEnabled; }

        public int getMobToMobChatRadius() { return mobToMobChatRadius; }
        public void setMobToMobChatRadius(int mobToMobChatRadius) { this.mobToMobChatRadius = mobToMobChatRadius; }

        public int getMaxMobToMobResponsesPerMessage() { return maxMobToMobResponsesPerMessage; }
        public void setMaxMobToMobResponsesPerMessage(int maxMobToMobResponsesPerMessage) { this.maxMobToMobResponsesPerMessage = maxMobToMobResponsesPerMessage; }

        /** Returns the locale-code override, e.g. {@code "ru_ru"}, or {@code ""} for auto. */
        public String getGenerationLanguage() { return generationLanguage == null ? "" : generationLanguage; }

        /**
         * Sets the locale-code override. Passing {@code null}, blank, or {@code "auto"}
         * resets to automatic (client locale).
         */
        public void setGenerationLanguage(String code) {
            if (code == null || code.isBlank() || code.equalsIgnoreCase("auto")) {
                this.generationLanguage = "";
            } else {
                this.generationLanguage = code.trim().toLowerCase();
            }
        }
    }
}
