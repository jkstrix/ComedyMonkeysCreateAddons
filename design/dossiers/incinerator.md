# Code Dossier: Incinerator

## Identity
- Registry id `incinerator`: block (`registry/ModBlocks.java:196-202`), block entity type (`registry/ModBlockEntities.java:81-83`), simple `BlockItem` (`registry/ModItems.java:65`), creative tab (`registry/ModCreativeTabs.java:72`).
- Classes: `content/incinerator/IncineratorBlock.java` (extends `BaseEntityBlock`, implements Create `IWrenchable`, `IncineratorBlock.java:35`), `content/incinerator/IncineratorBlockEntity.java` (plain `BlockEntity` + Create `IHaveGoggleInformation`, `IncineratorBlockEntity.java:26`).
- **No renderer, no Flywheel visual, no menu, no animated texture.** The only client classes that mention it are the Ponder scene and plugin (`client/ponder/NewBlockScenes.java:159-219`, `client/ponder/CMPonderPlugin.java:66-67,107`).
- Footprint: one block, full cube.
- Family (survey, `design/dossiers/survey.md:38`): heat and lava, with the Pressure Crucible, Magma Engines, Lava Generator and Geothermal Turbine, all finished and hands-off.

All Java paths below are under `src/main/java/com/comedymonkey/createaddons/`, assets under `src/main/resources/assets/comedymonkeysaddons/`.

## What It Does
1. **Anything goes in, from any face.** The item handler and the fluid handler are registered for every side, including `null` (`ComedyMonkeysCreateAddons.java:156-160`). Hoppers, chutes, funnels, arms and Create fluid pipes can feed it on all six faces, top and bottom included.
2. **Items vanish instantly.** One fake slot that always reads empty (`IncineratorBlockEntity.java:117-127`); `insertItem` accepts the whole stack, any size, even oversized ones, adds the count to `itemsBurned` and returns empty (`:129-137`; test `IncineratorBlockEntityTest.java:46-51`). Nothing can be extracted (`:140-142`). Simulated inserts burn nothing and don't light it (`:132`; test `:54-61`).
3. **Fluids vanish instantly.** Any fluid, any amount, capacity `Integer.MAX_VALUE` (`IncineratorBlockEntity.java:156-186`); `fill` adds the amount to `fluidBurned` and accepts all of it (`:178-186`). Draining returns nothing (`:189-196`).
4. **Rate: unlimited.** No buffer, no processing time, no config (`BURN_TICKS` is a hard-coded constant, `:28`; no Incinerator entry in `CMConfig`). It destroys as fast as the inserter delivers. It is never fuelled by what it burns: water, cobblestone and lava all count the same.
5. **Lights up while burning.** Each real burn calls `burned()` (`:59-65`): if it was out it sets the `LIT` blockstate true (`:60-61`, `:67-73`) and the timer resets to 100 ticks (5 s) (`:62`). Every server tick the timer counts down; at 0 the block goes back to unlit (`:50-51`; tests `:94-119`). So it stays lit as long as something arrives at least every 5 s.
6. **Heats a boiler above while lit.** Registered as a Create `BoilerHeater`: heat `1` when `LIT`, `BoilerHeater.NO_HEAT` when not (`ComedyMonkeysCreateAddons.java:267-269`). Heat 1 is what Create gives a kindled (not superheated) Blaze Burner; `NO_HEAT` means a cold Incinerator doesn't count as a heater at all, not even passive heat (Create behaviour, not in this repo). It is binary: one stick or a full stack every tick gives the same heat 1.
7. **Effects while lit** (client `animateTick`, `IncineratorBlock.java:89-104`): a `FLAME` particle every animate tick on the front face, 0.52 out, 0.25–0.55 up, ±0.2 sideways (`:94-98`); one `LARGE_SMOKE` in three from the top centre at y + 1.05, drifting up (`:99-100`); vanilla `FURNACE_FIRE_CRACKLE` one tick in ten at volume 0.8 (`:101-103`). Nothing when cold (`:92-93`).
8. **Light** 13 when lit, 0 when cold (`ModBlocks.java:202`).
9. **Goggles** show "Incinerator:", then "Burning" (gold) or "Cold: nothing to burn" (dark grey), "Items burned: N", "Fluid burned: N mB" (`IncineratorBlockEntity.java:75-88`; lang `en_us.json:348-352`). The totals reach the client at most once a second while they change (`IncineratorBlockEntity.java:52-56`).
10. **Wrench**: Create's default `IWrenchable` (rotate; sneak-wrench to pick up) (`IncineratorBlock.java:35`). Rotation is cosmetic: it only turns the front texture and where the flames spawn.
11. **Physical**: stone sound, strength 3.5/6, needs a pickaxe (`ModBlocks.java:197-202`; `data/minecraft/tags/block/mineable/pickaxe.json:9`); drops itself if it survives the explosion (`data/comedymonkeysaddons/loot_table/blocks/incinerator.json`).

