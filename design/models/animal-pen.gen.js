// Animal Pens redesign generator -> design/models/animal-pen.bbmodel (Generic Model preview with clips).
// Built in the game's model space (window north), on the family kit; see design/briefs/animal-pen.md.
// The pen's renderer contract (AnimalPenRenderer, PenAnimal): floor at y 2.5, feet wander x 3.5-12.5 and z 4.5-9.5
// (the brief moves z min from 2.5 for the tray), trough food at (5.5 / 8 / 10.5, 3.1, 12), shaft stub at x6-10 y6-10 z12.5-16.
const path = require('path');
const { P, C, textureSet, painters, mot, hash, edge, FIELD, HI, LO, shaftStub, preview, SHOW, HIDE, lerp, trayItem } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'animal-pen.bbmodel');
const FLOOR_Y = 2.5;
const WANDER = { x0: 3.5, x1: 12.5, z0: 4.5, z1: 9.5 };
const FOOD = [[5.5, -40], [8, 30], [10.5, 100]]; // x and yaw of the three trough items (AnimalPenRenderer.java:31-32)
// Milk sight glass: a slot in each side wall, recessed 0.5 px, z 7-9, y 3-13; the renderer draws the milk column inside it
const GAUGE = { z0: 7, z1: 9, y0: 3, y1: 13, depth: 0.5 };

// ---------- textures ----------
P.straw = ['#6e5f3a', '#8a7a4a', '#9c8a52'];
P.milk = ['#a39d8b', '#b0aa97', '#b9b29f'];
P.brassDark = ['#6e5a2a', '#76612e', '#7e6832'];
// the brass tier's trim ring: aged, dark brass with the odd brighter pixel; bright brass stays for rivets, bolts, tap and lips
P.brassTrim = ['#5e4c24', '#6e5a2a', '#8f7638'];
// shared by both tiers: animal_pen_<key>.png
const PEN = textureSet(); PEN.file = 'animal_pen_';
// the brass tier's frame and top: brass_animal_pen_<key>.png
const BRASS = textureSet(); BRASS.file = 'brass_animal_pen_';

// Sides: a copper-framed sight glass down the middle (u 6-9, v 2-13), so the milk level reads from outside
const gaugeFrame = (x, y) => {
  if (x < 6 || x > 9 || y < 2 || y > 13) return null;
  if (x === 6 || x === 9 || y === 2 || y === 13) return mot('copper', x, y, 21);
  return '#1f2327';
};
for (const [set, trim] of [[PEN, 'andesite'], [BRASS, 'brassTrim']]) {
  set.paint('front', painters.front(trim));
  set.paint('side', painters.side(trim, gaugeFrame));
  set.paint('back', painters.back(trim, trim === 'brassTrim' ? '#9c8240' : undefined));
  set.paint('bottom', painters.bottom(trim));
}
// Brass top: walnut boards in a muted brass frame (replacing Create's brighter brass casing)
BRASS.paint('top', (x, y) => {
  const e = edge(x, y);
  if (e === 0) return mot('brassTrim', x, y, 22);
  if (e === 1) return '#2e2118';
  return y % 4 === 1 ? '#2e2118' : mot('walnut', x + (y >> 2) * 5, y, 24);
});
PEN.paint('glass', painters.glass());
PEN.paint('ceiling', painters.ceiling());
// Stall walls: walnut boarding up to y 7 (v 9 and below in face space), lighter steel panels above
PEN.paint('stall', (x, y) => {
  if (y >= 9) return x % 4 === 0 ? '#2e2118' : mot('walnut', x, y + (x >> 2) * 3, 25);
  return painters.chamber()(x, y);
});
PEN.paint('straw', (x, y) => (hash(x, y, 26) > 0.9 ? '#5a4d30' : mot('straw', x + ((y * 3) & 7), y, 27)));
// The sight glass's backing: dark glass with faint copper level ticks on one edge
PEN.paint('gauge', (x, y) => (x === 8 && y % 2 === 0 ? '#3a3a36' : (hash(x, y, 28) > 0.85 ? '#2c3236' : '#2a2f33')));
PEN.paint('milk', (x, y) => mot('milk', x, y, 29));
// Hay chute face: walnut boards with the dark feed opening at its foot (the face covers u/v 5.5-11 in block space)
PEN.paint('chute', (x, y) => {
  if (y >= 10) return x === 5 || x === 10 ? mot('walnut', x, y, 30) : '#1f2327';
  return x % 3 === 0 ? '#2e2118' : mot('walnut', x, y, 31);
});
PEN.paint('walnut', painters.walnut());
PEN.paint('andesite', painters.andesite());
PEN.paint('brass', painters.brass());
PEN.paint('copper', painters.copper());
PEN.paint('dark', painters.dark());
// Preview-only stand-ins for the real animals the renderer draws (so clearance can be checked against the stall)
PEN.paint('cow', (x, y) => (hash(x >> 1, y >> 1, 32) > 0.75 ? '#6b6e6a' : mot('walnut', x, y, 33)));
PEN.paint('chicken', (x, y) => (hash(x, y, 34) > 0.8 ? '#73766f' : '#7c7f79'));
PEN.paint('sheep', (x, y) => mot('andesite', x, y, 35));
PEN.paint('beak', painters.brass());

