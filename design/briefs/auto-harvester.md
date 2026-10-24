# Design Brief: Auto Harvester

Redesign of an existing block. What the code does today is in `design/dossiers/auto-harvester.md`. Family brief shared with `design/briefs/animal-pen.md`.

## Hard Requirements

From the user:
- Scope: "Redesign + small code". New look and new part models. Small renderer changes are allowed where the design needs them; the shaft and the block's behaviour stay as they are.
- "Own textures": the harvester gets its own copies of every texture it shares today. The Cobble Generators and Stone Infuser must not change.
- Show the **stored harvest** on the block.
- Fix what the user dislikes: the "flat stand-in tools", the "near-white glass glints", the "dark, empty chamber".
- Dark, muted Create palette; light areas mid-grey at most; accents as trims, no hazard stripes (house style).
- Previewed in Blockbench before anything goes into the mod.
- Nothing on `design/hands-off.md` changes (the Trade Bot and the Cuckoo Clock).

From the code (non-negotiable in this scope unless listed under code changes in Builder Notes):
- `AutoHarvesterRenderer.java:31-36` loads six part models by name: `block/auto_harvester_carriage`, `_pole`, `_reel`, `_saw`, `_crop`, `_tree`. The names stay.
- `blockstates/auto_harvester.json`: `block/auto_harvester` (crop, farmland) and `block/auto_harvester_dirt` (none and tree, dirt). Both stay and stay identical apart from the soil.
- Everything is modelled with the window facing **north**; the renderer and blockstates rotate it (`AutoHarvesterRenderer.java:70-73`).
- The carriage only slides along X, +0 to +8 px from parked; it never drops or turns (`:119-121`).
- The pole is a 1-px-tall unit from y 0–1, stretched in Y from the hub up to the carriage's underside (`:122-128`). Any detail on it gets stretched.
- The reel and saw spin about the Z axis (out through the window) around a hub modelled at (8, 8) (`:131-138`). Reel 18°/tick, saw 45°/tick.
- The back shaft stub (`cobble_generator_shaft`, shared with the Cuckoo Clock, so frozen) sits at x6–10, y6–10, z12.5–16 in north-facing space. The back face needs a 4 × 4 px socket centred at (8, 8).
- Shaft into the **back** only; items in and out on **every** face (`AutoHarvesterBlock.java:58-62`, `ComedyMonkeysCreateAddons.java:137-138`). The look must not suggest only one face is a port.
- Plant volume stays clear of static geometry: x4.4–11.6, z4.4–11.6, y3 to about 12.6, soil top at y 3 (`AutoHarvesterRenderer.java:45`, `RealPlants.java:42`). Cut particles spawn in x4.8–11.2, y3.5–7.5, z4.8–11.2.
- Full-cube voxel shape, cutout render type (glass pixels fully clear or fully opaque), no light level.
- The item model (`models/item/auto_harvester.json`) is a separate static copy and must be rebuilt: head parked, a crop in place.

## Asset Type

Block with moving parts (block-entity renderer). 16 × 16 × 16 px, `java_block`. Static shell in the blockstate models; the carriage, pole, reel and saw as separate part models; a new item tray drawn by the renderer.

## Family

The **sealed process box**: the Auto Harvester and the Animal Pens are designed as a set (the Cobble Generators can adopt the shell later). Shared across the family:
- the same frame, glass, top, bottom and back construction and textures (copied per block, so nothing else changes);
- **the front tray**: a low walnut tray with an andesite lip just behind the glass, where whatever is waiting to be collected sits on show. The harvester shows its harvest there, the pen its eggs and drops;
- a lighter, panelled chamber instead of the near-black brick, so what's inside reads;
- muted glass: a few cool grey glints, never near-white.

Each block gets its own interior: the harvester a planter under a gantry, the pen a stall with a trough.

## Fantasy

A whole farm shrunk into one Create box: a tiny combine on an overhead gantry that drops, sweeps the planter with a turning reel or a buzzing saw, and parks, leaving the harvest piled in the tray at your feet.

## Hero Element

**The gantry head**: a little trolley on a girder rail, carrying a **real reel drum** (crops) or a **toothed saw blade** (trees) on a drop-down pole. It's the part that does the mechanic, and it's what a player watches through the glass. Today it is a casing cube and two flat sprites; the redesign makes it a small machine with depth.

## Theme

Create andesite machine, dark end of the range. Sits beside the Cobble Generators and Mechanical Harvester/Saw (its recipe).

| Part | Material | Colours |
|---|---|---|
| Frame, walls outside | dark blue-grey steel with andesite edge trim | `#2f343b` `#3c424a` `#4a5059`, trim `#6b6e6a` `#7c7f79` (never above `#8a8c86`) |
| Top | Create andesite casing (borrowed, as today) | — |
| Chamber walls | dark steel panels with seams, lighter than today's brick | `#3a3f46` `#454b53` `#4f555d` |
| Rail, carriage, pole | steel girder and trolley | `#2f343b` `#3c424a` `#5f656e` |
| Reel | walnut bats, steel tines, brass end hubs | `#4e3626` `#634532`, `#5f656e`, `#8f7638` `#b08f45` |
| Saw | today's `auto_harvester_saw` steel (already on style) | `#282a30`–`#7a8086` |
| Planter and tray | dark walnut boxes with andesite lips | `#3b2a1f` `#4e3626`, `#6b6e6a` |
| Soil | dry tilled soil (no water mechanic) and dirt | muted browns |
| Glass | clear with 4–6 cool grey glints | `#6f7f86` `#83939a` |

