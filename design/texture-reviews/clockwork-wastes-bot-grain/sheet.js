// D77 review sheets from shots.js's screenshots: one row per shot (gameplay dormant and lit, close dormant and lit, an action
// frame), one column per version (before: the sheen bands; after A: the grain, lit top/left edge kept; after B: the grain
// with the house's dark bottom/right edge too), captioned in Minecraft's font. The shots have a clear sky, laid here over a
// neutral grey. Gameplay shots are cropped at 1:1 (the size they are in a 1200 x 900 window); close-ups and the action frame
// are fitted to the cell. Writes contact-sheet.png (all three bots), sheet-<bot>.png (bigger cells) and
// textures-before-after.png (each bot's atlas at 4x, the three versions side by side).
// node design/texture-reviews/clockwork-wastes-bot-grain/sheet.js
const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');
const { decode, encode } = require('../clockwork-wastes-icons/pngio.js');
const REPO = path.resolve(__dirname, '../../..');
const MC_JAR = path.join(REPO, 'build/moddev/artifacts/neoforge-21.1.248-client-extra-aka-minecraft-resources.jar');
// the font sheet is a 1-bit palette PNG, so it takes the city vault GUI script's full decoder (design/gui/city-vault.gui.js)
const zlib = require('zlib');
function decodeAny(buf) {
  let o = 8, idat = [], w, h, ct, depth = 8, plte = null, trns = null;
  while (o < buf.length) { const len = buf.readUInt32BE(o), type = buf.toString('ascii', o + 4, o + 8);
    if (type === 'IHDR') { w = buf.readUInt32BE(o + 8); h = buf.readUInt32BE(o + 12); depth = buf[o + 16]; ct = buf[o + 17]; }
    if (type === 'PLTE') plte = buf.slice(o + 8, o + 8 + len); if (type === 'tRNS') trns = buf.slice(o + 8, o + 8 + len);
    if (type === 'IDAT') idat.push(buf.slice(o + 8, o + 8 + len)); o += 12 + len; }
  const bpp = ct === 6 ? 4 : ct === 2 ? 3 : ct === 4 ? 2 : 1, raw = zlib.inflateSync(Buffer.concat(idat)), stride = Math.ceil(w * bpp * depth / 8), fb = Math.max(1, (bpp * depth) >> 3), out = new Uint8Array(w * h * 4), prev = new Uint8Array(stride), cur = new Uint8Array(stride);
  for (let y = 0; y < h; y++) {
    const f = raw[y * (stride + 1)], line = raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1));
    for (let i = 0; i < stride; i++) { const a = i >= fb ? cur[i - fb] : 0, b = prev[i], c = i >= fb ? prev[i - fb] : 0; let v = line[i];
      if (f === 1) v += a; else if (f === 2) v += b; else if (f === 3) v += (a + b) >> 1; else if (f === 4) { const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c); v += pa <= pb && pa <= pc ? a : pb <= pc ? b : c; }
      cur[i] = v & 255; }
    if (ct === 3 || depth < 8) { for (let x = 0; x < w; x++) { const bit = x * depth, v = depth === 8 ? cur[x] : (cur[bit >> 3] >> (8 - depth - (bit & 7))) & ((1 << depth) - 1), d = (y * w + x) * 4;
      if (ct === 3) { out[d] = plte[v * 3]; out[d + 1] = plte[v * 3 + 1]; out[d + 2] = plte[v * 3 + 2]; out[d + 3] = trns && v < trns.length ? trns[v] : 255; } else { const g = Math.round(v * 255 / ((1 << depth) - 1)); out[d] = out[d + 1] = out[d + 2] = g; out[d + 3] = 255; } } prev.set(cur); continue; }
    for (let x = 0; x < w; x++) { const d = (y * w + x) * 4; if (bpp >= 3) { out[d] = cur[x * bpp]; out[d + 1] = cur[x * bpp + 1]; out[d + 2] = cur[x * bpp + 2]; out[d + 3] = bpp === 4 ? cur[x * bpp + 3] : 255; } else { out[d] = out[d + 1] = out[d + 2] = cur[x * bpp]; out[d + 3] = bpp === 2 ? cur[x * bpp + 1] : 255; } }
    prev.set(cur);
  }
  return { w, h, data: out };
}
const FONT = decodeAny(execFileSync('unzip', ['-p', MC_JAR, 'assets/minecraft/textures/font/ascii.png'], { maxBuffer: 1 << 24 }));
const BOTS = ['scuttler', 'sentry', 'custodian'];
const VERS = [['sheen', 'Before: the sheen bands'], ['grain', 'After A: grain, lit edge'], ['framed', 'After B: grain, lit + dark edge']];
const SHOTS = [['gameplay-dormant', 'gameplay, dormant'], ['gameplay-lit', 'gameplay, lit'], ['close-dormant', 'close, dormant'], ['close-lit', 'close, lit'], ['action', 'action frame']];
const ACTION = { scuttler: 'leap 0.55 s', sentry: 'fire 0.7 s', custodian: 'slam 0.9 s' };
const BG = [140, 140, 146], INK = [235, 235, 240], BAND = [34, 33, 44], GAP = 6;

