// Monster Cages generator -> design/models/monster-cage.bbmodel (Generic Model preview with clips).
// Built in the game's model space (window north), on the family kit; see design/briefs/monster-cage.md.
// Sister to the Animal Pens (animal-pen.gen.js): same shell, tray and stub; the cage adds bars, a stone cell, a chow bowl,
// a padlock and a grated top. Five blocks: andesite, brass, flooded andesite, flooded brass, netherite.
// Renderer contract carried over from the pens: floor at y 2.5, feet wander x 3.5-12.5 and z 4.5-8.8, tray x 3-13 z 1.2-3.8,
// shaft stub at x6-10 y6-10 z12.5-16. New for the cage renderer: chow items in the bowl at (6.6 / 8 / 9.4, 3.1, 11.6), scale 0.2.
const path = require('path');
const { P, C, textureSet, painters, mot, hash, edge, FIELD, HI, LO, shaftStub, preview, SHOW, HIDE, lerp, trayItem } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'monster-cage.bbmodel');
const FLOOR_Y = 2.5;
const WANDER = { x0: 3.5, x1: 12.5, z0: 4.5, z1: 8.8 };
const GLASS_Z = 0.5;                  // the cage's glass sits further forward than the pen's, to make room for the bars
const BAR_Z = [1.0, 1.35];            // bars: clear of the glass (0.5) and the water's front (0.75); the tray (1.4) faces the other way
const BARS_X = [4.5, 7.5, 10.5];      // three 1 px bars
const CROSS_Y = [9.5, 10.5];
const CHOW = [[6.6, -30], [8, 40], [9.4, 110]];
const GAUGE = { z0: 7, z1: 9, y0: 3, y1: 13, depth: 0.5 };   // the pens' sight glass (Flooded cages: water)
const VENT = { z0: 6, z1: 10, y0: 6, y1: 10, depth: 0.5 };   // dry cages: a barred vent; netherite: a power terminal
const LOCK = { y0: 5.5, y1: 8.5, depth: 0.6 };               // the padlock pocket in the west (viewer's right) post

// ---------- palette ----------
Object.assign(P, {
  brassMid: ['#7e6832', '#8f7638', '#a3873f'],
  brassTrim: ['#6e5a2a', '#7e6832', '#8f7638'],
  iron: ['#3c424a', '#454b53', '#5f656e'],
  stone: ['#33363a', '#3d4045', '#474a4f'],
  flag: ['#3a3c40', '#3f4245', '#45484c'],
  tile: ['#2c3438', '#30393d', '#353f44'],
  neth: ['#31292a', '#3a3233', '#443a3b'],
  nethEdge: ['#4d4345', '#554a4c', '#5d5052'],
  obsid: ['#1b1424', '#221a2c', '#2e1f3d'],
  blackstone: ['#1f1a22', '#2b2430', '#352d3a'],
  copperC: ['#9a5038', '#a75a40', '#b26247'],
  chow: ['#5a4630', '#6e5a3a', '#4f5a34'],
  kelp: ['#2f4a2a', '#3e5e33', '#4f7340'],
  zombie: ['#3e5e33', '#4f7340', '#46683a'],
  drowned: ['#2f5563', '#3a6670', '#336a77'],
  cloth: ['#2f4a6a', '#354f70', '#3a5a7a'],
  creeper: ['#3e6a33', '#4f7a40', '#5a8a48'],
  wither: ['#1c1c1e', '#262628', '#303033'],
});

// ---------- textures ----------
const CAGE = textureSet(); CAGE.file = 'monster_cage_';          // andesite frame + everything both dry/flooded tiers share
const BRASS = textureSet(); BRASS.file = 'brass_monster_cage_';  // brass frame, grate, lock, lips
const NETH = textureSet(); NETH.file = 'netherite_monster_cage_';

