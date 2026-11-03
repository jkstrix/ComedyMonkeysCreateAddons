# Clockwork Jetpack: Systems Spec

Status: ready for development  ·  Related: [Clockwork Juggernaut](clockwork-juggernaut.md) (the Mainspring), brief: `design/briefs/clockwork-jetpack.md`

## Concept
User, 2026-11-03: "a clockwork jetpack ... a similar pattern to the create jetpack mod but instead of a back tank it uses the mainspring and instead of the elytra it uses a precision mechanism. It should have the same options and defaults, we also need to make a new curio slot for it so we can wear back packs and clockwork jetpacks at the same time." Then: "It should default to fall 0 for hover descend too" and "I would also like a flight mode, so it's like an elytra without rockets and a little bit faster, wings should extend when this is active".

Clarified: a spring-driven jetpack worn in its own Curios slot. It flies exactly like the Create Jetpack (same controls, same tunable numbers), runs on the wind in a Mainspring instead of compressed air, hovers dead still by default, and has a flight mode that unfolds wings and glides like an elytra, a little faster.

## Reference: Create Jetpack 5.2.1 (NeoForge 1.21.1), read from the jar 2026-11-03
- **Item:** a Create `BacktankItem` worn in the **chest armour slot**. It holds its own air (it *is* a backtank: feeds the Diving Helmet, can be placed as a block and filled by a shaft). Netherite version via smithing.
- **Recipe** (Mechanical Crafter, mirrored allowed, keeps the backtank's components):
  ```
   P S P        P = brass sheet (c:plates/brass)   S = shaft
  P Y X Y P     Y = precision mechanism            X = copper backtank
  P C E C P     C = chute                          E = elytra
    C   C
  ```
- **Controls** (flightlib): Jump = up, Sneak = down, **G** = engine on/off (**on** by default), **H** = hover on/off (**off** by default), plus an "activate elytra" key. Settings are per player.
- **Server config** (synced to clients):

  | Key | Default | Range |
  |---|---|---|
  | `air.seconds_per_tank` | 450 | 1+ |
  | `air.seconds_per_tank_hover` | 900 | 1+ |
  | `speed.horizontal` | 0.02 | 0.01–100 |
  | `speed.vertical` | 0.4 | 0.01–100 |
  | `speed.acceleration` | 0.6 | 0.01–100 |
  | `speed.hover_descend` | -0.03 | -100–0 |
  | `speed.swim_modifier` | 1.8 | 0–100 |
  | `features.elytra_boost` | 1.25 | 1–100 |
  | `enchantments.list` | empty | ids |
  | `enchantments.is_blacklist` | true | |
  | `heightAboveGroundLimit` | -1 (none) | |
- **Client config:** `overlay.enabled` true, `overlay.position.x` 6, `overlay.position.y` 6 (negative = from right/bottom), `overlay.scale` 1.0, `effects.seasonal` true, `controls.double_tap_wings` true.
- **Licences:** Create Jetpack code MIT, its assets All Rights Reserved; flightlib (the flight library it bundles) All Rights Reserved. We can copy numbers and behaviour, not its art, and can't ship flightlib.

## Decisions
| # | Area | Decision | Source |
|---|---|---|---|
| D1 | What it is | A Clockwork Jetpack following the Create Jetpack's pattern | user, 2026-11-03 |
| D2 | Power | A Mainspring takes the place of the backtank: the jetpack runs on wind, not air | user, 2026-11-03 |
| D3 | Recipe | Create Jetpack's Mechanical Crafter pattern with the Mainspring for the Copper Backtank and a Precision Mechanism for the Elytra: 6 Brass Sheets, 1 Shaft, 3 Precision Mechanisms, 1 Mainspring, 4 Chutes | user, 2026-11-03; pattern from Create Jetpack |
| D4 | Options and defaults | Every Create Jetpack option above, with its default (air renamed to wind where it names air) | user, 2026-11-03 |
| D5 | Hover descend | `speed.hover_descend` defaults to **0**: hovering holds height exactly (Create Jetpack: -0.03) | user, 2026-11-03 |
| D6 | Flight mode | A second mode: flies like an elytra without needing rockets, a little faster; wings extend while it's active | user, 2026-11-03 |
| D7 | Slot | A new Curios slot of its own, so a backpack (Curios `back` slot, e.g. Sophisticated Backpacks) and the jetpack are worn together | user, 2026-11-03 |
| D8 | Controls | Create Jetpack's: Jump up, Sneak down, G engine (on by default), H hover (off by default) | D4 |
| D9 | Tier | End game: the Mainspring comes only from a Clockwork Juggernaut, one per kill, and his Winding Key costs Netherite | existing rule, Juggernaut spec D17, D30 |
| D10 | No farm | The Mainspring stays one per kill; nothing here adds another source | existing rule, Juggernaut spec D15, D30 |
| D11 | Art | Our own model and textures; nothing from Create Jetpack's (All Rights Reserved) assets | licence |
| D12 | Winding | **Only at a Jetpack Winding Station** (D38), which replaces placing the jetpack itself as a block (user, 2026-11-03). The station winds it at the rate a Copper Backtank fills at that RPM (Hand Crank and Spring Engine both work). The jetpack is a worn item only, with no block form, and keeps its wind as an item. A full wind gives 450 s of thrust or 900 s of hover (D4) | user, round 1; revised 2026-11-03 |
| D13 | Flight mode feel | **Revised by D16 (2026-11-03):** it glides exactly like an elytra, free; holding Jump boosts, a little faster than an elytra, on wind. (Was: powered level flight draining wind the whole time) | user, round 1; revised 2026-11-03 |
| D14 | Entering flight mode | A new rebindable **Wings** key (default J) toggles it in the air: the wings unfold and glide controls take over. Landing folds them and returns to jetpack mode | user, round 1 |
| D15 | Flight code | Our own thrust, hover and flight logic with Create Jetpack's numbers and controls; no flightlib, no Create Jetpack dependency | user, round 1 |
| D16 | Flight rules | **Works like an elytra.** Without Jump, the wings glide exactly as an elytra does, and gliding uses no wind. **Holding Jump boosts:** the fans push along your look up to an elytra's shallow-glide speed (1.4 × 1.25: about 14 blocks/s; options `flight.speed_modifier` and `elytra_boost`; raised from 1.15 after testing, 2026-11-03); a level gaze holds height and looking up climbs while boosting. The boost uses wind at the hover rate: 900 s of boost a wind (`wind.seconds_per_wind_flight`). Fireworks still work on top | user, round 2 ("Cheaper flight"); revised 2026-11-03 ("pressing or hold space should boost your speed, while gliding feels exactly the same") |
| D17 | Out of wind | The wings stay out and it becomes a plain unpowered elytra glide, so you can land. Low wind: one warning sound as it crosses 10% (D32), the gauge turns amber, and below 10% the key on your back ticks at half speed (no sound), so others can see too | user, round 2; revised 2026-11-03 |
| D18 | Without Curios | Worn in the chest armour slot, like the Create Jetpack. With Curios it fits its own slot as well | user, round 2 |
| D19 | Netherite | A Netherite Clockwork Jetpack by smithing (Netherite Upgrade Template + Clockwork Jetpack + Netherite Ingot), keeping its wind and enchantments. Fire-proof as an item and keeps working in lava, like the Netherite Jetpack. **In lava the plain one cuts out:** no thrust, hover or boost, and its wings fold | user, round 2; lava rule 2026-11-03 |
| D20 | Armour | Gives armour wherever it's worn, Curios slot included: the Clockwork Jetpack has the Copper Backtank's chest armour (Create's copper material), the Netherite one the Netherite Backtank's (8 armour, 3 toughness, 0.1 knockback resistance). It holds no air: it never feeds a Diving Helmet | user, round 3 ("Armour, no air") |
| D21 | Slot | One Curios slot named **Jetpack**, with its own icon. It takes items in the tag `curios:jetpack`: the two Clockwork Jetpacks only (Create's jetpacks dropped with D23's revision) | user, round 3; revised 2026-11-03 |
| D22 | Create Jetpacks in the slot | **Withdrawn** (2026-11-03): the Jetpack slot no longer takes them (D23) | user, 2026-11-03 |
| D23 | Two flyers | **A backtank or Create Jetpack is never worn with a Clockwork Jetpack** ("You shouldn't be able to equip a jetpack or a backtank with this mod"): with one in the chest the Jetpack slot won't take a Clockwork Jetpack, and one put in the chest while a Clockwork Jetpack is worn comes straight off into the inventory. An Elytra in the chest glides as normal and gives way only while the Clockwork Jetpack is thrusting or hovering (user, 2026-11-03). A Clockwork Jetpack in the slot beats one in the chest. (So no stand-off rails: a backpack gets pack mode, brief) | user, round 3; revised 2026-11-03 |
| D24 | HUD | A wind gauge with Create Jetpack's client options: `overlay.enabled` true, `overlay.position.x` 6, `overlay.position.y` 6 (negative counts from the right or bottom), `overlay.scale` 1.0, `effects.seasonal` true (seasonal particle variants; the art director picks them) | proposed defaults, round 3 |
| D25 | Enchanting | Max wind is raised by **Overwound** (D34), not Create's Capacity: an anvil refuses Capacity on Clockwork Jetpacks. `enchantments.list` (empty) and `enchantments.is_blacklist` (true) work as in Create Jetpack. No durability, so no Unbreaking or Mending | proposed defaults, round 3; Capacity replaced by Overwound, user 2026-11-03 |
| D26 | Station readouts | On the Winding Station: Engineer's Goggles show the docked jetpack's wind % and time to full; a comparator reads its wind (0–15); it loads the shaft with a Copper Backtank's stress (was the placed jetpack's, before D38) | proposed defaults, round 3; moved to the station 2026-11-03 |
| D27 | Creative | Never uses wind | proposed defaults, round 3 |
| D28 | Water | Flight mode can't open underwater; under water the wings fold and jetpack mode runs with `swim_modifier` 1.8 | proposed defaults, round 3 |
| D29 | Riding | Mounting a seat, boat, minecart, train or anything else stops flight and folds the wings | proposed defaults, round 3 |
| D30 | Weather and dimensions | Rain, storms, the Nether and the End change nothing | proposed defaults, round 3 |
| D31 | Death | Follows Curios and keepInventory rules like any curio; the item keeps its wind | proposed defaults, round 3 |
| D32 | Sounds | **No running sounds** on the jetpack: it's strapped to your back, so it plays only one-shots, for the engine starting and stopping, the wings unfolding and folding, the run-down, boost, the hover clicks, a single low-wind warning at 10%, and equipping. Flight's whoosh is vanilla's elytra sound. The Winding Station plays its winding loop only while winding, with no idle sound. Details in design/sound/clockwork-jetpack.md | proposed defaults, round 3; user 2026-11-03 ("I don't want there to be any running sounds") |
| D33 | Teaching | A Ponder scene for winding, JEI info pages for the jetpacks and the station, game tests | proposed defaults, round 3 |
| D34 | Overwound | A new enchantment, **Overwound** I–III, that raises max wind: ×(1 + level ⁄ 3), so ×1.33, ×1.67 and **×2 at III**. The same steps as Create's Capacity (900 → 1,800 air at III) | user, 2026-11-03 ("an enchantment called overwound that increases flight time", "overwound 3 should double it") |
| D35 | Overwound scope | Every mode lasts longer, because it's more wind: at III, 900 s of thrust, 1,800 s of hover and 1,800 s of flight. Winding speed doesn't change, so a full wind takes proportionally longer | user, Overwound round |
| D36 | Overwound items | Clockwork Jetpack, Netherite Clockwork Jetpack and the **Spring Engine** (Juggernaut spec D63). Not Create Jetpacks: they hold air and keep Capacity | user, Overwound round |
| D37 | Getting Overwound | The mod's pattern (Fruitful, Monstrous): books from **Librarians** (their random book trades plus an extra Apprentice trade), never the enchanting table; applied at an anvil. Weight 2, anvil cost 4, max level III. Kept through smithing, hanging on the station and breaking | user, Overwound round |
| D38 | Winding Station | A **Jetpack Winding Station**, like the Trade Bot's station but running on SU: a cradle and a winder arm that swings in and turns the jetpack's key. The only way to wind (D12) | user, 2026-11-03 ("it should have a winding station like the trade bot (except running on SU)") |
| D39 | Docking | Two ways. **Hang it:** right-click the station with the jetpack to hang it on the cradle. Right-click it without a jetpack in hand while wearing one and it comes off your back onto the station; right-click a station with one hung on it and it goes into your Jetpack slot (the chest without Curios) if that's free, or your inventory if not (user, 2026-11-03). **Back in:** wearing it, stand on the block in front of the station with your back to it (facing within 45° of the station's facing) and sneak; the winder swings in and winds it while you stay; step away or stop sneaking and it swings back | user, station round |
| D40 | Station power | A **shaft from below**, like Create's backtank. The wind rate scales with RPM at the Copper Backtank's fill rate and stress (D12, D26); stopped or no shaft, nothing winds | user, station round |
| D41 | Station recipe | Brass Casing, a Cogwheel, a Shaft and 2 Brass Sheets at a crafting table. Mid game (anyone with a jetpack already has brass) | station defaults |
| D42 | What it holds | One Clockwork Jetpack, plain or Netherite. Not Create Jetpacks (they fill with air) | station defaults |
| D43 | Automation | Funnels and Mechanical Arms can put a jetpack in and take out one that's fully wound | station defaults |
| D44 | Station block | Faces the player when placed, any horizontal direction. No light (the clockwork rule): the jetpack's porthole coil is the gauge. Breaking it drops the hung jetpack. Has its own BlockItem (Carry On) | station defaults |
| D45 | Teaching | The station's Ponder scene (hanging, shaft speed, backing in, the empty-hand swap) replaces the old winding scene (D33) | station defaults |
| D46 | Double tap | Double-tapping Jump **toggles flight mode**, as J does: from the ground (the first tap jumps), mid-jump or while hovering it spreads the wings; in flight it folds them. A tap is Jump held no longer than 0.5 s, so holding it to thrust or boost never counts; the second press comes within 0.4 s of letting go of the first. Not where a double tap already means flight (Creative). Client option `controls.double_tap_wings`, on by default | user, 2026-11-03 ("double tap should start flight and stop flight... it should work when you are already floating too") |
| D47 | Engine off in flight mode | With the engine off the wings still open and glide (free); holding Jump doesn't boost | user, 2026-11-03 (review) |
| D48 | Height limit | `heightAboveGroundLimit` holds on the server too: climbing past it, the server stops excusing the flight (fall distance and the fly-kick timer). Otherwise movement is the client's, as in Create Jetpack | user, 2026-11-03 (review) |
| D49 | Armour stands and mobs | A Clockwork Jetpack in an armour stand's or a humanoid mob's chest slot is drawn on its back | user, 2026-11-03 (review) |

