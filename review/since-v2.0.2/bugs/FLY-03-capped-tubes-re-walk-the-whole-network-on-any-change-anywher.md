# FLY-03: Capped tubes re-walk the whole network on any change anywhere

| | |
|---|---|
| **Severity** | medium |
| **Kind** | Performance |
| **Area** | Flywheel & Clockwork Tubes |
| **Confidence** | medium |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/tube/ClockworkTubeBlockEntity.java:336-354` (`tick`), `TubeJoints.java:617-642` (`refusal`: two `reach` walks of up to `max` tubes), `TubeJoints.java:523-529` (`changed` bumps one level-wide version), `ClockworkInletBlockEntity.java:228-232` and `ClockworkTubeBlockEntity.java:325-329` (`onLoad` bumps it on every chunk load)

## What is wrong

Every tube block entity ticks on the server and, once a second, if `TubeJoints.version(level)` moved and any of its joints is `CAPPED` for a reason other than `SHUT`, calls `ClockworkTubeBlock.rejoin` → `TubeJoints.compute` → up to 6 × `refusal`, each `refusal` running two BFS walks (`reach`) bounded only by `clockworkTubeMaxLength` (256) with `HashSet`/`ArrayDeque` allocations and ~6 `getBlockState`/`isLoaded` per visited tube. The version is a single counter per level, bumped by every tube/inlet/receiver placed, broken, turned, wrenched or chunk-loaded anywhere in that level. So a network at the length cap (every tube past the 256th is `CAPPED TOO_LONG`), or a run capped `TWO_RECEIVERS`, re-walks ~256 tubes several times per capped tube every time anyone anywhere places a tube or a chunk with tubes loads. The Receiver itself is smarter (`touches(changesSince)` filters by position) but the tubes aren't.

## How to see it happen

Build a 256-tube network plus a 50-tube tail past the cap (50 capped tubes). Another player keeps building tubes 1,000 blocks away: each placement bumps the version, and within a second each of the 50 capped tubes does 2-4 BFS walks of ~256 tubes (≈ 1,500 block lookups per walk) → roughly 300k block-state lookups per placement, on the server tick thread.

## How to fix it

Give the tubes the Receiver's filter: in `ClockworkTubeBlockEntity.tick`, replace `version == checked` with `long[] changes = TubeJoints.changesSince(level, checked); checked = version;` and only recheck when `changes == null` or some change is within, say, `clockworkTubeMaxLength` Manhattan blocks of `worldPosition` (cheap `BlockPos.of(at).distManhattan(worldPosition)`), since a change further away than the longest possible network can't alter this cap. Better still, drop the periodic recheck and have the Receiver do it: when `TubeNetwork.read` finishes, for each tube with a `TOO_LONG`/`TWO_RECEIVERS` refusal on the network edge call `rejoin` once (it already knows which tubes it touched). Also consider removing `tick()` from the tube entirely (SmartBlockEntity ticks every tube on both sides for a `floorMod` early-out; the javadoc says "Tubes don't tick").

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** A capped tube re-walks only when a change happened within reach of it.

Tests: TubeGameTests: onlyANearbyChangeReopensACap.
