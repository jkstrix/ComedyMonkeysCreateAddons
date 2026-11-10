# Pointed Dripstone Generator: Systems Spec

Status: ready for development  ·  Related: [Cobble Generators brief](../briefs/cobble-generator.md), [Cobble Generators dossier](../dossiers/cobble-generator.md), [Monster Cage and Fish Tank spec](monster-cage-and-fish-tank.md) (screen rules)  ·  Quick read: [pointed-dripstone-generator.page.html](pointed-dripstone-generator.page.html)

## Concept

In the user's words: "new block that compliments the stone generators, pointed dripstone generator. You add 4 pointed
dripstone and supply a water to it and it generates pointed dripstone", and "you should need to supply SU to the back
also".

Clarified: a sealed, shaft-driven generator in the stone generators' family. Seeded with four Pointed Dripstone and fed
water by pipe, it grows Pointed Dripstone inside, faster with faster rotation, and hands it out to automation or
through its wrench screen.

## Decisions

| # | Area | Decision | Source |
|---|---|---|---|
| D1 | Name | **Pointed Dripstone Generator** (Wireless: **Wireless Pointed Dripstone Generator**) | user, 2026-11-10 |
| D2 | Product | Pointed Dripstone | user, 2026-11-10 |
| D3 | Seed | It needs **4 Pointed Dripstone** put in before it makes anything | user, 2026-11-10 |
| D4 | Water | It needs **water supplied** to it | user, 2026-11-10 |
| D5 | Power | Rotation (SU) through a **shaft into its back** only, as the stone generators | user, 2026-11-10 |
| D6 | Family | Complements the stone generators (Cobblestone, Basalt, Blackstone, Tuff): the same sealed process-box family | user, 2026-11-10 |
| D7 | Pace | **Grown pace, sped up by rotation**, not drill pace (D11) | user, round 1 |
| D8 | Seed kept | The 4 Pointed Dripstone **stay in** as the stalactites it grows from: shown in the window, given back when the block breaks; it won't run without all four | user, round 1 |
| D9 | Water used | **Used up**, a little per Pointed Dripstone (D12), piped in (Create pipes, Fluid Lines, a Wellspring) into a small internal tank | user, round 1 |
| D10 | Tiers | **Andesite** and **Wireless Andesite** (Redstone Link frequency in two slots on top, as the family), like the Tuff Generator; no Brass | user, round 1 |
| D11 | Pace number | *(Superseded by D35: one every 5 s.)* One Pointed Dripstone every **10 s at 256 RPM**, in proportion to speed (20 s at 128, 40 s at 64, 80 s at 32): 200 × 256 / RPM ticks. About 360 an hour flat out. Config | user, round 2 |
| D12 | Water numbers | **100 mB per Pointed Dripstone**, taken when the tips start growing (400 mB for the four, D35); a **1,000 mB** tank. Config | user, round 2 |
| D13 | Stress | **4 SU per RPM**, as a Mechanical Drill (1,024 SU at 256 RPM); **0 while idle**. Config | user, round 2 |
| D14 | Storage | Holds **64** Pointed Dripstone, then stops | user, round 2 |
| D15 | Seed in | **Right-click with Pointed Dripstone** puts in as many as it needs, up to 4. Automation can't put the seed in or take it out | user, round 2 ("hand and wrench, same UI rules as the mob cages") |
| D16 | Wrench screen | **Right-click with a Wrench** opens its screen, as the cages: the seed slot (take the seed out here), the water gauge, the growth gauge (time per Pointed Dripstone at the current speed) and the output, in the Create-style GUI kit. Sneak + Wrench picks the block up, as Create blocks do | user, round 2 (cage rules); GUI kit rule |
| D17 | Hand output | The screen's output **works by hand**, with the quick-move buttons and mouse wheel | user, round 3 |
| D18 | Automation output | Funnels, hoppers, chutes, pipes and arms take the output from **any side** | user, round 2 (family rules) |
| D19 | Water in | **Water only** (other fluids refused), into **any side but the front and back** | user, round 2 |
| D20 | Idle | It stops, and puts **no stress** on the network, when full, switched off by redstone, missing seed, or short of water | user, round 2 |
| D21 | Redstone | Any wired signal switches it off; the Wireless one also listens on its Redstone Link frequency | user, round 2 (family rules) |
| D22 | Comparator | 0 when empty, else 1 + 14 × stored / 64, as the family | user, round 2 (family rules) |
| D23 | Breaking | Drops the block, the seed and the stored Pointed Dripstone. The water is lost | user, round 2 |
| D24 | Moved | Pistons, contraptions and Aeronautics moves keep its seed, water, output and growth; it **doesn't run while moving** | user, round 2 |
| D25 | Facing | The window faces you when placed (4 horizontal facings) | user, round 2 (family rules) |
| D26 | Dimensions | **Works anywhere**, the Nether included: it's sealed, like a Create Fluid Tank | user, round 3 |
| D27 | Recipe | Crafting table: andesite alloy, **2 Iron Blocks** (the hammer is a swinging block of iron, not a drill: user, 2026-11-10), an andesite casing, 2 Dripstone Blocks and a Create Fluid Pipe. Wireless: the generator + a Create Transmitter, shapeless, as the family | user, round 2 |
| D28 | Teaching | Its own **Ponder scene, "Drip, Drip, Drop"**; a JEI info page; one advancement, **"Biggest Drip"** ("Grow Pointed Dripstone in a Pointed Dripstone Generator") | user, rounds 2 and 3 |
| D29 | Goggles | Header "Pointed Dripstone Generator:", stored / 64, water / 1,000 mB, then one status: Switched off by redstone · Full · Needs 4 Pointed Dripstone · Needs water · Needs rotation · N per minute; then Create's kinetic and stress lines | family rule (goggles show stored and status), user round 2 |
| D30 | Look | The sealed process-box shell with a window showing the four stalactites and the new tip growing; designed later by the art director and previewed in Blockbench first | existing mod rules (family look, preview in Blockbench first) |
| D31 | Harvest | **Automatic**, like the stone generators' drill and the Auto Harvester's cut: each grown tip is broken off inside and drops into the store. The player never harvests | user, 2026-11-10 ("them dropping should be automatic like the stone generator or the farm") |
| D32 | Breaker | **A small Create-style hammer knocks the grown tip off**, with a fun animation: the swing, the crack, the tip falling into the store. The piece goes into the output on the hit. The knock is part of each cycle, so the pace (D35) doesn't change | user, 2026-11-10 ("fun animation on cracking the spikes"; hammer chosen) |
| D34 | Sound | **Vanilla's Pointed Dripstone break sound** on each knock, and nothing custom: no sound design pass | user, 2026-11-10 ("for sounds I just want the vanilla sounds for breaking dripstone") |
| D33 | Rhythm | **All four tips grow at the same time, then the hammer goes round knocking each off** (a quarter turn and a knock, 2 s each), and the cycle starts again. Replaces "the four take turns, one per cycle" | user, 2026-11-10 ("lets have them all grow at the same time then the hammer moves to each one with a view of doubling the output") |
| D35 | Pace, doubled | **One every 5 s at 256 RPM** (about 720 an hour): a cycle of four takes 20 s, 12 s growing and 8 s knocking. The round of knocks is the same at any speed; only the growing slows (a cycle is 400 × 256 / RPM ticks). 400 mB of water is taken as the four start growing (still 100 mB a piece). Replaces D11's 10 s | user, 2026-11-10 (doubling, as D33) |

