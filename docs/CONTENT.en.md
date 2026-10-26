[简体中文](CONTENT.md) &nbsp;·&nbsp; **English**

# TOMC-Trim Effectively — Content Overview

> For players and modpack users: this document answers only one question — what is actually in the game right now.
> After reading it you will know: what each of the 18 patterns and the 11 materials gives you, where `Endphase` comes from and how it is calculated, how the three extra recipes in the smithing table work, and the two commands plus the config screen.
> To change numbers or add your own trims, see [DATA.md](DATA.en.md); to see how the engine calculates things, see [INTERNALS.md](INTERNALS.en.md).

---

## 1. Trims do something now

One rule beats a thousand sentences of explanation:

1. **The pattern decides one set of effects, the material decides another, and both are active at the same time.** Put a `sentry` pattern on a chestplate with a copper ingot and you get both `Long Shot` (百步穿杨, `sentry`) and `Tough Shell` (固若金汤, `copper`).
2. **Counted per piece, not per tier.** Each extra worn piece carrying the same trim adds one more share of the number, up to 4 pieces. Helmet, chestplate, leggings and boots count as one piece each; mainhand and offhand count too, so 6 slots participate.
3. **While the trim is there it works; when the trim is gone, all of it is gone.** Strip the trim and the attributes, the toggle state and the stamp all disappear together. No extra step.
4. **Hold the "Reveal Trim Effects" key to see the details.** The item tooltip normally only carries one line — "Hold [key] to reveal trim effects" — and the numbers expand while you hold it.
5. **Effects can be switched off.** Once off, the tooltip reads "This item's trim has been made inert"; the appearance and the name are unchanged (how to switch off: see §5).

A 4-piece `sentry` set plus a 4-piece copper set, hitting from 3 blocks away: +60% damage (4 sentry pieces × 3 blocks × 5%), armour toughness +8. Every number multiplies out in the open; there are no hidden multipliers.

---

## 2. The 18 patterns

The "per piece" column is a single piece; the "four pieces" column is the value with four pieces worn. Hands count toward the piece count as well.

The Trim column gives the data id. The Chinese edition spells the same thing with its in-game Chinese name minus the trailing "Armor Trim" (塑造盔甲纹饰 becomes 塑造), and materials minus the trailing "质".

| Name | Trim | Per piece | Four pieces |
|---|---|---|---|
| **Long Shot** (百步穿杨, `sentry`) | `sentry` | +5% damage on this hit per block of distance | At 4 blocks, +80% on one hit; no cap |
| **Vexing Guard** (鬼使神差, `vex`) | `vex` | Taking a hit summons 1 vex that lives as many seconds as the damage of that hit | 4 vexes per hit, each living just as long |
| **Farm Bot** (奇迹行者, `wild`) | `wild` | One extra loot roll on kills, on loot containers, on fishing and on archaeology | 4 extra rolls of each |
| **Waterborne** (浪里白条, `coast`) | `coast` | Oxygen cap +2; underwater movement efficiency +0.667 (the same value as Depth Strider II) | Oxygen +8, almost unimpeded in water |
| **Prescience** (未卜先知, `dune`) | `dune` | On hit, squeezes the target's hurt invulnerability frames down to 70% | Down to 0, so you can chain hits |
| **Old Hand** (轻车熟路, `wayfinder`) | `wayfinder` | +50% speed while moving toward your respawn point | +200% in that direction |
| **Scientific Breeding** (科学配种, `raiser`) | `raiser` | Animal breeding cooldown -20% (6000 → 4800 tick) | Cooldown -80% |
| **Excelsior** (精益求精, `shaper`) | `shaper` | Repairing this piece on an anvil no longer accrues the penalty | Same (no stacking) |
| **Service Contract** (劳务合同, `host`) | `host` | 20% chance a trade does not consume one of the villager's uses | 80% chance |
| **Sculker** (幽匿侵染, `ward`) | `ward` | Your attacks use the sonic damage type; sounds made within 8 blocks get labelled (not through wool) | Same radius, only the damage type stacks |
| **Low-Key** (销声匿迹, `silence`) | `silence` | 30% chance a sound is swallowed: nobody hears it and vibrations do not detect it | The chance never stacks past 100%, so in practice it is total silence |
| **Asperges** (涤瑕荡秽, `tide`) | `tide` | Every negative effect on you runs out 200% faster | 800% faster |
| **Mammonism** (拜金主义, `snout`) | `snout` | Per "stack" of gold items in your inventory, crit multiplier +1% (base 1.5×) | Stacks by stack count as well; no cap |
| **Last Stand** (视死如归, `rib`) | `rib` | Every negative effect on you gives +20% damage; being on fire counts as one | 4 negative effects: +320% |
| **Ender Avatar** (末影化身, `eye`) | `eye` | 10% chance to apply `Endphase` to the target, for √(2 × damage) seconds | 40% chance, duration ×4 |
| **Kinetic Barrier** (动能屏障, `spire`) | `spire` | -1% damage taken per 1 m/s of movement speed | -40% while sprinting at 10 m/s |
| **Steadfast** (岿然不动, `bolt`) | `bolt` | Knockback resistance +0.15 (the tooltip shows +1.5) | +0.6 |
| **Sky Leap** (步步高升, `flow`) | `flow` | One extra jump in mid-air | 4 extra jumps |

