# Run report — 2026-10-16 · epic 06, phase A, step 2: one shared engine (`UpscaleEngine`)

## Work
`plans/07_epic06_A_adb_api.md` step 2 — the engine block of `UpscaleViewModel` (the loaded net, `ensureLoaded`, `release`,
the pass plan and progress mapping of `start()`, `withFallback`, `decode` with `MAX_INPUT_MP`, `learnRate`) moved into the
process-wide `object UpscaleEngine` on `EngineThread.dispatcher`; the ViewModel keeps the screens and calls it. Exit of the
step: build and JVM tests green; on the Titan, by touch, one 4× run, one 8× run, one «Отменить» mid-run — results and ring
as before.

## Contour
HEADWOLF Titan 1 (Android 16, Mali-G615) at `100.99.111.48:5555`; `app-debug.apk` built 12:08:17 from the working tree on
top of `53bbeaa` + the step 2 change, installed 12:08; the owner's accumulated app state (stored order, 8× left from
the last session, downloaded models); input `Pictures/KARTUN_test/beach.png` 256 × 256 through the system photo picker.

## Runs
UI agent through a wrapper that passes each command as ONE argv item (`python <scratchpad>/ui.py "tapText …"`); taps on the
Working screen through `KARTUN_SERIAL=100.99.111.48:5555 node tools/titan-tap.mjs <x> <y>`; screenshots
`adb exec-out screencap -p | magick png:- -resize 500x -quality 80 <f>.webp`.
1. 12:09 — `am start … -a android.intent.action.SEND --eu android.intent.extra.STREAM content://media/external/images/media/63267 --grant-read-uri-permission`
   from the adb shell → «Не удалось открыть это фото.» (see Found 1); then «Выберите фото» (`titan-tap 800 1142`) → Подборки →
   На этом устройстве → KARTUN_test → beach → «Готово».
2. 12:12 — «Улучшить · ≈ 2 секунды» (General v3, 8× from the stored setting), `wait Сохранить`, `labels`.
3. 12:13 — Back, scroll, tap the 4× point of the slider (800, 1814), «Улучшить · ≈ 1 секунда», `wait Сохранить`, `labels`.
4. 12:13 — Back, `tapText 4x NMKD Siax` (4×, «≈ 7 секунд»), start, `titan-tap 1383 2078` («Отменить») ≈ 3 s in.
5. 12:14:54 — Back, 8× (1142, 1814), Siax «≈ 34 секунды», start; 12:14:59 `titan-tap 1383 2078` («Отменить», exit 0);
   screenshots at once and 3 s later.
6. 12:15 — `ls …/files/crashes/`, `tail engine-stderr.log`, `pidof com.kriniks.kartun.debug`.

## Checks
- Hygiene: 43 JVM tests green, 0 failures (`.\gradlew.bat :app:testDebugUnitTest assembleDebug`, results 12:08:18),
  `EngineThreadGuardTest` among them (no new executor, nobody stops the engine thread).
- Functional run: the owner's path on the Titan — the picker, «Улучшить», the Working screen, «Отменить», the result —
  read from `labels`, screenshots, the crash journal.

| # | Case | Status | Observed |
|---|---|---|---|
| 1 | a picked photo opens (decode moved to `UpscaleEngine.decode`) | pass | «Улучшение · 256 × 256 · 0,1 Мп» after the picker |
| 2 | 8× run (two passes) | pass | «256 × 256 → 2048 × 2048 · 2 секунды · GPU · Vulkan», result screen with «До / После» |
| 3 | 4× run | pass | «256 × 256 → 1024 × 1024 · 1 секунда · GPU · Vulkan» |
| 4 | model switch through the shared engine | pass | Siax loaded: `2026-10-16_12-13-29 load 4x-NMKD-Siax-fp16 gpu=true thread=kartun-engine` |
| 5 | «Отменить» mid-run | pass | at «15 % · проход 1/2 · тайл 2/13 · прошло 5 с · осталось ≈ 25 с» → 3 s later «Улучшение», no error line |
| 6 | the ring and the Working screen as before | pass | ring percent, pass, tile, elapsed, left, «GPU · Vulkan»; no spinner on tiles |
| 7 | no crash | pass | `crashes/` holds only the three records of earlier days; pid 16691 alive from 12:09 to 12:15 |

## Found
1. A photo shared from the adb SHELL (`--grant-read-uri-permission` on a MediaStore URI) does not open — «Не удалось открыть
   это фото.», nothing in logcat (`open()` maps every decode failure to that line without logging). Not a regression of this
   step: the same photo opens through the picker on the same build. The shell most likely cannot grant a media URI it does
   not own — a harness limit, not tested further. `[AI]` `open()` logging its failure with `Log.w` would have answered
   this in one line — left for the owner's backlog, not done (out of the step's scope).
2. Run 4's «Отменить» (≈ 3 s into a 7 s, 4-tile run) still showed the result: the tap reached the last tile, and the
   listener is read between tiles. Not a defect — the repeat on a 13-tile run (case 5) cancelled.

## Traces
Screenshots in the session scratchpad (`run1_8x.webp`, `setup2.webp`, `run3_working.webp`, `run4_a.webp`, `run4_b.webp`);
outputs quoted above.

## Verdict
pass — the step's exit observed on the Titan: 4×, 8×, a mid-run cancel, the ring, a model switch, no crash.
