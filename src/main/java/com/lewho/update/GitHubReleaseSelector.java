// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class GitHubReleaseSelector {
    private GitHubReleaseSelector() {
    }

    public static Optional<UpdateCandidate> select(
            List<GitHubReleaseParser.Release> releases,
            String archiveBaseName,
            String currentVersion,
            String minecraftVersion,
            boolean allowPrerelease
    ) {
        UpdateCandidate best = null;
        String prefix = archiveBaseName + "-";
        String suffix = "+" + minecraftVersion + ".jar";

        for (GitHubReleaseParser.Release release : releases) {
            if (release.draft() || (release.prerelease() && !allowPrerelease)) {
                continue;
            }

            Map<String, GitHubReleaseParser.Asset> assetsByName = new HashMap<>();
            for (GitHubReleaseParser.Asset asset : release.assets()) {
                assetsByName.put(asset.name(), asset);
            }

            for (GitHubReleaseParser.Asset asset : release.assets()) {
                String assetName = asset.name();
                if (!assetName.startsWith(prefix) || !assetName.endsWith(suffix)) {
                    continue;
                }
                String version = assetName.substring(prefix.length(), assetName.length() - ".jar".length());
                if (!UpdateVersion.isNewer(version, currentVersion)) {
                    continue;
                }

                GitHubReleaseParser.Asset sha512Asset = assetsByName.get(assetName + ".sha512");
                if (sha512Asset == null || sha512Asset.browserDownloadUrl().isBlank()) {
                    continue;
                }

                UpdateCandidate candidate = new UpdateCandidate(
                        version,
                        assetName,
                        asset.browserDownloadUrl(),
                        sha512Asset.browserDownloadUrl(),
                        "",
                        release.htmlUrl(),
                        release.body()
                );
                if (best == null || UpdateVersion.isNewer(candidate.version(), best.version())) {
                    best = candidate;
                }
            }
        }

        return Optional.ofNullable(best);
    }
}
