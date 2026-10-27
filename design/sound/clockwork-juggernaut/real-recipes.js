// The Clockwork Juggernaut's real sounds (Stage 2): for each event, the recordings it's layered from (rung 2 and 3:
// the project's Kenney packs, Trade Bot's approved tick and wind takes, BigSoundBank CC0 cuts from cuts.js) and the few
// synth layers no recording covers (rune hum, sub-whoosh, spring wobble). `target` is the loudness the event sits at
// (max momentary LUFS for one-shots, integrated for loops), from the table in clockwork-juggernaut.md; levels.js turns
// it into each recipe's `level`. Pitch is a resample factor: 0.5 is an octave down.
const k = (stem, n, from = 0) => Array.from({ length: n }, (_, i) => `${stem}_${String(i + from).padStart(3, '0')}`);
const n = (stem, ids) => ids.map((i) => `${stem}_${i}`);
const TICKS = n('jug_tick', [1, 2, 3, 4]), TOCKS = n('jug_tock', [1, 2, 3, 4]);
const STONES = n('jug_stone', [1, 2, 3, 4, 5, 6, 7]), WHOOSH = n('jug_whoosh', [1, 2, 3, 4, 5, 6]);
const GONGS = ['jug_gong_a', 'jug_gong_b', 'jug_gong_c'], ANVIL = n('jug_anvil', [1, 2, 3]);
const CREAKS = ['jug_creak_low_a', 'jug_creak_low_b', 'jug_creak_low_c'], STEEL = ['1771', '1772', '1774', '1776', '1778'];
const CLINKS = ['jug_chain_clink_a', 'jug_chain_clink_b', 'jug_chain_clink_c'], GRIND = ['jug_grind_stone_a', 'jug_grind_stone_b', 'jug_grind_stone_c'];
const SA3_TICK = n('sa3_tradebot_tick', [1, 2, 3, 4]), SA3_WIND = n('sa3_tradebot_wind', [1, 2, 3, 4]);
const PUNCH = k('impactPunch_heavy', 5), PLATE = k('impactPlate_heavy', 5), METAL_H = k('impactMetal_heavy', 5), METAL_L = k('impactMetal_light', 5), BELL = k('impactBell_heavy', 5);
const STEAM = ['jug_steam_puff', 'jug_steam_burst'];

// shared stacks
const rune = (at, dur, g = 1, f = 98) => [
  { type: 'swell', at, gain: 0.5 * g, f, f2: f * 1.5, dur },
  { type: 'hum', at: at + dur * 0.2, gain: 0.35 * g, f: f * 2, dur: dur * 0.8 },
];
const rattle = (at, g = 1) => [
  { pick: CLINKS, at, gain: 0.35 * g, pitch: 0.8 },
  { src: 'jug_chain_rattle', at: at + 0.03, gain: 0.3 * g, trim: [0, 0.4], fadeOut: 0.2, pitch: 0.85 },
  { pick: METAL_L, at: at + 0.06, gain: 0.15 * g, pitch: 0.6 },
];
const boom = (at, len, g = 1, pitch = 0.7) => ({ src: '1023', at, gain: g, trim: [0, len], pitch, fadeOut: len * 0.6, lowpass: 2500 });
const POST = { lowpass: 9000 };

