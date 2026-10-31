# JUG-06: Cog and melee hit bystanders that never attacked him

| | |
|---|---|
| **Severity** | low |
| **Kind** | Exploit or balance hole |
| **Area** | Clockwork Juggernaut |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/juggernaut/ThrownCog.java:76-85` (`onHitEntity`), `:91-93` (`canHitEntity`: any `LivingEntity`), `ClockworkJuggernaut.java:1453-1456` (`victims`: any `OwnableEntity` with a player owner)

## What is wrong

D27 says he fights players and anything that hits him, and ignores villagers and golems. The thrown cog damages (19) and knocks back any living entity in its path except himself: villagers, golems, livestock, and any player's horses/wolves. Melee moves (`victims`) also include every tamed animal of every player in range (`AbstractHorse`, `Wolf`, `Cat`, `Parrot` are `OwnableEntity`), not just pets that attacked. Both let a player kite him into a village or past another player's stables to kill their animals (`stopRiding` also throws riders off), with no cost to the kiter.

## How to see it happen

Lure him toward a village, stand on a 3-high pillar behind the villagers: after 5 s stuck he throws cogs through the crowd (19 damage each, one every few seconds) until the villagers are dead. Or stand next to another player's tamed horses: a stomp hits every horse.

## How to fix it

In `ThrownCog.canHitEntity`, apply the same filter as `victims`: `target instanceof Player p ? ClockworkJuggernaut.fightable(p) : target == owner.getTarget() || (target instanceof OwnableEntity pet && pet.getOwner() == owner.getTarget())` (get the thrower via `getOwner()`); the cog then passes through bystanders. In `victims`, narrow pets to ones that have hurt him: keep a small `Set<UUID> attackers` that `hurt()` adds to (`source.getEntity()` when it is an `OwnableEntity`), and accept a pet only if it is in that set or is the current target.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Cogs and blows only hit survival players, his target, and whatever has hit him this fight.

Tests: JuggernautGameTests: heSparesBystanders, hisLandingSparesAVillager, hisCogPassesThroughBystanders.
