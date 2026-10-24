# Design Brief: Magma Engine

Redesign of an existing block. What the code does today is in `design/dossiers/magma-engine.md`. Family: the Supercharged Magma Engine (`design/briefs/supercharged-magma-engine.md`), and the heat-and-lava blocks just redesigned: the Lava Generator and the Geothermal Turbine.

## Hard Requirements

From the user:
- "Something that fits my style, is dynamic, fun, fits the purpose."
- "Can be connected to pipez on any side but the output": lava pipes on the back, both sides, the top and the bottom; never the front, where the shaft is. Ports sit at the **middle of each face (8, 8)**, where Pipez cables and fluid pipes attach (as learned on the Lava Generator).
- Scope: "Redesign + small code". New look and renderer motion; numbers and behaviour unchanged.
- **New design everywhere**: drop the Simulated (Aeronautics) Portable Engine shape swap, so the engine always looks like this, with or without Aeronautics.
- Show: **pistons and a flywheel** moving with the shaft, and **heat and exhaust**.
- **Any facing**, shaft up and down included: "The motor should be able to be placed in all directions the previous one only being flat was annoying" (said while it went in). Pipes still connect on every side but the front.
- The house style: dark muted Create palette, crisp bevelled panels (no blotchy mottle), accents as trims, never large saturated yellow. Previewed in Blockbench first.
- Nothing on `design/hands-off.md` changes. In shared files (`ComedyMonkeysCreateAddons.java`, the client setup, lang, the Ponder file) only the engines' lines.
- A sound pass once the model is approved, if wanted, as for the generator and motor.

From the code:
- Blockstates load `block/magma_engine` (`lit=false`) and `block/magma_engine_lit` (`lit=true`), y-rotated by horizontal `facing`, modelled with the **shaft to the north** (`blockstates/magma_engine.json`). Both models keep identical geometry; only lit parts change.
- The shaft is the shared `cobble_generator_shaft` stub at the front centre (x 6–10, y 6–10), drawn by `MagmaEngineRenderer` spinning about the facing axis (`MagmaEngineRenderer.java:55-61`). Anything round it clears a 2.83 px corner sweep.
- The renderer draws the moving parts from the shaft's angle (`KineticBlockEntityRenderer.getAngleForBe`), so motion speeds up and stops with the shaft (`:50`). New parts replace `block/magma_engine_head`.
- Lava only, 4,000 mB; a bucket works on any face (`MagmaEngineBlock.java:113-122`).
- A value box sets the turning direction (`MagmaEngineBlockEntity.java:128-174`); today on the back centre, which becomes a port, so it moves (code change).
- Light 10 while lit; cutout; `noOcclusion`; full-cube shape. Everything now stays inside the block (the old heads stood 0.4 above it).
- Display sources and goggles unchanged.

## Asset Type

Block, 16 × 16 × 16 px, `java_block`, horizontal facing. Static model in two states plus renderer-drawn moving parts.

## Family

The heat-and-lava set the Lava Generator and Geothermal Turbine established: crisp blue-grey steel with andesite trim, round masses built from staves, copper for lava ports and pipes, muted amber heat behind dark grates, brass only on bezels and trims. The engines are the **muscle** of the family: heavier, more mechanical, and the only ones whose insides you can see working. The Supercharged engine is this one with a supercharger bolted on.

## Fantasy

A hot rod engine in a box: a V-twin that thumps away on lava, pistons pumping, flywheel spinning, exhausts coughing little puffs of smoke in time.

## Hero Element

**The V-twin**: two finned cylinders standing out at 45° to the upper left and upper right, each with an exposed piston rod that slides in and out of its cylinder as the crank turns, one after the other. The **spoked flywheel** turning on the shaft at the front is the second read. Together they make the engine readable as "running, and how fast" from across a room.

## Theme

Steel `#2f343b` `#3c424a` `#4a5059`, andesite trim `#6b6e6a` `#7c7f79`, light steel for the polished piston rods and the flywheel rim `#5f656e`. Copper `#7a4a2e` `#9c5f3a` for the lava ports. Dark aged brass `#8f7638` `#b08f45` for a few bezels and bolts only. Heat: muted amber `#c76a2a` `#d99a4a` behind dark grates, exhaust mouths glowing dark orange. No saturated gold, no blotchy mottle, no stripes.

## Shape Language

A V on a box: a squat crankcase with two cylinders leaning out at 45°, like a motorcycle V-twin seen from the front, with a round flywheel in front of it. Rounded masses from staves; crisp chamfers.

## Silhouette Description

