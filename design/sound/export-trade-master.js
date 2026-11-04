// The Trade Master's sound events (design/plans/trade-master.md M5): one for every Trade Bot event, on the very same
// files (spec D39, P13: "the animations and noises should be the same"), so only the subtitles differ: "Trade Master
// ticks", not "Trade Bot ticks". Writes the trade_master.* entries of sounds.json and their subtitles from the
// trade_bot.* ones; export-game.js runs it after every Trade Bot export, so the two never drift apart.
// Usage: node design/sound/export-trade-master.js
const fs = require('fs');
const path = require('path');

const MOD = 'comedymonkeysaddons';
const assets = path.resolve(__dirname, '../../src/main/resources/assets', MOD);
const soundsPath = path.join(assets, 'sounds.json');
const langPath = path.join(assets, 'lang/en_us.json');

function exportTradeMaster() {
  const sounds = JSON.parse(fs.readFileSync(soundsPath, 'utf8'));
  const lang = JSON.parse(fs.readFileSync(langPath, 'utf8'));
  for (const k of Object.keys(sounds)) if (k.startsWith('trade_master.')) delete sounds[k];
  for (const k of Object.keys(lang)) if (k.startsWith(`subtitles.${MOD}.trade_master.`)) delete lang[k];
  let events = 0;
  for (const [key, entry] of Object.entries({ ...sounds })) {
    if (!key.startsWith('trade_bot.')) continue;
    const id = key.slice('trade_bot.'.length);
    const subtitle = `subtitles.${MOD}.trade_master.${id}`;
    sounds['trade_master.' + id] = { ...entry, subtitle };
    const text = lang[`subtitles.${MOD}.trade_bot.${id}`];
    if (text === undefined) throw new Error('no Trade Bot subtitle for ' + id);
    lang[subtitle] = text.replace(/Trade Bot/g, 'Trade Master');
    events++;
  }
  fs.writeFileSync(soundsPath, JSON.stringify(sounds, null, 2) + '\n');
  fs.writeFileSync(langPath, JSON.stringify(lang, null, 2) + '\n');
  console.log(`exported ${events} Trade Master sound events (Trade Bot's files, his own subtitles)`);
}

if (require.main === module)
  exportTradeMaster();
module.exports = { exportTradeMaster };
