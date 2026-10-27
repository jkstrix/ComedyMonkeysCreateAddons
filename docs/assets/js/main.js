// Starts the codex: loads the data, draws the frame, and hands each address to its page.

import { db, load } from './core/data.js';
import { route, otherwise, onRoute, start } from './core/router.js';
import { toggleFavourite } from './core/storage.js';
import { renderShell, markNav } from './ui/shell.js';
import { installImageFallbacks } from './ui/components.js';
import { installLightbox } from './ui/lightbox.js';
import { injectIcons, icon } from './ui/icons.js';
import { animate } from './ui/motion.js';
import { homePage } from './pages/home.js';
import { categoryPage } from './pages/category.js';
import { entryPage } from './pages/entry.js';
import { recipesPage } from './pages/recipes.js';
import { progressionPage } from './pages/progression.js';
import { releasesPage } from './pages/releases.js';
import { searchPage, savedPage, itemPage, notFound } from './pages/misc.js';

const root = document.getElementById('app');

async function boot() {
  document.documentElement.classList.add('js');
  try {
    await load();
  } catch (e) {
    root.innerHTML = `<div class="empty" style="min-height:100vh"><h2>The codex couldn't load</h2>
      <p>${location.protocol === 'file:' ? 'Open it through a web server (for example <code>npx serve docs</code>): browsers block loading data straight from disk.' : `A data file failed to load: ${String(e.message ?? e)}`}</p></div>`;
    console.error(e);
    return;
  }
  injectIcons();
  renderShell(root);
  installImageFallbacks();
  installLightbox();
  const view = () => document.getElementById('view');

  route('', () => homePage(view()));
  route('recipes', (p, q, same) => recipesPage(view(), q, same));
  route('progression', () => progressionPage(view()));
  route('whats-new', () => releasesPage(view()));
  route('whats-new/:version', p => releasesPage(view(), p.version));
  route('search', (p, q) => searchPage(view(), q));
  route('favourites', () => savedPage(view()));
  route('item/:ns/:path', p => itemPage(view(), `${p.ns}:${p.path}`));
  otherwise((segments, query) => {
    const cat = db.category.get(segments[0]);
    if (cat && segments.length === 1) return categoryPage(view(), cat, query);
    const entry = segments.length === 2 ? db.entries.get(segments[1]) : null;
    // an entry found under the wrong category (an old link): show it at its real address
    if (entry && entry.cat !== cat) { location.replace(`#/${entry.cat.id}/${entry.id}`); return; }
    if (entry) return entryPage(view(), entry, query);
    if (segments.length === 1 && db.entries.get(segments[0])) { const e = db.entries.get(segments[0]); location.replace(`#/${e.cat.id}/${e.id}`); return; }
    notFound(view());
  });
  onRoute(({ segments }) => {
    markNav(segments);
    animate(view());
  });

  // favourite stars anywhere on the page
  document.addEventListener('click', e => {
    const b = e.target.closest('[data-fav]');
    if (!b) return;
    e.preventDefault();
    e.stopPropagation();
    const on = toggleFavourite(b.dataset.fav);
    document.querySelectorAll(`[data-fav="${CSS.escape(b.dataset.fav)}"]`).forEach(x => {
      x.classList.toggle('on', on);
      x.setAttribute('aria-pressed', on);
      x.innerHTML = `${icon(on ? 'starFill' : 'star')}<span class="sr-only">Favourite</span>`;
    });
  });

  await start();
}

boot();
