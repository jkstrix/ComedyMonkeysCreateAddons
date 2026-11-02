# FLY-04: Receiver scans every tube every tick, loads or not

| | |
|---|---|
| **Severity** | low |
| **Kind** | Performance |
| **Area** | Flywheel & Clockwork Tubes |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/tube/ClockworkReceiverBlockEntity.java:202-258` (`move`), `:239-250` (6 × `tube.relative(side)` per empty tube), `:357-374` (`refreshFigures` every 10 ticks), `:174-185` (`pass` → `deliver` every tick while the output is full)

## What is wrong

While the cog turns, `move()` visits every tube of the network each tick: `ClockworkTubeBlock.running(level, tube)` (`isLoaded` + `shouldTickBlocksAt`, two chunk lookups) and `level.getBlockEntity(tube)`, then for an empty tube six `relative()` allocations and `home.get()` lookups. At the 256-tube cap that's ~256 BE lookups, ~512 chunk lookups and ~1,500 `BlockPos` allocations per tick per Receiver even when nothing is in transit, plus the same walk again in `refreshFigures` every 10 ticks. `pass()` also calls `deliver()` (capability lookup + `insertItemStacked` attempt) every tick while the output is full.

## How to see it happen

Ten idle 200-tube networks turning with nothing to move: ~2,000 block-entity lookups and ~15,000 allocations per tick for no work.

## How to fix it

Track occupancy: keep an `int loaded` (or a `Set<BlockPos>` of tubes holding a load, maintained where `setLoad`/`loadMovedOn` are called, and rebuilt in `TubeNetwork.read`). In `move()`, skip the "take from behind" scan when no tube behind holds a load (or iterate only the loaded tubes and their forward neighbour). In `pass()`, when `deliver` moved nothing, wait a short cooldown (e.g. 4 ticks, as Create's funnels do) before trying again. Replace `tube.relative(side)` in the inner loop with a `BlockPos.MutableBlockPos`.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** The Receiver caches its tubes' block entities and the tubes behind each; a full output retries every 4 ticks (maintainer's choice).

Tests: TubeGameTests: aBackedUpNetworkLosesNothing.
