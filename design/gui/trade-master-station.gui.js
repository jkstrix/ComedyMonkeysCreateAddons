// Trade Master station screen background (design/specs/trade-master.md P2, P3), in the mod's GUI kit and the Trade Bot
// station screen's width and parts (trade-bot-station.gui.js): his status and charge, the network he's on, the gauge
// he's working on, a scrolling list of every other gauge on it (user, 2026-11-04: "the list could get long", and "I liked
// the working on row"), and Call home. The band is his netherite
// (user's pick over Trade Bot's brass). Writes design/gui/trade_master_station.png (256 x 256 sheet, screen in the
// top-left W x H); once approved it's copied to the mod's textures/gui.
// Usage: node trade-master-station.gui.js
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const W = 212, H = 230, S = 256;
// layout (shared with the preview page and, once approved, the screen code)
const L = {
  band: 14,
  status: { x: 6, y: 18, w: 138, h: 14 }, gauge: { x: 148, y: 18, w: 58, h: 14 },
  networkLabel: 36, network: { x: 6, y: 46, w: 200, h: 18 },
  workingLabel: 68, working: { x: 6, y: 78, w: 200, h: 18 },
  // the other gauges: five rows of 18 inside a recess, and a scroll track down its right edge
  gaugesLabel: 100, list: { x: 6, y: 110, w: 200, h: 92 }, rowH: 18, rows: 5, track: { x: 200, y: 111, w: 5, h: 90 },
  button: { x: 6, y: 208, w: 200, h: 16 },
};

const BANDS = {
  // the Trade Bot station's brass
  brass: { top: '#ccaa60', fill: ['#af8b43', '#b08c44', '#b18d45', '#b28e46', '#b38f47', '#b49048', '#b59149'], seam: '#5c4c2c' },
  // his netherite: vanilla's armour plum (design/visual-language.md, netherite worn and plating), lit along the top
  netherite: { top: '#5d565d', fill: ['#49393f', '#4b3b41', '#4d3d43', '#4f3f45', '#51444e', '#4e4049'], seam: '#241f20' },
};
const OUTLINE = '#161718';
const BODY = ['#363029', '#37312a', '#38322b', '#39332c', '#3a342d', '#3b352e', '#3c362f', '#3d3730'];
const BODY_LIGHT = '#423c35', BODY_DARK_R = '#302a23', BODY_DARK_B = '#2e2821';
const RECESS_EDGE = '#141516', RECESS_TOP = '#222424', RECESS = '#282a2a', RECESS_HI = '#404242', RECESS_ALT = '#252727';
const TRACK = '#1c1d1e', TRACK_EDGE = '#141516';
const BUTTON = ['#4a4640', '#4c4842', '#4e4a44'], BUTTON_TOP = '#615c55', BUTTON_EDGE = '#2a2622';
const GAUGE_FILL = ['#b88437', '#bc883a', '#b5813a'], GAUGE_TOP = '#d0a15a', GAUGE_BOTTOM = '#8a6229', GAUGE_GAP = '#4a3a22';

function draw(band) {
  const px = Buffer.alloc(S * S * 4);
  const hex = (h) => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];
  const set = (x, y, h, a = 255) => { const o = (y * S + x) * 4; const [r, g, b] = hex(h); px[o] = r; px[o + 1] = g; px[o + 2] = b; px[o + 3] = a; };
  let seed = 7;
  const rnd = () => ((seed = (Math.imul(seed, 1103515245) + 12345) >>> 0) / 4294967296);
  const pick = (list) => list[Math.floor(rnd() * list.length)];

  // frame and body
  for (let y = 0; y < H; y++)
    for (let x = 0; x < W; x++) {
      const edge = x === 0 || y === 0 || x === W - 1 || y === H - 1;
      const corner = (x === 0 || x === W - 1) && (y === 0 || y === H - 1);
      if (corner) continue;
      if (edge) set(x, y, OUTLINE);
      else if (y < L.band) set(x, y, y === 1 ? band.top : pick(band.fill));
      else if (y === L.band) set(x, y, band.seam);
      else if (x === 1) set(x, y, BODY_LIGHT);
      else if (x === W - 2) set(x, y, BODY_DARK_R);
      else if (y === H - 2) set(x, y, BODY_DARK_B);
      else set(x, y, pick(BODY));
    }
  const recess = ({ x, y, w, h }) => {
    for (let j = 0; j < h; j++)
      for (let i = 0; i < w; i++) {
        const X = x + i, Y = y + j;
        if (i === 0 || j === 0) set(X, Y, RECESS_EDGE);
        else if (i === w - 1 || j === h - 1) set(X, Y, RECESS_HI);
        else if (j === 1) set(X, Y, RECESS_TOP);
        else set(X, Y, RECESS);
      }
  };
  const button = ({ x, y, w, h }) => {
    for (let j = 0; j < h; j++)
      for (let i = 0; i < w; i++) {
        const X = x + i, Y = y + j;
        if (i === w - 1 || j === h - 1) set(X, Y, BUTTON_EDGE);
        else if (i === 0 || j === 0) set(X, Y, BUTTON_TOP);
        else set(X, Y, pick(BUTTON));
      }
  };
  recess(L.status);
  recess(L.gauge);
  recess(L.network);
  recess(L.working);
  recess(L.list);
  // every other row a shade darker, so a long list reads
  for (let r = 1; r < L.rows; r += 2)
    for (let j = 0; j < L.rowH; j++)
      for (let i = 1; i < L.track.x - L.list.x; i++)
        set(L.list.x + i, L.list.y + 1 + r * L.rowH + j, RECESS_ALT);
  for (let j = 0; j < L.track.h; j++)
    for (let i = 0; i < L.track.w; i++)
      set(L.track.x + i, L.track.y + j, i === 0 ? TRACK_EDGE : TRACK);
  button(L.button);

  // the charge gauge fill the screen draws on top, below the screen on the sheet (y = H + 4), as Trade Bot's
  const SY = H + 4;
  for (let j = 0; j < 12; j++)
    for (let i = 0; i < 56; i++) {
      const gap = i % 7 === 6;
      set(i, SY + j, gap ? GAUGE_GAP : j === 0 ? GAUGE_TOP : j === 11 ? GAUGE_BOTTOM : pick(GAUGE_FILL));
    }
  return px;
}

function png(px, out) {
  const crcT = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0; });
  const crc = (buf) => { let c = 0xffffffff; for (const b of buf) c = crcT[(c ^ b) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; };
  const chunk = (type, body) => { const len = Buffer.alloc(4); len.writeUInt32BE(body.length); const tb = Buffer.concat([Buffer.from(type), body]); const cr = Buffer.alloc(4); cr.writeUInt32BE(crc(tb)); return Buffer.concat([len, tb, cr]); };
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(S, 0); ihdr.writeUInt32BE(S, 4); ihdr[8] = 8; ihdr[9] = 6;
  const raw = Buffer.alloc((S * 4 + 1) * S);
  for (let y = 0; y < S; y++) px.copy(raw, y * (S * 4 + 1) + 1, y * S * 4, (y + 1) * S * 4);
  fs.writeFileSync(out, Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]));
  console.log('wrote', out);
}

png(draw(BANDS.netherite), path.join(__dirname, 'trade_master_station.png'));
fs.writeFileSync(path.join(__dirname, 'trade_master_station.layout.json'), JSON.stringify({ W, H, ...L }, null, 1));
