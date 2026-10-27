// The home page. What it shows comes from site.json "home": the hero, the latest additions, featured machines,
// blocks and mobs, the introduction and the developer notes.

import { db, href, entry as getEntry, entriesOf, entryImages } from '../core/data.js';
import { esc, md, plural } from '../core/text.js';
import { recent, seenCount, isGoalDone } from '../core/storage.js';
import { picture, entryPic, entryCard, dossier, categoryTile, listItem, stats, row } from '../ui/components.js';
import { icon } from '../ui/icons.js';
import { gear } from '../ui/gear.js';

export function homePage(view) {
  const h = db.site.home ?? {};
  document.title = `${db.site.modName} · Codex`;
  const heroEntry = getEntry(h.hero?.entry);
  const total = db.all.length;
  const seen = seenCount(db.all.map(e => e.id));
  const recents = recent().map(getEntry).filter(Boolean).slice(0, 8);
  const featured = entriesOf(h.featuredMachines);
  const browse = db.categories.filter(c => c.file || c.status === 'soon');

  view.innerHTML = `
    <header class="hero">
      <div class="bg"></div>${gear(32, 'gear-bg')}${gear(14, 'gear-bg small')}
      ${heroEntry ? `<a class="hero-render" href="${href(heroEntry)}" aria-label="${esc(heroEntry.name)}">${picture(entryImages(heroEntry), { alt: heroEntry.name, eager: true })}</a>` : ''}
      <div class="txt">
        <div class="kicker">// CODEX · ${esc(db.site.modName.toUpperCase())} · v${esc(db.site.version)}</div>
        <h1>${esc(h.hero?.title ?? db.site.modName)}${h.hero?.accent ? `<span>${esc(h.hero.accent)}</span>` : ''}</h1>
        <p>${md(h.hero?.text ?? '')}</p>
        <div class="btns">
          <a class="btn primary" href="#/guide${db.category.get('guide')?.entries[0] ? `/${db.category.get('guide').entries[0].id}` : ''}">${icon('book')}Start the guide</a>
          <a class="btn" href="#/progression">${icon('steps')}What to build next</a>
        </div>
        <div class="progress"><div class="bar"><i style="width:${total ? Math.max(2, seen / total * 100) : 0}%"></i></div>Viewed ${seen} of ${plural(total, 'entry', 'entries')}</div>
      </div>
    </header>

    <section class="sec"><h2 class="sec-title">Browse</h2><div class="tiles">${browse.map(categoryTile).join('')}</div></section>

    ${recents.length ? `<section class="sec" style="padding-top:0"><h2 class="sec-title">Recently viewed<a href="#/favourites">saved &amp; recent</a></h2>
      <div class="chips">${recents.map(e => `<a class="chip recent" href="${href(e)}">${entryPic(e)}${esc(e.name)}</a>`).join('')}</div></section>` : ''}

    ${h.latest?.length ? `<section class="sec"><h2 class="sec-title">Latest additions</h2><div class="cards">${entriesOf(h.latest).map(entryCard).join('')}</div></section>` : ''}

    ${featured.length ? `<section class="sec"><h2 class="sec-title">Featured machines<a href="#/machines">all machines</a></h2>${spotlight(featured)}</section>` : ''}

    ${systemsBlock()}

    ${progressionBlock()}

    ${h.featuredBlocks?.length ? `<section class="sec"><h2 class="sec-title">Featured blocks<a href="#/blocks">all blocks</a></h2><div class="cards">${entriesOf(h.featuredBlocks).map(entryCard).join('')}</div></section>` : ''}

    ${h.featuredMobs?.length ? `<section class="sec"><h2 class="sec-title">Mob dossiers<a href="#/mobs">all mobs</a></h2><div class="dossiers">${entriesOf(h.featuredMobs).map(dossier).join('')}</div></section>` : ''}

    <section class="sec intro">
      <div><h2 class="sec-title">${esc(h.intro?.title ?? 'The workshop')}</h2><div class="prose reveal">${(h.intro?.body ?? []).map(p => `<p>${md(p)}</p>`).join('')}</div></div>
      ${h.notes?.length ? `<div><h2 class="sec-title">Developer notes</h2><div class="notes">${h.notes.map(n => `<div class="note reveal"><span class="when">${esc(n.date ?? '')}</span><b>${esc(n.title)}</b><p>${md(n.body)}</p></div>`).join('')}</div></div>` : ''}
    </section>`;

  wireSpotlight(view, featured);
}

