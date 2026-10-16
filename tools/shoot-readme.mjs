#!/usr/bin/env node
// shoot-readme.mjs - walks KARTUN on the Titan along the user's path and saves the README screenshots:
// home -> «Открыть с помощью → KARTUN» with the beach photo -> «Улучшение» -> model and scale (4x) -> processing ->
// before/after -> settings -> guide -> about.
// One set per language: docs/screenshots/<lang>/NN_<screen>.webp (540 px wide, ImageMagick `magick`).
// The status bar is cleaned by Android's System UI demo mode; the rotation lock and the app language are put back after.
// The photo is the Real-ESRGAN sample `input2.jpg` (BSD-3, engine/third_party/testdata/test256.png), pushed as beach.png.
// Nothing is tapped by its place on the screen (the owner's rule R7, HOUSE_RULES.md): the photo comes in by ACTION_VIEW with
// its MediaStore id (a grid tap by place twice opened the owner's own photos, 2026-10-16), every button is found by its text
// or its content description in the uiautomator tree; the only gesture is a scroll of «Улучшение».
// The release package by default (the debug build shows its «Отладка» section in the settings). [NOT-TESTED: rewritten
// 2026-10-16 23:4x for the 1.0 README; the 2026-10-14 version was TESTED on the Titan]
//
// Usage: node tools/shoot-readme.mjs [--lang ru|en|all]
// Env: KARTUN_ADB (default C:/Android/Sdk/platform-tools/adb.exe), KARTUN_SERIAL (e.g. 192.168.1.3:5555),
//      KARTUN_PKG (default com.kriniks.kartun).
import { spawnSync } from 'node:child_process';
import { mkdirSync, rmSync } from 'node:fs';
import { join } from 'node:path';

const ADB = process.env.KARTUN_ADB || 'C:/Android/Sdk/platform-tools/adb.exe';
const PKG = process.env.KARTUN_PKG || 'com.kriniks.kartun';
const ACTIVITY = `${PKG}/com.kriniks.kartun.MainActivity`;
const OUT = 'docs/screenshots';
const WIDTH = 540;
const PHOTO_LOCAL = 'engine/third_party/testdata/test256.png';
const PHOTO_DEVICE = '/sdcard/Pictures/KARTUN_test/beach.png';
const LANGS = { ru: 'ru-RU', en: 'en-US' };