## Open Questions
None. Every gap was answered by the user or taken as an approved proposed default (rounds 1–4, the Overwound round and the Winding Station round, 2026-11-03).

Handed on, not gameplay: the look (wings, thrusters, where the Mainspring shows, what the thrust looks like instead of air puffs, the Netherite version, the slot icon) goes to the voxel-art-director brief at `design/briefs/clockwork-jetpack.md`.

## Core Loop
1. Beat a Clockwork Juggernaut for his Mainspring; build the Clockwork Jetpack in Mechanical Crafters.
2. Build a Jetpack Winding Station on a shaft; hang the jetpack on it (or back into it while wearing it) until it's wound.
3. Wear it in the Jetpack slot, with a chestplate and a backpack still on.
4. Hop about in jetpack mode (Jump up, Sneak down, hover with H); press J in the air to unfold the wings and fly level to somewhere far.
5. When the gauge runs low, glide home and back into the station, or hang it there while you do something else.

## Behaviour / Rules
**Modes**
1. **Off** (engine off, G): no thrust or hover, and the wings glide without boosting (D47); a chest Elytra glides as normal (D23).
2. **Jetpack:** Create Jetpack's movement and numbers exactly (D4). Jump thrusts up at `speed.vertical`, Sneak goes down, horizontal control at `speed.horizontal` with `speed.acceleration`. Uses wind at 450 s per wind.
3. **Hover** (H): holds the player in the air; `speed.hover_descend` 0, so they hold height dead still (D5). Uses wind at 900 s per wind.
4. **Flight** (J, or double-tap Jump with the second tap in the air; in the air only): the wings unfold and you glide exactly as with an elytra, using no wind. Hold Jump to boost along your look (about 14 blocks/s, holding height when level), at 900 s of boost a wind. Fireworks still work (D16).
5. Flight mode ends, and the wings fold, when you touch the ground, press J again, enter water (D28) or mount anything (D29).
6. **Out of wind:** in flight mode the wings stay out and it glides like a plain elytra (D17); in jetpack or hover mode the player falls. At 10% left a single warning sounds and the key ticks slower from then on (D17).

