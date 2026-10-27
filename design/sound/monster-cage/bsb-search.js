// BigSoundBank (all CC0) search and download helper.
// node bsb-search.js search <query...>      -> lists "id  slug" per query
// node bsb-search.js get <id> <outDir>      -> downloads https://bigsoundbank.com/UPLOAD/ogg/<id>.ogg
const fs = require('fs');
const path = require('path');
const UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124 Safari/537.36';
const [cmd, ...args] = process.argv.slice(2);
(async () => {
  if (cmd === 'search') {
    for (const q of args) {
      const res = await fetch('https://bigsoundbank.com/search?q=' + encodeURIComponent(q), { headers: { 'user-agent': UA } });
      const html = await res.text();
      const hits = [...new Set([...html.matchAll(/\/([a-z0-9-]+)-s(\d{3,5})\.html/g)].map((m) => `${m[2].padStart(4, '0')}  ${m[1]}`))];
      console.log(`## ${q} (${hits.length})\n` + hits.slice(0, 30).join('\n'));
    }
  } else if (cmd === 'get') {
    const [id, out] = args;
    fs.mkdirSync(out, { recursive: true });
    const res = await fetch(`https://bigsoundbank.com/UPLOAD/ogg/${id}.ogg`, { headers: { 'user-agent': UA, referer: 'https://bigsoundbank.com/' } });
    if (!res.ok) throw new Error(`${id}: HTTP ${res.status}`);
    const file = path.join(out, `${id}.ogg`);
    fs.writeFileSync(file, Buffer.from(await res.arrayBuffer()));
    console.log('got', file);
  }
})().catch((e) => { console.error(e.message); process.exit(1); });
