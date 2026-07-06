# Building CreatureChat™

This **guide** explains how to build the MobChat fork of CreatureChat™ from <https://github.com/Le-Who/MobChat>. The fork is already present in this workspace; do not clone or publish against the public upstream CreatureChat repository unless explicitly requested.

## Prerequisites
- **Git**: If you don't have Git installed, download and install it from [git-scm.com](https://git-scm.com/).
- **Java JDK**: Fabric requires Java Development Kit (JDK) to compile and run. Download 
  and install it from [AdoptOpenJDK](https://adoptopenjdk.net/) or your preferred JDK provider. JDK version 8 or 11 
  is recommended.
- **IDE**: Any IDE that supports Java and Gradle, such as **IntelliJ IDEA**, **Eclipse**, or **VS Code**.

## Open the Project
Navigate into the local CreatureChat project:
```
cd CreatureChat
```

## Build the Mod
Use the Gradle wrapper included in the repository to build the project. Run the 
following command in the terminal within the project directory:

```
./gradlew build
```

For **Windows** users, use:

```
.\gradlew.bat build
```

This command compiles the project and outputs the build artifacts, including the 
mod `.jar` file, into the `build/libs` directory.

## Testing the Mod
To test the mod, you can run it in a development environment provided by Fabric:

```
./gradlew runClient
```

For **Windows** users, use:

```
.\gradlew.bat runClient
```

This command launches a Minecraft client with the mod loaded, allowing you to 
test the mod's functionality directly.

## Updating Dependencies

When Fabric or Minecraft is updated, the build dependencies also need to
be updated. Below are the general steps for this upgrade process.

1. Visit https://fabricmc.net/develop for updated version #s
1. Copy/paste the recommended versions into `gradle.properties`
1. **Optional:** Update the Loom version in `build.gradle`
1. Re-build: `./gradlew build` and watch for any errors
1. Re-run: `./gradlew runClient`
1. **Optional:** Re-start **IDE** to clear cached gradle
