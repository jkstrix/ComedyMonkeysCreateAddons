// Writes clockwork-jetpack.cues.json: the Clockwork Jetpack's and its Winding Station's sound events (placeholder
// recipes) and every clip's cues. Cue times come from design/models/clockwork-jetpack.gen.js: the ribs land at their
// spring peaks (0.2 s after each starts), the canvas goes taut as the elbows straighten, the socket latches as the boom
// settles. See design/sound/clockwork-jetpack.md.
// No running sounds (user, 2026-11-03: "being strapped to your back constant sounds are going to get annoying"): only
// one-shots for starting, stopping and the wings; flight's whoosh is vanilla's elytra sound, which plays by itself.
const fs = require('fs');
const path = require('path');

const E = (subtitle, group, layers) => ({ subtitle, group, layers });
// signature sounds as layer stacks, so events can reuse them at an offset
const TICK = (at = 0, g = 1, f = 2900) => [
  { type: 'tick', at, gain: 0.5 * g, f, dur: 0.04 },
  { type: 'clink', at: at + 0.004, gain: 0.08 * g, f: 1900, dur: 0.12 },
];
const FAN = (at, dur, g = 1, f = 170, f2 = 210) => [
  { type: 'servo', at, gain: 0.22 * g, f, f2, dur },
  { type: 'whoosh', at, gain: 0.12 * g, f: 600, f2: 900, dur },
];
const RIB = (at, g = 1) => [
  { type: 'clack', at, gain: 0.45 * g, f: 900, dur: 0.07 },
  { type: 'whoosh', at: at - 0.04, gain: 0.12 * g, f: 1400, f2: 700, dur: 0.06 },
];
const KNUCKLE = (at, g = 1) => [{ type: 'click', at, gain: 0.3 * g, dur: 0.03 }, { type: 'ping', at: at + 0.003, gain: 0.06 * g, f: 2600, dur: 0.12 }];
const RATCHET = (at, dur, g = 1, rate = 14) => [{ type: 'ratchet', at, gain: 0.35 * g, rate, f: 2200, dur }];

