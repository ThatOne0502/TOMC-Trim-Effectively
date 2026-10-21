[English](INTERNALS.en.md) &nbsp;·&nbsp; **简体中文**

# TOMC-Trim Effectively 实现原理

> 写给同行和想纠错的开发者：这份文档说的是代码里到底怎么跑，不解释为什么这样选。
> 读完你能：定位一次求值发生在哪、为什么某些效果必须两份 mixin、`TrimStamp` 盖章在防什么、以及复现这些问题时该看哪。
> 设计取舍见 [设计理念](DESIGN.md)；数据格式见 [数据文档](DATA.md)。

坐标：Fabric / MC 1.21.11 / Java 21 / fabric-loom `remap` 插件 / mojmap（注意 1.21.11 起 `ResourceLocation` 叫 `Identifier`、`Holder` 在部分签名里是 `RegistryEntry`）/ 包名 `io.github.thatone0502.trimeffectively`。

---

## 1. 数据流

```
data/*/trimeffectively/tre_{pattern,material}/*.json
  └─ DynamicRegistries.registerSynced（数据包加载 + 热重载 + codec 校验 + 服务端→客户端同步）
       └─ TreEngine 每 10 tick：
            TrimEffects.load(access)   → 扁平索引 Map<trimId, List<Named>>（快照）
            AttributeEngine.load(access) → List<TrimEntry>
            遍历所有 LivingEntity：
              TrimSnapshot.of(entity)    → 这件实体身上「哪些部件带纹饰、各是哪条」
              TrimEffects.active(...)    → 命中 trigger 的 Active(entryId, entry, pieces)
              Value#eval(pieces, vars)   → 数字
              AttributeEngine.apply      → 修饰符
       事件型效果：各自的效果类在对应 hook 里调 TrimEffects.activeOfType(...)
```

两条不变量撑起全部：

1. **注册表只读，索引每轮重建。** 引擎每轮把两个注册表拍成不可变 `Index`，handler 从不直接读注册表。
2. **handler 看不到 `TrimEffect`，只看到 `Active`。** `Active` 里已经带了 `pieces`，所以效果类不需要知道「怎么数件数」。

---

## 2. 动态注册表：为什么一次拿到四件事

```java
public static final ResourceKey<Registry<TrimEffect>> TRE_PATTERN  = Tre.key("tre_pattern");
public static final ResourceKey<Registry<TrimEffect>> TRE_MATERIAL = Tre.key("tre_material");

DynamicRegistries.registerSynced(TRE_PATTERN,  TrimEffect.CODEC);
DynamicRegistries.registerSynced(TRE_MATERIAL, TrimEffect.CODEC);
```

- `registerSynced` 是硬要求：客户端要用定义来画 tooltip、算耐久条、渲染幽匿标注。不同步的话客户端只有空注册表。
- 目录规则由注册表 key 决定：`data/<pack>/trimeffectively/tre_pattern/<文件名>.json`。条目 id 必然是 `<pack>:<文件名>`，这就是「条目 id 不能等于 `minecraft:sentry`、绑定关系必须写进 JSON」的由来。
- 数据包路径规则还决定了覆盖语义：同 id 后加载者胜，整份替换，不合并。
- 上游噪声：`DynamicRegistrySetupCallback` 在 26.1+ 快照有回归（fabric-api#5149），与本版无关，但移植 26.x 时必须重测这块。

---

## 3. 四个角色的分工

| 类 | 输入 | 输出 | 不该做的事 |
|---|---|---|---|
| `TrimSnapshot` | 一个 `LivingEntity` | 带纹饰的部件列表（槽位 + `pattern` + `material` + 物品栈） | 不查注册表、不求值 |
| `TrimEffects` | 注册表 + 实体 | `Index`（扁平）、`List<Active>` | 不应用效果 |
| `AttributeEngine` | `List<TrimEntry>` + 实体 | 属性修饰符 | 不处理事件型效果 |
| 各 `*Effect` 类 | `Active` + 上下文 | 副作用（伤害、召唤、声音…） | 不数件数、不读注册表 |

`TrimSnapshot` 的两个细节：

- **关掉效果的那件不是一件。** `TrimStamp.enabled(stack) == false` 的物品在拍快照时就被丢掉——如果放到下游再过滤，件数、属性求和、`/tre dump` 会各错一次。
- `countByIdAndSlots(trimId, slots)` 按槽位组过滤（`armor` = 头胸腿脚，`hand` = 主副手，`any` = 全部），并且 `pattern == trimId || material == trimId` 都算命中。

`TrimEffects` 的三个细节：

