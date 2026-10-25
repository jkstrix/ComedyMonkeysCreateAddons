# Code Dossier: Stone Infuser

What the code does today. The Stone Infuser and the String Tangler are one machine in code with two kinds. Everything they share (block class, block entity, synced fields, the `faceFront`/`shaft`/`quad`/`cube` helpers, the back socket, the Ponder opening, goggle keys, JEI category, config section) is already analysed in `design/dossiers/string-tangler.md` ("What It Does" steps 1-7 and "Shared with the Stone Infuser"). This dossier references that and records only what is the infuser's own.

Paths: `J/` = `src/main/java/com/comedymonkey/createaddons/`, `A/` = `src/main/resources/assets/comedymonkeysaddons/`, `D/` = `src/main/resources/data/comedymonkeysaddons/`. Model elements are cited by index (`#n`) and the line their `"from"` is on.

Line-number note: the tangler dossier's citations into `TwoInputMachineBlock.java` don't match the file as it is now. The correct lines are placement `:50-52`, `hasShaftTowards` `:55-57`, rotation axis `:60-62`, rotate/mirror `:65-72`, `useItemOn` `:75-88` (the wrench/blocks skip at `:79-81`), `useWithoutItem` `:91-97`, drops `:100-104`, comparator `:107-114`. The behaviour it describes is right.

## Identity

- Registry id `stone_infuser`. Single block, kinetic machine, 1x1x1.
- Block `J/content/machine/TwoInputMachineBlock.java` (shared), registered `J/registry/ModBlocks.java:186-193`.
- Block entity `J/content/machine/TwoInputMachineBlockEntity.java` (shared), its own type `STONE_INFUSER` built with `MachineKind.INFUSING` (`J/registry/ModBlockEntities.java:94-95`, `J/content/machine/MachineKind.java:11-12`).
- Item: a plain `BlockItem` (`J/registry/ModItems.java:67`), in the creative tab (`J/registry/ModCreativeTabs.java:58`), with a Create-style tooltip (`J/ComedyMonkeysCreateAddons.java:251`).
- Renderer `MachineRenderers.Infuser` (`J/client/MachineRenderers.java:169-221`), registered `J/client/ComedyMonkeysCreateAddonsClient.java:101`. No Flywheel visual, no menu or screen.
- Recipe type and serializer `comedymonkeysaddons:infusing` (`J/registry/ModRecipes.java:31-34`). JEI category `J/compat/jei/TwoInputCategory.java` (`J/compat/jei/CMJeiPlugin.java:28`), catalyst `CMJeiPlugin.java:46`.
- Loot table `D/loot_table/blocks/stone_infuser.json` (drops itself), advancement `D/advancement/stone_infuser.json`.
- Family (survey): "Two-input machines" with the String Tangler, "Redesign them as a pair" (`design/dossiers/survey.md:39`). Survey row: rank 9, score 10 (`survey.md:15`).

## What It Does

Power, loading, unloading, stopping, automation and comparator are the shared behaviour (tangler dossier "What It Does" 1-2 and 5-7). The infuser's own parts:

