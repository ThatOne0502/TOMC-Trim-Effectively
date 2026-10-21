[English](README.en.md) &nbsp;·&nbsp; **简体中文**

# TOMC-Trim Effectively — 工程目录

这里是实现本身。模组说明、安装方式与效果清单见仓库根的 [README.md](../README.md)。

## 构建

```sh
gradlew.bat build      # Windows
./gradlew build        # Linux / macOS
```

- **启动 Gradle 需要 JDK 17+**，编译目标是 **Java 21**（`java.toolchain`，由 foojay 自动
  provision）。这两件事别混：`JAVA_HOME` 决定跑 Gradle 的那个 JVM，toolchain 决定 `javac`
  用哪个 JDK。
- 若 `JAVA_HOME` 指向旧 JDK，看 [gradle.properties](gradle.properties) 顶部的说明，
  在那里放开 `org.gradle.java.home` 或每次构建加 `-Dorg.gradle.java.home=...`。
- 首次构建下载 Gradle / toolchain / 依赖，5–15 分钟。

产物：

```
build/libs/tomc-trimeffectively-<maj>.<min>.<rev>.<build>-fabric+mc1.21.11.jar
build/libs/tomc-trimeffectively-<...>-sources.jar   ← 源码包，别装进 mods/
```

版本号前三段由 [build.gradle](build.gradle) 里的 `baseVersion` 维护，第四段是
`build-count.txt` 里累计的构建次数（只有 `remapJar` 成功才写回，构建失败不消耗号段）。
`sources` 只用来发布源码，误装进 `mods/` 会因为缺 `.class` 报 mixin 找不到。

## 开发命令

```sh
gradlew runClient      # 带模组启动客户端
gradlew runServer      # 带模组启动服务端（验证注册表/配方加载最方便）
```

数据文件是手写的 JSON，没有 datagen 入口。

服务端启动时应该看到三行自检：

```
TOMC-Trim Effectively loaded
[tre] recipe hijack: 19 of 1474 recipes now take 1 diamond and 7 of the pattern's own material
[tre] server started - tre_pattern=18 tre_material=11
```

最后一行的数字是「扫描到的条目数」，改数据包后应当随之变化。

## 代码地图

| 包 | 职责 |
| --- | --- |
| `data/` | 数据模型与 codec：`TrimEffect`、`EffectEntry`、`Value`（含 `power` 乘方）、动态注册表 |
| `engine/` | `TreEngine`（注册与巡检）、`TrimSnapshot`（身上每件）、`TrimStamp`（盖章/身份）、`AttributeEngine`（5 Hz 属性刷新）、`TreRecipes`（配方劫持） |
| `effect/` | 每种 `type` 的实现，一个类一种；`DamageEffects` 是受击/命中的分发表 |
| `mixin/` | 只做原版没法从外部做到的事（战利品表重入、伤害结算、物品属性读取等） |
| `client/` | Mod Menu 配置页、HUD / 粒子 / 声音标注、tooltip |
| `net/` | 服务端 → 客户端配置同步 |
| `command/` | `/tre dump`、`/tre reload` |
| `recipe/` | 锻造台三式（取下纹饰、取下并返还模板、开关单件效果） |

## 注意事项

- 用的是**官方 Mojang 映射**（mojmap），不是 Yarn：`ResourceLocation`、`Holder`、
  `BuiltInRegistries` 这一套。照着 Yarn 教程写会编译不过。
- 数据文件在 `src/main/resources/data/trimeffectively/trimeffectively/` 下 —— 第一层是
  **命名空间**，第二层是**动态注册表 id**（`trimeffectively:tre_pattern`）。
- 渲染与声音只放客户端；数值与判定只放服务端。
