# Test run report — RC2 fix verification on the Redmi Note 12 Pro+ (Android 14, Mali-G68)

**Created:** 2026-10-16 21:42 +03:00 · **Run by:** the agent (Claude Opus 5.5, subagent) · **Version/build:**
`C:\Users\krinik\AppData\Local\Temp\rc\kartun-rc2.apk` (62 953 421 bytes, 21:18), release `com.kriniks.kartun` `0.1 (268)`,
versionCode 268, minSdk 34, targetSdk 37, commit `64b66a4`

## 1. Work

Verification of the RC smoke fixes on one device, case set `testcases/TC_release_1_0_rc2_fixes.md` (F0–F12). The defects
being re-checked come from `testcases/reports/2026-10-16_rc_smoke_redmi.md`: the estimate was 1.37× optimistic, the size line
wrapped inside numbers, the picked photo was named by a number, the Android value was garbled in Arabic About, and the
progress text was truncated at 1080 px.

## 2. Contour

Xiaomi Redmi Note 12 Pro+ 5G (`22101316UG`), Android 14 (API 34), Dimensity 1080, Mali-G68 MC4, 1080 × 2400. adb over home Wi-Fi
`192.168.1.76:41705`. System language English. Battery 90 → 86 %, not charging. Battery temperature 29.6 °C at the start,
32.7 °C after three 4× runs in 20 minutes. The phone got warm but did not slow down noticeably: the three runs took 4:17, 4:18
and 4:24. RC2 (`0.1 (267)`, installed 21:11) was replaced by `0.1 (268)` with `adb install -r`, then `pm clear` before F1.
UI was driven by locators with the UI agent `C:\Android\private\ui-agent\ui.py`, port 8392. The agent was started with
`am instrument … AutomationServer#serve -e port 8392`. Test photo: `engine/third_party/testdata/photo1.jpg` stripped of EXIF
(960 × 1280), pushed at 21:21:53 to `/sdcard/Pictures/KARTUN_test/kartun_smoke.jpg` and scanned. In the picker it is «Photo
taken on Oct 16, 2026 9:21 PM». MediaStore `datetaken=NULL`. Original settings: `accelerometer_rotation=1`, `user_rotation=0`,
app locales `[]`.
REAL WORLD: release build installed with adb, not from a store. Otherwise the same as a user's phone.

## 3. Runs

`adb` = `C:/Android/Sdk/platform-tools/adb.exe -s 192.168.1.76:41705`. `ui "<cmd>"` =
`python C:/Android/private/ui-agent/ui.py --serial 192.168.1.76:41705 --port 8392 "<cmd>"` (through
`C:\Users\krinik\AppData\Local\Temp\rc2\redmi\redmi_ui.sh`). Screenshots: `adb exec-out screencap -p` → `magick … .webp`
(`redmi_shot.sh`).

