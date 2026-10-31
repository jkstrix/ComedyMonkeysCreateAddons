// Game export for the Clockwork Wastes bots: node design/models/clockwork-wastes-bots.game-export.js [scuttler|sentry|custodian|all]
// Reads the approved previews (clockwork-wastes-<bot>.bbmodel and .png, from clockwork-wastes-bots.gen.js) and writes
// into the mod's resources, in the format Trade Bot's readers already use (TradeBotGeometry, ClipLibrary), as the
// Juggernaut's exporter does:
//   textures/entity/<bot>.png        the box-UV texture, cropped to the rows the cubes use
//   textures/entity/<bot>_glow.png   only the glowing parts' pixels (lenses, seams, studs, the sigil), drawn full bright
//   <bot>/geometry.json              bones (pivot, rotation) and cubes (from, to, uv) in Blockbench units
//   <bot>/clips.json                 every clip's sampled tracks, step tracks and sound cues
//   textures/entity/custodian_tether.png  (custodian) the tether cables' materials from the approved heart-hall preview
//                                    (clockwork-wastes-heart-hall.bbmodel): the patina'd cable, the void-alloy plugs and
//                                    sleeves, the starlight bead, 16 x 16 each side by side
// Geometry and tracks stay in Blockbench's frame; TradeBotGeometry converts them in one place.
// Sounds come last (the staging ladder): when design/sound/clockwork-wastes-bots/<bot>.cues.json exists its cues go in.
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const HERE = __dirname;
const RES = path.resolve(HERE, '../../src/main/resources/assets/comedymonkeysaddons');
// the glowing parts (the generator's starlight cubes: lenses, the bore, seams, studs, the pectoral sigil), by cube name;
// a cube under a bone named *_glow glows too
const GLOWS = /(_lit|_glow)$|^wrist_seam_|^pectoral|^studs_|^spring_seam$|^petal_\d_seam$/;
// clips that play round and round (the gaits, the Sentry's watching and aiming); 'hold' clips in the preview hold their
// last frame; the rest play once
const LOOPS = new Set(['idle', 'walk', 'watching', 'run', 'climb', 'aim']);

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

