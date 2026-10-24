# Code Dossier: Supercharged Magma Engine

The same block, block entity and renderer as the Magma Engine (`magma-engine.md`). Only the differences are listed here.

## Identity

- Block id `supercharged_magma_engine`: `MagmaEngineBlock(properties, true)` (`ModBlocks.java:208-209`).
- Crafted from a Precision Mechanism over a Magma Engine over Brass Casing (README).

## What It Does Differently

- Twice the engine: 128 RPM (config `superchargedMagmaEngine.speed`), 128 SU per RPM, so 16,384 SU, for 20 mB of lava a second (`CMConfig.java:318-332`).
- An 8,000 mB tank (`MagmaEngineBlock.java:75-77`).
- Everything else, placement, ports, value box, display sources and Ponder, is shared.

## What the Player Is Told

- "A _Magma Engine_ rebuilt with a _Precision Mechanism_: twice as fast and twice as strong" (`en_us.json:362`).
- Advancement "Twin Turbo": "Supercharge a Magma Engine. Twice the speed, twice the stress, twice the lava bill" (`en_us.json:490-491`).
- The promise: the same engine, tuned up. A supercharger, twin turbos, a precision mechanism.

## Code Contract Differences

- Blockstates `supercharged_magma_engine.json` → `block/supercharged_magma_engine(_lit)`, which today are texture swaps on the Magma Engine's geometry (parent `block/magma_engine`).
- Renderer head `block/supercharged_magma_engine_head` (a texture swap of the engine's head).
- Simulated's shape: `supercharged_magma_engine_sim*` in a brass-trimmed texture.

## Current Look

The Magma Engine in brass trim (before shots `supercharged-3q.png`, `supercharged-lit-3q.png`). Nothing about its shape says "twice the engine" or shows the Precision Mechanism it was built with; it bobs the same heads, only faster.
