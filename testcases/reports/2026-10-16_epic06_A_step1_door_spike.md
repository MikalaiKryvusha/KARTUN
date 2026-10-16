# Run report — 2026-10-16 · epic 06, phase A, step 1: the ADB door spike (`AgentService` answering `INFO`)

## Work
`plans/07_epic06_A_adb_api.md` step 1 — the five unverified facts of the ADB door (`plans/06` §4.2, §4.3; `researches/10`
§1.2, §1.4 `[UNVERIFIED]`), on the skeleton `app/src/main/kotlin/com/kriniks/kartun/agent/AgentService.kt`: an exported
service guarded by `android.permission.DUMP`, `foregroundServiceType="mediaProcessing"`, whose `INFO` writes
`files/agent/info.json` (app, device, GPU) atomically; a temporary field `probe_read` (bytes of `x.in.png`) for check 3.

## Contour
HEADWOLF Titan 1 (Android 16 / API 36, Mali-G615) at `100.99.111.48:5555`; `app-debug.apk` built 11:52:52 (with
`probe_read`) and 11:55:05 (without it), from the working tree on top of `a965610`; KARTUN never opened during the runs
(focus: `com.android.launcher3/…SearchLauncher`). Input `engine/third_party/testdata/test256.png` (88 933 bytes).

## Runs
All commands through `C:/Android/Sdk/platform-tools/adb.exe -s 100.99.111.48:5555` with `MSYS_NO_PATHCONV=1`;
`SVC=com.kriniks.kartun.debug/com.kriniks.kartun.agent.AgentService`.
1. 11:53:31 — `install -r app-debug.apk`; `shell am start-foreground-service -n $SVC -a com.kriniks.kartun.agent.INFO`;
   `ls -la --full-time …/files/agent; cat …/info.json`; `logcat -d -T '<start>'`.
2. 11:53:5x — `am force-stop com.kriniks.kartun.debug` (`dumpsys package` → `stopped=true`); `input keyevent 223`
   (`dumpsys power` → `mWakefulness=Asleep`); the INFO start; `ls --full-time info.json`; `input keyevent 224` at the end.
3. 11:53:5x — the INFO start, 0.3 s later `dumpsys notification --noredact | grep 'NotificationRecord.*pkg=com.kriniks.kartun.debug'`,
   and again 2 s later.
4. 11:53:5x — `push test256.png …/files/agent/x.in.png`; the INFO start; `cat info.json`.
5. 11:54 — `shell run-as com.kriniks.kartun.debug am start-foreground-service -n $SVC -a …INFO`, then the same with `--user 0`;
   `pm list permissions -f -g` for `android.permission.DUMP`.
6. 11:55:13 — `install -r` the build without `probe_read`; delete `info.json`, `x.in.png`; the INFO start; `cat info.json`;
   delete `info.json` and the `agent/` folder.

## Checks
- Hygiene: 43 JVM tests green, 0 failures (`.\gradlew.bat :app:testDebugUnitTest assembleDebug`, both builds); the built
  manifest carries the service with `permission=android.permission.DUMP`, `exported=true`, `foregroundServiceType=0x2000`
  (`aapt dump xmltree`).
- Functional run: the agent's path — raw adb from the PC to an unopened KARTUN — read from `info.json`, its mtime, logcat,
  `dumpsys package/power/notification`.

| # | Case (plans/07 step 1) | Status | Observed |
|---|---|---|---|
| 1 | install, do not open, INFO → `info.json`, no `ForegroundServiceStartNotAllowedException` | pass | `info.json` 257 bytes at 11:53:32.596 (start 11:53:31): `{"v":1,"app":{"package":"com.kriniks.kartun.debug","version":"0.1 (1)","build":"debug"},"device":{"manufacturer":"HEADWOLF","model":"Titan 1","sdk":36},"gpu":"gpu_count=1 name=Mali-G615 MC6 …","probe_read":null}`; logcat `Background started FGS: Allowed [callingPackage: com.android.shell … code:SYSTEM_UID]`. After an UPDATE install the package was `stopped=false` — the true stopped state is case 2 |
| 2 | force-stop + screen asleep, INFO → `info.json` rewritten | pass | `stopped=true`, `mWakefulness=Asleep`; logcat `Start proc 15095:com.kriniks.kartun.debug … for service {…AgentService}`; mtime 11:53:32.596 → 11:53:54.296 |
| 3 | the app reads a file the shell pushed into `files/agent/` | pass | `x.in.png` owner `shell`, mode `rw-rw-rw-`; `"probe_read":88933` = its size; the app's own rewrite of `info.json` beside it |
| 4 | a caller without DUMP is refused (A7) | blocked | `run-as` runs as KARTUN's OWN uid (u0a247), so the DUMP check never applies to it: without `--user` → `SecurityException … asks to run as user -2 … requires INTERACT_ACROSS_USERS`; with `--user 0` → `ForegroundServiceStartNotAllowedException … mAllowStartForeground false` (the background-FGS rule, not DUMP). The plan's check cannot fail on the door — see Found. On paper: `DUMP` is `protectionLevel:signature\|privileged\|development` — no ordinary app holds it unless adb grants it |
| 5 | no notification visible during an INFO start | pass | no `NotificationRecord` of the package at 0.3 s and at 2.3 s (default FGS deferral; the service lives ~0.2 s: `Stop FGS timeout` at +0.23 s) |
| 6 | the build without `probe_read` still answers | pass | `info.json` without the field; the `agent/` folder removed afterwards, `files/` = `crashes/`, `test108.png`, `test256.png` |

## Found
1. Case 4 of plan 07 is designed wrong: `run-as <our package>` IS our package, a component's permission never blocks its
   own uid. A7 needs a caller of ANOTHER uid without DUMP — a tiny probe APK of our own (`com.kriniks.kartun.probe`, one
   button / one `am`-free `startForegroundService`) is the cheapest honest caller; the debuggable apps of other sessions
   on the Titan (KrinikCam, Krinik Tribit) are not ours to use.
2. `adb push` keeps the PC file's mtime: `x.in.png` arrived dated 2026-10-14 13:56. Step 4's «delete `files/agent/*`
   older than 24 h» by mtime would delete a fresh input before its job runs — the cleanup must key on the job's own
   status file time or on the app's own write time, not on the pushed file's mtime.

## Traces
Command output in this session's transcript (quoted above); nothing pulled — `info.json` contents are quoted in full.

## Verdict
partial — facts 1, 2, 3, 5 observed on the Titan; fact 4 (A7, a foreign caller refused) not observed: the planned check
tests our own uid. Not a door failure — no `FORK: ADB door` switch to the content provider; A7 moves to step 9 with a
foreign-uid caller (Found 1).
