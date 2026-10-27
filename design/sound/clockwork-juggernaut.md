# Sound design: Clockwork Juggernaut

Sounds for the Clockwork Juggernaut and his props: the Winding Key ritual and the Spring Engine.

- **Sources:**
  - brief: [design/briefs/clockwork-juggernaut.md](../briefs/clockwork-juggernaut.md) (Revision 2 and "Props as built");
  - spec: [design/specs/clockwork-juggernaut.md](../specs/clockwork-juggernaut.md);
  - clip timings: [clockwork-juggernaut.clips.js](../models/clockwork-juggernaut.clips.js) and [juggernaut-props.gen.js](../models/juggernaut-props.gen.js).
- **Cue file:** [clockwork-juggernaut/clockwork-juggernaut.cues.json](clockwork-juggernaut/clockwork-juggernaut.cues.json), written by [make-cues.js](clockwork-juggernaut/make-cues.js). Edit the script, not the JSON.

**Progress:**
- Stage 1 (placeholders): approved ("sounds good").
- Stage 2 (real sounds): approved ("I like those").
- Stage 3 (sound keyframes in the Blockbench clips): approved ("Looks great"):
  - [clockwork-juggernaut.clips.js](../models/clockwork-juggernaut.clips.js) and [juggernaut-props.gen.js](../models/juggernaut-props.gen.js) read the cue file. Each clip's cues become sound keyframes on its Effects track, pointing at `real/<id>.ogg`.
  - The build stops if a cued sound has no real file.
  - That's 106 keyframes across 32 Juggernaut clips (all but `pose_sheet`), and 133 across the 3 prop clips.

- **The board:** [clockwork-juggernaut/clockwork-juggernaut-soundboard.html](clockwork-juggernaut/clockwork-juggernaut-soundboard.html). It switches between the real sounds and the placeholders.
- **To open it:**
  1. Serve the folder: `node ~/.claude/skills/character-sound-designer/scripts/serve.js design/sound/clockwork-juggernaut 8768`.
  2. Go to http://localhost:8768/clockwork-juggernaut-soundboard.html.

The placeholders are synth sketches, for judging timing and character only. They never ship.

**Rebuilding the real sounds** (in `design/sound/clockwork-juggernaut/`):
1. `node cuts.js` (only when the cuts change).
2. `node make-cues.js`.
3. `node ~/.claude/skills/character-sound-designer/scripts/build-real.js clockwork-juggernaut.cues.json ../sources real`.
4. `node levels.js`.
5. Run `make-cues.js` and `build-real.js` once more. `levels.js` sets each event's `level` from its loudness `target`, so the second build lands on target.

## Who he sounds like

**Mechanical identity.** He's a giant spring barrel driving an escapement:
- a heavy **tick-TOCK** sets the pace of everything he does;
- the **key's ratchet** winds him;
- **cog teeth** step his joints.

**Material identity.** Each material has its own sound:
- **stone** (feet, the left fist, the mask): deep thuds and grinding;
- **dark iron**: dull clanks;
- **tarnished-bronze shields**: a low ringing boom, like a war shield struck;
- **chains**: rattles;
- **frost-blue runes**: a low hum.

He's twice the Wither's size, so everything the family makes comes back an octave or two lower and slower.

**Emotional identity.** Relentless, proud, weary:
- *Relentless:* an unhurried rhythm that never stops.
- *Proud:* sharp, confident attacks on his impacts, and one bright ring on the salute.
- *Weary:* creaks that fall in pitch, ticks that slow and drop as he runs down, and long sighs of steam.

**Movement identity.** Heavy and slow:
- each footstep is a low stone thud, and his loose plates and chains rattle a beat later;
- his big moves have a long wind-up you can hear, so a player learns to dodge by ear.

**No voice.** The family's chirps are gone. **Steam through the iron beard is his voice**: a snort when he lowers his head, a sigh when he's idle, a roar when he loses his armour, and a last breath when he dies.

### Signature sounds (they recur through almost every event)

| Signature | What it is | Family link | Where it recurs |
|---|---|---|---|
| **Tick-TOCK** | A heavy escapement tick: an iron click on top of a deep thud, with a faint bronze ring | Trade Bot's `clock_tick`, two octaves down | His heartbeat while wound; slows during the run-down; a heavy tick every quarter turn of the ritual; the Spring Engine's escapement in miniature |
| **Key ratchet** | The giant key's pawl racing over its teeth | Trade Bot's `key_rattle`, slowed and heavier | Rewind, the ritual, leap coil, gear spin, key turns, the key jam, the Spring Engine's winding |
| **Steam breath** | Hissing through the beard bars and pipes | New: replaces the voice | Charge snort, sigh, the vent, the stage roar, cog throw, death |
| **Shield ring** | Tarnished bronze and steel struck: a low bell boom | Trade Bot's `brass_ping`, an octave or two down | Every hit while he's wound, every impact, the plates bursting off, the grab clang |
| **Rune hum** | A low frost-blue swell, from a fifth up to a soft drone | New | His slit lighting, the steam-vent tell, the stage change, lightning, the ritual's runes, the Spring Engine's rune band |

### Personality into sound

| Trait | Sound |
|---|---|
| Relentless | The tick never stops while he's wound. Attacks are unhurried wind-ups with hard landings. |
| Proud | Sharp attacks and a clean, bright clap-and-ring on the salute (the one charming beat) |
| Weary | Falling creaks, slowing ticks that drop in pitch, steam sighs |
| Grim soldier | No voice, no music. Drill-like regular rhythms; the plates rattle like kit on a march |

## Loudness and range

He's a boss in an open arena, so he plays louder and carries further than the mod's machines ([staging.md](staging.md)). Everything is set from the same bed, B = −36 LUFS, measured in game. Minecraft's range is 16 blocks × the volume when the volume is above 1. All his sounds use the `hostile` category, and the props use `blocks`.

| Role | Loudness | Range | Events |
|---|---|---|---|
| Heaviest impacts | B + 15 (−21) | 48 | `stomp_impact`, `leap_land`, `mainspring_burst`, `ritual_unfold` |
| Attack tells and big moves | B + 11 to 13 | 32 | `charge_tell`, `vent_tell`, `vent_blast`, `leap_coil`, `leap_launch`, `spin_*`, `stage_roar`, `plates_burst`, `grab_*`, `throw_whoosh`, `swing_*`, `feint_snap`, `key_jam` |
| Heartbeat tick and the rhythm states | B + 8 (−28) | 32 | `tick`, `tock`, `run_down_tick`, `rewind_loop`, `rewind_end` |
| Reactions | B + 9 | 24 | `hit_wound`, `hit_open`, `lightning`, `plate_ring` |
| Footsteps and loose kit | B + 5 to 7 | 24 | `step`, `step_run`, `skid`, `plates_settle`, `joint_*`, `head_ratchet` |
| Idle mannerisms, dormant | B + 2 to 4 | 16 | `steam_sigh`, `knuckle_grind`, `claw_flex`, `ember_flicker`, `steam_wisp` |
| The ritual's ticking | B → B + 10 as it winds | 16 → 48 | `ritual_tick`, `ritual_click` (the code raises their volume with the wind level) |
| Spring Engine pulses (more than once a second) | B to B − 3 | 8 | `engine_ratchet`, `engine_tick`, `engine_tock` |
| Spring Engine level changes | B + 2 to 5 | 8 | `engine_rune_up`, `engine_rune_down`, `engine_full`, `engine_empty` |

