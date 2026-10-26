// Geothermal Turbine settings screen background, drawn in the mod's GUI kit: an andesite band (the turbine's trim) over
// a dark steel body (its blue-grey panels), laid out like the Electric Motor's screen: one scroll field for the power
// level, a recessed read-out of what that makes and burns, and Create's confirm button bottom right. Writes
// design/gui/geothermal_turbine.png (256 x 256 sheet, screen in the top-left W x H); once approved it's copied to the
// mod's textures/gui.
// Usage: node geothermal-turbine.gui.js [out.png]
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const W = 224, H = 116, S = 256;
// layout (shared with the preview page and, once approved, the screen code)
const L = {
  band: 14,
  powerLabel: { x: 12, y: 32 },
  power: { x: 84, y: 28, w: 129, h: 16 },
  readout: { x: 6, y: 54, w: 184, h: 44 },
  confirm: { x: W - 30, y: H - 30 },
};

const px = Buffer.alloc(S * S * 4);
const hex = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
const set = (x, y, h, a = 255) => { const o = (y * S + x) * 4; const [r, g, b] = hex(h); px[o] = r; px[o + 1] = g; px[o + 2] = b; px[o + 3] = a; };
let seed = 11;
const rnd = () => ((seed = (Math.imul(seed, 1103515245) + 12345) >>> 0) / 4294967296);
const pick = (list) => list[Math.floor(rnd() * list.length)];

const OUTLINE = '#161718';
// andesite, as the Auto Harvester's band
const BAND_TOP = '#b9bab4', BAND = ['#9e9f99', '#a0a19b', '#a2a39d', '#a4a59f', '#a6a7a1'], BAND_EDGE = '#5f605c';
// the turbine's dark blue-grey steel
const BODY = ['#2b2f35', '#2c3036', '#2d3137', '#2e3238', '#2f3339', '#30343a'];
const BODY_LIGHT = '#393e45', BODY_DARK_R = '#25292e', BODY_DARK_B = '#23272c';
const RECESS_EDGE = '#141516', RECESS_TOP = '#1f2124', RECESS = '#25282b', RECESS_HI = '#3d4146';

for (let y = 0; y < H; y++)
  for (let x = 0; x < W; x++) {
    const edge = x === 0 || y === 0 || x === W - 1 || y === H - 1;
    const corner = (x === 0 || x === W - 1) && (y === 0 || y === H - 1);
    if (corner) continue;
    if (edge) set(x, y, OUTLINE);
    else if (y < L.band) set(x, y, y === 1 ? BAND_TOP : pick(BAND));
    else if (y === L.band) set(x, y, BAND_EDGE);
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
recess(L.power);
recess(L.readout);
// the field's little scroll arrow, as on the motor's fields
for (let j = 0; j < 5; j++)
  for (let i = 0; i <= Math.min(j, 4 - j); i++)
    set(L.power.x + 3 + i, L.power.y + 5 + j, '#6b6e6a');

const out = process.argv[2] || path.join(__dirname, 'geothermal_turbine.png');
const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
const crc = (buf) => { let c = 0xffffffff; for (const b of buf) c = crcT[(c ^ b) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
const chunk = (type, body) => { const len = Buffer.alloc(4); len.writeUInt32BE(body.length); const tb = Buffer.concat([Buffer.from(type), body]); const cr = Buffer.alloc(4); cr.writeUInt32BE(crc(tb)); return Buffer.concat([len, tb, cr]); };
const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(S, 0); ihdr.writeUInt32BE(S, 4); ihdr[8] = 8; ihdr[9] = 6;
const raw = Buffer.alloc((S * 4 + 1) * S);
for (let y = 0; y < S; y++) px.copy(raw, y * (S * 4 + 1) + 1, y * S * 4, (y + 1) * S * 4);
fs.writeFileSync(out, Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]));
fs.writeFileSync(path.join(path.dirname(out), 'geothermal_turbine.layout.json'), JSON.stringify({ W, H, ...L }, null, 1));
console.log('wrote', out);
