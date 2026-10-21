[English](STATUS.en.md) &nbsp;·&nbsp; **简体中文**

# 状态表：TOMC-Trim Effectively 0.1.0.53

> 本文件是变更记录，只记版本之间改了什么；结构说明见 [README](README.md) 与其它文档。

（0.1.0.26 ~ 0.1.0.31 的说明已并入本表；更早的版本历史见 git 之外的发布说明。0.1.0.32/33 处理的是「重新纹饰不刷新效果」——组件现在记住纹饰身份，换纹饰即强制重算；0.1.0.34 给幽匿标注加了底板与幽匿青字色；0.1.0.35 加了模板复制配方的劫持；0.1.0.36 把配置搬进 Mod Menu + Cloth Config 的配置页，并加了服务端 → 客户端的只读同步；0.1.0.37 修正只读判定——主菜单也能改，只有连他人服务器才置灰；0.1.0.40 加了锻造台三式配方、纹饰无效化开关与空跃风弹特效；0.1.0.44 把荒野的嵌套战利品表修对；0.1.0.45 定下 Endphase 的三条给药路线：药水 20/40/10 秒，药箭沿用原版的 1/8、炖菜固定 5 秒；0.1.0.46 回退了药箭时长的拦截，并把眼眸纹饰的时效改成「伤害 × 件数」；0.1.0.47 把那条时效改成开方曲线（√(2×伤害) 秒/件），`Value` 增加可选 `power` 字段；0.1.0.49 做发布前清理——删掉从未接点的 `anvil_penalty`、所有实验性探针日志与死代码，`gradle.properties` 里写死的 JDK 路径改成注释，补齐 README / LICENSE / .gitattributes；0.1.0.50 更正了文档里写错的数据目录契约——数据在 `data/<pack>/trimeffectively/tre_{pattern,material}/`，条目 id 是 `<pack>:<文件名>`，绑定哪条原版纹饰由 JSON 的 `pattern`/`material` 字段决定；0.1.0.52 把文档按读者拆成四份，并重写发布页文案；0.1.0.53 定下对外发布名 `[TRE] 纹以载道 Trim Effectively`，发布文案改成三篇同源，全部详细文档补齐英文版。）

## 本版改了什么

### 1. 材料：钻石的耐久乘数一直等于 1（解码顺序的锅）

`data/EffectEntry.java` 里那个 `Literal`（`item_property` 的 `properties` 值的联合类型）原先按 **布尔 → 数字 → 字符串** 的顺序去试。但 `DynamicOps#getBooleanValue` 的默认实现是「拿得到数字就当成非零」——它内部就是 `getNumberValue(input).map(n -> n.byteValue() != 0)`。于是 JSON 里的 `"durability_multiplier": 2` 被 `Codec.BOOL` 接走、变成 `true`，`number()` 再 `asNumber()` 得 `1.0`，乘数恒等于 1。玩家用 `data get` 看到 `durability_multiplier: 1b` 正是这个 `BoolTag(true)`（SNBT 里布尔就写作 `1b`），`durability_multiplier: 1b` 与 `durability_multiplier: true` 是同一件东西。

顺序改为 **数字 → 布尔 → 字符串**（先试 `Codec.DOUBLE`）。真布尔不会被 `getNumberValue` 接住，所以 `fire_resistant: true` 这类仍然解成布尔。

注意：**已经盖过章的旧物品不会被自动修正**——`TrimStamp.reconcile` 只在缺组件时盖章，不会重算已有的。要让旧胸甲从 `true` 变回数字 2，需要重新纹饰，或在游戏里跑一次强制重算（`/tre reload` 会强制重盖章）。新纹饰的物品直接就是对的。

1184 = 592 × 2，是修复后该看到的数字（`ItemStackEffectsMixin` 在 `ItemStack#getMaxDamage` 的 `RETURN` 上乘 `floor(基础值 × 乘数)`，下限 1）。

### 2. 材料：下界合金的着火时长改走原版 `minecraft:burning_time`

原版就有这个属性（`Attributes.BURNING_TIME`，默认 1.0，范围 0~1024，`Sentiment.NEGATIVE`，由 `LivingEntity.createLivingAttributes()` 给所有生物注册），火焰保护就是靠它：`data/minecraft/enchantment/fire_protection.json` 用 `operation: add_multiplied_base`、每级 `-0.15`（4 级 = -0.6）。

唯一读取点是 `LivingEntity#igniteForTicks(int)`：

```java
super.igniteForTicks(Mth.ceil(ticks * getAttributeValue(Attributes.BURNING_TIME)));
```

乘算发生在写入 fire-ticks 字段**之前**，所以只要能改属性就够了，不需要任何注入。

因此本版把 `trimeffectively:fire_duration`（22 号类型）与 `ignite` 触发器一起退役，`netherite.json` 的那一条换成：

```json
{ "type": "trimeffectively:attribute",
  "attribute": "minecraft:burning_time",
  "operation": "add_multiplied_base",
  "amount": { "value": -0.30, "stack": "per_piece" } }
```

每件 -30%，四件 -120% → 属性值被钳到下限 0 → 完全不点着，与原来自研类型的上限行为一致，但现在和火焰保护走同一条乘算通道、彼此叠加，而不再是我们从外面跟原版抢同一个数字。`effect/FireDurationEffect.java` 删除，`mixin/EntityMixin.java` 里的 `setRemainingFireTicks` 注入删除（该类保留，仍负责 `SpeedTracker` 采样），`DamageEffects` 去掉 `TYPE_FIRE_DURATION` / `TRIGGER_IGNITE`。

### 3. 重新纹饰后旧效果不消失（组件不认纹饰身份）

0.1.0.32/33。同一件物品反复锻造时，原版是**就地覆写** `minecraft:trim` —— 图案和材质换掉了，组件还是那个组件。我们的巡检原先只看「有没有纹饰组件」「有没有盖章」，两者都在就认为同步，于是钻石翻倍耐久的物品重锻成石英之后，`durability_multiplier: 2` 还赖在物品上。

修法不是「取件时先删再生成」（那只能兜住锻造台一条路，别的模组改纹饰还是漏），而是**让盖章记住它是给哪套纹饰盖的**：`trimeffectively:trim_properties` 的内容从裸 map 变成 `{pattern, material, values}`，两个 id 就是盖章时读到的 `minecraft:trim`。巡检比对当前纹饰的两个 id 与章上的两个 id，**不一致就整份丢弃、按数据包重新算**，不做合并 —— 旧值对新纹饰没有任何意义。

