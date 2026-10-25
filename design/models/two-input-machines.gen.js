// String Tangler and Stone Infuser redesign generator -> design/models/two-input-machines.bbmodel (Generic Model preview)
// To design/briefs/string-tangler.md and stone-infuser.md: both on the process-box steel shell with a front window,
// the output tray and the flanged back socket (the tangler in copper trim, the infuser in andesite). The tangler: a
// Create whisk on the shaft, two string spools behind side windows, a slime pot, the web growing on the whisk. The
// infuser: a turntable geared to the shaft, a 6 px stone on it with the new stone climbing it, a nugget feeder.
// Model space: window north, shaft south (MachineRenderers.java:26-29); the shared stub at x/y 6-10, z 12.5-16.
const path = require('path');
const { P, C, hash, mot, edge, HI, LO, textureSet, painters, preview, SHOW, HIDE } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'two-input-machines.bbmodel');

// ---------- textures ----------
const TG = textureSet(); TG.file = 'string_tangler_';
const SI = textureSet(); SI.file = 'stone_infuser_';
// the tangler's side windows onto its spools: z 5-11, y 5-11 (u 5-11, v 5-11 on both side faces)
const WIN = { z0: 5, z1: 11, y0: 5, y1: 11 };
const sideWindowFrame = (trim) => (x, y) => {
  const inX = x >= WIN.z0 - 1 && x <= WIN.z1, inY = y >= 16 - WIN.y1 - 1 && y <= 16 - WIN.y0;
  if (!inX || !inY) return null;
  if (x === WIN.z0 - 1 || y === 16 - WIN.y1 - 1) return mot(trim, x, y, 60);
  if (x === WIN.z1 || y === 16 - WIN.y0) return P[trim][0];
  return null;
};
// the infuser's sides: the riveted inspection plate the cobble generators have
const plate = (x, y) => {
  if (x < 5 || x > 10 || y < 5 || y > 10) return null;
  if (x === 5 || y === 5) return HI;
  if (x === 10 || y === 10) return LO;
  if ((x === 6 || x === 9) && (y === 6 || y === 9)) return '#5f656e';
  return hash(x, y, 40) > 0.86 ? '#4a5059' : '#454b53';
};
const boardsTop = (trim) => (x, y) => {
  const e = edge(x, y);
  if (e === 0) return mot(trim, x, y, 61);
  if (e === 1) return '#2e2118';
  return y % 4 === 1 ? '#2e2118' : mot('walnut', x + (y >> 2) * 5, y, 62);
};
for (const [set, trim, side] of [[TG, 'copper', (t) => painters.side(t, sideWindowFrame(t))], [SI, 'andesite', (t) => painters.side(t, plate)]]) {
  set.paint('front', painters.front(trim));
  set.paint('side', side(trim));
  set.paint('back', painters.back(trim));
  set.paint('bottom', painters.bottom(trim));
  set.paint('top', boardsTop(trim));
  set.paint('glass', painters.glass());
  set.paint('chamber', painters.chamber());
  set.paint('ceiling', painters.ceiling());
  set.paint('floor', painters.floor());
  set.paint('steel', painters.steel());
  set.paint('steel_light', painters.steelLight());
  set.paint('dark', painters.dark());
  set.paint('walnut', painters.walnut());
  set.paint('trim', (x, y) => mot(trim, x, y, 63));
}
TG.paint('copper', painters.copper());
// string: muted off-white, never pure white
TG.paint('string', (x, y) => (y % 2 === 0 ? '#a39d8b' : hash(x, y, 64) > 0.7 ? '#b9b29f' : '#ada795'));
P.slime = ['#4e7a44', '#5f8a4e', '#6f9a58'];
TG.paint('slime', (x, y) => (hash(x, y, 65) > 0.85 ? '#7faa62' : mot('slime', x, y, 66)));
// the web: a muted grey-white mesh, cut out between the threads
TG.paint('web', (x, y) => {
  const cx = x - 7.5, cy = y - 7.5, r = Math.hypot(cx, cy), a = Math.atan2(cy, cx);
  if (r > 7.6) return null;
  const spoke = Math.abs(Math.sin(a * 4)) < 0.2, ring = Math.abs((r % 2.5) - 1.25) < 0.45;
  return spoke || ring ? (r < 3 ? '#b0aa97' : '#a39d8b') : null;
});
// preview only: cobwebs waiting in the tray
TG.paint('cobweb', (x, y) => ((x === y || x === 15 - y || x === 7 || y === 8 || (Math.abs(Math.hypot(x - 7.5, y - 7.5) - 5) < 0.6)) ? '#b0aa97' : null));
// the turntable: a steel top with radial grooves, an andesite rim
SI.paint('turntable', (x, y) => {
  const cx = x - 7.5, cy = y - 7.5, r = Math.hypot(cx, cy);
  if (r > 6.8) return mot('andesite', x, y, 67);
  if (Math.abs(Math.sin(Math.atan2(cy, cx) * 3)) < 0.12 && r > 2) return '#2f343b';
  return r < 1.5 ? '#5f656e' : mot('steelLight', x, y, 68);
});
SI.paint('andesite', painters.andesite());
// a bearing face on the drive housing
SI.paint('housing', (x, y) => { const r = Math.hypot(x - 7.5, y - 7.5); return r < 1.5 ? '#1f2327' : r < 2.4 ? '#2a2f35' : mot('panel', x, y, 69); });
// preview only: the result's colour (asurine), nuggets in the feeder, finished stones in the tray
SI.paint('asurine', (x, y) => (hash(x >> 1, y >> 1, 70) > 0.6 ? '#4f7c96' : hash(x, y, 71) > 0.8 ? '#6a95a8' : '#3f6478'));
SI.paint('nuggets', (x, y) => (hash(x, y, 72) > 0.55 ? (hash(x, y, 73) > 0.5 ? '#8a8f95' : '#a3a8ad') : '#5f656e'));

