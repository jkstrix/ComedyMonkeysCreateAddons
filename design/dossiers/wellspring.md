# Code Dossier: Fluid Wellspring

What the code does today. Paths: `J/` = `src/main/java/com/comedymonkey/createaddons/`, `A/` = `src/main/resources/assets/comedymonkeysaddons/`, `D/` = `src/main/resources/data/comedymonkeysaddons/`. Model elements are cited by index (`#n`) and the line their `"from"` is on. Pixel coordinates are in 1/16 block, model space (y up); texture rows are counted from the top of the PNG.

## Identity

- Registry id `wellspring`, shown as **"Fluid Wellspring"** (`A/lang/en_us.json:3`). Single block, 1x1x1, no facing, no kinetics.
- Block `J/content/wellspring/WellspringBlock.java` (`BaseEntityBlock`, `IWrenchable`), registered `J/registry/ModBlocks.java:37-47`.
- Block entity `J/content/wellspring/WellspringBlockEntity.java` (plain `BlockEntity` + `IHaveGoggleInformation`, `MenuProvider`, `Clearable`; `:40`), type `J/registry/ModBlockEntities.java:34-36`.
- Renderer `J/client/WellspringRenderer.java` (a `BlockEntityRenderer`, 38 lines), registered `J/client/ComedyMonkeysCreateAddonsClient.java:89`. No Flywheel visual, no partial models.
- Menu `J/content/wellspring/WellspringMenu.java` (`J/registry/ModMenus.java:21-22`), screen `J/client/WellspringScreen.java` (`ComedyMonkeysCreateAddonsClient.java:80`).
- Upgrades: `WellspringUpgrade` (enum of 5), `WellspringUpgradeItem`, `WellspringUpgradeInventory` (6 slots); fluid kinds `WellspringSource` (enum of 5).
- Display Link sources `WellspringDisplaySources` (`J/registry/ModDisplaySources.java:21-39`); contraption storage `WellspringMountedStorage(Type)` + `WellspringMovementBehaviour` (`J/registry/ModMountedStorage.java:29-30, 44-46`).
- Item: plain `BlockItem` (`J/registry/ModItems.java:25`) whose model is `wellspring_0` (`A/models/item/wellspring.json`). Creative tab `J/registry/ModCreativeTabs.java:23-30` (with the templates and five upgrades).
- Family (survey): "The Wellspring feeds the turbine" (`design/dossiers/survey.md:37`); it is also the lava supply in the Lava Generator, Magma Engine and Geothermal Turbine ponders (`J/client/ponder/NewBlockScenes.java:111, 142`; `J/client/ponder/ElectricScenes.java:222, 273`).

## What It Does

