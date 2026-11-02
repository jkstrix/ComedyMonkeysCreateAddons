# WARN-07: Raw types and redundant casts

| | |
|---|---|
| **Severity** | low |
| **Kind** | Something does not work |
| **Area** | Compiler warnings |
| **Confidence** | high |

## Where to look

rawtypes: `content/line/PowerLines.java:57`, `content/electric/BatteryBlockEntity.java:76`; redundant casts (14): `content/line/PowerLines.java:175`, `content/helperbot/HelperBotDyeRecipe.java:42`, `client/ponder/CageScenes.java:393`, and 11 in gametests (`TankGameTests:86,245,297`, `CagePerformanceGameTests:199,276,345`, `CageFieldGameTests:107`, `LineGameTests:925`, `LightGameTests:421`, `JuggernautGameTests:122`, `BatteryGameTests:155`)

## What is wrong

Two generic arrays are created raw (`new Set[INTERVAL]`, `new BlockCapabilityCache[4]`) and carry `@SuppressWarnings("unchecked")`, but javac reports `rawtypes` separately. Fourteen casts cast an expression to its own static type and do nothing.

## How to see it happen

None at runtime; noise.

## How to fix it

Widen the two suppressions to `@SuppressWarnings({"unchecked", "rawtypes"})`. Delete each redundant `(Type)`; e.g. `PowerLines.java:175`: `Math.floorMod(long, int)` already returns `int`, so write `phases[Math.floorMod(now, INTERVAL)]`.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Raw generic arrays carry a stated suppression; 14 redundant casts removed.
