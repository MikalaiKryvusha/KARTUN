#!/usr/bin/env node
// ctrl-char-guard.mjs - refuses text files that carry an ASCII control character (anything below 0x20 except tab, LF,
// CR, and DEL): the visible trace of a Windows path typed into a Python or JS string literal, where "\a", "\b", "\f",
// "\v" or "\t" in "C:\Android\...\adb.exe" silently become control bytes (EXPERIENCE EXP-0003, EXP-0017).
//
// @guard ctrl-char-guard
// THREAT:         a scripted edit of a document writes a path whose backslash escape turned into a control byte, and it
//                 is committed (2026-10-15: «platform-tools\adb.exe» became «platform-tools<BEL>db.exe» in STATUS.md)
// PROVED-AGAINST: a scratch .md with a BEL byte -> exit 1; the same file without it -> exit 0 (2026-10-15)
// GAP:            a "\t" that became a real tab is allowed (tabs are legal text); escapes that map to printable
//                 characters are not seen
// ON-REAL-PATH:   .git/hooks/pre-commit of this clone runs it on the staged files (wired 2026-10-15)
//
// Usage: node tools/ctrl-char-guard.mjs <file...>   or   node tools/ctrl-char-guard.mjs --staged
import { readFileSync, existsSync } from 'node:fs';
import { execFileSync } from 'node:child_process';

let files = process.argv.slice(2);
if (files[0] === '--staged') {
  files = execFileSync('git', ['diff', '--cached', '--name-only', '--diff-filter=ACM'], { encoding: 'utf8' })
    .split('\n').filter((f) => /\.(md|json|mjs|js|py|kt|kts|xml|cpp|h|html|txt)$/.test(f) && existsSync(f));
}
let bad = 0;
for (const f of files) {
  const lines = readFileSync(f, 'utf8').split('\n');
  lines.forEach((line, i) => {
    // eslint-disable-next-line no-control-regex
    const m = line.match(/[\x00-\x08\x0B\x0C\x0E-\x1F\x7F]/);
    if (m) { console.error(`CONTROL 0x${m[0].charCodeAt(0).toString(16).padStart(2, '0')} ${f}:${i + 1}: ${JSON.stringify(line.slice(0, 120))}`); bad++; }
  });
}
if (bad) { console.error(`ctrl-char-guard: ${bad} line(s) with a control character - an escaped Windows path? (EXP-0017)`); process.exit(1); }
console.log(`ctrl-char-guard OK — ${files.length} file(s)`);
