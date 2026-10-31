# Texture Review: Clockwork Wastes, the rest of the lot (batches 2–4)

Family: Conduit Pylon, Derelict Star Reactor, Star Reactor; the broken machines (Press, Mixer, Deployer, Mechanical Arm, Crushing Wheel), Worker and Resistance Shells, Charging Alcove, Maker Statue, Scrap Heap, the Unknown Device and Clockwork Scrap icons; the Scuttler, Sentry and Custodian. Compared with batch 1 (`clockwork-wastes-city.md`), `design/visual-language.md`, Trade Bot, the Juggernaut and the Industrial Battery  ·  Briefs: design/briefs/clockwork-wastes-blocks.md, clockwork-wastes-bots.md  ·  Models: design/models/clockwork-wastes-{reactors,salvage,scuttler,sentry,custodian}.bbmodel  ·  Date: 2026-10-29

Screenshots (in `clockwork-wastes-lot/`): `1-reactors-gameplay`, `1b-pylon-crowns`, `1c-derelict-reactor`, `1d-star-reactors`, `2-salvage-gameplay`, `2b-machines`, `2c-shells`, `2d-decor`, `3a-scuttler`, `3b-sentry`, `3c-custodian`, `3d-custodian-face`; the bots' atlases `clockwork-wastes-{scuttler,sentry,custodian}.png`; the icons in `clockwork-wastes-salvage/`.

## Design Intent

The same three layers as batch 1:
- **The makers' Create kit, broken and aged:** salvage machines and the statue.
- **The uprising's void-alloy and starlight:** pylons, the derelict reactor, the alcove, the Worker and all three bots.
- **The war on both.**

The resistance adds the makers' own materials, field-repaired and never lit. The crafted Star Reactor is the player's world (a new brass casing) holding the uprising's core.

The bots share one face, "eyes like ours": a lens in a ring with a hand-placed catchlight (Trade Bot's pods). Their hostility shows in **one ember pixel**, never a red-lit face.

## Current Strengths

- **Each layer reads at gameplay distance** (`2-salvage-gameplay`): grey Create casings, near-black uprising pieces, stone statues, the brass-and-andesite resistance.
- **The broken machines carry Create's casing language without copying it.** The frame, the inset and rust runs from the top corner all come straight from the visual-language rules.
- **The resistance reads as the makers' kin:** andesite torso, brass straps, two glass eyes in brass rims, a walnut patch. It's clearly not the uprising.
- **The Maker Statue standing** is the batch's best storytelling: carved End Stone, brass trims, and the blank void-alloy plate clamped over its face.
- **The Custodian's pectoral** (the broken chain) and its lit wrist seams are the right amount of light on a dark body.
- **Starlight stays edge tone on seams and grooves,** with core pixels only at lens centres and pylon cores.

## Current Weaknesses

1. **Lit lenses read as flat lavender squares.** On all three bots, the lit lens is a block of edge tone `#7f76a8` with a core patch. There's no ring or depth and no catchlight, so it doesn't read as an eye. The Custodian's three stacked lenses merge into one bright lavender stripe down its face (`3d-custodian-face`), the brightest, largest lit area on any bot. That breaks the glow rule (core and edge tone on small areas only) and the family face (Trade Bot's catchlight).
2. **Old brass looks new.** Brass on the salvage machines (the Mechanical Arm's elbow, ring and claw, the Deployer's hand) and on the resistance (drum, key, shoulders) uses mid brass with frequent `#a3873f` highlights. It reads as saturated mustard, as fresh as the crafted Star Reactor. Story: the makers' kit and the resistance are ages old.
3. **The Scrap Heap is confetti.** `scrap_mix` picks one of eight colours per pixel at random: rust, iron, steel, soot and brass. At distance it reads as orange-and-black speckle (`2-salvage-gameplay`): noise with no reason.
4. **The toppled statue reads as a wooden crate.** `carved_fold` paints a regular vertical line every 4 px. Standing, these read as folds. Lying down, beside its brass bands, they read as planks (`2d-decor`).
5. **The Custodian's war scorch doesn't show.** The robe hem's soot (`#26211c`) is the same value as void-alloy (`#1d1c25`), so the brief's "scorched hem" is invisible.
6. **The Star Reactor's brass casing is the most saturated surface in the Wastes** (`1d-star-reactors`). Mid brass is right for a new, player-made casing, but the frequent `#a3873f` highlight pixels make it read yellow beside the dark city.
7. **The Crushing Wheel doesn't read as a wheel** (`2b-machines`, far left): dark stone slabs and loose teeth. That's partly shape (for the modeller): the half-wheels' curve is lost at this size. Texture can help with ridges running round the rim, not straight across.
8. **Technical:** the Custodian's 256 px atlas uses its top 40 rows only, and the Scuttler and Sentry similarly. The atlases can be smaller (the texel density doesn't change).

