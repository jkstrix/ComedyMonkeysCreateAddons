// Clockwork Wastes: the Derelict Cable's corner knot, 2 px -> 2.5 px (D74) -> design/models/clockwork-wastes-cable-knot.bbmodel
// (Generic Model preview). Approved (D76) and in the mod: the exporter's cableKnot() is the AFTER knot, and its wall arms are
// the fixed ones below (derelict_cable_arm_south turned y 270).
// node clockwork-wastes-cable-knot.gen.js   (writes the preview and prints the z-fight check, before and after;
// --as-mod draws the wall's up/down arms as the mod drew them before D76, see FOUND below)
// The knot (design/models/clockwork-wastes.export.js, "the Derelict Cable") is a void-alloy clip 2 x 3 px, 1.5 high, laid
// over the middle of a cable block wherever two arms meet at right angles. It is exactly as wide as the strand pair, so the
// strands of an arm running along its long side share its side faces (z-fight). The fix: widen it to 2.5 px (x 6.75-9.25 in
// its model), everything else unchanged.
// The scene builds the blocks as the game does: the arm models (the east half of the city preview's floor run, as the
// exporter cuts them), turned by the blockstate's x and y for each surface and side, and the knot turned as the exporter's
// multipart turns it; the riser foot (a mast girder's cable foot) as the exporter's city props section builds it. Two copies
// side by side: BEFORE (2 px knot, west) and AFTER (2.5 px knot, 10 blocks east). Each: Scorched End Stone floor with an L
// corner, two Ts, a cross and runs between them; a void-alloy wall (D75's plate) with a wall corner; a riser foot on a
// Create girder with a side arm. Glows are shade:false parts in lit_ groups; clips 'dormant' and 'awake'.
// The geometry helpers below are copies of the exporter's (it writes the mod's assets when run, so it can't be required).
const path = require('path');
const fs = require('fs');
const { preview, DIRS, fitUv } = require('./process-box.lib.js');
const M = require('./clockwork-wastes-materials.gen.js');
const CITY = require('./clockwork-wastes-city.gen.js');
const CP = require('./clockwork-wastes-city-props.gen.js');
require('./clockwork-wastes-building-blocks.gen.js'); // paints D75's void-alloy plate (va_bond_s) for the wall
const { check } = require('./zfight-check.js');
const { T, box } = M;

const OUT = path.resolve(__dirname, 'clockwork-wastes-cable-knot.bbmodel');
const GLOW = { shade: false };

// ---------- the exporter's geometry helpers (copies) ----------
const r3 = (n) => Math.round(n * 1000) / 1000;
const AXIS_I = { x: 0, y: 1, z: 2 };
const faceTex = (c, dir) => (dir in c.faces ? c.faces[dir] : c.faces.all);
const move = (cubes, d) => cubes.map((c) => ({ ...c, from: c.from.map((v, i) => v + d[i]), to: c.to.map((v, i) => v + d[i]),
  rot: c.rot && [c.rot[0], c.rot[1], c.rot[2].map((v, i) => v + d[i])] }));
const rad = (d) => (d * Math.PI) / 180;
function turnPoint(p, axis, deg, o) {
  const a = rad(deg), q = p.map((v, i) => v - o[i]);
  const [i, j] = axis === 'x' ? [1, 2] : axis === 'y' ? [2, 0] : [0, 1];
  const u = q[i] * Math.cos(a) - q[j] * Math.sin(a), w = q[i] * Math.sin(a) + q[j] * Math.cos(a);
  q[i] = u; q[j] = w;
  return q.map((v, k) => v + o[k]);
}
const VEC = { north: [0, 0, -1], south: [0, 0, 1], west: [-1, 0, 0], east: [1, 0, 0], down: [0, -1, 0], up: [0, 1, 0] };
const dirOf = (v) => Object.keys(VEC).find((d) => VEC[d].every((n, i) => Math.abs(n - v[i]) < 1e-6));
const FRAME = { north: [[-1, 0, 0], [0, -1, 0]], south: [[1, 0, 0], [0, -1, 0]], east: [[0, 0, -1], [0, -1, 0]],
  west: [[0, 0, 1], [0, -1, 0]], up: [[1, 0, 0], [0, 0, 1]], down: [[1, 0, 0], [0, 0, -1]] };
