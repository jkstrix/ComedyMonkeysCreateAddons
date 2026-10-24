# Design Brief: Electric Motor

Redesign of an existing block. What the code does today is in `design/dossiers/electric-motor.md`. Family: the Geothermal Turbine (`design/briefs/geothermal-turbine.md`) and the Lava Generator (`design/briefs/lava-generator.md`).

## Hard Requirements

From the user:
- Scope: "Redesign + small code". New look; behaviour unchanged.
- Keep **all six facings**: it must read right lying down or standing up.
- Show: the **power state**, a cyan lamp while running and a dark-red one when it's short of power.
- Match the Geothermal Turbine's look. Dark muted Create palette, crisp bevelled panels, accents as trims. Previewed in Blockbench first.
- Nothing on `design/hands-off.md` changes. The turbine is hands-off: share its look, never its files.
- "Then we get the sound designer on them": a sound pass once the models are approved.

From the code:
- Blockstates load `block/electric_motor` (`powered=false`) and `block/electric_motor_powered` (`powered=true`), rotated for all six facings. Both models keep **identical geometry**; only the lamp differs.
- The shaft comes out of the model's **north face**, centred on (8, 8). The renderer draws the frozen shared stub `cobble_generator_shaft` (x 6–10, y 6–10, z −0.02–3.5) spinning about that axis. Its corners sweep a 2.83 px radius, so any opening round it is at least 5.7 px across.
- FE goes in **only through the base** (the flange face opposite the shaft), in every facing. User's call after the first model: "with the motor being round we can only connect to the base". Code change: `ElectricMotorBlock.isInput`, used by the energy capability; the base shows copper contacts on an insulator pad.
- Light 6 when powered; everything cutout; full-cube shape.
- Lamp glow is declared per element with `neoforge_data` full-bright.

## Asset Type

Block, 16 × 16 × 16 px, `java_block`, any of six facings. Static model in two states; the renderer draws only the shared shaft.

## Family

As the generator: the turbine's crisp blue-grey steel and andesite trim, round masses built from staves, copper windings, brass bezels only, and the family's lamp colours (cyan for "has power and is running", dark red for "short of power").

## Fantasy

A sturdy flange-mounted motor: copper windings in a ribbed steel can, its shaft turning out of a bearing at the front, and a lamp that tells you at a glance whether it's getting enough power.

## Hero Element

**The ribbed can with its copper windings showing** through a slotted band round the middle: the electric heart the old model only talked about ("the coil glows"). The shaft turning out of its bearing at the front is the second read.

## Theme

As the generator: steel, andesite trim, brass bezels, copper `#7a4a2e` `#9c5f3a` `#b87a4e` for the windings, the cyan lamp `#6fb7c4` and the dark-red lamp `#8a2f22`. No salmon copper, no teal, no green-grey mottle.

## Shape Language

A circle on a square: the round can on a square mounting flange. With no feet, it reads the same in any of its six facings, the way a real flange-mounted motor bolts to whatever it drives.

## Silhouette Description

From the front: a square flange plate behind a round can, a bearing ring round the turning shaft in the middle, the lamp in one corner of the flange. From the side: the flange at the back, the ribbed can, the copper band, the end bell at the front. Standing up, the same shapes rotated: nothing reads upside down.

## Primary Forms

- **Mounting flange**: a square plate at the back, x/y 1–15, z 13–16, bevelled, with four bolt heads at the corners.
- **Can**: a 16-sided drum along Z, axis at (8, 8), radius 6, z 3–13.

## Secondary Forms

- **Cooling ribs**: thin steel ribs along the can (on the stave seams).
- **Winding band**: a slotted ring round the middle of the can (z 7–9) where copper windings show through.
- **End bell**: a front cap with a bearing ring round the shaft, opening at least 6 px across so the stub's corners clear it.
- **Lamp**: a small lens in a brass bezel on the flange's front face, top corner.
- **Nameplate**: a small brass plate on the can (trim only).

## Interesting Features

- The copper windings finally make "electric" readable.
- Square flange + round can works in every facing.
- One lamp, two colours, tells the whole power story.

## States

| State | What changes | Carried by |
|---|---|---|
| Running (`powered=true`) | Lamp lit cyan | powered model (`neoforge_data` full-bright) |
| Short of power / off (`powered=false`) | Lamp lit dark red | unpowered model (`neoforge_data` full-bright) |
| Speed and direction | Shaft spins (existing) | renderer |

## Rig Requirements

None beyond the existing shaft. Both lamps are static elements in their state's model.

## Sound

A sound pass follows once the model is approved (`character-sound-designer`).

## Fun Score (1-10)

8. It finally looks electric, it sits right whichever way it's placed, and the lamp answers "why isn't it turning?" before the goggles do.

## Builder Notes

**Start from scratch** on the shared kit; drums along Z need a Z-axis version of the turbine's stave drum. Export both state models from one part list (the lamp's colour and glow differ). New textures `electric_motor_*`.

**Code changes**: power in through the base only (`ElectricMotorBlock.isInput`, the energy capability, the Ponder text and the README). The lamp swaps with the existing `powered` blockstate.

**Must not lose**: the shaft socket on the north face with a ≥ 5.7 px opening, identical geometry in both models, a look that's right in all six facings, no z-fighting (overlays 0.25 px apart), cell-relative UVs.

**Screenshot for review**: front, three-quarter, side, back; standing up (facing up and down); powered vs not; with the turbine and generator for the family read; beside the before shots.
