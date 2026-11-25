// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.goals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ProtectionThreatTests {
    private final ProtectPlayerGoal.ProtectionThreat<Actor> protection = new ProtectPlayerGoal.ProtectionThreat<>(
            actor -> actor.attackedAt, actor -> actor.attacker, actor -> actor.available);

    @Test
    void replacementBeneficiaryCanReportAFreshThreatAtTheSameAge() {
        Actor original = new Actor(1);
        Actor originalThreat = new Actor(2);
        original.receiveAttack(originalThreat, 7);

        Actor respawned = new Actor(original.id);
        Actor newThreat = new Actor(3);
        respawned.receiveAttack(newThreat, 7);

        assertSame(originalThreat, protection.resolve(original, null));
        assertSame(newThreat, protection.resolve(respawned, originalThreat));
    }

    @Test
    void replacementWithNoAttackDoesNotInheritThePreviousThreat() {
        Actor original = new Actor(1);
        Actor threat = new Actor(2);
        original.receiveAttack(threat, 7);

        assertSame(threat, protection.resolve(original, null));
        assertNull(protection.resolve(new Actor(original.id), threat));
    }

    @Test
    void unavailableBeneficiaryReleasesTheThreatAndReturnRequiresANewAttack() {
        Actor beneficiary = new Actor(1);
        Actor threat = new Actor(2);
        beneficiary.receiveAttack(threat, 7);

        assertSame(threat, protection.resolve(beneficiary, null));
        assertNull(protection.resolve(null, threat));
        assertNull(protection.resolve(beneficiary, null));

        beneficiary.receiveAttack(threat, 10);
        assertSame(threat, protection.resolve(beneficiary, null));
    }

    @Test
    void unavailableAttackerCannotRemainATarget() {
        Actor beneficiary = new Actor(1);
        Actor threat = new Actor(2);
        beneficiary.receiveAttack(threat, 7);
        assertSame(threat, protection.resolve(beneficiary, null));

        threat.available = false;
        assertNull(protection.resolve(beneficiary, threat));
    }

    private static final class Actor {
        private final int id;
        private int attackedAt;
        private Actor attacker;
        private boolean available = true;

        private Actor(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Actor actor && actor.id == id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        private void receiveAttack(Actor actor, int tick) {
            attacker = actor;
            attackedAt = tick;
        }
    }
}
