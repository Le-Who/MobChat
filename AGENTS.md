# MobChat Agent Instructions

These instructions apply throughout this repository.

## Project Identity

- This is the [MobChat fork of CreatureChat](https://github.com/Le-Who/MobChat). Local source, local docs, and server configuration are authoritative.
- Keep support links, metadata, setup flows, and error guidance pointed at this repository and local/server configuration. Public upstream CreatureChat websites, Modrinth/CurseForge pages, Discord links, and upstream GitHub docs are authoritative only when the user explicitly asks for an upstream comparison.
- AI Villager development is paused. The former `ai_villager/` compiled artifact tree and `ai_villager_src/` decompiled source tree are intentionally absent.
- Commit, push, or publish only when the user asks for it.

## Coding Standards By Change

Before changing code, read [CONTRIBUTING: Find the owner before changing behavior](CONTRIBUTING.md#find-the-owner-before-changing-behavior) for ownership, existing patterns, and official Mojang names. Then read every applicable section of [CODING_STANDARDS.md](CODING_STANDARDS.md):

| Change touches | Read before editing |
| --- | --- |
| LLM requests, provider adapters, response parsing, structured output, token budgets, or quotas | [Request orchestration and structured output](CODING_STANDARDS.md#request-orchestration-and-structured-output) |
| Credentials, setup packets, provider diagnostics, error logging, or runtime file tracking | [Credentials and configuration boundaries](CODING_STANDARDS.md#credentials-and-configuration-boundaries) |
| Chat state, asynchronous callbacks, persistence, shutdown, or server/world/entity mutations | [Server state and request lifetime](CODING_STANDARDS.md#server-state-and-request-lifetime) |
| Social events, automatic LLM triggers, goals, or mob inventory transfers | [Gameplay policy and inventory](CODING_STANDARDS.md#gameplay-policy-and-inventory) |
| Mixins, Minecraft API signatures, or version-specific overrides | [Mixins and version adapters](CODING_STANDARDS.md#mixins-and-version-adapters) |

## Other References By Change

- **Data flow:** Before cross-module changes or changes to chat state, request/save lifetime, inventory, networking, or version adapters, read the relevant sections of [ARCHITECTURE.md](ARCHITECTURE.md).
- **Build and tools:** Before building, changing the Minecraft target or Java toolchain, running datagen, or using `build.sh`, read [INSTALL.md](INSTALL.md). Read current versions and toolchain settings from `gradle.properties` and `build.gradle`.
- **Setup and player behavior:** Before changing setup, provider settings, player commands, or their guidance, read the relevant sections of [README.md](README.md). Read current provider models and URLs from [ConfigurationPresets](src/main/java/com/lewho/commands/ConfigurationPresets.java).
- **Verification:** Before completing a source or documentation change, read [CONTRIBUTING: Tests](CONTRIBUTING.md#tests) for required checks and offline/live-provider boundaries. Select focused suites from [ARCHITECTURE: Verification map](ARCHITECTURE.md#12-verification-map).
- **Documentation and releases:** When behavior, build requirements, contributor workflow, or public contracts change, follow [CONTRIBUTING: Documentation and releases](CONTRIBUTING.md#documentation-and-releases). Read its version/changelog requirements before claiming a jar or updater release is ready.
- **Licensing:** Before adding Java files or changing license headers, read [CONTRIBUTING: Licensing and reports](CONTRIBUTING.md#licensing-and-reports) for the project SPDX header. Preserve existing headers, including longer asset/trademark notices.
