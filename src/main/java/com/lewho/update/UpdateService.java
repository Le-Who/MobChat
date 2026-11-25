// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class UpdateService {
    private final UpdateSource source;

    public UpdateService(UpdateSource source) {
        this.source = source;
    }

    public Optional<UpdateCandidate> check(
            String archiveBaseName,
            String currentVersion,
            String minecraftVersion,
            boolean allowPrerelease
    ) throws IOException, InterruptedException {
        return source.findUpdate(archiveBaseName, currentVersion, minecraftVersion, allowPrerelease);
    }

    public Optional<PendingUpdate> downloadAndStage(
            Path gameDir,
            Path currentJar,
            String archiveBaseName,
            String currentVersion,
            String minecraftVersion,
            boolean allowPrerelease
    ) throws IOException, InterruptedException {
        Optional<UpdateCandidate> maybeCandidate = check(archiveBaseName, currentVersion, minecraftVersion, allowPrerelease);
        if (maybeCandidate.isEmpty()) {
            return Optional.empty();
        }

        UpdateCandidate candidate = maybeCandidate.get();
        return Optional.of(stageCandidate(gameDir, currentJar, candidate));
    }

    public Optional<PendingUpdate> prepareUpdate(RuntimeModInfo runtime) throws IOException, InterruptedException {
        Optional<UpdateCandidate> latest = check(
                runtime.archiveBaseName(), runtime.currentVersion(), runtime.minecraftVersion(), false);
        Optional<PendingUpdate> staged = loadCompatibleUpdate(runtime);
        if (latest.isEmpty() || (staged.isPresent() && !UpdateVersion.isNewer(latest.get().version(), staged.get().version()))) {
            return staged;
        }
        return Optional.of(stageCandidate(runtime.gameDir(), runtime.currentJar(), latest.get()));
    }

    private Optional<PendingUpdate> loadCompatibleUpdate(RuntimeModInfo runtime) {
        Path directory = UpdateStager.updatesDir(runtime.gameDir()).toAbsolutePath().normalize();
        try {
            PendingUpdate pending = PendingUpdate.load(UpdateStager.pendingFile(runtime.gameDir()));
            Path expectedJar = directory.resolve(runtime.archiveBaseName() + "-" + pending.version() + ".jar");
            if (runtime.currentJar().toAbsolutePath().normalize().equals(pending.currentJar())
                    && runtime.minecraftVersion().equals(UpdateVersion.minecraftVersion(pending.version()))
                    && UpdateVersion.isNewer(pending.version(), runtime.currentVersion())
                    && pending.stagedJar().equals(expectedJar)
                    && directory.resolve("backup").equals(pending.backupJar().getParent())
                    && Files.isRegularFile(pending.stagedJar())
                    && UpdateHashes.sha512(pending.stagedJar()).equals(UpdateHashes.normalizeSha512(pending.sha512()))) {
                return Optional.of(pending);
            }
        } catch (IOException | IllegalArgumentException | JsonParseException ignored) {
        }
        return Optional.empty();
    }

    public PendingUpdate stageCandidate(Path gameDir, Path currentJar, UpdateCandidate candidate)
            throws IOException, InterruptedException {
        String hash = UpdateHashes.normalizeSha512(source.downloadText(candidate.sha512Url()));
        byte[] jarBytes = source.downloadBytes(candidate.downloadUrl());
        return UpdateStager.stage(gameDir, currentJar, candidate.withSha512(hash), jarBytes);
    }
}
