// Writes the approved Trade Board (design/models/trade-board.gen.js, brief design/briefs/trade-board.md) into the mod:
// the board, drawn by its entity renderer in the board's own space (hung on a north-facing face: the container's face at
// z 16), its item model (the board in 3D, the inventory view fitted to the slot), and only the trade_board_* textures
// they use.
// Usage: node design/models/trade-board.export.js
const fs = require('fs');
const path = require('path');
const { ASSETS, fitGui } = require('./process-box.lib.js');
const { blockModel } = require('./block-model.lib.js');
// the generator rewrites its Blockbench preview as it loads; send that to a scratch file, not over the approved one
process.argv[2] = path.join(require('os').tmpdir(), 'trade-board.export-scratch.bbmodel');
const G = require('./trade-board.gen.js');

const NS = 'comedymonkeysaddons';
const used = new Set();
const ref = (key) => {
  if (!key.startsWith('tb:')) throw new Error('texture key outside the board: ' + key);
  used.add(key.slice(3));
  return `${NS}:block/trade_board_${key.slice(3)}`;
};
const all = [...G.board, ...G.rim];
const write = (kind, name, extra = {}) => {
  const model = { ...blockModel(ref, all, 'tb:board'), ...extra };
  fs.writeFileSync(path.join(ASSETS, 'models', kind, name + '.json'), JSON.stringify(model, null, 2) + '\n');
  console.log(`wrote models/${kind}/${name}.json (${model.elements.length} elements)`);
};
write('block', 'trade_board');
// the item: the board as it hangs, turned to face the viewer in the inventory
const item = blockModel(ref, all, 'tb:board');
write('item', 'trade_board', {
  gui_light: 'front',
  display: {
    gui: { ...fitGui(item.elements), rotation: [0, 180, 0] },
    fixed: { rotation: [0, 180, 0], translation: [0, 0, 7], scale: [1, 1, 1] },
    ground: { rotation: [0, 0, 0], translation: [0, 2, 0], scale: [0.5, 0.5, 0.5] },
    thirdperson_righthand: { rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [0.4, 0.4, 0.4] },
    thirdperson_lefthand: { rotation: [75, 45, 0], translation: [0, 2.5, 0], scale: [0.4, 0.4, 0.4] },
    firstperson_righthand: { rotation: [0, 225, 0], translation: [0, 2, 0], scale: [0.4, 0.4, 0.4] },
    firstperson_lefthand: { rotation: [0, 225, 0], translation: [0, 2, 0], scale: [0.4, 0.4, 0.4] },
  },
});
for (const key of used) {
  const buf = G.T.OWN[key];
  if (!buf) throw new Error('no texture painted for ' + key);
  fs.writeFileSync(path.join(ASSETS, 'textures/block', 'trade_board_' + key + '.png'), buf);
}
console.log(`wrote ${used.size} textures/block/trade_board_*.png`);
