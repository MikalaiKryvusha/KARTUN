# Run report — 2026-10-16 · «Улучшение»: collapsed by default, the handle without snap points, full screen below 80 %

## Work
The owner's order 2026-10-16 ≈ 12:46 +03:00 (MASTER_PLAN decision log): «Давай экран Улучшение по-умолчанию открывать со
свернутым фото в строку. Драг хендл чтобы не прилипал к конкретным местам, а превью фото разворачивалось на такой размер, на
который драг хендл пролистнули … А на всеь экран пусть разворачивается, когда драг хендл ниже, чем 80% высоты экрана».
`app/src/main/kotlin/com/kriniks/kartun/ui/SetupScreen.kt`: the preview starts at the row (`HERO_MIN`); the handle and the
photo pull it 1:1 up to the bottom of the screen, no rubber band and no snap; let go with the handle's centre below 80 % of
the screen height → the full-screen viewer, and the preview returns to its size before that pull; the settings' own scroll
still shrinks the preview and grows it back up to `HERO_MAX` (330 dp).

## Contour
HEADWOLF Titan 1, screen 1600 × 2560, portrait; `app-debug.apk` built ≈ 12:48 from the working tree on top of the step 9
commit, installed 12:48; `beach.png` 256 × 256 picked through the system photo picker (Подборки → На этом устройстве →
KARTUN_test); Siax, 8× from the stored settings.

## Runs
Screenshots `adb exec-out screencap -p | magick png:- -resize 500x -quality 80 <f>.webp` (factor 3.2 to device px).
1. 12:50 — the Setup screen as it opens.
2. 12:50 — `input swipe 800 534 800 1300 1500` (handle to 51 %), screenshot; `input swipe 800 1300 800 1900 1200` (74 %), screenshot.
3. 12:51 — `input swipe 800 1846 800 2200 1000` (86 %), screenshot; `input keyevent 4`, screenshot.
4. 12:51:4x — `input swipe 800 1846 800 300 1200`; `labels`.

## Checks
- Hygiene: `.\gradlew.bat :app:testDebugUnitTest assembleDebug` exit 0 (54 JVM tests).
- Functional run: the owner's path on the Titan, read from the screenshots and the UI agent's labels.

| # | Case | Status | Observed |
|---|---|---|---|
| 1 | opens collapsed | pass | the row «63267.png · 256 × 256 → 2048 × 2048 · Другое фото», the handle under it at y ≈ 534 |
| 2 | stays where let go, 51 % | pass | released at 1300 → handle at ≈ 1274 (the platform's touch slop), preview ≈ 330 dp, settings below |
| 3 | past the old limit, 74 % | pass | released at 1900 → handle at ≈ 1846, preview taller than the old 330 dp cap, no rubber band |
| 4 | below 80 % → full screen | pass | released at 2200 (86 %) → the full-screen viewer with «‹» |
| 5 | back from full screen | pass | the preview at its size before that pull (handle ≈ 1846) |
| 6 | back up to the row | pass | `labels`: «Улучшение · 63267.png · 256 × 256 → 2048 × 2048 · Другое фото» |

Not covered: landscape (the screen has no handle there), the photo card itself as the handle (only the bar was dragged),
a fast fling.

## Found
`[AI]` choices the owner did not name: after the full-screen viewer closes, the preview returns to its size BEFORE the pull
(not to the 80 % line, not to the row); a scroll of the settings grows the preview back only up to 330 dp — further growth
is the handle's alone, so a fling of the list does not throw the photo over the screen. Both for the owner's verdict.

## Traces
Screenshots `s1_default.webp`, `s2_mid.webp`, `s3_74.webp`, `s4_full.webp`, `s5_back.webp` in the session scratchpad.

## Verdict
pass — the three asks observed on the Titan; the feel of the handle is the owner's to judge.
