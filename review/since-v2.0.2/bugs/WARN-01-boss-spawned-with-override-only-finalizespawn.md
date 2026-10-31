# WARN-01: Boss spawned with override-only finalizeSpawn

| | |
|---|---|
| **Severity** | low |
| **Kind** | Something does not work |
| **Area** | Compiler warnings |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/juggernaut/WindingKeyBlockEntity.java:196`

## What is wrong

The Winding Key spawns the Juggernaut and calls `j.finalizeSpawn(...)` directly. NeoForge marks `Mob.finalizeSpawn` as `@Deprecated @ApiStatus.OverrideOnly`: mods must call it through `EventHooks.finalizeMobSpawn`, which fires `MobSpawnEvent.FinalizeSpawn`. Calling it directly skips that event, so spawn-control mods never see the boss appear.

## How to see it happen

Install a spawn-management mod (one that cancels or logs boss spawns). Perform the ritual. The Juggernaut appears without the FinalizeSpawn event firing; the other mod's rules are bypassed.

## How to fix it

In `WindingKeyBlockEntity`, replace the direct call with `net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(j, server, server.getCurrentDifficultyAt(pos), MobSpawnType.TRIGGERED, null);`. It returns the same `SpawnGroupData`. It also honours a cancel from another mod, so check `j.isRemoved()` before adding the boss to the level if the add happens after this call. Leave the `@Override finalizeSpawn` in `ClockworkJuggernaut` as it is: overriding is the intended use; silence its own warning with `@SuppressWarnings("deprecation")`.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** The boss is spawned through EventHooks.finalizeMobSpawn.

Tests: JuggernautGameTests: theRitualIsWoundUp.
