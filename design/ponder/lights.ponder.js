// The Electric Lights' Ponder structures -> src/main/resources/assets/comedymonkeysaddons/ponder/{lights,lamp_posts,floodlight_festoons}.nbt
//   node design/ponder/lights.ponder.js
// A white/light-grey checker plate (y 0) like the mod's other scenes. The Ponder world doesn't run the lines or the
// lights, so the wires' links are written out and every light starts dark; the scenes light them, switch them, dye them
// and aim them themselves (client/ponder/LightScenes). The camera's side is north (low z).
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

// BlockPos.asLong: x in the top 26 bits, z in the next 26, y in the low 12
const asLong = (x, y, z) => (BigInt.asUintN(26, BigInt(x)) << 38n) | (BigInt.asUintN(26, BigInt(z)) << 12n) | BigInt.asUintN(12, BigInt(y));
// a floor wire's links (north, south, west, east), three bits each: line 1, terminal 4, post 5, lamp (flat to a light) 7
const LINE = 1, TERMINAL = 4, POST_LINK = 5, LAMP = 7;
const wires = (north, south, west, east, extra = {}) => ({ id: T.str(MOD + ':power_line'), Links: { t: 'intArray', v: [north | (south << 3) | (west << 6) | (east << 9), 0, 0, 0, 0, 0] }, ...extra });
const WIRE = [`${MOD}:copper_power_line`, { down: true, up: false, north: false, south: false, west: false, east: false, waterlogged: false }];
const GENERATOR = [`${MOD}:lava_generator`, { facing: 'north', fuelled: true, lit: true }];
// a light, dark (the scenes light it)
const light = (id, props = {}) => [`${MOD}:${id}`, { facing: 'up', light: 0, color: 'none', waterlogged: false, ...props }];
const dark = (be = 'light', extra = {}) => ({ id: T.str(`${MOD}:${be}`), Phase: T.int(0), ...extra });
const STRIP = [`${MOD}:light_strip`, { light: 0, color: 'none', waterlogged: false, down: true, up: false, north: false, south: false, west: false, east: false }];
const post = (id, role, branches = {}) => [`${MOD}:${id}`, { facing: 'up', role, north: false, east: false, south: false, west: false, waterlogged: false, ...branches }];

// 1. Electric Lights: a Lava Generator, a wire through a Light Switch to a Bollard, a branch to a run of Light Strips
structure('lights', 8, 3, 6, (set) => {
  set(1, 1, 2, ...GENERATOR);
  set(2, 1, 2, ...WIRE, wires(0, 0, TERMINAL, LINE));
  set(3, 1, 2, ...WIRE, wires(0, 0, LINE, LINE, { Fitting: T.str('switch'), FittingSide: T.int(5) }));
  set(4, 1, 2, ...WIRE, wires(0, LINE, LINE, LINE));
  set(5, 1, 2, ...WIRE, wires(0, 0, LINE, LAMP));
  set(6, 1, 2, ...light('bollard_light'), dark());
  set(4, 1, 3, ...WIRE, wires(LINE, LAMP, 0, 0));
  for (let x = 2; x <= 6; x++)
    set(x, 1, 4, ...STRIP, dark('light_strip'));
});

// 2. Lamp Posts: a Cast Iron pole three high fed at its foot, a Lantern Head on top, Globe Heads on arms either side
structure('lamp_posts', 7, 5, 5, (set) => {
  set(1, 1, 2, ...GENERATOR);
  set(2, 1, 2, ...WIRE, wires(0, 0, TERMINAL, POST_LINK));
  set(3, 1, 2, ...post('cast_iron_lamp_post', 'foot'));
  set(3, 2, 2, ...post('cast_iron_lamp_post', 'pole'));
  set(3, 3, 2, ...post('cast_iron_lamp_post', 'head', { east: true, west: true }));
  set(3, 4, 2, ...light('lantern_head', { arm: 'none' }), dark());
  set(2, 3, 2, ...light('globe_head', { facing: 'west', arm: 'iron' }), dark());
  set(4, 3, 2, ...light('globe_head', { facing: 'east', arm: 'iron' }), dark());
});

// 3. The Floodlight and Festoons: a Floodlight on the floor fed from the generator's wire, a stone block to aim it at;
// a Steel Lamp Post fed by a branch of the wire, and Festoon Lights from a hook on its head to a hook on a stone wall
const FLOOD = [3, 1, 3], TARGET = [9, 1, 5];
structure('floodlight_festoons', 11, 5, 7, (set) => {
  set(1, 1, 3, ...GENERATOR);
  set(2, 1, 3, ...WIRE, wires(LINE, 0, TERMINAL, LAMP));
  set(2, 1, 2, ...WIRE, wires(POST_LINK, LINE, 0, 0));
  set(...FLOOD, ...light('floodlight'), dark());
  set(...TARGET, 'minecraft:stone_bricks');
  set(2, 1, 1, ...post('steel_lamp_post', 'foot'));
  set(2, 2, 1, ...post('steel_lamp_post', 'pole'));
  set(2, 3, 1, ...post('steel_lamp_post', 'head'));
  for (let y = 1; y <= 3; y++)
    set(10, y, 1, 'minecraft:stone_bricks');
  const hook = (facing, isPost) => [`${MOD}:festoon_hook`, { facing, post: isPost, light: 0, color: 'none' }];
  // the post's hook holds the string (Spans: the far hook, which way it faces, held); the wall's has it back
  const span = (x, y, z, facing, held) => T.list([T.comp({ To: { t: 'long', v: asLong(x, y, z) }, Facing: T.int(facing), Held: { t: 'byte', v: held ? 1 : 0 } })]);
  set(3, 3, 1, ...hook('east', true), dark('festoon', { Spans: span(9, 3, 1, 4, true) }));
  set(9, 3, 1, ...hook('west', false), dark('festoon', { Spans: span(3, 3, 1, 5, false) }));
});

module.exports = { FLOOD, TARGET, asLong };
