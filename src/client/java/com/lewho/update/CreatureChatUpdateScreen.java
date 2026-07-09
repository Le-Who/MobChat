// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class CreatureChatUpdateScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 180;
    private final UpdateCandidate candidate;
    private String status = "";
    private int statusColor = 0xA0A0A0;
    private Button downloadButton;

    public CreatureChatUpdateScreen(UpdateCandidate candidate) {
        super(Component.literal("CreatureChat Update"));
        this.candidate = candidate;
    }

    @Override
    protected void init() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int buttonY = panelY + PANEL_HEIGHT - 34;

        downloadButton = addRenderableWidget(Button.builder(Component.literal("Download"), button -> download())
                .bounds(panelX + 18, buttonY, 96, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Changelog"), button -> openChangelog())
                .bounds(panelX + 122, buttonY, 96, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("Later"), button -> onClose())
                .bounds(panelX + PANEL_WIDTH - 114, buttonY, 96, 20)
                .build());
    }

    private void download() {
        downloadButton.active = false;
        status = "Downloading CreatureChat " + candidate.version() + "...";
        statusColor = 0xF0E68C;
        ClientUpdateManager.downloadAndArm(candidate, (success, message) -> {
            status = message;
            statusColor = success ? 0x55FF55 : 0xFF5555;
            if (!success && downloadButton != null) {
                downloadButton.active = true;
            }
        });
    }

    private void openChangelog() {
        if (candidate.releaseUrl() != null && !candidate.releaseUrl().isBlank()) {
            Util.getPlatform().openUri(candidate.releaseUrl());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xE0101010);
        graphics.fill(panelX + 1, panelY + 1, panelX + PANEL_WIDTH - 1, panelY + PANEL_HEIGHT - 1, 0xE0202020);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, panelY + 14, 0xFFFFFF);
        graphics.drawString(this.font, Component.literal("New version available: " + candidate.version()), panelX + 18, panelY + 46, 0xFFFFFF);
        graphics.drawString(this.font, Component.literal(candidate.assetName()), panelX + 18, panelY + 66, 0xA0A0A0);
        graphics.drawString(this.font, Component.literal("The update will be installed after Minecraft exits."), panelX + 18, panelY + 92, 0xA0A0A0);
        graphics.drawString(this.font, Component.literal("No files need to be replaced manually."), panelX + 18, panelY + 110, 0xA0A0A0);
        if (!status.isEmpty()) {
            graphics.drawString(this.font, Component.literal(status), panelX + 18, panelY + 134, statusColor);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
