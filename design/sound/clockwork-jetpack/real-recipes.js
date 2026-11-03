// The Clockwork Jetpack's real sounds (Stage 2): for each event, the recordings it's layered from (rung 2: Trade Bot's
// approved tick and wind takes, the Kenney packs, the Juggernaut's BigSoundBank cuts; rung 3: new BigSoundBank CC0 cuts
// from cuts.js) and the few synth layers no recording covers (an air surge). `target` is the loudness the event sits
// at (max momentary LUFS for one-shots, integrated for loops), from the table in clockwork-jetpack.md; levels.js turns
// it into each recipe's `level`. Pitch is a resample factor: 0.5 is an octave down. No running sounds (user call).
const k = (stem, n, from = 0) => Array.from({ length: n }, (_, i) => `${stem}_${String(i + from).padStart(3, '0')}`);
const n = (stem, ids) => ids.map((i) => `${stem}_${i}`);
const TICK = n('sa3_tradebot_tick', [1, 2, 3, 4]), WIND = n('sa3_tradebot_wind', [1, 2, 3, 4]);
const WOOD = k('impactWood_light', 5), METAL_L = k('impactMetal_light', 5);
// the fans: a small drone, pitched down and softened (user pick over the electric fan, 2026-11-03)
const FAN_UP = ['cj_fan_up_drone'], FAN_DOWN = ['cj_fan_down_drone'];
const SNAP = ['cj_canvas_snap_a', 'cj_canvas_snap_b', 'cj_canvas_snap_c'];
const POST = { lowpass: 9000 };

// shared stacks
const tick = (at, gain = 1, pitch = 1.25) => ({ pick: TICK, at, gain, pitch, trim: [0, 0.18], fadeOut: 0.06 });
const knuckle = (at, gain = 1) => ({ src: 'metalClick', at, gain: 0.45 * gain, pitch: 1.6 });
const rib = (at, gain = 1, pitch = 1.5) => ({ pick: WOOD, at, gain: 0.8 * gain, pitch, highpass: 300 });
const fanUp = (at, gain = 1, trim = [0, 1.2]) => ({ pick: FAN_UP, at, gain, pitch: 0.75, trim, fadeIn: 0.25, fadeOut: 0.35, lowpass: 2600 });
const fanDown = (at, gain = 1) => ({ pick: FAN_DOWN, at, gain, pitch: 0.75, trim: [0, 1.1], fadeIn: 0.02, fadeOut: 0.6, lowpass: 2400 });

