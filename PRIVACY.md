# Privacy Notes for the MobChat CreatureChat Fork

This repository does not operate the public CreatureChat API, token shop, Discord, or any upstream hosted CreatureChat service.

The mod sends generation requests to the AI endpoint configured by the server administrator in `creaturechat.json` or through the setup commands/screen. The built-in default is OpenAI-compatible chat completions at `https://api.openai.com/v1/chat/completions`; the AI Studio preset uses native Gemini `generateContent`. A local Ollama/LiteLLM endpoint may keep requests on the operator's own deployment, depending on that deployment's configuration.

Generation requests can include player display names and language, shared conversation history, the mob's character sheet, memories, relationship and recent social context, selected Minecraft world/player context, and the administrator's story prompt. The configured credential is sent to the selected endpoint for authentication. Automatic reactions, proximity chat and mob-to-mob conversations can also generate requests according to the server's settings and throttles.

The server world's `chatdata.json` stores character sheets, shared histories, memories and per-player relationships. Configuration files store provider credentials. `creaturechat_usage.json` beside the effective configuration stores hashed quota buckets and daily counts. Review access to these files and backups alongside access to the server.

The auto-update checker contacts GitHub Releases for `Le-Who/MobChat` to read release metadata and download jar assets after server command or client consent. CreatureChat conversation text, API keys, and world chat data are not sent to GitHub by the updater.

Per-player chat display preferences are stored locally in the server world's `creaturechat_player_prefs.json` and are not sent to AI providers or GitHub. The setup screen receives a sanitized DTO instead of the stored full API keys. Provider error diagnostics redact configured keys before reporting or logging them.

Each NPC has one shared history and memory set. Friendship is per player. Turning overhearing off filters current NPC replies in bubbles and normal chat; opening the NPC chat screen still requests that NPC's recent shared history. This setting is not a private-conversation boundary.

Operational privacy depends on the configured AI provider and server deployment. The provider's own privacy and retention rules apply. The mod can log conversation/context diagnostics and response previews; review server and local-endpoint logs as well as persisted world files.

Server operators should avoid sending private or sensitive information through mob conversations, prompts, story text, API keys, or logs.
