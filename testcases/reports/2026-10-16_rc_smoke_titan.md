# Test run report — release 1.0 RC smoke on the HEADWOLF Titan 1

**Created:** 2026-10-16 20:56 +03:00 · **Run by:** the agent (Claude Opus 5.5, subagent of the RC smoke coordinator) ·
**Version/build:** `C:\Users\krinik\AppData\Local\Temp\rc\kartun-rc-2028.apk` — release `com.kriniks.kartun`, badging
`versionCode='1' versionName='0.1 (1)'`, minSdk 34, targetSdk 37, engine `MikalaiKryvusha/ncnn 20260526-kartun.1`

## 1. Work

The release 1.0 RC smoke (`plans/08_release_1_0.md` step 4) on one device by the case set
`testcases/TC_release_1_0_rc_smoke.md` (C1–C13), plus C14 added by the coordinator mid-run (the owner: look at the app's
screens in every supported language — en, ru, es, pt, fr, de, zh, hi, bn, ar; home, Enhance, Settings, About each).

## 2. Contour

HEADWOLF Titan 1 tablet, Android 16 (API 36), Mali-G615 MC6, 1600 × 2560, system language Russian (ru-BY), adb
`192.168.1.3:5555` over Wi-Fi. The RC APK installed over the previous build with `adb install -r` (no rebuild). UI driven
by the UI agent `C:\Android\private\ui-agent\ui.py` on port 8381 (locators: `wait`, `tapText`, `labels`, `tree`, `back`,
`type`; taps by coordinates only on nodes taken from `tree`; two scroll swipes with `adb shell input swipe`). Test photo:
`engine/third_party/testdata/photo1.jpg` stripped of EXIF → `/sdcard/Pictures/KARTUN_test/kartun_smoke.jpg` (960 × 1280,
pushed 20:39:01, file time 20:38:47, MediaStore `_id=63346`), picked by its description «Фото, дата и время съемки:
16 окт. 2026 г. 20:38». App data cleared (`pm clear`) for C2/C3 and once more before C4. Network used only in C7 and C8.
REAL WORLD: this is the real release-signed RC on a real device; the only difference from a user's phone is the adb/agent harness.

## 3. Runs

One pass, cases in order, then C14. Shell: Git Bash with `A=C:/Android/Sdk/platform-tools/adb.exe; S=192.168.1.3:5555;
MSYS_NO_PATHCONV=1`; UI commands `python C:\Android\private\ui-agent\ui.py --serial 192.168.1.3:5555 --port 8381 "<cmd>"`
(abbreviated `ui "<cmd>"` below); scripted steps in `python -I -X utf8 <scratchpad>/c3_cancel.py | c12_cancel.py | c14_langs.py`.

