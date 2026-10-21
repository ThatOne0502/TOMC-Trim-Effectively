# 发布页文案 / Store Page Copy

对外发布名 **`[TRE] 纹以载道 Trim Effectively`**。这只是发布平台上的展示名，游戏内的模组名仍是
`fabric.mod.json` 里的 `TOMC-Trim Effectively`，两者不冲突，也不需要同步。

**一句话总结**

- 英文（直接复用游戏内 `description` 原文）
  **Unique effects for trim smithing — each pattern or material carries its own way.**
- 中文（上面那句的译法，保留了「各有各的门道」这层意思）
  **为纹饰锻造带来独一无二的效果——每个图案、每种材料，都有自己的门道。**

下面三篇正文同源，按平台任选其一复制：**第一篇**中英逐段穿插（通用），**第二篇**纯英（CurseForge / Modrinth），
**第三篇**纯中（MC百科等中文平台）。三篇都不写安装步骤与构建方法（各平台有自己的版本表），也不写实现细节
（那在 [docs/](docs/README.md)）。

## 元数据（照抄）

| 字段 | 值 |
|---|---|
| 发布名 | `[TRE] 纹以载道 Trim Effectively` |
| 英文名 | `Trim Effectively` |
| 中文名 | `纹以载道` |
| Mod ID | `trimeffectively` |
| 平台 / 版本 | Fabric · Minecraft 1.21.11 |
| 运行环境 | 客户端需装，服务端需装（两端版本必须一致） |
| 依赖 | Fabric API（必须）；Mod Menu + Cloth Config（可选，仅配置页需要） |
| 许可 | GPL-3.0-or-later |
| 作者 | ThatOne |
| 仓库 | （待填 GitHub 地址） |
| 分类 | 装备 / 冒险 / 实用（CurseForge）；Equipment、Adventure and RPG、Utility（Modrinth） |

---

## 一、中英对照（逐段穿插）

**纹饰不该只是花纹。 / A trim should be more than a pattern.**

原版的纹饰很漂亮，也很无用。一套钻石纹饰和一套铜纹饰除了颜色，没有任何区别——你把模板按在胸甲上，得到的只是一层花纹。
In vanilla a trim is beautiful and useless. A diamond trim and a copper trim differ only in colour: press a template onto a chestplate and all you get is a pattern.

这个模组让纹饰真正生效：**图案**决定一件装备给你什么样的能力，**材料**决定它的底子，两者同时生效。
This mod makes the trim do something: the **pattern** decides what the gear does for you, the **material** decides what it is made of, and both apply at once.

同一个纹饰只算一份，**穿几件就是几份**。四件荒野不是「效果变强」，而是真的多投四次战利品；四件钻石不是「一件更耐用」，而是四件各自翻一倍。取下纹饰不会毁掉装备，也不会有旧效果赖在新纹饰上。
One entry counts once; **how many pieces you wear is how many copies you get**. Four `wild` pieces really do roll loot four extra times, and four `diamond` pieces each double their own durability. Stripping a trim never destroys the item, and no stale effect clings to a new trim.

### 图案 / Patterns

18 个图案各有各的路子，部分内容如下：/ Eighteen patterns, some of them:

- **荒野 / Wild**：击杀与开箱时按件数多投战利品，四件套收一竿能得 5 条鱼。/ Extra loot rolls per piece on kills and containers; a full set pulls 5 fish per cast.
- **眼眸 / Eye**：命中时每件 10% 概率给目标挂上 `相位解离`，时长 = √(2 × 本次伤害) 秒 × 件数，一刀 8 点伤害就是每件 4 秒。/ On hit, a 10%-per-piece chance to apply `Endphase` for √(2 × damage) seconds per piece — an 8-damage hit is 4 seconds on each piece.
- **尖塔 / Spire**：按移动速度减免受到的伤害，每件每 1 m/s 减 1%。/ Incoming damage reduced by movement speed: 1% per m/s on every piece.
- **幽匿 / Silence**：每件 30% 概率把你发出的声音吞掉，走位与挖掘更难被听见。/ A 30%-per-piece chance to swallow the sounds you make, so mining and walking carry less.
- **监守 / Ward**：纯客户端表现——振动粒子，加上只给你自己看的青色底板标注，同时改写造成的伤害类型。/ Client-side only: vibration particles and a cyan label only you can read, plus a rewritten damage type.
- **恼鬼 / Vex**：挨打后每件召一只继承你装备的恼鬼，存在时长等于你受到的伤害点数。/ Taking a hit summons one vex per piece wearing your gear, alive for as many seconds as damage taken.
- **涡流 / Flow**：每件多一段跳跃，配合风弹能连着上台阶。/ One extra jump per piece, enough to chain up a cliff with a wind charge.