- `Named.key()` 是 `entryId + "#" + index`。一个文件里有多条效果，它们共享文件 id；只用文件 id 做键会把第一条之后的全丢掉。
- 多条数据可以指向同一个 trim id，所以索引是 `Map<trimId, List<Named>>`，一条 trim 可以叠加好几份数据。
- **没有 `DamageSource` 就不判 `conditions`**：判不了的条件一律当不成立并打一次 warn。

---

## 4. 引擎节拍

```java
ServerTickEvents.END_SERVER_TICK.register(server -> {
    PortalTracker.flush(server);
    sweepInventories(server);                 // 每 tick，只处理一个槽
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

- **10 tick（0.5 秒）**：属性修饰符自己会持久，重算只是为了让「血量比例、速度、金组数」这类会变的量跟上。10 tick 让残血加速这类效果看起来是连续的，又不至于每 tick 走一遍全部实体。
- **每 200 tick（10 秒）`prune`**：忘掉已经不在任何 level 里的实体 UUID，`APPLIED` 表不会随「刷怪—死亡」无限长。
- `sweepInventories` 每 tick 只过**一个玩家背包里的一个槽位**，按游标轮转。它只做 `TrimStamp.reconcile`，用来采纳「盖章组件出现之前就存在」的旧物品；不扫世界里的容器（理由见 §5）。

---

## 5. `TrimStamp` 身份盖章

三个组件必须同进同退：

```
minecraft:trim            （原版）
trimeffectively:properties （本模组的盖章值）
trimeffectively:enabled    （关闭时写 false；缺失 = 开）
```

不变量：**盖章值必须属于物品上实际那套纹饰**。`reconcile(stack)` 做的就是这件事——读 `TRIM`，按 `(pattern, material, enabled)` 从数据重算一份，与现有盖章值比对：

- 相同 → 什么都不做（不写组件，不触发同步，客户端不重绘）；
- 不同 → 写新的；
- 没有 `TRIM` → 把三个组件一起删掉。

`force(stack)` 是不比对、直接写，只在「玩家明确要求重新计算」的地方用：纹饰开关重新打开、`/tre reload` 后强制刷新。

**为什么是身份比对而不是每次重算**

- 盖章值要能被离线读到：展示框里的装备、掉在地上的物品、别的模组翻背包，都没有穿戴者。
- 10 tick 一次无条件重写会让物品组件版本每轮变一次，客户端跟着重渲染 tooltip 与耐久条；顺带把玩家在铁砧/创造模式里手改过的同名属性抹掉。
- 代价：**旧物品不会自动升级**。三处补：引擎每 tick 的背包 sweep、打开物品栏时的 `ItemStack` hook、以及 `/tre reload` 的强制刷新。世界里没人碰的容器里的旧物品不会被扫到，这是有意的——一个物品只有在经过玩家手上的时候才值得被采纳。

---

## 6. 伤害结算链上的混入点

`Player#actuallyHurt` 是**整段重写并且不调 `super`** 的，所以 `LivingEntity#actuallyHurt` 上的注入对玩家不生效。于是同一组逻辑有两份：`LivingEntityMixin` 与 `PlayerActuallyHurtMixin`。`actuallyHurt` 是虚调用派发，一次受击只会走其中一份，不会双算。

单个方法内的顺序（玩家侧为例）：

| 顺序 | 注入 | 做什么 |
|---|---|---|
| 1 | `@ModifyVariable(HEAD, argsOnly)` on `DamageSource` | 如果受击者身上有 endphase：把来源换成 `trimeffectively:endphase`（保留 `directEntity` / `causingEntity`），已经是这个类型就跳过 |
| 2 | `@ModifyVariable(HEAD, argsOnly)` on `float amount` | `EndphaseEffect.onDamageTaken`：解离度 += `floor(amount)` 秒 |
| 3 | `@Inject(HEAD)` | 记下 `getHealth()`，供 TAIL 算实际掉血 |
| 4 | 原版方法体 | 护甲、附魔、吸收、`damage_in` 一类在这里被读取 |
| 5 | `@Inject(TAIL)` | `ThornsEffect.afterDamage`（用 HEAD 记的血量差算「实际受到」）、`SummonEffect.afterDamage`、`MobEffectApplyEffect.afterDamage` |

顺序是有语义的：**先改来源、再累解离、最后在 TAIL 里用真实掉血量结算反伤**。反伤用「实际掉血」而不是「传入 amount」，是因为后者在护甲减伤后仍会按原样反射出去，等于无视护甲。

