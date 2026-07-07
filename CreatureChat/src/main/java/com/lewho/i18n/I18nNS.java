// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.i18n;

import static com.lewho.ModInit.MODID;

import net.minecraft.network.chat.Component;

/**
 * Namespace helper for translation keys.
 */
public final class I18nNS {
    private I18nNS() {}

    public static String k(String path) {
        return MODID + "." + path;
    }

    public static Component tr(String path, String en, Object... args) {
        return Component.translatableWithFallback(k(path), en, args);
    }
}