// side faces: a trim ring round a recess; u = z, v = 16 - y on a west face
const ringAround = (r) => (x, y) => {
  const u0 = r.z0 - 1, u1 = r.z1, v0 = 16 - r.y1 - 1, v1 = 16 - r.y0;
  const inBox = x >= u0 && x <= u1 && y >= v0 && y <= v1;
  if (!inBox) return null;
  return x === u0 || x === u1 || y === v0 || y === v1 ? (x === u0 || y === v0 ? LO : HI) : '#1f2327';
};
const gaugeFrame = (x, y) => {
  if (x < 6 || x > 9 || y < 2 || y > 13) return null;
  if (x === 6 || x === 9 || y === 2 || y === 13) return mot('copper', x, y, 21);
  return '#1f2327';
};
for (const [set, trim] of [[CAGE, 'andesite'], [BRASS, 'brassMid']]) {
  set.paint('front', painters.front(trim));
  set.paint('side', painters.side(trim, ringAround(VENT)));
  set.paint('side_flooded', painters.side(trim, gaugeFrame));
  set.paint('back', painters.back(trim, trim === 'brassMid' ? '#a3873f' : undefined));
  set.paint('bottom', painters.bottom(trim));
  // grated lid: 1 px bars every 2 px over darkness, in a trim frame with a bevel
  set.paint('grate', (x, y) => {
    const e = edge(x, y);
    if (e === 0) return mot(trim, x, y, 40);
    if (e === 1) return x === 1 || y === 1 ? LO : HI;
    if (x === 7 || x === 8) return mot('iron', x, y, 41);          // the stiffening bar across the middle
    return x % 2 === 0 ? mot('iron', x, y, 42) : (y % 4 === 0 ? '#2a2f35' : '#141619');
  });
}
// Netherite: plate, its own trim, the hazard band under the window and a blank owner plate top-left
const nethPlate = (x, y, s) => (hash(x, y, s) > 0.86 ? '#3a3233' : '#352e2f');
const nethEdgePx = (x, y) => {
  if (edge(x, y) === 0) return mot('nethEdge', x, y, 50);
  if (edge(x, y) === 1) return x === 1 || y === 1 ? '#221c1d' : '#4d4345';
  return null;
};
NETH.paint('front', (x, y) => {
  if (y === 14 && x >= 2 && x <= 13) return x % 2 === 0 ? '#8f7a3a' : '#1c1a1a';   // the one hazard band
  if (edge(x, y) === 0) return mot('nethEdge', x, y, 51);
  if (x >= 1 && x <= 2 && y >= 2 && y <= 3) return y === 2 ? '#a3873f' : '#8f7638';               // owner plate (viewer's left)
  if ((x === 1 || x === 14) && (y === 1)) return '#5d5052';
  return nethPlate(x, y, 52);
});
NETH.paint('side', (x, y) => nethEdgePx(x, y) || ringAround(VENT)(x, y) || nethPlate(x, y, 53));
NETH.paint('back', (x, y) => {
  const e = nethEdgePx(x, y);
  if (e) return e;
  const r = Math.hypot(x - 7.5, y - 7.5);
  if (r < 2.2) return '#120f14';
  if (r < 3.2) return x + y < 14 ? '#5d5052' : '#3a3233';
  if (r < 3.8) return '#221c1d';
  if ((x === 4 || x === 11) && (y === 4 || y === 11)) return '#5d5052';
  return nethPlate(x, y, 54);
});
NETH.paint('bottom', (x, y) => nethEdgePx(x, y) || nethPlate(x, y, 55));
NETH.paint('top', (x, y) => {
  const e = nethEdgePx(x, y);
  if (e) return e;
  const r = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5));
  if (r < 2.5) return '#120f14';                                    // the beacon well (the lens sits in it)
  if (r < 3.5) return '#221c1d';
  return y === 5 || y === 10 ? '#2b2425' : nethPlate(x, y, 56);
});
NETH.paint('cell', (x, y) => {
  const row = y >> 2, off = row % 2 ? 4 : 0;
  if (y % 4 === 3 || (x + off) % 8 === 0) return '#120f14';
  return hash(x, y, 57) > 0.7 ? mot('obsid', x, y, 58) : mot('blackstone', x, y, 59);
});
NETH.paint('ceiling', (x, y) => mot('blackstone', x, y, 60));
NETH.paint('floor', (x, y) => ((x % 5 === 0 || y % 5 === 0) ? '#120f14' : mot('blackstone', x, y, 61)));
NETH.paint('plate', (x, y) => mot('neth', x, y, 62));
NETH.paint('bars', (x, y) => (y % 5 === 0 ? '#3a3233' : mot('neth', x, y, 63)));
NETH.paint('port', (x, y) => {                                       // the sealed hopper port's blast door
  const r = Math.hypot(x - 7.5, y - 7.5);
  if (r > 7.6) return null;
  if (r > 6.4) return mot('nethEdge', x, y, 64);
  if (Math.abs(x - y) < 1 || Math.abs(x + y - 15) < 1) return '#221c1d';
  return r < 1.6 ? '#5d5052' : mot('neth', x, y, 65);
});
NETH.paint('coil', (x, y) => (y % 2 === 0 ? mot('copperC', x, y, 66) : '#7a3f2c'));
NETH.paint('coil_cap', (x, y) => mot('copperC', x, y, 67));
NETH.paint('tip_lit', (x, y) => (hash(x, y, 68) > 0.6 ? '#6fb7c4' : '#4a86c4'));
NETH.paint('tip_dark', (x, y) => '#1f2a36');
NETH.paint('terminal', (x, y) => (edge(x, y) <= 1 ? '#7a3f2c' : mot('copperC', x, y, 69)));
NETH.paint('collar', (x, y) => (y % 3 === 0 ? '#120f14' : '#1f1a22'));
NETH.paint('lens', (x, y) => (x % 3 === 0 || y % 4 === 0 ? (hash(x, y, 70) > 0.5 ? '#5a221c' : '#4a1c17') : null));
NETH.paint('lens_lit', (x, y) => (x % 3 === 0 || y % 4 === 0 ? (hash(x, y, 71) > 0.5 ? '#c4553c' : '#a3412f') : null));
NETH.paint('reflector', (x, y) => (x < 8 ? '#8a8c86' : '#5f656e'));
NETH.paint('guard', (x, y) => mot('nethEdge', x, y, 72));
NETH.paint('tray', (x, y) => mot('neth', x, y, 73));
// the shield: glowing blue, translucent, a drifting cell grid and a brighter rim where its faces meet
NETH.paint('shield', (x, y) => {
  if (x === 0 || y === 0 || x === 15 || y === 15) return '#4a96e0c0';
  // big hexagon-ish cells: horizontal runs every 6 px, short verticals offset row to row
  const row = Math.floor((y + 3) / 6), yy = (y + 3) % 6;
  const grid = (yy === 0 && (x + row * 4) % 8 !== 0) || (yy !== 0 && (x + row * 4) % 8 === 0);
  return grid ? '#3d82d890' : (hash(x, y, 74) > 0.85 ? '#2f6fc840' : '#1f4f9a28');
});

const SHIELD_FRAMES = 12;
{
  const { png } = require('./process-box.lib.js');
  const rgbaOf = (h, a) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), Math.max(0, Math.min(255, Math.round(a)))];
  const d = new Uint8Array(16 * 16 * SHIELD_FRAMES * 4);
  for (let f = 0; f < SHIELD_FRAMES; f++) {
    const pulse = 1 + 0.25 * Math.sin((f / SHIELD_FRAMES) * Math.PI * 2);
    const sweep = 15 - Math.round((f / SHIELD_FRAMES) * 18) + 1;            // shimmer row, bottom to top
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      const yy0 = (y + f) % 12, row = Math.floor((yy0 + 3) / 6), yy = (yy0 + 3) % 6;
      const grid = (yy === 0 && (x + row * 4) % 8 !== 0) || (yy !== 0 && (x + row * 4) % 8 === 0);
      const shimmer = Math.abs(y - sweep) <= 0 ? 1.8 : Math.abs(y - sweep) === 1 ? 1.35 : 1;
      let c;
      if (x === 0 || y === 0 || x === 15 || y === 15) c = rgbaOf('#4a96e0', 0xc0 * pulse);
      else if (grid) c = rgbaOf(shimmer > 1 ? '#6fb7e8' : '#3d82d8', 0x90 * pulse * shimmer);
      else c = rgbaOf(shimmer > 1 ? '#3d82d8' : (hash(x, y, 74) > 0.85 ? '#2f6fc8' : '#1f4f9a'), (hash(x, y, 74) > 0.85 ? 0x40 : 0x28) * pulse * shimmer);
      d.set(c, ((f * 16 + y) * 16 + x) * 4);
    }
  }
  NETH.OWN.shield = png(16, 16 * SHIELD_FRAMES, d);
}

