# CAGE-05: Blue-axolotl tank: scooped colour disagrees with the window and the blue count

| | |
|---|---|
| **Severity** | low |
| **Kind** | Something does not work |
| **Area** | Monster Cage & Fish Tank |
| **Confidence** | high |

## Where to look

`src/main/java/com/comedymonkey/createaddons/content/tank/FishTankBlockEntity.java:459-482` (scoop), `:335-349` (setCaptive blue bookkeeping), `:684-704` (clientTick ownVariant)

## What is wrong

`scoop()` builds the bucketed axolotl from the captive's NBT and only *sets* BLUE when `blue > 0`; it never sets a non-blue colour. With a blue captive, after the one counted blue is scooped (`blue` → 0), every further scoop still has `Variant = BLUE` from the NBT, so the tank hands out unlimited blue axolotls while `blue == 0`. Meanwhile the client shows the school as `ownVariant` = LUCY (`clientTick` maps a blue captive to LUCY), the cull "blue ones go last" ordering and the blue advancement never fire for them, and the goggles say nothing about blue. The server and the window disagree about what the school is.

## How to see it happen

Catch a blue axolotl, put it in a tank, breed to 10 adults. Goggles: "Blue: 1". Scoop 5 times with water buckets: all five buckets are blue axolotls; the window shows lucy axolotls.

## How to fix it

Pick the rule with the user. (a) Vanilla two-blue-parents behaviour: a blue captive's young are blue → in `setCaptive` note `blueCaptive = isBlue == 1`, in `work()` count `blueBorn = born` when `blueCaptive`, and in `clientTick` drop the LUCY remap. (b) Spec F13 as written ("young copy their parent's colour; 1 in 1,200 are blue"): in `scoop()` when `!isBlue && made instanceof Axolotl a && a.getVariant() == BLUE` call `a.setVariant(Axolotl.Variant.LUCY)` (same remap as the client). Either way one test: blue captive, scoop twice, assert the second bucket's variant.

## Before you call it done

- Build with the lint flags on (`./gradlew compileJava` with `-Xlint:all`, see WARN-09) and make sure you added no new warnings.
- Run the unit tests (`./gradlew test`) and the game tests for the area (`./gradlew runGameTestServer`), and add the test named above where one is suggested. The maintainer prefers a test over a manual in-game checklist.
- If the fix touches a renderer or visual, check it once in the dev client with Flywheel on and once with `/flywheel backend off`.

## Status (2026-10-31)

**fixed.** Maintainer's rule: a non-blue captive's tank can roll one blue at a time (shown in the window, scooped first once grown); a blue captive breeds all blue; old saves with a blue captive load all blue.

Tests: TankGameTests: blueCaptiveBreedsBlue, oneBlueAtATime.
