# ELEC-04: Partly loaded tower rebuilds 81 BlockPos every tick

| | |
|---|---|
| **Severity** | low |
| **Kind** | Performance |
| **Area** | Battery, Dynamo & generators |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/electric/BatteryBlockEntity.java:474-489` (`buildTower`: `for (BlockPos p : BatteryTowers.blocks(master, height, towerFacing)) { if (!level.isLoaded(p) ...) return null; ...}`), called from `store()` (`:389`) on every master tick (`:614`), on every capability call (`view.live()` `:97`, `getEnergyStored` `:121`) and from `storeKey()`/`storeBlocks()` (`:411`, `:418`).

## What is wrong

`BatteryTowers.blocks()` eagerly allocates an `ArrayList` plus `height * 9` `BlockPos` objects (up to 81) before the loop looks at the first position. While any block of the tower is in an unloaded chunk (a tower across a chunk border at the edge of the loaded area, which is exactly the "runs on paper while not all loaded" case the design allows), `buildTower()` fails on the first unloaded position, caches nothing, and the master repeats the full allocation every tick, plus once more for every Power Lines query of `storeKey()`/`storeBlocks()`.

## How to see it happen

A 9-high tower straddling a chunk border, player far enough that one chunk is unloaded: the master's `tick()` allocates 82 objects and does up to 81 `isLoaded` checks per tick, forever. Not a hitch, but steady garbage from an idle block.

## How to fix it

In `buildTower()` iterate without materialising the list (nested loop over `center.offset(dx, y, dz)` with a `MutableBlockPos`, or a `BatteryTowers.forEachBlock`), returning at the first unloaded position. Better, remember the failure: set `waitingForChunk = true` and skip `buildTower()` until `onLoad()` of any Battery with this master (or a 20-tick retry) clears it; `storeKey()`/`storeBlocks()` return null / `List.of(master)` while it is set.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** A partly loaded tower checks its four corner chunks first and allocates nothing.

Tests: covered by existing tower tests.
