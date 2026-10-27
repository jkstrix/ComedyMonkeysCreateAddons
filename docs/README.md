# The Codex: the mod's companion website

A static site (HTML, CSS and JavaScript, no frameworks, no build step) that GitHub Pages serves
straight from this `docs/` folder. Every page is generated in the browser from the JSON files in
`data/`. To add a block, item, machine, mob, biome or system, add an object to the right file;
nothing else needs to change.

- Preview locally: `npx serve docs`, or `python -m http.server -d docs`, then open the address
  it prints. (Opening `index.html` straight from disk doesn't work: browsers block `fetch` there.)
- Publish: GitHub → Settings → Pages → *Deploy from a branch*, folder `/docs`.
- Addresses look like `#/machines/pressure-crucible`, so no server rewrite rules are needed.

## Files

```
docs/
  index.html            the only page: the shell (rail, top bar) that the router fills
  data/
    site.json           mod name, version, categories (drive the nav), home page, progression
    machines.json       one array per category, each holding entries (see "Entries")
    blocks.json
    items.json
    mobs.json
    biomes.json         empty for now: the category shows "coming soon" until it has entries
    systems.json
    guide.json          the long-form guide's chapters
    recipes.json        generated from the mod's recipe files (see "Recipes")
    releases.json       the "What's new" page: one object per release, newest first (see below)
  assets/
    css/                tokens, base, layout, components, pages, print
    js/                 core/ (data, router, search, storage), ui/ (components), pages/
    renders/            entry renders, made by the game (see "Renders")
    icons/              item icons by namespace: icons/minecraft/obsidian.png
```

## Entries

Every category file is an array of entries. Only `id`, `name` and `summary` are required; every
other field is optional and its section simply doesn't appear when it's missing.

```jsonc
{
  "id": "pressure-crucible",          // unique across ALL categories, kebab-case; the page address
  "name": "Pressure Crucible",
  "registry": ["comedymonkeysaddons:pressure_crucible"],  // game ids this entry covers: links recipes
                                      // and icons to it (include tiers and incomplete_ items)
  "summary": "One sentence, shown on cards and in search.",
  "tags": ["processing"],             // the first tag is the subcategory shown in lists and filters
  "stage": "advanced",                // early | mid | advanced | end (progression)
  "requires": ["upgrade-template"],   // entries to build first: arrows on the progression chart
  "requiresMods": ["Create Aeronautics"],

  // The five questions every entry answers (shown as the "At a glance" panel)
  "teach": {
    "what": "What does it do?",
    "why": "Why would I use it?",
    "when": "When should I unlock it?",
    "interacts": "How does it work with other systems?",
    "mistakes": ["What mistakes should I avoid?", "..."]
  },

  // The readout column: gauges and a spec list
  "stats": [{ "label": "Stress", "value": "12", "unit": "SU per RPM", "gauge": 0.45 }],
  "specs": [["Base speed", "16 RPM"], ["Inputs", "Any side"]],

  // Tiers and kinds that share one page
  "variants": [{ "id": "brass", "name": "Brass Cobblestone Generator",
                 "registry": ["comedymonkeysaddons:brass_cobble_generator"],
                 "summary": "...", "specs": [["Per cycle", "16 cobblestone"]] }],

  "tips": ["Pro tips"],
  "facts": ["Did-you-know facts"],
  "flows": [{ "title": "Emeralds from cobblestone", "steps": [
      { "label": "Cobblestone", "kind": "source" },
      { "item": "create:crushing_wheel", "label": "Crushing Wheels", "kind": "machine" },
      { "ref": "emerald-dust", "kind": "item", "via": "5%" },
      { "ref": "pressure-crucible", "kind": "machine", "note": "heated", "via": "×9" },
      { "item": "minecraft:emerald", "label": "Emerald", "kind": "result" }
  ]}],
  "sections": [{ "id": "mechanics", "title": "How it works", "body": ["Blocks: see below"] }],
  "related": ["emerald-dust", "netherite-dust"],
  "gallery": [{ "src": "renders/pressure-crucible--superheated.png", "caption": "Superheated" }]
}
```

A flow step names an entry with `ref` (its name and page come from that entry), a game item with
`item` (its icon comes from `assets/icons/`), or just a `label`. `kind` is `source`, `machine`,
`item` or `result`; `via` labels the arrow coming into the step; `note` is a small line above it.

### Fields by category

All of these are lists of strings unless shown otherwise, and each becomes its own section.

| Category | Fields |
|---|---|
| blocks | `uses`, `mechanics`, `automation` |
| machines | `power` (object: `kind` = `rotation`, `fe`, `lava` or `none`; `stress`, `speed`, `draws`, `makes` as strings), `inputs`, `outputs`, `throughput`, `rules`, `setups` (`[{ "title", "body": [blocks] }]`), `efficiency` |
| items | `obtain`, `uses` |
| mobs | `kind` (e.g. "Mod creature"), `threat` and `rarity` (0 to 5), `behaviour`, `spawning`, `ai`, `attacks`, `weaknesses`, `loot`, `farming`, `strategies` |
| biomes | `climate`, `features`, `resources`, `mobs` (entry ids), `strategies` |
| systems, guide | long-form: `sections` (and `chapter`, a number, in the guide) |

### Text

Strings support a little markup:

- `**bold**` and `*italic*`
- `[[pressure-crucible]]` links to an entry by its name; `[[pressure-crucible|the Crucible]]` with your own words
- `` `code` `` for commands and ids

### Body blocks (`sections[].body`, `setups[].body`)

A body is an array. A plain string is a paragraph; the objects are:

```jsonc
{ "heading": "Sub-heading" }
{ "list": ["one", "two"] }            // add "ordered": true for numbered steps
{ "tip": "..." }  { "warning": "..." }  { "fact": "..." }
{ "strategy": { "title": "...", "body": "..." } }
{ "table": { "head": ["Tier", "Holds"], "rows": [["Copper", "128"]] } }
{ "flow": { "title": "...", "steps": [ ... ] } }     // as in "flows"
{ "entries": ["fluid-wellspring", "magma-engine"] }   // a row of cards
{ "image": { "src": "renders/x.png", "caption": "..." } }
```

## Images

Nothing needs listing. An entry's render is `assets/renders/<id>.png`, each variant's is
`assets/renders/<id>--<variant id>.png`, and an item's icon is `assets/icons/<namespace>/<path>.png`
(from the first id in `registry`). An entry with no block of its own (a system, a guide chapter)
names a picture with `"image": "renders/fluid-lines.png"` (any path under `assets/`). A variant that
is the same block as the entry uses the entry's render. A missing image shows a "render pending"
plate, so entries can go in before their pictures. `gallery` adds more.

## Renders

The renders and icons are made by the game itself: the real block, block entity, item and entity
renderers, into transparent PNGs (Flywheel is switched off for the run, so every part is drawn by
its renderer). Run

```
./gradlew runRenders                       # everything (about 4 minutes)
./gradlew runRenders -Ponly="fish-tank.*"  # just the scenes whose id matches
./gradlew runRenders -Pstay                # leave the game open afterwards, to look around
```

The game makes a fresh flat world in `run-renders/`, flies to each scene in
`tools/website/renders.json`, builds it with commands, lets it run, draws just the scene's blocks
and entities into `docs/assets/renders/`, then draws an icon for **every game id named anywhere in
`docs/data`** (and every item of the mod) into `docs/assets/icons/`, writes the names it knows into
`data/names.json`, and quits. `tools/website/renders-report.txt` lists what it made.

A scene is a list of commands with `~` at the scene's origin; helpers outside its `region` (Creative
Motors, Blaze Burners, walls) run it without being drawn. Block-entity NBT fills machines (lava in
tanks, a captive in a jar), so the pictures show them working. `"block"` and `"variants"` repeat a
scene for each tier. A step `!use <item> x y z face` has a fake player use the item on that face of
that block, as a click would: it lays wires on a face and clips fittings onto lines, which
commands can't. Blocks face south and the camera looks from the south-east, so fronts are on
the left. The code is `src/main/java/.../client/dev/RenderExporter.java`; it does nothing unless
the game is started by this run.

