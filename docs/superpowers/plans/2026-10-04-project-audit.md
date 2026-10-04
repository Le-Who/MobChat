# MobChat architecture and stability implementation plan

> **For agentic workers:** Use the research reports and implement each repair with a failing behavioral regression, then review the final integrated changes. Steps use checkbox syntax for tracking. User authorization covers local changes; commits and external publication are excluded.

**Goal:** Document the complete current architecture and repair established stability defects.

**Architecture:** Keep the existing Fabric source sets and module ownership. Share transport policy across the two provider adapters, serialize game state changes through the server executor, and keep version mixins as narrow adapters over common policy.

**Tech Stack:** Java 17 source/API release, JDK 26 compiler toolchain, Fabric Loom 1.11.8, Gradle wrapper, JUnit 5, Gson.

**Spec:** [Architecture and stability audit design](../specs/2026-10-04-project-audit-design.md)

## Global constraints

- Preserve the `creaturechat` namespace and existing saved JSON contracts.
- Preserve `NONE`, `CHAT`, and `CHARACTER` output modes and schema/token floors.
- Keep keys server-side and redact all configured keys in diagnostics.
- Run provider tests against localhost fixtures only.
- Leave user changes intact. Make no commits or pushes.
- Verify the target in `gradle.properties`; label newer-target validation separately.

## Task 1: Establish coverage and documentation evidence

**Files:** all Java source sets and overrides, resources, build scripts, existing Markdown.

**Interfaces:** Produces a map of owners, data flow, invariants, verified defects, and validation gaps for subsequent tasks.

- [x] Inventory the tree and assign independent research areas with explicitly selected models and reasoning.
- [x] Read entrypoints, configuration, packets, and updater flow; reconcile reports with sources.
- [x] Run a fresh baseline with `./gradlew.bat test --rerun-tasks --console=plain` and record actual test counts and skips: 125 cases, 12 live-provider skips, zero failures/errors.

## Task 2: Shared native and compatible provider policy

**Files:** `ChatGPTRequest.java`, `GeminiNativeRequest.java`, their JUnit suites, and a narrow transport helper only if needed.

**Interfaces:** Existing `CompletableFuture<String>` fetch entrypoints remain callable. Native payload building and response parsing retain their own formats; orchestration owns quota, candidate selection, HTTP lifetime, and diagnostics.

- [x] Add localhost regressions: two native requests with an RPM limit of one rotate from the first synthetic key to the second; exhausted daily budget emits no further HTTP request; retryable errors traverse configured models; permanent errors avoid redundant retries; echoed keys are redacted.
- [x] Run targeted tests against the original implementation and confirm the expected failures: 20-case policy run, 17 expected failures. Additional envelope/logging edges are tracked with their own red/green evidence.
- [x] Route both request shapes through one orchestration path, snapshot mutable inputs, and preserve schema/token-floor behavior.
- [x] Re-run request, structured output, native Gemini, rotation, error, and quota tests: 66 request-related cases pass, including 26 native-policy regressions and reviewed success/error escaped-key cases.

## Task 3: Server response and save lifecycle

**Files:** `EntityChatData.java`, `ChatDataManager.java`, `ChatDataAutoSaver.java`, `ChatDataSaverScheduler.java`, lifecycle wiring in `ServerPackets.java` if required, new behavioral tests.

**Interfaces:** HTTP remains asynchronous; the response handoff owns current-session checks and schedules all effects on the originating server executor. Save snapshots are made where mutable state is owned.

- [x] Establish a deterministic test seam for executor handoff and state ownership without booting Minecraft or introducing test-only production cleanup.
- [x] Add regressions for deferred mutation, late response after state removal/replacement, player/world invalidation, and persistence of complete UTF-8 snapshots.
- [x] Confirm expected failure before each behavioral repair.
- [x] Apply character and chat callbacks through the same checked handoff; keep discarded callbacks from leaving the current entity permanently pending.
- [x] Schedule autosave on the server thread, make file replacement atomic where supported, and order shutdown so queued saves cannot overwrite the final save.
- [x] Run lifecycle/state/rate-limit regression tests: 17 lifecycle, 5 persistence and 6 admission/dispatch regressions pass. Admission-before-policy also covers actual automatic buckets and damage cooldown state.

