[简体中文](../README.md) &nbsp;·&nbsp; **English**

> [!WARNING]
> **Early Development**
>
> This project targets Minecraft **1.21.11 · Fabric** only and is in the `0.1.0.x` line. Content, data format and options may still change without backward compatibility. Back up your world before updating.
>
> **AI-Assisted**
>
> This project was developed with assistance from AI (DeepSeek Harness, deepseek-flash). Not all code, documentation and design have been reviewed and tested by humans.

# TOMC-Trim Effectively

> Published as **`[TRE] Trim Effectively`** (Chinese title: `[TRE] 纹以载道`). The in-game mod name stays `TOMC-Trim Effectively`.

> 📘 **Data & type reference** — [DATA.md](DATA.md) (Chinese) &nbsp;·&nbsp; documentation index — [README.md](README.md) (Chinese)

**In vanilla, an armour trim is only a pattern. Here it does something.**

Once a trim is applied, the **pattern** and the **material** each bring their own set of effects: `wild` lets you pull more fish out of the water, `diamond` doubles durability, `silence` swallows the sounds you make, `ward` turns vibrations into cyan callouts only you can see. A single piece can feed off both its pattern and its material, and effects stack **per worn piece** — not per trim tier, and not as a multiplier on one entry. Four `wild` pieces are four extra rolls; four `diamond` pieces each double their own durability.

The entire catalogue is **data**: 18 trim patterns and 11 trim materials, 29 entries and 20 effect types, all written as JSON and loaded into a synced dynamic registry. A datapack can replace the whole catalogue, append to it, or switch any entry off — including entries for trims added by your own mod.

## What it does

| Trim | Effect |
| --- | --- |
| `wild` | Extra loot rolls per piece on kills and loot containers; a full set pulls 5 fish per cast |
| `eye` | On hit, a 10%-per-piece chance to apply `Endphase` for √(2 × damage) seconds × pieces |
| `spire` | Reduces incoming damage by movement speed: 1% per m/s on every piece |
| `netherite` | −30% `burning_time` per piece (four pieces never burn), −16% damage taken, fire-proof item |
| `diamond` | Durability cap ×2 on each piece |
| `silence` | A 30%-per-piece chance to swallow the sounds you make |
| `ward` | Client-side only: vibration particles and a cyan label above the head, plus a rewritten damage type |
| Dragon-head potion | The `Endphase` effect itself: max health pinned to 1, water hurts, magenta; brewed from a dragon head, also used for stew and tipped arrows |

The full list is in [CONTENT.md](CONTENT.md) (Chinese).

## Installation

| Requirement | Notes |
| --- | --- |
| Minecraft | 1.21.11 |
| Fabric Loader | ≥ 0.19.3 |
| Fabric API | Required |
| Java | 21 |
| Mod Menu + Cloth Config | Optional; only needed for the in-game config screen |

Drop the jar into `mods/`. Both client and server need it (`environment: "*"`), and the **versions must match** — the effect catalogue is a synced dynamic registry, so a mismatch will refuse the connection.

## Commands

Gamemaster permission required:

- `/tre dump` — prints the loaded entries, the pattern/material of every worn piece, and how many pieces each entry actually counted.
- `/tre reload` — re-reads the datapack and forces a re-stamp (how old items heal themselves).

## Documentation

| If you want to… | Read | Written for |
| --- | --- | --- |
| see what exists in game | [CONTENT.en.md](CONTENT.en.md) | players, modpack users |
| write your own effects or trims | [DATA.en.md](DATA.en.md) | datapack and mod authors |
| know how it works inside | [INTERNALS.en.md](INTERNALS.en.md) | developers |
| know why it is built this way | [DESIGN.en.md](DESIGN.en.md) | anyone interested in the trade-offs |
| see what changed in each build | [STATUS.en.md](STATUS.en.md) | everyone |

Every document exists in both languages with matching numbering; the Chinese version is the source of truth and each English file has a switch link back to its counterpart. The full index is in [README.index.en.md](README.index.en.md).

## Building from source

```sh
cd fabric
./gradlew build        # Linux / macOS
gradlew.bat build      # Windows
```

Output: `fabric/build/libs/tomc-trimeffectively-<version>-fabric+mc1.21.11.jar`. Gradle itself needs **JDK 17+** to start; the **Java 21** compile target is provisioned by the toolchain. See [fabric/README.md](../fabric/README.md) for the jar naming, `runClient`/`runServer` and the code map.

## Licence & credits

Code is released under **GPL-3.0-or-later**; the full text is in [LICENSE](../LICENSE).

Developed with assistance from DeepSeek Harness (deepseek-flash): bytecode forensics, data-format design, documentation and build verification. The design decisions and the final responsibility are the author's.