| # | Moment | Command | Exit / outcome |
|---|---|---|---|
| 1 | 2026-10-16 21:20 +03:00 | `adb install -r C:/Users/krinik/AppData/Local/Temp/rc/kartun-rc2.apk`; `adb shell dumpsys package com.kriniks.kartun` | `Success`; versionCode=268, versionName `0.1 (268)`, lastUpdateTime 21:20:32 |
| 2 | 2026-10-16 21:20 +03:00 | F1: `adb shell pm clear com.kriniks.kartun`; `adb logcat -c`; `adb logcat -b crash -c`; `adb shell am start -n com.kriniks.kartun/.MainActivity`; screencap at +2 s; `ui "labels"`; `ui "waitGone Measuring 90000"`; `ui "labels"`; `adb logcat -d` | dialog, then home; `bench_probe` and `bench` lines |
| 3 | 2026-10-16 21:21 +03:00 | F0: `ui "tapText Settings"` → `ui "tapText Version, device, author, licences"` → `ui "labels"` | «Version 0.1 (268)» |
| 4 | 2026-10-16 21:21 +03:00 | `magick photo1.jpg -strip kartun_smoke.jpg`; `adb push … /sdcard/Pictures/KARTUN_test/kartun_smoke.jpg`; `adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/KARTUN_test/kartun_smoke.jpg` | 960 × 1280 row in MediaStore. A first attempt pushed an empty file because `MSYS_NO_PATHCONV` broke the magick path. That row and file were deleted at once, before the picker was opened |
| 5 | 2026-10-16 21:22 +03:00 | F2/F3/F6 EN: `ui back` ×2 → `ui "tapText Choose a photo"` → `ui "tapText Photo taken on Oct 16, 2026 9:21 PM"` → `ui "tapText Done"` → `ui labels`, `ui tree` | Enhance: `IMG_20261016_212153.jpg`; estimate ≈ 4 min 10 s |
| 6 | 2026-10-16 21:23 +03:00 | F4/F5 EN: `adb logcat -c`; `ui "tapText Enhance · ≈"` → `ui tree` → `ui "tap 540 2022"` (node «Allow»); `redmi_poll.sh f5en` (`labels` every ≈ 21 s, screenshots) | result at 21:27:33, **4 minutes 17 seconds** · GPU · Vulkan |
| 7 | 2026-10-16 21:27 +03:00 | F2 save: `ui "tapText Save"` → `ui "tapText JPEG"` → `ui tree` → `ui "tap 585 1725"` (dialog «Save» node); `adb shell "content query --uri content://media/external/images/media --projection _display_name:width:height:_size --where \"relative_path LIKE 'Pictures/KARTUN/%'\""` | `IMG_20261016_212153_kartun_4xNomosWebPhoto-RealPLKSR-fp16_4x.jpg` 3840 × 5120 |
| 8 | 2026-10-16 21:28 +03:00 | F7: `ui "tapText Share"` → `ui "waitGone Rendering… 60000"` → `ui labels` → `ui "tapText Cancel"` | sheet with the file name; cancelled |
| 9 | 2026-10-16 21:28 +03:00 | F9: `ui back` → `ui "tapText All models"`; `adb shell input swipe 540 1700 540 1450 600` (scroll) | gap under the chips |
| 10 | 2026-10-16 21:29 +03:00 | F10: `ui back` ×2; `adb shell settings put system accelerometer_rotation 0`; `adb shell settings put system user_rotation 1`; screenshot; `user_rotation 0`, `accelerometer_rotation 1`; `adb shell cmd locale set-app-locales com.kriniks.kartun --locales fr`; screenshot | landscape and French home |
| 11 | 2026-10-16 21:30 +03:00 | F11/F3/F6 DE: `… --locales de`; `ui "tapText Einstellungen"`; `ui back`; `ui "tapText Foto auswählen"` → photo → `Done` | DE settings and Enhance |
| 12 | 2026-10-16 21:30 +03:00 | F4/F5 DE: `ui "tapText Verbessern · ≈"`; `redmi_poll.sh f5de NOCH` | result at 21:35:23, **4 Minuten 18 Sekunden** (estimate ≈ 4 min 13 s) |
| 13 | 2026-10-16 21:35 +03:00 | F6 RU: `… --locales ru` (on the result), `ui back` → Enhance; screenshot | RU Enhance |
| 14 | 2026-10-16 21:36 +03:00 | F8 AR: `… --locales ar`; `ui tree`; `ui "tap 495 2208"` (node of the Enhance button); `redmi_poll.sh f8ar المتبقي`; `ui back` ×2 → `ui "tap 115 185"` (node «الإعدادات») → `ui "tap 748 1392"` (node «حول التطبيق») | AR Enhance, result (**4 min 24 s**, estimate ≈ 4 min 15 s), About |
| 15 | 2026-10-16 21:41 +03:00 | F12: `adb logcat -d -b crash` (buffer cleared 21:20:56, covers runs 2–14); `adb logcat -d` grep `FATAL EXCEPTION|Fatal signal` | 0 lines; 0 |
| 16 | 2026-10-16 21:42 +03:00 | cleanup: `adb shell "content delete --uri content://media/external/images/media --where \"relative_path LIKE 'Pictures/KARTUN/%' OR relative_path LIKE 'Pictures/KARTUN_test/%'\""`; `adb shell rmdir /sdcard/Pictures/KARTUN /sdcard/Pictures/KARTUN_test`; `… --locales ""`; `ui quit`; `adb forward --remove tcp:8392` | query `Pictures/KARTUN%` or `%kartun%` → «No result found»; both folders gone; locales `[]`; rotation 1 / 0 |

## 4. Checks

Hygiene: NONE. This was a device check of a prebuilt RC2 APK. No build, unit, lint or mutation was run in this pass.
Functional run: RC2 on the Redmi Note 12 Pro+ (Android 14) as a user. Covered: first-launch bench, About, picker → Enhance →
three 4× runs (EN, DE, AR) → result → save JPEG → share, models list, landscape and French home, German settings, RU Enhance,
Arabic Enhance / result / About, crash journal. Every step was read in the accessibility tree (`labels`/`tree`). I read the
screenshots of every screen named below myself. Bench numbers come from logcat. The saved file name comes from a MediaStore query.

