// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.chat;

import net.minecraft.server.MinecraftServer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;

/**
 * The {@code ChatDataSaverScheduler} class is used to start the auto save Runnable task and schedule it.
 */
public class ChatDataSaverScheduler {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ChatSession session;

    public void startAutoSaveTask(MinecraftServer server, long interval, TimeUnit timeUnit) {
        this.session = ChatDataManager.getServerInstance().getSession(server);
        ChatDataAutoSaver saverTask = new ChatDataAutoSaver(server);
        scheduler.scheduleAtFixedRate(saverTask, 1, interval, timeUnit);
    }

    public void stopAutoSaveTask() {
        scheduler.shutdownNow();
    }

    // Schedule a task to run after 1 tick (basically immediately)
    public void scheduleTask(Runnable task) {
        ChatSession originatingSession = session;
        if (originatingSession == null || !originatingSession.isOpen()) {
            return;
        }
        try {
            scheduler.schedule(() -> originatingSession.execute(task), 50, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            // Server shutdown already cancelled this scheduler's remaining work.
        }
    }
}