| # | Moment | Command | Exit / outcome |
|---|---|---|---|
| C1 | 2026-10-16 20:36 +03:00 | `$A -s $S install -r C:/Users/krinik/AppData/Local/Temp/rc/kartun-rc-2028.apk; $A -s $S shell dumpsys package com.kriniks.kartun \| grep -E "versionName\|minSdk\|versionCode"` | 0 · `Success`; `versionCode=1 minSdk=34 targetSdk=37`, `versionName=0.1 (1)` |
| C2 | 2026-10-16 20:37 +03:00 | `$A -s $S shell pm clear com.kriniks.kartun; $A -s $S logcat -c; $A -s $S logcat -b crash -c; $A -s $S shell am start -n com.kriniks.kartun/.MainActivity; sleep 2; $A -s $S exec-out screencap -p > c2_bench_dialog.png` | 0 · dialog on screen at +2 s; bench line at +3.77 s |
| C3 | 2026-10-16 20:38 +03:00 | `python -I -X utf8 c3_cancel.py` (pm clear; logcat -c; am start; `wait Отмена 5000`; `tapText Отмена`; `labels`), then `am force-stop` + `am start` + screenshot | 0 · dialog closed by Cancel; no bench line; relaunch without dialog (a first attempt at 20:37 from PowerShell tapped at +1.8 s; the scripted retry is the run of record) |
| C2b | 2026-10-16 20:38 +03:00 | `$A -s $S shell pm clear com.kriniks.kartun; $A -s $S logcat -c; $A -s $S shell am start -n com.kriniks.kartun/.MainActivity; sleep 8; $A -s $S logcat -d -v time \| grep -E "Start proc.*kriniks\|\"bench\""` | 0 · second full bench so that C4's estimate is calibrated (C3 left the app with a cancelled bench) |
| C4 | 2026-10-16 20:39 +03:00 | `magick photo1.jpg -strip kartun_smoke.jpg; $A -s $S push … /sdcard/Pictures/KARTUN_test/kartun_smoke.jpg; $A -s $S shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Pictures/KARTUN_test/kartun_smoke.jpg`; `ui "tapText Выберите фото"`, `ui "tapText 20:38"`, `ui "tap 1382 2333"` (Готово, node from `tree`), `ui "tapText Улучшить ·"`, `ui "tap 799 1371"` (Разрешить, node from `tree`), `ui "waitGone Отменить 150000"` | 0 · Enhance 960 × 1280 → 3840 × 5120, estimate ≈ 1 min 1 s; result 1 min 7 s (20:40:03 → 20:41:12) |
| C5 | 2026-10-16 20:41 +03:00 | ×3: `ui "tapText Сохранить"`, `ui "tapText PNG\|JPEG\|WEBP"`, `ui "tap 848 1658"` / `ui "tap 848 1796"` (dialog Save node from `tree`); then `$A -s $S shell "content query --uri content://media/external/images/media --projection _display_name:width:height:_size --where \"relative_path LIKE 'Pictures/KARTUN/%' AND _display_name LIKE '63346%'\""` | 0 · three rows 3840 × 5120 |
| C6 | 2026-10-16 20:42 +03:00 | `ui "tapText Поделиться"`, `labels`, screenshots at once and ~25 s later, `ui "back"` | 0 · sheet opened, nothing sent; no file name/preview on the sheet |
| C7 | 2026-10-16 20:43 +03:00 | `ui "back"` ×2, `ui "tapText Вставить ссылку на фото"`, `ui "type https://raw.githubusercontent.com/MikalaiKryvusha/KARTUN/main/docs/model-previews/4x-NMKD-Siax-fp16.webp"`, `ui "tapText Скачать"`, `ui "wait Улучшение 20000"`, `labels` | 0 · Enhance with 1024 × 512 → 4096 × 2048 |
| C8 | 2026-10-16 20:43 +03:00 | `ui "tapText Все модели"`, `tree`, `ui "tap 1307 1691"` («Скачать» node of «4x UltraSharp V2 Lite»), `ui "waitGone из 15 МБ 120000"`, `$A -s $S shell input swipe 800 2000 800 1300 400`, `tree`; `$env:KARTUN_SERIAL='192.168.1.3:5555'; node D:\work\ai_sandbox\KARTUN\tools\kartun-cli.mjs info` | 0 · download 20:44:00 → ≤ 20:44:05; card shows «Удалить»; CLI `"installed":true` |
| C9 | 2026-10-16 20:45 +03:00 | `ui "tapText Настройки"`, `ui "tapText О приложении"`, `labels`, screenshot | 0 · About rows as expected |
| C10 | 2026-10-16 20:45 +03:00 | `$A -s $S shell cmd locale set-app-locales com.kriniks.kartun --locales ru` → home + About screenshots; `… --locales ar` → About, Settings, home screenshots; `… --locales "''"` | 0 · Russian text; Arabic mirrored (two bidi defects, see Found) |
| C11 | 2026-10-16 20:46 +03:00 | `$A -s $S shell settings put system accelerometer_rotation 0; $A -s $S shell settings put system user_rotation 1`; screenshots of home and Enhance (+ one scroll of the right pane); `settings put system user_rotation 0; settings put system accelerometer_rotation 1` (old values 1 / 0, read back 1 / 0) | 0 · layouts fit |
| C12 | 2026-10-16 20:47 +03:00 | `python -I -X utf8 c12_cancel.py` (`tapText Улучшить ·`, poll `labels` until the ring ≥ 10 %, `tapText Отменить`); then `ui "tapText Улучшить ·"` + `ui "waitGone Нейросеть 50000"` | 0 · cancelled at 11 % (20:47:35), back on Enhance; second run to the result 1 min 7 s (20:47:54 → 20:49:03) |
| C13 | 2026-10-16 20:49 +03:00 | `$A -s $S logcat -d -b crash > c13_crash.txt; grep -c kriniks c13_crash.txt` (buffer cleared at 20:37 in C2) | 0 · 0 lines; re-checked 20:56 after C14: 0 lines |
| C14 | 2026-10-16 20:49 +03:00 | `python -I -X utf8 c14_langs.py en`, then `… ru es pt fr de zh hi bn ar` (per language: `cmd locale set-app-locales … --locales <lang>`, `am force-stop`, `am start`, home shot, pick photo «20:38» → «Готово» → Enhance shot, `back`, Settings shot, About shot; labels to `c14_labels.txt`); 20:55 a full-resolution re-shot of the German Enhance card; `cmd locale set-app-locales com.kriniks.kartun --locales "''"` | 0 · 40 screens taken and read, finished 20:54; locale reset to `[]` |
| cleanup | 2026-10-16 20:56 +03:00 | `$A -s $S shell content delete --uri content://media/external/images/media/<id>` for 63346 (kartun_smoke.jpg), 63347–63349 (the three `63346_kartun_*` results); `ls` both folders | 0 · only my four files removed; `KARTUN_test/` holds its five older files |

