# Trade Master: Systems Spec

Status: **ready for development**  ·  Related: [Trade Bot spec](trade-bot.md) (v1, built), [Trade Bot brief](../briefs/trade-bot.md), [Helper Bot spec](helper-bot.md) (Stock Ticker precedent)

Name: **Trade Master** (D26).

## Concept
"I want to create an upgraded version of the Trade Bot. This upgraded version should be expensive and use stock links to decide what to trade for. This needs to be well thought out and useful without being too overpowered." (user, 2026-11-04)

"Not stock ticker, the gauges to request the amount of a thing you would want." "I want the bot to make sure there is always 2 stacks of bookshelves, for example." "This would allow us to attach him to multiple output chests, putting emeralds in one, bookshelves in another, all the terracottas in a third (different gauges on one chest)." (user, 2026-11-04)

Clarified: **an expensive, late-game upgrade of the Trade Bot. He tunes into a Create 6 logistics network and fills its Factory Gauges by trading with seated villagers: he buys what a gauge wants and sells network goods for emeralds, so each chest ends up holding what its gauges ask for.**

This is the v1 spec's parked **"v2: Trade Ledger upgrade"** (advanced jobs: buying, specific trades, multiple outputs) and its **"v2 (maybe): Create 6 packages"** (Trade Bot D14, D41).

## How Create's gauges work (6.0.10, verified in sources)
- A **Factory Gauge** has an item and an amount (in items or stacks) and joins a network by tuning.
- **On a Packager** it's a **restocker**: it counts only the inventory that packager is attached to (one chest), and asks the network to keep that chest at the amount. Up to 4 gauges share one gauge block, so one chest can carry several (all the terracottas).
- **Anywhere else** it counts the **whole network's** stock.
- **Wired to input gauges** with an address, it's a factory recipe: it sends the inputs to the address and expects the output back.
- It shows satisfied, promised (on its way) or missing, and remembers promises so it doesn't re-order while things travel.

