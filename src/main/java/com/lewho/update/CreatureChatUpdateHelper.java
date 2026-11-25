// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

public final class CreatureChatUpdateHelper {
    static final String STABLE_INSTALLED_JAR = "creaturechat.jar";

    private CreatureChatUpdateHelper() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 6) {
            throw new IllegalArgumentException("Expected pid, currentJar, stagedJar, backupJar, pendingFile, sha512.");
        }

        long pid = Long.parseLong(args[0]);
        Path currentJar = Path.of(args[1]);
        Path stagedJar = Path.of(args[2]);
        Path backupJar = Path.of(args[3]);
        Path pendingFile = Path.of(args[4]);
        String expectedSha512 = args[5].trim().toLowerCase();

        log("Waiting for process " + pid + " to exit before replacing " + currentJar);
        waitForExit(pid);
        replace(currentJar, stagedJar, backupJar, pendingFile, expectedSha512);
        log("Installed CreatureChat update from " + stagedJar + " to " + stableTargetFor(currentJar));
    }

    private static void waitForExit(long pid) throws InterruptedException {
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        while (handle.isPresent() && handle.get().isAlive()) {
            Thread.sleep(1000L);
        }
    }

    static void replace(Path currentJar, Path stagedJar, Path backupJar, Path pendingFile, String expectedSha512) throws IOException {
        String actualSha512 = sha512(stagedJar);
        if (!actualSha512.equals(expectedSha512)) {
            throw new IOException("Staged jar sha512 did not match pending update metadata.");
        }

        Files.createDirectories(backupJar.getParent());
        Path targetJar = stableTargetFor(currentJar);
        Files.move(currentJar, backupJar);
        try {
            Files.move(stagedJar, targetJar);
        } catch (IOException e) {
            try {
                Files.move(backupJar, currentJar);
            } catch (IOException restoreFailure) {
                e.addSuppressed(restoreFailure);
            }
            throw e;
        }
        Files.deleteIfExists(pendingFile);
    }

    private static Path stableTargetFor(Path currentJar) {
        Path parent = currentJar.getParent();
        return parent == null ? Path.of(STABLE_INSTALLED_JAR) : parent.resolve(STABLE_INSTALLED_JAR);
    }

    static String sha512(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 is not available.", e);
        }
    }

    private static void log(String message) {
        System.out.println(Instant.now() + " " + message);
    }
}
