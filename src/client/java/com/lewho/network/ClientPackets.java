// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.network;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import com.lewho.chat.ChatHistoryEntry;
import com.lewho.chat.PlayerData;
import com.lewho.commands.ConfigurationScreenData;
import com.lewho.ui.ChatScreen;
import com.lewho.ui.CreatureChatConfigScreen;
import com.lewho.ui.LanguageSelectorScreen;
import com.lewho.ui.BubbleRenderer;
import com.lewho.ui.PlayerMessageManager;
import com.lewho.utils.ClientEntityFinder;
import com.lewho.utils.Compression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.DataFormatException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public class ClientPackets {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static final LoginChatChunks loginChatChunks = new LoginChatChunks();
    private static final Type CHAT_SNAPSHOT_TYPE = new TypeToken<Map<String, EntityChatData>>() {}.getType();

    public static void reset() {
        loginChatChunks.clear();
        ChatDataManager.getClientInstance().clearData();
        PlayerMessageManager.clear();
        BubbleRenderer.whitelist = List.of();
        BubbleRenderer.blacklist = List.of();
    }

    public static void sendGenerateGreeting(Entity entity) {
        String userLanguageCode = Minecraft.getInstance().getLanguageManager().getSelected();
        String userLanguageName = Minecraft.getInstance().getLanguageManager().getLanguage(userLanguageCode).toComponent().getString();

        FriendlyByteBuf buf = ClientBufferHelper.create();
        buf.writeUtf(entity.getStringUUID());
        buf.writeUtf(userLanguageName);

        ClientPacketHelper.send(ServerPackets.PACKET_C2S_GREETING, buf);
    }

    public static void sendUpdateLineNumber(Entity entity, Integer lineNumber) {
        FriendlyByteBuf buf = ClientBufferHelper.create();
        buf.writeUtf(entity.getStringUUID());
        buf.writeInt(lineNumber);

        ClientPacketHelper.send(ServerPackets.PACKET_C2S_READ_NEXT, buf);
    }

    public static void sendOpenChat(Entity entity) {
        FriendlyByteBuf buf = ClientBufferHelper.create();
        buf.writeUtf(entity.getStringUUID());

        ClientPacketHelper.send(ServerPackets.PACKET_C2S_OPEN_CHAT, buf);
    }

    public static void sendCloseChat() {
        FriendlyByteBuf buf = ClientBufferHelper.create();

        ClientPacketHelper.send(ServerPackets.PACKET_C2S_CLOSE_CHAT, buf);
    }

    public static void setChatStatus(Entity entity, ChatDataManager.ChatStatus new_status) {
        FriendlyByteBuf buf = ClientBufferHelper.create();
        buf.writeUtf(entity.getStringUUID());
        buf.writeUtf(new_status.toString());

        ClientPacketHelper.send(ServerPackets.PACKET_C2S_SET_STATUS, buf);
    }

    public static void sendChat(Entity entity, String message) {
        String userLanguageCode = Minecraft.getInstance().getLanguageManager().getSelected();
        String userLanguageName = Minecraft.getInstance().getLanguageManager().getLanguage(userLanguageCode).toComponent().getString();

        FriendlyByteBuf buf = ClientBufferHelper.create();
        buf.writeUtf(entity.getStringUUID());
        buf.writeUtf(message);
        buf.writeUtf(userLanguageName);

        ClientPacketHelper.send(ServerPackets.PACKET_C2S_SEND_CHAT, buf);
    }

    public static void sendRequestHistory(Entity entity) {
        FriendlyByteBuf buf = ClientBufferHelper.create();
        buf.writeUtf(entity.getStringUUID());

        ClientPacketHelper.send(ServerPackets.PACKET_C2S_REQUEST_HISTORY, buf);
    }

    public static void sendConfigSave(ConfigurationScreenData.SaveData data) {
        FriendlyByteBuf buf = ClientBufferHelper.create();
        buf.writeUtf(data.provider());
        buf.writeUtf(data.url());
        buf.writeUtf(data.apiKeys());
        buf.writeUtf(data.models());
        buf.writeInt(data.timeout());
        buf.writeInt(data.maxOutputTokens());
        buf.writeUtf(data.thinkingLevel());
        buf.writeUtf(data.generationLanguage() != null ? data.generationLanguage() : "");

        ClientPacketHelper.send(ServerPackets.PACKET_C2S_CONFIG_SAVE, buf);
    }

    public static void sendConfigTest(ConfigurationScreenData.SaveData data) {
        FriendlyByteBuf buf = ClientBufferHelper.create();
        buf.writeUtf(data.provider());
        buf.writeUtf(data.url());
        buf.writeUtf(data.apiKeys());
        buf.writeUtf(data.models());
        buf.writeInt(data.timeout());
        buf.writeInt(data.maxOutputTokens());
        buf.writeUtf(data.thinkingLevel());
        buf.writeUtf(data.generationLanguage() != null ? data.generationLanguage() : "");

        ClientPacketHelper.send(ServerPackets.PACKET_C2S_CONFIG_TEST, buf);
    }

    public static Map<String, PlayerData> readPlayerDataMap(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        Map<String, PlayerData> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buffer.readUtf();
            PlayerData data = new PlayerData();
            data.friendship = buffer.readInt();
            data.socialReputation = buffer.readInt();
            data.helpfulActions = buffer.readInt();
            data.harmfulActions = buffer.readInt();
            data.socialEventCount = buffer.readInt();
            data.socialSummary = buffer.readUtf(32767);
            map.put(key, data);
        }
        return map;
    }

    public static void register() {
        ClientPacketHelper.registerReceiver(ServerPackets.PACKET_S2C_ENTITY_MESSAGE, (client, handler, buffer, responseSender) -> {
            UUID entityId = UUID.fromString(buffer.readUtf());
            String message = buffer.readUtf(32767);
            int line = buffer.readInt();
            String status_name = buffer.readUtf(32767);
            ChatDataManager.ChatStatus status = ChatDataManager.ChatStatus.valueOf(status_name);
            String sender_name = buffer.readUtf(32767);
            ChatDataManager.ChatSender sender = ChatDataManager.ChatSender.valueOf(sender_name);
            Map<String, PlayerData> players = readPlayerDataMap(buffer);

            executeForConnection(client, handler, () -> {
                if (client.player == null || client.level == null) {
                    LOGGER.warn("Client not fully initialized. Dropping message for entity '{}'.", entityId);
                    return;
                }

                ChatDataManager chatDataManager = ChatDataManager.getClientInstance();
                EntityChatData chatData = chatDataManager.getOrCreateChatData(entityId.toString());

                if (!message.isEmpty()) {
                    chatData.currentMessage = message;
                }
                chatData.currentLineNumber = line;
                chatData.status = status;
                chatData.sender = sender;
                chatData.players = players;

                Mob entity = ClientEntityFinder.getEntityByUUID(client.level, entityId);
                if (entity != null) {
                    playNearbyUISound(client, entity, 0.2f);
                }
            });
        });

        ClientPacketHelper.registerReceiver(ServerPackets.PACKET_S2C_PLAYER_MESSAGE, (client, handler, buffer, responseSender) -> {
            UUID senderPlayerId = UUID.fromString(buffer.readUtf());
            String senderPlayerName = buffer.readUtf(32767);
            String message = buffer.readUtf(32767);

            executeForConnection(client, handler, () -> {
                if (client.player == null || client.level == null) {
                    LOGGER.warn("Client not fully initialized. Dropping message for sender '{}'.", senderPlayerId);
                    return;
                }

                PlayerMessageManager.addMessage(senderPlayerId, message, senderPlayerName, ChatDataManager.TICKS_TO_DISPLAY_USER_MESSAGE);
            });
        });

        ClientPacketHelper.registerReceiver(ServerPackets.PACKET_S2C_LOGIN, (client, handler, buffer, responseSender) -> {
            int sequenceNumber = buffer.readInt();
            int totalPackets = buffer.readInt();
            byte[] chunk = buffer.readByteArray();

            executeForConnection(client, handler, () -> {
                byte[] compressedSnapshot = loginChatChunks.accept(sequenceNumber, totalPackets, chunk);
                if (compressedSnapshot == null) {
                    return;
                }

                try {
                    String json = Compression.decompressString(compressedSnapshot);
                    Map<String, EntityChatData> snapshot = new Gson().fromJson(json, CHAT_SNAPSHOT_TYPE);
                    if (snapshot == null || snapshot.containsValue(null)) {
                        LOGGER.warn("Ignoring invalid login chat data.");
                        return;
                    }
                    ChatDataManager.getClientInstance().entityChatDataMap = new ConcurrentHashMap<>(snapshot);
                } catch (DataFormatException | JsonParseException e) {
                    LOGGER.warn("Could not decode login chat data.");
                }
            });
        });

        ClientPacketHelper.registerReceiver(ServerPackets.PACKET_S2C_WHITELIST, (client, handler, buffer, responseSender) -> {
            int whitelistSize = buffer.readInt();
            List<String> whitelist = new ArrayList<>(whitelistSize);
            for (int i = 0; i < whitelistSize; i++) {
                whitelist.add(buffer.readUtf(32767));
            }

            int blacklistSize = buffer.readInt();
            List<String> blacklist = new ArrayList<>(blacklistSize);
            for (int i = 0; i < blacklistSize; i++) {
                blacklist.add(buffer.readUtf(32767));
            }

            executeForConnection(client, handler, () -> {
                BubbleRenderer.whitelist = whitelist;
                BubbleRenderer.blacklist = blacklist;
            });
        });

        ClientPacketHelper.registerReceiver(ServerPackets.PACKET_S2C_PLAYER_STATUS, (client, handler, buffer, responseSender) -> {
            UUID playerId = UUID.fromString(buffer.readUtf());
            boolean isChatOpen = buffer.readBoolean();

            executeForConnection(client, handler, () -> {
                if (isChatOpen) {
                    PlayerMessageManager.openChatUI(playerId);
                    Player player = client.level == null ? null : client.level.getPlayerByUUID(playerId);
                    if (player != null) {
                        playNearbyUISound(client, player, 0.2f);
                    }
                } else {
                    PlayerMessageManager.closeChatUI(playerId);
                }
            });
        });

        ClientPacketHelper.registerReceiver(ServerPackets.PACKET_S2C_CONFIG_OPEN, (client, handler, buffer, responseSender) -> {
            ConfigurationScreenData.OpenData data = new ConfigurationScreenData.OpenData(
                    buffer.readUtf(32767),
                    buffer.readUtf(32767),
                    buffer.readUtf(32767),
                    buffer.readUtf(32767),
                    buffer.readUtf(32767),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readUtf(32767),
                    buffer.readUtf(32767)
            );

            executeForConnection(client, handler, () -> client.setScreen(new CreatureChatConfigScreen(data)));
        });

        ClientPacketHelper.registerReceiver(ServerPackets.PACKET_S2C_CHAT_HISTORY, (client, handler, buffer, responseSender) -> {
            UUID entityId = UUID.fromString(buffer.readUtf());
            int historySize = buffer.readInt();
            List<ChatHistoryEntry> entries = new ArrayList<>(historySize);
            for (int i = 0; i < historySize; i++) {
                ChatDataManager.ChatSender sender = ChatDataManager.ChatSender.valueOf(buffer.readUtf(32767));
                String name = buffer.readUtf(32767);
                String message = buffer.readUtf(32767);
                long timestamp = buffer.readLong();
                entries.add(new ChatHistoryEntry(message, sender, name, timestamp));
            }

            executeForConnection(client, handler, () -> {
                if (client.screen instanceof ChatScreen screen) {
                    screen.updateHistory(entityId, entries);
                }
            });
        });

        ClientPacketHelper.registerReceiver(ServerPackets.PACKET_S2C_CONFIG_STATUS, (client, handler, buffer, responseSender) -> {
            boolean success = buffer.readBoolean();
            String message = buffer.readUtf(32767);
            String maskedKeys = buffer.readUtf(32767);

            executeForConnection(client, handler, () -> {
                Screen activeScreen = client.screen;
                if (activeScreen instanceof LanguageSelectorScreen languageScreen) {
                    activeScreen = languageScreen.getParentScreen();
                }
                if (activeScreen instanceof CreatureChatConfigScreen screen) {
                    screen.updateServerStatus(success, message, maskedKeys);
                } else if (client.player != null) {
                    client.player.displayClientMessage(Component.literal(message), false);
                }
            });
        });
    }

    private static void executeForConnection(Minecraft client, ClientPacketListener handler, Runnable task) {
        client.execute(() -> {
            if (handler != null && handler == client.getConnection()) {
                task.run();
            }
        });
    }

    private static void playNearbyUISound(Minecraft client, Entity player, float maxVolume) {
        int distance_squared = 144;
        if (client.player != null) {
            double distance = client.player.distanceToSqr(player.getX(), player.getY(), player.getZ());
            if (distance <= distance_squared) {
                float volume = maxVolume - (float)distance / distance_squared * maxVolume;
                client.player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), volume, 0.8F);
            }
        }
    }
}

