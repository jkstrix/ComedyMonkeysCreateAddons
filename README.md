# ComedyMonkey's Create Addons

A NeoForge add-on for **Create** (Minecraft 1.21.1). Mod id: `comedymonkeysaddons`.

- **Fluid Wellspring**: an upgradeable Create-style tank that generates water or lava.
- **Chest Upgrades**: Copper, Iron, Gold and Diamond upgrades that raise a chest's or barrel's stack size (Diamond adds slots too).
- **Pressure Crucible**: presses Emerald Dust and Netherite Dust (rare drops from crushing cobblestone and netherrack) into Emeralds and Netherite Scrap.
- **Cobblestone, Basalt, Blackstone and Tuff Generators**: one-block stone generators, powered by rotation.
- **Magma Engine**: a lava-burning engine with four Portable Engines' worth of power.
- **String Tangler** (cobwebs from string and slime) and **Stone Infuser** (Create's ore-bearing stones from cobblestone and metal nuggets).
- **Incinerator**: destroys any item or fluid, and heats a boiler while it burns.
- **Compact Gyroscope** and **Torque Rotor** for Create Aeronautics ships: stay level, and turn without strafing.
- **Aviator's Goggles in the Curios head slot** (with Create Aeronautics).
- **Pipez can feed Blaze Burners** (with Pipez).

Everything works on Create contraptions and Create Aeronautics physics objects, and can be carried with Carry On.

## Requirements

| Mod | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.248 (21.1.228+) |
| Create | 6.0.10+mc1.21.1 |
| Curios API | 9.5.1+1.21.1 |

Optional: Create Aeronautics 1.3.2 (goggles in the Curios slot; Sable for the Compact Gyroscope and Torque Rotor), Create Aeronautics: Gyroscope Stabilizers (optional: converts into the Compact Gyroscope), Pipez (Blaze Burner feeding). Tested alongside Create: Liquid Fuel and Carry On.

## Upgrade Template

All upgrades start from an **Upgrade Template**, made on a Create sequenced assembly line:
copper sheet → deploy Cogwheel → deploy Glass → deploy Electron Tube.

## Fluid Wellspring

A copper-framed tank with glass windows that show the fluid level (notches mark 25%, 50% and 75%). Holds water or lava; fill or empty it with buckets, pumps or pipes from any side.

| Upgrade | Sequenced assembly on an Upgrade Template | Effect |
|---|---|---|
| Water Upgrade | 2 Pointed Dripstone (makes a **Drip Template**), then spout 250 mB water onto it | Generates water |
| Lava Upgrade | 2 Pointed Dripstone (makes a **Drip Template**), then spout 250 mB lava onto it | Generates lava |
| Capacity Upgrade | 3 Fluid Tanks | Enlarges the tank |
| Honey Upgrade | Drip Template, then **3 loops** of: deploy a Honeycomb Block, spout 1000 mB honey, deploy a Precision Mechanism, press | Generates Create honey |
| Chocolate Upgrade | Drip Template, then **3 loops** of: deploy Cocoa Beans, spout 1000 mB chocolate, deploy a Precision Mechanism, press | Generates Create chocolate |

- Honey and chocolate meet lava as limestone and scoria (Create's own fluid interactions), so a Honey or Chocolate Wellspring next to a lava one is an endless limestone or scoria source. They generate 10/25/50 mB/s with 1/2/3 upgrades (configurable).
- Up to 3 generation upgrades of one kind (lamps on top show how many) and up to 3 Capacity Upgrades.
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
- **Sort button**: upgraded chests and barrels have a small sort button at the top right of their screen. It merges stacks up to the slot's real limit and orders them by item. (Inventory Profiles Next sorts by simulating clicks with vanilla stack sizes, which oversized stacks break, so it only manages the player-inventory side of these screens.)
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

- The Crucible sits on a Blaze Burner and takes rotation through a shaft into its back wall (it faces away from you when placed, or backs onto a shaft next to it). It presses faster at higher speeds (16 RPM is the base speed, 256 RPM is 16x), with a heavy stress impact (12 SU per RPM by default).
- At 64 RPM an Emerald takes 2.5 seconds, a Netherite Scrap 10 seconds, and glass or stone half a second. The press plate lowers over exactly that time for each item.
- **Right-click with dust** to load it, **right-click with an empty hand** to take the results out. Funnels, hoppers and pipes insert dust and extract results from any side.
- Engineer's Goggles show the contents, the heat needed, progress and what's holding it up.
- Crafting recipe: 4 obsidian, a Mechanical Press, 2 brass sheets, a cogwheel and a Basin.
- JEI shows the Crucible recipes; Create's crushing and mixing pages show the dust chances and the netherrack recipe. All of them are ordinary data-pack recipes, so packs can change the chances, counts and times.

## Cobblestone Generators

One-block, lava-and-water cobblestone generators with a glass front onto a brick chamber. The front faces you when placed and you can watch it work: water and lava pour from inlet pipes into the basin and a block of cobblestone rises between them, with puffs of steam; the pours stop when it's unpowered or full. The lava glows. Power goes in through the **shaft hole on the back**, the only side that takes a shaft.

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

## Magma Engine

A heavy brass engine that burns lava: by default **64 RPM and 8,192 SU**, four times a Portable Engine, for **10 mB of lava a second**. The shaft comes out of the middle of its front, which faces you when placed (sneak to place it facing away). It holds 4 buckets; pipes fill it through the back or the bottom only (a Fluid Wellspring with Lava Upgrades runs it forever) or right-click with a lava bucket. With Create Aeronautics installed it takes the shape of Simulated's Portable Engine in a dark steel finish with magma glowing in the firebox: the hatch swings open while it burns, the exhausts glow red-hot and pulse with the shaft, and smoke comes from the outlets. (The geometry is loaded from Aeronautics at runtime; only our texture ships.) Without Aeronautics it has its own model, a dark steel block with a gold cog round the shaft and three cylinders whose heads bob in turn. Either way, with no lava it stops. Speed, capacity and lava use are configurable.

Display Links can read its **lava level** (millibuckets and percent) and **output** (RPM and stress units, or "Out of lava").

**Rotation direction:** like Simulated's Portable Engine, the value box on top (on the back without Aeronautics) sets the shaft turning clockwise or counter-clockwise.

Recipe: an iron sheet over an Engine Assembly (Create Aeronautics) over a Blaze Burner, like a Portable Engine. Without Aeronautics it's brass sheets, a Steam Engine, 2 Fluid Tanks, Brass Casing and a Blaze Burner instead.

### Supercharged Magma Engine

Twice a Magma Engine in every way: **128 RPM and 16,384 SU** for **20 mB of lava a second**, with an 8-bucket tank. It looks the same but with brass trim, and works the same way (placement, filling from the back or bottom, Display Links). Speed, capacity and lava use have their own config section.

Recipe: a Precision Mechanism over a Magma Engine over Brass Casing.

## String Tangler

A copper-framed glass box with a spindle inside. Every **4 string and 1 slime ball** become a **cobweb**: while it works the
strands spin with the shaft, are pulled in and twisted tighter and tighter, and a web balls up in the middle. Power goes in
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
window you can watch the new stone creep up over a block of cobblestone. Power goes in through the back; 200 ticks at 16 RPM.
Loading and unloading work as for the String Tangler.

Recipe: Andesite Casing, Framed Glass, iron sheets, a Basin and a shaft.

Both machines' recipes are data-driven (`comedymonkeysaddons:tangling` and `comedymonkeysaddons:infusing`, each with an
`ingredient` and `count`, an `extra` and `extra_count`, a `processing_time` and a `result`), and JEI shows them.

## Incinerator

Anything put into it is gone: items from funnels, chutes, hoppers or pipes, and any fluid from pipes, from any side. It never fills up, so it's a safe end for overflow lines. While something burned in the last 5 seconds it's **lit**, and a lit Incinerator under a **Steam Engine boiler** heats it like a lit Blaze Burner (cold, it counts as no heat). Goggles show how much it has burned.

Recipe: Andesite Alloy, a Magma Block, a Furnace and Andesite Casing.

## Compact Gyroscope and Torque Rotor (Create Aeronautics)

Only present when Sable (Create Aeronautics) is installed.

- **Compact Gyroscope**: the Gyroscope Stabilizers' Gyroscope in a single block, a flywheel spinning inside a brass-framed glass case. It runs the same controller with the same default strength, damping and lean correction: it pushes the ship back upright and calms its wobble, reaching full strength at 256 RPM (16 SU per RPM). It takes a shaft from above or below. Recipe: a Flywheel, a Precision Mechanism, a shaft, 4 Brass Casing and 2 Framed Glass. With Gyroscope Stabilizers installed you can also convert its Gyroscope by crafting it on its own (and turn a Compact one back the same way). Goggles show its spin, how hard it's correcting and the ship's tilt.
- **Flight Instruments**: a sloping cockpit console with four live dials: an altimeter (long hand per 100 m, short per 1000 m), airspeed (0 to 60 m/s), an artificial horizon that tilts with roll and drops with pitch, and heading. Place it facing you; the way you face it counts as the ship's forward. Goggles show every reading, and Display Links can put **altitude, airspeed, climb rate, heading or pitch and roll** on Display Boards and Nixie Tubes (refreshed every second). Recipe: iron sheets, a compass, 2 clocks, a Precision Mechanism and an electron tube.
- **Torque Rotor**: a propeller that turns the ship instead of pushing it. Its blades push the way they face (or pull, with the shaft reversed) exactly as hard as an Andesite Propeller at the same speed, and the ship turns about its centre of mass just as it would from a propeller in that spot, but without being pushed, so there's no sideways drift. Put it on the tail like a tail rotor, facing sideways, to yaw: the further from the middle, the harder it turns. It mounts on the face you click, with the shaft going into its base, and blows wind like a propeller. Recipe: iron sheets, Andesite Casing, 2 shafts and a Precision Mechanism.

## Compatibility

- **Create contraptions**: upgraded chests keep their stack sizes while moving (drills, harvesters and portable interfaces fill them, and they open as chests on the contraption). Chests and barrels can be **upgraded while on a moving contraption** too: right-click them with a chest upgrade.
- **Fluid Wellsprings on contraptions** keep generating while they move, and contraption pumps, spouts and portable fluid interfaces can use their tank. The fluid (and upgrades) are back in the block when it's placed again.
- **Pressure Crucible**: needs real rotation, so it presses on the ground and on Aeronautics physics objects. On a Create contraption it just rides along, keeping its contents.
- **Create Aeronautics / Sable physics objects**: the Wellspring, upgraded chests and barrels, the generators, the Magma Engine and the Pressure Crucible keep working and keep their contents when assembled and disassembled.
- **Carry On**: the Wellspring and upgraded chests and barrels can be carried and placed back with their contents. (Sneak + empty hand is Carry On's pickup, which is why the upgrade screens open with a Wrench.)
- **Pipez + Blaze Burners**: with Pipez installed, item pipes can feed fuel into Blaze Burners. Works alongside Create: Liquid Fuel.
- **Pipez + Portable Engines** (Create Aeronautics): item pipes feed coal and other burnable items, and fluid pipes feed lava and other fuel fluids (any fluid whose bucket burns). 1000 mB gives the same burn time as a bucket; engines are topped up to one bucket's worth at a time.
- **Curios + Aviator's Goggles**: wear them in the head slot (over a helmet if you like); they still count as goggles for Create's overlays.

Ponder scenes (hold **W**) cover the Wellspring, the chest and barrel upgrades, the Pressure Crucible, the generators (Basalt and Blackstone get their own scene too), the Magma Engine, the Incinerator, and with Sable the Compact Gyroscope and Torque Rotor.

## Configuration

Mods → Create → Config → *Access configs of other mods* → ComedyMonkey's Create Addons (server settings can be edited while in a world).

- **Fluid Wellspring**: base tank capacity, capacity per upgrade, max upgrades, water and lava generation rates.
- **Chest Upgrades**: Copper, Iron, Gold and Netherite stack multipliers (Diamond uses Gold's).
- **Pressure Crucible**: stress impact per RPM.
- **Machines**: String Tangler and Stone Infuser stress impacts.
- **Cobblestone Generators**: Andesite and Brass stress impacts (also used by the Basalt and Blackstone ones).
- **Magma Engine**: speed, stress capacity per RPM and lava burned per second.
- **Create Aeronautics**: Compact Gyroscope strength, damping, lean correction, full-strength speed and stress; Torque Rotor thrust per RPM and stress.
- **Mod Compatibility**: Pipez Blaze Burner feeding, whether only Pipez (not hoppers or funnels) may feed them, and Portable Engine fluid fuel.
- **Aviator's Goggles** (client): show goggles in the Curios slot and over helmets.

## Building

```
./gradlew build      # jar in build/libs/
./gradlew runClient  # dev client with Create, Curios, JEI, Pipez, Create Aeronautics, Liquid Fuel and Carry On
```

Textures are original pixel art in Create's copper, brass and steel palette. The creative tab icon is ComedyMonkey's own skin.
