# Design Brief: Stone Infuser

Redesign of an existing block. What the code does today is in `design/dossiers/stone-infuser.md` (its own parts) and
`design/dossiers/string-tangler.md` (what it shares). Family: the String Tangler (`design/briefs/string-tangler.md`),
designed as a pair; read that brief first, this one only adds what's different.

## Hard Requirements

From the user:
- Redesigned **as a pair with the String Tangler**, at the same scope: **Redesign + small code**, showing what it's
  doing (the second input, the infusing itself and the output).
- The house style (as the tangler); square box; previewed in Blockbench first; nothing on `design/hands-off.md`
  changes; the shared stub stays.

From the code:
- Shared with the tangler: block class, block entity and synced fields, `faceFront`/`shaft`/`quad`/`cube`, the back
  socket (z 14-16, the 4 x 4 hole at x/y 6-10), window north and shaft south. The `Infuser` renderer class can be
  rewritten alone.
- Recipes: 1 cobblestone + 9 nuggets -> zinc: asurine, iron: crimsite, gold: ochrum, copper: veridium; 8 SU/RPM;
  10 s per stone at 16 RPM, 0.625 s at 256 RPM.
- The renderer can read slot 0 (cobble), slot 1 (nuggets: which metal and how many), slot 2 (stone, 0-64),
  `processing`, `getProgress`, `getMaking` (the result), the shaft angle.
- Today the stone sits on a stand at y 3.5 and its 8 px cube turns in a cylinder of radius 5.8 px, which clips the
  shaft stub at the back (z 12.5): the new layout must clear the stub.
- Full-cube shape; unlike the tangler it suffocates (block property, unchanged).
- `cobble_generator_glass.png` is shared with every cobble generator (also being redesigned): the pair and the
  generators get one glass between them.

## Family

As the tangler: the same steel shell, front window, output tray and back socket; **andesite** trim (from its
Andesite Casing), where the tangler has copper. The tangler's two inputs are spools at the sides; the infuser's are
the stone in the middle and the nugget feeder at the side.

## Fantasy

A turntable in a sealed box: a cobblestone turns on it while a feeder sprinkles metal dust over it, and the new ore
stone creeps up from the bottom in the metal's colour until a block of asurine, crimsite, ochrum or veridium drops into
the tray.

## Hero Element

**The turntable and the stone on it**, turning with the shaft (a small bevel drive from the back socket), with the
new stone's band climbing it.

## Primary Forms

- Shell, window, back socket and tray as the tangler, in andesite trim.
- A round steel **turntable** on the chamber floor, its top at y 3.5, with a bevel gear under it meshed to a short
  drive from the back socket.
- A **nugget feeder** on the right-hand wall (viewed from the front): a small glass hopper showing the loaded nuggets
  (the real item, piled by count), with a spout over the stone.

## Interesting Features

- The stone turns **with the shaft** (geared down), not on a clock: faster rotation visibly turns it faster.
- While infusing, the spout sprinkles a fine dust of the metal's colour (particles) onto the stone.
- The stone is slightly smaller (7 px) and set a little forward so it clears the stub as it turns.
- The tray shows up to three finished stones, so the output is visible even while the next job runs.

## States

| State | What changes | Carried by |
|---|---|---|
| Empty | bare turntable, empty feeder, empty tray | renderer |
| Cobble loaded | the cobble on the turntable | renderer (slot 0) |
| Nuggets loaded | the feeder shows the nuggets, its pile by count | renderer (slot 1) |
| Working | the turntable and stone turn with the shaft; the dust sprinkles; the new stone's band climbs with progress | renderer, client particles |
| Stopped | still turntable, band part-grown, no dust | renderer |
| Output | 0-3 finished stones in the tray | TrayItems (slot 2) |

## Rig Requirements

New renderer-drawn parts (standalone models registered like `SHAFT`): `stone_infuser_turntable` (with its bevel
gear; spins about the vertical axis through x 8, z 7.5 at the shaft's angle times a gear ratio), `stone_infuser_drive`
(the short horizontal bevel shaft from the socket, spinning with the shaft). The stone, the band, the nuggets and the
tray are drawn in Java.

## Fun Score (1-10)

8. The metal visibly going into the stone, at the speed you give it.

## Builder Notes

- Built in the same generator as the tangler (`design/models/two-input-machines.gen.js`).
- Code (small): rewrite `MachineRenderers.Infuser` (turntable and drive from the shaft angle, the smaller forward
  stone and band, the feeder's nuggets, the dust particles on the client tick, the tray); register the part models;
  fix the band's top face z-fight at 100%. No blockstate or behaviour changes.
- Update the tooltip, so it describes what you can watch, and the item model (cobble below, the new stone climbing).
- Before: `design/dossiers/string-tangler/infuser-textures-before.png`.
