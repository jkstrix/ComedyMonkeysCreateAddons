// The Clockwork Tubes' Ponder structure -> src/main/resources/assets/comedymonkeysaddons/ponder/clockwork_tubes.nbt
//   node design/ponder/tubes.ponder.js
// A white/light-grey checker plate (y 0) like the mod's other scenes; the camera's side is north (low z). The user's
// diagram: three Inlets (on a chest, over a depot, over the floor) with tubes rising and joining into one, up to a
// Receiver outputting into a chest; its ring gear meshes with a cogwheel on a shaft from a motor behind (z 5). The
// scene shows the second Receiver (7, 4, 4) and the redstone block (8, 2, 4) later; it turns the left Inlet brass.
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
const SIDES = ['north', 'south', 'east', 'west', 'up', 'down'];
const tube = (open, capped = []) => Object.fromEntries([...SIDES.map((s) => [s, open.includes(s) ? 'open' : capped.includes(s) ? 'capped' : 'none']), ['waterlogged', false]]);

structure('clockwork_tubes', 9, 8, 9, (set) => {
  const TUBE = `${MOD}:clockwork_tube`, INLET = `${MOD}:clockwork_inlet`, RECEIVER = `${MOD}:clockwork_receiver`;
  // left: an Inlet on a chest
  set(1, 1, 4, 'minecraft:chest', { facing: 'north', type: 'single', waterlogged: false }, be('minecraft:chest'));
  set(1, 2, 4, INLET, { facing: 'down', powered: false }, be(INLET));
  set(1, 3, 4, TUBE, tube(['down', 'east']), be(TUBE));
  set(2, 3, 4, TUBE, tube(['east', 'west']), be(TUBE));
  set(3, 3, 4, TUBE, tube(['east', 'west']), be(TUBE));
  // middle: an Inlet over a depot, under the hub where the branches meet and rise
  set(4, 1, 4, 'create:depot', {}, be('create:depot'));
  set(4, 2, 4, INLET, { facing: 'down', powered: false }, be(INLET));
  set(4, 3, 4, TUBE, tube(['east', 'west', 'up', 'down']), be(TUBE));
  set(4, 4, 4, TUBE, tube(['up', 'down']), be(TUBE));
  // right: an Inlet hanging over the floor
  set(5, 3, 4, TUBE, tube(['east', 'west']), be(TUBE));
  set(6, 3, 4, TUBE, tube(['east', 'west']), be(TUBE));
  set(7, 3, 4, TUBE, tube(['west', 'down']), be(TUBE));
  set(7, 2, 4, INLET, { facing: 'down', powered: false }, be(INLET));
  // the Receiver, into a chest above; its cog meshed with a cogwheel on a shaft from a motor behind
  set(4, 5, 4, RECEIVER, { facing: 'up' }, be(RECEIVER));
  set(4, 6, 4, 'minecraft:chest', { facing: 'north', type: 'single', waterlogged: false }, be('minecraft:chest'));
  set(4, 1, 5, 'create:creative_motor', { facing: 'up' }, be('create:motor'));
  for (let y = 2; y <= 4; y++) set(4, y, 5, 'create:shaft', { axis: 'y', waterlogged: false }, be('create:simple_kinetic'));
  set(4, 5, 5, 'create:cogwheel', { axis: 'y', waterlogged: false }, be('create:simple_kinetic'));
  // shown later: a second Receiver on the right branch, and a redstone block beside the right Inlet
  set(7, 4, 4, RECEIVER, { facing: 'up' }, be(RECEIVER));
  set(8, 2, 4, 'minecraft:redstone_block');
});
