// Auto Harvester redesign generator -> design/models/auto-harvester.bbmodel (Generic Model preview with clips).
// Everything is built in the game's model space: front (window) facing north, x 0 west -> 16 east, y up, z 0 front -> 16 back.
// The same part lists export to the mod's block models later (see design/briefs/auto-harvester.md).
const path = require('path');
const { P, SEAM, C, textureSet, painters, mot, hash, shaftStub, preview, SHOW, HIDE, smooth, lerp, trayItem } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'auto-harvester.bbmodel');
// Renderer envelope (AutoHarvesterRenderer.java:39-45), with the brief's proposed changes for the bigger tool
const PARK_X = 4.5, PARK_Y = 11, SWEEP_END_X = 11.5, CUT_Y = 6, CARRIAGE_BOTTOM = 9, SOIL_Y = 3;
const HARVEST_TICKS = 80, REEL_DEG = 18, SAW_DEG = 45;

// ---------- textures (auto_harvester_<key>.png) ----------
const T = textureSet();
const paint = T.paint;
paint('front', painters.front('andesite'));
// Sides: a raised access hatch, because every face is an item port
paint('side', painters.side('andesite', (x, y) => {
  if (x >= 4 && x <= 11 && y >= 4 && y <= 11) {
    if (x === 4 || y === 4) return '#555b63';
    if (x === 11 || y === 11) return '#2a2f35';
    if (x === 10 && (y === 7 || y === 8)) return P.andesite[2];
    return hash(x, y, 2) > 0.86 ? '#4a5059' : '#454b53';
  }
  if (x === 3 && (y === 5 || y === 6 || y === 9 || y === 10)) return P.andesite[0];
  return null;
}));
paint('back', painters.back('andesite'));
paint('bottom', painters.bottom('andesite'));
paint('glass', painters.glass());
paint('chamber', painters.chamber());
paint('ceiling', painters.ceiling());
paint('floor', painters.floor());
paint('steel', painters.steel());
paint('steel_light', painters.steelLight());
paint('dark', painters.dark());
paint('andesite', painters.andesite());
paint('brass', painters.brass());
paint('walnut', painters.walnut());
// Own soils: tilled furrows (crop) and plain dirt (none, tree), muted and dark
P.soil = ['#3b2a1f', '#45311f', '#4e3826'];
P.dirt = ['#45311f', '#4e3826', '#58402b'];
paint('soil', (x, y) => (y % 4 === 0 ? '#2e2118' : y % 4 === 1 ? mot('dirt', x, y, 17) : mot('soil', x, y, 18)));
paint('dirt', (x, y) => mot('dirt', x, y, 19));
// Gear housing face: a dark bearing ring round a dark bore
paint('housing', (x, y) => {
  const r = Math.hypot(x - 7.5, y - 7.5);
  if (r < 1.3) return mot('dark', x, y);
  if (r < 2.1) return SEAM;
  return mot('panel', x, y, 16);
});
const OWN = T.OWN;
T.file = 'auto_harvester_';

