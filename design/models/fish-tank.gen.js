// Fish Tanks generator -> design/models/fish-tank.bbmodel (Generic Model preview with clips).
// Built in the game's model space (window north), on the family kit; see design/briefs/fish-tank.md.
// Sister to the Animal Pens: same frame proportions, back socket and front catch; glass on the front AND both sides.
// Renderer contract (new): water volume x 1-15, z 0.75-13.75 from the gravel to level (max y 13.5); swim volume
// x 3-13, z 4.5-12, y (gravel + 1) to (surface - 1); food under the lid feeder at (3.4 / 4.2 / 5.0, 12, 3.6); the pump piston
// rises 0-1.5 px with the shaft angle; catch net items via TrayItems at the pens' tray positions.
const path = require('path');
const { P, C, textureSet, painters, mot, hash, edge, FIELD, HI, LO, shaftStub, preview, SHOW, HIDE, lerp, trayItem } = require('./process-box.lib.js');
const { ITEMS } = require('./monster-cage.gen.js');

const OUT = path.resolve(__dirname, 'fish-tank.bbmodel');
const FLOOR_Y = 2.5, TOP = 13.5;

Object.assign(P, {
  brassMid: ['#7e6832', '#8f7638', '#a3873f'],
  walnutLid: ['#4e3626', '#634532', '#6f4d36'],
  gravel: ['#4a4740', '#5a564c', '#6b665a'],
  tile: ['#2e3a40', '#33424a', '#36444b'],
  kelp: ['#2f4a2a', '#3e5e33', '#4f7340'],
  copperC: ['#9a5038', '#a75a40', '#b26247'],
  cod: ['#6b5f4a', '#7a6c52', '#8a7a5c'],
  squid: ['#2f3f55', '#36495f', '#3e5268'],
  axo: ['#3f5f9a', '#4a6aa8', '#5577b4'],
  tropical: ['#b0602a', '#c06a30', '#8a8478'],
});
const TANK = textureSet(); TANK.file = 'fish_tank_';
const BTANK = textureSet(); BTANK.file = 'brass_fish_tank_';
for (const [set, trim] of [[TANK, 'andesite'], [BTANK, 'brassMid']]) {
  set.paint('frame', painters.front(trim));                       // front and both sides: a frame round a window
  set.paint('back', painters.back(trim, trim === 'brassMid' ? '#a3873f' : undefined));
  set.paint('bottom', painters.bottom(trim));
  // walnut lid in a trim frame, with the feeder hatch at the front-left (u 2-5, v 2-5)
  set.paint('lid', (x, y) => {
    const e = edge(x, y);
    if (e === 0) return mot(trim, x, y, 100);
    if (x >= 2 && x <= 5 && y >= 2 && y <= 5) return x === 2 || x === 5 || y === 2 || y === 5 ? mot(trim === 'andesite' ? 'steelLight' : 'brassMid', x, y, 101) : '#141619';
    if (e === 1) return '#2e2118';
    return y % 4 === 1 ? '#3b2a1f' : mot('walnutLid', x + (y >> 2) * 5, y, 102);
  });
  set.paint('net_rim', trim === 'andesite' ? painters.walnut() : (x, y) => mot('brassMid', x, y, 103));
  // a sunken cogwheel: walnut teeth and an andesite hub (brass for the brass tank)
  set.paint('cog', trim === 'andesite' ? (x, y) => (edge(x, y) === 0 ? '#2e2118' : mot('walnut', x, y, 104)) : (x, y) => (edge(x, y) === 0 ? '#5e4c24' : mot('brassMid', x, y, 105)));
  set.paint('hub', (x, y) => (x >= 6 && x <= 9 && y >= 6 && y <= 9 ? '#141619' : trim === 'andesite' ? mot('andesite', x, y, 106) : (hash(x, y, 107) > 0.8 ? '#a3873f' : '#7e6832')));
  set.paint('feeder', trim === 'andesite' ? (x, y) => mot('steel', x, y, 108) : (x, y) => mot('brassMid', x, y, 109));
  set.paint('pump_cap', trim === 'andesite' ? (x, y) => mot('steelLight', x, y, 110) : (x, y) => mot('brassMid', x, y, 111));
}
TANK.paint('glass', painters.glass());
TANK.paint('waterline', (x, y) => (y === 2 && x > 2 && x < 13 && x % 2 === 0 ? '#6f7f86' : null));
TANK.paint('tiles', (x, y) => (x % 4 === 0 || y % 4 === 0 ? '#263036' : mot('tile', x, y, 113)));
TANK.paint('ceiling', (x, y) => mot('walnut', x, y, 114));
TANK.paint('gravel', (x, y) => {
  const r = hash(x, y, 115);
  return r > 0.9 ? '#3a3832' : r > 0.75 ? '#6b665a' : mot('gravel', x + (y * 7) % 5, y, 116);
});
TANK.paint('dark', painters.dark());
TANK.paint('steel', painters.steel());
TANK.paint('copper', (x, y) => mot('copperC', x, y, 117));
TANK.paint('stone', (x, y) => mot('andesite', x, y, 118));
TANK.paint('net', (x, y) => ((x + y) % 3 === 0 || (x - y + 30) % 3 === 0 ? '#2a2c30' : null));
TANK.paint('kelp', (x, y) => (x >= 5 && x <= 10 && (x + (y >> 1)) % 3 !== 0 ? mot('kelp', x, y, 119) : null));
TANK.paint('seagrass', (x, y) => (x % 3 === 1 && y > 4 ? mot('kelp', x, y, 120) : null));
TANK.paint('water', (x, y) => (hash(x, y, 121) > 0.8 ? '#336a7780' : '#2a5a6678'));
TANK.paint('water_top', (x, y) => ((x + y * 3) % 7 === 0 ? '#4a7f8c98' : '#336a7788'));
// premultiplied (the old #3fa08c50 / #2f806e38 at their alpha): the game draws it additively, as the preview does
TANK.paint('glow', (x, y) => (hash(x, y, 122) > 0.6 ? '#14322c' : '#0a1c18'));
TANK.paint('bubble', (x, y) => (edge(x, y) <= 2 ? '#a3c0c8b0' : null));
// preview-only creature stand-ins
TANK.paint('cod', (x, y) => mot('cod', x, y, 123));
TANK.paint('squid', (x, y) => mot('squid', x, y, 124));
TANK.paint('axo', (x, y) => mot('axo', x, y, 125));
TANK.paint('tropical', (x, y) => (x % 4 < 2 ? '#b0602a' : '#8a8478'));
TANK.paint('eye', (x, y) => '#141619');

