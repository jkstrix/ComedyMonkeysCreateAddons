// Writes magma-engine.cues.json: the events with their placeholder recipes, and each clip's cues timed from
// design/models/engines.gen.js (lit from 1 s to 6.5 s; the crank turns 6 x RPM degrees a second; each piston chugs
// at the top of its stroke, the left at 180 degrees and the right at 0, every turn).
// Usage: node design/sound/magma-engine/make-cues.js   (keeps any "real" recipes already in the file)
const fs = require('fs');
const path = require('path');

const FILE = path.join(__dirname, 'magma-engine.cues.json');
const old = fs.existsSync(FILE) ? JSON.parse(fs.readFileSync(FILE, 'utf8')) : { events: {} };
const L = (type, o) => Object.assign({ type, at: 0 }, o);

const events = {
  chug: { group: 'Signature sounds', subtitle: 'Magma Engine chugs', layers: [
    L('bump', { gain: 0.2, f: 85, f2: 50, dur: 0.12 }),
    L('whoosh', { at: 0.01, gain: 0.08, f: 900, f2: 380, dur: 0.14 }),
    L('tick', { at: 0.005, gain: 0.05, f: 1500, dur: 0.03 }),
  ] },
  firebox: { group: 'Signature sounds', subtitle: 'Magma Engine burns', layers: [
    L('hum', { gain: 0.16, f: 55, dur: 1.5 }),
    L('noise', { gain: 0.05, f: 280, dur: 1.5 }),
    L('rustle', { gain: 0.05, density: 3, dur: 1.5 }),
  ] },
  blower: { group: 'Signature sounds', subtitle: 'Supercharger whines', layers: [
    L('servo', { gain: 0.09, f: 1150, f2: 1150, dur: 1.5 }),
    L('hum', { gain: 0.08, f: 190, dur: 1.5 }),
    L('whoosh', { gain: 0.03, f: 2400, f2: 2400, dur: 1.5 }),
  ] },
  lava_pop: { group: 'Signature sounds', subtitle: 'Lava bubbles', layers: [
    L('pop', { gain: 0.26, f: 170, dur: 0.12 }),
    L('bump', { at: 0.01, gain: 0.16, f: 90, f2: 60, dur: 0.14 }),
  ] },
  start: { group: 'Running', subtitle: 'Magma Engine starts', layers: [
    L('clack', { gain: 0.55, f: 800, dur: 0.08 }),
    L('whoosh', { at: 0.05, gain: 0.6, f: 130, f2: 400, dur: 0.5 }),
    L('bump', { at: 0.3, gain: 0.45, f: 80, f2: 50, dur: 0.12 }),
    L('bump', { at: 0.55, gain: 0.5, f: 85, f2: 50, dur: 0.12 }),
    L('bump', { at: 0.74, gain: 0.5, f: 85, f2: 50, dur: 0.12 }),
    L('swell', { at: 0.1, gain: 0.25, f: 60, f2: 95, dur: 0.8 }),
  ] },
  stop: { group: 'Running', subtitle: 'Magma Engine sputters out', layers: [
    L('bump', { gain: 0.5, f: 85, f2: 45, dur: 0.14 }),
    L('whoosh', { at: 0.01, gain: 0.25, f: 1100, f2: 300, dur: 0.25 }),
    L('bump', { at: 0.38, gain: 0.3, f: 75, f2: 40, dur: 0.16 }),
    L('whoosh', { at: 0.39, gain: 0.12, f: 800, f2: 250, dur: 0.25 }),
    L('thud', { at: 0.8, gain: 0.35, f: 70, dur: 0.2 }),
    L('tick', { at: 1.0, gain: 0.15, f: 1900, dur: 0.05 }),
  ] },
  cool_tick: { group: 'States', subtitle: 'Magma Engine ticks as it cools', layers: [
    L('tick', { gain: 0.2, f: 1900, dur: 0.05 }),
    L('tick', { at: 0.3, gain: 0.12, f: 1650, dur: 0.05 }),
  ] },
};
for (const [id, ev] of Object.entries(events)) {
  const was = old.events && old.events[id];
  if (was && was.real) ev.real = was.real;
  if (was && was.vanilla) ev.vanilla = was.vanilla;
}
events.lava_pop.vanilla = { event: 'minecraft:block.lava.pop', note: "Minecraft's own lava pop, referenced from sounds.json (type event), never copied into the mod. real/lava_pop.ogg is a local preview from the game assets, kept out of git." };

const ON = 1, OFF = 6.5, r3 = (v) => Math.round(v * 1000) / 1000;
function chugs(rpm) {
  const out = [], degPerS = rpm * 6;
  for (let a = 0; ; a += 180) {
    const t = ON + a / degPerS;
    if (t >= OFF) break;
    if (a > 0) out.push([r3(t), 'chug']);
  }
  return out;
}
const pieces = (from, to, id) => { const out = []; for (let t = from; t < to; t += 1.5) out.push([r3(t), id]); return out; };
const clips = {
  run: [[ON, 'start'], ...pieces(1.2, OFF, 'firebox'), ...chugs(64), [3.6, 'lava_pop'], [OFF, 'stop'], [7.4, 'cool_tick']],
  run_super: [[ON, 'start'], ...pieces(1.2, OFF, 'firebox'), ...pieces(1.3, OFF, 'blower'), ...chugs(128), [4.1, 'lava_pop'], [OFF, 'stop'], [7.4, 'cool_tick']],
  idle: [[0.2, 'cool_tick']],
};
fs.writeFileSync(FILE, JSON.stringify({ namespace: 'magma_engine', events, clips }, null, 2) + '\n');
console.log('wrote', FILE, Object.keys(events).length, 'events;', Object.entries(clips).map(([k, v]) => `${k} ${v.length} cues`).join(', '));
