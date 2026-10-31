# LINE-06: A turbine casing keeps pointing at a controller a contraption carried away

| | |
|---|---|
| **Severity** | low |
| **Kind** | Performance |
| **Area** | Fluid & Power Lines |
| **Confidence** | medium |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/electric/GeothermalCasingBlockEntity.java:46-48` (`generatorKey`), `src/main/java/com/comedymonkey/createaddons/content/electric/GeothermalControllerBlock.java:112-119` (`onRemove`), `src/main/java/com/comedymonkey/createaddons/content/line/PowerLines.java:517-532` (`throughGenerators`), `:1156-1160` (`grid.forget` in `Network.tick`)

## What is wrong

`GeothermalCasingBlockEntity.generatorKey()` returns the stored `controller` position whether or not a controller is still there. `GeothermalControllerBlock.onRemove` clears the casings (`be.unform()`) only when its block entity is present; Create removes the block entity first when a contraption takes the block, so the casings keep the stale position. Every network rebuild then re-faces the casing to the stale key (`grid.face` → `setDirty()` on the grid), and every once-a-second update finds `running(stale)` true with no `Generator` there and calls `grid.forget(stale)`, which clears those faces and marks the grid dirty again. Net effect: the pieces' `SavedData` (every wire and pole in the level) is re-marked dirty each second and rewritten at every autosave, which item 7 of the review plan was meant to stop, and the stale casing is counted as a "generator" branch in the Splitter screen.

## How to see it happen

Formed turbine with a copper line on a port casing. Glue the controller onto a contraption and move it. The casings stay `FORMED` with `controller` set. Watch `PowerGrid.setDirty()` fire each network update; the grid file is rewritten each autosave until the casings are broken or the turbine re-forms.

## How to fix it

In `GeothermalCasingBlockEntity.generatorKey()`, return `controller` only when `controller()` is non-null *or* the controller position is in an unloaded chunk (that is the legitimate on-paper case): `return controller != null && (!level.isLoaded(controller) || controller() != null) ? controller : worldPosition;`. And in `GeothermalControllerBlock.onRemove`, when the block entity is gone, still walk the 3x3x3 from the state's `FACING` and clear each `GeothermalCasingBlockEntity` whose `getController()` equals `pos` (the geometry comes from the block state, not the entity).

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** A turbine casing stops pointing at a controller that was carried off, unless the controller's chunk is unloaded (on paper).

Tests: LineGameTests: carriedOffTurbineFreesItsCasings.
