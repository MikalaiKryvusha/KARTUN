# Test run report — Phase 2, slice 1: the main path on screens, walked by touch on the Titan

**Created:** 2026-10-14 18:47 +03:00 · **Run by:** the agent (Claude Opus 5.5) · **Version/build:** debug APK
`com.kriniks.kartun.debug` 0.1 (1), this commit (screens of `plans/03_epic01_F2_screens.md` steps 1–6, engine with the
tile listener)

## 1. Work

`plans/03_epic01_F2_screens.md`, acceptance criteria 1–4 and 6 (criterion 5 — the look — is the owner's verdict):
home → system photo picker → setup screen → upscale with the percent ring (`[OWNER]` interview #003, Q1 = D) → result
with the before/after divider grabbed anywhere. Basis: the accepted mockups `design/kartun-mockups.html` (variant A v2).

## 2. Contour

HEADWOLF Titan 1 (Android 16, Mali-G615 MC6) over adb `100.99.111.48:42661`. Input: the owner's photo 960×1280 (the
phase 0 `photo1.jpg`), copied into the Titan gallery as `Pictures/KARTUN_test/kartun_test_photo1.jpg` for the picker
(test data; removed 2026-10-14 19:06 with `content delete` + `rm -rf /sdcard/Pictures/KARTUN_test` — `HOUSE_RULES.md` R1). The picker is driven by `adb shell input
tap` at the screen coordinates read from screenshots — taps, not intents; the user's path.

## 3. Runs

| # | Moment | Command | Exit / outcome |
|---|---|---|---|
| 1 | 2026-10-14 18:37 +03:00 | `adb install -r app-debug.apk` + `am start` + `screencap` | home screen; defect D1 (titles black) |
| 2 | 2026-10-14 18:39 +03:00 | `input tap 800 960` (pick card) → `input tap 265 1275` (photo) → `input tap 1380 2333` («Готово») | picker opened without a permission dialog; setup screen with «960 × 1280 · 1,2 Мп», «Улучшить · ≈ 8 с» |
| 3 | 2026-10-14 18:40 +03:00 | `input swipe 800 2000 800 1300 600` | preview shrinks; slider at «2× · по умолч.», «960 × 1280 → 1920 × 2560 · 4,9 Мп»; defect D2 (slider look/labels) |
| 4 | 2026-10-14 18:41 +03:00 | `input tap 800 1330` (4× on the track) → `input tap 800 2325` («Улучшить») | processing: «28%», «Тайл 10 из 35 · осталось ≈ 8 с», «GPU · Vulkan»; defect D3 (photo under the sheet); result «960 × 1280 → 3840 × 5120», «9 с · GPU · Vulkan» |
| 5 | ≈ 2026-10-14 18:42 +03:00 | `input swipe 400 1300 1200 1300 600`; `input swipe 1200 1000 300 1000 600` | divider pixels at x = 1189–1191 and 297–299 (row y = 1800, `magick … txt:-`) |
| 6 | 2026-10-14 18:44 +03:00 | path of run 2, `input tap 800 1912` (x4plus) → «Улучшить» → `input tap 800 2300` («Отменить») | «Улучшить · ≈ 1:50»; processing «8%», «Тайл 3 из 35 · осталось ≈ 2:10»; cancel → back on «Улучшение» |
| 7 | 2026-10-14 18:44 +03:00 | `input tap 1478 1335` ((i) of «Модель») | sheet «Модель» with the manual's text and «Закрыть» |
| 8 | ≈ 2026-10-14 18:45 +03:00 | `node tools/titan-probe.mjs --in test256.png --model realesr-general-x4v3 --gpu true --tile 200 --ref engine/third_party/testdata/torch_gx4v3_256.png` | ok · 398 ms · 52.30 dB — the adb probe still works after the move to `EngineProbe.kt` |
| 9 | 2026-10-14 18:46 +03:00 | full path of run 2 at the default 2× → `uiautomator dump` | result «960 × 1280 → 1920 × 2560», «8 с · GPU · Vulkan»; `logcat` FATAL/SIGSEGV count 0 |
| 10 | 2026-10-14 18:47 +03:00 | `am start -a android.intent.action.SEND -t image/jpeg --eu android.intent.extra.STREAM content://media/external/images/media/63257 --grant-read-uri-permission -n …/.MainActivity` | home with «Не удалось открыть это фото» — the adb shell cannot grant a read on another app's media; NOT the user's path, see run 13 |
| 11 | 2026-10-14 18:50 +03:00 | build with the owner's three remarks (collapsing preview, (i) dialogs, model preload) → `pm clear` → path of run 2 → `input swipe 800 2000 800 1700 400`, `input swipe 800 2100 800 1100 500`, `input swipe 800 1100 800 2100 500` | 4 frames: preview 330 dp → shrunk with the settings following it → compact row → full again; no jump; the fast model ready (tick) 0.6 s after the photo |
| 12 | 2026-10-14 18:54 +03:00 | build with the centred «Фото → Поделиться → KARTUN» → home screenshot → path of run 2 → `input tap 1478 1335` | home row centred; (i) opens a centred dialog «Модель» with «Закрыть»; FATAL/SIGSEGV count 0 |
| 13 | 2026-10-14 18:53 +03:00 | Google Photos: `am start -a VIEW -d content://media/external/images/media/63257 -p com.google.android.apps.photos` → tap «Поделиться» → «Оригинал» → «Поделиться» → swipe the chooser → tap «Debug KARTUN» | KARTUN opens «Улучшение» with «960 × 1280 · 1,2 Мп» — share-in works on the user's path |
| 14 | 2026-10-14 18:59 +03:00 | build with the vector icon (`python tools/make-vector-icon.py --apply`) → `am start` + `screencap` 0.15 s later; `input keyevent 3`, swipe up to the app list | splash: the vector glass heart on the dark field; app list: «Debug KARTUN» with the heart in the OS circle (`design/icon/compare_vector.png`) |
| 15 | 2026-10-14 19:00 +03:00 | path of run 2 → `uiautomator dump`; tap the x4plus card → dump; back to the fast card → «Улучшить» → dump on the result | «Улучшить · ≈ 7 секунд», «Улучшить · ≈ 1 минута 49 секунд», «8 секунд · GPU · Vulkan» — durations in whole words with plural forms |
| 16 | 2026-10-14 19:02 +03:00 | build with the pull-down (plans/03 step 9) → path of run 2 → `input swipe 800 1400 800 2300 1500` (on the settings) → `input swipe 800 1200 800 1900 400` | the photo opens full screen on black; the vertical swipe closes it back to «Улучшение» |
| 17 | 2026-10-14 19:05 +03:00 | build with the draggable preview → `input swipe 800 500 800 1300 1500` (on the photo) → `input keyevent 4` → `input swipe 800 1000 800 300 800` (photo up) | pull on the photo opens full screen (empty dump), Back closes it; dragging the photo up shrinks it; FATAL count 0 |

## 4. Checks

Hygiene: `assembleDebug` exit 0; `grep -rnE 'Text\("[^"]' app/src/main/kotlin` prints only the `"i"` glyph; `aapt2 dump
permissions` lists no `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`; no unit tests.
Functional run: the main path walked on the Titan by taps and swipes, screens READ from screenshots and uiautomator
dumps (`captures/`, local only), the divider position READ from screenshot pixels.

| Case | Status | Observation |
|---|---|---|
| C1 home → picker, no permission | pass | run 2; manifest without storage permissions |
| C2 setup screen content | pass | run 2–3: size chip, two model cards (fast selected), slider at 2×, result size line |
| C3 upscale with percent ring and estimate | pass | run 4, 6, 9: estimate on the button, percent ring + tile line during the run, result with size and time |
| C4 divider grabbed anywhere | pass | run 5: line within 11 px of the finger |
| C5 cancel between tiles | pass | run 6 |
| C6 strings only in resources | pass | grep above |
| C7 pinch zoom and pan | not run | `adb shell input` has no multi-touch; left for the owner's hands or a later instrumented test |
| C8 photo over 12.5 MP, the «слишком большое» notice | not run | no such photo in the test set |
| C9 share-in from another app | pass | run 13 (Google Photos); run 10 is an adb limit, not a defect |
| C11 durations in whole words (owner, ≈ 18:58) | pass | run 15 |
| C12 pull-down to full screen (owner, ≈ 18:57) | pass | runs 16–17; pinch inside the viewer not run (no multi-touch in adb) |
| C10 owner's remarks 18:48–18:50 | pass | run 11 (no jump, preload), run 12 (dialogs, centred row) — the look stays his verdict |

## 5. Found

- D1 — titles drawn black (no default content colour on plain Boxes) → `LocalContentColor` in `KartunTheme`; fixed, seen
  white in run 2.
- D2 — Material 3 slider showed a bar thumb and labels off the ticks → own slider like the mockup's `.kscale`; fixed, run 3.
- D3 — processing photo partly under the bottom sheet (fixed bottom padding) → column layout; fixed, run 6.
- D4 (the owner's eye, 2026-10-14 ≈ 18:48) — «прыгает картинка на экране Улучшение»: the preview shrank BY the scroll
  offset, so the settings' viewport changed height under the finger → collapsing header on a NestedScrollConnection;
  fixed, run 11.

## 6. Traces

Screenshots `captures/s1_home.png` … `captures/s12_info_model.png` (gitignored, local); run 8's JSON line above.

## 7. Verdict

partial — criteria 1–4 and 6 of plan 03 pass on the Titan by touch; C7 (pinch) and C8 (large photo) are not run, and
criterion 5 (the look next to the mockups) waits for the owner's eye.
