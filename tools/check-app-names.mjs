// check-app-names.mjs - guards the pair «HOUSE_RULES.md rule R3 ↔ app/build.gradle.kts»: the app has exactly two builds,
// named as the owner said (R3: debug «Debug KARTUN», prod «KARTUN»; the stage build was dropped by the owner 2026-10-15),
// and the names live in the build script as resValue lines. Exit 1 on drift or when a stage build type is back.
// @guard app-names
// THREAT:         a build type renamed, a name edited, or the dropped stage build re-added in build.gradle.kts
// PROVED-AGAINST: «Stage KARTUN» changed to «Staging KARTUN» -> exit 1 (2026-10-14 19:48 +03:00); a copy of the script's
//                 input with create("stage") still in it -> exit 1 (2026-10-15)
// GAP:            reads the build script, not the built APK (aapt2 badging of each APK is the device-side check)
// ON-REAL-PATH:   .git/hooks/pre-commit of this clone (wired 2026-10-14 19:48 +03:00), seen passing on the commit that wired it
import { readFileSync } from 'node:fs';

const file = process.argv[2] ?? 'app/build.gradle.kts';
const text = readFileSync(file, 'utf8');
const want = { debug: 'Debug KARTUN', release: 'KARTUN' };
let bad = 0;
for (const [type, name] of Object.entries(want)) {
  // the block of this build type up to its resValue line
  const re = new RegExp(`(?:${type} \\{|create\\("${type}"\\) \\{)[\\s\\S]*?resValue\\("string", "app_name", "([^"]+)"\\)`);
  const got = text.match(re)?.[1];
  if (got !== name) { console.error(`DRIFT ${type}: «${got}» in ${file}, R3 wants «${name}»`); bad++; }
}
if (/create\("stage"\)|getByName\("stage"\)/.test(text)) { console.error(`DRIFT stage: ${file} has a stage build again, R3 keeps two`); bad++; }
if (!bad) console.log(`app names match R3: ${Object.values(want).join(' · ')}`);
process.exit(bad ? 1 : 0);
