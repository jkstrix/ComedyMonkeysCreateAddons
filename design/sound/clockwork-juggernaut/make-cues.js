// Writes clockwork-juggernaut.cues.json: the Clockwork Juggernaut's sound events (placeholder recipes) and every clip's
// cues. Cue times come from design/models/clockwork-juggernaut.clips.js and juggernaut-props.gen.js, measured where the
// motion is springy (foot contacts, claw shuts, key ticks): see design/sound/clockwork-juggernaut.md.
// He is huge, old and grim: every family sound (Trade Bot's tick, key ratchet, brass) comes back an octave or two
// lower and heavier, and steam replaces the family's voice.
const fs = require('fs');
const path = require('path');

const E = (subtitle, group, layers) => ({ subtitle, group, layers });
// signature sounds as layer stacks, so events can reuse them at an offset
const TICK = (at = 0, g = 1) => [
  { type: 'tick', at, gain: 0.55 * g, f: 1150, dur: 0.05 },
  { type: 'thud', at: at + 0.003, gain: 0.5 * g, f: 62, dur: 0.22 },
  { type: 'clink', at: at + 0.006, gain: 0.18 * g, f: 640, dur: 0.35 },
];
const TOCK = (at = 0, g = 1) => [
  { type: 'tick', at, gain: 0.5 * g, f: 900, dur: 0.05 },
  { type: 'thud', at: at + 0.003, gain: 0.55 * g, f: 52, dur: 0.25 },
  { type: 'clink', at: at + 0.006, gain: 0.15 * g, f: 520, dur: 0.4 },
];
const PLATE = (at = 0, g = 1, f = 190) => [
  { type: 'clang', at, gain: 0.6 * g, f, dur: 1.4 },
  { type: 'ping', at: at + 0.004, gain: 0.12 * g, f: f * 3.1, dur: 0.9 },
];
const RATTLE = (at = 0, g = 1) => [
  { type: 'clink', at, gain: 0.22 * g, f: 700, dur: 0.2 },
  { type: 'clink', at: at + 0.07, gain: 0.16 * g, f: 820, dur: 0.18 },
  { type: 'ratchet', at: at + 0.03, gain: 0.18 * g, rate: 16, f: 1300, dur: 0.3 },
  { type: 'clink', at: at + 0.2, gain: 0.08 * g, f: 760, dur: 0.15 },
];
const HISS = (at, dur, g = 1, f = 5200) => [{ type: 'noise', at, gain: 0.45 * g, f, dur }];
const RUNE = (at, dur, g = 1, f = 98) => [
  { type: 'swell', at, gain: 0.35 * g, f, f2: f * 1.5, dur },
  { type: 'hum', at: at + dur * 0.2, gain: 0.25 * g, f: f * 2, dur: dur * 0.8 },
];
const STEP = (at = 0, g = 1) => [
  { type: 'thud', at, gain: 0.85 * g, f: 44, dur: 0.4 },
  { type: 'clack', at: at + 0.005, gain: 0.25 * g, f: 300, dur: 0.1 },
  ...RATTLE(at + 0.06, 0.55 * g),
];