## What the Player Is Told
- Name: "Incinerator" (`en_us.json:342`).
- Tooltip summary: "A _bottomless furnace_ that _destroys_ every _item_ and _fluid_ put into it. Perfect for the overflow nobody wants." (`en_us.json:343`). When Fed: "_Funnels_, chutes, hoppers and _pipes_ can push anything in from _any side_: it is gone for good. Never fills up, never backs up." (`:344-345`). Under a Boiler: "While it is _burning_ something it _heats_ a _Steam Engine boiler_ above it like a lit _Blaze Burner_. It stays lit for _5 seconds_ after the last thing burns." (`:346-347`). Registered as a Create-style tooltip (`ComedyMonkeysCreateAddons.java:249`).
- Advancement "Burn After Reading": "Craft an Incinerator. Some problems you solve. The rest, you set on fire" (`en_us.json:486-487`), child of the Pressure Crucible advancement (`data/comedymonkeysaddons/advancement/incinerator.json:2`).
- Ponder "Gone, Reduced to Atoms" (`NewBlockScenes.java:159-219`, text `en_us.json:508-513`), tagged Kinetic Appliances (`CMPonderPlugin.java:107`). Structure `ponder/incinerator.nbt`: two south-facing Incinerators, a hopper on top of the first, a fluid pipe on its west side, a 1×1 Create fluid tank on top of the second with a steam engine on the tank's east side. Steps:
  1. Incinerator appears: "gets rid of anything you give it. No questions asked" (`NewBlockScenes.java:168-176`).
  2. Hopper on top, rotten flesh shown going in, block switched to lit: "Hoppers, chutes, funnels and arms can push items in from any side. It never fills up, so it never backs up" (`:178-189`).
  3. Pipe on the west side: "Pipes work too: any fluid piped in simply vanishes" (`:191-198`).
  4. "It stays lit for a few seconds after the last thing burns..." then set unlit (`:200-206`).
  5. Second Incinerator with the boiler on top, set lit: "...and while it's lit, it heats a Steam Engine boiler above it just like a lit Blaze Burner. Your rubbish, now a power source" (`:208-218`). The lit state is forced by the scene, so the Ponder shows exactly the lit model and particles.
- Recipe: 6 Andesite Alloy around a Magma Block (top centre), a Furnace (centre) and Andesite Casing (bottom centre) (`data/comedymonkeysaddons/recipe/incinerator.json:4-21`). Material language: andesite, a furnace, magma.
- README: "destroys any item or fluid, and heats a boiler while it burns" (`README.md:15`) and the section at `README.md:248-252`.
- Fantasy promised: a bottomless Create-andesite furnace that eats rubbish and, while it eats, works as a boiler's firebox.

