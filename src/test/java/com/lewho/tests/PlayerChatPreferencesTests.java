// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.PlayerChatPreferences;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlayerChatPreferencesTests {

    @TempDir
    Path tempDir;

    @Test
    public void npcRepliesToOtherPlayersAreVisibleByDefaultAndCanBeHidden() {
        UUID viewer = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID otherPlayer = UUID.fromString("00000000-0000-0000-0000-000000000002");
        PlayerChatPreferences preferences = new PlayerChatPreferences();

        assertTrue(preferences.canReceiveEntityUpdate(
                viewer,
                otherPlayer.toString(),
                ChatDataManager.ChatSender.ASSISTANT
        ));

        preferences.setShowOtherNpcReplies(viewer, false);

        assertFalse(preferences.canReceiveEntityUpdate(
                viewer,
                otherPlayer.toString(),
                ChatDataManager.ChatSender.ASSISTANT
        ));
        assertTrue(preferences.canReceiveEntityUpdate(
                viewer,
                viewer.toString(),
                ChatDataManager.ChatSender.ASSISTANT
        ));
        assertTrue(preferences.canReceiveEntityUpdate(
                viewer,
                otherPlayer.toString(),
                ChatDataManager.ChatSender.USER
        ));
    }

    @Test
    public void playerPreferencePersistsToDisk() {
        UUID viewer = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Path file = tempDir.resolve("creaturechat_player_prefs.json");
        PlayerChatPreferences preferences = new PlayerChatPreferences();
        preferences.setShowOtherNpcReplies(viewer, false);

        preferences.save(file);

        PlayerChatPreferences loaded = PlayerChatPreferences.load(file);
        assertFalse(loaded.getShowOtherNpcReplies(viewer));
        assertTrue(loaded.getShowOtherNpcReplies(UUID.fromString("00000000-0000-0000-0000-000000000002")));
    }

    @Test
    public void npcRepliesInNormalChatUseTheSameOverhearPreference() {
        UUID viewer = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID addressedPlayer = UUID.fromString("00000000-0000-0000-0000-000000000002");
        PlayerChatPreferences preferences = new PlayerChatPreferences();

        assertTrue(preferences.canReceiveNpcReply(viewer, addressedPlayer.toString()));

        preferences.setShowOtherNpcReplies(viewer, false);

        assertFalse(preferences.canReceiveNpcReply(viewer, addressedPlayer.toString()));
        assertTrue(preferences.canReceiveNpcReply(addressedPlayer, addressedPlayer.toString()));
    }
}
