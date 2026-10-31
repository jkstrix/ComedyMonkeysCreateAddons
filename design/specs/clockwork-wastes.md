# Clockwork Wastes: Systems Spec

Status: **ready for development (v1)**, looks to be designed  ·  Related: [Clockwork Juggernaut](clockwork-juggernaut.md) (the Clockwork Ruins), [Electric Lights](electric-lights.md), [Fluid and Power Lines](fluid-and-power-lines.md), [Battery](battery.md), [Helper Bot](helper-bot.md), [Monster Cage](monster-cage-and-fish-tank.md) (Wither farming), quick read `clockwork-wastes.page.html`

## Concept

The user (2026-10-28): "I want a new biome, this is going to be the clockwork wastes, separate from the clockwork ruins we already have. Found in the end, it's going to be a city on an end island, complex structures, where you will find broken create machines, broken bots, technology is unknown. Electricity networks that when given power can illuminate it with little lamps and street lights there are. And one of my players wants a secondary use for the nether stars. This will be the first of three biomes we are going to add to the end. They must not clash with end city placement."

Clarified: **an End biome holding a dead clockwork city of unknown make. Players salvage its broken machines and bots, but the real prize is restoring its power: each district you light up opens its doors and vaults and wakes some of its bots. A Nether Star becomes fuel for the city's Star Reactors.**

## Decisions

