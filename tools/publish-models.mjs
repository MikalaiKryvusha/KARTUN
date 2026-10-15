#!/usr/bin/env node
// publish-models.mjs - uploads the catalogue's downloadable models to the GitHub release `models` of
// MikalaiKryvusha/KARTUN (the owner's variant B, MASTER_PLAN 2026-10-14 ≈ 22:04: «Б. Наш GitHub, отдельный релиз models»;
// plans/05 step 4). Every file is hashed and compared with docs/model-catalogue.json BEFORE anything leaves the machine
// (a file of unknown origin is never published); the licence texts the models need go up beside them. The release is
// created on first use as a prerelease that is never marked «latest», so the repository page does not show it as an app
// release. Files already on the release with the right size are skipped (re-runs resume).
// [NOT-TESTED]
//
// Usage (from the project root): node tools/publish-models.mjs [--dry-run]
import { createHash } from 'node:crypto';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
import { tmpdir } from 'node:os';

const REPO = 'MikalaiKryvusha/KARTUN';
const TAG = 'models';
const THIRD = join('engine', 'third_party');
const SEARCH = [join(THIRD, 'converted'), join(THIRD, 'realesrgan-win', 'models'), join(THIRD, 'catalogue')];
const LICENCE_FILES = {
  'BSD-3-Clause': join('docs', 'licenses', 'Real-ESRGAN_BSD-3-Clause.txt'),
  'CC-BY-4.0': join('docs', 'licenses', 'CC-BY-4.0.txt'),
  'CC-BY-NC-SA-4.0': join('docs', 'licenses', 'CC-BY-NC-SA-4.0.txt'),
  'Apache-2.0': join('docs', 'licenses', 'Apache-2.0.txt'),
  'WTFPL': join('docs', 'licenses', 'WTFPL.txt'),
};
const dry = process.argv.includes('--dry-run');
const gh = (...a) => spawnSync('gh', a, { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
const sha = (p) => createHash('sha256').update(readFileSync(p)).digest('hex');

const cat = JSON.parse(readFileSync(join('docs', 'model-catalogue.json'), 'utf8'));
const models = cat.models.filter((m) => !m.builtin);

// 1. Every byte checked against the catalogue first.
let bad = 0;
const uploads = [];
for (const m of models) for (const f of m.files) {
  const src = SEARCH.map((d) => join(d, f.name)).find((p) => existsSync(p));
  if (!src) { console.error(`missing ${f.name}`); bad++; continue; }
  const h = sha(src);
  if (h !== f.sha256) { console.error(`hash mismatch ${src}: ${h}, catalogue ${f.sha256}`); bad++; continue; }
  uploads.push({ path: src, name: f.name, bytes: f.bytes });
}
const licences = [...new Set(models.map((m) => m.licence))];
for (const l of licences) {
  if (!LICENCE_FILES[l] || !existsSync(LICENCE_FILES[l])) { console.error(`no licence text for ${l}`); bad++; continue; }
  uploads.push({ path: LICENCE_FILES[l], name: `LICENSE-${l}.txt`, bytes: readFileSync(LICENCE_FILES[l]).length });
}
if (bad) process.exit(1);
console.log(`${uploads.length} files checked (${models.length} models, ${licences.length} licences), ` +
  `${(uploads.reduce((s, u) => s + u.bytes, 0) / 1e6).toFixed(1)} MB`);

// 2. The release notes: which model, whose, under what licence (the models belong to their authors).
// A licence that allows commercial use but a model kept non-commercial = a disputed licence (ClearReality: Apache 2.0 on the
// author's card, CC BY-NC-SA 4.0 on OpenModelDB) - say why, so the line does not read as a contradiction.
const licenceCell = (m) => m.commercial ? m.licence : m.licence.includes('-NC-') ? `${m.licence} — not for commercial use`
  : `${m.licence} (disputed) — treated as not for commercial use`;
const rows = models.map((m) => `| ${m.name} | ${m.author} | ${licenceCell(m)} | ${m.files.map((f) => `\`${f.name}\``).join(' · ')} |`);
const notes = [
  'Neural upscaling models the KARTUN app downloads on demand from its «All models» screen. Every file carries its SHA-256 in',
  '[docs/model-catalogue.json](https://github.com/MikalaiKryvusha/KARTUN/blob/main/docs/model-catalogue.json); the app refuses a',
  'file whose hash differs.',
  '',
  '**The models belong to their authors.** KARTUN claims no ownership and passes on no rights beyond each author\'s licence;',
  'the licence texts are attached. Provenance and conversions: [docs/model-licenses.md](https://github.com/MikalaiKryvusha/KARTUN/blob/main/docs/model-licenses.md).',
  '',
  '| Model | Author | Licence | Files |',
  '|---|---|---|---|',
  ...rows,
  '',
].join('\n');
const notesFile = join(tmpdir(), 'kartun-models-notes.md');
writeFileSync(notesFile, notes);
if (dry) { console.log(notes); process.exit(0); }

// 3. The release (created once) and the uploads.
if (gh('release', 'view', TAG, '-R', REPO).status !== 0) {
  const r = gh('release', 'create', TAG, '-R', REPO, '--title', 'KARTUN models', '--notes-file', notesFile, '--prerelease', '--latest=false');
  if (r.status !== 0) { console.error(r.stderr); process.exit(1); }
  console.log('created release models');
} else {
  gh('release', 'edit', TAG, '-R', REPO, '--notes-file', notesFile);
}
const have = JSON.parse(gh('release', 'view', TAG, '-R', REPO, '--json', 'assets').stdout || '{"assets":[]}').assets;
for (const u of uploads) {
  const a = have.find((x) => x.name === u.name);
  if (a && a.size === u.bytes) { console.log(`have ${u.name}`); continue; }
  const r = gh('release', 'upload', TAG, `${u.path}#${u.name}`, '-R', REPO, '--clobber');
  if (r.status !== 0) { console.error(`upload failed ${u.name}: ${r.stderr}`); process.exit(1); }
  console.log(`uploaded ${u.name} (${(u.bytes / 1e6).toFixed(1)} MB)`);
}
console.log('done');
