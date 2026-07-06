// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.tests;

import com.owlmaddie.chat.EntityChatData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntityChatDataHomeTests {

    @Test
    public void homePositionCanBeStoredAndCleared() {
        EntityChatData data = new EntityChatData("entity-id");

        assertFalse(data.hasHome());

        data.setHome("minecraft:overworld", 10, 64, -3);

        assertTrue(data.hasHome());
        assertEquals("minecraft:overworld", data.homeDimension);
        assertEquals(10, data.homeX);
        assertEquals(64, data.homeY);
        assertEquals(-3, data.homeZ);

        data.clearHome();

        assertFalse(data.hasHome());
    }
}