Gentle mottling on every surface (2–4 close shades). Brass appears only as the reel's end hubs and one small cap on the carriage.

## Shape Language

Squares dominate (an industrial box, a girder, a trolley), with **circles only in the tools**: the reel drum and the saw blade are the only round things, so the eye goes straight to them.

## Silhouette Description

From outside it's a cube; the silhouette that matters is what's framed in the window. Straight on: a dark steel frame around a lit-looking, panelled chamber; a girder across the top; the head hanging from it on the right; a green plant in a walnut planter in the middle; a low tray along the bottom edge. At 5 blocks it reads as "box with a machine and a plant in it", and when harvesting, "something big and round is rolling across the plant".

## Primary Forms

- The shell: 16 px cube, 12 × 12 window on the north face in a 2-px steel frame.
- The planter: a walnut box with an andesite lip, x3–13, z4–13, soil top at y 3.
- The girder rail: a steel I-beam spanning the chamber x2–14 at the top back.
- The front tray: x3–13, z1.2–3.8, bed at y 2.5.

## Secondary Forms

- The trolley (carriage): a small steel body under the girder on two wheels that sit on the rail's bottom flange, with a brass cap on its motor end. Parked within x2–6, underside at y 9.
- The pole: a square steel rod, plain along its length so stretching doesn't show.
- The reel: a drum along Z, about 5 px across and 6–7 px long (z ≈ 3–9.5): two brass end hubs, 4 walnut bats between them, each with a row of small steel tines.
- The saw: a 5-px toothed steel blade about 0.5 px thick on a short arbor with a hub nut.
- A gear housing on the back wall around the shaft stub, so the stub disappears into a real part.

## Interesting Features

- The reel actually reads as a reel: bats and tines rolling across the crop.
- The saw is a real disc with teeth, not a sprite.
- The harvest piles up in the front tray as the buffer fills.
- The side panels each carry a small square **access hatch** (a seam and a hinge pin): every face is an item port, and every side looks like one.
- The back loses Create's 4-way gearbox texture for a single centred shaft socket, because only the back takes a shaft.

## States

| State | What changes visually | Carried by |
|---|---|---|
| Empty (`plant=none`) | Bare dirt, no plant, reel parked | blockstate model, renderer |
| Growing | Plant grows; head parked on the right | renderer (existing) |
| Harvesting | Head plunges, sweeps right to left with the reel or saw turning, lifts and returns | renderer (existing path, new parts) |
| Crop vs tree | Farmland and reel vs dirt and saw | blockstate model, renderer |
| Stored harvest (`Output`) | 0–3 items lying in the front tray: 1 when there's anything, 2 from a third full, 3 from two thirds full | **renderer, new** |
| Full / blocked | Tray shows 3 items and the head stays parked: reads as "full, come and empty me" | renderer, new tray |
| Stopped | Everything freezes; the back shaft stops | existing |

## Rig Requirements

Part models, each modelled where the renderer expects it (north-facing):
- `auto_harvester_carriage`: modelled **parked**, centred on x 4, fitting x2–6, underside at y 9. Slides along X only.
- `auto_harvester_pole`: a 1-px-tall unit, y0–1, centred on the hub column x 4 and on the tool's axle in Z. Stretched in Y by code.
- `auto_harvester_reel`: modelled around the hub at **(8, 8)**, drum axis along Z. Spins about Z through (8, 8).
- `auto_harvester_saw`: same hub and axis.
- Leave `auto_harvester_crop` and `auto_harvester_tree` (fallback plants) as they are unless the palette check flags them.

## Fun Score (1-10)

8. A combine harvester in a jar: the player watches the reel roll across the wheat and the harvest drop into the tray, and the parked-and-full state tells them what to do.

## Builder Notes

**Start from scratch** on a copy of today's coordinates (the shell numbers are in the dossier). Export to the same file names; new textures under new names (`auto_harvester_frame`, `_chamber`, `_glass`, `_back`, `_bottom`, `_side`, `_tray`, `_planter`, `_reel`), so the Cobble Generators and Stone Infuser keep theirs.

**Code changes the design needs** (small, for the implementation stage):
1. **Bigger tool.** The reel and saw grow from 4 to about 5 px across. The renderer's envelope moves in to keep them off the walls and rim: proposed `PARK_X` 4 → 4.5, `SWEEP_END_X` 12 → 11.5, `CUT_Y` 5.5 → 6, `PARK_Y` 10.5 → 11 (`AutoHarvesterRenderer.java:39-42`). Check by posing park, both sweep ends and the cut in Blockbench before changing code.
2. **Tray items.** The renderer draws up to 3 items from the output buffer in the front tray (the first non-empty stacks), counted by how full the buffer is, at scale about 0.2 and tilted about 30° towards the window so they read from standing height. The pen uses the same rule, so make it one shared helper.
3. **Planter moves back** to z4–13 for the tray; the plant volume (x/z 4.4–11.6) is unchanged.

**Must not lose**: the head parking on the viewer's right and sweeping right to left; the stub's 4 × 4 socket; clear plant volume; full-cube read.

**Pitfalls**: Java block rotations are 22.5° steps on one axis, so the drum and blade are built from boxes at 0/22.5/45°. The pole stretches up to 3.5×, so keep its texture vertical-only. Glass is cutout: glints are opaque pixels, so keep them few and mid-grey.

**Screenshot for review**: straight on and three-quarter (crop and tree), a cutaway with the shell hidden, the head at park / plunge / mid-sweep / far end, the tray at 0–3 items, the item model, and each next to its "before" shot from `design/dossiers/auto-harvester/`.
