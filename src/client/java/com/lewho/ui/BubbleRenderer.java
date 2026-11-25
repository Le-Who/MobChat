// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import com.lewho.chat.PlayerData;
import com.lewho.render.BlendHelper;
import com.lewho.render.EntityTextureHelper;
import com.lewho.render.QuadBuffer;
import com.lewho.render.ShaderHelper;
import com.lewho.skin.PlayerCustomTexture;
import com.lewho.utils.*;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class BubbleRenderer {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    protected static TextureLoader textures = new TextureLoader();
    public static int DISPLAY_PADDING = 2;
    public static int animationFrame = 0;
    public static long lastTick = 0;
    public static int light = 15728880;
    public static int overlay = OverlayTexture.NO_OVERLAY;
    private static final float TEXT_Z_OFFSET = -0.05F;
    public static List<String> whitelist = new ArrayList<>();
    public static List<String> blacklist = new ArrayList<>();
    private static int queryEntityDataCount = 0;
    private static List<Entity> relevantEntities;

    public static void drawTextBubbleBackground(String base_name, PoseStack matrices, float x, float y, float width, float height, int friendship) {
        ShaderHelper.setTexturedShader();

        BlendHelper.enableBlend();
        BlendHelper.defaultBlendFunc();
        BlendHelper.enableDepthTest();
        BlendHelper.depthMask(true);

        QuadBuffer buffer = QuadBuffer.INSTANCE;
        float z = 0.01F;

        if (friendship == -3 && !base_name.endsWith("-player")) {
            TextureLoader.bind(0, textures.GetUI(base_name + "-enemy"));
        } else if (friendship == 3 && !base_name.endsWith("-player")) {
            TextureLoader.bind(0, textures.GetUI(base_name + "-friend"));
        } else {
            TextureLoader.bind(0, textures.GetUI(base_name));
        }
        drawTexturePart(matrices, buffer, x - 50, y, z, 228, 40);

        TextureLoader.bind(0, textures.GetUI("text-middle"));
        drawTexturePart(matrices, buffer, x, y + 40, z, width, height);

        TextureLoader.bind(0, textures.GetUI("text-bottom"));
        drawTexturePart(matrices, buffer, x, y + 40 + height, z, width, 5);

        BlendHelper.disableBlend();
        BlendHelper.disableDepthTest();
    }

    private static void drawTexturePart(PoseStack matrices, QuadBuffer buffer, float x, float y, float z, float width, float height) {
        Matrix4f matrix4f = matrices.last().pose();

        buffer.begin();

        buffer.vertex(matrix4f, x, y + height, z).color(255, 255, 255, 255).texture(0, 1).light(light).overlay(overlay);
        buffer.vertex(matrix4f, x + width, y + height, z).color(255, 255, 255, 255).texture(1, 1).light(light).overlay(overlay);
        buffer.vertex(matrix4f, x + width, y, z).color(255, 255, 255, 255).texture(1, 0).light(light).overlay(overlay);
        buffer.vertex(matrix4f, x, y, z).color(255, 255, 255, 255).texture(0, 0).light(light).overlay(overlay);
        buffer.draw();
    }

    private static void drawIcon(String ui_icon_name, PoseStack matrices, float x, float y, float width, float height) {
        ResourceLocation button_texture = textures.GetUI(ui_icon_name);

        ShaderHelper.setTexturedShader();
        TextureLoader.bind(0, button_texture);

        BlendHelper.enableBlend();
        BlendHelper.defaultBlendFunc();
        BlendHelper.enableDepthTest();
        BlendHelper.depthMask(true);

        QuadBuffer buffer = QuadBuffer.INSTANCE;

        Matrix4f matrix4f = matrices.last().pose();

        buffer.begin();

        buffer.vertex(matrix4f, x, y + height, 0.0F).color(255, 255, 255, 255).texture(0, 1).light(light).overlay(overlay);
        buffer.vertex(matrix4f, x + width, y + height, 0.0F).color(255, 255, 255, 255).texture(1, 1).light(light).overlay(overlay);
        buffer.vertex(matrix4f, x + width, y, 0.0F).color(255, 255, 255, 255).texture(1, 0).light(light).overlay(overlay);
        buffer.vertex(matrix4f, x, y, 0.0F).color(255, 255, 255, 255).texture(0, 0).light(light).overlay(overlay);
        buffer.draw();

        BlendHelper.disableBlend();
        BlendHelper.disableDepthTest();
    }

    private static void drawFriendshipStatus(PoseStack matrices, float x, float y, float width, float height, int friendship) {
        String ui_icon_name = "friendship" + friendship;

        ShaderHelper.setTexturedShader();

        ResourceLocation button_texture = textures.GetUI(ui_icon_name);
        TextureLoader.bind(0, button_texture);

        BlendHelper.enableBlend();
        BlendHelper.defaultBlendFunc();
        BlendHelper.enableDepthTest();
        BlendHelper.depthMask(true);

        QuadBuffer buffer = QuadBuffer.INSTANCE;

        Matrix4f matrix4f = matrices.last().pose();

        buffer.begin();

        float z = -0.01F;
        buffer.vertex(matrix4f, x, y + height, z).color(255, 255, 255, 255).texture(0, 1).light(light).overlay(overlay);
        buffer.vertex(matrix4f, x + width, y + height, z).color(255, 255, 255, 255).texture(1, 1).light(light).overlay(overlay);
        buffer.vertex(matrix4f, x + width, y, z).color(255, 255, 255, 255).texture(1, 0).light(light).overlay(overlay);
        buffer.vertex(matrix4f, x, y, z).color(255, 255, 255, 255).texture(0, 0).light(light).overlay(overlay);
        buffer.draw();

        BlendHelper.disableBlend();
        BlendHelper.disableDepthTest();
    }

    private static void drawEntityIcon(PoseStack matrices, Entity entity, float x, float y, float width, float height) {
        @SuppressWarnings("rawtypes")
        EntityRenderer renderer = EntityRendererAccessor.getEntityRenderer(entity);
        ResourceLocation skinId = EntityTextureHelper.getTexture(renderer, entity);
        if (skinId == null) return;

        String skinPath = skinId.getPath();
        ResourceLocation iconId = textures.GetEntity(skinPath);
        if (iconId == null) return;

        ShaderHelper.setTexturedShader();
        TextureLoader.bind(0, iconId);

        BlendHelper.enableBlend();
        BlendHelper.defaultBlendFunc();
        BlendHelper.enableDepthTest();
        BlendHelper.depthMask(true);

        QuadBuffer buffer = QuadBuffer.INSTANCE;

        Matrix4f matrix4f = matrices.last().pose();

        buffer.begin();

        float z = -0.01F;
        buffer.vertex(matrix4f, x, y + height, z).color(255, 255, 255, 255).texture(0, 1).light(light).overlay(overlay);
        buffer.vertex(matrix4f, x + width, y + height, z).color(255, 255, 255, 255).texture(1, 1).light(light).overlay(overlay);
        buffer.vertex(matrix4f, x + width, y, z).color(255, 255, 255, 255).texture(1, 0).light(light).overlay(overlay);
        buffer.vertex(matrix4f, x, y, z).color(255, 255, 255, 255).texture(0, 0).light(light).overlay(overlay);
        buffer.draw();

        BlendHelper.disableBlend();
        BlendHelper.disableDepthTest();
    }

    private static void drawPlayerIcon(PoseStack matrices, Entity entity, float x, float y, float width, float height) {
        @SuppressWarnings("rawtypes")
        EntityRenderer renderer = EntityRendererAccessor.getEntityRenderer(entity);
        ResourceLocation playerTexture = EntityTextureHelper.getTexture(renderer, entity);
        if (playerTexture == null) return;

        boolean customSkinFound = PlayerCustomTexture.hasCustomIcon(playerTexture);

        ShaderHelper.setTexturedShader();
        TextureLoader.bind(0, playerTexture);

        BlendHelper.enableBlend();
        BlendHelper.defaultBlendFunc();
        BlendHelper.enableDepthTest();
        BlendHelper.depthMask(true);

        QuadBuffer buffer = QuadBuffer.INSTANCE;
        buffer.begin();

        Matrix4f matrix4f = matrices.last().pose();
        float z = -0.01F;

        if (customSkinFound) {
            float[][] newCoordinates = {
                    {0.0F, 0.0F, 8.0F, 8.0F, 0F, 0F},
                    {24.0F, 0.0F, 32.0F, 8.0F, 8F, 0F},
                    {32.0F, 0.0F, 40.0F, 8.0F, 16F, 0F},
                    {56.0F, 0.0F, 64.0F, 8.0F, 0F, 8F},
                    {56.0F, 20.0F, 64.0F, 28.0F, 8F, 8F},
                    {36.0F, 16.0F, 44.0F, 20.0F, 16F, 8F},
                    {56.0F, 16.0F, 64.0F, 20.0F, 16F, 12F},
                    {56.0F, 28.0F, 64.0F, 36.0F, 0F, 16F},
                    {56.0F, 36.0F, 64.0F, 44.0F, 8F, 16F},
                    {56.0F, 44.0F, 64.0F, 48, 16F, 16F},
                    {12.0F, 48.0F, 20.0F, 52, 16F, 20F},
            };
            float scaleFactor = 0.77F;

            for (float[] coords : newCoordinates) {
                float newU1 = coords[0] / 64.0F;
                float newV1 = coords[1] / 64.0F;
                float newU2 = coords[2] / 64.0F;
                float newV2 = coords[3] / 64.0F;

                float offsetX = coords[4] * scaleFactor;
                float offsetY = coords[5] * scaleFactor;
                float scaledX = x + offsetX;
                float scaledY = y + offsetY;
                float scaledWidth = (coords[2] - coords[0]) * scaleFactor;
                float scaledHeight = (coords[3] - coords[1]) * scaleFactor;

                buffer.vertex(matrix4f, scaledX, scaledY + scaledHeight, z)
                        .color(255, 255, 255, 255).texture(newU1, newV2).light(light).overlay(overlay);
                buffer.vertex(matrix4f, scaledX + scaledWidth, scaledY + scaledHeight, z)
                        .color(255, 255, 255, 255).texture(newU2, newV2).light(light).overlay(overlay);
                buffer.vertex(matrix4f, scaledX + scaledWidth, scaledY, z)
                        .color(255, 255, 255, 255).texture(newU2, newV1).light(light).overlay(overlay);
                buffer.vertex(matrix4f, scaledX, scaledY, z)
                        .color(255, 255, 255, 255).texture(newU1, newV1).light(light).overlay(overlay);
            }
        } else {
            x += 2;
            y += 2;
            width -= 4;
            height -= 4;

            float u1 = 8.0F / 64.0F;
            float v1 = 8.0F / 64.0F;
            float u2 = 16.0F / 64.0F;
            float v2 = 16.0F / 64.0F;

            buffer.vertex(matrix4f, x, y + height, z)
                    .color(255, 255, 255, 255).texture(u1, v2).light(light).overlay(overlay);
            buffer.vertex(matrix4f, x + width, y + height, z)
                    .color(255, 255, 255, 255).texture(u2, v2).light(light).overlay(overlay);
            buffer.vertex(matrix4f, x + width, y, z)
                    .color(255, 255, 255, 255).texture(u2, v1).light(light).overlay(overlay);
            buffer.vertex(matrix4f, x, y, z)
                    .color(255, 255, 255, 255).texture(u1, v1).light(light).overlay(overlay);

            float hatU1 = 40.0F / 64.0F;
            float hatV1 = 8.0F / 64.0F;
            float hatU2 = 48.0F / 64.0F;
            float hatV2 = 16.0F / 64.0F;

            z -= 0.01F;

            buffer.vertex(matrix4f, x, y + height, z)
                    .color(255, 255, 255, 255).texture(hatU1, hatV2).light(light).overlay(overlay);
            buffer.vertex(matrix4f, x + width, y + height, z)
                    .color(255, 255, 255, 255).texture(hatU2, hatV2).light(light).overlay(overlay);
            buffer.vertex(matrix4f, x + width, y, z)
                    .color(255, 255, 255, 255).texture(hatU2, hatV1).light(light).overlay(overlay);
            buffer.vertex(matrix4f, x, y, z)
                    .color(255, 255, 255, 255).texture(hatU1, hatV1).light(light).overlay(overlay);
        }

        buffer.draw();

        BlendHelper.disableBlend();
        BlendHelper.disableDepthTest();
    }

    private static void drawMessageText(Matrix4f matrix, List<String> lines, int starting_line, int ending_line,
                                 MultiBufferSource immediate, float lineSpacing, int fullBright, float yOffset) {
        Font fontRenderer = Minecraft.getInstance().font;
        Matrix4f textMatrix = new Matrix4f(matrix).translate(0.0F, 0.0F, TEXT_Z_OFFSET);
        int currentLineIndex = 0;

        for (String lineText : lines) {
            if (currentLineIndex >= starting_line && currentLineIndex < ending_line) {
                fontRenderer.drawInBatch(lineText, -fontRenderer.width(lineText) / 2f, yOffset, 0xffffffff,
                        false, textMatrix, immediate, DisplayMode.NORMAL, 0, fullBright);
                yOffset += fontRenderer.lineHeight + lineSpacing;
            }
            currentLineIndex++;

            if (currentLineIndex > ending_line) {
                break;
            }
        }
    }

    private static void drawEntityName(Entity entity, Matrix4f matrix, MultiBufferSource immediate,
                                int fullBright, float yOffset, boolean truncate) {
        Font fontRenderer = Minecraft.getInstance().font;

        String nameText = "";
        if (entity instanceof Mob) {
            if (entity.getCustomName() != null) {
                nameText = entity.getCustomName().getString();
            }
        } else if (entity instanceof Player) {
            nameText = entity.getName().getString();
        }

        if (nameText.length() > 14 && truncate) {
            nameText = nameText.substring(0, 14) + "...";
        }

        Matrix4f textMatrix = new Matrix4f(matrix).translate(0.0F, 0.0F, TEXT_Z_OFFSET);
        fontRenderer.drawInBatch(nameText, -fontRenderer.width(nameText) / 2f, yOffset, 0xffffffff,
                false, textMatrix, immediate, DisplayMode.NORMAL, 0, fullBright);
    }

    public static void drawTextAboveEntities(WorldRenderContext context, long tick, float partialTicks) {
        float lineSpacing = 1F;
        float textHeaderHeight = 40F;
        float textFooterHeight = 5F;
        int fullBright = 0xF000F0;
        double renderDistance = 11.0;

        Camera camera = context.camera();
        Entity cameraEntity = camera.getEntity();
        if (cameraEntity == null) return;
        Level world = cameraEntity.level();

        Vec3 pos = cameraEntity.position();
        AABB area = new AABB(pos.x - renderDistance, pos.y - renderDistance, pos.z - renderDistance,
                pos.x + renderDistance, pos.y + renderDistance, pos.z + renderDistance);

        Font fontRenderer = Minecraft.getInstance().font;
        PoseStack matrices = context.matrixStack();
        MultiBufferSource immediate = context.consumers();

        Vec3 interpolatedCameraPos = new Vec3(camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);

        queryEntityDataCount++;

        if (queryEntityDataCount % 3 == 0 || relevantEntities == null) {
            List<Entity> nearbyEntities = world.getEntities(null, area);

            relevantEntities = nearbyEntities.stream()
                    .filter(entity -> (entity instanceof Mob || entity instanceof Player))
                    .filter(entity -> !entity.isVehicle())
                    .filter(entity -> !(entity.equals(cameraEntity) && !camera.isDetached()))
                    .filter(entity -> !(entity.equals(cameraEntity) && entity.isSpectator()))
                    .filter(entity -> {
                        if (entity instanceof Player) {
                            return true;
                        }
                        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
                        String entityIdString = entityId.toString();
                        if (blacklist.contains(entityIdString)) {
                            return false;
                        }
                        return whitelist.isEmpty() || whitelist.contains(entityIdString);
                    })
                    .collect(Collectors.toList());

            queryEntityDataCount = 0;
        }

        for (Entity entity : relevantEntities) {

            matrices.pushPose();

            float entityHeight = EntityHeights.getAdjustedEntityHeight(entity);

            double paddingAboveEntity = 0.4D;
            Vec3 interpolatedEntityPos = EntityRenderPosition.getInterpolatedPosition(entity, partialTicks);

            Vec3 bubblePosition;
            if (entity instanceof EnderDragon) {
                EnderDragon dragon = (EnderDragon) entity;
                EnderDragonPart head = dragon.head;

                Vec3 headPos = EntityRenderPosition.getInterpolatedPosition(head, partialTicks);

                bubblePosition = headPos.add(0, entityHeight + paddingAboveEntity, 0);
            } else {
                float entityYawRadians = (float) Math.toRadians(entity.getViewYRot(partialTicks));
                Vec3 forwardOffset = new Vec3(-Math.sin(entityYawRadians), 0.0, Math.cos(entityYawRadians));

                Vec3 scaledForwardOffset = forwardOffset.scale(entity.getBbWidth() / 2.0 * 0.8);

                bubblePosition = interpolatedEntityPos.add(scaledForwardOffset)
                        .add(0, entityHeight + paddingAboveEntity, 0);
            }

            matrices.translate(bubblePosition.x - interpolatedCameraPos.x,
                    bubblePosition.y - interpolatedCameraPos.y,
                    bubblePosition.z - interpolatedCameraPos.z);

            Vec3 difference = interpolatedCameraPos.subtract(new Vec3(interpolatedEntityPos.x, interpolatedEntityPos.y + entityHeight + paddingAboveEntity, interpolatedEntityPos.z));

            double yaw = -(Math.atan2(difference.z, difference.x) + Math.PI / 2D);

            float halfYaw = (float) yaw * 0.5f;
            double sinHalfYaw = Mth.sin(halfYaw);
            double cosHalfYaw = Mth.cos(halfYaw);
            Quaternionf yawRotation = new Quaternionf(0, sinHalfYaw, 0, cosHalfYaw);

            matrices.mulPose(yawRotation);

            double horizontalDistance = Math.sqrt(difference.x * difference.x + difference.z * difference.z);
            double pitch = Math.atan2(difference.y, horizontalDistance);

            float halfPitch = (float) pitch * 0.5f;
            double sinHalfPitch = Mth.sin(halfPitch);
            double cosHalfPitch = Mth.cos(halfPitch);
            Quaternionf pitchRotation = new Quaternionf(sinHalfPitch, 0, 0, cosHalfPitch);

            matrices.mulPose(pitchRotation);

            Matrix4f matrix = matrices.last().pose();

            LocalPlayer player = Minecraft.getInstance().player;

            EntityChatData chatData = null;
            PlayerData playerData = null;
            if (entity instanceof Mob) {
                chatData = ChatDataManager.getClientInstance().getOrCreateChatData(entity.getStringUUID());
                if (chatData != null) {
                    playerData = chatData.getPlayerData(player.getStringUUID(), player.getDisplayName().getString());
                }
            } else if (entity instanceof Player) {
                chatData = PlayerMessageManager.getMessage(entity.getUUID());
                playerData = new PlayerData();
            }

            float minTextHeight = (ChatDataManager.DISPLAY_NUM_LINES * (fontRenderer.lineHeight + lineSpacing)) + (DISPLAY_PADDING * 2);
            float scaledTextHeight = 0;

            if (chatData != null) {
                List<String> lines = chatData.getWrappedLines();
                float linesDisplayed = 0;
                int starting_line = chatData.currentLineNumber;
                int ending_line = Math.min(chatData.currentLineNumber + ChatDataManager.DISPLAY_NUM_LINES, lines.size());

                linesDisplayed = ending_line - starting_line;

                scaledTextHeight = linesDisplayed * (fontRenderer.lineHeight + lineSpacing);
                scaledTextHeight = Math.max(scaledTextHeight, minTextHeight);

                if (entity instanceof Mob) {
                    BubbleLocationManager.updateBubbleData(entity.getUUID(), bubblePosition,
                            128F / (1 / 0.02F), (scaledTextHeight + 25F) / (1 / 0.02F), yaw, pitch);
                }

                matrices.scale(-0.02F, -0.02F, 0.02F);

                matrices.translate(0F, -scaledTextHeight - textHeaderHeight - textFooterHeight, 0F);

                if (chatData.status == ChatDataManager.ChatStatus.NONE) {
                    drawIcon("button-chat", matrices, -16, textHeaderHeight, 32, 17);

                    drawEntityName(entity, matrix, immediate, fullBright, 24F + DISPLAY_PADDING, true);

                } else if (chatData.status == ChatDataManager.ChatStatus.PENDING) {
                    drawIcon("button-dot-" + animationFrame, matrices, -16, textHeaderHeight, 32, 17);

                } else if (chatData.sender == ChatDataManager.ChatSender.ASSISTANT && chatData.status != ChatDataManager.ChatStatus.HIDDEN) {
                    drawEntityName(entity, matrix, immediate, fullBright, 24F + DISPLAY_PADDING, true);

                    drawTextBubbleBackground("text-top", matrices, -64, 0, 128, scaledTextHeight, playerData.friendship);

                    drawEntityIcon(matrices, entity, -82, 7, 32, 32);

                    drawFriendshipStatus(matrices, 51, 18, 31, 21, playerData.friendship);

                    if (chatData.currentLineNumber > 0) {
                        drawIcon("arrow-left", matrices, -63, scaledTextHeight + 29, 16, 16);
                    }
                    if (!chatData.isEndOfMessage()) {
                        drawIcon("arrow-right", matrices, 47, scaledTextHeight + 29, 16, 16);
                    } else {
                        drawIcon("keyboard", matrices, 47, scaledTextHeight + 28, 16, 16);
                    }

                    drawMessageText(matrix, lines, starting_line, ending_line, immediate, lineSpacing, fullBright, 40.0F + DISPLAY_PADDING);

                } else if (chatData.sender == ChatDataManager.ChatSender.ASSISTANT && chatData.status == ChatDataManager.ChatStatus.HIDDEN) {
                    drawEntityName(entity, matrix, immediate, fullBright, 24F + DISPLAY_PADDING, false);

                    if (playerData.friendship == 3) {
                        drawIcon("button-chat-friend", matrices, -16, textHeaderHeight, 32, 17);
                    } else if (playerData.friendship == -3) {
                        drawIcon("button-chat-enemy", matrices, -16, textHeaderHeight, 32, 17);
                    } else {
                        drawIcon("button-chat", matrices, -16, textHeaderHeight, 32, 17);
                    }

                } else if (chatData.sender == ChatDataManager.ChatSender.USER && chatData.status == ChatDataManager.ChatStatus.DISPLAY) {
                    drawEntityName(entity, matrix, immediate, fullBright, 24F + DISPLAY_PADDING, true);

                    drawTextBubbleBackground("text-top-player", matrices, -64, 0, 128, scaledTextHeight, playerData.friendship);

                    drawPlayerIcon(matrices, entity, -75, 14, 18, 18);

                    drawMessageText(matrix, lines, starting_line, ending_line, immediate, lineSpacing, fullBright, 40.0F + DISPLAY_PADDING);
                }

            } else if (entity instanceof Player) {
                matrices.scale(-0.02F, -0.02F, 0.02F);

                boolean showPendingIcon = false;
                if (PlayerMessageManager.isChatUIOpen(entity.getUUID())) {
                    showPendingIcon = true;
                    scaledTextHeight += minTextHeight;
                } else {
                    scaledTextHeight -= 15;
                }

                matrices.translate(0F, -scaledTextHeight - textHeaderHeight - textFooterHeight, 0F);

                if (!entity.equals(cameraEntity) && !Minecraft.getInstance().options.hideGui) {
                    drawEntityName(entity, matrices.last().pose(), immediate, fullBright, 24F + DISPLAY_PADDING, true);

                    if (showPendingIcon) {
                        drawIcon("button-dot-" + animationFrame, matrices, -16, textHeaderHeight, 32, 17);
                    }
                }
            }

            if (lastTick != tick && tick % 5 == 0) {
                lastTick = tick;
                animationFrame++;
            }
            if (animationFrame > 8) {
                animationFrame = 0;
            }

            matrices.popPose();
        }

        List<UUID> activeEntityUUIDs = relevantEntities.stream()
                .map(Entity::getUUID)
                .collect(Collectors.toList());

        BubbleLocationManager.performCleanup(activeEntityUUIDs);
    }
}
