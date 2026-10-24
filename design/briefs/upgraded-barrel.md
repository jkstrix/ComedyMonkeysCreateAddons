# Design Brief: Upgraded Barrel

Redesign of an existing block. What the code does today is in `design/dossiers/upgraded-chest.md` (chests and barrels
share it). Family: the Upgraded Chest (`design/briefs/upgraded-chest.md`); read that brief first, this one only adds
what's different.

## Hard Requirements

From the user (as for the chest):
- Create inspired; wood in a metal frame; the fittings grow per tier; textures in this pass, alongside the chests.
- The house style; previewed in Blockbench first; nothing on `design/hands-off.md` changes.

From the code:
- A full cube, `minecraft:block/cube_bottom_top`, per tier `upgraded_barrel_<tier>` and `upgraded_barrel_<tier>_open`,
  textures `upgraded_barrel_<tier>_{side,top,top_open,bottom}` (`models/block/upgraded_barrel_*.json`).
- Six facings (`UpgradedBarrelBlock.java:47`): the top is the opening face, so it must read from any direction.
- Open swaps the top to `_top_open` (blockstate `open=true`).
- It stays a full cube (it's a full, occluding block): **textures only, no added geometry**.

## Asset Type

Block, full cube, 6 facings, 5 tiers x open/closed.

## Family

As the chest: the same metal per tier, the same 1 px bevelled frame, the same dark walnut, the same fittings ladder
in paint.

## Fantasy

A Create-style storage barrel: dark vertical staves held in the tier's metal, more metal each tier.

## Hero Element

**The lid on top**: a dark wood lid in a metal rim with the tier's lock plate and fittings, matching the chest's lock.

## Primary Forms

- Sides: dark walnut **vertical staves** inside the metal frame at the top and bottom edges (the barrel's hoops), plus
  the frame down the corners so it reads like a casing.
- Top: a framed lid of planks with the tier's hasp or lock in the middle; open: a dark interior inside the rim.
- Bottom: framed planks, plain.

## Secondary Forms (the tier ladder, in paint)

Copper: the frame. Iron: a middle hoop with rivets. Gold: corner brackets. Diamond: studs at the corners. Netherite:
plated corners and the heaviest lock, wood only in the recessed staves. Same metals as the chest.

## States

| State | What changes | Carried by |
|---|---|---|
| Tier | metal and fittings | per-tier textures |
| Open | the lid shows the dark inside within the rim | `_top_open` |

## Fun Score (1-10)

8, as a set with the chest.

## Builder Notes

Painted by the same generator as the chests (`design/models/upgraded-chest.gen.js`), previewed beside them. Export to
the existing texture names; the models don't change. Before: the last two columns of
`design/dossiers/upgraded-chest/textures-before.png`.