function exportBot(slug) {
  const model = JSON.parse(fs.readFileSync(path.join(HERE, `clockwork-wastes-${slug}.bbmodel`), 'utf8'));
  const cueFile = path.resolve(HERE, `../sound/clockwork-wastes-bots/${slug}.cues.json`);
  const cues = fs.existsSync(cueFile) ? JSON.parse(fs.readFileSync(cueFile, 'utf8')) : { clips: {} };

  // ---------- geometry ----------
  const groupByUuid = Object.fromEntries(model.groups.map((g) => [g.uuid, g]));
  const elementByUuid = Object.fromEntries(model.elements.map((e) => [e.uuid, e]));
  const bones = [], glowing = [];
  let maxV = 0;
  (function walk(node, parent, glowBone) {
    const g = groupByUuid[node.uuid];
    const bone = { name: g.name, parent, pivot: g.origin, rotation: g.rotation || [0, 0, 0], cubes: [] };
    bones.push(bone);
    const glows = glowBone || g.name.endsWith('_glow');
    for (const k of node.children) {
      if (typeof k !== 'string') { walk(k, g.name, glows); continue; }
      const e = elementByUuid[k];
      if (e.rotation && e.rotation.some((v) => v !== 0)) throw new Error(`${slug}: rotated cube ${e.name} (the bots have none)`);
      const out = { name: e.name, from: e.from, to: e.to, uv: e.uv_offset };
      const size = [0, 1, 2].map((a) => e.to[a] - e.from[a]);
      maxV = Math.max(maxV, e.uv_offset[1] + Math.ceil(size[2] + size[1]));
      bone.cubes.push(out);
      if (glows || GLOWS.test(e.name)) glowing.push({ u: e.uv_offset[0], v: e.uv_offset[1], w: Math.ceil(2 * (size[0] + size[2])), h: Math.ceil(size[2] + size[1]) });
    }
  })(model.outliner[0], null, false);
  const W = model.resolution.width, H = Math.max(16, 1 << Math.ceil(Math.log2(maxV)));

  // ---------- textures: the glow texture holds every pixel of each glowing cube's box-UV net ----------
  const src = readPng(fs.readFileSync(path.join(HERE, `clockwork-wastes-${slug}.png`)));
  if (src.w !== W) throw new Error(`${slug}: texture width ${src.w} != model ${W}`);
  const tex = src.data.slice(0, W * H * 4), glow = new Uint8Array(W * H * 4);
  let lit = 0;
  for (const r of glowing)
    for (let y = r.v; y < Math.min(H, r.v + r.h); y++)
      for (let x = r.u; x < Math.min(W, r.u + r.w); x++) {
        const o = (y * W + x) * 4;
        if (tex[o + 3] && !glow[o + 3]) { glow.set(tex.subarray(o, o + 4), o); lit++; }
      }
  fs.mkdirSync(path.join(RES, 'textures/entity'), { recursive: true });
  fs.writeFileSync(path.join(RES, `textures/entity/${slug}.png`), png(W, H, tex));
  fs.writeFileSync(path.join(RES, `textures/entity/${slug}_glow.png`), png(W, H, glow));
  fs.mkdirSync(path.join(RES, slug), { recursive: true });
  fs.writeFileSync(path.join(RES, `${slug}/geometry.json`), JSON.stringify({ texture: [W, H], bones }) + '\n');

  // ---------- clips ----------
  const num = (s) => +(+s).toFixed(3);
  const xyz = (k) => ['x', 'y', 'z'].map((i) => num(k.data_points[0][i]));
  const clips = model.animations.map((a) => {
    const tracks = {}, steps = {};
    for (const an of Object.values(a.animators)) {
      if (an.type !== 'bone') continue;
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
        ks.forEach((k, i) => { if (Math.abs(k.time - i / fps) > 0.002) throw new Error(`${slug} ${a.name} ${an.name}.${ch}: keys not evenly spaced`); });
        let values = ks.flatMap(xyz);
        // a channel that holds one value all through is one sample
        if (values.every((v, i) => v === values[i % 3])) values = values.slice(0, 3);
        (tracks[an.name] = tracks[an.name] || {})[ch] = { fps, interp: ks[0].interpolation, values };
      }
    }
    const mode = a.loop === 'hold' ? 'hold' : LOOPS.has(a.name) ? 'loop' : 'once';
    return { name: a.name, length: a.length, mode, root: 'visual', tracks, steps, cues: cues.clips[a.name] || [] };
  });
  fs.writeFileSync(path.join(RES, `${slug}/clips.json`), JSON.stringify({ clips }) + '\n');
  const kb = (f) => Math.round(fs.statSync(path.join(RES, f)).size / 1024) + ' KB';
  console.error(`${slug}: ${bones.length} bones, ${bones.reduce((n, b) => n + b.cubes.length, 0)} cubes, texture ${W}x${H} (${lit} glowing px), `
    + `${clips.length} clips: ${clips.map((c) => `${c.name} (${c.mode})`).join(', ')} (clips ${kb(`${slug}/clips.json`)})`);
}

// the Custodian's tether cables (drawn by its renderer): the heart-hall preview's textures, as painted there
const TETHER = ['wastes_tether.png', 'wastes_va.png', 'wastes_tether_pulse.png'];
function exportTether() {
  const hall = JSON.parse(fs.readFileSync(path.join(HERE, 'clockwork-wastes-heart-hall.bbmodel'), 'utf8'));
  const out = new Uint8Array(16 * TETHER.length * 16 * 4);
  TETHER.forEach((name, i) => {
    const t = hall.textures.find((x) => x.name === name);
    if (!t) throw new Error('the heart-hall preview has no ' + name);
    const img = readPng(Buffer.from(t.source.split(',')[1], 'base64'));
    if (img.w !== 16 || img.h !== 16) throw new Error(name + ' is not 16 x 16');
    for (let y = 0; y < 16; y++) out.set(img.data.subarray(y * 64, y * 64 + 64), (y * 16 * TETHER.length + i * 16) * 4);
  });
  fs.writeFileSync(path.join(RES, 'textures/entity/custodian_tether.png'), png(16 * TETHER.length, 16, out));
  console.error('custodian tether: ' + TETHER.join(', '));
}

const which = process.argv[2] || 'all';
for (const slug of ['scuttler', 'sentry', 'custodian'])
  if (which === slug || which === 'all') exportBot(slug);
if (which === 'custodian' || which === 'all') exportTether();
