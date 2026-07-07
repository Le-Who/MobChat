// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.chat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.lewho.commands.ConfigurationHandler;
import com.lewho.commands.CustomRoleHandler;
import com.lewho.controls.SpeedControls;
import com.lewho.goals.*;
import com.lewho.message.Behavior;
import com.lewho.message.MessageParser;
import com.lewho.message.ParsedMessage;
import com.lewho.network.ServerPackets;
import com.lewho.i18n.TR;
import com.lewho.particle.ParticleEmitter;
import com.lewho.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;

import static com.lewho.network.ServerPackets.*;

/**
 * The {@code EntityChatData} class represents a conversation between an
 * entity and one or more players, including friendship, character sheets,
 * and the status of the current displayed message.
 */
public class EntityChatData {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    public static final int MAX_MEMORY_ENTRIES = 20;
    public static final int MAX_MEMORY_CHARS = 180;
    public static final TR INFO_HELP_LINK = new TR("info.help_link", "Help: check %s");
    public static final TR ERROR_PREFIX = new TR("error.prefix", "Error: ");
    public static final List<TR> ERROR_MISC = List.of(
            INFO_HELP_LINK,
            ERROR_PREFIX
    );

    public static final TR SOLUTION_CONNECTION = new TR("solution.connection", "Solution: Check internet connection or firewall");
    public static final TR SOLUTION_VERIFY_URL = new TR("solution.verify_url", "Solution: Verify the API URL");
    public static final TR SOLUTION_ADD_KEY = new TR("solution.add_key", "Solution: Add a valid API key");
    public static final TR SOLUTION_CHECK_REGION = new TR("solution.check_region", "Solution: Check region or VPN");
    public static final TR SOLUTION_ADD_FUNDS = new TR("solution.add_funds", "Solution: Add funds to your account");
    public static final TR SOLUTION_SERVER_ERROR = new TR("solution.server_error", "Solution: Server error, try again later");
    public static final TR SOLUTION_TRY_AGAIN = new TR("solution.try_again", "Solution: Try again later");
    public static final List<TR> ERROR_SOLUTIONS = List.of(
            SOLUTION_CONNECTION,
            SOLUTION_VERIFY_URL,
            SOLUTION_ADD_KEY,
            SOLUTION_CHECK_REGION,
            SOLUTION_ADD_FUNDS,
            SOLUTION_SERVER_ERROR,
            SOLUTION_TRY_AGAIN
    );
    public String entityId;
    public String currentMessage;
    public int currentLineNumber;
    public ChatDataManager.ChatStatus status;
    public String characterSheet;
    public ChatDataManager.ChatSender sender;
    public int auto_generated;
    public List<ChatMessage> previousMessages;
    public String mood;
    public List<String> memories;
    public List<MemoryEntry> memoryEntries;
    public String homeDimension;
    public int homeX;
    public int homeY;
    public int homeZ;
    public boolean guardingHome;
    public Long born;
    public Long death;
    public transient AutoMessageBucket autoBucket;
    public transient AutoMessageBucket ambientBucket;

    @SerializedName("playerId")
    @Expose(serialize = false)
    private String legacyPlayerId;

    @SerializedName("friendship")
    @Expose(serialize = false)
    public Integer legacyFriendship;

    // The map to store data for each player interacting with this entity
    public Map<String, PlayerData> players;

    public EntityChatData(String entityId) {
        this.entityId = entityId;
        this.players = new HashMap<>();
        this.currentMessage = "";
        this.currentLineNumber = 0;
        this.characterSheet = "";
        this.status = ChatDataManager.ChatStatus.NONE;
        this.sender = ChatDataManager.ChatSender.USER;
        this.auto_generated = 0;
        this.previousMessages = new ArrayList<>();
        this.mood = "";
        this.memories = new ArrayList<>();
        this.memoryEntries = new ArrayList<>();
        this.homeDimension = "";
        this.homeX = 0;
        this.homeY = 0;
        this.homeZ = 0;
        this.guardingHome = false;
        this.born = System.currentTimeMillis();;
        this.autoBucket = null;
        this.ambientBucket = null;

        // Old, unused migrated properties
        this.legacyPlayerId = null;
        this.legacyFriendship = null;
    }

    // Post-deserialization initialization
    public void postDeserializeInitialization() {
        if (this.players == null) {
            this.players = new HashMap<>(); // Ensure players map is initialized
        }
        if (this.memories == null) {
            this.memories = new ArrayList<>();
        }
        if (this.memoryEntries == null) {
            this.memoryEntries = new ArrayList<>();
        }
        if (this.memoryEntries.isEmpty() && !this.memories.isEmpty()) {
            for (String memory : this.memories) {
                addMemoryEntry(memory);
            }
            syncLegacyMemories();
        } else {
            syncLegacyMemories();
        }
        if (this.mood == null) {
            this.mood = "";
        }
        if (this.homeDimension == null) {
            this.homeDimension = "";
        }
        if (this.legacyPlayerId != null && !this.legacyPlayerId.isEmpty()) {
            this.migrateData();
        }
    }

    public boolean hasHome() {
        return this.homeDimension != null && !this.homeDimension.isEmpty();
    }

    public void setHome(String dimension, int x, int y, int z) {
        this.homeDimension = dimension == null ? "" : dimension;
        this.homeX = x;
        this.homeY = y;
        this.homeZ = z;
    }

    public void clearHome() {
        this.homeDimension = "";
        this.homeX = 0;
        this.homeY = 0;
        this.homeZ = 0;
        this.guardingHome = false;
    }

    public BlockPos getHomeBlockPos() {
        return new BlockPos(this.homeX, this.homeY, this.homeZ);
    }

