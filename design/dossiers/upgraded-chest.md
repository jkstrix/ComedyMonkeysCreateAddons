# Code Dossier: Upgraded Chest (and Upgraded Barrel)

## Identity
`upgraded_chest` (block with block entity, single or double) and `upgraded_barrel`, one block each with a `tier` property:
copper, iron, gold, diamond, netherite (`content/chest/ChestTier.java`). Classes: `UpgradedChestBlock`,
`UpgradedChestBlockEntity`, `UpgradedChestRenderer` (lid only), `UpgradedBarrelBlock`, `UpgradedBarrelBlockEntity`.
Footprint: one block (a double chest is two `left`/`right` blocks).

## What It Does
1. A chest upgrade item, used on a vanilla chest or barrel (or a lower-tier upgraded one), turns it into an upgraded
   chest or barrel of that tier and keeps its items (lang `copper_chest_upgrade.tooltip.*`, ponder `chest_upgrades`).
   One upgrade covers a whole double chest.
2. Each tier doubles the items per slot: Copper x2, Iron x4, Gold x8 (`CMConfig.java:492`), Diamond keeps x8 and
   doubles the slots, Netherite doubles the slots and raises the stack size again (`ChestTier.java:36`).
3. Netherite is blast-proof (`ChestTier.java:41`).
4. They work in Create contraptions as mounted storage (`UpgradedChestMountedStorage`).

## What the Player Is Told
"Copper Chest", "Iron Chest"... and "Copper Barrel"... (`ChestTier.java` `chestName`/`barrelName`). The upgrades are
made on a Create sequenced-assembly line from a Chest Upgrade Template and the tier's sheets or ingots (lang
`chest_upgrade_template.tooltip.summary`). The fantasy: a chest reinforced with the tier's metal.

## States
| State | Source | How the player can tell today |
|---|---|---|
| tier (5) | `UpgradedChestBlock.java:58`, `UpgradedBarrelBlock.java:48` | trim colour only; the wood is identical |
| open / closed | `UpgradedChestBlock.java:60`, `UpgradedBarrelBlock.java:49` | chest lid swings; barrel top swaps to `_top_open` |
| single / left / right | `UpgradedChestBlock.java:57` | the double chest's halves share one trim |
| facing | chest: horizontal (`:56`); barrel: all six (`UpgradedBarrelBlock.java:47`) | |

## Moving and Changing Parts
| Part | Model file | Driven by | Axis / pivot / range | Source |
|---|---|---|---|---|
| Chest lid (with latch) | `block/upgraded_chest_<tier>_<type>_lid` | `OPEN` + block-entity openness, vanilla easing | X about y 10, z 15 (top back edge), 0 to 90 degrees | `UpgradedChestRenderer.java:28-60` |
| Barrel top | `upgraded_barrel_<tier>_open` model | `OPEN` blockstate | texture swap | blockstates `upgraded_barrel.json` |

## Code Contract
- Chest models: shared geometry `upgraded_chest_{single,left,right}_{body,lid,closed}.json` (body 1..15 x 0..10 x 1..15,
  lid 1..15 x 10..14, latch 7..9 x 7..11 x 0..1), plus per tier and type `upgraded_chest_<tier>_<type>{,_closed,_lid}`
  that only set textures `front, back, side, top, inner, bottom`.
- Closed chests draw the whole chest (lid included) from the block model; the renderer draws only an opening lid
  (`UpgradedChestRenderer.java:43`). The lid model's hinge must stay at y 10, z 15.
- Models face north; the renderer turns the lid by `-(facing.toYRot() + 180)` (`:56`).
- Textures: 16x16, per tier `upgraded_chest_<tier>_{front,side,top,front_left,front_right,back_left,back_right,top_left,top_right}`,
  shared `upgraded_chest_{inner,inner_left,inner_right,bottom}`. Face UVs: front body rows 6-16, lid rows 2-6, latch 7-9 x 5-9.
- Barrel: `cube_bottom_top` per tier with `upgraded_barrel_<tier>_{side,top,top_open,bottom}`.
- Item models parent the closed single chest / the barrel.
- Lid renders as cutout.

## Current Look
Before: `design/dossiers/upgraded-chest/textures-before.png` (rows copper to netherite; columns chest front, side, top,
front-left, front-right, barrel side, barrel top).
- Works: the vanilla chest silhouette is instantly a chest; the trim shows the tier from a distance.
- Doesn't: it's vanilla oak planks with a recoloured trim. Every tier is the same chest in a different colour; the
  trims are bright and saturated (salmon copper, near-white iron, yellow gold, cyan diamond), against the house style.
  Nothing says Create: no casing, no fittings, no rivets. Netherite reads as the dullest, not the best.

## Function the Look Doesn't Show
- How big the upgrade is: Diamond and Netherite double the slots, Netherite is blast-proof, but they only change hue.
- The tiers have no growth in build, so a Netherite chest looks no stronger than a Copper one.

## Look the Code Doesn't Back Up
None.

## History
All chest and barrel textures and models date from the initial check-in (`6a38571`), hand-drawn, no generator.
Survey rank 13 (`design/dossiers/survey.md`): "Use one tier language, with fittings that grow in build per tier, not
just in hue."
