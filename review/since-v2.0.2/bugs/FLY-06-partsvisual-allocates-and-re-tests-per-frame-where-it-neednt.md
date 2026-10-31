# FLY-06: PartsVisual allocates and re-tests per frame where it needn't

| | |
|---|---|
| **Severity** | low |
| **Kind** | Performance |
| **Area** | Flywheel & Clockwork Tubes |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/client/parts/PartsVisual.java:111` (`Vec3.atCenterOf(pos)` every frame per visual), `MachineVisuals.java:271` (Fluid Line `active` predicate includes `Fitting.INTAKE`, whose `parts()` returns at once: `FluidLineRenderer.java:534-535`)

## What is wrong

`beginFrame` allocates a `Vec3` per visual per frame for the view-distance test; `pos.distToCenterSqr(context.camera().getPosition())` does the same without allocating (it's what `AbstractBlockEntityVisual.doDistanceLimitThisFrame` uses). Separately, every Fluid Line Intake gets a full `PartsVisual` (a `DynamicVisual` whose `beginFrame` runs every frame: distance, frustum, limiter, `draw`) although `FluidLineRenderer.parts` draws nothing for an Intake; an `IdleVisual` would cost nothing.

## How to see it happen

A few hundred machines and intakes in view: a few hundred `Vec3` allocations per frame and a few hundred needless `beginFrame`s; small, but it's the one hot path shared by every machine.

## How to fix it

In `PartsVisual.beginFrame` use `pos.distToCenterSqr(context.camera().getPosition()) > viewDistanceSqr`. In `MachineVisuals.register` change the Fluid Line predicate to `be -> be.fitting() != Fitting.NONE && be.fitting() != Fitting.INTAKE && be.encasing() == null` (the renderer's `render()` keeps drawing the Intake's fluid either way).

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** No Vec3 per frame in PartsVisual; Fluid Line Intakes get the idle visual.

Tests: MachineVisualsTest.
