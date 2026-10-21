[简体中文](README.md) &nbsp;·&nbsp; **English**

# TOMC-Trim Effectively — Project Directory

This is the implementation itself. For the mod description, installation and the effect list, see the repository root [README.md](../README.md).

## Building

```sh
gradlew.bat build      # Windows
./gradlew build        # Linux / macOS
```

- **Gradle itself needs JDK 17+** to start; the compile target is **Java 21** (`java.toolchain`,
  provisioned automatically by foojay). Do not confuse the two: `JAVA_HOME` decides which JVM
  runs Gradle, the toolchain decides which JDK `javac` uses.
- If `JAVA_HOME` points at an old JDK, read the note at the top of
  [gradle.properties](gradle.properties): either uncomment `org.gradle.java.home` there, or pass
  `-Dorg.gradle.java.home=...` on each build.
- The first build downloads Gradle, the toolchain and the dependencies; expect 5–15 minutes.

Output:

```
build/libs/tomc-trimeffectively-<maj>.<min>.<rev>.<build>-fabric+mc1.21.11.jar
build/libs/tomc-trimeffectively-<...>-sources.jar   ← sources only, do not drop into mods/
```

The first three version segments are maintained by `baseVersion` in
[build.gradle](build.gradle); the fourth is the build counter in `build-count.txt` (written back
only when `remapJar` succeeds — a failed build does not burn a number). The `sources` jar is
published for source distribution only; dropping it into `mods/` fails at mixin lookup because it
contains no `.class` files.

## Development commands

```sh
gradlew runClient      # start the client with the mod
gradlew runServer      # start a server with the mod (handy for verifying registries/recipes)
```

The data files are hand-written JSON; there is no datagen entrypoint.

The server should print three self-check lines on startup:

```
TOMC-Trim Effectively loaded
[tre] recipe hijack: 19 of 1474 recipes now take 1 diamond and 7 of the pattern's own material
[tre] server started - tre_pattern=18 tre_material=11
```

The numbers on the last line are "how many entries were scanned"; they must change when you edit a datapack.

## Code map

| Package | Responsibility |
| --- | --- |
| `data/` | Data model and codecs: `TrimEffect`, `EffectEntry`, `Value` (including the `power` exponent), the dynamic registries |
| `engine/` | `TreEngine` (registration and sweeps), `TrimSnapshot` (every worn piece), `TrimStamp` (stamping/identity), `AttributeEngine` (5 Hz attribute refresh), `TreRecipes` (recipe hijack) |
| `effect/` | One class per `type`; `DamageEffects` is the dispatch table for hits and incoming damage |
| `mixin/` | Only what vanilla cannot do from the outside (loot-table re-entry, damage settlement, item property reads, …) |
| `client/` | Mod Menu config screen, HUD / particles / sound callouts, tooltips |
| `net/` | Server → client config sync |
| `command/` | `/tre dump`, `/tre reload` |
| `recipe/` | The three smithing-table recipes (strip a trim, strip and return the template, toggle one piece) |

## Notes

- This project uses **official Mojang mappings** (mojmap), not Yarn: `ResourceLocation`, `Holder`,
  `BuiltInRegistries`. Code copied from Yarn tutorials will not compile.
- Data files live under `src/main/resources/data/trimeffectively/trimeffectively/` — the first
  level is the **namespace**, the second is the **dynamic registry id**
  (`trimeffectively:tre_pattern`).
- Rendering and sound are client-only; numbers and decisions are server-only.
