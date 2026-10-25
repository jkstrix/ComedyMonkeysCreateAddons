# Code Dossier: Cobble Generators (Cobblestone, Basalt, Blackstone and Tuff Generators)

Path shorthands used in citations:
`J/` = `src/main/java/com/comedymonkey/createaddons/` ·
`A/` = `src/main/resources/assets/comedymonkeysaddons/` ·
`D/` = `src/main/resources/data/comedymonkeysaddons/` ·
`CGR` = `J/client/CobbleGeneratorRenderer.java` · `BE` = `J/content/cobblegen/CobbleGeneratorBlockEntity.java` · `Blk` = `J/content/cobblegen/CobbleGeneratorBlock.java`.
Line numbers in block/item model JSON point at the element's `"from"` line.
All pixel coordinates are in the renderer's model space: **front (window) facing north**, x = 0 west → 16 east, y = 0 bottom → 16 top, z = 0 front → 16 back (`CGR:81`). Seen from the front, +x (east) is on the viewer's **left**.

## Identity

- **14 registry ids, not 20.** The survey's "20 ids" overcounts. The code registers 4 products × andesite/brass × plain/wireless, except that Tuff has no Brass tier:

  | Product | Andesite | Brass | Wireless Andesite | Wireless Brass |
  |---|---|---|---|---|
  | Cobblestone | `andesite_cobble_generator` (`J/registry/ModBlocks.java:74-83`) | `brass_cobble_generator` (`:86-95`) | `wireless_andesite_cobble_generator` (`:98-100`) | `wireless_brass_cobble_generator` (`:103-105`) |
  | Basalt | `andesite_basalt_generator` (`:108`) | `brass_basalt_generator` (`:109`) | `wireless_andesite_basalt_generator` (`:110`) | `wireless_brass_basalt_generator` (`:111`) |
  | Blackstone | `andesite_blackstone_generator` (`:112`) | `brass_blackstone_generator` (`:113`) | `wireless_andesite_blackstone_generator` (`:114`) | `wireless_brass_blackstone_generator` (`:115`) |
  | Tuff | `tuff_generator` (`:130`) | none | `wireless_tuff_generator` (`:131`) | none |

  The basalt, blackstone and tuff ids come from two helpers, `andesiteGenerator` and `brassGenerator`, which copy the cobblestone tiers' properties (`ModBlocks.java:118-127`). `ModBlocks.generators()` lists all 14 (`:134-139`), and `ModBlocksTest.generatorsListsEveryGenerator` checks that list (`src/test/.../registry/ModBlocksTest.java:59-65`).
- Asset count: 14 blockstates, **21 block models** (14 plus 7 `wireless_*_powered`), 14 item models, 29 own texture PNGs plus 6 shared `cobble_generator_*` PNGs (see Code Contract).
- One class for every id: `CobbleGeneratorBlock` (a `KineticBlock`, `Blk:28`), configured by `GeneratorProduct` (`J/content/cobblegen/GeneratorProduct.java:14-18`), `perCycle`, `capacity` and `wireless` (`Blk:40-47`).
- Block entity: one type, `cobble_generator`, for all 14 (`J/registry/ModBlockEntities.java:54-57`), class `CobbleGeneratorBlockEntity` (`BE:39`, a `KineticBlockEntity`).
- Renderer: `CobbleGeneratorRenderer` (`CGR:34`, registered at `J/client/ComedyMonkeysCreateAddonsClient.java:92`). Standalone model `SHAFT` (`CGR:36-37`, registered at `ComedyMonkeysCreateAddonsClient.java:114`). Wireless frequency slots: `J/content/cobblegen/TopFrequencySlot.java`.
- No menu or screen, no Flywheel visual, no `useItemOn`/`useWithoutItem` override in `Blk` (whole file). Wrenching comes from Create's `KineticBlock`/`IRotate`.
- Block items: `J/registry/ModItems.java:40-51, 68-69`. All 14 are in the creative tab (`J/registry/ModCreativeTabs.java:37-50`).
- Footprint: 1 × 1 × 1. On wireless ids the antenna sticks up to y = 26, 10 px above the block (`A/models/block/wireless_andesite_cobble_generator.json:412`).

## What It Does

