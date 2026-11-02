# JUG-05: Leap aim recomputed every tick of the coil

| | |
|---|---|
| **Severity** | low |
| **Kind** | Performance |
| **Area** | Clockwork Juggernaut |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/juggernaut/ClockworkJuggernaut.java:842-847` (`tickLeap`), `:906-934` (`leapLanding` x2), `:951-959` (`arcClear`: 9 x `noCollision` of his 6x7x6 box), `:937-948` (`footing`)

## What is wrong

For the 30 ticks of the coil (`t < 1.5`), `tickLeap` calls `leapLanding(target, seconds)` every tick. Each call tries the led spot and then the target's spot, and each of those does `level().noCollision(this, box)` once for the landing plus nine times along the arc (`arcClear`), plus `footing` (up to 12 block reads). `noCollision` over a 6x7x6 box walks about 300 block shapes and scans entity collisions, so one coil tick costs around 20 `noCollision` calls, roughly 6-7k block-shape lookups, repeated 30 times per leap (and again for every band change in `pick`, which also calls `leapLanding`). It only feeds `turnToward` and the particle ring, which are redrawn every 2 ticks anyway.

## How to see it happen

Stage 2+, player 15 blocks away: `/debug` or spark shows `ClockworkJuggernaut.tickLeap` dominating the entity's tick during each 1.5 s coil; with two or three leapers (several summoned bosses) the spike is visible.

## How to fix it

In `tickLeap`, cache the aim: compute `aim` only when `tickCount % 5 == 0` (or when the target moved more than a block since the last computation) and keep it in a field `coilAim`; use the cached value for `turnToward` and `markLanding` on the other ticks. Keep the exact final check at `at(1.5f)` as it is (that one must be fresh). Optionally make `arcClear` sample 5 points instead of 9 and `deflate(0.3)` already helps.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** The leap aim is recomputed every 4 ticks of the coil; the take-off check stays fresh.

Tests: existing leap tests.
