// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.ui;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.ChatHistoryEntry;
import com.lewho.chat.LineWrapper;
import com.lewho.network.ClientPackets;
import com.lewho.utils.TextureLoader;
import com.lewho.i18n.CCText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The ChatScreen class displays a chat dialog UI for the player
 * and handles keyboard entry events.
 */
public class ChatScreen extends ScreenHelper {
    // Chat background size
    private static final int CHAT_BACKGROUND_WIDTH   = 261;
    private static final int CHAT_BACKGROUND_HEIGHT   = 88;
    private static final int HISTORY_PANEL_MAX_HEIGHT = 108;
    private static final int HISTORY_PANEL_MIN_HEIGHT = 48;
    private static final int HISTORY_PANEL_GAP = 6;
    private static final int HISTORY_PANEL_MARGIN = 10;
    private static final int HISTORY_LINE_CHARS = 42;
    private static final int HISTORY_LINE_SPACING = 2;
    private static final int HISTORY_COLOR_TITLE = 0xFFE6E6E6;
    private static final int HISTORY_COLOR_EMPTY = 0xFFAAAAAA;
    private static final int HISTORY_COLOR_MESSAGE = 0xFFFFFFFF;
    private static final int HISTORY_COLOR_NPC = 0xFFFFD37A;
    private static final int HISTORY_COLOR_PLAYER = 0xFF8FD7FF;

    // Chat bubble title (enter your message)
    private static final int CHAT_TITLE_OFFSET = 13;

    // text input margins and size
    private static final int TEXT_INPUT_MARGIN_X   = 22;
    private static final int TEXT_INPUT_MARGIN_TOP = 30;
    private static final int TEXT_INPUT_HEIGHT     = 20;

    // button dimensions and margins
    private static final int BUTTON_WIDTH    = 101;
    private static final int BUTTON_HEIGHT   = 21;
    private static final int BUTTON_MARGIN_X = 10;
    private static final int BUTTON_MARGIN_Y = 9;

    private EditBox textField;
    private Button sendButton;
    private Button cancelButton;
    private Entity screenEntity;
    private List<ChatHistoryEntry> historyEntries = new ArrayList<>();
    private int historyScrollLine = Integer.MAX_VALUE;
    private int historyX;
    private int historyY;
    private int historyWidth;
    private int historyHeight;
    private final Component labelText = CCText.UI_ENTER_MESSAGE.comp();
    private static final TextureLoader textures = new TextureLoader();

    private record HistoryLine(String text, int color) {
    }

    public ChatScreen(Entity entity, Player player) {
        super(CCText.UI_CHAT_TITLE.comp());
        this.screenEntity = entity;
        // tell server that chat opened
        ClientPackets.sendOpenChat(entity);
        ClientPackets.sendRequestHistory(entity);
    }

    @Override
    protected void init() {
        super.init();

        // Update the super background size
        BG_WIDTH = CHAT_BACKGROUND_WIDTH;
        BG_HEIGHT = CHAT_BACKGROUND_HEIGHT;
        TITLE_OFFSET = CHAT_TITLE_OFFSET;

        // center background horizontally, leaving room for history above the input box
        bgX = (this.width  - BG_WIDTH)  / 2;
        int availableHistoryHeight = Math.max(HISTORY_PANEL_MIN_HEIGHT, this.height - BG_HEIGHT - HISTORY_PANEL_GAP - 24);
        historyHeight = Math.min(HISTORY_PANEL_MAX_HEIGHT, availableHistoryHeight);
        bgY = Math.min(this.height - BG_HEIGHT - 10, 12 + historyHeight + HISTORY_PANEL_GAP);
        historyX = bgX + HISTORY_PANEL_MARGIN;
        historyY = bgY - historyHeight - HISTORY_PANEL_GAP;
        historyWidth = BG_WIDTH - HISTORY_PANEL_MARGIN * 2;

        // 1) text input
        int inputX = bgX + TEXT_INPUT_MARGIN_X;
        int inputY = bgY + TEXT_INPUT_MARGIN_TOP;
        int inputW = BG_WIDTH - TEXT_INPUT_MARGIN_X * 2;
        textField = new EditBox(
                font,
                inputX, inputY,
                inputW, TEXT_INPUT_HEIGHT,
                Component.literal("")
        );
        textField.setMaxLength(ChatDataManager.MAX_CHAR_IN_USER_MESSAGE);
        textField.setResponder(this::onTextChanged);
        setFocused(textField);
        addRenderableWidget(textField);

        // 2) image buttons anchored to bottom corners
        int btnY = bgY + BG_HEIGHT - BUTTON_HEIGHT - BUTTON_MARGIN_Y;

        // CANCEL / EXIT
        cancelButton = ButtonHelper.createImageButton(
                bgX + BUTTON_MARGIN_X,            // x
                btnY,                             // y
                BUTTON_WIDTH,                     // width
                BUTTON_HEIGHT,                    // height
                textures.GetUI("chat-button-exit"),        // normal texture
                textures.GetUI("chat-button-exit-hover"),  // hover texture
                widget -> onClose(),                // onPress
                widget -> Component.empty()            // narrationSupplier
        );
        addRenderableWidget(cancelButton);

        // SEND / DONE
        sendButton = ButtonHelper.createImageButton(
                bgX + BG_WIDTH - BUTTON_WIDTH - BUTTON_MARGIN_X,  // x
                btnY,                                             // y
                BUTTON_WIDTH,                                     // width
                BUTTON_HEIGHT,                                    // height
                textures.GetUI("chat-button-done"),               // normal texture
                textures.GetUI("chat-button-done-hover"),         // hover texture
                widget -> sendChatMessage(),                      // onPress
                widget -> Component.empty()                            // narrationSupplier
        );
        sendButton.active = false;
        addRenderableWidget(sendButton);
    }

