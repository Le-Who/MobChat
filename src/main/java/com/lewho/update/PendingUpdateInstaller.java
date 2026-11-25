// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import java.io.IOException;

public final class PendingUpdateInstaller {
    private PendingUpdateInstaller() {
    }

    public static boolean tryApplyNow(PendingUpdate pending) {
        try {
            applyNow(pending);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    public static void applyNow(PendingUpdate pending) throws IOException {
        CreatureChatUpdateHelper.replace(pending.currentJar(), pending.stagedJar(), pending.backupJar(),
                pending.pendingFile(), UpdateHashes.normalizeSha512(pending.sha512()));
    }
}