1. **Place it.** The window faces the player, like a furnace (`Blk:73-76`). Only horizontal facings exist (`Blk:30`, `HORIZONTAL_FACING`).
2. **Power it from the back.** Only the face opposite the window takes a shaft (`Blk:79-82`), and it turns on the facing axis (`Blk:84-87`).
3. **Inputs: none.** No lava, water or items go in. The output handler refuses all inserts (`BE:369-372, 391-394`). Only an item capability is registered (`J/ComedyMonkeysCreateAddons.java:134-136`), so there's no fluid input. The lava and water are paid for once, as recipe buckets (`D/recipe/andesite_cobble_generator.json:4-8`).
4. **It makes stone at drill pace.** Each tick while turning, not overstressed and not idle (`BE:102-103`), it runs one tick of Create's drill logic against an imaginary block of the product (`BE:114-130`):
   - each "hit" adds max(1, breakSpeed / hardness) damage stages out of 10, where breakSpeed = |RPM| / 100 (`BE:119-120`);
   - after each hit it waits (int)(hardness / breakSpeed) ticks (`BE:129`);
   - at 10 stages it adds `perCycle` to the buffer, up to `capacity` (`BE:121-124`), and plays vanilla `STONE_BREAK` at volume 0.15, pitch 0.9 (`BE:125`). The next block starts on the next tick (`BE:123`).
   - Hardness is the product block's own (`GeneratorProduct.java:41-43`): cobblestone 2, basalt 1.25, blackstone 1.5, tuff 1.5 (`GeneratorProduct.java:9-11` javadoc).
5. **Rates.** Cycle ticks = (hits − 1) × (wait + 1) + 1 (`BE:266-275`, tested by `CobbleGeneratorBlockEntityTest.java:208-260`). The goggles show perCycle × 1200 / ticks per minute (`BE:328`).

   | Product | Ticks per cycle at 256 RPM | Andesite /min | Brass /min | Ticks at 64 RPM (Ponder speed) | Andesite /min | Brass /min |
   |---|---|---|---|---|---|---|
   | Cobblestone | 10 | 120 | 1,920 | 37 | 32.4 | 519 |
   | Basalt | 5 | 240 | 3,840 | 19 | 63.2 | 1,011 |
   | Blackstone | 10 | 120 | 1,920 | 28 | 42.9 | 686 |
   | Tuff | 10 | 120 | (no brass) | 28 | 42.9 | (no brass) |

   Tiers: Andesite makes 1 per cycle and holds 64. Brass makes 16 per cycle and holds 256 (`ModBlocks.java:83, 95, 120, 126`). Wireless versions copy their base tier (`:100, 105`).
6. **Stress.** Impact per RPM: Andesite 4, Brass 16 (config defaults `J/config/CMConfig.java:197-204`, registered per id by `perCycle` at `J/ComedyMonkeysCreateAddons.java:233-238`). That's 1,024 / 4,096 SU at 256 RPM. **Zero stress while idle** (`BE:218-226`), and the network is re-notified when idle changes (`BE:104-106, 228-233`).
7. **Idle** = switched off by redstone, or full (`BE:151-154`). **Running** = turning, not overstressed and not idle (`BE:146-149`).
8. **Redstone off switch.** Any wired neighbour signal switches it off (`Blk:99-106`, `BE:160-164`). On wireless ids, a Redstone Link signal on its two-item frequency does too (`BE:166-170, 199-209`). Either one sets the `POWERED` blockstate (`BE:172-189`).
9. **Output** comes out of any side, but only through automation: hoppers, chutes, funnels, pipes or arms. It's capped at 64 per extraction, from one slot that shows up to 64 (`J/ComedyMonkeysCreateAddons.java:134-136`; `BE:357-395`). There's no hand-out on click.
10. **Comparator** = 0 when empty, else 1 + 14 × stored / capacity (`Blk:116-124`, `BE:294-296`).
11. **Breaking** it drops the whole buffer as stacks (`Blk:108-114`, `BE:283-292`). Contraptions and Aeronautics moves keep it (`Blk:110-111`, `BE:305-310`). The loot table drops the block itself if it survives the explosion (`D/loot_table/blocks/andesite_cobble_generator.json`).
12. **Client side only:** while running, it runs its own copy of the drill countdown (`clientProgress`, `BE:94-98, 132-144`) so the window keeps step. 1 tick in 8, it puffs `WHITE_SMOKE` at (8 ± 1.6, 5.1, 6.4) px, just above the forming block (`BE:254-263`).

## What the Player Is Told

