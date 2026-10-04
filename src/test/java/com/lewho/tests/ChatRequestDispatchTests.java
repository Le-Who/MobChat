// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.chat.AutoMessageBucket;
import com.lewho.chat.ChatDataManager;
import com.lewho.chat.ChatMessage;
import com.lewho.chat.ChatSession;
import com.lewho.chat.EntityChatData;
import com.lewho.chat.PlayerData;
import com.lewho.commands.ConfigurationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class ChatRequestDispatchTests {
    private final ChatDataManager manager = ChatDataManager.getServerInstance();
    private final UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    public void clearExistingRateBuckets() {
        manager.clearData();
    }

    @Test
    public void busyNpcDoesNotSpendAmbientQuotaOrRecordAnotherInput() {
        Fixture fixture = new Fixture();
        assertNotNull(fixture.begin());
        fixture.data.ambientBucket = new AutoMessageBucket(1, 3600);
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();

        boolean dispatched = ChatSession.dispatch(fixture::begin,
                () -> manager.handleAmbientResponse(fixture.data, playerId, "Alex", config),
                request -> fixture.data.previousMessages.add(new ChatMessage("extra", ChatDataManager.ChatSender.USER, "Alex")));

        assertFalse(dispatched);
        assertTrue(fixture.data.ambientBucket.hasTokens());
        assertFalse(manager.ambientResponseBuckets.containsKey(playerId));
        assertTrue(fixture.data.previousMessages.isEmpty());
    }

    @Test
    public void rejectedManualInputDoesNotResetReactiveAllowanceOrDamageSummary() {
        Fixture fixture = new Fixture();
        assertNotNull(fixture.begin());
        AutoMessageBucket allowance = new AutoMessageBucket(1, 3600);
        allowance.consume();
        PlayerData player = fixture.data.getPlayerData(playerId.toString(), "Alex");
        player.lastDamageReactionAt = 123L;
        player.suppressedDamageReactionCount = 3;

        boolean dispatched = ChatSession.dispatch(fixture::begin, () -> {
            allowance.reset();
            manager.resetReactiveCooldowns(fixture.data, player);
            return true;
        }, request -> fixture.data.currentMessage = "extra");

        assertFalse(dispatched);
        assertFalse(allowance.hasTokens());
        assertEquals(123L, player.lastDamageReactionAt);
        assertEquals(3, player.suppressedDamageReactionCount);
        assertEquals("", fixture.data.currentMessage);
    }

    @Test
    public void busyNpcDoesNotStartDamageReactionCooldown() {
        Fixture fixture = new Fixture();
        assertNotNull(fixture.begin());
        PlayerData player = fixture.data.getPlayerData(playerId.toString(), "Alex");
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();

        boolean dispatched = ChatSession.dispatch(fixture::begin,
                () -> manager.handleDamageReaction(fixture.data, player, "Alex", config),
                request -> fixture.data.currentMessage = "damage reply");

        assertFalse(dispatched);
        assertEquals(0L, player.lastDamageReactionAt);
        assertEquals(0, player.suppressedDamageReactionCount);
        assertEquals("", fixture.data.currentMessage);
    }

    @Test
    public void preparationFailureReleasesTokenAndRecoversOnlyItsPendingState() {
        Fixture fixture = new Fixture();

        assertThrows(IllegalStateException.class, () -> ChatSession.dispatch(fixture::begin, () -> true, request -> {
            fixture.data.status = ChatDataManager.ChatStatus.PENDING;
            throw new IllegalStateException("preparation failed");
        }));

        assertEquals(ChatDataManager.ChatStatus.HIDDEN, fixture.data.status);
        assertNotNull(fixture.begin());
    }

    @Test
    public void deniedPolicyDoesNotLeaveNpcAdmitted() {
        Fixture fixture = new Fixture();

        assertFalse(ChatSession.dispatch(fixture::begin, () -> false,
                request -> fixture.data.currentMessage = "unexpected"));

        assertEquals("", fixture.data.currentMessage);
        assertNotNull(fixture.begin());
    }

    @Test
    public void admittedAmbientRequestConsumesQuotaOnceUntilItsQueuedResponseRuns() {
        Fixture fixture = new Fixture();
        fixture.data.ambientBucket = new AutoMessageBucket(2, 3600);
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setMaxPlayerAmbientResponses(2);
        CompletableFuture<String> response = new CompletableFuture<>();

        assertTrue(ChatSession.dispatch(fixture::begin,
                () -> manager.handleAmbientResponse(fixture.data, playerId, "Alex", config),
                request -> {
                    fixture.data.status = ChatDataManager.ChatStatus.PENDING;
                    request.whenComplete(response, (message, error) -> {
                        fixture.data.currentMessage = message;
                        fixture.data.status = ChatDataManager.ChatStatus.DISPLAY;
                    });
                }));
        response.complete("accepted reply");
        assertFalse(ChatSession.dispatch(fixture::begin,
                () -> manager.handleAmbientResponse(fixture.data, playerId, "Alex", config),
                request -> fixture.data.currentMessage = "overlap"));
        assertTrue(fixture.data.ambientBucket.hasTokens());
        assertEquals("", fixture.data.currentMessage);

        fixture.drain();

        assertEquals("accepted reply", fixture.data.currentMessage);
        assertNotNull(fixture.begin());
    }

    private static final class Fixture {
        final Queue<Runnable> tasks = new ArrayDeque<>();
        final ChatSession session = new ChatSession(tasks::add);
        final EntityChatData data = new EntityChatData("npc");

        ChatSession.Request begin() {
            return session.tryBegin(data, () -> true, () -> true, data::recoverPendingResponse);
        }

        void drain() {
            while (!tasks.isEmpty()) {
                tasks.remove().run();
            }
        }
    }
}
