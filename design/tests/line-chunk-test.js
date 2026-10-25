// The unloaded-chunk test, on the local test server over RCON (no player online, so only force-loaded chunks and the
// two rings round them are loaded). A 128-block copper Fluid Line from a Wellspring making water (x 500, chunk 31) to a
// Create tank (x 629, chunk 39), and a 128-block copper Power Line from a Lava Generator to an Electric Motor, side by
// side. Each case force-loads only some chunks, so the others are really unloaded (at least three chunks from any
// forced one), then measures over five seconds what it can still read.
//   node chunk-test.js
const net = require('net');

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
const Y = -60, X0 = 500, X1 = 629, ZF = 0, ZP = 4;
const ALL = [X0 - 4, X1 + 10];

async function num(cmd, re) {
  const [r] = await rcon([cmd]);
  const m = r.match(re);
  if (m) return +m[1];
  return r.includes('has the following') ? 0 : NaN;
}
const tankB = () => num(`data get block ${X1} ${Y} ${ZF} TankContent`, /amount: (\d+)/);
const motor = () => num(`data get block ${X1} ${Y} ${ZP} Energy`, /data: (\d+)/);
const lava = () => num(`data get block ${X0} ${Y} ${ZP} Tank`, /amount: (\d+)/);
async function loadedAt(x) {
  const [r] = await rcon([`execute if loaded ${x} ${Y} ${ZF}`]);
  return r.includes('passed');
}

async function force(ranges) {
  await rcon([`forceload remove all`]);
  for (const [a, b] of ranges) await rcon([`forceload add ${a} ${ZF - 4} ${b} ${ZP + 4}`]);
}

async function run(label, ranges, refill = 0) {
  // reset with everything loaded: an empty tank B and a generator full of lava
  await force([ALL]);
  await wait(2000);
  await rcon([`data remove block ${X1} ${Y} ${ZF} TankContent.Fluid`, `data merge block ${X0} ${Y} ${ZP} {Tank:{Fluid:{id:"minecraft:lava",amount:8000}}}`]);
  await force(ranges);
  // a part of the line that lost its supply drained; it refills a block every 8 ticks once it's back
  await wait(6000 + refill);
  const where = [];
  for (const x of [X0, 565, X1]) where.push(`${x}:${(await loadedAt(x)) ? 'on' : 'off'}`);
  if (await loadedAt(X1)) await rcon([`data merge block ${X1} ${Y} ${ZP} {Energy:0}`, `data remove block ${X1} ${Y} ${ZF} TankContent.Fluid`]);
  const s = { b: await tankB(), m: await motor(), g: await lava() };
  await wait(5000);
  const f = { b: await tankB(), m: await motor(), g: await lava() };
  const d = (k) => (Number.isNaN(s[k]) || Number.isNaN(f[k]) ? 'unloaded' : (f[k] - s[k] >= 0 ? '+' : '') + (f[k] - s[k]));
  console.log(`${label.padEnd(24)} [${where.join(' ')}]  tank B ${d('b')} mB | motor ${d('m')} FE | generator lava ${d('g')} mB`);
}

(async () => {
  await force([ALL]);
  await wait(2000);
  await rcon([
    `fill ${X0 - 2} ${Y} ${ZF - 1} ${X1 + 2} ${Y + 3} ${ZP + 1} air`,
    `setblock ${X0} ${Y} ${ZF} comedymonkeysaddons:wellspring[source=water,upgrades=3]{UpgradeSlots:{Size:3,Items:[{Slot:0,id:"comedymonkeysaddons:water_upgrade",count:1},{Slot:1,id:"comedymonkeysaddons:water_upgrade",count:1},{Slot:2,id:"comedymonkeysaddons:water_upgrade",count:1}]}}`,
    `fill ${X0 + 1} ${Y} ${ZF} ${X1 - 1} ${Y} ${ZF} comedymonkeysaddons:copper_fluid_line`,
    `setblock ${X1} ${Y} ${ZF} create:fluid_tank`,
    `setblock ${X0} ${Y} ${ZP} comedymonkeysaddons:lava_generator[facing=north]`,
    `fill ${X0 + 1} ${Y} ${ZP} ${X1 - 1} ${Y} ${ZP} comedymonkeysaddons:copper_power_line[down=true]`,
    `setblock ${X1} ${Y} ${ZP} comedymonkeysaddons:electric_motor[facing=east]`,
  ]);
  await wait(1500);
  await rcon([`data merge block ${X0 + 1} ${Y} ${ZF} {Fitting:"intake",FittingSide:4}`]);
  await wait(3000);
  // a fresh 128-block copper line fills a block every 8 ticks before anything arrives: about 52 s
  await wait(56000);
  await run('everything loaded', [ALL]);
  await run('source unloaded', [[X1 - 37, X1 + 10]]);
  await run('middle unloaded', [[X0 - 4, X0 + 27], [X1 - 21, X1 + 10]]);
  await run('target unloaded', [[X0 - 4, X0 + 43]]);
  await run('everything loaded again', [ALL], 30000);
  await force([]);
})().catch((e) => { console.error(e); process.exit(1); });