// ---------- the pen model (both tiers share the geometry; tier picks the frame textures) ----------
function pen(tier) {
  const t = (k) => `${tier}:${k}`, lip = tier === 'brass' ? 'pen:brass' : 'pen:andesite';
  const g = GAUGE, out = [
    C('top', [0, 14, 0], [16, 16, 16], { north: t('front'), east: t('side'), west: t('side'), south: t('back'), up: tier === 'brass' ? 'brass:top' : 'create:block/andesite_casing', down: 'pen:ceiling' }),
    C('bottom', [0, 0, 0], [16, 2, 16], { north: t('front'), east: t('side'), west: t('side'), south: t('back'), down: t('bottom'), up: null }),
    C('wall_back', [2, 2, 14], [14, 14, 16], { south: t('back'), north: 'pen:stall', east: null, west: null, up: null, down: null }),
    C('glass', [2, 2, 1.2], [14, 14, 1.2], { north: 'pen:glass', south: 'pen:glass', all: null }, { shade: false }),
  ];
  // Side walls: an inner wall plus a 0.5 px outer skin around the sight-glass slot (west at x 0, east at x 16)
  for (const [side, x0, x1, sx0, sx1, outer, inner] of [['west', 0.5, 2, 0, 0.5, 'west', 'east'], ['east', 14, 15.5, 15.5, 16, 'east', 'west']]) {
    out.push(C(`wall_${side}`, [x0, 2, 0], [x1, 14, 16], { north: t('front'), south: t('back'), [outer]: 'pen:gauge', [inner]: 'pen:stall', up: null, down: null }));
    const skin = { north: null, south: null, up: null, down: null, east: null, west: null, [outer]: t('side') };
    out.push(C(`skin_${side}_front`, [sx0, 2, 0], [sx1, 14, g.z0], { ...skin, north: t('front'), south: 'pen:dark' }));
    out.push(C(`skin_${side}_back`, [sx0, 2, g.z1], [sx1, 14, 16], { ...skin, south: t('back'), north: 'pen:dark' }));
    out.push(C(`skin_${side}_top`, [sx0, g.y1, g.z0], [sx1, 14, g.z1], { ...skin, down: 'pen:dark' }));
    out.push(C(`skin_${side}_foot`, [sx0, 2, g.z0], [sx1, g.y0, g.z1], { ...skin, up: 'pen:dark' }));
    // a small brass tap at the foot of the glass, inside the recess
    const tx = side === 'west' ? [0.1, 0.5] : [15.5, 15.9];
    out.push(C(`tap_${side}`, [tx[0], g.y0, 7.55], [tx[1], g.y0 + 0.7, 8.45], { all: 'pen:brass' }));
  }
  out.push(
    // straw floor: its top is the animals' floor
    C('straw', [2, 2, 1.25], [14, FLOOR_Y, 14], { up: 'pen:straw', north: 'pen:straw', all: null }),
    // fence rail capping the boarding round the stall
    C('rail_west', [2, 6.5, 1.25], [2.4, 7, 14], { all: 'pen:walnut', west: null }),
    C('rail_east', [13.6, 6.5, 1.25], [14, 7, 14], { all: 'pen:walnut', east: null }),
    C('rail_back', [2.4, 6.5, 13.6], [13.6, 7, 14], { all: 'pen:walnut', south: null }),
    // trough at the back, centred on z 12, its straw bed just under the food at y 3.1
    C('trough_front', [3, FLOOR_Y, 10.5], [13, 4, 11], { all: 'pen:walnut', up: lip, down: null }),
    C('trough_back', [3, FLOOR_Y, 13], [13, 4, 13.5], { all: 'pen:walnut', up: lip, down: null }),
    C('trough_west', [3, FLOOR_Y, 11], [3.5, 4, 13], { all: 'pen:walnut', up: lip, down: null, north: null, south: null }),
    C('trough_east', [12.5, FLOOR_Y, 11], [13, 4, 13], { all: 'pen:walnut', up: lip, down: null, north: null, south: null }),
    C('trough_bed', [3.5, FLOOR_Y, 11], [12.5, 3, 13], { up: 'pen:straw', all: null }),
    // hay chute on the back wall, feeding the trough; it swallows the shaft stub's tip
    C('chute', [5.5, 5, 12.4], [10.5, 10.5, 14], { all: 'pen:walnut', north: 'pen:chute', down: 'pen:dark', south: null }),
    // front tray, where eggs, wool and drops wait; it sits on the straw
    C('tray_front', [3, FLOOR_Y, 1.4], [13, 3.3, 1.9], { all: 'pen:walnut', up: lip, down: null }),
    C('tray_back', [3, FLOOR_Y, 3.3], [13, 3.3, 3.8], { all: 'pen:walnut', up: lip, down: null }),
    C('tray_west', [3, FLOOR_Y, 1.9], [3.5, 3.3, 3.3], { all: 'pen:walnut', up: lip, down: null, north: null, south: null }),
    C('tray_east', [12.5, FLOOR_Y, 1.9], [13, 3.3, 3.3], { all: 'pen:walnut', up: lip, down: null, north: null, south: null }),
    C('tray_bed', [3.5, FLOOR_Y, 1.9], [12.5, 2.6, 3.3], { up: 'pen:walnut', all: null }),
  );
  return out;
}

