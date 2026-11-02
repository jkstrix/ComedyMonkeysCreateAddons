# WARN-06: Monster Chow food effect built with a shared MobEffectInstance

| | |
|---|---|
| **Severity** | low |
| **Kind** | Something does not work |
| **Area** | Compiler warnings |
| **Confidence** | high |

## Where to look

`content/cage/MonsterChowItem.java:25`

## What is wrong

`FoodProperties.Builder.effect(MobEffectInstance, float)` is deprecated by NeoForge because the one instance is shared by every eater. The supplier form creates a fresh instance each time, so a mod that mutates applied effect instances cannot leak changes across players.

## How to see it happen

Rare: a mod that edits applied effect instances in place changes the duration stored in the static FOOD field; every later Monster Chow applies the edited effect.

## How to fix it

`.effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8f)`.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Monster Chow uses the supplier form.
