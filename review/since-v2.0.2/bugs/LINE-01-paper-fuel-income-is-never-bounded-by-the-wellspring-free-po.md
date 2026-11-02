# LINE-01: Paper fuel income is never bounded by the Wellspring: free power from stock

| | |
|---|---|
| **Severity** | high |
| **Kind** | Exploit or balance hole |
| **Area** | Fluid & Power Lines |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/line/PowerGrid.java:192-225` (`Feeds.update`, `Feeds.into`), `:289-295` (`Paper.income`), `:316-343` (`Paper.advance`); `src/main/java/com/comedymonkey/createaddons/content/line/FluidLines.java:472-479` (`g.fed(...)`); `src/main/java/com/comedymonkey/createaddons/content/electric/LavaGeneratorBlockEntity.java:174-181` (`record`) and `GeothermalControllerBlockEntity.java:142-150`

## What is wrong

A generator's record stores, per Wellspring, the mB/t the Fluid Line *delivered* to it over the last seconds (`Feeds.update` is a 50/50 running average of `fed()` amounts). While the generator and the Wellspring are both away, `Paper.advance` credits that rate every tick for as long as they stay away. Nothing caps the rate at the Wellspring's real production (`WellspringBlockEntity.getGenerationRate()`), and the Wellspring's tank is never debited. Delivery is bounded by the *generator's burn* (its tank is full, so the line only tops up what it burns), not by what the Wellspring makes, so any Wellspring that is feeding a generator out of stored fluid records an income equal to the burn rate. On paper that generator then runs flat out for ever, and when the chunk reloads the Wellspring still has all its stock. This was documented as Cursor-findings.md #1 / power-lines-review-fixes.md item 13b; HEAD only fixes the "Wellspring running" half (`Paper.income` skips running sources). The "Wellspring away too" half is still open, and the plan's own argument for it ("comes to the same as both running") only holds when delivery equals production.

## How to see it happen

Place a Wellspring with no generation upgrade (it accepts lava or water, so it is just a tank) and fill it with 8 lava buckets (8,000 mB). Run a copper Fluid Line with an Intake from it into a Lava Generator, and a Power Line from the generator to a far-away base. Wait ~10 s: the generator's tank fills, then the line delivers 1 mB/t (the burn), and `Feeds.update` settles the source rate at ~1 mB/t. `record(p, true)` writes `income = {wellspring: 1.0}`. Walk away until both chunks stop ticking. `tickAway` now runs `Paper.advance` every second with `in[0] = 1.0`, `use = 1.0` → `run = 1`, store fills at 1,024 FE/t indefinitely and the base draws it. Loaded, 8,000 mB would have lasted 8,000 ticks (~6.7 min). Come back a day later: the generator's tank is still full and the Wellspring still has ~8,000 mB. Repeat with a 256,000 mB capacity-upgraded Wellspring for a bigger "legit-looking" stock; the stock is never consumed.

## How to fix it

Bound the recorded income by what the Wellspring actually makes, so paper never beats "both loaded". In `PowerGrid.Feeds.into(Fuel f, int fuel, Level level)`: for each source `src` that is loaded, look up `level.getBlockEntity(src)`; if it is a `WellspringBlockEntity w`, clamp the stored rate to `w.getGenerationRate() / 20.0` (and to 0 when its `getGenerationSource().fluid()` is not this fuel's fluid). Do the same clamp in `PowerGrid.tickAway` where it already visits `f.income` for loaded sources (the loop at `:703-706`), so a Wellspring whose upgrades were removed stops counting. A stricter alternative that also handles stock honestly: keep a small per-Wellspring paper record (amount, cap, rate) in `Records`, note it from `WellspringBlockEntity.tick`, and in `Paper.advance` draw income from that record (debiting it) up to its rate, exactly as generators' stores are drawn. The clamp is the small, safe change; the Wellspring record is what Cursor #1 asked for. Add a game test: Wellspring with no upgrades, filled by hand, generator marked away via `PowerGridTesting.away`; after N seconds `PowerGridTesting.store(...)` must not exceed what the real stock could have produced.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Each Wellspring that feeds a far generator keeps its own paper record (stock, rate, fluid). Generators on paper burn that stock, shared by income, and the real tank is drained by the same amount when the Wellspring loads again. A follow-up after review also debits the record when a Fluid Line drains the Wellspring while it is loaded but not ticking.

Tests: LineGameTests: paperBurnsOnlyTheWellspringsStock, paperIncomeNeverBeatsTheWellspringsRate, paperAndALiveLineNeverSpendTheStockTwice.