- **Names**: "Andesite / Brass / Wireless Andesite / Wireless Brass Cobblestone Generator", "… Basalt Generator", "… Blackstone Generator", "Tuff Generator", "Wireless Tuff Generator" (`A/lang/en_us.json:276, 282, 300, 310, 405, 413, 421, 431, 444, 452, 460, 470, 518, 526`).
- **Tooltip**: "A sealed, lava-fed _cobblestone generator_ in one block. Makes one cobblestone each time a _Mechanical Drill_ at the same speed would mine one" (`en_us.json:277`). Basalt and blackstone say "lava-and-ice … where _lava_ meets _blue ice_" (`:406, :445`). Tuff says "where _lava_ meets _water_ over a bed of _gravel_" (`:519`). Every tooltip adds: "Needs a _shaft_ into its _back_ (the window faces you when placed) … Holds a stack [four stacks] … **The window shows it working**" (`:279, :285`), "Only comes out through _hoppers_, _chutes_, _funnels_, pipes or arms, from any side" (`:281`), "_Stops_, and puts _no stress_ on the network…" (`:297`). Wireless adds "Its _antenna_ listens like a _Redstone Link_: set the _frequency_ with the two slots on top" (`:309`).
- **Goggles** (`BE:314-333`): a header per product, "Cobblestone Generator:" / "Basalt Generator:" / "Blackstone Generator:" / "Tuff Generator:" (`en_us.json:288, 441, 480, 536`). Then "stored / capacity <product>" in gold, then one status: "Switched off by redstone", "Full: needs emptying (no stress)", "Needs rotation" (also shown while overstressed), or "N per minute" (`en_us.json:289-291, 295`). Then Create's own kinetic and stress lines (`BE:331`). These are what the player is meant to monitor: **how full it is, and why it isn't running.**
- **Ponder "Cobblestone, on Tap"** (`J/client/ponder/CobbleGeneratorScenes.java:28-157`). It's attached to every id except tuff (`J/client/ponder/CMPonderPlugin.java:60-62`), at 64 RPM:
  1. "an entire cobblestone farm, squeezed into one block" (arrow at the window, `:53-57`);
  2. "Power it through **the shaft hole in the back**. Only the back!" (`:64-68`);
  3. "Inside, water meets lava and out pops cobblestone, exactly as fast as a Mechanical Drill could mine it" (`:72-76`);
  4. hoppers etc. "have to come and get it" (`:80-86`);
  5. full: it "simply stops, and stops using stress too" (the scene sets stored to 64, `:90-95`);
  6. "A redstone signal switches it off as well" (lever, powered = true, `:101-109`);
  7. the Brass one "makes sixteen at a time" (block swapped, `:118-124`);
  8. Wireless: "Set a frequency on top, just like a Redstone Link" (diamond/emerald frequency, `:128-140`);
  9. "switch it off from anywhere. Rocks, now with Wi-Fi" (POWERED set, `:145-154`).
- **Ponder "Lava, Meet Ice"** (`J/client/ponder/NewBlockScenes.java:26-97`, basalt and blackstone ids, `CMPonderPlugin.java:54-58`). Its text promises looks: "The Basalt Generator **wears ice-blue trim** and is crafted with Soul Soil" and "The Blackstone Generator **is all black and gold**" (`en_us.json:498-499`).
- **Ponder "When the Going Gets Tuff"** (`J/client/ponder/MachineScenes.java:158-200`, `CMPonderPlugin.java:72-73`): "lava meets water over a bed of gravel" (`en_us.json:584`).
- **Recipes**, the material language. Andesite: andesite alloy, lava bucket, water bucket, **2 Mechanical Drills**, andesite casing (`D/recipe/andesite_cobble_generator.json:4-8`). Basalt and blackstone swap the water for blue ice and add soul soil or blackstone (`D/recipe/andesite_basalt_generator.json`, `andesite_blackstone_generator.json`). Tuff adds gravel (`D/recipe/tuff_generator.json`). Brass is a 5×5 Mechanical Crafting of brass sheets, brass casing, electron tubes and **4 more Mechanical Drills** around the andesite generator (`D/recipe/mechanical_crafting/brass_cobble_generator.json`). Wireless is shapeless, generator + Create Transmitter (`D/recipe/wireless_andesite_cobble_generator.json`). The Wireless Brass has two routes (`D/recipe/wireless_brass_cobble_generator.json`, `D/recipe/mechanical_crafting/wireless_brass_cobble_generator.json`).
- **Advancements**: "Rock Solid Investment", "Sixteen Tons", "Can You Hear Me Now?", "Remote Control Rock Band", "Basalt-ically Perfect", "Back in Black", "Tuff Love" (`en_us.json:330-337, 442-443, 481-482, 537-538`).
- **Sounds**: none of its own. The only sound is vanilla `STONE_BREAK` once per cycle (`BE:125`), and there's no `sounds.json` entry.

## States

