// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.chat;

import java.util.Locale;

public class MemoryEntry {
    public MemoryType type;
    public String text;
    public long createdAt;
    public long updatedAt;
    public int salience;

    public MemoryEntry(String text) {
        this(MemoryType.infer(text), text, System.currentTimeMillis(), System.currentTimeMillis(), 1);
    }

    public MemoryEntry(MemoryType type, String text, long createdAt, long updatedAt, int salience) {
        this.type = type == null ? MemoryType.FACT : type;
        this.text = text == null ? "" : text;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.salience = Math.max(1, salience);
    }

    public String normalizedKey() {
        return text.trim().toLowerCase(Locale.ENGLISH);
    }

    public String toPromptString() {
        return "[" + type.name().toLowerCase(Locale.ENGLISH) + "] " + text;
    }
}