// interiors shared by andesite and brass (dry and flooded)
CAGE.paint('glass', painters.glass());
CAGE.paint('cell', (x, y) => {                                      // dark stone brick
  const row = y >> 2, off = row % 2 ? 4 : 0;
  if (y % 4 === 3 || (x + off) % 8 === 0) return '#2a2c30';
  return mot('stone', x + off, y, 30);
});
CAGE.paint('tiles', (x, y) => {                                     // flooded: wet tiles with a faint green tide line
  if (x % 4 === 0 || y % 4 === 0) return '#222a2e';
  if (y === 9 && hash(x, y, 31) > 0.3) return '#2f4a2a';
  return mot('tile', x, y, 32);
});
CAGE.paint('ceiling', (x, y) => mot('stone', x, y, 33));
CAGE.paint('floor', (x, y) => {                                     // flagstones with a drain grate in the middle
  if (x >= 6 && x <= 9 && y >= 6 && y <= 9) return (x === 6 || x === 9 || y === 6 || y === 9) ? '#5f656e' : (x % 2 ? '#141619' : '#3c424a');
  if (x === 5 || y === 11 || (y < 11 && x === 12)) return '#2a2c30';
  return mot('flag', x, y, 34);
});
CAGE.paint('floor_wet', (x, y) => (x % 4 === 0 || y % 4 === 0 ? '#222a2e' : mot('tile', x, y, 35)));
CAGE.paint('bars', (x, y) => (y % 6 === 0 ? '#5f656e' : mot('iron', x, y, 36)));
CAGE.paint('iron', (x, y) => mot('iron', x, y, 37));
CAGE.paint('bowl', (x, y) => (hash(x, y, 38) > 0.85 ? '#5f656e' : '#4a5059'));
CAGE.paint('bowl_rim', (x, y) => (hash(x, y, 39) > 0.7 ? '#7c7f79' : '#6f757d'));
CAGE.paint('lock', (x, y) => {
  if (edge(x, y) === 0) return '#2a2f35';
  if (x >= 5 && x <= 10 && y >= 5 && y <= 11) return x === 7 && y >= 7 && y <= 9 ? '#141619' : mot('brassMid', x, y, 44);   // the brass keyplate
  return mot('iron', x, y, 43);
});
CAGE.paint('keyplate', (x, y) => (x === 8 && y >= 7 && y <= 9 ? '#141619' : mot('brassMid', x, y, 44)));
CAGE.paint('hasp', (x, y) => (edge(x, y) === 0 || (x === 4 && y % 4 === 1) ? '#2a2f35' : mot('steel', x, y, 45)));
CAGE.paint('chain', (x, y) => (y % 3 === 1 ? '#141619' : (x % 2 ? '#5f656e' : '#454b53')));
CAGE.paint('chute', (x, y) => {
  if (y >= 10) return x === 5 || x === 10 ? mot('iron', x, y, 46) : '#141619';
  return x % 3 === 0 ? '#2a2f35' : mot('steel', x, y, 47);
});
CAGE.paint('vent_back', (x, y) => '#141619');
CAGE.paint('gauge', (x, y) => (x === 8 && y % 2 === 0 ? '#3a3a36' : (hash(x, y, 28) > 0.85 ? '#2c3236' : '#2a2f33')));
CAGE.paint('water', (x, y) => (hash(x, y, 48) > 0.8 ? '#336a778c' : '#2f55638c'));
CAGE.paint('water_top', (x, y) => ((x + y * 3) % 7 === 0 ? '#4a7f8ca0' : '#336a7790'));
CAGE.paint('water_col', (x, y) => mot('drowned', x, y, 49));
CAGE.paint('kelp', (x, y) => (x >= 5 && x <= 10 && (x + (y >> 1)) % 3 !== 0 ? mot('kelp', x, y, 75) : null));
CAGE.paint('stone_air', (x, y) => mot('andesite', x, y, 76));
CAGE.paint('hatch', (x, y) => (y === 0 ? '#5f656e' : (x % 4 === 0 ? '#2a2f35' : mot('iron', x, y, 77))));
CAGE.paint('dark', painters.dark());
CAGE.paint('steel', painters.steel());
CAGE.paint('andesite', painters.andesite());
CAGE.paint('copper', painters.copper());
CAGE.paint('brass', painters.brass());
CAGE.paint('chow', (x, y) => (hash(x, y, 78) > 0.5 ? mot('chow', x, y, 79) : mot('chow', y, x, 80)));
// preview-only monster stand-ins
CAGE.paint('zombie', (x, y) => mot('zombie', x, y, 81));
CAGE.paint('drowned', (x, y) => mot('drowned', x, y, 82));
CAGE.paint('cloth', (x, y) => mot('cloth', x, y, 83));
CAGE.paint('creeper', (x, y) => (hash(x >> 1, y >> 1, 84) > 0.7 ? '#2f4a2a' : mot('creeper', x, y, 85)));
CAGE.paint('wither', (x, y) => mot('wither', x, y, 86));
CAGE.paint('eyes', (x, y) => '#141619');
BRASS.paint('keyplate', (x, y) => (x === 8 && y >= 7 && y <= 9 ? '#141619' : mot('brassMid', x, y, 87)));
BRASS.paint('lock', (x, y) => (edge(x, y) === 0 ? '#5e4c24' : (x === 7 && y >= 8 && y <= 10 ? '#141619' : mot('brassMid', x, y, 88))));
BRASS.paint('lip', (x, y) => mot('brassMid', x, y, 89));
BRASS.paint('cap', (x, y) => (hash(x, y, 90) > 0.9 ? '#b08f45' : mot('brassMid', x, y, 91)));

