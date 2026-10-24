// Pressure Crucible redesign generator -> design/models/pressure-crucible.bbmodel (Generic Model preview with clips).
// Built on the family kit in the game's model space (back and shaft to the south, the open front to the north), to
// design/briefs/pressure-crucible.md: a square screw press. The shaft turns a pinion that drives a big gear on the
// screw; the platen rides down the screw onto the dust; a gauge needle climbs with each press; the bowl glows with the
// burner's heat. Parts flagged glow get NeoForge's full-bright light in game.
const path = require('path');
const { P, C, textureSet, painters, mot, hash, edge, FIELD, HI, LO, preview, SHOW, HIDE } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'pressure-crucible.bbmodel');
const LAYER = 0.25; // overlays sit this far in front of what's behind them (zfight-check.js)

// ---------- textures (pressure_crucible_<key>) ----------
const T = textureSet(); T.file = 'pressure_crucible_';
P.brassDark = ['#7d6630', '#8f7638', '#9c8040'];
T.paint('plate', (x, y) => (edge(x, y) === 0 ? (x === 0 || y === 0 ? HI : LO) : FIELD(x, y, 70)));
T.paint('panel', painters.side('andesite', () => null));
T.paint('trim', (x, y) => mot('andesite', x, y, 71));
T.paint('dark', painters.dark());
T.paint('steel', painters.steel());
T.paint('brass', (x, y) => mot('brassDark', x, y, 72));
// the screw: polished threads, running round it at a slant so they seem to climb as it turns
T.paint('thread', (x, y) => (y % 2 === 0 ? (x % 4 === 0 ? '#8a9099' : '#6b7178') : '#262a30'));
// gears: steel teeth, a darker face with a hub and spokes on top
T.paint('tooth', (x, y) => (y % 4 === 0 ? '#3c424a' : mot('steelLight', x, y, 74)));
T.paint('gear_top', (x, y) => {
  const r = Math.hypot(x - 7.5, y - 7.5), a = Math.atan2(y - 7.5, x - 7.5);
  if (r > 7.6) return null;
  if (r < 2) return mot('brassDark', x, y, 75);
  if (r > 6.4) return '#4a5059';
  return Math.abs(Math.sin(a * 2)) < 0.25 ? '#4a5059' : '#2f343b';
});
// the bowl: a dark die floor, its rim, and the bed's vents
T.paint('floor', (x, y) => (hash(x, y, 76) > 0.85 ? '#24282d' : '#1f2327'));
T.paint('vent', (x, y) => (y % 3 === 1 ? '#16191c' : mot('dark', x, y, 77)));
// the heat glows: faint red smouldering, amber heated, soul-blue superheated (muted, never near-white)
const ember = (x, y) => hash(x >> 1, y >> 1, 46) * 0.6 + hash(x, y, 47) * 0.4;
T.paint('heat_smouldering', (x, y) => { const n = ember(x, y); return n > 0.6 ? '#6a2a1a' : n > 0.3 ? '#4e2016' : '#2e1611'; });
T.paint('heat_kindled', (x, y) => { const n = ember(x, y); return n > 0.72 ? '#d08a3e' : n > 0.45 ? '#c06c2c' : n > 0.2 ? '#a4521f' : '#5a2a18'; });
T.paint('heat_seething', (x, y) => { const n = ember(x, y); return n > 0.72 ? '#6fa7cf' : n > 0.45 ? '#4f86b0' : n > 0.2 ? '#3a6688' : '#22384a'; });
// the gauge: a dark dial with ticks inside a dark brass bezel, and its needle
T.paint('dial', (x, y) => {
  const r = Math.hypot(x - 7.5, y - 7.5), a = Math.atan2(y - 7.5, x - 7.5) * 180 / Math.PI;
  if (r > 7.6) return null;
  if (r > 6.2) return mot('brassDark', x, y, 78);
  const deg = (a + 360 + 90) % 360; // 0 at the bottom
  if (r > 4.6 && deg > 45 && deg < 315 && Math.round(deg) % 30 < 8) return '#8a8c86';
  if (r > 4.6 && deg > 290 && deg < 315) return '#8a2f22'; // the red end of the scale
  return r < 1.2 ? '#4a5059' : '#1b1e22';
});
T.paint('needle', () => '#c9c2a8');
// preview only: a heap of emerald dust in the bowl (the renderer draws the real items)
T.paint('dust', (x, y) => (Math.hypot(x - 7.5, y - 7.5) > 7 ? null : hash(x, y, 81) > 0.6 ? '#4f8a5e' : '#3d6e4a'));
// the back: the gearbox face with a bearing ring round the shaft
T.paint('gearbox', (x, y) => {
  const r = Math.hypot(x - 7.5, y - 7.5);
  if (edge(x, y) === 0) return mot('andesite', x, y, 79);
  if (r < 3) return '#1f2327';
  if (r < 4.2) return '#5f656e';
  if ((x === 2 || x === 13) && (y === 2 || y === 13)) return P.brassDark[2];
  return FIELD(x, y, 80);
});

