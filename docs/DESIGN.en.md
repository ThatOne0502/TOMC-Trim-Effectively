[简体中文](DESIGN.md) &nbsp;·&nbsp; **English**

# TOMC-Trim Effectively — Design Philosophy

> Written for conversations between designers: this document is only about trade-offs, not about code.
> After reading it you will know what each decision is defending against, what it gave up, and which things are deliberately not done rather than not done yet.
> Mechanism details are in [INTERNALS.md](INTERNALS.en.md), fields and numbers in [DATA.md](DATA.en.md), and the player's view in [CONTENT.md](CONTENT.en.md).

Scope: 1.21.11 / Fabric, primarily singleplayer and usable in multiplayer; the mod changes neither how any vanilla trim is obtained nor how it looks, it only adds meaning to the fact that a trim already exists.

---

## 1. Three lines

The whole project compresses into three sentences:

1. **Content lives in the data, judgement lives in the engine.** The JSON says which trim gives what; the code says how to count, how to compute, and when to fire. Adding content needs no compile; changing the engine needs no touching of data.
2. **The code does not recognise "which tier", only "how many pieces".** A trim is not an enchantment: no levels, no experience, no upgrade path.
3. **The client only does presentation.** Every number is computed on the server; the client uses the registry to build tooltips and draw sound callouts. That settles the next point: what the client sees cannot constitute a fact on the server, and the reverse holds too.

Every section below explains why these three sentences are written this way, and what they cost.

---

## 2. Why piece count, not tier

Vanilla hands us a natural unit of measure: **the number of pieces carrying the same trim set**. Four armour pieces, at most four; add main hand and off hand and it is six at most.

The reasons for choosing it, in order of importance:

- **Predictable.** A full-diamond `sentry` set is 4 pieces; take the chestplate off and it is 3. Players can work it out while dressing and undressing, without opening a screen to read numbers.
- **It introduces no new progression system.** A tier needs an answer to "how does it go up" (mining? combat? idling?). Any answer becomes a new grind line, and this mod does not intend to deal that card.
- **The ceiling exists for free.** Six pieces is the cap, so every effect carries its own ceiling, which removes a whole class of balance problems.
- **Orthogonal to "material".** On the same piece of gear, the trim gives one set of effects and the material gives another, each counted by its own piece count. 8 trim patterns × 11 materials needs no interaction table.

The costs have to be written down too:

- **The four-piece set is the optimal build.** Wanting the effect means **wearing all four**, which pushes players' freedom of combination towards "one full set of the same trim". The design accepts this: in survival a trim is gated by template scarcity anyway, and assembling a full set is itself a goal.
- **No growth curve.** From the first piece to the fourth is linear; there is no qualitative break. The compensation is to hang the numbers on variables the player controls (distance, speed, missing health, gold groups, harmful effect count), so that the same suit behaves differently in different situations.
- **It multiplies cleanly with enchantments.** We do not intend to suppress enchantments, nor to step aside for them; a four-piece set plus Protection IV is strong, and that is acknowledged.

The four stacking modes of `stack` (`per_piece` / `per_piece_capped` / `once` / `full_set`) exist so that the "count by pieces" main line can absorb a few exceptions, not to build four separate systems. In the actual data only `per_piece`, `once` and `full_set` are in use; `per_piece_capped` stays in the contract, unimplemented.

