// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.google.gson.JsonParser;
import com.lewho.chat.ChatDataFile;
import com.lewho.chat.ChatSession;
import com.lewho.chat.EntityChatData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.HashMap;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ChatDataFileTests {
    @TempDir
    Path directory;

    @Test
    public void serializationFailurePreservesLastCompleteSave() throws Exception {
        Path save = directory.resolve("chatdata.json");
        Files.writeString(save, "{\"npc\":\"Последняя полная запись\"}");
        Map<String, Object> brokenSnapshot = new BrokenSnapshot();

        assertThrows(RuntimeException.class, () -> new ChatDataFile(save).save(brokenSnapshot));

        assertEquals("{\"npc\":\"Последняя полная запись\"}", Files.readString(save));
    }

    @Test
    public void writesCompleteUtf8SnapshotOverPreviousSave() throws Exception {
        Path save = directory.resolve("chatdata.json");
        Files.writeString(save, "{\"old\":\"data\"}");

        new ChatDataFile(save).save(Map.of("npc", "Привет, мандрівнику! 🐈"));

        assertEquals("Привет, мандрівнику! 🐈", JsonParser.parseString(Files.readString(save)).getAsJsonObject().get("npc").getAsString());
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    public void queuedAutosaveTraversesStateOnlyAfterServerExecutorRuns() throws Exception {
        Queue<Runnable> serverTasks = new ArrayDeque<>();
        ChatSession session = new ChatSession(serverTasks::add);
        Path save = directory.resolve("chatdata.json");
        Map<String, String> state = new HashMap<>();
        state.put("npc", "before");

        session.execute(() -> saveUnchecked(save, state));
        assertFalse(Files.exists(save));
        state.put("npc", "server-owned update");
        serverTasks.remove().run();

        assertEquals("server-owned update", JsonParser.parseString(Files.readString(save)).getAsJsonObject().get("npc").getAsString());
    }

    @Test
    public void queuedAutosaveCannotOverwriteFinalSaveAfterSessionCloses() throws Exception {
        Queue<Runnable> serverTasks = new ArrayDeque<>();
        ChatSession session = new ChatSession(serverTasks::add);
        Path save = directory.resolve("chatdata.json");
        session.execute(() -> saveUnchecked(save, Map.of("npc", "stale")));

        session.close();
        new ChatDataFile(save).save(Map.of("npc", "final"));
        serverTasks.remove().run();
        session.execute(() -> saveUnchecked(save, Map.of("npc", "later timer tick")));

        assertTrue(serverTasks.isEmpty());
        assertEquals("final", JsonParser.parseString(Files.readString(save)).getAsJsonObject().get("npc").getAsString());
    }

    @Test
    public void failedReplacementPreservesExistingDestinationAndRemovesTemporaryFile() throws Exception {
        Path save = directory.resolve("chatdata.json");
        Files.createDirectory(save);
        Path existing = save.resolve("previous.json");
        Files.writeString(existing, "{\"npc\":\"preserved\"}");

        assertThrows(IOException.class, () -> new ChatDataFile(save).save(Map.of("npc", "new")));

        assertEquals("{\"npc\":\"preserved\"}", Files.readString(existing));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    public void failedLoadPreservesExistingHistoryAcrossAutoAndFinalSave() throws Exception {
        Path save = directory.resolve("chatdata.json");
        String malformed = """
                {"old-npc":{"currentMessage":"recoverable history"}
                """;
        Files.writeString(save, malformed);
        ChatDataFile chatDataFile = new ChatDataFile(save);

        assertThrows(RuntimeException.class, chatDataFile::load);
        Queue<Runnable> serverTasks = new ArrayDeque<>();
        ChatSession session = new ChatSession(serverTasks::add);
        Map<String, EntityChatData> newHistory = Map.of("new-npc", new EntityChatData("new-npc"));
        session.execute(() -> saveUnchecked(chatDataFile, newHistory));
        serverTasks.remove().run();
        session.close();

        assertFalse(chatDataFile.save(newHistory));
        assertEquals(malformed, Files.readString(save));
    }

    @Test
    public void missingAndRecoveredHistoryCanBeSaved() throws Exception {
        Path save = directory.resolve("chatdata.json");
        ChatDataFile initialFile = new ChatDataFile(save);
        assertTrue(initialFile.load().isEmpty());
        assertTrue(initialFile.save(Map.of("npc", Map.of("entityId", "npc", "status", "HIDDEN"))));

        ChatDataFile nextSessionFile = new ChatDataFile(save);
        Map<String, EntityChatData> recovered = nextSessionFile.load();
        assertEquals("npc", recovered.get("npc").entityId);
        assertTrue(nextSessionFile.save(recovered));
    }

    private static void saveUnchecked(Path save, Object state) {
        saveUnchecked(new ChatDataFile(save), state);
    }

    private static void saveUnchecked(ChatDataFile save, Object state) {
        try {
            save.save(state);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class BrokenSnapshot extends AbstractMap<String, Object> {
        @Override
        public Set<Entry<String, Object>> entrySet() {
            throw new IllegalStateException("snapshot unavailable");
        }
    }
}
