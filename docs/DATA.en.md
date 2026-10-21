[简体中文](DATA.md) &nbsp;·&nbsp; **English**

# TOMC-Trim Effectively Data Reference

> Written for modpack authors, datapack authors, and mod authors who want to hook into this mod without writing Java.
> After reading it you will know: where the data goes, how to write an entry that loads, how to compose numbers with `Value`, what the 20 types in use and the 10 triggers in use mean, and how to change numbers against the 29 ready-made entries in the appendix.
> This document only covers "what to write". For why the engine computes it this way and where the code hangs, see [INTERNALS.md](INTERNALS.en.md); for design trade-offs, see [DESIGN.md](DESIGN.en.md).

---

## 1. Where the data goes

The dynamic registry keys are `trimeffectively:tre_pattern` and `trimeffectively:tre_material`; the data directory is given by the registry key's
`namespace/path`:

| Registry | Directory | Example entry id | Binding field |
|---|---|---|---|
| `trimeffectively:tre_pattern` | `data/<pack>/trimeffectively/tre_pattern/<file name>.json` | `trimeffectively:sentry` | `"pattern": "minecraft:sentry"` |
| `trimeffectively:tre_material` | `data/<pack>/trimeffectively/tre_material/<file name>.json` | `trimeffectively:copper` | `"material": "minecraft:copper"` |

- **Entry id = `<pack>:<file name>`**. `<pack>` is the datapack namespace (the data that ships with this mod uses `trimeffectively`).
- **The file name is only a label**. Which vanilla trim a piece of data serves is decided by the `pattern` / `material` field in the JSON:
  `"pattern": "minecraft:sentry"`. The field accepts an array, so one entry can serve several trims at once.
- The data that ships with this mod lives in `data/trimeffectively/trimeffectively/tre_pattern/` and `.../tre_material/`,
  with entry ids such as `trimeffectively:sentry`.
- Writing a vanilla id that does not exist in `pattern` / `material` **is not existence-checked**: it does not error, it simply never matches (the same as not writing it).
  Matching uses the trim id actually on the item.
- The mod's own content also lives under `data/trimeffectively/`: the damage type `trimeffectively:endphase`, four recipe files,
  and one trade/loot related id. For trims added by third parties (e.g. Trimmable Tools), dropping a copy at
  `data/<your namespace>/trimeffectively/tre_pattern/<any name>.json` under **your own namespace** with the right `pattern` is enough; this mod needs no changes.

---

## 2. The skeleton of a file

```json
{
  "pattern": "minecraft:sentry",
  "effects": [ { "...": "..." } ]
}
```

| Field | Type | Description |
|---|---|---|
| `pattern` / `material` | an id or an array of ids | One of the two, depending on the directory (`tre_pattern/` takes `pattern`, `tre_material/` takes `material`). An array = one entry serving several trims. |
| `slots` | optional, a slot or an array of aliases | Default `["armor","hand"]`. Aliases: `armor` = head, chest, legs, feet; `hand` = main and off hand; `any` = all. |
| `effects` | array | Every effect in this file; several are evaluated in order, numbers of the same kind are added first and multiplied after. |

The derived lang keys (using the file name `long_shot.json`, i.e. the entry `trimeffectively:long_shot`):

```
trim_effect.trimeffectively.long_shot.name  = Long Shot (百步穿杨)
trim_effect.trimeffectively.long_shot.desc  = +5% damage per 1 block of distance to the target…
```

Renaming the file changes the entry id and both of these keys — the one thing you have to know when overriding someone else's data or translating it.

---

## 3. The effect object

```json
{
  "type": "trimeffectively:damage_out",
  "trigger": "trimeffectively:attack_out",
  "amount": { "value": 0.05, "per": ["attack_distance"], "stack": "per_piece" },
  "conditions": []
}
```

