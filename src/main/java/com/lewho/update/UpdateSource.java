// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import java.io.IOException;
import java.util.Optional;

public interface UpdateSource {
    Optional<UpdateCandidate> findUpdate(
            String archiveBaseName,
            String currentVersion,
            String minecraftVersion,
            boolean allowPrerelease
    ) throws IOException, InterruptedException;

    String downloadText(String url) throws IOException, InterruptedException;

    byte[] downloadBytes(String url) throws IOException, InterruptedException;
}
