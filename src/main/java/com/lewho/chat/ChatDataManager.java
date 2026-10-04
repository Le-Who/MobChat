// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.chat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lewho.commands.ConfigurationHandler;
import com.lewho.network.ServerPackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * The {@code ChatDataManager} class manages chat data for all entities. This class also helps
 * generate new messages, set entity goals, and other useful chat-related functions.
 */
public class ChatDataManager {
    // Use a static instance to manage our data globally
    private static final ChatDataManager SERVER_INSTANCE = new ChatDataManager(true);
    private static final ChatDataManager CLIENT_INSTANCE = new ChatDataManager(false);
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    public static int MAX_CHAR_PER_LINE = 20;
    public static int DISPLAY_NUM_LINES = 3;
    public static int MAX_CHAR_IN_USER_MESSAGE = 512;
    public static int TICKS_TO_DISPLAY_USER_MESSAGE = 70;
    private static final Gson GSON = new Gson();

    public enum ChatStatus {
        NONE,       // No chat status yet
        PENDING,    // Chat is pending (e.g., awaiting response or processing)
        DISPLAY,    // Chat is currently being displayed
        HIDDEN,     // Chat is currently hidden
    }

    public enum ChatSender {
        USER,      // A user chat message
        ASSISTANT  // A GPT generated message
    }

    // HashMap to associate unique entity IDs with their chat data
    public ConcurrentHashMap<String, EntityChatData> entityChatDataMap;
    public ConcurrentHashMap<UUID, AutoMessageBucket> autoResponseBuckets;
    public ConcurrentHashMap<UUID, AutoMessageBucket> ambientResponseBuckets;
    private MinecraftServer sessionServer;
    private ChatSession responseSession;

    public void clearData() {
        if (responseSession != null) {
            responseSession.close();
        }
        responseSession = null;
        sessionServer = null;
        // Clear the chat data for the previous session
        entityChatDataMap.clear();
        autoResponseBuckets.clear();
        ambientResponseBuckets.clear();
    }

    private ChatDataManager(Boolean server_only) {
        // Constructor
        entityChatDataMap = new ConcurrentHashMap<>();
        autoResponseBuckets = new ConcurrentHashMap<>();
        ambientResponseBuckets = new ConcurrentHashMap<>();

        if (server_only) {
            // Generate initial quest
            // TODO: Complete the quest flow
            //generateQuest();
        }
    }

    // Method to get the global instance of the server data manager
    public static ChatDataManager getServerInstance() {
        return SERVER_INSTANCE;
    }

    // Method to get the global instance of the client data manager (synced from server)
    public static ChatDataManager getClientInstance() {
        return CLIENT_INSTANCE;
    }

    // Retrieve chat data for a specific entity, or create it if it doesn't exist
    public EntityChatData getOrCreateChatData(String entityId) {
        return entityChatDataMap.computeIfAbsent(entityId, k -> new EntityChatData(entityId));
    }

    public ChatSession getSession(MinecraftServer server) {
        return sessionServer == server ? responseSession : null;
    }

    public void closeSession(MinecraftServer server) {
        ChatSession session = getSession(server);
        if (session != null) {
            session.close();
        }
    }

    public void cancelRequest(EntityChatData data) {
        if (responseSession != null && data != null) {
            responseSession.cancel(data);
        }
    }

    /** Admit before consuming rate allowance, resetting cooldowns or preparing gameplay effects. */
    public boolean dispatchRequest(EntityChatData data, ServerPlayer player, BooleanSupplier policy,
                                   Consumer<ChatSession.Request> action) {
        return ChatSession.dispatch(() -> beginRequest(data, player), policy, action);
    }

    /** Capture world, entity and player identities before dispatching an HTTP request. */
    public ChatSession.Request beginRequest(EntityChatData data, ServerPlayer player) {
        MinecraftServer server = player == null ? null : player.getServer();
        ChatSession session = getSession(server);
        Level playerLevel = player == null ? null : player.level();
        if (server == null || session == null || data == null || !server.isSameThread()
                || !(playerLevel instanceof ServerLevel world)) {
            return null;
        }
        String entityId = data.entityId;
        if (entityId == null) {
            return null;
        }
        UUID entityUuid;
        try {
            entityUuid = UUID.fromString(entityId);
        } catch (IllegalArgumentException e) {
            return null;
        }
        Entity found = world.getEntity(entityUuid);
        if (!(found instanceof Mob entity)) {
            return null;
        }
        return session.tryBegin(data,
                () -> getSession(server) == session && entityChatDataMap.get(entityId) == data
                        && entityId.equals(data.entityId),
                () -> ServerPackets.serverInstance == server && server.getLevel(world.dimension()) == world
                        && player.level() == world && player.isAlive() && !player.isRemoved()
                        && server.getPlayerList().getPlayer(player.getUUID()) == player
                        && entity.level() == world && entity.isAlive() && !entity.isRemoved()
                        && world.getEntity(entityUuid) == entity,
                () -> {
                    boolean pending = data.status == ChatStatus.PENDING;
                    data.recoverPendingResponse();
                    if (pending && session.isOpen() && ServerPackets.serverInstance == server) {
                        ServerPackets.BroadcastEntityMessage(data);
                    }
                });
    }