1. **Place it.** A full-cube copper block: strength 3/6, copper sound, pickaxe, needs the right tool, orange map colour, no occlusion, not view-blocking or suffocating (`ModBlocks.java:38-47`; `src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json:3`). It has no facing: all four sides are the same.
2. **Fill it by hand or by pipe.** Buckets and other fluid containers fill or drain it on any clicked face (`WellspringBlock.java:98-103`). The fluid capability is exposed on **every side, including top and bottom** (the lambda ignores `side`, `J/ComedyMonkeysCreateAddons.java:127-128`), so Create pipes and pumps work from all six faces. One tank.
3. **What it accepts.** With no generation upgrade: water, lava, Create honey or Create chocolate (`WellspringBlockEntity.java:277-282`, `WellspringSource.java:26-34, 44-49`). With generation upgrades fitted: only that upgrade's fluid (`:279-280`). (The comment at `:276` still says "Water or lava only".)
4. **Fit upgrades.** Right-click with an upgrade installs one into the first free valid slot, consumes it and plays vanilla `SMITHING_TABLE_USE` at 0.6 vol / 1.2 pitch (`WellspringBlock.java:87-95`; `WellspringBlockEntity.java:173-209`). Refusals go to the action bar in gold: wrong kind, other fluid inside, slots full (`:176-201`; `en_us.json:23-26, 44`). Sneak + right-click (empty hand or with an upgrade) or right-click with a wrench (`c:tools/wrench` or a Create `WrenchItem`, `J/content/Wrenches.java:17-19`) opens the upgrade GUI (`WellspringBlock.java:81-92, 109-121`); sneak-wrenching still dismantles as in Create (`:81`).
5. **Slots** (`WellspringUpgradeInventory.java:14-16, 37-62`): 3 **generation** slots then 3 **capacity** slots, one item each. Generation slots take one kind only (water, lava, honey or chocolate, never mixed) and only one matching the fluid already inside (`:52-61`). Capacity slots take only Capacity Upgrades (`:47-48`). Slots beyond the server config limit are locked (padlock in the GUI) but can still be emptied (`:30-34`; `WellspringMenu.java:94-101`). A Capacity Upgrade can't be taken out while the fluid would not fit without it (`WellspringUpgradeInventory.java:65-70`; `WellspringBlockEntity.java:168-170`).
6. **Generate.** Every server tick, if the rate is above 0 and the tank isn't full, `rate` is added to a progress counter and every 20 units makes 1 mB of the upgrade's fluid (`WellspringBlockEntity.java:74-84`). So the rate is in mB per second. Default rates (`J/config/CMConfig.java:145-170, 503-529`):

   | Upgrades | Water | Lava | Honey | Chocolate |
   |---|---|---|---|---|
   | 1 | 100 mB/s | 10 | 10 | 10 |
   | 2 | 250 | 25 | 25 | 25 |
   | 3 | 500 | 50 | 50 | 50 |

   It stops when full and the counter resets (`:82-84`). There is no input, no power and no on/off: fitting a generation upgrade is the only switch.
7. **Capacity.** 8,000 mB base, +16,000 mB per Capacity Upgrade, up to 3: 8k / 24k / 40k / 56k mB (`CMConfig.java:125-143, 533-536`). Re-checked every tick; shrinking never leaves it overfull (`WellspringBlockEntity.java:66-72`).
8. **Take fluid out** by pump, pipe or bucket from any side (step 2). **Comparator**: 0 when empty, else 1 + 14 × fill fraction (`WellspringBlock.java:133-146`).
9. **Break it**: the upgrades drop; the fluid is lost (`WellspringBlock.java:123-131`; `WellspringBlockEntity.java:211-222`). Moving it (Create contraptions, Sable/Aeronautics, Carry On) keeps the upgrades and fluid (`WellspringBlock.java:125-128`; `WellspringBlockEntity.java:240-250`; `README.md:292-295`).
10. **On a contraption**: blocks don't tick, so the tank moves into `WellspringMountedStorage`, which keeps generating at the same rate (`WellspringMovementBehaviour.java:9-12, 15-18`; `WellspringMountedStorage.java:70-83`), is usable by contraption pumps, spouts and portable fluid interfaces, syncs the tank to the client block entity so the level rises (`:96-112`) and writes it back on placement (`:85-92`).
11. **Display Links** read: stored fluid ("Water: 12,000 / 32,000 mB" / "Empty"), fill percent, fill bar, and generation ("N mB/s", "Full", "No upgrades") (`WellspringDisplaySources.java:22-87`; `en_us.json:658-664`).
12. **In practice**: a 3-upgrade lava Wellspring makes 50 mB/s, so two feed the Geothermal Turbine's 90 mB/s of lava (`en_us.json:778`; `ElectricScenes.java:273`); one runs a Magma Engine (10 mB/s, `en_us.json:356`).

## What the Player Is Told