1. **Power**: a shaft into the **back** only (`TwoInputMachineBlock.java:55-57`). Stress **8 SU per RPM** by default, twice the tangler's 4 (`J/config/CMConfig.java:296-299`, registered `J/ComedyMonkeysCreateAddons.java:253`).
2. **Load**: cobblestone goes in the main slot (slot 0), nuggets in the extra slot (slot 1) (each recipe's `ingredient`/`extra`; slot choice `TwoInputMachineBlockEntity.java:196-201`). Only one kind of nugget at a time (`:204-216`), so one machine makes one stone until its nugget slot is emptied.
3. **Recipes** (each 1 cobblestone + 9 nuggets, 200 "16-RPM ticks"):

   | Nuggets (slot 1, tag) | Stone made | Source |
   |---|---|---|
   | 9 zinc (`c:nuggets/zinc`) | Create **asurine** (blue) | `D/recipe/infusing/asurine.json:7-13` |
   | 9 iron (`c:nuggets/iron`) | Create **crimsite** (red) | `D/recipe/infusing/crimsite.json:7-13` |
   | 9 gold (`c:nuggets/gold`) | Create **ochrum** (yellow) | `D/recipe/infusing/ochrum.json:7-13` |
   | 9 copper (`c:nuggets/copper`) | Create **veridium** (green) | `D/recipe/infusing/veridium.json:7-13` |

   Main ingredient `minecraft:cobblestone`, count 1, in all four (`:2-6`). The unit test pins all of this: cobblestone x1, 9 nuggets, result named after the file (`src/test/java/com/comedymonkey/createaddons/content/machine/TwoInputRecipeTest.java:153-165`).
4. **Work**: shared rule (`TwoInputMachineBlockEntity.java:112-121`), speed `|RPM|/16` clamped 1-512. At 200 ticks: **10 s per stone at 16 RPM or slower, 2.5 s at 64 RPM, 0.625 s at 256 RPM** (12.5 ticks; JEI prints "0.6s at 256 RPM", `TwoInputCategory.java:101-106`, `A/lang/en_us.json:564`).
5. **Finish**: takes 1 cobble + 9 nuggets, adds 1 stone to the output (`:123-131`), plays Create's **`MECHANICAL_PRESS_ACTIVATION` at 0.4 vol / 1.2 pitch** (the tangler's wool sound is the other branch, `:137-141`) and bursts 8 item particles of the finished stone from the block centre (`:142-144`). Output stops it at 64 stones (`:147-153`).
6. **Where the metal comes from** (README only, not this mod's data): crushing the stones gives fewer than 9 nuggets back, and crushing tuff gives a few of all four, so a Tuff Generator can feed it (`README.md:236-238`; Ponder text 4, `en_us.json:583`).

## What the Player Is Told

- Name "Stone Infuser" (`en_us.json:547`).
- Tooltip: "Infuses _cobblestone_ with _metal nuggets_ to make Create's _ore-bearing stones_: asurine, crimsite, ochrum and veridium" / "When Powered": the four nugget-to-stone pairs, "Needs a _shaft_ into its _back_" / "When Automated": feed both and take the stone "from _any side_" / R-click loads, empty hand takes (`en_us.json:548-554`). Unlike the tangler's tooltip, it doesn't describe anything you can watch.
- Goggles: header "Stone Infuser:" (`en_us.json:556`, key from `kind.id()`, `TwoInputMachineBlockEntity.java:300`), then the shared lines (`:301-333`, `en_us.json:557-563`): "N x Cobblestone", "N x Zinc Nugget", "Making Asurine NN%", etc. The goggles are the only place that names the nugget.
- Ponder "Cobblestone with Ambition" (`J/client/ponder/MachineScenes.java:104-156`, `en_us.json:573`, `:580-583`), after the shared opening (`:38-46`):
  1. "turns cobblestone into Create's ore-bearing stones", pointing at the north face (`:110-115`).
  2. Creative motor behind (2,2,3) and a hopper on top (2,3,2) appear, speed 64 (`:117-120`); a 9-zinc-nugget icon over the hopper (`:121-122`); state 8 cobble / 36 zinc, making asurine over 160 ticks (`:123-124`); "Power it through its back and feed it cobblestone and nine nuggets for each stone. Zinc nuggets make asurine..." (`:125-130`).
  3. "...iron nuggets make crimsite, gold makes ochrum and copper makes veridium": cycles iron/crimsite, gold/ochrum, copper/veridium, each a 140-tick job shown for 36 ticks with its nugget icon over the hopper (`:132-146`). At 64 RPM (4x) a 140-tick job takes 35 ticks, so each stone almost finishes creeping up before the next starts.
  4. Inputs emptied, one veridium in the output (the finished block shows on the stand), "Crush the stone again ... Crushing tuff gives a few of all four", pointing at the west side of (2,1,2), the hopper below (`:148-154`).
- README: "Through the window you can watch the new stone creep up over a block of cobblestone" (`README.md:225-241`, quote `:238`).
- Recipe: 4 Andesite Casing (corners), Framed Glass (top centre), 2 Iron Sheets (middle sides), a **Basin** (centre), a Shaft (bottom) (`D/recipe/stone_infuser.json:4-25`).
- Advancement "Mineral Rights": "Craft a Stone Infuser. Cobblestone with ambition" (`en_us.json:570-571`).
- Code comments call the extra ingredient "crushed ore" (`ModBlocks.java:185`, `MachineKind.java:11`); the recipes, lang and README all say nuggets.
- The promise: an andesite box with a basin inside where metal soaks into a block of cobblestone, turning it into a coloured ore stone you can watch through the window.

## States

Shared rows (empty, progress, why it's stopped, output count, shaft speed) behave as in the tangler dossier. The infuser's reading of them:

| State | Source | How the player can tell today |
|---|---|---|
| `facing` north/east/south/west | blockstate (`TwoInputMachineBlock.java:34`, `:50-52`) | Clearly: the front is the only face with a window. Sides and top are solid casing; the back has the shaft plate |
| Empty | inventory | Empty basalt chamber with the brass stand |
| Cobble loaded (1-64), not working, no output | slot 0 non-empty (`MachineRenderers.java:191`, `:207-208`) | One 8x8x8 px cobble cube on the stand, still. Same for 1 or 64, with or without nuggets, turning or not |
| Nuggets loaded, which metal, how many | slot 1 (synced) | **Nothing.** The renderer never reads slot 1. Goggles only |
| Working | `processing` (synced) | Cobble cube turns slowly; a shell of the new stone climbs it from the bottom (`:193-204`) |
| Progress 0-1 | `getProgress` (`TwoInputMachineBlockEntity.java:278-283`) | Height of the new-stone shell, 0 to 8 px. A real progress bar |
| What it's making | `making` (synced) | **Yes, by colour**: the shell uses the result block's particle sprite (`MachineRenderers.java:190`, `:204`, `:215-220`), so asurine shows blue, crimsite red, ochrum yellow, veridium green. The infuser renderer reads `making`; the tangler's doesn't |
| Output present (0-64), not working | slot 2 (synced) | One full cube of the finished stone on the stand (`:205-206`), replacing the cobble even when cobble is loaded. 1 and 64 look the same |
| Output present while working | slot 2 + `processing` | Hidden: the working branch wins (`:200-204`), so finished stones vanish from view while the next one is made |
| Not turning / overstressed / missing nuggets / output full | derived | All look the same: a still cube (cobble, or the output stone if there is one). Goggles tell them apart |
| Shaft speed | Create kinetic sync | Shaft stub turns. The cube's turn does **not** follow it (fixed rate, below); only the shell's climb speeds up |

## Moving and Changing Parts

| Part | Model file | Driven by | Axis / pivot / range | Source |
|---|---|---|---|---|
| Shaft stub | `A/models/block/cobble_generator_shaft.json`, shared helper | shaft angle | As in the tangler dossier: spins on the facing axis, model-space z 12.5-16.02, x/y 6-10 | `MachineRenderers.java:49-63`, called `:184` |
| Cobble cube | none: Java `cube()` with vanilla `block/cobblestone` (`:32`), cutout, block light | slot 0 non-empty, or working | 8 px cube x/z 4-12, y 3.5-11.5 (sits on the stand top at 3.5) | `:202`, `:208` |
| New-stone shell | none: `cube()` with the result block's particle sprite | `processing` and `making` non-empty | x/z 3.9-12.1 (0.1 px proud of the cobble), y from 3.4 to `3.5 + 8 × progress`, linear, drawn once taller than 0.05 px; reaches the cube's top (11.5) at 100% | `:200-204` |
| Finished-stone cube | none: `cube()` with the output's particle sprite | not working and slot 2 non-empty | Same box as the cobble | `:205-206` |
| Turn | | `processing` only; **game render time, not shaft speed** | Cube and shell rotate about the vertical axis through the block centre (pivot x/z 8, model +Y), **1.5° per tick = 30°/s, one turn every 12 s** at any RPM; 0° when idle | `:192-198` |
| Stone particles, press sound | | job finished | 8 item particles of the result at the centre, spread 0.2; Create press activation 0.4/1.2 | `TwoInputMachineBlockEntity.java:140-144` |

Notes, by my reading of the code:
- The turn angle comes from the free-running render clock, so the cube snaps back to 0° whenever `processing` goes false, and resumes at whatever angle the clock gives when it restarts.
- The shell's height jumps from full back to 0 when one job ends and the next starts; sync is every 4 ticks at most and the client counts down on its own in between (shared, tangler dossier).
- The rotating cube's corners sweep a circle of radius 5.66 px (shell 5.8 px) about x/z 8, i.e. x/z 2.2-13.8. That clears the chamber walls (x 2/14) by 0.2 px, but the shaft stub pokes into the chamber at x/y 6-10, z 12.5-14, and each corner passes through it to a depth of about 1.2 px as it swings past the back, four times per turn.
- At 100% the shell's top face (y 11.5) is coplanar with the cobble's top face, so the top probably z-fights in the last moment of each job.
- All `cube()` faces go through `quad()` with white vertex colour and a fixed up normal (`MachineRenderers.java:74`, `:77`, `:88`), so the cube has no per-face shading, unlike the baked block model around it.

## Code Contract

Shared pieces (block class, BE, helpers, back socket, modelling convention, Ponder opening) are in the tangler dossier's "Shared with the Stone Infuser" table and apply unchanged. The infuser's own:

**Files a new infuser look must keep (or change the code with it)**
- Blockstate `A/blockstates/stone_infuser.json`: 4 variants, north unrotated, east y90, south y180, west y270, all `block/stone_infuser`. Modelled window north, shaft south (`MachineRenderers.java:26-29`). Adding a blockstate property to the shared block class adds it here too.
- Block model `A/models/block/stone_infuser.json`: parent `minecraft:block/block`, `render_type` cutout, `ambientocclusion` false (`:2-4`). Textures (`:5-12`): `create:block/andesite_casing` (casing, particle), `minecraft:block/smooth_basalt` (inner), **`comedymonkeysaddons:block/stone_infuser_top`** (ours, infuser only), `create:block/brass_block` (stand), **`comedymonkeysaddons:block/cobble_generator_glass`** (ours, shared with every cobble/basalt/blackstone/tuff generator model and their wireless and powered variants, so not the infuser's to change; give it its own glass texture if it needs one).
- Item model `A/models/item/stone_infuser.json`: a copy of the block elements (#0-12) plus a half cobble / half asurine cube (#13 cobble y 3.5-7.5, `:787`; #14 asurine `create:block/palettes/stone_types/natural/asurine_0` y 7.5-11.5, `:855`). No shaft. Parent `block/block` display transforms.
- No part models. The cube, shell and finished stone are Java-drawn with world sprites; anything else that moves (a basin, nuggets, a pour) needs new renderer code.

**Geometry the renderer assumes (infuser only)**
- **The stone's stand height**: the renderer draws the cube from y 3.5 (shell from 3.4) at x/z 4-12 in fixed pixel coordinates (`MachineRenderers.java:202-208`). Whatever holds it must top out at y 3.5, centred on x/z 8, or the renderer's numbers change with the model. Today that is the brass stand #12 (x/z 6-10, y 2-3.5, `:718`) on the chamber floor (#0 top at y 2, `:15`).
- **The turning volume**: a vertical cylinder about x/z 8, radius about 5.8 px (x/z 2.2-13.8), y 3.4-11.5, must stay empty. A new model can't put a basin rim, pipes or walls inside it. The existing stub intrusion at z 12.5-14 already clips it (Moving Parts notes).
- **Visibility**: the stone is the whole show and today it is seen only through the front window (#9-11: casing lips y 2-3 and 13-14 at z 0-1, glass plane x/y 2-14 at z 0.9; `:602`, `:644`, `:686`). Sides (#2, #3) and top (#1) are solid casing 2 px thick, so the chamber is x 2-14, y 2-14, z 1-14.
- Back socket as the tangler's, which it must keep: back plate #4-7 (`:303-510`) with the 4x4 hole x/y 6-10, plus #8 (`:579`), a basalt-faced sliver at z 14-14.01 over the lower back strip. #8 duplicates #4's inner face 0.01 px in front of it.
- Full-cube shape, no `getShape` (shared). Properties (`ModBlocks.java:187-193`): map colour stone, strength 3.0/6.0, stone sound, pickaxe (`src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json:31`), `noOcclusion`, not view-blocking. Unlike the tangler it doesn't override `isSuffocating`, so it suffocates like a solid block. No light level; the cube uses the block's light (`MachineRenderers.java:76`).
- Ports: all six faces, items only (shared). No fluid port, although the recipe's Basin suggests one.
- Ponder (`A/ponder/stone_infuser.nbt`, same layout as the tangler's): text arrows point at the north face (`MachineScenes.java:112`, `:127`, `:136`) and the lower hopper (`:151`); item icons float over the top hopper (`:122`, `:141`). No arrow points at a model part, so the scene survives a new model, but texts 1-3 assume the stone is visible from the north.

**Borrowed parts (don't redesign)**
- Shaft stub `block/cobble_generator_shaft` (frozen, see tangler dossier).
- Create andesite casing, brass block, framed glass (recipe only), and the four Create stone sprites; vanilla smooth basalt and cobblestone.

## Current Look

Before sheet: `design/dossiers/string-tangler/infuser-textures-before.png` (made for the tangler dossier; it covers every infuser texture), left to right: Create andesite casing, vanilla smooth basalt, our `stone_infuser_top`, Create brass block, our `cobble_generator_glass`, vanilla cobblestone, Create asurine (item only), Create axis, axis top.

Geometry (13 elements, `stone_infuser.json`):
- A closed andesite box: floor slab y 0-2 (#0), roof slab y 14-16 (#1), side walls x 0-2 and 14-16 (#2, #3), all faced inside with smooth basalt (`:15-302`).
- Back plate z 14-16 with the shaft hole (#4-7), and the redundant basalt sliver #8.
- One front window: 1 px casing lips top and bottom (#9, #10) and a zero-thickness glass plane at z 0.9 (#11), 12 px wide by 10 px of opening.
- A 4x4x1.5 brass stand in the middle of the floor (#12). Nothing else inside; the stone is Java-drawn.

Textures (measured): `stone_infuser_top` is 16x16, an andesite-grey rim (`#505351`-`#a8b3ab`), a brown border (`#553a1f`/`#614b2e`) and a dark checkered grate centre (`#1c1b1a`/`#3a3837`), painted on the solid roof. `cobble_generator_glass` is transparent with near-white streaks (`#c6e2e6`, `#f4fdff`). Brass block is saturated yellow (`#d7aa5e`, highlights `#ffeb8c`).

What works:
- The front is unmistakable, and the window frames one clear subject.
- The shell climbing the cobble is a true, readable progress bar, and its colour tells you which stone is being made.
- Create materials (andesite casing, a shaft), restrained, no stripes.

What doesn't (against `create-house-style.md`):
- The raw brass block stand is the brightest, most saturated thing in the box, and it's a full brass block texture squeezed onto a 4x4 plinth.
- The glass streaks are near-white lines.
- The roof grate is a painted texture over a solid slab: it reads as an intake with nothing behind it.
- The cube's faces are flat-lit (no shading), so the hero object looks pasted in.
- From the side or top it's a plain andesite crate; nothing says "infuser" except through the front.
- Pair: it shares nothing visible with the copper-and-glass tangler except the back plate (see tangler dossier "What doesn't").

## Function the Look Doesn't Show

- **The nuggets**: the second ingredient is never drawn. A cobble cube with 0 nuggets looks the same as one ready to go, and nothing shows 9 vs 36 vs 64.
- **Which metal**: the nugget kind is goggles-only. You learn the metal only indirectly, from the shell's colour, and only once a job is running.
- **The infusing itself**: nothing carries metal to the stone. No pour, drip, glow, basin or bath; the new texture simply appears around the cobble's base and climbs. The only finish cue is a press "clunk" and an item-particle puff.
- **How much cobble**: 1 and 64 look the same.
- **Output**: one finished cube stands in for 1-64 stones, and it is hidden entirely while another job runs.
- **Why it's stopped**: not turning, overstressed, missing nuggets and output full all look the same still cube.
- **Speed**: the cube turns at a fixed 30°/s whatever the shaft does, so the visible motion doesn't follow the power; only the climb rate does.
- **Ports**: every face takes items, but only the front looks like an opening (and it's glass), and the roof grate suggests a top-only intake.

## Look the Code Doesn't Back Up

- **The roof grate** (`stone_infuser_top`): looks like a hopper intake or vent; the roof is a solid slab and items enter from any side.
- **The Basin in the recipe**: no basin in the model, no liquid, no fluid capability.
- **The brass stand as a turntable**: the cube turns on it, but the stand is static model geometry and nothing connects it to the shaft; the turn is a clock, not a drive.
- **The turn itself**: it implies the shaft is turning the stone, but it ignores the shaft's speed and direction.
- **The item's cube**: half cobble at the bottom, asurine on top. In the world the new stone grows from the bottom up, so the item shows the opposite of a part-infused block, and always asurine.
- **Code comments' "crushed ore"** (`ModBlocks.java:185`, `MachineKind.java:11`): the machine takes nuggets.
- **Iron sheets in the recipe**: no iron parts in the model.

## History

- `6a38571` Initial Checkin: block, block entity, renderer, models and `stone_infuser_top.png` (no later commits touch the infuser's model, texture or `MachineRenderers.java`).
- `26adbd3` Code review fixes: the shared wrench/blocks fix and the unit tests (see tangler dossier).
- Survey: rank 9, score 10: "Andesite frame, raw brass-block stand, near-white glass glints. The cobble turns while the new stone creeps up it"; doesn't show "The nuggets, the infusing itself, which metal" (`survey.md:15`).
- No earlier brief, spec, plan or model file for either machine in `design/briefs`, `design/specs`, `design/plans` or `design/models`.