| Case | Status | Observation |
|---|---|---|
| F0 version | pass | About: «Version 0.1 (268)» (`f0_about_en.webp`); `dumpsys` versionCode 268 |
| F1 first-launch bench | pass | At +2 s: a darker home screen behind the dialog «Measuring the device's GPU and CPU performance…», a pixel-heart spinner and «Cancel» (`f1_bench_dialog.webp`). The dialog closed by itself (`waitGone` → home). `Start proc` 21:20:56.112 → `{"bench":true` 21:21:12.008 = **15.9 s**. In RC1 it was 8.0–8.2 s; the ESRGAN probe adds ≈ 8 s. Lines: `{"bench_probe":true,"base_ms":759.0,"esrgan_ms":1912.0,"ratio":2.519104}` (21:21:09.734); `{"bench":true,"gpu_ms":2684.0,"cpu_ms":1113.0,"gpu_factor":3.135514,"cpu_factor":1.1521739,"esrgan_correction":1.3616778}` |
| F2 name of a picked photo | pass | Enhance: `IMG_20261016_212153.jpg` in the tree, which matches the push time 21:21:53. On screen it is cut by width to «IMG_20261…» (EN) or «IMG_202610…» (DE/RU), not a number. Saved JPEG: `IMG_20261016_212153_kartun_4xNomosWebPhoto-RealPLKSR-fp16_4x.jpg`, 3840 × 5120. Share file `IMG_20261016_212153_kartun_4xNomosWebPhoto-RealPLKSR-fp16_4x.png` |
| F3 size line | **fail** | Scale row: «960 × 1280 → 3840 × 5120» on one line in EN and DE. Result panel: one line in EN and DE (`f5en_result.webp`, `f5de_result.webp`). Collapsed photo row: three centred lines «960 × 1280» / pink ↓ / «3840 × 5120», but **the third line is clipped at the bottom by the card**: only the top half of «3840 × 5120» shows. It is the same in EN, DE and RU and stays the same on a second screenshot (`f3_photo_row_en_crop.webp`, `f3_photo_row_en_crop2.webp`, `f3_f6_enhance_de.webp`, `f6_enhance_ru.webp`). No break inside a number anywhere |
| F4 estimate | pass | EN: button «Enhance · ≈ 4 minutes 10 seconds» (250 s), real **4 min 17 s** (257 s): **ratio 1.03**. Second run (DE): ≈ 4 min 13 s → 4 min 18 s, **1.02**. Third (AR): ≈ 4 min 15 s → 4 min 24 s, **1.04**. Siax card estimate «≈ 9 minutes 11 seconds» (not run). RC1 was 1.37× |
| F5 processing panel | pass | EN (`f5_progress_en_a.webp`, `f5en_progress_c.webp`): title «The network is enhancing your photo» whole on 2 lines. «Cancel» is on the «GPU · Vulkan» chip's line under the title. Tiles 2 × 2 (PASS 1 / 1, TILE 24 / 35, ELAPSED «3 min 15 s», LEFT «≈ 1 min 29 s»), values whole. DE (`f5de_progress_c.webp`): «Das Netz verbessert dein Foto» whole on 2 lines, «Abbrechen» on the chip line, DURCHGANG / KACHEL / VERGANGEN «3 Min 6 s» / NOCH «≈ 1 Min 37 s», whole. AR (`f8ar_progress_c.webp`): mirrored, whole |
| F6 Enhance button | pass | EN «Enhance · ≈ 4 minutes 10 seconds», DE «Verbessern · ≈ 4 Minuten 13 Sekunden», RU «Улучшить · ≈ 4 минуты 15 секунд»: each on one line, words whole, the sparkle icon has a clear margin from the button edge (`f2_enhance_en.webp`, `f3_f6_enhance_de.webp`, `f6_enhance_ru.webp`) |
| F7 share | pass | «Rendering…», then the MIUI «Share» sheet with the file row `IMG_20261016_21…LKSR-fp16_4x.png` and a generic image icon, no thumbnail (`f7_share_sheet.webp`). «Cancel» pressed, back on the result, nothing sent |
| F8 Arabic | pass with a defect | Enhance (`f8_ar_enhance.webp`): sizes read «960 × 1280 ← 3840 × 5120» RTL, each number pair intact («3840 × 5120», not «5120 × 3840»). The file name starts «IMG_20261016_…» (cut by width, not reordered). Result (`f8ar_result.webp`): the tag over the left, softer side is «قبل» and the right is «بعد». The divider crop shows the left half is the blurry original (`f8_ar_result_divider_crop2.png`). About (`f8_ar_about.webp`): Android «14 (API 34)» whole on one line (this device is API 34). Defects seen (see Found): the Arabic result panel joins the size line to the time line, and the About version line reads «(268) 0.1» |
| F9 models list | pass | All models at the top: ≈ 35 px gap between the chips and the first card (`f9_models_top.webp`). After a short scroll the list clips ≈ 28 px below the chips, so the gap stays visible (`f9_models_scrolled.webp`) |
| F10 home | pass | Landscape (`f10_land_home.webp`): two columns, «Photo → Share → KARTUN» chips readable in the right card. French (`f10_fr_home.webp`): «Choisissez une photo» centred on one line under the icon |
| F11 German settings | pass | «GPU-Beschleunigung» whole on one line next to the switch (`f11_de_settings.webp`) |
| F12 crash journal | pass | `logcat -d -b crash` (cleared 21:20:56, covers the whole run): 0 lines, 0 for `com.kriniks.kartun`; main log: 0 `FATAL EXCEPTION`/`Fatal signal` |

