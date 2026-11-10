# Texture Review: Pointed Dripstone Generator

Family: Cobble Generators (Cobblestone, Basalt, Blackstone, Tuff), the process boxes (Auto Harvester, Animal Pens, String Tangler, Stone Infuser)  ·  Brief: design/briefs/pointed-dripstone-generator.md  ·  Model: design/models/pointed-dripstone-generator.bbmodel  ·  Date: 2026-11-10

Screenshots (Blockbench, `demo_fast` paused mid-growth): `pointed-dripstone-generator/inventory.png` (48 px), `block.png`,
`gameplay-distance.png` (about 6 blocks), `hammer-closeup.png`, and `family-cobble-generator.png` (the Andesite Cobblestone
Generator from the same camera).

## Texture Contract

New asset: no existing texture contract. What it shares (so changes to these change both families):
- `comedymonkeysaddons:block/cobble_generator_glass`, `_lamp`, `_lamp_running`, `_lamp_full`, `_lamp_off`, `_chamber`,
  `_floor`, `_ceiling`, `_steel`: the Cobble Generators' own, used as they are.
- Vanilla, by reference: `minecraft:block/dripstone_block` (ceiling), `pointed_dripstone_down_frustum` and
  `_down_tip` (stalactites, renderer-drawn), `minecraft:item/pointed_dripstone` (tray), vanilla water (renderer).
- Create, by reference: `redstone_antenna(_powered)`, `redstone_bridge` (Wireless top), the shared shaft stub.
- Its own, painted in `pointed-dripstone-generator.gen.js`: `pointed_dripstone_generator_front`, `_side`, `_top`,
  `_back`, `_bottom`, `_collar`, `_spindle`, `_hammer`, `_hammer_face`, `_arm` (all 16 × 16).

## Design Intent

A little dripstone cave sealed in the Cobble Generators' steel box: the player's own Create-era machine (maker: the
player's engineers), new and clean. It must read as **dripstone** (water over a dripstone ceiling, brown teeth) and as
a **sibling of the stone generators** (same shell, lamp, glass, tray), with the iron trip hammer as the primary element.

## Current Strengths

- **Family match is exact**: andesite trim, walnut-board top, near-flat steel sides, back socket, lamp and glass are the
  Cobble Generator's own, so the two read as one set side by side.
- **Dripstone reads instantly** at every distance: vanilla's own Dripstone Block and Pointed Dripstone textures, so the
  product is recognised without any repainting, and the blue water band separates it from the cobble generator's lava
  glow at a glance.
- **The iron hammer is the primary element**: the lightest values in the box (`#b1b0b0`–`#c1c1c1`, faces to `#d6d6d6`)
  against dark steel `#3c424a` and the chamber, so the eye goes to it first at gameplay distance.
- **Wear with a reason**: the striking faces are a shade brighter (`#c1c1c1`/`#d1cfcf`) as if polished by the knocks.
- **The water port** on each side is the family's grid-true socket (2 × 2 hole in a 4 × 4 andesite collar, corners off),
  standing where the cobble generator has its inspection plate, so the side still has one centred feature.

## Current Weaknesses

- **Inventory view**: at 48 px from the default item angle the side faces forward and the window, the block's whole
  identity, is barely visible. The game item needs the family's front-forward GUI view (a display-transform job when it
  goes into the mod, not a texture change).
- **Hammer texture ring never shows**: `_hammer` paints a darker `#828282` edge ring, but the head's faces are 1.5 px and
  take an inner 1.5-texel slice of the sprite, so only the mottle shows. Harmless (the head's own shading separates its
  faces), noted so nobody "fixes" a ring that isn't on screen.
- **Two working-tool metals in the family**: the cobble generator's drill is mid steel (`#4a5059`–`#6b7178` flutes);
  this hammer is now vanilla iron. Each is fine alone; side by side the family's "tool that works the product" changes
  metal. See Retexture Candidates.

## Storytelling Evaluation

- **Who made it**: the player's Create-era engineers, in the process-box workshop style. Shown by the shared shell.
- **Who uses it / how often**: a base machine running all day. The polished striking faces are the only use mark, and
  they have a reason.
- **How old**: new, as the visual language asks for crafted machines. Correct.
- **How expensive**: andesite tier (alloy, two drills, casing). No brass anywhere, correctly.
- **Who repairs it**: nobody needs to; nothing suggests a fault except the lamp states.

## Material Evaluation

