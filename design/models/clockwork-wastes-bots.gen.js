// Clockwork Wastes bots -> design/models/clockwork-wastes-<bot>.bbmodel (modded_entity, box UV, one painted atlas each).
// To design/briefs/clockwork-wastes-bots.md: the Scuttler, the Sentry and the Custodian, rigged to the brief's Rig
// Requirements, with preview clips (spring motion: each bone a beat behind the one carrying it, as Trade Bot and the
// Juggernaut) and a self-collision check of every clip (the Juggernaut's method).
// node clockwork-wastes-bots.gen.js <scuttler|sentry|custodian|all>
// Full size: 1 px = 1/16 block. Front is -Z, its right is +X. Rotations: rx+ swings a hanging part's lower end forward (-Z)
// and tips a standing part's top back (+Z); ry+ turns the front to -X; rz+ tips the top to -X. Origin: the ground under it.
const fs = require('fs');
const zlib = require('zlib');
const crypto = require('crypto');
const path = require('path');
const uuid = () => crypto.randomUUID();

// ---------- palette (the world brief's; bots are void-alloy, starlight, an ember pupil) ----------
const P = {
  va: ['#16151c', '#1d1c25', '#232130', '#2a2838'], vaHi: ['#34304a'], seamOff: ['#2e2a40'],
  star: ['#7f76a8'], dim: ['#3a3552'], core: ['#b8b0d8'], ember: ['#b5452f'], emberDark: ['#9c3a2a'],
  glass: ['#2a2833'], glint: ['#4c475f'],
  brass: ['#6e5a36', '#7e6832', '#8f7638'], heat: ['#3a2e24'], heatStreak: ['#4a3a2e'], patina: ['#4f6b5e', '#5b7a6c'], copper: ['#9a5038'], soot: ['#1a1714', '#26211c'],
  plug: ['#25232f'],
};
const rgb = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), 255];
function hash(x, y, s) { let h = (x * 374761393 + y * 668265263 + s * 2147483647) | 0; h = Math.imul(h ^ (h >>> 13), 1274126177); return ((h ^ (h >>> 16)) >>> 0) / 4294967295; }
const mot = (mat, x, y) => { const p = P[mat]; if (!p) throw new Error('no mat ' + mat); return p.length === 1 ? p[0] : p[Math.floor(hash(x, y, mat.length * 31) * p.length)]; };
const sides = (f) => f !== 'up' && f !== 'down';
// void-alloy: crisp facets, no mottle. A lit top/left edge on faces big enough to carry it, three bands across the face
// (a faint sheen), the top face lighter, the underside darkest.
// D77: the bands read as stripes, as the city's did (D75, D76), so the side faces' field goes onto the house grain
// (#232130 / #272534 / #2b2939, mostly the middle one, as clockwork-wastes-materials.gen.js's VA_GRAIN). Previewed first:
// BOT_GRAIN false paints the bands (the approved previews and the mod's entity textures unchanged); 'grain' paints the
// grain and keeps the lit top/left edge; 'framed' adds the house's dark bottom/right edge (#16151c) too, so each plate
// keeps its form without a sheen. WASTES_BOT_GRAIN=grain|framed in the environment sets it for the review; with
// WASTES_BOTS_OUT=<dir> the bots are written there as <bot>-<grain|framed|sheen>.bbmodel/.png instead of over the approved
// previews (design/texture-reviews/clockwork-wastes-bot-grain/). The top face, the underside, lenses, glows, ember, scorch
// and brass are unchanged in every setting.
const BOT_GRAIN = 'grain' || process.env.WASTES_BOT_GRAIN || false;
if (BOT_GRAIN && !['grain', 'framed'].includes(BOT_GRAIN)) throw new Error('BOT_GRAIN: grain or framed, not ' + BOT_GRAIN);
const VA_GRAIN_FIELD = ['#232130', '#272534', '#2b2939'];
// the field by the pixel's place on the atlas (seed 551: not a city texture's pattern), so no two faces repeat
const vaGrainAt = (x, y) => { const r = hash(x, y, 551); return r < 0.12 ? VA_GRAIN_FIELD[0] : r > 0.93 ? VA_GRAIN_FIELD[2] : VA_GRAIN_FIELD[1]; };
const vaPaint = (f, i, j, w, h, x = i, y = j) => {
  if (f === 'up') return P.va[3];
  if (f === 'down') return P.va[0];
  if (w >= 3 && h >= 3 && (i === 0 || j === 0)) return P.vaHi[0];
  if (BOT_GRAIN === 'framed' && w >= 3 && h >= 3 && (i === w - 1 || j === h - 1)) return P.va[0];
  if (BOT_GRAIN) return vaGrainAt(x, y);
  const t = (i + j) / (w + h);
  return t < 0.33 ? P.va[3] : t < 0.66 ? P.va[2] : P.va[1];
};
// a seam down the middle of a face (dark until lit; the lit version is its own glow cube)
const seamPaint = (f, i, j, w, h, x, y) => (sides(f) && i === Math.floor(w / 2) && j > 0 && j < h - 1 ? P.seamOff[0] : vaPaint(f, i, j, w, h, x, y));
// a lens face (north), w x w, drawn as an eye (the family face: Trade Bot's catchlight). Corners clear so it reads round.
// Dark: a rim, dark glass, one dull catchlight. Lit: a dim rim, the edge tone only on the ring inside it, one core pixel as
// the catchlight (top-left), and one ember pixel as the pupil when hostile. The light stays small: never a lit tile.
const lensPaint = (lit, pupil = true) => (f, i, j, w, h) => {
  if (f !== 'north') return 'clear';
  const c = (w - 1) / 2, d = Math.hypot(i - c, j - c);
  if (d > c + (w === 3 ? 0.3 : 0.6)) return 'clear';
  const rim = w >= 3 && d > c - 0.5;
  if (!lit) return rim ? P.vaHi[0] : i === Math.floor(c) && j === Math.floor(c) && w <= 3 ? P.glint[0] : (i === Math.floor(c - 0.5) && j === Math.floor(c - 0.5) ? P.glint[0] : P.glass[0]);
  const catchI = w >= 4 ? Math.floor(c) : w === 3 ? 1 : 0, catchJ = w >= 4 ? Math.floor(c) : 0;
  if (i === catchI && j === catchJ) return P.core[0];
  if (pupil && i === Math.ceil(c) && j === Math.ceil(c)) return P.ember[0];
  if (rim) return P.dim[0];
  return w >= 4 ? P.star[0] : P.dim[0];
};

// the extra lens states (D55's clip pass). Each is its own part, hidden inside its housing at rest and slid out over the
// lens by the clips that use it, so the rest pose never shows it.
// ember: the Custodian's wrath ("top and bottom go ember entirely"): a dark-ember ring round an ember centre, corners clear
const emberPaint = (f, i, j, w) => { if (f !== 'north') return 'clear'; const c = (w - 1) / 2, d = Math.hypot(i - c, j - c); if (d > c + (w === 3 ? 0.3 : 0.6)) return 'clear'; return d > c - 0.5 ? P.emberDark[0] : P.ember[0]; };
// blaze: the overload ("blaze to core tone"): the edge tone ring, the core tone inside (a 1 px centre on 3 px lenses, 2 x 2 on 4 px)
const blazePaint = (f, i, j, w) => { if (f !== 'north') return 'clear'; const c = (w - 1) / 2, d = Math.hypot(i - c, j - c); if (d > c + (w === 3 ? 0.3 : 0.6)) return 'clear'; return d > c - 0.5 && w >= 3 ? P.star[0] : P.core[0]; };
// dim: the Sentry's watching lenses: the dim tone, the catchlight pixel in the edge tone
const dimPaint = (f, i, j) => (f !== 'north' ? 'clear' : i === 0 && j === 0 ? P.star[0] : P.dim[0]);
// pupil: one ember pixel (the Sentry's target)
const pupilPaint = (f) => (f !== 'north' ? 'clear' : P.ember[0]);
// crack: the dying Scuttler's lens, a zigzag of #121118 through it
const crackPaint = (f, i, j) => (f !== 'north' ? 'clear' : [[2, 0], [1, 1], [2, 2], [1, 3]].some(([a, b]) => a === i && b === j) ? '#121118' : 'clear');

// ---------- model DSL (the Juggernaut's) ----------
const G = (name, origin, kids, o = {}) => ({ kind: 'g', name, origin, kids, rot: o.rot || [0, 0, 0], hidden: !!o.hidden });
const C = (name, from, to, mat, o = {}) => ({ kind: 'c', name, from, to, mat, ...o });
const VA = (name, from, to, o = {}) => C(name, from, to, 'va', { paint: vaPaint, ...o });
// a stepped disc along an axis (slabs that don't overlap)
function disc(name, c, r, a0, a1, axis, mat, o = {}) {
  const q = (v) => Math.round(v * 4) / 4;
  const h1 = q(r * 0.45), h2 = q(r * 0.8), w2 = q(r * 0.88), w3 = q(r * 0.55);
  const slabs = r < 1.5 ? [[-r, r, -r, r]] : [[-r, r, -h1, h1], [-w2, w2, h1, h2], [-w2, w2, -h2, -h1], [-w3, w3, h2, r], [-w3, w3, -r, -h2]];
  return slabs.map(([u0, u1, v0, v1], k) => {
    let from, to;
    if (axis === 'z') { from = [c[0] + u0, c[1] + v0, a0]; to = [c[0] + u1, c[1] + v1, a1]; }
    if (axis === 'x') { from = [a0, c[1] + v0, c[2] + u0]; to = [a1, c[1] + v1, c[2] + u1]; }
    if (axis === 'y') { from = [c[0] + u0, a0, c[2] + v0]; to = [c[0] + u1, a1, c[2] + v1]; }
    return C(`${name}_${k}`, from, to, mat, o);
  });
}

