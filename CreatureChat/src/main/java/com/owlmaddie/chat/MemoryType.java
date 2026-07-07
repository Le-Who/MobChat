// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.chat;

import java.util.Locale;

public enum MemoryType {
    FACT,
    GIFT,
    PROMISE,
    REQUEST,
    CONFLICT,
    RUMOR;

    public static MemoryType infer(String text) {
        if (text == null) {
            return FACT;
        }
        String normalized = text.toLowerCase(Locale.ENGLISH);
        if (normalized.contains("rumor") || normalized.contains("heard that") || normalized.contains("said nearby")) {
            return RUMOR;
        }
        if (normalized.contains("promised") || normalized.contains("promise")) {
            return PROMISE;
        }
        if (normalized.contains("gave") || normalized.contains("gift") || normalized.contains("present")) {
            return GIFT;
        }
        if (normalized.contains("asked") || normalized.contains("requested") || normalized.contains("guard") || normalized.contains("follow")) {
            return REQUEST;
        }
        if (normalized.contains("attacked") || normalized.contains("hit") || normalized.contains("hate") || normalized.contains("threat")) {
            return CONFLICT;
        }
        return FACT;
    }
}
