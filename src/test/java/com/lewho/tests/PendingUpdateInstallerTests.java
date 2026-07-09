// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.update.PendingUpdate;
import com.lewho.update.PendingUpdateInstaller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PendingUpdateInstallerTests {
    @TempDir
    Path tempDir;

    @Test
    public void replacesCurrentJarAndKeepsBackup() throws Exception {
        Path currentJar = tempDir.resolve("mods").resolve("creaturechat.jar");
        Path stagedJar = tempDir.resolve(".creaturechat-updates").resolve("creaturechat-3.0.1+1.20.1.jar");
        Path backupJar = tempDir.resolve(".creaturechat-updates").resolve("backup").resolve("creaturechat.jar.bak");
        Path pendingFile = tempDir.resolve(".creaturechat-updates").resolve("pending-update.json");
        Files.createDirectories(currentJar.getParent());
        Files.createDirectories(stagedJar.getParent());
        Files.writeString(currentJar, "old jar", StandardCharsets.UTF_8);
        Files.writeString(stagedJar, "new jar", StandardCharsets.UTF_8);

        PendingUpdate pending = new PendingUpdate(
                "3.0.1+1.20.1",
                currentJar,
                stagedJar,
                backupJar,
                pendingFile,
                sha512("new jar".getBytes(StandardCharsets.UTF_8))
        );
        pending.save();

        PendingUpdateInstaller.applyNow(pending);

        assertEquals("new jar", Files.readString(currentJar, StandardCharsets.UTF_8));
        assertEquals("old jar", Files.readString(backupJar, StandardCharsets.UTF_8));
        assertFalse(Files.exists(stagedJar));
        assertFalse(Files.exists(pendingFile));
    }

    @Test
    public void refusesToInstallWhenStagedHashChanged() throws Exception {
        Path currentJar = tempDir.resolve("mods").resolve("creaturechat-3.0.0+1.20.1.jar");
        Path stagedJar = tempDir.resolve(".creaturechat-updates").resolve("creaturechat-3.0.1+1.20.1.jar");
        Path backupJar = tempDir.resolve(".creaturechat-updates").resolve("backup").resolve("creaturechat-3.0.0+1.20.1.jar.bak");
        Path pendingFile = tempDir.resolve(".creaturechat-updates").resolve("pending-update.json");
        Files.createDirectories(currentJar.getParent());
        Files.createDirectories(stagedJar.getParent());
        Files.writeString(currentJar, "old jar", StandardCharsets.UTF_8);
        Files.writeString(stagedJar, "tampered jar", StandardCharsets.UTF_8);

        PendingUpdate pending = new PendingUpdate(
                "3.0.1+1.20.1",
                currentJar,
                stagedJar,
                backupJar,
                pendingFile,
                sha512("new jar".getBytes(StandardCharsets.UTF_8))
        );

        boolean installed = PendingUpdateInstaller.tryApplyNow(pending);

        assertFalse(installed);
        assertEquals("old jar", Files.readString(currentJar, StandardCharsets.UTF_8));
        assertTrue(Files.exists(stagedJar));
    }

    @Test
    public void migratesVersionedInstalledJarToStableInstalledName() throws Exception {
        Path currentJar = tempDir.resolve("mods").resolve("creaturechat-3.0.0+1.20.1.jar");
        Path targetJar = tempDir.resolve("mods").resolve("creaturechat.jar");
        Path stagedJar = tempDir.resolve(".creaturechat-updates").resolve("creaturechat-3.0.1+1.20.1.jar");
        Path backupJar = tempDir.resolve(".creaturechat-updates").resolve("backup").resolve("creaturechat-3.0.0+1.20.1.jar.bak");
        Path pendingFile = tempDir.resolve(".creaturechat-updates").resolve("pending-update.json");
        Files.createDirectories(currentJar.getParent());
        Files.createDirectories(stagedJar.getParent());
        Files.writeString(currentJar, "old jar", StandardCharsets.UTF_8);
        Files.writeString(stagedJar, "new jar", StandardCharsets.UTF_8);

        PendingUpdate pending = new PendingUpdate(
                "3.0.1+1.20.1",
                currentJar,
                stagedJar,
                backupJar,
                pendingFile,
                sha512("new jar".getBytes(StandardCharsets.UTF_8))
        );

        PendingUpdateInstaller.applyNow(pending);

        assertFalse(Files.exists(currentJar));
        assertEquals("new jar", Files.readString(targetJar, StandardCharsets.UTF_8));
        assertEquals("old jar", Files.readString(backupJar, StandardCharsets.UTF_8));
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