// ---------- helpers ----------
const AX = { x: 8, z: 8 }; // the screw's axis
// A round thing about a vertical axis from 16 staves turned up to 45 degrees about Y; teeth push every other stave out
function drumY(name, c, r, y0, y1, thick, tex, { teeth = 0, ends = {} } = {}) {
  const out = [];
  for (let s = 0; s < 16; s++) {
    const rr = r + (teeth && s % 2 === 0 ? teeth : 0);
    const w = 2 * r * Math.tan(Math.PI / 16) + 0.15;
    const phi = s * 22.5; // from -Z (north) round to +X
    const base = [0, 90, 180, 270].reduce((a, b) => (Math.abs(((phi - b + 540) % 360) - 180) < Math.abs(((phi - a + 540) % 360) - 180) ? b : a));
    const delta = ((phi - base + 540) % 360) - 180;
    let f, t;
    if (base === 0) { f = [c.x - w / 2, y0, c.z - rr]; t = [c.x + w / 2, y1, c.z - rr + thick]; }
    else if (base === 180) { f = [c.x - w / 2, y0, c.z + rr - thick]; t = [c.x + w / 2, y1, c.z + rr]; }
    else if (base === 90) { f = [c.x + rr - thick, y0, c.z - w / 2]; t = [c.x + rr, y1, c.z + w / 2]; }
    else { f = [c.x - rr, y0, c.z - w / 2]; t = [c.x - rr + thick, y1, c.z + w / 2]; }
    out.push(C(`${name}_${s}`, f, t, { all: tex, down: null, ...ends }, { rot: delta ? ['y', delta, [c.x, y0, c.z]] : undefined }));
  }
  return out;
}
// a flat round face on top: one plate, its texture cut out to a circle
const capY = (name, c, r, y0, y1, tex) => [C(name, [c.x - r, y0, c.z - r], [c.x + r, y1, c.z + r], { up: tex, all: null }, { uv: { up: [0, 0, 16, 16] } })];

