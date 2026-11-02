# WARN-04: Chunk-loaded checks use the deprecated hasChunkAt(BlockPos)

| | |
|---|---|
| **Severity** | low |
| **Kind** | Performance |
| **Area** | Compiler warnings |
| **Confidence** | high |

## Where to look

`content/line/LinePostBlock.java:175,196`, `content/light/StripLinks.java:25`, `content/light/LightMounts.java:53`, `content/light/HangingLampBlock.java:73`, `content/light/EdgeLinks.java:58`, `content/juggernaut/JuggernautArrival.java:43`, `content/light/Floodlights.java:143` (six-int `hasChunksAt`)

## What is wrong

`LevelReader.hasChunkAt(BlockPos)` and the six-int `hasChunksAt` are deprecated. The non-deprecated forms are `hasChunkAt(int blockX, int blockZ)` and `hasChunksAt(BlockPos from, BlockPos to)`. On a `Level` there is also `isLoaded(BlockPos)`, which the mod uses elsewhere (`PowerLines.java:177`). Behaviour is identical today; mixing the forms makes the "never load a chunk by accident" intent harder to audit.

## How to see it happen

None at runtime. Build output is noisy, which hides new warnings.

## How to fix it

Replace `level.hasChunkAt(p)` with `level.hasChunkAt(p.getX(), p.getZ())` (same semantics, not deprecated), or `level.isLoaded(p)` where `level` is a `Level`. In `Floodlights.loadedBetween` use `level.hasChunksAt(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ))`.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Every form of the game's chunk check on a LevelReader is deprecated, including the one the finding suggested, so all call sites go through one helper, content/Loaded, which holds the single suppression.
