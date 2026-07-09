// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.chat;

import com.lewho.utils.VillagerEntityAccessor;
import java.util.UUID;
import net.minecraft.world.entity.ai.gossip.GossipType;

/**
 * Override for 1.21.5 — uses the renamed VillagerGossipType.
 */
public class GossipTypeHelper {
    public static final GossipType MAJOR_POSITIVE   = GossipType.MAJOR_POSITIVE;
    public static final GossipType MINOR_POSITIVE   = GossipType.MINOR_POSITIVE;
    public static final GossipType MINOR_NEGATIVE   = GossipType.MINOR_NEGATIVE;
    public static final GossipType MAJOR_NEGATIVE   = GossipType.MAJOR_NEGATIVE;

    public static void startGossip(VillagerEntityAccessor villager, UUID playerId,
                                   GossipType type, int amount) {
        villager.getGossip().add(playerId, type, amount);
    }
}
