# FLY-05: Generic plant parts and the clock's workstation bypass Flywheel

| | |
|---|---|
| **Severity** | low |
| **Kind** | Rendering and Flywheel |
| **Area** | Flywheel & Clockwork Tubes |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/client/AutoHarvesterRenderer.java` (diff: `renderSafe` calls `renderPlant(ms, PartSink.screen(buffer, state, overlay), ...)` when `RealPlants.render` returns false; `renderPlant` draws the `TREE`/`CROP` part models), `src/main/java/com/comedymonkey/createaddons/client/CuckooClockRenderer.java` (diff: `renderSafe` draws the villager's workstation with `renderSingleBlock` during a show)

## What is wrong

Two solid baked models are still drawn through the BER every frame with Flywheel on: the harvester's generic `TREE`/`CROP` part (a `ModelResourceLocation` part, scaled by growth, forced through `PartSink.screen` because the RealPlants-or-generic decision is made in `render`), and the clock's workstation block model (a `BlockState`, drawn with `renderSingleBlock` while the villager is out). The harvester one is a small but constant per-frame cost on every harvester whose plant has no RealPlants model. Must stay on the BER: `RealPlants.render` (real plant block models need blockstate/ModelData and are translucent/cutout mixes) and the clock's procedural sky.

## How to see it happen

A field of Auto Harvesters growing a crop RealPlants can't draw: each still goes through `renderModel` for the plant every frame while the drill, head and shaft went to Flywheel.

## How to fix it

Harvester: move the generic plant into `parts()` behind a side-effect-free check (`!RealPlants.canRender(be.getPlant(), kind)`; factor the lookup out of `RealPlants.render`), and keep `RealPlants.render` in `render()`. Clock: either leave it (only visible during a show) or draw the workstation as a part via `Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().stateToModelLocation(station)`, which gives a `ModelResourceLocation` that `PartModels.get` can bake (it would need an overload taking a model location per blockstate; low value).

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed (harvester).** The harvester's generic plant is a Flywheel part when real plants can't draw it. The clock's workstation stays on the renderer (maintainer's choice).

Tests: RealPlantsTest.
