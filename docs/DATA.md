[English](DATA.en.md) &nbsp;·&nbsp; **简体中文**

# TOMC-Trim Effectively 数据文档

> 写给整合包作者、数据包作者、想联动本模组但不写 Java 的模组作者。
> 读完你能：知道数据放哪、写出能被加载的条目、用 `Value` 拼出数值、查 20 个在用 `type` 与 10 个在用 `trigger` 的语义，并对照附录里 29 条现成条目的真值改数字。
> 本文只讲「写什么」。引擎为什么这样算、代码里挂在哪，见 [实现原理](INTERNALS.md)；设计取舍见 [设计理念](DESIGN.md)。

---

## 1. 数据放哪

动态注册表 key 是 `trimeffectively:tre_pattern` 与 `trimeffectively:tre_material`，数据目录由注册表 key 的
`命名空间/路径` 决定：

| 注册表 | 目录 | 条目 id 示例 | 绑定字段 |
|---|---|---|---|
| `trimeffectively:tre_pattern` | `data/<pack>/trimeffectively/tre_pattern/<文件名>.json` | `trimeffectively:sentry` | `"pattern": "minecraft:sentry"` |
| `trimeffectively:tre_material` | `data/<pack>/trimeffectively/tre_material/<文件名>.json` | `trimeffectively:copper` | `"material": "minecraft:copper"` |

- **条目 id = `<pack>:<文件名>`**。`<pack>` 是数据包命名空间（本模组自带的数据是 `trimeffectively`）。
- **文件名只是标签**。一份数据服务哪条原版纹饰，由 JSON 里的 `pattern` / `material` 字段决定：
  `"pattern": "minecraft:sentry"`。字段接受数组，一条数据可以同时服务多条纹饰。
- 本模组自带的数据在 `data/trimeffectively/trimeffectively/tre_pattern/` 与 `.../tre_material/`，
  条目 id 是 `trimeffectively:sentry` 这类。
- `pattern` / `material` 写不存在的原版 id **不做存在性校验**：不报错，只是永远匹配不到（等价于没写）。
  匹配用的是物品上实际的 trim id。
- 模组自有内容也落在 `data/trimeffectively/` 下：伤害类型 `trimeffectively:endphase`、四个配方文件、
  一条交易/战利品相关 id。第三方（如 Trimmable Tools）加的纹饰，只要在**自己的命名空间**下丢一份
  `data/<你的命名空间>/trimeffectively/tre_pattern/<任意名>.json`、把 `pattern` 写对即可生效，本模组不需要改动。

---

## 2. 一个文件的骨架

