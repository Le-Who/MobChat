// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Optional;

public final class CreatureChatUpdateHelper {
    private static final String STABLE_INSTALLED_JAR = "creaturechat.jar";

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
        log("Installed CreatureChat update from " + stagedJar + " to " + currentJar);
    }

    private static void waitForExit(long pid) throws InterruptedException {
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        while (handle.isPresent() && handle.get().isAlive()) {
            Thread.sleep(1000L);
        }
    }

    private static void replace(Path currentJar, Path stagedJar, Path backupJar, Path pendingFile, String expectedSha512) throws IOException {
        String actualSha512 = sha512(stagedJar);
        if (!actualSha512.equals(expectedSha512)) {
            throw new IOException("Staged jar sha512 did not match pending update metadata.");
        }

        Files.createDirectories(backupJar.getParent());
        Path targetJar = stableTargetFor(currentJar);
        Files.move(currentJar, backupJar, StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.move(stagedJar, targetJar, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            restoreBackup(currentJar, backupJar);
            throw e;
        }
        Files.deleteIfExists(pendingFile);
    }

    private static void restoreBackup(Path currentJar, Path backupJar) {
        try {
            if (Files.exists(backupJar) && !Files.exists(currentJar)) {
                Files.move(backupJar, currentJar, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
        }
    }

    private static Path stableTargetFor(Path currentJar) {
        Path parent = currentJar.getParent();
        return parent == null ? Path.of(STABLE_INSTALLED_JAR) : parent.resolve(STABLE_INSTALLED_JAR);
    }

    private static String sha512(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hash = digest.digest(Files.readAllBytes(path));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 is not available.", e);
        }
    }

    private static void log(String message) {
        System.out.println(Instant.now() + " " + message);
    }
}
