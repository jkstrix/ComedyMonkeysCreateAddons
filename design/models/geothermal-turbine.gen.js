// Geothermal Turbine redesign generator -> design/models/geothermal-turbine.bbmodel (Generic Model preview with clips).
// Formed frame (ElectricRenderers.Turbine): north-facing, the core at 0-16, the 3x3x3 from -16 to 32; -x is the viewer's right.
// See design/briefs/geothermal-turbine.md. Parts drawn by the renderer are separate lists, each in the formed frame.
const path = require('path');
const { P, C, textureSet, painters, mot, hash, edge, FIELD, HI, LO, SEAM, defaultUv, preview, SHOW, HIDE, smooth, lerp } = require('./process-box.lib.js');

const fs = require('fs');
// approved sounds: each clip's cues become sound keyframes on its Effects track, pointing at the real OGG files
const SOUND_DIR = path.resolve(__dirname, '../sound/geothermal-turbine');
const CUES = JSON.parse(fs.readFileSync(path.join(SOUND_DIR, 'geothermal-turbine.cues.json'), 'utf8'));
function sounds(clipName) {
  const cues = CUES.clips[clipName] || [];
  const missing = [...new Set(cues.map(([, id]) => id).filter((id) => !fs.existsSync(path.join(SOUND_DIR, 'real', id + '.ogg'))))];
  if (missing.length) throw new Error(`clip ${clipName}: no real sound for ${missing.join(', ')}`);
  return cues.map(([t, id]) => [t, CUES.namespace + '.' + id, path.join(SOUND_DIR, 'real', id + '.ogg').split(path.sep).join('/')]);
}
const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'geothermal-turbine.bbmodel');

// ---------- layout (pixels, formed frame) ----------
const ROTOR = { y: 20, z: 2 };          // turbine axis, along X
const HOUSING = { r: 10, x0: -8, x1: 24, window: [-4, 20] };
const BOILER = { y: 9, z: 18, r: 9, x0: -12, x1: 28 };
const GEN = { r: 7, x0: 24, x1: 30 };
const FIRE = { x0: -14, x1: 30, z0: -10, z1: 30, y0: -13, y1: 2 };
const CAB = { x0: -6, x1: 22, y0: -13, y1: 14, z0: -15, z1: -10 };
// Overlays sit LAYER px in front of what's behind them; anything closer z-fights in game (zfight-check.js)
const LAYER = 0.25;
const GAUGE = { y0: -3, y1: 5, lava: [16, 19], water: [11, 14] };   // sight glasses, 8 px tall level with the dial; fills scale up from y0
const DIAL_E = { x: 2, y: 1 };                                       // energy buffer dial on the panel
const DIAL_S = { y: BOILER.y, z: BOILER.z, x: BOILER.x0 - 0.5 };      // steam dial on the boiler's west end cap (its face)
const LAMPS = { output: [-4, -1], stall: [5, 8], y: [9, 12] };
const GRATES = [[-11, -9], [-7, -5], [-3, -1]];                     // firebox vent slots (y); flames come out of these
const GRATE_Z = [-6, 22];

