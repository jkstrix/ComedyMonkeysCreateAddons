// Writes the approved Winding Key (design/models/juggernaut-props.gen.js, "Props as built" in the Juggernaut brief) into
// the mod: the block model (plate, collar and pawl), the renderer's two parts (the key that turns with the shaft, and the
// ring of runes that lights as it winds), the item model, and only the winding_key_* textures they use.
// Usage: node design/models/winding-key.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS } = require('./process-box.lib.js');
const { blockModel } = require('./block-model.lib.js');
// the generator rewrites its Blockbench preview as it loads; send that to a scratch file, not over the approved one
process.argv[2] = path.join(require('os').tmpdir(), 'juggernaut-props.export-scratch.bbmodel');
const P = require('./juggernaut-props.gen.js');

const NS = 'comedymonkeysaddons';
const used = new Set();
const ref = (key) => {
  if (!key.startsWith('jp:')) throw new Error('texture key outside the props: ' + key);
  used.add(key.slice(3));
  return `${NS}:block/winding_key_${key.slice(3)}`;
};
const write = (kind, name, cubes, particle, renderType) => {
  fs.writeFileSync(path.join(ASSETS, 'models', kind, name + '.json'), JSON.stringify(blockModel(ref, cubes, particle, renderType), null, 2) + '\n');
  console.log(`wrote models/${kind}/${name}.json (${cubes.length} elements)`);
};

write('block', 'winding_key', P.keyBase, 'jp:bronze');
write('block', 'winding_key_key', P.keyTurning, 'jp:bronze');
// drawn full-bright by the renderer once the key has some wind in it
write('block', 'winding_key_runes', P.keyRunes, 'jp:bronze');
write('item', 'winding_key', [...P.keyBase, ...P.keyTurning], 'jp:bronze');

let n = 0;
for (const key of used) {
  const buf = P.T.OWN[key];
  if (!buf) throw new Error('no texture painted for ' + key);
  fs.writeFileSync(path.join(ASSETS, 'textures/block', 'winding_key_' + key + '.png'), buf);
  n++;
}
console.log(`wrote ${n} textures/block/winding_key_*.png`);
