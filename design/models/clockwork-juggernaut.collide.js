// Self-collision check for the Clockwork Juggernaut: every cube's surface, sampled at 1 px, tested against every other
// cube, in the rest pose (node clockwork-juggernaut.collide.js) or in a pose given as { bone: { rotation, position } }
// functions of time (require it and call clipCollisions). Cubes on the same bone don't count against each other;
// parts built to sit round another (rings, cuffs, wraps, sleeves) are listed in WRAPS.
const J = require('./clockwork-juggernaut.gen.js');
const { jug, groupByName } = J;

const PI = Math.PI;
const M4 = {
  I: () => [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1],
  mul: (a, b) => { const o = new Array(16).fill(0); for (let r = 0; r < 4; r++) for (let c = 0; c < 4; c++) for (let k = 0; k < 4; k++) o[r * 4 + c] += a[r * 4 + k] * b[k * 4 + c]; return o; },
  T: (v) => [1, 0, 0, v[0], 0, 1, 0, v[1], 0, 0, 1, v[2], 0, 0, 0, 1],
  S: (v) => [v[0], 0, 0, 0, 0, v[1], 0, 0, 0, 0, v[2], 0, 0, 0, 0, 1],
  Rx: (d) => { const r = (d * PI) / 180, c = Math.cos(r), s = Math.sin(r); return [1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0, 0, 0, 0, 1]; },
  Ry: (d) => { const r = (d * PI) / 180, c = Math.cos(r), s = Math.sin(r); return [c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0, 0, 0, 0, 1]; },
  Rz: (d) => { const r = (d * PI) / 180, c = Math.cos(r), s = Math.sin(r); return [c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]; },
  apply: (m, p) => [0, 1, 2].map((r) => m[r * 4] * p[0] + m[r * 4 + 1] * p[1] + m[r * 4 + 2] * p[2] + m[r * 4 + 3]),
};
// rotation-and-translation inverse
const inv = (m) => {
  const R = [m[0], m[4], m[8], m[1], m[5], m[9], m[2], m[6], m[10]], t = [m[3], m[7], m[11]];
  const ti = [0, 1, 2].map((r) => -(R[r * 3] * t[0] + R[r * 3 + 1] * t[1] + R[r * 3 + 2] * t[2]));
  return [R[0], R[1], R[2], ti[0], R[3], R[4], R[5], ti[1], R[6], R[7], R[8], ti[2], 0, 0, 0, 1];
};
const add = (a, b) => [a[0] + b[0], a[1] + b[1], a[2] + b[2]];
const rotAbout = (o, r) => M4.mul(M4.T(o), M4.mul(M4.Rz(r[2]), M4.mul(M4.Ry(r[1]), M4.mul(M4.Rx(r[0]), M4.T(o.map((x) => -x))))));

// every cube with its bone chain
const CUBES = [];
(function walk(n, chain) {
  if (n.kind === 'c') { CUBES.push({ c: n, chain, bone: chain[chain.length - 1] }); return; }
  const ch = [...chain, n.name]; n.kids.forEach((k) => walk(k, ch));
})(jug, []);
const PARENT = {};
(function walk(n, parent) { if (n.kind !== 'g') return; PARENT[n.name] = parent; n.kids.forEach((k) => walk(k, n.name)); })(jug, null);
// bones hidden until a clip shows them, and props that never count as clipping (they're held, thrown or vented)
const HIDDEN = new Set(Object.values(groupByName).filter((g) => g.hidden).map((g) => g.name));
const PROPS = new Set([...HIDDEN, 'prop_scale']);

// parts made to sit round or against another part (name fragments, either way round)
const WRAPS = [
  ['ankle_ring', 'shin_'], ['shin_wrap', 'shin_'], ['wrist_cuff', 'forearm_'], ['forearm_guard', 'forearm_'], ['forearm_guard', 'wrist_cuff'],
  ['beard_ring', 'beard_'], ['key_stem', 'key_collar'], ['key_hub', 'key_stem'], ['_teeth', '_'], ['pipe_', 'drum'], ['drum_band', 'drum_'],
  ['shield_', 'shield_'], ['chain_wrap', 'forearm_'], ['chain_wrap', 'chain_wrap'], ['belly_chain', 'belly_chain'], ['chain_end', 'chain_end'],
  ['knee_cap', 'shin_guard'], ['waist_core', 'girdle'], ['hip_ball', 'girdle'], ['shin_wrap', 'calf_'], ['ankle_ring', 'calf_'], ['ankle_ball', 'shin_'], ['ankle_ball', 'calf_'], ['ankle_ball', 'ankle_ring'], ['upper_arm', 'shoulder_drum'], ['upper_arm', 'shoulder_teeth'], ['shoulder_axle', 'shoulder_drum'], ['finger_', 'finger_'], ['thumb', 'thumb'], ['slit_', 'mask'], ['slit_', 'slit_'], ['pawl', 'key_collar'],
];
const wrapped = (a, b) => WRAPS.some(([p, q]) => (a.includes(p) && b.includes(q)) || (a.includes(q) && b.includes(p)));

