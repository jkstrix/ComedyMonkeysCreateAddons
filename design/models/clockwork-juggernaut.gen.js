// Clockwork Juggernaut generator -> design/models/clockwork-juggernaut.bbmodel (modded_entity, box UV, 1024 px texture).
// An ancient Norse-style clockwork construct: charcoal iron, tarnished bronze, cracked stone held with staples, frost
// in the joints and faint blue runes. The model DSL, motting, UV packing and PNG writer are Helper Bot's
// (helper-bot.gen.js). Brief: design/briefs/clockwork-juggernaut.md. Built at full size: 1 px = 1/16 block.
// Front is -Z, his right is +X. Rotations right-handed (rx+ tips top to +Z, rz+ tips top to -X, ry+ turns front to -X).
// The origin is the ground under his hips.
const fs = require('fs');
const zlib = require('zlib');
const crypto = require('crypto');
const path = require('path');

const ARGS = process.argv.slice(2);
const OUT = ARGS.find((a) => !a.startsWith('--')) || path.resolve(__dirname, 'clockwork-juggernaut.bbmodel');
const TEX = 1024;
const uuid = () => crypto.randomUUID();
const HUNCH = -17; // the torso leans forward at the waist: centuries of walking

// ---------- palette ----------
const P = {
  iron: ['#343a42', '#3d444d', '#454d56'], // dark iron with blue-grey undertones
  ironHi: ['#56606b'],
  char: ['#1c1e21', '#232629', '#2a2d31'], // charcoal black
  rust: ['#3e3530', '#352d29'],
  plate: ['#4a525c', '#535c66', '#4e5761'], // weathered steel
  plateHi: ['#626c77'],
  brass: ['#5c4a2e', '#6e5a36', '#7d6a44'], // tarnished bronze
  brassDark: ['#463822'],
  patina: ['#4f6b5e', '#5b7a6c'],
  gold: ['#a8873e', '#93763a'], // ancient gold, inlays only
  stone: ['#4e504d', '#5a5c58', '#55574f'],
  stoneHi: ['#666863'],
  crack: ['#141516'],
  frost: ['#6f7c86', '#7a8791', '#68757f'],
  steam: ['#8d969c', '#979fa5'], // preview puffs
  dummy: ['#4d6170', '#566b7a'], // the stand-in player
  spark: ['#c9a75a'],
  leather: ['#3b2a1f', '#46321f'],
  rune: ['#4f8fae', '#4a87a4'], // faint frost-blue glow
  runeHi: ['#7ab8d0'],
  runeDim: ['#23404c'],
  slitOff: ['#14191c', '#181e22'],
  slot: ['#101214'],
  // preview props
  propStone: ['#5e5f5c', '#6f706c', '#7f807b'],
};
const rgb = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), 255];
function hash(x, y, s) {
  let h = (x * 374761393 + y * 668265263 + s * 2147483647) | 0;
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return ((h ^ (h >>> 16)) >>> 0) / 4294967295;
}
function mot(mat, x, y) {
  const p = P[mat];
  if (!p) throw new Error('no mat ' + mat);
  if (p.length === 1) return p[0];
  const r = hash(x, y, mat.length * 31 + mat.charCodeAt(0));
  if (p.length === 2) return r < 0.6 ? p[1] : p[0];
  return r < 0.18 ? p[0] : r < 0.84 ? p[1] : p[2];
}

// ---------- runes (Elder Futhark, 3x5; '#' lit) ----------
const RUNE_LIST = [
  ['.#.', '###', '#.#', '.#.', '.#.'], // tiwaz
  ['#.#', '##.', '#.#', '##.', '#..'], // fehu
  ['##.', '#.#', '#.#', '#.#', '#.#'], // uruz
  ['#..', '##.', '#.#', '##.', '#..'], // ansuz
  ['##.', '#.#', '##.', '#.#', '#.#'], // raido
  ['#.#', '#.#', '.#.', '.#.', '.#.'], // algiz
  ['.#.', '#.#', '.#.', '#.#', '#.#'], // othala
  ['.#.', '#.#', '.#.', '#.#', '.#.'], // ingwaz
];
// a rune band: glyphs every 4 px from (i0, j0) of a face; returns a colour or null
const runeAt = (i, j, i0, j0, seed, lit = 'rune') => {
  if (i < i0) return null;
  const k = Math.floor((i - i0) / 4), di = (i - i0) % 4, dj = j - j0;
  if (di === 3 || dj < 0 || dj > 4) return null;
  const g = RUNE_LIST[(k * 5 + seed) % RUNE_LIST.length];
  return g[dj][di] === '#' ? P[lit][(i + j) % P[lit].length] : null;
};

// ---------- model DSL ----------
const G = (name, origin, kids, o = {}) => ({ kind: 'g', name, origin, kids, rot: o.rot || [0, 0, 0], hidden: !!o.hidden });
const C = (name, from, to, mat, o = {}) => ({ kind: 'c', name, from, to, mat, ...o });
const sides = (f) => f !== 'up' && f !== 'down';
// frost settles on top faces and the top edge of the sides
const frosty = (f, j, x, y, k = 0.45, i = -1, w = 99, h = 99) => (f === 'up' && (i < 2 || j < 2 || i > w - 3 || j > h - 3) && hash(x >> 1, y >> 1, 31) < k ? mot('frost', x, y) : sides(f) && j === 0 && hash(x >> 1, y, 33) < k * 0.3 ? mot('frost', x, y) : null);
// old iron: frost on top, rust along the bottom
const aged = (f, i, j, w, h, x, y) => frosty(f, j, x, y, 0.45, i, w, h) || (!sides(f) ? null : j === 0 && hash(x, y, 2) < 0.5 ? mot('ironHi', x, y) : j >= h - 1 && hash(x >> 1, y, 3) < 0.45 ? mot('rust', x, y) : null);
// knotwork: an interlaced diamond lattice, engraved (darker) into a border band
const knot = (i, j) => ((i + j) % 4 === 0) !== ((i - j + 400) % 4 === 0);
// armour plates: an engraved knotwork border, bronze rivets at the corners, the odd dent, frost on top
const plated = (f, i, j, w, h, x, y) => {
  const fr = frosty(f, j, x, y, 0.45, i, w, h); if (fr) return fr;
  if (!sides(f)) return null;
  if (i === 0 || j === 0 || i === w - 1 || j === h - 1) return mot('plateHi', x, y);
  if ((i === 1 || i === w - 2) && (j === 1 || j === h - 2)) return P.brass[2];
  const border = i <= 3 || j <= 3 || i >= w - 4 || j >= h - 4;
  if (border && w > 13 && h > 9 && knot(i, j)) return P.char[2];
  if (hash(x, y, 7) < 0.03) return P.char[1];
  return null;
};
// cracked stone: a wandering crack with blue rune-light in it and iron staples across it
const crackAt = (i, j, w, h, seed) => {
  const x0 = Math.floor(hash(seed, 1, 5) * (w - 6)) + 3, s = (hash(seed, 2, 5) - 0.5) * 0.9, ph = hash(seed, 3, 5) * 6;
  const cx = Math.round(x0 + s * j + 1.4 * Math.sin(j * 0.7 + ph));
  if (j < 1 || j > h - 2) return null;
  if (i === cx) return 'glow';
  if (Math.abs(i - cx) === 1) return 'edge';
  if (j % 5 === 2 && Math.abs(i - cx) <= 2) return 'staple';
  return null;
};
const stony = (seed = 0, cracks = true) => (f, i, j, w, h, x, y) => {
  const fr = frosty(f, j, x, y, 0.35, i, w, h); if (fr) return fr;
  if (cracks && sides(f) && w >= 8 && h >= 8) {
    const c = crackAt(i, j, w, h, seed + f.length * 7);
    if (c === 'glow') return P.rune[(i + j) % 2];
    if (c === 'edge') return P.crack[0];
    if (c === 'staple') return mot('iron', x, y);
  }
  if (sides(f) && hash(x, y, 41) < 0.05) return P.stoneHi[0];
  return null;
};
const bandTeeth = (f, i, j, w, h, x, y) => frosty(f, j, x, y, 0.25, i, w, h) || (sides(f) && (i + j) % 3 === 0 ? mot('brassDark', x, y) : hash(x, y, 9) < 0.12 ? mot('patina', x, y) : null);
const leatherStrap = (f, i, j, w, h, x, y) => (sides(f) && (j === 0 || j === h - 1) ? P.char[0] : null);

