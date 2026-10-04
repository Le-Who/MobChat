# Building and installing MobChat

This guide describes the MobChat fork at [Le-Who/MobChat](https://github.com/Le-Who/MobChat). The build in this checkout is defined by [gradle.properties](gradle.properties) and [build.gradle](build.gradle). Use [README.md](README.md) for gameplay and provider setup.

## Build requirements

The compiler toolchain and the game runtime are separate requirements:

| Setting | Current checkout |
| --- | --- |
| Gradle wrapper | 8.14.3 |
| Fabric Loom plugin | 1.11.8, selected in `build.gradle` |
| Java compiler toolchain | JDK 26 |
| Compiled Java API and bytecode | Java 17 (`options.release = 17`) |
| Default Minecraft target | `minecraft_version` in `gradle.properties` |
| Mappings | Official Mojang mappings |

The checkout pins `org.gradle.java.installations.paths` to `E:/javaJDK/oracleJdk-26` and disables automatic toolchain discovery and downloading. On another machine, install a JDK 26 with `javac` and adjust that property to its location. Point `JAVA_HOME` and the Java executable used by the wrapper at a JDK capable of running the configured Gradle/Loom build. Installing only JDK 17 does not satisfy the current compiler toolchain declaration.

The current mod metadata declares Java `>=17` for the default Minecraft target. When changing Minecraft targets, check that target's own runtime requirements as well as the compiler settings; Java 17 bytecode alone does not establish a newer Minecraft runtime's requirements.

Git and a Java/Gradle IDE are useful for development. A separate Gradle installation is unnecessary because the wrapper is included.

## Build and verify

Run from the repository root on Windows:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

On a configured Unix checkout, use `./gradlew` with the same tasks. `build` includes the JUnit suite and access-widener validation. See [CONTRIBUTING.md](CONTRIBUTING.md) for targeted tests and the optional tests that use a real LLM.

The build produces:

```text
build/libs/creaturechat-<mod_version>+<minecraft_version>.jar
build/libs/creaturechat.jar
build/libs/creaturechat-<mod_version>+<minecraft_version>.jar.sha512
build/libs/creaturechat-<mod_version>+<minecraft_version>-sources.jar
```

The versioned jar is the remapped distribution. `creaturechat.jar` is an identical copy with a stable installation name. The `.sha512` file contains the SHA-512 digest of the versioned distribution. For GitHub updater releases, upload the versioned jar and its matching `.jar.sha512` asset together; the sources jar is for development.

## Install

1. Install Fabric Loader and Fabric API for the Minecraft target used to build the jar.
2. Stop Minecraft or the dedicated server before changing its `mods/` directory.
3. Copy `build/libs/creaturechat.jar` and the matching Fabric API jar into that installation's `mods/` directory. Keep a single MobChat jar there.
4. Start the Fabric client/server and configure the server's provider with `/creaturechat setup`.

Install the mod on both the server and participating clients for bubbles, custom screens, inventory menus, and packets. The technical mod ID, command prefix, assets, and jar name remain `creaturechat` in this fork.

The Forge/Sinytra Connector route described in [README.md](README.md) is expected for Minecraft 1.20.1. It is a compatibility route for the Fabric jar, not a separate native Forge implementation. Other targets need their own validation.

## Development launches

```powershell
.\gradlew.bat runClient
.\gradlew.bat runServer
```

These tasks create development runtime state under `run/`. Configure a development provider explicitly; a source checkout does not include working API credentials. A dedicated server also needs its normal Minecraft configuration and EULA setup.

## Data generation

```powershell
.\gradlew.bat runDatagen
```

The task regenerates `src/main/generated/`, which is ignored by Git and included as a resource directory. `runDatagen` deletes that directory before running, and `clean` deletes it as well. `LangSync` also updates tracked translation JSON under `src/main/resources/assets/creaturechat/lang/`: it preserves existing translations for current keys, fills new keys with fallback text, and removes obsolete keys. Review those changes before keeping them.

Ordinary `build` does not run datagen. Checked-in loot tables and advancements are available without generating them. If changing providers or generators, verify both the generated output and the resulting resource packaging.

## Version overrides and legacy tooling

`src/vs/vX_Y_Z/` contains cumulative Java overrides. The build applies every threshold at or below the selected Minecraft version, oldest first. A later file with the same relative path replaces the whole earlier class. See [ARCHITECTURE.md](ARCHITECTURE.md) for the adapter map and [CONTRIBUTING.md](CONTRIBUTING.md) for the required cross-version review.

`yarn_mappings` and `loom_version` in `gradle.properties` are legacy fields: the current build uses official mappings and selects Loom directly in `build.gradle`.

Use the wrapper for ordinary validation. `build.sh` is a legacy matrix/packaging script: it edits `gradle.properties` and `fabric.mod.json` in place, does not restore them automatically, skips tests and access-widener validation, and moves versioned jars into the repository root. Its Connector marker variants need hashes computed after packaging. It does not produce a complete updater release bundle automatically.

The existing `.gitlab-ci.yml` describes a legacy Linux runner and external publication jobs. Its pinned JDK differs from the current local toolchain; it is not evidence of a verified current CI matrix. An override directory or script target is a compatibility implementation to test, not a claim that every listed version currently builds or runs.
