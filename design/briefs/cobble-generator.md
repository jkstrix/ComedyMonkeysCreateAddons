# Design Brief: Cobble Generators

Redesign of existing blocks: all 14 ids (cobblestone, basalt, blackstone and tuff; andesite, brass and the wireless
versions). What the code does today is in `design/dossiers/cobble-generator.md`. Family: the sealed process boxes
(Auto Harvester and Animal Pens, finished), and the two-input machines being redesigned alongside (String Tangler,
Stone Infuser).

## Hard Requirements

From the user:
- Scope: **Redesign + small code**. A new casing in the house style that keeps the good insides, plus code to show:
  - **a drill striking**, moving with each cycle in time with the product block forming;
  - **running or stopped** at a glance: running, full, switched off by redstone, no rotation;
  - **Brass makes 16**: the brass tier visibly makes a batch, not the same single block as andesite.
- The house style: dark muted Create palette, crisp bevelled steel panels (not muddy mottle), accents as trims, the
  brass tier as dark aged brass with brighter brass only on rivets and lips, never near-white, never large saturated
  areas. **Square** (it's a box, not a motor). Previewed in Blockbench first.
- Nothing on `design/hands-off.md` changes. In particular `cobble_generator_shaft.json` and
  `CobbleGeneratorRenderer.renderShaft` stay exactly as they are, and the brass generator's block properties (copied by
  the hands-off Brass Animal Pen) don't change.

From the code (`design/dossiers/cobble-generator.md`):
- 14 ids, one block class and block entity; blockstates `facing` (4, horizontal) x `powered`; wireless ids swap to a
  `_powered` model. All model and texture names as listed in the dossier's Code Contract, or the code changes with them.
- Modelled with the **window north**, the shaft at the **back (south)** only (`CobbleGeneratorBlock.java:79-87`).
- Interior geometry the renderer draws (`CobbleGeneratorRenderer.java:81-108`): floor top **y 2**; lava pool x 2-6,
  z 2-13; water pool x 10-14 / blue ice x 10-14, y 2-6, z 4-9; the product block x 6.5-9.5, z 5-8, growing y 2-5;
  pour columns at x 3.5-4.5 and x 11.5-12.5, z 6.5-7.5, from y 3 up to **y 10**. Steam at about (8, 5.1, 6.4).
- The back socket leaves x/y 6-10 clear from z 12.5 to 16 for the shared stub.
- Wireless: the two frequency pads on the front half of the top at centres (5.5, 5.5) and (10.5, 5.5), y 16-17
  (`TopFrequencySlot.java:26-40`); the antenna anywhere.
- Full-cube shape, `noOcclusion`, light 7, cutout. Brass properties unchanged (the pen copies them).
- Ponder and lang promise "the shaft hole in the back", "ice-blue trim" (basalt), "all black and gold" (blackstone),
  "a bed of gravel" (tuff) and "the window shows it working": keep them true, or change the text with the look.
- `cobble_generator_glass.png` is also the Stone Infuser's glass: if it changes, the infuser changes with it (it's
  being redesigned too, so that's fine as long as both are previewed).

## Asset Type

Block, 16 px, `java_block`, four horizontal facings; 14 ids; static models (21 with the powered wireless ones) plus
renderer-drawn interior, drill and lamp.

## Family

- **Process boxes** (Auto Harvester, Animal Pens): the same steel shell language: a near-flat steel field with crisp
  bevels, a raised window frame on the front, the flanged shaft socket on the back, corner rivets in the trim metal.
  Andesite trim for the andesite tier, dark aged brass for brass, exactly as the pens' two tiers.
- **Two-input machines** (String Tangler, Stone Infuser): the same shell, window and back socket, so the four
  "box" machines look like one workshop.
- Glows follow the turbine: cyan for "working", dark red for "stopped", muted amber for heat.

## Fantasy

A drill rig in a box: lava and water pour inside a sealed chamber, a Create drill drives down on the stone as it
forms and bites it free, and the block drops into the store. You can see it working, and see at a glance when it
isn't.

## Hero Element

**The drill.** A Create-style drill head hanging from the ceiling over the forming block: it spins with the shaft and
jabs down on every drill hit, tracking the block as it rises, then lifts as the finished block drops away. Brass gets a
**wider four-bit head over a 2 x 2 batch**.

## Theme

Create's Mechanical Drill and its casings; the process-box steel shell; vanilla's lava-meets-water generator,
contained.

## Shape Language

Square box, bevelled steel panels, a raised window frame; round only for the drill head, the nozzles and the lamp.

