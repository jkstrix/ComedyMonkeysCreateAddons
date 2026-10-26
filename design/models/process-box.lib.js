// Shared kit for the "sealed process box" family (Auto Harvester, Animal Pens, later the Cobble Generators):
// the house palette, texture painting, the shell panel painters, the model DSL and the Blockbench preview writer.
// Every model is built in the game's model space: front (window) facing north, x 0 west -> 16 east, y up, z 0 front -> 16 back.
const fs = require('fs');
const zlib = require('zlib');
const crypto = require('crypto');
const path = require('path');
const { execFileSync } = require('child_process');

const REPO = path.resolve(__dirname, '../..');
const ASSETS = path.join(REPO, 'src/main/resources/assets/comedymonkeysaddons');
const JARS = {
  create: 'C:/Users/bones/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-281/484ecc6852a36a72fb50e905be79b3ce96181032/create-1.21.1-6.0.10-281-slim.jar',
  minecraft: path.join(REPO, 'build/moddev/artifacts/neoforge-21.1.248-client-extra-aka-minecraft-resources.jar'),
};
const uuid = () => crypto.randomUUID();

// ---------- palette (house style) ----------
const P = {
  steel: ['#2f343b', '#3c424a', '#4a5059'],
  panel: ['#363b42', '#3a3f46', '#3f454c'],
  steelLight: ['#4a5059', '#555b63', '#5f656e'],
  dark: ['#1f2327', '#24282d', '#2a2f35'],
  andesite: ['#6b6e6a', '#73766f', '#7c7f79'],
  walnut: ['#3b2a1f', '#4e3626', '#5a3f2c'],
  brass: ['#8f7638', '#9c8240', '#b08f45'],
  copper: ['#7a4a2e', '#8a5434', '#9c5f3a'],
  chamber: ['#4a5059', '#4f555d', '#545a62'],
  ceiling: ['#41474f', '#454b53', '#494f57'],
  tread: ['#33383f', '#363b42', '#393e45'],
};
const SEAM = '#2a2f35', RIVET = '#5f656e', CHAMBER_SEAM = '#3a3f46';
const HI = '#555b63', LO = '#2a2f35';
function hash(x, y, s) {
  let h = (x * 374761393 + y * 668265263 + s * 2147483647) | 0;
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return ((h ^ (h >>> 16)) >>> 0) / 4294967295;
}
function mot(mat, x, y, seed = 0) {
  const p = P[mat];
  const r = hash(x, y, seed + mat.length * 31 + mat.charCodeAt(0));
  return r < 0.2 ? p[0] : r < 0.82 ? p[1] : p[2];
}
// #rrggbb, or #rrggbbaa for a part-clear pixel (glass)
const rgba = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16), h.length > 7 ? parseInt(h.slice(7, 9), 16) : 255];
const edge = (x, y) => Math.min(x, y, 15 - x, 15 - y);
// A near-flat steel field with a faint second shade: big panels stay crisp instead of muddy
const FIELD = (x, y, s) => (hash(x, y, s) > 0.86 ? '#40464e' : '#3c424a');