// ---------- the shells ----------
function shell(k, { sideWindows = false } = {}) {
  const t = (n) => `${k}:${n}`;
  const S = { north: t('front'), east: t('side'), west: t('side'), south: t('back') };
  const wall = (name, x0, x1, inner, outer) => {
    if (!sideWindows) return [C(name, [x0, 2, 0], [x1, 14, 16], { north: S.north, south: S.south, [outer]: t('side'), [inner]: t('chamber'), up: null, down: null })];
    // with a window onto the spool: the wall in four pieces round it, and a pane in the middle of the wall
    const f = { [outer]: t('side'), [inner]: t('chamber') };
    return [
      C(`${name}_low`, [x0, 2, 0], [x1, WIN.y0, 16], { ...f, north: S.north, south: S.south, up: t('trim'), down: null }),
      C(`${name}_high`, [x0, WIN.y1, 0], [x1, 14, 16], { ...f, north: S.north, south: S.south, down: t('trim'), up: null }),
      C(`${name}_front`, [x0, WIN.y0, 0], [x1, WIN.y1, WIN.z0], { ...f, north: S.north, south: t('trim'), up: null, down: null }),
      C(`${name}_back`, [x0, WIN.y0, WIN.z1], [x1, WIN.y1, 16], { ...f, south: S.south, north: t('trim'), up: null, down: null }),
      C(`${name}_pane`, [(x0 + x1) / 2, WIN.y0, WIN.z0], [(x0 + x1) / 2, WIN.y1, WIN.z1], { east: t('glass'), west: t('glass'), all: null }, { shade: false }),
    ];
  };
  return [
    C('top', [0, 14, 0], [16, 16, 16], { ...S, up: t('top'), down: t('ceiling') }),
    C('bottom', [0, 0, 0], [16, 2, 16], { ...S, down: t('bottom'), up: t('floor') }),
    ...wall('wall_west', 0, 2, 'east', 'west'),
    ...wall('wall_east', 14, 16, 'west', 'east'),
    C('wall_back', [2, 2, 14], [14, 14, 16], { south: S.south, north: t('chamber'), east: null, west: null, up: null, down: null }),
    C('glass', [2, 2, 1.2], [14, 14, 1.2], { north: t('glass'), south: t('glass'), all: null }, { shade: false }),
    // the output tray at the front, as the harvester's (TrayItems draws at z 2.7, x 5 / 8 / 11, on bedY 2.5)
    C('tray_front', [3, 2, 1.4], [13, 2.9, 1.9], { all: t('walnut'), up: t('trim'), down: null }),
    C('tray_back', [3, 2, 3.3], [13, 2.9, 3.8], { all: t('walnut'), up: t('trim'), down: null }),
    C('tray_west', [3, 2, 1.9], [3.5, 2.9, 3.3], { all: t('walnut'), up: t('trim'), down: null, north: null, south: null }),
    C('tray_east', [12.5, 2, 1.9], [13, 2.9, 3.3], { all: t('walnut'), up: t('trim'), down: null, north: null, south: null }),
    C('tray_bed', [3.5, 2, 1.9], [12.5, 2.5, 3.3], { up: t('walnut'), all: null }),
  ];
}

