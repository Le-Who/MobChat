// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.chat;

/**
 * The {@code PlayerData} class represents data associated with a player,
 * specifically tracking their friendship level.
 */
public class PlayerData {
    private static final int MAX_SOCIAL_SUMMARY_CHARS = 180;

    public int friendship;
    public int lastDamageFriendship;
    public int signFlipCount;
    public int lastSign;
    public boolean seenHigh;
    public boolean seenLow;
    public boolean reachedPos3;
    public boolean reachedNeg3;
    public boolean attacking;
    public boolean fleeing;
    public boolean gaveItem;
    public boolean pigProtect;
    public int conversationCount;
    public boolean droppedBelowZero;
    public boolean openedInventory;
    public boolean wordsmithActive;
    public boolean wordsmithOpenedInventory;
    public boolean wordsmithGaveItem;
    public boolean wordsmithDamaged;
    public int messageCount;
    public boolean wasInOverworld;
    public int socialReputation;
    public int helpfulActions;
    public int harmfulActions;
    public int socialEventCount;
    public String socialSummary;
    public long lastDamageReactionAt;
    public int suppressedDamageReactionCount;

    public PlayerData() {
        this.friendship = 0;
        this.lastDamageFriendship = Integer.MIN_VALUE;
        this.signFlipCount = 0;
        this.lastSign = 0;
        this.seenHigh = false;
        this.seenLow = false;
        this.reachedPos3 = false;
        this.reachedNeg3 = false;
        this.attacking = false;
        this.fleeing = false;
        this.gaveItem = false;
        this.pigProtect = false;
        this.conversationCount = 0;
        this.droppedBelowZero = false;
        this.openedInventory = false;
        this.wordsmithActive = false;
        this.wordsmithOpenedInventory = false;
        this.wordsmithGaveItem = false;
        this.wordsmithDamaged = false;
        this.messageCount = 0;
        this.wasInOverworld = false;
        this.socialReputation = 0;
        this.helpfulActions = 0;
        this.harmfulActions = 0;
        this.socialEventCount = 0;
        this.socialSummary = "";
        this.lastDamageReactionAt = 0L;
        this.suppressedDamageReactionCount = 0;
    }

    public void recordSocialEvent(SocialEventType type, String summary) {
        if (type == null) {
            return;
        }
        int delta = type.getReputationDelta();
        this.socialReputation += delta;
        if (delta > 0) {
            this.helpfulActions++;
        } else if (delta < 0) {
            this.harmfulActions++;
        }
        this.socialEventCount++;
        if (summary != null && !summary.trim().isEmpty()) {
            this.socialSummary = truncate(summary.trim().replace("\n", " "));
        }
    }

    public void recordFriendshipShift(int oldFriendship, int newFriendship) {
        int delta = newFriendship - oldFriendship;
        if (delta == 0) {
            return;
        }
        this.socialReputation += delta;
        if (delta > 0) {
            this.helpfulActions++;
            this.socialSummary = "Friendship improved from " + oldFriendship + " to " + newFriendship + ".";
        } else {
            this.harmfulActions++;
            this.socialSummary = "Friendship dropped from " + oldFriendship + " to " + newFriendship + ".";
        }
        this.socialEventCount++;
    }

    public String consumeSuppressedDamageReactionSummary() {
        if (suppressedDamageReactionCount <= 0) {
            return "";
        }
        int count = suppressedDamageReactionCount;
        suppressedDamageReactionCount = 0;
        return count == 1
                ? "There was 1 additional hit during the cooldown."
                : "There were " + count + " additional hits during the cooldown.";
    }

    public void resetDamageReactionCooldown() {
        lastDamageReactionAt = 0L;
        suppressedDamageReactionCount = 0;
    }

    private static String truncate(String value) {
        return value.length() > MAX_SOCIAL_SUMMARY_CHARS
                ? value.substring(0, MAX_SOCIAL_SUMMARY_CHARS - 3) + "..."
                : value;
    }
}
