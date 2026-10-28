// Writes the approved Spring Engine (design/models/juggernaut-props.gen.js, "Props as built" in the Juggernaut brief) into
// the mod: the two blocks' models (the output end in front, the winding end behind, each in its own block's space, facing
// north), the renderer's parts (the coil seen through each end's porthole at four wind levels, the rune band's four
// levels, the ratchet and pawl on the winding end, the escape wheel and anchor on the output end), the item model (both
// ends, shown smaller), and only the spring_engine_* textures they use.
// Usage: node design/models/spring-engine.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, C } = require('./process-box.lib.js');
const { blockModel } = require('./block-model.lib.js');
// the generator rewrites its Blockbench preview as it loads; send that to a scratch file, not over the approved one
process.argv[2] = path.join(require('os').tmpdir(), 'juggernaut-props.export-scratch.bbmodel');
const P = require('./juggernaut-props.gen.js');

const NS = 'comedymonkeysaddons';
const used = new Set();
const ref = (key) => {
  if (key.startsWith('create:')) return key; // Create's own shaft textures
  if (!key.startsWith('jp:')) throw new Error('texture key outside the props: ' + key);
  used.add(key.slice(3));
  return `${NS}:block/spring_engine_${key.slice(3)}`;
};
const write = (kind, name, cubes, extra = {}) => {
  const model = { ...blockModel(ref, cubes, 'jp:iron'), ...extra };
  fs.writeFileSync(path.join(ASSETS, 'models', kind, name + '.json'), JSON.stringify(model, null, 2) + '\n');
  console.log(`wrote models/${kind}/${name}.json (${cubes.length} elements)`);
};
const shift = (cubes, dz) => cubes.map((c) => ({ ...c, from: [c.from[0], c.from[1], c.from[2] + dz], to: [c.to[0], c.to[1], c.to[2] + dz],
  rot: c.rot ? [c.rot[0], c.rot[1], [c.rot[2][0], c.rot[2][1], c.rot[2][2] + dz]] : undefined }));

write('block', 'spring_engine_output', P.engineFront);
write('block', 'spring_engine_winding', P.engineBack);
P.COILS.forEach((k) => {
  write('block', 'spring_engine_coil_front_' + k, P.frontWin.coil[k]);
  write('block', 'spring_engine_coil_back_' + k, P.backWin.coil[k]);
});
P.runeBand(2, 14).forEach((r, i) => write('block', 'spring_engine_runes_' + (i + 1), r));
write('block', 'spring_engine_escape_wheel', P.escapeWheel);
write('block', 'spring_engine_anchor', P.anchor);
write('block', 'spring_engine_ratchet', P.ratchet);
write('block', 'spring_engine_pawl', P.pawl);
// a short shaft out of each end, on Create's shaft line, turning; drawn while nothing's fitted there (a real shaft or
// machine shows its own)
const stub = (name, z0, z1) => [C(name, [6, 6, z0], [10, 10, z1], { north: 'create:block/axis_top', south: 'create:block/axis_top', all: 'create:block/axis' },
  { uv: { east: [6, 0, 10, z1 - z0], west: [6, 0, 10, z1 - z0], up: [6, 0, 10, z1 - z0], down: [6, 0, 10, z1 - z0], north: [6, 6, 10, 10], south: [6, 6, 10, 10] }, frot: { east: 90, west: 90 } })];
write('block', 'spring_engine_shaft_front', stub('shaft_out', -6, 0));
write('block', 'spring_engine_shaft_back', stub('shaft_in', 16, 22));
// the item: both ends, the output end in front, centred on the block and shown at about half size
const gui = { rotation: [30, 225, 0], translation: [0, 0, 0], scale: [0.4, 0.4, 0.4] };
write('item', 'spring_engine', [...shift([...P.engineFront, ...P.escapeWheel, ...P.anchor, ...P.frontWin.coil[2]], -8),
  ...shift([...P.engineBack, ...P.ratchet, ...P.pawl, ...P.backWin.coil[2]], 8)], {
  display: {
    gui, fixed: { rotation: [0, 0, 0], translation: [0, 0, 0], scale: [0.4, 0.4, 0.4] },
    ground: { rotation: [0, 0, 0], translation: [0, 3, 0], scale: [0.25, 0.25, 0.25] },
    thirdperson_righthand: { rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [0.3, 0.3, 0.3] },
    firstperson_righthand: { rotation: [0, 45, 0], translation: [0, 0, 0], scale: [0.3, 0.3, 0.3] },
    firstperson_lefthand: { rotation: [0, 225, 0], translation: [0, 0, 0], scale: [0.3, 0.3, 0.3] },
  },
});

let n = 0;
for (const key of used) {
  const buf = P.T.OWN[key];
  if (!buf) throw new Error('no texture painted for ' + key);
  fs.writeFileSync(path.join(ASSETS, 'textures/block', 'spring_engine_' + key + '.png'), buf);
  n++;
}
console.log(`wrote ${n} textures/block/spring_engine_*.png`);
