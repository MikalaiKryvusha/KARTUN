// stage-models.mjs - puts the models KARTUN ships inside the APK into app/src/main/assets/models/ (gitignored): exactly
// the catalogue entries with `builtin: true` (docs/model-catalogue.json - the single source of their hashes; the owner's
// pick 2026-10-15 14:12: NomosWebPhoto RealPLKSR + NMKD Siax). Every file is checked against its SHA-256, and every hash
// must also stand in docs/model-licenses.md (the provenance document), so the APK never carries a file of unknown origin.
// Files of models that are no longer built in are removed from the assets folder.
// [TESTED: 2026-10-14 · staged 4 files, hashes equal; the models load in the app on the Titan]
// [NOT-TESTED: the catalogue-driven version of 2026-10-16]
//
// Usage (from the project root): node tools/stage-models.mjs
import { createHash } from 'node:crypto';
import { copyFileSync, existsSync, mkdirSync, readFileSync, readdirSync, rmSync } from 'node:fs';
import { join } from 'node:path';

const ROOT = process.cwd();
const OUT = join(ROOT, 'app', 'src', 'main', 'assets', 'models');
const THIRD = join(ROOT, 'engine', 'third_party');
// Where the ncnn files of a model may lie: our conversions, the xinntao zip, the catalogue's converted/author files.
const SEARCH = [join(THIRD, 'converted'), join(THIRD, 'realesrgan-win', 'models'), join(THIRD, 'catalogue')];

const catalogue = JSON.parse(readFileSync(join(ROOT, 'docs', 'model-catalogue.json'), 'utf8'));
const builtin = catalogue.models.filter((m) => m.builtin);
const doc = readFileSync(join(ROOT, 'docs', 'model-licenses.md'), 'utf8');
const sha = (p) => createHash('sha256').update(readFileSync(p)).digest('hex');
let bad = 0;

// The pair «docs/model-licenses.md ↔ the catalogue»: every built-in hash must stand in the provenance document too.
for (const m of builtin) for (const f of m.files) {
  if (!doc.includes(f.sha256)) { console.error(`DRIFT ${f.name}: ${f.sha256.slice(0, 8)}… is not in docs/model-licenses.md`); bad++; }
}
mkdirSync(OUT, { recursive: true });
const wanted = new Set(builtin.flatMap((m) => m.files.map((f) => f.name)));
for (const name of readdirSync(OUT)) {
  if (!wanted.has(name)) { rmSync(join(OUT, name)); console.log(`removed ${name} (not built in any more)`); }
}
for (const m of builtin) for (const f of m.files) {
  const src = SEARCH.map((d) => join(d, f.name)).find((p) => existsSync(p));
  if (!src) { console.error(`missing ${f.name} in ${SEARCH.join(', ')} - convert or fetch it first`); bad++; continue; }
  const got = sha(src);
  if (got !== f.sha256) { console.error(`hash mismatch ${src}: ${got}, expected ${f.sha256}`); bad++; continue; }
  copyFileSync(src, join(OUT, f.name));
  console.log(`staged ${f.name} (${got.slice(0, 8)})`);
}
process.exit(bad ? 1 : 0);
