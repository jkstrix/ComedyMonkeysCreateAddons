# JUG-02: Spring Engine over capacity crashes every client that renders it

| | |
|---|---|
| **Severity** | medium |
| **Kind** | Something does not work |
| **Area** | Clockwork Juggernaut |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/client/juggernaut/SpringEngineRenderer.java:87-90` (`RUNES[lit - 1]` with `lit = Math.round(fill * 4)`), `src/main/java/com/comedymonkey/createaddons/content/juggernaut/SpringEngineBlockEntity.java:122-124` (`fill()` unclamped), `:292-297` (`read` takes `Stored` and `Fill` unclamped), `:135-137` (`comparatorLevel` unclamped)

## What is wrong

`fill()` is `stored / capacity()` and `capacity()` is the server config `springEngineCapacity * 20`. `stored` is clamped when wound (`add`), set (`setStored`) or placed from an item (`applyImplicitComponents`), but not when read back from NBT, and `shownFill` is synced unclamped. If the capacity config is lowered after engines were wound (an admin tunes it down, or a world is moved to a server with a smaller value), `fill()` becomes > 1, `lit` becomes 5..8 and `RUNES[lit - 1]` throws `ArrayIndexOutOfBoundsException` inside the block entity renderer (and inside `PartsVisual.beginFrame` with Flywheel on), which is a client crash. The chunk re-renders on rejoin, so every client that loads that chunk crashes again. `comparatorLevel()` also returns `1 + floor(fill * 14)` > 15, and the item tooltip reads "200% wound".

## How to see it happen

Wind a Spring Engine to full with the default config (58,982,400 SU-s). Stop the server, set `springEngineCapacity = 29491200` in the server config, start, stand in sight of the engine: client crash in "Rendering Block Entity" (or Flywheel's frame), repeatedly on every rejoin until the block is removed by command.

## How to fix it

In `SpringEngineBlockEntity.read`, clamp: `stored = Math.max(0, Math.min(capacity(), tag.getLong("Stored")))` and `shownFill = Mth.clamp(tag.getFloat("Fill"), 0, 1)`. Make `fill()` return `Mth.clamp((float) ((double) stored / capacity()), 0, 1)` and clamp `comparatorLevel()` to 15. In `SpringEngineRenderer.parts`, defensively clamp too: `int lit = Mth.clamp(Math.round(fill * 4), 0, 4)` and `COIL_*[Mth.clamp((int) (fill * 4), 0, 3)]`. Also clamp `SpringEngineItem.fill` to 1 for the tooltip.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Fill, comparator, tooltip and renderer are clamped, so an over-capacity engine can't crash clients.

Tests: SpringEngineBlockEntityTest, SpringEngineRendererTest.