// ---------- preview-only props ----------
// the milk column in both slots, drawn by the renderer from the synced level; scaled in y from the slot foot
const milk = () => [
  C('milk_west', [0.25, GAUGE.y0, GAUGE.z0 + 0.05], [0.25, GAUGE.y1, GAUGE.z1 - 0.05], { west: 'pen:milk', all: null }, { shade: false }),
  C('milk_east', [15.75, GAUGE.y0, GAUGE.z0 + 0.05], [15.75, GAUGE.y1, GAUGE.z1 - 0.05], { east: 'pen:milk', all: null }, { shade: false }),
];
// trough food: flat item sprites, 4.8 px across at 0.3 scale, turned to the renderer's yaws
const food = (tex) => FOOD.map(([x, yaw], i) => C(`food_${i}`, [x - 2.4, 3.1, 12 - 2.4], [x + 2.4, 3.1, 12 + 2.4], { up: tex, down: tex, all: null },
  { uv: { up: [0, 0, 16, 16], down: [0, 0, 16, 16] }, rot: ['y', yaw, [x, 3.1, 12]], shade: false }));
// Stand-ins sized from the vanilla models at the renderer's 0.26 scale, feet at the origin, facing north (-z)
const cow = (skin = 'pen:cow') => [
  ...[[-1.05, -1.1], [0.05, -1.1], [-1.05, 2.0], [0.05, 2.0]].map(([x, z], i) => C(`leg_${i}`, [x, FLOOR_Y, z], [x + 1, FLOOR_Y + 3.1, z + 1], { all: skin })),
  C('body', [-1.55, FLOOR_Y + 3.1, -1.6], [1.55, FLOOR_Y + 5.7, 3.1], { all: skin }),
  C('head', [-1.05, FLOOR_Y + 4.1, -3.6], [1.05, FLOOR_Y + 6.2, -1.6], { all: skin }),
];
const chicken = () => [
  C('legs', [-0.4, FLOOR_Y, -0.2], [0.4, FLOOR_Y + 0.8, 0.2], { all: 'pen:beak' }),
  C('body', [-0.8, FLOOR_Y + 0.8, -1.0], [0.8, FLOOR_Y + 2.4, 1.1], { all: 'pen:chicken' }),
  C('head', [-0.5, FLOOR_Y + 2.0, -1.6], [0.5, FLOOR_Y + 3.6, -0.8], { all: 'pen:chicken' }),
  C('beak', [-0.25, FLOOR_Y + 2.8, -2.0], [0.25, FLOOR_Y + 3.2, -1.6], { all: 'pen:beak' }),
];

// ---------- assemble ----------
const { cube, group, clip, write } = preview('animal-pen', { 'pen:': PEN, 'brass:': BRASS });
function scene(tier, off) {
  const cubes = (list, shift) => list.map((c) => cube(c, off, shift));
  const items = tier === 'brass' ? ['minecraft:block/gray_wool', 'minecraft:item/mutton', 'minecraft:item/leather'] : ['minecraft:item/egg', 'minecraft:item/feather', 'minecraft:item/leather'];
  const feed = tier === 'brass' ? 'minecraft:item/wheat' : 'minecraft:item/wheat_seeds';
  // animals start at their wander-rectangle positions; the clips move them
  const a1 = [6, WANDER.z0 + 1], a2 = [10, WANDER.z1 - 0.5];
  return group(tier, [8, 0, 8], off, [
    group(`${tier}_shell`, [8, 0, 8], off, cubes(pen(tier))),
    group(`${tier}_shaft`, [8, 8, 8], off, cubes(shaftStub())),
    group(`${tier}_milk`, [8, GAUGE.y0, 8], off, cubes(milk())),
    group(`${tier}_food`, [8, 3.1, 12], off, cubes(food(feed))),
    group(`${tier}_cow`, [a1[0], FLOOR_Y, a1[1]], off, cubes(tier === 'brass' ? cow('pen:sheep') : cow(), [a1[0], 0, a1[1]])),
    group(`${tier}_small`, [a2[0], FLOOR_Y, a2[1]], off, cubes(tier === 'brass' ? cow() : chicken(), [a2[0], 0, a2[1]])),
    ...[5, 8, 11].map((cx, i) => group(`${tier}_item${i + 1}`, [cx, 2.6, 2.2], off, cubes(trayItem(`${tier}_${i + 1}`, cx, items[i]), [0, 0.1, 0]))),
  ], { open: true });
}
const outliner = [scene('pen', [0, 0, 0]), scene('brass', [24, 0, 0])];