```json
{
  "pattern": "minecraft:sentry",
  "effects": [ { "...": "..." } ]
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `pattern` / `material` | id 或 id 数组 | 按所在目录二选一（`tre_pattern/` 写 `pattern`，`tre_material/` 写 `material`）。数组 = 一条数据服务多个纹饰。 |
| `slots` | 可选，槽位或别名数组 | 默认 `["armor","hand"]`。别名：`armor` = 头胸腿脚，`hand` = 主副手，`any` = 全部。 |
| `effects` | 数组 | 本文件的全部效果；多条按顺序求值，同类数值先加后乘。 |

推导出的 lang key（以文件名 `long_shot.json` 为例，即条目 `trimeffectively:long_shot`）：

```
trim_effect.trimeffectively.long_shot.name  = 百步穿杨 / Long Shot
trim_effect.trimeffectively.long_shot.desc  = 与目标每距离 1 格，本次伤害 +5%…
```

改文件名会改条目 id，也会改这两个 key——这是覆盖别人数据与翻译时必须知道的一件事。

---

## 3. 效果对象

```json
{
  "type": "trimeffectively:damage_out",
  "trigger": "trimeffectively:attack_out",
  "amount": { "value": 0.05, "per": ["attack_distance"], "stack": "per_piece" },
  "conditions": []
}
```

| 字段 | 说明 |
|---|---|
| `type` | 效果类型，决定「做什么」。**必须带命名空间**（真注册表，裸名会被当成 `minecraft:`）。 |
| `trigger` | 何时。**省略 = `equipped`（常驻）**，属性类效果基本都省略。非注册表，但统一写 `trimeffectively:` 前缀，裸名也接受。 |
| `amount` / 其它数值字段 | 统一是 `Value`，见 §4。**所有数值字段一律 `Value` 形状，不写裸数**（`radius`、`count`、`duration` 也如此）。 |
| `operation` | **只有 `attribute` 类型有**，取值 `add_value` / `add_multiplied_base` / `add_multiplied_total`。其它类型不写——它们的 `amount` 本身就是「相对基准量的比例」（`0.05` = +5%、`-0.16` = -16%），符号即方向。 |
| `conditions` | 原版 `LootItemCondition` 数组（`entity_properties` / `damage_source_properties` / `location_check` / `all_of` / `inverted` / `value_check`…）。 |
| `client` | `true` 表示纯客户端效果，服务端只同步定义、不执行。 |

### 3.1 已核实的原版词表（对着 1.21.11 `minecraft-merged.jar` 查过）

- **属性 id 全部存在**：`minecraft:armor` `armor_toughness` `movement_speed` `attack_speed`
  `block_break_speed` `oxygen_bonus` `water_movement_efficiency` `luck` `knockback_resistance` `burning_time`。
- **本模组自建的属性**：`trimeffectively:crit_multiplier`（默认 1.5，范围 0~20，同步到客户端）与
  `trimeffectively:extra_jumps`（默认 0）。它们是**真属性**，命令、其它模组、客户端都按普通属性看待；
  写法与 `minecraft:` 属性完全一样（`attribute` 填 id，`operation` 填 `add_value`）。
- **伤害类型标签存在**：`minecraft:is_fire`、`minecraft:is_projectile`。
  1.21 起 `DamageSourcePredicate` **没有 `is_projectile` 这个布尔字段**了，要用标签谓词
  `{"tags": [{"id": "minecraft:is_projectile", "expected": false}]}`（`expected: false` = 必须**不**带这个标签，即近战）。
- `wither_immune_to` / `always_hurts_ender_dragons` 标签存在，且**不要把 endphase 加进去**（已定：不被凋灵、末影龙免疫）。

---

## 4. `Value` —— 数值、变量、叠加

```json
"amount": { "value": 0.05, "per": ["attack_distance"], "power": 1.0, "stack": "per_piece", "min": null, "max": null }
```

计算式：`final = value × Π(per 里各变量)^power × stackFactor`，再 `clamp(min,max)`。

| 字段 | 类型 | 说明 |
|---|---|---|
| `value` | 必填，数字 | 单件基准值。 |
| `per` | 可选，字符串数组 | 乘进去的上下文变量名，见下表。空/省略 = 纯常数。**任一变量缺失按 1.0 代入**。 |
| `power` | 可选，数字，默认 1 | 只作用在 `per` 的乘积上；乘积为负时跳过幂运算（避免 NaN）。`0.5` 就是开方。 |
| `stack` | 可选，默认 `per_piece` | 件数因子，见 §4.1。 |
| `min` / `max` | 可选，数字 | 求值后的钳制。本版全部条目都不写，机制保留。 |

`power` 只弯曲 `per` 的乘积：`value` 与件数因子仍按线性相乘。所以「随伤害开方增长」不需要表达式语言，
一个 `0.5` 就够；`per` 里的变量缺失时乘积退化成常数，开方后的条目也只是变成线性常数。

### 4.1 `stack`：四种叠法

| 值 | 语义 | 结果 |
|---|---|---|
| `per_piece`（**默认**） | × 当前生效件数 n（1~4） | 1 件 +5%，4 件 +20% |
| `per_piece_capped` | × min(n, `stack_cap`) | 由 `stack_cap` 封顶，`stack_cap` 是该写法配的同伴字段 |
| `once` | 有 ≥1 件即生效，数值不变 | 1 件和 4 件一样 |
| `full_set` | 仅 n == 4 时生效 | 差一件即归零 |

`stack` **可以写在效果级**（作该效果所有数值的默认），**也可以写在任意单个 `Value` 里覆盖**。
`vex.json` 就是这个用法：召唤数逐件叠（`per_piece`，四件套挨一下召四只），存活时长只随本次伤害走（`once`）。
两个 `Value` 各用各的 `stack`，互不牵连。

> `per_piece_capped` 的 `stack_cap` 目前只有契约，引擎侧尚未实现上限分支，写法会被当成 `per_piece`。
> 需要封顶时用 `max`（`final = value × Πper × n` 之后再钳），后果是钳制发生在乘件数之后，语义与「先封顶件数」不同。

### 4.2 上下文变量

| 变量 | 含义 |
|---|---|
| `attack_distance` | 攻击瞬间与目标的距离（格） |
| `damage_taken` / `damage_dealt` | 本次伤害数值（受击侧 / 输出侧） |
| `health_ratio` / `missing_health_ratio` | 0~1 |
| `speed_mps` | 水平速度 m/s，取该实体最近 5 tick 的峰值；站立时不计竖直分量 |
| `harmful_effect_count` | 非正面效果数量（**含着火**：着火算一项） |
| `gold_group_count` | 背包中金质物品「组数」 |
| `on_fire` | 0 / 1 |
| `gear.armor` | **所有生效部件自身护甲值之和** |

`gear.*` 是「对生效部件求和」的变量，与普通上下文量在数学上等价，只是来源不同。

常用组合：

- `共振应激`（材料·紫水晶）：`{ "value": 1.0, "per": ["missing_health_ratio"], "stack": "per_piece" }`，
  `operation` 取 `add_multiplied_base`，作用于 `movement_speed`。单件满血 +0%，残血趋近 +100%（即 200% 速度）；
  4 件残血趋近 +400%（即 500% 速度）。
- `百步穿杨`（纹饰·哨兵）：`{ "value": 0.05, "per": ["attack_distance"] }`。1 件 3 格 → +15%；4 件 4 格 → +80%。不设上限。
- `动能屏障`（纹饰·尖塔）：`{ "value": -0.01, "per": ["speed_mps"], "stack": "per_piece" }`。每 1 m/s 每件 -1%。

---

## 5. `type` 总表（20 个在用）

`type` 是注册表 id，**必须带命名空间**。下表「独有字段」列里，除 `trigger` / `conditions` / `stack` 外的字段名即 JSON 字段名。

| # | `type` | 语义 | 独有字段 |
|---|---|---|---|
| 1 | `trimeffectively:attribute` | 属性修饰符 | `attribute`（属性 id）、`operation`（`add_value` / `add_multiplied_base` / `add_multiplied_total`）、`amount`（`Value`） |
| 2 | `trimeffectively:damage_out` | 我方造成伤害的加成/削减 | `amount`（`Value`，百分比，0.05 = +5%） |
| 3 | `trimeffectively:damage_in` | 我方受到伤害的加成/削减 | `amount`（`Value`，负数 = 减免） |
| 4 | `trimeffectively:damage_type_override` | 改写本次伤害的伤害类型 | `damage_type`（伤害类型 id）、`keep_attacker`（bool，默认 `true`，**不能丢攻击者**否则死亡消息 `.player` 变体不触发） |
| 5 | `trimeffectively:summon` | 召唤生物 | `entity`、`count`（`Value`）、`lifetime`（`Value`，秒）、`inherit_equipment`（bool）、`attack_target`（`"attacker"` / `"victim"` / 省略） |
| 6 | `trimeffectively:extra_loot_roll` | 战利品额外 roll | `rolls`（`Value`） |
| 7 | `trimeffectively:apply_mob_effect` | 施加状态效果 | `effect`、`chance`（`Value`）、`duration`（`Value`，tick）、`amplifier`（`Value`）、`target`（`"victim"` / `"attacker"` / `"self"`） |
| 8 | `trimeffectively:item_property` | 装备自身属性（`scope: item`，**盖章进物品组件**，见 §9） | `properties`（开放 map：键 → 布尔/数字/字符串） |
| 9 | `trimeffectively:trade_price` | 交易价格调整 | `amount`（`Value`，负数 = 更便宜） |
| 10 | `trimeffectively:trade_keep` | 交易不消耗次数 | `chance`（`Value`） |
| 11 | `trimeffectively:breed_cooldown` | 配种冷却调整 | `amount`（`Value`，负数 = 更快） |
| 12 | `trimeffectively:mute_sound` | 声音静音 | `chance`（`Value`）、`scope`（`"both"` / `"audible"` / `"vibration"`） |
| 13 | `trimeffectively:annotate_sound` | 客户端声音标注 | `radius`（`Value`，格）、`client`（bool，必须是 `true`）、`exclude_own`（bool）、`blocked_by`（方块标签）、`particle`、`label`（见 §7） |
| 14 | `trimeffectively:harmful_effect_rate` | 负面效果倒计时速率 | `amount`（`Value`，`2.0` = +200%） |
| 15 | `trimeffectively:crit_damage` | 暴击伤害加成 | `amount`（`Value`）。现成条目已改用属性 `trimeffectively:crit_multiplier`，见附录 A.1 的 `snout.json` |
| 16 | `trimeffectively:xp_gain` | 经验获取调整 | `amount`（`Value`） |
| 17 | `trimeffectively:thorns` | 近战反伤 | `amount`（`Value`，`0.5` = 反射受到伤害的 50%） |
| 18 | `trimeffectively:piglin_neutral` | 猪灵中立 | `also_brutes`（bool） |
| 19 | `trimeffectively:extra_jumps` | 额外跳跃次数 | `count`（`Value`）。实现上写成真属性 `trimeffectively:extra_jumps` |
| 20 | `trimeffectively:invulnerability_window` | 受击无敌帧时长调整 | `amount`（`Value`，负数 = 缩短） |

> 一共 20 个 `type`。下面是三次退役，编号已随之顺延；旧稿里「21 种」的计数包含的是这些已删项。
>
> 0.1.0.29 删除了 `trimeffectively:damage_protection`（保护点数通道）。尖塔改用现成的 `damage_in`，见附录 A.1 的 `spire.json` 行。
>
> 0.1.0.31 又退役了 `trimeffectively:fire_duration` 与 `ignite` 触发器：下界合金的着火时长改用原版 `minecraft:burning_time` 属性（`add_multiplied_base`，每件 -0.30），与火焰保护走同一条乘算通道。
>
> 0.1.0.48 删除了 `trimeffectively:anvil_penalty`：`keep_repair_cost` 早已改由 `item_property` 盖章实现（见附录 A.1 的 `shaper.json` 行），这个类型没有任何接点，属死代码。上表编号随之顺延。

新增一个 `type` 只发生在代码侧（一个类 + 注册一行），数据包不能注册新类型。

---

## 6. `trigger` 词表

闭枚举，写 `trimeffectively:` 前缀，也接受裸名：

`equipped`（省略时的默认值，常驻）· `attack_out` · `take_damage_before` · `take_damage_after` ·
`kill` · `loot_roll` · `trade` · `breed` · `xp_gain` · `sound_emitted`

10 个值，没有别名，全部有实现。`loot_roll` 覆盖容器/钓鱼/考古几个入口，`kill` 是击杀专用；其余都是事件。

`trigger` 字段本身是自由 `Identifier`：写别的值不会报错，但永远不会被触发。`death` / `tick` /
`item_break` / `heal` 这类名字留作将来的语义位，当前没有实现，写进去等于不生效。

---

## 7. 三个复杂字段的形状

`annotate_sound.label`：

```json
{ "subtitle": true, "visible_ticks": 40, "fade_ticks": 60, "see_through": true, "billboard": "center" }
```

`subtitle: true` = 取该声音的隐藏式字幕文本；取不到则回退到声音 id 原文（`minecraft:entity.zombie.ambient`）。

`apply_mob_effect.chance` / `trade_keep.chance` / `mute_sound.chance`：`Value`，结果在引擎内钳到 `[0,1]`。
这是防非法值的硬底线（时长 ≥ 0 tick、概率 ∈ [0,1]、倍率 ≥ -1），不属于平衡钳制，也不写进 JSON。

`blocked_by`：方块标签 id，**不带 `#`**（`#` 只是原版自己的标签引用写法）。`"minecraft:wool"` =
声源与玩家之间隔着羊毛就不标注，与监守者听觉过滤对齐。

