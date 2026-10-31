// Proposed flat icons for the Lift and the Lift Call Plate (D62), in the style of the exporter's ICONS (16 x 16, a step
// lighter than the block textures, VA_ICON palette, a 1 px #121118 outline round the silhouette). Preview only: the rows and
// palettes below are what would go into design/models/clockwork-wastes.export.js ICONS once approved.
// Approved (D65): the exporter's ICONS now hold the approved rows (lift, and lift_call_plate = callPlate(true)); this stays the review script.
// node lift-icons.js  -> lift.png, lift_call_plate.png (+ the alternative), lift-icons-review.png (8x, beside the city icons)
const fs = require('fs'), path = require('path');
const { textureSet } = require('../../models/process-box.lib.js');
const { decode, encode } = require('./pngio.js');

const VA_ICON = { H: '#5d5778', A: '#4c475f', B: '#3a3650', C: '#2a2838', L: '#7f76a8', S: '#b8b0d8' };

// the Lift: the deck halfway up its shaft, from the front: two guide rails, the deck across them (its top, the lit groove
// along its front edge, its front face), clamps biting the rails, the guard rail (behind) and the west post above
// it, the control post with its lens, the cross beam under the deck. The empty shaft above and below says "it rides".
const lift = { rows: [
  '                ',
  ' HB          HB ',
  ' HB          HB ',
  ' HB          HB ',
  ' HBBBBBBBBBBBHB ',
  ' HBB      AA HB ',
  ' HBB      SA HB ',
  ' HBB      AA HB ',
  'HHHAAAAAAAAAAHHH',
  'HABLLLLLLLLLLBAH',
  'HBBCCCCCCCCCCBBH',
  ' HB   BBBB   HB ',
  ' HB          HB ',
  ' HB          HB ',
  ' HB          HB ',
  '                '],
  pal: { ...VA_ICON } };

// the call plate: the lens-in-cog (the Substation's sigil, eight teeth) on its square void-alloy mount, chamfered;
// lit (a call waiting) or dark (as placed)
const COG = ['....##....', '.#..##..#.', '..######..', '..##oo##..', '###oooo###', '###oooo###', '..##oo##..', '..######..', '.#..##..#.', '....##....'];
function callPlate(lit) {
  const rows = [];
  for (let y = 0; y < 16; y++) {
    let r = '';
    for (let x = 0; x < 16; x++) {
      const inPlate = x >= 2 && x <= 13 && y >= 2 && y <= 13 && !((x === 2 || x === 13) && (y === 2 || y === 13));
      if (!inPlate) { r += ' '; continue; }
      const k = (COG[y - 3] || '')[x - 3];
      if (k === 'o') r += x === 7 && y === 7 ? (lit ? 'S' : 'g') : lit ? 'L' : 'C';
      else if (k === '#') r += 'B';
      else if (y === 2 || x === 2) r += 'H';
      else if (y === 13 || x === 13) r += 'C';
      else r += 'A';
    }
    rows.push(r);
  }
  return { rows, pal: { ...VA_ICON, g: '#4c475f' } };
}
const ICONS = { lift, lift_call_plate: callPlate(true), lift_call_plate_dark: callPlate(false) };

// the exporter's painting, as it is: the icon, and the outline in every empty pixel beside it
const T = textureSet();
const out = __dirname;
for (const [id, icon] of Object.entries(ICONS)) {
  if (icon.rows.length !== 16 || icon.rows.some((r) => r.length !== 16)) throw new Error(id + ': the icon must be 16 x 16');
  const at = (x, y) => (x < 0 || y < 0 || x > 15 || y > 15 ? null : icon.pal[icon.rows[y][x]] || null);
  T.paint('item_' + id, (x, y) => at(x, y) || ([[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => at(x + dx, y + dy)) ? '#121118' : null));
  fs.writeFileSync(path.join(out, id + '.png'), T.OWN['item_' + id]);
}

// the 8x review strip: rows of icons on mid-grey tiles (as outline-before-after.png): the Cuckoo Clock, the approved city
// icons, then the new ones. Extra rows (process.argv: before files) can be stacked above.
const ITEM = path.resolve(__dirname, '../../../src/main/resources/assets/comedymonkeysaddons/textures/item');
function strip(files, file) {
  const S = 8, TILE = 16 * S + 8, GAP = 8, BG = [139, 139, 139];
  const rows = files.map((r) => r.map((f) => (f ? decode(fs.readFileSync(f)) : null)));
  const cols = Math.max(...rows.map((r) => r.length)), W = cols * (TILE + GAP) - GAP, H = rows.length * (TILE + GAP) - GAP;
  const o = new Uint8Array(W * H * 4).fill(255);
  rows.forEach((r, ry) => r.forEach((im, cx) => {
    const ox = cx * (TILE + GAP), oy = ry * (TILE + GAP);
    for (let y = 0; y < TILE; y++) for (let x = 0; x < TILE; x++) {
      const di = ((oy + y) * W + ox + x) * 4; let c = BG;
      const px = Math.floor((x - 4) / S), py = Math.floor((y - 4) / S);
      if (im && px >= 0 && py >= 0 && px < 16 && py < 16) {
        const sx = Math.floor(px * im.w / 16), sy = Math.floor(py * im.h / 16), si = (sy * im.w + sx) * 4, al = im.data[si + 3] / 255;
        c = [0, 1, 2].map((i) => Math.round(im.data[si + i] * al + BG[i] * (1 - al)));
      }
      o[di] = c[0]; o[di + 1] = c[1]; o[di + 2] = c[2]; o[di + 3] = 255;
    }
  }));
  fs.writeFileSync(file, encode(W, H, o));
}
const city = ['cuckoo_clock', 'substation', 'sliding_door', 'derelict_street_lamp', 'conduit_pylon', 'charging_alcove', 'maker_statue'].map((n) => path.join(ITEM, n + '.png'));
module.exports = { ICONS, strip, city };
if (require.main === module) {
  strip([[...city, path.join(out, 'lift.png'), path.join(out, 'lift_call_plate.png'), path.join(out, 'lift_call_plate_dark.png')]], path.join(out, 'lift-icons-review.png'));
  console.log('wrote', Object.keys(ICONS).join(', '), 'and lift-icons-review.png');
}
