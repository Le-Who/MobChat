# MobChat Coding Standards

Read the sections selected by [AGENTS.md](AGENTS.md#coding-standards-by-change) before changing the corresponding behavior. These are change constraints; [ARCHITECTURE.md](ARCHITECTURE.md) describes the owners and data flow, and [CONTRIBUTING.md](CONTRIBUTING.md) owns verification, documentation, release, and licensing workflow.

## Request Orchestration And Structured Output

- Keep shared orchestration in `ChatGPTRequest`: input snapshots, quota preflight, candidate traversal, and retry policy. Provider adapters keep native Gemini payload/envelope and OpenAI-compatible chat-completions wire formats distinct.
- Gameplay and setup callbacks consume their own immutable `ChatGPTRequest.RequestResult`. Use legacy static `last*` fields only as compatibility diagnostics.
- Preserve schema-backed chat and character generation, the strict contracts consumed by `ChatGPTResponse`, `MessageParser`, and `CharacterSheetNormalizer`, and the `ChatGPTRequest.StructuredOutputMode` modes `NONE`, `CHAT`, and `CHARACTER`.
- Lower `max_tokens` / `maxOutputTokens` floors only with tests proving complete chat and character JSON are not truncated.
- Preflight Gemini usage through `ApiUsageLimiter` before every HTTP attempt, including retries and native requests. Retain provider `429` handling alongside local preflight limits.

See [ARCHITECTURE: Provider contracts and request policy](ARCHITECTURE.md#5-provider-contracts-and-request-policy) for transport selection and shared policy boundaries. Read current provider presets from `ConfigurationPresets` rather than caching models or URLs here.

## Credentials And Configuration Boundaries

- Redact all configured API keys before logging or reporting errors, including keys in decoded provider JSON. Request results and legacy diagnostics must also be free of raw keys.
- Keep stored API keys server-side. Use sanitized `ConfigurationScreenData` for the setup screen/client boundary.
- Keep `creaturechat.json` (credentials), `chatdata.json` (world conversations), and `creaturechat_usage.json` (runtime quota state) ignored by Git.

See [ARCHITECTURE: Client interface and protocol](ARCHITECTURE.md#8-client-interface-and-protocol) for the setup boundary and [Configuration, files and resource generation](ARCHITECTURE.md#9-configuration-files-and-resource-generation) for runtime file ownership.

## Server State And Request Lifetime

- Persist per-entity and per-player chat state through `EntityChatData` and `PlayerData`.
- Admit character/chat requests through the current `ChatSession` before changing history or automatic-message state.
- Apply the whole result callback on the originating server executor, after checking current session, state, player, entity, and world identity.
- Keep server/world/entity mutations on the server thread unless the Minecraft/Fabric API explicitly permits otherwise.
- Create mutable chat snapshots on the server thread. Stop request/save scheduling before the final save; preserve the last good file if snapshot serialization fails.

See [ARCHITECTURE: Persisted domain model](ARCHITECTURE.md#3-persisted-domain-model) and [Response state and cancellation](ARCHITECTURE.md#response-state-and-cancellation) for identity, admission side effects, invalidation, and save ordering.

## Gameplay Policy And Inventory

- Record player social events through `SocialEventRecorder` rather than editing summaries at individual event sites.
- Before adding an automatic LLM path, apply the existing `ChatDataManager`, `AutoMessageBucket`, damage-cooldown, ambient-response, and Gemini usage-limiter patterns. Automatic reactions require rate limits.
- Route dynamic behavior through `EntityBehaviorManager` and existing goal classes. Check the manager's policy before changing goal selectors.
- Honor slot permission for occupied-stack merges and empty destinations during inventory transfer.
- Mirror hand container slots to equipment after in-place mutations as well as `set` and `onTake`.

See [ARCHITECTURE: Events, rate limits and actions](ARCHITECTURE.md#6-events-rate-limits-and-actions) and [Inventories and Minecraft hooks](ARCHITECTURE.md#7-inventories-and-minecraft-hooks) for the existing policy boundaries.

## Mixins And Version Adapters

- Keep mixin guards early, casts checked, and changes surgical. Establish the target method and version behavior before broad mixin changes.
- For narrow Minecraft API differences, override a small helper under `src/vs/` instead of copying a large class.
- Treat overrides as cumulative whole-class replacements: inspect the base class and every later applicable override of the same path. Preserve shared `LivingEntityChatHooks` / `MobInteractionHooks` delegation and UUID-based player state when adapting signatures.
- Verify folder version naming and the build's printed override selection against the version-selection block in `build.gradle`.

Read [CONTRIBUTING: Version adapters and resources](CONTRIBUTING.md#version-adapters-and-resources) for cross-version review and [ARCHITECTURE: Build and version map](ARCHITECTURE.md#11-build-and-version-map) for the adapter map. Use [INSTALL: Version overrides and legacy tooling](INSTALL.md#version-overrides-and-legacy-tooling) before invoking historical packaging scripts.
