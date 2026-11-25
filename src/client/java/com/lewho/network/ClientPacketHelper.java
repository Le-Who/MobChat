// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC – unauthorized use prohibited
package com.lewho.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ClientPacketHelper {

    private ClientPacketHelper() {}


    public static void send(ResourceLocation channelId, FriendlyByteBuf buf) {
        ClientPlayNetworking.send(channelId, buf);
    }

    public static void registerReceiver(
            ResourceLocation channelId,
            ClientPlayNetworking.PlayChannelHandler handler) {
        ClientPlayNetworking.registerGlobalReceiver(channelId, handler);
    }


    public static void send(ServerPlayer player, ResourceLocation channelId, FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, channelId, buf);
    }

    public static void registerReceiver(
            ResourceLocation channelId,
            ServerPlayNetworking.PlayChannelHandler handler) {
        ServerPlayNetworking.registerGlobalReceiver(channelId, handler);
    }
}