// ---------- the tank model ----------
function tank(tier) {
  const T = (k) => `${tier}:${k}`, F = T('frame');
  const out = [
    C('lid', [0, 14, 0], [16, 16, 16], { north: F, east: F, west: F, south: T('back'), up: T('lid'), down: 'tank:ceiling' }),
    C('base', [0, 0, 0], [16, 2, 16], { north: F, east: F, west: F, south: T('back'), down: T('bottom'), up: F }),
    C('back_wall', [0, 2, 14], [16, 14, 16], { south: T('back'), north: 'tank:tiles', east: F, west: F, up: null, down: null }),
    C('post_west', [0, 2, 0], [2, 14, 2], { north: F, west: F, south: 'tank:dark', east: 'tank:dark', up: null, down: null }),
    C('post_east', [14, 2, 0], [16, 14, 2], { north: F, east: F, south: 'tank:dark', west: 'tank:dark', up: null, down: null }),
    C('glass_front', [2, 2, 0.5], [14, 14, 0.5], { north: 'tank:glass', south: 'tank:glass', all: null }, { shade: false }),
    C('glass_west', [0.5, 2, 2], [0.5, 14, 14], { west: 'tank:glass', east: 'tank:glass', all: null }, { shade: false }),
    C('glass_east', [15.5, 2, 2], [15.5, 14, 14], { west: 'tank:glass', east: 'tank:glass', all: null }, { shade: false }),
    // gravel, rising in two steps towards the back (0.25 px in from the glass, so they don't z-fight)
    C('gravel', [0.75, 2, 0.75], [15.25, FLOOR_Y, 14], { up: 'tank:gravel', north: 'tank:gravel', east: 'tank:gravel', west: 'tank:gravel', all: null }),
    C('gravel_2', [0.75, FLOOR_Y, 9], [15.25, 3, 14], { up: 'tank:gravel', north: 'tank:gravel', east: 'tank:gravel', west: 'tank:gravel', all: null }),
    C('gravel_3', [0.75, 3, 11.5], [15.25, 3.5, 14], { up: 'tank:gravel', north: 'tank:gravel', east: 'tank:gravel', west: 'tank:gravel', all: null }),
    // the air pump round the shaft stub, with its copper air line down to the air stone
    C('pump', [5.5, 5, 12.4], [10.5, 10.5, 14], { all: 'tank:steel', up: T('pump_cap'), south: null }),
    C('pipe_down', [3, 3.5, 12.9], [3.6, 8, 13.5], { all: 'tank:copper', south: null }),
    C('pipe_across', [3.6, 7.4, 12.9], [5.5, 8, 13.5], { all: 'tank:copper', south: null, west: null, east: null }),
    C('air_stone', [2.7, 3.5, 12.6], [3.9, 4, 13.8], { all: 'tank:stone', down: null }),
    // the feeder's chute under the lid hatch
    C('feeder', [2.5, 13, 2.5], [5.5, 14, 5.5], { all: T('feeder'), up: null, down: 'tank:dark' }),
    // the catch net at the front, hanging just above the gravel
    C('net_front', [3, 2.9, 1.4], [13, 3.6, 1.8], { all: T('net_rim'), down: null }),
    C('net_back', [3, 2.9, 3.4], [13, 3.6, 3.8], { all: T('net_rim'), down: null }),
    C('net_west', [3, 2.9, 1.8], [3.4, 3.6, 3.4], { all: T('net_rim'), down: null, north: null, south: null }),
    C('net_east', [12.6, 2.9, 1.8], [13, 3.6, 3.4], { all: T('net_rim'), down: null, north: null, south: null }),
    C('net_mesh', [3.4, 2.95, 1.8], [12.6, 2.95, 3.4], { up: 'tank:net', down: 'tank:net', all: null }, { shade: false }),
  ];
  // kelp (two crossed planes each) and seagrass tufts
  for (const [i, [x, z, h]] of [[12.6, 12.4, 11], [4.4, 10.6, 9.5]].entries())
    for (const yaw of [45, -45])
      out.push(C(`kelp_${i}_${yaw}`, [x - 1, 3, z], [x + 1, h, z], { north: 'tank:kelp', south: 'tank:kelp', all: null }, { rot: ['y', yaw, [x, 3, z]], shade: false }));
  for (const [i, [x, z, y]] of [[9, 10, 3], [11, 8, FLOOR_Y], [6.5, 9.6, 3]].entries())
    for (const yaw of [45, -45])
      out.push(C(`grass_${i}_${yaw}`, [x - 0.8, y, z], [x + 0.8, y + 1.6, z], { north: 'tank:seagrass', south: 'tank:seagrass', all: null }, { rot: ['y', yaw, [x, y, z]], shade: false }));
  return out;
}

