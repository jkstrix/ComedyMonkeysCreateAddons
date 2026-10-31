# LINE-05: A span whose partner post is carried off stays on the post left behind

| | |
|---|---|
| **Severity** | low |
| **Kind** | Something does not work |
| **Area** | Fluid & Power Lines |
| **Confidence** | medium |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/line/LinePostBlock.java:289-299` (`onRemove`), `src/main/java/com/comedymonkey/createaddons/content/line/LinePostBlockEntity.java:137-152, 163-173` (`dropBrokenSpans`, `onLoad`)

## What is wrong

`LinePostBlock.onRemove` only takes spans down when `level.getBlockEntity(pos) instanceof LinePostBlockEntity`. Create's `Contraption.removeBlocksFromWorld` calls `world.removeBlockEntity(add)` before `setBlock(air)` (Create 6.0.10 sources, `Contraption.java:1052-1060`), so a post picked up by a contraption never tells its partner. The partner only tidies in `dropBrokenSpans()`, which runs on its own `onLoad`. Until that chunk is reloaded the stationary post keeps a ghost span: its insulator counts as occupied (`canTie` → "span_insulator_full" / "span_other_tier"), and the span's wire refund is delayed to the reload (owner end) or never happens on that end (non-owner end; the moving end refunds on landing if it owns it). No duplication (I traced both owner cases), but D25 ("a span whose two ends move apart comes down and drops its wire") is only met after a chunk reload. The same happens when a post is removed by anything else that drops the block entity first.

## How to see it happen

Two posts A (owner) and B with a span; A sits on a minecart contraption. Assemble and drive off. B still shows the span as held (shears on B's insulator: the item refund is paid now, fine), but try to hang a brass span onto B's top insulator that held a copper span: "another tier" until B's chunk is unloaded and loaded again. A, when disassembled elsewhere, pops the wire (owner) because its offset now points at nothing.

## How to fix it

Make the stationary end notice without a reload. Two small changes: (1) in `PowerLines.read(BlockPos p, ...)`, when a position that the grid knows as a pole head (`grid.piece(new Node(p, POLE)) != null`) is read and is no longer a `LinePostBlock`, look up the piece's links and, for each linked pole whose position is loaded, call `dropBrokenSpans()` on that `LinePostBlockEntity` (the grid already knows both ends of every span, so no search is needed); `onRemove` of the air-set already calls `PowerLines.changed(pos)`, which gets here. (2) As a safety net, have `LinePostBlockEntity.dropBrokenSpans()` also run from `PowerLines.tick` once a second for the posts in `due`-style buckets (loaded only, as the method already guards). Keep refunding only on the owner end so the one-refund rule holds.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** A post or battery tower left behind drops a span whose partner was carried off, at once; only the owner end refunds the wire.

Tests: LineGameTests: postLeftBehindDropsACarriedOffSpan, towerDropsASpanToACarriedOffPost.
