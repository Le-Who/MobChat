# MobChat AI Agents Instructions

This repository contains two AI-integrated Minecraft Fabric mods: **CreatureChat** and **AI Villager**.
These instructions act as a guideline for AI agents to understand the project structure, architectural intent, and coding guidelines.

## 📁 Repository Overview

- **`CreatureChat/`**: Full source project for the CreatureChat mod. Contains the complete Gradle build system (`build.gradle`, `gradlew.bat`, `settings.gradle`) and all Java source files under `src/main/java/`. This is the primary working directory for this mod.
  - **Edit sources at**: `CreatureChat/src/main/java/com/owlmaddie/`
  - **Edit resources (prompts, etc.) at**: `CreatureChat/src/main/resources/data/creaturechat/`
  - **Build from**: `CreatureChat/` using `.\gradlew.bat build`
- **`ai_villager/`**: Advanced AI Villager mod (compiled `.class` files only). Source code not yet migrated locally.
- **`ai_villager_src/`**: Decompiled `.java` sources for AI Villager. Use for reading/searching only.

## 🧠 Architectural Concepts

1. **AI & JSON Parsing**: Both mods communicate with LLM APIs using strict JSON schemas. Never break the JSON format expected by `ActionParser` (AI Villager) or `ChatGPTRequest` (CreatureChat).
2. **Dynamic Goals (`goals/`)**: We inject AI-driven goals into vanilla `GoalSelector`. Use `EntityBehaviorManager` to safely swap goals.
3. **Mixins**: Extensive use of Fabric Mixins to hijack core Minecraft functions (e.g. `MixinMobEntity`). Be surgical when modifying Mixins to prevent crashing other mods.
4. **Custom Roles**: `CustomRoleHandler.java` manages `custom_roles.json` — a server-side config giving each mob species a physiological/behavioral description for the LLM.
5. **Context Gathering**: `ai_villager` relies heavily on reading surrounding blocks and entities. When adding new behaviors, always update `ContextGatherer`.

## 🛠️ Development & Coding Guidelines

- **Mappings**: The code uses Official Mojang mappings (`loom.officialMojangMappings()` in `build.gradle`). Use readable names (e.g. `ServerPlayer`, `MinecraftServer`) when searching.
- **Simplicity First**: When implementing a new feature, avoid adding complex new systems. Instead, extend the existing `VillagerAction` or `ChatGPTResponse` logic.
- **Version**: Currently targeting the latest upstream Minecraft version (see `CreatureChat/gradle.properties`). Version-specific overrides live in `CreatureChat/src/vs/`.
- **Java Toolchain**: Uses Java 17. Path configured in `CreatureChat/gradle.properties` via `org.gradle.java.installations.paths`.

## 🚀 Execution Commands

- **Search**: Use `grep_search` or PowerShell `Select-String` directly on `CreatureChat/src/main/java/` to find references.
- **Build**: Run `.\gradlew.bat build` from the `CreatureChat/` directory. The output JAR will be in `CreatureChat/build/libs/`.
- **Install**: Copy the built JAR to `%APPDATA%\.minecraft\mods\`.
- **Tests**: When diagnosing bugs, always check `LOGGER` output tagged `creaturechat` in the Minecraft console output.
