# MobChat Agent Instructions

These instructions apply to the whole `E:\Projects\MobChat` workspace.

## Project Identity

- This is the MobChat fork of CreatureChat at `https://github.com/Le-Who/MobChat`.
- Local source, local docs, and server configuration are authoritative for this fork.
- Do not use public upstream CreatureChat websites, Modrinth/CurseForge pages, Discord links, or upstream GitHub docs as authoritative unless the user explicitly asks for an upstream comparison.
- AI Villager development is paused. The former `ai_villager/` compiled artifact tree and `ai_villager_src/` decompiled source tree are intentionally absent.
- Keep support links and setup guidance pointed at this repository and local/server configuration.

## Current Runtime Shape

- Default Minecraft target is defined by `minecraft_version` in `gradle.properties`.
- Build system: Gradle with Fabric Loom.
- Java source/target compatibility: Java 17.
- The compiler toolchain is declared in `build.gradle`; its local path is configured in `gradle.properties`. Source/API release compatibility is separate from the JDK used to compile.
- Official Mojang mappings are used through `loom.officialMojangMappings()`. Search and code with Mojang names such as `ServerPlayer`, `Mob`, `LivingEntity`, and `MinecraftServer`.
- Version-specific source overrides live under `src/vs/vX_Y_Z/` and are applied by `build.gradle` when the target Minecraft version is greater than or equal to the folder version.
- Before cross-module changes, read `ARCHITECTURE.md` for ownership, data boundaries, response/save lifetime and version adapters. Use `CONTRIBUTING.md` for test/release workflow and `INSTALL.md` for toolchain setup.

## Folder Map

- `src/main/java/com/lewho/chat/`: chat state, prompt flow, LLM request handling, memories, social events, usage limiting, and parsing helpers.
- `src/main/java/com/lewho/commands/`: config loading/saving, provider presets, setup commands, custom roles.
- `src/main/java/com/lewho/goals/`: AI-driven entity goals and `EntityBehaviorManager`.
- `src/main/java/com/lewho/mixin/`: server/common mixins into Minecraft entities and chat hooks.
- `src/main/java/com/lewho/network/`: server packets and server/client sync entry points.
- `src/main/java/com/lewho/inventory/`: mob inventory menu, loot, and inventory behavior.
- `src/client/java/com/lewho/`: client UI, rendering, packet handlers, particles, and screens.
- `src/main/java/com/lewho/update/` and `src/client/java/com/lewho/update/`: release selection, verified staging, exit-time helper installation, and client consent.
- `src/main/resources/data/creaturechat/prompts/`: LLM prompt templates.
- `src/main/resources/assets/creaturechat/lang/`: translations.
- `src/main/resources/data/creaturechat/loot_tables/`: loot tables used by mob inventories.
- `src/test/java/com/lewho/tests/`: JUnit tests for parser, request, configuration, rate limit, behavior policy, and data classes.
- `generate_roles.py`: local tooling for role data generation.
- `.agents/` and `skills-lock.json`: local agent tooling, not product code.

## Working Rules

- Keep edits scoped to the task and to the module already responsible for the behavior.
- Prefer existing project patterns over new abstractions. Add a new abstraction only when it clearly reduces complexity or matches an established local pattern.
- Do not revert user changes or unrelated local work.
- Do not point docs, metadata, setup flows, or error text back to upstream CreatureChat services.
- Do not commit or push unless the user asks for it.
- For docs, keep README files human-facing and keep `AGENTS.md` focused on durable instructions for coding agents.

## Build Commands

Use the Gradle wrapper from the repository root.

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
build/libs/creaturechat-<mod_version>+<minecraft_version>.jar
```

Read `mod_version` and `minecraft_version` from `gradle.properties`; do not copy a hardcoded version from this file.

`build.sh` builds a historical multi-version matrix, edits `gradle.properties` and `fabric.mod.json`, and does not restore them. It skips tests/access-widener validation. Prefer `.\gradlew.bat build` for ordinary validation unless the user asks for multi-version packaging.

## LLM And JSON Contracts

- `ChatGPTRequest` owns shared request orchestration. OpenAI-compatible chat completions and native Gemini payload/envelope adapters retain their separate wire formats.
- Gameplay and setup callbacks consume their own immutable `ChatGPTRequest.RequestResult`. Legacy static `last*` fields are compatibility diagnostics, not request ownership.
- `ChatGPTResponse`, `MessageParser`, and `CharacterSheetNormalizer` depend on strict structured JSON contracts.
- Do not replace schema-backed chat or character generation with free-form text parsing.
- Preserve the output modes in `ChatGPTRequest.StructuredOutputMode`: `NONE`, `CHAT`, and `CHARACTER`.
- Structured output needs enough `max_tokens` / `maxOutputTokens`; do not lower floors without tests proving chat and character JSON are not truncated.
- Redact all configured keys before logging or reporting errors, including decoded provider JSON. Request results and legacy diagnostics must not expose raw keys.
- Keep input snapshots, quota preflight, candidate traversal and retry policy in the shared runner when adding a provider adapter.

## Provider And Quota Handling

- Provider presets live in `ConfigurationPresets`.
- Read the current model and URL from `ConfigurationPresets`; the Google AI Studio preset uses native `generateContent` at the Gemini `v1beta` endpoint. Google URLs with `/openai` retain the compatible shape.
- `ApiUsageLimiter` preflights Gemini usage before every HTTP attempt, including retries and native requests:
  - default RPM: `14`
  - default RPD: `450`
  - default scope: `per_key`
  - runtime state file: `creaturechat_usage.json`
- `creaturechat_usage.json` is runtime state and must remain ignored by Git.
- `creaturechat.json` contains credentials and `chatdata.json` contains world conversations; keep both as ignored runtime files.
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
/creaturechat setup language <locale_code|auto>
/creaturechat setup show
/creaturechat setup test
```