// A stepped disc (a cylinder made of slabs that don't overlap). axis: the cylinder's axis ('x', 'y' or 'z').
// c: centre [x, y, z], r: radius, a0/a1: the extent along the axis. o.paint also gets disc-local (du, dv).
function disc(name, c, r, a0, a1, axis, mat, o = {}) {
  const q = (v) => Math.round(v * 4) / 4;
  const h1 = q(r * 0.45), h2 = q(r * 0.8), w2 = q(r * 0.88), w3 = q(r * 0.55);
  const slabs = [[-r, r, -h1, h1], [-w2, w2, h1, h2], [-w2, w2, -h2, -h1], [-w3, w3, h2, r], [-w3, w3, -r, -h2]];
  return slabs.map(([u0, u1, v0, v1], k) => {
    let from, to;
    if (axis === 'z') { from = [c[0] + u0, c[1] + v0, a0]; to = [c[0] + u1, c[1] + v1, a1]; }
    if (axis === 'x') { from = [a0, c[1] + v0, c[2] + u0]; to = [a1, c[1] + v1, c[2] + u1]; }
    if (axis === 'y') { from = [c[0] + u0, a0, c[2] + v0]; to = [c[0] + u1, a1, c[2] + v1]; }
    const paint = o.paint && ((f, i, j, w, h, x, y) => o.paint(f, i, j, w, h, x, y, u0 + i + 0.5, v1 - j - 0.5));
    return C(`${name}_${k}`, from, to, mat, { ...o, paint });
  });
}
// a cog: a disc plus a ring of teeth (a second, slightly thinner disc turned 45 degrees)
function cog(name, c, r, a0, a1, axis, o = {}) {
  const teeth = { x: [45, 0, 0], y: [0, 45, 0], z: [0, 0, 45] }[axis];
  const inset = 0.05, k = r * 0.72;
  const lo = { x: [a0 + inset, c[1] - k, c[2] - k], y: [c[0] - k, a0 + inset, c[2] - k], z: [c[0] - k, c[1] - k, a0 + inset] }[axis];
  const hi = { x: [a1 - inset, c[1] + k, c[2] + k], y: [c[0] + k, a1 - inset, c[2] + k], z: [c[0] + k, c[1] + k, a1 - inset] }[axis];
  return [...disc(name, c, r - 1, a0, a1, axis, 'brass', { paint: bandTeeth, ...o }), C(`${name}_teeth`, lo, hi, 'brassDark', { rotation: teeth, origin: c })];
}
// chain links: alternating flat and edge-on, 3 px long, along x, z or y
const linkPaint = (flat) => (f, i, j, w, h, x, y) => (flat && sides(f) && i > 0 && i < w - 1 && j > 0 && j < h - 1 ? P.char[0] : j === 0 ? mot('ironHi', x, y) : null);
function chainRun(name, from, to) {
  const d = [0, 1, 2].map((a) => to[a] - from[a]), len = Math.hypot(...d), n = Math.max(1, Math.floor(len / 2.5));
  const ax = Math.abs(d[1]) > Math.abs(d[0]) && Math.abs(d[1]) > Math.abs(d[2]) ? 1 : Math.abs(d[0]) >= Math.abs(d[2]) ? 0 : 2;
  const out = [];
  for (let k = 0; k < n; k++) {
    const p = [0, 1, 2].map((a) => from[a] + (d[a] * (k + 0.5)) / n), flat = k % 2 === 0;
    // half-extents: along the run 1.5; the flat link is 1 wide and 0.5 thick, the edge-on link the other way round
    const e = ax === 0 ? (flat ? [1.5, 1, 0.5] : [1.5, 0.5, 1]) : ax === 2 ? (flat ? [0.5, 1, 1.5] : [1, 0.5, 1.5]) : (flat ? [1, 1.5, 0.5] : [0.5, 1.5, 1]);
    out.push(C(`${name}_${k}`, [p[0] - e[0], p[1] - e[1], p[2] - e[2]], [p[0] + e[0], p[1] + e[1], p[2] + e[2]], 'iron', { paint: linkPaint(flat) }));
  }
  return out;
}
// a chain wrapped round a box (x0..x1, z0..z1) at height y, standing off it by `gap`
function chainRing(name, x0, x1, z0, z1, y, gap = 0.75) {
  const X0 = x0 - gap - 1, X1 = x1 + gap + 1, Z0 = z0 - gap - 1, Z1 = z1 + gap + 1;
  return [
    ...chainRun(`${name}_f`, [X0, y, Z0], [X1, y, Z0]), ...chainRun(`${name}_b`, [X0, y, Z1], [X1, y, Z1]),
    ...chainRun(`${name}_l`, [X0, y, Z0 + 1.5], [X0, y, Z1 - 1.5]), ...chainRun(`${name}_r`, [X1, y, Z0 + 1.5], [X1, y, Z1 - 1.5]),
  ];
}

