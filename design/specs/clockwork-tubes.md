# Clockwork Tubes: Systems Spec

Status: **ready for development**  ·  Related: [visual language](../visual-language.md) (bot-built family), [Clockwork Dynamo spec](dynamo.md), [Trade Bot brief](../briefs/trade-bot.md), [Fluid and Power Lines spec](fluid-and-power-lines.md) (D1: no item pipes there), brief (to come, `design/briefs/clockwork-tubes.md`), quick read `clockwork-tubes.page.html`

## Concept

The user (2026-10-31): "I want to make three new blocks. These allow the user to suck items from one end of a chute to another, attached is a simple diagram. The red block is powered by SU the blue blocks are connectors that can connect to blocks that contain items, be placed above depots or belts, or above the floor. They can branch off in all directions but there is only one receiver block allowed in a network. This should follow the clockwork theme of the trade bot and the dynamo." Then: "the reciever should be powered by cog, the receives cog should be hollow to allow itms to pass through", and "There should be brass versions of the opening block with filters".

The diagram: three blue blocks at the bottom, each with a tube rising from it. The tubes join into one network, branching left and right, and every arrow points along the tubes towards one red block at the top.

Clarified: **a suction network for items. Clockwork Inlets take items from chests, depots, belts or the floor; Clockwork Tubes carry them, visible through glass windows; one Clockwork Receiver, turned by its own hollow cog, pulls them all to itself and pushes them out through the middle of the cog.**

## Decisions