- Name "Fluid Wellspring" (`en_us.json:3`). Upgrade names: Water Upgrade, **Lava Upgrade** (registry id `dripstone_upgrade`), Honey Upgrade, Chocolate Upgrade, Capacity Upgrade (`:4-5, 39, 607, 614`).
- Block tooltip: "A _sealed reservoir_ for _water_ or _lava_. Fit it with _upgrades_ and it will _generate_ its fluid endlessly." / "Fills itself with water (Water Upgrades) or lava (Lava Upgrades). Up to three of one kind … Up to three Capacity Upgrades enlarge the tank." / "Fluid can be pumped out from any side, or collected with a bucket. Comparators read the fill level." / R-click with an upgrade installs, wrench or sneak opens the GUI (`en_us.json:8-16`). Honey and chocolate are not mentioned on the block.
- Upgrade tooltips call them modules: "A _hydraulic module_" (water), "A _geothermal module_" (lava), "A rare _honey module_", "A rare _chocolate module_", "A _reinforced tank module_" (capacity) (`en_us.json:17, 20, 41, 609, 616`).
- Goggles (`WellspringBlockEntity.java:322-367`; `en_us.json:27-31, 47-50, 71, 612, 619`): "Wellspring Info:"; "Upgrades:" with "N / 3 Water|Lava|Honey|Chocolate" and "N / 3 Capacity" (or "No upgrades installed"); "Generating:" then "N mB/s" (aqua) or "Tank is full" (red); "Fill Level:" fluid name, "NN%" (gold) and "(amount / capacity mB)", or "Empty (capacity mB)".
- GUI (`WellspringScreen.java`; `WellspringMenu.java:20-55`): machine panel 176×84 (`WellspringMenu.java:28`) with "Generation" slots at y 28 and "Capacity" slots at y 60, x 8/26/44 (`:24-26, 48-52`); "Fill" as a large percentage in the fluid's colour and "Output" (rate, "None" or "Tank full") at x 68 (`WellspringScreen.java:35, 115-136`); a fluid gauge 36×58 at (132, 18) tiled with the fluid's still texture under the shared glass sprite (`:30-33, 73-103`). Text colours: water `#5A8FE0`, lava `#E07838`, honey `#D8901C`, chocolate `#A8704A` (`:37-38, 154-164`). Locked slots get a padlock and the tooltip "Slot locked by the server config" (`:58-71`; `en_us.json:60`). No quick-move buttons, by design (`WellspringMenu.java:21-22`).
- Ponder "Generating Water and Lava with the Fluid Wellspring" (`J/client/ponder/WellspringScenes.java:31-150`, structure `A/ponder/wellspring.nbt`, text `en_us.json:83-91`). Only water is shown:
  1. "The Fluid Wellspring is a tank that holds water or lava" (west face, `:45-51`).
  2. Right-click with a Water Upgrade: blockstate set to water / 1 lamp, the tank fills in 6 steps (`:53-68`).
  3. Two more: lamps go to 3. "Each lamp on top is one upgrade, and each one speeds up generation" (`:70-83`).
  4. White outline on the block: "The glass windows show the fill level. The notches on the frame mark 25%, 50% and 75%" (north face, `:85-93`).
  5. Capacity Upgrade: tank capacity +16,000 (the level visibly drops), "have their own slots and make the tank larger" (`:95-108`).
  6. Wrench opens the upgrade screen (`:110-121`). 7. Bucket on the east face: "Pumps and pipes can draw fluid out from any side" (`:123-134`). 8. Goggles (`:136-147`).
- README: "A copper-framed tank with glass windows that show the fluid level (notches mark 25%, 50% and 75%)"; "lamps on top show how many" (`README.md:38-56`).
- Recipes: the block is copper sheets ×4 (corners), Mechanical Pump (top), glass ×2 (sides), Create Fluid Tank (centre), Copper Casing (bottom) (`D/recipe/wellspring.json`). Upgrades start from an Upgrade Template (copper sheet → cogwheel → glass → electron tube, `D/recipe/sequenced_assembly/upgrade_template.json`); +2 pointed dripstone = Drip Template (`drip_template.json`); spout 250 mB water or lava = Water/Lava Upgrade (`D/recipe/filling/water_upgrade.json`, `dripstone_upgrade.json`); honey and chocolate take 3 loops of honeycomb block or cocoa beans, 1000 mB of the fluid, a precision mechanism and a press (`sequenced_assembly/honey_upgrade.json`, `chocolate_upgrade.json`); Capacity = template + 3 Fluid Tanks (`capacity_upgrade.json`).
- Advancements: "Spring Into Action", "Make It Rain", "The Floor Is Lava", "Tank You Very Much", "Maximum Overdrip" (3 generation upgrades) (`en_us.json:227-236`; `D/advancement/maxed_wellspring.json`).
- The promise: a sealed copper-and-glass tank with a pump on top that fills itself from fitted modules, whose windows show how full it is.

