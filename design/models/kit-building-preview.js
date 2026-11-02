// Kit building -> Blockbench preview, from the game's own block models. Preview only: nothing here goes into the mod.
//   node kit-building-preview.js <layout.json> [more.json ...] <out.bbmodel>   (one building per JSON, side by side along x)
//   or require it: { readLayout, Resolver, Scene }.
// A layout is what a game test dumps from a kit Model: [{ x, y, z, state: "ns:id[prop=value,...]" }] in local block
// coordinates (x across the front, y up, z back from the front). Every block state is resolved the way the game does it:
// its blockstate JSON (variants or multipart, weighted picks by position), each model's parent chain (textures merged leaf
// over root, the nearest elements), the variant's x/y turn (90 degree steps: boxes stay boxes, faces and UVs remapped,
// uvlock re-projected), and its textures from this mod's assets, the vanilla client jar and Create's jar, embedded in the
// .bbmodel. Faces whose cullface touches a full opaque block are dropped, and a part with every face dropped goes too.
// A block that won't resolve (no blockstate, no elements: Create's part-rendered blocks, odd multiparts) becomes a plain
// cube in its particle (or side) texture and is listed in scene.fallbacks.
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const crypto = require('crypto');
const { png } = require('./process-box.lib.js');

const REPO = path.resolve(__dirname, '../..');
const MOD = 'comedymonkeysaddons';
const uuid = () => crypto.randomUUID();
const DIRS = ['north', 'east', 'south', 'west', 'up', 'down'];
const VEC = { north: [0, 0, -1], south: [0, 0, 1], west: [-1, 0, 0], east: [1, 0, 0], up: [0, 1, 0], down: [0, -1, 0] };
const dirOf = (v) => DIRS.find((d) => VEC[d].every((c, i) => Math.round(v[i]) === c));

// ---------------------------------------------------------------- resources: the mod's folders and the jars
function findFile(root, test, depth = 6) {
  if (depth < 0 || !fs.existsSync(root)) return null;
  for (const e of fs.readdirSync(root, { withFileTypes: true })) {
    const p = path.join(root, e.name);
    if (e.isFile() && test(e.name)) return p;
    if (e.isDirectory()) { const f = findFile(p, test, depth - 1); if (f) return f; }
  }
  return null;
}
function jars() {
  const home = process.env.USERPROFILE || process.env.HOME;
  const vanilla = findFile(path.join(REPO, 'build/moddev/artifacts'), (n) => /client-extra.*\.jar$/.test(n), 1);
  const create = findFile(path.join(home, '.gradle/caches/modules-2/files-2.1/com.simibubi.create'), (n) => /^create-1\.21\.1-6\.0\.10.*\.jar$/.test(n) && !/sources/.test(n));
  if (!vanilla) throw new Error('no vanilla client jar under build/moddev/artifacts (run a Gradle build first)');
  if (!create) throw new Error('no Create 6.0.10 jar in the Gradle caches');
  return [vanilla, create];
}
// A zip's central directory, read once; entries inflated on demand
function openZip(file) {
  const buf = fs.readFileSync(file);
  let e = buf.length - 22;
  while (e >= 0 && buf.readUInt32LE(e) !== 0x06054b50) e--;
  if (e < 0) throw new Error('not a zip: ' + file);
  const n = buf.readUInt16LE(e + 10);
  const entries = new Map();
  let p = buf.readUInt32LE(e + 16);
  for (let i = 0; i < n; i++) {
    const nl = buf.readUInt16LE(p + 28), el = buf.readUInt16LE(p + 30), cl = buf.readUInt16LE(p + 32);
    entries.set(buf.toString('utf8', p + 46, p + 46 + nl), { method: buf.readUInt16LE(p + 10), size: buf.readUInt32LE(p + 20), at: buf.readUInt32LE(p + 42) });
    p += 46 + nl + el + cl;
  }
  return {
    file,
    read(name) {
      const en = entries.get(name);
      if (!en) return null;
      const start = en.at + 30 + buf.readUInt16LE(en.at + 26) + buf.readUInt16LE(en.at + 28), data = buf.subarray(start, start + en.size);
      return en.method === 0 ? Buffer.from(data) : zlib.inflateRawSync(data);
    },
  };
}

