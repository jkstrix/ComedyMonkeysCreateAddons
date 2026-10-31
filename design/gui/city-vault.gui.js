// The City Vault's screen (spec D46): the GUI kit with a void-alloy material band instead of the Upgraded Chest's copper
// one. An offline before/after preview for the user, drawn from the mod's own upgraded_chest.png (read, not changed), at
// 3x, with the title and labels in Minecraft's font and a heart vault's loot in the slots. Writes
// design/texture-reviews/clockwork-wastes-gui/city_vault_{before,after_a,after_b}.png and city-vault-screen.png (all three
// side by side). With --mod it also writes the chosen band (D59: option A, the void-alloy band with its lit starlight
// seam) into the mod as textures/gui/city_vault_band.png: the band's inside (174 x 12), which CityVaultScreen draws over
// the kit's panel in place of a chest tier's tinted band.
// Usage: node city-vault.gui.js [--mod]
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');
const { execFileSync } = require('child_process');

const REPO = path.resolve(__dirname, '../..');
const ASSETS = path.join(REPO, 'src/main/resources/assets/comedymonkeysaddons/textures');
const MC_JAR = path.join(REPO, 'build/moddev/artifacts/neoforge-21.1.248-client-extra-aka-minecraft-resources.jar');
const OUT = path.join(REPO, 'design/texture-reviews/clockwork-wastes-gui');
const S = 3; // preview scale

