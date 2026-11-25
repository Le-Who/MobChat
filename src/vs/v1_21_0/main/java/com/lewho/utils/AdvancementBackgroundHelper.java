// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.utils;

import net.minecraft.resources.ResourceLocation;

public final class AdvancementBackgroundHelper {
    private AdvancementBackgroundHelper() {}

    public static ResourceLocation ui(String name) {
        return new ResourceLocation("creaturechat", "ui/" + name);
    }

    public static ResourceLocation prependTextures(ResourceLocation base) {
        return base;
    }
}
