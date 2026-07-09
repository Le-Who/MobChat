// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.tests;

import com.lewho.update.ModJarLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModJarLocatorTests {
    @TempDir
    Path tempDir;

    @Test
    public void findsInstalledModsJarByFabricMetadata() throws Exception {
        Path modsDir = tempDir.resolve("mods");
        Path installedJar = modsDir.resolve("creaturechat-3.0.0+1.20.1.jar");
        Path runtimeJar = tempDir.resolve("connector-cache").resolve("creaturechat-3.0.0+1.20.1_mapped_srg_1.20.1.jar");
        writeModJar(installedJar, "creaturechat", "3.0.0+1.20.1");
        writeModJar(runtimeJar, "creaturechat", "3.0.0+1.20.1");

        Optional<Path> found = ModJarLocator.findInstalledModJar(
                modsDir,
                "creaturechat",
                "3.0.0+1.20.1",
                "creaturechat"
        );

        assertTrue(found.isPresent());
        assertEquals(installedJar.toAbsolutePath().normalize(), found.get());
    }

    @Test
    public void ignoresSourceJarAndWrongModVersion() throws Exception {
        Path modsDir = tempDir.resolve("mods");
        writeModJar(modsDir.resolve("creaturechat-3.0.0+1.20.1-sources.jar"), "creaturechat", "3.0.0+1.20.1");
        writeModJar(modsDir.resolve("creaturechat-2.9.0+1.20.1.jar"), "creaturechat", "2.9.0+1.20.1");
        Path installedJar = modsDir.resolve("creaturechat-3.0.0+1.20.1.jar");
        writeModJar(installedJar, "creaturechat", "3.0.0+1.20.1");

        Optional<Path> found = ModJarLocator.findInstalledModJar(
                modsDir,
                "creaturechat",
                "3.0.0+1.20.1",
                "creaturechat"
        );

        assertTrue(found.isPresent());
        assertEquals(installedJar.toAbsolutePath().normalize(), found.get());
    }

    @Test
    public void findsStableInstalledJarName() throws Exception {
        Path modsDir = tempDir.resolve("mods");
        Path installedJar = modsDir.resolve("creaturechat.jar");
        writeModJar(installedJar, "creaturechat", "3.0.0+1.20.1");

        Optional<Path> found = ModJarLocator.findInstalledModJar(
                modsDir,
                "creaturechat",
                "3.0.0+1.20.1",
                "creaturechat"
        );

        assertTrue(found.isPresent());
        assertEquals(installedJar.toAbsolutePath().normalize(), found.get());
    }

    private void writeModJar(Path path, String modId, String version) throws IOException {
        Files.createDirectories(path.getParent());
        String metadata = """
                {
                  "schemaVersion": 1,
                  "id": "%s",
                  "version": "%s"
                }
                """.formatted(modId, version);
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(path))) {
            jar.putNextEntry(new JarEntry("fabric.mod.json"));
            jar.write(metadata.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
    }
}
