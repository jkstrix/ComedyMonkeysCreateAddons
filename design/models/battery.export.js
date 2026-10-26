// Writes the approved Battery and Industrial Battery into the mod: the Battery's multipart blockstate and its parts (the
// base, and a terminal or a bus bar on each of its sides and back), the renderer's parts (tube fill, ember, pulse, lit
// lamps), the item models by charge, and the Industrial Battery as 16 px tiles by height, layer and position, with an
// index the renderer reads (assets/comedymonkeysaddons/battery_tower.json). Only the battery_* textures the models use.
// Usage: node design/models/battery.export.js
const fs = require('fs');
const os = require('os');
const path = require('path');
const crypto = require('crypto');
const { ASSETS, fitUv, DIRS, fitGui } = require('./process-box.lib.js');
// the generator also writes its Blockbench preview: somewhere it won't change the design file
process.argv[2] = path.join(os.tmpdir(), 'battery-export.bbmodel');
const B = require('./battery.gen.js');

const NS = 'comedymonkeysaddons';
const ref = (key) => {
  if (key.startsWith('bat:')) return `${NS}:block/battery_${key.slice(4)}`;
  if (key.includes(':')) return key;
  throw new Error('texture key without a block: ' + key);
};
const used = new Set();
const r3 = (n) => Math.round(n * 1000) / 1000;

function blockModel(cubes, particle, extra = {}, renderType = 'minecraft:cutout') {
  const vars = new Map();
  const v = (r) => { if (!vars.has(r)) vars.set(r, r.replace(/^.*[/:]/, '').replace(/^battery_/, '').replace(/[^a-z0-9_]/g, '_')); return '#' + vars.get(r); };
  // an element with no faces left is invalid to Minecraft, so it's dropped
  const elements = cubes.filter((c) => DIRS.some((d) => (d in c.faces ? c.faces[d] : c.faces.all))).map((c) => {
    const faces = {};
    for (const dir of DIRS) {
      const t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      if (t.startsWith('bat:')) used.add(t);
      const raw = (c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to);
      if (raw.some((n) => n < -0.001 || n > 16.001)) throw new Error(`${c.name} ${dir}: UV ${raw} is off the texture`);
      faces[dir] = { uv: raw.map(r3), texture: v(ref(t)) };
    }
    const el = { name: c.name, from: c.from.map(r3), to: c.to.map(r3) };
    if (c.rot) {
      const rot = Array.isArray(c.rot[0]) ? c.rot[0] : c.rot;
      if (![-45, -22.5, 0, 22.5, 45].includes(rot[1])) throw new Error(c.name + ': angle ' + rot[1]);
      el.rotation = { origin: rot[2].map(r3), axis: rot[0], angle: rot[1] };
    }
    if (c.shade === false) el.shade = false;
    if (c.glow) el.neoforge_data = { block_light: 15, sky_light: 15 };
    el.faces = faces;
    for (const p of [...el.from, ...el.to]) if (p < -16 || p > 32) throw new Error(c.name + ' is outside the model bounds: ' + JSON.stringify([el.from, el.to]));
    return el;
  });
  used.add(particle);
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  return { parent: 'block/block', render_type: renderType, textures, elements, ...extra };
}
function writeJson(rel, obj) {
  const file = path.join(ASSETS, rel);
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, JSON.stringify(obj, null, 2) + '\n');
}
const write = (name, cubes, particle = 'bat:plate', extra, renderType) => writeJson(`models/block/${name}.json`, blockModel(cubes, particle, extra, renderType));

