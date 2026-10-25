# Design Brief: String Tangler

Redesign of an existing block. What the code does today is in `design/dossiers/string-tangler.md`. Family: the Stone
Infuser (`design/briefs/stone-infuser.md`), designed as a pair, and the process-box machines (Cobble Generators,
Auto Harvester, Animal Pens).

## Hard Requirements

From the user:
- Scope: **Redesign + small code**: a new look, plus code to show what it's doing: **a real spindle, the slime and
  string going in, the web growing and the cobweb coming out**.
- Redesigned **as a pair with the Stone Infuser**, keeping both working.
- The house style: dark muted Create palette, crisp bevelled steel panels, copper as warm trims (never the salmon
  casing), never near-white, never large saturated areas; square box. Previewed in Blockbench first.
- Nothing on `design/hands-off.md` changes; the shared shaft stub `cobble_generator_shaft` stays as it is.

From the code:
- One block class and block entity with the Stone Infuser (`TwoInputMachineBlock`, `TwoInputMachineBlockEntity`):
  the shared behaviour, synced fields and tests stay; a new blockstate property would land on both blocks.
- `facing` (4), modelled with the **window north, the shaft at the back (south)**; the back plate z 14-16 with the
  4 x 4 hole at x/y 6-10, the stub in it (`MachineRenderers.java:49-63`). Shared helpers `faceFront`, `shaft`,
  `quad`, `cube` keep working for the infuser; the `Tangler` renderer class can be rewritten alone.
- The renderer can read: slot 0 (string count), slot 1 (slime count), slot 2 (cobwebs, 0-64), `processing`,
  `getProgress(partialTicks)`, `getMaking()`, and the shaft angle.
- Full-cube shape, `noOcclusion`, cutout; items in and out on every face.
- The tooltip and ponder promise "watch the strands spin, twist and knot" and the README "a spindle inside ... a web
  balls up in the middle": the new look makes those true (and the text is updated where the look changes).

## Asset Type

Block, 16 px, `java_block`, four horizontal facings; a static model plus renderer-drawn moving parts.

## Family

- **Two-input pair** (with the Stone Infuser): the same shell, the same front window and output tray, the same back
  socket, the same "two inputs on the sides" layout; the tangler in **copper** trim (from its Copper Casing recipe),
  the infuser in **andesite** trim.
- **Process boxes**: the steel shell language of the harvester, pens and cobble generators (crisp bevels, corner
  rivets, the flanged back socket), and the **front tray** motif showing the output (`client/TrayItems.java`, used
  read-only).

## Fantasy

A little spinning machine: two spools pay string out to a Create whisk on the shaft, a dab of slime glues it, and the
whisk balls the strands up into a web as it spins faster and tighter, until a cobweb drops into the tray at the front.

## Hero Element

**The whisk spindle** in the middle, on the shaft's axis: Create's whisk (from the recipe) turning with the shaft,
with the strands running onto it and the **web growing on it** as the job goes on.

## Theme

Create's Mechanical Mixer whisk, a spinning wheel, a workshop machine in a glass-fronted steel box.

## Shape Language

Square box; round only for the spools, the whisk and the web.

## Silhouette Description

A steel cube with copper trim, a big front window onto the whisk, a small tray at the bottom of the front, a spool
hub visible through a side port on each side, the flanged socket on the back.

## Primary Forms

- Shell: steel panels with a copper trim ring and bevels; a raised front window frame; the back socket as the
  process boxes'. Side panels each with a small round window onto its spool, so the box reads two inputs, one output.
- Interior: dark steel chamber with depth.

## Secondary Forms

- **Two string spools** on axles at the left and right walls, wound with string: the winding's thickness shows how
  much string is loaded (0, then up to full at 64).
- **The slime pot**: a small glass dish at the bottom, behind the tray, with a green slime blob whose size shows the
  slime count; muted slime green.
- **The tray** at the front, under the window, showing up to three cobwebs as the output fills (TrayItems).

## Interesting Features

- The strands run from each spool to the whisk and wind onto it: thicker, muted off-white (not pure white) ribbons
  that read as string, pulling in and twisting as the job goes on (today's motion, kept and made to meet real parts).
- **The web balls up on the whisk**: a growing web (crossed cobweb-textured quads, muted grey-white) that scales from
  nothing to a full ball with progress, then pops off into the tray when it's done (the existing puff of particles
  and wool sound).
- The spools turn as string pays out, slower than the whisk.

## States

| State | What changes | Carried by |
|---|---|---|
| Empty | bare spools, empty pot, still whisk, empty tray | renderer |
| String loaded | spool winding thickness by count | renderer (slot 0) |
| Slime loaded | slime blob size by count | renderer (slot 1) |
| Working | whisk spins with the shaft; strands wind on; web grows with progress; spools turn | renderer |
| Stopped (no rotation, missing slime, output full) | whisk still, strands slack, web stays part-grown | renderer (and the tray shows full) |
| Output | 0-3 cobwebs in the tray | TrayItems (slot 2) |

## Rig Requirements

New renderer-drawn parts (standalone models registered like `SHAFT`): `string_tangler_whisk` (spinning about the
model Z axis through x/y 8, its back end in front of the stub's inner face at z 12.5), `string_tangler_spool` (drawn
twice, axles along X at the side walls). The web, the strands, the slime blob and the spool winding are drawn in
Java. The strand spin direction is fixed so they follow the shaft on every facing.

## Fun Score (1-10)

9. It finally does what it says: you watch string become a web.

## Builder Notes

- From scratch, in a new generator `design/models/two-input-machines.gen.js` that builds both the tangler and the
  infuser on the process-box kit (the kit may grow, but must keep producing identical output for the harvester and
  pens: regenerate and compare).
- Code (small): rewrite `MachineRenderers.Tangler` (whisk, spools, slime, web, strands to real parts, tray); register
  the part models; the shared helpers and the infuser's renderer keep working. No blockstate changes needed.
- New textures of its own (today it has none, all borrowed): `string_tangler_*`.
- Update the tooltip and README lines that describe the look.
- Preview: empty, loaded, working at progress 0 / 0.5 / 1, stopped, and output 1-3, beside the infuser.
- Before: `design/dossiers/string-tangler/textures-before.png`.
