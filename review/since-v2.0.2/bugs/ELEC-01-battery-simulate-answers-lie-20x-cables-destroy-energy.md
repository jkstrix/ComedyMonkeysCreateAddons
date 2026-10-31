# ELEC-01: Battery simulate answers lie 20x, cables destroy energy

| | |
|---|---|
| **Severity** | high |
| **Kind** | Something does not work |
| **Area** | Battery, Dynamo & generators |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/electric/BatteryStore.java:162` and `:178` (`limit = simulate ? perTick * SECOND : perTick - inNow`); the same pattern in the paper view at `src/main/java/com/comedymonkey/createaddons/content/line/PowerGrid.java:1028` and `:1041`; the battery capability is handed to every mod at `src/main/java/com/comedymonkey/createaddons/ComedyMonkeysCreateAddons.java:239` via `BatteryBlockEntity.energy(side)` (`BatteryBlockEntity.java:150-155`).

## What is wrong

`BatteryStore.receiveEnergy(max, true)` and `extractEnergy(max, true)` answer with a whole second's worth (`perTick * 20`, 40,960 FE for one Battery) while the real call in the same tick only moves `perTick - inNow` (2,048). This is deliberate so the mod's own Power Lines can plan a second at once (they hand the difference back through `RefundingStorage.refund`), but the capability is the plain `IEnergyStorage` that every other mod sees, and the FE contract every cable mod relies on is "a simulate followed by a real call of the same or smaller amount in the same tick succeeds for that amount". Pipez (a declared runtime dependency, and named as the battery's cable in `LavaGeneratorBlockEntity`'s javadoc) does `source.extract(n, true)` -> `dest.receive(sim, true)` -> `source.extract(received, false)` -> `dest.receive(extracted, false)` and returns the *extracted* amount, discarding what the destination really took (verified in the bytecode of `de/maxhenkel/pipez/corelib/energy/EnergyUtils.pushEnergy`, pipez-neoforge-1.21.1-1.2.31). So a cable pushing into a Battery pulls up to 20x the per-tick limit out of the source for real and the Battery keeps 2,048 of it: the rest is destroyed every tick. A cable *pulling* from a Battery is safe only because Pipez happens to do the real extract before the real insert; a cable mod that inserts first would duplicate energy instead.

## How to see it happen

Geothermal Turbine (46,080 FE/t, 4M FE store) -> Pipez energy pipe with an upgrade whose rate exceeds 2,048 FE/t -> one plain Battery. Each tick: `battery.receiveEnergy(rate, true)` returns min(rate, room, 40,960); Pipez extracts that much from the turbine for real; `battery.receiveEnergy(x, false)` accepts 2,048; up to 38,912 FE/t vanish. The turbine's store drains while the battery fills at 2,048/t; nothing is logged, the goggles just show a slow fill.

## How to fix it

Make simulate honest and give Power Lines its own planning call.
  1. In `BatteryStore.receiveEnergy` / `extractEnergy` use `long limit = perTick - inNow` (resp. `outNow`) for simulate as well; add `int planReceive(int max)` / `int planExtract(int max)` that return today's `perTick * SECOND`-based answers.
  2. Extend `PowerGrid.Refund` (or add a sibling interface `PowerGrid.Planned`) with `planReceive(int)` / `planExtract(int)`; implement it on `BatteryBlockEntity.view` (delegating to the live store) and on `PowerGrid.paperStore`'s anonymous `RefundingStorage` (same change at `PowerGrid.java:1028/1041`).
  3. In `PowerLines` where it plans a second (`PowerLines.java:1081`, `:1092`, `:1123`, `:1164`) call `e instanceof PowerGrid.Planned p ? p.planExtract(rate) : e.extractEnergy(rate, true)` (and the same for receive). The per-tick execution at `PowerLines.java:1299` already asks one tick's worth and can keep using simulate.
  4. Game test: a Pipez-style pusher (simulate receive -> real extract from a source -> real receive) against a Battery must not lose energy: assert source loss == battery gain every tick.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Battery and paper stores answer simulate for this tick only; Power Lines plan a second through their own planReceive/planExtract.

Tests: BatteryGameTests: cablesNeverLoseEnergy.