// ---------- the String Tangler ----------
const WHISK = { x: 8, y: 8 };
// the tangler's own static parts: spool axles in their wall brackets, and the slime pot
function tanglerStatic() {
  const t = (n) => 'tg:' + n, out = [];
  for (const [n, x0, x1] of [['west', 2, 3], ['east', 13, 14]]) out.push(C(`axle_bracket_${n}`, [x0, 7, 7], [x1, 9, 9], { all: t('steel'), [n === 'west' ? 'west' : 'east']: null }));
  // the slime pot: a small glass dish on the floor under the whisk, behind the tray
  out.push(
    C('pot_floor', [6.5, 2, 4.5], [9.5, 2.25, 7.5], { up: t('dark'), all: null }),
    C('pot_front', [6.5, 2, 4.5], [9.5, 3.5, 4.5], { north: t('glass'), south: t('glass'), all: null }, { shade: false }),
    C('pot_back', [6.5, 2, 7.5], [9.5, 3.5, 7.5], { north: t('glass'), south: t('glass'), all: null }, { shade: false }),
    C('pot_west', [6.5, 2, 4.5], [6.5, 3.5, 7.5], { east: t('glass'), west: t('glass'), all: null }, { shade: false }),
    C('pot_east', [9.5, 2, 4.5], [9.5, 3.5, 7.5], { east: t('glass'), west: t('glass'), all: null }, { shade: false }),
    // a copper ring round the top of the pot, open so the slime shows
    C('pot_rim_n', [6.25, 3.5, 4.25], [9.75, 3.75, 4.75], { up: t('trim'), all: t('copper'), down: null }),
    C('pot_rim_s', [6.25, 3.5, 7.25], [9.75, 3.75, 7.75], { up: t('trim'), all: t('copper'), down: null }),
    C('pot_rim_w', [6.25, 3.5, 4.75], [6.75, 3.75, 7.25], { up: t('trim'), all: t('copper'), down: null, north: null, south: null }),
    C('pot_rim_e', [9.25, 3.5, 4.75], [9.75, 3.75, 7.25], { up: t('trim'), all: t('copper'), down: null, north: null, south: null }),
  );
  return out;
}
// the whisk, on the shaft's axis (model Z): a hub, and four balloon loops of wire bowing out from it
function whisk() {
  const t = (n) => 'tg:' + n, out = [C('hub', [WHISK.x - 0.5, WHISK.y - 0.5, 4], [WHISK.x + 0.5, WHISK.y + 0.5, 12.25], { all: t('steel_light') }),
    C('cap', [WHISK.x - 0.75, WHISK.y - 0.75, 3.5], [WHISK.x + 0.75, WHISK.y + 0.75, 4], { all: t('copper') }),
    C('collar', [WHISK.x - 1, WHISK.y - 1, 11.5], [WHISK.x + 1, WHISK.y + 1, 12.5], { all: t('copper') })];
  const w = 0.25, R = 2.25, Z0 = 5, Z1 = 11; // half a wire, the loops' radius and their front and back
  // four loops (flat, upright and the two diagonals): each a wire either side of the hub, joined to it by a spoke at
  // the front and the back
  for (const [li, rot, upright] of [[0, 0, false], [1, 0, true], [2, 45, false], [3, -45, false]]) {
    const o = rot ? { rot: ['z', rot, [WHISK.x, WHISK.y, 8]] } : {};
    for (const side of [-1, 1]) {
      const at = (a, b) => (upright ? [WHISK.x + b, WHISK.y + a] : [WHISK.x + a, WHISK.y + b]);
      const [wx, wy] = at(side * R, 0);
      out.push(C(`wire_${li}_${side}`, [wx - w, wy - w, Z0], [wx + w, wy + w, Z1], { all: t('steel_light') }, o));
      for (const z of [Z0, Z1 - 2 * w]) {
        // a touch thinner than the wire, so where they meet the spoke's faces sit inside it
        const sw = w * 0.8, [a0, b0] = at(side > 0 ? 0.5 : -R + sw, -sw), [a1, b1] = at(side > 0 ? R - sw : -0.5, sw);
        out.push(C(`spoke_${li}_${side}_${z}`, [Math.min(a0, a1), Math.min(b0, b1), z + 0.05], [Math.max(a0, a1), Math.max(b0, b1), z + 2 * w - 0.05], { all: t('steel_light') }, o));
      }
    }
  }
  return out;
}
// a string spool on an axle along X, centred on (x, 8, 8): two flanges (square and turned 45 so they read round)
// round a core; the winding is drawn separately so the renderer can size it by the string count
function spool(name, x) {
  const t = (n) => 'tg:' + n, out = [C(`${name}_core`, [x - 1, 7.25, 7.25], [x + 1, 8.75, 8.75], { all: t('walnut') })];
  for (const fx of [x - 1.25, x + 0.75]) {
    out.push(C(`${name}_flange_${fx}`, [fx, 6.5, 6.5], [fx + 0.5, 9.5, 9.5], { all: t('walnut') }));
    out.push(C(`${name}_flange45_${fx}`, [fx + 0.05, 6.5, 6.5], [fx + 0.45, 9.5, 9.5], { all: t('walnut') }, { rot: ['x', 45, [x, 8, 8]] }));
  }
  return out;
}
const winding = (name, x) => [C(`${name}_winding`, [x - 0.75, 6.75, 6.75], [x + 0.75, 9.25, 9.25], { all: 'tg:string' })];
const slime = () => [C('slime', [7, 2.25, 5], [9, 3.25, 7], { all: 'tg:slime', down: null })];
// the web, balled on the whisk: three crossed planes, modelled full size and scaled by progress
const web = () => [
  C('web_xy', [5, 5, 8], [11, 11, 8], { north: 'tg:web', south: 'tg:web', all: null }, { uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] } }),
  C('web_yz', [8, 5, 5], [8, 11, 11], { east: 'tg:web', west: 'tg:web', all: null }, { uv: { east: [0, 0, 16, 16], west: [0, 0, 16, 16] } }),
  C('web_xz', [5, 8, 5], [11, 8, 11], { up: 'tg:web', down: 'tg:web', all: null }, { uv: { up: [0, 0, 16, 16], down: [0, 0, 16, 16] } }),
];
// preview only: strands from each spool's winding to the whisk, and cobwebs in the tray
const strands = () => [
  C('strand_w', [4.5, 7.9, 7.9], [7.5, 8.1, 8.1], { all: 'tg:string' }, { rot: ['y', -22.5, [6, 8, 8]] }),
  C('strand_e', [8.5, 7.9, 7.9], [11.5, 8.1, 8.1], { all: 'tg:string' }, { rot: ['y', 22.5, [10, 8, 8]] }),
];
const trayWebs = () => [5, 8, 11].map((cx, i) => C(`tray_web_${i}`, [cx - 1.5, 2.5, 2.7], [cx + 1.5, 5.5, 2.7], { north: 'tg:cobweb', south: 'tg:cobweb', all: null }, { uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] }, rot: ['x', -22.5, [cx, 2.5, 2.7]] }));