// ---------- textures (geothermal_turbine_<key>.png) ----------
P.drum = ['#2f343b', '#33383f', '#363b42'];
P.coil = ['#7a4a2e', '#8a5434', '#9c5f3a'];
P.amber = ['#c76a2a', '#d0803a', '#d99a4a'];
P.lava = ['#8a2f22', '#c76a2a', '#d99a4a'];
P.water = ['#3f5d6e', '#47667a', '#4f6f80'];
const T = textureSet(); T.file = 'geothermal_turbine_';
// Big faces are tiled one block at a time; each tile is a crisp plate with a bevel, so the seams read as panel joints
T.paint('panel', painters.side('andesite', () => null));
T.paint('plate', (x, y) => (edge(x, y) === 0 ? (x === 0 || y === 0 ? HI : LO) : FIELD(x, y, 40)));
T.paint('steel', painters.steel());
T.paint('steel_light', painters.steelLight());
T.paint('dark', painters.dark());
T.paint('andesite', painters.andesite());
T.paint('brass', painters.brass());
T.paint('copper', painters.copper());
// Boiler plates: dark steel with a rivet row every 8 px along the drum
T.paint('drum', (x, y) => (x % 8 === 0 ? (y % 4 === 1 ? '#4a5059' : '#2a2f35') : (hash(x, y, 41) > 0.88 ? '#363b42' : '#33383f')));
// End caps: a disc of rings and a rivet circle, the same from any of the four turned plates
T.paint('cap', (x, y) => {
  const r = Math.hypot(x - 7.5, y - 7.5), a = Math.atan2(y - 7.5, x - 7.5);
  if (r > 7.6) return null;
  if (r > 6.0) return '#5a5f5c';
  // eight rivets on a circle
  const k = Math.round(a / (Math.PI / 4)), rx = 7.5 + 4.6 * Math.cos(k * Math.PI / 4), ry = 7.5 + 4.6 * Math.sin(k * Math.PI / 4);
  if (Math.abs(x - rx) < 0.6 && Math.abs(y - ry) < 0.6) return '#4a5059';
  if (r < 1.5) return P.andesite[1];
  if (r < 2.5) return '#2a2f35';
  return '#33383f';
});
// a pipe port: copper flange round a dark bore, with four bolts
T.paint('port', (x, y) => {
  const r = Math.hypot(x - 7.5, y - 7.5);
  if (r > 7.6) return null;
  if (r < 3.6) return r < 2.8 ? '#1f2327' : '#2a2f35';
  if ((x === 3 || x === 12) && (y === 3 || y === 12)) return P.andesite[1];
  return x + y < 15 ? '#9c5f3a' : '#7a4a2e';
});
T.paint('coil', (x, y) => (x % 3 === 2 ? '#5e3a24' : mot('coil', x, y, 44)));
T.paint('blade', (x, y) => (y === 0 || x === 0 ? '#6b7078' : mot('steelLight', x, y, 45)));
T.paint('grate', (x, y) => (x % 2 === 0 ? '#16191c' : '#1f2327'));
T.paint('glow', (x, y) => (x % 4 === 0 ? '#8a3a1c' : mot('amber', x, y, 46)));
T.paint('lamp_off', (x, y) => (hash(x, y, 47) > 0.8 ? '#2c3236' : '#22272b'));
T.paint('lamp_cyan', (x, y) => (edge(x % 4, y % 4) === 0 ? '#4f8f99' : '#6fb7c4'));
T.paint('lamp_red', (x, y) => (edge(x % 4, y % 4) === 0 ? '#6a2219' : '#8a2f22'));
// Gauge fills squash as they drop, so they're horizontal bands only
T.paint('lava', (x, y) => (hash(x, y, 51) > 0.8 ? '#b85e26' : hash(x, y, 52) > 0.85 ? '#cf7a34' : '#c76a2a'));
T.paint('water', (x, y) => (hash(x, y, 53) > 0.8 ? '#3f5d6e' : hash(x, y, 54) > 0.85 ? '#4f6f80' : '#47667a'));
T.paint('dial', (x, y) => {
  const r = Math.hypot(x - 7.5, y - 7.5);
  if (r > 7.6) return null;
  const a = (Math.atan2(y - 7.5, x - 7.5) * 180 / Math.PI + 360) % 360;
  if (r > 5.6 && r < 7 && (Math.round(a / 30) * 30 - a + 360) % 360 < 6 && !(a > 45 && a < 135)) return '#2f343b';
  return mot('andesite', x, y, 49);
});
T.paint('bezel', (x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); return r > 7.6 || r < 6.2 ? null : (x + y < 15 ? '#b08f45' : '#8f7638'); });
T.paint('ring', (x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); return r > 7.6 || r < 6.4 ? null : mot('andesite', x, y, 50); });
T.paint('needle', () => '#1f2327');
T.paint('glass', painters.glass());
T.paint('port_plate', (x, y) => {
  const r = Math.hypot(x - 7.5, y - 7.5);
  if (r < 2.6) return '#1f2327';
  if (r < 3.4) return '#2a2f35';
  if (r < 5.2) return x + y < 15 ? '#9c5f3a' : '#7a4a2e';
  if (edge(x, y) === 0) return x === 0 || y === 0 ? HI : LO;
  return FIELD(x, y, 55);
});

