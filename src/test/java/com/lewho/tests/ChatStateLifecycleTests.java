// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.google.gson.Gson;
import com.lewho.chat.ChatDataManager;
import com.lewho.chat.ChatSession;
import com.lewho.chat.EntityChatData;
import com.lewho.chat.ChatMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

public class ChatStateLifecycleTests {
    @Test
    public void rejectedChatRequestDoesNotChangeHistoryOrAutoMessageState() {
        EntityChatData data = new EntityChatData("npc");
        data.currentMessage = "previous";
        data.auto_generated = 2;

        assertDoesNotThrow(() -> data.generateMessage("en_us", null, "rejected", true));

        assertTrue(data.previousMessages.isEmpty());
        assertEquals("previous", data.currentMessage);
        assertEquals(2, data.auto_generated);
        assertEquals(ChatDataManager.ChatStatus.NONE, data.status);
    }

    @Test
    public void rejectedCharacterRequestDoesNotChangeHistoryOrAutoMessageState() {
        EntityChatData data = new EntityChatData("npc");
        data.currentMessage = "previous";
        data.auto_generated = 2;

        assertDoesNotThrow(() -> data.generateCharacter("en_us", null, "rejected", false));

        assertTrue(data.previousMessages.isEmpty());
        assertEquals("previous", data.currentMessage);
        assertEquals(2, data.auto_generated);
        assertEquals(ChatDataManager.ChatStatus.NONE, data.status);
    }

    @Test
    public void completedResponseDoesNotMutateChatUntilServerExecutorRuns() {
        Queue<Runnable> serverTasks = new ArrayDeque<>();
        EntityChatData data = new EntityChatData("npc");
        CompletableFuture<String> response = new CompletableFuture<>();
        ChatSession.handoff(serverTasks::add, response, (message, failure) -> data.currentMessage = message);

        response.complete("Привет, traveller!");

        assertEquals("", data.currentMessage);
        serverTasks.remove().run();
        assertEquals("Привет, traveller!", data.currentMessage);
    }

    @Test
    public void savedPendingRequestRecoversWithoutAnHttpRequestAfterReload() {
        EntityChatData data = new Gson().fromJson(
                "{\"entityId\":\"npc\",\"status\":\"PENDING\",\"currentMessage\":\"hello\",\"auto_generated\":3}", EntityChatData.class);

        data.postDeserializeInitialization();

        assertEquals(ChatDataManager.ChatStatus.HIDDEN, data.status);
        assertEquals(0, data.auto_generated);
        assertEquals("hello", data.currentMessage);
    }

    @Test
    public void overlappingRequestIsRejectedUntilTheFirstCompletionIsApplied() {
        Fixture fixture = new Fixture();
        ChatSession.Request request = fixture.begin();
        CompletableFuture<String> response = fixture.watch(request);

        assertNull(fixture.begin());
        response.complete("first");
        assertNull(fixture.begin());
        assertTrue(fixture.data.previousMessages.isEmpty());

        fixture.drain();
        assertEquals("first", fixture.data.previousMessages.get(0).message);
        assertNotNull(fixture.begin());
    }

    @Test
    public void removedStateDoesNotReceiveALateResponse() {
        Fixture fixture = new Fixture();
        CompletableFuture<String> response = fixture.watch(fixture.begin());
        fixture.states.remove("npc");

        response.complete("late");
        fixture.drain();

        assertTrue(fixture.data.previousMessages.isEmpty());
        assertTrue(fixture.states.isEmpty());
    }

    @Test
    public void replacedStateAndItsPendingRequestAreNotChangedByOldResponse() {
        Fixture fixture = new Fixture();
        CompletableFuture<String> oldResponse = fixture.watch(fixture.begin());
        EntityChatData original = fixture.data;
        fixture.data = new EntityChatData("npc");
        fixture.states.put("npc", fixture.data);
        CompletableFuture<String> newResponse = fixture.watch(fixture.begin());

        oldResponse.complete("old");
        fixture.drain();
        assertEquals(ChatDataManager.ChatStatus.PENDING, fixture.data.status);
        assertTrue(fixture.data.previousMessages.isEmpty());
        assertTrue(original.previousMessages.isEmpty());

        newResponse.complete("new");
        fixture.drain();
        assertEquals("new", fixture.data.currentMessage);
    }

