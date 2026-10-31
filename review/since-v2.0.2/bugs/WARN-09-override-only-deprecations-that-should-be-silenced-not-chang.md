# WARN-09: Override-only deprecations that should be silenced, not changed

| | |
|---|---|
| **Severity** | low |
| **Kind** | Something does not work |
| **Area** | Compiler warnings |
| **Confidence** | high |

## Where to look

`setBlockState` overrides in `MagmaEngineBlockEntity:64`, `LavaGeneratorBlockEntity:133`, `BatteryBlockEntity:159`, `UpgradedChestBlockEntity:183`, `UpgradedBarrelBlockEntity:101`; `removeComponentsFromTag` overrides in `FishTankBlockEntity:953`, `AnimalPenBlockEntity:898`, `LineBlockEntity:537`, `SpringEngineBlockEntity:342`, `AutoHarvesterBlockEntity:451`, `BatteryBlockEntity:849`, `MonsterCageBlockEntity:1529`; `canBeAffected` and `finalizeSpawn` overrides in `ClockworkJuggernaut:1707,1804`; `state.rotate(rotation)` inside `mirror()` in 20 block classes (`CuckooClockBlock:132` etc.)

## What is wrong

These are `@Deprecated` with the meaning "override only, do not call from outside" (`finalizeSpawn`, `canBeAffected`, `setBlockState`, `removeComponentsFromTag`) or "vanilla's own pattern" (`BlockState.rotate` inside `mirror`, which vanilla itself does). Overriding them is correct. Together they are 40 of the 91 deprecation warnings and bury the real ones (WARN-01 to WARN-06).

## How to see it happen

None at runtime.

## How to fix it

Add `@SuppressWarnings("deprecation")` above each such override (and above `mirror()` where `state.rotate` is called) with a one-line comment "override-only API". Do not replace these. Then add `-Xlint:all` to `build.gradle` (`tasks.withType(JavaCompile).configureEach { options.compilerArgs << '-Xlint:all' }`) so new warnings are seen.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Override-only methods and vanilla's mirror pattern carry a stated suppression; build.gradle now compiles with -Xlint:all and the tree has no warnings.
