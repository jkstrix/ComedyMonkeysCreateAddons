// What's new: each release in releases.json, newest first. The highlights are cards of the entries it added; the
// sections are ordinary body blocks. #/whats-new shows the newest; #/whats-new/<version> any one of them.

import { db, entriesOf } from '../core/data.js';
import { esc, md } from '../core/text.js';
import { entryCard, body, crumbs } from '../ui/components.js';
import { icon } from '../ui/icons.js';
import { gear } from '../ui/gear.js';

export function releasesPage(view, version) {
  const all = db.releases;
  const r = version ? all.find(x => x.version === version) : all[0];
  if (!r) {
    view.innerHTML = `<div class="empty" style="min-height:60vh">${gear(12)}<h2>No release notes yet</h2></div>`;
    return;
  }
  document.title = `What's new in ${r.version} · ${db.site.modName} Codex`;
  const others = all.filter(x => x !== r);

  view.innerHTML = `
    <section class="banner">${gear(24, 'gear-bg')}
      ${crumbs([['Codex', '#/'], ["What's new", '#/whats-new'], ...(version ? [[r.version]] : [])])}
      <div class="kicker" style="margin-top:12px">// VERSION ${esc(r.version)}${r.from ? ` · SINCE ${esc(r.from)}` : ''}${r.date ? ` · ${esc(r.date)}` : ''}</div>
      <h1>What's new in ${esc(r.version)}</h1>
      ${r.title ? `<p style="font:600 18px var(--display);color:var(--text);margin-bottom:6px">${esc(r.title)}</p>` : ''}
      <p>${md(r.summary ?? '')}</p>
    </section>
    ${r.highlights?.length ? `<section class="sec"><h2 class="sec-title">New in ${esc(r.version)}</h2><div class="cards compact">${entriesOf(r.highlights).map(entryCard).join('')}</div></section>` : ''}
    <section class="sec release">
      ${(r.sections ?? []).map(s => `<div class="release-sec reveal"><h2>${icon(s.icon ?? 'cog')}${esc(s.title)}</h2>${body(s.body)}</div>`).join('')}
    </section>
    ${others.length ? `<section class="sec"><h2 class="sec-title">Earlier releases</h2><div class="rows">${others.map(o =>
      `<a class="row" href="#/whats-new/${encodeURIComponent(o.version)}"><span class="thumb" style="width:42px;height:42px;font:700 13px var(--display);color:var(--copper)">${esc(o.version)}</span><span><b>${esc(o.title ?? o.version)}</b><small>${md(o.summary ?? '', false)}</small></span></a>`).join('')}</div></section>` : ''}`;
}