## States

| State | Source | How the player can tell today |
|---|---|---|
| `source=none` | Blockstate (`WellspringBlock.java:35, 53`). Set when there are no generation upgrades and the tank is empty or holds nothing it knows (`WellspringBlockEntity.java:297-298`) | Model `wellspring_0`: dark porthole and dark lamps on top (`A/blockstates/wellspring.json:3-14`) |
| `source=water / lava / honey / chocolate` | Blockstate. The generation upgrades' fluid, or else the fluid inside (`WellspringBlockEntity.java:297-298`, written by `updateBlockState` `:292-306` on every tank or slot change `:284-290`) | Only when `upgrades` ≥ 1: the top porthole and lit lamps take the fluid's colour (`wellspring.json:18-62`). With `upgrades=0` every source uses `wellspring_0` (`:15-17, 27-29, 39-44`), so the type shows only through the fluid in the windows |
| `source=lava` light | `lightLevel` 9 for `source=lava`, any upgrade count (`ModBlocks.java:47`) | The block lights its surroundings at 9, even when empty with lava upgrades fitted. No part of the model glows |
| `upgrades=0..3` | Blockstate (`WellspringBlock.java:37`): number of **generation** upgrades, capped at 3 (`WellspringBlockEntity.java:299`) | 1, 2 or 3 lamps lit on the top face (top textures `_1/_2/_3`). Not visible from the side |
| Fluid type and amount | Synced BE: tank, capacity and upgrade slots go in the update tag (`WellspringBlockEntity.java:423-434`), sent at most every 10 ticks (`:41, 86-89, 312-318`) | The fluid column in the four windows; its texture/tint gives the type. Smoothly eased on the client (`:92-110`) |
| Empty | Tank empty | Nothing drawn (`WellspringRenderer.java:27-32`); the windows are fully transparent, so you see straight through the block to the world behind |
| Full | Amount ≥ capacity | Fluid reaches the top of the windows (13 px). Looks the same whether or not it is generating |
| Generating | Derived, not stored: generation upgrades fitted, rate > 0, not full (`WellspringBlockEntity.java:74-84`). The client has all the inputs (upgrades and tank are synced; the GUI already computes the rate client-side, `WellspringScreen.java:127-136`) | Nothing. The level rises, but slowly: 10 px of window holds 8,000-56,000 mB, so lava/honey/chocolate at 50 mB/s rise 1 px every 16 s (8k tank) to 112 s (56k); at 10 mB/s, 1 px every 80-560 s. Only water at 250-500 mB/s visibly climbs. Once full, or while something drains it as fast as it fills, the look is static |
| Capacity upgrades 0..3 | Synced BE (upgrade slots `FIRST_CAPACITY_SLOT`.., `WellspringBlockEntity.java:139-145`; capacity `:425`) | Nothing. The level is a fraction, so a half-full 56k tank looks like a half-full 8k one. The ponder shows the level dropping when one is added (`WellspringScenes.java:100-101`) |
| Generation upgrade kind and count beyond 3 lamps | Synced BE slots | Lamp colour (top only) |
| Generation progress | Server only: saved (`WellspringBlockEntity.java:390`) but not in the update tag (`:423-427`) | Can't be shown without a sync change (and is meaningless to show: sub-mB remainder) |
| Locked slots (server config) | `WellspringUpgradeInventory.isUnlocked` (`:30-34`) | GUI padlock only |
| On a contraption | `WellspringMountedStorage` (client sync of tank and capacity, `:107-111`) | The level still rises. The blockstate (lamps, light) is whatever it was at assembly; the mounted storage never updates it |

