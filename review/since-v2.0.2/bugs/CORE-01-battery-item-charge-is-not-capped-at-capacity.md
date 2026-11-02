# CORE-01: Battery item charge is not capped at capacity

| | |
|---|---|
| **Severity** | low |
| **Kind** | Exploit or balance hole |
| **Area** | Registry, payloads & data |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/electric/BatteryBlockEntity.java:845` (applyImplicitComponents), `:794` (read), `src/main/java/com/comedymonkey/createaddons/content/electric/BatteryStore.java:47` (energy += members.get(i).charge)

## What is wrong

`applyImplicitComponents` takes the `BATTERY_CHARGE` component with only `Math.max(0, …)`; the component codec is a plain `Codec.INT`/`VAR_INT` (ModDataComponents.java:40) with no range, and `read` (line 794) takes the saved `Charge` tag with no clamp at all. The Spring Engine does this right (`SpringEngineBlockEntity.java:337` clamps to `capacity()`), the Battery does not. A placed Battery can therefore hold far more than `batteryCapacity` (default 10,000,000; max 25,000,000), and a tower built from such blocks sums them into its `long energy`, all extractable through the normal `extractEnergy` path (BatteryStore.java:179 only checks `energy`, not capacity).

## How to see it happen

In creative (or with a modified client, which can send any stack through the creative-slot packet): `/give @s comedymonkeysaddons:battery[comedymonkeysaddons:battery_charge=2000000000]`, place it → the Battery shows full and gives 2 GFE to a Power Line; nine of them formed into an Industrial Battery hold 18 GFE, more than the tower's whole capacity, and keep giving it out. Only creative/modified clients can make the stack, so this is low: it does not help a survival player, but on a server where creative is handed out it is "free FE far beyond what the block is allowed to hold", and a negative `Charge` from an edited save is only neutralised because `BatteryStore.add` floors at 0.

## How to fix it

In `BatteryBlockEntity.applyImplicitComponents`, clamp to the block's own capacity the same way the Spring Engine does: `charge = Mth.clamp(input.getOrDefault(ModDataComponents.BATTERY_CHARGE.get(), 0), 0, CMConfig.SERVER.batteryCapacity());` (a plain Battery never legitimately holds more than one Battery's share: `collectImplicitComponents` writes `share()`, which for a formed tower is `energy / n` ≤ `industrialBatteryCapacity`, so clamp to `Math.max(batteryCapacity(), industrialBatteryCapacity())` if a tower block's share must survive a pick-up). In `read` (line 794) use `Math.max(0, tag.getInt("Charge"))`. Optionally register the component with `Codec.intRange(0, 25_000_000)` in `ModDataComponents.java:40` so an out-of-range value fails to parse instead of being accepted.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** A placed Battery's charge is clamped to the larger of the two configured capacities; the component codecs clamp to 0..25,000,000.

Tests: BatteryGameTests: placedChargeIsCapped; ModDataComponentsTest.
