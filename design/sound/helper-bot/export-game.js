// Writes Helper Bot's sounds into the mod. Usage: node design/sound/helper-bot/export-game.js
// Every cue-file event becomes comedymonkeysaddons:helper_bot.<id> with a Helper Bot subtitle:
//  - Trade Bot's sounds he reuses point at Trade Bot's files already in the mod (nothing copied);
//  - the lever is Minecraft's own copper trapdoor, pitched up a little (referenced, never shipped);
//  - only his whistle's files are copied in, to sounds/helper_bot/.
const fs = require('fs');
const path = require('path');

const MOD = 'comedymonkeysaddons';
const here = __dirname;
const assets = path.resolve(here, '../../../src/main/resources/assets', MOD);
const cues = JSON.parse(fs.readFileSync(path.join(here, 'helper-bot.cues.json'), 'utf8'));
const VANILLA = { lever_clunk: { files: ['toggle1', 'toggle2', 'toggle3', 'toggle4'].map((f) => 'minecraft:block/copper_trapdoor/' + f), pitch: 1.1 } };

const outDir = path.join(assets, 'sounds/helper_bot');
fs.rmSync(outDir, { recursive: true, force: true });
fs.mkdirSync(outDir, { recursive: true });

const soundsPath = path.join(assets, 'sounds.json');
const sounds = JSON.parse(fs.readFileSync(soundsPath, 'utf8'));
for (const k of Object.keys(sounds)) if (k.startsWith('helper_bot.')) delete sounds[k];
const langPath = path.join(assets, 'lang/en_us.json');
const lang = JSON.parse(fs.readFileSync(langPath, 'utf8'));
for (const k of Object.keys(lang)) if (k.startsWith(`subtitles.${MOD}.helper_bot.`)) delete lang[k];

// the takes of an event in a folder: <id>_1.ogg … <id>_N.ogg, or just <id>.ogg
const takes = (dir, id) => {
  const numbered = fs.readdirSync(dir).filter((f) => new RegExp('^' + id + '_\\d+\\.ogg$').test(f)).map((f) => f.slice(0, -4)).sort((a, b) => +a.split('_').pop() - +b.split('_').pop());
  return numbered.length ? numbered : fs.existsSync(path.join(dir, id + '.ogg')) ? [id] : [];
};
let copied = 0;
for (const [id, ev] of Object.entries(cues.events)) {
  let list;
  if (VANILLA[id]) list = VANILLA[id].files.map((name) => ({ name, pitch: VANILLA[id].pitch }));
  else if (ev.reuse === 'trade_bot') {
    const t = takes(path.join(assets, 'sounds/trade_bot'), id);
    if (!t.length) throw new Error('Trade Bot has no sound ' + id + ' in the mod');
    list = t.map((n) => `${MOD}:trade_bot/${n}`);
  } else {
    const t = takes(path.join(here, 'real'), id);
    if (!t.length) throw new Error('no real sound for ' + id);
    for (const n of t) { fs.copyFileSync(path.join(here, 'real', n + '.ogg'), path.join(outDir, n + '.ogg')); copied++; }
    list = t.map((n) => `${MOD}:helper_bot/${n}`);
  }
  const subtitle = `subtitles.${MOD}.helper_bot.${id}`;
  sounds['helper_bot.' + id] = { subtitle, sounds: list };
  lang[subtitle] = ev.subtitle;
}
fs.writeFileSync(soundsPath, JSON.stringify(sounds, null, 2) + '\n');
fs.writeFileSync(langPath, JSON.stringify(lang, null, 2) + '\n');
console.log(`helper_bot: ${Object.keys(cues.events).length} sound events, ${copied} files copied`);
