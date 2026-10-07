# Contributing to MobChat

Contributions target the [MobChat repository](https://github.com/Le-Who/MobChat), the local fork of CreatureChat. Start with [INSTALL.md](INSTALL.md) for the build environment and [ARCHITECTURE.md](ARCHITECTURE.md) for module ownership and data flow. Local source and configuration describe this fork's behavior.

## Find the owner before changing behavior

Before changing code, configuration, or resources, find the module in [ARCHITECTURE: Source ownership](ARCHITECTURE.md#2-source-ownership) and apply [CODING_STANDARDS: Source ownership](CODING_STANDARDS.md#source-ownership).

Read every matching standards branch: [Requests, JSON, and quotas](CODING_STANDARDS.md#requests-json-and-quotas), [Credentials and client boundaries](CODING_STANDARDS.md#credentials-and-client-boundaries), [Server state and persistence](CODING_STANDARDS.md#server-state-and-persistence), [Social events, automatic reactions, and goals](CODING_STANDARDS.md#social-events-automatic-reactions-and-goals), or [Inventory transfers](CODING_STANDARDS.md#inventory-transfers).

## Tests

Run the relevant targeted suite, then the ordinary build:

```powershell
.\gradlew.bat test --tests com.lewho.tests.ChatGPTRequestStructuredOutputTests
.\gradlew.bat test --tests com.lewho.tests.ChatGPTRequestUsageLimitTests
.\gradlew.bat test --tests com.lewho.tests.GeminiNativePolicyTests
.\gradlew.bat test --tests com.lewho.tests.DamageReactionRateLimitTests
.\gradlew.bat test --tests com.lewho.tests.ChatStateLifecycleTests --tests com.lewho.tests.ChatRequestDispatchTests --tests com.lewho.tests.ChatDataFileTests
.\gradlew.bat test --tests com.lewho.tests.MobHandSlotTests
.\gradlew.bat test --tests com.lewho.inventory.MobInventoryAccessTests
.\gradlew.bat build
```

Select commands appropriate to the change; the test map in [ARCHITECTURE.md](ARCHITECTURE.md) points to other focused suites. A fresh complete test run is `.\gradlew.bat test --rerun-tasks --console=plain`. For documentation-only changes, run `git diff --check` and check local links.

Ordinary provider tests use localhost HTTP fixtures and temporary files, with synthetic credentials. Keep `API_KEY` unset for offline verification. `BehaviorTests` is an optional live-provider suite: it reads `API_KEY`, `API_URL`, and `API_MODEL`, incurs real requests when enabled, and writes `src/test/BehaviorOutputs.json` during cleanup. It is skipped without a key. Review fixture changes from intentional live runs; do not confuse skips with verified live-provider behavior.

Write behavioral regressions that catch the original defect. Real `ItemStack`/container fixtures, manually drained executors, temporary files, and controlled HTTP responses are preferable to assertions about source text or trivial getters. Headless tests do not establish rendering correctness or live Minecraft behavior; use `runClient`/`runServer` when those are the changed surfaces.

## Version adapters and resources

Before changing Minecraft signatures, mixins, or source overrides, read [CODING_STANDARDS: Mixins and version adapters](CODING_STANDARDS.md#mixins-and-version-adapters).

Check target-specific dependencies and metadata before packaging another Minecraft version. The current default-target build and static inspection of newer adapters are different forms of evidence; report each accurately.

Prompt templates live in `src/main/resources/data/creaturechat/prompts/`. Resource-manager loading permits data-pack overrides. `system-quest` is presently unused. Loot tables and advancements have checked-in data plus datagen providers. Translations use fallback text in code; new keys need a matching datagen update. `runDatagen` can change tracked locale files, as described in [INSTALL.md](INSTALL.md).

`custom_roles.json` is read from the current world root first, then the process working directory. Roles are keyed by the entity's displayed type name. The legacy `generate_roles.py` writes `data/creaturechat/custom_roles.json`; move its output into one of the runtime locations before using it. It is optional tooling, not part of the build.

## Documentation and releases

Update `README.md` for player/admin behavior, `INSTALL.md` for installation/build changes, and this guide for contributor workflow. Update the architecture map when ownership, data flow, state lifecycle, or public contracts change. Keep `AGENTS.md` for repository scope and document routing, `CODING_STANDARDS.md` for change constraints, and release history in `CHANGELOG.md`.

Add release-worthy user-visible notes under `## Unreleased` while developing; documentation-only cleanup may skip the changelog unless release notes are requested. Before declaring a user-visible jar or updater release ready for distribution, bump `mod_version` in `gradle.properties` according to SemVer and move its notes into a dated `## [version] - YYYY-MM-DD` section. Build the remapped distribution, verify metadata and SHA-512, and include each target's jar/hash pair in a GitHub release for the updater.

Use a descriptive branch for contributions. Keep changes focused, preserve unrelated work, and include a concrete problem/result and validation in a PR or patch. Commits, pushes, external publication, and merging remain explicit repository/user actions.

## Licensing and reports

Before adding Java files or changing license headers, read [CODING_STANDARDS: Java licensing](CODING_STANDARDS.md#java-licensing).

Source code is under [GPL-3.0-or-later](LICENSE.md); non-code assets have the separate [CC-BY-NC-SA-4.0 terms](LICENSE-ASSETS.md). Preserve attribution, ShareAlike, and non-commercial asset conditions. The original CreatureChat trademark and branding terms remain separate; use the licensing documents for their complete conditions.

Report bugs through the MobChat tracker or the current development chat. Include Minecraft/mod/loader versions, reproduction steps, expected and actual behavior, sanitized logs, and whether the problem occurs with the configured provider or a localhost fixture.