// ---------- legs (thick pillar legs, stone below the knee; a living siege engine) ----------
const LEG_X = 13;
function leg(side) {
  const s = side > 0 ? 'right' : 'left';
  const X = (a, b) => (side > 0 ? [a, b] : [-b, -a]);
  const cx = LEG_X * side;
  const toePaint = (k) => (f, i, j, w, h, x, y) => (f === 'north' && j <= 1 ? mot('iron', x, y) : stony(k + 50 * side, false)(f, i, j, w, h, x, y));
  const stance = side > 0 ? [8, 0, 3] : [5, 0, -3];
  return G(`leg_${s}`, [cx, 28, 0], [
    ...cog(`hip_cog_${s}`, [cx, 28, 0], 6, X(22, 24)[0], X(22, 24)[1], 'x'),
    ...disc(`hip_ball_${s}`, [cx, 28, 0], 4.5, X(5, 21)[0], X(5, 21)[1], 'x', 'iron', { paint: aged }),
    C(`thigh_${s}`, [X(4, 22)[0], 19.5, -7], [X(4, 22)[1], 28, 7], 'iron', { paint: aged }),
    C(`tasset_${s}`, [X(3, 23)[0], 19, -9], [X(3, 23)[1], 25, -7], 'plate', { paint: plated }),
    G(`shin_${s}`, [cx, 16, 0], [
      ...cog(`knee_cog_${s}`, [cx, 16, 0], 5, X(23, 25)[0], X(23, 25)[1], 'x'),
      ...disc(`knee_ball_${s}`, [cx, 16, 0], 3.5, X(4, 22)[0], X(4, 22)[1], 'x', 'iron', { paint: aged }),
      C(`shin_${s}_core`, [X(3, 23)[0], 6.5, -9], [X(3, 23)[1], 13, 4], 'stone', { paint: stony(11 + side) }),
      C(`calf_${s}`, [X(3, 23)[0], 6.5, 4], [X(3, 23)[1], 10, 9], 'stone', { paint: stony(13 + side, false) }),
      C(`ankle_ring_${s}`, [X(2.5, 23.5)[0], 6.5, -9.5], [X(2.5, 23.5)[1], 8, 9.5], 'brass', { paint: bandTeeth }),
      C(`shin_wrap_${s}`, [X(2.5, 23.5)[0], 9, -9.5], [X(2.5, 23.5)[1], 10.5, 9.5], 'leather', { paint: leatherStrap }),
      G(`shin_guard_${s}`, [cx, 10, -10], [
        C(`shin_guard_${s}_plate`, [X(5, 21)[0], 8.5, -11], [X(5, 21)[1], 14.5, -9.5], 'plate', { paint: plated }),
        C(`knee_cap_${s}`, [X(7, 19)[0], 13, -12.5], [X(7, 19)[1], 19.5, -10], 'plate', { paint: plated }),
      ]),
      G(`foot_${s}`, [cx, 4, 0], [
        ...disc(`ankle_ball_${s}`, [cx, 4, 0], 3, X(6, 20)[0], X(6, 20)[1], 'x', 'iron', { paint: aged }),
        ...disc(`foot_${s}`, [cx, 0, -3], 12, 0, 3, 'y', 'stone', { paint: stony(23 + side, false) }),
        C(`toe_${s}_0`, [cx - 10.75, 0, -17], [cx - 6.75, 4, -12.5], 'stone', { paint: toePaint(0) }),
        C(`toe_${s}_1`, [cx - 3, 0, -18.5], [cx + 3, 4.5, -15], 'stone', { paint: toePaint(1) }),
        C(`toe_${s}_2`, [cx + 6.75, 0, -17], [cx + 10.75, 4, -12.5], 'stone', { paint: toePaint(2) }),
        C(`heel_${s}`, [cx - 5, 0, 9], [cx + 5, 6, 13], 'stone', { paint: stony(29 + side, false) }),
      ], { rot: [stance[0] * 0.6, 0, -stance[2]] }),
    ], { rot: [-stance[0] * 1.6, 0, 0] }),
  ], { rot: stance });
}