| Field | Description |
|---|---|
| `type` | The effect type, which decides "what it does". **Must carry a namespace** (it is a real registry; a bare name is treated as `minecraft:`). |
| `trigger` | When. **Omitted = `equipped` (always on)**; attribute-like effects almost always omit it. Not a registry, but it is uniformly written with the `trimeffectively:` prefix; bare names are also accepted. |
| `amount` / other number fields | Always a `Value`, see §4. **Every number field is `Value`-shaped; never write a bare number** (`radius`, `count`, `duration` too). |
| `operation` | **Only the `attribute` type has it**, with values `add_value` / `add_multiplied_base` / `add_multiplied_total`. Other types omit it — their `amount` is itself "a ratio relative to a baseline" (`0.05` = +5%, `-0.16` = -16%), and the sign is the direction. |
| `conditions` | An array of vanilla `LootItemCondition` (`entity_properties` / `damage_source_properties` / `location_check` / `all_of` / `inverted` / `value_check`…). |
| `client` | `true` means a pure client-side effect: the server only syncs the definition, it does not run it. |

### 3.1 Vanilla vocabulary that has been verified (checked against the 1.21.11 `minecraft-merged.jar`)

- **All attribute ids exist**: `minecraft:armor` `armor_toughness` `movement_speed` `attack_speed`
  `block_break_speed` `oxygen_bonus` `water_movement_efficiency` `luck` `knockback_resistance` `burning_time`.
- **Attributes this mod adds**: `trimeffectively:crit_multiplier` (default 1.5, range 0~20, synced to the client) and
  `trimeffectively:extra_jumps` (default 0). They are **real attributes**: commands, other mods and the client treat them like any other;
  you write them exactly like `minecraft:` attributes (`attribute` holds the id, `operation` holds `add_value`).
- **The damage type tags exist**: `minecraft:is_fire`, `minecraft:is_projectile`.
  Since 1.21 `DamageSourcePredicate` **no longer has an `is_projectile` boolean field**; use the tag predicate
  `{"tags": [{"id": "minecraft:is_projectile", "expected": false}]}` (`expected: false` = must **not** carry that tag, i.e. melee).
- The `wither_immune_to` / `always_hurts_ender_dragons` tags exist, and **do not add endphase to them** (settled: it is not immune to withers or the ender dragon).

---

## 4. `Value` — numbers, variables, stacking

```json
"amount": { "value": 0.05, "per": ["attack_distance"], "power": 1.0, "stack": "per_piece", "min": null, "max": null }
```

The formula: `final = value × Π(each variable in per)^power × stackFactor`, then `clamp(min,max)`.

| Field | Type | Description |
|---|---|---|
| `value` | required, number | The base value per piece. |
| `per` | optional, array of strings | Names of the context variables multiplied in, see the table below. Empty/omitted = a plain constant. **A missing variable is substituted as 1.0**. |
| `power` | optional, number, default 1 | Applies only to the `per` product; when the product is negative the exponentiation is skipped (to avoid NaN). `0.5` is a square root. |
| `stack` | optional, default `per_piece` | The piece factor, see §4.1. |
| `min` / `max` | optional, number | Clamping after evaluation. No entry in this version writes them; the mechanism is kept. |

`power` only bends the `per` product: `value` and the piece factor are still multiplied linearly. So "grows with the square root of damage" needs no expression language —
a single `0.5` is enough; when the variables in `per` are missing the product degrades to a constant, and the square-rooted entry merely becomes a linear constant.

### 4.1 `stack`: four ways to stack

| Value | Meaning | Result |
|---|---|---|
| `per_piece` (**default**) | × the number n of pieces currently in effect (1~4) | 1 piece +5%, 4 pieces +20% |
| `per_piece_capped` | × min(n, `stack_cap`) | Capped by `stack_cap`; `stack_cap` is the companion field of this form |
| `once` | applies as soon as there is ≥1 piece, value unchanged | 1 piece and 4 pieces are the same |
| `full_set` | applies only when n == 4 | one piece short and it is zero |

`stack` **can be written at effect level** (as the default for all numbers in that effect) **and can also be written in any single `Value` to override it**.
`vex.json` uses exactly this: the summon count stacks per piece (`per_piece` — take one hit in a full set and four vexes appear), while the lifetime follows only this hit's damage (`once`).
The two `Value`s each use their own `stack`; neither affects the other.

> `per_piece_capped`'s `stack_cap` is currently contract only: the engine side has no cap branch yet, so that form is treated as `per_piece`.
> When you need a cap, use `max` (`final = value × Πper × n`, clamped afterwards); the consequence is that clamping happens after multiplying by the piece count, which is not the same semantics as "cap the piece count first".

