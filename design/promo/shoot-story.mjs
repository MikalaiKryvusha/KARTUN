// shoot-story.mjs - renders design/promo/story-1.0.html into Instagram story PNGs (1080 × 1920), Russian and English.
// Usage (from the project root): node design/promo/shoot-story.mjs  ->  design/promo/story-1.0-ru.png, story-1.0-en.png
import { chromium } from 'playwright';
import { pathToFileURL } from 'node:url';
import { join } from 'node:path';

const page_ = pathToFileURL(join(process.cwd(), 'design', 'promo', 'story-1.0.html')).href;
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1080, height: 1920 }, deviceScaleFactor: 1 });
for (const lang of ['ru', 'en']) {
  await page.goto(`${page_}?lang=${lang}`);
  await page.evaluate(() => document.fonts.ready);
  const out = join('design', 'promo', `story-1.0-${lang}.png`);
  await page.screenshot({ path: out });
  console.log(out);
}
await browser.close();
