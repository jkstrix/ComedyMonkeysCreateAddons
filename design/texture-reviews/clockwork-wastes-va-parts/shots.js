// Takes the va-parts review's screenshots in the user's Blockbench (its MCP plugin over http://localhost:3000/bb-mcp; see
// va-parts.gen.js): opens va-parts.bbmodel as the tab "va-parts" (closing an older one), then for each clip ('dormant',
// 'awake') a gameplay-distance shot of every pair (before left, after right), and dormant close-ups of the small parts,
// before and after each.  node design/texture-reviews/clockwork-wastes-va-parts/shots.js   (then sheet.js)
const fs = require('fs');
const path = require('path');
const URL = 'http://localhost:3000/bb-mcp';
const MODEL = path.join(__dirname, 'va-parts.bbmodel');
const marks = JSON.parse(fs.readFileSync(path.join(__dirname, 'shots.json'), 'utf8'));
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
(async () => {
  await rpc('initialize', { protocolVersion: '2025-03-26', capabilities: {}, clientInfo: { name: 'va-parts-shots', version: '1' } });
  await fetch(URL, { method: 'POST', headers: { 'content-type': 'application/json', accept: 'application/json, text/event-stream', 'mcp-session-id': sid },
    body: JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized' }) });
  // open the scene as its own tab (risky_eval takes no comment markers, so none in this code)
  const p = MODEL.replace(/\\/g, '\\\\').replace(/\//g, '\\\\');
  console.log(await evalText(`var m = ModelProject.all.find(function(m){return m.name == "va-parts"}); if (m) { m.saved = true; m.close(true); } "closed"`));
  console.log(await evalText(`Blockbench.read(["${p}"], {}, function(files){ loadModelFile(files[0]); }); "loading"`));
  for (let i = 0; i < 40; i++) { await sleep(500); if ((await evalText('Project ? Project.name : ""')).includes('va-parts')) break; }
  console.log('project', await evalText('Project.name + " " + Outliner.elements.length'));
  const view = 'shots';
  await call('create_offscreen_view', { id: view, width: 1200, height: 900, antialias: true, copy_view: 'none' });
  const shoot = async (m, file) => {
    await call('set_camera_angle', { view, position: m.position, target: m.target, projection: 'perspective', fov: 60 });
    const r = await call('capture_screenshot', { view, format: 'png' });
    const img = (r.content || []).find((c) => c.type === 'image');
    if (!img) throw new Error('no image for ' + file);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, Buffer.from(img.data, 'base64'));
    console.log('saved', path.relative(__dirname, file));
  };
  for (const clip of ['dormant', 'awake']) {
    console.log(await evalText(`Modes.options.animate.select(); var a = Animation.all.find(function(a){return a.name=="${clip}"}); a.select(); Timeline.setTime(0.5); Animator.preview(); "${clip}"`));
    await sleep(300);
    for (const [i, m] of marks.gameplay.entries()) await shoot(m, path.join(__dirname, 'shots', `${String(i + 1).padStart(2, '0')}-${m.id}-${clip}.png`));
    if (clip === 'dormant') for (const [i, m] of marks.close.entries()) await shoot(m, path.join(__dirname, 'shots', 'close', `${String(i + 1).padStart(2, '0')}-${m.id}.png`));
  }
  await call('delete_offscreen_view', { view });
})().catch((e) => { console.error(e); process.exit(1); });
