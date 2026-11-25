// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.network;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

final class LoginChatChunks {
    private final Map<Integer, byte[]> chunks = new HashMap<>();
    private int expectedCount;

    byte[] accept(int sequence, int count, byte[] chunk) {
        if (count <= 0 || sequence < 0 || sequence >= count || (expectedCount != 0 && expectedCount != count)) {
            clear();
            return null;
        }

        expectedCount = count;
        chunks.putIfAbsent(sequence, chunk);
        if (chunks.size() < expectedCount) {
            return null;
        }

        ByteArrayOutputStream snapshot = new ByteArrayOutputStream();
        for (int i = 0; i < expectedCount; i++) {
            snapshot.writeBytes(chunks.get(i));
        }
        clear();
        return snapshot.toByteArray();
    }

    void clear() {
        chunks.clear();
        expectedCount = 0;
    }
}
