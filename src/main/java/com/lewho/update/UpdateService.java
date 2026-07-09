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
        String hash = UpdateHashes.normalizeSha512(source.downloadText(candidate.sha512Url()));
        byte[] jarBytes = source.downloadBytes(candidate.downloadUrl());
        return Optional.of(UpdateStager.stage(gameDir, currentJar, candidate.withSha512(hash), jarBytes));
    }
}
