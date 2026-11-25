// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record LegacyPayload(CustomPacketPayload.Type<LegacyPayload> id, FriendlyByteBuf data)
        implements CustomPacketPayload {

    @Override
    public CustomPacketPayload.Type<LegacyPayload> type() {
        return id;
    }

    public static CustomPacketPayload.Type<LegacyPayload> idFor(ResourceLocation chan) {
        return new CustomPacketPayload.Type<>(chan);
    }

    public static StreamCodec<RegistryFriendlyByteBuf, LegacyPayload> codec(
            CustomPacketPayload.Type<LegacyPayload> pid) {

        return StreamCodec.of(
                (RegistryFriendlyByteBuf buf, LegacyPayload p) -> {
                    FriendlyByteBuf data = p.data();
                    buf.writeBytes(data, data.readerIndex(), data.readableBytes());
                },

                (RegistryFriendlyByteBuf buf) -> {
                    byte[] bytes = new byte[buf.readableBytes()];
                    buf.readBytes(bytes);
                    return new LegacyPayload(pid,
                            new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes)));
                });
    }
}
