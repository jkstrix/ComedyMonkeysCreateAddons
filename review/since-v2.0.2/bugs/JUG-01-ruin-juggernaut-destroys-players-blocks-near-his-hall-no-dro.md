# JUG-01: Ruin Juggernaut destroys players' blocks near his hall, no drops

| | |
|---|---|
| **Severity** | medium |
| **Kind** | Exploit or balance hole |
| **Area** | Clockwork Juggernaut |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/juggernaut/ClockworkJuggernaut.java:1123-1147` (`crumbles`, `crumbleAhead`, `crumbleAround`), `:1413-1427` (`safeAhead` treats crumble-able walls as walkable), tag `src/main/resources/data/comedymonkeysaddons/tags/block/juggernaut_ruin_blocks.json`

## What is wrong

A Juggernaut found in a ruin keeps `home` for life. `crumbles(p)` only asks two things: is `p` within 45 blocks of `home`, and is the block in `#comedymonkeysaddons:juggernaut_ruin_blocks` (deepslate bricks, cracked bricks, tiles, cobbled and polished deepslate, mossy cobblestone, tile slabs, vines, moss carpet, End Stone bricks and slabs). Nothing ties the block to the structure, so any block a player placed of those very common building materials within 45 blocks of the hall is destroyed with `destroyBlock(p, false, this)`, i.e. with no drops, whenever he walks (`crumbleAhead`, every tick while `zza > 0` or charging), stomps (`crumbleAround(3)` on every stomp and leap landing) or hops. `safeAhead` ignores such blocks, so he walks straight into a player's wall rather than round it. D62 says "its own building blocks only" and D2 "never breaks or changes blocks"; the code cannot tell its own from a player's.

## How to see it happen

Find a ruin; build an arena wall or a small base of deepslate bricks / cobbled deepslate / polished deepslate pillars 20 blocks from the hall (a natural thing to do, the ruin is made of it). Wake him. He charges through the wall and stomps beside the pillars: every block vanishes, nothing drops. Same for vines or moss carpets a player placed. A respawn hut of deepslate tiles 40 blocks away goes the same way.

## How to fix it

In `ClockworkJuggernaut.crumbles(BlockPos p)`, also require the position to be inside the ruin structure itself: `level()` is a `ServerLevel` here, so add `((ServerLevel) level()).structureManager().getStructureWithPieceAt(p, <the juggernaut_ruin structure>).isValid()` (look the `Structure` up by `ResourceKey` from `Registries.STRUCTURE`, `comedymonkeysaddons:juggernaut_ruin` / `juggernaut_ruin_end`; cache the two holders in a field on first use). `getStructureWithPieceAt` is cheap (a lookup in the chunk's structure references). That limits crumbling to the pieces' bounding boxes. Because a player can still build inside the ruin's own 81x81 footprint, also change the three `destroyBlock(p, false, this)` calls in `crumbleAhead`/`crumbleAround` to `destroyBlock(p, true, this)` so a player's block at least drops (the ruin's own loose bricks dropping is harmless, they were free anyway). Apply the same structure check inside `safeAhead` (the `!crumbles(...)` branch) so he walks round a player's wall outside the ruin instead of through it.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** He crumbles only blocks the ruin itself built (its pieces, and the field's own columns, vines and moss, checked against what generation laid). Still no drops (maintainer's choice). A ruin placed with /place structure no longer crumbles.

Tests: JuggernautGameTests: aRuinOneLeavesAPlayersWallStanding, theRuinClaimsOnlyItsOwnColumns, theRuinFieldKnowsWhereItBuilt.
