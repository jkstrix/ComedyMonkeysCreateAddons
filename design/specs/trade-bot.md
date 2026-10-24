# Trade Bot: Systems Spec

Status: **ready for development (v1)**  ·  Related: [brief](../briefs/trade-bot.md), [sounds](../sound/trade-bot.md), model [trade-bot.bbmodel](../models/trade-bot.bbmodel)

## Concept
"A Create mod style robot called Trade Bot" who rolls on one wheel, waits at an idle station for actions, handles items in containers, and trades with seated villagers.
Clarified: **a clockwork robot, powered by FE through his station, who sells your goods to seated villagers for emeralds.**

## Decisions
| # | Area | Decision | Source |
|---|---|---|---|
| D1 | Identity | Clockwork robot, 1.5 blocks tall, one wheel, two arms, Wall-E face, Create palette | brief (user) |
| D2 | Home | Idle station where he waits for actions; his lights go off and the station glows while he's docked | brief (user) |
| D3 | Station flow | Reverse-hops into the cradle; hops out forwards when an action comes in; stays docked at night | brief |
| D4 | Movement | Rolls; hops stairs, slabs and blocks taller than a quarter block (up to a full block); rolls over anything 4 px or lower | brief (user) |
| D5 | Item moves | Takes from and puts into chests, barrels and depots; puts onto belts | brief (user) |
| D6 | Blind containers | Barrels, high or unreachable chests (and any container without its own animation): stands back half a block, the container opens itself, items "poof" in or out | brief (user) |
| D7 | Trading | Trades with villagers **only while they sit on a seat**; from up to one block away, obstacle or not | brief (user) |
| D8 | Fear | Hostile mob seen → spot, flee to a corner, hide, cower, recover (five clips) | brief (user) |
| D9 | Reactions | Wall bump, sees player, player walks away, trade done, gets hit | brief |
| D10 | Seats | Villagers and wandering traders sit on Create seats, including contraption seats (config `villagersSitInSeats`) | existing mod |
| D11 | Restocks | The Cuckoo Clock sets nearby villagers' restocks to 8:00 and 12:00 | existing mod |
| D12 | Mod rules | Mod id without "create" (Carry On); Carry On block pickup needs a BlockItem | existing mod |
| D13 | Purpose | **Automated trader**: sells items from your storage to seated villagers for emeralds | user R1 |
| D14 | Jobs (v1) | You assign **input** containers; he trades their contents **for emeralds** and delivers them to **one output** (belt, chest, barrel, depot or any container). A later **upgrade** adds a Trade Ledger for advanced jobs, so the job system must be extensible | user R1 |
| D15 | Obtaining | **Large, expensive mechanical crafting** recipe (Create mechanical crafters) | user R1 |
| D16 | Station power | The station needs **FE** supplied **from underneath** it | user R1 |
| D17 | Trade rules | The villager's **real** trades, like a player: uses stock, waits for restocks, earns villager XP and levels | user R1 |
| D18 | Climbing | **Can't climb ladders or use bubble columns** | user R1 |
| D19 | Linking (v1) | Right-click the station: screen buttons **Add input** / **Set output**, then click the block. No extra item (the ledger replaces this later) | user R2 |
| D20 | Buyers | **Any seated** villager or wandering trader in range whose "buys" trades match an item he holds; found automatically | user R2 |
| D21 | Charging | The station **charges him with FE while he's docked** (64 FE/t, 100,000 FE station buffer); with too little charge he stays home | user R2 |
| D22 | Inputs/output | Up to **4 inputs + 1 output**; items no seated villager will buy **stay in the input**; if the output is full he **waits beside it** (worried) and retries | user R3 |
| D23 | Ownership | The **placer owns him**; only the owner (and their team, if a team mod is present) can open the station, change jobs or use the wrench on him or the station; **max 4 bots per player** (config) | user R3 |
| D24 | Damage | **20 HP**; at 0 he **winds down** (slumps, eyes dark) until re-wound with a wrench; never dies, never drops his load | user R3 |
| D25 | Tier | **Mid-late game**: after brass and the mod's electricity | user R3 |
| D26 | Hostiles | Monsters **ignore** him but he still runs the scared routine; explosions and stray projectiles hurt him | user R4 |
| D27 | Item form | Crafted as **one item: Trade Bot on his station**. **Wrench on him** sends him home. **Sneak + wrench on the station** packs both into that one item (jobs kept) | user R4 |
| D28 | Range | Works within **64 blocks** of his station (config); **opens doors**, trapdoors and fence gates | user R4 |
| D29 | Water and lava | **Avoids** both; if pushed into water he winds down until out and dry (~5 s); lava hurts normally | user R4 |
| D30 | Packing while away | The station **recalls him first** and only packs once he's home; nothing is lost | user R5 |
| D31 | Riding | **Not in v1** | user R5 |
| D32 | Weather | Works in rain; **goes home in thunderstorms** | user R5 |
| D33 | Chunk unloads | Saves his job, load and pouch and **resumes** on load; never deletes items; if a target has gone, returns items to their source or his station | user R5 |
| D34 | Prices | **His own reputation** with each villager, earned through trading; player reputation and Hero of the Village don't apply | user R5 |
| D35 | Speed | **Animation-paced**: one trade per trade animation (~7 s) | user R6 |
| D36 | Containers | **Any item container** (anything exposing an item handler) as input or output; containers without their own animation use the blind routine; belts are output-only | user R6 |
| D37 | Machines | Deployers and mechanical arms **can't** use or feed him | user R6 |
| D38 | Redstone | **Comparator output**: idle / working / out of charge (three levels); a **redstone signal pauses** him at home | user R6 |
| D39 | Goggles | On the station: charge (FE and estimated trips left), current task, linked inputs and output, trades today | user R7 |
| D40 | Teaching | **Ponder scenes in v1** (station, linking, power, trading) | user R7 |
| D41 | Packages | **Not in v1**; maybe in v2 (not necessarily tied to the ledger) | user R9 |
| D42 | Reputation | Kept **per villager** when he's packed; grows with each trade; **capped** at vanilla's limit; **never negative** | user R7 |
| D43 | Sounds | Voice cooldowns as in the sound doc; normal entity range; subtitles | user R7 |
| D44 | Recipe | **5×5 mechanical crafting**, trader-themed: 4 precision mechanisms, 4 electron tubes, brass casing, 6 brass sheets, 4 andesite casings, cogwheels, an emerald block and a copper-cased FE coil (exact grid set at implementation) | user R8 |
| D45 | Load | Carries **a barrel's worth: 27 stacks**, **mixed**, and can visit **several seated villagers** in one trip. He trades until no villager will buy what he has, he's low on charge, or he's out of items | user R8, R9 |
| D46 | Interruptions | Villager stands up, dies, or a player opens its trades: he **aborts safely** (shrug), keeps everything, returns unsold items to the inputs, retries later; **players always get priority** | user R8 |
| D47 | Scheduling | Leaves only when it's day, he's not paused, has enough charge, an input holds something a seated villager in range buys, and the output has room. **Cycles inputs in turn** | user R8 |
| D48 | Emeralds | Earned emeralds go into a **separate, unlimited emerald pouch** (not the 27 slots), delivered to the output | user R9 |
| D49 | Charge drain | Charge drains **4 FE per block rolled** and **50 FE per trade**, from a **100,000 FE** full charge (config). He turns home with just enough to get back; never leaves without enough for the round trip plus one trade. Buyers are visited **nearest-first**, re-planning after each | user R9 |
| D50 | Owner offline | **Keeps working** while his area is loaded | user R10 |
| D51 | Station breaking | **Unbreakable except by the (owner's) wrench**: pickaxes, explosions and mobs can't break it | user R10 |
| D52 | Idle charging | With nothing to sell (inputs empty, or no seated villager buys what's there) he **goes home and charges** | user R10 |
| D53 | Chunk loaders | **Works whenever his area is loaded, including by chunk loaders** (any mod's), with no player nearby; he behaves exactly as he would with a player present | user R10 |
| D54 | Shared villagers | **Two bots, one villager:** first come, first served; the second bot picks another buyer | user R11 |
| D55 | Unloaded targets | **A target in an unloaded chunk** (inside his range but outside the loaded area): he skips it this trip and retries later | user R11 |
| D56 | FE Coil | A new crafting component, the **FE Coil**, made from **copper and redstone** (proposed: 4 copper sheets around 1 redstone dust, shaped). It's the FE part of his recipe (D44), and later electric machines can reuse it. Its look gets previewed in Blockbench first | user R12 |
| D57 | Trade XP | No XP orbs. The XP vanilla would drop for each trade is banked and **delivered to the output as Create Nuggets of Experience** (1 per 3 XP, the remainder kept for next time), together with the emeralds. Villagers still gain their own XP and levels | user R12 |
| D58 | Comparator | Strengths: **0** no bot, **5** docked and idle, **10** working, **15** out of charge (refines D38) | user R12 |
| D59 | Lava and void | If he's wound down in lava, or falls into the void, he's **returned to his station, still wound down**, so he's never lost or stuck burning | user R12 |
| D60 | Doors and gates | He **closes the doors and fence gates he opened** once he's through; ones that were already open stay open (refines D28) | user R13 |
| D61 | Linking (refines D19) | **Right-click the station** for its screen (status, charge, links with an ✕ to unlink; **Select containers**, **Call home**). **Select containers** starts Create Mechanical Arm-style picking in the world: right-click a container: the first click makes it **takes from** (cyan outline); a second makes it **delivers to** (amber) if there's no output yet, or unlinks it if there is; a click on the output unlinks it. Belts can only be the output. Chests never open while picking (the server takes the clicks over). Picking uses any hand (no item needed): **left-click unlinks**; a hint line shows above the hotbar. Sneak-right-click or the station again to finish | user R14 |
| D62 | Output and returns (refines D22, D48) | The output **only ever gets emeralds and Nuggets of Experience**. Unsold items are **returned to the input they came from on his way back to the station**, nearest first; anything whose input is full goes to any input with room, and if they're all full he keeps it for next time | user R15 |

## Open Questions
None for v1.

Parked for later versions:
- **v2: Trade Ledger upgrade**: advanced jobs (buying, specific trades, multiple outputs, routes).
- **v2 (maybe): Create 6 packages**.
- **Later: riding** trains, minecarts and seats.

## Core Loop
1. Place the Trade Bot item: a station with him docked. Feed the station FE from below.
2. Link up to 4 input containers and 1 output at the station's screen.
3. Seat villagers (and wandering traders) on Create seats within 64 blocks.
4. He charges, then rolls out, loads up to 27 stacks that seated villagers buy, trades from up to a block away, banks the emeralds in his pouch, and visits buyers nearest-first.
5. He delivers the emeralds to the output, returns unsold items to the inputs, goes home and charges, and repeats as restocks allow, with or without players around if a chunk loader keeps him loaded.

## Behaviour / Rules
States, with their animation clips:
1. **Docked / charging** (`dock_lights_out`, waiting): the station charges him at 64 FE/t. Every few seconds he checks the leave conditions (D47). With nothing to sell he stays docked and charges (D52).
2. **Leave** (`action_comes_in`): the flag springs up, his lights come on, he hops out (`start_rolling` → `rolling`).
3. **Load** (`chest_take`, `barrel_take`, `chest_take_high`, `depot_take`, or the blind routine): from the inputs in turn, he takes items that seated buyers in range want, up to 27 stacks.
4. **Trade run** (`rolling` between buyers; `trade_villager` / `trade_villager_open` at each): at each seated buyer, one trade per animation while that villager buys anything he holds and his charge allows (D45, D49). Emeralds go to the pouch (D48), and the trade's XP is banked for nuggets (D57). His reputation with that villager grows (D42).
5. **Deliver** (`chest_put`, `barrel_put`, `chest_put_high`, `depot_place`, `belt_place`): the pouch goes to the output; if it's full he waits beside it and retries (D22).
6. **Return**: unsold items back to their inputs; home; reverse-hop into the cradle (`stop_rolling`, `dock_lights_out`).
7. **Interrupts at any time**:
   - a hostile mob seen → the scared routine (`scared_*`), then resume;
   - a thunderstorm or night → go home (D32, D3);
   - a redstone pause → go home (D38);
   - the villager is gone or busy → abort safely (D46);
   - damage to 0 → wind down (D24);
   - water → wind down until dry (D29);
   - a wrench on him → go home (D27).

Movement: he paths around water and lava, hops up to one block, uses doors and gates, and never uses ladders or bubble columns (D4, D18, D28, D29). Mannerisms play while idle; reactions play as in the brief. Nothing depends on a player being present (D50, D53).

## Progression
**Mid-late game** (D25). He needs brass and precision mechanisms (Create mid game) for the 5×5 mechanical-crafting recipe (D15, D44), and a steady FE supply, such as the mod's Lava Generator at 1,024 FE/t, which runs a station at 64 FE/t many times over. He unlocks hands-free emerald income from bulk goods (farms, mines), feeding emerald-based progression and other villager trades. The Trade Ledger upgrade (v2) is the next tier.

## Balance
- **Cost:** an expensive recipe, FE infrastructure, seats and housing for villagers, and storage plumbing.
- **Reward:** emeralds at real villager prices, limited by villager stock and restocks (D17). With the Cuckoo Clock that's two restocks a day per villager, so income scales with the number of seated villagers, not with time alone. Chunk-loaded bases (D53) earn while you're away, but still only as fast as restocks allow.
- **Exploits considered:**
  1. **Infinite emeralds:** blocked by real stock and restock limits.
  1a. **XP farm:** trade XP arrives as nuggets in the output (D57), so it's capped by the same stock and restock limits as the emeralds (about 3–6 XP per trade), never by time alone.
  2. **Discount farming** (zombie curing, Hero of the Village): blocked by his own capped, never-negative reputation (D34, D42).
  3. **Duplication on chunk unload or packing:** saved state, recall before packing, no deleting (D30, D33).
  4. **Griefing:** owner-only control; an unbreakable station; he winds down instead of dying (D23, D24, D51).
  5. **Server load:** 4 bots per player and a 64-block range (configurable); chunk-loaded bots tick constantly, so the per-player cap matters most on servers.
- **Overpowered check:** at scale he equals a player standing at many seated villagers; restocks cap him. With 4 bots and many villagers he's strong, but that took mid-late investment.
- **Useless check:** nothing in Create automates villager trading, so he fills a real gap.

## Create Integration
| System | Interaction | Notes |
|---|---|---|
| Mechanical crafters | His recipe (5×5) | D44 |
| Electricity (this mod) / any FE | Station power from below | D16, D21 |
| Shafts, gears | None in v1 | The station runs on FE, not rotation |
| Belts | Output target | `belt_place` |
| Depots | Input and output | `depot_take`, `depot_place` |
| Item vaults, other storage | Input and output | Blind routine (D36) |
| Funnels, chutes | Indirect | Players feed his inputs or drain his output with them |
| Seats (+ contraption seats) | Where his buyers sit | D7, D10; a villager on a contraption counts while it's seated; if the contraption moves away, abort (D46) |
| Trains, contraptions, elevators | No riding in v1 | D31 |
| Deployers, mechanical arms | No | D37 |
| Redstone, comparators | Pause input; comparator 0 / 5 / 10 / 15 | D38, D58 |
| Nuggets of Experience | His trade XP, delivered to the output | D57 |
| Engineer's Goggles | Station info | D39 |
| Ponder | Station, linking, power, trading | D40 |
| Packages / logistics | Not in v1 (maybe v2) | D41 |

## Technical Notes
- **Entity:** `TradeBotEntity` with a vanilla `EntityModel`/`ModelPart` (box UV) and code-driven animation reproducing the Blockbench clips; spring-chain secondary motion (brief). Synced entity data covers the animation state, lights, held-item visual, owner, charge (coarse) and reputation summary.
- **Station:** a block plus block entity with a renderer (winder, flag, glow); an FE capability on the bottom face only; a 100,000 FE buffer; a screen for linking; comparator output; goggles info; owner-only wrench handling; unbreakable otherwise (hardness −1, blast-proof).
- **Item:** `trade_bot` (the station with him docked), carrying NBT for his jobs, charge, reputation and owner.
- **AI:** a custom brain or goal set: a scheduler (D47), loader, trade runner (nearest-first), deliverer, returner; scared routine (hostile detection, corner search); home rules (night, thunder, pause, idle charging D52). No logic may require a nearby player (D50, D53): no player-distance checks, no client-only state in the decisions.
- **Chunk loading (D53):** he ticks whenever his chunk is entity-ticking, however it's loaded. Pathing only into loaded chunks; targets in unloaded chunks are skipped this trip.
- **Pathfinding:** a node evaluator with doors and gates allowed; ladders, bubble columns, water and lava forbidden; step-up to one block via hop.
- **Trading:** uses `MerchantOffers` directly (uses, villager XP and levels, restock rules) through an addition to the existing `VillagerMixin`, not vanilla `notifyTrade` (which drops XP orbs and posts NeoForge's trade event with no player). His reputation lives on him, per villager. A player trading with the villager blocks him (D46).
- **Saving:** jobs (input and output positions plus dimension), the 27-slot load, the emerald pouch, the banked XP, charge, per-villager reputation and owner, all with codecs. On unload, the load is persisted with him.
- **Config (`CMConfig`, server):** range (64), bots per player (4), FE per tick charging (64), station buffer (100,000), full charge (100,000), FE per block (4), FE per trade (50), trade-time floor.
- **Data:** mechanical crafting recipe, the FE Coil item and its recipe, lang (with 80 subtitles), sounds.json (80 events with variants), an entity tag for the Carry On blacklist, Ponder scenes.

# ONE MINUTE UNDERSTANDING
Trade Bot is a cheerful clockwork robot who sells your stuff to villagers. Craft him (on his winding station) with Create mechanical crafters, power the station with FE from below, and link up to four input containers and one output. Seat villagers on Create seats nearby. He charges, rolls out, loads up to a barrel's worth of goods, trades at real villager prices with every seated buyer in range, banks the emeralds, delivers them, and goes home to charge. He keeps working whenever his area is loaded, even by a chunk loader with no one around. Real stock limits keep him fair; his own reputation slowly earns him better prices.

# WHAT IT IS
A mid-late-game automation robot that trades your goods to seated villagers for emeralds. He lives on an FE-powered winding station. He's crafted together with the station, as one item.

# WHY IT EXISTS
- Nothing in Create automates villager trading.
- Turns farm and mine output into emerald income, hands-free.
- A charming, living machine that makes a base feel alive.

# HOW PLAYERS USE IT
- Craft the 5×5 recipe on mechanical crafters; place him and his station.
- Feed FE into the station from below.
- Link up to 4 inputs and 1 output at the station screen.
- Seat villagers on Create seats within 64 blocks.
- Collect the emeralds from the output; wrench him home or pack him up.

# HOW IT FITS CREATE
- Built with mechanical crafters, precision mechanisms and brass.
- Uses Create seats (and this mod's sitting villagers) for trading.
- Feeds belts and depots; works with vaults and any storage.
- Comparator and redstone control; goggles info; Ponder scenes.
- Runs on this mod's FE electricity.

# IMPORTANT RULES
- Only trades with villagers who are **sitting on a seat**.
- Real trades: stock, restocks and villager XP all apply.
- Sells for emeralds only (buying comes with the v2 ledger).
- Carries 27 stacks (mixed), plus a separate emerald pouch; trade XP comes out as Nuggets of Experience.
- Charge drains per block and per trade; recharges docked.
- Goes home at night, in thunderstorms, when paused, or when idle.
- Never climbs ladders or uses bubble columns; avoids water and lava.
- Owner-only control; max 4 per player; works with the owner offline and in chunk-loaded areas.
- Never dies: winds down at 0 HP; re-wind him with a wrench.
- The station breaks only to the owner's wrench (packs both into one item).

# EDGE CASES
- Villager stands up, dies or gets a player customer mid-trade: he aborts and retries later. Another bot got there first: he picks a different buyer.
- Output full: he waits beside it, worried, and retries.
- Chunk unloads mid-trip: everything is saved; he resumes on load.
- Kept loaded by a chunk loader with no players: he works normally. A target in an unloaded chunk is skipped and retried next trip.
- Station packed while he's out: he's recalled first, then it packs.
- An input or output removed: items go back to their source or his station.
- Pushed into water: he winds down until he's out and dry. Wound down in lava, or fallen into the void: returned to his station.
- Hostile appears: scared routine; monsters never target him.
- No charge to get home: he never leaves without a round-trip reserve.
- A villager on a contraption seat moves away: abort (he doesn't ride).

# DEVELOPMENT CHECKLIST
- [ ] `TradeBotEntity`: model (box UV from the bbmodel), renderer, emissive lights, code animations for every clip, spring-chain motion
- [ ] Station block, block entity and renderer (winder, flag, glow); bottom-face FE capability; 100k buffer; charging
- [ ] Combined item (station + bot) with NBT; place / pack (wrench) / recall logic; unbreakable otherwise
- [ ] Station screen: link 4 inputs and 1 output by clicking blocks; job display
- [ ] AI: scheduler, loader, nearest-first trade runner, deliverer, returner, idle charging
- [ ] Trading via `MerchantOffers` (uses, XP, levels, restocks); seated-only check; player priority; abort handling
- [ ] Bot reputation (per villager, capped, persistent through packing)
- [ ] 27-slot load inventory and unlimited emerald pouch; never voids items
- [ ] Pathfinding: doors and gates yes; ladders, bubble columns, water and lava no; one-block hop; loaded chunks only
- [ ] Charge drain (per block, per trade) and reserve rule
- [ ] Day/night, thunderstorm, redstone pause, water wind-down, 0 HP wind-down and wrench re-wind
- [ ] Hostile detection, corner finding, scared state machine (5 stages)
- [ ] Owner, team access and per-player limit (saved data)
- [ ] No player-presence dependencies; tested in a chunk-loaded area with no players
- [ ] Comparator output (0 / 5 / 10 / 15); redstone pause input
- [ ] Trade XP banked and delivered as Create Nuggets of Experience
- [ ] Lava and void rescue to the station
- [ ] Engineer's Goggles tooltip
- [ ] Save/load with codecs; chunk-unload safety
- [ ] Config entries in `CMConfig`; lang and subtitles
- [ ] Sounds: register 80 events, sounds.json with variants, looping sound instances (roll, fast roll, tremble, station hum)
- [ ] FE Coil item (look previewed in Blockbench first) and its recipe
- [ ] 5×5 mechanical-crafting recipe
- [ ] Ponder scenes: station and power, linking, trading
- [ ] Carry On blacklist tag for the entity
- [ ] Unit tests (config, trade rules, charge maths, save round-trip)

# "IDIOT TEST"
- Robot sells your stuff to villagers for emeralds.
- Villagers must be sitting on seats.
- Craft him on his station; power the station with FE.
- Link four inputs and one output at the station.
- He carries 27 stacks plus an emerald pouch.
- Real villager trades: stock and restocks still apply.
- Goes home to charge: night, storms, idle, low charge.
- No ladders, no bubble columns, avoids water and lava.
- Owner only; can't be killed; the wrench packs him.
- Works unattended in chunk-loaded areas; hostiles scare him.

# COMPLETENESS SCORE
Gameplay: 95% · World Integration: 92% · Balance: 85% · Progression: 90% · Automation: 92% · Edge Cases: 95% · **Overall Design Readiness: 85%**
- Gameplay 95%: v1 is complete; the ledger (v2) is deliberately out of scope.
- World Integration 92%: team-mod detection for owner access is to be confirmed at implementation.
- Balance 85%: the FE numbers, the 7 s trade pace and the recipe cost need a playtest pass.
- Progression 90%: the recipe's exact 5×5 grid is still to be laid out, and the FE Coil's recipe is a proposal.
- Automation 92%: packages deferred to v2 by choice.
- Edge Cases 95%: all decided; villagers on moving contraption seats need testing in game.
- **Overall 85%** (the lowest area, balance): ready to build, with numbers to tune in playtesting.