// ---------------------------------------------------------------- PNG in (any 8-bit-or-less colour type, for alpha and frames) and out
function decodePng(buf) {
  let p = 8, w, h, depth, type, plte = null, trns = null;
  const idat = [];
  while (p < buf.length) {
    const len = buf.readUInt32BE(p), t = buf.toString('ascii', p + 4, p + 8), body = buf.subarray(p + 8, p + 8 + len);
    if (t === 'IHDR') { w = body.readUInt32BE(0); h = body.readUInt32BE(4); depth = body[8]; type = body[9]; if (body[12]) throw new Error('interlaced PNG'); }
    else if (t === 'PLTE') plte = body;
    else if (t === 'tRNS') trns = body;
    else if (t === 'IDAT') idat.push(body);
    p += 12 + len;
  }
  const ch = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[type], bits = ch * depth, bpp = Math.max(1, bits >> 3), stride = Math.ceil((w * bits) / 8);
  const raw = zlib.inflateSync(Buffer.concat(idat)), px = Buffer.alloc(stride * h);
  for (let y = 0; y < h; y++) {
    const f = raw[y * (stride + 1)], src = raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1)), o = y * stride;
    for (let i = 0; i < stride; i++) {
      const a = i >= bpp ? px[o + i - bpp] : 0, b = y ? px[o - stride + i] : 0, c = y && i >= bpp ? px[o - stride + i - bpp] : 0;
      let v = src[i];
      if (f === 1) v += a; else if (f === 2) v += b; else if (f === 3) v += (a + b) >> 1;
      else if (f === 4) { const q = a + b - c, pa = Math.abs(q - a), pb = Math.abs(q - b), pc = Math.abs(q - c); v += pa <= pb && pa <= pc ? a : pb <= pc ? b : c; }
      px[o + i] = v & 255;
    }
  }
  const sample = (y, i) => { // the i-th sample of row y, scaled to 8 bits
    if (depth === 8) return px[y * stride + i];
    if (depth === 16) return px[y * stride + i * 2];
    const bit = i * depth, v = (px[y * stride + (bit >> 3)] >> (8 - depth - (bit & 7))) & ((1 << depth) - 1);
    return type === 3 ? v : Math.round((v * 255) / ((1 << depth) - 1));
  };
  const rgba = new Uint8Array(w * h * 4);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) {
    const o = (y * w + x) * 4;
    if (type === 3) { const k = sample(y, x); rgba.set([plte[k * 3], plte[k * 3 + 1], plte[k * 3 + 2], trns && k < trns.length ? trns[k] : 255], o); }
    else if (type === 0) { const g = sample(y, x); rgba.set([g, g, g, 255], o); }
    else if (type === 4) { const g = sample(y, x * 2); rgba.set([g, g, g, sample(y, x * 2 + 1)], o); }
    else for (let c = 0; c < ch; c++) rgba[o + c] = sample(y, x * ch + c), ch === 3 && (rgba[o + 3] = 255);
  }
  return { w, h, rgba };
}