const neg = (v) => v.map((n) => -n || 0);
const same = (a, b) => a.every((n, i) => Math.abs(n - b[i]) < 1e-6);
const faceTurn = (U, V, deg) => ({ 0: [U, V], 90: [V, neg(U)], 180: [neg(U), neg(V)], 270: [neg(V), U] }[deg]);
function quarter(cubes, axis, deg, o) {
  return cubes.map((c) => {
    if (c.rot) throw new Error(c.name + ': quarter() turns unrotated cubes only');
    const a = turnPoint(c.from, axis, deg, o), b = turnPoint(c.to, axis, deg, o);
    const from = a.map((v, i) => r3(Math.min(v, b[i]))), to = a.map((v, i) => r3(Math.max(v, b[i])));
    const faces = {}, uv = {}, frot = {};
    for (const d of DIRS) {
      const tex = faceTex(c, d);
      const nd = dirOf(turnPoint(VEC[d], axis, deg, [0, 0, 0]));
      faces[nd] = tex || null;
      if (!tex) continue;
      uv[nd] = (c.uv && c.uv[d]) || fitUv(d, c.from, c.to);
      const [tu, tv] = faceTurn(...FRAME[d], (c.frot && c.frot[d]) || 0).map((v) => turnPoint(v, axis, deg, [0, 0, 0]));
      const r = [0, 90, 180, 270].find((k) => { const [u, v] = faceTurn(...FRAME[nd], k); return same(u, tu) && same(v, tv); });
      if (r === undefined) throw new Error(c.name + ': no face rotation for ' + nd);
      if (r) frot[nd] = r;
    }
    return { ...c, from, to, faces, uv, frot };
  });
}
function cut(cubes, axis, lo, hi, by) {
  const i = AXIS_I[axis], lowDir = { x: 'west', y: 'down', z: 'north' }[axis], highDir = { x: 'east', y: 'up', z: 'south' }[axis];
  const out = [];
  for (const c of cubes) {
    const a = Math.max(c.from[i], lo), b = Math.min(c.to[i], hi);
    const flat = c.from[i] === c.to[i];
    if (flat ? !(c.from[i] >= lo && c.from[i] < hi) : b <= a) continue;
    if (c.rot && (c.from[i] < lo || c.to[i] > hi)) throw new Error(c.name + ': a rotated cube can\'t be cut');
    const from = [...c.from], to = [...c.to];
    if (!flat) { from[i] = a; to[i] = b; }
    const faces = { ...c.faces };
    if (!flat && a > c.from[i]) faces[lowDir] = null;
    if (!flat && b < c.to[i]) faces[highDir] = null;
    let uv = c.uv;
    if (uv && !flat && (a > c.from[i] || b < c.to[i])) {
      uv = {};
      const len = c.to[i] - c.from[i];
      for (const [dir, r] of Object.entries(c.uv)) {
        const [tu, tv] = faceTurn(...FRAME[dir], (c.frot && c.frot[dir]) || 0);
        const cu = (lo0, hi0, s) => (s > 0 ? [lo0 + (a - c.from[i]) / len * (hi0 - lo0), lo0 + (b - c.from[i]) / len * (hi0 - lo0)]
          : [lo0 + (c.to[i] - b) / len * (hi0 - lo0), lo0 + (c.to[i] - a) / len * (hi0 - lo0)]);
        const nu = tu[i] ? cu(r[0], r[2], tu[i]) : [r[0], r[2]], nv = tv[i] ? cu(r[1], r[3], tv[i]) : [r[1], r[3]];
        uv[dir] = [nu[0], nv[0], nu[1], nv[1]];
      }
    }
    const d = [0, 0, 0]; d[i] = -by;
    out.push(move([{ ...c, from, to, faces, uv }], d)[0]);
  }
  return out;
}
const area = (c, dir) => {
  const s = [0, 1, 2].map((i) => Math.abs(c.to[i] - c.from[i]));
  return dir === 'north' || dir === 'south' ? s[0] * s[1] : dir === 'east' || dir === 'west' ? s[2] * s[1] : s[0] * s[2];
};
const tiled = (cubes) => cubes.flatMap((c) => (c.rot || c.to[1] - c.from[1] < 16 ? [c] : [-16, 0, 16, 32].flatMap((lo) => cut([c], 'y', lo, lo + 16, 0))));
// the exporter's z-fight rule: only fights between different textures show
function zfights(cubes) {
  const byTex = {};
  for (const c of cubes) for (const d of DIRS) {
    const t = faceTex(c, d);
    if (t && area(c, d) > 1e-6) (byTex[t] = byTex[t] || []).push({ ...c, faces: { [d]: t } });
  }
  const keys = Object.keys(byTex), parts = Object.fromEntries(keys.map((k, i) => ['t' + i, byTex[k]]));
  const out = [];
  for (const p of check(parts)) {
    const m = p.match(/^t(\d+):(\S+) (\w+) vs t(\d+):(.*)$/);
    if (m && m[1] !== m[4]) out.push(`${keys[m[1]]}:${m[2]} ${m[3]} vs ${keys[m[4]]}:${m[5]}`);
  }
  return out;
}

