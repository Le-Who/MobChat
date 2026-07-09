// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores per-player chat display preferences that should follow the player on
 * the server without changing global OP-only CreatureChat configuration.
 */
public class PlayerChatPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Map<String, Boolean> showOtherNpcRepliesByPlayer = new HashMap<>();

    public boolean getShowOtherNpcReplies(UUID playerId) {
        if (playerId == null) {
            return true;
        }
        return showOtherNpcRepliesByPlayer.getOrDefault(playerId.toString(), true);
    }

    public void setShowOtherNpcReplies(UUID playerId, boolean enabled) {
        if (playerId == null) {
            return;
        }
        showOtherNpcRepliesByPlayer.put(playerId.toString(), enabled);
    }

    public boolean canReceiveEntityUpdate(UUID viewerId, String currentPlayerId, ChatDataManager.ChatSender sender) {
        if (sender != ChatDataManager.ChatSender.ASSISTANT) {
            return true;
        }
        return canReceiveNpcReply(viewerId, currentPlayerId);
    }

    public boolean canReceiveNpcReply(UUID viewerId, String currentPlayerId) {
        if (viewerId == null || currentPlayerId == null || currentPlayerId.isBlank()) {
            return true;
        }
        if (viewerId.toString().equals(currentPlayerId)) {
            return true;
        }
        return getShowOtherNpcReplies(viewerId);
    }

    public void save(Path file) {
        if (file == null) {
            return;
        }
        try {
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save CreatureChat player preferences to {}", file, e);
        }
    }

    public static PlayerChatPreferences load(Path file) {
        if (file == null || !Files.exists(file)) {
            return new PlayerChatPreferences();
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            PlayerChatPreferences preferences = GSON.fromJson(reader, PlayerChatPreferences.class);
            if (preferences == null) {
                return new PlayerChatPreferences();
            }
            preferences.postDeserialize();
            return preferences;
        } catch (Exception e) {
            LOGGER.error("Failed to load CreatureChat player preferences from {}", file, e);
            return new PlayerChatPreferences();
        }
    }

    private void postDeserialize() {
        if (showOtherNpcRepliesByPlayer == null) {
            showOtherNpcRepliesByPlayer = new HashMap<>();
        }
    }
}