The `iron` entry is the only place where `per_piece` is deliberately not used: its value applies to `armor`, and `per` already takes `gear.armor` (the sum of the four pieces' armour values). Multiplying by the piece count again would count the same piece twice, so it is written as `once`. This is not a balance choice, it is to prevent double counting.

---

## 3. Why trim identity has to be stamped

Three things get stamped: **which trim + which material** this stamped value was computed from; which attributes it added to the item; and whether this item's effects are switched off.

Stamping, rather than computing on demand every time it is needed, is a response to three hard constraints:

- **An item outlives its wearer.** A chestplate in an item frame, a dropped item, an inventory another mod has walked through: none of them has a wearer to serve as context. Deciding what this thing does can only use what is written on the thing itself.
- **An engine that runs once every 10 ticks cannot rewrite items every pass.** Unconditional recomputation would bump the component version every round, and the client would redraw the tooltip with it; it would likewise wipe same-named attributes a player had written by hand at an anvil or in creative mode.
- **A stamp written once can be read offline.** That is exactly the design of `reconcile`: if the value has not changed it does nothing at all, not even a component write.

The cost is that **old items do not upgrade themselves**. A datapack has changed the formula; the piece already sitting in a chest still holds the old value. There are three remedies: the engine sweeps one slot of one player's inventory per tick, an item hook fires when an inventory is opened, and `/tre reload` forces a re-stamp. Containers nobody has touched in the world are not swept — an item is only worth adopting once it has passed through a player's hands, and that is deliberate, not an oversight.

The second cost: `reconcile` must also hold for items that existed before the trim component did. Gear from earlier versions has no stamp, and the first time it is swept it needs an **empty stamp** filled in — even if the data gives no attributes at all. Otherwise "has a trim, has no stamp" would be both a legal state and a drift awaiting repair, and the sweep would never finish.

---

## 4. Why the client only does presentation

Because "the client can compute it" and "the client should compute it" are two different things.

Conceptually, the client's inputs are the registry synced from the datapack plus the player's own entity state. For tooltips, for callouts, for fading, the inputs are complete; for damage they are not (no access to the server's random numbers, no complete state for other entities, and no basis for trust).

So the division of labour is: **the server is the only source of numbers, the client only decides how things look**.

Two places where this cuts deep:

- **The sculk callout** (`ward`) is not a server entity or a datapack broadcast; the client spawns a local `TextDisplay` that is visible only to this client, follows the beam, shows through walls and fades out. The reason is not only saving sync: custom world-space rendering in 1.21.9+ has to go through the submit system, which costs far more than a local entity. The cost is that this callout cannot be audited by the server and does not appear in recordings — accepted.
- **The tooltip** is replaced in place, not appended to. One line replaces one line, and with the key not held it is identical to vanilla.

The cost list: two code paths to maintain (sound and particles), any overreach on the client degrading to "not visible", and every bug where "it looks right on the client but nothing happened on the server" being harder to reproduce.

---

## 5. Why data-driven, not hard-coded

Hard-coding 29 effects would mean: one compile and one compatibility pass per effect change; and a modpack that wants a given effect gone can only fork the whole thing.

What data-driven buys:

- **Override semantics for free.** Same id, last one loaded wins, the entry replaced wholesale. Someone who thinks "this trim is too strong" only has to drop in a JSON of the same name; no code change.
- **Extensions need no code change from us.** Another mod can add its own trims just by putting data into the same-named directory, or by putting a `pattern` in its own datapack that points at it — the engine does not need to know that mod exists.
- **Balancing numbers is editing text.**

The costs:

- **Validation becomes a runtime matter.** A broken JSON does not fail a compile; it logs one error in that round's log and the entry does not take effect. We chose "dirty data is inert plus a log you can search" over "loading fails" — one bad file should not scrap an entire datapack.
- **The cliff is steeper.** Every call rests on "rebuild the index every round", and there is no hot-path cache to lean on.

---

## 6. Three things deliberately not done

### No expression language

`Value` is always `value × Π per × piece factor`, plus the optional `power` and `min`/`max`. No conditional branches, no functions, no other data sources.

Reason: once you allow expressions you have to write a parser, write a sandbox, answer "does this variable have a value on the client", and handle division by zero inside expressions. And the fun of this mod is not "how complex an effect can you write", it is "assembling simple things into a set" — `power` already expresses a curve like "stronger the further away, but flattening out", and in practice the material has been sufficient.

Cost: a combination like "scale speed with missing health, with a cap, and also scaling with piece count" has to be split into two data entries. Acceptable.

The same goes for what was not done: `effect_id` (to tell the same effect appearing twice on one item apart), a condition DSL (vanilla predicates are enough), and tiers.

### No modifier-monitoring scheme

The early plan was "listen for attribute changes and maintain an external table". It was abandoned because it fights vanilla and other mods over the same fact: attribute modifiers are idempotent anyway, and the same id overrides. It became "one aggregate modifier per (attribute, entry), with a prefixed id, removing our own before recomputing".

The criterion is fixed in code review: **any logic that has to walk an entity's modifier list in order to decide something is a design error.** The four lifecycles — re-equipping, relogging, dying, unloading — are already covered by prefix cleanup.

### No damage-reduction ceiling

`damage_in` is a pure multiplicative channel, and we **do not add an extra "80% reduction at most" gate** — vanilla already has a cap of its own, and stacking another layer on top of it would double-clamp and leave data authors unable to work out the final value.

The two gates that are kept are vanilla semantics, not our invention: damage tagged `#minecraft:bypasses_effects` or `#minecraft:bypasses_resistance` (void, `/kill`, starvation and the like) skips trim damage reduction outright. Without those two, a player at full reduction would be immune even to the void.

The cost of that trade: **trim damage reduction can stack to very near immunity.** We accept it, because it needs a four-piece set plus high movement speed (`spire` is "1% per m/s on every piece") — you only get the reduction while moving, not standing still; and it is "reduction that goes into the damage calculation", not invulnerability.

---

## 7. Why the vanilla 1/8 on tipped arrows is not intercepted

Vanilla's handling of tipped arrows is to **divide the potion effect by 8** (both duration and amplifier shrink), and that is the only place where the rule "an arrow is weaker than a bottle" is implemented.

We chose **not to intervene**, which means an Endphase applied by a trim through an arrow carries only 1/8 of the duration, rather than our recomputing it from `√(2 × damage)`.

Three reasons:

1. **Rule consistency.** Players already know that "potion effects on arrows are 1/8". Making trim arrows an exception would require them to remember a second set of rules.
2. **Arrows are cheap.** Infinity bow plus tipped arrows is a combination that is easy to mass-produce. Making its dissociation strength equal to melee would turn `eye`'s optimal play into "stop hitting, start shooting".
3. **The balance comes without extra code.** By not writing that code, the 1/8 happens naturally, and it is exactly vanilla.

Cost: **`eye`'s ranged route is cut down much harder than melee.** That is deliberate and is stated in the content document; it is not a hidden mechanic.

---

## 8. Endphase, revised three times

A custom debuff, not complicated in itself, but it brings three trade-offs into the open at once.

**First version: 10 / 15 / 5 seconds for base, long and strong.** Dissociation = duration in seconds × (amplifier + 1), applied to max health.

**The problem found:** the strong version at 90 s × 2 = 180 points of maximum-health loss, against a base max health of 20, so drinking it kills instantly. Any design that converts "duration straight into health" explodes at the strong tier.

**Second revision: three tiers at 20 / 40 / 10 seconds.** Normal at 20×1 and strong at 10×2 both total 20 points of drain; only the cadence differs: normal bleeds steadily, strong kills within ten seconds. So "strong = faster" rather than "strong = more painful", and the semantics stay clean.

The surviving counter-intuitive point: **the long version hurts more but slower; the strong version is faster but totals the same.** Players used to "the higher the potion level, the stronger" will first assume the strong version drains more health. We added no hint to explain it, because the duration in the tooltip already says so.

**Third decision: how many seconds a trim applies.** A fixed value would make `eye` mean "one glancing touch and you are dissociated forever". The final choice is `√(2 × this hit's damage)`, accumulated per piece, with no cap.

- The square root keeps a heavy hit from emptying the bar in one second, and gives a light hit some meaning (1 point of damage → 1.41 seconds).
- Accumulating per piece with no cap is deliberate: this is the strongest single burst in the mod, and a four-piece set landing a melee hit ought to be lethal.
- The cost is **very long durations on a chain of reflected hits**: hit a target fully stacked with `eye` while they carry a high-damage weapon, and the duration snowballs. Accepted, because that is the consequence of "you chose to hit an enderman avatar with a heavy weapon".

**Water damage and the death message:** water is copied from the enderman exactly (1 point per tick, not reduced by armour, and does not raise dissociation again through damage); for the death message we create no new `DeathMessageType`, only a damage type plus two lang entries, because vanilla's `message_id` mechanism exists for precisely this.

---

## 9. Boundaries and costs

### Who it applies to

The engine hangs off `LivingEntity`, with no player branch. So zombies, skeletons and armour stands wearing trimmed armour all take the effects; player-only entries such as trading, jumping and breeding naturally never fire.

Cost: **trimming a mob buffs the mob**, including `eye`, which can kill you. This is a feature, not a bug, but server owners need to know it.

### Tools and redstone

Mods such as `trimmable_tools` push tools into the `#minecraft:trimmable_armor` tag, and the tools then carry vanilla's `minecraft:trim` component. We write no compatibility code at all — `TrimSnapshot` reads "any item in any slot that carries `TRIM`", so it works automatically.

Cost: `item_property` effects (durability ×2, keeping the repair cost) land on tools as well. Acknowledged — and that is the benefit of being compatible with that mod.

### Performance

Every 10 ticks it walks all entities and recomputes attributes; the cost is linear in the number of entities. Imperceptible in singleplayer, a real expense on a large server. The "only compute players nearby" optimisation was not done, because it would make the telemetry disappear — `/tre dump` has to be able to show the state of any entity.

### Observability

`/tre dump` lists only **what has already been computed**: entries, piece counts, the aggregate value of each attribute. It does not recompute and does not evaluate conditions (conditions are tagged `[conditions NOT evaluated yet: n]`). That is the trade-off: the command must not change state, so that "the numbers changed after running dump once" cannot happen.

### Compatibility risks

- Coexisting with any mod that also touches attribute modifiers, the prefix is our only boundary. Another mod writing ids into the `trimeffectively:` prefix as well would collide (very unlikely).
- Coexisting with other injections into `Player#actuallyHurt`, the order depends on mixin priority. We chose the default priority and settle in TAIL, so as not to compete for HEAD.
- Datapack revisions are not retroactive: change the formula and old items in the world keep the old values until they are swept or `/tre reload` runs. This is the direct cost of stamping; see §3.

---

## 10. One sentence to close

This mod is not "enchantments for armour", it is **giving a trim an identity**: something that already exists, that template scarcity gates, and that players assemble for looks, gains a layer of "what happens when you have assembled it".

Every trade-off circles around that one point: making people willing to wear the same set, rather than calculating which set has the highest numbers.