**Wind**
7. A full wind is the jetpack's whole capacity. Overwound I–III raises it ×1.33 / ×1.67 / ×2 (D34), and every mode's time scales with it (D35). Capacity doesn't go on it (D25).
8. Winding happens only at a Winding Station on a turning shaft, hung on its cradle or backed into while worn, at a Copper Backtank's fill rate for that RPM and with its stress (D12, D26, D38–D40). The jetpack keeps its wind as an item, in the slot, on the station, and through death and smithing.

**Wearing**
9. With Curios: the Jetpack slot (D21), or the chest slot. Without Curios: the chest slot (D18). Armour applies in either (D20).
10. Keys: G engine (on by default), H hover (off by default), J wings. All are rebindable, and they're stored per player like Create Jetpack's toggles. Double-tapping Jump also toggles the wings, from the ground, mid-jump or hovering (D46).

## Progression
**End game.** You need a Juggernaut kill (a Winding Key with Netherite, then the fight), Mechanical Crafters, brass and three Precision Mechanisms (the Juggernaut drops 1–2). It unlocks jetpack flight without giving up the chestplate or the backpack, and elytra-style travel that needs no fireworks. The Netherite smithing upgrade makes it lava-safe for the Nether.

It sits beside Create Jetpack rather than above it. Create Jetpack is mid-to-late game (backtank and elytra) and takes the chest. Ours costs a boss and frees the chest. The two are never worn together (D23).