**The tick is the fight's music.** There's no boss music (spec D28), so the tick has to come through in a fight:
- it's sparse and short, so it can sit high without tiring the ear;
- it sits above the footsteps.

## Code-driven sounds (the game, not the clips)

- **Heartbeat.** While he's wound he plays `tick` and `tock` alternately, one every **0.75 s**:
  - in stage 3, every 0.65 s (1.15× faster, matching the motion speed-up);
  - it stops while he's dormant.
  
  Spec D28 says the ticking runs "faster as it runs down". I've read that as the fight speeding up stage by stage. The run-down itself is the four slowing ticks of `run_down_tick`, which matches the brief and the clip.
- **Run-down.** Four `run_down_tick`s at the clip's times. Each is pitched lower: 1.0, 0.94, 0.88 and 0.82.
- **Feint.** The first two run-down ticks, then `feint_snap`: a sharp, bright click where a third, deeper tick should come. Telling the two apart is the whole tell.
- **Rewind.** `rewind_loop` loops for the 4–6 s of the rewind, then `rewind_end` plays.
- **Hurt sounds.** `hit_wound` while he's wound (barely a dent) and `hit_open` while he's rewinding (a real hurt), so you can tell when you're doing damage.
- **Cog throw.** `cog_fly` loops on the cog while it flies, and `cog_hit` plays where it lands.
- **The ritual.**
  - `key_fit` plays when the key is placed in the effigy.
  - While the shaft winds it, `ritual_click` plays every 30° of key turn and `ritual_tick` every 90°. Both get louder and carry further as the wind fills (D18).
  - `ritual_runes` plays when the rune ring lights, and `ritual_steam` now and then.
  - `ritual_ready` plays at full wind, then `ritual_unfold` with `wake_ritual`.
  - Underground, `ritual_collapse` plays instead.
- **Spring Engine.**
  - `engine_ratchet` plays for each ratchet step while winding (the rate follows the input speed).
  - `engine_tick` and `engine_tock` play for each escapement step while it gives power.
  - `engine_rune_up` and `engine_rune_down` play when a quarter of the rune band lights or goes out.
  - `engine_full` and `engine_empty` play at the ends.
  - It's silent when idle.

## Sound events

The **Source** lines below are the plan for Stage 2: real sounds first, working down the ladder in order:
1. Minecraft's own sounds, referenced and never copied;
2. the project's library;
3. new CC0 recordings;
4. synthesis or AI generation, only where needed.

### Signature sounds

### tick
**Purpose** · His heartbeat while wound, and the fight's only music. Steady means he's dangerous.
**Sound Layers** · Mechanical: an escapement click. Material: an iron thud with a faint bronze ring. Personality: steady, unhurried.
**Timing** · Click 0–0.05 s, thud tail to 0.25 s, ring tail to 0.35 s.
**Variations** · 4 takes, pitch ±3%, gain ±1 dB. `tock` is its lower partner.
**Implementation Notes** · Code-driven, one-shot, every 0.75 s (0.65 s in stage 3), alternating with `tock`. Range 32.
**Source** · Rung 2: Trade Bot's `sa3_tradebot_tick_*` pitched down two octaves, layered over a Kenney `impactPunch_heavy` thud. Rung 1 for comparison: `block.copper_grate.step`, pitched low.

### tock
**Purpose** · The lower half of the tick-TOCK pair. Two notes read as clockwork; one note would read as a metronome.
**Sound Layers** · As `tick`: a lower click, a deeper thud, a darker ring.
**Timing** · 0.05 s click, a 0.3 s tail.
**Variations** · 4 takes, pitch ±3%.
**Implementation Notes** · Alternates with `tick`.
**Source** · As `tick`.

### run_down_tick
**Purpose** · The spring is giving out. Get behind him.
**Sound Layers** · Mechanical: a heavy tock, then the spring sagging (a low wobble). Material: an iron groan. Personality: weary, falling.
**Timing** · Tock at 0, a creak falling 0.05–0.65 s, a sag wobble to 0.6 s.
**Variations** · 3 takes. The code pitches each tick lower (1.0, 0.94, 0.88, 0.82).
**Implementation Notes** · 4 per run-down at the clip's times; the first 2 also play in a feint and the first 2 in death. Range 32.
**Source** · `tock`'s layers plus rung 2 `rpg-audio: creak1–3` pitched down. Rung 1 for comparison: `block.chain.step`.

### key_ratchet
**Purpose** · The key is turning.
**Sound Layers** · Mechanical: the pawl clicking over teeth. Material: bronze. Ends on a heavy catch.
**Timing** · A 0.6 s ratchet at 9 clicks/s, then a thud.
**Variations** · 3 takes, pitch ±4%.
**Implementation Notes** · One-shot (wake_ritual).
**Source** · Rung 2: Trade Bot's `sa3_tradebot_wind_*`, slowed to 0.5×.

### plate_ring
**Purpose** · His armour taking a blow.
**Sound Layers** · Material: a low bronze-and-steel boom with a high shimmer. Personality: unbothered.
**Timing** · Strike at 0, ringing out over 1.4 s.
**Variations** · 4 takes (shields, chest plate, helm, greaves), pitch ±6%.
**Implementation Notes** · Layered into `hit_wound`, `grab_clang` and the impacts. It also plays alone when a projectile hits him while he's wound. Range 24.
**Source** · Rung 1: `block.anvil.land` and `block.bell.resonate`, both too bright, so for comparison only. Rung 2: Kenney `impactBell_heavy`, `impactPlate_heavy` and `impactMetal_heavy`, pitched down. Rung 3: BigSoundBank "iron bar falls" (0235, already in the project).

### glow_on
**Purpose** · His slit and runes light up: he's awake.
**Sound Layers** · Rune hum: a swell rising a fifth into a soft drone, with a faint frost sparkle.
**Timing** · Swell 0–1.2 s, drone from 0.25 s.
**Variations** · 2 takes.
**Implementation Notes** · wake, wake_ritual. Range 32.
**Source** · Rung 1: `block.respawn_anchor.charge` and `block.beacon.activate` (try both). Rung 4 synth swell as the backing layer: no recording exists of runes lighting.

### steam_puff
**Purpose** · A short burst from the pipes: effort.
**Sound Layers** · A steam hiss and a small air whoosh.
**Timing** · 0.45 s.
**Variations** · 4 takes, pitch ±8%.
**Implementation Notes** · charge, wake_ritual. Range 24.
**Source** · Rung 1: `block.fire.extinguish` and `entity.generic.extinguish_fire`. Rung 3: a BigSoundBank pressure-release recording, for a cleaner hiss.

