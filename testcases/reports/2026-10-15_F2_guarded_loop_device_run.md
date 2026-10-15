# Test run report — the guarded-loop backlog (TC_F2) and the tail of `ea9d205`, walked on the Titan

**Created:** 2026-10-15 11:20 +03:00 · **Run by:** the agent (Claude Opus 5.5) · **Version/build:** debug APK
`com.kriniks.kartun.debug` 0.1 (1): run 1 on the build installed 2026-10-14 23:25 (`ea9d205`), runs 2+ on `ea9d205` + the
`KARTUN_PANEL` debug logs removed (this commit)

## 1. Work

The case set `testcases/TC_F2_guarded_loop_device_backlog.md` (25 cases + K1–K3) and the open control of `ea9d205`
(STATUS «Где продолжить» 2): a gesture 55 dp from the divider but far above the knob must NOT move the line; the same
55 dp at the knob's height must (the invisible 72 dp grab zone, the owner 2026-10-14 ≈ 23:05 +03:00: «вокруг драга до
после нужно сделать невидимую более широкую область, за которую его можно хватать»).

## 2. Contour

HEADWOLF Titan 1 (Android 16, Mali-G615 MC6, 1600×2560, density 480 = 3 px per dp) over adb `100.99.111.48:35437`
(Tailscale). Test data: `Pictures/KARTUN_test/beach.png` (256×256, left from 2026-10-14) and `Pictures/KARTUN_test/photo1.jpg`
(960×1280, the phase 0 photo, pushed 2026-10-15 11:11) — both removed after the run (`HOUSE_RULES.md` R1). The UI is
driven by the on-device UI agent (`C:\Android\private\ui-agent`, `HOUSE_RULES.md` §6) through the system photo picker — the
user's path; swipes by `adb shell input swipe`; the divider's x is read from screencap row y = 900 (pink `#FF1A8C` run).

## 3. Runs

| # | Moment | Command | Exit / outcome |
|---|---|---|---|
| 1 | 2026-10-15 11:10 +03:00 | picker → `beach.png` → «Улучшить» (2×) → `python knob_control.py 100.99.111.48:35437 <dir>` (swipes `965,500→1365,500` and `965,1280→1300,1280`) | old build: line 800 → 800 (control) → 1302 (knob zone) |
| 2 | 2026-10-15 11:11 +03:00 | `.\gradlew.bat :app:testDebugUnitTest assembleDebug` → `adb install -r app-debug.apk` | 0 · 28 JVM tests, 0 failures; installed |
| 3 | 2026-10-15 11:18 +03:00 | `python s_c5.py` (home → picker → `photo1.jpg` → «Улучшить» at the default 2× → result) | «960 × 1280 · 1,2 Мп», «Улучшить · ≈ 8 секунд»; processing «Тайл 6 … 14 из 35», «GPU · Vulkan»; result «960 × 1280 → 1920 × 2560», «8 секунд · GPU · Vulkan» in 10.2 s |
| 4 | 2026-10-15 11:18 +03:00 | `python knob_control.py 100.99.111.48:35437 <dir>` on the new build | line 800 → 800 (control) → 1301 (knob zone); `logcat` FATAL/SIGSEGV count 0 |
| 5 | 2026-10-15 11:21 +03:00 | `python s_c11.py` (dialog → PNG, then JPEG, then WEBP → the dialog's «Сохранить»; `ls /sdcard/Pictures/KARTUN` after each) | three new files `63282_kartun_realesr-general-x4v3_2x.{png,jpg,webp}` 8 082 198 / 3 340 559 / 1 896 150 bytes, all 1920×2560 (`magick identify` on pulled copies); the original's mtime and size unchanged |
| 6 | 2026-10-15 11:23 +03:00 | `python s_c13.py` (tap «Поделиться», poll `mCurrentFocus`, screenshot, Back) | `ChooserActivityLauncher` in front after 2.2 s, sheet «Отправка изображения»; closed with Back, nothing sent |
| 7 | 2026-10-15 11:27 +03:00 | `python s_c8.py` (scroll the settings, tap the track at 16×) | «960 × 1280 → 15 360 × 20 480 · 314,6 Мп», notice «Результат больше 200 Мп — для этого фото подойдёт масштаб до 8×», «Улучшить» grey and inert |
| 8 | 2026-10-15 11:33 +03:00 | `python s_c8b.py --run` (track at 8× → «Улучшить») | «2 прохода · дольше · большой файл»; cards ≈ 57 s / ≈ 9 min 5 s (2×: ≈ 11 s / ≈ 1 min 49 s); «Проход 1 из 2 · тайл 5 … 32 из 165», then «Проход 2 из 2 · тайл 35 … 162 из 165»; result «960 × 1280 → 7680 × 10 240 · 1 минута 8 секунд · GPU · Vulkan» in 70.3 s; FATAL/SIGSEGV/OOM 0 |
| 9 | 2026-10-15 11:39 +03:00 | build with the owner's 11:35–11:36 words (round ring ends, 24 dp divider fades) → `python s_look.py`, `python s_ring.py` (8× run, screenshot at 6 s, «Отменить») | ring at 9 % with round ends; fades ≈ 24 dp; «Отменить» → «Улучшение», no error notice |
| 10 | 2026-10-15 11:43 +03:00 | build with «Рендер…» → `python s_render2.py` (PNG save; tap and `screencap` in one device shell, 0.4 s apart) | the card «Рендер…» over the picture, «Сохраняем…» on the button; at 90 % opacity the knob showed through → made opaque |
| 11 | 2026-10-15 11:49 +03:00 | build with the knob following the panel + the calm spinner → `python s_knob2.py` | knob y = 965 px with the panel shown (was 1280); a grab 55 dp off at y 965 drags the line 799 → 1196; the same at the old y 1280 leaves it; panel swiped away → knob y = 1279 |
| 12 | 2026-10-15 11:51 +03:00 | path to the result → `python s_render2.py` | opaque card «Рендер…» with the calm spinner, the knob above it |
| 14 | 2026-10-15 11:54 +03:00 | build with «Отменить» under «Рендер…» → `python s_cancel.py` (8× run, PNG save, «Отменить» 2 s in) | the render stopped 2.2 s after the tap; `content query` and `ls /sdcard/Pictures/KARTUN` show no new item; no error line |
| 15 | 2026-10-15 11:59 +03:00 | build with still counters + heavy digits → `python s_digits.py` (8× run, UI-tree lines read every 0.5 s) | «Проход 1 из 2 · тайл ␣␣7 из 165» (figure spaces) and «Осталось ≈ 48 секунд» on two lines; heavy digits in the scale line, the compact header and the result panel (`d2`, `d5`, `d6` crops) |
| 13 | 2026-10-15 11:42 +03:00 | `python s_c13.py` twice (share timed by window focus, no UI-tree polling) | sheet after 2.4 s both times — the 16 s seen in run 10's tree-polling loop was the harness, not the app |

## 4. Checks

Hygiene: unit 28/28 (`:app:testDebugUnitTest`)
Functional run: the main path in portrait and the result screen's divider gestures, walked on the Titan as the user (system
picker, taps, swipes); READ: the screen labels through the UI agent, screencaps (divider pixels), `logcat`.

