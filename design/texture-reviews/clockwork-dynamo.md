# Texture Review: Clockwork Dynamo

Family: Trade Bot, Helper Bot (material family); Electric Motor, Lava Generator, Geothermal Turbine (electric neighbours)  ·  Brief: design/briefs/clockwork-dynamo.md  ·  Model: design/models/clockwork-dynamo.bbmodel (generator `clockwork-dynamo.gen.js`)  ·  Date: 2026-10-31

Screenshots judged from:
- Gameplay distance (about 6 blocks), beside the motor: `clockwork-dynamo/gameplay-distance.png`
- Inventory size: `clockwork-dynamo/inventory-size.png`
- Block size, front and back: `../models/clockwork-dynamo-review/front-3q-working.png`, `back-3q-idle.png`
- Primary close-up: `../models/clockwork-dynamo-review/hero-commutator.png`
- Family side by side: `../models/clockwork-dynamo-review/family-with-motor.png`, `facings-up-down.png`
- Palettes sampled from the shipped `entity/trade_bot.png`, `entity/helper_bot/*.png` and `block/electric_motor_*.png`.

## Texture Contract

New asset; nothing in the mod yet. Textures will be `block/clockwork_dynamo_*.png`, 16 × 16, painted by the generator. Faces map 1:1 by position, except the bearing boss (2 texels a pixel, approved so the ring is round), the band (explicit strips so the rivet line follows it) and the vents (explicit 4 × 7). Glow: the lamp lens (`neoforge_data` full-bright). Borrowed: Create's `block/axis` and `block/axis_top` for the shaft stub.

## Design Intent

A dynamo the bots built from their own parts: a bot's chassis (andesite panels in a dark-steel stepped frame), a bot's riveted brass waist band, a bot's copper aerial with its brass bead. It must read as the bots' work, never as a face, and never as the turbine family. The hero is the copper commutator spinning in the top window. Themes: mechanical, friendly-industrial, clockwork; electricity as something the automatons understood.

## Current Strengths