- 比对是两个 `Identifier` 相等，不查数据包，所以随装备读取做也没有成本。
- 纹饰为 `null`（没有纹饰）→ 删组件；组件为 `null`（有纹饰没章）→ 盖章。这两个方向仍是原来那条不变量。
- 两个 id 任一取不到（Holder 不由注册表持有）→ 不动，否则每次读都会重算一次。
- 旧版本写下的组件没有这两个 id → 解码后身份为空 → 判为「不可证明同步」→ 下次读到就按数据包重算一次。旧物品因此**自愈**，不需要 `/tre reload`（这正是 0.1.0.31 那条「旧物品不会自动修正」的补丁）。
- 数据包改版仍不追溯：换数据包不改纹饰身份，章还是旧的。只有换纹饰才强制重算。

验证用的是临时探针（已删除），`runServer` 日志：编码出 `{material:"trimeffectively:diamond",pattern:"trimeffectively:sentry",values:{durability_multiplier:2.0d}}`；回读身份与 values 全部还原且 `2.0` 仍是 double；旧形态 `{durability_multiplier:2}` 解出 `pattern=null, material=null, values={}`，即触发重算。
### 4. 尖塔：改成直接百分比减伤，不设上限

上一版把尖塔接进原版保护点数（`EnchantmentHelper#getDamageProtection`），天花板被原版钉死在 20 点 = 80%。本版**整条保护点数通道拆掉**：`effect/DamageProtectionEffect.java`、`mixin/EnchantmentHelperMixin.java`、`trimeffectively:damage_protection` 这个类型全部删除（类型表 23 → 22）。

尖塔改用现成的 `damage_in`：

```json
{ "type": "trimeffectively:damage_in",
  "trigger": "trimeffectively:take_damage_before",
  "amount": { "value": -0.01, "per": ["speed_mps"], "stack": "per_piece" } }
```

每 1 m/s 每件减 1%，可以一直累过 100%（那时结果钳到 0，这一下完全不掉血）。

原版抗性提升的实现（`LivingEntity#getDamageAfterMagicAbsorb` 字节码）：

```
if (source.is(BYPASSES_EFFECTS)) return amount;
if (hasEffect(RESISTANCE) && !source.is(BYPASSES_RESISTANCE)) {
    int i = (amplifier + 1) * 5;
    amount = Math.max(amount * (25 - i) / 25.0f, 0.0f);   // 钳到 0
}
if (amount <= 0) return 0;
if (source.is(BYPASSES_ENCHANTMENTS)) return amount;
return CombatRules.getDamageAfterMagicAbsorb(amount, EnchantmentHelper.getDamageProtection(...));
```

也就是说抗性提升本身就是"一个乘性百分比 + 钳到 0"，天花板是 `24 级 → 0`，而不是原版附魔保护那条 20 点曲线。我们改成同一种形态，于是：

- 乘性减伤与护甲**可以交换顺序**（乘积不变），我们的钩子在 `hurtServer` 头部、护甲之前，结果与抗性提升在护甲之后结算一致。
- 钳到 0 的语义天然由 `DamageEffects.compute` 末行的 `scaled <= 0 ? 0` 提供。
- `DamageInEffect` 补上抗性的两道闸：`#minecraft:bypasses_effects` 与 `#minecraft:bypasses_resistance` 的伤害完全不受纹饰影响。**这一步是必需的**——我们挂的层比原版抗性更早，少了这道判断，满减伤会连虚空和 `/kill` 一起吃掉。

`speed_mps` 取自实体 `Motion`（`SpeedTracker` 每 tick 记录 `getDeltaMovement()`，取最近 5 tick 的峰值 ×20 得 m/s）：碰撞在伤害结算前就清零了速度，瞬时值会恒等于静止；落地那一帧的竖直速度也只有窗口能留住。站立时竖直分量不计（否则被取消的重力步会给站立者记 ≈1.6 m/s）。

### 5. 猪灵蛮兵：把仇恨的两端都掐掉

上一版在 `AbstractPiglin#getTarget` 上返回 `null`，结果"举斧—放下"的循环还在。原因是这个出口根本不在仇恨的链路上：蛮兵的目标是 brain 的 `ATTACK_TARGET` 记忆，**写它的一端**是空活动里的 `StartAttacking`（喂数据的是 `PiglinBruteAi#findNearestValidAttackTarget`），**清它的一端**是战斗活动里的 `StopAttackingIfTargetInvalid`（判断用的是 `PiglinBruteAi#isNearestValidAttackTarget`），而战斗活动一停还会 `addActivityAndRemoveMemoryWhenStopped` 再抹一次。

上一版只让"清"的一端说"这目标没了"，于是：

1. `StopAttackingIfTargetInvalid` 抹掉记忆 → 手臂放下；
2. 空活动的 `StartAttacking` 用的是另一个函数（看 `ANGRY_AT` / `NEAREST_VISIBLE_ATTACKABLE_PLAYER` / `NEAREST_VISIBLE_NEMESIS`），下一 tick 又把玩家重新选回来 → 手臂举起；
3. 回到 1。

`setAggressive(brain.hasMemoryValue(ATTACK_TARGET))` 每 tick 跟着这个记忆翻面，所以看得见的正是每 tick 抬手又放下。

现在 `mixin/PiglinBruteAiMixin.java` 同时掐两端：`findNearestValidAttackTarget` 的 `RETURN` 上把被中立化的玩家变成 `Optional.empty()`（记忆永远写不进来），`isNearestValidAttackTarget` 的 `HEAD` 上回 `false`（已经写进去的那份被清掉）。记忆永久不存在 → 蛮兵停在空活动、保持默认手臂姿势，与"从没敌视过你"的玩家眼里完全一样。

`mixin/AbstractPiglinTargetMixin.java` 已删除：它既是这个循环的成因，也再没有存在的必要。

### 6. `SmithingMenuMixin` 之前根本没注册

`mixin/SmithingMenuMixin.java` 一直在源码里、一直能编译，但 `trimeffectively.mixins.json` 的名单里没有它，所以从未生效（锻造台上取件时盖章这件设计其实没跑）。本版补进名单，并用 `javap` 核对了 `SmithingMenu#onTake(Player, ItemStack)` 的 `protected void` 签名。