// ---------- PNG in and out ----------
function decode(buf) {
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
function encode(w, h, data) {
  const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
  const crc = (b) => { let c = 0xffffffff; for (const x of b) c = crcT[(c ^ x) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
  const chunk = (t, body) => { const l = Buffer.alloc(4); l.writeUInt32BE(body.length); const tb = Buffer.concat([Buffer.from(t), body]); const c = Buffer.alloc(4); c.writeUInt32BE(crc(tb)); return Buffer.concat([l, tb, c]); };
  const ih = Buffer.alloc(13); ih.writeUInt32BE(w, 0); ih.writeUInt32BE(h, 4); ih[8] = 8; ih[9] = 6;
  const raw = Buffer.alloc((w * 4 + 1) * h); for (let y = 0; y < h; y++) Buffer.from(data.buffer, data.byteOffset + y * w * 4, w * 4).copy(raw, y * (w * 4 + 1) + 1);
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ih), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
const fromJar = (entry) => decode(execFileSync('unzip', ['-p', MC_JAR, entry], { maxBuffer: 1 << 24 }));
const hex = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];

// ---------- a canvas ----------
function canvas(w, h, bg) { const data = new Uint8Array(w * h * 4); if (bg) for (let i = 0; i < w * h; i++) data.set([...hex(bg), 255], i * 4); return { w, h, data }; }
const setPx = (c, x, y, rgb, a = 255) => { if (x < 0 || y < 0 || x >= c.w || y >= c.h) return; const o = (y * c.w + x) * 4; if (a >= 255) { c.data.set([...rgb, 255], o); return; } for (let i = 0; i < 3; i++) c.data[o + i] = Math.round(c.data[o + i] * (1 - a / 255) + rgb[i] * a / 255); c.data[o + 3] = 255; };
const getPx = (c, x, y) => { const o = (y * c.w + x) * 4; return [c.data[o], c.data[o + 1], c.data[o + 2], c.data[o + 3]]; };
function blit(dst, src, dx, dy, sx = 0, sy = 0, w = src.w, h = src.h) { for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) { const [r, g, b, a] = getPx(src, sx + x, sy + y); if (a) setPx(dst, dx + x, dy + y, [r, g, b], a); } }

// ---------- Minecraft's font (ascii.png: 16 x 16 glyphs of 8 x 8; a glyph's width is its last lit column + 1, then 1 px apart) ----------
const FONT = fromJar('assets/minecraft/textures/font/ascii.png');
const glyphW = (ch) => { if (ch === ' ') return 3; const k = ch.charCodeAt(0), gx = (k % 16) * 8, gy = Math.floor(k / 16) * 8; let w = 0; for (let y = 0; y < 8; y++) for (let x = 0; x < 8; x++) if (getPx(FONT, gx + x, gy + y)[3]) w = Math.max(w, x + 1); return w; };
const textW = (s) => [...s].reduce((n, ch) => n + glyphW(ch) + 1, 0) - 1;
function text(c, s, x, y, colour, shadow = false, scale = 1) {
  const draw = (ox, oy, rgb) => { let cx = ox; for (const ch of s) { const k = ch.charCodeAt(0), gx = (k % 16) * 8, gy = Math.floor(k / 16) * 8;
    for (let yy = 0; yy < 8; yy++) for (let xx = 0; xx < 8; xx++) if (getPx(FONT, gx + xx, gy + yy)[3]) for (let a = 0; a < scale; a++) for (let b = 0; b < scale; b++) setPx(c, cx + xx * scale + a, oy + yy * scale + b, rgb);
    cx += (glyphW(ch) + 1) * scale; } };
  const rgb = hex(colour); if (shadow) draw(x + scale, y + scale, rgb.map((v) => Math.floor(v / 4))); draw(x, y, rgb);
}

// ---------- the screen ----------
const CHEST = decode(fs.readFileSync(path.join(ASSETS, 'gui/upgraded_chest.png')));
const W = 176, PANEL = 76, H = PANEL + 4 + 96; // UpgradedChestMenu.height(3) = 76; MenuLayout: gap 4, inventory 96
const HEADER = 14;
const item = (file) => decode(fs.readFileSync(file));
const ITEMS = {
  elytra: fromJar('assets/minecraft/textures/item/elytra.png'), diamond: fromJar('assets/minecraft/textures/item/diamond.png'),
  emerald: fromJar('assets/minecraft/textures/item/emerald.png'), apple: fromJar('assets/minecraft/textures/item/golden_apple.png'),
  book: fromJar('assets/minecraft/textures/item/enchanted_book.png'),
  device: item(path.join(ASSETS, 'item/unknown_device.png')), scrap: item(path.join(ASSETS, 'item/clockwork_scrap.png')),
};
// the heart vault's loot (loot table chests/clockwork_city_heart_vault, as the progress notes list it), slot index -> [item, count]
const LOOT = { 4: ['elytra', 1], 10: ['device', 1], 16: ['device', 1], 11: ['diamond', 3], 15: ['emerald', 7], 20: ['apple', 1], 22: ['book', 1], 24: ['scrap', 9] };

// the band as the code draws it now: the texture's grey band multiplied by the copper tier colour (GuiKit.tintBand)
function copperBand(c) { const t = hex('#c4764e'); for (let y = 1; y < HEADER - 1; y++) for (let x = 1; x < W - 1; x++) { const [r, g, b] = getPx(c, x, y); setPx(c, x, y, [r * t[0] / 255, g * t[1] / 255, b * t[2] / 255].map(Math.round)); } }
// the void-alloy band: a cast bar, no mottle (the visual language): the lit facet along its top and left, a faint violet
// gradient down its face, the dark edge at the right. seam: a lit hairline along its foot (the uprising's light lives in
// seams), stopping short of the corners, or none
const VA = { hi: '#34304a', a: '#2a2838', b: '#232130', c: '#1d1c25', lo: '#16151c', seamOff: '#2e2a40' };
function voidBand(c, seam) {
  for (let y = 1; y < HEADER - 1; y++) for (let x = 1; x < W - 1; x++) {
    let col = y === 1 || x === 1 ? VA.hi : x === W - 2 ? VA.lo : y <= 4 ? VA.a : y <= 8 ? VA.b : VA.c;
    if (y === HEADER - 2) col = seam && x >= 4 && x <= W - 5 ? seam : VA.lo;
    setPx(c, x, y, hex(col));
  }
}
function screen({ band, seam, title }) {
  const c = canvas(W, H);
  blit(c, CHEST, 0, 0, 0, 0, W, H);
  if (band === 'copper') copperBand(c); else voidBand(c, seam);
  text(c, 'City Vault', Math.floor((W - textW('City Vault')) / 2), 3, title);
  text(c, 'Inventory', 8, PANEL + 4 + 4, '#33352f');
  for (const [slot, [name, n]] of Object.entries(LOOT)) { const x = 8 + (slot % 9) * 18, y = 18 + Math.floor(slot / 9) * 18; blit(c, ITEMS[name], x, y); if (n > 1) text(c, String(n), x + 17 - textW(String(n)), y + 9, '#ffffff', true); }
  return c;
}
const up = (c, s) => { const o = canvas(c.w * s, c.h * s); for (let y = 0; y < o.h; y++) for (let x = 0; x < o.w; x++) { const p = getPx(c, Math.floor(x / s), Math.floor(y / s)); if (p[3]) o.data.set(p, (y * o.w + x) * 4); } return o; };

const SHOTS = [
  ['city_vault_before', 'Now: the Upgraded Chest\'s copper band', { band: 'copper', title: '#2b2724' }],
  ['city_vault_after_a', 'A (recommended): void-alloy, lit seam, starlight title', { band: 'void', seam: '#7f76a8', title: '#b8b0d8' }],
  ['city_vault_after_b', 'B: void-alloy, dark seam, Netherite\'s light title', { band: 'void', seam: VA.seamOff, title: '#e6dee0' }],
];
fs.mkdirSync(OUT, { recursive: true });
const big = SHOTS.map(([file, , o]) => { const s = up(screen(o), S); fs.writeFileSync(path.join(OUT, file + '.png'), encode(s.w, s.h, s.data)); return s; });
// all three side by side on a dark sheet, each captioned in Minecraft's font (2x)
const PAD = 24, CAP = 40, sheet = canvas(PAD + big.length * (W * S + PAD), CAP + H * S + PAD, '#101014');
big.forEach((s, i) => { const x = PAD + i * (W * S + PAD); text(sheet, SHOTS[i][1], x, 12, '#8a8d88', false, 2); blit(sheet, s, x, CAP); });
fs.writeFileSync(path.join(OUT, 'city-vault-screen.png'), encode(sheet.w, sheet.h, sheet.data));
if (process.argv.includes('--mod')) {
  // the band of option A as previewed, inside its 1 px border (x 1..W-2, y 1..HEADER-2), without the title (the game
  // draws that, in the starlight core tone)
  const band = canvas(W - 2, HEADER - 2), plain = canvas(W, H); blit(plain, CHEST, 0, 0, 0, 0, W, H); voidBand(plain, SHOTS[1][2].seam);
  for (let y = 0; y < band.h; y++) for (let x = 0; x < band.w; x++) band.data.set(getPx(plain, x + 1, y + 1), (y * band.w + x) * 4);
  const file = path.join(ASSETS, 'gui/city_vault_band.png');
  fs.writeFileSync(file, encode(band.w, band.h, band.data));
  console.log('wrote', file);
}
console.log('wrote', OUT);
