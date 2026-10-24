// Copies Trade Bot's approved real sounds into the mod and writes their sounds.json entries and subtitles.
// Usage: node design/sound/export-game.js
// Every cue-file event becomes the sound event comedymonkeysaddons:trade_bot.<id> with all its variants, except the
// villager stand-ins (the game plays vanilla villager sounds for those).
const fs = require('fs');
const path = require('path');

const MOD = 'comedymonkeysaddons';
const here = __dirname;
const assets = path.resolve(here, '../../src/main/resources/assets', MOD);
const cues = JSON.parse(fs.readFileSync(path.join(here, 'trade-bot.cues.json'), 'utf8'));
const manifest = JSON.parse(fs.readFileSync(path.join(here, 'real/manifest.json'), 'utf8'));
const variantsOf = manifest.events || manifest;

const outDir = path.join(assets, 'sounds/trade_bot');
fs.rmSync(outDir, { recursive: true, force: true });
fs.mkdirSync(outDir, { recursive: true });

const soundsPath = path.join(assets, 'sounds.json');
const sounds = fs.existsSync(soundsPath) ? JSON.parse(fs.readFileSync(soundsPath, 'utf8')) : {};
for (const k of Object.keys(sounds)) if (k.startsWith('trade_bot.')) delete sounds[k];

const langPath = path.join(assets, 'lang/en_us.json');
const lang = JSON.parse(fs.readFileSync(langPath, 'utf8'));
for (const k of Object.keys(lang)) if (k.startsWith(`subtitles.${MOD}.trade_bot.`)) delete lang[k];

let files = 0;
for (const [id, ev] of Object.entries(cues.events)) {
  if (id.startsWith('villager_')) continue;
  const n = (variantsOf[id] && variantsOf[id].variants) || 1;
  const takes = fs.existsSync(path.join(here, 'real', id + '_1.ogg')) ? Array.from({ length: n }, (_, i) => `${id}_${i + 1}`) : [id];
  for (const t of takes) {
    const src = path.join(here, 'real', t + '.ogg');
    if (!fs.existsSync(src)) throw new Error('missing real sound ' + src);
    fs.copyFileSync(src, path.join(outDir, t + '.ogg'));
    files++;
  }
  const subtitle = `subtitles.${MOD}.trade_bot.${id}`;
  sounds['trade_bot.' + id] = { subtitle, sounds: takes.map((t) => `${MOD}:trade_bot/${t}`) };
  lang[subtitle] = ev.subtitle;
}
fs.writeFileSync(soundsPath, JSON.stringify(sounds, null, 2) + '\n');
fs.writeFileSync(langPath, JSON.stringify(lang, null, 2) + '\n');
console.log(`exported ${Object.keys(sounds).filter((k) => k.startsWith('trade_bot.')).length} sound events, ${files} files`);