### 4.2 Context variables

| Variable | Meaning |
|---|---|
| `attack_distance` | distance to the target at the moment of the attack (blocks) |
| `damage_taken` / `damage_dealt` | this hit's damage value (taken side / dealt side) |
| `health_ratio` / `missing_health_ratio` | 0~1 |
| `speed_mps` | horizontal speed in m/s, the peak over the entity's last 5 ticks; the vertical component is not counted while standing |
| `harmful_effect_count` | number of non-positive effects (**fire included**: being on fire counts as one) |
| `gold_group_count` | the number of "groups" of gold items in the inventory |
| `on_fire` | 0 / 1 |
| `gear.armor` | **the sum of the armour values of all pieces in effect themselves** |

`gear.*` are "sum over the pieces in effect" variables: mathematically equivalent to ordinary context values, only the source differs.

Common combinations:

- `Fight or Flight` (material · amethyst): `{ "value": 1.0, "per": ["missing_health_ratio"], "stack": "per_piece" }`,
  with `operation` set to `add_multiplied_base`, applied to `movement_speed`. One piece at full health +0%, near death approaching +100% (i.e. 200% speed);
  4 pieces near death approaching +400% (i.e. 500% speed).
- `Long Shot` (trim · sentry): `{ "value": 0.05, "per": ["attack_distance"] }`. 1 piece, 3 blocks → +15%; 4 pieces, 4 blocks → +80%. No cap.
- `Kinetic Barrier` (trim · spire): `{ "value": -0.01, "per": ["speed_mps"], "stack": "per_piece" }`. -1% per 1 m/s per piece.

---

## 5. The `type` table (20 in use)

`type` is a registry id and **must carry a namespace**. In the "unique fields" column below, field names other than `trigger` / `conditions` / `stack` are the JSON field names.

| # | `type` | Meaning | Unique fields |
|---|---|---|---|
| 1 | `trimeffectively:attribute` | attribute modifier | `attribute` (attribute id), `operation` (`add_value` / `add_multiplied_base` / `add_multiplied_total`), `amount` (`Value`) |
| 2 | `trimeffectively:damage_out` | bonus/cut to the damage we deal | `amount` (`Value`, percentage, 0.05 = +5%) |
| 3 | `trimeffectively:damage_in` | bonus/cut to the damage we take | `amount` (`Value`, negative = reduction) |
| 4 | `trimeffectively:damage_type_override` | rewrites the damage type of this hit | `damage_type` (damage type id), `keep_attacker` (bool, default `true`, **you must not drop the attacker**, otherwise the `.player` variants of death messages do not fire) |
| 5 | `trimeffectively:summon` | summon mobs | `entity`, `count` (`Value`), `lifetime` (`Value`, seconds), `inherit_equipment` (bool), `attack_target` (`"attacker"` / `"victim"` / omitted) |
| 6 | `trimeffectively:extra_loot_roll` | extra loot rolls | `rolls` (`Value`) |
| 7 | `trimeffectively:apply_mob_effect` | apply a status effect | `effect`, `chance` (`Value`), `duration` (`Value`, ticks), `amplifier` (`Value`), `target` (`"victim"` / `"attacker"` / `"self"`) |
| 8 | `trimeffectively:item_property` | properties of the item itself (`scope: item`, **stamped into the item components**, see §9) | `properties` (open map: key → boolean/number/string) |
| 9 | `trimeffectively:trade_price` | trade price adjustment | `amount` (`Value`, negative = cheaper) |
| 10 | `trimeffectively:trade_keep` | trades do not consume a use | `chance` (`Value`) |
| 11 | `trimeffectively:breed_cooldown` | breeding cooldown adjustment | `amount` (`Value`, negative = faster) |
| 12 | `trimeffectively:mute_sound` | mute sounds | `chance` (`Value`), `scope` (`"both"` / `"audible"` / `"vibration"`) |
| 13 | `trimeffectively:annotate_sound` | client-side sound annotation | `radius` (`Value`, blocks), `client` (bool, must be `true`), `exclude_own` (bool), `blocked_by` (block tag), `particle`, `label` (see §7) |
| 14 | `trimeffectively:harmful_effect_rate` | countdown rate of harmful effects | `amount` (`Value`, `2.0` = +200%) |
| 15 | `trimeffectively:crit_damage` | critical damage bonus | `amount` (`Value`). The ready-made entries have moved to the attribute `trimeffectively:crit_multiplier`, see `snout.json` in appendix A.1 |
| 16 | `trimeffectively:xp_gain` | XP gain adjustment | `amount` (`Value`) |
| 17 | `trimeffectively:thorns` | melee damage reflection | `amount` (`Value`, `0.5` = reflect 50% of the damage taken) |
| 18 | `trimeffectively:piglin_neutral` | piglin neutrality | `also_brutes` (bool) |
| 19 | `trimeffectively:extra_jumps` | extra jumps | `count` (`Value`). Implemented as the real attribute `trimeffectively:extra_jumps` |
| 20 | `trimeffectively:invulnerability_window` | length of the hurt invulnerability window | `amount` (`Value`, negative = shorter) |

