#!/usr/bin/env node
// titan-probe.mjs - phase 0 harness: runs one engine probe on the Titan through adb and prints the KARTUN_PROBE
// JSON line; with --ref <png> it pulls the output and prints PSNR against the reference (ImageMagick `magick`).
// Plan: plans/02_epic01_F0_engine_spike.md, steps 6-8. [NOT-TESTED]
//
// Usage: node tools/titan-probe.mjs --in test256.png --model realesrgan-x4plus --gpu true --tile 128
//          [--ref engine/third_party/testdata/ref_x4plus_256.png] [--bicubic engine/third_party/testdata/bicubic256x4.png]
// Env: KARTUN_ADB (default D:/Android/Sdk/platform-tools/adb.exe), KARTUN_SERIAL (default from `adb devices`).
import { spawnSync } from 'node:child_process';
import { join } from 'node:path';

const ADB = process.env.KARTUN_ADB || 'D:/Android/Sdk/platform-tools/adb.exe';
const PKG = 'com.kriniks.kartun.debug';
const DEVICE_DIR = `/sdcard/Android/data/${PKG}/files`;
const LOCAL_DIR = 'engine/third_party/testdata';
const TIMEOUT_MS = 10 * 60 * 1000;
const POLL_MS = 2000;

const arg = (k, d) => { const i = process.argv.indexOf(`--${k}`); return i > 0 ? process.argv[i + 1] : d; };
const serial = process.env.KARTUN_SERIAL ? ['-s', process.env.KARTUN_SERIAL] : [];
const adb = (...a) => spawnSync(ADB, [...serial, ...a], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });

const input = arg('in', 'test256.png');
const model = arg('model', 'realesrgan-x4plus');
const gpu = arg('gpu', 'true');
const tile = arg('tile', '128');

adb('logcat', '-c');
adb('shell', 'am', 'start', '-S', '-n', `${PKG}/com.kriniks.kartun.MainActivity`,
  '--es', 'in', input, '--es', 'model', model, '--ez', 'gpu', gpu, '--ei', 'tile', tile, '--ei', 'scale', '4');

let line = null;
const t0 = Date.now();
while (!line && Date.now() - t0 < TIMEOUT_MS) {
  const log = adb('logcat', '-d', '-s', 'KARTUN_PROBE:*', 'AndroidRuntime:E').stdout || '';
  line = log.split(/\r?\n/).find((l) => l.includes('KARTUN_PROBE') || l.includes('FATAL')) || null;
  if (!line) await new Promise((r) => setTimeout(r, POLL_MS));
}
// The probe starts KARTUN without its interface (MainActivity returns before setContent), so the device shows a black
// screen until the app is opened again - the owner saw it on 2026-10-14 ≈ 22:18 («почему черный экран сейчас?»).
// Every exit below therefore puts the normal interface back first.
const restoreUi = () => adb('shell', 'am', 'start', '-S', '-n', `${PKG}/com.kriniks.kartun.MainActivity`);
if (!line) { restoreUi(); console.error('no probe result within timeout'); process.exit(1); }
restoreUi();
const json = line.slice(line.indexOf('{'));
console.log(json);
const res = JSON.parse(json);
if (!res.ok) process.exit(1);

const ref = arg('ref'); const bic = arg('bicubic');
if (ref || bic) {
  const local = join(LOCAL_DIR, res.out);
  adb('pull', `${DEVICE_DIR}/${res.out}`, local);
  const psnr = (other) => {
    const r = spawnSync('magick', ['compare', '-metric', 'PSNR', local, other, 'null:'], { encoding: 'utf8' });
    return (r.stderr || r.stdout).trim().split(' ')[0];
  };
  if (ref) console.log(`PSNR vs reference: ${psnr(ref)} dB`);
  if (bic) console.log(`PSNR vs bicubic:   ${psnr(bic)} dB`);
}
