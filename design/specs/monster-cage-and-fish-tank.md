# Monster Cage & Fish Tank: Systems Spec

Status: ready for development, both blocks (looks to be designed)  ·  Related: [Animal Pen dossier](../dossiers/animal-pen.md), [Animal Pen brief](../briefs/animal-pen.md), briefs [Monster Cage](../briefs/monster-cage.md) and [Fish Tank](../briefs/fish-tank.md), quick read `monster-cage-and-fish-tank.page.html`

## Concept

The user (2026-10-27): "I want to create a new block for this mod pack, has to be complementary, useful, not too overpowered, have a good cost to reward ratio and be Create mod themed." From the shortlist they chose the **Monster Cage**, at the early–mid tier.

Clarified: the Animal Pen's counterpart for hostile mobs. Catch one monster in a Capture Bottle, keep it in a sealed Create-style box, and harvest its drops (string, bones, gunpowder, slime, rotten flesh) without building a mob farm. Its drops feed the String Tangler and the rest of a Create base.

## Decisions

| # | Area | Decision | Source |
|---|---|---|---|
| D1 | What it is | A block that keeps one kind of captured hostile mob and turns it into a steady supply of its drops: the Animal Pen's counterpart for monsters | user, round 1 |
| D2 | Tier | Early–mid: an Andesite cage first, a Brass cage later (like the Cobblestone Generators and Animal Pens) | user, round 1 |
| D3 | Goals | Complementary, useful, not overpowered, a good cost-to-reward ratio, Create-themed | user, brief request |
| D4 | Family | Behaves like the pens unless a decision says otherwise: shaft into the back only, no stress while empty or idle, items out of every face, wrench screen, comparator, kept when moved by pistons or contraptions. (Speed: see D19) | existing mod rule (Animal Pen dossier) |
| D5 | Screens | The wrench screen uses the Create-style GUI kit and the quick-move buttons and mouse wheel on its output | existing mod rule (GUI kit, quick moves) |
| D6 | Look | Dark, muted, Create-like; previewed in Blockbench before it goes into the mod | user's standing art rules |
| D7 | Carry On | A plain BlockItem, so Carry On can pick it up; the mod id stays free of "create" | existing mod rule |
| D8 | Core loop | **Horde + sword**: one bottled monster starts it; the cage brings in more of the same kind over time, up to a cap; a sword or a Deployer with a sword culls half (always leaving one), with the mob's real loot table, Looting, Fire Aspect and XP, as the pens do | user, round 2 |
| D9 | Upkeep | Each new monster costs **Monster Chow** (an item), a running cost like the pens' food; automatable; the drops can't pay for themselves | user, round 2 |
| D10 | Tiers and monsters | Split by tier: **Andesite** cage for common Overworld monsters; **Brass** cage adds Nether and End monsters; a third, **Netherite** cage, expensive and made with Mechanical Crafters, for the Wither | user, round 2 ("Split by tier, add a netherite one that is expensive and required mechanical crafters for the wither") |
| D11 | Catching | Monsters are caught in **jars**, not the animal Capture Bottle: an **Iron Jar** for regular monsters, a **Netherite Jar** for the Wither | user, round 2; round 3 ("Iron Jars for regular mobs, Netherite Jars for the Wither") |
| D12 | The Wither | The **Netherite Monster Cage** holds the **Wither boss**, caught in a Netherite Jar. It needs **both rotation (SU) and Forge Energy (FE)** to contain and farm him | user, round 3 |
| D13 | Rare mobs | Evoker, vindicator, ravager, shulker and elder guardian go in the **Brass** cage with the Nether and End monsters. The Warden and the Ender Dragon are never caged | user, round 3; Warden and Dragon carried over from the round 2 recommendation |
| D14 | Chow | A **new item, Monster Chow** (first called a lure) made from cheap things you get from **milling**; monsters from higher tiers need **more chow** for each new one | user, round 3 |
| D15 | Containment | The Netherite cage's rotation runs the farm and its FE holds a containment field. With FE short, a klaxon sounds and a **5-minute countdown** (revised from 30 s, D74) shows on goggles, the comparator and Display Links and is announced to players nearby (D75); then **one Wither breaks out** at full health and the rest stay in. A server config option turns escapes off (the cage just stops) | user, round 4 |
| D16 | Netherite cage contents | **Only the Wither.** The Brass cage stays the best cage for everything else | user, round 4 |
| D17 | Wither cost | Each new Wither costs **chow only** (no skulls or soul sand). Nether Stars become cheaper than in vanilla, so the chow count, the rate, the cap and the FE upkeep are what limit them | user, round 4 |
| D18 | Growth | **Steady rate**: one new monster per interval while there's chow and room, however many are already in | user, round 4 |
| D19 | Speed | **Rotation speed affects the rates, and speeds up consumption too**: faster shafts bring in monsters sooner and burn chow (and upkeep) faster. This replaces the pens' "fixed stress, no faster at higher speed" rule from D4 | user, mid round 5 (2026-10-27: "Rotation speed should affect rates but it also speeds up consumption") |