## Decisions
| # | Area | Decision | Source |
|---|---|---|---|
| D1 | Identity | An **upgraded version of the Trade Bot** | user, 2026-11-04 |
| D2 | Cost | **Expensive** | user, 2026-11-04 |
| D3 | Brain | Uses **Create 6 Stock Links** (the logistics network) **to decide what to trade for** | user, 2026-11-04 |
| D4 | Balance goal | **Useful without being too overpowered** | user, 2026-11-04 |
| D5 | Trade rules | The villager's **real** trades, like a player: stock, restocks, villager XP and levels | Trade Bot D17 (inherited) |
| D6 | Who he trades with | **Seated** villagers and wandering traders only, from up to one block away | Trade Bot D7, D20 (inherited) |
| D7 | Prices | **His own** per-villager reputation: capped at vanilla's limit, never negative; player reputation and Hero of the Village don't apply | Trade Bot D34, D42 (inherited) |
| D8 | Body rules | Rolls and hops (no ladders or bubble columns), avoids water and lava, opens and closes doors and gates, winds down at 0 HP and never dies, scared of hostiles, which ignore him; home at night and in thunderstorms | Trade Bot D4, D18, D24, D26, D28, D29, D32, D60 (inherited) |
| D9 | Station | FE from below; charges him while docked; owner-only; unbreakable except by the owner's wrench; packs into one item with him | Trade Bot D16, D21, D23, D27, D51 (inherited) |
| D10 | Loading | Works whenever his area is loaded (chunk loaders included), owner online or not; saves and resumes on unload; never deletes items | Trade Bot D33, D50, D53 (inherited) |
| D11 | Trade XP | No orbs; XP banked and delivered as Nuggets of Experience | Trade Bot D57 (inherited) |
| D12 | Create network rules | A network's frequency is shared by tuning (right-click an existing logistics block); an owner-locked network only accepts players allowed in; a link with a **full-strength redstone signal** drops out of the network | Create 6.0.10 `LogisticallyLinkedBehaviour`, `LogisticsManager` |
| D13 | Mod rules | Mod id without "create"; Carry On pickup needs a BlockItem; looks previewed in Blockbench first; agree every choice with the user | existing mod |
| D14 | Wants | **Create Factory Gauges** say what he trades for. **Not** the Stock Ticker, and no custom rules screen | user, 2026-11-04 |
| D15 | Gauge meaning | A gauge is a **floor**: "always 2 stacks of bookshelves" means he trades whenever there are fewer than 128, in the gauge's own units | user, 2026-11-04 |
| D16 | Form | **Made by upgrading a Trade Bot:** a new robot with an upgraded look; the recipe uses up a Trade Bot and **keeps that bot's villager reputation** | user R1 |
| D17 | Direction | He **buys and sells**, and can sell **anything in the network** | user R1 |
| D18 | Network link | **Built-in Stock Link:** his station is a network node, tuned like a Stock Link; no separate Packager or Stock Link needed at the station | user R1 |
| D19 | Which gauges | **Gauges on his network.** *Refined by D31 (R4): only gauges on Trade Hatches count* | user R1, R4 |
| D20 | Multiple outputs | **Each chest's gauges choose what ends up in it:** gauges on that chest's **Trade Hatch** (D31; first proposed as Packager restocker gauges) (one chest: emeralds; another: bookshelves; a third: several terracotta gauges). Emeralds are just another wanted item | user R1 |
| D21 | Carrying | **By hand.** The network tells him where everything is; he rolls to the chest holding what he needs (goods to sell, emeralds to spend), takes it, trades, and puts purchases into the chest whose gauge wanted them. *Refined by D40: every chest he uses has a Trade Hatch.* Only chests and villagers **within his range** (D8) | user R2 |
| D22 | When he sells | **Only when emeralds are needed:** an unmet emerald gauge, or a purchase for another gauge that the network's emeralds can't cover. Nothing is sold just because it's there | user R2 |
| D23 | What he may sell | **Gauges are the only protection:** he never sells an item below a gauge's amount for it; anything else in the network is fair game. **The network design is the player's control:** keep what you don't want sold off his network (e.g. whisk it away by Packager to another network). *Refined by D40: keep it in a chest without a Trade Hatch; he never sees those.* | user R2 |
| D24 | When he steps in | **The way Create does it:** he acts on a gauge only when Create shows it **unmet** (neither satisfied nor promised). *Refined by D35:* he fetches owned stock first, since Create can't restock hatch chests. His trips register as promises, so gauges don't double-order. Set up the network correctly and it just works | user R2 |
| D25 | Look | He has a **beard** (the Helper Bot has a moustache), on top of Trade Bot's family parts. For the art brief | user, 2026-11-04 |
| D26 | Name | **Trade Master** | user R3 |
| D27 | Recipe | **5×5 mechanical crafting around a Trade Bot**, plus a new **Trade Ledger** component (sequenced assembly: book → precision mechanism → emerald block → electron tube, pressed), a Stock Link, 2 Factory Gauges, 4 Netherite Sheets and brass. **Grid (approved 2026-11-04):** `NSKSN / SPLPS / GETEG / SPCPS / NSSSN` (T Trade Bot, L Trade Ledger, K Stock Link, G Factory Gauge ×2, P precision mechanism ×4, E emerald block ×2, C brass casing, N Netherite Sheet ×4 (tag `c:plates/netherite`), S brass sheet ×9). **Ledger (approved):** book → deploy precision mechanism → deploy emerald block → deploy electron tube → press; 1 loop, no scrap | user R3; grid, Ledger and netherite corners user 2026-11-04 |
| D28 | Stats | **Same body, smarter brain:** v1's 64-block range, 27-stack load, speed, charge and drain (Trade Bot D28, D45, D49) | user R3 |
| D29 | Cap | **Up to 2 Trade Masters per player**, separate from the 4 Trade Bots; upgrading a Trade Bot frees its slot (config `tradeMastersPerPlayer`) | user R3 |
| D30 | Leftovers | **Overshoot goes into the chest that wanted it** (a gauge is a floor). Spare emeralds and unsold goods go **back to the chest he took them from**, else any hatch chest in range with room, else he keeps them. Nuggets of Experience go to a chest with a **Nugget of Experience gauge**, else he banks them until there is one | user R4 |
| D31 | Gauge mounts (refines D19, D20) | Gauges count for him **only when they sit on a new Trade Master chest attachment** (working name **Trade Hatch**), which fits the theme better than Create's Packager. **Gauges anywhere else are ignored.** So every gauge he serves belongs to one chest | user R4 |
| D32 | Prices | **Never above the base price:** he skips an offer while demand has pushed it above its base price (after his reputation discount) and retries after a restock | user R4 |
| D33 | Defaults | Proposed defaults **P1–P15** below, approved together | user R4 |
| D34 | Hatch role | **Gauge mount, delivery point and source:** fits on any container face like a Packager, tuned to his network; gauges on its free faces count only that container (4 per face). He delivers there and may also take from it (spare emeralds, items above its gauges). Create's logistics doesn't use it; add a Packager to the same chest for that | user R5 |
| D35 | Owned stock first (refines D24) | When a hatch gauge is unmet and another hatch chest holds the item (D40), he **fetches it by hand first** and trades only for the shortfall | user R5 |
| D36 | Reserves (extends D23) | **He never takes anything out of a chest below a gauge's amount for it, to sell or to spend.** An emerald gauge is a savings floor; once emeralds are at it, he sells goods to fund purchases | user R5 |
| D37 | Hatch defaults | Trade Hatch defaults **H1–H9** below, approved together; hatches must be **cheap** | user R5, 2026-11-04 |
| D38 | Netherite Sheet | A **new Netherite Sheet** item: **1 netherite ingot in a Mechanical Press → 1 sheet**. Tagged `c:plates/netherite`, and the Trade Master recipe uses the tag, so other mods' netherite sheets work too. 4 of them make the corners of his grid (16 ancient debris). Texture previewed before it goes in the mod | user, 2026-11-04 |
| D39 | Look and motion | **Trade Bot + changes; the same animations and sounds.** Changes: a **brass whisker-grille beard**; **netherite** body frame edges and lid plates (brass kept); a **Stock Link aerial** bulb; a **monocle**; a **ledger** on his back. Brief: [trade-master.md](../briefs/trade-master.md) | user, 2026-11-04 |
| D40 | Inputs (refines D21, D23, D30, D34, D35) | **Inputs use Trade Hatches too; no Packagers required.** He only sees and uses chests with a Trade Hatch tuned to him. A hatch with **no gauges is an input** (he sells and spends from it); a hatch **with gauges is an output** he fills, and draws from only above its gauges. **A chest without a hatch is invisible to him**, which is how you keep things from being sold | user, 2026-11-04 |

