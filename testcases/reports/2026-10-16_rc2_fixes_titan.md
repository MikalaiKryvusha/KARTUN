# Test run report — RC2 fix verification on the HEADWOLF Titan 1

**Created:** 2026-10-16 21:31 +03:00 · **Run by:** the agent (Claude Opus 5.5, subagent of the RC2 coordinator) ·
**Version/build:** `C:\Users\krinik\AppData\Local\Temp\rc\kartun-rc2.apk` — release `com.kriniks.kartun`, `dumpsys`
`versionCode=268 versionName=0.1 (268)`, minSdk 34, targetSdk 37, commit `64b66a4`

## 1. Work

Verification of the RC smoke fixes in RC2 on one device by the case set `testcases/TC_release_1_0_rc2_fixes.md` (F0–F12):
version, first-launch bench with the ESRGAN probe, picked-photo name, size line, estimate, processing panel, Enhance
button, share preview, Arabic bidi, models list gap, home in landscape/French, German settings hyphenation, crash journal.
Basis: the defects of `testcases/reports/2026-10-16_rc_smoke_titan.md` and commit `64b66a4`.

## 2. Contour

HEADWOLF Titan 1 tablet, Android 16 (API 36), Mali-G615 MC6, 1600 × 2560, system language Russian, adb `192.168.1.3:5555`
over Wi-Fi, battery 29 → 26 %. Before the run the device held `0.1 (267)`; RC2 installed over it with `adb install -r` (no
rebuild). UI driven by the UI agent `C:\Android\private\ui-agent\ui.py` on port 8391 (`wait`, `waitGone`, `tapText`,
`labels`, `tree`, `back`; taps by coordinates only on nodes from `tree`: the permission «Разрешить» 799 1371 and the Save
dialog's «Save» 848 1796; two scroll swipes with `adb shell input swipe`). App data cleared (`pm clear`) for F1, so the
estimates of F4 come from this run's bench. Test photo: `engine/third_party/testdata/photo1.jpg` stripped of EXIF →
`/sdcard/Pictures/KARTUN_test/titan_rc2.jpg` (960 × 1280, pushed 21:22:00, file time 21:21:59, MediaStore `_id=63350`),
picked by «Фото, дата и время съемки: 16 окт. 2026 г. 21:21» (unique in the grid). Network not used. Rotation before the run:
`accelerometer_rotation 1`, `user_rotation 0`; app locales `[]`.
REAL WORLD: the release-signed RC2 on a real device; the only difference from a user's tablet is the adb/agent harness.

## 3. Runs

One pass, F0–F12 in order (F3/F5/F6/F8 screens were collected along the two Enhance runs). Shell: Git Bash with
`source C:/Users/krinik/AppData/Local/Temp/rc2/titan/titan_env.sh` (`A=C:/Android/Sdk/platform-tools/adb.exe;
S=192.168.1.3:5555; MSYS_NO_PATHCONV=1; ui() = python C:/Android/private/ui-agent/ui.py --serial $S --port 8391 "<cmd>"`;
`shotfull <name>` = `$A -s $S exec-out screencap -p > <name>.png && magick <name>.png -quality 85 <name>.webp`).

| # | Moment | Command | Exit / outcome |
|---|---|---|---|
| install | 2026-10-16 21:20 +03:00 | `$A -s $S install -r C:/Users/krinik/AppData/Local/Temp/rc/kartun-rc2.apk; $A -s $S shell dumpsys package com.kriniks.kartun \| grep -E "versionName\|versionCode"` | 0 · `Success`; `versionCode=268`, `versionName=0.1 (268)` |
| F0 | 2026-10-16 21:21 +03:00 | `$A -s $S shell cmd locale set-app-locales com.kriniks.kartun --locales en; $A -s $S shell am start -n com.kriniks.kartun/.MainActivity; ui "tapText Settings"; ui "tapText About"; ui labels; shotfull f0_about_en` | 0 · «Version 0.1 (268)» |
| F1 | 2026-10-16 21:21 +03:00 | `$A -s $S shell pm clear com.kriniks.kartun; $A -s $S logcat -c; $A -s $S logcat -b crash -c; $A -s $S shell am start -n com.kriniks.kartun/.MainActivity; sleep 2; shotfull f1_bench_dialog; ui labels; sleep 10; ui labels; $A -s $S logcat -d -v time > f1_logcat.txt; grep -E "Start proc.*kriniks\|bench" f1_logcat.txt` | 0 · dialog at +2 s, home alone at ~+12 s; bench line at +6.49 s |
| F2 | 2026-10-16 21:22 +03:00 | `magick photo1.jpg -strip titan_rc2.jpg; $A -s $S push titan_rc2.jpg /sdcard/Pictures/KARTUN_test/titan_rc2.jpg; $A -s $S shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/KARTUN_test/titan_rc2.jpg`; `--locales en`; `ui "tapText Choose a photo"`, `ui "tapText 21:21"`, `ui "tapText Готово"`, `ui labels`, `shotfull f2_enhance_en` | 0 · Enhance titled «IMG_20261016_212159.jpg» |
| F3/F4/F5 en | 2026-10-16 21:23 +03:00 | `$A -s $S logcat -c; ui "tapText Enhance ·"`; `ui tree` → `ui "tap 799 1371"` (Разрешить); `shotfull f5_run_en_a`; `sleep 20; shotfull f5_run_en_b`; `ui "waitGone Cancel 120000"`; `ui labels`; `shotfull f3_result_en`; `$A -s $S logcat -d -v time > f4_logcat.txt` | 0 · estimate ≈ 1 min 14 s; engine tile 1/35 21:23:31.0 → 35/35 21:24:34.4; result «1 minute 5 seconds» |
| F2 save | 2026-10-16 21:24 +03:00 | `ui "tapText Save"; ui "tapText JPEG"; ui tree` → `ui "tap 848 1796"`; `$A -s $S shell "content query --uri content://media/external/images/media --projection _id:_display_name:width:height:_size --where \"relative_path LIKE 'Pictures/KARTUN/%' AND _display_name LIKE 'IMG_2026%'\""` | 0 · `IMG_20261016_212159_kartun_4xNomosWebPhoto-RealPLKSR-fp16_4x.jpg` 3840 × 5120 |
| F7 | 2026-10-16 21:25 +03:00 | `ui "tapText Share"; ui "waitGone Rendering 30000"; ui labels; shotfull f7_share_sheet; ui back`; MediaStore query `_id>63349` | 0 · sheet with image preview; backed out, no new rows |
| F3/F5/F6 de | 2026-10-16 21:25 +03:00 | `--locales de` (on the result) → `shotfull f3_result_de`; `ui "tapText Zurück"`; `shotfull f3_enhance_de`; `ui "tapText Verbessern ·"`; `sleep 4; shotfull f5_run_de_a; sleep 15; shotfull f5_run_de_b`; `ui "waitGone Abbrechen 120000"`; `ui labels` | 0 · estimate ≈ 1 min 10 s, result «1 Minute 6 Sekunden» (21:26:30 → 21:27:37 incl. tap) |
| F8 | 2026-10-16 21:27 +03:00 | `--locales ar` (on the result) → `ui labels; shotfull f8_result_ar`; `ui "tapText رجوع"` → `shotfull f8_enhance_ar`; `ui "tapText رجوع"`, `ui "tapText الإعدادات"`, `ui "tapText حول التطبيق"`, `shotfull f8_about_ar` | 0 · Arabic result, Enhance, About read |
| F6 ru / F9 | 2026-10-16 21:29 +03:00 | `--locales ru`; `ui back` ×2; `ui "tapText Выберите фото"`, `ui "tapText 21:21"`, `ui "tapText Готово"`; `shotfull f6_enhance_ru`; `ui "tapText Выбрать"`; `shotfull f9_models_top`; `$A -s $S shell input swipe 800 1800 800 1500 500`; `shotfull f9_models_scrolled` | 0 · ru Enhance; models list before/after scroll |
| F10 | 2026-10-16 21:30 +03:00 | `ui back` ×2; `$A -s $S shell settings put system accelerometer_rotation 0; $A -s $S shell settings put system user_rotation 1`; `shotfull f10_home_land`; `settings put system user_rotation 0`; `--locales fr`; `shotfull f10_home_fr` | 0 · landscape home (ru), French home |
| F11 | 2026-10-16 21:30 +03:00 | `--locales de`; `ui "tapText Einstellungen"`; `ui labels`; `shotfull f11_settings_de` | 0 · «GPU-Beschleunigung» on one line |
| F12 | 2026-10-16 21:30 +03:00 | `$A -s $S logcat -d -b crash > f12_crash.txt; wc -l < f12_crash.txt; grep -c kriniks f12_crash.txt` (buffer cleared 21:21 in F1) | 0 · 0 lines |
| cleanup | 2026-10-16 21:30 +03:00 | `$A -s $S shell content delete --uri content://media/external/images/media/63350` and `…/63351`; `content query … --where "_id>=63350"`; `cmd locale set-app-locales com.kriniks.kartun --locales '""'`; `settings put system user_rotation 0; settings put system accelerometer_rotation 1`; read back | 0 · «No result found»; locales `[]`; rotation 0 / 1 (the old values) |

## 4. Checks

Hygiene: NONE (no code changed; a device verification run only)
Functional run: RC2 walked as a user on the Titan (Android 16, release build): About, first-launch bench after `pm clear`,
gallery pick → Enhance → two full runs (en, de) → result → Save JPEG → Share → back, the result/Enhance/About in Arabic,
Enhance in ru, the models list, home in landscape and French, German settings, the crash buffer — READ: `labels`/`tree` of
every step, the screenshots (every key .webp and the full-resolution crops opened and looked at), logcat (`Start proc`,
`{"bench_probe":…}`, `{"bench":…}`, `KARTUN_ENGINE` tiles), the MediaStore rows, the crash buffer.

| Case | Status | Observation |
|---|---|---|
| F0 | pass | About (en): «Version 0.1 (268)»; `dumpsys` `versionCode=268`. Also shown in Arabic About «الإصدار 0.1 (268)» |
| F1 | pass | +2 s screenshot: home dimmed behind the dialog «Измеряем производительность GPU и CPU устройства…» (pm clear reset the app locale to the device's Russian), pixel-heart spinner, «Отмена». `labels` at ~+12 s: home only — closed by itself. Logcat: `21:21:31.487 Start proc 1705` → `21:21:36.759 {"bench_probe":true,"base_ms":371.0,"esrgan_ms":559.0,"ratio":1.5067385}` → `21:21:37.972 {"bench":true,"gpu_ms":795.0,"cpu_ms":596.0,"gpu_factor":0.9287383,"cpu_factor":0.6169772,"esrgan_correction":0.81445324}` = **6.49 s** (RC1: 3.77 s; the probe adds ~2.7 s) |
| F2 | pass | Picker grid entry «… 16 окт. 2026 г. 21:21» → «Готово» → Enhance title «IMG_20261016_212159.jpg» (file time 21:21:59, the push), not «63350.jpg». Saved JPEG: `IMG_20261016_212159_kartun_4xNomosWebPhoto-RealPLKSR-fp16_4x.jpg`, 3840 × 5120, 6 987 595 B |
| F3 | pass | One line «960 × 1280 → 3840 × 5120» everywhere on the Titan: Enhance collapsed photo row and scale row (en `f2_enhance_en`, de `f3_enhance_de`, ru `f6_enhance_ru`), result panel (en `f3_result_en`, de crop `f3_result_de_panel`). No break inside a number. The 3-line form never triggered at tablet width |
| F4 | pass | Button «Enhance · ≈ 1 minute 14 seconds» (74 s) → result «1 minute 5 seconds» (65 s; engine tiles 21:23:31.0 → 21:24:34.4) → **real / estimate = 0.88**. Second run (de) after self-correction: estimate ≈ 1 min 10 s (70 s), real «1 Minute 6 Sekunden» (66 s) → 0.94. Siax card estimate (not run): «≈ 1 minute 38 seconds» (98 s; Siax / Nomos = 1.32) |
| F5 | pass | en (`f5_run_en_a/b`): title «The network is enhancing your photo» whole on one line; «Cancel» on the line of the «GPU · Vulkan» chip under the title; 4 tiles in one row PASS 1 / 1 · TILE 15 / 35 · ELAPSED 31 s · LEFT ≈ 40 s. de (crop `f5_run_de_a_panel`): «Das Netz verbessert dein Foto» whole, «Abbrechen» on the chip line, DURCHGANG 1 / 1 · KACHEL 2 / 35 · VERGANGEN 5 s · NOCH «≈ 1 Min 14 s» whole (ends ~40 px short of the tile edge). No «…» anywhere |
| F6 | pass | Crops `f6_buttons`: «Enhance · ≈ 1 minute 14 seconds», «Verbessern · ≈ 1 Minute 10 Sekunden», «Улучшить · ≈ 1 минута 8 секунд» — each on one line, words whole, sparkle icon well inside the pill (≥ 200 px from the edge) |
| F7 | pass | «Share» → «Rendering… / Cancel» for a moment → system sheet «Отправка изображения» with a large **thumbnail of the result** (pencil edit badge) above the targets (RC1 showed none). No file-name line on this stock sheet, the thumbnail satisfies «name and/or thumbnail». `back` closed it; MediaStore had no new rows, nothing sent |
| F8 | pass (with 1 minor new defect) | Result (ar): tag «قبل» over the left side (the original, same layout as «Before» in en), «بعد» right; size «960 × 1280 ← 3840 × 5120» read right to left, each value «3840 × 5120» intact. Enhance (ar): «IMG_20261016_212159.jpg» whole, sizes intact in the photo row and the scale row. About (ar crop): «16 (API 36)», «Mali-G615 MC6», «MikalaiKryvusha/ncnn 20260526-kartun.1» whole. New: the scale slider's active track fills the RIGHT half (thumb → ×16) while its labels stay ×1 left … ×16 right (Found 2) |
| F9 | pass | Models (ru), top: chips end ~y 370, first card starts ~y 405 (full-res px). After a 300 px scroll the cards clip at a line ~30 px below the chips (crop `f9_gap_crop`): visible gap, no card under the chips |
| F10 | pass | Landscape 2560 × 1600 (ru): two columns; chips «Фото → Поделиться → KARTUN» whole and readable under the share text. French portrait: «Choisissez une photo» on one line, centred in the card (x 250–1000 of 1250 in the shot), subtitle centred too |
| F11 | pass | German Settings: «GPU-Beschleunigung» on one line, whole (`f11_settings_de_crop`). Note: the Titan's width never forces a break, so the hyphenation path itself is not exercised here (phone-width devices check that) |
| F12 | pass | `logcat -d -b crash`: 0 lines since 21:21 (both runs, locale switches, rotation included) |

## 5. Found

No bug documents were filed (the run's brief forbids editing other documents); each defect is written for a bug doc. Both
are outside the RC2 fix list.

- **1 · The before/after divider line shows through the result panel.** Description: on the result screen the pink
  vertical divider line continues faintly through the bottom info panel (across the «Saved to Pictures/KARTUN» banner and
  between Save and Share). Steps: Enhance any photo → result screen; look at x = 800 of the panel. Expected: the panel is
  opaque above the comparison, no line inside it. Actual: a dim pink line through the panel at the divider's x
  (`f3_result_en.webp`, crop `f3_result_de_panel.webp`). Cosmetic; the RC1 screenshot is too small to say if it is new.
- **2 · Arabic: the scale slider's fill is mirrored but its labels are not.** Description: in RTL the slider's active track
  runs from the thumb at 4× to the right end, under the labels ×8 / ×16, while the labels keep ×1 on the left and ×16 on the
  right. Steps: `cmd locale set-app-locales com.kriniks.kartun --locales ar`; open a photo; look at the scale slider at 4×.
  Expected: fill and labels agree (fill from ×1 to the thumb — either both mirrored or neither). Actual: fill over ×8–×16
  with ×1–×4 empty (`f8_enhance_ar.webp`); the size line correctly says 3840 × 5120. Minor, Arabic only.

## 6. Traces

All in `C:\Users\krinik\AppData\Local\Temp\rc2\titan\`:

- F0: `f0_about_en.webp` · F1: `f1_bench_dialog.webp`, `f1_logcat.txt`
- F2: `f2_enhance_en.webp`, `f2_tree_save.txt`, test photo `titan_rc2.jpg` (local copy; device copy deleted)
- F3: `f3_enhance_en_scrolled.webp`, `f3_result_en.webp`, `f3_result_de.webp`, `f3_result_de_panel.webp`, `f3_enhance_de.webp`
- F4: `f4_logcat.txt` (engine tiles), `f4_tree_perm.txt` · F5: `f5_run_en_a.webp`, `f5_run_en_b.webp`, `f5_run_de_a.webp`, `f5_run_de_a_panel.webp`, `f5_run_de_b.webp`
- F6: `f6_enhance_ru.webp`, `f6_enhance_{en,de,ru}_btn.webp`, `f6_buttons.webp` · F7: `f7_share_sheet.webp`
- F8: `f8_result_ar.webp`, `f8_enhance_ar.webp`, `f8_about_ar.webp`, `f8_about_ar_crop.webp`, `f8_about_ar_crop2.webp`, `f8_tree_home.txt`
- F9: `f9_models_top.webp`, `f9_models_scrolled.webp`, `f9_gap_crop.webp` · F10: `f10_home_land.webp`, `f10_home_fr.webp`
- F11: `f11_settings_de.webp`, `f11_settings_de_crop.webp` · F12: `f12_crash.txt` (empty)
- Helper: `titan_env.sh` (adb/ui/shot shell functions)

## 7. Verdict

`pass` — all thirteen RC2 checks hold on the Titan (version 268, bench 6.49 s with the ESRGAN probe, IMG_ names, sizes on one
line, estimate within 0.88–0.94, panel/button/share/Arabic/list/home/German fixes visible, crash buffer empty); the two
minor cosmetic defects found are outside the RC2 fix list.
