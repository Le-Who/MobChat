// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.update.ClientUpdatePromptPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClientUpdatePromptPolicyTests {

    @Test
    public void clientUpdateCheckStartsAfterStartupTicksEvenWithoutPlayer() {
        assertFalse(ClientUpdatePromptPolicy.shouldStartCheck(false, 80));
        assertTrue(ClientUpdatePromptPolicy.shouldStartCheck(false, 81));
        assertFalse(ClientUpdatePromptPolicy.shouldStartCheck(true, 120));
    }

    @Test
    public void promptCanBeShownOnTitleScreenOrIdleInGame() {
        assertTrue(ClientUpdatePromptPolicy.shouldPrompt(false, true, true, false));
        assertTrue(ClientUpdatePromptPolicy.shouldPrompt(false, true, false, true));
        assertFalse(ClientUpdatePromptPolicy.shouldPrompt(false, true, false, false));
        assertFalse(ClientUpdatePromptPolicy.shouldPrompt(true, true, true, false));
        assertFalse(ClientUpdatePromptPolicy.shouldPrompt(false, false, true, false));
    }
}