### Movement

### step
**Purpose** · He's walking toward you, and you can tell where from.
**Sound Layers** · Material: a deep stone thud, an iron clack, then the plate and chain rattle a beat later.
**Timing** · Thud 0–0.4 s, rattle from 0.06 s.
**Variations** · 6 takes, pitch ±5%.
**Implementation Notes** · Code: on each foot contact (walk 0.4 and 1.2 s of the 1.6 s cycle). Range 24.
**Source** · Rung 1: `block.heavy_core.step` and `entity.iron_golem.step`, pitched down. Rung 2: Kenney `impactPunch_heavy` and `impactMetal_heavy` for the rattle.

### step_run
**Purpose** · He's charging: faster, heavier footfalls.
**Sound Layers** · `step` plus a deeper sub-thud.
**Timing** · 0.5 s.
**Variations** · 4 takes.
**Implementation Notes** · charge, 4 footfalls. Range 32.
**Source** · As `step`.

### foot_scrape
**Purpose** · The charge's bull-like paw at the ground: get out of the way.
**Sound Layers** · Stone dragged on stone, then a small thud.
**Timing** · 0.22 s.
**Variations** · 3 takes.
**Implementation Notes** · charge 0.12 and 0.32 s.
**Source** · Rung 1: `block.grindstone.use` (short cut). Rung 3: a CC0 recording of stone dragged on stone.

### skid
**Purpose** · The end of the charge: he skids, stops and rocks back.
**Sound Layers** · A long scrape, a planted thud, a rattle.
**Timing** · Scrape 0–0.45 s, thud 0.38 s, rattle 0.42 s.
**Variations** · 2 takes.
**Implementation Notes** · charge 1.62 s.
**Source** · As `foot_scrape` plus `step`.

### plates_settle
**Purpose** · His loose kit catching up after a move (secondary motion).
**Sound Layers** · Clinks, a quick chain ratchet, and a last small clink.
**Timing** · Settles over 0.35 s.
**Variations** · 4 takes.
**Implementation Notes** · charge, grab_throw, wake and wake_ritual; layered into every impact.
**Source** · Rung 1: `block.chain.step` and `block.chain.hit`. Rung 2: Kenney `impactMetal_light`.

### joint_step
**Purpose** · His joints moving in clockwork steps as he pushes himself up.
**Sound Layers** · A tooth click over a short creak.
**Timing** · 0.2 s.
**Variations** · 4 takes, pitch ±6%.
**Implementation Notes** · wake, 4 steps between 0.85 and 1.45 s.
**Source** · Rung 2: `rpg-audio: metalClick` and `creak*`.

### head_ratchet
**Purpose** · The sentry scan: his head ticking round in steps.
**Sound Layers** · A fine ratchet over a neck creak.
**Timing** · 0.35 s per sweep.
**Variations** · 3 takes.
**Implementation Notes** · scan at 0.29, 1.16 and 1.99 s. Range 16.
**Source** · Rung 2: Trade Bot's wind takes, sped up.

### joint_creak
**Purpose** · A big joint taking weight.
**Sound Layers** · A slow stick-slip creak.
**Timing** · 0.5 s.
**Variations** · 3 takes.
**Implementation Notes** · knuckles 0.25 s.
**Source** · Rung 2: `rpg-audio: creak1–3`, pitched down.

### knuckle_grind
**Purpose** · He rolls his stone fist and the cog ring on his knuckles turns.
**Sound Layers** · A stone grind and a cog ratchet.
**Timing** · 0.4 s.
**Variations** · 2 takes.
**Implementation Notes** · knuckles 0.9 and 1.3 s.
**Source** · Rung 1: `block.grindstone.use`.

### claw_flex
**Purpose** · An idle flex of the gauntlet's fingers.
**Sound Layers** · A short fine ratchet and a clink.
**Timing** · 0.3 s.
**Variations** · 3 takes.
**Implementation Notes** · idle 1.45 s.
**Source** · Rung 2: Helper Bot's `claw_fidget` (ui-audio switches), pitched down.

### steam_sigh
**Purpose** · The weary old soldier: his shoulders rise and fall and his pipes breathe out.
**Sound Layers** · A creak as he breathes in, a long hiss as he breathes out, a low falling swell.
**Timing** · Creak 0–0.6 s, hiss 0.6–1.5 s.
**Variations** · 2 takes.
**Implementation Notes** · sigh 0.2 s. Range 16.
**Source** · Rung 1: `block.fire.extinguish`, slowed. Rung 3: a steam-release recording.

### Attacks

### charge_tell
**Purpose** · The charge is coming.
**Sound Layers** · His brows slam with a clack, steam snorts from the beard, his neck creaks down.
**Timing** · Clack 0, snort 0.02–0.32 s.
**Variations** · 2 takes.
**Implementation Notes** · charge 0.05 s. Range 32.
**Source** · Rung 2: `rpg-audio: metalLatch`. Rung 1: `entity.generic.extinguish_fire` for the snort.

### swing_windup
**Purpose** · The torso twisting back: a swing is coming.
**Sound Layers** · A deep creak and a slow ratchet.
**Timing** · 0.4 s.
**Variations** · 2 takes.
**Implementation Notes** · swing 0.02 s.
**Source** · Rung 2 creaks plus the wind takes.

### swing_whoosh
**Purpose** · The stone fist sweeping through.
**Sound Layers** · A low, heavy whoosh and the chain whipping round.
**Timing** · 0.4 s, peaking at about 0.12 s (the waist passes centre at 0.54 s in the clip).
**Variations** · 3 takes.
**Implementation Notes** · swing 0.42 s, feint 1.25 s.
**Source** · Rung 1: `entity.player.attack_sweep`, pitched down. Rung 4 synth whoosh as a backing layer.

### stomp_raise
**Purpose** · The leg lifting high: a stomp is coming.
**Sound Layers** · A straining creak and a hiss from the hip.
**Timing** · 0.5 s.
**Variations** · 2 takes.
**Implementation Notes** · stomp and stomp_double 0.1 s.
**Source** · Rung 2 creaks; rung 1 `block.fire.extinguish`.

### stomp_impact
**Purpose** · A shockwave, the strongest sound in his kit along with `leap_land`.
**Sound Layers** · A sub-boom, ground crunch, a shield ring, a long plate and chain rattle.
**Timing** · Impact 0, boom 0.9 s, rattle 0.12–0.5 s.
**Variations** · 4 takes, pitch ±4%.
**Implementation Notes** · stomp 0.73 s; stomp_double 0.72 and 1.27 s. Range 48.
**Source** · Rung 1: `item.mace.smash_ground_heavy` and `block.heavy_core.place` (try both), plus a rung 2 Kenney plate ring and rattle.

### grab_open
**Purpose** · The gauntlet rises and opens wide: a grab is coming.
**Sound Layers** · A ratchet of fingers spreading and a creak.
**Timing** · 0.45 s.
**Variations** · 2 takes.
**Implementation Notes** · grab_throw 0.05 s, grab_blocked 0.02 s. Range 32.
**Source** · Rung 2 wind takes and creaks.

