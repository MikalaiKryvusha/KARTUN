# Test run report — release 1.0 RC smoke on the Redmi Note 12 Pro+ (Android 14, Mali-G68)

**Created:** 2026-10-16 20:59 +03:00 · **Run by:** the agent (Claude Opus 5.5, subagent) · **Version/build:**
`C:\Users\krinik\AppData\Local\Temp\rc\kartun-rc-2028.apk` (62 936 993 bytes, 20:35), release `com.kriniks.kartun`
`0.1 (1)`, versionCode 1, minSdk 34, targetSdk 37; engine MikalaiKryvusha/ncnn `20260526-kartun.1`; first-launch bench
with REF 1130 / 966 ms

## 1. Work

The release 1.0 RC smoke, case set `testcases/TC_release_1_0_rc_smoke.md` (C1–C13), parent `plans/08_release_1_0.md` step 4.
One device: this report. C4–C9 had passed on the same device earlier today on an older build
(`testcases/reports/2026-10-16_release_1_0_redmi.md`). They were run again on this build. The new things in this build are
the first-launch GPU/CPU bench (commit `3d51295`, ideas/04) and its effect on the time estimate.

## 2. Contour

Xiaomi Redmi Note 12 Pro+ 5G (`22101316UG`), Android 14 (API 34), Dimensity 1080, Mali-G68 MC4. adb over home Wi-Fi
`192.168.1.76:41705`, system language English, battery 95 → 91 %, not charging. UI driven by locators with the UI agent
(`C:\Android\private\ui-agent\ui.py`, port 8382, agent started by `am instrument … AutomationServer#serve`). Test photo
`engine/third_party/testdata/photo1.jpg` stripped of EXIF (`magick photo1.jpg -strip kartun_smoke.jpg`, 960 × 1280),
pushed to `/sdcard/Pictures/KARTUN_test/kartun_smoke.jpg` and scanned. In the picker it is «Photo taken on Oct 16, 2026
8:37 PM»: adb push kept the local file's mtime, 20:37:11, and the device `stat` confirmed it. `Pictures/KARTUN/` was empty
before the run. Original device settings: `accelerometer_rotation=1`, `user_rotation=0`, app locales `[]`.
REAL WORLD: release build installed with adb, not from a store. Otherwise the same as a user's phone.

## 3. Runs

All commands used `C:/Android/Sdk/platform-tools/adb.exe -s 192.168.1.76:41705` (shown below as `adb`). UI commands used
`python C:/Android/private/ui-agent/ui.py --serial 192.168.1.76:41705 --port 8382 "<cmd>"` (shown below as `ui "<cmd>"`).