// ---------- the cage model ----------
// tier: 'cage' (andesite) | 'brass' | 'neth'; kind: 'dry' | 'flooded' | 'neth'
function cageModel(tier, kind) {
  const isN = kind === 'neth', fl = kind === 'flooded';
  const T = (k) => `${tier}:${k}`;
  const I = (k) => (isN ? `neth:${k}` : `cage:${k}`);                 // interior textures
  const sideTex = isN ? 'neth:side' : fl ? T('side_flooded') : T('side');
  const lip = isN ? 'neth:plate' : tier === 'brass' ? 'brass:lip' : 'cage:andesite';
  const trayTex = isN ? 'neth:tray' : 'cage:iron';
  const wall = fl ? 'cage:tiles' : I('cell');
  const rec = fl ? GAUGE : VENT;
  const out = [
    C('top', [0, 14, 0], [16, 16, 16], { north: T('front'), east: sideTex, west: sideTex, south: T('back'), up: isN ? 'neth:top' : T('grate'), down: I('ceiling') }),
    C('bottom', [0, 0, 0], [16, 2, 16], { north: T('front'), east: sideTex, west: sideTex, south: T('back'), down: T('bottom'), up: null }),
    C('wall_back', [2, 2, 14], [14, 14, 16], { south: T('back'), north: wall, east: null, west: null, up: null, down: null }),
  ];
  // glass: whole, or round the trident hatch's notch on the flooded cages
  const gl = { north: 'cage:glass', south: 'cage:glass', all: null };
  if (fl) out.push(
    C('glass_main', [2, 2, GLASS_Z], [14, 12, GLASS_Z], gl, { shade: false }),
    C('glass_tl', [2, 12, GLASS_Z], [6, 14, GLASS_Z], gl, { shade: false }),
    C('glass_tr', [10, 12, GLASS_Z], [14, 14, GLASS_Z], gl, { shade: false }));
  // dry and netherite cages are open behind their bars (user, 2026-10-27: no glass on the ones without water)
  // side walls: inner wall + 0.5 px outer skin round the side recess; the west post has the padlock pocket at the front
  for (const [side, x0, x1, sx0, sx1, outer, inner] of [['west', 0.5, 2, 0, 0.5, 'west', 'east'], ['east', 14, 15.5, 15.5, 16, 'east', 'west']]) {
    const lockSide = side === 'west' && !isN;
    const zf = lockSide ? LOCK.depth : 0;
    out.push(C(`wall_${side}`, [x0, 2, zf], [x1, 14, 16], { north: lockSide ? 'cage:hasp' : T('front'), south: T('back'), [outer]: fl ? 'cage:gauge' : 'cage:vent_back', [inner]: wall, up: null, down: null }));
    const skin = { north: null, south: null, up: null, down: null, east: null, west: null, [outer]: sideTex };
    out.push(C(`skin_${side}_front`, [sx0, 2, zf], [sx1, 14, rec.z0], { ...skin, north: lockSide ? 'cage:hasp' : T('front'), south: 'cage:dark' }));
    out.push(C(`skin_${side}_back`, [sx0, 2, rec.z1], [sx1, 14, 16], { ...skin, south: T('back'), north: 'cage:dark' }));
    out.push(C(`skin_${side}_top`, [sx0, rec.y1, rec.z0], [sx1, 14, rec.z1], { ...skin, down: 'cage:dark' }));
    out.push(C(`skin_${side}_foot`, [sx0, 2, rec.z0], [sx1, rec.y0, rec.z1], { ...skin, up: 'cage:dark' }));
    const ox = side === 'west' ? [0.1, 0.5] : [15.5, 15.9];
    if (fl) out.push(C(`tap_${side}`, side === 'west' ? [0, GAUGE.y0, 7.55] : [15.6, GAUGE.y0, 7.55], side === 'west' ? [0.4, GAUGE.y0 + 0.7, 8.45] : [16, GAUGE.y0 + 0.7, 8.45], { all: 'cage:brass' }));
    else if (isN) {
      // power terminal at the face centre: a copper stud in an insulated collar
      const cx = side === 'west' ? [0, 0.5] : [15.5, 16];
      out.push(C(`collar_${side}`, [cx[0] + (side === 'west' ? 0.25 : 0), 6.5, 6.5], [cx[1] - (side === 'east' ? 0.25 : 0), 9.5, 9.5], { all: 'neth:collar', [inner]: null }));
      out.push(C(`terminal_${side}`, [cx[0], 7.25, 7.25], [cx[1], 8.75, 8.75], { all: 'neth:terminal', [inner]: null }));
    } else {
      // three bars across the vent
      for (const [i, z] of [6.75, 7.75, 8.75].entries())
        out.push(C(`vent_bar_${side}_${i}`, [ox[0], VENT.y0, z], [ox[1] - 0.05, VENT.y1, z + 0.5], { all: T('front') === 'brass:front' ? 'cage:iron' : 'cage:iron', [inner]: null, up: null, down: null }));
    }
    if (lockSide) {
      // the post round the padlock pocket, and the lock hanging on its hasp
      out.push(C('post_upper', [0, LOCK.y1, 0], [2, 14, LOCK.depth], { north: T('front'), west: sideTex, east: wall, down: 'cage:dark', up: null, south: null }));
      out.push(C('post_lower', [0, 2, 0], [2, LOCK.y0, LOCK.depth], { north: T('front'), west: sideTex, east: wall, up: 'cage:dark', down: null, south: null }));
      out.push(C('lock_body', [0.45, 5.8, 0.1], [1.55, 7.3, LOCK.depth], { all: T('lock'), south: null }));
      out.push(C('lock_shackle_l', [0.6, 7.3, 0.25], [0.8, 8.2, 0.45], { all: 'cage:iron' }));
      out.push(C('lock_shackle_r', [1.2, 7.3, 0.25], [1.4, 8.2, 0.45], { all: 'cage:iron' }));
      out.push(C('lock_shackle_t', [0.8, 8.0, 0.25], [1.2, 8.2, 0.45], { all: 'cage:iron', east: null, west: null }));
    }
  }
  // floor (its top is the monsters' floor)
  out.push(C('floor', [2, 2, GLASS_Z + 0.25], [14, FLOOR_Y, 14], { up: fl ? 'cage:floor_wet' : I('floor'), north: I('floor'), all: null }));
  // bars and crossbar between the glass and the tray
  const barTex = isN ? 'neth:bars' : 'cage:bars';
  for (const [i, x] of BARS_X.entries())
    out.push(C(`bar_${i}`, [x, FLOOR_Y, BAR_Z[0]], [x + 1, 14, BAR_Z[1]], { all: barTex, up: null, down: null }));
  if (!isN) {
    const xs = [2, ...BARS_X.flatMap((x) => [x, x + 1]), 14];
    for (let i = 0; i < xs.length; i += 2) {
      const ends = i === 0 || i === xs.length - 2, capTex = tier === 'brass' && ends ? 'brass:cap' : 'cage:iron';
      out.push(C(`crossbar_${i / 2}`, [xs[i], CROSS_Y[0], BAR_Z[0]], [xs[i + 1], CROSS_Y[1], BAR_Z[1]], { all: capTex, east: null, west: null }));
    }
  }
  // the tray of drops in front
  out.push(
    C('tray_front', [3, FLOOR_Y, 1.4], [13, 3.3, 1.9], { all: trayTex, up: lip, down: null }),
    C('tray_back', [3, FLOOR_Y, 3.3], [13, 3.3, 3.8], { all: trayTex, up: lip, down: null }),
    C('tray_west', [3, FLOOR_Y, 1.9], [3.5, 3.3, 3.3], { all: trayTex, up: lip, down: null, north: null, south: null }),
    C('tray_east', [12.5, FLOOR_Y, 1.9], [13, 3.3, 3.3], { all: trayTex, up: lip, down: null, north: null, south: null }),
    C('tray_bed', [3.5, FLOOR_Y, 1.9], [12.5, 2.75, 3.3], { up: trayTex, all: null }),
  );
  if (!isN) {
    // chow chute down the back wall (swallows the stub's tip) and the dented steel dog bowl under it
    out.push(C('chute', [5.5, 5, 12.4], [10.5, 10.5, 14], { all: 'cage:steel', north: 'cage:chute', down: 'cage:dark', south: null }));
    const rim = tier === 'brass' ? 'brass:lip' : 'cage:bowl_rim', B = 'cage:bowl';
    out.push(
      C('bowl_bed', [5.5, FLOOR_Y, 9.5], [10.5, 2.75, 13.5], { up: B, all: null }),
      C('bowl_front', [6, FLOOR_Y, 9], [10, 4, 9.5], { all: B, up: rim, down: null }),
      C('bowl_back', [6, FLOOR_Y, 13.5], [10, 4, 14], { all: B, up: rim, down: null, south: null }),
      C('bowl_west', [5, FLOOR_Y, 10], [5.5, 4, 13], { all: B, up: rim, down: null }),
      C('bowl_east', [10.5, FLOOR_Y, 10], [11, 4, 13], { all: B, up: rim, down: null }),
      C('bowl_c1', [5.5, FLOOR_Y, 9.5], [6, 3.6, 10], { all: B, up: rim, down: null }),          // the dent: one corner lower
      C('bowl_c2', [10, FLOOR_Y, 9.5], [10.5, 4, 10], { all: B, up: rim, down: null }),
      C('bowl_c3', [5.5, FLOOR_Y, 13], [6, 4, 13.5], { all: B, up: rim, down: null }),
      C('bowl_c4', [10, FLOOR_Y, 13], [10.5, 4, 13.5], { all: B, up: rim, down: null }),
    );
    // two short chains hanging in the back corners
    for (const [i, x] of [2.4, 13.1].entries())
      out.push(C(`chain_${i}`, [x, 10.8, 12.8], [x + 0.5, 14, 13.3], { all: 'cage:chain', up: null }));
  } else {
    // sealed hopper port round the stub; field coils in the window's inner corners; the beacon set into the roof
    out.push(C('port', [4.5, 4.5, 13.5], [11.5, 11.5, 14], { north: 'neth:port', all: 'neth:plate', south: null }, { uv: { north: [0, 0, 16, 16] } }));
    for (const [i, [x, y0, y1]] of [[2.05, FLOOR_Y, 5.5], [13.05, FLOOR_Y, 5.5], [2.05, 11, 13.95], [13.05, 11, 13.95]].entries()) {
      out.push(C(`coil_${i}`, [x, y0, 1.4], [x + 0.9, y1, 2.5], { all: 'neth:coil', up: 'neth:coil_cap', down: 'neth:coil_cap' }));
    }
    // beacon: lens in a well in the roof, under a guard; the slab is split round the well
    out.push(
      C('roof_n', [0, 14, 0], [16, 16, 5.5], { up: 'neth:top', north: T('front'), east: sideTex, west: sideTex, down: null, south: null }),
      C('roof_s', [0, 14, 10.5], [16, 16, 16], { up: 'neth:top', south: T('back'), east: sideTex, west: sideTex, down: null, north: null }),
      C('roof_w', [0, 14, 5.5], [5.5, 16, 10.5], { up: 'neth:top', west: sideTex, down: null, north: null, south: null }),
      C('roof_e', [10.5, 14, 5.5], [16, 16, 10.5], { up: 'neth:top', east: sideTex, down: null, north: null, south: null }),
      C('well_floor', [5.5, 14, 5.5], [10.5, 14.5, 10.5], { up: 'neth:plate', all: null }),
      C('well_n', [5.5, 14.5, 5.5], [10.5, 16, 5.5], { south: 'neth:plate', all: null }),
      C('well_s', [5.5, 14.5, 10.5], [10.5, 16, 10.5], { north: 'neth:plate', all: null }),
      C('well_w', [5.5, 14.5, 5.5], [5.5, 16, 10.5], { east: 'neth:plate', all: null }),
      C('well_e', [10.5, 14.5, 5.5], [10.5, 16, 10.5], { west: 'neth:plate', all: null }),
      C('guard_a', [5.5, 15.6, 7.6], [10.5, 15.9, 7.9], { all: 'neth:guard' }),
      C('guard_b', [5.5, 15.6, 8.6], [10.5, 15.9, 8.9], { all: 'neth:guard' }),
    );
  }
  if (fl) {
    // kelp in the back corners (two crossed planes each) and the air stone
    for (const [i, x] of [3.2, 12.3].entries())
      for (const yaw of [45, -45])
        out.push(C(`kelp_${i}_${yaw}`, [x - 1, FLOOR_Y, 12.8], [x + 1, 9.5, 12.8], { north: 'cage:kelp', south: 'cage:kelp', all: null }, { rot: ['y', yaw, [x, FLOOR_Y, 12.8]], shade: false }));
    out.push(C('air_stone', [3, FLOOR_Y, 10.4], [4.2, 3.0, 11.4], { all: 'cage:stone_air', down: null }));
  }
  // the top face of the plain slab is replaced by the split roof on the netherite cage
  if (isN) out.shift();
  if (isN) out.unshift(C('ceiling', [2, 13.99, 0.5], [14, 14, 14], { down: 'neth:ceiling', all: null }));
  return out;
}