| State | Source | How the player can tell today |
|---|---|---|
| `facing` = north / east / south / west | `Blk:30, 73-76`; blockstate `y` 0/90/180/270 (`A/blockstates/andesite_cobble_generator.json:3-32`) | The window side. The renderer turns the interior to match (`CGR:56, 84-87`) |
| `powered` = false / true, plain ids | `Blk:32`, set by `BE:183-189` | **No model change**: both values use the same model (`A/blockstates/andesite_cobble_generator.json:3-8`). Only the pours stopping and the frozen block give it away (same as "no rotation") |
| `powered` = false / true, wireless ids | same | The antenna texture swaps to Create's `redstone_antenna_powered` (red tip) (`A/models/block/wireless_andesite_cobble_generator_powered.json:17` vs the unpowered `:17`; blockstate `A/blockstates/wireless_brass_basalt_generator.json:3-32`). It lights for a **wired** signal too, because `powered` = wired OR link (`BE:172-180`) |
| Running (turning, not overstressed, not idle) | `BE:146-149` (client copy synced from `stored`, `powered`, speed) | Lava and water pours fall (`CGR:105-109`), the product block grows, steam puffs (`BE:95-98`), the back shaft stub turns, and a quiet stone-break click plays each cycle |
| No rotation, or overstressed (Create reports 0 speed) | `BE:102, 148` | Shaft still, pours gone, block frozen at its last height. Goggles: "Needs rotation" (`BE:325-326`) |
| Full (`stored` ≥ capacity) | `BE:152-153, 235-237` | Pours gone, **finished block sits at full size** (`BE:246-248`), shaft **still turning** (the network turns it; only the stress is dropped). Goggles: "Full" |
| Switched off (`powered`) | `BE:48, 172-180` | Pours gone, block frozen at partial height, shaft still turning. Wireless: lit antenna tip |
| `stored` 0–64 (Andesite) / 0–256 (Brass) | `BE:43`, synced at most every 10 ticks (`BE:40, 108-111, 298-303`), saved as `Stored` (`BE:344`) | **Not shown.** Goggles and comparator only |
| `destroyProgress` 0–9 (server), `clientProgress` 0–9 (client) | `BE:44, 57`; `Progress` is saved/sent (`BE:345, 353`), but the window uses the client's own countdown (`BE:249`), which starts at 0 on load and is never corrected | Product block height 0–3 px in tenths (`CGR:100-102`) |
| Product: cobble / basalt / blackstone / tuff | `GeneratorProduct.java:14-18` | Casing trim colour, chamber materials, blue ice vs water, the sprite of the forming block |
| Tier: Andesite / Brass | `perCycle`/`capacity` (`Blk:34-37`) | Casing colour and the back texture (`create:block/gearbox` vs `brass_gearbox`) only. **The interior is identical** |
| Wireless vs plain | `Blk:37`; `BE:199-213` | Antenna and two frequency pads in the model, with the frequency items drawn on them (`CGR:60-61`) |

## Moving and Changing Parts

| Part | Model file | Driven by | Axis / pivot / range | Source |
|---|---|---|---|---|
| Back shaft stub (**frozen, shared**) | `A/models/block/cobble_generator_shaft.json:9` (one cube x6–10, y6–10, z−0.02–3.5, Create `axis` sides, `axis_top` ends, uv `:12-17`) | Kinetic angle, `KineticBlockEntityRenderer.getAngleForBe` | Turned to the back with `YP −(back.toYRot()+180)`, then spun about the **world** X or Z axis (the facing axis) through the block centre (0.5, 0.5, 0.5), unbounded, like a Create shaft. In model space it sits at x6–10, y6–10, **z12.5–16.02**: through the 2 px back wall (z14–16) and 1.5 px into the chamber. Drawn into `RenderType.solid()` at block light | `CGR:57, 64-79` |
| Forming product block | Drawn in Java, no model file. Sprite = vanilla `block/cobblestone`, `block/basalt_side`, `block/blackstone` or `block/tuff` (`GeneratorProduct.java:14-18`), UVs mapped as a block model would (`CGR:117-139`) | `getFormingFraction()` = clientProgress / 10, or 1 while full (`BE:246-251`). **Steps in tenths, no partial-tick lerp** | Grows **up** from the floor: x6.5–9.5, z5–8, y2 → 2 + 3 × fraction (max y5). Hidden below 0.05 px (`CGR:100-102`). Snaps back to 0 when a cycle completes. At 64 RPM a cobble step is 0.3 px every 4 ticks; at 256 RPM it rises in 10 ticks, and basalt in 5 steps of 0.6 px | `CGR:99-102` |
| Lava pour | Java, `minecraft:block/lava_flow`, full-bright | `isRunning()` | Static column x3.5–4.5, y3–10, z6.5–7.5. On or off, no animation beyond the sprite | `CGR:105-106` |
| Water pour | Java, `minecraft:block/water_flow` tinted `0x3F76E4` (fixed plains colour, `CGR:44-45`) | `isRunning()` and not blue ice | Column x11.5–12.5, y3–10, z6.5–7.5 | `CGR:107-108` |
| Lava pool | Java, `lava_still`, full-bright | Always drawn | x2–6, y2–3, z2–13 | `CGR:93` |
| Water pool / blue ice | Java, `water_still` tinted / `minecraft:block/blue_ice` | Always drawn. Product decides which (`GeneratorProduct.java:51-53`) | Water x10–14, y2–3, z2–13; ice block x10–14, y2–6, z4–9 | `CGR:94-97` |
| Frequency items (wireless only) | Create `LinkRenderer.renderOnBlockEntity` | Link frequency items | Lying flat at (5.5, 17.05, 5.5) and (10.5, 17.05, 5.5) px (turned from south-facing offsets), scale 0.4 | `CGR:60-61`; `TopFrequencySlot.java:26-40` |
| Antenna tip (wireless only) | Block model texture swap `redstone_antenna` ↔ `redstone_antenna_powered` | Blockstate `powered` | Static | `A/blockstates/wireless_*_generator.json`; `wireless_*_powered.json:17` |
| Steam puffs | Particles, not the renderer | Running, 1 in 8 ticks | (8 ± 1.6, 5.1, 6.4) px, drifting up 0.012/tick | `BE:254-263` |

