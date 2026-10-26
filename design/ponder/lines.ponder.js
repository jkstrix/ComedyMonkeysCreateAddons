// The Fluid and Power Lines' Ponder structures -> src/main/resources/assets/comedymonkeysaddons/ponder/{fluid_lines,line_fittings,power_lines,power_networks}.nbt
//   node design/ponder/lines.ponder.js
// A white/light-grey checker plate (y 0) like the mod's other scenes, with the lines on top, their sides and fittings
// written out (the Ponder world doesn't run the lines' networks); the scenes switch the flow on themselves
// (client/ponder/LineScenes). The camera's side is north (low z).
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

const SIDES = ['north', 'south', 'west', 'east', 'up', 'down'];
// a Fluid Line with the given sides joined (the rest none)
function fluidLine(tier, sides) {
  const props = { waterlogged: false };
  for (const s of SIDES) props[s] = sides[s] || 'none';
  const joined = SIDES.filter((s) => props[s] !== 'none');
  const straight = joined.length === 2 && ((joined.includes('west') && joined.includes('east')) || (joined.includes('north') && joined.includes('south'))
    || (joined.includes('up') && joined.includes('down')));
  props.hub = !straight;
  return [`${MOD}:${tier}_fluid_line`, props];
}
const lineNbt = (extra = {}) => ({ id: T.str(MOD + ':fluid_line'), ...extra });
// Fittings by name; FittingSide is the 3D data value (down 0, up 1, north 2, south 3, west 4, east 5)
const fitting = (name, side) => ({ Fitting: T.str(name), FittingSide: T.int(side) });
const TANK = 'create:fluid_tank';

// 1. Fluid Lines: a tank, an Intake, a run of copper line, a second tank; a brass line beside a copper one
structure('fluid_lines', 7, 4, 6, (set) => {
  set(1, 1, 2, TANK);
  set(2, 1, 2, ...fluidLine('copper', { west: 'intake', east: 'line' }), lineNbt(fitting('intake', 4)));
  set(3, 1, 2, ...fluidLine('copper', { west: 'line', east: 'line' }), lineNbt());
  set(4, 1, 2, ...fluidLine('copper', { west: 'line', east: 'tank' }), lineNbt());
  set(5, 1, 2, TANK);
  // copper and brass side by side: they never join
  set(2, 1, 4, ...fluidLine('copper', { east: 'none' }), lineNbt());
  set(3, 1, 4, ...fluidLine('brass', { west: 'none' }), lineNbt());
});

// 2. The fittings: Intake, Valve, Filter, Meter, Splitter along one line, a T to two tanks at the end
structure('line_fittings', 9, 4, 5, (set) => {
  set(1, 1, 2, TANK);
  set(2, 1, 2, ...fluidLine('copper', { west: 'intake', east: 'line' }), lineNbt(fitting('intake', 4)));
  set(3, 1, 2, ...fluidLine('copper', { west: 'line', east: 'line' }), lineNbt(fitting('valve', 5)));
  set(4, 1, 2, ...fluidLine('copper', { west: 'line', east: 'line' }), lineNbt(fitting('filter', 5)));
  set(5, 1, 2, ...fluidLine('copper', { west: 'line', east: 'line' }), lineNbt(fitting('meter', 5)));
  set(6, 1, 2, ...fluidLine('copper', { west: 'line', east: 'line' }), lineNbt(fitting('splitter', 5)));
  set(7, 1, 2, ...fluidLine('copper', { west: 'line', north: 'tank', south: 'tank' }), lineNbt());
  set(7, 1, 1, TANK);
  set(7, 1, 3, TANK);
});

// 3. Power Lines: a generator, wire on the floor (a Breaker on it), a Line Post, a span to a second post, wire, a motor
// The wires' links, packed three bits a direction across the floor (north 0, south 1, west 2, east 3): line 1,
// terminal 4, post 5
const links = (west, east) => ({ id: T.str(MOD + ':power_line'), Links: { t: 'intArray', v: [(west << 6) | (east << 9), 0, 0, 0, 0, 0] } });
const WIRE = [`${MOD}:copper_power_line`, { down: true, up: false, north: false, south: false, west: false, east: false, waterlogged: false }];
const POST = [`${MOD}:line_post`, { facing: 'up', role: 'single', north: false, east: false, south: false, west: false, waterlogged: false }];
const span = (dx, owner) => T.list([T.comp({ From: T.int(0), Offset: { t: 'intArray', v: [dx, 0, 0] }, To: T.int(0), Tier: T.str('copper'),
  ...(owner ? { Owner: { t: 'byte', v: 1 } } : {}) })]);
structure('power_lines', 11, 5, 5, (set) => {
  set(1, 1, 2, `${MOD}:lava_generator`, { facing: 'north', fuelled: true, lit: true });
  set(2, 1, 2, ...WIRE, links(4, 1));
  set(3, 1, 2, ...WIRE, { ...links(1, 5), ...fitting('breaker', 5) });
  set(4, 1, 2, ...POST, { id: T.str(MOD + ':line_post'), Spans: span(4, true) });
  set(8, 1, 2, ...POST, { id: T.str(MOD + ':line_post'), Spans: span(-4, false) });
  set(9, 1, 2, ...WIRE, links(5, 4));
  set(10, 1, 2, `${MOD}:electric_motor`, { facing: 'east', powered: true });
});

// 4. Power networks: a Lava Generator with a brass wire to a motor on one side and copper on the other, to a Power
// Splitter on a T: one branch on to a motor, the other through a drilled wall to a third motor
const wires = (north, south, west, east) => ({ id: T.str(MOD + ':power_line'), Links: { t: 'intArray', v: [north | (south << 3) | (west << 6) | (east << 9), 0, 0, 0, 0, 0] } });
const LINE = 1, TERMINAL = 4, THROUGH = 6;
const BRASS_WIRE = [`${MOD}:brass_power_line`, WIRE[1]];
// the Splitter's levels by direction (down, up, north, south, west, east): 0 first, 1 second, 2 last
const priority = (levels) => ({ Priority: { t: 'byteArray', v: levels } });
structure('power_networks', 9, 4, 9, (set) => {
  set(1, 1, 2, `${MOD}:electric_motor`, { facing: 'west', powered: true });
  set(2, 1, 2, ...BRASS_WIRE, wires(0, 0, TERMINAL, LINE));
  set(3, 1, 2, ...BRASS_WIRE, wires(0, 0, LINE, TERMINAL));
  set(4, 1, 2, `${MOD}:lava_generator`, { facing: 'north', fuelled: true, lit: true });
  set(5, 1, 2, ...WIRE, wires(0, 0, TERMINAL, LINE));
  // the Splitter: its fitting's side is its first branch round the face (north, south, west, east): south here
  set(6, 1, 2, ...WIRE, { ...wires(0, LINE, LINE, LINE), ...fitting('splitter', 3), ...priority([1, 1, 1, 2, 1, 0]) });
  set(7, 1, 2, ...WIRE, wires(0, 0, LINE, TERMINAL));
  set(8, 1, 2, `${MOD}:electric_motor`, { facing: 'east', powered: true });
  set(6, 1, 3, ...WIRE, wires(LINE, THROUGH, 0, 0));
  // a low wall, so the motor beyond it shows
  for (let x = 5; x <= 7; x++)
    set(x, 1, 4, 'minecraft:stone_bricks');
  set(6, 1, 5, ...WIRE, wires(THROUGH, TERMINAL, 0, 0));
  set(6, 1, 6, `${MOD}:electric_motor`, { facing: 'south', powered: true });
});
