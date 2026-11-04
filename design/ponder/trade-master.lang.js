// The Trade Master's Ponder text -> en_us.json, from client/ponder/TradeMasterScenes.java: each scene's header, then
// its texts numbered in the order they appear (comedymonkeysaddons.ponder.<scene>.text_N), as Ponder looks them up.
//   node design/ponder/trade-master.lang.js
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '../..');
const src = fs.readFileSync(path.join(ROOT, 'src/main/java/com/comedymonkey/createaddons/client/ponder/TradeMasterScenes.java'), 'utf8');
const langPath = path.join(ROOT, 'src/main/resources/assets/comedymonkeysaddons/lang/en_us.json');
const lang = JSON.parse(fs.readFileSync(langPath, 'utf8'));
for (const k of Object.keys(lang))
  if (/^comedymonkeysaddons\.ponder\.trade_master(_gauges)?\./.test(k)) delete lang[k];
for (const part of src.split('scene.title(').slice(1)) {
  const title = part.match(/^"([^"]+)", "([^"]+)"/);
  const id = title[1];
  lang[`comedymonkeysaddons.ponder.${id}.header`] = title[2];
  let n = 0;
  for (const text of part.matchAll(/\.text\("((?:[^"\\]|\\.)*)"\)/g))
    lang[`comedymonkeysaddons.ponder.${id}.text_${++n}`] = JSON.parse(`"${text[1]}"`);
  console.log(`${id}: header and ${n} texts`);
}
fs.writeFileSync(langPath, JSON.stringify(lang, null, 2) + '\n');
