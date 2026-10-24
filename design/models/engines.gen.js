// Magma Engine and Supercharged Magma Engine redesign generator -> design/models/engines.bbmodel (Generic Model preview with clips).
// Built on the family kit in the game's model space (shaft north, z 0), to design/briefs/magma-engine.md and
// design/briefs/supercharged-magma-engine.md: a V-twin on a crankcase, pistons pumping and a flywheel turning with the
// shaft, lava ports at the middle of every face but the front. Each engine's two states come from one part list;
// parts flagged glow get NeoForge's full-bright light in game.
const fs = require('fs');
const path = require('path');
const { P, C, textureSet, painters, mot, hash, edge, FIELD, HI, LO, preview, SHOW, HIDE } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'engines.bbmodel');
const LAYER = 0.25; // overlays sit this far in front of what's behind them (zfight-check.js)

// ---------- textures: one set per engine (magma_engine_<key>, supercharged_magma_engine_<key>) ----------
P.brassDark = ['#7d6630', '#8f7638', '#9c8040'];
function paintEngine(T, sup) {
  const trim = sup ? 'brassDark' : 'andesite';
  T.paint('plate', (x, y) => (edge(x, y) === 0 ? (x === 0 || y === 0 ? HI : LO) : FIELD(x, y, 40)));
  T.paint('panel', painters.side(sup ? 'brass' : 'andesite', () => null));
  T.paint('dark', painters.dark());
  T.paint('steel', painters.steel());
  T.paint('rod', (x, y) => (x % 4 === 0 ? '#7a8089' : mot('steelLight', x, y, 42)));
  T.paint('trim', (x, y) => mot(trim, x, y, 43));
  T.paint('copper', painters.copper());
  // a port's face: a copper flange round a dark bore
  T.paint('inlet', (x, y) => {
    const r = Math.hypot(x - 7.5, y - 7.5);
    if (r < 3.4) return '#1f2327';
    if (r < 4.2) return '#2a2f35';
    if (r < 6) return x + y < 15 ? '#9c5f3a' : '#7a4a2e';
    if ((x === 1 || x === 14) && (y === 1 || y === 14)) return P[trim][2];
    if (edge(x, y) === 0) return x === 0 || y === 0 ? HI : LO;
    return FIELD(x, y, 54);
  });
  // the underside, with the bottom port's flange in the middle
  T.paint('base', (x, y) => {
    const r = Math.hypot(x - 7.5, y - 7.5);
    if (r < 2.4) return '#1f2327';
    if (r < 3) return '#2a2f35';
    if (Math.abs(x - 7.5) < 3.6 && Math.abs(y - 7.5) < 3.6) return x + y < 15 ? '#9c5f3a' : '#7a4a2e';
    return mot('dark', x, y, 44);
  });
  // cylinder fins: polished edges on dark steel
  T.paint('fin', (x, y) => (y % 4 === 0 ? '#5f656e' : mot('steel', x, y, 45)));
  T.paint('barrel', (x, y) => (y % 2 === 0 ? '#1f2327' : '#2a2f35'));
  T.paint('exhaust', (x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); return r < 5 ? '#16191c' : mot(trim, x, y, 46); });
  T.paint('exhaust_lit', (x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); return r < 3 ? '#d08a3e' : r < 5 ? '#a4521f' : mot(trim, x, y, 46); });
  // the firebox grate behind the flywheel: dark ash cold, blotchy embers burning (no stripes)
  const ember = (x, y) => hash(x >> 1, y >> 1, 46) * 0.6 + hash(x, y, 47) * 0.4;
  T.paint('grate', (x, y) => (ember(x, y) < 0.25 ? '#2a1f1a' : hash(x, y, 48) > 0.8 ? '#1f2327' : '#1a1d20'));
  T.paint('glow', (x, y) => { const n = ember(x, y); return n > 0.72 ? '#d08a3e' : n > 0.45 ? '#c06c2c' : n > 0.2 ? '#a4521f' : '#5a2a18'; });
  // the drum: staves with a seam and rivets; its end caps
  T.paint('drum', (x, y) => (x % 8 === 0 ? '#2a2f35' : y % 8 === 4 && x % 8 === 4 ? P[trim][2] : FIELD(x, y, 56)));
  T.paint('cap', (x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); if (r > 7.6) return null; return r > 6.6 ? mot(trim, x, y, 57) : FIELD(x, y, 58); });
  T.paint('grate_round', (x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); if (r > 7.6) return null; return r > 6.6 ? mot(trim, x, y, 57) : ember(x, y) < 0.25 ? '#2a1f1a' : '#1a1d20'; });
  T.paint('glow_round', (x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); if (r > 7.6) return null; if (r > 6.6) return mot(trim, x, y, 57); const n = ember(x, y); return n > 0.72 ? '#d08a3e' : n > 0.45 ? '#c06c2c' : n > 0.2 ? '#a4521f' : '#5a2a18'; });
  // the flywheel: rim staves, spokes and hub
  T.paint('rim', () => '#4a5059');
  T.paint('tread', (x, y) => (x % 4 === 0 ? '#3c424a' : mot('steelLight', x, y, 49)));
  T.paint('spoke', (x, y) => mot('steel', x, y, 50));
  T.paint('hub', (x, y) => (edge(x, y) <= 1 ? mot(trim, x, y, 51) : mot('dark', x, y, 55)));
  if (sup) {
    T.paint('blower', (x, y) => (x % 3 === 0 ? '#24282d' : mot('steel', x, y, 52)));
    T.paint('band', (x, y) => mot('brass', x, y, 53));
    T.paint('pulley', (x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); if (r > 7.6) return null; return r < 2 ? '#1f2327' : r > 6.4 ? '#24221f' : (Math.round(Math.atan2(y - 7.5, x - 7.5) / (Math.PI / 3)) % 2 ? '#8f7638' : '#6b6e6a'); });
    T.paint('belt', (x, y) => (y % 3 === 0 ? '#2a2724' : '#1c1a18'));
  }
}
const ENG = textureSet(); ENG.file = 'magma_engine_';
const SUP = textureSet(); SUP.file = 'supercharged_magma_engine_';
paintEngine(ENG, false);
paintEngine(SUP, true);

