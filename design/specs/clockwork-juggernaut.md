# Clockwork Juggernaut: Systems Spec

Status: ready for development (look to be designed)  ·  Related: brief to come (`design/briefs/clockwork-juggernaut.md`), [Trade Bot](trade-bot.md), [Helper Bot](helper-bot.md), [Monster Cage](monster-cage-and-fish-tank.md), quick read `clockwork-juggernaut.page.html`

## Concept

The user (2026-10-27): "hostile, big, hard to kill but non destructive"; on the pitch: "I like the idea of the clockwork juggernaut, but the drop would have to power something or have some use worth the effort. I would have it so you could summon him with some clockwork ritual."

Clarified: a summoned boss, a huge war automaton and the bots' grim ancestor. It's armoured while its spring is wound and only truly hurt while it stops to rewind with its key exposed, so the fight is about reading its rhythm. It never breaks blocks. It drops a Mainspring, the heart of the Spring Engine: the only block that stores rotation.

## Decisions

| # | Area | Decision | Source |
|---|---|---|---|
| D1 | Kind | Hostile, big, hard to kill | user |
| D2 | Blocks | Never breaks or changes blocks: its attacks are knockback and damage only | user ("non destructive") |
| D3 | Fight rhythm | Wound: heavily armoured, charges, swings and stomps. Running down: slows and sags. Rewinding: stands still, winding key on its back exposed, takes full damage. Then wound again | pitch, user liked it |
| D4 | Family | Looks like the bots' ancestor: Trade Bot and Helper Bot family, built for war, long abandoned | pitch |
| D5 | Spawning | Summoned with a clockwork ritual, the way the Wither is built | user |
| D6 | Drop | The Mainspring, which must power something or have a use worth the effort | user |
| D7 | Farms | Vanilla-style balance: limit the pace by cost, never by inflating loot | mod rule (farms against vanilla) |
| D8 | Look | Dark, muted, Create-like; previewed in Blockbench first | user's standing art rules |
| D9 | Mainspring use | The Spring Engine: a block built round a Mainspring that stores rotation. Wind it from any shaft while there's spare, and it unwinds later as big rotation. Nothing in Create or the mod stores rotation | user, round 1 |
| D10 | Ritual | Build and wind: build an effigy, fit a crafted Winding Key in its back, drive the key with a shaft. It ticks louder as it winds, and wakes when fully wound | user, round 1 |
| D11 | Tier | Late game, beside the Wither: summoning needs brass, Precision Mechanisms and something hard-won, but not a Nether Star | user, round 1 |
| D12 | Wild ones | Summoned, and also found dormant in rare ruins; a dormant one wakes when a player comes close | user, round 1 |
| D13 | Spring size | A full Spring Engine holds an hour at 16,384 SU (58,982,400 SU-seconds) | user, round 2 |
| D14 | Toughness | 400 health. Wound, it takes 10% of damage; rewinding (about 6 s in every 25) it takes full damage. About 4-6 rewinds, 2-3 minutes, for a geared player. +50% health for each extra player within 32 blocks when it wakes | user, round 2 |
| D15 | Jars and cages | The Capture Bottle refuses it: no Juggernaut in a cage, no Mainspring farm | user, round 2 |
| D16 | Effigy | Four Brass Blocks in a T (like the Wither's soul sand). The Winding Key is a block placed on the back of the T's centre | defaults round 3 |
| D17 | Winding Key | Made in a Mechanical Crafter from a Netherite Ingot, 4 Precision Mechanisms and 4 Brass Sheets. Single use: the ritual consumes it with the effigy | defaults round 3 |
| D18 | Winding | A shaft into the key at 64 RPM or more for 30 s of continuous turning. If the shaft stops, the key slowly unwinds back. The ticking grows louder; players within 64 blocks see "Something is being wound…" on the action bar. Fully wound, the effigy and key become the Juggernaut and the shaft pops off | defaults round 3 |
| D19 | Peaceful | The key turns but never wakes it; dormant ones in ruins never wake | defaults round 3 |
| D20 | Body | 7 blocks tall, twice the Wither (user, 2026-10-27: "he wants to be twice as big as a wither"; was 2 × 3½); hitbox about 6 wide to match his shield pauldrons (user, 2026-10-27: "widen hitbox to match"); steps up 2 blocks; knockback-immune; can't be leashed, picked up (Carry On), ride anything, or be pushed by pistons or contraptions | defaults round 3 |
| D21 | Attacks | A charge (12 damage, big knockback), a sweeping arm swing (8), and a stomp shockwave ring you jump over (6, knocks up). Damage scales with difficulty as vanilla's does. No attack breaks or changes a block. More moves by stage: D44 | defaults round 3 |
| D22 | Anti-cheese | No path to its target for 5 s: it hurls a heavy cog (8 damage, no block damage) | defaults round 3 |
| D23 | Hits | Wound, arrows and tridents glance off. Rewinding, every hit does full damage, and hits on the key from behind do 1.5× | defaults round 3 |
| D24 | Half health | Replaced by the three stages (D43) on 2026-10-27; "overwound" is now stage 3 | defaults round 3; revised round 4 |
| D25 | Lightning | A strike rewinds it instantly, skipping its open window: storm fights are harder | defaults round 3 |
| D26 | Immunities | Fire, lava, drowning, poison and wither. In water it sinks and walks the bottom | defaults round 3 |
| D27 | Targets | Players, and anything that hits it; ignores villagers and golems unless they attack | defaults round 3 |
| D28 | Boss bar | Brass-coloured, within 64 blocks, with a marker while it rewinds. No music: its ticking, faster as it runs down, is the tell | defaults round 3 |
| D29 | Left alone | Never despawns. With no player within 64 blocks for 60 s it sits down dormant where it stands and slowly mends to full; coming close wakes it again, as a ruin one | defaults round 3 |
| D30 | Drops | Always one Mainspring (Looting adds none), 4-8 Brass Ingots, 1-2 Precision Mechanisms, 50 XP; the same however it dies | defaults round 3 |
| D31 | Ruins | Juggernaut Ruins: an overgrown, collapsed hall (about 9 blocks high and 11 wide, room for him to kneel and stand) holding one dormant Juggernaut, kneeling under vines. Overworld land, about one per 64×64 chunks, and the End (D55); config toggle and spacing for each | defaults round 3 |
| D32 | Waking a ruin one | A player within 16 blocks who can see it starts a 5 s warning (eyes light, ticking starts). Back off beyond 32 blocks during the warning and it settles again | defaults round 3 |
| D33 | Ruin chest | Brass, zinc, Andesite Alloy and a few Precision Mechanisms. Never a Winding Key | defaults round 3 |
| D34 | Spring Engine recipe | The Mainspring with a Brass Casing, 4 Brass Sheets and 2 Shafts, at a crafting table | defaults round 3 |
| D35 | Winding the engine | A shaft into its back. A value box sets the stress it takes to wind (default 1,024 SU, up to 16,384); each second it stores that many SU-seconds. Lossless | defaults round 3 |
| D36 | Unwinding | A shaft out of its front. A value box sets the speed (1-256 RPM, like the Creative Motor); it offers 16,384 SU and drains by the stress actually used, so a light load lasts longer. It can wind and unwind at once | defaults round 3 |
| D37 | Redstone | A signal stops the output; a comparator reads how wound it is | defaults round 3 |
| D38 | Keeping wind | Broken or picked up, it keeps its wind as an item. One Mainspring per engine; engines don't join. Unloaded, it pauses like any Create machine | defaults round 3 |
| D39 | Moving | Works on Aeronautics ships (their kinetics are real); on trains and other contraptions it's carried but stays still | defaults round 3 |
| D40 | Goggles | Wind level, time left at the current load, input and output stress | defaults round 3 |
| D41 | Teaching | Ponder scenes for the ritual and the Spring Engine. Advancements: "Wound Up" (summon one), "Run Down" (defeat one), "Tightly Wound" (fill a Spring Engine) | defaults round 3 |
| D42 | Config | Health, damage taken while wound, rewind timings, per-player scaling, ritual wind time, ruins on/off and spacing (Overworld and End), Spring Engine capacity and maximum SU | defaults round 3 |
| D43 | Stages | Stage 1 (100-67% health): 10% damage taken while wound, 19 s wound (last 4 running down) / 6 s rewind. Stage 2 (67-33%): sheds chest and shoulder plates, 15% while wound, 16 s / 5 s. Stage 3, overwound (under 33%): sheds the rest, 20% while wound, 13 s / 4 s, double stomps. Shed plates are clatter and particles only, no items, no blocks | user, round 4 ("his attack cycle should be more varied"); numbers approved |
| D44 | Moves | Stage 1: charge, swing, stomp, grab and throw. Stage 2 adds spring leap and steam vent. Stage 3 adds gear spin. The cog throw stays for pathless targets (D22) | user, round 4 |
| D45 | The new moves | Grab and throw: close player in front, 1 s wind-up (hand opens), 6 damage, hurled about 12 blocks; a raised shield blocks it. Spring leap: a player 14+ blocks away, 1.5 s coil, lands where they stood, 10 damage within 5 blocks plus a knockback ring. Stomp ring: 6 blocks. Steam vent: 2 s tell (hiss, chest grille glows), 9-block cone, 4 damage, sets players (never blocks) alight, 2 s blindness, knockback. Gear spin: 2 s wind-up, 4 s spin, pulls players within 10 blocks, 6 damage a hit, outrunnable | user, round 4; numbers approved |
| D46 | Choosing | Situational combos: picks by distance (close under 6: swing, grab, stomp, spin; 6-14: charge, stomp, vent; far, over 14: leap, charge; scaled to his size 2026-10-27), chains 2 moves in stage 1 up to 3 in stage 3, never the same move twice in a row | user, round 4 |
| D47 | Feints | From stage 2: 25% of run-downs are fake (40% in stage 3). Tell: a sharp click instead of a slowing tick, and his eyes stay lit (a real run-down dims them). He lunges at anyone within 6 blocks; his real rewind follows 5 s later | user, round 4; numbers approved |
| D48 | Key jam | 3 hits on the key from behind in one rewind jam it: that window gets 3 s longer, once per rewind, with sparks and grinding | user, round 4; numbers approved |
| D50 | Immunities | Immune to suffocation, fall damage, fire and lava, and water (drowning; it sinks and walks the bottom). Extends D26 | user, 2026-10-27 |
| D51 | Contraptions | Nothing Create does can hurt it: Mechanical Saws, Drills, Crushing Wheels, Deployers (their fake players), and contraption collisions do no damage. Only real players, their projectiles and their pets can | user, 2026-10-27 |
| D52 | Where he appears | Always under open sky with room to stand (a clear space 7 wide and 8 high): at the effigy if it has that, otherwise at the nearest such spot above it, searching up to the surface and up to 16 blocks out. Summoned underground, he appears on the surface above, kneeling dormant, and wakes like a ruin one (D32) when a player comes close; the summoner sees "The Juggernaut rises to the surface above you". No spot found: the key won't wind past halfway ("He needs room to stand") | user, 2026-10-27 ("If summoned underground he should appear on the surface always"); arrival chosen by the user |
| D53 | Model scale | Built at full size (1 px = 1/16 block, a 1024 px texture), not a small model drawn larger as the Wither is, so his pixels match the world | user, 2026-10-27 |
| D54 | Dimensions | The ritual works in the Overworld and the End only. In the Nether the key turns but never winds ("The key won't take here") | user, 2026-10-27 |
| D55 | End ruins | Also on the End's outer islands (not the central island): the same hall half-buried in End Stone, with chorus plants over him instead of vines. About one per 48×48 chunks, with its own config toggle and spacing | user, 2026-10-27 ("Can we find ruins in the end as well as overworld?") |
| D56 | Ledges | He never steps off a drop higher than 3 blocks, so (being knockback-immune) he can't fall into the void and take the Mainspring with him. Players can still be knocked off | user, 2026-10-27 (with D55) |
| D57 | Look | An ancient Norse-style clockwork construct: charcoal and blue-grey iron, tarnished bronze, gold only where it matters, cracked stone held with staples, frost on the edges, faint frost-blue runes; a carved mask with one glowing slit and an iron beard (the steam vent); Viking-shield pauldrons; chains and leather straps; a forge-maker's seal. The key and clockwork stay. See the brief's Revision 2 | user, 2026-10-27 (art direction) |
| D49 | Rhythm | Fixed within a stage (variable wind length was not chosen): the rhythm is learnable and tightens stage by stage | user, round 4 |

## Open Questions

None.

## Core Loop

1. Reach brass and Netherite; craft a Winding Key in a Mechanical Crafter.
2. Build the T of Brass Blocks, fit the key, and turn it with a shaft for 30 s. Or find a ruin with one asleep.
3. Fight: survive the wound phase (dodge charges, jump stomps), then punish each rewind, ideally at the key from behind.
4. Take the Mainspring home; build a Spring Engine.
5. Wind it from spare rotation (windmills by day, water wheels, a surplus engine), and unwind it when you need a big push: a night's peak, a huge machine start, or fuel-free power on an Aeronautics ship.
6. Want more storage? Another key, another fight.

## Behaviour / Rules

### The ritual
1. The effigy is four Brass Blocks in a T, with a Winding Key block on the back of the T's centre (D16). The pattern is checked whenever the key turns.
2. The key is a kinetic consumer. Turned at ≥ 64 RPM, it winds; 30 s of continuous winding wakes the Juggernaut (D18). Under 64 RPM or stopped, the wind falls back slowly.
3. While winding: ticking grows louder; action-bar warning to players within 64 blocks.
4. Fully wound: the effigy and key are consumed and the shaft pops off as an item. If the effigy stands under open sky with room, the Juggernaut appears in its place, facing the way the T faces, and plays its wake-up. Otherwise he appears at the nearest spot above with open sky and room, kneeling dormant (D52).
5. The Nether: the key turns but never winds (D54).
6. Peaceful: it winds, but never wakes (D19).

### The fight (state machine)
| State | Lasts | What it does | Damage taken |
|---|---|---|---|
| Waking | about 3 s | Eyes light, unfolds, steam vents | none |
| Wound | 19 / 16 / 13 s by stage | Hunts its target in combos of its stage's moves (D44-D46); cog throw if no path for 5 s | 10 / 15 / 20% by stage; arrows and tridents glance off |
| Running down | last 4 s of Wound | Slows, sags, ticking slows, eyes dim, key turns lazily. From stage 2 some are feints (D47) | as Wound |
| Rewinding | 6 / 5 / 4 s by stage, +3 s if the key jams (D48) | Stands still, key spins on its back | full; 1.5× on the key from behind |
| Dormant | until a player comes | Kneels, mends slowly to full | full (but it's waking) |

- Stages (D43): at 67% and 33% health it sheds plates (a stagger of about 1 s, clatter and particles), takes more damage while wound, gains moves and tightens its rhythm. Stage 3 is "overwound": double stomps.

| Stage | Health | Wound / rewind | Damage while wound | Moves | Feints |
|---|---|---|---|---|---|
| 1 | 100-67% | 19 s / 6 s | 10% | charge, swing, stomp, grab and throw | none |
| 2 | 67-33% | 16 s / 5 s | 15% | + spring leap, steam vent | 25% of run-downs |
| 3, overwound | under 33% | 13 s / 4 s | 20% | + gear spin; stomps come double | 40% of run-downs |

- Combos (D46): by distance, close (under 4 blocks): swing, grab, stomp, spin; 4-10: charge, stomp, vent; over 10: leap, charge. Two moves a combo in stage 1, up to three in stage 3; never the same move twice in a row.
- Feints (D47): a sharp click instead of the slowing tick, eyes stay lit; it lunges at anyone within 6 blocks, then really runs down 5 s later.
- Key jam (D48): 3 hits on the key from behind in one rewind add 3 s to it, once per rewind.
- Lightning: jumps straight from Rewinding (or Running down) to Wound (D25).
- Left alone 60 s with nobody within 64 blocks: Dormant (D29). A player within 12 blocks who can see it: 5 s warning, then Waking (D32).
- Health scaling: on waking, max health = 400 × (1 + 0.5 × extra players within 32 blocks) (D14). It doesn't rescale mid-fight.

### Drops
One Mainspring always, 4-8 Brass Ingots, 1-2 Precision Mechanisms, 50 XP (D30).

### Spring Engine
- Back shaft: winding, at the stress set on its value box (D35). Front shaft: output, at the speed set on its value box, 16,384 SU offered (D36).
- Stored = SU-seconds; capacity 58,982,400 (an hour at 16,384 SU, D13). Winding adds the set stress per second; output removes the stress actually used per second.
- Empty: the output stops. Full: the winding input stops taking stress (it stops loading the input network).
- Redstone stops the output; comparator gives 0-15 by wind (D37).

## Progression

**Late game, beside the Wither** (D11). Needs Create brass, Precision Mechanisms (sequenced assembly), a Mechanical Crafter, and a Netherite Ingot for each key. It doesn't need the Wither, so players can meet either boss first. It unlocks the Spring Engine, rotational storage for big late-game builds and Aeronautics ships. The ruins (D31) are an exploration route to a first Mainspring without a key, for players who find one.

## Balance

- **Cost per Mainspring:** a Netherite Ingot, 4 Precision Mechanisms, 4 Brass Sheets, 4 Brass Blocks, and a 2-3 minute fight with real risk. Ruins give a free key's worth once each.
- **Reward:** an engine that never wears out. It makes nothing: every SU it gives was put in earlier (lossless, D35), so it moves power in time, not out of nowhere.
- **Filling it is a real job:** an hour at 16,384 SU in takes an hour at 16,384 SU, or 16 hours at the default 1,024 SU. Players who want it full overnight need a big surplus.
- **Exploits checked:**
  - Self-winding (front shaft into its own back, or two engines winding each other): drains by actual use, including the winding, so the net is zero. No free power.
  - Mainspring farming: no jars or cages (D15); every Mainspring costs a key or a ruin.
  - Cheesing the fight: pillars and pits meet the cog throw (D22); arrows glance off while wound (D23); lava and drowning traps do nothing (D26); it can't be pushed into machines (D20).
  - Griefing: it breaks nothing (D2), and summoning needs the summoner's own Netherite.
- **Overpowered?** On an Aeronautics ship, an hour of 16,384 SU replaces fuel for a voyage. That's the point of the reward, and it has to be wound at home first.
- **Useless?** No: nothing else stores rotation, and its output is as big as a Supercharged Magma Engine's.

## Create Integration

| System | Interaction | Notes |
|---|---|---|
| Shafts and gears | Ritual: a shaft winds the key. Spring Engine: shaft in at the back, shaft out at the front | Core of both |
| Mechanical Crafter | Makes the Winding Key | Keeps the key a Create late-game build |
| Belts and depots | None | Nothing to process |
| Contraptions | The Juggernaut can't be pushed or carried. The Spring Engine is carried but stays still | D20, D39 |
| Aeronautics ships | The Spring Engine works aboard | D39; the big use case |
| Trains and elevators | The Juggernaut can't ride; the engine is carried, still | D20, D39 |
| Logistics and packages | None | — |
| Vaults, funnels, chutes | None | — |
| Deployers and arms | A deployer can place the key block or the effigy (it's ordinary blocks). Deployers, saws, drills and crushing wheels can't hurt it | D51 |
| Redstone, comparators | Spring Engine: signal stops output; comparator reads wind | D37 |
| Goggles | Spring Engine wind, time left, input and output stress | D40 |
| Ponder | The ritual; the Spring Engine | D41 |

## Technical Notes

- **Entity:** a new hostile `Monster` with a synced phase (WAKING, WOUND, RUNNING_DOWN, REWINDING, DORMANT), an overwound flag and a phase timer; the client animates from these. `ServerBossEvent` for the bar (D28). Max health scaled once on waking and saved.
- **Saving:** phase, timer, overwound, scaled max health, dormant flag, the 60 s alone timer. Persistent (never despawns).
- **Damage rule:** in `hurt`/`actuallyHurt`, scale damage by phase; projectiles glance (deflect) while wound; key bonus from the attack's direction relative to its back.
- **No block changes:** none of its goals call block-breaking; the mob-griefing rule doesn't matter because it never tries.
- **Capture Bottle:** refuse it in `MonsterCapture.canCatch`.
- **Ritual:** the Winding Key is a kinetic consumer block entity holding wind progress; the T is checked with a `BlockPattern` like the Wither's. On waking, search for the arrival spot (open sky via the heightmap, a clear 7×8×7 space) from the effigy up and out to 16 blocks; refuse to finish winding if none, or in the Nether.
- **Spring Engine:** a Create kinetic block that winds from one side and drives the other. One block entity sits on one kinetic network, so the two shafts likely need two linked block entities (or a two-block engine). Settle this in implementation, before the look is final, since it may change the footprint.
- **Ruins:** two structures (Overworld hall; End hall on outer islands, biome tag `end_highlands`/`end_midlands`), template plus placement with spacing and separation, whose piece spawns one dormant, persistent Juggernaut and a loot chest.
- **Ledges:** a path node evaluator that refuses drops over 3 blocks (D56).
- **Config (CMConfig, server):** juggernautHealth, woundDamageTaken (0.1), rewindSeconds, overwoundRewindSeconds, perPlayerHealthBonus, ritualWindSeconds, ruinsEnabled, ruinSpacing, endRuinsEnabled, endRuinSpacing, springEngineCapacity, springEngineMaxStress.
- **Data:** entity type and attributes, spawn egg, loot table, structure and its tags, recipes (one mechanical crafting), blocks and items, lang with subtitles, advancements, mineable tags.
- **Tests:** unit tests for the wind arithmetic and the damage rule; game tests for the ritual (summons; peaceful doesn't; stopping unwinds), the Capture Bottle refusing it, drops, no block changed after a round of attacks, and the Spring Engine storing and giving back the same SU-seconds.

# ONE MINUTE UNDERSTANDING

The Clockwork Juggernaut is a giant war automaton, twice the Wither's height, the bots' grim ancestor. Build a T of Brass Blocks, fit a Winding Key and wind it with a shaft, and it wakes, or find one asleep in a ruin. While its spring is wound, it shrugs off almost everything; it regularly stops to rewind, key exposed, and that's when you hurt it. Each third of its health it loses comes with plates shed, new moves (a leap, a grab and throw, a steam vent, a gear spin) and a tighter rhythm, and from the second stage it sometimes fakes running down. It never breaks a block. Beat it for a Mainspring, the heart of the Spring Engine: the only block that stores rotation, an hour at 16,384 SU, wound up from spare power and let out when you need it.

# WHAT IT IS

A summoned late-game boss whose fight is about rhythm: dodge while it's wound, punish while it rewinds. It drops a Mainspring, which builds a Spring Engine that stores rotation.

# WHY IT EXISTS

- A second big boss beside the Wither, built from Create's own materials.
- Create can't store rotation; the Spring Engine can.
- Fuel-free power for Aeronautics ships, wound at home.

# HOW PLAYERS USE IT

- Craft a Winding Key in a Mechanical Crafter (Netherite, Precision Mechanisms, brass).
- Build the brass T, fit the key, turn it 30 s with a shaft.
- Fight: dodge charges and stomps, hit it while it rewinds.
- Build a Spring Engine from the Mainspring; wind it from spare rotation.
- Unwind it for peaks, big machines, or a voyage.

# HOW IT FITS CREATE

- The ritual is rotation: a shaft winds the key.
- The key is made in a Mechanical Crafter.
- The Spring Engine takes a shaft in and gives a shaft out.
- Works on Aeronautics ships; goggles and comparators read it.
- Ponder scenes teach the ritual and the engine.

# IMPORTANT RULES

- It never breaks or changes blocks.
- Wound: 10/15/20% damage by stage, arrows glance off. Rewinding: full, 1.5× on the key.
- 400 health, +50% for each extra player nearby when it wakes.
- Three stages at 67% and 33%: plates shed, new moves, tighter rhythm.
- From stage 2, some run-downs are feints; three key hits in one rewind jam it for 3 s more.
- Lightning rewinds it instantly; no path for 5 s, it throws a cog.
- Always drops exactly one Mainspring.
- Can't be jarred or caged, or hurt by contraptions; never wakes on peaceful.
- Always appears under open sky; summoned underground, he kneels on the surface above. Not in the Nether.
- The Spring Engine gives back exactly what went in, never more.

# EDGE CASES

- Shaft stops mid-ritual → the key slowly unwinds back.
- Everyone leaves → after 60 s it sits dormant and mends.
- Fought in a storm → lightning skips its rewind.
- Player pillars up → it throws cogs. Saw or drill trap → no damage.
- Lava, water, a drop or buried in sand → immune; walks the bottom.
- Engine wound from its own output → net zero.
- Chunk unloads mid-fight → saved, carries on when loaded.
- Fooled by a feint → it lunges, but the real rewind comes 5 s later.
- Summoned underground → kneels on the surface above until you come up.
- Fought on an End island → he never steps off; you can be knocked off.

# DEVELOPMENT CHECKLIST

- [ ] Juggernaut entity: attributes, synced phase, overwound flag, timers, save and load
- [ ] AI goals: hunt, combo picker (distance, stage, no repeats), charge, swing, stomp (shockwave), grab and throw, spring leap, steam vent, gear spin, cog throw when pathless, rewind, feint, key jam, dormant and wake
- [ ] Stages: health thresholds, plate shedding (stagger, particles), damage taken and timings per stage
- [ ] Damage rule: 10% while wound, projectile glance, full while rewinding, 1.5× on the key
- [ ] Health scaling by players on waking; boss bar with rewind marker
- [ ] Lightning rewind; immunities (fire, lava, drowning, suffocation, falls, poison, wither); no damage from contraptions or Deployer fake players
- [ ] Never despawns; dormant after 60 s alone; mends while dormant
- [ ] Peaceful handling (ritual and ruins)
- [ ] Capture Bottle refuses it
- [ ] Loot table (Mainspring, brass, Precision Mechanisms, 50 XP)
- [ ] Winding Key: item, block, kinetic block entity, effigy pattern check, wind progress, unwind, action-bar warning, summon; arrival spot search (open sky, room), surface arrival kneeling, Nether refusal
- [ ] Mechanical crafting recipe for the key
- [ ] Mainspring item
- [ ] Spring Engine: block(s), kinetic in and out, value boxes, SU-second store, redstone stop, comparator, keeps wind as an item, goggles
- [ ] Spring Engine on Aeronautics ships; still on contraptions
- [ ] Spring Engine recipe
- [ ] Juggernaut Ruins: Overworld and End structures, placement, dormant spawn, loot chest, config; no drops over 3 blocks in pathing
- [ ] Config keys (CMConfig)
- [ ] Model, rig, animations (brief → Blockbench first), renderer
- [ ] Sounds through the sound workflow (ticking, winding, attacks, rewind, wake, Spring Engine)
- [ ] Lang, subtitles, advancements, Ponder scenes, website entries
- [ ] Unit tests (wind arithmetic, damage rule) and game tests (ritual, peaceful, bottle refusal, drops, no block changes, engine in = out)

# "IDIOT TEST"

- Brass T, Winding Key, turn with shaft: boss wakes.
- Wound: nearly unhurtable. Arrows bounce.
- It stops to rewind, faster each stage.
- Rewinding: hit it. Key from behind hurts most.
- Seven blocks tall. Never breaks blocks.
- Kill it: one Mainspring, always.
- Mainspring builds a Spring Engine.
- Spring Engine stores rotation: shaft in, shaft out.
- Full engine: an hour at 16,384 SU.
- Can't be jarred. Also sleeps in rare ruins.

# COMPLETENESS SCORE

- **Gameplay: 95%.** The look and motion feel are still for the brief.
- **World Integration: 90%.** The ruin's layout isn't designed yet.
- **Balance: 85%.** The numbers haven't been played; it needs a fight-length check in game.
- **Progression: 95%.** Placement is settled.
- **Automation: 95%.** Contraptions can't hurt it; the Spring Engine's two-shaft question is still for implementation.
- **Edge Cases: 95%.** Ruin edge cases (a ruin buried by terrain, or two side by side) still need checking when the structure is built.
- **Overall Design Readiness: 85%.**