// ---------- geometry helpers ----------
const GRID = [-16, 0, 16, 32];
const cellBase = (a, b) => Math.floor(((a + b) / 2) / 16) * 16;
const localUv = (dir, f, t) => {
  const b = [0, 1, 2].map((i) => cellBase(f[i], t[i]));
  return defaultUv(dir, f.map((v, i) => v - b[i]), t.map((v, i) => v - b[i])).map((v) => Math.max(0, Math.min(16, v)));
};
// Split an unturned box at the block lines, so each piece's faces map to one 16 px texture tile; inner cut faces are dropped
function tiles(name, from, to, faces, o = {}) {
  const cuts = (a, b) => [a, ...GRID.filter((g) => g > a && g < b), b];
  const xs = cuts(from[0], to[0]), ys = cuts(from[1], to[1]), zs = cuts(from[2], to[2]);
  const out = [];
  for (let i = 0; i < xs.length - 1; i++) for (let j = 0; j < ys.length - 1; j++) for (let k = 0; k < zs.length - 1; k++) {
    const f = [xs[i], ys[j], zs[k]], t = [xs[i + 1], ys[j + 1], zs[k + 1]];
    const fc = { ...faces };
    const inner = { west: i > 0, east: i < xs.length - 2, down: j > 0, up: j < ys.length - 2, north: k > 0, south: k < zs.length - 2 };
    const uv = {};
    for (const d of ['north', 'east', 'south', 'west', 'up', 'down']) {
      if (inner[d]) fc[d] = null;
      else uv[d] = localUv(d, f, t);
    }
    out.push(C(`${name}_${i}${j}${k}`, f, t, fc, { ...o, uv }));
  }
  return out;
}
// A round drum along X from 16 staves (each a box at 0/90/180/270 degrees, turned up to 45 more about the axis).
// phi is measured from +Y towards -Z (the front). skip(phi, x0, x1) leaves a stave out (for windows).
function drum(name, ax, r, x0, x1, thick, tex, o = {}) {
  const w = 2 * r * Math.tan(Math.PI / 16) + 0.15 + (o.wExtra || 0), out = [];
  const xs = [x0, ...GRID.filter((g) => g > x0 && g < x1), x1];
  for (let s = 0; s < 16; s++) {
    const phi = -180 + s * 22.5 + (s === 0 ? 0 : 0);
    const base = [0, 90, 180, -90].reduce((a, b) => (Math.abs(((phi - b + 540) % 360) - 180) < Math.abs(((phi - a + 540) % 360) - 180) ? b : a));
    let delta = ((phi - base + 540) % 360) - 180;
    for (let i = 0; i < xs.length - 1; i++) {
      const [a, b] = [xs[i], xs[i + 1]];
      for (const [p0, p1] of (o.windowAt ? o.windowAt(phi, a, b) : [[a, b]])) {
        if (p1 - p0 < 0.01) continue;
        let f, t;
        if (base === 0) { f = [p0, ax.y + r - thick, ax.z - w / 2]; t = [p1, ax.y + r, ax.z + w / 2]; }
        else if (base === 180) { f = [p0, ax.y - r, ax.z - w / 2]; t = [p1, ax.y - r + thick, ax.z + w / 2]; }
        else if (base === 90) { f = [p0, ax.y - w / 2, ax.z - r]; t = [p1, ax.y + w / 2, ax.z - r + thick]; }
        else { f = [p0, ax.y - w / 2, ax.z + r - thick]; t = [p1, ax.y + w / 2, ax.z + r]; }
        const faces = { all: tex, east: o.ends || null, west: o.ends || null };
        // the right-hand turn about +X carries +Y towards +Z, the opposite way to phi
        out.push(C(`${name}_${s}_${i}`, f, t, faces, { uv: Object.fromEntries(['north', 'south', 'up', 'down'].map((d) => [d, localUv(d, f, t)])), rot: delta ? ['x', -delta, [p0, ax.y, ax.z]] : undefined }));
      }
    }
  }
  return out;
}
// A disc facing +/-X: one plate whose texture is cut out to a circle (cutout render), no rim faces
const disc = (name, ax, r, x0, x1, tex, westOnly = false) => [C(name, [x0, ax.y - r, ax.z - r], [x1, ax.y + r, ax.z + r], { east: westOnly ? null : tex, west: tex, all: null },
  { uv: { east: [0, 0, 16, 16], west: [0, 0, 16, 16] }, shade: true })];
// A disc facing -Z (front panels)
const discZ = (name, cx, cy, r, z0, z1, tex, northOnly = false) => [C(name, [cx - r, cy - r, z0], [cx + r, cy + r, z1], { north: tex, south: northOnly ? null : tex, all: null },
  { uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] } })];
// A round post along Y from 8 staves (east/south/west/north, each also turned 45 degrees about Y)
function postY(name, cx, cz, r, y0, y1, tex) {
  const w = 2 * r * Math.tan(Math.PI / 8) + 0.1, t = 0.6, out = [];
  for (const a of [0, 45]) for (const [n, f, to] of [['e', [cx + r - t, y0, cz - w / 2], [cx + r, y1, cz + w / 2]], ['w', [cx - r, y0, cz - w / 2], [cx - r + t, y1, cz + w / 2]],
    ['s', [cx - w / 2, y0, cz + r - t], [cx + w / 2, y1, cz + r]], ['n', [cx - w / 2, y0, cz - r], [cx + w / 2, y1, cz - r + t]]])
    out.push(C(`${name}_${n}_${a}`, f, to, { all: tex }, { uv: Object.fromEntries(['north', 'east', 'south', 'west', 'up', 'down'].map((d) => [d, localUv(d, f, to)])), rot: a ? ['y', a, [cx, y0, cz]] : undefined }));
  return out;
}

