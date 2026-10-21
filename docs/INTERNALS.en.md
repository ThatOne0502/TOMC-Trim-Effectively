[简体中文](INTERNALS.md) &nbsp;·&nbsp; **English**

# TOMC-Trim Effectively Internals

> Written for peers and for developers who want to correct us: this document describes what the code actually does at runtime. It does not explain why things were chosen that way.
> After reading it you can: locate where a single evaluation happens, why some effects require two mixins, what `TrimStamp` stamping is guarding against, and where to look when reproducing these problems.
> For design trade-offs see [Design](DESIGN.en.md); for the data format see [Data](DATA.en.md).

Coordinates: Fabric / MC 1.21.11 / Java 21 / fabric-loom `remap` plugin / mojmap (note that from 1.21.11 on, `ResourceLocation` is called `Identifier`, and `Holder` is `RegistryEntry` in some signatures) / package `io.github.thatone0502.trimeffectively`.

---

## 1. Data flow

```
data/*/trimeffectively/tre_{pattern,material}/*.json
  └─ DynamicRegistries.registerSynced (datapack load + hot reload + codec validation + server→client sync)
       └─ TreEngine every 10 ticks:
            TrimEffects.load(access)   → flat index Map<trimId, List<Named>> (snapshot)
            AttributeEngine.load(access) → List<TrimEntry>
            walk every LivingEntity:
              TrimSnapshot.of(entity)    → for this entity, "which pieces carry a trim, and which one each"
              TrimEffects.active(...)    → Active(entryId, entry, pieces) for entries whose trigger matched
              Value#eval(pieces, vars)   → number
              AttributeEngine.apply      → modifier
       event-type effects: each effect class calls TrimEffects.activeOfType(...) in the matching hook
```

Two invariants hold all of it up:

1. **The registry is read-only; the index is rebuilt every round.** Each round the engine photographs both registries into an immutable `Index`, and handlers never read the registry directly.
2. **A handler never sees `TrimEffect`, only `Active`.** `Active` already carries `pieces`, so an effect class does not need to know "how to count pieces".

---

## 2. Dynamic registry: why one call yields four things

```java
public static final ResourceKey<Registry<TrimEffect>> TRE_PATTERN  = Tre.key("tre_pattern");
public static final ResourceKey<Registry<TrimEffect>> TRE_MATERIAL = Tre.key("tre_material");

DynamicRegistries.registerSynced(TRE_PATTERN,  TrimEffect.CODEC);
DynamicRegistries.registerSynced(TRE_MATERIAL, TrimEffect.CODEC);
```

