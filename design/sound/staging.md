# Sound staging

How loud each kind of machine sound plays in game, so every block sits with the others. Set on 2026-10-24 across the
Geothermal Turbine, Lava Generator, Electric Motor, Magma Engines and Pressure Crucible; the Magma Engine (tuned in game)
is the reference.

Loudness is measured **in game**: the file's loudness plus the volume the code plays it at (`20 log10(volume)`).
Loops are measured as integrated LUFS, one-shots as their loudest 400 ms (max momentary LUFS). B is the running bed,
**-36 LUFS**.

| Role | Loudness | Range |
|---|---|---|
| Running loop, main layer | B (-36) | 8 blocks (a 3x3x3 multiblock 12) |
| Running loop, second layer | B - 3 to 6 | 8 (12) |
| Running pulse, more than once a second (chuffs, chugs, sparks) | B to B - 3 | 8 (12) |
| Running pulse, occasional (a creak, a crunch, a groan) | B + 2 to 5 | 8 (12) |
| Starting / stopping, main sound | B + 9 to 11 (-25 to -27) | 16 |
| Starting / stopping, extra layers | B + 5 to 8 | 16 |
| Cooling ticks | B - 8 | 8 (12) |
| Forming a multiblock | about B + 12 | 16 |

Running sounds play all day in a base, so they stay quiet and short-ranged; starting and stopping can be heard across a
room. Levels are set through each block's cue-file `level` (peak-based, so a change of x dB is a factor of
`10^(x/20)`); the code's volumes stay as they are. To check a block, measure its shipped files with ffmpeg's `ebur128`
filter and add the code's volume.
