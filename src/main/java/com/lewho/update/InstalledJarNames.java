// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import java.nio.file.Path;

final class InstalledJarNames {
    static final String STABLE_INSTALLED_JAR = "creaturechat.jar";

    private InstalledJarNames() {
    }

    static Path stableTargetFor(Path currentJar) {
        Path parent = currentJar.getParent();
        return parent == null ? Path.of(STABLE_INSTALLED_JAR) : parent.resolve(STABLE_INSTALLED_JAR);
    }
}
