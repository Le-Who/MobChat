// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.chat;

import java.util.HashMap;
import java.util.Map;

public class EntityChatDataLight {
    public String entityId;
    public String currentMessage;
    public int currentLineNumber;
    public ChatDataManager.ChatStatus status;
    public ChatDataManager.ChatSender sender;
    public Map<String, PlayerData> players;

    public EntityChatDataLight(EntityChatData fullData, String playerId, String playerName) {
        this.entityId = fullData.entityId;
        this.currentMessage = fullData.currentMessage;
        this.currentLineNumber = fullData.currentLineNumber;
        this.status = fullData.status;
        this.sender = fullData.sender;

        this.players = new HashMap<>();
        this.players.put(playerId, fullData.getPlayerDataForDisplay(playerId, playerName));
    }
}