const events = {
  // ---------- signature sounds ----------
  tick: E('Juggernaut ticks', 'Signature sounds', TICK()),
  tock: E('Juggernaut ticks', 'Signature sounds', TOCK()),
  run_down_tick: E('Juggernaut winds down', 'Signature sounds', [
    ...TOCK(0, 1.1),
    { type: 'creak', at: 0.05, gain: 0.45, f: 34, dur: 0.6 },
    { type: 'boing', at: 0.04, gain: 0.18, f: 70, wobble: 0.25, dur: 0.6 },
  ]),
  key_ratchet: E('Juggernaut\'s key turns', 'Signature sounds', [
    { type: 'ratchet', at: 0, gain: 0.6, rate: 9, f: 950, dur: 0.6 },
    { type: 'thud', at: 0.6, gain: 0.35, f: 70, dur: 0.15 },
  ]),
  plate_ring: E('Juggernaut\'s armour rings', 'Signature sounds', PLATE()),
  glow_on: E('Juggernaut\'s runes glow', 'Signature sounds', [...RUNE(0, 1.2), { type: 'sparkle', at: 0.25, gain: 0.06, f: 1800 }]),
  steam_puff: E('Juggernaut vents steam', 'Signature sounds', [...HISS(0, 0.45, 0.8), { type: 'whoosh', at: 0, gain: 0.25, f: 200, f2: 900, dur: 0.4 }]),

  // ---------- movement ----------
  step: E('Juggernaut stomps', 'Movement', STEP(0, 0.5)),
  step_run: E('Juggernaut thunders', 'Movement', [...STEP(0, 0.65), { type: 'thud', at: 0.01, gain: 0.2, f: 30, dur: 0.5 }]),
  foot_scrape: E('Juggernaut scrapes the ground', 'Movement', [
    { type: 'scrape', at: 0, gain: 0.5, f: 500, dur: 0.22 },
    { type: 'thud', at: 0.02, gain: 0.3, f: 60, dur: 0.15 },
  ]),
  skid: E('Juggernaut skids', 'Movement', [
    { type: 'scrape', at: 0, gain: 0.55, f: 420, dur: 0.45 },
    { type: 'thud', at: 0.38, gain: 0.5, f: 50, dur: 0.3 },
    ...RATTLE(0.42, 0.8),
  ]),
  plates_settle: E('Juggernaut\'s plates rattle', 'Movement', RATTLE(0, 1.2)),
  joint_step: E('Juggernaut\'s joints step', 'Movement', [
    { type: 'tick', at: 0, gain: 0.35, f: 800, dur: 0.04 },
    { type: 'creak', at: 0.01, gain: 0.35, f: 40, dur: 0.18 },
  ]),
  head_ratchet: E('Juggernaut turns his head', 'Movement', [
    { type: 'ratchet', at: 0, gain: 0.35, rate: 18, f: 1100, dur: 0.35 },
    { type: 'creak', at: 0, gain: 0.2, f: 60, dur: 0.35 },
  ]),
  joint_creak: E('Juggernaut creaks', 'Movement', [{ type: 'creak', at: 0, gain: 0.45, f: 32, dur: 0.5 }]),
  knuckle_grind: E('Juggernaut grinds his fist', 'Movement', [
    { type: 'scrape', at: 0, gain: 0.35, f: 380, dur: 0.4 },
    { type: 'ratchet', at: 0.05, gain: 0.25, rate: 12, f: 900, dur: 0.3 },
  ]),
  claw_flex: E('Juggernaut flexes his claw', 'Movement', [{ type: 'ratchet', at: 0, gain: 0.3, rate: 14, f: 1300, dur: 0.22 }, { type: 'clink', at: 0.22, gain: 0.15, f: 900 }]),
  steam_sigh: E('Juggernaut sighs steam', 'Movement', [
    { type: 'creak', at: 0, gain: 0.35, f: 30, dur: 0.6 },
    ...HISS(0.6, 0.9, 0.7, 3800),
    { type: 'swell', at: 0.6, gain: 0.12, f: 140, f2: 90, dur: 0.9 },
  ]),

  // ---------- attacks ----------
  charge_tell: E('Juggernaut lowers his head', 'Attacks', [
    { type: 'clack', at: 0, gain: 0.45, f: 500, dur: 0.08 },
    { type: 'noise', at: 0.02, gain: 0.5, f: 2200, dur: 0.3 },
    { type: 'creak', at: 0.05, gain: 0.3, f: 36, dur: 0.4 },
  ]),
  swing_windup: E('Juggernaut winds up a swing', 'Attacks', [
    { type: 'creak', at: 0, gain: 0.45, f: 30, dur: 0.4 },
    { type: 'ratchet', at: 0.05, gain: 0.25, rate: 10, f: 850, dur: 0.3 },
  ]),
  swing_whoosh: E('Juggernaut swings', 'Attacks', [
    { type: 'whoosh', at: 0, gain: 0.7, f: 120, f2: 520, dur: 0.38 },
    { type: 'ratchet', at: 0.12, gain: 0.2, rate: 22, f: 1100, dur: 0.35 },
  ]),
  stomp_raise: E('Juggernaut raises his foot', 'Attacks', [
    { type: 'creak', at: 0, gain: 0.45, f: 28, dur: 0.5 },
    ...HISS(0.25, 0.25, 0.3),
  ]),
  stomp_impact: E('Juggernaut stomps a shockwave', 'Attacks', [
    { type: 'thud', at: 0, gain: 1.0, f: 36, dur: 0.9 },
    { type: 'bump', at: 0, gain: 0.6, f: 80, f2: 30, dur: 0.5 },
    { type: 'noise', at: 0.01, gain: 0.35, f: 700, dur: 0.5 },
    ...PLATE(0.03, 0.55, 170),
    ...RATTLE(0.12, 0.9),
  ]),
  grab_open: E('Juggernaut opens his claw', 'Attacks', [
    { type: 'ratchet', at: 0, gain: 0.4, rate: 16, f: 1000, dur: 0.4 },
    { type: 'creak', at: 0.05, gain: 0.3, f: 45, dur: 0.4 },
  ]),
  grab_snap: E('Juggernaut grabs', 'Attacks', [
    { type: 'clack', at: 0, gain: 0.6, f: 420, dur: 0.1 },
    { type: 'thud', at: 0.005, gain: 0.55, f: 80, dur: 0.2 },
    { type: 'clink', at: 0.01, gain: 0.25, f: 600, dur: 0.3 },
  ]),
  grab_clang: E('Juggernaut\'s claw clangs shut', 'Attacks', [
    ...PLATE(0, 0.9, 260),
    { type: 'clack', at: 0, gain: 0.5, f: 520, dur: 0.08 },
    ...RATTLE(0.1, 0.7),
  ]),
  throw_whoosh: E('Juggernaut hurls', 'Attacks', [
    { type: 'whoosh', at: 0, gain: 0.7, f: 140, f2: 700, dur: 0.45 },
    ...HISS(0.25, 0.3, 0.4, 3000),
  ]),
  grab_release: E('Juggernaut lets go', 'Attacks', [{ type: 'ratchet', at: 0, gain: 0.3, rate: 20, f: 1200, dur: 0.18 }, { type: 'clink', at: 0.18, gain: 0.15, f: 800 }]),
  leap_coil: E('Juggernaut coils his spring', 'Attacks', [
    { type: 'creak', at: 0, gain: 0.45, f: 26, dur: 1.4 },
    { type: 'ratchet', at: 0.1, gain: 0.25, rate: 8, f: 900, dur: 0.5 },
    { type: 'ratchet', at: 0.6, gain: 0.35, rate: 18, f: 1000, dur: 0.45 },
    { type: 'ratchet', at: 1.05, gain: 0.45, rate: 32, f: 1100, dur: 0.4 },
    ...HISS(0.95, 0.5, 0.45),
  ]),
  leap_launch: E('Juggernaut leaps', 'Attacks', [
    { type: 'boing', at: 0, gain: 0.6, f: 75, wobble: 0.45, dur: 0.9 },
    { type: 'whoosh', at: 0.03, gain: 0.55, f: 110, f2: 600, dur: 0.55 },
    { type: 'thud', at: 0, gain: 0.5, f: 50, dur: 0.3 },
  ]),
  leap_land: E('Juggernaut slams down', 'Attacks', [
    { type: 'thud', at: 0, gain: 1.0, f: 30, dur: 1.2 },
    { type: 'bump', at: 0, gain: 0.7, f: 70, f2: 25, dur: 0.7 },
    { type: 'noise', at: 0.01, gain: 0.45, f: 600, dur: 0.8 },
    ...PLATE(0.03, 0.7, 150),
    ...RATTLE(0.15, 1.1),
    ...RATTLE(0.45, 0.6),
  ]),
  vent_tell: E('Juggernaut builds up steam', 'Attacks', [
    ...RUNE(0.15, 1.8, 1.1, 82),
    { type: 'noise', at: 0.4, gain: 0.12, f: 4500, dur: 0.6 },
    { type: 'noise', at: 0.9, gain: 0.22, f: 4500, dur: 0.6 },
    { type: 'noise', at: 1.4, gain: 0.35, f: 4500, dur: 0.55 },
    { type: 'creak', at: 0, gain: 0.3, f: 32, dur: 0.5 },
  ]),
  vent_blast: E('Juggernaut blasts steam', 'Attacks', [
    { type: 'noise', at: 0, gain: 0.6, f: 1600, dur: 0.95 },
    { type: 'noise', at: 0, gain: 0.45, f: 6500, dur: 0.85 },
    { type: 'whoosh', at: 0, gain: 0.5, f: 90, f2: 400, dur: 0.9 },
    { type: 'noise', at: 0.85, gain: 0.2, f: 2500, dur: 0.12 },
    { type: 'noise', at: 1.0, gain: 0.12, f: 2500, dur: 0.1 },
  ]),
  spin_windup: E('Juggernaut winds up a spin', 'Attacks', [
    { type: 'creak', at: 0, gain: 0.45, f: 28, dur: 0.9 },
    { type: 'ratchet', at: 0, gain: 0.25, rate: 8, f: 900, dur: 0.7 },
    { type: 'ratchet', at: 0.7, gain: 0.35, rate: 20, f: 1000, dur: 0.5 },
    { type: 'servo', at: 0.9, gain: 0.45, f: 60, f2: 160, dur: 0.9 },
    { type: 'whoosh', at: 1.0, gain: 0.5, f: 120, f2: 500, dur: 0.5 },
  ]),
  spin_loop: E('Juggernaut spins', 'Attacks', [
    { type: 'servo', at: 0, gain: 0.35, f: 150, f2: 160, dur: 1.0 },
    { type: 'whoosh', at: 0, gain: 0.45, f: 120, f2: 520, dur: 0.48 },
    { type: 'whoosh', at: 0.5, gain: 0.45, f: 120, f2: 520, dur: 0.48 },
  ]),
  spin_down: E('Juggernaut spins down', 'Attacks', [
    { type: 'servo', at: 0, gain: 0.4, f: 160, f2: 45, dur: 1.0 },
    { type: 'whoosh', at: 0, gain: 0.35, f: 120, f2: 420, dur: 0.6 },
    { type: 'thud', at: 1.0, gain: 0.55, f: 50, dur: 0.3 },
    ...RATTLE(1.05, 0.8),
  ]),
  cog_draw: E('Juggernaut draws a cog', 'Attacks', [
    { type: 'scrape', at: 0, gain: 0.35, f: 1100, dur: 0.18 },
    { type: 'clink', at: 0.16, gain: 0.3, f: 560, dur: 0.4 },
  ]),
  cog_throw: E('Juggernaut throws a cog', 'Attacks', [
    { type: 'whoosh', at: 0, gain: 0.6, f: 150, f2: 650, dur: 0.35 },
    ...HISS(0.1, 0.35, 0.55, 3200),
  ]),
  cog_fly: E('A cog whirls', 'Attacks', [{ type: 'servo', at: 0, gain: 0.35, f: 210, f2: 200, dur: 1.0 }, { type: 'whoosh', at: 0, gain: 0.25, f: 300, f2: 700, dur: 0.25 }, { type: 'whoosh', at: 0.25, gain: 0.25, f: 300, f2: 700, dur: 0.25 }, { type: 'whoosh', at: 0.5, gain: 0.25, f: 300, f2: 700, dur: 0.25 }, { type: 'whoosh', at: 0.75, gain: 0.25, f: 300, f2: 700, dur: 0.25 }]),
  cog_hit: E('A cog crashes', 'Attacks', [
    { type: 'clang', at: 0, gain: 0.6, f: 300, dur: 0.8 },
    { type: 'thud', at: 0, gain: 0.5, f: 70, dur: 0.25 },
    { type: 'clink', at: 0.18, gain: 0.25, f: 650, dur: 0.25 },
    { type: 'clink', at: 0.32, gain: 0.12, f: 690, dur: 0.2 },
  ]),

  // ---------- the rhythm: winding, rewinding, feints, jams, stages ----------
  rewind_loop: E('Juggernaut rewinds', 'Rhythm', [
    { type: 'ratchet', at: 0, gain: 0.55, rate: 24, f: 950, dur: 1.0 },
    { type: 'hum', at: 0, gain: 0.12, f: 98, dur: 1.0 },
    { type: 'swell', at: 0.05, gain: 0.08, f: 196, f2: 220, dur: 0.4 },
    { type: 'swell', at: 0.55, gain: 0.08, f: 196, f2: 220, dur: 0.4 },
  ]),
  rewind_end: E('Juggernaut is wound', 'Rhythm', [
    { type: 'ratchet', at: 0, gain: 0.45, rate: 14, f: 950, dur: 0.3 },
    { type: 'clack', at: 0.3, gain: 0.6, f: 380, dur: 0.1 },
    { type: 'thud', at: 0.3, gain: 0.5, f: 55, dur: 0.25 },
    ...HISS(0.35, 0.4, 0.5),
    ...RATTLE(0.38, 0.6),
  ]),
  feint_snap: E('Juggernaut snaps awake', 'Rhythm', [
    { type: 'click', at: 0, gain: 0.9, dur: 0.02 },
    { type: 'tick', at: 0, gain: 0.7, f: 2400, dur: 0.04 },
    { type: 'clack', at: 0.01, gain: 0.5, f: 600, dur: 0.06 },
    { type: 'noise', at: 0.04, gain: 0.45, f: 2500, dur: 0.25 },
  ]),
  key_jam: E('Juggernaut\'s key jams', 'Rhythm', [
    { type: 'scrape', at: 0.05, gain: 0.55, f: 900, dur: 0.95 },
    { type: 'ratchet', at: 0.05, gain: 0.35, rate: 30, f: 700, dur: 0.9 },
    { type: 'rustle', at: 0.1, gain: 0.35, density: 40, dur: 0.22 },
    { type: 'rustle', at: 0.5, gain: 0.3, density: 40, dur: 0.14 },
    { type: 'rustle', at: 0.8, gain: 0.25, density: 40, dur: 0.1 },
    { type: 'thud', at: 0.18, gain: 0.45, f: 60, dur: 0.2 },
    { type: 'thud', at: 0.58, gain: 0.4, f: 60, dur: 0.2 },
  ]),
  plates_burst: E('Juggernaut sheds his armour', 'Rhythm', [
    { type: 'thud', at: 0, gain: 0.55, f: 55, dur: 0.3 },
    ...PLATE(0.06, 0.8, 200),
    ...PLATE(0.11, 0.6, 235),
    ...PLATE(0.17, 0.5, 175),
    { type: 'whoosh', at: 0.08, gain: 0.3, f: 200, f2: 700, dur: 0.4 },
  ]),
  stage_roar: E('Juggernaut roars steam', 'Rhythm', [
    { type: 'noise', at: 0, gain: 0.8, f: 1300, dur: 0.75 },
    { type: 'noise', at: 0.05, gain: 0.4, f: 6000, dur: 0.65 },
    ...RUNE(0, 0.8, 0.9, 73),
    { type: 'creak', at: 0, gain: 0.35, f: 26, dur: 0.6 },
  ]),
  plates_land: E('Armour plates clatter', 'Rhythm', [
    { type: 'clang', at: 0, gain: 0.35, f: 230, dur: 0.6 },
    { type: 'clang', at: 0.13, gain: 0.3, f: 190, dur: 0.6 },
    { type: 'clink', at: 0.25, gain: 0.2, f: 640, dur: 0.2 },
    { type: 'clang', at: 0.3, gain: 0.2, f: 260, dur: 0.5 },
    { type: 'clink', at: 0.42, gain: 0.12, f: 700, dur: 0.15 },
  ]),

  // ---------- reactions ----------
  hit_wound: E('Juggernaut shrugs off a blow', 'Reactions', [...PLATE(0, 0.8, 210), { type: 'tick', at: 0.06, gain: 0.25, f: 1100, dur: 0.04 }]),
  hit_open: E('Juggernaut is hurt', 'Reactions', [
    { type: 'thud', at: 0, gain: 0.6, f: 58, dur: 0.35 },
    { type: 'clack', at: 0, gain: 0.4, f: 350, dur: 0.1 },
    { type: 'boing', at: 0.02, gain: 0.35, f: 110, wobble: 0.5, dur: 0.6 },
    { type: 'noise', at: 0.03, gain: 0.25, f: 2500, dur: 0.2 },
  ]),
  lightning: E('Juggernaut is struck by lightning', 'Reactions', [
    { type: 'rustle', at: 0, gain: 0.55, density: 50, dur: 0.5 },
    ...RUNE(0, 0.7, 1.2, 110),
    { type: 'servo', at: 0.1, gain: 0.35, f: 90, f2: 220, dur: 0.6 },
    { type: 'clack', at: 0.12, gain: 0.45, f: 420, dur: 0.08 },
  ]),

  // ---------- rest, waking, death ----------
  ember_flicker: E('Juggernaut stirs in his sleep', 'Rest, waking and death', [{ type: 'hum', at: 0, gain: 0.12, f: 98, dur: 0.25 }, { type: 'tick', at: 0.02, gain: 0.08, f: 900, dur: 0.03 }]),
  steam_wisp: E('Steam wisps', 'Rest, waking and death', HISS(0, 0.9, 0.25, 3500)),
  key_turn: E('Juggernaut\'s key turns', 'Rest, waking and death', [
    { type: 'ratchet', at: 0, gain: 0.45, rate: 10, f: 900, dur: 0.3 },
    { type: 'clack', at: 0.3, gain: 0.55, f: 360, dur: 0.1 },
    { type: 'thud', at: 0.3, gain: 0.45, f: 55, dur: 0.25 },
  ]),
  salute_snap: E('Juggernaut salutes', 'Rest, waking and death', [
    { type: 'clack', at: 0, gain: 0.6, f: 480, dur: 0.08 },
    { type: 'thud', at: 0, gain: 0.35, f: 90, dur: 0.15 },
    { type: 'ping', at: 0.01, gain: 0.25, f: 740, dur: 0.9 },
    ...HISS(0.12, 0.3, 0.3),
  ]),
  glow_out: E('Juggernaut\'s runes fade', 'Rest, waking and death', [
    { type: 'swell', at: 0, gain: 0.3, f: 120, f2: 60, dur: 1.0 },
    { type: 'tick', at: 0, gain: 0.15, f: 900, dur: 0.03 },
  ]),
  kneel_impact: E('Juggernaut falls to his knee', 'Rest, waking and death', [
    { type: 'thud', at: 0, gain: 0.9, f: 40, dur: 0.8 },
    { type: 'noise', at: 0.01, gain: 0.3, f: 600, dur: 0.4 },
    ...PLATE(0.03, 0.5, 165),
    ...RATTLE(0.12, 1),
  ]),
  key_unwind: E('Juggernaut\'s key spins free', 'Rest, waking and death', [
    { type: 'ratchet', at: 0, gain: 0.5, rate: 40, f: 900, dur: 0.35 },
    { type: 'ratchet', at: 0.35, gain: 0.4, rate: 22, f: 850, dur: 0.3 },
    { type: 'servo', at: 0, gain: 0.3, f: 240, f2: 80, dur: 0.7 },
  ]),
  mainspring_burst: E('The Mainspring bursts out', 'Rest, waking and death', [
    { type: 'clack', at: 0, gain: 0.7, f: 380, dur: 0.12 },
    { type: 'thud', at: 0, gain: 0.5, f: 60, dur: 0.3 },
    { type: 'boing', at: 0.01, gain: 0.85, f: 105, wobble: 0.6, dur: 1.5 },
    { type: 'boing', at: 0.02, gain: 0.3, f: 210, wobble: 0.5, dur: 1.1 },
    { type: 'ping', at: 0.02, gain: 0.15, f: 1050, dur: 1.0 },
  ]),
  fist_thud: E('Juggernaut slumps', 'Rest, waking and death', [{ type: 'thud', at: 0, gain: 0.5, f: 45, dur: 0.5 }, ...RATTLE(0.08, 0.6)]),
  death_steam: E('Juggernaut lets out his last steam', 'Rest, waking and death', [
    ...HISS(0, 1.4, 0.55, 3000),
    { type: 'swell', at: 0, gain: 0.15, f: 120, f2: 55, dur: 1.4 },
    { type: 'creak', at: 0.4, gain: 0.25, f: 24, dur: 0.6 },
  ]),

  // ---------- the ritual ----------
  key_fit: E('A Winding Key is fitted', 'The ritual', [
    { type: 'clack', at: 0, gain: 0.6, f: 420, dur: 0.1 },
    { type: 'clink', at: 0.01, gain: 0.35, f: 620, dur: 0.5 },
    { type: 'ratchet', at: 0.12, gain: 0.25, rate: 20, f: 1100, dur: 0.12 },
  ]),
  ritual_click: E('The effigy\'s key clicks', 'The ritual', [{ type: 'tick', at: 0, gain: 0.35, f: 1500, dur: 0.04 }, { type: 'clink', at: 0.003, gain: 0.1, f: 700, dur: 0.15 }]),
  ritual_tick: E('The effigy ticks', 'The ritual', [...TICK(0, 0.9), ...RATTLE(0.02, 0.45)]),
  ritual_runes: E('The effigy\'s runes glow', 'The ritual', RUNE(0, 1.0, 0.6, 110)),
  ritual_steam: E('Steam escapes the effigy', 'The ritual', HISS(0, 0.6, 0.35, 4000)),
  ritual_ready: E('The effigy is fully wound', 'The ritual', [
    ...RUNE(0, 0.8, 1.0, 98),
    { type: 'ratchet', at: 0, gain: 0.35, rate: 30, f: 1100, dur: 0.6 },
    { type: 'ping', at: 0.1, gain: 0.2, f: 588, dur: 1.0 },
  ]),
  ritual_unfold: E('The effigy unfolds', 'The ritual', [
    { type: 'thud', at: 0, gain: 0.8, f: 45, dur: 0.6 },
    ...RUNE(0, 0.6, 1.0, 98),
    { type: 'clang', at: 0.05, gain: 0.35, f: 230, dur: 0.6 },
    { type: 'clang', at: 0.17, gain: 0.3, f: 200, dur: 0.6 },
    { type: 'ratchet', at: 0.05, gain: 0.35, rate: 22, f: 1000, dur: 0.5 },
    ...HISS(0.1, 0.8, 0.6),
  ]),
  ritual_collapse: E('The effigy collapses into steam', 'The ritual', [
    { type: 'thud', at: 0, gain: 0.7, f: 50, dur: 0.5 },
    { type: 'clang', at: 0.02, gain: 0.3, f: 210, dur: 0.7 },
    { type: 'noise', at: 0.05, gain: 0.6, f: 2500, dur: 1.4 },
    { type: 'whoosh', at: 0.1, gain: 0.4, f: 150, f2: 800, dur: 1.2 },
  ]),

  // ---------- the Spring Engine ----------
  engine_ratchet: E('Spring Engine winds', 'Spring Engine', [{ type: 'tick', at: 0, gain: 0.3, f: 1700, dur: 0.035 }, { type: 'clink', at: 0.003, gain: 0.08, f: 900, dur: 0.12 }]),
  engine_tick: E('Spring Engine ticks', 'Spring Engine', [{ type: 'tick', at: 0, gain: 0.28, f: 1500, dur: 0.035 }, { type: 'thud', at: 0, gain: 0.15, f: 110, dur: 0.08 }]),
  engine_tock: E('Spring Engine ticks', 'Spring Engine', [{ type: 'tick', at: 0, gain: 0.28, f: 1200, dur: 0.035 }, { type: 'thud', at: 0, gain: 0.15, f: 95, dur: 0.08 }]),
  engine_rune_up: E('Spring Engine fills', 'Spring Engine', RUNE(0, 0.6, 0.35, 147)),
  engine_rune_down: E('Spring Engine empties', 'Spring Engine', [{ type: 'swell', at: 0, gain: 0.12, f: 147, f2: 110, dur: 0.6 }]),
  engine_full: E('Spring Engine is fully wound', 'Spring Engine', [
    { type: 'clack', at: 0, gain: 0.4, f: 500, dur: 0.06 },
    { type: 'ping', at: 0.01, gain: 0.2, f: 784, dur: 0.8 },
    ...RUNE(0, 0.6, 0.35, 196),
  ]),
  engine_empty: E('Spring Engine runs down', 'Spring Engine', [
    { type: 'creak', at: 0, gain: 0.3, f: 40, dur: 0.4 },
    { type: 'thud', at: 0.35, gain: 0.35, f: 70, dur: 0.2 },
  ]),
};

