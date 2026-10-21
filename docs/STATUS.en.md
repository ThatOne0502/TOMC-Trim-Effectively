[简体中文](STATUS.md) &nbsp;·&nbsp; **English**

# Status: TOMC-Trim Effectively 0.1.0.53

> This file is a change log: it records only what changed between versions. For the structural description see [README](README.index.en.md) and the other documents.

(Notes for 0.1.0.26 ~ 0.1.0.31 have been folded into this table; earlier version history lives in release notes outside git. 0.1.0.32/33 dealt with "re-trimming does not refresh the effects" — the component now remembers the trim identity, and changing the trim forces a recompute; 0.1.0.34 gave the sculk annotation a backing plate and sculk-cyan text colour; 0.1.0.35 added the template duplication recipe hijack; 0.1.0.36 moved configuration into a Mod Menu + Cloth Config screen and added a read-only server → client sync; 0.1.0.37 corrected the read-only rule — the main menu can edit too, only connecting to someone else's server greys it out; 0.1.0.40 added the three smithing-table recipes, the trim-disable toggle and the air-jump wind-burst effect; 0.1.0.44 fixed wild's nested loot tables; 0.1.0.45 settled Endphase's three delivery routes: potion 20/40/10 seconds, tipped arrows keep vanilla's 1/8, stew fixed at 5 seconds; 0.1.0.46 reverted the tipped-arrow duration interception and changed the eye trim's duration to "damage × pieces"; 0.1.0.47 changed that duration to a square-root curve (√(2×damage) seconds per piece) and gave `Value` an optional `power` field; 0.1.0.49 did pre-release cleanup — deleted the never-wired `anvil_penalty`, every experimental probe log and dead code, turned the hard-coded JDK path in `gradle.properties` into a comment, and filled in README / LICENSE / .gitattributes; 0.1.0.50 corrected the data directory contract the docs had wrong — data lives in `data/<pack>/trimeffectively/tre_{pattern,material}/`, the entry id is `<pack>:<filename>`, and which vanilla trim an entry binds to is decided by the JSON's `pattern`/`material` fields; 0.1.0.52 split the documentation by reader and rewrote the store-page copy; 0.1.0.53 fixed the public name as `[TRE] 纹以载道 Trim Effectively`, turned the store copy into three copies of one text, and gave every detailed document an English version.)

## What changed in this build

### 1. Materials: diamond's durability multiplier was always 1 (a decode-order problem)

