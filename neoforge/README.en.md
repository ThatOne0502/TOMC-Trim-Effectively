[简体中文](README.md) &nbsp;·&nbsp; **English**

# TOMC-Trim Effectively — NeoForge project

This is the **NeoForge 26.1.2** implementation. The mod description, installation and effect list
live in the repository [README](../docs/README.en.md); the Fabric 1.21.11 build is in
[fabric/](../fabric/README.en.md). Both share the same data files, effect implementations and
documentation — only the loader wiring differs.

## Build

```sh
./gradlew build        # Linux / macOS
gradlew.bat build      # Windows
```

- **Gradle itself needs JDK 17+**; the compile target is **Java 25** (`java.toolchain`, provisioned
  by foojay). 26.x ships Java 25 bytecode, so a local JDK 25 is not required — it is downloaded on
  the first build.
- If `JAVA_HOME` points at an old JDK (e.g. only Java 8 on `PATH`), see the notes at the top of
  [gradle.properties](gradle.properties), or pass
  `-Dorg.gradle.java.home="C:\path\to\jdk-21"` on each build.
- The first build runs NeoForm: download both 26.1.2 jars → decompile 6800+ sources → apply the
  NeoForge patches → recompile. Expect ~20 minutes once; afterwards it is cached in
  `~/.gradle/caches/neoformruntime` and takes seconds.

Output:

```
build/libs/tomc-trimeffectively-<version>-neoforge+mc26.1.2.jar
build/libs/tomc-trimeffectively-<version>-neoforge+mc26.1.2-sources.jar
```

The `-sources` one is for reading only — do not drop it into `mods/`.

The version comes from `mod_version` in [gradle.properties](gradle.properties) (same number as the
Fabric build, without the build counter).

## Development commands

```sh
gradlew runClient      # start the client with the mod
gradlew runServer      # start the server with the mod
```

A healthy server start prints:

```
TOMC-Trim Effectively loaded
[tre] recipe hijack: 19 of 1519 recipes now take 1 diamond and 7 of the pattern's own material
Loaded 1519 recipes
[tre] server started - tre_pattern=18 tre_material=11
```

The last line reports the scanned entry counts and should follow any datapack change.

## Code map

Same layout as the [Fabric code map](../fabric/README.en.md); only the loader layer differs:

| Where | NeoForge shape |
| --- | --- |
| `Trimeffectively.java` | `@Mod` constructor with `IEventBus`; registration via `RegisterEvent` / `DeferredRegister` |
| `TrimeffectivelyClient.java` | `@Mod(value = MOD_ID, dist = Dist.CLIENT)`, events on `NeoForge.EVENT_BUS` |
| `data/TreRegistries.java` | `DataPackRegistryEvent.NewRegistry#dataPackRegistry` (synced datapack registries) |
| `engine/PortalTracker.java` | `AttachmentType` + `PlayerChangedDimensionEvent` / `PlayerLoggedInEvent` |
| `net/` | `RegisterPayloadHandlersEvent` + `PacketDistributor` |
| `effect/TreAttributes.java` | `EntityAttributeModificationEvent` to attach player attributes |
| `client/TrimTooltip.java` | `RegisterKeyMappingsEvent` + `ItemTooltipEvent` |
| `client/SoundAnnotations.java` | `RegisterGuiLayersEvent#registerAboveAll` + `GuiGraphicsExtractor` |

## Notes

- Mappings are **official Mojang mappings**. Since 26.x Minecraft is not obfuscated at all, so the
  class and method names are the mojmap ones (`Identifier`, `LivingEntityRenderer`, `ModelPart`).
- Two **package moves** versus 1.21.11: `CameraRenderState` lives in
  `net.minecraft.client.renderer.state.level`, `RenderType` in
  `net.minecraft.client.renderer.rendertype`.
- Data files keep the same location: `src/main/resources/data/trimeffectively/trimeffectively/` —
  first level is the namespace, second is the registry id (`trimeffectively:tre_pattern`).
- **The config screen comes from Cloth Config** (optional dependency): with it installed, the mod
  list grows a `Config` button, wired through NeoForge's `IConfigScreenFactory` extension point (the
  counterpart of Mod Menu's `ModMenuApi` on Fabric). Without it there is only
  `config/trimeffectively.json`, and the file writing plus server sync behave identically. The
  compile-time dependency is `compileOnly` (`me.shedaniel.cloth:cloth-config-neoforge:26.1.154`), so
  nothing is bundled and nothing is declared mandatory.
- The mod-list banner comes from `logoFile` in `neoforge.mods.toml`, pointing at
  `assets/trimeffectively/logo.png`; on Fabric the equivalent is `icon` in `fabric.mod.json`.
