// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.chat;

import com.lewho.commands.ConfigurationHandler;
import com.lewho.network.ServerPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/** Shared gameplay policy behind the version-specific living-entity injections. */
public final class LivingEntityChatHooks {
    private LivingEntityChatHooks() {
    }

    public static boolean preventsFriendlyAttack(LivingEntity entity, LivingEntity target) {
        if (!(entity.level() instanceof ServerLevel) || !(target instanceof ServerPlayer player)) {
            return false;
        }
        EntityChatData data = ChatDataManager.getServerInstance().getOrCreateChatData(entity.getStringUUID());
        return isFriendlyTo(data, player.getStringUUID(), player.getDisplayName().getString());
    }

    public static boolean isFriendlyTo(EntityChatData data, String playerId, String playerName) {
        return data.getPlayerData(playerId, playerName).friendship > 0;
    }

    /** Every applied hit is remembered, even when its automatic reply is suppressed. */
    public static boolean recordDamageReaction(EntityChatData data, String playerId, String playerName,
                                               ConfigurationHandler.Config config) {
        PlayerData playerData = recordDamageEvent(data, playerId, playerName);
        return !data.characterSheet.isEmpty()
                && ChatDataManager.getServerInstance().handleDamageReaction(data, playerData, playerName, config);
    }

    private static PlayerData recordDamageEvent(EntityChatData data, String playerId, String playerName) {
        PlayerData playerData = data.getPlayerData(playerId, playerName);
        playerData.lastDamageFriendship = playerData.friendship;
        playerData.wordsmithDamaged = true;
        SocialEventRecorder.record(data, playerId, playerName, SocialEventType.DAMAGE_DEALT,
                "Player attacked this entity.");
        return playerData;
    }

    public static void onDamage(LivingEntity entity, DamageSource source, boolean applied) {
        if (!applied || !(entity.level() instanceof ServerLevel) || !(entity instanceof Mob mob)
                || entity.isDeadOrDying() || !(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        EntityChatData data = ChatDataManager.getServerInstance().getOrCreateChatData(entity.getStringUUID());
        String playerName = player.getDisplayName().getString();
        PlayerData playerData = recordDamageEvent(data, player.getStringUUID(), playerName);
        if (data.characterSheet.isEmpty()) {
            return;
        }
        ConfigurationHandler.Config config = new ConfigurationHandler(player.getServer()).loadConfig();
        ChatDataManager manager = ChatDataManager.getServerInstance();
        manager.dispatchRequest(data, player,
                () -> manager.handleDamageReaction(data, playerData, playerName, config)
                        && manager.handleAutoResponse(data, player, true, config), request -> {
                    ItemStack weapon = player.getMainHandItem();
                    String weaponName = weapon.isEmpty() ? "with fists" : "with " + weapon.getItem().toString();
                    String directness = source.getDirectEntity() == player ? "directly" : "indirectly";
                    String message = "<" + playerName + " attacked you " + directness + " " + weaponName + ">";
                    String suppressedSummary = playerData.consumeSuppressedDamageReactionSummary();
                    if (!suppressedSummary.isEmpty()) {
                        message += " " + suppressedSummary;
                    }
                    ServerPackets.generate_chat(request, "N/A", data, player, mob, message, true, config);
                });
    }
}