## Approved defaults (D33)
P1. **Tuning:** as Create's logistics items: right-click a Stock Link (or any block on the network) with the Trade Master item, then place him. A locked network only lets in players allowed to use it.
P2. **Station screen:** the network he's on, the gauges he's working on, gauges he can't fill (and why), charge, and **Call home**. No container links: the Trade Master works only through his network.
P3. **Can't-fill feedback:** a gauge whose item no seated villager in range sells, or whose chest is out of range, is listed as "nobody sells this" or "out of range" on the screen and in goggles. He never wanders off for it.
P4. **Priority:** the emptiest gauge first (by share of its amount); the cheapest seller at his prices, then the nearest; all of a villager's useful trades in one visit (Trade Bot D63).
P5. **Selling choice:** when he needs emeralds, he sells what his hatch chests hold the **most** of (that isn't held up by a gauge), so scarce items go last.
P6. **Two-item trades:** if a trade also costs an item (a book for an enchanted book), he fetches that from his hatch chests too; if it's missing, he skips the trade.
P7. **Exact matches:** items match exactly as Create's gauges do, so a "Mending book" gauge works.
P8. **Several Trade Masters on one network:** first come, first served per gauge; the others pick other gauges.
P9. **Redstone:** a full-strength signal on the station takes him off the network and he stays home (Create's link rule plus v1's pause). Comparator as v1: 0 / 5 / 10 / 15.
P10. **The upgrade carries over** the Trade Bot's owner, reputation and charge; the recipe takes a packed Trade Bot item.
P11. **Goggles:** network connected or not, the current task, the gauge he's serving, charge.
P12. **Ponder:** two scenes: "Joining a network" (tuning, the station) and "Filling gauges" (buying, selling for an emerald gauge, leftovers).
P13. **Sounds:** *changed 2026-11-04:* **the same sounds as Trade Bot** (user: "the animations and noises should be the same").
P14. **Advancement:** "Trade Master": he fills a gauge for the first time.
P15. **Display Link source:** parked for a later version (what he's buying, on a display board).

## Trade Hatch defaults (D37)
H1. **Name:** Trade Hatch.
H2. **Placement:** against any item container (chest, barrel, item vault, the mod's upgraded chests), facing it; several per container allowed.
H3. **Tuning:** like Create's logistics items: right-click his station or any block on his network with the hatch, then place.
H4. **Recipe:** cheap, because you need several: crafting table, andesite casing + brass sheet + electron tube, makes 1. Layout (approved 2026-11-04): shaped, middle column `T / A / S` (electron tube over andesite casing over brass sheet). The user asked that hatches be **cheap**.
H5. **Access:** follows the network's lock; only allowed players can tune or break it.
H6. **Breaking:** drops itself; gauges on it pop off, as on any block.
H7. **Range:** a hatch more than 64 blocks from his station counts as "out of range" (P3).
H8. **Goggles:** its container, network, gauges and their status.
H9. **Look:** Trade Master family (brass, a slotted flap that opens when he delivers), previewed in Blockbench first.

## Open Questions
None for this version.

Parked for later versions (agreed):
- **Display Link source** showing what he's buying (P15).
- **Riding** trains, minecarts and seats (Trade Bot parked list).

## Core Loop
1. **Upgrade** a Trade Bot into a Trade Master on a 5×5 mechanical crafter with a Trade Ledger (D27). He keeps the Trade Bot's reputation (D16, P10).
2. **Tune** him to a Create logistics network (right-click a Stock Link with the item), then place his station and power it with FE from below (P1, D9).
3. **Put Trade Hatches** on the chests near him that he should take from (inputs: no gauges) and fill (outputs), tuned to him; no Packagers needed (D40). **Put Factory Gauges on the output hatches**: "2 stacks of bookshelves" on one, "4 stacks of emeralds" on another, a terracotta gauge for each colour on a third (D20, D31, D37).
4. When a hatch gauge is unmet, he **fetches what his other hatch chests already hold** first (D35). For the rest, he takes emeralds above any emerald gauge (D36), or **sells goods from his hatch chests** if the emeralds can't cover it (D22), **buys from seated villagers** at no more than base price (D32), and **puts the purchases into that hatch's chest** (D21).
5. He returns leftovers where they came from, goes home, charges, and repeats as restocks allow (D30, Trade Bot loop).

## Behaviour / Rules
**What he knows.** Through his built-in Stock Link (D18) he knows every chest with a **Trade Hatch** tuned to him (D40), and what each holds; Packagers aren't needed and chests without a hatch don't exist for him. Hatches without gauges are inputs; hatches with gauges are outputs he fills and draws from only above their gauges. He reaches only those within 64 blocks of his station (D21, D28, H7).

**What he works on.** Only gauges on Trade Hatches tuned to his network (D31). A gauge is **unmet** when its hatch chest holds less than its amount, counting what he's already carrying for it as promised (D24). Other gauges are ignored.

**Picking a job** (checked while docked, as v1 D47 plus the network):
1. Skip if it's night or a thunderstorm, he's paused, he's off the network (P9), or he's short of charge.
2. List the unmet gauges in range. Skip any another Trade Master has claimed (P8). Drop any whose item no seated villager in range sells *and* the network doesn't hold: these show as "nobody sells this" (P3).
3. Take the emptiest gauge first, by share of its amount (P4).

**Filling a gauge:**
1. **Courier first** (D35): if other hatch chests in range hold the item above their own gauges (D36), he fetches it and delivers it. Done if that meets the gauge.
2. **Price it:** find seated sellers in range whose offer, at his reputation and **not above base price** (D32), gives the item. Cheapest first, then nearest (P4). If a trade also needs an item (a book for an enchanted book), that comes from the network too, or the trade is skipped (P6).
3. **Pay:** take emeralds from hatch chests, only above any emerald gauge's amount (D36).
4. **Raise emeralds if short** (D22): sell what his hatch chests hold the most of, above their gauges (D23, D36, P5), to seated buyers in range, until he can pay. An unmet **emerald gauge** is filled the same way, with the emeralds delivered to its hatch chest.
5. **Trade:** one visit per villager, every useful trade in one animation (Trade Bot D63). Load 27 stacks plus the emerald pouch (D28).
6. **Deliver:** everything bought goes into the gauge's hatch chest, overshoot included (D30). The hatch flap opens (H9).
7. **Tidy up** (D30): spare emeralds and unsold goods go back where he took them; Nuggets of Experience go to a hatch chest with a Nugget gauge, else he banks them.

**Never** (the protection rules): he never takes anything out of a chest below a gauge's amount for it, to sell or to spend (D23, D36). He never pays above base price (D32). He never trades with a villager who isn't seated (D6). Anything you don't want sold, keep in a chest without a hatch (D23, D40).

**Body, home and interrupts:** all as v1: night, storms, hostiles (scared routine), damage and wind-down, water, lava and void rescue, doors, players' priority at a villager, interrupted trades returned safely (D8, D9, D10; Trade Bot D46, D54, D55, D59).

## Progression
**Late game.** Before him you need a working Trade Bot (mid-late: brass, precision mechanisms, FE), a Create 6 logistics network (Stock Links, Factory Gauges), and a trading hall of seated villagers. The upgrade (D27) costs a Trade Bot, roughly twice that again in Create parts, and 4 Netherite Sheets (D38), so it needs a Nether expedition for ancient debris. He unlocks **hands-free buying**: a base that keeps itself stocked with anything villagers sell (bookshelves, glass, enchanted books, Bottles o' Enchanting), paid for by its own surplus. Trade Hatches stay cheap (H4, user) so the expense is the brain, not the plumbing.

## Balance
- **Cost:** an expensive 5×5 upgrade that uses up a Trade Bot (D27), FE upkeep, a logistics network, seated villagers, Trade Hatches on each chest he fills.
- **Reward:** automated buying and selling at real, base-or-better villager prices.
- **Limits that keep him fair:** real stock and restocks (D5); never above base price (D32); a 64-block reach for chests and villagers (D28); 27 stacks per trip; one trade animation per villager (Trade Bot D63); charge per block and per trade; 2 per player (D29).
- **Exploits considered:**
  1. **Buy-sell loops and arbitrage:** he sells an item only above its gauges, and buys it only below its gauge, so he can't both buy and sell it (D23, D36). He sells only when emeralds are needed (D22), so nothing churns.
  2. **Emerald drain:** the base-price rule (D32) and emerald gauges as a savings floor (D36).
  3. **Discount farming:** his own capped, never-negative reputation (D7). Hero of the Village and zombie curing don't apply.
  4. **Infinite XP** (Bottles o' Enchanting, trade XP nuggets): capped by restocks, as for a player.
  5. **Selling your valuables:** by design, the network is the protection (D23). Ponder and the station screen must make that obvious.
  6. **Duplication** (unload, packing, a hatch broken mid-trip): v1's saved state and never-delete rules (D10); leftovers routed back (D30).
  7. **Other players' networks:** Create's network lock (P1, H5).
  8. **Server load:** 2 per player, a 64-block reach, gauge checks only while docked, and a cached list of hatches.
- **Overpowered check:** he does what a player at a trading hall does, no faster than restocks allow, and spends real goods to do it. Mending books and diamond gear are vanilla-obtainable at these prices. The strongest case is many seated master villagers plus a big surplus farm: that took late-game investment.
- **Useless check:** nothing in Create buys from villagers, and Create's gauges can't make items that don't exist in the network. He's the only way a gauge gets filled from villagers.

## Create Integration
| System | Interaction | Notes |
|---|---|---|
| Stock Links / logistics network | **His brain:** built-in link; ties him to his Trade Hatches and gauges | D18, P1, D40 |
| Factory Gauges | **His orders**, but only on Trade Hatches | D14, D15, D31 |
| Packagers | **Not needed** (D40). He only uses hatch chests; a Packager on a hatch chest still lets Create move items in and out | D40 |
| Trade Hatch (new) | Gauge mount, delivery point and source on any container | D34, D37 |
| Packages, frogports, postboxes, trains | **No:** he carries by hand. A Packager on the same chest lets Create move items in and out as usual | D21, D34 |
| Network lock | Only allowed players can tune him or hatches | P1, H5 |
| Mechanical crafters, sequenced assembly | His recipe and the Trade Ledger | D27 |
| Electricity / FE | Station power, as v1 | D9 |
| Seats (+ contraption seats) | Where sellers and buyers sit | D6 |
| Nuggets of Experience | Trade XP; delivered to a Nugget gauge's hatch chest | D11, D30 |
| Vaults, barrels, the mod's upgraded chests | Valid hatch containers | H2 |
| Redstone, comparators | Full signal: off the network, stays home; comparator 0 / 5 / 10 / 15 | P9 |
| Engineer's Goggles | Station (P11) and hatch (H8) | |
| Ponder | "Joining a network", "Filling gauges" | P12 |
| Display Link | Parked for later | P15 |
| Deployers, mechanical arms | No (as v1) | Trade Bot D37 |
| Stock Ticker | Not used for his orders | D14 |

## Technical Notes
- **Entity:** `TradeMasterEntity`. Reuse `TradeBotEntity`'s brain, motion, clips, trading (`TradeBotCustomer`, `BotPricing`, `VillagerClaims`) and saving; swap the job source (container links) for a network job source. The new model (beard, D25) comes from the art brief.
- **Station:** a Trade Master variant of the Trade Bot station. It stores the network `freqId` and exposes it, so hatches and Create items can tune from it. It doesn't need to register as a `LogisticallyLinkedBehaviour` link. If it does, its summary must be empty, so Create never routes requests to it.
- **Finding chests:** a server-side registry of Trade Hatches per frequency (no Packager lookups, D40); each hatch knows its container and its gauges. Read only loaded chunks; skip and retry the rest (Trade Bot D55).
- **Gauges on hatches:** a Factory Gauge on a Trade Hatch must count only the hatch's container and never trigger Create's own requests. Mixin `FactoryPanelBehaviour.getRelevantSummary` (and `getPromised`) to return the hatch container's summary and his promises when the panel's attached block is a Trade Hatch. Leave `restocker` false, and skip `tickRequests` for hatch-mounted gauges, so Create sends nothing. The gauge then shows satisfied, promised or missing natively.
- **Promises:** his claimed trips register as promises on the gauge, so status and other Trade Masters see them (D24, P8).
- **Price rule (D32):** compare the offer's cost after his reputation against its base cost before demand (`MerchantOffer.getBaseCostA` vs special price and demand).
- **Reserve (D36):** available(item, chest) = count − the largest gauge amount on that chest for that item. Network-wide available = the sum over reachable chests.
- **Saving:** v1's fields plus `freqId`, claimed gauge positions, the promises in flight, and the banked XP.
- **Config (`CMConfig`, server):** `tradeMastersPerPlayer` (2). His range, charge and drain reuse the Trade Bot values (D28) unless separate keys are wanted later.
- **Data:** Trade Master item and entity, Trade Master station, Trade Hatch block and item, Trade Ledger and its incomplete item; recipes (5×5 mechanical crafting, sequenced assembly, hatch crafting); lang and subtitles; the advancement (P14); Carry On blacklist tag; Ponder scenes.
- **Tests:** unit tests for gauge selection, the reserve maths, the base-price rule, leftover routing and the sell choice. Game tests for a full loop: hatch gauge → courier → sell → buy → deliver. Write each test to fail first.

# ONE MINUTE UNDERSTANDING
The Trade Master is a bearded, upgraded Trade Bot who keeps your base stocked by trading with villagers. Tune him to a Create logistics network, put cheap Trade Hatches on the chests he takes from and the ones he fills (no Packagers needed), and stick Factory Gauges on the hatches he fills: "always 2 stacks of bookshelves here", "4 stacks of emeralds there". When a gauge runs low he first fetches what your other hatch chests already hold. Then he buys the rest from seated villagers, never above base price, paying with spare emeralds or by selling surplus from the network. He carries everything by hand and puts it in the right chest. Gauges protect what they hold; anything else in a hatch chest is his to sell; chests without a hatch he never touches.

# WHAT IT IS
A late-game upgrade of the Trade Bot that reads a Create 6 logistics network and fills Factory Gauges by trading with seated villagers. He buys what gauges want and sells surplus for emeralds. He carries it all by hand, between chests fitted with Trade Hatches.

# WHY IT EXISTS
- Create's gauges can't create items the network doesn't have; villagers can.
- Buying was the Trade Bot's missing half (his parked "Trade Ledger" upgrade).
- A late-game goal that turns a trading hall into a self-running shop.

# HOW PLAYERS USE IT
- Upgrade a Trade Bot on a 5×5 crafter with a Trade Ledger.
- Tune him to your network; place and power his station.
- Put Trade Hatches on the chests he takes from and fills; Factory Gauges on the ones he fills.
- Seat villagers who sell what you want (and buy what you have spare).
- Chests without a hatch are invisible to him: keep what you don't want sold there.

# HOW IT FITS CREATE
- Factory Gauges are his order sheet, natively showing satisfied, promised and missing.
- A built-in Stock Link ties him to his hatches and gauges; no Packagers needed.
- Trade Hatches sit on containers the way Packagers do.
- Mechanical crafting and sequenced assembly make him; FE powers him.
- Respects network locks and the link's redstone rule.

# IMPORTANT RULES
- Only gauges on Trade Hatches count; all others are ignored.
- A gauge is a floor: he trades when the chest holds less.
- Fetches what his hatch chests already hold before trading.
- Never takes anything below a gauge's amount, to sell or to spend.
- Sells only when emeralds are needed; sells the most plentiful thing first.
- Never pays above a villager's base price.
- Seated villagers only; real stock, restocks and villager XP.
- Carries everything by hand within 64 blocks; 27 stacks per trip.
- Up to 2 per player; keeps the Trade Bot's reputation.
- Everything else (home, night, storms, fear, wind-down) as Trade Bot.

# EDGE CASES
- Nobody in range sells the item: gauge shown as "nobody sells this".
- Hatch chest out of range: "out of range"; he never goes.
- Network out of emeralds and nothing sellable: he waits; gauge stays missing.
- Trade gives more than needed: the extra goes in the gauge's chest anyway.
- Hatch or gauge removed mid-trip: goods go back where they came from.
- Two Trade Masters, one gauge: first claims it; the other picks another.
- Demand pushed the price up: he skips until a restock.
- A Nugget of Experience gauge missing: he banks the XP until there is one.
- Station on full redstone: off the network, stays home.
- Two-item trade with the item missing: he skips that trade.

# DEVELOPMENT CHECKLIST
- [ ] `TradeMasterEntity` reusing the Trade Bot brain, motion, clips, trading and saving; new model and renderer (beard)
- [ ] Trade Master station variant: network `freqId`, tuning, FE, screen (network, gauges in work, can't-fill list, charge, Call home)
- [ ] Item: the Trade Master on his station; tuning on right-click of logistics blocks (P1); locked-network check
- [ ] Upgrade recipe: 5×5 mechanical crafting taking a packed Trade Bot, carrying owner, reputation and charge over (P10)
- [ ] Trade Ledger item, incomplete item and sequenced-assembly recipe
- [ ] Netherite Sheet item (texture previewed first), Mechanical Press recipe, `c:plates/netherite` tag
- [ ] Trade Hatch block, block entity, item, model (flap animation), tuning, network lock, goggles; cheap crafting recipe
- [ ] Mixin: gauges on hatches count only the hatch container, use his promises, and send no Create requests
- [ ] Hatch reader: the Trade Hatch registry for his frequency (inputs without gauges, outputs with); loaded chunks and range only
- [ ] Job picker: unmet hatch gauges, claims, emptiest first, can't-fill reasons
- [ ] Courier step: fetch owned stock first, respecting reserves
- [ ] Pricing: sellers by his price, base-price cap, two-item trades
- [ ] Selling for emeralds: most plentiful item above its gauges; emerald gauges
- [ ] Reserve rule (never below a gauge) in every take
- [ ] Delivery into hatch chests, overshoot allowed; leftovers routed back; nuggets to a Nugget gauge or banked
- [ ] Promises registered and cleared (trip done, aborted, unloaded)
- [ ] Redstone: full signal off network; comparator 0 / 5 / 10 / 15
- [ ] Goggles for station and hatch
- [ ] Config `tradeMastersPerPlayer` (2); per-player cap saved data
- [ ] Advancement "Trade Master"
- [ ] Ponder: "Joining a network", "Filling gauges"
- [ ] Sounds: Trade Bot set, deeper (sound designer); lang and subtitles
- [ ] Carry On blacklist tag
- [ ] Unit tests (selection, reserve, price rule, leftovers, sell choice) and game tests (full loop), each proven to fail first

# "IDIOT TEST"
- Upgraded Trade Bot with a beard; costs a lot.
- Tune him to a Create logistics network.
- Put Trade Hatches on chests. They're cheap.
- Put Factory Gauges on hatches: "keep N here".
- Low gauge: fetch owned stock, then buy the rest.
- Pays with spare emeralds, or sells surplus first.
- Never sells or spends below any gauge.
- Never pays above base price; restocks still limit him.
- Carries by hand, 64 blocks, seated villagers only.
- No hatch on a chest? He never touches it.

# COMPLETENESS SCORE
Gameplay: 95% · World Integration: 90% · Balance: 85% · Progression: 90% · Automation: 92% · Edge Cases: 92% · **Overall Design Readiness: 85%**
- Gameplay 95%: every decision is made; the Display Link source is parked by choice.
- World Integration 90%: the gauge-on-hatch mixin and tuning-from-station need a proof in code against Create 6.0.10.
- Balance 85%: the recipes are set; the courier trips' charge cost needs a playtest.
- Progression 90%: the Trade Ledger's sequence and the netherite cost need checking in JEI and playtest.
- Automation 92%: no packages by choice (D21); a Packager on the same chest bridges to Create logistics.
- Edge Cases 92%: hatches on moving contraptions and gauges removed mid-trip need game tests.
- **Overall 85%** (the lowest area, balance): ready to build, with numbers to tune in playtesting.
