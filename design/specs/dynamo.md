# Clockwork Dynamo: Systems Spec

Status: built for 2.5.0  ·  Related: [brief](../briefs/clockwork-dynamo.md), [sounds](../sound/clockwork-dynamo/clockwork-dynamo.md), [Electric Motor brief](../briefs/electric-motor.md), [Lava Generator brief](../briefs/lava-generator.md), [Fluid and Power Lines spec](fluid-and-power-lines.md), [Battery spec](battery.md), [Trade Bot brief](../briefs/trade-bot.md), [Helper Bot brief](../briefs/helper-bot.md), Clockwork Wastes lore ([spec](clockwork-wastes.md) D36), quick read `dynamo.page.html`

## Concept

The user (2026-10-31): "We have ways to generate electricity from lava but we need a way to generate it from SU. This should be themed differently than the rest of the blocks: have textures themed on the Trade and Helper Bots. This will later become part of the backstory building in the Clockwork Wastes branch. It should be balanced and give us some loss when converting SU to electricity and then back to SU (using the Electric Motor)." Later: "The dynamo should have some whimsy like the bots, but not much."

Clarified: the Clockwork Dynamo is the Electric Motor run backwards. Set it with a wrench to take a fixed amount of stress off a Create shaft, and it gives 75% of what that stress would cost a motor, as Forge Energy for the Power Lines network. It's built in the bots' materials, and one bot touch gives it character: Trade Bot's copper aerial, which stands up while it works.

## Decisions