| # | Area | Decision | Source |
|---|---|---|---|
| D1 | Kind | A new **biome** in the End, the **Clockwork Wastes**, holding a **city** of complex structures | user |
| D2 | Separate | Separate from the existing Clockwork Ruins (the Juggernaut's halls, Overworld and End) | user |
| D3 | Contents | Broken Create machines, broken bots, technology of unknown make | user |
| D4 | Electricity | The city has electricity networks; given power, its little lamps and street lights light up | user |
| D5 | Nether Star | A secondary use for Nether Stars (a player's request) | user |
| D6 | Series | The first of three End biomes. Its placement rules are written so the next two can share them | user |
| D7 | End cities | Must not clash with End city placement | user |
| D8 | House rules | Mod id has no "create"; dark, muted Create palette, previewed in Blockbench first; quiet sounds on the staging ladder; vanilla drop rates, pace limited by cost; game tests over manual checks | existing mod (memories) |
| D9 | Stars are farmable | The Netherite Monster Cage already farms Withers (64 Monster Chow each) for Nether Stars, so the new use is balanced by Wither cost, not star rarity | existing mod (`MonsterCageBlockEntity`) |
| D10 | Core loop | **Light it to open it.** Salvage broken machines and bots while exploring; the real prize is restoring power. Each district that gets power lights its lamps and wakes its locked doors, lifts and vaults | user, round 1 |
| D11 | Bots | **Salvage, some wake hostile.** Most broken bots are dead shells to strip for parts. Some are only dormant: when their district gets power, they stir and attack. Lighting the city has a cost | user, round 1 |
| D12 | Nether Star use | **Star Reactor fuel.** A Nether Star is a long-burning power source. The city's dead Star Reactors run on one (a star at the city's heart lights the whole grid), and a reactor salvaged or rebuilt powers a base for a long time before the star burns out | user, round 1 |
| D13 | Island | **On the big outer islands.** The Wastes takes over part of the outer End's highlands and midlands, and the city sits on that land. End cities never generate inside it (their biome tag leaves it out), and the city keeps at least 6 chunks from any End city (structure-set exclusion zone, like the End ruins). Accepted cost: islands that are Wastes have no End cities | user, round 1 |

| D14 | Rarity | **About 1 in 10** of the outer End's big-island area is Wastes. End cities stay common; all three End biomes together stay under a third of the land | user, round 2 |
| D15 | Giving power | **Substations, or the heart.** Each district has a dead Substation: feed it FE (cable a generator or Battery into it) and that district lights. Or set a Nether Star in the Star Reactor at the city's heart and the whole city lights at once | user, round 2 |
| D16 | City grid | **Derelict variants.** The city's lamps and cables are their own old blocks of unknown make. They light like ours but break into salvage scrap, never into free Electric Lights or Power Lines | user, round 2 |
| D17 | Salvage | **Scrap + rare devices.** Broken machines and bots give Create parts and a new salvage material. Powered vaults hold rare intact devices of unknown make: the parts for building your own Star Reactor and for later End tech | user, round 2 |
| D18 | Wings | **Wherever the Wastes replaces End-city land, wings must be found somewhere else**, so Elytra stay as available as before | user, 2026-10-28 ("if we replace an end city we must put wings somewhere") |

| D19 | Wings | **One vanilla Elytra per city, in the heart vault** behind the last locked door. Lighting the city earns it, as an End ship's Elytra is earned | user, round 3 |
| D20 | Shulkers | **Not needed.** The Wastes is a small share of the land; plenty of End cities remain for shulker shells. The city has no shulkers | user, round 3 |
| D21 | City size | **Medium: about 9 × 9 chunks (144 blocks), four districts round a heart** holding the Star Reactor | user, round 3 |
| D22 | Bots respawn | **No, a fixed set.** Each district has its own bots; destroyed, they stay destroyed. A lit, cleared district is safe to build in and can't be farmed | user, round 3 |
| D23 | Star Reactor strength | **8,192 FE/t for 8 hours of running per star** (about 4.7 billion FE), then the star burns out. Below the Geothermal Turbine (46,080 FE/t); it wins by needing no fuel line, lava or water, anywhere (airships). Accepted: a Wither cage (about 74M FE + 64 chow per star) can feed several reactors, held back by chow and its 20-minute pace | user, round 4 |
| D24 | Substations | **Wake once, lamps need upkeep.** The first 1,000,000 FE into a district's Substation wakes it for good: doors and its vault unlock, lifts work, its dormant bots wake. After that its lamps glow only while it's fed (about 500 FE/t) | user, round 4 |
| D25 | Bot kinds | **Two kinds plus a heart guardian.** Scuttlers: small, fast crawlers in groups. Sentries: wall or floor turrets that unfold and fire bolts. One big guardian at the heart vault, guarding the Elytra: a mini-boss, not Juggernaut-scale | user, round 4 |
| D26 | Salvaging | **Wrench for full, pickaxe for scrap.** Sneak-right-click with a Create Wrench takes a broken machine or bot shell apart for its full salvage (Create parts + scrap); a pickaxe breaks it into scrap only | user, round 4 |

| D27 | Defaults | **P1–P31 approved as written** (P14 amended by D29) | user, round 5 |
| D28 | Biome tech | **TerraBlender** adds the End biomes (all three), a required dependency. It has an End biome API (highlands, midlands, edge and island slots); confirm the API on 1.21.1 when building | user, round 5 |
| D29 | Reactor burn | **Flat out, like the other generators.** A fuelled Star Reactor always makes 8,192 FE/t; what isn't used fills stores and the rest is lost; a star burns out after 8 hours of running whatever the load | user, round 5 |
| D30 | Custodian fight | **Power-fed.** Wired to the heart's grid by four conduit pylons: while any pylon stands it's shielded and mends. Break the pylons (under fire, while Scuttlers come) to drop the shield, then fight it. At half health it overloads: faster, sparking, the heart's lamps surge. Replaces P20's sketch | user, round 6 |
| D31 | Heart vault | **The Custodian's death opens the vault.** All four districts awake opens the heart's doors and wakes the Custodian; the heart vault unlocks only when it's destroyed. If everyone leaves or dies, it walks back to its dais and mends fully. No grab-and-run for the Elytra. Amends P11 | user, round 6 |
| D32 | Automation | **Deployers salvage, arms feed.** A Deployer holding a Wrench salvages like a player (full salvage); drills, saws and other block-breaking contraptions give scrap like a pickaxe. Funnels, Mechanical Arms, Deployers and packages can feed Nether Stars into Star Reactors. Salvage is finite per city, so automating it is a convenience, not a farm | user, round 6 |
| D33 | 7th Unknown Device | **Reactor upgrade.** Fitting one Unknown Device into a Star Reactor makes each star burn 50% longer (12 hours instead of 8). One per reactor. Biomes two and three can add more uses | user, round 6 |
| D34 | TerraBlender facts | Checked against TerraBlender's 1.21.1 source (2026-10-28): version `1.21.1-4.1.0.8` (Maven `com.github.glitchfiend:TerraBlender-neoforge`, repo `maven.minecraftforge.net`). `EndBiomeRegistry.registerHighlandsBiome` / `registerMidlandsBiome(key, weight)`; vanilla's weight in each slot is 10, so **weight 1 in both slots ≈ 9%**. Within ~1,024 blocks of the origin it always returns `the_end` (central island safe). Patches are big and contiguous (about 1,024 blocks), with fuzzy edges, and don't follow island shapes. Its island slot is bugged on 1.21.1 (issue #179); we don't use it. `#minecraft:has_structure/end_city` is only `end_highlands` and `end_midlands` | research, 2026-10-28 |
| D35 | /kill and the vault | **Any Custodian death unlocks the heart vault, /kill included**; only a player kill drops its own loot (Unknown Device, scrap, XP). Grab-and-run stays impossible: nothing but a player can kill it. Amends P55 ("vault opens only on its player kill"), which would have left the vault locked for ever | user, 2026-10-28 ("allow /kill to unlock vault") |
| D36 | Lore and look | **"A race of automatons whose makers were overthrown by their creations long before the cities were destroyed by war."** The city is the automatons' own, built over their makers' Create-era town; glow is **pale starlight**; ruin state **intact core, broken edge**; bots are **their own family with eyes like ours**. **The war:** "automatons that found electricity and then turned on their masters, and the clockwork resistance: bots that helped their creators flee and fought back, eventually wiping each other out." Every hostile bot is the uprising (electricity wakes them); the resistance's dead are **Resistance Shells**, a look variant of the bot-shell salvage with the same rules (D26, P16), and never wake. The Elytra in the heart vault are the last pair of wings, sealed in by the uprising after the resistance flew the makers out. Look in [world](../briefs/clockwork-wastes-world.md), [blocks](../briefs/clockwork-wastes-blocks.md) and [bots](../briefs/clockwork-wastes-bots.md) briefs | user, 2026-10-28 (art direction) |
| D37 | Decor drops | The decor set (Scorched End Stone, Void-Alloy Plate / Pillar / Seamed Plate, Charging Alcove, Maker Statue, Scrap Heap) **drops itself as building blocks** when broken, so builders can use the city's look. Scrap stays the reward from salvage blocks. No crafting recipes in v1, so void-alloy is finite per city (proposed default) | user, 2026-10-28 |
| D38 | The resistance | Story only in the Wastes: Resistance Shells are ordinary salvage (D36). Any real role for the resistance is **saved for End biome 2 or 3**; that lore is decided later | user, 2026-10-28 |
| D39 | No v2 | **Everything in the spec ships.** No item is deferred to a later version; anything the spec is silent on, or that can't be built as written, goes to the user before it's built | user, 2026-10-29 ("There is no v2, why cut the lifts?", "Do not make choices without agreeing with the user") |
| D40 | Lifts | Built (P49), to the approved design in `design/models/clockwork-wastes-lift.gen.js`: a 2 x 2 void-alloy deck with a lit edge groove, guard rails on its two closed sides (open front and back for the 2-wide doors), four guide rails with clamps, a control post, a call plate at each landing, a drive drum at the head of the shaft | user, 2026-10-29 |
| D41 | Leaning lamps | Built: each block of a leaning post has its own model, every part turned 22.5 degrees about the post's foot, so the slices line up into one leaning post | user, 2026-10-29 ("Add a second model in") |
| D42 | After waking | Per P50: woken vaults, doors and Substations are ordinary blocks, breakable by players and contraptions; locked ones stay unbreakable and contraption-proof | user, 2026-10-29 |
| D43 | Pylon hits | 6 hits from players or their projectiles, at most one counted every 8 ticks (so sweeps and multishot can't skip it); a pylon re-lit by the overload needs the full 6 again | user, 2026-10-29 |
| D44 | Heart star | A Nether Star in the heart lights the whole city, including a district whose Substation was broken | user, 2026-10-29 |
| D45 | Lamp timing | Lamps ripple on at the wake (with the lever's clunk); upkeep keeps them lit afterwards | user, 2026-10-29 |
| D46 | Vault screen | The GUI kit with a void-alloy material band (previewed first), not the Upgraded Chest's copper band | user, 2026-10-29 |
| D47 | Star Reactor facing | Front faces the placer like the other machines, any of six facings; the copper FE terminal is on the far side | user, 2026-10-29 |
| D48 | Unloaded upkeep | Per P24: an unloaded Substation draws its upkeep through the Power Lines network on paper | user, 2026-10-29 |
| D49 | Icons, poses, statue, weight | The six new flat icons get a texture review before they go in; the slumped shells are re-posed (hands in front of the wall, legs on the floor), previewed first; the toppled statue lies on the ground with its head in the third block at 22.5 degrees; the biome weight is a common (startup) config | user, 2026-10-29 |
| D50 | Icons and poses, approved | The six flat icons carry a 1 px dark outline (#121118) round their silhouette, as the Cuckoo Clock; the shells are re-posed so every pose rests on the floor and the slumped Worker sits against its wall, knees up, hands in front (previewed and approved, exported) | user, 2026-10-29 |
| D51 | Re-waking | After a reset, the kneeling Custodian wakes again when a player is within about 16 blocks with line of sight to it | user, 2026-10-29 |
| D52 | Dormant bots | Untouchable while dormant: a district's bots are fought after it wakes. A kneeling Custodian is untouchable too | user, 2026-10-29 |
| D53 | Bot loot | Sentry: 1–2 Clockwork Scrap and a 15% Electron Tube; Custodian: 4–8 scrap besides its Unknown Device; both only on a player's kill. Scuttler scrap drops on any death | user, 2026-10-29 |
| D54 | Overload lamps | At the overload every heart lamp surges and flickers for about 3 s, then settles (P36) | user, 2026-10-29 |
| D55 | Tethers and clips | The brief's copper tether cables from the Custodian's back sockets to the pylons are built, after a Blockbench preview. A clip pass builds every brief expression and reaction that has no clip, and the eight new Custodian clips (walk, shockwave, bolt volley, chain arc, overload, wrath, shield down, kneel) are previewed; the user reviews all in Blockbench before they go in | user, 2026-10-29 |
| D56 | Peaceful, stuck, shockwave | Switching to Peaceful sends woken bots dormant (they wake again when difficulty rises). A Custodian that can't make progress home for 10 s appears on its dais. The shockwave is a real starlight ring running out 8 blocks along the floor, hit as it passes (jump it) | user, 2026-10-29 |
| D57 | Fight and vents | The Custodian's fight numbers in code are kept to start, tuned in play (targets nearest player within 32; slam within 2 in front; shockwave on the ground within 8; volley in sight within 24; chain arc 1 in 3 overloaded; 1.5 s between moves; Scuttler speed 0.35, Custodian walk 0.18, bolts 1.5 blocks/tick). Overload: not held at half health, a random broken pylon re-lights, interrupts the move; a reset starts a whole new fight. Called Scuttlers burst out of **wall vents** the city places in the heart hall (a new block, designed and previewed first) | user, 2026-10-29 |
| D58 | Small details | Boss bar yellow (nearest to brass); hitboxes Scuttler 0.75 x 0.5, Sentry 1 x 1, Custodian 1.4 x 3; a Sentry fires once its aim is within 10 degrees; seams stay lit while a bot sleeps (only lenses go dark); on a player's kill the heart vault opens when the death clip ends, /kill opens it at once; toughness fixed | user, 2026-10-29 |
| D59 | Previews approved | The bots' clip pass (51 clips incl. the reworked Custodian fight clips and the small fixes to approved clips), the Custodian's copper tether cables (heart-hall preview), the wall vent (vent preview) and the City Vault screen option A (void-alloy band, lit starlight seam along its foot, title in starlight) | user, 2026-10-29 |
| D60 | Bot look rules | In wrath the Custodian's top and bottom lenses go fully ember (the brief, over the one-pixel rule); the code aims the head for wrath / mourn / glance-at-vault; the Scuttler walks at 0.45 s a cycle and runs at 0.3 s; 8 single-use vents, two beside each pylon, each left open; on death the tether cables unplug and drop; the other preview details as shown (Sentry lens swivel 5 degrees, watching reworked, counting on its palm, cable routing, vent catchlight, the kit's body under the vault band) | user, 2026-10-29 |
| D61 | Sky-cable spans | A new **Derelict Insulator** block for towers and masts (designed and previewed first) holds spans as a Line Post head does, drawn in the derelict cable look; the Substation's tray is the district end; the city links them, players can't make them; a broken anchor drops its spans as scrap; a snapped variant hangs from one end. The wake bead runs ~2 s along each span leaving the woken Substation, starting at the lever's slam | user, 2026-10-29 |
| D62 | Lifts, details | The call plate's lens is dark until pressed and lit while the call waits; the Lift and call plate get flat icons with a texture review; the lift's build, operation and odds and ends as built (custom deck; one Lift block at the shaft head; landings = shaft floor + call plates within 4; post use = up, sneak-use = down; halts when blocked; calls while moving ignored; players only; works once awake, fed or not; settles at the top; drops 2–3 scrap / 1; names Lift, Lift Call Plate, Lift Deck; shafts up to 64; uncarryable; never mirrored) | user, 2026-10-29 |
| D63 | Kept as built | Leaning posts are city-only; woken city blocks stay uncarryable (Carry On); an unloaded Substation with no line goes unfed, paper upkeep ignores line rates and Splitter priority; the shockwave ring (0.5 blocks/tick, passes walls, hits on its floor, tuned in play); bot details (Peaceful re-wake, untouchable kneel at reset end, fight Scuttlers crumble on Peaceful, stuck = 0.25 block in 10 s); block details (dark heart lamps flicker in the surge, lit decor casts no light and player ones stay dark, same-side cable joins, re-lit pylon keeps its failing look, no reactor sneak-reverse) | user, 2026-10-29 |
| D64 | Derelict Insulator | Approved as previewed (`design/models/clockwork-wastes-insulator.gen.js`): a three-tier chamfered void-alloy insulator stack with one edge-tone neck ring lit while its district is fed; one block with three mounts by the face it's placed on (top: a collar whose claws bite a girder, kept on void-alloy towers too; wall: a plate and braced arm; hanging: a socket under a beam, doubling as the spire's crown-arm ends); holds several spans; spans 1.2 px wide (two patina strands and a hairline gap, copper where they rub at the ties); a snapped span hangs about 3 blocks with a curl, bare copper at the break, a slight sway, never lit. The city's masts are Create Metal Girders. The wake bead runs 2 s per span, hopping span to span up to the crown, each span's line lighting in behind it | user, 2026-10-29 |
| D65 | Lift icons | The Lift's flat icon is its deck halfway up an open shaft; the Lift Call Plate's shows its lens lit (`design/texture-reviews/clockwork-wastes-icons/lift-icons.js`) | user, 2026-10-29 |
| D66 | City review | The city is reviewed in game, not as Blockbench vignettes: a dev command builds the whole city next to the user in a test world to walk round; it goes into world generation only after the user approves it | user, 2026-10-29 |
| D67 | Previews in the mod, follow-ups | The Scuttler plays `curious` when a player it can't fight (creative or spectator) is within about 8 blocks; wall vents re-seal with a new Scuttler when the Custodian resets (the smoke at the pylon's foot only for a broken or unloaded vent); a hit flips a Scuttler at most once every 4 s; a short clip pass adds mirrored left-side hits for the Sentry and Custodian, previewed in Blockbench first; tether beads crawl only on standing pylons' cables; the city builds the preview's 3×3 one-block dais; kept as built: wrath then call when a pylon breaks (a pylon broken during wrath adds to the next call), Scuttler grief 8 blocks / huddle 1.5 blocks / called Scuttlers still for the 2.4 s burst, the Wall Vent block (unbreakable until spent, then hardness 5, 1 scrap, uncarryable, full collision, shut-vent item), Custodian mannerisms every 6–10 s with no move, head aim 75° / 40° in wrath and 1.5× the clip's turn for mourn and glance, the Sentry fires from 0.3 s into `target` with code aim replacing the clip's turn, fallen cables vanish at 6.5 s, only the calling plate lights | user, 2026-10-29 |
| D68 | D67 follow-ups | The mirrored `hit_left` clips for the Sentry and Custodian are approved and in the game (`CityBot.LEFT_HIT_CLIPS` on); the Scuttler is curious for 3 s, then waits 20 s; a vent unloaded at a reset stays open until a later reset; a re-sealing vent snaps shut with no animation; a Scuttler hit inside its 4 s flip window plays no clip | user, 2026-10-29 |
| D69 | City systems | City spacing stays 32; Elytra parity is made up by giving outskirts-only sites (those that fail island fit) their own Elytra; the Derelict Insulator is on Carry On's blacklist; the brief items still standing in as plain blocks (half-built assembly-line bodies, alcove trinkets, brass plaques, dead vines, cable up the lattice masts) are modelled next and previewed in Blockbench; kept as built: spacing in the datapack, the highlands fallback as a second structure in the set, dips filled up to 6 and trestles to 32 deep, island-fit failures get wrecks only, crown insulators bound to their cable's district, the bead runs only for a wake seen loaded and fed, 1 scrap per 4 blocks of fallen span, sag 0.075, a 4 s spire climb, rebuilding at a spot resets that city | user, 2026-10-29 |
| D70 | Resistance cache | Sites that fail island fit hold a resistance cache: a vanilla Barrel in the site's middle with exactly one Elytra (loot table `chests/clockwork_resistance_cache`, nothing else), behind a short siege-line cover wall with one slumped Resistance Shell, unlit and unguarded; it's the pair the resistance never came back for, so D36's "last pair of wings" stands for the heart; the floating-wreck fix, `/wastes buildoutskirts` and the five-seed parity tests are kept | user, 2026-10-29 |
| D71 | City buildings | The first in-game walk: buildings were bland one-material boxes with no texture or detail. They are redone as a building kit: an art-director brief per building type (makers' shed, furnace shed, terrace, long workshop, slotted tower, alcove wall, gantry, Substation yard, heart drum, and their war-damaged versions) with real relief and reasoned wear; a void-alloy building block set (stairs, slabs, walls, slotted grille panel, trim or cornice, a clamp bracket onto old stonework) previewed in Blockbench first; each building built as an in-game catalogue (`/wastes buildcatalogue`), shown to the user as in-game screenshots (street level and distance) before they walk it; approved buildings then replace the boxes in the city | user, 2026-10-29 |
| D72 | City props | Approved as previewed (`design/models/clockwork-wastes-city-props.gen.js`): the Assembly Clamp (one block, stages 1–5, facing; baked like the shells with one-block collision; unlit void-alloy jig; copper harness in stages 1–3; stage 5 replaces the `frozen` Worker on the line; salvageable like the shells; flat icon); alcove trinkets as a `trinket` state (none, cog, key, cart, framed flower) on the Charging Alcove's new corner shelf, in 1 alcove in 5, kind at random, key standing; dead vines as a `vines` state on the Charging Alcove, about 1 in 4 in the Quarters' alcove wall and split tower; the Brass Plaque, a thin wall block (one bolt lost), beside makers' shed doors and on terraces about 1.5 up, askew in the outer districts, drops itself; the mast riser as a `girder` mount of Derelict Cable (foot, run, top; a clamp every block; pothead and unlit jumper to the insulator; lights with its district), the yard's tray span kept; and **corner masts**: each district also gets a lattice mast with a riser at a street corner, as the world brief says | user, 2026-10-29 |
| D73 | Building kit brief | `design/briefs/clockwork-wastes-buildings.md` approved with all its recommendations: one stone per district (Foundry scoria firebrick, Workshops tuff with andesite-casing piers, Rail Yard deepslate, Quarters End brick), slate roofs with a copper ridge; the makers bricked up their own terrace doors in hasty rubble; new blocks beyond D71's set: a Stained End Stone Brick set (bricks, cracked, stairs, slab, wall), the Scarred Plate (chipped, cracked, heat-tinted; only cracked glows dim) and Key-Marked Stone (the winding key on the siege works); no limestone, calcite, framed glass, smooth stone, brass block or mossy blocks, andesite bars for makers' work and void-alloy wall for automatons' work; lined cut doorways, the pouring channel, the stripped dome, open roof hatches; a void-alloy gantry; the brief's lighting list; damage rings intact to 22, worn 23–45, damaged 46–70, ruined beyond, tops fall toward the heart; four-leg girder lattice masts, corner masts at u,v 35 with a span to the sky tower; sky tower 5×5, chimneys 3×3, vault house on an 11×11 plinth; plaques also on the furnace shed, workshop, control hut, signal box and heart doors; the Rail Yard canopy and signal box in the kit; void-alloy slab switchbacks on the alcove wall | user, 2026-10-29 |
| D74 | Props in the mod, follow-ups | Salvage wrecks are all carryable (the Assembly Clamp comes off Carry On's blacklist, like the shells); the Derelict Cable's corner knot widens to 2.5 px to end its z-fight, previewed first; the Charging Alcove's flat icon is redrawn with the corner shelf, previewed first; kept as built: the corner mast is a branch of the bead's path from the yard mast, at `CORNER_MAST` by the street crossing with a ~12-block span, risers on the far side from spans, ordinary cable for the yard's ground run, the clamp item places stage 5 (whole-Worker icon), the shells' salvage at every stage, the plaque carryable, askew plaques decided by the building kit's damaged versions, plaques to the doorway's left 1 up, one trinket in each five alcoves, trinkets and vines only from the city, vines 1 in 4, players can make risers on girders, wrecks kept out of the core | user, 2026-10-29 |
| D75 | Building block set | Approved as previewed (`design/models/clockwork-wastes-building-blocks.gen.js`). The void-alloy base surface is option C, an ashlar bond of 1×1 and 2×1 plates in offset courses picked by position, replacing the striped `va_tile` everywhere (every void-alloy block in the mod); Stained End Stone Brick at 97 brightness with three weighted variants and dusty tops; two new blocks: a Broken Makers' Window (dark, dusty, mostly broken glass in iron frames) and a Patina Rib block (muted copper patina for the dome ribs); kept as recommended: no riveted variant (no rivets in the world), the grille's dim pane across each slot, Band connection states for flush joins, shaped blocks on the plain plate, slab ramps and decks with a plate kerb, an axis state on Band and Grille for fallen tops, wall straps at tower feet, band rings through corners, End dust on brick tops only and no soot on the intact heart, cut-deepslate dressings on the drum, Key-Marked Stone scratched into Scorched End Stone, the tower walkway deck | user, 2026-10-29 |
| D76 | Block set follow-ups | Machines and props (alcove, door, pylon, lift guides, reactors, vault, vent, clamps…) take the frameless void-alloy field so no lines cross one object; the framed plain plate stays on stairs, slabs and walls; the small-parts `va` texture is repainted with the new grain (no diagonal sheen anywhere), previewed first; Derelict Cable's swapped up/down wall arms are fixed with a game test; the 2.5 px cable knot and the redrawn Charging Alcove icon (plain, no seams) go in as previewed; recipes: void-alloy stairs, slab, wall, band, grille and clamp bracket from the Void-Alloy Plate in a Stonecutter, Stained End Stone Bricks from End Stone Bricks plus brown dye, the Patina Rib from copper, the Broken Makers' Window from iron bars and glass; Key-Marked Stone and Scarred Plates city-only; the rest of the D75 agent's choices kept as built | user, 2026-10-29 |
| D77 | Small parts and recipes | The small-parts `va` texture is repainted with the house grain as previewed (`VA_GRAIN` on; also `sub_sigil`, `lift_sigil` and the shells); the bots' diagonal sheen bands (`vaPaint` in `clockwork-wastes-bots.gen.js`) are repainted with the same grain, previewed first; kept as built: the Wall Vent and riser guard on the frameless field, 1 Clamp Bracket per plate, cracked stained bricks by smelting, the Patina Rib from 2 stacked copper blocks of any weathering, the window BGB/BGB for 4, no recipe-book unlocks | user, 2026-10-29 |
| D78 | Bot grain | The bots' diagonal sheen bands become the house grain with the lit top-left edge kept (version A, `BOT_GRAIN = 'grain'`), no dark bottom-right edge; tops and undersides stay flat; the slightly lighter average is accepted so the bots match the city | user, 2026-10-29 |

## Defaults (P1–P31 approved by the user, 2026-10-28)

| # | Area | Proposal |
|---|---|---|
| P1 | Biome placement | The Wastes takes about 1 in 10 of the outer End's highlands/midlands, chosen by large-scale noise so a region is a few hundred blocks across. Never on the central island (vanilla's outer End starts about 1,000 blocks out). Weight in config. Built as a shared "End biome slot" list so the next two biomes slot in beside it |
| P2 | End cities | The Wastes is never in `#minecraft:has_structure/end_city`, so no End city starts inside it. The city's structure set keeps 6 chunks from `minecraft:end_cities`. End gateways are untouched (the dragon fight places them, not biomes) |
| P3 | City placement | Cities in the Wastes only (random spread; spacing 32 chunks, separation 12, from P45; was "one per region, about 40-chunk spacing" before TerraBlender showed regions are about 1,024 blocks). Its heart needs End Stone under it; districts reaching past the island's edge stand on struts and bridges over the void. A region with no room for a city still gets outskirts: scattered wrecks, scrap heaps, a fallen lamp post |
| P4 | Layout | The heart (Star Reactor hall, heart vault, the tallest tower) in the middle; four districts round it: **Foundry, Workshops, Rail Yard, Quarters**. Each district has a Substation, one locked vault, streets of Derelict lamps on Derelict cables, broken machines, slumped bot shells, 2–4 dormant Sentries and a pack of 3–5 Scuttlers. Bridges join them |
| P5 | Land and air | Scorched, rust-stained End Stone, scrap heaps, toppled cogs, tangled cable; no chorus. Fog and sky tinted rust-brown; faint drifting brass-dust particles; distant ticking and creaks as ambience; no music. Look goes to the art director |
| P6 | Mobs | Endermen spawn as in the rest of the outer End, at half the rate. Scuttlers, Sentries and the guardian are only ever placed by the city, never spawn naturally |
| P7 | Substation | A block in each district. Takes FE from anything that gives it (Power Lines, a Battery beside it). Goggles show the wake charge (x / 1,000,000 FE), then once woken whether it's fed and its draw. Wake charge and upkeep in config. Short of upkeep, its lamps flicker and go dark like Electric Lights |
| P8 | Waking | When a Substation wakes: the district's lamps ripple on down the streets, doors and the vault unlock with a heavy clunk, lifts start, and within 5 s its Sentries unfold and Scuttlers stir. Action bar for players nearby: "The Foundry wakes…" |
| P9 | Sentries need power | Sentries run off their district's grid: while the district is unfed (dark), its Sentries fold up and stop. So a player can wake a district with a carried Battery, unhook it, and walk into the dark safely past the Sentries (Scuttlers stay awake) |
| P10 | The heart | A Nether Star in the heart's Derelict Star Reactor wakes all four districts at once (everything wakes together) and runs the whole city's lamps for that star's 8 hours. Its spare power can be tapped with a Power Line, so a lit city can become an outpost |
| P11 | Heart vault | Its doors open once all four districts have woken (by Substations or the heart), which wakes the Custodian; the vault itself opens on the Custodian's death (D31). Holds the Elytra (D19), 2 Unknown Devices and good loot |
| P12 | Unknown Device | The rare intact tech: 1 in each district vault, 2 in the heart vault, 1 from the guardian: 7 per city. A Star Reactor takes 3, so one city builds two reactors with one to spare: the spare is the reactor upgrade (D33) |
| P13 | Star Reactor recipe | Mechanical Crafter: 3 Unknown Devices, 1 Brass Casing, 1 Precision Mechanism, 4 Clockwork Scrap. The city's own Derelict Star Reactor can't be moved; wrench-salvaged it gives 2 Unknown Devices (the heart has no reactor afterwards) |
| P14 | Star Reactor rules | One star at a time, put in by hand or funnel. ~~Burns only what the network draws~~ Runs flat out, 8,192 FE/t for 8 hours of running (D29). Redstone pauses it (and the star's burn); a comparator reads the star left; goggles show output and time left at the current draw. Broken or picked up, it keeps the star's remaining burn. Never explodes or melts down. Works on Aeronautics ships; on paper while unloaded |
| P15 | Clockwork Scrap | The salvage material. Crushing Wheels turn it into iron, copper and brass nuggets with a small chance of zinc. Used in the Star Reactor and kept for the next End biomes' tech |
| P16 | Salvage yields | Broken machine, wrench: 1–3 Create parts matching what it was (cogwheels, shafts, casings, belts; rarely a Precision Mechanism) + 1–2 scrap. Bot shell, wrench: scrap + a chance of an Electron Tube. Pickaxe: 1 scrap. Derelict lamp: 1 scrap; Derelict cable: 1 scrap per 4 |
| P17 | District vault loot | Brass, zinc, Electron Tubes, Precision Mechanisms, scrap, 1 Unknown Device, sometimes an enchanted book |
| P18 | Scuttler | About half a block, 12 health (P42), fast, climbs walls, leaps; 5 damage (P42); groups of 3–5. Drops 0–2 scrap, 5 XP |
| P19 | Sentry | Fixed to a wall or floor, 30 health, needs line of sight, fires a bolt every 2 s (7 damage from P42, 24 blocks). Drops scrap and sometimes an Electron Tube, 10 XP |
| P20 | Guardian | (Superseded by D30, P32–P41: 400 health, pylons.) **The Custodian**: about 3 blocks tall, 300 health, heavy melee, a ground shockwave, calls 2 Scuttlers; brass boss bar within 32 blocks. Never breaks blocks; never leaves the heart; never respawns. Drops 1 Unknown Device, scrap, 100 XP |
| P21 | Locked things | Locked doors and vaults can't be broken or blown up until their district wakes (so walls can't be dug round to skip the lock: the loot is inside the vault block). Once woken, everything is ordinary and breakable |
| P22 | Peaceful | Bots never wake; doors and vaults still unlock, so the loot is still there |
| P23 | Multiplayer | Whoever powers a Substation wakes it for everyone. Vaults are shared: first come, first served; one Elytra per city, like an End ship |
| P24 | Unloaded | Substations keep their state; upkeep runs through the Power Lines network on paper; bots never despawn and stay where they were |
| P25 | Derelict lamps | Light like Electric Lights while fed; not dyeable and no brightness steps; break into scrap. Players can't craft them |
| P26 | Config | Biome weight, cities on/off and spacing, wake charge, upkeep, reactor output and burn time, bot and Custodian health and damage |
| P27 | Teaching | Ponder: "Waking a district" (Substation) and "The Star Reactor". JEI info on the Unknown Device and Clockwork Scrap. Advancements: "Lights On" (wake a district), "City of Lights" (wake all four), "Star Power" (fuel a Star Reactor) |
| P28 | Sounds | On the staging ladder: city ambience, lamps rippling on, the waking clunk, Scuttler skitter, Sentry unfold and bolt, the Custodian, the reactor's hum |
| P29 | Names | Clockwork Wastes, Substation, City Vault, Derelict Street Lamp / Wall Lamp / Cable, Star Reactor, Derelict Star Reactor, Unknown Device, Clockwork Scrap, Scuttler, Sentry, the Custodian |
| P30 | Website | The codex's Biomes section opens with the Clockwork Wastes page |
| P31 | Tests | Game tests: no End city in or within 6 chunks of a Wastes city; wake at 1M FE; locked blocks unbreakable; vault opens after four wakes; reactor burns flat out and keeps its burn when broken; Sentries fold when unfed |

## Defaults round 2 (P32–P56 approved by the user, 2026-10-28)

Damage numbers start higher than first instinct: the Juggernaut's first numbers did under a heart through full diamond with Protection IV and had to be raised three times (Juggernaut D21).

| # | Area | Proposal |
|---|---|---|
| P32 | Arena | The heart hall: about 31 × 31 blocks, the Custodian kneeling on a dais in the middle, a conduit pylon (3 blocks tall, a glowing coil on top) in each corner, cables arcing from each pylon to the Custodian's back while it's shielded |
| P33 | Pylons | A pylon's coil breaks after 6 hits from any melee weapon or projectile (it sparks and dims as it weakens); it can't be mined, and can't be harmed before the Custodian wakes. Contraptions can't hurt it (as the Juggernaut, D51). Broken, it slumps and stops arcing |
| P34 | Shield | While any pylon stands, the Custodian takes no damage and mends 2 health a second per standing pylon. All four down: the shield drops with a crack and it takes full damage |
| P35 | Pressure | Breaking a pylon makes the Custodian turn on the breaker; each broken pylon calls 2 Scuttlers out of the walls (8 in the whole fight). Fight Scuttlers drop nothing and crumble when it dies or resets, so they can't be farmed |
| P36 | Overload | At half health it overloads once: 30% faster, sparks pour off it, the heart's lamps surge and flicker, and it re-lights **one** pylon (the shield comes back until that pylon is broken again) |
| P37 | Moves | Slam (20 damage, 2 blocks in front, 1 s wind-up); shockwave ring (12, jump it, 8 blocks); bolt volley (3 bolts, 8 each, 24 blocks); from overload, chain arc (10 to the nearest player, jumping to one more within 6 blocks). Never breaks blocks |
| P38 | Toughness | 400 health (raised from 300 so the shieldless phase isn't over in seconds against netherite). +50% per extra player within 32 blocks when it wakes, given as toughness like the Juggernaut. Knockback-resistant; immune to falls, fire, drowning and contraptions; it never leaves the hall |
| P39 | Reset | No player within 48 blocks for 30 s, or every fighter dead: it walks back to its dais, every pylon re-lights, it mends fully, fight Scuttlers crumble. The vault stays locked |
| P40 | Death | The heart vault unlocks with the heavy clunk. It drops 1 Unknown Device, scrap and 100 XP; a /kill or void death drops nothing (only a player's kill counts, like the Juggernaut) |
| P41 | Peaceful | It never wakes; with four districts awake the heart vault simply opens |
| P42 | Bot numbers | Scuttler 12 health, 5 damage; Sentry 30 health, bolts of 7 every 2 s (raised from 3 and 5 for the same reason as P37) |
| P43 | Machine salvage | A Deployer with a Wrench salvages (full salvage) like a player's sneak-click; Drills, Saws and other contraption breakers give scrap only. Locked doors, vaults, Substations and pylons ignore every contraption |
| P44 | Reactor upgrade | Right-click a Star Reactor with an Unknown Device to fit it (also by Deployer). A visible part appears on the reactor; goggles say "Overhauled: 12 h a star". Permanent: kept when broken or picked up |
| P45 | Elytra parity | Vanilla puts End cities about one per 20 × 20 chunks in the highlands (only some with a ship), so a 1,024-block Wastes patch displaces several. Cities are spaced **32 chunks, separation 12** (never overlapping), about 3–4 per patch, each with one Elytra. A game test counts Elytra per area in a Wastes patch against vanilla End-city ships on the same seed, and the spacing is tuned until the Wastes give at least as many |
| P46 | Island fit | A city only places where at least half of its 9 × 9-chunk footprint has End Stone at the heart's height; the rest stands on struts and bridges. Fail: that spot gets outskirts only. A test samples 200 seeds' worth of placements and reports how many fit |
| P47 | Island split | A Wastes patch may cover part of an island; the biome shows on that part only (rust-brown haze, scrap), and an End city may stand on the vanilla part. The 6-chunk exclusion keeps it away from every Wastes city |
| P48 | Wake records | Each city keeps one saved record in the level (city origin → each district awake or not, heart awake, Custodian dead). Every city block reads its district's state from that record when its chunk loads, so a district half-unloaded at waking catches up without loading anything |
| P49 | Doors and lifts | Doors slide open into the wall when woken and never close on anyone. Lifts move only when a player standing on one calls it, and stop if anything is in the way |
| P50 | After waking | Substations, lamps and vaults become ordinary blocks: break one and the district stays awake (the record holds); a broken Substation drops scrap and its lamps go dark for good |
| P51 | Endermen | City blocks are never picked up by Endermen |
| P52 | Lag | Lamps don't tick; Substations tick once a second; a woken district's lamps come on over 1 s (the Electric Lights ripple); about 20 bots per district and 8 at most in the fight |
| P53 | Other End mods | If another mod replaces the End's biomes (BetterEnd, Nullscape) or a datapack End stops TerraBlender's, the Wastes can't appear. The mod logs one warning, and a config option (off by default) lets cities place in vanilla highlands instead, keeping the 6-chunk End-city exclusion |
| P54 | Dependency | TerraBlender `1.21.1-4.1.0.8` or newer, required, in `mods.toml` and `build.gradle` |
| P55 | Tests (extra) | Custodian shield holds while a pylon stands; reset re-lights pylons; vault opens on its death, its loot drops only on a player kill (amended by D35); Deployer salvage; reactor upgrade burns 12 h; wake record catches up on chunk load; Elytra parity and island-fit sampling (P45, P46) |
| P56 | Teaching (extra) | Advancements: "Short Circuit" (break a pylon), "Custodian Down" (defeat it). The Star Reactor Ponder shows the upgrade |

## Open Questions

None. Later, not v1: the next two End biomes (they share the End biome slot list, P1, and Unknown Devices, P12); shulker shells if End cities ever feel scarce (D20).

## Core Loop

1. **Find it.** Fly or bridge out across the outer End; a rust-brown haze and drifting brass dust mark the Wastes.
2. **Salvage the streets.** Wrench broken machines and bot shells apart for Create parts and Clockwork Scrap (or send a Deployer with a Wrench).
3. **Wake a district.** Carry power in (a charged Battery, a generator and a line) and put 1,000,000 FE into its Substation. Its lamps ripple on, its doors and vault unlock, and its Sentries and Scuttlers wake.
4. **Fight and loot.** Clear the woken bots, open the vault: parts, scrap, one Unknown Device.
5. **Light the heart.** Wake all four districts, or set one Nether Star in the heart's reactor to wake them all at once. The heart's doors open and the Custodian wakes, shielded by four pylons. Break the pylons, beat it, and the heart vault opens: the Elytra and more Unknown Devices.
6. **Take it home.** Three Unknown Devices build a Star Reactor: a Nether Star becomes 8 hours of 8,192 FE/t anywhere, airships included. A fourth device upgrades it to 12 hours a star.

Why players come back: Elytra (each Wastes patch holds 3–4 cities, at least as many as the End-city ships it replaces), Unknown Devices for reactors, and a lit city to use as an outpost.

## Behaviour / Rules

1. **Biome.** TerraBlender, weight 1 in the highlands and midlands slots (about 9% each); never the central island (D13, D14, D28, D34, P1).
2. **End cities.** None start inside the Wastes (biome tag); none within 6 chunks of a Wastes city (exclusion zone) (D7, D13, P2, P47).
3. **Cities.** Spacing 32 chunks, separation 12, about 144 blocks each: a heart and four districts (Foundry, Workshops, Rail Yard, Quarters). At least half the footprint on End Stone, or the spot gets outskirts only (D21, P3, P4, P45, P46).
4. **Salvage.** Wrench (by hand or Deployer) gives full salvage; pickaxe, drill or saw gives scrap. Derelict lamps and cables give scrap only, never our Electric Lights or Power Lines (D16, D26, D32, P16, P25, P43).
5. **Substation.** Dormant until it has taken 1,000,000 FE in total, then woken for good. Afterwards its lamps need 500 FE/t of upkeep (D24, P7).
6. **Waking.** The lamps ripple on, doors slide open, lifts start, and the bots wake within 5 s. The city's saved record holds who's awake, so unloaded parts catch up when loaded (P8, P48, P49).
7. **Sentries** fold and stop while their district is unfed; **Scuttlers** don't care (P9).
8. **Locks.** Locked doors, vaults, Substations and pylons are unbreakable, blast-proof and immune to contraptions until woken (P21, P43).
9. **Heart.** Four districts awake opens the heart's doors and wakes the Custodian. Four pylons shield it and mend it; break each with 6 hits. At half health it overloads and re-lights one pylon. Its death opens the heart vault (only a player's kill drops its loot; nothing but a player can kill it). Everyone gone for 30 s or dead: it resets fully (D30, D31, P32–P40).
10. **Star Reactor.** Flat out at 8,192 FE/t while it holds a star, 8 hours a star (12 with an Unknown Device fitted); redstone pauses it; it keeps its burn and upgrade when broken (D23, D29, D33, P14, P44).
11. **No respawns.** A city's bots and loot are a fixed set; the Custodian's Scuttlers drop nothing (D22, P35).
12. **Peaceful.** Nothing wakes; locks open, and the heart vault opens once four districts are awake (P22, P41).

## Progression

**End game.** You need the dragon dead (the outer End), a way across the void, a portable power source (a Battery from the Power Lines tier), and gear for a mini-boss. The heart route needs a Nether Star, so a Wither kill or the Netherite cage. It unlocks the Elytra, the Star Reactor (compact, fuel-free power), its 12-hour upgrade, and Unknown Devices, which carry on into the next two End biomes.

## Balance

| Check | Finding | Fix / decision |
|---|---|---|
| Cost | 1M FE per district: one Battery (10M) wakes up to ten. Lamps 500 FE/t, about half a Lava Generator | Config (P26) |
| Reward | One Elytra and 7 Unknown Devices (two reactors + one upgrade) per city; parts and scrap | Finite per city (D22, P12, D33) |
| Risk | Sentries (7 a bolt), Scuttlers (5), the Custodian (slam 20, 400 health behind four pylons), the void | Numbers start high after the Juggernaut lesson (P37, P42); Sentries fold if you unhook the power (P9) |
| Star loop | One Wither (about 74M FE + 64 chow) makes 4.7B FE in a reactor (7B upgraded), so the cage feeds reactors at a net gain | Accepted (D23): pace limited by chow, the 20-minute Wither time, and finite reactors (devices only come from cities) |
| Free lights | Stripping the city for Electric Lights and Power Lines | Derelict variants only drop scrap (D16) |
| Bot farming | Woken bots or the Custodian's Scuttlers farmed | Bots never respawn; fight Scuttlers drop nothing (D22, P35) |
| Skipping locks | Digging round a door, or a drill contraption through a vault | Loot is inside the vault block, unbreakable and contraption-proof while locked (P21, P43) |
| Elytra grab-and-run | Taking the Elytra without the fight | The vault opens on the Custodian's death, and nothing but a player can kill it: contraption-immune, never leaves the hall (D31, P38, P40) |
| Elytra duplication | Several players each taking an Elytra | One shared Elytra per city (P23) |
| End cities lost | A Wastes patch displaces several End cities | 3–4 cities per patch, each with an Elytra; a test checks parity with vanilla ships (P45) |
| Overpowered? | Reactor (8,192 FE/t) under the Turbine (46,080) | Its edge is fuel-free placement, not raw output |
| Useless? | Substation upkeep with nothing to gain | Upkeep keeps the city lit as an outpost and powers its Sentries (P9, P10) |

## Create Integration

| System | Interaction | Notes |
|---|---|---|
| Shafts and gears | None in the city | Broken machines are salvage; the city's power is FE |
| Belts, depots | None | — |
| Contraptions | Drills and saws break salvage blocks into scrap; they can't touch locked blocks or pylons. Star Reactor works on Aeronautics ships | P43, P14 |
| Trains | The Rail Yard district has broken track and a broken train as salvage | Nothing drives; v1 decor |
| Logistics / packages | Packages can deliver Nether Stars to a reactor's funnel | D32 |
| Funnels, chutes, arms | Feed Nether Stars into a Star Reactor | D32, P14 |
| Deployers | Wrench: full salvage. Star: feeds a reactor. Unknown Device: fits the upgrade | D32, P43, P44 |
| Crushing Wheels | Clockwork Scrap → iron, copper, brass nuggets, small chance of zinc | P15 |
| Mechanical Crafter | Star Reactor recipe | P13 |
| Redstone, comparators | Redstone pauses a reactor; comparator reads star left; Substation comparator reads wake charge | P14, P7 |
| Goggles | Substation: charge or fed/draw. Reactor: output, burn left, "Overhauled" | P7, P14, P44 |
| Ponder | "Waking a district", "The Star Reactor" (with the upgrade) | P27, P56 |
| Wrench | Full salvage (sneak-right-click) | D26 |

## Technical Notes

- **Biome:** TerraBlender `1.21.1-4.1.0.8`+, required (`mods.toml`; `build.gradle`: repo `https://maven.minecraftforge.net/`, `com.github.glitchfiend:TerraBlender-neoforge`). `EndBiomeRegistry.registerHighlandsBiome(key, 1)` and `registerMidlandsBiome(key, 1)`, from one shared list for all three End biomes; never the island slot (bugged on 1.21.1). Biome JSON: fog/sky/water colours, ambient particles (brass dust), ambient loop, spawns (Endermen at half rate). Not in `#minecraft:has_structure/end_city`.
- **Structure:** a custom structure type (like `juggernaut_ruin`): heart plus four district pieces, struts and bridges; structure set in the Wastes only, spacing 32, separation 12, `exclusion_zone` against `minecraft:end_cities` (6 chunks); island-fit check (half the footprint on End Stone) or outskirts as a placed feature. Fallback config (off): place in vanilla highlands when another End mod hides the biome; log one warning when TerraBlender's End source isn't active (P53).
- **City record:** a level `SavedData` keyed by city origin: each district awake, heart awake, Custodian dead. City blocks store their city origin and district id and read the record on load (P48).
- **Blocks:** Substation (BE: wake charge, upkeep buffer; `PowerGrid` consumer), City Vault (BE: locked, loot table), locked doors (slide open), lifts, Derelict lamps and cable (lit by district feed, no dye), conduit pylons (6-hit coil), Derelict Star Reactor (fixed), Star Reactor (BE: burn ticks, paused, upgraded; `PowerGrid` generator, on paper when unloaded), broken machine and bot-shell blocks (wrench, Deployer and pickaxe salvage). Locked blocks refuse breaking, blasts, contraptions and Carry On.
- **Entities:** Scuttler, Sentry (anchored, folds when unfed), Custodian (boss bar, shield linked to pylons, overload, reset, toughness per player). Persistent; never despawn; never naturally spawn. Fight Scuttlers flagged so they drop nothing.
- **Items:** Unknown Device, Clockwork Scrap.
- **Saving:** wake state in the city record; lock and burn state in BE NBT; the Star Reactor item keeps burn and upgrade in data components.
- **Config (`CMConfig`):** P26 list, plus city spacing/separation, island-fit share, pylon hits, Custodian numbers, highlands fallback.
- **Data:** loot tables (district vault, heart vault with one Elytra, Custodian, salvage), recipes (Star Reactor, crushing scrap), lang with subtitles, advancements (P27, P56), tags (Wastes biome, salvage blocks, Endermen can't hold city blocks).
- **Mod rules:** id without "create"; Carry On needs BlockItems; Flywheel for solid moving parts, BER for glows and arcs.

# ONE MINUTE UNDERSTANDING

Out in the End, about one big island in ten is the Clockwork Wastes: dead cities of unknown make, each built in four districts round a heart. Their streets are full of broken Create machines and slumped bots you can wrench apart for parts and Clockwork Scrap. Doors and vaults stay locked until you bring power: pour 1,000,000 FE into a district's Substation and the street lamps ripple on, the locks open, and its sleeping Sentries and Scuttlers wake to fight. Wake all four (or drop a Nether Star into the heart's reactor to wake them all at once) and the Custodian rises, shielded by four power pylons. Break the pylons, beat it, and the heart vault gives up the Elytra and the Unknown Devices to build your own Star Reactor: a Nether Star becomes 8 hours of 8,192 FE/t, anywhere.

# WHAT IT IS

An End biome, the first of three, holding dead clockwork cities. Power is the key: each district wakes when you feed its Substation, opening its loot and waking its guards, and the heart's Custodian guards the Elytra. Nether Stars become fuel for Star Reactors.

# WHY IT EXISTS

- To give the End a Create-flavoured place to explore after the dragon.
- To give the mod's electricity (Power Lines, Batteries) an adventure use.
- To give Nether Stars a second use: compact, fuel-free power.

# HOW PLAYERS USE IT

- Fly out, find the rust-brown haze, wrench apart wrecks for parts and scrap.
- Carry a charged Battery in and wake a district's Substation (1M FE).
- Fight the woken Sentries and Scuttlers; open the district vault.
- Wake all four; break the Custodian's pylons, beat it for the Elytra.
- Build Star Reactors from Unknown Devices; power bases and airships with stars.

# HOW IT FITS CREATE

- Wrench salvage, by hand or Deployer, like Create's dismantling.
- Crushing Wheels turn Clockwork Scrap into metal nuggets.
- Star Reactor built in a Mechanical Crafter; funnels, arms and packages feed it stars.
- Runs on the mod's Power Lines network; Star Reactors work on Aeronautics ships.
- Goggles and Ponder explain Substations and reactors.

# IMPORTANT RULES

- Never inside or within 6 chunks of an End city, never on the central island.
- Cities 32 chunks apart, heart plus four districts, about 144 blocks.
- A district wakes for good at 1,000,000 FE; its lamps need 500 FE/t after.
- Locked blocks are unbreakable, blast-proof and contraption-proof until woken.
- City lamps and cables break into scrap, never free lights or lines.
- Bots never respawn; a cleared district stays safe.
- Sentries fold while their district is unfed.
- Custodian: shielded while any pylon stands; its death opens the heart vault.
- One Elytra per city, shared.
- Star Reactor: 8,192 FE/t flat out, 8 hours a star (12 upgraded), keeps its burn when broken.

# EDGE CASES

- **Peaceful:** nothing wakes; locks open; the heart vault opens with four districts awake.
- **Two players power one Substation:** their FE adds up; it wakes for everyone.
- **District partly unloaded when it wakes:** the city record holds it; blocks catch up when loaded.
- **Power cut after waking:** lamps go dark, Sentries fold, locks stay open.
- **Substation broken after waking:** the district stays awake; its lamps go dark for good.
- **Everyone flees or dies mid-fight:** the Custodian resets, pylons re-light, vault stays locked.
- **Custodian removed by /kill:** the vault opens (any death does), but it drops nothing; it can't reach the void, as it never leaves the hall.
- **Reactor broken mid-burn:** drops with its remaining burn and its upgrade.
- **Island too small:** that spot gets outskirts only; no floating city.
- **Another End mod hides the biome:** a warning in the log; the config fallback puts cities in vanilla highlands.

# DEVELOPMENT CHECKLIST

- [ ] TerraBlender `1.21.1-4.1.0.8`+ dependency (build, `mods.toml`), shared End biome slot list (highlands + midlands, weight 1)
- [ ] Clockwork Wastes biome JSON (colours, particles, ambience, spawns), lang
- [ ] Biome kept out of `#minecraft:has_structure/end_city`
- [ ] City structure type: heart + 4 districts, struts/bridges, island-fit check, outskirts feature
- [ ] Structure set: Wastes only, spacing 32 / separation 12, 6-chunk End city exclusion, config; highlands fallback (off) and warning
- [ ] City record (`SavedData`): districts, heart, Custodian
- [ ] Substation block + BE (wake charge, upkeep), PowerGrid consumer, goggles, comparator
- [ ] City Vault, sliding locked doors, lifts (locked: unbreakable, blast- and contraption-proof, no Carry On)
- [ ] Derelict Street Lamp, Wall Lamp, Cable (lit by district feed, scrap drops)
- [ ] Broken machine and bot-shell blocks: wrench / Deployer full salvage, pickaxe / drill / saw scrap
- [ ] Conduit pylons (6-hit coil, arcs, re-light)
- [ ] Derelict Star Reactor (heart) and Star Reactor (crafted): BE, burn, upgrade, redstone, comparator, goggles, on paper when unloaded, keeps burn and upgrade as item, Aeronautics
- [ ] Decor set blocks (D37): drop themselves; Resistance Shell variants on the shell salvage (D36)
- [ ] Items: Unknown Device, Clockwork Scrap
- [ ] Entities: Scuttler, Sentry (folds when unfed), Custodian (shield, moves, overload, reset, toughness, boss bar); persistent, no natural spawns, peaceful rule
- [ ] Loot tables: district vault, heart vault (1 Elytra, 2 devices), Custodian (player kill only), salvage, Sentry/Scuttler drops
- [ ] Recipes: Star Reactor (Mechanical Crafter), Crushing Wheel scrap
- [ ] Config (P26 and extras), lang + subtitles, tags (incl. Endermen)
- [ ] Advancements: Lights On, City of Lights, Star Power, Short Circuit, Custodian Down
- [ ] Ponder: Waking a district, The Star Reactor (with upgrade); JEI info pages
- [ ] Sounds on the staging ladder (P28, plus pylons, shield, overload)
- [ ] Game tests (P31, P55), including Elytra parity and island-fit sampling
- [ ] Website: Biomes section, Clockwork Wastes page
- [ ] Looks: briefs for the city, derelict blocks, pylons, bots, Custodian, Star Reactor (art director, Blockbench first)

# "IDIOT TEST"

- Dead clockwork cities on one End island in ten.
- Never near End cities.
- Wrench wrecks for parts; pickaxe gives only scrap.
- 1M FE into a Substation wakes its district.
- Waking opens locks and wakes the bots.
- Cut its power: lamps and Sentries go dark.
- Four districts awake wakes the Custodian.
- Break its four pylons, then kill it.
- Its death opens the vault: Elytra inside.
- Nether Star in a Star Reactor: 8h of power.

# COMPLETENESS SCORE

Gameplay: 100% · World Integration: 95% · Balance: 90% · Progression: 100% · Automation: 100% · Edge Cases: 95% · **Overall Design Readiness: 90%**

- Gameplay 100%: every system has its rules, including the Custodian's fight.
- World Integration 95%: API confirmed; island fit and Elytra parity have rules and tests, but their results only exist once the city generates.
- Balance 90%: numbers set with the Juggernaut lesson, but not yet fought; wake charge and upkeep unplayed.
- Progression 100%: every reward has a use in v1, including the 7th device.
- Automation 100%: every Create system has a stated interaction.
- Edge Cases 95%: rules written for all found cases; multi-chunk waking proven only by its test.
- The last points come from the first playable build: run the parity and island-fit tests, fight the Custodian, tune.