const canvas = (w, h, rgb) => { const c = { w, h, data: new Uint8Array(w * h * 4) }; for (let i = 0; i < w * h; i++) c.data.set([...rgb, 255], i * 4); return c; };
const px = (c, x, y) => (y * c.w + x) * 4;
const glyphW = (ch) => { if (ch === ' ') return 3; const k = ch.charCodeAt(0), gx = (k % 16) * 8, gy = Math.floor(k / 16) * 8; let w = 0; for (let y = 0; y < 8; y++) for (let x = 0; x < 8; x++) if (FONT.data[px(FONT, gx + x, gy + y) + 3]) w = Math.max(w, x + 1); return w; };
function text(c, s, x, y, scale = 2, rgb = INK) {
  let cx = x;
  for (const ch of s) { const k = ch.charCodeAt(0), gx = (k % 16) * 8, gy = Math.floor(k / 16) * 8;
    if (ch !== ' ') for (let yy = 0; yy < 8; yy++) for (let xx = 0; xx < 8; xx++) if (FONT.data[px(FONT, gx + xx, gy + yy) + 3])
      for (let a = 0; a < scale; a++) for (let b = 0; b < scale; b++) { const X = cx + xx * scale + a, Y = y + yy * scale + b; if (X < c.w && Y < c.h) c.data.set([...rgb, 255], px(c, X, Y)); }
    cx += (glyphW(ch) + 1) * scale; }
}
// the lit pixels' box over several shots (so the three versions share one framing)
function bbox(imgs) { let x0 = 1e9, y0 = 1e9, x1 = -1, y1 = -1; for (const im of imgs) for (let y = 0; y < im.h; y++) for (let x = 0; x < im.w; x++) if (im.data[px(im, x, y) + 3] > 8) { x0 = Math.min(x0, x); y0 = Math.min(y0, y); x1 = Math.max(x1, x); y1 = Math.max(y1, y); } return [x0, y0, x1, y1]; }
// draws the region (sx, sy, sw, sh) of a shot into a cell, over the grey, scaled by area averaging (or 1:1)
function draw(dst, im, sx, sy, sw, sh, dx, dy, cw, ch) {
  const s = sw / cw;
  for (let y = 0; y < ch; y++) for (let x = 0; x < cw; x++) {
    const ax = sx + x * s, ay = sy + y * s, n = Math.max(1, Math.round(s)); let r = 0, g = 0, b = 0, k = 0;
    for (let j = 0; j < n; j++) for (let i = 0; i < n; i++) { const X = Math.floor(ax + i * s / n), Y = Math.floor(ay + j * s / n);
      let c = BG; if (X >= 0 && Y >= 0 && X < im.w && Y < im.h) { const o = px(im, X, Y), a = im.data[o + 3] / 255; c = [0, 1, 2].map((q) => im.data[o + q] * a + BG[q] * (1 - a)); }
      r += c[0]; g += c[1]; b += c[2]; k++; }
    dst.data.set([Math.round(r / k), Math.round(g / k), Math.round(b / k), 255], px(dst, dx + x, dy + y));
  }
}
function sheet(bots, cw, out) {
  const ch = Math.round(cw * 3 / 4), LW = 230, HH = 40, BH = 34;
  const W = LW + 3 * (cw + GAP) + GAP, H = HH + bots.length * (BH + SHOTS.length * (ch + GAP)) + GAP;
  const c = canvas(W, H, BAND);
  VERS.forEach(([, label], k) => text(c, label, LW + GAP + k * (cw + GAP) + 6, 12));
  let y = HH;
  for (const bot of bots) {
    text(c, bot[0].toUpperCase() + bot.slice(1), 10, y + 8, 3);
    y += BH;
    for (const [shot, label] of SHOTS) {
      const imgs = VERS.map(([v]) => decode(fs.readFileSync(path.join(__dirname, 'shots', bot, `${v}-${shot}.png`))));
      const [x0, y0, x1, y1] = bbox(imgs), mx = (x0 + x1) / 2, my = (y0 + y1) / 2;
      let sw, sh;
      if (shot.startsWith('gameplay')) { sw = Math.min(imgs[0].w, cw); sh = Math.round(sw * 3 / 4); }
      else { sw = Math.max((x1 - x0) * 1.12, (y1 - y0) * 1.12 * 4 / 3); sh = sw * 3 / 4; }
      imgs.forEach((im, k) => draw(c, im, mx - sw / 2, my - sh / 2, sw, sh, LW + GAP + k * (cw + GAP), y, cw, ch));
      text(c, label, 10, y + 8);
      text(c, shot.startsWith('gameplay') ? '1:1 crop' : shot === 'action' ? ACTION[bot] : 'fitted', 10, y + 30, 2, [150, 148, 170]);
      y += ch + GAP;
    }
  }
  fs.writeFileSync(path.join(__dirname, out), encode(W, H, c.data));
  console.log(out, W + ' x ' + H);
}
sheet(BOTS, 420, 'contact-sheet.png');
for (const b of BOTS) sheet([b], 600, `sheet-${b}.png`);
// the atlases, 4x, three versions side by side (the review's bbmodels' textures)
{
  const S = 4, HH = 40, LW = 140;
  const atl = BOTS.map((b) => VERS.map(([v]) => decode(fs.readFileSync(path.join(__dirname, 'models', `${b}-${v}.png`)))));
  const colW = Math.max(...atl.flat().map((a) => a.w)) * S;
  const W = LW + 3 * (colW + GAP) + GAP, H = HH + atl.reduce((n, r) => n + r[0].h * S + GAP, 0) + GAP;
  const c = canvas(W, H, BAND);
  VERS.forEach(([, label], k) => text(c, label, LW + GAP + k * (colW + GAP) + 4, 12));
  let y = HH;
  atl.forEach((row, r) => { text(c, BOTS[r], 10, y + 8);
    row.forEach((a, k) => { for (let j = 0; j < a.h * S; j++) for (let i = 0; i < a.w * S; i++) { const o = px(a, Math.floor(i / S), Math.floor(j / S)), al = a.data[o + 3] / 255;
      const checker = ((i >> 3) + (j >> 3)) & 1 ? 70 : 58; c.data.set([0, 1, 2].map((q) => Math.round(a.data[o + q] * al + checker * (1 - al))).concat(255), px(c, LW + GAP + k * (colW + GAP) + i, y + j)); } });
    y += row[0].h * S + GAP; });
  fs.writeFileSync(path.join(__dirname, 'textures-before-after.png'), encode(W, H, c.data));
  console.log('textures-before-after.png', W + ' x ' + H);
}
