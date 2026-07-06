# CreatureChat™

## Chat with any mob in Minecraft! All creatures can talk & react using AI!

## Fork Notice

This repository contains the MobChat fork of CreatureChat: <https://github.com/Le-Who/MobChat>. It was originally based on CreatureChat, but this codebase has diverged and should not be treated as the public upstream release. Build, install, and support this fork from the MobChat repository.

### Features
- **AI-Driven Chats:** Using ChatGPT or open-source AI models, each conversation is unique and engaging!
- **Behaviors:** Creatures can make decisions on their own and **Follow, Flee, Attack, Protect**, and more!
- **Reactions:** Creatures automatically react to being damaged, shown items, or receiving or loosing items.
- **Friendship:** Track your relationships from friends to foes.
- **Multi-Player:** Share the experience; conversations sync across server & players.
- **Memory:** Creatures remember your past interactions, making each chat more personal.
- **Inventory:** Every mob has an inventory with random loot. Give items to your friends or take items to create enemies.
- **Advancements:** Earn unique CreatureChat milestones as your friendships progress.

### Create meaningful conversations and enduring friendships? A betrayal perhaps?

![CreatureChat screenshot](src/main/resources/assets/creaturechat/screenshots/video-thumbnail.jpeg)

## Installation Instructions
### Fabric (Recommended)