### 7. 玩家身上的「受伤后」效果：`Player#actuallyHurt` 不调 super

`Player#actuallyHurt(ServerLevel, DamageSource, float)` 是完整重写：护甲减免、附魔减免、吸收伤害、扣血都是自己再做一遍，**从不 `invokespecial LivingEntity.actuallyHurt`**。而 `actuallyHurt` 是虚派发，所以挂在 `LivingEntity#actuallyHurt` 上的注入对玩家一次都不会执行。

受影响的：恼鬼召唤、荆棘反伤、Endphase 累计与伤害源替换、命中给目标上毒。新增 `mixin/PlayerActuallyHurtMixin.java`（`@Mixin(Player.class)`）镜像同样五个钩子。虚派发保证每次受伤只跑其中一份。

伤害类效果（`damage_out` / `damage_in` / `crit_damage` / `damage_type_override`）不受此影响：`Player#hurtServer` 走 `invokespecial Avatar.hurtServer`，最终落到 `LivingEntity#hurtServer`，那条注入有效。

### 8. 同一文件里的多条效果被去重吞掉

`TrimEffects.collect` 原来用 `entryId`（**文件 id**）做去重键，一个文件里的多条效果共享同一个 id，于是每个文件只有第一条生效（荒野的两条、净界的多条等）。改为 `entryId#序号`。

### 9. 荒野：钓鱼与容器

- 钓鱼：`FishingHook#retrieve` 构造的 `LootParams` 里 `THIS_ENTITY` 是**钓钩自己**（`aload_0`），不是钓手。`LootRollEffect` 于是既拿不到 `LivingEntity`，也拿不到容器玩家，整条路径静默丢弃 —— 这就是"钓上来只有一条鱼"的原因。现在识别到 `FishingHook` 时改用 `getPlayerOwner()` 当受益人。
- 容器：`RandomizableContainer#unpackLootTable(Player)` 收到了玩家，但它构造的战利品参数里没有玩家。`mixin/ContainerLootMixin.java` 在方法头把玩家存到线程局部、尾部清除，于是箱子/木桶/潜影盒/漏斗矿车的投骰都能归因。额外物品走容器自己的插入回调，装不下的按原版规则掉在容器位置。

### 10. 幽匿标注

- 字幕不再靠拼语言键（`subtitles.<path>`），改为读 `sounds.json` 里声明的字幕：`SoundManager#getSoundEvent(id)` 返回的 `WeighedSoundEvents` 有 `getSubtitle()`。脚步声这类音效的语言键**无法从音效 id 推导**，很多 id 共用一条字幕。
- 不再标注"自己发出的声音"：判定从**眼睛**改到**脚底**，因为原版把玩家自己的声源放在 `player.position()`（站立方块中心），离眼睛整整一个眼高。之前正是这个差值让脚步和挥击漏了过去。
- 字号随距离放大（0.8~2.4 倍），每个标注带一个由自身派生的稳定屏幕偏移，避免同位置多声源完全重叠。
- 文字改为幽匿青 `#00FFFF` 并加一块半透明深色底板（含 1 px 青色描边），板与字一起渐隐；此前纯白字在深色石头/玄武岩上会糊成一片。底板用 `GuiGraphics#fill`，与文字共用同一个缩放矩阵，所以板随字号一起放大。
- 仍然**不生成任何实体**：标注是 HUD 直绘，渐隐/遮挡由客户端本地处理。
- 监听半径本来就走 `Value.eval(件数, …)`，`stack` 默认就是 `per_piece`，所以一件 8 格、两件 16 格、四件 32 格——**这本来就是件的线性叠加**，不需要改。只有数值常量（8 格）不随件数变，"每 1 格"这类系数才是随件数放大的那一项，与整份设计一致。

### 11. 向导：事件驱动，不再每 tick 轮询

`engine/PortalEntry.java` + `engine/PortalTracker.java`：用 Fabric attachment API 把"进入当前维度时的坐标"存在玩家身上（随玩家存档、随死亡复制）。

维度变化监听的是 `ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD`——**有**这个事件，不必轮询。但事件触发时目的维度还在装配中，此刻读到的坐标不一定就是玩家落脚点，所以记录延后一个服务器 tick（`TreEngine` 的 `END_SERVER_TICK` 里 flush，`PENDING` 为空时是空操作），那时坐标已经定死。首次进服（`ServerPlayConnectionEvents.JOIN`）只有在还没有记录时才写一次，之后同维度登录不覆盖，符合"每次进维度覆盖上一次"。

判定顺序：同维度重生点（床/重生锚）→ 记录下来的进入位置 → 末地祭坛 / 该维度出生点。

### 12. 眼眸

- 重复施加不再缩短已有时间：保留更长的剩余时间与更高的倍率。
- `WitherBoss` / `EnderDragon` 各有一个 mixin 放行 Endphase（`WitherBoss#canBeAffected`、`EnderDragon#addEffect`）。**这两条要真的打一次凋灵/末影龙才能验证。**
- 1.21.11 没有 `EntityBoundSound`，`SoundInstance` 也没有 `getLocation()`：音效 id 要走 `AbstractSoundInstance#getIdentifier()`。

### 13. 模板复制配方劫持

原版 18 个纹饰模板的增产配方全是同一个写法：3×3、`#S#`/`#C#`/`###`，七个 `#` 是钻石、`S` 是被复制的模板、`C` 是那个图案自己的耗材，产物 2 个 `S`。七个钻石换两个模板偏贵，所以本版在配方装载时把它们改成 `MSM`/`MDM`/`MMM`：`M` 换成原来中间那个耗材（七个），`D` 换成原来周围那七个钻石（一个），`S` 留在原位，产物不变。

