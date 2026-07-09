// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class ModJarLocator {
    private ModJarLocator() {
    }

    public static Optional<Path> findInstalledModJar(Path modsDir, String modId, String currentVersion, String archiveBaseName) {
        if (modsDir == null || !Files.isDirectory(modsDir)) {
            return Optional.empty();
        }

        try (Stream<Path> files = Files.list(modsDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> isCandidateName(path, archiveBaseName))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .filter(path -> hasMetadata(path, modId, currentVersion))
                    .map(path -> path.toAbsolutePath().normalize())
                    .findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static boolean isCandidateName(Path path, String archiveBaseName) {
        String name = path.getFileName().toString();
        return (name.equals(InstalledJarNames.STABLE_INSTALLED_JAR) || name.startsWith(archiveBaseName + "-"))
                && name.endsWith(".jar")
                && !name.endsWith("-sources.jar")
                && !name.contains("_mapped_");
    }

    private static boolean hasMetadata(Path path, String modId, String currentVersion) {
        try (JarFile jar = new JarFile(path.toFile())) {
            JarEntry metadataEntry = jar.getJarEntry("fabric.mod.json");
            if (metadataEntry == null) {
                return false;
            }
            try (Reader reader = new InputStreamReader(jar.getInputStream(metadataEntry), StandardCharsets.UTF_8)) {
                JsonObject metadata = JsonParser.parseReader(reader).getAsJsonObject();
                String id = metadata.has("id") ? metadata.get("id").getAsString() : "";
                String version = metadata.has("version") ? metadata.get("version").getAsString() : "";
                return modId.equals(id) && currentVersion.equals(version);
            }
        } catch (Exception e) {
            return false;
        }
    }
}