// ---------------------------------------------------------------- block states and models
// "ns:id[a=b,c=d]" -> { id, props }
function parseState(s) {
  const m = /^([^[]+)(?:\[(.*)\])?$/.exec(s);
  const props = {};
  if (m[2]) for (const kv of m[2].split(',')) { const [k, v] = kv.split('='); props[k] = v; }
  return { id: m[1].includes(':') ? m[1] : 'minecraft:' + m[1], props };
}
function readLayout(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8')).map((b) => ({ x: b.x, y: b.y, z: b.z, state: b.state, ...parseState(b.state) }));
}
const rl = (ref, kind) => { const [ns, p] = ref.includes(':') ? ref.split(':') : ['minecraft', ref]; return { ns, file: `assets/${ns}/${kind}/${p}` }; };
function hash3(x, y, z) {
  let h = (x * 3129871) ^ (z * 116129781) ^ y;
  h = h * h * 42317861 + h * 11;
  return ((h >> 16) & 0xffff) / 0x10000;
}

class Resolver {
  constructor() {
    this.dirs = [path.join(REPO, 'src/main/resources'), path.join(REPO, 'src/generated/resources')];
    this.zips = jars().map(openZip);
    this.cache = new Map();
    this.textures = new Map();
    this.notes = new Set();
  }
  read(file) {
    if (this.cache.has(file)) return this.cache.get(file);
    let buf = null;
    for (const d of this.dirs) { const f = path.join(d, file); if (fs.existsSync(f)) { buf = fs.readFileSync(f); break; } }
    for (const z of this.zips) { if (buf) break; buf = z.read(file); }
    this.cache.set(file, buf);
    return buf;
  }
  json(file) { const b = this.read(file); return b ? JSON.parse(b.toString('utf8')) : null; }
  // the model with its parents folded in: { textures, elements, chain }
  model(ref) {
    const key = 'model ' + ref;
    if (this.cache.has(key)) return this.cache.get(key);
    const chain = [];
    let cur = ref.includes(':') ? ref : 'minecraft:' + ref;
    while (cur && !cur.startsWith('builtin/') && !cur.startsWith('minecraft:builtin/') && chain.length < 32) {
      const j = this.json(rl(cur, 'models').file + '.json');
      if (!j) { this.notes.add('no model ' + cur); break; }
      chain.push({ ref: cur, j });
      cur = j.parent ? (j.parent.includes(':') ? j.parent : 'minecraft:' + j.parent) : null;
    }
    const textures = {};
    for (let i = chain.length - 1; i >= 0; i--) Object.assign(textures, chain[i].j.textures || {});
    const withEls = chain.find((c) => c.j.elements);
    const out = { ref, textures, elements: withEls ? withEls.j.elements : [], chain: chain.map((c) => c.ref), renderType: chain.map((c) => c.j.render_type).find(Boolean) };
    this.cache.set(key, out);
    return out;
  }
  tex(textures, name) {
    let t = name;
    for (let i = 0; i < 16 && t && t.startsWith('#'); i++) t = textures[t.slice(1)];
    return t && !t.startsWith('#') ? (t.includes(':') ? t : 'minecraft:' + t) : null;
  }
  // a texture's PNG (the first frame of an animated strip), and whether any pixel is see-through
  texture(ref) {
    if (this.textures.has(ref)) return this.textures.get(ref);
    const buf = this.read(rl(ref, 'textures').file + '.png');
    let out = null;
    if (buf) {
      const img = decodePng(buf);
      let { w, h, rgba } = img, data = buf;
      if (h > w && h % w === 0) { rgba = rgba.slice(0, w * w * 4); h = w; data = png(w, h, rgba); this.notes.add('animated texture, first frame: ' + ref); }
      let alpha = false;
      for (let i = 3; i < rgba.length; i += 4) if (rgba[i] < 255) { alpha = true; break; }
      out = { ref, data, w, h, alpha };
    } else this.notes.add('no texture ' + ref);
    this.textures.set(ref, out);
    return out;
  }
  // the parts a block state shows: [{ model, x, y, uvlock }], or [] when it has no blockstate
  parts(id, props, pos) {
    const bs = this.json(rl(id, 'blockstates').file + '.json');
    if (!bs) return [];
    const pick = (v) => {
      if (!Array.isArray(v)) return v;
      const total = v.reduce((s, o) => s + (o.weight || 1), 0);
      let r = hash3(pos.x, pos.y, pos.z) * total;
      for (const o of v) { r -= o.weight || 1; if (r < 0) return o; }
      return v[v.length - 1];
    };
    const one = (v) => ({ model: this.model(v.model), x: v.x || 0, y: v.y || 0, uvlock: !!v.uvlock });
    if (bs.variants) {
      for (const [k, v] of Object.entries(bs.variants)) {
        const ok = k === '' || k === 'normal' || k.split(',').every((kv) => { const [a, b] = kv.split('='); return props[a] === b; });
        if (ok) return [one(pick(v))];
      }
      return [];
    }
    const when = (w) => {
      if (!w) return true;
      if (w.OR) return w.OR.some(when);
      if (w.AND) return w.AND.every(when);
      return Object.entries(w).every(([k, v]) => String(v).split('|').includes(props[k]));
    };
    return (bs.multipart || []).filter((p) => when(p.when)).map((p) => one(pick(p.apply)));
  }
}

// ---------------------------------------------------------------- turning an element by a variant's x and y
// The game turns by R_y(-y) R_x(-x) about the block's middle (BlockModelRotation).
function turn(p, rx, ry, c = 8) {
  let [x, y, z] = [p[0] - c, p[1] - c, p[2] - c];
  const a = (-rx * Math.PI) / 180, b = (-ry * Math.PI) / 180;
  [y, z] = [y * Math.cos(a) - z * Math.sin(a), y * Math.sin(a) + z * Math.cos(a)];
  [x, z] = [x * Math.cos(b) + z * Math.sin(b), -x * Math.sin(b) + z * Math.cos(b)];
  const r = (v) => Math.round(v * 1e4) / 1e4;
  return [r(x + c), r(y + c), r(z + c)];
}
// the face's texture frame: U, V of a point on it (rotation 0, the game's default UV)
const UV = {
  north: (p) => [16 - p[0], 16 - p[1]], south: (p) => [p[0], 16 - p[1]], west: (p) => [p[2], 16 - p[1]],
  east: (p) => [16 - p[2], 16 - p[1]], up: (p) => [p[0], p[2]], down: (p) => [p[0], 16 - p[2]],
};
function defaultUv(dir, f, t) {
  const a = UV[dir](f), b = UV[dir](t);
  return [Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[0], b[0]), Math.max(a[1], b[1])];
}
// the face's four corners as TL, BL, BR, TR in its texture frame
function corners(dir, f, t) {
  const ax = { north: 2, south: 2, west: 0, east: 0, up: 1, down: 1 }[dir], at = VEC[dir][ax] > 0 ? t[ax] : f[ax];
  const pts = [];
  for (const a of [f, t]) for (const b of [f, t]) {
    const p = [0, 0, 0], others = [0, 1, 2].filter((i) => i !== ax);
    p[ax] = at; p[others[0]] = a[others[0]]; p[others[1]] = b[others[1]];
    pts.push(p);
  }
  const uv = pts.map(UV[dir]);
  const minU = Math.min(...uv.map((q) => q[0])), maxU = Math.max(...uv.map((q) => q[0])), minV = Math.min(...uv.map((q) => q[1])), maxV = Math.max(...uv.map((q) => q[1]));
  if (minU === maxU || minV === maxV) return null;
  const find = (u, v) => pts[uv.findIndex((q) => q[0] === u && q[1] === v)];
  return [find(minU, minV), find(minU, maxV), find(maxU, maxV), find(maxU, minV)];
}
const texAt = (uv, rot, k) => { const C = [[uv[0], uv[1]], [uv[0], uv[3]], [uv[2], uv[3]], [uv[2], uv[1]]]; return C[(k + rot / 90) % 4]; };
const same = (a, b) => Math.abs(a[0] - b[0]) < 1e-3 && Math.abs(a[1] - b[1]) < 1e-3;
const key3 = (p) => p.map((v) => v.toFixed(3)).join(',');
// one face, turned: its new direction, UV and UV rotation
function turnFace(dir, face, f, t, rx, ry, nf, nt, uvlock) {
  const nd = dirOf(turn(VEC[dir], rx, ry, 0));
  const uv = face.uv || defaultUv(dir, f, t), rot = face.rotation || 0;
  if (uvlock || (!rx && !ry)) return { dir: nd, uv: uvlock && (rx || ry) ? defaultUv(nd, nf, nt) : uv, rotation: uvlock && (rx || ry) ? 0 : rot };
  const old = corners(dir, f, t), now = corners(nd, nf, nt);
  if (!old || !now) return { dir: nd, uv, rotation: rot };
  const at = new Map(old.map((p, k) => [key3(turn(p, rx, ry)), texAt(uv, rot, k)]));
  const want = now.map((p) => at.get(key3(p)));
  for (let r = 0; r < 4; r++) {
    const idx = (s) => [0, 1, 2, 3].find((k) => (k + r) % 4 === s);
    const a = want[idx(0)], b = want[idx(2)];
    const cand = [a[0], a[1], b[0], b[1]];
    if ([0, 1, 2, 3].every((k) => same(texAt(cand, r * 90, k), want[k]))) return { dir: nd, uv: cand, rotation: r * 90 };
  }
  return { dir: nd, uv, rotation: rot };
}
// an element of a model, turned by a variant: { from, to, rotation: {origin, axis, angle} | null, faces: {dir: {uv, rotation, texture, cullface}} }
function turnElement(el, rx, ry, uvlock) {
  const a = turn(el.from, rx, ry), b = turn(el.to, rx, ry);
  const nf = [0, 1, 2].map((i) => Math.min(a[i], b[i])), nt = [0, 1, 2].map((i) => Math.max(a[i], b[i]));
  const faces = {};
  for (const [dir, face] of Object.entries(el.faces || {})) {
    const tf = turnFace(dir, face, el.from, el.to, rx, ry, nf, nt, uvlock);
    faces[tf.dir] = { uv: tf.uv, rotation: tf.rotation, texture: face.texture, cullface: face.cullface ? dirOf(turn(VEC[face.cullface === 'bottom' ? 'down' : face.cullface], rx, ry, 0)) : null, tintindex: face.tintindex };
  }
  let rotation = null;
  if (el.rotation && el.rotation.angle) {
    const v = turn({ x: [1, 0, 0], y: [0, 1, 0], z: [0, 0, 1] }[el.rotation.axis], rx, ry, 0), i = v.findIndex((c) => Math.abs(c) > 0.5);
    rotation = { origin: turn(el.rotation.origin, rx, ry), axis: 'xyz'[i], angle: el.rotation.angle * Math.sign(v[i]) };
  }
  return { from: nf, to: nt, rotation, faces, shade: el.shade !== false, name: el.name };
}