## Storytelling Evaluation

- **Who made it:** clear for every piece except the brass, which can't tell old makers' brass from new player brass.
- **How old:** the makers' layer is aged (rust, dust, cracks) but its brass is new. The uprising's bots look clean, which is right because they slept powered down indoors. The Custodian alone fought at the walls, and its scorch doesn't show.
- **Who repairs it:** the resistance's field patches and straps say "repaired by hand". Good.
- **Expense:** the crafted Star Reactor should look precious and new, and it does. It's just a little loud.

## Material Evaluation

| Material | Reads? | Matches family? |
|---|---|---|
| Andesite casing (aged) | Yes | Matches batch 1's aged andesite and Create's language |
| Mid brass | Yes, too bright where old | Right for the Star Reactor; wrong age for salvage and the resistance |
| Void-alloy (bots, box UV) | Yes, crisp, no mottle | Matches the blocks' void-alloy: same tones, same facet edge |
| Starlight (lenses) | Too big and flat on lenses | Seams and grooves match batch 1; lenses don't follow the single-pixel core rule |
| Carved End Stone | Yes standing; not lying | New: smooth stone, sits right beside the Wastes' brick |
| Scrap | No (noise) | Should be built from the family's own materials in readable pieces |
| Spring steel | Yes (the resistance's split spring) | Matches the Mainspring, as fixed in batch 1 |

## Minecraft Readability Evaluation

- **Primary:**
  - bots' lenses: too loud on the Custodian, too flat everywhere;
  - pylon cores: small but fine;
  - the star in its reactors: reads well through the clear ports.
- **Secondary:** casings, robes, frames. Good.
- **Tertiary:** rust runs, dust, tags, patches. Good. The scrap heap breaks this tier by putting tertiary noise on a whole block.
- **Texel density:** one texel per pixel on blocks. Bots are box UV at 1:1.
- **Zoom-only detail:** none. The walnut patch and stamp read at three-quarter view.

## Cohesion Evaluation

- **Shared materials:** consistent, except brass age (weakness 2). That's a new rule the visual-language doc doesn't yet have.
- **Manufacturing language:**
  - makers: bolted and framed;
  - uprising: cast and clamped;
  - resistance: makers' materials, field-patched.

  All three hold.
- **Colour logic:** starlight = the uprising's power, everywhere. Ember = hostile, but it's only one pixel if the lens art is fixed. Amber doesn't appear in the Wastes, which is right.
- **Age:** the brass is the one break.

## Recommended Texture Changes

1. **Lens art for every bot** (`lensPaint` in clockwork-wastes-bots.gen.js), so it reads as an eye and keeps the light small:
   - a 1 px dim ring `#3a3552` round the lens edge (the glass rim);
   - the edge tone `#7f76a8` only on a ring inside it;
   - a dark glass centre `#2a2833` with the core `#b8b0d8` as a single pixel;
   - one ember pupil pixel `#b5452f` beside the core when hostile;
   - a hand-placed catchlight pixel (`#b8b0d8`) at the top-left of every lit lens, as Trade Bot.

   For 3 px lenses: the ember pupil in the middle, the catchlight top-left, the rest dim ring.
2. **Aged brass** for the makers' and the resistance's work: a new `brass_aged`, `#6e5a36` `#7e6832` `#8f7638`, no `#a3873f`, with a few patina pixels `#4f6b5e` in recesses. Apply it to:
   - the salvage machines (Arm elbow, ring and claw; Deployer hand; Press details);
   - the Resistance Shells (drum, key, shoulders, rims, straps);
   - the statue's trims;
   - the alcove's toy cart wheels.

   The Star Reactor keeps mid brass: it's new.
3. **Scrap in pieces** (`scrap_mix`): chunks of 2–3 px of one material each, outlined by a dark gap `#1a1714`. The chunks are rust-streaked iron plates, a steel bar, one or two aged-brass cog teeth, and soot in the hollows. Something to pick through, not static.
4. **Statue folds that read as cloth** (`carved_fold`): fold lines at irregular spacing (3, 5, 4, 6 px) that start and stop and taper, darkest at the hem, gone by the chest. No continuous parallel lines.
5. **The Custodian's scorched hem as heat-tint:** the bottom 2–3 rows of the robe plates in a warm dark `#3a2e24` with `#4a3a2e` streaks running up. That's heat discolouration on metal, which reads against void-alloy where soot doesn't.
6. **Quieter Star Reactor brass:** cut the `#a3873f` highlight from about 10% of pixels to about 3%, so the casing reads as brass, not yellow. Same palette and frame.
7. **Crushing Wheel ridges round the rim** (`wheel_stone`): ridges following the wheel's curve on its faces, and the teeth in aged steel, not bright steel.
   - **For the modeller:** a full stepped-disc half-wheel would carry the curve better than stacked rows.
8. **Smaller atlases** (technical): the Scuttler and Sentry at 64 px, the Custodian at 128 px.

## Recommended Retexture Candidates

Now that these assets exist, are there earlier assets that should be revisited for visual consistency?

| Asset | Reason for review | What has changed | Proposed update |
|---|---|---|---|
| Substation (batch 1) | Its brass pivot boss and contact jaws are makers' work, ages old | Change 2 adds aged brass for old work | Boss and jaws to `brass_aged` |
| Material sheet (batch 1) | Its brass swatch and winding key show only mid brass | Aged brass becomes a separate material | Add an aged-brass swatch; the resistance's winding key in aged brass |
| Worker Shells (this batch) | They're the uprising's dead; the lens rule now differs for dead and lit eyes | Change 1 rewrites the lit lens | None: dead lenses already have dark glass and one catchlight. Check they match the new lit lens's ring |
| Trade Bot, Helper Bot | The family face the bots echo | Nothing on their side | None. Their catchlight is the reference the new lens art follows |

## Art Director Verdict

**Approved with changes.** The lot reads as one world with three clear makers. The resistance and the standing statue add story without a word of text.

Before modelling is finished, these need doing:
- **Change 1 (lens art):** the bots' faces are their hero, and right now they're the loudest, flattest thing on them.
- **Change 2 (aged brass)** together with its two retexture candidates, so brass tells the age story everywhere.
- **Changes 3–5:** they fix specific misreads (confetti, crate, invisible scorch).

## Follow-up

User's picks (2026-10-29): all of changes 1–8; both retexture candidates; both visual-language rules.

Applied in the generators:
- **1.** Lens art (`lensPaint`, clockwork-wastes-bots.gen.js): corners clear, a dim rim, edge tone only inside it, one core-pixel catchlight top-left, one ember pupil pixel. The Custodian's face column is no longer a lit stripe.
- **2.** `brass_aged` (`#6e5a36` `#7e6832` `#8f7638`, odd patina pixel) on all salvage, the Resistance Shells, the statue's trims and the toy cart (the salvage generator's brass is aged throughout); the bots' makers' plate too.
- **3.** Scrap in 3 x 2 pieces with dark gaps.
- **4.** Statue folds at irregular spacing, starting at their own heights. Toppled, it still reads a little like a crate: partly shape (the brass bands across it); for the modeller if you want it changed.
- **5.** The Custodian's hem heat-tinted (`#3a2e24`, streaks `#4a3a2e`).
- **6.** The Star Reactor's brass highlight cut to about 3%.
- **7.** Crushing Wheel ridges concentric round the axle; teeth in darker steel.
- **8.** Atlases: Scuttler 64 px, Custodian 128 px; the Sentry stays at 128 (its parts don't fit 64).
- **Retexture candidates:** the Substation's boss, jaws and pin in aged brass; the material sheet has an aged-brass swatch and the winding key in aged brass.
- **Visual language:** brass ages (Materials), the lens rule (new section), heat-tint for fire on void-alloy (Age and wear).

After shots: `clockwork-wastes-lot/after-*.png`. No cross-texture z-fights in any block model; the bots' collision results unchanged.
