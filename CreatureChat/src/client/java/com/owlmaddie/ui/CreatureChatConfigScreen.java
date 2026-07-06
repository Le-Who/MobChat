// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.ui;

import com.owlmaddie.commands.ConfigurationPresets;
import com.owlmaddie.commands.ConfigurationScreenData;
import com.owlmaddie.network.ClientPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Client-side OP configuration screen. The server remains authoritative.
 */
public class CreatureChatConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 560;
    private static final int PANEL_HEIGHT = 326;
    private static final int ROW_HEIGHT = 34;
    private static final int BUTTON_HEIGHT = 20;
    private static final String[] THINKING_LEVELS = {"auto", "low", "medium", "high"};

    private final ConfigurationScreenData.OpenData initialData;
    private String selectedProvider;
    private String maskedApiKeys;
    private String selectedThinkingLevel;
    private String statusMessage = "";
    private int statusColor = 0xA0A0A0;

    private EditBox urlField;
    private EditBox apiKeysField;
    private EditBox modelsField;
    private EditBox timeoutField;
    private Button thinkingButton;

    public CreatureChatConfigScreen(ConfigurationScreenData.OpenData initialData) {
        super(Component.literal("CreatureChat Setup"));
        this.initialData = initialData;
        this.selectedProvider = initialData.provider().isEmpty() ? "custom" : initialData.provider();
        this.maskedApiKeys = initialData.maskedApiKeys();
        this.selectedThinkingLevel = normalizeThinkingLevel(initialData.thinkingLevel());
    }

    @Override
    protected void init() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int providerX = panelX + 16;
        int providerY = panelY + 42;
        int providerW = 122;
        int fieldX = panelX + 158;
        int fieldW = PANEL_WIDTH - 178;
        int rowY = panelY + 42;

        int idx = 0;
        for (ConfigurationPresets.ProviderPreset preset : ConfigurationPresets.presets()) {
            int y = providerY + idx * 24;
            addRenderableWidget(Button.builder(
                            Component.literal(preset.displayName()),
                            button -> applyPreset(preset))
                    .bounds(providerX, y, providerW, BUTTON_HEIGHT)
                    .build());
            idx++;
        }

        urlField = new EditBox(this.font, fieldX, rowY + 12, fieldW, 20, Component.literal("Endpoint"));
        urlField.setMaxLength(32767);
        urlField.setValue(initialData.url());
        addRenderableWidget(urlField);

        apiKeysField = new EditBox(this.font, fieldX, rowY + ROW_HEIGHT + 12, fieldW, 20, Component.literal("API keys"));
        apiKeysField.setMaxLength(32767);
        apiKeysField.setValue("");
        addRenderableWidget(apiKeysField);

        modelsField = new EditBox(this.font, fieldX, rowY + ROW_HEIGHT * 2 + 12, fieldW, 20, Component.literal("Models"));
        modelsField.setMaxLength(32767);
        modelsField.setValue(initialData.models().replace('\n', ','));
        addRenderableWidget(modelsField);

        timeoutField = new EditBox(this.font, fieldX, rowY + ROW_HEIGHT * 3 + 12, 80, 20, Component.literal("Timeout"));
        timeoutField.setMaxLength(5);
        timeoutField.setValue(Integer.toString(initialData.timeout()));
        addRenderableWidget(timeoutField);

        thinkingButton = addRenderableWidget(Button.builder(thinkingLabel(), button -> cycleThinkingLevel())
                .bounds(fieldX, rowY + ROW_HEIGHT * 4 + 12, 112, BUTTON_HEIGHT)
                .build());

        int buttonY = panelY + PANEL_HEIGHT - 36;
        addRenderableWidget(Button.builder(Component.literal("Save"), button -> saveConfig())
                .bounds(fieldX, buttonY, 86, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Test"), button -> testConfig())
                .bounds(fieldX + 94, buttonY, 86, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(fieldX + fieldW - 86, buttonY, 86, BUTTON_HEIGHT)
                .build());

        setFocused(urlField);
    }

    private void applyPreset(ConfigurationPresets.ProviderPreset preset) {
        this.selectedProvider = preset.id();
        urlField.setValue(preset.url());
        modelsField.setValue(preset.defaultModel());
        if ("ai-studio".equals(preset.id()) && "auto".equals(selectedThinkingLevel)) {
            selectedThinkingLevel = "medium";
            updateThinkingButtonLabel();
        }
        statusMessage = "Preset selected: " + preset.displayName();
        statusColor = 0xF0E68C;
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
        try {
            timeout = Integer.parseInt(timeoutField.getValue().trim());
        } catch (NumberFormatException e) {
            updateLocalStatus(false, "Timeout must be a number.");
            return null;
        }

        return new ConfigurationScreenData.SaveData(
                selectedProvider,
                urlField.getValue(),
                apiKeysField.getValue(),
                modelsField.getValue(),
                timeout,
                selectedThinkingLevel
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

    private void updateThinkingButtonLabel() {
        if (thinkingButton != null) {
            thinkingButton.setMessage(thinkingLabel());
        }
    }

    private Component thinkingLabel() {
        return Component.literal("Thinking: " + selectedThinkingLevel);
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
        this.statusColor = success ? 0x55FF55 : 0xFF5555;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int fieldX = panelX + 158;
        int rowY = panelY + 42;

        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE0101010);
        graphics.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + PANEL_HEIGHT - 1, 0xE0202020);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, panelY + 12, 0xFFFFFF);

        graphics.drawString(this.font, Component.literal("Provider").withStyle(ChatFormatting.GRAY), panelX + 16, panelY + 30, 0xA0A0A0);
        graphics.drawString(this.font, Component.literal("Endpoint"), fieldX, rowY, 0xFFFFFF);
        graphics.drawString(this.font, Component.literal("API keys"), fieldX, rowY + ROW_HEIGHT, 0xFFFFFF);
        graphics.drawString(this.font, Component.literal(maskedApiKeys + " | leave blank to keep existing keys"), fieldX + 70, rowY + ROW_HEIGHT, 0xA0A0A0);
        graphics.drawString(this.font, Component.literal("Models"), fieldX, rowY + ROW_HEIGHT * 2, 0xFFFFFF);
        graphics.drawString(this.font, Component.literal("Timeout"), fieldX, rowY + ROW_HEIGHT * 3, 0xFFFFFF);
        graphics.drawString(this.font, Component.literal("Thinking (Gemini)"), fieldX, rowY + ROW_HEIGHT * 4, 0xFFFFFF);

        if (!statusMessage.isEmpty()) {
            graphics.drawString(this.font, Component.literal(statusMessage), fieldX, panelY + PANEL_HEIGHT - 56, statusColor);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