// ---------- the sunken cogwheel: Create's cogwheel_shaftless, drawn by the renderer ----------
// Not part of the block model: the renderer draws create:block/cogwheel_shaftless at COG.scale, centred at COG.at and turned
// by COG.rot (degrees about X, Y, Z), so it lies tipped in the gravel with one side sunk in. Both tiers use Create's cog.
const COG = { at: [5.2, 2.9, 7.2], scale: 0.33, rot: [-28, 20, -12] };
function createCog() {
  const { execFileSync } = require('child_process');
  const JAR = 'C:/Users/bones/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-281/484ecc6852a36a72fb50e905be79b3ce96181032/create-1.21.1-6.0.10-281-slim.jar';
  const m = JSON.parse(execFileSync('unzip', ['-p', JAR, 'assets/create/models/block/cogwheel_shaftless.json'], { encoding: 'utf8' }));
  const S = COG.scale, sc = (p) => p.map((v, k) => COG.at[k] + (v - 8) * S);
  return m.elements.map((e) => {
    const faces = {}, uv = {}, frot = {};
    for (const [d, f] of Object.entries(e.faces)) { faces[d] = 'create:block/cogwheel'; uv[d] = f.uv; if (f.rotation) frot[d] = f.rotation; }
    for (const d of ['north', 'east', 'south', 'west', 'up', 'down']) if (!(d in faces)) faces[d] = null;
    const o = { uv, frot };
    if (e.rotation) o.rot = [e.rotation.axis, e.rotation.angle, sc(e.rotation.origin)];
    return C(`cog_${e.name}`, sc(e.from), sc(e.to), faces, o);
  });
}