## 4. Checks

Hygiene: NONE (no code changed; a device smoke run only)
Functional run: the RC walked as a user on the Titan (Android 16, release build): first-launch bench and its cancel,
gallery pick → Enhance → result, save in three formats, share, paste a link, catalogue download, About, ru/ar, landscape,
cancel processing, crash buffer, then home/Enhance/Settings/About in all ten languages — READ: the screens through `labels`/
`tree` and the screenshots (every key .webp opened and looked at), logcat (`Start proc`, `{"bench":true,…}`, engine tiles),
the MediaStore rows, `kartun-cli info`, the crash buffer.

| Case | Status | Observation |
|---|---|---|
| C1 | pass | `Success`; `dumpsys`: `minSdk=34`, `targetSdk=37`. Version is `0.1 (1)` / versionCode 1 — see Found 1 |
| C2 | pass | +2 s screenshot: dimmed home, dialog «Измеряем производительность GPU и CPU устройства…» (Russian = device language), the pixel-heart spinner, «Отмена». Dialog closed by itself (`labels` at ~+4 s: home only). Logcat `20:37:15.861 Start proc` → `20:37:19.626 {"bench":true,"gpu_ms":872.0,"cpu_ms":585.0,"gpu_factor":0.7716814,"cpu_factor":0.60559005}` = **3.77 s**. Second run 20:38:47.054 → 20:38:50.827 = 3.77 s, gpu 861 / cpu 583 ms, factors 0.762 / 0.604 |
| C3 | pass | Dialog found 1.19 s after `am start` (it appears ~1.1 s after start), Cancel tapped at 1.92 s, i.e. ≈ 0.7 s after it showed; `labels` 0.5 s later: home, no dialog. No `{"bench":…}` line (engine finished its 512 px pass in the background at 20:38:19.2, result not recorded). Crash buffer 0. Force-stop + relaunch: home with no dialog (`labels` + c3_relaunch.webp) |
| C4 | pass | Picked by «… 16 окт. 2026 г. 20:38» (unique); multi-select picker → «Готово» node. Enhance: «63346.jpg · 960 × 1280 → 3840 × 5120», model 4xNomosWebPhoto RealPLKSR, button «Улучшить · ≈ 1 минута 1 секунда» (61 s). Notification dialog → «Разрешить» node. Result: «960 × 1280 → 3840 × 5120 · 1 минута 7 секунд · GPU · Vulkan». **Estimate 61 s vs real 67 s → real/estimate 1.10** (engine: 35 tiles, 20:40:0x → 20:41:12). After the run the estimate self-corrected to ≈ 1 min 4 s; the C12 repeat took 67 s again (ratio 1.05). The picker hands the file over as `63346.jpg` (MediaStore id), not `kartun_smoke.jpg`, so results are named `63346_kartun_…` |
| C5 | pass | Dialog «Файл ≈ 38,0 МБ» (PNG), «≈ 7,0 МБ» (JPEG q92), «≈ 6,5 МБ» (WEBP q92); «Сохранено в Pictures/KARTUN» each time. MediaStore: `…_4x.png` 3840 × 5120 40 399 931 B (38.5 MiB), `…_4x.jpg` 3840 × 5120 6 987 595 B (6.66 MiB), `…_4x.webp` 3840 × 5120 6 444 818 B (6.15 MiB) — all within ~5 % of the dialog |
| C6 | fail | System sheet (com.android.intentresolver) opened with the title «Отправка изображения» and the target apps; **no file name and no image preview**, also ~25 s later. `back` closed it, nothing sent. See Found 2 |
| C7 | pass | Field took the URL; «Скачать» → Enhance «4x-NMKD-Siax-fp16.webp · 1024 × 512 → 4096 × 2048», estimate ≈ 27 s |
| C8 | pass | Catalogue (14 models, 2 on device); «Скачать» on «4x UltraSharp V2 Lite» → ring «0,0 из 15 МБ» → done in ≤ 5 s; card shows the tick badge and, below its preview, «Удалить» (`tree` + c8_lite_delete.webp); Enhance then says «на устройстве: 3»; `kartun-cli info`: `4x-UltraSharpV2-Lite-fp16 … "installed":true` |
| C9 | pass | About (ru): «Движок MikalaiKryvusha/ncnn 20260526-kartun.1»; row «MikalaiKryvusha/ncnn — кастомный модифицированный и улучшенный форк»; GPU «Mali-G615 MC6»; Android «16 (API 36)»; device «HEADWOLF Titan 1»; «Версия 0.1 (1)». In English (C14): «Engine MikalaiKryvusha/ncnn 20260526-kartun.1», «MikalaiKryvusha/ncnn — custom modified and improved fork» |
| C10 | pass (with 2 minor defects) | ru: home and About fully Russian. ar: back arrow on the right («رجوع» node at x 1474), chevrons point left, settings rows mirrored, home mirrored, nothing cut. Bidi defects in Arabic: About shows «(API 36) 16» and Enhance shows the file name as «jpg.63346» — Found 3 |
| C11 | pass | Landscape 2560 × 1600: home two-column (pick card left, link + share card right), Enhance photo left / options right with «Улучшить» pinned; the right pane scrolls to «Все модели», the scale slider and «960 × 1280 → 3840 × 5120 · 19,7 Мп»; nothing cut (the licence chip «… · CC BY 4.0» wraps to two lines — cosmetic, not a defect). Rotation restored to 1 / 0 |
| C12 | pass | Ring «14%», tile 5/35 when Cancel pressed; engine log stops after tile 6/35 (20:47:36); screen back to Enhance (`tree` 20:47:42), «Уведомление KARTUN» gone, crash buffer 0, same PID alive. Second Enhance ran to the result: «1 минута 7 секунд · GPU · Vulkan» |
| C13 | pass | `logcat -d -b crash`: 0 lines (since 20:37), also after C14 at 20:56 |
| C14 | pass (with minor defects) | 10 languages × 4 screens read; all text in the right language, nothing cut; Arabic mirrored. Defects: Arabic bidi (Found 3), German model description touching the selected tick (Found 4). Result screen skipped: each result costs ~65 s of GPU time per language (and the 1024 × 512 link photo ~27 s), so it is not «cheap»; result screens were seen in Russian only (C4, C12) |

