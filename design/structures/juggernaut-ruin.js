// The Juggernaut Ruins (spec D31, D55) -> src/main/resources/data/comedymonkeysaddons/structure/juggernaut_ruin{,_end}.nbt
//   node design/structures/juggernaut-ruin.js
// An overgrown, collapsed hall, 11 wide inside and open to the sky in places, where a dormant Clockwork Juggernaut
// kneels facing the doorway (north, low z). Overworld: dark deepslate brick gone to moss, broken dark oak roof beams,
// chains and vines hanging from them. The End: the same hall in End Stone, half-buried (the structure set sinks it),
// with chorus growing where the vines would be. Seeded, so it builds the same every time.
const fs = require('fs');
const path = require('path');
const { write, T } = require('../ponder/nbt.js');

const OUT = path.resolve(__dirname, '../../src/main/resources/data/comedymonkeysaddons/structure');
const MOD = 'comedymonkeysaddons';
const SX = 15, SY = 12, SZ = 15;
const D = (v) => ({ t: 'double', v }), F = (v) => ({ t: 'float', v }), B = (v) => ({ t: 'byte', v });

function rng(seed) {
  let s = seed >>> 0;
  return () => ((s = (s * 1664525 + 1013904223) >>> 0) / 4294967296);
}

// what each kind of block is in each version
const PALETTES = {
  overworld: {
    brick: ['minecraft:deepslate_bricks', 'minecraft:deepslate_bricks', 'minecraft:cracked_deepslate_bricks', 'minecraft:deepslate_tiles'],
    pillar: 'minecraft:polished_deepslate',
    floor: ['minecraft:deepslate_tiles', 'minecraft:cobbled_deepslate', 'minecraft:deepslate_bricks', 'minecraft:moss_block'],
    dais: 'minecraft:polished_deepslate',
    rubble: ['minecraft:cobbled_deepslate', 'minecraft:gravel', 'minecraft:cracked_deepslate_bricks'],
    slab: 'minecraft:deepslate_tile_slab',
    beam: 'minecraft:dark_oak_log',
    carpet: 'minecraft:moss_carpet',
    plant: 'minecraft:fern',
  },
  end: {
    brick: ['minecraft:end_stone_bricks', 'minecraft:end_stone_bricks', 'minecraft:end_stone', 'minecraft:end_stone_bricks'],
    pillar: 'minecraft:purpur_pillar',
    floor: ['minecraft:end_stone_bricks', 'minecraft:end_stone', 'minecraft:end_stone', 'minecraft:end_stone_bricks'],
    dais: 'minecraft:purpur_block',
    rubble: ['minecraft:end_stone', 'minecraft:end_stone_bricks', 'minecraft:end_stone'],
    slab: 'minecraft:end_stone_brick_slab',
    beam: 'minecraft:purpur_pillar',
    carpet: null,
    plant: null,
  },
};

