# HBOT-03: Pick-block keeps the dye but drops the anvil name

| | |
|---|---|
| **Severity** | low |
| **Kind** | Something does not work |
| **Area** | Helper Bot |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/helperbot/HelperBotBlock.java:134-140` (`getCloneItemStack`); compare `HelperBotBlockEntity.java:154-161` (`collectImplicitComponents`) and `src/main/resources/data/comedymonkeysaddons/loot_table/blocks/helper_bot.json` (copies `helper_bot_dye` and `minecraft:custom_name`)

## What is wrong

Breaking a named bot keeps his name (loot table copies `custom_name`; `collectImplicitComponents` sets it), the dye recipe (`HelperBotDyeRecipe.assemble`) and washing (`RecipeApplierMixin`) both go out of their way to keep it, and goggles show it. But `getCloneItemStack` only sets the dye component, so middle-click (creative pick-block) on "Driver Dave" gives a nameless red bot. Inconsistent with the rest of the name-keeping work.

## How to see it happen

creative mode, place a bot renamed "Driver Dave" on an anvil, middle-click him: the stack in hand is "Red Helper Bot" with no custom name.

## How to fix it

in `HelperBotBlock.getCloneItemStack`, after setting the dye, also copy the name: `if (be.name() != null) stack.set(DataComponents.CUSTOM_NAME, be.name());` (the `instanceof HelperBotBlockEntity be` pattern variable is already there; restructure the `if` so both components are copied inside one block).

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Pick-block keeps the bot's name as well as his colour.

Tests: HelperBotGameTests: pickingHimKeepsHisColourAndName.
