# Clockwork Jetpack: Sound Design

Brief: design/briefs/clockwork-jetpack.md  ·  Spec: design/specs/clockwork-jetpack.md (D32)  ·  Model and clips: design/models/clockwork-jetpack.gen.js  ·  Cues: design/sound/clockwork-jetpack/make-cues.js → clockwork-jetpack.cues.json  ·  Stage: **3, in the Blockbench clips** (2026-11-03; real sounds approved)

## Identity
- **Mechanical:** a mainspring letting go through an escapement, driving two little ducted fans. Brass, steel, walnut and canvas.
- **Emotional:** quietly confident and a bit whimsical, like a wind-up toy that really flies.
- **Movement:** silent while it runs. It's strapped to your back, so it never plays a running sound (user, 2026-11-03: "I don't want there to be any running sounds ... being strapped to your back constant sounds are going to get annoying"). It speaks only when something changes: the engine starting and stopping, the wings fanning open and shut, the spring running out. Flight's whoosh is vanilla's elytra sound, which the game plays by itself in the fall-flying pose.

## Signature sounds
1. **The escapement tick:** Trade Bot's clock tick, lighter and quicker. It's the family's heartbeat, heard only as the engine starts, in the run-down and in the low-wind warning.
2. **The fan spin:** the ducted fans spinning up or down. Only on start, stop and boost, never as a loop.
3. **The rib cascade:** five dry walnut clacks in a row, with brass knuckle clicks and a canvas *fwump*. That's the wings.
4. **The key wind:** Trade Bot's winding rattle with the pawl clicking. That's the station.
5. **The run-down:** ticks spreading apart and falling in pitch, the pawl dropping, one last shiver. The Juggernaut's death, in miniature.

## Loudness intent (the staging ladder, design/sound/staging.md)
- **No running loops on the jetpack.** Flight's wind is vanilla's elytra sound.
- **Wings unfold and fold, run-down, engine on and off:** starting/stopping class, B + 9 (about -27), 16 blocks.
- **Controls** (hover clicks, wind low, equip, rails): small, -34.
- **Station:** winding loops at B (-36), 8 blocks, but only while it is actually winding a jetpack; there is no idle sound. "Fully wound" is starting class (-27), so you hear it from across the base.

## Events