The renderer's `box()` draws every face but the bottom (`CGR:117-120`). **Nothing moves with each drill hit except the block's height**, and no drill or other mechanism is drawn.

## Code Contract

**Model, blockstate and item files (all must keep their names, or the registry/blockstate side changes with them):**
- 14 blockstates `A/blockstates/<id>.json`, 8 variants each (facing × powered), rotated by `y` 0/90/180/270 for north/east/south/west (`A/blockstates/andesite_cobble_generator.json:3-32`). Plain ids map both `powered` values to `block/<id>` (`:3-8`). Wireless ids map `powered=true` to `block/<id>_powered` (`A/blockstates/wireless_brass_basalt_generator.json:6-8`).
- 21 block models: `A/models/block/<id>.json` ×14 plus `wireless_{andesite,brass}_{cobble,basalt,blackstone}_generator_powered.json` and `wireless_tuff_generator_powered.json`. Every one is `parent: block/block`, `render_type: minecraft:cutout`, `ambientocclusion: false` (`A/models/block/andesite_cobble_generator.json:2-4`). The powered model differs from the unpowered one only in the `antenna` texture (`…_powered.json:17`).
- 14 item models `A/models/item/<id>.json`: standalone copies of the block model with a **static interior** baked in. Lava pool, water pool (custom `cobble_generator_item_water`) or blue ice, a 2 px product block (x6.5–9.5, y2–4, z5–8), lava pour and, for water products, a water pour (`A/models/item/andesite_cobble_generator.json:392, 416, 439, 465, 492`; basalt `A/models/item/andesite_basalt_generator.json:392-498`). No renderer is involved for items.
- Standalone part model: only `block/cobble_generator_shaft` (`CGR:36-37`). Nothing else is loaded by name.

**Texture naming (by tier and product):**
- Per tier and product: `<andesite|brass>_<cobble|basalt|blackstone>_generator_{front,side,top}.png`. Tuff is `tuff_generator_{front,side,top}.png`, with no tier prefix (`A/models/block/tuff_generator.json:6-10`).
- Bottoms are per tier only: `andesite_cobble_generator_bottom` (all andesite and tuff ids) and `brass_cobble_generator_bottom` (all brass ids) (`A/models/block/andesite_basalt_generator.json:11`, `brass_basalt_generator.json:11`).
- Shared interior: `cobble_generator_{chamber,floor,ceiling}` (cobblestone ids only), `cobble_generator_pipe` (all), `cobble_generator_glass` (all), `cobble_generator_item_water` (item models).
- Borrowed vanilla interior per product: basalt = `smooth_basalt` chamber/ceiling + `soul_soil` floor. Blackstone = `polished_blackstone_bricks` chamber + `polished_blackstone` floor/ceiling. Tuff = `tuff_bricks` chamber + **`gravel` floor** (the "bed of gravel") + `polished_tuff` ceiling (`A/models/block/andesite_basalt_generator.json:12-14`, `andesite_blackstone_generator.json:12-14`, `tuff_generator.json:12-14`).
- Borrowed Create textures: back = `create:block/gearbox` (andesite and tuff) or `create:block/brass_gearbox` (brass). These are Create's casing faces with the round shaft hole (`A/models/block/andesite_cobble_generator.json:8`). Wireless adds `create:block/redstone_antenna` / `_powered` and `create:block/redstone_bridge` (`A/models/block/wireless_andesite_cobble_generator.json:17-18`).
- Java-side sprites: vanilla `water_still`, `water_flow`, `lava_still`, `lava_flow`, `blue_ice` (`CGR:39-43`) and the product sprites (`GeneratorProduct.java:14-18`). `GeneratorProductTest.spritesAreVanillaBlockTextures` and `spriteTexturesExist` check these (`src/test/.../cobblegen/GeneratorProductTest.java:50-66`).

**Interior geometry the renderer depends on (model space, front north):**
- **Floor top at y = 2** (`CGR:81`). The pools and the block all start at y2 (`CGR:93-102`).
- The clear volume must include the lava pool x2–6 / z2–13 (y2–3), the water pool x10–14 / z2–13 or the ice block x10–14, y2–6, z4–9, the product block x6.5–9.5, z5–8, y2–5, and both pour columns from y3 up to **y10** at x3.5–4.5 and x11.5–12.5, z6.5–7.5 (`CGR:93-108`).
- The pours end at y10, so anything that "feeds" them must end there. Today the model's nozzle tips are x3.5–4.5 / x11.5–12.5, y10–11, z6.5–7.5, under 2×3×2 pipes at y11–14 (`A/models/block/andesite_cobble_generator.json:273, 302, 331, 360`). On basalt and blackstone the right-hand nozzle has no pour (`CGR:107`).
- Steam spawns at about (8, 5.1, 6.4) px (`BE:257`), so that spot must be inside open chamber.
- The window has to show this from the front: today a 12×12 pane at z0.9, x2–14, y2–14, unshaded, both faces (`A/models/block/andesite_cobble_generator.json:389`).
- The renderer turns the chamber only about Y (`CGR:84-87`). A non-horizontal facing would need renderer work.