// ---------- the Stone Infuser ----------
const TABLE = { x: 8, z: 7.75, r: 3.75 };
const STONE = { x0: 5, x1: 11, z0: 4.75, z1: 10.75, y0: 3.5, y1: 9.5 };
// static: the drive housing under the stub and the nugget feeder on the west wall (the right as you face it)
function infuserStatic() {
  const t = (n) => 'si:' + n;
  return [
    C('housing', [6, 2, 12], [10, 5.5, 14], { all: t('steel'), north: t('housing'), south: null }, { uv: { north: [0, 0, 16, 16] } }),
    // the feeder: a glass-fronted hopper on a bracket, its spout over the stone's edge
    // its front is glass, so the nuggets show through the window
    C('feeder_window', [2, 10.25, 5.6], [5.25, 13.25, 5.6], { north: t('glass'), south: t('glass'), all: null }, { shade: false }),
    C('feeder_frame', [2, 13.25, 5.25], [5.5, 13.5, 5.5], { all: t('steel') }),
    C('feeder_rear', [2, 10, 9.25], [5.5, 13.5, 9.5], { all: t('steel'), north: t('dark') }),
    C('feeder_side', [5.25, 10, 5.75], [5.5, 13.5, 9.25], { east: t('glass'), west: t('glass'), all: null }, { shade: false }),
    C('feeder_floor', [2, 10, 5.75], [5.25, 10.25, 9.25], { all: t('steel'), up: t('dark') }),
    C('feeder_lip', [2, 13.5, 5.5], [5.75, 13.75, 9.5], { all: t('trim') }),
    C('spout', [5.5, 10, 7], [6.75, 10.5, 8.25], { all: t('steel_light'), down: t('dark') }),
  ];
}
// the turntable: an octagon (a square and a square turned 45) with its crown gear under it; spins about Y at TABLE
function turntable() {
  const t = (n) => 'si:' + n, r = TABLE.r;
  const sq = (name, y0, y1, rr, tex, o = {}) => C(name, [TABLE.x - rr, y0, TABLE.z - rr], [TABLE.x + rr, y1, TABLE.z + rr], { all: t('andesite'), up: tex, down: null }, o);
  return [
    sq('table', 2.75, 3.5, r * 0.92, t('turntable'), { uv: { up: [0, 0, 16, 16] } }),
    sq('table_45', 2.76, 3.49, r * 0.92, t('turntable'), { uv: { up: [0, 0, 16, 16] }, rot: ['y', 45, [TABLE.x, 3, TABLE.z]] }),
    sq('crown', 2.25, 2.55, 1.5, t('steel')),
  ];
}
// the drive: a short rod from the housing to the crown gear, spinning about Z
const drive = () => [C('drive', [7.5, 2, 8.5], [8.5, 2.75, 12], { all: 'si:steel_light' })];
// the stone on the table (the renderer draws cobblestone and the climbing band from the result's sprite)
const stone = () => [C('stone', [STONE.x0, STONE.y0, STONE.z0], [STONE.x1, STONE.y1, STONE.z1], { all: 'minecraft:block/cobblestone', down: null })];
const band = () => [C('band', [STONE.x0 - 0.1, STONE.y0 - 0.1, STONE.z0 - 0.1], [STONE.x1 + 0.1, STONE.y1 - 0.1, STONE.z1 + 0.1], { all: 'si:asurine', down: null })];
const nuggets = () => [C('nuggets', [2.25, 10.25, 6], [5, 11.75, 9], { up: 'si:nuggets', east: 'si:nuggets', all: null })];
// a block item in the tray draws about 1.5 px across (FIXED display x TrayItems' 0.18 scale)
const trayStones = () => [5, 8, 11].map((cx, i) => C(`tray_stone_${i}`, [cx - 0.75, 2.5, 2.0], [cx + 0.75, 4, 3.5], { all: 'si:asurine', down: null }));

