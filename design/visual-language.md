# Visual Language

Status: **approved** (user, 2026-10-31; drafted in the Clockwork Dynamo texture review from the approved assets and the user's recorded calls). It changes only with the user's approval. Reviews check against it.

## Makers

- **Create-era engineers** (the player's own machines): dark blue-grey steel bodies, andesite trim, crisp bevelled panels, round masses only for motors and engines, brass as thin bezels and rings. Examples: Geothermal Turbine, Lava Generator, Electric Motor, Magma Engines, the process boxes.
- **The bots** (Trade Bot, Helper Bot, and the machines they build: the Clockwork Dynamo, the Clockwork Tubes): rounded squares with stepped corners, andesite panels in a dark-steel frame, a riveted brass waist band, whisker grilles, copper aerials with brass beads, cyan or amber eyes. Friendly, never a face on a block.
  - **Bot-built blocks borrow the Trade Bot's actual parts and painters** (from `trade-bot.gen.js`): his andesite mottle and steel, his waist band (brass with dark brass `#6e5a2a` every other pixel along its length), dark-brass bolts, brass rims like his eye rims, his station's brass cog, his steel lid plates (lighter `#5a616b` leading edge). This holds on **any tier**: for bot-built blocks it outranks "each tier keeps its own trim metal" (user, 2026-10-31).
- **The automatons of the Clockwork Wastes** (End): void-alloy plates set offset, aged End brick, relief on every wall, pale starlight glow. Detail lives in the Wastes briefs.

## Materials

| Material | Shades (dark → highlight) | Grain | Used for |
|---|---|---|---|
| Dark blue-grey steel | `#2f343b` `#3c424a` `#4a5059` | Big panels near flat: `#3c424a` with a rare `#40464e` (the process boxes); the bots' three-shade mottle on frames and edges | Bodies, frames, bot frames |
| Andesite | `#6b6e6a` `#73766f` `#7c7f79` `#8a8c86` (ceiling) | Gentle, two or three close shades | Panels, trim; the bots' panels are mostly `#7c7f79` |
| Brass on blocks | `#7e6832` `#8f7638` `#a3873f` (no highlights) | Gentle | Trims, rings, bands. House brass `#b08f45` reads too bright on blocks |
| Bot brass | `#6e5a2a` `#8f7638` `#b08f45` `#c9a75a` (highlights only) | Gentle | Bot bodies, and every brass part on bot-built blocks: bands, rims, cogs, lips, beads (the Dynamo, the Clockwork Tubes). Big areas (a ring gear, a lip) use `#b08f45` over `#8f7638` only; `#c9a75a` stays on bands and small trims |
| Glass (cutout) | edge `#6f7f87`, glint `#a9bac2` | Clear, a lit edge line, a two-texel diagonal glint every 12 on outward faces only | Item tubes and windows, where the renderer draws items behind it. Same hue as the Fluid Lines' translucent glass, which stays as it is |
| Copper (house) | `#7a4a2e` `#8a5434` `#9c5f3a` `#b87a4e` | Gentle | Windings, contacts, commutators, aerials, bot forks |
| Copper (Create's) | `#904931` … `#c87456`, rivets `#d67b5b` | Clean plates, no mottle | Copper-bodied machines (Wellspring, Incinerator): match Create there |
| Walnut | `#4e3626` `#634532` `#6f4d36` | Planks | Containers, aviation accents, the Cuckoo Clock (the darker house walnut reads black in game) |
| Carbon / rubber | `#26272a` `#323337` | Slight | Tyres, brushes, insulators |

## Colour meanings

| Colour | Means | Where it appears | Never used for |
|---|---|---|---|
| Copper | Electricity flows here | Windings, commutators, contacts, power lines, aerials | Decoration on non-electric parts of electric blocks |
| Cyan glow | Making or has power, and running | Every electric block's running lamp; Helper Bot's eyes | Warnings |
| Amber glow | Full / warm process; Trade Bot's eyes | Machine "output full" lamps, firebox and lava glow | Power state |
| Dark red glow | Short of power / stopped by a fault | Machine lamps | Decoration |
| Dark teal `#1e3236` | A bot device at rest (an unlit cyan) | Helper Bot's switched-off eyes, the Dynamo's idle lamp | Faults |
| Muted emerald `#4f9a68` | Open for trade | Trade Bot's lamp | Power |
| Pale starlight | The Clockwork Wastes' own light | Wastes structures | Overworld machines |

## Glow

Small areas only, muted, never near-white. One light per meaning: the electric family's cyan lamp is `#4f8f99` edge, `#6fb7c4` centre. Lamps glow full-bright via `neoforge_data`; the unlit state is a dark lens, not a hole.

## Age and wear

Crafted machines look new: no grime, no scratches. Wear needs a reason (soot above an exhaust, heat on a firebox). Ruins of the Clockwork Wastes are aged on purpose (intact core, broken edge).

## Markings

No hazard stripes (one small accent at most, usually none). No saturated surfaces. Brass, copper and glow are trims. Rivets read as dots a shade apart, never as stripes.

## Families

- **Turbine family** (Create-era electric): Geothermal Turbine, Lava Generator, Electric Motor. Steel staves, andesite trim, brass bezels, copper windings, cyan / dark-red lamps.
- **Process boxes**: Auto Harvester, Animal Pens, Cobble Generators, String Tangler, Stone Infuser. Sealed steel boxes, crisp bevels, a front tray, state lamps (cyan running, amber full, dark red off, unlit no rotation).
- **Engines**: Magma Engines. Round cradled drums, V-twin cylinders.
- **Bots and bot-built machines**: Trade Bot, Helper Bot, Clockwork Dynamo.
- **Copper machines**: Wellspring, Incinerator (Create's copper).
- **Containers**: Upgraded Chests and Barrels (walnut in a riveted metal frame).
- **Aeronautics set**: aviator steel, mid brass, walnut accents.
- **Clockwork Wastes**: void alloy, aged End brick, starlight.