// ---------- the Battery: its parts ----------
// the base is everything but the two sides' and the back's connectors; each of those comes as a terminal or a bus bar
const isSide = (c, side) => (side === 'west' ? /_west(_|$)/ : side === 'east' ? /_east(_|$)/ : /^(terminal_back|post_back|collar_back|bus_south)/).test(c.name);
const plain = B.battery({}), joined = B.battery({ joined: ['west', 'east', 'south'] });
// the flow lamps are in the base, dark; the renderer lays the lit ones over them. The tubes' glass is part-clear: its own
// translucent part (cutout would draw its faint streaks solid)
const isGlass = (c) => c.name.startsWith('tube_glass_');
const base = [...plain.filter((c) => !isGlass(c) && !['west', 'east', 'south'].some((s) => isSide(c, s))), ...B.lamps(false, false)];
write('battery', base);
write('battery_glass', plain.filter(isGlass), 'bat:plate', {}, 'minecraft:translucent');
for (const side of ['west', 'east', 'south']) {
  const n = side === 'west' ? 'left' : side === 'east' ? 'right' : 'back';
  write(`battery_${n}_terminal`, plain.filter((c) => isSide(c, side)));
  write(`battery_${n}_bus`, joined.filter((c) => isSide(c, side)));
}
// formed into an Industrial Battery: the tower is drawn by its renderer, so the block shows nothing (a particle only)
write('battery_formed', []);
// the renderer's parts
write('battery_fill', B.tubeFill(1));
write('battery_ember', B.tubeFill(0));
write('battery_pulse', B.pulses());
// lit lamps sit 0.25 px in front of the dark ones in the base
const lit = (n, tex) => [{ name: n, from: n === 'lamp_in' ? [6, 11, 0.5] : [8.2, 11, 0.5], to: n === 'lamp_in' ? [7.8, 12.75, 0.5] : [10, 12.75, 0.5],
  faces: { north: tex, all: null }, uv: { north: [0, 0, 16, 16] }, glow: true }];
write('battery_lamp_in', lit('lamp_in', 'bat:lamp_in_on'));
write('battery_lamp_out', lit('lamp_out', 'bat:lamp_out_on'));

// the multipart blockstate: the base turned to its facing, each connector side as a terminal or, joined, a bus bar
const Y = { north: 0, east: 90, south: 180, west: 270 };
const multipart = [{ when: { formed: 'true' }, apply: { model: `${NS}:block/battery_formed` } }];
for (const [f, y] of Object.entries(Y)) {
  multipart.push({ when: { formed: 'false', facing: f }, apply: { model: `${NS}:block/battery`, y } });
  multipart.push({ when: { formed: 'false', facing: f }, apply: { model: `${NS}:block/battery_glass`, y } });
  for (const n of ['left', 'right', 'back'])
    for (const j of ['false', 'true'])
      multipart.push({ when: { formed: 'false', facing: f, [n]: j }, apply: { model: `${NS}:block/battery_${n}_${j === 'true' ? 'bus' : 'terminal'}`, y } });
}
writeJson('blockstates/battery.json', { multipart });

// the item: the Battery with its tubes filled to its charge (0 empty, 1 to 4 quarters), chosen by the charge property;
// translucent, for its glass
const itemModel = (f) => {
  const m = blockModel([...plain.filter((c) => !isGlass(c)), ...B.lamps(false, false), ...B.tubeFill(f), ...plain.filter(isGlass)], 'bat:plate', {},
    'minecraft:translucent');
  m.display = { gui: fitGui(m.elements) };
  return m;
};
for (let q = 0; q <= 4; q++) writeJson(`models/item/battery_${q}.json`, itemModel(q / 4));
writeJson('models/item/battery.json', { ...itemModel(0), overrides: [1, 2, 3, 4].map((q) => ({ predicate: { [`${NS}:charge`]: (q - 0.5) / 4 }, model: `${NS}:item/battery_${q}` })) });

