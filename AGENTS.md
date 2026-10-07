# MobChat Agent Instructions

These instructions apply throughout this repository.

This is the [MobChat fork of CreatureChat](https://github.com/Le-Who/MobChat). Local source, docs, and server configuration are authoritative. Keep support links, metadata, setup flows, and error guidance pointed at this repository and local/server configuration. Use upstream CreatureChat websites, Modrinth/CurseForge pages, Discord links, or GitHub docs as authority only for an explicitly requested upstream comparison.

AI Villager development is paused; `ai_villager/` and `ai_villager_src/` are intentionally absent. Commit, push, or publish only when the user asks.

Read the references whose conditions match the change:

- **Code, resources, configuration, or Minecraft APIs:** Read applicable branches of [CODING_STANDARDS.md](CODING_STANDARDS.md) and find the owner in [ARCHITECTURE](ARCHITECTURE.md#2-source-ownership). Read its relevant sections for cross-module, state, request/save lifetime, inventory, networking, or version-adapter changes.
- **Build, Minecraft target, Java toolchain, datagen, or historical packaging:** Read [INSTALL.md](INSTALL.md).
- **Prompts, translations, loot, advancements, or custom roles:** Read [CONTRIBUTING: Version adapters and resources](CONTRIBUTING.md#version-adapters-and-resources).
- **Player/admin setup, commands, behavior, or their guidance:** Read the relevant sections of [README.md](README.md).
- **Completing any change:** Follow [CONTRIBUTING: Tests](CONTRIBUTING.md#tests).
- **Documentation, behavior, build requirements, contributor workflow, ownership/contracts, or release-ready claims:** Follow [CONTRIBUTING: Documentation and releases](CONTRIBUTING.md#documentation-and-releases).
