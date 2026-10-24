// Upgraded Chest and Upgraded Barrel redesign generator -> design/models/upgraded-chest.bbmodel (Generic Model preview)
// To design/briefs/upgraded-chest.md and upgraded-barrel.md: dark walnut in a riveted metal frame like a Create
// casing, with fittings that grow each tier (copper frame and hasp; iron straps; gold corner caps and a lock plate;
// diamond studs; netherite plated corners and a heavy lock). Every texture is painted as a straight view of its side,
// so faces use the default projected UVs. Exported by upgraded-chest.export.js.
const path = require('path');
const { C, hash, textureSet, preview } = require('./process-box.lib.js');

const OUT = process.argv.slice(2).find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'upgraded-chest.bbmodel');
const TIERS = ['copper', 'iron', 'gold', 'diamond', 'netherite'];
const RANK = Object.fromEntries(TIERS.map((t, i) => [t, i]));
const at = (tier, min) => RANK[tier] >= RANK[min];

// ---------- palette ----------
// each metal dark to light: [shadow, base, mid, lit], plus a rivet highlight
const METAL = {
  copper: { s: ['#5e3823', '#7a4a2e', '#8a5434', '#9c5f3a'], hi: '#b87a4e' },
  iron: { s: ['#363b42', '#4a5059', '#555b63', '#5f656e'], hi: '#767b82' },
  gold: { s: ['#6e5a2a', '#8f7638', '#9c8240', '#b08f45'], hi: '#c9a75a' },
  diamond: { s: ['#24282d', '#2f343b', '#3c424a', '#4a5059'], hi: '#5f656e', gem: ['#4f8a96', '#6fb7c4'] },
  netherite: { s: ['#211c1e', '#2b2729', '#3a3438', '#443c40'], hi: '#6b6266' },
};
// a step lighter than the house walnut's darkest: in game the side shading darkens it further ("very dark in game")
const WOOD = ['#4e3626', '#634532', '#6f4d36'], SEAM = '#3b2a1f', INSET = '#33251b', DEEP = ['#45311f', '#4e3626', '#5a3f2c'];
const wood = (gx, gy, s, deep) => {
  if (gy % 3 === 2) return SEAM;
  const p = deep ? DEEP : WOOD, r = hash(gx + ((gy / 3) | 0) * 7, gy, s);
  return r < 0.18 ? p[0] : r < 0.85 ? p[1] : p[2];
};
const metal = (tier, x, y, s = 0) => { const m = METAL[tier].s, r = hash(x, y, 50 + s); return r < 0.15 ? m[0] : r < 0.85 ? m[1] : m[2]; };

// ---------- a framed panel ----------
// One flat face of a part (a lid side, a body side, a barrel side) as the viewer sees it: w x h pixels, with the
// frame on every edge except a join side (a double chest's middle). gx/gw place it along the whole face (both halves
// of a double chest), so straps and plank grain run across the pair.
function panel(tier, lx, ly, w, h, { join = null, gx = lx, gw = w, straps = false, hinges = false, kind = 'body', seed = 0 }) {
  const m = METAL[tier].s, hi = METAL[tier].hi;
  const fl = join !== 'left' && lx === 0, fr = join !== 'right' && lx === w - 1, ft = ly === 0, fb = ly === h - 1;
  const cornerOf = (cx, cy) => (fl || fr) && (ft || fb) && lx === cx && ly === cy;
  // the frame, bevelled: lit along the top and left, in shadow along the bottom and right
  if (fl || fr || ft || fb) {
    if ((fl || fr) && (ft || fb)) {
      if (tier === 'diamond') return METAL.diamond.gem[ft ? 1 : 0];
      return hi; // a rivet at each corner
    }
    if (ft || fl) return ft && hash(lx, ly, 7 + seed) > 0.9 ? m[2] : m[3];
    return m[0];
  }
  const ix = lx - (join === 'left' ? 0 : 1), iy = ly - 1, iw = w - (join ? 1 : 2), ih = h - 2;
  // netherite: plated corners, three pixels each way, inside the frame
  if (at(tier, 'netherite')) {
    const nearX = (join !== 'left' && ix < 2) || (join !== 'right' && ix > iw - 3), nearY = iy < 2 || iy > ih - 3;
    if (nearX && nearY) return (iy === 0 || ix === 0) && !(join === 'left' && ix === 0) ? m[2] : (iy === ih - 1 || ix === iw - 1) ? m[0] : m[1];
  }
  // gold and up: small L brackets in the corners
  if (at(tier, 'gold') && !at(tier, 'netherite')) {
    const l = join !== 'left' && ix === 0, r = join !== 'right' && ix === iw - 1, t = iy === 0, b = iy === ih - 1;
    if ((l || r) && (t || b)) return m[2];
  }
  // iron and up: straps running over the lid and down the body, riveted
  if (straps && at(tier, 'iron')) {
    const s = strapCols(gw);
    if (s.includes(gx)) return iy === 0 || iy === ih - 1 ? hi : gx === s[0] || gx === s[2] ? m[2] : m[1];
    if (s.some((c) => c === gx - 1)) return m[0]; // the strap's shadow
  }
  // hinges on the back: short plates across the lid line
  if (hinges && !at(tier, 'iron')) {
    const s = strapCols(gw);
    if (s.includes(gx) && ((kind === 'lid' && iy >= ih - 1) || (kind === 'body' && iy <= 1))) return kind === 'lid' ? m[3] : m[1];
  }
  // the wood, inset under the frame's top and left edge (not on a lid's front: two rows of wood can't spare one)
  if (ih > 2 && (iy === 0 || (ix === 0 && join !== 'left'))) return INSET;
  return wood(gx, ly + (kind === 'lid' ? 0 : 4), seed, at(tier, 'netherite'));
}
// straps over a face gw pixels wide: one pair per 14-15 pixel chest width, set in from each end
const strapCols = (gw) => (gw <= 16 ? [3, gw - 4] : [3, 11, gw - 12, gw - 4]);