// ---------- parts and preview props ----------
const piston = () => [
  C('rod', [7.6, 10.5, 12.85], [8.4, 11.8, 13.55], { all: 'tank:steel', down: null }),
  C('head', [7, 11.8, 12.7], [9, 12.3, 13.7], { all: 'tank:steel' }),
];
const water = () => [C('water', [1, 2.6, 0.75], [15, TOP, 13.75], { north: 'tank:water', east: 'tank:water', west: 'tank:water', up: 'tank:water_top', all: null }, { shade: false })];
const glow = () => [C('glow', [2, 3.6, 13.75], [14, 13.4, 13.75], { north: 'tank:glow', all: null }, { shade: false, uv: { north: [0, 0, 16, 16] } })];
const food = () => [[3.4, 12.2, 3.6], [4.2, 11.6, 4.4], [5.0, 12.4, 3.2]].map(([x, y, z], i) =>
  C(`food_${i}`, [x - 0.8, y, z - 0.8], [x + 0.8, y, z + 0.8], { up: 'item:fish_food', down: 'item:fish_food', all: null }, { uv: { up: [0, 0, 16, 16], down: [0, 0, 16, 16] }, rot: ['y', [20, -35, 60][i], [x, y, z]], shade: false }));
const bubbles = () => [0, 1, 2].map((i) => C(`bubble_${i}`, [3.1, 4.3, 13.0], [3.5, 4.7, 13.4], { all: 'tank:bubble' }, { shade: false }));
// creatures at the renderer's scale, centred at the origin, nose to -z
const fish = (skin) => [
  C('body', [-0.45, -0.45, -1.05], [0.45, 0.45, 1.05], { all: skin }),
  C('tail', [-0.05, -0.5, 1.05], [0.05, 0.5, 1.7], { east: skin, west: skin, all: null }),
  C('eye_l', [-0.47, 0.05, -0.8], [-0.45, 0.25, -0.6], { west: 'tank:eye', all: null }),
  C('eye_r', [0.45, 0.05, -0.8], [0.47, 0.25, -0.6], { east: 'tank:eye', all: null }),
];
const squid = () => [
  C('mantle', [-0.9, 0, -0.9], [0.9, 2.4, 0.9], { all: 'tank:squid' }),
  ...[[-0.7, -0.7], [0.4, -0.7], [-0.7, 0.4], [0.4, 0.4]].map(([x, z], i) => C(`tentacle_${i}`, [x, -1.6, z], [x + 0.3, 0, z + 0.3], { all: 'tank:squid' })),
  C('eyes', [-0.7, 1.2, -0.95], [0.7, 1.5, -0.9], { north: 'tank:eye', all: null }),
];
const axolotl = () => [
  C('body', [-0.55, -0.35, -1.2], [0.55, 0.35, 1.2], { all: 'tank:axo' }),
  C('head', [-0.8, -0.4, -2.2], [0.8, 0.5, -1.2], { all: 'tank:axo' }),
  C('tail', [-0.05, -0.45, 1.2], [0.05, 0.35, 2.4], { east: 'tank:axo', west: 'tank:axo', all: null }),
  C('gills_l', [-1.15, -0.2, -1.8], [-0.8, 0.8, -1.7], { all: 'tank:axo' }),
  C('gills_r', [0.8, -0.2, -1.8], [1.15, 0.8, -1.7], { all: 'tank:axo' }),
];

