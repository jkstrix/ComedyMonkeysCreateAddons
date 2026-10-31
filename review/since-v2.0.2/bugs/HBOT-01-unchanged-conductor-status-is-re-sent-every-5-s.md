# HBOT-01: Unchanged conductor status is re-sent every 5 s

| | |
|---|---|
| **Severity** | low |
| **Kind** | Performance |
| **Area** | Helper Bot |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/helperbot/HelperBotMovement.java:40` (`RESEND = 100`), `:67-71` (`sendStatus`)

## What is wrong

`sendStatus` sends a `HelperBotStatusPayload` to every player tracking the carriage whenever the flags change, *and* again every 100 ticks when nothing changed. The stated reason ("players who arrive later catch up", payload javadoc) is already covered: the flags are written to `context.data` under `STATUS` (line 70), `MovementContext.writeToNBT` puts `data` into the contraption's spawn NBT, and `HelperBotClient.state(MovementContext)` (`HelperBotClient.java:164-168`) reads them from there when a client first sees the carriage. The spec (Technical Notes) says the byte is sent "only when it changes". So the resend is redundant traffic: one packet per driving bot per tracking player every 5 s, for the life of every scheduled train.

## How to see it happen

a server with 20 scheduled trains, each with a Helper Bot driving, and a player near a busy station: 20 small packets every 5 s that carry nothing new.

## How to fix it

in `HelperBotMovement.sendStatus`, delete the `RESEND` branch: replace `if (!changed && time % RESEND != 0) return;` with `if (!changed) return;` and remove the `RESEND` constant. The spawn-data path already handles late joiners and re-tracked players (a player who leaves and re-enters tracking range gets a fresh spawn packet with the current `context.data`). If you want belt-and-braces, keep a resend but make it much rarer (e.g. 1200 ticks).

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Status is sent only when it changes; late joiners get it from the carriage's spawn data.

Tests: none (needs a tracking client).
