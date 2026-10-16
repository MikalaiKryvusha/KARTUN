# Run report — 2026-10-16 · epic 06, phase A, steps 4–6: the agent service (UPSCALE · CANCEL · INFO · queue), file output, notification strings

## Work
`plans/07_epic06_A_adb_api.md` steps 4 (the complete `agent/AgentService.kt`), 5 (`ResultStore.writeFile`: `.tmp` →
rename, WebP side limit) and 6 (`agent_job_progress` in 10 languages through `tools/make-translations.py`), checked
against the protocol block of plans/07 and the SERVICE halves of the criteria of `plans/06` §2 — A2, A3, A4, A5, A6 are
written there as CLI commands (step 7); here the same requests go by raw adb.

## Contour
HEADWOLF Titan 1 (Android 16, Mali-G615) at `100.99.111.48:5555`; `app-debug.apk` built ≈ 12:21 from the working tree on
top of `ce828d7` (step 3 commit) + steps 4–6, installed 12:21; KARTUN force-stopped before the first job and never opened
(focus stayed on the launcher); the owner's model state (4 models on the device, NomosWebPhoto default); inputs
`engine/third_party/testdata/test256.png` (256 × 256) and `hathaway.jpg` (6415 × 8019, 51 MP — for the cancel only).

## Runs
Driver `bash <scratchpad>/agent-run.sh <job> <input> <scale> <format> [am args]`: `adb push <input> …/files/agent/<job>.in.<ext>`
→ `adb shell am start-foreground-service -n com.kriniks.kartun.debug/com.kriniks.kartun.agent.AgentService -a
com.kriniks.kartun.agent.UPSCALE --es job <job> --es in <job>.in.<ext> --ei scale <s> --es format <f> …` → poll
`adb exec-out cat …/<job>.json` every 0.5 s. All adb with `MSYS_NO_PATHCONV=1`.
1. 12:22:14 — `am force-stop`, then job `a2test0001` 4× png; `adb pull` the output, `magick identify`; `ls` the folder; `dumpsys window | grep mCurrentFocus`.
2. 12:22:3x — `a3jpg00002` 2× jpg `--ei quality 80`; `a3webp0003` 1× webp.
3. 12:23:01 — five bad starts: scale 3, `in ../x.png`, `model no-such-model`, no input file, job id `e5short`; the four status
   files read 3 s later; `logcat | grep 'agent: bad request'`.
4. 12:23:2x — three jobs back to back: `c1cancel07` (hathaway 2× jpg), `q1queue008` (4× `--ez gpu false`), `q2queue009` (4×);
   statuses at 3 s; `-a …CANCEL --es job c1cancel07` at 12:23:24; statuses 25 s later; `dumpsys notification` during q1.
5. 12:24:12 — the queue drained; `dumpsys activity services com.kriniks.kartun.debug | grep -c AgentService`; active notifications.
6. 12:24:3x — `-a …INFO`, `info.json` parsed with Python `json`; `rm -rf …/files/agent`.

## Checks
- Hygiene: 54 JVM tests green and `assembleDebug` (`.\gradlew.bat :app:testDebugUnitTest assembleDebug`, exit 0);
  `python tools/make-translations.py --check` green, `git diff --stat app/src/main/res` = exactly one new string per language.
- Functional run: the agent's path — raw adb from the PC, KARTUN closed — read from the status files, the pulled image,
  logcat, `dumpsys`.

| # | Case (criterion) | Status | Observed |
|---|---|---|---|
| 1 | A2 / A4 (service half; app closed, screen ON in this run): a job on a closed app, the file comes back | pass | `succeeded`, `in 256×256 → out 1024×1024`, 1 569 580 bytes, `elapsed_ms 5517`, GPU; pulled file `PNG 1024x1024 8-bit sRGB`; focus stayed `SearchLauncher` |
| 2 | A3 (1×, 2×, 4× — 8× not run here): formats and scales | pass | 2× jpg → 512 × 512, 31 689 bytes; 1× webp → 256 × 256, 13 108 bytes |
| 3 | A6: bad requests answer with codes | pass | scale 3 → `bad_request` «scale must be one of 1, 2, 4, 8, 16»; `../x.png` → `bad_request` «in must be a bare file name»; `no-such-model` → `model_not_installed`; no file → `input_missing`; `e5short` → no status file, logcat `agent: bad request e5short: job must match …` |
| 4 | A5: cancel a running job | pass | `c1cancel07` `canceled` at tile 2 / 1353, `elapsed_ms 4727` (the 51 MP photo would have taken ≈ 55 min: `estimate_ms 3336006`) |
| 5 | queue, one after another | pass | at 3 s: c1 running, q1 and q2 `queued`; q1 ran after the cancel, q2 after q1 |
| 6 | CPU on request | pass | q1 `"gpu":false`, 4 tiles in 39 216 ms; q2 on the GPU 5 527 ms |
| 7 | notification during a long job | pass | `id=3 … FOREGROUND_SERVICE actions=1`, `android.title=Задание агента · 50%` during q1 |
| 8 | the service leaves when the queue is empty | pass | `AgentService` records: 0; no active notification of the package |
| 9 | input deleted on success, kept on cancel | pass | the folder after the run: `*.json`, `*.out.*`; inputs left only of the canceled c1 and of jobs not yet run |
| 10 | INFO: models and limits | pass | valid JSON; 14 models, installed: General v3, x4plus, Siax, NomosWebPhoto (default); `limits` 100 / 500 MP, scales 1–16, formats png/jpg/webp; `charging false`, `queue 0` |

Not covered here: A1 (the CLI — step 7), A4 with the screen off (step 1 showed INFO with the screen asleep; no UPSCALE
was run so), A7 (a foreign caller — step 9), A8 (the doc — step 8), A9 (the folder clean after `upscale` — the CLI pulls
and deletes, step 7), A10 (airplane mode), the queue cap of 8 (`busy`), `onTimeout` / `quota`, the 24 h sweep, the WebP
side limit of step 5 (no result over 16 383 px was made), «Отменить» in the notification (not tapped).

## Found
1. The estimate for a CPU job came from the catalogue's starting rate (63 750 ms) and the run took 39 216 ms; for the 51 MP
   photo the GPU estimate was 55 min. Neither is a defect of this step — the estimate is the screens' own (learned per
   device); noted for the CLI's progress line.
2. none else.

## Traces
The status JSON lines quoted above; pulled `a2test0001.out.png` in the session scratchpad; the device folder removed after
the run (R1).

## Verdict
partial — the service halves of A2, A3 (without 8×), A4 (app closed), A5 and A6 observed on the Titan by raw adb; A1, A7,
A8, A9, A10 and the CLI halves belong to steps 7–9.