## Open Questions

None. Look, sounds and model come next, from the art director's brief.

## Core Loop

Load four Pointed Dripstone, pipe water to a side, turn the shaft in the back → the four stalactites grow their tips
together, a little faster the faster it turns, using 100 mB of water each, then a hammer goes round knocking each
grown tip off into the store, one on each strike → funnels pull them out (or take them in the wrench
screen) → craft Dripstone Blocks, Drip Templates for Wellspring upgrades, or traps. Repeat: keep the water and rotation
up and the output emptied.

## Behaviour / Rules

1. **Placing**: the window faces the placer. Only the back takes a shaft (D5, D25).
2. **Seeding**: right-click with Pointed Dripstone; it takes up to 4 from the stack. Without all 4 it's idle:
   "Needs 4 Pointed Dripstone" (D3, D8, D15).
3. **Water**: any side but the front and back accepts water from pipes, Fluid Lines or a Wellspring, up to 1,000 mB;
   anything but water is refused (D12, D19).
4. **Growing**: when it's running and the tank holds 400 mB (100 a piece), the four tips start growing together and
   the water is taken. The growing takes 400 × 256 / |RPM| ticks less the round of knocks (8 s, any speed). Then the
   hammer goes round: for each stalactite a quarter turn (10 ticks) and a knock (30); on each strike one Pointed
   Dripstone goes into the output. After the fourth, the next four start growing (D12, D31–D35).
