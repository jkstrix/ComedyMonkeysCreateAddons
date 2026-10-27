// Helper Bot's test world: built on the local test server, then installed as a singleplayer save.
//   node helper-bot-world.js build     (server running, on the world named in run-server/server.properties) builds the
//                                      courses with /helperbot testsetup, waits for the train, saves and stops it
//   node helper-bot-world.js install <world folder in run-server>   copies it to run/saves/"Helper Bot Test", creative
//                                      with cheats on
const net = require('net');
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

function rcon(cmds) {
  return new Promise((resolve, reject) => {
    const s = net.connect(25575, '127.0.0.1');
    let id = 0, buf = Buffer.alloc(0);
    const out = [];
    const send = (type, body) => { const b = Buffer.from(body, 'utf8'); const p = Buffer.alloc(14 + b.length); p.writeInt32LE(10 + b.length, 0); p.writeInt32LE(++id, 4); p.writeInt32LE(type, 8); b.copy(p, 12); s.write(p); };
    const next = () => { if (id - 1 < cmds.length) send(2, cmds[id - 1]); else { s.end(); resolve(out); } };
    s.on('connect', () => send(3, 'cmca-test'));
    s.on('data', (d) => {
      buf = Buffer.concat([buf, d]);
      while (buf.length >= 4 && buf.length >= 4 + buf.readInt32LE(0)) {
        const len = buf.readInt32LE(0), rid = buf.readInt32LE(4), body = buf.slice(12, 4 + len - 2).toString('utf8');
        buf = buf.slice(4 + len);
        if (rid > 1) out.push(body);
        next();
      }
    });
    s.on('error', reject);
  });
}
const wait = (ms) => new Promise((r) => setTimeout(r, ms));
// the courses start here: on a default flat world the ground is at y -61
const O = [0, -60, 0];

async function build() {
  const log = (r) => r.forEach((x) => x && console.log('  ' + x.replace(/\n/g, ' ')));
  log(await rcon([
    'forceload add -16 -24 80 32',
    'gamerule doDaylightCycle false', 'time set 6000', 'gamerule doWeatherCycle false', 'weather clear',
    'gamerule doMobSpawning false', 'gamerule doFireTick false',
  ]));
  await wait(3000);
  log(await rcon([`execute positioned ${O.join(' ')} run helperbot testsetup`]));
  // the stations register, get their names, and B assembles the train over the next few seconds
  for (let i = 0; i < 20; i++) {
    await wait(1000);
    const [r] = await rcon(['execute if entity @e[type=create:carriage_contraption]']);
    if (/passed/i.test(r)) { console.log('train assembled'); break; }
    if (i === 19) console.log('no train yet: assemble it at station B (open it, Assembly Mode, Assemble)');
  }
  log(await rcon([`setworldspawn ${O[0] + 34} ${O[1]} ${O[2] + 5}`, 'save-all flush']));
  await wait(3000);
  await rcon(['stop']).catch(() => {});
  console.log('saved and stopped');
}

