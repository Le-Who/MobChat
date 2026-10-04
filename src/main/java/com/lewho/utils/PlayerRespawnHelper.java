// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Minecraft-version adapter for the player's configured home/respawn point. */
public final class PlayerRespawnHelper {
    private PlayerRespawnHelper() {
    }

    public static BlockPos position(ServerPlayer player) {
        return player.getRespawnPosition();
    }

    public static ResourceKey<Level> dimension(ServerPlayer player) {
        return player.getRespawnDimension();
    }
}
