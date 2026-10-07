# MobChat Agent Instructions

These instructions apply throughout this repository.

## Project Identity

- This is the [MobChat fork of CreatureChat](https://github.com/Le-Who/MobChat). Local source, local docs, and server configuration are authoritative.
- Keep support links, metadata, setup flows, and error guidance pointed at this repository and local/server configuration. Public upstream CreatureChat websites, Modrinth/CurseForge pages, Discord links, and upstream GitHub docs are authoritative only when the user explicitly asks for an upstream comparison.
- AI Villager development is paused. The former `ai_villager/` compiled artifact tree and `ai_villager_src/` decompiled source tree are intentionally absent.
- Commit, push, or publish only when the user asks for it.

## Read For The Change

- **Source ownership:** Before changing code, read [CONTRIBUTING: Find the owner before changing behavior](CONTRIBUTING.md#find-the-owner-before-changing-behavior) and [Contracts to preserve](CONTRIBUTING.md#contracts-to-preserve). Use the existing owner and official Mojang names.
- **Data flow:** Before cross-module changes or changes to chat state, request/save lifetime, inventory, networking, or version adapters, read the relevant sections of [ARCHITECTURE.md](ARCHITECTURE.md).
- **Build and tools:** Before building, changing the Minecraft target or Java toolchain, running datagen, or using `build.sh`, read [INSTALL.md](INSTALL.md). Read current versions and toolchain settings from `gradle.properties` and `build.gradle`.
- **Setup and player behavior:** Before changing setup, provider settings, player commands, or their guidance, read the relevant sections of [README.md](README.md#ai-provider-setup). Read current provider models and URLs from `ConfigurationPresets`.
- **Verification:** Before validating a change, read [CONTRIBUTING: Tests](CONTRIBUTING.md#tests) and select suites from [ARCHITECTURE: Verification map](ARCHITECTURE.md#12-verification-map). Keep offline and live-provider validation separate.
- **Documentation and releases:** When behavior, build requirements, contributor workflow, or public contracts change, update the corresponding docs as specified in [CONTRIBUTING: Documentation and releases](CONTRIBUTING.md#documentation-and-releases). Read that section before claiming a jar or updater release is ready; it requires a SemVer version bump and dated changelog notes for user-visible releases.
- **Licensing:** Before adding Java files or changing license headers, read [CONTRIBUTING: Licensing and reports](CONTRIBUTING.md#licensing-and-reports) for the project SPDX header. Preserve existing headers, including longer asset/trademark notices.

## LLM And JSON Contracts

- `ChatGPTRequest` owns shared request orchestration. OpenAI-compatible chat completions and native Gemini payload/envelope adapters retain their separate wire formats.
- Gameplay and setup callbacks consume their own immutable `ChatGPTRequest.RequestResult`. Legacy static `last*` fields are compatibility diagnostics, not request ownership.
- Preserve schema-backed chat and character generation and the `ChatGPTRequest.StructuredOutputMode` modes `NONE`, `CHAT`, and `CHARACTER`.
- Structured output needs enough `max_tokens` / `maxOutputTokens`; lower floors only with tests proving chat and character JSON are not truncated.
- Redact all configured keys before logging or reporting errors, including decoded provider JSON. Request results and legacy diagnostics must not expose raw keys.
- Keep input snapshots, quota preflight, candidate traversal, and retry policy in the shared runner when adding a provider adapter.

## Provider, Quota, And Credentials

- `ApiUsageLimiter` must preflight Gemini usage before every HTTP attempt, including retries and native requests. Keep `429` handling as a fallback even when local preflight limiting exists.
- Keep `creaturechat_usage.json`, `creaturechat.json`, and `chatdata.json` ignored by Git: they contain runtime quota state, credentials, and world conversations.
- The setup screen must not echo stored API keys back to clients. `ConfigurationScreenData` is the sanitized DTO for that boundary.

## Gameplay And State Rules

- Use `EntityChatData` and `PlayerData` for persisted per-entity/per-player chat state.
- Admit character/chat requests through the current `ChatSession` before changing history or automatic-message state. Apply the whole callback on the originating server executor, after checking current session, state, player, entity, and world identity.
- Create mutable chat snapshots on the server thread. Stop request/save scheduling before the final save; preserve the last good file if serialization fails.
- Use `SocialEventRecorder` for player social events instead of manually changing summaries at event sites.
- Automatic reactions must be rate-limited. Check existing `ChatDataManager`, `AutoMessageBucket`, damage cooldown, ambient response, and Gemini usage limiter patterns before adding a new automatic LLM path.
- Dynamic behavior goes through `EntityBehaviorManager` and existing goal classes. Check the manager's policy before changing goal selectors.
- Inventory transfer must honor slot permission for both occupied-stack merges and empty destinations. Hand container slots must mirror equipment after in-place mutations as well as `set`/`onTake`.
- Mixins need early guards, checked casts, and surgical edits. Make broad mixin changes only when the target method and version behavior are clear.
- Server/world/entity mutations stay on the server thread unless the Minecraft/Fabric API explicitly allows otherwise.

## Version-Specific Overrides

When Minecraft API differences require source changes, read [CONTRIBUTING: Version adapters and resources](CONTRIBUTING.md#version-adapters-and-resources) and [ARCHITECTURE: Build and version map](ARCHITECTURE.md#11-build-and-version-map).

- Prefer a small helper overridden under `src/vs/` over copying a large class for a narrow API difference.
- Overrides are cumulative whole-class replacements. Preserve shared `LivingEntityChatHooks` / `MobInteractionHooks` delegation and UUID-based player state when adapting Minecraft signatures.
- Verify folder version naming and the build's printed override selection against the version-selection block in `build.gradle`.
