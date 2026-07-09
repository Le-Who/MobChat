// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public final class UpdateStager {
    private static final String UPDATE_DIR = ".creaturechat-updates";

    private UpdateStager() {
    }

    public static PendingUpdate stage(Path gameDir, Path currentJar, UpdateCandidate candidate, byte[] jarBytes) throws IOException {
        Path currentJarParent = currentJar.getParent();
        if (currentJarParent != null && !Files.isWritable(currentJarParent)) {
            throw new IOException("CreatureChat mod jar directory is not writable: " + currentJarParent);
        }
        String expectedHash = UpdateHashes.normalizeSha512(candidate.sha512());
        String actualHash = UpdateHashes.sha512(jarBytes);
        if (!actualHash.equals(expectedHash)) {
            throw new IllegalArgumentException("Downloaded jar sha512 did not match expected sha512.");
        }

        Path updatesDir = updatesDir(gameDir);
        Path stagedJar = updatesDir.resolve(candidate.assetName());
        Files.createDirectories(updatesDir);
        Files.write(stagedJar, jarBytes);

        Path backupJar = updatesDir.resolve("backup")
                .resolve(currentJar.getFileName().toString() + "." + Instant.now().toEpochMilli() + ".bak");
        PendingUpdate pending = new PendingUpdate(
                candidate.version(),
                currentJar.toAbsolutePath().normalize(),
                stagedJar.toAbsolutePath().normalize(),
                backupJar.toAbsolutePath().normalize(),
                pendingFile(gameDir).toAbsolutePath().normalize(),
                expectedHash
        );
        pending.save();
        return pending;
    }

    public static Path pendingFile(Path gameDir) {
        return updatesDir(gameDir).resolve("pending-update.json");
    }

    public static Path updatesDir(Path gameDir) {
        return gameDir.resolve(UPDATE_DIR);
    }
}