// ---------- arms (wide-set shoulder joints; a brutal gauntlet on the right, a stone fist on the left) ----------
const ARM_X = 40, ARM_REST = -HUNCH + 5; // the arms hang a little forward of plumb
const SHOULDER_Y = 70;
// Viking-shield pauldrons: a big shield behind a smaller one, bronze rims and bosses, plank lines and knotwork
function shield(name, r, y0, y1, mat, seed) {
  const paint = (f, i, j, w, h, x, y, du, dv) => {
    const fr = frosty(f, j, x, y, 0.4, i, w, h); if (fr) return fr;
    if (f !== 'up') return null;
    const rr = Math.hypot(du, dv);
    if (rr > r - 1.6) return hash(x, y, 51) < 0.25 ? mot('patina', x, y) : mot('brass', x, y);
    if (rr > r - 3.2 && rr < r - 2.2 && knot(Math.round(du + 40), Math.round(dv + 40))) return P.brassDark[0];
    if (Math.round(du + 40) % 3 === 0) return P.char[0];
    const c = crackAt(Math.round(du + r), Math.round(dv + r), 2 * r, 2 * r, seed);
    if (c === 'staple') return mot('iron', x, y);
    if (c === 'edge' && seed % 2) return P.crack[0];
    return null;
  };
  return disc(name, [0, 0, 0], r, y0, y1, 'y', mat, { paint });
}
function pauldron(side) {
  const s = side > 0 ? 'right' : 'left';
  const X = (a, b) => (side > 0 ? [a, b] : [-b, -a]);
  const px = ARM_X * side;
  // two layered shields on the shoulder, tilted out and down; his left back shield is a later bronze replacement
  const place = (cubes, dx, dy) => cubes.map((c) => ({ ...c, from: [c.from[0] + px + dx, c.from[1] + dy, c.from[2]], to: [c.to[0] + px + dx, c.to[1] + dy, c.to[2]] }));
  return G(`pauldron_${s}`, [px, SHOULDER_Y, 0], [
    G(`shield_${s}`, [px, 86, 0], [
      ...place(shield(`shield_${s}_back`, 12, 0, 3, side > 0 ? 'char' : 'brass', 61 + side), 4 * side, 86),
      ...place(disc(`shield_${s}_rim`, [0, 0, 0], 12.75, 0.75, 2.25, 'y', 'brass', { paint: bandTeeth }), 4 * side, 86),
      ...place(shield(`shield_${s}_front`, 8, 0, 2, side > 0 ? 'iron' : 'char', 71 + side), 8 * side, 89),
      ...place(disc(`shield_${s}_boss`, [0, 0, 0], 3, 0, 2, 'y', 'brass', { paint: (f, i, j, w, h, x, y) => (f === 'up' && i === 1 && j === 1 ? P.gold[0] : bandTeeth(f, i, j, w, h, x, y)) }), 8 * side, 91),
    ], { rot: [0, 0, -22 * side] }),
  ], { rot: [-ARM_REST, 0, 0] });
}
function finger(name, px, pz, z0, z1, curl, w = 6) {
  const x0 = px - w / 2, x1 = px + w / 2;
  return G(name, [px, 20, pz], [
    C(`${name}_knuckle`, [x0 + 0.5, 18.5, pz - 1], [x1 - 0.5, 20, pz + 1], 'brass', { paint: bandTeeth }),
    C(`${name}_base`, [x0, 13, z0], [x1, 18.5, z1], 'iron', { paint: aged }),
    C(`${name}_pin`, [x0 - 0.25, 12.5, (z0 + z1) / 2 - 1], [x1 + 0.25, 14, (z0 + z1) / 2 + 1], 'brass'),
    G(`${name}_tip`, [px, 13, pz], [
      C(`${name}_mid`, [x0 + 0.5, 7, z0 + 0.5], [x1 - 0.5, 13, z1 - 0.5], 'iron', { paint: aged }),
      C(`${name}_point`, [x0 + 1, 4, z0 + 1], [x1 - 1, 7, z1 - 1], 'brass', { paint: bandTeeth }),
    ], { rot: [curl * 1.2, 0, 0] }),
  ], { rot: [curl, 0, 0] });
}
function arm(side) {
  const s = side > 0 ? 'right' : 'left';
  const X = (a, b) => (side > 0 ? [a, b] : [-b, -a]);
  const px = ARM_X * side, fx = px + 2 * side; // forearm centre
  const hand = side > 0
    ? G('claw_right', [fx, 24, 0], [
        C('palm_right', [fx - 10, 20, -9], [fx + 10, 26, 9], 'iron', { paint: aged }),
        finger('finger_1', fx - 6, -6.5, -9, -4, -11),
        finger('finger_2', fx, -6.5, -9, -4, -12),
        finger('finger_3', fx + 6, -6.5, -9, -4, -9, 5.5),
        finger('thumb', fx - 2, 6.5, 4, 9, 5),
        G('held', [fx, 12, -4], [
          G('held_cog', [fx, 12, -4], [...cog('held_cog_disc', [fx, 12, -4], 6, -5, -3, 'z')], { hidden: true }),
          G('held_player', [fx, 10, -4], [
            C('dummy_head', [fx - 4, 4, -8], [fx + 4, 12, 0], 'dummy'),
            C('dummy_body', [fx - 4, -8, -6], [fx + 4, 4, -2], 'dummy'),
            C('dummy_arm_l', [fx - 8, -8, -6], [fx - 4, 4, -2], 'dummy'),
            C('dummy_arm_r', [fx + 4, -8, -6], [fx + 8, 4, -2], 'dummy'),
            C('dummy_legs', [fx - 4, -20, -6], [fx + 4, -8, -2], 'dummy'),
          ], { hidden: true }),
        ]),
      ])
    : G('fist_left', [fx, 26, 0], [
        C('fist_core', [fx - 10, 9, -10], [fx + 10, 26, 10], 'stone', { paint: stony(77) }),
        C('fist_band', [fx - 10.5, 18, -10.5], [fx + 10.5, 20.5, 10.5], 'iron', { paint: (f, i, j, w, h, x, y) => (sides(f) && i % 5 === 2 ? P.brass[1] : aged(f, i, j, w, h, x, y)) }),
        ...[-6.5, -2, 2.5, 7].map((dx, k) => C(`knuckle_${k}`, [fx + dx - 1.5, 21, -11.5], [fx + dx + 1.5, 24, -10], 'iron', { paint: (f, i, j, w, h, x, y) => (f === 'north' ? mot('ironHi', x, y) : null) })),
        C('fist_thumb', [fx + 10, 12, -8], [fx + 13, 21, -1], 'stone', { paint: stony(79, false) }),
      ], { rot: [0, 8, 0] });
  const lift = (n) => (n.kind === 'c' ? { ...n, from: [n.from[0], n.from[1] + 6, n.from[2]], to: [n.to[0], n.to[1] + 6, n.to[2]], origin: n.origin && [n.origin[0], n.origin[1] + 6, n.origin[2]] } : { ...n, origin: [n.origin[0], n.origin[1] + 6, n.origin[2]], kids: n.kids.map(lift) });
  const chains = side < 0
    ? [G('chain_wrap', [fx, 38, 0], [
        ...chainRing('chain_wrap_a', fx - 10, fx + 10, -10, 10, 43, 0.5),
      ]),
      G('chain_end', [fx - 9, 36, 16.5], [...chainRun('chain_end_links', [fx - 9, 35, 16.5], [fx - 9, 21, 16.5])])]
    : [];
  return G(`arm_${s}`, [px, SHOULDER_Y, 0], [
    // the shoulder joint: an axle stub out of the chest, then the cog the arm swings on (wide-set, so swings clear him)
    C(`shoulder_axle_${s}`, [X(26, 34)[0], SHOULDER_Y - 3, -3], [X(26, 34)[1], SHOULDER_Y + 3, 3], 'char', { paint: (f, i, j, w, h, x, y) => frosty(f, j, x, y, 0.5, i, w, h) }),
    G(`shoulder_cog_${s}`, [px, SHOULDER_Y, 0], [
      ...disc(`shoulder_drum_${s}`, [px, SHOULDER_Y, 0], 11, X(34, 46)[0], X(34, 46)[1], 'x', 'iron', { paint: aged }),
      C(`shoulder_teeth_${s}`, [X(34.5, 45.5)[0], SHOULDER_Y - 8.6, -8.6], [X(34.5, 45.5)[1], SHOULDER_Y + 8.6, 8.6], 'brass', { paint: bandTeeth, rotation: [45, 0, 0], origin: [px, SHOULDER_Y, 0] }),
    ]),
    C(`upper_arm_${s}`, [X(35, 46)[0], 50, -6], [X(35, 46)[1], 66, 6], 'iron', { paint: aged }),
    C(`upper_arm_band_${s}`, [X(34.5, 46.5)[0], 55, -6.5], [X(34.5, 46.5)[1], 57.5, 6.5], 'leather', { paint: leatherStrap }),
    C(`upper_arm_buckle_${s}`, [X(39, 42)[0], 54.5, -7.25], [X(39, 42)[1], 58, -6.5], 'brass'),
    C(`elbow_block_${s}`, [fx - 6, 48, -7], [fx + 6, 50, 1], 'char', { paint: aged }),
    ...cog(`elbow_cog_${s}`, [fx, 50, 0], 6, X(52, 54)[0], X(52, 54)[1], 'x'),
    pauldron(side),
    G(`forearm_${s}`, [fx, 48, -8], [
      C(`forearm_${s}_core`, [fx - 9, 32, -9], [fx + 9, 48, 9], 'iron', { paint: aged }),
      C(`wrist_cuff_${s}`, [fx - 9.5, 32, -9.5], [fx + 9.5, 34, 9.5], 'brass', { paint: bandTeeth }),
      G(`forearm_guard_${s}`, [fx, 40, -10], [
        C(`forearm_guard_${s}_plate`, [fx - 10, 35, -10], [fx + 10, 46, 10], side > 0 ? 'plate' : 'stone', { paint: side > 0 ? plated : stony(83) }),
        C(`forearm_guard_${s}_strap`, [fx - 10.5, 36.5, -10.5], [fx + 10.5, 38.5, 10.5], 'leather', { paint: leatherStrap }),
        C(`forearm_guard_${s}_buckle`, [fx - 1.5, 36, -11.25], [fx + 1.5, 39, -10.5], 'brass'),
      ]),
      ...chains,
      lift(hand),
    ], { rot: [side > 0 ? 16 : 10, 0, 0] }),
  ], { rot: [ARM_REST + (side > 0 ? 3 : 0), 0, side > 0 ? 4 : -5] });
}