- 识别是**形状识别**，不是白名单：必须是 `ShapedRecipe` 且 3×3；槽 `{0,2,3,5,6,7,8}` 必须全为同一个 `Ingredient`；槽 4 与它不同；槽 1 的 Ingredient 必须能吃下产物（原版没有 `trim_templates` 物品标签，`trim_materials`/`trimmable_armor` 都用不上，所以用"能不能被模板槽接受"来认定谁是模板）；产物数量必须是 2。任一条件不满足就原样放回。
- 改写后的配方**沿用原 recipe id**，配方书条目与服务端解锁进度（advancement）都不受影响。
- 注入点是 `RecipeManager#apply(RecipeMap, ResourceManager, ProfilerFiller)` —— 首次装载和 `/reload` 都经过这一个漏斗，改完的 map 既是被保存的表，也是同步给客户端的那份。
- 服务端权威是结构性的：客户端配方来自 `ClientPacketListener#handleUpdateRecipes`，从不经过 `RecipeManager#apply`，所以纯客户端永远不读这个配置，也永远不会自作主张改配方。配置项 `hijack_trim_duplication_recipes`（默认 `true`）写在 `config/trimeffectively.json`，每次配方重载都会重读；文件缺失时写默认，缺键时补写，文件损坏时告警一次并回退 `true`。
- 实测（`runServer`）：`[tre] recipe hijack: 19 of 1470 recipes rewritten`。19 而不是 18，因为 `minecraft:netherite_upgrade_smithing_template`（下界合金升级模板）的增产配方是同一个形状，也被一并改写——它不是纹饰模板，但同样是"七个钻石换两个模板"的昂贵配方。


### 14. 配置页：Mod Menu + Cloth Config（都是软依赖）

`modmenu` 17.0.1 与 `cloth-config-fabric` 21.11.160 都不写进 `depends`。cloth 用 `modImplementation`（开发环境里就有，方便自测），modmenu 用 `modCompileOnly`（只为拿到 `ModMenuApi` 的签名）；`fabric.mod.json` 里用 `suggests` 声明，`entrypoints.modmenu` 指向 `TreMenuIntegration`。两个模组都不在时这个 entrypoint 不会被实例化，游戏照常启动。

页面交给 Cloth 的 `ConfigBuilder#setGlobalized(true)`，它会换成 `GlobalizedClothConfigScreen`：顶部搜索栏、左侧分类索引、单页滚动、小标题分节——也就是兄弟项目 Creefern 的那套骨架。现在只有一个配置项也照这个标准搭好了，加条目只需往同一分类里再塞一行。工具提示就是需求原文那两行，按 `\n` 切成 `Component` 数组交给 Cloth（每个实参渲染成一行）。

只读与同步：

- 服务端在玩家 `JOIN` 时把 `config/trimeffectively.json` 的当前值打成 `TreConfigPayload`（S2C）发给该玩家；`/reload` 成功后再广播一次。通道是单向的——客户端没有回写入口，也不能主动请求。
- 客户端只缓存，不落盘。只读判定是 `Minecraft#level != null && !hasSingleplayerServer()`——**只有"连着别人的服务器"才置灰**；主菜单没有世界也没有主机，那里改的就是本地文件，正是玩家开档前想调的默认值，所以可编辑；单人/主机同样可编辑。顶部说明从 `scope.local` 换成 `scope.server`。
- 主机保存后除了写文件，还会 `reloadResources(已选中的数据包)` 一次，配方立刻按新配置重建，所以改完不用手动 `/reload`；连服务器时改不了，也就没有这一步。

没有为"服务端权威"写过任何客户端/服务端分支判定——劫持只发生在 `RecipeManager#apply`，本来就只在专职服务器或主机上跑。

### 15. 锻造台三式：剥纹、剥纹取模板、纹饰无效化

三个配方都在 `RecipeType.SMITHING` 里，都实现 `SmithingRecipe`，都注册成 `PlacementInfo.NOT_PLACEABLE` + `isSpecial()`（配方书与 JEI 不显示）。数据文件只有一行 `{"type": "trimeffectively:..."}`——输入是固定的，所以编解码器是 unit codec，配方实例在解码时构造。

| 配方 | 模板槽 | 基槽 | 副槽 | 结果 |
| --- | --- | --- | --- | --- |
| `trim_strip` | 铁粒 | 任何带纹饰的可纹饰装备 | 空 | 去掉纹饰（模板不归还） |
| `trim_strip_recover` | 铁粒 | 同上 | 书与笔 | 去掉纹饰，并归还该纹饰对应的模板物品 |
| `trim_toggle` | 玻璃板 | 同上 | 空 | 纹饰保留，效果开关取反 |

- 「基槽 = 任何带纹饰的可纹饰装备」是两层判据：`Ingredient` 来自 `minecraft:trimmable_armor` 物品标签（槽位谓词，自动进 `RecipePropertySet.SMITHING_BASE`），`matches` 里再要求 `DataComponents.TRIM` 存在。**标签不能在解码时读**：配方重载发生在物品标签绑定之前，那时 `BuiltInRegistries.ITEM.getOrThrow(TagKey)` 抛 `Missing tag TagKey[minecraft:item / minecraft:trimmable_armor]`，整个数据包装载失败。所以基槽的 `Ingredient` 是懒解析 + 记忆化，首次使用点是 `RecipeManager#apply`（标签已就位），记忆的是标签的活 `HolderSet`，换数据包仍会跟着变；标签缺失时退化成"匹配不到任何物品"，只失效不崩溃。
- 归还模板没有走配方结果（锻造台结果只有一个栈），而是在 `SmithingMenuMixin#onTake` 的 HEAD 里发：那时三个输入槽还满着，而 `onTake` 拿到结果后就把它们清空了。判定只能靠槽位——`SmithingMenu` 不保存匹配到的配方对象。**1.21.11 的 `TrimPattern` 没有 `templateItem` 字段**（只有 `assetId`/`description`/`decal`），所以模板物品是扫 `SmithingTrimRecipe` 找 `pattern` 相同者、取它模板槽的第一个物品（`SmithingTrimRecipeAccessor` 用 `@Accessor("pattern")` 读包私有字段）。数据包自定义图案因此也能正确归还。
- 纹饰无效化是物品组件 `trimeffectively:tre_enabled`（`true` 时不写，等效缺省 `true`）。生效点只有两处：`TrimSnapshot#of` 在 `reconcile` 之后直接 `continue`（件数、属性、变量同时归零，`DamageEffects`/`AttributeEngine`/标注一行都没改），以及 `TrimStamp#expected` 在关闭时返回"空值 + 身份"。**清空印记组件不够**——效果件数是按 `minecraft:trim` 的纹饰 id 数出来的，与印记无关。Tooltip 在关闭态把折叠提示换成「该物品的纹饰已被无效化」，并强制不展开。
- 重新开启时按当前数据包**重算**而不是恢复旧值（`TrimStamp.force`），所以关闭期间数据包改过的数字会生效。

### 16. 空跃特效

