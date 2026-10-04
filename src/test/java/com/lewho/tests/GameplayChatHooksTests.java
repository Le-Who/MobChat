// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import com.lewho.chat.PlayerData;
import com.lewho.commands.ConfigurationHandler;
import com.lewho.chat.LivingEntityChatHooks;
import com.lewho.chat.MobInteractionHooks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameplayChatHooksTests {
    @BeforeEach
    public void resetManager() {
        ChatDataManager.getServerInstance().clearData();
    }

    @Test
    public void friendlyTargetingUsesUuidEvenWhenNameEntryDisagrees() {
        EntityChatData data = new EntityChatData("mob");
        data.getPlayerData("player-uuid", "Alex").friendship = 3;
        data.getPlayerData("Alex").friendship = -3;

        assertTrue(LivingEntityChatHooks.isFriendlyTo(data, "player-uuid", "Alex"));
        assertFalse(LivingEntityChatHooks.isFriendlyTo(data, "another-uuid", "Sam"));
    }

    @Test
    public void everyDamageEventIsRememberedButImmediateSecondReplyIsSuppressed() {
        EntityChatData data = new EntityChatData("mob");
        data.characterSheet = "Name: Test";
        PlayerData player = data.getPlayerData("player-uuid", "Alex");
        player.friendship = 2;
        data.getPlayerData("Alex").friendship = -3;
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setDamageReactionCooldownSeconds(60);

        assertTrue(LivingEntityChatHooks.recordDamageReaction(data, "player-uuid", "Alex", config));
        assertFalse(LivingEntityChatHooks.recordDamageReaction(data, "player-uuid", "Alex", config));

        assertEquals(2, player.socialEventCount);
        assertEquals(2, player.harmfulActions);
        assertEquals(2, player.lastDamageFriendship);
        assertTrue(player.wordsmithDamaged);
        assertEquals(1, player.suppressedDamageReactionCount);
        assertEquals(0, data.players.get("Alex").harmfulActions);
        assertEquals(ChatDataManager.ChatStatus.NONE, data.status);
    }

    @Test
    public void damageWithoutCharacterStillRecordsSocialEventWithoutRequestingReply() {
        EntityChatData data = new EntityChatData("mob");

        assertFalse(LivingEntityChatHooks.recordDamageReaction(data, "player-uuid", "Alex", null));

        PlayerData player = data.players.get("player-uuid");
        assertEquals(1, player.harmfulActions);
        assertEquals(0, player.lastDamageReactionAt);
    }

    @Test
    public void directGiftUpdatesUuidStateWithoutChangingChatStatus() {
        EntityChatData data = new EntityChatData("mob");
        data.characterSheet = "Name: Test";
        PlayerData player = data.getPlayerData("player-uuid", "Alex");
        data.getPlayerData("Alex");

        assertSame(player, MobInteractionHooks.recordItemInteraction(data, "player-uuid", "Alex", true));

        assertEquals(1, player.helpfulActions);
        assertEquals(1, player.socialEventCount);
        assertEquals(0, data.players.get("Alex").helpfulActions);
        assertEquals(ChatDataManager.ChatStatus.NONE, data.status);
    }

    @Test
    public void showingAnItemDoesNotRecordGift() {
        EntityChatData data = new EntityChatData("mob");
        data.characterSheet = "Name: Test";

        PlayerData player = MobInteractionHooks.recordItemInteraction(data, "player-uuid", "Alex", false);

        assertEquals(0, player.helpfulActions);
        assertEquals(0, player.socialEventCount);
    }
}