const arg = (k, d) => { const i = process.argv.indexOf(`--${k}`); return i > 0 ? process.argv[i + 1] : d; };
const serial = process.env.KARTUN_SERIAL ? ['-s', process.env.KARTUN_SERIAL] : [];
const adb = (...a) => spawnSync(ADB, [...serial, ...a], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
const sh = (cmd) => adb('shell', cmd).stdout || '';
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const tap = (x, y) => sh(`input tap ${x} ${y}`);

/** Nodes of the current screen: [{text, x, y}] (centre of the bounds); `text` is the node's text, or its content
 *  description when it has no text (icon buttons: settings, back). */
function nodes() {
  sh('uiautomator dump /sdcard/kartun-ui.xml >/dev/null');
  const xml = sh('cat /sdcard/kartun-ui.xml');
  return [...xml.matchAll(/<node [^>]*>/g)].map((m) => {
    const a = (k) => (m[0].match(new RegExp(`${k}="([^"]*)"`)) || [])[1] || '';
    const b = a('bounds').match(/\[(\d+),(\d+)\]\[(\d+),(\d+)\]/);
    return b && { text: a('text') || a('content-desc'), x: (+b[1] + +b[3]) >> 1, y: (+b[2] + +b[4]) >> 1 };
  }).filter(Boolean);
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
  home: /^(Выберите фото|Choose a photo)$/,
  enhance: /^(Улучшить|Enhance) ·/, // the button carries «· ≈ time»; the English screen title is «Enhance» too
  allow: /^(Разрешить|Allow)$/, // the notification dialog's button; its title «Allow KARTUN to …» does not match
  result: /^(Сохранить|Save)$/,
  settings: /^(Настройки|Settings)$/, // the gear's content description on the home screen
  manual: /(Руководство|guide)/i,
  about: /^(О приложении|About)$/, // a settings row since 2026-10-14 22:00 (plan 03 step 39)
};

/** «Открыть с помощью → KARTUN»: the photo by its MediaStore id, read permission granted - no picker, no grid. */
function openPhoto(id) {
  sh(`am start -W -a android.intent.action.VIEW -t image/png -d content://media/external/images/media/${id} --grant-read-uri-permission -n ${ACTIVITY}`);
}

async function walk(lang, photoId) {
  console.log(`${lang}:`);
  sh(`cmd locale set-app-locales ${PKG} --locales ${LANGS[lang]}`);
  sh(`am force-stop ${PKG}`); sh(`am start -n ${ACTIVITY}`);
  await waitFor(W.home, 15000); await sleep(800);
  shot(lang, '01_home');
  openPhoto(photoId);
  await waitFor(W.enhance, 15000); await sleep(1500);
  shot(lang, '02_enhance');
  sh('input swipe 800 1900 800 900 600'); await sleep(1500); // scroll to the model cards and the scale slider (4× by default)
  shot(lang, '03_setup');
  // The 256 px beach takes a few seconds at 4x on the Titan: the processing screen is shot right after it opens.
  await tapText(W.enhance, 900);
  const allow = find(W.allow);
  if (allow) { tap(allow.x, allow.y); await sleep(900); }
  shot(lang, '04_working');
  await waitFor(W.result, 180000); await sleep(1200);
  shot(lang, '05_result');
  sh('rm -f /sdcard/kartun-ui.xml');
  sh('input keyevent KEYCODE_BACK'); await sleep(1200); sh('input keyevent KEYCODE_BACK'); await sleep(1500);
  await waitFor(W.home, 10000);
  await tapText(W.settings, 1500);
  shot(lang, '06_settings');
  await tapText(W.manual, 1500);
  shot(lang, '07_manual');
  sh('input keyevent KEYCODE_BACK'); await sleep(1200);
  await tapText(W.about, 1500);
  shot(lang, '08_about');
}

const which = arg('lang', 'all');
const rotation = sh('settings get system accelerometer_rotation').trim();
const locales = sh(`cmd locale get-app-locales ${PKG}`);
try {
  adb('push', PHOTO_LOCAL, PHOTO_DEVICE);
  sh(`am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://${PHOTO_DEVICE} >/dev/null`);
  await sleep(1500);
  const row = sh(`content query --uri content://media/external/images/media --projection _id --where "_data='${PHOTO_DEVICE}'"`);
  const photoId = (row.match(/_id=(\d+)/) || [])[1];
  if (!photoId) throw new Error(`the pushed photo is not in MediaStore: ${row.trim()}`);
  sh('settings put system accelerometer_rotation 0; settings put system user_rotation 0');
  sh('settings put global sysui_demo_allowed 1');
  const demo = (cmd) => sh(`am broadcast -a com.android.systemui.demo -e command ${cmd} >/dev/null`);
  demo('enter');
  demo('clock -e hhmm 1200'); demo('battery -e level 100 -e plugged false'); demo('network -e wifi show -e level 4 -e mobile hide');
  demo('notifications -e visible false');
  for (const lang of which === 'all' ? Object.keys(LANGS) : [which]) await walk(lang, photoId);
} finally {
  // R1: the test photo leaves the Titan with the run.
  sh(`rm -f ${PHOTO_DEVICE}; rmdir /sdcard/Pictures/KARTUN_test 2>/dev/null`);
  sh(`am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://${PHOTO_DEVICE} >/dev/null`);
  sh('am broadcast -a com.android.systemui.demo -e command exit >/dev/null');
  sh(`settings put system accelerometer_rotation ${rotation || 1}`);
  // Back to what the app had: an empty list = follow the system language.
  const had = (locales.match(/are \[([^\]]*)\]/) || [])[1] || '';
  sh(`cmd locale set-app-locales ${PKG} --locales "${had}"`);
}
