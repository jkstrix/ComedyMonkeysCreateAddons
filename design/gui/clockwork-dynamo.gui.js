// Clockwork Dynamo screen background, drawn in the mod's GUI kit: the brass band of the Electric Motor's and the Trade
// Bot Station's screens over the dark body, one recessed row for the stress field (the dynamo follows its shaft, so
// there's no speed or direction), and a one-line read-out of what it makes. Writes design/gui/clockwork_dynamo.png (256 x 256
// sheet, screen in the top-left W x H); once approved it's copied to the mod's textures/gui.
// Usage: node clockwork-dynamo.gui.js
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const W = 224, S = 256;
// layout (shared with the preview page and the screen code)
const L = { band: 14, row: { x: 6, y: 22, w: 212, h: 24 }, field: { x: 84, y: 26, w: 129, h: 16 }, readout: { x: 6, y: 52, w: 182, h: 20 } };
L.H = 80;
// the confirm tick sits beside the read-out, centred on it
L.confirm = { x: W - 30, y: L.readout.y + 1 };

const hex = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const OUTLINE = '#161718';
const BAND_TOP = '#ccaa60', BAND = ['#af8b43', '#b08c44', '#b18d45', '#b28e46', '#b38f47', '#b49048', '#b59149'];
const BODY = ['#363029', '#37312a', '#38322b', '#39332c', '#3a342d', '#3b352e', '#3c362f', '#3d3730'];
const BODY_LIGHT = '#423c35', BODY_DARK_R = '#302a23', BODY_DARK_B = '#2e2821';
const RECESS_EDGE = '#141516', RECESS_TOP = '#222424', RECESS = '#282a2a', RECESS_HI = '#404242';
const ROW_EDGE = '#24201b', ROW = ['#2f2a24', '#302b25'], ROW_HI = '#47413a';

const H = L.H;
const px = Buffer.alloc(S * S * 4);
const set = (x, y, h) => { const o = (y * S + x) * 4; const [r, g, b] = hex(h); px[o] = r; px[o + 1] = g; px[o + 2] = b; px[o + 3] = 255; };
let seed = 7;
const pick = (list) => list[Math.floor(((seed = (Math.imul(seed, 1103515245) + 12345) >>> 0) / 4294967296) * list.length)];
for (let y = 0; y < H; y++)
  for (let x = 0; x < W; x++) {
    const edge = x === 0 || y === 0 || x === W - 1 || y === H - 1;
    if ((x === 0 || x === W - 1) && (y === 0 || y === H - 1)) continue;
    if (edge) set(x, y, OUTLINE);
    else if (y < L.band) set(x, y, y === 1 ? BAND_TOP : pick(BAND));
    else if (y === L.band) set(x, y, '#5c4c2c');
    else if (x === 1) set(x, y, BODY_LIGHT);
    else if (x === W - 2) set(x, y, BODY_DARK_R);
    else if (y === H - 2) set(x, y, BODY_DARK_B);
    else set(x, y, pick(BODY));
  }
const box = ({ x, y, w, h }, edge, fill, hi, top) => {
  for (let j = 0; j < h; j++)
    for (let i = 0; i < w; i++) {
      const X = x + i, Y = y + j;
      if (i === 0 || j === 0) set(X, Y, edge);
      else if (i === w - 1 || j === h - 1) set(X, Y, hi);
      else if (top && j === 1) set(X, Y, top);
      else set(X, Y, Array.isArray(fill) ? pick(fill) : fill);
    }
};
box(L.row, ROW_EDGE, ROW, ROW_HI);
box(L.field, RECESS_EDGE, RECESS, RECESS_HI, RECESS_TOP);
box(L.readout, RECESS_EDGE, RECESS, RECESS_HI, RECESS_TOP);

const out = process.argv[2] || path.join(__dirname, 'clockwork_dynamo.png');
const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
const crc = (buf) => { let c = 0xffffffff; for (const b of buf) c = crcT[(c ^ b) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
const chunk = (type, body) => { const len = Buffer.alloc(4); len.writeUInt32BE(body.length); const tb = Buffer.concat([Buffer.from(type), body]); const cr = Buffer.alloc(4); cr.writeUInt32BE(crc(tb)); return Buffer.concat([len, tb, cr]); };
const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(S, 0); ihdr.writeUInt32BE(S, 4); ihdr[8] = 8; ihdr[9] = 6;
const raw = Buffer.alloc((S * 4 + 1) * S);
for (let y = 0; y < S; y++) px.copy(raw, y * (S * 4 + 1) + 1, y * S * 4, (y + 1) * S * 4);
fs.writeFileSync(out, Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]));
console.log('wrote', out, JSON.stringify(L));
