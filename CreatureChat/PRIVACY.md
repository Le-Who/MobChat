# Privacy Notes for the MobChat CreatureChat Fork

This repository does not operate the public CreatureChat API, token shop, Discord, or any upstream hosted CreatureChat service.

The mod sends chat-generation requests only to the AI endpoint configured by the server administrator in `creaturechat.json` or through `/creaturechat url set`. By default, the endpoint is OpenAI-compatible: `https://api.openai.com/v1/chat/completions`.

Operational privacy depends on the configured AI provider. If a server uses OpenAI, Groq, LiteLLM, Ollama, or another compatible endpoint, that provider's own privacy and retention rules apply. For local endpoints, review the local deployment and logs.

Server operators should avoid sending private or sensitive information through mob conversations, prompts, story text, API keys, or logs.
