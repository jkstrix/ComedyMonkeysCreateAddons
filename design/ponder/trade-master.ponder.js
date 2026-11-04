// The Trade Master's Ponder structures -> src/main/resources/assets/comedymonkeysaddons/ponder/trade_master*.nbt
//   node design/ponder/trade-master.ponder.js
// As Trade Bot's (trade-bot.ponder.js): a white/light-grey checker plate (y 0) with blocks on top; the Trade Master,
// his Trade Boards (entities), the villagers and their seats are made by the scenes (client/ponder/TradeMasterScenes),
// which also set the gauges. The camera's side is north (low z).
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
  const set = (x, y, z, id, props) => blocks.push(T.comp({ pos: T.list([T.int(x), T.int(y), T.int(z)]), state: T.int(state(id, props)) }));
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

const STATION = MOD + ':trade_master';
const CHEST = (facing) => ['minecraft:chest', { facing, type: 'single', waterlogged: false }];
// a Factory Gauge on a block's top, the way it sits on a Trade Board there (its panels are set by the scene)
const GAUGE_ON_TOP = ['create:factory_gauge', { face: 'floor', facing: 'north', powered: false, waterlogged: false }];

// 1. joining a network: his station at the back with a Stock Link beside it; an input chest and an output chest in
// front, each to get a board on its top, the output a gauge (at (4, 2, 2))
structure('trade_master', 7, 3, 7, (set) => {
  set(3, 1, 5, STATION, { facing: 'north', glow: true });
  set(5, 1, 5, 'create:stock_link', { face: 'floor', facing: 'north', powered: false, waterlogged: false });
  set(1, 1, 2, ...CHEST('north'));
  set(4, 1, 2, ...CHEST('north'));
  set(4, 2, 2, ...GAUGE_ON_TOP);
});

// 2. filling gauges: his station on the left, an input chest and an output chest with a gauge (both boarded on top);
// a farmer's booth (seat (7, 1, 4), composter (6, 1, 4)) and a librarian's (seat (7, 1, 1), lectern (6, 1, 1))
structure('trade_master_gauges', 9, 3, 7, (set) => {
  set(1, 1, 3, STATION, { facing: 'east', glow: true });
  set(1, 1, 5, ...CHEST('east'));
  set(3, 1, 5, ...CHEST('north'));
  set(3, 2, 5, ...GAUGE_ON_TOP);
  set(7, 1, 4, 'create:green_seat', { waterlogged: false });
  set(6, 1, 4, 'minecraft:composter', { level: 0 });
  set(7, 1, 1, 'create:red_seat', { waterlogged: false });
  set(6, 1, 1, 'minecraft:lectern', { facing: 'east', has_book: false, powered: false });
});
