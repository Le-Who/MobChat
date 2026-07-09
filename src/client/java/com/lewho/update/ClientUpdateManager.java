// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
package com.lewho.update;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public final class ClientUpdateManager {
    private static boolean checkStarted;
    private static boolean promptShown;
    private static int ticks;
    private static volatile UpdateCandidate availableUpdate;

    private ClientUpdateManager() {
    }

    public static void tick(Minecraft client) {
        ticks++;
        if (ClientUpdatePromptPolicy.shouldStartCheck(checkStarted, ticks)) {
            checkStarted = true;
            checkForUpdate();
        }
        boolean titleScreenVisible = client.screen instanceof TitleScreen;
        boolean inGameWithoutScreen = client.player != null && client.screen == null;
        if (ClientUpdatePromptPolicy.shouldPrompt(promptShown, availableUpdate != null, titleScreenVisible, inGameWithoutScreen)) {
            promptShown = true;
            client.setScreen(new CreatureChatUpdateScreen(availableUpdate));
        }
    }

    public static void downloadAndArm(UpdateCandidate candidate, BiConsumer<Boolean, String> callback) {
        Optional<RuntimeModInfo> runtime = UpdateRuntime.detect();
        if (runtime.isEmpty()) {
            callback.accept(false, "Auto-update is only available when CreatureChat is loaded from a jar file.");
            return;
        }

        RuntimeModInfo info = runtime.get();
        CompletableFuture.runAsync(() -> {
            try {
                GitHubReleaseClient source = new GitHubReleaseClient(UpdateRuntime.GITHUB_OWNER, UpdateRuntime.GITHUB_REPO, info.currentVersion());
                String hash = source.downloadText(candidate.sha512Url());
                byte[] jar = source.downloadBytes(candidate.downloadUrl());
                PendingUpdate pending = UpdateStager.stage(info.gameDir(), info.currentJar(), candidate.withSha512(hash), jar);
                Path log = UpdateHelperLauncher.launch(pending, info.javaExecutable(), ProcessHandle.current().pid());
                Minecraft.getInstance().execute(() -> callback.accept(true, "Downloaded. Restart Minecraft to install. Log: " + log));
            } catch (Exception e) {
                Minecraft.getInstance().execute(() -> callback.accept(false, "Download failed: " + e.getMessage()));
            }
        });
    }

    private static void checkForUpdate() {
        Optional<RuntimeModInfo> runtime = UpdateRuntime.detect();
        if (runtime.isEmpty()) {
            return;
        }
        RuntimeModInfo info = runtime.get();
        CompletableFuture.runAsync(() -> {
            try {
                UpdateService service = new UpdateService(new GitHubReleaseClient(
                        UpdateRuntime.GITHUB_OWNER,
                        UpdateRuntime.GITHUB_REPO,
                        info.currentVersion()
                ));
                availableUpdate = service.check(
                        info.archiveBaseName(),
                        info.currentVersion(),
                        info.minecraftVersion(),
                        false
                ).orElse(null);
            } catch (Exception ignored) {
                availableUpdate = null;
            }
        });
    }
}