module.exports = {
  // ---------- signature sounds ----------
  tick: { variants: 4, target: -28, vary: { pitch: 0.025 }, post: POST, layers: [
    { pick: TICKS, pitch: 0.62, gain: 1 },
    { pick: STONES, at: 0.005, pitch: 0.5, gain: 0.3, trim: [0, 0.3], fadeOut: 0.15 },
    { pick: SA3_TICK, pitch: 0.4, gain: 0.25 },
  ] },
  tock: { variants: 4, target: -28, vary: { pitch: 0.025 }, post: POST, layers: [
    { pick: TOCKS, pitch: 0.55, gain: 1 },
    { pick: STONES, at: 0.005, pitch: 0.45, gain: 0.35, trim: [0, 0.3], fadeOut: 0.15 },
    { pick: SA3_TICK, pitch: 0.33, gain: 0.22 },
  ] },
  run_down_tick: { variants: 3, target: -27, vary: { pitch: 0.02 }, post: POST, layers: [
    { pick: TOCKS, pitch: 0.5, gain: 1 },
    { pick: STONES, at: 0.005, pitch: 0.4, gain: 0.4, trim: [0, 0.3], fadeOut: 0.15 },
    { pick: CREAKS, at: 0.06, pitch: 0.6, gain: 0.55, trim: [0, 0.7], fadeOut: 0.3 },
    { src: 'jug_spring_b', at: 0.04, pitch: 0.45, gain: 0.22, trim: [0, 0.35], fadeOut: 0.5 },
  ] },
  key_ratchet: { variants: 3, target: -25, post: POST, layers: [
    { pick: SA3_WIND, pitch: 0.55, gain: 0.9, trim: [0, 0.34], fadeOut: 0.08 },
    { src: 'jug_reel_slow', pitch: 0.6, gain: 0.4, trim: [0, 0.36] },
    { pick: ['jug_key_lock_a', 'jug_key_lock_b'], at: 0.6, pitch: 0.6, gain: 0.7 },
    { pick: STONES, at: 0.6, pitch: 0.45, gain: 0.4, trim: [0, 0.3], fadeOut: 0.15 },
  ] },
  plate_ring: { variants: 4, target: -27, vary: { pitch: 0.06 }, post: POST, layers: [
    { pick: GONGS, pitch: 0.85, gain: 1, trim: [0, 1.4], fadeOut: 0.9 },
    { pick: ANVIL, pitch: 0.45, gain: 0.3, fadeOut: 0.4 },
    { pick: PLATE, pitch: 0.6, gain: 0.5 },
  ] },
  glow_on: { variants: 2, target: -27, post: POST, layers: [
    ...rune(0, 1.2),
    { src: 'jug_chime', at: 0.25, pitch: 0.5, gain: 0.14, trim: [0, 0.6], fadeOut: 0.6, highpass: 300 },
  ] },
  steam_puff: { variants: 4, target: -26, vary: { pitch: 0.08 }, post: { lowpass: 7000 }, layers: [
    { pick: STEAM, pitch: 0.8, gain: 1, fadeOut: 0.15 },
    { type: 'whoosh', at: 0, gain: 0.2, f: 200, f2: 900, dur: 0.4 },
  ] },

  // ---------- movement ----------
  step: { variants: 6, target: -30, vary: { pitch: 0.05 }, post: { lowpass: 6000 }, layers: [
    { pick: STONES, pitch: 0.45, gain: 1, trim: [0, 0.4], fadeOut: 0.2 },
    { pick: PUNCH, pitch: 0.7, gain: 0.7 },
    ...rattle(0.07, 0.6),
  ] },
  step_run: { variants: 4, target: -27, vary: { pitch: 0.05 }, post: { lowpass: 6000 }, layers: [
    { pick: STONES, pitch: 0.42, gain: 1, trim: [0, 0.4], fadeOut: 0.2 },
    { pick: PUNCH, pitch: 0.45, gain: 0.7 },
    boom(0, 0.6, 0.4, 0.8),
    ...rattle(0.06, 0.7),
  ] },
  foot_scrape: { variants: 3, target: -29, post: POST, layers: [
    { pick: GRIND, pitch: 0.8, gain: 1, trim: [0, 0.25], fadeIn: 0.03, fadeOut: 0.08 },
    { pick: STONES, at: 0.02, pitch: 0.55, gain: 0.45, trim: [0, 0.25], fadeOut: 0.12 },
  ] },
  skid: { variants: 2, target: -27, post: POST, layers: [
    { pick: GRIND, pitch: 0.7, gain: 1, trim: [0, 0.5], fadeIn: 0.05, fadeOut: 0.15 },
    { pick: STONES, at: 0.38, pitch: 0.45, gain: 0.7, trim: [0, 0.35], fadeOut: 0.15 },
    ...rattle(0.42, 0.8),
  ] },
  plates_settle: { variants: 4, target: -31, post: POST, layers: rattle(0, 1.2) },
  joint_step: { variants: 4, target: -31, vary: { pitch: 0.06 }, post: POST, layers: [
    { src: 'metalClick', pitch: 0.6, gain: 0.8 },
    { pick: CREAKS, at: 0.01, pitch: 0.8, gain: 0.6, trim: [0, 0.2], fadeOut: 0.08 },
  ] },
  head_ratchet: { variants: 3, target: -32, post: POST, layers: [
    { src: 'jug_reel', pitch: 0.55, gain: 1, trim: [0, 0.2], fadeOut: 0.05 },
    { pick: CREAKS, pitch: 1.0, gain: 0.4, trim: [0, 0.35], fadeOut: 0.15 },
  ] },
  joint_creak: { variants: 3, target: -32, post: POST, layers: [
    { pick: CREAKS, pitch: 0.75, gain: 1, fadeOut: 0.2 },
    { src: 'jug_creak_long', pitch: 0.5, gain: 0.3, trim: [0, 0.25], fadeOut: 0.15 },
  ] },
  knuckle_grind: { variants: 2, target: -33, post: POST, layers: [
    { pick: GRIND, pitch: 0.85, gain: 1, trim: [0, 0.4], fadeIn: 0.05, fadeOut: 0.12 },
    { src: 'jug_reel_slow', at: 0.05, pitch: 0.6, gain: 0.4, trim: [0, 0.2] },
  ] },
  claw_flex: { variants: 3, target: -33, post: POST, layers: [
    { src: 'jug_reel', pitch: 0.8, gain: 1, trim: [0, 0.22], fadeOut: 0.05 },
    { pick: CLINKS, at: 0.22, pitch: 0.9, gain: 0.35 },
  ] },
  steam_sigh: { variants: 2, target: -32, post: { lowpass: 6000 }, layers: [
    { pick: CREAKS, pitch: 0.6, gain: 0.6, trim: [0, 0.4], fadeOut: 0.2 },
    { src: '0227', at: 0.55, pitch: 0.85, gain: 1, trim: [0.4, 1.6], fadeIn: 0.2, fadeOut: 0.6 },
    { type: 'swell', at: 0.6, gain: 0.12, f: 140, f2: 90, dur: 0.9 },
  ] },

  // ---------- attacks ----------
  charge_tell: { variants: 2, target: -24, post: POST, layers: [
    { src: 'metalLatch', pitch: 0.55, gain: 0.8 },
    { src: 'jug_steam_burst', at: 0.02, pitch: 0.7, gain: 1 },
    { pick: CREAKS, at: 0.05, pitch: 0.8, gain: 0.5, trim: [0, 0.35], fadeOut: 0.15 },
  ] },
  swing_windup: { variants: 2, target: -26, post: POST, layers: [
    { src: 'jug_creak_long', pitch: 0.55, gain: 1, trim: [0, 0.22], fadeIn: 0.05, fadeOut: 0.1 },
    { src: 'jug_reel_slow', at: 0.05, pitch: 0.5, gain: 0.4, trim: [0, 0.15] },
  ] },
  swing_whoosh: { variants: 3, target: -24, vary: { pitch: 0.05 }, post: POST, layers: [
    { pick: WHOOSH, pitch: 0.6, gain: 1 },
    { type: 'whoosh', at: 0, gain: 0.3, f: 110, f2: 450, dur: 0.4 },
    { src: 'jug_chain_rattle', at: 0.12, pitch: 0.9, gain: 0.3, trim: [0.2, 0.6], fadeOut: 0.15 },
  ] },
  stomp_raise: { variants: 2, target: -27, post: POST, layers: [
    { pick: CREAKS, pitch: 0.55, gain: 1, trim: [0, 0.45], fadeOut: 0.2 },
    { src: 'jug_steam_puff', at: 0.25, pitch: 0.9, gain: 0.4, fadeOut: 0.15 },
  ] },
  stomp_impact: { variants: 4, target: -21, vary: { pitch: 0.04 }, post: POST, layers: [
    { pick: STONES, pitch: 0.35, gain: 1, fadeOut: 0.25 },
    boom(0, 1.2, 0.9),
    { pick: PUNCH, pitch: 0.4, gain: 0.7 },
    { pick: GONGS, at: 0.03, pitch: 0.6, gain: 0.3, trim: [0, 1.2], fadeOut: 0.7 },
    { pick: STEEL, at: 0.12, pitch: 0.6, gain: 0.3, trim: [0, 0.9], fadeOut: 0.4 },
    ...rattle(0.14, 0.8),
  ] },
  grab_open: { variants: 2, target: -26, post: POST, layers: [
    { src: 'jug_reel', pitch: 0.65, gain: 1, trim: [0, 0.28], fadeOut: 0.05 },
    { src: 'jug_creak_metal', pitch: 0.7, gain: 0.45, trim: [0, 0.3], fadeOut: 0.15 },
  ] },
  grab_snap: { variants: 3, target: -24, post: POST, layers: [
    { src: 'metalLatch', pitch: 0.5, gain: 1 },
    { pick: PUNCH, pitch: 0.6, gain: 0.8 },
    { pick: ANVIL, pitch: 0.5, gain: 0.25, trim: [0, 0.3], fadeOut: 0.2 },
  ] },
  grab_clang: { variants: 2, target: -23, post: POST, layers: [
    { pick: ANVIL, pitch: 0.6, gain: 1 },
    { pick: GONGS, pitch: 0.9, gain: 0.5, trim: [0, 1.2], fadeOut: 0.7 },
    { src: 'metalLatch', pitch: 0.7, gain: 0.5 },
    ...rattle(0.12, 0.7),
  ] },
  throw_whoosh: { variants: 2, target: -24, post: POST, layers: [
    { pick: WHOOSH, pitch: 0.5, gain: 1 },
    { src: 'jug_steam_burst', at: 0.25, pitch: 0.8, gain: 0.5 },
  ] },
  grab_release: { variants: 2, target: -28, post: POST, layers: [
    { src: 'jug_reel', pitch: 0.9, gain: 1, trim: [0, 0.18], fadeOut: 0.04 },
    { pick: CLINKS, at: 0.18, gain: 0.4 },
  ] },
  leap_coil: { variants: 2, target: -24, post: POST, layers: [
    { src: 'jug_creak_long', pitch: 0.5, gain: 0.8, trim: [0, 0.7], fadeIn: 0.1, fadeOut: 0.3 },
    { pick: SA3_WIND, at: 0.1, pitch: 0.5, gain: 0.45, trim: [0, 0.25], fadeOut: 0.05 },
    { pick: SA3_WIND, at: 0.6, pitch: 0.7, gain: 0.6, trim: [0, 0.3], fadeOut: 0.05 },
    { pick: SA3_WIND, at: 1.05, pitch: 0.95, gain: 0.8, trim: [0, 0.38], fadeOut: 0.05 },
    { src: 'jug_steam_mid', at: 0.95, gain: 0.5, trim: [0, 0.5], fadeIn: 0.15, fadeOut: 0.1 },
  ] },
  leap_launch: { variants: 2, target: -23, post: POST, layers: [
    { src: 'jug_spring_b', pitch: 0.45, gain: 1, trim: [0, 0.45], fadeOut: 0.6 },
    { pick: WHOOSH, at: 0.03, pitch: 0.45, gain: 0.8 },
    { pick: STONES, pitch: 0.4, gain: 0.6, fadeOut: 0.2 },
    { type: 'boing', at: 0, gain: 0.2, f: 70, wobble: 0.45, dur: 0.9 },
  ] },
  leap_land: { variants: 3, target: -21, vary: { pitch: 0.04 }, post: POST, layers: [
    { pick: STONES, pitch: 0.3, gain: 1, fadeOut: 0.3 },
    boom(0, 1.6, 1, 0.6),
    { pick: PUNCH, pitch: 0.35, gain: 0.7 },
    { pick: GONGS, at: 0.03, pitch: 0.55, gain: 0.35, trim: [0, 1.4], fadeOut: 0.8 },
    { pick: STEEL, at: 0.15, pitch: 0.55, gain: 0.35, trim: [0, 0.9], fadeOut: 0.4 },
    ...rattle(0.15, 1),
    ...rattle(0.45, 0.5),
  ] },
  vent_tell: { variants: 2, target: -25, post: { lowpass: 7000 }, layers: [
    ...rune(0.15, 1.8, 1, 82),
    { src: 'jug_steam_long', at: 0.3, pitch: 0.9, gain: 0.7, trim: [0, 1.6], fadeIn: 1.4, fadeOut: 0.1 },
    { pick: CREAKS, pitch: 0.6, gain: 0.4, trim: [0, 0.4], fadeOut: 0.2 },
  ] },
  vent_blast: { variants: 2, target: -23, post: { lowpass: 8000 }, layers: [
    { src: 'jug_steam_long', pitch: 0.8, gain: 1, trim: [0, 0.8], fadeOut: 0.3 },
    { src: '3019', pitch: 0.85, gain: 0.55, trim: [0.9, 1.8], fadeOut: 0.3 },
    { type: 'whoosh', at: 0, gain: 0.35, f: 90, f2: 400, dur: 0.9 },
    { src: 'jug_steam_burst', at: 0.85, pitch: 0.8, gain: 0.35 },
    { src: 'jug_steam_burst', at: 1.0, pitch: 0.7, gain: 0.25 },
  ] },
  spin_windup: { variants: 2, target: -24, post: POST, layers: [
    { src: 'jug_creak_long', pitch: 0.5, gain: 0.8, trim: [0, 0.45], fadeIn: 0.1, fadeOut: 0.2 },
    { pick: SA3_WIND, pitch: 0.5, gain: 0.4, trim: [0, 0.35], fadeOut: 0.05 },
    { pick: SA3_WIND, at: 0.7, pitch: 0.8, gain: 0.55, trim: [0, 0.4], fadeOut: 0.05 },
    { src: 'jug_whirr_loop', at: 0.9, pitch: 0.5, gain: 0.7, trim: [0, 0.5], fadeIn: 0.6, fadeOut: 0.1 },
    { pick: WHOOSH, at: 1.0, pitch: 0.55, gain: 0.7 },
  ] },
  spin_loop: { loop: 1.0, target: -26, post: POST, layers: [
    { src: 'jug_whirr_loop', pitch: 0.6, gain: 0.7, trim: [0, 0.8] },
    { pick: WHOOSH, at: 0, pitch: 0.55, gain: 0.8 },
    { pick: WHOOSH, at: 0.5, pitch: 0.55, gain: 0.8 },
    { pick: WHOOSH, at: 1.0, pitch: 0.55, gain: 0.8 },
  ] },
  spin_down: { variants: 2, target: -25, post: POST, layers: [
    { src: 'jug_whirr_loop', pitch: 0.55, gain: 0.6, trim: [0, 0.55], fadeOut: 0.5 },
    { type: 'servo', at: 0, gain: 0.3, f: 160, f2: 45, dur: 1.0, soft: true },
    { pick: WHOOSH, at: 0, pitch: 0.5, gain: 0.6 },
    { pick: STONES, at: 1.0, pitch: 0.45, gain: 0.8, trim: [0, 0.35], fadeOut: 0.15 },
    ...rattle(1.05, 0.8),
  ] },
  cog_draw: { variants: 2, target: -29, post: POST, layers: [
    { src: 'jug_grind_metal', pitch: 1.0, gain: 0.6, trim: [0, 0.18], fadeIn: 0.03, fadeOut: 0.05 },
    { pick: ANVIL, at: 0.16, pitch: 0.9, gain: 0.5, trim: [0, 0.4], fadeOut: 0.25 },
  ] },
  cog_throw: { variants: 2, target: -25, post: POST, layers: [
    { pick: WHOOSH, pitch: 0.7, gain: 1 },
    { src: 'jug_steam_burst', at: 0.1, pitch: 0.9, gain: 0.55 },
  ] },
  cog_fly: { loop: 1.0, target: -28, post: POST, layers: [
    { src: 'jug_whirr_loop', pitch: 0.9, gain: 0.6, trim: [0, 1.3] },
    ...[0, 0.25, 0.5, 0.75, 1.0].map((at) => ({ pick: WHOOSH, at, pitch: 1.1, gain: 0.45 })),
  ] },
  cog_hit: { variants: 3, target: -25, post: POST, layers: [
    { pick: ANVIL, pitch: 0.75, gain: 1, fadeOut: 0.3 },
    { pick: METAL_H, pitch: 0.7, gain: 0.7 },
    { pick: CLINKS, at: 0.18, gain: 0.35 },
    { pick: CLINKS, at: 0.32, pitch: 1.1, gain: 0.2 },
  ] },

  // ---------- the rhythm ----------
  rewind_loop: { loop: 1.0, target: -28, post: POST, layers: [
    { src: 'jug_ratchet_hoist_loop', pitch: 0.6, gain: 1, trim: [0, 0.8] },
    { type: 'hum', at: 0, gain: 0.15, f: 98, dur: 1.4 },
    ...[0.05, 0.55, 1.05].map((at) => ({ type: 'swell', at, gain: 0.12, f: 196, f2: 220, dur: 0.4 })),
  ] },
  rewind_end: { variants: 2, target: -25, post: POST, layers: [
    { src: 'jug_ratchet_hoist', pitch: 0.6, gain: 0.8, trim: [0, 0.18], fadeOut: 0.1 },
    { pick: ['jug_key_lock_a', 'jug_key_lock_b'], at: 0.3, pitch: 0.6, gain: 0.9 },
    { pick: STONES, at: 0.3, pitch: 0.5, gain: 0.6, trim: [0, 0.3], fadeOut: 0.15 },
    { src: 'jug_steam_puff', at: 0.35, pitch: 0.85, gain: 0.6, fadeOut: 0.15 },
    ...rattle(0.38, 0.6),
  ] },
  feint_snap: { variants: 2, target: -23, post: POST, layers: [
    { pick: ['switch12', 'switch13', 'switch14'], pitch: 0.9, gain: 1 },
    { src: 'metalLatch', at: 0.01, pitch: 1.0, gain: 0.6 },
    { pick: SA3_TICK, pitch: 1.0, gain: 0.6 },
    { src: 'jug_steam_burst', at: 0.04, pitch: 1.0, gain: 0.55 },
  ] },
  key_jam: { variants: 2, target: -24, post: POST, layers: [
    { src: 'jug_grind_metal', at: 0.05, pitch: 0.6, gain: 0.9, trim: [0, 0.6], fadeOut: 0.2 },
    { src: 'jug_ratchet_hoist', at: 0.05, pitch: 0.5, gain: 0.4, trim: [0, 0.45], tremolo: { hz: 14, depth: 0.9 }, fadeOut: 0.15 },
    { pick: ['jug_grinder_a', 'jug_grinder_b', 'jug_grinder_c'], at: 0.1, gain: 0.45, fadeOut: 0.1 },
    { pick: ['jug_grinder_b', 'jug_grinder_c', 'jug_grinder_a'], at: 0.5, gain: 0.35, fadeOut: 0.1 },
    { pick: ['jug_grinder_c', 'jug_grinder_a', 'jug_grinder_b'], at: 0.8, gain: 0.25, fadeOut: 0.1 },
    { src: 'jug_weld', at: 0.1, gain: 0.3, trim: [0, 0.22], fadeOut: 0.08 },
    { pick: STONES, at: 0.18, pitch: 0.5, gain: 0.5, trim: [0, 0.25], fadeOut: 0.1 },
    { pick: STONES, at: 0.58, pitch: 0.5, gain: 0.45, trim: [0, 0.25], fadeOut: 0.1 },
  ] },
  plates_burst: { variants: 2, target: -23, post: POST, layers: [
    { pick: STONES, pitch: 0.5, gain: 0.6, trim: [0, 0.3], fadeOut: 0.15 },
    { pick: GONGS, at: 0.06, pitch: 0.8, gain: 0.6, trim: [0, 1.2], fadeOut: 0.7 },
    { pick: ANVIL, at: 0.11, pitch: 0.65, gain: 0.6, fadeOut: 0.3 },
    { pick: STEEL, at: 0.17, pitch: 0.8, gain: 0.6, trim: [0, 0.9], fadeOut: 0.4 },
    { pick: WHOOSH, at: 0.08, pitch: 0.6, gain: 0.4 },
  ] },
  stage_roar: { variants: 2, target: -23, post: { lowpass: 8000 }, layers: [
    { src: 'jug_steam_long', pitch: 0.65, gain: 1, trim: [0, 0.6], fadeOut: 0.3 },
    { src: 'jug_steam_mid', at: 0.05, pitch: 0.8, gain: 0.5, trim: [0, 0.6], fadeOut: 0.3 },
    ...rune(0, 0.8, 0.9, 73),
    { pick: CREAKS, pitch: 0.5, gain: 0.4, trim: [0, 0.35], fadeOut: 0.2 },
  ] },
  plates_land: { variants: 3, target: -27, post: POST, layers: [
    { pick: STEEL, pitch: 0.75, gain: 1, trim: [0, 0.9], fadeOut: 0.4 },
    { pick: ['1774', '1778', '1771', '1776', '1772'], at: 0.13, pitch: 0.7, gain: 0.8, trim: [0, 0.9], fadeOut: 0.4 },
    { pick: CLINKS, at: 0.25, gain: 0.4 },
    { pick: ['1776', '1772', '1778', '1771', '1774'], at: 0.3, pitch: 0.85, gain: 0.5, trim: [0, 0.9], fadeOut: 0.4 },
  ] },

  // ---------- reactions ----------
  hit_wound: { variants: 4, target: -27, vary: { pitch: 0.06 }, post: POST, layers: [
    { pick: GONGS, pitch: 0.95, gain: 1, trim: [0, 1.3], fadeOut: 0.8 },
    { pick: PLATE, pitch: 0.6, gain: 0.6 },
    { pick: SA3_TICK, at: 0.06, pitch: 0.5, gain: 0.3 },
  ] },
  hit_open: { variants: 4, target: -25, vary: { pitch: 0.05 }, post: POST, layers: [
    { pick: PUNCH, pitch: 0.7, gain: 1 },
    { pick: STONES, pitch: 0.6, gain: 0.6, trim: [0, 0.3], fadeOut: 0.15 },
    { src: 'jug_spring_a', at: 0.02, pitch: 0.9, gain: 0.4, trim: [0, 0.5], fadeOut: 0.3 },
    { src: 'jug_steam_burst', at: 0.03, pitch: 1.1, gain: 0.4 },
  ] },
  lightning: { variants: 1, target: -24, post: POST, layers: [
    { pick: ['spark_shock_a', 'spark_shock_b'], gain: 0.8 },
    { src: 'jug_weld', at: 0.05, gain: 0.5, trim: [0, 0.45], fadeOut: 0.15 },
    ...rune(0, 0.7, 1.2, 110),
    { type: 'servo', at: 0.1, gain: 0.3, f: 90, f2: 220, dur: 0.6, soft: true },
    { src: 'metalLatch', at: 0.12, pitch: 0.7, gain: 0.6 },
  ] },

  // ---------- rest, waking, death ----------
  ember_flicker: { variants: 3, target: -34, post: POST, layers: [
    { pick: ['flicker_bulb_a', 'flicker_bulb_b'], pitch: 0.5, gain: 0.6, trim: [0, 0.25], fadeOut: 0.08 },
    { type: 'hum', at: 0, gain: 0.25, f: 98, dur: 0.25 },
  ] },
  steam_wisp: { variants: 2, target: -35, post: { lowpass: 4000 }, layers: [
    { src: '0227', gain: 1, trim: [1.5, 2.4], fadeIn: 0.25, fadeOut: 0.4 },
  ] },
  key_turn: { variants: 2, target: -26, post: POST, layers: [
    { pick: SA3_WIND, pitch: 0.55, gain: 0.9, trim: [0, 0.16], fadeOut: 0.05 },
    { pick: ['jug_key_lock_a', 'jug_key_lock_b'], at: 0.3, pitch: 0.6, gain: 0.9 },
    { pick: STONES, at: 0.3, pitch: 0.5, gain: 0.5, trim: [0, 0.3], fadeOut: 0.15 },
  ] },
  salute_snap: { variants: 2, target: -26, post: POST, layers: [
    { src: 'metalLatch', pitch: 0.75, gain: 1 },
    { src: 'jug_chime', at: 0.01, pitch: 0.75, gain: 0.5, trim: [0, 1.0], fadeOut: 0.6 },
    { pick: BELL, at: 0.01, pitch: 0.8, gain: 0.35 },
    { src: 'jug_steam_puff', at: 0.12, gain: 0.3, fadeOut: 0.15 },
  ] },
  glow_out: { variants: 1, target: -30, post: POST, layers: [
    { type: 'swell', at: 0, gain: 0.45, f: 120, f2: 60, dur: 1.0 },
    { type: 'hum', at: 0, gain: 0.2, f: 196, dur: 0.5 },
    { pick: SA3_TICK, pitch: 0.4, gain: 0.3 },
  ] },
  kneel_impact: { variants: 2, target: -23, post: POST, layers: [
    { pick: STONES, pitch: 0.38, gain: 1, fadeOut: 0.25 },
    boom(0, 1.0, 0.7, 0.75),
    { pick: GONGS, at: 0.03, pitch: 0.65, gain: 0.3, trim: [0, 1.0], fadeOut: 0.6 },
    ...rattle(0.12, 1),
  ] },
  key_unwind: { variants: 1, target: -26, post: POST, layers: [
    { src: 'jug_ratchet_hoist', pitch: 0.9, gain: 1, trim: [0, 0.35], fadeOut: 0.05 },
    { src: 'jug_reel', at: 0.35, pitch: 0.7, gain: 0.7, trim: [0, 0.25], fadeOut: 0.1 },
    { type: 'servo', at: 0, gain: 0.25, f: 240, f2: 80, dur: 0.7, soft: true },
  ] },
  mainspring_burst: { variants: 1, target: -21, post: POST, layers: [
    { src: 'metalLatch', pitch: 0.5, gain: 0.8 },
    { pick: STONES, pitch: 0.5, gain: 0.5, trim: [0, 0.3], fadeOut: 0.15 },
    { src: 'jug_spring_a', at: 0.01, pitch: 0.6, gain: 1, trim: [0, 1.0], fadeOut: 0.8 },
    { src: '1950', at: 0.02, pitch: 0.7, gain: 0.4, trim: [0, 1.0], fadeOut: 0.5 },
    { type: 'boing', at: 0.01, gain: 0.3, f: 105, wobble: 0.6, dur: 1.5 },
    { pick: ANVIL, at: 0.02, pitch: 0.9, gain: 0.2, fadeOut: 0.4 },
  ] },
  fist_thud: { variants: 2, target: -27, post: POST, layers: [
    { pick: STONES, pitch: 0.45, gain: 1, fadeOut: 0.2 },
    { pick: PUNCH, pitch: 0.7, gain: 0.6 },
    ...rattle(0.08, 0.6),
  ] },
  death_steam: { variants: 1, target: -26, post: { lowpass: 6000 }, layers: [
    { src: '0227', pitch: 0.85, gain: 1, trim: [0, 1.4], fadeOut: 0.9 },
    { type: 'swell', at: 0, gain: 0.15, f: 120, f2: 55, dur: 1.4 },
    { pick: CREAKS, at: 0.4, pitch: 0.5, gain: 0.35, trim: [0, 0.35], fadeOut: 0.2 },
  ] },

  // ---------- the ritual ----------
  key_fit: { variants: 2, target: -28, post: POST, layers: [
    { pick: ['jug_key_lock_a', 'jug_key_lock_b'], pitch: 0.85, gain: 1 },
    { pick: ANVIL, pitch: 0.9, gain: 0.3, trim: [0, 0.5], fadeOut: 0.3 },
    { src: 'jug_reel', at: 0.12, gain: 0.3, trim: [0, 0.15], fadeOut: 0.04 },
  ] },
  ritual_click: { variants: 4, target: -31, vary: { pitch: 0.04 }, post: POST, layers: [
    { pick: SA3_TICK, pitch: 0.9, gain: 1 },
    { src: 'metalClick', pitch: 0.8, gain: 0.3 },
  ] },
  ritual_tick: { variants: 4, target: -28, vary: { pitch: 0.03 }, post: POST, layers: [
    { pick: TICKS, pitch: 0.7, gain: 1 },
    { pick: METAL_L, at: 0.02, pitch: 0.6, gain: 0.3 },
    { pick: CLINKS, at: 0.04, pitch: 1.2, gain: 0.2 },
  ] },
  ritual_runes: { variants: 1, target: -30, post: POST, layers: [
    ...rune(0, 1.0, 0.6, 110),
    { src: 'jug_chime', at: 0.1, pitch: 0.5, gain: 0.12, trim: [0, 0.6], fadeOut: 0.5, highpass: 300 },
  ] },
  ritual_steam: { variants: 3, target: -31, post: { lowpass: 6000 }, layers: [
    { pick: ['jug_steam_mid', 'jug_steam_puff', 'jug_steam_long'], gain: 1, trim: [0, 0.6], fadeIn: 0.05, fadeOut: 0.3 },
  ] },
  ritual_ready: { variants: 1, target: -23, post: POST, layers: [
    ...rune(0, 0.8, 1.0, 98),
    { src: 'jug_ratchet_hoist', pitch: 0.8, gain: 0.5, trim: [0, 0.6], fadeOut: 0.3 },
    { src: 'jug_chime', at: 0.1, pitch: 0.6, gain: 0.6, trim: [0, 1.4], fadeOut: 0.8 },
  ] },
  ritual_unfold: { variants: 1, target: -21, post: POST, layers: [
    { pick: STONES, pitch: 0.45, gain: 0.8, fadeOut: 0.2 },
    boom(0, 1.0, 0.8, 0.8),
    ...rune(0, 0.6, 1.0, 98),
    { src: '1774', at: 0.05, pitch: 0.75, gain: 0.5, trim: [0, 0.9], fadeOut: 0.4 },
    { src: '1776', at: 0.17, pitch: 0.7, gain: 0.45, trim: [0, 0.9], fadeOut: 0.4 },
    { src: 'jug_ratchet_hoist', at: 0.05, pitch: 0.7, gain: 0.4, trim: [0, 0.45], fadeOut: 0.15 },
    { src: 'jug_steam_long', at: 0.1, gain: 0.6, trim: [0, 0.8], fadeOut: 0.4 },
  ] },
  ritual_collapse: { variants: 1, target: -24, post: POST, layers: [
    { pick: STONES, pitch: 0.5, gain: 0.8, fadeOut: 0.2 },
    { src: '1772', at: 0.02, pitch: 0.7, gain: 0.5, trim: [0, 0.9], fadeOut: 0.4 },
    { src: 'jug_steam_long', at: 0.05, pitch: 0.85, gain: 0.8, trim: [0, 1.4], fadeOut: 0.6 },
    { pick: WHOOSH, at: 0.1, pitch: 0.5, gain: 0.6 },
  ] },

  // ---------- the Spring Engine ----------
  engine_ratchet: { variants: 4, target: -38, vary: { pitch: 0.04 }, post: POST, layers: [
    { pick: SA3_TICK, pitch: 1.2, gain: 1 },
    { src: 'metalClick', pitch: 1.0, gain: 0.2 },
  ] },
  engine_tick: { variants: 3, target: -38, vary: { pitch: 0.03 }, post: POST, layers: [{ pick: TICKS, pitch: 1.6, gain: 1, fadeOut: 0.1 }] },
  engine_tock: { variants: 3, target: -38, vary: { pitch: 0.03 }, post: POST, layers: [{ pick: TOCKS, pitch: 1.5, gain: 1, fadeOut: 0.1 }] },
  engine_rune_up: { variants: 1, target: -33, post: POST, layers: [
    ...rune(0, 0.6, 0.6, 147),
    { src: 'jug_chime', at: 0.05, pitch: 0.7, gain: 0.1, trim: [0, 0.6], fadeOut: 0.4, highpass: 300 },
  ] },
  engine_rune_down: { variants: 1, target: -34, post: POST, layers: [{ type: 'swell', at: 0, gain: 0.4, f: 147, f2: 110, dur: 0.6 }] },
  engine_full: { variants: 1, target: -31, post: POST, layers: [
    { src: 'jug_key_lock_a', pitch: 1.0, gain: 0.8 },
    { src: 'jug_chime', at: 0.01, pitch: 0.9, gain: 0.5, trim: [0, 0.9], fadeOut: 0.5 },
    ...rune(0, 0.6, 0.4, 196),
  ] },
  engine_empty: { variants: 1, target: -33, post: POST, layers: [
    { pick: CREAKS, pitch: 0.9, gain: 1, trim: [0, 0.4], fadeOut: 0.2 },
    { pick: STONES, at: 0.35, pitch: 0.8, gain: 0.5, trim: [0, 0.25], fadeOut: 0.1 },
  ] },
};