---

## 8. 校验、加载与热重载

```
加载：data/*/trimeffectively/tre_{pattern,material}/**.json → 动态注册表（含热重载、服务端→客户端同步）
每次求值：
  1. 取实体全部生效部件（头/胸/腿/脚/主/副手，凡带 TRIM 组件）
  2. 按 pattern/material 匹配到条目 → 得到「生效部件集合」
  3. 对每条 effect：trigger 匹配？conditions 全过？→ 求 Value
  4. 汇总：同类数值先加后乘 → 应用
```

- **编解码即校验**：JSON 写错时错误信息会给出文件与字段路径，加载失败不影响其它文件。
- **热重载**：`/reload` 与 `/tre reload` 都重载这两张注册表；条目立即按新值重算（已盖章的物品例外，见 §9）。
- **缺变量按 1.0**：`per` 里写了没有的变量不会报错，只是退化成乘 1。
- **条件缺上下文即跳过**：带 `conditions` 的条目在没有伤害来源可供判定时按「不成立」处理，不会被当成满足。
- **`appliesTo` 不校验**：`pattern` / `material` 指向不存在的原版 id 只是永远匹配不到。
- **调试**：`/tre dump`（gamemaster 权限）列出当前生效的 pattern / material 条目、件数与每个属性的最终聚合值；
  `/tre reload`（同权限）重载注册表并刷新缓存。

