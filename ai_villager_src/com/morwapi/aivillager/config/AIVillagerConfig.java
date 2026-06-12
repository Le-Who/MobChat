/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.shedaniel.autoconfig.ConfigData
 *  me.shedaniel.autoconfig.annotation.Config
 */
package com.morwapi.aivillager.config;

import java.util.ArrayList;
import java.util.List;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name="morwapi_ai_villager")
public class AIVillagerConfig
implements ConfigData {
    public String apiUrl = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions";
    public String apiKey = "";
    public String modelName = "gemini-2.5-flash";
    public List<String> personality = new ArrayList<String>(List.of("You are a helpful and friendly Minecraft Villager.", "You speak in Japanese (Kansai dialect).", "Act naturally and do not mention that you are an AI."));
    public boolean enableAI = true;
    public int maxAutoSteps = 5;
    public boolean autoSwitchModel = true;
    public List<String> fallbackModels = new ArrayList<String>(List.of("gemini-2.5-flash-lite", "gemini-2.0-flash", "gemini-2.0-flash-lite"));
    public boolean hasShownWelcomeMessage = false;
}

