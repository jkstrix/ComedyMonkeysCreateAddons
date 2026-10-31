# CAGE-02: Create drills break a locked-down / foreign Netherite cage

| | |
|---|---|
| **Severity** | medium |
| **Kind** | Exploit or balance hole |
| **Area** | Monster Cage & Fish Tank |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/cage/MonsterCageBlock.java:348-371` (getDestroyProgress / onDestroyedByPlayer / onRemove), `src/main/java/com/comedymonkey/createaddons/registry/ModBlocks.java:185-187` (strength 5 / 1200); Create `BlockBreakingKineticBlockEntity.isBreakable` (sources jar: `blockHardness == -1 || create:non_breakable`)

## What is wrong

D41 ("can't be broken while its field is down") and D46 ("only the owner can break it") are enforced only on the player-mining path (`getDestroyProgress` → 0, `onDestroyedByPlayer` → false). A Mechanical Drill (or Saw, or any mod's block breaker) calls `BlockHelper.destroyBlock` with no player: Create only refuses hardness -1 or the `create:non_breakable` tag, neither applies (hardness 5, no tag file in this mod). `onRemove` then runs `dropContents()`, which culls the Withers into Nether Stars and drops the jar, bypassing both the lock-down and the owner lock.

## How to see it happen

Player B puts a Mechanical Drill against player A's Netherite cage (field up or down, doesn't matter). The drill breaks the block in a few seconds; A's filled Netherite Jar, (count-1) Nether Stars, chow and output all drop at the drill for B to collect.

## How to fix it

Decide with the user which they want (both are small):
  - Hard: add `comedymonkeysaddons:netherite_monster_cage` to `data/create/tags/block/non_breakable.json` (new file, `{"values": [...]}`) so drills never break it; players still can via the owner path. Document that in the spec.
  - Soft: in `MonsterCageBlock.onRemove`, when `be.isNetherite() && be.isLockedDown()` and the removal isn't by the owner (`dropContents` can't tell), don't convert Withers into stars: spawn them as escaping Withers via the existing `escape()` logic instead, so a drill can't farm stars. This needs the escape code lifted out of the tick path.
  Either way add `netherite_monster_cage` to the Carry On block blacklist check (already done) and a game test with a drill.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** The Netherite Monster Cage is in create:non_breakable: drills and saws can't break it (maintainer's choice).

Tests: TankGameTests: drillsLeaveTheNetheriteCage.