## Moving and Changing Parts

| Part | Model file | Driven by | Axis / pivot / range | Source |
|---|---|---|---|---|
| Fluid column | none: drawn by Create/catnip's `renderFluidBox` with the fluid's still texture and tint | Rendered fill fraction: tank amount / capacity, eased 25% per client tick toward the synced value and lerped by partial ticks (`WellspringBlockEntity.java:92-114`) | Axis-aligned box x 2.05→13.95, z 2.05→13.95, y 3 → 3 + 10 × fill px (full = 13). Not drawn below fill 0.01 (~0.1 px). No bottom face; top and four sides drawn | `WellspringRenderer.java:17-19, 26-36` |
| Lamps (0-3) and porthole colour | Texture swap per blockstate: `wellspring_<source>_<n>.json` → `wellspring_top_<source>_<n>.png` | `upgrades` and `source` blockstate | Static. Top face only | `A/blockstates/wellspring.json`; `A/models/block/wellspring_*_?.json` |
| Block light | none | `source=lava` | 9, whole block | `ModBlocks.java:47` |
| GUI gauge (screen only) | `A/textures/gui/wellspring.png` + glass sprite in `A/textures/gui/widgets.png` (u 0, v 16) | `getFillFraction()` (not eased) | 36 × 58, fills from the bottom | `WellspringScreen.java:30-33, 73-103`; `J/client/GuiKit.java:35-39` |

Nothing rotates, nothing animates, no particles and no sounds besides the install clunk and copper block sounds.

## Code Contract

