# Run report — 2026-10-16 · the Working screen waits for an API job (variant C)

## Work
The owner's word 2026-10-16 ≈ 13:2x +03:00 on the sheet `design/processing-waiting.html`: «макет C принят, но не нужно писать
про ИИ агента,а нужно писать, что KARTUN занят заданием по API». Built: `UpscaleEngine.apiBusy` / `stopApiJob()`, the API run
marked `fromApi`; the app's run clock starts when the engine takes it (`Progress.started`); `WorkingScreen.kt` shows
«Ждёт очереди» · «KARTUN занят заданием по API. Ваше фото — следующее.» · tiles «Проход —», «Тайл —», «Ждёт», «Работа ≈» ·
«Остановить задание по API»; five new strings in 10 languages.

## Contour
HEADWOLF Titan 1, `app-debug.apk` built ≈ 13:33 and again ≈ 13:37 (no «now» tile while waiting, the stop button small);
the beehive photo 1920 × 2560, General v3, 4×; the API job: `kartun-cli upscale test1mp.png --model realesrgan-x4plus --scale 4`.

## Runs
1. 13:35:18 the API job; 13:35:24 `titan-tap 848 2326` («Улучшить»); 13:35:2x screenshot `w2_waiting.webp`; 13:35:52
   `titan-tap 800 2368` («Остановить задание по API»); +4 s screenshot `w3_running.webp`; the CLI's JSON; ≈ 13:37 `labels`.
2. 13:38 (the second build) the same start; screenshot `w4_waiting.webp`; `kartun-cli cancel <job>`; the app's run cancelled;
   screenshot `w5.webp`; `ls files/agent/`, `ls files/crashes/ | wc -l`.

## Checks
- Hygiene: `.\gradlew.bat :app:testDebugUnitTest assembleDebug` exit 0 (54 JVM tests); `make-translations.py --check` green.
- Functional run: the owner's path on the Titan, read from screenshots, labels and the CLI's JSON.

| # | Case | Status | Observed |
|---|---|---|---|
| 1 | waiting screen as mockup C | pass | «⏳ · Ждёт очереди · KARTUN занят заданием по API. Ваше фото — следующее. · Отменить · Проход — · Тайл — · Ждёт 4 с · Работа ≈ 34 с · Остановить задание по API» |
| 2 | «Остановить задание по API» | pass | the API job `"state":"canceled"`, `"progress":0.222`; 4 s later the app's run «3% · тайл 5 / 130 · прошло 2 с» |
| 3 | the wait stays out of the times | pass | «Прошло 2 с» right after the start; the result «7680 × 10 240 · 51 секунда» after a 28 s wait |
| 4 | the second build's fixes | pass | no pink tile while waiting; the stop button 260 dp on the left |
| 5 | nothing left behind | pass | `files/agent/` empty; `crashes/` unchanged |

## Found
none.

## Traces
`w2_waiting.webp`, `w3_running.webp`, `w4_waiting.webp`, `w5.webp`, `cli/w.out` in the session scratchpad.

## Verdict
pass.