// ---------- model DSL ----------
// ---------- the shell (blockstate models) ----------
function shell(soil) {
  const S = { north: 'front', east: 'side', west: 'side', south: 'back' };
  return [
    C('top', [0, 14, 0], [16, 16, 16], { ...S, up: 'create:block/andesite_casing', down: 'ceiling' }),
    C('bottom', [0, 0, 0], [16, 2, 16], { ...S, down: 'bottom', up: 'floor' }),
    C('wall_west', [0, 2, 0], [2, 14, 16], { north: 'front', west: 'side', south: 'back', east: 'chamber', up: null, down: null }),
    C('wall_east', [14, 2, 0], [16, 14, 16], { north: 'front', east: 'side', south: 'back', west: 'chamber', up: null, down: null }),
    C('wall_back', [2, 2, 14], [14, 14, 16], { south: 'back', north: 'chamber', east: null, west: null, up: null, down: null }),
    C('glass', [2, 2, 1.2], [14, 14, 1.2], { north: 'glass', south: 'glass', all: null }, { shade: false }),
    // planter: walnut box with an andesite lip, moved back for the tray
    C('planter_front', [3, 2, 4], [13, 3.5, 4.75], { all: 'walnut', up: 'andesite', down: null }),
    C('planter_back', [3, 2, 12.25], [13, 3.5, 13], { all: 'walnut', up: 'andesite', down: null }),
    C('planter_west', [3, 2, 4.75], [3.75, 3.5, 12.25], { all: 'walnut', up: 'andesite', down: null, north: null, south: null }),
    C('planter_east', [12.25, 2, 4.75], [13, 3.5, 12.25], { all: 'walnut', up: 'andesite', down: null, north: null, south: null }),
    C('soil', [3.75, 2, 4.75], [12.25, SOIL_Y, 12.25], { up: soil, all: null }),
    // front tray, where the harvest waits
    C('tray_front', [3, 2, 1.4], [13, 2.9, 1.9], { all: 'walnut', up: 'andesite', down: null }),
    C('tray_back', [3, 2, 3.3], [13, 2.9, 3.8], { all: 'walnut', up: 'andesite', down: null }),
    C('tray_west', [3, 2, 1.9], [3.5, 2.9, 3.3], { all: 'walnut', up: 'andesite', down: null, north: null, south: null }),
    C('tray_east', [12.5, 2, 1.9], [13, 2.9, 3.3], { all: 'walnut', up: 'andesite', down: null, north: null, south: null }),
    C('tray_bed', [3.5, 2, 1.9], [12.5, 2.5, 3.3], { up: 'walnut', all: null }),
    // girder rail: an I-beam wall to wall
    C('girder_bottom', [2, 12, 8.5], [14, 12.4, 11.5], { all: 'steel', up: 'steel_light', east: null, west: null }),
    C('girder_web', [2, 12.4, 9.75], [14, 13.6, 10.25], { all: 'steel', east: null, west: null, up: null, down: null }),
    C('girder_top', [2, 13.6, 8.5], [14, 14, 11.5], { all: 'steel', east: null, west: null, up: null }),
    // gear housing on the back wall: swallows the shaft stub's tip (x6-10, y6-10, z12.5-14)
    C('housing', [5.5, 5.5, 12], [10.5, 10.5, 14], { all: 'steel', north: 'housing', south: null }),
    C('housing_45', [5.5, 5.5, 12.01], [10.5, 10.5, 13.99], { all: 'steel', north: 'housing', south: null }, { rot: ['z', 45, [8, 8, 13]] }),
  ];
}