- **Blockstate**: properties `source` (`none`, `water`, `lava`, `honey`, `chocolate`) and `upgrades` (0-3) = 20 variants, all mapped explicitly (`A/blockstates/wellspring.json:2-63`; `WellspringBlock.java:35-37, 52-54`; `WellspringSource.java:12-18, 58-61`). No facing and no rotation: a redesign must look right from all four sides, or add a facing property (a code change). Today every `upgrades=0` variant uses one model.
- **Model files today**: `wellspring_base` (geometry) and 13 texture-only children: `wellspring_0`, `wellspring_{water,lava,honey,chocolate}_{1,2,3}` (`A/models/block/`). Each child sets `side`, `bottom` and `top`; only `top` changes per state (e.g. `wellspring_water_1.json`). The renaming is free as long as the blockstate is updated with it; the renderer names no models.
- **Item model**: parent `comedymonkeysaddons:block/wellspring_0` (`A/models/item/wellspring.json`). The inventory icon is the empty, unlit block (a BER does not draw in the inventory).
- **Textures today** (all 16×16, own files, not shared with any other block): `wellspring_side`, `wellspring_bottom`, `wellspring_top_0`, `wellspring_top_{water,lava,honey,chocolate}_{1,2,3}` (`A/textures/block/`). Particle = `#side` (`wellspring_base.json:5`).
- **Render type** `minecraft:cutout` (`wellspring_base.json:3`): the windows are fully transparent pixels, not tinted glass. A tinted glass pane would need a `translucent` render type and a check that it sorts correctly against the fluid box (not tried).
- **Voxel shape**: none overridden, so the full cube (no `getShape` in `WellspringBlock.java`). `noOcclusion`, not view-blocking (`ModBlocks.java:42-46`). The model should keep filling the full cube or close to it.
- **The fluid box** (`WellspringRenderer.java:17-19, 34-36`): x/z **2.05-13.95 px**, y **3 px to 13 px** at full. Any new model must keep that volume clear and hollow, keep the see-through windows on the faces the player looks at, and line the window bottom up with y 3 and the top with y 13 or change `FLOOR`/`CEILING`/`INSET` with it. The current windows are x/z 3-13 and y 3-13 (side texture transparent at columns 3-12, rows 3-12), so the fluid is 0.95 px wider than the window on each side, hidden behind the frame's inner rim. The frame notches at 25/50/75% (side texture column 13, rows 10, 7 and 5 = y 5-6, 8-9, 10-11 px) are promised by the ponder and README (`en_us.json:87`; `README.md:40`), so they must stay readable against y 5.5 / 8 / 10.5.
- **Inner walls**: element `#1` (`wellspring_base.json:83`) is a 0.02-px-inset box with `from` and `to` swapped (15.98 → 0.02), so its four faces point inward and draw the far walls from inside; `#2` (`:133`) is an inner floor at y 0.02 and `#3` (`:156`) an inner ceiling at y 15.98, both with `#bottom`. The fluid sits inside these (2.05 > 0.02), so no z-fighting. A new hollow shell needs the same: inner faces outside the fluid box.
- **Fluid light**: catnip's `renderFluidBox` raises the block light of the fluid to the fluid's own luminosity (`net/createmod/catnip/render/FluidRenderHelper.java:41-43`, ponder-neoforge 1.0.82 sources), so lava is drawn at light 15 and is the brightest thing on the block; water, honey and chocolate take world light. The block's own light is 9 for `source=lava` (`ModBlocks.java:47`). No emissive faces on the model.
- **Fluid colours** come from the fluids, not from us: vanilla water and lava still textures, Create honey and chocolate (`WellspringSource.java:26-38`). The top textures' lamp and porthole colours are hand-painted per source.
- **Ports**: fluid in and out on all six faces (`ComedyMonkeysCreateAddons.java:127-128`); bucket use on any face (`WellspringBlock.java:99-103`). Create pipes will attach to any face, including top and bottom, and draw their own end rims. No face is special.
- **No Create parts are drawn or borrowed**: no partial models, no Create textures, parent `minecraft:block/block` (`wellspring_base.json:2`). The recipe's Mechanical Pump, Fluid Tank and Copper Casing are not shown.
- **Interaction**: no click zones (the whole block is one target). Wrench and sneak open the GUI; sneak-wrench dismantles.
- **GUI** (separate from the model): uses the shared GUI kit (`GuiKit` title band, label colours, `widgets.png` padlock and glass sprite; `MenuLayout` inventory panel). The glass sprite at `widgets.png` (0, 16) is used only by this screen (`WellspringScreen.java:75`; the padlock and Sort sprites are shared with the chest screens, `ChestSortButton.java:35`, `QuickTransferControls.java:65`).
- **Hands-off**: nothing Wellspring is listed in `design/hands-off.md`. Do not touch the finished blocks it sits beside in ponders: the Geothermal Turbine (`hands-off.md:23-28`), Lava Generator (`:30-34`), Electric Motor (`:36-40`) and Magma Engines (`:46-50`), nor the scenes in `ElectricScenes.java` / `NewBlockScenes.java` that show it. The shared `GuiKit`/`widgets.png` padlock and Sort sprites must keep drawing the same for the chest screens.

## Current Look

Contact sheet: `design/dossiers/wellspring/textures-before.png` (each 16×16 scaled 8× nearest-neighbour; transparent pixels shown as mid grey). Row 1: `side`, `bottom`, `top_0`. Row 2: `top_water_1-3`, `top_lava_1-3`. Row 3: `top_honey_1-3`, `top_chocolate_1-3`.

Model: 4 elements (`wellspring_base.json`): `#0` (`:9`) the full 0-16 cube, all six faces with cullfaces, `side` on the four sides, `top` and `bottom`; `#1` (`:83`) the inward-facing inner walls with `side`; `#2` (`:133`) inner floor and `#3` (`:156`) inner ceiling with `bottom`. So it is a plain cube with a picture of a frame on each face, no relief.

