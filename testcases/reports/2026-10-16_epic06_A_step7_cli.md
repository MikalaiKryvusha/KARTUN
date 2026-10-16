# Run report — 2026-10-16 · epic 06, phase A, step 7: the PC CLI `tools/kartun-cli.mjs`

## Work
`plans/07_epic06_A_adb_api.md` step 7 — `node tools/kartun-cli.mjs` (`info`, `models`, `upscale`, `status`, `cancel`) over
the agent service of steps 4–6; judged by the criteria A1–A6 and A9 of `plans/06` §2, written there as CLI commands.

## Contour
HEADWOLF Titan 1 (Android 16, Mali-G615) at `100.99.111.48:5555` (`KARTUN_SERIAL`); the debug APK of the steps 4–6 commit
installed 12:21; the PC: Windows 11, Node v24.15.0, Git Bash; adb `C:/Android/Sdk/platform-tools/adb.exe` (the CLI's
default); inputs `engine/third_party/testdata/test256.png` (256 × 256), `hathaway.jpg` (51 MP, for the cancel only);
outputs into the session scratchpad `cli/`.

## Runs
1. 12:26 — `node tools/kartun-cli.mjs --help`; `node tools/kartun-cli.mjs info`.
2. 12:27 — `node tools/kartun-cli.mjs upscale engine/third_party/testdata/test256.png --model realesrgan-x4plus --scale 4 --out <cli>/`; `magick identify`.
3. 12:27–12:28 — six runs `upscale test256.png --scale <s> --format <f> --out <cli>/a3_x<s>.<f>` for 1 png, 2 jpg, 4 webp, 8 png,
   8 jpg, 8 webp; `magick identify` each.
4. 12:29 — `adb shell am force-stop com.kriniks.kartun.debug`, `input keyevent 223` (`mWakefulness=Asleep`), `upscale test256.png`,
   `mWakefulness` again, `ls -la …/files/agent/`, `input keyevent 224`.
5. 12:29:36 — `upscale hathaway.jpg --scale 2 --format jpg` in the background, its job id read from stderr; 12:29:42 `cancel <job>`.
6. 12:30 — `upscale test256.png --model no-such-model`; `info --serial 10.255.255.1:5555`; `upscale x.png --scale 3`;
   `KARTUN_ADB=C:/nope/adb.exe … info`; `models`; `ls -la …/files/agent/`.

## Checks
- Hygiene: `node tools/kartun-cli.mjs --help` prints the usage, exit 0.
- Functional run: the agent's path — the CLI on the PC, KARTUN closed — read from stdout JSON, exit codes, the pulled files
  (`magick identify`), the device folder and `dumpsys power`.

| # | Criterion | Status | Observed |
|---|---|---|---|
| 1 | A1 info | pass | one JSON line, `"ok":true`, `app {"package":"com.kriniks.kartun.debug","version":"0.1 (1)"}`, 14 models |
| 2 | A2 upscale x4plus 4× | pass | `"state":"succeeded"`, `"model":"realesrgan-x4plus"`, 1024 × 1024, `elapsed_ms 8741`; file `PNG 1024x1024`; exit 0; stderr progress 0 % → 25 % → 50 % → 100 % (A4's progress lines) |
| 3 | A3 scales × formats | pass | 1× png `PNG 256x256` · 2× jpg `JPEG 512x512` · 4× webp `WEBP 1024x1024` · 8× png `PNG 2048x2048` · 8× jpg `JPEG 2048x2048` · 8× webp `WEBP 2048x2048` |
| 4 | A4 without the screen | pass | app force-stopped, `mWakefulness=Asleep` before and after; `"ok":true`, exit 0 |
| 5 | A5 cancel | pass | `cancel` → `"state":"canceled"` at tile 2 / 1353, exit 0; the waiting `upscale` ended `"ok":false` with exit 1 at 12:29:43.6, 1.5 s after the cancel |
| 6 | A6 errors | pass | no model → `model_not_installed`, exit 1; no device → `no_device`, exit 3; scale 3 → `bad_arguments`, exit 2; no adb → `adb_missing`, exit 3 |
| 7 | A9 the device clean after `upscale` | pass | `files/agent/` holds nothing of the jobs; `info.json` of `info` was left at 12:27 — fixed in the same step (the CLI deletes it after reading), `models` at 12:30 left the folder empty |

Not covered: A7 (step 9, a foreign-uid caller), A8 (step 8, the doc), A10 (airplane mode: adb reaches the Titan over Wi-Fi
and Tailscale only — airplane mode would cut the run itself; needs the cable), Ctrl+C during `upscale` (Git Bash cannot
send SIGINT to a native Node process; to try from a Windows console by hand).

## Found
1. `info` left `info.json` on the device (R1) — fixed in this step and re-run (case 7).
2. Argument errors were reported after the device check (`upscale x.png --scale 3` first ran `adb get-state`). Fixed 13:04
   the same day (`connect()` after the checks): `upscale x.png --scale 3 --serial 10.255.255.1:5555` → `bad_arguments`,
   exit 2; `info`, `status nosuchjob1` (exit 1), `upscale test256.png --scale 2` (exit 0) re-run on the Titan.
3. none else.

## Traces
Outputs in the session scratchpad `cli/` (`test256_x4.png`, `a3_x*.{png,jpg,webp}`, `a4.png`, `a5.out`, `a5.err`); the
stdout JSON lines quoted above.

## Verdict
partial — A1–A6 and A9 pass on the Titan through the CLI; A7, A8, A10 and Ctrl+C remain (steps 8–9, the cable).
