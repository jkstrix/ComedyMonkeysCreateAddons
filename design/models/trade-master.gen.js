// Trade Master generator -> design/models/trade-master.bbmodel (+ trade-master.png)
// He is Trade Bot plus changes (design/briefs/trade-master.md): this runs Trade Bot's generator with the variant module,
// so his rig, clips and sounds are always Trade Bot's. Extra arguments pass through (an output path, for example).
const { execFileSync } = require('child_process');
const path = require('path');
execFileSync(process.execPath, [path.resolve(__dirname, 'trade-bot.gen.js'), '--variant=trade-master.variant.js', ...process.argv.slice(2)], { stdio: 'inherit', env: process.env });