Textures (sampled pixel values):
- **Frame (all six faces)**: a 3-px copper border. Outer bevel `#e3826c` top/left, `#904931`/`#9a5038` bottom/right, inner band `#d67b5b`/`#a75a40`, inner shadow rim `#6d3421`, corner "rivets" `#f7cb6c`. Flat single colour per band, no mottling.
- **Side**: a 10×10 window (transparent) with an opaque white-cyan glass streak (`#c6e2e6`, `#f4fdff`) running diagonally across it; three dark notches on the right-hand inner rim. Identical on all four sides.
- **Top**: dark interior (`#2b2a29`-`#5e5b5a`, `#67625f` ledge at row 12), a copper-ringed 4×4 "porthole" off-centre toward the north (columns 6-9, rows 4-7), dark when idle and filled with the fluid colour when upgraded; a row of three 2×1 lamps at row 11. Lit lamp colours: water `#7fb0ff`/`#3f76e4`, lava `#ffd34a`/`#ff8c1a`, honey `#ffd25a`/`#e6a020`, chocolate `#9c5e34`/`#6b3a1e`; unlit `#444140`/`#3a3837`.
- **Bottom**: same frame, dark plate with four `#8a8580` bolt dots and a small grate-like checker in the middle. Also used for the inner floor and ceiling.

What works:
- The fluid level is the hero and reads well: the window is 10 of 16 px on every side, the fluid fills it exactly, and the notches give it a scale (survey: "The fluid level reads well").
- Real fluids (lava at full light, honey, chocolate) give each kind its look for free.
- The interior greys of the top and bottom are dark, near the house steel range.
- Simple full-cube silhouette that matches the full-cube shape.

What doesn't, against the house style (`create-house-style.md`):
- **The copper is the whole block and too bright and salmon**: `#e3826c`/`#d67b5b` versus house copper `#7a4a2e`/`#9c5f3a`/`#b87a4e`, and copper is meant for "pipes, rivets, warm accents", not every face (survey `:18`; the String Tangler dossier makes the same call, `string-tangler.md:126`). The `#f7cb6c` corner rivets are near-yellow.
- **Flat fills** on every band; no gentle mottling.
- **Saturated lamp colours**: lava `#ffd34a` is close to yellow, water `#7fb0ff` bright; house glows are muted amber `#d99a4a` / warm orange `#c76a2a`. Chocolate's lit lamps are darker than many unlit details and barely read as lit.
- **No Create materials or parts**: no casing, no brass, no pump, no pipe flanges, no fittings for the "modules". It doesn't read as a Create machine, nor as kin to the Geothermal Turbine it feeds.
- **No relief**: a single cube with painted frames; every face the same, nothing to tell top from side except the lamps.
- **Painted glass streak** sits in front of the fluid as opaque pixels; the rest of the window is a hole, so an empty Wellspring is see-through to the landscape behind.
- **Lamps are tiny** (2×1 px) and only on top, so from eye level beside a turbine they can't be seen.

## Function the Look Doesn't Show

