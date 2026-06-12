/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package com.morwapi.aivillager.action;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.morwapi.aivillager.AIVillagerMod;
import java.util.ArrayList;
import java.util.List;

public class ActionParser {
    private static final Gson gson = new Gson();

    public static AIResponse parse(String jsonString) {
        try {
            String cleanJson = jsonString;
            int startIndex = jsonString.indexOf("{");
            int endIndex = jsonString.lastIndexOf("}");
            if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
                throw new IllegalStateException("No JSON object found in response");
            }
            cleanJson = jsonString.substring(startIndex, endIndex + 1);
            AIVillagerMod.LOGGER.info("Extracted JSON: {}", (Object)cleanJson.replace("\n", " ").replace("\r", " "));
            JsonObject root = JsonParser.parseString((String)cleanJson).getAsJsonObject();
            AIResponse response = new AIResponse();
            if (root.has("thought")) {
                response.thought = root.get("thought").getAsString();
            }
            if (root.has("actions")) {
                JsonArray actionsArray = root.getAsJsonArray("actions");
                for (JsonElement element : actionsArray) {
                    VillagerAction action = (VillagerAction)gson.fromJson(element, VillagerAction.class);
                    response.actions.add(action);
                }
            }
            AIVillagerMod.LOGGER.info("Parsed {} actions.", (Object)response.actions.size());
            return response;
        }
        catch (Exception e) {
            AIVillagerMod.LOGGER.error("Failed to parse AI response as JSON: {}", (Object)e.getMessage());
            AIVillagerMod.LOGGER.debug("Raw response was: {}", (Object)jsonString);
            AIResponse response = new AIResponse();
            response.thought = "Failed to parse JSON: " + e.getMessage();
            VillagerAction chatAction = new VillagerAction();
            chatAction.type = "CHAT";
            chatAction.message = jsonString;
            response.actions.add(chatAction);
            return response;
        }
    }

    public static class AIResponse {
        public String thought;
        public List<VillagerAction> actions = new ArrayList<VillagerAction>();
    }

    public static class VillagerAction {
        public String type;
        public String message;
        public Double x;
        public Double y;
        public Double z;
        public double[] coordinates;
        public String blockId;
        public String itemName;
        public String targetName;
        public Integer count;
        public String memoryKey;
        public String memoryValue;
    }
}

