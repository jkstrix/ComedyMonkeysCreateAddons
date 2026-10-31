// The Assembly Clamp's flat icon (D72), for review beside the approved city icons: the exporter's ICONS.assembly_clamp
// (design/models/clockwork-wastes.export.js), drawn as every flat icon is (16 x 16, a step lighter than the block textures,
// the VA_ICON palette, a 1 px #121118 outline round the silhouette). Run the exporter first (it writes assembly_clamp.png
// here and into the mod), then:
// node assembly-clamp-icon.js  -> assembly-clamp-icon-review.png (8x: the Cuckoo Clock, the approved city icons, the lift
// icons, then the Assembly Clamp; a second row at 1x and 2x, the sizes it shows at in an inventory)
const fs = require('fs'), path = require('path');
const { strip, city } = require('./lift-icons.js');
const { decode, encode } = require('./pngio.js');

const out = __dirname;
const ITEM = path.resolve(__dirname, '../../../src/main/resources/assets/comedymonkeysaddons/textures/item');
const icon = path.join(ITEM, 'assembly_clamp.png');
if (!fs.existsSync(icon)) throw new Error('run design/models/clockwork-wastes.export.js first');
fs.copyFileSync(icon, path.join(out, 'assembly_clamp.png'));
const row = [...city, path.join(ITEM, 'lift.png'), path.join(ITEM, 'lift_call_plate.png'), icon];
strip([row], path.join(out, 'assembly-clamp-icon-review.png'));

// the small sizes: the icon at 1x, 2x and 3x on an inventory slot's grey, beside the Charging Alcove's and the Maker Statue's
{
  const BG = [139, 139, 139], items = ['charging_alcove', 'maker_statue', 'assembly_clamp'].map((n) => decode(fs.readFileSync(path.join(ITEM, n + '.png'))));
  const scales = [1, 2, 3], GAP = 6, W = items.length * scales.reduce((a, s) => a + 16 * s + GAP, 0), H = 16 * 3 + 2 * GAP;
  const o = new Uint8Array(W * H * 4);
  for (let i = 0; i < W * H; i++) { o[i * 4] = BG[0]; o[i * 4 + 1] = BG[1]; o[i * 4 + 2] = BG[2]; o[i * 4 + 3] = 255; }
  let x0 = GAP / 2;
  for (const s of scales)
    for (const im of items) {
      for (let y = 0; y < 16 * s; y++) for (let x = 0; x < 16 * s; x++) {
        const si = (Math.floor(y / s) * im.w + Math.floor(x / s)) * 4, al = im.data[si + 3] / 255, di = ((GAP + y) * W + x0 + x) * 4;
        for (let c = 0; c < 3; c++) o[di + c] = Math.round(im.data[si + c] * al + BG[c] * (1 - al));
      }
      x0 += 16 * s + GAP;
    }
  fs.writeFileSync(path.join(out, 'assembly-clamp-icon-sizes.png'), encode(W, H, o));
}
console.log('wrote assembly-clamp-icon-review.png and assembly-clamp-icon-sizes.png');
