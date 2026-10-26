// Batteries on paper, for real, on the local test server over RCON (no player online, so only force-loaded chunks and
// the rings round them are loaded): a bank of two touching Batteries (a line on one only), 5,000,000 FE each, 128
// blocks of copper Power Line from an Electric Motor (set to the lowest stress, so it hardly uses what it's given).
//   node battery-paper-test.js setup   builds it, and lets the motor fill from the bank
//   node battery-paper-test.js away    the bank's chunks unloaded: the motor, emptied, should be filled from the bank's
//                                      record; then the bank loads, and should hold what it did less what the motor got
//   (stop the server, start it again, `setup` skipped)
//   node battery-paper-test.js away    the same after a restart (the record is saved)
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
const Y = -60, X0 = 500, X1 = 629, Z = 60;
const ALL = [X0 - 4, X1 + 10], MOTOR_END = [X1 - 37, X1 + 10];

async function num(cmd, re) {
  const [r] = await rcon([cmd]);
  const m = r.match(re);
  return m ? +m[1] : NaN;
}
const motor = () => num(`data get block ${X1} ${Y} ${Z} Energy`, /data: (\d+)/);
const charge = (x) => num(`data get block ${x} ${Y} ${Z} Charge`, /data: (\d+)/);
const bank = async () => (await charge(X0)) + (await charge(X0 + 1));
const gametime = () => num('time query gametime', /time is (\d+)/);
async function force([a, b]) {
  await rcon(['forceload remove all', `forceload add ${a} ${Z - 4} ${b} ${Z + 4}`]);
}
async function loaded(x) {
  const [r] = await rcon([`execute if loaded ${x} ${Y} ${Z}`]);
  return r.includes('passed');
}

(async () => {
  const phase = process.argv[2];
  if (phase === 'setup') {
    await force(ALL);
    await wait(2000);
    await rcon([
      `fill ${X0 - 2} ${Y} ${Z - 1} ${X1 + 2} ${Y + 2} ${Z + 1} air`,
      `setblock ${X0} ${Y} ${Z} comedymonkeysaddons:battery[facing=north]{Charge:5000000}`,
      `setblock ${X0 + 1} ${Y} ${Z} comedymonkeysaddons:battery[facing=north]{Charge:5000000}`,
      `fill ${X0 + 2} ${Y} ${Z} ${X1 - 1} ${Y} ${Z} comedymonkeysaddons:copper_power_line[down=true]`,
      `setblock ${X1} ${Y} ${Z} comedymonkeysaddons:electric_motor[facing=east]{SpeedSetting:16,StressSetting:256}`,
    ]);
    // the motor fills from the bank (737,280 FE at 81,920 a second), then hardly draws: the bank stands still
    await wait(15000);
    console.log(`built; the bank holds ${await bank()}, the motor ${await motor()}`);
  } else if (phase === 'away') {
    await force(ALL);
    await wait(3000);
    const before = await bank();
    await force(MOTOR_END);
    await wait(3000);
    const away = !(await loaded(X0)) && !(await loaded(X0 + 1));
    await rcon([`data merge block ${X1} ${Y} ${Z} {Energy:0}`]);
    const from = await gametime();
    await wait(15000);
    const got = await motor(), ticks = (await gametime()) - from;
    console.log(`bank's chunks ${away ? 'unloaded' : 'LOADED'}: the motor got ${got} FE in ${ticks} ticks from the bank on paper ` +
      `(copper carries up to ${4096 * ticks}): ${got > 0 ? 'yes' : 'NOTHING'}`);
    await force(ALL);
    await wait(3000);
    const after = await bank();
    console.log(`back: the bank held ${before}, now ${after}: down ${before - after}, the motor got ${got} ` +
      `(${Math.abs(before - after - got) < 50000 ? 'matches' : 'MISMATCH'}, within what the motor itself uses)`);
    await rcon(['forceload remove all']);
  }
})().catch((e) => { console.error(e); process.exit(1); });
