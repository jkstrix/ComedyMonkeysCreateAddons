# CAGE-01: Shift-click dupes the jar in a locked-down Netherite cage

| | |
|---|---|
| **Severity** | high |
| **Kind** | Exploit or balance hole |
| **Area** | Monster Cage & Fish Tank |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/cage/MonsterCageMenu.java:527-537` (quickMoveStack), `src/main/java/com/comedymonkey/createaddons/content/cage/MonsterCageBlockEntity.java:1698-1722` (CaptiveHandler.getStackInSlot / setStackInSlot), `:1713-1714` (the silent `if (isLockedDown()) return;`), `src/main/java/com/comedymonkey/createaddons/content/cage/MonsterCageBlock.java:327-331` (onWrenched opens the menu while locked down)

## What is wrong

`CaptiveHandler.getStackInSlot` hands the menu a *copy* of the jar, and `quickMoveStack` moves that copy into the player's inventory with `moveItemStackTo` before telling the handler to clear the slot. During lock-down (`isLockedDown()`: Netherite cage, Wither inside, field down) `setStackInSlot(0, EMPTY)` silently returns without releasing, so the cage keeps its jar and horde while the player walks away with a second filled Netherite Jar holding a Wither. The normal click path is safe (`extractItem` refuses, and `SlotItemHandler.getMaxStackSize` → `setStackInSlot` swap is also refused), only the QUICK_MOVE path leaks. The owner can open the menu during lock-down because `onWrenched` only checks `mayManage`, not `isLockedDown`.

## How to see it happen

Netherite cage with 1+ Withers, cut its FE so the field drops (lock-down). Owner wrenches it (menu opens), shift-clicks the jar slot. Server: `slot.getItem()` = copy → moved into player inventory → `slot.setByPlayer(EMPTY)` → `setStackInSlot` hits `isLockedDown()` and returns. Result: player has a filled Netherite Jar (Wither), cage still holds the jar and its count. Put the copy into a second Netherite cage → two Wither farms from one Wither; repeat every lock-down. Also works with any cage in lock-down state for the cheaper horde-preserving "take the jar without culling".

## How to fix it

Make the jar slot refuse pick-up while locked down and make quickMove go through the handler's own extract:
  1. In `MonsterCageMenu` constructor, give the jar slot an override: `public boolean mayPickup(Player p) { return !be.isLockedDown(); }` and in `quickMoveStack`, before `if (index < firstPlayer)`, add `if (!slot.mayPickup(player)) return ItemStack.EMPTY;`.
  2. Safer still, for `index == JAR_SLOT` do the move through the handler so the handler decides: `ItemStack taken = be.getCaptiveSlot().extractItem(0, 1, false); if (taken.isEmpty()) return ItemStack.EMPTY; if (!moveItemStackTo(taken, firstPlayer, slots.size(), true)) { be.getCaptiveSlot().insertItem(0, taken, false); return ItemStack.EMPTY; } return original;` (extractItem already culls the horde via release()).
  3. Optionally in `MonsterCageBlock.onWrenched` show `tr("locked_down")` and don't open the menu while `be.isLockedDown()` (matches D41 "nor have its jar taken out").
  Add a game test: locked-down cage, `menu.quickMoveStack(player, 0)`, assert the player's inventory has no jar and `cage.hasMonster()`.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Shift-click on the jar goes through the cage's own extract, which refuses during lock-down. The wrench screen still opens in lock-down (maintainer's choice).

Tests: TankGameTests: lockedDownCageKeepsItsJarOnShiftClick.