    private boolean ensureBestFriendHome(ServerPlayer player, PlayerData playerData) {
        if (!hasHome()) {
            if (playerData.friendship < 3) {
                LOGGER.info("Home action ignored for entity {} because player {} is not a best friend", entityId, player.getDisplayName().getString());
                return false;
            }

            BlockPos spawnPos = player.getRespawnPosition();
            ResourceKey<Level> spawnDimension = player.getRespawnDimension();
            if (spawnPos == null || spawnDimension == null) {
                spawnPos = serverInstance.overworld().getSharedSpawnPos();
                spawnDimension = Level.OVERWORLD;
            }
            setHome(spawnDimension.location().toString(), spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        }
        return true;
    }

    private boolean isHomeInCurrentDimension(Mob entity) {
        return hasHome() && entity.level().dimension().location().toString().equals(this.homeDimension);
    }

    public void applyStructuredMetadata(ParsedMessage parsedMessage) {
        if (parsedMessage == null) {
            return;
        }
        String parsedMood = parsedMessage.getMood();
        if (!parsedMood.isEmpty()) {
            this.mood = truncateString(parsedMood, MAX_MEMORY_CHARS);
        }
        if (this.memories == null) {
            this.memories = new ArrayList<>();
        }
        for (String memory : parsedMessage.getMemoryUpdates()) {
            if (memory == null || memory.trim().isEmpty()) {
                continue;
            }
            String cleanedMemory = truncateString(memory.trim().replace("\n", " "), MAX_MEMORY_CHARS);
            addMemoryEntry(cleanedMemory);
        }
        syncLegacyMemories();
    }

    private void addMemoryEntry(String memory) {
        if (memory == null || memory.trim().isEmpty()) {
            return;
        }
        if (this.memoryEntries == null) {
            this.memoryEntries = new ArrayList<>();
        }
        MemoryEntry newEntry = new MemoryEntry(truncateString(memory.trim().replace("\n", " "), MAX_MEMORY_CHARS));
        this.memoryEntries.removeIf(entry -> entry != null && entry.normalizedKey().equals(newEntry.normalizedKey()));
        this.memoryEntries.add(newEntry);
        while (this.memoryEntries.size() > MAX_MEMORY_ENTRIES) {
            this.memoryEntries.remove(0);
        }
    }

    private void syncLegacyMemories() {
        if (this.memories == null) {
            this.memories = new ArrayList<>();
        }
        this.memories.clear();
        if (this.memoryEntries == null) {
            return;
        }
        for (MemoryEntry entry : this.memoryEntries) {
            if (entry != null && entry.text != null && !entry.text.isEmpty()) {
                this.memories.add(entry.text);
            }
        }
    }

    public void rememberRumor(String rumor) {
        if (rumor == null || rumor.trim().isEmpty()) {
            return;
        }
        addMemoryEntry("Rumor: " + truncateString(rumor.trim().replace("\n", " "), MAX_MEMORY_CHARS));
        syncLegacyMemories();
    }

    // Migrate old data into the new structure
    private void migrateData() {
        // Ensure the blank player data entry exists
        PlayerData blankPlayerData = this.players.computeIfAbsent("", k -> new PlayerData());

        // Update the previousMessages arraylist and add timestamps if missing
        if (this.previousMessages != null) {
            for (ChatMessage message : this.previousMessages) {
                if (message.timestamp == null) {
                    message.timestamp = System.currentTimeMillis();
                }
                if (message.name == null || message.name.isEmpty()) {
                    message.name = "";
                }
            }
        }
        blankPlayerData.friendship = this.legacyFriendship;
        if (this.born == null) {
            this.born = System.currentTimeMillis();;
        }

        // Clean up old player data
        this.legacyPlayerId = null;
        this.legacyFriendship = null;
    }

    public static String getPlayerKey(ServerPlayer player) {
        return player == null ? "" : player.getStringUUID();
    }

    public static String describeMaturity(boolean isBaby) {
        return isBaby ? "Baby" : "Adult";
    }

    private static String getEntityMaturity(Mob entity) {
        return describeMaturity(entity instanceof AgeableMob ageable && ageable.isBaby());
    }

    public PlayerData getPlayerData(ServerPlayer player) {
        return getPlayerData(getPlayerKey(player), player == null ? "" : player.getDisplayName().getString());
    }

    // Get the player data (or fallback to the blank player)
    public PlayerData getPlayerData(String playerName) {
        return getPlayerData(playerName, "");
    }

    public PlayerData getPlayerData(String playerKey, String legacyPlayerName) {
        if (this.players == null) {
            this.players = new HashMap<>();
        }

        String normalizedKey = playerKey == null ? "" : playerKey;
        String normalizedLegacyName = legacyPlayerName == null ? "" : legacyPlayerName;

        if (!normalizedKey.isEmpty() && this.players.containsKey(normalizedKey)) {
            return this.players.get(normalizedKey);
        }

        if (!normalizedKey.isEmpty() && !normalizedLegacyName.isEmpty() && this.players.containsKey(normalizedLegacyName)) {
            PlayerData migrated = this.players.remove(normalizedLegacyName);
            this.players.put(normalizedKey, migrated);
            return migrated;
        }

        if (this.players.containsKey("")) {
            // Legacy single-player save data uses a blank key; keep it stable until an explicit UUID entry exists.
            return this.players.get("");
        }

        String key = !normalizedKey.isEmpty() ? normalizedKey : normalizedLegacyName;
        PlayerData newPlayerData = new PlayerData();
        this.players.put(key, newPlayerData);
        return newPlayerData;
    }

    // Generate light version of chat data (no previous messages)
    public EntityChatDataLight toLightVersion(String playerName) {
        return new EntityChatDataLight(this, playerName);
    }

    public String getCharacterProp(String propertyName) {
        // Create a case-insensitive regex pattern to match the property name and capture its value
        Pattern pattern = Pattern.compile("-?\\s*" + Pattern.quote(propertyName) + ":\\s*(.+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(characterSheet);

        if (matcher.find()) {
            // Return the captured value, trimmed of any excess whitespace
            return matcher.group(1).trim().replace("\"", "");
        }

        return "N/A";
    }

    public String getShortGreetingOrFallback(String fallback) {
        String shortGreeting = firstUsableCharacterProp("Short Greeting", "Greeting");
        if (!shortGreeting.isEmpty()) {
            return shortGreeting.replace("\n", " ");
        }
        return fallback == null ? "" : fallback.replace("\n", " ");
    }

    private String firstUsableCharacterProp(String... propertyNames) {
        for (String propertyName : propertyNames) {
            String value = getCharacterProp(propertyName);
            if (isUsableCharacterProp(value)) {
                return value;
            }
        }
        return "";
    }

    private static boolean isUsableCharacterProp(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return isSpokenReplyCandidate(trimmed) && !"N/A".equalsIgnoreCase(trimmed);
    }

    public static boolean isSpokenReplyCandidate(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        return !trimmed.matches("^<[^<>]{1,80}>$");
    }

    public static String getSpokenFallbackMessage(String userLanguage) {
        String normalized = userLanguage == null ? "" : userLanguage.toLowerCase(Locale.ROOT);
        if (normalized.contains("рус") || normalized.contains("russian")) {
            return "Я задумался на миг. Скажи еще раз?";
        }
        return "I lost my words for a moment. Say that again?";
    }

    // Get list of status effects for player (handle different Minecraft versions)
    private static MobEffect effectOf(MobEffectInstance inst) {
        Object raw = inst.getEffect();        // 1.20.4: StatusEffect
        if (raw instanceof MobEffect se) {     // J 17-compatible pattern match
            return se;
        }
        return ((Holder<MobEffect>) raw).value();
    }

    // Generate context object
    public Map<String, String> getPlayerContext(ServerPlayer player, String userLanguage, ConfigurationHandler.Config config) {
        // Add PLAYER context information
        Map<String, String> contextData = new HashMap<>();
        contextData.put("player_name", player.getDisplayName().getString());
        contextData.put("player_health", Math.round(player.getHealth()) + "/" + Math.round(player.getMaxHealth()));
        contextData.put("player_hunger", String.valueOf(player.getFoodData().getFoodLevel()));
        contextData.put("player_held_item", String.valueOf(player.getMainHandItem().getItem().toString()));
        contextData.put("player_biome", player.level().getBiome(player.blockPosition()).unwrapKey().get().location().getPath());
        contextData.put("player_is_creative", player.isCreative() ? "yes" : "no");
        contextData.put("player_is_swimming", player.isSwimming() ? "yes" : "no");
        contextData.put("player_is_on_ground", player.onGround() ? "yes" : "no");
        contextData.put("player_language", userLanguage);

        ItemStack headArmor = ArmorHelper.getArmor(player, EquipmentSlot.HEAD);
        ItemStack chestArmor = ArmorHelper.getArmor(player, EquipmentSlot.CHEST);
        ItemStack legsArmor = ArmorHelper.getArmor(player, EquipmentSlot.LEGS);
        ItemStack feetArmor = ArmorHelper.getArmor(player, EquipmentSlot.FEET);
        contextData.put("player_armor_head", headArmor.getItem().toString());
        contextData.put("player_armor_chest", chestArmor.getItem().toString());
        contextData.put("player_armor_legs", legsArmor.getItem().toString());
        contextData.put("player_armor_feet", feetArmor.getItem().toString());

        // Get active player effects
        String effectsString = player.getActiveEffectsMap().values().stream()
            .map(inst -> effectOf(inst).getDescriptionId() + " x" + (inst.getAmplifier() + 1))
            .collect(Collectors.joining(", "));
        contextData.put("player_active_effects", effectsString);

        // Add custom story section (if any)
        if (!config.getStory().isEmpty()) {
            contextData.put("story", "Story: " + config.getStory());
        } else {
            contextData.put("story", "");
        }


        // Get World time (as 24 hour value)
        int hours = (int) ((player.level().getDayTime() / 1000 + 6) % 24); // Minecraft day starts at 6 AM
        int minutes = (int) (((player.level().getDayTime() % 1000) / 1000.0) * 60);
        contextData.put("world_time", String.format("%02d:%02d", hours, minutes));
        contextData.put("world_is_raining", player.level().isRaining() ? "yes" : "no");
        contextData.put("world_is_thundering", player.level().isThundering() ? "yes" : "no");
        contextData.put("world_difficulty", player.level().getDifficulty().getKey());
        contextData.put("world_is_hardcore", player.level().getLevelData().isHardcore() ? "yes" : "no");

        // Get moon phase
        String moonPhaseDescription = switch (player.level().getMoonPhase()) {
            case 0 -> "Full Moon";
            case 1 -> "Waning Gibbous";
            case 2 -> "Last Quarter";
            case 3 -> "Waning Crescent";
            case 4 -> "New Moon";
            case 5 -> "Waxing Crescent";
            case 6 -> "First Quarter";
            case 7 -> "Waxing Gibbous";
            default -> "Unknown";
        };
        contextData.put("world_moon_phase", moonPhaseDescription);

        // Get Entity details
        Mob entity = (Mob) ServerEntityFinder.getEntityByUUID((ServerLevel)player.level(), UUID.fromString(entityId));
        if (entity.getCustomName() == null) {
            contextData.put("entity_name", "");
        } else {
            contextData.put("entity_name", entity.getCustomName().getString());
        }
        contextData.put("entity_type", entity.getType().getDescription().getString());
        CustomRoleHandler customRoleHandler = new CustomRoleHandler(ServerPackets.serverInstance);
        Map<String, String> customRoles = customRoleHandler.loadRoles();
        String customRole = customRoles.getOrDefault(contextData.get("entity_type"), "");
        if (!customRole.isEmpty()) {
            contextData.put("custom_role", "Role Description: " + customRole);
        } else {
            contextData.put("custom_role", "");
        }
        contextData.put("entity_health", Math.round(entity.getHealth()) + "/" + Math.round(entity.getMaxHealth()));
        contextData.put("entity_personality", getCharacterProp("Personality"));
        contextData.put("entity_speaking_style", getCharacterProp("Speaking Style / Tone"));
        contextData.put("entity_likes", getCharacterProp("Likes"));
        contextData.put("entity_dislikes", getCharacterProp("Dislikes"));
        contextData.put("entity_age", getCharacterProp("Age"));
        contextData.put("entity_alignment", getCharacterProp("Alignment"));
        contextData.put("entity_class", getCharacterProp("Class"));
        contextData.put("entity_skills", getCharacterProp("Skills"));
        contextData.put("entity_background", getCharacterProp("Background"));
        contextData.put("entity_mood", this.mood == null || this.mood.isEmpty() ? "neutral" : this.mood);
        if (this.memoryEntries == null || this.memoryEntries.isEmpty()) {
            contextData.put("entity_memories", "none");
        } else {
            contextData.put("entity_memories", this.memoryEntries.stream()
                    .filter(entry -> entry != null && entry.text != null && !entry.text.isEmpty())
                    .map(MemoryEntry::toPromptString)
                    .collect(Collectors.joining("; ")));
        }
        contextData.put("entity_maturity", getEntityMaturity(entity));

        PlayerData playerData = this.getPlayerData(player);
        if (playerData != null) {
            contextData.put("entity_friendship", String.valueOf(playerData.friendship));
            contextData.put("player_social_reputation", String.valueOf(playerData.socialReputation));
            contextData.put("player_social_summary", playerData.socialSummary == null || playerData.socialSummary.isEmpty() ? "none" : playerData.socialSummary);
        } else {
            contextData.put("entity_friendship", String.valueOf(0));
            contextData.put("player_social_reputation", "0");
            contextData.put("player_social_summary", "none");
        }

        return contextData;
    }

    // Generate a new character
    public void generateCharacter(String userLanguage, ServerPlayer player, String userMessage, boolean is_auto_message) {
        String systemPrompt = "system-character";
        if (is_auto_message) {
            // Increment an auto-generated message
            this.auto_generated++;
        } else {
            // Reset auto-generated counter
            this.auto_generated = 0;
        }

        // Add USER Message
        this.addMessage(userMessage, ChatDataManager.ChatSender.USER, player, systemPrompt);

        // Get config (api key, url, settings)
        ConfigurationHandler.Config config = new ConfigurationHandler(ServerPackets.serverInstance).loadConfig();
        String promptText = ChatPrompt.loadPromptFromResource(ServerPackets.serverInstance.getResourceManager(), systemPrompt);

        // Add PLAYER context information
        Map<String, String> contextData = getPlayerContext(player, userLanguage, config);

        // fetch HTTP response from ChatGPT
        ChatGPTRequest.fetchMessageFromChatGPT(config, promptText, contextData, previousMessages, ChatGPTRequest.StructuredOutputMode.CHARACTER).thenAccept(output_message -> {
            try {
                if (output_message != null) {
                    // Character Sheet: Remove system-character message from previous messages
                    previousMessages.clear();

                    // Add NEW CHARACTER sheet & greeting
                    this.characterSheet = CharacterSheetNormalizer.normalize(output_message);
                    String shortGreeting = getShortGreetingOrFallback(getSpokenFallbackMessage(userLanguage));
                    this.addMessage(shortGreeting, ChatDataManager.ChatSender.ASSISTANT, player, systemPrompt);

                } else {
                    // No valid LLM response
                    throw new RuntimeException(ChatGPTRequest.lastErrorMessage);
                }

            } catch (Exception e) {
                // Log the exception for debugging
                LOGGER.error("Error processing LLM response", e);

                Randomizer.ErrorType type = Randomizer.ErrorType.GENERAL;
                int code = ChatGPTRequest.lastErrorCode;
                if (code == -1) {
                    type = Randomizer.ErrorType.CONNECTION;
                } else if (code == 401 || (config.getApiKey() == null || config.getApiKey().isEmpty())) {
                    type = Randomizer.ErrorType.CODE401;
                } else if (code == 403) {
                    type = Randomizer.ErrorType.CODE403;
                } else if (code == 429) {
                    type = Randomizer.ErrorType.CODE429;
                } else if (code == 500) {
                    type = Randomizer.ErrorType.CODE500;
                } else if (code == 503) {
                    type = Randomizer.ErrorType.CODE503;
                }
                Component helpTarget = Component.literal(Randomizer.HELP_TARGET)
                        .withStyle(ChatFormatting.BLUE);

                TR randomError = Randomizer.getRandomError(type);
                MutableComponent randomComp = randomError.comp(helpTarget);
                this.addMessage(randomComp.getString(), ChatDataManager.ChatSender.ASSISTANT, player, systemPrompt, false);

                MutableComponent errorComp = ERROR_PREFIX.comp();
                if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                    errorComp.append(Component.literal(truncateString(e.getMessage(), 50)));
                }
                player.displayClientMessage(errorComp.withStyle(ChatFormatting.RED), false);

                // Remove the error message from history to prevent it from affecting future ChatGPT requests
                if (!previousMessages.isEmpty()) {
                    previousMessages.remove(previousMessages.size() - 1);
                }

                TR solution = getSolutionMessage(code);
                if (solution != null) {
                    player.displayClientMessage(solution.comp().withStyle(ChatFormatting.BLUE), false);
                }

                player.displayClientMessage(INFO_HELP_LINK.comp(helpTarget), false);
            }
        });
    }

    // Generate greeting
    public void generateMessage(String userLanguage, ServerPlayer player, String userMessage, boolean is_auto_message) {
        generateMessage(userLanguage, player, userMessage, is_auto_message, !is_auto_message);
    }

    public void generateMessage(String userLanguage, ServerPlayer player, String userMessage, boolean is_auto_message, boolean allow_mob_to_mob_reactions) {
        String systemPrompt = "system-chat";
        if (is_auto_message) {
            // Increment an auto-generated message
            this.auto_generated++;
        } else {
            // Reset auto-generated counter
            this.auto_generated = 0;
        }

        // Add USER Message
        this.addMessage(userMessage, ChatDataManager.ChatSender.USER, player, systemPrompt);

        // Get config (api key, url, settings)
        ConfigurationHandler.Config config = new ConfigurationHandler(ServerPackets.serverInstance).loadConfig();
        String promptText = ChatPrompt.loadPromptFromResource(ServerPackets.serverInstance.getResourceManager(), systemPrompt);

        // Add PLAYER context information
        Map<String, String> contextData = getPlayerContext(player, userLanguage, config);

        // Get messages for player
        PlayerData playerData = this.getPlayerData(player);
        if (previousMessages.size() == 1) {
            // No messages exist yet for this player (start with normal greeting)
            String shortGreeting = getShortGreetingOrFallback(getSpokenFallbackMessage(userLanguage));
            previousMessages.add(0, new ChatMessage(shortGreeting, ChatDataManager.ChatSender.ASSISTANT, player.getDisplayName().getString()));
        }

        // fetch HTTP response from ChatGPT
        ChatGPTRequest.fetchMessageFromChatGPT(config, promptText, contextData, previousMessages, true).thenAccept(output_message -> {
            try {
                if (output_message != null) {
                    // Chat Message: Parse message for behaviors
                    ParsedMessage result = MessageParser.parseMessage(output_message.replace("\n", " "));
                    this.applyStructuredMetadata(result);
                    Mob entity = (Mob) ServerEntityFinder.getEntityByUUID((ServerLevel)player.level(), UUID.fromString(entityId));

                    if (entity != null) {
                        // Determine entity's default speed
                        // Some Entities (i.e. Axolotl) set this incorrectly... so adjusting in the SpeedControls class
                        float entitySpeed = SpeedControls.getMaxSpeed(entity);
                        float entitySpeedMedium = Mth.clamp(entitySpeed * 1.15F, 0.5f, 1.15f);
                        float entitySpeedFast = Mth.clamp(entitySpeed * 1.3F, 0.5f, 1.3f);

                        // Apply behaviors (if any)
                        boolean ambientContext = isAmbientContext(userMessage);
                        List<Behavior> allowedBehaviors = BehaviorPolicy.filterAllowed(result.getBehaviors(), playerData, ambientContext, hasHome());
                        for (Behavior behavior : allowedBehaviors) {
                            LOGGER.info("Behavior: " + behavior.getName() + (behavior.getArgument() != null ?
                                    ", Argument: " + behavior.getArgument() : ""));

                            // Apply behaviors to entity
                            if (behavior.getName().equals("FOLLOW")) {
                                FollowPlayerGoal followGoal = new FollowPlayerGoal(player, entity, entitySpeedMedium);
                                EntityBehaviorManager.removeGoal(entity, TalkPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, WaitHereGoal.class);
                                EntityBehaviorManager.removeGoal(entity, ReturnHomeGoal.class);
                                EntityBehaviorManager.removeGoal(entity, GuardHomeGoal.class);
                                EntityBehaviorManager.addGoal(entity, followGoal, GoalPriority.FOLLOW_PLAYER);
                                if (playerData.attacking) {
                                    AdvancementHelper.calmTheStorm(player);
                                    playerData.attacking = false;
                                }
                                playerData.fleeing = false;
                                AdvancementHelper.follow(player);
                                if (playerData.friendship >= 0) {
                                    ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) FOLLOW_FRIEND_PARTICLE, 0.5, 1);
                                } else {
                                    ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) FOLLOW_ENEMY_PARTICLE, 0.5, 1);
                                }

                            } else if (behavior.getName().equals("UNFOLLOW")) {
                                EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);

                            } else if (behavior.getName().equals("FLEE")) {
                                float fleeDistance = 40F;
                                FleePlayerGoal fleeGoal = new FleePlayerGoal(player, entity, entitySpeedFast, fleeDistance);
                                EntityBehaviorManager.removeGoal(entity, TalkPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, ProtectPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, WaitHereGoal.class);
                                EntityBehaviorManager.removeGoal(entity, ReturnHomeGoal.class);
                                EntityBehaviorManager.removeGoal(entity, GuardHomeGoal.class);
                                EntityBehaviorManager.addGoal(entity, fleeGoal, GoalPriority.FLEE_PLAYER);
                                ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) FLEE_PARTICLE, 0.5, 1);
                                playerData.fleeing = true;
                                if (playerData.attacking) {
                                    AdvancementHelper.calmTheStorm(player);
                                    playerData.attacking = false;
                                }

                            } else if (behavior.getName().equals("UNFLEE")) {
                                EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                                if (playerData.fleeing) {
                                    AdvancementHelper.standYourGround(player);
                                    playerData.fleeing = false;
                                }

                            } else if (behavior.getName().equals("ATTACK")) {
                                AttackPlayerGoal attackGoal = new AttackPlayerGoal(player, entity, entitySpeedFast);
                                EntityBehaviorManager.removeGoal(entity, TalkPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, ProtectPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, WaitHereGoal.class);
                                EntityBehaviorManager.removeGoal(entity, ReturnHomeGoal.class);
                                EntityBehaviorManager.removeGoal(entity, GuardHomeGoal.class);
                                EntityBehaviorManager.addGoal(entity, attackGoal, GoalPriority.ATTACK_PLAYER);
                                ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) FLEE_PARTICLE, 0.5, 1);
                                playerData.attacking = true;

                            } else if (behavior.getName().equals("PROTECT")) {
                                if (playerData.friendship <= 0) {
                                    // force friendship to prevent entity from attacking player when protecting
                                    playerData.friendship = 1;
                                }
                                ProtectPlayerGoal protectGoal = new ProtectPlayerGoal(player, entity, 1.0);
                                EntityBehaviorManager.removeGoal(entity, TalkPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                                EntityBehaviorManager.addGoal(entity, protectGoal, GoalPriority.PROTECT_PLAYER);
                                if (playerData.attacking) {
                                    AdvancementHelper.calmTheStorm(player);
                                    playerData.attacking = false;
                                }
                                playerData.fleeing = false;
                                AdvancementHelper.bodyguard(player);
                                if (entity.getType() == net.minecraft.world.entity.EntityType.PIG && playerData.friendship == 3) {
                                    playerData.pigProtect = true;
                                    ItemStack main = entity.getMainHandItem();
                                    ItemStack off = entity.getOffhandItem();
                                    if (main.getItem() == Items.DIAMOND_SWORD || main.getItem() == Items.NETHERITE_SWORD ||
                                            off.getItem() == Items.DIAMOND_SWORD || off.getItem() == Items.NETHERITE_SWORD) {
                                        AdvancementHelper.aLegend(player);
                                        playerData.pigProtect = false;
                                    }
                                }
                                ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) PROTECT_PARTICLE, 0.5, 1);

                            } else if (behavior.getName().equals("UNPROTECT")) {
                                EntityBehaviorManager.removeGoal(entity, ProtectPlayerGoal.class);

                            } else if (behavior.getName().equals("LEAD")) {
                                LeadPlayerGoal leadGoal = new LeadPlayerGoal(player, entity, entitySpeedMedium);
                                EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, WaitHereGoal.class);
                                EntityBehaviorManager.removeGoal(entity, ReturnHomeGoal.class);
                                EntityBehaviorManager.removeGoal(entity, GuardHomeGoal.class);
                                EntityBehaviorManager.addGoal(entity, leadGoal, GoalPriority.LEAD_PLAYER);
                                if (playerData.attacking) {
                                    AdvancementHelper.calmTheStorm(player);
                                    playerData.attacking = false;
                                }
                                playerData.fleeing = false;
                                AdvancementHelper.lead(player);
                                if (playerData.friendship >= 0) {
                                    ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) LEAD_FRIEND_PARTICLE, 0.5, 1);
                                } else {
                                    ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) LEAD_ENEMY_PARTICLE, 0.5, 1);
                                }
                            } else if (behavior.getName().equals("UNLEAD")) {
                                EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);

                            } else if (behavior.getName().equals("WAIT")) {
                                WaitHereGoal waitGoal = new WaitHereGoal(entity, entitySpeedMedium);
                                EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, ProtectPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);
                                EntityBehaviorManager.removeGoal(entity, ReturnHomeGoal.class);
                                EntityBehaviorManager.removeGoal(entity, GuardHomeGoal.class);
                                EntityBehaviorManager.addGoal(entity, waitGoal, GoalPriority.WAIT_HERE);
                                playerData.fleeing = false;
                                playerData.attacking = false;

                            } else if (behavior.getName().equals("RETURN_HOME")) {
                                if (ensureBestFriendHome(player, playerData) && isHomeInCurrentDimension(entity)) {
                                    ReturnHomeGoal returnHomeGoal = new ReturnHomeGoal(entity, getHomeBlockPos(), entitySpeedMedium);
                                    EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);
                                    EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                                    EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                                    EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);
                                    EntityBehaviorManager.removeGoal(entity, WaitHereGoal.class);
                                    EntityBehaviorManager.addGoal(entity, returnHomeGoal, GoalPriority.RETURN_HOME);
                                    playerData.fleeing = false;
                                    playerData.attacking = false;
                                }

                            } else if (behavior.getName().equals("GUARD_HOME")) {
                                if (ensureBestFriendHome(player, playerData) && isHomeInCurrentDimension(entity)) {
                                    GuardHomeGoal guardHomeGoal = new GuardHomeGoal(entity, getHomeBlockPos(), entitySpeedMedium);
                                    EntityBehaviorManager.removeGoal(entity, FollowPlayerGoal.class);
                                    EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                                    EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                                    EntityBehaviorManager.removeGoal(entity, LeadPlayerGoal.class);
                                    EntityBehaviorManager.removeGoal(entity, WaitHereGoal.class);
                                    EntityBehaviorManager.removeGoal(entity, ReturnHomeGoal.class);
                                    EntityBehaviorManager.addGoal(entity, guardHomeGoal, GoalPriority.GUARD_HOME);
                                    this.guardingHome = true;
                                    playerData.fleeing = false;
                                    playerData.attacking = false;
                                }

                            } else if (behavior.getName().equals("FRIENDSHIP")) {
                                if (behavior.getArgument() == null) {
                                    continue;
                                }
                                int new_friendship = Math.max(-3, Math.min(3, behavior.getArgument()));
                                int old_friendship = playerData.friendship;

                                // Does friendship improve?
                                if (new_friendship > playerData.friendship) {
                                    // Stop any attack/flee if friendship improves
                                    EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                                    EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);

                                    if (entity instanceof WitherBoss && new_friendship == 3) {
                                        // Best friend a Nether and get a NETHER_STAR
                                        WitherBoss wither = (WitherBoss) entity;
                                        ((WitherEntityAccessor) wither).callDropEquipment(entity.level().damageSources().generic(), 1, true);
                                        entity.level().playSound(entity, entity.blockPosition(), SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 0.3F, 1.0F);
                                    }

                                    if (entity instanceof EnderDragon && new_friendship == 3) {
                                        // Trigger end of game (friendship always wins!)
                                        EnderDragon dragon = (EnderDragon) entity;

                                        // Emit particles & sound
                                        ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) HEART_BIG_PARTICLE, 3, 200);
                                        entity.level().playSound(entity, entity.blockPosition(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.PLAYERS, 0.3F, 1.0F);
                                        entity.level().playSound(entity, entity.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5F, 1.0F);

                                        // Check if the game rule for mob loot is enabled
                                        ServerLevel serverWorld = (ServerLevel) entity.level();
                                        boolean doMobLoot = serverWorld.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT);

                                        // If this is the first time the dragon is 'befriended', adjust the XP
                                        int baseXP = 500;
                                        if (dragon.getDragonFight() != null && !dragon.getDragonFight().hasPreviouslyKilledDragon()) {
                                            baseXP = 12000;
                                        }

                                        // If the world is a server world and mob loot is enabled, spawn XP orbs
                                        if (entity.level() instanceof ServerLevel && doMobLoot) {
                                            // Loop to spawn XP orbs
                                            for (int j = 1; j <= 11; j++) {
                                                float xpFraction = (j == 11) ? 0.2F : 0.08F;
                                                int xpAmount = Mth.floor((float) baseXP * xpFraction);
                                                ExperienceOrb.award((ServerLevel) entity.level(), entity.position(), xpAmount);
                                            }
                                        }

                                        // Mark fight as over
                                        dragon.getDragonFight().setDragonKilled(dragon);
                                    }
                                }

                                // Merchant deals (if friendship changes with a Villager
                                if (entity instanceof Villager && playerData.friendship != new_friendship) {
                                    VillagerEntityAccessor villager = (VillagerEntityAccessor) entity;
                                    switch (new_friendship) {
                                        case 3:
                                            GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                    GossipTypeHelper.MAJOR_POSITIVE, 20);
                                            GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                    GossipTypeHelper.MINOR_POSITIVE, 25);
                                            break;
                                        case 2:
                                            GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                    GossipTypeHelper.MINOR_POSITIVE, 25);
                                            break;
                                        case 1:
                                            GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                    GossipTypeHelper.MINOR_POSITIVE, 10);
                                            break;
                                        case -1:
                                            GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                    GossipTypeHelper.MINOR_NEGATIVE, 10);
                                            break;
                                        case -2:
                                            GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                    GossipTypeHelper.MINOR_NEGATIVE, 25);
                                            break;
                                        case -3:
                                            GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                    GossipTypeHelper.MAJOR_NEGATIVE, 20);
                                            GossipTypeHelper.startGossip(villager, player.getUUID(),
                                                    GossipTypeHelper.MINOR_NEGATIVE, 25);
                                            break;
                                    }
                                }


                                // Tame best friends and un-tame worst enemies
                                if (entity instanceof TamableAnimal && playerData.friendship != new_friendship) {
                                    TamableAnimal tamableEntity = (TamableAnimal) entity;
                                    if (new_friendship == 3 && !tamableEntity.isTame()) {
                                        tamableEntity.tame(player);
                                    } else if (new_friendship == -3 && tamableEntity.isTame()) {
                                        TameableHelper.setTamed((TamableAnimal) entity, false);
                                        TameableHelper.clearOwner(tamableEntity);
                                    }
                                }

                                // Emit friendship particles
                                if (playerData.friendship != new_friendship) {
                                    int friendDiff = new_friendship - playerData.friendship;
                                    if (friendDiff > 0) {
                                        // Heart particles
                                        if (new_friendship == 3) {
                                            ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) HEART_BIG_PARTICLE, 0.5, 10);
                                        } else {
                                            ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) HEART_SMALL_PARTICLE, 0.1, 1);
                                        }

                                    } else if (friendDiff < 0) {
                                        // Fire particles
                                        if (new_friendship == -3) {
                                            ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) FIRE_BIG_PARTICLE, 0.5, 10);
                                        } else {
                                            ParticleEmitter.emitCreatureParticle((ServerLevel) entity.level(), entity, (ParticleOptions) FIRE_SMALL_PARTICLE, 0.1, 1);
                                        }
                                    }
                                }

                                playerData.friendship = new_friendship;
                                playerData.recordFriendshipShift(old_friendship, new_friendship);
                                if (playerData.attacking) {
                                    EntityBehaviorManager.removeGoal(entity, AttackPlayerGoal.class);
                                    AdvancementHelper.calmTheStorm(player);
                                    playerData.attacking = false;
                                }
                                if (playerData.fleeing && new_friendship >= 0) {
                                    EntityBehaviorManager.removeGoal(entity, FleePlayerGoal.class);
                                    playerData.fleeing = false;
                                }
                                AdvancementHelper.friendshipChanged(player, playerData, old_friendship, new_friendship, entity);
                            }
                        }
                    }

                    // Get cleaned message (i.e. no <BEHAVIOR> strings)
                    String cleanedMessage = result.getCleanedMessage();
                    boolean syntheticFallbackMessage = false;
                    if (!isSpokenReplyCandidate(cleanedMessage)) {
                        cleanedMessage = getSpokenFallbackMessage(userLanguage);
                        syntheticFallbackMessage = true;
                    }

                    // Add ASSISTANT message to history
                    this.addMessage(cleanedMessage, ChatDataManager.ChatSender.ASSISTANT, player, systemPrompt);

                    // Update the last entry in previousMessages to use the original message
                    String historyMessage = syntheticFallbackMessage ? cleanedMessage : result.getOriginalMessage();
                    this.previousMessages.set(this.previousMessages.size() - 1,
                            new ChatMessage(historyMessage, ChatDataManager.ChatSender.ASSISTANT, player.getDisplayName().getString()));

                    if (allow_mob_to_mob_reactions && entity != null && !syntheticFallbackMessage) {
                        ServerPackets.handleNearbyMobChat(player, entity, cleanedMessage, config);
                    }

                } else {
                    // No valid LLM response
                    throw new RuntimeException(ChatGPTRequest.lastErrorMessage);
                }

            } catch (Exception e) {
                // Log the exception for debugging
                LOGGER.error("Error processing LLM response", e);

                Randomizer.ErrorType type = Randomizer.ErrorType.GENERAL;
                int code = ChatGPTRequest.lastErrorCode;
                if (code == -1) {
                    type = Randomizer.ErrorType.CONNECTION;
                } else if (code == 401 || (config.getApiKey() == null || config.getApiKey().isEmpty())) {
                    type = Randomizer.ErrorType.CODE401;
                } else if (code == 403) {
                    type = Randomizer.ErrorType.CODE403;
                } else if (code == 429) {
                    type = Randomizer.ErrorType.CODE429;
                } else if (code == 500) {
                    type = Randomizer.ErrorType.CODE500;
                } else if (code == 503) {
                    type = Randomizer.ErrorType.CODE503;
                }
                Component helpTarget = Component.literal(Randomizer.HELP_TARGET)
                        .withStyle(ChatFormatting.BLUE);

                TR randomError = Randomizer.getRandomError(type);
                MutableComponent randomComp = randomError.comp(helpTarget);
                this.addMessage(randomComp.getString(), ChatDataManager.ChatSender.ASSISTANT, player, systemPrompt, false);

                MutableComponent errorComp = ERROR_PREFIX.comp();
                if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                    errorComp.append(Component.literal(truncateString(e.getMessage(), 50)));
                }
                player.displayClientMessage(errorComp.withStyle(ChatFormatting.RED), false);

                // Remove the error message from history to prevent it from affecting future ChatGPT requests
                if (!previousMessages.isEmpty()) {
                    previousMessages.remove(previousMessages.size() - 1);
                }

                TR solution = getSolutionMessage(code);
                if (solution != null) {
                    player.displayClientMessage(solution.comp().withStyle(ChatFormatting.BLUE), false);
                }

                player.displayClientMessage(INFO_HELP_LINK.comp(helpTarget), false);
            }
        });
    }

    public static String truncateString(String input, int maxLength) {
        return input.length() > maxLength ? input.substring(0, maxLength - 3) + "..." : input;
    }

    public static boolean isAmbientContext(String message) {
        if (message == null) {
            return false;
        }
        String trimmed = message.trim();
        return trimmed.startsWith("[Nearby player chat.") || trimmed.startsWith("[Nearby mob chat.");
    }

    public static TR getSolutionMessage(int code) {
        return switch (code) {
            case -1 -> SOLUTION_CONNECTION;
            case 0 -> SOLUTION_VERIFY_URL;
            case 401 -> SOLUTION_ADD_KEY;
            case 403 -> SOLUTION_CHECK_REGION;
            case 429 -> SOLUTION_ADD_FUNDS;
            case 500 -> SOLUTION_SERVER_ERROR;
            case 503 -> SOLUTION_TRY_AGAIN;
            default -> null;
        };
    }

    // Add a message to the history and update the current message
    public void addMessage(String message, ChatDataManager.ChatSender sender, ServerPlayer player, String systemPrompt) {
        addMessage(message, sender, player, systemPrompt, true);
    }

    // Internal helper allowing callers to skip advancement triggers
    public void addMessage(String message, ChatDataManager.ChatSender sender, ServerPlayer player, String systemPrompt, boolean triggerAdvancement) {
        // Truncate message (prevent crazy long messages... just in case)
        String truncatedMessage = message.substring(0, Math.min(message.length(), ChatDataManager.MAX_CHAR_IN_USER_MESSAGE));

        // Add context-switching logic for USER messages only
        String playerName = player.getDisplayName().getString();
        if (sender == ChatDataManager.ChatSender.USER && previousMessages.size() > 1) {
            ChatMessage lastMessage = previousMessages.get(previousMessages.size() - 1);
            if (lastMessage.name == null || !lastMessage.name.equals(playerName)) {  // Null-safe check
                boolean isReturningPlayer = previousMessages.stream().anyMatch(msg -> playerName.equals(msg.name)); // Avoid NPE here too
                String note = isReturningPlayer
                        ? "<returning player: " + playerName + " resumes the conversation>"
                        : "<a new player has joined the conversation: " + playerName + ">";
                previousMessages.add(new ChatMessage(note, sender, playerName));

                // Log context-switching message
                LOGGER.info("Conversation-switching message: status=PENDING, sender={}, message={}, player={}, entity={}",
                        ChatDataManager.ChatStatus.PENDING, note, playerName, entityId);
            }
        }

        // Add message to history
        previousMessages.add(new ChatMessage(truncatedMessage, sender, playerName));

        // Log regular message addition
        LOGGER.info("Message added: status={}, sender={}, message={}, player={}, entity={}",
                status.toString(), sender.toString(), truncatedMessage, playerName, entityId);

        // Update current message and reset line number of displayed text
        this.currentMessage = truncatedMessage;
        this.currentLineNumber = 0;
        this.sender = sender;

        // Determine status for message
        if (sender == ChatDataManager.ChatSender.ASSISTANT) {
            status = ChatDataManager.ChatStatus.DISPLAY;
        } else {
            status = ChatDataManager.ChatStatus.PENDING;
        }

        if (sender == ChatDataManager.ChatSender.USER && systemPrompt.equals("system-chat") && auto_generated == 0) {
            // Broadcast new player message (when not auto-generated)
            ServerPackets.BroadcastPlayerMessage(this, player);
        }

        // Broadcast new entity message status (i.e. pending)
        ServerPackets.BroadcastEntityMessage(this);

        if (sender == ChatDataManager.ChatSender.ASSISTANT && triggerAdvancement) {
            AdvancementHelper.chatExchange(player, this);
            Mob entity = (Mob) ServerEntityFinder.getEntityByUUID((ServerLevel) player.level(), UUID.fromString(entityId));
            if (entity != null) {
                AdvancementHelper.checkInnerCircle(player, entity);

                // Send one-time message to player text chat if enabled
                ConfigurationHandler.Config config = new ConfigurationHandler(ServerPackets.serverInstance).loadConfig();
                if (config.getSendToChat()) {
                    String mobName = entity.getDisplayName().getString();
                    player.sendSystemMessage(Component.literal("[" + mobName + "] " + truncatedMessage));
                }
            }
        }
    }

    // Get wrapped lines
    public List<String> getWrappedLines() {
        return LineWrapper.wrapLines(this.currentMessage, ChatDataManager.MAX_CHAR_PER_LINE);
    }

    public boolean isEndOfMessage() {
        int totalLines = this.getWrappedLines().size();
        // Check if the current line number plus DISPLAY_NUM_LINES covers or exceeds the total number of lines
        return currentLineNumber + ChatDataManager.DISPLAY_NUM_LINES >= totalLines;
    }

    public void setLineNumber(Integer lineNumber) {
        int totalLines = this.getWrappedLines().size();
        // Ensure the lineNumber is within the valid range
        currentLineNumber = Math.min(Math.max(lineNumber, 0), totalLines);

        // Broadcast to all players
        ServerPackets.BroadcastEntityMessage(this);
    }

    public void setStatus(ChatDataManager.ChatStatus new_status) {
        status = new_status;

        // Broadcast to all players
        ServerPackets.BroadcastEntityMessage(this);
    }
}
