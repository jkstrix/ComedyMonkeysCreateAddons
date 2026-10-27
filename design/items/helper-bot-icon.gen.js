// Helper Bot's flat inventory icon (design/briefs/helper-bot.md, Builder Notes): his binocular eye pods with cyan eyes
// and the whistle on the left pod, the whisker grille, his body with its dye panels, the post and the foot plate. A
// second, greyscale layer covers only his chest panels; the game tints it with his dye (tintindex 1).
// Writes a large preview of the icon in plain and a few dyes, tinted as the game will.
//   node design/items/helper-bot-icon.gen.js [--game]   (--game also writes both layers into the mod's item textures)
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

// palette: a touch lighter than his block textures, as the mod's other icons are
const P = {
  '.': null,
  k: '#2c2e33', // steel dark (frame)
  s: '#4b4f57', // steel
  S: '#6d727b', // steel highlight (lid plates)
  b: '#c9a75a', // brass
  B: '#a87a28', // brass shadow (rims' lower edge, whiskers)
  c: '#c46a4e', // copper (whistle pipe)
  g: '#1c2228', // dark glass
  i: '#8fcad4', // cyan iris
  I: '#e6f6f8', // catchlight
  p: '#9a9c96', // chest panel (dye zone; the tint layer covers these)
  P: '#8a8c86', // chest panel, shaded
  a: '#a2a49e', // foot plate
  A: '#7c7f79', // foot plate, shaded
};

const ICON = [
  '..bb............',
  '...c............',
  '.SSSSSS..SSSSSS.',
  '.ssssss..ssssss.',
  '.bgIigb..bgIigb.',
  '.bgiigbkkbgiigb.',
  '.BBBBBB..BBBBBB.',
  '....bBbBBbBb....',
  '...skkkkkkkks...',
  '..s.kpPppPpk.s..',
  '..s.kbbbbbbk.s..',
  '..s.kPppPppk.s..',
  '.b.bkkkkkkkkb.b.',
  '.......ss.......',
  '...aaaaaaaaaa...',
  '....AAAAAAAA....',
];
// the tint layer: light grey over the panels only (the game multiplies it by his dye)
const MASK = { p: '#e2e2e2', P: '#c9c9c9' };
// the mod's muted dye colours (LineColors.DYES), brightened for icons as HelperBotClient.registerItemColors does
const DYES = { white: 0x888c8b, red: 0x7f312c, blue: 0x383d7a, yellow: 0xa58e31, green: 0x546629, black: 0x353537 };
const PLAIN = 0x9a9c96;
const ICON_LIGHTEN = 1.3;

const rgb = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const lighten = (c) => [16, 8, 0].map((s) => Math.min(255, Math.round(((c >> s) & 255) * ICON_LIGHTEN)));
function png(w, h, data) {
  const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
  const crc = (buf) => { let c = 0xffffffff; for (const b of buf) c = crcT[(c ^ b) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
  const chunk = (type, body) => { const len = Buffer.alloc(4); len.writeUInt32BE(body.length); const tb = Buffer.concat([Buffer.from(type), body]); const cr = Buffer.alloc(4); cr.writeUInt32BE(crc(tb)); return Buffer.concat([len, tb, cr]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) { raw[y * (w * 4 + 1)] = 0; Buffer.from(data.buffer, y * w * 4, w * 4).copy(raw, y * (w * 4 + 1) + 1); }
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}
const layer = (pick) => {
  const d = new Uint8Array(16 * 16 * 4);
  ICON.forEach((row, y) => [...row].forEach((ch, x) => {
    const col = pick(ch);
    if (!col) return;
    const o = (y * 16 + x) * 4;
    [d[o], d[o + 1], d[o + 2]] = rgb(col);
    d[o + 3] = 255;
  }));
  return d;
};
if (ICON.some((r) => r.length !== 16) || ICON.length !== 16) throw new Error('icon must be 16x16');
const base = layer((ch) => P[ch]), mask = layer((ch) => MASK[ch]);

// preview: the icon in plain and a few dyes, 12x, on the inventory's grey
const tints = [['plain', [PLAIN >> 16, (PLAIN >> 8) & 255, PLAIN & 255]], ...Object.entries(DYES).map(([n, c]) => [n, lighten(c)])];
const S = 12, W = tints.length * (16 * S + 8) + 8, H = 16 * S + 16;
const prev = new Uint8Array(W * H * 4);
for (let i = 0; i < W * H; i++) prev.set([139, 139, 139, 255], i * 4);
tints.forEach(([, t], n) => {
  for (let y = 0; y < 16; y++) for (let x = 0; x < 16; x++) {
    const o = (y * 16 + x) * 4;
    let c = null;
    if (mask[o + 3]) c = [0, 1, 2].map((k) => Math.round((mask[o + k] * t[k]) / 255));
    else if (base[o + 3]) c = [base[o], base[o + 1], base[o + 2]];
    if (!c) continue;
    for (let dy = 0; dy < S; dy++) for (let dx = 0; dx < S; dx++) {
      const px = 8 + n * (16 * S + 8) + x * S + dx, py = 8 + y * S + dy;
      prev.set([...c, 255], (py * W + px) * 4);
    }
  }
});
fs.writeFileSync(path.join(__dirname, 'helper_bot.preview.png'), png(W, H, prev));
console.log('wrote helper_bot.preview.png (' + tints.map((t) => t[0]).join(', ') + ')');
if (process.argv.includes('--game')) {
  const out = path.resolve(__dirname, '../../src/main/resources/assets/comedymonkeysaddons/textures/item');
  fs.writeFileSync(path.join(out, 'helper_bot.png'), png(16, 16, base));
  fs.writeFileSync(path.join(out, 'helper_bot_panels.png'), png(16, 16, mask));
  console.log('wrote textures/item/helper_bot.png and helper_bot_panels.png');
}