---

## 9. `item_property` 的 `properties`

`item_property` 回答的是**关于物品本身**的问题——「这物品有多耐久」「它会不会烧掉」「猪灵想不想要它」。
答案在物品被打上纹饰的那一刻**盖章写进物品组件**，之后所有提问都只是读组件；掉落物、展示框、耐久条、
别的模组翻背包时都能读到，不需要穿戴者在线。盖章与撤销的时机见 [实现原理](INTERNALS.md)。

现成的四个键：

| 键 | 类型 | 谁在用 |
|---|---|---|
| `durability_multiplier` | 数字 | 钻石（`2`：耐久 ×2） |
| `fire_resistant` | 布尔 | 下界合金（`true`：物品不被烧毁） |
| `piglin_loved` | 布尔 | 金锭（`true`：猪灵视作金质） |
| `repair_cost` | 数字 | 预留给精益求精 |

`properties` 是开放 map，加新键不需要改存储层；但**每个键仍需要 Java 侧有人去读**，这是物理上限——
写一个没人读的键，组件里会有值，游戏里什么也不会发生。

---

## 10. 字面量的解码顺序

`item_property` 的 `properties` 值是一个联合类型（数字 / 布尔 / 字符串）。解码顺序必须是 **数字 → 布尔 → 字符串**：
`DynamicOps#getBooleanValue` 的默认实现是 `getNumberValue(input).map(n -> n.byteValue() != 0)`，
先试布尔就会把 `"durability_multiplier": 2` 解成 `true`（0.1.0.31 之前就是这个 bug，乘数恒为 1.0）。
反过来，真布尔不会被 `getNumberValue` 接受，所以数字优先的顺序是无损的。任何「数字还是布尔」的字面量都照这条办。

