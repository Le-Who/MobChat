// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho;

import com.lewho.commands.CreatureChatCommands;
import com.lewho.inventory.ModMenus;
import com.lewho.network.ServerPackets;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModInit implements ModInitializer {
        public static final String MODID = "creaturechat";
        public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	@Override
	public void onInitialize() {

                CreatureChatCommands.register();

                ModMenus.register();
                ServerPackets.register();

		LOGGER.info("CreatureChat MOD Initialized!");
	}
}