## Balance
- **Cost:** a boss kill (one Mainspring each, no farm: Juggernaut spec D15, D30), 3 Precision Mechanisms, 6 Brass Sheets, 4 Chutes, a Shaft. Netherite: one ingot and a template.
- **Reward:** 450 s of thrust, 900 s of hover, or 900 s of boosting (about 14 b/s, ≈ 12 km) per wind, and gliding is free; a Hand Crank or a Spring Engine refills it. Overwound III doubles every figure (≈ 25 km of boost) for the price of Librarian trades and a longer wind.
- **Overwound on the Spring Engine (D36):** doubles its store to two hours at 16,384 SU. It's still lossless and gives back only what went in, so it's more buffer, not more power. Winding a doubled engine takes twice as long at the same stress.
- **Risk:** running dry mid-air in jetpack or hover mode means a fall. Flight mode always glides down safely.
- **Armour stacking (accepted):** a chestplate plus jetpack armour in the slot is more than vanilla allows (D20). The cost and the single slot limit it. Watch it in play.
- **Elytra and rockets still matter:** rockets are faster than our ~14 b/s boost, and an elytra costs no boss.
- **Exploits and fixes:**
  - Dupe by breaking or moving the Winding Station mid-wind: the hung jetpack (with its wind on the item) lives in the station's block entity and drops once. Contraptions, Carry On and pistons must move the block entity whole (Create removes the block entity first; checked in review). Breaking is covered by a game test.
  - Backing in to wind someone else's worn jetpack: winding only ever applies to the jetpack worn by the sneaking player on the front block.
  - Smithing or the crafter clearing the wind: smithing copies components (D19). The crafter uses a fresh Mainspring with no wind.
  - Two Clockwork Jetpacks (slot and chest) to double the time: only one drives (D23); swapping by hand is just carrying spares, which is fine.
  - Free wind from a Creative Motor: that's creative play anyway.
  - Hover AFK towers: hover holds height but drains wind (900 s, 1,800 s at Overwound III); no infinite hover.
  - Overwound duplicating wind: wind is stored as an amount, not a fraction, so moving a wound jetpack's wind can't grow it. Taking Overwound off (grindstone) clamps the wind to the new max.
