# Code Dossier: Torque Rotor

What the code does today. The Torque Rotor is one of the three "ship hardware" blocks (with the Compact Gyroscope and Flight Instruments) that exist only when Sable / Create Aeronautics is installed. "Shared with the ship hardware set" lists what the three have in common.

Paths: `J/` = `src/main/java/com/comedymonkey/createaddons/`, `S/` = `J/compat/sable/`, `A/` = `src/main/resources/assets/comedymonkeysaddons/`, `D/` = `src/main/resources/data/comedymonkeysaddons/`. Short names: `TRB` = `S/TorqueRotorBlock.java`, `TRBE` = `S/TorqueRotorBlockEntity.java`, `AR` = `S/client/AeronauticsRenderers.java`, `AC` = `S/AeronauticsContent.java`, `AS` = `S/client/AeronauticsScenes.java`. Model elements are cited by index (`#n`) and the line their `"from"` is on.

## Identity

- Registry id `torque_rotor`. Single block, kinetic consumer, 1x1x1 with blades drawn 4 px past four sides.
- Block `TRB` (`DirectionalKineticBlock`, `IBE`), registered `AC:39-48`. Block entity `TRBE` (`KineticBlockEntity` + Sable's `BlockEntitySubLevelActor`, `TRBE:36`), type registered `AC:88-90`. Plain `BlockItem` `AC:80`.
- Renderer `AR.RotorRenderer` (`AR:106-137`), registered `AR:46`. Part model `ROTOR_BLADES` = `block/torque_rotor_blades` (`AR:38-39`, registered `AR:52`). No Flywheel visual, no menu.
- Only exists with Sable: `SableCompat.LOADED` (`S/SableCompat.java:10-11`) gates registration (`J/ComedyMonkeysCreateAddons.java:73-74`), stress and Create tooltip (`:270-273`), renderers and models (`J/client/ComedyMonkeysCreateAddonsClient.java:102-103`, `:121-122`), Ponder (`J/client/ponder/CMPonderPlugin.java:91-92`, kinetic appliances tag `:119-122`) and the creative tab (`J/registry/ModCreativeTabs.java:73-74`). Recipe and advancement have a `mod_loaded: sable` condition; the pickaxe tag entry is `required: false` (`src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json:16-19`).
- Family (survey): "Ship hardware" (`design/dossiers/survey.md:41`).

## What It Does

1. **Placing**: it mounts on the face you click, blades pointing away from it (`FACING` = clicked face, `TRB:36-39`), like an Aeronautics propeller (`TRB:16-19`). All six directions. Sneaking does nothing different (the override ignores it).
2. **Power**: a shaft only through the **base**, the face opposite the blades (`TRB:52-55`); rotation axis = the facing axis (`TRB:47-49`). In practice the block it's mounted on is its drive (the Ponder mounts it on a motor, `AS:134-148`). Stress 4 SU per RPM by default (`J/config/CMConfig.java:404-407`, registered `AC:104`).
3. **Thrust**: thrust = 1.0 × RPM, signed so positive "blows air out along the facing" (`TRBE:62-69`, `CMConfig.java:398-403`), the same per-RPM thrust as an Aeronautics Andesite Propeller (`TRBE:29-35`). Reversing the shaft reverses it.
4. **Turning (only on an Aeronautics ship, in Sable's physics step)** (`TRBE:72-87`): it works out the push a propeller there would give (against the airflow, `:81`), takes the lever arm from the ship's centre of mass to the block centre (`:83-84`), and applies only the turning that push causes (arm × push) as a torque impulse (`:85-86`). The push itself is never applied, so the ship turns without sliding sideways. Further from the centre of mass turns harder.
5. **Wind**: on the client, whenever it turns (on a ship or not), it puffs up to 5 particles a tick through a 0.75-block-radius disc around the axis, blown along the facing at a speed that grows with RPM (`TRBE:90-121`, `BLADE_RADIUS` `:40`). The particle is Aeronautics' own propeller airflow particle when that class exists (by reflection), else Create's air particle (`:124-134`).
6. **Off a ship** it spins, blows wind and draws stress, but turns nothing.

## What the Player Is Told

- Name "Torque Rotor" (`A/lang/en_us.json:381`).
- Tooltip: "A _propeller_ that _turns_ an _Aeronautics ship_ without pushing it: none of a tail propeller's _sideways drift_. The tail rotor your helicopter wanted." / "When Spinning on a Ship: Its blades push the way they _face_ (or pull, with the shaft reversed) … _not pushed_. Mount it _far from the middle_ … the further out, the harder it turns." / "When Placed: Mounts on the face you click, like a propeller. The _shaft_ goes in through its _base_." (`en_us.json:382-384`, `:483-484`). Registered as a Create tooltip (`J/ComedyMonkeysCreateAddons.java:273`).
- Goggles: Create's kinetic stress lines (`TRBE:138`), "Torque Rotor:", "Thrust: N" (|thrust| with thousands separators), aqua while turning, dark grey when still (`TRBE:139-143`, `en_us.json:385-386`). Nothing says whether it's on a ship.
- Ponder "Turn Without the Drift" (`AS:127-179`, `en_us.json:515`, `:653-657`): a small helicopter section (1,2,2)–(5,3,4) drops in, turning about the cabin end (2.5, 2.5, 3.5); the rotor at (5,3,3) facing south with its motor behind at (5,3,2) (`A/ponder/torque_rotor.nbt`: torque rotor `facing=south`, andesite casing, motors). "looks like a propeller, but it turns a ship instead of pushing it along"; 128 RPM, "mounts on the face you click, with the shaft going in through its base"; the heli yaws 35°, "swinging the ship round its centre of mass…", "…but without dragging the ship sideways. Just what a helicopter's tail needs"; −128 RPM, it yaws back, "Reverse the shaft to turn the other way. Mounted further from the middle, it turns harder".
- README: "a propeller that turns the ship instead of pushing it … Put it on the tail like a tail rotor, facing sideways … blows wind like a propeller" (`README.md:260`).
- Recipe: shaped `ICI / SPS / ICI` = 4 Iron Sheets (corners), 2 Andesite Casing, 2 Shafts, a Precision Mechanism (`D/recipe/torque_rotor.json:10-27`).
- Advancement "Spin Doctor": "Turn left, turn right, never drift sideways" (`en_us.json:494-495`; parent is the gyroscope's, `D/advancement/torque_rotor.json:8`).
- The promise: a tail rotor. It looks like a propeller on purpose and the text leans on that.

## States

| State | Source | How the player can tell today |
|---|---|---|
| `facing` up/down/north/south/east/west | blockstate (`DirectionalKineticBlock.FACING`), clicked face on placement (`TRB:36-39`) | The blades point that way and the base sits against the support |
| Turning / still | Create kinetic speed | Hub and blades spin; wind particles |
| Spin direction (sign of RPM) = push vs pull | Create kinetic speed | Only the spin direction and the wind's direction (`TRBE:102`, `:118-119`); the blades' pitch is fixed |
| Speed / thrust magnitude | derived (`TRBE:67-69`) | Spin rate and wind particle count and speed; goggles give the thrust number |
| Overstressed | Create kinetic | Stops; Create's goggle lines |
| On a ship / not | not tracked or synced at all (the physics hook just runs or doesn't) | Nothing; not even goggles |
| Lever arm (distance from the ship's centre of mass) | computed in physics (`TRBE:83-84`), never synced | Nothing |

## Moving and Changing Parts

| Part | Model file | Driven by | Axis / pivot / range | Source |
|---|---|---|---|---|
| Hub and four blades | `A/models/block/torque_rotor_blades.json` (`ROTOR_BLADES`), modelled pointing **up** | `KineticBlockEntityRenderer.getAngleForBe` on the facing axis (`AR:119`) | Spins about the world facing axis through the block centre (0.5, 0.5, 0.5), exactly as Create spins shafts (`AR:121-123`); then stood on the facing by the shortest-arc rotation from +Y (`AR:131-134`). Continuous, at shaft speed | `AR:121-135` |
| Shaft stub | `A/models/block/cobble_generator_shaft.json` via `CobbleGeneratorRenderer.SHAFT` (`J/client/CobbleGeneratorRenderer.java:39-40`), modelled going into the north face (x/y 6–10, z −0.02–3.5) | same angle | Same spin; turned so its north end goes into the **back** (base) face (`AR:124-130`). It ends up x/z 6–10 from the base face to 3.5 px in, entirely inside the base plate and collar, so it is never seen | `AR:129` |
| Wind | particles | RPM, client tick | Disc of radius 0.75 block round the axis, blown along the facing (`TRBE:97-121`) | `TRBE:93` |

Both parts go into one `RenderType.solid()` buffer at the block's light; the code notes that asking for a second render type would end the first mid-draw (`AR:117-118`). Render bounding box inflated 0.5 so the overhanging blades aren't culled (`TRBE:52-55`). The base plate and collar are the static blockstate model.

## Code Contract

**Files a new look must keep (or change the code with it)**
- Blockstate `A/blockstates/torque_rotor.json`: six variants of `block/torque_rotor`: `up` unrotated, `down` x180, `north` x90, `south` x90 y180, `east` x90 y90, `west` x90 y270 (`:3-28`). So the static model is built **facing up, base on the bottom**.
- Static model `A/models/block/torque_rotor.json`: parent `minecraft:block/block`; base plate 14x3x14 at y 0–3 in andesite casing (#0 `:13`), brass-casing collar 8x1x8 at y 3–4 (#1 `:82`). Texture keys `casing` `create:block/andesite_casing`, `collar` `create:block/brass_casing`, plus `hub`, `blade`, `tip` declared but unused here (`:3-10`).
- Part model `A/models/block/torque_rotor_blades.json` (the file name is the renderer's contract, `AR:38-39`): parent `minecraft:block/block`, AO off, modelled pointing up (`:1-11`). It must be built around the spin axis x = z = 8 (the renderer spins it about the block centre), and everything in it spins.
- Our only texture: `A/textures/block/torque_rotor_blade.png` (16x16), used only by the rotor's three model files.
- Item model `A/models/item/torque_rotor.json`: a full copy of base, collar, hub and blades (#0–12 `:13-884`), no shaft, with its own display transforms (GUI 30/225/0 at 0.45 and translated down, ground 0.25, fixed 0.5, hand views 0.375–0.4, `:961`), because the blades overhang the block. Rebuild by hand with the new look.

**Geometry the renderer and code assume**
- Blade reach: the blades end 12 px from the axis (x −4 to 20, `torque_rotor_blades.json` #3–10), and the wind disc uses the same 0.75-block radius (`TRBE:40`). A different span should keep these in step (or change `BLADE_RADIUS`). The render box allows 0.5 block of overhang on every side (`TRBE:52-55`); blades longer than 16 px from the axis would clip out when the block is off screen.
- Outline and collision: mounting plate and hub only, 14x14x10 from the base, by facing (`TRB:22-29`, `:42-44`; up = x/z 1–15, y 0–10). The blades never collide. A new base and hub should fill about that box; the nose cone (to y 11.5, #2 `:150`) already pokes 1.5 px past it.
- Shaft face: the base only. The frozen stub sits x/z 6–10 in the first 3.5 px behind the base face; today the base is solid there. A visible socket would have to leave that 4x4 x 3.5 px space open.
- Facing: all six; the base must work against walls, floors and ceilings, and the blades must read pointing away from the support.
- Properties: map colour stone, strength 2.5/6.0, metal sound, needs a pickaxe, `noOcclusion`, not a redstone conductor, not suffocating, not view-blocking (`AC:40-48`). No light level.
- No redstone input, no item/fluid/energy ports, no filter slot, no value box, no scroll behaviour. Wrench behaviour is Create's default for kinetic blocks (not overridden).
- Ponder: rotor at (5,3,3) facing south on a motor at (5,3,2); arrows point at the rotor's south face, the motor's top and the cabin (`AS:134-176`). Survives a new model.

**Borrowed (don't redesign, design around)**
- `CobbleGeneratorRenderer.SHAFT` / `cobble_generator_shaft.json`: frozen and hands-off (`design/hands-off.md:63`); also drawn by the generators, Auto Harvester, Animal Pens, Cuckoo Clock, String Tangler, Stone Infuser, Electric Motor and Magma Engines (`design/dossiers/cobble-generator.md:139`). A different stub needs a new model of ours and a change to `AR:129` (`AR` is not hands-off).
- Create's andesite casing, brass casing and brass block textures (andesite casing is also on the Auto Harvester, Animal Pen, Incinerator and hands-off Trade Bot models).
- Aeronautics' propeller airflow particle, by class name at runtime (`TRBE:127-128`); no Aeronautics model or texture is used. Aeronautics and Simulated assets are All Rights Reserved: their propeller models must not be copied, only matched in spirit.

### Shared with the ship hardware set

| Shared piece | Where | What depends on it |
|---|---|---|
| Registration and Sable gate | `AC:27-107`, `S/SableCompat.java` | Name-only |
| `AeronauticsRenderers` | `AR`: `RotorRenderer` and `GyroscopeRenderer` share `rotationAbout`, `renderBaked` (solid layer, block light) and `model` (`AR:67-82`); `renderShaft` (Create's shaft) is the gyroscope's only (`AR:56-65`); it also registers the instruments' renderer (`AR:47`) | `RotorRenderer` can be rewritten alone; changing `renderBaked` changes the gyroscope too |
| Texture `create:block/brass_casing` | rotor collar (`torque_rotor.json:5`), gyroscope frame (`A/models/block/compact_gyroscope.json:6`) | Create's; swap only |
| Ponder scenes file | `AS:24-31` (all three) | Shared file, separate methods |
| Config section `aeronautics` | `CMConfig.java:367-408` | Not affected by a model change |
| Advancement tree | child of the gyroscope's (`D/advancement/torque_rotor.json:8`) | Icon is the item |
| Recipe language | Precision Mechanism in all three; iron sheets here and in the instruments; andesite casing and shafts here | Materials a shared casing could draw on |

Nothing in `compat/sable/` or these assets is on `design/hands-off.md`; the frozen stub above is the only hands-off dependency.

## Current Look

Before sheet: `design/dossiers/aeronautics-set/torque-rotor-textures-before.png`, left to right: Create andesite casing (base), Create brass casing (collar), Create brass block (hub and tips), our `torque_rotor_blade`, Create axis and axis top (the hidden stub).

Geometry:
- Static (`torque_rotor.json`, 2 elements): a 14x14x3 andesite-casing slab (#0) whose top shows the casing's wood planks inside a grey stone rim, and an 8x8x1 brass-casing collar (#1) showing the casing's dark wood.
- Spinning (`torque_rotor_blades.json`, 11 elements): a stepped brass-block hub, 6x5x6 then 4x1.5x4 then a 2x1x2 nose (#0–2 `:14-150`, y 4–11.5); four blades 9x1x3 px from the hub out to 12 px from the axis at y 6.5–7.5, each twisted 22.5° about its own length for pitch (#3, #5, #7, #9 `:218-680`); a 2x1x3 brass-block tip on each end (#4, #6, #8, #10).

Textures (measured): the blade texture is grey stone `#76726f` with a pale top `#c8c3bd`/`#aaa5a0` and darker bottom rows; the blades sample its top three rows (v 0–3), so they read pale. Brass block is saturated `#d7aa5e`/`#e4b763` with `#ffeb8c` highlights. Andesite casing: brown wood `#553a1f`–`#82613a` inside a grey rim `#505351`–`#9aa49d`.

What works:
- Reads instantly as a propeller, which is the point (Ponder text 1), and mounts like an Aeronautics propeller on any face.
- Motion is tied to the shaft, and the wind follows the spin direction and speed.
- Small, simple, on Create materials.

What doesn't (against `create-house-style.md`):
- Whole surfaces of saturated brass block on the hub and all four tips: brass as a fill, not a trim.
- The blades are pale stone (near-white top rows), an odd material for blades and the lightest thing on the block.
- The base is a flat generic slab; the support it mounts on doesn't look like anything the shaft goes into, and the shaft itself is never visible.
- Nothing distinguishes it from a pushing propeller, though its whole purpose is "turns, doesn't push".
- No link to the gyroscope or instruments: different materials (andesite and stone against brass and glass, against dark steel).

Survey: rank 18, total 8 (Gap 3, Presence 2, Fit 3, effort small–medium): "Andesite base, pitched blades in pale near-white stone, solid brass-block tips"; "Thrust is shown only by spin direction" (`survey.md:24`).

## Function the Look Doesn't Show

- **That it turns rather than pushes**: the defining behaviour has no visual cue at all.
- **Thrust strength**: only spin rate; the number is in goggles only (`TRBE:140-143`).
- **Push vs pull**: only the spin direction and the wind; the blades' fixed pitch doesn't change.
- **On a ship or not**: identical, and not even the goggles say so; off a ship it spins and blows wind but does nothing.
- **Lever arm**: "further from the middle turns harder" is invisible; there is no hint of where the turn goes.
- **Shaft input**: the stub is buried in the base, so you can't see that the base is the drive face, or that the block it's mounted on must be the drive.

## Look the Code Doesn't Back Up

- **A propeller that pushes**: blades, pitch and wind all say "pushes along its axis"; the code never pushes (intentional, and the Ponder says so, but the look gives no counter-cue).
- **Blade pitch**: the 22.5° twist suggests a direction of thrust set by the blades; in code the direction comes only from the RPM sign and facing (`TRBE:62-69`), so the pitch is decoration.
- **Solid base where the shaft goes in**: the shaft is said to enter "through its base", but the base is a solid slab with no socket.
- **Unused texture keys** in the static model (`hub`, `blade`, `tip`, `torque_rotor.json:6-8`): harmless, but the static model declares parts it doesn't have.

## History

- `6a38571` Initial Checkin: block, block entity, renderer, models, texture, recipe, Ponder; no later commits to the rotor's files (`AR` changed in `933852d` for the gyroscope brake only).
- No tests for the rotor (the `compat/sable` tests cover the gyroscope and instruments only).
- Survey rank 18 (`survey.md:24`); "Ship hardware" family note (`survey.md:41`).
- No earlier brief, spec or plan in `design/briefs`, `design/specs` or `design/plans`.
