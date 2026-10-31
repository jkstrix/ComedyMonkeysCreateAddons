// Tiles the base-swap shots (shots/NN-<block>-<dormant|awake>.png) into one sheet per clip, two to a row, each over a
// white ground (Blockbench's shots have a clear sky), scaled by nearest pixel so the textures stay crisp.
// node design/texture-reviews/clockwork-wastes-base-swap/sheet.js
const fs = require('fs');
const path = require('path');
const { decode, encode } = require('../clockwork-wastes-icons/pngio.js');
const DIR = path.join(__dirname, 'shots');
const CELL_W = 800, COLS = 2, GAP = 8;
for (const clip of ['dormant', 'awake']) {
  const files = fs.readdirSync(DIR).filter((f) => f.endsWith(`-${clip}.png`)).sort();
  const imgs = files.map((f) => decode(fs.readFileSync(path.join(DIR, f))));
  const cellH = Math.round(imgs[0].h * CELL_W / imgs[0].w), rows = Math.ceil(imgs.length / COLS);
  const W = COLS * CELL_W + (COLS + 1) * GAP, H = rows * cellH + (rows + 1) * GAP;
  const out = new Uint8Array(W * H * 4).fill(40);
  for (let i = 3; i < out.length; i += 4) out[i] = 255;
  imgs.forEach((img, k) => {
    const ox = GAP + (k % COLS) * (CELL_W + GAP), oy = GAP + Math.floor(k / COLS) * (cellH + GAP);
    for (let y = 0; y < cellH; y++) for (let x = 0; x < CELL_W; x++) {
      const sx = Math.min(img.w - 1, Math.floor(x * img.w / CELL_W)), sy = Math.min(img.h - 1, Math.floor(y * img.h / cellH));
      const s = (sy * img.w + sx) * 4, d = ((oy + y) * W + ox + x) * 4, a = img.data[s + 3] / 255;
      for (let c = 0; c < 3; c++) out[d + c] = Math.round(img.data[s + c] * a + 255 * (1 - a));
    }
  });
  const file = path.join(__dirname, `base-swap-sheet-${clip}.png`);
  fs.writeFileSync(file, encode(W, H, out));
  console.log(file, files.length, 'shots');
}