- **The material split carries the story**: andesite fields, dark-steel steps, one brass belt, copper only where current runs (commutator, brushes' holders, contacts, aerial). It reads as "bot casing" at a glance.
- **The steel matches the bots exactly** (`#2f343b` `#3c424a` `#4a5059`, the Trade Bot's three most-used steel shades).
- **The hero works**: warm copper in a dark well behind a cool steel rim is the strongest contrast on the top.
- **Restraint held**: brass is one band plus small trims; no stripes, no hazard marks, no saturated areas.
- **No face from any side** after the build fixes (stacked contacts, three-slot vents, lamp beside the aerial).
- At 6 blocks it's unmistakably different from the motor (box and belt against a round ribbed can).

## Current Weaknesses

1. **The commutator bars are 16 texels on a 1 px face.** Each bar squeezes the whole 16 × 16 texture into under a pixel, so the close-up shows fine pinstripes that only exist zoomed in, and at distance they blur to an even brown. The hero loses its segmented read exactly where it matters.
2. **The lamp is a step brighter than every other electric lamp**: `#8fcad4` fill with `#6fb7c4` edges, against the motor and generators' `#6fb7c4` fill with `#4f8f99` edges. Full-bright, it's the palest thing on the block, heading for the near-white the house style rules out, and "cyan = making power" stops being one light.
3. **Steel steps are checkered.** The 1 px stepped edges use the 3-shade steel mottle at 20/62/18%. At that width each pixel is a different shade, so every edge reads as a dotted line. The 12 × 14 base plate's back looks blotchy for the same reason. The process-box lesson ("big panels crisp, not mottled") applies.
4. **The andesite is the process boxes', not the bots'.** The field is mostly `#73766f` (the generators' andesite). The Trade Bot's panels are mostly `#7c7f79` with `#6b6e6a` and `#8a8c86`. Side by side with the bots it would look a shade duller than the bodies it claims to be built from.
5. **The aerial's bead is generic mid brass.** It's the one part lifted straight off the Trade Bot, but it's painted in the generators' `#7e6832`–`#a3873f` mottle instead of the bot's own bead brass (`#b08f45` with a `#c9a75a` highlight).

## Storytelling Evaluation

- **Who made it**: the bots, in the fiction; the player crafts it with andesite casings. The textures say so: andesite-in-steel with stepped corners, the riveted band and the whisker grille are all bot parts.
- **Who uses it**: anyone with a shaft to spare; it runs unattended.
- **How old**: new. Factory-fresh is right for a crafted block; wear has no reason yet. (The Clockwork Wastes branch may want an aged salvage look; that's for that branch.)
- **How often / where**: constantly, beside water wheels, windmills and boilers. Nothing it does would stain it: no heat, no exhaust.
- **How expensive**: brass tier. The precision of the band's rivets and the clean copper fit that.
- **Who repairs it**: nobody; no maintenance marks are needed.

## Material Evaluation

| Material | Reads? | Against the family |
|---|---|---|
| Dark steel | Yes | Same shades as the bots. Too mottled on 1 px steps and the base (weakness 3) |
| Andesite | Yes | A shade darker than the bots' panels (weakness 4) |
| Brass (band, lamp rim, collar) | Yes | Mid brass `#8f7638` as on the generators: right for a block (house brass reads too bright on blocks). Bead should be the bot's brass (weakness 5) |
| Brass edge `#6e5a2a` | Yes | The bots' own dark brass: matches |
| Copper | Yes | `#7a4a2e` `#8a5434` `#9c5f3a` + `#b87a4e`, the same as the motor's windings and the bots' fork and aerial |
| Carbon / dark | Yes | `#26272a` `#323337`, the bots' rubber shades |
| Glow | Too bright (weakness 2) | Off state `#1e3236` is the Helper Bot's switched-off iris: a good bot tie |

## Minecraft Readability Evaluation

- **Primary**: the commutator window (top) and the brass bearing ring (front). Both carry the strongest contrast. The window loses its segments at distance (weakness 1).
- **Secondary**: the band, the stepped steel frame, the base plate. They support it; the checkered steps add noise (weakness 3).
- **Tertiary**: rivets, brushes, springs, vent slots, aerial. None dominates; the rivets now read as rivets.
- **Texel density**: 1:1 everywhere except the commutator bars (16:1, unintended, weakness 1) and the bearing boss (2:1, approved for roundness, like the motor's end bell).
- **Inventory size**: reads as a grey box with a brass belt and a ring. Fine.
- **Gradients and clutter**: none.

## Cohesion Evaluation

- **Shared materials**: steel, copper, dark brass and carbon match the bots exactly; andesite and the bead don't yet (weaknesses 4, 5).
- **Manufacturing language**: stepped corners, riveted band, whisker grille, aerial. A player who knows Trade Bot will believe the same hands built it. It doesn't borrow the turbine kit's staves or bezels. Good.
- **Colour logic**: copper sits only where current runs (commutator, contacts, aerial), as on the motor's windings: worth writing down as a rule (below). Cyan should mean "making power" on every electric block, so the lamp should match the electric family exactly (weakness 2).
- **Age**: new, like every crafted machine in the mod. Consistent.

## Recommended Texture Changes

1. **Commutator bars 1:1** (`bar_a`, `bar_b`, the 16 bars' faces): map each bar at one texel a pixel. Each bar becomes a flat copper shade, alternating `#9c5f3a` / `#8a5434`, with one `#b87a4e` highlight texel on the outward face and a single `#2a2420` mica texel on one edge. *Why*: the drum then reads as distinct copper segments at every distance, and the spin shows as bars passing.
2. **Lamp to the electric family's cyan** (`lamp_cyan`): fill `#8fcad4` → `#6fb7c4`, edge `#6fb7c4` → `#4f8f99`, as on the motor and generators. Keep the off lamp `#1e3236`. *Why*: one meaning, one light; it stops being the palest thing on the block.
3. **Calm the steel** (`steel` on the steps, `base` field): from the 3-shade mottle to the kit's near-flat field: `#3c424a` with a rare `#40464e` (as on the process boxes). Keep the base's bevel (`#555b63` / `#2a2f35`), its corner bolts and the contact pad. *Why*: edges read as clean frame lines instead of dotted ones; the back stops looking blotchy.
4. **Andesite to the bots' panel shade** (`field`): mostly `#7c7f79`, with a sparse `#73766f` and a rare `#8a8c86` (from mostly `#73766f`). *Why*: it's built from bot casing, so it should match the bot's panels. Still mid grey (the house ceiling is `#8a8c86`).
5. **The bead in the Trade Bot's brass** (aerial bead): `#b08f45` with one `#c9a75a` highlight on top (from the mid-brass mottle). The collar and the band stay mid brass. *Why*: it's the bot's own part, the one place the bots' brighter brass belongs; and it helps the small aerial read.

## Recommended Retexture Candidates

Now that this asset exists, are there earlier assets that should be revisited for visual consistency?

**None.** The Dynamo borrows from two approved families without changing what any of their colours mean, and the changes above move it onto their exact shades rather than asking them to move. Checked:
- *Electric Motor, Lava Generator, Turbine*: their cyan lamps and copper are the reference this review matches; no change.
- *Trade Bot, Helper Bot*: their steel, andesite and brass are the reference; no change.
- *The motor's side-by-side base contacts*: on a round turbine-family motor they don't read as eyes (no bot context, the can dominates); no change.

## Visual-Language Updates

`design/visual-language.md` didn't exist, so this review drafted it from the approved assets (marked draft). Changes it records from this review, for approval:
- **Copper means current.** Copper goes where electricity flows: windings, commutators, contacts, power lines, aerials.
- **Cyan means making power.** One lamp colour on every electric block: `#4f8f99` edge, `#6fb7c4` centre. Dark red means short of power; dark teal `#1e3236` means a bot device at rest.
- **Brass on blocks is mid brass**; the bots' brighter `#b08f45` is for bot bodies and the bot parts a block borrows (like the Dynamo's bead).
- **Bot makers** in the Makers section: stepped corners, andesite in steel, riveted brass band, whisker grilles, aerials.

## Art Director Verdict

**Approved with changes.** The Dynamo strengthens the mod's identity: it's the first block that clearly belongs to the bots, and it gives the Wastes backstory a believable piece of their engineering. The five changes are small and only move it onto the family's exact shades; change 1 matters most, because it's the hero. No earlier assets need revisiting.

## Follow-up

User's picks (2026-10-31):
- **Applied** in `clockwork-dynamo.gen.js`: 1 (commutator bars 1:1: flat copper per bar, alternating `#9c5f3a` / `#7a4a2e`, a highlight row; the darker alternate bars do the mica's job, since a single-texel mica line can't sit on a 1 px bar), 2 (lamp `#4f8f99` / `#6fb7c4`, the motor's painter), 4 (andesite mostly `#7c7f79`), 5 (bead `#b08f45`, top `#c9a75a`). After shots: `clockwork-dynamo/after-*.png`.
- **Declined**: 3 (calmer steel). The bots' three-shade steel stays on the frame and the base.
- **Retexture candidates**: none.
- **Visual language**: `design/visual-language.md` approved (its steel rule no longer includes the declined edge change).
- **Approved** by the user in Blockbench (2026-10-31). The model is finished; sound and the move into the mod are separate steps.
