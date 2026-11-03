// Writes the Clockwork Jetpack's approved sounds into the mod: node design/sound/clockwork-jetpack/export-game.js
// Every cue-file event becomes comedymonkeysaddons:clockwork_jetpack.<id> with its subtitle, its takes copied to
// sounds/clockwork_jetpack/, and its range (attenuation_distance). Flight's wind is vanilla's elytra sound, played by
// the game itself, so nothing here is Minecraft's own audio.
const fs = require('fs');
const path = require('path');

const MOD = 'comedymonkeysaddons', NS = 'clockwork_jetpack';
const here = __dirname;
const assets = path.resolve(here, '../../../src/main/resources/assets', MOD);
const cues = JSON.parse(fs.readFileSync(path.join(here, 'clockwork-jetpack.cues.json'), 'utf8'));

// ranges in blocks, from the loudness section of design/sound/clockwork-jetpack.md (16 when not listed): the station's
// winding and the small controls carry 8
const RANGE = {
  8: ['station_winding', 'hover_on', 'hover_off', 'wind_low', 'equip', 'station_hang', 'station_take', 'station_boom', 'station_release'],
};
const range = (id) => +(Object.entries(RANGE).find(([, ids]) => ids.includes(id)) || [16])[0];

const outDir = path.join(assets, 'sounds', NS);
fs.rmSync(outDir, { recursive: true, force: true });
fs.mkdirSync(outDir, { recursive: true });
const soundsPath = path.join(assets, 'sounds.json');
const sounds = JSON.parse(fs.readFileSync(soundsPath, 'utf8'));
for (const k of Object.keys(sounds)) if (k.startsWith(NS + '.')) delete sounds[k];
const langPath = path.join(assets, 'lang/en_us.json');
const lang = JSON.parse(fs.readFileSync(langPath, 'utf8'));
for (const k of Object.keys(lang)) if (k.startsWith(`subtitles.${MOD}.${NS}.`)) delete lang[k];

const takes = (id) => {
  const dir = path.join(here, 'real');
  const numbered = fs.readdirSync(dir).filter((f) => new RegExp('^' + id + '_\\d+\\.ogg$').test(f)).map((f) => f.slice(0, -4)).sort((a, b) => +a.split('_').pop() - +b.split('_').pop());
  return numbered.length ? numbered : fs.existsSync(path.join(dir, id + '.ogg')) ? [id] : [];
};
let copied = 0;
for (const [id, ev] of Object.entries(cues.events)) {
  const t = takes(id);
  if (!t.length) throw new Error('no real sound for ' + id);
  for (const n of t) { fs.copyFileSync(path.join(here, 'real', n + '.ogg'), path.join(outDir, n + '.ogg')); copied++; }
  const subtitle = `subtitles.${MOD}.${NS}.${id}`;
  const r = range(id);
  sounds[NS + '.' + id] = { subtitle, sounds: t.map((n) => (r === 16 ? `${MOD}:${NS}/${n}` : { name: `${MOD}:${NS}/${n}`, attenuation_distance: r })) };
  lang[subtitle] = ev.subtitle;
}
fs.writeFileSync(soundsPath, JSON.stringify(sounds, null, 2) + '\n');
fs.writeFileSync(langPath, JSON.stringify(lang, null, 2) + '\n');
console.log(`${NS}: ${Object.keys(cues.events).length} sound events, ${copied} files copied`);