// ---------- head (a carved Norse mask: one glowing slit behind a nose guard, runes across the brow, an iron beard) ----------
const FACE = -20; // the skull's front face
const maskPaint = (f, i, j, w, h, x, y) => {
  const fr = frosty(f, j, x, y, 0.5, i, w, h); if (fr) return fr;
  if (f !== 'north') return null;
  if (j >= 5 && j <= 6 && i >= 1 && i <= w - 2) return 'clear'; // the slit (the glow sits just behind it)
  if (j === 4 || j === 7) return P.char[0];
  if (j >= 8 && (i === 3 || i === w - 4)) return P.char[1]; // cheekbone grooves
  if (j <= 3 && knot(i, j)) return P.char[2];
  return hash(x, y, 43) < 0.08 ? P.stoneHi[0] : null;
};
const beardBar = (f, i, j, w, h, x, y) => (sides(f) && j % 6 === 3 ? P.char[0] : sides(f) && i === 0 ? mot('ironHi', x, y) : null);
// the beard: iron bars hanging from the jaw, longest in the middle, two braids ringed in bronze
const BEARD = [[-6, 9], [-4, 12], [-2, 15], [0, 16], [2, 15], [4, 12], [6, 9]];
const head = G('neck', [0, 74, -8], [
  C('neck_block', [-6, 72, -14], [6, 73.5, -7], 'char'),
  G('head', [0, 76, -10], [
    C('skull', [-10, 74, FACE], [10, 90, -7], 'iron', { paint: aged }),
    C('mask', [-9, 76, FACE - 2], [9, 88, FACE], 'stone', { paint: maskPaint }),
    G('slit', [0, 82, FACE - 1], [
      C('slit_dark', [-8, 81, FACE - 1.5], [8, 83, FACE - 0.5], 'slitOff'),
      G('slit_glow', [0, 82, FACE - 1], [C('slit_lit', [-8, 81, FACE - 1.75], [8, 83, FACE - 0.75], 'rune', { paint: (f, i) => (f === 'north' ? (i === 4 || i === 11 ? P.runeHi[0] : P.rune[0]) : null) })]),
    ]),
    C('nose_guard', [-1.5, 77, FACE - 3.5], [1.5, 83, FACE - 2], 'iron', { paint: (f, i, j, w, h, x, y) => (f === 'north' && j === 0 ? mot('ironHi', x, y) : f === 'north' && j === 1 ? P.gold[0] : null) }),
    G('brow_left', [-4.5, 84.5, FACE - 2], [C('brow_left_plate', [-10, 84, FACE - 5], [-0.75, 87.5, FACE - 2], 'iron', { paint: aged })], { rot: [0, 0, -10] }),
    G('brow_right', [4.5, 84.5, FACE - 2], [C('brow_right_plate', [0.75, 84, FACE - 5.25], [10, 87.5, FACE - 2], 'iron', { paint: aged })], { rot: [0, 0, 10] }),
    C('rune_band', [-10, 88, FACE - 2.5], [10, 90, FACE], 'char', { paint: (f, i, j) => (f === 'north' ? runeAt(i, j - 0, 1, -2, 3) || (j === 0 ? P.brass[1] : null) : null) }),
    ...[-1, 1].map((sd) => C(`cheek_${sd > 0 ? 'right' : 'left'}`, [sd > 0 ? 10 : -11.5, 75, FACE - 1], [sd > 0 ? 11.5 : -10, 87, -10], 'plate', { paint: plated })),
    G('helm', [0, 90, -12], [
      C('helm_cap', [-10.5, 90, FACE - 0.5], [10.5, 93, -6.5], 'iron', { paint: aged }),
      C('helm_top', [-7, 93, FACE + 2], [7, 95, -8.5], 'iron', { paint: aged }),
      C('helm_crest', [-1.25, 93, FACE - 1], [1.25, 97, -7], 'brass', { paint: bandTeeth }),
    ]),
    G('jaw', [0, 76, FACE - 1], [
      C('jaw_chin', [-9, 72, FACE - 5], [9, 76, FACE], 'iron', { paint: aged }),
      ...BEARD.map(([bx, len]) => C(`beard_${bx + 8}`, [bx - 1, 72 - len, FACE - 5], [bx + 1, 72, FACE - 3.5], 'iron', { paint: beardBar })),
      ...[-2, 2].map((bx) => C(`beard_ring_${bx > 0 ? 'r' : 'l'}`, [bx - 1.5, 61, FACE - 5.5], [bx + 1.5, 63, FACE - 3], 'brass', { paint: (f, i) => (f === 'north' && i === 1 ? P.gold[0] : null) })),
      G('beard_glow', [0, 62, FACE - 1], [C('beard_glow_plate', [-7, 56, FACE - 3.25], [7, 72, FACE - 3], 'rune')], { hidden: true }),
      G('vent_steam', [0, 64, FACE - 4], [
        C('vent_puff_a', [-6, 58, FACE - 12], [6, 70, FACE - 4], 'steam'),
        C('vent_puff_b', [-9, 55, FACE - 22], [9, 73, FACE - 12], 'steam'),
        C('vent_puff_c', [-12, 52, FACE - 34], [12, 76, FACE - 22], 'steam'),
      ], { hidden: true }),
    ]),
  ], { rot: [0, 5, -5] }),
], { rot: [-HUNCH - 5, 0, 0] });

