# MobChat Agent Instructions

These instructions apply to the whole `E:\Projects\MobChat` workspace. The `CreatureChat/AGENTS.md` file is closer to the mod source and overrides or narrows these rules when work is inside `CreatureChat/`.

## Scope And Identity

- This repository is the MobChat fork of CreatureChat at `https://github.com/Le-Who/MobChat`.
- Treat local files and this repository as authoritative. Do not use public upstream CreatureChat pages, Modrinth/CurseForge listings, Discord links, or upstream GitHub metadata as the source of truth unless the user explicitly asks for an upstream comparison.
- AI Villager development is paused. The former `ai_villager/` compiled artifact tree and `ai_villager_src/` decompiled source tree are intentionally absent.
- The active codebase is the Fabric mod under `CreatureChat/`.

## Repository Map

- `CreatureChat/`: main Fabric mod project, Gradle wrapper, Java sources, resources, tests, docs, and build scripts.
- `CreatureChat/src/main/java/com/lewho/`: shared/server-side mod logic.
- `CreatureChat/src/client/java/com/lewho/`: client UI, rendering, packets, and client-only helpers.
- `CreatureChat/src/main/resources/data/creaturechat/prompts/`: system prompts for chat, character generation, and quests.
- `CreatureChat/src/main/resources/data/creaturechat/loot_tables/`: loot tables used by mob inventories.
- `CreatureChat/src/vs/`: version-specific Java source overrides.
- `generate_roles.py`: local tooling for role data generation.
- `.agents/` and `skills-lock.json`: local agent tooling, not product code.

## Working Rules

- Keep edits scoped to the task and to the module already responsible for the behavior.
- Prefer existing project patterns over new abstractions. Add a new abstraction only when it clearly reduces complexity or matches an established local pattern.
- Do not revert user changes or unrelated local work.
- Do not point docs, metadata, setup flows, or error text back to upstream CreatureChat services.
- Do not commit or push unless the user asks for it.
- For docs, keep README files human-facing and keep `AGENTS.md` focused on durable instructions for coding agents.

## Build And Validation

Run commands from `CreatureChat/` unless noted otherwise.

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

Useful targeted test form:

```powershell
.\gradlew.bat test --tests com.lewho.tests.ChatGPTRequestUsageLimitTests
```

The built jar is written to:

```text
CreatureChat/build/libs/creaturechat-3.0.0+1.20.1.jar
```

Before claiming a change is complete, run the smallest relevant test set and, for source changes, a full `.\gradlew.bat build` when practical.

## Runtime Configuration

- Main config file: `creaturechat.json` in the server world root or default server root.
- Runtime usage state: `creaturechat_usage.json`; it is ignored by Git and must not be committed.
- OP setup command: `/creaturechat setup`.
- AI provider setup supports OpenAI-compatible endpoints, provider presets, multiple API keys, multiple model IDs, output token budget, Gemini thinking level, and Gemini usage limits.

## Safety And Secrets

- Never print, commit, or persist raw API keys outside the intended config file.
- Setup screen data must stay sanitized: stored API keys are not sent back to the client.
- If adding logs around LLM requests, sanitize provider keys and keep response previews short.
- Treat provider quota/rate-limit handling as cost-control code. Preserve `ApiUsageLimiter` behavior unless deliberately changing quota semantics.

## Documentation Expectations

When changing user-visible behavior, update the closest relevant docs:

- `CreatureChat/README.md` for player/admin setup or feature behavior.
- `CreatureChat/INSTALL.md` for build/install flow.
- `CreatureChat/CONTRIBUTING.md` for contributor workflow.
- `CreatureChat/AGENTS.md` for recurring agent mistakes, routing guidance, test commands, or project-specific implementation rules.

Do not make `AGENTS.md` a changelog. Put release/user history in `CHANGELOG.md` when a user-visible mod behavior change needs release notes.
