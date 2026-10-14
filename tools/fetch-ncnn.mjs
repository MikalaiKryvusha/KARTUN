#!/usr/bin/env node
// fetch-ncnn.mjs - download the prebuilt NCNN android-vulkan package into engine/third_party/ and verify it.
// The package is pinned by tag AND by SHA-256 so every build links exactly the same NCNN (research 03 section A:
// release 20260526 carries the Adreno/Mali fixes KARTUN relies on). [NOT-TESTED]
// Usage: node tools/fetch-ncnn.mjs
import { createHash } from 'node:crypto';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const TAG = '20260526';
const NAME = `ncnn-${TAG}-android-vulkan`;
const URL = `https://github.com/Tencent/ncnn/releases/download/${TAG}/${NAME}.zip`;
// SHA-256 of the release asset, recorded at the first download (2026-10-14); a mismatch refuses the package.
const SHA256 = '26909c92eed35afed4a966b5e9e503fcb0a529691ea3f910ec2c94a4fff52804';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const dir = join(root, 'engine', 'third_party');
const zip = join(dir, `${NAME}.zip`);
mkdirSync(dir, { recursive: true });

if (!existsSync(zip)) {
  console.log(`downloading ${URL}`);
  const res = await fetch(URL, { redirect: 'follow' });
  if (!res.ok) { console.error(`download failed: ${res.status}`); process.exit(1); }
  writeFileSync(zip, Buffer.from(await res.arrayBuffer()));
}
const got = createHash('sha256').update(readFileSync(zip)).digest('hex');
console.log(`sha256 ${got}  ${NAME}.zip`);
if (SHA256 && got !== SHA256) { console.error(`sha256 mismatch: expected ${SHA256}`); process.exit(1); }

if (!existsSync(join(dir, NAME))) {
  // Windows ships bsdtar (System32\tar.exe), which unpacks zip; elsewhere use unzip.
  const r = process.platform === 'win32'
    ? spawnSync(join(process.env.SystemRoot || 'C:/Windows', 'System32', 'tar.exe'), ['-xf', zip, '-C', dir], { stdio: 'inherit' })
    : spawnSync('unzip', ['-q', zip, '-d', dir], { stdio: 'inherit' });
  if (r.status !== 0) { console.error('unpack failed'); process.exit(1); }
}
console.log(`ready: ${join(dir, NAME)}`);
