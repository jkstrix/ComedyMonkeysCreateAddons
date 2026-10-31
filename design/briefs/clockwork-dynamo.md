# Design Brief: Clockwork Dynamo

New block. Gameplay: [design/specs/dynamo.md](../specs/dynamo.md). Family by material: the Trade Bot and Helper Bot (`design/briefs/trade-bot.md`, `design/briefs/helper-bot.md`). Neighbours by function: the Electric Motor and Lava Generator (`design/briefs/electric-motor.md`, `design/briefs/lava-generator.md`), which it must **not** look like.

## Hard Requirements

From the user:
- "This should be themed differently than the rest of the blocks: have textures themed on the Trade and Helper Bots."
- "This will later become part of the backstory building in the Clockwork Wastes branch." The look should read as **built by the bots**. No lore text or Wastes details now (spec D4, D30).
- "The dynamo should have some whimsy like the bots, but not much." The whimsy is **Trade Bot's copper aerial with a brass bead** (spec D16): **stands up** while it makes power, **droops** when idle (stopped, overstressed or on a contraption), and gives a **little spring wobble** when its output changes. **No face, no eyes** (spec D13).
- An ordinary **status lamp** (spec D13).
- Dark, muted, Create-like; accents as trims; **previewed in Blockbench first** (standing rules).

From the spec and the code contract (shared with the Electric Motor):
- **Six facings**, placed like the motor (spec D17). It must read right lying down or standing up; nothing may read upside down.
- **The shaft comes out of the model's north face**, centred on (8, 8). The renderer draws the frozen shared stub `cobble_generator_shaft` (x 6–10, y 6–10, z −0.02–3.5) spinning about that axis. Its corners sweep a 2.83 px radius, so the opening round it is **at least 5.7 px across** (make it 6).
- **FE goes out of the base only**: the south face (z = 16), opposite the shaft. It shows **copper contacts on an insulator pad**, the same "power connects here" sign as the motor's base, so players read both the same way.
- Two block models from one part list, `block/clockwork_dynamo` (`working=false`) and `block/clockwork_dynamo_working` (`working=true`), with **identical geometry**: only the lamp differs. Lamp glow is declared per element with `neoforge_data` full-bright. Everything cutout; full-cube collision shape; light 6 while working (as the motor).
- The aerial is **not** in the block models: it's a separate part the renderer draws (Flywheel part, spec Technical Notes), so it can move.
- Keep the whole model **inside the 16 px cube**, aerial included (machines get packed tight; nothing may poke into a neighbour).

## Asset Type

Block, 16 × 16 × 16 px, `java_block`, any of six facings. Two static state models, plus two renderer parts: the aerial and the commutator. New textures `clockwork_dynamo_*`, cell-relative UVs.

## Fantasy

A dynamo the bots built for themselves out of their own parts. It has a bot's chassis, a bot's brass waist band and a bot's little aerial, which perks up when the current starts flowing. It's the first machine that tells you the automatons understood electricity.

## Hero Element

**The commutator window**: a brass-rimmed slot in the top shows the copper commutator, a drum of copper bars, spinning with the shaft, with two carbon brushes pressed against it by tiny springs. It's the real heart of a dynamo, and you see it turn the moment the shaft does. The aerial is the second read and the charm.

## Theme

The bot palette, exactly as the Trade and Helper Bot briefs set it. The cyan lamp ties it to the electric family without borrowing the turbine kit.

