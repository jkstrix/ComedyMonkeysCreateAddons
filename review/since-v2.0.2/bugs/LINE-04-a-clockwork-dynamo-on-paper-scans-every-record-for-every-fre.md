# LINE-04: A Clockwork Dynamo on paper scans every record for every free source, every tick

| | |
|---|---|
| **Severity** | low |
| **Kind** | Performance |
| **Area** | Fluid & Power Lines |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/line/PowerGrid.java:270-278` (`Paper.paperRate`), `:729-737` (`lastToSee`), `:301-310` (`making`), `:316-325` (`advance`), `:401-416` (`of`)

## What is wrong

`Paper.paperRate(level)` is called by `advance()` (every tick for every paper source the network draws, via `draw()` in `Network.flow`) and by `making()`. For a dynamo record it loops its `freeShares` and, for each, calls `running(level, src)` and `of(level).lastToSee(src)`. `lastToSee` is a linear scan of every generator record in the level, and `of(level)` goes through `DimensionDataStorage.computeIfAbsent` with a freshly allocated `SavedData.Factory` each call. Cost per tick ≈ dynamos-on-paper × free sources × all records.

## How to see it happen

20 dynamos away, each with 6 water wheels, and 300 generator records in the level: 20 × 6 × 300 = 36,000 map/field reads plus 120 `Factory` allocations and data-storage lookups per tick, every tick, while the base draws from them.

## How to fix it

Pass the `PowerGrid` into `advance`/`paperRate` (callers already have it: `draw(Paper, ...)` and `tickAway` are grid methods) instead of calling `of(level)`. Replace the scan with an index: keep `Map<BlockPos, Paper> lastSeer` on the grid, updated in `note()` (when `refresh`) and when `freeShares` change in `tickAway`, and rebuild it once in `of()` after loading `Records`. Also cache the computed away rate per record for the current tick (`paperRateAt == now`), since `advance` and `making` both ask in the same tick.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Dynamo paper rates use an index of who last saw each source and a per-tick cache; no SavedData lookups per call.

Tests: PaperAwayRateTest (updated).