// ---------- clips ----------
// life: the animals potter about inside the wander rectangle, the milk rises, produce turns up in the tray
const LIFE_S = 8;
const walk = (s, pts) => { // pts: [[t, x, z]], linear between, yaw facing the way it walks
  let i = 0;
  while (i < pts.length - 2 && s > pts[i + 1][0]) i++;
  const [t0, x0, z0] = pts[i], [t1, x1, z1] = pts[i + 1];
  const f = Math.max(0, Math.min(1, (s - t0) / (t1 - t0)));
  const yaw = Math.atan2(-(x1 - x0), -(z1 - z0)) * 180 / Math.PI; // 0 faces north (-z)
  return { x: lerp(x0, x1, f), z: lerp(z0, z1, f), yaw: Math.abs(x1 - x0) + Math.abs(z1 - z0) < 1e-6 ? null : yaw };
};
function animalTracks(bone, start, pts) {
  let lastYaw = 0;
  const yawAt = (s) => { const w = walk(s, pts); if (w.yaw !== null) lastYaw = w.yaw; return lastYaw; };
  return { [bone]: { position: (s) => { const w = walk(s, pts); return [w.x - start[0], 0, w.z - start[1]]; }, rotation: (s) => [0, yawAt(s), 0] } };
}
const W = WANDER;
const big = [[0, 6, W.z0 + 1], [2, 11.5, W.z0 + 1], [2.6, 11.5, W.z0 + 1], [5, 11.5, W.z1 - 1], [5.6, 11.5, W.z1 - 1], [8, 6, W.z0 + 1]];
const small = [[0, 10, W.z1 - 0.5], [1.5, 7, W.z1], [3, 7, W.z1], [4.5, 4.5, W.z1 - 1.5], [6, 4.5, W.z1 - 1.5], [8, 10, W.z1 - 0.5]];
const lifeTracks = (tier) => ({
  ...animalTracks(`${tier}_cow`, [6, W.z0 + 1], big),
  ...animalTracks(`${tier}_small`, [10, W.z1 - 0.5], small),
  [`${tier}_milk`]: { scale: (s) => [1, Math.max(0.001, s / LIFE_S), 1] },
});
const traySteps = (tier, times) => Object.fromEntries([1, 2, 3].map((i) => [`${tier}_item${i}`, { scale: [[0, HIDE], [times[i - 1], SHOW]] }]));
clip('life', LIFE_S, 'loop', { ...lifeTracks('pen'), ...lifeTracks('brass') }, { ...traySteps('pen', [2, 4, 6]), ...traySteps('brass', [2, 4, 6]) });
// milk_levels: empty, half and full in the side glass
const milkSteps = (tier) => ({ [`${tier}_milk`]: { scale: [[0, [1, 0.001, 1]], [1, [1, 0.5, 1]], [2, [1, 1, 1]]] } });
clip('milk_levels', 3, 'loop', {}, { ...milkSteps('pen'), ...milkSteps('brass') });
// clearance: the big animal at the front-left corner of the wander rectangle facing the glass, then at the back facing the chute
const clearSteps = (tier) => ({
  [`${tier}_cow`]: { position: [[0, [W.x1 - 6, 0, W.z0 - (W.z0 + 1)]], [1.5, [W.x0 - 6, 0, W.z1 - (W.z0 + 1)]]], rotation: [[0, [0, 0, 0]], [1.5, [0, 180, 0]]] },
  [`${tier}_small`]: { position: [[0, [W.x0 - 10, 0, W.z1 - (W.z1 - 0.5)]], [1.5, [W.x1 - 10, 0, W.z0 - (W.z1 - 0.5)]]], rotation: [[0, [0, 90, 0]], [1.5, [0, -90, 0]]] },
});
clip('clearance', 3, 'loop', {}, { ...clearSteps('pen'), ...clearSteps('brass') });

write(OUT, outliner);
module.exports = { pen, PEN, BRASS, GAUGE, WANDER };