| Material | Reads? | Matches the family? |
|---|---|---|
| Dark blue-grey steel (shell, spindle) | Yes | Identical to the Cobble Generators (same painters) |
| Andesite (trim, collars) | Yes | Identical |
| Walnut boards (top) | Yes | Identical |
| Glass, lamp | Yes | Shared textures |
| Dripstone (vanilla) | Yes, strongly | New to the family; vanilla's own, no house palette needed |
| Vanilla iron (hammer head, arm) | Yes: neutral light grey, clearly not steel | **New material** for the mod: not in the visual-language doc yet (proposal below) |

## Minecraft Readability Evaluation

- **Primary**: the iron hammer (highest value contrast); strong at block size and gameplay distance.
- **Secondary**: the water band and dripstone ceiling, the four stalactites; they carry the silhouette through the
  window.
- **Tertiary**: lamp, port collars, tray pieces, rivets; none dominates.
- **Texel density**: one texel per pixel on every own face (the kit's `fitUv`); the stalactites use vanilla's cross
  sprites scaled 0.2, which the brief chose so they are real dripstone (they read finer than the box, as the harvester's
  real plants do).
- No gradients, no noise, no zoom-only detail.

## Cohesion Evaluation

- **Shared materials**: all shell materials are the family's own; iron is the one addition, chosen by the user.
- **Manufacturing language**: same shell, same window, same lamp, same port grid rule. One factory.
- **Colour logic** (against `design/visual-language.md`): lamp meanings unchanged (cyan running, amber full, dark red
  stopped, dark when not turning); no product trim colour (the dripstone is its own colour); no brass on the andesite
  tier; no glow besides the lamp. All consistent.
- **Age**: new, like every process box.

## Recommended Texture Changes

None required. The asset is ready as it stands. One note for when it goes into the mod (not a texture change):

1. **Item GUI view** (for the export): give the item the family's front-forward display transform so the window and
   the blue band face the player in the inventory. Why: the window is the block's identity, and the default angle hides
   it.

## Recommended Retexture Candidates

Now that this asset exists, are there earlier assets that should be revisited for visual consistency?

| Asset | Reason for review | What has changed | Proposed update |
|---|---|---|---|
| Cobble Generators' drill (`cobble_generator_drill`, all 14 generators) | The family's other "working tool" | Its sibling's tool is now vanilla iron (user's call) | Paint the drill flutes in the same iron: base `#b1b0b0`/`#b9b9b9`, grooves `#828282`, cutting edge `#c1c1c1`; keep the andesite (or brass) collar. Optional: the drill is Create's own tool, and Create's drill head is also a light silvery grey, so this moves it closer to Create too |

Nothing else needs revisiting: the shell, lamp, glass and port all come straight from the family.

### Proposed visual-language addition (needs your OK)

Add to **Materials**:

| Material | Shades (dark → highlight) | Grain | Used for |
|---|---|---|---|
| Vanilla iron | `#828282` `#a8a8a8` `#b1b0b0` `#b9b9b9` `#c1c1c1`, worn faces `#d1cfcf` `#d6d6d6` | Gentle, three close shades (sampled from vanilla `iron_block` and `iron_ingot`, without the block's whites) | Working tool heads that strike or cut (the Pointed Dripstone Generator's hammer). Lighter than the house's mid-grey cap by the user's choice, kept to small moving parts so it stays the primary element |

## Art Director Verdict

**Approved.** It strengthens the generator family: a second product reads at a glance through the same shell, and the
iron hammer gives it its own hero without breaking the house palette. The one open cohesion question is whether the
Cobble Generators' drill should follow the hammer into vanilla iron; that, and the visual-language addition, are your
call.

## Follow-up

2026-11-10, the user's decisions:
- **The hammer follows the drill, not the other way round** ("naaah lets change the hammer rather than the drill, we
  are in create after all"). The head and arm are now the Cobble Generators' drill steel (`#4a5059` `#555b63`
  `#5f656e`, striking faces `#6b7178`), so the family's two working tools share one metal: Create's. Compared side by
  side first (`pointed-dripstone-generator/hammer-drill-steel-vs-iron.png`): the steel hammer sits closer in value to
  the chamber walls than iron did; the user chose family cohesion.
- **Retexture candidate (drill to iron): declined.** The drill stays as it is.
- **Visual-language addition (vanilla iron): withdrawn**, as iron is no longer used.
- Recommended change 1 (the item's front-forward GUI view) stands for the export.

Verdict unchanged: approved, now with the hammer in drill steel.