/** A small codex of the featured machines: pick one on the left, see it in the middle, its dials on the right. */
function spotlight(list) {
  const first = list[0];
  return `<div class="spot reveal">
    <div class="plist">${list.map((e, i) => listItem(e, i === 0).replace('<a class="li', `<a data-spot="${i}" class="li`)).join('')}</div>
    <div class="pmain">
      <a class="stage-pic" href="${href(first)}">${entryPic(first)}</a>
      <div class="pinfo"><h3>${esc(first.name)}</h3><p>${md(first.summary)}</p><a class="btn" href="${href(first)}">Open entry ${icon('arrow')}</a></div>
    </div>
    <div class="pside"><h4 class="label" style="margin-bottom:10px">Readout</h4>${stats(first.stats?.slice(0, 4))}</div>
  </div>`;
}

function wireSpotlight(view, list) {
  const spot = view.querySelector('.spot');
  if (!spot) return;
  spot.querySelector('.plist').addEventListener('click', ev => {
    const a = ev.target.closest('[data-spot]');
    if (!a || ev.metaKey || ev.ctrlKey) return;
    ev.preventDefault();
    const e = list[+a.dataset.spot];
    spot.querySelectorAll('.li').forEach(li => li.classList.toggle('on', li === a));
    const pic = spot.querySelector('.stage-pic');
    pic.href = href(e);
    pic.innerHTML = entryPic(e);
    spot.querySelector('.pinfo').innerHTML = `<h3>${esc(e.name)}</h3><p>${md(e.summary)}</p><a class="btn" href="${href(e)}">Open entry ${icon('arrow')}</a>`;
    const side = spot.querySelector('.pside');
    side.innerHTML = `<h4 class="label" style="margin-bottom:10px">Readout</h4>${stats(e.stats?.slice(0, 4))}`;
    side.querySelectorAll('.gauge').forEach(g => {
      requestAnimationFrame(() => {
        g.querySelector('.arc').style.strokeDashoffset = g.querySelector('.arc').dataset.offset;
        g.querySelector('.needle').style.transform = `rotate(${g.querySelector('.needle').dataset.angle}deg)`;
      });
      g.dataset.armed = '1';
    });
  });
}

function systemsBlock() {
  const systems = db.category.get('systems')?.entries ?? [];
  if (!systems.length) return '';
  return `<section class="sec"><h2 class="sec-title">Major systems<a href="#/systems">all systems</a></h2>
    <div class="rows" style="display:grid;grid-template-columns:repeat(auto-fill,minmax(min(100%,320px),1fr));gap:8px">${systems.map(e => row(e, { sub: md(e.summary, false) })).join('')}</div></section>`;
}

function progressionBlock() {
  const stages = db.site.progression ?? [];
  if (!stages.length) return '';
  const nextAt = stages.findIndex(s => (s.goals ?? []).some((_, i) => !isGoalDone(`${s.id}:${i}`)));
  return `<section class="sec"><h2 class="sec-title">Progression<a href="#/progression">the full chart</a></h2><div class="stages">
    ${stages.map((s, i) => {
      const goals = s.goals ?? [];
      const done = goals.filter((_, g) => isGoalDone(`${s.id}:${g}`)).length;
      return `<a class="stage-card reveal" href="#/progression">
        ${i === nextAt ? '<span class="badge new next">you are here</span>' : ''}
        <div class="num">${String(i + 1).padStart(2, '0')}</div><h3>${esc(s.title)}</h3><div class="era">${esc(s.era ?? '')}</div>
        <p>${md(s.summary ?? '')}</p>
        ${goals.length ? `<div class="bar"><i style="width:${done / goals.length * 100}%"></i></div>` : ''}
      </a>`;
    }).join('')}</div></section>`;
}