// ---------- part models and preview props ----------
const hatch = () => [C('hatch', [6, 12, GLASS_Z - 0.15], [10, 14, GLASS_Z + 0.15], { all: 'cage:hatch' })];
const beacon = () => [
  C('lens', [6.5, 14.5, 6.5], [9.5, 15.5, 9.5], { all: 'neth:lens', down: null }, { uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16], east: [0, 0, 16, 16], west: [0, 0, 16, 16], up: [0, 0, 16, 16] } }),
];
const beaconLit = () => [C('lens_lit', [6.45, 14.5, 6.45], [9.55, 15.55, 9.55], { all: 'neth:lens_lit', down: null }, { uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16], east: [0, 0, 16, 16], west: [0, 0, 16, 16], up: [0, 0, 16, 16] } })];
const reflector = () => [C('reflector', [7, 14.6, 7.8], [9, 15.2, 8.2], { all: 'neth:reflector' })];
const tips = (lit) => [[2.05, 5.5], [13.05, 5.5], [2.05, 10.6], [13.05, 10.6]].map(([x, y], i) =>
  C(`tip_${lit ? 'lit' : 'dark'}_${i}`, [x + 0.15, y, 1.65], [x + 0.75, y + 0.4, 2.25], { all: lit ? 'neth:tip_lit' : 'neth:tip_dark' }));
const SH = { uv: Object.fromEntries(['north', 'east', 'south', 'west', 'up'].map((d) => [d, [0, 0, 16, 16]])), shade: false };
const shield = () => [C('shield', [-1, -1, -1], [17, 17, 17], { all: 'neth:shield', down: null }, SH)];
const water = (top) => [C('water', [2.05, FLOOR_Y, GLASS_Z + 0.25], [13.95, top, 13.75], { north: 'cage:water', east: 'cage:water', west: 'cage:water', south: null, up: 'cage:water_top', down: null }, { shade: false })];
const waterCols = () => [
  C('wcol_west', [0.25, GAUGE.y0, GAUGE.z0 + 0.05], [0.25, GAUGE.y1, GAUGE.z1 - 0.05], { west: 'cage:water_col', all: null }, { shade: false }),
  C('wcol_east', [15.75, GAUGE.y0, GAUGE.z0 + 0.05], [15.75, GAUGE.y1, GAUGE.z1 - 0.05], { east: 'cage:water_col', all: null }, { shade: false }),
];
const chow = (tex) => CHOW.map(([x, yaw], i) => C(`chow_${i}`, [x - 1.6, 3.1, 11.6 - 1.6], [x + 1.6, 3.1, 11.6 + 1.6], { up: tex, down: tex, all: null },
  { uv: { up: [0, 0, 16, 16], down: [0, 0, 16, 16] }, rot: ['y', yaw, [x, 3.1, 11.6]], shade: false }));
