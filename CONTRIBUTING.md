# Contributing to MobChat

Contributions target the [MobChat repository](https://github.com/Le-Who/MobChat), the local fork of CreatureChat. Start with [INSTALL.md](INSTALL.md) for the build environment and [ARCHITECTURE.md](ARCHITECTURE.md) for module ownership and data flow. Local source and configuration describe this fork's behavior.

## Find the owner before changing behavior

Common/server code lives under `src/main/java/com/lewho/`; client-only screens, rendering, skins, and packet handling live under `src/client/java/com/lewho/`. The build uses official Mojang names such as `ServerPlayer`, `ServerLevel`, `Mob`, and `MinecraftServer`.

Use the existing owner for a change:

- Conversation state, prompt context, provider requests, memories, social events, and quotas: `chat/`.
- Configuration, provider presets, setup commands, and custom roles: `commands/`.
- Action arbitration: `chat/BehaviorPolicy`; dynamic goal installation: `goals/EntityBehaviorManager`.
- Entity hooks and saved inventory/identity adapters: `mixin/`; menus and item accounting: `inventory/`.
- Client/server synchronization: `network/`; client interaction and bubbles: client `ui/` and `render/`.
- Download, staging, hash checks, and post-exit installation: `update/`.

Prefer an existing pattern over a new abstraction. A narrow helper is useful when it centralizes policy repeated across version adapters or exposes a meaningful behavior for tests. Avoid copying large classes into `src/vs/` for small API differences.

## Contracts to preserve

Minecraft mutations and traversal of mutable server state belong on the server thread. HTTP runs asynchronously; applying its result must return through the current server session and validate entity/player/world ownership. A concurrent outer map does not make mutable histories, memories, inventories, or player records thread-safe.

Chat and character generation use structured JSON contracts. Preserve `NONE`, `CHAT`, and `CHARACTER`, the required schemas, and output-token floors. Add parser/request regressions before changing response salvage or schema behavior. Provider routing must keep native Gemini and OpenAI-compatible payloads distinct while sharing quota/retry/error policy.

Record player social events through `SocialEventRecorder`. Automatic LLM triggers need the existing player/entity buckets, dedicated damage or ambient cooldowns, and provider preflight. Keep provider `429` handling even with local limits. Use UUID-scoped player lookup and preserve migration of older save data.

Configuration packets use `ConfigurationScreenData`; full stored API keys remain on the server. Provider diagnostics must redact configured keys before logging or displaying them. Client display preferences filter current reply feeds; shared entity conversation history is a separate data contract.

Hand slots mirror mob equipment. Both partial extraction and merging into an occupied hand must synchronize the resulting stack. Persisted chat/player/home data is distinct from live goal instances; saving metadata does not recreate a goal after restart.

## Tests

Run the relevant targeted suite, then the ordinary build:

```powershell
.\gradlew.bat test --tests com.lewho.tests.ChatGPTRequestStructuredOutputTests
.\gradlew.bat test --tests com.lewho.tests.ChatGPTRequestUsageLimitTests
.\gradlew.bat test --tests com.lewho.tests.GeminiNativePolicyTests
.\gradlew.bat test --tests com.lewho.tests.DamageReactionRateLimitTests
.\gradlew.bat test --tests com.lewho.tests.ChatStateLifecycleTests --tests com.lewho.tests.ChatRequestDispatchTests --tests com.lewho.tests.ChatDataFileTests
.\gradlew.bat test --tests com.lewho.tests.MobHandSlotTests
.\gradlew.bat test --tests com.lewho.inventory.MobInventoryAccessTests
.\gradlew.bat build
```

Select commands appropriate to the change; the test map in [ARCHITECTURE.md](ARCHITECTURE.md) points to other focused suites. A fresh complete test run is `.\gradlew.bat test --rerun-tasks --console=plain`. For documentation-only changes, run `git diff --check` and check local links.

Ordinary provider tests use localhost HTTP fixtures and temporary files, with synthetic credentials. Keep `API_KEY` unset for offline verification. `BehaviorTests` is an optional live-provider suite: it reads `API_KEY`, `API_URL`, and `API_MODEL`, incurs real requests when enabled, and writes `src/test/BehaviorOutputs.json` during cleanup. It is skipped without a key. Review fixture changes from intentional live runs; do not confuse skips with verified live-provider behavior.

Write behavioral regressions that catch the original defect. Real `ItemStack`/container fixtures, manually drained executors, temporary files, and controlled HTTP responses are preferable to assertions about source text or trivial getters. Headless tests do not establish rendering correctness or live Minecraft behavior; use `runClient`/`runServer` when those are the changed surfaces.

## Version adapters and resources

For a Minecraft API change, inspect the base class and every later applicable override of the same path. Overrides replace whole classes, so a gameplay fix in base can disappear on newer targets. Keep common gameplay policy in shared hooks and leave target signatures, serialization, and platform API calls in the adapters.

Verify the build's printed override selection. Check target-specific dependencies and metadata before packaging another Minecraft version. The current default-target build and static inspection of newer adapters are different forms of evidence; report each accurately.

Prompt templates live in `src/main/resources/data/creaturechat/prompts/`. Resource-manager loading permits data-pack overrides. `system-quest` is presently unused. Loot tables and advancements have checked-in data plus datagen providers. Translations use fallback text in code; new keys need a matching datagen update. `runDatagen` can change tracked locale files, as described in [INSTALL.md](INSTALL.md).

`custom_roles.json` is read from the current world root first, then the process working directory. Roles are keyed by the entity's displayed type name. The legacy `generate_roles.py` writes `data/creaturechat/custom_roles.json`; move its output into one of the runtime locations before using it. It is optional tooling, not part of the build.

## Documentation and releases

Update `README.md` for player/admin behavior, `INSTALL.md` for installation/build changes, and this guide for contributor workflow. Update the architecture map when ownership, data flow, state lifecycle, or public contracts change. Keep `AGENTS.md` for durable coding instructions and release history in `CHANGELOG.md`.

Add release-worthy notes under `## Unreleased` while developing. Before declaring a user-visible jar ready for distribution, bump `mod_version` according to SemVer and move its notes into a dated version section. Build the remapped distribution, verify metadata and SHA-512, and include each target's jar/hash pair in a GitHub release for the updater.

Use a descriptive branch for contributions. Keep changes focused, preserve unrelated work, and include a concrete problem/result and validation in a PR or patch. Commits, pushes, external publication, and merging remain explicit repository/user actions.

## Licensing and reports

Preserve existing license headers. New Java files use the project SPDX header:

```java
// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
```

Source code is under [GPL-3.0-or-later](LICENSE.md); non-code assets have the separate [CC-BY-NC-SA-4.0 terms](LICENSE-ASSETS.md). Preserve attribution, ShareAlike, and non-commercial asset conditions. The original CreatureChat trademark and branding terms remain separate; use the licensing documents for their complete conditions.

Report bugs through the MobChat tracker or the current development chat. Include Minecraft/mod/loader versions, reproduction steps, expected and actual behavior, sanitized logs, and whether the problem occurs with the configured provider or a localhost fixture.
