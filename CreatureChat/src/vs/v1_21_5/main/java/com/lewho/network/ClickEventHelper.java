// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.network;

import java.net.URI;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.ClickEvent.OpenUrl;

public class ClickEventHelper {
    /** new API: use the OpenUrl record */
    public static ClickEvent openUrl(String url) {
        return new OpenUrl(URI.create(url));
    }
}