- **Useless check:** without the slot it would just be a pricier Create Jetpack. The slot, the free chest, powered flight and armour are what make it worth a boss.

## Create Integration
| System | Interaction | Notes |
|---|---|---|
| Shafts and gears | The Winding Station winds it from a shaft below (D40) | Backtank fill rate and stress at that RPM |
| Spring Engine | Its output shaft drives a Winding Station | Nice pairing: store rotation, then spend it flying |
| Hand Crank | Drives a Winding Station | The early-access refill |
| Belts, depots, funnels, chutes | No | Wound in place like a backtank; nothing to process |
| Contraptions | A station on a contraption keeps its hung jetpack and wind; it winds only where kinetics run (Aeronautics ships) | |
| Trains, elevators, seats | Mounting stops flight (D29) | Prevents flying while seated |
| Aeronautics ships | Fly on and off decks; riding a ship's seat stops flight | No physics interaction |
| Packages and logistics | The item packages like any item | Wind kept |
| Deployers and arms | No | It's worn gear; deployers can't wear it |
| Redstone, comparators | The comparator reads the docked jetpack's wind (D26) | Lets a station signal "fully wound" |
| Engineer's Goggles | Wind % and time to full on the station (D26) | |
| Funnels, Mechanical Arms | Put a jetpack on the station; take a full one off (D43) | |
| Diving Helmet | No air (D20) | A backtank can't be worn with it (D23): take the jetpack off to dive |
| Ponder | Winding Station scene (D45) | |