// ---------- level.dat: a minimal NBT reader and writer ----------
function readNbt(buf) {
  let p = 0;
  const str = () => { const n = buf.readUInt16BE(p); p += 2; const s = buf.toString('utf8', p, p + n); p += n; return s; };
  function payload(t) {
    switch (t) {
      case 1: return { t, v: buf.readInt8(p++) };
      case 2: { const v = buf.readInt16BE(p); p += 2; return { t, v }; }
      case 3: { const v = buf.readInt32BE(p); p += 4; return { t, v }; }
      case 4: { const v = buf.readBigInt64BE(p); p += 8; return { t, v }; }
      case 5: { const v = buf.readFloatBE(p); p += 4; return { t, v }; }
      case 6: { const v = buf.readDoubleBE(p); p += 8; return { t, v }; }
      case 7: { const n = buf.readInt32BE(p); p += 4; const v = buf.subarray(p, p + n); p += n; return { t, v }; }
      case 8: return { t, v: str() };
      case 9: { const et = buf[p++], n = buf.readInt32BE(p); p += 4; const v = []; for (let k = 0; k < n; k++) v.push(payload(et)); return { t, et, v }; }
      case 10: { const v = []; for (;;) { const tt = buf[p++]; if (tt === 0) break; const name = str(); v.push([name, payload(tt)]); } return { t, v }; }
      case 11: { const n = buf.readInt32BE(p); p += 4; const v = []; for (let k = 0; k < n; k++) { v.push(buf.readInt32BE(p)); p += 4; } return { t, v }; }
      case 12: { const n = buf.readInt32BE(p); p += 4; const v = []; for (let k = 0; k < n; k++) { v.push(buf.readBigInt64BE(p)); p += 8; } return { t, v }; }
    }
    throw new Error('tag ' + t);
  }
  const t = buf[p++], name = str();
  return { name, root: payload(t) };
}
function writeNbt({ name, root }) {
  const parts = [];
  const u8 = (x) => parts.push(Buffer.from([x & 255]));
  const str = (s) => { const b = Buffer.from(s, 'utf8'); const l = Buffer.alloc(2); l.writeUInt16BE(b.length); parts.push(l, b); };
  const num = (fn, size, v) => { const b = Buffer.alloc(size); b[fn](v); parts.push(b); };
  function payload(n) {
    switch (n.t) {
      case 1: return u8(n.v);
      case 2: return num('writeInt16BE', 2, n.v);
      case 3: return num('writeInt32BE', 4, n.v);
      case 4: return num('writeBigInt64BE', 8, n.v);
      case 5: return num('writeFloatBE', 4, n.v);
      case 6: return num('writeDoubleBE', 8, n.v);
      case 7: num('writeInt32BE', 4, n.v.length); return parts.push(Buffer.from(n.v));
      case 8: return str(n.v);
      case 9: u8(n.et); num('writeInt32BE', 4, n.v.length); return n.v.forEach(payload);
      case 10: n.v.forEach(([k, c]) => { u8(c.t); str(k); payload(c); }); return u8(0);
      case 11: num('writeInt32BE', 4, n.v.length); return n.v.forEach((x) => num('writeInt32BE', 4, x));
      case 12: num('writeInt32BE', 4, n.v.length); return n.v.forEach((x) => num('writeBigInt64BE', 8, x));
    }
  }
  u8(root.t); str(name); payload(root);
  return Buffer.concat(parts);
}
const child = (compound, key) => compound.v.find(([k]) => k === key);
function set(compound, key, node) {
  const at = child(compound, key);
  if (at) at[1] = node; else compound.v.push([key, node]);
}

function install(worldName) {
  const repo = path.resolve(__dirname, '../..');
  const from = path.join(repo, 'run-server', worldName);
  const to = path.join(repo, 'run', 'saves', 'Helper Bot Test');
  if (!fs.existsSync(path.join(from, 'level.dat'))) throw new Error('no world at ' + from);
  fs.rmSync(to, { recursive: true, force: true });
  fs.cpSync(from, to, { recursive: true, filter: (f) => !/session\.lock$/.test(f) });
  const file = path.join(to, 'level.dat');
  const nbt = readNbt(zlib.gunzipSync(fs.readFileSync(file)));
  const data = child(nbt.root, 'Data')[1];
  set(data, 'allowCommands', { t: 1, v: 1 });
  set(data, 'GameType', { t: 3, v: 1 });
  set(data, 'LevelName', { t: 8, v: 'Helper Bot Test' });
  fs.writeFileSync(file, zlib.gzipSync(writeNbt(nbt)));
  console.log('installed ' + to);
}

const [cmd, arg] = process.argv.slice(2);
if (cmd === 'build') build().catch((e) => { console.error(e); process.exit(1); });
else if (cmd === 'install') install(arg || 'helperbot_test');
else console.log('usage: node helper-bot-world.js build | install <world folder>');