## Task 4: Inventory synchronization and version policy

**Files:** `MobInventoryMenu.java`, relevant common mixins and `src/vs` adapters, focused helper/test classes as justified.

**Interfaces:** Both the source hand slot and destination hand slot must write the resulting count to equipment. Version adapters invoke the same social/damage policy as the base.

- [x] Add real-container tests for partial hand extraction, hand stack merge, and resulting equipment count; confirm the regression. Also confirmed full-disarm accounting and occupied locked-slot transfer failures.
- [x] Synchronize hand slot changes and complete the quick-move take callback; preserve locked-slot permissions in both transfer passes.
- [x] Restore UUID/social-event/damage-cooldown/proximity delegation in applicable overrides using shared policy where it avoids duplicated gameplay logic. Restored UUID/social advancement lookups too.
- [x] Run targeted inventory/social/damage/ambient tests and compile the default target: 23 focused cases pass, including real-container hand/access regressions. Default-target fresh suite also passes.

## Task 5: Documentation and local release artifacts

**Files:** `ARCHITECTURE.md`, `README.md`, `INSTALL.md`, `CONTRIBUTING.md`, `ICONS.md`, `PRIVACY.md`, `AGENTS.md`, `CHANGELOG.md`, `gradle.properties`.

**Interfaces:** README serves players/admins, ARCHITECTURE serves readers tracing behavior, CONTRIBUTING serves code changes, INSTALL serves building/installing. Documentation links resolve both in the repository and for root Markdown packaged into the jar.

- [x] Write the mental map with diagrams and source links, including all modules and external/state boundaries.
- [x] Correct native Gemini, story commands, update staging/apply, toolchain, icon paths, and contributor test instructions.
- [x] Describe runtime file locations, generated resource effects, live tests, legacy matrix/CI limitations, and the remaining follow-up work.
- [x] Bump a patch version to 3.1.1 and add dated release notes; final verification remains required.
- [x] Review the integrated code with an independent GPT-6.1 Sol agent at xhigh for cross-module/concurrency complexity. Fixed successful-content redaction and admission-order findings; final reinspection approved without remaining findings.
- [x] Run a fresh offline suite and `./gradlew.bat build --console=plain`; inspect metadata, stable copy, SHA-512, packaged documentation, local links, and `git diff --check`.
- [x] Deliver completed changes and validation limits. The architecture file opening was queued in this chat's Codex panel; documentation, source review, default build and artifact checks are complete. The goal is finalized after these acceptance gates.

## Validation record

- Fresh default-target offline test run: 195 cases in 40 suites; 0 failures, 0 errors, 12 live-provider skips. `API_KEY` explicitly unset for this process; all seven Gradle tasks executed.
- Newer-target check used `-Pminecraft_version=1.21.7 -Ploader_version=0.17.2 -Pfabric_version=0.128.2+1.21.7` with `compileJava compileClientJava`, leaving tracked target/metadata unchanged. Server compilation passed after narrow respawn/event adapters and Java 17 pattern fixes. Client compilation remains blocked by eight pre-existing GUI API errors documented in `ARCHITECTURE.md`; no complete newer jar/runtime support is claimed.
- Local documentation links were independently checked with no broken references. Original provider credentials/runtime files were not read. No live-provider calls, commits, pushes, installation or publication occurred.
- Default `build --console=plain` succeeded for 3.1.1+1.20.1 (15 actionable tasks, 9 executed). Verified Minecraft/Java/version metadata, class major version 61, identical stable/versioned jars, SHA-512 pair and source jar presence. All 12 root Markdown files packaged exactly match the checkout, and all new production helper classes are present.
- Final link check covered 14 Markdown documents and 101 local references with zero broken references. Final `git diff --check` passed. HEAD remains `68940cb`; all changes are local/uncommitted.
