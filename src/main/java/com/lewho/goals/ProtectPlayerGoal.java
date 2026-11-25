// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.goals;

import java.lang.ref.WeakReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class ProtectPlayerGoal extends AttackPlayerGoal {
    private final GoalTarget<LivingEntity> beneficiary;
    private final ProtectionThreat<LivingEntity> protectionThreat;

    public ProtectPlayerGoal(LivingEntity protectEntity, Mob attackerEntity, double speed) {
        super(null, attackerEntity, speed);
        this.beneficiary = GoalTarget.track(attackerEntity, protectEntity);
        this.protectionThreat = new ProtectionThreat<>(LivingEntity::getLastHurtByMobTimestamp,
                LivingEntity::getLastAttacker,
                attacker -> attacker != attackerEntity && GoalTarget.isCurrent(attackerEntity, attacker));
    }

    @Override
    protected LivingEntity resolveTarget() {
        return protectionThreat.resolve(beneficiary.resolve(), targetEntity);
    }

    static final class ProtectionThreat<T> {
        private final ToIntFunction<T> hurtTimestamp;
        private final Function<T, T> lastAttacker;
        private final Predicate<T> isEligible;
        private WeakReference<T> previousBeneficiary = new WeakReference<>(null);
        private int lastAttackedTime;

        ProtectionThreat(ToIntFunction<T> hurtTimestamp, Function<T, T> lastAttacker, Predicate<T> isEligible) {
            this.hurtTimestamp = hurtTimestamp;
            this.lastAttacker = lastAttacker;
            this.isEligible = isEligible;
        }

        T resolve(T beneficiary, T currentThreat) {
            if (beneficiary == null) {
                return null;
            }
            if (previousBeneficiary.get() != beneficiary) {
                previousBeneficiary = new WeakReference<>(beneficiary);
                lastAttackedTime = -1;
                currentThreat = null;
            }
            int attackedAt = hurtTimestamp.applyAsInt(beneficiary);
            if (attackedAt != lastAttackedTime) {
                T threat = lastAttacker.apply(beneficiary);
                if (threat != null) {
                    lastAttackedTime = attackedAt;
                    if (isEligible.test(threat)) {
                        return threat;
                    }
                }
            }
            return currentThreat != null && isEligible.test(currentThreat) ? currentThreat : null;
        }
    }
}
