# Design Brief: Torque Rotor

Redesign of an existing block. What the code does today is in `design/dossiers/torque-rotor.md`. Family: the ship
hardware set (read `design/briefs/compact-gyroscope.md` for the family look).

## Hard Requirements

From the user:
- **Be bold.** Scope: **Rethink** (a new look; code where it helps the look). It stays a propeller: it turns the ship
  the way an Andesite Propeller at that spot would, without pushing it.
- **Aviator brass and steel**, as the family.
- Previewed in Blockbench first; nothing on `design/hands-off.md` changes (the frozen `cobble_generator_shaft`).

From the code (`design/dossiers/torque-rotor.md`):
- Six facings; the static model is built **facing up, base on the bottom**; the base works against walls, floors and
  ceilings, the blades pointing away from the support.
- The blades are the part model `torque_rotor_blades`, modelled pointing up and spinning about x = z = 8; **their
  reach stays 12 px** from the axis (the wind disc's `BLADE_RADIUS` 0.75, the 0.5-block render box); longer than 16 px
  clips.
- Outline 14 x 14 x 10 from the base (base and hub); the shaft comes in through the base only; the frozen stub sits
  at x/z 6-10 in the first 3.5 px behind the base face.
- One render layer (solid today); item model rebuilt by hand with its own display transforms (the blades overhang).
- Nothing of Aeronautics' propellers may be copied (All Rights Reserved), only matched in spirit.

## Asset Type

Block, 16 px base with blades overhanging to 24 px across; six facings; static base and hub plus the spinning blades.

## Fantasy

An airship's tail rotor: a riveted steel nacelle on a bolted flange, a brass spinner, and broad paddle blades that
blur when it runs.

## Hero Element

**The blades and spinner**: three broad, twisted paddle blades in dark steel with brass leading edges, bolted into a
brass hub under a pointed brass spinner.

## Shape Language

Round and radial (it's a rotor): a round flange, a round nacelle, a cone, blades on a circle.

## Silhouette Description

A low round flange plate bolted to the surface, a short drum-shaped steel nacelle rising from it with rivet bands, a
brass spinner cone on top, and three long blades sweeping out to the full 24 px.

## Primary Forms

- Flange: an octagonal (stepped round) steel plate 14 px across, 2 px thick, with brass bolt heads round its rim, and
  a round socket in the middle where the frozen stub's end shows turning (the 4 x 4 x 3.5 px space behind the face
  left open).
- Nacelle: a stepped-round steel drum about 8 px across up to about y 8, with two rivet bands.
- Hub and spinner: a brass hub ring and a stepped brass cone to about y 11.

## Secondary Forms

- Blades: three, at 120 degrees, each a root clamp in brass, a broad paddle in dark steel turned 22.5 degrees (the
  pitch), a brass leading edge, reaching 12 px from the axis.

## States

| State | What changes | Carried by |
|---|---|---|
| Spinning (either way) | the blades and spinner turn with the shaft, the direction showing which way it turns the ship | renderer (as now) |
| Still | blades at rest | renderer |

## Rig Requirements

`torque_rotor_blades` (blades, hub, spinner) spins about the facing axis through the block centre, as now. The static
model is the flange and nacelle, with the socket open so the stub turning behind it shows.

## Fun Score (1-10)

8. A proper airship rotor, the boldest silhouette in the set.

## Builder Notes

- In the shared generator `design/models/ship-hardware.gen.js`. Blade count changes from today's (the renderer
  doesn't care); keep the 12 px reach.
- Code (small): none needed for the look; the renderer keeps spinning the blade part. Switch to a cutout layer only if
  the new blades need it (they shouldn't).
- Before: `design/dossiers/aeronautics-set/torque-rotor-textures-before.png`.