| # | Moment | Command | Exit / outcome |
|---|---|---|---|
| 1 | 2026-10-16 20:36 +03:00 | `adb install -r C:/Users/krinik/AppData/Local/Temp/rc/kartun-rc-2028.apk`; `adb shell dumpsys package com.kriniks.kartun` | `Success`; versionCode=1 minSdk=34 targetSdk=37, lastUpdateTime 20:37:04 |
| 2 | 2026-10-16 20:37 +03:00 | `adb shell pm clear com.kriniks.kartun`; `adb logcat -c`; `adb shell am start -n com.kriniks.kartun/.MainActivity`; screencap at +2 s; `ui "waitGone Measuring 60000"` | bench dialog, then home; `{"bench":true,"gpu_ms":2750.0,"cpu_ms":1119.0,…}` |
| 3 | 2026-10-16 20:38 +03:00 | the same as run 2, then three screencaps in a row to check the spinner | spinner frames differ; `gpu_ms 2628, cpu_ms 1126` |
| 4 | 2026-10-16 20:38 +03:00 | `pm clear`; `logcat -c`; `logcat -b crash -c`; `am start …`; `ui "wait Cancel 5000"`; `ui "tapText Cancel"`; relaunch (`am force-stop` + `am start`) | dialog gone, home; relaunch has no dialog; crash buffer 0 lines |
| 5 | 2026-10-16 20:39 +03:00 | as run 4, but `wait Cancel` → `tapText Cancel` → `labels` sent through one client connection (stdin) | the next `labels` read showed home already; relaunch has no dialog; no `bench` line; crash 0 |
| 6 | 2026-10-16 20:39 +03:00 | `adb push …/kartun_smoke.jpg /sdcard/Pictures/KARTUN_test/kartun_smoke.jpg`; `am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/KARTUN_test/kartun_smoke.jpg` | MediaStore row `kartun_smoke.jpg 960×1280` |
| 7 | 2026-10-16 20:40 +03:00 | extra check, bench still cancelled: `ui "tapText Choose a photo"` → `ui "tapText Photo taken on Oct 16, 2026 8:37 PM"` → `ui "tapText Done"` → `labels` | estimate without bench data: «Enhance · ≈ 1 minute 20 seconds» |
| 8 | 2026-10-16 20:40 +03:00 | `pm clear`; `logcat -c`; `logcat -b crash -c`; `am start …`; `ui "waitGone Measuring 60000"`. This bench run stays in effect for C4 | `gpu_ms 2682, cpu_ms 1111, gpu_factor 2.373, cpu_factor 1.150` |
| 9 | 2026-10-16 20:41–20:45 +03:00 | C4: picker → photo → `Done` → `labels` → `ui "tapText Enhance · ≈"` → `ui tree` → `ui "tap 540 2022"` (node «Allow») → wait | estimate ≈ 3 min 9 s; real **4 min 19 s**, GPU · Vulkan; 960 × 1280 → 3840 × 5120 |
| 10 | 2026-10-16 20:45–20:46 +03:00 | C5: `ui "tapText Save"` → `ui "tapText PNG|JPEG|WEBP"` → `ui tree` → `ui "tap 585 1598"` / `ui "tap 585 1725"` (node of the dialog's «Save»), three times; then `adb shell "content query --uri content://media/external/images/media --projection _display_name:width:height:_size --where \"relative_path LIKE 'Pictures/KARTUN/%'\""` | three rows, 3840 × 5120 each |
| 11 | 2026-10-16 20:47 +03:00 | C6: `ui "tapText Share"` → `ui "waitGone Rendering… 60000"` → `labels` → `ui "tapText Cancel"` | sheet shows `1000009287_kartun_4xNomosWebPhoto-RealPLKSR-fp16_4x.png`; cancelled, back on the result |
| 12 | 2026-10-16 20:47–20:48 +03:00 | C7: `ui back` ×2 → `ui "tapText Paste a photo link"` → `ui "type https://raw.githubusercontent.com/MikalaiKryvusha/KARTUN/main/docs/model-previews/4x-NMKD-Siax-fp16.webp"` → `ui "tapText Download"` | Enhance: `4x-NMKD-Siax-fp16.webp`, 1024 × 512 → 4096 × 2048 |
| 13 | 2026-10-16 20:48 +03:00 | C8: `ui "tapText All models"` → `ui tree` → `ui "tap 812 1411"` (node «Download» of 4x UltraSharp V2 Lite); `$env:KARTUN_SERIAL='192.168.1.76:41705'; node D:\work\ai_sandbox\KARTUN\tools\kartun-cli.mjs info` | card shows «Delete»; cli `4x-UltraSharpV2-Lite-fp16 installed:true` |
| 14 | 2026-10-16 20:48–20:49 +03:00 | C9: `ui "tapText Settings"` → `ui "tapText Version, device, author, licences"` → `labels` | engine line and fork row present |
| 15 | 2026-10-16 20:49–20:50 +03:00 | C10: `adb shell cmd locale set-app-locales com.kriniks.kartun --locales ru`, then `--locales ar`; About and home in each; screenshots | RU correct; AR mirrored, one garbled value (see Found) |
| 16 | 2026-10-16 20:50–20:51 +03:00 | C11: `adb shell cmd locale set-app-locales com.kriniks.kartun --locales ""`; `adb shell settings put system accelerometer_rotation 0`; `adb shell settings put system user_rotation 1`; home, then picker → photo → Done → Enhance; restore `user_rotation 0`, `accelerometer_rotation 1` | two-pane layouts, nothing cut; settings read back 0 / 1 |
| 17 | 2026-10-16 20:51–20:53 +03:00 | C12: `ui "tapText Enhance · ≈"`; `labels` polled every ≈ 5 s; `ui "tapText Cancel"` at 37 % (tile 13/35) | back on Enhance after ≈ 6 s (the tile in flight, 14/35, finished first) |
| 18 | 2026-10-16 20:54–20:58 +03:00 | C12 second Enhance: `ui "tapText Enhance · ≈"`, `labels` polled every 25 s | result, **4 min 13 s** (estimate ≈ 3 min 44 s), 960 × 1280 → 3840 × 5120 |
| 19 | 2026-10-16 20:58 +03:00 | C13: `adb logcat -d -b crash` (buffer last cleared 20:40:28) | 0 lines in total, 0 for `com.kriniks.kartun` |
| 20 | 2026-10-16 20:59 +03:00 | cleanup: `adb shell "content delete --uri content://media/external/images/media --where \"relative_path LIKE 'Pictures/KARTUN/%' OR relative_path LIKE 'Pictures/KARTUN_test/%'\""`; `rmdir` both folders; locale `""`; `ui quit`; `adb forward --remove tcp:8382` | query `relative_path LIKE 'Pictures/KARTUN%' OR _display_name LIKE '%kartun%'` → «No result found»; locales `[]`; rotation 0 / 1 |

## 4. Checks

Hygiene: NONE. This was a device smoke of a prebuilt RC APK. No build, unit, lint or mutation was run in this pass.
Functional run: the release RC on the Redmi Note 12 Pro+ (Android 14) as a user, by the user's path. Covered: install,
first-launch bench, its Cancel, pick → Enhance → result, save in three formats, share, link, catalogue, About, RU/AR, landscape,
processing cancel, crash journal. Every step was read in the accessibility tree (`labels`/`tree`). I looked at the
screenshots of the key screens myself. Bench numbers and tile timings come from logcat. Saved files were checked with MediaStore
queries. Models were checked with `kartun-cli info`.

| Case | Status | Observation |
|---|---|---|
| C1 Install | pass | `Success`; `dumpsys`: versionCode=1, minSdk=34, targetSdk=37, versionName `0.1 (1)` |
| C2 First-launch bench | pass | At +2 s the home screen is dimmed behind a dialog: «Measuring the device's GPU and CPU performance…», a pixel-heart spinner and «Cancel» (`c2_bench_dialog.webp`). The spinner is live: three frames in a row differ (`c2r2_hearts.webp`). The dialog closed by itself. `Start proc` → `{"bench":true…}`: run 2: **8.2 s** (20:37:29.213 → 37.440), gpu 2750 ms / cpu 1119 ms, factors 2.434 / 1.158. Run 3: **8.0 s**, 2628 / 1126 ms, 2.326 / 1.166. Run 8: **8.0 s**, 2682 / 1111 ms, 2.373 / 1.150. The GPU pass is ≈ 2.4× slower than the CPU pass on this phone (see Found, observation) |
| C3 Bench «Cancel» | pass | Run 4: Cancel was pressed ≈ 2 s after `Displayed` (+403 ms), because each ui call is a separate process. Run 5: wait→tap through one connection, sent as soon as the dialog was found. I did not measure the press time to the millisecond. Both runs: the dialog closed at once and the next `labels` showed home. Relaunch twice: no dialog. No `bench` line was written. `logcat -b crash` was empty for the app. The engine finished its in-flight GPU pass in the background (`blobstat` line ≈ 0.4 s after the cancel), with no visible effect |
| C4 Main path + estimate | pass (estimate soft-fail, see Found) | Picker multi-select, so «Done» was needed. Enhance: `960 × 1280 → 3840 × 5120`, 19.7 MP, «Enhance · ≈ 3 minutes 9 seconds». The notification dialog «Allow» was tapped by its `tree` node (540 2022). Progress: «GPU · Vulkan», 35 tiles. Result: `960 × 1280 → 3840 × 5120`, «4 minutes 19 seconds · GPU · Vulkan» (`c4_result.webp`). **Real/estimate = 259 s / 189 s = 1.37×.** Without bench data (bench cancelled, run 7) the estimate was ≈ 1 min 20 s, which would be 3.2× off. After this run the app learned ≈ 3 min 44 s |
| C5 Save PNG/JPEG/WebP | pass | MediaStore: `…_4x.png` 3840×5120 40 451 589 B (dialog «≈ 37.9 MB», −6 %); `…_4x.jpg` 3840×5120 6 967 194 B (dialog «≈ 7.0 MB»); `…_4x.webp` 3840×5120 6 424 906 B (dialog «≈ 6.5 MB»). After each save: «Saved to Pictures/KARTUN» |
| C6 Share | pass | «Rendering…» first, then the system sheet «Share» with the file `1000009287_kartun_4xNomosWebPhoto-RealPLKSR-fp16_4x.png` (`c6_share_sheet.webp`). «Cancel» was pressed, nothing sent, back on the result |
| C7 Paste a link | pass | Downloaded from GitHub raw. Enhance screen: `4x-NMKD-Siax-fp16.webp`, `1024 × 512 → 4096 × 2048`, 8.4 MP (`c7_link_enhance.webp`) |
| C8 Catalogue download | pass | 4x UltraSharp V2 Lite (15 MB) «Download» by its node. After ≤ 15 s the card shows «Delete» (`c8_catalogue_delete.webp`). `kartun-cli info`: `4x-UltraSharpV2-Lite-fp16 installed:true`. Enhance then shows «on the device: 3» |
| C9 About | pass | «Engine: MikalaiKryvusha/ncnn 20260526-kartun.1»; row «MikalaiKryvusha/ncnn — custom modified and improved fork»; GPU «Mali-G68 MC4»; Android «14 (API 34)»; Version «0.1 (1)» (`c9_about_en.webp`) |
| C10 Languages | pass with a defect | RU: home and About fully Russian, the fork row «кастомный модифицированный и улучшенный форк», nothing cut (`c10_ru_home.webp`, `c10_ru_about.webp`). AR: home mirrored (logo right, gear left, flow chips right-to-left with «←»). Settings: back arrow on the right, row chevrons pointing left (`c10_ar_settings.webp`). About is mirrored, but the Android value «14 (API 34)» renders as «API) 14 / (34» on two lines (`c10_ar_about_android_crop.webp`), see Found |
| C11 Landscape | pass | Home: two columns, «Choose a photo» card left, link button and share card right, nothing cut (`c11_land_home_1200.webp`). Enhance: photo with «960 × 1280 · 1.2 MP» and «Choose another photo» left, model list and pinned «Enhance · ≈ 3 minutes 44 seconds» right; the right pane scrolls under the button (`c11_land_enhance.webp`). Rotation restored: `user_rotation 0`, `accelerometer_rotation 1` |
| C12 Cancel processing | pass | My poll missed 10 % (wrong regex), so I pressed Cancel at **37 %** (tile 13/35, 1 min 4x s; `c12_before_cancel.webp`). Tap at ≈ 20:53:32. The screen stayed on progress until the in-flight tile 14/35 finished (20:53:38.588), then returned to Enhance (`c12_after_cancel.webp`). No crash. Second Enhance: «≈ 3 min 44 s» promised, finished in **4 min 13 s**, 3840 × 5120, GPU · Vulkan (`c12_second_result.webp`) |
| C13 Crash journal | pass | `logcat -d -b crash` (cleared 20:40:28, covering C4–C12): 0 lines. Earlier windows: C3 runs checked separately (0). The C2 window's crash buffer was cleared at 20:38:26 before it was read. Its saved main log (`c2_logcat.txt`) has 0 `FATAL EXCEPTION`/`AndroidRuntime`/`Fatal signal` |

## 5. Found

No bug documents were filed: this run was report-only. Each item below is for the owner to triage.

- **Time estimate still 1.37× optimistic after the first-launch bench.** Description: the bench raised the estimate from
  ≈ 1 min 20 s (3.2× off) to ≈ 3 min 9 s, but the real time is 4 min 19 s. After one run it learned ≈ 3 min 44 s, and the
  second run took 4 min 13 s (1.13×). Steps: `pm clear` → launch → let the bench finish → pick the 960 × 1280 photo → Enhance 4×
  with 4xNomosWebPhoto RealPLKSR on GPU. Expected: estimate within ≈ ±20 % of real. Actual: 189 s promised, 259 s taken. The
  phone warmed up: tiles took ≈ 7.4 s each in run 9 and ≈ 8.3 s at the start of run 17.
- **Arabic About: the Android version is garbled.** Description: the LTR value «14 (API 34)» goes through RTL bidi reordering
  and wraps. Steps: `cmd locale set-app-locales com.kriniks.kartun --locales ar` → Settings → «حول التطبيق». Expected:
  «14 (API 34)» on one line. Actual: «API) 14» / «(34» on two lines (`c10_ar_about_android_crop.webp`). The same row in EN/RU is
  fine. Other LTR values in AR (`Xiaomi 22101316UG`, `Mali-G68 MC4`, `MikalaiKryvusha/ncnn 20260526-kartun.1`) render correctly.
- **Processing «Cancel» has no visible acknowledgement for ≈ 6 s.** Description: on this phone one tile takes 6–9 s. After
  Cancel the progress screen stays unchanged until the tile in flight ends. Steps: Enhance → at 37 % press «Cancel». Expected:
  an immediate reaction (e.g. «Cancelling…» or the return). Actual: the progress UI kept running ≈ 6 s (tap ≈ 20:53:32,
  tile 14/35 done 20:53:38.6, then Enhance).
- **Progress screen text truncated in English at 1080 px** (cosmetic). «The network is enhancing your ph…», ELAPSED «3 min 2…»,
  LEFT «≈ 1 min …» (`c4_progress_b.webp`, `c12_before_cancel.webp`).
- **Share shows «Saving…» on the Save button** while the share file renders (minor, read in the accessibility tree:
  `Rendering… | Cancel | … | Saving… | Share`). The user pressed Share, not Save.
- **PNG size promise 6 % low:** dialog «≈ 37.9 MB», file 40.45 MB (JPEG and WebP within 2 %).
- Known, unchanged since the earlier report: the size line «960 × 1280 → 3840 × 5120» wraps on the Enhance header and the
  result screen. The gallery file name is a number (`1000009287.jpg`). The picker is multi-select and needs «Done».
- Observation, not a defect: the bench measures the GPU pass at ≈ 2.7 s and the CPU pass at ≈ 1.1 s on this phone, yet GPU is
  the default and gave 4 min 19 s. A CPU run was not timed, so it is unknown whether CPU would be faster here.
- Observation: About shows «Version 0.1 (1)» on a build called the 1.0 RC. The version may still need a bump before release.

## 6. Traces

- Screenshots and logs: `C:\Users\krinik\AppData\Local\Temp\rc_smoke\redmi\`. Screenshots: `c2_bench_dialog.webp`,
  `c2r2_hearts.webp`, `c3_after_cancel.webp`, `c3_relaunch.webp`, `c4_enhance_nobench.webp`, `c4_enhance.webp`,
  `c4_progress.webp`, `c4_progress_b.webp`, `c4_result.webp`, `c5_save_dialog.webp`, `c6_share_sheet.webp`,
  `c7_link_enhance.webp`, `c8_catalogue_delete.webp`, `c9_about_en.webp`, `c10_ru_home.webp`, `c10_ru_about.webp`,
  `c10_ar_home.webp`, `c10_ar_settings.webp`, `c10_ar_about.webp`, `c10_ar_about_android_crop.webp`,
  `c11_land_home_1200.webp`, `c11_land_enhance.webp`, `c12_before_cancel.webp`, `c12_after_cancel.webp`,
  `c12_second_result.webp`. Logs: `c2_logcat.txt`, `c3_logcat.txt`, `c13_crash_buffer.txt` (empty). Test photo:
  `kartun_smoke.jpg`.
- Bench lines (logcat tag `KARTUN`): `{"bench":true,"gpu_ms":2750.0,"cpu_ms":1119.0,"gpu_factor":2.4336283,"cpu_factor":1.158385}`,
  `{… "gpu_ms":2628.0,"cpu_ms":1126.0,"gpu_factor":2.3256638,"cpu_factor":1.1656314}`,
  `{… "gpu_ms":2682.0,"cpu_ms":1111.0,"gpu_factor":2.3734512,"cpu_factor":1.1501036}`.
- Device state after the run: no rows for `Pictures/KARTUN%` or `%kartun%` in MediaStore. Both folders were removed. App
  locales `[]`, rotation `0` / `1`. UI agent stopped and port 8382 unforwarded. The downloaded model 4x UltraSharp V2 Lite
  stays in the app's data.
- Process note: the session scratchpad is shared with the other device agents. At 20:55 another agent overwrote my `shot.sh`
  helper, so one screenshot (20:58:40) went to `rc_smoke\a52\c12_second_result.*`. I deleted those two files, retook the
  screenshot of this phone into `redmi\`, and switched to uniquely named helpers. Every earlier screenshot was written to
  `redmi\` (each run printed the path).

## 7. Verdict

pass — all thirteen cases pass on Android 14 / Mali-G68. Three soft defects: the estimate is still 1.37× optimistic after the
bench, the Android version value is garbled in Arabic About, and the processing Cancel gives no feedback for ≈ 6 s. None
blocks the release path.