// ---------- back drum, key and steam pipes ----------
const KEY_Y = 76, LOOP = 26, LOOP_H = 11, BAR = 4;
// the key: tarnished bronze, the hero; a gold inlay runs along its loops
const keyBronze = (f, i, j, w, h, x, y) => (sides(f) && j === h - 1 ? mot('brassDark', x, y) : hash(x, y, 17) < 0.1 ? mot('patina', x, y) : null);
const inlay = (f, i, j, w, h, x, y) => (f === 'south' && j === 1 && i > 1 && i < w - 2 && i % 2 === 0 ? P.gold[(i / 2) % 2] : keyBronze(f, i, j, w, h, x, y));
function keyLoop(side) {
  const s = side > 0 ? 'r' : 'l';
  const X = (a, b) => (side > 0 ? [a, b] : [-b, -a]);
  const y = (v) => KEY_Y + v;
  return [
    C(`key_loop_${s}_top`, [X(4, LOOP)[0], y(LOOP_H - BAR), 34], [X(4, LOOP)[1], y(LOOP_H), 38], 'brass', { paint: inlay }),
    C(`key_loop_${s}_bottom`, [X(4, LOOP)[0], y(-LOOP_H), 34], [X(4, LOOP)[1], y(-LOOP_H + BAR), 38], 'brass', { paint: inlay }),
    C(`key_loop_${s}_outer`, [X(LOOP - BAR, LOOP)[0], y(-LOOP_H + BAR), 34], [X(LOOP - BAR, LOOP)[1], y(LOOP_H - BAR), 38], 'brass', { paint: keyBronze }),
  ];
}
const drumBack = (f, i, j, w, h, x, y, du, dv) => {
  if (f === 'south') { const r = Math.hypot(du, dv); if (r > 9 && r < 11.5) { const a = Math.round((Math.atan2(dv, du) + Math.PI) * 9); if (a % 4 !== 3 && hash(a, Math.round(r), 61) < 0.45) return P.rune[0]; } }
  return aged(f, i, j, w, h, x, y);
};
const backDrum = G('back_drum', [0, KEY_Y, 15], [
  ...disc('drum', [0, KEY_Y, 0], 14, 15, 25, 'z', 'iron', { paint: drumBack }),
  ...disc('drum_band_front', [0, KEY_Y, 0], 15, 16, 17.5, 'z', 'brass', { paint: bandTeeth }),
  ...disc('drum_band_back', [0, KEY_Y, 0], 15, 22.5, 24, 'z', 'brass', { paint: bandTeeth }),
  G('key_collar', [0, KEY_Y, 25], [...cog('key_collar_ring', [0, KEY_Y, 25], 7, 25, 28, 'z')]),
  // the click pawl that rides the collar's teeth (the spark point when the key jams)
  G('pawl', [10, KEY_Y + 7, 25.5], [
    C('pawl_bolt', [9, KEY_Y + 6, 25], [11, KEY_Y + 8, 27], 'brass'),
    C('pawl_arm', [4.5, KEY_Y + 6.25, 25.25], [10, KEY_Y + 7.75, 26.75], 'iron', { rotation: [0, 0, 20], origin: [10, KEY_Y + 7, 26] }),
  ]),
  G('sparks', [6, KEY_Y + 6, 27], [
    C('spark_a', [5, KEY_Y + 8, 26.5], [6, KEY_Y + 9, 27.5], 'spark'), C('spark_b', [8, KEY_Y + 9, 27], [9, KEY_Y + 10, 28], 'spark'),
    C('spark_c', [3, KEY_Y + 5, 27], [4, KEY_Y + 6, 28], 'spark'), C('spark_d', [9, KEY_Y + 4, 26.5], [10, KEY_Y + 5, 27.5], 'spark'),
  ], { hidden: true }),
  G('arcs', [0, KEY_Y, 36], [
    C('arc_a', [-16, KEY_Y + 6, 38.5], [-4, KEY_Y + 7, 39.5], 'runeHi', { rotation: [0, 0, 25], origin: [-10, KEY_Y + 6.5, 39] }),
    C('arc_b', [5, KEY_Y - 9, 38.5], [17, KEY_Y - 8, 39.5], 'runeHi', { rotation: [0, 0, -30], origin: [11, KEY_Y - 8.5, 39] }),
    C('arc_c', [-2, KEY_Y + 14, 38.5], [9, KEY_Y + 15, 39.5], 'runeHi', { rotation: [0, 0, 10], origin: [3, KEY_Y + 14.5, 39] }),
  ], { hidden: true }),
  G('key', [0, KEY_Y, 25], [
    C('key_stem', [-4, KEY_Y - 4, 28], [4, KEY_Y + 4, 33], 'brass', { paint: (f, i, j, w, h, x, y) => (sides(f) && i % 3 === 0 ? mot('brassDark', x, y) : null) }),
    C('key_hub', [-4, KEY_Y - 6, 33], [4, KEY_Y + 6, 38], 'brass', { paint: (f, i, j, w, h, x, y) => (f === 'south' && i >= 2 && i <= 5 && j >= 3 && j <= 8 ? ((i + j) % 2 ? P.gold[0] : P.brassDark[0]) : null) }),
    ...keyLoop(1),
    ...keyLoop(-1),
  ], { rot: [0, 0, 90] }),
  ...[-1, 1].map((side) => {
    const s = side > 0 ? 'right' : 'left';
    const X = (a, b) => (side > 0 ? [a, b] : [-b, -a]);
    return G(`pipe_${s}`, [8 * side, 87, 19], [
      C(`pipe_${s}_up`, [X(6.5, 9.5)[0], 87, 18], [X(6.5, 9.5)[1], 96, 21], 'iron', { paint: aged }),
      C(`pipe_${s}_flange`, [X(6, 10)[0], 89, 17.5], [X(6, 10)[1], 90.5, 21.5], 'brass', { paint: bandTeeth }),
      C(`pipe_${s}_over`, [X(6.5, 9.5)[0], 93, 7], [X(6.5, 9.5)[1], 96, 18], 'iron', { paint: aged }),
      C(`pipe_${s}_vent`, [X(6, 10)[0], 92, 5], [X(6, 10)[1], 97, 7], 'brass', { paint: (f, i, j) => (f === 'north' && i > 0 && i < 3 && j > 0 && j < 4 ? P.slot[0] : null) }),
      G(`pipe_steam_${s}`, [8 * side, 95, 4], [C(`pipe_puff_${s}`, [X(5, 11)[0], 92, -3], [X(5, 11)[1], 99, 4], 'steam')], { hidden: true }),
    ]);
  }),
]);