// ---------------------------------------------------------------- the scene: buildings placed side by side, written as a Generic Model
class Scene {
  constructor(name, resolver = new Resolver()) {
    this.name = name;
    this.R = resolver;
    this.textures = [];
    this.texIndex = new Map();
    this.elements = [];
    this.outliner = [];
    this.groups = [];
    this.fallbacks = new Map(); // state id -> count
    this.full = new Map();
  }
  // a texture by resource ref, or an own PNG under 'own:<key>' (addTexture)
  texId(ref) {
    if (this.texIndex.has(ref)) return this.texIndex.get(ref);
    const t = ref.startsWith('own:') ? this.own[ref] : this.R.texture(ref);
    if (!t) return null;
    const i = this.textures.length;
    this.texIndex.set(ref, i);
    this.textures.push({ path: '', name: ref.replace(/^.*[/:]/, '') + '.png', folder: '', namespace: '', id: String(i), width: t.w, height: t.h, uv_width: 16, uv_height: 16,
      particle: false, render_mode: 'default', render_sides: 'auto', frame_time: 1, frame_order_type: 'loop', visible: true, internal: true, saved: false, uuid: uuid(),
      source: 'data:image/png;base64,' + t.data.toString('base64') });
    return i;
  }
  addTexture(key, data) { (this.own = this.own || {})['own:' + key] = { data, w: 16, h: 16, alpha: false }; return 'own:' + key; }
  // the parts of a block state, turned, with texture refs resolved; [] when nothing resolves
  blockParts(b) {
    const out = [];
    for (const p of this.R.parts(b.id, b.props, b)) for (const el of p.model.elements) {
      const e = turnElement(el, p.x, p.y, p.uvlock);
      for (const f of Object.values(e.faces)) f.ref = this.R.tex(p.model.textures, f.texture);
      out.push(e);
    }
    return out;
  }
  // a block a neighbour's face can't be seen through: a full cube of opaque faces
  isFull(state) {
    if (!state) return false;
    if (this.full.has(state.state)) return this.full.get(state.state);
    const full = this.blockParts(state).some((e) => e.from.every((v) => v <= 0) && e.to.every((v) => v >= 16) && !e.rotation
      && DIRS.every((d) => e.faces[d] && e.faces[d].ref && this.R.texture(e.faces[d].ref) && !this.R.texture(e.faces[d].ref).alpha));
    this.full.set(state.state, full);
    return full;
  }
  fallbackTexture(b) {
    const bs = this.R.json(rl(b.id, 'blockstates').file + '.json');
    let model = null;
    if (bs) { const v = bs.variants ? Object.values(bs.variants)[0] : bs.multipart && bs.multipart[0].apply; model = v && this.R.model((Array.isArray(v) ? v[0] : v).model); }
    const tx = model ? model.textures : {};
    for (const k of ['particle', 'side', 'all', 'texture', ...Object.keys(tx)]) { const r = this.R.tex(tx, '#' + k); if (r && this.R.texture(r)) return r; }
    const [ns, p] = b.id.split(':');
    for (const guess of [`${ns}:block/${p}`, `${ns}:block/${p}_side`]) if (this.R.texture(guess)) return guess;
    return null;
  }
  // one element in scene units (a block is 16), its faces { dir: { ref, uv, rotation } | null }
  push(name, from, to, faces, o = {}) {
    const bf = {};
    for (const d of DIRS) {
      const f = faces[d];
      const ti = f && f.ref ? this.texId(f.ref) : null;
      bf[d] = ti == null ? { uv: [0, 0, 0, 0], texture: null } : { uv: f.uv.map((v) => Math.round(v * 1000) / 1000), texture: ti, ...(f.rotation ? { rotation: f.rotation } : {}) };
    }
    const id = uuid(), rot = [0, 0, 0];
    if (o.rotation) rot['xyz'.indexOf(o.rotation.axis)] = o.rotation.angle;
    this.elements.push({ name, type: 'cube', uuid: id, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
      from, to, origin: o.origin || from.map((v, i) => (v + to[i]) / 2), rotation: rot, autouv: 0, color: 0, inflate: 0, shade: o.shade !== false, faces: bf });
    return id;
  }
  group(name, children, origin = [0, 0, 0]) {
    const id = uuid();
    this.groups.push({ name, uuid: id, origin, rotation: [0, 0, 0], export: true, locked: false, visibility: true, color: 0, children: [], autouv: 0, isOpen: false });
    return { name, uuid: id, origin, rotation: [0, 0, 0], export: true, isOpen: false, locked: false, visibility: true, autouv: 0, color: 0, children };
  }
  // A building: blocks [{x, y, z, id, props, state}] at a block offset; returns its outliner node (added to the root unless
  // o.parent is given). o.custom(b) may return faces { dir: { ref, uv, rotation } } to draw a block as a plain cube instead.
  addBuilding(name, blocks, off = [0, 0, 0], o = {}) {
    const at = new Map(blocks.map((b) => [`${b.x},${b.y},${b.z}`, b]));
    const kids = [];
    const S = (b, v, i) => (b[['x', 'y', 'z'][i]] + off[i]) * 16 + v;
    for (const b of blocks) {
      const nb = (d) => at.get(`${b.x + VEC[d][0]},${b.y + VEC[d][1]},${b.z + VEC[d][2]}`);
      const custom = o.custom && o.custom(b);
      const label = `${b.id.replace(/^.*:/, '')} ${b.x},${b.y},${b.z}`;
      if (custom) {
        const faces = {};
        // hidden against a full block or another custom cube
        for (const d of DIRS) faces[d] = custom[d] && !(this.isFull(nb(d)) || (nb(d) && o.custom(nb(d)))) ? custom[d] : null;
        if (DIRS.some((d) => faces[d])) kids.push(this.push(label, [S(b, 0, 0), S(b, 0, 1), S(b, 0, 2)], [S(b, 16, 0), S(b, 16, 1), S(b, 16, 2)], faces));
        continue;
      }
      let parts = this.blockParts(b);
      if (!parts.length) {
        const ref = this.fallbackTexture(b);
        this.fallbacks.set(b.id, (this.fallbacks.get(b.id) || 0) + 1);
        const faces = {};
        for (const d of DIRS) faces[d] = { ref, uv: [0, 0, 16, 16], cullface: d };
        parts = [{ from: [0, 0, 0], to: [16, 16, 16], rotation: null, faces, shade: true }];
      }
      parts.forEach((e, n) => {
        const faces = {};
        for (const [d, f] of Object.entries(e.faces)) {
          if (f.cullface && this.isFull(nb(f.cullface))) continue;
          if (f.tintindex != null && f.tintindex >= 0) this.R.notes.add('tinted face drawn untinted: ' + b.id);
          faces[d] = f;
        }
        if (!Object.keys(faces).length) return;
        const origin = e.rotation ? e.rotation.origin.map((v, i) => S(b, v, i)) : null;
        kids.push(this.push(label + (parts.length > 1 ? ' ' + (e.name || n) : ''), e.from.map((v, i) => S(b, v, i)), e.to.map((v, i) => S(b, v, i)), faces, { rotation: e.rotation, origin, shade: e.shade }));
      });
    }
    const node = this.group(name, kids, [off[0] * 16, off[1] * 16, off[2] * 16]);
    (o.parent ? o.parent.children : this.outliner).push(node);
    return node;
  }
  write(out) {
    const bb = {
      meta: { format_version: '5.0', model_format: 'free', box_uv: false },
      name: this.name, model_identifier: '', visible_box: [1, 1, 0], variable_placeholders: '', variable_placeholder_buttons: [],
      timeline_setups: [], unhandled_root_fields: {}, resolution: { width: 16, height: 16 },
      elements: this.elements, groups: this.groups, outliner: this.outliner, textures: this.textures, animations: [],
    };
    fs.writeFileSync(out, JSON.stringify(bb));
    console.error('wrote', out, this.elements.length, 'cubes', this.textures.length, 'textures');
  }
  report() {
    const lines = [];
    if (this.fallbacks.size) lines.push('plain-cube fallbacks: ' + [...this.fallbacks].map(([k, n]) => `${k} x${n}`).join(', '));
    else lines.push('plain-cube fallbacks: none');
    for (const n of this.R.notes) lines.push('note: ' + n);
    return lines.join('\n');
  }
}

module.exports = { readLayout, parseState, Resolver, Scene, turn, turnElement, decodePng, openZip, corners, texAt, DIRS, VEC };

if (require.main === module) {
  const args = process.argv.slice(2);
  if (args.length < 2) { console.error('usage: node kit-building-preview.js <layout.json> [...] <out.bbmodel>'); process.exit(1); }
  const out = args.pop(), scene = new Scene(path.basename(out, '.bbmodel'));
  let x = 0;
  for (const f of args) {
    const blocks = readLayout(f), minX = Math.min(...blocks.map((b) => b.x)), maxX = Math.max(...blocks.map((b) => b.x));
    scene.addBuilding(path.basename(f, '.json'), blocks, [x - minX, 0, 0]);
    x += maxX - minX + 1 + 6;
  }
  scene.write(out);
  console.log(scene.report());
}