### grab_snap
**Purpose** · Caught.
**Sound Layers** · A hard clack and an iron thud, muffled by the player in the claw.
**Timing** · 0.3 s.
**Variations** · 3 takes.
**Implementation Notes** · grab_throw 1.22 s (the fingers shut at about 1.27 s).
**Source** · Rung 1: `block.iron_door.close`. Rung 2: `rpg-audio: metalLatch`.

### grab_clang
**Purpose** · A raised shield blocked the grab: the claw clangs on nothing.
**Sound Layers** · A bright shield ring, the clack of empty fingers, a rattle as he shakes it off.
**Timing** · 1.4 s ring.
**Variations** · 2 takes.
**Implementation Notes** · grab_blocked 0.6 s. The player's shield-block sound plays too.
**Source** · Rung 1: `item.shield.block` plus a `plate_ring` layer.

### throw_whoosh
**Purpose** · The overhead hurl.
**Sound Layers** · A big rising whoosh and a steam grunt.
**Timing** · 0.45 s.
**Variations** · 2 takes.
**Implementation Notes** · grab_throw 1.55 s.
**Source** · Rung 1: `entity.wind_charge.wind_burst`, low. Rung 4 synth whoosh.

### grab_release
**Purpose** · The claw springs open as he lets go.
**Sound Layers** · A quick ratchet and a clink.
**Timing** · 0.2 s.
**Variations** · 2 takes.
**Implementation Notes** · grab_throw 1.95 s.
**Source** · Rung 2.

### leap_coil
**Purpose** · The 1.5 s spring-leap tell: his key winds faster and faster.
**Sound Layers** · A long compression creak; the ratchet in three bursts, each faster (8, 18 and 32 clicks/s); a hiss as the pipes vent.
**Timing** · 0–1.45 s, accelerating.
**Variations** · 2 takes.
**Implementation Notes** · leap 0.05 s. Range 32.
**Source** · Rung 2: wind takes, time-stretched, and creaks.

### leap_launch
**Purpose** · The release.
**Sound Layers** · A huge, low spring twang, a launch whoosh and a push-off thud.
**Timing** · 0.9 s.
**Variations** · 2 takes.
**Implementation Notes** · leap 1.5 s.
**Source** · Rung 1: `entity.breeze.jump` for the push. Rung 4 synth boing: no recording exists of a giant spring.

### leap_land
**Purpose** · He lands where you stood: the heaviest sound he makes.
**Sound Layers** · A sub-boom, ground crunch, a shield ring, two rattles (one, then a later smaller one).
**Timing** · Impact 0, boom 1.2 s, rattles 0.15 and 0.45 s.
**Variations** · 3 takes.
**Implementation Notes** · leap 2.08 s. Range 48.
**Source** · Rung 1: `item.mace.smash_ground_heavy` plus rung 2 plates.

### vent_tell
**Purpose** · The 2 s steam-vent tell: back off from his face.
**Sound Layers** · The rune hum swelling as the beard glows, and pressure hisses building in three steps.
**Timing** · Swell 0.15–1.95 s; hisses at 0.4, 0.9 and 1.4 s, louder each time.
**Variations** · 2 takes.
**Implementation Notes** · steam_vent 0.05 s. Range 32.
**Source** · Rung 1: `block.respawn_anchor.charge` for the glow. Rung 3: a steam-pressure recording (BigSoundBank or Freesound CC0).

### vent_blast
**Purpose** · The cone of steam.
**Sound Layers** · A roaring low hiss, high spray, a breath whoosh, two gutters at the end.
**Timing** · Roar 0–0.95 s, gutters 0.85 and 1.0 s.
**Variations** · 2 takes.
**Implementation Notes** · steam_vent 2.0 s. Range 32.
**Source** · Rung 3: a steam-locomotive or pressure-release CC0 recording. Rung 1: `block.lava.extinguish` for the layer that sets players alight.

### spin_windup
**Purpose** · The gear spin's 2 s tell.
**Sound Layers** · The torso creaking round; the ratchet speeding up; a governor whirr rising as he lets go; a whoosh.
**Timing** · Creak 0–0.9 s, whirr from 0.9 s, whoosh 1.0 s.
**Variations** · 2 takes.
**Implementation Notes** · gear_spin_start 0.2 s. Range 32.
**Source** · Rung 3: a CC0 flywheel or hand-crank recording. Rung 2: the wind takes.

### spin_loop
**Purpose** · He's spinning: run.
**Sound Layers** · A steady governor whirr and two arm whooshes a second.
**Timing** · A 1.0 s loop.
**Variations** · 1 seamless loop.
**Implementation Notes** · Looped for the 4 s spin (gear_spin). Range 32.
**Source** · Rung 3 whirr; rung 4 synth whooshes, timed to the arms.

### spin_down
**Purpose** · The spin ends and he staggers, dizzy: a chance to hit.
**Sound Layers** · The whirr falling, a last whoosh, a stagger thud and rattle.
**Timing** · Whirr 0–1.0 s, thud 1.0 s.
**Variations** · 2 takes.
**Implementation Notes** · gear_spin_end 0.
**Source** · As `spin_loop`.

### cog_draw
**Purpose** · He takes a cog from the rack on his back.
**Sound Layers** · A short metal slide and a heavy clink.
**Timing** · 0.55 s.
**Variations** · 2 takes.
**Implementation Notes** · cog_throw 0.45 s.
**Source** · Rung 2: Kenney `impactMetal_medium`.

### cog_throw
**Purpose** · The sidearm throw, with a grunt of steam.
**Sound Layers** · A whoosh and a steam grunt.
**Timing** · 0.45 s.
**Variations** · 2 takes.
**Implementation Notes** · cog_throw 0.8 s (released at about 0.87 s).
**Source** · Rung 4 whoosh; rung 1 extinguish hiss.

### cog_fly
**Purpose** · A thrown cog whirling toward you.
**Sound Layers** · A spinning whirr with four whooshes a second.
**Timing** · A 1.0 s loop.
**Variations** · 1 loop, pitched by spin.
**Implementation Notes** · Code: loops on the projectile. Range 24.
**Source** · Rung 4 synth: a spinning cog in the air has no clean recording.

### cog_hit
**Purpose** · The cog lands.
**Sound Layers** · A clang and a thud, then two bounces.
**Timing** · 0.5 s.
**Variations** · 3 takes.
**Implementation Notes** · Code: on impact. Range 24.
**Source** · Rung 1: `block.anvil.land`, pitched up. Rung 2: Kenney `impactMetal_heavy`.

### The rhythm: winding, rewinding, feints, jams, stages

### rewind_loop
**Purpose** · He's stopped to rewind: the key is open, so hit it now.
**Sound Layers** · The key ratchet racing (24 clicks/s at 720°/s), a low rune drone, and a faint pulse twice a second with the slit.
**Timing** · A 1.0 s loop.
**Variations** · 1 seamless loop.
**Implementation Notes** · Code: loops for the 4–6 s rewind (and during a jam). Range 32.
**Source** · Rung 2: Trade Bot's wind takes, looped and slowed. Rung 4 drone.

