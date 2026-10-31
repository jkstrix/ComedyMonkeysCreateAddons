# HBOT-02: BER draw path looks up the render type through `Util.memoize` every frame

| | |
|---|---|
| **Severity** | low |
| **Kind** | Performance |
| **Area** | Helper Bot |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/client/helperbot/HelperBotModels.java:92-94` (`renderType`), `:121` (`draw` calls `renderType(dye)`)

## What is wrong

`HelperBotModels.draw` calls `renderType(dye)` → `RenderType.entityCutoutNoCull(texture)` for every bot drawn through the BER path (Flywheel off, Ponder, the shop screen, the item renderer). `entityCutoutNoCull` goes through `Util.memoize`, which builds a `Pair` key and does a `ConcurrentHashMap` lookup per call. The 17 render types are already built once into `RENDER_TYPES` (`:45-52`); nothing needs re-looking-up.

## How to see it happen

Flywheel disabled (or an unsupported renderer), a station with 10 placed bots in view: 10 `Pair` allocations and 10 CHM lookups per frame for values that never change. Same for every bot item in a JEI/creative tab page.

## How to fix it

in `HelperBotModels`, add `private static final RenderType PLAIN_TYPE` and `private static final Map<DyeColor, RenderType> DYED_TYPES = new EnumMap<>(DyeColor.class)` filled in the same static block that builds `RENDER_TYPES`, and make `renderType(dye)` return `dye == null ? PLAIN_TYPE : DYED_TYPES.get(dye)`. `RENDER_TYPES` can then be built from those two.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Render types come from the prebuilt table.

Tests: existing HelperBotClientTest.
