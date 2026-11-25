// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.chat;

public class AutoMessageBucket {
    private static final int MAX_TOKENS_CAP = 1000;
    private static final int MAX_COOLDOWN_SECONDS = 3600;

    private final int maxTokens;
    private final int cooldownSeconds;
    private int tokens;
    private long lastRefill;

    public AutoMessageBucket(int maxTokens, int cooldownSeconds) {
        this.maxTokens = Math.max(1, Math.min(maxTokens, MAX_TOKENS_CAP));
        this.cooldownSeconds = Math.max(1, Math.min(cooldownSeconds, MAX_COOLDOWN_SECONDS));
        this.tokens = this.maxTokens;
        this.lastRefill = System.currentTimeMillis();
    }

    public void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefill;
        int refill = (int) (elapsed / (cooldownSeconds * 1000L));
        if (refill > 0) {
            tokens = Math.min(maxTokens, tokens + refill);
            lastRefill += refill * cooldownSeconds * 1000L;
        }
    }

    public boolean hasTokens() {
        refill();
        return tokens > 0;
    }

    public void consume() {
        if (tokens > 0) {
            tokens--;
        }
    }

    public void reset() {
        tokens = maxTokens;
        lastRefill = System.currentTimeMillis();
    }
}

