// Helper Bot's Ponder structures -> src/main/resources/assets/comedymonkeysaddons/ponder/helper_bot*.nbt
//   node design/ponder/helper-bot.ponder.js
// Each is the mod's white/light-grey checker plate (y 0) with blocks on top; client/ponder/HelperBotScenes shows them
// a section at a time. The camera's side is north (low z).
const fs = require('fs');
const path = require('path');
const { write, T } = require('./nbt.js');

const OUT = path.resolve(__dirname, '../../src/main/resources/assets/comedymonkeysaddons/ponder');
const BOT = 'comedymonkeysaddons:helper_bot';

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

// 1. driving: track along x (z 2), a station behind it (z 3), a carriage on two bogeys, the controls at its east end
// facing back at the Helper Bot behind them
structure('helper_bot_driving', 9, 6, 5, (set) => {
  for (let x = 0; x < 9; x++) set(x, 1, 2, 'create:track', { shape: 'xo', turn: false, waterlogged: false });
  set(7, 1, 3, 'create:track_station', { assembling: false, waterlogged: false });
  for (const x of [2, 6]) set(x, 2, 2, 'create:small_bogey', { axis: 'x', waterlogged: false });
  for (let x = 1; x <= 7; x++) set(x, 3, 2, 'create:railway_casing');
  set(7, 4, 2, 'create:controls', { facing: 'west', open: false, virtual: false, waterlogged: false });
  set(6, 4, 2, BOT, { facing: 'east', ducked: false });
});

// 2. keeping shop: goods in a chest, a packager in front of it with a stock link on top, the Stock Ticker, and the
// Helper Bot beside it facing the customers (north)
structure('helper_bot_shop', 7, 4, 5, (set) => {
  set(1, 1, 3, 'minecraft:chest', { facing: 'north', type: 'single', waterlogged: false });
  set(1, 1, 2, 'create:packager', { facing: 'north', linked: true, powered: false });
  set(1, 2, 2, 'create:stock_link', { face: 'floor', facing: 'north', powered: false, waterlogged: false });
  set(3, 1, 2, 'create:stock_ticker', { facing: 'north' });
  set(4, 1, 2, BOT, { facing: 'north', ducked: false });
});
