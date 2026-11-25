// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.chat;

import com.lewho.network.ServerPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Shared post-interaction policy; inventory/NBT adapters stay in version mixins. */
public final class MobInteractionHooks {
    private MobInteractionHooks() {
    }

    public static PlayerData recordItemInteraction(EntityChatData data, String playerId, String playerName,
                                                    boolean given) {
        if (given && !data.characterSheet.isEmpty()) {
            return SocialEventRecorder.record(data, playerId, playerName, SocialEventType.GIFT_GIVEN,
                    "Player gave an item directly.");
        }
        return data.getPlayerData(playerId, playerName);
    }

    public static boolean handlesInteraction(Mob mob, Player player, InteractionHand hand) {
        return mob.level() instanceof ServerLevel && mob.isAlive() && player instanceof ServerPlayer
                && hand == InteractionHand.MAIN_HAND && !(mob instanceof Villager) && !(mob instanceof TamableAnimal);
    }

    public static void onItemGiven(Mob mob, ServerPlayer player, ItemStack before, ItemStack after,
                                   InteractionResult result) {
        if (isBucket(before.getItem())) {
            return;
        }

        EntityChatData data = ChatDataManager.getServerInstance().getOrCreateChatData(mob.getStringUUID());
        String playerName = player.getDisplayName().getString();
        int given = giftAmount(before, after, result.consumesAction(), player.getAbilities().instabuild);
        PlayerData playerData = recordItemInteraction(data, player.getStringUUID(), playerName,
                given > 0);
        if (!before.isEmpty()) {
            if (!data.characterSheet.isEmpty()) {
                String verb = given > 0 ? " gives " : " shows ";
                int amount = given > 0 ? given : before.getCount();
                String message = "<" + playerName + verb + "you " + amount + " "
                        + before.getItem().getName(before).getString() + ">";
                ServerPackets.generate_chat("N/A", data, player, mob, message, true);
            }
        } else if (playerData.friendship == 3) {
            player.startRiding(mob, true);
        }
    }

    static int giftAmount(ItemStack before, ItemStack after, boolean accepted, boolean infiniteItems) {
        if (before.isEmpty() || !accepted) {
            return 0;
        }
        int remaining = ItemStack.isSameItem(before, after) ? after.getCount() : 0;
        int consumed = Math.max(0, before.getCount() - remaining);
        return consumed == 0 && infiniteItems ? 1 : consumed;
    }

    private static boolean isBucket(Item item) {
        return item == Items.BUCKET || item == Items.WATER_BUCKET || item == Items.LAVA_BUCKET
                || item == Items.POWDER_SNOW_BUCKET || item == Items.MILK_BUCKET
                || item == Items.PUFFERFISH_BUCKET || item == Items.SALMON_BUCKET || item == Items.COD_BUCKET
                || item == Items.TROPICAL_FISH_BUCKET || item == Items.AXOLOTL_BUCKET || item == Items.TADPOLE_BUCKET;
    }
}
