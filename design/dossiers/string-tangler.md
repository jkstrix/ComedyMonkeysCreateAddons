# Code Dossier: String Tangler

What the code does today. The String Tangler and the Stone Infuser are one machine in code (same block class, block entity class, renderer file, back socket and Ponder opening) with two kinds; "Shared with the Stone Infuser" below lists exactly what a tangler redesign must not break for the infuser.

Paths: `J/` = `src/main/java/com/comedymonkey/createaddons/`, `A/` = `src/main/resources/assets/comedymonkeysaddons/`, `D/` = `src/main/resources/data/comedymonkeysaddons/`. Model elements are cited by index (`#n`) and the line their `"from"` is on.

## Identity

- Registry id `string_tangler`. Single block, kinetic machine, 1x1x1.
- Block `J/content/machine/TwoInputMachineBlock.java` (shared with `stone_infuser`), registered `J/registry/ModBlocks.java:174-183`.
- Block entity `J/content/machine/TwoInputMachineBlockEntity.java` (`KineticBlockEntity`, shared), its own type `STRING_TANGLER` built with `MachineKind.TANGLING` (`J/registry/ModBlockEntities.java:91-92`, `J/content/machine/MachineKind.java:9-10`).
- Item: a plain `BlockItem` (`J/registry/ModItems.java:66`), in the creative tab (`J/registry/ModCreativeTabs.java:57`).
- Renderer `MachineRenderers.Tangler` in `J/client/MachineRenderers.java:99-165`, registered `J/client/ComedyMonkeysCreateAddonsClient.java:100`. No Flywheel visual, no menu or screen.
- Recipe type `comedymonkeysaddons:tangling` (`J/content/machine/MachineKind.java:24-26`), JEI category `J/compat/jei/TwoInputCategory.java`, catalyst `J/compat/jei/CMJeiPlugin.java:45`.
- Family (survey): "Two-input machines" with the Stone Infuser, "Redesign them as a pair" (`design/dossiers/survey.md:39`).

## What It Does

1. **Power**: a shaft into the **back** only (`hasShaftTowards` is the face opposite `FACING`, `TwoInputMachineBlock.java:82-84`; rotation axis = facing axis, `:87-89`). Stress 4 SU per RPM by default (`J/config/CMConfig.java:292-295`, registered `J/ComedyMonkeysCreateAddons.java:252`).
2. **Load**: string goes in the main slot (slot 0), slime balls (tag `c:slime_balls`) in the extra slot (slot 1) (`D/recipe/tangling/cobweb.json`; slot choice `TwoInputMachineBlockEntity.java:196-201`). Only items some tangling recipe uses, one kind per slot (`:204-216`). Hoppers, funnels and pipes insert from **any side** (`J/ComedyMonkeysCreateAddons.java:151-153`, handler `TwoInputMachineBlockEntity.java:365-397`); by hand, right-click with string or slime (`TwoInputMachineBlock.java:102-115`). 64 per slot (`TwoInputMachineBlockEntity.java:388-391`). Other held items (wrench, goggles, blocks) do their usual thing (`TwoInputMachineBlock.java:106-108`).
3. **Work**: while it turns, isn't overstressed, holds ≥4 string and ≥1 slime and the output has room (`TwoInputMachineBlockEntity.java:116-121`), it runs a job of 160 "16-RPM ticks" (`cobweb.json` `processing_time`). Speed is `|RPM|/16`, clamped 1–512 (`:112-114`): 8 s per cobweb at 16 RPM or slower, 2 s at 64 RPM, 0.5 s at 256 RPM (JEI states the 256 RPM time, `TwoInputCategory.java:101-106`).
4. **Finish**: takes 4 string + 1 slime, adds 1 cobweb to the output slot (`:123-131`), plays vanilla `WOOL_PLACE` at 0.6 vol / 0.8 pitch (`:137-139`) and bursts 8 cobweb item particles from the block centre (`:142-144`). If enough is left it starts the next job on the next tick (`:94-100`).
5. **Stops** (job abandoned, progress reset to 0) whenever any condition in step 3 fails (`:86-92`). The output stops it at 64 cobwebs (`:147-153`).
6. **Unload**: automation extracts only the output, from any side (`:383-386`). Empty-hand right-click takes the output first, then slime, then string (`:229-239`). Breaking drops everything (`TwoInputMachineBlock.java:127-131`, `TwoInputMachineBlockEntity.java:241-250`).
7. **Comparator**: 0 when the output is empty, else 1–15 by output fullness (`TwoInputMachineBlockEntity.java:252-255`, `TwoInputMachineBlock.java:134-141`).

