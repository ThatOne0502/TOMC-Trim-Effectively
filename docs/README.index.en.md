**English** &nbsp;·&nbsp; [简体中文](README.md)

# Documentation Index

Four documents aimed at four different readers, plus a changelog. Each technical explanation lives in exactly one of them; everything else links to it. Every document exists in both Chinese and English, with matching numbering, so the two can be read side by side.

| You are… | Read | What you get |
|---|---|---|
| a player or modpack user | [CONTENT.en.md](CONTENT.en.md) · [中文](CONTENT.md) | what each of the 18 trims and 11 materials does, where `Endphase` comes from, how the smithing table works, what the commands do |
| a datapack author or an add-on developer (little or no Java) | [DATA.en.md](DATA.en.md) · [中文](DATA.md) | how to write a loadable entry: directory layout, fields, the `Value` formula, the 20 `type`s in use, triggers, variables, validation and hot reload |
| a fellow developer who wants to audit the code | [INTERNALS.en.md](INTERNALS.en.md) · [中文](INTERNALS.md) | where a single evaluation happens, which mixins are hooked where, what `TrimStamp` protects against, how to reproduce |
| a designer weighing the trade-offs | [DESIGN.en.md](DESIGN.en.md) · [中文](DESIGN.md) | what each decision guards against, what it cost, what was deliberately left out |
| someone looking up a version difference | [STATUS.en.md](STATUS.en.md) · [中文](STATUS.md) | what changed in each build, and which pitfall each version records |

The English overview of the project is [README.en.md](README.en.md).

## Reading order

- **Just want to play**: CONTENT.
- **Want to add content**: CONTENT (see what exists) → DATA (see how to write it). If you do not write code, stop at DATA.
- **Want to change code or review the design**: DESIGN (trade-offs first) → INTERNALS (implementation second). The other order works too, but reading the reasoning first saves you some swearing.
- **Upgrading or reproducing a problem**: STATUS, newest version first.

## In three sentences

1. Trim effects are **data**, not code: `data/<pack>/trimeffectively/tre_pattern|tre_material/<file name>.json`, one file per entry, scanned into a synced dynamic registry.
2. Effects stack **per worn piece**; the `stack` field of a `Value` picks the stacking rule, `per` is the variable table multiplied in, `power` is an optional exponent.
3. The client only renders, the server only decides; the two sides meet through the synced dynamic registry, so **versions must match**.

## Maintenance rules

- The Chinese version is the source of truth; the English version follows it. Structure, numbering and table dimensions must always match — change one, change the other.
- `STATUS.md` is a changelog: append newest-first, never delete old sections — they also record the pitfalls (decode order, and the like).
- Data format changes go to `DATA.md`; implementation changes to `INTERNALS.md`; a pure number change must be aligned in both `CONTENT.md` and the appendix of `DATA.md`.
- Probe logging never enters the repository: delete it once verified and leave the conclusion in `STATUS.md`. Code, JSON, field names, ids and numbers are byte-identical in both languages and are never translated.