// ---------- the Derelict Cable's models, as the exporter builds them ----------
const arm = cut(CITY.floorCable('cable'), 'x', 8, 16, 0), armGlow = cut(CITY.floorCableGlow('cable'), 'x', 8, 16, 0);
const KNOT_X = { before: [7, 9], after: [6.75, 9.25] };
const knot = (v) => [box('knot', [KNOT_X[v][0], 0, 6.5], [KNOT_X[v][1], 1.5, 9.5], { all: 'va', down: null })];
// FOUND (fixed in the mod by D76): the exporter's derelict_cable_arm_south is quarter(arm, 'y', 90), which runs NORTH (z 0-8),
// while its turnFor() takes it to run south, so on every wall the 'up' and 'down' arms are drawn on each other's half (a
// straight run up a wall looks whole; a wall corner or a run's end doesn't). This preview draws the arm truly south
// (y 270) so its wall corner shows the look intended, as the mod now does; --as-mod draws it as the mod did before D76.
const MOD_WALL_ARMS = process.argv.includes('--as-mod');
const SOUTH = MOD_WALL_ARMS ? 90 : 270;
const MODEL = { arm, arm_glow: armGlow, arm_south: quarter(arm, 'y', SOUTH, [8, 0, 8]), arm_glow_south: quarter(armGlow, 'y', SOUTH, [8, 0, 8]) };
// the blockstate's turn (x, then y, in quarter turns, as vanilla turns a model about the block's middle)
const turn = (cubes, rot) => {
  let out = cubes;
  if (rot.x) out = quarter(out, 'x', (360 - rot.x) % 360, [8, 8, 8]);
  if (rot.y) out = quarter(out, 'y', (360 - rot.y) % 360, [8, 8, 8]);
  return out;
};
const V = { down: [0, -1, 0], up: [0, 1, 0], north: [0, 0, -1], south: [0, 0, 1], west: [-1, 0, 0], east: [1, 0, 0] };
const bsTurn = (v, x, y) => turnPoint(turnPoint(v, 'x', -x, [0, 0, 0]), 'y', -y, [0, 0, 0]);
const vdir = (v) => Object.keys(V).find((d) => same(V[d], v));
const turnFor = (surface, d) => {
  for (const [suffix, arm0] of [['', V.east], ['_south', V.south]])
    for (const x of [0, 90, 180, 270]) for (const y of [0, 90, 180, 270])
      if (vdir(bsTurn(V.down, x, y)) === surface && vdir(bsTurn(arm0, x, y)) === d) return { suffix, rot: { ...(x ? { x } : {}), ...(y ? { y } : {}) } };
  throw new Error('no turn for ' + surface + ' ' + d);
};
const along = (s) => Object.keys(V).filter((d) => V[d].every((n, i) => n * V[s][i] === 0));
const perp = (a, b) => V[a].every((n, k) => n * V[b][k] === 0);
// one cable block on surface s with arms toward the given sides: { body, glow }
function cableBlock(s, arms, v) {
  const body = [], glow = [];
  for (const d of arms) {
    const t = turnFor(s, d);
    body.push(...turn(MODEL['arm' + t.suffix], t.rot));
    glow.push(...turn(MODEL['arm_glow' + t.suffix], t.rot));
  }
  if (arms.some((a) => arms.some((b) => a !== b && perp(a, b)))) body.push(...turn(knot(v), turnFor(s, along(s)[0]).rot));
  return { body, glow };
}
// the mast riser (the exporter's city props section): the girder south of the cable block (surface south, y 0)
const RISER = (() => {
  const R = CP.riser(5);
  const body = move(R.body, [0, 0, 16]), line = move(R.line, [0, 0, 16]);
  const footRun = CP.groundRun('foot_run', [8, 8], [8, 17.25]);
  const foot = [...tiled(body.filter((c) => c.name.startsWith('guard_'))), ...cut(body.filter((c) => /^riser_\d$/.test(c.name)), 'y', 0, 16, 0), ...footRun.body];
  const footGlow = [...cut(line, 'y', 0, 16, 0), ...footRun.line];
  return { foot, footGlow, run: cut(body, 'y', 32, 48, 32), runGlow: cut(line, 'y', 32, 48, 32) };
})();
// the riser's foot with floor arms (never north, toward... the girder is south: its own run goes there); the knot, turned
// a quarter (y 90) for a north/south girder, where an arm meets the run at a corner
function riserFoot(arms, v) {
  const body = [...RISER.foot], glow = [...RISER.footGlow];
  for (const d of arms) {
    const t = turnFor('down', d);
    body.push(...turn(MODEL['arm' + t.suffix], t.rot));
    glow.push(...turn(MODEL['arm_glow' + t.suffix], t.rot));
  }
  if (arms.some((d) => d !== 'north')) body.push(...turn(knot(v), { y: 90 }));
  return { body, glow };
}

