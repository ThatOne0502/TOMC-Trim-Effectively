[简体中文](CHANGELOG.md) &nbsp;·&nbsp; **English**

# Changelog: TOMC-Trim Effectively

> Versions are `major.minor.fix.build`. The build number is a counter of successful builds, not a semantic version — a higher number promises nothing about compatibility, it only means one more build happened.
> The `mc1.21.11` and `mc26.1.2` suffixes on the jar name are the target game versions. The same version number carries the same features on both.
> Only changes a player or a pack author can notice are listed here. The full description of effects and data is in [docs/CONTENT.en.md](docs/CONTENT.en.md) and [docs/DATA.en.md](docs/DATA.en.md); the reasoning behind the design is in [docs/DESIGN.en.md](docs/DESIGN.en.md) and [docs/INTERNALS.en.md](docs/INTERNALS.en.md).
> The format follows Keep a Changelog. No dates: these build numbers come from a build counter, and there is no trustworthy release date to write down.

## 0.2.0.1

### Added

- Ported to NeoForge 26.1.2. Both platforms share one set of data files, effect implementations and documents; the artefact is `tomc-trimeffectively-<version>-neoforge+mc26.1.2.jar`.
- The NeoForge side has a GUI config screen again: the entry point is the `IConfigScreenFactory` extension point, with Cloth Config as an optional dependency. Without it, edit `config/trimeffectively.json` by hand.
- The glitch now fires once per hit instead of on a timer: the server sends one packet carrying a seed and a length, and for the first 2 frames the client swaps the texture for a magenta-and-black checker, then displaces, twists, rolls and squashes the whole body while individual model parts slip out of place.
- `Wavefunction Collapse` now plays only for the **player being watched**; when the watched entity is a mob, there is no sound at all.
- A logo in the mod list (NeoForge's `logoFile`).

### Changed

- The version line restarts at 0.2.0 with the build counter zeroed.
- The three copies of the store copy in `PUBLISHING.md` were reflowed to match the published page: plain one-line section titles, list items ending in a semicolon, no installation or command sections.
- Chinese names for trims and materials now match the in-game names with the tail cut off — a trim drops `Armor Trim` (`Shaper Armor Trim` is written `Shaper`), a material drops its `-ite`/qualifier suffix — and the documents no longer use invented translations.

### Fixed

- The model kept twitching after the effect ended: the timer driven by uuid and a time window is gone, and only taking damage triggers the glitch now.
- The wrapper exception in `.gitignore` only ever matched the repository root, so both `gradle-wrapper.jar` files were swallowed by the `*.jar` rule.

## 0.1.0.57

### Added

- A gaze freeze for `Phase Dissociation`: whenever any creature looks straight at you, within 32 blocks and roughly a 10° cone, the remaining seconds stop counting down; a viewer wearing a carved pumpkin does not count.
- Two sounds, `Wavefunction Collapse` and `Glitch`, each with a subtitle; the same `Wavefunction Collapse` never plays over itself.
- A new mod icon.

### Fixed

- The client decrements the effect duration itself, so the icon vanished before the server's copy did: the effect packet is now re-sent every 5 ticks while being watched.

## 0.1.0.53

- The public name is fixed as `[TRE] 纹以载道 Trim Effectively`.
- `PUBLISHING.md` became three layouts of one body of text (Chinese only / English only / bilingual).
- Every detailed document got an English counterpart, aligned pair by pair on headings and bullets.

## 0.1.0.52

- The documentation was split by reader into five files: `CONTENT` for players, `DATA` for pack and addon authors, `INTERNALS` for other developers, `DESIGN` for trade-offs, `STATUS` for what changed.
- The store copy was rewritten, and the two documents folded into `DATA` were deleted.

## 0.1.0.50

- The data directory contract the docs had wrong was corrected: data lives in `data/<pack>/trimeffectively/tre_{pattern,material}/`, the entry id is `<pack>:<filename>`, and which vanilla trim an entry binds to is decided by the JSON's `pattern` / `material` fields.

## 0.1.0.49

- Pre-release cleanup: the never-wired `anvil_penalty` was deleted, along with every experimental probe log and all dead code.
- The hard-coded JDK path in `gradle.properties` became a comment.
- `README`, `LICENSE` and `.gitattributes` were filled in.

## 0.1.0.47

- The eye trim's `Phase Dissociation` duration became √(2 × that hit's damage) seconds per piece.
- `Value` gained an optional `power` field.

## 0.1.0.46

- The interception of vanilla's 1/8 tipped-arrow duration was reverted.
- The eye trim's duration became "damage × pieces".

## 0.1.0.45

- The three delivery routes for `Phase Dissociation` were settled: potion 20 / 40 / 10 seconds, tipped arrows keep vanilla's 1/8, and stew is fixed at 5 seconds.

## 0.1.0.44

- Wild's "nine fish from one cast" is fixed: a nested loot table now rolls once, so a four-piece set no longer counts one catch four times.

## 0.1.0.41 – 0.1.0.42

- `Phase Dissociation`'s max-health penalty is quantised to whole seconds, so it no longer rewrites twenty modifiers a second and the heart row stops jittering.
- The scripted execution is gone: `MAX_HEALTH` bottoms out at 1.0, any hit kills, and the death message is guaranteed by the damage type.

## 0.1.0.40

- Three smithing-table recipes: strip the trim (iron nugget), strip and recover the template (iron nugget + book and quill), and toggle the trim (glass pane).
- The `extra_jumps` air jump with its wind-burst sound.
- A toggle for the recipe hijack, so template duplication can go back to vanilla.

## 0.1.0.36 – 0.1.0.37

- Configuration moved into a Mod Menu + Cloth Config GUI (both are soft dependencies): one scrolling page, a left-hand index, a search box on top.
- A read-only server → client sync.
- `0.1.0.37` corrected the read-only rule: the main menu can edit too, and only connecting to someone else's server greys it out.

## 0.1.0.35

- The template duplication recipe hijack: 19 vanilla template duplications now cost 1 diamond plus 7 of that pattern's own material.

## 0.1.0.34

- The sculk annotation gained a backing plate and sculk-cyan text colour.

## 0.1.0.32 – 0.1.0.33

- "Re-trimming does not refresh the effects" is fixed: the trim identity is written into the item component, and changing the trim forces a recompute.

## 0.1.0 early (first public build ~ 0.1.0.31)

- The trim effect system itself: two orthogonal lines, patterns and materials, stacking per worn piece.
- Every effect is JSON-driven and scanned into a dynamic registry kept in sync with the client.
- A home-grown `Value` structure (`value` / `per` / `stack` / `min` / `max`).
- In-place trim tooltips: numbers expand only while the "show trim effects" key is held.
- Two commands, plus the `Phase Dissociation` status effect and its own damage type.