`take_damage_before` 与 `take_damage_after` 这两个 trigger 就落在这条链的前后：`damage_in` 走 before（影响减伤），`thorns` / `summon` / `apply_mob_effect` 走 after（只在伤害真的发生之后才有意义）。

`damage_in` 复刻了抗性提升的两道闸：`#minecraft:bypasses_effects` 与 `#minecraft:bypasses_resistance` 标记的伤害直接跳过纹饰减伤——否则虚空、`/kill`、饥饿这类伤害会被减伤吃掉。

---

## 7. 战利品：为什么是 `@WrapMethod`

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

两个决策：

1. **必须挂 `LootContext` 重载。** 1.21.11 有两个 `getRandomItemsRaw`；所有公开入口都先建 `LootContext` 再调这个重载，`LootParams` 那个永远见不到一次独立的生成。
2. **必须包体，不能在 HEAD 注入。** `NestedLootTable#createItemStack` 会把同一个 context 再喂回这个方法，所以一次钓鱼是两层（`gameplay/fishing` 内嵌 `gameplay/fishing/fish`）。HEAD 注入时外层调用早已返回、嵌套深度回零，两层各投一次额外骰——这正是 0.1.0.43 那个「一次钓鱼两条鱼」的 bug。包体让 `enter()` 在整个方法体内有效，`finally` 保证异常也不会把计数器漏成「永久关闭」。

`LootRollEffect` 内部就是一个进入计数：`enter()` 返回自己是不是最外层，`exit()` 递减。额外骰本身只投一次，与嵌套深度无关。

---

## 8. `Value#eval` 的求值路径

```java
double product = 1.0;
for (String name : per) product *= variables.getOrDefault(name, 1.0);
if (power != 1.0 && product >= 0.0) product = Math.pow(product, power);
double result = value * product;
result *= switch (stack) {
    case "once" -> 1.0;
    case "full_set" -> stackCount >= 4 ? stackCount : 0.0;
    default -> stackCount;               // 也覆盖 "per_piece_capped"（未实现独立分支）
};
if (min != null) result = Math.max(min, result);
if (max != null) result = Math.min(max, result);
```

- `power` 只弯曲 `per` 的乘积，`value` 与件数因子仍是线性的。乘积为负时跳过幂运算（`Math.pow(负数, 0.5)` = NaN）。
- 变量缺失按 1.0：这是刻意的宽松，让一条数据在上下文不全时退化而不是消失。
- `stack` 的三个分支之外没有第四种行为：`per_piece_capped` 会落到 `default`，等于 `per_piece`。契约先留着，引擎没实现。
- `variables` 由 `AttributeEngine.variables` 统一构建（`health_ratio`、`missing_health_ratio`、`gear.armor`、`speed_mps`、`harmful_effect_count`、`on_fire`），事件侧再补 `attack_distance` / `damage_*` / 目标状态等。
- `speed_mps` 取 `SpeedTracker.peak(entity)`：最近 5 tick 的水平速度峰值，而不是瞬时值——瞬时值在碰撞、上坡、台阶上会瞬间归零，误差肉眼可见。

---

## 9. 属性修饰符的写法

```java
Identifier modifierId = Tre.id(path + (operation == ADD_VALUE ? "" : "_" + operation.getSerializedName()));
```

- 一个 `(属性, 条目, operation)` 一个修饰符，id 由条目路径派生，`add_multiplied_base` 之类会加后缀。
- 同 id 是**覆盖不是累加**，所以同一轮里同 id 的多条效果在 `wanted` 表里先 `merge` 相加，再一次性 `addOrReplacePermanentModifier`。
- 重算时删掉「上一轮有、这一轮没有」的 id，保留「值一模一样」的 id（跳过写入，避免客户端抖动）。
- `APPLIED` 记「这个实体上我们碰过哪些属性」，`prune` 用它清理离场实体；`clear` 在死亡时整体摘除。
- 判据：任何需要遍历实体修饰符列表来判断的逻辑都是设计错误——前缀 + 记账已经覆盖换装、重登、死亡、卸载。

`extra_jumps` 是作者糖：它在 `AttributeEngine` 里被翻译成真属性 `trimeffectively:extra_jumps` 的 `add_value` 修饰符，命令、其它模组、客户端都按普通属性看待。

---

## 10. 客户端只做表现

客户端参与的部分只有三处，全部不参与数值：

