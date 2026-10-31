# Visual Language

The mod's shared look rules, so every texture review checks against the same thing. Drafted from the approved assets in the Clockwork Wastes batch 1 review (2026-10-28, `texture-reviews/clockwork-wastes-city.md`). Change it only with the user's approval. The house palette and what the user has approved and rejected live in the voxel-art-director's `create-house-style.md`; this doc builds on it.

## Makers

| Maker | Where | How you tell their work |
|---|---|---|
| **The makers** (Create; the player's world) | Every machine in the mod; the Wastes' oldest layer | Bolted, riveted, bevelled panels (the process-box look); house steel, andesite, brass, copper, walnut. Runs on rotation: shafts, cogs, springs |
| **The uprising** | Clockwork Wastes | Cast void-alloy, chamfered, tall; clamped onto others' work, never bolted, never mixed into it. Light lives in hairline seams. Emblem: the broken chain |
| **The clockwork resistance** | Clockwork Wastes (story; a real role later) | The makers' own materials, stocky and field-repaired; a mainspring drum with a winding key; two glass eyes in brass rims. Never lit. Emblem: the winding key, in brass |
| **The Norse makers** | The Juggernaut and its ruins | Stone, bronze, runes, frost, knotwork, gold. None of it appears in the Wastes |

## Materials

| Material | Shades (dark → highlight) | Grain | Used for |
|---|---|---|---|
| Dark steel | `#2f343b` `#3c424a` `#4a5059` | Near-flat, rare `#40464e` fleck; crisp bevels (`#4a5059` top-left, `#2a2f35` bottom-right) | Machine bodies, cabinets, frames |
| Andesite casing | `#5e615d` `#6b6e6a` `#767873` (aged: `#5e615d` `#666964` `#6b6e6a`) | Gentle mottle | Panels, light areas, mid grey at most |
| Cast iron | `#26292d` `#2f3338` `#3a3f45` | Mottle; shafts fluted (`#2b2f33` every other column) | Lamp posts, plinths, brackets. One painter, shared with the Electric Lights |
| Mid brass | `#7e6832` `#8f7638` `#a3873f` (fresh: Trade Bot's `#8f7638` `#b08f45` `#c9a75a`) | Mottle; the highlight rare (about 3%) | **New** work: the player's crafted blocks (the Star Reactor's casing), working machines |
| Aged brass | `#6e5a36` `#7e6832` `#8f7638`, a patina pixel `#4f6b5e` in the odd recess | Mottle, no highlight | **Old** work: the makers' derelict kit, the resistance, statues' trims (texture review 2, 2026-10-29) |
| Copper | Create's `#904931` `#9a5038` `#a75a40`; fresh span `#b26247` | Mottle | Pipes, cable, terminals |
| Patina | `#4f6b5e` `#5b7a6c` | Two-tone mottle | Aged copper: creeps from joints and lower edges; copper shows only where something rubs |
| Walnut | `#4e3626` `#634532` `#6f4d36` | Plank lines | Benches, grips, crates |
| Spring steel | edge `#4a525a`, strip `#6d7780`, highlight `#8a949c` | Smooth strip, a few highlights on its flat top. The dark between turns is the real gap, never painted onto a surface | Mainsprings (Spring Engine, Mainspring item, the resistance), always shown as a coil of strip, never as a block |
| Void-alloy | `#16151c` `#1d1c25` `#232130` `#2a2838`, facet edge `#34304a` | **No mottle.** Parts: a full facet frame. Block faces that join up: no frame, a diagonal sheen that tiles | The uprising's blocks, bots, spire, insulators |
| Forge iron (Norse makers) | charcoal `#1c1e21` `#232629` `#2a2d31`; iron `#343a42` `#3d444d` `#454d56`, edge `#56606b` | Mottle; shape read from 1 px edge bevels | The Juggernaut's frame and armour |
| Tarnished bronze (Norse makers) | body `#6e5a36` `#7d6a44`, lit edge `#8c774a`, dark edge `#463822` | Smooth body, bright top and outer edges. Patina `#4f6b5e` `#5b7a6c` only in recesses where water sits, in 2–3 px clusters | The Juggernaut's key, rims and cogs; the Winding Key; the Spring Engine (Juggernaut texture review, 2026-10-29) |
| End Stone brick (Wastes) | mortar `#6c6750`, bricks `#827d5f` `#8d876a` | Brick courses | The makers' masonry in the End, one step above the ground |
| Scorched End Stone | `#6f6a50` `#7c7659` `#878063`, stain `#6e5a3e` | Speckle, as End Stone | The Wastes' ground |

## Colour meanings

| Colour | Means | Where it appears | Never used for |
|---|---|---|---|
| Pale violet starlight (dim `#3a3552`, edge `#7f76a8`, core `#b8b0d8`) | The uprising's electricity | Seams, lenses, columns, cable lines in the Wastes | Makers' or resistance work; anything the player builds from scratch (the Star Reactor's core is the uprising's) |
| Amber (`#d99a4a`) | Makers' and players' light; warmth, safe | Electric Lights, lamps, Trade Bot's eyes | The uprising |
| Cool cyan (`#6fb7c4`) | A friendly bot's eyes (the variant) | Helper Bot's irises: round lenses in brass rims only | Runes, slits; anything hostile |
| Frost blue (`#4f8fae` `#4a87a4`, bright `#7ab8d0`, dim `#23404c`) | The Norse makers' runes | The Juggernaut's runes, eye slit and cracks; the Winding Key and Spring Engine rune bands. Carved shapes only | Round lenses; anything in the Wastes |
| Ember (`#9c3a2a` / `#b5452f`) | Hostile intent | A woken bot's pupil, one or two pixels | Surfaces, decoration |
| Hazard yellow (`#a1832e` `#b39234` on `#25282c`) | Industrial danger | The Industrial Battery; the Wastes' Substation yards only | Ordinary blocks; more than one band per structure |
| Patina green | Age (copper left alone) | Aged copper | A signal |

## Glow

- Glows are separate `shade:false` parts, so the renderer can light, dim, flicker and pulse them; never bake a lit and an unlit texture.
- **Starlight:** mostly the edge tone. The core tone only at single pixels: seam crossings, lens centres, a column's top. Secondary lights (frame seams, lens rings) use the dim tone, with edge-tone ends at most, so one thing per block is brightest.
- **Amber:** core over most of the lit face, edge tone at its border (the Electric Lights' glow).
- Never near-white.

## Lenses (the bots' family face)

A lens is drawn as an eye, with the corners clear so it reads round:
- **Dark:** a rim, dark glass `#2a2833`, one dull catchlight `#4c475f`.
- **Lit:** a dim rim `#3a3552`, the edge tone `#7f76a8` only on the ring inside it, and one core pixel `#b8b0d8` as the catchlight at the top-left (Trade Bot's).
- **Hostile:** one ember pixel `#b5452f` as the pupil.
- Never a lit tile; never a red-lit face (texture review 2, 2026-10-29).

## Age and wear

- **The makers' metal** rusts in runs from a fastener or a corner, never whole faces. Copper patinates from joints and lower edges, and shows bare where it's rubbed.
- **Void-alloy never rusts.** It **chips** (a 2 px notch, `#16151c` inside, a `#34304a` lip) and **cracks** (a `#121118` line with seam colour beside it). Only where fire hit it (outward-facing blocks), one mark per face, none at the Wastes' heart.
- **Dust** settles on up faces only, drifting to one edge and its corners. Never a uniform ring, never on vertical faces.
- **Soot** marks a cause: a scorch fan above arcing contacts, a blast on a barricade's outer face. On void-alloy, war damage by fire shows as **heat-tint** (`#3a2e24`, streaks `#4a3a2e`), because soot is the same value as the metal.
- **Brass ages:** mid brass for new work, aged brass for old (see Materials).
- A material's own gentle mottle is grain, not wear. Every wear mark needs a reason.

## Markings

- **Sigils:** at most one per block face. The broken chain and the lens-in-cog belong to the uprising; the winding key to the resistance. They're drawn in seam tones, and lit only on uprising work.
- **Hazard bands:** one at most, on industrial structures only (Battery, Substation yards).
- No letters; makers' plaques are worn smooth.
- **Runes and knotwork** (Norse makers only) are carved, never printed: a dark groove round each glyph, and knotwork drawn as bands 2 px or wider. No 1 px lattices; they read as perforation.

## Families

| Family | Members | Shared traits |
|---|---|---|
| Process boxes | Auto Harvester, cages, pens, generators, turbine, motor, engines, crucible | House steel with crisp bevels; andesite or brass trim; ports at face centres; round only for motors and engines |
| Electric Lights | 23 lights, poles, Line Posts, spans | Cast iron or galvanised steel; amber glow; undyed amber `#d99a4a` |
| Clockwork Wastes | Material sheet; Substation, City Vault, Sliding Door, Derelict lamps and cable; to come: pylons, reactors, salvage, shells, bots | The three layers (makers, uprising, war); void-alloy and starlight; makers' materials aged a step darker |
| Bots | Trade Bot, Helper Bot; the resistance's kin | House steel, brass, andesite; binocular lens eyes; amber or cool cyan lens glow |
| The Juggernaut | The Clockwork Juggernaut, its ruins, the Winding Key, the Spring Engine | Bronze, stone, runes, frost: kept apart from the Wastes |