// ---------- the static formed machine (block/geothermal_turbine) ----------
function machine() {
  const out = [];
  const W = HOUSING.window;
  // plinth and firebox: tiled plates
  out.push(...tiles('plinth', [-16, -16, -16], [32, -13, 32], { all: 'panel', down: 'port_plate' }));
  out.push(...tiles('firebox', [FIRE.x0, FIRE.y0, FIRE.z0], [FIRE.x1, FIRE.y1, FIRE.z1], { all: 'plate', down: null }));
  // firebox grates: dark slots on both sides (the glow model lights them while running)
  for (const [i, [y0, y1]] of GRATES.entries()) {
    out.push(C(`grate_w_${i}`, [FIRE.x0 - 0.5, y0, GRATE_Z[0]], [FIRE.x0, y1, GRATE_Z[1]], { west: 'grate', all: 'steel', east: null }));
    out.push(C(`grate_e_${i}`, [FIRE.x1, y0, GRATE_Z[0]], [FIRE.x1 + 0.5, y1, GRATE_Z[1]], { east: 'grate', all: 'steel', west: null }));
  }
  // firebox doors either side of the cabinet, with dark slits the glow lights
  for (const [n, x0, x1] of [['w', -13, -8], ['e', 24, 29]]) {
    out.push(C(`door_${n}`, [x0, -11, FIRE.z0 - 0.6], [x1, -2, FIRE.z0], { all: 'steel', north: 'plate' }, { uv: { north: [0, 0, 16, 16] } }));
    out.push(C(`door_${n}_slit`, [x0 + 1, -6, FIRE.z0 - 0.6 - LAYER], [x1 - 1, -4, FIRE.z0 - 0.6], { north: 'grate', all: null }));
    out.push(C(`door_${n}_hinge`, [x0 - 0.5, -10, FIRE.z0 - 0.8], [x0, -3, FIRE.z0 - 0.2], { all: 'andesite' }));
  }
  // control cabinet on the controller cell
  out.push(...tiles('cabinet', [CAB.x0, CAB.y0, CAB.z0], [CAB.x1, CAB.y1, CAB.z1], { all: 'plate', north: 'panel' }));
  out.push(C('cabinet_cap', [CAB.x0 - 0.5, CAB.y1, CAB.z0 - 0.5], [CAB.x1 + 0.5, CAB.y1 + 1, CAB.z1], { all: 'andesite' }));
  // sight glasses: dark backing, glass, brass bezel
  for (const [n, [x0, x1]] of [['lava', GAUGE.lava], ['water', GAUGE.water]]) {
    out.push(C(`glass_back_${n}`, [x0, GAUGE.y0, CAB.z0 - LAYER], [x1, GAUGE.y1, CAB.z0 - LAYER + 0.05], { north: 'dark', all: null }));
    out.push(C(`glass_${n}`, [x0, GAUGE.y0, CAB.z0 - 3 * LAYER], [x1, GAUGE.y1, CAB.z0 - 3 * LAYER], { north: 'glass', all: null }, { uv: { north: [0, 4, 4, 12] }, shade: false }));
    for (const [bn, f, t] of [['l', [x0 - 0.5, GAUGE.y0 - 0.5], [x0, GAUGE.y1 + 0.5]], ['r', [x1, GAUGE.y0 - 0.5], [x1 + 0.5, GAUGE.y1 + 0.5]],
      ['b', [x0, GAUGE.y0 - 0.5], [x1, GAUGE.y0]], ['t', [x0, GAUGE.y1], [x1, GAUGE.y1 + 0.5]]])
      out.push(C(`bezel_${n}_${bn}`, [f[0], f[1], CAB.z0 - 1], [t[0], t[1], CAB.z0], { all: 'brass', south: null }));
  }
  // energy dial: brass bezel, andesite face (the needle is a part)
  out.push(...discZ('dial_e_bezel', DIAL_E.x, DIAL_E.y, 4.2, CAB.z0 - LAYER, CAB.z0 - LAYER + 0.05, 'bezel', true));
  out.push(...discZ('dial_e_face', DIAL_E.x, DIAL_E.y, 3.7, CAB.z0 - 2 * LAYER, CAB.z0 - 2 * LAYER + 0.05, 'dial', true));
  // lamp lenses (dark until the lamp part lights them) in brass bezels
  for (const [n, [x0, x1]] of [['output', LAMPS.output], ['stall', LAMPS.stall]]) {
    out.push(C(`lens_${n}`, [x0, LAMPS.y[0], CAB.z0 - LAYER], [x1, LAMPS.y[1], CAB.z0], { all: 'lamp_off', south: null }));
    for (const [bn, f, t] of [['l', [x0 - 0.5, LAMPS.y[0] - 0.5], [x0, LAMPS.y[1] + 0.5]], ['r', [x1, LAMPS.y[0] - 0.5], [x1 + 0.5, LAMPS.y[1] + 0.5]],
      ['b', [x0, LAMPS.y[0] - 0.5], [x1, LAMPS.y[0]]], ['t', [x0, LAMPS.y[1]], [x1, LAMPS.y[1] + 0.5]]])
      out.push(C(`lens_${n}_bezel_${bn}`, [f[0], f[1], CAB.z0 - 2 * LAYER], [t[0], t[1], CAB.z0], { all: 'brass', south: null }));
  }
  // boiler drum behind, with end caps and riveted bands
  out.push(...drum('boiler', BOILER, BOILER.r, BOILER.x0, BOILER.x1, 1.5, 'drum'));
  out.push(...disc('boiler_cap_w', BOILER, BOILER.r, BOILER.x0 - 0.5, BOILER.x0 + 0.5, 'cap'));
  out.push(...disc('boiler_cap_e', BOILER, BOILER.r, BOILER.x1 - 0.5, BOILER.x1 + 0.5, 'cap'));
  for (const bx of [-6, 8, 22]) out.push(...drum(`boiler_band_${bx}`, BOILER, BOILER.r + 0.4, bx, bx + 1, 0.5, 'andesite', { wExtra: 0.6 }));
  // steam dial on the boiler's west end
  out.push(...disc('dial_s_bezel', DIAL_S, 3.4, DIAL_S.x - LAYER, DIAL_S.x - LAYER + 0.05, 'bezel', true), ...disc('dial_s_face', DIAL_S, 3, DIAL_S.x - 2 * LAYER, DIAL_S.x - 2 * LAYER + 0.05, 'dial', true));
  // turbine housing: staves round the rotor, with the front staves left out across the window
  const frontStave = (phi) => phi >= 56 && phi <= 124;
  out.push(...drum('housing', ROTOR, HOUSING.r, HOUSING.x0, HOUSING.x1, 1.5, 'drum', {
    windowAt: (phi, a, b) => (!frontStave(phi) ? [[a, b]] : [[a, Math.min(b, W[0])], [Math.max(a, W[1]), b]].filter(([p, q]) => q > p)),
  }));
  out.push(...disc('housing_cap_w', ROTOR, HOUSING.r, HOUSING.x0 - 0.5, HOUSING.x0 + 0.5, 'cap'));
  // the window: glass across the opening in a brass rim
  const wy = HOUSING.r * Math.sin((90 - 56) * Math.PI / 180) + 0.3, wz = ROTOR.z - HOUSING.r * Math.cos((90 - 56) * Math.PI / 180);
  out.push(C('window_glass', [W[0], ROTOR.y - wy, wz], [W[1], ROTOR.y + wy, wz], { north: 'glass', south: 'glass', all: null }, { uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] }, shade: false }));
  out.push(C('window_rim_t', [W[0] - 0.5, ROTOR.y + wy - 0.6, wz - 0.6], [W[1] + 0.5, ROTOR.y + wy + 0.2, wz + 0.4], { all: 'brass' }));
  out.push(C('window_rim_b', [W[0] - 0.5, ROTOR.y - wy - 0.2, wz - 0.6], [W[1] + 0.5, ROTOR.y - wy + 0.6, wz + 0.4], { all: 'brass' }));
  for (const x of [W[0] - 0.5, W[1]]) out.push(C(`window_rim_${x}`, [x, ROTOR.y - wy + 0.6, wz - 0.6], [x + 0.5, ROTOR.y + wy - 0.6, wz + 0.4], { all: 'brass', up: null, down: null }));
  // bearing pedestals and the shaft's west end
  out.push(C('pedestal_w', [-11, FIRE.y1, -3], [-6, ROTOR.y - 1, 7], { all: 'plate', down: null }, { uv: { north: [0, 0, 5, 16], south: [0, 0, 5, 16], east: [0, 0, 10, 16], west: [0, 0, 10, 16], up: [0, 0, 5, 10] } }));
  out.push(C('bearing_w', [-10.5, ROTOR.y - 2, ROTOR.z - 2], [-8, ROTOR.y + 2, ROTOR.z + 2], { all: 'steel', west: 'steel_light' }));
  // generator on the east end: copper windings in a steel cage, on its own pedestal
  out.push(C('pedestal_e', [23, FIRE.y1, -4], [29.5, ROTOR.y - GEN.r - 0.5, 8], { all: 'plate', down: null }, { uv: { north: [0, 0, 6.5, 11], south: [0, 0, 6.5, 11], east: [0, 0, 12, 11], west: [0, 0, 12, 11], up: [0, 0, 6.5, 12] } }));
  out.push(...drum('generator', ROTOR, GEN.r, GEN.x0, GEN.x1, 1.2, 'coil'), ...disc('generator_cap', ROTOR, GEN.r, GEN.x1 - 0.5, GEN.x1, 'cap'));
  for (const bx of [GEN.x0, GEN.x1 - 1]) out.push(...drum(`gen_band_${bx}`, ROTOR, GEN.r + 0.4, bx, bx + 1, 0.5, 'steel', { wExtra: 0.6 }));
  // steam pipe from the boiler's top into the turbine's back, with flanges
  out.push(C('pipe_up', [6, BOILER.y + BOILER.r - 1, 13.5], [9, 21, 16.5], { all: 'copper', up: null }));
  out.push(C('pipe_over', [6, 21, ROTOR.z + HOUSING.r - 1], [9, 24, 16.5], { all: 'copper', down: null }));
  out.push(C('pipe_flange', [5.5, 20.5, ROTOR.z + HOUSING.r - 0.5], [9.5, 24.5, ROTOR.z + HOUSING.r + 0.5], { all: 'andesite' }));
  // exhaust stack on the housing's top, where the steam comes out (steam spawns at y 31.2 over x/z 3-13)
  out.push(...postY('stack', 8, 8, 2.5, 22, 30.5, 'steel'));
  out.push(...postY('stack_lip', 8, 8, 3, 30, 31, 'andesite'));
  return out;
}