// ---------- cues (seconds into each clip) ----------
const range = (n, f) => Array.from({ length: n }, (_, k) => f(k));
// the effigy's key: angle 90s + 40s² degrees; the pawl clicks every 30° (a collar tooth), a heavy tick every 90°
const keyAt = (deg) => (-90 + Math.sqrt(90 * 90 + 4 * 40 * deg)) / 80;
const r2 = (x) => Math.round(x * 100) / 100;
const ritualClicks = [], ritualTicks = [];
for (let d = 30; keyAt(d) < 6; d += 30) (d % 90 ? ritualClicks : ritualTicks).push(r2(keyAt(d)));

const clips = {
  idle: [[0, 'tick'], [0.75, 'tock'], [1.5, 'tick'], [1.45, 'claw_flex'], [2.25, 'tock']],
  walk: [[0, 'tick'], [0.4, 'step'], [0.8, 'tock'], [1.2, 'step']],
  scan: [[0, 'tick'], [0.29, 'head_ratchet'], [0.75, 'tock'], [1.16, 'head_ratchet'], [1.5, 'tick'], [1.99, 'head_ratchet'], [2.25, 'tock']],
  knuckles: [[0, 'tick'], [0.25, 'joint_creak'], [0.75, 'tock'], [0.9, 'knuckle_grind'], [1.3, 'knuckle_grind'], [1.5, 'tick']],
  sigh: [[0, 'tick'], [0.2, 'steam_sigh'], [1.5, 'tock']],
  charge: [[0.05, 'charge_tell'], [0.12, 'foot_scrape'], [0.32, 'foot_scrape'], [0.45, 'steam_puff'], [0.75, 'step_run'], [1.0, 'step_run'], [1.25, 'step_run'], [1.5, 'step_run'], [1.62, 'skid'], [1.95, 'plates_settle']],
  swing: [[0.02, 'swing_windup'], [0.42, 'swing_whoosh']],
  stomp: [[0.1, 'stomp_raise'], [0.73, 'stomp_impact']],
  stomp_double: [[0.1, 'stomp_raise'], [0.72, 'stomp_impact'], [1.27, 'stomp_impact']],
  grab_throw: [[0.05, 'grab_open'], [1.22, 'grab_snap'], [1.55, 'throw_whoosh'], [1.95, 'grab_release'], [2.1, 'plates_settle']],
  grab_blocked: [[0.02, 'grab_open'], [0.6, 'grab_clang']],
  leap: [[0.05, 'leap_coil'], [1.5, 'leap_launch'], [2.08, 'leap_land']],
  steam_vent: [[0.05, 'vent_tell'], [2.0, 'vent_blast']],
  gear_spin_start: [[0.2, 'spin_windup']],
  gear_spin: [[0, 'spin_loop']],
  gear_spin_end: [[0, 'spin_down']],
  cog_throw: [[0.45, 'cog_draw'], [0.8, 'cog_throw']],
  run_down: [[0.45, 'run_down_tick'], [1.35, 'run_down_tick'], [2.36, 'run_down_tick'], [3.56, 'run_down_tick']],
  rewind: [[0, 'rewind_loop']],
  rewind_end: [[0, 'rewind_end']],
  feint: [[0.44, 'run_down_tick'], [0.99, 'run_down_tick'], [1.2, 'feint_snap'], [1.25, 'swing_whoosh']],
  key_jam: [[0, 'key_jam']],
  shed_plates_2: [[0.12, 'plates_burst'], [0.7, 'stage_roar'], [1.15, 'plates_land']],
  shed_plates_3: [[0.12, 'plates_burst'], [0.7, 'stage_roar'], [1.15, 'plates_land']],
  hit_wound: [[0, 'hit_wound']],
  hit_open: [[0, 'hit_open']],
  lightning: [[0, 'lightning']],
  dormant: [[1.3, 'ember_flicker'], [2.0, 'steam_wisp'], [3.1, 'ember_flicker']],
  wake: [[0.2, 'glow_on'], [0.55, 'key_turn'], [0.85, 'joint_step'], [1.05, 'joint_step'], [1.25, 'joint_step'], [1.45, 'joint_step'], [1.75, 'plates_settle'], [1.9, 'key_turn'], [2.3, 'salute_snap']],
  wake_ritual: [[0, 'ritual_unfold'], [0.2, 'key_ratchet'], [0.35, 'glow_on'], [0.9, 'steam_puff'], [1.1, 'plates_settle'], [2.05, 'salute_snap']],
  death: [[0.34, 'run_down_tick'], [0.84, 'run_down_tick'], [1.4, 'glow_out'], [2.25, 'kneel_impact'], [2.6, 'key_unwind'], [2.9, 'mainspring_burst'], [3.15, 'fist_thud'], [3.35, 'death_steam']],
  salute: [[0.45, 'salute_snap']],
  // props (juggernaut-props.bbmodel)
  ritual_winding: [...ritualClicks.map((t) => [t, 'ritual_click']), ...ritualTicks.map((t) => [t, 'ritual_tick']), [0.6, 'ritual_runes'], [2.5, 'ritual_steam'], [4.4, 'ritual_steam'], [5.2, 'ritual_ready']],
  engine_winding: [...range(24, (k) => [r2(k / 3), 'engine_ratchet']), ...[1, 3, 5, 7].map((t) => [t, 'engine_rune_up'])],
  engine_unwinding: [...range(32, (k) => [k / 4, k % 2 ? 'engine_tock' : 'engine_tick']), ...[1, 3, 5, 7].map((t) => [t, 'engine_rune_down'])],
};
for (const list of Object.values(clips)) list.sort((a, b) => a[0] - b[0]);

// Stage 2: the real recipes, each at the level levels.js measured for its loudness target
const NS = 'clockwork_juggernaut';
const REAL = require('./real-recipes.js');
let levels = {};
try { levels = JSON.parse(fs.readFileSync(path.join(__dirname, 'levels.json'), 'utf8')); } catch (e) { /* first build */ }
for (const [id, r] of Object.entries(REAL)) {
  if (!events[id]) throw new Error('real recipe for unknown event ' + id);
  events[id].real = { ...r, level: levels[`${NS}.${id}`] ?? r.level ?? 0.5 };
}

const out = { namespace: NS, events, clips };
fs.writeFileSync(path.join(__dirname, 'clockwork-juggernaut.cues.json'), JSON.stringify(out, null, 1));
const used = new Set(Object.values(clips).flat().map((c) => c[1]));
console.log(Object.keys(events).length + ' events, ' + Object.keys(clips).length + ' clips; not cued in any clip: ' + Object.keys(events).filter((id) => !used.has(id)).join(', '));
