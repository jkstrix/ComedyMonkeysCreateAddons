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

## The End's peoples: the Clockwork Wastes and the Juggernaut

The detailed rules for the Wastes and for the Juggernaut's Norse makers. They come from the Wastes texture reviews (2026-10-28 and 2026-10-29), the Wastes block set (D75 to D78) and the Juggernaut texture review (2026-10-29). The rules above still apply wherever this section doesn't say otherwise. The story behind the makers is in `design/lore/the-three-peoples.md`.

### Makers

| Maker | Where | How you tell their work |
|---|---|---|
| **The makers** (the Creators) | The Wastes' oldest layer, as the Create-era engineers above | Bolted, riveted, bevelled panels; house steel, andesite, brass, copper, walnut, End Stone brick. Runs on rotation: shafts, cogs, springs |
| **The uprising** (the Automatons) | Clockwork Wastes | Cast void-alloy, chamfered, tall; clamped onto others' work, never bolted, never mixed into it. Light lives in hairline seams. Emblem: the broken chain |
| **The clockwork resistance** | Clockwork Wastes (story; a real role later) | The makers' own materials, stocky and field-repaired; a mainspring drum with a winding key; two glass eyes in brass rims. Never lit. Emblem: the winding key, in brass |
| **The Norse makers** (the forge-holds) | The Juggernaut and its ruins | Stone, bronze, runes, frost, knotwork, gold. None of it appears in the Wastes |

### Materials