// stand-ins at the renderer's scale (6.4 px tall), feet at the origin, facing north (-z)
const humanoid = (skin, cloth) => [
  C('leg_l', [-0.8, FLOOR_Y, -0.4], [0, FLOOR_Y + 2.4, 0.4], { all: cloth }),
  C('leg_r', [0, FLOOR_Y, -0.4], [0.8, FLOOR_Y + 2.4, 0.4], { all: cloth }),
  C('body', [-0.8, FLOOR_Y + 2.4, -0.4], [0.8, FLOOR_Y + 4.8, 0.4], { all: cloth }),
  C('arm_l', [-1.6, FLOOR_Y + 4.0, -2.0], [-0.8, FLOOR_Y + 4.8, 0.4], { all: skin }),
  C('arm_r', [0.8, FLOOR_Y + 4.0, -2.0], [1.6, FLOOR_Y + 4.8, 0.4], { all: skin }),
  C('head', [-0.8, FLOOR_Y + 4.8, -0.8], [0.8, FLOOR_Y + 6.4, 0.8], { all: skin }),
  C('eyes', [-0.55, FLOOR_Y + 5.5, -0.85], [0.55, FLOOR_Y + 5.8, -0.8], { north: 'cage:eyes', all: null }),
];
const creeper = () => [
  C('feet_f', [-0.8, FLOOR_Y, -1.2], [0.8, FLOOR_Y + 1.2, -0.4], { all: 'cage:creeper' }),
  C('feet_b', [-0.8, FLOOR_Y, 0.4], [0.8, FLOOR_Y + 1.2, 1.2], { all: 'cage:creeper' }),
  C('body', [-0.8, FLOOR_Y + 1.2, -0.4], [0.8, FLOOR_Y + 4.8, 0.4], { all: 'cage:creeper' }),
  C('head', [-0.8, FLOOR_Y + 4.8, -0.8], [0.8, FLOOR_Y + 6.4, 0.8], { all: 'cage:creeper' }),
  C('face', [-0.5, FLOOR_Y + 5.0, -0.85], [0.5, FLOOR_Y + 6.0, -0.8], { north: 'cage:eyes', all: null }),
];
const wither = () => [
  C('spine', [-0.3, FLOOR_Y + 1.0, -0.3], [0.3, FLOOR_Y + 4.6, 0.3], { all: 'cage:wither' }),
  C('ribs', [-1.0, FLOOR_Y + 2.6, -0.4], [1.0, FLOOR_Y + 3.9, 0.4], { all: 'cage:wither' }),
  C('shoulders', [-2.0, FLOOR_Y + 4.6, -0.3], [2.0, FLOOR_Y + 5.2, 0.3], { all: 'cage:wither' }),
  C('head_c', [-0.65, FLOOR_Y + 5.2, -0.65], [0.65, FLOOR_Y + 6.4, 0.65], { all: 'cage:wither' }),
  C('head_l', [-2.0, FLOOR_Y + 5.0, -0.45], [-1.1, FLOOR_Y + 5.9, 0.45], { all: 'cage:wither' }),
  C('head_r', [1.1, FLOOR_Y + 5.0, -0.45], [2.0, FLOOR_Y + 5.9, 0.45], { all: 'cage:wither' }),
  C('eyes', [-0.45, FLOOR_Y + 5.75, -0.7], [0.45, FLOOR_Y + 5.95, -0.65], { north: 'neth:tip_lit', all: null }),
];

// ---------- jars: 3D item models, started from the Capture Bottle (models/item/capture_bottle.json) ----------
// The bottle: glass x4-12 y0-9, neck x5-11 y9-9.75, lid x4.5-11.5 y9.75-11.5. A jar is squatter and wider (x3.5-12.5, y0-8),
// with two metal bands, a clamp lid and a wire bail, so it never reads as a bottle.
const BOTTLE_GLASS = 'comedymonkeysaddons:item/capture_bottle_glass';
const FULL = Object.fromEntries(['north', 'east', 'south', 'west', 'up', 'down'].map((d) => [d, [0, 0, 16, 16]]));
function jar(metal, rim) {
  const M = `jar:${metal}`, L = `jar:${metal}_lid`;
  const band = (n, y0) => C(`band_${n}`, [3.25, y0, 3.25], [12.75, y0 + 0.75, 12.75], { all: M, up: null, down: null });
  const out = [
    C('glass', [3.5, 0, 3.5], [12.5, 8, 12.5], { all: BOTTLE_GLASS }, { uv: FULL }),
    band(0, 1.5), band(1, 5.25),
    C('neck', [5, 8, 5], [11, 8.75, 11], { all: L, down: null }),
    C('lid', [4.25, 8.75, 4.25], [11.75, 10.25, 11.75], { all: L }),
    C('lid_knob', [7, 10.25, 7], [9, 10.75, 9], { all: L, down: null }),
    // the wire bail: up both sides from the upper band, over the lid
    C('bail_w', [3.0, 6.0, 7.75], [3.25, 10.6, 8.25], { all: M }),
    C('bail_e', [12.75, 6.0, 7.75], [13.0, 10.6, 8.25], { all: M }),
    C('bail_top', [3.25, 10.6, 7.75], [12.75, 10.85, 8.25], { all: M, east: null, west: null }),
    C('clamp', [12.75, 8.6, 7.4], [13.25, 9.6, 8.6], { all: M }),
  ];
  if (rim) out.push(C('rim', [3.4, 7.4, 3.4], [12.6, 8, 12.6], { all: rim, up: null, down: null }));
  return out;
}
const JARS = textureSet(); JARS.file = '';
JARS.paint('iron', (x, y) => (y % 4 === 0 ? '#5f656e' : mot('iron', x, y, 130)));
JARS.paint('iron_lid', (x, y) => (edge(x, y) === 0 ? '#3c424a' : edge(x, y) === 1 ? '#5f656e' : '#4a5059'));
JARS.paint('netherite', (x, y) => (y % 4 === 0 ? '#5d5052' : mot('neth', x, y, 132)));
JARS.paint('netherite_lid', (x, y) => (edge(x, y) === 0 ? '#221c1d' : edge(x, y) === 1 ? '#5d5052' : '#3a3233'));
JARS.paint('blue_rim', (x, y) => (hash(x, y, 134) > 0.6 ? '#4a96e0' : '#3d82d8'));

// ---------- items (flat 16x16 sprites, shown as standing cards) ----------
function sprite(set, key, rows, pal) {
  set.paint(key, (x, y) => { const ch = rows[y][x]; return ch === '.' ? null : pal[ch]; });
}
const JAR = [
  '................',
  '.....wwwwww.....',
  '....w......w....',
  '....kKKKKKKk....',
  '...kkkkkkkkkk...',
  '...oRRRRRRRRo...',
  '...og.h.....o...',
  '...og.h.....o...',
  '...bBBBBBBBBb...',
  '...og.......o...',
  '...og.......o...',
  '...og.......o...',
  '...bBBBBBBBBb...',
  '...og.......o...',
  '....oooooooo....',
  '................',
];
const ITEMS = textureSet(); ITEMS.file = '';
sprite(ITEMS, 'iron_jar', JAR, { w: '#7c7f79', k: '#3c424a', K: '#5f656e', o: '#6f7f86', R: '#83939a', g: '#6f7f8660', h: '#a3b0b5', b: '#3c424a', B: '#5f656e', '.': null });
sprite(ITEMS, 'netherite_jar', JAR, { w: '#5d5052', k: '#31292a', K: '#4d4345', o: '#6f7f86', R: '#4a86c4', g: '#6f7f8660', h: '#a3b0b5', b: '#31292a', B: '#4d4345', '.': null });
sprite(ITEMS, 'monster_chow', [
  '................',
  '.........ll.....',
  '.....bb..lLl....',
  '....bBBb.lLLl...',
  '.....bb.cclLl...',
  '...tcCcCcCcCt...',
  '...tCcmcCmcCt...',
  '...TttttttttT...',
  '...tTTTTTTTTt...',
  '...TttttttttT...',
  '...tTTTTTTTTt...',
  '...TttttttttT...',
  '...tTTTTTTTTt...',
  '...TttttttttT...',
  '....dddddddd....',
  '................',
], { l: '#5f656e', L: '#7c7f79', b: '#8a8478', B: '#a39d8b', c: '#5a4630', C: '#6e5a3a', m: '#4f5a34', t: '#5f656e', T: '#6f757d', d: '#3c424a' });
sprite(ITEMS, 'fish_food', [
  '................',
  '..pp............',
  '..pPp...g.r.....',
  '...pPp.ogy.g....',
  '....pPpyrgoy....',
  '...eeeeeeeeee...',
  '...aAAAAAAAAa...',
  '...aAAAAAAAAa...',
  '...gGGGGGGGGg...',
  '...gGfGGGfGGg...',
  '...aAAAAAAAAa...',
  '...aAAAAAAAAa...',
  '...aAAAAAAAAa...',
  '...eeeeeeeeee...',
  '................',
  '................',
], { p: '#6f757d', P: '#8a8c86', g: '#4f7340', G: '#3e5e33', f: '#8a7a4a', r: '#9c5f3a', y: '#8a7a4a', o: '#6e5f3a', e: '#5f656e', a: '#8a8478', A: '#a39d8b' });