C14 sub-table (`c14_<lang>_<screen>.webp`):

| Lang | home | Enhance | Settings | About |
|---|---|---|---|---|
| en | ok | ok | ok | ok |
| ru | ok | ok | ok | ok |
| es | ok | ok | ok | ok |
| pt | ok | ok (description ends ~2 px from the tick — tight but not touching) | ok (section header «APP») | ok |
| fr | ok | ok | ok | ok |
| de | ok | defect: «≈ 1 Minute 5 Sekunden» runs into the selected-model tick, no gap (Found 4; full-res crop `c14_de_enhance_card_crop.webp`) | ok | ok («Engine» / «INSPIRATION UND ENGINE» — accepted German tech loanword) |
| zh | ok | ok | ok | ok |
| hi | ok | ok («19.7 MP» unit in Latin) | ok | ok |
| bn | ok | ok (Bengali digits in «১৯.৭ MP», «১ মিনিট», «মডেল: ১৪» but Latin digits in «960 × 1280 → 3840 × 5120» and the slider — mixed digit systems on one screen, cosmetic) | ok | ok |
| ar | ok (mirrored; gear left, logo right; flow «صورة ← مشاركة ← KARTUN») | defect: file name «jpg.63346»; size line rendered «5120 × 3840 ← 1280 × 960» (Found 3) | ok (mirrored, chevrons left) | defect: Android «(API 36) 16» (Found 3) |

## 5. Found

