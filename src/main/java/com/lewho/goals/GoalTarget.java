// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.goals;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

final class GoalTarget<T> {
    private final Supplier<T> findTarget;
    private final Predicate<T> isAvailable;

    private GoalTarget(Supplier<T> findTarget, Predicate<T> isAvailable) {
        this.findTarget = findTarget;
        this.isAvailable = isAvailable;
    }

    static GoalTarget<LivingEntity> track(Mob owner, LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            return trackPlayer(player.getUUID(), id -> findOnlinePlayer(owner, id),
                    candidate -> isAliveInOwnerLevel(owner, candidate));
        }
        WeakReference<LivingEntity> reference = new WeakReference<>(target);
        return new GoalTarget<>(reference::get, candidate -> isAliveInOwnerLevel(owner, candidate));
    }

    static <T> GoalTarget<T> trackPlayer(UUID playerId, Function<UUID, T> findOnlinePlayer, Predicate<T> isAvailable) {
        return new GoalTarget<>(() -> findOnlinePlayer.apply(playerId), isAvailable);
    }

    T resolve() {
        T target = findTarget.get();
        return target != null && isAvailable.test(target) ? target : null;
    }

    static boolean isCurrent(Mob owner, LivingEntity target) {
        return target != null && isAliveInOwnerLevel(owner, target)
                && (!(target instanceof ServerPlayer player) || findOnlinePlayer(owner, player.getUUID()) == player);
    }

    private static boolean isAliveInOwnerLevel(Mob owner, LivingEntity target) {
        return target.isAlive() && !target.isRemoved() && target.level() == owner.level();
    }

    private static ServerPlayer findOnlinePlayer(Mob owner, UUID playerId) {
        if (!(owner.level() instanceof ServerLevel level)) {
            return null;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
        return player != null && !player.hasDisconnected() ? player : null;
    }
}
