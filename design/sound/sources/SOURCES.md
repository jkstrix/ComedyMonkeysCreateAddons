# Sound sources

The pack recordings used for Trade Bot's real sounds are **CC0 (public domain)**: free to use, modify and ship, no attribution required (credit to Kenney is appreciated, not mandatory). His clock tick and key rattle are local Stable Audio 3 generations (Stability AI Community License; prompt, seed and processing in `generated/SOURCES.md`).

| Pack | By | Licence | URL |
|---|---|---|---|
| Impact Sounds (`kenney-impact-sounds/`) | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/impact-sounds |
| RPG Audio (`kenney-rpg-audio/`) | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/rpg-audio |
| Interface Sounds (`kenney-interface-sounds/`) | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/interface-sounds |
| Casino Audio (`kenney-casino-audio/`) | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/casino-audio |
| UI Audio (`kenney-ui-audio/`) | Kenney (kenney.nl) | CC0 1.0 | https://kenney.nl/assets/ui-audio |

Voice chirps, springs (boings), servos, swells, pops and hums are synthesised by the skill's `placeholder-synth.js` (no third-party material).

Never add Minecraft's or other mods' sound files here.

## Recordings used per event

| Event | Variants | Recordings | Synth layers |
|---|---|---|---|
| `clock_tick` | 4 | generated: sa3_tradebot_tick_1.wav to sa3_tradebot_tick_4.wav (Stable Audio 3, see generated/SOURCES.md) | — |
| `focus_click` | 3 | ui-audio: switch12.ogg, ui-audio: switch13.ogg, ui-audio: switch14.ogg, interface-sounds: tick_004.ogg | servo |
| `brass_ping` | 2 | impact-sounds: impactBell_heavy_003.ogg, impact-sounds: impactMetal_light_002.ogg | ping |
| `key_rattle` | 4 | generated: sa3_tradebot_wind_1.wav to sa3_tradebot_wind_4.wav (Stable Audio 3, see generated/SOURCES.md) | — |
| `merchant_trill` | 3 | interface-sounds: pluck_001.ogg | voice, ping |
| `roll_start` | 3 | impact-sounds: impactSoft_medium_000.ogg, impact-sounds: impactSoft_medium_001.ogg, impact-sounds: impactSoft_medium_002.ogg, impact-sounds: impactSoft_medium_003.ogg, impact-sounds: impactSoft_medium_004.ogg | roll, servo |
| `roll_stop` | 3 | impact-sounds: impactSoft_medium_000.ogg, impact-sounds: impactSoft_medium_001.ogg, impact-sounds: impactSoft_medium_002.ogg, impact-sounds: impactSoft_medium_003.ogg, impact-sounds: impactSoft_medium_004.ogg | roll, scrub |
| `servo_short` | 4 | ui-audio: click5.ogg, ui-audio: click4.ogg, ui-audio: click2.ogg | servo |
| `servo_snap` | 3 | ui-audio: switch12.ogg, ui-audio: switch13.ogg, ui-audio: switch14.ogg | servo |
| `hop_launch` | 3 | impact-sounds: impactSoft_heavy_000.ogg, impact-sounds: impactSoft_heavy_001.ogg, impact-sounds: impactSoft_heavy_002.ogg, impact-sounds: impactSoft_heavy_003.ogg, impact-sounds: impactSoft_heavy_004.ogg | thud, servo, boing, voice |
| `hop_launch_big` | 3 | impact-sounds: impactSoft_heavy_000.ogg, impact-sounds: impactSoft_heavy_001.ogg, impact-sounds: impactSoft_heavy_002.ogg, impact-sounds: impactSoft_heavy_003.ogg, impact-sounds: impactSoft_heavy_004.ogg | thud, servo, voice, boing |
| `land_soft` | 4 | impact-sounds: impactSoft_heavy_000.ogg, impact-sounds: impactSoft_heavy_001.ogg, impact-sounds: impactSoft_heavy_002.ogg, impact-sounds: impactSoft_heavy_003.ogg, impact-sounds: impactSoft_heavy_004.ogg, impact-sounds: impactPlate_heavy_000.ogg, impact-sounds: impactPlate_heavy_001.ogg, impact-sounds: impactPlate_heavy_002.ogg, impact-sounds: impactPlate_heavy_003.ogg, impact-sounds: impactPlate_heavy_004.ogg, impact-sounds: impactMetal_light_000.ogg, impact-sounds: impactMetal_light_001.ogg, impact-sounds: impactMetal_light_002.ogg, impact-sounds: impactMetal_light_003.ogg, impact-sounds: impactMetal_light_004.ogg | thud, boing |
| `land_heavy` | 3 | impact-sounds: impactPunch_heavy_000.ogg, impact-sounds: impactPunch_heavy_001.ogg, impact-sounds: impactPunch_heavy_002.ogg, impact-sounds: impactPunch_heavy_003.ogg, impact-sounds: impactPunch_heavy_004.ogg, impact-sounds: impactPlate_heavy_000.ogg, impact-sounds: impactPlate_heavy_001.ogg, impact-sounds: impactPlate_heavy_002.ogg, impact-sounds: impactPlate_heavy_003.ogg, impact-sounds: impactPlate_heavy_004.ogg, impact-sounds: impactMetal_heavy_000.ogg, impact-sounds: impactMetal_heavy_001.ogg, impact-sounds: impactMetal_heavy_002.ogg, impact-sounds: impactMetal_heavy_003.ogg, impact-sounds: impactMetal_heavy_004.ogg, casino-audio: dice-throw-2.ogg, casino-audio: die-throw-1.ogg, casino-audio: die-throw-4.ogg, impact-sounds: impactMetal_light_000.ogg, impact-sounds: impactMetal_light_001.ogg, impact-sounds: impactMetal_light_002.ogg, impact-sounds: impactMetal_light_003.ogg, impact-sounds: impactMetal_light_004.ogg | thud, boing |
| `drop_whoosh` | 3 | — | whoosh, voice |
| `aerial_boing` | 3 | impact-sounds: impactMetal_light_000.ogg, impact-sounds: impactMetal_light_001.ogg, impact-sounds: impactMetal_light_002.ogg, impact-sounds: impactMetal_light_003.ogg, impact-sounds: impactMetal_light_004.ogg | boing |
| `blink` | 2 | interface-sounds: click_003.ogg, interface-sounds: click_004.ogg, interface-sounds: click_005.ogg, interface-sounds: toggle_004.ogg | — |
| `bonk` | 2 | impact-sounds: impactBell_heavy_002.ogg, impact-sounds: impactMetal_heavy_001.ogg, impact-sounds: impactMetal_heavy_003.ogg, impact-sounds: impactPunch_medium_000.ogg | boing |
| `dizzy` | 2 | interface-sounds: scroll_002.ogg | voice |
| `head_shake` | 2 | ui-audio: switch12.ogg, ui-audio: switch13.ogg, ui-audio: switch14.ogg | servo |
| `grumble` | 3 | impact-sounds: impactSoft_medium_000.ogg, impact-sounds: impactSoft_medium_001.ogg, impact-sounds: impactSoft_medium_002.ogg, impact-sounds: impactSoft_medium_003.ogg, impact-sounds: impactSoft_medium_004.ogg | voice |
| `double_take` | 2 | ui-audio: switch14.ogg, rpg-audio: metalLatch.ogg | boing, voice |
| `wave_clink` | 4 | impact-sounds: impactMetal_light_000.ogg, impact-sounds: impactMetal_light_001.ogg, impact-sounds: impactMetal_light_002.ogg, impact-sounds: impactMetal_light_003.ogg, impact-sounds: impactMetal_light_004.ogg | ping, servo |
| `curious_chirp` | 3 | interface-sounds: tick_004.ogg | voice |
| `sad_aww` | 2 | rpg-audio: creak3.ogg | voice |
| `sigh` | 2 | rpg-audio: cloth3.ogg | servo |
| `snore` | 3 | rpg-audio: cloth4.ogg | voice |
| `startled_chirp` | 3 | ui-audio: switch14.ogg | voice, boing |
| `puzzled_hmm` | 3 | interface-sounds: tick_004.ogg | voice |
| `huh` | 3 | — | voice |
| `shrug` | 2 | impact-sounds: impactMetal_light_000.ogg, impact-sounds: impactMetal_light_001.ogg, impact-sounds: impactMetal_light_002.ogg, impact-sounds: impactMetal_light_003.ogg, impact-sounds: impactMetal_light_004.ogg | voice |
| `pleased_boop` | 3 | interface-sounds: pluck_001.ogg, interface-sounds: pluck_002.ogg | voice |
| `hi_boop` | 3 | interface-sounds: pluck_002.ogg | voice |
| `bye_boop` | 3 | interface-sounds: pluck_001.ogg | voice |
| `found_it` | 2 | impact-sounds: impactBell_heavy_004.ogg, interface-sounds: glass_002.ogg, interface-sounds: glass_003.ogg, interface-sounds: glass_005.ogg, interface-sounds: glass_006.ogg | voice |
| `hop_small` | 3 | impact-sounds: impactSoft_medium_000.ogg, impact-sounds: impactSoft_medium_001.ogg, impact-sounds: impactSoft_medium_002.ogg, impact-sounds: impactSoft_medium_003.ogg, impact-sounds: impactSoft_medium_004.ogg | boing |
| `light_off` | 2 | ui-audio: switch2.ogg, ui-audio: switch3.ogg, ui-audio: switch8.ogg | voice |
| `light_on` | 2 | ui-audio: switch5.ogg, ui-audio: switch6.ogg, ui-audio: switch9.ogg | voice |
| `flicker` | 2 | ui-audio: switch12.ogg, ui-audio: switch13.ogg, ui-audio: switch14.ogg | — |
| `sleepy_mmm` | 3 | rpg-audio: cloth4.ogg | voice |
| `glow_on` | 1 | interface-sounds: glass_004.ogg | swell, hum |
| `glow_off` | 1 | — | swell |
| `winder_engage` | 2 | rpg-audio: metalLatch.ogg, rpg-audio: metalClick.ogg, impact-sounds: impactMetal_medium_000.ogg, impact-sounds: impactMetal_medium_001.ogg, impact-sounds: impactMetal_medium_002.ogg, impact-sounds: impactMetal_medium_003.ogg, impact-sounds: impactMetal_medium_004.ogg | — |
| `winder_release` | 2 | rpg-audio: metalClick.ogg, impact-sounds: impactTin_medium_000.ogg, impact-sounds: impactTin_medium_001.ogg, impact-sounds: impactTin_medium_002.ogg, impact-sounds: impactTin_medium_003.ogg, impact-sounds: impactTin_medium_004.ogg | servo |
| `flag_ding` | 1 | impact-sounds: impactBell_heavy_003.ogg | boing |
| `drawer_open` | 3 | rpg-audio: handleCoins2.ogg, impact-sounds: impactWood_light_000.ogg, impact-sounds: impactWood_light_001.ogg, impact-sounds: impactWood_light_002.ogg, impact-sounds: impactWood_light_003.ogg, impact-sounds: impactWood_light_004.ogg | servo |
| `drawer_close` | 3 | impact-sounds: impactWood_medium_000.ogg, impact-sounds: impactWood_medium_001.ogg, impact-sounds: impactWood_medium_002.ogg, impact-sounds: impactWood_medium_003.ogg, impact-sounds: impactWood_medium_004.ogg, rpg-audio: handleCoins2.ogg | — |
| `chest_open` | 2 | rpg-audio: doorOpen_1.ogg, impact-sounds: impactWood_light_000.ogg, impact-sounds: impactWood_light_001.ogg, impact-sounds: impactWood_light_002.ogg, impact-sounds: impactWood_light_003.ogg, impact-sounds: impactWood_light_004.ogg | — |
| `chest_close` | 2 | rpg-audio: doorClose_1.ogg, rpg-audio: doorClose_2.ogg, rpg-audio: doorClose_3.ogg, impact-sounds: impactWood_heavy_000.ogg, impact-sounds: impactWood_heavy_001.ogg, impact-sounds: impactWood_heavy_002.ogg, impact-sounds: impactWood_heavy_003.ogg, impact-sounds: impactWood_heavy_004.ogg | — |
| `barrel_open` | 2 | impact-sounds: impactWood_light_000.ogg, impact-sounds: impactWood_light_001.ogg, impact-sounds: impactWood_light_002.ogg, impact-sounds: impactWood_light_003.ogg, impact-sounds: impactWood_light_004.ogg, rpg-audio: creak3.ogg | — |
| `barrel_close` | 2 | impact-sounds: impactWood_medium_000.ogg, impact-sounds: impactWood_medium_001.ogg, impact-sounds: impactWood_medium_002.ogg, impact-sounds: impactWood_medium_003.ogg, impact-sounds: impactWood_medium_004.ogg, impact-sounds: impactWood_heavy_000.ogg, impact-sounds: impactWood_heavy_001.ogg, impact-sounds: impactWood_heavy_002.ogg, impact-sounds: impactWood_heavy_003.ogg, impact-sounds: impactWood_heavy_004.ogg | — |
| `rummage` | 2 | rpg-audio: cloth1.ogg, rpg-audio: cloth2.ogg, rpg-audio: cloth3.ogg, rpg-audio: handleCoins.ogg, rpg-audio: bookFlip1.ogg, impact-sounds: impactWood_light_000.ogg, impact-sounds: impactWood_light_001.ogg, impact-sounds: impactWood_light_002.ogg, impact-sounds: impactWood_light_003.ogg, impact-sounds: impactWood_light_004.ogg | — |
| `toss_whoosh` | 3 | — | whoosh |
| `cobble_clack` | 2 | impact-sounds: impactMining_000.ogg, impact-sounds: impactMining_001.ogg, impact-sounds: impactMining_002.ogg, impact-sounds: impactMining_003.ogg, impact-sounds: impactMining_004.ogg, casino-audio: die-throw-2.ogg, casino-audio: dice-throw-2.ogg | — |
| `set_on_metal` | 3 | rpg-audio: bookPlace1.ogg, rpg-audio: bookPlace2.ogg, rpg-audio: bookPlace3.ogg, impact-sounds: impactPlate_light_000.ogg, impact-sounds: impactPlate_light_001.ogg, impact-sounds: impactPlate_light_002.ogg, impact-sounds: impactPlate_light_003.ogg, impact-sounds: impactPlate_light_004.ogg | — |
| `set_soft` | 3 | rpg-audio: bookPlace1.ogg, rpg-audio: bookPlace2.ogg, rpg-audio: bookPlace3.ogg, impact-sounds: impactSoft_medium_000.ogg, impact-sounds: impactSoft_medium_001.ogg, impact-sounds: impactSoft_medium_002.ogg, impact-sounds: impactSoft_medium_003.ogg, impact-sounds: impactSoft_medium_004.ogg | — |
| `pat` | 3 | impact-sounds: impactSoft_medium_000.ogg, impact-sounds: impactSoft_medium_001.ogg, impact-sounds: impactSoft_medium_002.ogg, impact-sounds: impactSoft_medium_003.ogg, impact-sounds: impactSoft_medium_004.ogg | — |
| `dust_off` | 2 | rpg-audio: cloth2.ogg, impact-sounds: impactMetal_light_000.ogg, impact-sounds: impactMetal_light_001.ogg, impact-sounds: impactMetal_light_002.ogg, impact-sounds: impactMetal_light_003.ogg, impact-sounds: impactMetal_light_004.ogg, rpg-audio: cloth3.ogg | — |
| `push_scrape` | 3 | impact-sounds: impactWood_light_000.ogg, impact-sounds: impactWood_light_001.ogg, impact-sounds: impactWood_light_002.ogg, impact-sounds: impactWood_light_003.ogg, impact-sounds: impactWood_light_004.ogg | scrape |
| `grip` | 3 | impact-sounds: impactTin_medium_000.ogg, impact-sounds: impactTin_medium_001.ogg, impact-sounds: impactTin_medium_002.ogg, impact-sounds: impactTin_medium_003.ogg, impact-sounds: impactTin_medium_004.ogg | ping |
| `claw_fidget` | 2 | ui-audio: switch12.ogg, ui-audio: switch13.ogg, ui-audio: switch14.ogg | — |
| `poof_in` | 3 | interface-sounds: glass_002.ogg, interface-sounds: glass_003.ogg, interface-sounds: glass_005.ogg, interface-sounds: glass_006.ogg | pop |
| `poof_out` | 3 | interface-sounds: glass_002.ogg, interface-sounds: glass_003.ogg, interface-sounds: glass_005.ogg, interface-sounds: glass_006.ogg | swell, pop |
| `haggle_chatter` | 3 | interface-sounds: tick_004.ogg | voice |
| `plead_chirp` | 2 | — | voice |
| `emerald_catch` | 3 | rpg-audio: handleCoins2.ogg, impact-sounds: impactBell_heavy_004.ogg, impact-sounds: impactGlass_light_000.ogg, impact-sounds: impactGlass_light_001.ogg, impact-sounds: impactGlass_light_002.ogg, impact-sounds: impactGlass_light_003.ogg, impact-sounds: impactGlass_light_004.ogg | ping |
| `spin_whir` | 2 | — | servo, whoosh |
| `villager_no` | 1 | — | voice, noise |
| `villager_yes` | 1 | — | voice, noise |
| `villager_trade` | 1 | — | voice, noise |
| `roll_loop` | 2 | — | roll |
| `scared_yelp` | 3 | ui-audio: switch14.ogg, rpg-audio: metalLatch.ogg | voice, boing |
| `panic_chatter` | 4 | interface-sounds: tick_004.ogg | voice |
| `roll_fast_loop` | 2 | — | roll |
| `hard_brake` | 3 | impact-sounds: impactSoft_medium_000.ogg, impact-sounds: impactSoft_medium_001.ogg, impact-sounds: impactSoft_medium_002.ogg, impact-sounds: impactSoft_medium_003.ogg, impact-sounds: impactSoft_medium_004.ogg | roll, scrub |
| `hide_tuck` | 2 | impact-sounds: impactMetal_light_000.ogg, impact-sounds: impactMetal_light_001.ogg, impact-sounds: impactMetal_light_002.ogg, impact-sounds: impactMetal_light_003.ogg, impact-sounds: impactMetal_light_004.ogg, impact-sounds: impactSoft_medium_000.ogg, impact-sounds: impactSoft_medium_001.ogg, impact-sounds: impactSoft_medium_002.ogg, impact-sounds: impactSoft_medium_003.ogg, impact-sounds: impactSoft_medium_004.ogg | servo |
| `tremble_loop` | 2 | impact-sounds: impactPlate_light_000.ogg, impact-sounds: impactMetal_light_003.ogg, impact-sounds: impactTin_medium_003.ogg, impact-sounds: impactMetal_light_004.ogg, impact-sounds: impactTin_medium_000.ogg, impact-sounds: impactPlate_light_004.ogg, impact-sounds: impactPlate_light_001.ogg, impact-sounds: impactTin_medium_001.ogg, impact-sounds: impactTin_medium_004.ogg, impact-sounds: impactMetal_light_000.ogg, impact-sounds: impactMetal_light_001.ogg, impact-sounds: impactMetal_light_002.ogg, impact-sounds: impactPlate_light_002.ogg | scrub |
| `whimper` | 3 | — | voice |
| `peek_click` | 2 | interface-sounds: tick_004.ogg | voice |
| `relief_sigh` | 2 | rpg-audio: cloth3.ogg | servo, voice |
| `shake_off` | 3 | casino-audio: dice-shake-2.ogg, casino-audio: dice-shake-3.ogg, impact-sounds: impactMetal_light_000.ogg, impact-sounds: impactMetal_light_001.ogg, impact-sounds: impactMetal_light_002.ogg, impact-sounds: impactMetal_light_003.ogg, impact-sounds: impactMetal_light_004.ogg | boing |


