# Helper Bot: Sound Design

Audio identity for Helper Bot, the train-driving, shop-keeping robot block. The character comes from [the brief](../briefs/helper-bot.md). The cue times come from the clips in [helper-bot.motion.js](../models/helper-bot.motion.js).

- **Cues:** [helper-bot/helper-bot.cues.json](helper-bot/helper-bot.cues.json), written by [make-cues.js](helper-bot/make-cues.js).
- **Stage 1 board:** [helper-bot/helper-bot-soundboard.html](helper-bot/helper-bot-soundboard.html). To open it, serve the folder with `node ~/.claude/skills/character-sound-designer/scripts/serve.js design/sound/helper-bot 8767` and go to http://localhost:8767/helper-bot-soundboard.html.

**Status:** the user picked whistle **D** (the real locomotive, pitched up 2.2x) and lever **B** (vanilla copper trapdoor) on 2026-10-27. All 174 cues are in the Blockbench clips as sound keyframes (45 clips), written from the cue file by [helper-bot.clips.js](../models/helper-bot.clips.js). The build stops if a cued sound has no real file. The board can show the candidates again with `node make-cues.js --candidates`. Waiting on the user's listen in Blockbench.

## Identity
- **Mechanical:** Trade Bot's clockwork family, bolted to a swivel post. He has servos, a gimbal spring that the train rocks, a little brass steam whistle, the Train Controls' levers and a pocket watch. He has no wind-up key, so his idle doesn't tick. The tick is now his **pocket watch**.
- **Emotional:** proud, punctual and a bit fussy. He uses the family voice (Trade Bot's chirps and boops), sparsely: a trill when he's handed the schedule, a huff when someone else drives, a "huh?" when he's lost.
- **Movement:** he never travels. His motion sounds are servo whirs (the swivel, head turns), the spring's wobble (*boing … boink … tink*) on starts, stops and the duck, and lever clunks.
- **Materials:** brass (whistle, watch, cap badge, claws), steel (levers, clicks), rubber cord, andesite.
- **Mix:** small and close, under a moving train. He's quieter than the train and quieter than a villager. Voice cues get a long cooldown in game (spec D27).

## Signature sounds
1. **`whistle_toot`** (new): his two-tone brass steam whistle, toot-toot on departure. It's the sound players will learn him by.
2. **`lever_clunk`** (new): a hand on the Train Controls, on throttle and brake.
3. **`clock_tick`** (Trade Bot's): his pocket watch, ticking whenever it's out of his pocket.
4. **`focus_click`** (Trade Bot's): the family lenses refocusing, when he checks or notices.
5. **`merchant_trill`** (Trade Bot's): his proud, happy voice, used for the salute and the proud face.

## Personality → sound
| Trait | Sound |
|---|---|
| Proud | `merchant_trill` on the salute, `pleased_boop` after a smooth start, `brass_ping` when he takes a shop order |
| Punctual | the watch: grip, lid click, ticks, snap shut (`light_off`), drumming taps (`pat`) |
| Fussy | `grumble` huffs (manual driving, the wait), `dust_off` on the controls, the lens polish |
| Lost | `curious_chirp` → `huh` → `shrug` over the upside-down schedule card |

## New events

### whistle_toot
**Purpose** · The train's leaving and he's proud of it. This is his signature, and players should hear it as "Helper Bot".
**Sound Layers** · mechanical: a short steam chuff as the valve opens / material: a small brass whistle, two close tones (about a major third apart) / personality: a bright, clipped toot, cheerful rather than shrill.
**Timing** · valve hiss 0–0.05 s, tone 0.02–0.25 s, steam tail to 0.3 s. It's cued twice, 0.35 s apart (toot-toot).
**Variations** · 3 variants, ±3% pitch and ±8% gain. The second toot of a pair can be a variant.
**Implementation Notes** · One-shot from `depart`, on the cord-pull beats. Category NEUTRAL, 16 blocks. It must sit clearly under Create's own train whistle (a little whistle on a little robot).
**Source** · Rung 1 (vanilla) has no steam whistle; rung 2 (the project's library) has none. **Rung 3, BigSoundBank (CC0)**, has four candidates. Each is trimmed to one toot of about 0.3 s and pitched up into a small brass whistle's range:
- A: train whistle foley #5 (3319), up 1.6x.
- B: foley #3 (3317), up 1.5x.
- C: foley #1 (3315), up 3x.
- D: a real locomotive (0225), up 2.2x.

Every candidate has a quiet steam-hiss puff on top (hiss of steam train #1, 0227, high-passed). Level 0.5 puts it about 1 dB over Trade Bot's `brass_ping` in short-window loudness.

### whistle_peep
**Purpose** · A quick single peep: the train's honk passed through his own whistle, and his little three-peep tune.
**Sound Layers** · the same whistle as `whistle_toot`, cut short with one tone and a smaller hiss.
**Timing** · hiss 0–0.04 s, tone 0.01–0.12 s.
**Variations** · 4 variants, ±5% pitch, so the three-peep `toot_tune` has a little melody.
**Implementation Notes** · One-shot from `honk` and `toot_tune`. Category NEUTRAL, 16 blocks.
**Source** · Cut from the same recording as the chosen (D, the locomotive) `whistle_toot` (0.14 s of tone and a shorter hiss), so the two always match. Level 0.42.

### lever_clunk
**Purpose** · His hands really work the controls: throttle forward, brake back.
**Sound Layers** · mechanical: a lever's detent click / material: steel on a wooden and metal desk, with a soft low thud.
**Timing** · click 0–0.03 s, clunk to 0.12 s, thud tail to 0.17 s.
**Variations** · 3 variants, ±4% pitch.
**Implementation Notes** · One-shot from `throttle_up` and `brake`. Category NEUTRAL, 16 blocks.
**Source** · Three candidates:
- A: **rung 1**, vanilla `block.lever.click` (`random/click`). It's thin.
- B: **rung 1**, vanilla `block.copper_trapdoor.open`, pitched up 1.1x. A heavier metal clunk.
- C: **rung 2**, Kenney `metalLatch` with a soft `impactSoft_heavy_000` thud.

The vanilla options are referenced by event in game; the board plays copies extracted to the git-ignored `sources/vanilla-preview/`. All three sit level with his `grip` and `focus_click`.

## Reused events (Trade Bot's approved sounds)
Each reused event keeps Trade Bot's id and files, and gets its own Helper Bot subtitle. In game, `sounds.json` defines `helper_bot.<id>` pointing at the same `trade_bot/<id>` OGG files, so nothing is copied into the mod. Their layers, variants and sources are in [the Trade Bot sound design](trade-bot.md).

| Group | Events |
|---|---|
| Signature | `clock_tick` (now the pocket watch), `focus_click`, `merchant_trill`, `brass_ping` (shop order ding) |
| Movement | `servo_short`, `servo_snap` (swivel and head), `aerial_boing` (now the gimbal spring), `hop_small`, `land_soft` (duck settle), `land_heavy` (crash jolt), `blink` |
| Hands, watch and cap | `grip`, `claw_fidget`, `pat` (taps), `set_soft` (card into the clip), `dust_off` (wiping), `drop_whoosh` (blowing), `light_off` (the watch snaps shut), `light_on`, `flicker`, `poof_in` / `poof_out` (cap pops on and off), `wave_clink` |
| Voice and reactions | `bonk`, `dizzy`, `head_shake`, `grumble` (huff), `double_take`, `curious_chirp`, `puzzled_hmm`, `huh`, `shrug`, `startled_chirp`, `sad_aww`, `sigh`, `snore`, `sleepy_mmm`, `pleased_boop`, `hi_boop`, `bye_boop` |
| Scared | `scared_yelp`, `hide_tuck`, `tremble_loop`, `whimper`, `peek_click`, `relief_sigh`, `shake_off` |

Trade Bot's wheel, drawer, station, chest and trading sounds aren't used: he has none of those parts. The silent idle (no key ticking) is on purpose.

## Cue table
174 cues across 45 clips. The full list, with times, is in the cue file; the board plays each clip's cues at their real timings. Highlights:

| Clip | Cues |
|---|---|
| `depart` | 0.15 servo_snap · 0.5 grip · **0.62 whistle_toot · 0.97 whistle_toot** · 1.45 wave_clink · 1.55 bye_boop |
| `throttle_up` | 0.08 servo_snap · 0.4 aerial_boing (the train's pull on his spring) · **0.6 lever_clunk** · 0.95 pleased_boop · 1.4 grip |
| `brake` | 0.2 aerial_boing · 0.35 grip · **0.6 lever_clunk** · 0.65 grumble |
| `station_wait` | 0.35 grip · 1.2 focus_click (lid) · watch ticks at 1.4, 1.9, 2.4, 3.0 and 3.5 · 2.05 pat · 3.95 grumble · 4.05 light_off (snap shut) · 4.4 grip · drumming pats at 4.95, 5.2 and 5.45 |
| `schedule_given` | 0.2 set_soft · 0.22 servo_snap · 0.3 aerial_boing · **0.45 poof_in (cap)** · 0.85 wave_clink · 0.9 merchant_trill · 1.85 grip |
| `no_path` | 0.4 grip · 0.8 focus_click · 1.3 claw_fidget · 1.45 curious_chirp · 2.0 claw_fidget · 2.2 huh · 2.6 / 2.95 servo_short · 3.3 shrug · 3.75 set_soft |
| `crash` | 0.12 bonk · 0.14 land_heavy · 0.25 aerial_boing · 0.5 dizzy · 2.0 head_shake · 2.55 sad_aww |
| `shop_order_nod` | 0.15 servo_short · **0.2 brass_ping** · 0.45 servo_short · 0.95 pleased_boop |
| `toot_tune` | 0.5 grip · **whistle_peep at 0.72, 1.17 and 1.62** · 2.2 pleased_boop |

Clips with no cues: `pose_sheet` and `expression_sheet` (review sheets).