From the front: a squat crankcase at the bottom, a round spoked flywheel on the shaft in the middle, and two finned cylinders leaning out to the top corners in a V, each topped by a short exhaust stack. The lava filler sits on top between the V. From the side: the crankcase, a cylinder rising at 45° towards the viewer's side, the round flywheel edge at the front, a copper port flange in the middle of the side. Reads as "engine" at a glance.

## Primary Forms

- **Crankcase**: x 1–15, y 1–9, z 2–15, steel panels, a plinth band at the base. The crank axis runs front to back through (8, 7).
- **Cylinders** (two, rotated ±45° about Z, about the crank axis): finned barrels about 5 px across, from about 4.5 to 9 px out along their axis, ending in a cylinder head near the upper corners (head centres about (2.5, 12.5) and (13.5, 12.5)), all inside the block.
- **Flywheel**: a spoked disc about 11 px across (radius 5.5) on the shaft at the front, z 0.5–2, turning with the shaft.

## Secondary Forms

- **Pistons**: polished rods between the crankcase and each cylinder, sliding along the cylinder's axis by about 1.5 px, half a turn apart (a 90° V fires unevenly; the two sit 180° apart on the crank so they visibly alternate).
- **Exhaust stacks**: one short stack on each cylinder head, mouth glowing dark orange while lit, a puff of smoke from each as its piston comes in.
- **Firebox grate**: a dark grate low on the front under the flywheel, glowing amber while lit.
- **Lava ports**: copper flanges with a dark bore, centred on the back, both sides, the top (between the V, as a filler neck) and the bottom.
- **Value box seat**: a small andesite plate above the back port, where the direction value box now sits.

## Interesting Features

- Everything that moves is tied to the shaft: stop the lava and the pistons, flywheel and puffs all stop together.
- The ports are obvious and everywhere but the front, exactly matching where pipes connect.
- The V shape and the puffing stacks make it the most alive block in the heat family.

## States

| State | What changes | Carried by |
|---|---|---|
| Off (`lit=false`) | Grate dark, exhaust mouths dark, pistons and flywheel still | `magma_engine` model; renderer parts at rest |
| Running (`lit=true`) | Grate glows amber, exhaust mouths glow (full-bright), pistons pump and flywheel spins at the shaft speed, smoke puffs from the stacks in time with the pistons | `magma_engine_lit` (`neoforge_data`), renderer, particles |
| Direction | Flywheel and pistons run the other way | renderer (from the shaft angle) |

## Rig Requirements

Renderer parts, modelled in the north-facing frame:
- `magma_engine_flywheel`: spins about the shaft axis through (8, 8).
- `magma_engine_piston_left`, `magma_engine_piston_right`: slide along their cylinder's 45° axis by up to 1.5 px, 180° apart on the crank angle.
- The old `magma_engine_head` part goes.

## Sound

A sound pass follows once the model is approved, if the user wants one: a thumping beat in time with the pistons, exhaust pops, a lava firebox bed; quiet while running (it runs 24/7), loud starting and stopping, as on the generator and motor.

## Fun Score (1-10)

9. A little V-twin hot rod that thumps and puffs away on lava. You can see how hard it's working, and where to plug it in.

## Builder Notes

**Start from scratch** on the family kit (`process-box.lib.js`, the `drumZ`, `disc` and stave helpers from `electric.gen.js`). One generator for both engines (`engines.gen.js`), the Supercharged one adding its supercharger. Export both state models from one part list, with the lit parts flagged full-bright, plus the renderer parts and a full item model. New textures `magma_engine_*` (the old `magma_engine_*` textures are replaced where the names match, and the unused ones removed).

**Code changes** (small):
1. Fluid capability on every side but the front (`side != FACING`), plus the null side (`ComedyMonkeysCreateAddons.java:160-163`).
2. Drop the Simulated shape: `MagmaEngineSimModels`, the renderer's Simulated branch, the `_sim` models and textures, the Simulated particle positions and value box position.
3. Renderer: the flywheel and the two pistons from the shaft angle, replacing the bobbing heads; the render box back to the block.
4. Particles: smoke puffs from each exhaust stack as its piston comes in, embers from the grate; replacing today's head and side particles.
5. Value box on a seat on top of the drum at the back (moved there from above the back port, where a pipe on the back would crowd it).
6. Text: tooltip, Ponder and README say pipes go in on any side but the front.

**Must not lose**: the shaft at the front centre, identical geometry in both state models, everything inside the block, a port flange at the middle of the back, sides, top and bottom, no z-fighting (overlays 0.25 px apart), cell-relative UVs, no exposed missing faces.

**Screenshot for review**: front, three-quarter front and back, both sides, top; off vs running; the pistons at both ends of their stroke; with the Supercharged engine and the Lava Generator for the family read; beside the before shots.
