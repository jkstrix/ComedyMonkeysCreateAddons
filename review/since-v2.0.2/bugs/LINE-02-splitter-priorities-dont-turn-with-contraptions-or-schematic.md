# LINE-02: Splitter priorities don't turn with contraptions or schematics

| | |
|---|---|
| **Severity** | medium |
| **Kind** | Something does not work |
| **Area** | Fluid & Power Lines |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/line/LineBlockEntity.java:121-130` (`transform`), `src/main/java/com/comedymonkey/createaddons/content/line/PowerLineBlockEntity.java:35-36, 327-345` (`priority` by `Direction.ordinal()`)

## What is wrong

`LineBlockEntity.transform(...)` rotates `fittingSide` and the wrenched `joints` when Create's `StructureTransform` is applied (contraption disassembly, schematicannon with rotation; Create's `Contraption.addBlocksToWorld` calls `transform.apply(blockEntity)` after `loadWithComponents`, verified in the Create 6.0.10 sources). `PowerLineBlockEntity` adds a `priority[6]` array indexed by world direction for the Splitter and does not override `transform`, so after a 90° turn the First branch is applied to the wrong world direction. The `links` array is fine (recomputed by `relink()` on load) and `PowerLineBlock.rotate/mirror` rotate the faces, so only the priorities (and, cosmetically, the `encasing` BlockState's axis/facing) are left behind.

## How to see it happen

Wire junction on a floor, Splitter fitted, screen: North = First, East/South = Second. Put it on a Create cart contraption (or copy with a schematicannon rotated 90°), disassemble turned 90° clockwise. The junction's arms now run East/South/West; `priority[NORTH]=0` still, so `branches()` = {E,S,W} all read 1 → `firstBranch()` null, the dial shows the fork and the East branch (the former North) is no longer served first. With a different turn the First lands on the wrong branch instead.

## How to fix it

In `PowerLineBlockEntity`, override `transform(BlockEntity be, StructureTransform transform)`: call `super.transform(...)`, then build `byte[] turned = {1,1,1,1,1,1}; for (Direction d : Direction.values()) turned[transform.rotateFacing(transform.mirrorFacing(d)).ordinal()] = priority[d.ordinal()]; System.arraycopy(turned, 0, priority, 0, 6);`. Optionally in `LineBlockEntity.transform` also rotate `encasing` with `encasing.rotate(transform.rotation)` / `mirror(...)` so a log casing keeps its axis.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Splitter priorities turn with contraptions and schematics; an encasing keeps its axis too.

Tests: SplitterTurnTest.