多段跳实际生效的那一处（`ExtraJumps#aiStep`，改完 `setDeltaMovement` 与 `hurtMarked` 之后）在服务端放 `SoundEvents.WIND_CHARGE_BURST` 与 `ParticleTypes.GUST_EMITTER_SMALL`。服务端做，因为跳跃授权本来就在服务端：客户端只被告知新速度，不该由它决定"有过一次风爆"。`net.minecraft.world.entity.projectile.windcharge.WindCharge` 这个类在 1.21.11 **不存在**，所以音效/粒子名是从 `SoundEvents`/`ParticleTypes` 逐个核过的，不是照抄风弹的类。

### 17. Endphase：去回合化与整秒量化（0.1.0.42）

两件事，一件是删、一件是改。

**删：斩杀线注释掉了。** 原先一旦解离度（秒）≥ 最大生命基值就补一刀 `hurtServer(..., getMaxHealth() + 1000)`，为的是确保死亡消息走 `endphase`。这层保险现在多余：伤害钩子已经把**这个受害者所受任何一击**的类型改写掉，普通致命一击本身就会产生正确的死亡消息。而且保险本身标定错了——`MAX_HEALTH` 是 `RangedAttribute("attribute.name.max_health", 20.0, 1.0, 1024.0)`，下限 1.0，所以越过阈值的人本来就处于「任何一击必死」，脚本化的那一刀并不比自然死亡更正确。代码以块注释保留在 `EndphaseEffect#applyEffectTick` 里，连同理由。

**改：最大生命惩罚从「小数秒」改成「整秒」。** 扣减量原来是 `-1 * seconds * (amplifier+1)`，而 `seconds = duration/20` 每 tick 都在变，于是 `addOrReplacePermanentModifier` 每 tick 重写一次 `trimeffectively:endphase_max_health` 修饰符——一秒钟二十次心形行重排，足以让血条看起来在抖。现在 `-Math.floor(seconds) * (amplifier+1)`：每秒变一次、落在整颗心的边界上，并且保留「值没变就不碰修饰符」的早退。

`MobEffectInstance` 没有 `setDuration`（1.21.11 只有 `update`），`LivingEntity#forceAddEffect` 复用同一个实例原地更新，所以闪烁不是「实例被替换」造成的——可排除那一路。

### 18. 荒野的「9 条鱼」：战利品表会嵌套（0.1.0.43）

一整套（4 件）荒野钓鱼本该多投 4 次、共 5 条，实测 9 条。诊断日志（`[tre] extra_loot_roll caller:`）给出的两张调用栈说明不是重复调用同一个表，而是**一次收竿里出现了两个不同的表**：

```
at LootTable.getRandomItemsRaw(LootContext, Consumer)      ← 外层 gameplay/fishing
  at LootPool.method_341 → LootPool.method_345
  at NestedLootTable.method_433                            ← 嵌套表条目
  at LootTable.getRandomItemsRaw(LootParams, Consumer)     ← 这里又进一次
  at LootTable.getRandomItemsRaw(LootContext, Consumer)    ← 同一个钩子第二次触发
```

映射为：`class_52` = `LootTable`、`class_55` = `LootPool`、`class_83` = `NestedLootTable`、`class_85` = `LootPoolSingletonContainer`。也就是说 `minecraft:gameplay/fishing` 里有一个 `minecraft:nested` 池条目指向 `gameplay/fishing/fish`，嵌入方式会把 `LootParams` 重载再转回 `LootContext` 重载——我们挂的那唯一一处钩子因此对**每一层表**各触发一次。9 = 1（原版）+ 4（嵌套表）+ 4（外层表）。

第一次修法（0.1.0.43）给权限加层级判据：`LootRollEffect` 里一个 `ThreadLocal<Integer> DEPTH`，`onRoll` 自增/自减，只有最外层（depth 0）才投额外骰。**这个修法无效**，实测仍是双倍：`@Inject(at = HEAD)` 的回调会在方法体（也就是跑池子、进而进嵌套表）之前就返回，`finally` 已经把 depth 复位成 0，嵌套调用到达时读到的又是 0，于是照旧放行。

真正生效的修法（0.1.0.44）把「层级」改成包住整个方法体：`mixin/LootTableEffectsMixin.java` 改用 MixinExtras 的 `@WrapMethod`，在 `original.call(context, output)` **之前** `enter()`、在 `finally` 里 `exit()`，只有 `enter()` 返回 true（即最外层）时才调 `LootRollEffect.onRoll`。判据仍然是"一次战利品生成"而不是"一次方法调用"——嵌套是表的正常组合方式，不是错误；区别只在于这次守卫真正覆盖了嵌套发生的区间。

顺带订正一处旧注释：`getRandomItemsRaw(LootParams, Consumer)` 并不是死代码——嵌套表条目正是通过它进入的，只是它随后会转调 `LootContext` 重载。

### 19. Endphase 的三条给药路线（0.1.0.45）

设计上 Endphase 能通过药水、药箭、谜之炖菜三条路给到目标，之前只实现了药水，另两条根本没接。现在三条都有，且各自的持续时间是**定值**：

| 路线 | 来源 | 持续时间 |
| --- | --- | --- |
| 药水（普通） | 酿造：粗制的药水 + 龙首 | 20 秒（400 tick） |
| 药水（延长） | 普通 + 红石 | 40 秒（800 tick） |
| 药水（增强） | 普通 + 荧石粉，amplifier I | 10 秒（200 tick，扣减速度 ×2） |
| 药箭 | 滞留药水 + 箭（原版 `tipped_arrow`） | 药水的 1/8：普通 2.5 秒 / 延长 5 秒 / 增强 1.25 秒 |
| 谜之炖菜 | 碗 + 褐菇 + 红菇 + 龙首 | 固定 5 秒（100 tick） |

