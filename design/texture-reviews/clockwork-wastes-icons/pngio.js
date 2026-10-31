// PNG decode (all filter types, 8-bit RGB/RGBA) and encode, plus a contact sheet
const fs = require('fs'), zlib = require('zlib');
function decode(buf) {
  let o = 8, idat = [], w, h, ct;
  while (o < buf.length) { const len = buf.readUInt32BE(o), type = buf.toString('ascii', o + 4, o + 8);
    if (type === 'IHDR') { w = buf.readUInt32BE(o + 8); h = buf.readUInt32BE(o + 12); ct = buf[o + 17]; }
    if (type === 'IDAT') idat.push(buf.slice(o + 8, o + 8 + len)); o += 12 + len; }
  const bpp = ct === 6 ? 4 : ct === 2 ? 3 : ct === 4 ? 2 : 1, raw = zlib.inflateSync(Buffer.concat(idat)), stride = w * bpp, out = new Uint8Array(w * h * 4), prev = new Uint8Array(stride), cur = new Uint8Array(stride);
  for (let y = 0; y < h; y++) {
    const f = raw[y * (stride + 1)]; const line = raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1));
    for (let i = 0; i < stride; i++) { const a = i >= bpp ? cur[i - bpp] : 0, b = prev[i], c = i >= bpp ? prev[i - bpp] : 0; let v = line[i];
      if (f === 1) v += a; else if (f === 2) v += b; else if (f === 3) v += (a + b) >> 1; else if (f === 4) { const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c); v += pa <= pb && pa <= pc ? a : pb <= pc ? b : c; }
      cur[i] = v & 255; }
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
// tiles images (scaled to cell width) into a grid
function contact(files, out, cols = 3, cellW = 420) {
  const imgs = files.map((f) => decode(fs.readFileSync(f)));
  const cellH = Math.max(...imgs.map((i) => Math.round(i.h * cellW / i.w))), rows = Math.ceil(imgs.length / cols), W = cols * cellW, H = rows * cellH;
  const o = new Uint8Array(W * H * 4).fill(255);
  imgs.forEach((im, k) => { const s = im.w / cellW, ox = (k % cols) * cellW, oy = Math.floor(k / cols) * cellH;
    for (let y = 0; y < Math.round(im.h / s); y++) for (let x = 0; x < cellW; x++) { const sx = Math.min(im.w - 1, Math.floor(x * s)), sy = Math.min(im.h - 1, Math.floor(y * s)), si = (sy * im.w + sx) * 4, di = ((oy + y) * W + ox + x) * 4; const g = (v) => Math.min(255, Math.round(255 * Math.pow(v / 255, 0.55))); o[di] = g(im.data[si]); o[di + 1] = g(im.data[si + 1]); o[di + 2] = g(im.data[si + 2]); o[di + 3] = 255; }
    for (let y = 0; y < cellH; y++) { const di = ((oy + y) * W + ox) * 4; o[di] = o[di + 1] = o[di + 2] = 90; }
    for (let x = 0; x < cellW; x++) { const di = ((oy) * W + ox + x) * 4; o[di] = o[di + 1] = o[di + 2] = 90; } });
  fs.writeFileSync(out, encode(W, H, o));
}
module.exports = { decode, encode, contact };
if (require.main === module) { const [out, cols, ...files] = process.argv.slice(2); contact(files, out, +cols); console.log(out); }
