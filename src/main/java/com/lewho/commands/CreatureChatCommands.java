// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.lewho.chat.ChatGPTRequest;
import com.lewho.network.ServerPackets;
import com.lewho.i18n.CCText;
import com.lewho.update.GitHubReleaseClient;
import com.lewho.update.PendingUpdate;
import com.lewho.update.RuntimeModInfo;
import com.lewho.update.UpdateCandidate;
import com.lewho.update.UpdateHelperLauncher;
import com.lewho.update.UpdateRuntime;
import com.lewho.update.UpdateService;
import com.lewho.update.UpdateStager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * The {@code CreatureChatCommands} class registers custom commands to set new API key, model, and url.
 * Permission level set to 4 (server owner), since this deals with API keys and potential costs.
 */
public class CreatureChatCommands {
    public static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
            registerCommands(dispatcher);
        });
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("creaturechat")
                .then(registerSetCommand("key", "API Key", StringArgumentType.string()))
                .then(registerSetCommand("url", "URL", StringArgumentType.string()))
                .then(registerSetCommand("model", "Model", StringArgumentType.string()))
                .then(registerSetCommand("timeout", "Timeout (seconds)", IntegerArgumentType.integer()))
                .then(registerSetCommand("outputtokens", "Output tokens", IntegerArgumentType.integer(ConfigurationHandler.Config.MIN_MAX_OUTPUT_TOKENS)))
                .then(registerSetCommand("damagecooldown", "Damage reaction cooldown (seconds)", IntegerArgumentType.integer(0)))
                .then(registerSetCommand("geminirpm", "Gemini requests per minute", IntegerArgumentType.integer(0)))
                .then(registerSetCommand("geminidaily", "Gemini requests per day", IntegerArgumentType.integer(0)))
                .then(registerSetCommand("geminiscope", "Gemini usage limit scope", StringArgumentType.word()))
                .then(registerSetupCommand())
                .then(registerPresetCommand())
                .then(registerConfigCommand())
                .then(registerStoryCommand())
                .then(registerWhitelistCommand())
                .then(registerBlacklistCommand())
                .then(registerChatBubbleCommand())
                .then(registerSendToChatCommand())
                .then(registerOverhearCommand())
                .then(registerUpdateCommand())
                .then(registerHelpCommand()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerSetCommand(String settingName, String settingDescription, ArgumentType<?> valueType) {
        return Commands.literal(settingName)
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("set")
                        .then(Commands.argument("value", valueType)
                                .then(addConfigArgs((context, useServerConfig) -> {
                                    if (valueType instanceof StringArgumentType)
                                        return setConfig(context.getSource(), settingName, StringArgumentType.getString(context, "value"), useServerConfig, settingDescription);
                                    else if (valueType instanceof IntegerArgumentType)
                                        return setConfig(context.getSource(), settingName, IntegerArgumentType.getInteger(context, "value"), useServerConfig, settingDescription);
                                    return 1;
                                }))
                                .executes(context -> {
                                    if (valueType instanceof StringArgumentType)
                                        return setConfig(context.getSource(), settingName, StringArgumentType.getString(context, "value"), false, settingDescription);
                                    else if (valueType instanceof IntegerArgumentType)
                                        return setConfig(context.getSource(), settingName, IntegerArgumentType.getInteger(context, "value"), false, settingDescription);
                                    return 1;
                                })
                        ));
    }

    private static List<ResourceLocation> getLivingEntityIds() {
        return BuiltInRegistries.ENTITY_TYPE
                .keySet()
                .stream()
                .filter(id ->
                        // getOptional(...) returns Optional<EntityType<?>> on all versions
                        BuiltInRegistries.ENTITY_TYPE
                                .getOptional(id)
                                .map(type -> type.getCategory() != MobCategory.MISC
                                        || isIncludedEntity(type))
                                .orElse(false)
                )
                .collect(Collectors.toList());
    }

    private static boolean isIncludedEntity(EntityType<?> entityType) {
        return entityType == EntityType.VILLAGER
                || entityType == EntityType.IRON_GOLEM
                || entityType == EntityType.SNOW_GOLEM;
    }

    private static List<String> getLivingEntityTypeNames() {
        return getLivingEntityIds().stream()
                .map(ResourceLocation::toString)
                .collect(Collectors.toList());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerSetupCommand() {
        return Commands.literal("setup")
                .requires(source -> source.hasPermission(4))
                .executes(context -> showSetupWizard(context.getSource()))
                .then(Commands.literal("provider")
                        .then(Commands.argument("provider", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(ConfigurationPresets.providerIds(), builder))
                                .executes(context -> applyPreset(context.getSource(), StringArgumentType.getString(context, "provider"), true))))
                .then(Commands.literal("key")
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                .executes(context -> setConfig(context.getSource(), "key", StringArgumentType.getString(context, "value"), true, "API Key"))))
                .then(Commands.literal("url")
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                .executes(context -> setConfig(context.getSource(), "url", StringArgumentType.getString(context, "value"), true, "URL"))))
                .then(Commands.literal("model")
                        .then(Commands.argument("value", StringArgumentType.greedyString())
                                .executes(context -> setConfig(context.getSource(), "model", StringArgumentType.getString(context, "value"), true, "Model"))))
                .then(Commands.literal("timeout")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                .executes(context -> setConfig(context.getSource(), "timeout", IntegerArgumentType.getInteger(context, "seconds"), true, "Timeout (seconds)"))))
                .then(Commands.literal("outputtokens")
                        .then(Commands.argument("tokens", IntegerArgumentType.integer(ConfigurationHandler.Config.MIN_MAX_OUTPUT_TOKENS))
                                .executes(context -> setConfig(context.getSource(), "outputtokens", IntegerArgumentType.getInteger(context, "tokens"), true, "Output tokens"))))
                .then(Commands.literal("damagecooldown")
                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0))
                                .executes(context -> setConfig(context.getSource(), "damagecooldown", IntegerArgumentType.getInteger(context, "seconds"), true, "Damage reaction cooldown (seconds)"))))
                .then(Commands.literal("geminirpm")
                        .then(Commands.argument("requests", IntegerArgumentType.integer(0))
                                .executes(context -> setConfig(context.getSource(), "geminirpm", IntegerArgumentType.getInteger(context, "requests"), true, "Gemini requests per minute"))))
                .then(Commands.literal("geminidaily")
                        .then(Commands.argument("requests", IntegerArgumentType.integer(0))
                                .executes(context -> setConfig(context.getSource(), "geminidaily", IntegerArgumentType.getInteger(context, "requests"), true, "Gemini requests per day"))))
                .then(Commands.literal("geminiscope")
                        .then(Commands.argument("scope", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("per_key", "shared"), builder))
                                .executes(context -> setConfig(context.getSource(), "geminiscope", StringArgumentType.getString(context, "scope"), true, "Gemini usage limit scope"))))
                .then(Commands.literal("show")
                        .executes(context -> showConfig(context.getSource())))
                .then(Commands.literal("test")
                        .executes(context -> testConfig(context.getSource())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerPresetCommand() {
        return Commands.literal("preset")
                .requires(source -> source.hasPermission(4))
                .then(Commands.argument("provider", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(ConfigurationPresets.providerIds(), builder))
                        .then(addConfigArgs((context, useServerConfig) -> applyPreset(context.getSource(), StringArgumentType.getString(context, "provider"), useServerConfig)))
                        .executes(context -> applyPreset(context.getSource(), StringArgumentType.getString(context, "provider"), false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerConfigCommand() {
        return Commands.literal("config")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("show")
                        .executes(context -> showConfig(context.getSource())))
                .then(Commands.literal("test")
                        .executes(context -> testConfig(context.getSource())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerUpdateCommand() {
        return Commands.literal("update")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("check")
                        .executes(context -> checkUpdate(context.getSource())))
                .then(Commands.literal("download")
                        .executes(context -> downloadUpdate(context.getSource())))
                .then(Commands.literal("apply")
                        .executes(context -> applyPendingUpdate(context.getSource())))
                .then(Commands.literal("status")
                        .executes(context -> showUpdateStatus(context.getSource())));
    }

    private static int checkUpdate(CommandSourceStack source) {
        Optional<RuntimeModInfo> runtime = UpdateRuntime.detect();
        if (runtime.isEmpty()) {
            sendUpdateMessage(source, false, "CreatureChat auto-update is only available when the mod is loaded from a jar file.");
            return 0;
        }

        RuntimeModInfo info = runtime.get();
        sendUpdateMessage(source, true, "Checking GitHub Releases for CreatureChat updates...");
        CompletableFuture.runAsync(() -> {
            try {
                Optional<UpdateCandidate> candidate = updateService(info).check(
                        info.archiveBaseName(),
                        info.currentVersion(),
                        info.minecraftVersion(),
                        false
                );
                source.getServer().execute(() -> {
                    if (candidate.isPresent()) {
                        UpdateCandidate update = candidate.get();
                        sendUpdateMessage(source, true, "CreatureChat update available: " + info.currentVersion()
                                + " -> " + update.version() + ". Use /creaturechat update download to stage it.");
                    } else {
                        sendUpdateMessage(source, true, "CreatureChat is up to date for Minecraft " + info.minecraftVersion()
                                + " (" + info.currentVersion() + ").");
                    }
                });
            } catch (Exception e) {
                source.getServer().execute(() -> sendUpdateMessage(source, false, "CreatureChat update check failed: " + e.getMessage()));
                LOGGER.error("CreatureChat update check failed", e);
            }
        });
        return 1;
    }

    private static int downloadUpdate(CommandSourceStack source) {
        Optional<RuntimeModInfo> runtime = UpdateRuntime.detect();
        if (runtime.isEmpty()) {
            sendUpdateMessage(source, false, "CreatureChat auto-update is only available when the mod is loaded from a jar file.");
            return 0;
        }

        RuntimeModInfo info = runtime.get();
        sendUpdateMessage(source, true, "Downloading CreatureChat update from GitHub Releases...");
        CompletableFuture.runAsync(() -> {
            try {
                Optional<PendingUpdate> pending = updateService(info).downloadAndStage(
                        info.gameDir(),
                        info.currentJar(),
                        info.archiveBaseName(),
                        info.currentVersion(),
                        info.minecraftVersion(),
                        false
                );
                source.getServer().execute(() -> {
                    if (pending.isEmpty()) {
                        sendUpdateMessage(source, true, "CreatureChat is already up to date for Minecraft " + info.minecraftVersion() + ".");
                        return;
                    }
                    try {
                        Path log = UpdateHelperLauncher.launch(pending.get(), info.javaExecutable(), ProcessHandle.current().pid());
                        sendUpdateMessage(source, true, "CreatureChat " + pending.get().version()
                                + " is staged. Stop and start the server when ready; the helper will replace the jar after this JVM exits. Log: "
                                + log);
                    } catch (Exception e) {
                        sendUpdateMessage(source, false, "Update staged, but helper could not be launched: " + e.getMessage()
                                + ". Use /creaturechat update apply before stopping the server.");
                        LOGGER.error("CreatureChat update helper launch failed", e);
                    }
                });
            } catch (Exception e) {
                source.getServer().execute(() -> sendUpdateMessage(source, false, "CreatureChat update download failed: " + e.getMessage()));
                LOGGER.error("CreatureChat update download failed", e);
            }
        });
        return 1;
    }

    private static int applyPendingUpdate(CommandSourceStack source) {
        Optional<RuntimeModInfo> runtime = UpdateRuntime.detect();
        if (runtime.isEmpty()) {
            sendUpdateMessage(source, false, "CreatureChat auto-update is only available when the mod is loaded from a jar file.");
            return 0;
        }

        RuntimeModInfo info = runtime.get();
        Path pendingPath = UpdateStager.pendingFile(info.gameDir());
        if (!Files.exists(pendingPath)) {
            sendUpdateMessage(source, false, "No CreatureChat update is staged.");
            return 0;
        }

        try {
            PendingUpdate pending = PendingUpdate.load(pendingPath);
            Path log = UpdateHelperLauncher.launch(pending, info.javaExecutable(), ProcessHandle.current().pid());
            sendUpdateMessage(source, true, "CreatureChat " + pending.version()
                    + " is pending. Stop and start the server when ready; the helper will replace the jar after this JVM exits. Log: "
                    + log);
            return 1;
        } catch (Exception e) {
            sendUpdateMessage(source, false, "Could not arm CreatureChat update helper: " + e.getMessage());
            LOGGER.error("CreatureChat update helper launch failed", e);
            return 0;
        }
    }

    private static int showUpdateStatus(CommandSourceStack source) {
        Optional<RuntimeModInfo> runtime = UpdateRuntime.detect();
        if (runtime.isEmpty()) {
            sendUpdateMessage(source, false, "CreatureChat auto-update is only available when the mod is loaded from a jar file.");
            return 0;
        }

        RuntimeModInfo info = runtime.get();
        Path pendingPath = UpdateStager.pendingFile(info.gameDir());
        if (!Files.exists(pendingPath)) {
            sendUpdateMessage(source, true, "CreatureChat " + info.currentVersion() + " is running. No update is staged.");
            return 1;
        }

        try {
            PendingUpdate pending = PendingUpdate.load(pendingPath);
            sendUpdateMessage(source, true, "CreatureChat " + info.currentVersion()
                    + " is running. Staged update: " + pending.version()
                    + " -> " + pending.currentJar());
            return 1;
        } catch (Exception e) {
            sendUpdateMessage(source, false, "Could not read CreatureChat pending update: " + e.getMessage());
            return 0;
        }
    }

    private static UpdateService updateService(RuntimeModInfo info) {
        return new UpdateService(new GitHubReleaseClient(UpdateRuntime.GITHUB_OWNER, UpdateRuntime.GITHUB_REPO, info.currentVersion()));
    }

    private static void sendUpdateMessage(CommandSourceStack source, boolean success, String message) {
        ChatFormatting color = success ? ChatFormatting.GREEN : ChatFormatting.RED;
        source.sendSuccess(() -> Component.literal(message).withStyle(color), false);
    }

    private static int showSetupWizard(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            ServerPackets.sendConfigScreen(player);
            source.sendSuccess(() -> Component.literal("Opening CreatureChat setup screen...").withStyle(ChatFormatting.GREEN), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal("CreatureChat setup (OP only). Values are saved to this server world's creaturechat.json.").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> commandHint("1. Choose provider preset:", "/creaturechat setup provider ai-studio"), false);
        source.sendSuccess(() -> Component.literal("   Providers: " + String.join(", ", ConfigurationPresets.providerIds())).withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> commandHint("2. Add one or more API keys:", "/creaturechat setup key <key1,key2>"), false);
        source.sendSuccess(() -> commandHint("3. Add one or more exact model ids:", "/creaturechat setup model gemini-3.1-flash-lite"), false);
        source.sendSuccess(() -> commandHint("4. Optional output budget:", "/creaturechat setup outputtokens 1024"), false);
        source.sendSuccess(() -> commandHint("5. Optional damage reaction cooldown:", "/creaturechat setup damagecooldown 25"), false);
        source.sendSuccess(() -> commandHint("6. Optional Gemini RPM/RPD:", "/creaturechat setup geminirpm 14"), false);
        source.sendSuccess(() -> commandHint("7. Optional Gemini daily limit:", "/creaturechat setup geminidaily 450"), false);
        source.sendSuccess(() -> commandHint("8. Optional Gemini scope:", "/creaturechat setup geminiscope per_key"), false);
        source.sendSuccess(() -> Component.literal("9. Optional Gemini thinking level is available in the setup screen.").withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> commandHint("10. Review:", "/creaturechat setup show"), false);
        source.sendSuccess(() -> commandHint("11. Test:", "/creaturechat setup test"), false);
        return 1;
    }

    private static MutableComponent commandHint(String label, String command) {
        return Component.literal(label + " ")
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(command).withStyle(style -> style
                        .withColor(ChatFormatting.YELLOW)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to paste this command")))));
    }

    private static int applyPreset(CommandSourceStack source, String provider, boolean useServerConfig) {
        ConfigurationPresets.ProviderPreset preset = ConfigurationPresets.find(provider).orElse(null);
        if (preset == null) {
            source.sendSuccess(() -> Component.literal("Unknown provider preset: " + provider + ". Available: " + String.join(", ", ConfigurationPresets.providerIds())).withStyle(ChatFormatting.RED), false);
            return 0;
        }

        ConfigurationHandler configHandler = new ConfigurationHandler(source.getServer());
        ConfigurationHandler.Config config = configHandler.loadConfig();
        ConfigurationPresets.applyPreset(config, preset);

        if (configHandler.saveConfig(config, useServerConfig)) {
            source.sendSuccess(() -> Component.literal("Preset applied: " + preset.displayName() + " -> " + preset.url() + " / " + preset.defaultModel()).withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("Failed to save preset: " + preset.displayName()).withStyle(ChatFormatting.RED), false);
        return 0;
    }

    private static int showConfig(CommandSourceStack source) {
        ConfigurationHandler.Config config = new ConfigurationHandler(source.getServer()).loadConfig();
        MutableComponent message = Component.literal("CreatureChat config\n").withStyle(ChatFormatting.GOLD)
                .append(Component.literal("URL: " + config.getUrl() + "\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("API keys: " + ConfigurationPresets.describeApiKeys(config.getApiKey()) + "\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Models: " + ConfigurationPresets.describeModels(config.getModel()) + "\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Active model: " + config.getActiveModel() + "\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Thinking: " + config.getThinkingLevel() + "\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Output tokens: " + config.getMaxOutputTokens() + "\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Damage reaction cooldown: " + config.getDamageReactionCooldownSeconds() + "s\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Gemini usage limits: " + (config.getGeminiUsageLimitsEnabled() ? "on" : "off")
                        + ", " + config.getGeminiRequestsPerMinute() + " RPM"
                        + ", " + config.getGeminiRequestsPerDay() + " RPD"
                        + ", scope=" + config.getGeminiUsageLimitScope() + "\n").withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Timeout: " + config.getTimeout() + "s").withStyle(ChatFormatting.GRAY));
        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static int testConfig(CommandSourceStack source) {
        ConfigurationHandler.Config config = new ConfigurationHandler(source.getServer()).loadConfig();
        if (config.getUrl() == null || config.getUrl().isBlank()) {
            source.sendSuccess(() -> Component.literal("Cannot test: URL is empty.").withStyle(ChatFormatting.RED), false);
            return 0;
        }
        if (config.getActiveApiKey().isBlank()) {
            source.sendSuccess(() -> Component.literal("Cannot test: API key is empty.").withStyle(ChatFormatting.RED), false);
            return 0;
        }
        if (config.getActiveModel().isBlank()) {
            source.sendSuccess(() -> Component.literal("Cannot test: model is empty.").withStyle(ChatFormatting.RED), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Testing CreatureChat AI configuration...").withStyle(ChatFormatting.YELLOW), false);
        ChatGPTRequest.fetchMessageFromChatGPT(config, "Reply with exactly: OK", new HashMap<>(), new ArrayList<>(), false)
                .thenAccept(response -> source.getServer().execute(() -> {
                    if (response != null && !response.isBlank()) {
                        source.sendSuccess(() -> Component.literal("CreatureChat AI test succeeded using model: " + config.getActiveModel()).withStyle(ChatFormatting.GREEN), false);
                    } else {
                        String message = ChatGPTRequest.lastErrorMessage != null ? ChatGPTRequest.lastErrorMessage : "No response";
                        source.sendSuccess(() -> Component.literal("CreatureChat AI test failed: " + message).withStyle(ChatFormatting.RED), false);
                    }
                }));
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerChatBubbleCommand() {
        return Commands.literal("chatbubble")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("set")
                        .then(Commands.literal("on")
                                .then(addConfigArgs((context, useServerConfig) -> setChatBubbleEnabled(context, true, useServerConfig)))
                                .executes(context -> setChatBubbleEnabled(context, true, false)))
                        .then(Commands.literal("off")
                                .then(addConfigArgs((context, useServerConfig) -> setChatBubbleEnabled(context, false, useServerConfig)))
                                .executes(context -> setChatBubbleEnabled(context, false, false))));
    }

    private static int setChatBubbleEnabled(CommandContext<CommandSourceStack> context, boolean enabled, boolean useServerConfig) {
        CommandSourceStack source = context.getSource();
        ConfigurationHandler configHandler = new ConfigurationHandler(source.getServer());
        ConfigurationHandler.Config config = configHandler.loadConfig();

        config.setChatBubbles(enabled);

        if (configHandler.saveConfig(config, useServerConfig)) {
            Component feedbackMessage = (enabled ? CCText.CONFIG_CHATBUBBLE_ENABLED : CCText.CONFIG_CHATBUBBLE_DISABLED)
                    .comp().withStyle(ChatFormatting.GREEN);
            source.sendSuccess(() -> feedbackMessage, true);
            return 1;
        } else {
            Component feedbackMessage = CCText.CONFIG_CHATBUBBLE_UPDATE_FAILED.comp().withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> feedbackMessage, false);
            return 0;
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerSendToChatCommand() {
        return Commands.literal("send_to_chat")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("set")
                        .then(Commands.literal("on")
                                .then(addConfigArgs((context, useServerConfig) -> setSendToChatEnabled(context, true, useServerConfig)))
                                .executes(context -> setSendToChatEnabled(context, true, false)))
                        .then(Commands.literal("off")
                                .then(addConfigArgs((context, useServerConfig) -> setSendToChatEnabled(context, false, useServerConfig)))
                                .executes(context -> setSendToChatEnabled(context, false, false))));
    }

    private static int setSendToChatEnabled(CommandContext<CommandSourceStack> context, boolean enabled, boolean useServerConfig) {
        CommandSourceStack source = context.getSource();
        ConfigurationHandler configHandler = new ConfigurationHandler(source.getServer());
        ConfigurationHandler.Config config = configHandler.loadConfig();

        config.setSendToChat(enabled);

        if (configHandler.saveConfig(config, useServerConfig)) {
            Component feedbackMessage = (enabled ? CCText.CONFIG_SENDTOCHAT_ENABLED : CCText.CONFIG_SENDTOCHAT_DISABLED)
                    .comp().withStyle(ChatFormatting.GREEN);
            source.sendSuccess(() -> feedbackMessage, true);
            return 1;
        } else {
            Component feedbackMessage = CCText.CONFIG_SENDTOCHAT_UPDATE_FAILED.comp().withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> feedbackMessage, false);
            return 0;
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerOverhearCommand() {
        return Commands.literal("overhear")
                .executes(context -> showOverhearStatus(context.getSource()))
                .then(Commands.literal("status")
                        .executes(context -> showOverhearStatus(context.getSource())))
                .then(Commands.literal("on")
                        .executes(context -> setOverhearEnabled(context.getSource(), true)))
                .then(Commands.literal("off")
                        .executes(context -> setOverhearEnabled(context.getSource(), false)));
    }

    private static int showOverhearStatus(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean enabled = ServerPackets.getShowOtherNpcReplies(player);
        Component message = (enabled ? CCText.CONFIG_OVERHEAR_STATUS_ENABLED : CCText.CONFIG_OVERHEAR_STATUS_DISABLED)
                .comp()
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static int setOverhearEnabled(CommandSourceStack source, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ServerPackets.setShowOtherNpcReplies(player, enabled);
        Component message = (enabled ? CCText.CONFIG_OVERHEAR_ENABLED : CCText.CONFIG_OVERHEAR_DISABLED)
                .comp()
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
        source.sendSuccess(() -> message, false);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerWhitelistCommand() {
        return Commands.literal("whitelist")
                .requires(source -> source.hasPermission(4))
                .then(Commands.argument("entityType", ResourceLocationArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(getLivingEntityIds(), builder))
                        .then(addConfigArgs((context, useServerConfig) -> modifyList(context, "whitelist", ResourceLocationArgument.getId(context, "entityType").toString(), useServerConfig)))
                        .executes(context -> modifyList(context, "whitelist", ResourceLocationArgument.getId(context, "entityType").toString(), false)))
                .then(Commands.literal("all")
                        .then(addConfigArgs((context, useServerConfig) -> modifyList(context, "whitelist", "all", useServerConfig)))
                        .executes(context -> modifyList(context, "whitelist", "all", false)))
                .then(Commands.literal("clear")
                        .then(addConfigArgs((context, useServerConfig) -> modifyList(context, "whitelist", "clear", useServerConfig)))
                        .executes(context -> modifyList(context, "whitelist", "clear", false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerBlacklistCommand() {
        return Commands.literal("blacklist")
                .requires(source -> source.hasPermission(4))
                .then(Commands.argument("entityType", ResourceLocationArgument.id())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(getLivingEntityIds(), builder))
                        .then(addConfigArgs((context, useServerConfig) -> modifyList(context, "blacklist", ResourceLocationArgument.getId(context, "entityType").toString(), useServerConfig)))
                        .executes(context -> modifyList(context, "blacklist", ResourceLocationArgument.getId(context, "entityType").toString(), false)))
                .then(Commands.literal("all")
                        .then(addConfigArgs((context, useServerConfig) -> modifyList(context, "blacklist", "all", useServerConfig)))
                        .executes(context -> modifyList(context, "blacklist", "all", false)))
                .then(Commands.literal("clear")
                        .then(addConfigArgs((context, useServerConfig) -> modifyList(context, "blacklist", "clear", useServerConfig)))
                        .executes(context -> modifyList(context, "blacklist", "clear", false)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerHelpCommand() {
        return Commands.literal("help")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> CCText.CONFIG_HELP.comp(), false);
                    return 1;
                });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> registerStoryCommand() {
        return Commands.literal("story")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("set")
                        .then(Commands.argument("value", StringArgumentType.string())
                                .then(addConfigArgs((context, useServerConfig) -> {
                                    String story = StringArgumentType.getString(context, "value");
                                    ConfigurationHandler.Config config = new ConfigurationHandler(context.getSource().getServer()).loadConfig();
                                    config.setStory(story);
                                    if (new ConfigurationHandler(context.getSource().getServer()).saveConfig(config, useServerConfig)) {
                                        context.getSource().sendSuccess(() -> CCText.CONFIG_STORY_SET_SUCCESS.comp(story).withStyle(ChatFormatting.GREEN), true);
                                        return 1;
                                    } else {
                                        context.getSource().sendSuccess(() -> CCText.CONFIG_STORY_SET_FAILED.comp().withStyle(ChatFormatting.RED), false);
                                        return 0;
                                    }
                                }))))
                .then(Commands.literal("clear")
                        .then(addConfigArgs((context, useServerConfig) -> {
                            ConfigurationHandler.Config config = new ConfigurationHandler(context.getSource().getServer()).loadConfig();
                            config.setStory("");
                            if (new ConfigurationHandler(context.getSource().getServer()).saveConfig(config, useServerConfig)) {
                            context.getSource().sendSuccess(() -> CCText.CONFIG_STORY_CLEARED_SUCCESS.comp().withStyle(ChatFormatting.GREEN), true);
                                return 1;
                            } else {
                            context.getSource().sendSuccess(() -> CCText.CONFIG_STORY_CLEARED_FAILED.comp().withStyle(ChatFormatting.RED), false);
                                return 0;
                            }
                        })))
                .then(Commands.literal("display")
                .executes(context -> {
                    ConfigurationHandler.Config config = new ConfigurationHandler(context.getSource().getServer()).loadConfig();
                    String story = config.getStory();
                    if (story == null || story.isEmpty()) {
                        context.getSource().sendSuccess(() -> CCText.CONFIG_STORY_NOT_SET.comp().withStyle(ChatFormatting.RED), false);
                        return 0;
                    } else {
                        context.getSource().sendSuccess(() -> CCText.CONFIG_STORY_SHOW.comp(story).withStyle(ChatFormatting.AQUA), false);
                        return 1;
                    }
                }));
    }

    private static <T> int setConfig(CommandSourceStack source, String settingName, T value, boolean useServerConfig, String settingDescription) {
        ConfigurationHandler configHandler = new ConfigurationHandler(source.getServer());
        ConfigurationHandler.Config config = configHandler.loadConfig();
        try {
            switch (settingName) {
                case "key":
                    config.setApiKey((String) value);
                    break;
                case "url":
                    config.setUrl((String) value);
                    break;
                case "model":
                    config.setModel((String) value);
                    break;
                case "timeout":
                    if (value instanceof Integer) {
                        config.setTimeout((Integer) value);
                    } else {
                        throw new IllegalArgumentException(CCText.CONFIG_TIMEOUT_INVALID_TYPE.comp().getString());
                    }
                    break;
                case "outputtokens":
                    if (value instanceof Integer) {
                        config.setMaxOutputTokens((Integer) value);
                    } else {
                        throw new IllegalArgumentException("Invalid type for output tokens, must be Integer.");
                    }
                    break;
                case "damagecooldown":
                    if (value instanceof Integer) {
                        config.setDamageReactionCooldownSeconds((Integer) value);
                    } else {
                        throw new IllegalArgumentException("Invalid type for damage reaction cooldown, must be Integer.");
                    }
                    break;
                case "geminirpm":
                    if (value instanceof Integer) {
                        config.setGeminiRequestsPerMinute((Integer) value);
                    } else {
                        throw new IllegalArgumentException("Invalid type for Gemini RPM, must be Integer.");
                    }
                    break;
                case "geminidaily":
                    if (value instanceof Integer) {
                        config.setGeminiRequestsPerDay((Integer) value);
                    } else {
                        throw new IllegalArgumentException("Invalid type for Gemini daily limit, must be Integer.");
                    }
                    break;
                case "geminiscope":
                    if (value instanceof String) {
                        String scope = ((String) value).trim();
                        if (!scope.equalsIgnoreCase("per_key") && !scope.equalsIgnoreCase("shared")) {
                            throw new IllegalArgumentException("Gemini usage limit scope must be per_key or shared.");
                        }
                        config.setGeminiUsageLimitScope(scope);
                    } else {
                        throw new IllegalArgumentException("Invalid type for Gemini usage limit scope, must be String.");
                    }
                    break;
                default:
                    throw new IllegalArgumentException(CCText.CONFIG_UNKNOWN_SETTING.comp(settingName).getString());
            }
        } catch (ClassCastException e) {
            Component errorMessage = CCText.CONFIG_INVALID_SETTING_TYPE.comp(settingName).withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> errorMessage, false);
            LOGGER.error("Type mismatch during configuration setting for: " + settingName, e);
            return 0;
        } catch (IllegalArgumentException e) {
            Component errorMessage = Component.literal(e.getMessage()).withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> errorMessage, false);
            LOGGER.error("Error setting configuration: " + e.getMessage(), e);
            return 0;
        }

        Component feedbackMessage;
        if (configHandler.saveConfig(config, useServerConfig)) {
            feedbackMessage = CCText.CONFIG_SETTING_SET_SUCCESS.comp(settingDescription).withStyle(ChatFormatting.GREEN);
            source.sendSuccess(() -> feedbackMessage, false);
            LOGGER.info("Command executed: " + feedbackMessage.getString());
            return 1;
        } else {
            feedbackMessage = CCText.CONFIG_SETTING_SET_FAILED.comp(settingDescription).withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> feedbackMessage, false);
            LOGGER.info("Command executed: " + feedbackMessage.getString());
            return 0;
        }
    }

    private static int modifyList(CommandContext<CommandSourceStack> context, String listName, String action, boolean useServerConfig) {
        CommandSourceStack source = context.getSource();
        ConfigurationHandler configHandler = new ConfigurationHandler(source.getServer());
        ConfigurationHandler.Config config = configHandler.loadConfig();
        List<String> entityTypes = getLivingEntityTypeNames();

        try {
            if ("all".equals(action)) {
                if ("whitelist".equals(listName)) {
                    config.setWhitelist(entityTypes);
                    config.setBlacklist(new ArrayList<>()); // Clear blacklist
                } else if ("blacklist".equals(listName)) {
                    config.setBlacklist(entityTypes);
                    config.setWhitelist(new ArrayList<>()); // Clear whitelist
                }
            } else if ("clear".equals(action)) {
                if ("whitelist".equals(listName)) {
                    config.setWhitelist(new ArrayList<>());
                } else if ("blacklist".equals(listName)) {
                    config.setBlacklist(new ArrayList<>());
                }
            } else {
                if (!entityTypes.contains(action)) {
                    throw new IllegalArgumentException("Invalid entity type: " + action);
                }
                if ("whitelist".equals(listName)) {
                    List<String> whitelist = new ArrayList<>(config.getWhitelist());
                    if (!whitelist.contains(action)) {
                        whitelist.add(action);
                        config.setWhitelist(whitelist);
                    }
                    // Remove from blacklist if present
                    List<String> blacklist = new ArrayList<>(config.getBlacklist());
                    blacklist.remove(action);
                    config.setBlacklist(blacklist);
                } else if ("blacklist".equals(listName)) {
                    List<String> blacklist = new ArrayList<>(config.getBlacklist());
                    if (!blacklist.contains(action)) {
                        blacklist.add(action);
                        config.setBlacklist(blacklist);
                    }
                    // Remove from whitelist if present
                    List<String> whitelist = new ArrayList<>(config.getWhitelist());
                    whitelist.remove(action);
                    config.setWhitelist(whitelist);
                }
            }
        } catch (IllegalArgumentException e) {
            Component errorMessage = Component.literal(e.getMessage()).withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> errorMessage, false);
            LOGGER.error("Error modifying list: " + e.getMessage(), e);
            return 0;
        }

        if (configHandler.saveConfig(config, useServerConfig)) {
            Component feedbackMessage = CCText.CONFIG_LIST_UPDATE_SUCCESS.comp(listName, action).withStyle(ChatFormatting.GREEN);
            source.sendSuccess(() -> feedbackMessage, false);

            // Send whitelist / blacklist to all players
            ServerPackets.send_whitelist_blacklist(null);
            return 1;
        } else {
            Component feedbackMessage = CCText.CONFIG_LIST_UPDATE_FAILED.comp(listName).withStyle(ChatFormatting.RED);
            source.sendSuccess(() -> feedbackMessage, false);
            return 0;
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> addConfigArgs(CommandExecutor executor) {
        return Commands.literal("--config")
                .then(Commands.literal("default").executes(context -> executor.run(context, false)))
                .then(Commands.literal("server").executes(context -> executor.run(context, true)))
                .executes(context -> executor.run(context, false));
    }

    @FunctionalInterface
    private interface CommandExecutor {
        int run(CommandContext<CommandSourceStack> context, boolean useServerConfig) throws CommandSyntaxException;
    }
}
