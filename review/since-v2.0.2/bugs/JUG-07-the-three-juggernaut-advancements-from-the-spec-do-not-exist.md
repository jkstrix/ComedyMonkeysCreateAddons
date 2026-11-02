# JUG-07: The three Juggernaut advancements from the spec do not exist

| | |
|---|---|
| **Severity** | low |
| **Kind** | Something does not work |
| **Area** | Clockwork Juggernaut |
| **Confidence** | high |

## Where to look

`src/main/resources/data/comedymonkeysaddons/advancement/` (no file mentions `clockwork_juggernaut`, `mainspring`, `spring_engine` or `winding_key`); spec `design/specs/clockwork-juggernaut.md` D41 ("Wound Up", "Run Down", "Tightly Wound") and the Data line of Technical Notes

## What is wrong

D41 promises advancements for summoning one, defeating one and filling a Spring Engine. None is in the data; the only "wound up" advancement is Trade Bot's. The Mainspring (Rarity EPIC) and the boss therefore give no advancement toast and no entry in the tree, and the spec's own checklist item is unmet. (Note JUG-03: when they are added, a `player_killed_entity` criterion will see a `generic` killing blow unless that fix lands too.)

## How to see it happen

Summon and kill him, craft and fill a Spring Engine: no advancement.

## How to fix it

Add `advancement/wound_up.json` (criterion `minecraft:summoned_entity` with `entity.type = comedymonkeysaddons:clockwork_juggernaut`, fired from `WindingKeyBlockEntity.summon` via `CriteriaTriggers.SUMMONED_ENTITY.trigger(serverPlayer, j)` for players within 32 blocks, or a custom trigger), `run_down.json` (`minecraft:player_killed_entity`, same entity type; relies on JUG-03 so the kill is credited), and `tightly_wound.json` (custom trigger fired from `SpringEngineBlockEntity.add` when `stored` reaches `capacity()`, for the nearest player within 16 blocks, or an `inventory_changed` fallback keyed on a full `spring_engine` item). Add lang entries and parents in the mod's advancement root.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Wound Up, Run Down and Tightly Wound added; who earns them, icons, frames and text confirmed by the maintainer.

Tests: JuggernautAdvancementsTest; JuggernautGameTests: theRitualIsWoundUp, aFullSpringEngineIsTightlyWound.
