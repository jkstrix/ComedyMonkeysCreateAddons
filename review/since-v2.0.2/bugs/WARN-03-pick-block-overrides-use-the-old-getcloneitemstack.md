# WARN-03: Pick-block overrides use the old getCloneItemStack

| | |
|---|---|
| **Severity** | low |
| **Kind** | Something does not work |
| **Area** | Compiler warnings |
| **Confidence** | high |

## Where to look

`content/light/HangingCableBlock.java:141`, `content/light/FestoonHookBlock.java:144`, `content/helperbot/HelperBotBlock.java:135`, `content/clock/CuckooClockWeightsBlock.java:93`, `content/chest/UpgradedChestBlock.java:274`, `content/chest/UpgradedBarrelBlock.java:147`

## What is wrong

These override the vanilla `getCloneItemStack(LevelReader, BlockPos, BlockState)`, which NeoForge marks deprecated ("use more sensitive version"). NeoForge's hook is `getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player)`; its default delegates to the vanilla one, so it works today, but the override cannot see who is picking or where they clicked, and a future NeoForge may stop delegating.

## How to see it happen

No behaviour bug today. If NeoForge removes the delegation, pick-block on these six blocks returns the plain block item (the Helper Bot loses his colour on pick-block).

## How to fix it

Change each override's signature to the NeoForge one and keep the body. Example for HelperBotBlock:
```java
@Override
public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
    ItemStack stack = super.getCloneItemStack(state, target, level, pos, player);
    if (level.getBlockEntity(pos) instanceof HelperBotBlockEntity be && be.dye() != null)
        stack.set(ModDataComponents.HELPER_BOT_DYE.get(), be.dye());
    return stack;
}
```
Import `net.minecraft.world.phys.HitResult` and `net.minecraft.world.entity.player.Player`. Any unit test calling the old signature must call the new one.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed (suppressed).** The six pick-block overrides keep the 3-argument method with a stated suppression: Create's Display Link screen still calls it directly, so moving to NeoForge's 5-argument hook broke its icons (caught in review).
