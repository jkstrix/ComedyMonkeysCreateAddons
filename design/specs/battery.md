# Battery and Industrial Battery: Systems Spec

Status: ready for development (look to be designed)  ·  Related: [Fluid and Power Lines spec](fluid-and-power-lines.md), brief to come (`design/briefs/battery.md`), quick read `battery.page.html`

## Concept

The user (2026-10-26): "This is a battery block. You can connect them together (like you can do magma generators), they have connectors on the side in the same way, but also ones front and back. If you connect 3x3x3 you get a bigger industrial battery multiblock that can go up to 9 high. Bigger multiblock battery has connectors like the poles that can only be connected to poles. Make it interesting and dynamic and fun."

Clarified: a Battery is a block that stores FE for the Power Lines network. Batteries that touch share one store, like touching Lava Generators. Fill a 3×3 footprint 3 to 9 blocks tall with them and they become one Industrial Battery, grid storage that hangs off the pole network by cables tied to Line Posts.

## Decisions

| # | Area | Decision | Source |
|---|---|---|---|
| D1 | What it is | A block that stores FE. On a Power Lines network it fills from what generators make beyond what's used, and gives it back when they fall short | user; Fluid and Power Lines D14 ("Fills stores, rest lost"; shortfall "power is shared") |
| D2 | Joining | Batteries that touch through their connectors join and share one store, like touching Lava Generators | user ("like you can do magma generators") |
| D3 | Connectors | Connectors on the sides, the same as the Lava Generator's, and on the front and back too; none on the top or bottom, so stacked Batteries don't share until they form an Industrial Battery | user; defaults approved round 3 |
| D4 | Industrial Battery | Batteries filling a 3×3×3 become one bigger Industrial Battery, up to 9 high (3×3 footprint, 3 to 9 tall) | user |
| D5 | Industrial connectors | Connectors like a pole's insulators, which can only be connected to poles | user |
| D6 | Feel | Interesting, dynamic and fun | user |
| D7 | Network rules | Networks plan once a second at most and only while a device is live; never load extra chunks; a battery is a store (both takes and gives): it charges from spare and discharges when short | Fluid and Power Lines spec, existing PowerLines code |
| D8 | Look | Dark, muted, Create-like; accents sparingly; previewed in Blockbench before it goes into the mod | user's standing art rules |
| D9 | Forming | Plain Batteries filling a 3×3 footprint, 3 to 9 tall, form it by themselves, no other parts. A column forms from the bottom up to 9; layers above that stay plain Batteries. A bank under 3 high never forms | user, round 1; defaults round 3 |
| D10 | Industrial connections | Only cables hung from its insulators to Line Posts (any tier). Wires and machines beside it don't connect, and it never joins plain Batteries beside it: it's grid storage | user, round 1; defaults round 3 |
| D11 | Far away | Both kinds keep charging and discharging while unloaded, from a record kept by the network (one per joined bank or Industrial Battery), like generators on paper | user, round 1 |
| D12 | Fun | Charge you can see: cells on the front light as it fills, a flow light shows charging or draining, and the Industrial Battery has a big level column that rises and falls. Lightning charges it. A broken Battery keeps its charge as an item. (Redstone control was not chosen) | user, round 1 |
| D13 | Numbers | Battery: 10,000,000 FE, in or out at up to 2,048 FE/t. Industrial Battery: 20,000,000 FE per block (540M at 3 high, 1.62B at 9); its rate is limited only by the cables hung on it. All in the config | user, round 2 ("Smaller"); raised 20 times after play (2026-10-26: "the capacity needs boosting, by 20 times") |
| D14 | Forming and coming apart | Forming adds every Battery's charge together. Breaking a block re-forms what's left if it can (D25): the broken one drops with its share, Batteries left out of the new tower are plain again with theirs, and the new tower holds the rest. Nothing is lost or made | user, round 2; revised in the what-ifs |
| D15 | Insulators | Four, one on each top corner, like a pole head with four branches. Up to 2 spans each, any tier; one tier and colour per insulator; the battery is one junction per colour, like a pole | user, round 2 |
| D16 | Lightning | A Lightning Rod on top of a Battery or the Industrial Battery catches strikes (rods draw lightning in storms): each strike adds 250,000 FE (scaled to the smaller numbers), with a crackle and arcs over it | user, round 2; amount from round 3 defaults |
| D17 | Bank rate | A joined bank moves up to 2,048 FE/t per Battery in it | defaults round 3 |
| D18 | Facing | Placed facing the player; the cells and flow light are on the front | defaults round 3 |
| D19 | Goggles | Charge and capacity, FE/t in or out, time to full or to empty, and the network line | defaults round 3 |
| D20 | Moving | Carry On and contraptions keep the charge; it doesn't charge or discharge while moving, and joins again once placed | defaults round 3 |
| D21 | Danger | No explosions. Blown up, it drops with its charge | defaults round 3 |
| D22 | Sound | A quiet hum that follows the flow; form, unform and lightning sounds (through the sound workflow) | defaults round 3 |
| D23 | Teaching | A Ponder scene for both; an advancement for forming an Industrial Battery | defaults round 3 |
| D25 | Breaking a block | The tower re-forms smaller if it can: the tallest full run of 3 or more layers left re-forms (the lower one if two tie); the broken layer and any run under 3 layers go back to plain Batteries. Breaking a top-layer block takes it one layer shorter | user, what-ifs (round 1 and the split follow-up) |
| D26 | Side by side | Two Industrial Batteries that touch stay separate; link them through poles | user, what-ifs |
| D27 | Too tall | A column of 10 or more: the bottom 9 form; layers above stay plain (and can form their own tower if 3 or more) | user, what-ifs |
| D28 | Front | The tower faces the way of the Battery whose placing completed it, so you choose the front as you finish it; the level column is on that face | user, what-ifs |
| D29 | Bigger than 3×3 | Every full 3×3 forms (3×6 makes two towers, side by side and separate). Where 3×3s overlap (4×4), one forms, from the lowest corner; the rest stay plain Batteries, not joined to it | user, what-ifs |
| D30 | Full when struck | A strike on a full battery still arcs and crackles; the extra is lost | user, what-ifs |
| D31 | Many lines, many colours | A Battery (or an Industrial Battery) takes lines of any tiers and colours. Lines that meet one battery are one network through it, as through a generator, and its one store serves every circuit: it gives to each by what its machines want and fills from those with generators. Cables of different colours still never join each other | user (2026-10-26: "joining them to two networks merges the networks, same as any join to two networks"; "batteries can connect to multiple colours") |
| D32 | Building it up | Like Create's vaults: once a tower stands, a Battery placed on its top (or under its bottom) fills the whole 3x3 layer, taking nine from the stack (only with enough in hand and the layer clear), and the tower grows. Shift-wrenching picks a Battery up, charge and all | user (2026-10-26: "form like vaults. you place one block down and nine are placed (after you have placed 3 layers)"; "shift wrench should pick blocks up") |
| D24 | Recipe | Battery: Brass Casing, 4 Copper Sheets, 2 Redstone and an Electron Tube | defaults round 3 |

