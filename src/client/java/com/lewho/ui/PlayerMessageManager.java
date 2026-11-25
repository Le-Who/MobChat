// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.ui;

import com.lewho.chat.ChatDataManager;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerMessageManager {
    private static final ConcurrentHashMap<UUID, PlayerMessage> messages = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> openChatUIs = new ConcurrentHashMap<>();

    public static void clear() {
        messages.clear();
        openChatUIs.clear();
    }

    public static void addMessage(UUID playerUUID, String messageText, String playerName, int ticks) {
        messages.put(playerUUID, new PlayerMessage(playerUUID.toString(), messageText, ticks));
    }

    public static PlayerMessage getMessage(UUID playerId) {
        return messages.get(playerId);
    }

    public static void tickUpdate() {
        messages.forEach((uuid, playerMessage) -> {
            if (playerMessage.tickCountdown.decrementAndGet() <= 0) {
                nextLineOrRemove(uuid, playerMessage);
            }
        });
    }

    private static void nextLineOrRemove(UUID uuid, PlayerMessage playerMessage) {
        if (!playerMessage.isEndOfMessage()) {
            playerMessage.currentLineNumber += ChatDataManager.DISPLAY_NUM_LINES;
            playerMessage.tickCountdown.set(ChatDataManager.TICKS_TO_DISPLAY_USER_MESSAGE);
        } else {
            messages.remove(uuid);
        }
    }

    public static void openChatUI(UUID playerId) {
        openChatUIs.put(playerId, Boolean.TRUE);
    }

    public static boolean isChatUIOpen(UUID playerId) {
        return openChatUIs.getOrDefault(playerId, Boolean.FALSE);
    }

    public static void closeChatUI(UUID playerId) {
        openChatUIs.remove(playerId);
    }
}

