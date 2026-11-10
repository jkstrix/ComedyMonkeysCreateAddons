# Design Brief: Pointed Dripstone Generator

New block in the process-box family, beside the Cobble Generators (`design/briefs/cobble-generator.md`, built from
`design/models/cobble-generator.gen.js` on `design/models/process-box.lib.js`). Gameplay: the approved spec
`design/specs/pointed-dripstone-generator.md`, whose decisions are hard requirements here.

## Hard Requirements

From the user (2026-11-10):
- "new block that compliments the stone generators, pointed dripstone generator": it belongs in the stone generators'
  family and must look it.
- "You add 4 pointed dripstone and supply a water to it and it generates pointed dripstone."
- "you should need to supply SU to the back also": a shaft socket in the back.
- "them dropping should be automatic like the stone generator or the farm": the grown tips are broken off inside and
  drop into the store by themselves.
- "fun animation on cracking the spikes": the break is a show, not a pop.
- A **hammer knocks them off** (user's choice), and the **four stalactites take turns**, one per cycle.

From the spec:
- The four seed Pointed Dripstone stay inside and are **shown in the window** (D8). Without all four it doesn't run.
- Water comes in from **any side but the front and back** (D19); window faces the placer, four horizontal facings,
  shaft only in the back (D5, D25).
- One piece per cycle: 200 × 256 / RPM ticks (10 s at 256 RPM, 40 s at 64); the knock happens inside the cycle (D11,
  D32).
- Holds 64; output out of any side (D14, D18).
- **No light emission**: nothing hot inside (spec, Technical Notes).
- Tiers: **Andesite** and **Wireless Andesite** (frequency pads and antenna on top, as the family) (D10).
- Idle when full, switched off, missing seed, out of water or not turning; a part-done growth waits where it is (D20).

From the house rules: dark muted palette, crisp bevelled steel panels (not mottle), accents as trims; square box (round
is for motors and engines); ports at face centres; previewed in Blockbench first; the item model is real geometry at
the standard angle.

## Asset Type

Block, 16 px, `java_block`, four horizontal facings; two ids (`pointed_dripstone_generator`,
`wireless_pointed_dripstone_generator`, plus the wireless `_powered` model as the family). Static shell models plus
renderer-drawn interior parts (hammer, stalactites, growing and falling tip, reservoir water, lamp, tray).

## Fantasy

A little dripstone cave in a box: water seeps through a dripstone ceiling, four stalactites grow tips in turn, and a
clockwork trip hammer swings round and cracks each one off into the tray.

## Hero Element

**The trip hammer.** A small Create-style hammer on an arm from a central spindle. Each cycle the spindle ticks round a
quarter turn to the next stalactite, the hammer draws back, holds, and whacks the grown tip, which shudders, cracks and
drops into the front tray. It's the thing players will stop to watch.

## Theme

The process boxes' shell and the Cobble Generators' chamber, with a cave inside.

| Material | Use | Colours |
|---|---|---|
| Dark blue-grey steel | shell field panels, the spindle | `#2f343b` `#3c424a` `#4a5059` (big panels near flat: `#3c424a`, rare `#40464e`) |
| Andesite | trim ring, window frame, socket, hammer arm | `#6b6e6a` `#73766f` `#7c7f79` `#8a8c86` |
| Dripstone (vanilla's own) | the ceiling slab, the stalactites | vanilla `dripstone_block` and `pointed_dripstone_*` textures, used as they are |
| Water | the ceiling reservoir | vanilla's animated water sprite, tinted as in the Cobble Generator's pool |
| Glass (cutout) | window, reservoir underside | edge `#6f7f87`, glint `#a9bac2` (the family glass, `cobble_generator_glass.png`) |
| Hammer head and arm | the trip hammer | the Cobble Generators' drill steel: `#4a5059` `#555b63` `#5f656e`, striking faces `#6b7178` (user, 2026-11-10, after trying vanilla iron: "lets change the hammer rather than the drill, we are in create after all") |
| Lamp | the family state lamp | cyan `#4f8f99`/`#6fb7c4` running, amber `#d99a4a` full, dark red `#8a2f22` stopped by a fault or redstone, dark lens unlit |

No product trim colour: the dripstone inside is the product's colour. No brass on the Andesite tier.

## Shape Language

Squares: the sealed box, bevelled steel panels and a raised window frame. Triangles inside only: the stalactites
point down, which gives the chamber its own shape against the square shell. Round only for the spindle, the port
collars and the lamp.

## Silhouette Description

The Cobble Generators' clean steel cube: a deep framed window on the front, the flanged socket on the back, a round
water port in the middle of each side, the lamp on the front frame. From the front at gameplay distance: a band of
blue water across the top of the window, four brown teeth hanging below it, and a little hammer in the middle. That
reads "dripstone" from five blocks, different from the cobble generator's lava-and-water glow. Wireless: frequency pads
and antenna on top, as the family.

## Primary Forms

- **Shell**: the process-box steel field panels, 1 px trim ring and bevels, exactly as the Cobble Generators (same
  kit), so the two sit side by side as one set.
- **Front**: the raised 2 px frame round the 12 × 12 window, glass with grey glints (never near-white).
- **Inside, top to bottom**:
  1. **Reservoir** (y about 13.5–15): a shallow glass-bottomed tray of water under the ceiling. Its level follows the
     tank (0–1,000 mB), so "needs water" shows as an empty reservoir.
  2. **Dripstone ceiling** (y about 12–13.5): a slab in vanilla's Dripstone Block texture, the water seeping through it.
  3. **Four stalactites** hanging from it in a 2 × 2 square, drawn from vanilla's Pointed Dripstone models scaled to the
     box (each seed about 4 px long), so they look like real dripstone.
  4. **Floor** (top y 2), the family's dark chamber floor, with the **front tray** below the window showing 0–3 knocked
     off tips (the family's tray motif, `TrayItems`).

## Secondary Forms

- **Spindle**: a round steel post rising from the middle of the floor, with a small andesite collar where it indexes;
  geared to the shaft (it's what the shaft drives).
- **Hammer arm**: an andesite arm from the top of the spindle with the hammer head on its end, reaching the stalactite
  tips.
- **Water ports**: a round andesite collar in the **centre (8, 8) of the left and right faces**, where Create pipes and
  Fluid Lines attach (2 × 2 hole in a 4 × 4 collar with the corners off, on the grid).
- **Back socket**: the family's flanged socket round the shared shaft stub.
- **Lamp**: the family's small round lens on the front frame, top right.
- Side panels otherwise plain steel fields, as the family. Bottom plain steel.

## Interesting Features

- **The cycle, in turns.** One stalactite at a time grows a fresh tip down from its seed. When it's done, the hammer
  cracks it off and the spindle ticks a quarter turn to the next one. Round and round: something happens every few
  seconds at speed.
- **The knock** (the fun bit, a fixed about 1.5 s at the end of each cycle, whatever the speed):
  1. **Wind-up** (10 ticks): the arm draws back from the tip, the head lifting a little; ease out, then a 3-tick hold
     (anticipation).
  2. **Whack** (2 ticks): snaps forward into the tip.
  3. **Crack** (on the hit): the tip shudders (a 1-px shake for 3 ticks), dripstone crumb particles and vanilla's
     Pointed Dripstone break sound (the user's call: vanilla sounds only, no custom sound);
     the piece goes into the output on this tick.
  4. **Drop** (6 ticks): the tip falls, turning a little, into the front tray, with a tiny bounce.
  5. **Recoil and settle** (6 ticks): the hammer bounces back past rest and settles; the spindle ticks a quarter turn to
     the next stalactite with a small overshoot.
- **Water you can see working**: now and then a drip falls from the growing tip (vanilla's dripping-water particle),
  which tells the player water is feeding it.
- **At a glance**: running (cyan lamp, tip growing, drips, the hammer's rhythm); full (amber, hammer parked, a grown tip
  waiting, tray full); stopped by redstone, missing seed or out of water (dark red, frozen; an empty reservoir or a
  missing stalactite shows which); no rotation (dark lamp, frozen).
- **Fewer than four seeds**: the empty hooks show as bare spots on the ceiling, so it's plain what's missing.

## Rig Requirements

Renderer-drawn parts, as standalone models registered like the family's `SHAFT`:

| Part | Model | Moves | Pivot / axis |
|---|---|---|---|
| Spindle + arm + hammer | `pointed_dripstone_generator_hammer` (one model, or spindle and arm as two) | Index: quarter turn per cycle, eased with a small overshoot. Swing: the arm tilts back and forward for the knock | Index about the vertical axis through the spindle centre (x 8, z 7.5 in the north-facing frame); swing about a horizontal axis at the top of the spindle, tangent to the arm's circle |
| Stalactites (four seeds) | vanilla `pointed_dripstone` models (down: base, middle, frustum) scaled | Static; drawn only for the seeds put in | Hang from the ceiling at the 2 × 2 points |
| Growing tip | vanilla `pointed_dripstone_down_tip` scaled | Grows from 0 to full length over the growth; shakes on the crack | Top of the tip at the bottom of its seed |
| Falling tip | same model | Drops with a slight turn and a bounce into the tray | Its own centre |
| Reservoir water | water quad at a height from the tank level | Level only | n/a |
| Lamp | family lamp (three lit textures, dark when unlit) | Swaps state | n/a |
| Tray items | the family `TrayItems` helper | 0–3 Pointed Dripstone, from stored count | n/a |
| Back shaft stub | the shared `cobble_generator_shaft.json`, unchanged | Kinetic angle | as the family |

Everything freezes where it is when idle, as the family (a part-done growth or a half-swung hammer stays put).

## Fun Score (1-10)

9. The wind-up, hold and whack land like a cartoon gag on a real mechanism; players will watch a few cycles.

## Builder Notes

- Build on the process-box kit in a new generator `design/models/pointed-dripstone-generator.gen.js`, reusing the
  Cobble Generator generator's shell, window, socket, lamp and tray as they are, so both blocks match exactly. The kit
  must keep producing identical output for the blocks already built on it (regenerate and compare).
- Model with the **window north**, the shaft at the back (south); keep x/y 6–10 clear from z 12.5 to 16 for the shared
  stub. Floor top y 2, as the family.
- Clearances: the hammer's swing must clear the window glass and the neighbouring stalactites at every index; check the
  head against each seed and against the reservoir at the top of its swing (self-collision check).
- The stalactites are vanilla's models, not repainted copies: draw them with the renderer, scaled, like the harvester's
  real plants. Show them in the preview with stand-in geometry only for review; never export stand-ins.
- Reservoir glass: if it's part-clear, give it its own translucent part (the Battery's `battery_glass` rule); opaque
  glints on clear pixels are fine in cutout.
- No light level on the block. The lamp glows full-bright via `neoforge_data` only.
- Item model: real geometry at the standard angle: the block with its four stalactites and the hammer shown.
- Preview in Blockbench, at 64 and 256 RPM: the knock (wind-up, hold, whack, crack, drop, settle) on each of the four
  stalactites in turn; running, full, off, no water (empty reservoir), three seeds, no rotation; the Wireless top. Run
  `zfight-check.js` and the export checks before anything goes into the game.