| Case | Status | Observation |
|---|---|---|
| `ea9d205` control: 55 dp off the line, far above the knob | pass | the line stays at x = 800 px on both builds (`k1_control.webp`) |
| `ea9d205` positive: 55 dp off the line, at the knob's height | pass | the line follows to x = 1302 / 1301 px (finger stopped at 1300; `k2_knob.webp`) |
| C5 portrait regression (run 9 path) | pass | same texts as run 9 of `2026-10-14_F2_screens_slice1.md`: «960 × 1280 → 1920 × 2560», «8 секунд · GPU · Vulkan» |
| K1 portrait shows the portrait layouts | pass | home, «Улучшение», processing and result in their portrait layouts (`c5_*.webp`) |
| K2 a 2× run has no «2 прохода» | pass | processing counted «из 35» tiles, no pass line |
| C7 cancel (in-app «Отменить», not the notification's) | pass | back on «Улучшение» with no error (run 9); the notification's button is not walked yet |
| C8 8× → 7680 × 10 240 in two passes | pass | run 8 |
| C9 16× refused, notice names 8× | pass | run 7 |
| C11 save PNG → new file, original intact | pass | run 5 |
| C12 JPEG and WebP smaller than PNG | pass | run 5 (quality 100 — the dialog's remembered value, not the case's 92) |
| C13 share sheet with the PNG | partial | sheet opens (run 6); the delivery to Telegram is not walked — the owner's account |
| C19 card estimates grow ≈ 5× at 8× | pass | run 8: 11 s → 57 s, 1:49 → 9:05 |
| C25 pass and tile lines | pass | run 8: one count of 165 across both passes (35 + 130) |
| owner 11:35 round ring ends | pass | run 9 screenshot |
| owner 11:36 short divider fades | pass | run 9 crop: ≈ 24 dp |
| owner 11:38 «Рендер…» over the picture | pass | runs 10, 12 |
| owner 11:45 knob between the screen top and the panel, centre when hidden | pass | run 11 (pixels) |
| owner 11:47 calmer spinner | built, seen in run 12 | speed is the owner's eye (taste class); one turn 1.6 s × the device's animator scale 0.5 |
| owner 11:53 «Отменить» under «Рендер…», the file deleted | pass | run 14 |
| owner 11:57 pass/tile counters stand still | pass (form) | run 15: padded counter + its own time line; the stillness to the eye is the owner's |
| owner 11:58 heavy digits, thin «×» and «→» | pass | run 15 crops |
| defect seen: picked photo named «63282.jpg» in the compact header | open | the picker URI's display name is the media id; `photo1.jpg` expected — S3, to EXPERIENCE/next step |

## 5. Found

- none so far

## 6. Traces

- screenshots (WebP, session scratchpad, not in git): `k0_start`, `k1_control`, `k2_knob`, `c5_0_home`, `c5_1_enhance`,
  `c5_2_processing`, `c5_3_result`
- harness scripts (throwaway, session scratchpad): `drv.py`, `knob_control.py`, `s_c5.py`

## 7. Verdict

partial — the `ea9d205` control and C5/K1/K2 pass; the rest of the case set is being walked in this session.
