# ELEC-03: Carry On pickup of one tower block unforms the whole tower, no re-form

| | |
|---|---|
| **Severity** | low |
| **Kind** | Something does not work |
| **Area** | Battery, Dynamo & generators |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/electric/BatteryBlockEntity.java:543-547` (`remove()` -> `BatteryTowers.removed(..., moving = true)`), `BatteryTowers.java:557-566` (`if (moving) return;` before the re-form), `BatteryBlock.java:112-118` (`onRemove` does nothing because the block entity is already gone).

## What is wrong

`remove()` cannot tell a contraption from Carry On, so it always passes `moving = true`, which unforms the tower and deliberately skips the re-form ("the rest is going too"). Carry On removes the block entity first and the block second (`tschipp/carryon/common/carry/PickupHandler.tryPickUpBlock`: `saveWithId` -> `CarryOnData.setBlock` -> `Level.removeBlockEntity` -> `Level.removeBlock`, verified in carry-on-PV8oLZ1q bytecode), so picking up a single block of an Industrial Battery with Carry On behaves unlike breaking or sneak-wrenching it (spec D25/D32): every remaining block drops to a plain Battery, the master's spans come down and the Power Lines on them are popped as items at the master (`finish()`), and `onRemove` never reaches `PowerLines.changed(pos)` for the removed block. Charge is conserved.

## How to see it happen

3x3x4 tower with brass cables on its insulators; sneak-right-click the top-middle block with an empty hand (Carry On). Expected (as with a wrench or pickaxe): a 3x3x3 tower re-forms and keeps its cables. Actual: 35 plain Batteries, cables on the floor.

## How to fix it

Decide the re-form where both paths meet. In `BatteryBlockEntity.remove()` dissolve the store and unform as now, but instead of returning early stash the tower in a small `Map<BlockPos, Tower> PENDING` (in `BatteryTowers`). In `BatteryBlock.onRemove` (which runs for Carry On's `removeBlock` with `movedByPiston == false` and for Create's contraption `setBlock` with `Block.UPDATE_MOVE_BY_PISTON`, i.e. `movedByPiston == true`), when the block entity is already gone, `PENDING.remove(pos)` and run the `run()`/`arrange()` re-form unless `movedByPiston`; also call `PowerLines.of(server).changed(pos)` in that branch. Clear `PENDING` in `finish()` so nothing lingers.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Carry On pickup re-forms the rest of the tower and keeps its cables; a contraption still doesn't. Also fixed: a fresh empty block entity was being created at the removed block. Pending entries are cleared on server stop.

Tests: BatteryGameTests: carryOnTakesATowerBlock.