5. **Running** = turning, not overstressed, not switched off, seeded, water for the next growth (or one already
   growing), and room in the output. Otherwise it's idle at **0 stress**, and a growth part done waits where it is
   (D13, D20).
6. **Switching off**: a wired redstone signal; the Wireless one also a Redstone Link on its frequency (D21).
7. **Output**: up to 64; out through funnels, hoppers, chutes, pipes and arms from any side, or by hand in the wrench
   screen with quick moves (D14, D17, D18).
8. **Wrench screen**: seed slot, water gauge, growth gauge, output (D16).
9. **Comparator**: how full the output is (D22).
10. **Breaking**: drops block, seed and output; water lost (D23).
11. **Moving**: keeps everything; doesn't grow while moving (D24).

## Progression

**Early to mid game (the andesite age).** It needs two Iron Blocks, an andesite casing and a Fluid Pipe, plus two
Dripstone Blocks and four Pointed Dripstone from a dripstone cave, and a steady water supply (a Create pump on a water
source, or a Wellspring). It unlocks easy Pointed Dripstone for Dripstone Blocks, the Wellspring's Drip Templates (two
Pointed Dripstone each) and dripstone traps.

## Balance

- **Output against vanilla**: about 720 an hour at 256 RPM (D35, doubled from 360 by growing all four at once),
  against roughly 60 an hour from a big 100-column vanilla farm: about a dozen big farms in one block, judged as the
  cages and tanks are, for 1,024 SU and 72 buckets of water an hour (D12, D13, D35).
- **Cost**: two Iron Blocks and a pipe up front; stress and water while running. Faster rotation is faster but costs more SU.
- **Exploits checked**:
  - Seed duplication: the seed is kept, not crafted back; breaking drops it once (it lives in the block entity only).
  - Breaking mid-growth: the growth is lost, never paid out early.
  - Contraptions: it doesn't run while moving, so no free growth in transit.
  - Infinite water (Create pumps, Wellsprings) is fine: water is a running cost, not the limiter; SU is.
- **Not overpowered**: it doesn't touch a Create challenge; Pointed Dripstone is a low-value block.
- **Not useless**: no Create or vanilla option grows dripstone in one block, and vanilla farms are huge and slow.

## Create Integration

| System | Interaction | Notes |
|---|---|---|
| Shafts and gears | Yes | Back only; 4 SU/RPM; 0 when idle |
| Belts and depots | Via funnels | Output any side |
| Contraptions, Aeronautics | Moved, not running | Keeps seed, water, output, growth |
| Trains, elevators | As contraptions | |
| Logistics and packages | Via funnels or Stock Links on its output | No special handling |
| Vaults, funnels, chutes | Yes | Output from any side |
| Pipes, Fluid Lines, Wellspring | Yes | Water into any side but front and back |
| Deployers and arms | Arms take output; nothing seeds it | Seed is hand only (D15) |
| Redstone, Redstone Links | Yes | Off switch; Wireless listens on its frequency |
| Comparators, display boards | Yes | Comparator from the output |
| Goggles | Yes | D29 |
| Ponder | Yes | "Drip, Drip, Drop" |

