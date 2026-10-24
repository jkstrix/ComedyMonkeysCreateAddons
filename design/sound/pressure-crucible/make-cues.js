// Writes pressure-crucible.cues.json: the events with their placeholder recipes, and each clip's cues timed from
// design/models/pressure-crucible.gen.js (heated: an Emerald every 2.5 s; superheated: Netherite every 5 s in the
// preview; the gears turn the whole time; each press starts as the last one ends).
// Usage: node design/sound/pressure-crucible/make-cues.js   (keeps any "real" recipes already in the file)
const fs = require('fs');
const path = require('path');

const FILE = path.join(__dirname, 'pressure-crucible.cues.json');
const old = fs.existsSync(FILE) ? JSON.parse(fs.readFileSync(FILE, 'utf8')) : { events: {} };
const L = (type, o) => Object.assign({ type, at: 0 }, o);

const events = {
  whirr: { group: 'Signature sounds', subtitle: 'Pressure Crucible gears turn', layers: [
    L('servo', { gain: 0.06, f: 300, f2: 300, dur: 1.5 }),
    L('ratchet', { gain: 0.04, rate: 22, dur: 1.5 }),
    L('hum', { gain: 0.05, f: 120, dur: 1.5 }),
  ] },
  creak: { group: 'Signature sounds', subtitle: 'Pressure Crucible screw winds down', layers: [
    L('creak', { gain: 0.22, f: 140, dur: 0.7 }),
    L('ratchet', { at: 0.05, gain: 0.07, rate: 8, dur: 0.6 }),
  ] },
  crunch: { group: 'Signature sounds', subtitle: 'Pressure Crucible crunches', layers: [
    L('clack', { gain: 0.17, f: 500, dur: 0.06 }),
    L('thud', { at: 0.01, gain: 0.27, f: 70, dur: 0.25 }),
    L('rustle', { at: 0.02, gain: 0.12, density: 30, dur: 0.25 }),
  ] },
  fire: { group: 'Heat', subtitle: 'Pressure Crucible fire crackles', layers: [
    L('hum', { gain: 0.12, f: 60, dur: 1.5 }),
    L('noise', { gain: 0.05, f: 300, dur: 1.5 }),
    L('rustle', { gain: 0.06, density: 4, dur: 1.5 }),
  ] },
  fire_soul: { group: 'Heat', subtitle: 'Pressure Crucible soul fire hisses', layers: [
    L('hum', { gain: 0.1, f: 90, dur: 1.5 }),
    L('whoosh', { gain: 0.05, f: 1800, f2: 1800, dur: 1.5 }),
    L('rustle', { gain: 0.04, density: 3, dur: 1.5 }),
  ] },
};
for (const [id, ev] of Object.entries(events)) {
  const was = old.events && old.events[id];
  if (was && was.real) ev.real = was.real;
}

const r3 = (v) => Math.round(v * 1000) / 1000;
// the running sounds repeat at the length of their seamless loops (real/<id>_loop.ogg, see make-loops.js)
const LOOP = { whirr: 4.2, fire: 7.4, fire_soul: 7.4 };
const pieces = (to, id, from = 0) => { const out = []; for (let t = from; t < to; t += LOOP[id]) out.push([r3(t), id]); return out; };
// a press: the screw creaks as the platen starts down, and the item crunches when it's done
function presses(every, length) {
  const out = [];
  for (let t = 0; t < length; t += every) {
    out.push([r3(t + 0.05), 'creak']);
    if (t + every < length) out.push([r3(t + every), 'crunch']);
  }
  return out;
}
const clips = {
  heated: [...pieces(7.5, 'whirr'), ...pieces(7.5, 'fire', 0.1), ...presses(2.5, 7.5)],
  superheated: [...pieces(10, 'whirr'), ...pieces(10, 'fire_soul', 0.1), ...presses(5, 10)],
  smouldering: [[0, 'fire']],
  cold: [],
};
fs.writeFileSync(FILE, JSON.stringify({ namespace: 'pressure_crucible', events, clips }, null, 2) + '\n');
console.log('wrote', FILE, Object.keys(events).length, 'events;', Object.entries(clips).map(([k, v]) => `${k} ${v.length} cues`).join(', '));
