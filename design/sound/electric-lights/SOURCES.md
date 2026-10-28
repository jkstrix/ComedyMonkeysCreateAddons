# Electric Lights sources

Every recording the lights' real sounds are built from. All are **CC0** (BigSoundBank, Kenney); nothing from Minecraft, other mods or Epidemic Sound, and nothing generated. Cuts live in `design/sound/sources/bigsoundbank/cuts/` (`lt_*.wav`).

| Event | Rung | Built from |
|---|---|---|
| `switch_on`, `switch_off` | 3 (new CC0) | BigSoundBank #0052 [Electric switch #2](https://bigsoundbank.com/detail-0052-electric-switch-2.html) (two takes: `lt_sw_a_*`, `lt_sw_b_*`). #0540 [Old switch](https://bigsoundbank.com/old-switch-s0540.html) was auditioned as a third take; the user preferred the modern clicks (2026-10-28) |
| `flicker` | 2 (project library) | BigSoundBank #2143 and #2144 Flashing light bulb (`lt_bulb_*`), over #2118 Neon or transformer (`lt_transformer`), low-passed and chopped by an 11 Hz tremolo |
| `warm_up` | 2 | Kenney Interface Sounds `tick_004` (pitched down, slowing), over `lt_transformer` low-passed to a faint swell |
| `beacon_whirr` | 3 + 2 | BigSoundBank #1263 [Car antenna, exit](https://bigsoundbank.com/car-antenna-exit-s1263.html), its steady small motor (`lt_antenna_run`), pitched down and muffled; Kenney `tick_004` once a turn |
| `floodlight_aim` | 3 | #1263's motor start (`lt_antenna_start`) and its stop clunk (`lt_antenna_stop`) |
| `brightness_step` | 2 | Kenney UI Audio `switch24` (a soft detent), Kenney Interface Sounds `tick_002` |

Searched and not used: BigSoundBank's "gyrophare" results are sirens, not beacon motors; no ballast or cooling-tick recording turned up ("cooling", "quenching", "thermal"); #0036 Switch desk lamp (push-clicks with a release click, less like a wall switch); #0078 Electric fan #1 (very quiet, mostly hiss once raised).
