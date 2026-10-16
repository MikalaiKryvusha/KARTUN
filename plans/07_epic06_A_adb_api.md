# Plan 07 — Epic 06, phase A: the ADB door — agent service, one shared engine, `tools/kartun-cli.mjs`, `docs/agent-api.md`

> **Создан:** 2026-10-16 (agent, `/plan-epic` rung 3 on the owner's order in chat 2026-10-15: «API для ИИ-агентов по ADB и HTTPS (ideas/02); Сейчас делаем!»)
> **Родитель:** `plans/06_EPIC_agent_api.md` §3 row «A — кабель», gate: A1–A10 pass on the Titan with a run report; recon `researches/10_agent_api_recon.md`
> **Статус:** 🔧 written 2026-10-16; step 0 ✅ (catalogue committed `fc6b8d5`); step 1 ✅ partial 2026-10-16 11:55 +03:00 — the door works on the Titan (INFO from a stopped app, screen asleep, no notification, the app reads pushed files), A7 not observed (`testcases/reports/2026-10-16_epic06_A_step1_door_spike.md`); step 2 ✅ 2026-10-16 12:15 (`UpscaleEngine`); next — step 3
> **Вовне:** the owner's fork `plans/06` §6.3 (the ADB door in the release build) — until answered, the door is built in `src/main` and moves to `src/debug` on a «только Debug» answer

## Goal vector

Achieve: an agent on the PC runs `node tools/kartun-cli.mjs upscale <file>` and gets the upscaled file plus one JSON
line, over adb, with KARTUN closed and the Titan's screen untouched (`plans/06` §1). Avoid: a second copy of the engine
logic, a second engine thread, any activity start from the agent path (EXP-0010).

## Acceptance criteria

The criteria are `plans/06` §2 A1–A10 verbatim; this plan adds no new ones. Each step below names the criterion it
serves. Done = every A-row observed on the Titan and written to `testcases/reports/<date>_epic06_A_agent_api.md`.

## The protocol this phase builds (the contract `docs/agent-api.md` will publish)

Component (both builds; `<pkg>` = `com.kriniks.kartun` or `com.kriniks.kartun.debug`):
`<pkg>/com.kriniks.kartun.agent.AgentService` — exported, `android:permission="android.permission.DUMP"`,
`android:foregroundServiceType="mediaProcessing"` (`plans/06` §4.2).

```
# submit (the CLI makes the job id: [A-Za-z0-9_-]{8,64})
adb push photo.jpg /sdcard/Android/data/<pkg>/files/agent/<job>.in.jpg
adb shell am start-foreground-service -n <pkg>/com.kriniks.kartun.agent.AgentService \
  -a com.kriniks.kartun.agent.UPSCALE --es job <job> --es in <job>.in.jpg \
  --es model <id|default> --ei scale 4 --es format png --ei quality 92 [--ez gpu true|false]
# poll                     adb exec-out cat /sdcard/Android/data/<pkg>/files/agent/<job>.json
# result                   adb pull /sdcard/Android/data/<pkg>/files/agent/<job>.out.png
# cancel                   … -a com.kriniks.kartun.agent.CANCEL --es job <job>
# device info and models   … -a com.kriniks.kartun.agent.INFO   →  files/agent/info.json
```

Status file `<job>.json` (written by the app atomically: `<job>.json.tmp` → rename; at most every 500 ms while running):

```json
{"v":1,"job":"k3x9a1f0c2","state":"running","model":"realesrgan-x4plus","scale":4,"format":"png","gpu":true,
 "progress":0.42,"tiles_done":21,"tiles_total":50,"elapsed_ms":1234,"estimate_ms":5000,
 "in":{"w":256,"h":256},"out":null,"error":null}
```

States `queued · running · succeeded · failed · canceled` (`plans/06` §4.4). On `succeeded`, `out` =
`{"file":"<job>.out.png","w":1024,"h":1024,"bytes":…}`; on `failed`, `error` = `{"code":…,"message":…}` with codes
`bad_request · input_missing · decode_failed · too_big · model_not_installed · model_load_failed · engine_failed · busy ·
quota · internal`. The CLI adds its own `no_device · adb_missing · timeout`.

`info.json`: `{"v":1,"app":{"package","version","build"},"device":{"manufacturer","model","sdk"},"gpu":"<NcnnUpscaler.gpuInfo()>",
"gpu_default":true,"charging":true,"queue":0,"models":[{"id","name","scale","installed","default"}],
"limits":{"max_input_mp":100,"max_output_mp":500,"scales":[1,2,4,8,16],"formats":["png","jpg","webp"]}}`.

## Steps

0. [x] **Coordination gate** (`plans/06` §5.1 risk 3). Exit: `git log -- app/src/main/kotlin/com/kriniks/kartun/UpscaleViewModel.kt
   app/src/main/kotlin/com/kriniks/kartun/ModelStore.kt app/src/main/AndroidManifest.xml` shows the catalogue commit of
   `plans/05`, and `git status --short app/` is empty. Then re-read `UpscaleViewModel.kt`, `ModelStore.kt`,
   `UpscaleService.kt`, `ResultStore.kt` — the line references in step 2 are from 2026-10-16 and may have moved.

1. [x] **Device spike: the door's five unverified facts** (done 2026-10-16 11:55 +03:00: facts 1, 2, 3, 5 pass; fact 4 blocked —
   `run-as <our package>` runs as OUR uid, which a component's permission never blocks, so the check below cannot fail on
   the door; A7 moves to step 9 with a caller of another uid — a tiny probe APK of our own; report
   `testcases/reports/2026-10-16_epic06_A_step1_door_spike.md`) (`plans/06` §4.2, §4.3; `researches/10` §1.2, §1.4 `[UNVERIFIED]`).
   Build only the skeleton: `app/src/main/kotlin/com/kriniks/kartun/agent/AgentService.kt` answering `INFO` (writes
   `files/agent/info.json` with `app`, `device`, `gpu`), its manifest entry (exported, DUMP, `mediaProcessing`), and
   nothing else. On the Titan (`KARTUN_SERIAL=<serial>`, small files only — the owner 2026-10-15 15:34):
   1. `adb install -r` the debug APK, do NOT open it; `am start-foreground-service … -a com.kriniks.kartun.agent.INFO`
      → `info.json` appears (fresh install = stopped state) and logcat has no `ForegroundServiceStartNotAllowedException`.
   2. `am force-stop <pkg>`, screen off, repeat → `info.json` rewritten (new mtime).
   3. `adb push test256.png …/files/agent/x.in.png` into a folder the shell created; a temporary INFO field `"probe_read":
      <bytes of x.in.png>` proves the app reads it, the rewrite of `info.json` proves it writes there.
   4. `adb shell run-as com.kriniks.kartun.debug am start-foreground-service -n com.kriniks.kartun.debug/com.kriniks.kartun.agent.AgentService -a com.kriniks.kartun.agent.INFO`
      → refused with a permission error (criterion A7).
   5. Record whether a notification is visible during an INFO start (expected: none — the FGS lives under a second).
   Exit: all five observations in the run report. Any of 1–4 fails → stop, write the failure into `researches/10` §5
   `FORK: ADB door` and switch to option (b) (`content call` provider) before step 4. Remove the `probe_read` field.

