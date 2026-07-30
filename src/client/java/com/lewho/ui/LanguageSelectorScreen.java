// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.ui;

import com.lewho.commands.MinecraftLanguages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A modal sub-screen that shows all official Minecraft locale codes and their
 * in-language display names, letting the admin pick one as the generation language
 * override. "Auto" (empty code) is always the first entry.
 */
public class LanguageSelectorScreen extends Screen {

    private static final int ITEM_HEIGHT = 20;

    private final Screen parent;
    private final Consumer<String> onSelect;  // receives the selected locale code, or "" for auto
    private final String currentCode;

    private LanguageList list;

    public LanguageSelectorScreen(Screen parent, String currentCode, Consumer<String> onSelect) {
        super(Component.literal("Select Generation Language"));
        this.parent = parent;
        this.currentCode = currentCode == null ? "" : currentCode;
        this.onSelect = onSelect;
    }

    @Override
    protected void init() {
        int listTop = 40;
        int listBottom = this.height - 40;

        list = new LanguageList(this.minecraft, this.width, listBottom - listTop, listTop, listBottom, ITEM_HEIGHT, currentCode);
        addRenderableWidget(list);

        addRenderableWidget(Button.builder(Component.literal("Select"), button -> {
            LanguageEntry selected = list.getSelected();
            if (selected != null) {
                onSelect.accept(selected.code);
            }
            this.minecraft.setScreen(parent);
        }).bounds(this.width / 2 - 92, this.height - 32, 86, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button ->
                this.minecraft.setScreen(parent))
                .bounds(this.width / 2 + 6, this.height - 32, 86, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.literal("code / display name").withStyle(net.minecraft.ChatFormatting.GRAY),
                this.width / 2, 28, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- List widget ----

    static class LanguageList extends ObjectSelectionList<LanguageEntry> {

        LanguageList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight, String currentCode) {
            super(mc, width, height, top, bottom, itemHeight);
            setRenderBackground(false);
            setRenderTopAndBottom(false);

            // First entry: Auto
            LanguageEntry autoEntry = new LanguageEntry("", "Auto (client locale)", this);
            addEntry(autoEntry);
            if (currentCode.isEmpty()) {
                setSelected(autoEntry);
            }

            // All known locale codes, already sorted
            List<String> codes = MinecraftLanguages.ALL_CODES;
            for (String code : codes) {
                String display = MinecraftLanguages.displayName(code);
                LanguageEntry entry = new LanguageEntry(code, code + "  —  " + display, this);
                addEntry(entry);
                if (code.equalsIgnoreCase(currentCode)) {
                    setSelected(entry);
                    ensureVisible(entry);
                }
            }
        }
    }

    static class LanguageEntry extends ObjectSelectionList.Entry<LanguageEntry> {

        final String code;
        private final String label;
        private final LanguageList parentList;

        LanguageEntry(String code, String label, LanguageList parentList) {
            this.code = code;
            this.label = label;
            this.parentList = parentList;
        }

        @Override
        public Component getNarration() {
            return Component.literal(label);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left,
                           int width, int height, int mouseX, int mouseY,
                           boolean isHovered, float partialTick) {
            int color = isHovered ? 0xFFFFFF : 0xD0D0D0;
            graphics.drawString(Minecraft.getInstance().font,
                    label, left + 4, top + (height - 9) / 2, color, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            parentList.setSelected(this);
            return true;
        }
    }
}
