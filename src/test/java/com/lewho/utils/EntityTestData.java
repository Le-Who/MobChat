// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lewho.chat.ChatDataManager;
import com.lewho.chat.ChatMessage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntityTestData {
    public String entityId;
    public String playerId;
    public String currentMessage;
    public int currentLineNumber;
    public ChatDataManager.ChatStatus status;
    public List<ChatMessage> previousMessages;
    public String characterSheet;
    public ChatDataManager.ChatSender sender;
    public int friendship;
    public int auto_generated;

    public EntityTestData(String entityId, String playerId) {
        this.entityId = entityId;
        this.playerId = playerId;
        this.currentMessage = "";
        this.currentLineNumber = 0;
        this.previousMessages = new ArrayList<>();
        this.characterSheet = "";
        this.status = ChatDataManager.ChatStatus.NONE;
        this.sender = ChatDataManager.ChatSender.USER;
        this.friendship = 0;
        this.auto_generated = 0;
    }

    public String getCharacterProp(String propertyName) {
        Pattern pattern = Pattern.compile("-?\\s*" + Pattern.quote(propertyName) + ":\\s*(.+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(characterSheet);

        if (matcher.find()) {
            return matcher.group(1).trim().replace("\"", "");
        }

        return "N/A";
    }

    public void addMessage(String message, ChatDataManager.ChatSender messageSender, String playerName) {
        String truncatedMessage = message.substring(0, Math.min(message.length(), ChatDataManager.MAX_CHAR_IN_USER_MESSAGE));

        previousMessages.add(new ChatMessage(truncatedMessage, messageSender, playerName));

        currentMessage = truncatedMessage;
        currentLineNumber = 0;
        if (messageSender == ChatDataManager.ChatSender.ASSISTANT) {
            status = ChatDataManager.ChatStatus.DISPLAY;
        } else if (messageSender == ChatDataManager.ChatSender.USER) {
            status = ChatDataManager.ChatStatus.PENDING;
        }
        sender = messageSender;
    }

    public Map<String, String> getPlayerContext(Path worldPath, Path playerPath, Path entityPath) {
        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> contextData = new HashMap<>();

        try {
            String worldContent = Files.readString(worldPath);
            Map<String, String> worldContext = gson.fromJson(worldContent, mapType);
            contextData.putAll(worldContext);

            String playerContent = Files.readString(playerPath);
            Map<String, String> playerContext = gson.fromJson(playerContent, mapType);
            contextData.putAll(playerContext);

            String entityContent = Files.readString(entityPath);
            Map<String, String> entityContext = gson.fromJson(entityContent, mapType);
            contextData.putAll(entityContext);

            contextData.put("entity_name", getCharacterProp("Name"));
            contextData.put("entity_friendship", String.valueOf(this.friendship));
            contextData.put("entity_personality", getCharacterProp("Personality"));
            contextData.put("entity_speaking_style", getCharacterProp("Speaking Style / Tone"));
            contextData.put("entity_likes", getCharacterProp("Likes"));
            contextData.put("entity_dislikes", getCharacterProp("Dislikes"));
            contextData.put("entity_age", getCharacterProp("Age"));
            contextData.put("entity_alignment", getCharacterProp("Alignment"));
            contextData.put("entity_class", getCharacterProp("Class"));
            contextData.put("entity_skills", getCharacterProp("Skills"));
            contextData.put("entity_background", getCharacterProp("Background"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return contextData;
    }
}