2. [x] **One shared engine** (done 2026-10-16 12:15 +03:00: `UpscaleEngine.kt`, the net keyed by model AND backend; 43 JVM
   tests; Titan by touch — 8×, 4×, «Отменить» at tile 2/13, model switch, no crash —
   `testcases/reports/2026-10-16_epic06_A_step2_shared_engine.md`) (`plans/06` §4.1; risk 5.1.1–2). New file `app/src/main/kotlin/com/kriniks/kartun/UpscaleEngine.kt`:
   a process-wide `object UpscaleEngine` that owns what `UpscaleViewModel` owns today — the single-thread dispatcher
   (`engineThread`), the loaded net (`loaded`, `loadedModel`, `ensureLoaded`, `release`), the pass plan and the progress
   mapping of `start()`'s engine block, `withFallback`, `decode` with `MAX_INPUT_MP`, the `fits`/`MAX_OUTPUT_MP` check and
   `learnRate`. API sketch: `suspend fun run(ctx, input: Bitmap, model, scale, useGpu, onProgress: (done, total, gpu) -> Unit,
   cancelled: () -> Boolean): Output(bitmap, gpuUsed, ms)` and `suspend fun preload(ctx, model, useGpu)`.
   `UpscaleViewModel` calls it and keeps the screens, the choices, the estimate text and `ProcessingState`; its
   `onCleared` releases the net through the engine and never closes the shared thread. Move the code verbatim — a
   behaviour change is out of scope. Exit: `.\gradlew.bat :app:testDebugUnitTest assembleDebug` green (29 tests today,
   `PassPlanTest`, `EstimateTest` among them); on the Titan, by touch: one 4× run, one 8× run, one «Отменить» mid-run,
   results and ring as before (screenshots in WebP, R4).