- 药水三档改的是 `EndphasePotions` 的三个常量（原为 200/300/100，即 10/15/5 秒）。解离度就是剩余秒数，所以「延长」换来的是更小的最大生命惩罚、「增强」则是更快归零：普通与增强的总扣减都是 20，只有节奏不同。
- 药箭**保持原版行为**：`TippedArrowRecipe#assemble` 的字节码里 `bipush 8` 就是「药箭持续时间 = 药水 / 8」，这是原版自己的设计，不拦。曾经写过 `TippedArrowRecipeMixin` 把药箭钉成固定 5 秒，已按要求整体回退（0.1.0.46 删除该 mixin 与 `APPLIED_TICKS` 常量）。
- 谜之炖菜：1.21.11 **没有** `SuspiciousStewRecipe` 这个类，原版是每条花一份 shapeless JSON，效果直接写在结果的 `minecraft:suspicious_stew_effects` 组件里。所以龙首炖菜只是一份新 JSON：`data/trimeffectively/recipe/suspicious_stew_from_dragon_head.json`，碗 + 褐菇 + 红菇 + 龙首，`duration: 100`、`id: trimeffectively:endphase`。没有新增任何代码。
- 副作用提醒：任何一档药水在生效瞬间就会把最大生命压到 1（20 秒 × 每秒 1 点 = 20 点），这是龙首配方昂贵的前提下的有意设计。增强档同理（10 秒 × 每秒 2 点）。

### 20. 眼眸纹饰的时效：按伤害开方 × 件数（0.1.0.46 → 0.1.0.47）

`eye.json` 的 `duration` 从「固定 200 tick（10 秒）」改成按本次攻击的实际伤害与件数缩放，曲线取**开方**：

```json
"duration": { "value": 28.284271247461902, "per": ["damage_dealt"], "power": 0.5, "stack": "per_piece", "min": 0 }
```

- **单件时效 = √(2 × 本次伤害) 秒**，再乘件数。`duration` 以 tick 计，所以 `value` 是 `20 × √2 = 28.284…`
  （20 tick = 1 秒），`power: 0.5` 对 `per` 的乘积开方，`stack: per_piece` 再乘件数。
- 数值感觉：6 点伤害 1 件 √12 ≈ 3.5 秒、4 件 ≈ 13.9 秒；20 点伤害 4 件 ≈ 25.3 秒；50 点伤害 4 件 = 40 秒。
  低伤害短、高伤害长，但增长被开方压住，不会再出现「暴击一眼秒人」那种线性膨胀。**不设上限**：
  伤害已经足够大时本来就不需要这个效果也能杀人；非玩家目标（Boss）的血量也不止 20 点。
- 引擎侧为此给 `Value` 加了可选字段 `power`（默认 1，`final = value × Π(per)^power × stackFactor`）。
  它只作用在 `per` 的乘积上，`value` 与件数因子仍线性；乘积为负时跳过幂运算以免 NaN。零散数据不受影响，
  所有旧 JSON 省略 `power` 即保持原语义。仍然没有表达式语言——只是乘积多了一次幂。

### 21. 发布前清理：把只有作者自己看得懂的东西搬出去（0.1.0.49）

这一版不改行为，只做「仓库要见人」的整理。

**删掉死代码**

- `effect/AnvilPenaltyEffect.java`：`anvil_penalty` 这个类型从来没有接过点。`shaper.json` 早就改成
  `item_property` 的 `keep_repair_cost`，`AnvilMenuMixin` 直接读 `TrimStamp.flag`，
  这个类即使被注册也不会有任何一条数据引用它。
- `effect/DamageEffects.init()`（"damage hooks armed" 日志）及它在 `Trimeffectively.onInitialize` 里的调用。
- `effect/SummonEffect` 的两处 summon 日志、`client/SoundAnnotations` 的 `LOGGED` 去重表与 annotate 日志、
  `client/TrimTooltip` 的 `logged` 门与 "tooltip line keys" 日志、`TrimTooltip.translatableKey()`。
- `engine/TreRecipes` 里那份 19 条配方名清单（日志改成计数）、`engine/TreEngine` 的 attribute entries 列表与
  `loadedIds()`、`LootRollEffect` 的 `PROBE` / `name()` / produced 统计。
- `EndphaseEffect.applyEffectTick` 里被注释掉的处决代码块：解离度（= 剩余秒数）≥ 最大生命基值时处决
  这条早就不走代码了 —— 最大生命被压到 1 之后任何一次伤害都会致死，致死原因由伤害类型改写保证。
  保留 5 行说明性注释，代码本身删除，随之失效的 `baseMaxHealth()` 一并删除。

**误删后恢复的一处**

`mixin/MobEffectInstanceAccessor.java`（`@Accessor("duration") void trimeffectively$setDuration(int)`）被
清理脚本判成孤儿：它登记在 `trimeffectively.mixins.json` 里，脚本因此认定「有人引用」。实际引用方是
`effect/HarmfulEffectRate.java:61`。删除后 `compileJava` 立刻报错，已恢复（连同 mixins.json 的条目）。
教训：判定未使用的依据必须是**编译器**，不是文本引用图 —— 只有 mixin 注册的类在源码里本来就没有 import。

**临时物与本地状态**

- 删除 `temp/merged`（50.9 MB、9849 个解包 class）与 `temp/trimmable-tools-2.1.0-fabric-1.21.11.jar`
  （第三方 mod 的参考包，公开仓库不宜携带），`temp/` 目录整体移除。
- `fabric/gradle.properties` 里的 `org.gradle.java.home` 是本机绝对路径，改为注释并附两条说明；
  等价做法是设 `JAVA_HOME` 或每次构建加 `-Dorg.gradle.java.home=...`。不写死的原因是：Gradle 自身
  要 JDK 17+ 才能启动，而编译目标 21 由 toolchain 负责，两者不该被同一个字段绑住。
- 新增面向公众的仓库文件：根 `README.md`（模组说明/安装/构建）、`LICENSE`（GPL-3.0 全文）、
  `.gitattributes`（LF、`*.bat` CRLF、二进制标记）、`docs/README.md`（文档索引），
  `.gitignore` 扩到构建产物、`backup/`、`temp/`、`build-count.txt`（本地计数器）并保留 wrapper jar。
- `fabric/README.md` 从脚手架文本改写成工程说明（构建、产物命名、代码地图、mojmap 注意事项）。

**冒烟**：`build` 成功；`runServer` 输出 `TOMC-Trim Effectively loaded`、
`[tre] recipe hijack: 19 of 1474 recipes now take 1 diamond and 7 of the pattern's own material`、
`Loaded 1474 recipes`、`[tre] server started - tre_pattern=18 tre_material=11`，无异常与警告。

### 22. 更正文档里写错的数据目录契约（0.1.0.50）

面向公众前的一次对账发现：文档（和两处 javadoc）一直写「数据包路径 `data/minecraft/tre_pattern/<文件名>.json`、
条目 id 就是原版 trim id `minecraft:sentry`」。这是 M1 期的设计假设，实现时并不成立。

