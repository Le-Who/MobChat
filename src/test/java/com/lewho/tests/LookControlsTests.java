// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.controls.LookControls;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class LookControlsTests {

    @Test
    public void nonVanillaSlimeMoveControlIsNotCompatibleWithVanillaSlimeCast() {
        assertFalse(LookControls.isVanillaSlimeMoveControlCompatible(new Object()));
        assertFalse(LookControls.isVanillaSlimeMoveControlCompatible(null));
    }
}
