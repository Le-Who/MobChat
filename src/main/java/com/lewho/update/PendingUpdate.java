// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.lewho.utils.AtomicFile;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public record PendingUpdate(
        String version,
        Path currentJar,
        Path stagedJar,
        Path backupJar,
        Path pendingFile,
        String sha512
) {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public void save() throws IOException {
        Files.createDirectories(pendingFile.getParent());
        JsonObject json = new JsonObject();
        json.addProperty("version", version);
        json.addProperty("currentJar", currentJar.toAbsolutePath().normalize().toString());
        json.addProperty("stagedJar", stagedJar.toAbsolutePath().normalize().toString());
        json.addProperty("backupJar", backupJar.toAbsolutePath().normalize().toString());
        json.addProperty("sha512", UpdateHashes.normalizeSha512(sha512));
        AtomicFile.replace(pendingFile, GSON.toJson(json).getBytes(StandardCharsets.UTF_8));
    }

    public static PendingUpdate load(Path pendingFile) throws IOException {
        try (Reader reader = Files.newBufferedReader(pendingFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                throw new IOException("Pending update metadata is empty.");
            }
            return new PendingUpdate(
                    stringValue(json, "version"),
                    Paths.get(stringValue(json, "currentJar")),
                    Paths.get(stringValue(json, "stagedJar")),
                    Paths.get(stringValue(json, "backupJar")),
                    pendingFile,
                    stringValue(json, "sha512")
            );
        }
    }

    private static String stringValue(JsonObject json, String key) throws IOException {
        if (!json.has(key) || !json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isString()) {
            throw new IOException("Pending update metadata is missing a string for " + key + ".");
        }
        return json.get(key).getAsString();
    }
}