// ---------- assemble ----------
const BD = textureSet(); BD.file = 'preview_';
BD.paint('dark', (x, y) => (hash(x, y, 99) > 0.9 ? '#26282c' : '#222428'));
const { cube, group, clip, write } = preview('fish-tank', { 'tank:': TANK, 'andesite:': TANK, 'brass:': BTANK, 'item:': ITEMS, 'bd:': BD });
const SCENES = [['andesite', 0], ['brass', 24]];
const NET = { andesite: ['minecraft:item/cod', 'minecraft:item/ink_sac', 'minecraft:item/bone_meal'], brass: ['minecraft:item/tropical_fish', 'minecraft:item/pufferfish', 'minecraft:item/glow_ink_sac'] };
const START = { a: [6, 8, 7], b: [10, 6, 9] };
function scene([tier, ox]) {
  const off = [ox, 0, 0];
  const cubes = (list, shift) => list.map((c) => cube(c, off, shift));
  return group(tier, [8, 0, 8], off, [
    group(`${tier}_shell`, [8, 0, 8], off, cubes(tank(tier))),
    group(`${tier}_shaft`, [8, 8, 8], off, cubes(shaftStub())),
    group(`${tier}_piston`, [8, 10.5, 13.2], off, cubes(piston())),
    group(`${tier}_water`, [8, 2.6, 8], off, cubes(water())),
    group(`${tier}_glow`, [8, 8, 13.75], off, cubes(glow())),
    group(`${tier}_food`, [4, 12, 4], off, cubes(food())),
    group(`${tier}_bubbles`, [3.3, 4.3, 13.2], off, cubes(bubbles())),
    group(`${tier}_cog`, COG.at, off, cubes(createCog())),
    group(`${tier}_a`, START.a, off, cubes(tier === 'brass' ? axolotl() : fish('tank:cod'), START.a)),
    group(`${tier}_b`, START.b, off, cubes(tier === 'brass' ? fish('tank:tropical') : squid(), START.b)),
    ...[5, 8, 11].map((cx, i) => group(`${tier}_item${i + 1}`, [cx, 2.6, 2.2], off, cubes(trayItem(`${tier}_${i + 1}`, cx, NET[tier][i]), [0, 0.4, 0]))),
  ], { open: true });
}
const outliner = SCENES.map(scene);
outliner.push(group('backdrop', [8, 0, 8], [0, 0, 0], [
  cube(C('ground', [-30, -0.5, -40], [60, -0.01, 30], { up: 'bd:dark', all: null }, { uv: { up: [0, 0, 16, 16] } }), [0, 0, 0]),
  cube(C('wall', [-30, -0.5, 30], [60, 40, 30], { north: 'bd:dark', all: null }, { uv: { north: [0, 0, 16, 16] } }), [0, 0, 0]),
]));

// ---------- sound keyframes (stage 3): every clip's Effects track comes from the approved cue file ----------
// Each cue plays the event's real take 1. The build refuses to run if a cued event has no real file.
function soundsFor(cuesPath, realDir) {
  const fs = require('fs'), p = require('path');
  const cues = JSON.parse(fs.readFileSync(cuesPath, 'utf8'));
  return (clipName) => (cues.clips[clipName] || []).map(([t, id]) => {
    const file = p.join(realDir, id + '.ogg');
    if (!fs.existsSync(file)) throw new Error(`no real sound for ${cues.namespace}.${id} (${file}): build the real sounds first`);
    return [t, `${cues.namespace}.${id}`, file.split(require('path').sep).join('/')];
  });
}
const sfx = soundsFor(path.resolve(__dirname, '../sound/fish-tank/fish-tank.cues.json'), path.resolve(__dirname, '../sound/fish-tank/real'));

