// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC – unauthorized use prohibited
package com.lewho.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

public final class BufferHelper {

    private BufferHelper() {}

    public static FriendlyByteBuf create() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    public static FriendlyByteBuf create(int initialCapacity) {
        return new FriendlyByteBuf(Unpooled.buffer(initialCapacity));
    }
}