> That is 20 `type`s in total. Three retirements follow; the numbering above has already been shifted along with them; the "21 kinds" count in the old draft included these removed items.
>
> 0.1.0.29 removed `trimeffectively:damage_protection` (the protection-point channel). Spire moved to the ready-made `damage_in`, see the `spire.json` row in appendix A.1.
>
> 0.1.0.31 retired `trimeffectively:fire_duration` and the `ignite` trigger as well: netherite's burning time moved to the vanilla `minecraft:burning_time` attribute (`add_multiplied_base`, -0.30 per piece), on the same multiplicative channel as fire protection.
>
> 0.1.0.48 removed `trimeffectively:anvil_penalty`: `keep_repair_cost` had long since been implemented by stamping with `item_property` (see the `shaper.json` row in appendix A.1), and this type had no attach point left — dead code. The numbering in the table above shifted along with it.

Adding a new `type` only happens on the code side (one class plus a registration line); a datapack cannot register new types.

---

## 6. The `trigger` vocabulary

A closed enum; write it with the `trimeffectively:` prefix, bare names are also accepted:

`equipped` (the default when omitted, always on) · `attack_out` · `take_damage_before` · `take_damage_after` ·
`kill` · `loot_roll` · `trade` · `breed` · `xp_gain` · `sound_emitted`

10 values, no aliases, all implemented. `loot_roll` covers the container/fishing/archaeology entry points, `kill` is kill-only; the rest are events.

`trigger` itself is a free `Identifier`: writing something else does not error, but it will never fire. Names such as `death` / `tick` /
`item_break` / `heal` are reserved as future semantic slots and are not implemented today; writing them is the same as not writing the effect.

---

## 7. The shape of three complex fields

`annotate_sound.label`:

```json
{ "subtitle": true, "visible_ticks": 40, "fade_ticks": 60, "see_through": true, "billboard": "center" }
```

`subtitle: true` = take that sound's closed-caption text; if it is unavailable, fall back to the raw sound id (`minecraft:entity.zombie.ambient`).

`apply_mob_effect.chance` / `trade_keep.chance` / `mute_sound.chance`: a `Value`, whose result is clamped to `[0,1]` inside the engine.
This is the hard floor against illegal values (duration ≥ 0 ticks, probability ∈ [0,1], multiplier ≥ -1); it is not a balance clamp, and it is not written into the JSON.