### rewind_end
**Purpose** · Fully wound again: get away from him.
**Sound Layers** · The ratchet slowing, a heavy pawl catch, a release of steam, a rattle as he straightens.
**Timing** · Ratchet 0–0.3 s, catch 0.3 s, hiss and rattle to 0.75 s.
**Variations** · 2 takes.
**Implementation Notes** · rewind_end 0. Range 32.
**Source** · Rung 2 wind takes and `rpg-audio: metalLatch`; rung 1 extinguish.

### feint_snap
**Purpose** · It was a fake: a sharp click instead of the third slowing tick (D47).
**Sound Layers** · A bright, dry click, a high tick, a lid clack and a steam snort. It has to be as unlike `run_down_tick` as possible: high, short, sharp.
**Timing** · 0.3 s.
**Variations** · 2 takes.
**Implementation Notes** · feint 1.2 s. Range 32.
**Source** · Rung 2: Trade Bot's `focus_click` switches, plus `rpg-audio: metalLatch`.

### key_jam
**Purpose** · The key has jammed (it's stuck, keep hitting).
**Sound Layers** · A grinding scrape, a stuttering ratchet, crackles of sparks at 0.1, 0.5 and 0.8 s, two body jerks.
**Timing** · 1.0 s.
**Variations** · 2 takes.
**Implementation Notes** · key_jam 0, over the rewind loop. Range 32.
**Source** · Rung 2: BigSoundBank 0303 (grinding metal gate), plus 0134 and 0952 (shock and weld sparks), all already in the project.

### plates_burst
**Purpose** · A stage change: his armour springs off.
**Sound Layers** · A stagger thud, three shield rings at different pitches, a fling whoosh.
**Timing** · Thud 0, rings 0.06, 0.11 and 0.17 s.
**Variations** · 2 takes.
**Implementation Notes** · shed_plates_2 and shed_plates_3, at 0.12 s. Range 32.
**Source** · Rung 3: BigSoundBank 1771 and 1776 (falling sheet steel, already in the project) and `plate_ring` layers.

### stage_roar
**Purpose** · He roars steam: he's angrier now.
**Sound Layers** · A roaring hiss, spray, a low rune swell, a deep creak.
**Timing** · 0.8 s.
**Variations** · 2 takes.
**Implementation Notes** · shed_plates 0.7 s. Range 32.
**Source** · As `vent_blast`, shorter, plus a rune hum layer.

### plates_land
**Purpose** · The plates clattering to the ground around him.
**Sound Layers** · Three clangs falling in pitch, two clinks.
**Timing** · 0.5 s.
**Variations** · 3 takes.
**Implementation Notes** · shed_plates 1.15 s. In game it plays where the plate particles land.
**Source** · Rung 3: BigSoundBank 1771 and 1776.

### Reactions

### hit_wound
**Purpose** · You hit him while he's wound: barely a dent.
**Sound Layers** · A shield ring and a head tick toward you.
**Timing** · 1.4 s ring.
**Variations** · 4 takes.
**Implementation Notes** · The hurt sound while he's wound. Range 24.
**Source** · Rung 1: `entity.iron_golem.damage` for comparison. Rung 2 plate ring.

### hit_open
**Purpose** · You hit him while he's rewinding: real damage.
**Sound Layers** · A deep iron thud, a clack, a loose spring wobble, a spit of steam.
**Timing** · 0.6 s.
**Variations** · 4 takes.
**Implementation Notes** · The hurt sound while he's rewinding. Range 24.
**Source** · Rung 1: `entity.iron_golem.hurt`, pitched down. Rung 4 boing.

### lightning
**Purpose** · Lightning rewound him: he's straight back in the fight.
**Sound Layers** · Crackling arcs, a rune surge, a whirr spinning up, a jolt clack.
**Timing** · 0.7 s.
**Variations** · 1 take.
**Implementation Notes** · lightning 0. The vanilla thunder plays too.
**Source** · Rung 1: `entity.lightning_bolt.impact`; rung 2 BigSoundBank 0134.

### Rest, waking and death

### ember_flicker
**Purpose** · Dormant, but not dead.
**Sound Layers** · A faint hum and a tiny tick.
**Timing** · 0.25 s.
**Variations** · 3 takes.
**Implementation Notes** · dormant 1.3 and 3.1 s. Range 16.
**Source** · Rung 2: BigSoundBank 2143 (flashing bulb).

### steam_wisp
**Purpose** · A breath of steam while he's dormant.
**Sound Layers** · A soft hiss.
**Timing** · 0.9 s.
**Variations** · 2 takes.
**Implementation Notes** · dormant 2.0 s. Range 16.
**Source** · Rung 1: extinguish, quiet.

### key_turn
**Purpose** · One heavy turn of the key: waking.
**Sound Layers** · A short ratchet and a heavy catch.
**Timing** · 0.55 s.
**Variations** · 2 takes.
**Implementation Notes** · wake 0.55 and 1.9 s.
**Source** · Rung 2 wind takes and `metalLatch`.

### salute_snap
**Purpose** · His one beat of charm: a stiff military salute.
**Sound Layers** · A crisp clack, a bronze ring that stays, a small proud puff of steam.
**Timing** · 0.9 s.
**Variations** · 2 takes.
**Implementation Notes** · wake 2.3 s, wake_ritual 2.05 s, salute 0.45 s. Range 32.
**Source** · Rung 2: Trade Bot's `brass_ping` recording, an octave down, plus `metalLatch`.

### glow_out
**Purpose** · His slit going dark.
**Sound Layers** · A falling swell and a last tick.
**Timing** · 1.0 s.
**Variations** · 1 take.
**Implementation Notes** · death 1.4 s.
**Source** · Rung 1: `block.respawn_anchor.deplete` and `block.beacon.deactivate`.

### kneel_impact
**Purpose** · He falls to one knee.
**Sound Layers** · A deep thud, a crunch, a shield ring, a rattle.
**Timing** · 0.8 s.
**Variations** · 2 takes.
**Implementation Notes** · death 2.25 s. Range 32.
**Source** · Rung 1: `block.heavy_core.place` plus rung 2 plates.

### key_unwind
**Purpose** · The key spins free backwards.
**Sound Layers** · A racing ratchet that slows, and a falling whirr.
**Timing** · 0.7 s.
**Variations** · 1 take.
**Implementation Notes** · death 2.6 s.
**Source** · Rung 2 wind takes, reversed and sped up.

### mainspring_burst
**Purpose** · The Mainspring tears out of his chest: the reward.
**Sound Layers** · A clack, a thud, a huge low twang with a long wobble, a higher partner twang, a bright ring.
**Timing** · Twang 1.5 s, wobbling down: boing … boink … tink.
**Variations** · 1 take.
**Implementation Notes** · death 2.9 s. Range 48.
**Source** · Rung 4 synth boing on a rung 2 metal impact: no recording exists of a giant uncoiling spring. Rung 3 to try first: a recorded spring door stop or ruler twang, pitched down.

### fist_thud
**Purpose** · He slumps forward onto his fists.
**Sound Layers** · A stone thud and a rattle.
**Timing** · 0.5 s.
**Variations** · 2 takes.
**Implementation Notes** · death 3.15 s.
**Source** · As `step`.

### death_steam
**Purpose** · His last breath.
**Sound Layers** · A long falling hiss, a falling swell, a last creak.
**Timing** · 1.4 s.
**Variations** · 1 take.
**Implementation Notes** · death 3.35 s. Range 32.
**Source** · Rung 1 extinguish, slowed. Rung 3 steam release.

### The ritual

### key_fit
**Purpose** · The Winding Key goes into the effigy.
**Sound Layers** · A heavy clack, a bronze ring, a small ratchet.
**Timing** · 0.5 s.
**Variations** · 2 takes.
**Implementation Notes** · Code: on placing the key. Range 16. Category `blocks`.
**Source** · Rung 2: BigSoundBank 0378 (a lock and a big key, already in the project).

### ritual_click
**Purpose** · The effigy's key collar clicking as it winds.
**Sound Layers** · A tooth click.
**Timing** · 0.15 s.
**Variations** · 4 takes.
**Implementation Notes** · Code: every 30° of key turn. Its volume rises with the wind level. Range 16 → 48.
**Source** · Rung 2: Trade Bot's tick takes, pitched up.

### ritual_tick
**Purpose** · The effigy ticking louder and louder: something is waking.
**Sound Layers** · His heavy `tick`, plus the brass blocks rattling.
**Timing** · 0.4 s.
**Variations** · 4 takes.
**Implementation Notes** · Code: every 90° of key turn, volume rising with wind (D18).
**Source** · As `tick`.

### ritual_runes
**Purpose** · The rune ring on the Winding Key lights up.
**Sound Layers** · A soft rune hum.
**Timing** · 1.0 s.
**Variations** · 1 take.
**Implementation Notes** · Plays once the ring lights.
**Source** · Rung 1: `block.respawn_anchor.charge`, quiet.

### ritual_steam
**Purpose** · Steam escaping the effigy's seams.
**Sound Layers** · A hiss.
**Timing** · 0.6 s.
**Variations** · 3 takes.
**Implementation Notes** · Code: now and then while winding.
**Source** · Rung 1: extinguish.

### ritual_ready
**Purpose** · Fully wound: it's about to wake.
**Sound Layers** · A rune swell, a racing ratchet, a bronze ring.
**Timing** · 1.0 s.
**Variations** · 1 take.
**Implementation Notes** · Code: at full wind, just before the unfold. Range 48.
**Source** · Rung 1: `block.vault.activate` and `block.trial_spawner.ominous_activate` (try both).

### ritual_unfold
**Purpose** · The effigy becomes the Juggernaut.
**Sound Layers** · A deep thud, a rune flash, brass clangs, a ratchet, a burst of steam.
**Timing** · 0.9 s.
**Variations** · 1 take.
**Implementation Notes** · wake_ritual 0. Range 48.
**Source** · Rung 1: `block.vault.open_shutter` plus rung 2 plates and wind.

### ritual_collapse
**Purpose** · Underground, the effigy falls into a column of steam (he appears on the surface).
**Sound Layers** · A thud, a clang, a long hiss and a rising whoosh.
**Timing** · 1.4 s.
**Variations** · 1 take.
**Implementation Notes** · Code: in place of the unfold underground. Range 32.
**Source** · Rung 1: extinguish plus `entity.wind_charge.wind_burst`.

### Spring Engine

### engine_ratchet
**Purpose** · It's winding.
**Sound Layers** · A small pawl click.
**Timing** · 0.1 s.
**Variations** · 4 takes, pitch ±4%.
**Implementation Notes** · Code: on each ratchet step (3 a second in the preview). Range 8. Category `blocks`.
**Source** · Rung 2: Trade Bot's tick takes, pitched up and quiet.

### engine_tick
**Purpose** · It's giving power.
**Sound Layers** · An escapement tick with a small knock.
**Timing** · 0.08 s.
**Variations** · 3 takes.
**Implementation Notes** · Code: alternates with `engine_tock`, on each escapement step. Range 8.
**Source** · As `tick`, small.

### engine_tock
**Purpose** · The tick's partner.
**Sound Layers** · Same as `engine_tick`, lower.
**Timing** · 0.08 s.
**Variations** · 3 takes.
**Implementation Notes** · See `engine_tick`.
**Source** · As `engine_tick`.

### engine_rune_up
**Purpose** · Another quarter is stored.
**Sound Layers** · A soft rune swell.
**Timing** · 0.6 s.
**Variations** · 1 take; the code pitches it higher per level.
**Implementation Notes** · Code: when a rune quarter lights.
**Source** · Rung 1: `block.respawn_anchor.charge`, quiet and pitched.

### engine_rune_down
**Purpose** · Another quarter is spent.
**Sound Layers** · A falling swell.
**Timing** · 0.6 s.
**Variations** · 1 take.
**Implementation Notes** · Code: when a rune quarter goes out.
**Source** · Rung 1: `block.respawn_anchor.deplete`, quiet.

### engine_full
**Purpose** · Fully wound: an hour's power is stored.
**Sound Layers** · A pawl lock clack, a bronze ring, a rune swell.
**Timing** · 0.8 s.
**Variations** · 1 take.
**Implementation Notes** · Code: on reaching full. Range 16.
**Source** · Rung 2: Trade Bot's `brass_ping`, low, plus `metalLatch`.

### engine_empty
**Purpose** · It's run down.
**Sound Layers** · A sagging creak and a soft thunk.
**Timing** · 0.5 s.
**Variations** · 1 take.
**Implementation Notes** · Code: on reaching empty. Range 16.
**Source** · Rung 2 creaks.

## Cues per clip

Times are in seconds, measured from the clips (foot contacts, finger crossings and key-speed peaks). The scratch script `jtimes.js` was used for that.

| Clip | Cues |
|---|---|
| idle (3 s loop) | tick 0 · tock 0.75 · claw_flex 1.45 · tick 1.5 · tock 2.25 |
| walk (1.6 s loop) | tick 0 · step 0.4 · tock 0.8 · step 1.2 |
| scan | tick 0 · head_ratchet 0.29 · tock 0.75 · head_ratchet 1.16 · tick 1.5 · head_ratchet 1.99 · tock 2.25 |
| knuckles | tick 0 · joint_creak 0.25 · tock 0.75 · knuckle_grind 0.9 · knuckle_grind 1.3 · tick 1.5 |
| sigh | tick 0 · steam_sigh 0.2 · tock 1.5 |
| charge | charge_tell 0.05 · foot_scrape 0.12 · foot_scrape 0.32 · steam_puff 0.45 · step_run 0.75, 1.0, 1.25, 1.5 · skid 1.62 · plates_settle 1.95 |
| swing | swing_windup 0.02 · swing_whoosh 0.42 |
| stomp | stomp_raise 0.1 · stomp_impact 0.73 |
| stomp_double | stomp_raise 0.1 · stomp_impact 0.72 · stomp_impact 1.27 |
| grab_throw | grab_open 0.05 · grab_snap 1.22 · throw_whoosh 1.55 · grab_release 1.95 · plates_settle 2.1 |
| grab_blocked | grab_open 0.02 · grab_clang 0.6 |
| leap | leap_coil 0.05 · leap_launch 1.5 · leap_land 2.08 |
| steam_vent | vent_tell 0.05 · vent_blast 2.0 |
| gear_spin_start | spin_windup 0.2 |
| gear_spin (1 s loop) | spin_loop 0 |
| gear_spin_end | spin_down 0 |
| cog_throw | cog_draw 0.45 · cog_throw 0.8 |
| run_down | run_down_tick 0.45 · 1.35 · 2.36 · 3.56 |
| rewind (1 s loop) | rewind_loop 0 |
| rewind_end | rewind_end 0 |
| feint | run_down_tick 0.44 · run_down_tick 0.99 · feint_snap 1.2 · swing_whoosh 1.25 |
| key_jam | key_jam 0 |
| shed_plates_2, shed_plates_3 | plates_burst 0.12 · stage_roar 0.7 · plates_land 1.15 |
| hit_wound / hit_open / lightning | the event of the same name at 0 |
| dormant (4 s loop) | ember_flicker 1.3 · steam_wisp 2.0 · ember_flicker 3.1 |
| wake | glow_on 0.2 · key_turn 0.55 · joint_step 0.85, 1.05, 1.25, 1.45 · plates_settle 1.75 · key_turn 1.9 · salute_snap 2.3 |
| wake_ritual | ritual_unfold 0 · key_ratchet 0.2 · glow_on 0.35 · steam_puff 0.9 · plates_settle 1.1 · salute_snap 2.05 |
| death | run_down_tick 0.34 · 0.84 · glow_out 1.4 · kneel_impact 2.25 · key_unwind 2.6 · mainspring_burst 2.9 · fist_thud 3.15 · death_steam 3.35 |
| salute | salute_snap 0.45 |
| ritual_winding (props, 6 s loop) | ritual_click every 30° of the accelerating key (from 0.31 s, closer and closer), ritual_tick every 90° · ritual_runes 0.6 · ritual_steam 2.5, 4.4 · ritual_ready 5.2 |
| engine_winding (props, 8 s loop) | engine_ratchet every 1/3 s · engine_rune_up 1, 3, 5, 7 |
| engine_unwinding (props, 8 s loop) | engine_tick and engine_tock alternating every 1/4 s · engine_rune_down 1, 3, 5, 7 |

Not in any clip, because only the game plays them: `plate_ring` (on its own), `cog_fly`, `cog_hit`, `key_fit`, `ritual_collapse`, `engine_full`, `engine_empty`.

## Stage 2: the real sounds

Every event is now built from **real recordings**: [real-recipes.js](clockwork-juggernaut/real-recipes.js), rendered into [clockwork-juggernaut/real/](clockwork-juggernaut/real/) as mono 44.1 kHz OGG. The per-recording licences are in [sources/SOURCES.md](sources/SOURCES.md) (Clockwork Juggernaut section). Where these differ from the **Source** plans above, this section wins.

**Rung 1 (Minecraft's own sounds)** were checked first: the mace smash, the heavy core, the iron golem, anvil, extinguish, respawn anchor and vault.
- None is baked into a file, because mixing a vanilla file into ours would ship Mojang audio.
- None plays alone, because each is too small or too bright for a 7-block stone giant.
- The code can still play a vanilla event alongside ours if a moment wants it in game, for example `item.mace.smash_ground_heavy` under `leap_land`. That's a decision for testing in game.

**What he's made of:**
- **Heartbeat:** a real **grandfather clock**'s tick and tock (BigSoundBank 1567), pitched down, on a dropped stone, with Trade Bot's own tick an octave and a half down underneath (the family link).
- **Key ratchet:** Trade Bot's winding takes slowed down, a fishing reel's click train, a big key in a lock for the catch, and a **chain hoist** for the racing rewind.
- **Shield ring:** three struck **gongs**, pitched down and damped, with an anvil and Kenney plate impacts.
- **Stone:** stones dropped one by one for feet, knees and fists, a **tombstone slab** sliding open for scrapes and grinds, and a distant explosion as the sub-boom under the stomps and the leap.
- **Steam:** steam-locomotive hisses. His voice is the steam.
- **Joints and kit:** low **barn-door groans**, a long creaking door and a metal door for the creaks, plus chains and falling sheet steel for the plates.
- **Mainspring:** a **large door-stop spring** flicked, pitched down, with a cartoon twang on top.
- **Rune hum (rung 4, synth):** there's no recording of runes glowing. It's a soft swell and drone, with a pendulum chime pitched down for shimmer. Small synth layers also sit *under* recordings: a sub-whoosh under the recorded whooshes, a wobble under the recorded spring, and a falling whirr under the gear-spin motor.

**Loudness:** each event has a `target` from the table above. `levels.js` measured every render (max momentary LUFS for one-shots, integrated for the loops) and set its `level`. All 75 are within 0.2 dB of their targets.

**Lengths:** the longest one-shot is the leap landing's 2.7 s rumble. Everything else is under 2.1 s, and the loops are exactly 1 s.

**Per event:**

| Event | Takes | Recordings | Synth layers |
|---|---|---|---|
| `tick` | 4 | jug_tick, jug_stone, Trade Bot sa3_tradebot_tick | — |
| `tock` | 4 | jug_tock, jug_stone, Trade Bot sa3_tradebot_tick | — |
| `run_down_tick` | 3 | jug_tock, jug_stone, jug_creak_low, jug_spring | — |
| `key_ratchet` | 3 | Trade Bot sa3_tradebot_wind, jug_reel_slow, jug_key_lock, jug_stone | — |
| `plate_ring` | 4 | jug_gong, jug_anvil, Kenney impactPlate_heavy | — |
| `glow_on` | 2 | jug_chime | swell, hum |
| `steam_puff` | 4 | jug_steam_puff, jug_steam_burst | whoosh |
| `step` | 6 | jug_stone, Kenney impactPunch_heavy, jug_chain_clink, jug_chain_rattle, Kenney impactMetal_light | — |
| `step_run` | 4 | jug_stone, Kenney impactPunch_heavy, BSB 1023, jug_chain_clink, jug_chain_rattle, Kenney impactMetal_light | — |
| `foot_scrape` | 3 | jug_grind_stone, jug_stone | — |
| `skid` | 2 | jug_grind_stone, jug_stone, jug_chain_clink, jug_chain_rattle, Kenney impactMetal_light | — |
| `plates_settle` | 4 | jug_chain_clink, jug_chain_rattle, Kenney impactMetal_light | — |
| `joint_step` | 4 | Kenney metalClick, jug_creak_low | — |
| `head_ratchet` | 3 | jug_reel, jug_creak_low | — |
| `joint_creak` | 3 | jug_creak_low, jug_creak_long | — |
| `knuckle_grind` | 2 | jug_grind_stone, jug_reel_slow | — |
| `claw_flex` | 3 | jug_reel, jug_chain_clink | — |
| `steam_sigh` | 2 | jug_creak_low, BSB 0227 | swell |
| `charge_tell` | 2 | Kenney metalLatch, jug_steam_burst, jug_creak_low | — |
| `swing_windup` | 2 | jug_creak_long, jug_reel_slow | — |
| `swing_whoosh` | 3 | jug_whoosh, jug_chain_rattle | whoosh |
| `stomp_raise` | 2 | jug_creak_low, jug_steam_puff | — |
| `stomp_impact` | 4 | jug_stone, BSB 1023, Kenney impactPunch_heavy, jug_gong, BSB 1771, BSB 1772, BSB 1774, BSB 1776, BSB 1778, jug_chain_clink, jug_chain_rattle, Kenney impactMetal_light | — |
| `grab_open` | 2 | jug_reel, jug_creak_metal | — |
| `grab_snap` | 3 | Kenney metalLatch, Kenney impactPunch_heavy, jug_anvil | — |
| `grab_clang` | 2 | jug_anvil, jug_gong, Kenney metalLatch, jug_chain_clink, jug_chain_rattle, Kenney impactMetal_light | — |
| `throw_whoosh` | 2 | jug_whoosh, jug_steam_burst | — |
| `grab_release` | 2 | jug_reel, jug_chain_clink | — |
| `leap_coil` | 2 | jug_creak_long, Trade Bot sa3_tradebot_wind, jug_steam_mid | — |
| `leap_launch` | 2 | jug_spring, jug_whoosh, jug_stone | boing |
| `leap_land` | 3 | jug_stone, BSB 1023, Kenney impactPunch_heavy, jug_gong, BSB 1771, BSB 1772, BSB 1774, BSB 1776, BSB 1778, jug_chain_clink, jug_chain_rattle, Kenney impactMetal_light | — |
| `vent_tell` | 2 | jug_steam_long, jug_creak_low | swell, hum |
| `vent_blast` | 2 | jug_steam_long, BSB 3019, jug_steam_burst | whoosh |
| `spin_windup` | 2 | jug_creak_long, Trade Bot sa3_tradebot_wind, jug_whirr_loop, jug_whoosh | — |
| `spin_loop` | 1 | jug_whirr_loop, jug_whoosh | — |
| `spin_down` | 2 | jug_whirr_loop, jug_whoosh, jug_stone, jug_chain_clink, jug_chain_rattle, Kenney impactMetal_light | servo |
| `cog_draw` | 2 | jug_grind_metal, jug_anvil | — |
| `cog_throw` | 2 | jug_whoosh, jug_steam_burst | — |
| `cog_fly` | 1 | jug_whirr_loop, jug_whoosh | — |
| `cog_hit` | 3 | jug_anvil, Kenney impactMetal_heavy, jug_chain_clink | — |
| `rewind_loop` | 1 | jug_ratchet_hoist_loop | hum, swell |
| `rewind_end` | 2 | jug_ratchet_hoist, jug_key_lock, jug_stone, jug_steam_puff, jug_chain_clink, jug_chain_rattle, Kenney impactMetal_light | — |
| `feint_snap` | 2 | Kenney switch12, Kenney switch13, Kenney switch14, Kenney metalLatch, Trade Bot sa3_tradebot_tick, jug_steam_burst | — |
| `key_jam` | 2 | jug_grind_metal, jug_ratchet_hoist, jug_grinder, jug_weld, jug_stone | — |
| `plates_burst` | 2 | jug_stone, jug_gong, jug_anvil, BSB 1771, BSB 1772, BSB 1774, BSB 1776, BSB 1778, jug_whoosh | — |
| `stage_roar` | 2 | jug_steam_long, jug_steam_mid, jug_creak_low | swell, hum |
| `plates_land` | 3 | BSB 1771, BSB 1772, BSB 1774, BSB 1776, BSB 1778, jug_chain_clink | — |
| `hit_wound` | 4 | jug_gong, Kenney impactPlate_heavy, Trade Bot sa3_tradebot_tick | — |
| `hit_open` | 4 | Kenney impactPunch_heavy, jug_stone, jug_spring, jug_steam_burst | — |
| `lightning` | 1 | spark_shock, jug_weld, Kenney metalLatch | swell, hum, servo |
| `ember_flicker` | 3 | flicker_bulb | hum |
| `steam_wisp` | 2 | BSB 0227 | — |
| `key_turn` | 2 | Trade Bot sa3_tradebot_wind, jug_key_lock, jug_stone | — |
| `salute_snap` | 2 | Kenney metalLatch, jug_chime, Kenney impactBell_heavy, jug_steam_puff | — |
| `glow_out` | 1 | Trade Bot sa3_tradebot_tick | swell, hum |
| `kneel_impact` | 2 | jug_stone, BSB 1023, jug_gong, jug_chain_clink, jug_chain_rattle, Kenney impactMetal_light | — |
| `key_unwind` | 1 | jug_ratchet_hoist, jug_reel | servo |
| `mainspring_burst` | 1 | Kenney metalLatch, jug_stone, jug_spring, BSB 1950, jug_anvil | boing |
| `fist_thud` | 2 | jug_stone, Kenney impactPunch_heavy, jug_chain_clink, jug_chain_rattle, Kenney impactMetal_light | — |
| `death_steam` | 1 | BSB 0227, jug_creak_low | swell |
| `key_fit` | 2 | jug_key_lock, jug_anvil, jug_reel | — |
| `ritual_click` | 4 | Trade Bot sa3_tradebot_tick, Kenney metalClick | — |
| `ritual_tick` | 4 | jug_tick, Kenney impactMetal_light, jug_chain_clink | — |
| `ritual_runes` | 1 | jug_chime | swell, hum |
| `ritual_steam` | 3 | jug_steam_mid, jug_steam_puff, jug_steam_long | — |
| `ritual_ready` | 1 | jug_ratchet_hoist, jug_chime | swell, hum |
| `ritual_unfold` | 1 | jug_stone, BSB 1023, BSB 1774, BSB 1776, jug_ratchet_hoist, jug_steam_long | swell, hum |
| `ritual_collapse` | 1 | jug_stone, BSB 1772, jug_steam_long, jug_whoosh | — |
| `engine_ratchet` | 4 | Trade Bot sa3_tradebot_tick, Kenney metalClick | — |
| `engine_tick` | 3 | jug_tick | — |
| `engine_tock` | 3 | jug_tock | — |
| `engine_rune_up` | 1 | jug_chime | swell, hum |
| `engine_rune_down` | 1 |  | swell |
| `engine_full` | 1 | jug_key_lock, jug_chime | swell, hum |
| `engine_empty` | 1 | jug_creak_low, jug_stone | — |