## What the Player Is Told

- Name "String Tangler" (`A/lang/en_us.json:539`).
- Tooltip: "A copper-framed glass box that _tangles string_ into _cobwebs_. Watch the strands spin, twist and knot as it works." / "Twists _4 string_ and _1 slime ball_ into a _cobweb_. Needs a _shaft_ into its _back_; faster rotation tangles faster." / feed and take "from _any side_" / R-click loads, empty hand takes (`en_us.json:540-546`).
- Goggles: "String Tangler:", then each loaded input "N x Item", or "Empty"; "Making Cobweb NN%" while working; otherwise "Needs rotation", "Output full", "Needs more ingredients" or "Needs its other ingredient"; then "Output:" and the count (`TwoInputMachineBlockEntity.java:298-335`, `en_us.json:555-563`). Plus Create's kinetic stress lines (`:333`).
- Ponder "Webs on Demand" (`J/client/ponder/MachineScenes.java:48-102`, `en_us.json:572-579`): the machine drops in with a hopper and chest below (`:38-46`); "twists string and slime into cobwebs" (front, `:54-58`); a creative motor appears behind it at 64 RPM, "Give it rotation through the shaft hole in its back" (`:61-68`); a hopper drops in on top, "four string and a slime ball … from any side" (`:71-79`); working state 12 string / 3 slime making a cobweb, "Watch the strands spin, tangle and knot themselves up into a web" (`:82-88`); a cobweb in the output, "Pull the cobwebs out from any side … Spider-free, guaranteed" (`:91-99`).
- README: "A copper-framed glass box with a spindle inside … the strands spin with the shaft, are pulled in and twisted tighter and tighter, and a web balls up in the middle" (`README.md:215-221`).
- Recipe: 4 Copper Casing (corners), 3 Framed Glass (top and sides), a **Whisk** (centre), a Shaft (bottom) (`D/recipe/string_tangler.json`).
- Advancement "Web Developer": "Craft a String Tangler. Charlotte would be proud" (`en_us.json:568-569`).
- The promise: a copper and glass box where you watch string being spun and knotted into a web by a whisk-like spindle.

## States

| State | Source | How the player can tell today |
|---|---|---|
| `facing` north/east/south/west | blockstate, set to face the player on placement (`TwoInputMachineBlock.java:61`, `:77-79`) | Only from the back: the copper back plate with the shaft hole. Front, left, right and top are identical glass panes |
| Empty | inventory (synced, `TwoInputMachineBlockEntity.java:346`) | Empty glass box |
| String loaded (any count ≥1), not working | slot 0 non-empty (`MachineRenderers.java:120`) | 12 slack strands, still, sagging 0.8 px (`:127-132`). Same look for 1 or 64 string, with or without slime, turning or not |
| Slime loaded | slot 1 (synced) | Nothing; goggles only |
| Working | `processing` (synced, `:349`), client counts the timer down itself (`:80-81`) | Strands spin with the shaft and draw in and twist as progress rises (`MachineRenderers.java:123-131`) |
| Progress 0–1 | `timer`/`maxTime` (synced, `:347-348`; `getProgress` `:278-283`) | Strand radius and twist; goggles give the % |
| What it's making | `making` (synced, `:350-351`) | Goggles only. The tangler renderer never reads it |
| Not turning / overstressed / missing slime / output full | derived (`:116-121`) | All look the same: slack still strands (if string loaded). Goggles tell them apart (`:319-325`) |
| Output (cobwebs, 0–64) | slot 2 (synced) | Nothing on the model; comparator and goggles only |
| Shaft speed | Create kinetic sync | Shaft stub turns; strands spin at shaft speed while working |