### wings_unfold
**Purpose** · The showpiece: the wings fan open and lock.
**Sound Layers** · anticipation: a small creak (the tuck); movement: five walnut rib clacks, each with a little air; impact: five brass knuckle clicks as the elbows lock, then the canvas snapping taut (*fwump*); settle: a soft creak as the wings lift into their V.
**Timing** · tuck 0; rib clacks at 0.20, 0.25, 0.30, 0.35, 0.40 s (each rib's spring peak), falling in volume; knuckles 0.48–0.64; canvas taut 0.42–0.6; lift 0.6.
**Variations** · 3 takes, pitch ±4%.
**Implementation Notes** · a one-shot on entering flight mode (the J key); 16 blocks; subtitle "Clockwork Jetpack spreads its wings". Vanilla's elytra wind takes over from here.
**Source** · placeholder synth. Planned real: rung 2/3, wooden clacks (the Kenney impact pack's wood, or BigSoundBank's wooden-stick-on-bars `0995`, already used); brass clicks (Kenney `metalClick`); the canvas snap from a CC0 umbrella or sail recording (rung 3).

### wings_fold
**Purpose** · The wings close: landing, water or riding.
**Sound Layers** · the elbows unlock (knuckle clicks, trailing first), the canvas slackens (a soft air puff), the ribs close in a cascade, and a tiny bounce clack.
**Timing** · knuckles 0.05–0.17; slack 0.12; ribs 0.30–0.50; bounce 0.57.
**Variations** · 3 takes.
**Implementation Notes** · a one-shot on leaving flight mode.
**Source** · as wings_unfold.

### run_down
**Purpose** · Out of wind: everything slows to a stop.
**Sound Layers** · ticks spreading out and falling in pitch over 1.55 s; the fans spinning down; the pawl drops (a brass clink at 1.6); a last half-tick shiver at 1.78.
**Timing** · 1.9 s.
**Variations** · 2 takes.
**Implementation Notes** · a one-shot when the wind hits 0 (the loops stop). Starting class, 16 blocks.
**Source** · placeholder synth. Planned real: rung 2, Trade Bot's ticks re-timed, the fan recording slowed, a Kenney metal clink.

### engine_on / engine_off
**Purpose** · G turns the jetpack on or off.
**Sound Layers** · on: a brass click, the fans spinning up, two ticks; off: a click, the fans spinning down, the pawl clink.
**Timing** · on 0.55 s, off 0.6 s.
**Variations** · 2 takes each.
**Implementation Notes** · one-shots on the toggle.
**Source** · placeholder synth. Planned real: Kenney `metalClick` and the fan recording sped up or slowed.

### hover_on / hover_off
**Purpose** · H latches or releases hover.
**Sound Layers** · a click and a small brass clink (on); reversed order (off).
**Timing** · 0.2 s.
**Variations** · 2 takes each.
**Implementation Notes** · one-shots; quiet.
**Source** · placeholder synth. Planned real: Kenney interface clicks plus a clink.

### boost
**Purpose** · Holding Jump in flight: a surge (×1.25).
**Sound Layers** · the fans rising in pitch, a whoosh, a quick ratchet burst.
**Timing** · 0.5 s.
**Variations** · 3 takes.
**Implementation Notes** · a one-shot when the boost starts; flight_loop plays on.
**Source** · placeholder synth. Planned real: the fan recording sped up, Trade Bot's wind for the ratchet.

### wind_low
**Purpose** · The warning from 10% wind: a stuttering double tick.
**Sound Layers** · tick-tick, the second weaker, and a dull clink.
**Timing** · 0.45 s, played **once** as the wind crosses 10% (not repeated).
**Variations** · 2 takes.
**Implementation Notes** · players category, heard by the wearer and those nearby (8 blocks).
**Source** · placeholder synth. Planned real: rung 2 ticks.

### equip
**Purpose** · Putting it on: straps and buckles.
**Sound Layers** · a leather rustle, then two brass buckle clinks.
**Timing** · 0.5 s.
**Variations** · 2 takes.
**Implementation Notes** · the equip sound for the Jetpack slot and the chest slot.
**Source** · placeholder synth. Planned real: rung 1, vanilla `item.armor.equip_leather` as an event layered with a Kenney clink, or Kenney cloth (`cloth*`).

### rails_extend
**Purpose** · The stand-off rails slide out when a backpack is worn.
**Sound Layers** · a brass slide and a click into place.
**Timing** · 0.4 s.
**Variations** · 1 take.
**Implementation Notes** · played when the rails extend (the backpack goes on).
**Source** · placeholder synth. Planned real: Kenney metal slide plus `metalClick`.

### station_hang / station_take
**Purpose** · Hanging the jetpack on the Winding Station, or taking it down.
**Sound Layers** · a wooden thunk on the post and a brass clink on the yoke; the take is a clink and a strap rustle.
**Timing** · 0.25 s / 0.2 s.
**Variations** · 2 takes each.
**Implementation Notes** · one-shots on the right-click.
**Source** · placeholder synth. Planned real: Kenney wood impacts and metal clinks.

### station_boom
**Purpose** · The boom reaching out and lowering.
**Sound Layers** · a brass tube slide (the telescope), then a creak as it lowers.
**Timing** · 0.6 s, from 0.1 s into the clip.
**Variations** · 2 takes.
**Implementation Notes** · a one-shot when winding starts.
**Source** · placeholder synth. Planned real: the Juggernaut's `jug_reel` (rung 2) or a Kenney slide.

### station_engage
**Purpose** · The socket drops onto the key.
**Sound Layers** · a sharp latch click and a short brass clang.
**Timing** · 0.3 s, at 0.6 s.
**Variations** · 2 takes.
**Implementation Notes** · a one-shot.
**Source** · placeholder synth. Planned real: Kenney `metalLatch` (Trade Bot's winder engage, rung 2).

### station_winding
**Purpose** · Winding: the station is filling the spring.
**Sound Layers** · Trade Bot's key-wind ratchet; the pawl clicking 6 times a second (the clip's tick rate); a low drive whirr.
**Timing** · a 2 s loop.
**Variations** · one loop; the code pitches it by RPM (±15%).
**Implementation Notes** · loops while winding; running bed (-36), 8 blocks.
**Source** · placeholder synth. Planned real: rung 2, `sa3_tradebot_wind_*` (Trade Bot's approved key wind) and the Kenney clicks.

### station_full
**Purpose** · Fully wound: a satisfying brass ping you hear across the base.
**Sound Layers** · a pawl click, then two brass pings, a fifth apart.
**Timing** · 1.1 s.
**Variations** · 1 take.
**Implementation Notes** · a one-shot as the jetpack reaches full; starting class, 16 blocks. The same moment triggers the comparator change.
**Source** · placeholder synth. Planned real: rung 2/3, a real small bell or a brass ping (Kenney casino or UI chimes, or BigSoundBank); Trade Bot's `brass ping` if it fits.

### station_release
**Purpose** · The boom lets go and parks.
**Sound Layers** · a click, a creak as it lifts, then the tube sliding in.
**Timing** · 0.7 s.
**Variations** · 2 takes.
**Implementation Notes** · a one-shot after station_full or when winding is interrupted.
**Source** · as station_boom.

## Clips
| Clip | Cues (s → event) |
|---|---|
| thrust, hover, idle | (none: no running sounds) |
| flight | (none: vanilla elytra wind, played by the game) |
| wings_unfold | 0 wings_unfold |
| wings_fold | 0 wings_fold |
| run_down | 0 run_down |
| coil_states | 3 wind_low (as the coil reaches its last frame) |
| station_hang, station_back_in | 0.1 station_boom · 0.6 station_engage · 0.8 and 2.8 station_winding · 4.9 station_full · 5.0 station_release |
| standoff | 0.1 rails_extend |

Not on a clip (code only): engine_on, engine_off, hover_on, hover_off, boost, equip, station_hang, station_take.

## Stage 2: real sounds
Built by design/sound/clockwork-jetpack/real-recipes.js (cuts.js for the new recordings, levels.js to set each event's level from its loudness target). Every event lands within 0.2 dB of its target. Sources and licences: design/sound/sources/SOURCES.md, "Clockwork Jetpack".

| Event | Real layers (rung) | Target |
|---|---|---|
| wings_unfold | Kenney creak (tuck); 5 Kenney light wood impacts (ribs); 5 metalClick (knuckles); a shaken nylon flag's snap (canvas taut, 3 takes: flags 1, 2, 4); creak (lift) (2, 3) | -27 |
| wings_fold | metalClick ×5; flag 3's shake (slack); wood impacts ×5; a tiny wood bounce (2, 3) | -28 |
| run_down | Trade Bot's ticks re-timed and sagging; the fans spinning down; a metal clink (pawl); two shiver ticks (2, 3) | -27 |
| engine_on / engine_off | metalClick; the fans spinning up or down: a small drone pitched down and softened (user pick over an electric fan); ticks or the pawl clink (2, 3) | -27 / -28 |
| hover_on / hover_off | Kenney toggles and a light metal clink (2) | -34 |
| boost | the fan's spin-up mid-section, Trade Bot's wind; a synth air whoosh (4: no clean real air surge) | -30 |
| wind_low | two Trade Bot ticks and a clink (2) | -32 |
| equip | Kenney belt handling and two buckle clinks (2) | -32 |
| rails_extend | Kenney card slide pitched down and a latch (2) | -34 |
| station_hang / station_take | wood impact and clink; clink and leather (2) | -31 / -32 |
| station_boom / station_release | the Juggernaut's fishing-reel ratchet and Kenney creaks (2) | -32 |
| station_engage | metalLatch and a medium metal impact (2) | -30 |
| station_winding | a 2 s loop: Trade Bot's four key-wind takes end to end, the pawl clicking every third of a second, the Pressure Crucible's gears far under (2) | -36 (loop) |
| station_full | a click, then the pendulum chime pitched up (user pick over a struck bell) (2) | -27 |