    public void updateHistory(UUID entityId, List<ChatHistoryEntry> entries) {
        if (screenEntity == null || entityId == null || !screenEntity.getUUID().equals(entityId)) {
            return;
        }
        historyEntries = entries == null ? new ArrayList<>() : new ArrayList<>(entries);
        historyScrollLine = Integer.MAX_VALUE;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        renderHistory(context);
    }

    private void renderHistory(GuiGraphics context) {
        if (historyHeight <= 0) {
            return;
        }

        context.fill(historyX, historyY, historyX + historyWidth, historyY + historyHeight, 0xCC141414);
        context.fill(historyX, historyY, historyX + historyWidth, historyY + 1, 0xFF6F6F6F);
        context.fill(historyX, historyY + historyHeight - 1, historyX + historyWidth, historyY + historyHeight, 0xFF050505);

        int titleX = historyX + 6;
        int titleY = historyY + 5;
        context.drawString(this.font, CCText.UI_HISTORY_TITLE.comp(), titleX, titleY, HISTORY_COLOR_TITLE);

        List<HistoryLine> lines = getHistoryLines();
        int lineHeight = this.font.lineHeight + HISTORY_LINE_SPACING;
        int firstLineY = titleY + this.font.lineHeight + 5;
        int visibleLines = Math.max(1, (historyY + historyHeight - 5 - firstLineY) / lineHeight);
        int maxScroll = Math.max(0, lines.size() - visibleLines);
        historyScrollLine = Math.max(0, Math.min(historyScrollLine, maxScroll));

        if (lines.isEmpty()) {
            context.drawString(this.font, CCText.UI_HISTORY_EMPTY.comp(), titleX, firstLineY, HISTORY_COLOR_EMPTY);
            return;
        }

        int end = Math.min(lines.size(), historyScrollLine + visibleLines);
        int y = firstLineY;
        for (int i = historyScrollLine; i < end; i++) {
            HistoryLine line = lines.get(i);
            context.drawString(this.font, line.text(), titleX, y, line.color());
            y += lineHeight;
        }
    }

    private List<HistoryLine> getHistoryLines() {
        List<HistoryLine> lines = new ArrayList<>();
        for (ChatHistoryEntry entry : historyEntries) {
            String message = entry.message() == null ? "" : entry.message().replace('\n', ' ').replace('\r', ' ').trim();
            if (message.isEmpty()) {
                continue;
            }

            boolean npc = entry.sender() == ChatDataManager.ChatSender.ASSISTANT;
            String speaker = npc
                    ? CCText.UI_HISTORY_NPC.comp().getString()
                    : (entry.name() == null || entry.name().isBlank() ? CCText.UI_HISTORY_PLAYER.comp().getString() : entry.name());
            lines.add(new HistoryLine(speaker + ":", npc ? HISTORY_COLOR_NPC : HISTORY_COLOR_PLAYER));
            for (String line : LineWrapper.wrapLines(message, HISTORY_LINE_CHARS - 2)) {
                lines.add(new HistoryLine("  " + line, HISTORY_COLOR_MESSAGE));
            }
        }
        return lines;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (isMouseOverHistory(mouseX, mouseY)) {
            historyScrollLine -= amount > 0 ? 1 : -1;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private boolean isMouseOverHistory(double mouseX, double mouseY) {
        return mouseX >= historyX && mouseX <= historyX + historyWidth
                && mouseY >= historyY && mouseY <= historyY + historyHeight;
    }

    private void sendChatMessage() {
        // Send message to server
        String message = textField.getValue();
        ClientPackets.sendChat(screenEntity, message);
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && textField.isFocused()
                && !textField.getValue().isEmpty()) {
            sendChatMessage();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void onTextChanged(String text) {
        // Enable the button only if the text field is not empty
        sendButton.active = !text.isEmpty();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        ClientPackets.sendCloseChat();
    }

    @Override
    protected EditBox getTextField() {
        return this.textField;
    }

    @Override
    protected Component getLabelText() {
        return this.labelText;
    }
}
