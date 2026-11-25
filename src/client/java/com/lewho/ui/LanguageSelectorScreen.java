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

import java.util.List;
import java.util.function.Consumer;

public class LanguageSelectorScreen extends VersionedScreen {

    private static final int ITEM_HEIGHT = 20;

    private final Screen parent;
    private final Consumer<String> onSelect;
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

        list = new LanguageList(this.minecraft, this.width, listTop, listBottom, ITEM_HEIGHT, currentCode);
        addRenderableWidget(list);

        addRenderableWidget(Button.builder(Component.literal("Select"), button -> {
            LanguageEntry selected = list.getSelected();
            if (selected != null) {
                onSelect.accept(selected.code);
            }
            onClose();
        }).bounds(this.width / 2 - 92, this.height - 32, 86, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> onClose())
                .bounds(this.width / 2 + 6, this.height - 32, 86, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderModalBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.literal("code / display name").withStyle(net.minecraft.ChatFormatting.GRAY),
                this.width / 2, 28, 0xFFA0A0A0);
    }

    public Screen getParentScreen() {
        return parent;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }


    static class LanguageList extends LanguageListWidget<LanguageEntry> {

        LanguageList(Minecraft mc, int width, int top, int bottom, int itemHeight, String currentCode) {
            super(mc, width, top, bottom, itemHeight);

            LanguageEntry autoEntry = new LanguageEntry("", "Auto (client locale)", this);
            addEntry(autoEntry);
            if (currentCode.isEmpty()) {
                setSelected(autoEntry);
            }

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
            int color = isHovered ? 0xFFFFFFFF : 0xFFD0D0D0;
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