- **实际目录**由动态注册表 key 决定：`trimeffectively:tre_pattern` → `data/<pack>/trimeffectively/tre_pattern/`，
  条目 id = `<pack>:<文件名>`。本模组自己的 18 + 11 份数据因此在
  `data/trimeffectively/trimeffectively/tre_{pattern,material}/`，条目 id 是 `trimeffectively:sentry` 这类。
  运行期做过验证：按 `Tre.id("eye")` 查注册表能取到条目，按 `minecraft:eye` 取不到。
- **绑定关系不在文件名里**，在 JSON 的 `pattern` / `material` 字段（`"pattern": "minecraft:eye"`）。
  `TrimEffect` 的 javadoc 本来就是这么写的（「The file name is only a name」），是文档没跟上。
- `appliesTo` 不做存在性校验：写错的原版 id 不会报错，只是永远匹配不到；匹配用的是物品上实际的 trim id。
- 更正的文件：`docs/EFFECT_TYPES.md` §0（重写目录/命名空间/id 三句，加「第三方怎么加自己的数据」）、
  §4.1 §4.2 的路径、`docs/DESIGN.md` §2.2（标题从「条目 id 直接对齐原版 trim id」改成「条目 id 与
  『服务哪个原版纹饰』解耦」，并注明 §9-Q1 的推荐项被注册表规则否决）、§3.1 的示例注释、`docs/SCHEMA.md` §5
  的加载行、`Tre.java` / `data/TreRegistries.java` / `data/TrimEffect.java` 的 javadoc。

### 23. 文档按读者拆成四份，重写发布页文案（0.1.0.52）

这一版不改运行时行为：改动全在文档，另有一处失效的 javadoc 交叉引用。

**为什么拆**

旧的 `DESIGN.md` 同时承担「为什么这么设计」与「JSON 怎么写」两件事，`SCHEMA.md` 与 `EFFECT_TYPES.md` 是两份互相引用的字段表——写一个条目要先读三份文档。现在按读者切：

| 文件 | 写给谁 | 内容来自 |
|---|---|---|
| `docs/CONTENT.md`（154 行） | 玩家、整合包使用者 | 旧 DESIGN §5 与 EFFECT_TYPES 的装配表 |
| `docs/DATA.md`（468 行） | 不写或很少写 Java 的整合包与联动作者 | `SCHEMA.md` + `EFFECT_TYPES.md` 全量合并 |
| `docs/INTERNALS.md`（256 行） | 同行、想纠错的开发者 | 旧 DESIGN 的引擎叙述与踩坑表 |
| `docs/DESIGN.md`（196 行） | 关心取舍的人 | 只留论证线，具体实现给链接 |
| `docs/STATUS.md` | 查版本差异的人 | 本表 |

`SCHEMA.md` 与 `EFFECT_TYPES.md` 已删除，内容核对无丢失：附录 A 的 29 条逐行保留、4 个完整样本、`Value` 的 5 个字段与 `stack` 4 个取值、9 个上下文变量、`item_property` 的 4 个键、字面量解码顺序、第三方覆盖做法，全部在 `DATA.md`。

**顺带更正的计数与真值**（旧文档写错，现以代码与 JSON 为准）

- `type` 由「21 个」改成 **20 个**：三批退役（`damage_protection`、`fire_duration`/`ignite`、`anvil_penalty`）之后计数没跟着改。自带 29 条数据实际用到 **19 个**，`crit_damage` 目前没有条目使用——`snout` 已改走属性 `trimeffectively:crit_multiplier`。
- `trigger` 由「15 个」改成 **10 个**，全部有实现；`death` / `tick` / `item_break` / `heal` 是占位名，写进去不报错也不触发。
- `per_piece_capped` **只有契约、引擎未实现**（落到 `per_piece`），`DATA.md` 已就此加了说明，不再让整合包作者以为存在封顶。
- `spire` 不是「直接百分比减伤、绕开原版 80% 上限」，而是 `damage_in` 按 `speed_mps` 每件每 1 m/s 减 1%。
- 客户端青色标注那条是 `ward`（`damage_type_override` + `annotate_sound`）；`silence` 是 `mute_sound`，每件 30% 概率静音。
- 钻石耐久是每件各自 ×2，不存在「四件翻四倍」这种跨件乘算。
- `DamageVars.forAttacker` 的 javadoc 原引用 `EFFECT_TYPES.md` §5.1，改为 `docs/DATA.md` §4.2。

**新增面向发布的门面**

- 根 `README.md` 只回答「这是什么」：logo、语言导航、AI 与早期开发声明、一段定位、效果表、安装、命令、文档路由表、构建入口、许可与致谢。
- `docs/README.en.md` 为英文镜像；`docs/README.md` 是五份文档的索引（读者表、阅读顺序、维护约定、三句话版本）。
- `PUBLISHING.md`：`## 【MC百科】` 整篇中文、`## 【CurseForge / Modrinth】` 整篇英文，两节不逐节交叉；顶部一张发布元数据表（中英文名、mod id、平台与版本、运行环境、依赖、许可、作者、仓库地址待填）。文案风格照 MC百科 现有页面：加粗 tagline → 两段概述 → 分主题短 bullet，不写安装步骤、不写实现细节。

**验证**：`build` 成功，产物 `tomc-trimeffectively-0.1.0.52-fabric+mc1.21.11.jar`（279 KB）。文档不进 jar，构建只用于确认骨架与数据文件未被这次整理误改。

### 24. 发布名、三篇发布文案，以及全套文档双语（0.1.0.53）

**发布名与实际模组名分开。** 对外展示名定为 **`[TRE] 纹以载道 Trim Effectively`**，只用于发布页；`fabric.mod.json` 里仍是 `TOMC-Trim Effectively`，游戏内、日志、jar 名、数据目录一律不变。两者不需要同步，也不该同步——改 `fabric.mod.json` 会改 jar 的显示名并让已有存档的模组列表出现"新模组"。

**`PUBLISHING.md` 改成三篇同源文案**：中英逐段穿插（通用）、纯英（CurseForge / Modrinth）、纯中文（MC百科等）。原来按平台分节、每节内部再分语言，同一段话要在文件里维护两遍以上；现在三篇共享一个骨架——加粗 tagline → 两段概述 → 图案 7 条 → 材料 6 条 → 龙首与 `相位解离` → 锻造台三式 → 杂项 → 早期开发提示 → AI 披露——改一处观感就只改一处。一句话总结英文直接用 `fabric.mod.json` 的 `description` 原文，中文是它的译法。

