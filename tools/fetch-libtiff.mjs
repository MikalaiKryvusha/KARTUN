#!/usr/bin/env node
// fetch-libtiff.mjs - downloads libtiff 4.7.2 (the libtiff licence: permissive, BSD-like) into engine/third_party/ and checks
// its SHA-256, so the engine's second decoder can open TIFF (plans/08 step 3: Android's ImageDecoder refuses TIFF on the
// Titan, 2026-10-16). Pinned by version AND hash (the engine itself is pinned by tag and commit in tools/build-ncnn.mjs); engine/third_party/ stays outside git.
// Usage (from the project root): node tools/fetch-libtiff.mjs
import { createHash } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const VERSION = '4.7.2';
const URL = `https://download.osgeo.org/libtiff/tiff-${VERSION}.tar.gz`;
// SHA-256 recorded at the first download (2026-10-16 15:10 +03:00); a mismatch refuses the archive.
const SHA256 = '672bd7d10aee4606171afb864f3570b83340f6a33e2c186dc0512f7145ffdf6a';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const dir = join(root, 'engine', 'third_party');
const tgz = join(dir, `tiff-${VERSION}.tar.gz`);
const out = join(dir, `tiff-${VERSION}`);

if (existsSync(join(out, 'CMakeLists.txt'))) { console.log(`libtiff ${VERSION} already in ${out}`); process.exit(0); }
mkdirSync(dir, { recursive: true });
if (!existsSync(tgz)) {
  const res = await fetch(URL);
  if (!res.ok) { console.error(`download failed: ${res.status} ${URL}`); process.exit(1); }
  writeFileSync(tgz, Buffer.from(await res.arrayBuffer()));
}
const got = createHash('sha256').update(readFileSync(tgz)).digest('hex');
if (got !== SHA256) { console.error(`SHA-256 mismatch for ${tgz}: ${got}`); process.exit(1); }
execFileSync('tar', ['-xzf', `tiff-${VERSION}.tar.gz`], { cwd: dir, stdio: 'inherit' });
console.log(`libtiff ${VERSION} unpacked to ${out} (sha256 ok)`);
