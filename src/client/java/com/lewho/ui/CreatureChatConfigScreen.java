// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.ui;

import com.lewho.commands.ConfigurationPresets;
import com.lewho.commands.ConfigurationScreenData;
import com.lewho.commands.ConfigurationHandler;
import com.lewho.network.ClientPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class CreatureChatConfigScreen extends VersionedScreen {
    private static final int MAX_PANEL_WIDTH = 560;
    private static final int PANEL_HEIGHT = 254;
    private static final int ROW_HEIGHT = 32;
    private static final int BUTTON_HEIGHT = 20;
    private static final String[] THINKING_LEVELS = {"auto", "minimal", "low", "medium", "high"};

    private final ConfigurationScreenData.OpenData initialData;
    private String selectedProvider;
    private String maskedApiKeys;
    private String selectedThinkingLevel;
    private String selectedLanguageCode;
    private String statusMessage = "";
    private int statusColor = 0xFFA0A0A0;

    private EditBox urlField;
    private EditBox apiKeysField;
    private EditBox modelsField;
    private EditBox timeoutField;
    private EditBox outputTokensField;
    private Button thinkingButton;
    private int panelWidth;
    private int panelX;
    private int panelY;
    private int fieldX;
    private int fieldW;
    private int rowY;

    public CreatureChatConfigScreen(ConfigurationScreenData.OpenData initialData) {
        super(Component.literal("CreatureChat Setup"));
        this.initialData = initialData;
        this.selectedProvider = initialData.provider().isEmpty() ? "custom" : initialData.provider();
        this.maskedApiKeys = initialData.maskedApiKeys();
        this.selectedThinkingLevel = normalizeThinkingLevel(initialData.thinkingLevel());
        this.selectedLanguageCode = initialData.generationLanguage() != null ? initialData.generationLanguage() : "";
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_PANEL_WIDTH, this.width - 8);
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - PANEL_HEIGHT) / 2;
        int providerX = panelX + 10;
        int providerW = Math.min(122, panelWidth / 3);
        fieldX = providerX + providerW + 12;
        fieldW = panelX + panelWidth - 12 - fieldX;
        rowY = panelY + 29;
        int secondColumn = fieldX + fieldW / 2 + 2;

        int idx = 0;
        for (ConfigurationPresets.ProviderPreset preset : ConfigurationPresets.presets()) {
            int y = rowY + 12 + idx * 23;
            addRenderableWidget(Button.builder(
                            Component.literal(preset.displayName()),
                            button -> applyPreset(preset))
                    .bounds(providerX, y, providerW, BUTTON_HEIGHT)
                    .build());
            idx++;
        }

        String draftUrl = urlField == null ? initialData.url() : urlField.getValue();
        String draftApiKeys = apiKeysField == null ? "" : apiKeysField.getValue();
        String draftModels = modelsField == null ? initialData.models().replace('\n', ',') : modelsField.getValue();
        String draftTimeout = timeoutField == null ? Integer.toString(initialData.timeout()) : timeoutField.getValue();
        String draftOutputTokens = outputTokensField == null
                ? Integer.toString(initialData.maxOutputTokens()) : outputTokensField.getValue();

        urlField = new EditBox(this.font, fieldX, rowY + 12, fieldW, 20, Component.literal("Endpoint"));
        urlField.setMaxLength(32767);
        urlField.setValue(draftUrl);
        addRenderableWidget(urlField);

        apiKeysField = new EditBox(this.font, fieldX, rowY + ROW_HEIGHT + 12, fieldW, 20, Component.literal("API keys"));
        apiKeysField.setMaxLength(32767);
        apiKeysField.setValue(draftApiKeys);
        addRenderableWidget(apiKeysField);

        modelsField = new EditBox(this.font, fieldX, rowY + ROW_HEIGHT * 2 + 12, fieldW, 20, Component.literal("Models"));
        modelsField.setMaxLength(32767);
        modelsField.setValue(draftModels);
        addRenderableWidget(modelsField);

        timeoutField = new EditBox(this.font, fieldX, rowY + ROW_HEIGHT * 3 + 12, fieldW / 2 - 3, 20, Component.literal("Timeout"));
        timeoutField.setMaxLength(5);
        timeoutField.setValue(draftTimeout);
        addRenderableWidget(timeoutField);

        outputTokensField = new EditBox(this.font, secondColumn, rowY + ROW_HEIGHT * 3 + 12, fieldW / 2 - 2, 20, Component.literal("Output tokens"));
        outputTokensField.setMaxLength(5);
        outputTokensField.setValue(draftOutputTokens);
        addRenderableWidget(outputTokensField);

        thinkingButton = addRenderableWidget(Button.builder(thinkingLabel(), button -> cycleThinkingLevel())
                .bounds(fieldX, rowY + ROW_HEIGHT * 4 + 12, fieldW / 2 - 3, BUTTON_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(languageLabel(), button ->
                this.minecraft.setScreen(new LanguageSelectorScreen(this, selectedLanguageCode, code -> selectedLanguageCode = code)))
                .bounds(secondColumn, rowY + ROW_HEIGHT * 4 + 12, fieldW / 2 - 2, BUTTON_HEIGHT)
                .build());

        int buttonY = panelY + PANEL_HEIGHT - 29;
        int buttonW = (fieldW - 12) / 3;
        addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveConfig())
                .bounds(fieldX, buttonY, buttonW, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Test"), button -> testConfig())
                .bounds(fieldX + buttonW + 6, buttonY, buttonW, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(fieldX + fieldW - buttonW, buttonY, buttonW, BUTTON_HEIGHT)
                .build());

        setFocused(urlField);
    }

    private void applyPreset(ConfigurationPresets.ProviderPreset preset) {
        boolean isSwitching = !preset.id().equals(this.selectedProvider);
        this.selectedProvider = preset.id();
        urlField.setValue(preset.url());
        if (isSwitching || modelsField.getValue().isBlank()) {
            modelsField.setValue(preset.defaultModel());
        }
        if (parsePositiveInt(outputTokensField.getValue(), 0) < ConfigurationHandler.Config.DEFAULT_MAX_OUTPUT_TOKENS) {
            outputTokensField.setValue(Integer.toString(ConfigurationHandler.Config.DEFAULT_MAX_OUTPUT_TOKENS));
        }
        selectedThinkingLevel = normalizeThinkingLevel(preset.defaultThinkingLevel());
        updateThinkingButtonLabel();
        statusMessage = "Preset selected: " + preset.displayName();
        statusColor = 0xFFF0E68C;
    }

    private void saveConfig() {
        ConfigurationScreenData.SaveData data = collectDraft();
        if (data == null) {
            return;
        }
        ClientPackets.sendConfigSave(data);
        updateLocalStatus(true, "Saving configuration...");
    }

    private void testConfig() {
        ConfigurationScreenData.SaveData data = collectDraft();
        if (data == null) {
            return;
        }
        ClientPackets.sendConfigTest(data);
        updateLocalStatus(true, "Testing configuration draft...");
    }

    private ConfigurationScreenData.SaveData collectDraft() {
        int timeout;
        int outputTokens;
        try {
            timeout = parsePositiveInt(timeoutField.getValue(), -1);
            if (timeout < 1) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            updateLocalStatus(false, "Timeout must be a number.");
            return null;
        }
        try {
            outputTokens = parsePositiveInt(outputTokensField.getValue(), -1);
            if (outputTokens < ConfigurationHandler.Config.MIN_MAX_OUTPUT_TOKENS) {
                updateLocalStatus(false, "Output tokens must be at least " + ConfigurationHandler.Config.MIN_MAX_OUTPUT_TOKENS + ".");
                return null;
            }
        } catch (NumberFormatException e) {
            updateLocalStatus(false, "Output tokens must be a number.");
            return null;
        }

        return new ConfigurationScreenData.SaveData(
                selectedProvider,
                urlField.getValue(),
                apiKeysField.getValue(),
                modelsField.getValue(),
                timeout,
                outputTokens,
                selectedThinkingLevel,
                selectedLanguageCode
        );
    }

    private void cycleThinkingLevel() {
        int currentIndex = 0;
        for (int i = 0; i < THINKING_LEVELS.length; i++) {
            if (THINKING_LEVELS[i].equals(selectedThinkingLevel)) {
                currentIndex = i;
                break;
            }
        }
        selectedThinkingLevel = THINKING_LEVELS[(currentIndex + 1) % THINKING_LEVELS.length];
        updateThinkingButtonLabel();
    }

    private int parsePositiveInt(String value, int fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return Integer.parseInt(value.trim());
    }

    private void updateThinkingButtonLabel() {
        if (thinkingButton != null) {
            thinkingButton.setMessage(thinkingLabel());
        }
    }

    private Component thinkingLabel() {
        return Component.literal("Thinking: " + selectedThinkingLevel);
    }

    private Component languageLabel() {
        if (selectedLanguageCode == null || selectedLanguageCode.isEmpty()) {
            return Component.literal("Language: auto (client)");
        }
        return Component.literal("Language: " + selectedLanguageCode);
    }

    private String normalizeThinkingLevel(String value) {
        if (value == null) {
            return "auto";
        }
        String normalized = value.trim().toLowerCase();
        for (String level : THINKING_LEVELS) {
            if (level.equals(normalized)) {
                return level;
            }
        }
        return "auto";
    }

    public void updateServerStatus(boolean success, String message, String maskedApiKeys) {
        this.maskedApiKeys = maskedApiKeys;
        updateLocalStatus(success, message);
        if (success && message.toLowerCase().contains("saved")) {
            apiKeysField.setValue("");
        }
    }

    private void updateLocalStatus(boolean success, String message) {
        this.statusMessage = message;
        this.statusColor = success ? 0xFF55FF55 : 0xFFFF5555;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderModalBackground(graphics);

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + PANEL_HEIGHT, 0xE0101010);
        graphics.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + PANEL_HEIGHT - 1, 0xE0202020);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, panelY + 9, 0xFFFFFFFF);

        graphics.drawString(this.font, Component.literal("Provider").withStyle(ChatFormatting.GRAY), panelX + 10, rowY, 0xFFA0A0A0);
        graphics.drawString(this.font, Component.literal("Endpoint"), fieldX, rowY, 0xFFFFFFFF);
        graphics.drawString(this.font, Component.literal("API keys"), fieldX, rowY + ROW_HEIGHT, 0xFFFFFFFF);
        String keysHint = maskedApiKeys + " | blank keeps saved keys";
        graphics.drawString(this.font, this.font.plainSubstrByWidth(keysHint, fieldW - 49), fieldX + 49, rowY + ROW_HEIGHT, 0xFFA0A0A0);
        graphics.drawString(this.font, Component.literal("Models"), fieldX, rowY + ROW_HEIGHT * 2, 0xFFFFFFFF);
        graphics.drawString(this.font, Component.literal("Timeout"), fieldX, rowY + ROW_HEIGHT * 3, 0xFFFFFFFF);
        int secondColumn = fieldX + fieldW / 2 + 2;
        graphics.drawString(this.font, Component.literal("Output tokens"), secondColumn, rowY + ROW_HEIGHT * 3, 0xFFFFFFFF);
        graphics.drawString(this.font, Component.literal("Gemini thinking"), fieldX, rowY + ROW_HEIGHT * 4, 0xFFFFFFFF);
        graphics.drawString(this.font, Component.literal("Generation language"), secondColumn, rowY + ROW_HEIGHT * 4, 0xFFFFFFFF);

        if (!statusMessage.isEmpty()) {
            graphics.drawString(this.font, this.font.plainSubstrByWidth(statusMessage, fieldW), fieldX, panelY + PANEL_HEIGHT - 49, statusColor);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