## 5. Found

No bug documents were filed: this run was report-only. Each item below is for the owner to triage.

- **F3: the collapsed photo row clips the third size line.** On Enhance, the photo card is too short for the three-line block
  «960 × 1280» / ↓ / «3840 × 5120». Only the top half of «3840 × 5120» shows. EN, DE and RU at 1080 px
  (`f3_photo_row_en_crop.webp`). In AR the row has room and shows one line. Expected: the whole third line, or one line.
- **Arabic result panel: the size and the time run together on one row.** The row reads «GPU · Vulkan · ٤ دقائق ٢٤ ثانية3840 × 5120
  ← 960 × 1280». The Arabic «٤» touches «3840» with no space or separator (`f8_ar_result_sizeline_crop.webp`). In EN/DE they are
  two separate lines.
- **Arabic About: the version line is reordered.** It reads «(268) 0.1 الإصدار», not «0.1 (268)». The value has no bidi isolate.
  The tree shows `الإصدار 0.1 (268)` without ⁦⁩, while Android/GPU/Engine values have them. This is the same class as the RC1
  «14 (API 34)» defect, which is fixed now.
- Observation: the first-launch bench now takes 15.9 s on this phone (was 8 s in RC1), because the ESRGAN probe was added.
- Observation: the picked file name is cut by width on the Enhance card («IMG_20261…»). It is readable, not a number.
- Observation: the MIUI share sheet shows the file name with a generic icon, not a thumbnail. The case accepts a name and/or a
  thumbnail.

## 6. Traces

- Screenshots and logs: `C:\Users\krinik\AppData\Local\Temp\rc2\redmi\`. Screenshots: `f0_about_en.webp`, `f1_bench_dialog.webp`,
  `f2_enhance_en.webp`, `f3_enhance_en_again.webp`, `f3_photo_row_en_crop.webp`, `f3_photo_row_en_crop2.webp`,
  `f3_f6_enhance_de.webp`, `f5_progress_en_a.webp`, `f5en_progress_b.webp`, `f5en_progress_c.webp`, `f5en_result.webp`,
  `f5_progress_de_a.webp`, `f5de_progress_b.webp`, `f5de_progress_c.webp`, `f5de_result.webp`, `f6_enhance_ru.webp`,
  `f7_share_sheet.webp` (shows the owner's share targets; keep it local), `f8_ar_enhance.webp`, `f8_ar_progress_a.webp`,
  `f8ar_progress_b.webp`, `f8ar_progress_c.webp`, `f8ar_result.webp`, `f8_ar_result_sizeline_crop.webp`,
  `f8_ar_result_divider_crop.webp`, `f8_ar_result_divider_crop2.png`, `f8_ar_about.webp`, `f9_models_top.webp`,
  `f9_models_scrolled.webp`, `f10_land_home.webp`, `f10_fr_home.webp`, `f11_de_settings.webp`. Logs: `f1_logcat.txt` (bench lines),
  `f4_run_logcat.txt`, `f12_main_logcat.txt`, `f12_crash_buffer.txt` (empty), progress polls `f5en_poll.txt`, `f5de_poll.txt`,
  `f8ar_poll.txt`, trees `f3_tree_en.txt`, `f8_ar_enhance_tree.txt`. Test photo `kartun_smoke.jpg`. Helpers `redmi_ui.sh`,
  `redmi_shot.sh`, `redmi_poll.sh`.
- Device state after the run: no MediaStore rows for `Pictures/KARTUN%` or `%kartun%`. Both test folders were removed. App
  locales `[]`. `accelerometer_rotation 1`, `user_rotation 0`, as before. UI agent stopped and port 8392 unforwarded.
  The app keeps its bench data and the notification permission granted during F4.

## 7. Verdict

partial — F0–F2 and F4–F12 pass, with the estimate within 1.02–1.04×. F3 fails: the collapsed photo row clips the third size
line in EN/DE/RU. F8 passes on its four checks but shows two new Arabic bidi/layout defects (result size+time joined, version
«(268) 0.1»).
