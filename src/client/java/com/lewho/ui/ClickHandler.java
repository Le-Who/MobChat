// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.ui;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import com.lewho.network.ClientPackets;
import com.lewho.utils.ClientEntityFinder;
import com.lewho.utils.UseItemCallbackHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class ClickHandler {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static boolean wasClicked = false;

    public static void register() {
        UseItemCallback.EVENT.register(UseItemCallbackHelper::handleUseItemAction);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.options.keyUse.isDown()) {
                if (!wasClicked && client.player != null && client.player.getMainHandItem().isEmpty()) {
                    if (handleUseKeyClick(client)) {
                        wasClicked = true;
                    }
                }
            } else {
                wasClicked = false;
            }
        });
    }

    public static boolean shouldCancelAction(Level world) {
        if (world.isClientSide) {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.options.keyUse.isDown()) {
                return handleUseKeyClick(client);
            }
        }
        return false;
    }

    public static boolean handleUseKeyClick(Minecraft client) {
        Camera camera = client.gameRenderer.getMainCamera();
        Entity cameraEntity = camera.getEntity();
        if (cameraEntity == null) return false;

        LocalPlayer player = client.player;

        Vec3 startRay = camera.getPosition();

        Vec3 lookVec = player.getViewVector(1.0F);

        double closestDistance = Double.MAX_VALUE;
        Optional<Vec3> closestHitResult = null;
        UUID closestEntityUUID = null;
        BubbleLocationManager.BubbleData closestBubbleData = null;

        for (Map.Entry<UUID, BubbleLocationManager.BubbleData> entry : BubbleLocationManager.getAllBubbleData().entrySet()) {
            UUID entityUUID = entry.getKey();
            BubbleLocationManager.BubbleData bubbleData = entry.getValue();

            Vec3[] corners = getBillboardCorners(bubbleData.position, camera.getPosition(), bubbleData.height, bubbleData.width, bubbleData.yaw, bubbleData.pitch);

            Optional<Vec3> hitResult = rayIntersectsPolygon(startRay, lookVec, corners);
            if (hitResult.isPresent()) {
                double distance = startRay.distanceToSqr(hitResult.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntityUUID = entityUUID;
                    closestHitResult = hitResult;
                    closestBubbleData = bubbleData;
                }
            }
        }

        if (closestEntityUUID != null) {
            Mob closestEntity = ClientEntityFinder.getEntityByUUID(client.level, closestEntityUUID);
            if (closestEntity != null) {
                EntityChatData chatData = ChatDataManager.getClientInstance().getOrCreateChatData(closestEntityUUID.toString());

                String hitRegion = determineHitRegion(closestHitResult.get(), closestBubbleData.position, camera, closestBubbleData.height);
                LOGGER.debug("Clicked region: " + hitRegion);

                if (chatData.status == ChatDataManager.ChatStatus.NONE) {
                    ClientPackets.sendGenerateGreeting(closestEntity);

                } else if (chatData.status == ChatDataManager.ChatStatus.DISPLAY) {
                    if (hitRegion.equals("RIGHT") && !chatData.isEndOfMessage()) {
                        ClientPackets.sendUpdateLineNumber(closestEntity, chatData.currentLineNumber + ChatDataManager.DISPLAY_NUM_LINES);
                    } else if (hitRegion.equals("LEFT") && chatData.currentLineNumber > 0) {
                        ClientPackets.sendUpdateLineNumber(closestEntity, chatData.currentLineNumber - ChatDataManager.DISPLAY_NUM_LINES);
                    } else if (hitRegion.equals("RIGHT") && chatData.isEndOfMessage()) {
                        client.setScreen(new ChatScreen(closestEntity, client.player));
                    } else if (hitRegion.equals("TOP")) {
                        ClientPackets.setChatStatus(closestEntity, ChatDataManager.ChatStatus.HIDDEN);
                    }
                } else if (chatData.status == ChatDataManager.ChatStatus.HIDDEN) {
                    ClientPackets.setChatStatus(closestEntity, ChatDataManager.ChatStatus.DISPLAY);
                }
                return true;
            }
        }
        return false;
    }

    public static Vec3[] getBillboardCorners(Vec3 center, Vec3 cameraPos, double height, double width, double yaw, double pitch) {
        double radYaw = Math.toRadians(yaw);
        double radPitch = Math.toRadians(pitch);

        Vec3 toCamera = cameraPos.subtract(center).normalize();

        Vec3 globalUp = new Vec3(0, 1, 0);
        Vec3 right = globalUp.cross(toCamera).normalize();
        Vec3 up = toCamera.cross(right).normalize();

        right = rotateVector(right, radYaw, radPitch);
        up = rotateVector(up, radYaw, radPitch);

        Vec3 adjustedCenter = center.add(up.scale(height / 2));

        Vec3 topLeft = adjustedCenter.subtract(right.scale(width / 2)).add(up.scale(height / 2));
        Vec3 topRight = adjustedCenter.add(right.scale(width / 2)).add(up.scale(height / 2));
        Vec3 bottomRight = adjustedCenter.add(right.scale(width / 2)).subtract(up.scale(height / 2));
        Vec3 bottomLeft = adjustedCenter.subtract(right.scale(width / 2)).subtract(up.scale(height / 2));

        return new Vec3[] {topLeft, topRight, bottomRight, bottomLeft};
    }

    private static Vec3 rotateVector(Vec3 vector, double yaw, double pitch) {
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        Vec3 yawRotated = new Vec3(
                vector.x * cosYaw + vector.z * sinYaw,
                vector.y,
                -vector.x * sinYaw + vector.z * cosYaw
        );

        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        return new Vec3(
                yawRotated.x,
                yawRotated.y * cosPitch - yawRotated.z * sinPitch,
                yawRotated.y * sinPitch + yawRotated.z * cosPitch
        );
    }

    public static Optional<Vec3> rayIntersectsPolygon(Vec3 rayOrigin, Vec3 rayDirection, Vec3[] vertices) {
        rayDirection = rayDirection.normalize();
        return Stream.of(
                        rayIntersectsTriangle(rayOrigin, rayDirection, vertices[0], vertices[1], vertices[2]),
                        rayIntersectsTriangle(rayOrigin, rayDirection, vertices[0], vertices[2], vertices[3])
                ).filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.empty());
    }

    public static Optional<Vec3> rayIntersectsTriangle(Vec3 rayOrigin, Vec3 rayDirection, Vec3 v0, Vec3 v1, Vec3 v2) {
        Vec3 edge1 = v1.subtract(v0);
        Vec3 edge2 = v2.subtract(v0);
        Vec3 h = rayDirection.cross(edge2);
        double a = edge1.dot(h);

        if (Math.abs(a) < 1e-6) return Optional.empty();

        double f = 1.0 / a;
        Vec3 s = rayOrigin.subtract(v0);
        double u = f * s.dot(h);
        if (u < 0.0 || u > 1.0) return Optional.empty();

        Vec3 q = s.cross(edge1);
        double v = f * rayDirection.dot(q);
        if (v < 0.0 || u + v > 1.0) return Optional.empty();

        double t = f * edge2.dot(q);
        if (t > 1e-6) {
            return Optional.of(rayOrigin.add(rayDirection.scale(t)));
        }
        return Optional.empty();
    }

    public static String determineHitRegion(Vec3 hitPoint, Vec3 center, Camera camera, double height) {
        Vec3 cameraPos = camera.getPosition();
        Vec3 toCamera = cameraPos.subtract(center).normalize();

        Vec3 globalUp = new Vec3(0, 1, 0);

        Vec3 right = globalUp.cross(toCamera).normalize();

        if (right.lengthSqr() == 0) {
            right = new Vec3(1, 0, 0);
        }

        Vec3 up = toCamera.cross(right).normalize();

        Vec3 relPosition = hitPoint.subtract(center);
        double relX = relPosition.dot(right);
        double relY = relPosition.dot(up);

        if (relY > 0.70 * height) {
            return "TOP";
        } else {
            return relX < -0.5 ? "LEFT" : "RIGHT";
        }
    }
}