// ---------- torso (an inverted triangle: a broad chest over a narrow belly) ----------
// the forge-maker's seal: a stamped bronze roundel, a gold ring, a hammer over an anvil
const HAMMER = ['#######', '#######', '...#...', '...#...', '.#####.', '..###..', '#######'];
const sealPaint = (f, i, j, w, h, x, y, du, dv) => {
  if (f !== 'north') return null;
  const r = Math.hypot(du, dv);
  if (r > 5) return mot('brassDark', x, y);
  if (r > 4) return Math.round((Math.atan2(dv, du) + Math.PI) * 4) % 2 === 0 ? P.gold[0] : P.gold[1];
  const hx = Math.round(du + 3), hy = Math.round(3 - dv);
  if (hx >= 0 && hx < 7 && hy >= 0 && hy < 7 && HAMMER[hy][hx] === '#') return P.brassDark[0];
  return hash(x, y, 47) < 0.15 ? mot('patina', x, y) : null;
};
// the chest under the pectoral plates: cracked, rune-light showing through (seen once the plates are gone)
const chestPaint = (f, i, j, w, h, x, y) => {
  const fr = frosty(f, j, x, y, 0.5, i, w, h); if (fr) return fr;
  if (f === 'north') { const c = crackAt(i, j, w, h, 91) || crackAt((i + 23) % w, j, w, h, 97); if (c === 'glow') return P.rune[(i + j) % 2]; if (c === 'edge') return P.crack[0]; if (c === 'staple') return mot('ironHi', x, y); }
  return aged(f, i, j, w, h, x, y);
};
const louvre = (face) => (f, i, j, w, h, x, y) => (f === face ? (j % 2 === 1 && i > 0 && i < w - 1 ? P.slot[0] : mot('ironHi', x, y)) : null);
const GZ = -16; // the upper chest's front face
const pectoral = (side) => {
  const s = side > 0 ? 'r' : 'l';
  const X = (a, b) => (side > 0 ? [a, b] : [-b, -a]);
  const pp = (f, i, j, w, h, x, y) => (f === 'north' && j >= 4 && j <= 8 ? runeAt(i, j, 4, 4, 2 + side) : null) || plated(f, i, j, w, h, x, y);
  // the plate swings forward about its outer edge, so its inner edge stands proud: a prow under the beard
  return C(`pectoral_${s}`, [X(16, 26)[0], 61, GZ - 2.5], [X(16, 26)[1], 72, GZ], 'plate', { paint: pp, rotation: [0, -5 * side, 0], origin: [26 * side, 68, GZ] });
};
const torso = G('torso', [0, 40, 0], [
  ...disc('waist_core', [0, 40, 0], 6, -12, 12, 'x', 'char', { paint: (f, i, j, w, h, x, y) => (sides(f) && i % 3 === 0 ? P.brassDark[0] : null) }),
  C('belly', [-16, 46.5, -12], [16, 54, 12], 'iron', { paint: aged }),
  ...disc('seal', [0, 53, 0], 6, -15, -14.25, 'z', 'brass', { paint: sealPaint }),
  G('belly_chain', [0, 53, 0], [...chainRing('belly_chain', -16, 16, -12, 12, 53, 0.25)]),
  C('chest_lower', [-21, 54, -14], [21, 62, 14], 'iron', { paint: aged }),
  C('chest_upper', [-26, 62, GZ], [26, 72, 15], 'iron', { paint: chestPaint }),
  C('chest_back', [-18, 72, 1], [18, 80, 15], 'iron', { paint: aged }),
  ...[-1, 1].map((sd) => C(`chest_vent_${sd > 0 ? 'right' : 'left'}`, [sd > 0 ? 21 : -22, 55, -7], [sd > 0 ? 22 : -21, 61, 5], 'iron', { paint: louvre(sd > 0 ? 'east' : 'west') })),
  G('chest_plate', [0, 68, GZ - 2], [pectoral(1), pectoral(-1)]),
  G('mainspring', [0, 64, GZ - 6], [
    ...[7, 5, 3].flatMap((r, k) => [
      C(`ms_${k}_t`, [-r, 64 + r - 1, GZ - 6.5], [r, 64 + r, GZ - 5.5], 'plateHi'), C(`ms_${k}_b`, [-r, 64 - r, GZ - 6.5], [r, 64 - r + 1, GZ - 5.5], 'plateHi'),
      C(`ms_${k}_l`, [-r, 64 - r + 1, GZ - 6.5], [-r + 1, 64 + r - 1, GZ - 5.5], 'plateHi'), C(`ms_${k}_r`, [r - 1, 64 - r + 1, GZ - 6.5], [r, 64 + r - 1, GZ - 5.5], 'plateHi'),
    ]),
    C('ms_arbor', [-1, 63, GZ - 7], [1, 65, GZ - 5], 'brass'),
  ], { hidden: true }),
  arm(1),
  arm(-1),
  backDrum,
  head,
]);

// ---------- the Juggernaut ----------
const hipsPaint = (f, i, j, w, h, x, y) => (sides(f) && j <= 1 ? mot('brass', x, y) : aged(f, i, j, w, h, x, y));
const jug = G('juggernaut', [0, 0, 0], [
  G('hips', [0, 34, 0], [
    C('girdle', [-19, 35, -12], [19, 40, 12], 'iron', { paint: hipsPaint }),
    C('fauld', [-3.5, 21, -15], [3.5, 35, -13], 'leather', { paint: (f, i, j, w, h, x, y) => (sides(f) && j % 5 === 0 ? P.char[0] : f === 'north' && i === 3 && j % 5 === 2 ? P.brass[2] : null) }),
    G('cog_rack', [0, 30, 12], [
      C('cog_peg', [-1, 29, 12], [1, 31, 22.5], 'iron'),
      G('cog_1', [0, 30, 15], [...cog('rack_cog_1', [0, 30, 0], 6, 15, 17, 'z')]),
      G('cog_2', [0, 30, 17.5], [...cog('rack_cog_2', [0, 30, 0], 6, 17.5, 19.5, 'z')]),
      G('cog_3', [0, 30, 20], [...cog('rack_cog_3', [0, 30, 0], 6, 20, 22, 'z')]),
    ]),
    leg(1),
    leg(-1),
    G('waist', [0, 40, 0], [torso], { rot: [HUNCH, 0, 0] }),
  ]),
  G('stomp_ring', [0, 0, -6], [
    C('ring_n', [-24, 0, -30], [24, 1, -28], 'frost'), C('ring_s', [-24, 0, 16], [24, 1, 18], 'frost'),
    C('ring_w', [-26, 0, -30], [-24, 1, 18], 'frost'), C('ring_e', [24, 0, -30], [26, 1, 18], 'frost'),
  ], { hidden: true }),
]);

// ---------- preview props: built where the clips use them, parked 400 px below ----------
const PROP_DROP = 400;
const pv = (v) => [v[0], v[1] - PROP_DROP, v[2]];
const PC = (name, from, to, mat, o = {}) => C(name, pv(from), pv(to), mat, { ...o, origin: o.origin ? pv(o.origin) : undefined });
const PG = (name, origin, kids) => G(name, pv(origin), kids);
// a Wither-sized stand-in for scale (3.5 blocks), beside him
const propScale = PG('prop_scale', [0, 0, 0], [PC('scale_wither', [64, 0, -8], [80, 56, 8], 'propStone', { paint: (f, i, j) => (sides(f) && j % 16 === 0 ? P.slot[0] : null) })]);
const PROPS = ['prop_scale'];
const ROOTS = [jug, propScale];

