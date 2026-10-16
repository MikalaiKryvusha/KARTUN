#!/usr/bin/env node
// format-matrix.mjs - which input formats KARTUN opens on the device (plans/08 step 3). Makes one test picture per format
// from a source photo with ImageMagick, sends each through the agent API (tools/kartun-cli.mjs, the same decoder path as
// the screens: UpscaleEngine.decodeAny), and compares every result with the PNG one by PSNR: a lossless format must come
// out identical (PSNR 120 in ImageMagick's terms), a lossy one close.
//
// Usage: KARTUN_SERIAL=<ip:port> node tools/format-matrix.mjs <source photo> <work dir> [--model <id>]
// Exit 1 when a format of EXPECT_OPEN does not open or a lossless one differs from PNG.
import { execFileSync, spawnSync } from 'node:child_process';
import { existsSync, mkdirSync } from 'node:fs';
import { join } from 'node:path';

const [src, dir] = process.argv.slice(2);
if (!src || !dir) { console.error('usage: node tools/format-matrix.mjs <source photo> <work dir> [--model <id>]'); process.exit(2); }
const mi = process.argv.indexOf('--model');
const model = mi > 0 ? process.argv[mi + 1] : 'realesr-general-x4v3';

// format -> [must open, lossless vs the PNG run]. pgm is greyscale: compared to nothing, only opened.
const FORMATS = {
  png: [true, true], jpg: [true, false], webp: [true, false], gif: [true, false], bmp: [true, true], avif: [true, false],
  ico: [true, true], tga: [true, true], psd: [true, true], ppm: [true, true], pgm: [true, false], qoi: [true, true],
  hdr: [true, false], tif: [true, true], jxl: [false, false], exr: [false, false],
};

mkdirSync(join(dir, 'out'), { recursive: true });
const base = join(dir, 'base.png');
execFileSync('magick', [src, '-resize', '400x', base]);
let bad = 0;
const rows = [];
for (const [ext, [must, lossless]] of Object.entries(FORMATS)) {
  const file = join(dir, `fmt_${ext}.${ext}`);
  execFileSync('magick', [base, file]);
  const out = join(dir, 'out', `${ext}.png`);
  const r = spawnSync('node', ['tools/kartun-cli.mjs', 'upscale', file, '--model', model, '--scale', '2', '--format', 'png', '--out', out], { encoding: 'utf8' });
  const line = (r.stdout || '').trim().split('\n').pop() || '';
  const ok = /"ok":true/.test(line) && existsSync(out);
  let psnr = '';
  if (ok && ext !== 'png' && existsSync(join(dir, 'out', 'png.png'))) {
    const c = spawnSync('magick', ['compare', '-metric', 'PSNR', join(dir, 'out', 'png.png'), out, 'null:'], { encoding: 'utf8' });
    psnr = (c.stderr || '').trim().split(' ')[0];
  }
  const fail = (must && !ok) || (lossless && ok && psnr && psnr !== '120' && psnr !== 'inf');
  if (fail) bad++;
  rows.push(`${ext.padEnd(5)} ${ok ? 'opens' : 'NO   '} ${psnr ? 'PSNR ' + psnr : ''}${fail ? '  <- FAIL' : ''}${!ok ? '  ' + (line.match(/"code":"[a-z_]+"/)?.[0] ?? '') : ''}`);
}
console.log(rows.join('\n'));
console.log(bad ? `format-matrix: ${bad} failure(s)` : 'format-matrix: every expected format opens; lossless ones match PNG');
process.exit(bad ? 1 : 0);
