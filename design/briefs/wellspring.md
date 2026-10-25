# Design Brief: Wellspring

Redesign of an existing block. What the code does today is in `design/dossiers/wellspring.md`.

## Hard Requirements

From the user:
- Scope: **Redesign + small code**.
- **The liquid stays visible like it is now**: big windows on all four sides onto the fluid column, at least as open as
  today's (x/z 3-13, y 3-13).
- Show **which upgrades are fitted**: visible bays that fill in as generation and capacity upgrades are added.
- Look: a **muted copper tank**: still a copper machine (it's crafted from Copper Casing, a pump and a tank) but in the
  house's dark aged copper, with riveted bands, never the bright salmon of today.
- House style; previewed in Blockbench first; nothing on `design/hands-off.md` changes.

From the code (`design/dossiers/wellspring.md`):
- No facing: it must read the same from all four sides. Full cube, cutout, `noOcclusion`.
- Blockstate `source` (none, water, lava, honey, chocolate) x `upgrades` (0-3, the generation upgrades fitted): 20
  variants, models free to rename with the blockstate.
- The renderer's fluid box: x/z 2.05-13.95 px, y 3-13 px; inner walls must stay outside it, and the windows' bottom and
  top line up with y 3 and y 13. The 25 / 50 / 75 % notches (y 5.5 / 8 / 10.5) stay readable.
- The capacity upgrade count (0-3) is synced to the client but in no blockstate: showing it is renderer work.
- Ports on all six faces (pipes attach at face centres); lava gives light 9.

## Asset Type

Block, 16 px, no facing; static models per state plus the renderer's fluid column and capacity modules.

## Family

Feeds the Geothermal Turbine and the Lava Generator; sits beside the process-box machines. It keeps its own copper
identity, in the house copper (`#5e3823` `#7a4a2e` `#8a5434` `#9c5f3a`, highlight `#b87a4e` on rivets only), with
dark blue-grey steel (`#2f343b` `#3c424a`) for bands and fittings.

## Fantasy

A riveted copper spring-tank: glass on all sides showing the fluid welling up inside, its upgrade modules plugged into
sockets on its lid.

## Hero Element

**The fluid behind the glass**, as now, framed by four slim copper corner posts so the windows are as big as possible.

## Silhouette Description

A copper-framed glass cube: slim corner posts, a riveted steel band top and bottom, a lid with six round sockets.

## Primary Forms

- Corner posts 2 px square in dark copper, riveted; bottom and top bands 3 px tall (y 0-3 and 13-16) in riveted dark
  steel, so the windows run y 3-13 between them.
- Windows on all four sides x/z 3-13 (or wider), the notches at 25 / 50 / 75 % as small copper ticks on the posts.
- Lid: a copper plate with **six sockets**: three for generation upgrades and three for capacity upgrades, in two rows.

## Secondary Forms

- Generation modules (from the blockstate): a dark steel cap in each fitted socket, with a small lens in the fluid's
  colour (blue water, amber lava, honey gold, chocolate brown), muted, unlit.
- Capacity modules (drawn by the renderer from the synced count): a squat copper canister in each fitted socket.
- Bottom: a plain copper plate with a riveted rim (no drain grate).

## States

| State | What changes | Carried by |
|---|---|---|
| Fluid and level | the column behind the glass | renderer (as now) |
| Generation upgrades 0-3 and which fluid | 0-3 caps with the fluid's lens in the generation sockets | blockstate models |
| Capacity upgrades 0-3 | 0-3 canisters in the capacity sockets | renderer (new) |

## Code Changes

- The renderer draws one capacity canister part model per fitted capacity upgrade at the socket positions.

## Fun Score (1-10)

8. The same readable tank, now visibly kitted out.

## Builder Notes

- Generator `design/models/wellspring-incinerator.gen.js` (shared with the Incinerator); preview every source with 0-3
  generation and 0-3 capacity upgrades, empty, half and full.
- Before: `design/dossiers/wellspring/textures-before.png`.
