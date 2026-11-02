# CAGE-03: Jar release never checks the jar is the right kind for its monster

| | |
|---|---|
| **Severity** | low |
| **Kind** | Exploit or balance hole |
| **Area** | Monster Cage & Fish Tank |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/cage/MonsterJarItem.java:180-186` (create), `:196-224` (useOn), `src/main/java/com/comedymonkey/createaddons/content/cage/CatcherDispensing.java:71-77`, `src/main/java/com/comedymonkey/createaddons/content/cage/MonsterCageBlockEntity.java:1749-1752` (CaptiveHandler.isItemValid)

## What is wrong

`MonsterJarItem.create` only checks `CageRules.kindOf(type) != null` and `isCageable`, never `jar.holds(type)`. An *Iron* Jar whose `ENTITY_DATA.id` is `minecraft:wither` passes every check: `useOn` (netherite == false) releases a full-health Wither anywhere in the world, the dispenser branch releases it too, and `isItemValid` lets it into a Netherite cage (where `escape()` later hands back an *Iron* jar via `item.empty()`). D21 says a Wither may only ever come out into a Netherite cage. Legit play can't make such a jar (`canCatch` checks `holds`), so this needs creative `/give` or an NBT editor; it's a cheap defence-in-depth gap.

## How to see it happen

`/give @s comedymonkeysaddons:iron_jar[entity_data={id:"minecraft:wither"}]`, right-click the ground → a Wither spawns (also on a server where only the item was given, e.g. through a creative-mode player or a bugged recipe).

## How to fix it

In `MonsterJarItem.create(stack, level)`, after resolving `type`, add `if (!(stack.getItem() instanceof MonsterJarItem jar) || !jar.holds(type)) return Optional.empty();`. That one check covers useOn, the dispenser, `isItemValid` (via `fits` it already does the cage side) and `sample()`.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** A jar only releases or enters a cage with a monster its kind can hold.

Tests: TankGameTests: wrongJarLetsNothingOut.
