# CreatureChat Agent Instructions

This file applies to all work inside `CreatureChat/`. It narrows the root `AGENTS.md` for the active Fabric mod project.

## Project Identity

- This is the MobChat fork of CreatureChat at `https://github.com/Le-Who/MobChat`.
- Local source, local docs, and server configuration are authoritative for this fork.
- Do not use public upstream CreatureChat websites, Modrinth/CurseForge pages, Discord links, or upstream GitHub docs as authoritative unless the user explicitly asks for an upstream comparison.
- Keep support links and setup guidance pointed at this repository and local/server configuration.

## Current Runtime Shape

- Minecraft target in this branch: `1.20.1`.
- Build system: Gradle with Fabric Loom.
- Java source/target compatibility: Java 17.
- Local Gradle toolchain path is configured in `gradle.properties`.
- Official Mojang mappings are used through `loom.officialMojangMappings()`. Search and code with Mojang names such as `ServerPlayer`, `Mob`, `LivingEntity`, and `MinecraftServer`.
- Version-specific source overrides live under `src/vs/vX_Y_Z/` and are applied by `build.gradle` when the target Minecraft version is greater than or equal to the folder version.

## Folder Map

- `src/main/java/com/lewho/chat/`: chat state, prompt flow, LLM request handling, memories, social events, usage limiting, and parsing helpers.
- `src/main/java/com/lewho/commands/`: config loading/saving, provider presets, setup commands, custom roles.
- `src/main/java/com/lewho/goals/`: AI-driven entity goals and `EntityBehaviorManager`.
- `src/main/java/com/lewho/mixin/`: server/common mixins into Minecraft entities and chat hooks.
- `src/main/java/com/lewho/network/`: server packets and server/client sync entry points.
- `src/main/java/com/lewho/inventory/`: mob inventory menu, loot, and inventory behavior.
- `src/client/java/com/lewho/`: client-only UI, rendering, packet handlers, particles, and screens.
- `src/main/resources/data/creaturechat/prompts/`: LLM prompt templates.
- `src/main/resources/assets/creaturechat/lang/`: translations.
- `src/test/java/com/lewho/tests/`: JUnit tests for parser, request, configuration, rate limit, behavior policy, and data classes.

## Build Commands

Use the Gradle wrapper from `CreatureChat/`.

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

Targeted tests:

```powershell
.\gradlew.bat test --tests com.lewho.tests.ChatGPTRequestStructuredOutputTests
.\gradlew.bat test --tests com.lewho.tests.ChatGPTRequestUsageLimitTests
.\gradlew.bat test --tests com.lewho.tests.DamageReactionRateLimitTests
```

The release jar is generated under:

```text
build/libs/creaturechat-3.0.0+1.20.1.jar
```

`build.sh` can build multiple Minecraft versions and may temporarily edit `gradle.properties` and `fabric.mod.json`. Prefer `.\gradlew.bat build` for ordinary validation unless the user asks for multi-version packaging.

## LLM And JSON Contracts

- `ChatGPTRequest` sends OpenAI-compatible chat completions requests.
- `ChatGPTResponse`, `MessageParser`, and `CharacterSheetNormalizer` depend on strict structured JSON contracts.
- Do not replace schema-backed chat or character generation with free-form text parsing.
- Preserve the output modes in `ChatGPTRequest.StructuredOutputMode`: `NONE`, `CHAT`, and `CHARACTER`.
- Structured output needs enough `max_tokens`; do not lower floors without tests proving chat and character JSON are not truncated.
- `ChatGPTRequest.lastErrorMessage` must not leak raw API keys.
- When adding provider support, prefer OpenAI-compatible request/response shapes already used by `ConfigurationPresets`.

## Provider And Quota Handling

- Provider presets live in `ConfigurationPresets`.
- Google AI Studio preset uses `gemini-3.1-flash-lite` and the Gemini OpenAI-compatible endpoint.
- `ApiUsageLimiter` preflights Gemini usage before HTTP requests:
  - default RPM: `14`
  - default RPD: `450`
  - default scope: `per_key`
  - runtime state file: `creaturechat_usage.json`
- `creaturechat_usage.json` is runtime state and must remain ignored by Git.
- If multiple AI Studio keys belong to one Google project, admins can use `geminiscope shared`; otherwise `per_key` preserves key rotation.
- Keep `429` handling as a fallback even when local preflight limiting exists.

## Configuration Surface

Primary player/admin path:

```text
/creaturechat setup
```

Important command-backed settings:

```text
/creaturechat setup provider <openai|ai-studio|openrouter|groq|ollama|litellm>
/creaturechat setup key <key1,key2>
/creaturechat setup model <model1,model2>
/creaturechat setup outputtokens <value>
/creaturechat setup damagecooldown <seconds>
/creaturechat setup geminirpm <requests>
/creaturechat setup geminidaily <requests>
/creaturechat setup geminiscope <per_key|shared>
/creaturechat setup show
/creaturechat setup test
```

The setup screen must not echo stored API keys back to clients. `ConfigurationScreenData` is the sanitized DTO for that boundary.

## Gameplay And State Rules

- Use `EntityChatData` and `PlayerData` for persisted per-entity/per-player chat state.
- Use `SocialEventRecorder` for player social events instead of manually changing summaries in random call sites.
- Automatic reactions must be rate-limited. Check existing `ChatDataManager`, `AutoMessageBucket`, damage cooldown, ambient response, and Gemini usage limiter patterns before adding a new automatic LLM path.
- Dynamic behavior should go through `EntityBehaviorManager` and existing goal classes. Do not mutate goal selectors from scattered code without checking existing manager behavior.
- Mixins are high risk. Keep guards early, casts checked, and edits surgical. Avoid broad mixin changes unless the target method and version behavior are clear.
- Server/world/entity mutations must stay on the server thread unless the Minecraft/Fabric API explicitly allows otherwise.

## Version-Specific Overrides

When Minecraft API differences require source changes:

- Prefer a small helper class that can be overridden under `src/vs/`.
- Avoid copying a large class into `src/vs/` when a narrow adapter would work.
- If adding an override, verify the folder version naming and build output from the version-selection block in `build.gradle`.

## Tests And Verification

Add or update targeted tests for behavior changes when practical.

Common test areas:

- `ChatGPTRequestStructuredOutputTests`: request payloads, JSON schema, structured output diagnostics.
- `ChatGPTRequestUsageLimitTests` and `GeminiUsageLimiterTests`: local quota and key rotation behavior.
- `DamageReactionRateLimitTests`: combat-triggered auto reply cooldown.
- `AmbientRateLimitTests`: proximity and mob-to-mob auto-response throttling.
- `StructuredResponseParserTests`: parser behavior for structured responses and salvage paths.
- `BehaviorPolicyTests`: server-side action arbitration.

Before finishing source changes, run the relevant targeted tests and then `.\gradlew.bat build` when practical. For docs-only changes, at least run `git diff --check`.

## Documentation Rules

- Update `README.md` for admin/player setup and behavior changes.
- Update `INSTALL.md` only for install/build changes.
- Update `CONTRIBUTING.md` for contributor workflow changes.
- Update `CHANGELOG.md` under `## Unreleased` for release-note-worthy user-visible behavior changes. Docs-only cleanups may skip the changelog unless the user asks for release notes.
- Keep docs specific to the MobChat fork. Do not restore upstream CreatureChat support links.

## SPDX And Licensing

New Java files should start with the project SPDX header used by nearby files:

```java
// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
```

If a file already carries the longer asset/trademark notice, preserve that style. Do not remove license headers.