### 材料 / Materials

11 种材料各管一件事，按件数叠：/ Eleven materials, each answering one question, all stacking per piece:

- **下界合金 / Netherite**：每件 −30% `burning_time`（四件完全不点着）、−16% 受到的伤害，装备本身也烧不毁。/ −30% `burning_time` per piece (four pieces never burn), −16% damage taken, and the item itself cannot burn.
- **钻石 / Diamond**：每件把耐久上限翻倍。/ Every piece doubles the durability cap.
- **紫水晶 / Amethyst**：残血时逐件加速，四件濒死趋近 +400%。/ The closer to death, the faster you move; a full set approaches +400%.
- **树脂 / Resin**：每件多一份经验。/ A full share of extra experience per piece.
- **铜 / Copper**：每件 +2 盔甲韧性。/ +2 armour toughness per piece.
- **青金石 / Lapis**：每件 +1 幸运。/ +1 luck per piece.
- **铁、金、绿宝石、石英、红石 / Iron, Gold, Emerald, Quartz, Redstone**：各有分工，详见游戏内提示与仓库文档。/ Each does its own job; the item tooltip and the repository docs have the numbers.

### 龙首与 `相位解离`（`Endphase`）/ The dragon head and `Endphase`

龙首是这套系统里最贵的一味料，也是最不讲道理的一个：给它上药，最大生命立刻被压到 1，落水受伤，屏幕边缘泛品红。它能靠三条路线进入游戏——**药水**（普通 20 秒 / 延长 40 秒 / 增强 10 秒）、**谜之炖菜**（5 秒）、**药箭**（沿用原版 1/8，普通药箭 2.5 秒）。它和监守一样会改写伤害类型：被它打死的人，死亡消息里写的是它。
The dragon head is the most expensive ingredient and the least reasonable: apply its potion and your max health is pinned to 1, water hurts, the screen turns magenta. It enters the game three ways — **potions** (20 s / 40 s extended / 10 s strong), **suspicious stew** (5 s), and **tipped arrows** (vanilla's own 1/8, so 2.5 s). Like `ward`, it rewrites the damage type: kill someone with it and the death message says so.

### 锻造台三式 / Three smithing recipes

- **取下纹饰 / Strip**：把纹饰从装备上剥下来，装备留下。/ Take the trim off, keep the item.
- **取下并返还 / Strip and return**：剥下来的同时归还模板（代价是一本书与笔）。/ Take it off and get the template back, for the price of a book and quill.
- **纹饰开关 / Toggle**：单独关掉某一件的效果——关掉的件既不算件数，也不给效果。/ Switch one piece's effect off; a disabled piece neither counts toward the stack nor grants anything.

### 杂项 / Also

- 全部效果都是**数据**：18 个图案与 11 种材料共 29 条条目写在 JSON 里，数据包可以整份替换、追加或关掉；别的模组新增的纹饰，只要丢一份数据就自动生效。
  Every effect is **data**: 18 patterns and 11 materials, 29 entries in JSON, loaded into a synced dynamic registry. A datapack can replace, extend or disable any of it, and a trim added by another mod starts working the moment it ships a data file.
- 附带 **Mod Menu + Cloth Config** 配置页，服务端配置会同步给客户端；连他人服务器时只读。
  An optional **Mod Menu + Cloth Config** screen; server settings sync to the client and go read-only on someone else's server.
- `/tre dump` 与 `/tre reload` 两条 gamemaster 命令，用来查当前生效的条目、以及让旧物品按新数据自愈。
  `/tre dump` and `/tre reload` (gamemaster) to inspect what is active and to heal old items after a data change.
- **纹饰决定内容，材料决定内容，两者可同时叠**：一件胸甲可以既是哨兵又是铜，两套效果一起给。
  A piece can carry both a pattern and a material; one chestplate can be sentry *and* copper, and both apply.

**目前只面向 Minecraft 1.21.11 · Fabric，处于早期开发阶段**：内容与数据格式仍会调整，升级前请备份存档。
**Minecraft 1.21.11 · Fabric only, early development.** Content and data format may still change; back up your world before updating.

> 本模组在开发过程中使用了 AI 辅助（DeepSeek Harness，deepseek-flash）。代码、文档与设计并非全部经过人工审查与测试，取舍与最终责任在作者。
> Developed with assistance from AI (DeepSeek Harness, deepseek-flash). Not all code, documentation and design have been reviewed and tested by humans; the design decisions and the final responsibility are the author's.

---

## 二、English only

**A trim should be more than a pattern.**

In vanilla a trim is beautiful and useless. A diamond trim and a copper trim differ only in colour: press a template onto a chestplate and all you get is a pattern. This mod makes the trim do something — the **pattern** decides what the gear does for you, the **material** decides what it is made of, and both apply at once.

One entry counts once; **how many pieces you wear is how many copies you get**. Four `wild` pieces really do roll loot four extra times, and four `diamond` pieces each double their own durability. Stripping a trim never destroys the item, and no stale effect ever clings to a new trim.

### Patterns

Eighteen patterns, some of them:

- **Wild** — extra loot rolls per piece on kills and containers; a full set pulls 5 fish per cast.
- **Eye** — on hit, a 10%-per-piece chance to apply `Endphase` for √(2 × damage) seconds per piece: an 8-damage hit is 4 seconds on each piece.
- **Spire** — incoming damage reduced by movement speed: 1% per m/s on every piece.
- **Silence** — a 30%-per-piece chance to swallow the sounds you make, so mining and walking carry less.
- **Ward** — client-side only: vibration particles and a cyan label only you can read, plus a rewritten damage type.
- **Vex** — taking a hit summons one vex per piece wearing your gear, alive for as many seconds as damage taken.
- **Flow** — one extra jump per piece, enough to chain up a cliff with a wind charge.

### Materials

Eleven materials, each answering one question, all stacking per piece:

- **Netherite** — −30% `burning_time` per piece (four pieces never burn), −16% damage taken, and the item itself cannot burn.
- **Diamond** — every piece doubles the durability cap.
- **Amethyst** — the closer to death, the faster you move; a full set approaches +400%.
- **Resin** — a full share of extra experience per piece.
- **Copper** — +2 armour toughness per piece.
- **Lapis** — +1 luck per piece.
- **Iron, gold, emerald, quartz, redstone** — each does its own job; the tooltip and the repository docs have the numbers.

### The dragon head and `Endphase`

The most expensive ingredient is also the least reasonable: apply its potion and your max health is pinned to 1, water hurts, the screen turns magenta. It enters the game three ways — **potions** (20 s / 40 s extended / 10 s strong), **suspicious stew** (5 s), and **tipped arrows** (vanilla's own 1/8, so 2.5 s). Like `ward`, it rewrites the damage type: kill someone with it and the death message says so.

### Three smithing recipes

- **Strip** — take the trim off, keep the item.
- **Strip and return** — take it off and get the template back, for the price of a book and quill.
- **Toggle** — switch one piece's effect off; a disabled piece neither counts toward the stack nor grants anything.

### Also

- Every effect is **data**: 18 patterns and 11 materials, 29 entries in JSON, loaded into a synced dynamic registry. A datapack can replace, extend or disable any of it, and a trim added by another mod starts working the moment it ships a data file.
- An optional **Mod Menu + Cloth Config** screen; server settings sync to the client and go read-only on someone else's server.
- `/tre dump` and `/tre reload` (gamemaster) to inspect what is active and to heal old items after a data change.
- A piece can carry both a pattern and a material; one chestplate can be sentry *and* copper, and both apply.

**Minecraft 1.21.11 · Fabric only, early development.** Content and data format may still change; back up your world before updating.

> Developed with assistance from AI (DeepSeek Harness, deepseek-flash). Not all code, documentation and design have been reviewed and tested by humans; the design decisions and the final responsibility are the author's.

---

## 三、纯中文

**纹饰不该只是花纹。**

原版的纹饰很漂亮，也很无用。一套钻石纹饰和一套铜纹饰除了颜色，没有任何区别——你把模板按在胸甲上，得到的只是一层花纹。这个模组让纹饰真正生效：**图案**决定一件装备给你什么样的能力，**材料**决定它的底子，两者同时生效。

同一个纹饰只算一份，**穿几件就是几份**。四件荒野不是「效果变强」，而是真的多投四次战利品；四件钻石不是「一件更耐用」，而是四件各自翻一倍。取下纹饰不会毁掉装备，也不会有旧效果赖在新纹饰上。

### 图案

18 个图案各有各的路子，部分内容如下：

- **荒野**：击杀与开箱时按件数多投战利品，四件套收一竿能得 5 条鱼；
- **眼眸**：命中时每件 10% 概率给目标挂上 `相位解离`，时长 = √(2 × 本次伤害) 秒 × 件数，一刀 8 点伤害就是每件 4 秒；
- **尖塔**：按移动速度减免受到的伤害，每件每 1 m/s 减 1%；
- **幽匿**：每件 30% 概率把你发出的声音吞掉，走位与挖掘更难被听见；
- **监守**：纯客户端表现——振动粒子，加上只给你自己看的青色底板标注，同时改写造成的伤害类型；
- **恼鬼**：挨打后每件召一只继承你装备的恼鬼，存在时长等于你受到的伤害点数；
- **涡流**：每件多一段跳跃，配合风弹能连着上台阶。

### 材料

11 种材料各管一件事，按件数叠：

- **下界合金**：每件 −30% `burning_time`（四件完全不点着）、−16% 受到的伤害，装备本身也烧不毁；
- **钻石**：每件把耐久上限翻倍；
- **紫水晶**：残血时逐件加速，四件濒死趋近 +400%；
- **树脂**：每件多一份经验；
- **铜**：每件 +2 盔甲韧性；
- **青金石**：每件 +1 幸运；
- **铁**、**金**、**绿宝石**、**石英**、**红石** 各有分工，详见游戏内提示与仓库文档。

### 龙首与 `相位解离`

龙首是这套系统里最贵的一味料，也是最不讲道理的一个：给它上药，最大生命立刻被压到 1，落水受伤，屏幕边缘泛品红。它能靠三条路线进入游戏——**药水**（普通 20 秒 / 延长 40 秒 / 增强 10 秒）、**谜之炖菜**（5 秒）、**药箭**（沿用原版 1/8，普通药箭 2.5 秒）。它和监守一样会改写伤害类型：被它打死的人，死亡消息里写的是它。

### 锻造台三式

- **取下纹饰**：把纹饰从装备上剥下来，装备留下；
- **取下并返还**：剥下来的同时归还模板（代价是一本书与笔）；
- **纹饰开关**：单独关掉某一件的效果——关掉的件既不算件数，也不给效果。

### 杂项

- 全部效果都是**数据**：18 个图案与 11 种材料共 29 条条目写在 JSON 里，数据包可以整份替换、追加或关掉；别的模组新增的纹饰，只要丢一份数据就自动生效；
- 附带 **Mod Menu + Cloth Config** 配置页，服务端配置会同步给客户端；连他人服务器时只读；
- `/tre dump` 与 `/tre reload` 两条 gamemaster 命令，用来查当前生效的条目、以及让旧物品按新数据自愈；
- 一件装备可以同时带图案与材料两套效果，一件胸甲既可以是哨兵也可以是铜。

**目前只面向 Minecraft 1.21.11 · Fabric，处于早期开发阶段**：内容与数据格式仍会调整，升级前请备份存档。

> 本模组在开发过程中使用了 AI 辅助（DeepSeek Harness，deepseek-flash）。代码、文档与设计并非全部经过人工审查与测试，取舍与最终责任在作者。