// ---------- textures ----------
// Chest faces as the default UVs project them: a north face's u runs from x 16 down to x 0 (the viewer's left to
// right), v from y 16 at the top down; lid rows are v 2-5, body rows v 6-15.
const CH = textureSet(); CH.file = 'upgraded_chest_';
const BR = textureSet(); BR.file = 'upgraded_barrel_';
// a face texture for one block of a chest: cols u0..u1-1 hold the face; join: which edge of the texture meets the
// other half; gx0: where this block's cols start along the whole face
function chestFace(tier, { u0, u1, join = null, gx0 = 0, gw, straps = false, hinges = false, seed }) {
  return (x, y) => {
    if (x < u0 || x >= u1) return null;
    const lx = x - u0, w = u1 - u0;
    if (y >= 2 && y <= 5) return panel(tier, lx, y - 2, w, 4, { join, gx: gx0 + lx, gw, straps, hinges, kind: 'lid', seed });
    if (y >= 6) return panel(tier, lx, y - 6, w, 10, { join, gx: gx0 + lx, gw, straps, hinges, kind: 'body', seed });
    return null;
  };
}
// a top (lid) view: u along x, v along z (front at the top)
function chestTop(tier, { u0, u1, join = null, gx0 = 0, gw }) {
  return (x, y) => {
    if (x < u0 || x >= u1 || y < 1 || y > 14) return null;
    return panel(tier, x - u0, y - 1, u1 - u0, 14, { join, gx: gx0 + x - u0, gw, straps: true, kind: 'lid', seed: 3 });
  };
}
for (const t of TIERS) {
  // single: x 1..15 -> u 1..15
  CH.paint(`${t}_front`, chestFace(t, { u0: 1, u1: 15, gw: 14, straps: true, seed: 1 }));
  CH.paint(`${t}_back`, chestFace(t, { u0: 1, u1: 15, gw: 14, straps: true, hinges: true, seed: 2 }));
  CH.paint(`${t}_side`, chestFace(t, { u0: 1, u1: 15, gw: 14, seed: 4 }));
  CH.paint(`${t}_top`, chestTop(t, { u0: 1, u1: 15, gw: 14 }));
  // double: the left block (x 1..16) is on the viewer's right from the front, its join at u 0; the right block
  // (x 0..15) on the viewer's left, its join at u 16. From the back and the top it's the other way round.
  CH.paint(`${t}_front_left`, chestFace(t, { u0: 0, u1: 15, join: 'left', gx0: 15, gw: 30, straps: true, seed: 1 }));
  CH.paint(`${t}_front_right`, chestFace(t, { u0: 1, u1: 16, join: 'right', gx0: 0, gw: 30, straps: true, seed: 1 }));
  CH.paint(`${t}_back_left`, chestFace(t, { u0: 1, u1: 16, join: 'right', gx0: 0, gw: 30, straps: true, hinges: true, seed: 2 }));
  CH.paint(`${t}_back_right`, chestFace(t, { u0: 0, u1: 15, join: 'left', gx0: 15, gw: 30, straps: true, hinges: true, seed: 2 }));
  CH.paint(`${t}_top_left`, chestTop(t, { u0: 1, u1: 16, join: 'right', gx0: 0, gw: 30 }));
  CH.paint(`${t}_top_right`, chestTop(t, { u0: 0, u1: 15, join: 'left', gx0: 15, gw: 30 }));
  CH.paint(`${t}_metal`, (x, y) => metal(t, x, y));
}
// the lock, as the viewer sees it, w x h; painted where the single and both halves of a double sample it
const LOCK = { copper: [2, 3], iron: [2, 4], gold: [4, 4], diamond: [4, 4], netherite: [6, 5] };
function lockPx(t, lx, ly) {
  const [w, h] = LOCK[t], m = METAL[t].s, hi = METAL[t].hi;
  const edgeL = lx === 0, edgeR = lx === w - 1, top = ly === 0, bot = ly === h - 1;
  const mid = (w - 1) / 2;
  if (t === 'copper') return ly === 1 ? '#2a1e16' : top ? m[3] : m[1]; // a hasp with its slot
  if (t === 'iron') return ly === h - 1 ? '#2a2f35' : ly === h - 2 ? m[1] : top ? hi : m[2]; // plate over a dark ring
  if (t === 'diamond' && Math.abs(lx - mid) < 1 && (ly === 1 || ly === 2)) return METAL.diamond.gem[ly === 1 ? 1 : 0];
  if (Math.abs(lx - mid) < 1 && (ly === Math.floor(h / 2) || ly === Math.floor(h / 2) + 1) && t !== 'diamond') return '#1a1614'; // keyhole
  if ((edgeL || edgeR) && (top || bot)) return hi;
  if (top || edgeL) return m[3];
  if (bot || edgeR) return m[0];
  return m[1];
}
for (const t of TIERS) {
  const [w, h] = LOCK[t];
  const top = 16 - 11; // the lock's top edge sits at y 11
  CH.paint(`${t}_lock`, (x, y) => {
    const ly = y - top;
    if (ly < 0 || ly >= h) return metal(t, x, y, 3);
    if (x >= 8 - w / 2 && x < 8 + w / 2) return lockPx(t, x - (8 - w / 2), ly); // single, centred
    if (x < w / 2) return lockPx(t, x + w / 2, ly); // a left block's half: the lock's right-hand cols
    if (x >= 16 - w / 2) return lockPx(t, x - (16 - w / 2), ly); // a right block's half: its left-hand cols
    return metal(t, x, y, 3);
  });
}
// shared: the inside, painted with depth as vanilla does it: a steel rim, then the walls falling away into shadow
// and a near-black floor. Seen from the front with the lid up, the back wall (the bottom of the texture, z 15) catches
// the light and the front wall is in its own shadow.
const WALL = { back: ['#4e3626', '#3b2a1f', '#2e2118'], side: ['#3b2a1f', '#2e2118', '#241a14'], front: ['#241a14', '#1d1612', '#191310'] };
const FLOOR = ['#141010', '#171211', '#1a1412'];
// u0..u1-1 x v0..v1-1 is the opening; the join side (a double chest's middle) has no wall
function inside(u0, u1, v0, v1, join, seed) {
  return (x, y) => {
    if (x < u0 || x >= u1 || y < v0 || y >= v1) return null;
    const dl = join === 'left' ? 99 : x - u0, dr = join === 'right' ? 99 : u1 - 1 - x, df = y - v0, db = v1 - 1 - y;
    const d = Math.min(dl, dr, df, db);
    if (d === 0) return (df === 0 || dl === 0) ? '#3c424a' : '#2f343b'; // the rim
    if (d <= 3) {
      const wall = d === db ? WALL.back : d === df ? WALL.front : WALL.side;
      // plank seams run along the walls
      return (d === db || d === df ? x : y) % 4 === 0 && d < 3 ? '#1d1612' : wall[d - 1];
    }
    return FLOOR[(hash(x, y, seed) * 3) | 0];
  };
}
CH.paint('inner', inside(1, 15, 1, 15, null, 61));
CH.paint('inner_left', inside(1, 16, 1, 15, 'right', 62)); // x 1..16, the join at x 16
CH.paint('inner_right', inside(0, 15, 1, 15, 'left', 63)); // x 0..15, the join at x 0
// the lid's underside: planks in shade inside a steel rim
const lidInner = (u0, u1, join) => (x, y) => {
  if (x < u0 || x >= u1 || y < 1 || y > 14) return null;
  if (y === 1 || y === 14 || (join !== 'left' && x === u0) || (join !== 'right' && x === u1 - 1)) return '#2f343b';
  return wood(x, y, 65, true);
};
CH.paint('lid_inner', lidInner(1, 15, null));
CH.paint('lid_inner_left', lidInner(1, 16, 'right'));
CH.paint('lid_inner_right', lidInner(0, 15, 'left'));
CH.paint('bottom', (x, y) => (x === 0 || x === 15 || y === 0 || y === 15 ? '#2f343b' : wood(x, y, 64, true)));

