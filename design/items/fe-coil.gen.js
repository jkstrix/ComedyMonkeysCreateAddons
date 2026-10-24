// FE Coil item icon (design/specs/trade-bot.md D56): copper windings on a bobbin with steel end flanges, a redstone
// core showing at both ends, and two brass leads below. Writes the 16x16 icon and a large preview.
//   node design/items/fe-coil.gen.js [--game]   (--game also copies the icon into the mod's item textures)
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

// palette, sampled to sit beside the mod's upgrade items (copper body, dark outline, gold and red accents)
const P = {
  '.': null,
  O: '#4a2a20', // outline
  c: '#c46a4e', // copper
  C: '#e8936f', // copper highlight
  d: '#8c4532', // copper shadow (between turns)
  k: '#2c2e33', // steel dark
  s: '#4b4f57', // steel
  S: '#6d727b', // steel highlight
  r: '#a82c2a', // redstone
  R: '#e05a4a', // redstone highlight
  g: '#e8b84a', // brass lead
  G: '#a87a28', // brass shadow
};

const ICON = [
  '................',
  '.kkk........kkk.',
  '.kSk........kSk.',
  '.kSkOOOOOOOOkSk.',
  '.kSkCdCdCdCdkSk.',
  '.kSkCdCdCdCdkSk.',
  'RkSkcdcdcdcdkSkR',
  'rkskcdcdcdcdkskr',
  'rkskcdcdcdcdkskr',
  '.kskdOdOdOdOksk.',
  '.kskOOOOOOOOksk.',
  '.ksk.gG..gG.ksk.',
  '.kkk.gG..gG.kkk.',
  '.....gG..gG.....',
  '.....GG..GG.....',
  '................',
];

function png(w, h, rgba) {
  const crc = (buf) => { let c = ~0; for (const b of buf) { c ^= b; for (let k = 0; k < 8; k++) c = (c >>> 1) ^ (0xedb88320 & -(c & 1)); } return ~c >>> 0; };
  const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length); const td = Buffer.concat([Buffer.from(type), data]); const c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([len, td, c]); };
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) { raw[y * (w * 4 + 1)] = 0; for (let x = 0; x < w * 4; x++) raw[y * (w * 4 + 1) + 1 + x] = rgba[y * w * 4 + x]; }
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
const hex = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];

function render(grid) {
  const px = new Uint8Array(16 * 16 * 4);
  grid.forEach((row, y) => {
    if (row.length !== 16) throw new Error('row ' + y + ' is ' + row.length + ' wide');
    [...row].forEach((ch, x) => {
      if (!(ch in P)) throw new Error('unknown colour ' + ch);
      if (!P[ch]) return;
      const [r, g, b] = hex(P[ch]);
      px.set([r, g, b, 255], (y * 16 + x) * 4);
    });
  });
  return px;
}

const icon = render(ICON);
const here = __dirname;
fs.writeFileSync(path.join(here, 'fe_coil.png'), png(16, 16, icon));
// preview: 16x on the mod's dark item-slot grey, beside nothing else (the review page lines it up with the other items)
const S = 16, W = 16 * S, prev = new Uint8Array(W * W * 4);
for (let y = 0; y < W; y++) for (let x = 0; x < W; x++) {
  const o = (y * W + x) * 4, i = (Math.floor(y / S) * 16 + Math.floor(x / S)) * 4;
  prev.set(icon[i + 3] ? [icon[i], icon[i + 1], icon[i + 2], 255] : [60, 60, 66, 255], o);
}
fs.writeFileSync(path.join(here, 'fe_coil.preview.png'), png(W, W, prev));
if (process.argv.includes('--game')) {
  const out = path.resolve(here, '../../src/main/resources/assets/comedymonkeysaddons/textures/item/fe_coil.png');
  fs.copyFileSync(path.join(here, 'fe_coil.png'), out);
  console.log('wrote ' + out);
}
console.log('wrote fe_coil.png and fe_coil.preview.png');