## Moving and Changing Parts

| Part | Model file | Driven by | Axis / pivot / range | Source |
|---|---|---|---|---|
| Shaft stub | `A/models/block/cobble_generator_shaft.json` (`CobbleGeneratorRenderer.SHAFT`, `J/client/CobbleGeneratorRenderer.java:36-37`), Create `axis`/`axis_top` textures | `KineticBlockEntityRenderer.getAngleForBe` on the back axis | Spins about the world facing axis through the block centre; model x/y 6–10, z −0.02–3.5, yawed so it sits in the back: model-space z 12.5–16.02 for a north-facing tangler | `MachineRenderers.java:49-63`, called `:115` |
| 6 string strands | none: Java quads, vanilla `block/tripwire` sprite, UV rows v 6–8, tinted `0xF2F2F2`, both sides, cutout, block light, normal fixed up | slot 0 non-empty or `processing` | Ribbons 0.9 px wide (`w` = 0.45 px half-width), from radius `outer` at z0 to radius `inner` at z1 around the block centre (x/y 8, model axis = Z); z0/z1 pairs 5→11, 8→8, 11→5 px by strand | `MachineRenderers.java:127-139`, `:150-164` |
| 6 crossing strands | same | same | From the opposite side (base + π), radius `outer × 0.9`, z1→z0, twisted the other way | `:140-143` |
| Strand spin | | shaft angle (`getAngleForBe` on the facing axis) while `processing`; 0 otherwise | Rotates every strand about the model Z axis through the centre, at shaft speed. By my reading of `faceFront` (yaw `-(toYRot+180)`), model +Z is world +Z for north and +X for west but −Z for south and −X for east, so on south- and east-facing tanglers the strands likely spin the opposite way to the shaft (worth checking in game) | `:123-124`, `:134`, `:42-46` |
| Pull-in | | progress | `outer` 6.2 → 3.4 px, `inner` 1.2 → 0.4 px, linear | `:129-130` |
| Twist | | progress | 0.6 → 4.1 rad between each strand's ends while working; 0.15 rad idle | `:131` |
| Sag | | not working | Inner ends drop 0.8 px idle; 0 while working | `:132` |
| Cobweb particles, wool sound | | job finished | 8 item particles at the centre, spread 0.2 | `TwoInputMachineBlockEntity.java:137-144` |

No progress lerping beyond `partialTicks` in `getProgress` (`:281`). When a job finishes and the next starts, progress jumps from 1 back to 0, so the strands snap from tight back to loose. The server syncs at most every 4 ticks (`:41`, `:106-109`), and the client counts down on its own in between (`:80-81`); at 256 RPM a job is only 10 ticks.

## Code Contract