## Technical Notes
- **Items:** `clockwork_jetpack`, `netherite_clockwork_jetpack` (fire-proof). No block form (D12). New block: `jetpack_winding_station` with a block entity and a BlockItem (Carry On). No "create" in ids.
- **Data component:** wind (int), shared by item and block entity; max wind = base × (1 + Overwound level ⁄ 3).
- **Overwound:** `data/comedymonkeysaddons/enchantment/overwound.json` in the shape of `monstrous.json` (weight 2, max level 3, anvil cost 4); `supported_items` tag `#comedymonkeysaddons:enchantable/overwound` = both Clockwork Jetpacks + the Spring Engine item; added to `minecraft:tags/enchantment/tradeable` with an extra Librarian Apprentice trade like Monstrous; kept out of the enchanting table. Anvil refuses `create:capacity` on Clockwork Jetpacks (Capacity's `supported_items` is `#create:pressurized_air_sources`, so don't add ours to that tag). The Spring Engine block entity keeps the item's enchantments when placed and puts them back on the drop.
- **Curios (optional):** add the `jetpack` slot to `curios/entities/players.json` beside `head`; a slot definition with icon; tag `curios:jetpack` holding our two items only (D23). Their armour in the slot comes from their curio (D20). Everything Curios is behind the existing compat guard (`AviatorGogglesCurios` pattern).
- **Chest fallback:** an armour-type chest item; flight logic reads the slot first, then the chest.
- **Flight logic (own code, D15):** client input → per-player toggle state (engine, hover, wings) synced to the server; server-authoritative wind use from the keys the client reports; movement applied on the client for the local player, as Create Jetpack does (the fly-kick is excused while it runs, except climbing past the height limit, D48).
- **Flight mode:** uses the vanilla fall-flying pose and its physics, plus, while Jump is held, the boost's push along the look to its speed cap (a level gaze holds height). Other players see the wings through a synced flag.
- **Config, server (synced, `CMConfig` section `clockworkJetpack`):** `wind.seconds_per_wind` 450, `wind.seconds_per_wind_hover` 900, `wind.seconds_per_wind_flight` 900, `speed.horizontal` 0.02, `speed.vertical` 0.4, `speed.acceleration` 0.6, `speed.hover_descend` **0.0** (−100–0), `speed.swim_modifier` 1.8, `features.elytra_boost` 1.25, `flight.speed_modifier` 1.4, `enchantments.list` [], `enchantments.is_blacklist` true, `heightAboveGroundLimit` −1. **Client:** `overlay.enabled` true, `overlay.position.x` 6, `overlay.position.y` 6, `overlay.scale` 1.0, `effects.seasonal` true.
- **Keys:** `key.comedymonkeysaddons.jetpack_engine` G, `…jetpack_hover` H, `…jetpack_wings` J, in their own key category.
- **Saving:** toggles go in a player attachment that persists across death; wind lives on the item and the block entity.
- **Licences:** our own art; Create Jetpack's MIT code may guide the behaviour but isn't copied wholesale; flightlib is not used.