// ---------- the part models (renderer-drawn) ----------
const carriage = () => [
  C('body', [3, 9, 8.5], [6, 12, 11.5], { all: 'steel', up: null }),
  C('plate_front', [3, 9, 8.1], [6, 13.3, 8.5], { all: 'steel_light', north: 'steel' }),
  C('plate_back', [3, 9, 11.5], [6, 13.3, 11.9], { all: 'steel_light', south: 'steel' }),
  C('wheel_front', [3.4, 12.4, 8.6], [5.6, 13.2, 9.6], { all: 'dark', east: 'steel_light', west: 'steel_light' }),
  C('wheel_back', [3.4, 12.4, 10.4], [5.6, 13.2, 11.4], { all: 'dark', east: 'steel_light', west: 'steel_light' }),
  C('cap', [4, 10, 7.9], [5, 11, 8.1], { all: 'brass', south: null }),
];
const pole = () => [C('pole', [PARK_X - 0.6, 0, 9.4], [PARK_X + 0.6, 1, 10.6], { all: 'steel_light', up: null, down: null })];
function reel() {
  const out = [C('axle', [7.6, 7.6, 2.6], [8.4, 8.4, 10], { all: 'steel_light' })];
  for (const [z0, z1, end] of [[2.8, 3.3, 'front'], [7.2, 7.7, 'back']]) {
    const spoke = end === 'front' ? 'andesite' : 'steel';
    out.push(C(`spoke_h_${end}`, [5.9, 7.75, z0], [10.1, 8.25, z1], { all: spoke }));
    out.push(C(`spoke_v_${end}`, [7.75, 5.9, z0], [8.25, 10.1, z1], { all: spoke }));
    out.push(C(`hub_${end}`, [7.4, 7.4, z0 - 0.2], [8.6, 8.6, z1 + 0.2], { all: 'brass' }));
    // each rim bar runs between two neighbouring spoke tips (1.485 px from the hub on both axes), turned 45 degrees
    for (const [n, cx, cy, a] of [['ne', 9.05, 9.05, -45], ['nw', 6.95, 9.05, 45], ['se', 9.05, 6.95, 45], ['sw', 6.95, 6.95, -45]])
      out.push(C(`rim_${n}_${end}`, [cx - 1.5, cy - 0.15, z0], [cx + 1.5, cy + 0.15, z1], { all: spoke }, { rot: ['z', a, [cx, cy, (z0 + z1) / 2]] }));
  }
  // four walnut bats, each with a row of steel tines on its outer edge
  const bats = [['east', [9.35, 7.6], [9.95, 8.4], [9.95, 7.8], [10.45, 8.2]], ['west', [6.05, 7.6], [6.65, 8.4], [5.55, 7.8], [6.05, 8.2]],
    ['up', [7.6, 9.35], [8.4, 9.95], [7.8, 9.95], [8.2, 10.45]], ['down', [7.6, 6.05], [8.4, 6.65], [7.8, 5.55], [8.2, 6.05]]];
  for (const [n, a, b, ta, tb] of bats) {
    out.push(C(`bat_${n}`, [a[0], a[1], 3.3], [b[0], b[1], 7.2], { all: 'walnut' }));
    [3.7, 5.05, 6.4].forEach((z, i) => out.push(C(`tine_${n}_${i}`, [ta[0], ta[1], z], [tb[0], tb[1], z + 0.4], { all: 'steel_light' })));
  }
  return out;
}
const saw = () => [
  C('blade', [5.55, 5.55, 5.75], [10.45, 10.45, 6.25], { north: 'comedymonkeysaddons:block/auto_harvester_saw', south: 'comedymonkeysaddons:block/auto_harvester_saw', all: null },
    { uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] } }),
  C('hub', [7.3, 7.3, 5.4], [8.7, 8.7, 6.6], { all: 'steel_light' }),
  C('nut', [7.6, 7.6, 5.2], [8.4, 8.4, 5.4], { all: 'brass', south: null }),
  C('arbor', [7.6, 7.6, 6.6], [8.4, 8.4, 10], { all: 'steel' }),
];