function cubeMatrix(entry, T, t) {
  let M = M4.I();
  for (const b of entry.chain) {
    const g = groupByName[b], tr = (T && T[b]) || {};
    const rot = add(g.rot, tr.rotation ? tr.rotation(t) : [0, 0, 0]), pos = tr.position ? tr.position(t) : [0, 0, 0], sc = tr.scale ? tr.scale(t) : null;
    M = M4.mul(M, M4.mul(M4.T(pos), rotAbout(g.origin, rot)));
    if (sc && sc.some((v) => v !== 1)) M = M4.mul(M, M4.mul(M4.T(g.origin), M4.mul(M4.S(sc), M4.T(g.origin.map((x) => -x)))));
  }
  const c = entry.c;
  if (c.rotation && c.rotation.some((v) => v)) M = M4.mul(M, rotAbout(c.origin || c.from.map((v, i) => (v + c.to[i]) / 2), c.rotation));
  return M;
}
const scaleOf = (T, b, t) => (T && T[b] && T[b].scale ? T[b].scale(t) : null);
const scaledOut = (entry, T, t) => entry.chain.some((b) => { const sc = scaleOf(T, b, t); return sc ? sc.some((v) => Math.abs(v) < 1e-3) : HIDDEN.has(b); });
const isProp = (entry, skip) => entry.chain.some((b) => PROPS.has(b) || (skip && skip.includes(b)));

// worst penetration of each pair in one pose: [{ a, b, depth }]
function collisions(T, t = 0, minDepth = 0.3, skip = null) {
  const live = CUBES.filter((e) => !scaledOut(e, T, t) && !isProp(e, skip)).map((e) => {
    const M = cubeMatrix(e, T, t), lo = [1e9, 1e9, 1e9], hi = [-1e9, -1e9, -1e9];
    for (const x of [e.c.from[0], e.c.to[0]]) for (const y of [e.c.from[1], e.c.to[1]]) for (const z of [e.c.from[2], e.c.to[2]]) {
      const p = M4.apply(M, [x, y, z]); for (let a = 0; a < 3; a++) { lo[a] = Math.min(lo[a], p[a]); hi[a] = Math.max(hi[a], p[a]); }
    }
    return { ...e, M, Mi: inv(M), lo, hi };
  });
  const out = [];
  for (let i = 0; i < live.length; i++) for (let k = 0; k < live.length; k++) {
    if (i === k) continue;
    const A = live[i], B = live[k];
    if (A.bone === B.bone || wrapped(A.c.name, B.c.name)) continue;
    if ([0, 1, 2].some((a) => A.hi[a] < B.lo[a] || B.hi[a] < A.lo[a])) continue;
    const joint = PARENT[A.bone] === B.bone || PARENT[B.bone] === A.bone;
    let worst = 0;
    const [f, to] = [A.c.from, A.c.to];
    const steps = [0, 1, 2].map((a) => Math.max(1, Math.ceil(to[a] - f[a])));
    for (let a = 0; a < 3; a++) for (const side of [f[a], to[a]]) {
      const u = (a + 1) % 3, v = (a + 2) % 3;
      for (let iu = 0; iu <= steps[u]; iu++) for (let iv = 0; iv <= steps[v]; iv++) {
        const p = [0, 0, 0]; p[a] = side; p[u] = f[u] + ((to[u] - f[u]) * iu) / steps[u]; p[v] = f[v] + ((to[v] - f[v]) * iv) / steps[v];
        const q = M4.apply(B.Mi, M4.apply(A.M, p));
        const d = Math.min(q[0] - B.c.from[0], B.c.to[0] - q[0], q[1] - B.c.from[1], B.c.to[1] - q[1], q[2] - B.c.from[2], B.c.to[2] - q[2]);
        if (d > worst) worst = d;
      }
    }
    if (worst > (joint ? minDepth + 0.7 : minDepth)) out.push({ a: A.c.name, b: B.c.name, depth: +worst.toFixed(2), joint });
  }
  // keep one entry per unordered pair
  const seen = new Map();
  for (const o of out) { const key = [o.a, o.b].sort().join('|'); if (!seen.has(key) || seen.get(key).depth < o.depth) seen.set(key, o); }
  return [...seen.values()].sort((x, y) => y.depth - x.depth);
}
// worst collision over a clip (sampled at fps)
function clipCollisions(T, length, fps = 10, minDepth = 0.3, skip = null) {
  const worst = new Map();
  for (let t = 0; t <= length + 1e-9; t += 1 / fps) for (const o of collisions(T, t, minDepth, skip)) {
    const key = [o.a, o.b].sort().join('|');
    if (!worst.has(key) || worst.get(key).depth < o.depth) worst.set(key, { ...o, t: +t.toFixed(2) });
  }
  return [...worst.values()].sort((x, y) => y.depth - x.depth);
}
// the lowest point (feet on the ground?) and the highest point in a pose
function extent(T, t = 0, only = null) {
  let lo = [1e9, ''], hi = [-1e9, ''];
  for (const e of CUBES) {
    if (scaledOut(e, T, t) || isProp(e) || (only && !only(e))) continue;
    const M = cubeMatrix(e, T, t);
    for (const x of [e.c.from[0], e.c.to[0]]) for (const y of [e.c.from[1], e.c.to[1]]) for (const z of [e.c.from[2], e.c.to[2]]) {
      const p = M4.apply(M, [x, y, z]);
      if (p[1] < lo[0]) lo = [p[1], e.c.name];
      if (p[1] > hi[0]) hi = [p[1], e.c.name];
    }
  }
  return { lo, hi };
}

module.exports = { collisions, clipCollisions, extent, cubeMatrix, CUBES, M4 };
if (require.main === module) {
  const hits = collisions(null, 0);
  console.log(hits.length ? hits.map((h) => `${h.depth.toFixed(2).padStart(6)}  ${h.a}  <->  ${h.b}${h.joint ? '  (joint)' : ''}`).join('\n') : 'no clipping in the rest pose');
  const e = extent(null, 0);
  console.log('lowest', e.lo[0].toFixed(2), e.lo[1], ' highest', e.hi[0].toFixed(2), e.hi[1]);
}
