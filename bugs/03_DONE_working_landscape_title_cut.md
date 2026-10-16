# Bug 03 — экран обработки в альбомной ориентации: заголовок «Нейросеть улу…» и плитки обрезаны

**Status:** ✅ DONE · **Severity:** S2 · **Fixing:** this session
**Version/build:** debug, `403582b` · **When/context:** 2026-10-16 17:03 +03:00, the owner's screenshot on the Titan in landscape
during the 50 MP run; reported in chat ≈ 17:05

## Symptom

The owner: «в лендскейпе не проработан дизайн экрана. Обзезается заголовок "Нейросеть улучшает фото"». On
`Screenshot_20261016-170349.png`: the title reads «Нейросеть улу…», the tiles read «ОСТАЛО», «≈ 4», «2 мин», «483 /».

## Repro (deterministic)

Any run → rotate the Titan to landscape → the processing screen. The landscape layout had never been walked on the device
(`[NOT-TESTED: landscape - built only]` in WorkingScreen.kt; TC_F2 C3 open).

## Root cause

The landscape panel is 420 dp wide (PANEL_WIDTH) and kept the portrait layout: ring 84 dp + title + «Отменить» in one row leaves
the title ~150 dp at 15 sp, one line, ellipsis; four tiles in one row leave ~65 dp of text each.

## Fix

- The title may take two lines and shrinks to 11 sp (`FitText`, maxLines = 2).
- Landscape: the four tiles stand in two rows of two; each tile's text shrinks to fit (bug 02).

## Decisions made without the owner

- `[AI]` 2 × 2 tiles in landscape instead of a wider panel - the photo keeps its half of the screen.

## Links

bugs/02 · testcases/TC_F2_guarded_loop_device_backlog.md C3

## ✅ STATUS: DONE (2026-10-16 17:13 +03:00)

Hygiene: `.\gradlew.bat :app:testDebugUnitTest assembleDebug` green; `:app:lintDebug` 0 errors.
Functional run: zurich84mp at 4× (2176 tiles) on the Titan, the processing screen read in portrait and landscape by the UI agent
and screenshots - testcases/reports/2026-10-16_fractional_scale_titan.md run 4. The owner's eye on the new look is still his verdict.
