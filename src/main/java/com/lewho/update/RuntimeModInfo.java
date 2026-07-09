// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import java.nio.file.Path;

public record RuntimeModInfo(
        String archiveBaseName,
        String currentVersion,
        String minecraftVersion,
        Path currentJar,
        Path gameDir,
        Path javaExecutable
) {
}
