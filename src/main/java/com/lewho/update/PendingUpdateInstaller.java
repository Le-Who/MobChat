// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class PendingUpdateInstaller {
    private PendingUpdateInstaller() {
    }

    public static boolean tryApplyNow(PendingUpdate pending) {
        try {
            applyNow(pending);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    public static void applyNow(PendingUpdate pending) throws IOException {
        verifyStagedHash(pending);
        Path targetJar = InstalledJarNames.stableTargetFor(pending.currentJar());
        Files.createDirectories(pending.backupJar().getParent());
        Files.move(pending.currentJar(), pending.backupJar(), StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.move(pending.stagedJar(), targetJar, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            restoreBackup(pending.currentJar(), pending.backupJar());
            throw e;
        }
        Files.deleteIfExists(pending.pendingFile());
    }

    private static void verifyStagedHash(PendingUpdate pending) throws IOException {
        String actualHash = UpdateHashes.sha512(pending.stagedJar());
        String expectedHash = UpdateHashes.normalizeSha512(pending.sha512());
        if (!actualHash.equals(expectedHash)) {
            throw new IOException("Staged jar sha512 did not match pending update metadata.");
        }
    }

    private static void restoreBackup(Path currentJar, Path backupJar) {
        try {
            if (Files.exists(backupJar) && !Files.exists(currentJar)) {
                Files.move(backupJar, currentJar, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // Preserve the original failure; admins can restore the backup manually if this fallback fails.
        }
    }
}