// barrels: full 16-pixel faces
for (const t of TIERS) {
  const m = METAL[t].s, hi = METAL[t].hi;
  // sides: vertical staves between a hoop at the top and bottom and the frame down the corners
  BR.paint(`${t}_side`, (x, y) => {
    const p = panel(t, x, y, 16, 16, { gx: x, gw: 16, seed: 5 });
    const inside = x > 0 && x < 15 && y > 0 && y < 15;
    if (!inside) return p;
    // iron and up: a riveted middle hoop
    if (at(t, 'iron') && (y === 7 || y === 8)) return y === 7 ? (x % 5 === 2 ? hi : m[2]) : m[0];
    if (p === INSET || (p && !WOOD.includes(p) && !DEEP.includes(p) && p !== SEAM)) return p;
    // vertical staves instead of planks
    if (x % 4 === 0) return SEAM;
    const r = hash(x, y >> 2, 70);
    const pal = at(t, 'netherite') ? DEEP : WOOD;
    return r < 0.18 ? pal[0] : r < 0.85 ? pal[1] : pal[2];
  });
  // the lid: framed planks with the tier's lock in the middle
  const lid = (open) => (x, y) => {
    if (open) {
      // the tier's rim, then the same painted depth as a chest's inside
      if (x === 0 || x === 15 || y === 0 || y === 15) return x + y < 15 ? m[3] : m[0];
      return inside(1, 15, 1, 15, null, 71)(x, y);
    }
    const [w, h] = LOCK[t];
    const lx = x - (8 - w / 2), ly = y - (8 - Math.ceil(h / 2));
    if (lx >= 0 && lx < w && ly >= 0 && ly < h) return lockPx(t, lx, ly);
    return panel(t, x, y, 16, 16, { gx: x, gw: 16, seed: 6 });
  };
  BR.paint(`${t}_top`, lid(false));
  BR.paint(`${t}_top_open`, lid(true));
  BR.paint(`${t}_bottom`, (x, y) => panel(t, x, y, 16, 16, { gx: x, gw: 16, seed: 7 }));
}