已经盖过章的旧物品不会被自动修正，需要重新纹饰或 `/tre reload` 强制重算。

---

## 11. 第三方：加自己的数据 / 整份覆盖

**加**（不碰本模组的文件）：

1. 在你自己命名空间下新建 `data/<你的命名空间>/trimeffectively/tre_pattern/<任意名>.json`；
2. `pattern` 写 `minecraft:<纹饰 id>`，或直接写你模组新增的纹饰 id；
3. 条目 id 就是 `<你的命名空间>:<文件名>`，lang key 随之推导；
4. 想让物品真的挂上纹饰，靠的是原版 `minecraft:trim` 组件——原版锻造模板的 base 槽校验
   `#minecraft:trimmable_armor` 物品标签，往该标签里加东西就能让工具/自定义装备打纹饰。本模组只读 `TRIM` 组件，
   **不需要任何兼容代码**。

**整份覆盖**（换成你自己的数值）：

- 数据包加载时同 id 后加载者胜。把 `trimeffectively:sentry` 这一份用同名文件、同 pack 命名空间重新提供，
  或另起 `data/trimeffectively/trimeffectively/tre_pattern/sentry.json` 放进更高优先级的数据包，即可整份替换。
- 覆盖是**整份**的：文件里的 `effects` 数组整体替换，不与原文件合并。想留一条改一条，就复制原文件再改。
- 想禁用一个条目：提供一份 `effects: []` 的同 id 文件，或把你的数据包排到前面覆盖掉它。
- 覆盖后条目 id 不变、lang key 不变，玩家看到的仍是原效果名；改名就等于换了一条效果，旧翻译失效。

