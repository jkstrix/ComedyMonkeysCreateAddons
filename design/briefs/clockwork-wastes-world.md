# Design Brief: Clockwork Wastes, World and City (group 1)

Group 1 of the Clockwork Wastes look ([spec](../specs/clockwork-wastes.md)): the **look bible** every Wastes asset follows, the **biome's atmosphere and surface**, and the **city's architecture** (heart, four districts, outskirts). Group 2 is the blocks and items ([clockwork-wastes-blocks.md](clockwork-wastes-blocks.md)); group 3 is the bots ([clockwork-wastes-bots.md](clockwork-wastes-bots.md)).

## Hard Requirements

From the user:
- "a city on an end island, complex structures, where you will find broken create machines, broken bots, technology is unknown" (2026-10-28).
- "Electricity networks that when given power can illuminate it with little lamps and street lights" (2026-10-28).
- **Lore (2026-10-28):** "A race of automatons whose makers were overthrown by their creations long before the cities were destroyed by war."
- **The war (2026-10-28):** "The war is between these automatons that found electricity and then turned on their masters, and the clockwork resistance: bots that helped their creators flee and fought back, eventually wiping each other out."
- **Glow:** pale starlight (chosen over amber and two-tone).
- **Ruin state:** intact core, broken edge: collapse at the outskirts, the heart nearly whole and frozen mid-shift.
- **Bots:** their own family, "with eyes like ours": lens eyes that echo Trade Bot's pods.
- Separate from the Clockwork Ruins (the Juggernaut's halls).

From the spec:
- About 144 blocks across: **a heart and four districts, Foundry, Workshops, Rail Yard, Quarters**; struts and bridges over the void at the island's edge; outskirts where there's no room (D21, P3, P4, P46).
- Each district: a Substation, a City Vault, streets of Derelict lamps on Derelict cables, broken machines, slumped bot shells, dormant Sentries and Scuttlers (P4).
- The heart: Star Reactor hall, heart vault, the tallest tower; a 31 × 31 arena with four conduit pylons and a dais (P4, P32).
- Fog and sky tinted rust-brown, drifting brass-dust particles, distant ticking and creaks, no music; scorched, rust-stained End Stone, scrap heaps, toppled cogs, tangled cable; no chorus (P5).
- Lamps ripple on down the streets when a district wakes (P8); doors slide into the walls (P49).

Standing art rules (user):
- Dark and muted; light areas mid grey at most; glows muted, never near-white.
- Accents are trims. Ordinary blocks stay restrained. Big industrial structures get real relief hardware and may use warning colours. No decoration that repeats a readout.
- Real-object detail is welcome when every part is a real component (the Cuckoo Clock).
- Create's own copper palette, not a muddier one; mid brass, sparing.
- Round is for motors and engines; presses, boxes and cabinets stay square.
- Every look is previewed in Blockbench before it goes into the mod.

## Asset Type

World: a biome (colours, particles, ambience, surface blocks) and a structure set (the city, outskirts). 16 px = 1 block. The city is built from blocks: our new Wastes blocks (group 2), vanilla blocks, and Create's blocks where they fit. Structure pieces are authored like the Juggernaut ruins (`design/structures/juggernaut-ruin.js`), not in Blockbench; vignettes and the key blocks are previewed in Blockbench.

## Fantasy

You land on an island where the machines won, and then lost. A city built by automatons, for automatons, on the bones of their makers' Create-era town. The ones who found electricity rose up and took it; the clockwork bots who stayed loyal flew their makers out and came back to fight; the war ground on until neither side was left. Feed it power and it remembers: starlight runs along its sky-cables from district to district, its lamps come on street by street, and the uprising's soldiers stand up. The resistance's dead stay dark. They ran on springs, and nobody is left to wind them.

### The story, in order
1. **The makers** built a Create town and clockwork automatons to work it: spring-wound, cog-driven, wound by hand with a key. The town ran on **rotation** (shafts, cogs, springs). There was no electricity.
2. **The uprising:** some automatons found electricity (the city's pale starlight). It freed them from the key, and they turned on their masters. Their emblem is the **broken chain**.
3. **The clockwork resistance:** bots that stayed wound and loyal helped their makers flee **on wings** (the Elytra hook), then came back and fought from outside the walls. Their emblem is the **winding key**.
4. **The long war:** the uprising built its void-alloy city over the makers' town while the resistance besieged it. The overthrow came first; the war that finished the city came long after.
5. **Nobody won.** Both sides wiped each other out. The uprising's soldiers only sleep, because electricity can wake them. The resistance are dead for good.

In these briefs, **"the automatons" means the uprising** unless a line says the resistance.

## Hero Element

**The city lighting up along its sky-cables.** Derelict cable spans run from every district's Substation up to the heart's spire, slung between towers like a web. Dark, they're just a tangle. When a district wakes, a pulse of starlight runs out along its cables, the street lamps follow in a ripple, and that quarter of the skyline joins the spire. Four districts awake, and the whole web glows: a map of the city drawn in light, visible from the next island. Everything else in the world brief serves that moment.

## Theme

### The three layers (the look's rule)

Every structure and block belongs to one of three layers. A player should be able to tell which by looking.

| Layer | What | Materials | Feel |
|---|---|---|---|
| **1. Makers' era** (oldest) | The town the automatons inherited: streets, factory sheds, the rail yard, the machines, cast-iron oil-lamp posts, statues of the makers. It ran on rotation, never electricity | Andesite casing, dark steel, mid brass, Create copper aged to patina, dark walnut, End Stone bricks | Familiar. "I know what that is." Create, abandoned |
| **2. The uprising's era** | What the electric automatons added after they took the town: Substations, vaults, sliding doors, pylons, reactors, sky-cables, charging alcoves, the spire. Everything electrical in the city is theirs | **Void-alloy** (below) with hairline **starlight seams**; no rivets, no bolts, no wood: cast and fitted, never fastened | Strange. "...what is that?" Smooth, faceted, tall, built for bodies that don't sit |
| **3. The war** (latest) | The uprising against the clockwork resistance, over both earlier layers: scorch fans, bolt craters, welded scrap barricades, fallen war-frames from **both sides**, the resistance's siege lines outside the walls, toppled towers, hanging bridges | Soot, rust, melted edges, raw scrap; the resistance's brass and spring steel among the uprising's void-alloy | Violent, then still. Heaviest at the edge, lightest at the heart |

### The war's two sides

A player should tell the sides apart from across a street, without text.

| | **The uprising** | **The clockwork resistance** |
|---|---|---|
| Who | Automatons that found electricity and overthrew their makers | Loyal clockwork bots that flew their makers out and fought back |
| Material | Void-alloy, starlight seams | The makers' own: andesite casing, mid brass, dark steel, walnut, **blued spring steel** |
| Engine | Electricity: a seam that lights | A **mainspring drum** on the back with a **winding key** in it (round, because it's their engine: house rule) |
| Eyes | One lens, or a ring of lenses, in a void-alloy ring; starlight and an ember pupil when woken | **Two round glass eyes in brass rims**, warm dark glass with a dull catchlight: binocular like Trade Bot and Helper Bot. **They never light** |
| Shape | Tall chamfered rectangles | Squarer, stockier, riveted, patched: Create-built, repaired in the field |
| Emblem | The broken chain | The winding key |
| Where | Everywhere inside, thickest at the heart | Outside and at the edges: the siege lines, the barricades, the outskirts. A few deep inside, where they got furthest |
| When you bring power | Wakes: lamps, doors, Sentries, Scuttlers, the Custodian | Stays dark. Always |

The resistance are the city's quiet tragedy and the reason players keep finding brass among the void-alloy. **Every woken hostile is the uprising**; nothing of the resistance moves.

Rules that keep the layers readable:
- **Automatons build over, never with.** Void-alloy is clamped onto old stonework, threaded through old windows, stood on old foundations. It never mixes into a makers' wall as random blocks.
- **The makers are absent.** No bones, no bodies. They appear only as **statues** (tall, robed, faces chiselled flat and replaced with a blank lens plate) and in what's left of their scale: a few human-sized doorways, bricked up.
- **The machines' scale wins.** Automaton doorways are 2 wide × 3 tall; ceilings are high; stairs are rare (they used ramps and lifts).

### Palette

| Material | Use | Colours |
|---|---|---|
| **Void-alloy** (automatons) | Their blocks, bots, spire, cables | near-black with a cool violet cast `#16151c` `#1d1c25` `#25232f`; facet edges catch `#34304a`; seam lines (unlit) `#2e2a40` |
| **Starlight glow** (the city's power) | Lit seams, lamp lenses, bot lenses, reactor cores, cable pulses | dim `#3a3552`, edge `#7f76a8`, core `#b8b0d8`. The core never covers more than a few pixels per face; most lit seams use the edge tone |
| **Hostile pupil** | Woken bots' pupils only | ember `#9c3a2a`, centre `#b5452f`, a single pixel |
| Andesite casing (makers) | Machine bodies, sheds | `#5e615d` `#6b6e6a` `#767873` (a step darker than house: aged) |
| Dark steel (makers) | Frames, rails, lamp posts | `#2f343b` `#3c424a` `#4a5059` |
| Cast iron (makers' lamps) | Derelict lamp posts | `#26292d` `#2f3338` `#3a3f45` (as our Cast Iron Lamp Post) |
| Mid brass (makers) | Trims, plaques, gauges | `#7e6832` `#8f7638` `#a3873f`, no highlights |
| Copper, aged (makers) | Pipes, tanks | Create's copper `#904931` `#9a5038` `#a75a40` on whole parts; **patina** `#4f6b5e` `#5b7a6c` creeping from edges and joints |
| Dark walnut (makers) | Benches, crates, rail sleepers | `#4e3626` `#634532` `#6f4d36` |
| Blued spring steel (resistance) | Mainsprings, screws, spilled coils | `#2c3446` `#36405a` `#45506a`: a real clockwork finish; small parts only, so it never competes with void-alloy's violet |
| Glass eye (resistance) | Their eyes | warm dark glass `#3a3430`, rim mid brass, one dull catchlight `#6b6e6a`. Never lit |
| Rust (age and war) | Streaks under bolts and on steel | `#5a3523` `#6e4129` `#7f4d30`, as runs and edges, never whole faces |
| Soot (war) | Scorch fans, crater rims | `#1a1714` `#26211c` `#30291f` |
| Scorched End Stone (surface) | The biome's ground | `#6f6a50` `#7c7659` `#878063`, with rust stain `#6e5a3e` |
| End Stone bricks, stained (makers' masonry) | Walls, foundations | vanilla End Stone Bricks tinted down: `#8f8a68` `#9c9672` |

### Motifs (each one tells part of the story)

- **The broken chain:** the automatons' emblem of the uprising. A chain with one link sprung open. On vault doors, door seams, pylons and the spire's crown. Never more than one per block face.
- **The lens-in-cog sigil:** their mark of a citizen. A cog with a lens where its axle should be. On Substations and over district gates. It's also how their bots' faces are built: a lens set into a ring.
- **Defaced makers:** statues of tall robed figures with their faces chiselled flat and a blank void-alloy plate bolted over the face. Some are toppled; one in each district stands, facing away from the heart.
- **Charging alcoves:** upright cradles shaped round a standing body. These are the Quarters' "homes", and they appear in every district's walls.
- **Sky-cables:** derelict cable spans slung between towers. Everything electrical leads up to them.
- **The winding key:** the resistance's mark, the opposite of the broken chain (the uprising broke free of the key; the resistance kept it). A butterfly-bowed clock key, in brass. Scratched into barricades from the outside, stamped on resistance shoulder plates, and hung on a nail in a few places inside the city where they got furthest.

### What it sits beside

The Juggernaut ruins (deepslate, vines, chorus in the End), the Electric Lights (the city's lamps are their ancestors), the Industrial Battery (substation hardware), Trade Bot and Helper Bot (their eyes echo the city's bots). The Wastes must never look like the Juggernaut's Norse halls: **no runes, no frost, no knotwork, no gold**. Those belong to an older, different maker.

## Shape Language

**Tall, narrow rectangles with chamfered tops** dominate the automatons' layer: slots, doorways, towers, alcoves and pylons are all upright with clipped corners. They read as built for standing bodies. **Circles** appear as their accent: lenses, the vault's iris, reactor cores. **The makers' layer is squarer and lower**: Create-style sheds, boxes and pitched roofs. The war breaks both into **jagged diagonals**: snapped towers, leaning posts, splayed scrap.

## Silhouette Description

- **The city from the next island:** a ragged ring of broken towers round a taller, intact centre spire, with a web of cables slung from the ring to the spire. "A spider web strung between broken teeth, with a needle in the middle."
- **The spire:** a tall void-alloy needle (about 60 blocks above the island) rising out of a stout makers'-era drum of End Stone brick, with a **crown of four outstretched arms** at the top, each holding a cable down to a district. Lit, the crown's seams glow first.
- **A district at street level:** low makers' sheds and roofs, with void-alloy additions rising out of them: tall slotted towers, alcove walls, gantries. A Substation yard at the district's edge nearest the heart, its cables climbing a lattice mast to the sky-web.
- **The outskirts:** stubs, toppled lamp posts, scrap heaps and fallen war-frames lying in scorch fans, thinning to the island's edge, where bridges snap off into the void.

## Primary Forms

### The heart (about 41 × 41 blocks)
1. **The spire.** A makers'-era drum (21 blocks across, 14 tall: End Stone bricks, copper-patina dome ribs left bare) with the void-alloy spire rising out of its broken dome (5 × 5 at the base, tapering to 3 × 3, chamfered, a seam of starlight up each corner). It ends in the **crown**: four arms reaching out 8 blocks, each ending in an insulator from which a sky-cable drops to its district.
2. **The reactor hall,** inside the drum: the 31 × 31 arena (P32). The Derelict Star Reactor stands under the spire's shaft on a raised ring, facing the Custodian's dais. The four conduit pylons stand in the corners. The floor is void-alloy plate with **conduit grooves** running from each pylon to the dais; lit, starlight runs down the grooves.
3. **The heart vault,** behind the dais, in the drum's wall: the largest City Vault frame (group 2), flanked by two standing maker statues whose blank face plates were turned to face the wall. It holds **the last pair of wings**: the resistance flew the makers out on wings, and the uprising sealed the last pair in the heart so nobody else could leave. That's the Elytra (D18) in the story.

### The districts (each about 45 × 45 blocks, one per quadrant)
Each has its own silhouette and one **set piece**, so players can tell where they are:

| District | Makers' layer | Automatons' layer | War | Set piece |
|---|---|---|---|---|
| **Foundry** | Brick furnace sheds, a cold blast furnace, chimneys, crucibles, Create presses and mixers (broken) | Casting floors where they poured their own bodies: void-alloy moulds shaped like limbs and heads, stacked | One chimney toppled across the street; a scorch fan up a furnace wall | **The casting floor:** a frozen pour, a half-cast body still in its mould, a crane above it with a ladle tipped mid-pour (dark glass-like spill) |
| **Workshops** | Long sheds with walnut benches, Create Mechanical Arms, Deployers and belts (broken) | Assembly gantries: automatons building automatons; half-built frames clamped upright | A shed roof collapsed onto the line; bolt craters in the floor | **The assembly line:** a row of 5 half-built bodies in clamps, each more complete than the last; the last one has its lens in |
| **Rail Yard** | Track, a turntable, a signal box, platform canopies, a halted Create-style train (carriage casings, bogeys) | Charging cradles on the platform where automaton passengers waited; void-alloy signal masts | A derailed carriage on its side; track torn up in a crater | **The last train:** a train stopped at the platform, doors open, a **resistance shell** on the carriage step, one hand reaching back down to the platform for someone who isn't there. Uprising shells lie on the platform behind it |
| **Quarters** | Makers' terraces, their small doorways bricked up | **Alcove walls:** honeycomb walls of charging alcoves, stacked 3–5 high, reached by ramps and lifts; small trinkets left in some (a cog, a key, a maker's toy) | One alcove tower split down the middle; barricades across the main ramp | **The vigil:** a plaza of alcoves facing a toppled maker statue; every alcove's shell is turned to look at it |

### The outskirts and the island edge
- **Rings of damage** like the Juggernaut ruins, which the user loved ("grow bigger towards the middle"), reversed: from the districts outward, collapse grows. First come snapped towers and fallen lamp posts, then scrap heaps, welded barricades and fallen war-frames, then scattered stubs, and finally bare scorched ground at the island's edge.
- **The siege lines** (the resistance's ring, in the outermost band): what's left of their field works, built from the makers' materials. Low walls of stacked andesite casing and walnut, a toppled **siege winch** (a big brass mainspring drum on a wheeled frame, its spring sprung out across the ground in a blued coil), resistance shells behind cover, all facing in toward the city. The winding key is scratched into the cover stones.
- **Struts and bridges** (P3): where a district reaches past the island, it stands on void-alloy trestles (tall, thin, cross-braced, with chamfered feet on the island's underside). Bridges between them are railed void-alloy decks; some have snapped, with half a bridge hanging down into the void on its cables.

## Secondary Forms

- **Streets:** makers' paving (polished andesite and stone brick, worn) down the middle, gutters of scorched End Stone. Lamp posts every 6–8 blocks, alternating sides. Derelict cable runs along the walls at head height on brackets, rising up lattice masts at the corners.
- **Substation yards** (one per district, the industrial set piece): the Substation block (group 2) in a fenced yard of real hardware built from blocks: a transformer bank (iron bars and void-alloy fins), a lattice mast carrying the sky-cable, cable trays, a makers'-era control hut. This is the one place in a district where **muted safety yellow and one hazard band** appear (the Industrial Battery's lesson: industrial structures earn warning colours). Lit, the transformer fins glow along their edges.
- **Barricades:** the uprising's welded scrap walls (bent iron bars, plates, cogs, a door torn off), set across streets at district edges, scorched on the outside face, with the winding key scratched into them from outside. The war came from outside: the resistance attacked inward.
- **Fallen war-frames** (8–12 blocks long, built from blocks; the biggest "what happened here?" moment on the way in), from **both sides**, often **locked together** where they fell:
  - **Uprising war-frame:** tall and thin, void-alloy, a big dark lens (a cracked dark glass disc, the one place it catches light).
  - **Resistance war-frame:** stocky, square, andesite casing and brass plates, riveted and patched, a split mainspring drum on its back with the spring burst out in a blued coil, a winding key the size of a door, and two big glass eyes in brass rims.
  - The best one per city: the two locked in a grapple in the outskirts, the resistance frame's hand on the uprising frame's lens.
- **Scrap heaps:** low mounds of Clockwork Scrap blocks, iron bars, chain, and a toppled cog or two, with salvage blocks half-buried in them.

## Interesting Features

- **The skyline lights in order.** A district's sky-cable pulses (a bright starlight bead runs up to the crown in about 2 s), then its street lamps ripple outward from the Substation (the Electric Lights' ripple). When the fourth district wakes, the crown's seams light all at once and the spire's corner seams climb from the bottom to the top over 4 s. *Renderer and block-state driven; previewed in Blockbench as a short timeline.*
- **When the city wakes, the resistance stays dark.** Every lamp around a resistance shell comes on, and its two glass eyes just catch the light. That contrast is the story's last line, told with no text.
- **Alcove trinkets:** one alcove in five holds a tiny object: a cog, a winding key, a toy cart, a pressed flower (the only colour in the city, faded). Lore without words: they kept things. The winding key in an uprising alcove is deliberate, and players can make of it what they like.
- **The statues face away from the heart.** All standing maker statues have been turned to face outwards, except the two at the heart vault, which face the wall. Players who notice will wonder.
- **The frozen shift:** at the heart, things are mid-motion: a Mechanical Arm holding a part over a depot, a lift halfway up its shaft, a bot shell mid-step. Further out, everything has fallen.
- **Brass makers' plaques** on old buildings, worn smooth: the only writing, and it's unreadable (a few raised bars, no letters).

## Biome Atmosphere

| Element | Setting |
|---|---|
| Fog colour | rust-brown `#4a3a2e` (thick enough to soften the far islands, never to hide the spire) |
| Sky colour | `#3b2e26` (the End's sky texture still shows; this tints it warm) |
| Water / water fog | `#3f4a52` / `#2a3036` (rarely seen; kept dull) |
| Grass / foliage | none; a few dead vines in Quarters alcoves use a dull olive `#4f5236` |
| Particles | **brass dust**: slow drifting 1 px motes `#8f7638` at low alpha, about a third as dense as Basalt Deltas' ash. While a district is lit, a rare **starlight spark** drops from its cables (1 in 40 seconds per span) |
| Ambience loop | distant ticking that never keeps time, a long metal creak, wind through cables (sound brief later) |
| Mood additions | a soft, far-off clank every minute or so; no music |
| Surface | **Scorched End Stone** (new block: vanilla End Stone's shape, palette above) on top, End Stone below; rust stains as a texture variant; **scrap litter** (a few pixels of bent wire and cog teeth on the block top, a texture variant, not a separate block) |

## Fun Score (1-10)

**9.** The three layers give every street a story a player can read without text, and the skyline lighting up along its cables, a district at a time, is a moment players will screenshot and come back to show friends.

## Builder Notes

- **Order of work:** the look bible palette and motifs first, as a material sheet in Blockbench (swatches, void-alloy facets lit and dark, a seam, the sigils). Then the blocks (group 2), then vignettes: one street corner, the Substation yard, the spire's crown, each as a Blockbench scene from the real block models. Then the structure pieces in code.
- **Void-alloy must not read as black plastic.** It needs facets: every face gets a 1 px lighter top-left edge (`#34304a`) and a darker bottom-right, with a faint violet gradient across big faces. No mottle (crisp, as the process-box panels landed). Lit seams are 1 px wide, edge tone, with the core tone only at intersections.
- **Starlight is pale.** Keep the core `#b8b0d8` to single pixels and short runs, or it reads near-white and breaks the house rule. Check it next to the Electric Lights' amber in one render.
- **The layers must stay separate.** Check every vignette for void-alloy mixed randomly into makers' walls; it should only ever be clamped on, threaded through or built on top.
- **Warning colours only in Substation yards.** Nowhere else in the city.
- **The resistance never glows.** No starlight, no seams, no lit eyes, on anything of theirs. And they must read as **bots**, not as makers' buildings: the mainspring drum, key and two eyes are the tell, so never hide all three.
- **The Juggernaut's motifs are banned here:** runes, frost, knotwork, gold.
- **Screenshot for review:** the material sheet; one street corner dark and lit; the skyline from 60 blocks dark, then with one, two and four districts lit; the Rail Yard's last train; a Quarters alcove wall with a trinket; the two war-frames locked together, dark and with the district lit around them.
