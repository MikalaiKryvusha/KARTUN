# TC epic06 A — the agent API over adb (criteria A1–A10 of `plans/06` §2)

**Basis:** `plans/06_EPIC_agent_api.md` §2 (A1–A10, the owner's interview #005 answers behind them), the protocol block of
`plans/07_epic06_A_adb_api.md`, `docs/agent-api.md`. **Stand:** HEADWOLF Titan 1, debug build (A7: plus the release build,
debug-signed for the run). **Dimensions:** commands (info · models · upscale · status · cancel), scales 1/2/4/8, formats
png/jpg/webp, backend GPU/CPU, app state (closed · screen off), errors (request · model · input · device · adb), caller
(adb shell · another app), network (airplane mode). **Left out:** 16× (the 500 MP output limit makes it a big-file run),
the queue cap of 8, `quota` (6 h per day), the 24 h sweep — by risk, they wait for a long run.

| # | Case | Command / steps | Expected | Status | Run |
|---|---|---|---|---|---|
| A1 | info | `node tools/kartun-cli.mjs info` | one JSON line, `"ok":true`, version, GPU, models | pass | 2026-10-16_epic06_A_step7_cli.md #1 |
| A2 | upscale x4plus 4× | `upscale engine/third_party/testdata/test256.png --model realesrgan-x4plus --scale 4`; `magick compare -metric PSNR <out> engine/third_party/testdata/ref_x4plus_256.png null:` | 1024 × 1024 PNG, PSNR ≥ 38 dB | pass — 39.34 dB | step7 #2; 2026-10-16_epic06_A_agent_api.md #1 |
| A3 | scales × formats | `upscale test256.png --scale <1\|2\|4\|8> --format <png\|jpg\|webp>` | sizes ×1…×8, formats by `magick identify` | pass | step7 #3 |
| A4 | without the screen | `am force-stop`, `input keyevent 223`, `upscale …`; focus/wakefulness before and after | job succeeds, the screen stays off | pass (app closed + screen off); the «owner's other app on top» variant not run — no owner app is opened by the agent | step7 #4 |
| A5 | cancel | `upscale test1mp.png --model realesrgan-x4plus --scale 4` in the background; at 30 s `cancel <job>` | `canceled` ≤ 5 s after the cancel | pass — 3.1 s, tile 10/36 | agent_api #2 |
| A6 | errors | `--model no-such-model`; `--serial 10.255.255.1:5555`; `--scale 3`; `KARTUN_ADB=C:/nope/adb.exe` | `model_not_installed` exit 1 · `no_device` exit 3 · `bad_arguments` exit 2 · `adb_missing` exit 3 | pass | step7 #6 |
| A7 | only adb | another app's uid starts the door: `run-as com.kriniks.kartun.debug am start-foreground-service --user 0 -n com.kriniks.kartun/…AgentService -a …INFO`; control from the adb shell | refused; the adb shell works | partial — refused with «Error: Not found; no service started» (package visibility: the caller cannot see the package), the `DUMP` check behind it not reached; the control answered | agent_api #3 |
| A8 | the doc | every command of `docs/agent-api.md` run as written | each works | pass | 2026-10-16_epic06_A_step8_api_doc.md |
| A9 | the device clean | `ls …/files/agent/` after `upscale` | empty (or `info.json` only) | pass — empty | step7 #7 |
| A10 | offline | A2 with `cmd connectivity airplane-mode enable` … `disable` | succeeds | blocked — adb reaches the Titan over Wi-Fi/Tailscale only; airplane mode cuts the run itself; needs the USB cable | — |
| U1 | UI during an agent job | an agent job running, «Улучшить» in the app | the UI job waits on the one engine thread, then finishes; no crash | pass — after the owner unlocked the Titan (12:42): the UI run sat at 0 % / tile «—» while the agent job ran, went on after its cancel, finished 256 × 256 → 2048 × 2048 in 51 s; both loads on `kartun-engine`; no new crash | agent_api #4, #6 |
| R1 | the release build (R8) | the release APK (debug-signed for the run) — `INFO`, `upscale --pkg com.kriniks.kartun` | the door survives R8; the result equals the debug build's | pass — `"build":"release"`; PSNR 120 (identical) to the debug build's run of the same model | agent_api #3 |
