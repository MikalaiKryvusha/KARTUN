# Run report — 2026-10-16 · full-screen photo: double tap zooms in, the next one steps back

## Work
The owner's word 2026-10-16 ≈ 13:2x +03:00: «я просил, чтобы дабл тап во фул скрин превью фото увеличивал фото» · «а повторный
дабл тап - уменьшал на шаг назад». `app/src/main/kotlin/com/kriniks/kartun/ui/FullPhoto.kt`: a double tap zooms ×2.5 at the
tapped point (the point stays under the finger) and remembers the zoom and pan it left; the next double tap returns to them.

## Contour
HEADWOLF Titan 1, `app-debug.apk` built ≈ 13:27 from the working tree, installed ≈ 13:27; the beehive test photo
(`Pictures/KARTUN_test`, 1920 × 2560) through the picker; the viewer opened by the handle pulled to 88 % of the screen.

## Runs
1. 13:30 — screenshot of the viewer (`dt2_full.webp`).
2. 13:30 — `adb shell "input tap 608 1280 & sleep 0.12; input tap 608 1280"` on the «80» of the hive; screenshot `dt3_zoom.webp`.
3. 13:30 — the same double tap again; screenshot `dt4_back.webp`; `magick compare -metric RMSE dt4_back.webp dt2_full.webp null:`.

## Checks
- Hygiene: `.\gradlew.bat assembleDebug` exit 0.
- Functional run: the viewer on the Titan, read from screenshots.

| # | Case | Status | Observed |
|---|---|---|---|
| 1 | double tap zooms in at the point | pass | ×2.5, the «80» stays at the tapped place |
| 2 | the next double tap steps back | pass | the view equals the one before the zoom: RMSE `728.632 (0.0111182)` (the status bar differs) |

Not covered: pinch (adb has no multi-touch), a double tap after a pinch.

## Found
none.

## Traces
`dt2_full.webp`, `dt3_zoom.webp`, `dt4_back.webp` in the session scratchpad.

## Verdict
pass.
