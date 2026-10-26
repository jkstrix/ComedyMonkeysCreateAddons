# Battery sources

Every recording the Industrial Battery's sounds are built from. All are local Stable Audio 3 generations, listed with prompt,
seed and processing in `design/sound/sources/generated/SOURCES.md` (weights under the Stability AI Community License;
commercial use allowed under US$1M revenue, and the mod is free). Nothing from Minecraft, other mods or Epidemic Sound.

| Event | Built from |
|---|---|
| `hum` | `sa3_battery_hum_v3_loop` |
| `form` | `sa3_battery_contactor_close_v2_clean`, with `sa3_battery_hum_v3_loop` swelling in |
| `unform` | `sa3_battery_contactor_open_v2_clean`, with `sa3_battery_hum_v3_loop` falling away (pitched down) |
| `strike` | `sa3_battery_arc_v2_clean` and `sa3_battery_arc_v3_clean` (one per take), with a synthesised low thump |

The plain Battery has no sounds of its own: it uses the vanilla metal block sounds.
