// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.chat.EntityChatData;
import com.lewho.network.ServerPackets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ServerPacketsSafetyTests {

    @Test
    public void broadcastEntityMessageIsIgnoredAfterServerInstanceIsCleared() {
        ServerPackets.serverInstance = null;
        EntityChatData chatData = new EntityChatData("00000000-0000-0000-0000-000000000000");

        assertDoesNotThrow(() -> ServerPackets.BroadcastEntityMessage(chatData));
    }
}