// ---------- the press ----------
// the gauge's dial centre (its needle sits just in front of the dial face)
const GAUGE = { x: 4.5, y: 13.75, z: -LAYER };
const BIG = { r: 4.25 }, PIN = { x: 8, z: 13.5, r: 1.25 }; // the gears (big on the screw); centres 5.5 apart, so they mesh
function press() {
  const t = (k) => 'c:' + k, out = [];
  // the bed, its vents and the die bowl (the rim's lip at the plate's bottom height, the floor just under the contents)
  out.push(
    C('bed', [0, 0, 0.5], [16, 3, 16], { all: t('plate'), down: t('dark') }),
    C('bed_trim', [0, 2.5, 0.25], [16, 3, 0.5], { all: t('trim'), south: null }),
    C('vents', [3, 0.75, 0.5 - LAYER], [13, 2.25, 0.5], { north: t('vent'), all: t('dark'), south: null }),
    C('floor', [4, 3, 4], [12, 3.75, 12], { all: t('floor'), down: null }),
  );
  for (const [n, f, to] of [['n', [3, 3, 3], [13, 4.5, 4]], ['s', [3, 3, 12], [13, 4.5, 13]], ['w', [3, 3, 4], [4, 4.5, 12]], ['e', [12, 3, 4], [13, 4.5, 12]]])
    out.push(C(`rim_${n}`, f, to, { all: t('trim'), down: null }));
  // four square corner posts with andesite caps
  for (const [n, x0, z0] of [['nw', 0.5, 0.5], ['ne', 13, 0.5], ['sw', 0.5, 13], ['se', 13, 13]]) {
    out.push(C(`post_${n}`, [x0, 3, z0], [x0 + 2.5, 13, z0 + 2.5], { all: t('plate'), up: null, down: null }));
    out.push(C(`post_cap_${n}`, [x0 - 0.25, 12, z0 - 0.25], [x0 + 2.75, 13, z0 + 2.75], { all: t('trim') }));
  }
  // the crosshead, the screw's collar on it, and the back gearbox the shaft drives (the stub's end shows past it)
  out.push(
    C('crosshead', [0.5, 13, 0.5], [15.5, 14.5, 15.5], { all: t('plate') }),
    C('gearbox', [5, 3, 13.25], [11, 13, 15.75], { all: t('plate'), south: t('gearbox'), up: null }, { uv: { south: [0, 0, 16, 16] } }),
  );
  // the pressure gauge on the crosshead's front-left: a dial in a dark brass bezel
  out.push(C('gauge', [GAUGE.x - 1.75, GAUGE.y - 1.75, 0], [GAUGE.x + 1.75, GAUGE.y + 1.75, LAYER], { north: t('dial'), all: null }, { uv: { north: [0, 0, 16, 16] } }));
  out.push(C('gauge_back', [GAUGE.x - 1.5, GAUGE.y - 1.5, LAYER], [GAUGE.x + 1.5, GAUGE.y + 1.5, 0.5], { all: t('brass'), north: null }));
  return out;
}
// renderer parts, in the model frame
function screw() {
  const t = (k) => 'c:' + k;
  return [
    C('screw', [AX.x - 1.5, 6.5, AX.z - 1.5], [AX.x + 1.5, 15, AX.z + 1.5], { all: t('thread'), down: t('steel') }),
    ...drumY('gear', AX, BIG.r, 14.5, 15.5, 1, t('tooth'), { teeth: 0.5 }),
    ...capY('gear_face', AX, BIG.r - 0.9, 15.25, 15.25, t('gear_top')),
  ];
}
function pinion() {
  const t = (k) => 'c:' + k;
  return [
    // sunk a quarter pixel into the crosshead so its top doesn't z-fight the big gear's where their teeth meet
    ...drumY('pinion', PIN, PIN.r, 14.25, 15.25, 0.6, t('tooth'), { teeth: 0.45 }),
    ...capY('pinion_face', PIN, PIN.r - 0.45, 15, 15, t('gear_top')),
  ];
}
// the platen, modelled at y 0 (the renderer lifts it by the plate's travel), with the nut boss round the screw
function ram() {
  const t = (k) => 'c:' + k;
  return [
    C('platen', [3.5, 0, 3.5], [12.5, 1.5, 12.5], { all: t('plate') }),
    C('nut', [6, 1.5, 6], [10, 2, 10], { all: t('trim'), down: null }),
  ];
}
// the gauge needle, pointing straight down at rest (the renderer turns it about the dial's centre)
const needle = () => [C('needle', [GAUGE.x - 0.2, GAUGE.y - 1.4, GAUGE.z], [GAUGE.x + 0.2, GAUGE.y + 0.2, GAUGE.z], { north: 'c:needle', all: null })];
// the heat glow: plates just inside the rim's walls, and over the vents
function heat(level) {
  const t = 'c:heat_' + level, out = [];
  out.push(C('heat_vents', [3, 0.75, 0.5 - 2 * LAYER], [13, 2.25, 0.5 - 2 * LAYER], { north: t, all: null }, { glow: level !== 'smouldering' }));
  for (const [n, f, to, face] of [['n', [4, 3.75, 4 + LAYER], [12, 4.5, 4 + LAYER], 'south'], ['s', [4, 3.75, 12 - LAYER], [12, 4.5, 12 - LAYER], 'north'],
    ['w', [4 + LAYER, 3.75, 4], [4 + LAYER, 4.5, 12], 'east'], ['e', [12 - LAYER, 3.75, 4], [12 - LAYER, 4.5, 12], 'west']])
    out.push(C(`heat_rim_${n}`, f, to, { [face]: t, all: null }, { glow: level !== 'smouldering' }));
  return out;
}
const AXIS = 'create:block/axis', AXIS_TOP = 'create:block/axis_top';
const shaftStub = () => [C('shaft_stub', [6, 6, 12.5], [10, 10, 16], { north: AXIS_TOP, south: AXIS_TOP, all: AXIS },
  { uv: { north: [6, 6, 10, 10], south: [6, 6, 10, 10], east: [6, 0, 10, 4], west: [6, 0, 10, 4], up: [6, 0, 10, 4], down: [6, 0, 10, 4] }, frot: { east: 90, west: 90 } })];
// a stand-in for the dust in the bowl (the renderer draws the real items); preview only
const dust = () => [C('dust_preview', [5.5, 4.05, 5.5], [10.5, 4.05, 10.5], { up: 'c:dust', all: null })];

