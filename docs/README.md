[English](README.index.en.md) &nbsp;·&nbsp; **简体中文**

# 文档索引

四份面向不同读者的文档，加一份变更记录。同一段技术解释只出现在一份里，其它地方给链接。每份都有中英对照的两个版本，内容和编号一一对应，可以左右对着屏幕比。

| 你是 | 去哪份 | 读完能做什么 |
|---|---|---|
| 玩家、整合包使用者 | [CONTENT.md](CONTENT.md) · [EN](CONTENT.en.md) | 知道 18 个纹饰 + 11 种材料各给什么、Endphase 怎么来、锻造台怎么用、命令能干什么 |
| 整合包作者、想联动的模组作者（不写或很少写 Java） | [DATA.md](DATA.md) · [EN](DATA.en.md) | 写出一个能加载的数据包条目：目录、字段、`Value` 算式、20 个在用 `type`、触发器、变量、校验与热重载 |
| 同行、想纠错的开发者 | [INTERNALS.md](INTERNALS.md) · [EN](INTERNALS.en.md) | 定位一次求值发生在哪、mixin 挂在哪、`TrimStamp` 在防什么、怎么复现 |
| 设计者、想理解取舍的人 | [DESIGN.md](DESIGN.md) · [EN](DESIGN.en.md) | 每个决定在防什么、放弃了什么、哪些是有意不做 |
| 查版本差异的人 | [STATUS.md](STATUS.md) · [EN](STATUS.en.md) | 每一版改了什么、坑记在哪一条 |

英文总览在 [README.en.md](README.en.md)。

## 阅读顺序

- **想玩**：CONTENT。
- **想加内容**：CONTENT（知道有什么）→ DATA（知道怎么写）。不写代码的话到 DATA 结束。
- **想改代码或审设计**：DESIGN（先看取舍）→ INTERNALS（再看落地）。反过来也行，但先看理由会少骂两句。
- **升级/复现问题**：STATUS，按版本倒序往下翻。

## 三句话版本

1. 纹饰的效果是**数据**，不是代码：`data/<pack>/trimeffectively/tre_pattern|tre_material/<文件名>.json`，一份文件就是一条条目，扫描进来成为同步的动态注册表。
2. 效果**按件数**叠加，`Value` 的 `stack` 字段决定叠法，`per` 是乘进去的变量表，`power` 是可选乘方。
3. 客户端只做表现，服务端只做判定；两边靠动态注册表同步，版本必须一致。

## 维护约定

- 中文版是事实来源，英文版跟着改；两份的结构、编号、表格行列必须始终一致，改一份就要改另一份。
- `STATUS.md` 是变更记录，按版本倒序追加，旧版本段落不删——它同时记录了踩过的坑（例如解码顺序那类问题）。
- 数据格式改了就改 `DATA.md`；实现变了改 `INTERNALS.md`；只是改数值，`CONTENT.md` 与 `DATA.md` 附录两边都要对齐。
- 实验性探针日志一律不打进仓库：验证完就删，`STATUS.md` 里记结论即可。代码、JSON、字段名、id、数字在两个语言版本里逐字相同，不翻译。
