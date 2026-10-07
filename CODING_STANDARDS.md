# MobChat Coding Standards

Apply every branch that matches the change. [ARCHITECTURE.md](ARCHITECTURE.md) owns maps and data flow; [CONTRIBUTING.md](CONTRIBUTING.md) owns verification, resources, documentation, and release workflow.

## Source Ownership

- Extend the existing owner and patterns; a new abstraction must reduce complexity or follow an established local pattern. A narrow helper can centralize repeated policy across version adapters or expose meaningful behavior for tests.
- Search and code with official Mojang names, such as `ServerPlayer`, `Mob`, `LivingEntity`, and `MinecraftServer`.

Find the module in [ARCHITECTURE: Source ownership](ARCHITECTURE.md#2-source-ownership).

## Requests, JSON, And Quotas

- Keep shared orchestration in `ChatGPTRequest`: immutable input snapshots, quota preflight, candidate traversal, retry, and error policy. Keep native Gemini payload/envelope and OpenAI-compatible chat-completions wire formats in their separate adapters.
- Gameplay and setup callbacks consume their own immutable `ChatGPTRequest.RequestResult`; legacy static `last*` fields are compatibility diagnostics.
- Preserve schema-backed chat and character generation, the strict contracts consumed by `ChatGPTResponse`, `MessageParser`, and `CharacterSheetNormalizer`, and `StructuredOutputMode.NONE`, `CHAT`, and `CHARACTER`. Add parser/request regressions before changing schema or response-salvage behavior.
- Lower `max_tokens` / `maxOutputTokens` floors only with tests proving complete chat and character JSON are not truncated.
- Preflight Gemini usage through `ApiUsageLimiter` before every HTTP attempt, including retries and native requests. Retain provider `429` handling alongside local limits.

Read current models and URLs from `ConfigurationPresets`. [ARCHITECTURE: Provider contracts and request policy](ARCHITECTURE.md#5-provider-contracts-and-request-policy) describes transport selection and quota scope.

## Credentials And Client Boundaries

- Redact all configured API keys before logging or reporting errors, including decoded provider JSON. Request results and legacy diagnostics must also be free of raw keys.
- Keep stored API keys server-side; use sanitized `ConfigurationScreenData` for setup/client packets.
- Keep `creaturechat.json` (credentials), `chatdata.json` (world conversations), and `creaturechat_usage.json` (runtime quota state) ignored by Git.
- Keep current-reply display filtering separate from the shared entity conversation-history contract.

See [ARCHITECTURE: Client interface and protocol](ARCHITECTURE.md#8-client-interface-and-protocol) and [Configuration, files and resource generation](ARCHITECTURE.md#9-configuration-files-and-resource-generation).

## Server State And Persistence

- Persist chat state through `EntityChatData` and `PlayerData`. Use UUID-scoped player lookup and preserve older-save migration.
- Keep server/world/entity mutations and mutable-state traversal on the server thread unless the Minecraft/Fabric API explicitly permits otherwise. A concurrent outer map does not make histories, memories, inventories, or player records thread-safe.
- Admit character/chat requests through the current `ChatSession` before changing history or automatic-message state. Apply the whole result callback on the originating server executor after checking current session, state, player, entity, and world identity.
- Create mutable chat snapshots on the server thread. Stop request/save scheduling before the final save; preserve the last good file if snapshot serialization fails.

See [ARCHITECTURE: Persisted domain model](ARCHITECTURE.md#3-persisted-domain-model) and [Response state and cancellation](ARCHITECTURE.md#response-state-and-cancellation).

## Social Events, Automatic Reactions, And Goals

- Record player social events through `SocialEventRecorder`.
- Before adding an automatic LLM path, apply existing `ChatDataManager`, player/entity `AutoMessageBucket`, damage/ambient cooldown, and Gemini preflight patterns. Automatic reactions require rate limits.
- Route dynamic behavior and goal-selector changes through `EntityBehaviorManager` and existing goal classes.
- Keep persisted chat/player/home metadata distinct from live goals; saving metadata does not recreate a goal after restart.

See [ARCHITECTURE: Events, rate limits and actions](ARCHITECTURE.md#6-events-rate-limits-and-actions).

## Inventory Transfers

- Honor slot permission for occupied-stack merges and empty destinations.
- Mirror hand container slots to equipment after in-place mutations, partial extraction, occupied-hand merging, `set`, and `onTake`.

See [ARCHITECTURE: Inventories and Minecraft hooks](ARCHITECTURE.md#7-inventories-and-minecraft-hooks).

## Mixins And Version Adapters

- Keep mixin guards early, casts checked, and changes surgical. Establish the target method and version behavior before broad changes.
- For narrow Minecraft API differences, override a small helper under `src/vs/` instead of copying a large class.
- Overrides are cumulative whole-class replacements: inspect the base class and every later applicable override of the same path. Keep common policy in shared `LivingEntityChatHooks` / `MobInteractionHooks`; preserve their delegation and UUID-based player state. Keep target signatures, serialization, and platform API calls in adapters.
- Verify folder version naming and the build's printed override selection against the version-selection block in `build.gradle`.

See [ARCHITECTURE: Build and version map](ARCHITECTURE.md#11-build-and-version-map). Follow [CONTRIBUTING: Version adapters and resources](CONTRIBUTING.md#version-adapters-and-resources) before packaging another target.

## Java Licensing

Preserve existing license headers, including longer asset/trademark notices. Start new Java files with:

```java
// SPDX-FileCopyrightText: 2026 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
```

See [CONTRIBUTING: Licensing and reports](CONTRIBUTING.md#licensing-and-reports) for complete source, asset, and trademark terms.