| # | Area | Decision | Source |
|---|---|---|---|
| D1 | Blocks | Three kinds: the **Clockwork Receiver** (red), the **Clockwork Inlet** (blue; with a brass version, D21) and the **Clockwork Tube** that joins them | user |
| D2 | Purpose | Items are sucked from the Inlets, through the tubes, to the Receiver | user |
| D3 | Power | The Receiver runs on Create rotation (SU), **driven through a cog** that meshes with Create's cogwheels (no shaft socket). "the reciever should be powered by cog, the receives cog should be hollow to allow itms to pass through" | user, 2026-10-31 |
| D3a | Hollow cog | The Receiver's cog is a **hollow ring**: items pass through its centre, so the cog's axis lies along the items' path through the Receiver | user, 2026-10-31 |
| D4 | Inlet placement | An Inlet can sit against a block that holds items, above a depot or belt, or above the floor | user |
| D5 | Layout | Tubes branch in every direction (up, down, sideways, T-joins and crosses) | user, diagram |
| D6 | One Receiver | **One Receiver per network**, at most | user |
| D7 | Flow | Always one way: from the Inlets towards the Receiver | diagram |
| D39 | Short run | **An Inlet right against a Receiver's input joins it directly**, no tube needed: its loads go into the Receiver's throat (one load, saved; dropped if the Receiver is broken) and out as soon as its output can take them. Placing a Receiver on an Inlet's back, or an Inlet on a Receiver's input, joins them (as D27 for tubes). "you should be able to just place reciever against an inlet, short run" | user, 2026-10-31 |
| D8 | Look | **Bot-built family**: the Trade Bot's and the Dynamo's materials and parts (andesite panels in a dark-steel frame, riveted brass bands, rounded squares with stepped corners). Per the visual language: **no copper** (copper means electricity), no face, glows small and muted. Dark and muted, previewed in Blockbench first | user; `design/visual-language.md`; standing rules |
| D9 | Not a Line | A separate system from the Fluid and Power Lines, which carry no items (their D1). Tubes never join Lines | existing spec |
| D10 | Mod rules | Ids without "create"; every block has a BlockItem (Carry On) | existing mod rules |
| D11 | Receiver output | Items leave through the hollow cog on the face **opposite the incoming tube**. They're **inserted into whatever is there** (chest, vault, funnel, depot, belt, any item handler); with **no block there they drop out**, like Create's Chute. **Full: the network waits** (nothing lost): the Inlets go on filling empty tubes until the backup reaches them, one load a tube, then stop (D26; confirmed by the user, 2026-10-31) | user, round 1 |
| D12 | Cog size | **Small cogwheel** size, within one block. Meshes like a Create cogwheel: small cogs side by side, large cogs on the diagonal. The hole is about 6 px, so an item can be seen passing through | user, round 1 |
| D13 | Speed and stress | **The cog's speed sets the rate**: faster means more items a second and faster travel. **Stress is fixed per RPM** (like the Encased Fan), so the cost never changes with the network's size. Numbers: D25 | user, round 1 |
| D14 | Transit | **Visible**: tubes have glass windows; items slide past them towards the Receiver, then through the hollow cog. Items in transit are **saved**; never lost or duplicated | user, round 1 |
| D15 | Floor reach | An Inlet facing empty air takes only items lying in **the one block space in front of its mouth**, like a hopper's top. Over a floor, it hangs one block up (D27) and sweeps the air block under it. Not a vacuum hopper | user, round 2 |
| D16 | Two Receivers | **Refuse to join**: a tube never joins two networks that each have a Receiver; it stays capped at that end, and the goggles say why. A Receiver placed on a network that already has one doesn't connect, and you get a message. Nothing breaks or stops | user, round 2 |
| D17 | Unloaded chunks | **Loaded parts only**: nothing moves unless the Receiver's chunk is running; Inlets in unloaded chunks don't pull; items in an unloaded stretch wait there, saved; **no chunk is ever loaded** | user, round 2 |
| D18 | Names | **Clockwork Tube**, **Clockwork Inlet**, **Brass Clockwork Inlet**, **Clockwork Receiver**. Ids `clockwork_tube`, `clockwork_inlet`, `brass_clockwork_inlet`, `clockwork_receiver`. ("Chute" is Create's; "Intake" is a Fluid Line fitting) | user, round 3 |
| D19 | Control | **Redstone on an Inlet switches it off** (both kinds). The Receiver has no redstone: a **Clutch** on its cog stops the network (as the Dynamo, its D23) | user, round 3 |
| D20 | Breaking a tube | Items inside **pop out where it broke**. Items further down still reach the Receiver; items further up wait in their tubes until the gap is mended. Nothing lost, nothing duplicated | user, round 3 |
| D21 | Brass Inlet | **Two Inlets, like Create's funnels**: the plain Clockwork Inlet has **no filter**; the **Brass Clockwork Inlet has a Create filter slot** (item, attribute and list filters). "There should be brass versions of the opening block with filters" | user, 2026-10-31 |
| D22 | Inlet tiers | **Like Create's funnels**: the plain Inlet takes **one item** at a time; the Brass Inlet takes **up to a stack**, its filter's count setting how many | user, round 4 |
| D23 | Tier | **Andesite network, brass extras**: Tube, plain Inlet and Receiver are andesite age; the Brass Inlet is brass age | user, round 4 |
| D24 | Whimsy | **Inlet gulp**: the Inlet's mouth flap twitches inward each time it takes something. Everything else is mechanism: the hollow cog turns, items slide past the windows and pop through the cog | user, round 4 |
| D25 | Numbers | **Stress** 4 SU per RPM (like the Mechanical Pump; 1,024 SU at 256 RPM). **Rate** RPM ÷ 64 loads a second for the whole network (4 at 256 RPM). A **load** is 1 item (plain Inlet) or up to a stack (Brass). Each Inlet waits **at least 8 ticks** between loads, like a funnel. **Travel** RPM ÷ 16 blocks a second (16 at 256 RPM). Any direction of rotation works | user, round 5 (proposed defaults) |
| D26 | Order and backup | Inlets **take turns** (round robin). **Each tube holds one load**; when the Receiver can't empty, loads queue back along the tubes and Inlets behind a full tube stop pulling. Each load takes the **shortest route** to the Receiver; at a join, the first to arrive goes first | user, round 5 (proposed defaults) |
| D27 | Facing | Clicking **a tube** with an Inlet or Receiver **joins it to that tube**: the Inlet's mouth, or the Receiver's output, faces away (how an Inlet hangs over a floor). Clicking **anything else** points the Inlet's mouth, or the Receiver's output, into that block. An Inlet joins a tube **at its back only**; a Receiver takes its tube **on the face opposite its output only**. The cog meshes around the Receiver's rim | user, round 5 (proposed default; tube rule approved 2026-10-31) |
| D27a | Inlet facings | **Inlets face any of the six directions**: mouth down, up or sideways, tube at the back. Facing up into empty air, it takes items lying on top of it, so it can act as a collecting floor. "Inlets should be able to face any direction" | user, 2026-10-31 |
| D28 | What an Inlet reads | In front of its mouth: an **item handler** (chest, barrel, vault, any mod's inventory) → takes from it; a **depot** → takes what it holds (a Brass Inlet with a filter can wait for pressed or processed output); a **belt** → takes items passing under it; **empty air** → takes items lying in that block space (any direction, D15). Anything else: idle | user, round 5 (proposed defaults); any-direction sweep approved 2026-10-31 |
| D29 | Tube joints | Tubes join tubes, Inlet backs and Receiver inputs on their own; **never** Create's Chutes or Funnels, or Lines. A **wrench on a tube's side closes or opens that joint** (like Create's Fluid Pipes), so networks can run side by side. Waterloggable, not flammable | user, 2026-10-31 |
| D30 | Reach | At most **256 tubes per network** (server config). A tube past the limit caps off, and the goggles say why. A Receiver joined to a longer run of tubes isn't refused: it serves the nearest 256, and its goggles say only those are served (build note, 2026-10-31, from the adversarial review) | user, round 5 |
| D31 | Hiding | **No hiding**: the windows and moving items are the point and show where a jam is. Players can still run tubes behind walls | user, round 5 |
| D32 | Moving | On an **Aeronautics ship**: works as normal (Sable restarts block entities from NBT; networks rebuild on load, like the Lines). On a **moving Create contraption**: paused, items kept, carries on when placed again | user, round 5 |
| D33 | Goggles | **Receiver**: speed, loads a second, items in transit, number of Inlets and tubes, why it's waiting ("Output full", "Not turning", "Overstressed"). **Inlet**: "Switched off by redstone", "No Receiver on this network". **Tube**: why it's capped ("Would join two Receivers", "Network is 256 tubes long") | user, round 5 (proposed defaults) |
| D34 | Display Link | The Receiver is a source: **items moved a minute**. No comparator output anywhere | user, round 5 (proposed defaults) |
| D35 | Carry On, breaking | Carry On picks up the Inlet and Receiver (settings kept); a carried tube keeps its load. Every block drops itself when broken (items inside: D20) | user, round 5 (proposed defaults) |
| D36 | Recipes | **Clockwork Tube ×4**: shaped `A / G / A` (andesite alloy, glass). **Clockwork Inlet**: andesite casing + Andesite Funnel + Clockwork Tube. **Brass Clockwork Inlet**: brass casing + Brass Funnel + Clockwork Tube. **Clockwork Receiver**: andesite casing + Encased Fan + cogwheel + Clockwork Tube | user, round 5 (proposed defaults) |
| D37 | Sound | **Only Create's standard cog sound**, nothing of our own: the Receiver is a small cog (`ICogWheel`), so Create's `KineticBlockEntity.tickAudio` plays its cog soundscape while it turns, pitched by speed; `isNoisy()` returns false as on Create's cogwheels, so it sounds exactly like one. Inlets and Tubes are silent; the gulp is silent. "the only sound it needs are the standard cog sound from create" | user, 2026-10-31 (replaces the round-5 whoosh and gulp) |
| D38 | Teaching | Ponder **"Down the Tubes"**: Inlets on a chest, over a depot and over the floor; tubes branching; the Receiver into a chest; a Brass Inlet's filter; redstone on an Inlet; the one-Receiver rule. Advancement **"Down the Tubes"**: move your first item | user, round 5 (proposed defaults) |

## Open Questions

None. Next in the pipeline: the look (design brief from the voxel art director, in the bot family), then the model and textures. Sound is settled (D37: Create's cog sound only).

## Core Loop

1. Put a **Clockwork Inlet** where items collect: on a chest, over a depot or belt, or hanging over a mob farm's floor. Use a **Brass Inlet** to take only some items, or whole stacks.
2. Run **Clockwork Tubes** from every Inlet, branching and joining as you like, to one spot.
3. Put the **Clockwork Receiver** there, output into a chest, vault, funnel or belt, and mesh a cogwheel with its hollow cog.
4. Items slide past the tube windows and pop out through the middle of the turning cog.
5. Turn the cog faster to move more; add Inlets as the base grows.

Why players come back to it: it's the only way in Create to gather from many places into one, in any direction, without laying belts. Every new farm becomes one Inlet and a few tubes.

## Behaviour / Rules

1. **Network**: the Receiver owns its network: every tube reachable from its input face through open joints, up to 256 (D30), and the Inlets on those tubes. It's rebuilt only when a tube, Inlet or Receiver on it is placed, broken, wrenched or loaded, never every tick.
2. **Joining**: a tube that would join two networks that each have a Receiver stays capped (D16). A tube past the 256 limit caps off (D30). A second Receiver placed on a network doesn't connect, and the player gets a message.
3. **Running**: each tick, if the Receiver's chunk is running, its cog is turning, the network isn't overstressed and it isn't on a moving contraption:
   - it gives out up to `|RPM| ÷ 64` loads a second (fractions carried over) to the Inlets, **taking turns**;
   - an Inlet may take a load only if its own 8-tick cooldown is over, it isn't powered by redstone, its chunk is running, and the tube behind it is empty;
   - it takes from what's in front of its mouth (D28): one item (plain) or up to a stack (Brass, filter count), only items its filter passes. Taking is a real extract, so a full or empty source just means it skips its turn;
   - the Inlet gulps (D24, a block event to clients).
4. **Travel**: each load moves along its shortest route at `|RPM| ÷ 16` blocks a second. It enters the next tube only if that tube is empty and loaded (D17, D26). At a join, the first load to arrive goes first.
5. **Delivery**: a load that reaches the Receiver passes through the cog and is inserted into the block on its output face; what doesn't fit stays in the last tube and the network waits. With no block there it drops out as an item, moving gently outward like the Chute's (D11).
6. **Stopped** (not turning, overstressed, Clutch open, on a contraption): nothing is pulled and nothing moves; loads stay where they are.
7. **Breaking**: a broken tube drops its load as an item where it stood (D20). Loads downstream carry on; loads upstream wait. A broken Inlet or Receiver drops itself; a broken Receiver leaves its network stopped with loads waiting.
8. **Unloaded**: loads never move into a tube whose chunk isn't running, and live in exactly one tube's block entity, so an unload can't lose or copy one.

## Progression

**Tier: early to mid, andesite age, with a brass upgrade.** It sits beside Create's andesite funnels and chutes.

- **Needs before it**: andesite alloy, andesite casing, glass, a cogwheel and an Encased Fan (which needs a propeller: iron), plus a shaft source.
- **Unlocks**: gathering from several farms into one chest; collecting a mob farm's drops without hoppers; moving items up, down and around corners without belts or fans.
- **Brass upgrade**: the Brass Inlet (brass casing, Brass Funnel) for filtering and whole stacks, when the player starts sorting.
- **Early limit**: plain Inlets move one item per load, so an early network at low speed is modest (1 item a second at 64 RPM).

## Balance

**Rates (default config)**

| Cog speed | Stress | Loads a second | Plain Inlets, items/s | Brass Inlets, items/s (max) | Travel |
|---|---|---|---|---|---|
| 16 RPM | 64 SU | 0.25 | 0.25 | 16 | 1 block/s |
| 64 RPM | 256 SU (one water wheel's worth) | 1 | 1 | 64 | 4 blocks/s |
| 128 RPM | 512 SU | 2 | 2 | 128 | 8 blocks/s |
| 256 RPM | 1,024 SU | 4 | 4 | 256 | 16 blocks/s |

One Inlet alone can't take more than 2.5 loads a second (its 8-tick cooldown), the same as a funnel. Compare: a Brass Funnel extracts up to 64 every 8 ticks (160 items a second) from one inventory.

- **Cost**: cheap to start (andesite), costs real stress to run fast (1,024 SU at full speed, a windmill's worth).
- **Reward**: many sources into one place, any direction, small footprint.
- **Risk**: none; nothing explodes, nothing is lost.
- **Automation**: fully automatic; redstone per Inlet, a Clutch for the whole network.
- **Exploits and fixes**:
  - *Duplication by breaking mid-transit*: each load lives in one tube only; breaking drops that load once (D20).
  - *Duplication by chunk unload*: loads never move into an unloaded tube (D17, rule 8).
  - *Carry On a tube with a load*: the load goes with the block's data, not copied (D35).
  - *Two Inlets or a hopper on one dropped item*: taking an item entity is a single server-side shrink; whoever takes it first gets it.
  - *Free stress at high speed*: fixed per RPM, so speed always costs (D13).
  - *Server lag*: 256 tubes at most, one load per tube, network rebuilt only on change, floor checks only on an Inlet's turn and only in one block space.
  - *Griefing*: an Inlet against someone's chest is the same as a Create Funnel or a hopper; claim mods handle it through placement.
- **Overpowered?** At full speed with Brass Inlets it moves 256 items a second: more than one Brass Funnel, for the cost of a windmill's stress and a brass upgrade. It still has one destination, no routing, no requests and a 256-tube reach, so Create 6's packages, frogports and trains keep long-range, routed logistics.
- **Useless?** No. Belts can't go up or round corners easily, chutes only go up and down, funnels only reach neighbours. Nothing else gathers from many places into one.

## Create Integration

| System | Interaction | Notes |
|---|---|---|
| Cogwheels, shafts | **Yes**: the Receiver's hollow cog meshes with small cogs side by side and large cogs on the diagonal; stress 4 SU/RPM | D3, D12, D25 |
| Clutch, Gearshift | **Yes**: a Clutch on the cog's drive stops the network; direction doesn't matter | D19 |
| Belts | **Yes**: an Inlet over a belt takes passing items; the Receiver puts items onto a belt in front of its output | D11, D28 |
| Depots | **Yes**: an Inlet takes what a depot holds (a Brass filter can wait for processed output); the Receiver can feed a depot | D28 |
| Chests, barrels, vaults, any item handler | **Yes**: Inlets take, the Receiver inserts | D11, D28 |
| Funnels, Chutes | The Receiver can push into them; tubes never join them | D29 |
| Encased Fan | Ingredient of the Receiver; no other interaction | D36 |
| Filters (item, attribute, list) | **Yes**: in the Brass Inlet's filter slot, with count | D21, D22 |
| Mechanical contraptions | Paused while moving, items kept | D32 |
| Trains, elevators | Same as contraptions | D32 |
| Aeronautics ships | **Works as normal** | D32 |
| Logistics and packages | Not part of it; a package travels as an item like any other. The Receiver can feed a Packager's inventory | D9 |
| Deployers, mechanical arms | A deployer can place the blocks; an arm can feed or take from the chest a Receiver fills. No special interaction | |
| Redstone | **Yes**: on an Inlet, switches it off. No comparator output | D19, D34 |
| Display Link | **Yes**: the Receiver is a source, items moved a minute | D34 |
| Engineer's Goggles | **Yes**: all three blocks | D33 |
| Ponder | **Yes**: "Down the Tubes" | D38 |

## Technical Notes

- **Classes**: `ClockworkReceiverBlock` (directional, axis along its facing, implements Create's `ICogWheel` with `isSmallCog` true so neighbours mesh) with `ClockworkReceiverBlockEntity extends KineticBlockEntity` (stress impact registered through `BlockStressValues`, value from config). `ClockworkInletBlock` (directional, `POWERED`, two variants; the brass one adds Create's `FilteringBehaviour`) with its block entity (cooldown). `ClockworkTubeBlock` (six connection booleans + waterlogged, like a fluid pipe; wrench toggles a side) with `ClockworkTubeBlockEntity` (at most one load: stack, progress, next direction).
- **Network**: `TubeNetwork`, owned by the Receiver: BFS from its input face over open joints, max 256 tubes; ordered Inlet list; next-hop table (shortest route) per tube. Rebuilt lazily when a member is placed, broken, wrenched or loaded (Sable restarts from NBT on ships, so rebuild in `initialize`/on load). Refuses joins per D16 and D30, storing the reason on the tube for the goggles.
- **Taking items**: item handlers via the `Capabilities.ItemHandler.BLOCK` capability; depots and belts via Create's `TransportedItemStackHandlerBehaviour` / `DirectBeltInputBehaviour` (as the funnel does); floor via an AABB of the one block space in front of the mouth, only on the Inlet's turn.
- **Output**: insert through the capability on the output face, or Create's `DirectBeltInputBehaviour` for belts and depots; else spawn an item entity at the face, moving outward like the Chute's.
- **Sync**: tube loads (stack, progress at entry, direction) sent when a load enters or leaves a tube; the client interpolates with the network speed. The gulp is a block event; the cog's rotation is Create's own kinetic sync.
- **Rendering**: the hollow cog as a Flywheel part that turns like a cogwheel (memory: Flywheel where possible); items in tubes and passing through the cog on the BER; the gulp flap as a part kicked by the block event.
- **Saving**: tube: `Load` (stack, progress, direction). Inlet: cooldown, filter (Brass). Receiver: round-robin index, fractional `Carry`. Joints in the blockstate.
- **Config** (`CMConfig`): `clockworkReceiverStress` (4.0 SU per RPM), `clockworkTubeLoadsPerRpm` (1/64), `clockworkTubeBlocksPerRpm` (1/16), `clockworkInletCooldown` (8 ticks), `clockworkTubeMaxLength` (256).
- **Data**: four recipes, loot tables (drop themselves), `mineable/pickaxe`, lang (names, tooltips, goggles, Ponder, advancement), advancement, Display Link source. No sound files (D37).
- **Compatibility**: ids have no "create"; all four have BlockItems (Carry On); tubes on Aeronautics ships rebuild from NBT.
- **Tests (game tests)**: breaking a tube mid-transit drops exactly one load; unloading a chunk mid-transit loses and copies nothing; two Receivers never join; 256-tube cap; backup stops Inlets; round robin shares fairly; Brass filter and count; redstone on an Inlet; output full waits, no output drops.

# ONE MINUTE UNDERSTANDING

Clockwork Tubes suck items from many places into one. Put a Clockwork Inlet on a chest, over a depot or belt, or hanging over a farm's floor. Run Clockwork Tubes from every Inlet, branching however you like, to one Clockwork Receiver. Mesh a cogwheel with the Receiver's hollow cog and items slide past the tube windows and pop out through the middle of the turning cog, into a chest or belt or onto the floor. Faster cog, more items, more stress. Plain Inlets take one item at a time; Brass Inlets filter and take whole stacks. One Receiver per network, up to 256 tubes, and nothing is ever lost or duplicated.

# WHAT IT IS

Three andesite-age blocks and one brass upgrade that build a suction network for items: Inlets take, Tubes carry, one cog-driven Receiver delivers. They're built in the Trade Bot's and Dynamo's bot-made style, and the Inlets gulp.

# WHY IT EXISTS

- Nothing in Create gathers from many places into one, in any direction, without belts.
- Item pipes were left out of the Fluid and Power Lines; this is their own, Create-fair answer.
- More bot-built machines, building towards the Clockwork Wastes' story.

# HOW PLAYERS USE IT

- Put an Inlet where items collect, facing any way: chest, depot, belt or floor.
- Run tubes from each Inlet to one place.
- Put the Receiver there, output into a chest or belt.
- Mesh a cogwheel with its hollow cog; speed sets the rate.
- Use Brass Inlets to filter or take whole stacks; redstone switches an Inlet off.

# HOW IT FITS CREATE

- Powered through a small hollow cog that meshes with Create's cogwheels; 4 SU per RPM.
- Inlets work like funnels: one item plain, stacks and filters in brass.
- Takes from and gives to chests, vaults, depots and belts.
- A Clutch stops the network; goggles, Display Link and Ponder all covered.
- Works on Aeronautics ships; pauses on moving contraptions.

# IMPORTANT RULES

- One Receiver per network; a tube never joins two.
- Items only ever flow towards the Receiver.
- Each tube holds one load; full tubes back up, Inlets stop.
- Rate = RPM ÷ 64 loads a second; travel = RPM ÷ 16 blocks a second.
- Plain Inlet: 1 item a load. Brass: up to a stack, filtered.
- Each Inlet waits at least 8 ticks between loads; Inlets take turns.
- Receiver output: insert into the block in front, or drop if there's no block.
- Nothing moves unless the Receiver's chunk is running; no chunk is ever loaded.
- At most 256 tubes per network.
- Wrench a tube's side to close a joint.

# EDGE CASES

- Tube broken with an item inside: the item drops there; others carry on or wait.
- Receiver's chest is full: everything waits in the tubes.
- Two networks each with a Receiver meet: the tube stays capped.
- Inlet's chunk unloaded: it skips its turn.
- Overstressed or Clutch open: nothing moves, loads stay put.
- Receiver broken: the network stops, loads wait.
- Inlet over a floor and a hopper both see an item: first one takes it.
- On a moving contraption: paused, resumes when placed.
- Picked up with Carry On: tubes keep their load, Inlet and Receiver keep settings.
- Player dies under a floor Inlet: their drops get sucked up, like a hopper would.

# DEVELOPMENT CHECKLIST

- [ ] Blocks and BlockItems: `clockwork_tube`, `clockwork_inlet`, `brass_clockwork_inlet`, `clockwork_receiver`
- [ ] Receiver: kinetic block entity, `ICogWheel` small-cog meshing, stress 4 SU/RPM via config
- [ ] `TubeNetwork`: BFS from the Receiver, 256 cap, shortest-route next hops, lazy rebuild, refusal reasons
- [ ] Tube block: six joints, wrench toggle, waterlogged, one load per tube, saved
- [ ] Inlet: six facings (D27a), taking from item handlers, depots, belts and the floor block space; redstone off; 8-tick cooldown
- [ ] Brass Inlet: Create `FilteringBehaviour`, count up to a stack
- [ ] Round robin, backup, join order at T-joins
- [ ] Receiver output: insert, belt/depot input, else drop; full means wait
- [ ] Placement rules (D27): on a tube, joins it; otherwise points into the block
- [ ] Unloaded-chunk rules; never load chunks
- [ ] Contraption pause; Aeronautics rebuild from NBT
- [ ] Sync: tube loads, client interpolation; gulp block event
- [ ] Rendering: Flywheel hollow cog part; BER for items in tubes and through the cog; gulp flap
- [ ] Goggles on all three blocks
- [ ] Display Link source: items moved a minute
- [ ] Config keys in `CMConfig`
- [ ] Recipes (4), loot tables, `mineable/pickaxe`, lang, subtitles
- [ ] Sound: Receiver `isNoisy()` false, so Create's own cog sound is all it makes (D37); no sound files
- [ ] Ponder "Down the Tubes"; advancement "Down the Tubes"
- [ ] Game tests (Technical Notes list)
- [ ] Design brief, Blockbench model and textures in the bot family, before any of the above goes in

# "IDIOT TEST"

- Inlets suck items in, facing any way.
- One Receiver spits them out.
- Tubes carry items between them; you can see them.
- Only one Receiver per network.
- Receiver runs on a cog; items pass through its middle.
- Faster cog: more items, more stress.
- Plain Inlet: one item. Brass: stacks, with a filter.
- Redstone turns an Inlet off; a Clutch stops everything.
- Full chest at the end: everything waits, nothing lost.
- Max 256 tubes; no chunk loading.

# COMPLETENESS SCORE

Gameplay: 95% · World Integration: 90% · Balance: 85% · Progression: 90% · Automation: 95% · Edge Cases: 90% · **Overall Design Readiness: 85%**

- **Gameplay 95%**: the gulp's exact motion is the art director's to set.
- **World Integration 90%**: belt pickup through Create's behaviours and claim-mod handling are assumed, not tried.
- **Balance 85%**: 4 SU/RPM and RPM ÷ 64 are untested in play; check a full-speed brass network against belts and Brass Funnels.
- **Progression 90%**: the recipes aren't checked for JEI conflicts; the Encased Fan makes the Receiver need iron.
- **Automation 95%**: redstone and Clutch control are shown only in Ponder.
- **Edge Cases 90%**: piston-pushed tubes with loads and join order under heavy load need game tests.
- **Overall 85%**: Balance is the lowest. Nothing blocks development; tune the numbers in the first playtest.