    @Test
    public void playerDisconnectBeforeQueuedCompletionRecoversCurrentPendingState() {
        Fixture fixture = new Fixture();
        CompletableFuture<String> response = fixture.watch(fixture.begin());
        fixture.data.auto_generated = 1;
        response.complete("late");
        fixture.playerCurrent.set(false);

        fixture.drain();

        assertTrue(fixture.data.previousMessages.isEmpty());
        assertEquals(ChatDataManager.ChatStatus.HIDDEN, fixture.data.status);
        assertEquals(0, fixture.data.auto_generated);
    }

    @Test
    public void worldChangeDiscardsResponseAndReleasesAdmission() {
        Fixture fixture = new Fixture();
        CompletableFuture<String> response = fixture.watch(fixture.begin());
        fixture.worldCurrent.set(false);
        response.complete("wrong world");
        fixture.drain();

        assertEquals(ChatDataManager.ChatStatus.HIDDEN, fixture.data.status);
        assertTrue(fixture.data.previousMessages.isEmpty());
        fixture.worldCurrent.set(true);
        assertNotNull(fixture.begin());
    }

    @Test
    public void cancelledRequestCannotClearTheNextRequestPendingState() {
        Fixture fixture = new Fixture();
        CompletableFuture<String> oldResponse = fixture.watch(fixture.begin());
        fixture.session.cancel(fixture.data);
        CompletableFuture<String> newResponse = fixture.watch(fixture.begin());

        oldResponse.complete("cancelled");
        fixture.drain();

        assertEquals(ChatDataManager.ChatStatus.PENDING, fixture.data.status);
        assertTrue(fixture.data.previousMessages.isEmpty());
        newResponse.complete("accepted");
        fixture.drain();
        assertEquals("accepted", fixture.data.currentMessage);
    }

    @Test
    public void invalidContextAllowsNewRequestWithoutOldCompletionChangingIt() {
        Fixture fixture = new Fixture();
        CompletableFuture<String> oldResponse = fixture.watch(fixture.begin());
        fixture.playerCurrent.set(false);
        EntityChatData captured = fixture.data;
        ChatSession.Request replacement = fixture.session.tryBegin(captured,
                () -> fixture.states.get("npc") == captured, () -> true, captured::recoverPendingResponse);
        assertNotNull(replacement);
        captured.status = ChatDataManager.ChatStatus.PENDING;

        oldResponse.complete("disconnected player");
        fixture.drain();

        assertEquals(ChatDataManager.ChatStatus.PENDING, captured.status);
        assertTrue(captured.previousMessages.isEmpty());
        replacement.cancel();
        assertEquals(ChatDataManager.ChatStatus.HIDDEN, captured.status);
    }

    @Test
    public void uuidMigrationCancelsRequestBeforeChangingStoredIdentity() {
        Fixture fixture = new Fixture();
        CompletableFuture<String> response = fixture.watch(fixture.begin());
        fixture.session.cancel(fixture.data);
        fixture.states.remove("npc");
        fixture.data.entityId = "released-npc";
        fixture.states.put("released-npc", fixture.data);

        response.complete("old identity");
        fixture.drain();

        assertEquals(ChatDataManager.ChatStatus.HIDDEN, fixture.data.status);
        assertEquals("released-npc", fixture.data.entityId);
        assertTrue(fixture.data.previousMessages.isEmpty());
    }

    @Test
    public void changedStateIdentifierDoesNotReceiveALateResponse() {
        Fixture fixture = new Fixture();
        CompletableFuture<String> response = fixture.watch(fixture.begin());
        fixture.data.entityId = "changed-without-migration";

        response.complete("old identifier");
        fixture.drain();

        assertTrue(fixture.data.previousMessages.isEmpty());
        assertEquals("", fixture.data.currentMessage);
    }