// ---------- helpers ----------
const AX = { x: 8, y: 8 }; // the crank: the output shaft's axis
const DRUM_R = 5; // the body drum round it
const BARREL0 = 6.6, FINS = [6.6, 7.3, 8.0]; // where the barrels start, and their fins (out along the cylinder)
const DEG = Math.PI / 180;
// A round drum along Z from 16 staves (as electric.gen.js); ends: textures for the staves' north/south faces
function drumZ(name, ax, r, z0, z1, thick, tex, ends = {}) {
  const w = 2 * r * Math.tan(Math.PI / 16) + 0.15, out = [];
  for (let s = 0; s < 16; s++) {
    const phi = s * 22.5;
    const base = [0, 90, 180, 270].reduce((a, b) => (Math.abs(((phi - b + 540) % 360) - 180) < Math.abs(((phi - a + 540) % 360) - 180) ? b : a));
    const delta = ((phi - base + 540) % 360) - 180;
    let f, t;
    if (base === 0) { f = [ax.x - w / 2, ax.y + r - thick, z0]; t = [ax.x + w / 2, ax.y + r, z1]; }
    else if (base === 180) { f = [ax.x - w / 2, ax.y - r, z0]; t = [ax.x + w / 2, ax.y - r + thick, z1]; }
    else if (base === 90) { f = [ax.x + r - thick, ax.y - w / 2, z0]; t = [ax.x + r, ax.y + w / 2, z1]; }
    else { f = [ax.x - r, ax.y - w / 2, z0]; t = [ax.x - r + thick, ax.y + w / 2, z1]; }
    out.push(C(`${name}_${s}`, f, t, { all: tex, north: null, south: null, ...ends }, { rot: delta ? ['z', -delta, [ax.x, ax.y, z0]] : undefined }));
  }
  return out;
}
const disc = (name, ax, r, z0, z1, tex, side = 'north') => [C(name, [ax.x - r, ax.y - r, z0], [ax.x + r, ax.y + r, z1], { [side]: tex, all: null }, { uv: { [side]: [0, 0, 16, 16] } })];
// A box built standing up from the crank (dx across, dy out along the cylinder), then leaned with its cylinder:
// +45 about Z leans it to the upper left (-X), -45 to the upper right
const lean = (side) => (side === 'left' ? 45 : -45);
const cyl = (side, name, dx0, dy0, dx1, dy1, z0, z1, faces, o = {}) =>
  C(`${name}_${side}`, [AX.x + dx0, AX.y + dy0, z0], [AX.x + dx1, AX.y + dy1, z1], faces, { ...o, rot: ['z', lean(side), [AX.x, AX.y, 8]] });

