# FLY-01: Upgraded Chest lid vanishes while open (Flywheel on)

| | |
|---|---|
| **Severity** | high |
| **Kind** | Rendering and Flywheel |
| **Area** | Flywheel & Clockwork Tubes |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/client/parts/MachineVisuals.java:278-279` (registration), `MachineVisuals.java:310-317` (`visualWhen`: factory picks `IdleVisual` when `active` is false at creation; `skipVanillaRender(partsOnly::test)` is always true for the chest), `src/main/java/com/comedymonkey/createaddons/client/UpgradedChestRenderer.java:19-24, 50-52` (the comment relies on "Flywheel makes the visual again when it opens or closes, a block state change")

## What is wrong

The chest is registered with `visualWhen(UPGRADED_CHEST, parts, partsOnly = true, open)`. A chest is almost always closed when its chunk loads, so Flywheel creates an `IdleVisual` for it. Flywheel 1.0.6 does notice the block-state change (its `visualmanage/LevelRendererMixin` hooks `LevelRenderer.setBlockDirty` and calls `queueUpdate` for the block entity there), but an update only calls `Visual.update(partialTick)`, which is a no-op in `AbstractVisual`, in `IdleVisual` and in `PartsVisual`; a visual is only ever re-created on `queueRemove` + `queueAdd` (block entity removed or added, or the mod's own `revisualize`). Verified by disassembling `Storage.update` in the Flywheel jar bundled with Create 6.0.10. A same-block state change (`OPEN` toggled by `UpgradedChestBlockEntity.setOpenState` → `level.setBlock(..., UPDATE_CLIENTS)`) keeps the block entity (`BlockEntityType.isValid` is true for the same block), so no add/remove happens. Nothing in the mod calls `queueUpdate`/`revisualize` for the chest (grep: only lines, posts, batteries and lights do). With `partsOnly = true` the BER never runs either. Result: when the chest opens, the block model hides its static lid and nobody draws the animated one.

## How to see it happen

Flywheel backend on (default). Place an Upgraded Chest, leave/re-enter the chunk (or just load a world with one). Open it: the lid disappears for as long as it is open; close it: the static lid is back. Without Flywheel (`/flywheel backend off`) the lid animates. A chest whose visual was created while open keeps working, which hides the bug in quick tests.

## How to fix it

Simplest and robust: register it as an always-on parts visual, `visual(ModBlockEntities.UPGRADED_CHEST, UpgradedChestRenderer::parts, true)` (drop the `visualWhen`). `UpgradedChestRenderer.parts` already returns early while closed, so a closed chest's `PartsVisual.beginFrame` costs one `active` test, a distance compare and a frustum test and hides nothing (no instances exist until first opened). If the idle visual is wanted, keep `visualWhen` but make the client re-create the visual when `OPEN` changes: in `UpgradedChestBlockEntity` override `setBlockState(BlockState)` (vanilla calls it from `LevelChunk.setBlockState` when the block stays the same), compare `OPEN` before/after and, on the client, call `ComedyMonkeysCreateAddonsClient.revisualize(this)` through a static client hook like `LightBlockEntity.clientCue` (the same pattern Lights use for `CUE_LIT`). Also fix the comment in `UpgradedChestRenderer` and the `visualWhen` javadoc ("The client makes it again when that changes" is only true where the mod does it itself).

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** The chest's visual is remade on the client whenever it opens or shuts (no per-frame cost for closed chests).

Tests: UpgradedChestBlockEntityTest: lidHookOnTheClientOnly, lidHookNotForOtherChanges.
