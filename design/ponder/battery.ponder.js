// The Battery's and the Industrial Battery's Ponder structures -> src/main/resources/assets/comedymonkeysaddons/ponder/{battery,industrial_battery}.nbt
//   node design/ponder/battery.ponder.js
// A white/light-grey checker plate (y 0) like the mod's other scenes. The Ponder world doesn't run the batteries or the
// lines, so what they show (how full, which way it flows, the tower they form) is written into their block entities; the
// scenes switch the tower on themselves (client/ponder/BatteryScenes). The camera's side is north (low z).
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
const BATTERY = `${MOD}:battery`;
const battery = (joins = {}, formed = false) => [BATTERY, { facing: 'north', formed, left: !!joins.left, right: !!joins.right, back: !!joins.back }];
// what a battery shows: how full (0-1), which way it flows (1 in, -1 out), and the figures its goggles read
const shown = (fill, flow, energy, capacity, extra = {}) => ({ id: T.str(MOD + ':battery'), Fill: { t: 'float', v: fill }, Flow: { t: 'byte', v: flow },
  Energy: { t: 'long', v: BigInt(energy) }, Capacity: { t: 'long', v: BigInt(capacity) }, Size: T.int(1), ...extra });

// the wires' links, packed three bits a direction across the floor (north 0, south 1, west 2, east 3): line 1, terminal 4, post 5
const links = (west, east) => ({ id: T.str(MOD + ':power_line'), Links: { t: 'intArray', v: [(west << 6) | (east << 9), 0, 0, 0, 0, 0] } });
const WIRE = [`${MOD}:copper_power_line`, { down: true, up: false, north: false, south: false, west: false, east: false, waterlogged: false }];
const POST = [`${MOD}:line_post`, { facing: 'up', role: 'single', north: false, east: false, south: false, west: false, waterlogged: false }];
const TERMINAL = 4, POST_LINK = 5;

// 1. The Battery: a Lava Generator charging one down a wire; a bank of three side by side, a motor running from its end
structure('battery', 7, 3, 6, (set) => {
  set(1, 1, 1, `${MOD}:lava_generator`, { facing: 'north', fuelled: true, lit: true });
  set(2, 1, 1, ...WIRE, links(TERMINAL, TERMINAL));
  set(3, 1, 1, ...battery(), shown(0.5, 1, 5000000, 10000000));
  set(1, 1, 4, ...battery({ right: true }), shown(0.75, -1, 22500000, 30000000, { Size: T.int(3) }));
  set(2, 1, 4, ...battery({ left: true, right: true }), shown(0.75, -1, 22500000, 30000000, { Size: T.int(3) }));
  set(3, 1, 4, ...battery({ left: true }), shown(0.75, -1, 22500000, 30000000, { Size: T.int(3) }));
  set(4, 1, 4, `${MOD}:electric_motor`, { facing: 'east', powered: true });
});

// 2. The Industrial Battery: 27 Batteries 3 x 3 x 3 (plain, knowing the tower they'll form: master at the middle of the
// front row), a Line Post with a span to the tower's front-right insulator, wire from the post's foot to a motor
const TOWER = { x0: 1, z0: 1, h: 3 }, MASTER = [2, 1, 1], POST_AT = [8, 1, 2];
const towerSpan = (from, dx, dy, dz, to, owner) => T.list([T.comp({ From: T.int(from), Offset: { t: 'intArray', v: [dx, dy, dz] }, To: T.int(to),
  Tier: T.str('copper'), ...(owner ? { Owner: { t: 'byte', v: 1 } } : {}), Wires: T.int(2) })]);
structure('industrial_battery', 11, 6, 6, (set) => {
  const capacity = 27 * 20000000;
  for (let y = 0; y < TOWER.h; y++)
    for (let dz = 0; dz < 3; dz++)
      for (let dx = 0; dx < 3; dx++) {
        const x = TOWER.x0 + dx, z = TOWER.z0 + dz, isMaster = x === MASTER[0] && y + 1 === MASTER[1] && z === MASTER[2];
        const tower = { Master: { t: 'long', v: asLong(...MASTER) }, Height: T.int(TOWER.h), TowerFacing: T.str('north'), Charge: T.int(9000000) };
        const extra = isMaster ? { ...tower, Spans: towerSpan(1, POST_AT[0] - MASTER[0], 0, POST_AT[2] - MASTER[2], 0, false) } : tower;
        set(x, 1 + y, z, ...battery({ left: dx > 0, right: dx < 2, back: dz < 2 }), shown(0.45, 1, 243000000, capacity, extra));
      }
  set(...POST_AT, ...POST, { id: T.str(MOD + ':line_post'), Spans: towerSpan(0, MASTER[0] - POST_AT[0], 0, MASTER[2] - POST_AT[2], 1, true) });
  set(9, 1, 2, ...WIRE, links(POST_LINK, TERMINAL));
  set(10, 1, 2, `${MOD}:electric_motor`, { facing: 'east', powered: true });
  // the Lightning Rod the scene brings in on top
  set(2, 1 + TOWER.h, 2, 'minecraft:lightning_rod', { facing: 'up', powered: false, waterlogged: false });
});
