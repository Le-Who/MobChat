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

    public static void onItemGiven(Mob mob, Player player, InteractionHand hand, InteractionResult result) {
        if (!(mob.level() instanceof ServerLevel) || !(player instanceof ServerPlayer serverPlayer)
                || hand != InteractionHand.MAIN_HAND || mob instanceof Villager || mob instanceof TamableAnimal) {
            return;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (isBucket(stack.getItem())) {
            return;
        }

        EntityChatData data = ChatDataManager.getServerInstance().getOrCreateChatData(mob.getStringUUID());
        String playerName = player.getDisplayName().getString();
        PlayerData playerData = recordItemInteraction(data, player.getStringUUID(), playerName,
                !stack.isEmpty() && result.consumesAction());
        if (!stack.isEmpty()) {
            if (!data.characterSheet.isEmpty()) {
                String verb = result.consumesAction() ? " gives " : " shows ";
                String message = "<" + playerName + verb + "you " + stack.getCount() + " "
                        + stack.getItem().getName(stack).getString() + ">";
                ServerPackets.generate_chat("N/A", data, serverPlayer, mob, message, true);
            }
        } else if (playerData.friendship == 3) {
            player.startRiding(mob, true);
        }
    }

    private static boolean isBucket(Item item) {
        return item == Items.BUCKET || item == Items.WATER_BUCKET || item == Items.LAVA_BUCKET
                || item == Items.POWDER_SNOW_BUCKET || item == Items.MILK_BUCKET
                || item == Items.PUFFERFISH_BUCKET || item == Items.SALMON_BUCKET || item == Items.COD_BUCKET
                || item == Items.TROPICAL_FISH_BUCKET || item == Items.AXOLOTL_BUCKET || item == Items.TADPOLE_BUCKET;
    }
}
