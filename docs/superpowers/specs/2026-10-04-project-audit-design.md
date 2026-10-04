# MobChat architecture and stability audit

The goal is a verified map of the current MobChat fork, documentation that matches its actual behavior, and focused repairs for defects established by source inspection and regression tests. The existing Fabric architecture, `creaturechat` namespace, structured response contracts, saved data formats, and provider presets remain the starting point.

## Evidence and scope

Research covers common and client Java sources, cumulative version overrides, prompt and data resources, JUnit tests, Gradle and auxiliary scripts, and existing documentation. Binary assets are inventoried by purpose. Local sources and configuration declarations are authoritative; runtime files containing credentials are excluded from inspection.

The verified build target is the value in `gradle.properties`. The presence of newer version overrides or legacy matrix scripts does not establish tested support for those targets. Offline HTTP fixtures must exercise provider behavior without real API credentials or external calls.

## Repairs

1. Share request orchestration between compatible chat completions and native Gemini: request input snapshots, local quota reservations, bounded key/model candidate rotation, transient connection recovery, sanitized diagnostics, and resource cleanup. Keep native and compatible payload/schema adapters distinct.
2. Apply chat and character responses on the originating server executor. Check that the server session, entity state, player, and world are still current before producing effects. Autosave must traverse mutable state on the server thread and preserve the last complete file when a write fails.
3. Keep hand inventory stacks and mob equipment synchronized during partial Shift+Click and destination stack merges. Restore social event recording, UUID lookup, damage cooldown, and nearby player chat in version overrides that lost those calls.

Each repair belongs in the module that already owns the operation. A small helper is justified when it removes repeated policy from version adapters or exposes real behavior to deterministic tests. Broad rewrites of rendering, goals, or mixin targets are outside this repair scope.

## Documentation

- `ARCHITECTURE.md`: mental map, owning classes, main flows, persistence and threading, LLM and network contracts, updater, resource/version adapters, test map, and clearly identified remaining limitations.
- `README.md`: player/admin gameplay, setup, provider routing, command examples, update download/apply distinction, and navigation.
- `INSTALL.md`: actual build toolchain, runtime versus compiler requirements, artifacts, datagen effects, and validation limits.
- `CONTRIBUTING.md`: local contributor workflow, test selection, offline versus live tests, version overrides, resources, and release checklist.
- `ICONS.md`, `PRIVACY.md`, `AGENTS.md`: correct factual implementation references while preserving licensing terms and durable agent rules.
- `CHANGELOG.md` and `gradle.properties`: a dated patch release for the user-visible repairs, with no publication implied.

## Constraints and acceptance

- Java source/API release remains 17; use official Mojang mappings.
- Preserve `NONE`, `CHAT`, and `CHARACTER` structured output modes and their token floors.
- Provider errors and client configuration packets must not expose raw keys.
- Automatic reactions remain limited before creating HTTP requests.
- No commits, pushes, releases, live-provider tests, or installation into a running Minecraft instance.
- Finish with targeted regression tests, a fresh offline suite, `gradlew.bat build`, hash/artifact inspection, documentation link checks, and `git diff --check`.
- State the limits of headless validation: a compiled jar and JUnit tests do not prove visual gameplay or all Minecraft versions.