# ONE MINUTE UNDERSTANDING
A spring-wound jetpack for the end game. Craft it from the Juggernaut's Mainspring, wind it at a Winding Station on a shaft and wear it in a new Jetpack slot, so your chestplate and backpack stay on. It flies exactly like Create's Jetpack (Jump up, Sneak down, G engine, H hover, the same numbers), except that hover holds you dead still. Press J in the air (or double-tap Jump) and brass wings unfold: they glide exactly like an elytra for free, and holding Jump boosts you, a little faster than an elytra, for 15 minutes of boost a wind. When it runs dry with the wings out, you glide down safely.

# WHAT IT IS
A Mainspring-powered jetpack, worn in its own Curios slot, with a jetpack mode and a winged flight mode. It copies the Create Jetpack's controls, options and defaults, except that hover holds height.

# WHY IT EXISTS
- To give the Juggernaut's Mainspring a second use worth a boss fight.
- To let players fly without giving up their chestplate or backpack.
- To offer fuel-free long-distance flight, powered by Create rotation.

# HOW PLAYERS USE IT
- Kill a Juggernaut, then build the jetpack in Mechanical Crafters.
- Hang it on a Winding Station on a shaft, or back into the station wearing it.
- Wear it in the Jetpack slot: Jump and Sneak to fly, H to hover, G for the engine.
- Press J in the air for wings and level flight; land to fold them.
- Upgrade it to Netherite for lava.

# HOW IT FITS CREATE
- Its Winding Station runs on any shaft at a backtank's rate and stress.
- A Spring Engine stores rotation that becomes flight time.
- Mechanical Crafter recipe in Create Jetpack's pattern.
- Goggles and comparators read the docked jetpack's wind; funnels and arms swap jetpacks.
- A backtank or Create Jetpack is never worn with it.

# IMPORTANT RULES
- Same options and defaults as Create Jetpack, but hover descend is 0.
- Overwound I–III raises max wind; III doubles it (jetpack and Spring Engine).
- 450 s of thrust, 900 s of hover, 900 s of boost a wind; gliding is free.
- Flight glides exactly like an elytra, free; holding Jump boosts (about 14 b/s) on wind.
- Out of wind with the wings out: an elytra glide, never a drop.
- Only one flyer works at a time; the Clockwork Jetpack goes first.
- It gives backtank armour in the slot or the chest (chest only without Curios); it holds no air.
- Flight mode ends on landing, in water or when you mount anything.
- Creative never uses wind.
- One Mainspring per boss kill; no other source.

# EDGE CASES
- Wind runs out while hovering high: you fall, after a slowing-tick warning.
- Backtank or Create Jetpack: can't be worn with it; one put in the chest pops back into the inventory.
- Dive into water in flight mode: the wings fold and swim thrust takes over.
- Sit on a train seat mid-flight: flight stops and the wings fold.
- Station broken with a jetpack hung on it: the jetpack drops with its wind.
- Death: Curios/keepInventory rules; the wind stays on the item.
- Netherite upgrade: keeps wind and enchantments.
- Chest Elytra with it in the slot: the Elytra glides, and gives way only while the jetpack thrusts or hovers.
- No Curios: chest slot only, same behaviour.
- Lava: the Netherite one keeps flying; the plain one cuts out (no thrust, hover or boost; its wings fold).

