// SPDX-FileCopyrightText: 2025 owlmaddie LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © owlmaddie LLC - unauthorized use prohibited
package com.owlmaddie.message;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code ParsedMessage} class represents a list of behaviors and a cleaned message.
 */
public class ParsedMessage {
    private String cleanedMessage;
    private String originalMessage;
    private List<Behavior> behaviors;
    private String mood;
    private List<String> memoryUpdates;

    public ParsedMessage(String cleanedMessage, String originalMessage, List<Behavior> behaviors) {
        this(cleanedMessage, originalMessage, behaviors, "", new ArrayList<>());
    }

    public ParsedMessage(String cleanedMessage, String originalMessage, List<Behavior> behaviors, String mood, List<String> memoryUpdates) {
        this.cleanedMessage = cleanedMessage;
        this.originalMessage = originalMessage;
        this.behaviors = behaviors;
        this.mood = mood == null ? "" : mood;
        this.memoryUpdates = memoryUpdates == null ? new ArrayList<>() : memoryUpdates;
    }

    // Get cleaned message (no behaviors)
    public String getCleanedMessage() {
        return cleanedMessage.trim();
    }

    // Get original message
    public String getOriginalMessage() {
        return originalMessage.trim();
    }

    public List<Behavior> getBehaviors() {
        return behaviors;
    }

    public String getMood() {
        return mood.trim();
    }

    public List<String> getMemoryUpdates() {
        return memoryUpdates;
    }
}