// ---------- the scene ----------
// cable cells: [name, block x, y, z, surface, arms] (block coords inside one copy; x east, z south)
const CELLS = [
  // the floor network: an L, runs, a T, a cross, a T at the riser's side run
  ['L', 1, 0, 1, 'down', ['east', 'south']], ['r_2_1', 2, 0, 1, 'down', ['west', 'east']], ['r_3_1', 3, 0, 1, 'down', ['west', 'east']],
  ['T_north', 4, 0, 1, 'down', ['west', 'east', 'south']], ['r_5_1', 5, 0, 1, 'down', ['west', 'east']], ['r_6_1', 6, 0, 1, 'down', ['west', 'east']],
  ['r_1_2', 1, 0, 2, 'down', ['north', 'south']], ['r_4_2', 4, 0, 2, 'down', ['north', 'south']],
  ['r_0_3', 0, 0, 3, 'down', ['west', 'east']], ['T_west', 1, 0, 3, 'down', ['north', 'west', 'east']], ['r_2_3', 2, 0, 3, 'down', ['west', 'east']],
  ['r_3_3', 3, 0, 3, 'down', ['west', 'east']], ['cross', 4, 0, 3, 'down', ['north', 'south', 'west', 'east']], ['r_5_3', 5, 0, 3, 'down', ['west', 'east']],
  ['r_6_3', 6, 0, 3, 'down', ['west', 'east']],
  ['T_south', 4, 0, 4, 'down', ['north', 'south', 'east']], ['r_5_4', 5, 0, 4, 'down', ['west', 'east']], ['r_4_5', 4, 0, 5, 'down', ['north', 'south']],
  // up the wall (the wall is north of row z 0): a run along it at head height, a corner, and up
  ['w_2_1', 2, 1, 0, 'north', ['west', 'east']], ['w_3_1', 3, 1, 0, 'north', ['west', 'east']], ['w_4_1', 4, 1, 0, 'north', ['west', 'east']],
  ['wall_corner', 5, 1, 0, 'north', ['west', 'up']], ['w_5_2', 5, 2, 0, 'north', ['down', 'up']],
];
const RISER_AT = [6, 0, 4]; // the riser's foot; its girder at (6, y, 5)
const SCENES = { before: 0, after: 10 };

const { cube, group, clip, write } = preview('clockwork_wastes_cable_knot', { 'w:': T });
const outliner = [], LIT = [];
const at = (bx, by, bz) => [bx * 16, by * 16, bz * 16];
const named = (cubes, p) => cubes.map((c) => ({ ...c, name: p + c.name }));
const put = (list, off) => list.map((c) => cube(c, off));
const girder = (name) => [box(name, [4, 0, 4], [12, 16, 12], { all: 'create:block/girder_pole_side', up: null, down: null },
  { uv: { north: [4, 0, 12, 16], south: [4, 0, 12, 16], east: [4, 0, 12, 16], west: [4, 0, 12, 16] } })];