The setup screen must not echo stored API keys back to clients. `ConfigurationScreenData` is the sanitized DTO for that boundary.

## Gameplay And State Rules

- Use `EntityChatData` and `PlayerData` for persisted per-entity/per-player chat state.
- Admit character/chat requests through the current `ChatSession` before changing history or automatic-message state. Apply the whole callback on the originating server executor, after checking current session, state, player, entity and world identity.
- Create mutable chat snapshots on the server thread. Stop request/save scheduling before the final save; preserve the last good file if serialization fails.
- Use `SocialEventRecorder` for player social events instead of manually changing summaries in random call sites.
- Automatic reactions must be rate-limited. Check existing `ChatDataManager`, `AutoMessageBucket`, damage cooldown, ambient response, and Gemini usage limiter patterns before adding a new automatic LLM path.
- Dynamic behavior should go through `EntityBehaviorManager` and existing goal classes. Do not mutate goal selectors from scattered code without checking existing manager behavior.
- Inventory transfer must honor slot permission for both occupied-stack merges and empty destinations. Hand container slots must mirror equipment after in-place mutations as well as `set`/`onTake`.
- Mixins are high risk. Keep guards early, casts checked, and edits surgical. Avoid broad mixin changes unless the target method and version behavior are clear.
- Server/world/entity mutations must stay on the server thread unless the Minecraft/Fabric API explicitly allows otherwise.

## Version-Specific Overrides

When Minecraft API differences require source changes:

- Prefer a small helper class that can be overridden under `src/vs/`.
- Avoid copying a large class into `src/vs/` when a narrow adapter would work.
- Overrides are cumulative whole-class replacements. Preserve shared `LivingEntityChatHooks` / `MobInteractionHooks` delegation and UUID-based player state when adapting Minecraft signatures.
- If adding an override, verify the folder version naming and build output from the version-selection block in `build.gradle`.

## Tests And Verification

Add or update targeted tests for behavior changes when practical.

Common test areas:

- `ChatGPTRequestStructuredOutputTests`: request payloads, JSON schema, structured output diagnostics.
- `ChatGPTRequestUsageLimitTests` and `GeminiUsageLimiterTests`: local quota and key rotation behavior.
- `GeminiNativePolicyTests`: shared native/compatible fallback, input snapshots, per-request diagnostics, redaction and native payload policy.
- `ChatStateLifecycleTests`, `ChatRequestDispatchTests` and `ChatDataFileTests`: deferred callbacks, admission before quota/cooldown effects, stale/cancelled requests, pending recovery and file replacement.
- `MobHandSlotTests`, `MobInventorySocialTests` and `com.lewho.inventory.MobInventoryAccessTests`: equipment mirroring, disarm accounting and slot access during transfers.
- `DamageReactionRateLimitTests`: combat-triggered auto reply cooldown.
- `AmbientRateLimitTests`: proximity and mob-to-mob auto-response throttling.
- `StructuredResponseParserTests`: parser behavior for structured responses and salvage paths.
- `BehaviorPolicyTests`: server-side action arbitration.

Before finishing source changes, run the relevant targeted tests and then `.\gradlew.bat build` when practical. For docs-only changes, at least run `git diff --check`.

Ordinary verification uses localhost provider fixtures. `BehaviorTests` needs `API_KEY`, calls a live provider and can change `src/test/BehaviorOutputs.json`; keep it separate from offline validation. `runDatagen` clears generated resources and can edit tracked locales through `LangSync`, so review its diff explicitly.

## Documentation Rules

- Update `README.md` for player/admin setup and behavior changes.
- Update `INSTALL.md` only for install/build changes.
- Update `CONTRIBUTING.md` for contributor workflow changes.
- Update `CHANGELOG.md` under `## Unreleased` for release-note-worthy user-visible behavior changes. Docs-only cleanups may skip the changelog unless the user asks for release notes.
- When a user-visible change is intended to ship through the built jar or updater, bump `mod_version` in `gradle.properties` according to SemVer and move the relevant changelog notes into a dated `## [version] - YYYY-MM-DD` section before claiming the build is release/update-ready.
- Keep docs specific to the MobChat fork. Do not restore upstream CreatureChat support links.
- Do not make `AGENTS.md` a changelog. Put release/user history in `CHANGELOG.md` when a user-visible mod behavior change needs release notes.

## SPDX And Licensing

New Java files should start with the project SPDX header used by nearby files:

```java
// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
```

If a file already carries the longer asset/trademark notice, preserve that style. Do not remove license headers.
