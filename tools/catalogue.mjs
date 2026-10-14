#!/usr/bin/env node
// catalogue.mjs - copies the model catalogue's single source (docs/model-catalogue.json) into the app
// (app/src/main/assets/catalogue.json), checking every entry's shape first; `--check` exits 1 when the app's copy drifted
// from the source (the pair «docs/model-catalogue.json ↔ assets/catalogue.json», plans/05_model_catalogue.md step 2).
// [TESTED: 2026-10-14 · written, then --check green, then a hand-edited copy -> DRIFT exit 1, restored]
//
// Usage (from the project root): node tools/catalogue.mjs [--check]
import { readFileSync, writeFileSync, existsSync, mkdirSync } from 'node:fs';
import { join } from 'node:path';

const SRC = join('docs', 'model-catalogue.json');
const OUT_DIR = join('app', 'src', 'main', 'assets');
const OUT = join(OUT_DIR, 'catalogue.json');
const LICENCES = new Set(['BSD-3-Clause', 'CC-BY-4.0', 'CC-BY-NC-SA-4.0', 'MIT']);

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
}
if (bad) process.exit(1);

// The app needs only what it shows and checks; the source note stays in the docs.
const app = { release: src.release, models: src.models.map(({ source, ...m }) => m) };
const text = JSON.stringify(app, null, 1) + '\n';
if (process.argv.includes('--check')) {
  const now = existsSync(OUT) ? readFileSync(OUT, 'utf8') : '';
  if (now !== text) { console.error(`DRIFT ${OUT} differs from ${SRC} - edit the source and rerun without --check`); process.exit(1); }
  console.log(`catalogue matches: ${app.models.length} models`);
} else {
  mkdirSync(OUT_DIR, { recursive: true });
  writeFileSync(OUT, text);
  console.log(`written ${OUT}: ${app.models.length} models`);
}
