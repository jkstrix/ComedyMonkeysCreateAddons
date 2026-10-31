# LINE-03: Fluid networks walk every piece every tick to refresh the sight slots

| | |
|---|---|
| **Severity** | low |
| **Kind** | Performance |
| **Area** | Fluid & Power Lines |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/line/FluidLines.java:123-152` (`tick`), `:291-300` (`chunksAsBuilt`), `:495-557` (`Network.show`)

## What is wrong

`FluidLines.tick()` copies the whole `intakes` set to a new list every tick and, for every Intake, calls `n.chunksAsBuilt(level)` (a `hasChunk` per chunk of the network and per edge chunk) before the network's own once-per-tick guard. `Network.show()` then iterates every member every tick (unless the network is completely empty), doing several map lookups and allocating a `Shown` record for each full piece, only to compare it with `applied` and usually do nothing. The display only changes at known ticks (`filledAt`, `drainedAt`, `wetAt + SHOW_FOR`, `fillingUntil`), so this is per-tick work that could be event-driven.

## How to see it happen

A 2,000-block brass Fluid Line with 10 Intakes and 20 ends, full and idle: each tick ~10 × (chunks ≈ 130) `hasChunk` calls plus 2,000 `Shown` allocations and ~8,000 map lookups, for no change. Ten such lines on a server is ~100k allocations a tick on top of the real transfer.

## How to fix it

(1) In `tick()`, run `chunksAsBuilt` once per network per tick (remember the last tick it was checked on the `Network`, or check it from `Network.tick` under its `lastTick == now` guard). (2) In `show()`, keep a `nextChange` tick on the network: when `fill()` recomputes, set it to the minimum of all `filledAt`, `filledAt + per`, `drainedAt - per`, `drainedAt`, `fillingUntil` and the `wetAt + SHOW_FOR` expiries; return early from the member loop while `now < nextChange` and no `wetAt` changed this tick (deliver() only touches `wetAt` on `now % 5 == 0`). Cheaper still: iterate only the members whose state can change this tick (keep them in a small set keyed by tick).

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Fluid networks check their chunks once a tick and only walk their pieces when the display can change.

Tests: LineGameTests: lineStandsStillOnceItsTankFills.
