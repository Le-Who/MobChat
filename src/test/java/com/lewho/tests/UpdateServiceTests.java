// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.update.PendingUpdate;
import com.lewho.update.RuntimeModInfo;
import com.lewho.update.UpdateCandidate;
import com.lewho.update.UpdateService;
import com.lewho.update.UpdateSource;
import com.lewho.update.UpdateStager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateServiceTests {
    @TempDir
    Path tempDir;

    @Test
    public void downloadsSha512AndJarBeforeStagingPendingUpdate() throws Exception {
        Path currentJar = tempDir.resolve("mods").resolve("creaturechat-3.0.0+1.20.1.jar");
        Files.createDirectories(currentJar.getParent());
        Files.writeString(currentJar, "old jar", StandardCharsets.UTF_8);
        byte[] newJar = "new jar".getBytes(StandardCharsets.UTF_8);
        String hash = sha512(newJar);

        FakeUpdateSource source = new FakeUpdateSource(new UpdateCandidate(
                "3.0.1+1.20.1",
                "creaturechat-3.0.1+1.20.1.jar",
                "https://example.invalid/jar",
                "https://example.invalid/sha512",
                "",
                "https://github.com/Le-Who/MobChat/releases/tag/v3.0.1",
                "Stable release"
        ), hash + "  creaturechat-3.0.1+1.20.1.jar", newJar);

        UpdateService service = new UpdateService(source);
        PendingUpdate pending = service.downloadAndStage(
                tempDir,
                currentJar,
                "creaturechat",
                "3.0.0+1.20.1",
                "1.20.1",
                false
        ).orElseThrow();

        assertEquals("3.0.1+1.20.1", pending.version());
        assertEquals(hash, pending.sha512());
        assertTrue(Files.exists(pending.stagedJar()));
    }

    @Test
    public void rejectsAStagedUpgradeFromAnotherMinecraftTarget() throws Exception {
        Path currentJar = tempDir.resolve("mods/creaturechat.jar");
        Files.createDirectories(currentJar.getParent());
        Files.writeString(currentJar, "current jar");
        byte[] jar = "jar for another target".getBytes(StandardCharsets.UTF_8);
        UpdateCandidate other = new UpdateCandidate("4.0.0+1.21.7", "creaturechat-4.0.0+1.21.7.jar",
                "", "", sha512(jar), "", "");
        UpdateStager.stage(tempDir, currentJar, other, jar);
        UpdateService service = new UpdateService(new FakeUpdateSource(null, "", new byte[0]));
        RuntimeModInfo runtime = new RuntimeModInfo("creaturechat", "3.1.1+1.20.1", "1.20.1",
                currentJar, tempDir, null);

        assertTrue(service.prepareUpdate(runtime).isEmpty());
    }

    @Test
    public void reusesAnIntactUpdateAndRestagesWhenItsJarIsDamaged() throws Exception {
        Path currentJar = tempDir.resolve("mods/creaturechat.jar");
        Files.createDirectories(currentJar.getParent());
        Files.writeString(currentJar, "current jar");
        byte[] jar = "intact update".getBytes(StandardCharsets.UTF_8);
        String hash = sha512(jar);
        UpdateCandidate candidate = new UpdateCandidate("3.1.2+1.20.1", "creaturechat-3.1.2+1.20.1.jar",
                "", "", hash, "", "");
        PendingUpdate staged = UpdateStager.stage(tempDir, currentJar, candidate, jar);
        RuntimeModInfo runtime = new RuntimeModInfo("creaturechat", "3.1.1+1.20.1", "1.20.1",
                currentJar, tempDir, null);
        UpdateService noNewRelease = new UpdateService(new FakeUpdateSource(null, "", new byte[0]));
        assertEquals(staged, noNewRelease.prepareUpdate(runtime).orElseThrow());

        Files.writeString(staged.stagedJar(), "damaged update");
        UpdateService latestRelease = new UpdateService(new FakeUpdateSource(candidate, hash, jar));
        Path restored = latestRelease.prepareUpdate(runtime).orElseThrow().stagedJar();
        assertEquals("intact update", Files.readString(restored));
    }

    private static final class FakeUpdateSource implements UpdateSource {
        private final UpdateCandidate candidate;
        private final String sha512Text;
        private final byte[] jarBytes;

        private FakeUpdateSource(UpdateCandidate candidate, String sha512Text, byte[] jarBytes) {
            this.candidate = candidate;
            this.sha512Text = sha512Text;
            this.jarBytes = jarBytes;
        }

        @Override
        public Optional<UpdateCandidate> findUpdate(String archiveBaseName, String currentVersion, String minecraftVersion, boolean allowPrerelease) {
            return Optional.ofNullable(candidate);
        }

        @Override
        public String downloadText(String url) {
            return sha512Text;
        }

        @Override
        public byte[] downloadBytes(String url) {
            return jarBytes;
        }
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