- **tooltip**（`TrimTooltip`）：按住可绑定键时，把原版 tooltip 里「纹饰名」「材料名」两行**原位替换**成效果名 + 描述；一行换一行，不新增行。数据显示靠 synced 注册表，不靠网络包。
- **声音标注**（`SoundAnnotations` + `SoundManagerAnnotateMixin` / `ServerLevelGameEventMixin`）：`annotate_sound` 只在本客户端渲染。振动粒子用 `ParticleTypes.VIBRATION` + `VibrationParticleOption(EntityPositionSource(本玩家), ticks)` 从声源指向玩家；文字是**客户端本地 spawn 的 `TextDisplay`**（不发给服务端），`billboard=CENTER`、`see_through=true`，每 tick 用带 alpha 的 `Style` 重写做渐隐。自定义渲染管线（1.21.9+ 的 submit 体系）成本太高，本地实体绕开了它。
- **配置页**（`TreConfigScreen` / `TreMenuIntegration`）：本地配置写 `config/trimeffectively.json`；联机时只读显示服务端的值（`TreConfigPayload` / `TreConfigSync`）。

`extra_jumps` 的落地重置有客户端移动判定，但服务端保留一次校验——纯客户端的话创造飞行、连跳、`onGround` 时序三者会互相打架。

---

## 11. 踩过的坑

| 症状 | 根因 | 去处 |
|---|---|---|
| 同一个玩家被扣两次血 / 掉落双份 | `Player#actuallyHurt` 是重写方法，`LivingEntity` 上的注入对玩家不生效（漏 hook 时是一半生效）；嵌套战利品表各投一次额外骰 | 双份 mixin；`@WrapMethod` + 深层计数 |
| 修饰符越叠越多、卸下不消失 | 原版按 id 存在 map 里，同 id 覆盖；早期按槽位拼 id 导致「同 id 天然叠加」不成立 | 聚合修饰符 + 前缀记账 |
| `durability_multiplier: 2` 变成 `true` | `DynamicOps#getBooleanValue` 的默认实现是 `getNumberValue(...).map(b -> b != 0)`，先试布尔就把 2 解成 true | 字面量解码顺序固定为数字 → 布尔 → 字符串 |
| 一次钓鱼两条鱼 | 见 §7 | — |
| 满减伤连虚空一起吃掉 | `damage_in` 是乘性通道，必须复刻 `#bypasses_*` 两道闸 | — |
| tooltip 每 10 tick 闪一下 | 修饰符值与上一轮相同却仍重写，物品组件版本变化触发重绘 | 值相同就跳过写入 |
| 未附字幕的声音标注显示空白 | 隐藏式字幕查不到 `subtitles.*` | 回退到声音 id 原文，走普通文本翻译 |
| 强化版药水一上就秒杀 | `delta = -(剩余秒数) × (amplifier+1)`，90 秒 ×2 直接超过 20 点基础生命 | 三档改 20 / 40 / 10 秒，总扣减对齐 |

**取证方法**（不用开游戏也能确认接口）：

```bash
# 找方法签名与字段
javap -p -c -classpath <loom 缓存里的 minecraft-merged.jar> net.minecraft.world.entity.LivingEntity
# 查某个方法到底调了谁（判断该挂哪个重载、有没有 super 调用）
javap -p -c ... net.minecraft.world.entity.player.Player | grep -A5 actuallyHurt
# 确认标签/谓词的真实字段名（1.21 删过 DamageSourcePredicate#is_projectile 这类字段）
unzip -p minecraft-merged.jar data/minecraft/tags/damage_type/is_projectile.json
```

`javap` 给不出的（mixin 目标是否被别的模组也注入过、注入后的字节码顺序），用 `-Dmixin.debug.export=true` 导出 `run/.mixin.out/class/` 再看反编译结果。这一步在排查「两份 mixin 只生效一份」时是唯一可靠的办法。

---

## 12. 工程侧

- 构建产物：`tomc-trimeffectively-<maj>.<min>.<rev>.<build>-fabric+mc<mcver>.jar`；`mod_version` 手改，`<build>` 由 `build-count.txt` 自增；`fabric.mod.json` 里的版本写到四段（`0.1.0.1`），文件名另拼，避免 loader 解析失败。
- 数据与代码分离：JSON 全在 `fabric/src/main/resources/data/trimeffectively/`，将来移植时抽到 `common/data/` 共享。
- 三条锻造台配方注册为 `PlacementInfo.NOT_PLACEABLE` 且 `isSpecial()`——它们是在台子前试出来的东西，不是配方书能排布的网格配方。
- 基础槽的 `Ingredient` 是延迟解析的（`Supplier` + 首次使用时记忆化）：配方解码发生在物品标签绑定之前，构造期查 `#minecraft:trimmable_armor` 会抛 `Missing tag`；记忆化存的是 tag 的活 `HolderSet`，所以数据包改标签仍然立刻生效。
