// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.tests;

import com.owlmaddie.controls.LookControls;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class LookControlsTests {

    @Test
    public void nonVanillaSlimeMoveControlIsNotCompatibleWithVanillaSlimeCast() {
        assertFalse(LookControls.isVanillaSlimeMoveControlCompatible(new Object()));
        assertFalse(LookControls.isVanillaSlimeMoveControlCompatible(null));
    }
}
