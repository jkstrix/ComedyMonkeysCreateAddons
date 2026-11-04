// Writes the approved Trade Master items (design/models/trade-master-items.gen.js, brief design/briefs/trade-master-items.md)
// into the mod: the Netherite Sheet as a flat icon like Create's sheets, and the Trade Ledger and the incomplete Trade
// Ledger as 3D item models (block-model geometry, the inventory view fitted to the slot), with only the textures they use.
// Usage: node design/models/trade-master-items.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitGui } = require('./process-box.lib.js');
const { blockModel } = require('./block-model.lib.js');
// the generator rewrites its Blockbench preview as it loads; send that to a scratch file, not over the approved one
process.argv[2] = path.join(require('os').tmpdir(), 'trade-master-items.export-scratch.bbmodel');
const G = require('./trade-master-items.gen.js');

const NS = 'comedymonkeysaddons';
const used = new Map(); // generator key -> texture name
const texName = (key) => (key.startsWith('trade_ledger') ? key : 'trade_ledger_' + key);
const ref = (key) => {
  if (!key.startsWith('tm:')) throw new Error('texture key outside the items: ' + key);
  const k = key.slice(3);
  used.set(k, texName(k));
  return `${NS}:item/${texName(k)}`;
};
// held and placed like a book: small in the hand, lying on the ground, flat in an item frame
const DISPLAY = {
  ground: { rotation: [0, 0, 0], translation: [0, 2, 0], scale: [0.5, 0.5, 0.5] },
  fixed: { rotation: [90, 0, 0], translation: [0, 0, -2], scale: [0.75, 0.75, 0.75] },
  thirdperson_righthand: { rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [0.4, 0.4, 0.4] },
  thirdperson_lefthand: { rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [0.4, 0.4, 0.4] },
  firstperson_righthand: { rotation: [0, 45, 0], translation: [0, 3, 0], scale: [0.5, 0.5, 0.5] },
  firstperson_lefthand: { rotation: [0, 225, 0], translation: [0, 3, 0], scale: [0.5, 0.5, 0.5] },
};
const write = (name, cubes) => {
  const model = blockModel(ref, cubes, 'tm:trade_ledger_cover_plain', 'minecraft:cutout', 'minecraft:item/generated');
  delete model.parent; // a model of its own elements; display transforms below
  model.gui_light = 'side';
  model.display = { gui: fitGui(model.elements), ...DISPLAY };
  fs.writeFileSync(path.join(ASSETS, 'models/item', name + '.json'), JSON.stringify(model, null, 2) + '\n');
  console.log(`wrote models/item/${name}.json (${model.elements.length} elements)`);
};
write('trade_ledger', G.ledger);
write('incomplete_trade_ledger', G.incomplete);

// the Netherite Sheet: Create's sheets are flat icons, so this is one too
fs.writeFileSync(path.join(ASSETS, 'models/item/netherite_sheet.json'),
  JSON.stringify({ parent: 'minecraft:item/generated', textures: { layer0: `${NS}:item/netherite_sheet` } }, null, 2) + '\n');
fs.writeFileSync(path.join(ASSETS, 'textures/item/netherite_sheet.png'), G.T.OWN.netherite_sheet);
console.log('wrote models/item/netherite_sheet.json, textures/item/netherite_sheet.png');

for (const [key, name] of used) {
  const buf = G.T.OWN[key];
  if (!buf) throw new Error('no texture painted for ' + key);
  fs.writeFileSync(path.join(ASSETS, 'textures/item', name + '.png'), buf);
}
console.log(`wrote ${used.size} textures/item/trade_ledger_*.png`);
