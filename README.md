# ComedyMonkey's Create Addons

A NeoForge add-on for **Create** (Minecraft 1.21.1). Mod id: `comedymonkeysaddons`.

- **Fluid Wellspring**: an upgradeable Create-style tank that generates water, lava, honey or chocolate.
- **Chest Upgrades**: Copper, Iron, Gold, Diamond and Netherite upgrades that raise a chest's or barrel's stack size (Diamond and Netherite add slots too), with a sort button.
- **Pressure Crucible**: presses Emerald, Netherite and Quartz Dust (rare drops from crushing cobblestone, netherrack and blackstone) into Emeralds, Netherite Scrap and Nether Quartz, and makes glass, stone, amethyst, obsidian and diamonds.
- **Cobblestone, Basalt, Blackstone and Tuff Generators**: one-block stone generators powered by rotation, in Andesite, Brass and Wireless versions.
- **Auto Harvester**: a Create-style botany pot that grows and harvests any crop or sapling (modded crops too) at a fixed stress, sped up by the new **Bountiful** enchantment from Librarians.
- **Animal Pen** and **Brass Animal Pen**: catch one animal in a **Capture Bottle** and breed a whole herd of it (128 or 512) in one block, with vanilla breeding and growing-up times, sped up by the new **Fruitful** enchantment.
- **Cuckoo Clock**: a Black Forest wall clock that sets the villagers around it to restock at **8:00 and 12:00**, and chimes the hour with a cuckoo and a carved villager.
- **Trade Bot**: a one-wheeled clockwork robot who sells the goods in your chests to **villagers sitting on Create seats**, brings home the emeralds (and the trades' XP as Nuggets of Experience), and lives in a winding station charged with FE.
- **Magma Engine** and **Supercharged Magma Engine**: lava-burning engines with four and eight Portable Engines' worth of power.
- **String Tangler** (cobwebs from string and slime) and **Stone Infuser** (Create's ore-bearing stones from cobblestone and metal nuggets).
- **Incinerator**: destroys any item or fluid, and heats a boiler while it burns.
- **Electricity**: the **Lava Generator** and the 3x3x3 **Geothermal Turbine** burn lava for Forge Energy, and the **Electric Motor** turns it back into rotation.
- **Compact Gyroscope**, **Torque Rotor** and **Flight Instruments** for Create Aeronautics ships: stay level, turn without strafing, and read altitude, airspeed, attitude and heading.
- **Aviator's Goggles in the Curios head slot** (with Create Aeronautics).
- **Pipez**: pipes can feed Blaze Burners and Portable Engines, and **Pipez Covers** hide pipework inside walls and floors.

Everything works on Create contraptions and Create Aeronautics physics objects (except the Cuckoo Clock, which runs only as a placed block), and can be carried with Carry On.

## Requirements

| Mod | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 (21.1.228+) |
| Create | 6.0.10+mc1.21.1 |

Optional: Create Aeronautics 1.3.2 (with Curios API 9.5.1+1.21.1, goggles in the Curios head slot; Sable for the Compact Gyroscope and Torque Rotor), Create Aeronautics: Gyroscope Stabilizers (optional: converts into the Compact Gyroscope), Pipez (Blaze Burner and Portable Engine feeding, Pipez Covers, energy pipes for the generators). Tested alongside Create: Liquid Fuel, Carry On, Inventory Profiles Next and Supplementaries.

## Upgrade Template

All upgrades start from an **Upgrade Template**, made on a Create sequenced assembly line:
copper sheet → deploy Cogwheel → deploy Glass → deploy Electron Tube.

## Fluid Wellspring

A riveted copper tank with big glass windows on all four sides that show the fluid level (ticks on the corner posts mark 25%, 50% and 75%). Holds water or lava; fill or empty it with buckets, pumps or pipes from any side.

| Upgrade | Sequenced assembly on an Upgrade Template | Effect |
|---|---|---|
| Water Upgrade | 2 Pointed Dripstone (makes a **Drip Template**), then spout 250 mB water onto it | Generates water |
| Lava Upgrade | 2 Pointed Dripstone (makes a **Drip Template**), then spout 250 mB lava onto it | Generates lava |
| Capacity Upgrade | 3 Fluid Tanks | Enlarges the tank |
| Honey Upgrade | Drip Template, then **3 loops** of: deploy a Honeycomb Block, spout 1000 mB honey, deploy a Precision Mechanism, press | Generates Create honey |
| Chocolate Upgrade | Drip Template, then **3 loops** of: deploy Cocoa Beans, spout 1000 mB chocolate, deploy a Precision Mechanism, press | Generates Create chocolate |

- Honey and chocolate meet lava as limestone and scoria (Create's own fluid interactions), so a Honey or Chocolate Wellspring next to a lava one is an endless limestone or scoria source. They generate 10/25/50 mB/s with 1/2/3 upgrades (configurable).
- Up to 3 generation upgrades of one kind (each plugs into a socket on the lid, with a lens in its fluid's colour) and up to 3 Capacity Upgrades (each a copper canister in the lid's back row).
- **Right-click with an upgrade** to install it. **Wrench** (or sneak + right-click) opens the upgrade screen to add or remove upgrades.
- Engineer's Goggles show the upgrades, generation rate and fill level as a percentage.
- Display Links can read its **stored fluid** (name and millibuckets), **fill level** (as a percentage or a progress bar) and **generation rate** (mB/s, "Full" or "No upgrades").
- Crafting recipe: copper sheets, Mechanical Pump, glass, Fluid Tank and Copper Casing.

## Chest Upgrades

Upgrade Template → **Chest Upgrade Template** (deploy a chest and Andesite Alloy) → Copper → Iron → Gold → Diamond → Netherite. Each tier is the previous upgrade plus two copper sheets, iron sheets, golden sheets or diamonds, then pressed.

| Tier | Items per slot (64-stack items) | Slots per chest |
|---|---|---|
| Copper | 128 | 27 |
| Iron | 256 | 27 |
| Gold | 512 | 27 |
| Diamond | 512 | 54 (108 as a double chest) |
| Netherite | 1024 | 54 (108 as a double chest), and blast-proof |

- **Right-click a chest or barrel with an upgrade** to turn it into an upgraded one, keeping its items. Using a higher tier upgrades it and hands back the old upgrade. A double chest takes **one** upgrade for both halves.
- **Wrench** (or sneak + right-click with an upgrade) opens the upgrade screen: swap the upgrade, or take it out to turn the chest back into a vanilla chest. Downgrades that would leave stacks or slots that don't fit are refused.
- **Sneak + right-click the side of a single upgraded chest with a chest** to extend it into a double chest of the same tier.
- The **Netherite Chest Upgrade** is smithed like netherite gear: a Netherite Upgrade template, a Diamond Chest Upgrade and a netherite ingot. Netherite chests and barrels shrug off explosions, and the upgrade item survives fire and lava.
- **Sort button**: upgraded chests and barrels have a small sort button in the band at the top of their screen. It merges stacks up to the slot's real limit and orders them by item.
- **Quick moves**, done with the real stack sizes (Inventory Profiles Next's own simulate vanilla ones, so it's kept off these screens): four small buttons by the "Inventory" label put everything in, put in only what the chest already holds, take everything out, or take out only what you already carry; the **mouse wheel** over a slot moves one item at a time (towards the other panel sends it, away pulls a matching one back); and **shift + drag** shift-clicks every slot you sweep over. The Animal Pens and Auto Harvester get the take-out half of this for their output.
- Big stacks show their count small enough to stay in the slot (from 1,000 shortened, so 4,096 reads "4.0k"); the tooltip gives the exact number.
- Unstackable items are never multiplied. Hoppers, Create funnels and arms, and Pipez fill slots to the full limit.
- Breaking an upgraded chest drops a vanilla chest and its upgrade (a double chest keeps the upgrade in the remaining half).
- **Barrels** take the same upgrades with the same tiers and rules (no double barrels); breaking one drops a barrel and its upgrade.
- **With Carry On**: place a carried upgraded chest against the side of a matching single one to join them into a double. Picking up half of a double moves the upgrade to whichever half needs it, and is refused if neither half would fit a vanilla chest.

## Pressure Crucible

Fully automatable emeralds and netherite, built from Create machines:

| Step | Machine | Recipe |
|---|---|---|
| Netherrack | Mixer over a **heated** Blaze Burner | Cobblestone + 50 mB lava |
| Emerald Dust | Crushing Wheels | Cobblestone or stone → gravel, plus a **5%** chance of Emerald Dust |
| Emerald Dust | Millstone | Cobblestone or stone → gravel, plus a **2.5%** chance of Emerald Dust |
| Netherite Dust | Crushing Wheels | Netherrack → cinder flour as usual (1, plus 50% for a second), plus a **1%** chance of Netherite Dust |
| Emerald | Pressure Crucible over a **heated** Blaze Burner | 9 Emerald Dust |
| Netherite Scrap | Pressure Crucible over a **superheated** Blaze Burner (Blaze Cake) | 9 Netherite Dust |
| Glass | Pressure Crucible over a **heated** Blaze Burner (quick) | 1 Sand |
| Stone | Pressure Crucible, no heat needed (quick) | 1 Gravel |
| Quartz Dust | Crushing Wheels | Netherrack: **5%** chance; Blackstone (→ gravel): **10%** chance |
| Nether Quartz | Pressure Crucible over a **heated** Blaze Burner | 4 Quartz Dust |
| Amethyst Shard ×2 | Pressure Crucible over a **superheated** Blaze Burner | 4 Nether Quartz |
| Obsidian | Pressure Crucible over a **superheated** Blaze Burner | 4 Cobblestone |
| Diamond | Pressure Crucible over a **superheated** Blaze Burner (slow) | 64 Coal |


Two more blocks you can now make with Create's Mechanical Mixer:

| Block | Mixer recipe |
|---|---|
| Gilded Blackstone | Blackstone + 20 gold nuggets, **heated** (crushing it gives 18 nuggets back) |
| Calcite | Diorite + bone meal (milling it gives 0.75 bone meal back) |

- The Crucible is a square screw press. It sits on a Blaze Burner and takes rotation through a shaft into the gearbox on its back (it faces away from you when placed, or backs onto a shaft next to it), which turns the big gear and the screw on top. It presses faster at higher speeds (16 RPM is the base speed, 256 RPM is 16x), with a heavy stress impact (12 SU per RPM by default).
- At 64 RPM an Emerald takes 2.5 seconds, a Netherite Scrap 10 seconds, and glass or stone half a second. The platen rides down the screw over exactly that time for each item, while the gauge on the front climbs to the red, and the press crunches as it lands. The bowl glows with the burner's heat: a faint red when smouldering, amber when heated, soul blue when superheated.
- **Right-click with dust** to load it, **right-click with an empty hand** to take the results out. Funnels, hoppers and pipes insert dust and extract results from any side.
- Engineer's Goggles show the contents, the heat needed, progress and what's holding it up.
- Crafting recipe: 4 obsidian, a Mechanical Press, 2 brass sheets, a cogwheel and a Basin.
- JEI shows the Crucible recipes; Create's crushing and mixing pages show the dust chances and the netherrack recipe. All of them are ordinary data-pack recipes, so packs can change the chances, counts and times.

## Cobblestone Generators

One-block, lava-and-water cobblestone generators: a steel casing with a glass front onto a dark chamber. The front faces you when placed and you can watch it work: water and lava pour from nozzles into the basin and a block of cobblestone rises between them, with puffs of steam, while a drill above it spins with the shaft and jabs down on every drill hit. The Brass tier makes a batch of four smaller blocks under a four-bit drill head. A lamp on the front frame shows its state: **cyan** running, **amber** full, **dark red** switched off by redstone, unlit with no rotation. The lava glows. Power goes in through the **shaft hole on the back**, the only side that takes a shaft.

| Generator | Per cycle | Holds | Stress | Recipe |
|---|---|---|---|---|
| Andesite | 1 cobblestone | 64 | 4 SU per RPM | Crafting: Andesite Alloy, 2 Mechanical Drills, Andesite Casing, a water bucket and a lava bucket (buckets returned) |
| Brass | 16 cobblestone | 256 | 16 SU per RPM | 5x5 **Mechanical Crafting**: an Andesite Cobblestone Generator in the middle, with 4 Mechanical Drills, Brass Casing, brass sheets and electron tubes |

- A cycle takes exactly as long as a Mechanical Drill at the same speed takes to mine cobblestone: 10 ticks at 256 RPM, 130 ticks at 16 RPM.
- Cobblestone only comes out through item transport (hoppers, chutes, funnels, Pipez, arms) from any side. When full the generator stops; breaking it drops what it holds.
- **No stress when idle:** when full, or switched off with a **redstone signal**, it stops and puts no load on the network until it's emptied or the signal goes.
- **Wireless Andesite / Brass Cobblestone Generators:** craft either generator with a **Transmitter** (shapeless). It's permanent: a wireless generator stays wireless, and a Wireless Andesite one in the Brass Mechanical Crafting recipe makes a Wireless Brass one. They have an antenna on top with two frequency slots, set just like a Redstone Link; a signal on that frequency switches the generator off, and the antenna lights up.
- Engineer's Goggles show how much it holds and its output per minute. Stress impacts are configurable.
- **Basalt and Blackstone Generators** come in the same four kinds (Andesite, Brass, and Wireless of each) and work the same way, but lava meets a block of **blue ice** in the chamber. They're easy to tell apart: Basalt ones have ice-blue trim and a smooth basalt chamber, Blackstone ones black-and-gold panels and a polished blackstone chamber. They follow the same drill timing for their own block: basalt is softer, so at 256 RPM it comes every 5 ticks (twice cobblestone's rate); blackstone matches cobblestone's 10 ticks at 256 RPM and is a little quicker at lower speeds (100 ticks at 16 RPM, against 130).
  - Andesite Basalt Generator: Blue Ice, a lava bucket, 2 Mechanical Drills, Andesite Casing, Andesite Alloy and **Soul Soil**.
  - Andesite Blackstone Generator: the same with **Blackstone** in place of the Soul Soil.
  - Brass and Wireless versions are made exactly like the cobblestone ones (Mechanical Crafting around the Andesite one, or add a Transmitter).

- **Tuff Generator** and **Wireless Tuff Generator**: lava meets water over a bed of gravel and makes tuff, at the same
  drill-paced speed (tuff is as soft as blackstone: 10 ticks at 256 RPM). It works like the Andesite generators and holds a
  stack; trimmed in grey-green with a tuff brick chamber.
  - Recipe: Andesite Alloy, a lava bucket, 2 Mechanical Drills, Andesite Casing, a water bucket and 2 gravel. Add a
    Transmitter (shapeless) for the Wireless one.

## Auto Harvester

A Create-style botany pot: the Cobblestone Generator's box with a planter behind the glass. Plant a crop or a sapling and it grows and harvests it over and over; the plant itself is never used up.

- **Planting**: use a seed, crop or sapling on it, or open its screen with a **wrench** (sneak-wrench still picks it up) and put one in the planter slot. Funnels and arms can plant it while it's empty. Empty-handed, use takes the harvest out; sneak-use takes the plant back.
- **What it takes**: any vanilla crop (wheat, carrots, beetroot, melon and pumpkin seeds, pitcher pods, sweet berries, nether wart, sugar cane, cactus, bamboo, cocoa, kelp...), flowers small and two-tall, mushrooms, every sapling and both nether fungi, and modded crops built the same way (Supplementaries' flax, Farmer's Delight's cabbages and onions...). Grass, ferns, dead bushes, roots, sprouts, seagrass and lily pads are refused. Item tags `comedymonkeysaddons:auto_harvester_plantable` and `auto_harvester_blacklist` add or forbid plants.
- **What it gives**: a crop gives what it would drop fully grown (wheat and the odd seed, a melon's slices...); a tree gives 4 to 6 of its logs plus a few rolls of its leaves' drops (saplings, sticks, apples), and a nether fungus a huge fungus's stems, wart blocks and the odd shroomlight. It holds nine stacks, taken out from any side by hoppers, chutes, funnels, pipes or arms; full, it stops.
- **Power**: a shaft into the back. It always uses **512 SU** and runs **no faster** for more speed. While empty or full it puts no stress on the network.
- **Speed**: a crop grows in 40 seconds, scaled by how fast it grows in vanilla (sweet berries and sugar cane about three times as fast, beetroot a little slower); a tree takes 80 seconds. Each harvest then takes 4 seconds: the head drops, sweeps the planter with a harvester reel (crops) or a saw (trees), and parks.
- **Bountiful** (I to III): an enchantment for the Auto Harvester only, put on with an anvil and kept when it's broken and placed again. Each level grows the plant 50% faster (2.5x at III). Enchanting tables don't offer it: buy the book from a **Librarian**, in their usual random book trades or as an extra Apprentice trade.
- **The window** shows the real plant at its growth stage (flowers and other plants without stages grow in size), and a sapling growing into a little tree of its own wood.
- **Wrench screen**: the planter slot, the harvest, and a growth gauge that shows its status, time per harvest and Bountiful level.
- Recipe: andesite alloy in the corners, a Mechanical Harvester above, a Mechanical Saw below, dirt either side and Andesite Casing in the middle.

## Animal Pens

A replacement for sprawling mob pens: one block that keeps a herd of one kind of animal, breeding and behaving as a vanilla pen would. Same box as the Auto Harvester, with straw on the floor and a feeding trough at the back, and the real animals wandering about behind the glass.

- **Capture Bottle**: use it on any animal that breeds (modded ones too) to catch it; someone else's tamed pet can't be taken. The bottle shows the animal standing inside it, and keeps its colour, name and variant. Use it on the ground to let the animal out again. Empty bottles stack; a full one holds one animal. Recipe: a copper nugget on a glass bottle.
- **Starting a herd**: use the Bottled Animal on a pen (one is enough), then put its breeding food in the trough by hand or with a funnel. The empty trough shows which food it eats.
- **Breeding**: with a shaft turning in the back, every **five minutes** each adult eats one food and has a baby. Babies grow up in **twenty minutes**, as in vanilla, and then breed too. The **Animal Pen** holds **128**, the **Brass Animal Pen** **512**; the trough (2 slots, 8 on the Brass pen) holds food for every animal the pen can hold.
- **Only adults count**, as with vanilla animals: only they breed, give drops and XP, grow wool, lay eggs or make milk.
- **Sword**: use a sword on the pen to take **half the adults** (rounded, always leaving one to breed), with their drops as if you killed them, so **Looting** and **Fire Aspect** count, and their XP. The young are left to grow up.
- **Shears** take every adult sheep's wool (it grows back in a minute); a **bucket** takes milk from cows and goats, and pipes can draw the milk out; a **bowl** takes stew from mooshrooms; chickens lay **eggs** and armadillos shed **scutes** by themselves.
- **Deployers** can use swords, shears and buckets on it, so a farm can run itself. A comparator reads how full of animals it is.
- **Power**: it always uses the same stress (**256 SU**, **1,024 SU** for the Brass pen) at any speed, and none while empty. It only breeds, grows and produces while it turns.
- **Output**: drops, wool and eggs go into a 9-slot buffer (18 on the Brass pen), emptied from any side by hoppers, chutes, funnels or arms, or empty-handed by hand.
- **Wrench screen**: the animal's bottle (take it out to empty the pen; the other adults are butchered into the output), the trough, a breeding gauge with the time until the next litter grows up, a milk gauge, the output, and a count of adults and young.
- **Fruitful** (I to III): an enchantment for the pens only, put on with an anvil and kept when the pen is broken. Each level breeds and grows the herd 50% faster (2.5x at III). Buy the book from a **Librarian**.
- Recipes: Animal Pen: andesite alloy corners, wooden fences, Andesite Casing and a hay bale. Brass Animal Pen: Mechanical Crafting around an Animal Pen, with brass sheets, Brass Casing, electron tubes and hay bales.

## Cuckoo Clock

A Black Forest cuckoo clock, two blocks tall, in dark walnut and brass: a shingled roof, a dial whose hands keep the game's time, a window above it where a sun and moon disc turns once a day, a brass cog turning behind a porthole on each side, and a pendulum and pine-cone weights hanging into the block below.

- **Placing**: it hangs on the wall you click, facing out from it, and needs the block below free for its weights. The **shaft** goes into the **middle of its back** (usually through the wall). It always uses the same stress, **1,024 SU** by default (a windmill of about 16 sails), at any speed.
- **Restocks**: while it turns, every villager within **64 blocks** (across the ground, and as far up and down) restocks at **8:00** and at **12:00** game time instead of vanilla's times. As in vanilla they still have to get to their workstation, so each restocks on its first trip there after the hour. Villagers out of reach, or near a clock that has stopped, keep the vanilla rules.
- **On the hour**: at 8:00 and 12:00 it chimes, the doors at the bottom open and a **carved villager** rolls out onto the balcony with a little **workstation**, picked at random from the workstations within reach (with its profession's work sound), and the **cuckoo** comes out of the gable and calls the hour: eight times, or twelve.
- **Stopped**, the hands, the sun and moon and the pendulum stop where they are, and there's no show.
- **Engineer's Goggles** show the time, the next restock and how long until it, and how many traders it reaches (grown villagers with a job).
- Recipe: dark oak planks over brass sheets either side of a Clock, over chains either side of a Precision Mechanism.

## Trade Bot

A one-wheeled clockwork shopkeeper with big binocular eyes. He lives docked in his **winding station**, and sells your goods to villagers, by himself, for emeralds.

- **Placing**: he comes as one item, docked in his station; place it and he's yours. Only you (and your team) can open his station or change his jobs, and you can have **4** of him. **Sneak-click the station with a Wrench** to pack them both back into the item, jobs and all; nothing else can break it.
- **Power**: feed **Forge Energy into the station from underneath** (a cable such as a Pipez energy pipe, or a generator that outputs upward). It holds 100,000 FE and winds him up at **64 FE a tick** while he's docked. He uses **4 FE a block** and **50 FE a trade**, so a full charge of 100,000 FE carries him about 25,000 blocks; he heads home with just enough to get back.
- **Linking**: right-click his station for its screen, then **Select containers** and right-click containers in the world. The first click makes one an **input** he takes goods from (up to 4); a second click makes it his **output** if he hasn't one yet. Left-click unlinks; a belt can only be an output. They must be within **64 blocks** of his station.
- **Trading**: by day, when a **villager sitting on a Create seat** (or a wandering trader) within 64 blocks buys something in his inputs, he sets off. He takes only **whole lots** of what they'll buy, rolls to each buyer nearest first, and trades from **two blocks away, across its workstation**: in one animation, every trade he can pay for, at the villager's **real prices and stock**. Villagers gain XP and level up as usual, and new trades that opens up he sells on the same trip. His own **reputation** with each villager grows with every trade and earns him a discount, up to vanilla's cap.
- **Bringing it home**: his output only ever gets his earnings: **emeralds**, and **Nuggets of Experience** for the XP the trades would have dropped (1 per 3 XP). What didn't sell goes back to the input it came from. If the output is full he waits beside it.
- **Getting around**: he rolls over anything up to 4 pixels high and hops slabs, blocks and stairs, opens doors and fence gates (and shuts the ones he opened), but can't climb ladders. He keeps out of water and lava.
- **Players first**: if someone is trading with his villager he waits his turn; if the villager stands up or goes, he shrugs and tries someone else.
- **Feeling alive**: he's scared of monsters (they ignore him, but he doesn't know that): he flees to a corner, hides and recovers. He greets you, glares if you hit him, and bounces after a good day's trading. At **no health** he **winds down** instead of dying; wind him back up with a Wrench. Pushed into water, he winds down until he's dry; wound down in lava or fallen out of the world, he's put back by his station.
- **Redstone**: a signal on his station keeps him home. A comparator reads **5** while he's docked, **10** while he's out working and **15** when he needs you (low on charge, or wound down).
- **Engineer's Goggles** on his station show his charge, what he's doing, his containers and today's trades. `/tradebot trade` sends him out now.
- Recipe: 5x5 Mechanical Crafting: brass sheets, Electron Tubes, Precision Mechanisms, an Emerald Block, Andesite and Brass Casing, Cogwheels and an **FE Coil** (4 copper sheets around redstone dust).

## Magma Engine

A lava-burning V-twin: by default **64 RPM and 8,192 SU**, four times a Portable Engine, for **10 mB of lava a second**. The shaft comes out of the middle of its front, which points at you when placed, any way up, sideways, up or down (sneak to point it away). It holds 4 buckets; pipes fill it on **any side but the front** (a Fluid Wellspring with Lava Upgrades runs it forever), or right-click with a lava bucket. While it burns, its two pistons pump in and out of their finned cylinders, the spoked flywheel turns with the shaft, the firebox glows through the spokes, and each exhaust chugs a puff of smoke in time. With no lava it sputters to a stop. Speed, capacity and lava use are configurable.

Display Links can read its **lava level** (millibuckets and percent) and **output** (RPM and stress units, or "Out of lava").

**Rotation direction:** like Simulated's Portable Engine, the value box on top of the drum at the back sets the shaft turning clockwise or counter-clockwise.

Recipe: an iron sheet over an Engine Assembly (Create Aeronautics) over a Blaze Burner, like a Portable Engine. Without Aeronautics it's brass sheets, a Steam Engine, 2 Fluid Tanks, Brass Casing and a Blaze Burner instead.

### Supercharged Magma Engine

Twice a Magma Engine in every way: **128 RPM and 16,384 SU** for **20 mB of lava a second**, with an 8-bucket tank. It's the same engine with brass trim and a belt-driven supercharger whining away on the front, and works the same way (placement, pipes on any side but the front, Display Links). Speed, capacity and lava use have their own config section.

Recipe: a Precision Mechanism over a Magma Engine over Brass Casing.

## String Tangler

A copper-trimmed machine with a Create whisk on the shaft inside. Every **4 string and 1 slime ball** become a **cobweb**: the
string pays out from two spools (their winding shows how much is loaded) onto the whisk as it spins, the slime sits in a
little pot, and the web balls up on the whisk until it drops into the tray at the front. Power goes in
through the back (the glass front faces you when placed); faster rotation tangles faster (160 ticks at 16 RPM, half a second
at 256 RPM). Hoppers, funnels and pipes load both ingredients and take the cobwebs from any side; by hand, right-click with
string or slime to load it and with an empty hand to take the cobwebs.

Recipe: 4 Copper Casing, 3 Framed Glass, a Whisk and a shaft.

## Stone Infuser

Makes Create's ore-bearing stones from **cobblestone and 9 nuggets** of the matching metal:

| Nuggets | Stone |
|---|---|
| 9 Zinc Nuggets | Asurine |
| 9 Iron Nuggets | Crimsite |
| 9 Gold Nuggets | Ochrum |
| 9 Copper Nuggets | Veridium |

Crushing one of these stones gives some nuggets back, but always fewer than 9 (veridium about 8, the others 2 to 4), so it
can't loop into free metal. Crushing **tuff** gives a few of all four nuggets, so a Tuff Generator can feed it. Through the
window you can watch the cobblestone turn on a turntable geared to the shaft while metal dust falls on it from the nugget
feeder and the new stone creeps up over it; finished stones wait in the tray at the front. Power goes in through the back; 200 ticks at 16 RPM.
Loading and unloading work as for the String Tangler.

Recipe: Andesite Casing, Framed Glass, iron sheets, a Basin and a shaft.

Both machines' recipes are data-driven (`comedymonkeysaddons:tangling` and `comedymonkeysaddons:infusing`, each with an
`ingredient` and `count`, an `extra` and `extra_count`, a `processing_time` and a `result`), and JEI shows them.

## Incinerator

A squat steel furnace with a grated firebox mouth and a round hot plate on top. Anything put into it is gone: items from funnels, chutes, hoppers or pipes, and any fluid from pipes, from any side. It never fills up, so it's a safe end for overflow lines. Everything burns, water too, and the fire ramps up and down: each item, or each tenth of a bucket, adds a point of heat, and it loses a point a second. At 10 points it's **lit**, and it stays lit until it has cooled right down, so feed it at least one thing a second to keep it hot; stop, and it's cold within 10 seconds. Lit, its embers glow behind the grate and its hot plate glows from the middle out, and under a **Steam Engine boiler** it heats it like a lit Blaze Burner (cold, it counts as no heat). Goggles show how much it has burned.

Recipe: Andesite Alloy, a Magma Block, a Furnace and Andesite Casing.

## Compact Gyroscope, Torque Rotor and Flight Instruments (Create Aeronautics)

Only present when Sable (Create Aeronautics) is installed.

- **Compact Gyroscope**: the Gyroscope Stabilizers' Gyroscope in a single block, a flywheel spinning in a brass gimbal inside a riveted steel and walnut cage with portholes on every side. The gimbal's rings swing so the flywheel stays level with the world while the ship tilts round it, and brake calipers close on its rim as a redstone signal rises. It runs the same controller with the same default strength, damping and lean correction: it pushes the ship back upright and calms its wobble, reaching full strength at 256 RPM (16 SU per RPM). It takes a shaft from above or below. Recipe: a Flywheel, a Precision Mechanism, a shaft, 4 Brass Casing and 2 Framed Glass. With Gyroscope Stabilizers installed you can also convert its Gyroscope by crafting it on its own (and turn a Compact one back the same way). Goggles show its spin, how hard it's correcting and the ship's tilt. A **redstone signal brakes it**: the stronger the signal, the slower the flywheel spins and the less it holds the ship, and a full-strength signal stops it (the shaft keeps turning).
- **Flight Instruments**: a sloping riveted steel console with walnut trim and a glare hood, and four live brass-ringed dials: an altimeter (long hand per 100 m, short per 1000 m), airspeed (0 to 60 m/s), an artificial horizon that tilts with roll and drops with pitch, and heading. Place it facing you; the way you face it counts as the ship's forward. Goggles show every reading, and Display Links can put **altitude, airspeed, climb rate, heading or pitch and roll** on Display Boards and Nixie Tubes (refreshed every second). Recipe: iron sheets, a compass, 2 clocks, a Precision Mechanism and an electron tube.
- **Torque Rotor**: a three-bladed airship tail rotor (laminated walnut blades with brass leading edges on a riveted steel nacelle) that turns the ship instead of pushing it. Its blades push the way they face (or pull, with the shaft reversed) exactly as hard as an Andesite Propeller at the same speed, and the ship turns about its centre of mass just as it would from a propeller in that spot, but without being pushed, so there's no sideways drift. Put it on the tail like a tail rotor, facing sideways, to yaw: the further from the middle, the harder it turns. It mounts on the face you click, with the shaft going into its base, and blows wind like a propeller. Recipe: iron sheets, Andesite Casing, 2 shafts and a Precision Mechanism.

## Electricity: Lava Generator, Geothermal Turbine and Electric Motor

Burn lava for Forge Energy (FE), and turn FE back into rotation. All the numbers are configurable.

| | Takes | Gives |
|---|---|---|
| **Lava Generator** | 20 mB/s lava, in at the middle of the back | **1,024 FE/t**, out of the terminals in the middle of each side |
| **Geothermal Turbine** (3x3x3) | 90 mB/s lava and 250 mB/s water, through the ports under its base | **46,080 FE/t**, through the ports under its base, into anything under it |
| **Electric Motor** | 1 FE/t for every 16 SU it's set to give, in through its base (the flange opposite the shaft) | Up to **294,912 SU** (a full superheated boiler) |

- **Lava Generator**: a small lava-fired turbine; its wheel spins behind the brass-ringed window, faster the more power is taken, and the lava glows behind the glass in front while it burns. It pushes its power into whatever is on its sides (an Electric Motor, or a Pipez energy pipe). One generator runs a motor at 16,384 SU.
- **Geothermal Turbine**: build a 3x3x3 of Geothermal Casings with a Turbine Core in the very middle and the Controller in the middle of the front, facing you. It forms into one big machine within a second. Lava heats water to steam that turns the turbine: ten times as efficient with lava as the Lava Generator, so two fully upgraded Lava Wellsprings and a water supply run it, and it powers two and a half motors flat out. Its gauges show the lava and water, and while it runs it steams and its vents flicker with flame.
- **Electric Motor**: a flange-mounted motor that takes power through its base, opposite the shaft, in any facing; its lamp is cyan while it runs and red when it's short of power. Right-click it with a wrench for its settings. Speed snaps to Create's standard speeds (1 to 256 RPM); stress snaps to what the generators make (a quarter or half of a Lava Generator, whole generators, up to one Geothermal Turbine), and the screen shows the power it will draw. It stops when it runs short of power and restarts once it has a reserve stored (30 seconds' worth, or a few seconds' at high stress), so a weak supply makes it pause now and then rather than stutter the whole network.
- Recipes: Lava Generator: brass sheets, an electron tube, 2 Fluid Tanks, a Propeller, Industrial Iron and a Blaze Burner. Electric Motor: brass sheets, an electron tube, copper, a Precision Mechanism, Industrial Iron and Brass Casing. Geothermal Casing (4): iron sheets, Industrial Iron and Copper Casing. Controller: brass sheets, electron tubes, Precision Mechanisms and a casing. Turbine Core: 4 Precision Mechanisms, 4 Propellers and a Flywheel.

## Pipez Covers (with Pipez)

Copycat-style covers for Pipez pipes, to hide pipework inside walls and floors. The pipe keeps working underneath.

- **Snug Pipez Cover**: hugs the pipe a pixel proud all round, following its core, every connected arm and the collars on extracting sides.
- **Full Pipez Cover**: fills the whole block, which becomes solid: you can stand on it, build against it and click anywhere on it.
- **Right-click a pipe** with a cover to fit it (the other kind swaps it). **Right-click a covered pipe with any full block** to dress the cover in it, like a Create copycat: one block is used, and given back when you change it. Tinted blocks (grass, leaves) keep their colours, and see-through blocks like glass still show the pipe inside.
- **Sneak + right-click with a wrench** takes the block off, then the cover. Breaking the pipe drops both.
- Clicking the face where a pipe meets the cover still opens Pipez's settings for that side.
- Engineer's or Aviator's Goggles on a covered pipe show what kind of pipe it is, and its cover.
- Recipe: a Zinc Ingot on a Stonecutter makes 4 Snug or 2 Full covers.

## Compatibility

- **Create contraptions**: upgraded chests keep their stack sizes while moving (drills, harvesters and portable interfaces fill them, and they open as chests on the contraption). Chests and barrels can be **upgraded while on a moving contraption** too: right-click them with a chest upgrade.
- **Fluid Wellsprings on contraptions** keep generating while they move, and contraption pumps, spouts and portable fluid interfaces can use their tank. The fluid (and upgrades) are back in the block when it's placed again.
- **Pressure Crucible**: needs real rotation, so it presses on the ground and on Aeronautics physics objects. On a Create contraption it just rides along, keeping its contents.
- **Create Aeronautics / Sable physics objects**: the Wellspring, upgraded chests and barrels, the generators, the Magma Engine and the Pressure Crucible keep working and keep their contents when assembled and disassembled.
- **Carry On**: the Wellspring and upgraded chests and barrels can be carried and placed back with their contents. (Sneak + empty hand is Carry On's pickup, which is why the upgrade screens open with a Wrench.)
- **Pipez + Blaze Burners**: with Pipez installed, item pipes can feed fuel into Blaze Burners. Works alongside Create: Liquid Fuel.
- **Pipez + Portable Engines** (Create Aeronautics): item pipes feed coal and other burnable items, and fluid pipes feed lava and other fuel fluids (any fluid whose bucket burns). 1000 mB gives the same burn time as a bucket; engines are topped up to one bucket's worth at a time.
- **Curios + Aviator's Goggles**: wear them in the head slot (over a helmet if you like); they still count as goggles for Create's overlays.
- **Modded crops** (Supplementaries, Farmer's Delight and others): any crop built on vanilla's crop or bush blocks grows in the Auto Harvester with its own drops, growth stages and model. Two-block crops like flax show both halves once they're tall enough.

Ponder scenes (hold **W**) cover the Wellspring, the chest and barrel upgrades, the Pressure Crucible, the generators (Basalt, Blackstone and Tuff get their own scenes too), both Magma Engines, the Incinerator, the String Tangler, the Stone Infuser, the Lava Generator, the Electric Motor, the Geothermal Turbine, the Auto Harvester and the Animal Pens, and with Sable the Compact Gyroscope, Torque Rotor and Flight Instruments.

An advancement tab follows the mod from the first Upgrade Template to a maxed-out Wellspring, a Netherite chest, a full-power Electric Motor, the Auto Harvester and a Brass Animal Pen of 512.

## Configuration

Mods → Create → Config → *Access configs of other mods* → ComedyMonkey's Create Addons (server settings can be edited while in a world).

- **Fluid Wellspring**: base tank capacity, capacity per upgrade, max upgrades, and water, lava, honey and chocolate generation rates.
- **Chest Upgrades**: Copper, Iron, Gold and Netherite stack multipliers (Diamond uses Gold's).
- **Pressure Crucible**: stress impact per RPM.
- **Machines**: String Tangler and Stone Infuser stress impacts.
- **Cobblestone Generators**: Andesite and Brass stress impacts (also used by the Basalt and Blackstone ones).
- **Auto Harvester**: stress (SU, at any speed), crop and tree grow times, Bountiful's speed bonus per level, logs per tree and leaf drop rolls.
- **Animal Pens**: stress and max animals for each pen, seconds per breeding and to grow up, Fruitful's speed bonus per level, wool regrowth, egg time and milk per animal.
- **Cuckoo Clock**: stress (SU, at any speed) and how far it reaches villagers (blocks).
- **Trade Bot**: his range from the station, bots per player, the station's charge rate and buffer, a full charge, FE per block and per trade, and how near a monster has to be to scare him.
- **Magma Engine** and **Supercharged Magma Engine**: each has its own speed, stress capacity per RPM and lava burned per second.
- **Electricity**: the Electric Motor's maximum stress and FE drawn per SU; the Lava Generator's and Geothermal Turbine's FE per tick and lava (and the turbine's water) per second.
- **Create Aeronautics**: Compact Gyroscope strength, damping, lean correction, full-strength speed and stress; Torque Rotor thrust per RPM and stress.
- **Mod Compatibility**: Pipez Blaze Burner feeding, whether only Pipez (not hoppers or funnels) may feed them, and Portable Engine fluid fuel.
- **Aviator's Goggles** (client): show goggles in the Curios slot and over helmets.

## Building

```
./gradlew build      # jar in build/libs/
./gradlew runClient  # dev client with Create, Curios, JEI, Pipez, Create Aeronautics (and Sable), Liquid Fuel, Carry On,
                     # Inventory Profiles Next and Supplementaries
```

Textures are original pixel art in Create's copper, brass and steel palette. The creative tab icon is ComedyMonkey's own skin.