// ---------- clips ----------
const LIFE_S = 8, rad = Math.PI / 180;
// swim loops inside the swim volume: x 3-13, z 4.5-12, y 4-12
const loop = (cx, cy, cz, rx, ry, rz, speed, phase) => (s) => {
  const a = (s / LIFE_S) * Math.PI * 2 * speed + phase;
  return [cx + rx * Math.cos(a), cy + ry * Math.sin(a * 2), cz + rz * Math.sin(a)];
};
const swim = (bone, start, path) => {
  const yawAt = (s) => { const p0 = path(s), p1 = path(s + 0.05); return Math.atan2(-(p1[0] - p0[0]), -(p1[2] - p0[2])) / rad; };
  return { [bone]: { position: (s) => { const p = path(s); return [p[0] - start[0], p[1] - start[1], p[2] - start[2]]; }, rotation: (s) => [0, yawAt(s), 0] } };
};
const tracks = {}, steps = {};
for (const [tier] of SCENES) {
  Object.assign(tracks,
    swim(`${tier}_a`, START.a, loop(8, 7.5, 8, 4, 1.2, 3, 1, 0)),
    swim(`${tier}_b`, START.b, tier === 'brass' ? loop(8, 9.5, 8, 3.5, 1, 2.8, 2, 1.5) : (s) => [8 + 3 * Math.cos(s * 0.6), 7 + 1.5 * Math.sin(s * 1.6), 8 + 2 * Math.sin(s * 0.6)]),
    { [`${tier}_piston`]: { position: (s) => [0, 0.75 - 0.75 * Math.cos(s * Math.PI), 0] } },
    { [`${tier}_bubbles`]: { position: (s) => [0, ((s * 3) % 9), 0], scale: (s) => (((s * 3) % 9) > 8.6 ? [0.001, 0.001, 0.001] : [1, 1, 1]) } },
    { [`${tier}_food`]: { position: (s) => [0, -Math.min(4, (s % 4) * 1.2), 0] } },
  );
  steps[`${tier}_glow`] = { scale: [[0, HIDE]] };
  Object.assign(steps, Object.fromEntries([1, 2, 3].map((i) => [`${tier}_item${i}`, { scale: [[0, HIDE], [i * 2, SHOW]] }])));
}
clip('life', LIFE_S, 'loop', tracks, steps, sfx('life'));
const lv = {};
for (const [tier] of SCENES) lv[`${tier}_water`] = { scale: [[0, [1, 0.001, 1]], [1, [1, 0.5, 1]], [2, [1, 1, 1]]] };
clip('water_levels', 3, 'loop', {}, lv);
const gl = {};
for (const [tier] of SCENES) gl[`${tier}_glow`] = { scale: [[0, SHOW]] };
clip('glow_squid', 2, 'loop', {}, gl);
clip('pump', 2, 'loop', Object.fromEntries(SCENES.map(([t]) => [`${t}_piston`, { position: (s) => [0, 0.75 - 0.75 * Math.cos(s * Math.PI), 0] }])), {}, sfx('pump'));

write(OUT, outliner);
{
  const fs = require('fs'), bb = JSON.parse(fs.readFileSync(OUT, 'utf8'));
  for (const t of bb.textures) if (t.name.includes('glow')) t.render_mode = 'additive';
  // tip the cog in the preview as the renderer will (Generic Model groups can turn on every axis)
  const tipCog = (node) => { if (node && typeof node === 'object') { if (node.name && node.name.endsWith('_cog') && node.children) node.rotation = COG.rot; (node.children || []).forEach(tipCog); } };
  bb.outliner.forEach(tipCog);
  for (const g of bb.groups) if (g.name.endsWith('_cog')) g.rotation = COG.rot;
  fs.writeFileSync(OUT, JSON.stringify(bb));
}
module.exports = { tank, piston, glow, TANK, BTANK, COG, TOP };
