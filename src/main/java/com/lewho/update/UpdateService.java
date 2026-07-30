// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import java.io.IOException;
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

    /**
     * Downloads, verifies, and stages a specific {@link UpdateCandidate} without re-running the
     * version check. Use this when the caller already holds the candidate from a prior
     * {@link #check} call.
     */
    public PendingUpdate stageCandidate(Path gameDir, Path currentJar, UpdateCandidate candidate)
            throws IOException, InterruptedException {
        String hash = UpdateHashes.normalizeSha512(source.downloadText(candidate.sha512Url()));
        byte[] jarBytes = source.downloadBytes(candidate.downloadUrl());
        return UpdateStager.stage(gameDir, currentJar, candidate.withSha512(hash), jarBytes);
    }
}