No bug documents were filed (the run's brief forbids editing other documents); each defect below is written for a bug doc.

- **1 · RC carries version `0.1 (1)` / versionCode 1.** Description: the RC APK's badging is `versionCode='1' versionName='0.1 (1)'`, while `app/build.gradle.kts` derives `buildNumber` from `git rev-list --count HEAD` (267 now) and falls back to 1 on failure — the RC was built with the fallback. Steps: `aapt2 dump badging kartun-rc-2028.apk`; or Settings → About. Expected: versionCode = commit count (267), and per `plans/08` step 8 `versionName = "1.0 (<count>)"` for the release. Actual: `0.1 (1)`, About shows «Версия 0.1 (1)»; an update over any earlier build cannot be seen as newer.
- **2 · Share sheet shows no file name or preview on stock Android 16.** Description: «Поделиться» on the result opens the system sheet without the image's name or thumbnail (the Redmi's MIUI sheet showed `…_4x.png`). Steps: result screen → «Поделиться». Expected (C6): the sheet shows the file name. Actual: title «Отправка изображения», target apps only, no preview, still none ~25 s later. Likely cause to check: `ResultScreen.kt` sends `ACTION_SEND` with only `EXTRA_STREAM` — no `ClipData` with a label / `EXTRA_TITLE`, which the AOSP IntentResolver uses for the preview row.
- **3 · Arabic bidi: LTR values reorder inside RTL text.** Description: left-to-right strings are not isolated in the Arabic layout. Steps: `cmd locale set-app-locales com.kriniks.kartun --locales ar`; open a photo; Settings → About. Expected: «63346.jpg», «16 (API 36)», «960 × 1280 → 3840 × 5120» readable as in LTR (wrapped in LRI/PDI or `TextDirection.Ltr`). Actual: Enhance title «jpg.63346»; About Android «(API 36) 16»; size line «5120 × 3840 ← 1280 × 960» (mirrored as a whole, arguable).
- **4 · German: the selected model's description runs into its tick.** Description: on Enhance the first model card's description «Für Fotos aus dem Netz, komprimiert und unscharf · ≈ 1 Minute 5 Sekunden» ends exactly at the pink tick circle with no gap. Steps: locale `de`; open a photo; look at the selected «4xNomosWebPhoto RealPLKSR» card. Expected: a gap between text and tick (the text wraps earlier). Actual: «Sekunden» touches the tick (full-res crop `c14_de_enhance_card_crop.webp`); Portuguese is ~2 px short of the same.

## 6. Traces

All in `C:\Users\krinik\AppData\Local\Temp\rc_smoke\titan\`:

- C2: `c2_bench_dialog.webp`, `c2_logcat.txt` · C3: `c3_logcat.txt`, `c3_logcat_run2.txt`, `c3_relaunch.webp`
- C4: `c4_enhance.webp`, `c4_result.webp`, `c4_logcat.txt`, test photo `kartun_smoke.jpg`
- C5: `c5_save_dialog_png.webp` · C6: `c6_share_sheet.webp`, `c6_share_sheet_later.webp`
- C7: `c7_link_enhance.webp` · C8: `c8_catalogue_after.webp`, `c8_lite_delete.webp` · C9: `c9_about_ru.webp`
- C10: `c10_home_ru.webp`, `c10_about_ru.webp`, `c10_about_ar.webp`, `c10_settings_ar.webp`, `c10_home_ar.webp`
- C11: `c11_home_land.webp`, `c11_enhance_land.webp`, `c11_enhance_land_scrolled.webp`
- C12: `c12_logcat.txt`, `c12_second_result.webp` · C13: `c13_crash.txt` (empty)
- C14: `c14_<en|ru|es|pt|fr|de|zh|hi|bn|ar>_<home|enhance|settings|about>.webp` (40 files), `c14_labels.txt` (the `labels` of every screen), `c14_de_enhance_card_crop.webp`, montages `montage\m_<lang>.png`, crops `montage\crop_ar.png`, `montage\crop_cards.png`
- Scripts: `C:\Users\krinik\AppData\Local\Temp\claude\d--work-ai-sandbox-KARTUN\88dec3f0-d75f-4dbf-8a3f-c708b82eaaac\scratchpad\{c3_cancel.py,c12_cancel.py,c14_langs.py,c14_de_full.py}`

## 7. Verdict

`partial` — the main path, bench, cancel paths, saving, link, catalogue, About, landscape, languages and the crash journal
all work on the Titan with no crash and a well-calibrated estimate (real/estimate 1.10, then 1.05), but C6 fails its
expectation (no file name on the stock share sheet) and the RC carries version `0.1 (1)`, plus two minor layout defects (Arabic bidi, German tick).
