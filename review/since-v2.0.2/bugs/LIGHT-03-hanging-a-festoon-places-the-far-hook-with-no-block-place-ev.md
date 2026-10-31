# LIGHT-03: Hanging a festoon places the far hook with no block-place event (protection bypass)

| | |
|---|---|
| **Severity** | low |
| **Kind** | Exploit or balance hole |
| **Area** | Electric Lights |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/light/Festoons.java:606-612` (`putUp`), `:593-594` (called from `hang`), `FestoonItem.java:444`; compare `EdgeLightItem.java:614-616`

## What is wrong

`Festoons.hang` puts up both hooks with a bare `level.setBlock(...)`. The second end is protected by the click itself (claim mods cancel `RightClickBlock`), but the first end `a` was chosen up to `festoonReach` (16) blocks earlier and is placed without firing `EventHooks.onBlockPlace`, so claim/protection mods never see it. The spec (P31) says claim and protection mods apply through the normal block events, and `EdgeLightItem` already fires the event for exactly this reason.

## How to see it happen

On a claimed server, stand outside a claim and right-click the outside face of an unclaimed block whose air cell in front lies just inside the claim border (the hook cell is `clicked.relative(face)`); walk up to 16 blocks and click your own wall: a Festoon Hook block appears inside the claim, with a string and light spots over it. Shears on the hook (inside the claim) are the owner's only way to remove it, and `cut` refunds the items to the griefer's hook position.

## How to fix it

In `Festoons.hang` pass the player (nullable) through from `FestoonItem.useOn`, and in `putUp` do what `EdgeLightItem` does before `setBlock`: `if (player != null && EventHooks.onBlockPlace(player, BlockSnapshot.create(level.dimension(), level, cell), facing)) return false;` and make `hang` return `Result.NO_HOOK` when either `putUp` is refused. Do this for both ends (the clicked end costs nothing extra).

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Every festoon hook, new or already there, goes through the block-place event (except the hook the player clicked); a refusal hangs nothing and costs nothing.

Tests: LightGameTests: aClaimCanRefuseEitherFestoonHook.
