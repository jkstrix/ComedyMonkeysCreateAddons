# CAGE-06: Per-tick allocation while a Netherite field is down

| | |
|---|---|
| **Severity** | low |
| **Kind** | Performance |
| **Area** | Monster Cage & Fish Tank |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/cage/MonsterCageBlockEntity.java:1119-1132` (playerNear / playersNear), called from `runField()` `:932,937` every tick the field is down

## What is wrong

`playersNear()` allocates an `ArrayList` and a `Vec3.atCenterOf(worldPosition)` per player every tick for the whole countdown / escape clock (hours, if the field stays down), and `runField` calls it twice on the tick a countdown starts. Everything else in the field path is allocation-free, so this is the one leftover. Small per cage, but it's the hot path for exactly the cages the maintainer tuned.

## How to see it happen

Netherite cage with FE cut, 10 players online: 11 allocations/tick for 5 minutes, then for every game day thereafter.

## How to fix it

Give `playerNear()` its own loop without the list: `double r2 = range*range; double cx = worldPosition.getX()+0.5 ...; for (ServerPlayer p : server.players()) if (!p.isSpectator() && p.distanceToSqr(cx, cy, cz) <= r2) return true; return false;`. Keep `playersNear()` for the announcements only (every 30-60 s), and in `runField` reuse the `near` result instead of calling `playerNear()` again when `escapeIn < 0`.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** No allocation in the per-tick player check while a field is down.

Tests: covered by existing field tests.