// ---------- preview-only props (the game draws real plants and items) ----------
// A crop is drawn at the growth stage nearest its age (RealPlants): wheat's eight stage textures on crop planes at 0.45 scale
const cropStage = (k) => { const w = 'minecraft:block/wheat_stage' + k, uv = [0, 0, 16, 16]; return [
  C(`crop${k}_a`, [6.2, 3, 4.4], [6.2, 10.2, 11.6], { east: w, west: w, all: null }, { uv: { east: uv, west: uv } }),
  C(`crop${k}_b`, [9.8, 3, 4.4], [9.8, 10.2, 11.6], { east: w, west: w, all: null }, { uv: { east: uv, west: uv } }),
  C(`crop${k}_c`, [4.4, 3, 6.2], [11.6, 10.2, 6.2], { north: w, south: w, all: null }, { uv: { north: uv, south: uv } }),
  C(`crop${k}_d`, [4.4, 3, 9.8], [11.6, 10.2, 9.8], { north: w, south: w, all: null }, { uv: { north: uv, south: uv } }),
]; };
// A sapling is a cross of two planes at 0.45 scale until it's a third grown, then a small tree that grows to full size
const SAPLING = 'minecraft:block/oak_sapling';
const sapling = () => [
  C('sapling_a', [4.4, 3, 8], [11.6, 10.2, 8], { north: SAPLING, south: SAPLING, all: null }, { uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] }, rot: ['y', 45, [8, 3, 8]] }),
  C('sapling_b', [4.4, 3, 8], [11.6, 10.2, 8], { north: SAPLING, south: SAPLING, all: null }, { uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] }, rot: ['y', -45, [8, 3, 8]] }),
];
const LOG = 'comedymonkeysaddons:block/auto_harvester_log', LEAVES = 'comedymonkeysaddons:block/auto_harvester_leaves';
const grownTree = () => [
  C('log', [7, 3, 7], [9, 7, 9], { all: LOG }),
  C('leaves_x', [5, 7, 7], [11, 9, 9], { all: LEAVES }),
  C('leaves_z', [7, 7, 5], [9, 9, 11], { all: LEAVES }),
  C('leaves_top', [7, 9, 7], [9, 11, 9], { all: LEAVES }),
];
// ---------- assemble the preview ----------
const { cube, group, clip, write } = preview('auto-harvester', { 'own:': T });
function scene(prefix, kind, off) {
  const isTree = kind === 'tree';
  const cubes = (list, shift) => list.map((c) => cube(c, off, shift));
  // the tool is modelled at the hub (8, 8); in the preview it starts parked
  const toolShift = [PARK_X - 8, PARK_Y - 8, 0];
  const items = isTree ? ['minecraft:item/apple', 'minecraft:item/stick', 'minecraft:block/oak_sapling'] : ['minecraft:item/wheat', 'minecraft:item/wheat', 'minecraft:item/wheat'];
  return group(prefix, [8, 0, 8], off, [
    group(`${prefix}_shell`, [8, 0, 8], off, cubes(shell(isTree ? 'dirt' : 'soil'))),
    group(`${prefix}_shaft`, [8, 8, 8], off, cubes(shaftStub())),
    group(`${prefix}_plant`, [8, SOIL_Y, 8], off, isTree
      ? [group('tree_sapling', [8, SOIL_Y, 8], off, cubes(sapling())), group('tree_grown', [8, SOIL_Y, 8], off, cubes(grownTree()))]
      : [0, 1, 2, 3, 4, 5, 6, 7].map((k) => group(`crop_stage${k}`, [8, SOIL_Y, 8], off, cubes(cropStage(k))))),
    group(`${prefix}_carriage`, [PARK_X, 10, 10], off, [
      ...cubes(carriage()),
      // preview pole: a 1 px unit hanging from the carriage underside, stretched down from its top
      group(`${prefix}_pole`, [PARK_X, CARRIAGE_BOTTOM, 10], off, cubes(pole(), [0, CARRIAGE_BOTTOM - 1, 0])),
    ]),
    group(`${prefix}_tool`, [PARK_X, PARK_Y, 6], off, cubes(isTree ? saw() : reel(), toolShift)),
    ...[5, 8, 11].map((cx, i) => group(`${prefix}_item${i + 1}`, [cx, 2.5, 2.2], off, cubes(trayItem(`${prefix}_${i + 1}`, cx, items[i])))),
  ], { open: true });
}
const outliner = [scene('crop', 'crop', [0, 0, 0]), scene('tree', 'tree', [24, 0, 0])];

