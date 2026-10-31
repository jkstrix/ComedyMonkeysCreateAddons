// D77 review: the bots' void-alloy before (the sheen bands) and after (the house grain: 'grain' keeps the lit top/left edge,
// 'framed' adds the dark bottom/right edge). Takes the screenshots in the user's Blockbench (its MCP plugin over
// http://localhost:3000/bb-mcp). For each bot and each version it opens models/<bot>-<version>.bbmodel (written by
// gen.js here, from clockwork-wastes-bots.gen.js) as its own tab, then:
//   gameplay-dormant, gameplay-lit   at gameplay distance (Scuttler 5 blocks, Sentry 6, Custodian 8), eye height
//   close-dormant, close-lit         filling the frame
//   action                           one clip frame (Scuttler mid-leap, Sentry firing, Custodian's slam)
// into shots/<bot>/<version>-<shot>.png. The sheen tabs close after their shots; the grain and framed tabs stay open.
// node design/texture-reviews/clockwork-wastes-bot-grain/shots.js [bot ...]   (then sheet.js)
const fs = require('fs');
const path = require('path');
const URL = 'http://localhost:3000/bb-mcp';
const VERSIONS = ['sheen', 'grain', 'framed'];
// dormant / lit / action: [clip, time]; cameras: target height, gameplay distance, close distance (in px, 16 a block)
const BOTS = {
  scuttler: { dormant: ['wake', 0], lit: ['idle', 0], action: ['leap', 0.55], ty: 4, far: 80, near: 16, actionTy: 11, actionNear: 22 },
  sentry: { dormant: ['unfold', 0], lit: ['unfold', 2.4], action: ['fire', 0.7], ty: 10, far: 96, near: 30, actionTy: 11, actionNear: 30, side: -1 },
  custodian: { dormant: ['waking', 0], lit: ['idle', 0], action: ['slam', 0.9], ty: 25, far: 128, near: 54, actionTy: 30, actionNear: 66 },
};
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
// a 3/4 view of the front (front is -Z; side 1 from +X, -1 from -X, the way the Sentry's head turns), eye height for gameplay
let SIDE = 1;
const cam = (ty, d, eye) => ({ position: [SIDE * 0.5 * d, eye === undefined ? ty + 0.25 * d : eye, -0.86 * d], target: [0, ty, 0] });
(async () => {
  await rpc('initialize', { protocolVersion: '2025-03-26', capabilities: {}, clientInfo: { name: 'bot-grain-shots', version: '1' } });
  await fetch(URL, { method: 'POST', headers: { 'content-type': 'application/json', accept: 'application/json, text/event-stream', 'mcp-session-id': sid },
    body: JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized' }) });
  const which = process.argv.slice(2).length ? process.argv.slice(2) : Object.keys(BOTS);
  for (const bot of which) for (const ver of VERSIONS) {
    const B = BOTS[bot]; SIDE = B.side || 1; const name = `${bot}-${ver}`, file = path.join(__dirname, 'models', name + '.bbmodel');
    const p = file.replace(/\\/g, '\\\\').replace(/\//g, '\\\\');
    await evalText(`var m = ModelProject.all.find(function(m){return m.name == "${name}"}); if (m) { m.saved = true; m.close(true); } "closed"`);
    await evalText(`Blockbench.read(["${p}"], {}, function(files){ loadModelFile(files[0]); }); "loading"`);
    let ok = false;
    for (let i = 0; i < 60; i++) { await sleep(500); if ((await evalText('Project ? Project.name : ""')).replace(/"/g, '') === name) { ok = true; break; } }
    if (!ok) throw new Error('did not open ' + name);
    await sleep(500);
    console.log('project', await evalText('Project.name + " " + Outliner.elements.length + " cubes"'));
    const view = 'botgrain';
    try { await call('delete_offscreen_view', { view }); } catch (e) { /* none yet */ }
    await call('create_offscreen_view', { id: view, width: 1200, height: 900, antialias: true, copy_view: 'none' });
    const pose = async ([clip, t]) => { await evalText(`Modes.options.animate.select(); var a = Animation.all.find(function(a){return a.name=="${clip}"}); a.select(); Timeline.setTime(${t}); Animator.preview(); "${clip}"`); await sleep(350); };
    const shoot = async (m, shot) => {
      await call('set_camera_angle', { view, position: m.position, target: m.target, projection: 'perspective', fov: 60 });
      const r = await call('capture_screenshot', { view, format: 'png' });
      const img = (r.content || []).find((c) => c.type === 'image');
      if (!img) throw new Error('no image for ' + name + ' ' + shot);
      const out = path.join(__dirname, 'shots', bot, `${ver}-${shot}.png`);
      fs.mkdirSync(path.dirname(out), { recursive: true });
      fs.writeFileSync(out, Buffer.from(img.data, 'base64'));
      console.log('saved', path.relative(__dirname, out));
    };
    await pose(B.dormant);
    await shoot(cam(B.ty * 0.6, B.far, 26), 'gameplay-dormant');
    await shoot(cam(B.ty * 0.6, B.near), 'close-dormant');
    await pose(B.lit);
    await shoot(cam(B.ty, B.far, 26), 'gameplay-lit');
    await shoot(cam(B.ty, B.near), 'close-lit');
    await pose(B.action);
    await shoot(cam(B.actionTy, B.actionNear), 'action');
    await call('delete_offscreen_view', { view });
    await evalText('Modes.options.edit.select(); "edit"');
    if (ver === 'sheen') await evalText(`var m = ModelProject.all.find(function(m){return m.name == "${name}"}); if (m) { m.saved = true; m.close(true); } "closed"`);
  }
})().catch((e) => { console.error(e); process.exit(1); });