**Files a new tangler look must keep (or change the code with it)**
- Blockstate `A/blockstates/string_tangler.json`: 4 variants, `facing=north` unrotated, east y90, south y180, west y270, all `block/string_tangler`. Modelled with the **window north and the shaft south** (`MachineRenderers.java:26-29`).
- Block model `A/models/block/string_tangler.json` (parent `minecraft:block/block`, `render_type` cutout, `ambientocclusion` false; textures all borrowed: `create:block/copper_casing`, `create:block/brass_block`, `create:block/palettes/framed_glass`; `:1-10`). The tangler has **no textures of its own**.
- Item model `A/models/item/string_tangler.json`: a full copy of the block elements plus a cobweb cross (`minecraft:block/cobweb`, #22 `:1402`, #23 `:1434`), no shaft, default display transforms from `block/block`.
- No part models. Everything that moves is Java-drawn quads (strands) or the shared shaft stub. A spindle, a web or a slime blob would need new renderer code and (if modelled) a `ModelEvent.RegisterAdditional` entry like `CobbleGeneratorRenderer.SHAFT` (`ComedyMonkeysCreateAddonsClient.java:114`).

**Geometry the renderer assumes**
- Back socket: back plate at z 14–16 with a **4x4 hole at x 6–10, y 6–10** (#12 `:861`, #13 `:930`, #14 `:999`, #15 `:1068`), where the stub turns (model z 12.5–16.02). The stub's inner end face is at z 12.5, so the interior must leave x/y 6–10 clear down to z ≈ 12.5.
- Strand volume: centred on x/y 8, radius up to 6.2 px (reaching x/y 1.8–14.2 on the axes), z 5–11. The interior must stay open there and visible: a new body that blocks the view or puts solid parts in that cylinder hides or clips the strands.
- No voxel shape override: full-cube shape and collision (no `getShape` in `TwoInputMachineBlock.java`). The model should read as a full block.
- Properties: map colour orange, strength 2.5/6, copper sound, needs a pickaxe (`ModBlocks.java:176-180`, `src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json:30`), `noOcclusion`, not view-blocking, not suffocating (`ModBlocks.java:181-183`). No light level; the strands use the block's light (`MachineRenderers.java:76`).
- Facing: horizontal only, faces the player on placement, no sneak flip (`TwoInputMachineBlock.java:61`, `:77-79`). `rotate`/`mirror` follow facing (`:92-99`).
- Ports: items in and out on **all six faces**, no sided ports (`ComedyMonkeysCreateAddons.java:151-153`). No fluids, no filter slot, no value box, no scroll behaviour.
- Ponder layout (`MachineScenes.java:38-46`, `A/ponder/string_tangler.nbt`): machine at (2,2,2), creative motor behind at (2,2,3), hopper on top (2,3,2), hopper and chest below. Text arrows point at the north face, south face and the hopper; nothing points at a model part, so the scene survives a new model.

**Borrowed parts (don't redesign)**
- The shaft stub `block/cobble_generator_shaft` is drawn by every generator, the hands-off Cuckoo Clock, Auto Harvester and Animal Pens, the Electric Motor, Magma Engines and Torque Rotor, and both two-input machines; earlier dossiers record it as frozen (`design/dossiers/electric-motor.md:136`, `design/dossiers/animal-pen.md:195`). A different stub for the tangler needs its own model and a change to `MachineRenderers.java:59-61`.
- Create's copper casing, brass block and framed glass textures, vanilla tripwire and cobweb.

### Shared with the Stone Infuser (must not break)

| Shared piece | Where | What depends on it |
|---|---|---|
| Block class `TwoInputMachineBlock` | `J/content/machine/TwoInputMachineBlock.java` (both `ModBlocks.java:175-193`) | Adding a blockstate property (e.g. `working`) adds it to **both** blocks: `A/blockstates/stone_infuser.json` would need the new variants too. Facing, placement, back-only shaft and interaction are identical |
| Block entity class `TwoInputMachineBlockEntity` | same file for both types (`ModBlockEntities.java:91-95`) | Synced fields (`Inventory`, `Timer`, `MaxTime`, `Processing`, `Making`, `:343-362`) and their semantics drive the infuser renderer too (it reads `getMaking`, `getProgress`, slots 0 and 2). The finish sound/particles branch on `kind` (`:137-144`); tests pin the shared behaviour (`src/test/java/com/comedymonkey/createaddons/content/machine/TwoInputMachineBlockEntityTest.java`) |
| `MachineRenderers` helpers `faceFront`, `shaft`, `quad`, `cube`, the `COBBLESTONE` sprite | `MachineRenderers.java:31-95` | `Infuser` uses `faceFront`, `shaft` and `cube` (`:184-208`). Changing the north-window convention, the stub position or `quad`'s UV/normal handling changes the infuser. `Tangler` and `Infuser` are separate inner classes: `Tangler` (`:104-165`) can be rewritten alone |
| Back socket geometry | tangler #12–15 (`:861-1068`) = infuser #4–7 (`A/models/block/stone_infuser.json:303-510`): back plate z 14–16, 4x4 hole x/y 6–10 | Both stubs come from one `shaft()` call at the same place. Moving the tangler's socket means giving it its own stub placement; the infuser's must stay |
| Modelling convention | window north, shaft south (`MachineRenderers.java:26-29`); both blockstates the same four y-rotations | Both blockstate files have the same structure; they are separate files |
| Ponder opening `setUp` and structure layout | `MachineScenes.java:38-46`; `A/ponder/string_tangler.nbt` and `stone_infuser.nbt` hold the same blocks (machine, creative motor, motor, hoppers, chest) | Shared code; each scene's text is its own |
| Goggle keys `gui.goggles.machine.*`, JEI category, config section, Ponder tag | `en_us.json:557-567`; `TwoInputCategory.java`; `CMConfig.java:289-299`; `CMPonderPlugin.java:108-109` | Name-only coupling; not affected by a model change |
| Texture `create:block/brass_block` | tangler hub (`string_tangler.json:7`), infuser stand (`stone_infuser.json`) | Borrowed from Create; nothing of ours to break |

Not shared: block and item models, blockstate files, map colour/sound/strength (infuser: stone, 3.0, not `isSuffocating`, `ModBlocks.java:186-193`), recipes, the inner renderer classes. The infuser uses two textures of ours: `A/textures/block/stone_infuser_top.png` (infuser only) and `A/textures/block/cobble_generator_glass.png` (**shared with all generator models**, so not the tangler's to change).

## Current Look

Before sheets (each 16x16 texture scaled to 128 with nearest neighbour, on dark grey):
- Tangler: `design/dossiers/string-tangler/textures-before.png`, left to right: Create copper casing (frame), Create brass block (hub), Create framed glass (panes), vanilla tripwire (strands, before the `0xF2F2F2` tint), vanilla cobweb (item only), Create axis and axis top (shaft stub).
- Infuser, for comparison: `design/dossiers/string-tangler/infuser-textures-before.png`: Create andesite casing, vanilla smooth basalt (interior), our `stone_infuser_top`, Create brass block (stand), our `cobble_generator_glass`, vanilla cobblestone, Create asurine (item), axis, axis top.

Geometry (22 elements, `string_tangler.json`):
- 12 copper-casing edge bars 2x2 px round the cube (#0–11, `:13-791`): an open cage.
- A copper back plate z 14–16 with the 4x4 shaft hole (#12–15, `:861-1068`).
- A copper floor plate x/z 2–14, y 0–2 (#16, `:1137`): its top shows the casing's dark grey plank centre.
- A 3x3x1 brass hub at x/y 6.5–9.5, z 13–14 (#17, `:1206`). It sits entirely inside the turning shaft stub's volume (x/y 6–10, z 12.5–16), so in game it is hidden (at most a corner flickers past the stub's flats).
- Four zero-thickness framed-glass panes 0.5 px in from the outside: front (#18, `:1274`), west (#19), east (#20), top (#21, `:1370`). No glass on the bottom or back.
- Inside: nothing modelled. The strands are the only content.

Textures (measured): copper casing has a salmon copper rim `#9a5038`/`#b26247` up to `#e3826c` round a mid-grey plank centre `#4b4948`/`#5e5b5a`. Brass block is saturated yellow `#d7aa5e`, highlights `#ffeb8c`. Framed glass is a pale cyan 1 px border `#d0eae9` with corner glints, the rest clear. Tripwire rows 6–7 (the strip the strands sample) are only about a quarter opaque, grey `#5b5b5b`–`#cecece`. Cobweb is pure white `#ffffff`.

What works:
- Create material language (copper casing, framed glass, a shaft) and a clean, restrained cage. No hazard stripes, no clutter.
- Glass on four faces, so the strand animation can be seen from almost anywhere, and the motion is tied to the shaft and the job's progress.
- Strands drawing in and twisting tighter is a real progress read.

What doesn't (against `create-house-style.md`):
- The copper rim is lighter and more salmon than the house copper (`#7a4a2e`–`#b87a4e`), and it is the whole frame, not a trim. The survey makes the same call on the Wellspring's copper (`survey.md:18`).
- The framed glass's near-white cyan border draws a second frame inside the copper one on every face: near-white lines the style rules out.
- The strands are thin (0.9 px) and sample a mostly transparent strip of the tripwire texture under cutout, so they read as broken dotted lines, tinted near-white.
- The interior is empty: no spindle, no slime, no web, no floor detail. Idle and empty, it's a hollow copper glass cube with nothing saying "tangler".
- Front, sides and top look the same, so you can't see which way it faces except from the back.
- The item shows a pure-white cobweb the block never shows.
- Against the infuser: the pair don't look related. The tangler is a four-sided copper and glass cage; the infuser is a closed andesite and basalt box with one front window (`stone_infuser.json` #0–11), its own top and a raw brass stand. Only the back plate and stub are common. The infuser's `cobble_generator_glass` streaks are also near-white (`#c6e2e6`/`#f4fdff`), and its brass stand is the same saturated `brass_block`.

## Function the Look Doesn't Show

- **Slime**: the second ingredient is never drawn. A tangler with string and no slime looks the same as one ready to go.
- **How much string**: 1 string and 64 string look the same (`MachineRenderers.java:120`), and fewer than the 4 a web needs still shows strands.
- **The web growing**: the code's `making` (a cobweb) and progress never become a web. Nothing balls up in the middle (contrary to `README.md:218`).
- **Output**: finished cobwebs (up to 64) are invisible until taken; comparator and goggles only.
- **Why it's stopped**: not turning, overstressed, missing slime and output full all show the same slack still strands.
- **Finishing a web**: only the wool sound and an item-particle puff; the strands just snap back loose.
- **Which side is the front** (only the back plate shows facing), and that every face is an item port.
- **Speed**: the spin follows the shaft, but nothing else changes with speed.

## Look the Code Doesn't Back Up

- **The brass hub**: modelled, but hidden inside the shaft stub, and nothing attaches to it. The strands' inner ends meet at the block centre (z 5–11), not at the hub (z 13–14).
- **"Spindle" / whisk**: the README and the recipe's whisk promise a spindle inside; there is no spindle geometry, the strands converge on empty space.
- **"Stretched between the copper frame"** (`MachineRenderers.java:100`): the outer ends reach the frame only at progress 0 along the axes (6.2 px vs the frame's 6 px); they float free at the corners and once pulled in (3.4 px).
- **The item's cobweb cross**: the block never shows a web.
- **Glass on four faces** suggests you can look in from any side for something; the only thing to see is the strands.
- **Strand spin direction**: likely reversed relative to the shaft on south- and east-facing tanglers (see Moving Parts), so the strands may not visibly follow the shaft they're driven by.

## History

- `6a38571` Initial Checkin: block, block entity, renderer and models (the tangler's model files have no other commits).
- `26adbd3` Code review fixes: "String Tangler / Stone Infuser no longer emptied by wrenches and blocks" (the `SKIP_DEFAULT_BLOCK_INTERACTION` path, `TwoInputMachineBlock.java:106-108`), plus the unit tests `MachineKindTest`, `TwoInputMachineBlockEntityTest`, `TwoInputRecipeTest`.
- Survey: ranked 10th (score 9): "Copper-casing frame and glass. Spinning string ribbons, static hub"; doesn't show "Slime, the web growing, the cobweb output" (`survey.md:16`).
- No earlier brief, spec or plan for either machine in `design/briefs`, `design/specs` or `design/plans`.
