# Helper Bot: Systems Spec

Status: **ready for development (v1)**  ·  Related: [Trade Bot spec](trade-bot.md), [Trade Bot brief](../briefs/trade-bot.md), model [trade-bot.bbmodel](../models/trade-bot.bbmodel) (look reference), **[Helper Bot brief](../briefs/helper-bot.md)**.

## Concept
"A new npc, a train driver robot that is part block (doesn't move from the spot) so he can't fall off, a bit like blaze burners when you get them to drive create trains. He should be based on the trade bot's look and feel with the ability to be dyed in a crafting bench etc (one dye per one driver), but be relatively cheap as he's just replacing the blaze burner with something more fun and animated."

Clarified: **Helper Bot is a cheap, dyeable, animated robot block that does a Blaze Burner's two Create jobs (driving trains and keeping shop at a Stock Ticker) under exactly the Blaze Burner's rules.**

## Decisions
| # | Area | Decision | Source |
|---|---|---|---|
| D1 | Role | Train driver: does the job a Blaze Burner does when it conducts a Create train | user |
| D2 | Form | **Part block**: placed like a block and never moves from its spot, so he can't fall off a train | user |
| D3 | Look | Based on **Trade Bot's look and feel** (Wall-E eye pods, clockwork, muted Create palette, spring-chain motion) | user; [Trade Bot brief](../briefs/trade-bot.md) |
| D4 | Character | More fun and animated than a Blaze Burner | user |
| D5 | Dyeing | Dyed in a **crafting table, one dye per bot** | user |
| D6 | Cost | **Relatively cheap**: he's a Blaze Burner replacement, not a big machine | user |
| D7 | Conductor rules | Create's own rule, unchanged: a conductor block sits **directly behind Train Controls** along the train's length, with the controls facing him; one conductor per direction (front and back for a two-way train); checked at assembly | Create 6.0.10 `CarriageContraption.inControl` |
| D8 | Schedules | Create's own handling: **right-click him on an assembled train with a Schedule** to hand it over; **empty hand** takes it back; right-click resumes a paused schedule; a bot that isn't in control says "not in control" | Create 6.0.10 `ConductorBlockInteractionBehavior` |
| D9 | Hooks | Driving goes through Create's public **`ConductorBlockInteractionBehavior`** API (the same class the Blaze Burner uses), no mixin | Create 6.0.10 API |
| D10 | Mod rules | Mod id without "create"; Carry On pickup needs a BlockItem; works on contraptions and Aeronautics physics objects; looks previewed in Blockbench before the mod | existing mod |
| D11 | Dye house rule | Like Fluid Lines: dyed **as an item**, colour stored on the item and kept when placed and broken; **bulk washing** takes the dye off; dyed names say the colour | existing mod (Fluid Lines D29, P11) |
| D12 | Job scope | **Blaze Burner rules plus train reactions** (throttle, brake, waiting at a station, departure toot, no-path shrug, waving at players), **and he can also be a Stock Keeper** behind a Create 6 Stock Ticker, like a Blaze Burner can | user R1 |
| D13 | Size | **Full Trade Bot size: 1.5 blocks tall** (24 px), the same robot rather than a scaled-down one | user R1 |
| D14 | Recipe | **Crafting table, andesite tier**, shaped: andesite casing (body), electron tube (eyes), cogwheel (clockwork), 2 iron sheets (arms). Cheaper than a Blaze Burner because there's no Nether trip. Proposed grid: `_ T _ / S C S / _ G _` (T tube, S iron sheet, C andesite casing, G cogwheel), makes 1 | user R1 |
| D15 | Name | **Helper Bot** (dyed: "Red Helper Bot" and so on) | user R2 |
| D16 | Dye methods | **Items only** (the house rule): crafting table, Helper Bot + 1 dye → that colour (re-dye the same way); **bulk washing** makes him plain again; Create: Dragons Plus **Bulk Coloring** also dyes him when that mod is present. A placed bot can't be dyed. 16 colours + plain | user R2 |
| D17 | Low roof | When the block above him has collision, **he ducks**: his telescoping neck retracts and his head lowers to fit in one block. He works the same when ducked | user R2 |
| D18 | Power | **None**, no upkeep (like a Blaze Burner) | user R2 (defaults) |
| D19 | Ownership | **None**: anyone can hand him a schedule or take it back, as in Create | user R2 (defaults) |
| D20 | Caps | **Train cap** only while he's in control of a train that has a schedule; **logistics cap** while he's keeping shop (Create's own Blaze Burner rule). If both apply, the train cap wins | user R2, R3 (defaults) |
| D21 | Facing | Placed facing the player. **Turns to face** the Train Controls beside him (re-checked when a neighbour changes). Keeping shop he keeps facing the player, not the Stock Ticker ("no fun if you're looking at his profile"). The wrench rotates him otherwise | user R2 (defaults); shop facing user, 2026-10-27 review |
| D22 | Shape | **Collision 1 block**; **selection box 1.5 blocks** (1 block when ducked) | user R2 (defaults) |
| D23 | Block props | **Pickaxe**, andesite-casing hardness and blast resistance; **stacks to 64**, one stack per colour; not waterloggable; flowing water doesn't break him | user R2, R3 (defaults) |
| D24 | Light | Eyes glow **on the model only**; he gives off no light | user R2 (defaults) |
| D25 | Goggles | Engineer's Goggles show: driving forward or back (or "not in control"), the schedule's name, and status (travelling, waiting, paused, no path, derailed); in a shop, which Stock Ticker he's keeping | user R2 (defaults) |
| D26 | Ponder | Two scenes: **"Driving a train"** (behind the controls, schedule in, empty hand out) and **"Keeping shop"** (beside a Stock Ticker) | user R2 (defaults) |
| D27 | Sound | A quiet voice with a **long cooldown**, because trains are loud; subtitles | user R2 (defaults) |
| D28 | Not in control | No controls in front of him, or a second Helper Bot: he **rides along as a passenger**, looks out of the window, waves | user R3 |
| D29 | Manual driving | While a player drives by hand, he lets go and **folds his arms**. **Create's rule stands**: the schedule stays paused when they get up; he taps the glass and points at the schedule until someone **right-clicks him** to resume | user R3, R4 |
| D30 | Crash | Train crash or derailment: he jolts, goes **dizzy**, then sulks until the train is back on the rails | user R3 |
| D31 | Disassembly | Disassembled at a station, he's a normal block again; the schedule follows Create's rules (back to the player's empty hand, else dropped at the station), as with a Blaze Burner | user R3; Create `StationBlockEntity.dropSchedule` |
| D32 | Off duty | No train and no shop: idle mannerisms, **watches nearby players**, **waves if right-clicked** | user R3 |
| D33 | Hostiles | Hostile mob nearby: eyes widen and he shrinks down (neck in) but never leaves his spot; **mobs ignore him** | user R3 |
| D34 | Weather | Rain on him: wipes his lenses now and then; thunder makes him flinch | user R3 |
| D35 | Shop | Right-click him beside a Stock Ticker to open its shop (as with a Blaze Burner); he's **drawn in the shop screen** (mixin) and **nods when an order is placed** | user R3 |
| D36 | Saving | Only his **colour and facing** are saved (ducking comes from the block above); animations restart on load. Trains running in unloaded chunks are Create's business | user R3 |
| D37 | Other contraptions | On Aeronautics ships and other non-train contraptions he's a **passenger who idles**; flying ships is parked for later | user R3 |

## Open Questions
None for v1.

The look is settled in the [brief](../briefs/helper-bot.md) (2026-10-27): his wheel is swapped for a **swivel post on a round foot plate** (the post telescopes down to duck), and the dye goes on the **body panels and forearms** in the mod's `LineColors.DYES` palette.

Parked for later versions:
- **Later: flying Aeronautics ships** (a sky-pilot job).
- **Later: Blaze Burner extras** such as wearing goggles; v1 matches only the driving and shop jobs.

## Core Loop
1. Craft a Helper Bot (cheap, andesite tier) and, if you like, dye him to match your train.
2. Place him directly behind the Train Controls on your train (or beside a Stock Ticker for a shop). He turns to face the controls; in a shop he faces you.
3. Assemble the train, then right-click him with a Schedule. His train cap pops on and he drives.
4. You watch him work: throttle up out of stations, haul the brake on arrival, check his clock while waiting, toot as he leaves, shrug when there's no path, wave at you on the platform.
5. Take the schedule back with an empty hand, drive by hand while he sulks with folded arms, or right-click him to let him resume.

## Behaviour / Rules
**Jobs**
1. **Driver**: valid when assembled into a train with Train Controls directly in front of him along the train's axis, facing him (D7). Everything about schedules is Create's (D8, D29, D31). He never moves; he's part of the carriage.
2. **Shop keeper**: valid when a Stock Ticker is **horizontally beside him on the same level** (the Blaze Burner's rule). Right-click him to open the shop (D35).
3. **Passenger / off duty**: everything else (D28, D32, D37).

**Animation states on a train** (the art director names the clips; these are the triggers):

| State | Trigger | He does |
|---|---|---|
| Ready | in control, schedule, train still | hands on the controls, small idle mannerisms |
| Throttle up | speed rising | pushes the lever forward, leans in |
| Cruising | steady speed | relaxed, glances out, the occasional wave at a player near the track |
| Braking | speed falling | hauls back, braces |
| Waiting at a station | stopped at a station on a schedule | checks his clock, taps his wheel |
| Departing | starts from a station | toot, little wave |
| Honk | the train honks | pulls the whistle cord |
| No path | the schedule can't find a route | shrug, looks at the map |
| Paused | a player is driving, or the schedule is paused | arms folded; once nobody's at the controls, taps the glass and points at the schedule |
| Crash | derailed or collided | jolts, dizzy, then sulks until re-railed |
| No schedule | in control without one | waits, looks at players expectantly |

**Anywhere**: ducks under a solid block (D17); flinches from hostiles without leaving (D33); wipes his lenses in rain and flinches at thunder (D34).

## Progression
**Early-mid game, available the moment you build trains.** The recipe (andesite casing, electron tube, cogwheel, iron sheets) uses only parts every train builder already has. He unlocks nothing new: he's a friendlier, Nether-free alternative to the Blaze Burner conductor and keeper. Blaze Burners stay as they are.

## Balance
- **Cost:** a little cheaper than a Blaze Burner (no Nether trip, no blaze to catch). That's fine: his job is to be the fun option, not a stronger one.
- **Reward:** exactly what a Blaze Burner gives a train or a shop, nothing more: no speed, fuel or trade bonus.
- **Exploits considered:**
  1. **Dye duplication**: one dye in, one bot out; washing returns only the bot (the dye is lost). Nothing to duplicate.
  2. **Breaking a dyed bot mid-train**: he's a block in the carriage; disassembly places him back with his colour (block-entity data travels with the contraption).
  3. **Ownership griefing**: anyone can take a schedule from him, but that's Create's rule for every conductor (D19). Server owners who want more use claim mods.
  4. **Lag**: no server ticking beyond a lazy neighbour check and a small status sync while on a train. Animation is client-only.
- **Overpowered?** No: it's an exact match for Create's rules.
- **Useless?** No: he's cheaper and needs no Nether trip, and he's a character. Blaze Burners stay, so players can choose either.

## Create Integration
| System | Interaction | Notes |
|---|---|---|
| Trains | **Yes**: conductor | Create's `ConductorBlockInteractionBehavior`; one per direction; schedules handled by Create |
| Train Controls | **Yes** | Must be directly in front of him, facing him (D7); he turns to face them (D21) |
| Stock Ticker / shops | **Yes**: keeper | Mixin on `StockTickerBlockEntity.isKeeperPresent`; opens via `StockTickerInteractionHandler.interactWithLogisticsManagerAt`; drawn in `StockKeeperRequestScreen` (mixin) |
| Stations | Indirect | Disassembly drops or returns the schedule (Create) |
| Contraptions (non-train) | Passenger only | Survives being moved; idles (D37) |
| Aeronautics physics objects | Passenger only | Can't fly ships in v1 |
| Wrench | Yes | Rotates him; sneak-wrench picks him up, as for any Create block |
| Bulk washing | Yes | Removes the dye from Helper Bot items |
| Bulk Coloring (Dragons Plus) | Optional | Dyes and re-dyes items when present |
| Goggles | Yes | D25 |
| Ponder | Yes | D26 |
| Shafts, belts, funnels, deployers, arms, vaults, packages | No | He has no inventory or power. Deployers can't hand him schedules, just as with a Blaze Burner |
| Redstone, comparators, Display Links | No | Create's own train and station sources already cover train status |

## Technical Notes
- **Conductor**: register a `ConductorBlockInteractionBehavior` subclass for the block (`isValidConductor` → always true). Create's assembly (`CarriageContraption.capture`) then collects him like a Blaze Burner. `onScheduleUpdate` isn't needed for the cap: like the burner, the renderer decides from "in control and `cce.hasSchedule()`".
- **Movement behaviour**: a `MovementBehaviour` that renders him on contraptions (Flywheel visual or `renderInContraption`) and works out his state. In-control is computed once per contraption (as `BlazeBurnerMovementBehaviour.determineIfConducting` does) and cached in `context.data`.
- **What the client already knows**: carriage motion (so speed, acceleration and direction can be derived), the synced `SCHEDULED` flag, and the honk (clients play the whistle). **Needs our own sync**: waiting at a station, no path, paused or manual driving, and derailed. Send a small status byte per conductor from the server, only when it changes, to players tracking the carriage.
- **Stock Keeper**: Create 6.0.10 hard-codes keepers. `StockTickerBlockEntity.isKeeperPresent()` accepts an occupied `SeatEntity` (on the ticker's level or one below) or a `HEATER` block entity horizontally beside it on its level. So we need a **mixin there** to also accept a Helper Bot beside the ticker, a **mixin on `StockKeeperRequestScreen`** to draw him (its keeper lookup only knows seated mobs and Blaze Burners), and his own `useItemOn` that calls `StockTickerInteractionHandler.interactWithLogisticsManagerAt`, as `BlazeBurnerBlock` does. "Nods when an order is placed" needs a hook on the order request (server) and a small client event.
- **Block**: horizontal `FACING`, plus a boolean `DUCKED` blockstate updated in `updateShape` from whether the block above has collision. A blockstate, so it's captured as-is on contraptions. Block entity: `colour` (`DyeColor` or none), synced. Collision 1 block; selection 1.5 (1 when ducked).
- **Items**: one `helper_bot` BlockItem with a dye-colour **data component** (as Fluid Lines do), not 17 blocks. Name and tooltip read the colour. Carry On works through the BlockItem. Pick-block keeps the colour.
- **Recipes**: shaped recipe (D14); a shapeless dye recipe for each of the 16 dyes (Helper Bot of any colour + dye → that colour); `create:splashing` → plain; Dragons Plus bulk coloring recipes, loaded conditionally.
- **Data**: loot table (drops himself with his colour), mineable/pickaxe tag, lang (16 dyed names, goggles lines, subtitles, Ponder text), advancement (optional: "Hired Help", hand a schedule to a Helper Bot); Create's `CONDUCTOR` advancement already fires through the API.
- **Animation**: code-driven from Blockbench timings, like Trade Bot (no GeckoLib). Client-only state machine; nothing animation-related is saved (D36).
- **Config (`CMConfig`)**: `helperBotVoiceCooldownSeconds`; `helperBotShopKeeper` (on/off, in case a Create update breaks the mixin).
- **Tests**: game tests for dyeing and washing recipes, colour kept through place/break, ducking under a block, and the conductor flag after assembly (train behind controls; a second bot not in control); a mixin test for `isKeeperPresent`.

# ONE MINUTE UNDERSTANDING
Helper Bot is Trade Bot's cousin who's bolted in place. Put him where you'd put a Blaze Burner (behind a train's controls, or beside a Stock Ticker) and he does that job by exactly Create's rules: hand him a Schedule and he drives; stand him by a ticker and he keeps shop. The difference is that he's a character: he works the throttle and brake, checks his clock at stations, toots, waves, shrugs when lost and sulks after a crash. He's cheap (andesite tier, no Nether trip), 1.5 blocks tall (he ducks under low roofs), and you can dye him any of 16 colours, one dye per bot.

# WHAT IT IS
A cheap, dyeable, animated robot block based on Trade Bot. He does a Blaze Burner's two Create jobs, train conductor and Stock Keeper, under the Blaze Burner's exact rules. He never moves from his block, so he can't fall off.

# WHY IT EXISTS
- Blaze Burner conductors work but are a Nether chore and have little character.
- Trains feel alive with a driver who visibly drives.
- Dye colours let players match bots to train liveries and shops.

# HOW PLAYERS USE IT
- Craft: andesite casing, electron tube, cogwheel, 2 iron sheets.
- Optionally dye him in a crafting table (bot + 1 dye).
- Place him directly behind Train Controls, assemble, right-click with a Schedule.
- Take the schedule back with an empty hand; right-click to resume after driving by hand.
- Or stand him beside a Stock Ticker and right-click him to shop.

# HOW IT FITS CREATE
- Uses Create's own conductor API: identical rules to the Blaze Burner.
- Stock Keeper beside a Stock Ticker, like a Blaze Burner (via a small mixin).
- Bulk washing removes his dye; Dragons Plus Bulk Coloring dyes him.
- Goggles show his direction, schedule and status; two Ponder scenes.
- Rides any contraption safely; drives only trains.

# IMPORTANT RULES
- Drives only when Train Controls are directly in front of him, facing him.
- One driver per direction; extra bots are passengers.
- Schedules: Create's rules (right-click with a Schedule, empty hand to take back).
- Manual driving pauses the schedule; right-click him to resume.
- Keeps shop only beside a Stock Ticker on the same level.
- Train cap while driving with a schedule; logistics cap in a shop.
- No power, no upkeep, no owner.
- Dyed as an item only; a placed bot can't be dyed.
- 1.5 blocks tall; ducks to fit one block under anything solid.
- Gives no bonuses over a Blaze Burner.

# EDGE CASES
- Second bot or no controls: passenger who looks out of the window.
- Player drives: arms folded; afterwards he waits to be right-clicked.
- Derail or crash: dizzy, then sulks until re-railed.
- Disassembled: block again; schedule handled as Create does.
- Solid block above: ducks, still works.
- Hostile mob: shrinks down, never leaves; mobs ignore him.
- Rain or thunder: wipes his lenses; flinches.
- Driving and beside a ticker: the train cap wins.
- Chunk unload: colour and facing saved; animation restarts.
- Aeronautics ship: idles; no flying in v1.

# DEVELOPMENT CHECKLIST
- [ ] Brief (art director): look, "part block" anchoring, duck pose, train and shop caps, dye zones, clip list from the state table
- [ ] Blockbench model and preview clips (preview before the mod)
- [ ] Sounds: quiet voice set, toot, whistle-cord pull, lever clunks; subtitles; long cooldown
- [ ] `HelperBotBlock`: `FACING`, `DUCKED` (`updateShape`), collision 1 / selection 1.5, auto-face controls (not the ticker), wrench rotate, no waterlogging
- [ ] `HelperBotBlockEntity`: colour, synced; save colour only
- [ ] `helper_bot` BlockItem with a dye data component; dyed names and tooltip; Carry On check
- [ ] `ConductorBlockInteractionBehavior` subclass registered for the block
- [ ] `MovementBehaviour` + contraption rendering (Flywheel visual), in-control check cached in `context.data`
- [ ] Server → client status sync (waiting, no path, paused, derailed), sent only on change
- [ ] Client animation state machine (state table above), spring-chain motion like Trade Bot
- [ ] Caps: train cap (in control + schedule), logistics cap (beside a ticker); train cap wins
- [ ] Stock Keeper: mixin `isKeeperPresent`, `useItemOn` → `interactWithLogisticsManagerAt`, shop-screen render mixin, order nod
- [ ] Off-duty behaviour: watch players, wave on right-click; hostile shrink; rain wipe and thunder flinch
- [ ] Goggles tooltip (D25)
- [ ] Recipes: shaped craft, 16 dye recipes, splashing → plain, conditional Dragons Plus bulk coloring
- [ ] Loot table (keeps colour), pickaxe tag, lang, optional "Hired Help" advancement
- [ ] Config: voice cooldown, shop-keeper toggle
- [ ] Ponder: "Driving a train", "Keeping shop"
- [ ] JEI: recipes and dyeing show up
- [ ] Game tests: dye and wash, colour kept, ducking, conductor after assembly, second bot not in control; mixin test for `isKeeperPresent`
- [ ] Dedicated-server test (trains with bots across chunk unloads)

# "IDIOT TEST"
- Helper Bot replaces the Blaze Burner. Same rules.
- Put him right behind the Train Controls.
- Right-click him with a Schedule. He drives.
- Empty hand takes the schedule back.
- Beside a Stock Ticker, he keeps shop.
- He never moves. He can't fall off.
- Crafting table plus one dye recolours him.
- Bulk washing makes him plain again.
- Low roof? He ducks. Still works.
- No power, no owner, no bonuses.

# COMPLETENESS SCORE
Gameplay: 100% · World Integration: 100% · Balance: 95% · Progression: 95% · Automation: 100% · Edge Cases: 90% · **Overall Design Readiness: 90%**

- **Gameplay 100%:** both jobs, every trigger and reaction decided.
- **World Integration 100%:** the "part block" look is settled in the brief (swivel post on a foot plate).
- **Balance 95%:** the recipe grid is proposed, not final; the ingredients are decided.
- **Progression 95%:** tier set; nothing unlocked by design.
- **Automation 100%:** by design there's nothing to automate beyond Create's own schedules.
- **Edge Cases 90%:** the status sync for no path, paused and derailed needs confirming against Create's runtime during implementation; the behaviour is decided.
- **Overall 90%:** ready for the brief and the model; the two Create hooks (keeper mixin, status sync) are the risks to prototype first.
