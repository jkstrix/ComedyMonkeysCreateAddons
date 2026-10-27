// Writes the approved Fish Tank models into the mod: the two tanks, the parts the renderer draws, Fish Food's item model
// and every texture. Usage: node design/models/fish-tank.export.js (regenerates the preview too).
// The renderer's parts (FishTankRenderer): the pump piston, the water (a 1 px unit it stretches to the level) and the
// glow on the back wall. The sunken cogwheel is Create's own, drawn by the renderer at FT.COG.
const fs = require('fs');
const path = require('path');
const { ASSETS, C } = require('./process-box.lib.js');
const BM = require('./block-model.lib.js');
const FT = require('./fish-tank.gen.js');
const { ITEMS } = require('./monster-cage.gen.js');

const NS = 'comedymonkeysaddons';
const MODELS = path.join(ASSETS, 'models');
const BLOCK_TEX = path.join(ASSETS, 'textures/block');
const ITEM_TEX = path.join(ASSETS, 'textures/item');

const PREFIX = { 'tank:': 'fish_tank_', 'andesite:': 'fish_tank_', 'brass:': 'brass_fish_tank_' };
function ref(key) {
  for (const [p, file] of Object.entries(PREFIX)) if (key.startsWith(p)) return `${NS}:block/${file}${key.slice(p.length)}`;
  if (key.startsWith('item:')) return `${NS}:item/${key.slice(5)}`;
  return key;
}
const blockModel = (...args) => BM.blockModel(ref, ...args);
const writeJson = (rel, obj) => { fs.writeFileSync(path.join(MODELS, rel), JSON.stringify(obj, null, 2) + '\n'); console.log('wrote models/' + rel); };
const writePng = (dir, name, buf) => { fs.writeFileSync(path.join(dir, name + '.png'), buf); };

// ---------- the two tanks (the same model lit or not: GLOWING only gives light) ----------
for (const [name, tier] of [['fish_tank', 'andesite'], ['brass_fish_tank', 'brass']]) {
  writeJson(`block/${name}.json`, blockModel(FT.tank(tier), `${tier}:frame`));
  writeJson(`item/${name}.json`, { parent: `${NS}:block/${name}` });
  const variants = {};
  for (const [f, y] of [['north', 0], ['east', 90], ['south', 180], ['west', 270]])
    for (const glowing of [false, true])
      variants[`facing=${f},glowing=${glowing}`] = y ? { model: `${NS}:block/${name}`, y } : { model: `${NS}:block/${name}` };
  fs.writeFileSync(path.join(ASSETS, 'blockstates', name + '.json'), JSON.stringify({ variants }, null, 2) + '\n');
}

// ---------- the renderer's parts ----------
writeJson('block/fish_tank_pump.json', blockModel(FT.piston(), 'tank:steel'));
writeJson('block/fish_tank_water.json', blockModel([
  C('water', [1, 0, 0.75], [15, 1, 13.75], { north: 'tank:water', east: 'tank:water', west: 'tank:water', up: 'tank:water_top', all: null },
    { uv: { north: [0, 0, 16, 16], east: [0, 0, 16, 16], west: [0, 0, 16, 16], up: [0, 0, 16, 16] }, shade: false }),
], 'tank:water', 'minecraft:translucent'));
writeJson('block/fish_tank_glow.json', blockModel(FT.glow(), 'tank:glow', 'minecraft:translucent'));
writeJson('item/fish_food.json', { parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/fish_food` } });

// ---------- textures ----------
// the creature stand-ins and bubbles are the preview's; the cog is Create's own
const PREVIEW_ONLY = new Set(['cod', 'squid', 'axo', 'tropical', 'eye', 'bubble', 'cog', 'hub', 'waterline']);
for (const [set, prefix] of [[FT.TANK, 'fish_tank_'], [FT.BTANK, 'brass_fish_tank_']])
  for (const [key, buf] of Object.entries(set.OWN)) if (!PREVIEW_ONLY.has(key)) writePng(BLOCK_TEX, prefix + key, buf);
writePng(ITEM_TEX, 'fish_food', ITEMS.OWN.fish_food);
console.log('textures written');
