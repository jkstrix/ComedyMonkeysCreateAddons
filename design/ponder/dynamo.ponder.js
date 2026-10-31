// The Clockwork Dynamo's Ponder structure -> src/main/resources/assets/comedymonkeysaddons/ponder/clockwork_dynamo.nbt
//   node design/ponder/dynamo.ponder.js
// A white/light-grey checker plate (y 0) like the mod's other scenes; the camera's side is north (low z).
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

// 7 x 5, lying east-west a block up (the Water Wheel is two blocks across): a Water Wheel (free power) and a Clutch driving
// the dynamo from the west, and an Electric Motor against its base to the east, its own shaft running on; the dynamo and
// motor stand on Andesite Casings. The scene sets the speeds and lamps (client/ponder/ElectricScenes).
structure('clockwork_dynamo', 7, 4, 5, (set) => {
  set(3, 1, 2, 'create:andesite_casing');
  set(4, 1, 2, 'create:andesite_casing');
  set(1, 2, 2, 'create:water_wheel', { facing: 'east' }, be('create:water_wheel'));
  set(2, 2, 2, 'create:clutch', { axis: 'x', powered: false }, be('create:clutch'));
  set(3, 2, 2, `${MOD}:clockwork_dynamo`, { facing: 'west', working: false }, { ...be(`${MOD}:clockwork_dynamo`), StressSetting: T.int(256) });
  set(4, 2, 2, `${MOD}:electric_motor`, { facing: 'east', powered: false }, { ...be(`${MOD}:electric_motor`), SpeedSetting: T.int(8), StressSetting: T.int(256) });
  set(5, 2, 2, 'create:shaft', { axis: 'x', waterlogged: false }, be('create:simple_kinetic'));
});
