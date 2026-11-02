# ELEC-02: A stale formed block tears down a live tower sharing its master

| | |
|---|---|
| **Severity** | medium |
| **Kind** | Something does not work |
| **Area** | Battery, Dynamo & generators |
| **Confidence** | medium |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/electric/BatteryTowers.java:344-357` (`placed`: `if (!intact) ... unform(level, towerOf(be), null)`), `BatteryTowers.java:516-537` (`unform` matches other blocks only by `master.equals(be.master)`), `BatteryTowers.java:483-487` (`form` moves loose spans onto the next tower on the footprint), `BatteryTowers.java:426-449` (`arrange` unforms every tower from `towersIn` that is not in the plan).

## What is wrong

When a formed Battery is (re)loaded or put down and `intact()` says its tower is not as it remembers (height, facing or `formedAt` differ), `placed()` calls `unform()` on the tower *as this block remembers it*. `unform()` walks the remembered tower's 27-81 positions and resets every block whose `master` equals that master, without checking height, facing or `formedAt`. A block can carry a stale memory of a tower that has since re-formed on the same master (same bottom-front-middle block) with another height, or that the player rebuilt after the block was carried off. Those blocks match, so a perfectly good Industrial Battery is dissolved to plain Batteries, the master's spans are taken into `LOOSE`, and the following `form()` of whatever the stale block completes may hand those cables to another tower on the footprint (`old.getY() >= t.bottom() - MAX`), or `finish()` drops them as items. Charge is kept (every block keeps its share), but the formed tower, its model and its cable connections are lost. The same unconditional master match is reachable from `arrange()` via `towersIn()` whenever a stale formed block sits in the footprint's centre column.

## How to see it happen

(a) Build a 3x3x9 tower across a chunk border and let the far chunk unload (a corner block out of view distance) while a drill contraption, TNT or `/setblock` removes a block in layer 5. `removed()` unforms what is loaded and re-forms layers 1-4 as a tower with the *same master* M (height 4). Walk back so the far chunk loads: its block L still holds master=M, height=9. `onLoad` schedules `placed()`; `intact()` fails (`o.height != be.height`); `unform(Tower(M,9))` resets all 36 blocks of the live 4-high tower and takes its spans; `column()` from L's layer forms layers 6-9 (if full) as a new tower and `form()` moves the lower tower's cables onto it. Result: the lower four layers are plain Batteries and the wires hang from the upper tower. (b) Contraption variant: a piston contraption carries one block of a 3x3x3 tower away (the rest go plain); the player plugs the hole from the same side, re-forming a tower with the same master; later the contraption disassembles that block anywhere in the dimension: `placed()` -> `intact()` false (`formedAt != pos`) -> `unform(Tower(M,3,facing))` tears down the rebuilt tower at the old site.

## How to fix it

In `BatteryTowers.placed()`, when `be.formedAt != null && !pos.equals(be.formedAt)` (this block itself is displaced) do not call `unform` at all: only reset this block (the existing `be.dissolveStore(); be.setTower(null, ...); be.forgetSpans(); setBlock(FORMED=false)` branch) and go on to `column()`. In `BatteryTowers.unform(level, t, without)` count a block as part of `t` only when `t.equals(towerOf(be))` (same master, height and facing) and (`be.formedAt == null || p.equals(be.formedAt)`), in both the `dissolveStore` loop (line 527) and the reset loop (line 530); likewise take the master's spans (line 519) only when `towerOf(m)` equals `t`. Every current caller (`removed`, `arrange`) passes `t = towerOf(someBlock)`, so a genuine tower still matches all its blocks. Game test: form 3x3x4 with master M, give one block the memory (M, height 9) via NBT and reload it; the 4-high tower must still be formed with its span afterwards.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** A displaced or stale formed block only resets itself; unform matches master, height, facing and formedAt.

Tests: BatteryGameTests: staleBlockLeavesALiveTower, movedBlockLeavesTheTowerItCameFrom.
