// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPacketHelper {

    private ClientPacketHelper() {}

    private static final Map<ResourceLocation, CustomPacketPayload.Type<LegacyPayload>> IDS =
            new ConcurrentHashMap<>();

    private static CustomPacketPayload.Type<LegacyPayload> idOf(ResourceLocation ch) {
        return IDS.computeIfAbsent(ch, key -> {
            var pid   = LegacyPayload.idFor(key);
            var codec = LegacyPayload.codec(pid);

            try { PayloadTypeRegistry.playC2S().register(pid, codec);
            } catch (IllegalArgumentException ignored) { }

            try { PayloadTypeRegistry.playS2C().register(pid, codec);
            } catch (IllegalArgumentException ignored) { }

            return pid;
        });
    }

    @FunctionalInterface
    public interface ClientHandler {
        void receive(Minecraft client,
                     ClientPacketListener handler,
                     FriendlyByteBuf buffer,
                     PacketSender responseSender);
    }

    public static void send(ResourceLocation channel, FriendlyByteBuf buf) {
        ClientPlayNetworking.send(new LegacyPayload(idOf(channel), buf));
    }

    public static void registerReceiver(ResourceLocation channel, ClientHandler h) {
        ClientPlayNetworking.registerGlobalReceiver(idOf(channel),
                (LegacyPayload p, ClientPlayNetworking.Context ctx) -> {
                    ClientPacketListener handler = ctx.player().connection;
                    ctx.client().execute(() -> h.receive(
                            ctx.client(),
                            handler,
                            p.data(),
                            ctx.responseSender()));
                });
    }
}