## Technical Notes

- Code: a new block and block entity in `content/cobblegen/` beside the stone generators (a `KineticBlock` /
  `KineticBlockEntity`), reusing their facing, redstone and frequency (`TopFrequencySlot`), comparator, output handler
  and idle-stress logic, plus a water tank (`FluidTank`, water only, side-restricted capability) and a seed slot that
  no capability exposes. Menu and screen on the Create-style GUI kit with `QuickTransferMenu`.
- Saved: seed count, output, water, growth progress, powered state, frequency (wireless). Synced: what the window and
  goggles show (seed, water level, growth progress, running state).
- Config (`CMConfig`, a `dripstoneGenerator` section): seconds per Pointed Dripstone at 256 RPM (5; a cycle of four is four times this), water per piece
  (100 mB), tank (1,000 mB), stress per RPM (4).
- Compatibility: BlockItem for Carry On; mod id rule unaffected; contraption keep via the family's move handling.
- Data: two blocks and items, blockstates, models, loot tables (drop self), recipes (D27), advancement, lang (names,
  tooltips, goggles lines, screen, Ponder, advancement), JEI info page, `ModBlocks.generators()` if it's listed there.
- No light emission: there's nothing hot inside (the stone generators glow from their lava).

# ONE MINUTE UNDERSTANDING

A Create machine that grows Pointed Dripstone. Put four Pointed Dripstone in by hand, pipe water into a side and turn a
shaft in its back. The four grow their tips together, then a little hammer goes round knocking each one off into the store:
four every 20 seconds at 256 RPM, one every 5 (slower at lower speeds), using 100 mB of water each. It holds 64. Funnels pull them out, or take them in the wrench screen. It stops, with no stress, when full, switched off,
unseeded or dry.

# WHAT IT IS

A sealed, shaft-driven generator in the stone generators' family. Four Pointed Dripstone seed it, piped water feeds it,
and rotation sets the pace.

# WHY IT EXISTS

- Pointed Dripstone grows painfully slowly in vanilla; farms are huge.
- It completes the generator family with a "grown" product.
- It feeds Dripstone Blocks and the Wellspring's Drip Templates.

# HOW PLAYERS USE IT

- Craft it from alloy, two Iron Blocks, a casing, two Dripstone Blocks and a pipe.
- Right-click it with four Pointed Dripstone.
- Pipe water into a side; turn a shaft in the back.
- Pull the output with funnels, or open it with a Wrench.
- Watch the goggles: stored, water, and why it's stopped.

# HOW IT FITS CREATE

- Rotation through the back shaft, 4 SU/RPM, none while idle.
- Water from Create pipes, Fluid Lines or a Wellspring.
- Output to funnels, chutes, hoppers, arms from any side.
- Redstone and Redstone Links switch it off.
- Survives contraption moves, but doesn't run while moving.

# IMPORTANT RULES

- Needs all 4 seed Pointed Dripstone, put in by hand only.
- Seed comes back out only in the wrench screen, or on breaking.
- Four Pointed Dripstone per 400 × 256 / RPM ticks (one every 5 s at 256 RPM); the knocks take a fixed 8 s.
- 100 mB of water per piece, taken as it starts; tank 1,000 mB.
- Water only, into any side but front and back.
- Holds 64; full, it stops.
- Idle (full, off, unseeded, dry, no rotation): no stress.
- Comparator reads the output.
- Works in every dimension.
- Andesite and Wireless Andesite only.

# EDGE CASES

- **Broken mid-growth**: the growth is lost; seed and output drop; water lost.
- **Water runs out mid-growth**: the growth already paid for finishes; the next waits.
- **Fewer than 4 seeds**: idle, "Needs 4 Pointed Dripstone".
- **Lava or another fluid piped in**: refused.
- **On a moving contraption**: keeps everything, doesn't grow.
- **Overstressed or stopped**: idle; a part-done growth waits.
- **In the Nether**: works; it's sealed.
- **Chunk unloaded**: nothing happens until it's loaded again (no catch-up).
- **Output full while funnels are blocked**: stops at 64, no stress.
- **Deployer or funnel offering Pointed Dripstone**: it goes nowhere: the seed is hand only and the output takes no inserts.

