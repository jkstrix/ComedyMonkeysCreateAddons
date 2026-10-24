# Release 2.0, stage 1: Trade Bot

ComedyMonkey's Create Addons 2.0.0, for Minecraft 1.21.1, NeoForge 21.1.248+ and Create 6.0.10.

Stage 1 of 2.0 adds **Trade Bot**: a one-wheeled clockwork robot who sells the goods in your chests to villagers sitting on Create seats, and brings the emeralds home.

## New

### Trade Bot
- **What he is:** a clockwork shopkeeper with big binocular eyes. He lives docked in his **winding station**, and you place and pack them as one item.
- **Ownership:** he's yours. Only you and your team can open his station or change his jobs, and each player can have up to 4 bots.
- **Power:** feed Forge Energy into the station **from underneath**. It stores 100,000 FE and winds him up at 64 FE a tick while he's docked. He uses 4 FE a block and 50 FE a trade, so a full charge carries him about 25,000 blocks.
- **Linking:** right-click the station, choose **Select containers**, then click containers in the world. The first click makes a container an input; a second click makes it his output. He takes up to 4 inputs and 1 output, within 64 blocks.
- **Trading:**
  - **When he goes:** by day, he sets off by himself when a seated villager or a wandering trader in range buys something in his inputs.
  - **What he takes:** only whole lots of what they'll buy.
  - **How he trades:** from two blocks away, across the villager's workstation. In one animation he makes every trade he can pay for, at the villager's real prices and stock.
  - **Villagers:** they gain XP and level up as usual, and new trades that opens up he sells on the same trip.
  - **Reputation:** his reputation with each villager earns him a discount.
- **Earnings:** his output only ever gets emeralds, plus **Nuggets of Experience** for the XP the trades would have dropped. Anything that didn't sell goes back to the input it came from.
- **Getting around:**
  - he rolls over small steps and hops slabs, blocks and stairs;
  - he opens doors and fence gates, and shuts the ones he opened;
  - he keeps out of water and lava.
- **Players come first:** he waits his turn if someone is trading with his villager, and shrugs and moves on if the villager leaves.
- **Personality:**
  - he's scared of monsters: he flees to a corner, hides and recovers;
  - he greets players and glares if you hit him;
  - he bounces after a good day's trading.
- **He never dies:** at no health he winds down, and a Wrench winds him back up. Water winds him down until he's dry. If he's wound down in lava or falls out of the world, he's put back by his station.
- **Redstone:** a signal keeps him at home. A comparator reads 5 while he's docked, 10 while he's working, and 15 when he needs you.
- **Goggles:** Engineer's Goggles on the station show his charge, his task, his containers and today's trades.
- **Command:** `/tradebot trade` sends him out now.
- **Sound:** 80 sounds with subtitles, including loops for rolling, fleeing and trembling.
- **Recipe:** 5x5 Mechanical Crafting from brass sheets, Electron Tubes, Precision Mechanisms, an Emerald Block, Andesite and Brass Casing, Cogwheels and an FE Coil.

### FE Coil
A new crafting part made of 4 copper sheets around redstone dust. It's the FE part of Trade Bot's recipe, and later electric machines can use it too.

### Ponder
Three new scenes for Trade Bot:
- his station and its power;
- linking his containers;
- trading with villagers.

### Advancements
Ten new advancements, from **Coiled and Ready** and **Clockwork Clerk** up to **Employee of the Month** (64 trades in a day). There's also a hidden one to find.

### Configuration
A new **Trade Bot** section in the server config sets:
- his range;
- bots per player;
- the station's charge rate and buffer;
- a full charge;
- FE per block and per trade;
- how near a monster must be to scare him.

## Requirements
No new required mods. Villagers sit on Create seats as before (the `villagersSitInSeats` setting).
