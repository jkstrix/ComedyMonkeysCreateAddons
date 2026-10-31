// The Charging Alcove's flat icon, redrawn (D74): the corner shelf in place of phase 1's thin ledge and toy cart, empty (a
// player's alcove, which the item places, has no trinket), on D75's crisp plain void-alloy plate. Drawn as every flat icon is
// (the exporter's ICONS in design/models/clockwork-wastes.export.js): 16 x 16, a step lighter than the block textures (the
// VA_ICON palette), a 1 px #121118 outline round the silhouette (D50). Approved (D76, the plain version) and in the mod: the
// rows, palette and grain below are the exporter's ICONS.charging_alcove (keep the two alike). The exporter now writes the new
// icon to charging_alcove.png here too, so phase 1's icon, the 'before', is kept as charging_alcove_old.png.
// node charging-alcove-icon.js  -> charging_alcove_new.png (+ charging_alcove_new_seams.png, the option), charging-alcove-
// icon-review.png (8x: the approved neighbouring icons, then before and after; the option under the after),
// charging-alcove-icon-sizes.png (16, 32 and 64 px on an inventory slot's grey), charging-alcove-icon.bbmodel (cards at
// 1 block and 4x beside the block itself, for Blockbench)
const fs = require('fs'), path = require('path');
const { textureSet, hash, preview, C } = require('../../models/process-box.lib.js');
const { decode, encode } = require('./pngio.js');

const out = __dirname;
const ITEM = path.resolve(__dirname, '../../../src/main/resources/assets/comedymonkeysaddons/textures/item');
const VA_ICON = { H: '#5d5778', A: '#4c475f', B: '#3a3650', C: '#2a2838', L: '#7f76a8', S: '#b8b0d8' };

// The alcove from the front, as phase 1's icon: the arch, the two cheeks (lit edge H on the left and top, the dark edge C on
// the right and bottom, D75's plate a step lighter), the dark niche, the contact plate up its back (s: a pixel left of
// phase 1's, so the shelf stands clear of it and doesn't read as one hooked shape), the sill. The corner shelf where the
// ledge was, on the cheek's side (the right, as phase 1's ledge and the other icons draw east), at chest height: its top catching the light (h), its front in shade (e), the
// stepped bracket under it (b). 'A' is the plate's field: its grain (three close shades) is added below.
const rows = [
  '     HHHHHH     ',
  '    HAAAAAAC    ',
  '   HAAkkkkAAC   ',
  '   HAkkkkkkAC   ',
  '   HAkkkkkkAC   ',
  '   HAkkskkkAC   ',
  '   HAkkskkkAC   ',
  '   HAkkskkkAC   ',
  '   HAkkskkkAC   ',
  '   HAkkskhhAC   ',
  '   HAkkskeeAC   ',
  '   HAkkkkkbAC   ',
  '   HAkkkkkkAC   ',
  '   HABBBBBBAC   ',
  '   HAAAAAAAAC   ',
  '   CCCCCCCCCC   '];
const pal = { ...VA_ICON, k: '#1b1a22', s: '#6d6696', h: VA_ICON.H, e: VA_ICON.B, b: VA_ICON.B };
// the plate's grain on the field: mostly A, a darker shade (about 12%) and a lighter one (about 7%), as D75's three shades
const GRAIN = { lo: '#48435a', hi: '#514b66' };
const grain = (x, y) => { const r = hash(x, y, 611); return r < 0.12 ? GRAIN.lo : r > 0.93 ? GRAIN.hi : VA_ICON.A; };
// the option: the plate's seams where the alcove's blocks meet (y 16 and 32 of its 48 px: rows 5 and 10 here), a dark
// row where the upper plate ends on each cheek
const SEAM_ROWS = [5, 10];
function icon(seams) {
  return (x, y) => {
    if (x < 0 || y < 0 || x > 15 || y > 15) return null;
    const k = rows[y][x];
    if (k === ' ') return null;
    if (seams && SEAM_ROWS.includes(y) && (x === 3 || x === 4 || x === 11 || x === 12)) return VA_ICON.C;
    if (k === 'A') return grain(x, y);
    return pal[k];
  };
}
if (rows.length !== 16 || rows.some((r) => r.length !== 16)) throw new Error('the icon must be 16 x 16');

// painted as the exporter paints ICONS: the icon, and the outline in every empty pixel beside it
const T = textureSet();
const ICONS = { charging_alcove_new: icon(false), charging_alcove_new_seams: icon(true) };
for (const [id, at] of Object.entries(ICONS)) {
  T.paint(id, (x, y) => at(x, y) || ([[1, 0], [-1, 0], [0, 1], [0, -1]].some(([dx, dy]) => at(x + dx, y + dy)) ? '#121118' : null));
  fs.writeFileSync(path.join(out, id + '.png'), T.OWN[id]);
}