const events = {
  // ---- signature sounds ----
  wings_unfold: E('Clockwork Jetpack spreads its wings', 'Signature sounds', [
    { type: 'creak', at: 0, gain: 0.18, f: 420, dur: 0.06 }, // the tuck
    ...[1, 2, 3, 4, 5].flatMap((k) => RIB(0.05 + 0.05 * (k - 1) + 0.15, 1 - 0.1 * (k - 1))), // each rib lands
    ...[1, 2, 3, 4, 5].flatMap((k) => KNUCKLE(0.3 + 0.04 * (k - 1) + 0.18, 0.8)), // the elbows lock
    { type: 'whoosh', at: 0.42, gain: 0.35, f: 260, f2: 140, dur: 0.18 }, // the canvas goes taut
    { type: 'thud', at: 0.52, gain: 0.25, f: 140, dur: 0.12 },
    { type: 'creak', at: 0.6, gain: 0.12, f: 300, dur: 0.15 }, // the lift into the V
  ]),
  wings_fold: E('Clockwork Jetpack folds its wings', 'Signature sounds', [
    ...[5, 4, 3, 2, 1].flatMap((k, i) => KNUCKLE(0.03 * i + 0.05, 0.7)), // the elbows unlock
    { type: 'whoosh', at: 0.12, gain: 0.2, f: 300, f2: 600, dur: 0.15 }, // the canvas slackens
    ...[5, 4, 3, 2, 1].flatMap((k, i) => RIB(0.2 + 0.05 * i + 0.1, 0.8 - 0.05 * i)), // ribs close, trailing first
    { type: 'clack', at: 0.57, gain: 0.12, f: 1100, dur: 0.05 }, // the leading rib's bounce
  ]),
  run_down: E('Clockwork Jetpack winds down', 'Signature sounds', [
    // the tick slows: gaps grow, pitch falls
    ...[0, 0.1, 0.21, 0.34, 0.5, 0.69, 0.92, 1.2, 1.55].flatMap((t, i) => TICK(t, 0.75 - i * 0.05, 2900 - i * 120)),
    { type: 'servo', at: 0, gain: 0.2, f: 190, f2: 60, dur: 1.6 },
    { type: 'clink', at: 1.6, gain: 0.3, f: 1500, dur: 0.15 }, // the pawl drops
    { type: 'tick', at: 1.78, gain: 0.25, f: 2200, dur: 0.04 }, // the last half-tick shiver
    { type: 'tick', at: 1.86, gain: 0.12, f: 2100, dur: 0.04 },
  ]),
  // ---- controls ----
  engine_on: E('Clockwork Jetpack starts', 'Controls', [...KNUCKLE(0, 1.2), ...FAN(0.04, 0.5, 1, 60, 190), ...TICK(0.3), ...TICK(0.38)]),
  engine_off: E('Clockwork Jetpack stops', 'Controls', [...KNUCKLE(0, 1.2), ...FAN(0.02, 0.6, 1, 190, 50), { type: 'clink', at: 0.45, gain: 0.2, f: 1500, dur: 0.12 }]),
  hover_on: E('Clockwork Jetpack holds', 'Controls', [{ type: 'click', at: 0, gain: 0.4, dur: 0.03 }, { type: 'clink', at: 0.06, gain: 0.15, f: 1700, dur: 0.15 }]),
  hover_off: E('Clockwork Jetpack lets go', 'Controls', [{ type: 'clink', at: 0, gain: 0.15, f: 1400, dur: 0.15 }, { type: 'click', at: 0.05, gain: 0.3, dur: 0.03 }]),
  boost: E('Clockwork Jetpack surges', 'Controls', [...FAN(0, 0.5, 1.4, 190, 280), { type: 'whoosh', at: 0, gain: 0.3, f: 400, f2: 1100, dur: 0.45 }, ...RATCHET(0, 0.2, 0.4, 24)]),
  wind_low: E('Clockwork Jetpack runs low', 'Controls', [...TICK(0, 0.9, 2400), ...TICK(0.12, 0.6, 2300), { type: 'clink', at: 0.3, gain: 0.12, f: 1300, dur: 0.12 }]),
  equip: E('Clockwork Jetpack buckles on', 'Controls', [{ type: 'rustle', at: 0, gain: 0.25, density: 10, dur: 0.25 }, { type: 'clink', at: 0.2, gain: 0.3, f: 1600, dur: 0.15 }, { type: 'clink', at: 0.32, gain: 0.25, f: 1750, dur: 0.15 }]),
  rails_extend: E('Clockwork Jetpack rails slide out', 'Controls', [{ type: 'scrape', at: 0, gain: 0.2, f: 1800, dur: 0.25 }, ...KNUCKLE(0.28, 1)]),
  // ---- the Winding Station ----
  station_hang: E('Jetpack hung up', 'Winding Station', [{ type: 'clack', at: 0, gain: 0.4, f: 500, dur: 0.1 }, { type: 'clink', at: 0.04, gain: 0.25, f: 1400, dur: 0.2 }]),
  station_take: E('Jetpack taken down', 'Winding Station', [{ type: 'clink', at: 0, gain: 0.22, f: 1500, dur: 0.18 }, { type: 'rustle', at: 0.02, gain: 0.15, density: 8, dur: 0.15 }]),
  station_boom: E('Winding Station reaches out', 'Winding Station', [{ type: 'scrape', at: 0, gain: 0.15, f: 1500, dur: 0.3 }, { type: 'creak', at: 0.3, gain: 0.2, f: 380, dur: 0.3 }]),
  station_engage: E('Winding Station takes the key', 'Winding Station', [{ type: 'click', at: 0, gain: 0.45, dur: 0.03 }, { type: 'clang', at: 0.005, gain: 0.15, f: 700, dur: 0.3 }]),
  station_winding: E('Winding Station winds', 'Winding Station', [...RATCHET(0, 2, 0.8, 6), { type: 'servo', at: 0, gain: 0.12, f: 110, f2: 110, dur: 2 }, ...[0.17, 0.5, 0.83, 1.17, 1.5, 1.83].flatMap((t) => [{ type: 'click', at: t, gain: 0.2, dur: 0.02 }])]),
  station_full: E('Jetpack fully wound', 'Winding Station', [{ type: 'click', at: 0, gain: 0.35, dur: 0.03 }, { type: 'ping', at: 0.03, gain: 0.4, f: 1320, dur: 1 }, { type: 'ping', at: 0.18, gain: 0.25, f: 1760, dur: 0.9 }]),
  station_release: E('Winding Station lets go', 'Winding Station', [{ type: 'click', at: 0, gain: 0.3, dur: 0.03 }, { type: 'creak', at: 0.05, gain: 0.18, f: 360, dur: 0.3 }, { type: 'scrape', at: 0.45, gain: 0.12, f: 1500, dur: 0.25 }]),
};

// cues per clip, from the generator's timings
const station = [[0.1, 'station_boom'], [0.6, 'station_engage'], [0.8, 'station_winding'], [2.8, 'station_winding'], [4.9, 'station_full'], [5.0, 'station_release']];
const clips = {
  wings_unfold: [[0, 'wings_unfold']],
  wings_fold: [[0, 'wings_fold']],
  run_down: [[0, 'run_down']],
  coil_states: [[3, 'wind_low']],
  station_hang: station,
  station_back_in: station,
  standoff: [[0.1, 'rails_extend']],
};

// Stage 2: the real recipes, each at the level levels.js measured for its loudness target
const NS = 'clockwork_jetpack';
const real = require('./real-recipes.js');
let levels = {};
try { levels = JSON.parse(fs.readFileSync(path.join(__dirname, 'levels.json'), 'utf8')); } catch (e) { /* first build */ }
for (const [id, r] of Object.entries(real)) {
  if (!events[id]) throw new Error('real recipe for an unknown event: ' + id);
  events[id].real = { ...r, level: levels[`${NS}.${id}`] ?? r.level ?? 0.5 };
}
fs.writeFileSync(path.join(__dirname, 'clockwork-jetpack.cues.json'), JSON.stringify({ namespace: NS, events, clips }, null, 2));
console.log('wrote', Object.keys(events).length, 'events,', Object.keys(clips).length, 'clips');