| D20 | Netherite FE | **Split**: the field costs a **flat, high-ish amount** whenever the cage holds any Wither, at any speed and however many it holds (the field never sleeps); farming adds more on top, scaling with speed. Slowing down saves power but never drops the field. (Amounts: what-if round 2) | user, round 5; revised in what-if round 1 ("The field should be a flat cost, but high-ish, not scale with amount") |
| D21 | Wither jar | A Wither in a Netherite Jar can be let out **only into a Netherite cage**, never into the world | user, round 5 |
| D22 | Wither catch | The Netherite Jar catches a Wither at **any health**, any time after it spawns | user, round 5 |
| D23 | Numbers | Andesite: holds 64, a monster every 20 s × chow cost at 64 RPM, 4 SU/RPM. Brass: holds 256, 10 s × chow cost at 64 RPM, 16 SU/RPM. Netherite: holds 3 Withers, one every 20 min at 256 RPM (80 min at 64), 16 SU/RPM, 100,000 FE buffer. Rates are linear with speed (1–256 RPM). Chow slots / output slots: 2/9, 8/18, 8/9. All in the config | defaults approved, round 5 (with D19) |
| D24 | Chow cost | Interval = base × chow. 1 Monster Chow: Overworld monsters. 2: Nether, End, ocean and pillager-type monsters, and modded monsters by default. 4: rare (evoker, ravager, shulker, elder guardian). 64: the Wither (at its own interval) | defaults approved, round 5 |
| D25 | Monster Chow | Create Mixer, no heat: 1 Wheat Flour + 1 Bone Meal + 1 Cinder Flour → 3 Monster Chow | defaults approved, round 5 |
| D26 | Tags | Entity tags `monster_cage/andesite`, `/brass`, `/rare`, `/netherite`, `/blacklist`. Bosses other than the Wither, the Warden, the Giant and the Illusioner are blacklisted | defaults approved, round 5 |
| D27 | Copies | New monsters arrive bare: no armour, weapons or held items, only loot-table drops (one exception: drowned tridents, D60). Slimes and magma cubes drop as their smallest size that drops anything | defaults approved, round 5 |
| D28 | XP | A player's sword gets the vanilla XP; a Deployer's cull gives none (as the pens) | defaults approved, round 5 |
| D29 | Breaking | Drops the cage, the jar (with one monster), the chow, the output, and the other monsters culled into drops (Withers into Nether Stars). Taking the jar out in the wrench screen does the same. The Netherite cage is blast-proof | defaults approved, round 5 |
| D30 | Unloaded, Peaceful | Nothing grows while unloaded (as the pens). On Peaceful the cages hold but don't grow, Iron Jars won't let monsters out into the world, and no Wither escapes | defaults approved, round 5 |
| D31 | Moving | On a contraption a cage rides along without working, and the Netherite one doesn't count down. Andesite and Brass can be carried with Carry On; the Netherite cage can't | defaults approved, round 5 |
| D32 | Jars | **Iron Jar**: a Deployer puts an Iron Sheet on a Glass Bottle (a one-step sequenced recipe). Catches any monster a cage takes except the Wither, at any health; emptying it gives the empty jar back. **Netherite Jar**: smithing, Netherite Upgrade template + Iron Jar + Netherite Ingot; fireproof | defaults approved, round 5 |
| D33 | Recipes | Andesite Monster Cage: andesite alloy corners, 3 iron bars, a chain, Andesite Casing. Brass: 5×5 Mechanical Crafting around an Andesite cage with brass sheets, Brass Casing, electron tubes, iron bars. Netherite: Mechanical Crafting around a Brass cage with 4 netherite ingots, 4 Precision Mechanisms, 2 FE Coils and obsidian | defaults approved, round 5 |
| D34 | Info | Goggles: horde n/cap, status (needs rotation, overstressed, needs chow, full, next one n%), chow cost and time per monster; Netherite adds FE stored, FE/t and the containment state. Comparator: how full of monsters (1–15). Display Links read the Netherite cage's containment state and countdown | defaults approved, round 5 |
| D35 | Teaching | Ponder: "Monster Cages" (catch, Monster Chow, shaft, speed, cull, Deployer, tiers) and "Containment" (FE, alarm, escape). Advancements: first caged monster, first caged Wither | defaults approved, round 5 |
| D36 | Escape details | An escape happens out of the cage's front with vanilla's 10-second summoning charge-up, then its blast. FE returning during the countdown cancels it. **If the field stays down, another Wither escapes every game day until the cage is empty** (revised: the first version let the rest stay in for good) | derived from D15; revised by the user 2026-10-27 ("if you don't fix it more will escape every game day until it's empty") |
| D37 | Idle | No stress while empty, full, out of chow or overstressed; the Netherite field keeps drawing its holding FE whenever it holds a Wither, even when idle | derived from D4 and D20 |
| D38 | Escape clock | With the field still down, the next Wither escapes **a game day (24,000 ticks) after the previous escape**; its 5-minute warning (klaxon and messages) is the **last 5 minutes of that day** (D77). Goggles show "next escape in mm:ss". The field back up stops the clock where it stands; once it has held for the hold (D78) it resets it all | user, what-if round 1; warning revised 2026-10-27; hold 2026-10-27 (D78) |
| D39 | Brownout | The field cost is flat (D20), so it's all or nothing. **Off, the cage takes no FE at all**: the power stays on the network for other machines, and whatever it had stored stays unused. It comes back only on a **full supply**: once the FE offered to it over a whole second covers the field cost, it starts a **one-second trial**, taking at most the field cost a tick and drawing nothing: it only measures what arrives. If at least the field cost arrives every tick for that second (the first FE may take up to 2 s to come, as a Power Line plans once a second), the field comes up with that FE as its reserve. If not, it's off again, the trial's FE kept in its store, unused, and the next trial waits **10 s, then 20, 40 and 60 s** after each failure, back to none once a field has held for the hold (D78). Up, it runs as before on its 200,000 FE store, farming FE shed first, so short dips are covered; once the store can't pay the field cost, it's off again. So a supply short of the cost (half of it, or a trickle) never comes up, and a store or generator that can hold the field up only in bursts never calls off a countdown (D78): the countdown runs out | user, what-if round 1 (follows from D20); revised 2026-10-27: "if there isn't enough power going in the cage should draw no power at all" (stay off and take nothing, rather than bank it); trial and retries 2026-10-27 ("short trial, slow retries") |
| D40 | Unloaded while down | The countdown and the escape clock pause while the chunk is unloaded and **resume where they stopped** when it loads with a player near (D44, D76) | user, what-if round 1; revised 2026-10-27 |
| D41 | Breaking mid-alarm | The Netherite cage **can't be broken** (or have its jar taken out) during a countdown or while its field is down. At other times breaking culls the extras into Nether Stars (D29) | user, what-if round 1 |
| D42 | Field numbers | The field takes **2,048 FE/t** whenever any Wither is inside; farming adds **up to 1,024 FE/t** at 256 RPM, scaling with speed (3,072 FE/t flat out). Buffer **200,000 FE** | user, what-if round 2 |
| D43 | Blocked front | An escaping Wither comes out at the **nearest open space** big enough for it around the cage | user, what-if round 2 |
| D44 | Nobody home | Escapes happen **only with a player within 128 blocks**. With nobody near, the countdown and the escape clock **stop, even if the chunk is kept loaded**, and the klaxon is silent; when a player comes back they resume where they stopped, with a fresh announcement of the time left (D76). Nothing is ever overdue | user, what-if round 2 ("Should only escape when a player is near, I don't want to be too mean (well maybe a little mean)"); 128-block range approved in Fish Tank round 3 |
| D45 | Airships | On Create Aeronautics physics objects the Netherite cage works with real rotation and FE, like the Pressure Crucible; if the ship's power fails, Withers escape in mid-air | user, what-if round 2 |
| D46 | Owner | The Netherite cage remembers who placed it: only they and their team can wrench or break it. Lines and generators aren't protected | user, what-if round 3 |
| D47 | Release | An Iron Jar's monster can be let out anywhere (not on Peaceful, D30) | user, what-if round 3 |
| D48 | Jar automation | **Deployers and Dispensers can catch and release** with jars | user, what-if round 3 |
| D49 | Lost jar | A destroyed filled jar loses its monster. The Netherite Jar is fireproof and blast-proof | user, what-if round 3 |
| D50 | Deployers and the Wither | A Deployer with a Netherite Jar can catch a Wither. Release is still only into a Netherite cage (D21), so a Dispenser does nothing with a full one | user, what-if round 4 |
| D51 | Weather | Sunlight, rain and lightning have no effect on any cage | user, what-if round 4 |
| D52 | Water monsters | Drowned, guardians and elder guardians live only in a **Flooded Monster Cage** (Andesite and Brass versions, same tiers, caps and rates as the dry ones) with a flooded interior. Water is piped in (pipes, Fluid Lines or a bucket, on any face but the back) to a tank of **4,000 mB** (Andesite) or **16,000 mB** (Brass); each new monster uses **250 mB** along with its Monster Chow; short of water it stops growing, and the ones inside are fine. The interior shows the tank's level. Dry monsters can't go in a flooded cage (except zombies and husks, which convert: D56), nor water monsters in a dry one | user, what-if round 4 and follow-up; the dry/wet split derived |
| D53 | Small what-ifs | Overstressed or stopped: progress pauses and is kept. Speed changes apply at once. Monster Chow is taken only when a monster arrives; a full cage takes none. Culling with one left does nothing and costs no durability. Copies are never named (a name tag stays on the jarred one). Night, moon phase, difficulty and dimension don't change rates. Countdowns and escape clocks survive a server restart | user, what-if round 4 |
| D54 | Fish Tank (follow-up) | The pens can't hold fish today: the Capture Bottle only takes vanilla `Animal`s, and fish, squid and dolphins aren't. A separate **Fish Tank** that works like the Flooded cage (water piped in) is to be designed in its own spec. When it ships, **axolotls** move to it: they join `pen_blacklist` (pens that already hold one keep it). Turtles and frogs stay in the pens | user, what-if round 4 ("Move Axolotls only") |
| D55 | Name | The upkeep item is **Monster Chow** (first called a "lure") | user, 2026-10-27 ("Can we change 'lures' to 'Monster Chow'") |
| D56 | Drowning | **A zombie put in a Flooded cage converts** into a drowned, as in vanilla, and the cage becomes a drowned cage. (A husk converts too, by way of a zombie.) Taking the jar out gives a jarred drowned | user, 2026-10-27 ("putting a zombie in a flooded monster cage should convert it"); husk and jar details, and the timing (vanilla's 30 seconds, the zombie shaking behind the glass; nothing grows until it's done; it needs water in the tank; a husk takes two steps, about a minute), approved in Fish Tank round 3 |
| D57 | Flooded recipe | Shapeless: the dry cage of the same tier + a Fluid Tank + 2 Framed Glass. The Brass Flooded cage can also be made by Mechanical Crafting around an Andesite Flooded one | approved, Fish Tank round 3 |
| D58 | Monstrous | A cage enchantment, **Monstrous** I–III, like Fruitful: each level makes monsters arrive 50% faster (2.5× at III); Monster Chow per monster doesn't change, so it's spent faster; stress doesn't change. Put on with an anvil, kept when the cage is broken. Books come from **Librarians** (their random book trades plus an extra Apprentice trade), never enchanting tables | user, 2026-10-27 ("we should have an enchant to speed up monster growth") and enchantment round |
| D60 | Drowned tridents | A slim chance: each new drowned has vanilla's 6.25% chance of holding a trident, and a culled one drops it at vanilla's 8.5% (+1% per Looting level), damaged as in vanilla. About 1 trident in 160–190 drowned culled | user, 2026-10-27 ("drowned should have a chance to have tridents (slim)"); odds proposed from vanilla |
| D61 | Armed drowned | While a Flooded cage holds at least one armed drowned, it **throws a real trident out of its front** at the nearest player within 16 blocks in sight, at most every 3 seconds however many are armed. Vanilla damage (about 4 hearts); shields block it; the thrown trident can't be picked up and vanishes, as vanilla drowned tridents do. A cull takes armed drowned first; behind or beside the cage is safe; Deployers aren't targeted; nothing is thrown on Peaceful. Goggles show "Armed: n" | user, 2026-10-27 ("but if they do they will throw it at players") and follow-up round |
| D59 | Monstrous on Withers | Every cage takes it, **the Netherite cage too**: up to 7.5 Nether Stars an hour at Monstrous III and 256 RPM | user, enchantment round |
| D62 | Upgrading | Only **empty** cages and tanks can be upgraded (Andesite → Brass, dry → Flooded, Fish Tank → Brass Fish Tank); the recipe refuses a cage holding a jar, monsters, chow or output | user, final sweep |
| D63 | Catchers | Strict split: the **Capture Bottle** takes animals and water creatures, the **Iron Jar** monsters, the **Netherite Jar** the Wither. The wrong one is refused with a hint naming the right one | user, final sweep |
| D64 | Bottle automation | Deployers and Dispensers catch and release with the Capture Bottle too, the same as with jars | user, final sweep |
| D65 | Eating and using | **Monster Chow is edible, badly** (like rotten flesh, with a chance of a fun effect: final sweep round 2). **Fish Food works in the world**: it breeds axolotls as a Bucket of Tropical Fish does, and fish swim after a player holding it | user, final sweep ("Chow is edible badly, has chance for other fun effect") |
| D66 | Eating Monster Chow | Like rotten flesh: 4 hunger, 80% chance of Hunger. Plus a 25% chance of a **Monster Belch** (a random monster's ambient sound from the player, heard by others: zombie groan, creeper hiss, ghast wail…) and a 5% chance of **Chow Breath** (Nausea for 10 s and a puff of green particles from the mouth) | user, final sweep round 2; chances proposed |
| D67 | Enchantments | Each to its own: **Fruitful** on Animal Pens and Fish Tanks, **Monstrous** on every Monster Cage, **Bountiful** on the Auto Harvester; an anvil refuses the others | user, final sweep round 2 |
| D68 | Display Links | Every cage and tank can show its count and status ("Blaze: 112 / 256 · next in 14 s", "Needs chow"), plus water on Flooded cages and tanks; the Netherite cage adds containment and the escape clock | user, final sweep round 2 (replaces the Netherite-only line in D34) |
| D69 | Stack sizes | Empty Iron and Netherite Jars stack to 16; filled jars and bottles to 1; Monster Chow and Fish Food to 64. The empty Capture Bottle stays at 64, as it already ships (cutting it would shrink players' stacks) | user, final sweep round 2; Capture Bottle kept at its released size |
| D74 | Warning length | The countdown before a Wither escapes is **5 minutes** (6,000 ticks), "that gives you enough time to fix it"; the field back up stops it, and calls it off once it has held for the hold (D78) | user, 2026-10-27; hold 2026-10-27 (D78) |
| D75 | Warning messages | Broadcast to **every player within 128 blocks**: a chat message when it starts ("Containment failing at x, y, z! A Wither breaks out in 5:00"), chat reminders at 4, 3, 2 and 1 minutes and at 30 s, and a live countdown on the action bar in the last 30 s. A chat message when the field comes back up mid-countdown ("Containment holding at x, y, z: the alarm stands down if the power holds for 1:00"), when it has held and the countdown is called off ("Containment restored at x, y, z"), and when a Wither escapes | user, 2026-10-27 ("they should broadcast to all local players"); format approved; holding message 2026-10-27 (D78) |
| D76 | Leaving stops it | If no player is within 128 blocks, the countdown (and the escape clock) **stops, even with the chunk loaded**, and resumes where it stopped when a player returns | user, 2026-10-27 ("If players leave the area the timer stops (even if chunk loaded)") |
| D77 | Klaxon rhythm | Bursts that speed up: three blasts when the countdown starts, then three every 30 s, every 10 s in the last minute, and continuously (every 1.6 s) in the last 10 s; silent while paused | user, 2026-10-27 |
| D78 | Restore hold | A field back up mid-countdown **pauses** the countdown (and the escape clock) where it stands; it calls it off, with "Containment restored", only once the field has **held for 60 s** (server config `restoreHoldSeconds`). If the field drops before then, the countdown carries on from where it stood, without restarting the warning or its messages. So bursts of power (a battery charged by a weak generator) never reset the countdown, and the Wither escapes on time, give or take the bursts | user, 2026-10-27 ("pause, then cancel after a minute") |
| D70 | Shield | While its field is up, the Netherite cage shows a **blue glowing shield around the whole block**; it flickers and cracks through a countdown (its field down for want of FE, D39, with a player near) and is gone when the field is down otherwise. Look details in the brief | user, 2026-10-27 ("Netherite one should have a blue glowing shield around it when it's powered with FE"); flicker wording follows D39 (2026-10-27) |

## Open Questions

None for v1.

Parked for later versions:
- Sharing one Monster Chow supply between cages (Create 6 packages or a chow line).
- Mob heads (charged creepers) and raid captains' banners.

## Core Loop

1. Catch a monster in an **Iron Jar** (a Wither in a **Netherite Jar**) by hand, or with a Deployer.
2. Put the jar in a cage (a **Flooded** one for water monsters), feed it **Monster Chow** (mixed from milled flour, bone meal and cinder flour) and turn the shaft.
3. A new monster joins every interval, sooner the faster the shaft turns, each one eating Monster Chow.
4. Cull half with a sword, or let a Deployer with a sword do it; the drops go into the output for funnels, arms and chutes.
5. Climb the tiers: Brass for Nether, End, ocean and rare monsters; Netherite, on the power grid, for the Wither and its stars.

## Behaviour / Rules

1. **Catching**: an Iron Jar catches a living monster that some cage takes (not the Wither) at any health, unless it's ridden or blacklisted. A Netherite Jar catches a Wither at any health. Players, Deployers and Dispensers can catch (D48, D50). The jar keeps the monster's NBT except its position, UUID, leash and equipment.
2. **Releasing**: an Iron Jar's monster can be let out anywhere, by a player, Deployer or Dispenser, except on Peaceful. A Wither only goes into a Netherite cage.
3. **Starting**: use a jar on an empty cage of a tier that takes that monster (Andesite: `andesite`; Brass: `andesite`, `brass` and `rare`; Netherite: `netherite` only), dry or Flooded to match the monster (D52); a zombie or husk put in a Flooded cage converts into a drowned (D56). Otherwise it's refused, with a hint naming the cage needed.
4. **Monster Chow** goes in by hand or by automation from any face; a new monster needs its chow cost (D24) in the slots, and a Flooded cage also needs 250 mB of water.
5. **Running**: with the shaft turning in the back, not overstressed, and with Monster Chow (and water) and room in the cage, progress builds at `speed / 64` per tick towards `base interval × chow cost` (D23, D24; the Netherite cage uses `speed / 256` against 20 minutes). When it completes, the chow (and water) is used and one bare copy joins (D27). Stress is `SU per RPM × speed` while running, 0 when idle (D37).
6. **Culling**: a sword (a player's or a Deployer's) culls round(n/2), always leaving one. The drops come from the monster's own loot table as if a player killed it (Looting and Fire Aspect count); XP goes only to a real player (D28); each cull costs 1 sword durability.
7. **Output**: 9 slots (Brass 18), emptied from any face by automation or by an empty-hand click. What doesn't fit is thrown out of the front.
8. **Netherite field**: FE comes in on any face but the back, from Power Lines or any FE source, into a 200,000 FE buffer. Whenever the cage holds a Wither the field draws a flat **2,048 FE/t**; farming adds up to **1,024 FE/t** at 256 RPM, scaled by speed (D42). Short of FE, the farming draw is shed first. With the field off the cage takes no FE at all, and the field comes back only through a one-second trial of full supply, retried after 10, 20, 40 and 60 s if it fails (D39).
9. **Field down**: below the field cost, with a player within 128 blocks, a 5-minute countdown starts: the klaxon sounds in bursts that speed up (D77), players within 128 blocks get chat messages and, in the last 30 s, a live action-bar countdown (D75), and goggles, the comparator (15) and Display Links show it. The field back up (D39) pauses it, and calls it off once it has held for a minute (D78); dropping before then, it carries on where it stood. At zero, one Wither comes out at the nearest open space around the cage, with the vanilla charge-up and blast, at full health. While the field stays down the cage stops, and another Wither escapes a game day later, its 5-minute warning being the last 5 minutes of that day, until it's empty. The countdown and the clock stop whenever no player is within 128 blocks, even with the chunk loaded, and while it's unloaded, and resume where they stopped (D40, D76). The config option `escapes = false` makes the cage just stop.
10. **Owner and breaking**: the Netherite cage belongs to whoever placed it (and their team): only they can wrench or break it, and not even they during a countdown or while the field is down (D41, D46).
11. **Wrench screen**: the jar slot, Monster Chow slots, growth gauge (time per monster at the current speed), output and horde count; Flooded adds the water gauge; Netherite adds the FE bar, the containment state and the escape clock. Quick-move buttons and the mouse wheel work on the output.
12. **Monstrous** I–III divides the interval by 1 + 0.5 × level, on every cage (D58, D59).
13. **Breaking or emptying** (D29), **unloaded and Peaceful** (D30), **moving** (D31) and **airships** (D45) work as decided.

## Progression

- **Andesite: early–mid.** Needs andesite alloy, casing, iron bars and a Deployer for the jar, plus a Mixer and a mill for Monster Chow: your first Create workshop. It gives the String Tangler its string and slime, plus bones, gunpowder and arrows. The Flooded version adds a Fluid Tank and a water supply (drowned: copper, and now and then a trident, D60).
- **Brass: mid.** Mechanical Crafting with brass and electron tubes, at the Brass Animal Pen's tier. It opens up blaze rods, ender pearls, shulker shells, Totems, gold and wither skulls; the Flooded Brass cage, prismarine and sponges.
- **Netherite: late.** 4 netherite ingots (the Pressure Crucible's netherite line is the natural source), Precision Mechanisms, FE Coils, and a real power grid (Lava Generators or a Geothermal Turbine, Power Lines, ideally Batteries). It gives Nether Stars.

## Balance

**Cost against reward.** Monster Chow per monster stays fixed; speed only changes how fast it's spent.

| Setup | Per hour | Monster Chow per hour | Upkeep |
|---|---|---|---|
| Andesite, zombies, 64 RPM | 180 monsters | 180 (60 Mixer batches) | 256 SU |
| Andesite, creepers, 256 RPM | 720 monsters | 720 | 1,024 SU |
| Brass, blazes, 256 RPM | 720 monsters | 1,440 | 4,096 SU |
| Brass, evokers, 64 RPM | 90 monsters (90 Totems) | 360 | 1,024 SU |
| Flooded Brass, guardians, 64 RPM | 180 monsters | 360 | 1,024 SU + 45,000 mB water |
| Netherite, Withers, 256 RPM | 3 Withers (3 Nether Stars) | 192 | 4,096 SU + 3,072 FE/t (3 Lava Generators) |
| Netherite, Withers, 256 RPM, Monstrous III | 7.5 Withers (7.5 Nether Stars) | 480 | 4,096 SU + 3,072 FE/t |

A good vanilla farm still out-produces a cage; the cage wins on space, safety and automation. It replaces a farm, not a resource. Holding even one Wither costs 2,048 FE/t around the clock.

**Exploits and fixes.**
- Equipment farm (a jarred zombie in diamond armour): copies arrive bare (D27).
- Self-feeding: Monster Chow needs Wheat Flour and Cinder Flour, which no caged monster drops; only the bone meal can come from skeletons.
- Duplication by breaking mid-growth: progress isn't stored as monsters; breaking culls the extras into drops once, and the jar holds exactly one.
- Jar loops (release, re-catch): the jar holds one monster, and caging gives no drops by itself.
- Cashing out to dodge an escape: the Netherite cage can't be broken or emptied while its field is down (D41).
- A Wither as a weapon: jarred Withers only go into cages (D21, D50); escapes need a player nearby, and servers can turn them off.
- Griefing a Netherite cage: only its owner can wrench or break it (D46); cut lines still set it off, which claim mods and the config cover.
- Dispenser monster cannons (D48): ordinary monsters only, one per jar, and each one had to be caught first.
- Holding Withers without power: an outage costs one Wither at once and one more every game day until the cage is empty (D36, D38).
- Speed abuse: stress and FE scale with speed, and Monster Chow per monster doesn't change.

**Overpowered check.** The Netherite cage makes Nether Stars from Monster Chow alone (D17): 3 an hour at full speed, 7.5 with Monstrous III (D59). The user judges this balanced (2026-10-27): a redstone Wither-killing machine in vanilla does 4,000+ stars an hour, so the cage is far slower than the best vanilla build; its edge is safety and space, not output. Every number is still in the config.

**Drop rates.** Every cull rolls the monster's vanilla loot table at vanilla rates, evokers' Totems included; the cage changes how often monsters come, never what they drop (user, 2026-10-27: "Drops should be vanilla rates even for evokers").

**Useless check.** An Andesite cage competes with an hour of building a dark-room farm. It wins on space and safety, and the String Tangler gives its string and slime somewhere to go.

## Create Integration

| System | Interaction | Notes |
|---|---|---|
| Shafts and gears | Yes | Shaft into the back only. Rate and stress scale with speed (D19) |
| Belts and depots | No | Output goes through funnels and chutes instead, as the pens' does |
| Contraptions | Rides along | Doesn't work or count down while moving; keeps its horde (D31) |
| Aeronautics physics objects | Works | With real rotation and FE, the Netherite cage works and can fail in flight (D45) |
| Trains and elevators | As contraptions | Nothing special |
| Packages and logistics | Indirect | Monster Chow in and drops out through funnels, so Stock Links and Frogports can serve it; shared chow lines are parked |
| Vaults, funnels, chutes | Yes | Monster Chow in and output out on every face |
| Deployers, Dispensers and arms | Yes | A Deployer with a sword culls; Deployers and Dispensers catch and release with jars (D48, D50); arms load chow and empty the output |
| Fluid pipes, Fluid Lines | Flooded cages | Water in on any face but the back (D52) |
| Redstone, Display Links | Yes | Comparator = how full of monsters (15 during a Netherite countdown); Display Links read containment, the countdown and the escape clock |
| Goggles | Yes | D34; Flooded adds water; Netherite adds the escape clock |
| Ponder | Yes | Two scenes (D35); the first shows a Flooded cage too |
| Mixer, Millstone, Crushing Wheels | Yes | Monster Chow is milled ingredients mixed in a Mixer (D25) |
| Power Lines, Batteries | Netherite only | FE on any face but the back |

## Technical Notes

- **Code**: one `MonsterCageBlock` and block entity for every cage (a tier enum plus a `flooded` flag), built on the Animal Pen's (`content/pen/`): reuse its sword culling, loot, output, quick-move menu and renderer approach. Stress via a per-RPM impact like the Pressure Crucible's, with idle at 0.
- **Sync**: horde count, the shown monster's NBT, progress, Monster Chow count, water, FE, and containment state with the countdown and escape clock, on block entity updates. Client-only: wandering (or swimming) monsters, the alarm light, particles.
- **Saving**: jar NBT, count, progress, Monster Chow, water, output, FE buffer, owner UUID, countdown ticks and the escape clock (ticks counted only while loaded with a player near).
- **Config** (`CMConfig`, a "Monster Cages" section): per tier, the cap, base interval, SU per RPM and slots; the chow cost per class; Flooded tank sizes and water per monster; for Netherite, the field FE, farming FE, buffer, countdown seconds, escape interval, player range and `escapes` on or off.
- **Compatibility**: plain BlockItems. The Netherite cage goes in Carry On's block blacklist. Jar dispense behaviour registered for both jars. The mod id doesn't change.
- **Data**: recipes (3 dry cages, 2 Flooded, 2 jars including a sequenced Deployer step and smithing, the Monster Chow Mixer recipe); loot tables that keep contents; entity tags (D26) plus `monster_cage/flooded` (drowned, guardian, elder guardian); lang keys with subtitles (klaxon, cull, growls); advancements; JEI info for which monsters go in which cage and their chow costs.

# ONE MINUTE UNDERSTANDING

The Monster Cage is the Animal Pen for monsters. Catch a zombie, skeleton, creeper or blaze in an Iron Jar, put it in a cage, feed it Monster Chow and turn the shaft. More of the same monster arrive, sooner the faster it turns, and each one eats chow. Cull them with a sword or a Deployer for their real drops. Andesite cages hold Overworld monsters, Brass adds Nether, End and rare ones, Flooded cages hold the water monsters, and the Netherite cage holds Withers on the power grid for Nether Stars. If its power fails while you're near, a Wither breaks out, and another every game day until you fix it.

# WHAT IT IS

A shaft-driven box that keeps a horde of one kind of monster and turns Monster Chow into their drops. It comes in Andesite and Brass (each also Flooded, for water monsters) and Netherite (for the Wither only). It replaces a sprawling mob farm, not the resources themselves.

# WHY IT EXISTS

- Monster drops (string, slime, bones, gunpowder) had no compact Create-style source, and the String Tangler needs them.
- It completes the one-block farm family: generators, Auto Harvester, Animal Pens.
- It gives the power grid a big, high-stakes consumer.

# HOW PLAYERS USE IT

- Catch a monster in an Iron Jar (a Wither in a Netherite Jar), or let a Deployer do it.
- Use the jar on a cage of the right tier, Flooded for water monsters.
- Feed it Monster Chow (and water, if Flooded) and turn its shaft.
- Cull with a sword or a Deployer; take the drops out with funnels.
- Netherite: keep 2,048 FE/t flowing, or Withers escape.

# HOW IT FITS CREATE

- Speed sets the rate; stress and chow use rise with it.
- Monster Chow is milled flour, bone meal and cinder flour, mixed in a Mixer.
- Deployers catch and cull; funnels, arms and pipes feed and empty it.
- Goggles, the comparator, Display Links and Ponder explain it.
- The Netherite cage runs on Power Lines and Batteries, even on an airship.

# IMPORTANT RULES

- One monster type per cage; the tier (and dry or Flooded) decides which.
- Each new monster costs Monster Chow: 1, 2, 4, or 64 for a Wither. Monstrous I–III speeds it up.
- The rate is linear with RPM; chow per monster never changes.
- Culling takes half, always leaving one. Deployer culls give no XP.
- New monsters arrive bare: no armour, weapons or names (drowned: a slim trident chance).
- Jarred Withers only go into a Netherite cage.
- Netherite field: a flat 2,048 FE/t, plus up to 1,024 while farming.
- Field down: 5-minute warning to everyone nearby, then one Wither escapes; another every game day.
- Escapes only happen with a player within 128 blocks.
- Only the owner can break a Netherite cage, and never mid-alarm.

# EDGE CASES

- Cage broken: the cage, jar, Monster Chow and output drop; the extras are culled into drops.
- Netherite cage broken during an alarm: refused; fix the power first.
- FE back mid-countdown: a full supply (D39) brings the field up after a second of offers and a one-second trial; the countdown stands still, and once the field has held a minute it's called off and the clock resets (D78). A field that drops sooner lets the countdown carry on where it stood. A supply short of the field cost doesn't bring it up: the cage takes none of it (a failed trial keeps at most a second's cost, unused) and the countdown runs on.
- Field down and nobody near: nothing counts down until a player comes within 128 blocks.
- Cage built into a wall: the Wither comes out at the nearest open space.
- Everyone walks away while the field is down: the countdown stops, even chunk-loaded, and resumes when someone returns.
- Ship loses power mid-flight: a Wither escapes in the air.
- Out of chow (or water): it stops and takes no stress.
- Peaceful: it holds, with no growth, no escapes, no releases and no trident throws.
- In front of a drowned cage with an armed drowned: tridents come at you; cull or stand aside.

# DEVELOPMENT CHECKLIST

- [ ] Blocks: Andesite, Brass and Netherite Monster Cage, plus Flooded Andesite and Flooded Brass (one block class, tier enum, flooded flag), BlockItems, loot tables that keep contents
- [ ] Block entity: horde, progress by speed, Monster Chow, water (Flooded), output, culling, per-RPM stress with idle at 0, saving
- [ ] Netherite: FE capability (all faces but the back), 200,000 FE buffer, flat field draw and speed-scaled farming draw, 5-minute countdown and daily escape clock that both stop with no player within 128 blocks (even chunk-loaded) and resume where they stopped, chat and action-bar warnings to every player within 128 blocks (D75), the klaxon rhythm (D77), nearest-open-space escape with summoning charge-up, `escapes` config, no FE taken while off and the trial and retries (D39), the restore hold and `restoreHoldSeconds` config (D78)
- [ ] Netherite owner (placer and team), wrench and break checks, no breaking or emptying while the field is down
- [ ] Items: Iron Jar, Netherite Jar (filled states, tooltip naming the monster, the monster drawn inside, Netherite fireproof and blast-proof), Monster Chow
- [ ] Catching and releasing by hand, Deployer and Dispenser (Withers: catch only; release only into cages; no releases on Peaceful)
- [ ] Entity tags `monster_cage/andesite|brass|rare|netherite|flooded|blacklist`, filled with the vanilla lists
- [ ] Zombie and husk conversion to drowned in Flooded cages (D56)
- [ ] Drowned trident chance (D60) and the cage's trident throws (D61: front only, 16 blocks, line of sight, 3 s cooldown, armed culled first)
- [ ] Bare, unnamed copies; slime and magma cube size rule; Looting and Fire Aspect; Deployer culls without XP
- [ ] Item handlers on all faces (chow in, output out); fluid handler on Flooded cages; comparator; Display Link source for containment and the escape clock
- [ ] Wrench menu and screen (GUI kit, QuickTransferMenu), with the water gauge (Flooded) and FE bar, containment and escape clock (Netherite)
- [ ] Renderer: up to 4 wandering monsters (scaled), swimming ones in the flooded interior at the water level, a Wither in the Netherite cage, alarm state
- [ ] Goggles tooltip (D34 plus water and escape clock)
- [ ] Contraption behaviour (frozen, keeps the horde); Aeronautics physics objects (working); Carry On blacklist for Netherite
- [ ] Recipes: dry cages (crafting, 2 Mechanical Crafting), Flooded cages, Iron Jar (sequenced Deployer), Netherite Jar (smithing), Monster Chow (Mixer)
- [ ] Config section "Monster Cages" with every number in D23, D24, D42, D44, D52
- [ ] Sounds through the sound workflow: muffled growls, cull, chow eaten, klaxon, escape; subtitles
- [ ] Ponder: "Monster Cages" and "Containment"; JEI info pages for tiers and chow costs
- [ ] Monstrous enchantment (I–III, every cage, anvil only, kept when broken, Librarian trades); anvils refuse the wrong farm enchantment (D67)
- [ ] Monster Chow food and its Belch and Chow Breath effects (D66)
- [ ] Display Link sources on every cage (D68); upgrade recipes refuse non-empty cages (D62); strict catcher split with hints (D63); Capture Bottle dispense and Deployer behaviour (D64)
- [ ] Advancements: first caged monster, first caged Wither
- [ ] Lang and README entries
- [ ] Tests: game tests for growth against speed, chow and water use, culling half, bare copies, breaking, countdown cancel, the 5-minute countdown, warnings reaching every player within 128 blocks, daily escapes, the countdown stopping when everyone leaves (chunk still loaded) and resuming, clock pause while unloaded, no breaking mid-alarm, escapes off

# "IDIOT TEST"

- Animal Pen, but for monsters.
- Catch it in an Iron Jar.
- Jar into cage, Monster Chow in, shaft on.
- Faster shaft: more monsters, more chow eaten.
- A sword or Deployer culls half.
- Water monsters need a Flooded cage and water.
- Netherite cage: Withers only, needs FE too.
- Power fails: 5-minute warning, then Withers escape daily.
- New monsters never carry armour or weapons.
- Every number lives in the config.

# COMPLETENESS SCORE

Gameplay: 95% · World Integration: 95% · Balance: 95% · Progression: 95% · Automation: 100% · Edge Cases: 95% · **Overall Design Readiness: 95%**

- Gameplay 95%: the loop and every rule are set; how big monsters (ghast, ravager) fit in the window is left to the look.
- World Integration 95%: multiplayer, unloading, airships and weather are all settled; claim-mod behaviour is untested.
- Balance 95%: drops are vanilla and Nether Stars sit far below vanilla Wither farms (user, 2026-10-27); only how fast Monster Chow is used is left to playtest.
- Progression 95%: every recipe is set.
- Automation 100%: every step, catching included, can be automated.
- Edge Cases 95%: all answered; escape placement in tight spaces needs a playtest.
- Overall 95%: ready to build; Monster Chow use gets tuned after playtesting.

---

# Part 2: Fish Tank

## Concept

The user (2026-10-27), while designing the Flooded Monster Cage: "we should revisit animal pens, can they hold fish, they shouldn't but we should add a fish tank that works this way too", then "Let's add the tank to the spec and the artifact".

Clarified: the water-creature member of the family. Keep fish, squid and axolotls in a Create-style tank with water piped in, and harvest what they make, the way the pens keep animals and the cages keep monsters.

## Decisions

| # | Area | Decision | Source |
|---|---|---|---|
| F1 | What it is | A block that keeps water creatures, working like the Flooded Monster Cage (water piped in) | user, 2026-10-27 |
| F2 | Pens | The Animal Pens never hold fish (true today: the Capture Bottle only takes vanilla `Animal`s). When the tank ships, **axolotls** move to it: the pens refuse every tank creature in code (`TankRules.livesInTank`), with a hint naming the Fish Tank, rather than through `pen_blacklist`, which also stops the Capture Bottle and so would lock them out of tanks too; pens that already hold one keep it. Turtles and frogs stay in the pens | user, what-if round 4 (D54) |
| F3 | Family rules | Unless decided otherwise, it follows the cages: shaft into the back, speed sets the rate (D19), items out of every face, wrench screen with the GUI kit and quick moves, comparator, kept on contraptions, Carry On via a BlockItem, look previewed in Blockbench first | existing mod rules; Monster Cage D4–D7, D19 |
| F4 | Same spec | Designed in this spec and on the same quick-read page as the Monster Cage | user, 2026-10-27 |
| F5 | Core loop | **Breeds like the pens**: each adult eats food and has young on the breeding interval, so the school doubles up to a cap | user, Fish Tank round 1 |
| F6 | Harvest | A **sword** (or a Deployer with one) culls half for drops (raw cod, salmon, pufferfish, ink sacs, glow ink sacs, bone meal), as the cages and pens do. A **water bucket** scoops one live adult as a Bucket of Fish (axolotls in their colour) | user, Fish Tank round 1 |
| F7 | Stocking | The **Capture Bottle** catches every tank creature (extended to fish, squid and axolotls); use the bottle on the tank to start it | user, Fish Tank round 1 |
| F8 | Creatures | **Fish** (cod, salmon, tropical fish of every pattern, pufferfish), **squid** (squid, glow squid) and **axolotls**. No dolphins or tadpoles | user, Fish Tank round 1 |
| F9 | Food | **Fish Food** for every creature: a Create Mixer turns 1 Wheat Flour + 1 Dried Kelp + 1 Bone Meal into 4 Fish Food | user, Fish Tank round 2 |
| F10 | Sizes | **Fish Tank** holds 128, **Brass Fish Tank** 512. Each adult breeds every 5 minutes (at 64 RPM); axolotl young grow up in 20 minutes; fish and squid have no young in vanilla, so theirs arrive grown. Food slots / output slots: 2/9 and 8/18 | user, Fish Tank round 2 |
| F11 | Speed and water | Speed sets the rate as with the cages (64 RPM = the pens' times, 256 RPM four times as fast), with stress per RPM and none while idle. Each birth uses **250 mB** of water from a tank of 4,000 mB (16,000 Brass), piped in on any face but the back; short of water, no births | user, Fish Tank round 2 |
| F12 | Fruitful | The pens' **Fruitful** enchantment works on tanks: each level breeds and grows 50% faster, kept when broken | user, Fish Tank round 2 |
| F13 | Variants | Young copy their parent's tropical fish pattern or axolotl colour; axolotl births have vanilla's 1-in-1,200 chance of a blue one. A bucket scoops a blue axolotl first if there is one | user, Fish Tank round 3 |
| F14 | Dry tank | Short of water it stops breeding (as the Flooded cage); the creatures are fine. The window shows the level dropping and the fish sitting lower | user, Fish Tank round 3 |
| F15 | Glow | A tank holding glow squid gives light level 6 | user, Fish Tank round 3 |
| F16 | Recipes | Fish Tank: andesite alloy corners, 3 Framed Glass, a Fluid Tank, Andesite Casing. Brass Fish Tank: Mechanical Crafting around a Fish Tank with brass sheets, Brass Casing, electron tubes, Framed Glass | defaults approved, Fish Tank round 3 |
| F17 | Automation | Deployers use swords and water buckets on it, as on the pens. A Capture Bottle can't take a creature back out of a tank (the wrench screen's bottle slot does, as on the pens) | defaults approved, Fish Tank round 3 |
| F18 | Small rules | Pufferfish don't puff or sting from inside a tank. Culls give loot-table drops only: no fishing junk or treasure. The last adult can't be culled or scooped | defaults approved, Fish Tank round 3 |
| F19 | Info and teaching | Goggles: the school (adults + young / cap), status (needs rotation, needs Fish Food, needs water, full, breeding n%), water level, Fruitful. Comparator: how full of creatures. Ponder "Fish Tanks"; advancement for the first blue axolotl | defaults approved, Fish Tank round 3 |
| F20 | Catching | The Capture Bottle is extended to fish (cod, salmon, tropical fish, pufferfish), squid and glow squid; it already takes axolotls. Tadpoles and dolphins are refused. A bottled tank creature used on a pen is refused with a hint naming the Fish Tank | derived from F7 and F8 |

## Open Questions

None for v1.

## Core Loop

1. Catch a fish, squid or axolotl in a **Capture Bottle**.
2. Use the bottle on a **Fish Tank**, pipe water in, and put **Fish Food** (mixed from Wheat Flour, Dried Kelp and Bone Meal) in its trough.
3. Turn the shaft: each adult has young every 5 minutes at 64 RPM (sooner faster), each birth eating food and 250 mB of water.
4. Cull half with a sword or Deployer for cod, salmon, ink sacs and pufferfish, or scoop live fish with a bucket (tropical fish for axolotls, the rare blue axolotl).

## Behaviour / Rules

1. **Starting**: a bottled fish, squid or axolotl used on an empty tank moves in (as the pens). Babies arrive as babies (axolotls only).
2. **Breeding**: with the shaft turning, not overstressed, and with food, water and room, every adult eats one Fish Food and has one young per breeding interval: 5 minutes at 64 RPM, scaled by speed (and Fruitful, F12). Each birth uses 250 mB of water. Fish and squid young arrive grown; axolotl young grow up in 20 minutes (scaled the same way).
3. **Water**: a 4,000 mB tank (16,000 Brass), piped in on any face but the back by pipes, Fluid Lines or a bucket. Short of water, no births (F14).
4. **Culling**: a sword or Deployer culls round(adults/2), always leaving one, with loot-table drops (Looting and Fire Aspect count) and XP for a player.
5. **Scooping**: a water bucket (a player's or a Deployer's) takes one live adult as its vanilla Bucket of Fish (axolotl in its colour, blue first). Squid can't be bucketed.
6. **Output**: 9 slots (Brass 18), emptied from any face; what doesn't fit is thrown out of the front.
7. **Stress**: 4 SU per RPM (16 Brass) while breeding; none while empty, full, or out of food or water.
8. **Light**: level 6 while it holds glow squid.
9. **Wrench screen**: the bottle slot, food trough, water gauge, breeding gauge, output and school count, with quick moves. Taking the bottle out culls the other adults into the output (as the pens).
10. **Breaking**: drops the tank, the bottle, the food, the output and the extra adults' drops. Contraptions carry it with its school, without breeding. Nothing breeds while unloaded.

## Progression

Early–mid, beside the Animal Pen: andesite alloy, Framed Glass, a Fluid Tank and Andesite Casing, plus a Mixer for Fish Food and a water supply (a Water Wellspring is the natural one). The Brass Fish Tank is mid game. It gives food (cod, salmon), dyes (ink sacs, glow ink sacs), pufferfish for Water Breathing, and axolotls for colour and for fighting alongside you.

## Balance

| Setup (at cap, culling every interval) | Per hour | Fish Food per hour | Water per hour | Stress |
|---|---|---|---|---|
| Fish Tank, cod, 64 RPM | about 770 cod | 770 | 192,000 mB (53 mB/s) | 256 SU |
| Brass Fish Tank, squid, 64 RPM | about 3,070 ink sacs worth of squid | 3,070 | 768,000 mB | 1,024 SU |

- Nothing here is rare: cod, salmon and ink are cheap in vanilla too. The tank's value is space and automation, not new resources.
- The blue axolotl is the only rare result, at vanilla's odds.
- **Exploits**: tropical fish to axolotl loops are fine (both cost food and water); scooping can't take the last adult; the Capture Bottle can't pull creatures out of a tank to skip the wrench rule.
- **Useless check**: vanilla fish farms barely exist (fish don't breed), so the tank is the only steady fish and ink source in the pack.

## Create Integration

| System | Interaction | Notes |
|---|---|---|
| Shafts and gears | Yes | Shaft into the back; speed sets the rate |
| Fluid pipes, Fluid Lines, Wellspring | Yes | Water in on any face but the back |
| Mixer | Yes | Fish Food |
| Deployers and arms | Yes | Swords cull, buckets scoop; arms load food and empty the output |
| Funnels, chutes, vaults | Yes | Food in, drops out, every face |
| Contraptions | Rides along | Keeps its school, doesn't breed while moving |
| Goggles, comparator, Ponder | Yes | F19 |

## Technical Notes

- Build on the Animal Pen (`content/pen/`): one block class with a `brass` flag, its breeding, young, culling, output and menu; add the fluid tank (reuse the Flooded cage's) and speed-scaled timing.
- `AnimalCapture.canCapture`: accept `AbstractFish` and `Squid` as well as `Animal`, minus a new `fish_tank_blacklist` tag (tadpoles) and `pen_blacklist`; route by type: tank creatures, axolotls included, to tanks (refused by the pens in code, F2).
- Renderer: up to 4 real creatures swimming in the water volume (swim paths instead of walk paths), water surface at the tank's level.
- Saving: bottle NBT, school (adults, young timers), food, water, output, progress. Sync the same set.
- Config ("Fish Tanks"): caps, breeding and growing seconds at 64 RPM, SU per RPM, tank sizes, water per birth, glow light level.
- Data: recipes (2 tanks, Fish Food Mixer recipe), loot tables keeping contents, entity tags, lang keys, Ponder, advancement.

# FISH TANK: ONE MINUTE UNDERSTANDING

The Fish Tank is the Animal Pen for water creatures. Catch a fish, squid or axolotl in a Capture Bottle, put it in a tank, pipe water in, fill its trough with Fish Food and turn the shaft. The school breeds every five minutes (faster with speed or Fruitful), each birth using food and a little water. Cull half for cod, salmon and ink, or scoop live fish out with a bucket, including the rare blue axolotl.

# FISH TANK: WHAT IT IS

A shaft-driven glass tank that breeds one kind of water creature, in two sizes (128 and 512). It's the pens' rules underwater, with the cages' speed rule and water use.

# FISH TANK: WHY IT EXISTS

- Fish and ink have no steady source: fish don't breed in vanilla.
- The pens refuse fish, and axolotls belong in water.
- It completes the farm family alongside pens and cages.

# FISH TANK: HOW PLAYERS USE IT

- Catch a creature in a Capture Bottle.
- Use the bottle on a tank; pipe water in.
- Put Fish Food in the trough; turn the shaft.
- Cull with a sword or Deployer; scoop with a bucket.
- Pull drops out with funnels.

# FISH TANK: HOW IT FITS CREATE

- Speed sets the breeding pace; stress follows speed.
- Water comes by pipe, Fluid Line or Wellspring.
- Fish Food is mixed in a Mixer from milled flour, kelp and bone meal.
- Deployers cull and scoop; arms and funnels feed and empty it.
- Goggles, comparator and Ponder explain it.

# FISH TANK: IMPORTANT RULES

- One kind of creature per tank.
- Every adult breeds each interval, eating Fish Food and 250 mB of water.
- 5 minutes at 64 RPM; faster with speed and Fruitful.
- Fish and squid young arrive grown; axolotl young grow up in 20 minutes.
- Culling takes half, always leaving one.
- A bucket scoops one live adult; blue axolotls first.
- Young copy their parent's pattern or colour.
- No water, food or rotation: no breeding.
- Glow squid tanks give light 6.
- Axolotls live here now, not in pens.

# FISH TANK: EDGE CASES

- Tank runs dry: breeding stops; the creatures are fine.
- Bottled fish used on a pen: refused, with a hint to use a Fish Tank.
- A pen that already holds axolotls: keeps them.
- Last adult: can't be culled or scooped.
- Squid and a bucket: nothing happens; squid can't be bucketed.
- Tank broken: tank, bottle, food and output drop; the extras are culled.
- On a contraption: rides along, no breeding.
- Unloaded: nothing breeds.
- Tadpole or dolphin: the bottle refuses them.
- Output full: drops spill out of the front.

# FISH TANK: DEVELOPMENT CHECKLIST

- [ ] Blocks: Fish Tank, Brass Fish Tank (one class, brass flag), BlockItems, loot tables keeping contents
- [ ] Block entity: school, young timers, speed-scaled breeding, Fish Food, water tank, output, culling, scooping, per-RPM stress, saving
- [ ] Capture Bottle catches fish, squid and glow squid; `fish_tank_blacklist` tag (tadpoles); pens refuse tank creatures, axolotls included
- [ ] Fish Food item and Mixer recipe; in the world it breeds axolotls and fish swim after a player holding it (D65)
- [ ] Variant rules: copy pattern or colour; 1-in-1,200 blue axolotl
- [ ] Light level 6 with glow squid
- [ ] Item and fluid handlers; comparator; Deployer sword and bucket use
- [ ] Wrench menu and screen (GUI kit, QuickTransferMenu) with a water gauge
- [ ] Renderer: swimming creatures in the water volume, water surface at the level
- [ ] Goggles tooltip (F19); Fruitful support
- [ ] Recipes (F16); config section "Fish Tanks"
- [ ] Sounds through the sound workflow (bubbles, splash on scoop)
- [ ] Ponder "Fish Tanks"; advancement for the first blue axolotl; lang and README
- [ ] Tests: breeding against speed, water and food use, dry tank, culling half, scooping (blue first, never the last), bottle routing between pens and tanks

# FISH TANK: "IDIOT TEST"

- Animal Pen, but underwater.
- Catch it in a Capture Bottle.
- Bottle into tank, water piped in.
- Fish Food in, shaft on.
- Every adult breeds every 5 minutes.
- Each birth uses food and water.
- Sword culls half; bucket scoops one.
- Blue axolotls can turn up.
- Axolotls moved here from the pens.
- Glow squid tanks glow.

# FISH TANK: COMPLETENESS SCORE

Gameplay: 95% · World Integration: 90% · Balance: 90% · Progression: 95% · Automation: 100% · Edge Cases: 90% · **Overall Design Readiness: 90%**

- Gameplay 95%: how creatures swim inside a one-block window is left to the look.
- World Integration 90%: moving axolotls out of the pens needs a playtest on a world that already has axolotl pens.
- Balance 90%: cheap resources, vanilla odds for blue; water use at the Brass cap is heavy and may want tuning.
- Progression 95%: recipes and tier set.
- Automation 100%: every step, catching aside, as the pens.
- Edge Cases 90%: covered; the bottle routing between pens and tanks needs a test.
- Overall 90%: limited by World Integration, Balance and Edge Cases.
