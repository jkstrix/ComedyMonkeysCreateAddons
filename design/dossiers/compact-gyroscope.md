# Code Dossier: Compact Gyroscope

What the code does today. The Compact Gyroscope is one of the three "ship hardware" blocks (with the Torque Rotor and Flight Instruments) that exist only when Sable / Create Aeronautics is installed. "Shared with the ship hardware set" at the end lists what the three have in common.

Paths: `J/` = `src/main/java/com/comedymonkey/createaddons/`, `S/` = `J/compat/sable/`, `A/` = `src/main/resources/assets/comedymonkeysaddons/`, `D/` = `src/main/resources/data/comedymonkeysaddons/`. Short names: `CGB` = `S/CompactGyroscopeBlock.java`, `CGBE` = `S/CompactGyroscopeBlockEntity.java`, `AR` = `S/client/AeronauticsRenderers.java`, `AC` = `S/AeronauticsContent.java`, `AS` = `S/client/AeronauticsScenes.java`. Model elements are cited by index (`#n`) and the line their `"from"` is on.

## Identity

- Registry id `compact_gyroscope`. Single block, kinetic consumer, 1x1x1.
- Block `CGB` (`KineticBlock`, `IBE`), registered `AC:27-36`. Block entity `CGBE` (`KineticBlockEntity` + Sable's `BlockEntitySubLevelActor`, `CGBE:34`), type registered `AC:83-85`. Plain `BlockItem` `AC:79`.
- Renderer `AR.GyroscopeRenderer` (`AR:85-103`), registered `AR:45`. No Flywheel visual, no menu, no screen, no part model of ours.
- Only exists with Sable: `SableCompat.LOADED` = `ModList.isLoaded("sable")` (`S/SableCompat.java:10-11`) gates `AeronauticsContent.init()` (`J/ComedyMonkeysCreateAddons.java:73-74`), common setup and the Create tooltip (`:270-273`), the renderers and extra models (`J/client/ComedyMonkeysCreateAddonsClient.java:102-103`, `:121-122`), Ponder (`J/client/ponder/CMPonderPlugin.java:91-92`, tag `:119-122`) and the creative tab (`J/registry/ModCreativeTabs.java:73-74`). Every recipe and the advancement carry a `neoforge:mod_loaded` `sable` condition; the pickaxe tag entry is `"required": false` (`src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json:12-15`).
- Origin: a port of the Gyroscope Stabilizers mod's Gyroscope controller (MIT, by raprek) into one block (`CGBE:29-33`, `AC:26`).
- Family (survey): "Ship hardware" (`design/dossiers/survey.md:41`).

## What It Does

1. **Power**: a shaft from **above or below** only (`hasShaftTowards` is any Y face, `CGB:24-26`; rotation axis always Y, `CGB:19-21`). Stress 16 SU per RPM by default (`J/config/CMConfig.java:391-394`, registered `AC:103`), so 4,096 SU at the full-strength speed.
2. **Flywheel spin**: the block's own "effective speed" chases the shaft's speed, scaled by the redstone brake: target = shaft RPM × (15 − signal) / 15 (`CGBE:90`). It closes the gap gently (exponential chase, rate 1/64 per tick, `CGBE:39`, `:90-91`), so the flywheel spins up and down over a few seconds rather than snapping.
3. **Redstone brake**: every server tick it reads the strongest signal from any of the six neighbours (`getBestNeighborSignal`, `CGBE:81-88`); a change is saved and synced (`:83-86`). Signal 15 stops the flywheel while the shaft keeps turning (`en_us.json:692`, `README.md:258`). The block is not a redstone conductor (`AC:34`).
4. **Strength**: 0 to 1 = |effective speed| / reference RPM (256 by default), capped at 1 (`CGBE:128-130`, `CMConfig.java:387-390`). All three gains scale with it (`CGBE:134-137`).
5. **Levelling (only on an Aeronautics ship, in Sable's physics step)** (`CGBE:133-182`):
   - Works out the axis to turn about to bring the ship's up back to world up (`:154-156`) and applies a restoring torque along it, `strength` 16,000 at full spin (`:164`, `CMConfig.java:371-374`).
   - Adds an integral term that removes the steady lean from off-centre weight (gain 6,400, capped at 0.5, `:158-162`, `CMConfig.java:379-386`).
   - The restoring push is limited to a 0.5 rad/s change per step (`CGBE:37`, `:165-168`).
   - **Damps all of the ship's spin**, about every axis including yaw (gain 4,000, `:170-172`, `CMConfig.java:375-378`), clamped so it can slow a spin to a stop but never reverse it (`:191-201`).
   - Applies the sum as a torque impulse in the ship's frame (`:174-175`).
   - With no spin (strength 0) it does nothing and resets the integral (`:139-144`).
6. **Readings**: the physics step records how hard it pushed (0 to 1, square-rooted so small corrections still show, `:177-180`) and the ship's tilt in degrees (`:181`, `:185-188`). Every 10 ticks the server checks whether physics steps ran (that is, whether it's on a ship) and syncs on-ship, effort and tilt when they change by more than 0.03 / 0.5° (`:97-98`, `:113-125`).
7. **Off a ship** it only spins: it draws stress and turns the flywheel, but no physics step runs, so nothing is levelled (goggles say "Only works on an Aeronautics ship", `:216-219`).
8. **Mass**: 4.0 to Sable's physics (`D/physics_block_properties/compact_gyroscope.json:4`).

## What the Player Is Told

- Name "Compact Gyroscope" (`A/lang/en_us.json:372`).
- Tooltip: "A _Gyroscope_ squeezed into _one block_: a flywheel spinning in a _framed glass_ case that keeps _Aeronautics ships level_." / "When Spinning on a Ship: Pushes the ship back _upright_ and _damps_ its wobble … full strength at _256 RPM_. Takes a _shaft_ from above or below." / crafting and conversion / "When Powered by Redstone: _Brakes_ the flywheel … A _full_ signal stops it." (`en_us.json:373-377`, `:691-692`). Registered as a Create tooltip (`J/ComedyMonkeysCreateAddons.java:272`).
- Goggles: Create's kinetic stress lines (`CGBE:205`), then "Compact Gyroscope:", "Spin ……." with an 18-segment gold/yellow/green bar of strength (`:206-210`, `en_us.json:378-379`), a red "Braked by redstone: N%" or "Stopped by redstone" when signalled (`CGBE:211-215`, `en_us.json:689-690`), then either a dark grey "Only works on an Aeronautics ship" (`CGBE:216-219`, `en_us.json:516`) or "Correcting ." with a green/yellow/red effort bar and "Tilt: N.N°" (`CGBE:221-226`, `en_us.json:380`, `:517`).
- Ponder "Keeping Things Level" (`AS:33-95`, `en_us.json:514`, `:644-648`): a 3x2x3 ship drops in with the gyroscope at (2,2,2), "a whole Gyroscope Stabilizer, squeezed into one framed glass block" (pointing at its south face); a creative motor facing down drops on top (2,3,2) and spins it at 256 RPM, "Spin it with a shaft from above or below … up to 256 RPM"; the ship tips 20° about Z, "whenever something tips it over…", then rights itself with a small overshoot (−24°, +4°), "…it pushes back upright and calms the wobble"; finally the recipe text with the item shown over the motor. The Ponder structure is `A/ponder/compact_gyroscope.nbt` (gyroscope, creative motor `facing=down`, spruce planks, concrete).
- README: "a flywheel spinning inside a brass-framed glass case … Goggles show its spin, how hard it's correcting and the ship's tilt. A redstone signal brakes it … (the shaft keeps turning)" (`README.md:258`).
- Recipes: shaped `BSB / GFG / BPB` = 4 Brass Casing, a Shaft (top), 2 Framed Glass, a **Flywheel** (centre), a Precision Mechanism (bottom) (`D/recipe/compact_gyroscope_crafting.json:10-31`). With Gyroscope Stabilizers (`gyro`) also loaded: shapeless `gyro:gyroscope` → compact (`D/recipe/compact_gyroscope.json`) and back (`D/recipe/gyroscope_from_compact_gyroscope.json`).
- Advancement "Pocket-Sized Stability": "All of the balance, none of the floor space" (`en_us.json:492-493`, `D/advancement/compact_gyroscope.json`); it is the parent of the other two ship hardware advancements.
- The promise: a brass and glass instrument case with a flywheel spinning inside that holds the ship upright.

## States

| State | Source | How the player can tell today |
|---|---|---|
| Blockstate | none: one variant `""` (`A/blockstates/compact_gyroscope.json:3-5`); `KineticBlock` adds no property | Always the same model; no facing, no axis |
| Shaft turning / still | Create kinetic speed (synced by `KineticBlockEntity`) | The through-shaft turns (`AR:92-93`) |
| Flywheel speed (effective speed, 0 to shaft speed) | `effectiveSpeed`, run on both sides from synced speed and `Brake` (`CGBE:90-91`) | Flywheel spins inside the glass at that speed (`AR:97`); spin-up and spin-down are gradual |
| Strength 0–1 (speed / 256 RPM) | derived (`CGBE:128-130`) | Only as flywheel speed; goggles bar for the number |
| Redstone brake 0–15 | `Brake`, synced (`CGBE:233`, `:244`) | Partial: the flywheel turns slower than the shaft through it. Full: flywheel stopped, shaft still turning. No lamp or marking; goggles give the % |
| Overstressed | Create kinetic | Shaft and flywheel stop; Create's goggle lines |
| On a ship / not | `OnShip`, client packet only (`CGBE:235`, `:246`) | Nothing on the model; goggles only |
| Correcting effort 0–1 | `Effort`, client packet only (`:236`, `:247`) | Nothing on the model; goggles bar |
| Ship tilt (degrees) | `Tilt`, client packet only (`:237`, `:248`) | Nothing on the model (the block tilts with the ship, which shows it indirectly); goggles number |

## Moving and Changing Parts

| Part | Model file | Driven by | Axis / pivot / range | Source |
|---|---|---|---|---|
| Through-shaft | Create's own shaft block model: the baked model of `create:shaft` with `axis=y` (`AR:40`, `:58`), i.e. `create:block/shaft` (4x4 px, x/z 6–10, full height y 0–16, textures `create:block/axis`/`axis_top`) | `KineticBlockEntityRenderer.getAngleForBe` on Y (`AR:92`) | Spins about Y through the block centre at network speed, exactly like the shafts it meets (`AR:56-65`) | `AR:93` |
| Flywheel | Create's `create:block/flywheel/block` (`AR:35-36`; an OBJ, `flywheel_shaftless.obj`, texture `create:block/flywheel`) | `getFlywheelAngle(partialTicks)`: its own angle, advanced each client tick by effective RPM × 0.3° (`CGBE:92-96`), lerped between ticks and unwrapped across 360° (`CGBE:102-110`) | Spins about Y through the block centre; scaled 0.26 about the centre, "the 2.75-block flywheel to 12 pixels across, so it clears the 2-pixel frame" (`AR:41-42`, `:94-101`). Slips behind the shaft when braked | `AR:97` |

Both parts go into one `RenderType.solid()` buffer at the block's light (`AR:75-78`, `:91`). The static case (frame and glass) is the blockstate model. No lerping beyond `partialTicks` for the flywheel; the spin-up smoothing is the chase in step 2.

## Code Contract

**Files a new look must keep (or change the code with it)**
- Blockstate `A/blockstates/compact_gyroscope.json`: a single unrotated variant, model `block/compact_gyroscope`. No facing to design for: it must read the same from all four sides, with the shaft vertical.
- Block model `A/models/block/compact_gyroscope.json`: parent `minecraft:block/block`, `render_type` cutout, `ambientocclusion` false, textures `frame` = `create:block/brass_casing`, `glass` = `create:block/palettes/framed_glass` (`:2-9`). The gyroscope has **no textures of its own**. The static model holds only the case; the flywheel and the shaft are drawn by the renderer and must not be modelled into it.
- Item model `A/models/item/compact_gyroscope.json`: a separate full copy of the 16 case elements plus a hand-built stand-in flywheel (a 10x10 ring of 1 px bars at y 7–9 with a cross of spokes, #16–21 `:845-1185`, texture `create:block/flywheel`) and a 3x3 static axis through the full height (#22 `:1253`, `create:block/axis`); GUI display rotation 30/225/0, scale 0.625 (`:1303`). A new look needs this file rebuilt by hand too: no item renderer draws the real parts.
- Part models: none of ours. `FLYWHEEL` points at Create's model (`AR:35-36`, registered `AR:50-53`). A different rotor (a gimbal, a disc of our own) needs a new model file, a `registerModels` entry and a change to `AR:94-101`.

**Geometry the renderer assumes**
- The through-shaft is 4x4 at x/z 6–10, y 0–16: the top and bottom frame plates leave exactly that hole (#0–3 `:12-208`, #4–7 `:260-456`). A new top and bottom must keep a 4x4 opening there (or cover the shaft and accept that it's hidden).
- The flywheel is centred on the block centre at about 12 px across (scale 0.26), spinning about Y: the interior from about 2 to 14 px across at mid-height must stay clear and visible from the sides.
- Shape and collision: full cube (no `getShape` in `CGB`). The model should read as a full block.
- Properties: map colour terracotta yellow, strength 3.0/6.0, copper sound, needs a pickaxe, `noOcclusion`, not a redstone conductor, not suffocating, not view-blocking (`AC:28-36`). No light level; both parts use the block's light.
- Kinetic faces: up and down only. Redstone: any face. No item, fluid or energy ports, no filter slot, no value box, no scroll behaviour, no wrench action of its own.
- Physics: Sable mass 4.0 (`D/physics_block_properties/compact_gyroscope.json`); a heavier-looking design does not change this.
- Ponder: gyroscope at (2,2,2) inside a 3x2x3 section (1,1,1)–(3,2,3), motor on top at (2,3,2); arrows point at the gyroscope's south and west faces and the motor's top (`AS:40-91`). Nothing points at a model part, so the scene survives a new model.

**Borrowed (don't redesign, design around)**
- Create's shaft block model and Create's flywheel model, drawn live by the renderer. Create's brass casing and framed glass textures. Nothing here is from Create Aeronautics, Simulated or Gyroscope Stabilizers (code port only); Aeronautics and Simulated assets are All Rights Reserved, so nothing of theirs may be copied.

### Shared with the ship hardware set

| Shared piece | Where | What depends on it |
|---|---|---|
| Registration and Sable gate | `AC` (all three blocks, items, block entities, display sources, `AC:27-107`), `S/SableCompat.java` | Name-only; a model change doesn't touch it |
| `AeronauticsRenderers` | `AR`: the gyroscope and rotor renderers plus private helpers `renderShaft` (Create's shaft block model, used only by the gyroscope, `AR:56-65`), `rotationAbout`, `renderBaked`, `model` (`AR:67-82`); also registers the Flight Instruments renderer (`AR:47`) | Changing `renderBaked` (solid layer, block light) changes both the gyroscope and the rotor. The two renderer classes are separate inner classes and can be rewritten alone |
| `CobbleGeneratorRenderer.SHAFT` | drawn by the Torque Rotor (`AR:129`), **not** by the gyroscope | Frozen and hands-off (`design/hands-off.md:63`); the gyroscope uses Create's shaft instead |
| Texture `create:block/brass_casing` | gyroscope frame (`compact_gyroscope.json:6`), Torque Rotor collar (`A/models/block/torque_rotor.json:5`) | Create's; can only be swapped, not edited. No other block of ours uses it today |
| Ponder scenes file | `AS` holds all three scenes (`AS:24-31`); gyroscope and rotor are in the kinetic appliances tag, the instruments aren't (`CMPonderPlugin.java:119-122`) | Shared file; each scene's code is its own |
| Config section `aeronautics` | `CMConfig.java:367-408` (gyroscope and rotor only) | Not affected by a model change |
| Advancement tree | gyroscope is the parent of the rotor's and the instruments' (`D/advancement/torque_rotor.json:8`, `D/advancement/flight_instruments.json:8`) | Icons are the items, so they show the new item models |
| Recipe language | Precision Mechanism in all three; iron sheets in rotor and instruments; brass casing, framed glass, flywheel here | The materials a shared casing could draw on |

Nothing in `compat/sable/` or these assets is on `design/hands-off.md`. The one hands-off dependency in the set is the frozen `cobble_generator_shaft` stub the rotor draws.

## Current Look

Before sheet: `design/dossiers/aeronautics-set/compact-gyroscope-textures-before.png` (each texture nearest-neighbour scaled to 128, on dark grey), left to right: Create brass casing (frame), Create framed glass (panes), Create flywheel (32x32, the flywheel model's texture), Create axis and axis top (the through-shaft).

Geometry (16 elements, `compact_gyroscope.json`):
- Bottom plate y 0–2 in four pieces round a 4x4 shaft hole (#0–3 `:12-208`) and the same at the top, y 14–16 (#4–7 `:260-456`). Their tops show the whole brass casing face (dark wood planks inside a brass rim), cut by the hole; their sides show the casing's 2 px rim strip.
- Four 2x2 corner posts, y 2–14 (#8–11 `:508-664`), also the rim strip.
- Four zero-thickness framed-glass panes 0.5 px inside each side face, x/z 2–14, y 2–14, both faces drawn (#12–15 `:716-812`).
- Inside, from the renderer: Create's full shaft running top to bottom and a 12 px Create flywheel at mid-height.

Textures (measured): brass casing rim highlights `#ffeb8c`/`#f7cb6c`/`#e4b763` over brown `#9e6947`/`#724731`, dark wood centre `#3a2411`/`#301e0e`. Framed glass is clear with a 1 px pale cyan border `#d0eae9`/`#8bc1cd` and corner glints. The flywheel texture is saturated brass (`#ffd25d`, `#ffe97c`, `#cea05a`) and grey.

What works:
- Create's own material language: brass casing, framed glass, the real Create flywheel and shaft, so it reads as a Create part.
- The hero, the spinning flywheel, is visible from every side, and its motion is tied to the function: speed is strength, and the brake shows as slip between the wheel and the shaft.
- Simple, restrained geometry: no stripes, no clutter.

What doesn't (against `create-house-style.md`):
- Every edge is the brightest strip of the brass casing: 12 bars of saturated yellow rim (`#ffeb8c`) outline the block, so brass is the frame rather than a trim.
- The framed glass's near-white cyan border draws a second, pale frame inside the brass one on all four sides, the near-white line the style rules out.
- The flywheel is small (12 px) and thin at mid-height; behind a pale border and a full-height shaft it reads as a toy wheel, not a gyroscope. Its saturated brass adds more yellow.
- Nothing says "ship" or "level": no gimbal, no horizon, no up/down cue, no tie to the other two ship hardware blocks (survey: "They don't look alike today", `survey.md:41`).
- It is the same open "glass box with something inside" as the String Tangler and the generators, so it doesn't stand out as flight equipment.
- The item shows a different, square-ring flywheel and a 3x3 static axis, not what the block draws.

Survey: rank 14, total 9 (Gap 3, Presence 2, Fit 4, effort medium): "Brass casing, glass, a mini flywheel spinning"; hidden states "Levelling, brake clamping" (`survey.md:20`).

## Function the Look Doesn't Show

- **Whether it's working at all**: on a ship or not, it looks the same. Off a ship it spins and draws stress but does nothing.
- **Levelling**: how hard it's correcting (effort) and the ship's tilt are synced to the client (`CGBE:235-237`) but drawn nowhere.
- **The brake**: no lamp, lever socket or marking; partial braking is only a speed difference between the wheel and a shaft running through it, and a full stop looks like a broken or unpowered wheel with a turning shaft.
- **Full strength at 256 RPM**: nothing marks "up to speed"; slow and fast only differ in blur.
- **That it damps yaw too**: it resists every turn of the ship, including the turns a Torque Rotor makes, not just roll and pitch (`CGBE:170-172`).
- **Up and down only**: the shaft faces are the top and bottom; the sides look the same as any glass box face.

## Look the Code Doesn't Back Up

- **The flywheel as the working part**: the levelling is a control loop applied straight to the ship; the flywheel's only role is that its speed scales the gains (`CGBE:128-137`). The metaphor is fine, but nothing physical about the wheel (size, mass, orientation) matters.
- **The item's flywheel and axis**: a square ring with spokes and a 3x3 axis that the block never draws.
- **Brass casing tops with a "port" look on both ends**: fine for the shaft, but the casing's wood centre on the top reads as a lid you might open; there's no inventory.
- Little else: the case is plain, so it promises little the code doesn't do.

## History

- `6a38571` Initial Checkin: block, block entity, renderer, models, recipes, Ponder (the model files have no other commits).
- `933852d` Release 1.6.0: "Compact Gyroscope: a redstone signal brakes the flywheel, the stronger the slower, and full power stops it" (`CGBE` +39 lines, `AR` 3 lines: the flywheel got its own angle so it can slip behind the shaft).
- Tests pin the controller, not the look: `src/test/java/com/comedymonkey/createaddons/compat/sable/CompactGyroscopeBlockEntityTest.java` (damping clamp `:54-110`, tilt `:114-133`, goggle bar `:137-151`, strength vs RPM `:155-172`, flywheel angle lerp and wrap `:174-192`).
- Survey rank 14 (`survey.md:20`); "Ship hardware" family note (`survey.md:41`).
- No earlier brief, spec or plan in `design/briefs`, `design/specs` or `design/plans`.