    private AutoMessageBucket getPlayerBucket(UUID playerId, ConfigurationHandler.Config config) {
        return autoResponseBuckets.computeIfAbsent(playerId,
                k -> new AutoMessageBucket(config.getMaxPlayerAutoResponses(), config.getPlayerAutoCooldownSeconds()));
    }

    private AutoMessageBucket getEntityBucket(EntityChatData chatData, ConfigurationHandler.Config config) {
        if (chatData.autoBucket == null) {
            chatData.autoBucket = new AutoMessageBucket(config.getMaxEntityAutoResponses(), config.getEntityAutoCooldownSeconds());
        }
        return chatData.autoBucket;
    }

    private AutoMessageBucket getAmbientPlayerBucket(UUID playerId, ConfigurationHandler.Config config) {
        return ambientResponseBuckets.computeIfAbsent(playerId,
                k -> new AutoMessageBucket(config.getMaxPlayerAmbientResponses(), config.getPlayerAmbientCooldownSeconds()));
    }

    private AutoMessageBucket getAmbientEntityBucket(EntityChatData chatData, ConfigurationHandler.Config config) {
        if (chatData.ambientBucket == null) {
            chatData.ambientBucket = new AutoMessageBucket(config.getMaxEntityAmbientResponses(), config.getEntityAmbientCooldownSeconds());
        }
        return chatData.ambientBucket;
    }

    public boolean handleAutoResponse(EntityChatData chatData, ServerPlayer player, boolean isAuto, ConfigurationHandler.Config config) {
        AutoMessageBucket entityBucket = getEntityBucket(chatData, config);
        AutoMessageBucket playerBucket = getPlayerBucket(player.getUUID(), config);

        if (!isAuto) {
            playerBucket.reset();
            resetReactiveCooldowns(chatData, chatData.getPlayerData(player));
            return true;
        }

        if (!entityBucket.hasTokens()) {
            LOGGER.info("Auto response skipped for entity {}: entity cooldown active", chatData.entityId);
            return false;
        }
        if (!playerBucket.hasTokens()) {
            LOGGER.info("Auto response skipped for player {}: player cooldown active", player.getDisplayName().getString());
            return false;
        }

        entityBucket.consume();
        playerBucket.consume();
        return true;
    }

    public boolean handleDamageReaction(EntityChatData chatData, PlayerData playerData, String playerName, ConfigurationHandler.Config config) {
        if (playerData == null) {
            return true;
        }

        int cooldownSeconds = config == null ? 25 : config.getDamageReactionCooldownSeconds();
        if (cooldownSeconds <= 0) {
            playerData.lastDamageReactionAt = System.currentTimeMillis();
            return true;
        }

        long now = System.currentTimeMillis();
        long cooldownMillis = cooldownSeconds * 1000L;
        if (playerData.lastDamageReactionAt <= 0L || now - playerData.lastDamageReactionAt >= cooldownMillis) {
            playerData.lastDamageReactionAt = now;
            return true;
        }

        playerData.suppressedDamageReactionCount++;
        String entityId = chatData == null ? "<unknown>" : chatData.entityId;
        LOGGER.info("Damage reaction skipped for entity {} and player {}: damage cooldown active ({} suppressed hit(s))",
                entityId,
                playerName == null || playerName.isEmpty() ? "<unknown>" : playerName,
                playerData.suppressedDamageReactionCount);
        return false;
    }

    public void resetReactiveCooldowns(EntityChatData chatData, PlayerData playerData) {
        if (playerData == null) {
            return;
        }
        playerData.resetDamageReactionCooldown();
    }

    public boolean handleAmbientResponse(EntityChatData chatData, ServerPlayer player, ConfigurationHandler.Config config) {
        return handleAmbientResponse(chatData, player.getUUID(), player.getDisplayName().getString(), config);
    }

    public boolean handleAmbientResponse(EntityChatData chatData, UUID playerId, String playerName, ConfigurationHandler.Config config) {
        AutoMessageBucket entityBucket = getAmbientEntityBucket(chatData, config);
        AutoMessageBucket playerBucket = getAmbientPlayerBucket(playerId, config);

        if (!entityBucket.hasTokens()) {
            LOGGER.info("Ambient response skipped for entity {}: entity cooldown active", chatData.entityId);
            return false;
        }
        if (!playerBucket.hasTokens()) {
            LOGGER.info("Ambient response skipped for player {}: player cooldown active", playerName);
            return false;
        }

        entityBucket.consume();
        playerBucket.consume();
        return true;
    }

