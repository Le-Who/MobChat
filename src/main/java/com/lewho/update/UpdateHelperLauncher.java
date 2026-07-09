// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class UpdateHelperLauncher {
    private static final String HELPER_CLASS_NAME = "com.lewho.update.CreatureChatUpdateHelper";
    private static final String HELPER_RESOURCE = "/com/lewho/update/CreatureChatUpdateHelper.class";

    private UpdateHelperLauncher() {
    }

    public static Path launch(PendingUpdate pending, Path javaExecutable, long currentPid) throws IOException {
        Path helperRoot = pending.pendingFile().getParent().resolve("helper-classes");
        Path helperClass = helperRoot.resolve("com").resolve("lewho").resolve("update").resolve("CreatureChatUpdateHelper.class");
        Files.createDirectories(helperClass.getParent());
        try (InputStream input = UpdateHelperLauncher.class.getResourceAsStream(HELPER_RESOURCE)) {
            if (input == null) {
                throw new IOException("CreatureChat update helper class resource was not found.");
            }
            Files.copy(input, helperClass, StandardCopyOption.REPLACE_EXISTING);
        }

        Path logFile = pending.pendingFile().getParent().resolve("helper.log").toAbsolutePath().normalize();
        List<String> command = List.of(
                javaExecutable.toString(),
                "-cp",
                helperRoot.toAbsolutePath().normalize().toString(),
                HELPER_CLASS_NAME,
                Long.toString(currentPid),
                pending.currentJar().toString(),
                pending.stagedJar().toString(),
                pending.backupJar().toString(),
                pending.pendingFile().toString(),
                pending.sha512()
        );
        new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()))
                .redirectErrorStream(true)
                .start();
        return logFile;
    }
}
