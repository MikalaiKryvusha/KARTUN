#!/usr/bin/env node
// build-ncnn.mjs - build KARTUN's engine library from OUR ncnn fork, MikalaiKryvusha/ncnn (owner 2026-10-16 ~19:1x:
// "СОБИРАЕМ НА НАШЕМ!"): ncnn 20260526 + the sdpa_cross bound check (researches/08 sections 11-14), android-vulkan,
// arm64-v8a only (the app ships nothing else), static - the same CMake options as Tencent's own release workflow
// (.github/workflows/release.yml, job "android", id android-vulkan).
// The source is pinned by tag AND by commit; the build is proved by its own content - the compiled sdpa_cross shader must
// carry the fix (EXP-0022: a build "with the change" is proved by the artifact, never by exit 0).
// Output: engine/third_party/ncnn-<TAG>-android-vulkan/arm64-v8a (engine/src/main/cpp/CMakeLists.txt links it).
// [TESTED: 2026-10-16 · cloned the fork tag from GitHub, built, the fix found in the compiled shader (stock 20260526: 0 hits, the tag: 4); the APK on it upscaled 256 -> 1024 on the Redmi (Mali-G68) and its .so carries the fixed shader]
// Usage: node tools/build-ncnn.mjs
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const REPO = 'https://github.com/MikalaiKryvusha/ncnn.git';
const TAG = '20260526-kartun.1';
const COMMIT = 'e26eef03966a28826362e81c775d8a1a26439580';
const ABI = 'arm64-v8a';
// The NDK and CMake of this machine's SDK (HOUSE_RULES.md section 4); the NDK is the one the app builds with.
const SDK = process.env.ANDROID_HOME || 'C:/Android/Sdk';
const NDK = join(SDK, 'ndk', '29.0.14206865');
const CMAKE_BIN = join(SDK, 'cmake', '3.31.6', 'bin');
const exe = (n) => join(CMAKE_BIN, process.platform === 'win32' ? `${n}.exe` : n);
// A line of the fixed shader: loads clamp the row index instead of reading past the buffer.
const FIX_MARK = 'min(gy4';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const tp = join(root, 'engine', 'third_party');
const src = join(tp, 'ncnn-src');
const out = join(tp, `ncnn-${TAG}-android-vulkan`, ABI);
const stamp = join(out, 'KARTUN_BUILD.txt');

function run(cmd, args, cwd) {
  console.log(`> ${cmd} ${args.join(' ')}`);
  const r = spawnSync(cmd, args, { cwd, stdio: 'inherit' });
  if (r.status !== 0) { console.error(`failed: ${cmd} (exit ${r.status})`); process.exit(1); }
}
const git = (args, cwd) => spawnSync('git', args, { cwd, encoding: 'utf8' }).stdout.trim();

if (existsSync(stamp) && readFileSync(stamp, 'utf8').includes(COMMIT)) {
  console.log(`ready: ${out} (${TAG})`);
  process.exit(0);
}

mkdirSync(tp, { recursive: true });
if (!existsSync(src)) run('git', ['clone', '--branch', TAG, '--depth', '1', REPO, src], tp);
else run('git', ['fetch', '--depth', '1', 'origin', 'tag', TAG], src), run('git', ['checkout', '--force', TAG], src);
const head = git(['rev-parse', 'HEAD'], src);
if (head !== COMMIT) { console.error(`commit mismatch: ${TAG} is ${head}, expected ${COMMIT}`); process.exit(1); }
run('git', ['submodule', 'update', '--init', '--depth', '1', 'glslang'], src);

const build = join(src, `build-${ABI}`);
mkdirSync(build, { recursive: true });
run(exe('cmake'), [
  '-G', 'Ninja', `-DCMAKE_MAKE_PROGRAM=${exe('ninja')}`,
  `-DCMAKE_TOOLCHAIN_FILE=${join(NDK, 'build', 'cmake', 'android.toolchain.cmake')}`,
  '-DANDROID_PLATFORM=android-21', '-DANDROID_USE_LEGACY_TOOLCHAIN_FILE=False', '-DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON',
  '-DCMAKE_BUILD_TYPE=Release', `-DCMAKE_INSTALL_PREFIX=${out}`, `-DNCNN_VERSION_STRING=${TAG}`,
  '-DNCNN_BUILD_BENCHMARK=OFF', '-DNCNN_VULKAN=ON', '-DNCNN_SHARED_LIB=OFF', `-DANDROID_ABI=${ABI}`, '..',
], build);
run(exe('cmake'), ['--build', '.'], build);
run(exe('cmake'), ['--build', '.', '--target', 'install/strip'], build);

// The proof: decode the compiled shader text that ncnn embeds and look for the fixed line.
const hex = readFileSync(join(build, 'src', 'layer', 'vulkan', 'shader', 'sdpa_cross.comp.hex.h'), 'utf8');
const shader = Buffer.from((hex.match(/0x([0-9a-fA-F]{2})/g) || []).map((x) => parseInt(x, 16))).toString('latin1');
if (!shader.includes(FIX_MARK)) { console.error(`the built sdpa_cross shader lacks the fix ("${FIX_MARK}")`); process.exit(1); }
writeFileSync(stamp, `${REPO} ${TAG} ${COMMIT}\n`);
console.log(`ready: ${out} (${TAG}, fix present)`);