// ---------- the engines ----------
// state: 'off' or 'lit'; sup: the Supercharged engine (brass trim, the supercharger)
function engine(state, sup) {
  const t = (k) => (sup ? 's:' : 'e:') + k, lit = state === 'lit', out = [];
  // The body is one round drum along the shaft (the crankcase and the lava tank in one), cradled on the plinth.
  // Each port stands out on a short boss from the drum to the middle of its face. The bottom port's flange is
  // painted on the plinth's underside.
  out.push(
    C('plinth', [0.5, 0, 1.5], [15.5, 1, 15.5], { all: t('trim'), down: t('base') }),
    ...drumZ('drum', AX, DRUM_R, 2.5, 14.5, 1.2, t('drum'), { north: t('rim'), south: t('rim') }),
    // the firebox: a round grate in the drum's front, seen through the flywheel's spokes
    ...disc('firebox', AX, DRUM_R - 0.6, 2.5 + LAYER, 2.55 + LAYER, t(lit ? 'glow_round' : 'grate_round')).map((c) => ({ ...c, glow: lit })),
    ...disc('back_cap', AX, DRUM_R - 0.6, 14.2, 14.25, t('cap'), 'south'),
  );
  for (const [n, z0, z1] of [['f', 3.5, 5.5], ['b', 11.5, 13.5]]) // the cradle under the drum, front and back
    out.push(C(`cradle_${n}`, [2.5, 1, z0], [13.5, 4.5, z1], { all: t('plate'), down: null }));
  // ports: a boss from the drum out to the face, then the flange
  out.push(
    C('boss_west', [0.75, 5.5, 5.5], [3.5, 10.5, 10.5], { all: t('steel') }),
    C('port_west', [0, 5, 5], [0.75, 11, 11], { all: t('copper'), west: t('inlet') }, { uv: { west: [0, 0, 16, 16] } }),
    C('boss_east', [12.5, 5.5, 5.5], [15.25, 10.5, 10.5], { all: t('steel') }),
    C('port_east', [15.25, 5, 5], [16, 11, 11], { all: t('copper'), east: t('inlet') }, { uv: { east: [0, 0, 16, 16] } }),
    C('boss_back', [5.5, 5.5, 14.25], [10.5, 10.5, 15.25], { all: t('steel'), north: null }),
    C('port_back', [5, 5, 15.25], [11, 11, 16], { all: t('copper'), south: t('inlet') }, { uv: { south: [0, 0, 16, 16] } }),
    // the seat for the direction value box: on top of the drum at the back, clear of every port
    C('value_seat', [6.25, 13, 11.75], [9.75, 13.3, 14.25], { all: t('trim'), down: null }),
    C('boss_bottom', [5.5, 1, 6], [10.5, 3.5, 10], { all: t('steel'), down: null }),
    // the filler neck between the cylinders, with the top port
    C('neck', [6.5, 12.5, 5.75], [9.5, 15, 10.25], { all: t('steel') }),
    C('port_top', [5, 15, 5], [11, 16, 11], { all: t('copper'), up: t('inlet') }, { uv: { up: [0, 0, 16, 16] } }),
  );
  // the V-twin: a base flange on the drum, a finned barrel with stepped corners (so it reads round), a head with
  // its exhaust stub on the front (the mouth glows while lit)
  for (const side of ['left', 'right']) {
    out.push(cyl(side, 'cyl_base', -2.75, DRUM_R - 0.4, 2.75, DRUM_R + 0.2, 4.5, 11.5, { all: t('trim') }));
    out.push(cyl(side, 'barrel', -1.5, BARREL0, 1.5, 8.75, 5.5, 10.5, { all: t('barrel'), down: null, up: null }));
    FINS.forEach((dy, k) => {
      out.push(cyl(side, `fin_${k}`, -2, dy, 2, dy + 0.5, 5, 11, { all: t('fin') }));
      out.push(cyl(side, `fin_${k}_r`, -1.5, dy + 0.05, 1.5, dy + 0.45, 4.5, 11.5, { all: t('fin'), up: null, down: null }));
    });
    out.push(cyl(side, 'head', -1.75, 8.75, 1.75, 9.25, 5.25, 10.75, { all: t('trim'), up: t('plate') }));
    out.push(cyl(side, 'exhaust', -0.85, 7.25, 0.85, 8.95, 3.75, 4.5, { all: t('trim'), north: t(lit ? 'exhaust_lit' : 'exhaust'), south: null }, { uv: { north: [0, 0, 16, 16] }, glow: lit }));
  }
  if (sup) {
    // the supercharger: a finned blower in the front of the V, a brass band, and the belt down to the shaft
    out.push(C('blower', [5.75, 13.5, 0.75], [10.25, 15.75, 4.5], { all: t('blower') }));
    out.push(C('blower_band', [5.5, 13.25, 2.25], [10.5, 16, 2.75], { all: t('band') }));
    for (const [n, x0] of [['l', 6.5], ['r', 9]])
      out.push(C(`belt_${n}`, [x0, 8, 0], [x0 + 0.5, 14.25, 0.25], { all: t('belt') }));
  }
  return out;
}
// renderer parts, in the north-facing frame
function flywheel(sup) {
  const t = (k) => (sup ? 's:' : 'e:') + k, out = [];
  out.push(...drumZ('flywheel_rim', AX, 5.25, 0.75, 2.25, 1, t('tread'), { north: t('rim'), south: t('rim') }));
  for (const a of [0, 45, -45]) out.push(C(`spoke_v_${a}`, [AX.x - 0.5, AX.y - 4.5, 1.25], [AX.x + 0.5, AX.y + 4.5, 1.75], { all: t('spoke') }, { rot: a ? ['z', a, [AX.x, AX.y, 1.25]] : undefined }));
  out.push(C('spoke_h', [AX.x - 4.5, AX.y - 0.5, 1.25 + LAYER], [AX.x + 4.5, AX.y + 0.5, 1.75 + LAYER], { all: t('spoke') }));
  out.push(C('hub', [AX.x - 2.5, AX.y - 2.5, 0.75], [AX.x + 2.5, AX.y + 2.5, 2.25], { all: t('hub') }));
  if (sup) out.push(...disc('drive_pulley', AX, 3.25, 0.25, 0.75, t('pulley')));
  return out;
}
const PISTON_TRAVEL = 1;
function piston(side, sup) {
  const t = (k) => (sup ? 's:' : 'e:') + k;
  return [
    cyl(side, 'rod', -0.75, 2, 0.75, 7, 7.25, 8.75, { all: t('rod') }),
    cyl(side, 'crosshead', -1.25, 5.25, 1.25, 5.65, 6.75, 9.25, { all: t('trim') }),
  ];
}
const BLOWER = { x: 8, y: 14.6 };
const blowerPulley = () => disc('blower_pulley', BLOWER, 1.25, 0.25, 0.75, 's:pulley');
const AXIS = 'create:block/axis', AXIS_TOP = 'create:block/axis_top';
const shaftStub = () => [C('shaft_stub', [6, 6, -0.02], [10, 10, 3.5], { north: AXIS_TOP, south: AXIS_TOP, all: AXIS },
  { uv: { north: [6, 6, 10, 10], south: [6, 6, 10, 10], east: [6, 0, 10, 4], west: [6, 0, 10, 4], up: [6, 0, 10, 4], down: [6, 0, 10, 4] }, frot: { east: 90, west: 90 } })];

