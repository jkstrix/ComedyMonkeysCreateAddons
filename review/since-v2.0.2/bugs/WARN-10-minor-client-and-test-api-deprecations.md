# WARN-10: Minor client and test API deprecations

| | |
|---|---|
| **Severity** | low |
| **Kind** | Something does not work |
| **Area** | Compiler warnings |
| **Confidence** | high |

## Where to look

`client/FishTankRenderer.java:92` (`TextureAtlas.LOCATION_BLOCKS`), `client/MachineRenderers.java:374` (`BlockModelShaper.getParticleIcon(BlockState)`), `content/line/LineInteractions.java:138` (`BlockState.getSoundType()`), `compat/jei/CMJeiPlugin.java:35` (`getLegacyStringSubtypeInfo`); tests: `RegistryFriendlyByteBuf(ByteBuf, RegistryAccess)` in `PipeCoverPayloadTest:51`, `ContraptionBlockSyncPayloadTest:58`, `StorageGameTests:375`, `HelperBotGameTests:134`, `MonsterCageBlockEntityTest:326`; `getLightEmission()` in `LavaGeneratorBlockTest:35-37`, `ModBlocksTest:85`; `Item.builtInRegistryHolder()` in `PlanningTest:51`

## What is wrong

Each has a drop-in replacement. `LOCATION_BLOCKS` is an alias of `InventoryMenu.BLOCK_ATLAS`. `getParticleIcon(state)` ignores model data. `getSoundType()` ignores position-dependent sound types; NeoForge's `state.getSoundType(level, pos, entity)` is the "more sensitive version". The JEI legacy method is kept for old JEI and already has `@SuppressWarnings("removal")`; add `"deprecation"`. The test byte-buf constructor needs `ConnectionType.NEOFORGE` as a third argument so codecs that branch on connection type behave as in game.

## How to see it happen

Mostly cosmetic. For `getSoundType()`: removing a casing whose sound depends on position plays the wrong break sound.

## How to fix it

`RenderType.eyes(InventoryMenu.BLOCK_ATLAS)`; `casing.getSoundType(level, be.getBlockPos(), player).getBreakSound()`; in `MachineRenderers.blockSprite` keep the call (there is no position for an item) but add `@SuppressWarnings("deprecation")` and a comment; in tests `new RegistryFriendlyByteBuf(Unpooled.buffer(), access, ConnectionType.NEOFORGE)`, `state.getLightEmission(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)`, `BuiltInRegistries.ITEM.wrapAsHolder(item)`.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Atlas constant, sound lookup and test helpers replaced; the item-sprite and JEI legacy calls carry a stated suppression.