**详细文档全套双语。** `docs/` 下每份文档都有 `.en.md` 对照版：`CONTENT`、`DATA`、`INTERNALS`、`DESIGN`、`STATUS`，加索引 `README.index.en.md`，以及 `fabric/README.en.md`。约定：

- 中文版是事实来源，英文版跟着改；两份的标题序列、表格行列、代码块必须一一对应（本轮实测 7 对文件标题序列全部一致，行数逐对相同）。
- 每个文件第一行是语言切换行，互指对方；英文文件里的交叉链接一律指向 `.en.md`，不把读者送回中文页。
- 代码、JSON、字段名、id、命令、日志原文、数字在两个语言版本里逐字相同，不翻译；英文里的自定义效果名用 `en_us.json` 的官方名（`Long Shot`、`Vexing Guard`、`Sculker`…），首次出现处附中文原名。

**顺手修掉的两处旧数字**（`DESIGN.md` 里的历史叙述，早于 18 图案定稿）：§2 的「8 种纹饰 × 11 种材料」改为 18；§8 那句「强化版 90 秒 × 2 = 180 点」与它上一句「强化 5 秒」自相矛盾，改成不依赖具体版本的表述——强化档把「扣减速率 ×2」与时长直接相乘，一口药就可能把玩家压在 1 点生命上限。

**验证**：全仓库 markdown 无 BOM、无 CR（纯 LF）；相对链接 0 断链；7 对中日/中英文件标题序列 `seqMatch=True`，`### N.` 小节数与小节内 bullet 数逐条一致（`STATUS` 两版 24 / 79）；`CONTENT`/`DATA`/`INTERNALS`/`DESIGN`/索引 五对行数逐对相同（156 / 470 / 258 / 198 / 35），`STATUS` 因英文分段位置不同差 2 行（387 / 385）。构建 `tomc-trimeffectively-0.1.0.53-fabric+mc1.21.11.jar`，日志与 0.1.0.52 一致。

## 仍未验证 / 已知缺口

- `PiglinBruteAiMixin` 的第二个注入（`findNearestValidAttackTarget`）只在 `PiglinBruteAi` 被加载时才应用，runClient 没有生成蛮兵，未做运行时验证；`defaultRequire:1` 保证签名不符时会直接崩而不是静默失效。
- `WitherBossMixin`、`EnderDragonMixin` 同上，未做运行时验证。
- 容器额外战利品只覆盖"首次开启时投骰"的容器；已经在世界生成时填好的箱子不再投骰。
- 荒野与恼鬼的诊断日志（`[tre] loot probe` / `[tre] summon probe` / `[tre] extra_loot_roll`）已在 0.1.0.49 全部删除，需要复现时再加回去。
- 荒野开箱"装不下掉在箱位"依赖容器插入失败的原版掉落逻辑，未实测。
- 荒野的「9 条鱼」已定性（嵌套战利品表被计成两层，见 §18）；0.1.0.43 的 depth 守卫已验证无效，0.1.0.44 换成 `@WrapMethod` 后，用户在 0.1.0.47 之后的复测里反馈游戏内效果符合预期。若再遇到双倍，最快的办法是把 `[tre] extra_loot_roll` 那行日志加回 `LootRollEffect` 看每个 wearer 被调用几次。
- Endphase 最大生命值闪烁只做了「整秒量化」这一项最可能的修复，未在游戏里复现/验证；若仍闪，需要区分是**心形行变短（最大生命）**还是**血量回填（当前生命）**。
- 0.1.0.31 的两处材料修复只做了数据加载验证（`runServer`：`tre_pattern=18 tre_material=11`，`attribute entries` 列表含 `trimeffectively:netherite`，无解码报错），耐久 592→1184 与着火时长 -30% 需要实测。
- 配方劫持只验证了"改写了 19 条、装载无报错"，实际在合成台摆出来能否出 2 个模板需要实测；配置项关掉后应恢复原配方（同一 `runServer` 里可看日志从 19 变 0）。
- 配置页只验证了构建与客户端启动（53 个 mod，多出 `cloth-config 21.11.160`），**没有点开过界面**；Mod Menu 是 `modCompileOnly`，开发环境里不存在。需在装了 Mod Menu 的实例里实测四件事：列表里能点开配置、界面是搜索栏+左侧索引的可滚动单页、**主菜单下可直接编辑并保存**（写进本地 `config/trimeffectively.json`）、单人下改动立刻生效（配方从 19 变回 18）、进别人服务器后整页置灰并显示服务端值。
- 声音标注只对**服务器发到本客户端**的声音生效：`ServerLevel#playSeededSound` 的广播半径是 `SoundEvent#getRange(volume)` = `fixedRange.orElse(volume > 1 ? 16*volume : 16)`，音量 1.0 的普通声音只有 16 格内的玩家会收到包，更远的声音客户端根本不知道它存在，annotate 半径写到 32 也看不见它。落地后若要突破，只能服务端另发一路事件，代价与收益都不划算，暂不做。
- 0.1.0.40 的三个锻造配方只验证到"数据包装载成功、配方总数 1470 → 1473、无解码报错"（`runServer` 日志 `Loaded 1473 recipes`）。**没有在锻造台里摆过**：铁粒/玻璃板是否出现在模板槽、书与笔是否出现在副槽、剥纹后效果是否立刻消失、归还的模板是否进背包（满了应掉在脚下）、失败时红叉表现，都要实测。
- `tre_enabled` 的客户端侧表现（tooltip 改为「已被无效化」且不展开）同样未实测。
- 空跃的风弹音效与粒子未实测（需要真的在空中连跳）。
- Endphase 三条给药路线（§19）与眼眸时效（§20）只验证了数据载荷与编译：`runServer` 日志 `Loaded 1474 recipes`（比 0.1.0.43 多出的正好是龙首炖菜那份 JSON）、`[tre] recipe hijack: 19 of 1474`、无解码报错。实际未测：酿出三档药水的持续时间是否为 20/40/10 秒、药箭是否按原版给出 1/8、龙首炖菜能否合成并给出 5 秒效果、眼眸触发后的时长是否随伤害与件数变化。

