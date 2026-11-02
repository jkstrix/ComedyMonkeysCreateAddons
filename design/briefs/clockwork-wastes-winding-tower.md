# Design Brief: The Wrecked Winding Tower

A kit building of the Clockwork Wastes city (D79 (2), D80, D81, D82; spec `design/specs/clockwork-wastes.md`, lore `design/lore/the-three-peoples.md` I and V). The drum is code (`content/wastes/city/kit/WindingTower.java`). This brief covers the parts that need new art, the sprung mainspring and the fallen rafters, and how the city must face the tower. Status: **preview** (Blockbench, `design/models/clockwork-wastes-winding-tower.bbmodel`). Nothing is registered until the user approves the look.

## Placing it in the city: spring side to the street (D82)

**The city must turn the tower so its west side, where the spring is, faces the street or the approach the player comes along.** (The city's placement code lives in `KitCity`.)

The reason:
- The sprung spiral stands parallel to the west wall, so it reads as a spiral only face-on, from the west.
- From the north or south it is edge-on: a grey drum or barrel beside the tower.
- From the east it is hidden behind the drum.
- The torn hole and the run peeling off are on the west side too.

The spring is the tower's story (lore I and V: the mainsprings let fly). Turned any other way, the tower reads as a plain broken brick drum with something grey beside it.

**It can be turned but never mirrored.** The prop pieces turn with their `facing`, but no blockstate can mirror a model, so a mirrored site would draw the spring wrong. `WindingTower` throws if a mirrored site reaches it.

## Registered (D84, D87)

The approved pass 4, exported by `design/models/clockwork-wastes-props.export.js`. The exporter runs the same checks as `clockwork-wastes.export.js` and generates `content/wastes/WindingTowerPieces.java`.
- **Pieces:** `sprung_mainspring_<piece>`, 20 blocks (the arbor turn's two halves, the straight, the burst, 5 peel sections, 10 spiral sections and the ruined ring's torn scrap), and `fallen_rafter_long` and `fallen_rafter_short`.
  - Each is a `PropPieceBlock` with `facing` (north as the tower lays it), an item and a loot table that drops itself (D37).
  - They have no collision; the outline is the part of the piece in its own block.
  - Spring pieces are in the pickaxe tag; the rafters are in the axe tag.
- **Standing (D87):** each piece has a reach, the cells its model runs through (`WindingTowerPieces`). The kit's floating check (`ModelChecks.joined`) counts a multi-block prop standing in every cell its pieces reach. So a prop that stands on the ground, or on a block, at one cell holds all its pieces. There is no invisible link block.
- **Ruined ring (D87, option c):** keeps the spiral on the ground, plus a torn-off scrap of the peel (`sprung_mainspring_fallen_0_0`, "Torn Scrap"), fallen with the wall.
  - The scrap lies flat on the stone pile's top (x −8..−7, z 3..4, y 16 px), its west end keeping a little curl (22.5° then 45° up).
  - It sits clear of the spiral's sag, which comes out from under the pile to the south: no fork, no ramp.
  - It is one section, anchored at (−8, 1, 3). The pile has no top slab in the ruined ring, so the scrap lies on the stone.
  - Nothing else of the spring or the rafters is laid.
- **What isn't laid:** the peel's last scrap (`peel_2_0`) lies under the stones, so it isn't a block.

## The Sprung Mainspring (prop)

### Hard requirements (D80, D81, D82)

- **Real thin strip.** About 2 px thick and **two blocks wide**, curved in segments. It must read as **a coil of strip, never as a block**. The dark between turns is the **real gap**, never painted on (visual language, "Spring steel").
- **How it lies (D81, D82): properly sprung.**
  - **An upright spiral standing on its edge, parallel to the west wall.**
  - **Three tight turns at its centre.** After them, the outer turns spring well out and apart: uneven and clearly loose, with one sagging onto the ground.
  - **The run peels off the wall in an arc**, out and away from the bricks, using diagonal pieces. It never lies flat down the wall like a downpipe.
  - The turn still on the arbor inside stays.
- **It must read as a spiral of several turns,** never one ring, a handle (pass 1's tankard), a pen (pass 2's sheet-metal loops) or a roll of tape (pass 3's tidy spiral).
- **Aged (D81).**
  - The approved spring-steel shades taken down a step, so the strip sits at the tower's value, never lighter than the brick.
  - Rust in **runs from the strip's edges**, never whole faces, no random noise (visual language, "Age and wear").
  - **A few bright scuffs, only where it scraped the wall:** the burst through the hole, and the peel's start over the lip.
  - Painted in the generator, so it stays reproducible.
- **Planned as block models.**
  - Every coordinate in −16..32.
  - One-axis element rotations of 0, ±22.5 or ±45 only.
  - No zero-width UVs; overlays at least 0.25 px apart (none are used).
  - Facing and turns through the blockstate (x, y in 90° steps).
- **No twist piece (D81).** The run (in the drum's plane) and the spiral (in the wall's plane) meet under a pile of thrown stones, which hides the change of angle.

### Palette (aged)

| Use | Colour | Note |
|---|---|---|
| Strip | `#565f67` | Luminance about 94. The brick is about 102. |
| Edge, cut ends | `#3b4248` | |
| Highlight | `#6b747c` | One short dash a block, on plain strip only. |
| Scuff | `#8a949c` | The approved highlight. Only where it scraped the hole. |
| Rust runs | `#7f4d30` at the edge, `#6e4129` body, `#5a3523` tail | The house's rust. |

- **Runs.** Each run starts at the strip's own edge and runs 2–8 px in. There are three hand-placed tile variants, and every flat face is mapped so its v 0 is that strip edge.
- **Where the rust goes.** Only on chords lying within 22.5° of level, where water sits: the tops of the turns, the insides of their feet, and the sag on the ground. Also on the peel's foot by the stones. About every other such chord, picked by a fixed hash.
- **The thin edges** are the plain edge tone.

### Pieces

| Piece | Size and shape | Where it's laid |
|---|---|---|
| `arbor_turn_upper`, `arbor_turn_lower` | One turn round the arbor, radius 10 to 16 px, in two halves. | Inside, on the arbor. |
| `straight` | One block. | Inside, out to the wall. |
| `burst` | Three blocks through the wall, buckled up 4 px in the hole. Scuffed. | The hole (x −1..1, y 9). |
| `peel_*` | **The run peeling off: the new diagonal pieces.** In the drum's plane, across z 3..4: chords of 0°, 22.5°, 45° and 67.5° arcing from the hole's lip (x −1, y 9.5) out and down to the ground about 7 blocks out (x −7). Cut at block lines into 3 × 3-block sections (6 pieces). Scuffed over the lip. | x −8..−2, y 0..9. |
| `spiral_*` | **The sprung spiral,** in the wall's plane. Cut into 3 × 3-block sections (12 pieces, one fixed set). | x −8..−7, z 3..14, y 0..7. |

Cutting at block lines keeps each chord's angle. A slanted cut overshoots the line by under half a pixel, which is trimmed.

### The spiral

- **Drawn by a turtle.** It turns 22.5° a chord (clockwise seen from the west), each chord 2r·tan 11.25° long for its radius of curvature r, so every chord is at a legal angle.
- **Three tight turns:** r from 3.5 to 16 px, about 4 px apart (a 2 px gap).
- **Then 2¼ sprung turns:** r from 26 to 64 px with a ±35% jitter that grows outward. Two of the loose chords hold their heading, as kinks.
- **The sag:** the last turn's foot lies along the ground for 96 px (6 blocks), heading north to the stones where the run comes down.
- **Size and place:** about 7 blocks across and 7 tall, 7–8 blocks out from the wall (x −8..−7), south of the hole (z 6..14).

## The Fallen Rafter (prop, D81, D82)

- Two dark oak beams, 8 px square, fallen in from the west rim.
  - The upper end is caught in the wall top; the beam is turned down into the drum.
  - One is 46 px at 45°; the other is 40 px at 22.5°.
- Dark oak's own log textures, in 16 px lengths.
- Until it's registered, `WindingTower.RAFTER_CELLS` hold a placeholder dark oak log.

## What not to lose

- It is **strip**: thin, with real gaps, aged smooth metal. No block of steel, no painted turns.
- **Several turns:** tight at the heart, sprung loose outside, one turn on the ground, never one ring.
- **It came from the arbor:** the turn inside, the burst, and the peel off the lip, so the story reads from the hole.
- It is no lighter than the tower, and rust only runs from the edges.
- **It faces the street.**

## Choices made provisionally (for the user's word)

1. **The spiral's place:** 7–8 blocks out from the wall and south of the hole, so the peel lands beside it rather than through it.
2. **The spiral's numbers:** three tight turns, then 2¼ loose ones, with the jitter, kinks and sag above.
3. **The peel's arc:** 0°, 22.5°, 45°, then one long 67.5° chord to the ground. A rounder arc would need more chords and sections.
4. **Sections:** the peel and spiral as 18 fixed 3 × 3-block section pieces. A smaller set of reusable diagonal pieces can't follow a 22.5° line, because it doesn't repeat on the block grid.
5. **The street-view cameras:** street level (eye 1.6 blocks up), 8 and 20 blocks out, measured from the spring's outer side (x −8).
