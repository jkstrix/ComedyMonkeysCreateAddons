// Writes the approved Geothermal Turbine into the mod: the formed machine and its renderer parts, the three unformed
// blocks, and the geothermal_turbine_* textures. Usage: node design/models/geothermal-turbine.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, defaultUv, fitUv, DIRS } = require('./process-box.lib.js');
const G = require('./geothermal-turbine.gen.js');

const NS = 'comedymonkeysaddons';
const MODELS = path.join(ASSETS, 'models/block');
const ref = (key) => (key.includes(':') ? key : `${NS}:block/geothermal_turbine_${key}`);

function blockModel(cubes, particle) {
  const vars = new Map();
  const v = (r) => { if (!vars.has(r)) vars.set(r, r.replace(/^.*[/:]/, '').replace(/^geothermal_turbine_/, '').replace(/[^a-z0-9_]/g, '_')); return '#' + vars.get(r); };
  // An element with no faces left (a tile buried inside a box) is invalid to Minecraft, so it's dropped
  const elements = cubes.filter((c) => DIRS.some((d) => (d in c.faces ? c.faces[d] : c.faces.all))).map((c) => {
    const faces = {};
    for (const dir of DIRS) {
      const t = dir in c.faces ? c.faces[dir] : c.faces.all;
      if (!t) continue;
      const uv = ((c.uv && c.uv[dir]) || fitUv(dir, c.from, c.to)).map((n) => Math.round(Math.max(0, Math.min(16, n)) * 1000) / 1000);
      faces[dir] = { uv, texture: v(ref(t)) };
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
  const textures = { particle: ref(particle) };
  for (const [r, name] of vars) textures[name] = r;
  return { parent: 'block/block', render_type: 'minecraft:cutout', textures, elements };
}
const write = (name, cubes, particle) => {
  fs.writeFileSync(path.join(MODELS, name + '.json'), JSON.stringify(blockModel(cubes, particle), null, 2) + '\n');
  console.log(`wrote models/block/${name}.json (${cubes.length} elements)`);
};

// formed machine and renderer parts, in the formed frame
write('geothermal_turbine', G.machine(), 'panel');
write('geothermal_turbine_glow', G.glow(), 'glow');
write('geothermal_turbine_rotor', G.rotor(), 'blade');
write('geothermal_turbine_needle_energy', G.needleEnergy(), 'needle');
write('geothermal_turbine_needle_steam', G.needleSteam(), 'needle');
write('geothermal_turbine_lamp_output', G.lamp('output', 'lamp_cyan'), 'lamp_cyan');
write('geothermal_turbine_lamp_stall', G.lamp('stall', 'lamp_red'), 'lamp_red');
write('geothermal_turbine_gauge_lava', G.gaugeFill('lava', 'lava'), 'lava');
write('geothermal_turbine_gauge_water', G.gaugeFill('water', 'water'), 'water');
// unformed blocks (their blockstates and item models already point here)
write('geothermal_casing', G.casing(), 'panel');
write('geothermal_core', G.core(), 'panel');
write('geothermal_controller', G.controller(), 'panel');
for (const [key, buf] of Object.entries(G.T.OWN)) fs.writeFileSync(path.join(ASSETS, 'textures/block', `geothermal_turbine_${key}.png`), buf);
console.log(`wrote ${Object.keys(G.T.OWN).length} textures/block/geothermal_turbine_*.png`);
