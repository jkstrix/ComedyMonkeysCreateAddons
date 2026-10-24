# Design Brief: Upgraded Chest

Redesign of an existing block. What the code does today is in `design/dossiers/upgraded-chest.md`. Family: the
Upgraded Barrel (`design/briefs/upgraded-barrel.md`), designed alongside it.

## Hard Requirements

From the user:
- "They are bad": the current textures go (vanilla oak with a recoloured trim).
- **Create inspired.**
- Scope: **textures plus fittings**. New textures and small extra geometry the tiers can grow into; the same chest
  shape and lid hinge; no code changes.
- **Wood in a metal frame**, like Create's casings: dark stained wood panels framed by the tier's metal, so it still
  reads as a chest.
- **Fittings grow per tier**: each tier adds build as well as changing metal, up to Netherite in full armour.
- The barrels get the same treatment in this pass and go into the game together.
- The house style: dark, muted palette, crisp bevels, accents as trims, never large saturated areas; previewed in
  Blockbench first. Nothing on `design/hands-off.md` changes.

From the code:
- Body 1..15 x 0..10 x 1..15, lid 1..15 x 10..14 x 1..15, latch 7..9 x 7..11 x 0..1, modelled facing north
  (`upgraded_chest_single_{body,lid,closed}.json`). New fittings stay inside the block and sit with the part they
  move with: lid fittings in the lid model, body fittings in the body model.
- The lid opens about **x at y 10, z 15** (the top back edge), 0 to 90 degrees (`UpgradedChestRenderer.java:28-29,60`).
  Nothing on the body may stand above y 10 at the back, or the open lid cuts through it.
- A closed chest draws everything from the block model; an opening one draws the body from the block model and the lid
  from `upgraded_chest_<tier>_<type>_lid` (`UpgradedChestRenderer.java:43,51`). Lid fittings must appear in both the
  `_closed` and `_lid` models, body fittings in both `_closed` and the open body model.
- Per tier and type the models `upgraded_chest_<tier>_<single|left|right>{,_closed,_lid}` already exist; today they
  only set textures on shared parents. They can carry their own elements instead (no code change).
- Double chests are two blocks, `left` and `right`, each 15 px wide toward the join (`upgraded_chest_left_body.json`):
  the pair must read as one chest (no frame down the middle of the front, one latch at the join).
- Lid renders as cutout; item models parent the closed single chest.

## Asset Type

Block, 16 px, `java_block`, four horizontal facings; 5 tiers x single / left / right; static models plus the lid the
renderer swings.

## Family

Upgraded Chest and Upgraded Barrel share the five tiers and one tier language: the **same metal, the same frame
profile and the same fittings ladder**, so a Gold chest and a Gold barrel side by side look like a matched set.
Both use the dark stained wood of the house palette.

## Fantasy

A storage chest built the Create way: dark stained planks set in a riveted metal frame, like a casing you can open.
Upgrade it and the workshop bolts on more metal: a Netherite chest looks like it would shrug off a creeper.

## Hero Element

**The lock plate on the front**, growing with the tier from a small copper hasp to a heavy Netherite lock, framed by
the tier's corner fittings. It's what the eye lands on, and it says the tier before the colour does.

## Theme

Create's casings (andesite, brass, copper casing: a wooden core in a metal frame), train casing's riveted steel, and
Create's own vault for the sense of a strongbox. Mechanical-workshop, hand-built, bolted.

## Shape Language

Square, framed, bolted. Flat panels inset slightly inside a raised frame; fittings are plates and caps with rivets.
No round forms except rivet heads and the diamond studs.

## Silhouette Description

The vanilla chest silhouette, kept: a box with a lid, the lock at the front centre. From Gold up, corner caps stand
a quarter pixel proud at the top corners, and the lock grows; Netherite gains plated corners all round. Recognisable
as a chest at inventory size; tier recognisable by its metal and the size of the lock.

## Primary Forms

- Body and lid as today, painted as **dark walnut planks** (`#3b2a1f` `#4e3626` `#634532`), horizontal, inset inside
  a metal **frame** around every face's edge (1 px, bevelled: lit top-left, dark bottom-right), like a casing.
- The lid's frame and the body's frame meet at the lid line with a dark gap, so the lid reads as a separate part.

## Secondary Forms (the tier ladder)

Every tier keeps the lower tier's fittings and adds its own, in its own metal:

| Tier | Metal (dark to light) | Adds |
|---|---|---|
| Copper | copper `#7a4a2e` `#9c5f3a` `#b87a4e` | the frame; a small copper hasp latch |
| Iron | steel `#2f343b` `#3c424a` `#5f656e` | a riveted band across the body and the lid; a hasp with a ring |
| Gold | muted brass `#8f7638` `#b08f45` (`#c9a75a` highlights only) | corner caps (geometry, a quarter pixel proud) at the lid's top corners; a lock plate with a keyhole |
| Diamond | dark steel frame, small muted cyan studs `#6fb7c4` `#4f8a96` | diamond studs at the frame corners and set in the lock |
| Netherite | netherite `#2b2729` `#3a3438` `#4a4247`, warm grey highlights `#6b6266` | plated corners on body and lid (geometry), the wood only in recessed panels, a heavy lock |

## Interesting Features

- The lock grows each tier (geometry): copper 2x3, iron 2x4 with a ring, gold 3x4 plate, diamond 3x4 with a stud,
  netherite 4x5. It stays on the lid as today, so it lifts with it.
- Rivets as single highlighted pixels at frame corners and along the bands, two shades only (no speckle).
- Inside: dark wood with the frame's rim showing when the lid's up; the same for every tier.

## States

| State | What changes | Carried by |
|---|---|---|
| Tier | metal colour, fittings and lock size | per-tier textures and per-tier model elements |
| Open | the lid swings up; the inside shows dark wood with a metal rim | the existing lid model, `inner` texture |
| Single / double | a double chest reads as one: frame round the pair, one lock at the join | left/right textures and models |

## Rig Requirements

No new moving parts. The lid group pivots at (8, 10, 15) about X, as now; lid fittings and the lock belong to it.

## Fun Score (1-10)

8. The ladder makes upgrading visible and satisfying, and it finally looks like Create.

## Builder Notes

- Refresh-plus: keep the existing geometry and part split; add fittings as small elements. Start from scratch on
  textures.
- A generator script (`design/models/upgraded-chest.gen.js`) paints the textures per tier and writes the per-tier
  models, so the five tiers and three types stay consistent. Preview all five tiers, single and double, open and
  closed, in Blockbench, beside the barrels.
- Export to the existing file names (dossier Code Contract). Shared `inner`/`bottom` textures may stay shared.
- Run the z-fight check on fittings (overlays 0.25 px apart) and check the open lid clears the body's back.
- Before: `design/dossiers/upgraded-chest/textures-before.png`.
