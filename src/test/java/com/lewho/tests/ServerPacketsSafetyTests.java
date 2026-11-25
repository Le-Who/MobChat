// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.chat.EntityChatData;
import com.lewho.chat.PlayerData;
import com.lewho.network.ServerPackets;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServerPacketsSafetyTests {

    @Test
    public void broadcastEntityMessageIsIgnoredAfterServerInstanceIsCleared() {
        ServerPackets.serverInstance = null;
        EntityChatData chatData = new EntityChatData("00000000-0000-0000-0000-000000000000");

        assertDoesNotThrow(() -> ServerPackets.BroadcastEntityMessage(chatData));
    }

    @Test
    public void relationshipSyncOnlyWritesTheViewersData() {
        EntityChatData data = new EntityChatData("npc");
        PlayerData viewer = data.getPlayerData("Viewer");
        viewer.friendship = 2;
        viewer.socialSummary = "Viewer note";
        data.getPlayerData("Other player").socialSummary = "Other note";
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            ServerPackets.writeViewerRelationship(buffer, data, "viewer-uuid", "Viewer");
            assertEquals(1, buffer.readInt());
            assertEquals("viewer-uuid", buffer.readUtf());
            assertEquals(2, buffer.readInt());
            buffer.skipBytes(4 * Integer.BYTES);
            assertEquals("Viewer note", buffer.readUtf());
            assertEquals(0, buffer.readableBytes());
            assertEquals(2, data.players.size());
        } finally {
            buffer.release();
        }
    }
}