// ---------- the Industrial Battery: tiles ----------
// The hardware is built in the tower's own coordinates; it's cut at the block lines into the tiles it crosses. A cut
// face between two pieces of one part is left out.
function cutInto(cube, H) {
  let pieces = [{ ...cube, from: [...cube.from], to: [...cube.to], cut: {} }];
  for (let a = 0; a < 3; a++) {
    const lines = a === 1 ? Array.from({ length: H - 1 }, (_, k) => (k + 1) * 16) : [16, 32];
    for (const L of lines) {
      const next = [];
      for (const p of pieces) {
        if (p.from[a] < L - 1e-6 && p.to[a] > L + 1e-6) {
          const lo = { ...p, from: [...p.from], to: [...p.to], cut: { ...p.cut } }, hi = { ...p, from: [...p.from], to: [...p.to], cut: { ...p.cut } };
          lo.to[a] = L; hi.from[a] = L;
          lo.cut[['east', 'up', 'south'][a]] = true;
          hi.cut[['west', 'down', 'north'][a]] = true;
          next.push(lo, hi);
        } else next.push(p);
      }
      pieces = next;
    }
  }
  return pieces.map((p) => {
    const faces = { ...p.faces };
    for (const d of Object.keys(p.cut)) faces[d] = null;
    const cell = (a, max) => Math.max(0, Math.min(max, Math.floor((p.from[a] + p.to[a]) / 2 / 16)));
    return { ...p, faces, cut: undefined, ix: cell(0, 2), layer: cell(1, H - 1), iz: cell(2, 2) };
  });
}
const index = {}, unique = new Map();
let tiles = 0;
for (let H = 3; H <= 9; H++) {
  const grid = Array.from({ length: H }, () => Array.from({ length: 3 }, () => [[], [], []]));
  for (let layer = 0; layer < H; layer++) for (let iz = 0; iz < 3; iz++) for (let ix = 0; ix < 3; ix++) grid[layer][iz][ix].push(...B.tile(ix, iz, layer, H));
  for (const c of B.substation(H)) for (const p of cutInto(c, H)) {
    const { ix, layer, iz } = p;
    grid[layer][iz][ix].push({ ...p, from: [p.from[0] - ix * 16, p.from[1] - layer * 16, p.from[2] - iz * 16], to: [p.to[0] - ix * 16, p.to[1] - layer * 16, p.to[2] - iz * 16] });
  }
  index[H] = grid.map((rows) => rows.map((row) => row.map((list) => {
    if (!list.length) return null;
    const json = JSON.stringify(blockModel(list, 'bat:tower'));
    if (!unique.has(json)) {
      const name = 'battery_tower_' + crypto.createHash('sha1').update(json).digest('hex').slice(0, 10);
      unique.set(json, name);
      write(name, list, 'bat:tower');
      tiles++;
    }
    return unique.get(json);
  })));
}
// the tower's renderer parts, about the bottom-front-middle block (the tower's x 16-32): the level fill one block tall
// from y 0 (the renderer stretches it from the plinth to the level), its ember where the channel starts, a pulse and the
// forming ripple from y 0 (moved up the channel), and one short arc piece along x (posed by the renderer)
const toMaster = (list) => list.map((c) => ({ ...c, from: [c.from[0] - 16, c.from[1], c.from[2]], to: [c.to[0] - 16, c.to[1], c.to[2]] }));
write('battery_tower_level', toMaster([{ name: 'level', from: [21, 0, 0.75], to: [27, 16, 0.75], faces: { north: 'bat:fill', all: null }, glow: true }]));
write('battery_tower_ember', toMaster(B.levelFill(9, 0)));
write('battery_tower_pulse', toMaster([{ name: 'pulse', from: [21, 0, 0.5], to: [27, 2, 0.5], faces: { north: 'bat:pulse', all: null }, glow: true }]));
write('battery_tower_ripple', toMaster([{ name: 'ripple', from: [21, 0, 0.45], to: [27, 8, 0.45], faces: { north: 'bat:pulse', all: null }, glow: true }]));
write('battery_arc', [{ name: 'arc', from: [0, -0.25, -0.25], to: [16, 0.25, 0.25], faces: { all: 'bat:arc' }, glow: true }]);
writeJson('battery_tower.json', index);

let n = 0;
for (const key of used) {
  const k = key.slice(4), buf = B.T.OWN[k];
  if (!buf) throw new Error('no texture painted for ' + key);
  fs.writeFileSync(path.join(ASSETS, 'textures/block', 'battery_' + k + '.png'), buf);
  n++;
}
console.log(`wrote the Battery's models and blockstate, ${tiles} tower tiles for 3 to 9 high, and ${n} textures`);