// the 8x review strip (as lift-icons.js draws it): rows of icons on mid-grey tiles
function strip(files, file) {
  const S = 8, TILE = 16 * S + 8, GAP = 8, BG = [139, 139, 139];
  const rowsIm = files.map((r) => r.map((f) => (f ? decode(fs.readFileSync(f)) : null)));
  const cols = Math.max(...rowsIm.map((r) => r.length)), W = cols * (TILE + GAP) - GAP, H = rowsIm.length * (TILE + GAP) - GAP;
  const o = new Uint8Array(W * H * 4).fill(255);
  rowsIm.forEach((r, ry) => r.forEach((im, cx) => {
    if (!im) return;
    const ox = cx * (TILE + GAP), oy = ry * (TILE + GAP);
    for (let y = 0; y < TILE; y++) for (let x = 0; x < TILE; x++) {
      const di = ((oy + y) * W + ox + x) * 4; let c = BG;
      const px = Math.floor((x - 4) / S), py = Math.floor((y - 4) / S);
      if (px >= 0 && py >= 0 && px < 16 && py < 16) {
        const sx = Math.floor(px * im.w / 16), sy = Math.floor(py * im.h / 16), si = (sy * im.w + sx) * 4, al = im.data[si + 3] / 255;
        c = [0, 1, 2].map((i) => Math.round(im.data[si + i] * al + BG[i] * (1 - al)));
      }
      o[di] = c[0]; o[di + 1] = c[1]; o[di + 2] = c[2]; o[di + 3] = 255;
    }
  }));
  fs.writeFileSync(file, encode(W, H, o));
}
const NEIGHBOURS = ['cuckoo_clock', 'substation', 'sliding_door', 'derelict_street_lamp', 'conduit_pylon', 'maker_statue', 'lift', 'lift_call_plate', 'assembly_clamp'];
const neighbours = NEIGHBOURS.map((n) => path.join(ITEM, n + '.png'));
const before = path.join(out, 'charging_alcove_old.png'), after = path.join(out, 'charging_alcove_new.png'), option = path.join(out, 'charging_alcove_new_seams.png');
strip([[...neighbours, before, after], [...neighbours.map(() => null), null, option]], path.join(out, 'charging-alcove-icon-review.png'));

// the sizes it shows at: 16, 32 and 64 px on an inventory slot's grey; before, after, the option, the Maker Statue beside
{
  const BG = [139, 139, 139], items = [before, after, option, path.join(ITEM, 'maker_statue.png')].map((f) => decode(fs.readFileSync(f)));
  const scales = [1, 2, 4], GAP = 8, W = items.length * scales.reduce((a, s) => a + 16 * s + GAP, 0) + GAP, H = 16 * 4 + 2 * GAP;
  const o = new Uint8Array(W * H * 4);
  for (let i = 0; i < W * H; i++) { o[i * 4] = BG[0]; o[i * 4 + 1] = BG[1]; o[i * 4 + 2] = BG[2]; o[i * 4 + 3] = 255; }
  let x0 = GAP;
  for (const s of scales)
    for (const im of items) {
      const y0 = H - GAP - 16 * s;
      for (let y = 0; y < 16 * s; y++) for (let x = 0; x < 16 * s; x++) {
        const si = (Math.floor(y / s) * im.w + Math.floor(x / s)) * 4, al = im.data[si + 3] / 255, di = ((y0 + y) * W + x0 + x) * 4;
        for (let c = 0; c < 3; c++) o[di + c] = Math.round(im.data[si + c] * al + BG[c] * (1 - al));
      }
      x0 += 16 * s + GAP;
    }
  fs.writeFileSync(path.join(out, 'charging-alcove-icon-sizes.png'), encode(W, H, o));
}

// Blockbench (as icons.bbmodel and lift-icons.bbmodel): the icons as cards at 1 block and 4x, and beside them the block
// itself (the alcove's base and corner shelf, its plain plate shown as D75's 1 x 1 bond plate, va_bond_s, as drawn before the swap went in)
{
  const M = require('../../models/clockwork-wastes-materials.gen.js');
  const CP = require('../../models/clockwork-wastes-city-props.gen.js');
  require('../../models/clockwork-wastes-building-blocks.gen.js'); // (the bond plates are in the materials sheet now)
  const list = [...NEIGHBOURS.map((n) => [n, ITEM]), ['charging_alcove_old', out], ['charging_alcove_new', out], ['charging_alcove_new_seams', out]];
  const set = { OWN: {}, file: '' };
  for (const [n, d] of list) set.OWN[n] = fs.readFileSync(path.join(d, n + '.png'));
  const { cube, group, write } = preview('charging_alcove_icon', { 'w:': M.T, 'i:': set });
  const outl = [];
  const card = (n, s, tag) => C(`${n}_${tag}`, [8 - 8 * s, 0, 8], [8 + 8 * s, 16 * s, 8], { north: 'i:' + n, south: 'i:' + n }, { shade: false, uv: { north: [0, 0, 16, 16], south: [16, 0, 0, 16] } });
  list.forEach(([n], k) => {
    outl.push(group(n, [8, 0, 8], [k * 20, 0, 0], [cube(card(n, 1, 'x1'), [k * 20, 0, 0])]));
    outl.push(group(n + '_big', [8, 0, 8], [k * 70 - 20, 30, 60], [cube(card(n, 4, 'x4'), [k * 70 - 20, 30, 60])]));
  });
  const alc = [...CP.alcoveBase(), ...CP.shelf()].map((c) => ({ ...c, faces: Object.fromEntries(Object.entries(c.faces).map(([d, t]) => [d, t === 'va_plain' ? 'va_bond_s' : t])) }));
  const aoff = [list.length * 20 + 20, 0, 0];
  outl.push(group('charging_alcove_block', [8, 0, 8], aoff, alc.map((c) => cube(c, aoff))));
  write(path.join(out, 'charging-alcove-icon.bbmodel'), outl);
}
console.log('wrote charging_alcove_new.png, charging_alcove_new_seams.png, charging-alcove-icon-review.png, charging-alcove-icon-sizes.png, charging-alcove-icon.bbmodel');