function png(w, h, data) {
  const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
  const crc = (buf) => { let c = 0xffffffff; for (const b of buf) c = crcT[(c ^ b) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
  const chunk = (type, body) => { const len = Buffer.alloc(4); len.writeUInt32BE(body.length); const tb = Buffer.concat([Buffer.from(type), body]); const cr = Buffer.alloc(4); cr.writeUInt32BE(crc(tb)); return Buffer.concat([len, tb, cr]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) { raw[y * (w * 4 + 1)] = 0; Buffer.from(data.buffer, y * w * 4, w * 4).copy(raw, y * (w * 4 + 1) + 1); }
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
// A texture set: key -> PNG; each key exports as <prefix><key>.png
function textureSet() {
  const OWN = {};
  const paint = (key, fn) => {
    const d = new Uint8Array(16 * 16 * 4);
    for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
      const c = fn(x, y);
      if (c) d.set(rgba(c), (y * 16 + x) * 4);
    }
    OWN[key] = png(16, 16, d);
  };
  return { OWN, paint };
}

// ---------- shell panel painters (shared by the family) ----------
// trim: the metal of the outer pixel ring ('andesite' or 'brass'). The field sits just below the trim: dark top-left, light bottom-right.
const trimPx = (trim, x, y) => mot(trim, x, y);
function panelEdge(trim, x, y) {
  if (edge(x, y) === 0) return trimPx(trim, x, y);
  if (edge(x, y) === 1) return x === 1 || y === 1 ? LO : HI;
  return null;
}
const painters = {
  // Window frame: trim on the outer pixel, steel inside it, corner rivets in the trim metal
  front: (trim) => (x, y) => {
    if (edge(x, y) === 0) return trimPx(trim, x, y);
    if ((x === 1 || x === 14) && (y === 1 || y === 14)) return P[trim][2];
    return mot('panel', x, y, 1);
  },
  // A side panel; inner(x, y) paints whatever sits in the middle (a hatch, a gauge frame), or returns null for plain field
  side: (trim, inner) => (x, y) => panelEdge(trim, x, y) || inner(x, y) || FIELD(x, y, 3),
  // Back: one recessed shaft socket in a raised flange, and four bolts
  back: (trim, bolt = P[trim][1]) => (x, y) => {
    const e = panelEdge(trim, x, y);
    if (e) return e;
    const r = Math.hypot(x - 7.5, y - 7.5);
    if (r < 2.2) return '#1f2327';
    if (r < 3.2) return x + y < 14 ? '#5f656e' : x + y > 16 ? '#454b53' : '#4f555d';
    if (r < 3.8) return LO;
    if ((x === 4 || x === 11) && (y === 4 || y === 11)) return bolt;
    return FIELD(x, y, 4);
  },
  bottom: (trim) => (x, y) => panelEdge(trim, x, y) || (y === 7 ? SEAM : FIELD(x, y, 5)),
  // Glass: clear, with a few cool grey glints (never near-white)
  glass: () => {
    const G = { '4,2': '#83939a', '3,3': '#83939a', '2,4': '#6f7f86', '12,12': '#6f7f86', '11,13': '#6f7f86' };
    return (x, y) => G[x + ',' + y] || null;
  },
  chamber: () => (x, y) => {
    if (x % 8 === 0) return CHAMBER_SEAM;
    if ((x % 8 === 2 || x % 8 === 6) && (y === 1 || y === 14)) return RIVET;
    return mot('chamber', x, y, 6);
  },
  ceiling: () => (x, y) => mot('ceiling', x, y, 7),
  floor: () => (x, y) => ((x + y) % 4 === 0 ? '#41474f' : mot('tread', x, y, 8)),
  steel: () => (x, y) => mot('steel', x, y, 9),
  steelLight: () => (x, y) => mot('steelLight', x, y, 10),
  dark: () => (x, y) => mot('dark', x, y, 11),
  andesite: () => (x, y) => mot('andesite', x, y, 12),
  brass: () => (x, y) => (hash(x, y, 99) > 0.93 ? '#c9a75a' : mot('brass', x, y, 13)),
  copper: () => (x, y) => mot('copper', x, y, 20),
  walnut: () => (x, y) => (y % 4 === 3 ? '#2e2118' : mot('walnut', x + (y >> 2) * 5, y, 14)),
};

// Borrowed textures (stay references in game; embedded for the preview)
function borrowed(ref) {
  const [ns, p] = ref.includes(':') ? ref.split(':') : ['minecraft', ref];
  if (ns === 'comedymonkeysaddons') return fs.readFileSync(path.join(ASSETS, 'textures', p + '.png'));
  return execFileSync('unzip', ['-p', JARS[ns], `assets/${ns}/textures/${p}.png`], { maxBuffer: 1 << 26 });
}

// ---------- model DSL ----------
// C(name, from, to, faces, opts): faces maps direction -> texture key ('all' as a default); null hides a face.
// opts.rot = [axis, angle, origin] (or a list of them, preview-only); opts.uv = {dir: [u0,v0,u1,v1]}; opts.frot = {dir: deg}; opts.shade = false
const DIRS = ['north', 'east', 'south', 'west', 'up', 'down'];
const C = (name, from, to, faces, o = {}) => ({ name, from, to, faces, ...o });
function defaultUv(dir, f, t) {
  switch (dir) {
    case 'north': return [16 - t[0], 16 - t[1], 16 - f[0], 16 - f[1]];
    case 'south': return [f[0], 16 - t[1], t[0], 16 - f[1]];
    case 'east': return [16 - t[2], 16 - t[1], 16 - f[2], 16 - f[1]];
    case 'west': return [f[2], 16 - t[1], t[2], 16 - f[1]];
    case 'up': return [f[0], f[2], t[0], t[2]];
    default: return [f[0], 16 - t[2], t[0], 16 - f[2]];
  }
}
const clampUv = (uv) => uv.map((v) => Math.max(0, Math.min(16, v)));
// The projected UV for a face anywhere in -16..32: worked out in the block cell the face sits in, so it lands inside
// its 0-16 texture. A face that crosses a cell boundary is slid back inside, and one wider than a block is fitted to it.
// (Clamping an absolute UV instead squashes faces outside the middle block to a zero-width sliver, which in game
// samples the neighbouring sprites in the atlas: speckle that looks like z-fighting.)
function fitUv(dir, f, t) {
  const lf = [], lt = [];
  for (let a = 0; a < 3; a++) {
    const lo = Math.min(f[a], t[a]), hi = Math.max(f[a], t[a]), span = hi - lo;
    if (span >= 16) { lf.push(0); lt.push(16); continue; }
    let b = lo - Math.floor(lo / 16) * 16;
    if (b + span > 16) b = 16 - span;
    lf.push(b);
    lt.push(b + span);
  }
  return defaultUv(dir, lf, lt).map((v) => Math.round(v * 1000) / 1000);
}
const faceTex = (c, dir) => (dir in c.faces ? c.faces[dir] : c.faces.all);
const AXIS = 'create:block/axis', AXIS_TOP = 'create:block/axis_top';
// The shared shaft stub (cobble_generator_shaft, frozen), turned to the back as CobbleGeneratorRenderer.renderShaft does; preview only
const shaftStub = () => [C('shaft_stub', [6, 6, 12.5], [10, 10, 16.02], { north: AXIS_TOP, south: AXIS_TOP, all: AXIS },
  { uv: { north: [6, 6, 10, 10], south: [6, 6, 10, 10], east: [6, 0, 10, 4], west: [6, 0, 10, 4], up: [6, 0, 10, 4], down: [6, 0, 10, 4] }, frot: { east: 90, west: 90 } })];

// ---------- Blockbench preview (Generic Model, so it can hold clips) ----------
// textures: { prefix: { OWN } } keyed by the texture-key prefix used in faces, e.g. 'own:' -> harvester set
function preview(name, sets) {
  const textures = [], texIndex = new Map(), elements = [], groupByName = {}, animations = [];
  const texIdx = (key) => {
    const set = Object.keys(sets).find((p) => key.startsWith(p));
    const k = key.includes(':') || set ? key : Object.keys(sets)[0] + key;
    if (!texIndex.has(k)) {
      const sp = Object.keys(sets).find((p) => k.startsWith(p));
      const buf = sp ? sets[sp].OWN[k.slice(sp.length)] : borrowed(k);
      if (!buf) throw new Error('no texture ' + k);
      texIndex.set(k, textures.length);
      textures.push({ path: '', name: sp ? `${sets[sp].file}${k.slice(sp.length)}.png` : k.replace(/^.*\//, '') + '.png', folder: '', namespace: '', id: String(textures.length),
        width: 16, height: 16, uv_width: 16, uv_height: 16, particle: false, render_mode: 'default', render_sides: 'auto', frame_time: 1, frame_order_type: 'loop',
        visible: true, internal: true, saved: false, uuid: uuid(), source: 'data:image/png;base64,' + buf.toString('base64') });
    }
    return texIndex.get(k);
  };
  // Generic Model space: block centred on x/z 0, standing on y 0; scenes sit side by side along x
  const toBB = (p, off) => [p[0] - 8 + off[0], p[1] + off[1], p[2] - 8 + off[2]];
  function cube(c, off, shift = [0, 0, 0]) {
    const from = c.from.map((v, i) => v + shift[i]), to = c.to.map((v, i) => v + shift[i]);
    const faces = {};
    for (const dir of DIRS) {
      const t = faceTex(c, dir);
      if (!t) { faces[dir] = { uv: [0, 0, 0, 0], texture: null }; continue; }
      faces[dir] = { uv: (c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to), texture: texIdx(t) };
      if (c.frot && c.frot[dir]) faces[dir].rotation = c.frot[dir];
    }
    const rots = !c.rot ? [] : Array.isArray(c.rot[0]) ? c.rot : [c.rot];
    const rotation = [0, 0, 0];
    for (const r of rots) rotation[{ x: 0, y: 1, z: 2 }[r[0]]] = r[1];
    const o = rots.length ? rots[0][2].map((v, i) => v + shift[i]) : [(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2];
    const id = uuid();
    elements.push({ name: c.name, type: 'cube', uuid: id, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
      from: toBB(from, off), to: toBB(to, off), origin: toBB(o, off), rotation, autouv: 0, color: 0, inflate: 0, shade: c.shade !== false, faces });
    return id;
  }
  function group(gname, pivot, off, kids, o = {}) {
    const id = uuid();
    groupByName[gname] = { uuid: id, name: gname, origin: toBB(pivot, off) };
    return { name: gname, uuid: id, origin: toBB(pivot, off), rotation: [0, 0, 0], export: true, isOpen: !!o.open, locked: false, visibility: true, autouv: 0, color: 0, children: kids };
  }
  const FPS = 20;
  const r3 = (v) => Math.round(v * 1000) / 1000;
  // tracks: { bone: { channel: (t) => [x, y, z] } } sampled at 20 fps; steps: { bone: { channel: [[t, [x, y, z]], ...] } } held;
  // sounds: [[t, effect, file]] for the clip's Effects track
  function clip(cname, length, loop, tracks, steps = {}, sounds = []) {
    const animators = {};
    const add = (bone, kf) => {
      const g = groupByName[bone];
      if (!g) throw new Error('no bone ' + bone);
      (animators[g.uuid] = animators[g.uuid] || { name: bone, type: 'bone', rotation_global: false, quaternion_interpolation: false, keyframes: [] }).keyframes.push(kf);
    };
    for (const bone of Object.keys(tracks)) for (const ch of Object.keys(tracks[bone])) {
      const n = Math.round(length * FPS);
      for (let k = 0; k <= n; k++) {
        const t = k / FPS, v = tracks[bone][ch](t);
        add(bone, { channel: ch, data_points: [{ x: String(r3(v[0])), y: String(r3(v[1])), z: String(r3(v[2])) }], uuid: uuid(), time: r3(t), color: -1, interpolation: 'linear' });
      }
    }
    for (const bone of Object.keys(steps)) for (const ch of Object.keys(steps[bone])) for (const [t, v] of steps[bone][ch])
      add(bone, { channel: ch, data_points: [{ x: String(v[0]), y: String(v[1]), z: String(v[2]) }], uuid: uuid(), time: r3(t), color: -1, interpolation: 'step' });
    if (sounds.length) animators.effects = { name: 'Effects', type: 'effect', keyframes: sounds.map(([t, effect, file]) => ({ channel: 'sound',
      data_points: [{ effect, locator: '', file }], uuid: uuid(), time: r3(t), color: -1, interpolation: 'linear' })) };
    animations.push({ uuid: uuid(), name: cname, loop, override: false, length, snapping: FPS, selected: false, anim_time_update: '', blend_weight: '', start_delay: '', loop_delay: '', animators });
  }
  function write(out, outliner) {
    const bb = {
      meta: { format_version: '5.0', model_format: 'free', box_uv: false },
      name, model_identifier: '', visible_box: [1, 1, 0], variable_placeholders: '', variable_placeholder_buttons: [],
      timeline_setups: [], unhandled_root_fields: {}, resolution: { width: 16, height: 16 },
      elements, groups: Object.values(groupByName).map((g) => ({ name: g.name, uuid: g.uuid, origin: g.origin, rotation: [0, 0, 0], export: true, locked: false, visibility: true, color: 0, children: [], autouv: 0, isOpen: false })),
      outliner, textures, animations,
    };
    fs.writeFileSync(out, JSON.stringify(bb));
    console.error('wrote', out, elements.length, 'cubes', textures.length, 'textures', animations.length, 'clips');
  }
  return { cube, group, clip, write };
}

const SHOW = [1, 1, 1], HIDE = [0, 0, 0];
const smooth = (f) => f * f * (3 - 2 * f);
const lerp = (a, b, f) => a + (b - a) * f;
// A tray item: a 3 px sprite leaning back 60 degrees from upright, so it shows to someone standing at the window (preview of the tray helper)
const trayItem = (n, cx, tex) => [C(`item_${n}`, [cx - 1.5, 2.5, 2.2], [cx + 1.5, 5.5, 2.2], { north: tex, south: tex, all: null },
  { uv: { north: [0, 0, 16, 16], south: [0, 0, 16, 16] }, rot: ['x', 60, [cx, 2.5, 2.2]], shade: false })];

// An inventory view that fits a model in the slot: block/block's angle, scaled so the model (its elements, turned by their
// own rotations) fills ICON_FILL of the slot, and centred. For parts much smaller or larger than a block, whose
// block/block view would be a speck in the slot or spill out of it.
const ICON_FILL = 14.7 / 16;
function fitGui(elements) {
  const rot = [30, 225, 0], rad = (d) => (d * Math.PI) / 180;
  // a corner of an element, turned by its own rotation, then by the view (z, then y, then x, as the game does)
  const view = ([x0, y0, z0]) => {
    let x = x0 - 8, y = y0 - 8, z = z0 - 8, t;
    const [rx, ry, rz] = rot.map(rad);
    t = x * Math.cos(rz) - y * Math.sin(rz); y = x * Math.sin(rz) + y * Math.cos(rz); x = t;
    t = x * Math.cos(ry) + z * Math.sin(ry); z = -x * Math.sin(ry) + z * Math.cos(ry); x = t;
    t = y * Math.cos(rx) - z * Math.sin(rx); z = y * Math.sin(rx) + z * Math.cos(rx); y = t;
    return [x, y];
  };
  const turn = (p, r) => {
    if (!r) return p;
    const a = rad(r.angle), o = r.origin, q = p.map((n, i) => n - o[i]);
    const [i, j] = r.axis === 'x' ? [1, 2] : r.axis === 'y' ? [2, 0] : [0, 1];
    const u = q[i] * Math.cos(a) - q[j] * Math.sin(a), w = q[i] * Math.sin(a) + q[j] * Math.cos(a);
    q[i] = u; q[j] = w;
    return q.map((n, k) => n + o[k]);
  };
  let minX = Infinity, maxX = -Infinity, minY = Infinity, maxY = -Infinity;
  for (const e of elements)
    for (const x of [e.from[0], e.to[0]]) for (const y of [e.from[1], e.to[1]]) for (const z of [e.from[2], e.to[2]]) {
      const [px, py] = view(turn([x, y, z], e.rotation));
      minX = Math.min(minX, px); maxX = Math.max(maxX, px); minY = Math.min(minY, py); maxY = Math.max(maxY, py);
    }
  const s = Math.min(1.6, (16 * ICON_FILL) / Math.max(maxX - minX, maxY - minY));
  const r3 = (n) => Math.round(n * 1000) / 1000;
  return { rotation: rot, translation: [r3((-(minX + maxX) / 2) * s), r3((-(minY + maxY) / 2) * s), 0], scale: [r3(s), r3(s), r3(s)] };
}
module.exports = { REPO, ASSETS, P, SEAM, RIVET, HI, LO, hash, mot, edge, FIELD, png, textureSet, painters, borrowed, DIRS, C, defaultUv, fitUv, shaftStub, preview, SHOW, HIDE, smooth, lerp, trayItem, fitGui };