## Recipes

`data/recipes.json` is generated from the mod's recipe files (and the names in its lang file):

```
node tools/website/import-recipes.mjs
```

Re-run it when recipes change. The Recipes page, each entry's "How to make" and "Used in", and
ingredient links all come from it.

## What's new (`releases.json`)

One object per release, newest first; `#/whats-new` shows the newest, `#/whats-new/<version>` any
other, and the home page links the newest. Add the next release at the top:

```jsonc
{
  "version": "2.6.0", "from": "2.5.0", "date": "2026-11-20",
  "title": "A few words",
  "summary": "A sentence or two.",
  "highlights": ["entry-id", "entry-id"],        // cards of the entries it added
  "sections": [{ "title": "Fixed", "icon": "check", "body": [{ "list": ["...", "..."] }] }]
}
```

## Site settings (`site.json`)

- `categories` lists the nav in order: `id`, `label`, `single` (one of them, "Machine"), `icon`,
  `file`, `description` and, for one that isn't ready, `"status": "soon"` with a `teaser` (Biomes,
  until `biomes.json` has entries). `recipes` and `progression` are pages, not files.
- `home` picks the hero render and words, the latest additions (they get a "new" badge everywhere),
  featured machines, blocks and mobs, the introduction and the developer notes.
- `progression` holds the four stages: title, era, summary, what you'll need, goals (ticked off in
  the visitor's browser) and the entries on the chart. Entries' `requires` draw the lines.
- `recipeTypes` names each recipe type and the machine whose icon it shows; `timeAt` says what
  speed the recipe's time is measured at.
- `unreleased` lists game ids to keep off the site (their recipes and icons too) until they ship.
- `extraIcons` lists more game ids to render icons for.
- `mods` names namespaces for players ("create" is "Create").

## Icons in the interface

Line icons are in `assets/js/ui/icons.js`: `book`, `cube`, `cog`, `gem`, `skull`, `grid`, `steps`,
`net`, `mountain`, `bolt`, `flame`, `rotate`, `drop` and more. A category's `icon` names one.