    @Test
    public void restartedSessionPendingIsNotClearedByOldSessionCompletion() {
        Fixture fixture = new Fixture();
        CompletableFuture<String> oldResponse = fixture.watch(fixture.begin());
        fixture.session.close();
        ChatSession restarted = new ChatSession(fixture.tasks::add);
        ChatSession.Request request = restarted.tryBegin(fixture.data,
                () -> true, () -> true, fixture.data::recoverPendingResponse);
        assertNotNull(request);
        fixture.data.status = ChatDataManager.ChatStatus.PENDING;

        oldResponse.complete("previous server session");
        fixture.drain();

        assertEquals(ChatDataManager.ChatStatus.PENDING, fixture.data.status);
        assertTrue(fixture.data.previousMessages.isEmpty());
        restarted.close();
        assertEquals(ChatDataManager.ChatStatus.HIDDEN, fixture.data.status);
    }

    @Test
    public void closedSessionDiscardsResponseAndSettlesPendingBeforeFinalSave() {
        Fixture fixture = new Fixture();
        CompletableFuture<String> response = fixture.watch(fixture.begin());

        fixture.session.close();
        assertEquals(ChatDataManager.ChatStatus.HIDDEN, fixture.data.status);
        assertNull(fixture.begin());
        response.complete("old world");
        fixture.drain();

        assertTrue(fixture.data.previousMessages.isEmpty());
        assertEquals(ChatDataManager.ChatStatus.HIDDEN, fixture.data.status);
    }

    @Test
    public void exceptionalCompletionRunsOnOwnerAndReleasesPending() {
        Fixture fixture = new Fixture();
        CompletableFuture<String> response = new CompletableFuture<>();
        fixture.begin().whenComplete(response, (result, error) -> fixture.data.currentMessage = error.getMessage());

        response.completeExceptionally(new IllegalStateException("transport failed"));
        assertEquals("", fixture.data.currentMessage);
        fixture.drain();

        assertEquals("transport failed", fixture.data.currentMessage);
        assertEquals(ChatDataManager.ChatStatus.HIDDEN, fixture.data.status);
        assertNotNull(fixture.begin());
    }

    @Test
    public void processingFailureStillReleasesAdmissionAndRecoversPending() {
        Fixture fixture = new Fixture();
        CompletableFuture<String> response = new CompletableFuture<>();
        fixture.begin().whenComplete(response, (result, error) -> {
            throw new IllegalStateException("processing failed");
        });

        response.complete("received");
        IllegalStateException failure = assertThrows(IllegalStateException.class, fixture::drain);

        assertEquals("processing failed", failure.getMessage());
        assertEquals(ChatDataManager.ChatStatus.HIDDEN, fixture.data.status);
        assertNotNull(fixture.begin());
    }

    private static final class Fixture {
        final Queue<Runnable> tasks = new ArrayDeque<>();
        final ChatSession session = new ChatSession(tasks::add);
        final Map<String, EntityChatData> states = new HashMap<>();
        final AtomicBoolean playerCurrent = new AtomicBoolean(true);
        final AtomicBoolean worldCurrent = new AtomicBoolean(true);
        EntityChatData data = new EntityChatData("npc");

        Fixture() {
            states.put("npc", data);
        }

        ChatSession.Request begin() {
            EntityChatData captured = data;
            ChatSession.Request request = session.tryBegin(captured,
                    () -> states.get("npc") == captured && "npc".equals(captured.entityId),
                    () -> playerCurrent.get() && worldCurrent.get(), captured::recoverPendingResponse);
            if (request != null) {
                data.status = ChatDataManager.ChatStatus.PENDING;
            }
            return request;
        }

        CompletableFuture<String> watch(ChatSession.Request request) {
            EntityChatData captured = data;
            CompletableFuture<String> response = new CompletableFuture<>();
            request.whenComplete(response, (message, failure) -> {
                captured.currentMessage = message;
                captured.previousMessages.add(new ChatMessage(message, ChatDataManager.ChatSender.ASSISTANT, "Alex"));
                captured.status = ChatDataManager.ChatStatus.DISPLAY;
            });
            return response;
        }

        void drain() {
            while (!tasks.isEmpty()) {
                tasks.remove().run();
            }
        }
    }
}
