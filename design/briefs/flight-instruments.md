# Design Brief: Flight Instruments

Redesign of an existing block. What the code does today is in `design/dossiers/flight-instruments.md`. Family: the
ship hardware set (read `design/briefs/compact-gyroscope.md` for the family look).

## Hard Requirements

From the user:
- **Be bold.** Scope: **Rethink**. The four live dials stay (altimeter, airspeed, attitude, heading) and read the same.
- **Aviator brass and steel**, as the family.
- **Remove the three buttons** (they do nothing).
- Previewed in Blockbench first; nothing on `design/hands-off.md` changes.

From the code (`design/dossiers/flight-instruments.md`):
- Four horizontal facings, built **panel facing north** (the pilot on the north side).
- The panel's face lies exactly on the plane through (y 2, z 8) leant back 22.5 degrees about X at (8, 2, 8), 16 px
  wide, 15.15 px long, top edge at y 16; the renderer's needles and the attitude ball sit just in front of it.
  Dial centres at panel (4, 4), (12, 4), (4, 12), (12, 12), radius 2.75 (in sixteenths); the attitude dial a plain disc.
- Dial scales the painted marks must match: altimeter 0-9 (10 m a digit) on the long hand; airspeed -150 to +150
  degrees for 0-60 m/s; heading north up, clockwise.
- Outline in Java (`FlightInstrumentsBlock.SHAPES`): a lip y 0-2 and steps up to the panel; a new silhouette edits it.
- The moving parts' colours are Java constants (sky, ground, needles, hands, wings), so the palette change is code.
- Item: shows the block model (no item renderer).

## Asset Type

Block, 16 px, `java_block`, four horizontal facings; static desk plus renderer-drawn needles and attitude ball.

## Fantasy

An airship's cockpit binnacle: a riveted steel console with four brass-bezelled gauges under a curved glare hood.

## Hero Element

**The four brass-bezelled gauges**: raised brass rings round each dial, on the leaning panel, under a hood.

## Shape Language

The square console of today (it's a desk), round gauges and bezels, a curved hood made of stepped plates.

## Silhouette Description

A riveted steel desk rising to the leaning panel, a glare hood overhanging the top of the panel, four brass rings
standing proud of the face, brass corner caps and a brass rail along the front lip where the pilot rests a hand.

## Primary Forms

- Desk: riveted steel plates, the sides stepped as today's outline, brass corner caps.
- Panel: dark steel face (the 128 px panel texture repainted), dials as dark faces with muted marks.
- Bezels: a raised brass ring round each dial, a quarter pixel proud of the panel face (outside the dial radius so the
  needles sweep inside it).
- Hood: a stepped steel visor along the panel's top edge, overhanging it by 2 px.

## Secondary Forms

- A brass hand rail along the front lip (y 0-2), in place of the buttons.
- Rivet rows along the plate edges; a small engraved brass name plate under the dials.

## Palette for the moving parts (the Java constants)

Sky muted blue `#4f7c96`, ground walnut `#634532`, horizon line, long needles and the heading symbol warm off-white
`#c9c2a8` (never near-white), the altimeter's short hand muted amber `#d99a4a`, hubs dark steel `#2f343b`, the
aircraft symbol dark with a brass centre.

## Dial faces

Dark faces `#1f2327` with muted marks `#8a8c86`; the airspeed arc in muted green / amber / dark red; heading letters
N, E, S, W (fixing today's 3 / 6 / 9 in their places).

## States

| State | What changes | Carried by |
|---|---|---|
| Flying | needles and the attitude ball move with the ship (as now) | renderer |
| Standing | readings for where the block stands (as now) | renderer |

## Code Changes (Rethink)

- `FlightInstrumentsRenderer`'s colour constants to the palette above.
- `FlightInstrumentsBlock.SHAPES` updated to the new silhouette (the hood and rail).
- Register the Create tooltip that the dossier found missing, so the block's tooltip shows.

## Fun Score (1-10)

8. A cockpit you'd want to stand at.

## Builder Notes

- In the shared generator `design/models/ship-hardware.gen.js`; the panel texture repainted at 128 px with the dial
  centres and radius exactly where the renderer draws.
- Before: `design/dossiers/aeronautics-set/flight-instruments-textures-before.png`.
