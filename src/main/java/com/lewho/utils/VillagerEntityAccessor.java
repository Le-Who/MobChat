// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.utils;

import net.minecraft.world.entity.ai.gossip.GossipContainer;

/**
 * The {@code VillagerEntityAccessor} interface provides a method to access
 * the gossip system of a villager. It enables interaction with a villager's
 * gossip data for custom behavior or modifications.
 */
public interface VillagerEntityAccessor {
    GossipContainer getGossip();
}