| Material | Where | Colours |
|---|---|---|
| Andesite casing | chassis panels (front, sides, top, bottom) | `#6b6e6a` `#7c7f79` `#8a8c86` (mid grey at most) |
| Dark blue-grey steel | chassis frame edges, base plate, window surround, bearing housing | `#2f343b` `#3c424a` `#4a5059` |
| Muted brass | waist band and its rivets, window rim, bearing ring, lamp rim, aerial collar and bead, vent grilles | `#8f7638` `#b08f45` (`#c9a75a` highlights only) |
| Copper | commutator bars, brush holders, base contacts, aerial rod | `#7a4a2e` `#9c5f3a` `#b87a4e` |
| Carbon / rubber | brushes, insulator pad on the base | `#26272a` `#323337` |
| Mica (dark) | the thin lines between commutator bars | `#2a2420` |
| Lamp | working: cyan `#6fb7c4` with a `#8fcad4` centre (Helper Bot's iris); idle: switched-off `#1e3236` | small area only |

No stripes and no saturated surfaces. Brass, copper and the lamp are trims. Surfaces get the bots' gentle mottling.

**Not the motor family:** no round can, no staves, no ribbed drum, no turbine steel kit. It reads as a bot part, not a turbine part.

## Shape Language

**Rounded squares**, the bots' body language: a box with stepped (chamfered) corners and inset panels in a dark frame, like the Trade Bot Station's plinth. Circles appear only as the bearing ring, the lamp and the bead. It's friendly and sturdy, not a cylinder.

## Silhouette Description

A squat, rounded-square box, a little lower than a full block, with a thin aerial ticking up from one back corner of the top. From the front: a single brass bearing ring round the shaft in the middle of an andesite panel, and the brass waist band showing at the sides. From above: a slot window with copper inside, the aerial, and a small lamp. From the back: a dark plate with copper contacts. It reads as "a little bot torso with an aerial": never as a face, because the front has **one** circle only.

## Primary Forms

- **Chassis**: x 1.5–14.5, y 2–13, z 1–14, with 1 px stepped corners on all twelve edges. Andesite panels inset 0.25 px into a dark-steel frame (frame 1 px wide at every edge), as on the Trade Bot Station.
- **Base plate**: dark steel, x 1–15, y 1.5–13.5, z 14–16, bevelled edge. On its back face (z = 16): a dark **insulator pad** (x 5–11, y 4.5–10.5) carrying **two copper contacts** (2 × 2 px studs, side by side), the motor's language for "power out here".

## Secondary Forms

- **Bearing housing** (front, z 0.5–1): a dark-steel collar with a **brass bearing ring**, opening 6 px across, centred on (8, 8). It's the only circle on the front.
- **Waist band**: a 1.5 px brass band round the chassis (perpendicular to the shaft, z 7–8.5), crossing top, sides and bottom, with a **rivet line** (one rivet every 2 px), straight from the bots' waist band.
- **Commutator window** (top): an open slot x 4.5–11.5, z 2.5–6.5, with a brass rim 0.5 px proud. Inside, at depth y 10–12:
  - the **commutator drum**: axis along Z, radius 2, z 3–6, eight copper bars split by thin dark mica lines;
  - two **carbon brushes**: dark 1 × 1.5 px blocks in small copper holders, one either side (x 5 and x 11), each with a 0.5 px brass spring behind it.
- **Vent grilles** (both sides, x faces): a small muted-brass grille with dark slots (x faces, y 5–9, z 9.5–12.5), the Helper Bot's whisker grille re-used as cooling vents. One per side; nothing else on the sides.
- **Lamp** (top, back-left): a 2 × 2 px lens in a brass rim at x 3–5, z 10–12, sunk into the top panel. On top, so it reads with the aerial and never makes a face with the bearing.
- **Aerial** (top, back-right): a brass **collar** (1.5 × 0.5 × 1.5 px) at x 11–12.5, z 10.5–12; a copper **rod** 0.5 × 2 × 0.5 px; a brass **bead** 1 × 1 × 1 px on top, ending at y 16. It's slightly bigger than the Trade Bot's (rod 1.25 px tall) so it reads on a block, and asymmetric, off to one side as on the bot.

## Interesting Features

- You see the dynamo work: the copper turns in the window with the shaft, and the aerial pops up.
- The bot parts carry the theme: the stepped corners, waist band and rivets, whisker-grille vents, and the aerial on one corner. Anyone who knows Trade Bot sees it at once.
- The front has one ring and no face, so it never reads as a creature.

## Motion (block with moving parts)

**Motion feel**: the aerial is the loosest thing in the mod. It's a light spring on a stiff hinge that overshoots and settles in a few quick, shrinking wobbles, never a linear tween. It reacts and then goes still. **It has no idle sway**: "some whimsy, but not much".

| Trigger | Aerial | Time |
|---|---|---|
| **Starts working** | Springs up from folded: overshoots ~15° past upright towards the front, then two shrinking wobbles, and settles upright | 0.8 s |
| **Working, steady** | Upright and still | — |
| **Output changes** (setting changed, while working) | A little *boing*: a ±12° wobble about its hinge, three swings, settling; with a slight sideways lean (±5°) so it isn't a flat pendulum | 0.7 s |
| **Stops** (shaft stops, overstressed, onto a contraption) | Sags back to folded (~70° back, lying towards the base), with one small bounce as it lands | 1.0 s |
| **Idle** | Folded and still | — |

- The folded pose lies **back along the top, towards the base**: in model space, not by gravity, so it reads right in all six facings (an antenna folded flat, like a car aerial).
- **Commutator**: turns with the shaft, at shaft speed and direction. When stopped, it stops.
- **Lamp**: model swap with the `working` blockstate, nothing animated.
- In game: the aerial is a spring-damper on one bone, kicked when `working` or the setting changes (Trade Bot's aerial maths, spec Technical Notes).

## Rig Requirements

- **aerial**: pivot at the top of the collar, (11.75, 14, 11.5) after the build changes (originally (11.75, 13.5, 11.25)). Rotates about X (fold back, wobble) and a little about Z (sideways lean). The rod and bead are its children.
- **commutator**: pivot on the drum's own axis, (8, 11, 4.5), rotating about Z. It sits above the shaft axis (as if geared up from it), so it's a separate part; the renderer spins it about its own axis at the shaft's speed and direction.
- Everything else is static block model.

## Approved Choices

- **The commutator spins** with the shaft (user, 2026-10-31; spec D16 updated). Brief approved for modelling the same day.
- **Shape approved with these build changes** (user, 2026-10-31, "love it"); the model and generator are the reference where they differ from the sections below:
  1. The window rim is **dark steel**, not brass (brass there crowded the top).
  2. The lamp sits **right beside the aerial's collar** (x 8.5–10.5), one signal unit; at the back-left it made eyes over the window's mouth when facing down.
  3. The base contacts are **stacked in an upright pad**, like a plug socket; side by side they read as eyes.
  4. The side vents have **three** slots (two read as eyes standing up).
  5. Fits: chassis y 2.5–13.5 (centred on the shaft), base plate x 1–15 y 2–14, window 7 × 3 px (z 3.75–6.75, behind the stub's end), band z 7.75–9.25, aerial rod 1 px so the bead ends at y 16, vents y 4.5–11.5 z 9.75–13.
  6. The front bearing boss is mapped at 2 texels a pixel so its ring is round.

## Fun Score (1-10)

**8.** The copper spinning in the window makes the conversion visible, the bot parts make it unmistakably theirs, and the aerial popping up when you start a windmill is a small grin, without the block turning into a character.

## Builder Notes

- **Proportions**: the chassis is 13 × 11 × 13 px on a 2 px base plate; the aerial's bead ends exactly at y 16. Nothing above 16, below 0 or outside 0–16 in x and z, except the shared shaft stub.
- **Must not lose**: the north-face shaft opening ≥ 5.7 px (make it 6) centred on (8, 8); identical geometry in both state models (only the lamp's colour and glow differ); base contacts on the south face; one circle on the front; stepped corners; no z-fighting (overlays 0.25 px apart); cell-relative UVs.
- **Parts to export**: `block/clockwork_dynamo` and `block/clockwork_dynamo_working` (static), `block/clockwork_dynamo_aerial` (part, pivot at the collar), `block/clockwork_dynamo_commutator` (part, on its own axis).
- **Pitfalls**: the bearing ring plus the lamp or grille could read as a face. Keep the lamp on top and the grilles on the sides, and check the front alone. The brass band must stay a trim (1.5 px), not a stripe. Light panels stay mid grey. The window shows a dark interior behind the drum, never see-through to the sky.
- **Preview clips** (Blockbench): `start` (aerial springs up, commutator spins up), `working` (loop: commutator turning, aerial still), `boing` (output change), `stop` (aerial folds, commutator stops).
- **Screenshot for review**: front, three-quarter, side, back, top; standing up (facing up and down); working vs idle (lamp + aerial); the front on its own (no face?); beside the Electric Motor and the Trade Bot Station for the family read.
- **Item icon**: the 3D block model (it isn't tall).
- **Sound** comes after (spec D29: hum following output, spin-up and spin-down, a tiny springy *boing* on the wobble).