// ---------- geometry ----------
// type: 'single' | 'left' | 'right'. Body and lid as today; the fittings are added per tier.
const c = (k) => 'ch:' + k;
function span(type) { return type === 'single' ? [1, 15] : type === 'left' ? [1, 16] : [0, 15]; }
function faceNames(t, type) {
  const s = type === 'single' ? '' : '_' + type;
  return { front: `${t}_front${s}`, back: type === 'single' ? `${t}_back` : `${t}_back${s}`, side: `${t}_side`, top: `${t}_top${s}`,
    inner: type === 'single' ? 'inner' : `inner${s}`, lidInner: type === 'single' ? 'lid_inner' : `lid_inner${s}` };
}
function body(t, type) {
  const [x0, x1] = span(type), f = faceNames(t, type);
  const out = [C('body', [x0, 0, 1], [x1, 10, 15], { north: c(f.front), south: c(f.back), east: type === 'left' ? null : c(f.side),
    west: type === 'right' ? null : c(f.side), up: c(f.inner), down: c('bottom') })];
  // netherite: plated corner posts on the body's outer corners, a quarter pixel proud
  if (at(t, 'netherite')) for (const [cx, cz] of corners(type)) out.push(C(`guard_${cx}_${cz}`,
    // stopping a quarter pixel under the lid line, so their tops don't z-fight the inside when the lid's up
    [cx === 'w' ? 0.75 : 13.25, 0, cz === 'n' ? 0.75 : 13.25], [cx === 'w' ? 2.75 : 15.25, 9.75, cz === 'n' ? 2.75 : 15.25], { all: c(`${t}_metal`), down: null }));
  return out;
}
// the outer corners of this block ('w'/'e' x, 'n'/'s' z); a double chest has none at its join
function corners(type) {
  const xs = type === 'single' ? ['w', 'e'] : type === 'left' ? ['w'] : ['e'];
  return xs.flatMap((x) => [[x, 'n'], [x, 's']]);
}
function lid(t, type) {
  const [x0, x1] = span(type), f = faceNames(t, type);
  const out = [C('lid', [x0, 10, 1], [x1, 14, 15], { north: c(f.front), south: c(f.back), east: type === 'left' ? null : c(f.side),
    west: type === 'right' ? null : c(f.side), up: c(f.top), down: c(f.lidInner) })];
  // the lock: centred on a single chest, split across the join of a double
  const [w, h] = LOCK[t];
  const lx0 = type === 'single' ? 8 - w / 2 : type === 'left' ? 16 - w / 2 : 0, lx1 = type === 'single' ? 8 + w / 2 : type === 'left' ? 16 : w / 2;
  out.push(C('lock', [lx0, 11 - h, 0], [lx1, 11, 1], { north: c(`${t}_lock`), all: c(`${t}_metal`), south: null,
    east: type === 'left' ? null : c(`${t}_metal`), west: type === 'right' ? null : c(`${t}_metal`) }));
  // gold and up: caps over the lid's top corners; netherite: plates down the whole lid corner
  if (at(t, 'gold')) for (const [cx, cz] of corners(type)) out.push(C(`cap_${cx}_${cz}`,
    [cx === 'w' ? 0.75 : 13, at(t, 'netherite') ? 10.25 : 11.75, cz === 'n' ? 0.75 : 13], [cx === 'w' ? 3 : 15.25, 14.25, cz === 'n' ? 3 : 15.25],
    { all: c(`${t}_metal`) }));
  return out;
}