---

## 12. 完整样本

### 12.1 `sentry.json` —— 事件 + 距离变量 + 逐件叠加

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

1 件、3 格 → +15%；4 件、4 格 → +80%。**不设上限**（已裁定）。

### 12.2 `eye.json` —— 概率 + 开方时长

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

`duration` 单位是 tick。**单件时效 = √(2 × 本次伤害) 秒**：`power: 0.5` 开方，`value` 是 `20 × √2 = 28.284…`
（`duration` 以 tick 计，20 tick = 1 秒），`stack: per_piece` 再乘件数，`min: 0` 兜住负伤害。
一次 6 点伤害：1 件 √12 ≈ 3.5 秒，4 件 ≈ 13.9 秒；20 点伤害 4 件 ≈ 25.3 秒；50 点伤害 4 件 = 40 秒。
概率与时长各自独立：`chance` 逐件叠（4 件 = 每击 40%），`duration` 也可以写成 `once` 的固定值。

### 12.3 `ward.json` —— 客户端渲染 + 伤害类型覆盖

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

- `client: true` 是**必填**，不是可选优化：`annotate_sound` 只在本客户端渲染、不广播。
- 时间轴：0–2s 全不透明，2s 起 3 秒渐隐，5s 消失 → `visible_ticks 40` + `fade_ticks 60`。
- `keep_attacker` 省略即 `true`，务必保留——丢了攻击者，死亡消息的 `.player` 变体不会触发。

### 12.4 `netherite.json` —— 一条文件里三种族

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

| # | 条目 | `stack` | 4 件时 | 说明 |
|---|---|---|---|---|
| 1 | 火伤 -16% | `per_piece` | -64% | 无钳制 |
| 2 | 着火时长 -30% | `per_piece` | -120% | 无钳制 → 4 件完全不点着。走原版 `minecraft:burning_time` 属性乘算，与火焰保护叠加，属性下限 0 自行防负 |
| 3 | 物品不被烧毁 | — | — | 无 `trigger`、无 `Value`：`scope: item` |

第 3 条是唯一用 `item` 作用域的条目。它没有件数概念——一件就是一件，所以不写 `stack` 也不会有歧义。
现成文件里这条写成 `"properties": { "fire_resistant": true }`，两种写法等价。

---

## 附录 A：29 条条目的装配

路径约定：`data/trimeffectively/trimeffectively/tre_pattern/<文件名>.json` 与 `.../tre_material/<文件名>.json`。
「关键数值」列省略 `"stack"` 时即默认 `per_piece`；`Value` 写成 `{value, per}` 形式。

### A.1 纹饰（18）

