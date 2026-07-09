// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.update.PendingUpdate;
import com.lewho.update.UpdateCandidate;
import com.lewho.update.UpdateStager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateStagerTests {
    @TempDir
    Path tempDir;

    @Test
    public void stagesValidatedJarAndWritesPendingUpdate() throws Exception {
        Path currentJar = tempDir.resolve("mods").resolve("creaturechat-3.0.0+1.20.1.jar");
        Files.createDirectories(currentJar.getParent());
        Files.writeString(currentJar, "old jar", StandardCharsets.UTF_8);
        byte[] newJar = "new jar".getBytes(StandardCharsets.UTF_8);

        UpdateCandidate candidate = candidate(sha512(newJar));
        PendingUpdate pending = UpdateStager.stage(tempDir, currentJar, candidate, newJar);

        assertTrue(Files.exists(pending.stagedJar()));
        assertTrue(Files.exists(UpdateStager.pendingFile(tempDir)));
        assertEquals(currentJar, pending.currentJar());
        assertEquals(candidate.version(), pending.version());
        assertEquals(sha512(newJar), pending.sha512());
    }

    @Test
    public void rejectsDownloadedJarWhenSha512DoesNotMatch() throws Exception {
        Path currentJar = tempDir.resolve("mods").resolve("creaturechat-3.0.0+1.20.1.jar");
        Files.createDirectories(currentJar.getParent());
        Files.writeString(currentJar, "old jar", StandardCharsets.UTF_8);
        byte[] newJar = "new jar".getBytes(StandardCharsets.UTF_8);

        UpdateCandidate candidate = candidate("00");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> UpdateStager.stage(tempDir, currentJar, candidate, newJar)
        );
        assertTrue(error.getMessage().contains("sha512"));
    }

    private UpdateCandidate candidate(String hash) {
        return new UpdateCandidate(
                "3.0.1+1.20.1",
                "creaturechat-3.0.1+1.20.1.jar",
                "https://example.invalid/creaturechat-3.0.1+1.20.1.jar",
                "https://example.invalid/creaturechat-3.0.1+1.20.1.jar.sha512",
                hash,
                "https://github.com/Le-Who/MobChat/releases/tag/v3.0.1",
                "Stable release"
        );
    }

    private String sha512(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        byte[] hash = digest.digest(bytes);
        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
