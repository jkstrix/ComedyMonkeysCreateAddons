# JUG-03: Mainspring lost if the world saves during his death animation

| | |
|---|---|
| **Severity** | medium |
| **Kind** | Something does not work |
| **Area** | Clockwork Juggernaut |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/juggernaut/ClockworkJuggernaut.java:1328-1335` (`tickDying`), `:1660-1666` (`beginDying`), `:1888` (`readAdditionalSaveData` -> `beginDying()`), loot table `src/main/resources/data/comedymonkeysaddons/loot_table/entities/clockwork_juggernaut.json` (every pool is `killed_by_player`)

## What is wrong

The killing blow puts him in `DYING` with 1 health; 3.9 s (78 ticks) later `tickDying` calls `die(source)`. Vanilla's `dropAllDeathLoot` only sets `LAST_DAMAGE_PLAYER` (what `killed_by_player` checks) while `lastHurtByPlayerTime > 0`; that counter starts at 100 on the hit, counts down every tick and is not written to NBT. If the entity is saved during those 78 ticks (player quits a single-player world right after the kill, the server stops or the chunk unloads), on load `readAdditionalSaveData` re-enters `beginDying()` with `lastHurtByPlayerTime == 0`, the clip replays and `die()` drops nothing: no Mainspring, no brass, no XP, after a full fight. Separately, `getLastDamageSource()` returns null once 40 ticks have passed since the last hit, so `tickDying` always dies with `damageSources().generic()`: `LivingDeathEvent` and the player's `awardKillScore`/`PLAYER_KILLED_ENTITY` trigger see a generic source with no attacker (any mod or advancement keyed on the killing blow misses it).

## How to see it happen

Kill him in single-player; the moment the death clip starts, press Esc -> Save and Quit (the world saves him as `Phase=DYING`). Rejoin: he finishes dying and no items drop. Same for a server `/stop` or a chunk unload during the 3.9 s.

## How to fix it

In `beginDying()` remember the kill: `killingBlow = source` (pass the `DamageSource` from `hurt`) and `playerKill = lastHurtByPlayerTime > 0`; save `playerKill` in `addAdditionalSaveData` ("PlayerKill") and read it back. In `tickDying()`, before `die(...)`: `if (playerKill && lastHurtByPlayerTime <= 0) lastHurtByPlayerTime = 100;` (protected field, visible to the subclass; `lastHurtByPlayer` may be null after a reload, which is fine, `killed_by_player` passes once `LAST_DAMAGE_PLAYER` is set; if you also want the player credited for XP/stat, save the killer's UUID and look them up with `level().getPlayerByUUID`). Use `killingBlow != null ? killingBlow : damageSources().generic()` instead of `getLastDamageSource()`.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** The player kill and the killing blow survive a save during the death clip; the kill pays out exactly once even across a save between death and removal. If the killer is offline the loot still drops with no credit (maintainer's choice).

Tests: JuggernautGameTests: heDiesOfTheKillingBlow, aReloadInHisDeathStillDropsTheMainspring, aSaveAfterHisDeathPaysOutOnce.