`blocked_by`: a block tag id, **without `#`** (`#` is only vanilla's own tag-reference syntax). `"minecraft:wool"` =
if wool sits between the sound source and the player, no annotation; aligned with the warden's hearing filter.

---

## 8. Validation, loading and hot reload

```
Load: data/*/trimeffectively/tre_{pattern,material}/**.json → dynamic registry (hot reload, server→client sync included)
Per evaluation:
  1. collect every piece in effect on the entity (head/chest/legs/feet/main/off hand, any that carries the TRIM component)
  2. match pattern/material to an entry → giving the "set of pieces in effect"
  3. for each effect: does trigger match? do all conditions pass? → evaluate Value
  4. aggregate: numbers of the same kind are added first, then multiplied → apply
```

- **Decode is validation**: when the JSON is wrong the error message gives the file and the field path; a failed load does not affect other files.
- **Hot reload**: `/reload` and `/tre reload` both reload these two registries; entries are recomputed with the new values immediately (stamped items excepted, see §9).
- **Missing variables count as 1.0**: a variable written in `per` that does not exist does not error, it just degrades to multiplying by 1.
- **Conditions without context are skipped**: an entry with `conditions` is treated as "not met" when there is no damage source to judge against; it is never treated as satisfied.
- **`appliesTo` is not validated**: `pattern` / `material` pointing at a vanilla id that does not exist simply never matches.
- **Debugging**: `/tre dump` (gamemaster permission) lists the pattern / material entries currently in effect, the piece counts and the final aggregated value of every attribute;
  `/tre reload` (same permission) reloads the registries and refreshes the cache.

---

## 9. `item_property`'s `properties`

`item_property` answers questions **about the item itself** — "how durable is this item", "will it burn up", "would a piglin want it".
The answers are **stamped into the item components** at the moment the item is trimmed, and every later question just reads the components; dropped items, item frames, durability bars,
and other mods scanning an inventory can all read them, with no need for the wearer to be online. For when stamping and unstamping happen, see [INTERNALS.md](INTERNALS.en.md).

The four ready-made keys:

| Key | Type | Who uses it |
|---|---|---|
| `durability_multiplier` | number | diamond (`2`: durability ×2) |
| `fire_resistant` | boolean | netherite (`true`: the item is not destroyed by fire) |
| `piglin_loved` | boolean | gold ingot (`true`: piglins treat it as gold) |
| `repair_cost` | number | reserved for Excelsior (精益求精) |

`properties` is an open map, so adding a new key needs no change to the storage layer; but **someone still has to read every key on the Java side** — that is the physical limit.
Write a key nobody reads and the component will hold the value while nothing happens in game.

---

## 10. Decode order for literals

The `properties` value of `item_property` is a union type (number / boolean / string). The decode order must be **number → boolean → string**:
the default implementation of `DynamicOps#getBooleanValue` is `getNumberValue(input).map(n -> n.byteValue() != 0)`,
so trying boolean first decodes `"durability_multiplier": 2` as `true` (that was the bug before 0.1.0.31; the multiplier was always 1.0).
Conversely, a real boolean is not accepted by `getNumberValue`, so number-first order is lossless. Any "number or boolean" literal follows this rule.

Old items that were already stamped are not corrected automatically; they need to be re-trimmed, or forced to recompute with `/tre reload`.

---

## 11. Third parties: adding your own data / replacing wholesale

**Adding** (without touching this mod's files):

1. Create `data/<your namespace>/trimeffectively/tre_pattern/<any name>.json` under your own namespace;
2. write `pattern` as `minecraft:<trim id>`, or directly the id of a trim your own mod adds;
3. the entry id is then `<your namespace>:<file name>`, and the lang keys follow from it;
4. for an item to actually carry a trim, what matters is the vanilla `minecraft:trim` component — vanilla's smithing template checks the base slot against
   the `#minecraft:trimmable_armor` item tag, so adding things to that tag lets tools and custom armour take trims. This mod only reads the `TRIM` component,
   and **needs no compatibility code at all**.

**Replacing wholesale** (with your own numbers):

- On datapack load, the later-loaded copy wins for the same id. Re-provide the `trimeffectively:sentry` entry with a same-named file in the same pack namespace,
  or drop another `data/trimeffectively/trimeffectively/tre_pattern/sentry.json` into a higher-priority datapack, to replace it wholesale.
- Replacement is **wholesale**: the `effects` array in the file is replaced as a whole, not merged with the original file. To keep one effect and change one, copy the original file and edit it.
- To disable an entry: provide a same-id file with `effects: []`, or put your datapack ahead of it so it overrides.
- After replacement the entry id is unchanged and the lang key is unchanged, so players still see the original effect name; renaming the file is the same as swapping in a different effect, and old translations stop working.

---

## 12. Complete samples

### 12.1 `sentry.json` — an event + a distance variable + per-piece stacking

```json
{
  "pattern": "minecraft:sentry",
  "effects": [
    {
      "type": "trimeffectively:damage_out",
      "trigger": "attack_out",
      "amount": { "value": 0.05, "per": ["attack_distance"], "stack": "per_piece" }
    }
  ]
}
```

1 piece, 3 blocks → +15%; 4 pieces, 4 blocks → +80%. **No cap** (settled).

### 12.2 `eye.json` — a chance + a square-rooted duration

```json
{
  "pattern": "minecraft:eye",
  "effects": [
    {
      "type": "trimeffectively:apply_mob_effect",
      "trigger": "trimeffectively:attack_out",
      "effect": "trimeffectively:endphase",
      "chance":    { "value": 0.1, "stack": "per_piece" },
      "duration":  { "value": 28.284271247461902, "per": ["damage_dealt"], "power": 0.5, "stack": "per_piece", "min": 0 },
      "amplifier": { "value": 0, "stack": "once" },
      "target": "victim"
    }
  ]
}
```

`duration` is in ticks. **Per-piece duration = √(2 × this hit's damage) seconds**: `power: 0.5` takes the square root, `value` is `20 × √2 = 28.284…`
(`duration` counts ticks, 20 ticks = 1 second), `stack: per_piece` then multiplies by the piece count, and `min: 0` catches negative damage.
A 6-damage hit: 1 piece √12 ≈ 3.5 s, 4 pieces ≈ 13.9 s; a 20-damage hit with 4 pieces ≈ 25.3 s; a 50-damage hit with 4 pieces = 40 s.
Chance and duration are independent: `chance` stacks per piece (4 pieces = 40% per hit), and `duration` can also be written as a fixed `once` value.

### 12.3 `ward.json` — client-side rendering + a damage type override

```json
{
  "pattern": "minecraft:ward",
  "effects": [
    {
      "type": "trimeffectively:damage_type_override",
      "trigger": "attack_out",
      "damage_type": "minecraft:sonic_boom"
    },
    {
      "type": "trimeffectively:annotate_sound",
      "trigger": "sound_emitted",
      "client": true,
      "radius": { "value": 8 },
      "exclude_own": true,
      "blocked_by": "minecraft:wool",
      "particle": "minecraft:vibration",
      "label": {
        "subtitle": true,
        "visible_ticks": 40,
        "fade_ticks": 60,
        "see_through": true,
        "billboard": "center"
      }
    }
  ]
}
```

- `client: true` is **required**, not an optional optimisation: `annotate_sound` only renders on this client, it is not broadcast.
- Timeline: 0–2s fully opaque, fading from 2s over 3 seconds, gone at 5s → `visible_ticks 40` + `fade_ticks 60`.
- `keep_attacker` omitted means `true`; be sure to keep it — drop the attacker and the `.player` variants of death messages will not fire.

### 12.4 `netherite.json` — three families in one file

```json
{
  "material": "minecraft:netherite",
  "effects": [
    {
      "type": "trimeffectively:damage_in",
      "trigger": "trimeffectively:take_damage_before",
      "amount": { "value": -0.16, "stack": "per_piece" },
      "conditions": [
        {
          "condition": "minecraft:damage_source_properties",
          "predicate": { "tags": [ { "id": "minecraft:is_fire", "expected": true } ] }
        }
      ]
    },
    {
      "type": "trimeffectively:attribute",
      "attribute": "minecraft:burning_time",
      "operation": "add_multiplied_base",
      "amount": { "value": -0.30, "stack": "per_piece" }
    },
    {
      "type": "trimeffectively:item_property",
      "property": "fire_resistant",
      "value": true
    }
  ]
}
```

| # | Entry | `stack` | At 4 pieces | Notes |
|---|---|---|---|---|
| 1 | -16% fire damage | `per_piece` | -64% | no clamp |
| 2 | -30% burning time | `per_piece` | -120% | no clamp → 4 pieces never catch fire at all. Uses the vanilla `minecraft:burning_time` attribute multiplication and stacks with fire protection; the attribute's own floor of 0 prevents negatives |
| 3 | the item is not destroyed by fire | — | — | no `trigger`, no `Value`: `scope: item` |

Entry 3 is the only one using the `item` scope. It has no notion of piece count — one piece is one piece — so writing `stack` would be pointless and leaving it out is unambiguous.
In the shipped file this entry is written `"properties": { "fire_resistant": true }`; the two forms are equivalent.

---

## Appendix A: the wiring of the 29 entries

Path convention: `data/trimeffectively/trimeffectively/tre_pattern/<file name>.json` and `.../tre_material/<file name>.json`.
In the "key numbers" column an omitted `"stack"` means the default `per_piece`; `Value`s are written in `{value, per}` form.

### A.1 Trims (18)

| File | `type` | `trigger` | Key numbers and fields | Notes |
|---|---|---|---|---|
| `sentry.json` | `damage_out` | `attack_out` | `amount {value:0.05, per:["attack_distance"], stack:"per_piece"}` | +5% per 1 block, stacks per piece, no cap |
| `vex.json` | `summon` | `take_damage_after` | `entity:"minecraft:vex"`, `count {value:1, stack:"per_piece"}`, `lifetime {value:1.0, per:["damage_taken"], stack:"once"}`, `inherit_equipment:true`, `attack_target:"attacker"` | a full set takes one hit and summons four; damage points = seconds alive, the duration does not stack |
| `wild.json` | `extra_loot_roll` ×2 | `kill` / `loot_roll` | both are `rolls {value:1, stack:"per_piece"}` | one for kills and one for chests/fishing/archaeology; a single loot generation rolls only once |
| `coast.json` | `attribute` ×2 | `equipped` | ①`minecraft:oxygen_bonus` / `add_value` / `{value:2}` ②`minecraft:water_movement_efficiency` / `add_value` / `{value:0.667}` | 0.667 = the same value as Depth Strider II |
| `dune.json` | `invulnerability_window` | `attack_out` | `amount {value:-0.30, stack:"per_piece"}` | after a hit, the target's hurt invulnerability window is cut to 70% |
| `wayfinder.json` | `attribute` | `equipped` | `minecraft:movement_speed` / `add_multiplied_base` / `{value:0.5}`, `conditions:[{"condition":"trimeffectively:facing_respawn"}]` | the condition is recomputed on the engine tick |
| `raiser.json` | `breed_cooldown` | `breed` | `amount {value:-0.20, stack:"per_piece"}` | cooldown 6000 → 4800 ticks |
| `shaper.json` | `item_property` | always on | `properties {keep_repair_cost: true}` | stamped onto the item, the anvil penalty no longer accumulates |
| `host.json` | `trade_keep` | `trade` | `chance {value:0.2, stack:"per_piece"}` | 20% chance not to consume a trade use |
| `ward.json` | `damage_type_override` + `annotate_sound` | `attack_out` / `sound_emitted` | ①`damage_type:"minecraft:sonic_boom"`, `keep_attacker:true` ②`radius {value:8}`, `client:true`, `exclude_own:true`, `blocked_by:"minecraft:wool"`, `particle:"minecraft:vibration"`, `label` see §7 | the sound annotation renders only on this client |
| `silence.json` | `mute_sound` | `sound_emitted` | `chance {value:0.3, stack:"per_piece"}`, `scope:"both"` | others cannot hear it either, and the vibration system cannot detect it |
| `tide.json` | `harmful_effect_rate` | `equipped` | `amount {value:2.0, stack:"per_piece"}` | +200% countdown rate |
| `snout.json` | `attribute` | `equipped` | `trimeffectively:crit_multiplier` / `add_value` / `{value:0.01, per:["gold_group_count"], stack:"per_piece"}` | +1% crit multiplier per "group" of gold items; the attribute's default is 1.5 |
| `rib.json` | `damage_out` | `attack_out` | `amount {value:0.20, per:["harmful_effect_count"], stack:"per_piece"}` | +20% per harmful effect, being on fire counts as one |
| `eye.json` | `apply_mob_effect` | `attack_out` | `effect:"trimeffectively:endphase"`, `chance {value:0.1, stack:"per_piece"}`, `duration {value:28.284271247461902, per:["damage_dealt"], power:0.5, stack:"per_piece", min:0}`, `amplifier {value:0, stack:"once"}`, `target:"victim"` | per-piece duration = √(2×damage) seconds, then × pieces, no cap; chance stacks per piece |
| `spire.json` | `damage_in` | `take_damage_before` | `amount {value:-0.01, per:["speed_mps"], stack:"per_piece"}` | -1% per 1 m/s per piece; when the fraction passes -1.0 the result is clamped to 0 |
| `bolt.json` | `attribute` | `equipped` | `minecraft:knockback_resistance` / `add_value` / `{value:0.15}` | internally 0.15 = the tooltip shows +1.5 |
| `flow.json` | `extra_jumps` / attribute `trimeffectively:extra_jumps` | `equipped` | `count {value:1, stack:"per_piece"}` | extra jumps |

### A.2 Materials (11)

| File | `type` | `trigger` | Key numbers and fields | Notes |
|---|---|---|---|---|
| `amethyst.json` | `attribute` | `equipped` | `minecraft:movement_speed` / `add_multiplied_base` / `{value:1.0, per:["missing_health_ratio"], stack:"per_piece"}` | 4 pieces near death approach +400% (500% speed) |
| `copper.json` | `attribute` | `equipped` | `minecraft:armor_toughness` / `add_value` / `{value:2}` (stack omitted = `per_piece`) | two copper pieces → +4 |
| `diamond.json` | `item_property` | (none) | `properties:{durability_multiplier:2}` | `scope: item`, stamped into the components (before 0.1.0.31 this number was wrongly decoded as the boolean `true`, so the multiplier was always 1.0) |
| `emerald.json` | `trade_price` | `trade` | `amount {value:-0.10, stack:"per_piece"}` | price -10% |
| `gold.json` | `item_property` + `piglin_neutral` | (none) / `equipped` | ①`properties:{piglin_loved:true}` ②`also_brutes:true` | if the gear is already gold, even brutes stay neutral |
| `iron.json` | `attribute` | `equipped` | `minecraft:armor` / `add_value` / `{value:0.5, per:["gear.armor"], stack:"once"}` | **the only `stack: once` exception**: `gear.armor` is already "the sum of the armour on pieces with an iron trim", so multiplying by the piece count again would square it |
| `lapis.json` | `attribute` | `equipped` | `minecraft:luck` / `add_value` / `{value:1}` | |
| `quartz.json` | `thorns` | `take_damage_after` | `amount {value:0.5, stack:"per_piece"}` | melee only: `conditions` uses the tag predicate `{"tags":[{"id":"minecraft:is_projectile","expected":false}]}` (1.21 has no `is_projectile` boolean field) |
| `netherite.json` | `damage_in` + `attribute` + `item_property` | `take_damage_before` / (none) / (none) | ①`{-0.16}` + fire tag condition ②`minecraft:burning_time`'s `add_multiplied_base` `{-0.30}` ③`properties:{fire_resistant:true}` | see the full sample in §12.4 |
| `redstone.json` | `attribute` ×2 | `equipped` | ①`minecraft:attack_speed` / `add_value` / `{value:1}` ②`minecraft:block_break_speed` / `add_value` / `{value:1}` | |
| `resin.json` | `xp_gain` | `xp_gain` | `amount {value:1.0, stack:"per_piece"}` | +100% |

### A.3 Conventions across the whole table

1. Every `Value`'s `stack` is `per_piece`, **except for the one exception in `iron.json`** (reason in the table above).
2. This version writes no `min` / `max` at all (`eye.json`'s `min: 0` is a hard floor against negative damage, not a balance clamp). To tune, add something like `"max": -0.9` on the specific entry; do not change the engine.
3. `rib`'s "damage +20% per harmful effect (fire included)" — being on fire counts as one, so `harmful_effect_count` includes "currently on fire", not only `MobEffect`s.
4. `gold`'s `also_brutes` is a boolean switch; "whether the gear is already gold" is a runtime judgement, not data; `also_brutes:false` means piglins only stay neutral.
5. `damage_in` reproduces the two gates of resistance: damage flagged `#minecraft:bypasses_effects` and `#minecraft:bypasses_resistance` is **completely unaffected by trims** (otherwise full damage reduction would swallow the void and `/kill` as well).
6. Spire's damage-reduction fraction can pass `-1.0`; the result is clamped to 0 at the end, so ≥100% means "this hit does no damage at all". `speed_mps` takes the peak over the last 5 ticks; the vertical component is not counted while standing.
