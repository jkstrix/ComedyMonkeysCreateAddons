# Generated sources

Sound effects generated locally with **Stable Audio 3 medium** (Stability AI) through the `audio-studio` MCP
(https://github.com/kanaworksai-hue/audio-studio-mcp), steps 8, 44.1 kHz. Prompts include the server's `TrackType: SFX, `
prefix. The model weights are under the **Stability AI Community License** (commercial use allowed under US$1M annual
revenue; this mod is free). The outputs contain no third-party recordings.

Each file here is the picked take after the processing listed, mono 16-bit 44.1 kHz. A take can be regenerated from its
prompt, seed and cfg.

**Cleaned** = `highpass=f=40,afftdn=nr=18:nf=-42:tn=1,lowpass=f=8000` (ffmpeg: noise reduction, hiss above 8 kHz rolled off).
**Compressed** = `acompressor=threshold=0.03:ratio=10:attack=0.2:release=40:knee=4,alimiter=limit=0.5:attack=0.5:release=20`
(tames the crackle and tick peaks so the loop can sit at a steady quiet level).

| File | Used by | Seed | cfg | Processing | Prompt |
|---|---|---|---|---|---|
| `sa3_crucible_crunch_v3_clean.wav` | Pressure Crucible `crunch` | 1460098336 | 1 | cleaned | a heavy cast-iron screw press platen, slamming down onto a pile of mineral dust and crushing it into a gem, sharp heavy impact then a short gritty crumble that dies away quickly, cast iron on stone, deep low thud, crunchy gravel grit, no ringing, indoor workshop, close-up, dry. (1.5 s) |
| `sa3_crucible_gears_v1_x2.wav` | Pressure Crucible `whirr` | 1905876739 | 1 | `atempo=2` (double speed, same pitch), compressed | old cast-iron spur gears and a pinion in a hand-built press, turning slowly and steadily, teeth meshing with a soft rhythmic clicking and low grinding hum, constant and loopable, heavy oiled iron, muted, low and warm, no squeal, indoor workshop, close-up, dry. (10 s) |
| `sa3_crucible_fire_v2_clean.wav` | Pressure Crucible `fire` | 2044564465 | 1 | cleaned, compressed | a small furnace fire under an iron bowl, burning steadily with a soft warm roar and gentle occasional crackles, constant and loopable, coal and embers, low and warm, no hiss, indoor, close-up, dry. (10 s) |
| `sa3_crucible_soul_fire_v3_clean.wav` | Pressure Crucible `fire_soul` | 1236256779 | 1 | cleaned | an eerie magical blue soul fire burning in a furnace, burning steadily with a cool airy whooshing flame and soft spectral shimmer, few crackles, constant and loopable, gas-like blue flame, smooth, slightly hollow, gentle, indoor, close-up, dry. (10 s) |
| `sa3_lava_generator_start_v2.wav` | Lava Generator and Geothermal Turbine `spin_up` (the start), and its tail (2.4–4.55 s) looped as their `whir` and `turbine_whine` | 1670307420 | 1 | none (made by the user in the audio-studio web UI, 5 s, material "Steel") | An electricity generator starting up, fantasy, powered by boiling lava, whirring, coming to life, high quality |
| `sa3_generator_stop_v1.wav` | Lava Generator and Geothermal Turbine `spin_down` (powering down) | 972519014 | 1 | none (made by the user in the audio-studio web UI, 5 s, material "Steel"); faded out over its last 1.6 s in the recipe | An electricity generator spinning down, from a quiet hum, fantasy, powered by boiling lava, whirring, coming to life, high quality |
| `sa3_tradebot_tick_1.wav` to `_4.wav` | Trade Bot `clock_tick` (one per variant) | 1154936479 | 1 | the four strong ticks of the take (at 0.03, 1.13, 2.24 and 3.36 s), 0.2 s each from just before the hit, faded out over the last 60 ms; made by the user in the audio-studio web UI, material "Steel" | Single tick from a clock |
| `sa3_tradebot_wind_1.wav` to `_4.wav` | Trade Bot `key_rattle` (one per variant) | 1221440971 | 1 | the four winding bursts of the take (0.08–0.80, 0.98–1.76, 2.02–2.78 and 3.00–3.74 s), with 5 ms fade-ins and 50 ms fade-outs; made by the user in the audio-studio web UI, material "Steel" | Clock work automiton winding up, slow, deliverate consistant, two clicks evenly spaces for loops |
| `sa3_battery_hum_v3_loop.wav` | Industrial Battery `hum`, and the swell in `form` and `unform` | 1248207499 | 1.0 | cleaned, 1.0-9.5 s, looped with a 1.5 s crossfade (7 s) | large electrical substation power transformer, steady continuous low mains hum, constant, no changes, no clicks, deep 100 hertz electrical buzz drone with soft harmonics, smooth and calm, outdoors, a few metres away, dry, quiet background. |
| `sa3_battery_contactor_close_v2_clean.wav` | Industrial Battery `form` | 1571750054 | 1.0 | cleaned | heavy industrial electrical contactor switch closing, single solid clunk as the contacts slam shut, short metallic ring decaying quickly, then silence, thick steel housing, dull heavy thunk, no rattle, indoor switch room, close, slightly reverberant. |
| `sa3_battery_contactor_open_v2_clean.wav` | Industrial Battery `unform` | 916708134 | 1.0 | cleaned | electrical contactor switch opening, single light clack as the contacts spring open, very short, then silence, steel, crisp but muted, no rattle, indoor switch room, close, slightly reverberant. |
| `sa3_battery_arc_v2_clean.wav` | Industrial Battery `strike` (take 1) | 282419363 | 1.0 | cleaned; the recipe keeps only the first crack (0-0.45 s) | high voltage electric arc flashover, one sharp crack of electricity with two quick crackling bursts, over in half a second, a low thump underneath, sizzling electrical crackle, snapping, dry, outdoors, close, dry. |
| `sa3_battery_arc_v3_clean.wav` | Industrial Battery `strike` (take 2) | 282419364 | 1.0 | cleaned; the recipe keeps only the first crack (0-0.45 s) | high voltage electric arc flashover, one sharp crack of electricity with two quick crackling bursts, over in half a second, a low thump underneath, sizzling electrical crackle, snapping, dry, outdoors, close, dry. |