const report = {};
for (const [v, ox] of Object.entries(SCENES)) {
  const kids = [], fights = [];
  // floor: Scorched End Stone, top faces only (rust patches as the city previews scatter them)
  const floor = [];
  for (let x = -1; x <= 8; x++) for (let z = -1; z <= 7; z++) {
    const wx = x + ox;
    floor.push(cube(box(`${v}_floor_${x}_${z}`, [0, -1, 0], [16, 0, 16], { up: (wx * 7 + z * 3) % 5 === 0 ? 'scorched_rust' : 'scorched', all: null }), at(wx, 0, z)));
  }
  kids.push(group(v + '_floor', [8, 0, 8], at(ox, 0, 0), floor));
  // the wall: D75's plain void-alloy plate, 3 high, north of row 0
  const wall = [];
  for (let x = -1; x <= 8; x++) for (let y = 0; y < 3; y++) wall.push(...put([box(`${v}_wall_${x}_${y}`, [0, 0, 0], [16, 16, 16], { all: 'va_bond_s', down: null })], at(x + ox, y, -1)));
  kids.push(group(v + '_wall', [8, 0, 8], at(ox, 0, -1), wall));
  // the cable
  const cable = [], glow = [];
  for (const [name, x, y, z, s, arms] of CELLS) {
    const b = cableBlock(s, arms, v), off = at(x + ox, y, z);
    for (const f of zfights([...b.body, ...b.glow])) fights.push(`${name} (${s}: ${arms.join('+')}): ${f}`);
    cable.push(...put(named(b.body, `${v}_${name}_`), off));
    glow.push(...put(named(b.glow, `${v}_${name}_`), off));
  }
  // the riser: its foot (a side arm west, toward the floor run), two run blocks, the girder 3 high
  {
    const off = at(RISER_AT[0] + ox, 0, RISER_AT[2]);
    const f = riserFoot(['west'], v);
    for (const p of zfights([...f.body, ...f.glow])) fights.push(`riser_foot (girder south: west arm): ${p}`);
    cable.push(...put(named(f.body, `${v}_riser_foot_`), off));
    glow.push(...put(named(f.glow, `${v}_riser_foot_`), off));
    for (const y of [1, 2]) {
      const o = at(RISER_AT[0] + ox, y, RISER_AT[2]);
      cable.push(...put(named(RISER.run, `${v}_riser_run${y}_`), o));
      glow.push(...put(named(RISER.runGlow, `${v}_riser_run${y}_`), o));
    }
    const g = [];
    for (let y = 0; y < 3; y++) g.push(...put(named(girder('girder'), `${v}_g${y}_`), at(RISER_AT[0] + ox, y, RISER_AT[2] + 1)));
    kids.push(group(v + '_girder', [8, 0, 8], at(RISER_AT[0] + ox, 0, RISER_AT[2] + 1), g));
  }
  kids.push(group(v + '_cable', [8, 0, 8], at(ox, 0, 0), [...cable, group('lit_' + v + '_cable', [8, 0, 8], at(ox, 0, 0), glow)]));
  LIT.push('lit_' + v + '_cable');
  outliner.push(group(v, [8, 0, 8], at(ox, 0, 0), kids));
  report[v] = fights;
}

// ---------- clips ----------
const all = (val) => Object.fromEntries(LIT.map((g) => [g, { scale: [[0, val]] }]));
clip('dormant', 2, 'loop', {}, all([0, 0, 0]));
clip('awake', 2, 'loop', {}, all([1, 1, 1]));
write(OUT, outliner);

// ---------- the check ----------
for (const [v, fights] of Object.entries(report)) {
  const knots = fights.filter((f) => /knot/.test(f));
  console.log(`${v.toUpperCase()} (knot ${KNOT_X[v][1] - KNOT_X[v][0]} px): ${fights.length} z-fights, ${knots.length} with the knot`);
  for (const f of fights) console.log('  ' + f);
}
process.exitCode = report.after.some((f) => /knot/.test(f)) ? 1 : 0;
