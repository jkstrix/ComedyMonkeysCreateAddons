// Game export for the Clockwork Juggernaut: node design/models/clockwork-juggernaut.game-export.js
// Reads the approved preview (clockwork-juggernaut.bbmodel, its texture, and the sound cue file) and writes into the
// mod's resources, in the format Trade Bot's readers already use (TradeBotGeometry, ClipLibrary):
//   textures/entity/clockwork_juggernaut.png       the box-UV texture, cropped to the rows the cubes use
//   textures/entity/clockwork_juggernaut_glow.png  only the frost-blue rune pixels, drawn full bright in game
//   clockwork_juggernaut/geometry.json             bones (pivot, rotation) and cubes (from, to, uv) in Blockbench units
//   clockwork_juggernaut/clips.json                every clip's sampled tracks, step tracks and sound cues
// Preview-only props (the player dummy, steam, sparks, arcs, the stomp ring) are left out: the game draws those as
// particles. Geometry and tracks stay in Blockbench's frame; TradeBotGeometry converts them in one place.
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const HERE = __dirname;
const RES = path.resolve(HERE, '../../src/main/resources/assets/comedymonkeysaddons');
const model = JSON.parse(fs.readFileSync(path.join(HERE, 'clockwork-juggernaut.bbmodel'), 'utf8'));
const cues = JSON.parse(fs.readFileSync(path.resolve(HERE, '../sound/clockwork-juggernaut/clockwork-juggernaut.cues.json'), 'utf8'));
// drawn as particles in game (and the preview's scale figure)
const PREVIEW_ONLY = new Set(['held_player', 'vent_steam', 'pipe_steam_left', 'pipe_steam_right', 'sparks', 'arcs', 'stomp_ring', 'prop_scale']);
// pixels that glow: the rune colours (design/models/clockwork-juggernaut.gen.js, P.rune and P.runeHi)
const GLOW = ['#4f8fae', '#4a87a4', '#7ab8d0'].map((h) => [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16)));
// how each clip ends: run_down and death hold their last frame until the game moves on
const HOLD = new Set(['run_down', 'death', 'dormant_settle']);

// ---------- geometry ----------
const groupByUuid = Object.fromEntries(model.groups.map((g) => [g.uuid, g]));
const elementByUuid = Object.fromEntries(model.elements.map((e) => [e.uuid, e]));
const bones = [], kept = new Set();
let maxV = 0;
function walk(node, parent) {
  const g = groupByUuid[node.uuid];
  if (PREVIEW_ONLY.has(g.name)) return;
  const bone = { name: g.name, parent, pivot: g.origin, rotation: g.rotation || [0, 0, 0], cubes: [] };
  bones.push(bone);
  kept.add(g.name);
  for (const k of node.children) {
    if (typeof k !== 'string') { walk(k, g.name); continue; }
    const e = elementByUuid[k];
    const out = { name: e.name, from: e.from, to: e.to, uv: e.uv_offset };
    maxV = Math.max(maxV, e.uv_offset[1] + Math.ceil(e.to[2] - e.from[2]) + Math.ceil(e.to[1] - e.from[1]));
    // a rotated cube becomes its own small bone (ModelPart cubes can't rotate), pivoting on the cube's origin
    if (e.rotation && e.rotation.some((v) => v !== 0)) bones.push({ name: e.name + '#r', parent: g.name, pivot: e.origin, rotation: e.rotation, cubes: [out] });
    else bone.cubes.push(out);
  }
}
for (const root of model.outliner) walk(root, null);
const W = model.resolution.width, H = 1 << Math.ceil(Math.log2(maxV));

