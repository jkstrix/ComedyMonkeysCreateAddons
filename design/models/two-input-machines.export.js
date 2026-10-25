// Writes the approved String Tangler and Stone Infuser into the mod: both block models (the shell and the static parts),
// the renderer's parts (the tangler's whisk, its two spools and their windings; the infuser's turntable and drive),
// both item models (everything at rest) and only the string_tangler_* and stone_infuser_* textures they use, plus the
// ones the renderer draws in Java (string, slime, web).
// Usage: node design/models/two-input-machines.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitUv, DIRS, C } = require('./process-box.lib.js');
const G = require('./two-input-machines.gen.js');

const NS = 'comedymonkeysaddons';
const PREFIX = { 'tg:': 'string_tangler_', 'si:': 'stone_infuser_' };
const SETS = { 'tg:': G.TG, 'si:': G.SI };
const ref = (key) => { for (const [p, f] of Object.entries(PREFIX)) if (key.startsWith(p)) return `${NS}:block/${f}${key.slice(3)}`; return key; };
const used = new Set();

function blockModel(cubes, particle) {
  const vars = new Map();
  const v = (r) => {
    if (!vars.has(r)) {
      let n = r.replace(/^.*[/:]/, '').replace(/^(string_tangler|stone_infuser)_/, '').replace(/[^a-z0-9_]/g, '_');
      while ([...vars.values()].includes(n)) n += '_';
      vars.set(r, n);
    }
    return '#' + vars.get(r);
  };
  const elements = cubes.filter((c) => DIRS.some((d) => (d in c.faces ? c.faces[d] : c.faces.all))).map((c) => {
    const faces = {};
    for (const dir of DIRS) {
      const t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      if (t.slice(0, 3) in PREFIX) used.add(t);
      const raw = (c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to);
      if (raw.some((n) => n < -0.001 || n > 16.001)) throw new Error(`${c.name} ${dir}: UV ${raw} is off the texture`);
      faces[dir] = { uv: raw.map((n) => Math.round(n * 1000) / 1000), texture: v(ref(t)) };
    }
    const r3 = (a) => a.map((n) => Math.round(n * 1000) / 1000);
    const el = { name: c.name, from: r3(c.from), to: r3(c.to) };
    if (c.rot) {
      if (![-45, -22.5, 0, 22.5, 45].includes(c.rot[1])) throw new Error(c.name + ': angle ' + c.rot[1]);
      el.rotation = { origin: r3(c.rot[2]), axis: c.rot[0], angle: c.rot[1] };
    }
    if (c.shade === false) el.shade = false;
    el.faces = faces;
    for (const p of [...c.from, ...c.to]) if (p < -16 || p > 32) throw new Error(c.name + ' is outside the model bounds');
    return el;
  });
  if (particle.slice(0, 3) in PREFIX) used.add(particle);
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  return { parent: 'block/block', render_type: 'minecraft:cutout', ambientocclusion: false, textures, elements };
}
const write = (kind, name, cubes, particle) => {
  fs.writeFileSync(path.join(ASSETS, 'models', kind, name + '.json'), JSON.stringify(blockModel(cubes, particle), null, 2) + '\n');
  console.log(`wrote models/${kind}/${name}.json (${cubes.length} elements)`);
};

// the String Tangler
const tangler = [...G.shell('tg', { sideWindows: true }), ...G.tanglerStatic()];
write('block', 'string_tangler', tangler, 'tg:side');
write('block', 'string_tangler_whisk', G.whisk(), 'tg:steel_light');
for (const [side, x] of [['west', 3.5], ['east', 12.5]]) {
  write('block', `string_tangler_spool_${side}`, G.spool('spool', x), 'tg:walnut');
  write('block', `string_tangler_winding_${side}`, G.winding('spool', x), 'tg:string');
}
write('item', 'string_tangler', [...tangler, ...G.whisk(), ...G.spool('spool_w', 3.5), ...G.winding('spool_w', 3.5),
  ...G.spool('spool_e', 12.5), ...G.winding('spool_e', 12.5), ...G.slime()], 'tg:side');

// the Stone Infuser
const infuser = [...G.shell('si'), ...G.infuserStatic()];
write('block', 'stone_infuser', infuser, 'si:side');
write('block', 'stone_infuser_turntable', G.turntable(), 'si:andesite');
write('block', 'stone_infuser_drive', G.drive(), 'si:steel_light');
// the item: cobblestone on the table with the new stone (asurine) climbing its lower half
const S = G.STONE, mid = (S.y0 + S.y1) / 2;
write('item', 'stone_infuser', [...infuser, ...G.turntable(), ...G.drive(), ...G.stone(),
  C('band', [S.x0 - 0.1, S.y0 - 0.1, S.z0 - 0.1], [S.x1 + 0.1, mid, S.z1 + 0.1], { all: 'create:block/palettes/stone_types/natural/asurine_0', down: null })], 'si:side');

// textures: what the models use, and what the renderer draws itself
for (const k of ['tg:string', 'tg:slime', 'tg:web']) used.add(k);
let n = 0;
for (const key of used) {
  const p = key.slice(0, 3), k = key.slice(3), buf = SETS[p].OWN[k];
  if (!buf) throw new Error('no texture painted for ' + key);
  fs.writeFileSync(path.join(ASSETS, 'textures/block', PREFIX[p] + k + '.png'), buf);
  n++;
}
console.log(`wrote ${n} textures`);
