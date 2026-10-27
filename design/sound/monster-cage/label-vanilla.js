// Relabels the soundboard cards of `vanilla` events, so they say which Minecraft sound plays instead of listing the
// unused sketch layers. Usage: node label-vanilla.js <cues.json> <board.html>
const fs = require('fs');
const [cuesPath, boardPath] = process.argv.slice(2);
const cues = JSON.parse(fs.readFileSync(cuesPath, 'utf8'));
let html = fs.readFileSync(boardPath, 'utf8');
const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
for (const [id, ev] of Object.entries(cues.events)) {
  if (!ev.vanilla) continue;
  const re = new RegExp(`(<b>${id}</b><small>[^<]*</small><i>)[^<·]*`);
  html = html.replace(re, `$1Minecraft's own: ${esc(ev.vanilla.event)} (referenced, never shipped) `);
}
// real events: list the recordings (the board's own summary skips `pick` lists)
for (const [id, ev] of Object.entries(cues.events)) {
  if (!ev.real) continue;
  const names = [...new Set(ev.real.layers.flatMap((l) => l.pick || (l.src ? [l.src] : [l.type])))];
  const re = new RegExp(`(<b>${id}</b><small>[^<]*</small><i>)real:[^<·]*`);
  html = html.replace(re, `$1real: ${esc(names.join(' · '))} `);
}
fs.writeFileSync(boardPath, html);