// ---------- preview ----------
const { cube, group, clip, write } = preview('engines', { 'e:': ENG, 's:': SUP });
const cubes = (list, off) => list.map((c) => cube(c, off));
const E0 = [0, 0, 0], S0 = [24, 0, 0];
const U = { left: [-Math.SQRT1_2, Math.SQRT1_2, 0], right: [Math.SQRT1_2, Math.SQRT1_2, 0] }; // up each cylinder
function rig(prefix, off, sup) {
  const kids = [
    group(`${prefix}_off`, [8, 0, 8], off, cubes(engine('off', sup), off)),
    group(`${prefix}_lit`, [8, 0, 8], off, cubes(engine('lit', sup), off)),
    group(`${prefix}_flywheel`, [AX.x, AX.y, 1.5], off, cubes([...flywheel(sup), ...shaftStub()], off)),
    group(`${prefix}_piston_left`, [AX.x, AX.y, 8], off, cubes(piston('left', sup), off)),
    group(`${prefix}_piston_right`, [AX.x, AX.y, 8], off, cubes(piston('right', sup), off)),
  ];
  if (sup) kids.push(group(`${prefix}_blower_pulley`, [BLOWER.x, BLOWER.y, 0.5], off, cubes(blowerPulley(), off)));
  return group(prefix, [8, 0, 8], off, kids, { open: true });
}
const outliner = [rig('eng', E0, false), rig('sup', S0, true)];

