// Pointed Dripstone Generator screen background, in the mod's GUI kit: the Animal Pens' andesite screen (the Fish Tank
// uses the same art) with the slots it doesn't need taken out. The bottle slot stays as the seed slot, the two gauges
// stay (growth and water), the food row and the 3 x 3 output grid go, and one output slot sits where the grid's middle
// was. The erased areas are filled with the body's own pixels from its empty right-hand side, so the mottle matches.
// Writes design/gui/pointed_dripstone_generator.png (256 x 256 sheet, as the pen's); once approved it's copied to the
// mod's textures/gui. Layout constants: PointedDripstoneGeneratorMenu (seed 8,18; output 80,36) and its screen.
// Usage: node pointed-dripstone-generator.gui.js [out.png]
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const SRC = path.resolve(__dirname, '../../src/main/resources/assets/comedymonkeysaddons/textures/gui/animal_pen.png');

function decode(buf) {
  let p = 8, w, h, ct, depth; const idat = [];
  while (p < buf.length) {
    const len = buf.readUInt32BE(p), type = buf.toString('ascii', p + 4, p + 8), body = buf.slice(p + 8, p + 8 + len);
    if (type === 'IHDR') { w = body.readUInt32BE(0); h = body.readUInt32BE(4); depth = body[8]; ct = body[9]; }
    if (type === 'IDAT') idat.push(body);
    p += 12 + len;
  }
  if (depth !== 8 || (ct !== 6 && ct !== 2)) throw new Error(`unsupported PNG: colour type ${ct}, depth ${depth}`);
  const bpp = ct === 6 ? 4 : 3, stride = w * bpp, raw = zlib.inflateSync(Buffer.concat(idat)), out = Buffer.alloc(stride * h);
  for (let y = 0; y < h; y++) {
    const f = raw[y * (stride + 1)];
    for (let x = 0; x < stride; x++) {
      const v = raw[y * (stride + 1) + 1 + x], a = x >= bpp ? out[y * stride + x - bpp] : 0, b = y ? out[(y - 1) * stride + x] : 0, c = x >= bpp && y ? out[(y - 1) * stride + x - bpp] : 0;
      const pa = Math.abs(b - c), pb = Math.abs(a - c), pc = Math.abs(a + b - 2 * c);
      out[y * stride + x] = (v + [0, a, b, (a + b) >> 1, pa <= pb && pa <= pc ? a : pb <= pc ? b : c][f]) & 255;
    }
  }
  const rgba = Buffer.alloc(w * h * 4);
  for (let i = 0; i < w * h; i++) {
    rgba[i * 4] = out[i * bpp]; rgba[i * 4 + 1] = out[i * bpp + 1]; rgba[i * 4 + 2] = out[i * bpp + 2]; rgba[i * 4 + 3] = bpp === 4 ? out[i * bpp + 3] : 255;
  }
  return { w, h, px: rgba };
}
function encode(w, h, px) {
  const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
  const crc = (buf) => { let c = 0xffffffff; for (const b of buf) c = crcT[(c ^ b) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
  const chunk = (type, body) => { const len = Buffer.alloc(4); len.writeUInt32BE(body.length); const tb = Buffer.concat([Buffer.from(type), body]); const cr = Buffer.alloc(4); cr.writeUInt32BE(crc(tb)); return Buffer.concat([len, tb, cr]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) px.copy(raw, y * (w * 4 + 1) + 1, y * w * 4, (y + 1) * w * 4);
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}

const { w, h, px } = decode(fs.readFileSync(SRC));
const src = Buffer.from(px);
const copy = (sx, sy, dx, dy, cw, ch) => {
  for (let j = 0; j < ch; j++) for (let i = 0; i < cw; i++) {
    const s = ((sy + j) * w + sx + i) * 4, d = ((dy + j) * w + dx + i) * 4;
    src.copy(px, d, s, s + 4);
  }
};
// the pen's slot boxes (18 x 18, the item at +1): the 3 x 3 output grid at 61,17 and the food row at 7,79
const BODY_X = 118; // its empty right-hand side, for fill
copy(BODY_X, 17, 61, 17, 54, 54); // erase the 3 x 3 grid
copy(BODY_X, 79, 7, 79, 36, 18); // erase the food row
copy(61, 17, 79, 35, 18, 18); // one slot, from the grid's first, where the grid's middle was (item at 80,36)

const out = process.argv[2] || path.join(__dirname, 'pointed_dripstone_generator.png');
fs.writeFileSync(out, encode(w, h, px));
console.log('wrote', out, w + 'x' + h);