// ---------- preview ----------
const { cube, group, clip, write } = preview('pressure-crucible', { 'c:': T });
const cubes = (list) => list.map((c) => cube(c, [0, 0, 0]));
const outliner = [group('crucible', [8, 0, 8], [0, 0, 0], [
  group('body', [8, 0, 8], [0, 0, 0], cubes([...press(), ...dust()])),
  group('screw', [AX.x, 8, AX.z], [0, 0, 0], cubes(screw())),
  group('pinion', [PIN.x, 15, PIN.z], [0, 0, 0], cubes(pinion())),
  group('ram', [8, 0, 8], [0, 0, 0], cubes(ram())),
  group('needle', [GAUGE.x, GAUGE.y, GAUGE.z], [0, 0, 0], cubes(needle())),
  group('shaft', [8, 8, 14], [0, 0, 0], cubes(shaftStub())),
  group('heat_smouldering', [8, 0, 8], [0, 0, 0], cubes(heat('smouldering'))),
  group('heat_kindled', [8, 0, 8], [0, 0, 0], cubes(heat('kindled'))),
  group('heat_seething', [8, 0, 8], [0, 0, 0], cubes(heat('seething'))),
], { open: true })];

// The plate as the game moves it: down over exactly one item's press time, then easing back up (a quick rise slowing
// near the top) until it meets the next press and follows it down. 20 ticks a second.
const PLATE_TOP = 11, PLATE_BOTTOM = 4.5;
function plateCurve(pressSeconds, length) {
  const n = Math.round(length * 20), pressTicks = pressSeconds * 20, out = [];
  let plate = 0, timer = 0;
  for (let k = 0; k <= n; k++) {
    timer++;
    if (timer > pressTicks) timer = 1;
    const target = timer / pressTicks;
    plate = target >= plate ? target : Math.max(target, plate - Math.max(0.03, plate * 0.3));
    out.push(plate);
  }
  return (s) => out[Math.min(out.length - 1, Math.round(s * 20))];
}
const RATIO = BIG.r / PIN.r;
function running(rpm, pressSeconds, length, glow) {
  const plate = plateCurve(pressSeconds, length), deg = (s) => rpm * 6 * s;
  const tracks = {
    screw: { rotation: (s) => [0, -deg(s) % 360, 0] },
    pinion: { rotation: (s) => [0, (RATIO * deg(s)) % 360, 0] },
    ram: { position: (s) => [0, PLATE_TOP - plate(s) * (PLATE_TOP - PLATE_BOTTOM), 0] },
    needle: { rotation: (s) => [0, 0, -(-135 + 270 * plate(s))] },
    shaft: { rotation: (s) => [0, 0, deg(s) % 360] },
  };
  const steps = {};
  for (const h of ['smouldering', 'kindled', 'seething']) steps[`heat_${h}`] = { scale: [[0, h === glow ? SHOW : HIDE]] };
  return [tracks, steps];
}
// approved sounds: each clip's cues become sound keyframes on its Effects track, pointing at the real OGG files (the
// seamless loop render where there is one, since the running sounds repeat at their loop length)
const SOUND_DIR = path.resolve(__dirname, '../sound/pressure-crucible');
const CUES = JSON.parse(require('fs').readFileSync(path.join(SOUND_DIR, 'pressure-crucible.cues.json'), 'utf8'));
function sounds(clipName) {
  const cues = CUES.clips[clipName] || [];
  const file = (id) => [id + '_loop', id].map((n) => path.join(SOUND_DIR, 'real', n + '.ogg')).find((p) => require('fs').existsSync(p));
  const missing = [...new Set(cues.map(([, id]) => id).filter((id) => !file(id)))];
  if (missing.length) throw new Error(`clip ${clipName}: no real sound for ${missing.join(', ')}`);
  return cues.map(([t, id]) => [t, CUES.namespace + '.' + id, file(id).split(path.sep).join('/')]).sort((x, y) => x[0] - y[0]);
}
// heated: emeralds at 64 RPM, 2.5 s each; superheated: netherite, slower (shortened to 5 s for the preview)
clip('heated', 7.5, 'loop', ...running(64, 2.5, 7.5, 'kindled'), sounds('heated'));
clip('superheated', 10, 'loop', ...running(64, 5, 10, 'seething'), sounds('superheated'));
clip('cold', 1, 'hold', {}, { ram: { position: [[0, [0, PLATE_TOP, 0]]] }, needle: { rotation: [[0, [0, 0, 135]]] },
  heat_smouldering: { scale: [[0, HIDE]] }, heat_kindled: { scale: [[0, HIDE]] }, heat_seething: { scale: [[0, HIDE]] } });
clip('smouldering', 1, 'hold', {}, { ram: { position: [[0, [0, PLATE_TOP, 0]]] }, needle: { rotation: [[0, [0, 0, 135]]] },
  heat_smouldering: { scale: [[0, SHOW]] }, heat_kindled: { scale: [[0, HIDE]] }, heat_seething: { scale: [[0, HIDE]] } },
  sounds('smouldering'));

write(OUT, outliner);
module.exports = { press, screw, pinion, ram, needle, heat, shaftStub, T, AX, PIN, BIG, GAUGE, LAYER, PLATE_TOP, PLATE_BOTTOM };
