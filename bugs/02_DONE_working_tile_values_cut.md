# Bug 02 — экран обработки: число в плитке «Тайл» обрезано («459 /» вместо «459 / 1 344»)

**Status:** ✅ DONE · **Severity:** S2 · **Fixing:** this session
**Version/build:** debug, `403582b` · **When/context:** 2026-10-16 17:03 +03:00, the owner's screenshot on the Titan during the
50 MP fractional-scale run (plans/08 step 9); reported in chat ≈ 17:05

## Symptom

The owner: «а плитках номера Тайлон не помещаются. Смотри, чтобы эти плитки уважительно работали с длинными текстами и
номерами». The «ТАЙЛ» tile shows `459 /`; the total (1 344 tiles) is gone. Seen in portrait, `Screenshot_20261016-170341.png`.

## Repro (deterministic)

A photo with over 999 tiles (chimp50mp.jpg, 8640 × 5760 → 1 344 tiles of 200 px) → «Улучшить» → the processing screen.

## Root cause

`Metric` (ui/WorkingScreen.kt) drew the value with `maxLines = 1` and soft wrap ON: when «459 / 1344» is wider than the tile,
Compose wraps after «/» and keeps only the first line - no ellipsis, the number silently loses its half. The tile is a quarter of
the panel; four-digit totals and long labels of other languages do not fit at 14 sp.

## Fix

By form, for every tile and every language: label and value are one line that shrinks to the tile (`FitText`, Compose
`TextAutoSize.StepBased`, down to 8 / 10 sp), never wraps. Landscape tiles - bug 03.

TWINS: searched `maxLines = 1` without `overflow`/`softWrap` in app/src - found 1 other site: `app/src/debug/.../DebugTools.kt:316`
(debug build only, a centred one-line label; left as is). All other one-line texts already carry `overflow = Ellipsis`.

## Decisions made without the owner

- `[AI]` shrink instead of two lines for the tiles (two lines would change the tile height as numbers grow); minimum 10 sp for the value.

## Links

bugs/03 (landscape) · testcases/reports/2026-10-16_fractional_scale_titan.md

## ✅ STATUS: DONE (2026-10-16 17:13 +03:00)

Hygiene: `.\gradlew.bat :app:testDebugUnitTest assembleDebug` green; `:app:lintDebug` 0 errors.
Functional run: zurich84mp at 4× (2176 tiles) on the Titan, the processing screen read in portrait and landscape by the UI agent
and screenshots - testcases/reports/2026-10-16_fractional_scale_titan.md run 4. The owner's eye on the new look is still his verdict.
