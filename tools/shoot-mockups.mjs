#!/usr/bin/env node
// shoot-mockups.mjs - shoots every screen of design/kartun-mockups.html into a 390x900 PNG for the KUIPS sheet
// (design/flow-map/flow-map.json references these files). One run = one moment, so the sheet never mixes shoots.
// Also shoots the font row: the "A-setup" screen in each candidate font. [NOT-TESTED]
//
// Usage: node tools/shoot-mockups.mjs            -> design/flow-map/screens/<id>.png
import { chromium } from 'playwright';
import { mkdirSync } from 'node:fs';
import { resolve, join } from 'node:path';
import { pathToFileURL } from 'node:url';

const PAGE = pathToFileURL(resolve('design/kartun-mockups.html')).href;
const OUT = 'design/flow-map/screens';
const W = 390;
const H = 900;
const SCREENS = ['A-home', 'A-setup', 'A-setup-scrolled', 'info-scale', 'processing', 'result', 'save', 'settings', 'models', 'manual', 'manual-models', 'B-steps', 'C-home', 'C-setup', 'D-home', 'D-setup'];
const FONTS = ['manrope', 'nunito', 'unbounded', 'alternates'];
const FONT_SCREEN = 'A-setup';

mkdirSync(OUT, { recursive: true });
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: W, height: H }, deviceScaleFactor: 1 });

async function shoot(id, font, file) {
  await page.goto(`${PAGE}?shot=${id}&font=${font}`, { waitUntil: 'networkidle' });
  await page.evaluate(() => document.fonts.ready);
  const el = await page.$(`.phone-wrap[data-id="${id}"] .screen`);
  if (!el) throw new Error(`screen not found: ${id}`);
  await el.screenshot({ path: file });
  console.log(`shot ${file}`);
}

for (const id of SCREENS) await shoot(id, 'manrope', join(OUT, `${id}.png`));
for (const f of FONTS) await shoot(FONT_SCREEN, f, join(OUT, `font-${f}.png`));
await browser.close();