## States
| State | Source | How the player can tell today |
|---|---|---|
| `lit=false` (cold) | blockstate `LIT` (`IncineratorBlock.java:39,43`) | Unlit model: dark front grille, dark top grate with dark-red flecks (`models/block/incinerator.json`). No particles, no sound, light 0. Goggles "Cold". Gives a boiler no heat. |
| `lit=true` (burning, ≤5 s since last burn) | blockstate `LIT`, set by `IncineratorBlockEntity.java:59-73` | Lit model: front grille and whole top turn saturated orange to near-white (`models/block/incinerator_lit.json`). Flames at the front, smoke from the top, crackle (`IncineratorBlock.java:91-103`), light 13 (`ModBlocks.java:202`). Goggles "Burning". Boiler gets heat 1. |
| `facing` north/east/south/west | blockstate `HORIZONTAL_FACING`, faces the placer (`IncineratorBlock.java:37,57-59`) | Which face has the grille (`blockstates/incinerator.json` y 0/90/180/270). No functional meaning. |
| `burnTicks` 0–100 | BE field, saved and in the update tag (`IncineratorBlockEntity.java:32,93,107-109`) | Not shown. The client has no ticker (`IncineratorBlock.java:85-86`), so its copy is a stale snapshot from the last sync. |
| `itemsBurned`, `fluidBurned` (long totals) | BE fields, synced at most once a second while changing (`IncineratorBlockEntity.java:33-34,52-56`) | Goggles only. |
| What item or fluid was burned | not stored anywhere | Can't be shown without new synced state. |
| Burn rate / intensity | not tracked (only totals) | Not shown; LIT is the same for one stick as for a stream of stacks. |
| Boiler above or not | not tracked by the Incinerator (Create's boiler polls `BoilerHeater`) | Not shown. The top lights the same with or without a boiler. |
| Item in inventory | `models/item/incinerator.json` parents the unlit block model | Cold block. |

## Moving and Changing Parts
| Part | Model file | Driven by | Axis / pivot / range | Source |
|---|---|---|---|---|
| None | — | — | — | No renderer or visual is registered; no `.mcmeta` animated textures. The only change is the whole-model swap on `LIT`. |
| Lit textures (front, top) | `models/block/incinerator_lit.json` | `LIT` blockstate | model swap | `blockstates/incinerator.json:6-31` |
| Flame particles | world | `LIT`, client random | front face plane, 0.25–0.55 high, ±0.2 wide | `IncineratorBlock.java:94-98` |
| Smoke particles | world | `LIT`, 1 in 3 | top centre, y + 1.05, rising 0.04 | `IncineratorBlock.java:99-100` |

## Code Contract
- **File names** the blockstate and item use: `models/block/incinerator.json` (cold) and `models/block/incinerator_lit.json` (lit); `models/item/incinerator.json` parents the cold one. Any new part split or texture names are free as long as these two block models remain (or the blockstate is edited with them). Texture files `textures/block/incinerator_front(_lit)`, `_side`, `_top(_lit)` are used only by these two models (grep of `src/main/resources/assets`), so they can be replaced or dropped freely.
- **Facing**: model space front is north; the blockstate turns it by y 90/180/270 for east/south/west, no `uvlock` (`blockstates/incinerator.json`). Front faces the player who placed it (`IncineratorBlock.java:58`). Rotatable by wrench.
- **Shape and occlusion**: no `getShape` override, so a full 16³ cube for selection and collision; the block properties don't call `.noOcclusion()` (`ModBlocks.java:197-202`), so it culls neighbours' faces. A model that isn't a full cube would show see-through gaps beside neighbours unless `.noOcclusion()` is added (a one-line code change in `ModBlocks.java`).
- **Ports on all six faces** for items and fluids (`ComedyMonkeysCreateAddons.java:157-160`). Create pipes attach at the face centre (8, 8) on any face; funnels, chutes and hoppers sit on any face, including the top and the bottom. Every face is an intake, none is an outlet.
- **The top face under a boiler**: Create's boiler looks at the block directly under its bottom tank layer through the `BoilerHeater` registry (`ComedyMonkeysCreateAddons.java:268-269`); the model's geometry doesn't affect the heat, but a Create fluid tank sits flush at y 16, so nothing may stick up above y 16 or it clips into the tank (or a hopper/chute/funnel placed on top). The top is both the heat plate and a common feed face (hopper on top in the Ponder, `NewBlockScenes.java:165,178`).
- **Particle positions are in code**: flames spawn just in front of the front face at 4–8.8 px up and within ±3.2 px of centre (`IncineratorBlock.java:95-97`); a front fire opening should sit around there or the code moves with it. Smoke rises from the top centre at 16.8 px (`:100`), i.e. inside a boiler tank when one is there.
- **Light**: 13 when lit (`ModBlocks.java:202`). The lit model has no `neoforge_data` full-bright (unlike the family's lit models, e.g. `design/briefs/lava-generator.md:78`, `design/briefs/magma-engine.md:80`); emissive glow could be added in the model JSON without code.
- **Borrowed**: parent `minecraft:block/orientable_with_bottom` (vanilla cube) and Create's `create:block/andesite_casing` for the bottom and particle (`models/block/incinerator.json:2,7-8`, same in `_lit`). Create's texture can't be edited, only swapped. No Create part models or renderers.
- **Sound**: `SoundType.STONE` and vanilla `FURNACE_FIRE_CRACKLE`; no sound events of its own (`ModBlocks.java:200`, `IncineratorBlock.java:102`).
- **Ponder** uses the real models through `ponder/incinerator.nbt`; geometry changes don't break it.

### What it shares, and hands-off
- Shared textures: only Create's `andesite_casing` (also on the Auto Harvester, Animal Pen, Stone Infuser, Torque Rotor and hands-off Trade Bot models, `design/dossiers/animal-pen.md:207`).
- The Incinerator itself is not on `design/hands-off.md`.
- Files it shares with hands-off blocks, where only Incinerator lines may change:
  - `client/ponder/NewBlockScenes.java`: also holds the hands-off Magma Engine scene (`NewBlockScenes.java:103-156`; `design/hands-off.md:48`). Only `incinerator(...)` (`:159-219`) is the Incinerator's.
  - `ComedyMonkeysCreateAddons.java`: its capability lines (`:156-160`) sit right beside the hands-off Magma Engine capability (`:161-163`) and above the Lava Generator's (`design/hands-off.md:32,48`); its boiler-heater lines are `:267-269`.
  - `registry/ModBlocks.java:195-202` for the properties (e.g. `.noOcclusion()`).
- The family's finished textures (`lava_generator_*`, `magma_engine_*`, `pressure_crucible*`, `geothermal_turbine_*`) are hands-off assets (`design/hands-off.md:26,33,49,55`). Copying their palette is fine; referencing those files from the Incinerator model would tie it to hands-off assets, so own copies are safer.

## Current Look
Contact sheet (each 16×16 scaled to 128, nearest neighbour): `design/dossiers/incinerator/textures-before.png`, left to right `incinerator_front`, `incinerator_front_lit`, `incinerator_side`, `incinerator_top`, `incinerator_top_lit`. Bottom is Create's andesite casing (not in this repo).

Model: one full cube from the vanilla `orientable_with_bottom` parent (front on north, the side texture on east, west and south, top, andesite-casing bottom). No elements of its own.

Textures (16×16 RGBA, colours sampled from the PNGs):
- **Frame on every face**: a 2 px light grey border with bright highlights `#9aa49d` `#a8b3ab` and a green-grey cast (`#828784` `#60635f` `#505351`), and a dark bevel inside.
- **Front (cold)**: a 10×10 dark iron grille (3×3 bars) over near-black `#1e1410` `#120d0b` with two or three tiny dark-red ember pixels.
- **Front (lit)**: the same grille filled with a vertical fire gradient: `#b8400c` `#e05a12` at the top, `#ff8c1a` `#ffc23a`, then `#ffe27a` and near-white `#fff4c0` across the bottom rows. 52 bright pixels.
- **Side** (also the back): a brown, wood-like inner frame `#553a1f` `#5a4424` around a dark panel with three vertical slats (`#1c1b1a`).
- **Top (cold)**: the brown inner frame round a dark checker grate with dark-red `#3a1a0e` cells.
- **Top (lit)**: the same, with the whole grate saturated `#ffc23a` ×20, `#ff8c1a` ×16, `#e05a12` ×12: the whole top reads orange-yellow.

What works:
- The cold state is properly dark, and the cold front grille and top grate read as "furnace" at a glance.
- The cold top's dark-red flecks hint at embers in the right muted key.
- The grey frame sits comfortably next to Create's andesite casing on the bottom.

What doesn't, against `create-house-style.md` and the family:
- Near-white glow (`#fff4c0`, `#ffe27a`) on the lit front: "Never near-white" (house style, glow row).
- A whole saturated orange-yellow top when lit: "Never whole surfaces of saturated yellow"; glow is for small areas.
- Frame highlights `#9aa49d` `#a8b3ab` are lighter than the mid-grey cap (`#8a8c86`), and the green-grey cast is what the Lava Generator brief rules out ("no green-grey mottle", `design/briefs/lava-generator.md:42`).
- The brown inner frames are a wood/rust tone that matches neither Create's andesite nor the family's blue-grey steel `#2f343b` `#3c424a` `#4a5059`.
- The family's ember language is muted amber/orange behind near-black grates: `lava_generator_glow` is mottled `#a4521f` `#c06c2c` `#d08a3e` with dark-red `#5a2a18`, grates `#1f2327`, briefs' glow `#c76a2a` `#d99a4a`, lit glow full-bright. The Incinerator is far hotter and yellower, and not full-bright.
- The same framed square on five faces makes a crate or "textured cube", flat with no depth. Nothing like the family's firebox-on-a-plinth massing (`design/briefs/lava-generator.md:46-55`, `geothermal-turbine.md:74`).

## Function the Look Doesn't Show
- **Its boiler-heater role.** Nothing says "a boiler sits on me": no heat plate, flue or firebox crown on the top. The top lights the same with or without a tank above, and smoke spawns inside the tank when there is one (`IncineratorBlock.java:100`).
- **Items and fluids going in.** No renderer, and the item or fluid identity isn't even stored. Only totals sync, once a second (`IncineratorBlockEntity.java:52-56`). A throat, chute mouth or fire bed has nothing to draw from today; showing what's burning would need new synced state.
- **That every face is an intake.** Items and fluids enter on all six faces (`ComedyMonkeysCreateAddons.java:157-160`), but only the front grille looks open. The slatted sides look like closed vents, and the andesite-casing bottom looks sealed.
- **Bottomless.** Nothing suggests "never fills up": no hopper mouth, no void, no fire pit.
- **Burn intensity.** The code has none: `LIT` is binary, heat is 1 or nothing (`ComedyMonkeysCreateAddons.java:269`), and the rate isn't tracked. Any "busier when fed faster" look needs new client state (e.g. a synced rate or the totals' delta).
- **The 5-second linger.** `burnTicks` counts down server-side but the client never ticks it (`IncineratorBlock.java:85-86`), so a fading glow needs code. The look snaps from full fire to cold.
- **Cold means no heat at all** (`NO_HEAT`, not passive), so a boiler on a cold Incinerator stops. Only the dark textures say this.

## Look the Code Doesn't Back Up
- **The front grille as the way in.** It looks like the furnace door and the only opening, but the front has no special role: every face takes items and fluids, and `FACING` only orients the texture and the flames (`IncineratorBlock.java:57-59,94-97`).
- **The side slats as air vents or a radiator.** No airflow, cooling or side function exists.
- **The near-white fire as extreme heat.** It burns at one fixed intensity and gives the same heat as a kindled Blaze Burner, heat 1, not superheated (`ComedyMonkeysCreateAddons.java:269`). A white-hot core suggests a hotter, superheated grade.
- **The orange top as an open fire grate under the boiler.** It glows whether or not a boiler is there, and the boiler's heat doesn't depend on the top at all; that's Create's `BoilerHeater` lookup.
- **A "chimney".** The code comment speaks of "the grille and chimney" (`IncineratorBlock.java:89`) and smoke rises from the top centre (`:100`), but the model has no chimney or flue.
- **Wood-toned inner frames.** The recipe is andesite alloy, a furnace, a magma block and andesite casing (`recipe/incinerator.json:9-21`). There's no wood in it, and wood in a furnace frame contradicts the fantasy.
- **A fuel-burning furnace.** It burns anything and is never fuelled by what it eats (water lights it as well as coal does), and it has no fuel slot. A conventional fire-door-and-ash-pit furnace reads as "feed it fuel".

## History
- Every Incinerator file dates from `6a38571 Initial Checkin` (`git log --follow` on the model, textures and `content/incinerator/`). The look has never been revised.
- No earlier brief, spec, plan, dossier or Blockbench model (`design/briefs/`, `design/specs/`, `design/plans/`, `design/models/` have no incinerator entries).
- The survey ranks it 6th, score 10, effort small–medium (`design/dossiers/survey.md:12`), and puts it in the heat-and-lava family (`:38`). The family's language comes from the finished briefs: crisp blue-grey steel with andesite trim, brass on bezels only, copper for lava ports and pipes, muted amber heat behind dark grates, full-bright lit models (`design/briefs/geothermal-turbine.md:34,48,54`, `lava-generator.md:30,42`, `magma-engine.md:33,45`, `pressure-crucible.md:31,43`).
- Not covered by the sound staging pass (`design/sound/staging.md` has no Incinerator entry). It uses only the vanilla furnace crackle.