A few worth spelling out:

- The vexes from **Vexing Guard** inherit your gear and only attack the target that just hit you. Take an 8-damage hit and the summoned vex lives 8 seconds — **the heavier the blow, the longer the grudge**.
- **Farm Bot**'s extra rolls fire only once per loot generation; a chest inside a chest does not double them again.
- **Last Stand** counts being on fire as a negative effect: burning + poison + slowness is three of them, +60%.
- **Ender Avatar**'s chance and duration are two independent numbers: the chance stacks per piece and the duration multiplies per piece, so at 4 pieces it both triggers more often and lasts longer. One piece with a 6-damage hit = √12 ≈ 3.5 seconds; four pieces with a 50-damage hit = 40 seconds.

---

## 3. The 11 materials

A material trim gives you the baseline: durability, prices, armour, experience — the things that do not depend on timing.

| Name | Material | Effect | Four pieces |
|---|---|---|---|
| **Fight or Flight** (共振应激, `amethyst`) | `amethyst` | Movement speed +100% × missing health fraction | Full health +0%; near death approaches +400% (500% speed) |
| **Tough Shell** (固若金汤, `copper`) | `copper` | Armour toughness +2 | +8 |
| **Forever** (金刚不坏, `diamond`) | `diamond` | Durability ×2 | Still ×2 (does not stack) |
| **Millionaire** (腰缠万贯, `emerald`) | `emerald` | Trade price -10% | -40% |
| **Glittering** (金光闪闪, `gold`) | `gold` | Piglins treat you as gold; if the piece is already gold gear, even brutes stay neutral | Same (no stacking) |
| **Ironclad** (百炼成钢, `iron`) | `iron` | Armour +0.5 × the combined armour of your active pieces | Applies as soon as it is worn; not multiplied by piece count a second time |
| **Blessed** (吉人天相, `lapis`) | `lapis` | Luck +1 | +4 |
| **Splinter** (针锋相对, `quartz`) | `quartz` | Reflects 50% when hit in melee | Reflects 200% |
| **Firewall** (赴汤蹈火, `netherite`) | `netherite` | Fire damage -16%; burning time -30%; the item itself never burns away | Fire damage -64%; four pieces are almost impossible to set alight |
| **Redstone Ware** (红石义体, `redstone`) | `redstone` | Attack speed +1; block breaking speed +1 | +4 each |
| **Experienced** (岁月史书, `resin`) | `resin` | Experience gained +100% | +400% |

- **Firewall**'s "the item does not burn" is written onto the item: dropped into lava, or lying on the ground on fire, it is still there. This has nothing to do with wearing it.
- **Forever**'s durability ×2 applies only once; four diamond pieces do not become ×16 — it is an item property, not a stack.
- **Ironclad** at four pieces does not square: its base value already is "the armour total of every piece carrying the iron trim", so it is only applied once.

---

## 4. Endphase (相位解离)

A negative effect that treats "remaining time" as its damage source. The name is `Endphase`; the icon is magenta.

**How it calculates**

- **The remaining seconds are the dissociation level**: max health = your base max health − remaining seconds (converted to whole seconds, rounded down).
- The cap starts dropping the moment it is applied. A 20-second version on a 20-health player leaves 1 max health the instant it lands.
- **Getting hit makes it worse**: each hit adds `floor(damage)` seconds to the dissociation level. Cut for 7, you gain 7 seconds.
- **Healing helps only at full health**: for the part of the healing that overflows above full health, every `ceil(overflow)` points remove 1 second.
- Once the dissociation level exceeds your base max health you die. Max health has a floor of 1 point, so this is not an automatic death but rather **any single hit can finish you** — on death the damage type is recorded as `trimeffectively:endphase`, the death message is "%1$s had never existed", and the killed-by-a-player version is "%1$s's existence was disproven by %2$s".
- **In water or in rain you take 1 extra point of drowning-type damage per second**, the same flaw as an enderman.
- The Wither and the Ender Dragon are **not immune** to it.

**Three delivery routes**

1. **Potions**: Awkward Potion + dragon head → Potion of Endphase, 20 s. Add redstone → 40 s; add glowstone → 10 s but at strength I, with the health drain twice as fast.
   Note the counter-intuition here: **"extend" is not mercy** — the same total loss is spread thinner; **"strengthen" is the doubled pain** — 10 s at ×2 rate, with the same total loss as the base version. Dragon heads are not cheap, so even the weakest tier cuts you down to 1 max health the moment it lands. That is the intent, not a bug.