The `Literal` in `data/EffectEntry.java` (the union type for `item_property`'s `properties` values) used to try **boolean → number → string** in that order. But the default implementation of `DynamicOps#getBooleanValue` is "if you can get a number, treat it as non-zero" — internally it is just `getNumberValue(input).map(n -> n.byteValue() != 0)`. So `"durability_multiplier": 2` in JSON was picked up by `Codec.BOOL` and became `true`, then `number()` and `asNumber()` yielded `1.0`, pinning the multiplier at 1. What a player sees from `data get` as `durability_multiplier: 1b` is exactly this `BoolTag(true)` (SNBT writes booleans as `1b`); `durability_multiplier: 1b` and `durability_multiplier: true` are the same thing.

The order is now **number → boolean → string** (`Codec.DOUBLE` is tried first). A real boolean is not caught by `getNumberValue`, so `fire_resistant: true` and the like still decode as booleans.

Note: **old items that have already been stamped are not corrected automatically** — `TrimStamp.reconcile` only stamps when the component is missing and never recomputes an existing one. To get an old chestplate from `true` back to the number 2, re-apply the trim, or run a forced recompute in game (`/tre reload` forces a re-stamp). Newly trimmed items are correct straight away.

1184 = 592 × 2 is the number to expect after the fix (`ItemStackEffectsMixin` multiplies at the `RETURN` of `ItemStack#getMaxDamage` by `floor(base value × multiplier)`, with a floor of 1).

### 2. Materials: netherite's burn duration now goes through vanilla `minecraft:burning_time`

Vanilla already has this attribute (`Attributes.BURNING_TIME`, default 1.0, range 0~1024, `Sentiment.NEGATIVE`, registered for all mobs by `LivingEntity.createLivingAttributes()`), and fire protection relies on it: `data/minecraft/enchantment/fire_protection.json` uses `operation: add_multiplied_base` with `-0.15` per level (level 4 = -0.6).

The only read point is `LivingEntity#igniteForTicks(int)`:

```java
super.igniteForTicks(Mth.ceil(ticks * getAttributeValue(Attributes.BURNING_TIME)));
```

The multiplication happens **before** the fire-ticks field is written, so changing the attribute is enough; no injection is needed.

So this build retires `trimeffectively:fire_duration` (type 22) together with the `ignite` trigger, and the entry in `netherite.json` becomes:

```json
{ "type": "trimeffectively:attribute",
  "attribute": "minecraft:burning_time",
  "operation": "add_multiplied_base",
  "amount": { "value": -0.30, "stack": "per_piece" } }
```

-30% per piece, -120% for four pieces → the attribute value is clamped to its floor of 0 → it never ignites at all, matching the cap behaviour of the old home-grown type, but now it goes through the same multiplicative channel as fire protection, stacks with it, and we no longer fight vanilla over the same number from outside. `effect/FireDurationEffect.java` is deleted, the `setRemainingFireTicks` injection in `mixin/EntityMixin.java` is deleted (the class stays, it still does the `SpeedTracker` sampling), and `DamageEffects` loses `TYPE_FIRE_DURATION` / `TRIGGER_IGNITE`.

### 3. Old effects do not disappear after re-trimming (the component does not know the trim identity)

0.1.0.32/33. When the same item is smithed repeatedly, vanilla **overwrites `minecraft:trim` in place** — pattern and material change, but it is still the same component. Our sweep used to look only at "is there a trim component" and "is there a stamp", and treated both being present as in sync, so a diamond-doubled-durability item reforged to quartz kept `durability_multiplier: 2` on it.

The fix is not "delete then regenerate on take" (that only covers the smithing table path; other mods changing the trim would still slip through), but **making the stamp remember which trim it was stamped for**: the contents of `trimeffectively:trim_properties` change from a bare map to `{pattern, material, values}`, where the two ids are the `minecraft:trim` read at stamp time. The sweep compares the current trim's two ids against the stamp's two ids and, **on any mismatch, discards the whole thing and recomputes from the datapack** — no merging. Old values mean nothing for a new trim.

- The comparison is two `Identifier` equality checks with no datapack lookup, so doing it on every equipment read costs nothing.
- Trim is `null` (no trim) → delete the component; component is `null` (trim but no stamp) → stamp it. These two directions are the same invariant as before.
- Either id unresolvable (the Holder is not held by a registry) → leave it alone, otherwise every read would recompute.
- Components written by older versions have neither id → identity decodes as empty → judged "not provably in sync" → recomputed from the datapack on the next read. Old items therefore **heal themselves**, with no `/tre reload` needed (this is exactly the patch for the 0.1.0.31 note "old items are not corrected automatically").
- Datapack changes are still not retroactive: swapping the datapack does not change the trim identity, so the stamp stays old. Only changing the trim forces a recompute.

Verification used a temporary probe (since deleted), `runServer` log: encoded `{material:"trimeffectively:diamond",pattern:"trimeffectively:sentry",values:{durability_multiplier:2.0d}}`; reading back, identity and values were all restored and `2.0` was still a double; the old shape `{durability_multiplier:2}` decoded to `pattern=null, material=null, values={}`, i.e. it triggers a recompute.

### 4. Spire: straight percentage reduction, no cap

The previous build wired spire into vanilla's protection points (`EnchantmentHelper#getDamageProtection`), where vanilla pins the ceiling at 20 points = 80%. This build **tears out the whole protection-point channel**: `effect/DamageProtectionEffect.java`, `mixin/EnchantmentHelperMixin.java` and the `trimeffectively:damage_protection` type are all deleted (type table 23 → 22).

Spire now uses the existing `damage_in`:

```json
{ "type": "trimeffectively:damage_in",
  "trigger": "trimeffectively:take_damage_before",
  "amount": { "value": -0.01, "per": ["speed_mps"], "stack": "per_piece" } }
```

1% less per piece per 1 m/s, which can keep adding up past 100% (at that point the result is clamped to 0 and the hit does no damage at all).

Vanilla's resistance implementation (`LivingEntity#getDamageAfterMagicAbsorb` bytecode):

```
if (source.is(BYPASSES_EFFECTS)) return amount;
if (hasEffect(RESISTANCE) && !source.is(BYPASSES_RESISTANCE)) {
    int i = (amplifier + 1) * 5;
    amount = Math.max(amount * (25 - i) / 25.0f, 0.0f);   // clamp to 0
}
if (amount <= 0) return 0;
if (source.is(BYPASSES_ENCHANTMENTS)) return amount;
return CombatRules.getDamageAfterMagicAbsorb(amount, EnchantmentHelper.getDamageProtection(...));
```

That is, resistance is itself "a multiplicative percentage + clamp to 0", with a ceiling of `level 24 → 0`, not vanilla enchantment protection's 20-point curve. We moved to the same shape, so:

- A multiplicative reduction and armour **can be reordered** (the product is unchanged); our hook sits at the head of `hurtServer`, before armour, and the result matches resistance settling after armour.
- Clamp-to-0 comes for free from the last line of `DamageEffects.compute`, `scaled <= 0 ? 0`.
- `DamageInEffect` gains the two gates resistance has: damage from `#minecraft:bypasses_effects` and `#minecraft:bypasses_resistance` is completely unaffected by trims. **This step is required** — our layer sits earlier than vanilla resistance, and without this check a maxed-out reduction would eat the void and `/kill` as well.

`speed_mps` comes from the entity's `Motion` (`SpeedTracker` records `getDeltaMovement()` every tick and takes the peak over the last 5 ticks ×20 to get m/s): collision zeroes velocity before damage settles, so an instantaneous value would always read as standing still; and the vertical speed on the landing frame only survives in a window. The vertical component is ignored while standing (otherwise a cancelled gravity step would book ≈1.6 m/s for a standing player).

### 5. Piglin brute: cut off both ends of the aggression

The previous build returned `null` from `AbstractPiglin#getTarget`, and the "raise axe—lower axe" loop was still there. The reason is that this exit is not on the aggression path at all: a brute's target is the brain's `ATTACK_TARGET` memory, **the end that writes it** is `StartAttacking` in the idle activity (fed by `PiglinBruteAi#findNearestValidAttackTarget`), **the end that clears it** is `StopAttackingIfTargetInvalid` in the fight activity (judged by `PiglinBruteAi#isNearestValidAttackTarget`), and when the fight activity stops it wipes the memory once more via `addActivityAndRemoveMemoryWhenStopped`.

The previous build only made the "clear" end say "that target is gone", so:

1. `StopAttackingIfTargetInvalid` wipes the memory → arms drop;
2. the idle activity's `StartAttacking` uses a different function (looking at `ANGRY_AT` / `NEAREST_VISIBLE_ATTACKABLE_PLAYER` / `NEAREST_VISIBLE_NEMESIS`) and picks the player again the next tick → arms raise;
3. back to 1.

`setAggressive(brain.hasMemoryValue(ATTACK_TARGET))` flips every tick with this memory, so what you see is arms going up and down every tick.

Now `mixin/PiglinBruteAiMixin.java` cuts both ends at once: on the `RETURN` of `findNearestValidAttackTarget` a neutralised player becomes `Optional.empty()` (the memory can never be written), and on the `HEAD` of `isNearestValidAttackTarget` it returns `false` (whatever was already written gets cleared). The memory never exists → the brute stays in its idle activity with the default arm pose, exactly as it looks to a player it has never been hostile to.

`mixin/AbstractPiglinTargetMixin.java` has been deleted: it was both the cause of this loop and no longer has any reason to exist.

### 6. `SmithingMenuMixin` was never registered

`mixin/SmithingMenuMixin.java` was always in the source tree and always compiled, but it was missing from the list in `trimeffectively.mixins.json`, so it never took effect (the whole "stamp on take from the smithing table" design in fact never ran). This build adds it to the list, and `javap` was used to confirm the `protected void` signature of `SmithingMenu#onTake(Player, ItemStack)`.

### 7. On-player "after hurt" effects: `Player#actuallyHurt` does not call super

`Player#actuallyHurt(ServerLevel, DamageSource, float)` is a full rewrite: armour reduction, enchantment reduction, absorption and the health subtraction are all redone by hand, and it **never `invokespecial LivingEntity.actuallyHurt`**. And `actuallyHurt` is dispatched virtually, so an injection on `LivingEntity#actuallyHurt` never executes once for a player.

Affected: vex summoning, thorns retaliation, Endphase accumulation and damage-source replacement, and on-hit poison applied to the target. A new `mixin/PlayerActuallyHurtMixin.java` (`@Mixin(Player.class)`) mirrors the same five hooks. Virtual dispatch guarantees exactly one of the two runs per hit.

Damage effects (`damage_out` / `damage_in` / `crit_damage` / `damage_type_override`) are unaffected: `Player#hurtServer` goes through `invokespecial Avatar.hurtServer` and ends up in `LivingEntity#hurtServer`, where that injection is live.

### 8. Multiple effects in the same file were swallowed by deduplication

`TrimEffects.collect` used to key deduplication on `entryId` (**the file id**), and multiple effects in one file shared the same id, so only the first effect in each file took effect (wild's two, the nether's several, and so on). Changed to `entryId#index`.

### 9. Wild: fishing and containers

- Fishing: in the `LootParams` that `FishingHook#retrieve` builds, `THIS_ENTITY` is **the hook itself** (`aload_0`), not the angler. `LootRollEffect` therefore got neither a `LivingEntity` nor a container player, and the whole path was silently dropped — that is why "fishing gives you just one fish". Now, when a `FishingHook` is recognised, `getPlayerOwner()` is used as the beneficiary.
- Containers: `RandomizableContainer#unpackLootTable(Player)` does receive the player, but the loot params it builds contain no player. `mixin/ContainerLootMixin.java` stashes the player in a thread-local at method head and clears it at the tail, so rolls for chests / barrels / shulker boxes / hopper minecarts can be attributed. Extra items go through the container's own insert callback, and whatever does not fit drops at the container position per vanilla rules.

### 10. Sculk annotations

- Subtitles no longer come from concatenating a language key (`subtitles.<path>`); instead the subtitle declared in `sounds.json` is read: the `WeighedSoundEvents` returned by `SoundManager#getSoundEvent(id)` has `getSubtitle()`. The language key for sounds like footsteps **cannot be derived from the sound id**; many ids share one subtitle.
- Sounds the player makes themselves are no longer annotated: the test moved from **eyes** to **feet**, because vanilla puts the player's own sound source at `player.position()` (the centre of the block they stand on), a full eye-height away. That offset is exactly what let footsteps and swings slip through.
- The font scale grows with distance (0.8~2.4×), and each annotation carries a stable screen offset derived from itself, so multiple sources at the same position do not fully overlap.
- The text is sculk cyan `#00FFFF` with a translucent dark backing plate (including a 1 px cyan outline); plate and text fade together. Previously plain white text smeared together on dark stone / basalt. The plate uses `GuiGraphics#fill` and shares the same scale matrix as the text, so the plate grows with the font.
- It still **spawns no entities**: annotations are drawn straight to the HUD, and fading/occlusion is handled locally on the client.
- The listening radius already went through `Value.eval(pieces, …)`, and `stack` already defaults to `per_piece`, so one piece is 8 blocks, two 16, four 32 — **that is already a linear stack per piece** and needed no change. Only the numeric constant (8 blocks) does not scale with pieces; coefficients like "per 1 block" are the term that scales with pieces, consistent with the rest of the design.

### 11. Guide: event-driven, no more per-tick polling

`engine/PortalEntry.java` + `engine/PortalTracker.java`: the Fabric attachment API is used to store "the coordinates at which the current dimension was entered" on the player (saved with the player, copied on death).

Dimension changes listen to `ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD` — the event **does** exist, so no polling is needed. But when the event fires the destination dimension is still being assembled, so the coordinates read at that moment are not necessarily where the player lands; the record is therefore deferred by one server tick (flushed in `TreEngine`'s `END_SERVER_TICK`, a no-op when `PENDING` is empty), by which time the coordinates are fixed. First join (`ServerPlayConnectionEvents.JOIN`) only writes when there is no record yet, and later logins in the same dimension do not overwrite it, which matches "each dimension entry overwrites the previous one".

Decision order: same-dimension respawn point (bed / respawn anchor) → the recorded entry position → the End altar / that dimension's spawn point.

### 12. Eye

- Re-applying no longer shortens an existing duration: the longer remaining time and the higher amplifier are kept.
- `WitherBoss` and `EnderDragon` each have a mixin to let Endphase through (`WitherBoss#canBeAffected`, `EnderDragon#addEffect`). **Both of these really need a wither / ender dragon fight to verify.**
- 1.21.11 has no `EntityBoundSound`, and `SoundInstance` has no `getLocation()`: the sound id has to come from `AbstractSoundInstance#getIdentifier()`.

### 13. Template duplication recipe hijack

The 18 vanilla trim templates' duplication recipes are all written the same way: 3×3, `#S#`/`#C#`/`###`, the seven `#` are diamonds, `S` is the template being duplicated, `C` is that pattern's own material, output 2 `S`. Seven diamonds for two templates is expensive, so this build rewrites them at recipe load time to `MSM`/`MDM`/`MMM`: `M` becomes the material that was in the middle (seven of them), `D` becomes the seven diamonds that were around it (one), `S` stays where it was, output unchanged.

- Identification is **shape-based, not a whitelist**: it must be a `ShapedRecipe` and 3×3; slots `{0,2,3,5,6,7,8}` must all be the same `Ingredient`; slot 4 must differ from it; slot 1's `Ingredient` must accept the output (vanilla has no `trim_templates` item tag, and `trim_materials`/`trimmable_armor` are of no use here, so "is it accepted by the template slot" is what identifies the template); the output count must be 2. If any condition fails, the recipe is put back untouched.
- A rewritten recipe **keeps the original recipe id**, so recipe-book entries and server-side advancement unlocks are unaffected.
- The injection point is `RecipeManager#apply(RecipeMap, ResourceManager, ProfilerFiller)` — both the first load and `/reload` pass through this one funnel, and the rewritten map is both the table that is saved and the copy synced to clients.
- Server authority here is structural: client recipes come from `ClientPacketListener#handleUpdateRecipes`, which never passes through `RecipeManager#apply`, so a pure client never reads this config and never rewrites recipes on its own. The option `hijack_trim_duplication_recipes` (default `true`) lives in `config/trimeffectively.json` and is re-read on every recipe reload; a missing file is written with defaults, missing keys are filled in, and a corrupt file logs one warning and falls back to `true`.
- Measured (`runServer`): `[tre] recipe hijack: 19 of 1470 recipes rewritten`. 19 rather than 18 because `minecraft:netherite_upgrade_smithing_template`'s duplication recipe has the same shape and was rewritten along with them — it is not a trim template, but it is the same expensive "seven diamonds for two templates" recipe.

### 14. Config screen: Mod Menu + Cloth Config (both soft dependencies)

`modmenu` 17.0.1 and `cloth-config-fabric` 21.11.160 are not written into `depends`. Cloth uses `modImplementation` (present in the dev environment, which makes self-testing easier), modmenu uses `modCompileOnly` (only to get the `ModMenuApi` signatures); `fabric.mod.json` declares them under `suggests`, and `entrypoints.modmenu` points at `TreMenuIntegration`. When neither mod is present this entrypoint is never instantiated and the game starts as usual.

The screen is handed to Cloth's `ConfigBuilder#setGlobalized(true)`, which swaps in `GlobalizedClothConfigScreen`: search bar on top, category index on the left, a single scrolling page, sectioned subheadings — the same skeleton as the sibling project Creefern. Even with a single option it is built to that standard; adding an entry is one more row in the same category. The tooltip is the two lines of the requirement text verbatim, split on `\n` into a `Component` array handed to Cloth (each argument renders as one line).

Read-only and sync:

- On player `JOIN` the server packs the current values of `config/trimeffectively.json` into `TreConfigPayload` (S2C) and sends it to that player; after a successful `/reload` it broadcasts once more. The channel is one-way — the client has no write-back entry point and cannot request one either.
- The client only caches, never writes to disk. The read-only test is `Minecraft#level != null && !hasSingleplayerServer()` — **only "connected to someone else's server" greys it out**; the main menu has no world and no host, and what you change there is the local file, exactly the defaults a player wants to set before opening a world, so it is editable; singleplayer / LAN-hosted is editable too. The header note changed from `scope.local` to `scope.server`.
- After a host saves, besides writing the file it also runs `reloadResources(selected datapacks)` once, so recipes are rebuilt with the new config immediately and no manual `/reload` is needed; on a server you cannot change it, so there is no such step.

No client/server branch was written for "server authority" — the hijack only happens in `RecipeManager#apply`, which only runs on a dedicated server or a host anyway.

### 15. The three smithing-table recipes: strip, strip-and-recover, disable

All three recipes are in `RecipeType.SMITHING`, all implement `SmithingRecipe`, and all are registered as `PlacementInfo.NOT_PLACEABLE` + `isSpecial()` (so the recipe book and JEI do not show them). The data file is a single line `{"type": "trimeffectively:..."}` — the inputs are fixed, so the codec is a unit codec and the recipe instance is constructed when decoding.

| Recipe | Template slot | Base slot | Addition slot | Result |
| --- | --- | --- | --- | --- |
| `trim_strip` | iron nugget | any trimmable armour carrying a trim | empty | removes the trim (template not returned) |
| `trim_strip_recover` | iron nugget | same as above | book and quill | removes the trim and returns the template item that trim corresponds to |
| `trim_toggle` | glass pane | same as above | empty | keeps the trim, flips the effect toggle |

- "Base slot = any trimmable armour carrying a trim" is two layers of test: the `Ingredient` comes from the `minecraft:trimmable_armor` item tag (a slot predicate, automatically entering `RecipePropertySet.SMITHING_BASE`), and `matches` additionally requires `DataComponents.TRIM` to be present. **Tags cannot be read at decode time**: recipe reload happens before item tags are bound, and at that point `BuiltInRegistries.ITEM.getOrThrow(TagKey)` throws `Missing tag TagKey[minecraft:item / minecraft:trimmable_armor]` and the whole datapack fails to load. So the base slot's `Ingredient` is lazily resolved and memoised; the first use is at `RecipeManager#apply` (when tags are in place), what is memoised is the tag's live `HolderSet` so it still follows datapack changes, and a missing tag degrades to "matches no item" — it stops working without crashing.
- Returning the template does not go through the recipe result (the smithing table result is a single stack); it is emitted in the `HEAD` of `SmithingMenuMixin#onTake`, because at that point the three input slots are still full, whereas `onTake` clears them after taking the result. The decision can only be made from the slots — `SmithingMenu` does not keep the matched recipe object. **1.21.11's `TrimPattern` has no `templateItem` field** (only `assetId`/`description`/`decal`), so the template item is found by scanning `SmithingTrimRecipe` for the one with the same `pattern` and taking the first item of its template slot (`SmithingTrimRecipeAccessor` reads the package-private field with `@Accessor("pattern")`). Datapack-defined patterns therefore return correctly too.
- Disabling a trim is the item component `trimeffectively:tre_enabled` (not written when `true`, i.e. the default is `true`). It takes effect in exactly two places: `TrimSnapshot#of` `continue`s immediately after `reconcile` (piece count, attributes and variables all go to zero at once, with not one line of `DamageEffects`/`AttributeEngine`/annotation changed), and `TrimStamp#expected` returns "empty values + identity" when it is off. **Clearing the stamp component is not enough** — the effect piece count is counted from the trim ids on `minecraft:trim`, independently of the stamp. In the off state the tooltip replaces the collapsed hint with `该物品的纹饰已被无效化` and forces it not to expand.
- Turning it back on **recomputes** from the current datapack instead of restoring old values (`TrimStamp.force`), so numbers changed in the datapack while it was off do take effect.

### 16. Air-jump effect

The one place where multi-jump actually takes effect (`ExtraJumps#aiStep`, after `setDeltaMovement` and `hurtMarked` are touched) plays `SoundEvents.WIND_CHARGE_BURST` and `ParticleTypes.GUST_EMITTER_SMALL` on the server. On the server because the jump authorisation already lives there: the client is only told the new velocity and should not decide "there was a wind burst". The class `net.minecraft.world.entity.projectile.windcharge.WindCharge` **does not exist** in 1.21.11, so the sound/particle names were checked one by one from `SoundEvents`/`ParticleTypes` rather than copied from the wind charge class.

### 17. Endphase: dropping the execution blow, quantising to whole seconds (0.1.0.42)

Two things: one deletion, one change.

**Delete: the execution line is commented out.** Previously, once dissociation (in seconds) ≥ the base max health, one extra `hurtServer(..., getMaxHealth() + 1000)` was added, to make sure the death message went through `endphase`. That safety net is now redundant: the damage hook already rewrites the type of **any hit this victim takes**, and an ordinary lethal hit produces the correct death message by itself. The net was also miscalibrated — `MAX_HEALTH` is `RangedAttribute("attribute.name.max_health", 20.0, 1.0, 1024.0)` with a floor of 1.0, so anyone past the threshold is already at "any hit kills"; the scripted blow is no more correct than natural death. The code is kept as a block comment in `EndphaseEffect#applyEffectTick`, together with the reasoning.

**Change: the max-health penalty moves from "fractional seconds" to "whole seconds".** The deduction used to be `-1 * seconds * (amplifier+1)`, and since `seconds = duration/20` changes every tick, `addOrReplacePermanentModifier` rewrote the `trimeffectively:endphase_max_health` modifier every tick — twenty heart-row relayouts a second, enough to make the health bar look like it is shaking. Now it is `-Math.floor(seconds) * (amplifier+1)`: it changes once a second, lands on whole-heart boundaries, and keeps the early return of "if the value did not change, do not touch the modifier".

`MobEffectInstance` has no `setDuration` (1.21.11 only has `update`), and `LivingEntity#forceAddEffect` reuses the same instance and updates it in place, so the flicker is not caused by "the instance being replaced" — that possibility can be ruled out.

### 18. Wild's "9 fish": loot tables nest (0.1.0.43)

A full set (4 pieces) of wild fishing should roll 4 extra times, 5 fish in total; measured 9. The diagnostic log (`[tre] extra_loot_roll caller:`) produced two stack traces showing that it is not the same table being called twice, but **two different tables in a single retrieval**:

```
at LootTable.getRandomItemsRaw(LootContext, Consumer)      ← outer gameplay/fishing
  at LootPool.method_341 → LootPool.method_345
  at NestedLootTable.method_433                            ← nested table entry
  at LootTable.getRandomItemsRaw(LootParams, Consumer)     ← enters again here
  at LootTable.getRandomItemsRaw(LootContext, Consumer)    ← the same hook fires a second time
```

Mapped: `class_52` = `LootTable`, `class_55` = `LootPool`, `class_83` = `NestedLootTable`, `class_85` = `LootPoolSingletonContainer`. In other words `minecraft:gameplay/fishing` has a `minecraft:nested` pool entry pointing at `gameplay/fishing/fish`, and the nesting re-enters through the `LootParams` overload and back into the `LootContext` overload — so the single hook we have fires once per **table layer**. 9 = 1 (vanilla) + 4 (nested table) + 4 (outer table).

The first fix (0.1.0.43) added a depth test to the permission: a `ThreadLocal<Integer> DEPTH` in `LootRollEffect`, incremented/decremented in `onRoll`, rolling extra dice only at the outermost layer (depth 0). **That fix did not work**; it still measured double: the `@Inject(at = HEAD)` callback returns before the method body (which runs the pools and hence enters the nested table), so `finally` has already reset depth to 0, and the nested call reads 0 again and is let through as before.

The fix that actually works (0.1.0.44) turns "depth" into something wrapping the whole method body: `mixin/LootTableEffectsMixin.java` switches to MixinExtras' `@WrapMethod`, calling `enter()` **before** `original.call(context, output)` and `exit()` in `finally`, and only calling `LootRollEffect.onRoll` when `enter()` returned true (the outermost layer). The criterion is still "one loot generation" rather than "one method call" — nesting is a table's normal way of composing, not an error; the difference is that this guard actually covers the interval in which nesting happens.

One old comment corrected along the way: `getRandomItemsRaw(LootParams, Consumer)` is not dead code — nested table entries enter through it, it just then forwards to the `LootContext` overload.

### 19. Endphase's three delivery routes (0.1.0.45)

By design Endphase can be delivered to a target through potions, tipped arrows and suspicious stew; previously only the potion existed, the other two were never wired up. Now all three exist, each with a **fixed** duration:

| Route | Source | Duration |
| --- | --- | --- |
| Potion (normal) | Brewing: awkward potion + dragon head | 20 s (400 tick) |
| Potion (extended) | normal + redstone | 40 s (800 tick) |
| Potion (strong) | normal + glowstone dust, amplifier I | 10 s (200 tick, deduction speed ×2) |
| Tipped arrow | lingering potion + arrow (vanilla `tipped_arrow`) | 1/8 of the potion: normal 2.5 s / extended 5 s / strong 1.25 s |
| Suspicious stew | bowl + brown mushroom + red mushroom + dragon head | fixed 5 s (100 tick) |

- The three potion tiers change the three constants in `EndphasePotions` (they were 200/300/100, i.e. 10/15/5 seconds). Dissociation is the remaining seconds, so "extended" buys a smaller max-health penalty and "strong" reaches zero faster: normal and strong both deduct 20 in total, only the pacing differs.
- Tipped arrows **keep vanilla behaviour**: `bipush 8` in the bytecode of `TippedArrowRecipe#assemble` is "arrow duration = potion / 8", which is vanilla's own design and is not intercepted. A `TippedArrowRecipeMixin` that pinned arrows to a fixed 5 seconds was written at one point and has been fully reverted as requested (0.1.0.46 deletes that mixin and the `APPLIED_TICKS` constant).
- Suspicious stew: 1.21.11 has **no** `SuspiciousStewRecipe` class; vanilla has one shapeless JSON per flower, with the effect written straight into the result's `minecraft:suspicious_stew_effects` component. So dragon-head stew is just a new JSON: `data/trimeffectively/recipe/suspicious_stew_from_dragon_head.json`, bowl + brown mushroom + red mushroom + dragon head, `duration: 100`, `id: trimeffectively:endphase`. No code was added.
- Side effect to be aware of: any potion tier pins max health to 1 the moment it lands (20 s × 1 point per second = 20 points), which is the intended design given how expensive the dragon-head recipe is. The same holds for the strong tier (10 s × 2 points per second).

### 20. Eye trim duration: square root of damage × pieces (0.1.0.46 → 0.1.0.47)

`eye.json`'s `duration` changes from "fixed 200 tick (10 s)" to scaling with the actual damage of that hit and with the piece count, on a **square-root** curve:

```json
"duration": { "value": 28.284271247461902, "per": ["damage_dealt"], "power": 0.5, "stack": "per_piece", "min": 0 }
```

- **Duration per piece = √(2 × damage) seconds**, then multiplied by the piece count. `duration` is in ticks, so `value` is `20 × √2 = 28.284…`
  (20 tick = 1 s), `power: 0.5` takes the square root of the `per` product, and `stack: per_piece` multiplies by the piece count.
- What the numbers feel like: 6 damage on 1 piece √12 ≈ 3.5 s, 4 pieces ≈ 13.9 s; 20 damage on 4 pieces ≈ 25.3 s; 50 damage on 4 pieces = 40 s.
  Low damage short, high damage long, but the growth is held down by the square root, so there is no longer the linear blow-up of "one crit and you are dead". **No cap**:
  when the damage is already large enough you do not need this effect to kill, and non-player targets (bosses) also have more than 20 health.
- For this the engine gives `Value` an optional `power` field (default 1, `final = value × Π(per)^power × stackFactor`).
  It applies only to the product of the `per` terms; `value` and the piece factor stay linear; when the product is negative the power is skipped to avoid NaN. Scattered data is unaffected —
  every old JSON omits `power` and keeps its original semantics. There is still no expression language — the product just got one extra power.

### 21. Pre-release cleanup: moving out the things only the author could read (0.1.0.49)

This build changes no behaviour; it only tidies up so the repository is presentable.

**Dead code removed**

- `effect/AnvilPenaltyEffect.java`: the `anvil_penalty` type was never wired up. `shaper.json` had long since moved to
  `item_property`'s `keep_repair_cost`, and `AnvilMenuMixin` reads `TrimStamp.flag` directly,
  so even if this class were registered no data would reference it.
- `effect/DamageEffects.init()` (the "damage hooks armed" log) and its call in `Trimeffectively.onInitialize`.
- The two summon logs in `effect/SummonEffect`, the `LOGGED` dedup table and annotate log in `client/SoundAnnotations`,
  the `logged` gate and "tooltip line keys" log in `client/TrimTooltip`, and `TrimTooltip.translatableKey()`.
- The 19-entry recipe-name list in `engine/TreRecipes` (the log became a count), the attribute entries list and
  `loadedIds()` in `engine/TreEngine`, and `LootRollEffect`'s `PROBE` / `name()` / produced statistics.
- The commented-out execution block in `EndphaseEffect.applyEffectTick`: the rule "execute when dissociation (= remaining seconds) ≥ base max health"
  has long since been off any code path — once max health is pinned to 1, any damage kills, and the death cause is guaranteed by the damage-type rewrite.
  Five lines of explanatory comment are kept, the code itself is deleted, and the `baseMaxHealth()` that became dead with it is deleted too.

**One thing restored after being deleted by mistake**

`mixin/MobEffectInstanceAccessor.java` (`@Accessor("duration") void trimeffectively$setDuration(int)`) was judged an
orphan by the cleanup script: it is registered in `trimeffectively.mixins.json`, and the script therefore concluded "someone references it". The actual reference is
`effect/HarmfulEffectRate.java:61`. After deletion `compileJava` failed immediately; it has been restored (with the mixins.json entry).
Lesson: the basis for calling something unused must be **the compiler**, not a textual reference graph — classes registered only as mixins have no import in the source to begin with.

**Temporary artefacts and local state**

- Deleted `temp/merged` (50.9 MB, 9849 unpacked classes) and `temp/trimmable-tools-2.1.0-fabric-1.21.11.jar`
  (a reference build of a third-party mod, not suitable to carry in a public repository); the `temp/` directory is gone entirely.
- `org.gradle.java.home` in `fabric/gradle.properties` was an absolute path on this machine; it is now a comment with two explanatory lines;
  the equivalent is to set `JAVA_HOME` or pass `-Dorg.gradle.java.home=...` on each build. The reason for not hard-coding it: Gradle itself
  needs JDK 17+ to start, while the Java 21 compile target is provisioned by the toolchain, and the two should not be tied to the same field.
- New public-facing repository files: root `README.md` (mod description / install / build), `LICENSE` (full GPL-3.0 text),
  `.gitattributes` (LF, `*.bat` CRLF, binary markers), `docs/README.md` (documentation index),
  `.gitignore` extended to build output, `backup/`, `temp/`, `build-count.txt` (a local counter), while keeping the wrapper jar.
- `fabric/README.md` rewritten from scaffold text into an engineering note (build, output naming, code map, mojmap caveats).

**Smoke test**: `build` succeeded; `runServer` printed `TOMC-Trim Effectively loaded`,
`[tre] recipe hijack: 19 of 1474 recipes now take 1 diamond and 7 of the pattern's own material`,
`Loaded 1474 recipes`, `[tre] server started - tre_pattern=18 tre_material=11`, with no exceptions or warnings.

### 22. Correcting the data directory contract in the docs (0.1.0.50)

A reconciliation before going public found that the docs (and two javadocs) had always said "the datapack path is `data/minecraft/tre_pattern/<filename>.json` and the entry id is the vanilla trim id `minecraft:sentry`". That was an M1 design assumption; the implementation does not work that way.

- **The actual directory** is decided by the dynamic registry key: `trimeffectively:tre_pattern` → `data/<pack>/trimeffectively/tre_pattern/`,
  entry id = `<pack>:<filename>`. The mod's own 18 + 11 data files therefore live in
  `data/trimeffectively/trimeffectively/tre_{pattern,material}/`, with entry ids like `trimeffectively:sentry`.
  This was verified at runtime: looking up the registry by `Tre.id("eye")` finds the entry, `minecraft:eye` does not.
- **The binding is not in the filename** but in the JSON's `pattern` / `material` field (`"pattern": "minecraft:eye"`).
  `TrimEffect`'s javadoc always said this ("The file name is only a name"); the docs had not kept up.
- `appliesTo` does no existence check: a mistyped vanilla id does not error, it simply never matches; matching uses the trim id actually on the item.
- Files corrected: `docs/EFFECT_TYPES.md` §0 (the three sentences on directory / namespace / id rewritten, plus "how a third party adds its own data"),
  the paths in §4.1 §4.2, `docs/DESIGN.md` §2.2 (the heading changed from "entry id lines up directly with the vanilla trim id" to "entry id decoupled from
  which vanilla trim it serves", noting that §9-Q1's recommendation was overruled by the registry rule), the example comment in §3.1, the load line in
  `docs/SCHEMA.md` §5, and the javadocs of `Tre.java` / `data/TreRegistries.java` / `data/TrimEffect.java`.

### 23. Docs split into four by reader; release-page copy rewritten (0.1.0.52)

This build changes no runtime behaviour: the changes are all in the docs, plus one dead javadoc cross-reference.

**Why split**

The old `DESIGN.md` carried both "why it is designed this way" and "how to write JSON", and `SCHEMA.md` and `EFFECT_TYPES.md` were two field tables that referenced each other — writing one entry meant reading three documents first. It is now split by reader:

| File | Written for | Content came from |
|---|---|---|
| `docs/CONTENT.md` (154 lines) | players, modpack users | old DESIGN §5 and EFFECT_TYPES' assembly table |
| `docs/DATA.md` (468 lines) | pack and integration authors who write little or no Java | `SCHEMA.md` + `EFFECT_TYPES.md` merged in full |
| `docs/INTERNALS.md` (256 lines) | peers, developers who want to correct something | old DESIGN's engine narrative and pitfall table |
| `docs/DESIGN.md` (196 lines) | people who care about the trade-offs | the argument only, with links out to the implementation |
| `docs/STATUS.md` | people checking differences between versions | this table |

`SCHEMA.md` and `EFFECT_TYPES.md` are deleted, and the content was checked for loss: appendix A's 29 entries kept line by line, 4 complete samples, `Value`'s 5 fields and `stack`'s 4 values, 9 context variables, `item_property`'s 4 keys, the literal decode order and the third-party override recipe are all in `DATA.md`.

**Counts and facts corrected along the way** (the old docs were wrong; code and JSON now decide)

- `type` goes from "21" to **20**: the three retirements (`damage_protection`, `fire_duration`/`ignite`, `anvil_penalty`) were never reflected in the count. The 29 bundled data entries actually use **19**, and `crit_damage` currently has no entry using it — `snout` now goes through the attribute `trimeffectively:crit_multiplier`.
- `trigger` goes from "15" to **10**, all implemented; `death` / `tick` / `item_break` / `heal` are placeholder names that neither error nor fire when written.
- `per_piece_capped` **is a contract only, the engine does not implement it** (it falls back to `per_piece`); `DATA.md` now says so, and no longer lets pack authors believe a cap exists.
- `spire` is not "straight percentage reduction that bypasses vanilla's 80% cap" but `damage_in` reducing 1% per piece per 1 m/s of `speed_mps`.
- The client-side cyan annotation is `ward` (`damage_type_override` + `annotate_sound`); `silence` is `mute_sound`, 30% chance per piece to mute.
- Diamond durability is ×2 per piece; there is no cross-piece multiplication like "four pieces, four times".
- `DamageVars.forAttacker`'s javadoc referenced `EFFECT_TYPES.md` §5.1; it now points at `docs/DATA.md` §4.2.

**New release-facing front page**

- The root `README.md` answers only "what is this": logo, language navigation, AI and early-development statement, a positioning paragraph, effect table, installation, commands, documentation routing table, build entry point, licence and credits.
- `docs/README.en.md` is the English mirror; `docs/README.md` is the index of the five documents (reader table, reading order, maintenance conventions, three-sentence version).
- `PUBLISHING.md`: `## 【MC百科】` is all Chinese and `## 【CurseForge / Modrinth】` is all English, the two sections do not cross-link section by section; at the top is a release-metadata table (Chinese and English names, mod id, platform and version, runtime, dependencies, licence, author, repository URL to be filled in). The copy follows the style of existing MC百科 pages: bold tagline → two-paragraph overview → short bullets by theme, with no install steps and no implementation details.

**Verification**: `build` succeeded, output `tomc-trimeffectively-0.1.0.52-fabric+mc1.21.11.jar` (279 KB). Docs do not go into the jar; the build only confirms the skeleton and data files were not accidentally changed by this tidy-up.

### 24. The published name, three store-page copies, and a fully bilingual documentation set (0.1.0.53)

**The published name is separate from the actual mod name.** The public display name is **`[TRE] 纹以载道 Trim Effectively`**, used only on store pages; `fabric.mod.json` still says `TOMC-Trim Effectively`, and the in-game name, logs, jar name and data directory are untouched. The two need not and should not be synced — changing `fabric.mod.json` changes the jar's display name and makes existing worlds see a "new mod" in their mod list.

**`PUBLISHING.md` now holds three copies of the same text**: interleaved Chinese/English (general purpose), English only (CurseForge / Modrinth), Chinese only (MC百科 and other Chinese platforms). It used to be split by platform and then by language inside each platform, so the same paragraph had to be maintained two or more times in one file; the three copies now share a single skeleton — bold tagline → two overview paragraphs → 7 patterns → 6 materials → dragon head and `相位解离` → the three smithing recipes → misc → early-development notice → AI disclosure — so one editorial change is made in one place. The English one-liner is the `description` from `fabric.mod.json` verbatim; the Chinese one-liner is its translation.

**The whole documentation set is bilingual.** Every file under `docs/` has an `.en.md` counterpart: `CONTENT`, `DATA`, `INTERNALS`, `DESIGN`, `STATUS`, plus the index `README.index.en.md` and `fabric/README.en.md`. Rules:

- The Chinese version is the source of truth and the English one follows it; heading sequences, table dimensions and code blocks must correspond one-for-one (this round: all 7 pairs have identical heading sequences and identical line counts).
- The first line of every file is the language-switch line, pointing at the other version; cross-doc links inside English files always point at `.en.md` so a reader is never sent back to a Chinese page.
- Code, JSON, field names, ids, commands, raw log lines and numbers are byte-identical in both languages and are never translated; custom effect names use the official names from `en_us.json` (`Long Shot`, `Vexing Guard`, `Sculker`, …), with the Chinese original given at the first mention.

**Two stale numbers fixed in passing** (`DESIGN.md`, historical narration predating the 18-pattern set): §2's "8 trims × 11 materials" is now 18; §8's "the strong version at 90 s × 2 = 180 points" contradicted its own previous sentence ("strong 5 seconds"), so it now states the version-independent point — the strong tier multiplies a doubled drain rate by its duration, so a single dose can pin a player at 1 max health.

**Verification**: every markdown file in the repository has no BOM and no CR (pure LF); 0 broken relative links; 7 zh/en pairs report `seqMatch=True` on heading sequences, and section counts and bullet counts match one-for-one (`STATUS`: 24 / 79 in both); `CONTENT`/`DATA`/`INTERNALS`/`DESIGN` and the index have identical line counts per pair (156 / 470 / 258 / 198 / 35), while `STATUS` differs by 2 lines (387 / 385) because the English paragraphs break in different places. Built `tomc-trimeffectively-0.1.0.53-fabric+mc1.21.11.jar`; the log matches 0.1.0.52.

## Not yet verified / known gaps

- `PiglinBruteAiMixin`'s second injection (`findNearestValidAttackTarget`) is only applied when `PiglinBruteAi` is loaded, and runClient never spawned a brute, so there was no runtime verification; `defaultRequire:1` guarantees a signature mismatch crashes outright rather than failing silently.
- `WitherBossMixin`, `EnderDragonMixin`: as above, no runtime verification.
- Container extra loot only covers containers that roll "when first opened"; chests already filled during world generation do not roll again.
- Wild's and the vex's diagnostic logs (`[tre] loot probe` / `[tre] summon probe` / `[tre] extra_loot_roll`) were all deleted in 0.1.0.49; add them back when something needs reproducing.
- Wild's "does not fit, so it drops at the container position" relies on vanilla's drop-on-insert-failure logic; not measured.
- Wild's "9 fish" is characterised (nested loot tables counted as two layers, see §18); 0.1.0.43's depth guard is verified not to work, and after 0.1.0.44 switched to `@WrapMethod` the user's re-test after 0.1.0.47 reported the in-game effect matches expectations. If double drops appear again, the quickest route is to add the `[tre] extra_loot_roll` log line back into `LootRollEffect` and see how many times each wearer is called.
- Endphase's max-health flicker only got the "whole seconds" quantisation, the most likely fix; it was not reproduced or verified in game. If it still flickers, the next step is to tell apart **a shorter heart row (max health)** from **health being refilled (current health)**.
- 0.1.0.31's two material fixes were only verified at data-load level (`runServer`: `tre_pattern=18 tre_material=11`, the `attribute entries` list containing `trimeffectively:netherite`, no decode errors); durability 592→1184 and the -30% burn duration still need measuring in game.
- The recipe hijack is only verified as "19 rewritten, load without errors"; whether 2 templates actually come out when laid out on a crafting table needs measuring; turning the option off should restore the original recipes (in the same `runServer` the log should go from 19 to 0).
- The config screen is only verified by build and client startup (53 mods, with `cloth-config 21.11.160` added); **the screen was never opened**. Mod Menu is `modCompileOnly` and is not present in the dev environment. Four things need measuring on an instance with Mod Menu installed: opening the config from the list, the screen being a search bar + left index scrolling single page, **editing and saving directly from the main menu** (written to the local `config/trimeffectively.json`), changes taking effect immediately in singleplayer (recipes going from 19 back to 18), and the whole page greying out with server values shown after joining someone else's server.
- Sound annotation only works for sounds **the server sends to this client**: the broadcast radius of `ServerLevel#playSeededSound` is `SoundEvent#getRange(volume)` = `fixedRange.orElse(volume > 1 ? 16*volume : 16)`, so an ordinary sound at volume 1.0 only reaches players within 16 blocks; for anything farther the client does not know it exists, and writing an annotate radius of 32 will not show it. If this has to be broken later, the only way is a separate server-side event, and the cost is not worth the benefit, so it is not being done for now.
- 0.1.0.40's three smithing recipes are only verified as "datapack loads, recipe total 1470 → 1473, no decode errors" (`runServer` log `Loaded 1473 recipes`). **They have never been laid out on a smithing table**: whether the iron nugget / glass pane appears in the template slot, whether the book and quill appears in the addition slot, whether the effects vanish immediately after stripping, whether the returned template goes into the inventory (it should drop at your feet when full), and what the red cross looks like on failure, all need measuring.
- `tre_enabled`'s client-side behaviour (the tooltip changing to `已被无效化` and not expanding) is likewise unmeasured.
- The air-jump's wind-burst sound and particles are unmeasured (this needs actually chaining jumps in the air).
- Endphase's three delivery routes (§19) and the eye duration (§20) are only verified for the data payload and compilation: `runServer` log `Loaded 1474 recipes` (the extra one over 0.1.0.43 being exactly the dragon-head stew JSON), `[tre] recipe hijack: 19 of 1474`, no decode errors. Actually untested: whether the three potion tiers give 20/40/10 seconds, whether tipped arrows give vanilla's 1/8, whether dragon-head stew can be crafted and gives a 5-second effect, and whether the eye's duration after triggering varies with damage and piece count.