// ---------- renderer-drawn parts (each in the formed frame) ----------
// glow: amber in the grates and door slits, drawn only while running
function glow() {
  const out = [];
  for (const [i, [y0, y1]] of GRATES.entries()) {
    out.push(C(`glow_w_${i}`, [FIRE.x0 - 0.5 - LAYER, y0 + 0.2, GRATE_Z[0] + 0.5], [FIRE.x0 - 0.5 - LAYER, y1 - 0.2, GRATE_Z[1] - 0.5], { west: 'glow', all: null }, { shade: false }));
    out.push(C(`glow_e_${i}`, [FIRE.x1 + 0.5 + LAYER, y0 + 0.2, GRATE_Z[0] + 0.5], [FIRE.x1 + 0.5 + LAYER, y1 - 0.2, GRATE_Z[1] - 0.5], { east: 'glow', all: null }, { shade: false }));
  }
  for (const [n, x0, x1] of [['w', -13, -8], ['e', 24, 29]]) out.push(C(`glow_door_${n}`, [x0 + 1.2, -5.8, FIRE.z0 - 0.6 - 2 * LAYER], [x1 - 1.2, -4.2, FIRE.z0 - 0.6 - 2 * LAYER], { north: 'glow', all: null }, { shade: false }));
  return out;
}
// rotor: a shaft and three rows of eight paddle blades, spinning about X through ROTOR
function rotor() {
  const R = HOUSING.r - 1.8, out = [C('shaft', [HOUSING.x0 - 3, ROTOR.y - 1, ROTOR.z - 1], [GEN.x0 + 1, ROTOR.y + 1, ROTOR.z + 1], { all: 'dark' })];
  for (const [i, x] of [-1.5, 7.5, 16.5].entries()) {
    const x0 = x - 1.5, x1 = x + 1.5;
    out.push(...disc(`hub_${i}`, ROTOR, 2.4, x0 + 0.5, x1 - 0.5, 'cap'));
    for (const a of [0, 45, -45]) out.push(C(`blades_v_${i}_${a}`, [x0, ROTOR.y - R, ROTOR.z - 0.3], [x1, ROTOR.y + R, ROTOR.z + 0.3], { all: 'blade' },
      { uv: { north: [0, 0, 3, 16], south: [0, 0, 3, 16], east: [0, 0, 1, 16], west: [0, 0, 1, 16], up: [0, 0, 3, 1], down: [0, 0, 3, 1] }, rot: a ? ['x', a, [x0, ROTOR.y, ROTOR.z]] : undefined }));
    out.push(C(`blades_h_${i}`, [x0, ROTOR.y - 0.3, ROTOR.z - R], [x1, ROTOR.y + 0.3, ROTOR.z + R], { all: 'blade' },
      { uv: { up: [0, 0, 3, 16], down: [0, 0, 3, 16], east: [0, 0, 16, 1], west: [0, 0, 16, 1], north: [0, 0, 3, 1], south: [0, 0, 3, 1] } }));
  }
  return out;
}
const needleEnergy = () => [C('needle_e', [DIAL_E.x - 0.25, DIAL_E.y, CAB.z0 - 3 * LAYER], [DIAL_E.x + 0.25, DIAL_E.y + 3.1, CAB.z0 - 3 * LAYER + 0.05], { north: 'needle', all: null })];
const needleSteam = () => [C('needle_s', [DIAL_S.x - 3 * LAYER, DIAL_S.y, DIAL_S.z - 0.25], [DIAL_S.x - 3 * LAYER + 0.05, DIAL_S.y + 2.5, DIAL_S.z + 0.25], { west: 'needle', all: null })];
const lamp = (n, tex) => { const [x0, x1] = LAMPS[n]; return [C(`lamp_${n}`, [x0 + 0.2, LAMPS.y[0] + 0.2, CAB.z0 - 2 * LAYER], [x1 - 0.2, LAMPS.y[1] - 0.2, CAB.z0 - 2 * LAYER + 0.05], { north: tex, all: null }, { shade: false })]; };
const gaugeFill = (n, tex) => { const [x0, x1] = GAUGE[n]; return [C(`fill_${n}`, [x0, GAUGE.y0, CAB.z0 - 2 * LAYER], [x1, GAUGE.y1, CAB.z0 - 2 * LAYER + 0.05], { north: tex, all: null }, { uv: { north: [0, 0, 16, 16] }, shade: false })]; };

