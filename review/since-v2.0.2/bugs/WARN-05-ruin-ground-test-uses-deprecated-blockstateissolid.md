# WARN-05: Ruin ground test uses deprecated BlockState.isSolid()

| | |
|---|---|
| **Severity** | low |
| **Kind** | Something does not work |
| **Area** | Compiler warnings |
| **Confidence** | medium |

## Where to look

`content/juggernaut/ruin/RuinField.java:252`

## What is wrong

`isSolid()` is a deprecated legacy flag kept for a few old call sites; it does not mean "a block you can build on". It is true for some non-full blocks and false for some you can stand on. The ruin's ground check decides where hall and field pieces may sit.

## How to see it happen

A ruin generated over slabs, snow layers or modded partial blocks is accepted or rejected wrongly, so pieces float or are skipped.

## How to fix it

Use the collision shape instead: `below.isFaceSturdy(level, p, Direction.UP)` (the top face can hold a block) or `below.isCollisionShapeFullBlock(level, p)`. Keep the fluid, leaves and logs tests.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** The ruin's ground test uses isFaceSturdy; dirt paths, farmland and bottom slabs no longer count as ground.

Tests: JuggernautGameTests: theRuinStandsOnNaturalGround.
