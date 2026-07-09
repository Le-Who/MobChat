// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.chat;

import net.minecraft.server.level.ServerPlayer;

public final class SocialEventRecorder {
    private SocialEventRecorder() {
    }

    public static PlayerData record(EntityChatData chatData, ServerPlayer player, SocialEventType type, String summary) {
        if (player == null) {
            return record(chatData, "", "", type, summary);
        }
        return record(chatData, player.getStringUUID(), player.getDisplayName().getString(), type, summary);
    }

    public static PlayerData record(EntityChatData chatData, String playerKey, String legacyPlayerName, SocialEventType type, String summary) {
        if (chatData == null) {
            return new PlayerData();
        }
        PlayerData playerData = chatData.getPlayerData(playerKey, legacyPlayerName);
        playerData.recordSocialEvent(type, summary);
        return playerData;
    }
}
