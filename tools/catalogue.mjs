#!/usr/bin/env node
// catalogue.mjs - copies the model catalogue's single source (docs/model-catalogue.json) into the app
// (app/src/main/assets/catalogue.json), checking every entry's shape first; `--check` exits 1 when the app's copy drifted
// from the source (the pair «docs/model-catalogue.json ↔ assets/catalogue.json», plans/05_model_catalogue.md step 2).
// It also copies every model's preview (docs/model-previews/<id>.webp) into app/src/main/assets/previews/, so the
// «Все модели» screen shows them without the network; --check covers the previews too.
// [TESTED: 2026-10-14 · written, then --check green, then a hand-edited copy -> DRIFT exit 1, restored]
// [NOT-TESTED: the 2026-10-16 fields (purpose, kind, builtin, rates) and the previews]
//
// Usage (from the project root): node tools/catalogue.mjs [--check]
import { readFileSync, writeFileSync, existsSync, mkdirSync, copyFileSync, readdirSync, rmSync } from 'node:fs';
import { join } from 'node:path';

const SRC = join('docs', 'model-catalogue.json');
const OUT_DIR = join('app', 'src', 'main', 'assets');
const OUT = join(OUT_DIR, 'catalogue.json');
const PREVIEW_DIR = join(OUT_DIR, 'previews');
const LICENCES = new Set(['Apache-2.0', 'BSD-3-Clause', 'CC-BY-4.0', 'CC-BY-NC-SA-4.0', 'MIT', 'WTFPL']);
// Each purpose is a string resource model_purpose_<purpose> in all 10 languages; each kind a filter chip of «Все модели».
const PURPOSES = new Set(['fast_photo', 'detail', 'web_photo', 'universal', 'clean_photo', 'photo', 'balanced', 'faces',
  'anime', 'anime_fast', 'sharp', 'sharpest']);
const KINDS = new Set(['photo', 'anime']);
const STRINGS = readFileSync(join('app', 'src', 'main', 'res', 'values', 'strings.xml'), 'utf8');

const src = JSON.parse(readFileSync(SRC, 'utf8'));
let bad = 0;
const fail = (m) => { console.error(`BAD ${m}`); bad++; };
for (const m of src.models) {
  for (const k of ['id', 'name', 'author', 'licence', 'source', 'purpose', 'preview']) if (!m[k]) fail(`${m.id}: no ${k}`);
  if (!LICENCES.has(m.licence)) fail(`${m.id}: unknown licence ${m.licence}`);
  if (typeof m.commercial !== 'boolean') fail(`${m.id}: commercial must be true or false`);
  if (m.licence.includes('-NC-') && m.commercial) fail(`${m.id}: an NC licence marked commercial`);
  if (!Array.isArray(m.files) || m.files.length !== 2) fail(`${m.id}: needs a .param and a .bin`);
  for (const f of m.files || []) if (!/^[0-9a-f]{64}$/.test(f.sha256) || !(f.bytes > 0)) fail(`${m.id}: ${f.name} hash or size`);
  if (!PURPOSES.has(m.purpose)) fail(`${m.id}: unknown purpose ${m.purpose}`);
  else if (!STRINGS.includes(`name="model_purpose_${m.purpose}"`)) fail(`${m.id}: no string model_purpose_${m.purpose}`);
  if (!KINDS.has(m.kind)) fail(`${m.id}: unknown kind ${m.kind}`);
  if (typeof m.builtin !== 'boolean') fail(`${m.id}: builtin must be true or false`);
  if (!(m.gpuMsPerMp > 0) || !(m.cpuMsPerMp > m.gpuMsPerMp)) fail(`${m.id}: rates`);
  if (!existsSync(join('docs', m.preview))) fail(`${m.id}: no preview docs/${m.preview}`);
}
if (new Set(src.models.map((m) => m.id)).size !== src.models.length) fail('duplicate id');
if (src.models.filter((m) => m.builtin).length < 1) fail('no built-in model');
const defaults = src.models.filter((m) => m.default === true);
if (defaults.length !== 1 || !defaults[0].builtin) fail('exactly one default model, and it must be built in');
if (bad) process.exit(1);

// The app needs only what it shows and checks; the source note stays in the docs.
const app = { release: src.release, models: src.models.map(({ source, rateSource, preview, ...m }) => m) };
const previews = src.models.map((m) => [join('docs', m.preview), join(PREVIEW_DIR, `${m.id}.webp`)]);
// Who made the photos the previews start from (tools/make-previews.py) - shown in «О приложении».
previews.push([join('docs', 'model-previews', 'credits.json'), join(PREVIEW_DIR, 'credits.json')]);
const text = JSON.stringify(app, null, 1) + '\n';
if (process.argv.includes('--check')) {
  const now = existsSync(OUT) ? readFileSync(OUT, 'utf8') : '';
  if (now !== text) { console.error(`DRIFT ${OUT} differs from ${SRC} - edit the source and rerun without --check`); process.exit(1); }
  for (const [from, to] of previews) {
    if (!existsSync(to) || !readFileSync(from).equals(readFileSync(to))) { console.error(`DRIFT ${to} - rerun without --check`); process.exit(1); }
  }
  console.log(`catalogue matches: ${app.models.length} models, ${previews.length} previews`);
} else {
  mkdirSync(OUT_DIR, { recursive: true });
  writeFileSync(OUT, text);
  rmSync(PREVIEW_DIR, { recursive: true, force: true });
  mkdirSync(PREVIEW_DIR, { recursive: true });
  for (const [from, to] of previews) copyFileSync(from, to);
  console.log(`written ${OUT}: ${app.models.length} models, ${readdirSync(PREVIEW_DIR).length} previews`);
}
