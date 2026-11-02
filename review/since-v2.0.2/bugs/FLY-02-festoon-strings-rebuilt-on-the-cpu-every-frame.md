# FLY-02: Festoon strings rebuilt on the CPU every frame

| | |
|---|---|
| **Severity** | medium |
| **Kind** | Rendering and Flywheel |
| **Area** | Flywheel & Clockwork Tubes |
| **Confidence** | high (also reported as LIGHT-02) |

## Where to look

`src/main/java/com/comedymonkey/createaddons/client/FestoonRenderer.java:478-512` (`renderSafe`), `:515-528` (`beam`), `:554-556` (`getViewDistance` 128)

## What is wrong

Every frame, for every Festoon hook in view (up to 128 blocks), the BER regenerates the whole string: per owned span `max(8, ceil(length*3))` beams of 6 quads each, plus 2 beams per bulb, each `beam` call allocating ~15 `Vec3` and a `Vec3[]`; plus a `HashMap<Long,Integer>` with boxed keys for the light lookups and three atlas sprite lookups per BE. A 16-block string is ~390 quads (~1,560 vertices) and ~1,000 `Vec3` allocations per frame; a base with 50 hooks is ~75k vertices and ~50k short-lived objects per frame, all on the render thread, for geometry that never moves. Nothing drawn here is translucent: flex, sockets and bulbs all go to `RenderType.cutout()`, lit bulbs are full-bright cutout. So under the agreed rule every part of it is solid and can go to Flywheel; nothing must stay on the BER. (`LinePostVisual` already does exactly this for Line Post spans.)

The Electric Lights reviewer found the same thing independently and measured it: every frame, for every held span, the renderer recomputes the catenary (`Festoons.along`) for `max(8, ceil(length*3))` segments and builds a square rod per segment and per bulb with `Vec3` maths: `beam()` allocates ~25 `Vec3` per call (direction, two basis vectors, a 4-entry ring, 6 quads x 4 corner adds), `along()` 2 more per point. A 16-block string is 48 segments + 8 bulbs x 2 rods = 64 rods, about 1,700 `Vec3` plus a fresh `HashMap` and lambda for the light cache, every frame, plus three atlas sprite lookups. The geometry of a span never changes while the hooks stand; only the packed light does. This is solid, opaque geometry drawn through a BER every frame out to 128 blocks, which is what the brief says should go through Flywheel.

## How to see it happen

50-100 Festoon hooks in one base, camera anywhere within 128 blocks of them: profiler shows `FestoonRenderer.renderSafe` and GC churn every frame even when nothing changes; frame time scales with hook count.

## How to fix it

Add `client/FestoonVisual.java` modelled on `LinePostVisual`: in the constructor/`update`, lay the flex out as stretched unit-box `TransformedInstance`s (reuse `LinePostVisual.SEGMENT`-style model with the `flex` texture; the `segment(p, q, w, h)` matrix builder can be shared) and each socket/bulb as two more instances; store the bulb instances and their light positions. Implement `SimpleDynamicVisual.beginFrame` only to update the bulb instances' colour (`LightRenderer.runGlow`) and light (`FULL_BRIGHT` vs block light) when `lit`/`phase` changes (compare to the last value; nothing else per frame). Implement `updateLight` as `LinePostVisual` does. Register with `SimpleBlockEntityVisualizer.builder(ModBlockEntities.FESTOON.get()).factory(FestoonVisual::new).skipVanillaRender(be -> true)`, and re-lay-out on span changes with `VisualizationHelper.queueUpdate(be)` from `FestoonBlockEntity` the way `LinePostBlockEntity.clientSpansChanged` does. Keep `FestoonRenderer` for the no-Flywheel path.

If the Flywheel visual has to wait, the lights reviewer's stop-gap is cheaper than nothing: Cache the built mesh per span. In `FestoonRenderer` (or a small `FestoonMesh` class) keep a `Map<FestoonBlockEntity, List<float[]>>` (or a field on the BE for the client) holding, per span, the rod corner positions and normals relative to the hook (positions only depend on `eye(a)`/`eye(b)`), rebuilt only when `be.spans()` changes (compare a copy of the span list, or bump a version in `readSpans`). Per frame only sample the light every N segments (one lookup per block of string is enough) and emit vertices from the cached floats, with the bulb colour/light chosen by `lit`. Allocation drops to zero per frame. Longer term, register a Flywheel visual for `FESTOON` (as `LINE_POST` has `LinePostVisual` for spans: `MachineVisuals.java:95-98`) that lays the rods out once as `TransformedInstance`s of a unit cube model and only updates light and the bulb tint per frame, leaving the BER for Ponder and non-Flywheel paths.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Festoon strings are a Flywheel visual laid out once (FestoonVisual), with geometry shared with the fallback renderer (FestoonGeometry); the fallback no longer allocates per frame; the crack overlay still shows while mining.

Tests: FestoonGeometryTest (incl. sameCornersAsTheOldRod).