| 文件 | `type` | `trigger` | 关键数值与字段 | 备注 |
|---|---|---|---|---|
| `sentry.json` | `damage_out` | `attack_out` | `amount {value:0.05, per:["attack_distance"], stack:"per_piece"}` | 每 1 格 +5%，逐件叠，不设上限 |
| `vex.json` | `summon` | `take_damage_after` | `entity:"minecraft:vex"`、`count {value:1, stack:"per_piece"}`、`lifetime {value:1.0, per:["damage_taken"], stack:"once"}`、`inherit_equipment:true`、`attack_target:"attacker"` | 四件套挨一下召四只；伤害点数 = 存活秒数，时长不叠 |
| `wild.json` | `extra_loot_roll` ×2 | `kill` / `loot_roll` | 两条都是 `rolls {value:1, stack:"per_piece"}` | 击杀与开箱/钓鱼/考古各一条；同一次战利品生成只投一轮骰 |
| `coast.json` | `attribute` ×2 | `equipped` | ①`minecraft:oxygen_bonus` / `add_value` / `{value:2}` ②`minecraft:water_movement_efficiency` / `add_value` / `{value:0.667}` | 0.667 = 深海探索者 II 同值 |
| `dune.json` | `invulnerability_window` | `attack_out` | `amount {value:-0.30, stack:"per_piece"}` | 命中后把目标的受击无敌帧压到 70% |
| `wayfinder.json` | `attribute` | `equipped` | `minecraft:movement_speed` / `add_multiplied_base` / `{value:0.5}`，`conditions:[{"condition":"trimeffectively:facing_respawn"}]` | 条件按引擎节拍重算 |
| `raiser.json` | `breed_cooldown` | `breed` | `amount {value:-0.20, stack:"per_piece"}` | 冷却 6000 → 4800 tick |
| `shaper.json` | `item_property` | 常驻 | `properties {keep_repair_cost: true}` | 盖在物品上，铁砧惩罚不再累加 |
| `host.json` | `trade_keep` | `trade` | `chance {value:0.2, stack:"per_piece"}` | 20% 概率不消耗交易次数 |
| `ward.json` | `damage_type_override` + `annotate_sound` | `attack_out` / `sound_emitted` | ①`damage_type:"minecraft:sonic_boom"`、`keep_attacker:true` ②`radius {value:8}`、`client:true`、`exclude_own:true`、`blocked_by:"minecraft:wool"`、`particle:"minecraft:vibration"`、`label` 见 §7 | 声音标注只在本客户端渲染 |
| `silence.json` | `mute_sound` | `sound_emitted` | `chance {value:0.3, stack:"per_piece"}`、`scope:"both"` | 别人也听不到，振动体系也检测不到 |
| `tide.json` | `harmful_effect_rate` | `equipped` | `amount {value:2.0, stack:"per_piece"}` | +200% 倒计时速率 |
| `snout.json` | `attribute` | `equipped` | `trimeffectively:crit_multiplier` / `add_value` / `{value:0.01, per:["gold_group_count"], stack:"per_piece"}` | 每「组」金质物品 +1% 暴击倍率；属性默认 1.5 |
| `rib.json` | `damage_out` | `attack_out` | `amount {value:0.20, per:["harmful_effect_count"], stack:"per_piece"}` | 每种非正面效果 +20%，着火算一种 |
| `eye.json` | `apply_mob_effect` | `attack_out` | `effect:"trimeffectively:endphase"`、`chance {value:0.1, stack:"per_piece"}`、`duration {value:28.284271247461902, per:["damage_dealt"], power:0.5, stack:"per_piece", min:0}`、`amplifier {value:0, stack:"once"}`、`target:"victim"` | 单件时效 = √(2×伤害) 秒，再乘件数，不封顶；概率逐件叠 |
| `spire.json` | `damage_in` | `take_damage_before` | `amount {value:-0.01, per:["speed_mps"], stack:"per_piece"}` | 每 1 m/s 每件 -1%；分数累过 -1.0 时结果钳到 0 |
| `bolt.json` | `attribute` | `equipped` | `minecraft:knockback_resistance` / `add_value` / `{value:0.15}` | 内部 0.15 = tooltip 显示 +1.5 |
| `flow.json` | `extra_jumps` / 属性 `trimeffectively:extra_jumps` | `equipped` | `count {value:1, stack:"per_piece"}` | 额外跳跃次数 |

