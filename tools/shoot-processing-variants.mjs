#!/usr/bin/env node
// shoot-processing-variants.mjs - shoots design/processing-variants.html (the processing screen now + compact variants A-C)
// into one WebP sheet for the owner (HOUSE_RULES R4: WebP). [NOT-TESTED]
//
// Usage: node tools/shoot-processing-variants.mjs [set] [out.webp]   set 1 (now, A, B, C) or 2 (B, B1, B2)
//        -> default design/processing-variants[-<set>].webp (outside git)
import { chromium } from 'playwright';
import { execFileSync } from 'node:child_process';
import { resolve } from 'node:path';
import { pathToFileURL } from 'node:url';

const set = process.argv[2] || '1';
const out = process.argv[3] || `design/processing-variants${set === '1' ? '' : '-' + set}.webp`;
const png = out.replace(/\.webp$/, '.png');
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 2420, height: 1000 }, deviceScaleFactor: 1 });
await page.goto(pathToFileURL(resolve('design/processing-variants.html')).href + `?set=${set}`, { waitUntil: 'networkidle' });
await page.evaluate(() => document.fonts.ready);
await (await page.$('.sheet')).screenshot({ path: png });
await browser.close();
execFileSync('magick', [png, '-quality', '88', out]);
console.log(out);