// ---------- build: pack UVs, paint, emit ----------
const tex = new Uint8Array(TEX * TEX * 4);
const cubes = [];
for (const root of ROOTS) (function collect(n) { if (n.kind === 'c') cubes.push(n); else n.kids.forEach(collect); })(root);
for (const c of cubes) {
  c.size = [0, 1, 2].map((a) => +(c.to[a] - c.from[a]).toFixed(4));
  const [w, h, d] = c.size;
  c.netW = Math.ceil(2 * (w + d));
  c.netH = Math.ceil(d + h);
}
const order = [...cubes].sort((a, b) => b.netH - a.netH || b.netW - a.netW);
let sx = 0, sy = 0, rowH = 0;
for (const c of order) {
  if (sx + c.netW > TEX) { sx = 0; sy += rowH; rowH = 0; }
  c.uv = [sx, sy];
  sx += c.netW;
  rowH = Math.max(rowH, c.netH);
}
if (sy + rowH > TEX) throw new Error('texture overflow: ' + (sy + rowH));
console.error('texture rows used:', sy + rowH, 'of', TEX);

function faceRects(c) {
  const [w, h, d] = c.size;
  const [u, v] = c.uv;
  return { east: [u, v + d, d, h], north: [u + d, v + d, w, h], west: [u + d + w, v + d, d, h], south: [u + 2 * d + w, v + d, w, h], up: [u + d, v, w, d], down: [u + d + w, v, w, d] };
}
function paintCube(c, uv, buf, size) {
  const R = faceRects({ size: c.size, uv });
  for (const f of Object.keys(R)) {
    const [fx, fy, fw, fh] = R[f];
    const x0 = Math.floor(fx), y0 = Math.floor(fy);
    const W = Math.max(1, Math.ceil(fx + fw) - x0), H = Math.max(1, Math.ceil(fy + fh) - y0);
    for (let j = 0; j < H; j++)
      for (let i = 0; i < W; i++) {
        const x = x0 + i, y = y0 + j;
        let col = c.paint ? c.paint(f, i, j, W, H, x, y) : null;
        if (col === null || col === undefined) col = mot(c.mat, x, y);
        const o = (y * size + x) * 4;
        if (col === 'clear') { buf[o + 3] = 0; continue; }
        const [r, g, b, a] = rgb(col);
        buf[o] = r; buf[o + 1] = g; buf[o + 2] = b; buf[o + 3] = a;
      }
  }
}
for (const c of cubes) paintCube(c, c.uv, tex, TEX);
function png(w, h, data) {
  const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
  const crc = (buf) => { let c = 0xffffffff; for (const b of buf) c = crcT[(c ^ b) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
  const chunk = (type, body) => { const len = Buffer.alloc(4); len.writeUInt32BE(body.length); const tb = Buffer.concat([Buffer.from(type), body]); const cr = Buffer.alloc(4); cr.writeUInt32BE(crc(tb)); return Buffer.concat([len, tb, cr]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) { raw[y * (w * 4 + 1)] = 0; Buffer.from(data.buffer, y * w * 4, w * 4).copy(raw, y * (w * 4 + 1) + 1); }
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
const pngBuf = png(TEX, TEX, tex);
fs.writeFileSync(path.resolve(path.dirname(OUT), 'clockwork-juggernaut.png'), pngBuf);

const elements = [], groups = [], groupByName = {};
function emit(n, hidden = false) {
  if (n.kind === 'c') {
    n.uuid = uuid();
    const R = faceRects(n);
    const faces = {};
    for (const f of Object.keys(R)) { const [x, y, w, h] = R[f]; faces[f] = { uv: [x, y, x + w, y + h], texture: 0 }; }
    elements.push({
      name: n.name, box_uv: true, render_order: 'default', locked: false, export: true, allow_mirror_modeling: true,
      from: n.from, to: n.to, autouv: 0, color: 0, rotation: n.rotation || [0, 0, 0],
      origin: n.origin || [(n.from[0] + n.to[0]) / 2, (n.from[1] + n.to[1]) / 2, (n.from[2] + n.to[2]) / 2],
      uv_offset: n.uv, faces, type: 'cube', uuid: n.uuid, visibility: !hidden,
    });
    return n.uuid;
  }
  n.uuid = uuid();
  if (groupByName[n.name]) throw new Error('duplicate bone ' + n.name);
  groupByName[n.name] = n;
  groups.push({ name: n.name, uuid: n.uuid, export: true, locked: false, visibility: !n.hidden, origin: n.origin, rotation: n.rot, color: 0, children: [], reset: false, shade: true, mirror_uv: false, autouv: 0, isOpen: false });
  return { uuid: n.uuid, isOpen: ['juggernaut', 'hips', 'waist', 'torso'].includes(n.name), children: n.kids.map((k) => emit(k, hidden || n.hidden)) };
}
const outliner = ROOTS.map((r) => emit(r));

module.exports = { P, G, C, jug, cubes, groupByName, elements, groups, outliner, pngBuf, uuid, TEX, OUT, PROPS, PROP_DROP, HUNCH, png, paintCube, ARGS };
if (require.main === module) {
  const clips = path.join(__dirname, 'clockwork-juggernaut.clips.js');
  const animations = fs.existsSync(clips) ? require(clips).animations : [];
  const bb = {
    meta: { format_version: '5.0', model_format: 'modded_entity', box_uv: true },
    name: 'clockwork_juggernaut', model_identifier: 'clockwork_juggernaut', modded_entity_entity_class: '', modded_entity_version: '1.17', modded_entity_flip_y: true,
    visible_box: [4, 7, 0], variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: TEX, height: TEX },
    elements, groups, outliner,
    textures: [{ path: '', name: 'clockwork_juggernaut.png', folder: '', namespace: '', id: '0', group: '', width: TEX, height: TEX, uv_width: TEX, uv_height: TEX, particle: false, use_as_default: false, layers_enabled: false, sync_to_project: '', render_mode: 'default', render_sides: 'auto', pbr_channel: 'color', frame_time: 1, frame_order_type: 'loop', frame_order: '', frame_interpolate: false, visible: true, internal: true, saved: false, uuid: uuid(), source: 'data:image/png;base64,' + pngBuf.toString('base64') }],
    animations,
  };
  fs.writeFileSync(OUT, JSON.stringify(bb));
  console.error('wrote', OUT, elements.length, 'cubes', groups.length, 'groups', animations.length, 'animations');
}