| # | Area | Decision | Source |
|---|---|---|---|
| D1 | What it is | Turns Create rotation (SU) into Forge Energy (FE) | user |
| D2 | Loss | SU → FE → SU through the Electric Motor always loses some: never a loop that gains | user |
| D3 | Look | Textures themed on the Trade Bot and Helper Bot, unlike the turbine-family look of the other electric blocks | user |
| D4 | Lore | Later part of the Clockwork Wastes backstory (automatons "found electricity and then turned on their masters", Wastes D36). That tie-in is designed in the Wastes branch, not here | user; Wastes spec D36 |
| D5 | Exchange rate | The Electric Motor's rate is the reference: 1 FE/t for every 16 SU (`motorFePerStress` 0.0625). The dynamo's rate is that, times its efficiency | existing mod rule (`CMConfig`) |
| D6 | Network rules | It's a generator on the Power Lines network: networks plan once a second at most, never load extra chunks, generators are passive | Fluid and Power Lines D19, D32 |
| D7 | House style | Dark, muted, Create-like; previewed in Blockbench before it goes in the mod; sound through the staged sound workflow | user's standing rules |
| D8 | Stress taken | Set with a wrench, like the motor: the SU it takes **at any speed** (stress impact per RPM = setting ÷ speed, like the Cuckoo Clock). **More power always takes more SU**: the setting fixes both, always in proportion (21⅓ SU per FE/t; double the power, double the SU). Shaft speed doesn't change either | user, round 1; confirmed 2026-10-31 ("This should take more SU the more it generates") |
| D9 | Efficiency | **75%**: FE/t = SU × 0.0625 × 0.75. 256 SU → 12 FE/t; 4,096 → 192; 16,384 → 768; 294,912 → 13,824. Config `dynamoEfficiency` (0.75) | user, round 1 |
| D10 | Tier | **One brass-tier block**, beside the Electric Motor and Lava Generator. One block covers the whole range; no small or multiblock version | user, round 1 |
| D11 | Unloaded | **Runs on paper** for the Power Lines network while its chunk isn't running, under D12's rule | user, round 1 |
| D12 | Paper share | **Only free sources count on paper.** When its chunk stops running, it notes the share of its shaft network's stress capacity that comes from free sources (block tag `comedymonkeysaddons:free_kinetic_sources`: Create's water wheel, large water wheel, windmill bearing, creative motor; pack makers can add more). On paper it gives its output × that share, **per source**: each free source counts only while it isn't running itself (it's away too, so nothing else can use it: the Wellspring rule), only for the dynamo that saw it last (re-routed wheels count once), and is dropped if it's broken or replaced. Electric Motors, Magma Engines, steam engines, Spring Engines, hand cranks and anything untagged count zero, so a motor ↔ dynamo loop can't run free while away. Stopped or overstressed when noted: nothing on paper. With Create's stress switched off it makes nothing at all | user, round 2 (closes the paper loop found in round 1); per-source rule from the adversarial review, 2026-10-31 |
| D13 | Bot look | The Trade and Helper Bots' palette and parts: andesite casing, dark blue-grey steel, muted brass rims and bands, copper, rivet lines, the bots' glow colours. **No face, no eyes.** Whimsy: "some, like the bots, but not much": just the aerial (D16) | user, round 2; revised mid-round 3 |
| D14 | Name | **Clockwork Dynamo** (`clockwork_dynamo`) | user, round 2 |
| D15 | Nowhere to put power | Keeps taking its set stress; FE that doesn't fit its store is lost, like the Lava Generator. The shaft network never flickers | user, round 2 |
| D16 | Whimsy | **Trade Bot's copper aerial with a brass bead**, on top. It **stands up** while it makes power, **droops** when idle (stopped, overstressed, or on a contraption), and gives a **little spring wobble** when its output changes (setting changed, starts, stops). Client-side only. Besides the shaft and the aerial, the **commutator** in its top window spins at the shaft's speed (mechanism, not whimsy); nothing else animates | user, round 3; commutator approved with the brief, 2026-10-31 |
| D17 | Faces | Six facings, placed like the motor. The shaft comes in on the **front** only (no pass-through). FE goes out of the **base** only (opposite the shaft), pushed every tick into what touches it; Power Lines connect there | defaults, round 3 |
| D18 | Screen | Wrench opens a screen with **stress only** (it follows the shaft's speed and direction). Steps: 256, 512, 1,024, 2,048, then the motor's steps 4,096 … 294,912. Read-out "Makes N FE a tick" only (user, 2026-10-31: no comparison with the generators). Server checks as the motor: within 8 blocks, may build; clamped to multiples of 256 | defaults, round 3 |
| D19 | Default setting | **256 SU** (12 FE/t): exactly one water wheel, never overstresses a new network | defaults, round 3 |
| D20 | Range | Max = the motor's max (`motorMaxStress`, 294,912 SU). Makes power at any speed above 0; stopped or overstressed, nothing | defaults, round 3 |
| D21 | Store | 2 seconds of its max output: 552,960 FE (`max × 40` ticks of FE, so it follows the config). Nothing can be pushed into it | defaults, round 3 |
| D22 | Goggles | Store, "Making N FE a tick" (or why not: "Not turning" / "Overstressed"), "Taking N SU (75% becomes power)", and Create's stress impact line | defaults, round 3 |
| D23 | On / off | No redstone control: a Create Clutch or Gearshift on its shaft does that (shown in Ponder) | defaults, round 3 |
| D24 | Moving | Makes nothing while on a moving contraption (aerial drooped); keeps its setting when moved or picked up with Carry On | defaults, round 3 |
| D25 | Breaking | Drops itself; store and setting lost (as the motor) | defaults, round 3 |
| D26 | Display Link | A source: the FE a tick it makes, like the Magma Engine's sources | defaults, round 3 |
| D27 | Recipe | Shaped `PEP / CMC / ASA`: P brass sheet, E electron tube, C copper ingot (windings), M precision mechanism, A andesite casing (the bots' material), S shaft | defaults, round 3 |
| D28 | Teaching | Ponder "Charging Up": a water wheel and a Clutch drive the dynamo; the wrench setting; an Electric Motor against its base at the same setting runs short (the loss); the Clutch switches it off; paper explained in text. Advancements "They Found Electricity" (make one) and "Round Trip" (run an Electric Motor off one) | defaults, round 3; scene as built and names approved by the user, 2026-10-31 |
| D29 | Sound | A quiet hum whose pitch follows its output, a soft spin-up and spin-down, and a tiny springy *boing* on the aerial's wobble, through the staged sound workflow | defaults, round 3 (aerial sound added with D16) |
| D30 | Lore text | No Wastes flavour text now; the Wastes branch adds it (D4) | defaults, round 3 |

## Open Questions

None. Parked for later: the Wastes tie-in (D4, D30), in the Wastes branch.

## Core Loop

1. Build a shaft source: water wheels, a windmill, a steam boiler, a Magma Engine.
2. Run its shaft into the dynamo's front; run a Power Line (or a machine) off its base.
3. Wrench it and set how much stress it takes. The read-out says what that powers.
4. The aerial pops up; lights, motors, cages and batteries on the line get power.
5. Grow the source and turn the dynamo up; or move a windmill farm far away and let it feed the base on paper.

Why players come back to it: it makes every Create power source an electricity source, so the electric half of the mod no longer depends on lava. It's the only way to turn free rotation into power you can carry down a cable.

## Behaviour / Rules

1. **Placed**: facing like the motor (`DirectionalKineticBlock`): the shaft socket faces back at the player, or away from a single shaft pointing at it. A wrench opens the screen; sneak-wrench picks it up.
2. **Each server tick**, if its shaft speed ≠ 0 and the network isn't overstressed and it isn't on a contraption: it adds `setting × motorFePerStress × dynamoEfficiency` FE to its store (fractions carried over); what doesn't fit is lost (D15). Then it pushes as much as it can out of its base.
3. **Stress**: `calculateStressApplied()` returns `setting / |speed|`, so the network sees exactly `setting` SU at any speed (D8). On a speed change the network updates as Create does for the Cuckoo Clock.
4. **Settings change**: the network's stress is updated once; the aerial wobbles.
5. **Running flag** (`working` blockstate or synced field): true while it makes power. Drives the aerial (up/down), the hum and Display Link.
6. **On paper** (D11, D12): when its chunk stops running, it records `output × freeShare` with the Power Lines record (store, output, at). While away, the network draws on that record like a Lava Generator's; back in a running chunk it takes on how far the record moved (deltas, once).
7. **Broken**: drops itself, nothing kept (D25).

## Progression

**Tier: mid to late, brass age.** It sits beside the Electric Motor and Lava Generator. It needs a precision mechanism and an electron tube (brass, sequenced assembly), so it arrives when the rest of electricity does.

- **Needs before it**: brass, a precision mechanism, a shaft source worth converting.
- **Unlocks**: electricity without lava; lights off a water wheel; a windmill farm far away powering the base through Power Lines; steam power on cables; storing shaft power in Batteries (and getting 75% back through a motor).

## Balance

**Exchange rates (default config)**

| SU taken | FE/t made | A motor at the same SU draws |
|---|---|---|
| 256 (one water wheel) | 12 | 16 |
| 512 (one large water wheel) | 24 | 32 |
| 8,192 (a full 128-sail windmill) | 384 | 512 |
| 16,384 | 768 | 1,024 (one Lava Generator) |
| 294,912 (a full superheated boiler) | 13,824 | 18,432 |

**Lava compared** (the dedicated generators must stay better on lava):

| Route | Lava | FE/t | FE/t per mB/s |
|---|---|---|---|
| Lava Generator | 20 mB/s | 1,024 | 51.2 |
| Geothermal Turbine | 90 mB/s | 46,080 | 512 |
| Magma Engine → dynamo | 10 mB/s | 384 | 38.4 |
| Supercharged Magma Engine → dynamo | 20 mB/s | 768 | 38.4 |

- **Cost**: one brass-tier recipe, the same weight as the motor. **Reward**: grows with whatever the player's shaft source gives. **Risk**: none (no explosions, no fuel of its own).
- **Automation**: nothing to feed, so fully automatic once built.
- **Exploits and fixes**:
  - *Motor ↔ dynamo loop*: 75% < 100%, so the loop always runs down. Fixed by D9.
  - *Paper loop* (a motor isn't live while unloaded, so a far motor + dynamo would be free power): only free sources count on paper. Fixed by D12.
  - *Paper fuel dodge* (a far Magma Engine or boiler would burn nothing): fuel sources count zero on paper. Fixed by D12.
  - *Overstress cheat* (set it huge on a tiny network): overstressed means speed 0, which means no power (D20).
  - *Contraption power*: makes nothing while moving (D24).
  - *Creative motor*: free and infinite, but creative-only. Accepted.
- **Overpowered?** A full boiler gives 13,824 FE/t, under a third of one turbine (46,080), with a lot more building. Free sources are small (a full windmill: 384 FE/t, about a third of a Lava Generator). It doesn't replace any existing generator.
- **Useless?** No. It's the only route from free rotation or steam to FE, and lights cost 0.5–32 FE/t, so even one water wheel (12 FE/t) is worth having.

## Create Integration

| System | Interaction | Notes |
|---|---|---|
| Shafts and gears | **Yes**: a stress consumer on its front face; takes its set SU at any speed | Precedent: Cuckoo Clock (`calculateStressApplied`) |
| Water wheels, windmills, creative motor | **Yes**: free sources, count on paper | `free_kinetic_sources` tag |
| Steam engines, Magma Engines, Spring Engine, Electric Motor | Drive it while loaded; count zero on paper | D12 |
| Clutch / Gearshift | **Yes**: the on/off switch (no redstone on the block) | Ponder shows it |
| Belts, depots, funnels, vaults, chutes | No | No items involved |
| Contraptions | Can be carried; makes nothing while moving; keeps its setting | D24 |
| Trains, elevators | No | Same as contraptions |
| Logistics, packages | No | No items involved |
| Deployers, mechanical arms | No | Nothing to insert; a deployer with a wrench doesn't open the screen |
| Redstone links, comparators | No | Clutch does on/off; no comparator output |
| Display Link | **Yes**: source for FE/t made | D26 |
| Engineer's Goggles | **Yes**: store, FE/t, SU and 75%, stress impact | D22 |
| Ponder | **Yes**: "Charging Up" | D28 |

## Technical Notes

- **Classes**: `ClockworkDynamoBlock extends DirectionalKineticBlock implements IBE` (wrench opens the screen, like `ElectricMotorBlock`); `ClockworkDynamoBlockEntity extends KineticBlockEntity` with an `EnergyBuffer(cap, 0, cap)` (extract only), `setting`, fractional `carry`, `working`.
- **Energy capability**: base face only (`isOutput(state, side)`), like `ElectricMotorBlock.isInput`. Pushes every tick through a `BlockCapabilityCache` on the base, as `LavaGeneratorBlockEntity` does.
- **Power Lines**: registers as a generator with `PowerGrid`; on chunk stop, records `output × freeShare` (D12). `freeShare` is the sum of tagged sources' capacity over the network's total, read from Create's `KineticNetwork` sources when the chunk stops.
- **Sync**: `setting` and `working` to the client (the aerial and goggles). The aerial's spring wobble is client-only: a spring-damper on one bone, kicked when `working` or `setting` changes (Trade Bot's aerial maths).
- **Rendering**: the shaft as the motor's (shared `cobble_generator_shaft` stub); the aerial as a Flywheel part (memory: Flywheel where possible), with a BER fallback.
- **Saving**: `StressSetting` (int), `Energy`, `Working`, `Carry` (not sent to clients). Load clamps the setting to multiples of 256 within 256…max (as the motor).
- **Network payload**: `DynamoSettingsPayload` (like `MotorSettingsPayload`): within 8 blocks, may build.
- **Config** (`CMConfig`, `electricity`): `dynamoEfficiency` (0.75, 0.0–1.0; the comment warns that 1.0 ties the Lava Generator on lava and removes the round-trip loss); max stress shares `motorMaxStress`.
- **Data**: block tag `comedymonkeysaddons:free_kinetic_sources`; recipe; loot table (drops itself); `mineable/pickaxe`; lang (name, tooltip, goggles, screen, Ponder, advancement, subtitles); advancement.
- **Compatibility**: id `clockwork_dynamo` (no "create"); has a BlockItem (Carry On); no Aeronautics physics concerns beyond D24.

# ONE MINUTE UNDERSTANDING

The Clockwork Dynamo is the Electric Motor in reverse. Put a spinning shaft in its front and a Power Line on its base. Wrench it to choose how much stress it takes, from 256 to 294,912 SU, and it turns 75% of that into electricity. A water wheel runs a few lights, and a big steam boiler runs a lot. Run that power back into a motor and you get three quarters of your rotation back, never more. It's built from the bots' parts, and Trade Bot's little copper aerial on top pops up while it's working and flops down when it isn't. Far away, it keeps running on paper, but only on free power such as water wheels and windmills.

# WHAT IT IS

A brass-tier block that takes a set amount of stress off a Create shaft and makes Forge Energy from it, at 75% of the Electric Motor's rate. It looks like it was built by the Trade and Helper Bots, with Trade Bot's aerial as its one bit of whimsy.

# WHY IT EXISTS

- Electricity shouldn't need lava: every Create power source can now feed the grid.
- It gives the motor a partner with an honest round-trip loss.
- It's the first bot-built machine, ready for the Clockwork Wastes backstory.

# HOW PLAYERS USE IT

- Shaft into the front, Power Line or machine on the base.
- Wrench it and set the stress it takes; the read-out shows the power.
- Watch the aerial: up means it's working.
- Use a Clutch to switch it off.
- Put windmills or water wheels far away; they power the base on paper.

# HOW IT FITS CREATE

- A normal stress consumer: it takes its setting at any speed.
- Overstressed or stopped, it makes nothing, as Create players expect.
- Clutches and Gearshifts are its controls.
- Goggles, Display Link and Ponder all cover it.
- Water wheels, windmills and steam all feed it.

# IMPORTANT RULES

- FE/t = SU × 0.0625 × 0.75 (768 FE/t at 16,384 SU).
- More power always takes more SU: 21⅓ SU for every FE/t.
- It takes its set SU at any speed; speed doesn't change output.
- Power goes out of the base only; the shaft goes in at the front only.
- Full store: keeps the stress, and the extra power is lost.
- Not turning, overstressed or moving: no power, aerial down.
- On paper: output × the free-source share; motors and fuel engines count 0.
- Starts at 256 SU; it goes up to the motor's max.
- Store: 2 seconds of max output; nothing can push into it.
- Broken: drops itself, store and setting lost.
- No redstone control and no comparator output.

# EDGE CASES

- **Motor and dynamo on one shaft**: the motor needs 1,024 FE/t but gets 768; it runs down and stops.
- **Mixed windmill and Magma Engine network**: on paper it gives only the windmill's share of its output.
- **Overstressed when its chunk unloads**: nothing on paper until it runs again.
- **Set above what the network has**: the network overstresses and the dynamo makes nothing (Create's normal rule).
- **Shaft runs into an unloaded chunk while the dynamo is loaded**: Create drops those sources and may overstress; it's honest.
- **Picked up mid-run (Carry On / contraption)**: the setting is kept, the store is lost on break only, and it makes nothing while moving.
- **Two dynamos on one network**: each takes its own setting.
- **Speed changes (sequenced gearshift)**: the stress per RPM is recomputed, and the output stays the same.
- **Nothing on the base**: the store fills and the rest is lost; the aerial stays up because it's working.
- **Config efficiency set to 1.0**: the loop breaks even and ties the Lava Generator on lava; the config comment warns about this.

# DEVELOPMENT CHECKLIST

- [x] `ClockworkDynamoBlock` (six facings, wrench opens the screen, sneak-wrench pickup, `isOutput` base face)
- [x] `ClockworkDynamoBlockEntity`: stress = setting ÷ |speed|, FE per tick with carry, store, base push, `working` flag, goggles
- [x] Registry: block, block entity, BlockItem, creative tab
- [x] Energy capability on the base only (extract-only buffer)
- [x] `PowerGrid` generator registration; paper record with the free-source share; `back()` deltas
- [x] Block tag `free_kinetic_sources` (water wheel, large water wheel, windmill bearing, creative motor)
- [x] `DynamoSettingsPayload` + `ClockworkDynamoScreen` (GuiKit style; stress steps; read-out)
- [x] Config `dynamoEfficiency` with a warning comment
- [x] Renderer: the shared shaft stub; the aerial (Flywheel part + BER fallback) with up/droop and a client spring wobble
- [x] Model and textures (bot family; brief via voxel-art-director; Blockbench preview first)
- [x] Sounds: hum following output, spin-up/down, aerial boing (staged sound workflow), subtitles
- [x] Display Link source (FE/t)
- [x] Recipe `PEP / CMC / ASA`; loot table; `mineable/pickaxe`
- [x] Lang: name, tooltip, goggles, screen, Ponder, advancement, subtitles
- [x] Ponder "Charging Up" (a water wheel drives it; the loss beside a motor; a Clutch as its off switch; paper)
- [x] Advancement: run an Electric Motor off a Clockwork Dynamo
- [x] Unit tests: FE per tick at each step; stress at any speed; setting clamp on load; free share maths
- [x] Game tests (`gametest/DynamoGameTests`): power off a shaft, base-only output, powering a motor, overstressed makes 0, a free source counts in full on paper, a motor-driven dynamo counts nothing (proved to fail with the check broken). Not covered: on a moving contraption
- [ ] Server test: far windmill on paper gives power; far motor + dynamo gives nothing
- [x] Website Codex entry, render, recipe, 2.5.0 What's new section, Electricity page row and README (2.5.0)

# "IDIOT TEST"

- The motor in reverse: rotation in, electricity out.
- Shaft in the front, power out the base.
- Wrench it to set how much stress it takes.
- 75% comes out as power. Always loses some.
- More power takes more SU, always in step.
- Not turning or overstressed: no power.
- The aerial up means it's working.
- Far away, only free power (water wheels, windmills) counts.
- Use a Clutch to turn it off.
- Bot parts, no face, one wobbly aerial.

# COMPLETENESS SCORE

Gameplay: 95% · World Integration: 90% · Balance: 90% · Progression: 95% · Automation: 95% · Edge Cases: 90% · **Overall Design Readiness: 90%**

- Gameplay 95%: the aerial's exact wobble timing is the art director's to set.
- World Integration 90%: the Wastes tie-in is parked for that branch (D4).
- Balance 90%: 75% is untested in play; the free-source share should be checked on a real windmill farm.
- Progression 95%: the recipe hasn't been tried against JEI conflicts.
- Automation 95%: nothing to feed; the Clutch control is shown in Ponder only.
- Edge Cases 90%: Create's handling of a shaft split across chunk edges is assumed, not tested.
