// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.inventory;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import com.lewho.chat.PlayerData;
import com.lewho.utils.TextureLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Shared logic for mob inventory screens.
 */
public abstract class MobInventoryScreenBase extends AbstractContainerScreen<MobInventoryMenu> {
    protected static final TextureLoader textures = new TextureLoader();
    protected static final ResourceLocation FRIEND_TEXTURE = textures.GetUI("inventory");
    protected static final ResourceLocation ENEMY_TEXTURE = textures.GetUI("inventory-enemy");
    protected static final int INF = 1024;
    protected float xMouse;
    protected float yMouse;

    protected MobInventoryScreenBase(MobInventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY += 2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.xMouse = (float) mouseX;
        this.yMouse = (float) mouseY;
        super.render(guiGraphics, mouseX, mouseY, delta);
        if (this.minecraft.player != null) {
            for (Slot slot : this.menu.slots) {
                if (!slot.mayPickup(this.minecraft.player)) {
                    int x = this.leftPos + slot.x;
                    int y = this.topPos + slot.y;
                    guiGraphics.fill(x, y, x + 16, y + 16, 0x90000000);
                }
            }
        }
        this.renderJournalTooltip(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int k = (this.width - this.imageWidth) / 2;
        int l = (this.height - this.imageHeight) / 2;
        Mob mob = this.menu.getMob();
        ResourceLocation background = FRIEND_TEXTURE;
        if (mob != null && this.minecraft.player != null) {
            int friendship = ChatDataManager.getClientInstance()
                    .getOrCreateChatData(mob.getStringUUID())
                    .getPlayerData(this.minecraft.player.getStringUUID(), this.minecraft.player.getDisplayName().getString())
                    .friendship;
            if (friendship <= 0) {
                background = ENEMY_TEXTURE;
            }
        }
        this.blitBackground(guiGraphics, background, k, l);
        if (mob != null) {
            int boxL = k + 13, boxT = l + 18, boxR = boxL + 52, boxB = boxT + 52;
            int left = boxL + 8, top = boxT + 12, right = boxR - 8, bottom = boxB - 8;
            int w = right - left, h = bottom - top;
            int ROT_PAD = 2;
            float sx = (float)(w - ROT_PAD * 2) / mob.getBbWidth();
            float sy = (float)(h - ROT_PAD * 2) / mob.getBbHeight();
            int scale = (int)Math.floor(Math.min(sx, sy));
            float yOffset = -2f / Math.max(1, scale);
            this.renderMob(guiGraphics, mob, left, top, right, bottom, scale, yOffset);
        }
    }

    protected abstract void blitBackground(GuiGraphics guiGraphics, ResourceLocation background, int x, int y);

    protected abstract void renderMob(GuiGraphics guiGraphics, Mob mob, int left, int top, int right, int bottom, int scale, float yOffset);

    private void renderJournalTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Mob mob = this.menu.getMob();
        if (mob == null || this.minecraft.player == null) {
            return;
        }

        int boxL = this.leftPos + 13;
        int boxT = this.topPos + 18;
        int boxR = boxL + 52;
        int boxB = boxT + 52;
        if (mouseX < boxL || mouseX > boxR || mouseY < boxT || mouseY > boxB) {
            return;
        }

        EntityChatData chatData = ChatDataManager.getClientInstance().getOrCreateChatData(mob.getStringUUID());
        PlayerData playerData = chatData.getPlayerData(this.minecraft.player.getStringUUID(), this.minecraft.player.getDisplayName().getString());
        java.util.List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.literal("Mob Journal"));
        lines.add(Component.literal("Friendship: " + playerData.friendship));
        lines.add(Component.literal("Reputation: " + playerData.socialReputation));
        lines.add(Component.literal("Helpful: " + playerData.helpfulActions + "  Harmful: " + playerData.harmfulActions));
        lines.add(Component.literal("Events: " + playerData.socialEventCount));
        if (playerData.socialSummary != null && !playerData.socialSummary.isEmpty()) {
            lines.add(Component.literal("Note: " + playerData.socialSummary));
        }
        guiGraphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }
}