// ---------- preview ----------
const { cube, group, clip, write } = preview('two-input-machines', { 'tg:': TG, 'si:': SI });
const cubes = (list, off) => list.map((c) => cube(c, off));
const outliner = [];
for (const [name, off, busy] of [['tangler', [0, 0, 0], true], ['tangler_idle', [0, 0, 28], false]]) outliner.push(group(name, [8, 0, 8], off, [
  group(`${name}_shell`, [8, 0, 8], off, cubes([...shell('tg', { sideWindows: true }), ...tanglerStatic()], off)),
  group(`${name}_whisk`, [WHISK.x, WHISK.y, 8], off, cubes(whisk(), off)),
  group(`${name}_spool_w`, [3.5, 8, 8], off, cubes([...spool('spool_w', 3.5), ...winding('spool_w', 3.5)], off)),
  group(`${name}_spool_e`, [12.5, 8, 8], off, cubes([...spool('spool_e', 12.5), ...winding('spool_e', 12.5)], off)),
  group(`${name}_slime`, [8, 2.25, 6], off, cubes(slime(), off)),
  group(`${name}_web`, [8, 8, 8], off, cubes(web(), off)),
  group(`${name}_strands`, [8, 8, 8], off, cubes(strands(), off)),
  group(`${name}_tray`, [8, 2.5, 2.7], off, cubes(busy ? trayWebs() : [], off)),
]));
for (const [name, off, busy] of [['infuser', [24, 0, 0], true], ['infuser_idle', [24, 0, 28], false]]) outliner.push(group(name, [8, 0, 8], off, [
  group(`${name}_shell`, [8, 0, 8], off, cubes([...shell('si'), ...infuserStatic(), ...nuggets()], off)),
  group(`${name}_table`, [TABLE.x, 3, TABLE.z], off, [...cubes(turntable(), off),
    group(`${name}_stone`, [TABLE.x, 3.5, TABLE.z], off, cubes(stone(), off)),
    group(`${name}_band`, [TABLE.x, STONE.y0, TABLE.z], off, cubes(band(), off))]),
  group(`${name}_drive`, [8, 2.375, 10], off, cubes(drive(), off)),
  group(`${name}_tray`, [8, 2.5, 2.7], off, cubes(busy ? trayStones() : [], off)),
]));
// working: the whisk turns with the shaft (64 RPM, shown at a quarter speed so it reads), the spools pay out
// slower, the web grows to full over the job; the infuser's table turns geared down 4:1 and the band climbs
const LEN = 4, deg = (t) => t * 20 * 64 * 6 * 0.25;
clip('working', LEN, 'loop', {
  tangler_whisk: { rotation: (t) => [0, 0, deg(t) % 360] },
  tangler_spool_w: { rotation: (t) => [(deg(t) / 6) % 360, 0, 0] },
  tangler_spool_e: { rotation: (t) => [(-deg(t) / 6) % 360, 0, 0] },
  tangler_web: { scale: (t) => { const s = Math.max(0.02, t / LEN); return [s, s, s]; } },
  infuser_table: { rotation: (t) => [0, (deg(t) / 4) % 360, 0] },
  infuser_drive: { rotation: (t) => [0, 0, deg(t) % 360] },
  infuser_band: { scale: (t) => [1, Math.max(0.02, t / LEN), 1] },
}, {
  tangler_idle_web: { scale: [[0, HIDE]] }, tangler_idle_slime: { scale: [[0, [1, 0.3, 1]]] },
  tangler_idle_spool_w: { scale: [[0, [1, 0.5, 0.5]]] },
  infuser_idle_band: { scale: [[0, HIDE]] },
});
write(OUT, outliner);

module.exports = { TG, SI, WIN, WHISK, TABLE, STONE, shell, tanglerStatic, whisk, spool, winding, slime, web, infuserStatic, turntable, drive, stone, band };