## Open Questions

None. (The look, personality of the motion and sounds are the art director's, in the brief.)

## Core Loop

1. Build generators and lines; machines run, and what's spare is lost.
2. Put Batteries on the line: what's spare fills them, and they carry the machines through gaps (fuel running out, a turbine turned down, a night).
3. Grow the bank (touching Batteries share), then fill a 3×3 column and it becomes an Industrial Battery: hang brass from its insulators to the pole network, and it backs up the whole grid.
4. Watch it: the cells and the level column show how full it is and which way it's flowing; a storm with a Lightning Rod on top tops it up.

## Behaviour / Rules

1. **A Battery** stores up to 10,000,000 FE. Its connectors are its two sides, front and back. Power Lines and machines connect there; anything that gives or takes FE through a connector uses it.
2. **Touching Batteries** (connector to connector) are one bank with one store: the sum of their capacities, moving up to 2,048 FE/t per Battery. Stacked Batteries (top to bottom) aren't joined.
3. **On a network** a Battery or bank is a store: it fills from what the generators make beyond what's used (after machines), and gives when they fall short, shared like a local setup.
4. **The Industrial Battery** forms when a 3×3 footprint is filled with Batteries 3 to 9 high (from the bottom; layers above 9 stay plain). Its charge is every Battery's added together. It stores 20,000,000 FE per block.
5. **Its insulators** are four, on the top corners. Cables tied there go only to Line Posts (any tier, one tier and colour per insulator, two spans each); it's one junction per colour, like a pole. Wires and machines beside it, and plain Batteries beside it, don't connect.
6. **Breaking a block** of it re-forms what's left if it can: the tallest full run of 3+ layers re-forms (the lower if two tie), the broken one drops with its share, and the rest go plain with theirs. Breaking a top-layer block takes it one layer shorter.
6a. **Its front** is the way the Battery that completed it faces. Two towers that touch stay separate; over 9 high, the bottom 9 form; in a footprint bigger than 3×3, every full 3×3 forms (overlapping ones: one, from the lowest corner).
7. **Far away**, a bank or an Industrial Battery keeps working from its record on the network, filled and drawn like a generator on paper; loaded again, it takes the record on.
8. **Lightning**: a Lightning Rod on top catches strikes; each adds 250,000 FE (up to full), with a crackle and arcs.
9. **Picked up or broken**, a Battery keeps its charge as an item (shown in its tooltip and as a bar); placed, it joins a bank with it.
10. **What it shows**: cells on the front light with the charge; the flow light shows charging, draining or idle; the Industrial Battery's level column rises and falls.

## Progression

Mid game. It needs Brass Casing and an Electron Tube, and it's only useful once there's a generator and a line, so it comes after the Lava Generator and Power Lines. The Industrial Battery takes 27 to 81 Batteries and brass cable on poles: a late-game grid project.

## Balance

- **Cost against reward**: a Battery (a casing, four sheets, two redstone, a tube) holds 10,000,000 FE, about 8 minutes of a Lava Generator's output, or a full-stress motor for about 27 seconds; it's for evening out supply, not replacing generation. An Industrial Battery at 9 high (81 Batteries) holds 1.62B FE, a full motor for about 73 minutes. The Industrial bonus is double the per-block capacity.
- **Exploits**:
  - *Duplication by breaking mid-form or mid-move*: charge is only ever split or summed (D14, D20); a broken block takes its share and the rest keep theirs. Tests cover forming, breaking and picking up.
  - *Duplication on paper*: records apply only how far they moved (the generator records' rule), taken on once when loaded.
  - *Infinite lightning*: strikes are rare and only in storms; a rod catches only natural lightning (a Channeling trident can call strikes, which is a vanilla cost to the player and fine).
  - *Lag*: banks and Industrial Batteries are one record each, not one per block; the network plans once a second.
- **Overpowered?** Storage doesn't create power; generation is still needed. The double capacity rewards building, and 1.62B is about half an hour of a turbine flat out (a night and a bit).
- **Useless?** Without batteries, spare power is lost and gaps stop machines; the batteries fix exactly that.

## Create Integration

| System | Interaction | Notes |
|---|---|---|
| Shafts and gears | None | It's FE; Electric Motors turn it into rotation |
| Belts, depots | None | |
| Contraptions | Moved, it keeps its charge; doesn't charge or discharge while moving | D20; Aeronautics ships the same |
| Trains, elevators | Carried as a contraption block | |
| Logistics, packages | None | |
| Vaults, funnels | None | |
| Deployers, arms | None | |
| Redstone, comparators | None | Redstone control wasn't chosen |
| Goggles | Charge, flow, time to full or empty, network line | D19 |
| Ponder | One scene: a Battery, a bank, forming the Industrial Battery, poles, lightning | D23 |

## Technical Notes

- **Blocks**: `battery` (facing; a formed flag and part position for the Industrial Battery's model, or a renderer on the bottom-front-middle block). One block entity type for both.
- **Network**: a Battery is an energy capability on its connector sides (takes and gives); a bank shares one store like `LavaGeneratorBlockEntity.share()`, or better, one store object keyed by the bank (so it's one record). The Industrial Battery's insulators work like a Line Post head: spans tie to them (`PowerLineItem.hang`, `LinePostBlockEntity` spans), and `PowerLines.readPole` reads it as a pole piece whose node is the battery, with a store end.
- **Paper**: a store record in `PowerGrid.Records` (store, capacity, at), drawn and filled by the plan while away; `back()` takes it on once (deltas).
- **Saving**: charge in the block entity (each block its share when formed, so breaking one is exact); the item keeps it in a data component.
- **Config** (`CMConfig`): battery capacity, battery rate, industrial capacity per block, lightning amount.
- **Compatibility**: Carry On pickup keeps charge (BlockItem); Aeronautics physics objects as contraptions.
- **Data**: recipe, loot table copying the charge, lang (block, tooltip, goggles, subtitles), advancement, sounds, Ponder.
- **Tests**: bank sharing, forming at 3 and 9 high (and 10 stays 9 + plain), breaking shares out exactly, item keeps charge, network charges from spare and discharges when short, paper record while away, lightning adds 250,000, insulator spans only to poles.

# ONE MINUTE UNDERSTANDING

A Battery stores power for your Power Lines. Spare power fills it, and when your generators fall short it gives it back. Batteries that touch side to side share one store. Fill a 3×3 column, 3 to 9 high, with them and they become one Industrial Battery: a big tank of power you wire into your pole network by hanging cables from its four top insulators. You can watch the charge on the cells and the level column, and a Lightning Rod on top tops it up in a storm.

# WHAT IT IS

A storage block for FE on the Power Lines network. Touching Batteries share a store; a full 3×3 column of them becomes a multiblock Industrial Battery that connects only to poles.

# WHY IT EXISTS

- Spare power is lost without it ("Fills stores, rest lost").
- Gaps in supply stop machines; a battery carries them through.
- A late-game grid project: big storage on the pole network.

# HOW PLAYERS USE IT

- Place Batteries on a line; they fill from spare power.
- Put them side by side to make one bigger bank.
- Fill a 3×3, 3 to 9 high, to form an Industrial Battery.
- Hang cables from its four top insulators to Line Posts.
- Put a Lightning Rod on top to catch storms.

# HOW IT FITS CREATE

- Brass Casing and Electron Tube in the recipe.
- Electric Motors turn its FE into rotation.
- Survives contraptions, Carry On and Aeronautics ships, charge kept.
- Goggles show charge, flow and time left.
- A Ponder scene explains banks and forming.

# IMPORTANT RULES

- Battery: 10,000,000 FE, 2,048 FE/t; sides, front and back connect.
- Touching Batteries share one store; stacked ones don't.
- A 3×3 column 3–9 high forms an Industrial Battery.
- Industrial: 20,000,000 FE per block; only poles connect, via its four insulators.
- Charge is always kept: summed on forming, shared out on breaking; what's left re-forms if it can.
- Far away, banks and Industrial Batteries keep working on paper.
- Lightning Rod on top: 250,000 FE a strike.
- A broken Battery keeps its charge as an item.
- No explosions; it never loads chunks.

# EDGE CASES

- A 3×3×10 column: the bottom 9 form; the top layer stays plain.
- A 3×3 with one block missing: doesn't form.
- Two towers side by side, or a 3×6 footprint: each forms on its own; they don't join.
- Breaking a block: the tallest run of 3+ layers left re-forms; the rest go plain, charge shared out.
- A plain Battery beside an Industrial Battery: not joined.
- Moved on a contraption: keeps charge, does nothing until placed.
- Lightning on a full battery: nothing more is stored.
- Unloaded mid-charge: its record carries on; loaded, it takes the record on once.
- Blown up: drops with its charge.
- An insulator with a copper span: only copper (and that colour) ties on there.

# DEVELOPMENT CHECKLIST

- [ ] `battery` block and block entity: facing, connectors on four sides, charge, item with charge (data component, tooltip, bar)
- [ ] Bank sharing: one store per touching group (connectors only), rate 2,048 FE/t per Battery
- [ ] Industrial Battery forming: 3×3 from the bottom, 3–9 high, charge summed; coming apart shares it out; formed model or renderer with the level column
- [ ] Insulators on the formed battery: span tying (pole-only, tier and colour per insulator), read by PowerLines as a pole junction with a store
- [ ] Network: batteries as stores (existing); Industrial Battery as a store at its pole node
- [ ] Paper records for banks and Industrial Batteries; `back()` deltas
- [ ] Lightning Rod on top: strike adds 250,000 FE, arcs and crackle
- [ ] Charge display: front cells, flow light; Industrial level column (Flywheel visual)
- [ ] Goggles tooltip; lang; tooltips
- [ ] Config: capacity, rate, industrial per block, lightning amount
- [ ] Recipe, loot table (keeps charge), advancement
- [ ] Carry On and contraption handling (charge kept, no flow while moving)
- [ ] Sounds through the sound workflow (hum, form, unform, lightning)
- [ ] Ponder scene; JEI info
- [ ] Game tests (see Technical Notes)

# "IDIOT TEST"

- A Battery stores spare power from your lines.
- It gives power back when generators fall short.
- Touching Batteries side to side share one store.
- Stacked Batteries don't share until they form.
- Fill a 3×3 column, 3–9 high: Industrial Battery.
- The Industrial Battery only connects to Line Posts.
- Cables tie to its four top insulators.
- Charge is never lost: breaking one shares it out.
- Lightning Rod on top: storms charge it.
- Far away, batteries keep working on paper.

# COMPLETENESS SCORE

Gameplay: 95%: the look (how the cells and the column read) is the brief's · World Integration: 90%: the Industrial Battery's pole node in PowerLines needs a short technical design · Balance: 90%: numbers are set and in the config; tune after play · Progression: 90%: recipe set; the Industrial Battery has no extra cost beyond its Batteries and cable · Automation: 95%: nothing to automate by hand · Edge Cases: 90%: tested list above · **Overall Design Readiness: 90%**