// ---------- clips ----------
// The renderer's harvest path (AutoHarvesterRenderer.renderHead), at t 0-1 into a harvest
function headAt(t) {
  let x = PARK_X, y = PARK_Y;
  if (t < 0) return { x, y };
  if (t < 0.15) y = lerp(PARK_Y, CUT_Y, smooth(t / 0.15));
  else if (t < 0.65) { x = lerp(PARK_X, SWEEP_END_X, (t - 0.15) / 0.5); y = CUT_Y; }
  else if (t < 0.8) { x = SWEEP_END_X; y = lerp(CUT_Y, PARK_Y, smooth((t - 0.65) / 0.15)); }
  else x = lerp(SWEEP_END_X, PARK_X, smooth((t - 0.8) / 0.2));
  return { x, y };
}
// grow_and_harvest: the plant grows through its stages with the head parked, then one 80-tick harvest cuts it back to a seedling
const GROW_S = 4, HARVEST_S = HARVEST_TICKS / 20, CUT_S = GROW_S + 0.65 * HARVEST_S, CYCLE_S = GROW_S + HARVEST_S;
const tAt = (s) => (s < GROW_S ? -1 : Math.min(1, (s - GROW_S) / HARVEST_S));
const growthAt = (s) => (s < GROW_S ? s / GROW_S : s < CUT_S ? 1 : 0);
function headTracks(prefix, degPerTick, harvesting = true) {
  const t = harvesting ? tAt : () => -1;
  return {
    [`${prefix}_carriage`]: { position: (s) => [headAt(t(s)).x - PARK_X, 0, 0] },
    [`${prefix}_pole`]: { scale: (s) => { const y = headAt(t(s)).y; return y < CARRIAGE_BOTTOM ? [1, CARRIAGE_BOTTOM - y, 1] : [1, 0, 1]; } },
    [`${prefix}_tool`]: {
      position: (s) => { const h = headAt(t(s)); return [h.x - PARK_X, h.y - PARK_Y, 0]; },
      // the renderer turns ZP(-spin); Blockbench's group Z rotation runs the same way round in this preview
      rotation: (s) => [0, 0, t(s) < 0 ? 0 : -degPerTick * t(s) * HARVEST_TICKS],
    },
  };
}
// growth g (0-1) -> which plant groups show, and how big (RealPlants.java:83-115)
function plantTracks(g) {
  const T = {};
  for (let k = 0; k < 8; k++) T[`crop_stage${k}`] = { scale: (s) => (Math.min(7, Math.floor(g(s) * 8)) === k ? SHOW : HIDE) };
  T.tree_sapling = { scale: (s) => { const v = g(s); if (v >= 0.35) return HIDE; const k = 0.6 + 0.4 * v / 0.35; return [k, k, k]; } };
  T.tree_grown = { scale: (s) => { const v = g(s); if (v < 0.35) return HIDE; const k = 0.65 + 0.35 * (v - 0.35) / 0.65; return [k, k, k]; } };
  return T;
}
clip('grow_and_harvest', CYCLE_S, 'loop', {
  ...headTracks('crop', REEL_DEG), ...headTracks('tree', SAW_DEG), ...plantTracks(growthAt),
}, {
  crop_item1: { scale: [[0, SHOW]] }, crop_item2: { scale: [[0, HIDE], [CUT_S, SHOW]] }, crop_item3: { scale: [[0, HIDE]] },
  tree_item1: { scale: [[0, SHOW]] }, tree_item2: { scale: [[0, HIDE], [CUT_S, SHOW]] }, tree_item3: { scale: [[0, HIDE]] },
});
// tray_fill: the tray's four states, 0 to 3 items, with the plant grown and the head parked; at 3 it's full and waits to be emptied
clip('tray_fill', 4, 'loop', { ...headTracks('crop', REEL_DEG, false), ...headTracks('tree', SAW_DEG, false), ...plantTracks(() => 1) },
  Object.fromEntries(['crop', 'tree'].flatMap((p) => [1, 2, 3].map((i) => [`${p}_item${i}`, { scale: [[0, HIDE], [i, SHOW]] }]))));

write(OUT, outliner);

module.exports = { shell, carriage, pole, reel, saw, OWN, PARK_X, PARK_Y, SWEEP_END_X, CUT_Y };