// ---------- preview ----------
const { cube, group, clip, write } = preview('upgraded-chest', { 'ch:': CH, 'br:': BR });
const cubes = (list, off) => list.map((k) => cube(k, off));
const outliner = [], lids = [];
const chest = (name, t, type, off) => {
  const g = group(`lid_${name}`, [8, 10, 15], off, cubes(lid(t, type), off));
  lids.push(`lid_${name}`);
  return group(name, [8, 0, 8], off, [...cubes(body(t, type), off), g]);
};
TIERS.forEach((t, i) => {
  const x = i * 40;
  outliner.push(chest(`${t}_single`, t, 'single', [x, 0, 0]));
  // a double chest: the left block on the viewer's right from the front (lower x), the right block beside it
  outliner.push(group(`${t}_double`, [0, 0, 0], [0, 0, 0], [chest(`${t}_left`, t, 'left', [x - 8, 0, 28]), chest(`${t}_right`, t, 'right', [x + 8, 0, 28])]));
  const barrel = (open, z) => group(`${t}_barrel${open ? '_open' : ''}`, [8, 0, 8], [x + (open ? 10 : -10), 0, z],
    [cube(C('barrel', [0, 0, 0], [16, 16, 16], { all: `br:${t}_side`, up: `br:${t}_top${open ? '_open' : ''}`, down: `br:${t}_bottom` }), [x + (open ? 10 : -10), 0, z])]);
  outliner.push(barrel(false, 56), barrel(true, 56));
});
// the lids swing open about the top back edge, as the renderer does
const ease = (s) => { const o = 1 - Math.min(1, s / 0.5); return 1 - o * o * o; };
const open = Object.fromEntries(lids.map((l) => [l, { rotation: (s) => [90 * ease(s), 0, 0] }]));
clip('open', 1.5, 'hold', open);
clip('closed', 1, 'hold', {}, Object.fromEntries(lids.map((l) => [l, { rotation: [[0, [0, 0, 0]]] }])));
write(OUT, outliner);

module.exports = { TIERS, METAL, LOCK, CH, BR, body, lid, faceNames };