## Monster Cage and Fish Tank (2026-10-27)

Real-first ladder (`character-sound-designer` skill, Stage 2). **Rung 1, Minecraft's own** (referenced in game, never shipped; previews are copied from the local asset cache into git-ignored files only to audition them): the stored monster's ambient, the sweep, trident throw, drowned conversion, Wither spawn, eating, a monster's ambient for the belch, bubble pop, fish swim, bucket fill fish. **Rung 2, the project library**: Kenney Impact Sounds (`impactMetal_light`, `impactTin_medium`) and Interface Sounds (`glass_00x`). **Rung 3, new CC0 recordings**: BigSoundBank (all CC0, https://bigsoundbank.com), below; the segments used are cut by `design/sound/monster-cage/cuts.js` into `bigsoundbank/cuts/`. **Rung 4 (synth or AI): none used.**

Not found on BigSoundBank, so made from the nearest real recording: kibble into a steel bowl (rice into a metal saucepan, cereal into a bowl, plus a Kenney metal ring); an aquarium air pump (a small air compressor and an electric pump, pitched up and low-passed); an air stone (a fermentation airlock); the containment field (transformer and neon hum).

| File | Title | URL | Cuts used |
|---|---|---|---|
| 0026.ogg | electric-switch | https://bigsoundbank.com/electric-switch-s0026.html | switch_click (0.2-0.45 s) |
| 0134.ogg | electric-shock | https://bigsoundbank.com/electric-shock-s0134.html | spark_shock_a (0.8-1.1 s), spark_shock_b (1.6-1.85 s), spark_shock_c (2.2-2.45 s) |
| 0201.ogg | raw-rice-poured-into-a-saucepan | https://bigsoundbank.com/raw-rice-poured-into-a-saucepan-s0201.html | kibble_rice_pan (1.0-1.9 s) |
| 0235.ogg | iron-bar-falls-2 | https://bigsoundbank.com/iron-bar-falls-2-s0235.html | bars_iron_clang (0-1.0 s) |
| 0303.ogg | grinding-metal-gate-1 | https://bigsoundbank.com/grinding-metal-gate-1-s0303.html | bars_gate (0-1.2 s) |
| 0378.ogg | lock-and-big-key | https://bigsoundbank.com/lock-and-big-key-s0378.html | lock_big_key (1.0-1.5 s) |
| 0407.ogg | eat-a-rusk | https://bigsoundbank.com/eat-a-rusk-s0407.html | crunch_rusk (0.1-0.7 s) |
| 0596.ogg | bones-cracking | https://bigsoundbank.com/bones-cracking-s0596.html | crunch_bones (2.2-2.8 s) |
| 0686.ogg | yoghurt-pot | https://bigsoundbank.com/yoghurt-pot-s0686.html | lid_pot_a (0.2-0.6 s), lid_pot_b (0.6-1.0 s) |
| 0709.ogg | electric-pump | https://bigsoundbank.com/electric-pump-s0709.html | loop_electric_pump (1-5 s) |
| 0747.ogg | blow-on-hand | https://bigsoundbank.com/blow-on-hand-s0747.html | puff_blow_b (0.2-0.8 s) |
| 0748.ogg | blow-on-piece | https://bigsoundbank.com/blow-on-piece-s0748.html | puff_blow_a (0.1-0.7 s) |
| 0856.ogg | small-air-compressor | https://bigsoundbank.com/small-air-compressor-s0856.html | loop_air_compressor (5-9 s) |
| 0952.ogg | short-welds | https://bigsoundbank.com/short-welds-s0952.html | spark_weld (2.7-3.1 s) |
| 0995.ogg | wooden-stick-on-bars | https://bigsoundbank.com/wooden-stick-on-bars-s0995.html | bars_stick (3.1-3.9 s) |
| 1252.ogg | electrical-fence | https://bigsoundbank.com/electrical-fence-s1252.html | loop_fence (0.5-6.5 s) |
| 1432.ogg | eating-potato-chips-2 | https://bigsoundbank.com/eating-potato-chips-2-s1432.html | crunch_crisps (1.5-1.9 s) |
| 1527.ogg | metal-mailbox-1 | https://bigsoundbank.com/metal-mailbox-1-s1527.html | hatch_mailbox_shut (0.2-0.8 s) |
| 1528.ogg | metal-mailbox-2 | https://bigsoundbank.com/metal-mailbox-2-s1528.html | hatch_mailbox_open (0.1-0.6 s) |
| 1539.ogg | barn-lock-unlocking | https://bigsoundbank.com/barn-lock-unlocking-s1539.html | lock_barn_unlock (0.1-0.8 s) |
| 1540.ogg | barn-lock-locking | https://bigsoundbank.com/barn-lock-locking-s1540.html | lock_barn_lock (0.5-1.0 s) |
| 1771.ogg | falling-sheet-steel-bar-1 | https://bigsoundbank.com/falling-sheet-steel-bar-1-s1771.html | crash_steel_b (0-0.8 s) |
| 1776.ogg | fall-2-sheet-steel-bars-1 | https://bigsoundbank.com/fall-2-sheet-steel-bars-1-s1776.html | crash_steel_a (0-1.5 s) |
| 2071.ogg | cereal-in-bowl-1 | https://bigsoundbank.com/cereal-in-bowl-1-s2071.html | kibble_cereal_b (0.9-1.8 s) |
| 2072.ogg | cereal-in-bowl-2 | https://bigsoundbank.com/cereal-in-bowl-2-s2072.html | kibble_cereal_a (0.4-1.3 s) |
| 2073.ogg | cereal-in-bowl-3 | https://bigsoundbank.com/cereal-in-bowl-3-s2073.html | flakes_cereal (0.2-0.8 s) |
| 2118.ogg | neon-or-transformer-1 | https://bigsoundbank.com/neon-or-transformer-1-s2118.html | loop_neon (2-6 s) |
| 2143.ogg | flashing-light-bulb-1 | https://bigsoundbank.com/flashing-light-bulb-1-s2143.html | flicker_bulb_a (0-0.4 s) |
| 2144.ogg | flashing-light-bulb-2 | https://bigsoundbank.com/flashing-light-bulb-2-s2144.html | flicker_bulb_b (0-0.8 s) |
| 2437.ogg | electric-transformer-4 | https://bigsoundbank.com/electric-transformer-4-s2437.html | loop_transformer (2-6 s) |
| 2541.ogg | oogah-horn-2 | https://bigsoundbank.com/oogah-horn-2-s2541.html | klaxon_oogah_a (0-1.15 s) |
| 2542.ogg | oogah-horn-3 | https://bigsoundbank.com/oogah-horn-3-s2542.html | klaxon_oogah_b (0-1.41 s) |
| 2543.ogg | oogah-horn-4 | https://bigsoundbank.com/oogah-horn-4-s2543.html | klaxon_oogah_c (0-0.76 s) |
| 3442.ogg | electric-strike-1 | https://bigsoundbank.com/electric-strike-1-s3442.html | strike_clunk (0-0.6 s) |
| 3560.ogg | fermentation-airlock-1 | https://bigsoundbank.com/fermentation-airlock-1-s3560.html | loop_airlock_a (10-16 s) |
| 3561.ogg | fermentation-airlock-2 | https://bigsoundbank.com/fermentation-airlock-2-s3561.html | loop_airlock_b (10-16 s) |