function build(name, kind) {
  const P = PALETTES[kind], r = rng(kind === 'end' ? 4242 : 1717);
  const pick = (a) => a[Math.floor(r() * a.length)];
  const grid = new Map(); // "x,y,z" -> [id, props, nbt]
  const set = (x, y, z, id, props = {}, nbt) => { if (x >= 0 && y >= 0 && z >= 0 && x < SX && y < SY && z < SZ) grid.set(`${x},${y},${z}`, [id, props, nbt]); };
  const get = (x, y, z) => grid.get(`${x},${y},${z}`);
  const solid = (x, y, z) => { const b = get(x, y, z); return b && !/vine|chain|carpet|fern|chorus|slab|chest/.test(b[0]); };

  // the floor, with moss and gaps of bare earth
  for (let x = 1; x < SX - 1; x++)
    for (let z = 1; z < SZ - 1; z++)
      set(x, 0, z, pick(P.floor));
  // the dais he kneels on: 7 x 7, one step up
  for (let x = 4; x <= 10; x++)
    for (let z = 4; z <= 10; z++)
      set(x, 1, z, P.dais, P.dais.includes('pillar') ? { axis: 'y' } : {});

  // the walls: 1 thick at x 1 and 13 and z 13; the front (z 1) a broken arch round a 5-wide doorway. Each column of wall
  // stands to its own height, highest at the corners, broken lowest in the middle of each side
  const wallHeight = (along, side) => {
    const edge = Math.min(along - 1, 13 - along); // 0 at the corners
    const top = 9 - Math.round(edge * 0.9 + r() * 3) - (side === 'front' ? 1 : 0);
    return Math.max(2, top);
  };
  for (let a = 1; a <= 13; a++) {
    const hw = wallHeight(a, 'west'), he = wallHeight(a, 'east'), hb = wallHeight(a, 'back'), hf = wallHeight(a, 'front');
    for (let y = 1; y <= hw; y++) set(1, y, a, pick(P.brick));
    for (let y = 1; y <= he; y++) set(13, y, a, pick(P.brick));
    for (let y = 1; y <= hb; y++) set(a, y, 13, pick(P.brick));
    if (a <= 4 || a >= 10)
      for (let y = 1; y <= hf; y++) set(a, y, 1, pick(P.brick));
    else if (a === 5 || a === 9)
      for (let y = 6; y <= Math.max(6, hf); y++) set(a, y, 1, pick(P.brick)); // the arch's springing
  }
  // a few holes broken through the side walls
  for (const [x, z] of [[1, 6], [1, 7], [13, 9], [13, 4]])
    for (let y = 2 + Math.floor(r() * 2); y <= 4; y++) grid.delete(`${x},${y},${z}`);
  // corner pillars, standing to the full height
  for (const [x, z] of [[1, 1], [13, 1], [1, 13], [13, 13]])
    for (let y = 1; y <= 10; y++) set(x, y, z, P.pillar, { axis: 'y' });

  // the roof: two great beams across the hall (one broken off short), a few slabs left at the walls' tops
  for (let x = 1; x <= 13; x++) set(x, 10, 5, P.beam, { axis: 'x' });
  for (let x = 1; x <= 8; x++) set(x, 10, 10, P.beam, { axis: 'x' });
  set(9, 9, 10, P.beam, { axis: 'y' }); // its broken end hanging down
  for (let a = 2; a <= 12; a++)
    for (const [x, z] of [[1, a], [13, a], [a, 13]]) {
      let y = SY - 1;
      while (y > 0 && !get(x, y, z)) y--;
      if (y >= 6 && r() < 0.35) set(x, y + 1, z, P.slab, { type: 'bottom', waterlogged: false });
    }

  // rubble fallen from the roof, in heaps along the walls and one by the dais
  for (const [cx, cz, n] of [[2, 3, 5], [12, 11, 6], [3, 11, 4], [11, 3, 3]])
    for (let i = 0; i < n; i++) {
      const x = cx + Math.round((r() - 0.5) * 2), z = cz + Math.round((r() - 0.5) * 2);
      let y = 1;
      while (get(x, y, z)) y++;
      if (x > 1 && x < 13 && z > 1 && z < 13 && y < 4) set(x, y, z, pick(P.rubble));
    }
  // the clockwork that was: a brass block half sunk and a shaft lying in the moss
  set(3, 1, 3, 'create:brass_block');
  set(11, 1, 6, 'create:shaft', { axis: 'z', waterlogged: false }, { id: T.str('create:simple_kinetic') });

  // chains hanging from the beams
  for (const [x, z, len] of [[3, 5, 4], [11, 5, 3], [4, 10, 5]])
    for (let y = 9; y > 9 - len; y--) set(x, y, z, 'minecraft:chain', { axis: 'y', waterlogged: false });

  if (kind === 'overworld') {
    // vines down the inside of the walls, and hanging over him from the beams
    for (let a = 2; a <= 12; a++) {
      for (const [x, z, face] of [[2, a, 'west'], [12, a, 'east'], [a, 12, 'south']]) {
        if (r() > 0.45) continue;
        const wx = face === 'west' ? 1 : face === 'east' ? 13 : x, wz = face === 'south' ? 13 : z;
        let y = 9;
        while (y > 2 && !(solid(wx, y, wz) && !get(x, y, z))) y--;
        const len = 2 + Math.floor(r() * 4);
        for (let k = 0; k < len && y - k > 1 && solid(wx, y - k, wz) && !get(x, y - k, z); k++)
          set(x, y - k, z, 'minecraft:vine', { north: false, south: face === 'south', east: face === 'east', west: face === 'west', up: false });
      }
    }
    for (let x = 4; x <= 10; x += 2)
      set(x, 9, 5, 'minecraft:vine', { north: false, south: false, east: false, west: false, up: true });
    // moss and ferns on the floor
    for (let i = 0; i < 26; i++) {
      const x = 2 + Math.floor(r() * 11), z = 2 + Math.floor(r() * 11);
      if (get(x, 1, z)) continue;
      set(x, 1, z, r() < 0.3 && get(x, 0, z)[0] === 'minecraft:moss_block' ? P.plant : P.carpet);
    }
  } else {
    // chorus growing up the walls and through the gaps in the roof, where the vines would be
    for (const [x, z, h] of [[2, 2, 3], [12, 12, 4], [12, 2, 2], [2, 12, 3], [7, 12, 2]]) {
      set(x, 0, z, 'minecraft:end_stone');
      for (let y = 1; y <= h; y++)
        set(x, y, z, 'minecraft:chorus_plant', { down: true, up: y < h, north: false, south: false, east: false, west: false });
      set(x, h + 1, z, 'minecraft:chorus_flower', { age: 5 });
    }
  }

  // the chest, in the back corner (D33)
  set(11, 2, 11, 'minecraft:chest', { facing: 'north', type: 'single', waterlogged: false },
    { id: T.str('minecraft:chest'), LootTable: T.str(`${MOD}:chests/juggernaut_ruin`) });
  if (!get(11, 1, 11)) set(11, 1, 11, P.dais);

  // write: blocks, and air through the hall so the ground it's dug into is cleared (the rest is left as it was)
  const palette = [], index = new Map(), blocks = [];
  const state = (id, props) => {
    const key = id + JSON.stringify(props);
    if (!index.has(key)) {
      index.set(key, palette.length);
      const v = { Name: T.str(id) };
      if (Object.keys(props).length) v.Properties = T.comp(Object.fromEntries(Object.entries(props).map(([k, x]) => [k, T.str(String(x))])));
      palette.push(T.comp(v));
    }
    return index.get(key);
  };
  for (let x = 2; x <= 12; x++)
    for (let y = 1; y <= 9; y++)
      for (let z = 2; z <= 12; z++)
        if (!get(x, y, z)) set(x, y, z, 'minecraft:air');
  for (const [key, [id, props, nbt]] of grid) {
    const [x, y, z] = key.split(',').map(Number);
    const b = { pos: T.list([T.int(x), T.int(y), T.int(z)]), state: T.int(state(id, props)) };
    if (nbt) b.nbt = T.comp(nbt);
    blocks.push(T.comp(b));
  }
  // the Juggernaut, kneeling on the dais and facing the doorway (north)
  const juggernaut = {
    blockPos: T.list([T.int(7), T.int(2), T.int(7)]),
    pos: T.list([D(7.5), D(2), D(7.5)]),
    nbt: T.comp({ id: T.str(`${MOD}:clockwork_juggernaut`), Phase: T.str('DORMANT'), Ruin: B(1), Stage: B(1), Rotation: T.list([F(180), F(0)]),
      PersistenceRequired: B(1) }),
  };
  const root = T.comp({
    DataVersion: T.int(3955),
    size: T.list([T.int(SX), T.int(SY), T.int(SZ)]),
    palette: T.list(palette),
    blocks: T.list(blocks),
    entities: T.list([T.comp(juggernaut)]),
  });
  fs.mkdirSync(OUT, { recursive: true });
  fs.writeFileSync(path.join(OUT, name + '.nbt'), write(root));
  console.log(`wrote structure/${name}.nbt (${blocks.length} blocks, 1 Juggernaut)`);
}

build('juggernaut_ruin', 'overworld');
build('juggernaut_ruin_end', 'end');
