// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.chat;

import net.minecraft.server.MinecraftServer;

/**
 * The {@code ChatDataAutoSaver} class is a Runnable task, which autosaves the server chat data to JSON.
 * It can be scheduled with the {@code ChatDataSaverScheduler} class.
 */
public class ChatDataAutoSaver implements Runnable {
    private final MinecraftServer server;
    private final ChatSession session;

    public ChatDataAutoSaver(MinecraftServer server) {
        this.server = server;
        this.session = ChatDataManager.getServerInstance().getSession(server);
    }

    @Override
    public void run() {
        if (session != null) {
            session.execute(() -> ChatDataManager.getServerInstance().saveChatData(server));
        }
    }
}