## Helper Bot (2026-10-27)
Helper Bot reuses Trade Bot's approved sounds (see above). New layers, for his whistle and lever only:

| File | What | Source | Licence | Rung |
|---|---|---|---|---|
| `bigsoundbank/3319.ogg` | Train whistle, foley #5: whistle candidate A (pitched up 1.6x) | https://bigsoundbank.com/train-whistle-foley-5-s3319.html | CC0 | 3 |
| `bigsoundbank/3317.ogg` | Train whistle, foley #3: whistle candidate B (up 1.5x) | https://bigsoundbank.com/train-whistle-foley-3-s3317.html | CC0 | 3 |
| `bigsoundbank/3315.ogg` | Train whistle, foley #1: whistle candidate C (up 3x) | https://bigsoundbank.com/train-whistle-foley-1-s3315.html | CC0 | 3 |
| `bigsoundbank/0225.ogg` | Whistling train #1: whistle candidate D (up 2.2x) | https://bigsoundbank.com/whistling-train-1-s0225.html | CC0 | 3 |
| `bigsoundbank/0227.ogg` | Hiss of steam train #1: the puff under every toot | https://bigsoundbank.com/hiss-of-steam-train-1-s0227.html | CC0 | 3 |
| `vanilla-preview/random_click.ogg`, `block_copper_trapdoor_toggle1/2.ogg` | Lever candidates A and B: Minecraft's own (`block.lever.click`, `block.copper_trapdoor.open`), extracted for auditioning only, git-ignored, referenced by event in game | Minecraft 1.21.1 assets | Mojang (not shipped) | 1 |
| `kenney-rpg-audio/Audio/metalLatch.ogg`, `kenney-impact-sounds/.../impactSoft_heavy_000.ogg` | Lever candidate C | already in the library | CC0 | 2 |

Searched and not used: BigSoundBank 3316, 3318, 3320 (the same foley whistle, other takes), 3321 (too low), 0226 (a long locomotive whistle), 0196 (wooden whistle, too pure) and 1351 (kettle).
