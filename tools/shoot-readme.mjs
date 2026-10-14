#!/usr/bin/env node
// shoot-readme.mjs - walks KARTUN on the Titan along the user's path and saves the README screenshots:
// home -> pick the beach photo -> «Улучшение» -> settings folded (4x) -> processing -> before/after -> settings -> guide -> about.
// One set per language: docs/screenshots/<lang>/NN_<screen>.webp (540 px wide, ImageMagick `magick`).
// The status bar is cleaned by Android's System UI demo mode; the rotation lock and the app language are put back after.
// The photo is the Real-ESRGAN sample `input2.jpg` (BSD-3, engine/third_party/testdata/test256.png), pushed as beach.png.
// [TESTED: 2026-10-14 · ru and en sets shot on the Titan 21:16-21:21 and read screen by screen as contact sheets; the
// first en run caught the «Enhance» title matching the button - fixed]
//
// Usage: node tools/shoot-readme.mjs [--lang ru|en|all]
// Env: KARTUN_ADB (default D:/Android/Sdk/platform-tools/adb.exe), KARTUN_SERIAL (e.g. 100.99.111.48:44487).
// Coordinates are the Titan's portrait 1600x2560; elements with text are found by uiautomator, not by coordinates.
import { spawnSync } from 'node:child_process';
import { mkdirSync, rmSync } from 'node:fs';
import { join } from 'node:path';

const ADB = process.env.KARTUN_ADB || 'D:/Android/Sdk/platform-tools/adb.exe';
const PKG = 'com.kriniks.kartun.debug';
const ACTIVITY = `${PKG}/com.kriniks.kartun.MainActivity`;
const OUT = 'docs/screenshots';
const WIDTH = 540;
const PHOTO_LOCAL = 'engine/third_party/testdata/test256.png';
const PHOTO_DEVICE = '/sdcard/Pictures/KARTUN_test/beach.png';
const PICKER_FIRST = [256, 1280]; // the newest photo in the system picker grid = the just-pushed beach
const GEAR = [1472, 170]; // the round icon in the top-right corner (home: settings, settings: about)
const LANGS = { ru: 'ru-RU', en: 'en-US' };

const arg = (k, d) => { const i = process.argv.indexOf(`--${k}`); return i > 0 ? process.argv[i + 1] : d; };
const serial = process.env.KARTUN_SERIAL ? ['-s', process.env.KARTUN_SERIAL] : [];
const adb = (...a) => spawnSync(ADB, [...serial, ...a], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
const sh = (cmd) => adb('shell', cmd).stdout || '';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const tap = (x, y) => sh(`input tap ${x} ${y}`);

/** Nodes of the current screen: [{text, x, y}] (centre of the bounds). */
function nodes() {
  sh('uiautomator dump /sdcard/kartun-ui.xml >/dev/null');
  const xml = sh('cat /sdcard/kartun-ui.xml');
  return [...xml.matchAll(/text="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/g)]
    .map((m) => ({ text: m[1], x: (+m[2] + +m[4]) >> 1, y: (+m[3] + +m[5]) >> 1 }));
}
function find(re) { return nodes().find((n) => re.test(n.text)); }
async function tapText(re, wait = 1500) {
  const n = find(re);
  if (!n) throw new Error(`no element ${re} on the screen`);
  tap(n.x, n.y); await sleep(wait); return n;
}
async function waitFor(re, ms) {
  const t0 = Date.now();
  while (Date.now() - t0 < ms) { if (find(re)) return true; await sleep(1000); }
  throw new Error(`timeout waiting for ${re}`);
}

/** Screenshot -> docs/screenshots/<lang>/<name>.webp, WIDTH px wide. */
function shot(lang, name) {
  mkdirSync(join(OUT, lang), { recursive: true });
  const raw = join(OUT, lang, `${name}.png`);
  const png = spawnSync(ADB, [...serial, 'exec-out', 'screencap', '-p'], { maxBuffer: 64 * 1024 * 1024 }).stdout;
  spawnSync('magick', ['png:-', '-resize', `${WIDTH}x`, '-quality', '90', join(OUT, lang, `${name}.webp`)], { input: png });
  rmSync(raw, { force: true });
  console.log(`  ${lang}/${name}.webp`);
}

// Words of the two languages the walk looks for (from values/strings.xml and values-ru/strings.xml).
const W = {
  pick: /^(Выберите фото|Choose a photo)$/,
  done: /^(Готово|Done)$/,
  enhance: /^(Улучшить|Enhance) ·/, // the button carries «· ≈ time»; the English screen title is «Enhance» too
  fourX: /^4×$/,
  maxModel: /^Real-ESRGAN x4plus$/,
  result: /^(Сохранить|Save)$/,
  manual: /(Руководство|guide)/i,
};

async function walk(lang) {
  console.log(`${lang}:`);
  sh(`cmd locale set-app-locales ${PKG} --locales ${LANGS[lang]}`);
  sh(`am force-stop ${PKG}`); sh(`am start -n ${ACTIVITY}`); await sleep(2500);
  shot(lang, '01_home');
  await tapText(W.pick, 3000);
  tap(...PICKER_FIRST); await sleep(1000);
  await tapText(W.done, 3000);
  shot(lang, '02_enhance');
  sh('input swipe 800 1900 800 700 600'); await sleep(1500);
  const four = find(W.fourX); tap(four.x, four.y - 72); await sleep(1000); // the slider track sits above its tick label
  shot(lang, '03_setup');
  await tapText(W.maxModel, 1500);
  await tapText(W.enhance, 3500);
  shot(lang, '04_working');
  await waitFor(W.result, 60000); await sleep(800);
  shot(lang, '05_result');
  sh('input keyevent KEYCODE_BACK'); await sleep(1200); sh('input keyevent KEYCODE_BACK'); await sleep(1500);
  tap(...GEAR); await sleep(1500);
  shot(lang, '06_settings');
  await tapText(W.manual, 1500);
  shot(lang, '07_manual');
  sh('input keyevent KEYCODE_BACK'); await sleep(1200);
  tap(...GEAR); await sleep(1500);
  shot(lang, '08_about');
}

const which = arg('lang', 'all');
const rotation = sh('settings get system accelerometer_rotation').trim();
const locales = sh(`cmd locale get-app-locales ${PKG}`);
try {
  adb('push', PHOTO_LOCAL, PHOTO_DEVICE);
  sh(`am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://${PHOTO_DEVICE} >/dev/null`);
  sh('settings put system accelerometer_rotation 0; settings put system user_rotation 0');
  sh('settings put global sysui_demo_allowed 1');
  const demo = (cmd) => sh(`am broadcast -a com.android.systemui.demo -e command ${cmd} >/dev/null`);
  demo('enter');
  demo('clock -e hhmm 1200'); demo('battery -e level 100 -e plugged false'); demo('network -e wifi show -e level 4 -e mobile hide');
  demo('notifications -e visible false');
  for (const lang of which === 'all' ? Object.keys(LANGS) : [which]) await walk(lang);
} finally {
  sh('am broadcast -a com.android.systemui.demo -e command exit >/dev/null');
  sh(`settings put system accelerometer_rotation ${rotation || 1}`);
  // Back to what the app had: an empty list = follow the system language.
  const had = (locales.match(/are \[([^\]]*)\]/) || [])[1] || '';
  sh(`cmd locale set-app-locales ${PKG} --locales "${had}"`);
}
