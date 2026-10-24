// Trade Bot station screen background, drawn in the mod's GUI kit (brass band, dark mottled body, recessed rows),
// matching the Electric Motor screen's colours. Writes design/gui/trade_bot_station.png (256 x 256 sheet, screen in
// the top-left W x H); once approved it's copied to the mod's textures/gui.
// Usage: node trade-bot-station.gui.js [out.png]
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const W = 212, H = 182, S = 256;
// layout (shared with the preview page and, once approved, the screen code)
const L = {
  band: 14,
  status: { x: 6, y: 18, w: 138, h: 14 }, gauge: { x: 148, y: 18, w: 58, h: 14 },
  inputsLabel: 36,
  rows: [46, 65, 84, 103].map((y) => ({ x: 6, y, w: 200, h: 18 })),
  outputLabel: 126,
  output: { x: 6, y: 136, w: 200, h: 18 },
  buttons: [{ x: 6, y: 160, w: 98, h: 16 }, { x: 108, y: 160, w: 98, h: 16 }],
};

const px = Buffer.alloc(S * S * 4);
const hex = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const set = (x, y, h, a = 255) => { const o = (y * S + x) * 4; const [r, g, b] = hex(h); px[o] = r; px[o + 1] = g; px[o + 2] = b; px[o + 3] = a; };
let seed = 7;
const rnd = () => ((seed = (Math.imul(seed, 1103515245) + 12345) >>> 0) / 4294967296);
const pick = (list) => list[Math.floor(rnd() * list.length)];

const OUTLINE = '#161718';
const BAND_TOP = '#ccaa60', BAND = ['#af8b43', '#b08c44', '#b18d45', '#b28e46', '#b38f47', '#b49048', '#b59149'];
const BODY = ['#363029', '#37312a', '#38322b', '#39332c', '#3a342d', '#3b352e', '#3c362f', '#3d3730'];
const BODY_LIGHT = '#423c35', BODY_DARK_R = '#302a23', BODY_DARK_B = '#2e2821';
const RECESS_EDGE = '#141516', RECESS_TOP = '#222424', RECESS = '#282a2a', RECESS_HI = '#404242';
const BUTTON = ['#4a4640', '#4c4842', '#4e4a44'], BUTTON_TOP = '#615c55', BUTTON_EDGE = '#2a2622';

// frame and body
for (let y = 0; y < H; y++)
  for (let x = 0; x < W; x++) {
    const edge = x === 0 || y === 0 || x === W - 1 || y === H - 1;
    const corner = (x === 0 || x === W - 1) && (y === 0 || y === H - 1);
    if (corner) continue;
    if (edge) set(x, y, OUTLINE);
    else if (y < L.band) set(x, y, y === 1 ? BAND_TOP : pick(BAND));
    else if (y === L.band) set(x, y, '#5c4c2c');
    else if (x === 1) set(x, y, BODY_LIGHT);
    else if (x === W - 2) set(x, y, BODY_DARK_R);
    else if (y === H - 2) set(x, y, BODY_DARK_B);
    else set(x, y, pick(BODY));
  }

function recess({ x, y, w, h }) {
  for (let j = 0; j < h; j++)
    for (let i = 0; i < w; i++) {
      const X = x + i, Y = y + j;
      if (i === 0 || j === 0) set(X, Y, RECESS_EDGE);
      else if (i === w - 1 || j === h - 1) set(X, Y, RECESS_HI);
      else if (j === 1) set(X, Y, RECESS_TOP);
      else set(X, Y, RECESS);
    }
}
function button({ x, y, w, h }) {
  for (let j = 0; j < h; j++)
    for (let i = 0; i < w; i++) {
      const X = x + i, Y = y + j;
      if (i === w - 1 || j === h - 1) set(X, Y, BUTTON_EDGE);
      else if (i === 0 || j === 0) set(X, Y, BUTTON_TOP);
      else set(X, Y, pick(BUTTON));
    }
}
recess(L.status);
recess(L.gauge);
L.rows.forEach(recess);
recess(L.output);
L.buttons.forEach(button);

// the sprites the screen draws on top, below the screen on the sheet (y = H + 4): the charge gauge fill (56 x 12,
// segmented muted amber), and the unlink cross (8 x 8, normal and hovered)
const SY = H + 4;
const GAUGE_FILL = ['#b88437', '#bc883a', '#b5813a'], GAUGE_TOP = '#d0a15a', GAUGE_BOTTOM = '#8a6229', GAUGE_GAP = '#4a3a22';
for (let j = 0; j < 12; j++)
  for (let i = 0; i < 56; i++) {
    const gap = i % 7 === 6; // eight segments
    set(i, SY + j, gap ? GAUGE_GAP : j === 0 ? GAUGE_TOP : j === 11 ? GAUGE_BOTTOM : pick(GAUGE_FILL));
  }
const cross = (ox, oy, c) => { for (let k = 1; k < 7; k++) { set(ox + k, oy + k, c); set(ox + 7 - k, oy + k, c); } };
cross(60, SY, '#8a8478');
cross(70, SY, '#e0705a');

const out = process.argv[2] || path.join(__dirname, 'trade_bot_station.png');
const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
const crc = (buf) => { let c = 0xffffffff; for (const b of buf) c = crcT[(c ^ b) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
const chunk = (type, body) => { const len = Buffer.alloc(4); len.writeUInt32BE(body.length); const tb = Buffer.concat([Buffer.from(type), body]); const cr = Buffer.alloc(4); cr.writeUInt32BE(crc(tb)); return Buffer.concat([len, tb, cr]); };
const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(S, 0); ihdr.writeUInt32BE(S, 4); ihdr[8] = 8; ihdr[9] = 6;
const raw = Buffer.alloc((S * 4 + 1) * S);
for (let y = 0; y < S; y++) px.copy(raw, y * (S * 4 + 1) + 1, y * S * 4, (y + 1) * S * 4);
fs.writeFileSync(out, Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]));
fs.writeFileSync(path.join(path.dirname(out), 'trade_bot_station.layout.json'), JSON.stringify({ W, H, ...L }, null, 1));
console.log('wrote', out);