// ---------- unformed blocks (0-16) ----------
const casing = () => [
  C('casing', [0, 0, 0], [16, 16, 16], { all: 'panel' }),
  // a copper port flange on each side face: every face is a lava, water and FE port
  ...['north', 'south', 'east', 'west'].map((d) => {
    const at = { north: [[5, 5, -0.4], [11, 11, 0]], south: [[5, 5, 16], [11, 11, 16.4]], west: [[-0.4, 5, 5], [0, 11, 11]], east: [[16, 5, 5], [16.4, 11, 11]] }[d];
    return C(`port_${d}`, at[0], at[1], { all: null, [d]: 'port' }, { uv: { [d]: [0, 0, 16, 16] } });
  }),
];
function core() {
  const out = [];
  // a cage: corner posts and rails, with a rotor wheel inside
  for (const [x, z] of [[0, 0], [14, 0], [0, 14], [14, 14]]) out.push(C(`post_${x}_${z}`, [x, 0, z], [x + 2, 16, z + 2], { all: 'plate' }, { uv: { north: [x, 0, x + 2, 16], south: [x, 0, x + 2, 16], east: [z, 0, z + 2, 16], west: [z, 0, z + 2, 16], up: [0, 0, 2, 2], down: [0, 0, 2, 2] } }));
  for (const y of [0, 14]) for (const [n, f, t] of [['n', [2, y, 0], [14, y + 2, 2]], ['s', [2, y, 14], [14, y + 2, 16]], ['w', [0, y, 2], [2, y + 2, 14]], ['e', [14, y, 2], [16, y + 2, 14]]])
    out.push(C(`rail_${n}_${y}`, f, t, { all: 'andesite' }));
  const ax = { y: 8, z: 8 };
  out.push(C('core_shaft', [2, 7, 7], [14, 9, 9], { all: 'dark' }));
  for (const [i, x] of [4.5, 8, 11.5].entries()) {
    for (const a of [0, 45, -45]) out.push(C(`core_blades_${i}_${a}`, [x - 1, ax.y - 5.5, ax.z - 0.3], [x + 1, ax.y + 5.5, ax.z + 0.3], { all: 'blade' }, { rot: a ? ['x', a, [x, ax.y, ax.z]] : undefined }));
    out.push(C(`core_blades_h_${i}`, [x - 1, ax.y - 0.3, ax.z - 5.5], [x + 1, ax.y + 0.3, ax.z + 5.5], { all: 'blade' }));
  }
  return out;
}
const controller = () => [
  C('controller', [0, 0, 0], [16, 16, 16], { all: 'panel', north: 'plate' }, { uv: { north: [0, 0, 16, 16] } }),
  C('c_lava', [12.8, 5.5, -0.3], [14.8, 10, 0], { north: 'lava', all: 'brass', south: null }, { uv: { north: [0, 6, 16, 16] } }),
  C('c_water', [10, 5.5, -0.3], [12, 10, 0], { north: 'water', all: 'brass', south: null }, { uv: { north: [0, 4, 16, 16] } }),
  ...discZ('c_dial_bezel', 5, 8, 3, -0.2, -0.15, 'bezel'), ...discZ('c_dial', 5, 8, 2.6, -0.3, -0.25, 'dial'),
  C('c_lamp', [2, 12.5, -0.3], [4, 14, 0], { north: 'lamp_off', all: 'brass', south: null }),
];