// ---------- textures ----------
function readPng(buf) {
  let p = 8, w, h; const idat = [];
  while (p < buf.length) {
    const len = buf.readUInt32BE(p), type = buf.toString('ascii', p + 4, p + 8);
    if (type === 'IHDR') { w = buf.readUInt32BE(p + 8); h = buf.readUInt32BE(p + 12); if (buf[p + 16] !== 8 || buf[p + 17] !== 6) throw new Error('expected 8-bit RGBA'); }
    if (type === 'IDAT') idat.push(buf.slice(p + 8, p + 8 + len));
    p += 12 + len;
  }
  const raw = zlib.inflateSync(Buffer.concat(idat)), out = new Uint8Array(w * h * 4);
  for (let y = 0; y < h; y++) {
    if (raw[y * (w * 4 + 1)] !== 0) throw new Error('expected unfiltered rows (the generator writes them so)');
    raw.copy(out, y * w * 4, y * (w * 4 + 1) + 1, (y + 1) * (w * 4 + 1));
  }
  return { w, h, data: out };
}
function png(w, h, data) {
  const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
  const crc = (b) => { let c = 0xffffffff; for (const x of b) c = crcT[(c ^ x) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
  const chunk = (type, body) => { const len = Buffer.alloc(4); len.writeUInt32BE(body.length); const tb = Buffer.concat([Buffer.from(type), body]); const cr = Buffer.alloc(4); cr.writeUInt32BE(crc(tb)); return Buffer.concat([len, tb, cr]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) Buffer.from(data.buffer, data.byteOffset + y * w * 4, w * 4).copy(raw, y * (w * 4 + 1) + 1);
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))]);
}
const src = readPng(fs.readFileSync(path.join(HERE, 'clockwork-juggernaut.png')));
if (src.w !== W) throw new Error('texture width ' + src.w + ' != model ' + W);
const tex = src.data.slice(0, W * H * 4), glow = new Uint8Array(W * H * 4);
let lit = 0;
for (let i = 0; i < W * H; i++) {
  const o = i * 4;
  if (tex[o + 3] && GLOW.some(([r, g, b]) => tex[o] === r && tex[o + 1] === g && tex[o + 2] === b)) { glow.set(tex.subarray(o, o + 4), o); lit++; }
}
fs.mkdirSync(path.join(RES, 'textures/entity'), { recursive: true });
fs.writeFileSync(path.join(RES, 'textures/entity/clockwork_juggernaut.png'), png(W, H, tex));
fs.writeFileSync(path.join(RES, 'textures/entity/clockwork_juggernaut_glow.png'), png(W, H, glow));

fs.mkdirSync(path.join(RES, 'clockwork_juggernaut'), { recursive: true });
fs.writeFileSync(path.join(RES, 'clockwork_juggernaut/geometry.json'), JSON.stringify({ texture: [W, H], bones }) + '\n');

// ---------- clips ----------
const num = (s) => +(+s).toFixed(3);
const xyz = (k) => ['x', 'y', 'z'].map((i) => num(k.data_points[0][i]));
// the spring leap's flight is real in game (the server carries him to where the target stood), so the preview's arc
// and forward travel come out of the root; the ground lock stays (clockwork-juggernaut.clips.js, clip 12)
const lin = (t, a, b) => (t <= a ? 0 : t >= b ? 1 : (t - a) / (b - a));
const LEAP_AIR = (t) => (t > 1.55 && t < 2.1 ? Math.sin(Math.PI * lin(t, 1.55, 2.1)) : 0);
const clips = model.animations.filter((a) => a.name !== 'pose_sheet').map((a) => {
  const tracks = {}, steps = {};
  for (const an of Object.values(a.animators)) {
    if (an.type !== 'bone' || !kept.has(an.name)) continue;
    for (const ch of ['rotation', 'position', 'scale']) {
      const ks = an.keyframes.filter((k) => k.channel === ch).sort((p, q) => p.time - q.time);
      if (!ks.length) continue;
      // a channel with any step key (a part hidden and shown) goes as steps, whole
      if (ks.some((k) => k.interpolation === 'step')) {
        const keys = [];
        for (const k of ks) { const v = xyz(k); const last = keys[keys.length - 1]; if (!last || last.slice(1).some((x, i) => x !== v[i])) keys.push([num(k.time), ...v]); }
        (steps[an.name] = steps[an.name] || {})[ch] = keys;
        continue;
      }
      const fps = ks.length > 1 ? Math.round(1 / (ks[1].time - ks[0].time)) : 20;
      ks.forEach((k, i) => { if (Math.abs(k.time - i / fps) > 0.002) throw new Error(`${a.name} ${an.name}.${ch}: keys not evenly spaced`); });
      let values = ks.flatMap((k) => {
        const v = xyz(k);
        if (a.name === 'leap' && an.name === 'juggernaut' && ch === 'position') return [0, num(v[1] - 34 * LEAP_AIR(k.time)), 0];
        return v;
      });
      // a channel that holds one value all through is one sample
      if (values.every((v, i) => v === values[i % 3])) values = values.slice(0, 3);
      (tracks[an.name] = tracks[an.name] || {})[ch] = { fps, interp: ks[0].interpolation, values };
    }
  }
  const mode = HOLD.has(a.name) ? 'hold' : a.loop === 'loop' ? 'loop' : 'once';
  return { name: a.name, length: a.length, mode, root: 'visual', tracks, steps, cues: cues.clips[a.name] || [] };
});
fs.writeFileSync(path.join(RES, 'clockwork_juggernaut/clips.json'), JSON.stringify({ clips }) + '\n');
const kb = (f) => Math.round(fs.statSync(path.join(RES, f)).size / 1024) + ' KB';
console.error(`game export: ${bones.length} bones, ${bones.reduce((n, b) => n + b.cubes.length, 0)} cubes, texture ${W}x${H} (${lit} glowing px), ${clips.length} clips (clips ${kb('clockwork_juggernaut/clips.json')}, texture ${kb('textures/entity/clockwork_juggernaut.png')})`);