// run: cold until the lava arrives at 1 s, then the shaft turns (64 RPM, the Supercharged 128) until the tank runs dry
// at 6.5 s, when it stops at once (as the code does)
const L = 8, ON = 1, OFF = 6.5;
// approved sounds: each clip's cues become sound keyframes on its Effects track, pointing at the real OGG files
// (the lava pop is Minecraft's own; its preview file is made locally from the game assets and git-ignored)
const SOUND_DIR = path.resolve(__dirname, '../sound/magma-engine');
const CUES = JSON.parse(fs.readFileSync(path.join(SOUND_DIR, 'magma-engine.cues.json'), 'utf8'));
function sounds(clipName) {
  const cues = CUES.clips[clipName] || [];
  const missing = [...new Set(cues.map(([, id]) => id).filter((id) => !fs.existsSync(path.join(SOUND_DIR, 'real', id + '.ogg'))))];
  if (missing.length) throw new Error(`clip ${clipName}: no real sound for ${missing.join(', ')}`);
  return cues.map(([t, id]) => [t, CUES.namespace + '.' + id, path.join(SOUND_DIR, 'real', id + '.ogg').split(path.sep).join('/')]).sort((x, y) => x[0] - y[0]);
}
const litAt = (s) => s >= ON && s < OFF;
const angleAt = (rpm) => (s) => rpm * 6 * (Math.min(Math.max(s, ON), OFF) - ON); // degrees, held once stopped
const travel = (a, phase) => (PISTON_TRAVEL / 2) * (1 - Math.cos((a + phase) * DEG));
function tracks(prefix, rpm, sup) {
  const ang = angleAt(rpm), tr = {
    [`${prefix}_off`]: { scale: (s) => (litAt(s) ? HIDE : SHOW) },
    [`${prefix}_lit`]: { scale: (s) => (litAt(s) ? SHOW : HIDE) },
    [`${prefix}_flywheel`]: { rotation: (s) => [0, 0, -ang(s) % 360] },
  };
  for (const [side, phase] of [['left', 0], ['right', 180]])
    tr[`${prefix}_piston_${side}`] = { position: (s) => U[side].map((u) => u * travel(ang(s), phase)) };
  if (sup) tr[`${prefix}_blower_pulley`] = { rotation: (s) => [0, 0, -(2 * ang(s)) % 360] };
  return tr;
}
// one engine runs in each clip, so its sounds can be judged on their own; the other stays cold
const cold = (prefix) => ({ [`${prefix}_off`]: { scale: [[0, SHOW]] }, [`${prefix}_lit`]: { scale: [[0, HIDE]] } });
clip('run', L, 'loop', tracks('eng', 64, false), cold('sup'), sounds('run'));
clip('run_super', L, 'loop', tracks('sup', 128, true), cold('eng'), sounds('run_super'));
clip('idle', 1, 'hold', {}, { ...cold('eng'), ...cold('sup') }, sounds('idle'));

write(OUT, outliner);
module.exports = { engine, flywheel, piston, blowerPulley, shaftStub, ENG, SUP, AX, BLOWER, LAYER, PISTON_TRAVEL };
