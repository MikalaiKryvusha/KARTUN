# Run report — 2026-10-16 · epic 06, phase A, step 9: the A1–A10 run

## Work
`plans/07_epic06_A_adb_api.md` step 9 against the case set `testcases/TC_epic06_A_agent_api.md` (A1–A10 of `plans/06` §2
plus U1, R1). The cases already observed in the step 7 and step 8 runs are referenced there; this report holds the runs
made for step 9 itself.

## Contour
HEADWOLF Titan 1 (Android 16, Mali-G615) at `100.99.111.48:5555`; the debug APK of the steps 4–6 commit; for A7 and R1 the
release build of `HEAD` (`assembleRelease` 12:36:50, R8 on, 62 263 169 bytes), signed with the PC's debug key into the
session scratchpad (`apksigner sign --ks ~/.android/debug.keystore`), installed ≈ 12:37 and uninstalled ≈ 12:38. The
moments of the runs below are ≈ (no clock probe per run; the probes at 12:35:27, 12:39:59 and 12:40:37 bound them).

## Runs
1. 12:39 — `magick compare -metric PSNR <cli>/test256_x4.png engine/third_party/testdata/ref_x4plus_256.png null:` (the A2
   output of the step 7 run, model `realesrgan-x4plus`).
2. 12:38:4x — `node tools/kartun-cli.mjs upscale engine/third_party/testdata/test1mp.png --model realesrgan-x4plus --scale 4`
   in the background; 30 s later `cancel <job>`, the time to `canceled` measured around the call.
3. 12:37–12:39 — `adb install <debug-signed release>`; `adb shell run-as com.kriniks.kartun.debug am start-foreground-service
   --user 0 -n com.kriniks.kartun/com.kriniks.kartun.agent.AgentService -a com.kriniks.kartun.agent.INFO`; `run-as … id`;
   the same start from the adb shell, `cat info.json`; `upscale test256.png --pkg com.kriniks.kartun --scale 4`, PSNR against
   the debug build's A4 output; `adb uninstall com.kriniks.kartun`.
4. 12:38–12:40 — U1 attempt: `am start -S -W -f 0x10008000 -n …/MainActivity`, the UI agent «Выберите фото» → KARTUN_test;
   `dumpsys window`, `dumpsys power`, `input keyevent 224`, `wm dismiss-keyguard`, a screenshot; `input keyevent 223`.

5. 13:05 — `.\gradlew.bat :probe:assembleDebug` (the new test-only module `probe/`); `adb install probe-debug.apk`;
   `am start -W -n com.kriniks.kartun.probe/.ProbeActivity --es target com.kriniks.kartun.debug`;
   `logcat -T … | grep -E 'A7PROBE|Permission Denial.*AgentService'`; `ls …/files/agent/`; `adb uninstall com.kriniks.kartun.probe`.

## Checks
- Hygiene: none new in this step.
- Functional run: the agent's path by the CLI and raw adb, read from stdout JSON, PSNR, `dumpsys`.

| # | Case | Status | Observed |
|---|---|---|---|
| 1 | A2 PSNR vs the reference | pass | `39.3434` dB (≥ 38) |
| 2 | A5 cancel at 30 s | pass | `"state":"canceled"`, `"tiles_done":10,"tiles_total":36`; 3.1 s from the `cancel` call to its answer; `upscale` exit 1 |
| 3 | A7 another app's uid | partial | `run-as` uid `10247(u0_a247)` (the debug app) → `Error: Not found; no service started.` — the release package is invisible to the debug app (Android package visibility), so the `DUMP` guard was not the refusing layer; control from the adb shell: `Starting service …`, `info.json` `"build":"release"` |
| 4 | R1 the R8 release build | pass | `"ok":true`, 1024 × 1024 in 5 348 ms on the GPU; PSNR to the debug build's output of the same model: `120 (1)` — identical |
| 7 | A7 a visible foreign caller | pass | `W ActivityManager: Permission Denial: Accessing service com.kriniks.kartun.debug/com.kriniks.kartun.agent.AgentService from pid=24267, uid=10302 requires android.permission.DUMP`; `W A7PROBE : refused: java.lang.SecurityException: Not allowed to start service … without permission android.permission.DUMP`; `files/agent/` empty; the probe uninstalled |
| 5 | U1 UI during an agent job — first attempt | blocked | focus `NotificationShade`, `isKeyguardShowing=true` after waking; `wm dismiss-keyguard` did not dismiss it; the screenshot is black (a secure lock screen) |
| 6 | U1 — after the owner unlocked the Titan («я разлочил титан», ≈ 12:42) | pass | 12:43:44 agent job x4plus on `test1mp.png`; 12:43:49 `titan-tap 848 2326` («Улучшить», Siax 8× on beach); at +3 s the Working screen «0% · проход 1/2 · тайл — · прошло 3 с»; 12:44:09 `cancel` → `canceled`; at 12:44:2x «30% · проход 2/2 · тайл 4/13»; 12:45 result «256 × 256 → 2048 × 2048 · 51 секунда · GPU · Vulkan»; `engine-stderr.log`: `12-43-45 load realesrgan-x4plus … thread=kartun-engine`, `12-44-10 load 4x-NMKD-Siax-fp16 … thread=kartun-engine`; `crashes/` 7 entries as before; `files/agent/` empty |

## Found
1. **The agent locked the owner's tablet.** A4 of the step 7 run (12:29) put the screen to sleep (`input keyevent 223`) to
   test «without the screen»; the wake-up (`224`) brought back a SECURE lock screen — before that the Titan was unlocked on
   the launcher. The U1 attempt then sent one UI-agent tap and one `input tap 266 1232` onto that lock screen (12:38) before
   the state was read. The tablet is left asleep and locked; the owner unlocks it. Lesson in EXPERIENCE (EXP-0030).
2. A7 cannot be proven against `DUMP` with apps that do not see each other: package visibility refuses first. A caller that
   CAN see KARTUN (a probe APK with `<queries><package android:name="com.kriniks.kartun.debug"/></queries>`) is the next
   step for a full A7.
4. While the app's run waits for an agent job, the Working screen shows «0%», tile «—» and a running «прошло» with no word
   of why; the wait also goes into the result's time («51 секунда» for a run of ≈ 34 s). Whether the screen should say
   «ждёт задание агента» is the owner's call (the taste class) — raised in STATUS, not changed.
5. Owner's word 2026-10-16 ≈ 12:42: «выключи в нем автолок» · «или поставь на 30 минут» — `settings put secure
   lock_screen_lock_after_timeout 1800000`, read back `1800000`; `screen_off_timeout` was already `2147483000` (never).
   Whether «lock instantly with the power button» still locks at once on a sleep key — not tried on the owner's device.
3. The R8 release build runs the agent door and the engine (the `STATUS` item «прогон сжатой сборки на Титане» — done for
   one 4× job).

## Traces
`<scratchpad>/cli/a5b.out`, `a5b.err`, `release_x4.png`, `lock.webp`; the quoted lines above. Device: the agent folder
empty, the release build uninstalled, the screen asleep.

## Verdict
partial — A1–A9, U1, R1 pass (with the step 7/8 runs; A7 by the probe app at 13:05); A10 blocked (waits for the USB cable).
