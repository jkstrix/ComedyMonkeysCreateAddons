# ELEC-05: Generators allocate a Paper record every tick just to compare kinds

| | |
|---|---|
| **Severity** | low |
| **Kind** | Performance |
| **Area** | Battery, Dynamo & generators |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/line/PowerGrid.java:752` (`Paper fresh = g.newPaper();` in `note()`), driven every server tick by `ClockworkDynamoBlockEntity.tick()` (`:239`), `LavaGeneratorBlockEntity.tick()` (`:288`) and `GeothermalControllerBlockEntity.tick()` (`:296`).

## What is wrong

`note()` runs every tick for every loaded generator and calls `g.newPaper()`, which allocates a `Paper`, a `Fuel[]` and one `Fuel` per fuel (`PowerGrid.java:248-253`), only to read `fresh.kind` and `fresh.fuels.length`; the object is kept only when `p == null`. Two to four short-lived objects per generator per tick for nothing. (The file belongs to the Lines area; the per-tick callers are in this one.)

## How to see it happen

50 Lava Generators and a dozen dynamos: 150-200 allocations per tick, all garbage.

## How to fix it

Add `String paperKind()` / `int paperFuels()` to `PowerGrid.Generator` (constants per class) and compare those in `note()` and `back()`; call `newPaper()` only when a record is actually created.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Generators compare paper kinds without building a record.

Tests: GeneratorRecordKindTest.