    public boolean handleAmbientEntityResponse(EntityChatData chatData, ConfigurationHandler.Config config) {
        AutoMessageBucket entityBucket = getAmbientEntityBucket(chatData, config);

        if (!entityBucket.hasTokens()) {
            LOGGER.info("Ambient response skipped for entity {}: entity cooldown active", chatData.entityId);
            return false;
        }

        entityBucket.consume();
        return true;
    }

    // Update the UUID in the map (i.e. bucketed entity and then released, changes their UUID)
    public void updateUUID(String oldUUID, String newUUID) {
        EntityChatData data = entityChatDataMap.get(oldUUID);
        if (data != null) {
            cancelRequest(data);
            entityChatDataMap.remove(oldUUID, data);
            data.entityId = newUUID;
            entityChatDataMap.put(newUUID, data);
            LOGGER.info("Updated chat data from UUID (" + oldUUID + ") to UUID (" + newUUID + ")");

            // Broadcast to all players
            ServerPackets.BroadcastEntityMessage(data);
        } else {
            LOGGER.info("Unable to update chat data, UUID not found: " + oldUUID);
        }
    }

    // Save chat data to file
    public String GetLightChatData(String playerId) {
        return GetLightChatData(playerId, new PlayerChatPreferences());
    }

    // Save chat data to file
    public String GetLightChatData(String playerId, PlayerChatPreferences preferences) {
        try {
            // Create "light" version of entire chat data HashMap
            HashMap<String, EntityChatDataLight> lightVersionMap = new HashMap<>();
            UUID viewerId = parsePlayerId(playerId);
            PlayerChatPreferences effectivePreferences = preferences == null ? new PlayerChatPreferences() : preferences;
            this.entityChatDataMap.forEach((name, entityChatData) -> {
                if (effectivePreferences.canReceiveEntityUpdate(viewerId, entityChatData.currentPlayerId, entityChatData.sender)) {
                    lightVersionMap.put(name, entityChatData.toLightVersion(playerId));
                }
            });
            return GSON.toJson(lightVersionMap);
        } catch (Exception e) {
            // Handle exceptions
            return "";
        }
    }

    private static UUID parsePlayerId(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(playerId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Save chat data to file
    public void saveChatData(MinecraftServer server) {
        if (sessionServer != server) {
            return;
        }
        if (!server.isSameThread()) {
            ChatSession session = getSession(server);
            if (session != null) {
                session.execute(() -> saveChatData(server));
            }
            return;
        }
        File saveFile = new File(server.getWorldPath(LevelResource.ROOT).toFile(), "chatdata.json");
        LOGGER.info("Saving chat data to " + saveFile.getAbsolutePath());

        // Clean up blank, temp entities in data
        entityChatDataMap.values().removeIf(entityChatData -> entityChatData.status == ChatStatus.NONE);

        try {
            ChatDataFile.save(saveFile.toPath(), this.entityChatDataMap);
        } catch (Exception e) {
            String errorMessage = "Error saving `chatdata.json`. No CreatureChat chat history was saved! " + e.getMessage();
            LOGGER.error(errorMessage, e);
            ServerPackets.sendErrorToAllOps(server, errorMessage);
        }
    }

    // Load chat data from file
    public void loadChatData(MinecraftServer server) {
        clearData();
        sessionServer = server;
        responseSession = new ChatSession(server);
        File loadFile = new File(server.getWorldPath(LevelResource.ROOT).toFile(), "chatdata.json");
        LOGGER.info("Loading chat data from " + loadFile.getAbsolutePath());

        if (loadFile.exists()) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(loadFile), StandardCharsets.UTF_8)) {
                Type type = new TypeToken<ConcurrentHashMap<String, EntityChatData>>(){}.getType();
                this.entityChatDataMap = GSON.fromJson(reader, type);
                if (this.entityChatDataMap == null) {
                    this.entityChatDataMap = new ConcurrentHashMap<>();
                }

                // Clean up blank, temp entities in data
                entityChatDataMap.values().removeIf(entityChatData -> entityChatData.status == ChatStatus.NONE);

                // Post-process each EntityChatData object
                for (EntityChatData entityChatData : entityChatDataMap.values()) {
                    entityChatData.postDeserializeInitialization();
                }
            } catch (Exception e) {
                LOGGER.error("Error loading chat data", e);
                this.entityChatDataMap = new ConcurrentHashMap<>();
            }
        } else {
            // Init empty chat data
            this.entityChatDataMap = new ConcurrentHashMap<>();
        }
    }
}