| Material | Shades (dark → highlight) | Grain | Used for |
|---|---|---|---|
| Cast iron | `#26292d` `#2f3338` `#3a3f45` | Mottle; shafts fluted (`#2b2f33` every other column) | Lamp posts, plinths, brackets. One painter, shared with the Electric Lights |
| Aged andesite | `#5e615d` `#666964` `#6b6e6a` | Gentle mottle | The makers' derelict work, a step darker than new andesite |
| Mid brass | `#7e6832` `#8f7638` `#a3873f` | Mottle; the highlight rare (about 3%) | **New** work: the player's crafted blocks (the Star Reactor's casing) |
| Aged brass | `#6e5a36` `#7e6832` `#8f7638`, a patina pixel `#4f6b5e` in the odd recess | Mottle, no highlight | **Old** work: the makers' derelict kit, the resistance, statues' trims |
| Copper (Wastes) | Create's `#904931` `#9a5038` `#a75a40`; fresh span `#b26247` | Mottle | Pipes, cable, terminals |
| Patina | `#4f6b5e` `#5b7a6c` | Two-tone mottle | Aged copper: creeps from joints and lower edges; copper shows only where something rubs. The block set's patina copper is muted, never vanilla's mint |
| Spring steel | edge `#4a525a`, strip `#6d7780`, highlight `#8a949c` | Smooth strip, a few highlights on its flat top. The dark between turns is the real gap, never painted onto a surface | Mainsprings (Spring Engine, Mainspring item, the resistance), always shown as a coil of strip, never as a block |
| Void-alloy | `#16151c` `#1d1c25` `#232130` `#2a2838`, lit edge `#34304a` | Crisp plates with 2 to 4 shades of grain, a 1 px `#34304a` lit edge top-left and `#16151c` bottom-right; never a diagonal sheen (it reads as stripes). Full cubes: an ashlar bond of 1×1 and 2×1 plates in offset courses, picked by position. Stairs, slabs and walls: a plain plate. Machines and props: the frameless field, so no lines cross one object. Small parts and bots: the same grain (D75 to D78) | The uprising's blocks, bots, spire, insulators |
| End Stone brick (Wastes) | mortar `#6c6750`, bricks `#827d5f` `#8d876a` | Brick courses, three weighted variants, dust on up faces only; aged to about 97 brightness (vanilla's is 215) | The makers' masonry in the End |
| Scorched End Stone | `#6f6a50` `#7c7659` `#878063`, stain `#6e5a3e` | Speckle, as End Stone | The Wastes' ground |
| Forge iron (Norse makers) | charcoal `#1c1e21` `#232629` `#2a2d31`; iron `#343a42` `#3d444d` `#454d56`, edge `#56606b` | Mottle; shape read from 1 px edge bevels | The Juggernaut's frame and armour |
| Tarnished bronze (Norse makers) | body `#6e5a36` `#7d6a44`, lit edge `#8c774a`, dark edge `#463822` | Smooth body, bright top and outer edges. Patina `#4f6b5e` `#5b7a6c` only in recesses where water sits, in 2–3 px clusters | The Juggernaut's key, rims and cogs; the Winding Key; the Spring Engine |

### Colour meanings

| Colour | Means | Where it appears | Never used for |
|---|---|---|---|
| Pale violet starlight (dim `#3a3552`, edge `#7f76a8`, core `#b8b0d8`) | The uprising's electricity | Seams, lenses, columns, cable lines in the Wastes | Makers' or resistance work; anything the player builds from scratch (the Star Reactor's core is the uprising's) |
| Lamp amber (`#d99a4a`) | Makers' and players' light | The Electric Lights; the brightness check for starlight | The uprising |
| Frost blue (`#4f8fae` `#4a87a4`, bright `#7ab8d0`, dim `#23404c`) | The Norse makers' runes | The Juggernaut's runes, eye slit and cracks; the Winding Key and Spring Engine rune bands. Carved shapes only | Round lenses; anything in the Wastes |
| Ember (`#9c3a2a` / `#b5452f`) | Hostile intent | A woken bot's pupil, one or two pixels | Surfaces, decoration |
| Hazard yellow (`#a1832e` `#b39234` on `#25282c`) | Industrial danger | The Industrial Battery; the Wastes' Substation yards only (the exceptions to "no hazard stripes") | Ordinary blocks; more than one band per structure |
| Patina green | Age (copper left alone) | Aged copper | A signal |
| Battlefield blue (matte: leaf `#141c2e`-`#3b5886`, Crookwood leaf to `#7a8dab`) | Growth from the Twisted Battlefields (D92) | The overgrowth: vines, moss, lichen, crack plants, roots, trees | Made things |
| Shiftcap glow (blue `#2b4180` `#4f6bb5` `#8095d8`, plum `#3f2848` `#8a4fa0` `#b483c8`) | Living light: the only glow that's alive; always on (D92) | The Shiftcaps' gills, lips and spots | A seam; the violet between blue and plum (the uprising's starlight) |

### Glow

- Glows are separate `shade:false` parts, so the renderer can light, dim, flicker and pulse them; never bake a lit and an unlit texture.
- **Starlight:** mostly the edge tone. The core tone only at single pixels: seam crossings, lens centres, a column's top. Secondary lights (frame seams, lens rings) use the dim tone, with edge-tone ends at most, so one thing per block is brightest.
- **Amber lamps:** core over most of the lit face, edge tone at its border (the Electric Lights' glow).

### Lenses (the Wastes bots)

A lens is drawn as an eye, with the corners clear so it reads round:
- **Dark:** a rim, dark glass `#2a2833`, one dull catchlight `#4c475f`.
- **Lit:** a dim rim `#3a3552`, the edge tone `#7f76a8` only on the ring inside it, and one core pixel `#b8b0d8` as the catchlight at the top-left (Trade Bot's).
- **Hostile:** one ember pixel `#b5452f` as the pupil.
- Never a lit tile; never a red-lit face.

### Age and wear

- **The makers' metal** rusts in runs from a fastener or a corner, never whole faces. Copper patinates from joints and lower edges, and shows bare where it's rubbed.
- **Void-alloy never rusts.** It **chips** (a 2 px notch, `#16151c` inside, a `#34304a` lip) and **cracks** (a `#121118` line with seam colour beside it). Only where fire hit it (outward-facing blocks), one mark per face, none at the Wastes' heart.
- **Masonry cracks follow the mortar joints**, never scribbles; cracked stone sits only under clamp jaws.
- **Dust** settles on up faces only, drifting to one edge and its corners. Never a uniform ring, never on vertical faces.
- **Soot** marks a cause: a scorch fan above arcing contacts, a blast on a barricade's outer face. On void-alloy, war damage by fire shows as **heat-tint** (`#3a2e24`, streaks `#4a3a2e`), because soot is the same value as the metal.
- **Buildings are judged as architecture tiled across big walls**, never one block at a time: relief on every wall (plinths, piers, sills, corbels, bands), no large area of one block.

### Markings

- **Sigils:** at most one per block face. The broken chain and the lens-in-cog belong to the uprising; the winding key to the resistance. They're drawn in seam tones, and lit only on uprising work.
- No letters; makers' plaques are worn smooth.
- **Runes and knotwork** (Norse makers only) are carved, never printed: a dark groove round each glyph, and knotwork drawn as bands 2 px or wider. No 1 px lattices; they read as perforation.
