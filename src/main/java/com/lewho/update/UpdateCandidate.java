// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

public record UpdateCandidate(
        String version,
        String assetName,
        String downloadUrl,
        String sha512Url,
        String sha512,
        String releaseUrl,
        String releaseNotes
) {
    public UpdateCandidate withSha512(String hash) {
        return new UpdateCandidate(version, assetName, downloadUrl, sha512Url, hash, releaseUrl, releaseNotes);
    }
}