1. **Generating.** Nothing moves or lights while it generates. Lava, honey and chocolate rise at most 1 px per 16 s, so a running Wellspring looks identical to an idle one; a full one with upgrades looks the same as a full one without. The survey says this "needs a sync change"; in fact the client already has the upgrades and tank (update tag `WellspringBlockEntity.java:423-434`) and the GUI computes the rate client-side (`WellspringScreen.java:127-136`), so a renderer-driven cue (bubbling, a turning part, a pulsing light) needs a **renderer change only**. A static-model cue would need a new blockstate property set in `updateBlockState` (`:292-306`). The "full" edge lags up to 10 ticks (`:41`).
2. **Which upgrades are fitted, and where.** The upgrades are described as "modules" (`en_us.json:17, 20, 41, 609, 616`), up to 3 generation + 3 capacity, but the block has no bays, slots or cards. Generation count shows only as 2×1-px lamps on the top face; **capacity upgrades show nowhere** (`upgrades` counts generation only, `WellspringBlockEntity.java:299`) even though they make the tank up to 7× bigger.
3. **The source of the fluid.** Nothing suggests where the fluid comes from (the "spring"): no pump (despite the Mechanical Pump in the recipe), no dripstone (despite dripstone in every generation upgrade), no inlet.
4. **Fluid kind with no upgrades.** With `upgrades=0` all sources share `wellspring_0` (`wellspring.json:3-44`), so the top says nothing; only the fluid in the windows does. The item icon is always the empty, unlit block.
5. **Ports on all six faces.** Pipes connect anywhere, top and bottom included (`ComedyMonkeysCreateAddons.java:127-128`); the model shows no connection points.
6. **The rate tier.** Water makes 10× what lava does at the same lamp count (`CMConfig.java:145-170`); nothing distinguishes a fast spring from a slow one but lamp colour.
7. **On contraptions** the lamps and light freeze at their assembly state (blockstate never updated by `WellspringMountedStorage`).

## Look the Code Doesn't Back Up

1. **The top porthole** looks like a sight glass into the tank, but it is painted: it shows the **generation upgrade's** colour, stays full when the tank is drained, and is dark when the tank is full but has no upgrades (`WellspringBlockEntity.java:297-299`; `wellspring.json`).
2. **"Lamps"** look like lights but are flat texture pixels with no emission; the only light is the whole block at 9 for `source=lava` (`ModBlocks.java:47`), which also glows when the tank is **empty** with lava upgrades fitted, with nothing visible glowing.
3. **"Sealed reservoir"** (`en_us.json:8`): the windows are holes (cutout), so an empty Wellspring is see-through, and the glass is only a painted streak.
4. **The bottom grate** suggests a drain or outlet at the bottom; the bottom is no different from any other face (all six are ports).
5. **The 50% notch** sits half a pixel high (rows 7 = y 8-9 px; 50% of 3-13 is y 8.0), while 25% and 75% are centred (y 5-6 at 5.5, y 10-11 at 10.5).
6. **Copper-only build** suggests a simple copper tank; the recipe and function (pump, fluid tank, copper casing, cogwheel and electron-tube modules) are a machine.
7. The tooltip and ponder promise "water or lava" (`en_us.json:8, 10, 84-85`); honey and chocolate also work but nothing on the block or its text says so (only the upgrades' own tooltips and the README).

## History

- No earlier brief, dossier, spec or plan for the Wellspring (`design/briefs/`, `design/specs/`, `design/plans/` have none). Every model, texture and the blockstate date from the initial check-in (`git log` on `A/models/block/wellspring_base.json`, `A/textures/block/wellspring_*.png`: `6a38571 Initial Checkin`). The GUI texture was redone in the Create-style GUI pass (`26adbd3 Code review fixes, unit tests, Create-style GUIs and quick moves`).
- Survey: rank 12, scores 3/3/3 = 9, effort small-medium. "Copper-framed glass cube. The fluid level reads well. The copper is too bright and salmon." Doesn't show: "Generating (needs a sync change), which upgrades are fitted" (`design/dossiers/survey.md:18`). Family: "The Wellspring feeds the turbine" (`:37`); the Geothermal Turbine dossier notes "Nothing on the model connects to or echoes the Wellspring that feeds it" (`geothermal-turbine.md:291`) and that the two "should share a heat and lava language" (`:102`).
- Tests cover function only (generation, capacity, slots, sync, codecs: `src/test/java/com/comedymonkey/createaddons/content/wellspring/`); none check models, textures or the renderer. `WellspringBlockEntityTest.updateTagSyncsTankAndUpgrades` (`:236`) pins that upgrades are in the update tag.
- Unused lang key: `wellspring.too_full_to_remove` (`en_us.json:46`) is never sent; removing a needed Capacity Upgrade is refused silently (`WellspringUpgradeInventory.java:65-70`).
