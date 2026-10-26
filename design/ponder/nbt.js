// Minimal NBT (gzipped, big-endian) reader and writer for Ponder structure files.
// Tags are plain JS values with types: { t: 'byte'|'short'|'int'|'long'|'float'|'double'|'string'|'list'|'compound'|'intArray', v }.
const zlib = require('zlib');

const IDS = { end: 0, byte: 1, short: 2, int: 3, long: 4, float: 5, double: 6, byteArray: 7, string: 8, list: 9, compound: 10, intArray: 11, longArray: 12 };
const NAMES = Object.fromEntries(Object.entries(IDS).map(([k, v]) => [v, k]));

function read(buf) {
  const b = zlib.gunzipSync(buf);
  let o = 0;
  const str = () => { const n = b.readUInt16BE(o); o += 2; const s = b.toString('utf8', o, o + n); o += n; return s; };
  const payload = (id) => {
    switch (NAMES[id]) {
      case 'byte': return { t: 'byte', v: b.readInt8(o++) };
      case 'short': { const v = b.readInt16BE(o); o += 2; return { t: 'short', v }; }
      case 'int': { const v = b.readInt32BE(o); o += 4; return { t: 'int', v }; }
      case 'long': { const v = b.readBigInt64BE(o); o += 8; return { t: 'long', v }; }
      case 'float': { const v = b.readFloatBE(o); o += 4; return { t: 'float', v }; }
      case 'double': { const v = b.readDoubleBE(o); o += 8; return { t: 'double', v }; }
      case 'string': return { t: 'string', v: str() };
      case 'byteArray': { const n = b.readInt32BE(o); o += 4; const v = [...b.subarray(o, o + n)]; o += n; return { t: 'byteArray', v }; }
      case 'intArray': { const n = b.readInt32BE(o); o += 4; const v = []; for (let i = 0; i < n; i++) { v.push(b.readInt32BE(o)); o += 4; } return { t: 'intArray', v }; }
      case 'list': { const et = b[o++]; const n = b.readInt32BE(o); o += 4; const v = []; for (let i = 0; i < n; i++) v.push(payload(et)); return { t: 'list', of: NAMES[et], v }; }
      case 'compound': { const v = {}; for (;;) { const tid = b[o++]; if (tid === 0) break; const name = str(); v[name] = payload(tid); } return { t: 'compound', v }; }
      default: throw new Error('tag ' + id);
    }
  };
  const root = b[o++];
  str();
  return payload(root);
}

function write(root) {
  const parts = [];
  const u8 = (n) => parts.push(Buffer.from([n & 255]));
  const str = (s) => { const d = Buffer.from(s, 'utf8'); const l = Buffer.alloc(2); l.writeUInt16BE(d.length); parts.push(l, d); };
  const payload = (tag) => {
    const b = Buffer.alloc(8);
    switch (tag.t) {
      case 'byte': b.writeInt8(tag.v); parts.push(b.subarray(0, 1)); break;
      case 'short': b.writeInt16BE(tag.v); parts.push(b.subarray(0, 2)); break;
      case 'int': b.writeInt32BE(tag.v); parts.push(b.subarray(0, 4)); break;
      case 'long': b.writeBigInt64BE(BigInt(tag.v)); parts.push(b); break;
      case 'float': b.writeFloatBE(tag.v); parts.push(b.subarray(0, 4)); break;
      case 'double': b.writeDoubleBE(tag.v); parts.push(b); break;
      case 'string': str(tag.v); break;
      case 'byteArray': { const l = Buffer.alloc(4); l.writeInt32BE(tag.v.length); parts.push(l, Buffer.from(tag.v.map((x) => x & 255))); break; }
      case 'intArray': { const l = Buffer.alloc(4); l.writeInt32BE(tag.v.length); parts.push(l); for (const x of tag.v) { const e = Buffer.alloc(4); e.writeInt32BE(x); parts.push(e); } break; }
      case 'list': { u8(tag.v.length ? IDS[tag.v[0].t] : IDS[tag.of || 'end']); const l = Buffer.alloc(4); l.writeInt32BE(tag.v.length); parts.push(l); tag.v.forEach(payload); break; }
      case 'compound': for (const [k, t] of Object.entries(tag.v)) { u8(IDS[t.t]); str(k); payload(t); } u8(0); break;
      default: throw new Error('tag ' + tag.t);
    }
  };
  u8(IDS.compound);
  str('');
  payload(root);
  return zlib.gzipSync(Buffer.concat(parts));
}

// shorthands
const T = {
  int: (v) => ({ t: 'int', v }),
  str: (v) => ({ t: 'string', v }),
  list: (v, of) => ({ t: 'list', v, of }),
  comp: (v) => ({ t: 'compound', v }),
};

module.exports = { read, write, T };