**Shaft and power:**
- Shaft only on the back face (`Blk:79-82`), axis = facing axis (`Blk:84-87`). The stub sits at the face centre (8, 8). Its 4×4 corners sweep a radius of 2.83 px, so the socket opening must be at least about 5.7 px across, and the wall behind the stub must leave z12.5–16 free at x6–10, y6–10.
- **`cobble_generator_shaft.json` is frozen.** Besides the 14 generators, it is drawn by `CobbleGeneratorRenderer.renderShaft` for the **Auto Harvester** (`J/client/AutoHarvesterRenderer.java:71`), **Animal Pens** (`J/client/AnimalPenRenderer.java:57`) and **Cuckoo Clock** (`J/client/CuckooClockRenderer.java:88`), all hands-off. `SHAFT` itself is also drawn by the **Electric Motor** (`J/client/ElectricRenderers.java:91`, hands-off), the **Magma Engines** (`J/client/MagmaEngineRenderer.java:100`, hands-off), the String Tangler and Stone Infuser (`J/client/MachineRenderers.java:60`) and the Compact Gyroscope / Torque Rotor area of `J/compat/sable/client/AeronauticsRenderers.java:129` (`RotorRenderer`, `:106`).
- **`CobbleGeneratorRenderer.renderShaft(KineticBlockEntity, BlockState, Direction, PoseStack, MultiBufferSource, int, int)` is a shared API.** Its signature, package-private visibility and behaviour (turn to `back`, spin about the world X/Z, `solid` buffer) must stay, because three hands-off renderers call it (`CGR:64-79`). A generator-specific shaft look needs a **new** model and code path, leaving `SHAFT` and `renderShaft` alone.

**Wireless top:**
- Two frequency slots lie flat on pads on the **front half** of the top: centres (5.5, 5.5) and (10.5, 5.5) px, at y17.05, scale 0.4 (`TopFrequencySlot.java:26-40`; the javadoc describes red and blue pads, `:16-19`). The model's pads are x3.5–7.5 and x8.5–12.5, y16–17, z3.5–7.5, using `redstone_bridge` red/blue UVs (`A/models/block/wireless_andesite_cobble_generator.json:540, 599`). Moving the pads means changing `TopFrequencySlot`.
- Antenna: Create's crossed planes at x11–14 / z11–14 (the back corner on the +x side), rising to y26, with a 5×5 plate at y22 (`:412, 453, 485, 508`). Code doesn't depend on where it sits.

**Block properties:**
- Shape: `Blk` has no `getShape`/`getCollisionShape` override (whole file), so the shape is a full cube. The model fills 0–16, and only the wireless antenna rises above it.
- `noOcclusion`, `isViewBlocking(false)` (see-through, `ModBlocks.java:79-80, 91-92`). **Light 7 in every state** ("The lava in the window glows", `ModBlocks.java:82-83, 94-95`). The renderer draws the lava `FULL_BRIGHT` (`CGR:89, 93, 106`).
- Andesite: `MapColor.STONE`, strength 2.5/6, `SoundType.STONE`. Brass: `TERRACOTTA_YELLOW`, 3.0/6, `NETHERITE_BLOCK` (`ModBlocks.java:76-78, 88-90`).
- **The hands-off Brass Animal Pen copies `BRASS_COBBLE_GENERATOR`'s properties**, overriding only light to 0 (`ModBlocks.java:157`; checked by `ModBlocksTest.penTiers`, `:69-74`). Changing the brass generator's properties changes the pen.
- Particle texture = the `side` texture (`A/models/block/andesite_cobble_generator.json:6`).

**Files shared with other blocks (look-only changes still ripple):**
- `cobble_generator_glass.png` is also the **Stone Infuser's** glass (`A/models/block/stone_infuser.json:10`, `A/models/item/stone_infuser.json:9`).
- Shared with the hands-off Auto Harvester and Animal Pens: **no texture or model files** any more. Their redesign gave them their own textures "so the Cobble Generators and Stone Infuser are unchanged" (commit `c9ff682`). What remains shared is the `SHAFT` model and `renderShaft` (above), the pen's copied properties (above), and the **design kit** `design/models/process-box.lib.js`, whose header already names "later the Cobble Generators" (`:1`). Per `design/hands-off.md`, the kit may grow but must keep producing identical output for the harvester and pens (regenerate and compare). `design/models/process-box.export.js` writes only harvester and pen assets (`:1-7, 14-19`), so a generator export would need its own entries or script. The harvester/pen front tray helper `J/client/TrayItems.java` is also shared under the same rule. The generators don't use it today.
- Create textures are borrowed by reference, never copied.

