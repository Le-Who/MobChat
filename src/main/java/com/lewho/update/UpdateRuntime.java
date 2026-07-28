// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class UpdateRuntime {
    public static final String MOD_ID = "creaturechat";
    public static final String ARCHIVE_BASE_NAME = "creaturechat";
    public static final String GITHUB_OWNER = "Le-Who";
    public static final String GITHUB_REPO = "MobChat";

    private UpdateRuntime() {
    }

    public static Optional<RuntimeModInfo> detect() {
        FabricLoader loader = FabricLoader.getInstance();
        Optional<ModContainer> mod = loader.getModContainer(MOD_ID);
        if (mod.isEmpty()) {
            return Optional.empty();
        }

        String currentVersion = mod.get().getMetadata().getVersion().getFriendlyString();
        String minecraftVersion = UpdateVersion.minecraftVersion(currentVersion);
        if (minecraftVersion.isBlank()) {
            minecraftVersion = loader.getModContainer("minecraft")
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("");
        }

        Path gameDir = loader.getGameDir().toAbsolutePath().normalize();
        Optional<Path> currentJar = ModJarLocator.findInstalledModJar(
                gameDir.resolve("mods"),
                MOD_ID,
                currentVersion,
                ARCHIVE_BASE_NAME
        ).or(() -> currentJar(mod.get()));
        if (currentJar.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new RuntimeModInfo(
                ARCHIVE_BASE_NAME,
                currentVersion,
                minecraftVersion,
                currentJar.get().toAbsolutePath().normalize(),
                gameDir,
                javaExecutable()
        ));
    }

    private static Optional<Path> currentJar(ModContainer mod) {
        for (Path path : mod.getOrigin().getPaths()) {
            if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar")) {
                return Optional.of(path);
            }
        }
        @SuppressWarnings("deprecation")
        Path root = mod.getRootPath();
        if (Files.isRegularFile(root) && root.getFileName().toString().endsWith(".jar")) {
            return Optional.of(root);
        }
        return Optional.empty();
    }

    private static Path javaExecutable() {
        String executable = System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable);
    }
}
