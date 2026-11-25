// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.utils.Compression;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompressionTests {
    @Test
    public void encodesLoginTextAsUtf8AndRoundTripsUnicode() throws DataFormatException {
        String snapshot = "Привіт 世界 🌱";
        byte[] compressed = Compression.compressString(snapshot);

        Inflater inflater = new Inflater();
        try {
            inflater.setInput(compressed);
            byte[] decoded = new byte[128];
            int length = inflater.inflate(decoded);
            assertTrue(inflater.finished());
            assertArrayEquals(snapshot.getBytes(StandardCharsets.UTF_8), Arrays.copyOf(decoded, length));
        } finally {
            inflater.end();
        }
        assertEquals(snapshot, Compression.decompressString(compressed));
    }

    @Test
    public void rejectsAnIncompleteCompressedSnapshot() {
        byte[] compressed = Compression.compressString("{}");
        byte[] incomplete = Arrays.copyOf(compressed, compressed.length - 1);

        assertThrows(DataFormatException.class, () -> Compression.decompressString(incomplete));
    }

    @Test
    public void acceptsACompleteEmptyStream() throws DataFormatException {
        assertEquals("", Compression.decompressString(Compression.compressString("")));
    }
}
