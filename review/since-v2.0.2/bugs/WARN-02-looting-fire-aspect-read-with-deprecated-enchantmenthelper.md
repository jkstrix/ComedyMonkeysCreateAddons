# WARN-02: Looting / Fire Aspect read with deprecated EnchantmentHelper

| | |
|---|---|
| **Severity** | low |
| **Kind** | Something does not work |
| **Area** | Compiler warnings |
| **Confidence** | high |

## Where to look

`content/cage/MonsterCageBlockEntity.java:565,567`, `content/tank/FishTankBlockEntity.java:490`, `content/pen/AnimalPenBlockEntity.java:473`

## What is wrong

`EnchantmentHelper.getItemEnchantmentLevel(holder, stack)` is deprecated by NeoForge. It reads only the enchantments stored in the stack's NBT. The gameplay replacement `stack.getEnchantmentLevel(holder)` also asks the item (`IItemExtension.getAllEnchantments`), so a weapon from another mod that grants Looting without storing it in NBT is ignored by the cage, tank and pen today.

## How to see it happen

Put a weapon whose Looting comes from an item-level override (an "innate Looting" modded sword, or an enchantment-modifier mod) into a Monster Cage. The cage treats it as Looting 0.

## How to fix it

Replace each call with the instance method. Before: `EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.LOOTING), weapon)`. After: `weapon.getEnchantmentLevel(enchantments.getOrThrow(Enchantments.LOOTING))`. The `enchantments` lookup stays. Same for FIRE_ASPECT and in FishTank/AnimalPen. Run `MonsterCageBlockEntityTest` afterwards.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** stack.getEnchantmentLevel everywhere.
