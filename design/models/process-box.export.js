// Writes the approved Auto Harvester and Animal Pen models into the mod: block and item model JSON, and textures.
// Usage: node design/models/process-box.export.js   (regenerates the previews too, since it loads the generators)
const fs = require('fs');
const path = require('path');
const { ASSETS, C, defaultUv, DIRS } = require('./process-box.lib.js');
const H = require('./auto-harvester.gen.js');
const PN = require('./animal-pen.gen.js');

const NS = 'comedymonkeysaddons';
const MODELS = path.join(ASSETS, 'models');
const TEXTURES = path.join(ASSETS, 'textures/block');

// texture key in a part list -> resource location
function harvesterRef(key) { return key.includes(':') ? key : `${NS}:block/auto_harvester_${key}`; }
function penRef(key) {
  if (key.startsWith('pen:')) return `${NS}:block/animal_pen_${key.slice(4)}`;
  if (key.startsWith('brass:')) return `${NS}:block/brass_animal_pen_${key.slice(6)}`;
  return key;
}

// A part list -> Java block model JSON (cutout, no AO, like every model in the family)
function blockModel(cubes, ref, particle) {
  const vars = new Map();
  const v = (r) => { if (!vars.has(r)) vars.set(r, r.replace(/^.*[/:]/, '').replace(/[^a-z0-9_]/g, '_')); return '#' + vars.get(r); };
  const elements = cubes.map((c) => {
    const faces = {};
    for (const dir of DIRS) {
      const t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      const uv = ((c.uv && c.uv[dir]) || defaultUv(dir, c.from, c.to)).map((n) => Math.round(Math.max(0, Math.min(16, n)) * 1000) / 1000);
      faces[dir] = { uv, texture: v(ref(t)) };
      if (c.frot && c.frot[dir]) faces[dir].rotation = c.frot[dir];
    }
    const el = { name: c.name, from: c.from, to: c.to };
    if (c.rot) {
      if (Array.isArray(c.rot[0])) throw new Error(c.name + ': several rotations can\'t export');
      if (![-45, -22.5, 0, 22.5, 45].includes(c.rot[1])) throw new Error(c.name + ': angle ' + c.rot[1]);
      el.rotation = { origin: c.rot[2], axis: c.rot[0], angle: c.rot[1] };
    }
    if (c.shade === false) el.shade = false;
    el.faces = faces;
    for (const p of [...c.from, ...c.to]) if (p < -16 || p > 32) throw new Error(c.name + ' is outside the model bounds');
    return el;
  });
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  return { parent: 'block/block', render_type: 'minecraft:cutout', ambientocclusion: false, textures, elements };
}
const writeJson = (rel, obj) => { fs.writeFileSync(path.join(MODELS, rel), JSON.stringify(obj, null, 2) + '\n'); console.log('wrote models/' + rel); };
const writePng = (name, buf) => { fs.writeFileSync(path.join(TEXTURES, name + '.png'), buf); console.log('wrote textures/block/' + name + '.png'); };
const shift = (list, d) => list.map((c) => ({ ...c, from: c.from.map((n, i) => n + d[i]), to: c.to.map((n, i) => n + d[i]),
  rot: c.rot ? [c.rot[0], c.rot[1], c.rot[2].map((n, i) => n + d[i])] : undefined }));

// ---------- Auto Harvester ----------
writeJson('block/auto_harvester.json', blockModel(H.shell('soil'), harvesterRef, 'side'));
writeJson('block/auto_harvester_dirt.json', blockModel(H.shell('dirt'), harvesterRef, 'side'));
writeJson('block/auto_harvester_carriage.json', blockModel(H.carriage(), harvesterRef, 'steel'));
writeJson('block/auto_harvester_pole.json', blockModel(H.pole(), harvesterRef, 'steel_light'));
writeJson('block/auto_harvester_reel.json', blockModel(H.reel(), harvesterRef, 'walnut'));
writeJson('block/auto_harvester_saw.json', blockModel(H.saw(), harvesterRef, 'comedymonkeysaddons:block/auto_harvester_saw'));
// Item: no renderer runs for it, so it's the shell with the head parked and the fallback crop grown
const crop = JSON.parse(fs.readFileSync(path.join(MODELS, 'block/auto_harvester_crop.json'), 'utf8')).elements.map((e, i) =>
  C(`crop_${i}`, e.from, e.to, Object.fromEntries(Object.entries(e.faces).map(([d]) => [d, 'comedymonkeysaddons:block/auto_harvester_crop'])),
    { uv: Object.fromEntries(Object.entries(e.faces).map(([d, f]) => [d, f.uv])), rot: e.rotation ? [e.rotation.axis, e.rotation.angle, e.rotation.origin] : undefined }));
writeJson('item/auto_harvester.json', blockModel([...H.shell('soil'), ...H.carriage(), ...shift(H.reel(), [H.PARK_X - 8, H.PARK_Y - 8, 0]), ...crop], harvesterRef, 'side'));
for (const [key, buf] of Object.entries(H.OWN)) writePng('auto_harvester_' + key, buf);

// ---------- Animal Pens ----------
writeJson('block/animal_pen.json', blockModel(PN.pen('pen'), penRef, 'pen:side'));
writeJson('block/brass_animal_pen.json', blockModel(PN.pen('brass'), penRef, 'brass:side'));
// The milk column: a 1 px tall unit in both side slots, which the renderer lifts to the slot foot and stretches to the level
const G = PN.GAUGE;
writeJson('block/animal_pen_milk.json', blockModel([
  C('milk_west', [0.25, 0, G.z0 + 0.05], [0.25, 1, G.z1 - 0.05], { west: 'pen:milk', all: null }, { uv: { west: [G.z0, 16 - G.y1, G.z1, 16 - G.y0] }, shade: false }),
  C('milk_east', [15.75, 0, G.z0 + 0.05], [15.75, 1, G.z1 - 0.05], { east: 'pen:milk', all: null }, { uv: { east: [16 - G.z1, 16 - G.y1, 16 - G.z0, 16 - G.y0] }, shade: false }),
], penRef, 'pen:milk'));
const PREVIEW_ONLY = new Set(['cow', 'chicken', 'sheep', 'beak']);
for (const [key, buf] of Object.entries(PN.PEN.OWN)) if (!PREVIEW_ONLY.has(key)) writePng('animal_pen_' + key, buf);
for (const [key, buf] of Object.entries(PN.BRASS.OWN)) writePng('brass_animal_pen_' + key, buf);
