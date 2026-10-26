# 发布页文案 / Store Page Copy

对外发布名 **`[TRE] 纹以载道 Trim Effectively`**。这只是发布平台上的展示名，游戏内的模组名仍是
`fabric.mod.json` / `neoforge.mods.toml` 里的 `TOMC-Trim Effectively`，两者不冲突，也不需要同步。

**一句话总结**

- 英文（直接复用游戏内 `description` 原文）
  **Unique effects for trim smithing — each pattern or material carries its own way.**
- 中文（上面那句的译法，保留了「各有各的门道」这层意思）
  **为纹饰锻造带来独一无二的效果——每个图案、每种材料，都有自己的门道。**

下面三篇同源，按平台任选一篇整段复制：**第一篇**纯中文（MC百科这类中文平台），**第二篇**纯英文
（CurseForge / Modrinth），**第三篇**中英逐段穿插（需要双语并排的场合）。

三篇都按 [MC百科 32160](https://www.mcmod.cn/class/32160.html) 已发布页的排版写：**分节标题就是普通一行字**，
不用 `#` 也不用加粗；列表项以「；」收尾、项间空一行；不写安装步骤与构建方法（各平台有自己的版本表），
也不写实现细节（那在 [docs/](docs/README.md)）。粘进编辑器后就是原页那种朴素排面，不需要再调。

## 元数据（照抄）

| 字段 | 值 |
|---|---|
| 发布名 | `[TRE] 纹以载道 Trim Effectively` |
| 英文名 | `Trim Effectively` |
| 中文名 | `纹以载道` |
| Mod ID | `trimeffectively` |
| 平台 / 版本 | Fabric · Minecraft 1.21.11；NeoForge · Minecraft 26.1.2 |
| 运行环境 | 客户端需装，服务端需装（两端版本必须一致） |
| 依赖 | Fabric 版：Fabric API（必须），Mod Menu + Cloth Config（可选，仅配置页需要）；NeoForge 版：Cloth Config（可选，仅配置页需要） |
| 许可 | GPL-3.0-or-later（开源） |
| 作者 | ThatOne |
| 仓库 | https://github.com/ThatOne0502/TOMC-Trim-Effectively |
| 分类 | MC百科：道具 / 食物 / 生存 / 国创；CurseForge：装备 / 冒险 / 实用；Modrinth：Equipment、Adventure and RPG、Utility |

> MC百科现有条目（32160）只登记了 Fabric · 1.21.11。NeoForge · 26.1.2 要单独新增一个版本，
> 文件名 `tomc-trimeffectively-0.2.0.1-neoforge+mc26.1.2.jar`。

---

## 一、纯中文（MC百科，直接贴）

纹饰不该只是花纹

原版的纹饰很漂亮，但缺乏让玩家收集、运用的充分理由。本模组让纹饰**真正生效**：每种**盔甲纹饰**及其**锻造原材料**，均携带不同的增益，两者同时生效。

纹饰提供的效果通常是**可以叠加**的，多穿同一种纹饰/原材料并不会浪费，但也可以通过不同的**排列组合**来实现更多元的混合效果，这是本模组的主要玩法。

盔甲纹饰

原版目前的**所有**纹饰均有各自增益，且**无生态位重叠**、**上下位替代**，部分内容如下：

- 荒野：击杀、开箱、钓鱼时战利品表可以被额外调用一次（每件）；

- 眼眸：攻击时概率给目标挂上"相位解离"（效果见下），概率和时长随伤害提升；

- 尖塔：按移动速度减免受到的伤害，每件每 1 m/s 减 1%（无上限）；

- 幽静：每件 30% 概率把你发出的声音吞掉（客户端仍会听见，但不广播）；

- 监守：体验成为监守者的感觉——听声辩位（不是发光效果，是真实的声源标注），且打人更疼；

- 恼鬼：挨打后每件召一只继承你装备的恼鬼，存在时长等于你受到的伤害点数；

- 涡流：每件多一层多段跳。

锻造原材料

原版目前的**所有**原材料均有各自增益，同样**无生态位重叠**、**上下位替代**，部分内容如下：

- 下界合金：减少火焰伤害与燃烧时间，且物品实体不会被烧毁；

- 钻石：把该物品的耐久上限翻倍；

- 紫水晶：残血时逐件加速；

- 树脂：获得经验翻倍；

- 铜：每件 +2 盔甲韧性；

- 金：可以被**猪灵**视作金质，如果物品已经是金质，则也可以使得**猪灵蛮兵**中立；

完整的效果列表可以查看 GitHub 中的开发文档。

"相位解离"

相位解离是本模组添加的新状态效果，会**使得实体的最大生命值减少当前的"相位解离"秒数，受到伤害会增加对应伤害的秒数，凋灵、末影龙也无法免疫本效果。**

被其它生物直视眼睛时，它的剩余秒数不再回落——只有视线移开，倒计时才继续走。而每一次合法受伤，都会伴随一声"故障"与一次故障：模型短暂错位，或者整张纹理换成黑紫棋盘。

剔除、拓印与装裱

1. **剔除**：以铁粒为锻造模板，可以将装备已有的纹饰、原材料清空；

2. **拓印**：以铁粒为锻造模板、书与笔为原材料，可以在清空之后返还锻造模板；

3. **装裱**：以玻璃板为锻造模板，可以将某物品的已有纹饰、原材料无效化，使其回归原版的装饰效果，也可以随时以同配方启用效果。

数据驱动

**全部**效果都是数据驱动的：以 JSON 格式定义在数据包中，可以整份替换、追加或关掉；其他创作者如需联动，可以参考 GitHub 中的开发文档。

---

## 二、English only（CurseForge / Modrinth）

A trim should be more than a pattern

Vanilla trims are pretty but give players no real reason to collect or use them. This mod makes trims **actually work**: every **armor trim** and every **smithing material** carries its own bonus, and both are active at the same time.

Trim effects **stack**, so several pieces of the same trim or material are never wasted — but the point of the mod is the **combinations**, and that is where the play is.

Armor trims

Every vanilla trim has its own bonus, with **no overlapping niches and no strict upgrades**, a few of them:

- Wild: loot tables may be rolled one extra time on kills, container openings and fishing (per piece);

- Eye: attacking has a chance to apply "Endphase" to the target (see below); both the chance and the duration scale with damage;

- Spire: incoming damage is reduced by your movement speed, 1% per m/s per piece, with no cap;

- Silence: each piece has a 30% chance to swallow a sound you make (still audible to you, no longer broadcast);

- Ward: hear where things are — a real annotation of the sound source, not a glowing outline — and hit harder;

- Vex: after being hit, each piece summons a vex that inherits your equipment and lasts as many seconds as the damage you took;

- Flow: one extra multi-stage jump per piece.

Smithing materials

Every vanilla material has its own bonus, likewise **without overlapping niches or strict upgrades**, a few of them:

- Netherite: less fire damage and shorter burn time, and the item entity itself will not burn up;

- Diamond: doubles that item's durability;

- Amethyst: move faster while hurt, one step per piece;

- Resin: double experience;

- Copper: +2 armor toughness per piece;

- Gold: **piglins** treat the item as gold, and if it already is gold, **piglin brutes** will not attack you.

The full effect list lives in the development documents on GitHub.

"Endphase"

Endphase is a new status effect added by this mod: it **lowers the entity's max health by the current number of Endphase seconds, and damage taken adds that many seconds back on. The Wither and the Ender Dragon cannot ignore it.**

While another creature looks you straight in the eyes, the remaining seconds stop falling — the count resumes only once the gaze breaks. And every legitimate hit lands with a "Glitch" sound and a glitch of its own: the model briefly slips out of place, or its whole texture turns into a black-and-magenta checkerboard.

Stripping, copying and framing

1. **Stripping**: an iron nugget as the smithing template clears a piece's existing trim and material;

2. **Copying**: an iron nugget as the template plus a book and quill returns the smithing template after clearing;

3. **Framing**: a glass pane as the template disables a piece's trim and material, returning it to vanilla decoration — and the same recipe turns the effects back on.

Data driven

**Every** effect is data driven: defined as JSON in a datapack, replaceable, extendable or switchable off as a whole; other creators who want to hook in can consult the development documents on GitHub.

---

## 三、中英对照（逐段穿插）

纹饰不该只是花纹 / A trim should be more than a pattern

原版的纹饰很漂亮，但缺乏让玩家收集、运用的充分理由。本模组让纹饰**真正生效**：每种**盔甲纹饰**及其**锻造原材料**，均携带不同的增益，两者同时生效。
Vanilla trims are pretty but give players no reason to collect or use them. This mod makes trims **actually work**: every **armor trim** and every **smithing material** carries its own bonus, and both are active at the same time.

纹饰提供的效果通常是**可以叠加**的，多穿同一种纹饰/原材料并不会浪费，但也可以通过不同的**排列组合**来实现更多元的混合效果，这是本模组的主要玩法。
Trim effects **stack**, so several pieces of the same trim or material are never wasted — but the point of the mod is the **combinations**, and that is where the play is.

盔甲纹饰 / Armor trims

原版目前的**所有**纹饰均有各自增益，且**无生态位重叠**、**上下位替代**，部分内容如下：
Every vanilla trim has its own bonus, with **no overlapping niches and no strict upgrades**, a few of them:

- 荒野：击杀、开箱、钓鱼时战利品表可以被额外调用一次（每件）；
  Wild: loot tables may be rolled one extra time on kills, container openings and fishing (per piece);

- 眼眸：攻击时概率给目标挂上"相位解离"，概率和时长随伤害提升；
  Eye: attacking has a chance to apply "Endphase"; chance and duration scale with damage;

- 尖塔：按移动速度减免受到的伤害，每件每 1 m/s 减 1%（无上限）；
  Spire: incoming damage is reduced by movement speed, 1% per m/s per piece, no cap;

- 幽静：每件 30% 概率把你发出的声音吞掉（客户端仍会听见，但不广播）；
  Silence: 30% chance per piece to swallow a sound you make (audible to you, not broadcast);

- 监守：听声辩位（真实的声源标注，不是发光），且打人更疼；
  Ward: hear where things are (a real annotation of the source, not a glow), and hit harder;

- 恼鬼：挨打后每件召一只继承你装备的恼鬼，存在时长等于你受到的伤害点数；
  Vex: after being hit, each piece summons a vex that inherits your gear and lasts as many seconds as the damage taken;

- 涡流：每件多一层多段跳。
  Flow: one extra multi-stage jump per piece.

锻造原材料 / Smithing materials

原版目前的**所有**原材料均有各自增益，同样**无生态位重叠**、**上下位替代**，部分内容如下：
Every vanilla material has its own bonus, likewise **without overlapping niches or strict upgrades**, a few of them:

- 下界合金：减少火焰伤害与燃烧时间，物品实体不会被烧毁；
  Netherite: less fire damage, shorter burn time, and the item entity will not burn up;

- 钻石：把该物品的耐久上限翻倍；
  Diamond: doubles that item's durability;

- 紫水晶：残血时逐件加速；
  Amethyst: faster movement while hurt, one step per piece;

- 树脂：获得经验翻倍；
  Resin: double experience;

- 铜：每件 +2 盔甲韧性；
  Copper: +2 armor toughness per piece;

- 金：可以被**猪灵**视作金质，物品本身是金质时也会让**猪灵蛮兵**中立；
  Gold: **piglins** treat it as gold, and when the item already is gold, **piglin brutes** stay neutral.

完整的效果列表可以查看 GitHub 中的开发文档。
The full effect list lives in the development documents on GitHub.

"相位解离" / "Endphase"

相位解离是本模组添加的新状态效果，会**使得实体的最大生命值减少当前的"相位解离"秒数，受到伤害会增加对应伤害的秒数，凋灵、末影龙也无法免疫本效果。**
Endphase is a new status effect: it **lowers the entity's max health by the current number of Endphase seconds, and damage taken adds that many seconds back on. The Wither and the Ender Dragon cannot ignore it.**

被其它生物直视眼睛时，它的剩余秒数不再回落——只有视线移开，倒计时才继续走。而每一次合法受伤，都会伴随一声"故障"与一次故障：模型短暂错位，或者整张纹理换成黑紫棋盘。
While another creature looks you in the eyes the remaining seconds stop falling — the count resumes only once the gaze breaks. Every legitimate hit lands with a "Glitch" sound and a glitch of its own: the model slips out of place, or its texture turns into a black-and-magenta checkerboard.

剔除、拓印与装裱 / Stripping, copying and framing

1. **剔除**：以铁粒为锻造模板，可以将装备已有的纹饰、原材料清空；
   **Stripping**: an iron nugget as the template clears a piece's trim and material;

2. **拓印**：以铁粒为锻造模板、书与笔为原材料，可以在清空之后返还锻造模板；
   **Copying**: an iron nugget as the template plus a book and quill returns the template after clearing;

3. **装裱**：以玻璃板为锻造模板，可以将某物品的已有纹饰、原材料无效化，使其回归原版的装饰效果，也可以随时以同配方启用效果。
   **Framing**: a glass pane as the template disables a piece's effects, returning it to vanilla decoration — and the same recipe turns them back on.

数据驱动 / Data driven

**全部**效果都是数据驱动的：以 JSON 格式定义在数据包中，可以整份替换、追加或关掉；其他创作者如需联动，可以参考 GitHub 中的开发文档。
**Every** effect is data driven: JSON in a datapack, replaceable, extendable or switchable off as a whole; other creators who want to hook in can consult the development documents on GitHub.

---

## 改版时同步这几处

新版本发布时，三篇正文里需要跟着改的只有三类东西：**新增的纹饰/材料**（加一条列表项）、**数值变化**
（改对应那一句的数字）、**新增的给药途径或机制**（改「相位解离」那两段）。效果的全量清单不写进来——
它属于 [docs/CONTENT.md](docs/CONTENT.md)，正文只给「部分内容如下」。
