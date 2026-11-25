// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.ui;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;

import java.util.concurrent.atomic.AtomicInteger;

public class PlayerMessage extends EntityChatData {
    public AtomicInteger tickCountdown;

    public PlayerMessage(String playerId, String messageText, int ticks) {
        super(playerId);
        this.currentMessage = messageText;
        this.currentLineNumber = 0;
        this.tickCountdown = new AtomicInteger(ticks);
        this.status = ChatDataManager.ChatStatus.DISPLAY;
    }
}