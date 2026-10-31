// Tiles the va-parts shots into sheets over a white ground (Blockbench's shots have a clear sky), nearest-pixel so the
// textures stay crisp: va-parts-sheet-dormant.png and -awake.png (gameplay distance, each shot a pair: before left, after
// right; two shots a row) and va-parts-sheet-close.png (each close-up's before left, after right, a row each).
// node design/texture-reviews/clockwork-wastes-va-parts/sheet.js
const fs = require('fs');
const path = require('path');
const { decode, encode } = require('../clockwork-wastes-icons/pngio.js');
const DIR = path.join(__dirname, 'shots');
const GAP = 8;
function tile(files, cols, cellW, out) {
  const imgs = files.map((f) => decode(fs.readFileSync(f)));
  const cellH = Math.round(imgs[0].h * cellW / imgs[0].w), rows = Math.ceil(imgs.length / cols);
  const W = cols * cellW + (cols + 1) * GAP, H = rows * cellH + (rows + 1) * GAP;
  const o = new Uint8Array(W * H * 4).fill(40);
  for (let i = 3; i < o.length; i += 4) o[i] = 255;
  imgs.forEach((img, k) => {
    const ox = GAP + (k % cols) * (cellW + GAP), oy = GAP + Math.floor(k / cols) * (cellH + GAP);
    for (let y = 0; y < cellH; y++) for (let x = 0; x < cellW; x++) {
      const sx = Math.min(img.w - 1, Math.floor(x * img.w / cellW)), sy = Math.min(img.h - 1, Math.floor(y * img.h / cellH));
      const s = (sy * img.w + sx) * 4, d = ((oy + y) * W + ox + x) * 4, a = img.data[s + 3] / 255;
      for (let c = 0; c < 3; c++) o[d + c] = Math.round(img.data[s + c] * a + 255 * (1 - a));
    }
  });
  fs.writeFileSync(path.join(__dirname, out), encode(W, H, o));
  console.log(out, files.length, 'shots');
}
for (const clip of ['dormant', 'awake'])
  tile(fs.readdirSync(DIR).filter((f) => f.endsWith(`-${clip}.png`)).sort().map((f) => path.join(DIR, f)), 2, 800, `va-parts-sheet-${clip}.png`);
// close-ups: numbered in pairs, before then after
tile(fs.readdirSync(path.join(DIR, 'close')).filter((f) => f.endsWith('.png')).sort().map((f) => path.join(DIR, 'close', f)), 2, 700, 'va-parts-sheet-close.png');
