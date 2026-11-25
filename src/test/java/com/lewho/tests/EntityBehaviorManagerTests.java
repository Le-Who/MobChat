// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.goals.EntityBehaviorManager;
import com.lewho.goals.GoalPriority;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntityBehaviorManagerTests {
    @Test
    public void insertionMovesTiedGoalsAndStopsAtTheFirstUnusedPriority() throws ReflectiveOperationException {
        GoalSelector selector = newGoalSelector();
        Goal first = new InactiveGoal();
        Goal second = new InactiveGoal();
        Goal next = new InactiveGoal();
        Goal distant = new InactiveGoal();
        Goal earlier = new InactiveGoal();
        selector.addGoal(1, earlier);
        selector.addGoal(3, first);
        selector.addGoal(3, second);
        selector.addGoal(4, next);
        selector.addGoal(10, distant);

        EntityBehaviorManager.moveConflictingGoals(selector, GoalPriority.FOLLOW_PLAYER);

        assertEquals(Map.of(earlier, 1, first, 4, second, 4, next, 5, distant, 10), priorities(selector));
    }

    @Test
    public void insertionIntoAnUnusedPriorityLeavesOtherGoalsAlone() throws ReflectiveOperationException {
        GoalSelector selector = newGoalSelector();
        Goal earlier = new InactiveGoal();
        Goal later = new InactiveGoal();
        selector.addGoal(2, earlier);
        selector.addGoal(4, later);

        EntityBehaviorManager.moveConflictingGoals(selector, GoalPriority.FOLLOW_PLAYER);

        assertEquals(Map.of(earlier, 2, later, 4), priorities(selector));
    }

    private static Map<Goal, Integer> priorities(GoalSelector selector) {
        return selector.getAvailableGoals().stream()
                .collect(Collectors.toMap(WrappedGoal::getGoal, WrappedGoal::getPriority));
    }

    private static GoalSelector newGoalSelector() throws ReflectiveOperationException {
        try {
            return GoalSelector.class.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            return GoalSelector.class.getConstructor(Supplier.class).newInstance((Supplier<?>) () -> null);
        }
    }

    private static class InactiveGoal extends Goal {
        @Override
        public boolean canUse() {
            return false;
        }
    }
}