function build(root, TEX) {
  const cubes = [], groupByName = {};
  (function collect(n) { if (n.kind === 'c') cubes.push(n); else n.kids.forEach(collect); })(root);
  for (const c of cubes) { c.size = [0, 1, 2].map((a) => +(c.to[a] - c.from[a]).toFixed(4)); const [w, h, d] = c.size; c.netW = Math.ceil(2 * (w + d)); c.netH = Math.ceil(d + h); }
  let sx = 0, sy = 0, rowH = 0;
  for (const c of [...cubes].sort((a, b) => b.netH - a.netH || b.netW - a.netW)) { if (sx + c.netW > TEX) { sx = 0; sy += rowH; rowH = 0; } c.uv = [sx, sy]; sx += c.netW; rowH = Math.max(rowH, c.netH); }
  if (sy + rowH > TEX) throw new Error('texture overflow: ' + (sy + rowH));
  const tex = new Uint8Array(TEX * TEX * 4);
  const rects = (c) => { const [w, h, d] = c.size, [u, v] = c.uv; return { east: [u, v + d, d, h], north: [u + d, v + d, w, h], west: [u + d + w, v + d, d, h], south: [u + 2 * d + w, v + d, w, h], up: [u + d, v, w, d], down: [u + d + w, v, w, d] }; };
  for (const c of cubes) {
    const R = rects(c);
    for (const f of Object.keys(R)) {
      const [fx, fy, fw, fh] = R[f], x0 = Math.floor(fx), y0 = Math.floor(fy);
      const W = Math.max(1, Math.ceil(fx + fw) - x0), H = Math.max(1, Math.ceil(fy + fh) - y0);
      for (let j = 0; j < H; j++) for (let i = 0; i < W; i++) {
        const x = x0 + i, y = y0 + j;
        let col = c.paint ? c.paint(f, i, j, W, H, x, y) : null;
        if (col === null || col === undefined) col = mot(c.mat, x, y);
        const o = (y * TEX + x) * 4;
        if (col === 'clear') { tex[o + 3] = 0; continue; }
        const [r, g, b, a] = rgb(col); tex[o] = r; tex[o + 1] = g; tex[o + 2] = b; tex[o + 3] = a;
      }
    }
  }
  const elements = [], groups = [];
  function emit(n) {
    if (n.kind === 'c') {
      n.uuid = uuid();
      const R = rects(n), faces = {};
      for (const f of Object.keys(R)) { const [x, y, w, h] = R[f]; faces[f] = { uv: [x, y, x + w, y + h], texture: 0 }; }
      elements.push({ name: n.name, box_uv: true, render_order: 'default', locked: false, export: true, allow_mirror_modeling: true, from: n.from, to: n.to, autouv: 0, color: 0,
        rotation: n.rotation || [0, 0, 0], origin: n.origin || [(n.from[0] + n.to[0]) / 2, (n.from[1] + n.to[1]) / 2, (n.from[2] + n.to[2]) / 2], uv_offset: n.uv, faces, type: 'cube', uuid: n.uuid, visibility: true });
      return n.uuid;
    }
    n.uuid = uuid();
    if (groupByName[n.name]) throw new Error('duplicate bone ' + n.name);
    groupByName[n.name] = n;
    groups.push({ name: n.name, uuid: n.uuid, export: true, locked: false, visibility: true, origin: n.origin, rotation: n.rot, color: 0, children: [], reset: false, shade: true, mirror_uv: false, autouv: 0, isOpen: false });
    return { uuid: n.uuid, isOpen: n === root, children: n.kids.map(emit) };
  }
  const outliner = [emit(root)];
  return { cubes, groupByName, elements, groups, outliner, tex };
}
function png(w, h, data) {
  const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
  const crc = (buf) => { let c = 0xffffffff; for (const b of buf) c = crcT[(c ^ b) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
  const chunk = (type, body) => { const len = Buffer.alloc(4); len.writeUInt32BE(body.length); const tb = Buffer.concat([Buffer.from(type), body]); const cr = Buffer.alloc(4); cr.writeUInt32BE(crc(tb)); return Buffer.concat([len, tb, cr]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) { raw[y * (w * 4 + 1)] = 0; Buffer.from(data.buffer, y * w * 4, w * 4).copy(raw, y * (w * 4 + 1) + 1); }
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}

// ---------- motion maths (the Juggernaut's) ----------
const FPS = 20, PI = Math.PI;
const r3 = (v) => Math.round(v * 1000) / 1000;
const smooth = (t) => (t <= 0 ? 0 : t >= 1 ? 1 : t * t * (3 - 2 * t));
const seg = (t, a, b) => smooth((t - a) / (b - a));
const springStep = (t, t0, freq = 6, damp = 4) => { if (t < t0) return 0; const u = t - t0, w = 2 * PI * freq / 4; return 1 - Math.exp(-damp * u) * (Math.cos(w * u) + (damp / w) * Math.sin(w * u)); };
const V = (x = 0, y = 0, z = 0) => [x, y, z];
const addV = (...vs) => vs.reduce((a, b) => [a[0] + b[0], a[1] + b[1], a[2] + b[2]], [0, 0, 0]);
const ONE = V(1, 1, 1), NONE = V(0, 0, 0);
// keyed poses with springy transitions; delay[bone] puts each bone a beat behind the one carrying it
function posed(keys, delay = {}, f0 = 4, d0 = 6) {
  const bones = new Set(keys.flatMap(([, p]) => Object.keys(p))), T = {};
  for (const b of bones) {
    T[b] = {};
    for (const ch of new Set(keys.flatMap(([, p]) => Object.keys(p[b] || {})))) {
      const id = ch === 'scale' ? ONE : V(), vals = keys.map(([, p]) => (p[b] && p[b][ch]) || id), dl = delay[b] || 0;
      T[b][ch] = (t) => [0, 1, 2].map((i) => { let x = vals[0][i]; for (let k = 1; k < keys.length; k++) x += (vals[k][i] - vals[k - 1][i]) * springStep(t, keys[k][0] + dl, keys[k][2] || f0, keys[k][3] || d0); return ch === 'scale' && Math.abs(x) < 0.02 ? 0 : x; });
    }
  }
  return T;
}
function mix(...Ts) {
  const o = {};
  for (const T of Ts) for (const b of Object.keys(T)) for (const ch of Object.keys(T[b])) {
    o[b] = o[b] || {}; const prev = o[b][ch], f = T[b][ch];
    o[b][ch] = !prev ? f : ch === 'scale' ? (t) => prev(t).map((v, i) => v * f(t)[i]) : (t) => addV(prev(t), f(t));
  }
  return o;
}
const steps = (list) => { const s = [...list].sort((a, b) => a[0] - b[0]); return (t) => { let v = s[0][1]; for (const [k, x] of s) if (t >= k) v = x; return v; }; };
// a clip mirrored left for right (D67: the left-side hits): each bone's tracks go to its twin on the other side (swap),
// turns about Y and Z and moves along X change sign (mirroring in X keeps turns about X); timing, keys and springs are
// the same functions, so it moves exactly as the original does, on the other side
function mirrored(T, swap) {
  const o = {};
  for (const b of Object.keys(T)) {
    const m = swap(b); if (o[m]) throw new Error('mirrored: two bones map to ' + m);
    o[m] = {};
    for (const ch of Object.keys(T[b])) { const f = T[b][ch];
      o[m][ch] = ch === 'rotation' ? (t) => { const v = f(t); return V(v[0], -v[1], -v[2]); } : ch === 'position' ? (t) => { const v = f(t); return V(-v[0], v[1], v[2]); } : f; }
  }
  return o;
}
// a bone name's twin, by a map of the numbers in its name ('<prefix>_<n>...') and left <-> right
const twin = (nums = {}) => (b) => {
  let m = b.replace(/(left|right)/g, (s) => (s === 'left' ? 'right' : 'left'));
  for (const [prefix, map] of Object.entries(nums)) m = m.replace(new RegExp('^' + prefix + '_(\\d+)'), (s, n) => `${prefix}_${map[n] !== undefined ? map[n] : n}`);
  return m;
};

// ---------- self-collision (the Juggernaut's method): every cube's surface sampled at 1 px against every other cube ----------
const M4 = {
  I: () => [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1],
  mul: (a, b) => { const o = new Array(16).fill(0); for (let r = 0; r < 4; r++) for (let c = 0; c < 4; c++) for (let k = 0; k < 4; k++) o[r * 4 + c] += a[r * 4 + k] * b[k * 4 + c]; return o; },
  T: (v) => [1, 0, 0, v[0], 0, 1, 0, v[1], 0, 0, 1, v[2], 0, 0, 0, 1],
  S: (v) => [v[0], 0, 0, 0, 0, v[1], 0, 0, 0, 0, v[2], 0, 0, 0, 0, 1],
  Rx: (d) => { const r = d * PI / 180, c = Math.cos(r), s = Math.sin(r); return [1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0, 0, 0, 0, 1]; },
  Ry: (d) => { const r = d * PI / 180, c = Math.cos(r), s = Math.sin(r); return [c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0, 0, 0, 0, 1]; },
  Rz: (d) => { const r = d * PI / 180, c = Math.cos(r), s = Math.sin(r); return [c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]; },
  apply: (m, p) => [0, 1, 2].map((r) => m[r * 4] * p[0] + m[r * 4 + 1] * p[1] + m[r * 4 + 2] * p[2] + m[r * 4 + 3]),
};
// the inverse of an affine matrix (rotation, translation and scale: clips scale bones, so a transpose is not enough)
const inv = (m) => { const [a, b, c, d, e, f, g, h, i] = [m[0], m[1], m[2], m[4], m[5], m[6], m[8], m[9], m[10]]; const A = e * i - f * h, B = -(d * i - f * g), Cc = d * h - e * g, det = a * A + b * B + c * Cc;
  const R = [A, -(b * i - c * h), b * f - c * e, B, a * i - c * g, -(a * f - c * d), Cc, -(a * h - b * g), a * e - b * d].map((v) => v / det), t = [m[3], m[7], m[11]];
  const ti = [0, 1, 2].map((r) => -(R[r * 3] * t[0] + R[r * 3 + 1] * t[1] + R[r * 3 + 2] * t[2])); return [R[0], R[1], R[2], ti[0], R[3], R[4], R[5], ti[1], R[6], R[7], R[8], ti[2], 0, 0, 0, 1]; };
const rotAbout = (o, r) => M4.mul(M4.T(o), M4.mul(M4.Rz(r[2]), M4.mul(M4.Ry(r[1]), M4.mul(M4.Rx(r[0]), M4.T(o.map((x) => -x))))));
function checker(root, B, wraps, ignore = () => false) {
  const CUBES = [], PARENT = {};
  (function walk(n, chain) { if (n.kind === 'c') { CUBES.push({ c: n, chain, bone: chain[chain.length - 1] }); return; } const ch = [...chain, n.name]; n.kids.forEach((k) => walk(k, ch)); })(root, []);
  (function walk(n, p) { if (n.kind !== 'g') return; PARENT[n.name] = p; n.kids.forEach((k) => walk(k, n.name)); })(root, null);
  const wrapped = (a, b) => wraps.some(([p, q]) => (a.includes(p) && b.includes(q)) || (a.includes(q) && b.includes(p)));
  const matrix = (e, T, t) => {
    let M = M4.I();
    for (const b of e.chain) {
      const g = B.groupByName[b], tr = (T && T[b]) || {};
      const rot = addV(g.rot, tr.rotation ? tr.rotation(t) : V()), pos = tr.position ? tr.position(t) : V(), sc = tr.scale ? tr.scale(t) : null;
      M = M4.mul(M, M4.mul(M4.T(pos), rotAbout(g.origin, rot)));
      if (sc && sc.some((v) => v !== 1)) M = M4.mul(M, M4.mul(M4.T(g.origin), M4.mul(M4.S(sc), M4.T(g.origin.map((x) => -x)))));
    }
    const c = e.c; if (c.rotation && c.rotation.some((v) => v)) M = M4.mul(M, rotAbout(c.origin || c.from.map((v, i) => (v + c.to[i]) / 2), c.rotation));
    return M;
  };
  let SHED = {};
  const hidden = (e, T, t) => e.chain.some((b) => { if (SHED[b] !== undefined && t >= SHED[b]) return true; const sc = T && T[b] && T[b].scale ? T[b].scale(t) : null; return sc && sc.some((v) => Math.abs(v) < 1e-3); });
  function collisions(T, t = 0, minDepth = 0.3) {
    const live = CUBES.filter((e) => !hidden(e, T, t) && !ignore(e.c.name)).map((e) => { const M = matrix(e, T, t), lo = [1e9, 1e9, 1e9], hi = [-1e9, -1e9, -1e9];
      for (const x of [e.c.from[0], e.c.to[0]]) for (const y of [e.c.from[1], e.c.to[1]]) for (const z of [e.c.from[2], e.c.to[2]]) { const p = M4.apply(M, [x, y, z]); for (let a = 0; a < 3; a++) { lo[a] = Math.min(lo[a], p[a]); hi[a] = Math.max(hi[a], p[a]); } }
      return { ...e, M, Mi: inv(M), lo, hi }; });
    const out = new Map();
    for (const A of live) for (const Bc of live) {
      if (A === Bc || A.bone === Bc.bone || wrapped(A.c.name, Bc.c.name)) continue;
      if ([0, 1, 2].some((a) => A.hi[a] < Bc.lo[a] || Bc.hi[a] < A.lo[a])) continue;
      const joint = PARENT[A.bone] === Bc.bone || PARENT[Bc.bone] === A.bone;
      let worst = 0; const [f, to] = [A.c.from, A.c.to], st = [0, 1, 2].map((a) => Math.max(1, Math.ceil(to[a] - f[a])));
      for (let a = 0; a < 3; a++) for (const side of [f[a], to[a]]) { const u = (a + 1) % 3, v = (a + 2) % 3;
        for (let iu = 0; iu <= st[u]; iu++) for (let iv = 0; iv <= st[v]; iv++) { const p = [0, 0, 0]; p[a] = side; p[u] = f[u] + (to[u] - f[u]) * iu / st[u]; p[v] = f[v] + (to[v] - f[v]) * iv / st[v];
          const q = M4.apply(Bc.Mi, M4.apply(A.M, p)); const d = Math.min(q[0] - Bc.c.from[0], Bc.c.to[0] - q[0], q[1] - Bc.c.from[1], Bc.c.to[1] - q[1], q[2] - Bc.c.from[2], Bc.c.to[2] - q[2]); if (d > worst) worst = d; } }
      if (worst > (joint ? minDepth + 0.7 : minDepth)) { const key = [A.c.name, Bc.c.name].sort().join(' <-> '); if (!out.has(key) || out.get(key) < worst) out.set(key, worst); }
    }
    return out;
  }
  function clip(T, length, fps = 10, shed = {}) {
    SHED = shed;
    const worst = new Map();
    for (let t = 0; t <= length + 1e-9; t += 1 / fps) for (const [k, d] of collisions(T, t)) if (!worst.has(k) || worst.get(k)[0] < d) worst.set(k, [d, +t.toFixed(2)]);
    SHED = {};
    return [...worst.entries()].sort((a, b) => b[1][0] - a[1][0]);
  }
  function lowest(T, t, only) {
    let lo = 1e9;
    for (const e of CUBES) { if (hidden(e, T, t) || (only && !only(e.c.name))) continue; const M = matrix(e, T, t);
      for (const x of [e.c.from[0], e.c.to[0]]) for (const y of [e.c.from[1], e.c.to[1]]) for (const z of [e.c.from[2], e.c.to[2]]) lo = Math.min(lo, M4.apply(M, [x, y, z])[1]); }
    return lo;
  }
  // a cube's world box at time t (for tuning poses)
  function where(T, t, name) { const e = CUBES.find((x) => x.c.name === name), M = matrix(e, T, t), lo = [1e9, 1e9, 1e9], hi = [-1e9, -1e9, -1e9];
    for (const x of [e.c.from[0], e.c.to[0]]) for (const y of [e.c.from[1], e.c.to[1]]) for (const z of [e.c.from[2], e.c.to[2]]) { const p = M4.apply(M, [x, y, z]); for (let a = 0; a < 3; a++) { lo[a] = Math.min(lo[a], p[a]); hi[a] = Math.max(hi[a], p[a]); } }
    return { lo: lo.map((v) => +v.toFixed(2)), hi: hi.map((v) => +v.toFixed(2)) }; }
  // a point of a cube (in its own model coordinates) at time t, in the bot's space
  function pointAt(T, t, name, p) { const e = CUBES.find((x) => x.c.name === name); return M4.apply(matrix(e, T, t), p); }
  return { collisions, clip, lowest, where, pointAt };
}

// ---------- writing a bot ----------
// a clip's tracks with its ground pass: the root lifted or lowered so its lowest ground part sits at y 0 (plus any air)
function grounded(cl, root, CK) {
  let T = cl.T;
  if (cl.ground) {
    const base = T[root.name] && T[root.name].position, cache = new Map();
    const lift = (t) => { const k = r3(t); if (!cache.has(k)) cache.set(k, -CK.lowest({ ...T, [root.name]: { ...(T[root.name] || {}), position: base || (() => V()) } }, t, cl.ground)); return cache.get(k); };
    T = { ...T, [root.name]: { ...(T[root.name] || {}), position: (t) => addV(base ? base(t) : V(), V(0, lift(t) + (cl.air ? cl.air(t) : 0), 0)) } };
  }
  return T;
}
// a bot's rig, checker and grounded clips, for scenes that follow it (the heart hall's tethers)
function prepare(slug) {
  const root = slug === 'custodian' ? custodian() : slug === 'sentry' ? sentry() : scuttler();
  const B = build(root, slug === 'scuttler' ? 64 : 128), CK = checker(root, B, slug === 'custodian' ? cuWraps : slug === 'sentry' ? seWraps : scWraps);
  if (slug !== 'custodian') throw new Error('prepare: only the custodian (its rest pose has no stance offsets)');
  return { root, B, CK, clips: custodianClips().map((cl) => ({ ...cl, T: grounded(cl, root, CK) })) };
}

function writeBot(slug, root, TEX, clips, wraps, opts = {}) {
  const B = build(root, TEX);
  const CK = checker(root, B, wraps, opts.ignore);
  const animations = [], report = [];
  for (const cl of clips) {
    const T = grounded(cl, root, CK);
      const base = T[root.name] && T[root.name].position, cache = new Map();
      const lift = (t) => { const k = r3(t); if (!cache.has(k)) cache.set(k, -CK.lowest({ ...T, [root.name]: { ...(T[root.name] || {}), position: base || (() => V()) } }, t, cl.ground)); return cache.get(k); };
    const animators = {};
    for (const bone of Object.keys(T)) for (const ch of Object.keys(T[bone])) {
      const g = B.groupByName[bone]; if (!g) throw new Error(cl.name + ': no bone ' + bone);
      const n = Math.round(cl.length * FPS), id = ch === 'scale' ? 1 : 0, vals = [];
      for (let k = 0; k <= n; k++) vals.push([k / FPS, T[bone][ch](k / FPS)]);
      if (vals.every(([, v]) => v.every((x) => Math.abs(x - id) < 1e-4))) continue;
      animators[g.uuid] = animators[g.uuid] || { name: bone, type: 'bone', rotation_global: false, quaternion_interpolation: false, keyframes: [] };
      const stepped = ch === 'scale' && cl.stepScale !== false;
      for (const [t, v] of vals) animators[g.uuid].keyframes.push({ channel: ch, data_points: [{ x: String(r3(v[0])), y: String(r3(v[1])), z: String(r3(v[2])) }], uuid: uuid(), time: r3(t), color: -1, interpolation: stepped ? 'step' : 'linear' });
    }
    animations.push({ uuid: uuid(), name: cl.name, loop: cl.loop || 'loop', override: false, length: cl.length, snapping: FPS, selected: false, anim_time_update: '', blend_weight: '', start_delay: '', loop_delay: '', animators });
    const hits = CK.clip(T, cl.length, 10, cl.shed || {});
    report.push(`${cl.name}: ${hits.length ? hits.slice(0, 6).map(([k, [d, t]]) => `${k} ${d.toFixed(2)} px at ${t}s`).join('; ') : 'no clipping'}`);
    // range check for runaway springs
    let maxRot = 0; for (const b of Object.keys(T)) if (T[b].rotation) for (let t = 0; t <= cl.length; t += 0.05) maxRot = Math.max(maxRot, ...T[b].rotation(t).map(Math.abs));
    if (maxRot > (cl.spin ? 400 : 200)) report.push(`  ${cl.name}: a rotation reaches ${maxRot.toFixed(0)} degrees`);
  }
  const rest = CK.collisions(null, 0);
  report.unshift(`rest pose: ${rest.size ? [...rest.entries()].slice(0, 8).map(([k, d]) => `${k} ${d.toFixed(2)}`).join('; ') : 'no clipping'}`);
  // D77's review writes elsewhere (WASTES_BOTS_OUT), so the approved previews the game export reads stay as they are
  const REVIEW = process.env.WASTES_BOTS_OUT, stem = REVIEW ? path.resolve(REVIEW, `${slug}-${BOT_GRAIN || 'sheen'}`) : path.resolve(__dirname, `clockwork-wastes-${slug}`);
  const OUT = stem + '.bbmodel';
  const texPng = png(TEX, TEX, B.tex);
  if (REVIEW) fs.mkdirSync(REVIEW, { recursive: true });
  fs.writeFileSync(stem + '.png', texPng);
  const bb = {
    meta: { format_version: '5.0', model_format: 'modded_entity', box_uv: true },
    name: REVIEW ? path.basename(stem) : `clockwork_wastes_${slug}`, model_identifier: slug, modded_entity_entity_class: '', modded_entity_version: '1.17', modded_entity_flip_y: true,
    visible_box: [2, 4, 0], variable_placeholders: '', variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: TEX, height: TEX }, elements: B.elements, groups: B.groups, outliner: B.outliner,
    textures: [{ path: '', name: `clockwork_wastes_${slug}.png`, folder: '', namespace: '', id: '0', width: TEX, height: TEX, uv_width: TEX, uv_height: TEX, particle: false, render_mode: 'default', render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', visible: true, internal: true, saved: false, uuid: uuid(), source: 'data:image/png;base64,' + texPng.toString('base64') }],
    animations,
  };
  fs.writeFileSync(OUT, JSON.stringify(bb));
  console.error(`wrote ${OUT}: ${B.elements.length} cubes, ${B.groups.length} bones, ${animations.length} clips`);
  console.error(report.map((r) => '  ' + r).join('\n'));
}

// =====================================================================================================================
// SCUTTLER: a low wedge with one big lens at the front and six thin legs. Root at the body's centre, y 4.
// =====================================================================================================================
function scuttler() {
  const lensGroup = G('lens', [0, 4.5, -6.1], [
    C('lens_dark', [-2, 2.5, -7], [2, 6.5, -6], 'glass', { paint: lensPaint(false) }),
    G('lens_crack', [0, 4.5, -4.5], [C('lens_crack_dark', [-2, 2.5, -5], [2, 6.5, -4], 'glass', { paint: crackPaint })]),
  ]);
  const lensLit = G('lens_glow', [0, 4.5, -6.3], [C('lens_lit', [-2, 2.5, -7.25], [2, 6.5, -6.25], 'star', { paint: lensPaint(true) })]);
  const head = G('head', [0, 5, -3], [
    VA('housing', [-2.5, 2, -6], [2.5, 7, -3]), VA('housing_brow', [-2, 7, -5.5], [2, 7.5, -3.5]),
    VA('ring_top', [-2.5, 6.5, -6.5], [2.5, 7, -6]), VA('ring_bottom', [-2.5, 2, -6.5], [2.5, 2.5, -6]), VA('ring_l', [-2.5, 2.5, -6.5], [-2, 6.5, -6]), VA('ring_r', [2, 2.5, -6.5], [2.5, 6.5, -6]),
    lensGroup, lensLit,
    G('lid', [0, 7.5, -5], [VA('lid_plate', [-2, 7.5, -7], [2, 8, -5])]),
  ]);
  const body = [VA('rear', [-3, 2.5, 0], [3, 6.5, 5]), VA('front', [-2.5, 2.5, -3], [2.5, 5.5, 0]), VA('chamfer', [-2.5, 6.5, 0.5], [2.5, 7, 4.5]),
    C('spring_seam', [-0.5, 7, 0.5], [0.5, 7.05, 4.5], 'star')];
  const plate = (s) => G(`plate_${s > 0 ? 'right' : 'left'}`, [0, 7, 2.5], [VA(`plate_${s > 0 ? 'r' : 'l'}`, s > 0 ? [0.25, 7.05, -0.5] : [-3.4, 7.05, -0.5], s > 0 ? [3.4, 7.55, 5.5] : [-0.25, 7.55, 5.5])]);
  // legs: hip knuckle at the body's side; the upper leg reaches out and up, the shin down to a point on the ground
  const legs = [];
  [[-1, -1.5, 'l1'], [-1, 1.5, 'l2'], [-1, 4, 'l3'], [1, -1.5, 'r1'], [1, 1.5, 'r2'], [1, 4, 'r3']].forEach(([s, z, n]) => {
    const hx = s * 3;
    const shin = G(`shin_${n}`, [hx + s * 4, 4.5, z], [C(`shin_${n}_bar`, [hx + s * 4 - 0.5, 0.5, z - 0.5], [hx + s * 4 + 0.5, 4.5, z + 0.5], 'va', { paint: vaPaint }), C(`shin_${n}_tip`, [hx + s * 4 - 0.25, -0.5, z - 0.25], [hx + s * 4 + 0.25, 0.5, z + 0.25], 'va', { paint: vaPaint })]);
    legs.push(G(`leg_${n}`, [hx, 4.5, z], [C(`knuckle_${n}`, [hx - 0.75, 3.75, z - 0.75], [hx + 0.75, 5.25, z + 0.75], 'va', { paint: vaPaint }),
      C(`upper_${n}`, s > 0 ? [hx + 0.75, 4, z - 0.5] : [hx - 4, 4, z - 0.5], s > 0 ? [hx + 4, 5, z + 0.5] : [hx - 0.75, 5, z + 0.5], 'va', { paint: vaPaint }), shin]));
  });
  return G('scuttler', [0, 0, 0], [G('body', [0, 4, 1], [...body, plate(-1), plate(1), head, ...legs])]);
}
// the rest stance: legs up and out, shins splayed to the ground; front legs reach forward, rear legs back
const SC_STANCE = {};
for (const [n, s, yaw] of [['l1', -1, -30], ['l2', -1, 0], ['l3', -1, 28], ['r1', 1, 30], ['r2', 1, 0], ['r3', 1, -28]]) {
  SC_STANCE['leg_' + n] = { rotation: V(0, yaw, s * 28) };
  // with the thigh tipped up 28 degrees, the shin hangs down and out at the same angle: no extra turn needed
  SC_STANCE['shin_' + n] = { rotation: V(0, 0, -s * 4) };
}
const SC_CURLED = { body: { rotation: V(-10, 0, 0) }, head: { rotation: V(35, 0, 0) }, lid: { rotation: V(-40, 0, 0) }, lens_glow: { scale: NONE } };
for (const [n, s] of [['l1', -1], ['l2', -1], ['l3', -1], ['r1', 1], ['r2', 1], ['r3', 1]]) { SC_CURLED['leg_' + n] = { rotation: V(0, 0, s * 70) }; SC_CURLED['shin_' + n] = { rotation: V(0, 0, s * 95) }; }
const SC_DELAY = { body: 0, head: 0.04, lens: 0.08, lens_glow: 0.08, lid: 0.06, plate_left: 0.05, plate_right: 0.05 };
const scWraps = [['ring_', 'lens_'], ['ring_', 'housing'], ['knuckle_', 'rear'], ['knuckle_', 'front'], ['upper_', 'knuckle_'], ['shin_', 'upper_'], ['lid_plate', 'housing'], ['lid_plate', 'ring_'], ['plate_', 'chamfer'], ['plate_', 'rear'], ['spring_seam', 'chamfer'], ['housing', 'front'], ['ring_', 'front'], ['lens_crack', 'housing'], ['lens_crack', 'front']];
function scuttlerClips() {
  const stance = (extra = {}) => { const o = {}; for (const [b, v] of Object.entries(SC_STANCE)) o[b] = { ...v }; for (const [b, v] of Object.entries(extra)) o[b] = { ...(o[b] || {}), ...v }; return o; };
  const cl = [];
  // idle: leg tips jitter, the lens sweeps, the body breathes
  const idle = mix(posed([[0, stance()]]), {
    body: { position: (t) => V(0, 0.3 * Math.sin(t * PI), 0) },
    head: { rotation: (t) => V(4 * Math.sin(t * PI / 2), 25 * Math.sin(t * PI / 2), 0) },
    ...Object.fromEntries(['l1', 'l2', 'l3', 'r1', 'r2', 'r3'].map((n, i) => ['shin_' + n, { rotation: (t) => V(0, 0, 3 * Math.sin(t * 2 * PI * 2 + i * 1.7)) }])),
  });
  cl.push({ name: 'idle', length: 4, T: idle, ground: (n) => n.includes('tip') });
  // walk: tripod gait (l1, r2, l3 then r1, l2, r3), a full cycle in 0.3 s; the body banks a little; the lens settles last
  const gait = {};
  ['l1', 'r2', 'l3', 'r1', 'l2', 'r3'].forEach((n, i) => {
    const ph = i < 3 ? 0 : 0.5, s = n[0] === 'l' ? -1 : 1;
    gait['leg_' + n] = { rotation: (t) => { const u = ((t / 0.3) + ph) % 1; const lift = u < 0.5 ? Math.sin(u * 2 * PI) : 0; return V(0, 22 * Math.cos(u * 2 * PI) * (n.endsWith('2') ? 0.6 : 1), s * 14 * lift); } };
  });
  const walk = mix(posed([[0, stance()]]), gait, { body: { rotation: (t) => V(-4, 0, 3 * Math.sin(t / 0.3 * 2 * PI)), position: (t) => V(0, 0.3 * Math.abs(Math.sin(t / 0.3 * 2 * PI)), 0) }, head: { rotation: (t) => V(0, 6 * Math.sin(t / 0.6 * 2 * PI), 0) } });
  cl.push({ name: 'walk', length: 1.2, T: walk });
  // wake: curled into a ball of plates in the scrap, then the legs unroll one by one, the lens lights, the pupil snaps red
  const order = ['l1', 'r1', 'l2', 'r2', 'l3', 'r3'];
  const wakeKeys = [[0, SC_CURLED]];
  const unroll = (k) => { const p = {}; for (const [b, v] of Object.entries(SC_CURLED)) p[b] = v; order.slice(0, k).forEach((n) => { p['leg_' + n] = SC_STANCE['leg_' + n]; p['shin_' + n] = SC_STANCE['shin_' + n]; }); return p; };
  for (let k = 1; k <= 6; k++) wakeKeys.push([0.6 + k * 0.13, unroll(k), 5, 10]);
  wakeKeys.push([1.6, stance({ body: { rotation: V(0, 0, 0) }, head: { rotation: V(0, 0, 0) }, lid: { rotation: V(10, 0, 0) } }), 5, 6]);
  wakeKeys.push([2.2, stance({ head: { rotation: V(-6, 0, 0) }, lid: { rotation: V(0, 0, 0) } }), 4, 6]);
  const wake = posed(wakeKeys, { ...SC_DELAY, ...Object.fromEntries(['l1', 'l2', 'l3', 'r1', 'r2', 'r3'].map((n) => ['shin_' + n, 0.14])) });
  wake.lens_glow = { scale: steps([[0, NONE], [1.75, V(0.6, 0.6, 1)], [1.85, V(1.2, 1.2, 1)], [2.0, ONE]]) };
  cl.push({ name: 'wake', length: 3, loop: 'hold', T: wake, ground: (n) => n.includes('tip') || n === 'rear' || n === 'front' });
  // leap: back plates lift (the coil, body squashed), spring out (stretched), legs tuck mid-air, land splayed and settle
  const tuck = {}; for (const [n, s] of [['l1', -1], ['l2', -1], ['l3', -1], ['r1', 1], ['r2', 1], ['r3', 1]]) { tuck['leg_' + n] = { rotation: V(0, 0, s * 5) }; tuck['shin_' + n] = { rotation: V(0, 0, -s * 30) }; }
  const leap = posed([[0, stance()], [0.2, stance({ plate_left: { rotation: V(0, 0, -40) }, plate_right: { rotation: V(0, 0, 40) }, body: { scale: V(1.1, 0.8, 1.1), rotation: V(8, 0, 0) } }), 6, 7],
    [0.5, { ...tuck, plate_left: { rotation: V(0, 0, -30) }, plate_right: { rotation: V(0, 0, 30) }, body: { scale: V(0.9, 1.2, 0.9), rotation: V(-12, 0, 0) } }, 7, 7],
    [0.85, stance({ body: { scale: V(1.1, 0.85, 1.1) } }), 7, 6], [1.0, stance(), 5, 6]], SC_DELAY);
  leap.body = { ...leap.body, position: (t) => V(0, t > 0.3 && t < 0.9 ? 10 * Math.sin((t - 0.3) / 0.6 * PI) : 0, t > 0.3 ? -Math.min(1, (t - 0.3) / 0.6) * 0 : 0) };
  cl.push({ name: 'leap', length: 1.4, T: leap, stepScale: false });
  // sees a player: freezes, the lens snaps to them and widens, the pupil turns ember, it crouches
  const sees = posed([[0, stance()], [0.2, stance({ head: { rotation: V(-4, 18, 0) } }), 10, 9], [0.45, stance({ head: { rotation: V(-6, 18, 0) }, lid: { rotation: V(-15, 0, 0) }, body: { rotation: V(6, 0, 0) } }), 6, 6]], SC_DELAY);
  sees.lens_glow = { scale: (t) => (t < 0.35 ? ONE : t < 0.5 ? V(1.25, 1.25, 1) : V(0.8, 0.8, 1)) };
  sees.lens = { scale: (t) => (t < 0.35 ? ONE : t < 0.5 ? V(1.25, 1.25, 1) : V(0.8, 0.8, 1)) };
  cl.push({ name: 'sees_player', length: 1.5, loop: 'hold', T: sees, stepScale: false });
  // a mannerism: wipes its lens with a front leg
  const wipe = posed([[0, stance()], [0.3, stance({ leg_r1: { rotation: V(-70, 40, 85) }, shin_r1: { rotation: V(0, 0, -90) }, head: { rotation: V(0, -10, 8) } }), 5, 6],
    [0.6, stance({ leg_r1: { rotation: V(-70, 50, 85) }, shin_r1: { rotation: V(0, 0, -90) }, head: { rotation: V(0, -10, 8) } }), 8, 6],
    [0.85, stance({ leg_r1: { rotation: V(-70, 30, 85) }, shin_r1: { rotation: V(0, 0, -90) }, head: { rotation: V(0, -10, 8) } }), 8, 6], [1.2, stance(), 4, 6]], SC_DELAY);
  cl.push({ name: 'wipe_lens', length: 1.8, T: wipe });
  cl.push(...scuttlerMoreClips());
  return cl;
}

// =====================================================================================================================
// SENTRY: a mount, a yaw ring and a pitch knuckle, a bud of four petals round a ring of six lenses and a barrel.
// Floor-mounted here (a wall mount is the same model turned). Origin: the mount's centre on the floor.
// =====================================================================================================================
const SE_HEAD = [0, 14, 0];
function sentry() {
  // a petal: a chamfered triangle 8 wide, 10 long, 1 thick, built pointing forward (-Z) from its hinge on the hub's top rim,
  // a seam up its middle; its outer group turns it to its place round the hub (45 + 90k degrees about Z)
  const petal = (k) => {
    const hinge = [0, SE_HEAD[1] + 4, 1];
    const plate = [VA(`petal_${k}_a`, [-4, hinge[1], -3], [4, hinge[1] + 1, 1], {}), VA(`petal_${k}_b`, [-2.75, hinge[1], -6], [2.75, hinge[1] + 1, -3]), VA(`petal_${k}_c`, [-1.5, hinge[1], -8], [1.5, hinge[1] + 1, -6]), VA(`petal_${k}_d`, [-0.5, hinge[1], -9], [0.5, hinge[1] + 1, -8]),
      C(`petal_${k}_seam`, [-0.25, hinge[1] + 1, -8], [0.25, hinge[1] + 1.05, 0.5], 'star')];
    return G(`petal_${k}_place`, [SE_HEAD[0], SE_HEAD[1], 0], [G(`petal_${k}`, hinge, plate)], { rot: [0, 0, 45 + 90 * k] });
  };
  const lenses = [];
  for (let k = 0; k < 6; k++) {
    const a = (k / 6) * 2 * PI + PI / 2, x = Math.cos(a) * 2.75, y = SE_HEAD[1] + Math.sin(a) * 2.75;
    lenses.push(G(`lens_${k + 1}`, [x, y, -3.4], [C(`lens_${k + 1}_dark`, [x - 1, y - 1, -4.25], [x + 1, y + 1, -3.25], 'glass', { paint: lensPaint(false) }),
      G(`lens_${k + 1}_glow`, [x, y, -3.6], [C(`lens_${k + 1}_lit`, [x - 1, y - 1, -4.5], [x + 1, y + 1, -3.5], 'star', { paint: lensPaint(true, false) }),
        G(`lens_${k + 1}_pupil`, [x - 0.5, y - 0.5, -2], [C(`lens_${k + 1}_pupil_lit`, [x - 1, y - 1, -2.5], [x, y, -1.5], 'ember', { paint: pupilPaint })])]),
      G(`lens_${k + 1}_dim`, [x, y, -2], [C(`lens_${k + 1}_dim_lit`, [x - 1, y - 1, -2.5], [x + 1, y + 1, -1.5], 'dim', { paint: dimPaint })])]));
  }
  const head = G('head', [0, SE_HEAD[1], 0], [
    ...disc('hub', SE_HEAD, 4.5, -3.25, 2, 'z', 'va', { paint: vaPaint }),
    G('barrel', [0, SE_HEAD[1], -3], [...disc('barrel_tube', SE_HEAD, 1.5, -8, -3.25, 'z', 'va', { paint: vaPaint }), C('bore_glow', [-1, SE_HEAD[1] - 1, -8.5], [1, SE_HEAD[1] + 1, -7.5], 'star', { paint: lensPaint(true, false) })]),
    ...lenses, petal(0), petal(1), petal(2), petal(3),
  ]);
  const mount = [VA('base', [-5, 0, -5], [5, 2, 5]), C('maint_plate', [1.5, 2, -4.5], [4, 2.1, -2.5], 'brass'),
    ...disc('yaw_ring_base', [0, 0, 0], 3, 2, 3, 'y', 'va', { paint: vaPaint })];
  return G('sentry', [0, 0, 0], [G('mount', [0, 0, 0], [...mount,
    G('yaw', [0, 3, 0], [...disc('yaw_ring', [0, 0, 0], 2.5, 3, 4.5, 'y', 'va', { paint: vaPaint }), VA('neck', [-1, 4.5, -1], [1, 6, 1]),
      G('pitch', [0, 6, 0], [VA('knuckle', [-1.5, 5.5, -1.5], [1.5, 7, 1.5]), VA('stalk', [-0.75, 7, -0.75], [0.75, 12, 0.75]), VA('collar', [-1.25, 12, -1.25], [1.25, 13, 1.25]), head])])])]);
}
// petals: closed = tilted down to meet in a point in front; open = peeled back 120 degrees from closed
// a petal built pointing forward from the hub's top rim: rx- closes its tip down toward the barrel (the bud), rx+ peels it back
const PETAL_CLOSED = -22, PETAL_OPEN = 98;
const petals = (deg) => Object.fromEntries([0, 1, 2, 3].map((k) => [`petal_${k}`, { rotation: V(deg, 0, 0) }]));
const seWraps = [['hub_', 'barrel_tube'], ['lens_', 'hub_'], ['petal_', 'hub_'], ['petal_', 'petal_'], ['bore_glow', 'barrel_tube'], ['bore_glow', 'barrel'], ['neck', 'yaw_ring'], ['knuckle', 'neck'], ['collar', 'knuckle'], ['stalk', 'knuckle'], ['stalk', 'collar'], ['petal_', 'stalk'], ['collar', 'hub_'], ['yaw_ring', 'base'], ['maint_plate', 'base'], ['lens_', 'lens_'], ['petal_', 'lens_'], ['petal_', 'barrel'], ['petal_', 'bore'], ['petal_', 'collar'], ['_dim_lit', 'stalk'], ['_pupil_lit', 'stalk'], ['_dim_lit', 'collar'], ['_pupil_lit', 'collar']];
function sentryClips() {
  const cl = [], lensIds = [1, 2, 3, 4, 5, 6];
  const glow = (f) => Object.fromEntries(lensIds.map((k) => [`lens_${k}_glow`, { scale: f(k) }]));
  // unfold: the bud opens (springy, a flutter), the lenses light in a ripple, then all six turn to the target together
  const unfold = posed([[0, petals(PETAL_CLOSED)], [0.3, petals(PETAL_OPEN), 7, 5], [1.4, { ...petals(PETAL_OPEN), yaw: { rotation: V(0, 30, 0) }, pitch: { rotation: V(-8, 0, 0) } }, 5, 7]], { petal_1: 0.03, petal_2: 0.06, petal_3: 0.09 });
  Object.assign(unfold, glow((k) => steps([[0, NONE], [0.45 + k * 0.08, ONE]])));
  for (const k of lensIds) unfold[`lens_${k}_pupil`] = { position: steps([[0, V()], [1.35, V(0, 0, -2.25)]]) };
  cl.push({ name: 'unfold', length: 2.4, loop: 'hold', T: unfold });
  // watching (to the brief: petals open 100 degrees from closed and breathing, a slow sweep over 6 s, the lenses dim and a slow
  // ripple round the ring, one lens brightening to lit at a time, 0.5 s each)
  const watch = mix(posed([[0, petals(PETAL_CLOSED + 100)]]), {
    yaw: { rotation: (t) => V(0, 40 * Math.sin(t / 6 * 2 * PI), 0) },
    ...Object.fromEntries([0, 1, 2, 3].map((k) => [`petal_${k}`, { rotation: (t) => V(3 * Math.sin(t * PI + k), 0, 0) }])),
  });
  const ring = [1, 6, 5, 4, 3, 2]; // top, upper right, lower right, bottom, lower left, upper left
  for (const k of lensIds) {
    const on = (t) => ring[Math.floor(t / 0.5) % 6] === k;
    watch[`lens_${k}_glow`] = { scale: (t) => (on(t) ? ONE : NONE) };
    watch[`lens_${k}_dim`] = { position: () => V(0, 0, -2), scale: (t) => (on(t) ? NONE : ONE) };
  }
  cl.push({ name: 'watching', length: 6, T: watch });
  // fire: lenses narrow, the bore gathers light (0.6 s), the shot, the head kicks back 2 px and the petals flutter
  const fire = posed([[0, petals(PETAL_OPEN)], [0.1, { ...petals(PETAL_OPEN + 8) }, 8, 6], [0.7, { ...petals(PETAL_OPEN - 10), head: { position: V(0, 0, 1.5) } }, 12, 5], [0.95, petals(PETAL_OPEN), 4, 4]]);
  Object.assign(fire, glow(() => (t) => (t > 0.1 && t < 0.75 ? V(0.7, 0.7, 1) : ONE)));
  for (const k of lensIds) fire[`lens_${k}_pupil`] = { position: () => V(0, 0, -2.25) };
  fire.barrel = { scale: (t) => (t > 0.1 && t < 0.7 ? V(1 + 0.15 * seg(t, 0.1, 0.7), 1 + 0.15 * seg(t, 0.1, 0.7), 1) : ONE) };
  cl.push({ name: 'fire', length: 1.6, T: fire, stepScale: false });
  // fold (the district unfed): the petals close like a flower at dusk (2 s), the lenses going out one by one
  const fold = posed([[0, petals(PETAL_OPEN)], [0.2, petals(PETAL_CLOSED), 1.2, 4]], { petal_1: 0.15, petal_2: 0.3, petal_3: 0.45 });
  Object.assign(fold, glow((k) => steps([[0, ONE], [0.3 + k * 0.25, NONE]])));
  cl.push({ name: 'fold', length: 3, loop: 'hold', T: fold });
  cl.push(...sentryMoreClips());
  return cl;
}

// =====================================================================================================================
// THE CUSTODIAN: 48 px, robed in its makers' shape, three lenses stacked down a tall chamfered head, heavy forearm hammers,
// a robe of eight hanging plates, four tether sockets on its back. Root at the hips, y 22.
// =====================================================================================================================
const HIP_Y = 22;
// the ember and blaze layers rest inside the skull (the bottom lens's 2 px higher, clear of the neck); OUT slides one over its lens
const CU_STOW_Y = { top: 0, mid: 0, bottom: 2 };
const CU_OUT = (name) => V(0, -CU_STOW_Y[name], -3);
function custodian() {
  // the face: three lenses stacked (3, 4, 3 px), each a dark glass and a lit glow on its own bone
  const lens = (name, y, w) => (() => { const sy = CU_STOW_Y[name]; return G(`lens_${name}`, [0, y, -4.6], [C(`lens_${name}_dark`, [-w / 2, y - w / 2, -5], [w / 2, y + w / 2, -4], 'glass', { paint: lensPaint(false) }),
    G(`lens_${name}_glow`, [0, y, -5.1], [C(`lens_${name}_lit`, [-w / 2, y - w / 2, -5.25], [w / 2, y + w / 2, -4.25], 'star', { paint: lensPaint(true) })]),
    ...(name === 'mid' ? [] : [G(`lens_${name}_ember`, [0, y + sy, -1.75], [C(`lens_${name}_ember_lit`, [-w / 2, y + sy - w / 2, -2.25], [w / 2, y + sy + w / 2, -1.25], 'ember', { paint: emberPaint })])]),
    G(`lens_${name}_blaze`, [0, y + sy, -1.75], [C(`lens_${name}_blaze_lit`, [-w / 2, y + sy - w / 2, -2.25], [w / 2, y + sy + w / 2, -1.25], 'core', { paint: blazePaint })])]); })();
  const head = G('head', [0, 39, 0], [
    VA('skull', [-3.5, 39, -3.5], [3.5, 46, 3.5]), VA('mitre', [-3, 46, -3], [3, 48, 3]), VA('mitre_top', [-2, 48, -2], [2, 49, 2]),
    VA('face_plate', [-2.5, 39.5, -4.5], [2.5, 47, -3.5]),
    lens('top', 45, 3), lens('mid', 41.75, 4), lens('bottom', 38.75, 3),
  ]);
  const neck = G('neck', [0, 37, 0], [VA('neck_drum', [-1.5, 36.5, -1.5], [1.5, 39, 1.5]), head]);
  const shoulder = (s) => {
    const side = s > 0 ? 'right' : 'left', x = s * 8.5;
    const fingers = [-1.25, 0, 1.25].map((dz, k) => G(`finger_${side}_${k + 1}`, [x, 13, dz - 0.5], [VA(`finger_${side}_${k + 1}_bar`, [x - 0.5, 10.5, dz - 1], [x + 0.5, 13, dz])]));
    const hand = G(`hand_${side}`, [x, 14.5, 0], [VA(`palm_${side}`, [x - 1.5, 13, -1.5], [x + 1.5, 14.5, 1.5]), ...fingers,
      G(`thumbstuds_${side}`, [x, 13.5, 0], [C(`studs_${side}`, s > 0 ? [x - 1.75, 13.25, -1] : [x + 1.5, 13.25, -1], s > 0 ? [x - 1.5, 14.25, 1] : [x + 1.75, 14.25, 1], 'star')])]);
    // the forearm hammer, hinged at the front of the elbow; a seam round the wrist
    const forearm = G(`forearm_${side}`, [x, 24, -1], [VA(`hammer_${side}`, [x - 3, 15, -3], [x + 3, 24, 3], { paint: seamPaint }), VA(`hammer_cap_${side}`, [x - 2.5, 24, -2.5], [x + 2.5, 24.5, 2.5]),
      C(`wrist_seam_${side}`, [x - 3.25, 15, -3.25], [x + 3.25, 15.75, 3.25], 'star'), hand]);
    const arm = G(`arm_${side}`, [x, 33, 0], [VA(`upper_arm_${side}`, [x - 1, 24, -1], [x + 1, 32, 1]), forearm]);
    arm.rot = [0, 0, s * 5];
    return G(`shoulder_${side}`, [x, 33, 0], [...disc(`shoulder_drum_${side}`, [x, 33, 0], 2, x - 1.5, x + 1.5, 'x', 'va', { paint: vaPaint }),
      VA(`shoulder_cover_${side}`, [x - 2.5, 34, -2.5], [x + 2.5, 35.5, 2.5]), VA(`shoulder_strut_${side}`, s > 0 ? [5.5, 32, -1.5] : [x + 1.5, 32, -1.5], s > 0 ? [x - 1.5, 34, 1.5] : [-5.5, 34, 1.5]), arm]);
  };
  const chest = G('chest', [0, 24, 0], [VA('ribcage', [-6, 25.5, -3.5], [6, 37, 3.5], { paint: seamPaint }), VA('collar', [-4, 37, -3], [4, 37.5, 3]),
    // the pectoral: the broken chain, a link sprung open in the middle (5 x 3)
    G('pectoral', [0, 33, -3.6], [C('pectoral_sigil', [-2.5, 31.5, -4], [2.5, 34.5, -3], 'star', { paint: (f, i, j) => (f !== 'north' ? 'clear' : (['##.##', '#...#', '##.##'][j] || '')[i] === '#' ? P.star[0] : 'clear') })]),
    ...[[30, 0], [27, 1]].flatMap(([y, k]) => [-2.5, 2.5].map((x, i) => G(`socket_${k * 2 + i + 1}`, [x, y, 3.5], [VA(`socket_${k * 2 + i + 1}_plug`, [x - 1, y - 1, 3.5], [x + 1, y + 1, 4.5])]))),
    neck, shoulder(-1), shoulder(1)]);
  const waist = G('waist', [0, HIP_Y + 1, 0], [...disc('waist_drum', [0, HIP_Y + 1.75, 0], 3.5, HIP_Y + 0.5, HIP_Y + 3, 'y', 'va', { paint: vaPaint }), chest]);
  const leg = (s) => {
    const side = s > 0 ? 'right' : 'left', x = s * 1.6;
    const foot = G(`foot_${side}`, [x, 2, 0], [VA(`foot_${side}_sole`, [x - 1.25, 0, -3.5], [x + 1.25, 1.5, 1.5]), ...disc(`ankle_${side}`, [x, 2, 0], 1, x - 0.75, x + 0.75, 'x', 'va', { paint: vaPaint })]);
    const shin = G(`shin_${side}`, [x, 11, 0], [VA(`shin_${side}_bar`, [x - 1, 2.5, -1], [x + 1, 10.5, 1]), ...disc(`knee_${side}`, [x, 11, 0], 1.25, x - 1, x + 1, 'x', 'va', { paint: vaPaint }), foot]);
    return G(`leg_${side}`, [x, HIP_Y, 0], [...disc(`hip_${side}`, [x, HIP_Y, 0], 1.5, x - 1.25, x + 1.25, 'x', 'va', { paint: vaPaint }), VA(`thigh_${side}`, [x - 1, 11.5, -1], [x + 1, HIP_Y - 1, 1]), shin]);
  };
  // the robe: eight plates in two rings hanging from the hip ring, split at front and back so the legs move between them
  const robe = [];
  const plates = [[-90, 0], [-50, 0], [50, 0], [90, 0], [-130, 1], [-165, 1], [165, 1], [130, 1]];
  plates.forEach(([deg, ring], k) => {
    const a = deg * PI / 180, r = ring ? 5.5 : 5, x = Math.sin(a) * r, z = -Math.cos(a) * r;
    // the war's mark on its hem: heat-tint (soot wouldn't show on void-alloy), streaks running up from the bottom edge
    const scorch = (f, i, j, w, h, x, y) => (sides(f) && j >= h - 2 ? P.heat[0] : sides(f) && j >= h - 5 && hash(i, 0, k) < 0.45 ? P.heatStreak[0] : vaPaint(f, i, j, w, h, x, y));
    robe.push(G(`robe_${k + 1}_place`, [0, HIP_Y + 1, 0], [G(`robe_${k + 1}`, [x, HIP_Y + 1, z], [VA(`robe_${k + 1}_plate`, [x - 2, HIP_Y - 17, z - 0.5], [x + 2, HIP_Y + 1, z + 0.5], { paint: scorch })])], { rot: [0, -deg, 0] }));
  });
  // each plate built at its place, then its place group turned so it faces out: correct the plate back to the front frame
  robe.forEach((g, k) => { const [deg] = plates[k]; const a = deg * PI / 180, r = plates[k][1] ? 5.5 : 5; const p = g.kids[0]; p.origin = [0, HIP_Y + 1, -r]; p.kids[0].from = [-2, HIP_Y - 17, -r - 0.5]; p.kids[0].to = [2, HIP_Y + 1, -r + 0.5]; g.rot = [0, -deg, 0]; });
  return G('custodian', [0, 0, 0], [G('hips', [0, HIP_Y, 0], [VA('hip_ring', [-4.5, HIP_Y, -4.5], [4.5, HIP_Y + 1.5, 4.5]), waist, leg(-1), leg(1), ...robe])]);
}
const CU_DELAY = { hips: 0, waist: 0.03, chest: 0.05, neck: 0.08, head: 0.1, shoulder_left: 0.08, shoulder_right: 0.08, arm_left: 0.1, arm_right: 0.1, forearm_left: 0.14, forearm_right: 0.14, hand_left: 0.17, hand_right: 0.17,
  ...Object.fromEntries([1, 2, 3, 4, 5, 6, 7, 8].map((k) => [`robe_${k}`, 0.18 + k * 0.01])) };
const cuWraps = [['lens_', 'face_plate'], ['lens_', 'skull'], ['lens_', 'lens_'], ['face_plate', 'skull'], ['mitre', 'skull'], ['mitre_top', 'mitre'], ['neck_drum', 'skull'], ['neck_drum', 'collar'], ['neck_drum', 'ribcage'], ['collar', 'ribcage'],
  ['shoulder_drum', 'ribcage'], ['shoulder_strut', 'ribcage'], ['shoulder_strut', 'shoulder_drum'], ['shoulder_strut', 'shoulder_cover'], ['shoulder_drum', 'upper_arm'], ['shoulder_cover', 'shoulder_drum'], ['shoulder_cover', 'ribcage'], ['upper_arm', 'hammer'], ['hammer_cap', 'hammer'], ['hammer_cap', 'upper_arm'], ['wrist_seam', 'hammer'], ['palm', 'hammer'], ['palm', 'wrist_seam'],
  ['finger_', 'palm'], ['finger_', 'finger_'], ['studs_', 'palm'], ['pectoral', 'ribcage'], ['socket_', 'ribcage'], ['waist_drum', 'ribcage'], ['waist_drum', 'hip_ring'], ['hip_', 'hip_ring'], ['hip_', 'thigh'], ['thigh', 'hip_ring'],
  ['knee_', 'thigh'], ['knee_', 'shin_'], ['ankle_', 'shin_'], ['ankle_', 'foot_'], ['robe_', 'hip_ring'], ['robe_', 'robe_'], ['hip_', 'waist_drum'], ['lens_', 'mitre']];
// poses (deltas)
const KNEEL = { leg_right: { rotation: V(-10, 0, 0) }, shin_right: { rotation: V(-80, 0, 0) }, foot_right: { rotation: V(0, 0, 0) },
  leg_left: { rotation: V(70, 0, 0) }, shin_left: { rotation: V(-70, 0, 0) }, foot_left: { rotation: V(0, 0, 0) },
  waist: { rotation: V(-12, 0, 0) }, neck: { rotation: V(-10, 0, 0) }, head: { rotation: V(-10, 0, 0) },
  arm_left: { rotation: V(30, 0, -16) }, arm_right: { rotation: V(10, 0, 4) }, forearm_left: { rotation: V(-10, 0, 0) }, forearm_right: { rotation: V(20, 0, 0) },
  robe_1: { rotation: V(2, 0, 0) }, robe_2: { rotation: V(30, 0, 0) }, robe_3: { rotation: V(16, 0, 0) }, robe_4: { rotation: V(10, 0, 0) },
  robe_5: { rotation: V(16, 0, 0) }, robe_6: { rotation: V(40, 0, 0) }, robe_7: { rotation: V(40, 0, 0) }, robe_8: { rotation: V(16, 0, 0) } };
const LENSES_OFF = { lens_top_glow: { scale: NONE }, lens_mid_glow: { scale: NONE }, lens_bottom_glow: { scale: NONE }, thumbstuds_left: { scale: NONE }, thumbstuds_right: { scale: NONE } };
const STUDS_OFF = { thumbstuds_left: { scale: NONE }, thumbstuds_right: { scale: NONE } };
function custodianClips() {
  const cl = [], FEET = (n) => n.startsWith('foot_');
  const KNEELERS = (n) => n.startsWith('foot_') || n.startsWith('knee_') || n.startsWith('shin_');
  // idle: upright, hands open, the shoulders rise and fall slowly (a pump, not a breath), the plates sway a beat behind
  const idle = mix(posed([[0, STUDS_OFF]]), {
    chest: { rotation: (t) => V(1.5 * Math.sin(t / 3 * 2 * PI), 0, 0) },
    shoulder_left: { position: (t) => V(0, 0.4 * Math.sin(t / 3 * 2 * PI), 0) }, shoulder_right: { position: (t) => V(0, 0.4 * Math.sin(t / 3 * 2 * PI), 0) },
    head: { rotation: (t) => V(0, 6 * Math.sin(t / 6 * 2 * PI), 0) },
    ...Object.fromEntries([1, 2, 3, 4, 5, 6, 7, 8].map((k) => [`robe_${k}`, { rotation: (t) => V(2 * Math.sin(t / 3 * 2 * PI - 0.6 - k * 0.15), 0, 0) }])),
  });
  cl.push({ name: 'idle', length: 6, T: idle, ground: FEET });
  // the bow: formal, 25 degrees from the waist, a beat held, then up
  const bow = posed([[0, STUDS_OFF], [0.2, { ...STUDS_OFF, waist: { rotation: V(-25, 0, 0) }, neck: { rotation: V(-8, 0, 0) }, arm_left: { rotation: V(6, 0, 0) }, arm_right: { rotation: V(6, 0, 0) }, robe_2: { rotation: V(10, 0, 0) }, robe_3: { rotation: V(10, 0, 0) } }, 2.5, 5],
    [1.4, STUDS_OFF, 2.5, 5]], CU_DELAY);
  cl.push({ name: 'bow', length: 2.6, loop: 'hold', T: bow, ground: FEET });
  // the slam: forearms up over its head (1 s, lenses narrowing), down together, a held bend, then it straightens
  const UP = { ...STUDS_OFF, arm_left: { rotation: V(160, 0, 22) }, arm_right: { rotation: V(160, 0, -22) }, forearm_left: { rotation: V(25, 0, 0) }, forearm_right: { rotation: V(25, 0, 0) }, waist: { rotation: V(8, 0, 0) }, head: { rotation: V(6, 0, 0) },
    lens_mid_glow: { scale: V(0.8, 0.8, 1) } };
  const DOWN = { ...STUDS_OFF, arm_left: { rotation: V(62, 0, 4) }, arm_right: { rotation: V(62, 0, -4) }, forearm_left: { rotation: V(12, 0, 0) }, forearm_right: { rotation: V(12, 0, 0) }, waist: { rotation: V(-22, 0, 0) }, neck: { rotation: V(-6, 0, 0) },
    leg_left: { rotation: V(12, 0, 0) }, shin_left: { rotation: V(-10, 0, 0) }, leg_right: { rotation: V(-10, 0, 0) }, robe_1: { rotation: V(14, 0, 0) }, robe_2: { rotation: V(28, 0, 0) }, robe_3: { rotation: V(16, 0, 0) }, robe_4: { rotation: V(10, 0, 0) } };
  const slam = posed([[0, STUDS_OFF], [0.1, UP, 1.6, 5], [1.1, DOWN, 9, 7], [1.7, DOWN, 4, 6], [2.0, STUDS_OFF, 2, 5]], CU_DELAY);
  cl.push({ name: 'slam', length: 3.2, T: slam, ground: FEET, stepScale: false });
  // waking (the heart's doors open): kneeling and dark, the lenses light top to bottom, the head lifts, it rises, then bows
  const wake = posed([[0, { ...KNEEL, ...LENSES_OFF }], [1.6, { ...KNEEL, ...STUDS_OFF, head: { rotation: V(0, 0, 0) }, neck: { rotation: V(0, 0, 0) } }, 2, 5], [2.0, STUDS_OFF, 1.2, 4]], CU_DELAY);
  wake.lens_top_glow = { scale: steps([[0, NONE], [0.4, ONE]]) }; wake.lens_mid_glow = { scale: steps([[0, NONE], [0.9, ONE]]) }; wake.lens_bottom_glow = { scale: steps([[0, NONE], [1.4, ONE]]) };
  cl.push({ name: 'waking', length: 4.5, loop: 'hold', T: wake, ground: KNEELERS });
  // death: it stops, turns to the vault (behind it), kneels slowly and bows; the lenses go out top to bottom; the plates drop
  const TURNED = { hips: { rotation: V(0, 180, 0) } };
  const death = posed([[0, STUDS_OFF], [0.5, { ...STUDS_OFF, ...TURNED }, 1.2, 5], [1.2, { ...KNEEL, ...STUDS_OFF, ...TURNED, arm_left: { rotation: V(30, 0, -20) } }, 0.9, 5]], CU_DELAY);
  death.lens_top_glow = { scale: steps([[0, ONE], [3.5, NONE]]) }; death.lens_mid_glow = { scale: steps([[0, ONE], [3.8, NONE]]) }; death.lens_bottom_glow = { scale: steps([[0, ONE], [4.1, NONE]]) };
  for (let k = 1; k <= 8; k++) {
    const t0 = 4.4 + k * 0.18;
    death[`robe_${k}`] = { ...(death[`robe_${k}`] || {}), position: (t) => { const u = t < t0 ? 0 : t - t0; return V(0, -Math.min(14, 60 * u * u), -Math.min(9, 30 * u)); }, rotation: (t) => addV(KNEEL[`robe_${k}`] ? KNEEL[`robe_${k}`].rotation : V(), V(t < t0 ? 0 : Math.min(55, 120 * (t - t0)), 0, 0)) };
  }
  cl.push({ name: 'death', length: 6.5, loop: 'hold', T: death, ground: KNEELERS, shed: Object.fromEntries([1, 2, 3, 4, 5, 6, 7, 8].map((k) => [`robe_${k}`, 4.4 + k * 0.18])) });
  cl.push(...custodianFightClips());
  return cl;
}

// the fight's clips (brief, Motion Set: the Custodian's locomotion, attacks and reactions); poses are deltas, each bone a
// beat behind the one carrying it (CU_DELAY), springs between keys
function custodianFightClips() {
  const cl = [], FEET = (n) => n.startsWith('foot_');
  const KNEELERS = (n) => n.startsWith('foot_') || n.startsWith('knee_') || n.startsWith('shin_');
  const LENS = ['lens_top_glow', 'lens_mid_glow', 'lens_bottom_glow'];
  const lensScale = (f) => Object.fromEntries(LENS.map((b, i) => [b, { scale: f(i) }]));
  // walk: measured, a step each 0.8 s; the knee bends as the leg swings through, the sole stays level, the arms swing
  // against the legs, and the robe plates swing a beat behind each step (front plates pushed ahead of the thigh)
  const WT = 1.6, w = (t) => (t / WT) * 2 * PI, lag = 0.5;
  const legL = (t) => 18 * Math.sin(w(t)), shinL = (t) => -30 * Math.max(0, Math.cos(w(t)));
  const legR = (t) => -18 * Math.sin(w(t)), shinR = (t) => -30 * Math.max(0, -Math.cos(w(t)));
  const fwdL = (t) => Math.max(0, Math.sin(w(t) - lag)), fwdR = (t) => Math.max(0, -Math.sin(w(t) - lag));
  const walk = mix(posed([[0, STUDS_OFF]]), {
    leg_left: { rotation: (t) => V(legL(t), 0, 0) }, leg_right: { rotation: (t) => V(legR(t), 0, 0) },
    shin_left: { rotation: (t) => V(shinL(t), 0, 0) }, shin_right: { rotation: (t) => V(shinR(t), 0, 0) },
    foot_left: { rotation: (t) => V(-(legL(t) + shinL(t)), 0, 0) }, foot_right: { rotation: (t) => V(-(legR(t) + shinR(t)), 0, 0) },
    arm_left: { rotation: (t) => V(-8 * Math.sin(w(t)), 0, 0) }, arm_right: { rotation: (t) => V(8 * Math.sin(w(t)), 0, 0) },
    chest: { rotation: (t) => V(-0.6 - 0.6 * Math.cos(2 * w(t) - 3.84), 3 * Math.sin(w(t)), 0) }, head: { rotation: (t) => V(0, -2.5 * Math.sin(w(t - 0.1)), 0) },
    // weight into every step: a beat after each foot lands, the shoulders sink and the chest nods, then they rise
    shoulder_left: { position: (t) => V(0, -0.2 - 0.2 * Math.cos(2 * w(t) - 3.84), 0) }, shoulder_right: { position: (t) => V(0, -0.2 - 0.2 * Math.cos(2 * w(t) - 3.84), 0) },
    robe_1: { rotation: (t) => V(3 + 4 * Math.sin(w(t) - lag), 0, 0) }, robe_4: { rotation: (t) => V(3 - 4 * Math.sin(w(t) - lag), 0, 0) },
    robe_2: { rotation: (t) => V(6 + 26 * fwdL(t), 0, 0) }, robe_3: { rotation: (t) => V(6 + 26 * fwdR(t), 0, 0) },
    robe_5: { rotation: (t) => V(4 + 16 * fwdR(t), 0, 0) }, robe_6: { rotation: (t) => V(4 + 10 * fwdR(t), 0, 0) },
    robe_7: { rotation: (t) => V(4 + 10 * fwdL(t), 0, 0) }, robe_8: { rotation: (t) => V(4 + 16 * fwdL(t), 0, 0) },
  });
  cl.push({ name: 'walk', length: WT, T: walk, ground: FEET });
  // shockwave ring: plants its feet, the robe plates flare out (0.6 s), a stamp (the ring runs out at 0.8 s), the plates fall back
  const FLARE = Object.fromEntries([1, 2, 3, 4, 5, 6, 7, 8].map((k) => [`robe_${k}`, { rotation: V(40, 0, 0) }]));
  const BRACE = { ...STUDS_OFF, ...FLARE, arm_left: { rotation: V(0, 0, -30) }, arm_right: { rotation: V(0, 0, 30) }, waist: { rotation: V(-4, 0, 0) } };
  const shock = posed([[0, STUDS_OFF], [0.0, BRACE, 3, 6],
    [0.45, { ...BRACE, leg_right: { rotation: V(28, 0, 0) }, shin_right: { rotation: V(-40, 0, 0) }, foot_right: { rotation: V(12, 0, 0) } }, 6, 7],
    [0.72, { ...BRACE, waist: { rotation: V(-8, 0, 0) } }, 14, 8],
    [1.15, STUDS_OFF, 2.5, 5]], CU_DELAY);
  cl.push({ name: 'shockwave', length: 2.4, T: shock, ground: FEET });
  // bolt volley: one hand raised palm out (0.4 s), its thumb-studs light (0.5 s), three bolts 0.2 s apart (0.9, 1.1, 1.3 s),
  // the hand kicking back with each
  const AIM = { ...STUDS_OFF, arm_right: { rotation: V(82, 0, -4) }, forearm_right: { rotation: V(6, 0, 0) }, hand_right: { rotation: V(-48, 0, 0) },
    waist: { rotation: V(2, -8, 0) }, head: { rotation: V(-2, 8, 0) } };
  const KICK = { ...AIM, arm_right: { rotation: V(70, 0, -4) }, hand_right: { rotation: V(-38, 0, 0) }, chest: { rotation: V(3, 0, 0) } };
  const volley = posed([[0, STUDS_OFF], [0.0, AIM, 3, 6], [0.9, KICK, 14, 7], [0.98, AIM, 6, 6], [1.1, KICK, 14, 7], [1.18, AIM, 6, 6],
    [1.3, KICK, 14, 7], [1.38, AIM, 6, 6], [1.7, STUDS_OFF, 2.5, 5]], CU_DELAY);
  volley.thumbstuds_right = { scale: steps([[0, NONE], [0.4, ONE], [1.5, NONE]]) };
  cl.push({ name: 'bolt_volley', length: 2.6, T: volley, ground: FEET });
  // chain arc (overload only): spreads its arms (0.4 s), lightning crawls over its robe (0.4 s: particles), the arc leaps at 0.8 s
  const SPREAD = { ...STUDS_OFF, arm_left: { rotation: V(10, 0, -62) }, arm_right: { rotation: V(10, 0, 62) }, forearm_left: { rotation: V(10, 0, 0) },
    forearm_right: { rotation: V(10, 0, 0) }, waist: { rotation: V(5, 0, 0) }, head: { rotation: V(6, 0, 0) } };
  const arc = posed([[0, STUDS_OFF], [0.0, SPREAD, 3, 6], [0.8, { ...SPREAD, waist: { rotation: V(-6, 0, 0) }, head: { rotation: V(-4, 0, 0) } }, 12, 7],
    [1.2, STUDS_OFF, 2.5, 5]], CU_DELAY);
  arc.chest = { ...(arc.chest || {}), position: (t) => V(t > 0.4 && t < 0.8 ? 0.15 * Math.sin(t * 90) : 0, 0, 0) };
  Object.assign(arc, lensScale(() => (t) => (t > 0.4 && t < 0.8 ? V(1.2, 1.2, 1) : ONE)));
  cl.push({ name: 'chain_arc', length: 2.2, T: arc, ground: FEET, stepScale: false });
  // overload (once, at half health): arches back, the plates fan out, the lenses blaze and flicker
  const ARCH = { ...STUDS_OFF, ...FLARE, waist: { rotation: V(16, 0, 0) }, neck: { rotation: V(8, 0, 0) }, head: { rotation: V(10, 0, 0) },
    arm_left: { rotation: V(-6, 0, -28) }, arm_right: { rotation: V(-6, 0, 28) }, forearm_left: { rotation: V(8, 0, 0) }, forearm_right: { rotation: V(8, 0, 0) } };
  const over = posed([[0, STUDS_OFF], [0.1, ARCH, 2.5, 4], [1.7, STUDS_OFF, 2, 5]], CU_DELAY);
  // the lenses blaze to the core tone (the blaze layers out) and flicker back to lit, each on its own beat
  for (const [i, n] of ['top', 'mid', 'bottom'].entries()) {
    const blazing = (t) => t > 0.3 && t < 1.85 && Math.floor(t * 12 + i * 2) % 4 !== 0;
    over[`lens_${n}_blaze`] = { position: (t) => (t > 0.28 && t < 1.87 ? CU_OUT(n) : V()), scale: (t) => (blazing(t) ? ONE : NONE) };
    over[`lens_${n}_glow`] = { scale: (t) => (blazing(t) ? NONE : ONE) };
  }
  cl.push({ name: 'overload', length: 2.8, T: over, ground: FEET, stepScale: false });
  // wrath (a pylon broken): the head snaps to the breaker, the lenses narrow, it leans 10 degrees forward and steps toward them
  const LEAN = { ...STUDS_OFF, waist: { rotation: V(-10, 0, 0) }, head: { rotation: V(-4, 0, 0) } };
  const wrath = posed([[0, STUDS_OFF], [0.0, { ...STUDS_OFF, head: { rotation: V(7, 0, 0) }, neck: { rotation: V(3, 0, 0) } }, 16, 9], [0.25, { ...LEAN, leg_right: { rotation: V(22, 0, 0) }, shin_right: { rotation: V(-20, 0, 0) }, foot_right: { rotation: V(-2, 0, 0) },
    robe_3: { rotation: V(28, 0, 0) }, robe_2: { rotation: V(10, 0, 0) } }, 6, 7], [0.6, LEAN, 6, 7], [1.3, STUDS_OFF, 2.5, 5]], CU_DELAY);
  // the ember layers out over the top and bottom lenses, narrowed (0.8); the middle lens narrows to 0.7
  Object.assign(wrath, cuLayer('ember', ['top', 'bottom'], (t) => t > 0.05 && t < 1.45));
  wrath.lens_top_ember.scale = wrath.lens_bottom_ember.scale = (t) => (t > 0.05 && t < 1.45 ? V(0.8, 0.8, 1) : ONE);
  wrath.lens_mid_glow = { scale: (t) => (t > 0.05 && t < 1.45 ? V(0.7, 0.7, 1) : ONE) };
  cl.push({ name: 'wrath', length: 2.0, T: wrath, ground: FEET, stepScale: false });
  // shield down: staggers back a step, looks at its hands (a 0.6 s hold), the lenses flicker then steady, and it straightens
  const HANDS = { ...STUDS_OFF, arm_left: { rotation: V(48, 0, 6) }, arm_right: { rotation: V(48, 0, -6) }, forearm_left: { rotation: V(42, 0, 0) },
    forearm_right: { rotation: V(42, 0, 0) }, hand_left: { rotation: V(-20, 0, 0) }, hand_right: { rotation: V(-20, 0, 0) }, neck: { rotation: V(-8, 0, 0) }, head: { rotation: V(-10, 0, 0) } };
  const down = posed([[0, STUDS_OFF], [0.0, { ...STUDS_OFF, waist: { rotation: V(8, 0, 0) }, leg_left: { rotation: V(-14, 0, 0) }, shin_left: { rotation: V(-10, 0, 0) },
    foot_left: { rotation: V(24, 0, 0) }, robe_5: { rotation: V(20, 0, 0) }, robe_6: { rotation: V(34, 0, 0) } }, 8, 7], [0.4, HANDS, 3, 6], [1.25, STUDS_OFF, 2.5, 5]],
    { ...CU_DELAY, robe_5: 0, robe_6: 0 });
  Object.assign(down, lensScale((i) => (t) => (t < 0.5 && Math.floor(t * 16 + i) % 2 === 0 ? NONE : ONE)));
  cl.push({ name: 'shield_down', length: 2.3, T: down, ground: FEET, stepScale: false });
  // reset: back on its dais it kneels, bows its head, and the lenses go out (the dormant pose, the waking clip's start)
  // the plates lift clear first (no lag on them here), then the legs fold under
  const PLATES_UP = Object.fromEntries([1, 2, 3, 4, 5, 6, 7, 8].map((k) => [`robe_${k}`, KNEEL[`robe_${k}`]]));
  const kneel = posed([[0, STUDS_OFF], [0.0, { ...STUDS_OFF, ...PLATES_UP, arm_left: { rotation: V(0, 0, -16) } }, 3, 6], [0.3, { ...KNEEL, ...STUDS_OFF }, 0.9, 5]],
    { ...CU_DELAY, ...Object.fromEntries([1, 2, 3, 4, 5, 6, 7, 8].map((k) => [`robe_${k}`, 0])) });
  Object.assign(kneel, lensScale((i) => steps([[0, ONE], [2.0 + i * 0.25, NONE]])));
  cl.push({ name: 'kneel', length: 3.0, loop: 'hold', T: kneel, ground: KNEELERS });
  cl.push(...custodianMoreClips());
  return cl;
}

// =====================================================================================================================
// THE CLIP PASS (spec D55): every expression, reaction and mannerism in the bots brief that had no clip yet
// =====================================================================================================================
const env = (t, a, b) => (t <= a || t >= b ? 0 : Math.sin(PI * (t - a) / (b - a)));
// ---------- Scuttler ----------
const SC_LEGS = ['l1', 'l2', 'l3', 'r1', 'r2', 'r3'];
const scSide = (n) => (n[0] === 'l' ? -1 : 1);
// the stance's forward yaw per leg pair (front legs reach forward, rear legs back); ry = side x fwd
const SC_FWD = { 1: 30, 2: 0, 3: -28 };
const scStance = (extra = {}) => { const o = {}; for (const [b, v] of Object.entries(SC_STANCE)) o[b] = { ...v }; for (const [b, v] of Object.entries(extra)) o[b] = { ...(o[b] || {}), ...v }; return o; };
// one leg: up = the thigh's lift above level (stance 28), fwd = its forward yaw, shinIn = the shin's turn in under the body (stance 4)
const scLeg = (n, up = 28, fwd = SC_FWD[n[1]], shinIn = 4, rx = 0) => { const s = scSide(n); return { ['leg_' + n]: { rotation: V(rx, s * fwd, s * up) }, ['shin_' + n]: { rotation: V(0, 0, -s * shinIn) } }; };
const scLegs = (f) => SC_LEGS.reduce((o, n) => ({ ...o, ...f(n) }), {});
// a pose seen in a mirror: left and right legs and plates swap, yaw and roll change sign
const scMirror = (p) => { const o = {};
  for (const [b, v] of Object.entries(p)) {
    const nb = b.replace(/_([lr])(\d)$/, (m, sd, k) => `_${sd === 'l' ? 'r' : 'l'}${k}`).replace(/_left$/, '_TMP').replace(/_right$/, '_left').replace(/_TMP$/, '_right');
    o[nb] = {}; for (const [ch, val] of Object.entries(v)) o[nb][ch] = ch === 'rotation' ? V(val[0], -val[1], -val[2]) : ch === 'position' ? V(-val[0], val[1], val[2]) : val;
  }
  return o; };
const APERTURE = (a) => ({ lens: { scale: V(a, a, 1) }, lens_glow: { scale: V(a, a, 1) } });
// the back plates lift (both rotate up about the spine: the left plate rz-, the right rz+)
const PLATES_UP = (d) => ({ plate_left: { rotation: V(0, 0, -d) }, plate_right: { rotation: V(0, 0, d) } });
// the hostile face: aperture narrow, the lid slanted down at the inside, crouched, back plates lifted
const SC_HOSTILE = scStance({ ...APERTURE(0.7), lid: { rotation: V(-18, 0, 10) }, head: { rotation: V(4, 0, 0) }, body: { rotation: V(-4, 0, 0) }, ...PLATES_UP(25), ...scLegs((n) => scLeg(n, 16, SC_FWD[n[1]] * 1.15, 0)) });
const TIPS = (n) => n.includes('tip');
const ALL = () => true;
// the tripod gait (l1, r2, l3 then r1, l2, r3), as the walk's: period, yaw swing, lift
const scGait = (period, yawAmp, liftAmp) => Object.fromEntries(['l1', 'r2', 'l3', 'r1', 'l2', 'r3'].map((n, i) => {
  const ph = i < 3 ? 0 : 0.5, s = scSide(n);
  return ['leg_' + n, { rotation: (t) => { const u = ((t / period) + ph) % 1; const lift = u < 0.5 ? Math.sin(u * 2 * PI) : 0; return V(0, yawAmp * Math.cos(u * 2 * PI) * (n.endsWith('2') ? 0.6 : 1), s * liftAmp * lift); } }];
}));
// tuned with the checker: the climbing body's place (its planted tips on the wall plane z -6, its lowest point on the
// floor), the dead legs, the picking leg
const CLIMB = { lift: 5.5, z: -3.75 };
const DEATH_LEG = { up: 6, shinIn: 40 };
const PICK = { raise: [52, 62, 26], poke: [30, 62, 12] };
function scuttlerMoreClips() {
  const cl = [];
  // run: the tripod gait at its run speed (a cycle in 0.3 s), longer strides, the body low and pitched forward, banking
  const run = mix(posed([[0, scStance({ body: { rotation: V(-7, 0, 0) }, head: { rotation: V(7, 0, 0) }, lid: { rotation: V(-6, 0, 0) } })]]), scGait(0.3, 30, 20),
    { body: { rotation: (t) => V(0, 0, 5 * Math.sin(t / 0.3 * 2 * PI)) }, head: { rotation: (t) => V(0, 4 * Math.sin(t / 0.6 * 2 * PI - 0.8), 0) } });
  cl.push({ name: 'run', length: 0.6, T: run, ground: TIPS, air: (t) => 0.5 * Math.abs(Math.sin(t / 0.3 * 2 * PI)) });
  // climb: turned to face up the wall in front of it, clinging with its legs splayed wider and flatter, climbing in the
  // tripod gait; the lens sweeps
  const CLING = scStance({ body: { rotation: V(90, 0, 0) }, head: { rotation: V(-8, 0, 0) }, ...scLegs((n) => scLeg(n, 2, SC_FWD[n[1]] * 1.4, -42)) });
  const climb = mix(posed([[0, CLING]]), scGait(0.45, 18, 14), { head: { rotation: (t) => V(0, 12 * Math.sin(t / 0.9 * 2 * PI), 0) }, body: { rotation: (t) => V(0, 0, 3 * Math.sin(t / 0.45 * 2 * PI)) } });
  climb.scuttler = { position: () => V(0, CLIMB.lift, CLIMB.z) };
  cl.push({ name: 'climb', length: 0.9, T: climb });
  // curious: aperture wide, the lid raised, the head tilted 20 degrees; a front leg lifted, unsure
  const CURIOUS = scStance({ ...APERTURE(1.2), lid: { rotation: V(15, 0, 0) }, head: { rotation: V(6, -8, 20) }, body: { rotation: V(-3, 0, 0) }, ...scLeg('l1', 48, 38, 20) });
  cl.push({ name: 'curious', length: 1.4, loop: 'hold', T: posed([[0, scStance()], [0.1, CURIOUS, 5, 6]], SC_DELAY), ground: TIPS, stepScale: false });
  // hostile: narrow, the lid slanted, crouched, plates lifted (the sees-player clip's end state, held)
  cl.push({ name: 'hostile', length: 1.2, loop: 'hold', T: posed([[0, scStance()], [0.05, SC_HOSTILE, 8, 7]], SC_DELAY), ground: TIPS, stepScale: false });
  // gets hit (stunned): flips onto its back, the lid half shut and the lens flickering, legs paddling (0.5 s), then it
  // kicks and rights itself with a hop, rolling on the way it went
  {
    const BACK = scStance({ body: { rotation: V(0, 0, 180) }, lid: { rotation: V(-22, 0, 0) }, head: { rotation: V(-10, 0, 0) } });
    const UP = scStance({ body: { rotation: V(0, 0, 360) } });
    const flip = posed([[0, scStance()], [0.02, BACK, 3.2, 5], [0.95, UP, 2.6, 5]], { ...SC_DELAY, body: 0 });
    for (const n of SC_LEGS) {
      const k = SC_LEGS.indexOf(n), s = scSide(n), fl = flip['leg_' + n].rotation, fs = flip['shin_' + n] ? flip['shin_' + n].rotation : () => SC_STANCE['shin_' + n].rotation;
      flip['leg_' + n] = { rotation: (t) => addV(fl(t), V(0, 0, s * 26 * env(t, 0.3, 0.92) * Math.sin(2 * PI * 5 * t + k * 2.1))) };
      flip['shin_' + n] = { rotation: (t) => addV(fs(t), V(0, 0, s * 30 * env(t, 0.3, 0.92) * Math.sin(2 * PI * 5 * t + k * 2.1 + 1.2))) };
    }
    flip.lens_glow = { scale: steps([[0, ONE], [0.12, V(0.6, 0.6, 1)], [0.2, NONE], [0.3, ONE], [0.38, NONE], [0.5, V(0.8, 0.8, 1)], [0.62, NONE], [0.8, V(0.7, 0.7, 1)], [1.3, ONE]]) };
    cl.push({ name: 'flipped', length: 1.9, T: flip, ground: ALL, spin: true, air: (t) => 3 * env(t, 0, 0.3) + 4 * env(t, 0.95, 1.35) });
  }
  // a packmate dies: its lens turns to the body for a beat (0.3 s held), the lid lowers; then it snaps back, hostile
  {
    const GRIEF = scStance({ head: { rotation: V(-12, 40, 0) }, ...APERTURE(0.9), lid: { rotation: V(-12, 0, 0) }, body: { rotation: V(0, 8, -5) } });
    cl.push({ name: 'grief', length: 1.6, loop: 'hold', T: posed([[0, scStance()], [0.05, GRIEF, 4, 7], [0.55, SC_HOSTILE, 9, 7]], SC_DELAY), ground: TIPS, stepScale: false });
  }
  // called out by the Custodian: curled in a wall vent's cavity (its front just behind the hatch, 29.3 px behind where it
  // lands), it shoots straight out along the floor as the hatch blows (0.15-0.55 s, riding up onto the fallen hatch), then,
  // clear of the wall, twists into a somersault, legs unrolling, lands on its back past the hatch (0.9 s), paddles, rights
  // itself with a hop and its lens snaps on, hostile
  {
    const v = posed([[0, { ...SC_CURLED }], [0.58, scStance(), 4, 6], [1.75, SC_HOSTILE, 6, 7]], { ...SC_DELAY, body: 0 });
    const out = (t) => smooth((t - 0.15) / 0.4), fly = (t) => smooth((t - 0.52) / 0.38);
    const bodyRot = v.body && v.body.rotation;
    v.body = { ...(v.body || {}), rotation: (t) => addV(bodyRot ? bodyRot(t) : V(), V(-360 * fly(t), 0, 180 * fly(t) + 180 * springStep(t, 1.35, 2.6, 5))) };
    for (const n of SC_LEGS) { const k = SC_LEGS.indexOf(n), s = scSide(n), f = v['leg_' + n].rotation; v['leg_' + n] = { rotation: (t) => addV(f(t), V(0, 0, s * 24 * env(t, 0.92, 1.32) * Math.sin(2 * PI * 5 * t + k * 2.1))) }; }
    v.lens_glow = { scale: steps([[0, NONE], [1.7, V(0.6, 0.6, 1)], [1.77, V(1.2, 1.2, 1)], [1.87, V(0.7, 0.7, 1)]]) };
    v.lens = { scale: steps([[0, ONE], [1.87, V(0.7, 0.7, 1)]]) };
    v.scuttler = { position: (t) => V(0, 0, 29.3 * (1 - (0.62 * out(t) + 0.38 * fly(t)))) };
    cl.push({ name: 'vent_burst', length: 2.4, loop: 'hold', T: v, ground: ALL, spin: true, air: (t) => (t < 0.52 ? 1.6 * seg(t, 0.3, 0.4) : 1.6 * (1 - seg(t, 0.52, 0.9))) + 5 * env(t, 0.52, 0.9) + 3.5 * env(t, 1.35, 1.65) });
  }
  // dying: the lens cracks, flickers and goes out, the lid slams shut, the legs curl in one by one, it settles on its belly
  {
    const DEAD = scStance({ lid: { rotation: V(-33, 0, 0) }, head: { rotation: V(-14, 0, 0) }, body: { rotation: V(0, 0, 9) }, ...PLATES_UP(-4) });
    const keys = [[0, scStance()], [0.7, scStance({ lid: { rotation: V(-33, 0, 0) } }), 10, 9], [0.8, DEAD, 3, 6]];
    SC_LEGS.forEach((n, k) => { const prev = keys[keys.length - 1][1]; keys.push([0.8 + k * 0.07, { ...prev, ...scLeg(n, DEATH_LEG.up, SC_FWD[n[1]] * 0.6, DEATH_LEG.shinIn) }, 5, 6]); });
    const d = posed(keys, SC_DELAY);
    d.lens_crack = { position: steps([[0, V()], [0.08, V(0, 0, -2.5)]]) };
    d.lens_glow = { scale: steps([[0, ONE], [0.12, NONE], [0.2, V(0.8, 0.8, 1)], [0.32, NONE], [0.38, V(0.6, 0.6, 1)], [0.5, NONE], [0.56, V(0.4, 0.4, 1)], [0.66, NONE]]) };
    cl.push({ name: 'death', length: 2.2, loop: 'hold', T: d, ground: ALL });
  }
  // mannerism: sniffs the ground: the lens dips, the aperture widens, three quick sniffs, then it snaps back up
  {
    const DIP = scStance({ head: { rotation: V(-26, 0, 0) }, body: { rotation: V(-6, 0, 0) }, ...APERTURE(1.25), lid: { rotation: V(8, 0, 0) } });
    const sn = mix(posed([[0, scStance()], [0.15, DIP, 4, 6], [1.0, scStance(), 9, 7]], SC_DELAY), { head: { rotation: (t) => V(4 * env(t, 0.4, 0.95) * Math.sin(2 * PI * 6 * t), 0, 0) } });
    cl.push({ name: 'sniff', length: 1.6, T: sn, ground: TIPS, stepScale: false });
  }
  // mannerism: chitters: the whole body buzzes for 0.4 s, the lid rattling
  {
    const bz = (a) => (t) => a * env(t, 0.2, 0.6) * Math.sin(2 * PI * 24 * t);
    cl.push({ name: 'chitter', length: 1.0, T: mix(posed([[0, scStance()]]), { body: { rotation: (t) => V(bz(1.5)(t), 0, bz(2.5)(t + 0.01)) }, head: { rotation: (t) => V(0, bz(3)(t + 0.02), 0) }, lid: { rotation: (t) => V(Math.abs(bz(8)(t)), 0, 0) } }), ground: TIPS });
  }
  // mannerism: stretches its legs one at a time, front to back: each reaches out and up, then settles back
  {
    const order = ['l1', 'r1', 'l2', 'r2', 'l3', 'r3'], keys = [[0, scStance()]];
    order.forEach((n, k) => { const t0 = 0.15 + k * 0.35; keys.push([t0, scStance({ ...scLeg(n, 46, SC_FWD[n[1]] + (n[1] === '1' ? 18 : n[1] === '3' ? -18 : 0), -24), body: { rotation: V(0, 0, scSide(n) * 3) } }), 5, 6]); keys.push([t0 + 0.25, scStance(), 5, 6]); });
    cl.push({ name: 'stretch', length: 2.6, T: posed(keys, SC_DELAY), ground: TIPS });
  }
  // mannerism: huddles: leans in to a packmate at its side, lens turned to touch theirs; a small nuzzle; back. Two
  // mirrored clips (packmate on its right / on its left)
  {
    const LEAN = scStance({ body: { rotation: V(0, 0, -11) }, head: { rotation: V(-4, -30, -10) }, ...APERTURE(1.1), lid: { rotation: V(8, 0, 0) },
      ...scLeg('r1', 18, 30, 0), ...scLeg('r2', 18, 0, 0), ...scLeg('r3', 18, -28, 0), ...scLeg('l1', 36, 30, 8), ...scLeg('l2', 36, 0, 8), ...scLeg('l3', 36, -28, 8) });
    const mk = (pose) => mix(posed([[0, scStance()], [0.3, pose, 3, 6], [2.4, scStance(), 3, 6]], SC_DELAY), { head: { rotation: (t) => V(0, 3 * env(t, 0.9, 2.2) * Math.sin(2 * PI * 1.5 * t), 0) } });
    cl.push({ name: 'huddle_right', length: 3.0, T: mk(LEAN), ground: TIPS, stepScale: false });
    cl.push({ name: 'huddle_left', length: 3.0, T: mk(scMirror(LEAN)), ground: TIPS, stepScale: false });
  }
  // mannerism: picks at scrap: a front leg pokes the ground in front of it twice (a spark on the second, at 1.0 s)
  {
    const LOOK = { head: { rotation: V(-14, 12, 0) }, body: { rotation: V(-4, 0, 0) } };
    const RAISE = scStance({ ...LOOK, ...scLeg('l1', PICK.raise[0], PICK.raise[1], PICK.raise[2]) });
    const POKE = scStance({ ...LOOK, ...scLeg('l1', PICK.poke[0], PICK.poke[1], PICK.poke[2]) });
    cl.push({ name: 'pick_scrap', length: 2.0, T: posed([[0, scStance()], [0.2, RAISE, 5, 6], [0.55, POKE, 12, 8], [0.72, RAISE, 8, 6], [0.95, POKE, 12, 8], [1.1, RAISE, 8, 6], [1.45, scStance(), 4, 6]], SC_DELAY), ground: TIPS });
  }
  return cl;
}

// ---------- Sentry ----------
const SE_L = [1, 2, 3, 4, 5, 6];
// watching: petals open 100 degrees (from closed); target: full open, 120
const PETAL_WATCH = PETAL_CLOSED + 100;
const DIM_OUT = V(0, 0, -2), PUPIL_OUT = V(0, 0, -2.25);
// a whole-face look held through a clip: 'dim' (watching), 'lit', 'target' (lit with ember pupils)
const seLook = (look) => {
  const o = {};
  for (const k of SE_L) {
    o[`lens_${k}_glow`] = { scale: () => (look === 'lit' || look === 'target' ? ONE : NONE) };
    o[`lens_${k}_dim`] = { position: () => (look === 'dim' ? DIM_OUT : V()) };
    o[`lens_${k}_pupil`] = { position: () => (look === 'target' ? PUPIL_OUT : V()) };
  }
  return o;
};
const SE_TWIN = twin({ petal: { 0: 3, 3: 0, 1: 2, 2: 1 }, lens: { 2: 6, 6: 2, 3: 5, 5: 3 } });
function sentryMoreClips() {
  const cl = [];
  // target seen: from watching, the petals snap full open (0.25 s, a flutter), all six lenses light with ember pupils and
  // swivel to it together, then the yaw ring follows with a 5 degree overshoot while the lenses settle back
  {
    const LOOK = Object.fromEntries(SE_L.map((k) => [`lens_${k}`, { rotation: V(0, 5, 0) }]));
    const BACK = Object.fromEntries(SE_L.map((k) => [`lens_${k}`, { rotation: V(0, 0, 0) }]));
    const tg = posed([[0, petals(PETAL_WATCH)], [0.05, { ...petals(PETAL_OPEN), ...LOOK }, 9, 4], [0.3, { ...petals(PETAL_OPEN), ...BACK, yaw: { rotation: V(0, 30, 0) } }, 3.2, 3]],
      { petal_1: 0.02, petal_2: 0.04, petal_3: 0.06 });
    for (const k of SE_L) {
      tg[`lens_${k}_glow`] = { scale: steps([[0, NONE], [0.08, ONE]]) };
      tg[`lens_${k}_dim`] = { position: steps([[0, DIM_OUT], [0.08, V()]]) };
      tg[`lens_${k}_pupil`] = { position: steps([[0, V()], [0.08, PUPIL_OUT]]) };
    }
    cl.push({ name: 'target', length: 1.6, loop: 'hold', T: tg, stepScale: false });
  }
  // aiming: lenses narrowed (0.7), pupils on, the petals twitching tight round the barrel, the bore flickering with light
  {
    const am = mix(posed([[0, petals(PETAL_OPEN - 6)]]), Object.fromEntries([0, 1, 2, 3].map((k) => [`petal_${k}`, { rotation: (t) => V(1.5 * Math.sin(2 * PI * 9 * t + k * 1.7), 0, 0) }])));
    Object.assign(am, seLook('target'));
    for (const k of SE_L) am[`lens_${k}_glow`] = { scale: () => V(0.7, 0.7, 1) };
    am.barrel = { scale: (t) => { const v = 1 + 0.06 * Math.abs(Math.sin(2 * PI * 3 * t)); return V(v, v, 1); } };
    cl.push({ name: 'aim', length: 1.0, T: am, stepScale: false });
  }
  // hit (on its right): the right petals flinch to 70 degrees and reopen with a flutter, the right lenses blink out for 0.3 s,
  // the head jolts away from the hit
  {
    const FL = { petal_0: { rotation: V(PETAL_OPEN - 6, 0, 0) }, petal_1: { rotation: V(PETAL_OPEN - 6, 0, 0) }, petal_2: { rotation: V(PETAL_CLOSED + 70, 0, 0) }, petal_3: { rotation: V(PETAL_CLOSED + 70, 0, 0) },
      pitch: { rotation: V(4, 0, 6) } };
    const h = posed([[0, petals(PETAL_OPEN)], [0.02, FL, 12, 7], [0.22, { ...petals(PETAL_OPEN), pitch: { rotation: V(0, 0, 0) } }, 6, 3]]);
    Object.assign(h, seLook('target'));
    for (const k of [5, 6]) { h[`lens_${k}_glow`] = { scale: steps([[0, ONE], [0.04, NONE], [0.34, ONE]]) }; h[`lens_${k}_pupil`] = { position: steps([[0, PUPIL_OUT], [0.04, V()], [0.34, PUPIL_OUT]]) }; }
    cl.push({ name: 'hit', length: 1.0, T: h });
    // hit on its left (D67): the same, mirrored: the left petals (0, 1) flinch, the left lenses (2, 3) blink out, the head
    // jolts to its right. Twins across the face: petals 0-3, 1-2; lenses 2-6, 3-5 (1 and 4 sit on the centre line)
    cl.push({ name: 'hit_left', length: 1.0, T: mirrored(h, SE_TWIN) });
  }
  // destroyed: the petals splay open past full and droop, the lenses go out in a spiral round the ring (each with a last
  // flicker), the head and barrel sag
  {
    const SPLAY = Object.fromEntries([0, 1, 2, 3].map((k) => [`petal_${k}`, { rotation: V(PETAL_OPEN + [14, 24, 30, 18][k], 0, 0) }]));
    const d = posed([[0, petals(PETAL_OPEN)], [0.05, SPLAY, 4, 2.5], [0.9, { ...SPLAY, pitch: { rotation: V(-24, 0, 4) }, barrel: { rotation: V(-26, 0, 0) }, yaw: { rotation: V(0, 8, 0) } }, 1.6, 4]],
      { petal_1: 0.05, petal_2: 0.1, petal_3: 0.15 });
    Object.assign(d, seLook('target'));
    [1, 6, 5, 4, 3, 2].forEach((k, i) => { const t0 = 0.35 + i * 0.18;
      d[`lens_${k}_glow`] = { scale: steps([[0, ONE], [t0, NONE], [t0 + 0.06, V(0.6, 0.6, 1)], [t0 + 0.1, NONE]]) };
      d[`lens_${k}_pupil`] = { position: steps([[0, PUPIL_OUT], [t0, V()]]) }; });
    cl.push({ name: 'death', length: 3.2, loop: 'hold', T: d });
  }
  // mannerisms, from watching (petals at 100 degrees, the lenses dim)
  // spins its barrel a quarter-turn and back (a self-check), with a click of overshoot each way
  { const b = posed([[0, petals(PETAL_WATCH)], [0.2, { ...petals(PETAL_WATCH), barrel: { rotation: V(0, 0, 90) } }, 6, 4], [0.9, { ...petals(PETAL_WATCH), barrel: { rotation: V(0, 0, 0) } }, 6, 4]]);
    cl.push({ name: 'barrel_check', length: 1.6, T: { ...b, ...seLook('dim') } }); }
  // one petal twitches shut a little and flutters back
  { const p = posed([[0, petals(PETAL_WATCH)], [0.2, { ...petals(PETAL_WATCH), petal_3: { rotation: V(PETAL_WATCH - 16, 0, 0) } }, 12, 8], [0.32, petals(PETAL_WATCH), 8, 2.5]]);
    cl.push({ name: 'petal_twitch', length: 1.2, T: { ...p, ...seLook('dim') } }); }
  // all six lenses blink in sequence round the ring
  { const bl = { ...posed([[0, petals(PETAL_WATCH)]]), ...seLook('dim') };
    [1, 6, 5, 4, 3, 2].forEach((k, i) => { const t0 = 0.2 + i * 0.12; bl[`lens_${k}_dim`] = { position: () => DIM_OUT, scale: steps([[0, ONE], [t0, NONE], [t0 + 0.12, ONE]]) }; });
    cl.push({ name: 'blink', length: 1.4, T: bl }); }
  // tilts to listen: pitches down 10 degrees with a small cant, holds 1 s, and comes back up
  { const l = posed([[0, petals(PETAL_WATCH)], [0.2, { ...petals(PETAL_WATCH), pitch: { rotation: V(-10, 0, 5) } }, 2.5, 5], [1.4, { ...petals(PETAL_WATCH), pitch: { rotation: V(0, 0, 0) } }, 2.5, 5]]);
    cl.push({ name: 'listen', length: 2.4, T: { ...l, ...seLook('dim') } }); }
  return cl;
}

// ---------- the Custodian ----------
// a lens layer ('ember' or 'blaze') slid out over its lens while on(t), the lit layer hidden under it
const cuLayer = (layer, names, on = () => true) => { const o = {};
  for (const n of names) { o[`lens_${n}_${layer}`] = { position: (t) => (on(t) ? CU_OUT(n) : V()) }; o[`lens_${n}_glow`] = { scale: (t) => (on(t) ? NONE : ONE) }; }
  return o; };
// tuned with the checker: the straightening hand and its two fingers; the counting hand
const STRAIGHTEN = { arm: V(18, 0, -2), fore: V(28, 0, 0), hand: V(-30, 0, 0), f1: V(0, 0, -20), f3: V(0, 0, -20) };
const COUNT = { arm: V(30, 0, 6), fore: V(110, 0, 0), hand: V(20, 0, 0), tap: V(80, 0, 0) };
const CU_TWIN = twin({ robe: { 1: 4, 4: 1, 2: 3, 3: 2, 5: 8, 8: 5, 6: 7, 7: 6 }, socket: { 1: 2, 2: 1, 3: 4, 4: 3 } });
function custodianMoreClips() {
  const cl = [], FEET = (n) => n.startsWith('foot_');
  // calls Scuttlers (a pylon broken): its head comes up, it raises its right hand high, palm out; the vents burst at 0.8 s
  {
    const RAISE = { ...STUDS_OFF, arm_right: { rotation: V(150, 0, -8) }, forearm_right: { rotation: V(14, 0, 0) }, hand_right: { rotation: V(-34, 0, 0) },
      waist: { rotation: V(4, -6, 0) }, head: { rotation: V(8, -6, 0) }, lens_mid_glow: { scale: V(1.2, 1.2, 1) } };
    cl.push({ name: 'call_scuttlers', length: 2.4, T: posed([[0, STUDS_OFF], [0.05, RAISE, 3, 6], [0.78, { ...RAISE, hand_right: { rotation: V(-46, 0, 0) } }, 10, 7], [1.45, STUDS_OFF, 2.2, 5]], CU_DELAY), ground: FEET, stepScale: false });
  }
  // hit while unshielded: the struck side (its right) recoils, the head jolts, a robe plate rattles
  {
    const RECOIL = { ...STUDS_OFF, chest: { rotation: V(2, -7, -3) }, head: { rotation: V(3, 6, 0) }, arm_right: { rotation: V(-10, 0, 4) }, forearm_right: { rotation: V(6, 0, 0) } };
    const hit = posed([[0, STUDS_OFF], [0.0, RECOIL, 10, 7], [0.22, STUDS_OFF, 3.5, 5]], CU_DELAY);
    const rattle = (a, d) => (t) => V(t > d ? a * Math.exp(-5 * (t - d)) * Math.sin(2 * PI * 9 * (t - d)) : 0, 0, 0);
    hit.robe_4 = { rotation: rattle(7, 0.06) }; hit.robe_3 = { rotation: rattle(3, 0.1) }; hit.robe_8 = { rotation: rattle(3, 0.12) };
    cl.push({ name: 'hit', length: 1.2, T: hit, ground: FEET });
    // hit on its left (D67): the same, mirrored: its left side recoils, the head jolts to its right, the left-hand plates
    // rattle. Twins across it: robe plates 1-4, 2-3, 5-8, 6-7; sockets 1-2, 3-4; every *_left with its *_right
    cl.push({ name: 'hit_left', length: 1.2, T: mirrored(hit, CU_TWIN), ground: FEET });
  }
  // mannerism 1: straightens a robe plate with two fingers: looks down, the right hand pinches the front-right plate's
  // edge and tugs it out a little, lets go; the plate settles
  {
    const S = STRAIGHTEN;
    const REACH = { ...STUDS_OFF, waist: { rotation: V(-11, -6, 0) }, neck: { rotation: V(-5, 0, 0) }, head: { rotation: V(-11, -10, 0) },
      arm_right: { rotation: S.arm }, forearm_right: { rotation: S.fore }, hand_right: { rotation: S.hand }, finger_right_1: { rotation: S.f1 }, finger_right_3: { rotation: S.f3 } };
    const st = posed([[0, STUDS_OFF], [0.15, REACH, 3, 6], [0.95, { ...REACH, robe_3: { rotation: V(7, 0, 0) }, hand_right: { rotation: addV(S.hand, V(-6, 0, 0)) } }, 5, 6],
      [1.35, { ...REACH, robe_3: { rotation: V(0, 0, 0) } }, 6, 3], [1.75, STUDS_OFF, 2.5, 5]], { ...CU_DELAY, robe_3: 0 });
    cl.push({ name: 'straighten_plate', length: 2.8, T: st, ground: FEET });
  }
  // mannerism 2: turns its head to look at a broken pylon (its left here): a beat of mourning, the shoulders settle; back
  {
    const LOOK = { ...STUDS_OFF, neck: { rotation: V(2, 14, 0) }, head: { rotation: V(3, 42, 0) }, waist: { rotation: V(0, 8, 0) },
      shoulder_left: { position: V(0, -0.4, 0) }, shoulder_right: { position: V(0, -0.4, 0) }, lens_mid_glow: { scale: V(0.85, 0.85, 1) } };
    cl.push({ name: 'mourn', length: 3.0, T: posed([[0, STUDS_OFF], [0.1, LOOK, 2, 6], [1.9, STUDS_OFF, 1.8, 5]], CU_DELAY), ground: FEET, stepScale: false });
  }
  // mannerism 3: counts: lifts its right hand in front of its face, fingers up, and folds each fingertip down onto its palm
  // in turn, twice round, the head bowed to watch
  {
    const C3 = COUNT;
    const HOLD = { ...STUDS_OFF, arm_right: { rotation: C3.arm }, forearm_right: { rotation: C3.fore }, hand_right: { rotation: C3.hand }, neck: { rotation: V(-3, 0, 0) }, head: { rotation: V(-8, -8, 0) } };
    const keys = [[0, STUDS_OFF], [0.1, HOLD, 3, 6]];
    for (let r = 0; r < 2; r++) for (let f = 1; f <= 3; f++) { const t0 = 0.75 + (r * 3 + f - 1) * 0.24; keys.push([t0, { ...HOLD, [`finger_right_${f}`]: { rotation: C3.tap } }, 14, 8]); keys.push([t0 + 0.12, HOLD, 12, 8]); }
    keys.push([2.35, STUDS_OFF, 2.5, 5]);
    cl.push({ name: 'count', length: 3.4, T: posed(keys, CU_DELAY), ground: FEET });
  }
  // mannerism 4: turns its lenses to the vault behind it (over its left shoulder: the head leads, then the chest), holds, back
  {
    const BEHIND = { ...STUDS_OFF, waist: { rotation: V(0, 22, 0) }, neck: { rotation: V(0, 28, 0) }, head: { rotation: V(4, 46, 0) } };
    cl.push({ name: 'glance_vault', length: 3.0, T: posed([[0, STUDS_OFF], [0.1, BEHIND, 2, 6], [1.6, STUDS_OFF, 2, 5]], { ...CU_DELAY, head: 0, neck: 0.06, waist: 0.14, chest: 0.16 }), ground: FEET });
  }
  return cl;
}

module.exports = { P, G, C, VA, build, checker, png, posed, mix, steps, springStep, seg, smooth, V, addV, ONE, NONE, scuttler, sentry, custodian, scuttlerClips, sentryClips, custodianClips, SC_STANCE, SC_CURLED, PETAL_OPEN, PETAL_CLOSED, KNEEL, HIP_Y, scWraps, seWraps, cuWraps, lensPaint, vaPaint, writeBot, grounded, prepare, mirrored, twin, SE_TWIN, CU_TWIN };
if (require.main === module) {
const which = process.argv[2] || 'all';
if (which === 'scuttler' || which === 'all') {
  const root = scuttler();
  // the rest pose stands: write the stance into the bones' rest rotations
  const set = (n, b) => { if (n.kind !== 'g') return; if (SC_STANCE[n.name]) n.rot = SC_STANCE[n.name].rotation; n.kids.forEach((k) => set(k, b)); };
  set(root);
  // clips then carry only deltas from the stance
  const minus = (T) => { for (const b of Object.keys(SC_STANCE)) if (T[b] && T[b].rotation) { const f = T[b].rotation; T[b] = { ...T[b], rotation: (t) => addV(f(t), SC_STANCE[b].rotation.map((v) => -v)) }; } return T; };
  writeBot('scuttler', root, 64, scuttlerClips().map((c) => ({ ...c, T: minus(c.T) })), scWraps);
}
if (which === 'sentry' || which === 'all') {
  const root = sentry();
  const set = (n) => { if (n.kind !== 'g') return; if (/^petal_\d$/.test(n.name)) n.rot = [PETAL_OPEN, 0, 0]; n.kids.forEach(set); };
  set(root);
  const minus = (T) => { for (const b of Object.keys(T)) if (/^petal_\d$/.test(b) && T[b].rotation) { const f = T[b].rotation; T[b] = { ...T[b], rotation: (t) => addV(f(t), V(-PETAL_OPEN, 0, 0)) }; } return T; };
  writeBot('sentry', root, 128, sentryClips().map((c) => ({ ...c, T: minus(c.T) })), seWraps);
}
if (which === 'custodian' || which === 'all') writeBot('custodian', custodian(), 128, custodianClips(), cuWraps);
}
