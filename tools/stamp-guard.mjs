#!/usr/bin/env node
// stamp-guard.mjs - refuses moments written "by feel" that lie in the future: any "YYYY-MM-DD HH:MM" (also "≈ ..." and
// "YYYY-MM-DDTHH:MM") in the given files later than the current local time (+1 min tolerance) is a finding, exit 1.
// Born from EXP-0004 (two moments typed ahead of the clock in one session, 2026-10-14). [TESTED: 2026-10-14 · mutant
// file with a future stamp -> exit 1; the repo's staged docs -> exit 0; see EXPERIENCE EXP-0005]
//
// @guard stamp-guard
// THREAT:         a moment typed by feel ahead of the clock lands in a pushed document
// PROVED-AGAINST: a scratch file with a stamp one hour ahead of the probe -> exit 1
// GAP:            a past moment typed by feel (earlier than the truth) is not caught; dates without time are ignored
// ON-REAL-PATH:   git pre-commit hook of this clone (.git/hooks/pre-commit) runs it on the staged files
//
// Usage: node tools/stamp-guard.mjs <file...>        or   node tools/stamp-guard.mjs --staged
import { readFileSync, existsSync } from 'node:fs';
import { execFileSync } from 'node:child_process';

const TOLERANCE_MS = 60 * 1000;
const RE = /(\d{4})-(\d{2})-(\d{2})[ T](?:≈\s*)?(\d{2}):(\d{2})/g;

let files = process.argv.slice(2);
if (files[0] === '--staged') {
  files = execFileSync('git', ['diff', '--cached', '--name-only', '--diff-filter=ACM'], { encoding: 'utf8' })
    .split('\n').filter((f) => /\.(md|json|mjs|html)$/.test(f) && existsSync(f));
}
const now = Date.now();
let bad = 0;
for (const f of files) {
  const lines = readFileSync(f, 'utf8').split('\n');
  lines.forEach((line, i) => {
    for (const m of line.matchAll(RE)) {
      const t = new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5]).getTime();
      if (t - now > TOLERANCE_MS) {
        bad++;
        console.error(`✖ ${f}:${i + 1} — moment ${m[0]} is in the future (now ${new Date(now).toLocaleString('sv-SE').slice(0, 16)}); probe the clock: date '+%Y-%m-%d %H:%M %z'`);
      }
    }
  });
}
if (bad) process.exit(1);
console.log(`✅ stamp-guard OK — ${files.length} file(s), no future moments`);