2. **Stew**: Suspicious Stew + dragon head → a fixed 5 s. Cheap, controllable, good as a consumable.
3. **Tipped arrows**: **we do not override vanilla**. Tipped arrows keep the vanilla 1/8 rule, so a 20-second potion made into an arrow is 2.5 seconds — enough to apply it, not enough to kill.

The fourth route is a trim: `Ender Avatar` (眼眸) applies it on hit, scaled by the damage — see §2.

**While you are being watched**

- When another mob **looks you straight in the eyes**, the remaining seconds **stop falling** — the clock holds and only resumes once the stare breaks. The test is 32 blocks, an unobstructed line of sight and a heading within about 10°; players always count, other mobs are judged by their head direction, and a carved pumpkin on the gazer's head does not count (the enderman rule). Damage and water damage still land while the clock holds.
- While watched you carry `Wavefunction Collapse` (波函数坍缩): retried every 0.5 seconds, but never restarted while the previous one is still playing, so it sounds like one continuous stretch — but this one is targeted: it is sent to the watched player only, nobody else hears it, and when the watched thing is a mob there is no sound at all. Every time damage adds seconds you hear `Glitch` (故障) once. Both have closed captions, so anyone with subtitles on can read them instead of hearing them.
- Each time you take damage (a legitimate hit — the one that adds seconds), the model bursts apart for an instant, about 0.6 seconds: first the whole skin turns into a black-and-purple checkerboard, then the body parts come apart, twist once and collapse once, and it is over. The old "one twitch every 2 seconds" is gone, and nothing lingers after the effect ends. Everyone sees the same burst.

---

## 5. Three new recipes in the smithing table

What you put in the template slot decides which recipe you get; the base slot takes the gear to process (it must already carry a trim).

| Recipe | Template slot | Base slot | Addition slot | Result |
|---|---|---|---|---|
| **Strip the trim** | 1 iron nugget | Trimmed gear | Empty | Clean gear, the trim is destroyed |
| **Strip and recover the template** | 1 iron nugget | Trimmed gear | Book and quill | Clean gear + the original template returned to your inventory |
| **Toggle effects** | 1 glass pane | Trimmed gear | Empty | The same piece, with its effects switched off / on |

- Stripping is **cheap and total**: the trim, the stamped attributes and the toggle state all go at once. It does not give the template back — for that use the second row, and the price is a book and quill. The template is returned the moment you take the result out, before the three slots are consumed.
- Which template comes back is decided by the template recipe matching the trim on your gear; a trim defined by a datapack also returns its own template correctly.
- **The toggle** is for people who like the look but do not want the effect: appearance, name and trim all stay, only the effects stop. Switching it back on **recalculates from the current data**, so if you edited the datapack while it was off, switching it on hands you the new numbers.
- These three are not in the recipe book; they are things you work out in front of the smithing table.

---

## 6. Two commands

Gamemaster permission required (OP level 2).

| Command | What it does |
|---|---|
| `/tre dump` | Lists the trim entries currently in effect, how many pieces counted, and the final aggregated value of each attribute. Use this to work out "why is my damage wrong". |
| `/tre reload` | Reloads the trim data and refreshes the cache, without quitting and rejoining. Run it once after editing a datapack. |

`/reload` reloads the trim data as well, with the same effect as `/tre reload`.

---

## 7. Config screen

There is exactly one option, stored in `config/trimeffectively.json`; you can also change it from the config screen in the mod list (the screen needs Cloth Config installed — plus Mod Menu on Fabric, whereas Cloth alone is enough on NeoForge):

| Option | Default | Notes |
|---|---|---|
| **Hijack template duplication recipes** | Off | Tries to make every template recipe cheaper (duplicating a template costs less). **Requires a datapack reload to take effect.** |

- This is a **local setting**: it applies when a world is opened or the datapack is reloaded.
- On a server this screen is read-only: it shows the server's setting, and the client cannot change it.

---

## 8. A few easy trip-ups

- **Mainhand and offhand count toward the piece count.** Four worn pieces plus the same trim on both hands = 6 shares. An offhand shield does not count; a shield cannot take a trim.
- **The pattern brings one set of effects and the material brings another, and both stack.** To push a single direction, go "four pieces of the same trim + four pieces of the same material" — but there are only four armour pieces, and the trim and the material are two separate things on the same item. A chestplate is both sentry and copper and gives both sets at once.
- **Every number lives in the datapack.** If a number looks absurd, that is the data, not the engine: edit the JSON, see [DATA.md](DATA.en.md).
- **A piece with its effects switched off does not count.** Switch one piece of a four-piece set off and the remaining three count as 3 — not "4 pieces but one share short".
