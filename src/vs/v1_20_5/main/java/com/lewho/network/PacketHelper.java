// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC – unauthorized use prohibited
package com.lewho.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketHelper {

    private PacketHelper() {}

    @FunctionalInterface
    public interface TriConsumer<T, U, V> { void accept(T t, U u, V v); }

    private static final Map<ResourceLocation, CustomPacketPayload.Type<LegacyPayload>> IDS =
            new ConcurrentHashMap<>();

    private static CustomPacketPayload.Type<LegacyPayload> idOf(ResourceLocation ch) {
        return IDS.computeIfAbsent(ch, key -> {
            var pid   = LegacyPayload.idFor(key);
            var codec = LegacyPayload.codec(pid);

            try { PayloadTypeRegistry.playC2S().register(pid, codec); }
            catch (IllegalArgumentException ignored) {}
            try { PayloadTypeRegistry.playS2C().register(pid, codec); }
            catch (IllegalArgumentException ignored) {}

            return pid;
        });
    }

    public static void send(ServerPlayer player,
                            ResourceLocation channel,
                            FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, new LegacyPayload(idOf(channel), buf));
    }

    public static void registerReceiver(ResourceLocation channel,
                                        TriConsumer<MinecraftServer,
                                                ServerPlayer,
                                                FriendlyByteBuf> handler) {

        ServerPlayNetworking.registerGlobalReceiver(idOf(channel),
                (LegacyPayload p, ServerPlayNetworking.Context ctx) -> handler.accept(
                        ctx.player().getServer(),
                        ctx.player(),
                        p.data()
                ));
    }
}