## Silhouette Description

A clean steel cube with a deep framed window on the front showing the glowing chamber, a status lamp on the front
frame, the flanged socket on the back. Wireless: the frequency pads and the antenna on top, as now.

## Primary Forms

- Shell: steel field panels (sides, top, bottom) with a 1 px trim ring and bevels, as the pens; the back is the pens'
  flanged socket around the shared stub.
- Front: a raised 2 px frame round the 12 x 12 window (keeping the window size), the glass with grey glints, never
  near-white.
- Inside (kept, refined): the dark chamber, the per-product materials (smooth basalt and soul soil, blackstone
  bricks, tuff bricks over gravel), the lava and water pools or blue ice, the pours.

## Secondary Forms

- **Nozzles**: short steel nozzles in the ceiling over each pour (the lava and water are sealed in; no inlet pipes
  from outside, nothing on top that looks like a fill port). Basalt and blackstone lose the dead water nozzle.
- **Drill mount**: a steel bracket in the ceiling at the centre that the drill hangs from.
- **Product trim** (kept small): a thin trim line on the front frame in the product's colour: muted ice blue for
  basalt, dark gold on black for blackstone, sage for tuff, plain for cobble. This keeps the ponder promises as trims,
  not whole surfaces.
- Side panels: plain steel fields with a small riveted inspection plate; no glowing vents. Bottom: plain steel.

## Interesting Features

- The drill jab is the rhythm of the block: one jab per drill hit, so basalt chatters twice as fast as cobble, and
  the jabs speed up with RPM.
- Brass: a 2 x 2 batch of smaller blocks forming together under the four-bit head (16 per cycle, in four stacks of
  four, reads as "a batch").
- The lamp is a small round lens on the front frame, top right, with the meaning used across the family.

## States

| State | What changes | Carried by |
|---|---|---|
| Running | lamp **cyan**; pours falling; drill spinning and jabbing; block growing; steam | renderer (lamp, drill, pours, block) |
| Full | lamp **amber**; pours off; drill raised and still; finished block sits complete | renderer |
| Switched off (redstone or link) | lamp **dark red**; pours off; drill raised; block frozen part-formed | renderer reads `powered` (plain ids too) |
| No rotation / overstressed | lamp **unlit**; shaft still; drill still; pours off | renderer |
| Andesite / Brass | trim metal; one block under a one-bit drill vs a 2 x 2 batch under a four-bit head | textures; renderer |
| Product | product trim line, chamber materials, ice vs water, the forming block's texture | textures (per product), renderer |
| Wireless powered | antenna tip lights (as now) | `_powered` models |

## Rig Requirements

New renderer-drawn parts (standalone models, registered like `SHAFT`):
- `cobble_generator_drill` and `cobble_generator_drill_brass`: modelled pointing down, spin axis vertical through
  (8, y, 6.5) (over the block's centre, x 8, z 6.5), spinning with the shaft angle; vertical position from the code:
  raised at y 9 when idle, its tip following the block's top while running, with a short down-and-up jab of about
  0.75 px on each drill hit.
- `cobble_generator_lamp_{running,full,off}` (or one lamp model with three textures): a 2 px lens on the front frame,
  drawn full-bright when lit; nothing drawn when unlit (the frame shows a dark lens).
- The brass batch: the renderer draws four blocks of 2.25 px in a 2 x 2 grid inside x 5.5-10.5, z 4-9, growing
  together.

## Fun Score (1-10)

9. The drill jab makes the most-seen block in the mod feel alive, and the lamp answers "why isn't it working?"
without goggles.

## Builder Notes

- From scratch on the shell, on the process-box kit (`design/models/process-box.lib.js`) in a new generator
  `design/models/cobble-generator.gen.js`; the kit may grow, but must keep producing identical output for the
  harvester and pens (regenerate and compare).
- Keep the interior's renderer geometry; restyle the chamber, floor and ceiling textures (the checker ceiling goes).
- Code changes (small): `CobbleGeneratorRenderer` draws the drill (spin from the shaft angle, height and jab from the
  client progress and a per-hit tick counter in the block entity), the lamp (from running / full / powered / speed),
  and the brass batch; register the new part models; per-product nozzles in the models; item models matching the new
  block (with the drill and one block shown). `renderShaft` and `SHAFT` untouched.
- Preview every product and tier, running, full, off and stopped, with the drill animated at 64 and 256 RPM.
- Before: `design/dossiers/cobble-generator/textures-before.png`.
