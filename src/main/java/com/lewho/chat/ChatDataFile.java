// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.chat;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.lewho.utils.AtomicFile;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatDataFile {
    private static final Gson GSON = new Gson();
    private static final Type CHAT_DATA_TYPE = new TypeToken<ConcurrentHashMap<String, EntityChatData>>() {}.getType();

    private final Path path;
    private boolean loadFailed;

    public ChatDataFile(Path path) {
        this.path = path;
    }

    public ConcurrentHashMap<String, EntityChatData> load() throws IOException {
        loadFailed = true;
        ConcurrentHashMap<String, EntityChatData> data;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            data = GSON.fromJson(reader, CHAT_DATA_TYPE);
            if (data == null) {
                throw new JsonSyntaxException("Expected an object containing saved NPC data");
            }
            data.values().removeIf(entity -> entity.status == ChatDataManager.ChatStatus.NONE);
            data.values().forEach(EntityChatData::postDeserializeInitialization);
        } catch (NoSuchFileException e) {
            data = new ConcurrentHashMap<>();
        }
        loadFailed = false;
        return data;
    }

    public boolean save(Object snapshot) throws IOException {
        if (loadFailed) {
            return false;
        }
        AtomicFile.replace(path, GSON.toJson(snapshot).getBytes(StandardCharsets.UTF_8));
        return true;
    }
}
