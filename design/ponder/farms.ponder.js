// The Monster Cages' and Fish Tanks' Ponder structures -> src/main/resources/assets/comedymonkeysaddons/ponder/
//   {monster_cage,flooded_monster_cage,netherite_monster_cage,fish_tank}.nbt
//   node design/ponder/farms.ponder.js
// As the Animal Pen's: a 5 x 5 checker plate, the block in the middle with its window to the camera (north) and a
// Creative Motor behind it; the Netherite cage has a Battery at its west side for its FE. The Ponder world doesn't run
// them, so the scenes (client/ponder/FarmScenes) set what's inside.
const fs = require('fs');
const path = require('path');
const { write, T } = require('./nbt.js');

const OUT = path.resolve(__dirname, '../../src/main/resources/assets/comedymonkeysaddons/ponder');
const MOD = 'comedymonkeysaddons';

function structure(name, place) {
  const sx = 5, sy = 2, sz = 5;
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
  const set = (x, y, z, id, props) => blocks.push(T.comp({ pos: T.list([T.int(x), T.int(y), T.int(z)]), state: T.int(state(id, props)) }));
  for (let x = 0; x < sx; x++)
    for (let z = 0; z < sz; z++)
      set(x, 0, z, (x + z) % 2 ? 'minecraft:light_gray_concrete' : 'minecraft:white_concrete');
  place(set);
  // the motor behind, its shaft into the block's back
  set(2, 1, 3, 'create:creative_motor', { facing: 'north' });
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

structure('monster_cage', (set) => set(2, 1, 2, `${MOD}:monster_cage`, { facing: 'north' }));
structure('flooded_monster_cage', (set) => set(2, 1, 2, `${MOD}:flooded_monster_cage`, { facing: 'north' }));
structure('netherite_monster_cage', (set) => {
  set(2, 1, 2, `${MOD}:netherite_monster_cage`, { facing: 'north' });
  set(1, 1, 2, `${MOD}:battery`, { facing: 'north', formed: false, left: false, right: false, back: false });
});
structure('fish_tank', (set) => set(2, 1, 2, `${MOD}:fish_tank`, { facing: 'north', glowing: false }));