# DEVELOPMENT CHECKLIST
- [ ] Items: Clockwork Jetpack, Netherite Clockwork Jetpack (fire-proof), wind data component
- [ ] Jetpack Winding Station: block + block entity + renderer (cradle, winder arm, the hung jetpack's key turning); shaft from below at the backtank rate and stress; hang/take by right-click; back-in winding while worn (sneak, facing away, on the front block); comparator; goggle tooltip; funnel/arm insert and extract-when-full; drops the hung jetpack; recipe; BlockItem for Carry On
- [ ] Curios: `jetpack` slot (players.json, slot def, icon), `curios:jetpack` tag (ours only), armour in the slot
- [ ] Chest-slot fallback (armour item); slot-first lookup; "two flyers" priority
- [ ] Flight logic: jetpack, hover (descend 0), swim modifier, height limit, acceleration
- [ ] Flight mode: J toggle, fall-flying pose, level hold, look steering, speed modifier, Jump boost, fireworks, auto-fold on ground, water and riding, out-of-wind glide
- [ ] Wind use per mode (450/900/900), creative exemption, 10% warning
- [ ] Overwound enchantment I–III (×1 + level/3): data file, enchantable tag (jetpacks + Spring Engine), tradeable + Librarian Apprentice trade, no enchanting table, anvil refuses Capacity on jetpacks; Spring Engine keeps it placed and broken; lang + description; game tests for ×2 at III on both
- [ ] Per-player toggle attachment (G on, H off), keybinds G/H/J, client→server sync, anti-kick
- [ ] Config: server (synced) and client keys as listed
- [ ] HUD wind gauge with overlay options; seasonal effects
- [ ] Renderer: worn model in the slot and the chest; wings unfold and fold for all viewers; thrust particles
- [ ] Recipes: Mechanical Crafter (a fresh Mainspring, no components copied), Netherite smithing
- [ ] Lang + tooltips (controls like Create Jetpack's), subtitles
- [ ] Sounds: one-shots only (engine start/stop, wings, run-down, boost, hover clicks, single low-wind warning, equip); vanilla elytra wind in flight; station winding loop only while winding
- [ ] Ponder Winding Station scene; JEI
- [ ] Advancement (optional, e.g. "Wound Up and Away")
- [ ] Game tests: winding rate, wind kept through break/place/smithing, drain per mode, out-of-wind glide, two-flyer priority, no-Curios fallback; unit tests for config defaults (hover_descend 0)

# "IDIOT TEST"
- Jetpack runs on a Mainspring, not air.
- Wear it in the new Jetpack slot.
- Chestplate and backpack stay on.
- Wind it at its station: hang it or back in.
- Jump up, Sneak down, G engine, H hover.
- Hover holds you completely still.
- J in the air: wings out, elytra glide; hold Jump to boost.
- Flight is a bit faster than an elytra.
- No wind with wings out: glide down safely.
- Costs a Juggernaut's Mainspring; Netherite upgrade survives lava.

# COMPLETENESS SCORE
Gameplay: 95% · World Integration: 90% · Balance: 85% · Progression: 95% · Automation: 90% · Edge Cases: 90% · **Overall Design Readiness: 85%**

- Gameplay 95%: the exact flight-mode climb rate and dive speed cap get tuned in play.
- World Integration 90%: the slot icon and seasonal effects wait on the art brief.
- Balance 85%: armour stacking in the slot was accepted but is untested; the boost was raised to ~14 b/s after the first feel check.
- Progression 95%: fixed by the Juggernaut; no advancement named yet.
- Automation 90%: the backtank fill rate is matched rather than given in numbers; read it from Create at implementation.
- Edge Cases 90%: Create Jetpack can't be worn with it (D23); its keys sharing G/H with ours is untested with both mods installed.
