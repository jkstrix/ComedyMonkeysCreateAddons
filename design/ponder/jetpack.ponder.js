// The Clockwork Jetpack's Ponder structure -> src/main/resources/assets/comedymonkeysaddons/ponder/jetpack_winding_station.nbt
//   node design/ponder/jetpack.ponder.js
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

// The Jetpack Winding Station, 5 x 5: a Creative Motor turning it from below, the station facing the camera (north), the
// block in front of it left clear (where a wearer backs in).
structure('jetpack_winding_station', 5, 4, 5, (set) => {
  set(2, 1, 3, 'create:creative_motor', { facing: 'up' }, be('create:creative_motor'));
  set(2, 2, 3, `${MOD}:jetpack_winding_station`, { facing: 'north' }, be(`${MOD}:jetpack_winding_station`));
});
