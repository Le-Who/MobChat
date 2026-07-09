// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.update.UpdateVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UpdateVersionTests {

    @Test
    public void newerModVersionWithSameMinecraftVersionIsDetected() {
        assertTrue(UpdateVersion.isNewer("3.0.1+1.20.1", "3.0.0+1.20.1"));
        assertFalse(UpdateVersion.isNewer("3.0.0+1.20.1", "3.0.0+1.20.1"));
        assertFalse(UpdateVersion.isNewer("3.0.0+1.20.1", "3.0.1+1.20.1"));
    }

    @Test
    public void minecraftVersionIsExtractedFromBuildMetadata() {
        assertEquals("1.20.1", UpdateVersion.minecraftVersion("3.0.0+1.20.1"));
        assertEquals("", UpdateVersion.minecraftVersion("3.0.0"));
    }
}