// ---------- assemble the preview ----------
const { cube, group, clip, write } = preview('geothermal-turbine', { 'own:': T });
const off0 = [0, 0, 0];
const cubes = (list, off = off0) => list.map((c) => cube(c, off));
const outliner = [
  group('formed', [8, 8, 8], off0, [
    group('machine', [8, 8, 8], off0, cubes(machine())),
    group('glow', [8, 8, 8], off0, cubes(glow())),
    group('rotor', [8, ROTOR.y, ROTOR.z], off0, cubes(rotor())),
    group('needle_energy', [DIAL_E.x, DIAL_E.y, CAB.z0 - 3 * LAYER], off0, cubes(needleEnergy())),
    group('needle_steam', [DIAL_S.x, DIAL_S.y, DIAL_S.z], off0, cubes(needleSteam())),
    group('lamp_output', [8, 8, 8], off0, cubes(lamp('output', 'lamp_cyan'))),
    group('lamp_stall', [8, 8, 8], off0, cubes(lamp('stall', 'lamp_red'))),
    group('gauge_lava', [8, GAUGE.y0, CAB.z0], off0, cubes(gaugeFill('lava', 'lava'))),
    group('gauge_water', [8, GAUGE.y0, CAB.z0], off0, cubes(gaugeFill('water', 'water'))),
  ], { open: true }),
  group('unformed', [56, -8, 8], off0, [
    group('u_controller', [56, -8, -8], [48, -16, -16], cubes(controller(), [48, -16, -16])),
    group('u_core', [76, -8, -8], [68, -16, -16], cubes(core(), [68, -16, -16])),
    group('u_casing', [96, -8, -8], [88, -16, -16], cubes(casing(), [88, -16, -16])),
  ]),
];

