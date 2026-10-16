# Run report — 2026-10-16 · tiles without the spinner, the top card of «Улучшение», «Все модели» by author, bugs/01

## Work
The owner's three requests of 2026-10-16 (chat, before 10:48 +03:00; commit `eda6a40`): «Убираем нашу живую анимацию из
тайлов по время улучшения фото» · «Выбранная модель становится выбранной в верхней плиточке. Предыдущая которая была в
верхней плиточки смещается в нижнюю. Кнопка Все модели не принимает в себя вид модели» · «Список всех моделей сортируем по
авторам моделей. В начале вверху списка модели автора моделей Ультрамикс ультрашарп». Then bugs/01 (the owner's crash of
10:23:32, found in the app's crash journal) — its acceptance: the repro aborts on the old build and runs clean on the fixed one.

## Contour
HEADWOLF Titan 1 (Android 16, Mali-G615) at `100.99.111.48:5555` (moved from the owner's `:38701` by `tcpip 5555` at 10:51);
`app-debug.apk` of `eda6a40` installed 10:52, the fix build (EngineThread) installed 11:10; the owner's accumulated app
state (stored order General v3 / x4plus, x4plus chosen, scale 8×; downloaded General v3, x4plus, x4plus anime); test photo
`Pictures/KARTUN_test` beehives 1920 × 2560 (picked through the system photo picker).

## Runs
1. 10:53–10:55 — UI agent `python C:\Android\private\ui-agent\ui.py --serial 100.99.111.48:5555`: `tapText Выберите фото`,
   `tap 800 2317`, `tapText Готово` → «Улучшение»; screenshot `setup1.webp`.
2. 10:55–10:59 — `tapText Все модели`, `adb shell input swipe 800 2200 800 1000 400` ×8 with `labels` after each; picks
   `tapText 4xNomosWebPhoto RealPLKSR`, `tapText 4x NMKD Siax`, `tapText Real-ESRGAN General v3`, `labels` + screenshots after each.
3. 10:59–11:03 — «Улучшить» at 8× (General v3), `adb exec-out screencap -p | magick png:- -resize 700x …` at 1:39, 1:55 (pass 2)
   and, on a second start (`node tools/titan-tap.mjs 800 2330`), at 3 s and 13 s (pass 1); `titan-tap 1383 2078` («Отменить»).
4. 11:04 — `adb pull /sdcard/Android/data/com.kriniks.kartun.debug/files/crashes/` (the owner's crash 10:23:32).
5. 11:06–11:08 — old build: Back, Back (app leaves, `pidof` 10628 stays) → `am start -n …/MainActivity` → pick a photo.
6. 11:10–11:13 — fix build: `powershell -File repro01.ps1 -Rounds 3`, twice (pick → Back out → reopen, per round).

## Checks
- Hygiene: 43 JVM tests green (`.\gradlew.bat :app:testDebugUnitTest assembleDebug`), new: `CatalogueTest.allModelsListGoesByAuthorUltraSharpsAuthorFirst`,
  `RecencyTest.aPickInAllModelsTakesTheTopCardAndPushesTheTopOneDown`, `EngineThreadGuardTest` (red on `eda6a40` — three offenders).
- Functional run: the owner's path on the Titan, read from `labels`, screenshots, `pidof` and the crash journal.

| # | Case | Status | Observed |
|---|---|---|---|
| 1 | «Все модели» starts with Kim2091 | pass | ClearReality V1, UltraMix Balanced, UltraSharp V2, UltraSharp V2 Lite — then FoolhardyVEVO (Remacri), NMKD (Siax, Superscale), Phhofm (HFA2k, Nomos8kSC, NomosWebPhoto), Xintao Wang (AnimeVideo v3, General v3, x4plus, x4plus anime) |
| 2 | A pick takes the top card, the old top goes down | pass | before: General v3 / x4plus✓; NomosWebPhoto → NomosWebPhoto✓ / General v3; Siax → Siax✓ / NomosWebPhoto; General v3 → General v3✓ / Siax |
| 3 | «Все модели» stays a plain row, no third card | pass | `setup3.webp`: two cards, then «Все модели · Моделей: 14 · на устройстве: 4 · Выбрать ›» |
| 4 | No living spinner on tiles | pass | pass 1 (big tiles, 13/650 and 53/650) and pass 2 (398/650, 454/650): the current tile is a pink square, no animation |
| 5 | Run completes | pass | 1920 × 2560 → 15 360 × 20 480 in 2 min 51 s, GPU · Vulkan |
| 6 | bugs/01 repro on the old build | fail (expected) | process 10628 gone; `load realesr-general-x4v3 … thread=pool-7-thread-1` + `OMP: Error #13: Assertion failure at kmp_affinity.cpp(4755)` |
| 7 | bugs/01 repro on the fix build | pass | 6 rounds, pids 12185 / 12615 alive throughout; loads after a Back-out on `thread=kartun-engine` (11:10:38 → 11:11:08, 11:12:20 → 11:12:50); no new crash record |

Not run: the first round of each repro script picked no photo (the tap came before the picker was ready) — counted by the
load lines, not by rounds; landscape; the CPU path.

## Found
1. bugs/01 — root cause found and fixed (`bugs/01_DONE_openmp_affinity_abort_on_model_load.md`).
2. The UI agent times out (`TimeoutError` in `ui.py`) while the processing screen redraws; screenshots by plain adb work.

## Traces
Scratchpad of session `80e6d879-…`: `setup1.webp`, `setup2.webp`, `setup3.webp`, `work1.webp`, `work2.webp`, `p1a.webp`,
`p1b.webp`, `res.webp`, `cancel.webp`, `crashes/` (pulled journal), `repro01.ps1`; on the Titan `files/crashes/2026-10-16_11-08-10_native.*`.

## Verdict
pass — the three requests seen working on the Titan; bugs/01 reproduced and fixed by observation.
