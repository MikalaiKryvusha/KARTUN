# Test run report — дробный масштаб у предела и плитки экрана обработки на Титане (plans/08 step 9, bugs 02 and 03)

**Created:** 2026-10-16 17:13 +03:00 · **Run by:** the agent (Claude Opus 5.5) · **Version/build:** debug, working tree after
`403582b` (APK 16:58 for runs 1–2, 17:07 with the fitted tiles for run 4)

## 1. Work

(1) Over the output limit a scale step runs at the largest factor that fits - the owner 2026-10-16 ≈ 16:10 «да, правильно» to
«фото 50 Мп → шаг «4×» на ползунке становится «3,2×», подпись «Предел — 536 Мп. 4× для этого фото не поместится, можем 3,2×»».
(2) Bugs 02 and 03 from the owner's screenshots 17:03: tile numbers cut, landscape title and tiles cut.

## 2. Contour

HEADWOLF Titan 1 (`100.99.111.48:5555`), Android 16, debug build; photos `chimp50mp.jpg` (8640 × 5760, media id 63298) and
`zurich84mp.jpg` (12 732 × 6619, id 63297), Real-ESRGAN General v3 on GPU. UI by the UI agent's locators; scrolling and screen
rotation have no locator - `adb shell input swipe` once (setup screen) and the system rotation setting (restored after).

## 3. Runs

| # | Moment | Command | Exit / outcome |
|---|---|---|---|
| 1 | 2026-10-16 17:00 +03:00 | `adb shell am start -W -S -a android.intent.action.VIEW -t image/jpeg -d content://media/external/images/media/63298 --grant-read-uri-permission -n com.kriniks.kartun.debug/com.kriniks.kartun.MainActivity`, then `ui.py labels`, a swipe, a screenshot | setup screen read |
| 2 | 2026-10-16 17:00–17:08 +03:00 | `python run_limited.py` (tapText Улучшить, labels until «Сохранить») | result after 428 s |
| 3 | 2026-10-16 17:09–17:11 +03:00 | save dialog: `tapText JPEG`, `tapText Сохранить` (quality 100); `adb pull …/chimp50mp_kartun_realesr-general-x4v3_4x.jpg`; `.venv-convert/Scripts/python.exe -I tools/seam-check.py chimp_limited.jpg 656.3425925925926 656.3541666666666` | ratios 1.025 / 1.035 |
| 4 | 2026-10-16 17:11 +03:00 | `python tiles_check.py` (zurich84mp at 4× → processing screen, portrait shot, rotation to landscape, shot, «Отменить», rotation restored) | both layouts read |

## 4. Checks

Hygiene: `.\gradlew.bat :app:testDebugUnitTest assembleDebug` green (16:58, 17:07); `LimitedSizeTest` 3/3 (8640 × 5760 at 4× →
factor 3.2 within 536 MP; 4000 × 3000 at 4× not limited; at 8× → 6.6); `:app:lintDebug` 0 errors.
Functional run: the user's path on the Titan - open a 50 MP photo, read the scale slider and the notice, «Улучшить», read the
result screen, save as JPEG, measure seams on the saved file; a 84 MP run's processing screen read in portrait and landscape.

| Case | Status | Observation |
|---|---|---|
| Slider and size over the limit | pass | marks `1× · 2× · 3,2×` (3,2× lit), `8640 × 5760 → 28 354 × 18 903`, `536,0 Мп` |
| The notice | pass | screenshot: «Предел — 536 Мп. 4× для этого фото не поместится, можем 3,2× (28 354 × 18 903)» (the agent's `find` did not see it; the screenshot did) |
| The run | pass | `8640 × 5760 → 28 354 × 18 903 · 7 минут 3 секунды · GPU · Vulkan` |
| Saving the 536 MP result | pass | `chimp50mp_kartun_realesr-general-x4v3_4x.jpg`, 186 340 945 bytes, «Сохранено в Pictures/KARTUN» |
| File name of a limited result | fail → fixed | the run-3 build named it `_4x`; `scaleLabel` now writes `_3.2x` (built 17:02, the device run of the name waits for the release run) |
| Seams at the fractional tile borders | pass | vertical border/near 1.025 (border 1.889, near 1.844), horizontal 1.035 (1.830 / 1.767) |
| Bug 02 - tile «Тайл» with a 4-digit total, portrait | pass | `21 / 2176` whole (was `459 /`) |
| Bug 03 - landscape | pass | title in two lines «Нейросеть / улучшает фото», tiles 2 × 2: `1 / 1`, `62 / 2176`, `17 с`, `≈ 9 мин 49 с`, labels whole |
| Two-pass limited (8× where 4× fits, e.g. 12 MP → 6,6×) | skipped | JVM test only; no device run this session |

## 5. Found

- bugs/02 — tile values cut by a soft wrap under maxLines = 1; fixed (FitText), verified run 4.
- bugs/03 — landscape title and tiles cut; fixed (two-line title, 2 × 2 tiles), verified run 4.
- The limited result's file name said `4x` — fixed in the same step (ResultStore.fileName with a text scale).

## 6. Traces

Screenshots (session scratchpad): `limited_setup.webp`, `limited_setup2.webp`, `run50.webp`, `tiles_portrait.webp`,
`tiles_landscape.webp`; the owner's `Screenshot_20261016-170341/170349.png` on the Titan. The 186 MB JPEG stays in
`Pictures/KARTUN/` on the Titan until the release run, then deleted with the other test results (R1).

## 7. Verdict

pass — a step over the 536 MP limit runs at the largest factor that fits, the screen says so, the result has no seams; the
processing screen's tiles and title fit in portrait and landscape. Not walked on the device: the two-pass limited case.