**Ponder and lang that describe the look** (change them with the look): "shaft hole in the back" (`en_us.json:322`), "Set a frequency on top" (`:328`), "**ice-blue trim**" on basalt and "**all black and gold**" on blackstone (`:498-499`), "a bed of gravel" for tuff (`:519, 584`), "The window shows it working" (`:279`).

## Current Look

Contact sheet: `design/dossiers/cobble-generator/textures-before.png`. Each 16×16 is scaled ×8, nearest-neighbour. Rows: 1 andesite cobble, 2 brass cobble, 3 andesite basalt, 4 brass basalt, 5 andesite blackstone, 6 brass blackstone, 7 tuff (columns: front, side, top, chamber, floor, ceiling, or blue ice for the ice products). Row 8: andesite bottom, brass bottom, Create gearbox, Create brass gearbox, glass, pipe. Row 9: item water, vanilla lava, tinted vanilla water, Create antenna, antenna powered, Create axis.

**Geometry** (14 elements, `A/models/block/andesite_cobble_generator.json:20-389`): a 1 px deep front frame (top and bottom bars 2 px tall, side posts 2 px wide, z0–1) round a 12×12 window. 2 px side walls, a 2 px back wall, 2 px ceiling and floor slabs. Two copper inlet pipes with 1 px nozzles hang from the ceiling. One flat glass pane sits at z0.9. Wireless adds 6 elements: antenna, cross plate and two pads (`wireless_andesite_cobble_generator.json:412-599`). There's no relief on any outer face, so from outside it is a textured cube with a window. The chamber is 12 wide × 12 tall × 13 deep.

**Textures** (dominant colours sampled from the PNGs):
- `_front`: only its outer 2 px ring is ever seen (the frame strips use positional UVs). The plank centre is hidden behind the window. Andesite is a Create andesite-casing ring (`#8c8a82`, `#505351`, `#60635f`) round spruce planks (`#553a1f`, `#70522e`). Brass is a **bright brass ring `#e2b356`** on dark brown. Basalt is a blue ring (`#4f78c4`, **near-white blue `#a6c9fb`**). Blackstone is a gold ring (`#9a6a0a`, **saturated `#f4c542`**). Tuff is a sage ring (`#4f5a45`, `#9aa58a`).
- `_side`: a casing frame round a black grille with bright horizontal bars. Cobble and tuff bars are **saturated orange `#b8400c`**, basalt bars are **pale/near-white blue**, and blackstone bars are **saturated yellow `#f4c542`** on near-black (hazard-stripe-like).
- `_top`: two flat **icon panels**, a striped orange "lava" tile and a striped blue (or pale blue) "water/ice" tile, on the casing. The brass top reaches **`#ffeb8c`** (near-white yellow).
- `_bottom`: a casing frame round a grey bar grille.
- `cobble_generator_chamber`: near-black brick (`#2a2521`, `#171412`). `floor`: dark grey with diagonal lines (`#3a3837`). `ceiling`: a **two-tone checkerboard** (`#3a3837` / `#2b2a29`) that reads as a placeholder. `pipe`: vertical **salmon-copper stripes** (`#c26b4c`, highlight `#e3826c`). `glass`: transparent with a few **near-white glints** (`#f4fdff`, `#c6e2e6`). `item_water`: saturated blue diagonal (`#7fb0ff` highlight).

**What works:** the interior. The dark chamber has depth (matching the "insides dark with depth" call from the chests). Real animated vanilla lava and water, full-bright lava glow, and the product's own texture rising between them make "The window shows it working" true. Per-product chamber materials (smooth basalt, blackstone bricks, tuff bricks over gravel) tie each variant to its product. Create's own gearbox face on the back, with a real spinning shaft stub, matches the "shaft hole in the back" teaching. Create's own Redstone Link antenna is used on wireless. All the faces are 16×16 and gently mottled, not flat.

**What doesn't, against the house style:**
- Brass tier: the whole frame is bright brass (`#e2b356`, top highlight `#ffeb8c`), above the house brass highlight cap of `#c9a75a` and far from the "dark aged-brass trim, brighter only on rivets" that landed on the brass pen.
- Accents are whole surfaces of saturated colour: the side vents (orange, yellow, near-white blue), the coloured product rings, the top icon tiles. House rule: "accents are trims … never whole surfaces of saturated yellow", "never near-white", "hazard stripes at most one small accent".
- The top "icons" are symbols, not parts. House style asks for details that each represent a real part.
- Pipes are salmon-bright copper. The glass glints are near-white. The ceiling checker looks unfinished.
- The shell is a flat cube with 1–2 px walls and no bevels, sockets or hatches. The harvester and pens it used to share this shell with have moved on to the steel process-box panels (crisp bevelled field, flanged socket, front tray). The generators are the last blocks on the old shell.
- Tier reads only as colour. Brass and andesite have the same build.

