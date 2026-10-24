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
