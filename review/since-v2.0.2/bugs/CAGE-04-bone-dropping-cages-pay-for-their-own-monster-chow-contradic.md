# CAGE-04: Bone-dropping cages pay for their own Monster Chow (contradicts D9)

| | |
|---|---|
| **Severity** | low |
| **Kind** | Exploit or balance hole |
| **Area** | Monster Cage & Fish Tank |
| **Confidence** | high |

## Where to look

`src/main/resources/data/comedymonkeysaddons/recipe/mixing/monster_chow.json` (1 wheat flour + 1 bone meal + 1 cinder flour → 3 chow), `src/main/java/com/comedymonkey/createaddons/content/cage/CageRules.java:323-333` (1 chow per common monster), `src/main/java/com/comedymonkey/createaddons/content/cage/MonsterCageBlockEntity.java:556-597` (vanilla loot per cull)

## What is wrong

Spec D9 says "the drops can't pay for themselves". A skeleton (also stray, bogged, wither skeleton) costs 1 chow and drops 0-2 bones (avg 1, more with Looting). 1 bone → 3 bone meal → 3 mixing batches → 9 chow, with only wheat flour and cinder flour (milled wheat / netherrack) added. Each skeleton therefore returns ~9× its chow cost in chow alone, plus arrows. The maintainer's rule is "vanilla drop rates always; limit pace by cost, not loot", so this is a balance observation, not a code bug: the pace is still bounded by the interval, but the chow is not a running cost for these cages.

## How to see it happen

Andesite cage, skeleton jar, 64 RPM: 1 skeleton / 20 s eating 1 chow. Cull 32 every ~10 min → ~32 bones → 96 bone meal → 288 chow; the cage used 32.

## How to fix it

Design choice for the user (don't change silently): either raise the chow cost for the `andesite` tag types that drop bones, or make the chow recipe need something that isn't a cage product (e.g. drop bone meal for a Create product like Dough or Powdered Obsidian), or amend D9 to say chow is a pacing cost not a sink. Code change for the recipe is only the JSON.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**won't fix.** Accepted by the maintainer: chow from bone drops is fine.