# DEVELOPMENT CHECKLIST

- [ ] Blocks and items: `pointed_dripstone_generator`, `wireless_pointed_dripstone_generator` (BlockItems)
- [ ] Block: facing on place, shaft at the back only, redstone off switch, comparator, wrench screen on right-click, sneak-wrench pickup
- [ ] Block entity: seed slot (4, hand only), water tank (1,000 mB, water only, sides but front/back), output (64, extract from any side), cycle timer (400 × 256 / RPM ticks for four: the tips growing together, then the fixed 8 s round of knocks), 100 mB per growth taken at start
- [ ] Idle logic and 0 stress when full, off, unseeded, dry or not turning; per-RPM stress from config
- [ ] Wireless frequency slots and Redstone Link listening (reuse `TopFrequencySlot`)
- [ ] Right-click seeding with Pointed Dripstone (takes up to 4)
- [ ] Menu and screen (GUI kit, `QuickTransferMenu`): seed slot, water gauge, growth gauge, output with quick moves and mouse wheel
- [ ] Goggles lines (D29)
- [ ] Saving and syncing (seed, output, water, progress, powered, frequency)
- [ ] Contraption and Aeronautics keep; no running while moved
- [ ] Drops on break: block, seed, output
- [ ] Config section `dripstoneGenerator` (seconds, water, tank, stress)
- [ ] Recipes: shaped crafting (D27); wireless shapeless with a Transmitter
- [ ] Loot tables, blockstates, models, lang (names, tooltips, goggles, screen, Ponder, advancement)
- [ ] Advancement "Biggest Drip"
- [ ] Ponder "Drip, Drip, Drop"; JEI info page
- [ ] Renderer: the four stalactites, the growing tip moving round them in turn, and the hammer's swing, crack and falling tip on each knock; frozen when idle (after the art brief)
- [ ] Knock feedback: vanilla's Pointed Dripstone break sound (`SoundEvents.POINTED_DRIPSTONE_BREAK`, referenced, not copied) and dripstone particles on the hit (D34)
- [ ] Look, model and sounds via the art director and Blockbench preview (D30)
- [ ] Game tests: grows at the right pace for RPM, takes 100 mB each, idles at 0 stress without seed / water / room / rotation, refuses other fluids, keeps the seed on break, seed not insertable by funnels or deployers
- [ ] Website entry and release notes when it ships

# "IDIOT TEST"

- Makes Pointed Dripstone inside a block.
- A hammer knocks each grown tip off.
- Put four Pointed Dripstone in by hand.
- Pipe water into a side.
- Turn a shaft into the back.
- All four grow, then hammered off: one every 5 s at 256 RPM.
- Uses 100 mB water each.
- Holds 64; full, it stops.
- Funnels or Wrench screen take them out.
- No stress when stopped, any reason; Wrench screen returns the seed.

# COMPLETENESS SCORE

Gameplay: 100% · World Integration: 100% · Balance: 95% · Progression: 100% · Automation: 100% · Edge Cases: 95% · **Overall Design Readiness: 95%**

- Gameplay 100%: loop, seed, water, pace, output and screen all decided.
- World Integration 100%: dimensions, weather (sealed: none), chunk unloading, multiplayer (no ownership, like the family) covered.
- Balance 95%: numbers set and judged against vanilla farms; the 5 s pace hasn't been play-tested yet.
- Progression 100%: andesite age, unlocks Drip Templates and Dripstone Blocks.
- Automation 100%: every step but seeding automates, by decision.
- Edge Cases 95%: covered on paper; the look (what the window shows when idle or dry) comes with the art brief.
- Overall 95%: the lowest areas; development can start, the art brief runs alongside.