// ---------- clips ----------
// run: idle, lava and water go in and it winds up, runs flat out, then the power stops being taken: it stalls and coasts down
const RUN = { up: 1, full: 3.5, stall: 6.5, end: 9 };
const loadAt = (s) => (s < RUN.up ? 0 : s < RUN.full ? smooth((s - RUN.up) / (RUN.full - RUN.up)) : s < RUN.stall ? 1 : 0);
// rotor speed eases after the load (it has inertia), 30 deg a tick (600 deg/s) flat out
const speedAt = (s) => (s < RUN.up ? 0 : s < RUN.stall ? smooth(Math.min(1, (s - RUN.up) / 2.8)) : Math.max(0, 1 - (s - RUN.stall) / 2.2) ** 2);
const angle = (() => { const cache = []; let a = 0; for (let k = 0; k <= RUN.end * 100; k++) { cache.push(a); a += speedAt(k / 100) * 600 / 100; } return (s) => cache[Math.min(cache.length - 1, Math.round(s * 100))]; })();
const bufferAt = (s) => (s < RUN.up ? 0.1 : s < RUN.stall ? lerp(0.1, 0.55, (s - RUN.up) / (RUN.stall - RUN.up)) : Math.min(1, 0.55 + (s - RUN.stall) * 0.4));
const running = (s) => s >= RUN.up && s < RUN.stall + 2 / 1; // the 2 s hang after the last power made
clip('run', RUN.end, 'loop', {
  rotor: { rotation: (s) => [-angle(s) % 360, 0, 0] },
  needle_energy: { rotation: (s) => [0, 0, -135 + 270 * bufferAt(s)] },
  needle_steam: { rotation: (s) => [70 - 140 * loadAt(s), 0, 0] },
  glow: { scale: (s) => (running(s) && s < RUN.stall ? SHOW : HIDE) },
  lamp_output: { scale: (s) => (running(s) && s < RUN.stall ? SHOW : HIDE) },
  lamp_stall: { scale: (s) => (s >= RUN.stall ? SHOW : HIDE) },
  gauge_lava: { scale: (s) => [1, s < RUN.up ? 0.15 : lerp(0.9, 0.75, (s - RUN.up) / (RUN.end - RUN.up)), 1] },
  gauge_water: { scale: (s) => [1, s < RUN.up ? 0.2 : lerp(0.95, 0.6, (s - RUN.up) / (RUN.end - RUN.up)), 1] },
}, {}, sounds('run'));
// idle: formed, fuelled, nothing running (the resting look)
clip('idle', 1, 'hold', {}, { glow: { scale: [[0, HIDE]] }, lamp_output: { scale: [[0, HIDE]] }, lamp_stall: { scale: [[0, HIDE]] },
  needle_energy: { rotation: [[0, [0, 0, -135]]] }, needle_steam: { rotation: [[0, [70, 0, 0]]] } }, sounds('idle'));

write(OUT, outliner);
module.exports = { machine, glow, rotor, needleEnergy, needleSteam, lamp, gaugeFill, casing, core, controller, T, ROTOR, GAUGE, DIAL_E, DIAL_S, CAB, GRATES, FIRE, BOILER, HOUSING };
