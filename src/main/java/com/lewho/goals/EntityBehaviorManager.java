// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.goals;

import com.lewho.network.ServerPackets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Predicate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

public class EntityBehaviorManager {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");

    public static void addGoal(Mob entity, Goal goal, GoalPriority priority) {
        if (!(entity.level() instanceof ServerLevel)) {
            LOGGER.debug("Attempted to add a goal in a non-server world. Aborting.");
            return;
        }

        ServerPackets.serverInstance.execute(() -> {
            GoalSelector goalSelector = GoalUtils.getGoalSelector(entity);

            clearAndRemove(g -> goal.getClass().equals(g.getClass()), goalSelector);

            moveConflictingGoals(goalSelector, priority);

            goalSelector.addGoal(priority.getPriority(), goal);
            LOGGER.debug("Goal of type {} added with priority {}", goal.getClass().getSimpleName(), priority);
        });
    }

    public static void clearAndRemove(Predicate<Goal> predicate, GoalSelector goalSelector) {
        List<WrappedGoal> toBeRemoved = goalSelector.getAvailableGoals().stream()
                .filter(prioritizedGoal -> predicate.test(prioritizedGoal.getGoal()))
                .collect(Collectors.toList());

        toBeRemoved.forEach(prioritizedGoal -> {
            goalSelector.removeGoal(prioritizedGoal.getGoal());
        });
    }

    public static void removeGoal(Mob entity, Class<? extends Goal> goalClass) {
        ServerPackets.serverInstance.execute(() -> {
            GoalSelector goalSelector = GoalUtils.getGoalSelector(entity);
            clearAndRemove(g -> goalClass.equals(g.getClass()), goalSelector);
            LOGGER.debug("All goals of type {} removed.", goalClass.getSimpleName());
        });
    }

    public static void moveConflictingGoals(GoalSelector goalSelector, GoalPriority newGoalPriority) {
        List<WrappedGoal> goals = List.copyOf(goalSelector.getAvailableGoals());
        Set<Integer> occupiedPriorities = goals.stream()
                .map(WrappedGoal::getPriority)
                .collect(Collectors.toSet());
        int requestedPriority = newGoalPriority.getPriority();
        int freePriority = requestedPriority;
        while (occupiedPriorities.contains(freePriority)) {
            freePriority++;
        }
        for (WrappedGoal wrappedGoal : goals) {
            int priority = wrappedGoal.getPriority();
            if (priority >= requestedPriority && priority < freePriority) {
                goalSelector.removeGoal(wrappedGoal.getGoal());
                goalSelector.addGoal(priority + 1, wrappedGoal.getGoal());
            }
        }
    }
}
