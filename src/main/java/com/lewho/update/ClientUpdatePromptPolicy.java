// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

public final class ClientUpdatePromptPolicy {
    private static final int STARTUP_CHECK_TICKS = 80;

    private ClientUpdatePromptPolicy() {
    }

    public static boolean shouldStartCheck(boolean checkStarted, int ticks) {
        return !checkStarted && ticks > STARTUP_CHECK_TICKS;
    }

    public static boolean shouldPrompt(boolean promptShown, boolean updateAvailable, boolean titleScreenVisible, boolean inGameWithoutScreen) {
        return !promptShown && updateAvailable && (titleScreenVisible || inGameWithoutScreen);
    }
}
