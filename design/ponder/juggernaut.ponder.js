// The Clockwork Juggernaut's ritual Ponder structure -> src/main/resources/assets/comedymonkeysaddons/ponder/juggernaut_ritual.nbt
//   node design/ponder/juggernaut.ponder.js
// A white/light-grey checker plate (y 0), 9 x 9, like the mod's other scenes. The T of Brass Blocks stands at the back,
// facing the camera (north, low z), with the Winding Key on its back and a Hand Crank on the key; the Juggernaut appears in
// front of it (client/ponder/JuggernautScenes), 6 wide, on the plate.
const fs = require('fs');
const path = require('path');
const { write, T } = require('./nbt.js');

const OUT = path.resolve(__dirname, '../../src/main/resources/assets/comedymonkeysaddons/ponder');
const MOD = 'comedymonkeysaddons';
const SX = 9, SY = 10, SZ = 9;

const palette = [], index = new Map(), blocks = [];
const state = (id, props = {}) => {
  const key = id + JSON.stringify(props);
  if (!index.has(key)) {
    index.set(key, palette.length);
    const v = { Name: T.str(id) };
    if (Object.keys(props).length) v.Properties = T.comp(Object.fromEntries(Object.entries(props).map(([k, x]) => [k, T.str(String(x))])));
    palette.push(T.comp(v));
  }
  return index.get(key);
};
const set = (x, y, z, id, props, nbt) => {
  const b = { pos: T.list([T.int(x), T.int(y), T.int(z)]), state: T.int(state(id, props)) };
  if (nbt) b.nbt = T.comp(nbt);
  blocks.push(T.comp(b));
};
for (let x = 0; x < SX; x++)
  for (let z = 0; z < SZ; z++)
    set(x, 0, z, (x + z) % 2 ? 'minecraft:light_gray_concrete' : 'minecraft:white_concrete');

// the effigy: a T standing up, its foot at (4, 1, 6)
for (const [x, y] of [[4, 1], [4, 2], [3, 2], [5, 2]])
  set(x, y, 6, 'create:brass_block');
// the key on the T's back, pointing away (south), and a Hand Crank turning it
set(4, 2, 7, `${MOD}:winding_key`, { facing: 'south' }, { id: T.str(`${MOD}:winding_key`) });
set(4, 2, 8, 'create:hand_crank', { facing: 'south', waterlogged: false }, { id: T.str('create:hand_crank') });

const root = T.comp({
  DataVersion: T.int(3955),
  size: T.list([T.int(SX), T.int(SY), T.int(SZ)]),
  palette: T.list(palette),
  blocks: T.list(blocks),
  entities: T.list([], 'compound'),
});
fs.writeFileSync(path.join(OUT, 'juggernaut_ritual.nbt'), write(root));
console.log('wrote juggernaut_ritual.nbt (' + blocks.length + ' blocks)');
