# Code Dossier: Magma Engine

What the code does today. The Supercharged Magma Engine is the same block class with bigger numbers: its own dossier (`supercharged-magma-engine.md`) lists only the differences. Before shots of our own model: `design/dossiers/magma-engine/` (built by `tools/gen-before.js` from `tools/scene-engines.json`). With Create Aeronautics installed (as in the dev run) the game shows Simulated's Portable Engine shape instead, which isn't ours to screenshot or ship (All Rights Reserved; only our texture is ours).

## Identity

- Block id `magma_engine`, class `content/engine/MagmaEngineBlock.java` (shared with `supercharged_magma_engine`, `MagmaEngineBlock(properties, supercharged)`, `ModBlocks.java:205-209`).
- Block entity `MagmaEngineBlockEntity` (`GeneratingKineticBlockEntity`), one type `ModBlockEntities.MAGMA_ENGINE` for both engines.
- Renderer `client/MagmaEngineRenderer.java`; Aeronautics shape swap `client/MagmaEngineSimModels.java`.
- Family: heat and lava (survey): Lava Generator, Geothermal Turbine, Incinerator, Pressure Crucible.

## What It Does

1. Holds lava: a 4,000 mB tank (`MagmaEngineBlock.java:75-77`), lava only (`MagmaEngineBlockEntity.java:50`).
2. While the tank isn't empty it burns 10 mB a second (config `lavaPerSecond`, `MagmaEngineBlockEntity.java:105-113`) and turns its shaft at 64 RPM (config `speed`) with 128 SU per RPM (config `capacityPerRpm`), 8,192 SU (`CMConfig.java:302-316`): four Portable Engines.
3. Empty, it stops at once (no run-down in the code; `getGeneratedSpeed` is 0 when not running, `:142-148`).
4. The shaft comes out of the middle of the front (`FACING`, horizontal only). Placed facing the player; sneak to face away (`MagmaEngineBlock.java:85-90`).
5. A value box sets the turning direction, clockwise or counter-clockwise, like Simulated's Portable Engine (`MagmaEngineBlockEntity.java:128-134`). It sits on the back of our model, on top of the body with Simulated's shape (`:154-174`).
6. Lava goes in by pipe through **the back or the bottom only** (`ComedyMonkeysCreateAddons.java:160-163`), or from a bucket on any face (`MagmaEngineBlock.java:113-122`).
7. Display Links read its lava level ("2,450 mB (61%)") and output ("64 RPM, 8,192 SU" or "Out of lava") (`MagmaEngineDisplaySources.java`).
8. Works on Aeronautics physics objects (README).

## What the Player Is Told

- Tooltip: "A heavy _lava-burning engine_ with the power of _four Portable Engines_" … "_Pipes_ fill it through the _back_ or the _bottom_, and it holds _4 buckets_" (`en_us.json:354-360`).
- Ponder "Lava-Powered Muscle": "four Portable Engines' worth of muscle, in a single block"; "Pipe lava into its back or its bottom"; "Hook it up to a Fluid Wellspring with Lava Upgrades and it runs forever" (`en_us.json:502-507`, `NewBlockScenes.java:103-155`).
- Advancement "Hot Rod": "Four Portable Engines walked into a bar. One engine walked out" (`en_us.json:488-489`).
- README calls it "a heavy brass engine that burns lava".
- The promise: a hot, heavy, muscular engine. A hot rod.

## States

| State | Code | How the player tells today |
|---|---|---|
| Off, no lava | `lit=false`, speed 0 | Dark vents; heads still |
| Running | `lit=true` (light 10), speed ±64 | Vents glow, embers at the sides, smoke from the heads, heads bob in turn with the shaft |
| Tank level | `tank` (synced) | Goggles and Display Links only; nothing on the model |
| Direction | value box | The value box and the shaft's turning |

## Moving and Changing Parts

