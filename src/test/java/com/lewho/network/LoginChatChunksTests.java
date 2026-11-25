// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LoginChatChunksTests {
    @Test
    public void assemblesInSequenceOrderAfterAllDistinctChunksArrive() {
        LoginChatChunks chunks = new LoginChatChunks();

        assertNull(chunks.accept(1, 2, new byte[]{3, 4}));
        assertNull(chunks.accept(1, 2, new byte[]{3, 4}));
        assertArrayEquals(new byte[]{1, 2, 3, 4}, chunks.accept(0, 2, new byte[]{1, 2}));
        assertArrayEquals(new byte[]{5}, chunks.accept(0, 1, new byte[]{5}));
    }

    @Test
    public void discardsIncompleteSnapshotAfterDisconnect() {
        LoginChatChunks chunks = new LoginChatChunks();

        assertNull(chunks.accept(1, 3, new byte[]{3}));
        chunks.clear();

        assertNull(chunks.accept(0, 2, new byte[]{1}));
        assertArrayEquals(new byte[]{1, 2}, chunks.accept(1, 2, new byte[]{2}));
    }

    @Test
    public void rejectsInconsistentSnapshotMetadataAndAcceptsTheNextSnapshot() {
        LoginChatChunks chunks = new LoginChatChunks();

        assertNull(chunks.accept(0, 2, new byte[]{1}));
        assertNull(chunks.accept(1, 3, new byte[]{2}));
        assertNull(chunks.accept(2, 2, new byte[]{3}));
        assertArrayEquals(new byte[]{4}, chunks.accept(0, 1, new byte[]{4}));
    }
}