module.exports = {
  wings_unfold: {
    variants: 3, target: -27, vary: { pitch: 0.04, gain: 0.08, at: 0.004 }, post: POST,
    layers: [
      { src: 'creak1', at: 0, gain: 0.25, pitch: 1.4, trim: [0, 0.12], fadeOut: 0.05 }, // the tuck
      ...[1, 2, 3, 4, 5].map((r) => rib(0.05 + 0.05 * (r - 1) + 0.15, 1 - 0.1 * (r - 1), 1.35 + 0.05 * r)), // each rib lands
      ...[1, 2, 3, 4, 5].map((r) => knuckle(0.3 + 0.04 * (r - 1) + 0.18, 0.8)), // the elbows lock
      { pick: SNAP, at: 0.42, gain: 1, pitch: 0.85, highpass: 120 }, // the canvas goes taut
      { src: 'creak2', at: 0.6, gain: 0.18, pitch: 1.3, trim: [0, 0.2], fadeOut: 0.1 }, // the lift into the V
    ],
  },
  wings_fold: {
    variants: 3, target: -28, vary: { pitch: 0.04, gain: 0.08, at: 0.004 }, post: POST,
    layers: [
      ...[0, 1, 2, 3, 4].map((i) => knuckle(0.05 + 0.03 * i, 0.7)), // the elbows unlock, trailing first
      { src: 'cj_canvas_slack', at: 0.1, gain: 0.6, pitch: 0.9, fadeOut: 0.2 }, // the canvas slackens
      ...[0, 1, 2, 3, 4].map((i) => rib(0.3 + 0.05 * i, 0.8 - 0.05 * i, 1.6 - 0.04 * i)), // the ribs close
      { pick: WOOD, at: 0.57, gain: 0.15, pitch: 1.9 }, // the leading rib's bounce
    ],
  },
  run_down: {
    variants: 2, target: -27, vary: { pitch: 0.02, gain: 0.05 }, post: POST,
    layers: [
      ...[0, 0.1, 0.21, 0.34, 0.5, 0.69, 0.92, 1.2, 1.55].map((t, i) => tick(t, 0.9 - i * 0.07, 1.3 - i * 0.04)), // the ticks spread and sag
      fanDown(0, 0.6),
      { pick: METAL_L, at: 1.6, gain: 0.35, pitch: 1.3 }, // the pawl drops
      tick(1.78, 0.3, 0.95), tick(1.86, 0.15, 0.92), // the last half-tick shiver
    ],
  },
  engine_on: {
    variants: 2, target: -27, vary: { pitch: 0.03, gain: 0.06 }, post: POST,
    layers: [{ src: 'metalClick', at: 0, gain: 0.8, pitch: 1.2 }, fanUp(0.03, 0.9), tick(0.3, 0.6), tick(0.38, 0.45, 1.3)],
  },
  engine_off: {
    variants: 2, target: -28, vary: { pitch: 0.03, gain: 0.06 }, post: POST,
    layers: [{ src: 'metalClick', at: 0, gain: 0.8, pitch: 1.1 }, fanDown(0.02, 0.9), { pick: METAL_L, at: 0.45, gain: 0.3, pitch: 1.4 }],
  },
  hover_on: {
    variants: 2, target: -34, vary: { pitch: 0.04, gain: 0.06 }, post: POST,
    layers: [{ src: 'toggle_001', at: 0, gain: 0.8 }, { pick: METAL_L, at: 0.05, gain: 0.25, pitch: 1.6 }],
  },
  hover_off: {
    variants: 2, target: -34, vary: { pitch: 0.04, gain: 0.06 }, post: POST,
    layers: [{ pick: METAL_L, at: 0, gain: 0.25, pitch: 1.4 }, { src: 'toggle_002', at: 0.05, gain: 0.8 }],
  },
  boost: {
    variants: 3, target: -30, vary: { pitch: 0.05, gain: 0.08 }, post: POST,
    layers: [
      { pick: FAN_UP, at: 0, gain: 0.9, pitch: 0.95, trim: [0.6, 1.2], fadeIn: 0.08, fadeOut: 0.2, lowpass: 3000 },
      // rung 4: an air surge has no clean real take here; the synth whoosh is brown noise through a swept band-pass
      { type: 'whoosh', at: 0, gain: 0.25, f: 400, f2: 1100, dur: 0.45 },
      { pick: WIND, at: 0, gain: 0.35, pitch: 1.4, trim: [0, 0.2], fadeOut: 0.08 },
    ],
  },
  wind_low: {
    variants: 2, target: -32, vary: { pitch: 0.02, gain: 0.05 }, post: POST,
    layers: [tick(0, 0.9, 1.0), tick(0.12, 0.6, 0.95), { pick: METAL_L, at: 0.3, gain: 0.25, pitch: 1.0 }],
  },
  equip: {
    variants: 2, target: -32, vary: { pitch: 0.04, gain: 0.06 }, post: POST,
    layers: [{ pick: ['beltHandle1', 'beltHandle2'], at: 0, gain: 0.7 }, { pick: METAL_L, at: 0.2, gain: 0.3, pitch: 1.5 }, { pick: METAL_L, at: 0.32, gain: 0.25, pitch: 1.65 }],
  },
  rails_extend: {
    variants: 1, target: -34, post: POST,
    layers: [{ src: 'card-slide-1', at: 0, gain: 0.6, pitch: 0.7, lowpass: 5000 }, { src: 'metalLatch', at: 0.28, gain: 0.5, pitch: 1.3 }],
  },
  station_hang: {
    variants: 2, target: -31, vary: { pitch: 0.04, gain: 0.06 }, post: POST,
    layers: [{ pick: k('impactWood_medium', 5), at: 0, gain: 0.8, pitch: 1.1 }, { pick: METAL_L, at: 0.04, gain: 0.35, pitch: 1.2 }],
  },
  station_take: {
    variants: 2, target: -32, vary: { pitch: 0.04, gain: 0.06 }, post: POST,
    layers: [{ pick: METAL_L, at: 0, gain: 0.35, pitch: 1.25 }, { pick: ['handleSmallLeather', 'handleSmallLeather2'], at: 0.02, gain: 0.6 }],
  },
  station_boom: {
    variants: 2, target: -32, vary: { pitch: 0.04, gain: 0.06 }, post: POST,
    layers: [{ src: 'jug_reel', at: 0, gain: 0.5, pitch: 1.1, trim: [0, 0.32], fadeOut: 0.08 }, { pick: ['creak1', 'creak3'], at: 0.3, gain: 0.3, pitch: 1.2, trim: [0, 0.3], fadeOut: 0.12 }],
  },
  station_engage: {
    variants: 2, target: -30, vary: { pitch: 0.04, gain: 0.06 }, post: POST,
    layers: [{ src: 'metalLatch', at: 0, gain: 0.9 }, { pick: k('impactMetal_medium', 5), at: 0.005, gain: 0.35, pitch: 1.3 }],
  },
  // a loop, only while a jetpack is actually being wound: Trade Bot's key wind, the pawl, the drive far under
  station_winding: {
    // a 2 s loop (the pawl clicks every third of a second); the last take runs past the loop point to crossfade in
    variants: 1, target: -36, loop: 2, post: POST,
    layers: [
      { src: 'sa3_tradebot_wind_1', at: 0, gain: 0.8, fadeIn: 0.05, fadeOut: 0.08 },
      { src: 'sa3_tradebot_wind_2', at: 0.68, gain: 0.8, fadeIn: 0.05, fadeOut: 0.08 },
      { src: 'sa3_tradebot_wind_3', at: 1.36, gain: 0.8, fadeIn: 0.05, fadeOut: 0.08 },
      { src: 'sa3_tradebot_wind_4', at: 2.0, gain: 0.8, fadeIn: 0.05, fadeOut: 0.08 },
      ...[0.17, 0.5, 0.83, 1.17, 1.5, 1.83, 2.17].map((at) => ({ src: 'metalClick', at, gain: 0.18, pitch: 1.7 })),
      { src: 'sa3_crucible_gears_v1_x2', at: 0, gain: 0.12, trim: [0, 2.4], fadeIn: 0.05, fadeOut: 0.05, lowpass: 2000 },
    ],
  },
  // fully wound: a click and a clear ping heard across the base: the pendulum chime pitched up (user pick over a bell)
  station_full: {
    variants: 1, target: -27, post: POST,
    layers: [{ src: 'metalClick', at: 0, gain: 0.6 }, { src: 'jug_chime', at: 0.03, gain: 0.9, pitch: 1.5, trim: [0, 1.3], fadeOut: 0.6 }],
  },
  station_release: {
    variants: 2, target: -32, vary: { pitch: 0.04, gain: 0.06 }, post: POST,
    layers: [{ src: 'metalClick', at: 0, gain: 0.6 }, { pick: ['creak2', 'creak3'], at: 0.05, gain: 0.3, pitch: 1.15, trim: [0, 0.3], fadeOut: 0.12 }, { src: 'jug_reel', at: 0.45, gain: 0.4, pitch: 1.2, trim: [0, 0.25], fadeOut: 0.08 }],
  },
};
