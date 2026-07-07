# CreatureChat(TM) - MobChat Fork

Chat with any mob in Minecraft. Creatures can speak, remember players, react to events, and execute AI-driven behaviors through a configured LLM provider.

## Fork Notice

This repository contains the MobChat fork of CreatureChat: <https://github.com/Le-Who/MobChat>. It started from CreatureChat, but this codebase has diverged and should be treated as its own project. Build, install, configure, and support this fork from the MobChat repository and local server configuration, not from upstream CreatureChat services.

## Features

- **AI-driven mob conversations:** Each mob can generate contextual chat through an OpenAI-compatible LLM endpoint.
- **Structured AI output:** Chat and character generation use strict JSON schemas to reduce malformed replies and keep behavior parsing predictable.
- **Mob behavior actions:** Creatures can follow, flee, attack, protect, wait, return home, guard home, and react through the behavior system.
- **Character sheets:** New mobs can receive generated names, personalities, classes, skills, likes, dislikes, alignment, background, and greeting text.
- **Memory and relationships:** Mobs remember player interactions, social events, friendship changes, harmful actions, and recent context.
- **Automatic reactions:** Mobs can react to damage, item showing/giving/taking, arrivals, proximity chat, and mob-to-mob chat.
- **Cost controls:** Automatic responses have cooldowns and Gemini usage is preflight-limited before HTTP requests to avoid avoidable rate-limit freezes.
- **Inventories and loot:** Every mob has an inventory backed by generated per-biome loot tables.
- **Multiplayer sync:** Chat bubbles, messages, inventory UI, and entity chat state are synchronized for server players.
- **Advancements:** Players can unlock CreatureChat milestones as relationships develop.

![CreatureChat screenshot](src/main/resources/assets/creaturechat/screenshots/video-thumbnail.jpeg)

## Supported Runtime

- Minecraft target in this branch: `1.20.1`
- Loader: Fabric Loader with Fabric API
- Java source/target compatibility: Java 17
- Gradle toolchain configured locally in `gradle.properties`
- Version-specific source overrides live under `src/vs/` and are applied by the Gradle build when a newer Minecraft target needs patched source files.

## Build

From `CreatureChat/`:

```powershell
.\gradlew.bat build
```

The remapped mod jar is written to:

```text
CreatureChat/build/libs/creaturechat-3.0.0+1.20.1.jar
```

For targeted checks:

```powershell
.\gradlew.bat test
.\gradlew.bat test --tests com.lewho.tests.ChatGPTRequestUsageLimitTests
```

## Installation

### Fabric

1. Install Fabric Loader and Fabric API for the target Minecraft version.
2. Build this fork locally.
3. Copy `creaturechat-*.jar` and the matching `fabric-api-*.jar` into `.minecraft/mods`.
4. Launch Minecraft with the Fabric profile.
5. Configure an LLM provider in-game with `/creaturechat setup`.

### Forge With Sinytra Connector

Sinytra Connector support is only expected for Minecraft `1.20.1`.

1. Install Forge.
2. Install Forgified Fabric API.
3. Install Sinytra Connector.
4. Build this fork locally and copy `creaturechat-*.jar` into `.minecraft/mods`.
5. Launch Minecraft with the Forge profile.
6. Configure an LLM provider with `/creaturechat setup`.

## AI Provider Setup

CreatureChat requires an LLM for generated character sheets and chat replies. The mod sends OpenAI-compatible chat completions requests, so providers should expose an OpenAI-compatible endpoint.

Recommended setup path:

```text
/creaturechat setup
```

The setup screen is OP-only. It saves values to the server world's `creaturechat.json` and does not send stored API keys back to the client.

Provider presets currently include:

- `openai`
- `ai-studio` for Google AI Studio through the Gemini OpenAI-compatible endpoint
- `openrouter`
- `groq`
- `ollama`
- `litellm`

Console/script fallback:

```text
/creaturechat setup provider ai-studio
/creaturechat setup key <key1,key2>
/creaturechat setup model gemini-3.1-flash-lite
/creaturechat setup outputtokens 1024
/creaturechat setup test
/creaturechat setup show
```

You can enter multiple comma-separated API keys or models. The request layer rotates candidates when local quota checks or provider errors make the active candidate unavailable.

## Google AI Studio / Gemini Notes

The `ai-studio` preset uses:

```text
https://generativelanguage.googleapis.com/v1beta/openai/chat/completions
```

Default model:

```text
gemini-3.1-flash-lite
```

This fork preflights Gemini usage before sending HTTP requests:

- Default minute limit: `14 RPM`
- Default daily limit: `450 RPD`
- Default scope: `per_key`
- Daily usage state file: `creaturechat_usage.json`

Commands:

```text
/creaturechat setup geminirpm 14
/creaturechat setup geminidaily 450
/creaturechat setup geminiscope per_key
```

Use `geminiscope shared` if several configured keys belong to the same Google project and should share one local quota bucket. The usage file stores hashed key buckets and daily counts; it is runtime state and is ignored by Git.

## Output Tokens And Thinking

`maxOutputTokens` limits the generated response budget, not the input context. The default is `1024`. Structured JSON modes raise the effective floor when needed so character/chat JSON is less likely to be truncated.

Gemini thinking level is configurable through the setup screen. The AI Studio preset defaults to `minimal`.

## Gameplay Tuning

Useful runtime knobs:

```text
/creaturechat setup damagecooldown 25
/creaturechat outputtokens set <tokens>
/creaturechat timeout set <seconds>
/creaturechat model set <model1,model2>
/creaturechat url set "<url>"
```

Damage-triggered AI replies have their own cooldown. Suppressed hits are summarized into the next allowed damage reaction so long fights do not generate a request for every hit.

## Entity Visibility

```text
/creaturechat whitelist <entityType|all|clear>
/creaturechat blacklist <entityType|all|clear>
```

Whitelist and blacklist commands control which entity types show CreatureChat bubbles.

## Story Prompt

```text
/story set "<story-text>"
/story display
/story clear
```

The story text is included in character creation and chat prompts.

## Configuration Scope

Most setup commands accept an optional config scope:

- `--config server`: save to the current server world's `creaturechat.json`
- `--config default`: save to the default root config

If omitted, legacy commands use the default scope unless the `/creaturechat setup ...` subcommand explicitly saves to the server config.

## Development References

- [Build Instructions](INSTALL.md)
- [Contribution Guide](CONTRIBUTING.md)
- [Player & Entity Icon Tutorial](ICONS.md)
- [Privacy](PRIVACY.md)
- [Terms](TERMS.md)

## Screenshots

![Panda Following the Player](src/main/resources/assets/creaturechat/screenshots/panda-follow.jpeg)
![Piglins Reacting to Player](src/main/resources/assets/creaturechat/screenshots/piglin-reactions.jpeg)

## Authors

- MobChat fork maintainers
- Original CreatureChat authors are retained in source headers and license metadata where applicable.

## License

- [![REUSE Status](https://img.shields.io/badge/REUSE-compliant-brightgreen)](https://reuse.software)
- Source code: [GNU GPL v3](LICENSE.md)
- Non-code assets: [CC-BY-NC-SA-4.0](LICENSE-ASSETS.md)

## Legal Notices

- Review [Terms](TERMS.md) and [Privacy](PRIVACY.md) before operating any public server or remote AI service with this fork.
- CreatureChat(TM) is an independent project and is not endorsed by Mojang AB, Microsoft Corp., OpenAI, Google, or any LLM provider.
- Minecraft(R) is a trademark of Mojang AB. ChatGPT(R) is a trademark of OpenAI OpCo, LLC. All trademarks appear here for identification only.
- CreatureChat(TM) is a trademark of lewho LLC (registration pending). Factual nominative references such as "Fork of CreatureChat" that do not imply endorsement are allowed; all other uses of the name or logo require prior permission.
