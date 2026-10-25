# Design Brief: Compact Gyroscope

Redesign of an existing block. What the code does today is in `design/dossiers/compact-gyroscope.md`. Family: the ship
hardware set with the Torque Rotor and the Flight Instruments (`design/briefs/torque-rotor.md`,
`design/briefs/flight-instruments.md`).

## Hard Requirements

From the user:
- **Be bold.** Scope: **Rethink**: a new look, and code changes where they make the block show what it does. Its
  behaviour (the levelling, the brake) stays as it is.
- The family look: **aviator brass and steel**: dark riveted steel with brass bezels, gimbal rings and gauges, an
  airship's engine room, on the house palette (muted brass, never large saturated areas, never near-white).
- Show **the tilt**: gimbal rings that show which way the ship is leaning and how hard the gyroscope pushes back.
- Show **the brake**: brake calipers that visibly clamp the flywheel as the redstone signal rises.
- Previewed in Blockbench first; nothing on `design/hands-off.md` changes (the rotor's shaft stub stays frozen).

From the code (`design/dossiers/compact-gyroscope.md`):
- One unrotated blockstate, no facing: it must read the same from all four sides, the shaft vertical through the
  block (kinetic faces up and down only); the top and bottom keep a 4 x 4 opening at x/z 6-10 for Create's shaft.
- Full-cube shape; cutout; the case is the block model, everything that moves is drawn by the renderer.
- Synced today: brake (0-15), on a ship, effort (0-1), tilt (degrees). The flywheel's angle runs at the braked speed
  (`getFlywheelAngle`).
- Item model rebuilt by hand (no item renderer).
- Nothing of Create Aeronautics, Simulated or Gyroscope Stabilizers may be copied (All Rights Reserved).

## Asset Type

Block, 16 px, `java_block`, no facing; a static case plus renderer-drawn gimbal, flywheel, calipers and shaft.

## Family

The ship hardware set: **dark blue-grey riveted steel** (`#2f343b` `#3c424a` `#4a5059`) in plates with rows of rivets,
**brass rings and bezels** (`#8f7638` `#9c8240` `#b08f45`, brighter `#c9a75a` only on rivet heads and edges), round
glass portholes, and the same corner post profile on all three. The gyroscope is its heart: the brass gimbal.

## Fantasy

A real gyroscope in a riveted cage: the flywheel spins in brass gimbal rings and stays level while the ship lurches
round it, the rings swinging to keep it there; pull the brake lever and steel calipers bite the rim.

## Hero Element

**The brass gimbal**: two concentric rings, the outer on pins in the cage, the inner on pins in the outer ring at right
angles, carrying a brass-rimmed flywheel. The rings **counter-tilt so the flywheel stays level with the world** while
the ship tilts.

## Theme

Ship's gyrocompass and gyrostabiliser, aviation instrument brass, riveted airship steel.

## Shape Language

A square riveted cage outside (it's a block), all round inside: rings, a disc, round portholes.

## Silhouette Description

A steel frame cube: four riveted corner posts, top and bottom plates with brass shaft collars, and big round glass
portholes on all four sides, through which the brass rings and the spinning flywheel fill the block.

## Primary Forms

- Cage: corner posts 2 px square, top and bottom plates 2 px thick with the 4 x 4 shaft opening in a brass collar.
- Sides: a steel plate on each side with a large round porthole (about 10 px across) in a brass bezel; the plates stop
  short of the posts so the corners read as a frame.
- Gimbal: outer ring about 11 px across, inner ring about 9 px, 1 px brass bands, pins at their pivots.
- Flywheel: a brass-rimmed steel disc about 7 px across with spokes, on a short axle in the inner ring.

## Secondary Forms

- Brake: two steel caliper shoes on the inner ring, either side of the flywheel's rim, closing in with the signal.
- Shaft: Create's shaft through the top and bottom collars; inside, a short universal-jointed axle down to the flywheel.
- Rivet rows along every plate edge.

## Interesting Features

- The flywheel stays level in the world, so on a tilting ship the cage visibly rocks round a steady wheel.
- At full brake the calipers are clamped shut and the wheel stands still while the shaft keeps turning.
- Off a ship, the rings rest square.

## States

| State | What changes | Carried by |
|---|---|---|
| Spinning | flywheel turns at the braked speed | renderer (`getFlywheelAngle`) |
| Ship tilting | the outer ring turns about one horizontal axis and the inner about the other, opposite to the ship's pitch and roll, so the wheel stays level | renderer, from synced pitch and roll (new) |
| Braking (1-15) | the calipers close in proportion; the wheel slows | renderer (synced brake) |
| Fully braked | calipers shut on the rim; wheel still | renderer |
| Off a ship | rings square, wheel spins | renderer |

## Rig Requirements

Renderer-drawn parts (standalone models): `compact_gyroscope_outer_ring` (pivots about X through the centre),
`compact_gyroscope_inner_ring` (about Z, inside the outer), `compact_gyroscope_flywheel` (spins about the inner ring's
axis), `compact_gyroscope_caliper` (drawn twice, sliding toward the rim). Create's shaft drawn in the collars only.

## Code Changes (Rethink)

- The block entity syncs the tilt's two components relative to the block (pitch about X and roll about Z, in degrees)
  alongside the existing tilt, from the same orientation it already reads; the goggles keep using the total.
- The renderer draws the gimbal, flywheel and calipers instead of Create's flywheel; the ring angles ease toward the
  synced values so they swing smoothly.

## Fun Score (1-10)

9. A gyroscope you can watch keeping the ship level.

## Builder Notes

- From scratch, in a new generator `design/models/ship-hardware.gen.js` shared by the set (its casing parts are the
  family's); preview the gyroscope still, on a ship pitching and rolling, braking, and fully braked.
- Before: `design/dossiers/aeronautics-set/compact-gyroscope-textures-before.png`.