1. **Install Fabric Loader & API:** Follow the instructions [here](https://fabricmc.net/use/).
2. **Install CreatureChat Mod:** Build this fork locally, then copy `creaturechat-*.jar` and `fabric-api-*.jar` into your `.minecraft/mods` folder.
3. **Launch Minecraft** with the Fabric profile.
4. **Configure AI:** A LLM (large language model) is required for generating text (AI options **listed below**)

### Forge (with Sinytra Connector)
*NOTE: Sintra Connector only supports Minecraft 1.20.1.*

1. **Install Forge:** Download [Forge Installer](https://files.minecraftforge.net/), run it, select "Install client".
2. **Install Forgified Fabric API:** Download [Forgified Fabric API](https://curseforge.com/minecraft/mc-mods/forgified-fabric-api) and copy the `*.jar` into your `.minecraft/mods` folder.
3. **Install Sinytra Connector:** Download [Sinytra Connector](https://www.curseforge.com/minecraft/mc-mods/sinytra-connector) and copy the `*.jar` into your `.minecraft/mods` folder.
4. **Install CreatureChat Mod:** Build this fork locally, then copy `creaturechat-*.jar` into your `.minecraft/mods` folder.
6. **Launch Minecraft** with the Forge profile.
7. **Configure AI:** A LLM (large language model) is required for generating text (AI options **listed below**)

## AI Options
CreatureChat™ **requires** an AI / LLM (large language model) to generate text (characters and chat). There are many different
options for connecting an LLM. 

1. **Free & Local**: Use open-source and free-to-use LLMs without any API fees. [**Difficulty: Hard**]
2. **Bring Your Own Key**: Use your own API key from providers like OpenAI or Groq. [**Difficulty: Medium**]

### 1. Free & Local
CreatureChat™ fully supports **free and open-source** LLMs. To get started:

- An HTTP endpoint compatible with the OpenAI Chat Completion JSON syntax is required. We highly recommend using:
  - [Ollama](https://ollama.com/) & [LiteLLM](https://litellm.vercel.app/) as your HTTP proxy.
  - **LiteLLM Features:**
    - Supports over **100+ LLMs** (e.g., Anthropic, VertexAI, HuggingFace, Google Gemini, and Ollama).
    - Proxies them through a local HTTP endpoint compatible with CreatureChat.
    - **Note:** Running a local LLM on your computer requires a powerful GPU.
  - Open the OP-only setup screen in-game with `/creaturechat setup`, select `Ollama` or `LiteLLM`, then set the endpoint, exact model id, and timeout.
  - Check your local server logs if the endpoint cannot be reached.

### 2. Bring Your Own Key
For those already using a third-party API (e.g., OpenAI, Google AI Studio, OpenRouter, Groq):

- Integrate your own API key for seamless connectivity.
- Costs depend on the provider’s usage-based pricing model.
- By default, CreatureChat™ uses the OpenAI-compatible chat completions request format.
- Be aware that OpenAI’s developer API does not include free usage. Please review the [OpenAI pricing](https://openai.com/api/pricing/) for detailed information.
- Start the OP-only setup screen in-game:
  - `/creaturechat setup`
- Select a provider preset, enter one or more API keys, enter one or more exact model ids, then use `Save` and `Test`.
- Example Google AI Studio models: `gemini-3.5-flash,gemini-3.5-pro`. You can enter any exact model ids because provider model names change over time.

### In-game Commands / Configuration
- **RECOMMENDED:** `/creaturechat setup`
  - Opens the OP-only setup screen for players. The screen saves values to the server world's `creaturechat.json` and never sends stored API keys back to the client.
- **FALLBACK:** `/creaturechat setup provider|key|model|url|timeout|show|test ...`
  - Text-command setup is still available for console use, scripts, or servers where opening the screen is not convenient.
- **OPTIONAL:** `/creaturechat url set "<url>"`
  - Sets the URL of the API used to make LLM requests. Defaults to `"https://api.openai.com/v1/chat/completions"`.
- **OPTIONAL:** `/creaturechat model set <model>`
  - Sets the model used for generating responses in chats. Comma-separated model lists enable fallback rotation.
- **OPTIONAL:** `/creaturechat timeout set <seconds>`
  - Sets the timeout (in seconds) for API HTTP requests. Defaults to `10` seconds.
- **OPTIONAL:** `/creaturechat whitelist <entityType | all | clear>` - Show chat bubbles
  - Shows chat bubbles for the specified entity type or all entities, or clears the whitelist.
- **OPTIONAL:** `/creaturechat blacklist <entityType | all | clear>` - Hide chat bubbles
  - Hides chat bubbles for the specified entity type or all entities, or clears the blacklist.
- **OPTIONAL:** `/story set "<story-text>"`
  - Sets a custom story (included in character creation and chat prompts).
- **OPTIONAL:** `/story display | clear`
  - Display or clear the current story.

#### Configuration Scope (default | server):
- **OPTIONAL:** Specify the configuration scope at the end of each command to determine where settings should be applied:
  - **Default Configuration (`--config default`):** Applies the configuration universally, unless overridden by a server-specific configuration.
  - **Server-Specific Configuration (`--config server`):** Applies the configuration only to the server where the command is executed.
  - If the `--config` option is not specified, the `default` configuration scope is assumed.

### Screenshots
![Panda Following the Player](src/main/resources/assets/creaturechat/screenshots/panda-follow.jpeg)
![Piglins Reacting to Player](src/main/resources/assets/creaturechat/screenshots/piglin-reactions.jpeg)

### Authors

- MobChat fork maintainers
- Original CreatureChat authors are retained in source headers and license metadata where applicable.

### Contact & Resources

- [Build Instructions](INSTALL.md)
- [Player & Entity Icon Tutorial](ICONS.md)
- Source code is maintained at <https://github.com/Le-Who/MobChat>.

### License

- [![REUSE Status](https://img.shields.io/badge/REUSE-compliant-brightgreen)](https://reuse.software)
- **Source code:** [GNU GPL v3](LICENSE.md)
- **Non-code assets:** [CC-BY-NC-SA-4.0](LICENSE-ASSETS.md)

### Legal Notices

- Review [Terms](TERMS.md) and [Privacy](PRIVACY.md) before operating any public server or remote AI service with this fork.
- CreatureChat™ is an independent project and is **not** endorsed by Mojang AB, Microsoft Corp., or OpenAI. *Minecraft®* is a trademark of Mojang AB. *ChatGPT®* is a trademark of OpenAI OpCo, LLC. All trademarks appear here for identification only.
- *CreatureChat™* is a trademark of owlmaddie LLC (registration pending). Factual nominative references such as “Fork of CreatureChat” that do **not** imply endorsement are allowed; all other uses of the name or logo require prior permission.
