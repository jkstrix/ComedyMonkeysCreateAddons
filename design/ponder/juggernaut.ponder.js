// The Clockwork Juggernaut's Ponder structures -> src/main/resources/assets/comedymonkeysaddons/ponder/{juggernaut_ritual,spring_engine}.nbt
//   node design/ponder/juggernaut.ponder.js
// White/light-grey checker plates (y 0) like the mod's other scenes; the camera's side is north (low z).
const fs = require('fs');
const path = require('path');
const { write, T } = require('./nbt.js');

const OUT = path.resolve(__dirname, '../../src/main/resources/assets/comedymonkeysaddons/ponder');
const MOD = 'comedymonkeysaddons';

function structure(name, sx, sy, sz, place) {
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
  for (let x = 0; x < sx; x++)
    for (let z = 0; z < sz; z++)
      set(x, 0, z, (x + z) % 2 ? 'minecraft:light_gray_concrete' : 'minecraft:white_concrete');
  place(set);
  const root = T.comp({
    DataVersion: T.int(3955),
    size: T.list([T.int(sx), T.int(sy), T.int(sz)]),
    palette: T.list(palette),
    blocks: T.list(blocks),
    entities: T.list([], 'compound'),
  });
  fs.writeFileSync(path.join(OUT, name + '.nbt'), write(root));
  console.log('wrote ' + name + '.nbt (' + blocks.length + ' blocks)');
}
const be = (id) => ({ id: T.str(id) });

// 1. The ritual, 9 x 9: the T of Brass Blocks at the back, facing the camera, with the Winding Key on its back and a Hand
// Crank on the key; the Juggernaut appears in front of it (client/ponder/JuggernautScenes), 6 wide, on the plate.
structure('juggernaut_ritual', 9, 10, 9, (set) => {
  for (const [x, y] of [[4, 1], [4, 2], [3, 2], [5, 2]])
    set(x, y, 6, 'create:brass_block');
  set(4, 2, 7, `${MOD}:winding_key`, { facing: 'south' }, be(`${MOD}:winding_key`));
  set(4, 2, 8, 'create:hand_crank', { facing: 'south', waterlogged: false }, be('create:hand_crank'));
});

// 2. The Spring Engine, 7 x 5, lying east-west: a Creative Motor (spare power) and a shaft winding it from the west, its
// output end turning a shaft and an Encased Fan to the east, and a lever on its output end's south side.
structure('spring_engine', 7, 3, 5, (set) => {
  set(1, 1, 2, 'create:creative_motor', { facing: 'east' }, be('create:creative_motor'));
  set(2, 1, 2, 'create:shaft', { axis: 'x', waterlogged: false }, be('create:simple_kinetic'));
  set(3, 1, 2, `${MOD}:spring_engine`, { facing: 'east', part: 'winding' }, be(`${MOD}:spring_engine`));
  set(4, 1, 2, `${MOD}:spring_engine`, { facing: 'east', part: 'output' }, be(`${MOD}:spring_engine`));
  set(5, 1, 2, 'create:shaft', { axis: 'x', waterlogged: false }, be('create:simple_kinetic'));
  set(6, 1, 2, 'create:encased_fan', { facing: 'east' }, be('create:encased_fan'));
  set(4, 1, 3, 'minecraft:lever', { face: 'wall', facing: 'south', powered: false });
});
