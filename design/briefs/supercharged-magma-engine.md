# Design Brief: Supercharged Magma Engine

Redesign of an existing block. What the code does today is in `design/dossiers/supercharged-magma-engine.md` (differences) and `design/dossiers/magma-engine.md`. This brief adds to `design/briefs/magma-engine.md`; everything there applies unless changed here.

## Hard Requirements

From the user:
- Everything in the Magma Engine brief's user requirements (style, dynamic and fun, pipes on any side but the output, new design everywhere, pistons and flywheel, heat and exhaust).
- **"Add a supercharger"**: the same engine with a belt-driven supercharger that spins visibly, and the brass tier trim. It should read as "twice the engine".

From the code:
- Blockstates load `block/supercharged_magma_engine` and `block/supercharged_magma_engine_lit` (`blockstates/supercharged_magma_engine.json`); today they're texture swaps of the Magma Engine's geometry, now their own models with the supercharger added.
- 128 RPM (twice the engine), so everything driven by the shaft moves twice as fast.
- 8,000 mB tank, 20 mB a second; same ports, value box and placement.

## Asset Type

As the Magma Engine.

## Family

The Magma Engine with a supercharger bolted on top of the front, brass trim where the engine has andesite, and a precision feel: what a Precision Mechanism buys you.

## Fantasy

The hot rod with a blower: the same V-twin, now with a supercharger whining away on the front, its pulley spinning faster than the flywheel.

## Hero Element

**The supercharger**: a small finned blower housing on the front, above the flywheel and between the cylinders, driven by a belt from a pulley on the shaft. Its pulley spins visibly faster than the flywheel. The V-twin stays the body; the blower is what says "supercharged".

## Theme

As the Magma Engine, with dark aged brass `#8f7638` `#b08f45` on the trims the engine has in andesite (the plinth band, the cylinder head caps, the flywheel rim's bezel, the port bolts), and a brass blower housing band. Still restrained: trims, not surfaces.

## Shape Language

The V on a box, with a small rounded blower nestled in the V at the front and a straight belt dropping to the shaft.

## Silhouette Description

From the front: the Magma Engine's silhouette, with a rounded blower filling the top of the V in front of the filler neck, its pulley facing out and a belt running down to a pulley on the shaft in front of the flywheel. From the side and back: the Magma Engine with brass trim.

## Primary Forms

As the Magma Engine, plus:
- **Supercharger blower**: a rounded housing about x 5–11, y 11.5–15.5, z 0.5–4.5, finned, with a brass band.

## Secondary Forms

- **Blower pulley**: on the front of the blower, spinning with the shaft at about twice the flywheel's speed.
- **Drive pulley**: on the shaft, in front of the flywheel, spinning with it.
- **Belt**: two straight runs between the pulleys (static).
- Brass trims as listed under Theme.

## Interesting Features

- Two speeds you can see: flywheel and blower, the blower faster.
- Next to a plain engine it reads instantly as the upgrade.

## States

As the Magma Engine; the blower pulley spins while the shaft turns and stops with it.

## Rig Requirements

As the Magma Engine (`supercharged_magma_engine_flywheel`, `supercharged_magma_engine_piston_left`, `_right`, or shared parts if the trim is identical), plus:
- `supercharged_magma_engine_blower_pulley`: spins about its own axis (front to back) at twice the shaft angle.
- `supercharged_magma_engine_drive_pulley`: spins with the shaft (may be part of the flywheel part).

## Sound

As the Magma Engine, plus a supercharger whine that rises with it.

## Fun Score (1-10)

9. The same lovable engine with a blower: the upgrade you can see and hear.

## Builder Notes

Built in the same generator as the Magma Engine (`engines.gen.js`), sharing every part list and adding the supercharger and brass trims. The top filler port stays reachable behind the blower (the blower sits at the front of the V, z 0.5–4.5; the filler is centred on the top face). New textures `supercharged_magma_engine_*` only where they differ (brass trims); otherwise it uses the engine's.

**Code changes**: as the Magma Engine, plus the blower pulley in the renderer.

**Must not lose**: as the Magma Engine, and the top port clear of the blower.

**Screenshot for review**: as the Magma Engine, plus the blower close up and both engines side by side.