// ---------- assemble ----------
const BDSET = textureSet(); BDSET.file = 'preview_';
const { cube, group, clip, write } = preview('monster-cage', { 'cage:': CAGE, 'brass:': BRASS, 'neth:': NETH, 'item:': ITEMS, 'jar:': JARS, 'bd:': { get OWN() { return BDSET.OWN; }, get file() { return 'preview_'; } } });
const SCENES = [
  ['andesite', 'cage', 'dry', 0], ['brass', 'brass', 'dry', 24], ['flooded', 'cage', 'flooded', 48], ['brass_flooded', 'brass', 'flooded', 72], ['netherite', 'neth', 'neth', 96],
];
const TRAY = {
  andesite: ['minecraft:item/rotten_flesh', 'minecraft:item/bone', 'minecraft:item/string'],
  brass: ['minecraft:item/blaze_rod', 'minecraft:item/ender_pearl', 'minecraft:item/gunpowder'],
  flooded: ['minecraft:item/rotten_flesh', 'minecraft:item/copper_ingot', 'minecraft:item/trident'],
  brass_flooded: ['minecraft:item/prismarine_shard', 'minecraft:item/prismarine_crystals', 'minecraft:item/cod'],
  netherite: ['minecraft:item/nether_star', 'minecraft:item/nether_star', 'minecraft:block/wither_rose'],
};
const WATER_TOP = 13.5;
function scene([name, tier, kind, ox]) {
  const off = [ox, 0, 0];
  const cubes = (list, shift) => list.map((c) => cube(c, off, shift));
  const a1 = [6, WANDER.z0 + 1], a2 = [10, WANDER.z1 - 0.5];
  const kids = [
    group(`${name}_shell`, [8, 0, 8], off, cubes(cageModel(tier, kind))),
    group(`${name}_shaft`, [8, 8, 8], off, cubes(shaftStub())),
    ...[5, 8, 11].map((cx, i) => group(`${name}_item${i + 1}`, [cx, 2.6, 2.2], off, cubes(trayItem(`${name}_${i + 1}`, cx, TRAY[name][i]), [0, 0.1, 0]))),
  ];
  if (kind === 'neth') {
    kids.push(
      group(`${name}_mob1`, [8, FLOOR_Y, 6.5], off, cubes(wither(), [8, 0, 6.5])),
      group(`${name}_beacon`, [8, 15, 8], off, [...cubes(beacon()), group(`${name}_reflector`, [8, 15, 8], off, cubes(reflector()))]),
      group(`${name}_beacon_lit`, [8, 15, 8], off, cubes(beaconLit())),
      group(`${name}_tips_lit`, [8, 8, 2], off, cubes(tips(true))),
      group(`${name}_tips_dark`, [8, 8, 2], off, cubes(tips(false))),
      group(`${name}_shield`, [8, 8, 8], off, cubes(shield())),
    );
  } else {
    const skin = kind === 'flooded' ? 'cage:drowned' : 'cage:zombie';
    kids.push(
      group(`${name}_chow`, [8, 3.1, 11.6], off, cubes(chow('item:monster_chow'))),
      group(`${name}_mob1`, [a1[0], FLOOR_Y, a1[1]], off, cubes(humanoid(skin, 'cage:cloth'), [a1[0], 0, a1[1]])),
      group(`${name}_mob2`, [a2[0], FLOOR_Y, a2[1]], off, cubes(kind === 'flooded' ? humanoid(skin, 'cage:cloth') : creeper(), [a2[0], 0, a2[1]])),
    );
  }
  if (kind === 'flooded') {
    kids.push(
      group(`${name}_hatch`, [8, 14, GLASS_Z], off, cubes(hatch())),
      group(`${name}_water`, [8, FLOOR_Y, 8], off, cubes(water(WATER_TOP))),
      group(`${name}_wcol`, [8, GAUGE.y0, 8], off, cubes(waterCols())),
    );
  }
  return group(name, [8, 0, 8], off, kids, { open: true });
}
const outliner = SCENES.map(scene);
// item cards in a row in front of the cages
const jars = [['iron_jar', 'iron', null], ['netherite_jar', 'netherite', 'jar:blue_rim']].map(([k, metal, rim], i) => {
  const off = [-34 - i * 20, 0, 0];
  return group(`model_${k}`, [8, 0, 8], off, jar(metal, rim).map((c) => cube(c, off)));
});
const cards = ['monster_chow', 'fish_food'].map((k, i) => {
  const off = [-74 - i * 20, 0, 0];
  return group(`card_${k}`, [8, 0, 8], off, [cube(C(`card_${k}`, [0, 0, 8], [16, 16, 8], { north: `item:${k}`, south: `item:${k}`, all: null }, { uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] }, shade: false }), off)]);
});
const BD = BDSET;
BD.paint('dark', (x, y) => (hash(x, y, 99) > 0.9 ? '#26282c' : '#222428'));
outliner.push(group('backdrop', [8, 0, 8], [0, 0, 0], [
  cube(C('ground', [-130, -0.5, -40], [170, -0.01, 30], { up: 'bd:dark', all: null }, { uv: { up: [0, 0, 16, 16] } }), [0, 0, 0]),
  cube(C('wall', [-130, -0.5, 30], [170, 40, 30], { north: 'bd:dark', all: null }, { uv: { north: [0, 0, 16, 16] } }), [0, 0, 0]),
]));
outliner.push(group('items', [8, 0, 8], [-34, 0, 0], [...jars, ...cards], { open: false }));

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
const sfx = soundsFor(path.resolve(__dirname, '../sound/monster-cage/monster-cage.cues.json'), path.resolve(__dirname, '../sound/monster-cage/real'));