## Function the Look Doesn't Show

1. **How full it is.** 0–64 or 0–256 stored blocks (`BE:43`) appear nowhere on the model. There's no tray, gauge or pile, only the goggles and comparator. The sibling machines show their buffer in a front tray (`J/client/TrayItems.java`).
2. **Why it stopped.** Full, switched off and unpowered all look almost the same: the pours vanish (`CGR:105`). The only tells are subtle: full = complete block + shaft turning; off = partial block + shaft turning; no rotation or overstressed = partial block + shaft still. On **plain ids, redstone-off has no model change at all** (`A/blockstates/andesite_cobble_generator.json:3-8`).
3. **The drill.** Two to six Mechanical Drills go into the recipes, and the tooltip promises drill pace (`en_us.json:277`). No drill or bit appears, and nothing moves per drill hit (`BE:119-129`). The block's stepped height is the only pacing tell (`CGR:100`), plus a click at volume 0.15 (`BE:125`).
4. **Brass makes 16 per cycle and holds four stacks**, but its window shows the same single 3 px block as Andesite (`CGR:99-102`). Only the casing colour differs.
5. **Zero stress while idle** (`BE:218-226`) isn't shown.
6. **Speed and product hardness** (basalt twice as fast, `en_us.json:498`) show only as the growth rate of a 3 px cube. There's no meter or rate tell.
7. **Output from any side, automation only** (`BE:357-395`): no output hatch, chute or port is drawn.
8. **The wired redstone input** has no visible contact. The comparator output isn't shown either.

## Look the Code Doesn't Back Up

1. **Top lava and water icon tiles** suggest fill ports or inputs. Nothing goes in: no fluid capability, and items are refused (`BE:369-372, 391-394`; `J/ComedyMonkeysCreateAddons.java:134-136`). The lava and water are free and endless.
2. **Ceiling inlet pipes** suggest a feed from outside. Nothing is fed. On basalt and blackstone the **right-hand water nozzle never pours** (`CGR:107`) and hangs over the blue ice for nothing.
3. **Bright vent grilles on the sides** (orange, yellow, pale blue) read as hot exhaust or glowing heat vents. Nothing vents. They aren't emissive and don't change with state. Steam exists only inside the chamber (`BE:254-263`).
4. **The bottom grille** suggests a drain or bottom output. Output goes out of every side equally (`J/ComedyMonkeysCreateAddons.java:134-136`).
5. **The wireless antenna tip lights for any redstone-off**, including a wired lever, because `POWERED` = wired OR link (`BE:172-180`). The model suggests the link is what switched it off.
6. **The window suggests one block per cycle**, but the brass ids make 16 (`ModBlocks.java:95, 126`).
7. **The item model shows a block always running** (both pours on, a 2 px block forming, `A/models/item/andesite_cobble_generator.json:439-492`). That's harmless, but a redesigned item model should match the new block.

## History

- All 14 generators' textures and models date from the initial commit `6a38571` (2026-10-19) and haven't changed since (`git log` on `textures/block/*generator*`). Commit `569d7c4` touched only Lava Generator files.
- `bfb73f2` (Release 1.8.0) made `renderShaft` package-shared, taking any `KineticBlockEntity`, "shared with the Auto Harvester" (`CGR:64`). That's where the cross-block dependency started.
- `26adbd3` fixed the drill pace and added `CobbleGeneratorBlockEntityTest` and `GeneratorProductTest`.
- `c9ff682` redesigned the Auto Harvester and Animal Pens off the old shared shell, with their own textures, "so the Cobble Generators and Stone Infuser are unchanged". It introduced `design/models/process-box.lib.js`, which names the Cobble Generators as the family's later member (`:1`), and the shared `TrayItems` front tray.
- No brief, spec or plan exists for the generators (`design/briefs`, `design/specs`, `design/plans` have none). The survey (`design/dossiers/survey.md:11, 32, 36`) ranks them 5th by score and 4th in recommendations: "Good insides … casing is bright brass, with saturated icons on top, yellow vent stripes on blackstone and near-white vents on basalt", "Stored amount. Powered looks the same as unpowered. Nothing moves to show drill pacing".
- Calls on the sibling blocks that apply here (memory, process-box redesign, 2026-10-24/25): crisp near-flat steel panels instead of muddy mottle; brass tier = dark aged-brass trim with brighter brass only on rivets and lips; ports at face centres; square is fine for non-engine machines; container insides dark with depth; the user wants machines placeable in all six directions (for engines). These generators are horizontal-only today (`Blk:30`), and the chamber renderer turns only about Y (`CGR:84-87`).