3. [ ] **The job contract in pure Kotlin** (`plans/06` §4.4; A6). New file `app/src/main/kotlin/com/kriniks/kartun/agent/AgentJob.kt`:
   `JobSpec.parse(extras: Map<String, Any?>)` → spec or `bad_request` (job id regex, `in` = a bare file name with no `/`
   or `..`, scale ∈ `UpscaleViewModel.SCALES`, format ∈ png/jpg/webp, quality 1–100); `JobStatus` → JSON string; the
   file names (`<job>.in.*`, `<job>.json`, `<job>.out.<ext>`). JSON is written by a 20-line escaper in this file:
   `org.json` is a stub in JVM unit tests. New test `app/src/test/kotlin/com/kriniks/kartun/AgentJobTest.kt`: a valid
   spec, `../x.png`, `a/b.png`, scale 3, format `gif`, a job id of 7 chars, a status with a quote and a newline in the
   message (escaped). Exit: the test is mutation-proven — break the `..` check, see it red, restore.

4. [ ] **The agent service** (`plans/06` §4.2, §4.3, §4.1; A2, A4, A5, A9). Complete `AgentService.kt`:
   - every `onStartCommand` calls `startForeground(…, FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING)` first (the 10-second
     promise, `researches/10` §1.4), with the default notification deferral (no `FOREGROUND_SERVICE_IMMEDIATE`, so an
     `INFO` start shows nothing);
   - `UPSCALE` → validate (step 3) → write `queued` → FIFO queue, cap `[AI]` 8, above it `failed`/`busy` → on its turn
     decode `files/agent/<in>` through `UpscaleEngine.decode`, `UpscaleEngine.run` with a per-job cancel flag, status
     every ≤ 500 ms, encode with step 5, write `succeeded` with `out`, delete `<job>.in.*`;
   - `CANCEL` → set that job's flag (queued → `canceled` at once; running → `canceled` after the current tile);
   - `INFO` → write `info.json` (models from the catalogue/`ModelStore` as landed in step 0; `charging` from
     `BatteryManager`; `gpu_default` from the app's GPU setting);
   - `onTimeout` → running job `failed`/`quota`, `stopSelf()`;
   - stop the service when the queue is empty; on each start delete `files/agent/*` older than 24 h (R1) — by the time
     the APP last wrote the job's `<job>.json`, never by the pushed input's mtime: `adb push` keeps the PC file's date
     (step 1: `x.in.png` arrived dated two days back), so an mtime rule would delete a fresh input.
   The notification shows «Задание агента · 42 %» with «Отменить» (cancels the running job). Exit: the A2 command by raw
   adb (the protocol block above) succeeds on the Titan with KARTUN force-stopped.

5. [ ] **Encode to a file** (A3). Add `ResultStore.writeFile(bmp, file, format, quality, cancel)` beside `saveToGallery`,
   reusing `Format`, `Cancellable` and the WebP side limit (`fits`); a too-large WebP fails with `bad_request` and a
   message naming PNG/JPEG. Exit: A3's six runs give the right sizes and signatures (`file` on the PC).

6. [ ] **Notification strings in 10 languages** (`plans/01` §2.12). New strings for step 4's notification, added through
   `python tools/make-translations.py` (EN and RU by hand, eight `[AI]` drafts). Exit: `python tools/make-translations.py --check`
   and `node tools/check-app-names.mjs` green.

7. [ ] **The PC CLI** (`plans/06` §4.5; A1, A2, A5, A6, A9). New file `tools/kartun-cli.mjs`, Node built-ins only:
   - adb from `KARTUN_ADB` (default `C:/Android/Sdk/platform-tools/adb.exe`, then `adb` on PATH), serial from `--serial`
     or `KARTUN_SERIAL`; every call `spawnSync(ADB, argv)` with no shell (EXP-0002); package: `--pkg`, else the installed
     one of `com.kriniks.kartun` / `com.kriniks.kartun.debug` (`pm list packages`), release first;
   - `info` and `models` (send INFO, wait for a fresh `info.json` ≤ 10 s); `upscale <file…> [--model id|default]
     [--scale 1|2|4|8|16] [--format png|jpg|webp] [--quality N] [--gpu|--cpu] [--out path|dir] [--timeout s] [--keep]`
     (push → submit → poll every 500 ms with a progress line on stderr → pull → delete the job files unless `--keep`;
     several files run one after another); `status <job>`; `cancel <job>`;
   - Ctrl+C during `upscale` sends CANCEL before exiting;
   - stdout: one JSON line per result (`"ok":true|false` plus the status fields); exit codes 0 · 1 · 2 · 3;
   - the default output name is `<name>_x<scale>.<ext>` next to the input;
   - the CLI never starts an activity (EXP-0010).
   Header comment: purpose, plan, usage, `[NOT-TESTED]` until step 9 replaces it with `[TESTED: <date> · …]`.
   Exit: `node tools/kartun-cli.mjs --help` prints the usage; A1 and A2 pass on the Titan.

8. [ ] **The API doc** (`plans/06` §4.4; A8). New file `docs/agent-api.md` (English — agents read it): what KARTUN is in
   two lines; quick start (`info`, `upscale`); the CLI reference; the raw-adb protocol block for agents without Node; the
   status/info JSON fields; states; error codes and exit codes; limits (100 MP in, 500 MP out, scales, WebP side,
   `mediaProcessing` 6 h per 24 h → `quota`); privacy line «the image stays on the device you sent it to; nothing goes
   to the internet». README (EN and RU) gets one line in its features list with the link — the storefront rule
   (`AGENT_GUIDE.md` → The storefront). Exit: every command in the doc copy-pasted into a shell works on the Titan.

9. [ ] **Device run of A1–A10** (`plans/06` §3 gate A). New case set `testcases/TC_epic06_A_agent_api.md` (one case per
   A-row, each with its command and expected output, mapped to `plans/06` §2), run on the Titan, report
   `testcases/reports/<date>_epic06_A_agent_api.md`. Details that the cases must carry:
   - A2: `magick compare -metric PSNR <out> engine/third_party/testdata/ref_x4plus_256.png null:` ≥ 38 dB;
   - A4: `adb shell dumpsys activity activities | grep topResumedActivity` before and after — identical; one run with
     KARTUN force-stopped and the screen off (`input keyevent 223`), one while the owner's other app is on top;
   - A5: `realesrgan-x4plus` on `test1mp.png`, `cancel` at 30 s, seconds until `canceled` ≤ 5;
   - A6: `--model nope` → `model_not_installed`, exit 1; adb disconnected (`--serial 1.2.3.4:5555`) → `no_device`, exit 3;
   - A7: a caller of ANOTHER uid without DUMP is refused — not `run-as <our package>` (step 1.4 showed it runs as our own
     uid); a tiny probe APK of our own that calls `startForegroundService` on the door, installed and removed in the run;
   - A9: `adb shell ls /sdcard/Android/data/<pkg>/files/agent/` after A2 → only `info.json` (or empty);
   - A10: A2 with airplane mode on (`cmd connectivity airplane-mode enable`, then `disable`) — over a USB cable, since
     airplane mode drops adb over Wi-Fi;
   - UI regression: one «Улучшить» from the app while an agent job runs → the UI job waits, then finishes (queue on one
     engine thread); no crash in `files/crashes/`.
   Leave the Titan as found (EXP-0010, R1): KARTUN on its home screen if it was, test files deleted.
   Exit: every A-row has an observed PASS in the report.

10. [ ] **Fresh-agent check of the doc** (A8). A subagent gets only `docs/agent-api.md` and the serial, no repo access
    beyond `tools/kartun-cli.mjs`, and is told: «get the device info, upscale this 256×256 PNG 4×». Exit: it succeeds
    without questions; anything it asked or got wrong becomes a doc fix and a rerun.

11. [ ] **Closure** (`plans/06` §7). Status lines of `plans/06` and this plan; `ideas/02` status; `STATUS.md` handover;
    an `EXPERIENCE.md` entry for every non-obvious finding of steps 1 and 9; the phase B plan
    (`plans/NN_epic06_B_lan_server.md`) written now, after the owner's answers to `plans/06` §6.1, §6.2, §6.4, §6.5;
    this file renamed `07_DONE_epic06_A_adb_api.md` when the gate holds.

## Risks

- The engine move (step 2) shifts the UI's timing: preload, cancel and the second pass now cross an object boundary —
  covered by the step 2 touch run before any agent code.
- `ProcessingState.cancelRequested` is global today; an agent job must use its own flag, or «Отменить» in the app would
  stop an agent job — `AgentJobTest` cannot see this, the step 9 UI-regression case does.
- A big agent job plus a big UI photo can exceed memory → same limits; `too_big` / `engine_failed` reported honestly.
- adb over Wi-Fi drops when the Titan's Wi-Fi sleeps — the CLI reports `no_device` (exit 3); the run report says which
  transport was used (USB or Wi-Fi).