### A.2 材料（11）

| 文件 | `type` | `trigger` | 关键数值与字段 | 备注 |
|---|---|---|---|---|
| `amethyst.json` | `attribute` | `equipped` | `minecraft:movement_speed` / `add_multiplied_base` / `{value:1.0, per:["missing_health_ratio"], stack:"per_piece"}` | 4 件残血趋近 +400%（500% 速度） |
| `copper.json` | `attribute` | `equipped` | `minecraft:armor_toughness` / `add_value` / `{value:2}`（stack 省略 = `per_piece`） | 两件铜饰 → +4 |
| `diamond.json` | `item_property` | （无） | `properties:{durability_multiplier:2}` | `scope: item`，盖章进组件（0.1.0.31 前该数字曾被错解成布尔 `true`，乘数恒 1.0） |
| `emerald.json` | `trade_price` | `trade` | `amount {value:-0.10, stack:"per_piece"}` | 价格 -10% |
| `gold.json` | `item_property` + `piglin_neutral` | （无）/ `equipped` | ①`properties:{piglin_loved:true}` ②`also_brutes:true` | 已经是金质装备时连蛮兵也中立 |
| `iron.json` | `attribute` | `equipped` | `minecraft:armor` / `add_value` / `{value:0.5, per:["gear.armor"], stack:"once"}` | **唯一 `stack: once` 的例外**：`gear.armor` 本身已是「带铁饰部件护甲之和」，再乘件数会变成平方 |
| `lapis.json` | `attribute` | `equipped` | `minecraft:luck` / `add_value` / `{value:1}` | |
| `quartz.json` | `thorns` | `take_damage_after` | `amount {value:0.5, stack:"per_piece"}` | 仅近战：`conditions` 用标签谓词 `{"tags":[{"id":"minecraft:is_projectile","expected":false}]}`（1.21 没有 `is_projectile` 布尔字段） |
| `netherite.json` | `damage_in` + `attribute` + `item_property` | `take_damage_before` / （无） / （无） | ①`{-0.16}` + 火标签条件 ②`minecraft:burning_time` 的 `add_multiplied_base` `{-0.30}` ③`properties:{fire_resistant:true}` | 见 §12.4 完整样本 |
| `redstone.json` | `attribute` ×2 | `equipped` | ①`minecraft:attack_speed` / `add_value` / `{value:1}` ②`minecraft:block_break_speed` / `add_value` / `{value:1}` | |
| `resin.json` | `xp_gain` | `xp_gain` | `amount {value:1.0, stack:"per_piece"}` | +100% |

### A.3 全表统一约定

1. 所有 `Value` 的 `stack` 一律 `per_piece`，**除 `iron.json` 那一条例外**（理由见上表）。
2. 本版一律不写 `min` / `max`（`eye.json` 的 `min: 0` 是防负伤害的硬底线，不是平衡钳制）。需要调节时在具体条目上加 `"max": -0.9` 之类，不改引擎。
3. `rib` 的「每种非正面效果伤害 +20%（含着火）」——着火算一项，所以 `harmful_effect_count` 包含「着火中」，不只有 `MobEffect`。
4. `gold` 的 `also_brutes` 是布尔开关，「是否已是金质装备」是运行时判断、不是数据；`also_brutes:false` 表示只对猪灵中立。
5. `damage_in` 复刻了抗性提升的两道闸：`#minecraft:bypasses_effects` 与 `#minecraft:bypasses_resistance` 标记的伤害**完全不受纹饰影响**（否则满减伤会连虚空和 `/kill` 一起吃掉）。
6. 尖塔的减伤分数可以累过 `-1.0`，最终把结果钳到 0，所以 ≥100% 就是「这一下完全不掉血」。`speed_mps` 取最近 5 tick 峰值，站立时不计竖直分量。
