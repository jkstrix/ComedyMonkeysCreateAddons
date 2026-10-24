// Builds a "before" .bbmodel from the mod's Java block models, posed the way the renderers draw them.
// Usage: node gen-before.js <scene.json> <out.bbmodel>
const fs = require('fs'), path = require('path'), crypto = require('crypto');
const { execFileSync } = require('child_process');

const REPO = 'E:/Repos/Flight Goggles Compat';
const ASSETS = REPO + '/src/main/resources/assets/comedymonkeysaddons';
const JARS = {
  create: 'C:/Users/bones/.gradle/caches/modules-2/files-2.1/com.simibubi.create/create-1.21.1/6.0.10-281/484ecc6852a36a72fb50e905be79b3ce96181032/create-1.21.1-6.0.10-281-slim.jar',
  minecraft: REPO + '/build/moddev/artifacts/neoforge-21.1.248-client-extra-aka-minecraft-resources.jar',
};

const [, , sceneFile, outFile] = process.argv;
const scene = JSON.parse(fs.readFileSync(sceneFile, 'utf8'));
const uuid = () => crypto.randomUUID();

function readModel(name) {
  return JSON.parse(fs.readFileSync(`${ASSETS}/models/block/${name}.json`, 'utf8'));
}

function textureBytes(ref) {
  const [ns, p] = ref.includes(':') ? ref.split(':') : ['minecraft', ref];
  if (ns === 'comedymonkeysaddons') return fs.readFileSync(`${ASSETS}/textures/${p}.png`);
  return execFileSync('unzip', ['-p', JARS[ns], `assets/${ns}/textures/${p}.png`], { maxBuffer: 1 << 26 });
}

const textures = [];
const texIndex = new Map();
function texture(ref) {
  if (!texIndex.has(ref)) {
    const png = textureBytes(ref);
    texIndex.set(ref, textures.length);
    textures.push({
      name: ref.replace(/^.*\//, '') + '.png', id: String(textures.length), uuid: uuid(), folder: '', namespace: '',
      path: '', relative_path: '', particle: false, render_mode: 'default', render_sides: 'auto', frame_time: 1,
      frame_order_type: 'loop', saved: true, mode: 'bitmap', uv_width: 16, uv_height: 16,
      source: 'data:image/png;base64,' + png.toString('base64'),
    });
  }
  return texIndex.get(ref);
}

function resolve(map, key) {
  let v = key;
  for (let i = 0; i < 8 && v && v.startsWith('#'); i++) v = map[v.slice(1)];
  return v;
}

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

// A transform step maps a point; steps run in order. Supports translate, scale and a 180° turn about Y.
function applyPoint(p, steps) {
  let [x, y, z] = p;
  for (const s of steps) {
    if (s.translate) { x += s.translate[0]; y += s.translate[1]; z += s.translate[2]; }
    if (s.scale) { const o = s.scale.origin || [0, 0, 0]; x = o[0] + (x - o[0]) * s.scale.by[0]; y = o[1] + (y - o[1]) * s.scale.by[1]; z = o[2] + (z - o[2]) * s.scale.by[2]; }
    if (s.rotY180) { x = 16 - x; z = 16 - z; }
  }
  return [x, y, z];
}
const SWAP = { north: 'south', south: 'north', east: 'west', west: 'east', up: 'up', down: 'down' };

function convert(part) {
  const model = readModel(part.model);
  const steps = (part.steps || []).concat(part.offset ? [{ translate: part.offset }] : []);
  const turns = steps.filter((s) => s.rotY180).length % 2 === 1;
  const children = [];
  const elements = [];
  (model.elements || []).forEach((el, i) => {
    const a = applyPoint(el.from, steps), b = applyPoint(el.to, steps);
    const from = [0, 1, 2].map((k) => Math.min(a[k], b[k]));
    const to = [0, 1, 2].map((k) => Math.max(a[k], b[k]));
    const faces = {};
    for (const dir of ['north', 'east', 'south', 'west', 'up', 'down']) {
      const src = el.faces && el.faces[dir];
      const target = turns ? SWAP[dir] : dir;
      if (!src) { faces[target] = { uv: [0, 0, 0, 0], texture: null }; continue; }
      const ref = resolve(model.textures || {}, src.texture);
      const face = { uv: src.uv || defaultUv(dir, el.from, el.to), texture: ref && !ref.startsWith('#') ? texture(ref) : null };
      if (src.rotation) face.rotation = src.rotation;
      faces[target] = face;
    }
    let rotation = [0, 0, 0], origin = applyPoint(el.rotation ? el.rotation.origin : [8, 8, 8], steps);
    if (el.rotation) {
      const k = { x: 0, y: 1, z: 2 }[el.rotation.axis];
      rotation[k] = (turns && k !== 1 ? -1 : 1) * el.rotation.angle;
    }
    const id = uuid();
    elements.push({ name: el.name || `${part.model}_${i}`, type: 'cube', uuid: id, from, to, origin, rotation, faces,
      autouv: 0, box_uv: false, rescale: false, locked: false, render_order: 'default', allow_mirror_modeling: true,
      color: 0, inflate: 0, light_emission: 0, shade: el.shade !== false });
    children.push(id);
  });
  return { elements, group: { name: part.name, origin: [8, 8, 8], color: 0, uuid: uuid(), export: true, isOpen: false,
    locked: false, visibility: true, autouv: 0, children } };
}

const allElements = [];
const outliner = [];
for (const g of scene.groups) {
  const parts = g.parts.map(convert);
  parts.forEach((p) => allElements.push(...p.elements));
  outliner.push({ name: g.name, origin: [8, 8, 8], color: 0, uuid: uuid(), export: true, isOpen: true, locked: false,
    visibility: true, autouv: 0, children: parts.map((p) => p.group) });
}

const bb = {
  meta: { format_version: '4.10', model_format: 'java_block', box_uv: false },
  name: scene.name, parent: '', ambientocclusion: false, front_gui_light: false, visible_box: [1, 1, 0],
  variable_placeholders: '', variable_placeholder_buttons: [], unhandled_root_fields: {},
  resolution: { width: 16, height: 16 }, elements: allElements, outliner, textures, display: {},
};
fs.writeFileSync(outFile, JSON.stringify(bb));
console.log(`${outFile}: ${allElements.length} elements, ${textures.length} textures`);
