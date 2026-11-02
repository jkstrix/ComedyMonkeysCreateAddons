// Takes the overgrowth review's screenshots in the user's Blockbench (its MCP plugin over http://localhost:3000/bb-mcp):
// opens design/models/clockwork-wastes-overgrowth.bbmodel as the tab "clockwork-wastes-overgrowth" (closing an older copy of
// that tab only; other tabs are left alone), shoots every mark from an offscreen view, and lays each shot over the Wastes'
// sky colour (#3b2e26) so the plants are judged against the biome, not white.
//   node design/texture-reviews/clockwork-wastes-overgrowth/shots.js [mark ...]      (no marks: all of them)
const fs = require('fs');
const path = require('path');
const { png } = require('../../models/process-box.lib.js');
const { decodePng } = require('../../models/kit-building-preview.js');
const URL = 'http://localhost:3000/bb-mcp';
const TAB = 'clockwork-wastes-overgrowth';
const MODEL = path.resolve(__dirname, '../../models/clockwork-wastes-overgrowth.bbmodel');
const SKY = [59, 46, 38];
// camera marks in Blockbench units (16 a block); every front faces north, so the cameras stand to the north (-z)
const MARKS = [
  // the swatch board (z -40)
  ['01-swatch-board', [480, 150, -1130], [480, 20, -630]],
  ['02-swatch-knotvine-lichen', [56, 34, -720], [56, 16, -630]],
  ['03-swatch-twinerope', [152, 52, -760], [152, 40, -630]],
  ['04-swatch-moss', [248, 34, -700], [248, 8, -635]],
  ['05-swatch-crack-plants', [360, 22, -690], [360, 6, -635]],
  ['06-swatch-bramblecoil', [456, 28, -700], [456, 8, -635]],
  ['07-swatch-shiftcaps', [536, 34, -700], [536, 18, -632]],
  ['08-swatch-breakroot', [648, 34, -700], [648, 4, -635]],
  ['08b-swatch-burst-wall', [690, 40, -690], [712, 10, -630]],
  ['09-swatch-tree-blocks', [784, 44, -730], [784, 16, -632]],
  ['10-swatch-chips', [886, 76, -790], [886, 76, -624]],
  // the wall study (z 0): LIGHT terrace right of the tower in the shot, HEAVY left (x grows to the left from the north)
  ['11-wall-study-overview', [460, 160, -760], [460, 80, 0]],
  ['12-wall-light-20-blocks', [176, 60, -330], [176, 60, 0]],
  ['13-wall-heavy-20-blocks', [720, 60, -330], [720, 60, 0]],
  ['14-wall-heavy-close', [690, 40, -110], [690, 44, 0]],
  ['15-wall-light-close', [150, 40, -110], [150, 44, 0]],
  ['16-tower-grip-rule', [470, 70, -150], [470, 110, 24]],
  // the street corner
  ['17-corner-light', [1150, 80, -340], [1120, 40, 0]],
  ['18-corner-heavy', [1500, 80, -340], [1470, 40, 0]],
  ['19-corner-heavy-close', [1430, 36, -150], [1440, 24, -40]],
  // the trees
  ['20-grapple-crookwood', [1950, 130, -440], [1930, 120, 40]],
  ['21-grapple-close', [2000, 50, -170], [1980, 30, 20]],
  ['22-shieldcrown', [2272, 130, -440], [2272, 130, 70]],
  ['23-shieldcrown-from-inside', [2232, 30, 136], [2280, 210, 72]],
  ['24-tree-silhouettes', [2740, 150, -520], [2740, 130, 32]],
  // the lit check
  ['25-lit-check', [3140, 44, -180], [3140, 30, 0]],
  ['26-lit-check-close', [3120, 20, -80], [3110, 12, 0]],
  // distance
  ['27-skyline-60-blocks', [700, 260, -1150], [700, 80, 0]],
];
let sid = null, id = 1;
async function rpc(method, params) {
  const headers = { 'content-type': 'application/json', accept: 'application/json, text/event-stream' };
  if (sid) headers['mcp-session-id'] = sid;
  const r = await fetch(URL, { method: 'POST', headers, body: JSON.stringify({ jsonrpc: '2.0', id: id++, method, params }) });
  if (r.headers.get('mcp-session-id')) sid = r.headers.get('mcp-session-id');
  const text = await r.text();
  let msg = null;
  if (text.trim().startsWith('{')) msg = JSON.parse(text);
  else for (const line of text.split('\n')) if (line.startsWith('data:')) { try { const m = JSON.parse(line.slice(5)); if (m.id !== undefined) msg = m; } catch (e) { /* not ours */ } }
  if (msg && msg.error) throw new Error(method + ': ' + JSON.stringify(msg.error));
  return msg && msg.result;
}
const call = (name, args) => rpc('tools/call', { name, arguments: args });
const evalText = async (code) => { const r = await call('risky_eval', { code }); return ((r && r.content) || []).map((c) => c.text).join(''); };
const sleep = (ms) => new Promise((res) => setTimeout(res, ms));
function overSky(buf) {
  const { w, h, rgba } = decodePng(buf), out = new Uint8Array(w * h * 4);
  for (let i = 0; i < w * h; i++) {
    const a = rgba[i * 4 + 3] / 255;
    for (let c = 0; c < 3; c++) out[i * 4 + c] = Math.round(rgba[i * 4 + c] * a + SKY[c] * (1 - a));
    out[i * 4 + 3] = 255;
  }
  return png(w, h, out);
}
(async () => {
  const want = process.argv.slice(2);
  await rpc('initialize', { protocolVersion: '2025-03-26', capabilities: {}, clientInfo: { name: 'overgrowth-shots', version: '1' } });
  await fetch(URL, { method: 'POST', headers: { 'content-type': 'application/json', accept: 'application/json, text/event-stream', 'mcp-session-id': sid },
    body: JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized' }) });
  const p = MODEL.replace(/\\/g, '\\\\').replace(/\//g, '\\\\');
  console.log(await evalText(`var m = ModelProject.all.find(function(m){return m.name == "${TAB}"}); if (m) { m.saved = true; m.close(true); } "closed"`));
  console.log(await evalText(`Blockbench.read(["${p}"], {}, function(files){ loadModelFile(files[0]); }); "loading"`));
  for (let i = 0; i < 60; i++) { await sleep(500); if ((await evalText('Project ? Project.name : ""')).includes(TAB)) break; }
  console.log('project', await evalText(`var t = ModelProject.all.find(function(m){return m.name == "${TAB}"}); t.select(); Project.name + " " + Outliner.elements.length`));
  const view = 'overgrowth_shots';
  try { await call('delete_offscreen_view', { view }); } catch (e) { /* none yet */ }
  await call('create_offscreen_view', { id: view, width: 1400, height: 860, antialias: true, copy_view: 'none' });
  const dir = path.join(__dirname, 'shots');
  fs.mkdirSync(dir, { recursive: true });
  for (const [name, position, target] of MARKS) {
    if (want.length && !want.some((w) => name.includes(w))) continue;
    await evalText(`var t = ModelProject.all.find(function(m){return m.name == "${TAB}"}); if (Project !== t) t.select(); "ok"`);
    await call('set_camera_angle', { view, position, target, projection: 'perspective', fov: 60 });
    const r = await call('capture_screenshot', { view, format: 'png' });
    const img = (r.content || []).find((c) => c.type === 'image');
    if (!img) throw new Error('no image for ' + name);
    fs.writeFileSync(path.join(dir, name + '.png'), overSky(Buffer.from(img.data, 'base64')));
    console.log('saved', name);
  }
  await call('delete_offscreen_view', { view });
})().catch((e) => { console.error(e); process.exit(1); });
