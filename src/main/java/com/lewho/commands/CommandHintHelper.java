// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.commands;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

/** Version adapter for the setup wizard's command suggestion components. */
public final class CommandHintHelper {
    private CommandHintHelper() {
    }

    public static MutableComponent hint(String label, String command) {
        return Component.literal(label + " ")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(command).withStyle(style -> style
                        .withColor(ChatFormatting.YELLOW)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Click to paste this command")))));
    }
}
