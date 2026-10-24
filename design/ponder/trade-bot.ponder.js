// Trade Bot's Ponder structures -> src/main/resources/assets/comedymonkeysaddons/ponder/trade_bot*.nbt
//   node design/ponder/trade-bot.ponder.js
// Each is a white/light-grey checker plate (y 0) like the mod's other scenes, with blocks on top; the bot, the villager
// and its seat are entities the scenes create (client/ponder/TradeBotScenes). The camera's side is north (low z).
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

const STATION = MOD + ':trade_bot';
const CHEST = (facing) => ['minecraft:chest', { facing, type: 'single', waterlogged: false }];

// 1. placing him and powering his station: the station in the middle, a lever and a comparator beside it
structure('trade_bot', 5, 3, 5, (set) => {
  set(2, 1, 2, STATION, { facing: 'north', glow: true });
  set(3, 1, 2, 'minecraft:comparator', { facing: 'west', mode: 'compare', powered: false });
  set(4, 1, 2, 'minecraft:redstone_wire', { east: 'none', west: 'side', north: 'none', south: 'none', power: 0 });
  set(1, 1, 2, 'minecraft:lever', { face: 'floor', facing: 'north', powered: false });
});

// 2. linking: the station at the back, two input chests and an output barrel in front
structure('trade_bot_linking', 7, 3, 7, (set) => {
  set(3, 1, 5, STATION, { facing: 'north', glow: true });
  set(1, 1, 2, ...CHEST('east'));
  set(3, 1, 1, ...CHEST('south'));
  set(5, 1, 2, 'minecraft:barrel', { facing: 'up', open: false });
});

// 3. trading: the station at the left with an input chest and an output barrel; a fletcher's booth on the right
// (its Create seat at (7, 1, 1), the fletching table in front of it at (6, 1, 1)); he trades from (5, 1, 1)
structure('trade_bot_trading', 9, 3, 7, (set) => {
  set(1, 1, 4, STATION, { facing: 'east', glow: true });
  set(1, 1, 6, ...CHEST('east'));
  set(3, 1, 6, 'minecraft:barrel', { facing: 'up', open: false });
  set(7, 1, 1, 'create:red_seat', { waterlogged: false });
  set(6, 1, 1, 'minecraft:fletching_table');
});
