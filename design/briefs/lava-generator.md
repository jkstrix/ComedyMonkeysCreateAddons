# Design Brief: Lava Generator

Redesign of an existing block. What the code does today is in `design/dossiers/lava-generator.md`. Family: the Geothermal Turbine (`design/briefs/geothermal-turbine.md`, approved and in game) and the Electric Motor (`design/briefs/electric-motor.md`).

## Hard Requirements

From the user:
- Scope: "Redesign + small code". New look; a small renderer change so the rotor follows the load. Behaviour otherwise unchanged.
- Ports stay where they are: **lava in at the back, power out at both sides**. The new model makes those ports obvious.
- Show: the **rotor follows the load** (spins faster when more power is being taken).
- Match the Geothermal Turbine's look. Dark muted Create palette, crisp bevelled panels, accents as trims. Previewed in Blockbench first.
- Nothing on `design/hands-off.md` changes. The turbine is hands-off: share its look, never its files.
- "Then we get the sound designer on them": a sound pass once the models are approved.

From the code:
- Blockstates load `block/lava_generator` (`lit=false`) and `block/lava_generator_lit` (`lit=true`), y-rotated by horizontal `facing`. The two models must keep **identical geometry**; only the lit parts differ.
- The renderer draws `block/lava_generator_rotor` turned about the front-to-back axis through **(8, 11.5)**, modelled at z 4 (`ElectricRenderers.java:105-109`). The pivot stays.
- Lava inlet: the **back** face only (`LavaGeneratorBlock.inlet`). FE terminals: the **two sides** only (`isTerminal`). Unit tests pin both.
- Light 9 while lit; everything cutout; full-cube shape.
- Flames and lava pops spawn at y 4.48 just outside each side wall; they move to the new grate (code change below).
- The Ponder pointer is hard-coded at (8, 11.52, 2.4), the rotor window: keep the window there.
- Lit glow is declared per element with `neoforge_data` full-bright (as the current lit model does).

## Asset Type

Block, 16 × 16 × 16 px, `java_block`, horizontal facing. A static model in two states plus the renderer-drawn rotor.

## Family

The electric family's shared language, set by the turbine: crisp blue-grey steel panels with andesite trim, round masses on square bases built from staves, brass only on bezels, copper for windings, pipes and terminals, **muted amber** heat behind dark grates, and **one cyan light that means "making power"**. The generator is the turbine's little brother: the same firebox-and-turbine idea in one block.

## Fantasy

A turbine you can carry: a lava firebox under a little turbine wheel that whirrs faster the more you ask of it.

## Hero Element

**The turbine wheel behind its port window**: a bladed wheel about 7 px across (up from today's 4.8 px star) seen through a round, brass-bezelled window on the front, spinning faster under load.

## Theme

As the turbine: steel `#2f343b` `#3c424a` `#4a5059` with andesite trim `#6b6e6a` `#7c7f79`; brass bezel `#8f7638` `#b08f45`; copper `#7a4a2e` `#9c5f3a`; amber glow `#c76a2a` `#d99a4a` behind `#1f2327` grates; the cyan lamp `#6fb7c4`. Light steel blades `#4a5059` `#5f656e`. No salmon copper, no green-grey mottle, no teal.

## Shape Language

Circles on a square: a round turbine housing (its axis front to back) sitting on a square firebox.

## Silhouette Description

From the front: a low square firebox with a glowing grate across its face, a round housing on top with the brass-ringed window and the wheel inside. From the side: the firebox, the round housing, and a copper terminal block on the flank. From the back: the copper lava inlet flange low down. Reads as "small turbine on a stove".

## Primary Forms

- **Plinth**: full 16 × 16, y 0–2, steel with andesite trim.
- **Firebox**: x 1–15, z 1–15, y 2–8, crisp panels.
- **Turbine housing**: a 16-sided drum along Z, axis at (8, 11.5), radius 4.5 (y 7–16), from z 1 to 14, with stepped end caps.

## Secondary Forms

- **Port window**: a brass bezel ring on the housing's front cap round a dark opening, radius about 3.8, showing the wheel.
- **Front grate**: a dark slotted grate across the firebox front (x 3–13, y 3.5–6.5), amber when lit.
- **Side terminals**: on each flank, a dark insulator plate with two copper terminal posts and andesite collars (the FE ports), **centred on the face at (8, 8)** where Pipez cables attach, on a small steel terminal block standing on the firebox beside the drum. User, in game: "pipez and fluids always connect to middle of block".
- **Back inlet**: a copper flange with a dark bore, **centred on the back at (8, 8)** where pipes attach, on a short manifold bridging the firebox and the drum's end.
- **Cyan lamp**: a small lens on the housing's top, lit when running.

## Interesting Features

- The wheel's speed tells the player how hard it's working.
- Each port looks like what it is: copper posts for power, a pipe flange for lava.
- The grate glows only while it burns.

## States

| State | What changes | Carried by |
|---|---|---|
| Empty (`fuelled=false`) | Dark ash behind the window glass, lamp dark | `lava_generator` |
| Lava in the tank, not burning (`fuelled=true`, `lit=false`) | Calm, darker lava with a little crust behind the glass (full-bright, light 4), lamp dark, wheel coasting to a stop. User: "when the generator has lava the lava chamber should read as full even if it's not moving" | `lava_generator_full`, new `fuelled` blockstate |
| Running (`lit=true`) | Grate glows amber, cyan lamp lit, wheel spinning | lit model (`neoforge_data` full-bright), rotor |
| Load | Wheel speed follows the output, from a slow turn at light load to full speed flat out | **renderer/BE, new** |

## Rig Requirements

- `lava_generator_rotor`: modelled with its hub at (8, 11.5), blades in the XY plane, around z 3–5; spins about Z through that point. Radius ≤ 3.6 to clear the window.

## Sound

A sound pass follows once the model is approved (`character-sound-designer`), as for the turbine.

## Fun Score (1-10)

8. The first electric block a player builds now looks like the turbine's little brother, and its wheel shows the load at a glance.

## Builder Notes

**Start from scratch** on the shared kit (`process-box.lib.js`, the turbine generator's drum, disc and tile helpers). Export both state models from one part list, with the lit parts flagged; new textures `lava_generator_*`. Retire `machine_paint*`, `brass_weathered`, `electric_glow*` once neither block uses them.

**Code changes** (small):
1. **Rotor speed follows the load**: the target spin scales from about a quarter at light load to the full 24°/tick flat out, from the synced output (`LavaGeneratorBlockEntity`).
2. **Particles**: flames and lava pops out of the front grate instead of the side walls.

**Must not lose**: the inlet on the back, terminals on both sides, the rotor pivot at (8, 11.5) and the window at the Ponder pointer, identical geometry in both state models, no z-fighting (overlays 0.25 px apart), cell-relative UVs.

**Screenshot for review**: front, three-quarter front and back, both sides; off vs running; the wheel close up; with the turbine and motor for the family read; beside the before shots.
