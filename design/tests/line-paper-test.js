// Generators on paper, for real, on the local test server over RCON (no player online, so only force-loaded chunks
// and the rings round them are loaded): a row of two touching Lava Generators (a cable on one only) 128 blocks of copper
// Power Line from an Electric Motor (set to the lowest stress, so it hardly uses what it's given).
//   node line-paper-test.js setup   builds it, fills both generators with lava
//   node line-paper-test.js row     the generators' chunks unloaded: the motor should get both generators' output
//   (stop the server, start it again)
//   node line-paper-test.js after   still away after the restart: both again, from their records; then they load, and
//                                   each one's lava should be down by what it burns flat out over all that time
const net = require('net');
const fs = require('fs');
const path = require('path');

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
const Y = -60, X0 = 500, X1 = 629, Z = 40;
const ALL = [X0 - 4, X1 + 10], MOTOR_END = [X1 - 37, X1 + 10];
const STATE = path.join(__dirname, '.line-paper-test.json');

async function num(cmd, re) {
  const [r] = await rcon([cmd]);
  const m = r.match(re);
  return m ? +m[1] : NaN;
}
const motor = () => num(`data get block ${X1} ${Y} ${Z} Energy`, /data: (\d+)/);
const lava = (x) => num(`data get block ${x} ${Y} ${Z} Tank`, /amount: (\d+)/);
const gametime = () => num('time query gametime', /time is (\d+)/);
async function force([a, b]) {
  await rcon(['forceload remove all', `forceload add ${a} ${Z - 4} ${b} ${Z + 4}`]);
}
async function loaded(x) {
  const [r] = await rcon([`execute if loaded ${x} ${Y} ${Z}`]);
  return r.includes('passed');
}

// the motor's take over 15 seconds of game ticks, the generators' chunks unloaded: long enough that both generators' full
// stores (100,000 FE each) can't pass for the second one's output, short enough that the motor (737,280 FE) doesn't fill
async function measure(label) {
  await force(MOTOR_END);
  await wait(3000);
  const away = !(await loaded(X0)) && !(await loaded(X0 + 1));
  await rcon([`data merge block ${X1} ${Y} ${Z} {Energy:0}`]);
  const from = await gametime();
  await wait(15000);
  const got = await motor(), ticks = (await gametime()) - from;
  const one = 1024 * ticks;
  console.log(`${label.padEnd(22)} generators' chunks ${away ? 'unloaded' : 'LOADED'}; the motor got ${got} FE in ${ticks} ticks ` +
    `(one generator makes ${one}, two ${2 * one}): ${got > one * 1.8 ? 'both' : 'ONE OR NONE'}`);
}

(async () => {
  const phase = process.argv[2];
  if (phase === 'setup') {
    await force(ALL);
    await wait(2000);
    await rcon([
      `fill ${X0 - 2} ${Y} ${Z - 1} ${X1 + 2} ${Y + 2} ${Z + 1} air`,
      `setblock ${X0} ${Y} ${Z} comedymonkeysaddons:lava_generator[facing=north]`,
      `setblock ${X0 + 1} ${Y} ${Z} comedymonkeysaddons:lava_generator[facing=north]`,
      `fill ${X0 + 2} ${Y} ${Z} ${X1 - 1} ${Y} ${Z} comedymonkeysaddons:copper_power_line[down=true]`,
      `setblock ${X1} ${Y} ${Z} comedymonkeysaddons:electric_motor[facing=east]{SpeedSetting:16,StressSetting:256}`,
    ]);
    await wait(1500);
    // both full of lava, stores empty; a few seconds for the line to meet them and their records to be made
    await rcon([`data merge block ${X0} ${Y} ${Z} {Tank:{Fluid:{id:"minecraft:lava",amount:8000}},Energy:0}`,
      `data merge block ${X0 + 1} ${Y} ${Z} {Tank:{Fluid:{id:"minecraft:lava",amount:8000}},Energy:0}`]);
    const filled = await gametime();
    fs.writeFileSync(STATE, JSON.stringify({ filled }));
    await wait(5000);
    console.log('built; filled at game time', filled);
  } else if (phase === 'row') {
    await measure('row on paper');
  } else if (phase === 'after') {
    const { filled } = JSON.parse(fs.readFileSync(STATE, 'utf8'));
    await measure('after a restart');
    await force(ALL);
    await wait(1500);
    const now = await gametime();
    const a = await lava(X0), b = await lava(X0 + 1);
    // 20 mB a second flat out: a millibucket a tick, every tick since they were filled (the server's downtime doesn't count)
    const expected = 8000 - (now - filled);
    console.log(`back: ${now - filled} ticks since filled; lava ${a} and ${b} mB, flat out would leave ${expected} (each within a few ticks)`);
    await rcon(['forceload remove all']);
  }
})().catch((e) => { console.error(e); process.exit(1); });
