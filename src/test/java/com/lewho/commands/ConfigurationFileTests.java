// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationFileTests {
    @TempDir
    Path directory;

    @Test
    void fallsBackOnlyWhenTheWorldConfigurationIsMissing() throws Exception {
        Path worldConfig = directory.resolve("world.json");
        Path defaultConfig = directory.resolve("root.json");
        Files.writeString(defaultConfig, "{\"apiKey\":\"root-fixture-key\"}");
        ConfigurationHandler handler = new ConfigurationHandler(worldConfig, defaultConfig);

        assertEquals("root-fixture-key", handler.loadConfig().getActiveApiKey());
        Files.writeString(worldConfig, "{\"apiKey\":\"incomplete-fixture-key\"");
        assertEquals("", handler.loadConfig().getActiveApiKey());
        assertTrue(Files.readString(worldConfig).contains("incomplete-fixture-key"));
    }

    @Test
    void keepsStoredConfigurationWhenTheReplacementCannotBeSerialized() throws Exception {
        Path worldConfig = directory.resolve("world.json");
        ConfigurationHandler handler = new ConfigurationHandler(worldConfig, directory.resolve("root.json"));
        ConfigurationHandler.Config config = new ConfigurationHandler.Config();
        config.setStory("Мандрівник 🐈");
        assertTrue(handler.saveConfig(config, true));
        assertEquals("Мандрівник 🐈", handler.loadConfig().getStory());
        String original = Files.readString(worldConfig);

        config.setPercentOfContext(Double.NaN);
        assertFalse(handler.saveConfig(config, true));
        assertEquals(original, Files.readString(worldConfig));
    }
}
