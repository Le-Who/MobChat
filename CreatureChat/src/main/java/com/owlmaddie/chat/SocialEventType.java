// SPDX-FileCopyrightText: 2026 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.owlmaddie.chat;

public enum SocialEventType {
    CHAT_EXCHANGE(0),
    GIFT_GIVEN(1),
    ITEM_TAKEN(-1),
    DAMAGE_DEALT(-1),
    FRIENDSHIP_GAIN(1),
    FRIENDSHIP_LOSS(-1),
    RUMOR_HEARD(0);

    private final int reputationDelta;

    SocialEventType(int reputationDelta) {
        this.reputationDelta = reputationDelta;
    }

    public int getReputationDelta() {
        return reputationDelta;
    }
}
