**简体中文** &nbsp;·&nbsp; [English](README.en.md)

# TOMC-Trim Effectively — NeoForge 工程目录

这里是 **NeoForge 26.1.2** 的实现。模组说明、安装方式与效果清单见仓库根的 [README.md](../README.md)；
Fabric 1.21.11 那一份在 [fabric/](../fabric/README.md)。两边的数据文件、效果实现与文档是同一套，
只有加载器接线不同。

## 构建

```sh
gradlew.bat build      # Windows
./gradlew build        # Linux / macOS
```

- **启动 Gradle 需要 JDK 17+**，编译目标是 **Java 25**（`java.toolchain`，由 foojay 自动
  provision）——26.x 的目标字节码是 Java 25，本地没有 JDK 25 也能构建，只是第一次要下载。
- 若 `JAVA_HOME` 指向旧 JDK（例如 PATH 上只有 Java 8），看 [gradle.properties](gradle.properties)
  顶部的说明，或每次构建加 `-Dorg.gradle.java.home="C:\path\to\jdk-21"`。
- 首次构建要跑 NeoForm：下载 26.1.2 双端 jar → 反编译 6800 多个源文件 → 打 NeoForge patch →
  重新编译，20 分钟量级；之后走 `~/.gradle/caches/neoformruntime` 缓存，秒级。

产物：

```
build/libs/tomc-trimeffectively-<版本>-neoforge+mc26.1.2.jar
build/libs/tomc-trimeffectively-<版本>-neoforge+mc26.1.2-sources.jar
```

带 `-sources` 的那份只是给人看的，别放进 `mods/`。

版本号来自 [gradle.properties](gradle.properties) 的 `mod_version`（与 Fabric 版同一串数字，
不含构建计数器）。

## 开发命令

```sh
gradlew runClient      # 带模组启动客户端
gradlew runServer      # 带模组启动服务端（验证注册表/配方加载最方便）
```

服务端启动时应该看到这几行自检：

```
TOMC-Trim Effectively loaded
[tre] recipe hijack: 19 of 1519 recipes now take 1 diamond and 7 of the pattern's own material
Loaded 1519 recipes
[tre] server started - tre_pattern=18 tre_material=11
```

最后一行是「扫描到的条目数」，改数据包后应随之变化。

## 代码地图

与 [fabric 的代码地图](../fabric/README.md#代码地图) 同构，差别只在加载器层：

| 位置 | NeoForge 写法 |
| --- | --- |
| `Trimeffectively.java` | `@Mod` 构造器拿 `IEventBus`，所有注册用 `RegisterEvent` / `DeferredRegister` |
| `TrimeffectivelyClient.java` | `@Mod(value = MOD_ID, dist = Dist.CLIENT)`，客户端事件挂 `NeoForge.EVENT_BUS` |
| `data/TreRegistries.java` | `DataPackRegistryEvent.NewRegistry#dataPackRegistry`（同步动态注册表） |
| `engine/PortalTracker.java` | `AttachmentType` + `PlayerChangedDimensionEvent` / `PlayerLoggedInEvent` |
| `net/` | `RegisterPayloadHandlersEvent` + `PacketDistributor` |
| `effect/TreAttributes.java` | `EntityAttributeModificationEvent` 给玩家加属性 |
| `client/TrimTooltip.java` | `RegisterKeyMappingsEvent` + `ItemTooltipEvent` |
| `client/SoundAnnotations.java` | `RegisterGuiLayersEvent#registerAboveAll` + `GuiGraphicsExtractor` |

## 注意事项

- 用的是 **官方 Mojang 映射**，26.x 起 Minecraft 本身已不再混淆，所以类名与方法名就是
  `Identifier`、`LivingEntityRenderer`、`ModelPart` 这些，和 mojmap 一致。
- 26.1.2 相对 1.21.11 有两处**包移动**：`CameraRenderState` 在
  `net.minecraft.client.renderer.state.level`，`RenderType` 在
  `net.minecraft.client.renderer.rendertype`。
- 数据文件位置不变：`src/main/resources/data/trimeffectively/trimeffectively/`，第一层是命名空间，
  第二层是动态注册表 id（`trimeffectively:tre_pattern`）。
- **配置页由 Cloth Config 提供**（可选前置）：装了它，模组列表里就会出现 `Config` 按钮——走的是
  NeoForge 的 `IConfigScreenFactory` 扩展点（Fabric 那边对应 Mod Menu 的 `ModMenuApi`）。没装则只有
  `config/trimeffectively.json`，写文件与服务器同步的逻辑完全一致。编译期依赖是 `compileOnly`
  （`me.shedaniel.cloth:cloth-config-neoforge:26.1.154`），不打包、不声明为必需前置。
- 模组列表里的 banner 由 `neoforge.mods.toml` 的 `logoFile` 指定，指向
  `assets/trimeffectively/logo.png`；Fabric 那边对应的是 `fabric.mod.json` 的 `icon`。