- `registerSynced` is a hard requirement: the client needs the definitions to draw tooltips, compute durability bars, and render the sculk callouts. Without syncing, the client has an empty registry.
- The directory rule follows from the registry key: `data/<pack>/trimeffectively/tre_pattern/<filename>.json`. An entry id is necessarily `<pack>:<filename>`, which is where "an entry id cannot equal `minecraft:sentry`, and the binding must be written into the JSON" comes from.
- The datapack path rule also settles overwrite semantics: for the same id the later load wins, the whole file is replaced, never merged.
- Upstream noise: `DynamicRegistrySetupCallback` has a regression in 26.1+ snapshots (fabric-api#5149). It is unrelated to this version, but this area must be retested when porting to 26.x.

---

## 3. Division of labour between the four roles

| Class | Input | Output | What it must not do |
|---|---|---|---|
| `TrimSnapshot` | one `LivingEntity` | list of trimmed pieces (slot + `pattern` + `material` + item stack) | must not query the registry, must not evaluate |
| `TrimEffects` | registries + entity | `Index` (flat), `List<Active>` | must not apply effects |
| `AttributeEngine` | `List<TrimEntry>` + entity | attribute modifiers | must not handle event-type effects |
| each `*Effect` class | `Active` + context | side effects (damage, summon, sound…) | must not count pieces, must not read the registry |

Two details of `TrimSnapshot`:

- **A piece whose effect is switched off is not a piece.** Items with `TrimStamp.enabled(stack) == false` are dropped while the snapshot is taken—filtering them downstream instead would make the piece count, the attribute sum, and `/tre dump` each wrong once.
- `countByIdAndSlots(trimId, slots)` filters by slot group (`armor` = head/chest/legs/feet, `hand` = main/off hand, `any` = all), and both `pattern == trimId || material == trimId` count as a hit.

Three details of `TrimEffects`:

- `Named.key()` is `entryId + "#" + index`. One file may hold several effects and they share the file id; keying on the file id alone would drop everything after the first.
- Several data entries may point at the same trim id, so the index is `Map<trimId, List<Named>>`, and one trim can stack several data entries.
- **Without a `DamageSource`, `conditions` are not evaluated**: any condition that cannot be evaluated is treated as false and logs one warn.

---

## 4. Engine tick

```java
ServerTickEvents.END_SERVER_TICK.register(server -> {
    PortalTracker.flush(server);
    sweepInventories(server);                 // every tick, one slot only
    if (++counter % PERIOD != 0) return;      // PERIOD = 10
    TrimEffects.update(TrimEffects.load(access));
    var entries = AttributeEngine.load(access);
    SEEN.clear();
    for (ServerLevel level : server.getAllLevels())
        for (Entity entity : level.getAllEntities())
            if (entity instanceof LivingEntity living) {
                SEEN.add(living.getUUID());
                AttributeEngine.update(living, entries);
            }
    if (++counter % (PERIOD * 20) == 0) AttributeEngine.prune(SEEN);
});
```

- **10 ticks (0.5 s)**: attribute modifiers persist on their own; recomputing only keeps quantities that change—health ratio, speed, gold count—up to date. 10 ticks makes effects like low-health speed read as continuous without walking every entity each tick.
- **`prune` every 200 ticks (10 s)**: forgets the UUIDs of entities no longer in any level, so the `APPLIED` table does not grow forever across spawn-death cycles.
- `sweepInventories` walks **one slot in one player's inventory per tick**, rotating on a cursor. It only runs `TrimStamp.reconcile`, to adopt old items that existed before the stamp component did; it does not scan containers in the world (see §5 for why).

---

## 5. `TrimStamp` identity stamping

Three components must move together:

```
minecraft:trim            (vanilla)
trimeffectively:properties (this mod's stamped values)
trimeffectively:enabled    (writes false when switched off; missing = on)
```

The invariant: **the stamped values must belong to the trim actually on the item.** That is exactly what `reconcile(stack)` does—it reads `TRIM`, recomputes one from the data by `(pattern, material, enabled)`, and compares it with the existing stamp:

- same → do nothing (no component write, no sync, no client redraw);
- different → write the new one;
- no `TRIM` → remove all three components together.

`force(stack)` writes without comparing and is only used where "the player explicitly asked for a recompute": re-enabling the trim switch, and the forced refresh after `/tre reload`.

**Why identity comparison rather than unconditional recompute**

- Stamped values must be readable offline: equipment in an item frame, items on the ground, another mod walking an inventory—none of them has a wearer.
- An unconditional rewrite every 10 ticks would change the item component version each round, making the client re-render the tooltip and the durability bar; it would also wipe attributes the player hand-edited with the same name at an anvil or in creative mode.
- The cost: **old items do not upgrade themselves.** Three places make up for it: the engine's per-tick inventory sweep, the `ItemStack` hook when an inventory is opened, and the forced refresh in `/tre reload`. Old items in containers nobody touches in the world are never picked up, and that is deliberate—an item is only worth adopting once it passes through a player's hands.

---

## 6. Mixin points on the damage chain

`Player#actuallyHurt` is **fully rewritten and does not call `super`**, so injections on `LivingEntity#actuallyHurt` do not apply to players. The same logic therefore exists twice: `LivingEntityMixin` and `PlayerActuallyHurtMixin`. `actuallyHurt` is dispatched as a virtual call, so one hit only goes through one of them and nothing is counted twice.

Order within a single method (player side as the example):

| Order | Injection | What it does |
|---|---|---|
| 1 | `@ModifyVariable(HEAD, argsOnly)` on `DamageSource` | if the victim carries endphase: replace the source with `trimeffectively:endphase` (keeping `directEntity` / `causingEntity`); skip if it is already that type |
| 2 | `@ModifyVariable(HEAD, argsOnly)` on `float amount` | `EndphaseEffect.onDamageTaken`: dissociation += `floor(amount)` seconds |
| 3 | `@Inject(HEAD)` | record `getHealth()` so TAIL can compute the actual health lost |
| 4 | vanilla method body | armour, enchantments, absorption, `damage_in` and the like are read here |
| 5 | `@Inject(TAIL)` | `ThornsEffect.afterDamage` (uses the health difference recorded at HEAD as "actually taken"), `SummonEffect.afterDamage`, `MobEffectApplyEffect.afterDamage` |

The order is semantic: **change the source first, accumulate dissociation second, and settle the reflect at TAIL with the real health lost.** The reflect uses "actual health lost" rather than the incoming `amount` because the latter is still reflected unchanged after armour reduction, which amounts to ignoring armour.

The `take_damage_before` and `take_damage_after` triggers sit on either side of this chain: `damage_in` runs before (it affects damage reduction), while `thorns` / `summon` / `apply_mob_effect` run after (they only make sense once damage has actually happened).

`damage_in` replicates both of resistance's gates: damage tagged `#minecraft:bypasses_effects` and `#minecraft:bypasses_resistance` skips trim reduction outright—otherwise void, `/kill` and starvation damage would be eaten by the reduction.

---

## 7. Loot: why `@WrapMethod`

```java
@WrapMethod(method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V")
private void trimeffectively$extraRolls(LootContext context, Consumer<ItemStack> output, Operation<Void> original) {
    boolean outermost = LootRollEffect.enter();
    try {
        if (outermost) LootRollEffect.onRoll((LootTable) (Object) this, context, output);
        original.call(context, output);
    } finally {
        LootRollEffect.exit();
    }
}
```

Two decisions:

1. **It must hook the `LootContext` overload.** 1.21.11 has two `getRandomItemsRaw`; every public entry point builds a `LootContext` and then calls this overload, so the `LootParams` one never sees an individual roll.
2. **It must wrap the body; a HEAD injection will not do.** `NestedLootTable#createItemStack` feeds the same context back into this method, so one fishing cast is two layers (`gameplay/fishing` nesting `gameplay/fishing/fish`). With a HEAD injection the outer call has already returned and the nesting depth is back to zero, so each layer rolls the extra dice once—which is exactly the "two fish per cast" bug in 0.1.0.43. Wrapping keeps `enter()` valid for the whole method body, and `finally` guarantees an exception cannot leak the counter into "permanently off".

`LootRollEffect` is internally just an entry counter: `enter()` returns whether it is the outermost layer, `exit()` decrements. The extra dice are rolled once regardless of nesting depth.

---

## 8. The evaluation path of `Value#eval`

```java
double product = 1.0;
for (String name : per) product *= variables.getOrDefault(name, 1.0);
if (power != 1.0 && product >= 0.0) product = Math.pow(product, power);
double result = value * product;
result *= switch (stack) {
    case "once" -> 1.0;
    case "full_set" -> stackCount >= 4 ? stackCount : 0.0;
    default -> stackCount;               // also covers "per_piece_capped" (no separate branch implemented)
};
if (min != null) result = Math.max(min, result);
if (max != null) result = Math.min(max, result);
```

- `power` only bends the `per` product; `value` and the piece-count factor stay linear. The exponentiation is skipped when the product is negative (`Math.pow(negative, 0.5)` = NaN).
- A missing variable counts as 1.0: this laxity is deliberate, so a data entry degrades instead of disappearing when the context is incomplete.
- There is no fourth behaviour beyond the three `stack` branches: `per_piece_capped` falls through to `default`, i.e. it behaves as `per_piece`. The contract is reserved; the engine has not implemented it.
- `variables` is built centrally by `AttributeEngine.variables` (`health_ratio`, `missing_health_ratio`, `gear.armor`, `speed_mps`, `harmful_effect_count`, `on_fire`), and the event side adds `attack_distance` / `damage_*` / target state and so on.
- `speed_mps` takes `SpeedTracker.peak(entity)`: the peak horizontal speed over the last 5 ticks rather than the instantaneous value—the instantaneous value snaps to zero on collisions, slopes and steps, and the error is visible to the eye.

---

## 9. How attribute modifiers are written

```java
Identifier modifierId = Tre.id(path + (operation == ADD_VALUE ? "" : "_" + operation.getSerializedName()));
```

- One modifier per `(attribute, entry, operation)`; the id is derived from the entry path, and something like `add_multiplied_base` appends a suffix.
- The same id is an **overwrite, not an accumulation**, so multiple effects with the same id in one round are first `merge`d by addition in the `wanted` table and then applied in one `addOrReplacePermanentModifier`.
- On recompute, ids that "existed last round but do not exist this round" are removed, and ids with "exactly the same value" are kept (the write is skipped to avoid client jitter).
- `APPLIED` records "which attributes we have touched on this entity"; `prune` uses it to clean up departed entities, and `clear` removes everything on death.
- The criterion: any logic that has to iterate an entity's modifier list to decide something is a design error—the prefix plus the bookkeeping already covers equipment changes, relogging, death and unloading.

`extra_jumps` is author sugar: inside `AttributeEngine` it is translated into an `add_value` modifier on the real attribute `trimeffectively:extra_jumps`, so commands, other mods and the client see an ordinary attribute.

---

## 10. The client only renders

The client takes part in three places only, and none of them affects numbers:

- **tooltip** (`TrimTooltip`): while the bindable key is held, it **replaces in place** the two vanilla tooltip lines "trim pattern name" and "material name" with effect name + description; one line for one line, no added lines. The data comes from the synced registry, not from a network packet.
- **sound annotations** (`SoundAnnotations` + `SoundManagerAnnotateMixin` / `ServerLevelGameEventMixin`): `annotate_sound` renders on this client only. The vibration particle uses `ParticleTypes.VIBRATION` + `VibrationParticleOption(EntityPositionSource(this player), ticks)` pointing from the sound source to the player; the text is a **`TextDisplay` spawned locally on the client** (never sent to the server), with `billboard=CENTER`, `see_through=true`, rewritten every tick with an alpha-bearing `Style` to fade out. A custom render pipeline (the 1.21.9+ submit system) costs too much, and a local entity sidesteps it.
- **config screen** (`TreConfigScreen` / `TreMenuIntegration`): local config is written to `config/trimeffectively.json`; when connected it shows the server's values read-only (`TreConfigPayload` / `TreConfigSync`).

The landing reset of `extra_jumps` has a client-side movement check, but the server keeps one validation—client-side alone, creative flight, chained jumps and `onGround` timing all fight each other.

---

## 11. Pitfalls hit

| Symptom | Root cause | Fix |
|---|---|---|
| The same player takes damage twice / drops double | `Player#actuallyHurt` is a rewritten method, so the injection on `LivingEntity` does not apply to players (when a hook is missed it applies halfway); nested loot tables each roll the extra dice | two mixins; `@WrapMethod` + depth counting |
| Modifiers pile up and do not disappear when removed | vanilla stores them by id in a map and the same id overwrites; an early version built ids from slots, which made "same id stacks naturally" false | aggregated modifiers + prefix bookkeeping |
| `durability_multiplier: 2` becomes `true` | the default `DynamicOps#getBooleanValue` is `getNumberValue(...).map(b -> b != 0)`, so trying boolean first decodes 2 as true | fixed literal decode order: number → boolean → string |
| Two fish per fishing cast | see §7 | — |
| Full damage reduction eats the void too | `damage_in` is a multiplicative channel and must replicate both `#bypasses_*` gates | — |
| The tooltip flickers every 10 ticks | the modifier value is unchanged but still rewritten, and the item component version change triggers a redraw | skip the write when the value is unchanged |
| Sound annotations without subtitles show blank | closed captions cannot find `subtitles.*` | fall back to the raw sound id, going through ordinary text translation |
| An amplified potion kills instantly | `delta = -(remaining seconds) × (amplifier+1)`, and 90 s ×2 already exceeds 20 base health | three tiers changed to 20 / 40 / 10 seconds, total deduction aligned |

**Forensics method** (you can confirm an interface without launching the game):

```bash
# find a method signature and field
javap -p -c -classpath <minecraft-merged.jar in the loom cache> net.minecraft.world.entity.LivingEntity
# check who a method actually calls (to decide which overload to hook, and whether there is a super call)
javap -p -c ... net.minecraft.world.entity.player.Player | grep -A5 actuallyHurt
# confirm the real field names of tags/predicates (1.21 removed fields such as DamageSourcePredicate#is_projectile)
unzip -p minecraft-merged.jar data/minecraft/tags/damage_type/is_projectile.json
```

For what `javap` cannot show (whether a mixin target is also injected by another mod, and the bytecode order after injection), export with `-Dmixin.debug.export=true` and read the decompiled result under `run/.mixin.out/class/`. This step is the only reliable way when tracking down "two mixins, only one effective".

---

## 12. Engineering side

- Build output: `tomc-trimeffectively-<maj>.<min>.<rev>.<build>-fabric+mc<mcver>.jar`; `mod_version` is edited by hand, `<build>` is auto-incremented from `build-count.txt`; the version in `fabric.mod.json` is written with all four segments (`0.1.0.1`) while the file name is assembled separately, to avoid a loader parse failure.
- Data separated from code: all JSON lives in `fabric/src/main/resources/data/trimeffectively/`, to be extracted into a shared `common/data/` when porting.
- The three smithing table recipes are registered as `PlacementInfo.NOT_PLACEABLE` and `isSpecial()`—they are things you try out at the table, not grid recipes a recipe book could lay out.
- The base slot's `Ingredient` is resolved lazily (`Supplier` + memoized on first use): recipe decoding happens before item tags are bound, so querying `#minecraft:trimmable_armor` during construction throws `Missing tag`; the memo holds the tag's live `HolderSet`, so a datapack that changes the tag still takes effect immediately.
