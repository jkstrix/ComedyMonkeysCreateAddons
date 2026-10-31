# LIGHT-01: Run lights rebuild their piece list every tick, and every run re-walks on any change anywhere

| | |
|---|---|
| **Severity** | medium |
| **Kind** | Performance |
| **Area** | Electric Lights |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/light/RunLightBlockEntity.java:24` (`private static int changes`), `:92-94` (`runsChanged`), `:127-136` (`onLoad`/`onChunkUnloaded` bump it), `:152-172` (`run()`), `:219-234` (`follow()`); callers `:43-50` (`runEnergy.receiveEnergy`), `:58-71`, `:103-109`, `:119`; `EdgeLightBlockEntity.java:495,547`; `LightStripBlock.java:178,188`; `Festoons.java:601,632`

## What is wrong

`run()` caches the loaded-piece list only for the current game tick (`piecesAt == now`). The power network delivers a twentieth of the plan every tick (`PowerLines.java:62,979`) by calling `runEnergy.receiveEnergy` on every piece a wire touches, so every fed piece rebuilds its list every tick: `level.isLoaded(p)` + `level.getBlockEntity(p)` for each of up to 1024 pieces, plus a new `ArrayList`. Separately, `changes` is one static int shared by every level and by client and server in singleplayer; it is bumped by every strip/edge/festoon placed, removed, loaded or unloaded (also on the client via `handleUpdateTag`), after which every fed piece of every run in the world does the full BFS in `follow()` again. For strips `joined()` costs 6 faces x 4 sides x up to 2 `has()` lookups (`hasChunkAt` + `getBlockState`) per piece, i.e. about 50 state lookups per piece; for edges each neighbour is a `getBlockEntity`.

## How to see it happen

A town has three strip runs of 200 pieces, each fed at 2 points. Steady state: 6 fed pieces x 200 lookups = 1200 `getBlockEntity` calls every tick for nothing. A player flies past at render distance: every chunk containing any strip that loads or unloads (client or server) bumps `changes`, and on the next delivery tick each fed piece BFS-walks 200 pieces x ~50 lookups = 10k lookups, x6 = 60k lookups in one tick, repeated for every chunk event while the player moves. Edge Lights do the same with `getBlockEntity` per neighbour.

## How to fix it

In `RunLightBlockEntity.run()` keep `pieces` until `changes` moves (drop the `piecesAt == now` condition; keep `piecesRun == runAt`), and when iterating skip `piece.isRemoved()` so a piece removed mid-tick is harmless (removal already bumps `changes` via `onRemove`/`onChunkUnloaded`). Make the invalidation local: replace the static `int changes` with a per-level counter, e.g. `private static final Map<Level, int[]> CHANGES = new WeakHashMap<>()` keyed by the level (or by `level.dimension()` + `isClientSide`), and have `runsChanged(Level)` take the level; stop bumping from the client paths (`EdgeLightBlockEntity.handleUpdateTag`, client `onLoad`) since nothing on the client calls `run()`. Better still, share one `Run` object between all pieces of a run (store it on each piece when `follow()` finishes) so k feed points don't each walk and rebuild the same run; `receiveEnergy` on any fed piece then uses the shared list and the shared `next` round-robin index, which also fixes the `feeds`-division approximation in `getEnergyStored`.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Runs keep their piece list until the run changes; the change counter is per level and server-only.

Tests: LightGameTests: aRunSeesAPieceAddedOrTakenAwayAtOnce.
