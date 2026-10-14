// stage-models.mjs - copies the models KARTUN ships into app/src/main/assets/models/ (gitignored) and checks every
// file against the SHA-256 recorded in docs/model-licenses.md, so the APK never carries a file of unknown origin.
// Sources: our own conversion of realesr-general-x4v3 (tools/convert-model.py -> engine/third_party/converted/) and the
// unchanged x4plus from the xinntao release zip (engine/third_party/realesrgan-win/models/). [TESTED: 2026-10-14 · staged 4 files, hashes equal; the models load in the app on the Titan]
//
// Usage (from the project root): node tools/stage-models.mjs
import { createHash } from 'node:crypto';
import { copyFileSync, existsSync, mkdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';

const ROOT = process.cwd();
const OUT = join(ROOT, 'app', 'src', 'main', 'assets', 'models');
const THIRD = join(ROOT, 'engine', 'third_party');

// name -> [source dir, sha256 of .param, sha256 of .bin]; the hashes are the ones in docs/model-licenses.md.
const MODELS = {
  'realesr-general-x4v3': [join(THIRD, 'converted'),
    '2bf1ccee1c8b86ef4cbb23957ac4f4a0f6fbb1fca6bcc20b39caf3d7e28becfb',
    '8fdd6052f8931091164a570ee7b5c5b2d1dcd991112d464eb9ae1a5b52ece553'],
  'realesrgan-x4plus': [join(THIRD, 'realesrgan-win', 'models'),
    '35330ececcea33b6c397a72548e788d5d53becee4734c50b7fada36e89f10a86',
    '713ee713b0353afaa27976f0563a64a5043bd70b9bd8936c2e26e25ebcdbcddf'],
};

const sha = (p) => createHash('sha256').update(readFileSync(p)).digest('hex');
mkdirSync(OUT, { recursive: true });
let bad = 0;
for (const [name, [dir, hParam, hBin]] of Object.entries(MODELS)) {
  for (const [ext, want] of [['param', hParam], ['bin', hBin]]) {
    const src = join(dir, `${name}.${ext}`);
    if (!existsSync(src)) { console.error(`missing ${src} - run tools/fetch-ncnn.mjs / tools/convert-model.py first`); bad++; continue; }
    const got = sha(src);
    if (got !== want) { console.error(`hash mismatch ${src}: ${got}, expected ${want}`); bad++; continue; }
    copyFileSync(src, join(OUT, `${name}.${ext}`));
    console.log(`staged ${name}.${ext} (${got.slice(0, 8)})`);
  }
}
process.exit(bad ? 1 : 0);
