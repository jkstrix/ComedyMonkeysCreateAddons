# JUG-04: Create contraptions shove him (D20, D56)

| | |
|---|---|
| **Severity** | low |
| **Kind** | Something does not work |
| **Area** | Clockwork Juggernaut |
| **Confidence** | medium |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/juggernaut/ClockworkJuggernaut.java:1711-1719` (`isPushable`, `getPistonPushReaction`); Create 6.0.10 `ContraptionCollider.collideEntities` (sources jar, lines 100-315) and `AbstractContraptionEntity.canCollideWith` (line 834)

## What is wrong

The spec (D20) says he can't be pushed by pistons or contraptions, and D56 relies on it so the Mainspring can't be lost to the void. `isPushable() == false` and `PushReaction.IGNORE` cover vanilla entity pushing and pistons, but Create's `ContraptionCollider` collides with every entity for which `canCollideWith` is true (it only excludes spectators, `noPhysics`, hanging entities and minecarts) and moves them with `entity.setPos(...)`, ignoring `isPushable` and knockback resistance. A moving contraption therefore shoves him like any mob; his per-tick `stopWalking()` zeroes velocity but not a position set directly.

## How to see it happen

On an End island (D55 ruin), build a piston- or gantry-driven wall contraption and run it into him while he rewinds: he is carried along and off the edge; the void kills him (`BYPASSES_INVULNERABILITY`, no player) and no Mainspring drops. On land it is a way to pin or displace him through walls, since he does not resist the push.

## How to fix it

Create consults only `AbstractContraptionEntity.canCollideWith(e)` (spectator, `noPhysics`, hanging, minecart), none of which the Juggernaut can set (he needs block collision), and `Entity.isPushable` is never asked, so the mod has to undo the shove itself. In `ClockworkJuggernaut`, add a field `Vec3 anchoredAt`. At the end of `customServerAiStep`, when he is meant to be standing still (`move == null || move != Move.CHARGE`, `zza == 0`, `!leaping`), store `anchoredAt = position()`; otherwise set it to null. At the start of the next `customServerAiStep`, if `anchoredAt != null` and `position()` differs from it horizontally by more than `1e-3` and he is not being teleported by the mod (`!leaping`), call `setPos(anchoredAt.x, getY(), anchoredAt.z)` (keep Y so gravity still works) and `hurtMarked = true`. That cancels any external `setPos` (Create's collider, other mods' pushes) without touching Create. Add a game test: a `ControlledContraptionEntity`-free stand-in is hard, so instead call `j.setPos(j.getX() + 1, j.getY(), j.getZ())` from a tick hook and assert he is back the next tick. If the user prefers to accept the deviation, record it against D20/D56 in the spec.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** While standing still he snaps back from outside shoves, by position and by velocity (maintainer's choice).

Tests: JuggernautGameTests: aShoveDoesntMoveHim, aThrowDoesntMoveHim, aChargeStillMovesHim.