- **Shaft stub**: the shared `CobbleGeneratorRenderer.SHAFT`, spun about the facing axis (`MagmaEngineRenderer.java:55-61`).
- **Three cylinder heads** (our model): `block/magma_engine_head` drawn three times, 4.5 px apart along X, each lifting up to 1 px, a third of a turn apart, once per shaft turn (`:68-82`). They stand above the block (render box raised 0.4 block, `MagmaEngineBlockEntity.java:88-91`).
- **Simulated's shape**: the lower hatch swings open while lit (`hatchOpen`, eased 0.08 a tick, `:98-102`; drawn `MagmaEngineRenderer.java:98-106`), the exhaust pipes and outlets pulse with the shaft and glow full-bright while lit (`:108-118`).
- Particles: smoke from the head bores (or Simulated's outlets), lava embers just outside both sides, vanilla `LAVA_AMBIENT` now and then (`MagmaEngineBlock.java:124-153`).

## Code Contract

- Blockstates `magma_engine.json`: `facing` (4) × `lit` → `block/magma_engine`, `block/magma_engine_lit`, rotated by facing (modelled with the shaft north).
- Renderer parts: `block/magma_engine_head` (and `supercharged_magma_engine_head`), registered in `ComedyMonkeysCreateAddonsClient.java:118-119`.
- Shaft: the shared `cobble_generator_shaft` stub at the front centre (x 6–10, y 6–10).
- Item model: `models/item/magma_engine.json`, a full model (not a parent).
- With Simulated loaded, `MagmaEngineSimModels` swaps every state's model for Simulated's geometry (`magma_engine_sim*.json`, parents in Simulated's jar) and the renderer draws Simulated's hatch and exhausts instead of our heads.
- Fluid capability: back or bottom (`ComedyMonkeysCreateAddons.java:160-163`).
- `noOcclusion` (the body sits inside the block) (`ModBlocks.java:211-220`); full-cube shape by default.
- Light 10 while lit.
- The value box position (`DirectionBox`): on the back face centre (8, 8, 16.05) for our model.
- Particle positions are hard-coded to the current heads and sides.

## Current Look

Our own model (before shots):
- A dark steel plinth, a heavily mottled grey body with a chamfered top, three square cylinder heads on top with glowing amber windows, a saturated gold cog round the shaft, small red lamps on the front corners, pipework on the sides.
- What works: three cylinders bobbing in turn reads as "engine", and the motion follows the shaft.
- What doesn't: the blotchy grey mottle (the "muddy" look the user rejected on the harvester); the saturated yellow gold cog and its spiky teeth (off the dark, muted style); nothing shows lava or heat apart from the head windows; the heads stand above the block, so there's nothing a pipe can meet at the top; the ports aren't shown anywhere.

Simulated's shape (in game with Aeronautics): their Portable Engine geometry in our dark steel texture with magma in the firebox, a swinging hatch and pulsing exhausts. Not our design and not ours to change.

## Function the Look Doesn't Show

- The tank level (4 or 8 buckets): goggles and Display Links only.
- Where pipes go in (today: back and bottom), and where they can't.
- The speed: both engines bob the same way, the Supercharged one just faster.
- The direction setting, beyond the value box.

## Look the Code Doesn't Back Up

- The red lamps on the front corners: nothing drives them.
- The side pipework suggests ports; pipes don't connect there.

## History

- `6a38571` initial check-in, `46bea31` added the Supercharged engine, `26adbd3` review fixes and unit tests.
- The survey ranked the engines 15th and 16th (score 9): low gain while Simulated's shape stands in for them in game.

## Changed by the redesign (2026-10-24)

- **Look**: a V-twin on a cradled drum, a flywheel, ports on every face but the front (`design/models/engines.gen.js`, exported by `engines.export.js`): new `magma_engine(_lit)` and `supercharged_magma_engine(_lit)` models (the Supercharged no longer a texture swap), item models with the moving parts, `magma_engine_*` and `supercharged_magma_engine_*` textures; the old heads, every `_sim` model and the old textures retired.
- **No Aeronautics shape**: `MagmaEngineSimModels` is gone; the engines look the same with or without Aeronautics.
- **Any facing**: `FACING` is now all six directions (`BlockStateProperties.FACING`), placed with the shaft towards the player (sneak for away); blockstates have 12 variants each. `MagmaEngineBlock.modelToWorld` turns the north-facing model for the renderer, the value box and the effects.
- **Pipes on every side but the front**: `MagmaEngineBlock.isInlet`, used by the fluid capability; pinned by `MagmaEngineBlockTest`.
- **Renderer**: shaft stub and flywheel turning together, two pistons (1 px stroke, half a turn apart), the Supercharged's blower pulley at twice the shaft's speed, all from the shaft angle. The render box is the block.
- **Value box**: on its seat on top of the drum at the back, facing up.
- **Effects and sounds**: `client/MagmaEngineSounds.java` (from `clientTicker`): a chug and a smoke puff from that cylinder's exhaust at the top of each piston stroke, the firebox and supercharger loops (quiet, 8 blocks), Minecraft's own lava pop, a loud start and sputter-out, cooling ticks. The old head and side particles and the vanilla `LAVA_AMBIENT` call are gone.
