// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.network;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.ChatDataSaverScheduler;
import com.lewho.chat.ChatGPTRequest;
import com.lewho.chat.ChatHistoryEntry;
import com.lewho.chat.ChatSession;
import com.lewho.chat.EntityChatData;
import com.lewho.chat.PlayerData;
import com.lewho.chat.PlayerChatPreferences;
import com.lewho.commands.ConfigurationHandler;
import com.lewho.commands.ConfigurationScreenData;
import com.lewho.commands.MinecraftLanguages;
import com.lewho.goals.EntityBehaviorManager;
import com.lewho.goals.GoalPriority;
import com.lewho.goals.TalkPlayerGoal;
import com.lewho.inventory.ChatInventory;
import com.lewho.inventory.InventoryLootTables;
import com.lewho.inventory.LootTableHelper;
import com.lewho.particle.Particles;
import com.lewho.utils.Compression;
import com.lewho.utils.Randomizer;
import com.lewho.utils.ServerEntityFinder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ServerPackets {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static final int AMBIENT_CONTEXT_MAX_CHARS = 240;
    private static final int MAX_HISTORY_ENTRIES = 60;
    private static final int MAX_HISTORY_MESSAGE_CHARS = 4096;
    public static MinecraftServer serverInstance;
    public static ChatDataSaverScheduler scheduler = null;
    public static PlayerChatPreferences playerPreferences = new PlayerChatPreferences();
    public static final ResourceLocation PACKET_C2S_GREETING = new ResourceLocation("creaturechat", "packet_c2s_greeting");
    public static final ResourceLocation PACKET_C2S_READ_NEXT = new ResourceLocation("creaturechat", "packet_c2s_read_next");
    public static final ResourceLocation PACKET_C2S_SET_STATUS = new ResourceLocation("creaturechat", "packet_c2s_set_status");
    public static final ResourceLocation PACKET_C2S_OPEN_CHAT = new ResourceLocation("creaturechat", "packet_c2s_open_chat");
    public static final ResourceLocation PACKET_C2S_CLOSE_CHAT = new ResourceLocation("creaturechat", "packet_c2s_close_chat");
    public static final ResourceLocation PACKET_C2S_SEND_CHAT = new ResourceLocation("creaturechat", "packet_c2s_send_chat");
    public static final ResourceLocation PACKET_C2S_REQUEST_HISTORY = new ResourceLocation("creaturechat", "packet_c2s_request_history");
    public static final ResourceLocation PACKET_C2S_CONFIG_SAVE = new ResourceLocation("creaturechat", "packet_c2s_config_save");
    public static final ResourceLocation PACKET_C2S_CONFIG_TEST = new ResourceLocation("creaturechat", "packet_c2s_config_test");
    public static final ResourceLocation PACKET_S2C_ENTITY_MESSAGE = new ResourceLocation("creaturechat", "packet_s2c_entity_message");
    public static final ResourceLocation PACKET_S2C_PLAYER_MESSAGE = new ResourceLocation("creaturechat", "packet_s2c_player_message");
    public static final ResourceLocation PACKET_S2C_LOGIN = new ResourceLocation("creaturechat", "packet_s2c_login");
    public static final ResourceLocation PACKET_S2C_WHITELIST = new ResourceLocation("creaturechat", "packet_s2c_whitelist");
    public static final ResourceLocation PACKET_S2C_PLAYER_STATUS = new ResourceLocation("creaturechat", "packet_s2c_player_status");
    public static final ResourceLocation PACKET_S2C_CONFIG_OPEN = new ResourceLocation("creaturechat", "packet_s2c_config_open");
    public static final ResourceLocation PACKET_S2C_CONFIG_STATUS = new ResourceLocation("creaturechat", "packet_s2c_config_status");
    public static final ResourceLocation PACKET_S2C_CHAT_HISTORY = new ResourceLocation("creaturechat", "packet_s2c_chat_history");
    public static final ParticleType<?> HEART_SMALL_PARTICLE = Particles.HEART_SMALL_PARTICLE;
    public static final ParticleType<?> HEART_BIG_PARTICLE = Particles.HEART_BIG_PARTICLE;
    public static final ParticleType<?> FIRE_SMALL_PARTICLE = Particles.FIRE_SMALL_PARTICLE;
    public static final ParticleType<?> FIRE_BIG_PARTICLE = Particles.FIRE_BIG_PARTICLE;
    public static final ParticleType<?> ATTACK_PARTICLE = Particles.ATTACK_PARTICLE;
    public static final ParticleType<?> FLEE_PARTICLE = Particles.FLEE_PARTICLE;
    public static final ParticleType<?> FOLLOW_FRIEND_PARTICLE = Particles.FOLLOW_FRIEND_PARTICLE;
    public static final ParticleType<?> FOLLOW_ENEMY_PARTICLE = Particles.FOLLOW_ENEMY_PARTICLE;
    public static final ParticleType<?> PROTECT_PARTICLE = Particles.PROTECT_PARTICLE;
    public static final ParticleType<?> LEAD_FRIEND_PARTICLE = Particles.LEAD_FRIEND_PARTICLE;
    public static final ParticleType<?> LEAD_ENEMY_PARTICLE = Particles.LEAD_ENEMY_PARTICLE;
    public static final ParticleType<?> LEAD_PARTICLE = Particles.LEAD_PARTICLE;

    public static void register() {
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "heart_small"), HEART_SMALL_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "heart_big"), HEART_BIG_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "fire_small"), FIRE_SMALL_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "fire_big"), FIRE_BIG_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "attack"), ATTACK_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "flee"), FLEE_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "follow_enemy"), FOLLOW_ENEMY_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "follow_friend"), FOLLOW_FRIEND_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "protect"), PROTECT_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "lead_enemy"), LEAD_ENEMY_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "lead_friend"), LEAD_FRIEND_PARTICLE);
        Registry.register(BuiltInRegistries.PARTICLE_TYPE, new ResourceLocation("creaturechat", "lead"), LEAD_PARTICLE);

        PacketHelper.registerReceiver(PACKET_C2S_GREETING, (server, player, buf) -> {
            UUID entityId = UUID.fromString(buf.readUtf());
            String userLanguage = buf.readUtf(32767);

            server.execute(() -> {
                Mob entity = (Mob)ServerEntityFinder.getEntityByUUID((ServerLevel)player.level(), entityId);
                if (entity != null) {
                    EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entity.getStringUUID());
                    if (chatData.characterSheet.isEmpty()) {
                        generate_character(userLanguage, chatData, player, entity, false);
                    }
                }
            });
        });

        PacketHelper.registerReceiver(PACKET_C2S_READ_NEXT, (server, player, buf) -> {
            UUID entityId = UUID.fromString(buf.readUtf());
            int lineNumber = buf.readInt();

            server.execute(() -> {
                Mob entity = (Mob)ServerEntityFinder.getEntityByUUID((ServerLevel)player.level(), entityId);
                if (entity != null) {
                    TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 3.5F);
                    EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);

                    EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entity.getStringUUID());
                    LOGGER.debug("Update read lines to " + lineNumber + " for: " + entity.getType().toString());
                    chatData.setLineNumber(lineNumber);
                }
            });
        });

        PacketHelper.registerReceiver(PACKET_C2S_SET_STATUS, (server, player, buf) -> {
            UUID entityId = UUID.fromString(buf.readUtf());
            String status_name = buf.readUtf(32767);

            server.execute(() -> {
                Mob entity = (Mob)ServerEntityFinder.getEntityByUUID((ServerLevel)player.level(), entityId);
                if (entity != null) {
                    TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 3.5F);
                    EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);

                    EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entity.getStringUUID());
                    LOGGER.debug("Hiding chat bubble for: " + entity.getType().toString());
                    chatData.setStatus(ChatDataManager.ChatStatus.valueOf(status_name));
                }
            });
        });

        PacketHelper.registerReceiver(PACKET_C2S_OPEN_CHAT, (server, player, buf) -> {
            UUID entityId = UUID.fromString(buf.readUtf());

            server.execute(() -> {
                Mob entity = (Mob)ServerEntityFinder.getEntityByUUID((ServerLevel)player.level(), entityId);
                if (entity != null) {
                    TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 7F);
                    EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);
                }

                BroadcastPlayerStatus(player, true);
            });
        });

        PacketHelper.registerReceiver(PACKET_C2S_CLOSE_CHAT, (server, player, buf) -> {

            server.execute(() -> {
                BroadcastPlayerStatus(player, false);
            });
        });

        PacketHelper.registerReceiver(PACKET_C2S_SEND_CHAT, (server, player, buf) -> {
            UUID entityId = UUID.fromString(buf.readUtf());
            String message = buf.readUtf(32767);
            String userLanguage = buf.readUtf(32767);

            server.execute(() -> {
                Mob entity = (Mob)ServerEntityFinder.getEntityByUUID((ServerLevel)player.level(), entityId);
                if (entity != null) {
                    EntityChatData chatData = ChatDataManager.getServerInstance().getOrCreateChatData(entity.getStringUUID());
                    if (chatData.characterSheet.isEmpty()) {
                        generate_character(userLanguage, chatData, player, entity, false);
                    } else {
                        generate_chat(userLanguage, chatData, player, entity, message, false);
                    }
                }
            });
        });

        PacketHelper.registerReceiver(PACKET_C2S_REQUEST_HISTORY, (server, player, buf) -> {
            UUID entityId = UUID.fromString(buf.readUtf());
            server.execute(() -> sendChatHistory(player, entityId));
        });

        PacketHelper.registerReceiver(PACKET_C2S_CONFIG_SAVE, (server, player, buf) -> {
            ConfigurationScreenData.SaveData data = new ConfigurationScreenData.SaveData(
                    buf.readUtf(32767),
                    buf.readUtf(32767),
                    buf.readUtf(32767),
                    buf.readUtf(32767),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readUtf(32767),
                    buf.readUtf(32767)
            );

            server.execute(() -> saveConfigFromScreen(server, player, data));
        });

        PacketHelper.registerReceiver(PACKET_C2S_CONFIG_TEST, (server, player, buf) -> {
            ConfigurationScreenData.SaveData data = new ConfigurationScreenData.SaveData(
                    buf.readUtf(32767),
                    buf.readUtf(32767),
                    buf.readUtf(32767),
                    buf.readUtf(32767),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readUtf(32767),
                    buf.readUtf(32767)
            );

            server.execute(() -> testConfigFromScreen(server, player, data));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;

            send_whitelist_blacklist(player);

            LOGGER.info("Server send compressed, chunked login message packets to player: " + player.getDisplayName().getString());
            String chatDataJSON = ChatDataManager.getServerInstance().GetLightChatData(
                    player.getStringUUID(), player.getDisplayName().getString(), playerPreferences);
            byte[] compressedData = Compression.compressString(chatDataJSON);
            if (compressedData == null) {
                LOGGER.error("Failed to compress chat data.");
                return;
            }

            final int chunkSize = 32000;
            int totalPackets = (int) Math.ceil((double) compressedData.length / chunkSize);

            for (int i = 0; i < totalPackets; i++) {
                int start = i * chunkSize;
                int end = Math.min(compressedData.length, start + chunkSize);

                FriendlyByteBuf buffer = BufferHelper.create();
                buffer.writeInt(i);
                buffer.writeInt(totalPackets);

                byte[] chunk = Arrays.copyOfRange(compressedData, start, end);
                buffer.writeByteArray(chunk);

                PacketHelper.send(player, PACKET_S2C_LOGIN, buffer);
            }
        });

        ServerMessageEvents.CHAT_MESSAGE.register((chat, player, bound) -> {
            MinecraftServer server = player.getServer();
            String message = chat.filterMask().apply(chat.signedContent());
            if (server == null || server != serverInstance || message == null || message.isBlank()) {
                return;
            }
            ConfigurationHandler.Config config = new ConfigurationHandler(server).loadConfig();
            if (config.getChatBubbles()) {
                EntityChatData bubble = new EntityChatData(player.getStringUUID());
                bubble.currentMessage = message;
                BroadcastPlayerMessage(bubble, player);
            }
            handleNearbyPlayerChat(player, message);
        });

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (Level.OVERWORLD.equals(world.dimension())) {
                if (scheduler != null) {
                    scheduler.stopAutoSaveTask();
                }
                serverInstance = server;
                ChatDataManager.getServerInstance().loadChatData(server);
                playerPreferences = PlayerChatPreferences.load(playerPreferencesPath(server));

                scheduler = new ChatDataSaverScheduler();
                scheduler.startAutoSaveTask(server, 15, TimeUnit.MINUTES);
            }
        });
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (Level.OVERWORLD.equals(world.dimension()) && serverInstance == server) {
                ChatDataManager manager = ChatDataManager.getServerInstance();
                if (scheduler != null) {
                    scheduler.stopAutoSaveTask();
                    scheduler = null;
                }
                manager.closeSession(server);
                manager.saveChatData(server);
                playerPreferences.save(playerPreferencesPath(server));
                manager.clearData();
                serverInstance = null;

            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (world.getServer() != serverInstance) {
                return;
            }
            String entityUUID = entity.getStringUUID();
            ChatDataManager.getServerInstance().cancelRequest(
                    ChatDataManager.getServerInstance().entityChatDataMap.get(entityUUID));
            if (entity.getRemovalReason() == Entity.RemovalReason.KILLED && ChatDataManager.getServerInstance().entityChatDataMap.containsKey(entityUUID)) {
                LOGGER.debug("Entity killed (" + entityUUID + "), updating death time stamp.");
                ChatDataManager.getServerInstance().entityChatDataMap.get(entityUUID).death = System.currentTimeMillis();
            }
        });
    }

    public static void send_whitelist_blacklist(ServerPlayer player) {
        ConfigurationHandler.Config config = new ConfigurationHandler(ServerPackets.serverInstance).loadConfig();
        FriendlyByteBuf buffer = BufferHelper.create();

        List<String> whitelist = config.getWhitelist();
        buffer.writeInt(whitelist.size());
        for (String entry : whitelist) {
            buffer.writeUtf(entry);
        }

        List<String> blacklist = config.getBlacklist();
        buffer.writeInt(blacklist.size());
        for (String entry : blacklist) {
            buffer.writeUtf(entry);
        }

        if (player != null) {
            LOGGER.info("Sending whitelist / blacklist packet to player: " + player.getDisplayName().getString());
            PacketHelper.send(player, PACKET_S2C_WHITELIST, buffer);
        } else {
            for (ServerPlayer serverPlayer : serverInstance.getPlayerList().getPlayers()) {
                PacketHelper.send(serverPlayer, PACKET_S2C_WHITELIST, buffer);
            }
        }
    }

    private static java.nio.file.Path playerPreferencesPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("creaturechat_player_prefs.json");
    }

    public static boolean getShowOtherNpcReplies(ServerPlayer player) {
        if (player == null) {
            return true;
        }
        return playerPreferences.getShowOtherNpcReplies(player.getUUID());
    }

    public static void setShowOtherNpcReplies(ServerPlayer player, boolean enabled) {
        if (player == null) {
            return;
        }
        playerPreferences.setShowOtherNpcReplies(player.getUUID(), enabled);
        MinecraftServer server = player.getServer();
        if (server != null) {
            playerPreferences.save(playerPreferencesPath(server));
        }
    }

    private static void sendChatHistory(ServerPlayer player, UUID entityId) {
        FriendlyByteBuf buffer = BufferHelper.create();
        buffer.writeUtf(entityId.toString());

        EntityChatData chatData = ChatDataManager.getServerInstance().entityChatDataMap.get(entityId.toString());
        List<ChatHistoryEntry> entries = chatData == null
                ? Collections.emptyList()
                : ChatHistoryEntry.fromMessages(chatData.previousMessages, MAX_HISTORY_ENTRIES);

        buffer.writeInt(entries.size());
        for (ChatHistoryEntry entry : entries) {
            buffer.writeUtf(entry.sender().toString());
            buffer.writeUtf(entry.name());
            buffer.writeUtf(EntityChatData.truncateString(entry.message(), MAX_HISTORY_MESSAGE_CHARS));
            buffer.writeLong(entry.timestamp());
        }

        PacketHelper.send(player, PACKET_S2C_CHAT_HISTORY, buffer);
    }

    public static void sendConfigScreen(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ConfigurationHandler.Config config = new ConfigurationHandler(server).loadConfig();
        ConfigurationScreenData.OpenData data = ConfigurationScreenData.fromConfig(config);
        FriendlyByteBuf buffer = BufferHelper.create();
        buffer.writeUtf(data.provider());
        buffer.writeUtf(data.url());
        buffer.writeUtf(data.apiKeys());
        buffer.writeUtf(data.maskedApiKeys());
        buffer.writeUtf(data.models());
        buffer.writeInt(data.timeout());
        buffer.writeInt(data.maxOutputTokens());
        buffer.writeUtf(data.thinkingLevel());
        buffer.writeUtf(data.generationLanguage() != null ? data.generationLanguage() : "");
        PacketHelper.send(player, PACKET_S2C_CONFIG_OPEN, buffer);
    }

    private static void saveConfigFromScreen(MinecraftServer server, ServerPlayer player, ConfigurationScreenData.SaveData data) {
        if (!isConfigOperator(player)) {
            sendConfigStatus(player, false, "Only server operators can change CreatureChat configuration.");
            return;
        }

        ConfigurationHandler configHandler = new ConfigurationHandler(server);
        ConfigurationHandler.Config config = configHandler.loadConfig();
        try {
            ConfigurationScreenData.applyToConfig(config, data);
        } catch (IllegalArgumentException e) {
            sendConfigStatus(player, false, e.getMessage());
            return;
        }

        if (configHandler.saveConfig(config, true)) {
            sendConfigStatus(player, true, "CreatureChat configuration saved.");
        } else {
            sendConfigStatus(player, false, "CreatureChat configuration could not be saved. Check server logs.");
        }
    }

    private static void testConfigFromScreen(MinecraftServer server, ServerPlayer player, ConfigurationScreenData.SaveData data) {
        if (!isConfigOperator(player)) {
            sendConfigStatus(player, false, "Only server operators can test CreatureChat configuration.");
            return;
        }

        ConfigurationHandler.Config config = new ConfigurationHandler(server).loadConfig();
        try {
            ConfigurationScreenData.applyToConfig(config, data);
        } catch (IllegalArgumentException e) {
            sendConfigStatus(player, false, e.getMessage());
            return;
        }

        if (config.getActiveApiKey().isBlank()) {
            sendConfigStatus(player, false, "Cannot test: API key is empty.");
            return;
        }
        if (config.getActiveModel().isBlank()) {
            sendConfigStatus(player, false, "Cannot test: model is empty.");
            return;
        }

        ChatSession session = ChatDataManager.getServerInstance().getSession(server);
        Level playerLevel = player.level();
        if (session == null || !session.isOpen() || !(playerLevel instanceof ServerLevel world)) {
            return;
        }
        sendConfigStatus(player, true, "Testing CreatureChat AI configuration...");
        ChatGPTRequest.fetchResultFromChatGPT(config, "Reply with exactly: OK", new HashMap<>(), new ArrayList<>(),
                        ChatGPTRequest.StructuredOutputMode.NONE)
                .whenComplete((result, failure) -> session.execute(() -> {
                    if (!isCurrentPlayerContext(server, player, world) || !isConfigOperator(player)) {
                        return;
                    }
                    if (failure == null && result != null && result.content() != null && !result.content().isBlank()) {
                        sendConfigStatus(player, true, "CreatureChat AI test succeeded using model: " + config.getActiveModel());
                    } else {
                        String message = result != null && result.errorMessage() != null ? result.errorMessage() : "No response";
                        sendConfigStatus(player, false, "CreatureChat AI test failed: " + message);
                    }
                }));
    }

    private static void sendConfigStatus(ServerPlayer player, boolean success, String message) {
        MinecraftServer server = player.getServer();
        String maskedKeys = "";
        if (server != null) {
            ConfigurationHandler.Config config = new ConfigurationHandler(server).loadConfig();
            maskedKeys = ConfigurationScreenData.fromConfig(config).maskedApiKeys();
        }

        FriendlyByteBuf buffer = BufferHelper.create();
        buffer.writeBoolean(success);
        buffer.writeUtf(message);
        buffer.writeUtf(maskedKeys);
        PacketHelper.send(player, PACKET_S2C_CONFIG_STATUS, buffer);
    }

    private static boolean isConfigOperator(ServerPlayer player) {
        return player.createCommandSourceStack().hasPermission(4);
    }

    public static void generate_character(String userLanguage, EntityChatData chatData, ServerPlayer player, Mob entity, boolean is_auto_message) {
        ConfigurationHandler.Config config = new ConfigurationHandler(serverInstance).loadConfig();
        ChatDataManager manager = ChatDataManager.getServerInstance();
        manager.dispatchRequest(chatData, player,
                () -> manager.handleAutoResponse(chatData, player, is_auto_message, config),
                request -> generate_character(request, userLanguage, chatData, player, entity, is_auto_message, config));
    }

    private static void generate_character(ChatSession.Request request, String userLanguage, EntityChatData chatData,
                                           ServerPlayer player, Mob entity, boolean is_auto_message,
                                           ConfigurationHandler.Config config) {
        if (!request.isCurrent(chatData)) {
            return;
        }
        String effectiveLanguage = config.getGenerationLanguage().isEmpty()
                ? userLanguage
                : MinecraftLanguages.displayName(config.getGenerationLanguage());
        TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 3.5F);
        EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);

        String randomAdjective = Randomizer.getRandomMessage(Randomizer.RandomType.ADJECTIVE);
        String randomClass = Randomizer.getRandomMessage(Randomizer.RandomType.CLASS);
        String randomAlignment = Randomizer.getRandomMessage(Randomizer.RandomType.ALIGNMENT);
        String randomSpeakingStyle = Randomizer.getRandomMessage(Randomizer.RandomType.SPEAKING_STYLE);

        String randomLetter = Randomizer.RandomLetter();
        int randomSyllables = Randomizer.RandomNumber(5) + 1;

        StringBuilder userMessageBuilder = new StringBuilder();
        userMessageBuilder.append("Please generate a ").append(randomAdjective).append(" character. ");
        userMessageBuilder.append("This character is a ").append(randomClass).append(" class, who is ").append(randomAlignment).append(". ");
        if (entity.getCustomName() != null && !entity.getCustomName().getString().equals("N/A")) {
            userMessageBuilder.append("Their name is '").append(entity.getCustomName().getString()).append("'. ");
        } else {
            userMessageBuilder.append("Their name starts with the letter '").append(randomLetter)
                    .append("' and is ").append(randomSyllables).append(" syllables long. ");
        }
        userMessageBuilder.append("They speak in '").append(effectiveLanguage).append("' with a ").append(randomSpeakingStyle).append(" style.");

        chatData.generateCharacter(request, effectiveLanguage, player, userMessageBuilder.toString(), is_auto_message);

        if (entity instanceof ChatInventory chatInv) {
            Container inv = chatInv.creaturechat$getInventory();

            if (entity.level().isClientSide) return;

            if (entity instanceof AbstractChestedHorse chested && chested.hasChest()) {
                int chestStart = AbstractHorse.CHEST_SLOT_OFFSET + 1;
                int chestSlots = chested.getInventoryColumns() * 3;

                for (int i = 0; i < chestSlots && i < inv.getContainerSize(); i++) {
                    ItemStack stack = chested.getSlot(chestStart + i).get();
                    if (!stack.isEmpty()) {
                        inv.setItem(i, stack.copy());
                        chested.getSlot(chestStart + i).set(ItemStack.EMPTY);
                    }
                }
            }

            boolean empty = true;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                if (!inv.getItem(i).isEmpty()) {
                    empty = false;
                    break;
                }
            }

            if (empty && entity.level() instanceof ServerLevel level) {
                Holder<Biome> biome = level.getBiome(entity.blockPosition());
                ResourceLocation tableId = InventoryLootTables.forBiome(biome);
                LootTable table = LootTableHelper.get(level, tableId);
                LootParams params = new LootParams.Builder(level)
                        .withParameter(LootContextParams.ORIGIN, entity.position())
                        .withOptionalParameter(LootContextParams.THIS_ENTITY, entity)
                        .create(LootContextParamSets.COMMAND);
                List<ItemStack> stacks = table.getRandomItems(params);
                if (stacks.isEmpty()) {
                    LOGGER.info("No loot matched for {}", tableId);
                } else {
                    RandomSource random = entity.getRandom();
                    int limit = Math.min(3, Math.min(inv.getContainerSize(), stacks.size()));
                    List<Integer> slots = new ArrayList<>();
                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        slots.add(i);
                    }
                    Collections.shuffle(slots, new java.util.Random(random.nextLong()));
                    for (int i = 0; i < limit; i++) {
                        inv.setItem(slots.get(i), stacks.get(i));
                    }
                }
            }
        }
    }

    public static void generate_chat(String userLanguage, EntityChatData chatData, ServerPlayer player, Mob entity, String message, boolean is_auto_message) {
        ConfigurationHandler.Config config = new ConfigurationHandler(serverInstance).loadConfig();
        ChatDataManager manager = ChatDataManager.getServerInstance();
        manager.dispatchRequest(chatData, player,
                () -> manager.handleAutoResponse(chatData, player, is_auto_message, config),
                request -> generate_chat(request, userLanguage, chatData, player, entity, message, is_auto_message, config));
    }

    public static void generate_chat(ChatSession.Request request, String userLanguage, EntityChatData chatData,
                                    ServerPlayer player, Mob entity, String message, boolean is_auto_message,
                                    ConfigurationHandler.Config config) {
        if (request == null || !request.isCurrent(chatData)) {
            return;
        }
        String effectiveLanguage = config.getGenerationLanguage().isEmpty()
                ? userLanguage
                : MinecraftLanguages.displayName(config.getGenerationLanguage());

        TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 3.5F);
        EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);

        chatData.generateMessage(request, effectiveLanguage, player, message, is_auto_message, !is_auto_message);
    }

    public static void handleNearbyPlayerChat(ServerPlayer player, String chatMessage) {
        if (serverInstance == null || player == null || chatMessage == null || chatMessage.trim().isEmpty()) {
            return;
        }

        MinecraftServer server = player.getServer();
        ChatSession session = ChatDataManager.getServerInstance().getSession(server);
        Level playerLevel = player.level();
        if (session == null || !(playerLevel instanceof ServerLevel world)) {
            return;
        }
        session.execute(() -> {
            if (!isCurrentPlayerContext(server, player, world)) {
                return;
            }
            ConfigurationHandler.Config config = new ConfigurationHandler(server).loadConfig();
            if (!config.getProximityChatEnabled()) {
                return;
            }

            int remainingResponses = perMessageLimit(config.getMaxProximityResponsesPerMessage());
            int radius = ambientRadius(config.getProximityChatRadius());
            if (remainingResponses <= 0 || !(player.level() instanceof ServerLevel)) {
                return;
            }

            List<Mob> nearbyMobs = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius));
            nearbyMobs.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)));

            for (Mob mob : nearbyMobs) {
                if (mob.distanceToSqr(player) > radius * radius) {
                    continue;
                }
                EntityChatData mobChatData = getAmbientCandidateData(mob);
                if (mobChatData == null) {
                    continue;
                }

                String ambientMessage = nearbyPlayerContext(player, chatMessage);
                if (generate_ambient_chat("N/A", mobChatData, player, mob, ambientMessage, config, false)) {
                    remainingResponses--;
                    if (remainingResponses <= 0) {
                        break;
                    }
                }
            }
        });
    }

    public static void handleNearbyMobChat(ServerPlayer player, Mob sourceEntity, String sourceMessage, ConfigurationHandler.Config config) {
        if (serverInstance == null || player == null || sourceEntity == null || sourceMessage == null || sourceMessage.trim().isEmpty()) {
            return;
        }

        MinecraftServer server = player.getServer();
        ChatSession session = ChatDataManager.getServerInstance().getSession(server);
        if (session == null || !(sourceEntity.level() instanceof ServerLevel world)) {
            return;
        }
        EntityChatData sourceState = ChatDataManager.getServerInstance().entityChatDataMap.get(sourceEntity.getStringUUID());
        session.execute(() -> {
            if (!isCurrentPlayerContext(server, player, world) || !sourceEntity.isAlive() || sourceEntity.isRemoved()
                    || sourceEntity.level() != world || world.getEntity(sourceEntity.getUUID()) != sourceEntity
                    || ChatDataManager.getServerInstance().entityChatDataMap.get(sourceEntity.getStringUUID()) != sourceState) {
                return;
            }
            if (!config.getMobToMobChatEnabled()) {
                return;
            }

            int remainingResponses = perMessageLimit(config.getMaxMobToMobResponsesPerMessage());
            int radius = ambientRadius(config.getMobToMobChatRadius());
            if (remainingResponses <= 0 || !(sourceEntity.level() instanceof ServerLevel)) {
                return;
            }
            EntityChatData sourceChatData = getAmbientCandidateData(sourceEntity);
            if (sourceChatData == null || !ChatDataManager.getServerInstance().handleAmbientEntityResponse(sourceChatData, config)) {
                return;
            }

            List<Mob> nearbyMobs = sourceEntity.level().getEntitiesOfClass(Mob.class, sourceEntity.getBoundingBox().inflate(radius));
            nearbyMobs.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(sourceEntity)));

            for (Mob mob : nearbyMobs) {
                if (mob == sourceEntity || mob.distanceToSqr(sourceEntity) > radius * radius) {
                    continue;
                }
                EntityChatData mobChatData = getAmbientCandidateData(mob);
                if (mobChatData == null) {
                    continue;
                }

                String ambientMessage = nearbyMobContext(sourceEntity, sourceMessage);
                if (generate_ambient_chat("N/A", mobChatData, player, mob, ambientMessage, config, false)) {
                    mobChatData.rememberRumor(sourceEntity.getDisplayName().getString() + " said nearby: " + cleanAmbientInput(sourceMessage));
                    remainingResponses--;
                    if (remainingResponses <= 0) {
                        break;
                    }
                }
            }
        });
    }

    private static boolean isCurrentPlayerContext(MinecraftServer server, ServerPlayer player, ServerLevel world) {
        return server != null && serverInstance == server && server.getLevel(world.dimension()) == world
                && player.level() == world && player.isAlive() && !player.isRemoved()
                && server.getPlayerList().getPlayer(player.getUUID()) == player;
    }

    public static boolean generate_ambient_chat(String userLanguage, EntityChatData chatData, ServerPlayer player, Mob entity, String message,
                                                ConfigurationHandler.Config config, boolean allow_mob_to_mob_reactions) {
        ChatDataManager manager = ChatDataManager.getServerInstance();
        return manager.dispatchRequest(chatData, player,
                () -> manager.handleAmbientResponse(chatData, player, config), request -> {
                    String effectiveLanguage = config.getGenerationLanguage().isEmpty()
                            ? userLanguage
                            : MinecraftLanguages.displayName(config.getGenerationLanguage());

                    TalkPlayerGoal talkGoal = new TalkPlayerGoal(player, entity, 3.5F);
                    EntityBehaviorManager.addGoal(entity, talkGoal, GoalPriority.TALK_PLAYER);

                    chatData.generateMessage(request, effectiveLanguage, player, message, true, allow_mob_to_mob_reactions);
                });
    }

    private static int perMessageLimit(int configuredLimit) {
        return Math.max(0, Math.min(configuredLimit, 8));
    }

    private static int ambientRadius(int configuredRadius) {
        return Math.max(1, Math.min(configuredRadius, 64));
    }

    private static EntityChatData getAmbientCandidateData(Mob mob) {
        if (mob == null || !mob.isAlive()) {
            return null;
        }
        EntityChatData chatData = ChatDataManager.getServerInstance().entityChatDataMap.get(mob.getStringUUID());
        if (chatData == null || chatData.characterSheet == null || chatData.characterSheet.isEmpty()) {
            return null;
        }
        if (chatData.status == ChatDataManager.ChatStatus.PENDING) {
            return null;
        }
        return chatData;
    }

    private static String nearbyPlayerContext(ServerPlayer player, String chatMessage) {
        String playerName = player.getDisplayName().getString();
        String cleanMessage = cleanAmbientInput(chatMessage);
        return "[Nearby player chat. Respond only if your character naturally would. " + playerName + " said nearby: " + cleanMessage + "]";
    }

    private static String nearbyMobContext(Mob sourceEntity, String sourceMessage) {
        String mobName = sourceEntity.getDisplayName().getString();
        String cleanMessage = cleanAmbientInput(sourceMessage);
        return "[Nearby mob chat. Respond only if your character naturally would. " + mobName + " said nearby: " + cleanMessage + "]";
    }

    private static String cleanAmbientInput(String message) {
        if (message == null) {
            return "";
        }
        String singleLine = message.replace('\n', ' ').replace('\r', ' ').trim();
        return EntityChatData.truncateString(singleLine, AMBIENT_CONTEXT_MAX_CHARS);
    }

    public static void writeViewerRelationship(FriendlyByteBuf buffer, EntityChatData chatData,
                                               String playerId, String playerName) {
        PlayerData data = chatData.getPlayerDataForDisplay(playerId, playerName);
        buffer.writeInt(1);
        buffer.writeUtf(playerId);
        buffer.writeInt(data.friendship);
        buffer.writeInt(data.socialReputation);
        buffer.writeInt(data.helpfulActions);
        buffer.writeInt(data.harmfulActions);
        buffer.writeInt(data.socialEventCount);
        buffer.writeUtf(data.socialSummary == null ? "" : data.socialSummary);
    }

    public static void BroadcastEntityMessage(EntityChatData chatData) {
        if (serverInstance == null) {
            LOGGER.warn("Skipping entity message broadcast because the server instance is not available.");
            return;
        }
        LOGGER.info("Broadcasting entity message: entityId={}, status={}, currentMessage={}, currentLineNumber={}, senderType={}",
                chatData.entityId, chatData.status,
                chatData.currentMessage.length() > 24 ? chatData.currentMessage.substring(0, 24) + "..." : chatData.currentMessage,
                chatData.currentLineNumber, chatData.sender);

        for (ServerLevel world : serverInstance.getAllLevels()) {
            UUID entityId = UUID.fromString(chatData.entityId);
            Mob entity = (Mob)ServerEntityFinder.getEntityByUUID(world, entityId);
            if (entity != null) {
                String characterName = chatData.getCharacterProp("name");
                if (!characterName.isEmpty() && !characterName.equals("N/A") && entity.getCustomName() == null) {
                    LOGGER.debug("Setting entity name to " + characterName + " for " + chatData.entityId);
                    entity.setCustomName(Component.literal(characterName));
                    entity.setCustomNameVisible(true);
                    entity.setPersistenceRequired();
                }

                if (chatData.sender == ChatDataManager.ChatSender.USER && chatData.auto_generated > 0) {
                    chatData.status = ChatDataManager.ChatStatus.PENDING;
                }

                for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
                    if (!playerPreferences.canReceiveEntityUpdate(player.getUUID(), chatData.currentPlayerId, chatData.sender)) {
                        continue;
                    }
                    FriendlyByteBuf buffer = BufferHelper.create();
                    buffer.writeUtf(chatData.entityId);
                    buffer.writeUtf(chatData.currentMessage);
                    buffer.writeInt(chatData.currentLineNumber);
                    buffer.writeUtf(chatData.status.toString());
                    buffer.writeUtf(chatData.sender.toString());
                    writeViewerRelationship(buffer, chatData, player.getStringUUID(), player.getDisplayName().getString());

                    PacketHelper.send(player, PACKET_S2C_ENTITY_MESSAGE, buffer);
                }
                break;
            }
        }
    }

    public static void BroadcastPlayerMessage(EntityChatData chatData, ServerPlayer sender) {
        if (serverInstance == null) {
            LOGGER.warn("Skipping player message broadcast because the server instance is not available.");
            return;
        }
        LOGGER.info("Broadcasting player message: senderUUID={}, message={}", sender.getStringUUID(),
                chatData.currentMessage);

        FriendlyByteBuf buffer = BufferHelper.create();

        buffer.writeUtf(sender.getStringUUID());
        buffer.writeUtf(sender.getDisplayName().getString());
        buffer.writeUtf(chatData.currentMessage);

        for (ServerPlayer serverPlayer : serverInstance.getPlayerList().getPlayers()) {
            PacketHelper.send(serverPlayer, PACKET_S2C_PLAYER_MESSAGE, buffer);
        }
    }

    public static void SendEntityReplyToChat(EntityChatData chatData, Mob entity, String message) {
        if (serverInstance == null) {
            LOGGER.warn("Skipping entity chat message because the server instance is not available.");
            return;
        }
        if (chatData == null || entity == null || message == null || message.isBlank()) {
            return;
        }

        Component chatMessage = Component.literal("[" + entity.getDisplayName().getString() + "] " + message);
        for (ServerPlayer serverPlayer : serverInstance.getPlayerList().getPlayers()) {
            if (playerPreferences.canReceiveNpcReply(serverPlayer.getUUID(), chatData.currentPlayerId)) {
                serverPlayer.sendSystemMessage(chatMessage);
            }
        }
    }

    public static void BroadcastPlayerStatus(Player player, boolean isChatOpen) {
        if (serverInstance == null) {
            LOGGER.warn("Skipping player status broadcast because the server instance is not available.");
            return;
        }
        FriendlyByteBuf buffer = BufferHelper.create();

        buffer.writeUtf(player.getStringUUID());
        buffer.writeBoolean(isChatOpen);

        for (ServerPlayer serverPlayer : serverInstance.getPlayerList().getPlayers()) {
            LOGGER.debug("Server broadcast " + player.getDisplayName().getString() + " player status to client: " + serverPlayer.getDisplayName().getString() + " | isChatOpen: " + isChatOpen);
            PacketHelper.send(serverPlayer, PACKET_S2C_PLAYER_STATUS, buffer);
        }
    }

    public static void BroadcastMessage(Component message) {
        if (serverInstance == null) {
            LOGGER.warn("Skipping server chat broadcast because the server instance is not available.");
            return;
        }
        for (ServerPlayer serverPlayer : serverInstance.getPlayerList().getPlayers()) {
            serverPlayer.displayClientMessage(message, false);
        };
    }

    public static void sendErrorToAllOps(MinecraftServer server, String message) {
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (server.getPlayerList().isOp(player.getGameProfile())) {
                player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), false);
            }
        }
    }
}
