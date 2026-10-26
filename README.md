<p align="center">
  <img src="art/trimeffectively_logo.png" alt="TOMC-Trim Effectively" width="800">
</p>

<p align="center">
  简体中文（本页）&nbsp;·&nbsp;<a href="docs/README.en.md">English</a>
</p>

> [!WARNING]
> **早期开发阶段 / Early Development**
>
> 本项目有两个平台的版本：Minecraft **1.21.11 · Fabric** 与 **26.1.2 · NeoForge**，目前处于 `0.2.0.x` 线。内容、数据格式与配置项仍可能调整，不保证向后兼容；升级前请备份存档。
>
> This project ships for two platforms: Minecraft **1.21.11 · Fabric** and **26.1.2 · NeoForge**, currently in the `0.2.0.x` line. Content, data format and options may still change without backward compatibility. Back up your world before updating.
>
> **AI 辅助创作 / AI-Assisted**
>
> 本项目在开发过程中使用了 AI 辅助（DeepSeek Harness，deepseek-flash）。代码、文档与设计并非全部经过人工审查与测试，请自行判断是否用于长期存档。
>
> This project was developed with assistance from AI (DeepSeek Harness, deepseek-flash). Not all code, documentation and design have been reviewed and tested by humans.

# TOMC-Trim Effectively

> 发布名（对外）：**`[TRE] 纹以载道 Trim Effectively`**，游戏内模组名仍是 `TOMC-Trim Effectively`。

> 📘 **数据与类型参考** — [docs/DATA.md](docs/DATA.md) &nbsp;·&nbsp; 文档总索引 — [docs/README.md](docs/README.md)

**原版的纹饰只是花纹。这个模组让它生效。**

给盔甲打上纹饰之后，**图案**和**材料**各自带来一组效果：荒野让你多钓几条鱼，钻石把耐久翻倍，幽静把你发出的声音吞掉，监守把振动变成只有你自己看得见的青色标注。同一件装备可以同时吃图案与材料两条线；多件同时穿戴时，效果**按件数**叠加——不是按纹饰等级，也不是按等级乘系数。四件荒野就是四份战利品；四件钻石则是每一件都翻倍。

整套效果目录是**数据**：18 个纹饰图案与 11 种纹饰材料一共 29 条条目、20 种效果类型，全部写在 JSON 里，放在动态注册表中。装一个数据包就能整份替换、追加或关掉其中任何一条——包括你自己模组新增的纹饰。

## 它做了什么

| 纹饰 | 效果 |
| --- | --- |
| 荒野 `wild` | 击杀与开箱时按件数多投战利品；四件套收一竿得 5 条鱼 |
| 眼眸 `eye` | 命中时每件 10% 概率给目标上 `Endphase`，时长 = √(2 × 本次伤害) 秒 × 件数 |
| 尖塔 `spire` | 按移动速度减免受到的伤害：每件每 1 m/s 减 1% |
| 下界合金 `netherite` | 每件 −30% `burning_time`（四件完全不点着）、−16% 受伤，装备不毁于火 |
| 钻石 `diamond` | 每件的耐久上限 ×2 |
| 幽静 `silence` | 每件 30% 概率把你发出的声音吞掉 |
| 监守 `ward` | 纯客户端表现：振动粒子 + 头顶青色底板标注，并改写造成的伤害类型 |
| 龙首药水 | `Endphase`：上药即把最大生命压到 1，落水受伤，品红色；由龙首酿造，也做炖菜与药箭 |

完整清单见 [docs/CONTENT.md](docs/CONTENT.md)。

## 安装

| 需要 | Fabric 版 | NeoForge 版 |
| --- | --- | --- |
| Minecraft | 1.21.11 | 26.1.2 |
| 加载器 | Fabric Loader ≥ 0.19.3 | NeoForge ≥ 26.1.2.124 |
| 前置模组 | Fabric API（必须） | 无 |
| Java | 21 | 25 |
| 图形配置页 | Mod Menu + Cloth Config（可选，不装照常进游戏） | Cloth Config（可选；不装就改 `config/trimeffectively.json`） |

把**对应平台**的 jar 放进 `mods/`（两个平台的 jar 不要同时装，模组 id 相同）。客户端与服务端都要装，且**版本必须一致**——效果目录是同步的动态注册表，版本对不上会连不上。

## 命令

需要 gamemaster 权限：

- `/tre dump` —— 打印已加载的条目、身上每件的图案/材料、以及每条条目实际算了多少件。
- `/tre reload` —— 重读数据包并强制重盖章（旧物品自愈用）。

## 文档

| 你想… | 看这份 | 写给谁 |
| --- | --- | --- |
| 知道游戏里有什么 | [docs/CONTENT.md](docs/CONTENT.md) | 玩家、整合包使用者 |
| 自己写效果 / 加自己的纹饰 | [docs/DATA.md](docs/DATA.md) | 整合包作者、想联动的模组作者 |
| 知道它在代码里怎么运作 | [docs/INTERNALS.md](docs/INTERNALS.md) | 同行、想纠错的开发者 |
| 知道为什么这么做 | [docs/DESIGN.md](docs/DESIGN.md) | 关心取舍的人 |
| 每一版加了什么、改了什么 | [CHANGELOG.md](CHANGELOG.md) | 所有人 |
| 往发布站贴文案 | [PUBLISHING.md](PUBLISHING.md) | 作者本人 |

## 从源码构建

```sh
cd fabric && gradlew.bat build      # Fabric 版（Windows）
cd neoforge && gradlew.bat build    # NeoForge 版（Windows）
```

产物分别是 `fabric/build/libs/tomc-trimeffectively-<版本>-fabric+mc1.21.11.jar` 与 `neoforge/build/libs/tomc-trimeffectively-<版本>-neoforge+mc26.1.2.jar`。Gradle 自身需要 **JDK 17+** 才能启动；编译目标由 toolchain 自动 provision（Fabric 版 Java 21、NeoForge 版 Java 25）。产物命名、开发期 `runClient`/`runServer`、代码地图见 [fabric/README.md](fabric/README.md) 与 [neoforge/README.md](neoforge/README.md)。

## 许可与致谢

代码以 **GPL-3.0-or-later** 发布，全文见 [LICENSE](LICENSE)。

开发过程由 DeepSeek Harness（deepseek-flash）协助：字节码取证、数据格式设计、文档整理与构建验证。设计的取舍与最终责任在作者。
