// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.chat;

import com.lewho.message.Behavior;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BehaviorPolicy {
    private BehaviorPolicy() {
    }

    public static List<Behavior> filterAllowed(List<Behavior> behaviors, PlayerData playerData, boolean ambientContext, boolean hasHome) {
        List<Behavior> allowed = new ArrayList<>();
        if (behaviors == null || behaviors.isEmpty()) {
            return allowed;
        }
        if (ambientContext) {
            return allowed;
        }

        int effectiveFriendship = playerData == null ? 0 : playerData.friendship;
        for (Behavior behavior : behaviors) {
            if (behavior == null || behavior.getName() == null) {
                continue;
            }

            String name = behavior.getName().toUpperCase(Locale.ENGLISH);
            if ("FRIENDSHIP".equals(name)) {
                if (behavior.getArgument() == null) {
                    continue;
                }
                effectiveFriendship = clampFriendship(behavior.getArgument());
                allowed.add(behavior);
                continue;
            }

            if ("ATTACK".equals(name) && effectiveFriendship > 0) {
                continue;
            }

            if (("RETURN_HOME".equals(name) || "GUARD_HOME".equals(name)) && effectiveFriendship < 3 && !hasHome) {
                continue;
            }

            allowed.add(behavior);
        }
        return allowed;
    }

    private static int clampFriendship(int value) {
        return Math.max(-3, Math.min(3, value));
    }
}
