// check-app-names.mjs - guards the pair «HOUSE_RULES.md rule R3 ↔ app/build.gradle.kts»: the app names on the phone are
// the owner's (R3: «Stage KARTUN», «KARTUN»; debug «Debug KARTUN» [AI]) and live in the build script as resValue lines.
// Exit 1 on drift.
// @guard app-names
// THREAT:         a build type renamed or a name edited in build.gradle.kts, so the phone shows a name the owner did not give
// PROVED-AGAINST: «Stage KARTUN» changed to «Staging KARTUN» in a copy of the script's input -> exit 1 (2026-10-14 ≈ 19:49)
// GAP:            reads the build script, not the built APK (aapt2 badging of each APK is the device-side check)
// ON-REAL-PATH:   NOT YET (not wired into a pre-commit or release step)
import { readFileSync } from 'node:fs';

const file = process.argv[2] ?? 'app/build.gradle.kts';
const text = readFileSync(file, 'utf8');
const want = { debug: 'Debug KARTUN', stage: 'Stage KARTUN', release: 'KARTUN' };
let bad = 0;
for (const [type, name] of Object.entries(want)) {
  // the block of this build type up to its resValue line
  const re = new RegExp(`(?:${type} \\{|create\\("${type}"\\) \\{)[\\s\\S]*?resValue\\("string", "app_name", "([^"]+)"\\)`);
  const got = text.match(re)?.[1];
  if (got !== name) { console.error(`DRIFT ${type}: «${got}» in ${file}, R3 wants «${name}»`); bad++; }
}
if (!bad) console.log(`app names match R3: ${Object.values(want).join(' · ')}`);
process.exit(bad ? 1 : 0);