// ---------- clips ----------
const LIFE_S = 8;
const walk = (s, pts) => {
  let i = 0;
  while (i < pts.length - 2 && s > pts[i + 1][0]) i++;
  const [t0, x0, z0] = pts[i], [t1, x1, z1] = pts[i + 1];
  const f = Math.max(0, Math.min(1, (s - t0) / (t1 - t0)));
  const yaw = Math.atan2(-(x1 - x0), -(z1 - z0)) * 180 / Math.PI;
  return { x: lerp(x0, x1, f), z: lerp(z0, z1, f), yaw: Math.abs(x1 - x0) + Math.abs(z1 - z0) < 1e-6 ? null : yaw };
};
function mobTracks(bone, start, pts) {
  let lastYaw = 0;
  const yawAt = (s) => { const w = walk(s, pts); if (w.yaw !== null) lastYaw = w.yaw; return lastYaw; };
  return { [bone]: { position: (s) => { const w = walk(s, pts); return [w.x - start[0], 0, w.z - start[1]]; }, rotation: (s) => [0, yawAt(s), 0] } };
}
const W = WANDER;
const p1 = [[0, 6, W.z0 + 1], [2.5, 11.5, W.z0 + 1], [3.2, 11.5, W.z0 + 1], [5.5, 11.5, W.z1 - 0.3], [6.2, 11.5, W.z1 - 0.3], [8, 6, W.z0 + 1]];
const p2 = [[0, 10, W.z1 - 0.5], [1.8, 6.5, W.z1 - 0.2], [3, 6.5, W.z1 - 0.2], [4.8, 4.5, W.z0 + 0.5], [6, 4.5, W.z0 + 0.5], [8, 10, W.z1 - 0.5]];
const lifeTracks = {}, lifeSteps = {};
for (const [name, , kind] of SCENES) {
  if (kind === 'neth') {
    // the Wither drifts side to side and turns to glare at the window
    lifeTracks[`${name}_mob1`] = { position: (s) => [Math.sin((s / LIFE_S) * Math.PI * 2) * 3, Math.sin(s * 2.4) * 0.3 + 0.4, 0], rotation: (s) => [0, Math.sin((s / LIFE_S) * Math.PI * 4) * 25, 0] };
    lifeSteps[`${name}_tips_dark`] = { scale: [[0, HIDE]] };
    lifeSteps[`${name}_beacon_lit`] = { scale: [[0, HIDE]] };
  } else {
    Object.assign(lifeTracks, mobTracks(`${name}_mob1`, [6, W.z0 + 1], p1), mobTracks(`${name}_mob2`, [10, W.z1 - 0.5], p2));
  }
  Object.assign(lifeSteps, Object.fromEntries([1, 2, 3].map((i) => [`${name}_item${i}`, { scale: [[0, HIDE], [i * 2, SHOW]] }])));
}
clip('life', LIFE_S, 'loop', lifeTracks, lifeSteps, sfx('life'));
// water_levels: the flooded cages at empty, half and full (volume and both side glasses)
const levels = {};
for (const name of ['flooded', 'brass_flooded']) {
  const f = (v) => [1, Math.max(0.001, v), 1];
  levels[`${name}_water`] = { scale: [[0, f(0)], [1, f(0.5)], [2, f(1)]] };
  levels[`${name}_wcol`] = { scale: [[0, f(0)], [1, f(0.5)], [2, f(1)]] };
}
clip('water_levels', 3, 'loop', {}, levels);
// trident: the hatch flaps open and shut (it hinges on its top edge, swinging out)
const flap = (s) => (s < 0.3 ? 0 : s < 0.5 ? -70 * smooth01((s - 0.3) / 0.2) : s < 0.8 ? -70 : s < 1.2 ? -70 * (1 - smooth01((s - 0.8) / 0.4)) + 6 * Math.sin((s - 0.8) * 30) * Math.max(0, 1.2 - s) : 0);
function smooth01(f) { f = Math.max(0, Math.min(1, f)); return f * f * (3 - 2 * f); }
clip('trident_hatch', 2, 'loop', { flooded_hatch: { rotation: (s) => [flap(s), 0, 0] }, brass_flooded_hatch: { rotation: (s) => [flap(s), 0, 0] } }, {}, sfx('trident_hatch'));
// containment: shield up (2 s), short of FE (flicker + beacon spin, 3 s), field down (2 s)
const flick = [2, 2.15, 2.3, 2.7, 2.8, 3.1, 3.35, 3.4, 3.9, 4.0, 4.25, 4.6, 4.7];
const shieldSteps = [[0, SHOW], ...flick.map((t, i) => [t, i % 2 ? SHOW : HIDE]), [5, HIDE]];
clip('containment', 7, 'loop', {
  netherite_reflector: { rotation: (s) => [0, s < 2 ? 0 : (s - 2) * 540, 0] },
}, {
  netherite_shield: { scale: shieldSteps },
  netherite_tips_lit: { scale: shieldSteps },
  netherite_tips_dark: { scale: shieldSteps.map(([t, v]) => [t, v === SHOW ? HIDE : SHOW]) },
  netherite_beacon_lit: { scale: [[0, HIDE], [2, SHOW], [2.4, HIDE], [2.8, SHOW], [3.2, HIDE], [3.6, SHOW], [4, HIDE], [4.4, SHOW], [4.8, HIDE], [5.2, SHOW], [5.6, HIDE], [6, SHOW], [6.4, HIDE]] },
}, sfx('containment'));
// clearance: monsters at the wander corners, facing the bars and the bowl
const clear = {};
for (const [name, , kind] of SCENES) {
  if (kind === 'neth') { clear[`${name}_mob1`] = { position: [[0, [-3, 0.4, -2]], [1.5, [3, 0.4, 2.3]]] }; continue; }
  clear[`${name}_mob1`] = { position: [[0, [W.x1 - 6, 0, W.z0 - (W.z0 + 1)]], [1.5, [W.x0 - 6, 0, W.z1 - (W.z0 + 1)]]], rotation: [[0, [0, 0, 0]], [1.5, [0, 180, 0]]] };
  clear[`${name}_mob2`] = { position: [[0, [W.x0 - 10, 0, W.z1 - (W.z1 - 0.5)]], [1.5, [W.x1 - 10, 0, W.z0 - (W.z1 - 0.5)]]], rotation: [[0, [0, 90, 0]], [1.5, [0, -90, 0]]] };
}
clip('clearance', 3, 'loop', {}, clear);

function writePreview() {
  write(OUT, outliner);
  const fs = require('fs'), bb = JSON.parse(fs.readFileSync(OUT, 'utf8'));
  for (const e of bb.elements) if (e.name === 'shield') e.render_order = 'in_front';
  for (const t of bb.textures) if (t.name.includes('shield')) { t.render_mode = 'additive'; t.height = 16 * SHIELD_FRAMES; t.frame_time = 2; }
  fs.writeFileSync(OUT, JSON.stringify(bb));
}
if (require.main === module) writePreview();
module.exports = { writePreview, cageModel, CAGE, BRASS, NETH, ITEMS, JARS, GAUGE, VENT, WANDER, GLASS_Z, hatch, beacon, beaconLit, reflector, tips, shield, water, waterCols, jar };
