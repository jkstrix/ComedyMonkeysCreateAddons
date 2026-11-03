// What's new: every release in releases.json on one long page, newest first, like a release-note history you scroll
// back through. Each release has its heading, the cards of the entries it added (highlights) and its sections, which
// are ordinary body blocks. #/whats-new starts at the top; #/whats-new/<version> scrolls to that release.

import { db, entriesOf } from '../core/data.js';
import { esc, md } from '../core/text.js';
import { entryCard, body, crumbs } from '../ui/components.js';
import { icon } from '../ui/icons.js';
import { gear } from '../ui/gear.js';

const anchor = v => 'v-' + String(v).replace(/[^\w-]/g, '-');

function release(r, i) {
  return `
    <article class="release-entry${i === 0 ? ' latest' : ''}" id="${anchor(r.version)}">
      <header class="release-head">
        <div class="kicker">// VERSION ${esc(r.version)}${r.from ? ` · SINCE ${esc(r.from)}` : ''}${r.date ? ` · ${esc(r.date)}` : ''}${i === 0 ? ' · LATEST' : ''}</div>
        <h2>${esc(r.version)}${r.title ? `<span>${esc(r.title)}</span>` : ''}</h2>
        ${r.summary ? `<p>${md(r.summary)}</p>` : ''}
      </header>
      ${r.highlights?.length ? `<h3 class="sec-title">New in ${esc(r.version)}</h3><div class="cards compact">${entriesOf(r.highlights).map(entryCard).join('')}</div>` : ''}
      <div class="release">
        ${(r.sections ?? []).map(s => `<div class="release-sec reveal"><h2>${icon(s.icon ?? 'cog')}${esc(s.title)}</h2>${body(s.body)}</div>`).join('')}
      </div>
    </article>`;
}

export function releasesPage(view, version) {
  const all = db.releases;
  if (!all.length) {
    view.innerHTML = `<div class="empty" style="min-height:60vh">${gear(12)}<h2>No release notes yet</h2></div>`;
    return;
  }
  const target = version ? all.find(x => x.version === version) : null;
  document.title = `What's new${target ? ` in ${target.version}` : ''} · ${db.site.modName} Codex`;

  view.innerHTML = `
    <section class="banner">${gear(24, 'gear-bg')}
      ${crumbs([['Codex', '#/'], ["What's new", '#/whats-new'], ...(target ? [[target.version]] : [])])}
      <div class="kicker" style="margin-top:12px">// RELEASE NOTES · ${all.length} ${all.length === 1 ? 'RELEASE' : 'RELEASES'}</div>
      <h1>What's new</h1>
      <p>Every release, newest first. Scroll back through the history, or jump to one.</p>
      <nav class="chips" aria-label="Releases">${all.map((r, i) =>
        `<a class="chip${(target ? r === target : i === 0) ? ' on' : ''}" href="#/whats-new/${encodeURIComponent(r.version)}">${esc(r.version)}</a>`).join('')}</nav>
    </section>
    <section class="sec release-history">${all.map(release).join('')}</section>`;

  // the router scrolls to the top once the page is drawn: go to the release asked for after that
  if (target)
    requestAnimationFrame(() => document.getElementById(anchor(target.version))?.scrollIntoView({ block: 'start' }));
}
